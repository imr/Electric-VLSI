/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PrimitivePort.java
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

import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.technology.technologies.TecGeneric;

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
		if (tech != TecGeneric.tech && TecGeneric.tech != null)
		{
			ArcProto [] realPortArcs = new ArcProto[portArcs.length + 3];
			for(int i=0; i<portArcs.length; i++)
				realPortArcs[i] = portArcs[i];
			realPortArcs[portArcs.length] = TecGeneric.tech.universal_arc;
			realPortArcs[portArcs.length+1] = TecGeneric.tech.invisible_arc;
			realPortArcs[portArcs.length+2] = TecGeneric.tech.unrouted_arc;
			portArcs = realPortArcs;
		}
		PrimitivePort pp = new PrimitivePort(tech, parent, portArcs, protoName,
			portAngle, portRange, portTopology, function,
			left, bottom, right, top);
		return pp;
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

	// Get the bounds for this port with respect to some NodeInst.
	public void setConnections(ArcProto [] portArcs)
	{
		this.portArcs = portArcs;
	}

	// Get the bounds for this port with respect to some NodeInst.
	public Poly getPoly(NodeInst ni)
	{
		return tech.getPoly(ni, this);
	}

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
