/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PrimitiveArc.java
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

import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;

import java.util.Iterator;

/**
 * The PrimitiveArc class defines a type of ArcInst.
 * It is an implementation of the ArcProto class.
 * <P>
 * Every arc in the database appears as one <I>prototypical</I> object and many <I>instantiative</I> objects.
 * Thus, for a PrimitiveArc such as the CMOS Metal-1 there is one object (called a PrimitiveArc, which is a ArcProto)
 * that describes the wire prototype and there are many objects (called ArcInsts),
 * one for every instance of a Metal-1 wire that appears in a circuit.
 * PrimitiveArcs are statically created and placed in the Technology objects.
 * <P>
 * The basic PrimitiveArc has Layers that describe it graphically.
 */
public class PrimitiveArc extends ArcProto
{
	// ----------------------- private data -------------------------------

	/** Layers in this arc */							private Technology.ArcLayer [] layers;
	/** counter for enumerating primitive arcs */		private static int primArcNumber = 0;

	// ----------------- protected and private methods -------------------------

	/**
	 * The constructor is never called.  Use the factory "newInstance" instead.
	 */
	private PrimitiveArc(Technology tech, String protoName, double defaultWidth, Technology.ArcLayer [] layers)
	{
		this.protoName = protoName;
		this.defaultWidth = defaultWidth;
		this.widthOffset = 0;
		this.tech = tech;
		this.layers = layers;
		setIndex(primArcNumber++);
	}

	// ------------------------ public methods -------------------------------

	/**
	 * Routine to create a new PrimitiveArc from the parameters.
	 * @param tech the Technology in which to place this PrimitiveArc.
	 * @param protoName the name of this PrimitiveArc.
	 * It may not have unprintable characters, spaces, or tabs in it.
	 * @param defaultWidth the default width of this PrimitiveArc.
	 * @param layers the Layers that make up this PrimitiveArc.
	 * @return the newly created PrimitiveArc.
	 */
	public static PrimitiveArc newInstance(Technology tech, String protoName, double defaultWidth, Technology.ArcLayer [] layers)
	{
		// check the arguments
		if (tech.findArcProto(protoName) != null)
		{
			System.out.println("Error: technology " + tech.getTechName() + " has multiple arcs named " + protoName);
			return null;
		}
		if (defaultWidth < 0.0)
		{
			System.out.println("PrimitiveArc " + tech.getTechName() + ":" + protoName + " has negative width");
			return null;
		}

		PrimitiveArc ap = new PrimitiveArc(tech, protoName, defaultWidth, layers);
		tech.addArcProto(ap);
		return ap;
	}

	/**
	 * Routine to return the array of layers that comprise this PrimitiveArc.
	 * @return the array of layers that comprise this PrimitiveArc.
	 */
	public Technology.ArcLayer [] getLayers() { return layers; }

	/**
	 * Routine to find the PrimitiveNode pin corresponding to this PrimitiveArc type.
	 * For example, if this PrimitiveArc is metal-1 then return the Metal-1-pin.
	 * @return the PrimitiveNode pin that matches, or null if there is no match.
	 */
	public PrimitiveNode findPinProto()
	{
		Iterator it = tech.getNodes();
		while (it.hasNext())
		{
			PrimitiveNode pn = (PrimitiveNode) it.next();
			if (pn.isPin())
			{
				PrimitivePort pp = (PrimitivePort) pn.getPorts().next();
				if (pp.connectsTo(this)) return pn;
			}
		}
		return null;
	}

	/**
	 * Returns a printable version of this PrimitiveArc.
	 * @return a printable version of this PrimitiveArc.
	 */
	public String toString()
	{
		return "PrimitiveArc " + describe();
	}

}
