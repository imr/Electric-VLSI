/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AreaOverlapMetric.java
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

import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;
import com.sun.electric.tool.placement.simulatedAnnealing1.SimulatedAnnealing.PlacementNodePosition;

import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Parallel Placement
 * 
 * Metric to compute the ovelapping areas of the placement nodes.
 * This metric is incremental, i.e. given a new state it updates the score by only computing
 * the changes in the ovelapping areas caused by the nodes that were moved or swapped.
 **/
public class AreaOverlapMetric {

	private List<PlacementNode> allNodes;
	private com.sun.electric.tool.placement.simulatedAnnealing1.SimulatedAnnealing.IncrementalState incState;
	private double currentScore;
	private Map<PlacementNode, Double> oevrlapScores;

	/**
	 * Method to create a AreaOverlapMetric object.
	 * @param allNodes a list containing all <code>PlacementNode</code> objects.
	 * @param incrementalState an <code>IncremetntalState</code> object describing the current state.
	 */
	public AreaOverlapMetric(List<PlacementNode> allNodes, com.sun.electric.tool.placement.simulatedAnnealing1.SimulatedAnnealing.IncrementalState incrementalState){
		this.oevrlapScores = new HashMap<PlacementNode , Double>(allNodes.size());
		this.allNodes = allNodes;
		this.incState = incrementalState;
	}

	/**
	 * Initialises this metric.
	 * @param allNodes a list containing all <code>PlacementNode</code> objects.
	 * @return current score, which is the sum of all overlapping areas.
	 */
	public double init(List<PlacementNode> allNodes) {
		currentScore = 0;		
		for (PlacementNode node: allNodes) {
			oevrlapScores.put(node, new Double(0));
			computeOverlapForNode(node);
		}		
		return currentScore;		
	}
	
	/**
	 * Get current score.
	 * @return current score, which is the sum of all overlapping areas.
	 */
	public double getCurrentScore() {
		return currentScore;
	}

	/**
	 * Method that computes all overlapping areas caused by the given <code>PlacementNode</code>.
	 * @param theOne <code>PlacementNode</code> object for which the overlapping is to be computed.
	 * @return sum of all overlapping areas caused by this node.
	 */
	public double computeOverlapForNode(PlacementNode theOne) {
		double areaOverlap= 0.0;

		for (PlacementNode notTheOne: allNodes) {
			if (theOne == notTheOne) continue;
			double x1 = theOne.getPlacementX();
			double y1 = theOne.getPlacementY();
			double w1 = theOne.getWidth();
			double h1 = theOne.getHeight();
			int angle1 = theOne.getPlacementOrientation().getAngle();
			
			double x2 = notTheOne.getPlacementX();
			double y2 = notTheOne.getPlacementY();
			double w2 = theOne.getWidth();
			double h2 = theOne.getHeight();
			int angle2 = notTheOne.getPlacementOrientation().getAngle();
			
			//Check if node was moved or swapped.
			int originalPlacementNodeIndex = allNodes.indexOf(theOne);
			if (incState.isNodeChanged(originalPlacementNodeIndex)) {
				PlacementNodePosition changedNode = incState.getNodeFromState(originalPlacementNodeIndex);
				x1 = changedNode.getPlacementX();
				y1 = changedNode.getPlacementY();
				angle1 = changedNode.getPlacementOrientation().getAngle();
			}			
			
			//Get bounding rectangles and compute overlap.
			Rectangle2D.Double r1 = getRectangleForNode(x1, y1, w1, h1, angle1);
			Rectangle2D.Double r2 = getRectangleForNode(x2, y2, w2, h2, angle2);
			areaOverlap += getIntersectionArea(r1, r2);
		}
		
		double oldScore = oevrlapScores.get(theOne).doubleValue();
		if (oldScore > 0 && Math.abs(oldScore - areaOverlap) > 0.001) {
//			System.out.println("Score changed from " + oldScore + " to " + areaOverlap);
		}
		oevrlapScores.put(theOne, new Double(areaOverlap));
		currentScore = currentScore - oldScore + areaOverlap;
		return areaOverlap;
	}

	/**
	 * Method that creates a bounding <code>Rectangle2D.Double</code> given the nodes placement.
	 * @param x centre's X coordinate.
	 * @param y centre's Y coordinate.
	 * @param w width of the node
	 * @param h height of the node.
	 * @param angle the rotation angle of the node.
	 * @return <code>Rectangle2D.Double</code> object bounding the node.
	 */
	private Rectangle2D.Double getRectangleForNode(double x, double y, double w, double h, int angle){
		//TODO: Check the correctness of the generated rectangles.
		switch(angle){
			case 0:	 	return new Rectangle2D.Double(x - (w/2), y - (h/2), w, h); 
			case 90: 	return new Rectangle2D.Double(x - (h/2), y - (w/2), h, w); 
			case 180:	return new Rectangle2D.Double(x - (w/2), y - (h/2), w, h); 
			case 270:	return new Rectangle2D.Double(x - (h/2), y - (w/2), h, w); 
			default: 	return new Rectangle2D.Double(x - (w/2), y - (h/2), w, h);
		}
	}
	
	/**
	 * Method to compute the area overlapped by the two given <code>Rectangle2D.Double</code> objects.
	 * @param r1 the first <code>Rectangle2D.Double</code> object.
	 * @param r2 the second <code>Rectangle2D.Double</code> object
	 * @return returns the area being overlapped by the given rectangles.
	 */
	private double getIntersectionArea(Rectangle2D.Double r1, Rectangle2D.Double r2){
		double area = 0.0;
		Rectangle2D overlap = r1.createIntersection(r2);
		if(!overlap.isEmpty()) area = overlap.getWidth()*overlap.getHeight();
		return area;
	}

	/**
	 * Updates the metric score.
	 * @param index the index of the node that was moved or swapped.
	 * @return the new metric score.
	 */
	public double update(int index) {
		double BeforeIncUpdate = currentScore;
		computeOverlapForNode(allNodes.get(index));
		double incScore = currentScore;
		double newScore = currentScore;
		if (Math.abs(incScore - newScore) > 0.0001) {
			System.out.println("BeforeIncUpdate=" + BeforeIncUpdate + ", after update: incScore="+ incScore + ", correctScore=" + newScore);	
		}		
		return currentScore;
	}
}
