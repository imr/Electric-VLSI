/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Export.java
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
package com.sun.electric.database.hierarchy;

import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.database.network.JNetwork;

import java.awt.geom.AffineTransform;

/**
 * An Export is a PortProto at the Cell level.  It points to the
 * PortProto that got exported, and the NodeInst for that port.
 * It also allows instant access to the PrimitivePort that grounds
 * out the PortProto chain.
 */
public class Export extends PortProto
{
	// -------------------------- private data ---------------------------
	/** the PortProto that the exported port belongs to */	private PortInst originalPort;
	/** the NodeInst that the exported port belongs to */	private NodeInst originalNode;

	// -------------------- protected and private methods --------------
	protected Export()
	{
		this.userBits = 0;
	}

	/**
	 * Low-level access routine to create a cell in library "lib".
	 */
	public static Export lowLevelAllocate()
	{
		Export pp = new Export();
		return pp;
	}

	/**
	 * Low-level access routine to fill-in the cell name and parent.
	 * Returns true on error.
	 */
	public boolean lowLevelName(Cell parent, String protoName)
	{
		// initialize the parent object
		this.parent = parent;
		this.protoName = protoName;
		setParent(parent);
		return false;
	}

	/**
	 * Low-level access routine to fill-in the subnode and subport.
	 * Returns true on error.
	 */
	public boolean lowLevelPopulate(NodeInst originalNode, PortInst originalPort)
	{
		// initialize this object
		if (originalPort == null)
		{
			System.out.println("Null port on Export " + protoName + " in cell " + parent.describe());
			return true;
		}
		if (originalNode == null)
		{
			System.out.println("Null node on Export " + protoName + " in cell " + parent.describe());
			return true;
		}
		this.originalPort = originalPort;
		this.originalNode = originalNode;
		originalNode.addExport(this);
		
		this.userBits = originalPort.getPortProto().lowLevelGetUserbits();
		return false;
	}

	/** Initialize this Export with a parent (the Cell we're a port of),
	 * the original PortProto we're based on, the NodeInst that
	 * original port belongs to, and an appropriate Network. */
	public static Export newInstance(Cell parent, NodeInst originalNode, PortInst originalPort, String protoName)
	{
		Export pp = lowLevelAllocate();
		if (pp.lowLevelName(parent, protoName)) return null;
		if (pp.lowLevelPopulate(originalNode, originalPort)) return null;
		return pp;
	}	

	public void remove()
	{
		originalNode.removeExport(this);
		super.remove();
	}

	/** Get the PortProto that was exported to create this Export. */
	public PortInst getOriginalPort() { return originalPort; }

	/** Get the NodeInst that the port returned by getOriginal belongs to */
	public NodeInst getOriginalNode() { return originalNode; }

	/** Get the outline of this Export, relative to some arbitrary
	 * instance of this Export's parent, passed in as a Geometric
	 * object.
	 * @param ni the instance of the port's parent
	 * @return the shape of the port, transformed into the coordinates
	 * of the instance's Cell. */
	public Poly getPoly(NodeInst ni)
	{
		// We just figure out where our basis thinks it is, and ask ni to
		// transform it for us.
if (originalPort == null)
{
	System.out.println("Null originalPort on Export " + protoName + " in cell " + parent.describe());
	return null;
}
		Poly poly = originalPort.getPortProto().getPoly(originalNode);
		if (poly == null) return null;
		AffineTransform af = ni.transformOut();
		poly.transform(af);
		return poly;
	}

	protected void getInfo()
	{
		System.out.println(" Original: " + originalPort);
		System.out.println(" Base: " + getBasePort());
		System.out.println(" Cell: " + parent.describe());
		System.out.println(" Instance: " + originalNode.describe());
		super.getInfo();
	}

	// ----------------------- public methods ----------------------------

	/** Get the base PrimitivePort that generated this Export */
	public PrimitivePort getBasePort()
	{
		PortProto pp = originalPort.getPortProto();
		return pp.getBasePort();
	}

	public JNetwork getNetwork()
	{
		return getOriginalPort().getNetwork();
	}

	/** If this PortProto belongs to an Icon View Cell then return the
	 * PortProto with the same name on the corresponding Schematic View
	 * Cell.
	 *
	 * <p> If this PortProto doesn't belong to an Icon View Cell then
	 * return this PortProto.
	 *
	 * <p> If the Icon View Cell has no corresponding Schematic View
	 * Cell then return null. If the corresponding Schematic View Cell
	 * has no port with the same name then return null.
	 *
	 * <p> If there are multiple versions of the Schematic Cell return
	 * the latest. */
	public PortProto getEquivalent()
	{
		NodeProto equiv = parent.getEquivalent();
		if (equiv == parent)
			return this;
		if (equiv == null)
			return null;
		return equiv.findPortProto(protoName);
	}

	public String toString()
	{
		return "Export " + protoName;
	}
}
