/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DRCTemplate.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Class to define rules from TSCM files...
 */
public class DRCTemplate implements Serializable
{
    public enum DRCMode
    {
        /** None */                                                         NONE (-1),
        /** always */                                                       ALL (0),
        /** only applies if there are 2-3 metal layers in process */		M23 (03),
        /** only applies if there are 2 metal layers in process */			M2 (01),
        /** only applies if there are 3 metal layers in process */			M3 (02),
        /** only applies if there are 4-6 metal layers in process */		M456 (034),
        /** only applies if there are 4 metal layers in process */			M4 (04),
        /** only applies if there are 5-6 metal layers in process */		M56 (030),
        /** only applies if there are 5 metal layers in process */			M5 (010),
        /** only applies if there are 6 metal layers in process */			M6 (020),
        /** only applies if there are 7 metal layers in process */		    M7 (0100000),
        /** only applies if there are 8 metal layers in process */		    M8 (0200000),
        /** only applies if there are 9 metal layers in process */		    M9 (0400000),
        /** only applies if there are 10 metal layers in process */		    M10 (01000000),
        /** only applies if there are 11 metal layers in process */		    M11 (0200000),
        /** only applies if there are 12 metal layers in process */		    M12 (0400000),
        /** Max number of layers are dictated by EGraphics.TRANSPARENT_12.
        /** only applies if analog (npn-transistor( rules are in effect */  AN(04000000),

        /** only applies if alternate contact rules are in effect */		AC (040),
        /** only applies if alternate contact rules are not in effect */	NAC (0100),
        /** only applies if stacked vias are allowed */						SV (0200),
        /** only applies if stacked vias are not allowed */					NSV (0400),
        /** only applies if deep rules are in effect */						DE (01000),
        /** only applies if submicron rules are in effect */				SU (02000),
        /** only applies if scmos rules are in effect */					SC (04000);
//        /** only for TSMC technology */                                     TSMC (010000),
//        /** only for ST technology */                                       ST (020000),
//        /** only for MOSIS technology */                                    MOSIS (040000);

        private final int mode;   // mode
        DRCMode(int mode) {
            this.mode = mode;
        }
        public int mode() { return this.mode; }
        public String toString() {return name();}
    }

    public enum DRCRuleType
    {
    // the meaning of "ruletype" in the DRC table
        /** nothing chosen */			    NONE,
        /** a minimum-width rule */			MINWID,
        /** a conditional minimum-width rule */			MINWIDCOND,
        /** a node size rule */				NODSIZ,
        /** a general surround rule */		SURROUND,
        /** a spacing rule */				SPACING,
        /** an edge spacing rule */			SPACINGE,
        /** a connected spacing rule */		CONSPA,
        /** an unconnected spacing rule */	UCONSPA,
        /** a spacing rule for 2D cuts*/	UCONSPA2D,
        /** X contact cut surround rule */	CUTSURX,
        /** Y contact cut surround rule */	CUTSURY,
        /** arc surround rule */			ASURROUND,
        /** minimum area rule */			MINAREA,
        /** enclosed area rule */			MINENCLOSEDAREA,
        /** extension rule */               EXTENSION,
        /** forbidden rule */               FORBIDDEN,
        /** extension gate rule */          EXTENSIONGATE,
        /** slot size rule */               SLOTSIZE
    }

    // For sorting
    public static final DRCTemplateSort templateSort = new DRCTemplateSort();

    public String ruleName;			/* the name of the rule */
    public int when;				/* when the rule is used */
    public DRCRuleType ruleType;			/* the type of the rule */
    public String name1, name2;	/* two layers/nodes that are used by the rule */
    public double[] values;
    public double maxWidth;         /* max length where spacing is valid */
    public double minLength;       /* min paralell distance for spacing rule */
    public String nodeName;		/* the node that is used by the rule */
	public int multiCuts;         /* -1=dont care, 0=no cuts, 1=with cuts multi cut rule */
    public String condition;

    /**
     * Method to detect if a given rule could be ignored if the process is a PSubstrate process
     * @param pSubstrateProcess
     * @return true if a given rule could be ignored if the process is a PSubstrate process.
     */
    public boolean isRuleIgnoredInPSubstrateProcess(boolean pSubstrateProcess)
    {
        if (!pSubstrateProcess) return false; // never ignore

        if (ruleType == DRCRuleType.SPACING ||
            ruleType == DRCRuleType.SPACINGE ||
            ruleType == DRCRuleType.CONSPA ||
            ruleType == DRCRuleType.UCONSPA)
            return name1.toLowerCase().equals("p-well") && name2.toLowerCase().equals("p-well");
        else if (ruleType == DRCRuleType.MINAREA ||
            ruleType == DRCRuleType.MINENCLOSEDAREA ||
            ruleType == DRCRuleType.MINWID ||
            ruleType == DRCRuleType.MINWIDCOND)
           return name1.toLowerCase().equals("p-well");
        return false;
    }

    private void copyValues(double[] vals)
    {
        int len = vals.length;
        assert(len == 1 || len == 2);
        this.values = new double[len];
        System.arraycopy(vals, 0, this.values, 0, len);
    }

    public double getValue(int i)
    {
        return values[i];
    }

    public void setValue(int i, double val)
    {
        values[i] = val;
    }

    public DRCTemplate(DRCTemplate rule)
    {
        this.ruleName = rule.ruleName;
        this.when = rule.when;
        this.ruleType = rule.ruleType;
        this.name1 = rule.name1;
        this.name2 = rule.name2;
        copyValues(rule.values);
        this.maxWidth = rule.maxWidth;
        this.minLength = rule.minLength;
        this.nodeName = rule.nodeName;
        this.multiCuts = rule.multiCuts;
        this.condition = rule.condition;
    }

    public DRCTemplate(String rule, int when, DRCRuleType ruleType, String name1, String name2, double[] vals,
                       String nodeName, String condition)
    {
        this.ruleName = rule;
        this.when = when;
        this.ruleType = ruleType;
        this.name1 = name1;
        this.name2 = name2;
        copyValues(vals);
        this.nodeName = nodeName;
        this.condition = condition;
        this.multiCuts = -1; // don't care

        switch (ruleType)
        {
            case SPACING:
//            case SURROUND:
            {
                if (name1 == null || name2 == null)
                {
                    System.out.println("Error: missing one layer in no '" + rule + "' ");
                }
            }
            break;
            default:
        }
    }

	/**
	 * For different spacing depending on wire length and multi cuts.
	 */
    public DRCTemplate(String rule, int when, DRCRuleType ruleType, double maxW, double minLen, double[] vals, int multiCut)
    {
        this.ruleName = rule;
        this.when = when;
        this.ruleType = ruleType;
        copyValues(vals);
        this.maxWidth = maxW;
        this.minLength = minLen;
		this.multiCuts = multiCut;
    }

	/**
	 * For different spacing depending on wire length and multi cuts.
	 */
    public DRCTemplate(String rule, int when, DRCRuleType ruleType, double maxW, double minLen, String name1, String name2,
                       double[] vals, int multiCut)
    {
        this.ruleName = rule;
        this.when = when;
        this.ruleType = ruleType;
        this.name1 = name1;
        this.name2 = name2;
        copyValues(vals);
        this.maxWidth = maxW;
        this.minLength = minLen;
		this.multiCuts = multiCut;

        switch (ruleType)
        {
            case SPACING:
                {
                    if (name1 == null || name2 == null)
                    {
                        System.out.println("Error: missing one layer in no '" + rule + "' ");
                    }
                }
            break;
            default:
        }
    }

    /**
     * Auxiliar class to sort areas in array
     */
    public static class DRCTemplateSort implements Comparator<DRCTemplate>
    {
    	public int compare(DRCTemplate d1, DRCTemplate d2)
        {
    		double bb1 = d1.getValue(0);
    		double bb2 = d2.getValue(0); // not checking valueX

            if (bb1 < bb2) return -1;
            else if (bb1 > bb2) return 1;
            return (0); // identical
        }
    }

    /**
     * Method to import DRC deck from a file provided by URL. Note: it has to be URL otherwise
     * it won't file the file in Electric jar file.
     * @param fileURL
     * @param verbose
     * @return parsed information
     */
    public static DRCXMLParser importDRCDeck(URL fileURL, Xml.Technology tech, boolean verbose)
    {
        DRCXMLParser parser = new DRCXMLParser();
        parser.process(fileURL, tech, verbose);
        return parser;
    }

    /**
     * Method to transform strings into XML-compatible characters.
     * @param orig Original string
     * @return Modified string that is XML-compatible.
     */
    public static String covertToXMLFormat(String orig)
    {
        // XML has a special set of characters that cannot be used in normal XML strings. These characters are:
        // 1. & - &amp;
        // 2. < - &lt;
        // 3. > - &gt;
        // 4. " - &quot;
        // 5. ' - &#39;
        if (orig == null) return "";

        orig = orig.replaceAll("&", "&amp;");
        orig = orig.replaceAll("<", "&lt;");
        orig = orig.replaceAll(">", "&gt;");
        orig = orig.replaceAll("\"", "&quot;");
        orig = orig.replaceAll("'", "&#39;");
        return orig;
    }

    /**
     * Method to export DRC rules in XML format
     * @param fileName
     * @param tech
     */
    public static void exportDRCDecks(String fileName, Technology tech)
    {
        try
        {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            out.println("<!--");
            out.println("\t Document: DRC deck for " + tech);
            out.println("\t Generated by: Electric (" + Version.getVersion() + ")");
            out.println("-->");
            out.println("<!DOCTYPE DRCRules SYSTEM \"DRC.dtd\">");
            out.println("<DRCRules>");
            
            for (Iterator<Foundry> it = tech.getFoundries(); it.hasNext();)
            {
                Foundry foundry = it.next();
                List<DRCTemplate> rules = foundry.getRules();
                out.println("    <Foundry name=\"" + covertToXMLFormat(foundry.getType().getName()) + "\">");

                for (DRCTemplate rule : rules)
                {
                    if (rule.ruleType == DRCRuleType.EXTENSIONGATE
                            || rule.ruleType == DRCRuleType.CUTSURX
                            || rule.ruleType == DRCRuleType.CUTSURY
                            || rule.ruleType == DRCRuleType.SLOTSIZE)
                        continue; // Don't export experimental rule types
                    exportDRCRule(out, rule);
                }
                out.println("    </Foundry>");
            }
            out.println("</DRCRules>");
            out.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void exportDRCRule(PrintWriter out, DRCTemplate rule)
    {
        String whenName = null;
        int when = 0;
        for (DRCMode p : DRCMode.values())
        {
            if ((p.mode() & ~when) == 0) continue; // all bits of "p" are already written
            if ((p.mode() & ~rule.when) != 0) continue; // some bits of "p" can't be written'
            if (whenName == null) // first element
                whenName = "";
            else
                whenName += "|";
            whenName += p;
            when |= p.mode();
        }
        assert when == rule.when;
        if (whenName == null) whenName = DRCMode.ALL.name();  // When originally it was set to ALL
        String condition = "";

        // treating special characters in XML
        // SAX parser takes care of those characters during reading.
        String ruleName = covertToXMLFormat(rule.ruleName);
        String name1 = covertToXMLFormat(rule.name1);
        String name2 = covertToXMLFormat(rule.name2);
        String cond = covertToXMLFormat(rule.condition);
        String nodeName = covertToXMLFormat(rule.nodeName);

        switch(rule.ruleType)
        {
            case MINWIDCOND:
                condition = " condition=\"" + cond + "\"";
            case MINWID:
            case MINAREA:
            case MINENCLOSEDAREA:
                out.println("        <LayerRule ruleName=\"" + ruleName + "\""
                        + " layerName=\"" + name1 + "\""
                        + " type=\""+rule.ruleType+"\""
                        + condition
                        + " when=\"" + whenName + "\""
                        + " value=\"" + rule.getValue(0) + "\""
                        + "/>");
                break;
            case UCONSPA:
            case UCONSPA2D:
            case CONSPA:
            case SPACING:
            case SPACINGE:
            case EXTENSION:
            case EXTENSIONGATE:
                if (rule.condition != null)
                    condition = " condition=\"" + cond + "\"";

                String noName = (rule.nodeName != null) ? (" nodeName=\"" + nodeName + "\"") : "";
                String wideValues = (rule.maxWidth > 0) ? (" maxW=\"" + rule.maxWidth + "\""
                        + " minLen=\"" + rule.minLength + "\"") : "";
                out.println("        <LayersRule ruleName=\"" + ruleName + "\""
                        + " layerNames=\"{" + name1 + "," + name2 + "}\""
                        + " type=\""+rule.ruleType+"\""
                        + " when=\"" + whenName + "\""
                        + " value=\"" + rule.getValue(0) + "\""
                        + wideValues
                        + noName
                        + condition
                        + "/>");
                break;
            case SURROUND: // if nodeName==null -> LayersRule
            case ASURROUND:
                String ruleType = "NodeLayersRule";
                String value = " value=\"" + rule.getValue(0) + "\"";
                String layersName = " layerNames=\"{" + name1 + "," + name2 + "}\"";
                if (rule.getValue(0) != rule.getValue(1)) // x and y values
                {
                    value = " valueX=\"" + rule.getValue(0) + "\"" + " valueY=\"" + rule.getValue(1) + "\"";
                }
                noName = " nodeName=\"" + nodeName + "\"";
                if (rule.name2 == null) // conditional surround
                {
                    noName = "";
                    ruleType = "LayerRule";
                    layersName = " layerName=\"" + name1 + "\" condition=\"" + cond + "\"";
                }
                else if (rule.nodeName == null)  // LayersRule
                {
                    noName = "";
                    ruleType = "LayersRule";
                }
                out.println("        <" + ruleType + " ruleName=\"" + ruleName + "\""
                        + layersName
                        + " type=\""+rule.ruleType+"\""
                        + " when=\"" + whenName + "\""
                        + value
                        + noName
                        + "/>");
                break;
            case NODSIZ:
                value = " value=\"" + rule.getValue(0) + "\"";

                if (rule.getValue(0) != rule.getValue(1)) // x and y values
                {
                    value = " valueX=\"" + rule.getValue(0) + "\"" + " valueY=\"" + rule.getValue(1) + "\"";
                }
                out.println("        <NodeRule ruleName=\"" + ruleName + "\""
                        + " type=\""+rule.ruleType+"\""
                        + " when=\"" + whenName + "\""
                        + value
                        + " nodeName=\"" + nodeName + "\""
                        + "/>");
                break;
            case FORBIDDEN:
                if (rule.nodeName != null) {
                    out.println("        <NodeRule ruleName=\"" + ruleName + "\""
                            + " type=\""+rule.ruleType+"\""
                            + " when=\"" + whenName + "\""
                            + " nodeName=\"" + nodeName + "\""
                            + "/>");
                } else {
                    out.println("        <LayersRule ruleName=\"" + ruleName + "\""
                            + " layerNames=\"{" + name1 + "," + name2 + "}\""
                            + " type=\""+rule.ruleType+"\""
                            + " when=\"" + whenName + "\""
                            + "/>");
                }
                break;
            case CUTSURX:
            case CUTSURY:
            case SLOTSIZE:
            default:
                assert false;
                System.out.println("Case not implemented " + rule.ruleType);
        }
    }

    /** Method to build combined name in special spacing rules
     * @param layer
     * @param geo
     * @return combined name in special spacing rules
     */
    public static String getSpacingCombinedName(Layer layer, Geometric geo)
    {
        String n1 = layer.getName() + "-";

        if (geo != null)
        {
            if (geo instanceof NodeInst)
                n1 += ((NodeInst)geo).getProto().getName();
            else
                n1 += ((ArcInst)geo).getProto().getName();
        }
        return n1;
    }

    /**
     * Old method to parse DRC rules. It doesn't check if layer/node exists.
     * @return true if successful, false on error.
     */
    public static boolean parseXmlElement(List<DRCTemplate> drcRules, String qName, Attributes attributes, String localName)
    {
        System.out.println("Layer/Node names not checked");
        return parseXmlElement(drcRules, null, null, qName, attributes, localName);
    }

    public static boolean parseXmlElement(List<DRCTemplate> drcRules, Collection<String> layerNamesList,
                                          Collection<String> nodeNamesList,
                                          String qName, Attributes attributes, String localName)
    {
        boolean layerRule = qName.equals("LayerRule");
        boolean layersRule = qName.equals("LayersRule");
        boolean nodeLayersRule = qName.equals("NodeLayersRule");
        boolean nodeRule = qName.equals("NodeRule");

        if (!layerRule && !layersRule && !nodeLayersRule && !nodeRule) return false;

        String ruleName = "", layerNames = "", nodeNames = null, condition = null;
        int when = DRCTemplate.DRCMode.ALL.mode();
        DRCTemplate.DRCRuleType type = DRCTemplate.DRCRuleType.NONE;
        double[] values = new double[2];
        Double maxW = null, minLen = null;

        for (int i = 0; i < attributes.getLength(); i++)
        {
            if (attributes.getQName(i).equals("ruleName"))
                ruleName = attributes.getValue(i);
            else if (attributes.getQName(i).startsWith("layerName"))
                layerNames = attributes.getValue(i);
            else if (attributes.getQName(i).startsWith("nodeName"))
                nodeNames = attributes.getValue(i);
            else if (attributes.getQName(i).startsWith("condition"))
                condition = attributes.getValue(i);
            else if (attributes.getQName(i).equals("type"))
                type = DRCTemplate.DRCRuleType.valueOf(attributes.getValue(i));
            else if (attributes.getQName(i).equals("when"))
            {
                String[] modes = TextUtils.parseString(attributes.getValue(i), "|");
                for (String mode : modes)
                {
                    DRCTemplate.DRCMode m = DRCTemplate.DRCMode.valueOf(mode);
                    when |= m.mode();
                }
            }
            else if (attributes.getQName(i).equals("value"))
            {
                String value = attributes.getValue(i);
                if (value.toLowerCase().equals("double.max_value"))
                {
                    values[0] = values[1] = Double.MAX_VALUE;
                }
                else
                {
                    try
                    {
                        values[0] = values[1] = Double.parseDouble(value);
                    }
                    catch (Exception e)
                    {
                        System.out.println("Invalid attribute in DRCXMLParser: " + value + " is not a double in " + localName);
                        return false;
                    }
                }
            }
            else if (attributes.getQName(i).equals("valueX"))
                values[0] = Double.parseDouble(attributes.getValue(i));
            else if (attributes.getQName(i).equals("valueY"))
                values[1] = Double.parseDouble(attributes.getValue(i));
            else if (attributes.getQName(i).equals("maxW"))
                maxW = new Double(Double.parseDouble(attributes.getValue(i)));
            else if (attributes.getQName(i).equals("minLen"))
                minLen = new Double(Double.parseDouble(attributes.getValue(i)));
            else
            {
                System.out.println("Invalid attribute in DRCXMLParser in " + localName);
                return false;
            }
        }

        // They could be several layer names or pairs of names for the same rule
        if (layerRule)
        {
            String[] layers = TextUtils.parseString(layerNames, ",");
            for (String layer : layers)
            {
                if (!layerNamesList.contains(layer))
                {
                    System.out.println("Invalid layer '" + layer + "' in DRCXMLParser in " + localName);
                    return false; // layer not found
                }
                if (nodeNames == null)
                {
                    DRCTemplate tmp = new DRCTemplate(ruleName, when, type, layer,
                            null, values, null, condition);
                    drcRules.add(tmp);
                }
                else
                {
                    String[] names = TextUtils.parseString(nodeNames, ",");
                    for (String name : names)
                    {
                        if (!layerNamesList.contains(name))
                        {
                            System.out.println("Invalid node '" + name + "' in DRCXMLParser in " + localName);
                            return false; // layer not found
                        }
                        DRCTemplate tmp = new DRCTemplate(ruleName, when, type, layer,
                                null, values, name, null);
                        drcRules.add(tmp);
                    }
                }
            }
        }
        else if (nodeRule)
        {
            if (nodeNames == null)
            {
                DRCTemplate tmp = new DRCTemplate(ruleName, when, type, null, null, values, null, null);
                drcRules.add(tmp);
            }
            else
            {
                String[] names = TextUtils.parseString(nodeNames, ",");
                for (String name : names)
                {
                    if (!nodeNamesList.contains(name))
                    {
                        System.out.println("Invalid node '" + name + "' in DRCXMLParser in " + localName);
                        return false; // node not found
                    }
                    DRCTemplate tmp = new DRCTemplate(ruleName, when, type,
                            null, null, values, name, null);
                    drcRules.add(tmp);
                }
            }
        }
        else if (layersRule || nodeLayersRule)
        {
            String[] layerPairs = TextUtils.parseString(layerNames, "{}");
            for (String layerPair : layerPairs)
            {
                String[] pair = TextUtils.parseString(layerPair, ",");
                if (pair.length != 2) continue;
                if (nodeNames == null)
                {
                    DRCTemplate tmp;

                    if (maxW == null)
                        tmp = new DRCTemplate(ruleName, when, type, pair[0], pair[1], values, null, condition);
                    else
                        tmp = new DRCTemplate(ruleName, when, type, maxW.doubleValue(), minLen.doubleValue(), pair[0], pair[1], values, -1);

                    // not sure why this was done
                    /*
                    if (type == DRCTemplate.DRCRuleType.UCONSPA2D)
                    {
                        tmp.multiCuts = 1;
                        DRCTemplate tmp1 = new DRCTemplate(tmp);
                        tmp1.ruleType = DRCTemplate.DRCRuleType.UCONSPA;
                        drcRules.add(tmp1); // duplicate but with UCONSPA
                        DRCTemplate tmp2 = new DRCTemplate(tmp);
                        tmp1.ruleType = DRCTemplate.DRCRuleType.CONSPA;
                        drcRules.add(tmp2); // duplicate but with CONSPA
                    }
                    */
                    
                    drcRules.add(tmp);
                }
                else
                {
                    String[] names = TextUtils.parseString(nodeNames, ",");
                    for (String name : names)
                    {
                        DRCTemplate tmp = null;
                        if (maxW == null)
                            tmp = new DRCTemplate(ruleName, when, type,
                                    pair[0], pair[1], values, name, null);
                        else
                            System.out.println("When do I have this case?");
                        drcRules.add(tmp);
                    }
                }
            }
        }
        else
        {
            assert false;
        }
        return true;
    }

    /** Class used to store read rules and foundry associated to them */
    public static class DRCXMLBucket implements Serializable
    {
        public List<DRCTemplate> drcRules = new ArrayList<DRCTemplate>();
        public String foundry = Foundry.Type.NONE.getName();
    }

    /** Public XML Parser for DRC decks **/
    public static class DRCXMLParser
    {
        private List<DRCXMLBucket> rulesList = new ArrayList<DRCXMLBucket>();
        private DRCXMLBucket current = null;
        private boolean fullLoaded = true;
        private String fileName;
        private Collection<String> nodeNamesList = new ArrayList<String>();
        private Collection<String> layerNamesList = new ArrayList<String>();

        public List<DRCXMLBucket> getRules() { return rulesList; }
        public boolean isParseOK() { return fullLoaded; }

        /**
         * Method to parse XML file containing the DRC deck
         * @param fileURL
         * @param verbose
         * @return true if file was loaded without problems
         */
        protected boolean process(URL fileURL, Xml.Technology tech, boolean verbose)
        {
            fileName = TextUtils.getFileNameWithoutExtension(fileURL);
            nodeNamesList = tech.getNodeNames();
            layerNamesList = tech.getLayerNames();
            
            try
            {
                // Factory call
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setValidating(true);
                // create the parser
                SAXParser parser = factory.newSAXParser();
                URLConnection urlCon = fileURL.openConnection();
                InputStream inputStream = urlCon.getInputStream();

                if (verbose) System.out.println("Parsing XML file \"" + fileURL + "\"");

                DRCXMLHandler handler = new DRCXMLHandler();
                parser.parse(inputStream, handler);
                fullLoaded = handler.passed;

                if (verbose) System.out.println("End Parsing XML file ...");
            }
            catch (Exception e)
            {
                if (verbose) e.printStackTrace();
                fullLoaded = false;
            }
            return fullLoaded;
        }

        class DRCXMLHandler extends DefaultHandler
        {
            boolean passed;

            DRCXMLHandler()
            {
                passed = true; // by default there is no error in file
            }

            public InputSource resolveEntity (String publicId, String systemId) throws IOException, SAXException
            {
                URL fileURL = this.getClass().getResource("DRC.dtd");
                URLConnection urlCon = fileURL.openConnection();
                InputStream inputStream = urlCon.getInputStream();
                return new InputSource(inputStream);
            }

            public void startElement (String uri, String localName, String qName, Attributes attributes)
            {
                if (qName.equals("DRCRules"))
                    return;
                if (qName.equals("Foundry"))
                {
                    current = new DRCXMLBucket();
                    rulesList.add(current);
                    current.foundry = attributes.getValue(0);
                    return;
                }
                if (!parseXmlElement(current.drcRules, layerNamesList, nodeNamesList, qName, attributes, localName))
                {
                    passed = false;
                }
            }

            public void fatalError(SAXParseException e)
            {
                System.out.println("Parser Fatal Error: '" + e.getMessage() + "' in line " + e.getLineNumber() + " in '" + fileName + "'.");
                passed = false;
            }

            public void warning(SAXParseException e)
            {
                System.out.println("Parser Warning: '" + e.getMessage() + "' in line " + e.getLineNumber() + " in '" + fileName + "'.");
            }

            public void error(SAXParseException e)
            {
                System.out.println("Parser Error: " + e.getMessage() + "' in line " + e.getLineNumber() + " in '" + fileName + "'.");
                passed = false;
            }
        }
    }
}
