/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Nms2.java
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
import com.sun.electric.tool.generator.layout.FoldsAndWidth;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.StdCellParams;
import com.sun.electric.tool.generator.layout.Tech;
import com.sun.electric.tool.generator.layout.TrackRouter;
import com.sun.electric.tool.generator.layout.TrackRouterH;

public class Nms2 {
	private static final double gY = -4.0;
	private static final double dY = -11.0;
	private static final double nmosTop = -9.0;

	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

	public static Cell makePart(double sz, StdCellParams stdCell) {
		sz = stdCell.roundSize(sz);
		String nm = "nms2";
		sz = stdCell.checkMinStrength(sz, .5, nm);

		// p1_p1_sp/2 + p1m1_wid + p1_diff_sp
		double nmosLowest = stdCell.getCellBot() + 1.5 + 5 + 2;
		double spaceAvail = nmosTop - nmosLowest;
		int nbStacked = 2;
		double totWid = sz * 3 * nbStacked;
		FoldsAndWidth fw = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 1);
		error(fw == null, "can't make Nms2 this small: " + sz);

		// g2 must be spaced from gnd and nmos
		// lowerGndEdge -m1_m1_sp - m1_wid/2
		double g2FromGndY =
			stdCell.getGndY() - stdCell.getGndWidth() / 2 - 3 - 2;

		// lowerMosEdge - p1_diff_sp - p1m1_wid/2 
		double g2FromMosY = nmosTop - fw.physWid - 2 - 2.5;
		double g2Y = Math.min(g2FromGndY, g2FromMosY);

		Cell nms2 = stdCell.findPart(nm, sz);
		if (nms2 != null)
			return nms2;
		nms2 = stdCell.newPart(nm, sz);

		// leave vertical m1 track for g
		double gX = 1.5 + 2; // m1_m1_sp/2 + m1_wid/2
		LayoutLib.newExport(nms2, "g", PortCharacteristic.IN, Tech.m1, 4,
							gX, gY);
		double mosX = gX + 2 + 3 + 2; // m1_wid/2 + m1_m1_sp + m1_wid/2

		double nmosY = nmosTop - fw.physWid / 2;
		FoldedMos nmos =
			new FoldedNmos(
				mosX,
				nmosY,
				fw.nbFolds,
				nbStacked,
				fw.gateWid,
				nms2, stdCell);
		// g2  m1_wid/2 + m1_m1_sp + m1_wid/2
		double g2X = StdCellParams.getRightDiffX(nmos) + 2 + 3 + 2;
		LayoutLib.newExport(nms2, "g2", PortCharacteristic.IN, Tech.m1,
							4, g2X, g2Y);
		// output d  m1_wid/2 + m1_m1_sp + m1_wid/2
		double dX = g2X + 2 + 3 + 2;
		LayoutLib.newExport(nms2, "d", PortCharacteristic.OUT, Tech.m1,
							4, dX, dY);
		// create gnd export and connect to MOS source/drains
		stdCell.wireVddGnd(nmos, StdCellParams.EVEN, nms2);

//		// fool Electric's NCC into paralleling NMOS stacks by connecting
//		// stacks' internal diffusion nodes.
//		for (int i = 1; i < nmos.nbInternalSrcDrns(); i += 1) {
//			LayoutLib.newArcInst(Tech.universalArc, 0,
//								 nmos.getInternalSrcDrn(0),
//								 nmos.getInternalSrcDrn(i));
//		}

		// connect inputs a and b
		TrackRouter g = new TrackRouterH(Tech.m1, 3, gY, nms2);
		TrackRouter g2 = new TrackRouterH(Tech.m1, 3, g2Y, nms2);
		g.connect(nms2.findExport("g"));
		g2.connect(nms2.findExport("g2"));
		for (int i = 0; i < nmos.nbGates(); i += 2) {
			// connect 2 gates at a time in case nbFolds==1
			if ((i / 2) % 2 == 0) {
				g.connect(nmos.getGate(i, 'T'), -4, -Tech.getPolyLShapeOffset());
				g2.connect(nmos.getGate(i + 1, 'B'), 4, Tech.getPolyLShapeOffset());
			} else {
				g.connect(nmos.getGate(i + 1, 'T'), 4, -Tech.getPolyLShapeOffset());
				g2.connect(nmos.getGate(i, 'B'), -4, Tech.getPolyLShapeOffset());
			}
		}

		// connect output d
		TrackRouter d = new TrackRouterH(Tech.m2, 4, dY, nms2);
		d.connect(nms2.findExport("d"));
		for (int i = 1; i < nmos.nbSrcDrns(); i += 2) {
			d.connect(nmos.getSrcDrn(i));
		}

		// add wells
		double wellMinX = 0;
		double wellMaxX = dX + 2 + 1.5; // m1_wid/2 + m1m1_space/2
		stdCell.addNmosWell(wellMinX, wellMaxX, nms2);

		// add essential bounds
		stdCell.addNstackEssentialBounds(wellMinX, wellMaxX, nms2);

		// perform Network Consistency Check
		stdCell.doNCC(nms2, nm+"{sch}");

		return nms2;
	}
}
