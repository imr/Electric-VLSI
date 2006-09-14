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

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.technology.DRCRules;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * Class to define a complete set of design rules.
 * Includes constructors for initializing the data from a technology.
 */
public class MOSRules implements DRCRules {

	/** the technology */                                       private Technology tech;
	/** number of layers in the technology */					public int       numLayers;
	/** size of upper-triangle of layers */						public int       uTSize;
	/** width limit that triggers wide rules */					public Double    wideLimit;
	/** names of layers */										public String [] layerNames;
	/** minimum width of layers */								public Double [] minWidth;
	/** minimum width rules */									public String [] minWidthRules;
	/** minimum distances when connected */						public Double [] conList;
	/** minimum distance ruless when connected */				public String [] conListRules;
	/** minimum distance ruless when connected */				public String [] conListNodes;
	/** minimum distances when unconnected */					public Double [] unConList;
	/** minimum distance rules when unconnected */				public String [] unConListRules;
	/** minimum distance rules when unconnected */				public String [] unConListNodes;
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
	/** minimum area rules */								    public Double [] minArea;
	/** minimum area rule names */							    public String [] minAreaRules;
	/** maximum slot size rules */								    public Double [] slotSize;
	/** maximum slot size rule names */							    public String [] slotSizeRules;

	/** number of nodes in the technology */					public int       numNodes;
	/** names of nodes */										public String [] nodeNames;
	/** minimim node size in the technology */					public Double [] minNodeSize;
	/** minimim node size rules */								public String [] minNodeSizeRules;
    /** cut size in the technology */				            public Double [] cutNodeSize;
	/** cut size rules */								        public String [] cutNodeSizeRules;
    /** cut 1D spacing in the technology */				            public Double [] cutNodeSpa1D;
	/** cut 1D s[acing rules */								        public String [] cutNodeSpa1DRules;
    /** cut 2D spacing in the technology */				            public Double [] cutNodeSpa2D;
    /** cut 2D s[acing rules */								        public String [] cutNodeSpa2DRules;
    /** Only 1 surround per contact node! **/
    /** cut surround in the technology */				            public Double [] cutNodeSurround;
	/** cut surround rules */								        public String [] cutNodeSurroundRules;
    /** poly overhang/surround along gate **/                   public double transPolyOverhang;
	/** number of rules stored */                               private int      numberOfRules;
	/** DEFAULT null rule */                                private final static int MOSNORULE = -1;

	public MOSRules() {}

    /**
     * Method to set minimum node size
     * @param index represents node, widths are stored in index*2 and height in index*2+j
     * @param name
     * @param width
     * @param height
     */
	private void setMinNodeSize(int index, String name, double width, double height)
	{
        minNodeSizeRules[index] = name;
        minNodeSize[index*2] = width; // autoboxing
        minNodeSize[index*2+1] = height; // autoboxing
	}

    /**
     *
     * @param index represents node, widths are stored in index*2 and height in index*2+j
     * @param when represents the foundry being used
     * @return a rule describing the minimum node size.
     */
    public DRCTemplate getMinNodeSize(int index, int when)
    {
        // That division by 2 might be a problem
        DRCTemplate rule = new DRCTemplate(minNodeSizeRules[index], when, DRCTemplate.DRCRuleType.NODSIZ, 0, 0, null, null,
                minNodeSize[index*2], -1);
        rule.value2 = minNodeSize[index*2+1]; // height
        return (rule);
    }

    public MOSRules(Technology tech)
	{
		// compute sizes
		numLayers = tech.getNumLayers();
		numNodes = tech.getNumNodes();
        int numIndices = numLayers + numNodes;
		uTSize = (numIndices * numIndices + numIndices) / 2;
        //uTSize = (numLayers * numLayers + numLayers) / 2;

		// initialize the width limit
		wideLimit = 0.0; // auto-boxing

		// add names
		this.tech = tech;
		layerNames = new String[numLayers];
		int j = 0;
		for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
			layerNames[j++] = layer.getName();
		}
		nodeNames = new String[numNodes];
		j = 0;
		for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = it.next();
			nodeNames[j++] = np.getName();
		}

		// allocate tables
		conList = new Double[uTSize];
		conListRules = new String[uTSize];
		conListNodes = new String[uTSize];
		unConList = new Double[uTSize];
		unConListRules = new String[uTSize];
		unConListNodes = new String[uTSize];

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

        minArea = new Double[numLayers];
        minAreaRules = new String[numLayers];

        slotSize = new Double[numLayers];
        slotSizeRules = new String[numLayers];

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
            minArea[i] = new Double(MOSNORULE);        minAreaRules[i] = "";
            slotSize[i] = new Double(MOSNORULE);        slotSizeRules[i] = "";
		}

		// build node size tables
		minNodeSize = new Double[numNodes*2];
		minNodeSizeRules = new String[numNodes];
        cutNodeSize = new Double[numNodes];
		cutNodeSizeRules = new String[numNodes];
        cutNodeSurround = new Double[numNodes];
		cutNodeSurroundRules = new String[numNodes];
        cutNodeSpa1D = new Double[numNodes];
		cutNodeSpa1DRules = new String[numNodes];
        cutNodeSpa2D = new Double[numNodes];
		cutNodeSpa2DRules = new String[numNodes];
		j = 0;
		for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			minNodeSize[j*2] = np.getMinWidth();  // autoboxing
			minNodeSize[j*2+1] = np.getMinHeight(); // autoboxing
			minNodeSizeRules[j] = np.getMinSizeRule();
            cutNodeSizeRules[j] = "";
            cutNodeSize[j] = new Double(MOSNORULE);
            cutNodeSurroundRules[j] = "";
            cutNodeSurround[j] = new Double(MOSNORULE);
            cutNodeSpa1DRules[j] = "";
            cutNodeSpa1D[j] = new Double(MOSNORULE);
            cutNodeSpa2DRules[j] = "";
            cutNodeSpa2D[j] = new Double(MOSNORULE);
			j++;
		}
	}

    /**
	 * Method to determine the index in the upper-left triangle array for two layers/nodes. This function
     * assumes no rules for primitives nor single layers are stored.
	 * @param index1 the first layer/node index.
	 * @param index2 the second layer/node index.
	 * @return the index in the array that corresponds to these two layers/nodes.
	 */
	public int getRuleIndex(int index1, int index2)
	{
		if (index1 > index2) { int temp = index1; index1 = index2;  index2 = temp; }
		int pIndex = (index1+1) * (index1/2) + (index1&1) * ((index1+1)/2);
		pIndex = index2 + (tech.getNumLayers()) * index1 - pIndex;
		return pIndex;
	}

    /**
     * Method to return overhang of poly in transistors along the gate
     * @return the overhang of poly in transistors along the gate.
     */
    public double getPolyOverhang()
    {
        return transPolyOverhang;
    }

    /**
     * Method to determine if given node is not allowed by foundry.
     * @param nodeIndex index of node in DRC rules map to examine.
     * @param type rule type.
     * @return true if given node is not allowed by foundry.
     */
    public boolean isForbiddenNode(int nodeIndex, DRCTemplate.DRCRuleType type)
    {
        return false;
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
				rules.conList[i] = (conDist[i]);  // autoboxing
			}
		}
		if (unConDist != null)
		{
			for(int i=0; i<unConDist.length; i++)
			{
				rules.unConList[i] = (unConDist[i]);   // autoboxing
			}
		}
        rules.calculateNumberOfRules();
        return rules;
	}

    /**
	 * Method to find the worst spacing distance in the design rules.
	 * Finds the largest spacing rule in the Technology.
	 * @return the largest spacing distance in the Technology. Zero if nothing found
     * @param lastMetal
     */
	public double getWorstSpacingDistance(int lastMetal)
	{
        assert(lastMetal == -1); // not implemented for only metals yet
        double worstInteractionDistance = 0;

		for(int i = 0; i < uTSize; i++)
		{
			double dist = unConList[i];
			if (dist > worstInteractionDistance) worstInteractionDistance = dist;
			dist = unConListWide[i];
			if (dist > worstInteractionDistance) worstInteractionDistance = dist;
			dist = unConListMulti[i];
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
	    double wide = wideLimit;

		for(int i=0; i<tot; i++)
		{
			int pIndex = getRuleIndex(layerIndex, i);
			double dist = unConList[pIndex];
			if (dist > worstLayerRule) worstLayerRule = dist;
			if (maxSize > wide)
			{
				dist = unConListWide[pIndex];
				if (dist > worstLayerRule) worstLayerRule = dist;
			}
		}

		return worstLayerRule;
	}

    /**
	 * Method to find the extension rule between two layer.
	 * @param layer1 the first layer.
     * @param layer2 the second layer.
     * @param isGateExtension to decide between the rule EXTENSIONGATE or EXTENSION
     * @return the extension rule between the layers.
     * Returns null if there is no extension rule.
	 */
	public DRCTemplate getExtensionRule(Layer layer1, Layer layer2, boolean isGateExtension)
	{
		return null; //  not available for CMOS
	}

    /**
	 * Method to find the edge spacing rule between two layer.
	 * @param layer1 the first layer.
     * @param layer2 the second layer.
     * @return the edge rule distance between the layers.
     * Returns null if there is no edge spacing rule.
	 */
	public DRCTemplate getEdgeRule(Layer layer1, Layer layer2)
	{
		int pIndex = getRuleIndex(layer1.getIndex(), layer2.getIndex());
		double dist = edgeList[pIndex];
		if (dist < 0) return null;
		return new DRCTemplate(edgeListRules[pIndex], DRCTemplate.DRCMode.ALL.mode(),
                DRCTemplate.DRCRuleType.SPACINGE, 0, 0, null, null, dist, -1);
	}

     /**
	 * Method to find the spacing rule between two layer.
	 * @param layer1 the first layer.
      * @param layer2 the second layer.
      * @param connected true to find the distance when the layers are connected.
      * @param multiCut 1 to find the distance when this is part of a multicut contact.
      * @param wideS widest polygon
      * @param length length of the intersection
      * @return the spacing rule between the layers.
      * Returns null if there is no spacing rule.
	 */
	public DRCTemplate getSpacingRule(Layer layer1, Geometric geo1,
                                      Layer layer2, Geometric geo2, boolean connected,
                                      int multiCut, double wideS, double length)
	{
		int pIndex = getRuleIndex(layer1.getIndex(), layer2.getIndex());
        String n1 = DRCTemplate.getSpacingCombinedName(layer1, geo1);
        String n2 = DRCTemplate.getSpacingCombinedName(layer2, geo2);
		double bestDist = -1;
		String rule = null;

        if (connected)
        {
            double dist = conList[pIndex];
            boolean validName = true;
            if (conListNodes[pIndex] != null &&
                    (!n1.equals(conListNodes[pIndex]) && !n2.equals(conListNodes[pIndex])))
                validName = false;
            if (validName && dist >= 0) { bestDist = dist;   rule = conListRules[pIndex]; }
        } else
        {
            double dist = unConList[pIndex];  // autoboxing
            boolean validName = true;
            if (unConListNodes[pIndex] != null &&
                    (!n1.equals(unConListNodes[pIndex]) && !n2.equals(unConListNodes[pIndex])))
                validName = false;
            if (validName && dist >= 0) { bestDist = dist;   rule = unConListRules[pIndex]; }
         }

		if (wideS > wideLimit)  // autoboxing
		{
			if (connected)
			{
				double dist = conListWide[pIndex]; // autoboxing
				if (dist >= 0) { bestDist = dist;   rule = conListWideRules[pIndex]; }
			} else
			{
				double dist = unConListWide[pIndex]; // autoboxing
				if (dist >= 0) { bestDist = dist;   rule = unConListWideRules[pIndex]; }
			}
		}

		if (multiCut == 1)
		{
			if (connected)
			{
				double dist = conListMulti[pIndex]; // autoboxing
				if (dist >= 0) { bestDist = dist;   rule = conListMultiRules[pIndex]; }
			} else
			{
				double dist = unConListMulti[pIndex]; // autoboxing
				if (dist >= 0) { bestDist = dist;   rule = unConListMultiRules[pIndex]; }
			}
		}
		if (bestDist < 0) return null;

		return new DRCTemplate(rule, DRCTemplate.DRCMode.ALL.mode(),
                DRCTemplate.DRCRuleType.SPACING, 0, 0, bestDist, multiCut);
	}

    /**
     * Method to tell whether there are any design rules between two layers.
     * @param layer1 the first Layer to check.
     * @param layer2 the second Layer to check.
     * @return true if there are design rules between the layers.
     */
    public boolean isAnySpacingRule(Layer layer1, Layer layer2)
    {
        int pIndex = getRuleIndex(layer1.getIndex(), layer2.getIndex());
        if (conList[pIndex] >= 0) return true;
        if (unConList[pIndex] >= 0) return true;
        if (conListWide[pIndex] >= 0) return true;
        if (unConListWide[pIndex] >= 0) return true;
        if (conListMulti[pIndex] >= 0) return true;
        if (unConListMulti[pIndex] >= 0) return true;
        if (edgeList[pIndex] >= 0) return true;
        return false;
    }

    /**
     * Method to tell UI if multiple wide rules are allowed.
     * @param index the index in the upper-diagonal table of layers.
     * @return true if multiple wide rules are allowed.
     */
    public boolean doesAllowMultipleWideRules(int index)
    {
        return (unConListWide[index] == MOSNORULE);
    }

	/**
	 * Method to retrieve total number of rules stored.
	 */
	public int getNumberOfRules()
	{
		return numberOfRules;
	}

    /**
     * To retrieve those nodes that have rules.
     * @return an array of nodes that have rules.
     */
    public String[] getNodesWithRules() {return nodeNames;}

    public void addRule(int index, DRCTemplate rule)
    {
        new Error("Not implemented");
    }
    /**
	 * Method to add a rule based on template
     @param index
      * @param rule
     * @param wideRules
     */
	public void addRule(int index, DRCTemplate rule, DRCTemplate.DRCRuleType spacingCase, boolean wideRules)
	{
        if (rule.ruleType == DRCTemplate.DRCRuleType.NODSIZ)
            setMinNodeSize(index, rule.ruleName, rule.value1, rule.value2);
        else
        {
            if (rule.value1 <= 0) rule.value1 = MOSNORULE;

            switch (rule.ruleType)
            {
                case CONSPA: // Connected
                {
                    switch (spacingCase)
                    {
                        case SPACING:
                        {
                            if (!wideRules)
                            {
                                conList[index] = (rule.value1); // autoboxing
                                conListRules[index] = rule.ruleName;
                                if (rule.maxWidth > 0) wideLimit = (rule.maxWidth); // autoboxing
                            }
                            else
                            {
                                conListWide[index] = (rule.value1); // autoboxing
                                conListWideRules[index] = rule.ruleName;
                                if (rule.maxWidth > 0) wideLimit = (rule.maxWidth);
                            }
                        }
                            break;
                        case UCONSPA2D:
                            conListMulti[index] = (rule.value1); // autoboxing
                            conListMultiRules[index] = rule.ruleName;
                            break;
                        case SPACINGE: // edge rules
                            edgeList[index] = (rule.value1); // autoboxing
                            edgeListRules[index] = rule.ruleName;
                            break;
                         default:
                            System.out.println("Error in MOSRules.setSpacingRules");
                    }
                }
                break;
                case UCONSPA: // Connected
                {
                    switch (spacingCase)
                    {
                        case SPACING:
                        {
                            if (!wideRules)
                            {
                                unConList[index] = (rule.value1); // autoboxing
                                unConListRules[index] = rule.ruleName;
                                if (rule.maxWidth > 0) wideLimit = (rule.maxWidth);// autoboxing
                            }
                            else
                            {
                                unConListWide[index] = (rule.value1); // autoboxing
                                unConListWideRules[index] = rule.ruleName;
                                if (rule.maxWidth > 0) wideLimit = (rule.maxWidth);// autoboxing
                            }
                            break;
                        }
                        case UCONSPA2D:
                            unConListMulti[index] = (rule.value1); // autoboxing
                            unConListMultiRules[index] = rule.ruleName;
                            break;
                        default:
                            System.out.println("Error in MOSRules.setSpacingRules");
                    }
                }
                break;
                default:
                    System.out.println("Error in MOSRules.setSpacingRules");
            }
        }
    }

    /**
     * Method to delete a given spacing rule
     * @param index
     * @param rule
     */
    public void deleteRule(int index, DRCTemplate rule)
    {
        // Reset the actual value
        conListWide[index] = new Double(MOSNORULE);
        conListWideRules[index] = "";
		unConListWide[index] = new Double(MOSNORULE);
        unConListWideRules[index] = "";
    }

    /**
     *
     * @param index
     * @param newRules
     * @param spacingCase SPACING for normal case, SPACINGW for wide case, CUTSPA for multi cuts
     * @param wideRules
     */
    public void setSpacingRules(int index, List<DRCTemplate> newRules, DRCTemplate.DRCRuleType spacingCase, boolean wideRules)
    {
        for (DRCTemplate rule : newRules)
        {
            addRule(index, rule, spacingCase, false);
        }
    }

    /**
     * Method to retrieve different spacing rules depending on type.
     * @param index the index of the layer being queried.
     * @param type SPACING (normal values), SPACINGW (wide values),
     * SPACINGE (edge values) and CUTSPA (multi cuts).
     * @param wideRules
     * @return list of rules subdivided in UCONSPA and CONSPA
     */
    public List<DRCTemplate> getSpacingRules(int index, DRCTemplate.DRCRuleType type, boolean wideRules)
    {
        List<DRCTemplate> list = new ArrayList<DRCTemplate>(2);

		// SMR changed the four lines listed below...widelimit should appear in the SPACINGW rule, not the SPACING rule
        switch (type)
        {
            case SPACING: // normal rules
            {
                if (!wideRules)
                {
                    double dist = conList[index];  // autoboxing
                    if (dist >= 0)
                        list.add(new DRCTemplate(conListRules[index], DRCTemplate.DRCMode.ALL.mode(),
                                DRCTemplate.DRCRuleType.CONSPA,
                                0, 0, null, null, dist, -1));
                    dist = unConList[index];  // autoboxing
                    if (dist >= 0)
                        list.add(new DRCTemplate(unConListRules[index], DRCTemplate.DRCMode.ALL.mode(),
                                DRCTemplate.DRCRuleType.UCONSPA,
                                0, 0, null, null, dist, -1));
                }
                else
                {
                    double dist = conListWide[index];  // autoboxing
                    if (dist >= 0)
                        list.add(new DRCTemplate(conListWideRules[index], DRCTemplate.DRCMode.ALL.mode(),
                                DRCTemplate.DRCRuleType.CONSPA,
                                wideLimit, 0, null, null, dist, -1)); // autoboxing
                    dist = unConListWide[index];
                    if (dist >= 0)
                        list.add(new DRCTemplate(unConListWideRules[index], DRCTemplate.DRCMode.ALL.mode(),
                                DRCTemplate.DRCRuleType.UCONSPA,
                                wideLimit, 0, null, null, dist, -1));    // autoboxing
                }
           }
           break;
           case UCONSPA2D: // multi contact rules
           {
                double dist = conListMulti[index];  // autoboxing
                if (dist >= 0)
                    list.add(new DRCTemplate(conListMultiRules[index], DRCTemplate.DRCMode.ALL.mode(),
                            DRCTemplate.DRCRuleType.CONSPA,
                            0, 0, null, null, dist, 1));
                dist = unConListMulti[index];  // autoboxing
                if (dist >= 0)
                    list.add(new DRCTemplate(unConListMultiRules[index], DRCTemplate.DRCMode.ALL.mode(),
                            DRCTemplate.DRCRuleType.UCONSPA,
                            0, 0, null, null, dist, 1));
           }
           break;
           case SPACINGE: // edge rules
           {
                double dist = edgeList[index];  // autoboxing
                if (dist >= 0)
                    list.add(new DRCTemplate(edgeListRules[index], DRCTemplate.DRCMode.ALL.mode(),
                            DRCTemplate.DRCRuleType.SPACINGE,
                            0, 0, null, null, dist, -1));
           }
           break;
           default:
                System.out.println("Error in MOSRules.getSpacingRules");
        }
        return (list);
    }

	/**
	 * Method to calculate final number of rules
	 */
	private void calculateNumberOfRules()
	{
		int count = 0;
        // autoboxing was applied below
		for(int i = 0; i < uTSize; i++)
		{
            //Uncon
			if (unConList[i] > MOSNORULE) count++;
			if (unConListWide[i] > MOSNORULE) count++;
			if (unConListMulti[i] > MOSNORULE) count++;
			// Con
			if (conList[i] > MOSNORULE) count++;
			if (conListWide[i] > MOSNORULE) count++;
			if (conListMulti[i] > MOSNORULE) count++;
			// Edge rules
			if (edgeList[i] > MOSNORULE) count++;
		}
		for(int i=0; i<numLayers; i++)
		{
			if (minWidth[i] > MOSNORULE) count++;
		}
		for(int i=0; i<minNodeSize.length; i++)
		{
			if (minNodeSize[i] > MOSNORULE) count++;
		}
		numberOfRules = count;
	}

    /**
	 * Method to get the minimum <type> rule for a Layer
	 * where <type> is the rule type. E.g. MinWidth or Area
	 * @param layer the Layer to examine.
	 * @param type rule type
	 * @return the minimum rule for the layer.
	 * Returns null if there is no minimum width rule.
	 */
    public DRCTemplate getMinValue(Layer layer, DRCTemplate.DRCRuleType type)
	{
        int index = layer.getIndex();
        switch(type)
        {
            case MINWID:
            {
                double dist = minWidth[index]; // autoboxing

                if (dist < 0) return null;
                return (new DRCTemplate(minWidthRules[index], DRCTemplate.DRCMode.ALL.mode(),
                        DRCTemplate.DRCRuleType.MINWID, 0, 0, null, null, dist, -1));
            }
            case MINAREA:
            {
                double dist = minArea[index]; // autoboxing

                if (dist < 0) return null;
                return (new DRCTemplate(minAreaRules[index], DRCTemplate.DRCMode.ALL.mode(),
                        DRCTemplate.DRCRuleType.MINAREA, 0, 0, null, null, dist, -1));
            }
            case SLOTSIZE:
            {
                double dist = slotSize[index]; // autoboxing

                if (dist < 0) return null;
                return (new DRCTemplate(slotSizeRules[index], DRCTemplate.DRCMode.ALL.mode(),
                        DRCTemplate.DRCRuleType.SLOTSIZE, 0, 0, null, null, dist, -1));
            }
        }

        return null;
	}

    /**
     * Method to retrieve simple layer or node rules
     * @param index the index of the layer or node
     * @param type the rule type.
     * @return the requested rule.
     */
    public DRCTemplate getRule(int index, DRCTemplate.DRCRuleType type)
    {
        switch(type)
        {
            case MINWID:
                double minSize = minWidth[index]; // autoboxing
                if (minSize < 0) return null;
                return (new DRCTemplate(minWidthRules[index], DRCTemplate.DRCMode.ALL.mode(),
                        type, 0, 0, null, null, minSize, -1));
            case UCONSPA2D:
                 double cutSpa = cutNodeSpa1D[index]; // autoboxing
                if (cutSpa < 0) return null;
                return (new DRCTemplate(cutNodeSpa1DRules[index], DRCTemplate.DRCMode.ALL.mode(),
                        type, 0, 0, null, null, cutSpa, -1));

        }
        return null;
    }

    /**
     * Method to retrieve specific rules stored per node that involve two layers
     * @param index the combined index of the two layers involved
     * @param type
     * @param nodeName name of the primitive
     * @return null
     */
    public DRCTemplate getRule(int index, DRCTemplate.DRCRuleType type, String nodeName)
    {
        new Error("not implemented");
        return null;
    }

    /**
     * Method to set the minimum <type> rule for a Layer
     * where <type> is the rule type. E.g. MinWidth or Area
     * @param layer the Layer to examine.
     * @param name the rule name
     * @param value the new rule value
     * @param type rule type
     */
    public void setMinValue(Layer layer, String name, double value, DRCTemplate.DRCRuleType type)
    {
        int index = layer.getIndex();
        if (value <= 0) value = MOSNORULE;

        switch (type)
        {
            case MINWID:
                minWidth[index] = (value); // autoboxing
                minWidthRules[index] = name;
                break;
            case MINAREA:
                minArea[index] = (value); // autoboxing
                minAreaRules[index] = name;
                break;
            default:
                System.out.println("Not implemented for " + type + " in MOSRules.setMinValue");
        }
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
				int index = getRuleIndex(layer1.getIndex(), layer2.getIndex());
				if (key.equals("c"))
				{
					conList[index] = TextUtils.atof(newValue);
				} else if (key.equals("cr"))
				{
					conListRules[index] = newValue;
				} else if (key.equals("u"))
				{
					unConList[index] = TextUtils.atof(newValue);
				} else if (key.equals("ur"))
				{
					unConListRules[index] = newValue;
				} else if (key.equals("cw"))
				{
					conListWide[index] = TextUtils.atof(newValue);
				} else if (key.equals("cwr"))
				{
					conListWideRules[index] = newValue;
				} else if (key.equals("uw"))
				{
					unConListWide[index] = TextUtils.atof(newValue);
				} else if (key.equals("uwr"))
				{
					unConListWideRules[index] = newValue;
				} else if (key.equals("cm"))
				{
					conListMulti[index] = TextUtils.atof(newValue);
				} else if (key.equals("cmr"))
				{
					conListMultiRules[index] = newValue;
				} else if (key.equals("um"))
				{
					unConListMulti[index] = TextUtils.atof(newValue);
				} else if (key.equals("umr"))
				{
					unConListMultiRules[index] = newValue;
				} else if (key.equals("e"))
				{
					edgeList[index] = TextUtils.atof(newValue);
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
					minWidth[index] = TextUtils.atof(newValue);
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
				for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
				{
					PrimitiveNode oNp = it.next();
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
					minNodeSize[index*2] = TextUtils.atof(newValue1);
					minNodeSize[index*2+1] = TextUtils.atof(newValue2);
				} else if (key.equals("nr"))
				{
					startKey = override.indexOf('=', startKey);
					if (startKey < 0) break;
					endKey = override.indexOf(';', startKey);
					if (endKey < 0) break;
					minNodeSizeRules[index] = override.substring(startKey+1, endKey);
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
                wideLimit = TextUtils.atof(newValue);
                pos = endKey + 1;
                continue;
			}

			// Skip this format
			endKey = override.indexOf(';', startKey);
			pos = endKey + 1;
		}
	}
}
