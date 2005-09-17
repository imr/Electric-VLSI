/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Nand2LT.java
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

public class Nand2LT {
	private static final double nmosTop = -9.0;
	private static final double pmosBot = 9.0;
	private static final double wellOverhangDiff = 6;
	private static final double inaY = -4.0;
	private static final double inbY = 4.0;
	private static final double outHiY = 11.0;
	private static final double outLoY = -11.0;
    
	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}
	
	public static Cell makePart(double sz, StdCellParams stdCell) {
		sz = stdCell.roundSize(sz);
		String nm = "nand2LT";
		sz = stdCell.checkMinStrength(sz, 1, nm);
		
		// Compute number of folds and width for PMOS
		double spaceAvail = stdCell.getCellTop() - wellOverhangDiff - pmosBot;
		double totWid = sz * 3 * 2;	// 2 independent pullups
		FoldsAndWidth fwP = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 2);
		error(fwP==null, "can't make nand2 this small: "+sz);
		
		// Compute number of folds and width for NMOS
		int nbStacked = 2;
		spaceAvail = nmosTop - (stdCell.getCellBot()+wellOverhangDiff);
		totWid = sz * 3 * nbStacked;
		FoldsAndWidth fwN = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 2);
		error(fwN==null, "can't make nand2LT this small: "+sz);
		
		// create NAND Part
		Cell nand = stdCell.findPart(nm, sz);
		if (nand!=null) return nand;
		nand = stdCell.newPart(nm, sz);
		
		// leave vertical m1 track for inA
		double inaX = 1.5 + 2;		// m1_m1_sp/2 + m1_wid/2
		double mosX = inaX + 2 + 3 + 2;// m1_wid/2 + m1_m1_sp + diffCont_wid/2
		
		// NMOS
		double nmosY = nmosTop - fwN.physWid/2;
		FoldedMos nmos = new FoldedNmos(mosX, nmosY, fwN.nbFolds, nbStacked,
										fwN.gateWid, nand, stdCell);
		
		// PMOS FoldedMos.  Each FoldedMos has exactly 2 folds.
		double pmosY = pmosBot + fwP.physWid/2;
		FoldedMos[] pmoss = new FoldedMos[fwP.nbFolds/2];
		for (int i=0; i<pmoss.length; i++) {
			double pmosPitch = 26;
			double pmosX = mosX + pmosPitch * i;
			pmoss[i] = new FoldedPmos(pmosX, pmosY, 2, 1, fwP.gateWid, nand, stdCell);
		}
		stdCell.fillDiffAndSelectNotches(pmoss, true);
		
		// create vdd and gnd exports and connect to MOS source/drains
		stdCell.wireVddGnd(nmos, StdCellParams.EVEN, nand);
		stdCell.wireVddGnd(pmoss, StdCellParams.EVEN, nand);
		
//		// fool Electric's NCC into paralleling NMOS stacks by connecting
//		// stacks' internal diffusion nodes.
//		for (int i=1; i<nmos.nbInternalSrcDrns(); i++) {
//			LayoutLib.newArcInst(Tech.universalArc, 0,
//								 nmos.getInternalSrcDrn(0),
//								 nmos.getInternalSrcDrn(i));
//		}
		
		// Nand input B
		// m1_wid + m1_space + m1_wid/2
		double inbX = StdCellParams.getRightDiffX(nmos, pmoss) + 2 + 3 + 2;
		LayoutLib.newExport(nand, "inb", PortCharacteristic.IN, Tech.m1,
							4, inbX, inbY);
		TrackRouter inb = new TrackRouterH(Tech.m1, 3, inbY, nand);
		inb.connect(nand.findExport("inb"));
		for (int i=0; i<nmos.nbGates(); i+=2) {
			if (i/2 % 2 == 0) {
				inb.connect(nmos.getGate(i+1, 'T'), Tech.getPolyLShapeOffset());
			} else {
				inb.connect(nmos.getGate(i, 'T'), -6.5);
			}
		}
		for (int i=0; i<pmoss.length; i++) {
			inb.connect(pmoss[i].getGate(1, 'B'), -Tech.getPolyLShapeOffset());
		}
		
		// Nand input A
		LayoutLib.newExport(nand, "ina", PortCharacteristic.IN, Tech.m1,
							4, inaX, inaY);
		TrackRouter inA = new TrackRouterH(Tech.m1, 3, inaY, nand);
		inA.connect(nand.findExport("ina"));
		for (int i=0; i<nmos.nbGates(); i+=2) {
			if (i/2 % 2 == 0) {
				inA.connect(nmos.getGate(i, 'T'), -Tech.getPolyLShapeOffset());
			} else {
				//double offset = (i+1==nmos.nbGates()-1) ? 6.5;
				double offset = 6.5;
				PortInst g = nmos.getGate(i+1, 'T');
				double contX = LayoutLib.roundCenterX(g)+offset;
				if (inbX-contX<7) {
					// Shift right-most ina contact so it doesn't interfere with
					// vertical routing track for inb.
					offset -= 7 - (inbX-contX);
				}
				inA.connect(g, offset, -Tech.getPolyLShapeOffset());
			}
		}
		for (int i=0; i<pmoss.length; i++) {
			inA.connect(pmoss[i].getGate(0, 'B'), -Tech.getPolyLShapeOffset());
		}
		
		// Nand output
		double outX = inbX + 2 + 3 + 2;	// m1_wid/2 + m1_sp + m1_wid/2
		LayoutLib.newExport(nand, "out", PortCharacteristic.OUT, Tech.m1,
							4, outX, outHiY);
		TrackRouter outHi = new TrackRouterH(Tech.m2, 4, outHiY, nand);
		outHi.connect(nand.findExport("out"));
		for (int i=0; i<pmoss.length; i++) {
			outHi.connect(pmoss[i].getSrcDrn(1));
		}
		TrackRouter outLo = new TrackRouterH(Tech.m2, 4, outLoY, nand);
		outLo.connect(nand.findExport("out"));
		for (int i=1; i<nmos.nbSrcDrns(); i+=2) {
			outLo.connect(nmos.getSrcDrn(i));
		}
		
		// add wells
		double wellMinX = 0;
		double wellMaxX = outX + 2 + 1.5; // m1_wid/2 + m1m1_space/2
		stdCell.addNmosWell(wellMinX, wellMaxX, nand);
		stdCell.addPmosWell(wellMinX, wellMaxX, nand);
		
		// add essential bounds
		stdCell.addEssentialBounds(wellMinX, wellMaxX, nand);
		
		// perform Network Consistency Check
		stdCell.doNCC(nand, nm+"{sch}");
		
		return nand;
	}
}

