/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Pms1.java
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
import com.sun.electric.tool.generator.layout.FoldedPmos;
import com.sun.electric.tool.generator.layout.FoldsAndWidth;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.StdCellParams;
import com.sun.electric.tool.generator.layout.Tech;
import com.sun.electric.tool.generator.layout.TrackRouter;
import com.sun.electric.tool.generator.layout.TrackRouterH;

public class Pms1 {
	private static final double wellOverhangDiff = 6;
	private static final double gY = 4.0;
	private static final double dY = 11.0;
	private static final double pmosBot = 9.0;
	
	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}
	
	public static Cell makePart(double sz, StdCellParams stdCell) {
		sz = stdCell.roundSize(sz);
		String nm = "pms1" + (stdCell.getDoubleStrapGate() ? "_strap" : "");
		sz = stdCell.checkMinStrength(sz, .5, nm);
		
		// Space needed at bottom of MOS well.
		// We need more space if we're double strapping poly.
		double outsideSpace = stdCell.getDoubleStrapGate() ? (
            1.5 + 5 + 2		// p1_p1_sp/2 + p1m1_wid + p1_pd_sp
        ) : (
            wellOverhangDiff
        );
		
		double spaceAvail = stdCell.getCellTop() - outsideSpace - pmosBot;
		double totWid = sz * 6;
		FoldsAndWidth fw = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 1);
		error(fw==null, "can't make Pms1 this small: "+sz);
		
		Cell pms1 = stdCell.findPart(nm, sz);
		if (pms1!=null) return pms1;
		pms1 = stdCell.newPart(nm, sz);
		
		// leave vertical m1 track for g
		double gX = 1.5 + 2;		// m1_m1_sp/2 + m1_wid/2
		LayoutLib.newExport(pms1, "g", PortCharacteristic.IN, Tech.m1,
							4, gX, gY);
		double mosX = gX + 2 + 3 + 2; 	// m1_wid/2 + m1_m1_sp + m1_wid/2
		double pmosY = pmosBot + fw.physWid/2;
		FoldedMos pmos = new FoldedPmos(mosX, pmosY, fw.nbFolds, 1,
										fw.gateWid, pms1, stdCell);
		// output  m1_wid/2 + m1_m1_sp + m1_wid/2
		double dX = StdCellParams.getRightDiffX(pmos) + 2 + 3 + 2;
		LayoutLib.newExport(pms1, "d", PortCharacteristic.OUT, Tech.m1,
							4, dX, dY);
		// create gnd export and connect to MOS source/drains
		stdCell.wireVddGnd(pmos, StdCellParams.EVEN, pms1);
		
		// connect up g
		TrackRouter g = new TrackRouterH(Tech.m1, 3, gY, pms1);
		g.connect(pms1.findExport("g"));
		for (int i=0; i<pmos.nbGates(); i++) g.connect(pmos.getGate(i, 'B')); 
		
		if (stdCell.getDoubleStrapGate()) {
			// Connect gates using metal1 along top of cell 
			double vddTop = stdCell.getVddY() + stdCell.getVddWidth()/2;
			double gHiFromVdd = vddTop + 3 + 2;	// m1_m1_sp +m1_wid/2
			double pmosTop = pmosY + fw.physWid/2;
			double gHiFromMos = pmosTop + 2 + 2.5;	// pd_p1_sp + p1m1_wid/2
			double gHiY = Math.max(gHiFromVdd, gHiFromMos); 
			
			TrackRouter gHi = new TrackRouterH(Tech.m1, 3, gHiY, pms1);
			gHi.connect(pms1.findExport("g"));
			for (int i=0; i<pmos.nbGates(); i++) {
				gHi.connect(pmos.getGate(i, 'T'));
			}
		}
		
		// connect up output
		TrackRouter d = new TrackRouterH(Tech.m2, 4, dY, pms1);
		d.connect(pms1.findExport("d"));
		for (int i=1; i<pmos.nbSrcDrns(); i+=2) {d.connect(pmos.getSrcDrn(i));}
		
		// add wells
		double wellMinX = 0;
		double wellMaxX = dX + 2 + 1.5; // m1_wid/2 + m1m1_space/2
		stdCell.addPmosWell(wellMinX, wellMaxX, pms1);
		
		// add essential bounds
		stdCell.addPstackEssentialBounds(wellMinX, wellMaxX, pms1);

		// perform Network Consistency Check
		stdCell.doNCC(pms1, nm+"{sch}");

		return pms1;
	}
}

