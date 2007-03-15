/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Foundry.java
 * Written by Gilda Garreton, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

import com.sun.electric.tool.user.projectSettings.ProjSettingsNode;
import com.sun.electric.database.text.Setting;

import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

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

    private final Technology tech;
    private final Type type;
    private List<DRCTemplate> rules;
    private Setting[] gdsLayerSettings;

    public Foundry(Technology tech, Type mode) {
        this.tech = tech;
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
     * Method to set the factory-default GDS names of Layers in this Foundry.
     * @param tech Technology of this Foundry.
     * @param factoryDefault the factory-default GDS name of this Layer.
     */
    public void setFactoryGDSLayers(String... gdsLayers) {
        LinkedHashMap<Layer,String> gdsMap = new LinkedHashMap<Layer,String>();
        for (String gdsDef: gdsLayers) {
            int space = gdsDef.indexOf(' ');
            Layer layer = tech.findLayer(gdsDef.substring(0, space));
            while (space < gdsDef.length() && gdsDef.charAt(space) == ' ') space++;
            if (layer == null || gdsMap.put(layer, gdsDef.substring(space)) != null)
                throw new IllegalArgumentException(gdsDef);
        }
        
        assert gdsLayerSettings == null;
        gdsLayerSettings = new Setting[tech.getNumLayers()];
        for (Map.Entry<Layer,String> e: gdsMap.entrySet()) {
            Layer layer = e.getKey();
            String factoryDefault = e.getValue();
            // Getting rid of spaces
            String value = factoryDefault.replaceAll(", ", ",");
            makeLayerSetting(layer, value);
        }
    }

//    private static Foundry curFoundry = null;
//    public void setFactoryGDSLayer(Layer layer, String factoryDefault)
//    {
//        if (!toString().equals("ST")) {
//            if (this != curFoundry) {
//                System.out.println(layer.getTechnology() + " " + this);
//                curFoundry = this;
//            }
//            System.out.println("\"" + layer.getName() + " " + factoryDefault + "\",");
//        }
//        // Getting rid of spaces
//        String value = factoryDefault.replaceAll(", ", ",");
//        makeLayerSetting(layer, getGDSPrefName(), gdsLayerPrefs, value);
//    }
    
    /**
     * Method to return the GDS name of this layer.
     * @return the GDS name of this layer.
     */
    public String getGDSLayer(Layer layer)
    {
        assert layer.getTechnology() == tech;
        return gdsLayerSettings[layer.getIndex()].getString();
    }

    /**
     * Method to set the GDS name of this Layer in this Foundry.
     * @param gdsLayer the GDS name of this Layer.
     */
    public void setGDSLayer(Layer layer, String gdsLayer)
    {
        assert layer.getTechnology() == tech;
        gdsLayerSettings[layer.getIndex()].setString(gdsLayer);
    }

    private void makeLayerSetting(Layer layer, String factory) {
        assert layer.getTechnology() == tech;
        int layerIndex = layer.getIndex();
        assert gdsLayerSettings[layerIndex] == null;
        String techName = layer.getTechnology().getTechName();
        if (factory == null) factory = "";
        String what = getGDSPrefName();
        Setting setting = Setting.makeStringSetting(what + "LayerFor" + layer.getName() + "IN" + techName, Technology.getTechnologyPreferences(),
                getGDSNode(), layer.getName(),
                what + " tab", what + " for layer " + layer.getName() + " in technology " + techName, factory);
        gdsLayerSettings[layerIndex] = setting;
    }
    
    /**
     * Generate key name for GDS value depending on the foundry
     * @return
     */
    private String getGDSPrefName()
    {
        return ("GDS("+type.name()+")");
    }

    private ProjSettingsNode getGDSNode() {
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
    
     /**
     * Method to finish initialization of this Foundry.
     */
    void finish() {
        if (gdsLayerSettings == null)
            gdsLayerSettings = new Setting[tech.getNumLayers()];
        for (int layerIndex = 0; layerIndex < tech.getNumLayers(); layerIndex++) {
            if (gdsLayerSettings != null) continue;
            makeLayerSetting(tech.getLayer(layerIndex), "");
        }
    }
}
