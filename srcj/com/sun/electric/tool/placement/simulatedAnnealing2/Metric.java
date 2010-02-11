/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Metric.java
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

import java.util.List;
import java.util.Map;

public abstract class Metric
{
	/**
	 * Method that calculates how much a node overlaps with another node 
	 * @param node1
	 * @param node2
	 * @return The size of the overlapping area
	 */
	public double overlap(ProxyNode node1, ProxyNode node2)
	{	
		// Check if both nodes are close enough in the x-dimension to overlap
		double X1 = node1.getPlacementX();
		double X2 = node2.getPlacementX();
		double width1  = node1.width / 2;
		double width2  = node2.width / 2;
		double distX = Math.abs(X1 - X2);
		double minDistX = width1 + width2;

		if(distX < minDistX)
		{
			// Check if both nodes are close enough in the y-dimension to overlap
			double Y1 = node1.getPlacementY();
			double Y2 = node2.getPlacementY();
			double height1 = node1.height / 2;
			double height2 = node2.height / 2;
			double distY = Math.abs(Y1 - Y2);
			double minDistY = height1 + height2;

			if(distY < minDistY)
			{
				double minX1 = X1 - width1;
				double minX2 = X2 - width2;
				double maxX1 = X1 + width1;
				double maxX2 = X2 + width2;
				double minY1 = Y1 - height1;
				double minY2 = Y2 - height2;
				double maxY1 = Y1 + height1;
				double maxY2 = Y2 + height2;

				return (Math.min(maxX2, maxX1) - Math.max(minX1, minX2)) * (Math.min(maxY2, maxY1) - Math.max(minY1, minY2));
			}
		}
		
		return 0;
	}

	/**
	 * Method that calculates how much a node overlaps with a set of nodes
	 * @param node
	 * @param nodes
	 * @return The sum of the individual overlaps of that node with every node in the set
	 */
	public double overlap( ProxyNode node, List<ProxyNode> nodes ) {
		double overlap = 0;

		for ( ProxyNode n : nodes )
			if(n != node)
				overlap += overlap( node, n );

		return overlap;
	}
	
	/**
	 * Method that calculates how much nodes from a set of nodes overlap with each other
	 * @param nodes
	 * @return The sum of of the individual overlaps of that node with every node in the set
	 */
	public double overlap(List<ProxyNode> nodes)
	{
		double overlap = 0;
		for(int i = 0; i < nodes.size(); i++)
			for(int j = i + 1; j < nodes.size(); j++)
				overlap += overlap( nodes.get(i), nodes.get(j) );
		
		return overlap;
	}
	
	/**
	 * Method that calculates the size of the area cover by a set of nodes.
	 * (i.e. the size of the area of the bounding box of all cells)
	 * @param nodes
	 * @return The size of the used rectangular area
	 */
	public double area( List<ProxyNode> nodes ) {
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

		for ( ProxyNode node : nodes ) {
			double currMinX = node.getPlacementX() - node.width/2;
			double currMinY = node.getPlacementY() - node.height/2;
			double currMaxX = node.getPlacementX() + node.width/2;
			double currMaxY = node.getPlacementY() + node.height/2;

			if ( currMinX < minX ) minX = currMinX;
			if ( currMinY < minY ) minY = currMinY;
			if ( currMaxX > maxX ) maxX = currMaxX;
			if ( currMaxY > maxY ) maxY = currMaxY;
		}

		return ( maxX - minX ) * ( maxY - minY );
	}

	/**
	 * Method that approximates the conductor length of a net
	 * @param network
	 */
	public abstract double netLength(PlacementNetwork network);
	
	/**
	 * Method that approximates the conductor length of a net when proxies are used
	 * @param network
	 * @param proxyMap
	 * @param originals
	 * @param replacements
	 */
	public abstract double netLength(PlacementNetwork network, Map<PlacementNode, ProxyNode> proxyMap, ProxyNode[] originals, ProxyNode[] replacements);
	
	/**
	 * Convenience method
	 * @param network
	 * @param proxyMap
	 */
	public double netLength(PlacementNetwork network,  Map<PlacementNode, ProxyNode> proxyMap)
	{
		return netLength(network, proxyMap, new ProxyNode[]{}, new ProxyNode[]{});
	}

	/**
	 * Method that approximates the conductor length of a set of nets when proxies are used
	 * @param networks
	 * @param proxyMap
	 */
	public double netLength(List<PlacementNetwork> networks, Map<PlacementNode, ProxyNode> proxyMap)
	{
		double length = 0;
		
		for(PlacementNetwork net : networks)
			length += netLength(net, proxyMap, new ProxyNode[]{}, new ProxyNode[]{}) ;
		
		return length;
	}

	/**
	 * Method that approximates the conductor length of a set of nets
	 * @param networks
	 */
	public double netLength(List<PlacementNetwork> networks)
	{
		double length = 0;
		
		for(PlacementNetwork net : networks)
			length += netLength(net) ;
		
		return length;
	}	
}
