/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NodeUsage.java
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
import com.sun.electric.database.topology.NodeInst;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A NodeUsage is a usage of a NodeProto (a PrimitiveNode or a Cell) in
 * some Cell.  A NodeUsage points to its prototype and the Cell
 * in which it has been used. NodeUsage implies that there is one or more
 * instance of this NodeProto in the Cell. It lists all such NodeInsts.
 */
public class NodeUsage
{
	// ---------------------- private data ----------------------------------
	/** prototype of this node usage */						private NodeProto protoType;
	/** Cell using this prototype */						private Cell parent;
	/** List of NodeInsts of protoType in parent */			private List insts;

	// --------------------- private and protected methods ---------------------

	/**
	 * The constructor.
	 */
	NodeUsage(NodeProto protoType, Cell parent)
	{
		// initialize this object
		this.protoType = protoType;
		this.parent = parent;
		insts = new ArrayList();
	}

	/**
	 * Method to add an NodeInst to this NodeUsage.
	 * @param ni the NodeInsy to add.
	 */
	void addInst(NodeInst ni)
	{
		insts.add(ni);
	}

	/**
	 * Method to remove an NodeInst from this NodeUsage.
	 * @param ni the NodeInst to remove.
	 */
	void removeInst(NodeInst ni)
	{
		insts.remove(ni);
	}

	/**
	 * Method to self-check
	 */
	public int checkAndRepair()
	{
		int error = 0;
		for (int i = 0; i < insts.size(); i++)
		{
			NodeInst ni = (NodeInst)insts.get(i);
			if (ni.getNodeUsage() != this || ni.getProto() != protoType || ni.getParent() != parent)
			{
				System.out.println("Error in NodeUsage.chechAndRepair");
				error++;
			}
		}
		return error;
	}

	// ------------------------ public methods -------------------------------

	/**
	 * Method to return the prototype of this NodeUsage.
	 * @return the prototype of this NodeUsage.
	 */
	public NodeProto getProto() { return protoType; }

	/**
	 * Method to return the Cell that contains this Geometric object.
	 * @return the Cell that contains this Geometric object.
	 */
	public Cell getParent() { return parent; }

	/**
	 * Method to return by index a NodeInsts of this NodeIsage.
	 * @param i index
	 * @return specified NodeInst NodeUsage.
	 */
	public final NodeInst getInst(int i)
	{
		return (NodeInst)insts.get(i);
	}

	/**
	 * Method to return an Iterator for all NodeInsts of this NodeIsage .
	 * @return an Iterator for all NodeInsts of this NodeUsage.
	 */
	public Iterator getInsts()
	{
		return insts.iterator();
	}

	/**
	 * Method to return the number of NodeInsts of this NodeUsage.
	 * @return the number of NodeInsts of this NodeUsage.
	 */
	public int getNumInsts()
	{
		return insts.size();
	}

	/**
	 * Method to check if NodeUsages contains NodeInst.
	 * @param ni NodeInst to check
	 * @return true if NodeInst is contained
	 */
	public boolean contains(NodeInst ni)
	{
		return insts.contains(ni);
	}

	/**
	 * Method to return whether this NodeUsage has instances or related icon NodeUsages.
	 * @return true if this NodeUsage has instances or related icon NodeUsages.
	 */
	public boolean isEmpty()
	{
		return insts.size() == 0;
	}

	/**
	 * Method to tell whether this NodeUsage is an icon of its parent.
	 * Electric does not allow recursive circuit hierarchies (instances of Cells inside of themselves).
	 * However, it does allow one exception: a schematic may contain its own icon for documentation purposes.
	 * This method determines whether this NodeInst is such an icon.
	 * @return true if this NodeUsage is an icon of its parent.
	 */
	public boolean isIconOfParent() { return protoType.isIconOf(parent); };

	/**
	 * Method to determine whether this NodeUsage is an icon Cell.
	 * @return true if this NodeUsage is an icon  Cell.
	 */
	public boolean isIcon() { return protoType.isIcon(); }

	/**
	 * Returns a printable version of this NodeUsage.
	 * @return a printable version of this NodeUsage.
	 */
	public String toString()
	{
		return "NodeUsage of " + protoType + " in " + parent.describe();
	}

}
