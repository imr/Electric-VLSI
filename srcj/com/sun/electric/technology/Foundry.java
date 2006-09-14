package com.sun.electric.technology;

import com.sun.electric.tool.user.projectSettings.ProjSettingsNode;
import com.sun.electric.database.text.Pref;

import java.util.List;
import java.util.HashMap;

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
    private HashMap<String,Pref> gdsLayerPrefs = new HashMap<String, Pref>();

    public Foundry(Type mode) {
        this.type = mode;
    }
    public Type getType() { return type; }
//    /**
//     * Method to search rule names per node names.
//     * @param ruleName
//     * @param type
//     * @param modes
//     * @return
//     */
//    public DRCTemplate getRuleForNode(String ruleName, DRCTemplate.DRCRuleType type, int[] modes)
//    {
//        for (DRCTemplate tmp : rules)
//        {
//            if (tmp.ruleType == type && tmp.nodeName.equals(ruleName))
//            {
//                for (int i = 0; i < modes.length; i++)
//                {
//                    int mode = modes[i];
//                    if (tmp.when == DRCTemplate.DRCMode.ALL.mode() || tmp.when == mode || (tmp.when&mode) == mode)
//                        return tmp;
//                }
////                if ((tmp.when == DRCTemplate.DRCMode.ALL.mode() || (tmp.when&mode) == mode) && tmp.nodeName.equals(ruleName))
//            }
//        }
//        return null;
//    }
//    /**
//     * Method to search rule names per layer names. If second layer is null, then it searches the rule for a particular layer.
//     * @param layer1Name
//     * @param layer2Name
//     * @param type
//     * @param modes
//     * @return
//     */
//    public DRCTemplate getRuleForLayers(String layer1Name, String layer2Name, DRCTemplate.DRCRuleType type, int[] modes)
//    {
//        for (DRCTemplate tmp : rules)
//        {
//            if (tmp.ruleType == type && (tmp.name1.equals(layer1Name) && (layer2Name == null || tmp.name2.equals(layer2Name))))
//            {
//                for (int i = 0; i < modes.length; i++)
//                {
//                    int mode = modes[i];
//                    if (tmp.when == DRCTemplate.DRCMode.ALL.mode() || tmp.when == mode || (tmp.when&mode) == mode)
//                        return tmp;
//                }
//            }
////            if (tmp.ruleType == type && (tmp.when == DRCTemplate.DRCMode.ALL.mode() || (tmp.when&mode) == mode))
////            {
////                if (tmp.name1.equals(layer1Name) && (layer2Name == null || tmp.name2.equals(layer2Name)))
////                return tmp;
////            }
//        }
//        return null;
//    }
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
        getLayerSetting(layer, getGDSPrefName(), gdsLayerPrefs, value);
    }

    /**
     * Method to return the GDS name of this layer.
     * @return the GDS name of this layer.
     */
    public String getGDSLayer(Layer layer)
    {
        return getLayerSetting(layer, getGDSPrefName(), gdsLayerPrefs, null).getString();
    }

    private Pref getLayerSetting(Layer layer, String what, HashMap<String,Pref> map, String factory)
    {
        String techName = layer.getTechnology().getTechName();
        String key = layer.getName() + what + techName; // Have to compose hash value with what so more than 1 type of what can be stored.
        Pref pref = (Pref)map.get(key);
        if (pref == null)
        {
            if (factory == null) factory = "";
            pref = Pref.makeStringSetting(what + "LayerFor" + layer.getName() + "IN" + techName, Technology.getTechnologyPreferences(),
                    layer.getTechnology(), getGDSNode(layer.getTechnology()), layer.getName(),
                what + " tab", what + " for layer " + layer.getName() + " in technology " + techName, factory);
            map.put(key, pref);
        }
        return pref;
    }

    /**
     * Method to set the GDS name of this Layer in this Foundry.
     * @param gdsLayer the GDS name of this Layer.
     */
    public void setGDSLayer(Layer layer, String gdsLayer)
    {
        getLayerSetting(layer, getGDSPrefName(), gdsLayerPrefs, gdsLayer).setString(gdsLayer);
    }

    /**
     * Generate key name for GDS value depending on the foundry
     * @return
     */
    private String getGDSPrefName()
    {
        return ("GDS("+type.name()+")");
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
