/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Nms1.java
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

public class Nms1 {
	private static final double wellOverhangDiff = 6;
	private static final double gY = -4.0;
	private static final double dY = -11.0;
	private static final double nmosTop = -9.0;
	// The minimum diffusion surrounding a diffusion contact
	private static final double minContDiff = 5;
	
	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}
	
	public static Cell makePart(double sz, StdCellParams stdCell) {
		sz = stdCell.roundSize(sz);
		String nm = "nms1" + (stdCell.getDoubleStrapGate() ? "_strap" : "");
		sz = stdCell.checkMinStrength(sz, 1, nm);
		
		// Space needed at top of MOS well.
		// We need more space if we're double strapping poly.
		double outsideSpace = stdCell.getDoubleStrapGate() ? (
            2 + 5 +  1.5		// p1_nd_sp + p1m1_wid + p1_p1_sp/2
        ) : (
            wellOverhangDiff
        );
		
		double spaceAvail = nmosTop - (stdCell.getCellBot() + outsideSpace);
		double totWid = sz * 3;
		FoldsAndWidth fw = stdCell.calcFoldsAndWidth(spaceAvail, totWid, 1);
		error(fw==null, "can't make Nms1 this small: "+sz);
		
		Cell nms1 = stdCell.findPart(nm, sz);
		if (nms1!=null) return nms1;
		nms1 = stdCell.newPart(nm, sz);
		
		// leave vertical m1 track for g
		double gX = 1.5 + 2;		// m1_m1_sp/2 + m1_wid/2
		LayoutLib.newExport(nms1, "g", PortCharacteristic.IN, Tech.m1,
							4, gX, gY);
		// gnd port overlaps leftmost MOS diffusion contacts
		double mosX = gX + 2 + 3 + 2; 	// m1_wid/2 + m1_m1_sp + m1_wid/2
		
		double nmosY = nmosTop - fw.physWid/2;
		FoldedMos nmos = new FoldedNmos(mosX, nmosY, fw.nbFolds, 1,
										fw.gateWid, nms1, stdCell);
		// output d m1_wid/2 + m1_m1_sp + m1_wid/2
		double dX = StdCellParams.getRightDiffX(nmos) + 2 + 3 + 2;
		LayoutLib.newExport(nms1, "d", PortCharacteristic.OUT, Tech.m1,
							4, dX, dY);
		// create gnd export and connect to MOS source/drains
		stdCell.wireVddGnd(nmos, StdCellParams.EVEN, nms1);

		// connect up input g
		TrackRouter g = new TrackRouterH(Tech.m1, 3, gY, nms1);
		g.connect(nms1.findExport("g"));
		for (int i=0; i<nmos.nbGates(); i++) g.connect(nmos.getGate(i, 'T')); 
		
		if (stdCell.getDoubleStrapGate()) {
			// Connect gates using metal1 along bottom of cell 
			double gndBot = stdCell.getGndY() - stdCell.getGndWidth()/2;
			double inLoFromGnd = gndBot - 3 - 2;	// -m1_m1_sp -m1_wid/2
			double nmosBot = nmosY - fw.physWid/2;
			double inLoFromMos = nmosBot - 2 - 2.5;	// -nd_p1_sp -p1m1_wid/2
			double inLoY = Math.min(inLoFromGnd, inLoFromMos); 
			
			TrackRouter inLo = new TrackRouterH(Tech.m1, 3, inLoY, nms1);
			inLo.connect(nms1.findExport("g"));
			for (int i=0; i<nmos.nbGates(); i++) {
				inLo.connect(nmos.getGate(i, 'B'));
			}
		}
		
		// connect up output d
		TrackRouter d = new TrackRouterH(Tech.m2, 4, dY, nms1);
		d.connect(nms1.findExport("d"));
		for (int i=1; i<nmos.nbSrcDrns(); i+=2) {d.connect(nmos.getSrcDrn(i));}
		
		// add wells
		double wellMinX = 0;
		double wellMaxX = dX + 2 + 1.5; // m1_wid/2 + m1m1_space/2
		stdCell.addNmosWell(wellMinX, wellMaxX, nms1);
		
		// add essential bounds
		stdCell.addNstackEssentialBounds(wellMinX, wellMaxX, nms1);
		
		// perform Network Consistency Check
		stdCell.doNCC(nms1, nm+"{sch}");
		
	   return nms1;
	}
}
