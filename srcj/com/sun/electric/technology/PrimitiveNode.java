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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.util.Iterator;

/**
 * A PrimitiveNode represents information about a NodeProto that lives in a
 * Technology.  It has a name, and several functions that describe how
 * to draw it
 */
public class PrimitiveNode extends NodeProto
{
	// constants used in the "specialValues" field
	/** serpentine transistor */					public static final int SERPTRANS = 1;
	/** polygonally defined transistor */			public static final int POLYGONAL = 2;
	/** multi-cut contact */						public static final int MULTICUT =  3;
	/** MOS transistor (nonserpentine) */			public static final int MOSTRANS =  4;

	// --------------------- private data -----------------------------------
	
	private static final Point2D.Double ORIGIN = new Point2D.Double(0, 0);

	/** layers describing this primitive */			private Technology.NodeLayer [] layers;
	/** flag bits */								private int userBits;
	/** special factors for unusual primitives */	private int[] specialValues;
	/** default width and height */					private double defWidth, defHeight;
	/** offset from database to user */				private SizeOffset offset;

	// ------------------ private and protected methods ----------------------
	
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
		this.offset = offset;

		// add to the nodes in this technology
		tech.addNodeProto(this);
	}

	// ------------------------- public methods -------------------------------

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

	public Technology.NodeLayer [] getLayers() { return layers; }
	
	public void setDefSize(double defWidth, double defHeight)
	{
		this.defWidth = defWidth;
		this.defHeight = defHeight;
	}
	public double getDefWidth() { return defWidth; }
	public double getDefHeight() { return defHeight; }

//	public void setSizeOffset(double widthOffset, double heightOffset)
//	{
//		this.widthOffset = widthOffset;
//		this.heightOffset = heightOffset;
//	}
	public double getLowXOffset() { return offset.getLowXOffset(); }
	public double getHighXOffset() { return offset.getHighXOffset(); }
	public double getLowYOffset() { return offset.getLowYOffset(); }
	public double getHighYOffset() { return offset.getHighYOffset(); }
	public Technology getTechnology() { return tech; }

	public void addPrimitivePorts(PrimitivePort [] ports)
	{
		for(int i = 0; i < ports.length; i++)
		{
			ports[i].setParent(this);
		}
	}

	public int [] getSpecialValues() { return specialValues; }
	public void setSpecialValues(int [] specialValues) { this.specialValues = specialValues; }

	public Point2D.Double getReferencePoint() { return ORIGIN; }

	/**
	 * Does this PrimitiveNode act as a Pin (one port, small size)?
	 */
	public boolean isPin()
	{
		return (getFunction() == NodeProto.Function.PIN);
	}

	/** A PrimitiveNode is its own equivalent. */
	public NodeProto getEquivalent()
	{
		return this;
	}
	
	/**
	 * Routine to describe this Primitive NodeProto as a string.
	 * If the primitive is not from the current technology, prepend the technology name.
	 */
	public String describe()
	{
		String name = "";
		if (tech != Technology.getCurrent())
			name += tech.getTechName() + ":";
		name += protoName;
		return name;
	}

	public String toString()
	{
		return "PrimitiveNode " + describe();
	}

//	/**
//	 * Get the Electric bounds.  This excludes invisible widths. Base units
//	 */
//	Rectangle2D getVisBounds()
//	{
//		return new Rectangle2D.Double(
//			elecBounds.x + sizeOffset.lx,
//			elecBounds.y + sizeOffset.ly,
//			elecBounds.width - sizeOffset.lx - sizeOffset.hx,
//			elecBounds.height - sizeOffset.ly - sizeOffset.hy);
//	}

//	public SizeOffset getSizeOffset()
//	{
//		// Essential bounds never have a size offset
//		return sizeOffset;
//	}

}
