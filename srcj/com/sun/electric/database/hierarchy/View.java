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
 * A view is simply an object with a name that represents a representation
 * style, e.g. schematic, layout, etc.
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
	/** temporary integer for the view */					private int temp1;
	/** a list of all views in existence */					private static List views = new ArrayList();
	/** a list of views by short and long names */			private static HashMap viewNames = new HashMap();

	// -------------------------- public data -----------------------------

	/** the unknown view */
		public static final View UNKNOWN = newInstance("unknown", "");
	/** the simulation snapshot view */
		public static final View SIMSNAP = newInstance("simulation-snapshot", "sim");
	/** the NetLisp (netlist) view */
		public static final View NETLISTNETLISP = newTextInstance("netlist-netlisp-format", "net-netlisp");
	/** the RSIM (netlist) view */
		public static final View NETLISTRSIM = newTextInstance("netlist-rsim-format", "net-rsim");
	/** the SILOS (netlist) view */
		public static final View NETLISTSILOS = newTextInstance("netlist-silos-format", "net-silos");
	/** the QUISC (netlist) view */
		public static final View NETLISTQUISC = newTextInstance("netlist-quisc-format", "net-quisc");
	/** the ALS (netlist) view */
		public static final View NETLISTALS = newTextInstance("netlist-als-format", "net-als");
	/** the general Netlist view */
		public static final View NETLIST = newTextInstance("netlist", "net");
	/** the VHDL view */
		public static final View VHDL = newTextInstance("VHDL", "vhdl");
	/** the Verilog view */
		public static final View VERILOG = newTextInstance("Verilog", "ver");
	/** the Skeleton view */
		public static final View SKELETON = newInstance("skeleton", "sk");
	/** the Compensated view */
		public static final View COMP = newInstance("compensated", "comp");
	/** the Documentation view */
		public static final View DOC = newTextInstance("documentation", "doc");
	/** the Icon view */
		public static final View ICON = newInstance("icon", "ic");
	/** the Schematic view */
		public static final View SCHEMATIC = newInstance("schematic", "sch");
	/** the Layout view */
		public static final View LAYOUT = newInstance("layout", "lay");

	// -------------------------- private methods -----------------------------

	private View()
	{
	}

	public static View makeInstance(String fullName, String shortName, int type)
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

	public static View newInstance(String fullName, String shortName)
	{
		return makeInstance(fullName, shortName, 0);
	}

	public static View newTextInstance(String fullName, String shortName)
	{
		return makeInstance(fullName, shortName, TEXTVIEW);
	}

	/**
	 * Retrieve a view using its full or short name.  Return null if
	 * no such view.
	 */
	public static View getView(String name)
	{
		return (View) viewNames.get(name);
	}

	/**
	 * get the full name of this view.  This is a complete word, like
	 * schematic
	 */
	public String getFullName() { return fullName; }

	/**
	 * get the short name of this view.  This is the short sequence of
	 * characters you usually see inside the {} in Facet descriptions,
	 * e.g. sch
	 */
	public String getShortName() { return shortName; }

	public void setTemp1(int temp1) { this.temp1 = temp1; }
	public int getTemp1() { return temp1; }

	/** Get the Text-view bit */
	public boolean isTextView() { return (type & TEXTVIEW) != 0; }

	/** Get the Multipage-view bit */
	public boolean isMultiPageView() { return (type & MULTIPAGEVIEW) != 0; }

	/** Get the ermanent-view bit */
	public boolean isPermanentView() { return (type & PERMANENTVIEW) != 0; }

	/** Get an ordered array of the views. */
	public static Iterator getViews()
	{
		return views.iterator();
	}

	public String toString()
	{
		return "View " + fullName;
	}

} // end of class View
