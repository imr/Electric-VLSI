/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MoCMOS.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.technology.technologies;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.DRCRules;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.Foundry;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.TechFactory;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.XMLRules;
import com.sun.electric.technology.Xml;
import com.sun.electric.technology.Technology.NodeLayer;
import com.sun.electric.tool.user.User;

import java.io.PrintWriter;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This is the MOSIS CMOS technology.
 */
public class MoCMOS extends Technology
{
	/** Value for standard SCMOS rules. */		public static final int SCMOSRULES = 0;
	/** Value for submicron rules. */			public static final int SUBMRULES  = 1;
	/** Value for deep rules. */				public static final int DEEPRULES  = 2;

	/** key of Variable for saving technology state. */
	public static final Variable.Key TECH_LAST_STATE = Variable.newKey("TECH_last_state");

    public static final Version changeOfMetal6 = Version.parseVersion("8.02o"); // Fix of bug #357

    private static final String TECH_NAME = "mocmos";
    private static final String XML_PREFIX = TECH_NAME + ".";
    private static final String PREF_PREFIX = "/com/sun/electric/technology/technologies/";
    private static final TechFactory.Param techParamRuleSet =
            new TechFactory.Param(XML_PREFIX + "MOCMOS Rule Set", PREF_PREFIX + "MoCMOSRuleSet", Integer.valueOf(1));
     private static final TechFactory.Param techParamNumMetalLayers =
            new TechFactory.Param(XML_PREFIX + "NumMetalLayers",  PREF_PREFIX + TECH_NAME + "NumberOfMetalLayers", Integer.valueOf(6));
    private static final TechFactory.Param techParamUseSecondPolysilicon =
            new TechFactory.Param(XML_PREFIX +"UseSecondPolysilicon", PREF_PREFIX + TECH_NAME + "SecondPolysilicon", Boolean.TRUE);
    private static final TechFactory.Param techParamDisallowStackedVias =
            new TechFactory.Param(XML_PREFIX + "DisallowStackedVias", PREF_PREFIX + "MoCMOSDisallowStackedVias", Boolean.FALSE);
    private static final TechFactory.Param techParamUseAlternativeActivePolyRules =
            new TechFactory.Param(XML_PREFIX + "UseAlternativeActivePolyRules", PREF_PREFIX + "MoCMOSAlternateActivePolyRules", Boolean.FALSE);
    private static final TechFactory.Param techParamAnalog =
            new TechFactory.Param(XML_PREFIX + "Analog", PREF_PREFIX + TECH_NAME + "Analog", Boolean.FALSE);

    // Tech params
    private Integer paramRuleSet;
    private Boolean paramUseSecondPolysilicon;
    private Boolean paramDisallowStackedVias;
    private Boolean paramUseAlternativeActivePolyRules;
    private Boolean paramAnalog;

	// nodes. Storing nodes only whe they are need in outside the constructor
    /** metal-1-P/N-active-contacts */          private PrimitiveNode[] metalActiveContactNodes = new PrimitiveNode[2];
    /** Scalable Transistors */			        private PrimitiveNode[] scalableTransistorNodes = new PrimitiveNode[2];

	// -------------------- private and protected methods ------------------------

    public MoCMOS(Generic generic, TechFactory techFactory, Map<TechFactory.Param,Object> techParams, Xml.Technology t) {
        super(generic, techFactory, techParams, t);

        paramRuleSet = (Integer)techParams.get(techParamRuleSet);
        paramNumMetalLayers = (Integer)techParams.get(techParamNumMetalLayers);
        paramUseSecondPolysilicon = (Boolean)techParams.get(techParamUseSecondPolysilicon);
        paramDisallowStackedVias = (Boolean)techParams.get(techParamDisallowStackedVias);
        paramUseAlternativeActivePolyRules = (Boolean)techParams.get(techParamUseAlternativeActivePolyRules);
        paramAnalog = (Boolean)techParams.get(techParamAnalog);

		setStaticTechnology();
        setFactoryResolution(0.01); // value in lambdas   0.005um -> 0.05 lambdas
        // Logical Effort Tech-dependent settings
        setFactoryLESettings(0.167, 0.16, 0.7);

		//**************************************** NODES ****************************************

        metalActiveContactNodes[P_TYPE] = findNodeProto("Metal-1-P-Active-Con");
		metalActiveContactNodes[N_TYPE] = findNodeProto("Metal-1-N-Active-Con");

        scalableTransistorNodes[P_TYPE] = findNodeProto("P-Transistor-Scalable");
        scalableTransistorNodes[N_TYPE] = findNodeProto("N-Transistor-Scalable");

        for (Iterator<Layer> it = getLayers(); it.hasNext(); ) {
            Layer layer = it.next();
            if (!layer.getFunction().isUsed(getNumMetals(), isSecondPolysilicon() ? 2 : 1))
                layer.getPureLayerNode().setNotUsed(true);
        }
        findNodeProto("P-Transistor-Scalable").setCanShrink();
        findNodeProto("N-Transistor-Scalable").setCanShrink();
        findNodeProto("NPN-Transistor").setCanShrink();

    }

	/******************** SUPPORT METHODS ********************/

    @Override
    protected void copyState(Technology that) {
        super.copyState(that);
        MoCMOS mocmos = (MoCMOS)that;
        paramRuleSet = mocmos.paramRuleSet;
        paramNumMetalLayers = mocmos.paramNumMetalLayers;
        paramUseSecondPolysilicon = mocmos.paramUseSecondPolysilicon;
        paramDisallowStackedVias = mocmos.paramDisallowStackedVias;
        paramUseAlternativeActivePolyRules = mocmos.paramUseAlternativeActivePolyRules;
        paramAnalog = mocmos.paramAnalog;
    }

    @Override
    protected void dumpExtraProjectSettings(PrintWriter out, Map<Setting,Object> settings) {
        printlnSetting(out, settings, getRuleSetSetting());
        printlnSetting(out, settings, getSecondPolysiliconSetting());
        printlnSetting(out, settings, getDisallowStackedViasSetting());
        printlnSetting(out, settings, getAlternateActivePolyRulesSetting());
        printlnSetting(out, settings, getAnalogSetting());
    }

	/******************** SCALABLE TRANSISTOR DESCRIPTION ********************/

	private static final int SCALABLE_ACTIVE_TOP = 0;
	private static final int SCALABLE_METAL_TOP  = 1;
	private static final int SCALABLE_ACTIVE_BOT = 2;
	private static final int SCALABLE_METAL_BOT  = 3;
	private static final int SCALABLE_ACTIVE_CTR = 4;
	private static final int SCALABLE_POLY       = 5;
	private static final int SCALABLE_WELL       = 6;
	private static final int SCALABLE_SUBSTRATE  = 7;
	private static final int SCALABLE_TOTAL      = 8;

	/**
	 * Method to return a list of Polys that describe a given NodeInst.
	 * This method overrides the general one in the Technology object
	 * because of the unusual primitives in this Technology.
	 * @param ni the NodeInst to describe.
	 * @param electrical true to get the "electrical" layers.
	 * This makes no sense for Schematics primitives.
	 * @param reasonable true to get only a minimal set of contact cuts in large contacts.
	 * This makes no sense for Schematics primitives.
	 * @param primLayers an array of NodeLayer objects to convert to Poly objects.
	 * @param layerOverride the layer to use for all generated polygons (if not null).
	 * @return an array of Poly objects.
	 */
    @Override
	protected Poly [] getShapeOfNode(NodeInst ni, boolean electrical, boolean reasonable, Technology.NodeLayer [] primLayers, Layer layerOverride)
	{
		NodeProto prototype = ni.getProto();
		if (scalableTransistorNodes != null && (prototype == scalableTransistorNodes[P_TYPE] || prototype == scalableTransistorNodes[N_TYPE]))
            return getShapeOfNodeScalable(ni, null, reasonable);

        // Default
        return super.getShapeOfNode(ni, electrical, reasonable, primLayers, layerOverride);
    }

    /**
     * Special getShapeOfNode function for scalable transistors
     * @param ni
     * @param context
     * @param reasonable
     * @return Array of Poly containing layers representing a Scalable Transistor
     */
    private Poly [] getShapeOfNodeScalable(NodeInst ni, VarContext context, boolean reasonable)
    {
		// determine special configurations (number of active contacts, inset of active contacts)
		int numContacts = 2;
		boolean insetContacts = false;
		String pt = ni.getVarValue(TRANS_CONTACT, String.class);
		if (pt != null)
		{
			for(int i=0; i<pt.length(); i++)
			{
				char chr = pt.charAt(i);
				if (chr == '0' || chr == '1' || chr == '2')
				{
					numContacts = chr - '0';
				} else if (chr == 'i' || chr == 'I') insetContacts = true;
			}
		}
		int boxOffset = 4 - numContacts * 2;

		// determine width
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		double nodeWid = ni.getXSize();
		double activeWid = nodeWid - 14;
		int extraInset = 0;
		Variable var = ni.getVar(Schematics.ATTR_WIDTH);
		if (var != null)
		{
			VarContext evalContext = context;
			if (evalContext == null) evalContext = VarContext.globalContext;
			String extra = var.describe(evalContext, ni);
			Object o = evalContext.evalVar(var, ni);
			if (o != null) extra = o.toString();
			double requestedWid = TextUtils.atof(extra);
			if (requestedWid > activeWid)
			{
				System.out.println("Warning: " + ni.getParent() + ", " +
					ni + " requests width of " + requestedWid + " but is only " + activeWid + " wide");
			}
			if (requestedWid < activeWid && requestedWid > 0)
			{
				extraInset = (int)((activeWid - requestedWid) / 2);
				activeWid = requestedWid;
			}
		}
		double actInset = (nodeWid-activeWid) / 2;
        double gateOverhang = getTransistorExtension(np, true, cachedRules);
        double polyInset = actInset - gateOverhang; // cachedRules.getPolyOverhang();
		double actContInset = 7 + extraInset;

		// contacts must be 5 wide at a minimum
		if (activeWid < 5) actContInset -= (5-activeWid)/2;
		double metContInset = actContInset + 0.5;

		// determine the multicut information
        NodeLayer activeMulticut = metalActiveContactNodes[P_TYPE].findMulticut();
        NodeLayer activeSurround = metalActiveContactNodes[P_TYPE].getLayers()[1];
        assert activeSurround.getLayer().getFunction().isDiff();
        double cutSize = activeMulticut.getMulticutSizeX();
        assert cutSize == activeMulticut.getMulticutSizeY();
        double cutIndent = activeMulticut.getLeftEdge().getAdder() - activeSurround.getLeftEdge().getAdder();
        double cutSep = activeMulticut.getMulticutSep1D();
        assert cutSep == activeMulticut.getMulticutSep2D();
//		double [] specialValues = metalActiveContactNodes[P_TYPE].getSpecialValues();
//		double cutSize = specialValues[0];
//		double cutIndent = specialValues[2];   // or specialValues[3]
//		double cutSep = specialValues[4];      // or specialValues[5]
		int numCuts = (int)((activeWid-cutIndent*2+cutSep) / (cutSize+cutSep));
		if (numCuts <= 0) numCuts = 1;
		double cutBase = 0;
		if (numCuts != 1)
			cutBase = (activeWid-cutIndent*2 - cutSize*numCuts -
				cutSep*(numCuts-1)) / 2 + (nodeWid-activeWid)/2 + cutIndent;

		// now compute the number of polygons
		int extraCuts = numCuts*2 - (2-numContacts) * numCuts;
		Technology.NodeLayer [] layers = np.getLayers();
		int count = SCALABLE_TOTAL + extraCuts - boxOffset;
		Technology.NodeLayer [] newNodeLayers = new Technology.NodeLayer[count];

		// load the basic layers
		int fillIndex = 0;
		for(int box = boxOffset; box < SCALABLE_TOTAL; box++)
		{
			TechPoint [] oldPoints = layers[box].getPoints();
			TechPoint [] points = new TechPoint[oldPoints.length];
			for(int i=0; i<oldPoints.length; i++) points[i] = oldPoints[i].duplicate();
			switch (box)
			{
				case SCALABLE_ACTIVE_CTR:		// active that passes through gate
					points[0].getX().setAdder(actInset);
					points[0].getX().setAdder(actInset);
					points[1].getX().setAdder(-actInset);
					break;
				case SCALABLE_ACTIVE_TOP:		// active surrounding contacts
				case SCALABLE_ACTIVE_BOT:
					points[0].getX().setAdder(actContInset);
					points[1].getX().setAdder(-actContInset);
					if (insetContacts)
					{
						double shift = 0.5;
						if (points[0].getY().getAdder() < 0) shift = -0.5;
						points[0].getY().setAdder(points[0].getY().getAdder() + shift);
						points[1].getY().setAdder(points[1].getY().getAdder() + shift);
					}
					break;
				case SCALABLE_POLY:				// poly
					points[0].getX().setAdder(polyInset);
					points[1].getX().setAdder(-polyInset);
					break;
				case SCALABLE_METAL_TOP:		// metal surrounding contacts
				case SCALABLE_METAL_BOT:
					points[0].getX().setAdder(metContInset);
					points[1].getX().setAdder(-metContInset);
					if (insetContacts)
					{
						double shift = 0.5;
						if (points[0].getY().getAdder() < 0) shift = -0.5;
						points[0].getY().setAdder(points[0].getY().getAdder() + shift);
						points[1].getY().setAdder(points[1].getY().getAdder() + shift);
					}
					break;
				case SCALABLE_WELL:				// well and select
				case SCALABLE_SUBSTRATE:
					if (insetContacts)
					{
						points[0].getY().setAdder(points[0].getY().getAdder() + 0.5);
						points[1].getY().setAdder(points[1].getY().getAdder() - 0.5);
					}
					break;
			}
			newNodeLayers[fillIndex] = new Technology.NodeLayer(layers[box].getLayer(), layers[box].getPortNum(),
				layers[box].getStyle(), layers[box].getRepresentation(), points);
			fillIndex++;
		}

		// load the contact cuts
		for(int box = 0; box < extraCuts; box++)
		{
			int oldIndex = SCALABLE_TOTAL;
			if (box >= numCuts) oldIndex++;

			// make a new description of this layer
			TechPoint [] oldPoints = layers[oldIndex].getPoints();
			TechPoint [] points = new TechPoint[oldPoints.length];
			for(int i=0; i<oldPoints.length; i++) points[i] = oldPoints[i].duplicate();
			if (numCuts == 1)
			{
				points[0].getX().setAdder(ni.getXSize() / 2 - cutSize/2);
				points[1].getX().setAdder(ni.getXSize() / 2 + cutSize/2);
			} else
			{
				int cut = box % numCuts;
				double base = cutBase + cut * (cutSize + cutSep);
				points[0].getX().setAdder(base);
				points[1].getX().setAdder(base + cutSize);
			}
			if (insetContacts)
			{
				double shift = 0.5;
				if (points[0].getY().getAdder() < 0) shift = -0.5;
				points[0].getY().setAdder(points[0].getY().getAdder() + shift);
				points[1].getY().setAdder(points[1].getY().getAdder() + shift);
			}
			newNodeLayers[fillIndex] = new Technology.NodeLayer(layers[oldIndex].getLayer(), layers[oldIndex].getPortNum(),
				layers[oldIndex].getStyle(), layers[oldIndex].getRepresentation(), points);
			fillIndex++;
		}

		// now let the superclass convert it to Polys
		return super.getShapeOfNode(ni, false, reasonable, newNodeLayers, null);
	}

	/******************** PARAMETERIZABLE DESIGN RULES ********************/

	/**
	 * Method to build "factory" design rules, given the current technology settings.
	 * @return the "factory" design rules for this Technology.
	 * Returns null if there is an error loading the rules.
     */
    @Override
	protected XMLRules makeFactoryDesignRules()
	{
        Foundry foundry = getSelectedFoundry();
        List<DRCTemplate> theRules = foundry.getRules();
        XMLRules rules = new XMLRules(this);
        boolean pWellProcess = User.isPWellProcessLayoutTechnology();

        assert(foundry != null);

		// load the DRC tables from the explanation table
        int numMetals = getNumMetals();
        int rulesMode = getRuleSet();

		for(int pass=0; pass<2; pass++)
		{
			for(DRCTemplate rule : theRules)
			{
                // see if the rule applies
				if (pass == 0)
				{
					if (rule.ruleType == DRCTemplate.DRCRuleType.NODSIZ) continue;
				} else
				{
					if (rule.ruleType != DRCTemplate.DRCRuleType.NODSIZ) continue;
				}

				int when = rule.when;
                boolean goodrule = true;
				if ((when&(DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode())) != 0)
				{
					switch (rulesMode)
					{
						case DEEPRULES:  if ((when&DRCTemplate.DRCMode.DE.mode()) == 0) goodrule = false;   break;
						case SUBMRULES:  if ((when&DRCTemplate.DRCMode.SU.mode()) == 0) goodrule = false;   break;
						case SCMOSRULES: if ((when&DRCTemplate.DRCMode.SC.mode()) == 0) goodrule = false;   break;
					}
					if (!goodrule) continue;
				}
				if ((when&(DRCTemplate.DRCMode.M2.mode()|DRCTemplate.DRCMode.M3.mode()|DRCTemplate.DRCMode.M4.mode()|DRCTemplate.DRCMode.M5.mode()|DRCTemplate.DRCMode.M6.mode())) != 0)
				{
					switch (numMetals)
					{
						case 2:  if ((when&DRCTemplate.DRCMode.M2.mode()) == 0) goodrule = false;   break;
						case 3:  if ((when&DRCTemplate.DRCMode.M3.mode()) == 0) goodrule = false;   break;
						case 4:  if ((when&DRCTemplate.DRCMode.M4.mode()) == 0) goodrule = false;   break;
						case 5:  if ((when&DRCTemplate.DRCMode.M5.mode()) == 0) goodrule = false;   break;
						case 6:  if ((when&DRCTemplate.DRCMode.M6.mode()) == 0) goodrule = false;   break;
					}
					if (!goodrule) continue;
				}
				if ((when&DRCTemplate.DRCMode.AC.mode()) != 0)
				{
					if (!isAlternateActivePolyRules()) continue;
				}
				if ((when&DRCTemplate.DRCMode.NAC.mode()) != 0)
				{
					if (isAlternateActivePolyRules()) continue;
				}
				if ((when&DRCTemplate.DRCMode.SV.mode()) != 0)
				{
					if (isDisallowStackedVias()) continue;
				}
				if ((when&DRCTemplate.DRCMode.NSV.mode()) != 0)
				{
					if (!isDisallowStackedVias()) continue;
				}
				if ((when&DRCTemplate.DRCMode.AN.mode()) != 0)
				{
					if (!isAnalog()) continue;
				}

				// get more information about the rule
				String proc = "";
				if ((when&(DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode())) != 0)
				{
					switch (rulesMode)
					{
						case DEEPRULES:  proc = "DEEP";   break;
						case SUBMRULES:  proc = "SUBM";   break;
						case SCMOSRULES: proc = "SCMOS";  break;
					}
				}
				String metal = "";
				if ((when&(DRCTemplate.DRCMode.M2.mode()|DRCTemplate.DRCMode.M3.mode()|DRCTemplate.DRCMode.M4.mode()|DRCTemplate.DRCMode.M5.mode()|DRCTemplate.DRCMode.M6.mode())) != 0)
				{
					switch (getNumMetals())
					{
						case 2:  metal = "2m";   break;
						case 3:  metal = "3m";   break;
						case 4:  metal = "4m";   break;
						case 5:  metal = "5m";   break;
						case 6:  metal = "6m";   break;
					}
					if (!goodrule) continue;
				}
				String ruleName = rule.ruleName;
				String extraString = metal + proc;
                if (extraString.length() > 0 && ruleName.indexOf(extraString) == -1) {
                    rule = new DRCTemplate(rule);
                    rule.ruleName +=  ", " +  extraString;
                }

                rules.loadDRCRules(this, foundry, rule, pWellProcess);
			}
		}

        return rules;
    }

    @Override
    public SizeCorrector getSizeCorrector(Version version, Map<Setting,Object> projectSettings, boolean isJelib, boolean keepExtendOverMin) {
        SizeCorrector sc = super.getSizeCorrector(version, projectSettings, isJelib, keepExtendOverMin);
        if (!keepExtendOverMin) return sc;
        boolean newDefaults = version.compareTo(Version.parseVersion("8.04u")) >= 0;
        int numMetals = newDefaults ? 6 : 4;
        boolean isSecondPolysilicon = newDefaults ? true : false;
        int ruleSet = SUBMRULES;

        Object numMetalsValue = projectSettings.get(getNumMetalsSetting());
        if (numMetalsValue instanceof Integer)
            numMetals = ((Integer)numMetalsValue).intValue();

        Object secondPolysiliconValue = projectSettings.get(getSecondPolysiliconSetting());
        if (secondPolysiliconValue instanceof Boolean)
            isSecondPolysilicon = ((Boolean)secondPolysiliconValue).booleanValue();
        else if (secondPolysiliconValue instanceof Integer)
            isSecondPolysilicon = ((Integer)secondPolysiliconValue).intValue() != 0;

        Object ruleSetValue = projectSettings.get(getRuleSetSetting());
        if (ruleSetValue instanceof Integer)
            ruleSet = ((Integer)ruleSetValue).intValue();

        if (numMetals == getNumMetals() && isSecondPolysilicon == isSecondPolysilicon() && ruleSet == getRuleSet() && version.compareTo(changeOfMetal6) >= 0)
            return sc;

        setArcCorrection(sc, "Polysilicon-2", ruleSet == SCMOSRULES ? 3 : 7);
        setArcCorrection(sc, "Metal-3", numMetals <= 3 ? (ruleSet == SCMOSRULES ? 6 : 5) : 3);
        setArcCorrection(sc, "Metal-4", numMetals <= 4 ? 6 : 3);
        setArcCorrection(sc, "Metal-5", numMetals <= 5 ? 4 : 3);
        if (version.compareTo(changeOfMetal6) < 0) // Fix of bug #357
            setArcCorrection(sc, "Metal-6", 4);

        return sc;
    }

	/******************** OPTIONS ********************/

    private final Setting cacheRuleSet = makeIntSetting("MoCMOSRuleSet", "Technology tab", "MOSIS CMOS rule set",
        techParamRuleSet.xmlPath.substring(TECH_NAME.length() + 1), 1, "SCMOS", "Submicron", "Deep");
	/**
	 * Method to tell the current rule set for this Technology if Mosis is the foundry.
	 * @return the current rule set for this Technology:<BR>
	 * 0: SCMOS rules<BR>
	 * 1: Submicron rules (the default)<BR>
	 * 2: Deep rules
	 */
    public int getRuleSet() { return paramRuleSet.intValue(); }

//    private static DRCTemplate.DRCMode getRuleMode()
//    {
//        switch (getRuleSet())
//        {
//            case DEEPRULES: return DRCTemplate.DRCMode.DE;
//            case SUBMRULES: return DRCTemplate.DRCMode.SU;
//            case SCMOSRULES: return DRCTemplate.DRCMode.SC;
//        }
//        return null;
//    }

    /**
	 * Method to set the rule set for this Technology.
	 * @return the new rule setting for this Technology, with values:<BR>
	 * 0: SCMOS rules<BR>
	 * 1: Submicron rules<BR>
	 * 2: Deep rules
	 */
	public Setting getRuleSetSetting() { return cacheRuleSet; }

	private final Setting cacheSecondPolysilicon = makeBooleanSetting(getTechName() + "SecondPolysilicon", "Technology tab", getTechName().toUpperCase() + " CMOS: Second Polysilicon Layer",
		techParamUseSecondPolysilicon.xmlPath.substring(TECH_NAME.length() + 1), true);
	/**
	 * Method to tell the number of polysilicon layers in this Technology.
	 * The default is false.
	 * @return true if there are 2 polysilicon layers in this Technology.
	 * If false, there is only 1 polysilicon layer.
	 */
	public boolean isSecondPolysilicon() { return paramUseSecondPolysilicon.booleanValue(); }
	/**
	 * Returns project Setting to tell a second polysilicon layer in this Technology.
	 * @return project Setting to tell a second polysilicon layer in this Technology.
	 */
	public Setting getSecondPolysiliconSetting() { return cacheSecondPolysilicon; }

	private final Setting cacheDisallowStackedVias = makeBooleanSetting("MoCMOSDisallowStackedVias", "Technology tab", "MOSIS CMOS: Disallow Stacked Vias",
        techParamDisallowStackedVias.xmlPath.substring(TECH_NAME.length() + 1), false);
	/**
	 * Method to determine whether this Technology disallows stacked vias.
	 * The default is false (they are allowed).
	 * @return true if the MOCMOS technology disallows stacked vias.
	 */
	public boolean isDisallowStackedVias() { return paramDisallowStackedVias.booleanValue(); }
	/**
	 * Returns project Setting to tell whether this Technology disallows stacked vias.
	 * @return project Setting to tell whether this Technology disallows stacked vias.
	 */
	public Setting getDisallowStackedViasSetting() { return cacheDisallowStackedVias; }

	private final Setting cacheAlternateActivePolyRules = makeBooleanSetting("MoCMOSAlternateActivePolyRules", "Technology tab", "MOSIS CMOS: Alternate Active and Poly Contact Rules",
		techParamUseAlternativeActivePolyRules.xmlPath.substring(TECH_NAME.length() + 1), false);
	/**
	 * Method to determine whether this Technology is using alternate Active and Poly contact rules.
	 * The default is false.
	 * @return true if the MOCMOS technology is using alternate Active and Poly contact rules.
	 */
	public boolean isAlternateActivePolyRules() { return paramUseAlternativeActivePolyRules.booleanValue(); }
	/**
	 * Returns project Setting to tell whether this Technology is using alternate Active and Poly contact rules.
	 * @return project Setting to tell whether this Technology is using alternate Active and Poly contact rules.
	 */
	public Setting getAlternateActivePolyRulesSetting() { return cacheAlternateActivePolyRules; }

	private final Setting cacheAnalog = makeBooleanSetting(getTechName() + "Analog", "Technology tab", "MOSIS CMOS: Vertical NPN transistor pbase",
		techParamAnalog.xmlPath.substring(TECH_NAME.length() + 1), false);
	/**
	 * Method to tell whether this technology has layers for vertical NPN transistor pbase.
	 * The default is false.
	 * @return true if this Technology has layers for vertical NPN transistor pbase.
	 */
	public boolean isAnalog() { return paramAnalog.booleanValue(); }
	/**
	 * Returns project Setting to tell whether this technology has layers for vertical NPN transistor pbase.
	 * @return project Setting to tell whether this technology has layers for vertical NPN transistor pbase.
	 */
	public Setting getAnalogSetting() { return cacheAnalog; }

    /** set if no stacked vias allowed */			private static final int MOCMOSNOSTACKEDVIAS =   01;
//	/** set for stick-figure display */				private static final int MOCMOSSTICKFIGURE =     02;
	/** number of metal layers */					private static final int MOCMOSMETALS =         034;
	/**   2-metal rules */							private static final int MOCMOS2METAL =           0;
	/**   3-metal rules */							private static final int MOCMOS3METAL =          04;
	/**   4-metal rules */							private static final int MOCMOS4METAL =         010;
	/**   5-metal rules */							private static final int MOCMOS5METAL =         014;
	/**   6-metal rules */							private static final int MOCMOS6METAL =         020;
	/** type of rules */							private static final int MOCMOSRULESET =       0140;
	/**   set if submicron rules in use */			private static final int MOCMOSSUBMRULES =        0;
	/**   set if deep rules in use */				private static final int MOCMOSDEEPRULES =      040;
	/**   set if standard SCMOS rules in use */		private static final int MOCMOSSCMOSRULES =    0100;
	/** set to use alternate active/poly rules */	private static final int MOCMOSALTAPRULES =    0200;
	/** set to use second polysilicon layer */		private static final int MOCMOSTWOPOLY =       0400;
//	/** set to show special transistors */			private static final int MOCMOSSPECIALTRAN =  01000;

	/**
	 * Method to convert any old-style state information to the new options.
	 */
	/**
	 * Method to convert any old-style variable information to the new options.
	 * May be overrideen in subclasses.
	 * @param varName name of variable
	 * @param value value of variable
	 * @return true if variable was converted
	 */
    @Override
	public Map<Setting,Object> convertOldVariable(String varName, Object value)
	{
        if (varName.equals("MoCMOSNumberOfMetalLayers") || varName.equals("MOCMOSNumberOfMetalLayers"))
            return Collections.singletonMap(getNumMetalsSetting(), value);
        if (varName.equals("MoCMOSSecondPolysilicon"))
            return Collections.singletonMap(getSecondPolysiliconSetting(), value);

        if (!varName.equalsIgnoreCase(TECH_LAST_STATE.getName())) return null;
		if (!(value instanceof Integer)) return null;
		int oldBits = ((Integer)value).intValue();

        HashMap<Setting,Object> settings = new HashMap<Setting,Object>();

		boolean oldNoStackedVias = (oldBits&MOCMOSNOSTACKEDVIAS) != 0;
		settings.put(getDisallowStackedViasSetting(), new Integer(oldNoStackedVias?1:0));

		int numMetals = 0;
		switch (oldBits&MOCMOSMETALS)
		{
			case MOCMOS2METAL: numMetals = 2;   break;
			case MOCMOS3METAL: numMetals = 3;   break;
			case MOCMOS4METAL: numMetals = 4;   break;
			case MOCMOS5METAL: numMetals = 5;   break;
			case MOCMOS6METAL: numMetals = 6;   break;
		}
		settings.put(getNumMetalsSetting(), new Integer(numMetals));

		int ruleSet = 0;
		switch (oldBits&MOCMOSRULESET)
		{
			case MOCMOSSUBMRULES:  ruleSet = SUBMRULES;   break;
			case MOCMOSDEEPRULES:  ruleSet = DEEPRULES;   break;
			case MOCMOSSCMOSRULES: ruleSet = SCMOSRULES;  break;
		}
		settings.put(getRuleSetSetting(), new Integer(ruleSet));

		boolean alternateContactRules = (oldBits&MOCMOSALTAPRULES) != 0;
		settings.put(getAlternateActivePolyRulesSetting(), new Integer(alternateContactRules?1:0));

		boolean secondPoly = (oldBits&MOCMOSTWOPOLY) != 0;
		settings.put(getSecondPolysiliconSetting(), new Integer(secondPoly?1:0));

		return settings;
	}

    /**
     * This method is called from TechFactory by reflection. Don't remove.
     * Returns a list of TechFactory.Params affecting this Technology
     * @return list of TechFactory.Params affecting this Technology
     */
    public static List<TechFactory.Param> getTechParams() {
        return Arrays.asList(
                techParamRuleSet,
                techParamNumMetalLayers,
                techParamUseSecondPolysilicon,
                techParamDisallowStackedVias,
                techParamUseAlternativeActivePolyRules,
                techParamAnalog);
    }

    /**
     * This method is called from TechFactory by reflection. Don't remove.
     * Returns patched Xml description of this Technology for specified technology params
     * @param params values of technology params
     * @return patched Xml description of this Technology
     */
    public static Xml.Technology getPatchedXml(Map<TechFactory.Param,Object> params) {
        int ruleSet = ((Integer)params.get(techParamRuleSet)).intValue();
        int numMetals = ((Integer)params.get(techParamNumMetalLayers)).intValue();
        boolean secondPolysilicon = ((Boolean)params.get(techParamUseSecondPolysilicon)).booleanValue();
        boolean disallowStackedVias = ((Boolean)params.get(techParamDisallowStackedVias)).booleanValue();
        boolean alternateContactRules = ((Boolean)params.get(techParamUseAlternativeActivePolyRules)).booleanValue();
        boolean isAnalog = ((Boolean)params.get(techParamAnalog)).booleanValue();

        Xml.Technology tech = Xml.parseTechnology(MoCMOS.class.getResource("mocmos.xml"));
        Xml.Layer[] metalLayers = new Xml.Layer[6];
        Xml.ArcProto[] metalArcs = new Xml.ArcProto[6];
        Xml.ArcProto[] activeArcs = new Xml.ArcProto[2];
        Xml.ArcProto[] polyArcs = new Xml.ArcProto[6];
        Xml.PrimitiveNodeGroup[] metalPinNodes = new Xml.PrimitiveNodeGroup[9];
        Xml.PrimitiveNodeGroup[] activePinNodes = new Xml.PrimitiveNodeGroup[2];
        Xml.PrimitiveNodeGroup[] polyPinNodes = new Xml.PrimitiveNodeGroup[2];
        Xml.PrimitiveNodeGroup[] metalContactNodes = new Xml.PrimitiveNodeGroup[8];
        Xml.PrimitiveNodeGroup[] metalWellContactNodes = new Xml.PrimitiveNodeGroup[2];
        Xml.PrimitiveNodeGroup[] metalActiveContactNodes = new Xml.PrimitiveNodeGroup[2];
        Xml.PrimitiveNodeGroup[] metal1PolyContactNodes = new Xml.PrimitiveNodeGroup[3];
        Xml.PrimitiveNodeGroup[] transistorNodeGroups = new Xml.PrimitiveNodeGroup[2];
        Xml.PrimitiveNodeGroup[] scalableTransistorNodes = new Xml.PrimitiveNodeGroup[2];
        Xml.PrimitiveNodeGroup npnTransistorNode = tech.findNodeGroup("NPN-Transistor");
        for (int i = 0; i < metalLayers.length; i++) {
            metalLayers[i] = tech.findLayer("Metal-" + (i + 1));
            metalArcs[i] = tech.findArc("Metal-" + (i + 1));
            metalPinNodes[i] = tech.findNodeGroup("Metal-" + (i + 1) + "-Pin");
            if (i >= metalContactNodes.length) continue;
            metalContactNodes[i] = tech.findNodeGroup("Metal-" + (i + 1)+"-Metal-" + (i + 2) + "-Con");
        }
        for (int i = 0; i < 2; i++) {
            polyArcs[i] = tech.findArc("Polysilicon-" + (i + 1));
            polyPinNodes[i] = tech.findNodeGroup("Polysilicon-" + (i + 1) + "-Pin");
            metal1PolyContactNodes[i] = tech.findNodeGroup("Metal-1-Polysilicon-" + (i + 1) + "-Con");
        }
		metal1PolyContactNodes[2] = tech.findNodeGroup("Metal-1-Polysilicon-1-2-Con");
        for (int i = P_TYPE; i <= N_TYPE; i++) {
            String ts = i == P_TYPE ? "P" : "N";
    		activeArcs[i] = tech.findArc(ts + "-Active");
            activePinNodes[i] = tech.findNodeGroup(ts + "-Active-Pin");
            metalWellContactNodes[i] = tech.findNodeGroup("Metal-1-" + ts + "-Well-Con");
            metalActiveContactNodes[i] = tech.findNodeGroup("Metal-1-" + ts + "-Active-Con");
            transistorNodeGroups[i] = tech.findNodeGroup(ts + "-Transistor");
            scalableTransistorNodes[i] = tech.findNodeGroup(ts + "-Transistor-Scalable");
        }

		String rules = "";
		switch (ruleSet)
		{
			case SCMOSRULES: rules = "now standard";    break;
			case DEEPRULES:  rules = "now deep";        break;
			case SUBMRULES:  rules = "now submicron";   break;
		}
		int numPolys = 1;
		if (secondPolysilicon) numPolys = 2;
		String description = "MOSIS CMOS (2-6 metals [now " + numMetals + "], 1-2 polys [now " +
			numPolys + "], flex rules [" + rules + "]";
		if (disallowStackedVias) description += ", stacked vias disallowed";
		if (alternateContactRules) description += ", alternate contact rules";
		description += ")";

        tech.description = description;
        Xml.NodeLayer nl;
        ResizeData rd = new ResizeData(ruleSet, numMetals, alternateContactRules);
        for (int i = 0; i < 6; i++) {
            resizeArcPin(metalArcs[i], metalPinNodes[i], 0.5*rd.metal_width[i]);

            if (i >= 5) continue;
            Xml.PrimitiveNodeGroup via = metalContactNodes[i];
            nl = via.nodeLayers.get(2);
            nl.sizex = nl.sizey = rd.via_size[i];
            nl.sep1d = rd.via_inline_spacing[i];
            nl.sep2d = rd.via_array_spacing[i];
            if (i + 1 >= numMetals) continue;
            double halfSize = 0.5*rd.via_size[i] + rd.via_overhang[i + 1];
            resizeSquare(via, halfSize, halfSize, halfSize, 0);
        }
        for (int i = P_TYPE; i <= N_TYPE; i++) {
            double activeE = 0.5*rd.diff_width;
            double wellE = activeE + rd.nwell_overhang_diff_p;
            double selectE = activeE + rd.pplus_overhang_diff;
            resizeArcPin(activeArcs[i], activePinNodes[i], activeE, wellE, selectE);

            Xml.PrimitiveNodeGroup con = metalActiveContactNodes[i];
            double metalC = 0.5*rd.contact_size + rd.contact_metal_overhang_all_sides;
            double activeC = 0.5*rd.contact_size + rd.diff_contact_overhang;
            double wellC = activeC + rd.nwell_overhang_diff_p;
            double selectC = activeC + rd.nplus_overhang_diff;
            resizeSquare(con, activeC, metalC, activeC, wellC, selectC, 0);
            resizeContacts(con, rd);

            con = metalWellContactNodes[i];
            wellC = activeC + rd.nwell_overhang_diff_n;
            resizeSquare(con, activeC, metalC, activeC, wellC, selectC, 0);
            resizeContacts(con, rd);

            resizeSerpentineTransistor(transistorNodeGroups[i], rd);
        }
        resizeContacts(npnTransistorNode, rd);
        {
            Xml.PrimitiveNodeGroup con = metal1PolyContactNodes[0];
            double metalC = 0.5*rd.contact_size + rd.contact_metal_overhang_all_sides;
            double polyC = 0.5*rd.contact_size + rd.contact_poly_overhang;
            resizeSquare(con, polyC, metalC, polyC, 0);
        }
        for (int i = 0; i <= 2; i++)
            resizeContacts(metal1PolyContactNodes[i], rd);
        resizeArcPin(polyArcs[0], polyPinNodes[0], 0.5*rd.poly_width);
        resizeArcPin(polyArcs[1], polyPinNodes[1], 0.5*rd.poly2_width);

        for (int i = numMetals; i < 6; i++) {
            metalArcs[i].notUsed = true;
            metalPinNodes[i].notUsed = true;
            metalContactNodes[i-1].notUsed = true;
        }
        if (!secondPolysilicon) {
            polyArcs[1].notUsed = true;
            polyPinNodes[1].notUsed = true;
            metal1PolyContactNodes[1].notUsed = true;
            metal1PolyContactNodes[2].notUsed = true;
        }
        npnTransistorNode.notUsed = !isAnalog;

        return tech;
    }

    private static void resizeArcPin(Xml.ArcProto a, Xml.PrimitiveNodeGroup ng, double ... exts) {
        assert a.arcLayers.size() == exts.length;
        assert ng.nodeLayers.size() == exts.length;
        double baseExt = exts[0];
        double maxExt = 0;
        for (int i = 0; i < exts.length; i++) {
            Xml.ArcLayer al = a.arcLayers.get(i);
            Xml.NodeLayer nl = ng.nodeLayers.get(i);
            double ext = exts[i];
            assert al.layer.equals(nl.layer);
            assert nl.representation == Technology.NodeLayer.BOX;
            al.extend.value = ext;
            nl.hx.value = nl.hy.value = ext;
            nl.lx.value = nl.ly.value = ext == 0 ? 0 : -ext;
            maxExt = Math.max(maxExt, ext);
        }

        Integer version2 = Integer.valueOf(2);
        if (baseExt != 0)
            a.diskOffset.put(version2, Double.valueOf(baseExt));
        else
            a.diskOffset.clear();
        ng.baseLX.value = ng.baseLY.value = baseExt != 0 ? -baseExt : 0;
        ng.baseHX.value = ng.baseHY.value = baseExt;
        // n.setDefSize
    }

    private static void resizeSquare(Xml.PrimitiveNodeGroup ng, double base, double... size) {
        assert size.length == ng.nodeLayers.size();
        double maxSz = 0;
        for (int i = 0; i < ng.nodeLayers.size(); i++) {
            Xml.NodeLayer nl = ng.nodeLayers.get(i);
            assert nl.representation == Technology.NodeLayer.BOX || nl.representation == Technology.NodeLayer.MULTICUTBOX;
            double sz = size[i];
            assert sz >= 0;
            nl.hx.value = nl.hy.value = sz;
            nl.lx.value = nl.ly.value = sz == 0 ? 0 : -sz;
            maxSz = Math.max(maxSz, sz);
        }

        Integer version1 = Integer.valueOf(1);
        Integer version2 = Integer.valueOf(2);
        EPoint sizeCorrector1 = ng.diskOffset.get(version1);
        EPoint sizeCorrector2 = ng.diskOffset.get(version2);
        if (sizeCorrector2 == null)
            sizeCorrector2 = EPoint.ORIGIN;
        if (sizeCorrector1 == null)
            sizeCorrector1 = sizeCorrector2;

        ng.baseLX.value = ng.baseLY.value = base != 0 ? -base : 0;
        ng.baseHX.value = ng.baseHY.value = base;

        sizeCorrector2 = EPoint.fromLambda(base, base);
        ng.diskOffset.put(version2, sizeCorrector2);
        if (sizeCorrector1.equals(sizeCorrector2))
            ng.diskOffset.remove(version1);
        else
            ng.diskOffset.put(version1, sizeCorrector1);
    }

    private static void resizeContacts(Xml.PrimitiveNodeGroup ng, ResizeData rd) {
        for (Xml.NodeLayer nl: ng.nodeLayers) {
            if (nl.representation != Technology.NodeLayer.MULTICUTBOX) continue;
            nl.sizex = nl.sizey = rd.contact_size;
            nl.sep1d = rd.contact_spacing;
            nl.sep2d = rd.contact_array_spacing;
        }
    }

    private static void resizeSerpentineTransistor(Xml.PrimitiveNodeGroup transistor, ResizeData rd) {
        Xml.NodeLayer activeTNode = transistor.nodeLayers.get(0); // active Top or Left
        Xml.NodeLayer activeBNode = transistor.nodeLayers.get(1); // active Bottom or Right
        Xml.NodeLayer polyCNode = transistor.nodeLayers.get(2); // poly center
        Xml.NodeLayer polyLNode = transistor.nodeLayers.get(3); // poly left or Top
        Xml.NodeLayer polyRNode = transistor.nodeLayers.get(4); // poly right or bottom
        Xml.NodeLayer activeNode = transistor.nodeLayers.get(5); // active
        Xml.NodeLayer polyNode = transistor.nodeLayers.get(6); // poly
        Xml.NodeLayer wellNode = transistor.nodeLayers.get(7); // well
        Xml.NodeLayer selNode = transistor.nodeLayers.get(8); // select
        Xml.NodeLayer thickNode = transistor.nodeLayers.get(9); // thick
        double hw = 0.5*rd.gate_width;
        double hl = 0.5*rd.gate_length;
        double gateX = hw;
        double gateY = hl;
        double polyX = gateX + rd.poly_endcap;
        double polyY = gateY;
        double diffX = gateX;
        double diffY = gateY + rd.diff_poly_overhang;
        double wellX = diffX + rd.nwell_overhang_diff_p;
        double wellY = diffY + rd.nwell_overhang_diff_p;
        double selX  = diffX + rd.pplus_overhang_diff;
        double selY  = diffY + rd.pplus_overhang_diff;
        double thickX = diffX + rd.thick_overhang;
        double thickY = diffY + rd.thick_overhang;
        resizeSerpentineLayer(activeTNode, hw, -gateX,  gateX,   gateY,  diffY);
        resizeSerpentineLayer(activeBNode, hw, -diffX,  diffX,  -diffY, -gateY);
        resizeSerpentineLayer(polyCNode,   hw, -gateX,  gateX,  -gateY,  gateY);
        resizeSerpentineLayer(polyLNode,   hw, -polyX, -gateX,  -polyY,  polyY);
        resizeSerpentineLayer(polyRNode,   hw,  gateX,  polyX,  -polyY,  polyY);
        resizeSerpentineLayer(activeNode,  hw, -diffX,  diffX,  -diffY,  diffY);
        resizeSerpentineLayer(polyNode,    hw, -polyX,  polyX,  -polyY,  polyY);
        resizeSerpentineLayer(wellNode,    hw, -wellX,  wellX,  -wellY,  wellY);
        resizeSerpentineLayer(selNode,     hw, -selX,   selX,   -selY,   selY);
        resizeSerpentineLayer(thickNode,   hw, -thickX, thickX, -thickY, thickY);
    }

    private static void resizeSerpentineLayer(Xml.NodeLayer nl, double hw, double lx, double hx, double ly, double hy) {
        nl.lx.value = lx;
        nl.hx.value = hx;
        nl.ly.value = ly;
        nl.hy.value = hy;
        nl.lWidth = nl.hy.k == 1 ? hy : 0;
        nl.rWidth = nl.ly.k == -1 ? -ly : 0;
        nl.tExtent = nl.hx.k == 1 ? hx - hw : 0;
        nl.bExtent = nl.lx.k == -1 ? -lx - hw : 0;
    }

    private static class ResizeData {
        private final double diff_width = 3; // 2.1
        private final double diff_poly_overhang; // 3.4
        private final double diff_contact_overhang; // 6.2 6.2b
        private final double thick_overhang = 4;

        private final double poly_width = 2; // 3.1
        private final double poly_endcap; // 3.3

        private final double gate_length = poly_width; // 3.1
        private final double gate_width = diff_width; // 2.1
        private final double gate_contact_spacing = 2; // 5.4

        private final double poly2_width; // 11.1

        private final double contact_size = 2; // 5.1
        private final double contact_spacing; // 5.3
        private final double contact_array_spacing; // 5.3
        private final double contact_metal_overhang_all_sides = 1; // 7.3
        private final double contact_poly_overhang; // 5.2 5.2b

        private final double nplus_overhang_diff = 2; // 4.2
        private final double pplus_overhang_diff = 2; // 4.2

        private final double well_width;
        private final double nwell_overhang_diff_p; // 2.3
        private final double nwell_overhang_diff_n = 3; // 2.4

        private final double[] metal_width; // 7.1 9.1 15.1 22.1 26.1 30.1
        private final double[] via_size; // 8.1 14.1 21.1 25.1 29.1
    	private final double[] via_inline_spacing; // 8.2 14.2 21.2 25.2 29.2
    	private final double[] via_array_spacing; // 8.2 14.2 21.2 25.2 29.2
    	private final double[] via_overhang; // 8.3 14.3 21.3 25.3 29.3 30.3

        ResizeData(int ruleSet, int numMetals, boolean alternateContactRules) {
            switch (ruleSet) {
                case SUBMRULES:
                    diff_poly_overhang = 3;
                    poly_endcap = 2;
                    poly2_width = 7;
                    contact_spacing = 3;
                    well_width = 12;
                    nwell_overhang_diff_p = 6;
                    switch (numMetals) {
                        case 2:
                            metal_width = new double[] { 3, 3,      0, 0, 0, 5 };
                            via_size = new double[] { 2,      2, 2, 2, 3 };
                            via_inline_spacing = new double[] { 3,      3, 3, 3, 4 };
                            via_overhang = new double[] { 1, 1 };
                            break;
                        case 3:
                            metal_width = new double[] { 3, 3, 5,      0, 0, 5 };
                            via_size = new double[] { 2, 2,      2, 2, 3 };
                            via_inline_spacing = new double[] { 3, 3,      3, 3, 4 };
                            via_overhang = new double[] { 1, 1, 2 };
                            break;
                        case 4:
                            metal_width = new double[] { 3, 3, 3, 6,      0, 5 };
                            via_size = new double[] { 2, 2, 2,      2, 3 };
                            via_inline_spacing = new double[] { 3, 3, 3,      3, 4 };
                            via_overhang = new double[] { 1, 1, 1, 2 };
                            break;
                        case 5:
                            metal_width = new double[] { 3, 3, 3, 3, 4,      5 };
                            via_size = new double[] { 2, 2, 2, 2,      3 };
                            via_inline_spacing = new double[] { 3, 3, 3, 3,      4 };
                            via_overhang = new double[] { 1, 1, 1, 1, 1 };
                            break;
                        case 6:
                            metal_width = new double[] { 3, 3, 3, 3, 3, 5 };
                            via_size = new double[] { 2, 2, 2, 2, 3 };
                            via_inline_spacing = new double[] { 3, 3, 3, 3, 4 };
                            via_overhang = new double[] { 1, 1, 1, 1, 1, 1 };
                            break;
                        default:
                            throw new IllegalArgumentException("Illegal number of metals " + numMetals + " in SUB rule set");
                     }
                    break;
                case DEEPRULES:
                    diff_poly_overhang = 4;
                    poly_endcap = 2.5;
                    poly2_width = 0;
                    contact_spacing = 4;
                    well_width = 12;
                    nwell_overhang_diff_p = 6;
                    switch (numMetals) {
                        case 5:
                            metal_width = new double[] { 3, 3, 3, 3, 4,     5 };
                            via_size = new double[] { 3, 3, 3, 3,      4 };
                            via_inline_spacing = new double[] { 3, 3, 3, 3,      4 };
                            via_overhang = new double[] { 1, 1, 1, 1, 2 };
                            break;
                        case 6:
                            metal_width = new double[] { 3, 3, 3, 3, 3, 5 };
                            via_size = new double[] { 3, 3, 3, 3, 4 };
                            via_inline_spacing = new double[] { 3, 3, 3, 3, 4 };
                            via_overhang = new double[] { 1, 1, 1, 1, 1, 2 };
                            break;
                        default:
                            throw new IllegalArgumentException("Illegal number of metals " + numMetals + " in DEEP rule set");
                     }
                    break;
                case SCMOSRULES:
                    diff_poly_overhang = 3;
                    poly_endcap = 2;
                    poly2_width = 3;
                    contact_spacing = 2;
                    well_width = 10;
                    nwell_overhang_diff_p = 5;
                    switch (numMetals) {
                        case 2:
                            metal_width = new double[] { 3, 3,      0, 0, 0, 5 };
                            via_size = new double[] { 2,      2, 2, 0, 0 };
                            via_inline_spacing = new double[] { 3,      3, 3, 3, 4 };
                            via_overhang = new double[] { 1, 1 };
                            break;
                        case 3:
                            metal_width = new double[] { 3, 3, 6,      0, 0, 5 };
                            via_size = new double[] { 2, 2,      2, 0, 0 };
                            via_inline_spacing = new double[] { 3, 3,      3, 3, 4 };
                            via_overhang = new double[] { 1, 1, 2 };
                            break;
                        case 4:
                            metal_width = new double[] { 3, 3, 3, 6,      0, 5 };
                            via_size = new double[] { 2, 2, 2,      0, 0 };
                            via_inline_spacing = new double[] { 3, 3, 3,      3, 4 };
                            via_overhang = new double[] { 1, 1, 1, 2 };
                            break;
                        default:
                            throw new IllegalArgumentException("Illegal number of metals " + numMetals + " in SCMOS rule set");
                    }
                    break;
                default:
                    throw new AssertionError("Illegal rule set " + ruleSet);
            }
            diff_contact_overhang = alternateContactRules ? 1 : 1.5;
            contact_poly_overhang = alternateContactRules ? 1 : 1.5;
            contact_array_spacing = contact_spacing;
            via_array_spacing = via_inline_spacing;
        }
    }

/******************** OVERRIDES ********************/
    /**
     * Method to convert old primitive port names to their proper PortProtos.
     * @param portName the unknown port name, read from an old Library.
     * @param np the PrimitiveNode on which this port resides.
     * @return the proper PrimitivePort to use for this name.
     */
    @Override
    public PrimitivePort convertOldPortName(String portName, PrimitiveNode np)
    {
        String[] transistorPorts = { "poly-left", "diff-top", "poly-right", "diff-bottom" };
        for (int i = 0; i < transistorPorts.length; i++)
        {
            if (portName.endsWith(transistorPorts[i]))
                return (PrimitivePort)np.findPortProto(transistorPorts[i]);
        }
        return super.convertOldPortName(portName, np);
    }

    /**
     * Method to set the size of a transistor NodeInst in this Technology.
     * Override because for MOCMOS sense of "width" and "length" are
     * different for resistors and transistors.
     * @param ni the NodeInst
     * @param width the new width (positive values only)
     * @param length the new length (positive values only)
     */
    @Override
    public void setPrimitiveNodeSize(NodeInst ni, double width, double length)
    {
        if (ni.getFunction().isResistor()) {
        	super.setPrimitiveNodeSize(ni, length, width);
        } else {
        	super.setPrimitiveNodeSize(ni, width, length);
        }
    }

    /**
     * Method to calculate extension of the poly gate from active layer or of the active from the poly gate.
     * @param primNode
     * @param poly true to calculate the poly extension
     * @param rules
     * @return value of the extension
     */
    private double getTransistorExtension(PrimitiveNode primNode, boolean poly, DRCRules rules)
    {
        if (!primNode.getFunction().isTransistor()) return 0.0;

        Technology.NodeLayer activeNode = primNode.getLayers()[0]; // active
        Technology.NodeLayer polyCNode;

        if (scalableTransistorNodes != null && (primNode == scalableTransistorNodes[P_TYPE] || primNode == scalableTransistorNodes[N_TYPE]))
        {
            polyCNode = primNode.getLayers()[5]; // poly center
        }
        else
        {
            // Standard transistors
            polyCNode = primNode.getElectricalLayers()[2]; // poly center
        }
        DRCTemplate overhang = (poly) ?
                rules.getExtensionRule(polyCNode.getLayer(), activeNode.getLayer(), false) :
                rules.getExtensionRule(activeNode.getLayer(), polyCNode.getLayer(), false);
        return (overhang != null ? overhang.getValue(0) : 0.0);
    }

	/** Return a substrate PortInst for this transistor NodeInst
     * @param ni the NodeInst
     * @return a PortInst for the substrate contact of the transistor
	 */
	@Override
	public PortInst getTransistorBiasPort(NodeInst ni)
	{
		return ni.getPortInst(4);
	}
}
