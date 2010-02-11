/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MSTMetric.java
 * Written by Team 7: Felix Schmidt, Daniel Lechner
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
package com.sun.electric.tool.placement.forceDirected2.metrics;

import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;
import com.sun.electric.tool.placement.PlacementFrame.PlacementPort;

import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Parallel Placement
 */
public class MSTMetric extends AbstractMetric {

	/*
	 * Kruskal's algorithm finds a minimum spanning tree for a connected
	 * weighted graph. The program below uses a hard-coded example. You can
	 * change this to match your problem by changing the edges in the graph. The
	 * program uses 3 classes. The Kruskal class contains the main method. The
	 * Edge class represents an edge. The KruskalEdges class contains the edges
	 * determined by the Kruskal algorithm.
	 */
	class Edge implements Comparable<Edge> {
		PlacementNode vertexA, vertexB;
		double weight;

		public Edge(PlacementNode vertexA, PlacementNode vertexB, double weight) {
			this.vertexA = vertexA;
			this.vertexB = vertexB;
			this.weight = weight;
		}

		public int compareTo(Edge edge) {
			// == is not compared so that duplicate values are not eliminated.
			return (this.weight < edge.weight) ? -1 : 1;
		}

		public PlacementNode getVertexA() {
			return this.vertexA;
		}

		public PlacementNode getVertexB() {
			return this.vertexB;
		}

		public double getWeight() {
			return this.weight;
		}

		@Override
		public String toString() {
			return "(" + this.vertexA + ", " + this.vertexB + ") : Weight = " + this.weight;
		}
	}

	class KruskalEdges {
		Vector<HashSet<PlacementNode>> vertexGroups = new Vector<HashSet<PlacementNode>>();
		TreeSet<Edge> kruskalEdges = new TreeSet<Edge>();

		public TreeSet<Edge> getEdges() {
			return this.kruskalEdges;
		}

		HashSet<PlacementNode> getVertexGroup(PlacementNode vertex) {
			for (HashSet<PlacementNode> vertexGroup : this.vertexGroups) {
				if (vertexGroup.contains(vertex)) {
					return vertexGroup;
				}
			}
			return null;
		}

		/**
		 * The edge to be inserted has 2 vertices - A and B We maintain a vector
		 * that contains groups of vertices. We first check if either A or B
		 * exists in any group If neither A nor B exists in any group We create
		 * a new group containing both the vertices. If one of the vertices
		 * exists in a group and the other does not We add the vertex that does
		 * not exist to the group of the other vertex If both vertices exist in
		 * different groups We merge the two groups into one All of the above
		 * scenarios mean that the edge is a valid Kruskal edge In that
		 * scenario, we will add the edge to the Kruskal edges However, if both
		 * vertices exist in the same group We do not consider the edge as a
		 * valid Kruskal edge
		 */
		public void insertEdge(Edge edge) {
			PlacementNode vertexA = edge.getVertexA();
			PlacementNode vertexB = edge.getVertexB();

			HashSet<PlacementNode> vertexGroupA = this.getVertexGroup(vertexA);
			HashSet<PlacementNode> vertexGroupB = this.getVertexGroup(vertexB);

			if (vertexGroupA == null) {
				this.kruskalEdges.add(edge);
				if (vertexGroupB == null) {
					HashSet<PlacementNode> htNewVertexGroup = new HashSet<PlacementNode>();
					htNewVertexGroup.add(vertexA);
					htNewVertexGroup.add(vertexB);
					this.vertexGroups.add(htNewVertexGroup);
				} else {
					vertexGroupB.add(vertexA);
				}
			} else {
				if (vertexGroupB == null) {
					vertexGroupA.add(vertexB);
					this.kruskalEdges.add(edge);
				} else if (vertexGroupA != vertexGroupB) {
					vertexGroupA.addAll(vertexGroupB);
					this.vertexGroups.remove(vertexGroupB);
					this.kruskalEdges.add(edge);
				}
			}
		}
	}

	public MSTMetric(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks) {
		super(nodesToPlace, allNetworks);
	}

	@Override
	public Double compute() {
		double total = 0;
		for (PlacementNetwork net : this.allNetworks) {
			total += this.compute(net);
		}

		return new Double(total);
	}

	/**
	 * 
	 * @param net
	 * @return
	 */
	private double compute(PlacementNetwork net) {

		// TreeSet is used to sort the edges before passing to the algorithm
		TreeSet<Edge> edges = new TreeSet<Edge>();

		for (PlacementPort port1 : net.getPortsOnNet()) {
			for (PlacementPort port2 : net.getPortsOnNet()) {
				edges.add(new Edge(port1.getPlacementNode(), port2.getPlacementNode(), this.getDistance(port1, port2)));
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

	/**
	 * Calculate the distance between two PlacementPort objects
	 * 
	 * @param port1
	 * @param port2
	 * @return
	 */
	private double getDistance(PlacementPort port1, PlacementPort port2) {
		double deltaX = this.getPlacementX(port1) - this.getPlacementX(port2);
		double deltaY = this.getPlacementY(port1) - this.getPlacementY(port2);
		return Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
	}

	@Override
	public String getMetricName() {
		return "MSTMetric";
	}

	// Quelle: http://www.krishami.com/programs/java/kruskal.aspx

	/**
	 * Calculate the absolute position of a placementPort (x direction)
	 * 
	 * @param port
	 * @return absolut x position
	 */
	private double getPlacementX(PlacementPort port) {
		double nodeX = port.getPlacementNode().getPlacementX();
		double portX = port.getRotatedOffX();
		return nodeX + portX;
	}

	/**
	 * Calculate the absolute position of a placementPort (y direction)
	 * 
	 * @param port
	 * @return absolut y position
	 */
	private double getPlacementY(PlacementPort port) {
		double nodeY = port.getPlacementNode().getPlacementY();
		double portY = port.getRotatedOffY();
		return nodeY + portY;
	}
}