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

import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

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
	private Cell parent; // Cell that owns this JNetwork
	private TreeSet names = new TreeSet(); // Sorted list of names. The
	// first name is the most
	// appropriate.

	// ----------------------- protected and private methods -----------------

	// used for PrimitivePorts
	public JNetwork(Collection names, Cell cell)
	{
		this.parent = cell;
		for (Iterator it = names.iterator(); it.hasNext();)
		{
			addName((String) it.next());
		}
	}

	// used to build Cell networks
	public JNetwork(Cell cell)
	{
		this(new ArrayList(), cell);
	}

	public void addName(String nm)
	{
		if (nm != null)
			names.add(nm);
	}

	/** Remove this JNetwork.  Actually, we just let the garbage collector
	 * take care of it. */
	void remove()
	{
	}

	/** Create a JNetwork based on this one, but attached to a new Cell */
	JNetwork copy(Cell f)
	{
		return new JNetwork(names, f);
	}

	// --------------------------- public methods ------------------------------
	public NodeProto getParent()
	{
		return parent;
	}

	/** A net can have multiple names. Return alphabetized list of names. */
	public Iterator getNames()
	{
		return names.iterator();
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
		for (Iterator it = parent.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			for (Iterator pit = ni.getPortInsts(); pit.hasNext(); )
			{
				PortInst pi = (PortInst)pit.next();
				if (pi.getNetwork() == this)
					ports.add(pi);
			}
		}
		return ports.iterator();
	}

	/** Get iterator over all Exports on JNetwork */
	public Iterator getExports()
	{
		ArrayList exports = new ArrayList();
		for (Iterator it = parent.getPorts(); it.hasNext();)
		{
			Export e = (Export) it.next();
			if (e.getNetwork() == this)
				exports.add(e);
		}
		return exports.iterator();
	}

	/** Get iterator over all ArcInsts on JNetwork */
	public Iterator getArcs()
	{
		ArrayList arcs = new ArrayList();
		for (Iterator it = ((Cell) parent).getArcs(); it.hasNext();)
		{
			ArcInst ai = (ArcInst) it.next();
			if (ai.getHead().getPortInst().getNetwork() == this
				|| ai.getTail().getPortInst().getNetwork() == this)
			{
				arcs.add(ai);
			}
		}
		return arcs.iterator();
	}

	// options
	private static Preferences prefs = null;

	public static boolean isUnifyPowerAndGround()
	{
		if (prefs == null) prefs = Preferences.userNodeForPackage(com.sun.electric.database.network.JNetwork.class);
		return prefs.getBoolean("NetUnifyPowerAndGround", false);
	}
	public static void setUnifyPowerAndGround(boolean v)
	{
		if (prefs == null) prefs = Preferences.userNodeForPackage(com.sun.electric.database.network.JNetwork.class);
		prefs.putBoolean("NetUnifyPowerAndGround", v);
	}

	public static boolean isUnifyLikeNamedNets()
	{
		if (prefs == null) prefs = Preferences.userNodeForPackage(com.sun.electric.database.network.JNetwork.class);
		return prefs.getBoolean("NetUnifyLikeNamedNets", false);
	}
	public static void setUnifyLikeNamedNets(boolean v)
	{
		if (prefs == null) prefs = Preferences.userNodeForPackage(com.sun.electric.database.network.JNetwork.class);
		prefs.putBoolean("NetUnifyLikeNamedNets", v);
	}

	public static boolean isIgnoreResistors()
	{
		if (prefs == null) prefs = Preferences.userNodeForPackage(com.sun.electric.database.network.JNetwork.class);
		return prefs.getBoolean("NetIgnoreResistors", false);
	}
	public static void setIgnoreResistors(boolean v)
	{
		if (prefs == null) prefs = Preferences.userNodeForPackage(com.sun.electric.database.network.JNetwork.class);
		prefs.putBoolean("NetIgnoreResistors", v);
	}

	public static String getUnificationPrefix()
	{
		if (prefs == null) prefs = Preferences.userNodeForPackage(com.sun.electric.database.network.JNetwork.class);
		return prefs.get("NetUnificationPrefix", "");
	}
	public static void setUnificationPrefix(String v)
	{
		if (prefs == null) prefs = Preferences.userNodeForPackage(com.sun.electric.database.network.JNetwork.class);
		prefs.put("NetUnificationPrefix", v);
	}

	public static boolean isBusBaseZero()
	{
		if (prefs == null) prefs = Preferences.userNodeForPackage(com.sun.electric.database.network.JNetwork.class);
		return prefs.getBoolean("NetBusBaseZero", true);
	}
	public static void setBusBaseZero(boolean v)
	{
		if (prefs == null) prefs = Preferences.userNodeForPackage(com.sun.electric.database.network.JNetwork.class);
		prefs.putBoolean("NetBusBaseZero", v);
	}

	public static boolean isBusAscending()
	{
		if (prefs == null) prefs = Preferences.userNodeForPackage(com.sun.electric.database.network.JNetwork.class);
		return prefs.getBoolean("NetBusAscending", false);
	}
	public static void setBusAscending(boolean v)
	{
		if (prefs == null) prefs = Preferences.userNodeForPackage(com.sun.electric.database.network.JNetwork.class);
		prefs.putBoolean("NetBusAscending", v);
	}
}
