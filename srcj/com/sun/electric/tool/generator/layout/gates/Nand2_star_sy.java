/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Nand2_star_sy.java
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

class Nand2_star_sy {
	private static final double nmosTop = -9.0;
	private static final double pmosBot = 9.0;
	private static final double wellOverhangDiff = 6;
	private static final double inbY = 4.0;
	private static final double inaY = -4.0;
	private static final double outHiY = 11.0;
	private static final double outLoY = -11.0;
    
	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

	static Cell makePart(double sz, String threshold,
						 StdCellParams stdCell) {
		sz = stdCell.roundSize(sz);
		error(!threshold.equals("") && !threshold.equals("HLT"),
			  "Nand2_sy: threshold not \"\" or \"HLT\": "+threshold);
		String nm = "nand2" + threshold + "_sy";
		sz = stdCell.checkMinStrength(sz, 1, nm);
		
		// compute number of folds and width for PMOS
		double spaceAvail = stdCell.getCellTop() - wellOverhangDiff - pmosBot;
		double lamPerSz = threshold.equals("HLT") ? (6 * .75) : 6;
		double totWid = sz * lamPerSz * 2;	// 2 independent pullups
		FoldsAndWidth fwP = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 2);
		error(fwP==null, "can't make "+nm+" this small: "+sz);
		
		// Compute number of folds and width for NMOS
		int nbSeriesN = 2;
		spaceAvail = nmosTop - (stdCell.getCellBot() + wellOverhangDiff);
		totWid = sz * 3 * nbSeriesN;
		FoldsAndWidth fwN = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 2);
		error(fwN==null, "can't make "+nm+" this small: "+sz);
		
		// create NAND Part
		Cell nand = stdCell.findPart(nm, sz);
		if (nand!=null) return nand;
		nand = stdCell.newPart(nm, sz);
		
		// leave vertical m1 track for inA
		double inaX = 1.5 + 2;		// m1_m1_sp/2 + m1_wid/2
		double pmosX = inaX + 2 + 3 + 2;// m1_wid/2 + m1_m1_sp + diffCont_wid/2
		
		// PMOS
		double pmosY = pmosBot + fwP.physWid/2;
		FoldedMos pmos = new FoldedPmos(pmosX, pmosY, fwP.nbFolds, 1,
										fwP.gateWid, nand, stdCell);
		// NMOS
		double nmosY = nmosTop - fwN.physWid/2;
		
		// Create multiple NMOS FoldedMos.  Each NMOS FoldedMos is a 2
		// high stack.  Each NMOS FoldedMos has 2 folds.
		FoldedMos[] nmoss = new FoldedMos[(int) Math.ceil(fwN.nbFolds/2.0)];
		for (int nbFoldsN=0; nbFoldsN<fwN.nbFolds; nbFoldsN+=2) {
			int nbSeries = 2;
			int nbFolds = 2;
			double nmosPitch = 32;
			double nmosX = pmosX + (nbFoldsN/2)*nmosPitch;
			FoldedMos nmos = new FoldedNmos(nmosX, nmosY, nbFolds, nbSeries,
											fwN.gateWid, nand, stdCell);
			nmoss[nbFoldsN/2] = nmos;
		}
		stdCell.fillDiffAndSelectNotches(nmoss, true);
		
		// create vdd and gnd exports and connect to MOS source/drains
		stdCell.wireVddGnd(nmoss, StdCellParams.EVEN, nand);
		stdCell.wireVddGnd(pmos, StdCellParams.EVEN, nand);
		
//		// fool Electric's NCC into paralleling NMOS stacks by connecting
//		// stacks' internal diffusion nodes.
//		// fool Electric's NCC into paralleling NMOS stacks by connecting
//		// stacks' internal diffusion nodes.
//		for (int i=1; i<nmoss.length; i++) {
//			for (int j=0; j<nmoss[i].nbInternalSrcDrns(); j++) {
//				LayoutLib.newArcInst(Tech.universalArc, 0,
//									 nmoss[0].getInternalSrcDrn(j),
//									 nmoss[i].getInternalSrcDrn(j));
//			}
//		}
		
		// Nand input B
		// m1_wid + m1_space + m1_wid/2
		double inbX = StdCellParams.getRightDiffX(nmoss, pmos) + 2 + 3 + 2;
		LayoutLib.newExport(nand, "inb", PortCharacteristic.IN, Tech.m1,
							4, inbX, inbY);
		TrackRouter inb = new TrackRouterH(Tech.m1, 3, inbY, nand);
		inb.connect(nand.findExport("inb"));
		for (int i=0; i<nmoss.length; i++) {
			inb.connect(nmoss[i].getGate(1, 'T'), Tech.getPolyLShapeOffset());
			inb.connect(nmoss[i].getGate(3, 'T'), -Tech.getPolyLShapeOffset());
		}
		for (int i=0; i<pmos.nbGates(); i++) {
			if (i%4==1) {
				inb.connect(pmos.getGate(i, 'B'), -Tech.getPolyLShapeOffset());
			} else if (i%4==2) {
				inb.connect(pmos.getGate(i, 'B'), .5);
			}
		}
		
		// Nand input A
		LayoutLib.newExport(nand, "ina", PortCharacteristic.IN, Tech.m1,
							4, inaX, inaY);
		TrackRouter inA = new TrackRouterH(Tech.m1, 3, inaY, nand);
		inA.connect(nand.findExport("ina"));
		for (int i=0; i<nmoss.length; i++) {
			for (int j=0; j<nmoss[i].nbGates(); j+=2) {
				inA.connect(nmoss[i].getGate(j, 'T'), -Tech.getPolyLShapeOffset());
			}
		}
		for (int i=0; i<pmos.nbGates(); i++) {
			if (i%4 == 0) {
				inA.connect(pmos.getGate(i, 'B'), -Tech.getPolyLShapeOffset());
			} else if (i%4 == 3) {
				// Last contact may interfere, needlessly, with inB
				double offset = 6.5;
				PortInst g = pmos.getGate(i, 'B');
				double gX = LayoutLib.roundCenterX(g) + offset;
				if (inbX-gX<7)  offset -= 7 - (inbX-gX);
				inA.connect(g, offset, Tech.getPolyLShapeOffset());
			}
		}
		
		// Nand output
		double outX = inbX + 2 + 3 + 2;	// m1_wid/2 + m1_sp + m1_wid/2
		LayoutLib.newExport(nand, "out", PortCharacteristic.OUT, Tech.m1,
							4, outX, outHiY);
		TrackRouter outHi = new TrackRouterH(Tech.m2, 4, outHiY, nand);
		outHi.connect(nand.findExport("out"));
		for (int i=1; i<pmos.nbSrcDrns(); i+=2) {
			outHi.connect(pmos.getSrcDrn(i));
		}
		TrackRouter outLo = new TrackRouterH(Tech.m2, 4, outLoY, nand);
		outLo.connect(nand.findExport("out"));
		for (int i=0; i<nmoss.length; i++) {
			outLo.connect(nmoss[i].getSrcDrn(1));
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

