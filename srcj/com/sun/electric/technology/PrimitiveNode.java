/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PrimitiveNode.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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
package com.sun.electric.technology;

import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.*;
import com.sun.electric.technology.Technology;

import java.util.Iterator;

/**
 * A PrimitiveNode represents information about a NodeProto that lives in a
 * Technology.  It has a name, and several functions that describe how
 * to draw it
 */
public class PrimitiveNode extends NodeProto
{
	// constants used in the "specialType" field
	/** Defines a normal node. */							public static final int NORMAL = 0;
	/** Defines a serpentine transistor. */					public static final int SERPTRANS = 1;
	/** Defines a polygonal transistor. */					public static final int POLYGONAL = 2;
	/** Defines a multi-cut contact. */						public static final int MULTICUT =  3;

	// --------------------- private data -----------------------------------
	
	/** layers describing this primitive */			private Technology.NodeLayer [] layers;
	/** electrical layers describing this */		private Technology.NodeLayer [] electricalLayers;
	/** flag bits */								private int userBits;
	/** Index of this PrimitiveNode. */				private int primNodeIndex;
	/** special type of unusual primitives */		private int specialType;
	/** special factors for unusual primitives */	private double[] specialValues;
	/** default width and height */					private double defWidth, defHeight;
	/** minimum width and height */					private double minWidth, minHeight;
	/** minimum width and height rule */			private String minSizeRule;
	/** offset from database to user */				private SizeOffset offset;
	/** counter for enumerating primitive nodes */	private static int primNodeNumber = 0;

	// ------------------ private and protected methods ----------------------

	/**
	 * The constructor is never called externally.  Use the factory "newInstance" instead.
	 */
	private PrimitiveNode(String protoName, Technology tech, double defWidth, double defHeight,
		SizeOffset offset, Technology.NodeLayer [] layers)
	{
		// things in the base class
		this.protoName = protoName;

		// things in this class
		this.tech = tech;
		this.layers = layers;
		this.electricalLayers = null;
		this.userBits = 0;
		specialType = NORMAL;
//		this.specialValues = new double [] {0,0,0,0,0,0};
		this.defWidth = defWidth;
		this.defHeight = defHeight;
		if (offset == null) offset = new SizeOffset(0,0,0,0);
		this.offset = offset;
		this.minWidth = this.minHeight = -1;
		this.minSizeRule = "";
		primNodeIndex = primNodeNumber++;

		// add to the nodes in this technology
		tech.addNodeProto(this);
	}

	// ------------------------- public methods -------------------------------

	/**
	 * Method to create a new PrimitiveNode from the parameters.
	 * @param protoName the name of the PrimitiveNode.
	 * Primitive names may not contain unprintable characters, spaces, tabs, a colon (:), semicolon (;) or curly braces ({}).
	 * @param tech the Technology of the PrimitiveNode.
	 * @param width the width of the PrimitiveNode.
	 * @param height the height of the PrimitiveNode.
	 * @param offset the offset from the edges of the reported/selected part of the PrimitiveNode.
	 * @param layers the Layers that comprise the PrimitiveNode.
	 * @return the newly created PrimitiveNode.
	 */
	public static PrimitiveNode newInstance(String protoName, Technology tech, double width, double height,
		SizeOffset offset, Technology.NodeLayer [] layers)
	{
		// check the arguments
		if (tech.findNodeProto(protoName) != null)
		{
			System.out.println("Error: technology " + tech.getTechName() + " has multiple nodes named " + protoName);
			return null;
		}
		if (width < 0.0 || height < 0.0)
		{
			System.out.println("Error: technology " + tech.getTechName() + " node " + protoName + " has negative size");
			return null;
		}

		PrimitiveNode pn = new PrimitiveNode(protoName, tech, width, height, offset, layers);
		return pn;
	}

	/**
	 * Method to return the list of Layers that comprise this PrimitiveNode.
	 * @return the list of Layers that comprise this PrimitiveNode.
	 */
	public Technology.NodeLayer [] getLayers() { return layers; }

	/**
	 * Method to return the list of electrical Layers that comprise this PrimitiveNode.
	 * Like the list returned by "getLayers", the results describe this PrimitiveNode,
	 * but each layer is tied to a specific port on the node.
	 * If any piece of geometry covers more than one port,
	 * it must be split for the purposes of an "electrical" description.<BR>
	 * For example, the MOS transistor has 2 layers: Active and Poly.
	 * But it has 3 electrical layers: Active, Active, and Poly.
	 * The active must be split since each half corresponds to a different PrimitivePort on the PrimitiveNode.
	 * @return the list of electrical Layers that comprise this PrimitiveNode.
	 */
	public Technology.NodeLayer [] getElectricalLayers() { return electricalLayers; }

	/**
	 * Method to set the list of electrical Layers that comprise this PrimitiveNode.
	 * Like the list returned by "getLayers", the results describe this PrimitiveNode,
	 * but each layer is tied to a specific port on the node.
	 * If any piece of geometry covers more than one port,
	 * it must be split for the purposes of an "electrical" description.<BR>
	 * For example, the MOS transistor has 2 layers: Active and Poly.
	 * But it has 3 electrical layers: Active, Active, and Poly.
	 * The active must be split since each half corresponds to a different PrimitivePort on the PrimitiveNode.
	 * @param electricalLayers the list of electrical Layers that comprise this PrimitiveNode.
	 */
	public void setElectricalLayers(Technology.NodeLayer [] electricalLayers) { this.electricalLayers = electricalLayers; }

	/**
	 * Method to find the NodeLayer on this PrimitiveNode with a given Layer.
	 * If there are more than 1 with the given Layer, the first is returned.
	 * @param layer the Layer to find.
	 * @return the NodeLayer that has this Layer.
	 */
	public Technology.NodeLayer findNodeLayer(Layer layer)
	{
		for(int j=0; j<layers.length; j++)
		{
			Technology.NodeLayer oneLayer = layers[j];
			if (oneLayer.getLayer() == layer) return oneLayer;
		}
		return null;
	}

	/**
	 * Method to set the default size of this PrimitiveNode.
	 * @param defWidth the new default width of this PrimitiveNode.
	 * @param defHeight the new default height of this PrimitiveNode.
	 */
	public void setDefSize(double defWidth, double defHeight)
	{
		this.defWidth = defWidth;
		this.defHeight = defHeight;
	}

	/**
	 * Method to return the default width of this PrimitiveNode.
	 * @return the default width of this PrimitiveNode.
	 */
	public double getDefWidth() { return defWidth; }

	/**
	 * Method to return the default height of this PrimitiveNode.
	 * @return the default height of this PrimitiveNode.
	 */
	public double getDefHeight() { return defHeight; }

	/**
	 * Method to get the size offset of this PrimitiveNode.
	 * @return the size offset of this PrimitiveNode.
	 */
	public SizeOffset getSizeOffset() { return offset; }

	/**
	 * Method to return the minimum width of this PrimitiveNode.
	 * @return the minimum width of this PrimitiveNode.
	 */
	public double getMinWidth() { return minWidth; }

	/**
	 * Method to return the minimum height of this PrimitiveNode.
	 * @return the minimum height of this PrimitiveNode.
	 */
	public double getMinHeight() { return minHeight; }

	/**
	 * Method to return the minimum size rule for this PrimitiveNode.
	 * @return the minimum size rule for this PrimitiveNode.
	 */
	public String getMinSizeRule() { return minSizeRule; }

	/**
	 * Method to set the minimum height of this PrimitiveNode.
	 * @param minHeight the minimum height of this PrimitiveNode.
	 */
	public void setMinSize(double minWidth, double minHeight, String minSizeRule)
	{
		this.minWidth = minWidth;
		this.minHeight = minHeight;
		this.minSizeRule = minSizeRule;
	}

	/**
	 * Method to set the size offset of this PrimitiveNode.
	 * @param offset the size offset of this PrimitiveNode.
	 */
	public void setSizeOffset(SizeOffset offset) { this.offset = offset; }

	/**
	 * Method to return the Technology of this PrimitiveNode.
	 * @return the Technology of this PrimitiveNode.
	 */
	public Technology getTechnology() { return tech; }

	public void addPrimitivePorts(PrimitivePort [] ports)
	{
		for(int i = 0; i < ports.length; i++)
		{
			ports[i].setParent(this);
			addPort(ports[i], null);
		}
	}

	/**
	 * Method to return the special type of this PrimitiveNode.
	 * It can be one of NORMAL, SERPTRANS, POLYGONAL, or MULTICUT.
	 * @return the special type of this PrimitiveNode.
	 */
	public int getSpecialType() { return specialType; }

	/**
	 * Method to set the special type of this PrimitiveNode.
	 * @param specialType the newspecial type of this PrimitiveNode.
	 * It can be NORMAL, SERPTRANS, POLYGONAL, or MULTICUT.
	 */
	public void setSpecialType(int specialType) { this.specialType = specialType; }

	/**
	 * Method to return the special values stored on this PrimitiveNode.
	 * The special values are an array of integers that describe unusual features of the PrimitiveNode.
	 * They are only relevant for certain specialType cases:
	 * <UL>
	 * <LI>for MULTICUT:
	 *   <UL>
	 *   <LI>cut size is [0] x [1]
	 *   <LI>cut indented [2] from highlighting
	 *   <LI>cuts spaced [3] apart
	 *   </UL>
	 * <LI>for SERPTRANS:
	 *   <UL>
	 *   <LI>layer count is [0]
	 *   <LI>active port inset [1] from end of serpentine path
	 *   <LI>active port is [2] from poly edge
	 *   <LI>poly width is [3]
	 *   <LI>poly port inset [4] from poly edge
	 *   <LI>poly port is [5] from active edge
	 *   </UL>
	 * @return the special values stored on this PrimitiveNode.
	 */
	public double [] getSpecialValues() { return specialValues; }

	/**
	 * Method to set the special values stored on this PrimitiveNode.
	 * The special values are an array of values that describe unusual features of the PrimitiveNode.
	 * The meaning depends on the specialType (see the documentation for "getSpecialValues").
	 * @param specialValues the special values for this PrimitiveNode.
	 */
	public void setSpecialValues(double [] specialValues) { this.specialValues = specialValues; }

	/**
	 * Method to tell whether this PrimitiveNode is a Pin.
	 * Pin nodes have one port, no valid geometry, and are used to connect arcs.
	 * @return true if this PrimitiveNode is a Pin.
	 */
	public boolean isPin()
	{
		return (getFunction() == NodeProto.Function.PIN);
	}

	/**
	 * Method to describe this PrimitiveNode as a string.
	 * If the primitive is not from the current technology, prepend the technology name.
	 * @return a description of this PrimitiveNode.
	 */
	public String describe()
	{
		String name = "";
		if (tech != Technology.getCurrent())
			name += tech.getTechName() + ":";
		name += protoName;
		return name;
	}

	/**
	 * Method to get the index of this PrimitiveNode.
	 * @return the index of this PrimitiveNode.
	 */
	public final int getPrimNodeIndex() { return primNodeIndex; }

	/**
	 * Returns a printable version of this PrimitiveNode.
	 * @return a printable version of this PrimitiveNode.
	 */
	public String toString()
	{
		return "PrimitiveNode " + describe();
	}
    
}
