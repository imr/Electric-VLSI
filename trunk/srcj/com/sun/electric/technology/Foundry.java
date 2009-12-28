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
 * the Free Software Foundation; either version 3 of the License, or
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

import com.sun.electric.database.text.Setting;

import java.net.URL;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;

/**
 * This is supposed to better encapsulate a particular foundry
 * associated to a technology plus the valid DRC rules.
 */
public class Foundry {

    public static class Type {
        public static Type NONE = new Type("NONE", -1, 040); // DRC_BIT_NONE_FOUNDRY = 040; /* For NONE foundry selection */
        public static Type TSMC = new Type("TSMC", 010000, 010);  // DRC_BIT_TSMC_FOUNDRY = 010; /* For TSMC foundry selection */
        public static Type ST = new Type("ST", 020000, 04); // DRC_BIT_ST_FOUNDRY = 04; /* For ST foundry selection */
        public static Type MOSIS = new Type("MOSIS", 040000, 020); // DRC_BIT_MOSIS_FOUNDRY = 020; /* For Mosis foundry selection */
        private static List<Type> typeList = new ArrayList<Type>(4);

        static
        {
            typeList.add(NONE);
            typeList.add(TSMC);
            typeList.add(ST);
            typeList.add(MOSIS);
        }
//    public enum Type {
//        /** None */                                                         NONE (-1),
//        /** only for TSMC technology */                                     TSMC (010000),
//        /** only for ST technology */                                       ST (020000),
//        /** only for MOSIS technology */                                    MOSIS (040000);
        private final String name;  // foundry name
        private final int mode; // foundry mode
        private int bit; // foundry bit for DRC
        Type(String n, int m, int b) {
            this.name = n;
            this.mode = m;
            this.bit = b;
        }
        public int getBit() { return bit; }
        public int getMode() { return mode; }
        public String toString() {return name;}
        public String getName() {return name;}

        public static Type valueOf(String n)
        {
        	if (n == null) return NONE;
            for (Type t : typeList)
            {
                if (t.getName().equals(n))
                    return t;
            }
            // none of the known foundries
            Type t = new Type(n, 010000000, 0100);  // the mode has to be bigger than M9
            System.out.println("New foundry requested: '" + n + "'");
            typeList.add(t);
            return t;
        }

        public static List<Type> getValues() {return typeList;}
    }

    private final Technology tech;
    private final Type type;
    private final URL fileURL; // URL of xml file
    private List<DRCTemplate> rules;
    private boolean rulesLoaded;
    private Setting[] gdsLayerSettings;

    Foundry(Technology tech, Type mode, URL fileURL, String[] gdsLayers) {
        this.tech = tech;
        this.type = mode;
        this.fileURL = fileURL;
        if (fileURL == null)
            rulesLoaded = true;
        setFactoryGDSLayers(gdsLayers);
    }
    Foundry(Technology tech, Type mode, List<DRCTemplate> rules, String[] gdsLayers) {
        this.tech = tech;
        this.type = mode;
        fileURL = null;
        this.rules = rules;
        rulesLoaded = true;
        setFactoryGDSLayers(gdsLayers);
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
    public List<DRCTemplate> getRules() {
        if (!rulesLoaded)
            parseRules();
        return rules;
    }
    private void parseRules() {
        rulesLoaded = true;
        if (fileURL == null) {
            System.out.println("Problems loading " + this + " deck for " + tech);
            return;
        }
        DRCTemplate.DRCXMLParser parser = DRCTemplate.importDRCDeck(fileURL, tech.getXmlTech(), false);
        assert(parser.getRules().size() == 1);
        assert(parser.isParseOK());
        setRules(parser.getRules().get(0).drcRules);
    }
    public void setRules(List<DRCTemplate> list) { rules = list; }
    public String toString() { return type.getName(); }

    /**
     * Method to return the map from Layers of Foundry's technology to their GDS names in this foundry.
     * Only Layers with non-empty GDS names are present in the map
     * @return the map from Layers to GDS names
     */
    public Map<Layer,String> getGDSLayers()
    {
        LinkedHashMap<Layer,String> gdsLayers = new LinkedHashMap<Layer,String>();
        assert gdsLayerSettings.length == tech.getNumLayers();
        for (int layerIndex = 0; layerIndex < gdsLayerSettings.length; layerIndex++) {
            String gdsLayer = gdsLayerSettings[layerIndex].getString();
            if (gdsLayer.length() > 0)
                gdsLayers.put(tech.getLayer(layerIndex), gdsLayer);
        }
        return gdsLayers;
    }

    /**
     * Method to return the map from Layers of Foundry's technology to project preferences
     * which define their GDS names in this foundry.
     * @return the map from Layers to project preferences with their GDS names
     */
    public Setting getGDSLayerSetting(Layer layer) {
        if (layer.getTechnology() != tech)
            throw new IllegalArgumentException();
        return gdsLayerSettings[layer.getIndex()];
    }

    /**
     * Method to set the factory-default GDS names of Layers in this Foundry.
     * @param tech Technology of this Foundry.
     * @param factoryDefault the factory-default GDS name of this Layer.
     */
    private void setFactoryGDSLayers(String[] gdsLayers) {
        LinkedHashMap<Layer,String> gdsMap = new LinkedHashMap<Layer,String>();
        for (String gdsDef: gdsLayers) {
            int space = gdsDef.indexOf(' ');
            Layer layer = tech.findLayer(gdsDef.substring(0, space));
            while (space < gdsDef.length() && gdsDef.charAt(space) == ' ') space++;
            if (layer == null || layer.isPseudoLayer() || gdsMap.put(layer, gdsDef.substring(space)) != null)
                throw new IllegalArgumentException(gdsDef);
        }

        assert gdsLayerSettings == null;
        gdsLayerSettings = new Setting[tech.getNumLayers()];
        String techName = tech.getTechName();
        String what = getGDSPrefName();
        for (int layerIndex = 0; layerIndex < gdsLayerSettings.length; layerIndex++) {
            Layer layer = tech.getLayer(layerIndex);
            String factoryDefault = gdsMap.get(layer);
            if (factoryDefault == null)
                factoryDefault = "";
            // Getting rid of spaces
            factoryDefault = factoryDefault.replaceAll(", ", ",");

            Setting setting = getGDSNode().makeStringSetting(what + "LayerFor" + layer.getName() + "IN" + techName,
                    Technology.TECH_NODE,
                    layer.getName(),
                    what + " tab", what + " for layer " + layer.getName() + " in technology " + techName, factoryDefault);
            gdsLayerSettings[layerIndex] = setting;
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
     * Generate key name for GDS value depending on the foundry
     * @return
     */
    private String getGDSPrefName()
    {
        return ("GDS("+type.getName()+")");
    }

    private Setting.Group getGDSNode() {
        Setting.Group gdsNode = tech.getProjectSettings().node("GDS");
        if (type == Type.TSMC)
            return gdsNode.node("TSMC");
        else if (type == Type.MOSIS)
            return gdsNode.node("MOSIS");
        else if (type == Type.ST)
            return gdsNode.node("ST");
        else
            return gdsNode;
    }

     /**
     * Method to finish initialization of this Foundry.
     */
    void finish() {
        if (gdsLayerSettings == null)
            setFactoryGDSLayers(new String[0]);
    }
}
