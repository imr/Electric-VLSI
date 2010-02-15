/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MetricBoundingBox3.java
 * Written by Team 3: Christian Wittner
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
package com.sun.electric.tool.placement.genetic1.g1;

import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.PlacementFrame.PlacementPort;
import com.sun.electric.tool.placement.genetic1.Chromosome;
import com.sun.electric.tool.placement.genetic1.Metric;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 * Calculate Bounding Box metric for all networks in a chromosome.
 */
public class MetricBoundingBox3 implements Metric {

	// short[] represents ports x and y offset
	static HashMap<int[], Integer> port2ProxyIndexMap;

	// first level is net level
	// second level is port level
	// last level represents x and y offset
	static int[][][] net2PlacementPortsXYOffset;

	static Level LOG_LEVEL = Level.FINER;

	public static double cutOffThreshhold = .85;

	public static int nodeThreshhold = 100;

	public MetricBoundingBox3(List<PlacementNetwork> networks,
			PlacementNodeProxy[] nodeProxies) {
		if (port2ProxyIndexMap == null)
			generatePortToProxyIndexMap(networks, nodeProxies);
	}

	void generatePortToProxyIndexMap(List<PlacementNetwork> networks,
			PlacementNodeProxy[] nodeProxies) {

		if (GeneticPlacement.IS_LOGGING_ENABLED)
			GeneticPlacement.logger.log(LOG_LEVEL,
					"start precalculating portToProxyIndexMap");

		int allocationCounter = 0;
		int cutOffBarrier = (int) (cutOffThreshhold * nodeProxies.length);
		// if node number over defined threshold ignore nets with more than
		// cutoff percentage of connected nodes
		// idea: it doesn't make sense to optimize a bounding box for a net
		// containing all nodes
		// if a node is moved within the chip the outer bounding box doesn't get
		// smaller.
		// it is a waste of cpu time in metric calculation.
		if (nodeProxies.length > nodeThreshhold) {
			// calculate number of nets below cutoff for array allocation

			for (PlacementNetwork net : networks)
				if (net.getPortsOnNet().size() <= cutOffBarrier)
					allocationCounter++;
		} else
			allocationCounter = networks.size();

		net2PlacementPortsXYOffset = new int[allocationCounter][][];

		port2ProxyIndexMap = new HashMap<int[], Integer>(networks.size());

		PlacementNetwork curNet;
		PlacementPort curPort;
		int netIndex = 0;
		for (int i = 0; i < networks.size(); i++) {
			curNet = networks.get(i);

			// if cutting of and network to big skip it
			if (nodeProxies.length > nodeThreshhold
					&& curNet.getPortsOnNet().size() > cutOffBarrier)
				continue;

			// create array of all ports on that network
			net2PlacementPortsXYOffset[netIndex] = new int[curNet.getPortsOnNet()
					.size()][];

			for (int p = 0; p < curNet.getPortsOnNet().size(); p++) {

				curPort = curNet.getPortsOnNet().get(p);
				// store ports x and y offset in map
				net2PlacementPortsXYOffset[netIndex][p] = new int[2];
				net2PlacementPortsXYOffset[netIndex][p][0] = (short) curPort.getOffX();
				net2PlacementPortsXYOffset[netIndex][p][1] = (short) curPort.getOffY();

				List<PlacementNodeProxy> nodeProxyList = Arrays
						.asList(nodeProxies);

				// TODO: eventual introduce a port proxy to store port2Proxy
				// mapping
				// Definitely take care to do this calculation only once and not
				// for all threads!!
				// create map to find gene representing a node containing a port
				if (!port2ProxyIndexMap
						.containsKey(net2PlacementPortsXYOffset[netIndex][p])) {
					for (PlacementNodeProxy proxy : nodeProxies) {
						if (proxy.node == curPort.getPlacementNode()) {
							port2ProxyIndexMap.put(
									net2PlacementPortsXYOffset[netIndex][p],
									new Integer(nodeProxyList.indexOf(proxy)));
							break;
						}
					}
				}
			}
			netIndex++;
		}

		if (GeneticPlacement.IS_LOGGING_ENABLED)
			GeneticPlacement.logger.log(LOG_LEVEL,
					"done precalculating portToProxyIndexMap");
	}

	public void evaluate(List<Chromosome> population) {
		for (Chromosome c : population) {
			if (c.altered)
				c.fitness = Double.valueOf(0);
		}

		// TODO:Loop Locking auf Targetmachine anpassen :)
		// kann man cachezeilen groesse abfragen?
		for (int[][] net : net2PlacementPortsXYOffset) {
			for (Chromosome c : population) {
				if (c.altered)
					c.fitness = new Double(c.fitness.doubleValue() + compute(net, c));
			}
		}
		for (Chromosome c : population) {
			c.altered = false;
		}

	}

	public double evaluate(Chromosome c) {

		double sum = 0;

		for (int[][] net : net2PlacementPortsXYOffset) {

			// ignore nets which got only one port assigned
			if (net.length == 1)
				continue;

			sum += compute(net, c);
		}

		assert sum != 0;

		return sum;
	}

	private int getPortXOffset(int port[], short geneRotation) {
		// port[0] is it's x offset port[1] the y offset
		switch (geneRotation) {
		case 0:
			return port[0];
		case 900:
			return -port[1];
		case 1800:
			return -port[0];
		case 2700:
			return port[1];
		default:
			System.err.println(this.getClass().getName()
					+ " unsupported rotation angle: " + geneRotation);
			return -1;
		}
	}

	public int getPortYOffset(int port[], short geneRotation) {
		// port[0] is it's x offset port[1] the y offset
		switch (geneRotation) {
		case 0:
			return port[1];
		case 900:
			return port[0];
		case 1800:
			return -port[1];
		case 2700:
			return -port[0];
		default:
			System.err.println(this.getClass().getName()
					+ " unsupported rotation angle: " + geneRotation);
			return -1;
		}

	}

	private double compute(int[][] net, Chromosome c) {

		double left, right, top, bottom;
		double portX, portY;
		int indexOfProxy;

		// values of first cell as starting point
		{
			indexOfProxy = port2ProxyIndexMap.get(net[0]).intValue();
			short rotation = 0;
			int addendX = 0, addendY = 0;
			if (indexOfProxy < c.GeneRotation.length)
			{
				rotation = c.GeneRotation[indexOfProxy];
				addendX = c.GeneXPos[indexOfProxy];
				addendY = c.GeneYPos[indexOfProxy];
			}
			portX = getPortXOffset(net[0], rotation) + addendX;
			portY = getPortYOffset(net[0], rotation) + addendY;

			left = right = portX;
			top = bottom = portY;
		}

		// calculate rest of cells

		for (int i = 1; i < net.length; i++) {
			indexOfProxy = port2ProxyIndexMap.get(net[i]).intValue();
			short rotation = 0;
			int addendX = 0, addendY = 0;
			if (indexOfProxy < c.GeneRotation.length)
			{
				rotation = c.GeneRotation[indexOfProxy];
				addendX = c.GeneXPos[indexOfProxy];
				addendY = c.GeneYPos[indexOfProxy];
			}
			portX = getPortXOffset(net[i], rotation) + addendX;
			portY = getPortYOffset(net[i], rotation) + addendY;

			if (portX < left) {
				left = portX;
			} else if (portX > right) {
				right = portX;
			}

			if (portY > top) {
				top = portY;
			} else if (portY < bottom) {
				bottom = portY;
			}
		}

		assert (right > left || top > bottom);

		return (right - left) + (top - bottom);
	}
}
