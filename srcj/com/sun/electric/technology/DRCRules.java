package com.sun.electric.technology;

import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;

/**
 * Created by IntelliJ IDEA.
 * User: Gilda
 * Date: Aug 26, 2004
 * Time: 5:54:42 PM
 * To change this template use File | Settings | File Templates.
 */
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
