/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: View.java
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

import com.sun.electric.database.variable.ElectricObject;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A View is an object that represents a style of design, for example schematic, layout, etc.
 * Each Cell has a View associated with it.
 * Electric has a set of Views at the start, and users can define their own.
 */
public class View extends ElectricObject
{
	// -------------------------- private data -----------------------------

	/** view contains only text */							private final static int TEXTVIEW       = 01;	
	/** view is one of multiple pages  */					private final static int MULTIPAGEVIEW  = 02;	
	/** view is statically defined and cannot be deleted */ private final static int PERMANENTVIEW  = 04;	

	/** the full name of the view */						private String fullName;
	/** the abbreviation of the view */						private String shortName;
	/** flag bits for the view */							private int type;
	/** temporary integer for the view */					private int tempInt;
	/** a list of all views in existence */					private static List views = new ArrayList();
	/** a list of views by short and long names */			private static HashMap viewNames = new HashMap();

	// -------------------------- public data -----------------------------

	/** Defines the unknown view. */
		public static final View UNKNOWN = newInstance("unknown", "");
	/** Defines the simulation snapshot view. */
		public static final View SIMSNAP = newInstance("simulation-snapshot", "sim");
	/** Defines the NetLisp (netlist) view. */
		public static final View NETLISTNETLISP = newTextInstance("netlist-netlisp-format", "net-netlisp");
	/** Defines the RSIM (netlist) view. */
		public static final View NETLISTRSIM = newTextInstance("netlist-rsim-format", "net-rsim");
	/** Defines the SILOS (netlist) view. */
		public static final View NETLISTSILOS = newTextInstance("netlist-silos-format", "net-silos");
	/** Defines the QUISC (netlist) view. */
		public static final View NETLISTQUISC = newTextInstance("netlist-quisc-format", "net-quisc");
	/** Defines the ALS (netlist) view. */
		public static final View NETLISTALS = newTextInstance("netlist-als-format", "net-als");
	/** Defines the general Netlist view. */
		public static final View NETLIST = newTextInstance("netlist", "net");
	/** Defines the VHDL view. */
		public static final View VHDL = newTextInstance("VHDL", "vhdl");
	/** Defines the Verilog view. */
		public static final View VERILOG = newTextInstance("Verilog", "ver");
	/** Defines the Skeleton view. */
		public static final View SKELETON = newInstance("skeleton", "sk");
	/** Defines the Compensated view. */
		public static final View COMP = newInstance("compensated", "comp");
	/** Defines the Documentation view. */
		public static final View DOC = newTextInstance("documentation", "doc");
	/** Defines the Icon view. */
		public static final View ICON = newInstance("icon", "ic");
	/** Defines the Schematic view. */
		public static final View SCHEMATIC = newInstance("schematic", "sch");
	/** Defines the Layout view. */
		public static final View LAYOUT = newInstance("layout", "lay");

	// -------------------------- private methods -----------------------------

	/**
	 * This constructor is never called.  Use the factory "makeInstance" instead.
	 */
	private View()
	{
	}

	private static View makeInstance(String fullName, String shortName, int type)
	{
		// make sure the view doesn't already exist
		if (viewNames.get(shortName) != null)
		{
			System.out.println("multiple views with same name: " + shortName);
			return null;
		}
		if (viewNames.get(fullName) != null)
		{
			System.out.println("multiple views with same name: " + fullName);
			return null;
		}

		if (fullName.toLowerCase().startsWith("schematic-page-"))
			type |= View.MULTIPAGEVIEW;

		// create the view
		View v = new View();
		v.fullName = fullName;
		v.shortName = shortName;
		v.type = type;

		// enter both the full and short names into the hash table
		viewNames.put(fullName, v);
		viewNames.put(shortName, v);
		views.add(v);
		return v;
	}

	// -------------------------- public methods -----------------------------

	/**
	 * Routine to create a View with the given name.
	 * @param fullName the full name of the View, for example "layout".
	 * @param shortName the short name of the View, for example "lay".
	 * The short name is used inside of braces when naming a cell (for example "gate{lay}").
	 * @return the newly created View.
	 */
	public static View newInstance(String fullName, String shortName)
	{
		return makeInstance(fullName, shortName, 0);
	}

	/**
	 * Routine to create a Text-only View with the given name.
	 * Cells with text-only views have no nodes or arcs, just text.
	 * @param fullName the full name of the View, for example "documentation".
	 * @param shortName the short name of the View, for example "doc".
	 * The short name is used inside of braces when naming a cell (for example "gate{doc}").
	 * @return the newly created Text-only View.
	 */
	public static View newTextInstance(String fullName, String shortName)
	{
		return makeInstance(fullName, shortName, TEXTVIEW);
	}

	/**
	 * Routine to return a View using its full or short name.
	 * @param name the name of the View.
	 * @return the named View, or null if no such View exists.
	 */
	public static View getView(String name)
	{
		return (View) viewNames.get(name);
	}

	/**
	 * Routine to return the full name of this View.
	 * @return the full name of this View.
	 */
	public String getFullName() { return fullName; }

	/**
	 * Routine to return the short name of this View.
	 * The short name is used inside of braces when naming a cell (for example "gate{doc}").
	 * @return the short name of this View.
	 */
	public String getShortName() { return shortName; }

	/**
	 * Routine to set an arbitrary integer in a temporary location on this View.
	 * @param tempInt the integer to be set on this View.
	 */
	public void setTempInt(int tempInt) { this.tempInt = tempInt; }

	/**
	 * Routine to get the temporary integer on this View.
	 * @return the temporary integer on this View.
	 */
	public int getTempInt() { return tempInt; }

	/**
	 * Routine to return true if this View is Text-only.
	 * Cells with text-only views have no nodes or arcs, just text.
	 * @return true if this View is Text-only.
	 */
	public boolean isTextView() { return (type & TEXTVIEW) != 0; }

	/**
	 * Routine to return true if this View is Multipage.
	 * Multipage views are those where multiple cells are used to compose a single circuit.
	 * @return true if this View is Text-only.
	 */
	public boolean isMultiPageView() { return (type & MULTIPAGEVIEW) != 0; }

	/**
	 * Routine to return true if this View is permanent.
	 * Permanent views are those that are created initially, and cannot be deleted.
	 * @return true if this View is permanent.
	 */
	public boolean isPermanentView() { return (type & PERMANENTVIEW) != 0; }

	/**
	 * Routine to return an iterator over the views.
	 * @return an iterator over the views.
	 */
	public static Iterator getViews()
	{
		return views.iterator();
	}

	/**
	 * Routine to return the number of views.
	 * @return the number of views.
	 */
	public static int getNumViews()
	{
		return views.size();
	}

	/**
	 * Returns a printable version of this View.
	 * @return a printable version of this View.
	 */
	public String toString()
	{
		return "View " + fullName;
	}

} // end of class View
