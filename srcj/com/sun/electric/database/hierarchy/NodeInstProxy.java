/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NodeInstProxy.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;

import java.util.Arrays;
import java.util.Iterator;

/**
 * A NodeInstProxy is a class which represents virtual instance of schematic Cell
 * induced by one or more icon NodeInsts.
 */
public class NodeInstProxy implements Nodable
{
	// ---------------------- private data ----------------------------------
	/** node usage */										private NodeUsage nodeUsage;
	/** Icon subinstances NodeInst.Subinst */				private NodeInst.Subinst[] subinsts;
	/** Index of this NodeInstProxy in a cell */			private int index;

	// --------------------- private and protected methods ---------------------

	/**
	 * The constructor.
	 */
	NodeInstProxy(NodeUsage nodeUsage)
	{
		// initialize this object
		this.nodeUsage = nodeUsage;
		index = -1;
	}

	/**
	 * Routine to add an NodeInst to this NodeUsage.
	 * @param ni the NodeInsy to add.
	 */
	void addSubinst(NodeInst.Subinst nsi)
	{
		int length = subinsts != null ? subinsts.length : 0;
		NodeInst.Subinst[] newSubinsts = new NodeInst.Subinst[length + 1];
		for (int i = 0; i < length; i++)
			newSubinsts[i] = subinsts[i];
		newSubinsts[length] = nsi;
		subinsts = newSubinsts;
	}

	/**
	 * Routine to remove an NodeInst from this NodeUsage.
	 * @param ni the NodeInst to remove.
	 */
	void removeSubinst(NodeInst.Subinst nsi)
	{
		if (subinsts == null) return;
		if (subinsts.length == 1)
		{
			if (subinsts[0] == nsi) subinsts = null;
			return;
		}
		NodeInst.Subinst[] newSubinsts = new NodeInst.Subinst[subinsts.length - 1];
		int i = 0;
		for (; i < newSubinsts.length && subinsts[i] != nsi; i++)
			newSubinsts[i] = subinsts[i];
		for (; i < newSubinsts.length; i++)
			newSubinsts[i] = subinsts[i+1];
		subinsts = newSubinsts;
	}

	/**
	 * Routine to set an index of this NodeInstProxy in a cell.
	 * @param index an index of this NodeInstProxy in a cell.
	 */
	public void setIndex(int index) { this.index = index; }

	/**
	 * Routine to get the index of this NodeInstProxy in a cell.
	 * @return index of this NodeInstProxy in a cell.
	 */
	public int getIndex() { return index; }

	// ------------------------ public methods -------------------------------

	/**
	 * Returns the NodeUsage of this NodeInstProxy.
	 * @return the NodeUsage of this NideInstProxy.
	 */
	public NodeUsage getNodeUsage() { return nodeUsage; }

	/**
	 * Routine to return the prototype of this NodeInstProxy.
	 * @return the prototype of this NodeInstProxy.
	 */
	public NodeProto getProto() { return nodeUsage.getProto(); }

	/**
	 * Routine to return the Cell that contains this NodeInstProxy.
	 * @return the Cell that contains this NodeInstProxy.
	 */
	public Cell getParent() { return nodeUsage.getParent(); }

	/**
	 * Routine to return by index an icon subinstance generated this NodeInstProxy.
	 * @param i index
	 * @return specified icon subinstance for this NodeInstProxy.
	 */
	public final NodeInst.Subinst getSubinst(int i)
	{
		return subinsts[i];
	}

	/**
	 * Routine to return an Iterator for all icon subinstances which generated this NodeInstProxy.
	 * @return an Iterator for all icon instance of this NodeInstProxy.
	 */
	public Iterator getSubinsts() { return Arrays.asList(subinsts).iterator(); }

	/**
	 * Routine to return the number of icon subinstances which generated this NodeInstProxy.
	 * @return the number of icon subinstances which generated this NodeInstProxy.
	 */
	public int getNumSubinsts()
	{
		return subinsts.length;
	}

	/**
	 * Routine to return the name of this NodeInstProxy.
	 * @return the name of this NodeInstProxy.
	 */
	public String getName() { return subinsts[0].getName().toString(); }

	/**
	 * Routine to return the Name object of this NodeInstProxy.
	 * @return the name of this NodeInstProxy.
	 */
	public Name getNameLow() { return subinsts[0].getName(); }

	/**
	 * Routine to return the Variable on this Nodable with a given name.
	 * @param name the name of the Variable.
	 * @return the Variable with that name, or null if there is no such Variable.
	 */
	public Variable getVar(String name) { return subinsts[0].getInst().getVar(name); }

	/**
	 * Routine to get network by PortProto and bus index.
	 * @param portProto PortProto in protoType.
	 * @param busIndex index in bus.
	 */
	public JNetwork getNetwork(PortProto portProto, int busIndex)
	{
		if (getIndex() < 0) return null; // Nonelectric node
		if (portProto.getParent() != nodeUsage.getProto())
		{
			System.out.println("Invalid portProto argument in NodeInstProxy.getNetwork");
			return null;
		}
		if (busIndex < 0 || busIndex >= portProto.getProtoNameLow().busWidth())
		{
			System.out.println("NodeInstProxy.getNetwork: invalid arguments busIndex="+busIndex+" portProto="+portProto);
			return null;
		}
		return nodeUsage.getParent().getNetwork(getIndex() + portProto.getIndex() + busIndex);
	}

	/**
	 * Returns a printable version of this NodeInstProxy.
	 * @return a printable version of this NodeInstProxy.
	 */
	public String toString()
	{
		return "NodeInstProxy " + getName();
	}

}
