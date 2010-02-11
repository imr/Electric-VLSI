/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MSTMetric.java
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
import com.sun.electric.tool.placement.simulatedAnnealing1.SimulatedAnnealing.IncrementalState;
import com.sun.electric.tool.placement.simulatedAnnealing1.SimulatedAnnealing.PlacementNodePosition;

import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

/** Parallel Placement
 **/
public class MSTMetric extends AbstractMetric {

	private IncrementalState incState;
	private double currentScore;
	
	@Override
	public String getMetricName() { return "MSTMetric"; }
	
	public MSTMetric(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks, IncrementalState incState) {
		super(nodesToPlace, allNetworks);
		this.incState = incState;
	}
	
	public double init(List<PlacementNetwork> allNetworks) {		
		currentScore = compute();
		return currentScore;
	}
	
	/**
	 * Get current score.
	 * @return current score, which is the estimated wire length.
	 */
	public double getCurrentScore() {
		return currentScore;
	}
	
	public double update(int index) {
		currentScore = compute();
		return currentScore;
	}
	
	@Override
	public double compute() {
		double total = 0;
		for(PlacementNetwork net : allNetworks) {
			total += compute(net);
		}
		
		return total;
	}
	
	private double compute(PlacementNetwork net) {
		
        //TreeSet is used to sort the edges before passing to the algorithm
        TreeSet<Edge> edges = new TreeSet<Edge>();
        
		for(PlacementPort port1 : net.getPortsOnNet()) {
			for(PlacementPort port2 : net.getPortsOnNet()) {
				edges.add(new Edge(port1.getPlacementNode().getType().getName(), port2.getPlacementNode().getType().getName(), getDistance(port1, port2)));
			}
		}
		
        KruskalEdges vv = new KruskalEdges();
    	
        for (Edge edge : edges) {
            vv.insertEdge(edge);
        }
        
        double total = 0;
        for (Edge edge : vv.getEdges()) {
            total += edge.getWeight();
        }
        
        return total;
	}
	
	private double getDistance(PlacementPort port1, PlacementPort port2) {
	    PlacementNodePosition n1 = incState.getNodeFromState(nodesToPlace.indexOf(port1.getPlacementNode()));
	    PlacementNodePosition n2 = incState.getNodeFromState(nodesToPlace.indexOf(port2.getPlacementNode()));
		double deltaX = n1.getPlacementX() + port1.getRotatedOffX() - n2.getPlacementX() - port2.getOffX();
		double deltaY = n1.getPlacementY() + port1.getRotatedOffY() - n2.getPlacementY() - port2.getOffY();
		//return Math.sqrt((deltaX*deltaX) + (deltaY*deltaY));
		return ((deltaX*deltaX) + (deltaY*deltaY));
	}
		
// Quelle: http://www.krishami.com/programs/java/kruskal.aspx
	
	/*
	Kruskal's algorithm finds a minimum spanning tree for a connected weighted graph.
	The program below uses a hard-coded example.
	You can change this to match your problem by changing the edges in the graph.
	The program uses 3 classes.
	    The Kruskal class contains the main method.
	    The Edge class represents an edge.
	    The KruskalEdges class contains the edges determined by the Kruskal algorithm.
	*/
	
	class Edge implements Comparable<Edge>
	{
	    String vertexA, vertexB;
	    double weight;
	
	    public Edge(String vertexA, String vertexB, double weight)
	    {
	        this.vertexA = vertexA;
	        this.vertexB = vertexB;
	        this.weight = weight;
	    }
	    public String getVertexA()
	    {
	        return vertexA;
	    }
	    public String getVertexB()
	    {
	        return vertexB;
	    }
	    public double getWeight()
	    {
	        return weight;
	    }
	    @Override
	    public String toString()
	    {
	        return "(" + vertexA + ", " + vertexB + ") : Weight = " + weight;
	    }
	    public int compareTo(Edge edge)
	    {
	    	//== is not compared so that duplicate values are not eliminated. 
	    	return (this.weight < edge.weight) ? -1: 1;
	    }
	}
	
	class KruskalEdges
	{
	    Vector<HashSet<String>> vertexGroups = new Vector<HashSet<String>>();
	    TreeSet<Edge> kruskalEdges = new TreeSet<Edge>();
	
	    public TreeSet<Edge> getEdges()
	    {
	        return kruskalEdges;
	    }
	    HashSet<String> getVertexGroup(String vertex)
	    {
	        for (HashSet<String> vertexGroup : vertexGroups) {
	            if (vertexGroup.contains(vertex)) {
	                return vertexGroup;
	            }
	        }
	        return null;
	    }
	
	    /**
	     * The edge to be inserted has 2 vertices - A and B
	     * We maintain a vector that contains groups of vertices.
	     * We first check if either A or B exists in any group
	     * If neither A nor B exists in any group
	     *     We create a new group containing both the vertices.
	     * If one of the vertices exists in a group and the other does not
	     *     We add the vertex that does not exist to the group of the other vertex
	     * If both vertices exist in different groups
	     *     We merge the two groups into one
	     * All of the above scenarios mean that the edge is a valid Kruskal edge
	     * In that scenario, we will add the edge to the Kruskal edges    
	     * However, if both vertices exist in the same group
	     *     We do not consider the edge as a valid Kruskal edge
	     */
	    public void insertEdge(Edge edge)
	    {
	        String vertexA = edge.getVertexA();
	        String vertexB = edge.getVertexB();
	
	        HashSet<String> vertexGroupA = getVertexGroup(vertexA);
	        HashSet<String> vertexGroupB = getVertexGroup(vertexB);
	
	        if (vertexGroupA == null) {
	            kruskalEdges.add(edge);
	            if (vertexGroupB == null) {
	                HashSet<String> htNewVertexGroup = new HashSet<String>();
	                htNewVertexGroup.add(vertexA);
	                htNewVertexGroup.add(vertexB);
	                vertexGroups.add(htNewVertexGroup);
	            }
	            else {
	                vertexGroupB.add(vertexA);        	
	            }
	        }
	        else {
	            if (vertexGroupB == null) {
	                vertexGroupA.add(vertexB);
	                kruskalEdges.add(edge);
	            }
	            else if (vertexGroupA != vertexGroupB) {
	                vertexGroupA.addAll(vertexGroupB);
	                vertexGroups.remove(vertexGroupB);
	                kruskalEdges.add(edge);
	            }
	        }
	    }
	}
}