/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Inv_passgate.java
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
import com.sun.electric.database.topology.PortInst;
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
 * Caution: unlike Nand gates, Jon says "LT" for Inverters means
 * increase the size of the NMOS rather than decrease the size of the
 * PMOS.  This seems like a confusing inconsistency to me but.
 */
public class Inv_passgate {
	private static final double DEF_SIZE = LayoutLib.DEF_SIZE;
	private static final double wireWithPolyPitch = 8;
    private static final double wirePitch = 7;
    private static final double wellOverhangDiff = 6;
    private static final double enY = -wireWithPolyPitch/2;
    private static final double inY = wireWithPolyPitch/2;
    private static final double outHiY = 11.0;
    private static final double outLoY = -11.0;

	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}
	
	public static Cell makePart(double sz, StdCellParams stdCell) {
		sz = stdCell.roundSize(sz);
		String nm = "inv_passgate";
		sz = stdCell.checkMinStrength(sz, .5, nm);
		
		// find number of folds and width of PMOS
		// p1_p1_sp/2 + p1m1_wid + p1_diff_sp
		double pmosBot = 1.5 + 5 + 2;
		double spaceAvail = stdCell.getCellTop() - wellOverhangDiff - pmosBot;
		double lamPerSz = 6;
		double totWidP = sz * lamPerSz;
		FoldsAndWidth fwP = stdCell.calcFoldsAndWidth(spaceAvail, totWidP, 1);
		error(fwP==null, "can't make "+nm+" this small: "+sz);
		
		// find number of folds and width of pulldown NMOS
		// -(p1_p1_sp/2 + p1m1_wid + p1_diff_sp)
		double nmosTop =  -(1.5 + 5 + 2);
		
		// cellBot + m1_m1_sp/2 + m1_wid + m1_ndm1_sp
		spaceAvail = nmosTop - (stdCell.getCellBot() + 1.5 + 4 + 2.5);
		lamPerSz = 6;
		double elecWidN = sz * lamPerSz;
		int nbPullDnFolds = (int) Math.ceil(elecWidN/spaceAvail);
		// round up to odd number of folds so pulldown and pass gate can share
		if (nbPullDnFolds%2 ==0) nbPullDnFolds++;
		// round physical width to multiples of .5 lambda
		double gateWidN = Math.rint(elecWidN/nbPullDnFolds*2)/2;
		// but don't exceed space available
		gateWidN = Math.min(gateWidN, spaceAvail);
		double srcDrnWidN = Math.max(gateWidN, 5); // ndm1_wid
		
		// create Inverter Part
		Cell inv = stdCell.findPart(nm, sz);
		if (inv!=null) return inv;
		inv = stdCell.newPart(nm, sz);
		
		// leave vertical m1 track for inverter output
		double invOutX = wirePitch/2;
		double inX = invOutX + wirePitch;
		double mosX = inX + wirePitch;
		double nmosY = nmosTop - srcDrnWidN/2;
		FoldedMos nmos = new FoldedNmos(mosX, nmosY, nbPullDnFolds*2, 1,
										gateWidN, inv, stdCell);
		double pmosY = pmosBot + fwP.physWid/2;
		FoldedMos pmos = new FoldedPmos(mosX, pmosY, fwP.nbFolds, 1,
										fwP.gateWid, inv, stdCell);
		
		// create vdd and gnd exports and connect to MOS source/drains
		stdCell.wireVddGnd(pmos, StdCellParams.EVEN, inv);
		LayoutLib.newExport(inv, "gnd", PortCharacteristic.GND, Tech.m2,
							10, mosX, stdCell.getGndY());
		TrackRouter gnd = new TrackRouterH(Tech.m2, stdCell.getGndWidth(), inv);
		gnd.connect(inv.findExport("gnd"));
		for (int i=0; i<nbPullDnFolds; i+=2)  gnd.connect(nmos.getSrcDrn(i));
		
		// connect inverter output
		PortInst invOut = LayoutLib.newNodeInst(Tech.m1pin, invOutX, 0,
												DEF_SIZE, DEF_SIZE, 0, inv
												).getOnlyPortInst();
		TrackRouter invOutHi = new TrackRouterH(Tech.m2, 4, outHiY, inv);
		invOutHi.connect(invOut);
		for (int i=1; i<pmos.nbSrcDrns(); i+=2) {
			invOutHi.connect(pmos.getSrcDrn(i));
		}
		TrackRouter invOutLo = new TrackRouterH(Tech.m2, 4, outLoY, inv);
		invOutLo.connect(invOut);
		for (int i=1; i<nmos.nbSrcDrns(); i+=2) {
			invOutLo.connect(nmos.getSrcDrn(i));
		}
		
		// Connect input.
		LayoutLib.newExport(inv, "in", PortCharacteristic.IN,
							Tech.m1, 4, inX, inY);
		TrackRouter in = new TrackRouterH(Tech.m1, 3, inY, inv);
		in.connect(inv.findExport("in"));
		for (int i=0; i<pmos.nbGates(); i++) in.connect(pmos.getGate(i, 'B'));
		for (int i=0; i<nbPullDnFolds; i++) in.connect(nmos.getGate(i, 'T'));
		
		double jogX = StdCellParams.getRightDiffX(nmos, pmos) + wirePitch;
		double enX = jogX + wirePitch;
		double outX = enX + wirePitch;
		
		// connect output
		// - nd_m1_sp - m1_wid/2
		double outM1Y = nmosTop - srcDrnWidN - 2.5 - 2;
		TrackRouter outLo = new TrackRouterH(Tech.m1, 3, outM1Y, inv);
		for (int i=nbPullDnFolds+1; i<nmos.nbSrcDrns(); i+=2) {
			outLo.connect(nmos.getSrcDrn(i));
		}
		PortInst jog = LayoutLib.newNodeInst(Tech.m1pin, jogX, outLoY,
											 DEF_SIZE, DEF_SIZE, 0, inv
											 ).getOnlyPortInst();
		outLo.connect(jog);
		
		TrackRouter outHi = new TrackRouterH(Tech.m2, 3, outLoY, inv);
		outHi.connect(jog);
		LayoutLib.newExport(inv, "out", PortCharacteristic.OUT,
							Tech.m1, 4, outX, outLoY);
		outHi.connect(inv.findExport("out"));
		
		// Connect enable.
		LayoutLib.newExport(inv, "en", PortCharacteristic.IN, Tech.m1, 4,
							enX, enY);
		TrackRouter en = new TrackRouterH(Tech.m1, 3, enY, inv);
		en.connect(inv.findExport("en"));
		for (int i=nbPullDnFolds; i<nbPullDnFolds*2; i++) {
			en.connect(nmos.getGate(i, 'T'));
		}
		
		// add wells
		double wellMinX = 0;
		double wellMaxX = outX + 2 + 1.5; // m1_wid/2 + m1m1_space/2
		stdCell.addNmosWell(wellMinX, wellMaxX, inv);
		stdCell.addPmosWell(wellMinX, wellMaxX, inv);
		
		// add essential bounds
		stdCell.addEssentialBounds(wellMinX, wellMaxX, inv);
		
		// perform Network Consistency Check
		stdCell.doNCC(inv, nm+"{sch}");
		
		return inv;
  }
}

