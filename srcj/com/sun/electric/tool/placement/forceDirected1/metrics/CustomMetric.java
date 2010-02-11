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
import com.sun.electric.tool.placement.PlacementFrame.PlacementPort;

import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

public abstract class CustomMetric {

	protected List<PlacementNode> nodesToPlace;
	protected List<PlacementNetwork> allNetworks;

	public CustomMetric(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks) {
		this.nodesToPlace = nodesToPlace;
		this.allNetworks = allNetworks;
	}

	public abstract double compute();

	public abstract String getMetricName();

	public String toString() {
		DecimalFormat formater = new DecimalFormat("###,###.#");
		String output = "Result of " + getMetricName() + ": " + formater.format(compute());
		return output;
	}

	//--- private -------------------------------------------------------------

	/**
	 * Method returns the absolute position of all ports in the list
	 * placementNets. It is used to create the data structure for
	 * placement.metrics
	 * 
	 * @param placementNets
	 * @return the absolute position of all ports in the list placementNets
	 */
	protected List<Point2D.Double[]> getPositionsOfPorts(List<PlacementNetwork> placementNets) {
		LinkedList<Point2D.Double[]> posOfPorts = new LinkedList<Point2D.Double[]>();
		PlacementNetwork net;
		int numberOfPorts = 0;
		for (int i = 0; i < placementNets.size(); i++) {
			net = placementNets.get(i);
			if (net != null) {
				numberOfPorts = net.getPortsOnNet().size();
				Point2D.Double[] allPositionsOfNet = new Point2D.Double[numberOfPorts];
				for (int j = 0; j < numberOfPorts; j++) {
					Point2D.Double absPosOf = getAbsolutePositionOf(net.getPortsOnNet().get(j));
					allPositionsOfNet[j] = absPosOf;
				}
				posOfPorts.add(allPositionsOfNet);
			}
		}
		return posOfPorts;
	}

	/**
	 * Auxiliary method for getPositionOfPorts()
	 * 
	 * @param placementPort
	 * @return
	 */
	private Point2D.Double getAbsolutePositionOf(PlacementPort placementPort) {
		return new Point2D.Double(
				placementPort.getRotatedOffX() + placementPort.getPlacementNode().getPlacementX(),
				placementPort.getRotatedOffY() + placementPort.getPlacementNode().getPlacementY());
	}

}
