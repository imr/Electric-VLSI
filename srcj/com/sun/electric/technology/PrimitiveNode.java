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
import com.sun.electric.technology.Technology;

/**
 * A PrimitiveNode represents information about a NodeProto that lives in a
 * Technology.  It has a name, and several functions that describe how
 * to draw it
 */
public class PrimitiveNode extends NodeProto
{
	// constants used in the "specialValues" field
	/** Defines a serpentine transistor. */					public static final int SERPTRANS = 1;
	/** Defines a polygonal transistor. */					public static final int POLYGONAL = 2;
	/** Defines a multi-cut contact. */						public static final int MULTICUT =  3;

	// --------------------- private data -----------------------------------
	
	/** layers describing this primitive */			private Technology.NodeLayer [] layers;
	/** flag bits */								private int userBits;
	/** special factors for unusual primitives */	private int[] specialValues;
	/** default width and height */					private double defWidth, defHeight;
	/** offset from database to user */				private SizeOffset offset;

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
		this.userBits = 0;
		this.specialValues = new int [] {0,0,0,0,0,0};
		this.defWidth = defWidth;
		this.defHeight = defHeight;
		if (offset == null) offset = new SizeOffset(0,0,0,0);
		this.offset = offset;

		// add to the nodes in this technology
		tech.addNodeProto(this);
	}

	/**
	 * Add connections inside node proto to map of equivalent ports newEquivPort.
	 * Assume that each PortProto.getTempInt() contains sequential index of
     * port in this NodeProto.
	 */
	protected void connectEquivPorts(int[] newEquivPorrs)
	{
		/* Commented, hbecause getTopology() is not set properly now
		int i = 0;
		for (Iterator it = getPorts(); it.hasNext(); i++)
		{
			PrimitivePort pi = (PrimitivePort)it.next();
			int j = 0;
			for (Iterator jt = getPorts(); j < i; j++)
			{
				PrimitivePort pj = (PrimitivePort)jt.next();
				if (pi.getTopology() == pj.getTopology())
				{
					connectMap(newEquivPorts, i, j);
					break;
				}
			}
		}
		*/
	}

	// ------------------------- public methods -------------------------------

	/**
	 * Routine to create a new PrimitiveNode from the parameters.
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
	 * Routine to return the list of Layers that comprise this PrimitiveNode.
	 * @return the list of Layers that comprise this PrimitiveNode.
	 */
	public Technology.NodeLayer [] getLayers() { return layers; }

	/**
	 * Routine to set the default size of this PrimitiveNode.
	 * @param defWidth the new default width of this PrimitiveNode.
	 * @param defHeight the new default height of this PrimitiveNode.
	 */
	public void setDefSize(double defWidth, double defHeight)
	{
		this.defWidth = defWidth;
		this.defHeight = defHeight;
	}

	/**
	 * Routine to return the default width of this PrimitiveNode.
	 * @return the default width of this PrimitiveNode.
	 */
	public double getDefWidth() { return defWidth; }

	/**
	 * Routine to return the default height of this PrimitiveNode.
	 * @return the default height of this PrimitiveNode.
	 */
	public double getDefHeight() { return defHeight; }

	/**
	 * Routine to size offset of this PrimitiveNode.
	 * @return the size offset of this PrimitiveNode.
	 */
	public SizeOffset getSizeOffset() { return offset; }

	/**
	 * Routine to return the Technology of this PrimitiveNode.
	 * @return the Technology of this PrimitiveNode.
	 */
	public Technology getTechnology() { return tech; }

	public void addPrimitivePorts(PrimitivePort [] ports)
	{
		for(int i = 0; i < ports.length; i++)
		{
			ports[i].setParent(this);
		}
	}

	/**
	 * Routine to return the special values stored on this PrimitiveNode.
	 * The special values are an array of integers that describe unusual features of the PrimitiveNode.
	 * Element [0] of the array is one of SERPTRANS, POLYGONAL, or MULTICUT.
	 * Other values depend on the first entry:
	 * <UL>
	 * <LI>for MULTICUT:
	 *   <UL>
	 *   <LI>cut size is [1] x [2]
	 *   <LI>cut indented [3] from highlighting
	 *   <LI>cuts spaced [4] apart
	 *   </UL>
	 * <LI>for SERPTRANS:
	 *   <UL>
	 *   <LI>layer count is [1]
	 *   <LI>active port inset [2] from end of serpentine path
	 *   <LI>active port is [3] from poly edge
	 *   <LI>poly width is [4]
	 *   <LI>poly port inset [5] from poly edge
	 *   <LI>poly port is [6] from active edge
	 *   </UL>
	 * @return the special values stored on this PrimitiveNode.
	 */
	public int [] getSpecialValues() { return specialValues; }

	/**
	 * Routine to set the special values stored on this PrimitiveNode.
	 * The special values are an array of integers that describe unusual features of the PrimitiveNode.
	 * The first element of the array is one of SERPTRANS, POLYGONAL, MULTICUT, or MOSTRANS.
	 * Other values depend on the first entry (see the documentation for "getSpecialValues").
	 * @param specialValues the special values for this PrimitiveNode.
	 */
	public void setSpecialValues(int [] specialValues) { this.specialValues = specialValues; }

	/**
	 * Does this PrimitiveNode act as a Pin (one port, small size)?
	 */
	public boolean isPin()
	{
		return (getFunction() == NodeProto.Function.PIN);
	}

	/**
	 * Routine to describe this PrimitiveNode as a string.
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
	 * Returns a printable version of this PrimitiveNode.
	 * @return a printable version of this PrimitiveNode.
	 */
	public String toString()
	{
		return "PrimitiveNode " + describe();
	}
}
