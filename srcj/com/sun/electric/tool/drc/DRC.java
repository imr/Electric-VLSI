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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ErrorLog;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.util.Iterator;
import java.util.HashMap;
import java.util.Date;
import java.util.prefs.Preferences;

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

	/**
	 * Class to define a complete set of design rules.
	 * Includes constructors for initializing the data from a technology.
	 */
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
		/** minimum edge distances */								public Double [] edgeList;	
		/** minimum edge distance rules */							public String [] edgeListRules;

		/** number of nodes in the technology */					public int       numNodes;	
		/** names of nodes */										public String [] nodeNames;
		/** minimim node size in the technology */					public Double [] minNodeSize;
		/** minimim node size rules */								public String [] minNodeSizeRules;

		public Rules() {}

		public Rules(Technology tech)
		{
			// compute sizes
			numLayers = tech.getNumLayers();
			numNodes = tech.getNumNodes();
			uTSize = (numLayers * numLayers + numLayers) / 2;

			// initialize the width limit
			wideLimit = new Double(0);

			// add names
			techName = tech.getTechName();
			layerNames = new String[numLayers];
			int j = 0;
			for(Iterator it = tech.getLayers(); it.hasNext(); )
			{
				Layer layer = (Layer)it.next();
				layerNames[j++] = layer.getName();
			}
			nodeNames = new String[numNodes];
			j = 0;
			for(Iterator it = tech.getNodes(); it.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)it.next();
				nodeNames[j++] = np.getName();
			}

			// allocate tables
			conList = new Double[uTSize];
			conListRules = new String[uTSize];
			unConList = new Double[uTSize];
			unConListRules = new String[uTSize];
	
			conListWide = new Double[uTSize];
			conListWideRules = new String[uTSize];
			unConListWide = new Double[uTSize];
			unConListWideRules = new String[uTSize];
	
			conListMulti = new Double[uTSize];
			conListMultiRules = new String[uTSize];
			unConListMulti = new Double[uTSize];
			unConListMultiRules = new String[uTSize];
	
			edgeList = new Double[uTSize];
			edgeListRules = new String[uTSize];
	
			minWidth = new Double[numLayers];
			minWidthRules = new String[numLayers];
	
			// clear all tables
			for(int i=0; i<uTSize; i++)
			{
				conList[i] = new Double(-1);         conListRules[i] = "";
				unConList[i] = new Double(-1);       unConListRules[i] = "";
	
				conListWide[i] = new Double(-1);     conListWideRules[i] = "";
				unConListWide[i] = new Double(-1);   unConListWideRules[i] = "";
	
				conListMulti[i] = new Double(-1);    conListMultiRules[i] = "";
				unConListMulti[i] = new Double(-1);  unConListMultiRules[i] = "";
	
				edgeList[i] = new Double(-1);        edgeListRules[i] = "";
			}
			for(int i=0; i<numLayers; i++)
			{
				minWidth[i] = new Double(-1);        minWidthRules[i] = "";
			}

			// build node size tables
			minNodeSize = new Double[numNodes*2];
			minNodeSizeRules = new String[numNodes];
			j = 0;
			for(Iterator it = tech.getNodes(); it.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)it.next();
				minNodeSize[j*2] = new Double(np.getMinWidth());
				minNodeSize[j*2+1] = new Double(np.getMinHeight());
				minNodeSizeRules[j] = np.getMinSizeRule();
				j++;
			}
		}
	}

	public static class Rule
	{
		public double distance;
		public String rule;

		Rule(double distance, String rule)
		{
			this.distance = distance;
			this.rule = rule;
		}
	}

	public static class NodeSizeRule
	{
		public double sizeX, sizeY;
		public String rule;

		NodeSizeRule(double sizeX, double sizeY, String rule)
		{
			this.sizeX = sizeX;
			this.sizeY = sizeY;
			this.rule = rule;
		}
	}

	/** overrides of rules for each technology. */		private static HashMap prefDRCOverride = new HashMap();

	/**
	 * The constructor sets up the DRC tool.
	 */
	private DRC()
	{
		super("drc");
	}

	/**
	 * Method to initialize the DRC tool.
	 */
	public void init()
	{
//		setOn();
	}

	/**
	 * Method to check the current cell hierarchically.
	 */
	public static void checkHierarchically()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
		if (curCell.getView() == View.SCHEMATIC || curCell.getTechnology() == Schematics.tech)
		{
			// hierarchical check of schematics
			CheckSchematicHierarchically job = new CheckSchematicHierarchically(curCell, false);
		} else
		{
			// hierarchical check of layout
			CheckLayoutHierarchically job = new CheckLayoutHierarchically(curCell, false);
		}
	}

	/**
	 * Method to check the selected area of the current cell hierarchically.
	 */
	public static void checkAreaHierarchically()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
		if (curCell.getView() == View.SCHEMATIC || curCell.getTechnology() == Schematics.tech)
		{
			// hierarchical check of schematics
			CheckSchematicHierarchically job = new CheckSchematicHierarchically(curCell, true);
		} else
		{
			// hierarchical check of layout
			CheckLayoutHierarchically job = new CheckLayoutHierarchically(curCell, true);
		}
	}

	private static class CheckLayoutHierarchically extends Job
	{
		Cell cell;
		boolean justArea;

		protected CheckLayoutHierarchically(Cell cell, boolean justArea)
		{
			super("Design-Rule Check", tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.justArea = justArea;
			startJob();
		}

		public boolean doIt()
		{
			long startTime = System.currentTimeMillis();
			Quick.checkDesignRules(cell, 0, null, null, justArea);
			long endTime = System.currentTimeMillis();
			int errorcount = ErrorLog.numErrors();
			System.out.println(errorcount + " errors found (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
			return true;
		}
	}

	private static class CheckSchematicHierarchically extends Job
	{
		Cell cell;
		boolean justArea;

		protected CheckSchematicHierarchically(Cell cell, boolean justArea)
		{
			super("Design-Rule Check", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.justArea = justArea;
			startJob();
		}

		public boolean doIt()
		{
			long startTime = System.currentTimeMillis();
			Schematic.doCheck(cell);
			long endTime = System.currentTimeMillis();
			int errorcount = ErrorLog.numErrors();
			System.out.println(errorcount + " errors found (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
			return true;
		}
	}

	/**
	 * Method to delete all cached date information on all cells.
	 */
	public static void resetDRCDates()
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

	/****************************** DESIGN RULE CONTROL ******************************/

	/** Cached rules for a specific technology. */		private static Rules currentRules = null;
	/** The Technology whose rules are cached. */		private static Technology currentTechnology = null;

	/**
	 * Method to build a Rules object that contains the current design rules for a Technology.
	 * The DRC dialogs use this to hold the values while editing them.
	 * It also provides a cache for the design rule checker.
	 * @param tech the Technology to examine.
	 * @return a new Rules object with the design rules for the given Technology.
	 */
	public static Rules getRules(Technology tech)
	{
		if (currentRules != null && tech == currentTechnology) return currentRules;

		// constructing design rules: start with factory rules
		Rules currentRules = tech.getFactoryDesignRules();
		if (currentRules != null)
		{
			// add overrides
			StringBuffer override = getDRCOverrides(tech);
			applyDRCOverrides(override.toString(), currentRules, tech);
		}

		// remember technology whose rules are cached
		currentTechnology = tech;
		return currentRules;
	}

	/**
	 * Method to load a full set of design rules for a Technology.
	 * @param tech the Technology to load.
	 * @param newRules a complete design rules object.
	 */
	public static void setRules(Technology tech, Rules newRules)
	{
		// get factory design rules
		Rules factoryRules = tech.getFactoryDesignRules();

		// determine override differences from the factory rules
		StringBuffer changes = getRuleDifferences(tech, factoryRules, newRules);

		// get current overrides of factory rules
		StringBuffer override = getDRCOverrides(tech);

		// if the differences are the same as before, stop
		if (changes.toString().equals(override.toString())) return;

		// update the preference for the rule overrides
		setDRCOverrides(changes, tech);

		// update variables on the technology
		Variable var = tech.newVar(WIDE_LIMIT, newRules.wideLimit);
		var = tech.newVar(MIN_CONNECTED_DISTANCES, newRules.conList);
		if (var != null) var.setDontSave();
		var = tech.newVar(MIN_CONNECTED_DISTANCES_RULE, newRules.conListRules);
		if (var != null) var.setDontSave();
		var = tech.newVar(MIN_UNCONNECTED_DISTANCES, newRules.unConList);
		if (var != null) var.setDontSave();
		var = tech.newVar(MIN_UNCONNECTED_DISTANCES_RULE, newRules.unConListRules);
		if (var != null) var.setDontSave();
		
		var = tech.newVar(MIN_CONNECTED_DISTANCES_WIDE, newRules.conListWide);
		if (var != null) var.setDontSave();
		var = tech.newVar(MIN_CONNECTED_DISTANCES_WIDE_RULE, newRules.conListWideRules);
		if (var != null) var.setDontSave();
		var = tech.newVar(MIN_UNCONNECTED_DISTANCES_WIDE, newRules.unConListWide);
		if (var != null) var.setDontSave();
		var = tech.newVar(MIN_UNCONNECTED_DISTANCES_WIDE_RULE, newRules.unConListWideRules);
		if (var != null) var.setDontSave();
		
		var = tech.newVar(MIN_CONNECTED_DISTANCES_MULTI, newRules.conListMulti);
		if (var != null) var.setDontSave();
		var = tech.newVar(MIN_CONNECTED_DISTANCES_MULTI_RULE, newRules.conListMultiRules);
		if (var != null) var.setDontSave();
		var = tech.newVar(MIN_UNCONNECTED_DISTANCES_MULTI, newRules.unConListMulti);
		if (var != null) var.setDontSave();
		var = tech.newVar(MIN_UNCONNECTED_DISTANCES_MULTI_RULE, newRules.unConListMultiRules);
		if (var != null) var.setDontSave();
		
		var = tech.newVar(MIN_EDGE_DISTANCES, newRules.edgeList);
		if (var != null) var.setDontSave();
		var = tech.newVar(MIN_EDGE_DISTANCES_RULE, newRules.edgeListRules);
		if (var != null) var.setDontSave();
		
		var = tech.newVar(MIN_WIDTH, newRules.minWidth);
		if (var != null) var.setDontSave();
		var = tech.newVar(MIN_WIDTH_RULE, newRules.minWidthRules);
		if (var != null) var.setDontSave();

		// update per-node information
		int j = 0;
		for(Iterator it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			np.setMinSize(newRules.minNodeSize[j*2].doubleValue(), newRules.minNodeSize[j*2+1].doubleValue(),
				newRules.minNodeSizeRules[j]);
			j++;
		}

		// flush the cache of rules
		if (currentTechnology == tech) currentTechnology = null;
	}

	/**
	 * Method to create a set of Design Rules from some simple spacing arrays.
	 * Used by simpler technologies that do not have full-sets of design rules.
	 * @param tech the Technology to load.
	 * @param conDist an upper-diagonal array of layer-to-layer distances (when connected).
	 * @param unConDist an upper-diagonal array of layer-to-layer distances (when unconnected).
	 * @return a set of design rules for the Technology.
	 */
	public static Rules makeSimpleRules(Technology tech, double [] conDist, double [] unConDist)
	{
		Rules rules = new Rules(tech);
		if (conDist != null)
		{
			for(int i=0; i<conDist.length; i++)
			{
				rules.conList[i] = new Double(conDist[i]);
			}
		}
		if (unConDist != null)
		{
			for(int i=0; i<unConDist.length; i++)
			{
				rules.unConList[i] = new Double(unConDist[i]);
			}
		}
		return rules;
	}

	/****************************** INDIVIDUAL DESIGN RULES ******************************/

	/**
	 * Method to find the worst spacing distance in the design rules.
	 * Finds the largest spacing rule in the Technology.
	 * @param tech the Technology to examine.
	 * @return the largest spacing distance in the Technology.
	 */
	public static double getWorstSpacingDistance(Technology tech)
	{
		Rules rules = getRules(tech);
		double worstInteractionDistance = 0;
		for(int i = 0; i < rules.uTSize; i++)
		{
			double dist = rules.unConList[i].doubleValue();
			if (dist > worstInteractionDistance) worstInteractionDistance = dist;
			dist = rules.unConListWide[i].doubleValue();
			if (dist > worstInteractionDistance) worstInteractionDistance = dist;
			dist = rules.unConListMulti[i].doubleValue();
			if (dist > worstInteractionDistance) worstInteractionDistance = dist;
		}
		return worstInteractionDistance;
	}

	/**
	 * Method to find the maximum design-rule distance around a layer.
	 * @param layer the Layer to examine.
	 * @return the maximum design-rule distance around the layer.
	 */
	public static double getMaxSurround(Layer layer)
	{
		Technology tech = layer.getTechnology();
		Rules rules = getRules(tech);
		double worstLayerRule = -1;
		int layerIndex = layer.getIndex();
		int tot = tech.getNumLayers();
		for(int i=0; i<tot; i++)
		{
			int pIndex = getIndex(tech, layerIndex, i);
			double dist = rules.unConList[pIndex].doubleValue();
			if (dist > worstLayerRule) worstLayerRule = dist;
		}
		return worstLayerRule;
	}

	/**
	 * Method to find the edge spacing rule between two layer.
	 * @param layer1 the first layer.
	 * @param layer2 the second layer.
	 * @return the edge rule distance between the layers.
	 * Returns null if there is no edge spacing rule.
	 */
	public static Rule getEdgeRule(Layer layer1, Layer layer2)
	{
		Technology tech = layer1.getTechnology();
		Rules rules = getRules(tech);
		int pIndex = getIndex(tech, layer1.getIndex(), layer2.getIndex());
		double dist = rules.edgeList[pIndex].doubleValue();
		if (dist < 0) return null;
		return new Rule(dist, rules.edgeListRules[pIndex]);
	}

	/**
	 * Method to return the "wide" limit for a Technology.
	 * There are different design rules, depending on whether the geometry is wide or not.
	 * @param tech the Technology in question.
	 * @return the "wide" limit.  Anything this size or larger must use "wide" design rules.
	 */
	public static double getWideLimit(Technology tech)
	{
		Rules rules = getRules(tech);
		return rules.wideLimit.doubleValue();
	}

	/**
	 * Method to find the spacing rule between two layer.
	 * @param layer1 the first layer.
	 * @param layer2 the second layer.
	 * @param connected true to find the distance when the layers are connected.
	 * @param wide true to find the distance when one of the layers is "wide".
	 * @param multiCut true to find the distance when this is part of a multicut contact.
	 * @return the spacing rule between the layers.
	 * Returns null if there is no spacing rule.
	 */
	public static Rule getSpacingRule(Layer layer1, Layer layer2, boolean connected, boolean wide, boolean multiCut)
	{
		Technology tech = layer1.getTechnology();
		Rules rules = getRules(tech);
		int pIndex = getIndex(tech, layer1.getIndex(), layer2.getIndex());

		double bestDist = -1;
		String rule = null;
		if (connected)
		{
			double dist = rules.conList[pIndex].doubleValue();
			if (dist >= 0) { bestDist = dist;   rule = rules.conListRules[pIndex]; }
		} else
		{
			double dist = rules.unConList[pIndex].doubleValue();
			if (dist >= 0) { bestDist = dist;   rule = rules.unConListRules[pIndex]; }
		}

		if (wide)
		{
			if (connected)
			{
				double dist = rules.conListWide[pIndex].doubleValue();
				if (dist >= 0) { bestDist = dist;   rule = rules.conListWideRules[pIndex]; }
			} else
			{
				double dist = rules.unConListWide[pIndex].doubleValue();
				if (dist >= 0) { bestDist = dist;   rule = rules.unConListWideRules[pIndex]; }
			}
		}

		if (multiCut)
		{
			if (connected)
			{
				double dist = rules.conListMulti[pIndex].doubleValue();
				if (dist >= 0) { bestDist = dist;   rule = rules.conListMultiRules[pIndex]; }
			} else
			{
				double dist = rules.unConListMulti[pIndex].doubleValue();
				if (dist >= 0) { bestDist = dist;   rule = rules.unConListMultiRules[pIndex]; }
			}
		}
		if (bestDist < 0) return null;
		return new Rule(bestDist, rule);
	}

	/**
	 * Method to tell whether there are any design rules between two layers.
	 * @param layer1 the first Layer to check.
	 * @param layer2 the second Layer to check.
	 * @return true if there are design rules between the layers.
	 */
	public static boolean isAnyRule(Layer layer1, Layer layer2)
	{
		Technology tech = layer1.getTechnology();
		Rules rules = getRules(tech);
		int pIndex = getIndex(tech, layer1.getIndex(), layer2.getIndex());
		if (rules.conList[pIndex].doubleValue() >= 0) return true;
		if (rules.unConList[pIndex].doubleValue() >= 0) return true;
		if (rules.conListWide[pIndex].doubleValue() >= 0) return true;
		if (rules.unConListWide[pIndex].doubleValue() >= 0) return true;
		if (rules.conListMulti[pIndex].doubleValue() >= 0) return true;
		if (rules.unConListMulti[pIndex].doubleValue() >= 0) return true;
		if (rules.edgeList[pIndex].doubleValue() >= 0) return true;
		return false;
	}

	/**
	 * Method to get the minimum width rule for a Layer.
	 * @param layer the Layer to examine.
	 * @return the minimum width rule for the layer.
	 * Returns null if there is no minimum width rule.
	 */
	public static Rule getMinWidth(Layer layer)
	{
		Technology tech = layer.getTechnology();
		Rules rules = getRules(tech);
		int index = layer.getIndex();
		double dist = rules.minWidth[index].doubleValue();
		if (dist < 0) return null;
		return new Rule(dist, rules.minWidthRules[index]);
	}

	/**
	 * Method to get the minimum size rule for a NodeProto.
	 * @param np the NodeProto to examine.
	 * @return the minimum size rule for the NodeProto.
	 * Returns null if there is no minimum size rule.
	 */
	public static NodeSizeRule getMinSize(NodeProto np)
	{
		if (np instanceof Cell) return null;
		PrimitiveNode pnp = (PrimitiveNode)np;
		if (pnp.getMinWidth() < 0 && pnp.getMinHeight() < 0) return null;
		return new NodeSizeRule(pnp.getMinWidth(), pnp.getMinHeight(), pnp.getMinSizeRule());
	}
 
	/****************************** SUPPORT FOR DESIGN RULES ******************************/

	/**
	 * Method to get the DRC overrides from the preferences for a given technology.
	 * @param tech the Technology on which to get overrides.
	 * @return a Pref describing DRC overrides for the Technology.
	 */
	private static StringBuffer getDRCOverrides(Technology tech)
	{
		Pref pref = (Pref)prefDRCOverride.get(tech);
		if (pref == null)
		{
			pref = Pref.makeStringPref("DRCOverridesFor" + tech.getTechName(), DRC.tool.prefs, "");
			prefDRCOverride.put(tech, pref);
		}
		StringBuffer sb = new StringBuffer();
		sb.append(pref.getString());
		return sb;
	}

	/**
	 * Method to set the DRC overrides for a given technology.
	 * @param sb the overrides (a StringBuffer).
	 * @param tech the Technology on which to get overrides.
	 */
	private static void setDRCOverrides(StringBuffer sb, Technology tech)
	{
		if (sb.length() >= Preferences.MAX_VALUE_LENGTH)
		{
			System.out.println("Warning: Design rule overrides are too complex to be saved (are " +
				sb.length() + " long which is more than the limit of " + Preferences.MAX_VALUE_LENGTH + ")");
		}
		Pref pref = (Pref)prefDRCOverride.get(tech);
		if (pref == null)
		{
			pref = Pref.makeStringPref("DRCOverridesFor" + tech.getTechName(), DRC.tool.prefs, "");
			prefDRCOverride.put(tech, pref);
		}
		pref.setString(sb.toString());
	}

	/**
	 * Method to determine the index in the upper-left triangle array for two layers.
	 * @param layer1 the first layer index.
	 * @param layer2 the second layer index.
	 * @return the index in the array that corresponds to these two layers.
	 */
	private static int getIndex(Technology tech, int layer1Index, int layer2Index)
	{
		if (layer1Index > layer2Index) { int temp = layer1Index; layer1Index = layer2Index;  layer2Index = temp; }
		int pIndex = (layer1Index+1) * (layer1Index/2) + (layer1Index&1) * ((layer1Index+1)/2);
		pIndex = layer2Index + tech.getNumLayers() * layer1Index - pIndex;
		return pIndex;
	}

	/**
	 * Method to compare a Rules set with the "factory" set and construct an override string.
	 * @param tech the Technology with the design rules.
	 * @param origRules the original "Factory" rules.
	 * @param newRules the new Rules.
	 * @return a StringBuffer that describes any overrides.  Returns "" if there are none.
	 */
	private static StringBuffer getRuleDifferences(Technology tech, Rules origRules, Rules newRules)
	{
		StringBuffer changes = new StringBuffer();

		// include differences in the wide-rule limit
		if (!newRules.wideLimit.equals(origRules.wideLimit))
		{
			changes.append("w:"+newRules.wideLimit+";");
		}

		// include differences in layer spacings
		for(int l1=0; l1<tech.getNumLayers(); l1++)
			for(int l2=0; l2<=l1; l2++)
		{
			int i = getIndex(tech, l2, l1);
			if (!newRules.conList[i].equals(origRules.conList[i]))
			{
				changes.append("c:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.conList[i]+";");
			}
			if (!newRules.conListRules[i].equals(origRules.conListRules[i]))
			{
				changes.append("cr:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.conListRules[i]+";");
			}
			if (!newRules.unConList[i].equals(origRules.unConList[i]))
			{
				changes.append("u:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.unConList[i]+";");
			}
			if (!newRules.unConListRules[i].equals(origRules.unConListRules[i]))
			{
				changes.append("ur:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.unConListRules[i]+";");
			}

			if (!newRules.conListWide[i].equals(origRules.conListWide[i]))
			{
				changes.append("cw:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.conListWide[i]+";");
			}
			if (!newRules.conListWideRules[i].equals(origRules.conListWideRules[i]))
			{
				changes.append("cwr:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.conListWideRules[i]+";");
			}
			if (!newRules.unConListWide[i].equals(origRules.unConListWide[i]))
			{
				changes.append("uw:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.unConListWide[i]+";");
			}
			if (!newRules.unConListWideRules[i].equals(origRules.unConListWideRules[i]))
			{
				changes.append("uwr:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.unConListWideRules[i]+";");
			}

			if (!newRules.conListMulti[i].equals(origRules.conListMulti[i]))
			{
				changes.append("cm:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.conListMulti[i]+";");
			}
			if (!newRules.conListMultiRules[i].equals(origRules.conListMultiRules[i]))
			{
				changes.append("cmr:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.conListMultiRules[i]+";");
			}
			if (!newRules.unConListMulti[i].equals(origRules.unConListMulti[i]))
			{
				changes.append("um:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.unConListMulti[i]+";");
			}
			if (!newRules.unConListMultiRules[i].equals(origRules.unConListMultiRules[i]))
			{
				changes.append("umr:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.unConListMultiRules[i]+";");
			}

			if (!newRules.edgeList[i].equals(origRules.edgeList[i]))
			{
				changes.append("e:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.edgeList[i]+";");
			}
			if (!newRules.edgeListRules[i].equals(origRules.edgeListRules[i]))
			{
				changes.append("er:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.edgeListRules[i]+";");
			}
		}

		// include differences in minimum layer widths
		for(int i=0; i<newRules.numLayers; i++)
		{
			if (!newRules.minWidth[i].equals(origRules.minWidth[i]))
			{
				changes.append("m:"+tech.getLayer(i).getName()+"="+newRules.minWidth[i]+";");
			}
			if (!newRules.minWidthRules[i].equals(origRules.minWidthRules[i]))
			{
				changes.append("mr:"+tech.getLayer(i).getName()+"="+newRules.minWidthRules[i]+";");
			}
		}

		// include differences in minimum node sizes
		int j = 0;
		for(Iterator it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			if (!newRules.minNodeSize[j*2].equals(origRules.minNodeSize[j*2]) ||
				!newRules.minNodeSize[j*2+1].equals(origRules.minNodeSize[j*2+1]))
			{
				changes.append("n:"+np.getName()+"="+newRules.minNodeSize[j*2]+"/"+newRules.minNodeSize[j*2+1]+";");
			}
			if (!newRules.minNodeSizeRules[j].equals(origRules.minNodeSizeRules[j]))
			{
				changes.append("nr:"+np.getName()+"="+newRules.minNodeSizeRules[j]+";");
			}
			j++;
		}
		return changes;
	}

	/**
	 * Method to apply overrides to a set of rules.
	 * @param override the override string.
	 * @param rules the Rules to modify.
	 * @param tech the Technology in which these rules live.
	 */
	private static void applyDRCOverrides(String override, Rules rules, Technology tech)
	{
		int pos = 0;
		int len = override.length();
		while (pos < len)
		{
			int startKey = pos;
			int endKey = override.indexOf(':', startKey);
			if (endKey < 0) break;
			String key = override.substring(startKey, endKey);
			if (key.equals("c") || key.equals("cr") || key.equals("u") || key.equals("ur") ||
				key.equals("cw") || key.equals("cwr") || key.equals("uw") || key.equals("uwr") ||
				key.equals("cm") || key.equals("cmr") || key.equals("um") || key.equals("umr") ||
				key.equals("e") || key.equals("er"))
			{
				startKey = endKey + 1;
				Layer layer1 = getLayerFromOverride(override, startKey, '/', tech);
				if (layer1 == null) break;
				startKey = override.indexOf('/', startKey);
				if (startKey < 0) break;
				Layer layer2 = getLayerFromOverride(override, startKey+1, '=', tech);
				if (layer2 == null) break;
				startKey = override.indexOf('=', startKey);
				if (startKey < 0) break;
				endKey = override.indexOf(';', startKey);
				if (endKey < 0) break;
				String newValue = override.substring(startKey+1, endKey);
				int index = getIndex(tech, layer1.getIndex(), layer2.getIndex());
				if (key.equals("c"))
				{
					rules.conList[index] = new Double(TextUtils.atof(newValue));
				} else if (key.equals("cr"))
				{
					rules.conListRules[index] = newValue;
				} else if (key.equals("u"))
				{
					rules.unConList[index] = new Double(TextUtils.atof(newValue));
				} else if (key.equals("ur"))
				{
					rules.unConListRules[index] = newValue;
				} else if (key.equals("cw"))
				{
					rules.conListWide[index] = new Double(TextUtils.atof(newValue));
				} else if (key.equals("cwr"))
				{
					rules.conListWideRules[index] = newValue;
				} else if (key.equals("uw"))
				{
					rules.unConListWide[index] = new Double(TextUtils.atof(newValue));
				} else if (key.equals("uwr"))
				{
					rules.unConListWideRules[index] = newValue;
				} else if (key.equals("cm"))
				{
					rules.conListMulti[index] = new Double(TextUtils.atof(newValue));
				} else if (key.equals("cmr"))
				{
					rules.conListMultiRules[index] = newValue;
				} else if (key.equals("um"))
				{
					rules.unConListMulti[index] = new Double(TextUtils.atof(newValue));
				} else if (key.equals("umr"))
				{
					rules.unConListMultiRules[index] = newValue;
				} else if (key.equals("e"))
				{
					rules.edgeList[index] = new Double(TextUtils.atof(newValue));
				} else if (key.equals("er"))
				{
					rules.edgeListRules[index] = newValue;
				}
				pos = endKey + 1;
				continue;
			}
			if (key.equals("m") || key.equals("mr"))
			{
				startKey = endKey + 1;
				Layer layer = getLayerFromOverride(override, startKey, '=', tech);
				if (layer == null) break;
				startKey = override.indexOf('=', startKey);
				if (startKey < 0) break;
				endKey = override.indexOf(';', startKey);
				if (endKey < 0) break;
				String newValue = override.substring(startKey+1, endKey);
				int index = layer.getIndex();
				if (key.equals("m"))
				{
					rules.minWidth[index] = new Double(TextUtils.atof(newValue));
				} else if (key.equals("mr"))
				{
					rules.minWidthRules[index] = newValue;
				}
				pos = endKey + 1;
				continue;
			}
			if (key.equals("n") || key.equals("nr"))
			{
				startKey = endKey + 1;
				int endPos = override.indexOf('=', startKey);
				if (endPos < 0) break;
				String nodeName = override.substring(startKey, endPos);
				PrimitiveNode np = tech.findNodeProto(nodeName);
				if (np == null) break;
				int index = 0;
				for(Iterator it = tech.getNodes(); it.hasNext(); )
				{
					PrimitiveNode oNp = (PrimitiveNode)it.next();
					if (oNp == np) break;
					index++;
				}
				if (key.equals("n"))
				{
					startKey = override.indexOf('=', startKey);
					if (startKey < 0) break;
					endKey = override.indexOf('/', startKey);
					if (endKey < 0) break;
					String newValue1 = override.substring(startKey+1, endKey);
					int otherEndKey = override.indexOf(';', startKey);
					if (otherEndKey < 0) break;
					String newValue2 = override.substring(endKey+1, otherEndKey);
					rules.minNodeSize[index*2] = new Double(TextUtils.atof(newValue1));
					rules.minNodeSize[index*2+1] = new Double(TextUtils.atof(newValue2));
				} else if (key.equals("nr"))
				{
					startKey = override.indexOf('=', startKey);
					if (startKey < 0) break;
					endKey = override.indexOf(';', startKey);
					if (endKey < 0) break;
					String newValue = override.substring(startKey+1, endKey);
					rules.minNodeSizeRules[index] = newValue;
				}
				pos = endKey + 1;
				continue;
			}
			if (key.equals("w"))
			{
				startKey = endKey + 1;
				endKey = override.indexOf(';', startKey);
				if (endKey < 0) break;
				String newValue = override.substring(startKey, endKey);
				rules.wideLimit = new Double(TextUtils.atof(newValue));
				pos = endKey + 1;
				continue;
			}
		}
	}

	private static Layer getLayerFromOverride(String override, int startPos, char endChr, Technology tech)
	{
		int endPos = override.indexOf(endChr, startPos);
		if (endPos < 0) return null;
		String layerName = override.substring(startPos, endPos);
		Layer layer = tech.findLayer(layerName);
		return layer;
	}

	/****************************** OPTIONS ******************************/

	private static Pref cacheIncrementalDRCOn = Pref.makeBooleanPref("IncrementalDRCOn", DRC.tool.prefs, true);
	/**
	 * Method to tell whether DRC should be done incrementally.
	 * The default is "true".
	 * @return true if DRC should be done incrementally.
	 */
	public static boolean isIncrementalDRCOn() { return cacheIncrementalDRCOn.getBoolean(); }
	/**
	 * Method to set whether DRC should be done incrementally.
	 * @param on true if DRC should be done incrementally.
	 */
	public static void setIncrementalDRCOn(boolean on) { cacheIncrementalDRCOn.setBoolean(on); }

	private static Pref cacheOneErrorPerCell = Pref.makeBooleanPref("OneErrorPerCell", DRC.tool.prefs, false);
	/**
	 * Method to tell whether DRC should report only one error per Cell.
	 * The default is "false".
	 * @return true if DRC should report only one error per Cell.
	 */
	public static boolean isOneErrorPerCell() { return cacheOneErrorPerCell.getBoolean(); }
	/**
	 * Method to set whether DRC should report only one error per Cell.
	 * @param on true if DRC should report only one error per Cell.
	 */
	public static void setOneErrorPerCell(boolean on) { cacheOneErrorPerCell.setBoolean(on); }

	private static Pref cacheUseMultipleThreads = Pref.makeBooleanPref("UseMultipleThreads", DRC.tool.prefs, false);
	/**
	 * Method to tell whether DRC should use multiple threads.
	 * The default is "false".
	 * @return true if DRC should use multiple threads.
	 */
	public static boolean isUseMultipleThreads() { return cacheUseMultipleThreads.getBoolean(); }
	/**
	 * Method to set whether DRC should use multiple threads.
	 * @param on true if DRC should use multiple threads.
	 */
	public static void setUseMultipleThreads(boolean on) { cacheUseMultipleThreads.setBoolean(on); }

	private static Pref cacheNumberOfThreads = Pref.makeIntPref("NumberOfThreads", DRC.tool.prefs, 2);
	/**
	 * Method to return the number of threads to use when running DRC with multiple threads.
	 * The default is 2.
	 * @return the number of threads to use when running DRC with multiple threads.
	 */
	public static int getNumberOfThreads() { return cacheNumberOfThreads.getInt(); }
	/**
	 * Method to set the number of threads to use when running DRC with multiple threads.
	 * @param th the number of threads to use when running DRC with multiple threads.
	 */
	public static void setNumberOfThreads(int th) { cacheNumberOfThreads.setInt(th); }

	private static Pref cacheIgnoreCenterCuts = Pref.makeBooleanPref("IgnoreCenterCuts", DRC.tool.prefs, false);
    static { cacheIgnoreCenterCuts.attachToObject(DRC.tool, "Tool Options, DRC tab", "DRC ignores center cuts in large contacts"); }
	/**
	 * Method to tell whether DRC should ignore center cuts in large contacts.
	 * Only the perimeter of cuts will be checked.
	 * The default is "false".
	 * @return true if DRC should ignore center cuts in large contacts.
	 */
	public static boolean isIgnoreCenterCuts() { return cacheIgnoreCenterCuts.getBoolean(); }
	/**
	 * Method to set whether DRC should ignore center cuts in large contacts.
	 * Only the perimeter of cuts will be checked.
	 * @param on true if DRC should ignore center cuts in large contacts.
	 */
	public static void setIgnoreCenterCuts(boolean on) { cacheIgnoreCenterCuts.setBoolean(on); }

	public static final Variable.Key POSTSCRIPT_FILEDATE = ElectricObject.newKey("IO_postscript_filedate");
	/**
	 * Method to tell the date of the last successful DRC of a given Cell.
	 * @param cell the cell to query.
	 * @return the date of the last successful DRC of that Cell.
	 */
	public static Date getLastDRCDate(Cell cell)
	{
		Variable varDate = cell.getVar(LAST_GOOD_DRC, Integer[].class);
		if (varDate == null) return null;
		Integer [] lastDRCDateAsInts = (Integer [])varDate.getObject();
		long lastDRCDateInSecondsHigh = lastDRCDateAsInts[0].intValue();
		long lastDRCDateInSecondsLow = lastDRCDateAsInts[1].intValue();
		long lastDRCDateInSeconds = (lastDRCDateInSecondsHigh << 32) | (lastDRCDateInSecondsLow & 0xFFFFFFFFL);
		Date lastDRCDate = new Date(lastDRCDateInSeconds);
		return lastDRCDate;
	}
	/**
	 * Method to set the date of the last successful DRC of a given Cell.
	 * @param cell the cell to modify.
	 * @param date the date of the last successful DRC of that Cell.
	 */
	public static void setLastDRCDate(Cell cell, Date date)
	{
		long iVal = date.getTime();
		Integer [] dateArray = new Integer[2];
		dateArray[0] = new Integer((int)(iVal >> 32));
		dateArray[1] = new Integer((int)(iVal & 0xFFFFFFFF));
		cell.newVar(DRC.LAST_GOOD_DRC, dateArray);
	}
}
