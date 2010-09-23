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

import com.sun.electric.util.math.Orientation;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;


/**
 * Class to define a framework for Placement algorithms. To make Placement
 * algorithms easier to write, all Placement algorithms must extend this class.
 * The Placement algorithm then defines two methods:
 * 
 * public String getAlgorithmName() returns the name of the Placement algorithm
 * 
 * void runPlacement(List<PlacementNode> nodesToPlace, List<PlacementNetwork>
 * allNetworks, String cellName) runs the placement on the "nodesToPlace",
 * calling each PlacementNode's "setPlacement()" and "setOrientation()" methods
 * to establish the proper placement.
 * 
 * To avoid the complexities of the Electric database, three shadow-classes are
 * defined that describe the necessary information that Placement algorithms
 * want:
 * 
 * PlacementNode is an actual node (primitive or cell instance) that is to be
 * placed. PlacementPort is the connection site on the PlacementNode. Each
 * PlacementNode has a list of zero or more PlacementPort objects, and each
 * PlacementPort points to its "parent" PlacementNode. PlacementNetwork
 * determines which PlacementPort objects will connect together. Each
 * PlacementNetwork has a list of two or more PlacementPort objects that it
 * connects, and each PlacementPort has a PlacementNetwork in which it resides.
 */
public abstract class PlacementFrame {

	protected int numOfThreads;
	protected int runtime;
    private ArrayList<PlacementParameter> allParameters = new ArrayList<PlacementParameter>();

	/**
	 * Method to do Placement (overridden by actual Placement algorithms).
	 * 
	 * @param nodesToPlace
	 *            a list of all nodes that are to be placed.
	 * @param allNetworks
	 *            a list of all networks that connect the nodes.
	 * @param cellName
	 *            the name of the cell being placed.
	 */
	protected abstract void runPlacement(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks, String cellName);

	/**
	 * Method to return the name of the placement algorithm (overridden by
	 * actual Placement algorithms).
	 * 
	 * @return the name of the placement algorithm.
	 */
	public abstract String getAlgorithmName();

	/**
	 * Method to return a list of parameters for this placement algorithm.
	 * 
	 * @return a list of parameters for this placement algorithm.
	 */
	public final List<PlacementParameter> getParameters() {
		return allParameters;
	}

	public void setParamterValues(int threads, int runtime) {
		this.numOfThreads = threads;
		this.runtime = runtime;
	}

	/**
	 * Class to define a parameter for a placement algorithm.
	 */
	public class PlacementParameter {
		public static final int TYPEINTEGER = 1;
		public static final int TYPESTRING = 2;
		public static final int TYPEDOUBLE = 3;

        final String key;
		private final String title;
        final Object factoryValue;
        private Object cachedValue;
		private final int type;

		public PlacementParameter(String name, String title, int factory) {
            key = getAlgorithmName() + "-" + name;
			this.title = title;
            cachedValue = factoryValue = Integer.valueOf(factory);
			type = TYPEINTEGER;
//			pref = Pref.makeIntPref(getAlgorithmName() + "-" + name, Placement.getPlacementTool().prefs, factory);
            allParameters.add(this);
		}

		public PlacementParameter(String name, String title, String factory) {
            key = getAlgorithmName() + "-" + name;
			this.title = title;
            cachedValue = factoryValue = String.valueOf(factory);
			type = TYPESTRING;
//			pref = Pref.makeStringPref(getAlgorithmName() + "-" + name, Placement.getPlacementTool().prefs, factory);
            allParameters.add(this);
		}

		public PlacementParameter(String name, String title, double factory) {
            key = getAlgorithmName() + "-" + name;
			this.title = title;
            cachedValue = factoryValue = Double.valueOf(factory);
			type = TYPEDOUBLE;
//			pref = Pref.makeDoublePref(getAlgorithmName() + "-" + name, Placement.getPlacementTool().prefs, factory);
            allParameters.add(this);
		}

        public PlacementFrame getOwner() {
            return PlacementFrame.this;
        }

		public String getName() {
			return title;
		}

		public int getType() {
			return type;
		}

		public int getIntValue() {
            return ((Integer)cachedValue).intValue();
		}

		public String getStringValue() {
            return (String)cachedValue;
		}

		public double getDoubleValue() {
            return ((Double)cachedValue).doubleValue();
		}

        void setValue(Object value) {
            assert value.getClass() == factoryValue.getClass();
            if (value.equals(factoryValue))
                value = factoryValue;
            cachedValue = value;
        }
	}

	/**
	 * Class to define a node that is being placed. This is a shadow class for
	 * the internal Electric object "NodeInst". There are minor differences
	 * between PlacementNode and NodeInst, for example, PlacementNode is
	 * presumed to be centered in the middle, with port offsets based on that
	 * center, whereas the NodeInst has a cell-center that may not be in the
	 * middle.
	 */
	public abstract static class PlacementNode {
		private double xPos, yPos;
		private Orientation orient;
		private Object userObject;

		public Object getUserObject() {
			return userObject;
		}

		public void setUserObject(Object obj) {
			userObject = obj;
		}

		/**
		 * Method to return a list of PlacementPorts on this PlacementNode.
		 * 
		 * @return a list of PlacementPorts on this PlacementNode.
		 */
		public abstract List<PlacementPort> getPorts();

		/**
		 * Method to return the width of this PlacementNode.
		 * 
		 * @return the width of this PlacementNode.
		 */
		public abstract double getWidth();

		/**
		 * Method to return the height of this PlacementNode.
		 * 
		 * @return the height of this PlacementNode.
		 */
		public abstract double getHeight();

		/**
		 * Method to set the location of this PlacementNode. The Placement
		 * algorithm must call this method to set the final location of the
		 * PlacementNode.
		 * 
		 * @param x
		 *            the X-coordinate of the center of this PlacementNode.
		 * @param y
		 *            the Y-coordinate of the center of this PlacementNode.
		 */
		public void setPlacement(double x, double y) {
			xPos = x;
			yPos = y;
		}

		/**
		 * Method to set the orientation (rotation and mirroring) of this
		 * PlacementNode. The Placement algorithm may call this method to set
		 * the final orientation of the PlacementNode.
		 * 
		 * @param o
		 *            the Orientation of this PlacementNode.
		 */
		public void setOrientation(Orientation o) {
			orient = o;
			for (PlacementPort plPort : getPorts())
				plPort.computeRotatedOffset();
		}

		/**
		 * Method to return the X-coordinate of the placed location of this
		 * PlacementNode. This is the location that the Placement algorithm has
		 * established for this PlacementNode.
		 * 
		 * @return the X-coordinate of the placed location of this
		 *         PlacementNode.
		 */
		public double getPlacementX() {
			return xPos;
		}

		/**
		 * Method to return the Y-coordinate of the placed location of this
		 * PlacementNode. This is the location that the Placement algorithm has
		 * established for this PlacementNode.
		 * 
		 * @return the Y-coordinate of the placed location of this
		 *         PlacementNode.
		 */
		public double getPlacementY() {
			return yPos;
		}

		/**
		 * Method to return the Orientation of this PlacementNode. This is the
		 * Orientation that the Placement algorithm has established for this
		 * PlacementNode.
		 * 
		 * @return the Orientation of this PlacementNode.
		 */
		public Orientation getPlacementOrientation() {
			return orient;
		}

		/**
		 * Method to return the name of NodeProto of this PlacementNode.
		 *
		 * @return the NodeProto of this PlacementNode.
		 */
		public abstract String getTypeName();
	}

	/**
	 * Class to define ports on PlacementNode objects. This is a shadow class
	 * for the internal Electric object "PortInst".
	 */
	public static class PlacementPort {
		private double offX, offY;
		private double rotatedOffX, rotatedOffY;
		private PlacementNode plNode;
		private PlacementNetwork plNet;

		/**
		 * Constructor to create a PlacementPort.
		 * 
		 * @param x
		 *            the X offset of this PlacementPort from the center of its
		 *            PlacementNode.
		 * @param y
		 *            the Y offset of this PlacementPort from the center of its
		 *            PlacementNode.
		 */
		public PlacementPort(double x, double y) {
			offX = x;
			offY = y;
		}

		/**
		 * Method to set the "parent" PlacementNode on which this PlacementPort
		 * resides.
		 * 
		 * @param pn
		 *            the PlacementNode on which this PlacementPort resides.
		 */
		public void setPlacementNode(PlacementNode pn) {
			plNode = pn;
		}

		/**
		 * Method to return the PlacementNode on which this PlacementPort
		 * resides.
		 * 
		 * @return the PlacementNode on which this PlacementPort resides.
		 */
		public PlacementNode getPlacementNode() {
			return plNode;
		}

		/**
		 * Method to return the PlacementNetwork on which this PlacementPort
		 * resides.
		 * 
		 * @param pn
		 *            the PlacementNetwork on which this PlacementPort resides.
		 */
		public void setPlacementNetwork(PlacementNetwork pn) {
			plNet = pn;
		}

		/**
		 * Method to return the PlacementNetwork on which this PlacementPort
		 * resides.
		 * 
		 * @return the PlacementNetwork on which this PlacementPort resides. If
		 *         this PlacementPort does not connect to any other
		 *         PlacementPort, the PlacementNetwork may be null.
		 */
		public PlacementNetwork getPlacementNetwork() {
			return plNet;
		}

		/**
		 * Method to return the offset of this PlacementPort's X coordinate from
		 * the center of its PlacementNode. The offset is valid when no
		 * Orientation has been applied.
		 * 
		 * @return the offset of this PlacementPort's X coordinate from the
		 *         center of its PlacementNode.
		 */
		public double getOffX() {
			return offX;
		}

		/**
		 * Method to return the offset of this PlacementPort's Y coordinate from
		 * the center of its PlacementNode. The offset is valid when no
		 * Orientation has been applied.
		 * 
		 * @return the offset of this PlacementPort's Y coordinate from the
		 *         center of its PlacementNode.
		 */
		public double getOffY() {
			return offY;
		}

		/**
		 * Method to return the offset of this PlacementPort's X coordinate from
		 * the center of its PlacementNode. The coordinate assumes that the
		 * PlacementNode has been rotated by its Orientation.
		 * 
		 * @return the offset of this PlacementPort's X coordinate from the
		 *         center of its PlacementNode.
		 */
		public double getRotatedOffX() {
			return rotatedOffX;
		}

		/**
		 * Method to return the offset of this PlacementPort's Y coordinate from
		 * the center of its PlacementNode. The coordinate assumes that the
		 * PlacementNode has been rotated by its Orientation.
		 * 
		 * @return the offset of this PlacementPort's Y coordinate from the
		 *         center of its PlacementNode.
		 */
		public double getRotatedOffY() {
			return rotatedOffY;
		}

		/**
		 * Internal method to compute the rotated offset of this PlacementPort
		 * assuming that the Orientation of its PlacementNode has changed. TODO:
		 * why is this public? it should not be accessed!
		 */
		public void computeRotatedOffset() {
			Orientation orient = plNode.getPlacementOrientation();
			if (orient == Orientation.IDENT) {
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

	/**
	 * Class to define networks of PlacementPort objects. This is a shadow class
	 * for the internal Electric object "Network", but it is simplified for
	 * Placement.
	 */
	public static class PlacementNetwork {
		private List<PlacementPort> portsOnNet;

		/**
		 * Constructor to create this PlacementNetwork with a list of
		 * PlacementPort objects that it connects.
		 * 
		 * @param ports
		 *            a list of PlacementPort objects that it connects.
		 */
		public PlacementNetwork(List<PlacementPort> ports) {
			portsOnNet = ports;
		}

		/**
		 * Method to return the list of PlacementPort objects on this
		 * PlacementNetwork.
		 * 
		 * @return a list of PlacementPort objects on this PlacementNetwork.
		 */
		public List<PlacementPort> getPortsOnNet() {
			return portsOnNet;
		}
	}
}
