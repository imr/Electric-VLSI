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

	private Technology tech; // which Technology do I live in?
	private PrimitivePort [] ports;
	private NodeLayer [] layers;
	private int bits; // user bits

	// Electric's size offsets. This is the invisible portion of the bounds.
	private SizeOffset sizeOffset;

	// ------------------ private and protected methods ----------------------
	public PrimitiveNode(String protoName, Technology tech, double sx, double sy, double xo, double yo,
		PrimitivePort [] ports, NodeLayer [] layers, int bits, int [] specialValues)
	{
		this.protoName = protoName;
		this.tech = tech;
		elecBounds = new Rectangle2D.Double(-sx/2, -sy/2, sx, sy);
		sizeOffset = new SizeOffset(-sx/2, -sy/2, sx/2, sy/2);
		this.ports = ports;
		this.layers = layers;
		this.bits = bits;
		this.tech.addNodeProto(this);
	}

	Point2D.Double getRefPointBase()
	{
		return ORIGIN;
	}

	protected void getInfo()
	{
		System.out.println(" Name: " + protoName + '\n' + " Technology: " + tech);
		super.getInfo();
	}

	// ----------------------------- public data ------------------------------
	/** this node is a Pin */
	public static final int NPPIN = 1;

	/** this node is a Contact */
	public static final int NPCONTACT = 2;

	/** this node is a pure layer node */
	public static final int NPNODE = 3;

	// ------------------------- public methods -------------------------------
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
		return ((bits & NFUNCTION) >> NFUNCTIONSH);
	}

	/**
	 * Does this PrimitiveNode act as a Pin (one port, small size)?
	 */
	public boolean isPin()
	{
		return ((bits & 0374) >> 2) == 1; // NFUNCTION >> NFUNCTIONSH = NPPIN
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
	
	public String describeNodeProto()
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
