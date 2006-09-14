/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DRCRules.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.technology;

import com.sun.electric.database.geometry.Geometric;

import java.util.List;

/**
 * Interface for abstracting design rules.
 */
public interface DRCRules
{
    public int getRuleIndex(int index1, int index2);
    public double getWorstSpacingDistance(int lastMetal);
    public double getMaxSurround(Technology tech, Layer layer, double maxSize);
    public DRCTemplate getEdgeRule(Layer layer1, Layer layer2);
    public DRCTemplate getSpacingRule(Layer layer1, Geometric geo1,
                                      Layer layer2, Geometric geo2, boolean connected,
                                      int multiCut, double wideS, double length);
    public boolean isAnySpacingRule(Layer layer1, Layer layer2);
    public DRCTemplate getExtensionRule(Layer layer1, Layer layer2,
                                        boolean isGateExtension);
	public int getNumberOfRules();
    public DRCTemplate getMinValue(Layer layer, DRCTemplate.DRCRuleType type);

    /**
     * Method to retrieve simple layer or node rules
     * @param index the index of the layer or node
     * @param type the rule type.
     * @return the requested rule.
     */
    public DRCTemplate getRule(int index, DRCTemplate.DRCRuleType type);

    /**
     * Method to retrieve specific rules stored per node that involve two layers
     * @param index the combined index of the two layers involved
     * @param type
     * @param nodeName the name of the primitive
     * @return the requested rule.
     */
    public DRCTemplate getRule(int index, DRCTemplate.DRCRuleType type, String nodeName);

    public void setMinValue(Layer layer, String name, double value, DRCTemplate.DRCRuleType type);
    public void applyDRCOverrides(String override, Technology tech);
    public boolean isForbiddenNode(int nodeIndex, DRCTemplate.DRCRuleType type);
    
    /********************* For UI ***********************************/
    String[] getNodesWithRules();
    List<DRCTemplate> getSpacingRules(int index, DRCTemplate.DRCRuleType type, boolean wideRules);
    void addRule(int index, DRCTemplate rule);
    void addRule(int index, DRCTemplate rule, DRCTemplate.DRCRuleType spacingCase, boolean wideRules);
    void deleteRule(int index, DRCTemplate rule);
    void setSpacingRules(int index, List<DRCTemplate> newRules, DRCTemplate.DRCRuleType spacingCase, boolean wideRules);
    boolean doesAllowMultipleWideRules(int index);
}
