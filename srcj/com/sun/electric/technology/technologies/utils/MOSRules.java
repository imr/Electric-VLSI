/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MOSRules.java
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
package com.sun.electric.technology.technologies.utils;

import com.sun.electric.technology.*;
import com.sun.electric.database.text.TextUtils;

import java.util.Iterator;

/**
 * Class to define a complete set of design rules.
 * Includes constructors for initializing the data from a technology.
 */
public class MOSRules implements DRCRules {

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
	/** number of rules stored */                               private int      numberOfRules;
		/** DEFAULT null rule */                                private final static int MOSNORULE = -1;

	public MOSRules() {}
        
	public void setMinNodeSize(int index, double value)
	{
	   minNodeSize[index] = new Double(value);
	}

    public MOSRules(Technology tech)
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
			conList[i] = new Double(MOSNORULE);         conListRules[i] = "";
			unConList[i] = new Double(MOSNORULE);       unConListRules[i] = "";

			conListWide[i] = new Double(MOSNORULE);     conListWideRules[i] = "";
			unConListWide[i] = new Double(MOSNORULE);   unConListWideRules[i] = "";

			conListMulti[i] = new Double(MOSNORULE);    conListMultiRules[i] = "";
			unConListMulti[i] = new Double(MOSNORULE);  unConListMultiRules[i] = "";

			edgeList[i] = new Double(MOSNORULE);        edgeListRules[i] = "";
		}
		for(int i=0; i<numLayers; i++)
		{
			minWidth[i] = new Double(MOSNORULE);        minWidthRules[i] = "";
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

	/**
	 * Method to create a set of Design Rules from some simple spacing arrays.
	 * Used by simpler technologies that do not have full-sets of design rules.
	 * @param tech the Technology to load.
	 * @param conDist an upper-diagonal array of layer-to-layer distances (when connected).
	 * @param unConDist an upper-diagonal array of layer-to-layer distances (when unconnected).
	 * @return a set of design rules for the Technology.
	 */
	public static DRCRules makeSimpleRules(Technology tech, double [] conDist, double [] unConDist)
	{
		MOSRules rules = new MOSRules(tech);
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

    /**
	 * Method to find the worst spacing distance in the design rules.
	 * Finds the largest spacing rule in the Technology.
	 * @return the largest spacing distance in the Technology. Zero if nothing found
	 */
	public double getWorstSpacingDistance()
	{
		double worstInteractionDistance = 0;

		for(int i = 0; i < uTSize; i++)
		{
			double dist = unConList[i].doubleValue();
			if (dist > worstInteractionDistance) worstInteractionDistance = dist;
			dist = unConListWide[i].doubleValue();
			if (dist > worstInteractionDistance) worstInteractionDistance = dist;
			dist = unConListMulti[i].doubleValue();
			if (dist > worstInteractionDistance) worstInteractionDistance = dist;
		}
		return worstInteractionDistance;
	}

    /**
	 * Method to find the maximum design-rule distance around a layer.
	 * @param layer the Layer to examine.
	 * @return the maximum design-rule distance around the layer. -1 if nothing found.
	 */
	public double getMaxSurround(Technology tech, Layer layer, double maxSize)
	{
		double worstLayerRule = -1;
		int layerIndex = layer.getIndex();
		int tot = tech.getNumLayers();
	    double wide = wideLimit.doubleValue();

		for(int i=0; i<tot; i++)
		{
			int pIndex = tech.getLayerIndex(layerIndex, i);
			double dist = unConList[pIndex].doubleValue();
			if (dist > worstLayerRule) worstLayerRule = dist;
			if (maxSize > wide)
			{
				dist = unConListWide[pIndex].doubleValue();
				if (dist > worstLayerRule) worstLayerRule = dist;
			}
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
	public DRCRule getEdgeRule(Technology tech, Layer layer1, Layer layer2)
	{
		int pIndex = tech.getLayerIndex(layer1.getIndex(), layer2.getIndex());
		double dist = edgeList[pIndex].doubleValue();
		if (dist < 0) return null;
		return new DRCRule(dist, edgeListRules[pIndex]);
	}

     /**
	 * Method to find the spacing rule between two layer.
	 * @param layer1 the first layer.
	 * @param layer2 the second layer.
	 * @param connected true to find the distance when the layers are connected.
	 * @param multiCut true to find the distance when this is part of a multicut contact.
	 * @return the spacing rule between the layers.
	 * Returns null if there is no spacing rule.
	 */
	public DRCRules.DRCRule getSpacingRule(Technology tech, Layer layer1, Layer layer2, boolean connected,
                                           boolean multiCut, double wideS)
	{
		int pIndex = tech.getLayerIndex(layer1.getIndex(), layer2.getIndex());

		double bestDist = -1;
		String rule = null;
        if (connected)
        {
            double dist = conList[pIndex].doubleValue();
            if (dist >= 0) { bestDist = dist;   rule = conListRules[pIndex]; }
        } else
        {
            double dist = unConList[pIndex].doubleValue();
            if (dist >= 0) { bestDist = dist;   rule = unConListRules[pIndex]; }
         }

		if (wideS > wideLimit.doubleValue())
		{
			if (connected)
			{
				double dist = conListWide[pIndex].doubleValue();
				if (dist >= 0) { bestDist = dist;   rule = conListWideRules[pIndex]; }
			} else
			{
				double dist = unConListWide[pIndex].doubleValue();
				if (dist >= 0) { bestDist = dist;   rule = unConListWideRules[pIndex]; }
			}
		}

		if (multiCut)
		{
			if (connected)
			{
				double dist = conListMulti[pIndex].doubleValue();
				if (dist >= 0) { bestDist = dist;   rule = conListMultiRules[pIndex]; }
			} else
			{
				double dist = unConListMulti[pIndex].doubleValue();
				if (dist >= 0) { bestDist = dist;   rule = unConListMultiRules[pIndex]; }
			}
		}
		if (bestDist < 0) return null;
		return new DRCRules.DRCRule(bestDist, rule);
	}

    /**
     * Method to tell whether there are any design rules between two layers.
     * @param layer1 the first Layer to check.
     * @param layer2 the second Layer to check.
     * @return true if there are design rules between the layers.
     */
    public boolean isAnyRule(Technology tech, Layer layer1, Layer layer2)
    {
        int pIndex = tech.getLayerIndex(layer1.getIndex(), layer2.getIndex());
        if (conList[pIndex].doubleValue() >= 0) return true;
        if (unConList[pIndex].doubleValue() >= 0) return true;
        if (conListWide[pIndex].doubleValue() >= 0) return true;
        if (unConListWide[pIndex].doubleValue() >= 0) return true;
        if (conListMulti[pIndex].doubleValue() >= 0) return true;
        if (unConListMulti[pIndex].doubleValue() >= 0) return true;
        if (edgeList[pIndex].doubleValue() >= 0) return true;
        return false;
    }

	/**
	 * Method to retrieve total number of rules stored.
	 */
	public int getNumberOfRules()
	{
		return numberOfRules;
	}

	/**
	 * Method to calculate final number of rules
	 */
	public void calculateNumberOfRules()
	{
		int count = 0;
		for(int i = 0; i < uTSize; i++)
		{
			//Uncon
			if (unConList[i].doubleValue() > MOSNORULE) count++;
			if (unConListWide[i].doubleValue() > MOSNORULE) count++;;
			if (unConListMulti[i].doubleValue() > MOSNORULE) count++;
			// Con
			if (conList[i].doubleValue() > MOSNORULE) count++;
			if (conListWide[i].doubleValue() > MOSNORULE) count++;;
			if (conListMulti[i].doubleValue() > MOSNORULE) count++;
			// Edge rules
			if (edgeList[i].doubleValue() > MOSNORULE) count++;
		}
		for(int i=0; i<numLayers; i++)
		{
			if (minWidth[i].doubleValue() > MOSNORULE) count++;
		}
		for(int i=0; i<minNodeSize.length; i++)
		{
			if (minNodeSize[i].doubleValue() > MOSNORULE) count++;
		}
		numberOfRules = count;
	}

    /**
	 * Method to get the minimum <type> rule for a Layer
	 * where <type> is the rule type. E.g. MinWidth or Area
	 * @param layer the Layer to examine.
	 * @param type rule type
	 * @return the minimum width rule for the layer.
	 * Returns null if there is no minimum width rule.
	 */
    public DRCRules.DRCRule getMinValue(Layer layer, int type)
	{
	    if (type != DRCTemplate.MINWID) return (null);
	    
        int index = layer.getIndex();
        double dist = minWidth[index].doubleValue();

        if (dist < 0) return null;
		return (new DRCRules.DRCRule(dist, minWidthRules[index]));
	}

    /**
	 * Method to apply overrides to a set of rules.
	 * @param override the override string.
	 * @param tech the Technology in which these rules live.
	 */
	public void applyDRCOverrides(String override, Technology tech)
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
				Layer layer1 = Technology.getLayerFromOverride(override, startKey, '/', tech);
				if (layer1 == null) break;
				startKey = override.indexOf('/', startKey);
				if (startKey < 0) break;
				Layer layer2 = Technology.getLayerFromOverride(override, startKey+1, '=', tech);
				if (layer2 == null) break;
				startKey = override.indexOf('=', startKey);
				if (startKey < 0) break;
				endKey = override.indexOf(';', startKey);
				if (endKey < 0) break;
				String newValue = override.substring(startKey+1, endKey);
				int index = tech.getLayerIndex(layer1.getIndex(), layer2.getIndex());
				if (key.equals("c"))
				{
					conList[index] = new Double(TextUtils.atof(newValue));
				} else if (key.equals("cr"))
				{
					conListRules[index] = newValue;
				} else if (key.equals("u"))
				{
					unConList[index] = new Double(TextUtils.atof(newValue));
				} else if (key.equals("ur"))
				{
					unConListRules[index] = newValue;
				} else if (key.equals("cw"))
				{
					conListWide[index] = new Double(TextUtils.atof(newValue));
				} else if (key.equals("cwr"))
				{
					conListWideRules[index] = newValue;
				} else if (key.equals("uw"))
				{
					unConListWide[index] = new Double(TextUtils.atof(newValue));
				} else if (key.equals("uwr"))
				{
					unConListWideRules[index] = newValue;
				} else if (key.equals("cm"))
				{
					conListMulti[index] = new Double(TextUtils.atof(newValue));
				} else if (key.equals("cmr"))
				{
					conListMultiRules[index] = newValue;
				} else if (key.equals("um"))
				{
					unConListMulti[index] = new Double(TextUtils.atof(newValue));
				} else if (key.equals("umr"))
				{
					unConListMultiRules[index] = newValue;
				} else if (key.equals("e"))
				{
					edgeList[index] = new Double(TextUtils.atof(newValue));
				} else if (key.equals("er"))
				{
					edgeListRules[index] = newValue;
				}
				pos = endKey + 1;
				continue;
			}
			if (key.equals("m") || key.equals("mr"))
			{
				startKey = endKey + 1;
				Layer layer = Technology.getLayerFromOverride(override, startKey, '=', tech);
				if (layer == null) break;
				startKey = override.indexOf('=', startKey);
				if (startKey < 0) break;
				endKey = override.indexOf(';', startKey);
				if (endKey < 0) break;
				String newValue = override.substring(startKey+1, endKey);
				int index = layer.getIndex();
				if (key.equals("m"))
				{
					minWidth[index] = new Double(TextUtils.atof(newValue));
				} else if (key.equals("mr"))
				{
					minWidthRules[index] = newValue;
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
					minNodeSize[index*2] = new Double(TextUtils.atof(newValue1));
					minNodeSize[index*2+1] = new Double(TextUtils.atof(newValue2));
				} else if (key.equals("nr"))
				{
					startKey = override.indexOf('=', startKey);
					if (startKey < 0) break;
					endKey = override.indexOf(';', startKey);
					if (endKey < 0) break;
					String newValue = override.substring(startKey+1, endKey);
					minNodeSizeRules[index] = newValue;
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
                wideLimit = new Double(TextUtils.atof(newValue));
                pos = endKey + 1;
                continue;
			}

			// Skip this format
			endKey = override.indexOf(';', startKey);
			pos = endKey + 1;
		}
	}
}
