package com.sun.electric.technology;

import com.sun.electric.tool.user.projectSettings.ProjSettingsNode;

import java.util.List;

/**
 * This is supposed to better encapsulate a particular foundry
 * associated to a technology plus the valid DRC rules.
 */
public class Foundry {

    public enum Type {
        /** None */                                                         NONE (-1),
        /** only for TSMC technology */                                     TSMC (010000),
        /** only for ST technology */                                       ST (020000),
        /** only for MOSIS technology */                                    MOSIS (040000);
        private final int mode;
        Type(int mode) {
            this.mode = mode;
        }
        public int mode() { return this.mode; }
        public String toString() {return name();}
    }

    private Type type;
    private List<DRCTemplate> rules;

    public Foundry(Type mode) {
        this.type = mode;
    }
    public Type getType() { return type; }
    /**
     * Method to search rule names per node names.
     * @param ruleName
     * @param type
     * @param mode
     * @return
     */
    public DRCTemplate getRuleForNode(String ruleName, DRCTemplate.DRCRuleType type, int mode)
    {
        for (DRCTemplate tmp : rules)
        {
            if (tmp.ruleType == type)
                if ((tmp.when == DRCTemplate.DRCMode.ALL.mode() || (tmp.when&mode) == mode) && tmp.nodeName.equals(ruleName))
                return tmp;
        }
        return null;
    }
    /**
     * Method to search rule names per layer names. If second layer is null, then it searches the rule for a particular layer.
     * @param layer1Name
     * @param layer2Name
     * @param type
     * @param mode
     * @return
     */
    public DRCTemplate getRuleForLayers(String layer1Name, String layer2Name, DRCTemplate.DRCRuleType type, int mode)
    {
        for (DRCTemplate tmp : rules)
        {
            if (tmp.ruleType == type && (tmp.when == DRCTemplate.DRCMode.ALL.mode() || (tmp.when&mode) == mode))
            {
                if (tmp.name1.equals(layer1Name) && (layer2Name == null || tmp.name2.equals(layer2Name)))
                return tmp;
            }
        }
        return null;
    }
    public List<DRCTemplate> getRules() { return rules; }
    public void setRules(List<DRCTemplate> list) { rules = list; }
    public String toString() { return type.name(); }

    /**
	 * Method to set the factory-default GDS name of this Layer.
	 * @param factoryDefault the factory-default GDS name of this Layer.
     */
    public void setFactoryGDSLayer(Layer layer, String factoryDefault)
    {
        // Getting rid of spaces
        String value = factoryDefault.replaceAll(", ", ",");
        setGDSLayer(layer, value);
    }

    /**
	 * Method to return the GDS name of this layer.
	 * @return the GDS name of this layer.
	 */
	public String getGDSLayer(Layer layer)
    {
        return getGDSNode(layer.getTechnology()).getString(layer.getName());
    }

    /**
	 * Method to set the GDS name of this Layer in this Foundry.
	 * @param gdsLayer the GDS name of this Layer.
	 */
	public void setGDSLayer(Layer layer, String gdsLayer)
    {
        Technology tech = layer.getTechnology();
        getGDSNode(tech).putString(layer.getName(), gdsLayer);
    }

    private ProjSettingsNode getGDSNode(Technology tech) {
        ProjSettingsNode node = tech.getProjectSettings().getNode("GDS");
        if (type == Type.TSMC)
            return node.getNode("TSMC");
        else if (type == Type.MOSIS)
            return node.getNode("MOSIS");
        else if (type == Type.ST)
            return node.getNode("ST");
        else
            return node;
    }

}
