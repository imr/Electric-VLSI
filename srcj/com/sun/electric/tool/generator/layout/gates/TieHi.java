/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TieHi.java
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
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.StdCellParams;
import com.sun.electric.tool.generator.layout.Tech;

/**
 * This part has an output connected to Vdd.
 */ 
public class TieHi {
	private static final double DEF_SIZE = LayoutLib.DEF_SIZE;

	public static Cell makePart(StdCellParams stdCell) {
		String nm = stdCell.parameterizedName("tieHi")+"{lay}";
		Cell tieHi = stdCell.findPart(nm);
		if (tieHi!=null) return tieHi;
		tieHi = stdCell.newPart(nm);
		
		// (m1m1 space)/2 + (m1 width)/2
		double pwrX = 1.5 + 2;
		double pwrY = stdCell.getVddY();
		
		// We need to export two pins in order to export two hints, one
		// for the width of metal-2 Vdd connections and one for the width
		// of metal-1 out connections.
		String vddName = stdCell.getVddExportName();
		PortCharacteristic vddRole = stdCell.getVddExportRole();
		LayoutLib.newExport(tieHi, vddName, vddRole, Tech.m2, 4, pwrX, pwrY);
		LayoutLib.newExport(tieHi, "pwr", PortCharacteristic.OUT,
							Tech.m1, 4, pwrX, pwrY);

		// connect the two exports using a via
		PortInst via = LayoutLib.newNodeInst(Tech.m1m2, pwrX,
											 pwrY, 4, stdCell.getVddWidth(),
											 0, tieHi).getOnlyPortInst();
		
		LayoutLib.newArcInst(Tech.m2, DEF_SIZE,
							 tieHi.findExport(vddName).getOriginalPort(), via);
		LayoutLib.newArcInst(Tech.m1, DEF_SIZE,
							 tieHi.findExport("pwr").getOriginalPort(), via);
		
		// Well width must be at least 12 to avoid DRC errors
		// This cell is one of the rare cases where the cell's essential
		// bounds are narrower than the well
		double wellMinX = pwrX - 6;
		double wellMaxX = pwrX + 6;
		stdCell.addNmosWell(wellMinX, wellMaxX, tieHi);
		stdCell.addPmosWell(wellMinX, wellMaxX, tieHi);
		
		// add essential bounds
		double cellMaxX = pwrX + 2 + 1.5; // (m1 width)/2 + (m1-m1 space)/2
		stdCell.addEssentialBounds(0, cellMaxX, tieHi);
		
		return tieHi;
	}
}
