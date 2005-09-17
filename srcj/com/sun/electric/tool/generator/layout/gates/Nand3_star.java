/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Nand3_star.java
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

class Nand3_star {
	private static final double nmosTop = -9.0;
	private static final double pmosBot = 9.0;
//	private static final double wellOverhangDiff = 6;
	private static final double inaY = -4.0;
	private static final double incY = 4.0;
	private static final double outHiY = 11.0;
	private static final double outLoY = -11.0;
    
	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}
	
	static Cell makePart(double sz, String threshold,
						 StdCellParams stdCell) {
		sz = stdCell.roundSize(sz);
		error(!threshold.equals("") && !threshold.equals("LT") &&
			  !threshold.equals("MLT"),
			  "Nand3: threshold not \"\", \"MLT\", or \"LT\": "+threshold);
		String nm = "nand3" + threshold;
		double lamPerSz = threshold.equals("LT") ? (
            2 // three pullups on at once
        ) : threshold.equals("MLT") ? (
            3 // two pullups on at once
        ) : (
            6 // one pullup on at once
        );
		double minSz = 3/lamPerSz;
		sz = stdCell.checkMinStrength(sz, minSz, nm);
		
		// Compute number of folds and width for PMOS
		double spaceAvail =	 	// p1_p1_sp/2 + p1m1_wid + p1pd_sp
			stdCell.getCellTop() - (1.5 + 5 + 2) - pmosBot;
		double totWid = sz * lamPerSz * 3;	// 3 independent pullups
		FoldsAndWidth fwP = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 3);
		error(fwP==null, "can't make "+nm+" this small: "+sz);
		
		// Compute number of folds and width for NMOS
		int nbStackedN = 3;
		// p1OverhangDiff + p1_p1_sp + p1m1_wid + p1_p1_sp/2
		spaceAvail = nmosTop - (stdCell.getCellBot() + 2 + 3 + 5 + 1.5);
		totWid = sz * 3 * nbStackedN;
		FoldsAndWidth fwN = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 1);
		error(fwN==null, "can't make "+nm+" this small: "+sz);
		
		// create NAND Part
		Cell nand = stdCell.findPart(nm, sz);
		if (nand!=null) return nand;
		nand = stdCell.newPart(nm, sz);
		
		// leave vertical m1 tracks for ina, inb, and ina jog
		double inaX = 1.5 + 2;		// m1_m1_sp/2 + m1_wid/2
		double inbX = inaX + 2 + 3 + 2;	// m1_wid/2 + m1_m1_sp + m1_wid/2
		double jogbX = inbX + 2 + 3 + 2; 	// m1_wid/2 + m1_m1_sp + m1_wid/2
		double nmosX = jogbX + 2 + 3 + 2;//m1_wid/2 + m1_m1_sp + diffCont_wid/2
		
		// NMOS
		double nmosY = nmosTop - fwN.physWid/2;
		FoldedMos nmos = new FoldedNmos(nmosX, nmosY, fwN.nbFolds, nbStackedN,
										fwN.gateWid, nand, stdCell);

		// PMOS Create multiple PMOS ORs.  
		double pmosY = pmosBot + fwP.physWid/2;
		FoldedMos[] pmoss = new FoldedMos[fwP.nbFolds/3];
		for (int i=0; i<pmoss.length; i++) {
			// magic offset makes gate contacts work
			double pmosPitch = 36;
			double pmosX = nmosX - 3.5 + i*pmosPitch;
			pmoss[i] = new FoldedPmos(pmosX, pmosY, 3, 1, fwP.gateWid, nand, stdCell);
		}

		// create vdd and gnd exports and connect to MOS source/drains
		stdCell.wireVddGnd(nmos, StdCellParams.EVEN, nand);
		stdCell.wireVddGnd(pmoss, StdCellParams.EVEN, nand);
		
//		// fool Electric's NCC into paralleling NMOS stacks by connecting
//		// stacks' internal diffusion nodes.
//		for (int i=2; i<nmos.nbInternalSrcDrns(); i+=2) {
//			if ( (i/2) %2 == 0 ) {
//				LayoutLib.newArcInst(Tech.universalArc, 0,
//									 nmos.getInternalSrcDrn(0),
//									 nmos.getInternalSrcDrn(i));
//				LayoutLib.newArcInst(Tech.universalArc, 0,
//									 nmos.getInternalSrcDrn(1),
//									 nmos.getInternalSrcDrn(i+1));
//			} else {
//				LayoutLib.newArcInst(Tech.universalArc, 0,
//									 nmos.getInternalSrcDrn(0),
//									 nmos.getInternalSrcDrn(i+1));
//				LayoutLib.newArcInst(Tech.universalArc, 0,
//									 nmos.getInternalSrcDrn(1),
//									 nmos.getInternalSrcDrn(i));
//			}
//		}
		
		// Nand input C
		TrackRouter inc = new TrackRouterH(Tech.m1, 3, incY, nand);
		for (int i=0; i<nmos.nbGates(); i+=3) {
			if (i/3 % 2 == 0) {
				inc.connect(nmos.getGate(i+2, 'T'), 4, -Tech.getPolyLShapeOffset());
			} else {
				inc.connect(nmos.getGate(i, 'T'), -4, -Tech.getPolyLShapeOffset());
			}
		}
		for (int i=0; i<pmoss.length; i++) {
			inc.connect(pmoss[i].getGate(2, 'B'), Tech.getPolyLShapeOffset());
		}
		// m1_wid + m1_space + m1_wid/2
		double nmosRight = StdCellParams.getRightDiffX(nmos);
		double pmosRight = StdCellParams.getRightDiffX(pmoss);
		double incX = Math.max(nmosRight, pmosRight) + 2 + 3 + 2;
		LayoutLib.newExport(nand, "inc", PortCharacteristic.IN, Tech.m1,
							4, incX, incY);
		inc.connect(nand.findExport("inc"));
		
		// Nand input B
		double gndBot = stdCell.getGndY() - stdCell.getGndWidth()/2;
		double inbLoY = gndBot - 3 - 2;	// -m1_m1_sp -m1_wid/2
		// -polyOverhangDiff - p1_p1_sp -p1m1/2
		inbLoY = Math.min(inbLoY, nmosTop - fwN.physWid - 2 - 3 - 2.5); 
		double spFromVdd =    // vddTop + m1_m1_sp + m1_wid/2
			stdCell.getVddY() + stdCell.getVddWidth()/2 + 3 + 2;
		double spFromPmos =    // pmosTop + pd_p1_sp + p1m1_wid/2
			pmosBot + fwP.physWid + 2 + 2.5;
		double inbHiY = Math.max(spFromVdd, spFromPmos);
		LayoutLib.newExport(nand, "inb", PortCharacteristic.IN, Tech.m1,
							4, inbX, inbHiY);
		TrackRouter inbHi = new TrackRouterH(Tech.m1, 3, inbHiY, nand);
		inbHi.connect(nand.findExport("inb"));
		for (int i=0; i<pmoss.length; i++) {
			inbHi.connect(pmoss[i].getGate(1, 'T'));
		}
		TrackRouter inbLo = new TrackRouterH(Tech.m1, 3, inbLoY, nand);
		inbLo.connect(nand.findExport("inb"));
		for (int i=0; i<nmos.nbGates(); i+=3) {
			inbLo.connect(nmos.getGate(i+1, 'B'));
		}
		
		// Nand input A
		double inaLoY = -11;
		LayoutLib.newExport(nand, "ina", PortCharacteristic.IN, Tech.m1,
							4, inaX, inaLoY);
		TrackRouter inaLo = new TrackRouterH(Tech.m2, 3, inaLoY, nand);
		inaLo.connect(nand.findExport("ina"));
		PortInst jogb = LayoutLib.newNodeInst(Tech.m1pin, jogbX, inaLoY, 3, 3,
											  0, nand).getOnlyPortInst();
		inaLo.connect(jogb);
		
		TrackRouter ina = new TrackRouterH(Tech.m1, 3, inaY, nand);
		ina.connect(jogb);
		for (int i=0; i<nmos.nbGates(); i+=3) {
			if (i/3 % 2 == 0) {
				ina.connect(nmos.getGate(i+0, 'T'), -4, -Tech.getPolyLShapeOffset());
			} else {
				ina.connect(nmos.getGate(i+2, 'T'), 4, -Tech.getPolyLShapeOffset());
			}
		}
		for (int i=0; i<pmoss.length; i++) {
			ina.connect(pmoss[i].getGate(0, 'B'));
		}
		
		// Nand output
		double outX = incX + 2 + 3 + 2;	// m1_wid/2 + m1_sp + m1_wid/2
		LayoutLib.newExport(nand, "out", PortCharacteristic.OUT, Tech.m1,
							4, outX, outHiY);
		TrackRouter outHi = new TrackRouterH(Tech.m2, 4, outHiY, nand);
		outHi.connect(nand.findExport("out"));
		for (int i=0; i<pmoss.length; i++) {
			for (int j=1; j<pmoss[i].nbSrcDrns(); j+=2) {
				outHi.connect(pmoss[i].getSrcDrn(j));	
			}
		}
		TrackRouter outLo = new TrackRouterH(Tech.m2, 4, outLoY, nand);
		outLo.connect(nand.findExport("out"));
		for (int i=1; i<nmos.nbSrcDrns(); i+=2) {
			outLo.connect(nmos.getSrcDrn(i));
		}

        // ============================
        stdCell.fillDiffAndSelectNotches(pmoss, false);
//        StdCellParams.fillSelect(nand, true, false, false);
        // ============================

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

