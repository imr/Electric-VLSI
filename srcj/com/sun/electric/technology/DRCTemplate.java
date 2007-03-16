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

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

/**
 * Class to define rules from TSCM files...
 */
public class DRCTemplate implements Serializable
{
    public enum DRCMode
    {
        /** None */                                                         NONE (-1),
        /** always */                                                       ALL (0),
        /** only applies if there are 2 metal layers in process */			M2 (01),
        /** only applies if there are 3 metal layers in process */			M3 (02),
        /** only applies if there are 4 metal layers in process */			M4 (04),
        /** only applies if there are 5 metal layers in process */			M5 (010),
        /** only applies if there are 6 metal layers in process */			M6 (020),
        /** only applies if there are 2-3 metal layers in process */		M23 (03),
        /** only applies if there are 4-6 metal layers in process */		M456 (034),
        /** only applies if there are 5-6 metal layers in process */		M56 (030),
        /** only applies if there are 7 metal layers in process */		    M7 (0100000),
        /** only applies if there are 8 metal layers in process */		    M8 (0200000),
        /** only applies if there are 9 metal layers in process */		    M9 (0400000),

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
    }

    public DRCTemplate(String rule, int when, DRCRuleType ruleType, String name1, String name2, double[] vals, String nodeName)
    {
        this.ruleName = rule;
        this.when = when;
        this.ruleType = ruleType;
        this.name1 = name1;
        this.name2 = name2;
        copyValues(vals);
        this.nodeName = nodeName;
        this.multiCuts = -1; // don't care

        switch (ruleType)
        {
            case SPACING:
            case SURROUND:
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
    public static DRCXMLParser importDRCDeck(URL fileURL, boolean verbose)
    {
        DRCXMLParser parser = new DRCXMLParser();
        parser.process(fileURL, verbose);
        return parser;
    }

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
                out.println("    <Foundry name=\"" + foundry.getType().name() + "\">");

                for (DRCTemplate rule : rules)
                {
                    String whenName = null;
                    for (DRCMode p : DRCMode.values())
                    {
                        if (p == DRCMode.NONE ||
                                p.mode() == Foundry.Type.MOSIS.mode() ||
                                p.mode() == Foundry.Type.TSMC.mode() ||
                                p.mode() == Foundry.Type.ST.mode())
                            continue;
                        if ((p.mode() & rule.when) != 0)
                        {
                            if (whenName == null) // first element
                                whenName = "";
                            else
                                whenName += "|";
                            whenName += p;
                        }
                    }
                    if (whenName == null) whenName = DRCMode.ALL.name();  // When originally it was set to ALL
                    switch(rule.ruleType)
                    {
                        case MINWID:
                        case MINAREA:
                        case MINENCLOSEDAREA:
                            out.println("        <LayerRule ruleName=\"" + rule.ruleName + "\""
                                    + " layerName=\"" + rule.name1 + "\""
                                    + " type=\""+rule.ruleType+"\""
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
                            String noName = (rule.nodeName != null) ? (" nodeName=\"" + rule.nodeName + "\"") : "";
                            String wideValues = (rule.maxWidth > 0) ? (" maxW=\"" + rule.maxWidth + "\""
                                    + " minLen=\"" + rule.minLength + "\"") : "";
                            out.println("        <LayersRule ruleName=\"" + rule.ruleName + "\""
                                    + " layerNames=\"{" + rule.name1 + "," + rule.name2 + "}\""
                                    + " type=\""+rule.ruleType+"\""
                                    + " when=\"" + whenName + "\""
                                    + " value=\"" + rule.getValue(0) + "\""
                                    + wideValues
                                    + noName
                                    + "/>");
                            break;
                        case SURROUND: // if nodeName==null -> LayersRule
                        case ASURROUND:
                            String ruleType = "NodeLayersRule";
                            String value = " value=\"" + rule.getValue(0) + "\"";

                            if (rule.getValue(0) != rule.getValue(1)) // x and y values
                            {
                                value = " valueX=\"" + rule.getValue(0) + "\"" + " valueY=\"" + rule.getValue(1) + "\"";
                            }
                            noName = " nodeName=\"" + rule.nodeName + "\"";
                            if (rule.nodeName == null)  // LayersRule
                            {
                                noName = "";
                                ruleType = "LayersRule";
                            }
                            out.println("        <" + ruleType + " ruleName=\"" + rule.ruleName + "\""
                                    + " layerNames=\"{" + rule.name1 + "," + rule.name2 + "}\""
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
                            out.println("        <NodeRule ruleName=\"" + rule.ruleName + "\""
                                    + " type=\""+rule.ruleType+"\""
                                    + " when=\"" + whenName + "\""
                                    + value
                                    + " nodeName=\"" + rule.nodeName + "\""
                                    + "/>");
                            break;
                        case FORBIDDEN:
                        default:
                            System.out.println("Case not implemented " + rule.ruleType);
                    }
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



    /** Class used to store read rules and foundry associated to them */
    public static class DRCXMLBucket implements Serializable
    {
        public List<DRCTemplate> drcRules = new ArrayList<DRCTemplate>();
        public String foundry = Foundry.Type.NONE.name();
    }

    /** Public XML Parser for DRC decks **/
    public static class DRCXMLParser
    {
        private List<DRCXMLBucket> rulesList = new ArrayList<DRCXMLBucket>();
        private DRCXMLBucket current = null;
        private boolean fullLoaded = true;
        private String fileName;

        public List<DRCXMLBucket> getRules() { return rulesList; }
        public boolean isParseOK() { return fullLoaded; }

        /**
         * Method to parse XML file containing the DRC deck
         * @param fileURL
         * @param verbose
         * @return true if file was loaded without problems
         */
        protected boolean process(URL fileURL, boolean verbose)
        {
            fileName = TextUtils.getFileNameWithoutExtension(fileURL);
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

        private class DRCXMLHandler extends DefaultHandler
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
                if (qName.equals("Foundry"))
                {
                    current = new DRCXMLBucket();
                    rulesList.add(current);
                    current.foundry = attributes.getValue(0);
                    return;
                }
                boolean layerRule = qName.equals("LayerRule");
                boolean layersRule = qName.equals("LayersRule");
                boolean nodeLayersRule = qName.equals("NodeLayersRule");
                boolean nodeRule = qName.equals("NodeRule");

                if (!layerRule && !layersRule && !nodeLayersRule && !nodeRule) return;

                String ruleName = "", layerNames = "", nodeNames = null;
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
                        values[0] = values[1] = Double.parseDouble(attributes.getValue(i));
                    else if (attributes.getQName(i).equals("valueX"))
                        values[0] = Double.parseDouble(attributes.getValue(i));
                    else if (attributes.getQName(i).equals("valueY"))
                        values[1] = Double.parseDouble(attributes.getValue(i));
                    else if (attributes.getQName(i).equals("maxW"))
                        maxW = Double.parseDouble(attributes.getValue(i));
                    else if (attributes.getQName(i).equals("minLen"))
                        minLen = Double.parseDouble(attributes.getValue(i));
                    else
                        new Error("Invalid attribute in DRCXMLParser");
                }

                // They could be several layer names or pairs of names for the same rule
                if (layerRule)
                {
                    String[] layers = TextUtils.parseString(layerNames, ",");
                    for (String layer : layers)
                    {
                        if (nodeNames == null)
                        {
                            DRCTemplate tmp = new DRCTemplate(ruleName, when, type, layer,
                                    null, values, null);
                            current.drcRules.add(tmp);
                        }
                        else
                        {
                            String[] names = TextUtils.parseString(nodeNames, ",");
                            for (String name : names)
                            {
                                DRCTemplate tmp = new DRCTemplate(ruleName, when, type, layer,
                                        null, values, name);
                                current.drcRules.add(tmp);
                            }
                        }
                    }
                }
                else if (nodeRule)
                {
                    if (nodeNames == null)
                    {
                        DRCTemplate tmp = new DRCTemplate(ruleName, when, type, null, null, values, null);
                        current.drcRules.add(tmp);
                    }
                    else
                    {
                        String[] names = TextUtils.parseString(nodeNames, ",");
                        for (String name : names)
                        {
                            DRCTemplate tmp = new DRCTemplate(ruleName, when, type,
                                    null, null, values, name);
                            current.drcRules.add(tmp);
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
                                tmp = new DRCTemplate(ruleName, when, type, pair[0], pair[1], values, null);
                            else
                                tmp = new DRCTemplate(ruleName, when, type, maxW, minLen, pair[0], pair[1], values, -1);
                            current.drcRules.add(tmp);
                        }
                        else
                        {
                            String[] names = TextUtils.parseString(nodeNames, ",");
                            for (String name : names)
                            {
                                DRCTemplate tmp = null;
                                if (maxW == null)
                                    tmp = new DRCTemplate(ruleName, when, type,
                                            pair[0], pair[1], values, name);
                                else
                                    System.out.println("When do I have this case?");
                                current.drcRules.add(tmp);
                            }
                        }
                    }
                }
                else
                {
                    System.out.println("Case not implemented in DRCXMLParser");
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
