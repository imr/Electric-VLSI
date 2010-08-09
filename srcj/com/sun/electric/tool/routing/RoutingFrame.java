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

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
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
 * To make Routing algorithms easier to write, Routing algorithms can extend this class.
 * The Routing algorithm does this by defining two methods:
 *
 *    String getAlgorithmName()
 *       Returns the name of the Routing algorithm.
 *
 *    void runRouting(Cell cell, List<RoutingSegment> segmentsToRoute, List<RoutingLayer> allLayers,
 *            List<RoutingContact> allContacts, List<RoutingGeometry> blockages)
 *       Routes Electric Cell "cell" (this is not required, but may be useful for highlighting).
 *       The List "segmentsToRoute" defines all of the routes that must be made.
 *       The List "allLayers" defines the layers that can be used in routing (the Metal layers).
 *       The List "allContacts" defines the layer-to-layer contacts that can be used in routing.
 *       The List "blockages" defines existing geometry that must be routed-around.
 *       Routes are defined by calling, for each RoutingSegment, the methods "addWire()" and "addContact()".
 *
 * To avoid the complexities of the Electric database, these shadow-classes are defined to
 * describe the necessary information that routing algorithms want:
 *
 * RoutingSegment defines a segment that must be routed.
 * RoutingEnd defines the ends of a RoutingSegment.
 * RoutingLayer defines the layers that are used in routing.
 * RoutingGeometry defines a piece of geometry on a given RoutingLayer.
 * RoutingContact defines a particular type of contact between routing arcs for layer changes.
 * RoutingParameter is a way for code to store preference values persistently.
 *
 * These classes exist to define a solved route:
 *   RoutePoint defines a point in the finished route.
 *   RouteWire defines a wire between two RoutePoint objects in the finished route.
 */
public class RoutingFrame
{
	private static final boolean DEBUGROUTES = false;

	/**
	 * Static list of all Routing algorithms.
	 * When you create a new algorithm, add it to the following list.
	 */
	private static RoutingFrame [] routingAlgorithms = {
//		new RoutingFrameSample(),
//		new RoutingFrameSeaOfGates(),
		new com.sun.electric.tool.routing.experimentalAStar1.AStarRoutingFrame(),		// Team 1
		new com.sun.electric.tool.routing.experimentalAStar2.AStarRouter(),				// Team 3v2
		new com.sun.electric.tool.routing.experimentalAStar3.AStarRouter(),				// Team 3v1
		new com.sun.electric.tool.routing.experimentalLeeMoore1.yana(),					// Team 6
		new com.sun.electric.tool.routing.experimentalLeeMoore2.RoutingFrameLeeMoore(),	// Team 4
		new com.sun.electric.tool.routing.experimentalLeeMoore3.RoutingFrameLeeMoore(),	// Team 2
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
	protected void runRouting(Cell cell, List<RoutingSegment> segmentsToRoute, List<RoutingLayer> allLayers,
		List<RoutingContact> allContacts, List<RoutingGeometry> blockages) {}

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

	private static Map<RoutingParameter,Pref> separatePrefs;

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
		private int tempInt, cachedInt;
		private String tempString, cachedString;
		private double tempDouble, cachedDouble;
		private boolean tempBoolean, cachedBoolean;
		private boolean tempValueSet;
		private boolean cached;

		/**
		 * Constructor to create an integer routing parameter.
		 * @param name the parameter name (should be simple, no spaces).
		 * @param title the title of the parameter (used in the Preferences dialog).
		 * @param factory the default value of the parameter.
		 */
		public RoutingParameter(String name, String title, int factory)
		{
			this.title = title;
			type = TYPEINTEGER;
			tempValueSet = cached = false;
			if (separatePrefs == null) separatePrefs = new HashMap<RoutingParameter,Pref>();
			separatePrefs.put(this, Pref.makeIntPref(getAlgorithmName()+"-"+name, Routing.getRoutingTool().prefs, factory));
		}

		/**
		 * Constructor to create a String routing parameter.
		 * @param name the parameter name (should be simple, no spaces).
		 * @param title the title of the parameter (used in the Preferences dialog).
		 * @param factory the default value of the parameter.
		 */
		public RoutingParameter(String name, String title, String factory)
		{
			this.title = title;
			type = TYPESTRING;
			tempValueSet = cached = false;
			if (separatePrefs == null) separatePrefs = new HashMap<RoutingParameter,Pref>();
			separatePrefs.put(this, Pref.makeStringPref(getAlgorithmName()+"-"+name, Routing.getRoutingTool().prefs, factory));
		}

		/**
		 * Constructor to create a double routing parameter.
		 * @param name the parameter name (should be simple, no spaces).
		 * @param title the title of the parameter (used in the Preferences dialog).
		 * @param factory the default value of the parameter.
		 */
		public RoutingParameter(String name, String title, double factory)
		{
			this.title = title;
			type = TYPEDOUBLE;
			tempValueSet = cached = false;
			if (separatePrefs == null) separatePrefs = new HashMap<RoutingParameter,Pref>();
			separatePrefs.put(this, Pref.makeDoublePref(getAlgorithmName()+"-"+name, Routing.getRoutingTool().prefs, factory));
		}

		/**
		 * Constructor to create a boolean routing parameter.
		 * @param name the parameter name (should be simple, no spaces).
		 * @param title the title of the parameter (used in the Preferences dialog).
		 * @param factory the default value of the parameter.
		 */
		public RoutingParameter(String name, String title, boolean factory)
		{
			this.title = title;
			type = TYPEBOOLEAN;
			tempValueSet = cached = false;
			if (separatePrefs == null) separatePrefs = new HashMap<RoutingParameter,Pref>();
			separatePrefs.put(this, Pref.makeBooleanPref(getAlgorithmName()+"-"+name, Routing.getRoutingTool().prefs, factory));
		}

		public String getName() { return title; }

		public int getType() { return type; }

		/**
		 * Method to get the current Parameter value for an integer.
		 * @return the current Parameter value for an integer.
		 */
		public int getIntValue() { if (cached) return cachedInt; return separatePrefs.get(this).getInt(); }

		/**
		 * Method to get the current Parameter value for a String.
		 * @return the current Parameter value for a String.
		 */
		public String getStringValue() { if (cached) return cachedString; return separatePrefs.get(this).getString(); }

		/**
		 * Method to get the current Parameter value for a double.
		 * @return the current Parameter value for an double.
		 */
		public double getDoubleValue() { if (cached) return cachedDouble; return separatePrefs.get(this).getDouble(); }

		/**
		 * Method to get the current Parameter value for a boolean.
		 * @return the current Parameter value for a boolean.
		 */
		public boolean getBooleanValue() { if (cached) return cachedBoolean; return separatePrefs.get(this).getBoolean(); }

		public void resetToFactory() { separatePrefs.get(this).factoryReset(); }

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

		public void clearTempValue() { tempValueSet = cached = false; }

		public void cacheValue()
		{
			cached = true;
			switch (type)
			{
				case TYPEINTEGER: cachedInt = separatePrefs.get(this).getInt();         break;
				case TYPESTRING:  cachedString = separatePrefs.get(this).getString();   break;
				case TYPEDOUBLE:  cachedDouble = separatePrefs.get(this).getDouble();   break;
				case TYPEBOOLEAN: cachedBoolean = separatePrefs.get(this).getBoolean(); break;
			}
		}

		public void makeTempSettingReal()
		{
			if (tempValueSet)
			{
				switch (type)
				{
					case TYPEINTEGER: separatePrefs.get(this).setInt(tempInt);         break;
					case TYPESTRING:  separatePrefs.get(this).setString(tempString);   break;
					case TYPEDOUBLE:  separatePrefs.get(this).setDouble(tempDouble);   break;
					case TYPEBOOLEAN: separatePrefs.get(this).setBoolean(tempBoolean); break;
				}
				cached = false;
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
		private double maxSurround;
		private Map<RoutingLayer,Double> minSpacing;
		private RoutingContact pin;
		private boolean isMetal;

		/**
		 * Method to create a RoutingLayer object.
		 * @param layer the Electric Layer associated with this RoutingLayer.
		 * @param ap the Electric ArcProto associated with this RoutingLayer (only valid for via layers).
		 * @param minWidth the minimum width of wires on this RoutingLayer (only valid for via layers).
		 * @param maxSurround the maximum DRC rule around this RoutingLayer.
		 */
		public RoutingLayer(Layer layer, ArcProto ap, double minWidth, double maxSurround)
		{
			this.layer = layer;
			this.ap = ap;
			this.minWidth = minWidth;
			this.maxSurround = maxSurround;
			isMetal = layer.getFunction().isMetal();
			minSpacing = new HashMap<RoutingLayer,Double>();

			// add the pin information
			if (ap != null)
			{
				PrimitiveNode pinNP = ap.findPinProto();
				pin = new RoutingContact(pinNP, null, this, this, 0);
			}
		}

		/**
		 * Method to return the name of this RoutingLayer.
		 * @return the name of this RoutingLayer.
		 */
		public String getName() { return layer.getName(); }

		/**
		 * Method to tell whether this layer is metal or via.
		 * @return true for Metal, false for a via.
		 */
		public boolean isMetal() { return isMetal; }

		/**
		 * Method to return the metal layer number of this RoutingLayer.
		 * Only valid when this layer is Metal (not Via).
		 * @return the metal layer number (Metal-1 returns 1, etc.)
		 */
		public int getMetalNumber() { return layer.getFunction().getLevel(); }

		/**
		 * Method to return the proper RoutingContact to use when joining two of these layers.
		 * Pins are different than contacts: they have no geometry and are just
		 * virtual connection points, whereas contacts have geometry on two different
		 * metal layers as well as on a via layer in order to connect the two metals.
		 * This is only valid for metal layers (returns null for via layers).
		 * @return the proper RoutingContact to use when joining two of these layers.
		 */
		public RoutingContact getPin() { return pin; }

		/**
		 * Method to return the minimum width of wires on this RoutingLayer.
		 * Most wires should be created with this width.
		 * Some wires can be made wider if they are connecting to larger objects.
		 * This is only valid for metal layers (returns 0 for via layers).
		 * @return the minimum width of wires on this RoutingLayer.
		 */
		public double getMinWidth() { return minWidth; }

		/**
		 * Method to return the maximum DRC surround for this RoutingLayer.
		 * All design rules that involve this layer will be less than or
		 * equal to this value, so examining geometry in the vicinity of
		 * this layer only needs to look this far.
		 * @return the maximum DRC surround for this RoutingLayer.
		 */
		public double getMaxSurround() { return maxSurround; }

		/**
		 * Method to return the minimum spacing between this and another RoutingLayer.
		 * @param other the other RoutingLayer.
		 * @return the minimum distance that the two RoutingLayer objects must
		 * stay apart to avoid design-rule violations.
		 */
		public double getMinSpacing(RoutingLayer other)
		{
			Double dist = minSpacing.get(other);
			if (dist == null) return 0;
			return dist.doubleValue();
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
		 * @param layer the RoutingLayer on which this RoutingGeometry resides.
		 * @param bounds the shape of this RoutingGeometry.
		 * @param netID the global network number of this RoutingGeometry.
		 */
		public RoutingGeometry(RoutingLayer layer, Rectangle2D bounds, int netID)
		{
			this.layer = layer;
			this.bounds = bounds;
			this.netID = netID;
		}

		/**
		 * Method to return the RoutingLayer that this piece of RoutingGeometry uses.
		 * @return the RoutingLayer that this piece of RoutingGeometry uses.
		 */
		public RoutingLayer getLayer() { return layer; }

		/**
		 * Method to return the shape of this RoutingGeometry.
		 * The meaning of the shape varies depending on the context of
		 * this RoutingGeometry.  When found in a RoutingContact, the shape is
		 * based on the center of the contact.
		 * @return the shape of this RoutingGeometry.
		 */
		public Rectangle2D getBounds() { return bounds; }

		/**
		 * Method to return the global network index of this RoutingGeometry.
		 * These numbers help determine whether two pieces of geometry
		 * are on the same electrical network or not.
		 * @return the global network index of this RoutingGeometry.
		 */
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
		private double viaSpacing;
		private double defWidth, defHeight;

		/** a special RoutingContact that defines the start of a segment */
		public static RoutingContact STARTPOINT = new RoutingContact(null, null, null, null, 0);

		/** a special RoutingContact that defines the end of a segment */
		public static RoutingContact FINISHPOINT = new RoutingContact(null, null, null, null, 0);

		/**
		 * Method to create a RoutingContact object.
		 * @param np the PrimitiveNode associated with this RoutingContact.
		 * @param layers the geometry that composes this RoutingContact.
		 * @param first the first layer that this RoutingContact connects.
		 * @param second the second layer that this RoutingContact connects.
		 * @param viaSpacing the center-to-center minimum spacing of the vias in this RoutingContact.
		 */
		public RoutingContact(PrimitiveNode np, List<RoutingGeometry> layers, RoutingLayer first, RoutingLayer second,
			double viaSpacing)
		{
			this.np = np;
			this.layers = layers;
			this.first = first;
			this.second = second;
			this.viaSpacing = viaSpacing;
			if (np != null)
			{
				this.defWidth = np.getDefWidth();
				this.defHeight = np.getDefHeight();
			}
		}

		/**
		 * Method to return the name of this RoutingContact.
		 * This is useful for debugging.
		 * @return the name of this RoutingContact.
		 */
		public String getName()
		{
			if (this == STARTPOINT) return "START-POINT";
			if (this == FINISHPOINT) return "FINISH-POINT";
			return np.describe(false);
		}

		/**
		 * Method to return a List of pieces of RoutingGeometry that compose this RoutingContact.
		 * Each piece of RoutingGeometry has coordinates that are relative to the center of the contact.
		 * @return a List of pieces of RoutingGeometry that compose this RoutingContact.
		 */
		public List<RoutingGeometry> getGeometry() { return layers; }

		/**
		 * Method to return the first RoutingLayer that this RoutingContact connects.
		 * Each RoutingContact connects two different RoutingLayers.
		 * @return the first RoutingLayer that this RoutingContact connects.
		 */
		public RoutingLayer getFirstLayer() { return first; }

		/**
		 * Method to return the second RoutingLayer that this RoutingContact connects.
		 * Each RoutingContact connects two different RoutingLayers.
		 * @return the second RoutingLayer that this RoutingContact connects.
		 */
		public RoutingLayer getSecondLayer() { return second; }

		/**
		 * Method to return the minimum spacing between vias of this RoutingContact.
		 * When two contacts are nearby, the center coordinates of their vias must be
		 * separated by this distance.
		 * @return the minimum spacing between vias of this RoutingContact.
		 */
		public double getViaSpacing() { return viaSpacing; }

		/**
		 * Method to return the default width of this RoutingContact.
		 * Since this framework doesn't handle larger-than-normal contacts,
		 * this is actually the only width.
		 * @return the default width of this RoutingContact.
		 */
		public double getDefWidth() { return defWidth; }

		/**
		 * Method to return the default height of this RoutingContact.
		 * Since this framework doesn't handle larger-than-normal contacts,
		 * this is actually the only height.
		 * @return the default height of this RoutingContact.
		 */
		public double getDefHeight() { return defHeight; }
	}

	/**
	 * Class to define an end of a RoutingSegment.
	 */
	public static class RoutingEnd
	{
		private PortInst pi;
		private Point2D location;

		/**
		 * Constructor for building an end of a RoutingSegment.
		 * @param pi the Electric PortInst that is at this end of the route.
		 */
		public RoutingEnd(PortInst pi)
		{
			this.pi = pi;
			location = pi.getCenter();
		}

		/**
		 * Method to return the coordinates of this RoutingEnd.
		 * @return the coordinates of this RoutingEnd.
		 */
		public Point2D getLocation() { return location; }

		/**
		 * Method to return a description of this RoutingEnd.
		 * The description (for debugging) has the name of the Electric node and port.
		 * @return a description of this RoutingEnd.
		 */
		public String describe()
		{
			return pi.getPortProto().getName() + " of node " + pi.getNodeInst().describe(false);
		}
	}

	/**
	 * Class to define a desired route that should be created.
	 */
	public static class RoutingSegment
	{
		private RoutingEnd startEnd, finishEnd;
		private List<RoutingLayer> startLayers, finishLayers;
		private double startWidestArc, finishWidestArc;
		private List<RoutePoint> routedPoints;
		private List<RouteWire> routedWires;
		private String netName;
		private int netID;
		private List<Geometric> thingsToDelete;

		/**
		 * Constructor to create a RoutingSegment.
		 */
		public RoutingSegment(RoutingEnd startEnd, List<RoutingLayer> startLayers,
			RoutingEnd finishEnd, List<RoutingLayer> finishLayers, int netID, String netName,
			List<Geometric> thingsToDelete)
		{
			this.startLayers = startLayers;
			this.startEnd = startEnd;
			this.startWidestArc = getWidestMetalArcOnPort(startEnd.pi);

			this.finishLayers = finishLayers;
			this.finishEnd = finishEnd;
			this.finishWidestArc = getWidestMetalArcOnPort(finishEnd.pi);

			this.netID = netID;
			this.netName = netName;
			this.thingsToDelete = thingsToDelete;
			routedPoints = new ArrayList<RoutePoint>();
			routedWires = new ArrayList<RouteWire>();
		}

		/**
		 * Method to return the starting RoutingEnd of this RoutingSegment.
		 * Each RoutingSegment has a start and finish end that must be connected.
		 * @return the starting RoutingEnd of this RoutingSegment.
		 */
		public RoutingEnd getStartEnd() { return startEnd; }

		/**
		 * Method to return a List of RoutingLayers that may connect to
		 * the starting end of this RoutingSegment.
		 * Typically only one RoutingLayer can connect, but if the starting
		 * point is a contact, then two layers may connect.
		 * @return a List of RoutingLayers that may connect to
		 * the starting end of this RoutingSegment.
		 */
		public List<RoutingLayer> getStartLayers() { return startLayers; }

		/**
		 * Method to return the widest arc that is connected to the starting end.
		 * Routers should run wider wires when connecting here.
		 * @return the widest arc that is connected to the starting end.
		 */
		public double getWidestArcAtStart() { return startWidestArc; }

		/**
		 * Method to return the ending RoutingEnd of this RoutingSegment.
		 * Each RoutingSegment has a start and finish end that must be connected.
		 * @return the ending RoutingEnd of this RoutingSegment.
		 */
		public RoutingEnd getFinishEnd() { return finishEnd; }

		/**
		 * Method to return a List of RoutingLayers that may connect to
		 * the finish end of this RoutingSegment.
		 * Typically only one RoutingLayer can connect, but if the finish
		 * point is a contact, then two layers may connect.
		 * @return a List of RoutingLayers that may connect to
		 * the finish end of this RoutingSegment.
		 */
		public List<RoutingLayer> getFinishLayers() { return finishLayers; }

		/**
		 * Method to return the widest arc that is connected to the finish end.
		 * Routers should run wider wires when connecting here.
		 * @return the widest arc that is connected to the finish end.
		 */
		public double getWidestArcAtFinish() { return finishWidestArc; }

		/**
		 * Method to return the global network identifier for this RoutingSegment.
		 * These numbers help determine what is connected (when they have the same NetID).
		 * @return the global network identifier for this RoutingSegment.
		 */
		public int getNetID() { return netID; }

		/**
		 * Method to return the name of this RoutingSegment.
		 * This is for debugging.
		 * @return the name of this RoutingSegment.
		 */
		public String getNetName() { return netName; }

		/**
		 * Method to define a wire segment that is part of the final route.
		 * Routers must call this method and the "addWireEnd()" method to
		 * define the actual route.
		 * @param rw a wire segment that is part of the final route.
		 */
		public void addWire(RouteWire rw)
		{
			routedWires.add(rw);
		}

		/**
		 * Method to define a wire end that is part of the final route.
		 * Routers must call this method and the "addWire()" method to
		 * define the actual route.
		 * @param rp a contact that is part of the final route.
		 */
		public void addWireEnd(RoutePoint rp)
		{
			routedPoints.add(rp);
		}

		/**
		 * Method to return the widest metal arc already connected to a given PortInst.
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
	}

	/**
	 * Class to define a wire in the final routing of a RoutingSegment.
	 */
	public static class RouteWire
	{
		private RoutingLayer layer;
		private RoutePoint start, end;
		private double width;

		/**
		 * Constructor for defining a wire in the final routing.
		 * @param layer the RoutingLayer on which this wire resides.
		 * @param start a RoutePoint that defines the start of the wire.
		 * @param end a RoutePoint that defines the finish of the wire.
		 * @param width the width of the wire.
		 */
		public RouteWire(RoutingLayer layer, RoutePoint start, RoutePoint end, double width)
		{
			this.layer = layer;
			this.start = start;
			this.end = end;
			this.width = width;
		}
	}

	/**
	 * Class to define a point in the final routing of a RoutingSegment.
	 */
	public static class RoutePoint
	{
		private RoutingContact contact;
		private Point2D loc;
		private int angle;

		/**
		 * Constructor to create a RoutePoint in the final routing.
		 * @param contact the RoutingContact to place at this point.
		 * For the ends of the RoutingSegment, use RoutingContact.STARTPOINT
		 * and RoutingContact.FINISHPOINT.
		 * @param loc the coordinates of this RoutePoint.
		 * @param angle the rotation of the RoutingContact (in degrees).
		 */
		public RoutePoint(RoutingContact contact, Point2D loc, int angle)
		{
			this.contact = contact;
			this.loc = loc;
			this.angle = angle;
		}

		/**
		 * Method to return the RoutingContact that is to be placed at this RoutePoint.
		 * @return the RoutingContact that is to be placed at this RoutePoint.
		 */
		public RoutingContact getContact() { return contact; }

		/**
		 * Method to return the coordinates of this RoutePoint.
		 * @return the coordinates of this RoutePoint.
		 */
		public Point2D getLocation() { return loc; }
	}

	/**
	 * Entry point to do Routing of a Cell, called by Electric to do routing.
	 * Gathers the requirements for Routing into a collection of shadow objects
	 * (RoutingNode, RoutingSegment, etc.) then invokes "runRouting()" to route
	 * and finally places the routed geometry.
	 * @param cell the Cell to route.
	 * @return the number of segments that were routed.
	 */
	public int doRouting(Cell cell)
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
        GenMath.MutableDouble mutableDist = new GenMath.MutableDouble(0);
        for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			if (!ap.getFunction().isMetal()) continue;
			Layer layer = ap.getLayer(0);
			double minWidth = ap.getDefaultLambdaBaseWidth();
			DRC.getMaxSurround(layer, Double.MAX_VALUE, mutableDist);
            double maxSurround = mutableDist.doubleValue();
            RoutingLayer rl = new RoutingLayer(layer, ap, minWidth, maxSurround);
			routingLayers.put(layer, rl);
			allLayers.add(rl);
		}
		for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
			if (!layer.getFunction().isContact()) continue;
			if (layer.getFunction().getLevel() == 0) continue;
			DRC.getMaxSurround(layer, Double.MAX_VALUE, mutableDist);
			double maxSurround = mutableDist.doubleValue();
			RoutingLayer rl = new RoutingLayer(layer, null, 0, maxSurround);
			routingLayers.put(layer, rl);
			allLayers.add(rl);
		}

		// now fill in all of the design rules
		for(RoutingLayer rl : allLayers)
		{
			Layer layer = rl.layer;
			for(RoutingLayer oRl : allLayers)
			{
				Layer oLayer = oRl.layer;

				// advanced design-rules for wider-than-normal geometry is not implemented in this framework
				double width = 3;
				double length = 50;
				DRCTemplate rule = DRC.getSpacingRule(layer, null, oLayer, null, false, -1, width, length);
				if (rule != null) rl.minSpacing.put(oRl, new Double(rule.getValue(0)));
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
			Layer cutLayer = null;
			double width = pnp.getDefWidth();
			double height = pnp.getDefHeight();
			for(int i=0; i<nLayers.length; i++)
			{
				Layer lay = nLayers[i].getLayer();
				if (lay.getFunction().isContact())
				{
					cutLayer = lay;
					continue;
				}
				RoutingLayer rl = routingLayers.get(lay);
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
			if (first != null && second != null && cutLayer != null)
			{
				// determine via surround
				double spacing = 2;

				// advanced design-rules for wider-than-normal geometry is not implemented in this framework
				double arcWidth = 3;
				double arcLength = 50;
				DRCTemplate ruleSpacing = DRC.getSpacingRule(cutLayer, null, cutLayer, null, false, -1, arcWidth, arcLength);
				if (ruleSpacing != null) spacing = ruleSpacing.getValue(0);
			
				// determine cut size
				double cutWidth = 0;
				DRCTemplate ruleWidth = DRC.getMinValue(cutLayer, DRCTemplate.DRCRuleType.NODSIZ);
				if (ruleWidth != null) cutWidth = ruleWidth.getValue(0);
			
				double viaSurround = spacing + cutWidth;
				RoutingContact rc = new RoutingContact(pnp, contactGeom, first, second, viaSurround);
				allContacts.add(rc);
			}
		}

		// define all of the nodes in the cell and the arc geometry at the top level
		List<RoutingGeometry> blockages = new ArrayList<RoutingGeometry>();
		Map<ArcInst,Integer> netIDs = new HashMap<ArcInst,Integer>();
		GeometryVisitor visitor = new GeometryVisitor(routingLayers, blockages, netIDs);
		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, visitor);

		// create the RoutingSegment objects from the Unrouted arcs in the Cell
		List<RoutingSegment> segmentsToRoute = new ArrayList<RoutingSegment>();
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();
			if (ai.getProto() != Generic.tech().unrouted_arc) continue;

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

			RoutingEnd startPI = new RoutingEnd(ai.getHeadPortInst());
			RoutingEnd finishPI = new RoutingEnd(ai.getTailPortInst());
			List<Geometric> thingsToDelete = new ArrayList<Geometric>();
			thingsToDelete.add(ai);
			Integer netID = netIDs.get(ai);
			String netName = netList.getNetworkName(ai);
			RoutingSegment rs = new RoutingSegment(startPI, startLayers, finishPI, finishLayers,
				netID == null ? 0 : netID.intValue(), netName, thingsToDelete);
			segmentsToRoute.add(rs);
		}

		// prepare for routing
        long startTime = System.currentTimeMillis();
        System.out.println("Running Routing on cell '" + cell.describe(false) + "' using the '" + getAlgorithmName() + "' algorithm");

        // do the real work of Routing
		runRouting(cell, segmentsToRoute, allLayers, allContacts, blockages);

		// now implement the results
		int numRouted = 0;
		for(RoutingSegment rs : segmentsToRoute)
		{
			// see if the route was created 
			List<RoutePoint> contacts = rs.routedPoints;
			List<RouteWire> wires = rs.routedWires;
			if (contacts.size() == 0 && wires.size() == 0) continue;

// display route
if (DEBUGROUTES)
{
	System.out.println("++++ ROUTING SEGMENT: ++++");
	Map<RoutePoint,String> numberContacts = new HashMap<RoutePoint,String>();
	int num = 1;
	for(RoutePoint rp : contacts)
	{
		RoutingContact rc = rp.getContact();
		String contactName = "CONTACT-" + (num++);
		System.out.println(contactName + " IS " + rc.getName() + " AT (" + rp.loc.getX() + "," + rp.loc.getY() + ")");
		numberContacts.put(rp, contactName);
	}
	for(RouteWire rw : wires)
	{
		String start = numberContacts.get(rw.start);
		String end = numberContacts.get(rw.end);
		if (rw.start.getContact() == RoutingContact.STARTPOINT) start = "STARTING-POINT";
		if (rw.end.getContact() == RoutingContact.FINISHPOINT) end = "ENDING-POINT";
		System.out.println("WIRE " + rw.layer.getName() + " RUNS FROM " + start + " TO " + end);
	}
}

			// create the contacts
			Map<RoutePoint,PortInst> builtContacts = new HashMap<RoutePoint,PortInst>();
			for(RoutePoint rp : contacts)
			{
				RoutingContact rc = rp.getContact();
				if (rc == RoutingContact.FINISHPOINT || rc == RoutingContact.STARTPOINT) continue;
				Orientation orient = Orientation.fromAngle(rp.angle);
				double width = rc.np.getDefWidth();
				double height = rc.np.getDefHeight();
				NodeInst ni = NodeInst.makeInstance(rc.np, rp.loc, width, height, cell, orient, null);
				builtContacts.put(rp, ni.getOnlyPortInst());
			}
			for(RouteWire rw : wires)
			{
				RoutePoint start = rw.start;
				PortInst startPI = builtContacts.get(start);
				if (startPI == null)
				{
					if (start.getContact() == RoutingContact.STARTPOINT) startPI = rs.startEnd.pi; else
						if (start.getContact() == RoutingContact.FINISHPOINT) startPI = rs.finishEnd.pi;
					if (startPI == null)
					{
						System.out.println("CANNOT DETERMINE STARTING POINT OF WIRE");
					}
				}

				RoutePoint finish = rw.end;
				PortInst finishPI = builtContacts.get(finish);
				if (finishPI == null)
				{
					if (finish.getContact() == RoutingContact.STARTPOINT) finishPI = rs.startEnd.pi; else
						if (finish.getContact() == RoutingContact.FINISHPOINT) finishPI = rs.finishEnd.pi;
					if (finishPI == null)
					{
						System.out.println("CANNOT DETERMINE ENDING POINT OF WIRE");
					}
				}
				ArcProto ap = rw.layer.ap;
				ArcInst ai = ArcInst.makeInstanceBase(ap, rw.width, startPI, finishPI);
				if (ai == null)
				{
					System.out.println("FAILED TO RUN ARC");
				} else
				{
					if (start.loc.getX() != finish.loc.getX() && start.loc.getY() != finish.loc.getY())
						ai.setFixedAngle(false);
				}
			}

			// now delete the things to delete
			for(Geometric geom : rs.thingsToDelete)
				if (geom instanceof ArcInst) ((ArcInst)geom).kill();
			for(Geometric geom : rs.thingsToDelete)
				if (geom instanceof NodeInst) ((NodeInst)geom).kill();
			numRouted++;
		}

		long endTime = System.currentTimeMillis();
        System.out.println("Routed " + numRouted + " out of " + segmentsToRoute.size() +
        	" segments (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
        return numRouted;
	}

	/**
	 * HierarchyEnumerator subclass to geometry and build shadow data structures for routing.
	 */
	private class GeometryVisitor extends HierarchyEnumerator.Visitor
	{
		private Map<Layer,RoutingLayer> routingLayers;
		private List<RoutingGeometry> blockages;
		private Map<ArcInst,Integer> netIDs;

		public GeometryVisitor(Map<Layer,RoutingLayer> routingLayers, List<RoutingGeometry> blockages,
			Map<ArcInst,Integer> netIDs)
		{
			this.routingLayers = routingLayers;
			this.blockages = blockages;
			this.netIDs = netIDs;
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
					addGeometry(cell, polys[i], blockages, netID);
				}
				if (info.isRootCell() && ai.getProto() == Generic.tech().unrouted_arc)
					netIDs.put(ai, new Integer(netID));
			}
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				if (ni.isCellInstance()) continue;
				if (ni.getFunction() == PrimitiveNode.Function.PIN) continue;
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
					addGeometry(cell, poly, blockages, netID);
				}
			}
		}

		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) { return true; }

		private void addGeometry(Cell cell, PolyBase poly, List<RoutingGeometry> blockages, int netID)
		{
			RoutingLayer rl = routingLayers.get(poly.getLayer());
			if (rl == null) return;
			Rectangle2D bounds = poly.getBounds2D();
			RoutingGeometry rg = new RoutingGeometry(rl, bounds, netID);
			blockages.add(rg);
		}
	}
}
