package com.sun.electric.database;

import com.sun.electric.technologies.*;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;

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

	/** technology of this primitive */				private Technology tech;
	/** layers describing this primitive */			private Technology.NodeLayer [] layers;
	/** flag bits */								private int userBits;
	/** special factors for unusual primitives */	private int[] specialValues;
	/** default width and height */					private double defWidth, defHeight;
	/** offset from database to user */				private double widthOffset, heightOffset;

	// ------------------ private and protected methods ----------------------
	
	private PrimitiveNode(String protoName, Technology tech, double defWidth, double defHeight,
		double widthOffset, double heightOffset, Technology.NodeLayer [] layers)
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
		this.widthOffset = widthOffset;
		this.heightOffset = heightOffset;

		// add to the nodes in this technology
		tech.addNodeProto(this);
	}

	// ------------------------- public methods -------------------------------

	public static PrimitiveNode newInstance(String protoName, Technology tech, double width, double height,
		double widthOffset, double heightOffset, Technology.NodeLayer [] layers)
	{
		PrimitiveNode pn = new PrimitiveNode(protoName, tech, width, height, widthOffset, heightOffset, layers);
		return pn;
	}

	/** Get the Technology to which this PrimitiveNode belongs */
	public Technology getTechnology() { return tech; }

	public Technology.NodeLayer [] getLayers() { return layers; }
	
	public void setDefSize(double defWidth, double defHeight)
	{
		this.defWidth = defWidth;
		this.defHeight = defHeight;
	}
	public double getDefWidth() { return defWidth; }
	public double getDefHeight() { return defHeight; }

	public void setSizeOffset(double widthOffset, double heightOffset)
	{
		this.widthOffset = widthOffset;
		this.heightOffset = heightOffset;
	}
	public double getWidthOffset() { return widthOffset; }
	public double getHeightOffset() { return heightOffset; }

	public void AddPrimitivePorts(PrimitivePort [] ports)
	{
		for(int i = 0; i < ports.length; i++)
		{
//			addPort(ports[i]);
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
//	Rectangle2D.Double getVisBounds()
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
