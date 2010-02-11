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

import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.Variable.Key;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.placement.forceDirected1.PlacementForceDirectedTeam5;
import com.sun.electric.tool.placement.forceDirected2.PlacementForceDirectedStaged;
import com.sun.electric.tool.placement.genetic1.g1.GeneticPlacement;
import com.sun.electric.tool.placement.genetic2.PlacementGenetic;
import com.sun.electric.tool.placement.simulatedAnnealing1.SimulatedAnnealing;
import com.sun.electric.tool.placement.simulatedAnnealing2.PlacementSimulatedAnnealing;
import com.sun.electric.tool.user.IconParameters;

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
 *    void runPlacement(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks, String cellName)
 *       runs the placement on the "nodesToPlace", calling each PlacementNode's "setPlacement()"
 *       and "setOrientation()" methods to establish the proper placement.
 *
 * To avoid the complexities of the Electric database, four shadow-classes are defined that
 * describe the necessary information that Placement algorithms want:
 *
 * PlacementNode is an actual node (primitive or cell instance) that is to be placed.
 * PlacementPort is the connection site on the PlacementNode.
 *    Each PlacementNode has a list of zero or more PlacementPort objects,
 *    and each PlacementPort points to its "parent" PlacementNode.
 * PlacementNetwork determines which PlacementPort objects will connect together.
 *    Each PlacementNetwork has a list of two or more PlacementPort objects that it connects,
 *    and each PlacementPort has a PlacementNetwork in which it resides.
 * PlacementExport describes exports in the cell.
 *    Placement algorithms do not usually need this information:
 *    it exists as a way to communicate the information internally.
 */
public class PlacementFrame
{
	/**
	 * Static list of all Placement algorithms.
	 * When you create a new algorithm, add it to the following list.
	 */
	private static PlacementFrame [] placementAlgorithms = {
		new SimulatedAnnealing(),				// team 2
		new PlacementSimulatedAnnealing(),		// team 6
		new GeneticPlacement(),					// team 3
		new PlacementGenetic(),					// team 4
		new PlacementForceDirectedTeam5(),		// team 5
		new PlacementForceDirectedStaged(),		// team 7
		new PlacementMinCut(),
		new PlacementSimple(),
		new PlacementRandom()
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
	 * @param cellName the name of the cell being placed.
	 */
	protected void runPlacement(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks, String cellName) {}

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
	public static class PlacementNode
	{
		private NodeProto original;
		private String nodeName;
		private int techBits;
		private double width, height;
		private List<PlacementPort> ports;
		private double xPos, yPos;
		private Orientation orient;
		private Map<Key,Object> addedVariables;
		private Object userObject;

		public Object getUserObject() { return userObject; }
		public void setUserObject(Object obj) { userObject = obj; }

		/**
		 * Method to create a PlacementNode object.
		 * @param type the original Electric type of this PlacementNode.
		 * @param name the name to give the node once placed (can be null).
		 * @param tBits the technology-specific bits of this PlacementNode
		 * (typically 0 except for specialized Schematics components).
		 * @param wid the width of this PlacementNode.
		 * @param hei the height of this PlacementNode.
		 * @param pps a list of PlacementPort on the PlacementNode, indicating connection locations.
		 */
		public PlacementNode(NodeProto type, String name, int tBits, double wid, double hei, List<PlacementPort> pps)
		{
			original = type;
			nodeName = name;
			techBits = tBits;
			width = wid;
			height = hei;
			ports = pps;
		}

		/**
		 * Method to add variables to this PlacementNode.
		 * Variables are extra name/value pairs, for example a transistor width and length.
		 * @param name the Key of the variable to add.
		 * @param value the value of the variable to add.
		 */
		public void addVariable(Key name, Object value)
		{
			if (addedVariables == null)
				addedVariables = new HashMap<Key,Object>();
			addedVariables.put(name, value);
		}

		/**
		 * Method to return a list of PlacementPorts on this PlacementNode.
		 * @return a list of PlacementPorts on this PlacementNode.
		 */
		public List<PlacementPort> getPorts() { return ports; }

		/**
		 * Method to return the width of this PlacementNode.
		 * @return the width of this PlacementNode.
		 */
		public double getWidth() { return width; }

		/**
		 * Method to return the height of this PlacementNode.
		 * @return the height of this PlacementNode.
		 */
		public double getHeight() { return height; }

		/**
		 * Method to set the location of this PlacementNode.
		 * The Placement algorithm must call this method to set the final location of the PlacementNode.
		 * @param x the X-coordinate of the center of this PlacementNode.
		 * @param y the Y-coordinate of the center of this PlacementNode.
		 */
		public void setPlacement(double x, double y) { xPos = x;   yPos = y; }

		/**
		 * Method to set the orientation (rotation and mirroring) of this PlacementNode.
		 * The Placement algorithm may call this method to set the final orientation of the PlacementNode.
		 * @param o the Orientation of this PlacementNode.
		 */
		public void setOrientation(Orientation o)
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
		public double getPlacementX() { return xPos; }

		/**
		 * Method to return the Y-coordinate of the placed location of this PlacementNode.
		 * This is the location that the Placement algorithm has established for this PlacementNode.
		 * @return the Y-coordinate of the placed location of this PlacementNode.
		 */
		public double getPlacementY() { return yPos; }

		/**
		 * Method to return the Orientation of this PlacementNode.
		 * This is the Orientation that the Placement algorithm has established for this PlacementNode.
		 * @return the Orientation of this PlacementNode.
		 */
		public Orientation getPlacementOrientation() { return orient; }

		/**
		 * Method to return the NodeProto of this PlacementNode.
		 * @return the NodeProto of this PlacementNode.
		 */
		public NodeProto getType() { return original; }

		/**
		 * Method to return the technology-specific information of this PlacementNode.
		 * @return the technology-specific information of this PlacementNode
		 * (typically 0 except for specialized Schematics components).
		 */
		public int getTechBits() { return techBits; }

		public String toString()
		{
			String name = original.describe(false);
			if (nodeName != null) name += "[" + nodeName + "]";
			if (techBits != 0) name += "("+techBits+")";
			return name;
		}
	}

	/**
	 * Class to define ports on PlacementNode objects.
	 * This is a shadow class for the internal Electric object "PortInst".
	 */
	public static class PlacementPort
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
		public PlacementPort(double x, double y, PortProto pp)
		{
			offX = x;   offY = y;
			proto = pp;
		}

		/**
		 * Method to set the "parent" PlacementNode on which this PlacementPort resides.
		 * @param pn the PlacementNode on which this PlacementPort resides.
		 */
		public void setPlacementNode(PlacementNode pn) { plNode = pn; }

		/**
		 * Method to return the PlacementNode on which this PlacementPort resides.
		 * @return the PlacementNode on which this PlacementPort resides.
		 */
		public PlacementNode getPlacementNode() { return plNode; }

		/**
		 * Method to return the PlacementNetwork on which this PlacementPort resides.
		 * @param pn the PlacementNetwork on which this PlacementPort resides.
		 */
		public void setPlacementNetwork(PlacementNetwork pn) { plNet = pn; }

		/**
		 * Method to return the PlacementNetwork on which this PlacementPort resides.
		 * @return the PlacementNetwork on which this PlacementPort resides.
		 * If this PlacementPort does not connect to any other PlacementPort,
		 * the PlacementNetwork may be null.
		 */
		public PlacementNetwork getPlacementNetwork() { return plNet; }

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
		public double getOffX() { return offX; }

		/**
		 * Method to return the offset of this PlacementPort's Y coordinate from the center of its PlacementNode.
		 * The offset is valid when no Orientation has been applied.
		 * @return the offset of this PlacementPort's Y coordinate from the center of its PlacementNode.
		 */
		public double getOffY() { return offY; }

		/**
		 * Method to return the offset of this PlacementPort's X coordinate from the center of its PlacementNode.
		 * The coordinate assumes that the PlacementNode has been rotated by its Orientation.
		 * @return the offset of this PlacementPort's X coordinate from the center of its PlacementNode.
		 */
		public double getRotatedOffX() { return rotatedOffX; }

		/**
		 * Method to return the offset of this PlacementPort's Y coordinate from the center of its PlacementNode.
		 * The coordinate assumes that the PlacementNode has been rotated by its Orientation.
		 * @return the offset of this PlacementPort's Y coordinate from the center of its PlacementNode.
		 */
		public double getRotatedOffY() { return rotatedOffY; }

		/**
		 * Internal method to compute the rotated offset of this PlacementPort
		 * assuming that the Orientation of its PlacementNode has changed.
		 * TODO: why is this public?  it should not be accessed!
		 */
		public void computeRotatedOffset()
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
	public static class PlacementNetwork
	{
		private List<PlacementPort> portsOnNet;

		/**
		 * Constructor to create this PlacementNetwork with a list of PlacementPort objects that it connects.
		 * @param ports a list of PlacementPort objects that it connects.
		 */
		public PlacementNetwork(List<PlacementPort> ports)
		{
			portsOnNet = ports;
		}

		/**
		 * Method to return the list of PlacementPort objects on this PlacementNetwork.
		 * @return a list of PlacementPort objects on this PlacementNetwork.
		 */
		public List<PlacementPort> getPortsOnNet() { return portsOnNet; }
	}

	/**
	 * Class to define an Export that will be placed in the circuit.
	 */
	public static class PlacementExport
	{
		private PlacementPort portToExport;
		private String exportName;
		private PortCharacteristic characteristic;

		/**
		 * Constructor to create a PlacementExport with the information about an Export to be created.
		 * @param port the PlacementPort that is being exported.
		 * @param name the name to give the Export.
		 * @param chr the PortCharacteristic (input, output, etc.) to give the Export.
		 */
		public PlacementExport(PlacementPort port, String name, PortCharacteristic chr)
		{
			portToExport = port;
			exportName = name;
			characteristic = chr;
		}

		PlacementPort getPort() { return portToExport; }
		String getName() { return exportName; }
		PortCharacteristic getCharacteristic() { return characteristic; }
	}

	/**
	 * Entry point to do Placement of a Cell and create a new, placed Cell.
	 * Gathers the requirements for Placement into a collection of shadow objects
	 * (PlacementNode, PlacementPort, PlacementNetwork, and PlacementExport).
	 * Then invokes the alternate version of "doPlacement()" that works from shadow objedts.
	 * @param cell the Cell to place.
	 * Objects in that Cell will be reorganized in and placed in a new Cell.
	 * @return the new Cell with the placement results.
	 */
	public Cell doPlacement(Cell cell, Placement.PlacementPreferences prefs)
	{
		// get network information for the Cell
		Netlist netList = cell.getNetlist();
		if (netList == null)
		{
			System.out.println("Sorry, a deadlock aborted routing (network information unavailable).  Please try again");
			return null;
		}

		// convert nodes in the Cell into PlacementNode objects
		NodeProto iconToPlace = null;
		List<PlacementNode> nodesToPlace = new ArrayList<PlacementNode>();
		Map<NodeInst,Map<PortProto,PlacementPort>> convertedNodes = new HashMap<NodeInst,Map<PortProto,PlacementPort>>();
		List<PlacementExport> exportsToPlace = new ArrayList<PlacementExport>();
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (ni.isIconOfParent())
			{
				iconToPlace = ni.getProto();
				continue;
			}
			boolean validNode = ni.isCellInstance();
			if (!validNode)
			{
				if (ni.getProto().getTechnology() != Generic.tech())
				{
					PrimitiveNode.Function fun = ni.getFunction();
					if (fun != PrimitiveNode.Function.CONNECT && fun != PrimitiveNode.Function.CONTACT &&
						!fun.isPin())
							validNode = true;
				}
				if (ni.hasExports()) validNode = true;
			}
			if (validNode)
			{
				// make a list of PlacementPorts on this NodeInst
				NodeProto np = ni.getProto();
				List<PlacementPort> pl = new ArrayList<PlacementPort>();
				Map<PortProto,PlacementPort> placedPorts = new HashMap<PortProto,PlacementPort>();
				if (ni.isCellInstance())
				{
					for(Iterator<Export> eIt = ((Cell)np).getExports(); eIt.hasNext(); )
					{
						Export e = eIt.next();
						Poly poly = e.getPoly();
						PlacementPort plPort = new PlacementPort(poly.getCenterX(), poly.getCenterY(), e);
						pl.add(plPort);
						placedPorts.put(e, plPort);
					}
				} else
				{
					NodeInst niDummy = NodeInst.makeDummyInstance(np);
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
				}

				// add to the list of PlacementExports
				for(Iterator<Export> eIt = ni.getExports(); eIt.hasNext(); )
				{
					Export e = eIt.next();
					PlacementPort plPort = placedPorts.get(e.getOriginalPort().getPortProto());
					PlacementExport plExport = new PlacementExport(plPort, e.getName(), e.getCharacteristic());
					exportsToPlace.add(plExport);
				}

				// make the PlacementNode for this NodeInst
				String name = ni.getName();
				if (ni.getNameKey().isTempname()) name = null;
				PlacementNode plNode = new PlacementNode(np, name, ni.getTechSpecific(), np.getDefWidth(),
                    np.getDefHeight(), pl);
				nodesToPlace.add(plNode);
				for(PlacementPort plPort : pl)
					plPort.setPlacementNode(plNode);
				plNode.setOrientation(Orientation.IDENT);
				convertedNodes.put(ni, placedPorts);
			}
		}

		// gather connectivity information in a list of PlacementNetwork objects
        Map<Network,PortInst[]> portInstsByNetwork = null;
        if (cell.getView() != View.SCHEMATIC) portInstsByNetwork = netList.getPortInstsByNetwork();
		List<PlacementNetwork> allNetworks = new ArrayList<PlacementNetwork>();
		for(Iterator<Network> it = netList.getNetworks(); it.hasNext(); )
		{
			Network net = it.next();
			List<PlacementPort> portsOnNet = new ArrayList<PlacementPort>();
			PortInst[] portInsts = null;
			if (portInstsByNetwork != null) portInsts = portInstsByNetwork.get(net); else
			{
				List<PortInst> portList = new ArrayList<PortInst>();
				for(Iterator<PortInst> pIt = net.getPorts(); pIt.hasNext(); ) portList.add(pIt.next());
				portInsts = portList.toArray(new PortInst[]{});
			}
			for(int i=0; i<portInsts.length; i++)
	        {
				PortInst pi = portInsts[i];
				NodeInst ni = pi.getNodeInst();
				PortProto pp = pi.getPortProto();
				Map<PortProto,PlacementPort> convertedPorts = convertedNodes.get(ni);
				if (convertedPorts == null) continue;
				PlacementPort plPort = convertedPorts.get(pp);
				if (plPort != null) portsOnNet.add(plPort);
			}
			if (portsOnNet.size() > 1)
			{
				PlacementNetwork plNet = new PlacementNetwork(portsOnNet);
				for(PlacementPort plPort : portsOnNet)
					plPort.setPlacementNetwork(plNet);
				allNetworks.add(plNet);
			}
		}

		// do the placement from the shadow objects
		Cell newCell = doPlacement(cell.getLibrary(), cell.noLibDescribe(), nodesToPlace, allNetworks, exportsToPlace,
            iconToPlace, prefs.iconParameters);
		return newCell;
	}

	/**
	 * Entry point for other tools that wish to describe a network to be placed.
	 * Creates a cell with the placed network.
	 * @param lib the Library in which to create the placed Cell.
	 * @param cellName the name of the Cell to create.
	 * @param nodesToPlace a List of PlacementNodes to place in the Cell.
	 * @param allNetworks a List of PlacementNetworks to connect in the Cell.
	 * @param exportsToPlace a List of PlacementExports to create in the Cell.
	 * @param iconToPlace non-null to place an instance of itself (the icon) in the Cell.
	 * @return the newly created Cell.
	 */
	public Cell doPlacement(Library lib, String cellName, List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks,
                            List<PlacementExport> exportsToPlace, NodeProto iconToPlace, IconParameters iconParameters)
	{
        long startTime = System.currentTimeMillis();
        System.out.println("Running placement on cell '" + cellName + "' using the '" + getAlgorithmName() + "' algorithm");

        // do the real work of placement
		runPlacement(nodesToPlace, allNetworks, cellName);

		// create a new cell for the placement results
		Cell newCell = Cell.makeInstance(lib, cellName); // newCellName

		// place the nodes in the new cell
		Map<PlacementNode,NodeInst> placedNodes = new HashMap<PlacementNode,NodeInst>();
		for(PlacementNode plNode : nodesToPlace)
		{
			double xPos = plNode.getPlacementX();
			double yPos = plNode.getPlacementY();
			Orientation orient = plNode.getPlacementOrientation();
			NodeProto np = plNode.original;
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
				orient, plNode.nodeName, plNode.techBits);
			if (ni == null) System.out.println("Placement failed to create node"); else
				placedNodes.put(plNode, ni);
			if (plNode.addedVariables != null)
			{
				for(Key key : plNode.addedVariables.keySet())
				{
					Object value = plNode.addedVariables.get(key);
					Variable var = ni.newDisplayVar(key, value);
					if (key == Schematics.SCHEM_RESISTANCE)
					{
						ni.setTextDescriptor(key, var.getTextDescriptor().withOff(0, 0.5).
							withDispPart(TextDescriptor.DispPos.VALUE));
					} else if (key == Schematics.ATTR_WIDTH)
					{
						ni.setTextDescriptor(key, var.getTextDescriptor().withOff(0.5, -1).
							withRelSize(1).withDispPart(TextDescriptor.DispPos.VALUE));
					} else if (key == Schematics.ATTR_LENGTH)
					{
						ni.setTextDescriptor(key, var.getTextDescriptor().withOff(-0.5, -1).
							withRelSize(0.5).withDispPart(TextDescriptor.DispPos.VALUE));
					} else
					{
						ni.setTextDescriptor(key, var.getTextDescriptor().withDispPart(TextDescriptor.DispPos.VALUE));
					}
				}
			}
		}

		// place an icon if requested
		if (iconToPlace != null)
		{
			ERectangle bounds = newCell.getBounds();
			EPoint center = new EPoint(bounds.getMaxX() + iconToPlace.getDefWidth(), bounds.getMaxY() + iconToPlace.getDefHeight());
			NodeInst.makeInstance(iconToPlace, center, iconToPlace.getDefWidth(), iconToPlace.getDefHeight(), newCell);
		}

		// place exports in the new cell
		for(PlacementExport plExport : exportsToPlace)
		{
			PlacementPort plPort = plExport.getPort();
			String exportName = plExport.getName();
			PlacementNode plNode = plPort.getPlacementNode();
			NodeInst newNI = placedNodes.get(plNode);
			if (newNI == null) continue;
			PortInst portToExport = newNI.findPortInstFromProto(plPort.getPortProto());
			Export.newInstance(newCell, portToExport, exportName, plExport.getCharacteristic(), iconParameters);
		}

		ImmutableArcInst a = Generic.tech().unrouted_arc.getDefaultInst(newCell.getEditingPreferences());
		long gridExtend = a.getGridExtendOverMin();
		for(PlacementNetwork plNet : allNetworks)
		{
			PlacementPort lastPp = null;  PortInst lastPi = null;  EPoint lastPt = null;
			for(PlacementPort plPort : plNet.getPortsOnNet())
			{
				PlacementNode plNode = plPort.getPlacementNode();
				NodeInst newNi = placedNodes.get(plNode);
				if (newNi != null)
				{
					PlacementPort thisPp = plPort;
					PortInst thisPi = newNi.findPortInstFromProto(thisPp.getPortProto());
					EPoint thisPt = new EPoint(plNode.getPlacementX() + plPort.getRotatedOffX(),
						plNode.getPlacementY() + plPort.getRotatedOffY());
					if (lastPp != null)
					{
						// connect them
						ArcInst.newInstance(newCell, Generic.tech().unrouted_arc, null, null,
							lastPi, thisPi, lastPt, thisPt, gridExtend, 0, a.flags);						
					}
					lastPp = thisPp;
					lastPi = thisPi;
					lastPt = thisPt;
				}
			}
		}

        long endTime = System.currentTimeMillis();
        System.out.println("\t(took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
        return newCell;
	}
}
