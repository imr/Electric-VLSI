/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VertTrack.java
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
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.StdCellParams;
import com.sun.electric.tool.generator.layout.Tech;

/**
 * This part simply reserves space for one vertical metal1 track to be
 * used to connect N-stacks and P-stacks.
 */ 
public class VertTrack {
	public static Cell makePart(StdCellParams stdCell) {
		String nm = stdCell.parameterizedName("vertTrack")+"{lay}";
		Cell vtrack = stdCell.findPart(nm);
		if (vtrack!=null) return vtrack;
		vtrack = stdCell.newPart(nm);
		
		// (m1m1 space)/2 + (m1 width)/2
		double inX = 1.5 + 2;
		double inY = 0;
		PortInst inPin = LayoutLib.newNodeInst(Tech.m1pin, inX, inY, 1, 1, 0,
											   vtrack).getOnlyPortInst();
		Export.newInstance(vtrack, inPin, "in")
			.setCharacteristic(PortCharacteristic.IN);
		// Add some metal to port to allow software to guess the width
		// of metal to use to connect to this port.
		LayoutLib.newArcInst(Tech.m1, 3, inPin, inPin);
		
		// Well width must be at least 12 to avoid DRC errors
		// This cell is one of the rare cases where the cell's essential
		// bounds are narrower than the well
		double wellMinX = inX - 6;
		double wellMaxX = inX + 6;
		stdCell.addNmosWell(wellMinX, wellMaxX, vtrack);
		stdCell.addPmosWell(wellMinX, wellMaxX, vtrack);
		
		// add essential bounds
		double cellMaxX = inX + 2 + 1.5; // (m1 width)/2 + (m1-m1 space)/2
		stdCell.addEssentialBounds(0, cellMaxX, vtrack);
		
		return vtrack;
	}
}
