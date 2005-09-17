/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Nand2en_sy.java
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

public class Nand2en_sy {
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
		String nm = "nand2en_sy";
		sz = stdCell.checkMinStrength(sz, 1, nm);
		
		// compute number of folds and width for full strength PMOS
		double spaceAvail = stdCell.getCellTop() - wellOverhangDiff - pmosBot;
		double lamPerSz = 6;
		double totWid = sz * lamPerSz;
		FoldsAndWidth fwP = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 1);
		error(fwP==null, "can't make "+nm+" this small: "+sz);
		
		// Compute number of folds and width for weak PMOS.
		// Don't let transistor size drop below 5 lambda.
		totWid =
			Math.max(3, sz * lamPerSz * stdCell.getEnableGateStrengthRatio());
		FoldsAndWidth fwW = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 1);
		
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
		
		// leave vertical m1 track for inb
		double inbX = 1.5 + 2;		// m1_m1_sp/2 + m1_wid/2
		double nmosX = inbX + 2 + 3 + 2;// m1_wid/2 + m1_m1_sp + diffCont_wid/2
		
		// NMOS
		double nmosY = nmosTop - fwN.physWid/2;
		int nbSeries = 2;
		FoldedMos nmos = new FoldedNmos(nmosX, nmosY, fwN.nbFolds, nbSeries,
										fwN.gateWid, nand, stdCell);
		
		// Create multiple FoldedPmos.  Each FoldedPmos has, at most, 2
		// folds.
		FoldedMos[] pmoss = new FoldedMos[(int) Math.ceil(fwP.nbFolds/2.0)];
		double pmosY = pmosBot + fwP.physWid/2;
		for (int nbFoldsP=0; nbFoldsP<fwP.nbFolds; nbFoldsP+=2) {
			int nbFolds = Math.min(2, fwP.nbFolds-nbFoldsP);
			nbSeries = 1;
			double pmosPitch = 2 * 13;
			// pmos is shifted right by 2 lambda to allow weak PMOS to share drain
			double pmosX = nmosX + 2 + pmosPitch * (nbFoldsP/2);
			pmoss[nbFoldsP/2] = new FoldedPmos(pmosX, pmosY, nbFolds, nbSeries,
											   fwP.gateWid, nand, stdCell);
		}
		stdCell.fillDiffAndSelectNotches(pmoss, true);
		
		// Create weak PMOS
		double rightDiffX = StdCellParams.getRightDiffX(pmoss, nmos);
		// unrelated diffusion pitch is 8.5 lambda
		double weakX = rightDiffX + 8.5;
		double weakY = pmosBot + fwW.physWid/2;
		FoldedMos weakPmos = new FoldedPmos(weakX, weakY, fwW.nbFolds, 1,
											fwW.gateWid, nand, stdCell);
		// create an array that holds all PMOS, strong and weak
		FoldedMos[] stroWeakPmoss = new FoldedMos[pmoss.length+1];
		for (int i=0; i<pmoss.length; i++) {
			stroWeakPmoss[i] = pmoss[i];
		}
		stroWeakPmoss[pmoss.length] = weakPmos;
		
		// create vdd and gnd exports and connect to MOS source/drains
		stdCell.wireVddGnd(nmos, StdCellParams.EVEN, nand);
		stdCell.wireVddGnd(stroWeakPmoss, StdCellParams.EVEN, nand);
		
//		// fool Electric's NCC into paralleling NMOS stacks by connecting
//		// stacks' internal diffusion nodes.
//		// fool Electric's NCC into paralleling NMOS stacks by connecting
//		// stacks' internal diffusion nodes.
//		for (int i=2; i<nmos.nbInternalSrcDrns(); i++) {
//			LayoutLib.newArcInst(Tech.universalArc, 0,
//								 nmos.getInternalSrcDrn(i%2),
//								 nmos.getInternalSrcDrn(i));
//		}
		
		// Nand input B
		LayoutLib.newExport(nand, "inb", PortCharacteristic.IN, Tech.m1,
							4, inbX, inbY);
		TrackRouter inb = new TrackRouterH(Tech.m1, 3, inbY, nand);
		inb.connect(nand.findExport("inb"));
		
		for (int i=0; i<nmos.nbGates(); i++) {
			switch (i%4) {
			case 0: inb.connect(nmos.getGate(i, 'T'));  break;
			case 2: inb.connect(nmos.getGate(i, 'T'), -1.5);  break;
			}
		}
		for (int i=0; i<pmoss.length; i++) {
			for (int j=0; j<pmoss[i].nbGates(); j++) {
				switch (j) {
				case 0:	inb.connect(pmoss[i].getGate(j, 'B'), -2);  break;
				case 1:	inb.connect(pmoss[i].getGate(j, 'B'), 1.5);  break;
				}
			}
		}
		
		// Nand input A
		// m1_wid + m1_space + m1_wid/2
		double inaX = StdCellParams.getRightDiffX(weakPmos, nmos) + 2 + 3 + 2;
		LayoutLib.newExport(nand, "ina", PortCharacteristic.IN, Tech.m1,
							4, inaX, inaY);
		TrackRouter ina = new TrackRouterH(Tech.m1, 3, inaY, nand);
		ina.connect(nand.findExport("ina"));
		for (int i=0; i<nmos.nbGates(); i++) {
			if (i%2 == 1) ina.connect(nmos.getGate(i, 'T'), 1.5);
		}
		for (int i=0; i<weakPmos.nbGates(); i++) {
			ina.connect(weakPmos.getGate(i, 'B'), 1.5);
		}
		
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
		stdCell.doNCC(nand, nm+"{sch}");
		
		return nand;
	}
}

