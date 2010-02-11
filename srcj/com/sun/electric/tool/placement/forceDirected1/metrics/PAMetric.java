/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CustomMetric.java
 * Written by Team 5: Andreas Wagner, Thomas Hauck, Philippe Bartscherer
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
package com.sun.electric.tool.placement.forceDirected1.metrics;

import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;

import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Placement Area Metric
 */

public class PAMetric extends CustomMetric {

	private Rectangle2D.Double area = new Rectangle2D.Double(); // chip area

	public PAMetric(List<PlacementNode> nodesToPlace,
			List<PlacementNetwork> allNetworks) {
		super(nodesToPlace, null);
	}

	public String getMetricName() {
		return "Placement-Area";
	}

	public double compute() {
		// precondition
		if (nodesToPlace.size() <= 1)
			return 0;
		// variables
		double x, y, w, h;
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
		// dimension
		for (PlacementNode node : nodesToPlace) {
			x = node.getPlacementX();
			y = node.getPlacementY();
			w = node.getWidth() / 2;
			h = node.getHeight() / 2;
			minX = Math.min(x - w, minX);
			maxX = Math.max(x + w, maxX);
			minY = Math.min(y - h, minY);
			maxY = Math.max(y + h, maxY);
		}
		area.setRect(minX, minY, maxX - minX, maxY - minY);
		return getArea();
	}

	// --- interface -----------------------------------------------------------

	public double getX() {
		return area.getX();
	}

	public double getY() {
		return area.getY();
	}

	public double getWidth() {
		return area.getWidth();
	}

	public double getHeight() {
		return area.getHeight();
	}

	public double getArea() {
		return area.getWidth() * area.getHeight();
	}
}
