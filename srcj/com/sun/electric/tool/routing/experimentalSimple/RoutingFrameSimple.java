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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.routing.RoutingFrame;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple Routing algorithm to create direct paths.
 */
public class RoutingFrameSimple extends RoutingFrame
{
	// examples of parameters (integer, string, double, and boolean)
	public RoutingParameter maxThreadsParam = new RoutingParameter("threads", "Number of Threads to use:", 5);
	public RoutingParameter happinessParam = new RoutingParameter("happiness", "Happiness level:", "happy");
	public RoutingParameter numericParam = new RoutingParameter("double", "Floating-point value:", 7.2);
	public RoutingParameter booleanParam = new RoutingParameter("toggle", "Run quickly:", true);

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
		allParams.add(booleanParam);
		return allParams;
	}

	/**
	 * Method to do Simple routing.
	 */
	protected void runRouting(Cell cell, List<RoutingSegment> segmentsToRoute, List<RoutingLayer> allLayers,
		List<RoutingContact> allContacts, List<RoutingGeometry> otherBlockages)
	{
		// look at every segment that needs to be routed
		for(RoutingSegment rs : segmentsToRoute)
		{
			// get the layers that can connect to the two ends of the segment
			List<RoutingLayer> startLayers = rs.getStartLayers();
			List<RoutingLayer> finishLayers = rs.getFinishLayers();

			// see if there is a common layer so that a single wire can be run
			RoutingLayer commonLayer = null;
			for(RoutingLayer rl1 : startLayers)
			{
				for(RoutingLayer rl2 : finishLayers)
				{
					if (rl1 == rl2) { commonLayer = rl1;   break; }
				}
				if (commonLayer != null) break;
			}

			if (commonLayer != null)
			{
				// one layer: make a direct connection (at any angle)
				RoutePoint p1 = new RoutePoint(RoutingContact.STARTPOINT, rs.getStartEnd().getLocation(), 0);
				rs.addWireEnd(p1);
				RoutePoint p2 = new RoutePoint(RoutingContact.FINISHPOINT, rs.getFinishEnd().getLocation(), 0);
				rs.addWireEnd(p2);
				RouteWire rw = new RouteWire(commonLayer, p1, p2, commonLayer.getMinWidth());
				rs.addWire(rw);
			} else
			{
				// different layer: find a contact that connects them
				for(RoutingContact rc : allContacts)
				{
					RoutingLayer startLayer = null, finishLayer = null;
					for(RoutingLayer rl : startLayers)
						if (rl == rc.getFirstLayer() || rl == rc.getSecondLayer()) { startLayer = rl;   break; }
					for(RoutingLayer rl : finishLayers)
						if (rl == rc.getFirstLayer() || rl == rc.getSecondLayer()) { finishLayer = rl;   break; }

					// if this contact makes the connection, place it
					if (startLayer != null && finishLayer != null)
					{
						RoutePoint p1 = new RoutePoint(RoutingContact.STARTPOINT, rs.getStartEnd().getLocation(), 0);
						rs.addWireEnd(p1);
						RoutePoint p2 = new RoutePoint(RoutingContact.FINISHPOINT, rs.getFinishEnd().getLocation(), 0);
						rs.addWireEnd(p2);

						// place the bend at an arbitrary corner of the route
						Point2D contLoc = new Point2D.Double(rs.getStartEnd().getLocation().getX(), rs.getFinishEnd().getLocation().getY());
						RoutePoint pCon = new RoutePoint(rc, contLoc, 0);
						rs.addWireEnd(pCon);

						// now define wires to the bend point
						RouteWire rw1 = new RouteWire(startLayer, p1, pCon, startLayer.getMinWidth());
						rs.addWire(rw1);
						RouteWire rw2 = new RouteWire(finishLayer, pCon, p2, finishLayer.getMinWidth());
						rs.addWire(rw2);
						break;
					}
				}
			}
		}
	}
}
