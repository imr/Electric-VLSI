/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BoundingBoxMetric.java
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
import com.sun.electric.tool.placement.simulatedAnnealing1.SimulatedAnnealing.PlacementNodePosition;

import java.util.List;

/** Parallel Placement
 * 
 * A Bounding Box metric to compute the estimate wire length.
 * This metric is incremental, i.e. given a new state it computes the score by only computing
 * the changes in the nets caused by the nodes that were moved or swapped.
 **/
public class BoundingBoxMetric {
	
	private double[] netScoreCache;
	private List<PlacementNetwork> allNetworks;
	private List<PlacementNode> allNodes;
	private com.sun.electric.tool.placement.simulatedAnnealing1.SimulatedAnnealing.IncrementalState incState;
	private double currentScore;
	
	
	/**
	 * Method to create the metric.
	 * @param allNodes a list containing all <code>PlacementNode</code> objects.
	 * @param allNetworks a list of all <code>PlacementNetwork</code> objects between the nodes.
	 * @param finalState an <code>IncremetntalState</code> object describing the current state.
	 */
	public BoundingBoxMetric(List<PlacementNode> allNodes, List<PlacementNetwork> allNetworks, com.sun.electric.tool.placement.simulatedAnnealing1.SimulatedAnnealing.IncrementalState finalState) {
		netScoreCache = new double[allNetworks.size()];		
		this.allNodes = allNodes;		
		this.allNetworks = allNetworks;
		this.incState = finalState;
	}
	
	/**
	 * This initialises this metric.
	 * @param allNetworks a list of all <code>PlacementNetwork</code> objects between the nodes.
	 * @return current score, which is the estimated wire length based on the initial state.
	 */
	public double init(List<PlacementNetwork> allNetworks) {
		currentScore = 0;		
		for (int i = 0; i < allNetworks.size(); i++) { 
			PlacementNetwork n = allNetworks.get(i);			
			netScoreCache[i] = 0;
			calculateBoundingBoxScore(n);
		}		
		return currentScore;		
	}
	
	/**
	 * Get current score.
	 * @return current score, which is the estimated wire length.
	 */
	public double getCurrentScore() {
		return currentScore;
	}
	
	/**
	 * Gets the Bounding Box Score for the given <code>PlacementNetwork</code>
	 * @param n a <code>PlacementNetwork</code> to get the score for
	 * @return the metric score for the given <code>PlacementNetwork</code>
	 */
	public double getScoreForNetwork(PlacementNetwork n){
		return netScoreCache[allNetworks.indexOf( n )];
	}
	
	
	
	/**
	 * Method to compute the bounding box score for a given network.
	 * @param n a <code>PlacementNetwork</code> object for which the score is to be calculated.
	 * @return the score for this given <code>PlacementNetwork</code>.
	 */
	protected double calculateBoundingBoxScore(PlacementNetwork n) {
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
			int originalPlacementNodeIndex = allNodes.indexOf(originalPlacementNode);
			double movedX = 0, movedY = 0;
			if (incState.isNodeChanged(originalPlacementNodeIndex)) {
				PlacementNodePosition changedNode = incState.getNodeFromState(originalPlacementNodeIndex);
				movedX = changedNode.getPlacementX() - originalPlacementNode.getPlacementX() ;
				movedY = changedNode.getPlacementY() - originalPlacementNode.getPlacementY() ;
			}			
			p.computeRotatedOffset();
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
		int netIndex = allNetworks.indexOf(n);
		double oldScore = netScoreCache[netIndex];
		if (oldScore > 0 && Math.abs(oldScore - score) > 0.001) {
			//System.out.println("Score changed from " + oldScore + " to " + score);
		}
		netScoreCache[netIndex] = score;
		currentScore = currentScore - oldScore + score;
		return score;
	}

	/**
	 * Returns the metric's score for all <code>PlacementNetwork</code>s that the 
	 * given <code>PlacementNode</code> belongs to.
	 * @param n the given <code>PlacementNode</code>
	 * @return the metric's score
	 */
	public double getNetworkScoreForNode(PlacementNode n){
		double score = 0;
		List<PlacementPort> ports = allNodes.get( allNodes.indexOf( n ) ).getPorts();		
		for (PlacementPort p: ports) {
			if (p == null) continue;
			PlacementNetwork net = p.getPlacementNetwork();
			if (net == null) continue;
			score += getScoreForNetwork(net);
		}		
		return score;
	}
	
	/**
	 * Updates the metric score.
	 * @param index the index of the node that was moved or swapped.
	 * @return the new metric score.
	 */
	public double update(int index) {
		double BeforeIncUpdate = currentScore;
		List<PlacementPort> ports = allNodes.get(index).getPorts();		
		for (PlacementPort p: ports) {
			if (p == null) continue;
			PlacementNetwork net = p.getPlacementNetwork();
			if (net == null) continue;
			calculateBoundingBoxScore(net);
		}		
		double incScore = currentScore;
		//init(allNetworks);
		double newScore = currentScore;
		if (Math.abs(incScore - newScore) > 0.0001) {
			System.out.println("BeforeIncUpdate=" + BeforeIncUpdate + ", after update: incScore="+ incScore + ", correctScore=" + newScore);	
		}		
		return currentScore;
	}
}
