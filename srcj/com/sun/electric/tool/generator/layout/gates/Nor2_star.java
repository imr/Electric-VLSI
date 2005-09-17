/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Nor2_star.java
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

class Nor2_star {
	private static final double nmosTop = -9.0;
	private static final double pmosBot = 9.0;
	private static final double wellOverhangDiff = 6;
	private static final double inaY = 4.0;
	private static final double inbY = -4.0;
	private static final double outHiY = 11.0;
	private static final double outLoY = -11.0;
    
	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}
	
	static Cell makePart(double sz, String threshold,
						 StdCellParams stdCell) {
		sz = stdCell.roundSize(sz);
		error(!threshold.equals("") && !threshold.equals("LT"),
			  "Nor2: threshold not \"\" or \"LT\": "+threshold);
		
		String nm = "nor2" + threshold;
		
		sz = stdCell.checkMinStrength(sz, 1, nm);
		
		// Compute number of folds and width for PMOS
		int nbStackedP = 2;
		double spaceAvail = stdCell.getCellTop() - wellOverhangDiff - pmosBot;
		double totWid = sz * nbStackedP * (threshold.equals("LT") ? 3 : 6);
		FoldsAndWidth fwP = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 1);
		error(fwP==null, "can't make nor2 this small: "+sz);
		
		// Compute number of folds and width for NMOS
		spaceAvail = nmosTop - (stdCell.getCellBot() + wellOverhangDiff);
		totWid = sz * 3 * 2;  // pullups come in pairs
		FoldsAndWidth fwN = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 2);
		error(fwN==null, "can't make nor2 this small: "+sz);
		
		// create NOR Part
		Cell nor = stdCell.findPart(nm, sz);
		if (nor!=null) return nor;
		nor = stdCell.newPart(nm, sz);
		
		// leave vertical m1 track for inB
		double inbX = 1.5 + 2;		// m1_m1_sp/2 + m1_wid/2
		double nmosX = inbX + 2 + 3 + 2;// m1_wid/2 + m1_m1_sp + diffCont_wid/2
		
		// PMOS transistors will set the gasp cell width.  Pack PMOS
		// transistors into one FoldedMos.  Allocate two NMOS transistors
		// per FoldedMos.  Align NMOS gate 1 with PMOS gate 1
		
		// PMOS
		double pmosX = nmosX + 3;
		double pmosY = pmosBot + fwP.physWid/2;
		FoldedMos pmos = new FoldedPmos(pmosX, pmosY, fwP.nbFolds, nbStackedP,
										fwP.gateWid, nor, stdCell);
		
		double nmosY = nmosTop - fwN.physWid/2;
		FoldedMos[] nmoss = new FoldedMos[fwN.nbFolds/2];
		for (int nbFoldsN=0; nbFoldsN<fwN.nbFolds; nbFoldsN+=2) {
			double nmosPitch = 26;
			double x = nmosX + (nbFoldsN/2)*nmosPitch;
			FoldedMos nmos = new FoldedNmos(x, nmosY, 2, 1, fwN.gateWid, nor, stdCell);
			nmoss[nbFoldsN/2] = nmos;
		}
		stdCell.fillDiffAndSelectNotches(nmoss, true);
		
		// create vdd and gnd exports and connect to MOS source/drains
		stdCell.wireVddGnd(nmoss, StdCellParams.EVEN, nor);
		stdCell.wireVddGnd(pmos, StdCellParams.EVEN, nor);
		
//		// fool Electric's NCC into paralleling PMOS stacks by connecting
//		// stacks' internal diffusion nodes.
//		for (int i=0; i<pmos.nbInternalSrcDrns(); i++) {
//			LayoutLib.newArcInst(Tech.universalArc, 0,
//								 pmos.getInternalSrcDrn(0),
//								 pmos.getInternalSrcDrn(i));
//		}
		
		// Nor input A
		double inaX = StdCellParams.getRightDiffX(pmos, nmoss) + 2 + 3 + 2;
		LayoutLib.newExport(nor, "ina", PortCharacteristic.IN, Tech.m1,
							4, inaX, inaY);
		TrackRouter inA = new TrackRouterH(Tech.m1, 3, inaY, nor);
		inA.connect(nor.findExport("ina"));
		for (int i=0; i<pmos.nbGates(); i+=2) {
			if (i/2 % 2 == 0) {
				inA.connect(pmos.getGate(i, 'B'), -4, Tech.getPolyLShapeOffset());
			} else {
				inA.connect(pmos.getGate(i+1, 'B'), 4, Tech.getPolyLShapeOffset());
			}
		}
		for (int i=0; i<nmoss.length; i++) {
			inA.connect(nmoss[i].getGate(0, 'T'), -1);
		}
		
		// Nor input B
		// m1_wid + m1_space + m1_wid/2
		LayoutLib.newExport(nor, "inb", PortCharacteristic.IN, Tech.m1,
							4, inbX, inbY);
		TrackRouter inb = new TrackRouterH(Tech.m1, 3, inbY, nor);
		inb.connect(nor.findExport("inb"));
		for (int i=0; i<pmos.nbGates(); i+=2) {
			if (i/2 % 2 == 0){
				inb.connect(pmos.getGate(i+1, 'B'));
			} else {
				inb.connect(pmos.getGate(i, 'B'));
			}
		}
		for (int i=0; i<nmoss.length; i++) {
			inb.connect(nmoss[i].getGate(1, 'T'));
		}
		
		// Nor output
		double outX = inaX + 2 + 3 + 2;	// m1_wid/2 + m1_sp + m1_wid/2
		LayoutLib.newExport(nor, "out", PortCharacteristic.OUT, Tech.m1,
							4, outX, outHiY);
		TrackRouter outHi = new TrackRouterH(Tech.m2, 4, outHiY, nor);
		outHi.connect(nor.findExport("out"));
		for (int i=1; i<pmos.nbSrcDrns(); i+=2) {
			outHi.connect(pmos.getSrcDrn(i));
		}
		TrackRouter outLo = new TrackRouterH(Tech.m2, 4, outLoY, nor);
		outLo.connect(nor.findExport("out"));
		for (int i=0; i<nmoss.length; i++) {
			for (int j=1; j<nmoss[i].nbSrcDrns(); j+=2) {
				outLo.connect(nmoss[i].getSrcDrn(j));
			}
		}
		
		// add wells
		double wellMinX = 0;
		double wellMaxX = outX + 2 + 1.5; // m1_wid/2 + m1m1_space/2
		stdCell.addNmosWell(wellMinX, wellMaxX, nor);
		stdCell.addPmosWell(wellMinX, wellMaxX, nor);
		
		// add essential bounds
		stdCell.addEssentialBounds(wellMinX, wellMaxX, nor);
		
		// perform Network Consistency Check
		// RKao fixme NCC with "wrong" library element
		stdCell.doNCC(nor, nm+"{sch}");
		
		return nor;
	}
}


