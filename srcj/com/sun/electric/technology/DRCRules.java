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

import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;

public interface DRCRules {
	public void setMinNodeSize(int index, double value);
	public double getWorstSpacingDistance();
    public double getMaxSurround(Technology tech, Layer layer, double maxSize);
    public DRCRule getEdgeRule(Technology tech, Layer layer1, Layer layer2);
    public DRCRules.DRCRule getSpacingRule(Technology tech, Layer layer1, Layer layer2, boolean connected,
                                           boolean multiCut, double wideS);
    public boolean isAnyRule(Technology tech, Layer layer1, Layer layer2);
	public int getNumberOfRules();
    public DRCRules.DRCRule getMinValue(Layer layer, int type);
    public void applyDRCOverrides(String override, Technology tech);
    
    public static class DRCRule
	{
		public double value;
		public String rule;

		public DRCRule(double distance, String rule)
		{
			this.value = distance;
			this.rule = rule;
		}
	}
}
