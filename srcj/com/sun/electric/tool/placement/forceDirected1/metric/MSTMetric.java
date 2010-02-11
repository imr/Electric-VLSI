/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MSTMetric.java
 * Written by Team 5: Andreas Wagner
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
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Estimate the wirelength using the Minimum Spanning Tree Metric
 */
public class MSTMetric extends AbstractMetric
{
	
	@Override
	public String getMetricName() { return "MST Metric"; }
	
	@Override
	public double compute() {
		return compute(getPositionsOfPorts(allNetworks));
	}

	/**
	 * Method returns the absolute Position of all Port in the list placementNets
	 * It is used to create the data structure for placement.metrics
	 * @param placementNets
	 * @return
	 */
	private List<Point2D.Double[]> getPositionsOfPorts(List<PlacementNetwork> placementNets) {
		LinkedList<Point2D.Double[]> posOfPorts = new LinkedList<Point2D.Double[]>();
		PlacementNetwork net;
		int numberOfPorts = 0;
		for(int i = 0; i < placementNets.size(); i++) {
			net = placementNets.get(i);
			if(net != null) {
				numberOfPorts = net.getPortsOnNet().size();
				Point2D.Double[] allPositionsOfNet = new Point2D.Double[numberOfPorts];
				for(int j = 0; j < numberOfPorts; j++) {
					Point2D.Double absPosOf = getAbsolutePositionOf(net.getPortsOnNet().get(j));
					allPositionsOfNet[j] = absPosOf;
				}
				posOfPorts.add(allPositionsOfNet);
			}
		}
		return posOfPorts;
	}
	
	/**
	 * auxiliary function for getPositionOfPorts
	 * @param placementPort
	 * @return
	 */
	private Point2D.Double getAbsolutePositionOf(PlacementPort placementPort) {
		return new Point2D.Double(
				placementPort.getRotatedOffX() + placementPort.getPlacementNode().getPlacementX(),
				placementPort.getRotatedOffY() + placementPort.getPlacementNode().getPlacementY());
	}
	
	/**
	 * Calculates the length of the MST for each Network(euclidean distance)
	 * and returns the sum of all lengths
	 * @param positionsOfNetworks	list of Networks
	 * @return sum of lengths of the MST for each Network(euclidean distance)
	 */
	public double compute(List<Point2D.Double[]> positionsOfNetworks) {
		double result = 0;
		for(int i = 0; i < positionsOfNetworks.size(); i++) {
			result += compute(positionsOfNetworks.get(i));
		}
		return result;
	}
	
	/**
	 * calculates length of Minimum Spanning Tree
	 * Manhattan Distance is used
	 * @param positionsOfPorts
	 * @return the length of Minimum Spanning Tree
	 */
	public double compute(Point2D.Double[] positionsOfPorts) {
		double result = 0;
		
		//create Comparator
		LinkComparator linkComp = new LinkComparator(positionsOfPorts);
		
		//create Links in PriorityQueue
		PriorityQueue<Link> allLinks = new PriorityQueue<Link>(positionsOfPorts.length, linkComp );	
		for(int i = 0; i < positionsOfPorts.length; i++) {
			for(int j = i + 1; j < positionsOfPorts.length; j++) {
				allLinks.add(new Link(i, j, positionsOfPorts));
			}
		}
		

		
		/* Kruskal Algorithm to find the Minimum Spanning Tree.
		 * A Union Find Data Structure is used to efficently check if two nodes are in the same set.
		 */
		UnionFind uF = new UnionFind(positionsOfPorts.length);
		int numberOfPlacedLinks = 0;
		while(numberOfPlacedLinks < positionsOfPorts.length - 1) {//allLinks.size() > 0) {
			Link currentLink = allLinks.poll();
			int srcSet = uF.find(currentLink.src);
			int destSet = uF.find(currentLink.dest);
			if(srcSet == 0 && destSet == 0) {
				uF.union(uF.makeSet(currentLink.src), uF.makeSet(currentLink.dest));
				result += linkComp.getDistance(currentLink);
				numberOfPlacedLinks++;
			} else if(srcSet == 0 && destSet != 0) {
				uF.union(destSet, uF.makeSet(currentLink.src));
				result += linkComp.getDistance(currentLink);
				numberOfPlacedLinks++;
			} else if(srcSet != 0 && destSet == 0) {
				uF.union(srcSet, uF.makeSet(currentLink.dest));
				result += linkComp.getDistance(currentLink);
				numberOfPlacedLinks++;
			} else if(srcSet != destSet) {
				uF.union(srcSet, destSet);
				result += linkComp.getDistance(currentLink);
				numberOfPlacedLinks++;
			}
		}
		return result;
	}

	/**
	 * caller must ensure that nodes called in makesSet are not yet in any set.
	 * a tree structure is implemented to efficiently find elements
	 * further improvements in big O-notation could be made by implementing path compression
	 * union: 	O(1)
	 * find:  	O(log(n))
	 * makeSet:	O(1)
	 */
	private class UnionFind {
		int[] nodes;
		
		UnionFind(int length) {
			nodes = new int[length];
		}

		//returns the set a node is in
		int find(int src) {
			if(nodes[src] < 0) {
				return src;
			}
			if(nodes[src] == 0) {
				return 0;
			}
			int temp = nodes[src];
			while(nodes[temp] > 0) {
				temp = nodes[temp];
			}
			return temp;
		}
		
		// merges the two sets
		int union(int set1, int set2) {
			if(nodes[set1] < nodes[set2]) {	//more nodes in set1
				nodes[set1] += nodes[set2];	//update size of set
				nodes[set2] = set1;
				return set1;
			} else {						//more nodes in set2
				nodes[set2] += nodes[set1];	//update size of set
				nodes[set1] = set2;
				return set2;
			}
		}
		
		//makes a new set with node1
		int makeSet(int node1) {
			nodes[node1] = -1;
			return node1;
		}
		
		//makes a new set with node1 and node2
		int makeSet(int node1, int node2) {
			nodes[node1] = -2;
			nodes[node2] = node1;
			return node1;
		}
		
	}
	
	/**
	 * represents a Link consisting of two Points
	 * src and dest represent the index in the list which contain the nodes
	 * The Nodes are represented by a index to better fit the Union Find data structure
	 */
	private class Link{
		int src;
		int dest;
		
		Link(int src, int dest, Point2D.Double[] list) {
			this.src = src;
			this.dest = dest;
		}	
	}
	
	/**
	 * a Comparator Class for comparing two Points
	 */
	private class LinkComparator implements Comparator<Link> {
		Point2D.Double[] list;
		
		//pass a list to the comparator, so that compare only needs indices as arguments
		LinkComparator(Point2D.Double[] list) {
			this.list = list;
		}
		
		public int compare(Link arg0, Link arg1) {
			int result = java.lang.Double.compare(getDistance(arg0), this.getDistance(arg1));
			return result;
		}
		
		/*
		 * calculates the manhattan distance between two points
		 */
		public double getDistance(Link arg0) {
			double deltaX = Math.abs(list[arg0.src].getX() - list[arg0.dest].getX());
			double deltaY = Math.abs(list[arg0.src].getY() - list[arg0.dest].getY());
			
			return Math.sqrt(deltaX*deltaX + deltaY*deltaY);
		}		
	}
}
