/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MSTMetric.java
 * Written by Team 6: Sebastian Roether, Jochen Lutz
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
package com.sun.electric.tool.placement.simulatedAnnealing2;

import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;
import com.sun.electric.tool.placement.PlacementFrame.PlacementPort;

import java.util.Arrays;
import java.util.Map;

public final class MSTMetric extends Metric
{
	Edge[] edges = null;
	UnionFind connected = null;
	
	/**
	 * A connection of two ports and its (squared) euclidean distance
	 */
	private class Edge implements Comparable<Edge>
	{	
		int from;
		int to;
		int length;
		
		public Edge(PlacementPort from, PlacementPort to, int fi, int ti, Map<PlacementNode, ProxyNode> hm, ProxyNode[] originals, ProxyNode[] replacements )
		{
			this.from = fi;
			this.to = ti;

			double x_1, y_1, x_2, y_2;

			if ( hm == null ) {
				x_1 = from.getRotatedOffX() + from.getPlacementNode().getPlacementX();
				y_1 = from.getRotatedOffY() + from.getPlacementNode().getPlacementY();
				x_2 = to.getRotatedOffX() + to.getPlacementNode().getPlacementX();
				y_2 = to.getRotatedOffY() + to.getPlacementNode().getPlacementY();
			}
			else {
				ProxyNode fromNode = hm.get(from.getPlacementNode());
				ProxyNode toNode   = hm.get(to.getPlacementNode());

				for(int i = 0; i < originals.length; i++) {
					if(fromNode == originals[i]) fromNode = replacements[i];
					if(toNode   == originals[i]) toNode   = replacements[i];
				}

				x_1 = from.getRotatedOffX() + fromNode.getPlacementX();
				y_1 = from.getRotatedOffY() + fromNode.getPlacementY();
				x_2 = to.getRotatedOffX() + toNode.getPlacementX();
				y_2 = to.getRotatedOffY() + toNode.getPlacementY();
			}
			
			
			length = (int)(((x_1 - x_2) * (x_1 - x_2) + (y_1 - y_2) * (y_1 - y_2)) * 1000);
		}
		
		public int compareTo(Edge other)
		{
			return this.length - other.length;
		}
	}
	
	// TODO:
	// a=(a,b), b=(a,c) Kanten von a nach b,c
	// Wenn |b| > |a| und winkel < 90° dann ist b Hypothenuse von abc und bc ist kürzer als ac
	// weswegen ac nicht im MST sein kann
	// Kann eigentlich nur 4 Kanten pro Port geben ... sonst wäre vielleicht auch der Algorithmus von Prim besser...
	public MSTMetric( PlacementNetwork net, Map<PlacementNode, ProxyNode> hm, ProxyNode[] originals, ProxyNode[] replacements )
	{
		PlacementPort[] ports = new PlacementPort[net.getPortsOnNet().size()];
		ports = net.getPortsOnNet().toArray(ports);
		connected = new UnionFind(ports.length);

		// create one edge for every possible connection of two ports
		edges = new Edge[(ports.length - 1) * ports.length / 2];
		for(int i = 0, c = 0; i < ports.length; i++)
		{
			for(int j = i + 1; j < ports.length; j++, c++)
			{
				edges[c] = new Edge(ports[i], ports[j], i, j, hm, originals, replacements );
			}
		}
	}
	
	public MSTMetric( PlacementNetwork net ) {
		this( net, null, new ProxyNode[]{}, new ProxyNode[]{} );
	}
	
	/**
	 * Implementation of Kruskal MST algorithm. 
	 * @return total length of the minimum spanning tree
	 */
	public double compute() {
		double length = 0;
		int unions = connected.nodes.length - 1;
		
		Arrays.sort(edges);
		
		// n sets are merged after n - 1 merge operations
		for(int i = 0; i < edges.length && unions > 0; i++)
		{
			int subgraph1 = edges[i].from;
			int subgraph2 = edges[i].to;
			
			// if both ends of the connection are in different sets, the
			// edge is part of the mst
			if(connected.find(subgraph1) != connected.find(subgraph2))
			{
				connected.union(subgraph1, subgraph2);
				unions--;
				length += Math.sqrt((double)edges[i].length / 1000);
			}
		}
		return length;
	}
	
	
	/** 
	 * Union-Find implementation. Used to detect cycles within a graph
	 */
	static class UnionFind {
	    Node[] nodes;
	    Node[] stack;

	    public UnionFind(int size) {
	        nodes = new Node[size];
	        stack = new Node[size];
	    }

	    /**
	     * Searches the disjoint sets for a given integer.  Returns the set
	     * containing the integer a.
	     */
	    private Node findNode(int a) {
	          Node na = nodes[a];

	          if (na == null) {
	              // Start a new set with a
	              Node root = new Node(a);

	              root.child = new Node(a);
	              root.child.parent = root;

	              nodes[a] = root.child;

	              return root;
	          }

	          return findNode(na);
	    }

	    /**
	     * Returns the integer value associated with the first <tt>Node</tt>
	     * in a set.
	     */
	    public int find(int a) {
	        return findNode(a).value;
	    }

	    /**
	     * Finds the set containing a given Node.
	     */
	    private Node findNode(Node node) {
	        int top = 0;

	        // Find the child of the root element.
	        while (node.parent.child == null) {
	            stack[top++] = node;
	            node = node.parent;
	        }

	        // Do path compression on the way back down.
	        Node rootChild = node;

	        while (top > 0) {
	            node = stack[--top];
	            node.parent = rootChild;
	        }

	        return rootChild.parent;
	    }

	    /**
	     * Returns true if a and b are in the same set.
	     */
	    public boolean isEquiv(int a, int b) {
	        return findNode(a) == findNode(b);
	    }

	    /**
	     * Combines the set that contains a with the set that contains b.
	     */
	    public void union(int a, int b) {
	        Node na = findNode(a);
	        Node nb = findNode(b);

	        if (na == nb) {
	            return;
	        }

	        // Link the smaller tree under the larger.
	        if (na.rank > nb.rank) {
	            // Delete nb.
	            nb.child.parent = na.child;
	            na.value = b;
	        }
	        else {
	            // Delete na.
	            na.child.parent = nb.child;
	            nb.value = b;

	            if (na.rank == nb.rank) {
	                nb.rank++;
	            }
	        }
	    }

	    static class Node {
	        Node parent;  // The root of the tree in which this Node resides
	        Node child;
	        int value;
	        int rank;     // This Node's height in the tree

	        public Node(int v) {
	            value = v;
	            rank = 0;
	        }
	    }
	}
	
	

	@Override
	public double netLength(PlacementNetwork network) {
		MSTMetric foo = new MSTMetric(network);
		return foo.compute();
	}

	@Override
	public double netLength(PlacementNetwork network, Map<PlacementNode, ProxyNode> proxyMap, ProxyNode[] originals, ProxyNode[] replacements)
	{
		MSTMetric foo = new MSTMetric( network, proxyMap, originals, replacements );
		return foo.compute();
	}
}