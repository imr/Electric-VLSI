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
import com.sun.electric.database.hierarchy.Cell;
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
	/** List of NodeInstProxies of protoType in parent */	private List proxies;
	/** Usage of mainSchematics for icons */				private NodeUsage sch;
	/** Number of icon NodeUsages for schematics */			private List icons;

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
		proxies = new ArrayList();
		sch = null;
		if (protoType instanceof Cell && ((Cell)protoType).getView() == View.SCHEMATIC)
		{
			icons = new ArrayList();
		}
	}

	/**
	 * Routine to add an NodeInst to this NodeUsage.
	 * @param ni the NodeInsy to add.
	 */
	void addInst(NodeInst ni)
	{
		insts.add(ni);
	}

	/**
	 * Routine to remove an NodeInst from this NodeUsage.
	 * @param ni the NodeInst to remove.
	 */
	void removeInst(NodeInst ni)
	{
		insts.remove(ni);
	}

	/**
	 * Routine to add an NodeInst to this NodeUsage.
	 * @param nip the NodeInstProxy to add.
	 */
	void addProxy(NodeInstProxy nip)
	{
		proxies.add(nip);
	}

	/**
	 * Routine to remove an NodeInstProxy from this NodeUsage.
	 * @param ni the NodeInstProxy to remove.
	 */
	void removeProxy(NodeInstProxy nip)
	{
		proxies.remove(nip);
	}

	/**
	 * Routine to return the number of icon NodeUsages for this schematics.
	 * Returns zero if protoType is non-schematic.
	 * @return the number of NodeInsts of this NodeUsage.
	 */
	int getNumIcons()
	{
		return icons != null ? icons.size() : 0;
	}

	/**
	 * Routine to return an Iterator for all NodeUsages of icon of this schematics.
	 * @return an Iterator for all icon NodeUsages of this schematic NodeUsage.
	 */
	Iterator getIcons()
	{
		return icons.iterator();
	}

	/**
	 * Routine to increment the number of icon NodeUsages for this schematics.
	 */
	void addIcon(NodeUsage nu)
	{
		icons.add(nu);
		nu.sch = this;
	}

	/**
	 * Routine to decrement the number of icon NodeUsages for this schematics.
	 */
	void removeIcon(NodeUsage nu)
	{
		icons.remove(nu);
		nu.clearSch();
	}

	/**
	 * Routine to clear schematic NodeUsage of this icon NodeUsage
	 */
	void clearSch()
	{
		sch = null;
	}

	/**
	 * Routine to clear schematic NodeUsage of this icon NodeUsage
	 */
	NodeUsage getSch() { return sch; }

	/**
	 * Routine to self-check
	 */
	public int checkAndRepair()
	{
		int error = 0;
		if (protoType instanceof Cell && ((Cell)protoType).getView() == View.ICON)
		{
			Cell mainSch = ((Cell)protoType).getCellGroup().getMainSchematics();
			if (mainSch == null || isIconOfParent())
			{
				if (sch != null)
				{
					System.out.println(this+" is icon without mainSchematics, sch="+sch);
					error++;
				}
			} else
			{
				if (sch == null)
				{
					System.out.println(this+" is icon with mainScjematics "+mainSch+", sch=null");
					error++;
				}
				if (mainSch != sch.protoType)
				{
					System.out.println(this+" is icon with mainSchematics "+mainSch+", sch="+sch);
					error++;
				}
				if (!sch.icons.contains(this))
				{
					System.out.println(this+" is not contained in icons of "+sch);
					error++;
				}
			}
		} else
		{
			if (sch != null)
			{
				System.out.println(this+" is not icon, sch="+sch);
				error++;
			}
		}
		if (protoType instanceof Cell && ((Cell)protoType).getView() == View.SCHEMATIC)
		{
			if (icons == null)
			{
				System.out.println(this+" is schematics, icons == null");
				error++;
			}
			for (int i = 0; i < icons.size(); i++)
			{
				NodeUsage icon = (NodeUsage)icons.get(i);
				if (icon.sch != this) System.out.println(this+" is schematics, contain "+icon);
				for (int j = 0; j < i; j++)
					if (icons.get(j) == icon)
					{
						System.out.println(this+" contains icon "+icon+" twice");
						error++;
					}
			}
		} else
		{
			if (icons != null)
			{
				System.out.println(this+" is not schematics, icons!=null");
				error++;
			}
		}
		return error;
	}

	// ------------------------ public methods -------------------------------

	/**
	 * Routine to return the prototype of this NodeUsage.
	 * @return the prototype of this NodeUsage.
	 */
	public NodeProto getProto() { return protoType; }

	/**
	 * Routine to return the Cell that contains this Geometric object.
	 * @return the Cell that contains this Geometric object.
	 */
	public Cell getParent() { return parent; }

	/**
	 * Routine to return by index a NodeInsts of this NodeIsage.
	 * @param i index
	 * @return specified NodeInst NodeUsage.
	 */
	public final NodeInst getInst(int i)
	{
		return (NodeInst)insts.get(i);
	}

	/**
	 * Routine to return an Iterator for all NodeInsts of this NodeIsage .
	 * @return an Iterator for all NodeInsts of this NodeUsage.
	 */
	public Iterator getInsts()
	{
		return insts.iterator();
	}

	/**
	 * Routine to return the number of NodeInsts of this NodeUsage.
	 * @return the number of NodeInsts of this NodeUsage.
	 */
	public int getNumInsts()
	{
		return insts.size();
	}

	/**
	 * Routine to return by index a NodeInstProxies of this NodeIsage.
	 * @param i index
	 * @return specified NodeInstProxy.
	 */
	public final NodeInstProxy getProxy(int i)
	{
		return (NodeInstProxy)proxies.get(i);
	}

	/**
	 * Routine to return an Iterator for all NodeInstProxies of this NodeIsage .
	 * @return an Iterator for all NodeInstProxies of this NodeUsage.
	 */
	public Iterator getProxies()
	{
		return proxies.iterator();
	}

	/**
	 * Routine to return the number of NodeInsts of this NodeUsage.
	 * @return the number of NodeInsts of this NodeUsage.
	 */
	public int getNumProxies()
	{
		return insts.size();
	}

	/**
	 * Routine to check if NodeUsages contains NodeInst.
	 * @param ni NodeInst to check
	 * @return true if NodeInst is contained
	 */
	public boolean contains(NodeInst ni)
	{
		return insts.contains(ni);
	}

	/**
	 * Routine to return whether this NodeUsage has instances or related icon NodeUsages.
	 * @return true if this NodeUsage has instances or related icon NodeUsages.
	 */
	public boolean isEmpty()
	{
		return insts.size() == 0 && (icons == null || icons.size() == 0);
	}

	/**
	 * Routine to tell whether this NodeUsage is an icon of its parent.
	 * Electric does not allow recursive circuit hierarchies (instances of Cells inside of themselves).
	 * However, it does allow one exception: a schematic may contain its own icon for documentation purposes.
	 * This routine determines whether this NodeInst is such an icon.
	 * @return true if this NodeUsage is an icon of its parent.
	 */
	public boolean isIconOfParent() { return protoType.isIconOf(parent); };

	/**
	 * Routine to determine whether this NodeUsage is an icon Cell.
	 * @return true if this NodeUsage is an icon  Cell.
	 */
	public boolean isIcon() { return protoType.isIcon(); }

	/**
	 * Returns a printable version of this NodeInst.
	 * @return a printable version of this NodeInst.
	 */
	public String toString()
	{
		return "NodeUsage of " + protoType + " in " + parent.describe();
	}

}
