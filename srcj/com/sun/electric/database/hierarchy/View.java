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
 * Views have full names (such as "layout") and abbreviations which are used in cell names
 * (for example "gate{lay}").
 * <P>
 * Electric has a set of Views at the start, and users can define their own.
 */
public class View extends ElectricObject
{
	// -------------------------- private data -----------------------------

	/** view contains only text */							private final static int TEXTVIEW       = 01;	
	/** view is one of multiple pages  */					private final static int MULTIPAGEVIEW  = 02;	
	/** view is statically defined and cannot be deleted */ private final static int PERMANENTVIEW  = 04;	

	/** the full name of the view */						private String fullName;
	/** the abbreviation of the view */						private String abbreviation;
	/** ordering for this view */							private int order;
	/** flag bits for the view */							private int type;
	/** temporary integer for the view */					private int tempInt;
	/** a list of all views in existence */					private static List views = new ArrayList();
	/** a list of views by short and long names */			private static HashMap viewNames = new HashMap();

	// -------------------------- public data -----------------------------

	/**
	 * Defines the Icon view.
	 * This is used in schematics to represent instances.
	 * Cells with this view typically use primitives from the Artwork Technology.
	 */
	public static final View ICON = makeInstance("icon", "ic", 0, 0);

	/**
	 * Defines the Schematic view.
	 */
	public static final View SCHEMATIC = makeInstance("schematic", "sch", 0, 1);

	/**
	 * Defines the Skeleton view.
	 * Cells with this view contains only a protection frame or other dummy version of true layout.
	 */
	public static final View SKELETON = makeInstance("skeleton", "sk", 0, 2);

	/**
	 * Defines the Layout view.
	 */
	public static final View LAYOUT = makeInstance("layout", "lay", 0, 3);

	/**
	 * Defines the Compensated view.
	 * Cells with this view contains compensated layout (adjusted for fabrication).
	 */
	public static final View COMP = makeInstance("compensated", "comp", 0, 4);

	/**
	 * Defines the VHDL view (a text view).
	 * Cells with this view contains a textual description in the VHDL hardware-description language.
	 * The text is located in the “FACET_message” variable on the cell.
	 */
	public static final View VHDL = makeInstance("VHDL", "vhdl", TEXTVIEW, 5);

	/**
	 * Defines the Verilog view (a text view).
	 * Cells with this view contains a textual description in the Verilog hardware-description language.
	 * The text is located in the “FACET_message” variable on the cell.
	 */
	public static final View VERILOG = makeInstance("Verilog", "ver", TEXTVIEW, 6);

	/**
	 * Defines the Documentation view (a text view).
	 * Cells with this view contain documentation for other cells in the cell group.
	 * The text is located in the “FACET_message” variable on the cell.
	 */
	public static final View DOC = makeInstance("documentation", "doc", TEXTVIEW, 7);

	/**
	 * Defines the simulation snapshot view.
	 * Cells with this view contain snapshots of the simulation waveform window.
	 */
	public static final View SIMSNAP = makeInstance("simulation-snapshot", "sim", 0, 8);

	/**
	 * Defines the general Netlist view (a text view).
	 * Cells with this view contain an unknown format netlist.
	 * The text is located in the “FACET_message” variable on the cell.
	 */
	public static final View NETLIST = makeInstance("netlist", "net", TEXTVIEW, 9);

	/**
	 * Defines the NetLisp (netlist) view (a text view).
	 * Cells with this view contain a "netlisp" format netlist (for simulation).
	 * The text is located in the “FACET_message” variable on the cell.
	 */
	public static final View NETLISTNETLISP = makeInstance("netlist-netlisp-format", "net-netlisp", TEXTVIEW, 10);

	/**
	 * Defines the RSIM (netlist) view (a text view).
	 * Cells with this view contain an "RSIM" format netlist (for simulation).
	 * The text is located in the “FACET_message” variable on the cell.
	 */
	public static final View NETLISTRSIM = makeInstance("netlist-rsim-format", "net-rsim", TEXTVIEW, 11);

	/**
	 * Defines the SILOS (netlist) view (a text view).
	 * Cells with this view contain a "SILOS" format netlist (for simulation).
	 * The text is located in the “FACET_message” variable on the cell.
	 */
	public static final View NETLISTSILOS = makeInstance("netlist-silos-format", "net-silos", TEXTVIEW, 12);

	/**
	 * Defines the QUISC (netlist) view (a text view).
	 * Cells with this view contain an "QUISC" format netlist (for place-and-route by the QUISC Silicon Compiler).
	 * The text is located in the “FACET_message” variable on the cell.
	 */
	public static final View NETLISTQUISC = makeInstance("netlist-quisc-format", "net-quisc", TEXTVIEW, 13);

	/**
	 * Defines the ALS (netlist) view (a text view).
	 * Cells with this view contain an "ALS" format netlist (for simulation).
	 * The text is located in the “FACET_message” variable on the cell.
	 */
	public static final View NETLISTALS = makeInstance("netlist-als-format", "net-als", TEXTVIEW, 14);

	/**
	 * Defines the unknown view.
	 * This view has an empty abbreviation.
	 */
	public static final View UNKNOWN = makeInstance("unknown", "", 0, 15);

	// -------------------------- private methods -----------------------------

	/**
	 * This constructor is never called.  Use the factory "makeInstance" instead.
	 */
	private View()
	{
		setIndex(views.size());
	}

	private static View makeInstance(String fullName, String abbreviation, int type, int order)
	{
		// make sure the view doesn't already exist
		if (viewNames.get(abbreviation) != null)
		{
			System.out.println("multiple views with same name: " + abbreviation);
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
		v.abbreviation = abbreviation;
		v.type = type;
		v.order = order;

		// enter both the full and short names into the hash table
		viewNames.put(fullName, v);
		viewNames.put(abbreviation, v);
		views.add(v);
		return v;
	}

	// -------------------------- public methods -----------------------------

	private static int overallOrder = 16;

	/**
	 * Routine to create a View with the given name.
	 * @param fullName the full name of the View, for example "layout".
	 * @param abbreviation the short name of the View, for example "lay".
	 * The short name is used inside of braces when naming a cell (for example "gate{lay}").
	 * @return the newly created View.
	 */
	public static View newInstance(String fullName, String abbreviation)
	{
		return makeInstance(fullName, abbreviation, 0, overallOrder++);
	}

	/**
	 * Routine to create a Text-only View with the given name.
	 * Cells with text-only views have no nodes or arcs, just text.
	 * @param fullName the full name of the View, for example "documentation".
	 * @param abbreviation the short name of the View, for example "doc".
	 * The short name is used inside of braces when naming a cell (for example "gate{doc}").
	 * @return the newly created Text-only View.
	 */
	public static View newTextInstance(String fullName, String abbreviation)
	{
		return makeInstance(fullName, abbreviation, TEXTVIEW, overallOrder++);
	}

	/**
	 * Routine to return a View using its full or short name.
	 * @param name the name of the View.
	 * @return the named View, or null if no such View exists.
	 */
	public static View findView(String name)
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
	public String getAbbreviation() { return abbreviation; }

	/**
	 * Routine to get the ordering of this View for sorting.
	 * @return the ordering this View.
	 */
	public int getOrder() { return order; }

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
	public static Iterator getViews() { return views.iterator(); }

	/**
	 * Routine to return the number of views.
	 * @return the number of views.
	 */
	public static int getNumViews() { return views.size(); }

	/**
	 * Returns a printable version of this View.
	 * @return a printable version of this View.
	 */
	public String toString()
	{
		return "View " + fullName;
	}

} // end of class View
