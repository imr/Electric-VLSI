/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Inv2i.java
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

public class Inv2i {
	private static final double nmosTop = -9.0;
	private static final double pmosBot = 9.0;
	private static final double wellOverhangDiff = 6;
	private static final double pGatesY = 4.0;
	private static final double nGatesY = -4.0;
	private static final double outHiY = 11.0;
	private static final double outLoY = -11.0;
	
	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}
	
	public static Cell makePart(double sz, StdCellParams stdCell) {
		sz = stdCell.roundSize(sz);
		String nm = "inv2i";
		sz = stdCell.checkMinStrength(sz, 1, nm);
		
		// Compute number of folds and width for PMOS
		double spaceAvail = stdCell.getCellTop() - wellOverhangDiff - pmosBot;
		double totWid = sz * 6;
		FoldsAndWidth fwP = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 1);
		error(fwP==null, "can't make "+nm+" this small: "+sz);
		
		// Compute number of folds and width for NMOS
		spaceAvail = nmosTop - (stdCell.getCellBot() + wellOverhangDiff);
		totWid = sz * 3;
		FoldsAndWidth fwN = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 1);
		error(fwN==null, "can't make "+nm+" this small: "+sz);
		
		// create BUF Part
		Cell buf = stdCell.findPart(nm, sz);
		if (buf!=null) return buf;
		buf = stdCell.newPart(nm, sz);
		
		double inNX = 1.5 + 2;  // m1_m1_sp/2 + m1_wid/2
		double mosX = inNX + 2 + 3 + 2; // m1_wid/2 + m1_m1_sp + diffCont_wid/2
		
		// PMOS
		double pmosY = pmosBot + fwP.physWid/2;
		FoldedMos pmos = new FoldedPmos(mosX, pmosY, fwP.nbFolds, 1,
										fwP.gateWid, buf, stdCell);
		// NMOS
		double nmosY = nmosTop - fwN.physWid/2;
		FoldedMos nmos = new FoldedNmos(mosX, nmosY, fwN.nbFolds, 1,
										fwN.gateWid, buf, stdCell);
		
		// create vdd and gnd exports and connect to MOS source/drains
		stdCell.wireVddGnd(nmos, StdCellParams.EVEN, buf);
		stdCell.wireVddGnd(pmos, StdCellParams.EVEN, buf);
		
		// Connect NMOS and PMOS gates
		TrackRouter pGates = new TrackRouterH(Tech.m1, 3, pGatesY, buf);
		for (int i=0; i<pmos.nbGates(); i++) {
			pGates.connect(pmos.getGate(i, 'B'));
		}
		TrackRouter nGates = new TrackRouterH(Tech.m1, 3, nGatesY, buf);
		for (int i=0; i<nmos.nbGates(); i++) {
			nGates.connect(nmos.getGate(i, 'T'));
		}
		
		// input n
		LayoutLib.newExport(buf, "in[n]", PortCharacteristic.IN, Tech.m1,
							4, inNX, nGatesY);
		nGates.connect(buf.findExport("in[n]"));
		
		// input p
		double lastSrcDrnX = StdCellParams.getRightDiffX(pmos, nmos);
		double inPX = lastSrcDrnX + 2 + 3 + 2; // ndm1/2 + m1_m1_sp + m1_wid/2
		LayoutLib.newExport(buf,"in[p]", PortCharacteristic.IN, Tech.m1,
							4, inPX, pGatesY);
		pGates.connect(buf.findExport("in[p]"));
		
		// Buf output
		double outX = inPX + 2 + 3 + 2;	// m1_wid/2 + m1_sp + m1_wid/2
		LayoutLib.newExport(buf, "out", PortCharacteristic.OUT, Tech.m1,
							4, outX, 0);
		TrackRouter outHi = new TrackRouterH(Tech.m2, 4, outHiY, buf);
		outHi.connect(buf.findExport("out"));
		for (int i=1; i<pmos.nbSrcDrns(); i+=2) {
			outHi.connect(pmos.getSrcDrn(i));
		}
		
		TrackRouter outLo = new TrackRouterH(Tech.m2, 4, outLoY, buf);
		outLo.connect(buf.findExport("out"));
		for (int i=1; i<nmos.nbSrcDrns(); i+=2) {
			outLo.connect(nmos.getSrcDrn(i));
		}
		
		// add wells
		double wellMinX = 0;
		double wellMaxX = outX + 2 + 1.5; // m1_wid/2 + m1_sp/2
		stdCell.addNmosWell(wellMinX, wellMaxX, buf);
		stdCell.addPmosWell(wellMinX, wellMaxX, buf);
		
		// add essential bounds
		stdCell.addEssentialBounds(wellMinX, wellMaxX, buf);
		
		// perform Network Consistency Check
		stdCell.doNCC(buf, nm+"{sch}");
		
		return buf;
	}
}
