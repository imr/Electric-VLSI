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
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Tool;

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

	/**
	 * The constructor sets up the DRC tool.
	 */
	private DRC()
	{
		super("DRC");
	}

	/**
	 * Routine to initialize the DRC tool.
	 */
	public void init()
	{
//		setOn();
	}
}
