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
	// --------------------- private data -----------------------------------
	
	private static final Point2D.Double ORIGIN_2D = new Point2D.Double(0, 0);
	private static final Point2D.Double ORIGIN = new Point2D.Double(0, 0);

	private Technology tech;
	private Technology.NodeLayer [] layers;
	private int userBits;
	private int[] specialValues;
	private SizeOffset sizeOffset;

	// ------------------ private and protected methods ----------------------
	
	private PrimitiveNode(String protoName, Technology tech, double sx, double sy, double xo, double yo,
		PrimitivePort [] ports, Technology.NodeLayer [] layers, int userBits)
	{
		// things in the base class
		this.protoName = protoName;
		elecBounds = new Rectangle2D.Double(-sx/2, -sy/2, sx, sy);

		// things in this class
		this.tech = tech;
		for(int i = 0; i < ports.length; i++)
			addPort(ports[i]);
		this.layers = layers;
		this.userBits = userBits;
		this.specialValues = new int [] {0,0,0,0,0,0};
		this.sizeOffset = new SizeOffset(-sx/2, -sy/2, sx/2, sy/2);

		// add to the nodes in this technology
		tech.addNodeProto(this);
	}

	// ------------------------- public methods -------------------------------

	public static PrimitiveNode newInstance(String protoName, Technology tech, double sx, double sy, double xo, double yo,
		PrimitivePort [] ports, Technology.NodeLayer [] layers, int userBits)
	{
		PrimitiveNode pn = new PrimitiveNode(protoName, tech, sx, sy, xo, yo, ports, layers, userBits);
		return pn;
	}

	public void setSpecialValues(int [] specialValues)
	{
		this.specialValues = specialValues;
	}

	Point2D.Double getRefPointBase()
	{
		return ORIGIN;
	}

	public Point2D.Double getReferencePoint()
	{
		return ORIGIN_2D;
	}

	/**
	 * Get the Electric bounds.  This excludes invisible widths. Base units
	 */
	Rectangle2D.Double getVisBounds()
	{
		return new Rectangle2D.Double(
			elecBounds.x + sizeOffset.lx,
			elecBounds.y + sizeOffset.ly,
			elecBounds.width - sizeOffset.lx - sizeOffset.hx,
			elecBounds.height - sizeOffset.ly - sizeOffset.hy);
	}

	public SizeOffset getSizeOffset()
	{
		// Essential bounds never have a size offset
		return sizeOffset;
	}

	/**
	 * get the function of this node
	 */
	public final int getFunction()
	{
		return ((userBits & NFUNCTION) >> NFUNCTIONSH);
	}

	/**
	 * Does this PrimitiveNode act as a Pin (one port, small size)?
	 */
	public boolean isPin()
	{
		return ((userBits & NFUNCTION) >> NFUNCTIONSH) == NPPIN;
	}

	/** Get the Technology to which this PrimitiveNode belongs */
	public Technology getTechnology()
	{
		return tech;
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
		return "PrimitiveNode " + protoName + " (" + tech.getTechName() + ")";
	}
	
}
