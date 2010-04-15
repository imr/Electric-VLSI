/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RoutingFrameSimple.java
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
package com.sun.electric.tool.routing.experimentalSimple;

import com.sun.electric.tool.routing.RoutingFrame;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple Routing algorithm to create direct paths.
 */
public class RoutingFrameSimple extends RoutingFrame
{
	public RoutingParameter maxThreadsParam = new RoutingParameter("threads", "Number of Threads to use:", 5);
	public RoutingParameter happinessParam = new RoutingParameter("happiness", "Happiness level:", "happy");
	public RoutingParameter numericParam = new RoutingParameter("double", "Floating-point value:", 7.2);

	/**
	 * Method to return the name of this routing algorithm.
	 * @return the name of this routing algorithm.
	 */
	public String getAlgorithmName() { return "Simple"; }

	/**
	 * Method to return a list of parameters for this routing algorithm.
	 * @return a list of parameters for this routing algorithm.
	 */
	public List<RoutingParameter> getParameters()
	{
		List<RoutingParameter> allParams = new ArrayList<RoutingParameter>();
		allParams.add(maxThreadsParam);
		allParams.add(happinessParam);
		allParams.add(numericParam);
		return allParams;
	}

	/**
	 * Method to do Simple Routing.
	 */
	protected int runRouting(List<RoutingSegment> segmentsToRoute, List<RoutingLayer> allLayers,
		List<RoutingNode> allNodes, List<RoutingGeometry> otherBlockages, List<RoutingContact> allContacts)
	{
		int numRouted = 0;
		for(RoutingSegment rs : segmentsToRoute)
		{
			List<RoutingLayer> startLayers = rs.getStartLayers();
			RoutingLayer layerToUse = startLayers.get(0);
			RoutePoint p1 = new RoutePoint(RoutingContact.STARTPOINT, rs.getStartLocation());
			rs.addContact(p1);
			RoutePoint p2 = new RoutePoint(RoutingContact.FINISHPOINT, rs.getFinishLocation());
			rs.addContact(p2);
			RouteWire rw = new RouteWire(layerToUse, p1, p2, layerToUse.getMinWidth());
			rs.addWire(rw);
			numRouted++;
		}
		return numRouted;
	}
}
