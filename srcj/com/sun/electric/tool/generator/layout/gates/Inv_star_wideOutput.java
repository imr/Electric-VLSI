/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Inv_star_wideOutput.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.generator.layout.gates;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.tool.generator.layout.FoldedMos;
import com.sun.electric.tool.generator.layout.FoldedNmos;
import com.sun.electric.tool.generator.layout.FoldedPmos;
import com.sun.electric.tool.generator.layout.FoldsAndWidth;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.StdCellParams;
import com.sun.electric.tool.generator.layout.Tech;
import com.sun.electric.tool.generator.layout.TrackRouter;
import com.sun.electric.tool.generator.layout.TrackRouterH;

/**
 * Create inverters with wide output busses.
 */
/** run a wide output bus in metal-1 along n-well/p-well boundary */
public class Inv_star_wideOutput {
	private static final double outBusWidth = 10;
	private static final double outBusSpace = outBusWidth>10 ? 6 : 3;
	private static final double inY = 0; 
	private static final double outY = 0;

	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

	public static Cell makePart(double sz, String threshold, StdCellParams stdCell) {
		sz = stdCell.roundSize(sz);
		error(!threshold.equals("") && !threshold.equals("LT")
			  && !threshold.equals("HT") && !threshold.equals("CLK"),
			  "Inv: threshold not \"\", \"LT\", \"HT\" or \"CLK\": " + threshold);
		String nm = "inv"+threshold;

		sz = stdCell.checkMinStrength(sz, threshold.equals("LT") ? .5 : 1, nm);

		double outsideSpace = 2 + 5 + 1.5; // p1_nd_sp + p1m1_wid + p1_p1_sp/2
		double insideSpace = outBusWidth/2 + outBusSpace - .5; // MOS diff surrounds m1 by .5  

		// find number of folds and width of PMOS
		double spaceAvail =
			stdCell.getCellTop() - outsideSpace - insideSpace;
		double lamPerSz;
		if (threshold.equals("HT")) lamPerSz=12;
		else if (threshold.equals("CLK"))lamPerSz=9;
		else lamPerSz=6;

		double totWidP = sz * lamPerSz;
		FoldsAndWidth fwP = stdCell.calcFoldsAndWidth(spaceAvail, totWidP, 1);
		error(fwP==null, "can't make " + nm + " this small: " + sz);

		// find number of folds and width of NMOS
		spaceAvail = -insideSpace - (stdCell.getCellBot() + outsideSpace);
		lamPerSz = threshold.equals("LT") ? 6 : 3;
		double totWidN = sz * lamPerSz;
		FoldsAndWidth fwN = stdCell.calcFoldsAndWidth(spaceAvail, totWidN, 1);
		error(fwN==null, "can't make " + nm + " this small: " + sz);

		// create Inverter Part
		Cell inv = stdCell.findPart(nm, sz);
		if (inv!=null)  return inv;
		inv = stdCell.newPart(nm, sz);

		// leave vertical m1 track for in
		double inX = 1.5 + 2; // m1_m1_sp/2 + m1_wid/2
		LayoutLib.newExport(inv, "in", PortCharacteristic.IN, Tech.m1,
			                4, inX, inY);

		double mosX = inX + 2 + 3 + 2; // m1_wid/2 + m1_m1_sp + m1_wid/2
		double nmosY = -insideSpace - fwN.physWid / 2;
		FoldedMos nmos = new FoldedNmos(mosX, nmosY, fwN.nbFolds, 1, 
		                                fwN.gateWid, inv, stdCell);
		double pmosY = insideSpace + fwP.physWid / 2;
		FoldedMos pmos = new FoldedPmos(mosX, pmosY, fwP.nbFolds, 1,
		                                fwP.gateWid, inv, stdCell);

		// inverter output:  m1_wid/2 + m1_m1_sp + m1_wid/2 
		double outX = StdCellParams.getRightDiffX(nmos, pmos) + 2 + 3 + 2;
		LayoutLib.newExport(inv, "out", PortCharacteristic.OUT,
			                Tech.m1, 4, outX, 0);

		// create vdd and gnd exports and connect to MOS source/drains
		stdCell.wireVddGnd(nmos, StdCellParams.EVEN, inv);
		stdCell.wireVddGnd(pmos, StdCellParams.EVEN, inv);

//		// Connect up input. Do PMOS gates first because PMOS gate spacing
//		// is a valid spacing for p1m1 vias even for small strengths.
//		TrackRouter in = new TrackRouterH(Tech.m1, 3, inY, inv);
//		in.connect(inv.findExport("in"));
//		for (int i=0; i<pmos.nbGates(); i++)  in.connect(pmos.getGate(i, 'B'));
//		for (int i=0; i<nmos.nbGates(); i++)  in.connect(nmos.getGate(i, 'T'));

		// Connect gates using metal1 along bottom of cell 
		double gndBot = stdCell.getGndY() - stdCell.getGndWidth() / 2;
		double inLoFromGnd = gndBot - 3 - 2; // -m1_m1_sp -m1_wid/2
		double nmosBot = nmosY - fwN.physWid / 2;
		double inLoFromMos = nmosBot - 2 - 2.5; // -nd_p1_sp - p1m1_wid/2
		double inLoY = Math.min(inLoFromGnd, inLoFromMos);

		TrackRouter inLo = new TrackRouterH(Tech.m1, 3, inLoY, inv);
		inLo.connect(inv.findExport("in"));
		for (int i = 0; i < nmos.nbGates(); i++) {
			inLo.connect(nmos.getGate(i, 'B'));
		}

		// Connect gates using metal1 along top of cell 
		double vddTop = stdCell.getVddY() + stdCell.getVddWidth() / 2;
		double inHiFromVdd = vddTop + 3 + 2; // +m1_m1_sp + m1_wid/2
		double pmosTop = pmosY + fwP.physWid / 2;
		double inHiFromMos = pmosTop + 2 + 2.5; // +pd_p1_sp + p1m1_wid/2
		double inHiY = Math.max(inHiFromVdd, inHiFromMos);

		TrackRouter inHi = new TrackRouterH(Tech.m1, 3, inHiY, inv);
		inHi.connect(inv.findExport("in"));
		for (int i=0; i<pmos.nbGates(); i++) {
			inHi.connect(pmos.getGate(i, 'T'));
		}

		// connect up output
		TrackRouter out = new TrackRouterH(Tech.m1, outBusWidth, outY, inv);
		out.connect(inv.findExport("out"));
		for (int i=1; i<pmos.nbSrcDrns(); i+=2) {
			out.connect(pmos.getSrcDrn(i));
		}

		for (int i=1; i<nmos.nbSrcDrns(); i+=2) {
			out.connect(nmos.getSrcDrn(i));
		}

		// add wells
		double wellMinX = 0;
		double wellMaxX = outX + outBusWidth/2 + 1.5; // m1_wid/2 + m1m1_space/2
		stdCell.addNmosWell(wellMinX, wellMaxX, inv);
		stdCell.addPmosWell(wellMinX, wellMaxX, inv);

		// add essential bounds
		stdCell.addEssentialBounds(wellMinX, wellMaxX, inv);

		// perform Network Consistency Check
		stdCell.doNCC(inv, nm+"{sch}");

		return inv;
	}
}
