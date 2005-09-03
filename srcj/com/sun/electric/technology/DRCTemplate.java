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

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Class to define rules from TSCM files...
 */
public class DRCTemplate
{
    // design rule constants
    
    // the meaning of "when" in the DRC table
    /** None */                                                         public static final int NONE =    -1;
    /** always */			                                            public static final int ALL =      0;
    /** only applies if there are 2 metal layers in process */			public static final int M2 =      01;
    /** only applies if there are 3 metal layers in process */			public static final int M3 =      02;
    /** only applies if there are 4 metal layers in process */			public static final int M4 =      04;
    /** only applies if there are 5 metal layers in process */			public static final int M5 =     010;
    /** only applies if there are 6 metal layers in process */			public static final int M6 =     020;
    /** only applies if there are 2-3 metal layers in process */		public static final int M23 =     03;
//    /** only applies if there are 2-4 metal layers in process */		public static final int M234 =    07;
//    /** only applies if there are 2-5 metal layers in process */		public static final int M2345 =  017;
    /** only applies if there are 4-6 metal layers in process */		public static final int M456 =   034;
    /** only applies if there are 5-6 metal layers in process */		public static final int M56 =    030;
//    /** only applies if there are 3-6 metal layers in process */		public static final int M3456 =  036;

    /** only applies if alternate contact rules are in effect */		public static final int AC =     040;
    /** only applies if alternate contact rules are not in effect */	public static final int NAC =   0100;
    /** only applies if stacked vias are allowed */						public static final int SV =    0200;
    /** only applies if stacked vias are not allowed */					public static final int NSV =   0400;
    /** only applies if deep rules are in effect */						public static final int DE =   01000;
    /** only applies if submicron rules are in effect */				public static final int SU =   02000;
    /** only applies if scmos rules are in effect */					public static final int SC =   04000;
    /** only for TSMC technology */                                     public static final int TSMC = 010000;
    /** only for ST technology */                                       public static final int ST =   020000;
    /** for MOSIS technology */                                         public static final int MOSIS =040000;


    // the meaning of "ruletype" in the DRC table
    /** a minimum-width rule */			public static final int MINWID =     1;
    /** a node size rule */				public static final int NODSIZ =     2;
    /** a general surround rule */		public static final int SURROUND =   3;
    /** a via surround rule */			public static final int VIASUR =     4;
    /** a transistor well rule */		public static final int TRAWELL =    5;
    /** a transistor poly rule */		public static final int TRAPOLY =    6;
    /** a transistor active rule */		public static final int TRAACTIVE =  7;
    /** a spacing rule */				public static final int SPACING =    8;
    /** a multi-cut spacing rule */		public static final int SPACINGM =   9;
    /** a wide spacing rule */			public static final int SPACINGW =  10;
    /** an edge spacing rule */			public static final int SPACINGE =  11;
    /** a connected spacing rule */		public static final int CONSPA =    12;
    /** an unconnected spacing rule */	public static final int UCONSPA =   13;
    /** a contact cut spacing rule */	public static final int CUTSPA =    14;
    /** 2D contact cut spacing rule */	public static final int CUTSPA2D =  15;
    /** a contact cut size rule */		public static final int CUTSIZE =   16;
    /** a contact cut surround rule */	public static final int CUTSUR =    17;
    /** X contact cut surround rule */	public static final int CUTSURX =    18;
    /** Y contact cut surround rule */	public static final int CUTSURY =    19;
    /** arc surround rule */			public static final int ASURROUND = 20;
    /** minimum area rule */			public static final int AREA = 21;
    /** enclosed area rule */			public static final int ENCLOSEDAREA = 22;
	/** extension rule */               public static final int EXTENSION = 23;
    /** forbidden rule */               public static final int FORBIDDEN = 24;
    /** layer combination rule */       public static final int COMBINATION = 25;
    /** extension gate rule */          public static final int EXTENSIONGATE = 26;
    /** slot size rule */               public static final int SLOTSIZE = 26;

    // For sorting
    public static final DRCTemplateSort templateSort = new DRCTemplateSort();

    public String ruleName;			/* the name of the rule */
    public int when;				/* when the rule is used */
    public int ruleType;			/* the type of the rule */
    public String name1, name2;	/* two layers/nodes that are used by the rule */
    public double value1;		/* value1 is distance for spacing rule or width for node rule */
    public double value2;		/* value1 is height for node rule */
    public double maxWidth;         /* max length where spacing is valid */
    public double minLength;       /* min paralell distance for spacing rule */
    public String nodeName;		/* the node that is used by the rule */
	public int multiCuts;         /* -1=dont care, 0=no cuts, 1=with cuts multi cut rule */


    public DRCTemplate(String rule, int when, int ruleType, String name1, String name2, double distance, String nodeName)
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
    public DRCTemplate(String rule, int when, int ruleType, double maxW, double minLen, double distance, int multiCut)
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
    public DRCTemplate(String rule, int when, int ruleType, double maxW, double minLen, String name1, String name2, double distance, int multiCut)
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
    public static List makeRuleTemplates(String name, int when, int type, double maxW, double minLen, double value, String arrayL[])
	{
		// Clone same rule for different layers
		int length = arrayL.length;
		List list = new ArrayList(length);
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
    public static List makeRuleTemplates(String[] names, int[] when, int type, double value, String matrix[][])
	{
        List list = new ArrayList(names.length * matrix.length);

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
    public static List makeRuleTemplates(String[] names, int[] when, int type, double maxW, double value, String arrayL[][])
	{
        List list = new ArrayList(names.length);

        for (int i = 0; i < names.length; i++)
        {
            list.addAll(makeRuleTemplates(names[i], when[i], type, maxW, 0, value, arrayL, -1));
        }
		return list;
	}

	/**
	 * For multi cuts as well.
	 */
    public static List makeRuleTemplates(String name, int when, int type, double maxW, double minLen, double value, String arrayL[][], int multiCut)
	{
		// Clone same rule for different layers
		int l = arrayL.length;
		List list = new ArrayList(l);
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
	public static List makeRuleTemplates(String name, int when, int type, double value, String arrayL[])
	{
		// Clone same rule for different layers
		int length = arrayL.length;
		List list = new ArrayList(length);
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
    public static class DRCTemplateSort implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            double bb1 = ((DRCTemplate)o1).value1;
            double bb2 = ((DRCTemplate)o2).value1;

            if (bb1 < bb2) return -1;
            else if (bb1 > bb2) return 1;
            return (0); // identical
        }
    }
}
