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

import com.sun.electric.database.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * A View is an object that represents a style of design, for example schematic, layout, etc.
 * Each Cell has a View associated with it.
 * Views have full names (such as "layout") and abbreviations which are used in cell names
 * (for example "gate{lay}").
 * <P>
 * Electric has a set of Views at the start, and users can define their own.
 */
public class View implements Comparable<View>
{
	// -------------------------- private data -----------------------------

	/** view contains only text */							private final static int TEXTVIEW       = 01;	
	/** view is statically defined and cannot be deleted */ private final static int PERMANENTVIEW  = 04;	

	/** the full name of the view */						private final String fullName;
	/** the abbreviation of the view */						private final String abbreviation;
	/** ordering for this view */							private final int order;
	/** flag bits for the view */							private final int type;

	/** a set of all views in existence */					private static TreeSet<View> views = new TreeSet<View>();
	/** unmodifiable set of all views in existence */		private static Set<View> unmodifiableViews = Collections.unmodifiableSet(views);
	/** a list of views by short and long names */			private static HashMap<String,View> viewNames = new HashMap<String,View>();
	/** the index for new Views. */							private static int overallOrder = 16;

	// -------------------------- public data -----------------------------

	/**
	 * Defines the Schematic view.
	 */
	public static final View SCHEMATIC = makeInstance("schematic", "sch", PERMANENTVIEW, 0);

	/**
	 * Defines the Icon view.
	 * This is used in schematics to represent instances.
	 * Cells with this view typically use primitives from the Artwork Technology.
	 */
	public static final View ICON = makeInstance("icon", "ic", PERMANENTVIEW, 1);

	/**
	 * Defines the Layout view.
	 */
	public static final View LAYOUT = makeInstance("layout", "lay", PERMANENTVIEW, 2);

	/**
	 * Defines the Skeleton view.
	 * Cells with this view contains only a protection frame or other dummy version of true layout.
	 */
	public static final View LAYOUTSKEL = makeInstance("layout.skeleton", "lay.sk", PERMANENTVIEW, 3);

	/**
	 * Defines the Compensated view.
	 * Cells with this view contains compensated layout (adjusted for fabrication).
	 */
	public static final View LAYOUTCOMP = makeInstance("layout.compensated", "lay.comp", PERMANENTVIEW, 4);

	/**
	 * Defines the VHDL view (a text view).
	 * Cells with this view contains a textual description in the VHDL hardware-description language.
	 * The text is located in the "FACET_message" variable on the cell.
	 */
	public static final View VHDL = makeInstance("VHDL", "vhdl", PERMANENTVIEW|TEXTVIEW, 5);

	/**
	 * Defines the Verilog view (a text view).
	 * Cells with this view contains a textual description in the Verilog hardware-description language.
	 * The text is located in the "FACET_message" variable on the cell.
	 */
	public static final View VERILOG = makeInstance("Verilog", "ver", PERMANENTVIEW|TEXTVIEW, 6);

	/**
	 * Defines the Documentation view (a text view).
	 * Cells with this view contain documentation for other cells in the cell group.
	 * The text is located in the "FACET_message" variable on the cell.
	 */
	public static final View DOC = makeInstance("documentation", "doc", PERMANENTVIEW|TEXTVIEW, 7);

	/**
	 * Defines the simulation snapshot view.
	 * Cells with this view contain snapshots of the simulation waveform window.
	 */
	public static final View DOCWAVE = makeInstance("documentation.waveform", "doc.wave", PERMANENTVIEW, 8);

	/**
	 * Defines the general Netlist view (a text view).
	 * Cells with this view contain an unknown format netlist.
	 * The text is located in the "FACET_message" variable on the cell.
	 */
	public static final View NETLIST = makeInstance("netlist", "net", PERMANENTVIEW|TEXTVIEW, 9);

	/**
	 * Defines the NetLisp (netlist) view (a text view).
	 * Cells with this view contain a "netlisp" format netlist (for simulation).
	 * The text is located in the "FACET_message" variable on the cell.
	 */
	public static final View NETLISTNETLISP = makeInstance("netlist.netlisp", "net.netlisp", PERMANENTVIEW|TEXTVIEW, 10);

	/**
	 * Defines the RSIM (netlist) view (a text view).
	 * Cells with this view contain an "RSIM" format netlist (for simulation).
	 * The text is located in the "FACET_message" variable on the cell.
	 */
	public static final View NETLISTRSIM = makeInstance("netlist.rsim", "net.rsim", PERMANENTVIEW|TEXTVIEW, 11);

	/**
	 * Defines the SILOS (netlist) view (a text view).
	 * Cells with this view contain a "SILOS" format netlist (for simulation).
	 * The text is located in the "FACET_message" variable on the cell.
	 */
	public static final View NETLISTSILOS = makeInstance("netlist.silos", "net.silos", PERMANENTVIEW|TEXTVIEW, 12);

	/**
	 * Defines the QUISC (netlist) view (a text view).
	 * Cells with this view contain an "QUISC" format netlist (for place-and-route by the QUISC Silicon Compiler).
	 * The text is located in the "FACET_message" variable on the cell.
	 */
	public static final View NETLISTQUISC = makeInstance("netlist.quisc", "net.quisc", PERMANENTVIEW|TEXTVIEW, 13);

	/**
	 * Defines the ALS (netlist) view (a text view).
	 * Cells with this view contain an "ALS" format netlist (for simulation).
	 * The text is located in the "FACET_message" variable on the cell.
	 */
	public static final View NETLISTALS = makeInstance("netlist.als", "net.als", PERMANENTVIEW|TEXTVIEW, 14);

	/**
	 * Defines the unknown view.
	 * This view has an empty abbreviation.
	 */
	public static final View UNKNOWN = makeInstance("unknown", "", PERMANENTVIEW, 15);

	/****************************** CREATE, DELETE ******************************/

	/**
	 * Method to create a View with the given name.
	 * @param fullName the full name of the View, for example "layout".
	 * @param abbreviation the short name of the View, for example "lay".
	 * The short name is used inside of braces when naming a cell (for example "gate{lay}").
	 * @return the newly created View.
	 */
	public static View newInstance(String fullName, String abbreviation)
	{
		// make sure this can be done now
		EDatabase.theDatabase.checkChanging();

		View view = makeInstance(fullName, abbreviation, 0, getNextOrder());

		// handle change control, constraint, and broadcast
//		Undo.newObject(view);
		return view;
	}

	/**
	 * Method to create a Text-only View with the given name.
	 * Cells with text-only views have no nodes or arcs, just text.
	 * @param fullName the full name of the View, for example "documentation".
	 * @param abbreviation the short name of the View, for example "doc".
	 * The short name is used inside of braces when naming a cell (for example "gate{doc}").
	 * @return the newly created Text-only View.
	 */
	public static View newTextInstance(String fullName, String abbreviation)
	{
		// make sure this can be done now
		EDatabase.theDatabase.checkChanging();

		View view = makeInstance(fullName, abbreviation, TEXTVIEW, getNextOrder());

		// handle change control, constraint, and broadcast
//		Undo.newObject(view);
		return view;
	}

	/**
	 * Method to delete this View.
	 */
	public void kill()
	{
		System.out.println("Cannot delete views");
// 		// cannot delete the permanent views (created at initialization)
// 		if (isPermanentView()) return;
// 
// 		// cannot delete views that are in use
// 		for(Iterator it = Library.getLibraries(); it.hasNext(); )
// 		{
// 			Library lib = it.next();
// 			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
// 			{
// 				Cell cell = cIt.next();
// 				if (cell.getView() == this)
// 				{
// 					System.out.println("Cannot delete view " + this.getFullName() +
// 						" because it is in use (for example, cell " + cell.describe() + ")");
// 					return;
// 				}
// 			}
// 		}
// 
// 		// make sure this can be done now
// 		Job.checkChanging();
// 
// 		// handle change control, constraint, and broadcast
// 		Undo.killObject(this);
// 
// 		// delete this view
//         synchronized(View.class) {
//             viewNames.remove(fullName);
//             viewNames.remove(abbreviation);
//             views.remove(this);
//         }
	}

	/****************************** IMPLEMENTATION ******************************/

	/**
	 * This constructor is never called.  Use the factory "makeInstance" instead.
	 */
	private View(String fullName, String abbr, int type, int order)
	{
        this.fullName = fullName;
        this.abbreviation = abbr;
        this.type = type;
        this.order = order;
	}

	private static View makeInstance(String fullName, String abbreviation, int type, int order)
	{
		// make sure the view doesn't already exist
		if (viewNames.containsKey(abbreviation))
		{
			System.out.println("multiple views with same name: " + abbreviation);
			return null;
		}
		if (viewNames.containsKey(fullName))
		{
			System.out.println("multiple views with same name: " + fullName);
			return null;
		}

		// create the view
		View v = new View(fullName, abbreviation, type, order);

		// enter both the full and short names into the hash table
        synchronized(View.class) {
            viewNames.put(fullName, v);
            viewNames.put(abbreviation, v);
            views.add(v);
        }
		return v;
	}

    private static int getNextOrder() {
        synchronized(View.class) {
            return overallOrder++;
        }
    }

	/****************************** INFORMATION ******************************/

	/**
	 * Method to return a View using its full or short name.
	 * @param name the name of the View.
	 * @return the named View, or null if no such View exists.
	 */
	public static View findView(String name)
	{
		return viewNames.get(name);
	}

	/**
	 * Method to return the full name of this View.
	 * @return the full name of this View.
	 */
	public String getFullName() { return fullName; }

	/**
	 * Method to return the short name of this View.
	 * The short name is used inside of braces when naming a cell (for example "gate{doc}").
	 * @return the short name of this View.
	 */
	public String getAbbreviation() { return abbreviation; }

	/**
	 * Method to get the ordering of this View for sorting.
	 * @return the ordering this View.
	 */
	public int getOrder() { return order; }

	/**
	 * Method to return true if this View is Text-only.
	 * Cells with text-only views have no nodes or arcs, just text.
	 * @return true if this View is Text-only.
	 */
	public boolean isTextView() { return (type & TEXTVIEW) != 0; }

	/**
	 * Method to return true if this View is permanent.
	 * Permanent views are those that are created initially, and cannot be deleted.
	 * @return true if this View is permanent.
	 */
	public boolean isPermanentView() { return (type & PERMANENTVIEW) != 0; }

	/**
	 * Method to return an iterator over the views.
	 * @return an iterator over the views.
	 */
	public static Iterator<View> getViews() { return unmodifiableViews.iterator(); }

	/**
	 * Method to return the number of views.
	 * @return the number of views.
	 */
	public static int getNumViews() { return views.size(); }

	/**
	 * Method to return a List of all libraries, sorted by name.
	 * The list excludes hidden libraries (i.e. the clipboard).
	 * @return a List of all libraries, sorted by name.
	 */
	public static List<View> getOrderedViews()
	{
		List<View> sortedList = new ArrayList<View>(views);
		Collections.sort(sortedList, new ViewByOrder());
		return sortedList;
	}

	private static class ViewByOrder implements Comparator<View>
	{
		public int compare(View v1, View v2)
		{
			return v1.getOrder() - v2.getOrder();
		}
	}

    /**
     * Compares two <code>View</code> objects.
     * @param that the View to be compared.
     * @return the result of comparison.
     */
	public int compareTo(View that)
	{
		return TextUtils.STRING_NUMBER_ORDER.compare(getAbbreviation(), that.getAbbreviation());
    }

	/**
	 * Returns a printable version of this View.
	 * @return a printable version of this View.
	 */
	public String toString()
	{
		return "View " + fullName;
	}

}
