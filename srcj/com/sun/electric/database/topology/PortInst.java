/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PortInst.java
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
package com.sun.electric.database.topology;

import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.ElectricObject;

import java.awt.geom.Rectangle2D;

/**
 * The PortInst class represents an instance of a Port.  It is the
 * combination of a NodeInst and a PortProto.
 */
public class PortInst extends ElectricObject
{
	// ------------------------ private data ------------------------

	private NodeInst nodeInst;
	private PortProto portProto;

	// -------------------protected or private methods ---------------

	private PortInst() {}

	// ------------------------ public methods -------------------------

	/**
	 * Routine to create a PortInst object.
	 * @param portProto the PortProto on the prototype of the NodeInst.
	 * @param nodeInst the NodeInst that owns the port.
	 * @return the newly created PortInst.
	 */
	public static PortInst newInstance(PortProto portProto, NodeInst nodeInst)
	{
		PortInst pi = new PortInst();
		pi.portProto = portProto;
		pi.nodeInst = nodeInst;
		return pi;
	}

	/**
	 * Routine to return the NodeInst that this PortInst resides on.
	 * @return the NodeInst that this PortInst resides on.
	 */
	public NodeInst getNodeInst() { return nodeInst; }

	/**
	 * Routine to return the PortProto that this PortInst is an instance of.
	 * @return the PortProto that this PortInst is an instance of.
	 */
	public PortProto getPortProto() { return portProto; }

	/**
	 * Routine to get the index of this PortInst in NodeInst ports.
	 * @return index of this PortInst in NodeInst ports.
	 */
	public final int getPortIndex() { return portProto.getPortIndex(); }

	/** 
	 ** Routine to return the equivalent PortProto of this PortInst's PortProto.
	 * This is typically used to find the PortProto in the schematic view.
	 * @return the equivalent PortProto of this PortInst's PortProto, or null if not found.
	 */
    public PortProto getProtoEquivalent() 
    {
		return portProto.getEquivalent();
    }
    
	/**
	 * Routine to return the JNetwork connected to this PortInst.
	 * @return the JNetwork connected to this PortInst.
	 */
	public JNetwork getNetwork()
	{
		if (portProto.getProtoNameKey().isBus())
		{
			System.out.println("PortInst.getNetwork() was called for instance of bus port "+portProto.getProtoName());
			return null;
		}
		return nodeInst.getNetwork(portProto, 0);
	}

	/**
	 * Routine to return the bounds of this PortInst.
	 * The bounds are determined by getting the Poly and bounding it.
	 * @return the bounds of this PortInst.
	 */
	public Rectangle2D getBounds()
	{
		Rectangle2D r = nodeInst.getShapeOfPort(portProto).getBounds2D();
		return r;
	}

	/**
	 * Routine to return the Poly that describes this PortInst.
	 * @return the Poly that describes this PortInst.
	 */
	public Poly getPoly()
	{
		return nodeInst.getShapeOfPort(portProto);
	}

	/**
	 * Routine to describe this NodeInst as a string.
	 * @return a description of this NodeInst as a string.
	 */
	public String describe()
	{
		return nodeInst.describe() + "." + portProto.getProtoName();
	}

	/**
	 * Returns a printable version of this PortInst.
	 * @return a printable version of this PortInst.
	 */
	public String toString()
	{
		return "PortInst " + describe();
	}
}
