/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RoutingFrame.java
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
package com.sun.electric.tool.routing;

import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Technology.NodeLayer;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.placement.Placement;
import com.sun.electric.tool.routing.experimentalSeaOfGates.RoutingFrameSeaOfGates;
import com.sun.electric.tool.routing.experimentalSimple.RoutingFrameSimple;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Class to define a framework for Routing algorithms.
 * To make Routing algorithms easier to write, all Routing algorithms must extend this class.
 * The Routing algorithm then defines two methods:
 *
 *    public String getAlgorithmName()
 *       returns the name of the Routing algorithm
 *
 *    int runRouting(List<RoutingSegment> segmentsToRoute, List<RoutingLayer> allLayers,
 *            List<RoutingNode> allNodes, List<RoutingGeometry> otherBlockages, List<RoutingContact> allContacts)
 *       Routes "segmentsToRoute" using the layers in "allLayers".
 *       The existing "blockages" are defined by "allNodes".
 *       Layer-changing can be done using one of the contacts in "allContacts".
 *
 * To avoid the complexities of the Electric database, these shadow-classes are defined to
 * describe the necessary information that routing algorithms want:
 *
 * RoutingSegment defines a segment that must be routed.
 *    It has coordinates of the endpoints of the segment as well as layer information at those ends.
 * RoutingLayer defines the layers that are used in routing.
 *    It also includes minimum spacing information to other layers.
 * RoutingNode is an actual node that already exists in the cell.
 *    It may be at the end of a route, and it does contain a list of layers that act as routing blockages.
 * RoutingContact defines a particular type of contact between routing arcs for layer changes.
 *    It consists of many RoutingGeometry objects that define the contact.
 *    It also defines the two layers that connect through it.
 * RoutingGeometry defines a piece of geometry on a given RoutingLayer.
 *    It is relative to the center of the RoutingNode or RoutingContact.
 * RoutingParameter is a way for code to store preference values persistently.
 */
public class RoutingFrame
{
	/**
	 * Static list of all Routing algorithms.
	 * When you create a new algorithm, add it to the following list.
	 */
	private static RoutingFrame [] routingAlgorithms = {
		new RoutingFrameSimple(),
		new RoutingFrameSeaOfGates()
	};

	/**
	 * Method to return a list of all Routing algorithms.
	 * @return a list of all Routing algorithms.
	 */
	public static RoutingFrame [] getRoutingAlgorithms() { return routingAlgorithms; }

	/**
	 * Method to do Routing (overridden by actual Routing algorithms).
	 * @param segmentsToRoute a list of all routes that need to be made.
	 * @param allLayers a list of all layers that can be used in routing.
	 * @param allNodes a list of all nodes involved in the routing.
	 * @param allContacts a list of all contacts that can be used in routing.
	 */
	protected int runRouting(List<RoutingSegment> segmentsToRoute, List<RoutingLayer> allLayers,
		List<RoutingNode> allNodes, List<RoutingGeometry> otherBlockages, List<RoutingContact> allContacts) { return 0; }

	/**
	 * Method to return the name of the routing algorithm (overridden by actual Routing algorithms).
	 * @return the name of the routing algorithm.
	 */
	public String getAlgorithmName() { return "?"; }

	/**
	 * Method to return a list of parameters for this routing algorithm.
	 * @return a list of parameters for this routing algorithm.
	 */
	public List<RoutingParameter> getParameters() { return null; }

	/**
	 * Class to define a parameter for a routing algorithm.
	 */
	public class RoutingParameter
	{
		public static final int TYPEINTEGER = 1;
		public static final int TYPESTRING = 2;
		public static final int TYPEDOUBLE = 3;
		public static final int TYPEBOOLEAN = 4;

		private String title;
		private int type;
		private Pref pref;
		private int tempInt;
		private String tempString;
		private double tempDouble;
		private boolean tempBoolean;
		private boolean tempValueSet;

		public RoutingParameter(String name, String title, int factory)
		{
			this.title = title;
			type = TYPEINTEGER;
			tempValueSet = false;
			pref = Pref.makeIntPref(getAlgorithmName()+"-"+name, Routing.getRoutingTool().prefs, factory);
		}

		public RoutingParameter(String name, String title, String factory)
		{
			this.title = title;
			type = TYPESTRING;
			tempValueSet = false;
			pref = Pref.makeStringPref(getAlgorithmName()+"-"+name, Routing.getRoutingTool().prefs, factory);
		}

		public RoutingParameter(String name, String title, double factory)
		{
			this.title = title;
			type = TYPEDOUBLE;
			tempValueSet = false;
			pref = Pref.makeDoublePref(getAlgorithmName()+"-"+name, Routing.getRoutingTool().prefs, factory);
		}

		public RoutingParameter(String name, String title, boolean factory)
		{
			this.title = title;
			type = TYPEBOOLEAN;
			tempValueSet = false;
			pref = Pref.makeBooleanPref(getAlgorithmName()+"-"+name, Routing.getRoutingTool().prefs, factory);
		}

		public String getName() { return title; }

		public int getType() { return type; }

		public int getIntValue() { return pref.getInt(); }

		public String getStringValue() { return pref.getString(); }

		public double getDoubleValue() { return pref.getDouble(); }

		public boolean getBooleanValue() { return pref.getBoolean(); }

		public void resetToFactory() { pref.factoryReset(); }

		/******************** TEMP VALUES DURING THE PREFERENCES DIALOG ********************/ 

		public int getTempIntValue() { return tempInt; }

		public String getTempStringValue() { return tempString; }

		public double getTempDoubleValue() { return tempDouble; }

		public boolean getTempBooleanValue() { return tempBoolean; }
		
		public void setTempIntValue(int i) { tempInt = i;   tempValueSet = true; }

		public void setTempStringValue(String s) { tempString = s;   tempValueSet = true; }

		public void setTempDoubleValue(double d) { tempDouble = d;   tempValueSet = true; }

		public void setTempBooleanValue(boolean d) { tempBoolean = d;   tempValueSet = true; }

		public boolean hasTempValue() { return tempValueSet; }

		public void clearTempValue() { tempValueSet = false; }

		public void makeTempSettingReal()
		{
			if (tempValueSet)
			{
				switch (type)
				{
					case TYPEINTEGER: pref.setInt(tempInt);         break;
					case TYPESTRING:  pref.setString(tempString);   break;
					case TYPEDOUBLE:  pref.setDouble(tempDouble);   break;
					case TYPEBOOLEAN: pref.setBoolean(tempBoolean); break;
				}
			}
		}
	}

	/**
	 * Class to define a layer that can be routed.
	 */
	public static class RoutingLayer
	{
		private Layer layer;
		private ArcProto ap;
		private double minWidth;
		private Map<RoutingLayer,Double> minSpacing;

		/**
		 * Method to create a RoutingLayer object.
		 * @param type the original Electric type of this RoutingNode.
		 * @param name the name to give the node once routed (can be null).
		 */
		public RoutingLayer(Layer layer, ArcProto ap, double minWidth)
		{
			this.layer = layer;
			this.ap = ap;
			this.minWidth = minWidth;
			minSpacing = new HashMap<RoutingLayer,Double>();
		}

		public Layer getLayer() { return layer; }

		public ArcProto getArcProto() { return ap; }

		public String getName() { return layer.getName(); }

		public double getMinWidth() { return minWidth; }

		public double getMinSpacing(RoutingLayer other)
		{
			Double dist = minSpacing.get(other);
			if (dist == null) return 0;
			return dist.doubleValue();
		}

		public void setMinSpacing(RoutingLayer other, double dist)
		{
			minSpacing.put(other, new Double(dist));
		}
	}

	/**
	 * Class to define geometry in a RoutingContact and a RoutingNode.
	 */
	public static class RoutingGeometry
	{
		private RoutingLayer layer;
		private Rectangle2D bounds;
		private int netID;

		/**
		 * Method to create a RoutingGeometry object.
		 */
		public RoutingGeometry(RoutingLayer layer, Rectangle2D bounds, int netID)
		{
			this.layer = layer;
			this.bounds = bounds;
			this.netID = netID;
		}

		public RoutingLayer getLayer() { return layer; }
		public Rectangle2D getBounds() { return bounds; }
		public int getNetID() { return netID; }
	}

	/**
	 * Class to define a contact that can be used for routing.
	 */
	public static class RoutingContact
	{
		private PrimitiveNode np;
		private List<RoutingGeometry> layers;
		private RoutingLayer first, second;

		public static RoutingContact STARTPOINT = new RoutingContact(null, null, null, null);
		public static RoutingContact FINISHPOINT = new RoutingContact(null, null, null, null);

		/**
		 * Method to create a RoutingContact object.
		 */
		public RoutingContact(PrimitiveNode np, List<RoutingGeometry> layers, RoutingLayer first, RoutingLayer second)
		{
			this.np = np;
			this.layers = layers;
			this.first = first;
			this.second = second;
		}

		public String getName() { return np.describe(false); }

		public List<RoutingGeometry> getGeometry() { return layers; }

		public RoutingLayer getFirstLayer() { return first; }

		public RoutingLayer getSecondLayer() { return second; }
	}

	/**
	 * Class to define a node that is being routed.
	 * This is a shadow class for the internal Electric object "NodeInst".
	 * There are minor differences between RoutingNode and NodeInst,
	 * for example, RoutingNode is presumed to be centered in the middle, with
	 * port offsets based on that center, whereas the NodeInst has a cell-center
	 * that may not be in the middle.
	 */
	public static class RoutingNode
	{
		private String name;
		private List<RoutingGeometry> layers;
		private Object userObject;

		public Object getUserObject() { return userObject; }
		public void setUserObject(Object obj) { userObject = obj; }

		/**
		 * Method to create a RoutingNode object.
		 * @param type the original Electric type of this RoutingNode.
		 * @param name the name to give the node once routed (can be null).
		 */
		public RoutingNode(String name, List<RoutingGeometry> layers)
		{
			this.name = name;
			this.layers = layers;
		}

		public String getName() { return name; }

		public List<RoutingGeometry> getLayers() { return layers; }

		public String toString()
		{
			return name;
		}
	}

	/**
	 * Class to define networks of RoutingPort objects.
	 * This is a shadow class for the internal Electric object "Network", but it is simplified for Routing.
	 */
	public static class RoutingSegment
	{
		private Point2D start, finish;
		private PortInst startPI, finishPI;
		private List<RoutingLayer> startLayers, finishLayers;
		private double startWidestArc, finishWidestArc;
		private List<RoutePoint> routedPoints;
		private List<RouteWire> routedWires;
		private String netName;
		private int netID;
		private List<Geometric> thingsToDelete;

		/**
		 * Constructor to create this RoutingSegment.
		 */
		public RoutingSegment(Point2D start, PortInst startPI, List<RoutingLayer> startLayers,
			Point2D finish, PortInst finishPI, List<RoutingLayer> finishLayers, int netID, String netName,
			List<Geometric> thingsToDelete)
		{
			this.start = start;
			this.startLayers = startLayers;
			this.startPI = startPI;
			this.startWidestArc = getWidestMetalArcOnPort(startPI);

			this.finish = finish;
			this.finishLayers = finishLayers;
			this.finishPI = finishPI;
			this.finishWidestArc = getWidestMetalArcOnPort(finishPI);

			this.netID = netID;
			this.netName = netName;
			this.thingsToDelete = thingsToDelete;
			routedPoints = new ArrayList<RoutePoint>();
			routedWires = new ArrayList<RouteWire>();
		}

		/**
		 * Get the widest metal arc already connected to a given PortInst.
		 * Looks recursively down the hierarchy.
		 * @param pi the PortInst to connect.
		 * @return the widest metal arc connect to that port (zero if none)
		 */
		private double getWidestMetalArcOnPort(PortInst pi)
		{
			// first check the top level
			double width = 0;
			for (Iterator<Connection> it = pi.getConnections(); it.hasNext(); )
			{
				Connection c = it.next();
				ArcInst ai = c.getArc();
				if (!ai.getProto().getFunction().isMetal()) continue;
				double newWidth = ai.getLambdaBaseWidth();
				if (newWidth > width) width = newWidth;
			}

			// now recurse down the hierarchy
			NodeInst ni = pi.getNodeInst();
			if (ni.isCellInstance())
			{
				Export export = (Export)pi.getPortProto();
				PortInst exportedInst = export.getOriginalPort();
				double width2 = getWidestMetalArcOnPort(exportedInst);
				if (width2 > width) width = width2;
			}
			return width;
		}

		/**
		 * Method to return the starting point on this RoutingSegment.
		 * @return the starting point on this RoutingSegment.
		 */
		public Point2D getStartLocation() { return start; }

		public List<RoutingLayer> getStartLayers() { return startLayers; }

		public PortInst getStartPort() { return startPI; }

		public double getWidestArcAtStart() { return startWidestArc; }

		/**
		 * Method to return the ending point on this RoutingSegment.
		 * @return the ending point on this RoutingSegment.
		 */
		public Point2D getFinishLocation() { return finish; }

		public List<RoutingLayer> getFinishLayers() { return finishLayers; }

		public PortInst getFinishPort() { return finishPI; }

		public double getWidestArcAtFinish() { return finishWidestArc; }

		public void addWire(RouteWire rw)
		{
			routedWires.add(rw);
		}

		public void addContact(RoutePoint rp)
		{
			routedPoints.add(rp);
		}

		public List<RouteWire> getWires() { return routedWires; }

		public List<RoutePoint> getContacts() { return routedPoints; }

		public int getNetID() { return netID; }

		public String getNetName() { return netName; }

		public List<Geometric> getThingsToDelete() { return thingsToDelete; }
	}

	public static class RouteWire
	{
		private RoutingLayer layer;
		private RoutePoint start, end;
		private double width;

		public RouteWire(RoutingLayer layer, RoutePoint start, RoutePoint end, double width)
		{
			this.layer = layer;
			this.start = start;
			this.end = end;
			this.width = width;
		}

		public RoutingLayer getLayer() { return layer; }

		public RoutePoint getStart() { return start; }

		public RoutePoint getFinish() { return end; }

		public double getWidth() { return width; }
	}

	public static class RoutePoint
	{
		private RoutingContact contact;
		private Point2D loc;

		public RoutePoint(RoutingContact contact, Point2D loc)
		{
			this.contact = contact;
			this.loc = loc;
		}

		public RoutingContact getContact() { return contact; }

		public Point2D getLocation() { return loc; }
	}

	/**
	 * Entry point to do Routing of a Cell.
	 * Gathers the requirements for Routing into a collection of shadow objects
	 * (RoutingNode, RoutingSegment, etc.)
	 * Then invokes the alternate version of "doRouting()" that works from shadow objects.
	 * @param cell the Cell to route.
	 * @return the number of segments that were routed.
	 */
	public int doRouting(Cell cell, Placement.PlacementPreferences prefs)
	{
		// get network information for the Cell
		Netlist netList = cell.getNetlist();
		if (netList == null)
		{
			System.out.println("Sorry, a deadlock aborted routing (network information unavailable).  Please try again");
			return 0;
		}

		// create all of the RoutingLayer objects
		Map<Layer,RoutingLayer> routingLayers = new HashMap<Layer,RoutingLayer>();
		List<RoutingLayer> allLayers = new ArrayList<RoutingLayer>();
		Technology tech = cell.getTechnology();
		for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			if (!ap.getFunction().isMetal()) continue;
			Layer layer = ap.getLayer(0);
			double minWidth = ap.getDefaultLambdaBaseWidth();
			RoutingLayer rl = new RoutingLayer(layer, ap, minWidth);
			routingLayers.put(layer, rl);
			allLayers.add(rl);
		}

		// now fill in all of the design rules
		for(RoutingLayer rl : allLayers)
		{
			Layer layer = rl.getLayer();
			for(RoutingLayer oRl : allLayers)
			{
				Layer oLayer = oRl.getLayer();
				double width = 3;		// TODO: finish this
				double length = 50;
				DRCTemplate rule = DRC.getSpacingRule(layer, null, oLayer, null, false, -1, width, length);
				if (rule != null) rl.setMinSpacing(oRl, rule.getValue(0));
			}
		}

		// create all of the RoutingContact objects
		List<RoutingContact> allContacts = new ArrayList<RoutingContact>();
		for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode pnp = it.next();
			if (pnp.getFunction() != PrimitiveNode.Function.CONTACT) continue;
			List<RoutingGeometry> contactGeom = new ArrayList<RoutingGeometry>();
			NodeLayer[] nLayers = pnp.getNodeLayers();
			double width = pnp.getDefWidth();
			double height = pnp.getDefHeight();
			for(int i=0; i<nLayers.length; i++)
			{
				RoutingLayer rl = routingLayers.get(nLayers[i].getLayer());
				if (rl == null) continue;

				EdgeH left = nLayers[i].getLeftEdge();
				double leftSide = left.getAdder() + left.getMultiplier() * width;
				EdgeH right = nLayers[i].getRightEdge();
				double rightSide = right.getAdder() + right.getMultiplier() * width;

				EdgeV top = nLayers[i].getTopEdge();
				double topSide = top.getAdder() + top.getMultiplier() * height;
				EdgeV bot = nLayers[i].getBottomEdge();
				double botSide = bot.getAdder() + bot.getMultiplier() * height;

				Rectangle2D bounds = new Rectangle2D.Double(leftSide, topSide, rightSide-leftSide, botSide-topSide);
				RoutingGeometry rg = new RoutingGeometry(rl, bounds, 0);
				contactGeom.add(rg);
			}

			RoutingLayer first = null, second = null;
			PrimitivePort pp = pnp.getPort(0);
			ArcProto [] arcs = pp.getConnections();
			for(int i=0; i<arcs.length; i++)
			{
				if (arcs[i].getTechnology() == Generic.tech()) continue;
				for(int j=0; j<arcs[i].getNumArcLayers(); j++)
				{
					Layer lay = arcs[i].getLayer(j);
					RoutingLayer rl = routingLayers.get(lay);
					if (rl == null) continue;
					if (first == null) first = rl; else
						if (second == null) second = rl;
				}
			}
			if (first != null && second != null)
			{
				RoutingContact rc = new RoutingContact(pnp, contactGeom, first, second);
				allContacts.add(rc);
			}
		}

		// create the RoutingSegment objects from the Unrouted arcs in the Cell
		List<RoutingSegment> allRoutes = new ArrayList<RoutingSegment>();
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();
			if (ai.getProto() != Generic.tech().unrouted_arc) continue;

			Point2D start = ai.getHeadLocation();
			List<RoutingLayer> startLayers = new ArrayList<RoutingLayer>();
			ArcProto [] startArcs = ai.getHeadPortInst().getPortProto().getBasePort().getConnections();
			for(int i=0; i<startArcs.length; i++)
			{
				if (startArcs[i].getTechnology() == Generic.tech()) continue;
				for(int j=0; j<startArcs[i].getNumArcLayers(); j++)
				{
					Layer lay = startArcs[i].getLayer(j);
					RoutingLayer rl = routingLayers.get(lay);
					if (rl != null) startLayers.add(rl);
				}
			}

			Point2D finish = ai.getTailLocation();
			List<RoutingLayer> finishLayers = new ArrayList<RoutingLayer>();
			ArcProto [] endArcs = ai.getTailPortInst().getPortProto().getBasePort().getConnections();
			for(int i=0; i<endArcs.length; i++)
			{
				if (endArcs[i].getTechnology() == Generic.tech()) continue;
				for(int j=0; j<endArcs[i].getNumArcLayers(); j++)
				{
					Layer lay = endArcs[i].getLayer(j);
					RoutingLayer rl = routingLayers.get(lay);
					if (rl != null) finishLayers.add(rl);
				}
			}

			PortInst startPI = ai.getHeadPortInst();
			PortInst finishPI = ai.getTailPortInst();
			List<Geometric> thingsToDelete = new ArrayList<Geometric>();
			thingsToDelete.add(ai);
			int netID = 0;	// TODO: finish
			String netName = netList.getNetworkName(ai);
			RoutingSegment rs = new RoutingSegment(start, startPI, startLayers, finish, finishPI, finishLayers, netID,
				netName, thingsToDelete);
			allRoutes.add(rs);
		}

		// define all of the nodes in the cell and the arc geometry at the top level
		List<RoutingNode> allNodes = new ArrayList<RoutingNode>();
		List<RoutingGeometry> otherBlockages = new ArrayList<RoutingGeometry>();
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			List<RoutingGeometry> geometryInCell = new ArrayList<RoutingGeometry>();
			BlockageVisitor visitor = new BlockageVisitor(ni, geometryInCell, routingLayers, otherBlockages);
			HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, visitor);
		}

		// do the Routing from the shadow objects
		int numRouted = doRouting(cell, allRoutes, allLayers, allNodes, otherBlockages, allContacts);
		return numRouted;
	}

	private void addGeometry(Cell cell, PolyBase poly, List<RoutingGeometry> geometryInCell, Map<Layer,RoutingLayer> routingLayers, int netID)
	{
		RoutingLayer rl = routingLayers.get(poly.getLayer());
		if (rl == null) return;
		Rectangle2D bounds = poly.getBounds2D();
		RoutingGeometry rg = new RoutingGeometry(rl, bounds, netID);
		geometryInCell.add(rg);
	}

	/**
	 * HierarchyEnumerator subclass to examine a cell for a given layer and fill an R-Tree.
	 */
	private class BlockageVisitor extends HierarchyEnumerator.Visitor
	{
		private List<RoutingGeometry> geometryInCell;
		private NodeInst theNi;
		private Map<Layer,RoutingLayer> routingLayers;
		private List<RoutingGeometry> otherBlockages;

		public BlockageVisitor(NodeInst ni, List<RoutingGeometry> geometryInCell, Map<Layer,RoutingLayer> routingLayers,
			List<RoutingGeometry> otherBlockages)
		{
			this.geometryInCell = geometryInCell;
			this.theNi = ni;
			this.routingLayers = routingLayers;
			this.otherBlockages = otherBlockages;
		}

		public boolean enterCell(HierarchyEnumerator.CellInfo info) { return true; }

		public void exitCell(HierarchyEnumerator.CellInfo info)
		{
			Cell cell = info.getCell();
			Netlist nl = info.getNetlist();
			AffineTransform trans = info.getTransformToRoot();
			for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				Technology tech = ai.getProto().getTechnology();
				PolyBase [] polys = tech.getShapeOfArc(ai);
				int netID = -1;
				Network net = nl.getNetwork(ai, 0);
				if (net != null) netID = info.getNetID(net);
				for(int i=0; i<polys.length; i++)
				{
					polys[i].transform(trans);
					if (info.isRootCell())
					{
						addGeometry(cell, polys[i], otherBlockages, routingLayers, netID);
					} else
					{
						addGeometry(cell, polys[i], geometryInCell, routingLayers, netID);
					}
				}
			}
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				if (info.isRootCell() && ni != theNi) continue;
				AffineTransform nodeTrans = ni.rotateOut(trans);
				Technology tech = ni.getProto().getTechnology();
				PolyBase [] polys = tech.getShapeOfNode(ni, true, false, null);
				for(int i=0; i<polys.length; i++)
				{
					PolyBase poly = polys[i];
					poly.transform(nodeTrans);
					int netID = -1;
					if (poly.getPort() != null)
					{
						Network net = nl.getNetwork(ni, poly.getPort(), 0);
						if (net != null) netID = info.getNetID(net);
					}
					addGeometry(cell, poly, geometryInCell, routingLayers, netID);
				}
			}
		}

		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
		{
			NodeInst ni = no.getNodeInst();
			if (info.isRootCell() && ni != theNi) return false;
			if (!ni.isCellInstance())
			{
				Cell cell = info.getCell();
				Netlist nl = info.getNetlist();
				AffineTransform trans = info.getTransformToRoot();
				AffineTransform nodeTrans = ni.rotateOut(trans);
				PrimitiveNode pNp = (PrimitiveNode)ni.getProto();
				Technology tech = pNp.getTechnology();
				Poly [] nodeInstPolyList = tech.getShapeOfNode(ni, true, false, null);
				for(int i=0; i<nodeInstPolyList.length; i++)
				{
					PolyBase poly = nodeInstPolyList[i];
					poly.transform(nodeTrans);
					int netID = -1;
					if (poly.getPort() != null)
					{
						Network net = nl.getNetwork(no, poly.getPort(), 0);
						if (net != null) netID = info.getNetID(net);
					}
					addGeometry(cell, poly, geometryInCell, routingLayers, netID);
				}
			}
			return true;
		}
	}

	/**
	 * Entry point for other tools that wish to describe a network to be routed.
	 * @return the number of segments that were routed.
	 */
	public int doRouting(Cell cell, List<RoutingSegment> segmentsToRoute, List<RoutingLayer> allLayers,
		List<RoutingNode> allNodes, List<RoutingGeometry> otherBlockages, List<RoutingContact> allContacts)
	{
        long startTime = System.currentTimeMillis();
        System.out.println("Running Routing on cell '" + cell.describe(false) + "' using the '" + getAlgorithmName() + "' algorithm");

        // do the real work of Routing
		int numRouted = runRouting(segmentsToRoute, allLayers, allNodes, otherBlockages, allContacts);

		// do the routing
		for(RoutingSegment rs : segmentsToRoute)
		{
			Map<RoutePoint,PortInst> builtContacts = new HashMap<RoutePoint,PortInst>();

			// create the contacts
			List<RoutePoint> contacts = rs.getContacts();
			for(RoutePoint rp : contacts)
			{
				RoutingContact rc = rp.getContact();
				if (rc == RoutingContact.FINISHPOINT || rc == RoutingContact.STARTPOINT) continue;
				Orientation orient = null;
				double width = rc.np.getDefWidth();
				double height = rc.np.getDefHeight();
				NodeInst ni = NodeInst.makeInstance(rc.np, rp.loc, width, height, cell, orient, null);
				builtContacts.put(rp, ni.getOnlyPortInst());
			}
			List<RouteWire> wires = rs.getWires();
			for(RouteWire rw : wires)
			{
				RoutePoint start = rw.getStart();
				PortInst startPI = builtContacts.get(start);
				if (startPI == null)
				{
					if (start.getContact() == RoutingContact.STARTPOINT) startPI = rs.startPI; else
						if (start.getContact() == RoutingContact.FINISHPOINT) startPI = rs.finishPI;
					if (startPI == null)
					{
						System.out.println("CANNOT DETERMINE STARTING POINT OF WIRE");
					}
				}

				RoutePoint finish = rw.getFinish();
				PortInst finishPI = builtContacts.get(finish);
				if (finishPI == null)
				{
					if (finish.getContact() == RoutingContact.STARTPOINT) finishPI = rs.startPI; else
						if (finish.getContact() == RoutingContact.FINISHPOINT) finishPI = rs.finishPI;
					if (finishPI == null)
					{
						System.out.println("CANNOT DETERMINE ENDING POINT OF WIRE");
					}
				}
				ArcProto ap = rw.layer.getArcProto();
				ArcInst ai = ArcInst.makeInstanceBase(ap, rw.getWidth(), startPI, finishPI);
				if (start.loc.getX() != finish.loc.getX() && start.loc.getY() != finish.loc.getY())
					ai.setFixedAngle(false);
			}

			// now delete the things to delete
			for(Geometric geom : rs.getThingsToDelete())
				if (geom instanceof ArcInst) ((ArcInst)geom).kill();
			for(Geometric geom : rs.getThingsToDelete())
				if (geom instanceof NodeInst) ((NodeInst)geom).kill();
		}

        long endTime = System.currentTimeMillis();
        System.out.println("Routed " + numRouted + " out of " + segmentsToRoute.size() +
        	" segments (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
        return numRouted;
	}
}
