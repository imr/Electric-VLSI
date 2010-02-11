/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BBMetric.java
 * Written by Team 2: Jan Barth, Iskandar Abudiab
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
package com.sun.electric.tool.placement.simulatedAnnealing1.metrics;

import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;
import com.sun.electric.tool.placement.PlacementFrame.PlacementPort;

import java.util.List;

/** Parallel Placement
 **/
public class BBMetric {

	
	private double currentScore;
	private List<PlacementNetwork> allNetworks;
	
	public BBMetric( List<PlacementNetwork> allNetworks ){
		this.allNetworks = allNetworks;
	}
	
	public double getScore() {
		currentScore = 0;		
		for (int i = 0; i < allNetworks.size(); i++) { 
			PlacementNetwork n = allNetworks.get(i);			
			currentScore += calculateBoundingBoxScore(n);
		}		
		return currentScore;		
	}

	protected double calculateBoundingBoxScore( PlacementNetwork n ) {
		//base case
		List<PlacementPort> l =  n.getPortsOnNet();
		if (l.size() == 0) return 0;
		double xMin, xMax, yMin, yMax;
		xMin = Double.MAX_VALUE;
		xMax = Double.MIN_VALUE;
		yMin = Double.MAX_VALUE;
		yMax = Double.MIN_VALUE;
		
		for (PlacementPort p: l) {			
			PlacementNode originalPlacementNode = p.getPlacementNode();
			double movedX = 0, movedY = 0;
			double x = movedX + originalPlacementNode.getPlacementX(); // + p.getRotatedOffX();
			double y = movedY + originalPlacementNode.getPlacementY(); //+ p.getRotatedOffY() ;
			if (x < xMin) {
				xMin = x;
			}
			if (x > xMax) {
				xMax = x;
			}
			if (y < yMin) {
				yMin = y;
			}
			if (y > yMax) {
				yMax = y;
			}		
		}
		double score =  (xMax - xMin) + (yMax - yMin);
		return score;
	}
	
}
