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

import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.database.network.JNetwork;

import java.awt.geom.AffineTransform;

/**
 * An Export is a PortProto at the Cell level.  It points to the
 * PortInst that got exported, which identifies a NodeInst and a PortProto on that NodeInst.
 * <P>
 * An Export takes a PortInst on a NodeInst and makes it available as a PortInst
 * on instances of this NodeInst, farther up the hierarchy.
 * An Export therefore belongs to the NodeInst that is its source and also to the Cell
 * that the NodeInst belongs to.
 * The data structures look like this:
 * <P>
 * <CENTER><IMG SRC="doc-files/Export-1.gif"></CENTER>
 */
public class Export extends PortProto
{
	// -------------------------- private data ---------------------------
	/** the PortInst that the exported port belongs to */	private PortInst originalPort;

	// -------------------- protected and private methods --------------

	/**
	 * The constructor is only called by subclassed constructors.
	 */
	protected Export()
	{
		super();
	}

	/**
	 * Low-level access routine to create an Export.
	 * @return a newly allocated Export.
	 */
	public static Export lowLevelAllocate()
	{
		Export pp = new Export();
		return pp;
	}

	/**
	 * Low-level access routine to fill-in the Cell parent and the name of this Export.
	 * @param parent the Cell in which this Export resides.
	 * @param protoName the name of this Export.
	 * It may not have unprintable characters, spaces, or tabs in it.
	 * @return true on error.
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
	 * Low-level access routine to fill-in the subnode and subport of this Export.
	 * The Export is also linked into the Cell.
	 * @param originalNode the node inside of the Export's Cell from which this Export originated.
	 * @param originalPort the port on that node.
	 * @return true on error.
	 */
	public boolean lowLevelPopulate(PortInst originalPort)
	{
		// initialize this object
		if (originalPort == null)
		{
			System.out.println("Null port on Export " + protoName + " in cell " + parent.describe());
			return true;
		}
		this.originalPort = originalPort;
		NodeInst originalNode = originalPort.getNodeInst();
		originalNode.addExport(this);
		
		this.userBits = originalPort.getPortProto().lowLevelGetUserbits();
		return false;
	}

	/**
	 * Routine to create an Export with the specified values.
	 * @param parent the Cell in which this Export resides.
	 * @param originalNode the node inside of the Export's Cell from which this Export originated.
	 * @param originalPort the port on that node.
	 * @param protoName the name of this Export.
	 * It may not have unprintable characters, spaces, or tabs in it.
	 * @return the newly created Export.
	 */
	public static Export newInstance(Cell parent, PortInst originalPort, String protoName)
	{
		Export pp = lowLevelAllocate();
		if (pp.lowLevelName(parent, protoName)) return null;
		if (pp.lowLevelPopulate(originalPort)) return null;
		return pp;
	}	

	/**
	 * Routine to unlink this Export from its Cell.
	 */
	public void remove()
	{
		NodeInst originalNode = originalPort.getNodeInst();
		originalNode.removeExport(this);
		super.remove();
	}

	/**
	 * Routine to return the port on the NodeInst inside of the cell that is the origin of this Export.
	 * @return the port on the NodeInst inside of the cell that is the origin of this Export.
	 */
	public PortInst getOriginalPort() { return originalPort; }

	/**
	 * Routine to return the NodeInst inside of the cell that is the origin of this Export.
	 * @return the NodeInst inside of the cell that is the origin of this Export.
	 */
//	public NodeInst getOriginalNode() { return originalNode; }

	/**
	 * Routine to return a Poly that describes the shape of this Export on a particular NodeInst.
	 * @param ni the instance of the Export's parent.
	 * @return the shape of the port.
	 */
	public Poly getPoly(NodeInst ni)
	{
		// We just figure out where our basis thinks it is, and ask ni to
		// transform it for us.
if (originalPort == null)
{
	System.out.println("Null originalPort on Export " + protoName + " in cell " + parent.describe());
	return null;
}
		NodeInst originalNode = originalPort.getNodeInst();
		Poly poly = originalPort.getPortProto().getPoly(originalNode);
		if (poly == null) return null;
		AffineTransform af = ni.transformOut();
		poly.transform(af);
		return poly;
	}

	/*
	 * Routine to write a description of this Export.
	 * Displays the description in the Messages Window.
	 */
	protected void getInfo()
	{
		System.out.println(" Original: " + originalPort);
		System.out.println(" Base: " + getBasePort());
		System.out.println(" Cell: " + parent.describe());
		super.getInfo();
	}

	// ----------------------- public methods ----------------------------

	/**
	 * Routine to return the base-level port that this PortProto is created from.
	 * Since this is an Export, it returns the base port of its sub-port, the port on the NodeInst
	 * from which the Export was created.
	 * @return the base-level port that this PortProto is created from.
	 */
	public PrimitivePort getBasePort()
	{
		PortProto pp = originalPort.getPortProto();
		return pp.getBasePort();
	}

	/**
	 * Routine to return the Network object associated with this Export.
	 * @return the Network object associated with this Export.
	 */
	public JNetwork getNetwork()
	{
		return getOriginalPort().getNetwork();
	}

	/**
	 * Routine to return the PortProto that is equivalent to this in the
	 * corresponding Cell.
	 * It finds the PortProto with the same name on the corresponding Cell.
	 * If there are multiple versions of the Schematic Cell return the latest.
	 * @return the PortProto that is equivalent to this in the corresponding Cell.
	 */
	public PortProto getEquivalent()
	{
		Cell equiv = ((Cell)parent).getEquivalent();
		if (equiv == parent)
			return this;
		if (equiv == null)
			return null;
		return equiv.findPortProto(protoName);
	}

	/**
	 * Returns a printable version of this Export.
	 * @return a printable version of this Export.
	 */
	public String toString()
	{
		return "Export " + protoName;
	}
}
