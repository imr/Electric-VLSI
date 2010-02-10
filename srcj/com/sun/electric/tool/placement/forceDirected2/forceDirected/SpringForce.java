/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SpringForce.java
 * Written by Team 7: Felix Schmidt, Daniel Lechner
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
package com.sun.electric.tool.placement.forceDirected2.forceDirected;

import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;
import com.sun.electric.tool.placement.PlacementFrame.PlacementPort;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.util.Force2D;

/**
 * Parallel Placement
 * 
 * Calculate the force between two nodes
 */
public class SpringForce {

	public static Force2D calculate(PlacementNode node1, PlacementNode node2, double weight, double minDist) {

		double x = node2.getPlacementX() - node1.getPlacementX();
		double y = node2.getPlacementY() - node1.getPlacementY();

		return new Force2D(x, y).mult(weight);
	}

	public static Force2D calculate(PlacementPort port1, PlacementPort port2, double weight, double minDist) {

		PlacementNode node1 = port1.getPlacementNode();
		PlacementNode node2 = port2.getPlacementNode();

		double x = (node2.getPlacementX() + port2.getRotatedOffX()) - (node1.getPlacementX() + port1.getRotatedOffX());
		double y = (node2.getPlacementY() + port2.getRotatedOffY()) - (node1.getPlacementY() + port1.getRotatedOffY());

		return new Force2D(x, y).mult(weight);
	}

}
