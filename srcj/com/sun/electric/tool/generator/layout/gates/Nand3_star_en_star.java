/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Nand3_star_en_star.java
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

class Nand3_star_en_star {
	private static final double nmosTop = -11.5;
	private static final double pmosBot = 9.0;
//	private static final double wellOverhangDiff = 6;
	private static final double inbY = -4.0;
	private static final double incY = 4.0;
	private static final double outHiY = 11.0;
	private static final double outLoY = -11.0;
    
	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

	private static  void connectIncSymmetric(TrackRouter incLo, FoldedMos nmos,
											 FoldedMos[] pmoss) {
		for (int i=0; i<nmos.nbGates()/3; i++) {
			int dPort = 0;
			double dx = 0;
			switch (i%4) {
			case 0: dPort=2; dx=-Tech.getPolyLShapeOffset(); break;
			case 1: dPort=1; dx=-0.5; break;
			case 2: dPort=1; dx=-Tech.getPolyLShapeOffset(); break;
			case 3: dPort=0; dx= 0.5; break;
			}
			incLo.connect(nmos.getGate(i*3+dPort, 'T'), dx);
		}
		for (int i=0; i<pmoss.length; i++) {
			for (int j=0; j<pmoss[i].nbGates(); j++) {
				double dx = 0;
				boolean con = true;
				switch (j) {
				case 1: dx= 0.5; break;
				case 3: dx=-Tech.getPolyLShapeOffset(); break;
				case 5: dx=-0.5; break;
				case 7: dx=-Tech.getPolyLShapeOffset(); break;
				default: con = false;
				}
				if (con) incLo.connect(pmoss[i].getGate(j, 'B'), dx);
			}
		}
	}
	private static void connectIncAsymmetric(TrackRouter incLo, FoldedMos nmos,
											 FoldedMos[] pmoss) {
		for (int i=0; i<nmos.nbGates()/3; i++) {
			int dPort = 0;
			double dx=0, dy=0;
			switch (i%4) {
			case 0: dPort=2; dx=-Tech.getPolyLShapeOffset(); dy= 0.0; break;
			case 1: dPort=0; dx=-Tech.getPolyLShapeOffset(); dy= 0.0; break;
			case 2: dPort=2; dx= Tech.getPolyTShapeOffset(); dy=-Tech.getPolyTShapeOffset(); break;
			case 3: dPort=0; dx=-4.5; dy=-Tech.getPolyTShapeOffset(); break;
			}
			incLo.connect(nmos.getGate(i*3+dPort, 'T'), dx, dy);
		}
		for (int i=0; i<pmoss.length; i++) {
			for (int j=0; j<pmoss[i].nbGates(); j++) {
				double dx = 0;
				boolean con = true;
				switch (j) {
				case 1: dx= 0.5; break;
				case 2: dx= 0.5; break;
				case 4: dx= 0.0; break;
				case 6: dx= Tech.getPolyLShapeOffset(); break;
				default: con = false;
				}
				if (con) incLo.connect(pmoss[i].getGate(j, 'B'), dx);
			}
		}
	}
	
	private static  void connectInbSymmetric(TrackRouter inb, FoldedMos nmos,
											 FoldedMos[] pmoss) {
		for (int i=0; i<nmos.nbGates()/3; i++) {
			int dPort = 0;
			double dx = 0;
			switch (i%4) {
			case 0: dPort=1; dx=-Tech.getPolyLShapeOffset(); break;
			case 1: dPort=0; dx=-Tech.getPolyLShapeOffset(); break;
			case 2: dPort=2; dx= Tech.getPolyLShapeOffset(); break;
			case 3: dPort=1; dx= Tech.getPolyLShapeOffset(); break;
			}
			inb.connect(nmos.getGate(i*3+dPort, 'T'), dx);
		}
		for (int i=0; i<pmoss.length; i++) {
			for (int j=0; j<pmoss[i].nbGates(); j++) {
				double dx = 0;
				boolean con = true;
				switch (j) {
				case 0: dx= Tech.getPolyTShapeOffset(); break;
				case 2: dx= 0.5; break;
				case 4: dx= 0.0; break;
				case 6: dx=-0.5; break;
				default: con = false;
				}
				if (con) inb.connect(pmoss[i].getGate(j, 'B'), dx, Tech.getPolyLShapeOffset());
			}
		}
	}
	
	private static void connectInbAsymmetric(TrackRouter inb, FoldedMos nmos,
											 FoldedMos[] pmoss) {
		for (int i=0; i<nmos.nbGates()/3; i++) {
			int dPort = 0;
			double dx = 0;
			switch (i%4) {
			case 0: dPort=1; dx=-Tech.getPolyLShapeOffset(); break;
			case 1: dPort=1; dx= Tech.getPolyLShapeOffset(); break;
			case 2: dPort=1; dx=-Tech.getPolyLShapeOffset(); break;
			case 3: dPort=1; dx= Tech.getPolyLShapeOffset(); break;
			}
			inb.connect(nmos.getGate(i*3+dPort, 'T'), dx);
		}
		for (int i=0; i<pmoss.length; i++) {
			for (int j=0; j<pmoss[i].nbGates(); j++) {
				double dx=0, dy=0;
				boolean con = true;
				switch (j) {
				case 0: dx= Tech.getPolyTShapeOffset(); dy= 0.0; break;
				case 3: dx= 0.5; dy= 0.0; break;
				case 5: dx=-0.5; dy= 0.0; break;
				case 7: dx= 4.5; dy= 9.5; break;
				default: con = false;
				}
				if (con) inb.connect(pmoss[i].getGate(j, 'B'), dx, dy);
			}
		}
	}
	
//	// fool Electric's NCC into paralleling NMOS stacks by connecting
//	// stacks' internal diffusion nodes.
//	private static void universalArcSymmetric(FoldedMos nmos) {
//		// First do the source/drain just above (in the schematic) ground.
//		for (int i=2; i<nmos.nbInternalSrcDrns(); i+=2) {
//			LayoutLib.newArcInst(Tech.universalArc, 0,
//								 nmos.getInternalSrcDrn(0),
//								 nmos.getInternalSrcDrn(i+(i/2 %2)));
//		}
//		// Then do the source/drain just below (in the schematic) the output.
//		for (int i=4; i<nmos.nbInternalSrcDrns(); i+=4) {
//			int d1 = (i/4 % 2)==0 ? 1 : 2;
//			int d2 = (i/4 % 2)==0 ? 2 : 1;
//			LayoutLib.newArcInst(Tech.universalArc, 0,
//								 nmos.getInternalSrcDrn(1),
//								 nmos.getInternalSrcDrn(i+d1));
//			LayoutLib.newArcInst(Tech.universalArc, 0,
//								 nmos.getInternalSrcDrn(2),
//								 nmos.getInternalSrcDrn(i+d2));
//		}
//	}
//	// fool Electric's NCC into paralleling NMOS stacks by connecting
//	// stacks' internal diffusion nodes.
//	private static void universalArcAsymmetric(FoldedMos nmos) {
//		for (int i=2; i<nmos.nbInternalSrcDrns(); i+=2) {
//			// The source/drain just above (in the schematic) ground.
//			LayoutLib.newArcInst(Tech.universalArc, 0,
//								 nmos.getInternalSrcDrn(0),
//								 nmos.getInternalSrcDrn(i+(i/2 %2)));
//			// The source/drain just below (in the schematic) output.
//			LayoutLib.newArcInst(Tech.universalArc, 0,
//								 nmos.getInternalSrcDrn(1),
//								 nmos.getInternalSrcDrn(i+((i/2+1) %2)));
//		}
//	}
	
	static Cell makePart(double sz, String threshold, String symmetry,
						 StdCellParams stdCell) {
		sz = stdCell.roundSize(sz);
		error(!threshold.equals("") && !threshold.equals("LT"),
			  "Nand3en: threshold not \"\" or \"LT\": "+threshold);
		error(!symmetry.equals("") && !symmetry.equals("SY"),
			  "Nand3en: symmetry not \"\" or \"SY\": "+symmetry);
		
		String nm = "nand3" + threshold + "en" +
			(symmetry.equals("SY") ? "_sy" : "");
		
		double nmosMinSz = 1./3 * (symmetry.equals("SY") ? 2 : 1);
		double pmosMinSz = threshold.equals("LT") ? 3./3 : 3./6;
		sz = stdCell.checkMinStrength(sz, Math.max(nmosMinSz, pmosMinSz), nm);
		
		// COMPUTE NUMBER of folds and width for PMOS
		double spaceAvail =	 	// wellOverhangDiff
			stdCell.getCellTop() - 6 - pmosBot;
		double lamPerSz = threshold.equals("LT") ? 3 : 6;
		double totWid = sz * lamPerSz * 2;	// 2 independent pullups
		FoldsAndWidth fwP = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 2);
		error(fwP==null, "can't make "+nm+" this small: "+sz);
		
		// Compute number of folds and width for NMOS
		int nbStackedN = 3;
		// nd_p1_sp + p1m1_wid + p1_p1_sp/2
		spaceAvail = nmosTop - (stdCell.getCellBot() + 2 + 5 + 1.5);
		totWid = sz * 3 * nbStackedN;
		int grpSz = symmetry.equals("SY") ? 2 : 1;
		FoldsAndWidth fwN = stdCell.calcFoldsAndWidth(spaceAvail, totWid,grpSz);
		error(fwN==null, "can't make "+nm+" this small: "+sz);
		
		// create NAND Part
		Cell nand = stdCell.findPart(nm, sz);
		if (nand!=null) return nand;
		nand = stdCell.newPart(nm, sz);
		
		// leave vertical m1 tracks for ina, inc, and inb jog
		double incX = 1.5 + 2;		// m1_m1_sp/2 + m1_wid/2
		double inbX = incX + 2 + 3 + 2;	// m1_wid/2 + m1_m1_sp + m1_wid/2
		double jogcX = inbX + 2 + 3 + 2;	// m1_wid/2 + m1_m1_sp + m1_wid/2
		double mosX = jogcX + 2 + 3 + 2;// m1_wid/2 + m1_m1_sp + diffCont_wid/2
		
		// NMOS
		FoldedMos nmos =
			new FoldedNmos(mosX, nmosTop - fwN.physWid/2, fwN.nbFolds,
						   nbStackedN, fwN.gateWid, nand, stdCell);
		// PMOS
		// pmos pitch for 8 folds: 8 * 8 = 64
		// nmos pitch for 4 folds: 4 * 18 = 72
		// Create one FoldedMos for every 8 folds.
		// Align left diffusions of first NMOS and PMOS
		double pmosY = pmosBot + fwP.physWid/2;
		FoldedMos[] pmoss = new FoldedMos[(int) Math.ceil(fwP.nbFolds/8.0)];
		for (int i=0; i<pmoss.length; i++) {
			double pmosPitch = 72;
			int nbFolds = Math.min(8, fwP.nbFolds - i*8);
			pmoss[i] = new FoldedPmos(mosX + i*pmosPitch, pmosY, nbFolds, 1,
									  fwP.gateWid, nand, stdCell);
		}
		// Fill select notch between foldedmos
        stdCell.fillDiffAndSelectNotches(pmoss, false);

		// Drop down a single PMOS pullup for ina
		double rightPdiffX = StdCellParams.getRightDiffX(pmoss);
		double rightNdiffX = StdCellParams.getRightDiffX(nmos);
		// pdm1_wid/2 + selOverhangDiff + sel_sel_sp + selOverhangDiff + pdm1_wid/2
		double pmosaFromPmos = rightPdiffX + 2.5 + 2 + 2 + 2 + 2.5;
		double pmosaFromNmos = rightNdiffX - 8;
		double pmosaX = Math.max(pmosaFromPmos, pmosaFromNmos);
		FoldedMos pmosa = new FoldedPmos(pmosaX, stdCell.getVddY(), 1, 1, 5,
										 nand, stdCell);

        // Fill select notch betweeb pmosa and last pmos
        stdCell.fillDiffAndSelectNotches(new FoldedMos[]{pmoss[pmoss.length-1], pmosa}, false);

		// create vdd and gnd exports and connect to MOS source/drains
		stdCell.wireVddGnd(nmos, StdCellParams.EVEN, nand);
		stdCell.wireVddGnd(pmoss, StdCellParams.EVEN, nand);
		stdCell.wireVddGnd(new FoldedMos[] {pmoss[0], pmosa},
						   StdCellParams.EVEN, nand);
		
//		if (symmetry.equals("SY")) {
//			universalArcSymmetric(nmos);
//		} else {
//			universalArcAsymmetric(nmos);
//		}
		
		// Nand input C
		double incHiY = 11;
		LayoutLib.newExport(nand, "inc", PortCharacteristic.IN, Tech.m1,
							4, incX, incHiY);
		TrackRouter incHi = new TrackRouterH(Tech.m2, 3, incHiY, nand);
		incHi.connect(nand.findExport("inc"));
		PortInst jogc = LayoutLib.newNodeInst(Tech.m1pin, jogcX, incHiY, 3, 3,
											  0, nand).getOnlyPortInst();
		incHi.connect(jogc);
		
		TrackRouter incLo = new TrackRouterH(Tech.m1, 3, incY, nand);
		incLo.connect(jogc);
		if (symmetry.equals("SY")) {
			connectIncSymmetric(incLo, nmos, pmoss);
		} else {
			connectIncAsymmetric(incLo, nmos, pmoss);
		}
		
		// Nand input B
		TrackRouter inb = new TrackRouterH(Tech.m1, 3, inbY, nand);
		LayoutLib.newExport(nand, "inb", PortCharacteristic.IN, Tech.m1,
							4, inbX, inbY);
		inb.connect(nand.findExport("inb"));
		if (symmetry.equals("SY")) {
			connectInbSymmetric(inb, nmos, pmoss);
		} else {
			connectInbAsymmetric(inb, nmos, pmoss);
		}
		
		// Nand input A
		// above Vdd power rail
		// m1_wid/2 + m1_m1_sp + m1_wid/2
		double inaX = LayoutLib.roundCenterX(pmosa.getSrcDrn(1)) + 2 + 3 + 2;
		double inaHiY = stdCell.getVddY() + stdCell.getVddWidth()/2 +
			3 + 2;	// m1_m1_sp + m1_wid/2
		LayoutLib.newExport(nand, "ina", PortCharacteristic.IN, Tech.m1,
							4, inaX, inaHiY);
		TrackRouter inaHi = new TrackRouterH(Tech.m1, 3, inaHiY, nand);
		inaHi.connect(nand.findExport("ina"));
		inaHi.connect(pmosa.getGate(0, 'T'), Tech.getPolyLShapeOffset());
		
		// bottom of cell
		double gndBot = stdCell.getGndY() - stdCell.getGndWidth()/2;
		double inaFromGnd = gndBot - 3 - 2;		// -m1_m1_sp -m1_wid/2
		double nmosBot = nmosTop - fwN.physWid;
		double inaFromMos = nmosBot - 2 -2.5;	// -nd_p1_sp - p1m1_wid/2
		double inaLoY = Math.min(inaFromGnd, inaFromMos); 
		
		TrackRouter inaLo = new TrackRouterH(Tech.m1, 3, inaLoY, nand);
		for (int i=0; i<fwN.nbFolds; i++) {
			int dPort=0;
			double dx=0;
			switch (i%2) {
			case 0: dPort=0; dx=-4.0; break;
			case 1: dPort=2; dx= 4.0; break;
			}
			inaLo.connect(nmos.getGate(i*3+dPort, 'B'), dx, Tech.getPolyLShapeOffset());
		}
		inaLo.connect(nand.findExport("ina"));
		
		// Nand output
		double outX = inaX + 2 + 3 + 2;	// m1_wid/2 + m1_sp + m1_wid/2
		LayoutLib.newExport(nand, "out", PortCharacteristic.OUT, Tech.m1,
							4, outX, outHiY);
		TrackRouter outHi = new TrackRouterH(Tech.m2, 4, outHiY, nand);
		outHi.connect(nand.findExport("out"));
		for (int i=0; i<pmoss.length; i++) {
			for (int j=1; j<pmoss[i].nbSrcDrns(); j+=2) {
				outHi.connect(pmoss[i].getSrcDrn(j));	
			}
		}
		outHi.connect(pmosa.getSrcDrn(1));
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

