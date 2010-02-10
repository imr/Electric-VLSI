/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PlacementNodeProxy.java
 * Written by Team 3: Christian Wittner, Ivan Dimitrov
 * 
 * This code has been developed at the Karlsruhe Institute of Technology (KIT), Germany, 
 * as part of the course "Multicore Programming in Practice: Tools, Models, and Languages".
 * Contact instructor: Dr. Victor Pankratius (pankratius@ipd.uka.de)
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.tool.placement.genetic1.g1;

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;

public class PlacementNodeProxy {

	public PlacementNode node;

	public short angle;
	public int width;
	public int height;

	public PlacementNodeProxy(PlacementNode node, short angle) {
		this.node = node;
		angle = 0;
		width = ((int) node.getWidth()) + 2;
		height = ((int) node.getHeight()) + 2;
	}
	
	public PlacementNodeProxy(PlacementNode node) {
		this.node = node;
		angle = (short) node.getPlacementOrientation().getAngle();
		width = ((int) node.getWidth()) + 2;
		height = ((int) node.getHeight()) + 2;
	}

	public void setPlacement(int xPos, int yPos) {
		node.setPlacement(xPos, yPos);

	}

	public void setOrientation(Orientation fromAngle) {
		node.setOrientation(fromAngle);

	}

}
