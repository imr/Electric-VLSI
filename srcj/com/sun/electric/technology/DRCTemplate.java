package com.sun.electric.technology;

import java.util.List;
import java.util.ArrayList;

/**
* Class to define rules from TSCM files...
*/
public class DRCTemplate
{
    // design rule constants
    
    // the meaning of "when" in the DRC table
    /** always */			                                            public static final int ALL =      0;
    /** only applies if there are 2 metal layers in process */			public static final int M2 =      01;
    /** only applies if there are 3 metal layers in process */			public static final int M3 =      02;
    /** only applies if there are 4 metal layers in process */			public static final int M4 =      04;
    /** only applies if there are 5 metal layers in process */			public static final int M5 =     010;
    /** only applies if there are 6 metal layers in process */			public static final int M6 =     020;
    /** only applies if there are 2-3 metal layers in process */		public static final int M23 =     03;
    /** only applies if there are 2-4 metal layers in process */		public static final int M234 =    07;
    /** only applies if there are 2-5 metal layers in process */		public static final int M2345 =  017;
    /** only applies if there are 4-6 metal layers in process */		public static final int M456 =   034;
    /** only applies if there are 5-6 metal layers in process */		public static final int M56 =    030;
    /** only applies if there are 3-6 metal layers in process */		public static final int M3456 =  036;

    /** only applies if alternate contact rules are in effect */		public static final int AC =     040;
    /** only applies if alternate contact rules are not in effect */	public static final int NAC =   0100;
    /** only applies if stacked vias are allowed */						public static final int SV =    0200;
    /** only applies if stacked vias are not allowed */					public static final int NSV =   0400;
    /** only applies if deep rules are in effect */						public static final int DE =   01000;
    /** only applies if submicron rules are in effect */				public static final int SU =   02000;
    /** only applies if scmos rules are in effect */					public static final int SC =   04000;

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

    public String rule;			/* the name of the rule */
    public int when;				/* when the rule is used */
    public int ruleType;			/* the type of the rule */
    public String layer1, layer2;	/* two layers that are used by the rule */
    public double distance;		/* the spacing of the rule */
    public double maxW;         /* max length where spacing is valid */
    public String nodeName;		/* the node that is used by the rule */

    public DRCTemplate(String rule, int when, int ruleType, String layer1, String layer2, double distance, String nodeName)
    {
        this.rule = rule;
        this.when = when;
        this.ruleType = ruleType;
        this.layer1 = layer1;
        this.layer2 = layer2;
        this.distance = distance;
        this.nodeName = nodeName;

        switch (ruleType)
        {
            case SPACING:
                {
                    if (layer1 == null || layer2 == null)
                    {
                        System.out.println("Error: missing one layer in no '" + rule + "' ");
                    }
                }
            break;
            default:
        }
    }
    // For different spacing depending on wire length
    public DRCTemplate(String rule, int when, int ruleType, double maxW, String layer1, String layer2, double distance)
    {
        this.rule = rule;
        this.when = when;
        this.ruleType = ruleType;
        this.layer1 = layer1;
        this.layer2 = layer2;
        this.distance = distance;
        this.maxW = maxW;

        switch (ruleType)
        {
            case SPACING:
                {
                    if (layer1 == null || layer2 == null)
                    {
                        System.out.println("Error: missing one layer in no '" + rule + "' ");
                    }
                }
            break;
            default:
        }
    }

    public static List makeRuleTemplates(String name, int when, int type, double maxW, double value, String arrayL[])
	{
		// Clone same rule for different layers
		int length = arrayL.length;
		List list = new ArrayList(length);
		for (int i = 0; i < length; i++)
		{
			String layer = arrayL[i];
			DRCTemplate r = new DRCTemplate(name, when, type, maxW, layer, null, value);
			list.add(r);
		}
		return list;
	}

    public static List makeRuleTemplates(String name, int when, int type, double maxW, double value, String arrayL[][])
	{
		// Clone same rule for different layers
		int length = arrayL.length;
		List list = new ArrayList(length);
		for (int i = 0; i < length; i++)
		{
			String []layers = arrayL[i];
			if (layers.length != 2)
				System.out.println("Invalid number of layers in DRC::makeRuleTemplates");
			DRCTemplate r = new DRCTemplate(name, when, type, maxW, layers[0], layers[1], value);
			list.add(r);
		}
		return list;
	}

    // For primitive node rules
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
}
