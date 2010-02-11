/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BBMetric.java
 * Written by Team 5: Jochen Huck
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
package com.sun.electric.tool.placement.forceDirected1.metric;

import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.PlacementFrame.PlacementPort;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * Estimate wirelength using the bounding box metric
 */
public class BBMetric extends AbstractMetric {
	
	@Override
	public String getMetricName() { return "Bounding Box Metric"; }
	
	/**
	 * estimate wirelength using the bounding box metric for all nets
	 * @return - the wirelength estimate
	 */
	@Override
	public double compute() {
		double sum = 0;
			
		for(PlacementNetwork net : allNetworks) {
			sum = sum + compute(net);
		}
			
		return sum;
	}

	private double compute(PlacementNetwork net) {
		List<PlacementPort> portsOnNet = net.getPortsOnNet();
		
		double leftmost = Double.MAX_VALUE;
		double rightmost = Double.MIN_VALUE;
		double uppermost = Double.MIN_VALUE;
		double undermost = Double.MAX_VALUE;
		
		for(PlacementPort port : portsOnNet) {
			Point2D.Double position = getPortPosition(port);
			if(position.getX() < leftmost) leftmost = position.getX();
			if(position.getX() > rightmost) rightmost = position.getX();
			if(position.getY() > uppermost) uppermost = position.getY();		
			if(position.getY() < undermost) undermost = position.getY();
		}
		
//		DecimalFormat formater = new DecimalFormat("###.##");
//		System.out.println("left: " + formater.format(leftmost) + ", right: " + formater.format(rightmost));
//		System.out.println("upper: " + formater.format(uppermost) + ", under: " + formater.format(undermost));
//		System.out.println();
		
		return (rightmost - leftmost) + (uppermost - undermost);
			
	}
	
	private Point2D.Double getPortPosition(PlacementPort port) {
		double x = port.getRotatedOffX() + port.getPlacementNode().getPlacementX();
		double y = port.getRotatedOffY() + port.getPlacementNode().getPlacementY();
		
		return new Point2D.Double(x,y);
	}
}
