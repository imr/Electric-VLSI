/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Nms3_sy3.java
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
import com.sun.electric.tool.generator.layout.FoldsAndWidth;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.StdCellParams;
import com.sun.electric.tool.generator.layout.Tech;
import com.sun.electric.tool.generator.layout.TrackRouter;
import com.sun.electric.tool.generator.layout.TrackRouterH;

public class Nms3_sy3 {
	private static final double nmosTop = -9.0;
//	private static final double wellOverhangDiff = 6;
	private static final double incY = -4.0;
//	private static final double inaY = 4.0;
	private static final double outY = -11.0;

	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

	public static Cell makePart(double sz, StdCellParams stdCell) {
		sz = stdCell.roundSize(sz);
		String nm = "nms3_sy3";
		sz = stdCell.checkMinStrength(sz, 1, nm);

		// Compute number of folds and width for NMOS
		int nbStackedN = 3;
		double spaceAvail = nmosTop - (stdCell.getCellBot() +
			// p1OverhangDiff + p1_p1_sp + p1m1_wid + p1_p1_sp + p1m1_wid + p1_p1_sp/2
	2 + 3 + 5 + 3 + 5 + 1.5);
		double totWid = sz * 3 * nbStackedN;
		FoldsAndWidth fwN = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 3);
		error(fwN == null, "can't make " + nm + " this small: " + sz);

		// create NAND Part
		Cell nand = stdCell.findPart(nm, sz);
		if (nand != null)
			return nand;
		nand = stdCell.newPart(nm, sz);

		// leave vertical m1 tracks for inB, inC, and inc jog
		double inaX = 1.5 + 2; // m1_m1_sp/2 + m1_wid/2
		double inbX = inaX + 2 + 3 + 2; // m1_wid/2 + m1_m1_sp + m1_wid/2
		double jogaX = inbX + 2 + 3 + 2; // m1_wid/2 + m1_m1_sp + m1_wid/2
		double nmosX = jogaX + 2 + 3 + 2;
		// m1_wid/2 + m1_m1_sp + diffCont_wid/2

		// NMOS
		FoldedMos nmos =
			new FoldedNmos(
				nmosX,
				nmosTop - fwN.physWid / 2,
				fwN.nbFolds,
				nbStackedN,
				fwN.gateWid,
				nand, stdCell);

		// create vdd and gnd exports and connect to MOS source/drains
		stdCell.wireVddGnd(nmos, StdCellParams.EVEN, nand);

//		// fool Electric's NCC into paralleling NMOS stacks by connecting
//		// stacks' internal diffusion nodes.
//		for (int i = 6; i < nmos.nbInternalSrcDrns(); i += 6) {
//			for (int j = 0; j < 6; j++) {
//				if (i/6 % 2 == 0) {
//					LayoutLib.newArcInst(Tech.universalArc, 0,
//										 nmos.getInternalSrcDrn(j),
//										 nmos.getInternalSrcDrn(i+j));
//				} else {
//					LayoutLib.newArcInst(Tech.universalArc, 0,
//										 nmos.getInternalSrcDrn(j),
//										 nmos.getInternalSrcDrn(i+(5-j)));
//				}
//			}
//		}

		// Nand input A
		double inaHiY = -11;
		LayoutLib.newExport(nand, "ina", PortCharacteristic.IN, Tech.m1,
							4, inaX, inaHiY);
		TrackRouter inaHi = new TrackRouterH(Tech.m2, 3, inaHiY, nand);
		inaHi.connect(nand.findExport("ina"));
		PortInst joga =
			LayoutLib.newNodeInst(Tech.m1pin, jogaX, inaHiY, 3, 3, 0, nand
								  ).getOnlyPortInst();
		inaHi.connect(joga);

		double gndBot = stdCell.getGndY() - stdCell.getGndWidth() / 2;
		double spFromGnd = gndBot - 3 - 2; // -m1_m1_sp -m1_wid/2
		// -polyOverhangDiff - p1_p1_sp -p1m1/2
		double nmosBot = nmosTop - fwN.physWid;
		double inaLoY = Math.min(spFromGnd, nmosBot - 2 - 3 - 2.5);
		TrackRouter inaLo = new TrackRouterH(Tech.m1, 3, inaLoY, nand);
		inaLo.connect(joga);
		for (int i = 0; i < fwN.nbFolds; i++) {
			switch (i % 6) {
				case 0 :
					inaLo.connect(nmos.getGate(i * 3 + 0, 'B'), -4, Tech.getPolyLShapeOffset());
					break;
				case 1 :
					inaLo.connect(nmos.getGate(i * 3 + 1, 'B'), Tech.getPolyLShapeOffset());
					break;
				case 2 :
					inaLo.connect(nmos.getGate(i * 3 + 2, 'B'));
					break;
				case 3 :
					inaLo.connect(nmos.getGate(i * 3 + 0, 'B'));
					break;
				case 4 :
					inaLo.connect(nmos.getGate(i * 3 + 1, 'B'), -Tech.getPolyLShapeOffset());
					break;
				case 5 :
					inaLo.connect(nmos.getGate(i * 3 + 2, 'B'), 4, Tech.getPolyLShapeOffset());
					break;
			}
		}

		// Nand input B
		double inbY = inaLoY - 8; // poly contact pitch
		LayoutLib.newExport(nand, "inb", PortCharacteristic.IN, Tech.m1,
							4, inbX, inbY);
		TrackRouter inbLo = new TrackRouterH(Tech.m1, 3, inbY, nand);
		inbLo.connect(nand.findExport("inb"));
		for (int i = 0; i < fwN.nbFolds; i++) {
			switch (i % 6) {
				case 0 :
					inbLo.connect(nmos.getGate(i * 3 + 1, 'B'));
					break;
				case 1 :
					inbLo.connect(nmos.getGate(i * 3 + 0, 'B'));
					break;
				case 2 :
					inbLo.connect(nmos.getGate(i * 3 + 0, 'B'));
					break;
				case 3 :
					inbLo.connect(nmos.getGate(i * 3 + 2, 'B'));
					break;
				case 4 :
					inbLo.connect(nmos.getGate(i * 3 + 2, 'B'));
					break;
				case 5 :
					inbLo.connect(nmos.getGate(i * 3 + 1, 'B'));
					break;
			}
		}

		// Nand input C
		TrackRouter inc = new TrackRouterH(Tech.m1, 3, incY, nand);
		for (int i = 0; i < fwN.nbFolds; i++) {
			switch (i % 6) {
			case 0: inc.connect(nmos.getGate(i*3+2, 'T'), 1.5);  break;
			case 1: inc.connect(nmos.getGate(i*3+2, 'T'), 1.5);  break;
			case 2: inc.connect(nmos.getGate(i*3+1, 'T'), -11.5, 1); break;
			case 3: inc.connect(nmos.getGate(i*3+1, 'T'), 11.5, 1); break;
			case 4: inc.connect(nmos.getGate(i*3+0, 'T'), -1.5); break;
			case 5: inc.connect(nmos.getGate(i*3+0, 'T'), -1.5); break;
			}
		}

		// m1_wid + m1_space + m1_wid/2
		double incX = StdCellParams.getRightDiffX(nmos) + 2 + 3 + 2;
		LayoutLib.newExport(nand, "inc", PortCharacteristic.IN, Tech.m1,
							4, incX, incY);
		inc.connect(nand.findExport("inc"));

		// Nand output
		double outX = incX + 2 + 3 + 2; // m1_wid/2 + m1_sp + m1_wid/2
		LayoutLib.newExport(nand, "out", PortCharacteristic.OUT, Tech.m1,
							4, outX, outY);
		TrackRouter outLo = new TrackRouterH(Tech.m2, 4, outY, nand);
		outLo.connect(nand.findExport("out"));
		for (int i = 1; i < nmos.nbSrcDrns(); i += 2) {
			outLo.connect(nmos.getSrcDrn(i));
		}

		// add wells
		double wellMinX = 0;
		double wellMaxX = outX + 2 + 1.5; // m1_wid/2 + m1m1_space/2
		stdCell.addNmosWell(wellMinX, wellMaxX, nand);

		// add essential bounds
		stdCell.addNstackEssentialBounds(wellMinX, wellMaxX, nand);

		// perform Network Consistency Check
		stdCell.doNCC(nand, nm+"{sch}");

		return nand;
	}
}
