package com.sun.electric.database;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A PrimitivePort lives in a tecnology layer, and is always associated
 * with a primitive node.  It contains a list of ArcProto types that it
 * accepts connections from.  All Exports eventually ground out in a
 * PrimitivePort.
 */
public class PrimitivePort extends PortProto
{
	// ---------------------------- private data --------------------------
	//private ShapeProxy proxy; // a proxy determines the port's bounds
	private ArcProto portArcs[]; // Immutable list of possible connection types.
	private EdgeH left;
	private EdgeV bottom;
	private EdgeH right;
	private EdgeV top;
	private Technology tech;
	protected PortProto.Function function;

	// ---------------------- protected and private methods ----------------
	private PrimitivePort(Technology tech, PrimitiveNode parent, ArcProto [] portArcs, String protoName,
		int portAngle, int portRange, int portTopology, PortProto.Function function,
		EdgeH left, EdgeV bottom, EdgeH right, EdgeV top)
	{
		// initialize the parent object
		this.parent = parent;
		this.protoName = protoName;
		this.userBits = (portAngle<<PortProto.PORTANGLESH) |
						(portRange << PortProto.PORTARANGESH) |
						(portTopology << PortProto.PORTNETSH);

		// initialize this object
		this.tech = tech;
		this.portArcs = portArcs;
		this.function = function;
		this.left = left;
		this.bottom = bottom;
		this.right = right;
		this.top = top;
	}

	public static PrimitivePort newInstance(Technology tech, PrimitiveNode parent, ArcProto [] portArcs, String protoName,
		int portAngle, int portRange, int portTopology, PortProto.Function function,
		EdgeH left, EdgeV bottom, EdgeH right, EdgeV top)
	{
		PrimitivePort pp = new PrimitivePort(tech, parent, portArcs, protoName,
			portAngle, portRange, portTopology, function,
			left, bottom, right, top);
		return pp;
	}

	// Get the bounds for this port with respect to some NodeInst.
	public Poly getPoly(NodeInst ni)
	{
		return tech.getPoly(ni, this);
	}

	protected void getInfo()
	{
		System.out.println(" Connection types: " + portArcs.length);
		for (int i = 0; i < portArcs.length; i++)
		{
			System.out.println("   * " + portArcs[i]);
		}
		super.getInfo();
	}

	// ------------------------ public methods ------------------------

	/** Return this object (it *is* a base port) */
	public PrimitivePort getBasePort() { return this; }
	public EdgeH getLeft() { return left; }
	public EdgeH getRight() { return right; }
	public EdgeV getTop() { return top; }
	public EdgeV getBottom() { return bottom; }

	/** can this port connect to a particular arc type?
	 * @param arc the arc type to test for
	 * @return true if this port can connect to the arc, false if it can't */
	public boolean connectsTo(ArcProto arc)
	{
		for (int i = 0; i < portArcs.length; i++)
		{
			if (portArcs[i] == arc)
				return true;
		}
		return false;
	}

	class ArcTypeIterator implements Iterator
	{
		ArcProto types[];
		int idx;

		public ArcTypeIterator(ArcProto types[])
		{
			this.types = types;
			this.idx = 0;
		}

		public boolean hasNext()
		{
			return idx < types.length;
		}

		public Object next() throws NoSuchElementException
		{
			if (idx >= types.length)
			{
				throw new NoSuchElementException("No more portArcs");
			}
			return types[idx++];
		}

		public void remove() throws UnsupportedOperationException
		{
			throw new UnsupportedOperationException("Can't remove ArcType");
		}
	}

	/** Get an iterator over the possible arc connection types */
	public Iterator getConnectionTypes()
	{
		return new ArcTypeIterator(portArcs);
	}

	public String toString()
	{
		return "PrimitivePort " + protoName;
	}

	public PortProto getEquivalent() { return this; }
	
	/** Get the first "real" (non-zero width) ArcProto style that can
	 * connect to this port. */
//	public ArcProto getWire()
//	{
//		for (int i = 0; i < portArcs.length; i++)
//		{
//			if (portArcs[i].getBaseWidth() > 0)
//				return portArcs[i];
//		}
//		return portArcs[0];
//	}
}
