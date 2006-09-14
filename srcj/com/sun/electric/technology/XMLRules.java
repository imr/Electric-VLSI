/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: XMLRules.java
 * Written by Gilda Garreton, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems
 */
package com.sun.electric.technology;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.geometry.Geometric;

import java.util.*;

public class XMLRules implements DRCRules {

	/** Hash map to store rules per matrix index */                     public HashMap<XMLRules.XMLRule, XMLRules.XMLRule>[] matrix;
    /** To remeber the technology */                                    private Technology tech;

    public XMLRules (Technology tech)
    {
        int numLayers = tech.getNumLayers();
        int uTSize = (numLayers * numLayers + numLayers) / 2 + numLayers + tech.getNumNodes();

        this.matrix = new HashMap[uTSize];
        this.tech = tech;
    }

    /**
	 * Method to determine the index in the upper-left triangle array for two layers/nodes. In this type of rules,
     * the index starts after primitive nodes and single layers rules.
	 * @param index1 the first layer/node index.
	 * @param index2 the second layer/node index.
	 * @return the index in the array that corresponds to these two layers/nodes.
	 */
	public int getRuleIndex(int index1, int index2)
	{
		if (index1 > index2) { int temp = index1; index1 = index2;  index2 = temp; }
		int pIndex = (index1+1) * (index1/2) + (index1&1) * ((index1+1)/2);
		pIndex = index2 + (tech.getNumLayers()) * index1 - pIndex;
		return tech.getNumLayers() + tech.getNumNodes() + pIndex;
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
        return null;
    }

    /**
     * Method to tell UI if multiple wide rules are allowed
     * @param index
     * @return true if multiple wide riles are allowed.
     */
    public boolean doesAllowMultipleWideRules(int index) { return true; }

	/** Method to get total number of rules stored
	 */
	public int getNumberOfRules()
	{
        // This function is better not to be cached
        int numberOfRules = 0;

        for (HashMap<XMLRules.XMLRule, XMLRules.XMLRule> map : matrix)
        {
            if (map != null) numberOfRules++;
        }
		return numberOfRules;
	}

    /**
     * To retrieve those nodes whose have rules
     * @return Array of Strings
     */
    public String[] getNodesWithRules()
    {
        String[] nodesWithRules = new String[tech.getNumNodes()];
        int j = 0;
        for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = it.next();
			nodesWithRules[j++] = np.getName();
		}
        return nodesWithRules;
    }

    /**
     * Method to retrieve different spacing rules depending on spacingCase.
     * Type can be SPACING (normal values), SPACINGW (wide values),
     * SPACINGE (edge values) and CUTSPA (multi cuts)
     * @param index
     * @param spacingCase
     * @param wideRules
     * @return list of rules subdivided in UCONSPA and CONSPA
     */
    public List<DRCTemplate> getSpacingRules(int index, DRCTemplate.DRCRuleType spacingCase, boolean wideRules)
    {
        List<DRCTemplate> list = new ArrayList<DRCTemplate>(2);

        switch (spacingCase)
        {
            case SPACING: // normal rules
            {
                double maxLimit = 0;
                int multi = -1;

                if (wideRules)
                {
                    multi = 0;
                    maxLimit = Double.MAX_VALUE;
                }

                list = getRuleForRange(index, DRCTemplate.DRCRuleType.CONSPA, -1, multi, maxLimit, list);
                list = getRuleForRange(index, DRCTemplate.DRCRuleType.UCONSPA, -1, multi, maxLimit, list);
            }
            break;
            case UCONSPA2D: // multi contact rule
            {
//		        list = getRuleForRange(index, DRCTemplate.DRCRuleType.CONSPA, 1, -1, 0, list);
		        list = getRuleForRange(index, DRCTemplate.DRCRuleType.UCONSPA2D, 1, -1, 0, list);
            }
            break;
            default:
        }
        return (list);
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
        List<DRCTemplate> list = new ArrayList<DRCTemplate>(0);
        HashMap<XMLRules.XMLRule,XMLRules.XMLRule> map = matrix[index];

        // delete old rules
        for (DRCTemplate rule : newRules)
        {
            // Invalid data
            if (rule.value1 <= 0 || rule.ruleName == null || rule.ruleName.equals("")) continue;
            // first remove any possible rule in the map
            switch (spacingCase)
            {
                case SPACING: // normal rules
                {
                    double maxLimit = 0;
                    int multi = -1;

                    if (wideRules) // wide rules
                    {
                        multi = 0;
                        maxLimit = Double.MAX_VALUE;
                    }

                    list = getRuleForRange(index, rule.ruleType, -1, multi, maxLimit, list);
                }
                break;
                case UCONSPA2D: // multi contact rule
                {
                    list = getRuleForRange(index, rule.ruleType, 1, -1, 0, list);
                }
                break;
                default:
            }

            // No the most efficient algorithm
            for (DRCTemplate tmp : list)
                map.remove(tmp);
        }
        for (DRCTemplate rule : newRules)
        {
            // Invalid data
            if (rule.value1 <= 0 || rule.ruleName == null || rule.ruleName.equals("")) continue;

            addRule(index, rule);
        }
    }

     /**
     * Method to retrieve simple layer or node rules
     * @param index the index of the layer or node
     * @param type the rule type.
     * @return the requested rule.
     */
     public XMLRule getRule(int index, DRCTemplate.DRCRuleType type)
     {
         return (getRule(index, type, 0, 0, -1, null, null));
     }

    /**
     * Method to retrieve specific rules stored per node that involve two layers
     * @param index the combined index of the two layers involved
     * @param type
     * @param nodeName list containing the name of the primitive
     * @return DRCTemplate containing the rule
     */
    public XMLRule getRule(int index, DRCTemplate.DRCRuleType type, String nodeName)
    {
        List<String> nameList = new ArrayList<String>(1);
        nameList.add(nodeName);
        return (getRule(index, type, 0, 0, -1, nameList, null));
    }

    /**
	 * Method to get the minimum <type> rule for a Layer
	 * where <type> is the rule type. E.g. MinWidth or Area
	 * @param layer the Layer to examine.
	 * @param type rule type
	 * @return the minimum width rule for the layer.
	 * Returns null if there is no minimum width rule.
	 */
    public DRCTemplate getMinValue(Layer layer, DRCTemplate.DRCRuleType type)
	{
        int index = layer.getIndex();
		return (getRule(index, type));
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
        if (value <= 0)
        {
            System.out.println("Error: zero value in XMLRules:setMinValue");
            return;
        }
        XMLRule oldRule = getRule(index, type);
        HashMap<XMLRules.XMLRule,XMLRules.XMLRule> map = matrix[index];

        // Remove old rule first
        map.remove(oldRule);
        addRule(index, name, value, type, 0, 0, -1, DRCTemplate.DRCMode.ALL.mode(), null, null);
    }

    /**
     * Method similar to getRuleForRange() but only search based on range of widths
     * @param index
     * @param type
     * @param multiCut  -1 if don't care, 0 if no cuts, 1 if cuts
     * @param minW
     * @param maxW
     * @param list
     * @return List of DRC rules
     */
    private List<DRCTemplate> getRuleForRange(int index, DRCTemplate.DRCRuleType type, int multiCut, double minW, double maxW,
                                              List<DRCTemplate> list)
    {
        HashMap<XMLRules.XMLRule,XMLRules.XMLRule> map = matrix[index];

        if (list == null) list = new ArrayList<DRCTemplate>(2);
        if (map == null) return (list);

        for (XMLRule rule : map.values())
        {
            if (rule.ruleType == type)
            {
                // discard rules that are not valid for this particular tech mode (ST or TSMC)
//                if (rule.when != DRCTemplate.DRCMode.ALL.mode() && (rule.when&techMode) != techMode)
//                    continue;
                if (rule.multiCuts != -1 && rule.multiCuts != multiCut)
	                continue; // Cuts don't match
                if (rule.maxWidth <= maxW && rule.maxWidth > minW)
                    list.add(rule);
            }
        }
        return (list);
     }

     /**
     * Method to retrieve a rule based on type and max wide size
     * (valid for metal only)
     */
    private XMLRule getRule(int index, DRCTemplate.DRCRuleType type, double wideS, double length, int multiCut,
                            List<String> possibleNodeNames, List<String> layerNamesInOrder)
    {
        HashMap<XMLRules.XMLRule,XMLRules.XMLRule> map = matrix[index];
        if (map == null) return (null);

        XMLRule maxR = null;
        boolean searchFor = (wideS > 0);

        for (XMLRule rule : map.values())
        {
            if (rule.ruleType == type)
            {
                // discard rules that are not valid for this particular tech mode (ST or TSMC)
//                if (rule.when != DRCTemplate.DRCMode.ALL.mode() && (rule.when&techMode) != techMode)
//                    continue;
                // Needs multiCut values
                if (rule.multiCuts != -1 && rule.multiCuts != multiCut)
	                continue; // Cuts don't match
                // in case of spacing rules, we might need to match special names
                if (rule.nodeName != null && possibleNodeNames != null && !possibleNodeNames.contains(rule.nodeName))
                    continue; // No combination found
                // names in order do not match
                if (layerNamesInOrder != null && (!rule.name1.equals(layerNamesInOrder.get(0)) || !rule.name2.equals(layerNamesInOrder.get(1))))
                    continue;
                // First found is valid
                if (!searchFor) return (rule);
                if (rule.maxWidth < wideS && rule.minLength <= length &&
                        (maxR == null || (maxR.maxWidth < rule.maxWidth && maxR.minLength < rule.minLength)))
                {
                    maxR = rule;
                }
            }
        }
        return (maxR);
     }

    /**
     * To set wide limit for old techs
//     * @param values
     */
    public void setWideLimits(double[] values)
    {
        System.out.println("Review XMLRules::setWideLimits");

        for (HashMap<XMLRules.XMLRule,XMLRules.XMLRule> map : matrix)
        {
            if (map == null) continue;

            for (XMLRule rule : map.values())
            {
                if (rule.maxWidth > 0 && rule.maxWidth != values[0])
                    rule.maxWidth = values[0];
            }
        }
    }

    private void addXMLRule(int index, XMLRule rule)
    {
        HashMap<XMLRules.XMLRule,XMLRules.XMLRule> map = matrix[index];
        if (map == null)
        {
            map = new HashMap<XMLRules.XMLRule,XMLRules.XMLRule>();
            matrix[index] = map;
        }

       map.put(rule, rule);
    }

    private void addRule(int index, String name, double value, DRCTemplate.DRCRuleType type,
                         double maxW, double minLen, int multiCut, int when,
                         List nodes, String spacingNodeName)
    {
       XMLRule r = new XMLRule(name, value, type, maxW, minLen, multiCut, when, nodes, spacingNodeName);
       addXMLRule(index, r);
    }

    /**
     * Method to delete a given spacing rule
     * @param index
     * @param rule
     */
    public void deleteRule(int index, DRCTemplate rule)
    {
        HashMap<XMLRules.XMLRule,XMLRules.XMLRule> map = matrix[index];
        if (map == null) return; // no rule found

        for (XMLRule r : map.values())
        {
            if (r.ruleType == rule.ruleType && r.maxWidth == rule.maxWidth &&
                r.minLength == rule.minLength && r.multiCuts == rule.multiCuts &&
                r.ruleName.equals(rule.ruleName))
            {
                // found element to delete
                map.remove(r);
                return;
            }
        }
    }

    /** OLD FUNCTION*/
    public void addRule(int index, DRCTemplate rule, DRCTemplate.DRCRuleType spacingCase, boolean wideRules)
    {
        new Error("Not implemented");
    }
    /**
	 * Method to add a rule based on template
	 * @param index
     * @param rule
     */
	public void addRule(int index, DRCTemplate rule)
	{
		DRCTemplate.DRCRuleType internalType = rule.ruleType;
        List<Layer> list = null;

        // This is only required for this type of rule
        if (rule.ruleType == DRCTemplate.DRCRuleType.EXTENSION || rule.ruleType == DRCTemplate.DRCRuleType.EXTENSIONGATE)
        {
            list = new ArrayList<Layer>(2);
            list.add(tech.findLayer(rule.name1));
            list.add(tech.findLayer(rule.name2));
        }

		switch (rule.ruleType)
		{
//            case NODSIZ:
//                    addRule(index*2, rule.ruleName, rule.value1, DRCTemplate.DRCRuleType.NODSIZ, 0, 0, -1, DRCTemplate.DRCMode.ALL.mode(), null, null);
//                    addRule(index*2+1, rule.ruleName, rule.value2, DRCTemplate.DRCRuleType.NODSIZ, 0, 0, -1, DRCTemplate.DRCMode.ALL.mode(), null, null);
//                    return;
			case SPACING:
//		    case SPACINGW:
				internalType = DRCTemplate.DRCRuleType.UCONSPA;
				addRule(index, rule.ruleName, rule.value1, DRCTemplate.DRCRuleType.CONSPA, rule.maxWidth, rule.minLength,
                        rule.multiCuts, rule.when, list, rule.nodeName);
				break;
            default:
                XMLRule r = new XMLRule(rule);
                addXMLRule(index, r);
                return;
        }
		addRule(index, rule.ruleName, rule.value1, internalType, rule.maxWidth, rule.minLength, rule.multiCuts, rule.when, list, rule.nodeName);
	}

     /**
      * Method to return min value rule depending on type and wire length
      */
    private double getMinRule(int index, DRCTemplate.DRCRuleType type, double maxW)
    {
        HashMap<XMLRules.XMLRule, XMLRules.XMLRule> map = matrix[index];
        if (map == null) return (0.0);

        for (XMLRule rule : map.values())
        {
            if (rule.ruleType == type && rule.maxWidth <= maxW)
                return (rule.value1);
        }
        return (0.0);
    }

	/**
	 * Method to find the spacing rule between two layer.
	 * @param layer1 the first layer.
     * @param layer2 the second layer.
     * @param connected true to find the distance when the layers are connected.
     * @param multiCut true to find the distance when this is part of a multicut contact.
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
		DRCTemplate.DRCRuleType type = (connected) ? DRCTemplate.DRCRuleType.CONSPA : DRCTemplate.DRCRuleType.UCONSPA;

        // Composing possible name if
        String n1 = null, n2 = null;
        if (geo1 != null) n1 = DRCTemplate.getSpacingCombinedName(layer1, geo1);
        if (geo2 != null) n2 = DRCTemplate.getSpacingCombinedName(layer2, geo2);
        List<String> list = new ArrayList<String>(2);
        list.add(n1); list.add(n2);

		XMLRule r = getRule(pIndex, type, wideS, length, multiCut, list, null);

        // Search for surrounding conditions not attached to nodes
//        if (r == null)
//        {
//            r = getRule(pIndex, DRCTemplate.DRCRuleType.SURROUND, wideS, length, multiCut, null, null);
//            if (r != null && r.nodeName != null) r = null; // only spacing rule if not associated to primitive nodes.
//        }

        return (r);
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
		int pIndex = getRuleIndex(layer1.getIndex(), layer2.getIndex());
        List<String> list = new ArrayList<String>(2);
        list.add(layer1.getName());
        list.add(layer2.getName());
        DRCTemplate.DRCRuleType rule = (isGateExtension) ? DRCTemplate.DRCRuleType.EXTENSIONGATE : DRCTemplate.DRCRuleType.EXTENSION;
        return (getRule(pIndex, rule, 0, 0, -1, null, list));
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
        HashMap<XMLRules.XMLRule, XMLRules.XMLRule> map = matrix[pIndex];
        if (map == null) return false;
        for (XMLRule rule : map.values())
        {
            if (rule.isSpacingRule()) return true;
        }
        return false;
    }

    /**
     * Method to determine if given node is not allowed by foundry
     * @param nodeIndex index of node in DRC rules map to examine
     * @param type rule type
     * @return true if this is a forbidden node
     */
    public boolean isForbiddenNode(int nodeIndex, DRCTemplate.DRCRuleType type)
    {
        HashMap<XMLRules.XMLRule, XMLRules.XMLRule> map = matrix[nodeIndex];
        if (map == null) return (false);

        for (XMLRule rule : map.values())
        {
            if (rule.ruleType == type)
            {
                // discard rules that are not valid for this particular tech mode (ST or TSMC)
//                if (rule.when != DRCTemplate.DRCMode.ALL.mode() && (rule.when&techMode) != techMode)
//                    continue;
                return true; // found
            }
        }
        // nothing found
        return false;
    }

    /**
	 * Method to find the worst spacing distance in the design rules.
	 * Finds the largest spacing rule in the Technology.
	 * @return the largest spacing distance in the Technology. Zero if nothing found
     * @param lastMetal last metal to check if only metal values are requested
     */
    public double getWorstSpacingDistance(int lastMetal)
	{
		double worstDistance = 0;

        if (lastMetal != -1)
        {
            int numM = tech.getNumMetals();
            assert(numM >= lastMetal);
            numM = lastMetal;
            List<Layer> layers = new ArrayList<Layer>(numM);
            for (Iterator<Layer> itL = tech.getLayers(); itL.hasNext();)
            {
                Layer l = itL.next(); // skipping pseudo layers
                if (l != l.getNonPseudoLayer())
                    continue;
                if (l.getFunction().isMetal())
                    layers.add(l);
            }
            for (int i = 0; i < numM; i++)
            {
                Layer l1 = layers.get(i);
                for (int j = i; j < numM; j++) // starts from i so metal1-metal2(default one) can be checked
                {
                    int index = getRuleIndex(l1.getIndex(), layers.get(j).getIndex());
                    double worstValue = getMinRule(index, DRCTemplate.DRCRuleType.UCONSPA, Double.MAX_VALUE);
                    if (worstValue > worstDistance) worstDistance = worstValue;
                }
            }
        }
        else
        {
            for(int i = 0; i < matrix.length; i++)
            {
                double worstValue = getMinRule(i, DRCTemplate.DRCRuleType.UCONSPA, Double.MAX_VALUE);
                if (worstValue > worstDistance) worstDistance = worstValue;
            }
        }
        return worstDistance;
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

		for(int i=0; i<tot; i++)
		{
			int pIndex = getRuleIndex(layerIndex, i);
            double dist = getMinRule(pIndex, DRCTemplate.DRCRuleType.UCONSPA, maxSize);
			if (dist > worstLayerRule) worstLayerRule = dist;
		}

		return worstLayerRule;
	}

    /**
	 * Method to apply overrides to a set of rules.
	 * @param override the override string.
	 * @param tech the Technology in which these rules live.
	 */
	public void applyDRCOverrides(String override, Technology tech)
	{
       // if (Main.getDebug()) System.out.println("Check this function"); @TODO GVG tsmc90:applyDRCOverrides
        //@TODO check DRCCheckMode.ALL

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
                    XMLRule rule = getRule(index,  DRCTemplate.DRCRuleType.CONSPA);
                    if (rule != null) rule.value1 = TextUtils.atof(newValue);
				} else if (key.equals("cr"))
				{
                    XMLRule rule = getRule(index,  DRCTemplate.DRCRuleType.CONSPA);
                    if (rule != null) rule.ruleName = newValue;
				} else if (key.equals("u"))
				{
                    XMLRule rule = getRule(index,  DRCTemplate.DRCRuleType.UCONSPA);
                    if (rule != null) rule.value1 = TextUtils.atof(newValue);
				} else if (key.equals("ur"))
				{
                    XMLRule rule = getRule(index,  DRCTemplate.DRCRuleType.UCONSPA);
                    if (rule != null) rule.ruleName = newValue;
				} else if (key.equals("cw"))
				{
					//conListWide[index] = new Double(TextUtils.atof(newValue));
                    System.out.println("Not implemented in XMLRules:applyDRCOverrides");
				} else if (key.equals("cwr"))
				{
					//conListWideRules[index] = newValue;
                    System.out.println("Not implemented in XMLRules:applyDRCOverrides");
				} else if (key.equals("uw"))
				{
					//unConListWide[index] = new Double(TextUtils.atof(newValue));
                    System.out.println("Not implemented in XMLRules:applyDRCOverrides");
				} else if (key.equals("uwr"))
				{
					//unConListWideRules[index] = newValue;
                    System.out.println("Not implemented in XMLRules:applyDRCOverrides");
				} else if (key.equals("cm"))
				{
					//conListMulti[index] = new Double(TextUtils.atof(newValue));
                    System.out.println("Not implemented in XMLRules:applyDRCOverrides");
				} else if (key.equals("cmr"))
				{
					//conListMultiRules[index] = newValue;
                    System.out.println("Not implemented in XMLRules:applyDRCOverrides");
				} else if (key.equals("um"))
				{
					//unConListMulti[index] = new Double(TextUtils.atof(newValue));
                    System.out.println("Not implemented in XMLRules:applyDRCOverrides");
				} else if (key.equals("umr"))
				{
					//unConListMultiRules[index] = newValue;
                    System.out.println("Not implemented in XMLRules:applyDRCOverrides");
				} else if (key.equals("e"))
				{
					//edgeList[index] = new Double(TextUtils.atof(newValue));
                    System.out.println("Not implemented in XMLRules:applyDRCOverrides");
				} else if (key.equals("er"))
				{
					//edgeListRules[index] = newValue;
                    System.out.println("Not implemented in XMLRules:applyDRCOverrides");
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
                    XMLRule rule = getRule(index,  DRCTemplate.DRCRuleType.MINWID);
                    if (rule != null) rule.value1 = TextUtils.atof(newValue);
				} else if (key.equals("mr"))
				{
                    XMLRule rule = getRule(index,  DRCTemplate.DRCRuleType.MINWID);
                    if (rule != null) rule.ruleName = newValue;
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
//				int index = 0;
				for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
				{
					PrimitiveNode oNp = it.next();
					if (oNp == np) break;
//					index++;
				}
				if (key.equals("n"))
				{
					startKey = override.indexOf('=', startKey);
					if (startKey < 0) break;
					endKey = override.indexOf('/', startKey);
					if (endKey < 0) break;
//					String newValue1 = override.substring(startKey+1, endKey);
					int otherEndKey = override.indexOf(';', startKey);
					if (otherEndKey < 0) break;
//					String newValue2 = override.substring(endKey+1, otherEndKey);
//                    setMinNodeSize(index*2, "NODSIZE", TextUtils.atof(newValue1), TextUtils.atof(newValue2));
//                    setMinNodeSize(index*2+1, TextUtils.atof(newValue2));
				} else if (key.equals("nr"))
				{
					startKey = override.indexOf('=', startKey);
					if (startKey < 0) break;
					endKey = override.indexOf(';', startKey);
					if (endKey < 0) break;
//					String newValue = override.substring(startKey+1, endKey);
                    System.out.println("No implemented in TSMRules");
//                    setMinNodeSize(index, TextUtils.atof(newValue));
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
			    //rules.wideLimit = new Double(TextUtils.atof(newValue));
				double value = TextUtils.atof(newValue);
				if (value > 0) setWideLimits(new double[] {value});
                pos = endKey + 1;
                continue;
			}

			/*
			if (key.equals("W"))
			{
				startKey = endKey + 1;
				// Getting the number of wide values
				//endKey = override.indexOf('[', startKey);
				startKey = override.indexOf('[', endKey) + 1;
				endKey = override.indexOf(']', startKey);
				StringTokenizer parse = new StringTokenizer(override.substring(startKey, endKey));
				if (endKey < 0) break;

				try
				{
					while (parse.hasMoreElements())
					{
						String val = parse.nextToken(",");
						double value = TextUtils.atof(val);
						if (value > 0)
							setWideLimits(new double[] {value});
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				//String newValue = override.substring(startKey, endKey);
				//wideLimit = new Double(TextUtils.atof(newValue));
				pos = endKey + 2;
				continue;
			}
			*/
			// Skip this format
			endKey = override.indexOf(';', startKey);
			pos = endKey + 1;
		}
	}


    public void loadDRCRules(Technology tech, Foundry foundry, DRCTemplate theRule)
    {
        int numMetals = tech.getNumMetals();
        DRCTemplate.DRCMode m = DRCTemplate.DRCMode.valueOf("M"+numMetals);

        // load the DRC tables from the explanation table
        int when = theRule.when;

        if (when != DRCTemplate.DRCMode.ALL.mode())
        {
            // New calculation
            boolean newValue = true;
            // Check all possibles foundries for this particular technology
            for (Foundry.Type t : Foundry.Type.values())
            {
                // The foundry is present but is not the choosen one, then invalid rule
                if (t == Foundry.Type.NONE) continue;

                if ((when&t.mode()) != 0 && foundry.getType() != t)
                    newValue = false;
                if (!newValue) break;
            }
            boolean oldValue = true;
            // One of the 2 is present. Absence means rule is valid for both
            if ((when&Foundry.Type.ST.mode()) != 0 && foundry.getType() == Foundry.Type.TSMC)
                oldValue = false;
            else if ((when&Foundry.Type.TSMC.mode()) != 0 && foundry.getType() == Foundry.Type.ST)
                oldValue = false;
            if(oldValue != newValue)
                System.out.println("H");
            if (!oldValue)
                return; // skipping this rule
        }

        // Skipping metal rules if there is no match in number of metals
        // Add M2/M3/M4/M5/M6
        // @TOD CHECK THIS METAL CONDITION
        if ((when&(DRCTemplate.DRCMode.M7.mode()|DRCTemplate.DRCMode.M8.mode()|DRCTemplate.DRCMode.M5.mode()|DRCTemplate.DRCMode.M6.mode())) != 0)
        {
            if ((when&m.mode()) == 0)
                return;
        }

        // find the layer or primitive names
        Layer lay1 = null;
        int index1 = -1;
        if (theRule.name1 != null)
        {
            lay1 = tech.findLayer(theRule.name1);
            if (lay1 == null)
                index1 = tech.getRuleNodeIndex(theRule.name1);
            else
                index1 = lay1.getIndex();
            if (index1 == -1)
            {
                System.out.println("Warning: no layer '" + theRule.name1 + "' in " +
                        tech.getTechDesc());
                return;
            }
        }
        Layer lay2 = null;
        int index2 = -1;
        if (theRule.name2 != null)
        {
            lay2 = tech.findLayer(theRule.name2);
            if (lay2 == null)
                index2 = tech.getRuleNodeIndex(theRule.name2);
            else
                index2 = lay2.getIndex();
            if (index2 == -1)
            {
                System.out.println("Warning: no layer '" + theRule.name2 + "' in " +
                        tech.getTechDesc());
                return;
            }
        }

        // find the index in a two-layer upper-diagonal table
        int index = -1;
        if (index1 >= 0 && index2 >= 0)
            index = getRuleIndex(index1, index2);

        // get more information about the rule
        double distance = theRule.value1;

        // find the nodes and arcs associated with the rule
        PrimitiveNode nty = null;
        ArcProto aty = null;
        if (theRule.nodeName != null)
        {
            if (theRule.ruleType == DRCTemplate.DRCRuleType.ASURROUND)
            {
                aty = tech.findArcProto(theRule.nodeName);
                if (aty == null)
                {
                    System.out.println("Warning: no arc '" + theRule.nodeName + "' in mocmos technology");
                    return;
                }
            } else if (theRule.ruleType != DRCTemplate.DRCRuleType.SPACING) // Special case with spacing rules
            {
                nty = tech.findNodeProto(theRule.nodeName);
                if (nty == null)
                {
                    System.out.println("Warning: no node '" + theRule.nodeName + "' in " +
                            tech.getTechDesc());
                    return;
                }
            }
        }

        // set the rule
        double [] specValues;
        switch (theRule.ruleType)
        {
            case MINWID:
                tech.setLayerMinWidth(theRule.name1, theRule.ruleName, distance);
                addRule(index1, theRule);
                break;
            case FORBIDDEN:
                addRule(nty.getPrimNodeIndexInTech(), theRule);
                break;
            case MINAREA:
            case MINENCLOSEDAREA:
            case EXTENSION:
            case EXTENSIONGATE:
                if (index == -1)
                    addRule(index1, theRule);
                else
                    addRule(index, theRule);
                break;
            case SPACING:
//            case SPACINGW:
            case CONSPA:
            case UCONSPA:
            case UCONSPA2D:
            case COMBINATION:
                addRule(index, theRule);
                break;
            case CUTSURX:
                specValues = nty.getSpecialValues();
                specValues[2] = distance;
                assert(false);
                break;
            case CUTSURY:
                specValues = nty.getSpecialValues();
                specValues[3] = distance;
                assert(false);
                break;
            case NODSIZ:
                addRule(nty.getPrimNodeIndexInTech(), theRule);
                break;
            case SURROUND:
                addRule(index, theRule);
                break;
            case ASURROUND:
                tech.setArcLayerSurroundLayer(aty, lay1, lay2, distance);
                break;
            default:
                assert(false);
                System.out.println("Rule " +  theRule.ruleName + " type " + theRule.ruleType +
                        " not implemented in " + tech.getTechDesc());
        }
    }

    public void resizeContact(PrimitiveNode contact, Technology.NodeLayer cutNode, Technology.NodeLayer cutSurNode)
    {
        DRCTemplate cutSize = getRule(cutNode.getLayer().getIndex(), DRCTemplate.DRCRuleType.MINWID); // min and max for vias
        int index = getRuleIndex(cutSurNode.getLayer().getIndex(), cutNode.getLayer().getIndex());
        DRCTemplate cutSur = getRule(index, DRCTemplate.DRCRuleType.SURROUND);

        assert(cutSize != null); assert(cutSur != null);

        index = getRuleIndex(cutNode.getLayer().getIndex(), cutNode.getLayer().getIndex());
        DRCTemplate spacing1D = getRule(index, DRCTemplate.DRCRuleType.UCONSPA);
        assert(spacing1D != null);
        DRCTemplate spacing2D = getRule(index, DRCTemplate.DRCRuleType.UCONSPA2D);
        if (spacing2D == null) spacing2D = spacing1D;

        DRCTemplate minNode = getRule(contact.getPrimNodeIndexInTech(), DRCTemplate.DRCRuleType.NODSIZ);
        tech.setDefNodeSize(contact, minNode.value1, minNode.value1);

        SizeOffset so = contact.getProtoSizeOffset();
        double value = cutSur.value1 + so.getHighXOffset();
        cutNode.setPoints(Technology.TechPoint.makeIndented(value));
        contact.setSpecialValues(new double [] {cutSize.value1, cutSize.value1,
                                                cutSur.value1, cutSur.value1,
                                                spacing1D.value1, spacing2D.value1});
//        contact.setMinSize(minNode.value1, minNode.value1, minNode.ruleName);
    }

    /**
     * Common resize function for well and active contacts
     * @param contacts array of contacts to resize
     */
    public void resizeContactsWithActive(PrimitiveNode[] contacts)
    {

        for (PrimitiveNode contact : contacts)
        {
            Technology.NodeLayer cutNode = contact.getLayers()[4]; // Cut
            Technology.NodeLayer activeNode = contact.getLayers()[1]; // active
            resizeContact(contact, cutNode, activeNode);

            Technology.NodeLayer wellNode = contact.getLayers()[2]; // well
            Technology.NodeLayer sellNode = contact.getLayers()[3]; // select

            // setting well-active actSurround
            int index = getRuleIndex(activeNode.getLayer().getIndex(), wellNode.getLayer().getIndex());
            DRCTemplate actSurround = getRule(index, DRCTemplate.DRCRuleType.SURROUND, contact.getName());

            index = getRuleIndex(activeNode.getLayer().getIndex(), sellNode.getLayer().getIndex());
            DRCTemplate selSurround = getRule(index, DRCTemplate.DRCRuleType.SURROUND, contact.getName());

            assert(actSurround != null); assert(selSurround != null);

            SizeOffset so = contact.getProtoSizeOffset();
            double value = so.getHighXOffset() - actSurround.value1;
            wellNode.setPoints(Technology.TechPoint.makeIndented(value));

            value = so.getHighXOffset() - selSurround.value1;
            sellNode.setPoints(Technology.TechPoint.makeIndented(value));
        }
    }

    /*******************************************/
    /*** Local class to store information ******/
    public static class XMLRule extends DRCTemplate
	{
//        List nodes;

        public XMLRule(DRCTemplate rule)
        {
            super(rule);
        }

        public XMLRule(String name, double value, DRCRuleType type, double maxW, double minLen, int multiCuts, int when,
                        List nodes, String nodeName)
		{
            super(name, when, type, maxW, minLen, null, null, value, multiCuts);
            if (this.nodeName != null) new Error("here is not ok");
            this.nodeName = nodeName;
//            this.nodes = nodes;
		}

        public boolean isSpacingRule()
        { return ruleType == DRCTemplate.DRCRuleType.CONSPA || ruleType == DRCTemplate.DRCRuleType.UCONSPA; }

        public boolean equals(Object obj)
		{
			// reflexive
			if (obj == this) return true;

			// should consider null case
			// symmetry but violates transitivity?
			// It seems Map doesn't provide obj as PolyNode
			if (!(obj instanceof XMLRule))
				return obj.equals(this);

			XMLRule a = (XMLRule)obj;
            boolean basic = ruleName.equals(a.ruleName) && ruleType == a.ruleType;
            if (basic) // checking node names as well
                basic = nodeName == null || nodeName.equals(a.nodeName);
            if (basic) // checking the layerNames
                basic = name1 == null || name1.equals(a.name1);
            if (basic)
                basic = name2 == null || name2.equals(a.name2);
            return (basic);
		}
		public int hashCode()
		{
			return ruleType.mode();
		}
	}
}
