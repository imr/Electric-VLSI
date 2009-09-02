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

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PlacementFrame
{
	private static List<PlacementFrame> placementAlgorithms = new ArrayList<PlacementFrame>();

	static
	{
		placementAlgorithms.add(new PlacementMinCut());
		placementAlgorithms.add(new PlacementSimple());
	}

	public static List<PlacementFrame> getPlacementAlgorithms() { return placementAlgorithms; }

	void runPlacement(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks) {}

	public String getAlgorithmName() { return "?"; }

	protected static class PlacementNode
	{
		private NodeProto proto;
		private int techSpecific;
		private double width, height;
		private List<PlacementPort> ports;
		private double xPos, yPos;
		private Orientation orient;

		PlacementNode(NodeProto np, int ts, double w, double h, List<PlacementPort> pl)
		{
			proto = np;
			techSpecific = ts;
			width = w;
			height = h;
			ports = pl;
		}

		List<PlacementPort> getPorts() { return ports; }

		double getWidth() { return width; }

		double getHeight() { return height; }

		NodeProto getProto() { return proto; }

		int getTechSpecific() { return techSpecific; }

		void setPlacement(double x, double y) { xPos = x;   yPos = y; }

		void setOrientation(Orientation o)
		{
			orient = o;
			for(PlacementPort plPort : ports)
				plPort.computeRotatedOffset();
		}

		double getPlacementX() { return xPos; }

		double getPlacementY() { return yPos; }

		Orientation getPlacementOrientation() { return orient; }
	}

	protected static class PlacementPort
	{
		private double offX, offY;
		private double rotatedOffX, rotatedOffY;
		private PlacementNode plNode;
		private PlacementNetwork plNet;
		private PortProto proto;

		PlacementPort(double x, double y, PortProto pp)
		{
			offX = x;   offY = y;
			proto = pp;
		}

		void setPlacementNode(PlacementNode pn) { plNode = pn; }

		PlacementNode getPlacementNode() { return plNode; }

		void setPlacementNetwork(PlacementNetwork pn) { plNet = pn; }

		PlacementNetwork getPlacementNetwork() { return plNet; }

		PortProto getPortProto() { return proto; }

		double getOffX() { return offX; }

		double getOffY() { return offY; }

		double getRotatedOffX() { return rotatedOffX; }

		double getRotatedOffY() { return rotatedOffY; }

		void computeRotatedOffset()
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
	}

	protected static class PlacementNetwork
	{
		private List<PlacementPort> portsOnNet;

		PlacementNetwork(List<PlacementPort> ports)
		{
			portsOnNet = ports;
		}

		List<PlacementPort> getPortsOnNet() { return portsOnNet; }
	}

	public Cell doPlacement(Cell cell)
	{
		Netlist netList = cell.acquireUserNetlist();
		if (netList == null)
		{
			System.out.println("Sorry, a deadlock aborted routing (network information unavailable).  Please try again");
			return null;
		}

		// find all cells to be placed
		List<PlacementNode> nodesToPlace = new ArrayList<PlacementNode>();
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
				PlacementNode plNode = new PlacementNode(np, ni.getTechSpecific(), np.getDefWidth(), np.getDefHeight(), pl);
				nodesToPlace.add(plNode);
				for(PlacementPort plPort : pl)
					plPort.setPlacementNode(plNode);
				plNode.setOrientation(Orientation.IDENT);
				convertedNodes.put(ni, placedPorts);
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
			NodeProto np = plNode.getProto();
			if (np instanceof Cell)
			{
				Cell placementCell = (Cell)np;
				ERectangle bounds = placementCell.getBounds();
				xPos += bounds.getCenterX();
				yPos += bounds.getCenterY();
			}
			NodeInst ni = NodeInst.makeInstance(np, new EPoint(xPos, yPos), np.getDefWidth(), np.getDefHeight(), newCell,
				orient, null, plNode.getTechSpecific());
			placedNodes.put(plNode, ni);
		}

		// connect the nodes in the new cell
		placeConnections(allNetworks, placedNodes);

		return newCell;
	}

	private void placeConnections(List<PlacementNetwork> allNetworks, Map<PlacementNode,NodeInst> placedNodes)
	{
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
	}
}
