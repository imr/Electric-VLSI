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
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;

/**
 * Class to define rules from TSCM files...
 */
public class DRCTemplate
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

        /** only applies if alternate contact rules are in effect */		AC (040),
        /** only applies if alternate contact rules are not in effect */	NAC (0100),
        /** only applies if stacked vias are allowed */						SV (0200),
        /** only applies if stacked vias are not allowed */					NSV (0400),
        /** only applies if deep rules are in effect */						DE (01000),
        /** only applies if submicron rules are in effect */				SU (02000),
        /** only applies if scmos rules are in effect */					SC (04000),
        /** only for TSMC technology */                                     TSMC (010000),
        /** only for ST technology */                                       ST (020000),
        /** only for MOSIS technology */                                    MOSIS (040000);

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
        /** nothing chosen */			    NONE (-1),
        /** a minimum-width rule */			MINWID (1),
        /** a node size rule */				NODSIZ (2),
        /** a general surround rule */		SURROUND (3),
        /** a via surround rule */			VIASUR (4),
        /** a transistor well rule */		TRAWELL (5),
        /** a transistor poly rule */		TRAPOLY (6),
        /** a transistor active rule */		TRAACTIVE (7),
        /** a spacing rule */				SPACING (8),
        /** a multi-cut spacing rule */		SPACINGM (9),
        /** a wide spacing rule */			SPACINGW (10),
        /** an edge spacing rule */			SPACINGE (11),
        /** a connected spacing rule */		CONSPA (12),
        /** an unconnected spacing rule */	UCONSPA (13),
        /** a contact cut spacing rule */	CUTSPA (14),
        /** 2D contact cut spacing rule */	CUTSPA2D (15),
        /** a contact cut size rule */		CUTSIZE (16),
        /** a contact cut surround rule */	CUTSUR (17),
        /** X contact cut surround rule */	CUTSURX (18),
        /** Y contact cut surround rule */	CUTSURY (19),
        /** arc surround rule */			ASURROUND (20),
        /** minimum area rule */			MINAREA (21),
        /** enclosed area rule */			ENCLOSEDAREA (22),
        /** extension rule */               EXTENSION (23),
        /** forbidden rule */               FORBIDDEN (24),
        /** layer combination rule */       COMBINATION (25),
        /** extension gate rule */          EXTENSIONGATE (26),
        /** slot size rule */               SLOTSIZE (27);

        private final int mode;

        DRCRuleType(int mode)
        {
            this.mode = mode;
        }
        public int mode() { return this.mode; }
    }

    // For sorting
    public static final DRCTemplateSort templateSort = new DRCTemplateSort();

    public String ruleName;			/* the name of the rule */
    public int when;				/* when the rule is used */
    public DRCRuleType ruleType;			/* the type of the rule */
    public String name1, name2;	/* two layers/nodes that are used by the rule */
    public double value1;		/* value1 is distance for spacing rule or width for node rule */
    public double value2;		/* value1 is height for node rule */
    public double maxWidth;         /* max length where spacing is valid */
    public double minLength;       /* min paralell distance for spacing rule */
    public String nodeName;		/* the node that is used by the rule */
	public int multiCuts;         /* -1=dont care, 0=no cuts, 1=with cuts multi cut rule */


    public DRCTemplate(String rule, int when, DRCRuleType ruleType, String name1, String name2, double distance, String nodeName)
    {
        this.ruleName = rule;
        this.when = when;
        this.ruleType = ruleType;
        this.name1 = name1;
        this.name2 = name2;
        this.value1 = distance;
        this.nodeName = nodeName;

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
	 * For different spacing depending on wire length and multi cuts.
	 */
    public DRCTemplate(String rule, int when, DRCRuleType ruleType, double maxW, double minLen, double distance, int multiCut)
    {
        this.ruleName = rule;
        this.when = when;
        this.ruleType = ruleType;
        this.value1 = distance;
        this.maxWidth = maxW;
        this.minLength = minLen;
		this.multiCuts = multiCut;
    }

	/**
	 * For different spacing depending on wire length and multi cuts.
	 */
    public DRCTemplate(String rule, int when, DRCRuleType ruleType, double maxW, double minLen, String name1, String name2, double distance, int multiCut)
    {
        this.ruleName = rule;
        this.when = when;
        this.ruleType = ruleType;
        this.name1 = name1;
        this.name2 = name2;
        this.value1 = distance;
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
	 * Method for spacing rules in single layers.
	 */
    public static List<DRCTemplate> makeRuleTemplates(String name, int when, DRCRuleType type, double maxW, double minLen,
                                                      double value, String arrayL[])
	{
		// Clone same rule for different layers
		int length = arrayL.length;
		List<DRCTemplate> list = new ArrayList<DRCTemplate>(length);
		for (int i = 0; i < length; i++)
		{
			String layer = arrayL[i];
			DRCTemplate r = new DRCTemplate(name, when, type, maxW, minLen, layer, null, value, -1);
			list.add(r);
		}
		return list;
	}

    /**
     *  Create same rules for different foundries. In this case, primitive nodes are involved
     *  Matrix contains triple pair: layer1, layer2, primitiveNode
     */
    public static List<DRCTemplate> makeRuleTemplates(String[] names, int[] when, DRCRuleType type, double value, String matrix[][])
	{
        List<DRCTemplate> list = new ArrayList<DRCTemplate>(names.length * matrix.length);

        for (int i = 0; i < names.length; i++)
        {
            for (int j = 0; j < matrix.length; j++)
            {
                DRCTemplate r = new DRCTemplate(names[i], when[i], type, matrix[j][0], matrix[j][1], value,
                        (matrix[j].length>2)?matrix[j][2]:null);
			    list.add(r);
            }
        }
		return list;
	}

    /**
     * For same rules but with different names depending on the foundry
     */
    public static List<DRCTemplate> makeRuleTemplates(String[] names, int[] when, DRCRuleType type, double maxW, double value, String arrayL[][])
	{
        List<DRCTemplate> list = new ArrayList<DRCTemplate>(names.length);

        for (int i = 0; i < names.length; i++)
        {
            list.addAll(makeRuleTemplates(names[i], when[i], type, maxW, 0, value, arrayL, -1));
        }
		return list;
	}

	/**
	 * For multi cuts as well.
	 */
    public static List<DRCTemplate> makeRuleTemplates(String name, int when, DRCRuleType type, double maxW, double minLen, double value, String arrayL[][], int multiCut)
	{
		// Clone same rule for different layers
		int l = arrayL.length;
		List<DRCTemplate> list = new ArrayList<DRCTemplate>(l);
		for (int i = 0; i < l; i++)
		{
			String []layers = arrayL[i];
			if (layers.length != 2)
				System.out.println("Invalid number of layers in DRC::makeRuleTemplates");
			DRCTemplate r = new DRCTemplate(name, when, type, maxW, minLen, layers[0], layers[1], value, multiCut);
			list.add(r);
		}
		return list;
	}

    /**
     * For primitive node rules.
     */
	public static List<DRCTemplate> makeRuleTemplates(String name, int when, DRCRuleType type, double value, String arrayL[])
	{
		// Clone same rule for different layers
		int length = arrayL.length;
		List<DRCTemplate> list = new ArrayList<DRCTemplate>(length);
		for (int i = 0; i < length; i++)
		{
			String primitiveNode = arrayL[i];
			DRCTemplate r = new DRCTemplate(name, when, type, null, null, value, primitiveNode);
			list.add(r);
		}
		return list;
	}

    /**
     * Auxiliar class to sort areas in array
     */
    public static class DRCTemplateSort implements Comparator<DRCTemplate>
    {
/*5*/   public int compare(DRCTemplate d1, DRCTemplate d2)
//4*/   public int compare(Object o1, Object o2)
        {
/*5*/       double bb1 = d1.value1;
/*5*/       double bb2 = d2.value1;
//4*/       double bb1 = ((DRCTemplate)o1).value1;
//4*/       double bb2 = ((DRCTemplate)o2).value1;

            if (bb1 < bb2) return -1;
            else if (bb1 > bb2) return 1;
            return (0); // identical
        }
    }

    public static void importDRCDeck(String fileName, Technology tech)
    {
        DRCXMLParser parser = new DRCXMLParser();
        List<DRCTemplate> rules = parser.process(TextUtils.makeURLToFile(fileName));
        tech.setState(rules);
    }

    /**
     * Method to determine if rule is valid for this foundry, regarless if the bit is turn on
     * @param tech
     * @param foundry
     * @param rule
     * @return true if the rule is valid under this foundry
     */
    private static boolean isRuleValidInFoundry(Technology tech, DRCMode foundry, DRCTemplate rule)
    {
        // Direct reference in rule, then rule is valid
        if ((rule.when & foundry.mode()) != 0) return true;
        // if not direct reference, see if rule is for another foundry. If yes, then rule is not valid
        List<DRCMode> list = tech.getFactories();
        for (int i = 0; i < list.size(); i++)
        {
            DRCMode m = list.get(i);
            if (m == foundry) continue;
            if ((rule.when & m.mode()) != 0) return false; // belong to another foundry
        }
        return true;
    }

    public static void exportDRCDeck(String fileName, Technology tech)
    {
        DRCTemplate[] rules = tech.getDRCDeck();
        DRCMode foundry = tech.getFoundry();

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
            out.println("\t<Foundry name=\"" + foundry.name() + "\">");

            for (int i = 0; i < rules.length; i++)
            {
//                if ((rules[i].when & foundry.mode()) == 0) continue;
                if (!isRuleValidInFoundry(tech, foundry, rules[i])) continue;

                String whenName = null;
                for (DRCMode p : DRCMode.values())
                {
                    if (p == DRCMode.NONE || p == DRCMode.MOSIS || p == DRCMode.TSMC || p == DRCMode.ST) continue;
                    if ((p.mode() & rules[i].when) != 0)
                    {
                        if (whenName == null) // first element
                            whenName = "";
                        else
                            whenName += "|";
                        whenName += p;
                    }
                }
                if (whenName == null) whenName = DRCMode.ALL.name();  // When originally it was set to ALL
                switch(rules[i].ruleType)
                {
                    case MINWID:
                    case MINAREA:
                        out.println("\t\t<LayerRule ruleName=\"" + rules[i].ruleName + "\""
                                + " layerName=\"" + rules[i].name1 + "\""
                                + " type=\""+rules[i].ruleType+"\""
                                + " when=\"" + whenName + "\""
                                + " value=\"" + rules[i].value1 + "\""
                                + "/>");
                        break;
                    case VIASUR:
                        out.println("\t\t<LayerRule ruleName=\"" + rules[i].ruleName + "\""
                                + " layerName=\"" + rules[i].name1 + "\""
                                + " type=\""+rules[i].ruleType+"\""
                                + " when=\"" + whenName + "\""
                                + " value=\"" + rules[i].value1 + "\""
                                + " nodeName=\"" + rules[i].nodeName + "\""
                                + "/>");
                        break;
                    case UCONSPA:
                    case CONSPA:
                    case SPACING:
                    case SPACINGM:
                    case SPACINGE:
                        String noName = (rules[i].nodeName != null) ? (" nodeName=\"" + rules[i].nodeName + "\"") : "";
                        out.println("\t\t<LayersRule ruleName=\"" + rules[i].ruleName + "\""
                                + " layerNames=\"{" + rules[i].name1 + "," + rules[i].name2 + "}\""
                                + " type=\""+rules[i].ruleType+"\""
                                + " when=\"" + whenName + "\""
                                + " value=\"" + rules[i].value1 + "\""
                                + noName
                                + "/>");
                        break;
                    case SPACINGW:
                        out.println("\t\t<LayersRule ruleName=\"" + rules[i].ruleName + "\""
                                + " layerNames=\"{" + rules[i].name1 + "," + rules[i].name2 + "}\""
                                + " type=\""+rules[i].ruleType+"\""
                                + " when=\"" + whenName + "\""
                                + " value=\"" + rules[i].value1 + "\""
                                + " maxW=\"" + rules[i].maxWidth + "\""
                                + " minLen=\"" + rules[i].minLength + "\""
                                + "/>");
                        break;
                    case SURROUND:
                    case ASURROUND:
                        out.println("\t\t<NodeLayersRule ruleName=\"" + rules[i].ruleName + "\""
                                + " layerNames=\"{" + rules[i].name1 + "," + rules[i].name2 + "}\""
                                + " type=\""+rules[i].ruleType+"\""
                                + " when=\"" + whenName + "\""
                                + " value=\"" + rules[i].value1 + "\""
                                + " nodeName=\"" + rules[i].nodeName + "\""
                                + "/>");
                        break;
                    case TRAWELL:
                    case TRAPOLY:
                    case TRAACTIVE:
                        out.println("\t\t<NodeRule ruleName=\"" + rules[i].ruleName + "\""
                                + " type=\""+rules[i].ruleType+"\""
                                + " when=\"" + whenName + "\""
                                + " value=\"" + rules[i].value1 + "\""
                                + "/>");
                        break;
                    case NODSIZ:
                    case CUTSUR:
                    case CUTSPA:
                    case CUTSPA2D:
                    case CUTSIZE:
                        out.println("\t\t<NodeRule ruleName=\"" + rules[i].ruleName + "\""
                                + " type=\""+rules[i].ruleType+"\""
                                + " when=\"" + whenName + "\""
                                + " value=\"" + rules[i].value1 + "\""
                                + " nodeName=\"" + rules[i].nodeName + "\""
                                + "/>");
                        break;
                    default:
                        System.out.println("Case not implemented " + rules[i].ruleType);
                        ;
                }
            }
            out.println("\t</Foundry>");
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
     * @return
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
}
