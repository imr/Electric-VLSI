/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Nand3_star_en_sy3.java
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

class Nand3_star_en_sy3 {
	private static final double DEF_SIZE = LayoutLib.DEF_SIZE;
	private static final double nmosTop = -9.0;
	private static final double pmosBot = 9.0;
	private static final double wellOverhangDiff = 6;
	private static final double incY = -4.0;
	private static final double inbY = 4.0;
	private static final double outHiY = 11.0;
	private static final double outLoY = -11.0;
    
	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

	static Cell makePart(double sz, String threshold,
						 StdCellParams stdCell) {
		sz = stdCell.roundSize(sz);
		error(!threshold.equals("") && !threshold.equals("LT"),
			  "Nand3en_sy3: threshold not \"\" or \"LT\": "+threshold);
		String nm = "nand3" + threshold + "en_sy3";
		
		// Stack the 2 strong PMOSs above the symmetric NMOS stack
		// Compute the number of folds and width for the 2 strong PMOSs.
		double spaceAvail =	 	// p1_p1_sp/2 + p1m1_wid + p1pd_sp
			stdCell.getCellTop() - (1.5 + 5 + 2) - pmosBot;
		double lamPerSz = threshold.equals("LT") ? 2 : 6;
		double totWid = sz * lamPerSz * 2;	// 2 independent pullups
		FoldsAndWidth fwP = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 2);
		error(fwP==null, "can't make "+nm+" this small: "+sz);
		
		// Shift weak PMOS up so the lower edge of it's gate clears any p1m1.
		// Compute the number of folds and width for the weak PMOS.
		totWid = Math.max(5, sz * lamPerSz / 10);
		// p1m1_wid/2 + p1_p1_sp + gateOverhangDiff
		double weakBot = inbY + 2.5 + 3 + 2;
		spaceAvail = spaceAvail + pmosBot - weakBot;
		FoldsAndWidth fwW = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 1);
		error(fwW==null,
			  "can't make "+nm+" weak PMOS should never have a problem");
		
		// Compute number of folds and width for NMOS
		int nbStackedN = 3;
		// p1OverhangDiff + p1_p1_sp + p1m1_wid + p1_p1_sp/2
		spaceAvail = nmosTop - (stdCell.getCellBot() + 2 + 3 + 5 + 1.5);
		totWid = sz * 3 * nbStackedN;
		FoldsAndWidth fwN = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 3);
		error(fwN==null, "can't make "+nm+" this small: "+sz);
		
		// create NAND Part
		Cell nand = stdCell.findPart(nm, sz);
		if (nand!=null) return nand;
		nand = stdCell.newPart(nm, sz);
		
		// leave vertical m1 tracks for ina, inC, and inc jog
		double inbX = 1.5 + 2;		// m1_m1_sp/2 + m1_wid/2
		double incX = inbX + 2 + 3 + 2;	// m1_wid/2 + m1_m1_sp + m1_wid/2
		double jogbX = incX + 2 + 3 + 2;	// m1_wid/2 + m1_m1_sp + m1_wid/2
		double nmosX = jogbX + 2 + 3 + 2;//m1_wid/2 + m1_m1_sp + diffCont_wid/2
		
		// NMOS
		FoldedMos nmos = new FoldedNmos(nmosX, nmosTop - fwN.physWid/2,
										fwN.nbFolds, nbStackedN, fwN.gateWid,
										nand, stdCell);
		// PMOS
		// pmos pitch for 12 folds: 8 * 12 = 96
		// nmos pitch for 6 folds: (3 * 5 + 3) * 6 = 108
		// Create one FoldedMos for every 12 folds.
		// Center 12 pmos folds over 6 nmos folds
		double pmosY = pmosBot + fwP.physWid/2;
		FoldedMos[] pmoss = new FoldedMos[(int) Math.ceil(fwP.nbFolds/12.0)];
		for (int i=0; i<pmoss.length; i++) {
			double pmosPitch = 108;
			double pmosX = nmosX + 6 + i*pmosPitch;
			int nbFolds = Math.min(12, fwP.nbFolds - i*12);
			pmoss[i] = new FoldedPmos(pmosX, pmosY, nbFolds, 1, fwP.gateWid,
									  nand, stdCell);
		}
		
		// Create one FoldedMos for weak PMOS
		double weakX = StdCellParams.getRightDiffX(pmoss) + 12;// for now don't share
		double weakY = weakBot + fwW.physWid/2;
		FoldedMos weak = new FoldedPmos(weakX, weakY, fwW.nbFolds, 1,
										fwW.gateWid, nand, stdCell);
		
		// create an array that holds all PMOS, strong and weak
		FoldedMos[] stroWeakPmoss = new FoldedMos[pmoss.length+1];
		for (int i=0; i<pmoss.length; i++) {
			stroWeakPmoss[i] = pmoss[i];
		}
		stroWeakPmoss[pmoss.length] = weak;
		
		// create vdd and gnd exports and connect to MOS source/drains
		stdCell.wireVddGnd(nmos, StdCellParams.EVEN, nand);
		stdCell.wireVddGnd(stroWeakPmoss, StdCellParams.EVEN, nand);
		
		// fool Electric's NCC into paralleling NMOS stacks by connecting
		// stacks' internal diffusion nodes.
		for (int i=6; i<nmos.nbInternalSrcDrns(); i+=6) {
			for (int j=0; j<6; j++) {
				if (i/6 % 2 == 0) {
					LayoutLib.newArcInst(Tech.universalArc, 0,
										 nmos.getInternalSrcDrn(j),
										 nmos.getInternalSrcDrn(i+j));
				} else {
					LayoutLib.newArcInst(Tech.universalArc, 0,
										 nmos.getInternalSrcDrn(j),
										 nmos.getInternalSrcDrn(i+(5-j)));
				}
			}
		}
		
		// Nand input B
		double inbHiY = 11;
		LayoutLib.newExport(nand, "inb", PortCharacteristic.IN, Tech.m1,
							4, inbX, inbHiY);
		TrackRouter inbHi = new TrackRouterH(Tech.m2, 3, inbHiY, nand);
		inbHi.connect(nand.findExport("inb"));
		PortInst jogb = LayoutLib.newNodeInst(Tech.m1pin, jogbX, inbHiY,
											  DEF_SIZE, DEF_SIZE, 0, nand
											  ).getOnlyPortInst();
		inbHi.connect(jogb);
		
		TrackRouter inbLo = new TrackRouterH(Tech.m1, 3, inbY, nand);
		inbLo.connect(jogb);
		for (int i=0; i<fwN.nbFolds; i++) {
			switch (i%6) {
			case 0: inbLo.connect(nmos.getGate(i*3+0, 'T'), 1.5);  break;
			case 1: inbLo.connect(nmos.getGate(i*3+1, 'T'), -1);  break;
			case 2: inbLo.connect(nmos.getGate(i*3+2, 'T'));  break;
			case 3: inbLo.connect(nmos.getGate(i*3+0, 'T'));  break;
			case 4: inbLo.connect(nmos.getGate(i*3+1, 'T'), 1);  break;
			case 5: inbLo.connect(nmos.getGate(i*3+2, 'T'), -1.5);  break;
			}
		}
		
		for (int i=0; i<pmoss.length; i++) {
			for (int j=0; j<pmoss[i].nbGates(); j++) {
				switch (j%12) {
				case 0: inbLo.connect(pmoss[i].getGate(j, 'B'), -4.5, 1.5);
					break;
				case 2: inbLo.connect(pmoss[i].getGate(j, 'B')); break;
				case 5: inbLo.connect(pmoss[i].getGate(j, 'B')); break;
				case 6: inbLo.connect(pmoss[i].getGate(j, 'B')); break;
				case 9: inbLo.connect(pmoss[i].getGate(j, 'B')); break;
				case 11: inbLo.connect(pmoss[i].getGate(j, 'B'), 4.5, 1.5);
					break;
				}
			}
		}
		
		// Nand input A
		// m1_wid + m1_space + m1_wid/2
		double inaX = StdCellParams.getRightDiffX(nmos) + 2 + 3 + 2;
		double gndBot = stdCell.getGndY() - stdCell.getGndWidth()/2;
		double inaLoY = gndBot - 3 - 2;	// -m1_m1_sp -m1_wid/2
		// -polyOverhangDiff - p1_p1_sp -p1m1/2
		double nmosBot = nmosTop - fwN.physWid;
		inaLoY = Math.min(inaLoY, nmosBot - 2 - 3 - 2.5); 
		double vddTop = stdCell.getVddY() + stdCell.getVddWidth()/2;
		double weakTop = weakBot + fwW.physWid;
		// -m1_m1_sp -m1_wid/2
		double inaHiY = Math.max(vddTop, weakTop) + 3 + 2;
		LayoutLib.newExport(nand, "ina", PortCharacteristic.IN, Tech.m1,
							4, inaX, inaHiY);
		TrackRouter inaHi = new TrackRouterH(Tech.m1, 3, inaHiY, nand);
		inaHi.connect(nand.findExport("ina"));
		for (int i=0; i<weak.nbGates(); i++) {
			inaHi.connect(weak.getGate(i, 'T'));
		}
		TrackRouter inaLo = new TrackRouterH(Tech.m1, 3, inaLoY, nand);
		inaLo.connect(nand.findExport("ina"));
		for (int i=0; i<fwN.nbFolds; i++) {
			switch (i%6) {
			case 0: inaLo.connect(nmos.getGate(i*3+1, 'B')); break;
			case 1: inaLo.connect(nmos.getGate(i*3+0, 'B')); break;
			case 2: inaLo.connect(nmos.getGate(i*3+0, 'B')); break;
			case 3: inaLo.connect(nmos.getGate(i*3+2, 'B')); break;
			case 4: inaLo.connect(nmos.getGate(i*3+2, 'B')); break;
			case 5: inaLo.connect(nmos.getGate(i*3+1, 'B')); break;
			}
		}
		
		// Nand input C
		TrackRouter inc = new TrackRouterH(Tech.m1, 3, incY, nand);
		for (int i=0; i<fwN.nbFolds; i++) {
			switch (i%6) {
			case 0: inc.connect(nmos.getGate(i*3+2, 'T'), 1.5); break;
			case 1: inc.connect(nmos.getGate(i*3+2, 'T'), 1.5);break;
			case 2: inc.connect(nmos.getGate(i*3+1, 'T'), -11.5, 1);break;
			case 3: inc.connect(nmos.getGate(i*3+1, 'T'), 11.5, 1);break;
			case 4: inc.connect(nmos.getGate(i*3+0, 'T'), -1.5);break;
			case 5: inc.connect(nmos.getGate(i*3+0, 'T'), -1.5); break;
			}
		}
		for (int i=0; i<pmoss.length; i++) {
			for (int j=0; j<pmoss[i].nbGates(); j++) {
				switch (j%12) {
				case 1: inc.connect(pmoss[i].getGate(j, 'B'), -2.5, 3.5);
					break;
				case 3: inc.connect(pmoss[i].getGate(j, 'B'), -.5); break;
				case 4: inc.connect(pmoss[i].getGate(j, 'B'), -8.5, 3); break;
				case 7: inc.connect(pmoss[i].getGate(j, 'B'), 8.5, 3); break;
				case 8: inc.connect(pmoss[i].getGate(j, 'B'), .5); break;
				case 10: inc.connect(pmoss[i].getGate(j, 'B'), 2.5, 3.5);
					break;
				}
			}
		}
		
		LayoutLib.newExport(nand, "inc", PortCharacteristic.IN, Tech.m1,
							4, incX, incY);
		inc.connect(nand.findExport("inc"));
		
		// Nand output
		double outX = inaX + 2 + 3 + 2;	// m1_wid/2 + m1_sp + m1_wid/2
		LayoutLib.newExport(nand, "out", PortCharacteristic.OUT, Tech.m1,
							4, outX, outHiY);
		TrackRouter outHi = new TrackRouterH(Tech.m2, 4, outHiY, nand);
		outHi.connect(nand.findExport("out"));
		for (int i=0; i<stroWeakPmoss.length; i++) {
			for (int j=1; j<stroWeakPmoss[i].nbSrcDrns(); j+=2) {
				outHi.connect(stroWeakPmoss[i].getSrcDrn(j));	
			}
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
		// RKao fixme.  NCC with "wrong" schematic
		stdCell.doNCC(nand, nm+"{sch}");
		
		return nand;
	}
}

