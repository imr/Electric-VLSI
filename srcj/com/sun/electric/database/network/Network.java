/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Network.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
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
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

/** Networks represent connectivity.
 *
 * <p> For a Cell, each Network represents a collection of PortInsts
 * that are electrically connected.
 */
public class Network
{
	// ------------------------- private data ------------------------------
	private Netlist netlist; // Cell that owns this Network
	private int netIndex; // Index of this Network in Netlist.
	//private ArrayList names = new ArrayList(); // Sorted list of names. The
    private ArrayList exportedNames = new ArrayList();
    private ArrayList unexportedNames = new ArrayList();
	// first name is the most
	// appropriate.
	//private int exportedNamesCount;

	// ----------------------- protected and private methods -----------------

	// used for PrimitivePorts
// 	public Network(Collection names, Cell cell)
// 	{
// 		this.parent = cell;
// 		for (Iterator it = names.iterator(); it.hasNext();)
// 		{
// 			addName((String) it.next());
// 		}
// 	}

	/**
	 * Creates Network in a given netlist with specified index.
	 * @param netlist Netlist where Network lives.
	 * @param netIndex index of Network.
	 */
	public Network(Netlist netlist, int netIndex)
	{
		this.netlist = netlist;
		this.netIndex = netIndex;
		//		this(new ArrayList(), cell);
	}

	/**
	 * Add name to list of names of this Network.
	 * @param nm name to add.
	 * @param exported true if name is exported.
	 */
	public void addName(String nm, boolean exported)
	{
		if (nm != null) {
			if (exported) {
                exportedNames.add(nm);
                if (unexportedNames.contains(nm))
                    unexportedNames.remove(nm);
				//if (exportedNamesCount != names.size() - 1)
				//	throw new IllegalStateException("exported names should go first");
				//exportedNamesCount = names.size();
			} else {
                if (!exportedNames.contains(nm))
                    unexportedNames.add(nm);
            }
		}
	}

	// --------------------------- public methods ------------------------------

	/** Returns the Netlist of this Network.
	 * @return Netlist of this Network.
	 */
	public Netlist getNetlist()
	{
		return netlist;
	}

	/** Returns parent cell of this Network.
	 * @return parent cell of this Network.
	 */
	public Cell getParent()
	{
		return netlist.netCell.cell;
	}

	/** Returns index of this Network in netlist. */
	public int getNetIndex() { return netIndex; }

	/** A net can have multiple names. Return alphabetized list of names. */
	public Iterator getNames()
	{
        Collections.sort(exportedNames, TextUtils.STRING_NUMBER_ORDER);
        Collections.sort(unexportedNames, TextUtils.STRING_NUMBER_ORDER);
        ArrayList allNames = new ArrayList(exportedNames);
        allNames.addAll(unexportedNames);
		return allNames.iterator();
	}

	/** A net can have multiple names. Return alphabetized list of names. */
	public Iterator getExportedNames()
	{
        Collections.sort(exportedNames, TextUtils.STRING_NUMBER_ORDER);
		return exportedNames.iterator();
	}

	/** Returns true if Network has names */
	public boolean hasNames()
	{
		return (exportedNames.size() > 0 || unexportedNames.size() > 0);
	}

	/** Returns true if nm is one of Network's names */
	public boolean hasName(String nm)
	{
		return (exportedNames.contains(nm) || unexportedNames.contains(nm));
	}

//	public Network getNetwork()
//	{
//		return this;
//	}

	/** Get iterator over all PortInsts on Network.  Note that the
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

	/** Get iterator over all Exports on Network */
	public Iterator getExports()
	{
		ArrayList exports = new ArrayList();
		for (Iterator it = getParent().getPorts(); it.hasNext();)
		{
			Export e = (Export) it.next();
            int busWidth = netlist.getBusWidth(e);
            for (int i = 0; i < busWidth; i++)
            {
                if (netlist.getNetwork(e, i) == this)
                    exports.add(e);
            }
		}
		return exports.iterator();
	}

	/** Get iterator over all ArcInsts on Network */
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
	 * Method to tell whether this network has any exports on it.
	 * @return true if there are exports on this Network.
	 */
    public boolean isExported() { return (exportedNames.size() > 0); }

	/**
	 * Method to describe this Network as a string.
     * @param withQuotes to wrap description between quotes
	 * @return a String describing this Network.
	 */
	public String describe(boolean withQuotes)
	{
		if (hasNames())
		{
			Iterator it = getNames();
			String name = (String)it.next();
			while (it.hasNext())
				name += "/" + (String)it.next();
            if (withQuotes) name = "'"+name+"'";
			return name;
		}
		/* Unnamed net */
		for (Iterator it = getParent().getArcs(); it.hasNext();)
		{
			ArcInst ai = (ArcInst) it.next();
			if (netlist.getNetwork(ai, 0) == this)
			{
				return (withQuotes)?"'"+ai.getName()+"'":ai.getName();
			}
		}
		return "";
	}

	/**
	 * Returns a printable version of this Network.
	 * @return a printable version of this Network.
	 */
	public String toString()
	{
		return "network "+describe(true);
	}
}
