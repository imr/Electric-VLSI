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
import java.util.Date;

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

	/** Cached rules for a specific technology. */		private static Rules currentRules = null;
	/** The Technology whose rules are cached. */		private static Technology currentTechnology = null;

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
	 * Method to check the current cell hierarchically.
	 */
	public static void checkHierarchically()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
        CheckHierarchically job = new CheckHierarchically(curCell, false);
	}

	/**
	 * Method to check the selected area of the current cell hierarchically.
	 */
	public static void checkAreaHierarchically()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
        CheckHierarchically job = new CheckHierarchically(curCell, true);
	}

	protected static class CheckHierarchically extends Job
	{
		Cell cell;
		boolean justArea;

		protected CheckHierarchically(Cell cell, boolean justArea)
		{
			super("Design-Rule Check", tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.justArea = justArea;
			startJob();
		}

		public void doIt()
		{
			long startTime = System.currentTimeMillis();
			if (cell.getView() == View.SCHEMATIC || cell.getTechnology() == Schematics.tech)
			{
				// hierarchical check of schematics
				Schematic.doCheck(cell);
			} else
			{
				// hierarchical check of layout
				Quick.doCheck(cell, 0, null, null, justArea);
			}
			long endTime = System.currentTimeMillis();
			int errorcount = ErrorLog.numErrors();
			System.out.println(errorcount + " errors found (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
		}
	}

	/****************************** DESIGN RULES ******************************/

	/**
	 * Method to build a Rules object that contains all of the design rules for a Technology.
	 * The DRC dialogs use this to hold the values while editing them.
	 * It also provides a cache for the design rule checker.
	 * @param tech the Technology to examine.
	 * @return a new Rules object with the design rules for this Technology.
	 */
	public static Rules getRules(Technology tech)
	{
		cacheDesignRules(tech);
		return currentRules;
	}

	/**
	 * Method to update the design rules.
	 * @param newRules the new design rules.
	 */
	public static void modifyRules(Rules newRules)
	{
		NewDRCRules job = new NewDRCRules(newRules);
	}

	/**
	 * Method to find the worst spacing distance in the design rules.
	 * Finds the largest spacing rule in the Technology.
	 * @param tech the Technology to examine.
	 * @return the largest spacing distance in the Technology.
	 */
	public static double getWorstSpacingDistance(Technology tech)
	{
		cacheDesignRules(tech);
		double worstInteractionDistance = 0;
		for(int i = 0; i < currentRules.uTSize; i++)
		{
			double dist = currentRules.unConList[i].doubleValue();
			if (dist > worstInteractionDistance) worstInteractionDistance = dist;
			dist = currentRules.unConListWide[i].doubleValue();
			if (dist > worstInteractionDistance) worstInteractionDistance = dist;
			dist = currentRules.unConListMulti[i].doubleValue();
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
		cacheDesignRules(tech);
		double worstLayerRule = -1;
		int layerIndex = layer.getIndex();
		int tot = tech.getNumLayers();
		for(int i=0; i<tot; i++)
		{
			int pIndex = getIndex(tech, layerIndex, i);
			double dist = currentRules.unConList[pIndex].doubleValue();
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
		cacheDesignRules(tech);
		int pIndex = getIndex(tech, layer1.getIndex(), layer2.getIndex());
		double dist = currentRules.edgeList[pIndex].doubleValue();
		if (dist < 0) return null;
		return new Rule(dist, currentRules.edgeListRules[pIndex]);
	}

	/**
	 * Method to return the "wide" limit for a Technology.
	 * There are different design rules, depending on whether the geometry is wide or not.
	 * @param tech the Technology in question.
	 * @return the "wide" limit.  Anything this size or larger must use "wide" design rules.
	 */
	public static double getWideLimit(Technology tech)
	{
		cacheDesignRules(tech);
		return currentRules.wideLimit.doubleValue();
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
		cacheDesignRules(tech);
		int pIndex = getIndex(tech, layer1.getIndex(), layer2.getIndex());

		double bestDist = -1;
		String rule = null;
		if (connected)
		{
			double dist = currentRules.conList[pIndex].doubleValue();
			if (dist >= 0) { bestDist = dist;   rule = currentRules.conListRules[pIndex]; }
		} else
		{
			double dist = currentRules.unConList[pIndex].doubleValue();
			if (dist >= 0) { bestDist = dist;   rule = currentRules.unConListRules[pIndex]; }
		}

		if (wide)
		{
			if (connected)
			{
				double dist = currentRules.conListWide[pIndex].doubleValue();
				if (dist >= 0) { bestDist = dist;   rule = currentRules.conListWideRules[pIndex]; }
			} else
			{
				double dist = currentRules.unConListWide[pIndex].doubleValue();
				if (dist >= 0) { bestDist = dist;   rule = currentRules.unConListWideRules[pIndex]; }
			}
		}

		if (multiCut)
		{
			if (connected)
			{
				double dist = currentRules.conListMulti[pIndex].doubleValue();
				if (dist >= 0) { bestDist = dist;   rule = currentRules.conListMultiRules[pIndex]; }
			} else
			{
				double dist = currentRules.unConListMulti[pIndex].doubleValue();
				if (dist >= 0) { bestDist = dist;   rule = currentRules.unConListMultiRules[pIndex]; }
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
		cacheDesignRules(tech);
		int pIndex = getIndex(tech, layer1.getIndex(), layer2.getIndex());
		if (currentRules.conList[pIndex].doubleValue() >= 0) return true;
		if (currentRules.unConList[pIndex].doubleValue() >= 0) return true;
		if (currentRules.conListWide[pIndex].doubleValue() >= 0) return true;
		if (currentRules.unConListWide[pIndex].doubleValue() >= 0) return true;
		if (currentRules.conListMulti[pIndex].doubleValue() >= 0) return true;
		if (currentRules.unConListMulti[pIndex].doubleValue() >= 0) return true;
		if (currentRules.edgeList[pIndex].doubleValue() >= 0) return true;
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
		cacheDesignRules(tech);
		int index = layer.getIndex();
		double dist = currentRules.minWidth[index].doubleValue();
		if (dist < 0) return null;
		return new Rule(dist, currentRules.minWidthRules[index]);
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
	 * Method to cache the design rules for a given Technology.
	 * @param tech the Technology to examine.
	 */
	private static void cacheDesignRules(Technology tech)
	{
		if (tech == currentTechnology) return;
		currentTechnology = tech;
		currentRules = buildRules(currentTechnology);
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
	 * Method to build a Rules object that contains all of the design rules for a Technology.
	 * The DRC dialogs use this to hold the values while editing them.
	 * It also provides a cache for the design rule checker.
	 * @param tech the Technology to examine.
	 * @return a new Rules object with the design rules for this Technology.
	 */
	private static Rules buildRules(Technology tech)
	{
		Rules rules = new Rules();
		rules.techName = tech.getTechName();
		rules.numLayers = tech.getNumLayers();
		rules.uTSize = (rules.numLayers * rules.numLayers + rules.numLayers) / 2;
		Variable var = tech.getVar(WIDE_LIMIT, Double.class);
		if (var == null) rules.wideLimit = new Double(10); else
			rules.wideLimit = (Double)var.getObject();
		rules.layerNames = new String[rules.numLayers];

		int j=0;
		for(Iterator it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			rules.layerNames[j++] = layer.getName();
		}

		// minimum widths of each layer
		Variable minWidthVar = tech.getVar(MIN_WIDTH, Double[].class);
		if (minWidthVar != null) rules.minWidth = (Double[])minWidthVar.getObject(); else
		{
			rules.minWidth = new Double[rules.numLayers];
			for(int i=0; i<rules.numLayers; i++) rules.minWidth[i] = new Double(-1);
		}
		Variable minWidthRulesVar = tech.getVar(MIN_WIDTH_RULE, String[].class);
		if (minWidthRulesVar != null) rules.minWidthRules = (String[])minWidthRulesVar.getObject(); else
		{
			rules.minWidthRules = new String[rules.numLayers];
			for(int i=0; i<rules.numLayers; i++) rules.minWidthRules[i] = "";
		}

		// for connected layers
		Variable conListVar = tech.getVar(MIN_CONNECTED_DISTANCES, Double[].class);
		if (conListVar != null) rules.conList = (Double[])conListVar.getObject(); else
		{
			rules.conList = new Double[rules.uTSize];
			for(int i=0; i<rules.uTSize; i++) rules.conList[i] = new Double(-1);
		}
		Variable conListRulesVar = tech.getVar(MIN_CONNECTED_DISTANCES_RULE, String[].class);
		if (conListRulesVar != null) rules.conListRules = (String[])conListRulesVar.getObject(); else
		{
			rules.conListRules = new String[rules.uTSize];
			for(int i=0; i<rules.uTSize; i++) rules.conListRules[i] = "";
		}

		// for unconnected layers
		Variable unConListVar = tech.getVar(MIN_UNCONNECTED_DISTANCES, Double[].class);
		if (unConListVar != null) rules.unConList = (Double[])unConListVar.getObject(); else
		{
			rules.unConList = new Double[rules.uTSize];
			for(int i=0; i<rules.uTSize; i++) rules.unConList[i] = new Double(-1);
		}
		Variable unConListRulesVar = tech.getVar(MIN_UNCONNECTED_DISTANCES_RULE, String[].class);
		if (unConListRulesVar != null) rules.unConListRules = (String[])unConListRulesVar.getObject(); else
		{
			rules.unConListRules = new String[rules.uTSize];
			for(int i=0; i<rules.uTSize; i++) rules.unConListRules[i] = "";
		}

		// for connected layers that are wide
		Variable conListWideVar = tech.getVar(MIN_CONNECTED_DISTANCES_WIDE, Double[].class);
		if (conListWideVar != null) rules.conListWide = (Double[])conListWideVar.getObject(); else
		{
			rules.conListWide = new Double[rules.uTSize];
			for(int i=0; i<rules.uTSize; i++) rules.conListWide[i] = new Double(-1);
		}
		Variable conListWideRulesVar = tech.getVar(MIN_CONNECTED_DISTANCES_WIDE_RULE, String[].class);
		if (conListWideRulesVar != null) rules.conListWideRules = (String[])conListWideRulesVar.getObject(); else
		{
			rules.conListWideRules = new String[rules.uTSize];
			for(int i=0; i<rules.uTSize; i++) rules.conListWideRules[i] = "";
		}

		// for unconnected layers that are wide
		Variable unConListWideVar = tech.getVar(MIN_UNCONNECTED_DISTANCES_WIDE, Double[].class);
		if (unConListWideVar != null) rules.unConListWide = (Double[])unConListWideVar.getObject(); else
		{
			rules.unConListWide = new Double[rules.uTSize];
			for(int i=0; i<rules.uTSize; i++) rules.unConListWide[i] = new Double(-1);
		}
		Variable unConListWideRulesVar = tech.getVar(MIN_UNCONNECTED_DISTANCES_WIDE_RULE, String[].class);
		if (unConListWideRulesVar != null) rules.unConListWideRules = (String[])unConListWideRulesVar.getObject(); else
		{
			rules.unConListWideRules = new String[rules.uTSize];
			for(int i=0; i<rules.uTSize; i++) rules.unConListWideRules[i] = "";
		}

		// for connected layers that are multicut
		Variable conListMultiVar = tech.getVar(MIN_CONNECTED_DISTANCES_MULTI, Double[].class);
		if (conListMultiVar != null) rules.conListMulti = (Double[])conListMultiVar.getObject(); else
		{
			rules.conListMulti = new Double[rules.uTSize];
			for(int i=0; i<rules.uTSize; i++) rules.conListMulti[i] = new Double(-1);
		}
		Variable conListMultiRulesVar = tech.getVar(MIN_CONNECTED_DISTANCES_MULTI_RULE, String[].class);
		if (conListMultiRulesVar != null) rules.conListMultiRules = (String[])conListMultiRulesVar.getObject(); else
		{
			rules.conListMultiRules = new String[rules.uTSize];
			for(int i=0; i<rules.uTSize; i++) rules.conListMultiRules[i] = "";
		}

		// for unconnected layers that are multicut
		Variable unConListMultiVar = tech.getVar(MIN_UNCONNECTED_DISTANCES_MULTI, Double[].class);
		if (unConListMultiVar != null) rules.unConListMulti = (Double[])unConListMultiVar.getObject(); else
		{
			rules.unConListMulti = new Double[rules.uTSize];
			for(int i=0; i<rules.uTSize; i++) rules.unConListMulti[i] = new Double(-1);
		}
		Variable unConListMultiRulesVar = tech.getVar(MIN_UNCONNECTED_DISTANCES_MULTI_RULE, String[].class);
		if (unConListMultiRulesVar != null) rules.unConListMultiRules = (String[])unConListMultiRulesVar.getObject(); else
		{
			rules.unConListMultiRules = new String[rules.uTSize];
			for(int i=0; i<rules.uTSize; i++) rules.unConListMultiRules[i] = "";
		}

		// for edge distances between layers
		Variable edgeListVar = tech.getVar(MIN_EDGE_DISTANCES, Double[].class);
		if (edgeListVar != null) rules.edgeList = (Double[])edgeListVar.getObject(); else
		{
			rules.edgeList = new Double[rules.uTSize];
			for(int i=0; i<rules.uTSize; i++) rules.edgeList[i] = new Double(-1);
		}
		Variable edgeListRulesVar = tech.getVar(MIN_EDGE_DISTANCES_RULE, String[].class);
		if (edgeListRulesVar != null) rules.edgeListRules = (String[])edgeListRulesVar.getObject(); else
		{
			rules.edgeListRules = new String[rules.uTSize];
			for(int i=0; i<rules.uTSize; i++) rules.edgeListRules[i] = "";
		}

		rules.numNodes = tech.getNumNodes();
		rules.nodeNames = new String[rules.numNodes];
		rules.minNodeSize = new Double[rules.numNodes*2];
		rules.minNodeSizeRules = new String[rules.numNodes];
		j = 0;
		for(Iterator it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			rules.nodeNames[j] = np.getProtoName();
			rules.minNodeSize[j*2] = new Double(np.getMinWidth());
			rules.minNodeSize[j*2+1] = new Double(np.getMinHeight());
			rules.minNodeSizeRules[j] = np.getMinSizeRule();
			j++;
		}
		return rules;
	}

	/**
	 * Class to update primitive node information.
	 */
	protected static class NewDRCRules extends Job
	{
		Rules newRules;

		protected NewDRCRules(Rules newRules)
		{
			super("Update Design Rules", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.newRules = newRules;
			startJob();
		}

		public void doIt()
		{
			Technology tech = Technology.getCurrent();
			Rules oldRules = buildRules(tech);

			// update width-limit information
			if (newRules.wideLimit != oldRules.wideLimit)
				tech.newVar(WIDE_LIMIT, newRules.wideLimit);

			// update layer-to-layer information
			boolean conListChanged = false, conListRulesChanged = false;
			boolean unConListChanged = false, unConListRulesChanged = false;
			boolean conListWideChanged = false, conListWideRulesChanged = false;
			boolean unConListWideChanged = false, unConListWideRulesChanged = false;
			boolean conListMultiChanged = false, conListMultiRulesChanged = false;
			boolean unConListMultiChanged = false, unConListMultiRulesChanged = false;
			boolean edgeListChanged = false, edgeListRulesChanged = false;
			for(int i=0; i<newRules.uTSize; i++)
			{
				if (!newRules.conList[i].equals(oldRules.conList[i])) conListChanged = true;
				if (!newRules.conListRules[i].equals(oldRules.conListRules[i])) conListRulesChanged = true;
				if (!newRules.unConList[i].equals(oldRules.unConList[i])) unConListChanged = true;
				if (!newRules.unConListRules[i].equals(oldRules.unConListRules[i])) unConListRulesChanged = true;

				if (!newRules.conListWide[i].equals(oldRules.conListWide[i])) conListWideChanged = true;
				if (!newRules.conListWideRules[i].equals(oldRules.conListWideRules[i])) conListWideRulesChanged = true;
				if (!newRules.unConListWide[i].equals(oldRules.unConListWide[i])) unConListWideChanged = true;
				if (!newRules.unConListWideRules[i].equals(oldRules.unConListWideRules[i])) unConListWideRulesChanged = true;

				if (!newRules.conListMulti[i].equals(oldRules.conListMulti[i])) conListMultiChanged = true;
				if (!newRules.conListMultiRules[i].equals(oldRules.conListMultiRules[i])) conListMultiRulesChanged = true;
				if (!newRules.unConListMulti[i].equals(oldRules.unConListMulti[i])) unConListMultiChanged = true;
				if (!newRules.unConListMultiRules[i].equals(oldRules.unConListMultiRules[i])) unConListMultiRulesChanged = true;

				if (!newRules.edgeList[i].equals(oldRules.edgeList[i])) edgeListChanged = true;
				if (!newRules.edgeListRules[i].equals(oldRules.edgeListRules[i])) edgeListRulesChanged = true;
			}
			if (conListChanged) tech.newVar(MIN_CONNECTED_DISTANCES, newRules.conList);
			if (conListRulesChanged) tech.newVar(MIN_CONNECTED_DISTANCES_RULE, newRules.conListRules);
			if (unConListChanged) tech.newVar(MIN_UNCONNECTED_DISTANCES, newRules.unConList);
			if (unConListRulesChanged) tech.newVar(MIN_UNCONNECTED_DISTANCES_RULE, newRules.unConListRules);

			if (conListWideChanged) tech.newVar(MIN_CONNECTED_DISTANCES_WIDE, newRules.conListWide);
			if (conListWideRulesChanged) tech.newVar(MIN_CONNECTED_DISTANCES_WIDE_RULE, newRules.conListWideRules);
			if (unConListWideChanged) tech.newVar(MIN_UNCONNECTED_DISTANCES_WIDE, newRules.unConListWide);
			if (unConListWideRulesChanged) tech.newVar(MIN_UNCONNECTED_DISTANCES_WIDE_RULE, newRules.unConListWideRules);

			if (conListMultiChanged) tech.newVar(MIN_CONNECTED_DISTANCES_MULTI, newRules.conListMulti);
			if (conListMultiRulesChanged) tech.newVar(MIN_CONNECTED_DISTANCES_MULTI_RULE, newRules.conListMultiRules);
			if (unConListMultiChanged) tech.newVar(MIN_UNCONNECTED_DISTANCES_MULTI, newRules.unConListMulti);
			if (unConListMultiRulesChanged) tech.newVar(MIN_UNCONNECTED_DISTANCES_MULTI_RULE, newRules.unConListMultiRules);

			if (edgeListChanged) tech.newVar(MIN_EDGE_DISTANCES, newRules.edgeList);
			if (edgeListRulesChanged) tech.newVar(MIN_EDGE_DISTANCES_RULE, newRules.edgeListRules);

			// update per-layer information
			boolean minWidthChanged = false, minWidthRulesChanged = false;
			for(int i=0; i<newRules.numLayers; i++)
			{
				if (!newRules.minWidth[i].equals(oldRules.minWidth[i])) minWidthChanged = true;
				if (!newRules.minWidthRules[i].equals(oldRules.minWidthRules[i])) minWidthRulesChanged = true;
			}
			if (minWidthChanged) tech.newVar(MIN_WIDTH, newRules.minWidth);
			if (minWidthRulesChanged) tech.newVar(MIN_WIDTH_RULE, newRules.minWidthRules);

			// update per-node information
			int j = 0;
			for(Iterator it = tech.getNodes(); it.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)it.next();
				if (!newRules.minNodeSize[j*2].equals(oldRules.minNodeSize[j*2]) ||
					!newRules.minNodeSize[j*2+1].equals(oldRules.minNodeSize[j*2+1]) ||
					!newRules.minNodeSizeRules[j].equals(oldRules.minNodeSizeRules[j]))
				{
					np.setMinSize(newRules.minNodeSize[j*2].doubleValue(),
						newRules.minNodeSize[j*2+1].doubleValue(),
							newRules.minNodeSizeRules[j]);
				}
				j++;
			}

			// update the cache so that it points to the new set
			currentRules = newRules;
		}
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
}
