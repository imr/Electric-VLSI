/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JNetwork.java
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
package com.sun.electric.database.network;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;

import java.util.ArrayList;
import java.util.Iterator;

/** Jose uses JNetworks to represent connectivity.
 *
 * <p> For a Cell, each JNetwork represents a collection of PortInsts
 * that are electrically connected.  The JNetworks for a Cell and all
 * its descendents are created when the user calls
 * Cell.rebuildNetworks().
 *
 * <p> A Cell's JNetworks are <i>not</i> kept up to date when Cells
 * are modified.  If you modify Cells and wish to get an updated view
 * of the connectivity you <i>must</i> call Cell.rebuildNetworks() after all
 * modifications are complete.
 *
 * <p> The JNetwork is a pure-java data structure. It does *not*
 * reference a c-side Electric Network object.
 *
 * <p> TODO: I need to generalize this to handle busses. */
public class JNetwork
{
	// ------------------------- private data ------------------------------
	private Netlist netlist; // Cell that owns this JNetwork
	private int netIndex; // Index of this JNetwork in Netlist.
	private ArrayList names = new ArrayList(); // Sorted list of names. The
	// first name is the most
	// appropriate.

	// ----------------------- protected and private methods -----------------

	// used for PrimitivePorts
// 	public JNetwork(Collection names, Cell cell)
// 	{
// 		this.parent = cell;
// 		for (Iterator it = names.iterator(); it.hasNext();)
// 		{
// 			addName((String) it.next());
// 		}
// 	}

	// used to build Cell networks
	public JNetwork(Netlist netlist, int netIndex)
	{
		this.netlist = netlist;
		this.netIndex = netIndex;
		//		this(new ArrayList(), cell);
	}

	public void addName(String nm)
	{
		if (nm != null)
			names.add(nm);
	}

	/** Remove this JNetwork.  Actually, we just let the garbage collector
	 * take care of it. */
// 	void remove()
// 	{
// 	}

	/** Create a JNetwork based on this one, but attached to a new Cell */
// 	JNetwork copy(Cell f)
// 	{
// 		return new JNetwork(names, f);
// 	}

	// --------------------------- public methods ------------------------------
	public Cell getParent()
	{
		return netlist.netCell.cell;
	}

	/** Returns index of this JNetwork in netlist. */
	public int getNetIndex() { return netIndex; }

	/** A net can have multiple names. Return alphabetized list of names. */
	public Iterator getNames()
	{
		return names.iterator();
	}

	/** Returns true if JNetwork has names */
	public boolean hasNames()
	{
		return names.size() > 0;
	}

	/** Returns true if nm is one of JNetwork's names */
	public boolean hasName(String nm)
	{
		return names.contains(nm);
	}

	/** TODO: write getNetwork() in JNetwork */
	public JNetwork getNetwork()
	{
		return this;
	}

	/** Get iterator over all PortInsts on JNetwork.  Note that the
	 * PortFilter class is useful for filtering out frequently excluded
	 * PortInsts.  */
	public Iterator getPorts()
	{
		ArrayList ports = new ArrayList();
		for (Iterator it = getParent().getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			for (Iterator pit = ni.getPortInsts(); pit.hasNext(); )
			{
				PortInst pi = (PortInst)pit.next();
				if (netlist.getNetwork(pi) == this)
					ports.add(pi);
			}
		}
		return ports.iterator();
	}

	/** Get iterator over all Exports on JNetwork */
	public Iterator getExports()
	{
		ArrayList exports = new ArrayList();
		for (Iterator it = getParent().getPorts(); it.hasNext();)
		{
			Export e = (Export) it.next();
			if (netlist.getNetwork(e, 0) == this)
				exports.add(e);
		}
		return exports.iterator();
	}

	/** Get iterator over all ArcInsts on JNetwork */
	public Iterator getArcs()
	{
		ArrayList arcs = new ArrayList();
		for (Iterator it = getParent().getArcs(); it.hasNext();)
		{
			ArcInst ai = (ArcInst) it.next();
			if (netlist.getNetwork(ai, 0) == this)
			{
				arcs.add(ai);
			}
		}
		return arcs.iterator();
	}

	/**
	 * Method to describe this JNetwork as a string.
	 * @return a String describing this JNetwork.
	 */
	public String describe()
	{
		if (names.size() > 0)
		{
			Iterator it = names.iterator();
			String name = (String)it.next();
			while (it.hasNext())
				name += "/" + (String)it.next();
			return name;
		}
		/* Unnamed net */
		for (Iterator it = getParent().getArcs(); it.hasNext();)
		{
			ArcInst ai = (ArcInst) it.next();
			if (netlist.getNetwork(ai, 0) == this)
			{
				return ai.getName();
			}
		}
		return "";
	}

	/**
	 * Returns a printable version of this JNetwork.
	 * @return a printable version of this JNetwork.
	 */
	public String toString()
	{
		return "Network "+describe();
	}
}
