/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DRC.java
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
package com.sun.electric.tool.drc;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Tool;

import java.util.Iterator;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

/**
 * This is the Design Rule Checker tool.
 */
public class DRC extends Tool
{
	// ---------------------- private and protected methods -----------------

	/** the DRC tool. */		public static DRC tool = new DRC();

	/** key of Variable with width limit for wide rules. */
	public static final Variable.Key WIDE_LIMIT = ElectricObject.newKey("DRC_wide_limit");
	/** key of Variable for minimum separation when connected. */
	public static final Variable.Key MIN_CONNECTED_DISTANCES = ElectricObject.newKey("DRC_min_connected_distances");
	/** key of Variable for minimum separation rule when connected. */
	public static final Variable.Key MIN_CONNECTED_DISTANCES_RULE = ElectricObject.newKey("DRC_min_connected_distances_rule");
	/** key of Variable for minimum separation when unconnected. */
	public static final Variable.Key MIN_UNCONNECTED_DISTANCES = ElectricObject.newKey("DRC_min_unconnected_distances");
	/** key of Variable for minimum separation rule when unconnected. */
	public static final Variable.Key MIN_UNCONNECTED_DISTANCES_RULE = ElectricObject.newKey("DRC_min_unconnected_distances_rule");
	/** key of Variable for minimum separation when connected and wide. */
	public static final Variable.Key MIN_CONNECTED_DISTANCES_WIDE = ElectricObject.newKey("DRC_min_connected_distances_wide");
	/** key of Variable for minimum separation rule when connected and wide. */
	public static final Variable.Key MIN_CONNECTED_DISTANCES_WIDE_RULE = ElectricObject.newKey("DRC_min_connected_distances_wide_rule");
	/** key of Variable for minimum separation when unconnected and wide. */
	public static final Variable.Key MIN_UNCONNECTED_DISTANCES_WIDE = ElectricObject.newKey("DRC_min_unconnected_distances_wide");
	/** key of Variable for minimum separation rule when unconnected and wide. */
	public static final Variable.Key MIN_UNCONNECTED_DISTANCES_WIDE_RULE = ElectricObject.newKey("DRC_min_unconnected_distances_wide_rule");
	/** key of Variable for minimum separation when connected and multicut. */
	public static final Variable.Key MIN_CONNECTED_DISTANCES_MULTI = ElectricObject.newKey("DRC_min_connected_distances_multi");
	/** key of Variable for minimum separation rule when connected and multicut. */
	public static final Variable.Key MIN_CONNECTED_DISTANCES_MULTI_RULE = ElectricObject.newKey("DRC_min_connected_distances_multi_rule");
	/** key of Variable for minimum separation when unconnected and multicut. */
	public static final Variable.Key MIN_UNCONNECTED_DISTANCES_MULTI = ElectricObject.newKey("DRC_min_unconnected_distances_multi");
	/** key of Variable for minimum separation rule when unconnected and multicut. */
	public static final Variable.Key MIN_UNCONNECTED_DISTANCES_MULTI_RULE = ElectricObject.newKey("DRC_min_unconnected_distances_multi_rule");
	/** key of Variable for minimum edge distance. */
	public static final Variable.Key MIN_EDGE_DISTANCES = ElectricObject.newKey("DRC_min_edge_distances");
	/** key of Variable for minimum edge distance rule. */
	public static final Variable.Key MIN_EDGE_DISTANCES_RULE = ElectricObject.newKey("DRC_min_edge_distances_rule");
	/** key of Variable for minimum layer width. */
	public static final Variable.Key MIN_WIDTH = ElectricObject.newKey("DRC_min_width");
	/** key of Variable for minimum layer width rule. */
	public static final Variable.Key MIN_WIDTH_RULE = ElectricObject.newKey("DRC_min_width_rule");
	/** key of Variable for minimum node size. */
	public static final Variable.Key MIN_NODE_SIZE = ElectricObject.newKey("DRC_min_node_size");
	/** key of Variable for minimum node size rule. */
	public static final Variable.Key MIN_NODE_SIZE_RULE = ElectricObject.newKey("DRC_min_node_size_rule");

	/** key of Variable for last valid DRC date on a Cell. */
	public static final Variable.Key LAST_GOOD_DRC = ElectricObject.newKey("DRC_last_good_drc");

	public static class Rules
	{
		/** name of the technology */								public String    techName;
		/** number of layers in the technology */					public int       numLayers;
		/** size of upper-triangle of layers */						public int       uTSize;
		/** width limit that triggers wide rules */					public Double    wideLimit;
		/** names of layers */										public String [] layerNames;
		/** minimum width of layers */								public Double [] minWidth;
		/** minimum width rules */									public String [] minWidthRules;
		/** minimum distances when connected */						public Double [] conList;
		/** minimum distance ruless when connected */				public String [] conListRules;	
		/** minimum distances when unconnected */					public Double [] unConList;
		/** minimum distance rules when unconnected */				public String [] unConListRules;
		/** minimum distances when connected (wide) */				public Double [] conListWide;	
		/** minimum distance rules when connected (wide) */			public String [] conListWideRules;
		/** minimum distances when unconnected (wide) */			public Double [] unConListWide;
		/** minimum distance rules when unconnected (wide) */		public String [] unConListWideRules;
		/** minimum distances when connected (multi-cut) */			public Double [] conListMulti;	
		/** minimum distance rules when connected (multi-cut) */	public String [] conListMultiRules;
		/** minimum distances when unconnected (multi-cut) */		public Double [] unConListMulti;
		/** minimum distance rules when unconnected (multi-cut) */	public String [] unConListMultiRules;
		/** edge distances */										public Double [] edgeList;	
		/** edge distance rules */									public String [] edgeListRules;

		/** number of nodes in the technology */					public int       numNodes;	
		/** names of nodes */										public String [] nodeNames;
		/** minimim node size in the technology */					public Double [] minNodeSize;
		/** minimim node size rules */								public String [] minNodeSizeRules;

		public Rules() {}
	}

	private Preferences prefs = Preferences.userNodeForPackage(getClass());

	/**
	 * The constructor sets up the DRC tool.
	 */
	private DRC()
	{
		super("DRC");
	}

	/**
	 * Method to initialize the DRC tool.
	 */
	public void init()
	{
//		setOn();
	}

	/**
	 * Method to delete all cached date information on all cells.
	 */
	public static void resetDates()
	{
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				Variable var = cell.getVar(LAST_GOOD_DRC);
				if (var == null) continue;
				cell.delVar(LAST_GOOD_DRC);
			}
		}
	}

	/**
	 * Method to force all DRC Preferences to be saved.
	 */
	private static void flushOptions()
	{
		try
		{
	        tool.prefs.flush();
		} catch (BackingStoreException e)
		{
			System.out.println("Failed to save DRC options");
		}
	}

	/**
	 * Method to tell whether DRC should be done incrementally.
	 * The default is "true".
	 * @return true if DRC should be done incrementally.
	 */
	public static boolean isIncrementalDRCOn() { return tool.prefs.getBoolean("IncrementalDRCOn", true); }
	/**
	 * Method to set whether DRC should be done incrementally.
	 * @param on true if DRC should be done incrementally.
	 */
	public static void setIncrementalDRCOn(boolean on) { tool.prefs.putBoolean("IncrementalDRCOn", on);   flushOptions(); }

	/**
	 * Method to tell whether DRC should report only one error per Cell.
	 * The default is "false".
	 * @return true if DRC should report only one error per Cell.
	 */
	public static boolean isOneErrorPerCell() { return tool.prefs.getBoolean("OneErrorPerCell", false); }
	/**
	 * Method to set whether DRC should report only one error per Cell.
	 * @param on true if DRC should report only one error per Cell.
	 */
	public static void setOneErrorPerCell(boolean on) { tool.prefs.putBoolean("OneErrorPerCell", on);   flushOptions(); }

	/**
	 * Method to tell whether DRC should use multiple threads.
	 * The default is "false".
	 * @return true if DRC should use multiple threads.
	 */
	public static boolean isUseMultipleThreads() { return tool.prefs.getBoolean("UseMultipleThreads", false); }
	/**
	 * Method to set whether DRC should use multiple threads.
	 * @param on true if DRC should use multiple threads.
	 */
	public static void setUseMultipleThreads(boolean on) { tool.prefs.putBoolean("UseMultipleThreads", on);   flushOptions(); }

	/**
	 * Method to return the number of threads to use when running DRC with multiple threads.
	 * The default is 2.
	 * @return the number of threads to use when running DRC with multiple threads.
	 */
	public static int getNumberOfThreads() { return tool.prefs.getInt("NumberOfThreads", 2); }
	/**
	 * Method to set the number of threads to use when running DRC with multiple threads.
	 * @param rot the number of threads to use when running DRC with multiple threads.
	 */
	public static void setNumberOfThreads(int rot) { tool.prefs.putInt("NumberOfThreads", rot);   flushOptions(); }

	/**
	 * Method to tell whether DRC should ignore center cuts in large contacts.
	 * Only the perimeter of cuts will be checked.
	 * The default is "false".
	 * @return true if DRC should ignore center cuts in large contacts.
	 */
	public static boolean isIgnoreCenterCuts() { return tool.prefs.getBoolean("IgnoreCenterCuts", false); }
	/**
	 * Method to set whether DRC should ignore center cuts in large contacts.
	 * Only the perimeter of cuts will be checked.
	 * @param on true if DRC should ignore center cuts in large contacts.
	 */
	public static void setIgnoreCenterCuts(boolean on) { tool.prefs.putBoolean("IgnoreCenterCuts", on);   flushOptions(); }
}
