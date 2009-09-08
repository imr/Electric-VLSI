/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PlacementFrame.java
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.placement;

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Generic;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Class to define a framework for Placement algorithms.
 * To make Placement algorithms easier to write, all Placement algorithms must extend this class.
 * The Placement algorithm then defines two methods:
 *
 *    public String getAlgorithmName()
 *       returns the name of the Placement algorithm
 *
 *    void runPlacement(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks)
 *       runs the placement on the "nodesToPlace", calling each PlacementNode's "setPlacement()"
 *       and "setOrientation()" methods to establish the proper placement.
 *
 * To avoid the complexities of the Electric database, three shadow-classes are defined that
 * describe the necessary information that Placement algorithms want:
 *
 * PlacementNode is an actual node (primitive or cell instance) that is to be placed.
 * PlacementPort is the connection site on the PlacementNode.
 *    Each PlacementNode has a list of zero or more PlacementPort objects,
 *    and each PlacementPort points to its "parent" PlacementNode.
 * PlacementNetwork determines which PlacementPort objects will connect together.
 *    Each PlacementNetwork has a list of two or more PlacementPort objects that it connects,
 *    and each PlacementPort has a PlacementNetwork in which it resides.
 */
public class PlacementFrame
{
	/**
	 * Static list of all Placement algorithms.
	 * When you create a new algorithm, add it to the following list.
	 */
	private static PlacementFrame [] placementAlgorithms = {
		new PlacementMinCut(),
		new PlacementSimple()
	};

	/**
	 * Method to return a list of all Placement algorithms.
	 * @return a list of all Placement algorithms.
	 */
	public static PlacementFrame [] getPlacementAlgorithms() { return placementAlgorithms; }

	/**
	 * Method to do Placement (overridden by actual Placement algorithms).
	 * @param nodesToPlace a list of all nodes that are to be placed.
	 * @param allNetworks a list of all networks that connect the nodes.
	 */
	protected void runPlacement(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks) {}

	/**
	 * Method to return the name of the placement algorithm (overridden by actual Placement algorithms).
	 * @return the name of the placement algorithm.
	 */
	public String getAlgorithmName() { return "?"; }

	/**
	 * Class to define a node that is being placed.
	 * This is a shadow class for the internal Electric object "NodeInst".
	 * There are minor differences between PlacementNode and NodeInst,
	 * for example, PlacementNode is presumed to be centered in the middle, with
	 * port offsets based on that center, whereas the NodeInst has a cell-center
	 * that may not be in the middle.
	 */
	protected static class PlacementNode
	{
		private NodeInst original;
		private double width, height;
		private List<PlacementPort> ports;
		private double xPos, yPos;
		private Orientation orient;

		/**
		 * Method to create a PlacementNode object.
		 * @param orig the original Electric NodeInst of this PlacementNode.
		 * @param wid the width of this PlacementNode.
		 * @param hei the height of this PlacementNode.
		 * @param pps a list of PlacementPort on the PlacementNode, indicating connection locations.
		 */
		PlacementNode(NodeInst orig, double wid, double hei, List<PlacementPort> pps)
		{
			original = orig;
			width = wid;
			height = hei;
			ports = pps;
		}

		/**
		 * Method to return a list of PlacementPorts on this PlacementNode.
		 * @return a list of PlacementPorts on this PlacementNode.
		 */
		List<PlacementPort> getPorts() { return ports; }

		/**
		 * Method to return the width of this PlacementNode.
		 * @return the width of this PlacementNode.
		 */
		double getWidth() { return width; }

		/**
		 * Method to return the height of this PlacementNode.
		 * @return the height of this PlacementNode.
		 */
		double getHeight() { return height; }

		/**
		 * Method to return the original NodeInst of this PlacementNode.
		 * @return the original NodeInst of this PlacementNode.
		 */
		NodeInst getOriginal() { return original; }

		/**
		 * Method to set the location of this PlacementNode.
		 * The Placement algorithm must call this method to set the final location of the PlacementNode.
		 * @param x the X-coordinate of the center of this PlacementNode.
		 * @param y the Y-coordinate of the center of this PlacementNode.
		 */
		void setPlacement(double x, double y) { xPos = x;   yPos = y; }

		/**
		 * Method to set the orientation (rotation and mirroring) of this PlacementNode.
		 * The Placement algorithm may call this method to set the final orientation of the PlacementNode.
		 * @param o the Orientation of this PlacementNode.
		 */
		void setOrientation(Orientation o)
		{
			orient = o;
			for(PlacementPort plPort : ports)
				plPort.computeRotatedOffset();
		}

		/**
		 * Method to return the X-coordinate of the placed location of this PlacementNode.
		 * This is the location that the Placement algorithm has established for this PlacementNode.
		 * @return the X-coordinate of the placed location of this PlacementNode.
		 */
		double getPlacementX() { return xPos; }

		/**
		 * Method to return the Y-coordinate of the placed location of this PlacementNode.
		 * This is the location that the Placement algorithm has established for this PlacementNode.
		 * @return the Y-coordinate of the placed location of this PlacementNode.
		 */
		double getPlacementY() { return yPos; }

		/**
		 * Method to return the Orientation of this PlacementNode.
		 * This is the Orientation that the Placement algorithm has established for this PlacementNode.
		 * @return the Orientation of this PlacementNode.
		 */
		Orientation getPlacementOrientation() { return orient; }

		public String toString() { return original.describe(false); }
	}

	/**
	 * Class to define ports on PlacementNode objects.
	 * This is a shadow class for the internal Electric object "PortInst".
	 */
	protected static class PlacementPort
	{
		private double offX, offY;
		private double rotatedOffX, rotatedOffY;
		private PlacementNode plNode;
		private PlacementNetwork plNet;
		private PortProto proto;

		/**
		 * Constructor to create a PlacementPort.
		 * @param x the X offset of this PlacementPort from the center of its PlacementNode.
		 * @param y the Y offset of this PlacementPort from the center of its PlacementNode.
		 * @param pp the Electric PortProto of this PlacementPort.
		 */
		PlacementPort(double x, double y, PortProto pp)
		{
			offX = x;   offY = y;
			proto = pp;
		}

		/**
		 * Method to set the "parent" PlacementNode on which this PlacementPort resides.
		 * @param pn the PlacementNode on which this PlacementPort resides.
		 */
		void setPlacementNode(PlacementNode pn) { plNode = pn; }

		/**
		 * Method to return the PlacementNode on which this PlacementPort resides.
		 * @return the PlacementNode on which this PlacementPort resides.
		 */
		PlacementNode getPlacementNode() { return plNode; }

		/**
		 * Method to return the PlacementNetwork on which this PlacementPort resides.
		 * @param pn the PlacementNetwork on which this PlacementPort resides.
		 */
		void setPlacementNetwork(PlacementNetwork pn) { plNet = pn; }

		/**
		 * Method to return the PlacementNetwork on which this PlacementPort resides.
		 * @return the PlacementNetwork on which this PlacementPort resides.
		 */
		PlacementNetwork getPlacementNetwork() { return plNet; }

		/**
		 * Method to return the Electric PortProto that this PlacementPort uses.
		 * @return the Electric PortProto that this PlacementPort uses.
		 */
		PortProto getPortProto() { return proto; }

		/**
		 * Method to return the offset of this PlacementPort's X coordinate from the center of its PlacementNode.
		 * The offset is valid when no Orientation has been applied.
		 * @return the offset of this PlacementPort's X coordinate from the center of its PlacementNode.
		 */
		double getOffX() { return offX; }

		/**
		 * Method to return the offset of this PlacementPort's Y coordinate from the center of its PlacementNode.
		 * The offset is valid when no Orientation has been applied.
		 * @return the offset of this PlacementPort's Y coordinate from the center of its PlacementNode.
		 */
		double getOffY() { return offY; }

		/**
		 * Method to return the offset of this PlacementPort's X coordinate from the center of its PlacementNode.
		 * The coordinate assumes that the PlacementNode has been rotated by its Orientation.
		 * @return the offset of this PlacementPort's X coordinate from the center of its PlacementNode.
		 */
		double getRotatedOffX() { return rotatedOffX; }

		/**
		 * Method to return the offset of this PlacementPort's Y coordinate from the center of its PlacementNode.
		 * The coordinate assumes that the PlacementNode has been rotated by its Orientation.
		 * @return the offset of this PlacementPort's Y coordinate from the center of its PlacementNode.
		 */
		double getRotatedOffY() { return rotatedOffY; }

		/**
		 * Internal method to compute the rotated offset of this PlacementPort
		 * assuming that the Orientation of its PlacementNode has changed.
		 */
		private void computeRotatedOffset()
		{
			Orientation orient = plNode.getPlacementOrientation();
			if (orient == Orientation.IDENT)
			{
				rotatedOffX = offX;
				rotatedOffY = offY;
				return;
			}
			AffineTransform trans = orient.pureRotate();
			Point2D offset = new Point2D.Double(offX, offY);
			trans.transform(offset, offset);
			rotatedOffX = offset.getX();
			rotatedOffY = offset.getY();
		}

		public String toString() { return proto.getName(); }
	}

	/**
	 * Class to define networks of PlacementPort objects.
	 * This is a shadow class for the internal Electric object "Network", but it is simplified for Placement.
	 */
	protected static class PlacementNetwork
	{
		private List<PlacementPort> portsOnNet;

		/**
		 * Constructor to create this PlacementNetwork with a list of PlacementPort objects that it connects.
		 * @param ports a list of PlacementPort objects that it connects.
		 */
		PlacementNetwork(List<PlacementPort> ports)
		{
			portsOnNet = ports;
		}

		/**
		 * Method to return the list of PlacementPort objects on this PlacementNetwork.
		 * @return a list of PlacementPort objects on this PlacementNetwork.
		 */
		List<PlacementPort> getPortsOnNet() { return portsOnNet; }
	}

	/**
	 * Method to do Placement.
	 * Gathers the requirements for Placement into a collection of shadow objects
	 * (PlacementNode, PlacementPort, and PlacementNetwork).
	 * Then invokes "runPlacement()" in the subclass to do the actual work.
	 * @param cell the Cell to place.
	 * Objects in that Cell will be reorganized in and placed in a new Cell.
	 * @return the new Cell with the placement results.
	 */
	public Cell doPlacement(Cell cell)
	{
		// get network information for the Cell
		Netlist netList = cell.acquireUserNetlist();
		if (netList == null)
		{
			System.out.println("Sorry, a deadlock aborted routing (network information unavailable).  Please try again");
			return null;
		}

		// find all nodes in the cell that are to be placed
		List<PlacementNode> nodesToPlace = new ArrayList<PlacementNode>();
		Map<NodeInst,PlacementNode> shadowNodes = new HashMap<NodeInst,PlacementNode>();
		Map<NodeInst,Map<PortProto,PlacementPort>> convertedNodes = new HashMap<NodeInst,Map<PortProto,PlacementPort>>();
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (ni.isIconOfParent()) continue;
			boolean validNode = ni.isCellInstance();
			if (!validNode)
			{
				if (ni.getProto().getTechnology() != Generic.tech())
				{
					PrimitiveNode.Function fun = ni.getFunction();
					if (fun != PrimitiveNode.Function.CONNECT && fun != PrimitiveNode.Function.CONTACT &&
						fun != PrimitiveNode.Function.PIN)
							validNode = true;
				}
			}
			if (validNode)
			{
				// make the PlacementNode for this NodeInst
				NodeProto np = ni.getProto();
				List<PlacementPort> pl = new ArrayList<PlacementPort>();
				NodeInst niDummy = NodeInst.makeDummyInstance(np);
				Map<PortProto,PlacementPort> placedPorts = new HashMap<PortProto,PlacementPort>();
				for(Iterator<PortInst> pIt = niDummy.getPortInsts(); pIt.hasNext(); )
				{
					PortInst pi = pIt.next();
					Poly poly = pi.getPoly();
					double offX = poly.getCenterX() - niDummy.getTrueCenterX();
					double offY = poly.getCenterY() - niDummy.getTrueCenterY();
					PlacementPort plPort = new PlacementPort(offX, offY, pi.getPortProto());
					pl.add(plPort);
					placedPorts.put(pi.getPortProto(), plPort);
				}
				PlacementNode plNode = new PlacementNode(ni, np.getDefWidth(), np.getDefHeight(), pl);
				nodesToPlace.add(plNode);
				for(PlacementPort plPort : pl)
					plPort.setPlacementNode(plNode);
				plNode.setOrientation(Orientation.IDENT);
				convertedNodes.put(ni, placedPorts);
				shadowNodes.put(ni, plNode);
			}
		}

		// gather connectivity information
		List<PlacementNetwork> allNetworks = new ArrayList<PlacementNetwork>();
		for(Iterator<Network> it = netList.getNetworks(); it.hasNext(); )
		{
			Network net = it.next();
			List<PlacementPort> portsOnNet = new ArrayList<PlacementPort>();
			for(Iterator<PortInst> pIt = net.getPorts(); pIt.hasNext(); )
			{
				PortInst pi = pIt.next();
				NodeInst ni = pi.getNodeInst();
				PortProto pp = pi.getPortProto();
				Map<PortProto,PlacementPort> convertedPorts = convertedNodes.get(ni);
				if (convertedPorts == null) continue;
				PlacementPort plPort = convertedPorts.get(pp);
				if (plPort != null) portsOnNet.add(plPort);
			}
			if (portsOnNet.size() > 0)
			{
				PlacementNetwork plNet = new PlacementNetwork(portsOnNet);
				for(PlacementPort plPort : portsOnNet)
					plPort.setPlacementNetwork(plNet);
				allNetworks.add(plNet);
			}
		}

		// do the real work of placement
		runPlacement(nodesToPlace, allNetworks);

		// create a new cell for the placement results
		Cell newCell = Cell.makeInstance(cell.getLibrary(), cell.noLibDescribe());

		// place the nodes in the new cell
		Map<PlacementNode,NodeInst> placedNodes = new HashMap<PlacementNode,NodeInst>();
		for(PlacementNode plNode : nodesToPlace)
		{
			double xPos = plNode.getPlacementX();
			double yPos = plNode.getPlacementY();
			Orientation orient = plNode.getPlacementOrientation();
			NodeProto np = plNode.getOriginal().getProto();
			if (np instanceof Cell)
			{
				Cell placementCell = (Cell)np;
				Rectangle2D bounds = placementCell.getBounds();
				Point2D centerOffset = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
				orient.pureRotate().transform(centerOffset, centerOffset);
				xPos -= centerOffset.getX();
				yPos -= centerOffset.getY();
			}
			NodeInst ni = NodeInst.makeInstance(np, new Point2D.Double(xPos, yPos), np.getDefWidth(), np.getDefHeight(), newCell,
				orient, plNode.getOriginal().getName(), plNode.getOriginal().getTechSpecific());
			placedNodes.put(plNode, ni);
		}

		// place exports in the new cell
		for(Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
		{
			Export e = (Export)it.next();
			NodeInst orig = e.getOriginalPort().getNodeInst();
			PlacementNode plNode = shadowNodes.get(orig);
			NodeInst newNI = placedNodes.get(plNode);
			PortInst portToExport = newNI.findPortInstFromProto(e.getOriginalPort().getPortProto());
			Export.newInstance(newCell, portToExport, e.getName(), e.getCharacteristic());
		}

		// connect the connections in the new cell
		for(PlacementNetwork plNet : allNetworks)
		{
			List<PortInst> portsToConnect = new ArrayList<PortInst>();
			for(PlacementPort plPort : plNet.getPortsOnNet())
			{
				NodeInst newNi = placedNodes.get(plPort.getPlacementNode());
				if (newNi != null)
				{
					PortInst newPi = newNi.findPortInstFromProto(plPort.getPortProto());
					portsToConnect.add(newPi);
				}
			}

			// make the connections
			for(int i=1; i<portsToConnect.size(); i++)
			{
				PortInst lastPi = portsToConnect.get(i-1);
				PortInst thisPi = portsToConnect.get(i);
				ArcInst.makeInstance(Generic.tech().unrouted_arc, lastPi, thisPi);
			}
		}

		return newCell;
	}
}
