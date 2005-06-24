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

import java.util.List;

/**
 * Interface for abstracting design rules.
 */
public interface DRCRules
{
//	public void setMinNodeSize(int index, String name, double width, double height);
	public double getWorstSpacingDistance();
    public double getMaxSurround(Technology tech, Layer layer, double maxSize);
    public DRCTemplate getEdgeRule(Technology tech, Layer layer1, Layer layer2, int techMode);
    public DRCTemplate getSpacingRule(Technology tech, Layer layer1, Layer layer2, boolean connected,
                                      boolean multiCut, double wideS, double length, int techMode);
    public boolean isAnyRule(Technology tech, Layer layer1, Layer layer2);
    public DRCTemplate getExtensionRule(Technology tech, Layer layer1, Layer layer2,
                                        int techMode, boolean isGateExtension);
	public int getNumberOfRules();
    public DRCTemplate getMinValue(Layer layer, int type, int techMode);
    public void setMinValue(Layer layer, String name, double value, int type, int techMode);
    public void applyDRCOverrides(String override, Technology tech);
    public boolean isForbiddenNode(int nodeIndex, int type, int techMode);
    /********************* For UI ***********************************/
    DRCTemplate getMinNodeSize(int index, int when);
    String[] getNodesWithRules();
    List getSpacingRules(int index, int type, int techMode);
    void addRule(int index, DRCTemplate rule, int spacingCase);
    void deleteRule(int index, DRCTemplate rule);
    void setSpacingRules(int index, List newRules, int spacingCase);
    boolean doesAllowMultipleWideRules(int index);
    /********************* For information with DRC tool ******************/
//    public static class DRCRule1
//	{
//		public double value;
//        public double maxWidth;   // for spacing wire rules
//        public double minLength; // for spacing wire rules
//		public String ruleName;
//		public int type;
//
//        // For interface with DRC tool
//		public DRCRule1(double distance, String rule, int type)
//		{
//			this.value = distance;
//			this.ruleName = rule;
//            this.type = type;
//		}
//
//        public DRCRule1(String rule, double distance, double maxW, double maxL, int type)
//        {
//			this.value = distance;
//            this.maxWidth = maxW;
//            this.minLength = maxL;
//			this.ruleName = rule;
//            this.type = type;
//        }
//	}
//    public static class DRCNodeRule extends DRCRule1
//    {
//        public double height; // distance in DRCRule class will store width
//        public DRCNodeRule(double width, double height, String rule, int type)
//        {
//            super(width, rule, type);
//            this.height = height;
//        }
//        public double getWidth() {return value;}
//        public void setWidth(double w) {value = w;}
//        public double getHeight() {return height;}
//        public void setHeight(double h) {height = h;}
//    }
}
