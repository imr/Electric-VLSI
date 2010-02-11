/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BoundingBoxMetric.java
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

import java.util.Map;

/**
 * Class that implements a bounding box net length approximation
 */
public final class BoundingBoxMetric extends Metric
{	
	/**
	 * Method that calculates the bounding box net length approximation for a given net
	 * @param net 
	 * @return bounding box metric of the net
	 */
	@Override
	public double netLength( PlacementNetwork net )
	{
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

		for ( PlacementPort port : net.getPortsOnNet() ) {
			double currX = port.getPlacementNode().getPlacementX() + port.getRotatedOffX();
			double currY = port.getPlacementNode().getPlacementY() + port.getRotatedOffY();

			if ( currX < minX ) minX = currX;
			if ( currY < minY ) minY = currY;
			if ( currX > maxX ) maxX = currX;
			if ( currY > maxY ) maxY = currY;
		}

		return Math.abs(maxX - minX) + Math.abs(maxY - minY);
	}
	
	/**
	 * Method that calculates the bounding box net length approximation for a given net.
	 * It hashes the nodes of the ports in the nets to its proxies.
	 * Also, it may substitute a node with another one just for this calculation
	 * 
	 * @param net
	 * @param proxyMap a map that maps a node to its proxy node
	 * @param originals
	 * @param replacements
	 * @return bounding box metric of the proxied net with regards to the substitutions
	 */
	@Override
	public double netLength( PlacementNetwork net, Map<PlacementNode, ProxyNode> proxyMap, ProxyNode[] originals, ProxyNode[] replacements )
	{
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

		for ( PlacementPort port : net.getPortsOnNet() ) {
			ProxyNode proxy = proxyMap.get( port.getPlacementNode() );
			
			for(int i = 0; i < originals.length; i++)
				if(proxy == originals[i]) proxy = replacements[i];
			
			double currX = proxy.getPlacementX() + port.getRotatedOffX();
			double currY = proxy.getPlacementY() + port.getRotatedOffY();

			if ( currX < minX ) minX = currX;
			if ( currY < minY ) minY = currY;
			if ( currX > maxX ) maxX = currX;
			if ( currY > maxY ) maxY = currY;
		}

		return Math.abs(maxX - minX) + Math.abs(maxY - minY);
	}
}
