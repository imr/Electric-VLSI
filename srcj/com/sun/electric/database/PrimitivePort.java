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
	private String protoName;
	private int initialBits;
	private int [][] location;

	// ---------------------- protected and private methods ----------------
	private PrimitivePort(ArcProto [] portArcs, String protoName, int portAngle, int portRange, int portTopology, int [][] location)
	{
		this.portArcs = portArcs;
		this.protoName = protoName;
		this.initialBits = (portAngle<<PortProto.PORTANGLESH) |
						(portRange << PortProto.PORTARANGESH) |
						(portTopology << PortProto.PORTNETSH);
		this.location = location;
	}

	public static PrimitivePort newInstance(ArcProto [] portArcs, String protoName, int portAngle, int portRange, int portTopology,
		int [][] location)
	{
		PrimitivePort pp = new PrimitivePort(portArcs, protoName, portAngle, portRange, portTopology, location);
		return pp;
	}

	// Get the bounds for this port with respect to some NodeInst.  A
	// port can scale with its owner, so it doesn't make sense to ask
	// for the bounds of a port without a NodeInst.  The NodeInst should
	// be an instance of the NodeProto that this port belongs to,
	// although this isn't checked.
	Poly getBounds(NodeInst ni)
	{
		Poly portpoly = new Poly(null, 0, 0, 0, 0);
		return portpoly;
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
	public String toString()
	{
		return "PrimitivePort " + getName();
	}

	/** Return this object (it *is* a base port) */
	public PrimitivePort getBasePort()
	{
		return this;
	}

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
}
