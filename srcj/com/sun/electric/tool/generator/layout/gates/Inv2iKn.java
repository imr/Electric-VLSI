/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Inv2iKn.java
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

/** Tricky: Normally the strong PMOS is folded and the right most
 * source/drain is ground. However, if the strong PMOS is so small
 * that it has only one fold then the right most source/drain is the
 * output.  This creates a special case for the weak PMOS because the
 * weak PMOS overlaps the strong PMOS's right most source/drain. When
 * the strong PMOS gets very small, we have to mirror the weak PMOS
 * about it's gate. */
public class Inv2iKn {
	private static final double nmosTop = -9.0;
	private static final double pmosBot = 9.0;
	private static final double wellOverhangDiff = 6;
	private static final double pGatesY = 4.0;
	private static final double nGatesY = -4.0;
	private static final double outHiY = 11.0;
	private static final double outLoY = -11.0;
	private static final double weakRatio = 10;
	
	// Insert extra space between the leftmost diffusion contact and the
	// leftmost gate.
	private static final FoldedMos.GateSpace spaceFirstGate =
		new FoldedMos.GateSpace() {
			public double getExtraSpace(double requiredExtraSpace, int foldNdx,
										int nbFolds, int spaceNdx, int nbGates){
				return (foldNdx==0 && spaceNdx==0) ? .5 : requiredExtraSpace;
			}
		};
	
	// If strong PMOS has only one fold then connect ODD src/drn of weak
	// NMOS to ground
	private static final StdCellParams.SelectSrcDrn flipWeakPmos =
		new StdCellParams.SelectSrcDrn() {
			public boolean connectThisOne(int mosNdx, int srcDrnNdx) {
				if (mosNdx==0) {
					return srcDrnNdx%2==0;
				} else if (mosNdx==1) {
					return srcDrnNdx%2==1;
				} else {
					error(true, "more than two FoldedMos?");
					return false;
				}
			}
		};
	
	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}
	
	public static Cell makePart(double sz, StdCellParams stdCell) {
		sz = stdCell.roundSize(sz);
		String nm = "inv2iKn";
		sz = stdCell.checkMinStrength(sz, 1, nm);
		
		// Compute number of folds and width for PMOS
		double spaceAvail = stdCell.getCellTop() - wellOverhangDiff - pmosBot;
		double totWid = sz * 6;
		FoldsAndWidth fwP = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 1);
		error(fwP==null, "can't make "+nm+" this small: "+sz);
		
		// Weak PMOS is 1/weakRatio the width of the string PMOS
		totWid = Math.max(3, totWid/weakRatio);
		FoldsAndWidth fwW = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 1);
		error(fwW==null, "can't make "+nm+" this small: "+sz);
		
		// Compute number of folds and width for NMOS
		spaceAvail = nmosTop - (stdCell.getCellBot() + wellOverhangDiff);
		totWid = sz * 3;
		FoldsAndWidth fwN = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 1);
		error(fwN==null, "can't make "+nm+" this small: "+sz);
		
		// create BUF Part
		Cell buf = stdCell.findPart(nm, sz);
		if (buf!=null) return buf;
		buf = stdCell.newPart(nm, sz);
		
		// place exports 1/2 cell pitch inside cell boundary
		double leftInX = 3.5;
		// m1_wid/2 + m1_m1_sp + diffCont_wid/2
		double mosX = leftInX + 2 + 3 + 2;
		
		// PMOS
		double pmosY = pmosBot + fwP.physWid/2;
		FoldedMos pmos = new FoldedPmos(mosX, pmosY, fwP.nbFolds, 1,
										fwP.gateWid, null, 'B', buf, stdCell);
		// NMOS
		double nmosY = nmosTop - fwN.physWid/2;
		FoldedMos nmos = new FoldedNmos(mosX, nmosY, fwN.nbFolds, 1,
										fwN.gateWid, null, 'T', buf, stdCell);
		// weak PMOS overlaps strong PMOS
		//int lastSrcDrnP = pmos.nbSrcDrns()-1;
		double rightSrcDrnX = StdCellParams.getRightDiffX(pmos);
		boolean strongPmosOneFold = fwP.nbFolds==1;
		pmosY = pmosBot + fwW.physWid/2;
		FoldedMos pmosW = new FoldedPmos(rightSrcDrnX, pmosY, fwW.nbFolds, 1,
										 fwW.gateWid, spaceFirstGate, 'B', buf, stdCell);
		FoldedMos[] pmoss = new FoldedMos[] {pmos, pmosW};
		
		// create vdd and gnd exports and connect to MOS source/drains
		stdCell.wireVddGnd(nmos, StdCellParams.EVEN, buf);
		stdCell.wireVddGnd(pmoss,
						  strongPmosOneFold ? flipWeakPmos : StdCellParams.EVEN,
						   buf);
		
		// Connect NMOS and PMOS gates
		TrackRouter pGates = new TrackRouterH(Tech.m1, 3, pGatesY, buf);
		for (int i=0; i<pmos.nbGates(); i++) {
			pGates.connect(pmos.getGate(i, 'B'));
		}
		TrackRouter nGates = new TrackRouterH(Tech.m1, 3, nGatesY, buf);
		for (int i=0; i<nmos.nbGates(); i++) {
			nGates.connect(nmos.getGate(i, 'T'));
		}
		// weak PMOS connected to nGates 
		for (int i=0; i<pmosW.nbGates(); i++) {
			nGates.connect(pmosW.getGate(i, 'B'));
		}
		
		// input p is a reset input
		double lastSrcDrnX = StdCellParams.getRightDiffX(pmosW, nmos);
		double inPX = lastSrcDrnX + 7;	// track_pitch
		LayoutLib.newExport(buf, "in[p]", PortCharacteristic.IN, Tech.m1,
							4, inPX, pGatesY);
		pGates.connect(buf.findExport("in[p]"));
		
		// Buf output
		double outX = inPX + 7; // track_pitch
		LayoutLib.newExport(buf, "out", PortCharacteristic.OUT, Tech.m1,
							4, outX, 0);
		TrackRouter outHi = new TrackRouterH(Tech.m2, 4, outHiY, buf);
		outHi.connect(buf.findExport("out"));
		for (int i=0; i<pmoss.length; i++) {
			if (i==1 && strongPmosOneFold) {
				outHi.connect(pmoss[i].getSrcDrn(0));// see comment at top
			} else {
				for (int j=1; j<pmoss[i].nbSrcDrns(); j+=2) {
					outHi.connect(pmoss[i].getSrcDrn(j));
				}
			}
		}
		TrackRouter outLo = new TrackRouterH(Tech.m2, 4, outLoY, buf);
		outLo.connect(buf.findExport("out"));
		for (int i=1; i<nmos.nbSrcDrns(); i+=2) {
			outLo.connect(nmos.getSrcDrn(i));
		}
		// input n
		LayoutLib.newExport(buf, "in[n]", PortCharacteristic.IN, Tech.m1,
							4, leftInX, nGatesY);
		nGates.connect(buf.findExport("in[n]"));
		
		/*
		// reset output
		TrackRouter rstOut = new TrackRouterH(Tech.m2, 4, pGatesY, buf);
		buf.newExport("resetOut", PortProto.OUT, Tech.m2, 4, leftInX, pGatesY);
		rstOut.connect(buf.findExport("resetOut"));
		PortInst jog = Tech.m2pin.newInst(1,1,
		pmos.getSrcDrn(0).getCenterX(),
		pGatesY, 0, buf).getPort();
		rstOut.connect(jog);
		outHi.connect(jog);
		*/
		
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

