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
package com.sun.electric.technology.technologies;

import com.sun.electric.tool.Job;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.EditWindow0;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology.TechPoint;
import com.sun.electric.technology.technologies.utils.MOSRules;
import com.sun.electric.technology.*;
import com.sun.electric.technology.Technology.TechPref;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.net.URL;

/**
 * This is the MOSIS CMOS technology.
 */
public class MoCMOS extends Technology
{
	/** the MOSIS CMOS Technology object. */	public static final MoCMOS tech = initilizeMoCMOS();

    // Depending on plugins available
    private static MoCMOS initilizeMoCMOS()
    {
        MoCMOS tech = null;
        try
        {
            Class tsmcClass = Class.forName("com.sun.electric.plugins.tsmc.TSMC180");
            Object obj = tsmcClass.getDeclaredConstructor().newInstance();
            tech = (MoCMOS)obj;
        } catch (Exception e)
        {
            e.printStackTrace();

            System.out.println("GNU Release without extra plugins");
            tech = new MoCMOS();
        }
        return tech;
    }

	/** Value for standard SCMOS rules. */		public static final int SCMOSRULES = 0;
	/** Value for submicron rules. */			public static final int SUBMRULES  = 1;
	/** Value for deep rules. */				public static final int DEEPRULES  = 2;

	/** key of Variable for saving technology state. */
	public static final Variable.Key TECH_LAST_STATE = Variable.newKey("TECH_last_state");
	/** key of Variable for saving scalable transistor contact information. */
	public static final Variable.Key TRANS_CONTACT = Variable.newKey("MOCMOS_transcontacts");

	// layers to share with subclasses
	protected Layer poly1Layer, transistorPolyLayer;
    protected Layer silicideBlockLayer;
    protected Layer[] selectLayers;
    protected Layer[] metalLayers = new Layer[6]; // 1 -> 6
    protected Layer polyCutLayer;
    protected Layer pActiveWellLayer;
    protected Layer[] activeLayers = new Layer[2];
    protected Layer[] wellLayers = new Layer[2];
    protected Layer activeCutLayer;

	// arcs
    /** metal 1->6 arc */						protected ArcProto[] metalArcs = new ArcProto[6];
	/** polysilicon 1 arc */					protected ArcProto poly1_arc;
	/** polysilicon 2 arc */					private ArcProto poly2_arc;
    /** P/N-active arc */                       ArcProto[] activeArcs = new ArcProto[2];
	/** General active arc */					private ArcProto active_arc;

	// nodes
    /** metal-1->6-pin */				        private PrimitiveNode[] metalPinNodes = new PrimitiveNode[6];
	/** polysilicon-1-pin */					protected PrimitiveNode poly1Pin_node;
	/** polysilicon-2-pin */					private PrimitiveNode poly2Pin_node;
    /** P/N-active-pins */                      private PrimitiveNode[] activePinNodes = new PrimitiveNode[2];
	/** General active-pin */					private PrimitiveNode activePin_node;
    /** metal-1-P/N-active-contacts */          protected PrimitiveNode[] metalActiveContactNodes = new PrimitiveNode[2];
	/** metal-1-polysilicon-1-contact */		protected PrimitiveNode metal1Poly1Contact_node;
	/** metal-1-polysilicon-2-contact */		private PrimitiveNode metal1Poly2Contact_node;
	/** metal-1-polysilicon-1-2-contact */		private PrimitiveNode metal1Poly12Contact_node;
    /** P/N-Transistors */                      protected PrimitiveNode[] transistorNodes = new PrimitiveNode[2];
	/** ThickOxide Transistors */				private PrimitiveNode[] thickTransistorNodes = new PrimitiveNode[2];
    /** Scalable Transistors */			        protected PrimitiveNode[] scalableTransistorNodes = new PrimitiveNode[2];
    /** M1M2 -> M5M6 contacts */				protected PrimitiveNode[] metalContactNodes = new PrimitiveNode[5];
    /** metal-1-P/N-Well-contacts */            protected PrimitiveNode[] metalWellContactNodes = new PrimitiveNode[2];
	/** Metal-1 -> Metal-6 Nodes */			    private PrimitiveNode[] metalNodes = new PrimitiveNode[6];
	/** Polysilicon-1-Node */					private PrimitiveNode poly1Node_node;
	/** Polysilicon-2-Node */					private PrimitiveNode poly2Node_node;
	/** Active Nodes */						    private PrimitiveNode[] activeNodes = new PrimitiveNode[2];
	/** Select Nodes */						    private PrimitiveNode[] selectNodes = new PrimitiveNode[2];
	/** PolyCut-Node */							private PrimitiveNode polyCutNode_node;
	/** ActiveCut-Node */						private PrimitiveNode activeCutNode_node;
	/** Via-1 -. Via-5 Nodes */					private PrimitiveNode[] viaNodes = new PrimitiveNode[5];
	/** P-Well-Node */							private PrimitiveNode pWellNode_node;
	/** N-Well-Node */							private PrimitiveNode nWellNode_node;
	/** Passivation-Node */						private PrimitiveNode passivationNode_node;
	/** Pad-Frame-Node */						private PrimitiveNode padFrameNode_node;
	/** Poly-Cap-Node */						private PrimitiveNode polyCapNode_node;
	/** P-Active-Well-Node */					private PrimitiveNode pActiveWellNode_node;
	/** Polysilicon-1-Transistor-Node */		private PrimitiveNode polyTransistorNode_node;
	/** Silicide-Block-Node */					private PrimitiveNode silicideBlockNode_node;
	/** Thick-Active-Node */					private PrimitiveNode thickActiveNode_node;

	// for dynamically modifying the transistor geometry
	protected Technology.NodeLayer[] transistorPolyLayers = new Technology.NodeLayer[2];
	protected Technology.NodeLayer[] transistorActiveLayers = new Technology.NodeLayer[2];
	protected Technology.NodeLayer[] transistorActiveTLayers = new Technology.NodeLayer[2];
    protected Technology.NodeLayer[] transistorActiveBLayers = new Technology.NodeLayer[2];
	protected Technology.NodeLayer[] transistorPolyLLayers = new Technology.NodeLayer[2];
    protected Technology.NodeLayer[] transistorPolyRLayers = new Technology.NodeLayer[2];
    protected Technology.NodeLayer[] transistorPolyCLayers = new Technology.NodeLayer[2];
	protected Technology.NodeLayer[] transistorWellLayers = new Technology.NodeLayer[2];
	protected Technology.NodeLayer[] transistorSelectLayers = new Technology.NodeLayer[2];

	// design rule constants
	/** wide rules apply to geometry larger than this */				private static final double WIDELIMIT = 100;

    /**
     * Method to load MOSIS and TSMC rules in different classes
     * @param mosis
     * @param tsmc
     */
	private void loadRules(Foundry mosis, Foundry tsmc)
	{
        List<DRCTemplate> mRules = new ArrayList<DRCTemplate>(20);
        mosis.setRules(mRules);
        List<DRCTemplate> tRules = new ArrayList<DRCTemplate>(20);
        tsmc.setRules(tRules);
		mRules.add(new DRCTemplate("1.1 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.MINWID,   "P-Well",          null,            12, null));
		mRules.add(new DRCTemplate("1.1 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.MINWID,   "N-Well",          null,            12, null));
		mRules.add(new DRCTemplate("1.1 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.MINWID,   "Pseudo-P-Well",   null,            12, null));
		mRules.add(new DRCTemplate("1.1 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.MINWID,   "Pseudo-N-Well",   null,            12, null));
		mRules.add(new DRCTemplate("1.1 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.MINWID,   "P-Well",          null,            10, null));
		mRules.add(new DRCTemplate("1.1 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.MINWID,   "N-Well",          null,            10, null));
		mRules.add(new DRCTemplate("1.1 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Pseudo-P-Well",   null,            10, null));
		mRules.add(new DRCTemplate("1.1 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Pseudo-N-Well",   null,            10, null));
        tRules.add(new DRCTemplate("NW.W.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "P-Well",          null,            8.6, null));
        tRules.add(new DRCTemplate("NW.W.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "N-Well",          null,            8.6, null));
        tRules.add(new DRCTemplate("NW.W.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Pseudo-P-Well",   null,            8.6, null));
        tRules.add(new DRCTemplate("NW.W.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Pseudo-N-Well",   null,            8.6, null));

		mRules.add(new DRCTemplate("1.2 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.UCONSPA,  "P-Well",         "P-Well",         18, null));
		mRules.add(new DRCTemplate("1.2 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.UCONSPA,  "N-Well",         "N-Well",         18, null));
        mRules.add(new DRCTemplate("1.2 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.UCONSPA,  "P-Well",         "P-Well",         9,  null));
		mRules.add(new DRCTemplate("1.2 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.UCONSPA,  "N-Well",         "N-Well",         9,  null));
        tRules.add(new DRCTemplate("NW.S.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.UCONSPA,  "P-Well",         "P-Well",         14, null));
        tRules.add(new DRCTemplate("NW.S.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.UCONSPA,  "N-Well",         "N-Well",         14, null));

		mRules.add(new DRCTemplate("1.3 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.CONSPA,   "P-Well",         "P-Well",         6,  null));
		mRules.add(new DRCTemplate("1.3 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.CONSPA,   "N-Well",         "N-Well",         6,  null));
        tRules.add(new DRCTemplate("NW.S.2 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.CONSPA,   "P-Well",         "P-Well",         6,  null));
		tRules.add(new DRCTemplate("NW.S.2 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.CONSPA,   "N-Well",         "N-Well",         6,  null));

		// Valid in case of unconnected node or connected node DRCRuleType.UCONSPA -> SPACING May 21, 05
		mRules.add(new DRCTemplate("1.4 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "P-Well",         "N-Well",         0,  null));
        tRules.add(new DRCTemplate("NW.S.0 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "P-Well",         "N-Well",         0,  null));

		mRules.add(new DRCTemplate("2.1 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.MINWID,   "P-Active",        null,            3,  null));
		mRules.add(new DRCTemplate("2.1 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.MINWID,   "N-Active",        null,            3,  null));
        tRules.add(new DRCTemplate("OD.W.1/2 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "P-Active",        null,            2.2,  null));
		tRules.add(new DRCTemplate("OD.W.1/2 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "N-Active",        null,            2.2,  null));

		mRules.add(new DRCTemplate("2.2 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "P-Active",       "P-Active",       3,  null));
		mRules.add(new DRCTemplate("2.2 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "N-Active",       "N-Active",       3,  null));
		mRules.add(new DRCTemplate("2.2 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "P-Active-Well",  "P-Active-Well",  3,  null));
		mRules.add(new DRCTemplate("2.2 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "P-Active",       "P-Active-Well",  3,  null));
		mRules.add(new DRCTemplate("2.2 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "N-Active",       "P-Active-Well",  3,  null));
        tRules.add(new DRCTemplate("OD.S.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "P-Active",       "P-Active",       2.8,  null));
		tRules.add(new DRCTemplate("OD.S.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "N-Active",       "N-Active",       2.8,  null));
		tRules.add(new DRCTemplate("OD.S.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "P-Active-Well",  "P-Active-Well",  2.8,  null));
		tRules.add(new DRCTemplate("OD.S.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "P-Active",       "P-Active-Well",  2.8,  null));
		tRules.add(new DRCTemplate("OD.S.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "N-Active",       "P-Active-Well",  2.8,  null));

		mRules.add(new DRCTemplate("2.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.SURROUND, "N-Well",         "P-Active",       6,  "Metal-1-P-Active-Con"));
		mRules.add(new DRCTemplate("2.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.ASURROUND,"N-Well",         "P-Active",       6,  "P-Active"));
		mRules.add(new DRCTemplate("2.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.SURROUND, "P-Well",         "N-Active",       6,  "Metal-1-N-Active-Con"));
		mRules.add(new DRCTemplate("2.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.ASURROUND,"P-Well",         "N-Active",       6,  "N-Active"));
		mRules.add(new DRCTemplate("2.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.TRAWELL,   null,             null,            6,  null));
		mRules.add(new DRCTemplate("2.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.SURROUND, "N-Well",         "P-Active",       5,  "Metal-1-P-Active-Con"));
		mRules.add(new DRCTemplate("2.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.ASURROUND,"N-Well",         "P-Active",       5,  "P-Active"));
		mRules.add(new DRCTemplate("2.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.SURROUND, "P-Well",         "N-Active",       5,  "Metal-1-N-Active-Con"));
		mRules.add(new DRCTemplate("2.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.ASURROUND,"P-Well",         "N-Active",       5,  "N-Active"));
		mRules.add(new DRCTemplate("2.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.TRAWELL,   null,             null,            5,  null));
        tRules.add(new DRCTemplate("2.3 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SURROUND, "N-Well",         "P-Active",       6,  "Metal-1-P-Active-Con"));
        tRules.add(new DRCTemplate("2.3 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.ASURROUND,"N-Well",         "P-Active",       6,  "P-Active"));
        tRules.add(new DRCTemplate("2.3 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SURROUND, "P-Well",         "N-Active",       6,  "Metal-1-N-Active-Con"));
        tRules.add(new DRCTemplate("2.3 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.ASURROUND,"P-Well",         "N-Active",       6,  "N-Active"));
        tRules.add(new DRCTemplate("2.3 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.TRAWELL,   null,             null,            6,  null));

		// Rule 2.4 not implemented
		// In C-Electric it is implemented as 2.2 (min spacing=3) so we might have discrepancies.
		mRules.add(new DRCTemplate("2.5 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "P-Active",       "N-Active",       4,  null));
        tRules.add(new DRCTemplate("OD.S.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "P-Active",       "N-Active",       2.8,  null));

		mRules.add(new DRCTemplate("3.1 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.MINWID,   "Polysilicon-1",   null,            2,  null));
        mRules.add(new DRCTemplate("3.1 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.MINWID,   "Transistor-Poly", null,            2,  null));
        tRules.add(new DRCTemplate("PO.W.1/PO.W.2 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Polysilicon-1",   null,            1.8,  null));
        tRules.add(new DRCTemplate("PO.W.1/PO.W.2 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Transistor-Poly", null,            1.8,  null));

		mRules.add(new DRCTemplate("3.2 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.SPACING,  "Polysilicon-1",  "Polysilicon-1",  3,  null));
        mRules.add(new DRCTemplate("3.2 Mosis",   Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.SPACING,  "Polysilicon-1",  "Transistor-Poly",3,  null));
        mRules.add(new DRCTemplate("3.2 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Polysilicon-1",  "Polysilicon-1",  2,  null));
		mRules.add(new DRCTemplate("3.2 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Polysilicon-1",  "Transistor-Poly",2,  null));
        tRules.add(new DRCTemplate("PO.S.3 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Polysilicon-1",  "Polysilicon-1",  2.5,  null));
        tRules.add(new DRCTemplate("PO.S.3 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Polysilicon-1",  "Transistor-Poly",2.5,  null));

		mRules.add(new DRCTemplate("3.2a  Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.SPACING,  "Transistor-Poly","Transistor-Poly",3,  null));
		mRules.add(new DRCTemplate("3.2a Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Transistor-Poly","Transistor-Poly",2,  null));
        tRules.add(new DRCTemplate("PO.S.2 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Transistor-Poly","Transistor-Poly",2.5,  null));

		mRules.add(new DRCTemplate("3.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.TRAPOLY,   null,             null,            2.5,null));
		mRules.add(new DRCTemplate("3.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.TRAPOLY,   null,             null,            2,  null));
        tRules.add(new DRCTemplate("PO.O.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.TRAPOLY,   null,             null,            2.2,  null));

		mRules.add(new DRCTemplate("3.4 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.TRAACTIVE, null,             null,            4,  null));
		mRules.add(new DRCTemplate("3.4 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.TRAACTIVE, null,             null,            3,  null));
//        tRules.add(new DRCTemplate("3.4 TSMC",  DRCTemplate.DRCCheckMode.TSMC.mode(), DRCTemplate.DRCRuleType.TRAACTIVE, null,             null,            3.2,  null));

        // TSMC PO.C.1 = 1 too but they have different names
		mRules.add(new DRCTemplate("3.5 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "Polysilicon-1",  "P-Active",       1,  null));
		mRules.add(new DRCTemplate("3.5 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "Transistor-Poly","P-Active",       1,  null));
		mRules.add(new DRCTemplate("3.5 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "Polysilicon-1",  "N-Active",       1,  null));
		mRules.add(new DRCTemplate("3.5 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "Transistor-Poly","N-Active",       1,  null));
		mRules.add(new DRCTemplate("3.5 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "Polysilicon-1",  "P-Active-Well",  1,  null));
		mRules.add(new DRCTemplate("3.5 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "Transistor-Poly","P-Active-Well",  1,  null));

        tRules.add(new DRCTemplate("PO.C.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Polysilicon-1",  "P-Active",       1,  null));
		tRules.add(new DRCTemplate("PO.C.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Transistor-Poly","P-Active",       1,  null));
		tRules.add(new DRCTemplate("PO.C.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Polysilicon-1",  "N-Active",       1,  null));
		tRules.add(new DRCTemplate("PO.C.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Transistor-Poly","N-Active",       1,  null));
		tRules.add(new DRCTemplate("PO.C.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Polysilicon-1",  "P-Active-Well",  1,  null));
		tRules.add(new DRCTemplate("PO.C.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Transistor-Poly","P-Active-Well",  1,  null));

		mRules.add(new DRCTemplate("4.4 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.MINWID,   "P-Select",        null,            4,  null));
		mRules.add(new DRCTemplate("4.4 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.MINWID,   "N-Select",        null,            4,  null));
		mRules.add(new DRCTemplate("4.4 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.MINWID,   "Pseudo-P-Select", null,            4,  null));
		mRules.add(new DRCTemplate("4.4 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.MINWID,   "Pseudo-N-Select", null,            4,  null));
		mRules.add(new DRCTemplate("4.4 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.SPACING,  "P-Select",       "P-Select",       4,  null));
		mRules.add(new DRCTemplate("4.4 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.SPACING,  "N-Select",       "N-Select",       4,  null));
		mRules.add(new DRCTemplate("4.4 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.MINWID,   "P-Select",        null,            2,  null));
		mRules.add(new DRCTemplate("4.4 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.MINWID,   "N-Select",        null,            2,  null));
		mRules.add(new DRCTemplate("4.4 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Pseudo-P-Select", null,            2,  null));
		mRules.add(new DRCTemplate("4.4 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Pseudo-N-Select", null,            2,  null));
		mRules.add(new DRCTemplate("4.4 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.SPACING,  "P-Select",       "P-Select",       2,  null));
		mRules.add(new DRCTemplate("4.4 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.SPACING,  "N-Select",       "N-Select",       2,  null));
        mRules.add(new DRCTemplate("4.4 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "P-Select",       "N-Select",       0,  null));
        tRules.add(new DRCTemplate("PP/NP.W.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "P-Select",        null,            4.4,  null));
		tRules.add(new DRCTemplate("PP/NP.W.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "N-Select",        null,            4.4,  null));
		tRules.add(new DRCTemplate("PP/NP.W.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Pseudo-P-Select", null,            4.4,  null));
		tRules.add(new DRCTemplate("PP/NP.W.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Pseudo-N-Select", null,            4.4,  null));
		tRules.add(new DRCTemplate("PP/NP.S.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "P-Select",       "P-Select",       4.4,  null));
		tRules.add(new DRCTemplate("PP/NP.S.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "N-Select",       "N-Select",       4.4,  null));
		tRules.add(new DRCTemplate("PP/NP.S.0 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "P-Select",       "N-Select",       0,  null));
        tRules.add(new DRCTemplate("PP/NP.C.1 4 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "N-Select", "P-Active",              2.6,  "N-Select-Metal-1-N-Well-Con"));
        tRules.add(new DRCTemplate("PP/NP.C.1 4 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "P-Select", "N-Active",             2.6,  "P-Select-Metal-1-P-Well-Con"));

		mRules.add(new DRCTemplate("5.1 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.MINWID,   "Poly-Cut",        null,            2,  null));
        tRules.add(new DRCTemplate("CO.W.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Poly-Cut",        null,            2.2,  null));

		mRules.add(new DRCTemplate("5.2 Mosis",        Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            5,  "Metal-1-Polysilicon-1-Con"));
		mRules.add(new DRCTemplate("5.2 Mosis",        Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "Polysilicon-1",  "Metal-1",        0.5,"Metal-1-Polysilicon-1-Con"));
		mRules.add(new DRCTemplate("5.2 Mosis",        Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.CUTSUR,    null,             null,            1.5,"Metal-1-Polysilicon-1-Con"));
		mRules.add(new DRCTemplate("5.2b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            4,  "Metal-1-Polysilicon-1-Con"));
		mRules.add(new DRCTemplate("5.2b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SURROUND, "Polysilicon-1",  "Metal-1",        0,  "Metal-1-Polysilicon-1-Con"));
		mRules.add(new DRCTemplate("5.2b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.CUTSUR,    null,             null,            1,  "Metal-1-Polysilicon-1-Con"));
        tRules.add(new DRCTemplate("PolyCon NodeSize TSMC",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            5,  "Metal-1-Polysilicon-1-Con"));
		tRules.add(new DRCTemplate("PolyCon Surround TSMC",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "Polysilicon-1",  "Metal-1",        0.5,"Metal-1-Polysilicon-1-Con"));
        tRules.add(new DRCTemplate("CO.E.2-M1.E.2 TSMC",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.CUTSUR,    null,             null,            1.4,"Metal-1-Polysilicon-1-Con"));

		mRules.add(new DRCTemplate("5.3 Mosis",     Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.CUTSPA,    null,             null,            4,  "Metal-1-Polysilicon-1-Con"));
        mRules.add(new DRCTemplate("5.3 Mosis",     Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.CUTSPA2D,    null,             null,            4,  "Metal-1-Polysilicon-1-Con"));
		mRules.add(new DRCTemplate("5.3 Mosis",     Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.SPACING,  "Poly-Cut",       "Poly-Cut",       4,  null));
		mRules.add(new DRCTemplate("5.3,6.3 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.SPACING,  "Active-Cut",     "Poly-Cut",       4,  null));
		mRules.add(new DRCTemplate("5.3 Mosis",     Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.CUTSPA,    null,             null,            2,  "Metal-1-Polysilicon-1-Con"));
        mRules.add(new DRCTemplate("5.3 Mosis",     Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.CUTSPA2D,    null,             null,            2,  "Metal-1-Polysilicon-1-Con"));
		mRules.add(new DRCTemplate("5.3 Mosis",     Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Poly-Cut",       "Poly-Cut",       2,  null));
		mRules.add(new DRCTemplate("5.3,6.3 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.SPACING,  "Active-Cut",     "Poly-Cut",       2,  null));
        // Mosis Submicron
		mRules.add(new DRCTemplate("5.3 Mosis",     Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.CUTSPA,    null,             null,            3,  "Metal-1-Polysilicon-1-Con"));
        mRules.add(new DRCTemplate("5.3 Mosis",     Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.CUTSPA2D,    null,             null,            3,  "Metal-1-Polysilicon-1-Con"));
		mRules.add(new DRCTemplate("5.3 Mosis",     Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.SPACING,  "Poly-Cut",       "Poly-Cut",       3,  null));
		mRules.add(new DRCTemplate("5.3,6.3 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.SPACING,  "Active-Cut",     "Poly-Cut",       3,  null));
        // TSMC Submicron
        tRules.add(new DRCTemplate("CO.S.1 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.CUTSPA,    null,             null,            2.5,  "Metal-1-Polysilicon-1-Con"));
        tRules.add(new DRCTemplate("CO.S.2 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.CUTSPA2D,    null,             null,            2.8,  "Metal-1-Polysilicon-1-Con"));
		tRules.add(new DRCTemplate("CO.S.1 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Poly-Cut",       "Poly-Cut",       2.5,  null));
		tRules.add(new DRCTemplate("CO.S.1 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Active-Cut",     "Poly-Cut",       2.5,  null));

		mRules.add(new DRCTemplate("5.4 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "Poly-Cut",       "Transistor-Poly", 2,  null));
        tRules.add(new DRCTemplate("CO.C.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Poly-Cut",       "Transistor-Poly", 1.6,  null));

		mRules.add(new DRCTemplate("5.5b Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.UCONSPA,  "Poly-Cut",       "Polysilicon-1",  5,  null));
		mRules.add(new DRCTemplate("5.5b Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.UCONSPA,  "Poly-Cut",       "Transistor-Poly",5,  null));
		mRules.add(new DRCTemplate("5.5b Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.UCONSPA,  "Poly-Cut",       "Polysilicon-1",  4,  null));
		mRules.add(new DRCTemplate("5.5b Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.UCONSPA,  "Poly-Cut",       "Transistor-Poly",4,  null));

		mRules.add(new DRCTemplate("5.6b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SPACING,  "Poly-Cut",       "P-Active",       2,  null));
		mRules.add(new DRCTemplate("5.6b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SPACING,  "Poly-Cut",       "N-Active",       2,  null));

		mRules.add(new DRCTemplate("5.7b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SPACINGM, "Poly-Cut",       "P-Active",       3,  null));
		mRules.add(new DRCTemplate("5.7b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SPACINGM, "Poly-Cut",       "N-Active",       3,  null));

		mRules.add(new DRCTemplate("6.1 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.MINWID,   "Active-Cut",      null,            2,  null));
        tRules.add(new DRCTemplate("CO.W.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Active-Cut",      null,            2.2,  null));

		mRules.add(new DRCTemplate("6.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            5,  "Metal-1-P-Active-Con"));
		mRules.add(new DRCTemplate("6.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "P-Active",       "Metal-1",        0.5,"Metal-1-P-Active-Con"));
		mRules.add(new DRCTemplate("6.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "P-Select",       "P-Active",       2,  "Metal-1-P-Active-Con"));
        mRules.add(new DRCTemplate("6.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "N-Well",         "P-Active",       6,  "Metal-1-P-Active-Con"));
		mRules.add(new DRCTemplate("6.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "N-Well",         "P-Active",       5,  "Metal-1-P-Active-Con"));
		mRules.add(new DRCTemplate("6.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.CUTSUR,    null,             null,            1.5,"Metal-1-P-Active-Con"));
        mRules.add(new DRCTemplate("6.2b Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            4,  "Metal-1-P-Active-Con"));
		mRules.add(new DRCTemplate("6.2b Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SURROUND, "P-Active",       "Metal-1",        0,  "Metal-1-P-Active-Con"));
		mRules.add(new DRCTemplate("6.2b Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SURROUND, "P-Select",       "P-Active",       2,  "Metal-1-P-Active-Con"));
		mRules.add(new DRCTemplate("6.2b Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SURROUND, "N-Well",         "P-Active",       6,  "Metal-1-P-Active-Con"));
		mRules.add(new DRCTemplate("6.2b Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SURROUND, "N-Well",         "P-Active",       5,  "Metal-1-P-Active-Con"));
		mRules.add(new DRCTemplate("6.2b Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.CUTSUR,    null,             null,            1,  "Metal-1-P-Active-Con"));
        tRules.add(new DRCTemplate("ActCon NodeSize TSMC", Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            5,  "Metal-1-P-Active-Con"));
        tRules.add(new DRCTemplate("ActCon Surround TSMC", Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "P-Active",       "Metal-1",        0.5,"Metal-1-P-Active-Con"));
        tRules.add(new DRCTemplate("ActCon Surround TSMC", Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "N-Well",         "P-Active",        6,"Metal-1-P-Active-Con"));
        tRules.add(new DRCTemplate("PP/NP.E.1,PP/NP.C.3,PP/NP.E.4 TSMC",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "P-Select",       "P-Active",       1.8,  "Metal-1-P-Active-Con"));
        tRules.add(new DRCTemplate("CO.E.2-M1.E.2 TSMC",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.CUTSUR,    null,             null,            1.4,"Metal-1-P-Active-Con"));

		mRules.add(new DRCTemplate("6.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            5,  "Metal-1-N-Active-Con"));
		mRules.add(new DRCTemplate("6.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "N-Active",       "Metal-1",        0.5,"Metal-1-N-Active-Con"));
        mRules.add(new DRCTemplate("6.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "N-Select",       "N-Active",       2,  "Metal-1-N-Active-Con"));
        mRules.add(new DRCTemplate("6.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "P-Well",         "N-Active",       6,  "Metal-1-N-Active-Con"));
        mRules.add(new DRCTemplate("6.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode()|   DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "P-Well",         "N-Active",       5,  "Metal-1-N-Active-Con"));
        mRules.add(new DRCTemplate("6.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.CUTSUR,    null,             null,            1.5,"Metal-1-N-Active-Con"));
        mRules.add(new DRCTemplate("6.2b Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            4,  "Metal-1-N-Active-Con"));
		mRules.add(new DRCTemplate("6.2b Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SURROUND, "N-Active",       "Metal-1",        0,  "Metal-1-N-Active-Con"));
		mRules.add(new DRCTemplate("6.2b Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SURROUND, "N-Select",       "N-Active",       2,  "Metal-1-N-Active-Con"));
		mRules.add(new DRCTemplate("6.2b Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SURROUND, "P-Well",         "N-Active",       6,  "Metal-1-N-Active-Con"));
		mRules.add(new DRCTemplate("6.2b Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode()|   DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SURROUND, "P-Well",         "N-Active",       5,  "Metal-1-N-Active-Con"));
		mRules.add(new DRCTemplate("6.2b Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.CUTSUR,    null,             null,            1,  "Metal-1-N-Active-Con"));
        tRules.add(new DRCTemplate("ActCon NodeSize TSMC", Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            5,  "Metal-1-N-Active-Con"));
        tRules.add(new DRCTemplate("ActCon Surround TSMC", Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "N-Active",       "Metal-1",        0.5,"Metal-1-N-Active-Con"));
        tRules.add(new DRCTemplate("ActCon Surround TSMC", Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "P-Well",         "N-Active",        6, "Metal-1-N-Active-Con"));
        tRules.add(new DRCTemplate("PP/NP.E.1,PP.C.3,PP/NP.E.4 TSMC",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "N-Select",       "N-Active",       1.8,  "Metal-1-N-Active-Con")); // PP.C.3&PP/NP.E.4=1.8 PP/NP.C.1=2.6
        tRules.add(new DRCTemplate("CO.E.2-M1.E.2 TSMC",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.CUTSUR,    null,             null,            1.4,"Metal-1-N-Active-Con"));

		mRules.add(new DRCTemplate("6.2 Mosis",        Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            5,  "Metal-1-P-Well-Con"));
        mRules.add(new DRCTemplate("6.2 Mosis",        Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "P-Active-Well",  "Metal-1",        0.5,"Metal-1-P-Well-Con"));
        mRules.add(new DRCTemplate("6.2 Mosis",        Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "P-Select",       "P-Active-Well",  2,  "Metal-1-P-Well-Con"));
        mRules.add(new DRCTemplate("6.2 Mosis",        Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "P-Well",         "P-Active-Well",  3,  "Metal-1-P-Well-Con"));
        mRules.add(new DRCTemplate("6.2 Mosis",        Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.CUTSUR,    null,             null,            1.5,"Metal-1-P-Well-Con"));
        mRules.add(new DRCTemplate("6.2b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            4,  "Metal-1-P-Well-Con"));
		mRules.add(new DRCTemplate("6.2b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SURROUND, "P-Active-Well",  "Metal-1",        0,  "Metal-1-P-Well-Con"));
		mRules.add(new DRCTemplate("6.2b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SURROUND, "P-Select",       "P-Active-Well",  2,  "Metal-1-P-Well-Con"));
		mRules.add(new DRCTemplate("6.2b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SURROUND, "P-Well",         "P-Active-Well",  3,  "Metal-1-P-Well-Con"));
		mRules.add(new DRCTemplate("6.2b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.CUTSUR,    null,             null,            1,  "Metal-1-P-Well-Con"));
        tRules.add(new DRCTemplate("WellCon NodeSize TSMC",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            5,  "Metal-1-P-Well-Con"));
        tRules.add(new DRCTemplate("WellCon Surround TSMC",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "P-Active-Well",  "Metal-1",        0.5,"Metal-1-P-Well-Con"));
//		tRules.add(new DRCTemplate("6.2 TSMC",        DRCTemplate.DRCCheckMode.TSMC.mode()|DRCTemplate.DRCCheckMode.NAC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "P-Active-Well",  "Metal-1",        0.1,"Metal-1-P-Well-Con"));
        tRules.add(new DRCTemplate("PP/NP.C.2 TSMC",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "P-Select",       "P-Active-Well",  1,  "Metal-1-P-Well-Con"));   // PP/NP.C.2=1.0, PP/NP.C.1 4=1.8
        tRules.add(new DRCTemplate("OD.C.4 TSMC",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "P-Well",         "P-Active-Well",  4.3,  "Metal-1-P-Well-Con"));
        tRules.add(new DRCTemplate("CO.E.2-M1.E.2 TSMC",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.CUTSUR,    null,             null,            1.4,"Metal-1-P-Well-Con"));
        tRules.add(new DRCTemplate("CO.S.2 TSMC",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.CUTSPA,    null,             null,            2.8,"Metal-1-P-Well-Con"));
        tRules.add(new DRCTemplate("CO.S.2 TSMC",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.CUTSPA2D,    null,             null,            2.8,"Metal-1-P-Well-Con"));


		mRules.add(new DRCTemplate("6.2 Mosis",        Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            5,  "Metal-1-N-Well-Con"));
		mRules.add(new DRCTemplate("6.2 Mosis",        Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "N-Active",       "Metal-1",        0.5,"Metal-1-N-Well-Con"));
		mRules.add(new DRCTemplate("6.2 Mosis",        Foundry.Type.MOSIS.mode()|Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "N-Select",       "N-Active",       2,  "Metal-1-N-Well-Con"));
        mRules.add(new DRCTemplate("6.2 Mosis",        Foundry.Type.MOSIS.mode()|Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "N-Well",         "N-Active",       3,  "Metal-1-N-Well-Con"));
		mRules.add(new DRCTemplate("6.2 Mosis",        Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NAC.mode(),       DRCTemplate.DRCRuleType.CUTSUR,    null,             null,            1.5,"Metal-1-N-Well-Con"));
        mRules.add(new DRCTemplate("6.2b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            4,  "Metal-1-N-Well-Con"));
		mRules.add(new DRCTemplate("6.2b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SURROUND, "N-Active",       "Metal-1",        0,  "Metal-1-N-Well-Con"));
		mRules.add(new DRCTemplate("6.2b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SURROUND, "N-Select",       "N-Active",       2,  "Metal-1-N-Well-Con"));
		mRules.add(new DRCTemplate("6.2b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SURROUND, "N-Well",         "N-Active",       3,  "Metal-1-N-Well-Con"));
		mRules.add(new DRCTemplate("6.2b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.CUTSUR,    null,             null,            1,  "Metal-1-N-Well-Con"));
        tRules.add(new DRCTemplate("WellCon NodeSize TSMC",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            5,  "Metal-1-N-Well-Con"));
        tRules.add(new DRCTemplate("WellCon Surround TSM",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "N-Active",       "Metal-1",        0.5,"Metal-1-N-Well-Con"));
		tRules.add(new DRCTemplate("PP/NP.C.2 TSMC",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "N-Select",       "N-Active",       1,  "Metal-1-N-Well-Con"));
        tRules.add(new DRCTemplate("OD.C.4 TSMC",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.SURROUND, "N-Well",         "N-Active",       4.3,  "Metal-1-N-Well-Con"));
        tRules.add(new DRCTemplate("CO.E.2-M1.E.2 TSMC",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.CUTSUR,    null,             null,            1.4,"Metal-1-N-Well-Con"));
        tRules.add(new DRCTemplate("CO.S.2 TSMC",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.CUTSPA,    null,             null,            2.8,"Metal-1-N-Well-Con"));
        tRules.add(new DRCTemplate("CO.S.2 TSMC",        Foundry.Type.TSMC.mode(),       DRCTemplate.DRCRuleType.CUTSPA2D,    null,             null,            2.8,"Metal-1-N-Well-Con"));

		mRules.add(new DRCTemplate("6.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.CUTSPA,    null,             null,            4,  "Metal-1-P-Active-Con"));
        mRules.add(new DRCTemplate("6.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.CUTSPA2D,    null,             null,            4,  "Metal-1-P-Active-Con"));
		mRules.add(new DRCTemplate("6.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.CUTSPA,    null,             null,            4,  "Metal-1-N-Active-Con"));
        mRules.add(new DRCTemplate("6.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.CUTSPA2D,    null,             null,            4,  "Metal-1-N-Active-Con"));
		mRules.add(new DRCTemplate("6.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.SPACING,  "Active-Cut",     "Active-Cut",     4,  null));
		mRules.add(new DRCTemplate("6.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.CUTSPA,    null,             null,            2,  "Metal-1-P-Active-Con"));
        mRules.add(new DRCTemplate("6.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.CUTSPA2D,    null,             null,            2,  "Metal-1-P-Active-Con"));
		mRules.add(new DRCTemplate("6.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.CUTSPA,    null,             null,            2,  "Metal-1-N-Active-Con"));
        mRules.add(new DRCTemplate("6.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.CUTSPA2D,    null,             null,            2,  "Metal-1-N-Active-Con"));
		mRules.add(new DRCTemplate("6.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Active-Cut",     "Active-Cut",     2,  null));
        // Mosis
		mRules.add(new DRCTemplate("6.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.CUTSPA,    null,             null,            3,  "Metal-1-P-Active-Con"));
        mRules.add(new DRCTemplate("6.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.CUTSPA2D,    null,             null,            3,  "Metal-1-P-Active-Con"));
		mRules.add(new DRCTemplate("6.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.CUTSPA,    null,             null,            3,  "Metal-1-N-Active-Con"));
        mRules.add(new DRCTemplate("6.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.CUTSPA2D,    null,             null,            3,  "Metal-1-N-Active-Con"));
		mRules.add(new DRCTemplate("6.3 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.SPACING,  "Active-Cut",     "Active-Cut",     3,  null));
        // TSMC
		tRules.add(new DRCTemplate("CO.S.2 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.CUTSPA,    null,             null,            2.8,  "Metal-1-P-Active-Con")); // Decide to put CO.S.2 to avoid misaligments
		tRules.add(new DRCTemplate("CO.S.2 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.CUTSPA2D,    null,             null,            2.8,  "Metal-1-P-Active-Con"));
        tRules.add(new DRCTemplate("CO.S.2 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.CUTSPA,    null,             null,            2.8,  "Metal-1-N-Active-Con"));
        tRules.add(new DRCTemplate("CO.S.2 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.CUTSPA2D,    null,             null,            2.8,  "Metal-1-N-Active-Con"));
		tRules.add(new DRCTemplate("CO.S.2 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Active-Cut",     "Active-Cut",     2.8,  null));

        mRules.add(new DRCTemplate("6.4 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "Active-Cut",     "Transistor-Poly",2,  null));
        tRules.add(new DRCTemplate("CO.C.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Active-Cut",     "Transistor-Poly", 1.6,  null));

		mRules.add(new DRCTemplate("6.5b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.UCONSPA,  "Active-Cut",     "P-Active",       5,  null));
		mRules.add(new DRCTemplate("6.5b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.UCONSPA,  "Active-Cut",     "N-Active",       5,  null));

		mRules.add(new DRCTemplate("6.6b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SPACING,  "Active-Cut",     "Polysilicon-1",  2,  null));
		// 6.7b is not implemented due to complexity. See manual
		mRules.add(new DRCTemplate("6.8b Mosis",       Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.AC.mode(),        DRCTemplate.DRCRuleType.SPACING,  "Active-Cut",     "Poly-Cut",       4,  null));

		mRules.add(new DRCTemplate("7.1 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.MINWID,   "Metal-1",         null,            3,  null));
        tRules.add(new DRCTemplate("M1.W.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Metal-1",         null,            2.3,  null));
        tRules.add(new DRCTemplate("M1.A.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINAREA,   "Metal-1",         null,            20.2,  null));  // TSMC page 39
        tRules.add(new DRCTemplate("Mx.A.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINAREA,   "Metal-2",         null,            20.2,  null));  // TSMC page 39
        tRules.add(new DRCTemplate("Mx.A.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINAREA,   "Metal-3",         null,            20.2,  null));  // TSMC page 39
        tRules.add(new DRCTemplate("Mx.A.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINAREA,   "Metal-4",         null,            20.2,  null));  // TSMC page 39
        tRules.add(new DRCTemplate("Mx.A.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINAREA,   "Metal-5",         null,            20.2,  null));  // TSMC page 39
        tRules.add(new DRCTemplate("M6.A.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINAREA,   "Metal-6",         null,            56.2,  null));  // TSMC page 39
//        tRules.add(new DRCTemplate("AMS.1",  DRCTemplate.DRCCheckMode.TSMC.mode(), DRCTemplate.SLOTSIZE,   "Metal-1",         null,            350,  null));  // TSMC page 78
//        tRules.add(new DRCTemplate("AMS.1",  DRCTemplate.DRCCheckMode.TSMC.mode(), DRCTemplate.SLOTSIZE,   "Metal-2",         null,            350,  null));  // TSMC page 78
//        tRules.add(new DRCTemplate("AMS.1",  DRCTemplate.DRCCheckMode.TSMC.mode(), DRCTemplate.SLOTSIZE,   "Metal-3",         null,            350,  null));  // TSMC page 78
//        tRules.add(new DRCTemplate("AMS.1",  DRCTemplate.DRCCheckMode.TSMC.mode(), DRCTemplate.SLOTSIZE,   "Metal-4",         null,            350,  null));  // TSMC page 78
//        tRules.add(new DRCTemplate("AMS.1",  DRCTemplate.DRCCheckMode.TSMC.mode(), DRCTemplate.SLOTSIZE,   "Metal-5",         null,            350,  null));  // TSMC page 78
//        tRules.add(new DRCTemplate("AMS.1",  DRCTemplate.DRCCheckMode.TSMC.mode(), DRCTemplate.SLOTSIZE,   "Metal-6",         null,            350,  null));  // TSMC page 78

		mRules.add(new DRCTemplate("7.2 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.SPACING,  "Metal-1",        "Metal-1",        3,  null));
        mRules.add(new DRCTemplate("7.2 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Metal-1",        "Metal-1",        2,  null));
        tRules.add(new DRCTemplate("M1.S.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Metal-1",        "Metal-1",        2.3,  null));

		mRules.add(new DRCTemplate("7.4 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-1",        "Metal-1",        6, -1));
		mRules.add(new DRCTemplate("7.4 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-1",        "Metal-1",        4, -1));
        tRules.add(new DRCTemplate("M1.S.2 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-1",        "Metal-1",        6, -1));

		mRules.add(new DRCTemplate("8.1 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.CUTSIZE,   null,             null,            3, "Metal-1-Metal-2-Con"));
		mRules.add(new DRCTemplate("8.1 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            5, "Metal-1-Metal-2-Con"));
		mRules.add(new DRCTemplate("8.1 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.CUTSIZE,   null,             null,            2, "Metal-1-Metal-2-Con"));
        mRules.add(new DRCTemplate("8.1 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            4, "Metal-1-Metal-2-Con"));
        tRules.add(new DRCTemplate("VIAx.W.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.CUTSIZE,   null,             null,            2.6, "Metal-1-Metal-2-Con"));
        tRules.add(new DRCTemplate("M12Con NodeSize TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            4, "Metal-1-Metal-2-Con"));

		mRules.add(new DRCTemplate("8.2 Mosis",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "Via1",           "Via1",           3,  null));
        tRules.add(new DRCTemplate("VIAx.S.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Via1",           "Via1",           2.6,  null));

		mRules.add(new DRCTemplate("8.3 Mosis",  Foundry.Type.MOSIS.mode(),               DRCTemplate.DRCRuleType.VIASUR,   "Metal-1",         null,            1, "Metal-1-Metal-2-Con"));
        tRules.add(new DRCTemplate("> VIAx.E.2 TSMC",  Foundry.Type.TSMC.mode(),               DRCTemplate.DRCRuleType.VIASUR,   "Metal-1",         null,            0.7, "Metal-1-Metal-2-Con"));

		mRules.add(new DRCTemplate("8.4 Mosis",        Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NSV.mode(),       DRCTemplate.DRCRuleType.SPACING,  "Poly-Cut",       "Via1",           2,  null));
		mRules.add(new DRCTemplate("8.4 Mosis",        Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NSV.mode(),       DRCTemplate.DRCRuleType.SPACING,  "Active-Cut",     "Via1",           2,  null));

		mRules.add(new DRCTemplate("8.5 Mosis",        Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NSV.mode(),       DRCTemplate.DRCRuleType.SPACINGE, "Via1",           "Polysilicon-1",  2,  null));
		mRules.add(new DRCTemplate("8.5 Mosis",        Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NSV.mode(),       DRCTemplate.DRCRuleType.SPACINGE, "Via1",           "Transistor-Poly",2,  null));
		mRules.add(new DRCTemplate("8.5 Mosis",        Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NSV.mode(),       DRCTemplate.DRCRuleType.SPACINGE, "Via1",           "Polysilicon-2",  2,  null));
		mRules.add(new DRCTemplate("8.5 Mosis",        Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NSV.mode(),       DRCTemplate.DRCRuleType.SPACINGE, "Via1",           "P-Active",       2,  null));
		mRules.add(new DRCTemplate("8.5 Mosis",        Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.NSV.mode(),       DRCTemplate.DRCRuleType.SPACINGE, "Via1",           "N-Active",       2,  null));

		mRules.add(new DRCTemplate("9.1",  Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.MINWID,   "Metal-2",         null,            3,  null));
        tRules.add(new DRCTemplate("Mx.W.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Metal-2",         null,            2.8,  null));

		mRules.add(new DRCTemplate("9.2 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.SPACING,  "Metal-2",        "Metal-2",        4,  null));
		mRules.add(new DRCTemplate("9.2 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Metal-2",        "Metal-2",        3,  null));
        tRules.add(new DRCTemplate("Mx.S.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Metal-2",        "Metal-2",        2.8,  null));

		mRules.add(new DRCTemplate("9.3 Mosis",  Foundry.Type.MOSIS.mode(),               DRCTemplate.DRCRuleType.VIASUR,   "Metal-2",         null,            1, "Metal-1-Metal-2-Con"));
        tRules.add(new DRCTemplate("> VIAx.E.2 TSMCC",  Foundry.Type.TSMC.mode(),               DRCTemplate.DRCRuleType.VIASUR,   "Metal-2",         null,            0.7, "Metal-1-Metal-2-Con"));

		mRules.add(new DRCTemplate("9.4 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-2",        "Metal-2",        8, -1));
		mRules.add(new DRCTemplate("9.4 Mosis",  Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-2",        "Metal-2",        6, -1));
        tRules.add(new DRCTemplate("Mx.S.2 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-2",        "Metal-2",        6, -1));


		mRules.add(new DRCTemplate("11.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.MINWID,   "Polysilicon-2",   null,            7,  null));
		mRules.add(new DRCTemplate("11.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Polysilicon-2",   null,            3,  null));

		mRules.add(new DRCTemplate("11.2 Mosis", DRCTemplate.DRCMode.ALL.mode(), DRCTemplate.DRCRuleType.SPACING,  "Polysilicon-2",  "Polysilicon-2",  3,  null));

		mRules.add(new DRCTemplate("11.3 Mosis", DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.SURROUND, "Polysilicon-2",  "Polysilicon-1",  5,  "Metal-1-Polysilicon-1-2-Con"));
		mRules.add(new DRCTemplate("11.3 Mosis", DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            15, "Metal-1-Polysilicon-1-2-Con"));
		mRules.add(new DRCTemplate("11.3 Mosis", DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.CUTSUR,    null,             null,            6.5,"Metal-1-Polysilicon-1-2-Con"));
		mRules.add(new DRCTemplate("11.3 Mosis", DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.SURROUND, "Polysilicon-2",  "Polysilicon-1",  2,  "Metal-1-Polysilicon-1-2-Con"));
		mRules.add(new DRCTemplate("11.3 Mosis", DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            9,  "Metal-1-Polysilicon-1-2-Con"));
		mRules.add(new DRCTemplate("11.3 Mosis", DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.CUTSUR,    null,             null,            3.5,"Metal-1-Polysilicon-1-2-Con"));

		mRules.add(new DRCTemplate("14.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.CUTSIZE,   null,             null,            3,  "Metal-2-Metal-3-Con"));
		mRules.add(new DRCTemplate("14.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.MINWID,   "Via2",            null,            3,  null));
		mRules.add(new DRCTemplate("14.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            5,  "Metal-2-Metal-3-Con"));
		mRules.add(new DRCTemplate("14.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.CUTSIZE,   null,             null,            2,  "Metal-2-Metal-3-Con"));
        mRules.add(new DRCTemplate("14.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Via2",            null,            2,  null));
        mRules.add(new DRCTemplate("14.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode()|DRCTemplate.DRCMode.M23.mode(),   DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            6,  "Metal-2-Metal-3-Con"));
		mRules.add(new DRCTemplate("14.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode()|DRCTemplate.DRCMode.M456.mode(),  DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            4,  "Metal-2-Metal-3-Con"));
        tRules.add(new DRCTemplate("VIAx.W.1 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.CUTSIZE,   null,             null,            2.6,  "Metal-2-Metal-3-Con"));
        tRules.add(new DRCTemplate("VIAx.W.1 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Via2",            null,            2.6,  null));
        tRules.add(new DRCTemplate("M2M3 NodeSize TSMC", Foundry.Type.TSMC.mode(),  DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            4,  "Metal-2-Metal-3-Con"));

		mRules.add(new DRCTemplate("14.2 Mosis", Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "Via2",           "Via2",           3,  null));
        tRules.add(new DRCTemplate("VIAx.S.1 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Via2",           "Via2",           2.6,  null));

		mRules.add(new DRCTemplate("14.3 Mosis", Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.VIASUR,   "Metal-2",         null,            1,  "Metal-2-Metal-3-Con"));
        tRules.add(new DRCTemplate("> VIAx.E.2 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.VIASUR,   "Metal-2",         null,            0.7,  "Metal-2-Metal-3-Con"));

		mRules.add(new DRCTemplate("14.4 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode()|DRCTemplate.DRCMode.NSV.mode(),       DRCTemplate.DRCRuleType.SPACING,  "Via1",           "Via2",           2,  null));  /// ?? might need attention

		mRules.add(new DRCTemplate("15.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode()| DRCTemplate.DRCMode.M3.mode(),    DRCTemplate.DRCRuleType.MINWID,   "Metal-3",         null,            6,  null));
		mRules.add(new DRCTemplate("15.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()| DRCTemplate.DRCMode.M3.mode(),    DRCTemplate.DRCRuleType.MINWID,   "Metal-3",         null,            5,  null));
		mRules.add(new DRCTemplate("15.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode()| DRCTemplate.DRCMode.M456.mode(),  DRCTemplate.DRCRuleType.MINWID,   "Metal-3",         null,            3,  null));
		mRules.add(new DRCTemplate("15.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()| DRCTemplate.DRCMode.M456.mode(),  DRCTemplate.DRCRuleType.MINWID,   "Metal-3",         null,            3,  null));
		mRules.add(new DRCTemplate("15.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.MINWID,   "Metal-3",         null,            3,  null));
        tRules.add(new DRCTemplate("Mx.W.1 TSMC", Foundry.Type.TSMC.mode(),  DRCTemplate.DRCRuleType.MINWID,   "Metal-3",         null,            2.8,  null));

		mRules.add(new DRCTemplate("15.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.SPACING,  "Metal-3",        "Metal-3",        4,  null));
		mRules.add(new DRCTemplate("15.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.SPACING,  "Metal-3",        "Metal-3",        3,  null));
        mRules.add(new DRCTemplate("15.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode()|DRCTemplate.DRCMode.M3.mode(),    DRCTemplate.DRCRuleType.SPACING,  "Metal-3",        "Metal-3",        4,  null));
		mRules.add(new DRCTemplate("15.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode()|DRCTemplate.DRCMode.M456.mode(),  DRCTemplate.DRCRuleType.SPACING,  "Metal-3",        "Metal-3",        3,  null));
        tRules.add(new DRCTemplate("Mx.S.1 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Metal-3",        "Metal-3",        2.8,  null));

		mRules.add(new DRCTemplate("15.3 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.VIASUR,   "Metal-3",         null,            1, "Metal-2-Metal-3-Con"));
		mRules.add(new DRCTemplate("15.3 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode()|    DRCTemplate.DRCMode.M3.mode(),    DRCTemplate.DRCRuleType.VIASUR,   "Metal-3",         null,            2, "Metal-2-Metal-3-Con"));
		mRules.add(new DRCTemplate("15.3 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode()|    DRCTemplate.DRCMode.M456.mode(),  DRCTemplate.DRCRuleType.VIASUR,   "Metal-3",         null,            1, "Metal-2-Metal-3-Con"));
        tRules.add(new DRCTemplate("> VIAx.E.2 TSMC", Foundry.Type.TSMC.mode(),  DRCTemplate.DRCRuleType.VIASUR,   "Metal-3",         null,            0.7, "Metal-2-Metal-3-Con"));

		mRules.add(new DRCTemplate("15.4 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-3",        "Metal-3",        8, -1));
		mRules.add(new DRCTemplate("15.4 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-3",        "Metal-3",        6, -1));
		mRules.add(new DRCTemplate("15.4 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode()|DRCTemplate.DRCMode.M3.mode(),    DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-3",        "Metal-3",        8, -1));
		mRules.add(new DRCTemplate("15.4 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode()|DRCTemplate.DRCMode.M456.mode(),  DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-3",        "Metal-3",        6, -1));
        tRules.add(new DRCTemplate("Mx.S.2 TSMC", Foundry.Type.TSMC.mode(),  DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-3",        "Metal-3",        6, -1));

		mRules.add(new DRCTemplate("21.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.CUTSIZE,   null,             null,            3, "Metal-3-Metal-4-Con"));
		mRules.add(new DRCTemplate("21.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.MINWID,   "Via3",            null,            3,  null));
		mRules.add(new DRCTemplate("21.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            5, "Metal-3-Metal-4-Con"));
		mRules.add(new DRCTemplate("21.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.CUTSIZE,   null,             null,            2, "Metal-3-Metal-4-Con"));
        mRules.add(new DRCTemplate("21.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Via3",            null,            2,  null));
		mRules.add(new DRCTemplate("21.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.M4.mode(),    DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            6, "Metal-3-Metal-4-Con"));
		mRules.add(new DRCTemplate("21.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.M56.mode(),   DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            4, "Metal-3-Metal-4-Con"));
		mRules.add(new DRCTemplate("21.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SC.mode(), DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            6, "Metal-3-Metal-4-Con"));
        tRules.add(new DRCTemplate("VIAx.W.1 TSMC",  Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.CUTSIZE,   null,             null,            2.6, "Metal-3-Metal-4-Con"));
        tRules.add(new DRCTemplate("VIAx.W.1 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Via3",            null,            2.6,  null));
        tRules.add(new DRCTemplate("M34 NodeSize TSMC", Foundry.Type.TSMC.mode(),   DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            4, "Metal-3-Metal-4-Con"));

		mRules.add(new DRCTemplate("21.2 Mosis", Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "Via3",           "Via3",           3,  null));
        tRules.add(new DRCTemplate("VIAx.S.1 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Via3",           "Via3",           2.6,  null));

		mRules.add(new DRCTemplate("21.3 Mosis", Foundry.Type.MOSIS.mode(),               DRCTemplate.DRCRuleType.VIASUR,   "Metal-3",         null,            1, "Metal-3-Metal-4-Con"));
        tRules.add(new DRCTemplate("> VIAx.E.2 TSMC", Foundry.Type.TSMC.mode(),               DRCTemplate.DRCRuleType.VIASUR,   "Metal-3",         null,            0.7, "Metal-3-Metal-4-Con"));

		mRules.add(new DRCTemplate("22.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.M4.mode(),    DRCTemplate.DRCRuleType.MINWID,   "Metal-4",         null,            6,  null));
		mRules.add(new DRCTemplate("22.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.M56.mode(),   DRCTemplate.DRCRuleType.MINWID,   "Metal-4",         null,            3,  null));
        tRules.add(new DRCTemplate("Mx.W.1 TSMC", Foundry.Type.TSMC.mode(),   DRCTemplate.DRCRuleType.MINWID,   "Metal-4",         null,            2.8,  null));

		mRules.add(new DRCTemplate("22.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.M4.mode(),    DRCTemplate.DRCRuleType.SPACING,  "Metal-4",        "Metal-4",        6,  null));
		mRules.add(new DRCTemplate("22.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.M56.mode(),   DRCTemplate.DRCRuleType.SPACING,  "Metal-4",        "Metal-4",        4,  null));
		mRules.add(new DRCTemplate("22.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.M56.mode(),   DRCTemplate.DRCRuleType.SPACING,  "Metal-4",        "Metal-4",        3,  null));
        tRules.add(new DRCTemplate("Mx.S.1 TSMC", Foundry.Type.TSMC.mode(),   DRCTemplate.DRCRuleType.SPACING,  "Metal-4",        "Metal-4",        2.8,  null));

		mRules.add(new DRCTemplate("22.3 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.M4.mode(),    DRCTemplate.DRCRuleType.VIASUR,   "Metal-4",         null,            2, "Metal-3-Metal-4-Con"));
		mRules.add(new DRCTemplate("22.3 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.M56.mode(),   DRCTemplate.DRCRuleType.VIASUR,   "Metal-4",         null,            1, "Metal-3-Metal-4-Con"));
        tRules.add(new DRCTemplate("> VIAx.E.2 TSMC", Foundry.Type.TSMC.mode(),   DRCTemplate.DRCRuleType.VIASUR,   "Metal-4",         null,            0.7, "Metal-3-Metal-4-Con"));

		mRules.add(new DRCTemplate("22.4 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.M4.mode(),    DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-4",        "Metal-4",        12, -1));
		mRules.add(new DRCTemplate("22.4 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.M56.mode(),   DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-4",        "Metal-4",        8, -1));
		mRules.add(new DRCTemplate("22.4 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.M56.mode(),   DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-4",        "Metal-4",        6, -1));
        mRules.add(new DRCTemplate("22.4 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.M56.mode(),   DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-4",        "Metal-4",        6, -1));
        tRules.add(new DRCTemplate("Mx.S.2 TSMC", Foundry.Type.TSMC.mode(),   DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-4",        "Metal-4",        6, -1));

		mRules.add(new DRCTemplate("24.1 Mosis", Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.MINWID,  "Thick-Active",    null,            4,  null));
		mRules.add(new DRCTemplate("24.2 Mosis", Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING, "Thick-Active",   "Thick-Active",   4, null));
        tRules.add(new DRCTemplate("OD2.E.2 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,  "Thick-Active",    null,            4,  null)); // No OD2.W.1 found so using an extension function
		tRules.add(new DRCTemplate("OD2.S.1 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING, "Thick-Active",   "Thick-Active",   4.5, null));

		mRules.add(new DRCTemplate("25.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.CUTSIZE,   null,             null,            3, "Metal-4-Metal-5-Con"));
		mRules.add(new DRCTemplate("25.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.MINWID,   "Via4",            null,            3,  null));
		mRules.add(new DRCTemplate("25.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.CUTSIZE,   null,             null,            2, "Metal-4-Metal-5-Con"));
        mRules.add(new DRCTemplate("25.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.MINWID,   "Via4",            null,            2,  null));
        mRules.add(new DRCTemplate("25.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            4, "Metal-4-Metal-5-Con"));
		mRules.add(new DRCTemplate("25.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.M5.mode(),    DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            7, "Metal-4-Metal-5-Con"));
		mRules.add(new DRCTemplate("25.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.M6.mode(),    DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            5, "Metal-4-Metal-5-Con"));
        tRules.add(new DRCTemplate("M45 NodeSize TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            4, "Metal-4-Metal-5-Con"));
		tRules.add(new DRCTemplate("VIAx.W.1 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.CUTSIZE,   null,             null,            2.6, "Metal-4-Metal-5-Con"));
        tRules.add(new DRCTemplate("VIAx.W.1 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Via4",            null,            2.6,  null));

		// Bug even in C-Electric It was DRCRuleType.SPACINGW originally
		mRules.add(new DRCTemplate("25.2 Mosis", Foundry.Type.MOSIS.mode(),               DRCTemplate.DRCRuleType.SPACING, "Via4",           "Via4",           3, null));
        tRules.add(new DRCTemplate("VIAx.S.1 TSMC", Foundry.Type.TSMC.mode(),               DRCTemplate.DRCRuleType.SPACING, "Via4",           "Via4",           2.6, null));

		mRules.add(new DRCTemplate("25.3 Mosis", Foundry.Type.MOSIS.mode(),               DRCTemplate.DRCRuleType.VIASUR,   "Metal-4",         null,            1, "Metal-4-Metal-5-Con"));
        tRules.add(new DRCTemplate("> VIAx.E.2 TSMC", Foundry.Type.TSMC.mode(),               DRCTemplate.DRCRuleType.VIASUR,   "Metal-4",         null,            0.7, "Metal-4-Metal-5-Con"));

		mRules.add(new DRCTemplate("26.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.M5.mode(),    DRCTemplate.DRCRuleType.MINWID,   "Metal-5",         null,            4,  null));
		mRules.add(new DRCTemplate("26.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.M6.mode(),    DRCTemplate.DRCRuleType.MINWID,   "Metal-5",         null,            3,  null));
        tRules.add(new DRCTemplate("Mx.W.1 TSMC", Foundry.Type.TSMC.mode(),    DRCTemplate.DRCRuleType.MINWID,   "Metal-5",         null,            2.8,  null));

		mRules.add(new DRCTemplate("26.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.M5.mode(),    DRCTemplate.DRCRuleType.SPACING,  "Metal-5",        "Metal-5",        4,  null));
		mRules.add(new DRCTemplate("26.2 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.M6.mode(),    DRCTemplate.DRCRuleType.SPACING,  "Metal-5",        "Metal-5",        4,  null));
		mRules.add(new DRCTemplate("26.2 MOSIS", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.M6.mode(),    DRCTemplate.DRCRuleType.SPACING,  "Metal-5",        "Metal-5",        3,  null));
        tRules.add(new DRCTemplate("Mx.S.1 TSMC", Foundry.Type.TSMC.mode(),    DRCTemplate.DRCRuleType.SPACING,  "Metal-5",        "Metal-5",        2.8,  null));

		mRules.add(new DRCTemplate("26.3 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.M5.mode(),    DRCTemplate.DRCRuleType.VIASUR,   "Metal-5",         null,            2, "Metal-4-Metal-5-Con"));
		mRules.add(new DRCTemplate("26.3 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.M5.mode(),    DRCTemplate.DRCRuleType.VIASUR,   "Metal-5",         null,            1, "Metal-4-Metal-5-Con"));
		mRules.add(new DRCTemplate("26.3 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.M6.mode(), DRCTemplate.DRCRuleType.VIASUR,   "Metal-5",         null,            1, "Metal-4-Metal-5-Con"));
        tRules.add(new DRCTemplate("> VIAx.E.2 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.VIASUR,   "Metal-5",         null,            0.7, "Metal-4-Metal-5-Con"));

		mRules.add(new DRCTemplate("26.4 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.M5.mode(), DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-5",        "Metal-5",        8, -1));
		mRules.add(new DRCTemplate("26.4 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.M6.mode(),    DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-5",        "Metal-5",        8, -1));
		mRules.add(new DRCTemplate("26.4 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.M6.mode(),    DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-5",        "Metal-5",        6, -1));
        tRules.add(new DRCTemplate("Mx.S.2 TSMC", Foundry.Type.TSMC.mode(),    DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-5",        "Metal-5",        6, -1));

		mRules.add(new DRCTemplate("29.1 Mosis", DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.CUTSIZE,   null,             null,            4, "Metal-5-Metal-6-Con"));
		mRules.add(new DRCTemplate("29.1 Mosis", DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.MINWID,   "Via5",            null,            4,  null));
		mRules.add(new DRCTemplate("29.1 Mosis", DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            8, "Metal-5-Metal-6-Con"));
		mRules.add(new DRCTemplate("29.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.CUTSIZE,   null,             null,            3, "Metal-5-Metal-6-Con"));
		mRules.add(new DRCTemplate("29.1 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.MINWID,   "Via5",            null,            3,  null));
        mRules.add(new DRCTemplate("29.1 Mosis", DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            5, "Metal-5-Metal-6-Con"));
        tRules.add(new DRCTemplate("VIA5.W.1 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.CUTSIZE,   null,             null,            3.6, "Metal-5-Metal-6-Con"));
		tRules.add(new DRCTemplate("VIA5.W.1 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Via5",            null,            3.6,  null));
        tRules.add(new DRCTemplate("M56Con NodeSize", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.NODSIZ,    null,             null,            5, "Metal-5-Metal-6-Con"));

		mRules.add(new DRCTemplate("29.2 Mosis", Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "Via5",           "Via5",           4,  null));
        tRules.add(new DRCTemplate("VIA5.W.1 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Via5",           "Via5",           3.6,  null));

		mRules.add(new DRCTemplate("29.3 Mosis", Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.VIASUR,   "Metal-5",         null,            1, "Metal-5-Metal-6-Con"));
        tRules.add(new DRCTemplate("M6.E.1 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.VIASUR,   "Metal-5",         null,            0.9, "Metal-5-Metal-6-Con"));

		mRules.add(new DRCTemplate("30.1 Mosis", Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.MINWID,   "Metal-6",         null,            5,  null));
        tRules.add(new DRCTemplate("M6.W.1 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.MINWID,   "Metal-6",         null,            4.4,  null));

		mRules.add(new DRCTemplate("30.2 Mosis", Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACING,  "Metal-6",        "Metal-6",        5,  null));
        tRules.add(new DRCTemplate("M6.S.1 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACING,  "Metal-6",        "Metal-6",        4.6,  null));

		mRules.add(new DRCTemplate("30.3 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.DE.mode(), DRCTemplate.DRCRuleType.VIASUR,   "Metal-6",         null,            2, "Metal-5-Metal-6-Con"));
		mRules.add(new DRCTemplate("30.3 Mosis", Foundry.Type.MOSIS.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.VIASUR,   "Metal-6",         null,            1, "Metal-5-Metal-6-Con"));
        tRules.add(new DRCTemplate("M6.E.1 TSMC", Foundry.Type.TSMC.mode()|DRCTemplate.DRCMode.SU.mode(), DRCTemplate.DRCRuleType.VIASUR,   "Metal-6",         null,            0.9, "Metal-5-Metal-6-Con"));

		mRules.add(new DRCTemplate("30.4 Mosis", Foundry.Type.MOSIS.mode(), DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-6",        "Metal-6",        10, -1));
        tRules.add(new DRCTemplate("M6.S.2 TSMC", Foundry.Type.TSMC.mode(), DRCTemplate.DRCRuleType.SPACINGW, WIDELIMIT, 0, "Metal-6",        "Metal-6",        10, -1));
	};

	// -------------------- private and protected methods ------------------------

	protected MoCMOS()
	{
		super("mocmos");
		setTechShortName("MOSIS CMOS");
		setTechDesc("MOSIS CMOS");
		setFactoryScale(200, true);			// in nanometers: really 0.2 micron
		setNoNegatedArcs();
		setStaticTechnology();
		setFactoryTransparentLayers(new Color []
		{
			new Color( 96,209,255), // Metal-1
			new Color(255,155,192), // Polysilicon-1
			new Color(107,226, 96), // Active
			new Color(224, 95,255), // Metal-2
			new Color(247,251, 20)  // Metal-3
		});

        setFactoryResolution(0.01); // value in lambdas   0.005um -> 0.05 lambdas

        Foundry mosis = new Foundry(Foundry.Type.MOSIS);
        foundries.add(mosis);

        // Reading Mosis rules stored in Mosis.xml
        URL fileURL = MOSRules.class.getResource("Mosis180.xml");
        DRCTemplate.DRCXMLParser parser = DRCTemplate.importDRCDeck(fileURL);
        assert(parser.rulesList.size() == 1);
        mosis.setRules(parser.rulesList.get(0).drcRules);
        setFactoryPrefFoundry(Foundry.Type.MOSIS.name());  // default
//        loadRules(mosis, tsmc);

		//**************************************** LAYERS ****************************************
		/** metal-1 layer */
		metalLayers[0] = Layer.newInstance(this, "Metal-1",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_1, 96,209,255, 0.8,true,
			new int[] { 0x2222,   //   X   X   X   X
						0x0000,   //
						0x8888,   // X   X   X   X
						0x0000,   //
						0x2222,   //   X   X   X   X
						0x0000,   //
						0x8888,   // X   X   X   X
						0x0000,   //
						0x2222,   //   X   X   X   X
						0x0000,   //
						0x8888,   // X   X   X   X
						0x0000,   //
						0x2222,   //   X   X   X   X
						0x0000,   //
						0x8888,   // X   X   X   X
						0x0000}));//

		/** metal-2 layer */
		metalLayers[1] = Layer.newInstance(this, "Metal-2",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_4, 224,95,255, 0.7,true,
			new int[] { 0x1010,   //    X       X
						0x2020,   //   X       X
						0x4040,   //  X       X
						0x8080,   // X       X
						0x0101,   //        X       X
						0x0202,   //       X       X
						0x0404,   //      X       X
						0x0808,   //     X       X
						0x1010,   //    X       X
						0x2020,   //   X       X
						0x4040,   //  X       X
						0x8080,   // X       X
						0x0101,   //        X       X
						0x0202,   //       X       X
						0x0404,   //      X       X
						0x0808}));//     X       X

		/** metal-3 layer */
		metalLayers[2] = Layer.newInstance(this, "Metal-3",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_5, 247,251,20, 0.6,true,
			new int[] { 0x2222,   //   X   X   X   X
						0x0000,   //
						0x8888,   // X   X   X   X
						0x0000,   //
						0x2222,   //   X   X   X   X
						0x0000,   //
						0x8888,   // X   X   X   X
						0x0000,   //
						0x2222,   //   X   X   X   X
						0x0000,   //
						0x8888,   // X   X   X   X
						0x0000,   //
						0x2222,   //   X   X   X   X
						0x0000,   //
						0x8888,   // X   X   X   X
						0x0000}));//

		/** metal-4 layer */
		metalLayers[3] = Layer.newInstance(this, "Metal-4",
			new EGraphics(true, true, null, 0, 150,150,255, 0.5,true,
			new int[] { 0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000}));//

		/** metal-5 layer */
		metalLayers[4] = Layer.newInstance(this, "Metal-5",
			new EGraphics(true, true, EGraphics.Outline.PAT_S, 0, 255,190,6, 0.4,true,
			new int[] { 0x8888,   // X   X   X   X
						0x1111,   //    X   X   X   X
						0x2222,   //   X   X   X   X
						0x4444,   //  X   X   X   X
						0x8888,   // X   X   X   X
						0x1111,   //    X   X   X   X
						0x2222,   //   X   X   X   X
						0x4444,   //  X   X   X   X
						0x8888,   // X   X   X   X
						0x1111,   //    X   X   X   X
						0x2222,   //   X   X   X   X
						0x4444,   //  X   X   X   X
						0x8888,   // X   X   X   X
						0x1111,   //    X   X   X   X
						0x2222,   //   X   X   X   X
						0x4444}));//  X   X   X   X

		/** metal-6 layer */
		metalLayers[5] = Layer.newInstance(this, "Metal-6",
			new EGraphics(true, true, null, 0, 0,255,255, 0.3,true,
			new int[] { 0x8888,   // X   X   X   X
						0x4444,   //  X   X   X   X
						0x2222,   //   X   X   X   X
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X
						0x4444,   //  X   X   X   X
						0x2222,   //   X   X   X   X
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X
						0x4444,   //  X   X   X   X
						0x2222,   //   X   X   X   X
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X
						0x4444,   //  X   X   X   X
						0x2222,   //   X   X   X   X
						0x1111}));//    X   X   X   X

		/** poly layer */
		poly1Layer = Layer.newInstance(this, "Polysilicon-1",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_2, 255,155,192, 1,true,
			new int[] { 0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555}));//  X X X X X X X X

		/** poly2 layer */
		Layer poly2_lay = Layer.newInstance(this, "Polysilicon-2",
			new EGraphics(true, true, null, 0, 255,190,6, 1,true,
			new int[] { 0xAFAF,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X
						0xFAFA,   // XXXXX X XXXXX X
						0x8888,   // X   X   X   X
						0xAFAF,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X
						0xFAFA,   // XXXXX X XXXXX X
						0x8888,   // X   X   X   X
						0xAFAF,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X
						0xFAFA,   // XXXXX X XXXXX X
						0x8888,   // X   X   X   X
						0xAFAF,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X
						0xFAFA,   // XXXXX X XXXXX X
						0x8888}));// X   X   X   X

		/** P active layer */
		activeLayers[P_TYPE] = Layer.newInstance(this, "P-Active",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_3, 107,226,96, 1,true,
			new int[] { 0x0000,   //
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X
						0x0303,   //       XX      XX
						0x0000,   //
						0x3030,   //   XX      XX
						0x8484,   // X    X  X    X
						0x3030,   //   XX      XX
						0x0000,   //
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X
						0x0303,   //       XX      XX
						0x0000,   //
						0x3030,   //   XX      XX
						0x8484,   // X    X  X    X
						0x3030}));//   XX      XX

		/** N active layer */
		activeLayers[N_TYPE] = Layer.newInstance(this, "N-Active",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_3, 107,226,96, 1,true,
			new int[] { 0x0000,   //
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X
						0x0303,   //       XX      XX
						0x0000,   //
						0x3030,   //   XX      XX
						0x8484,   // X    X  X    X
						0x3030,   //   XX      XX
						0x0000,   //
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X
						0x0303,   //       XX      XX
						0x0000,   //
						0x3030,   //   XX      XX
						0x8484,   // X    X  X    X
						0x3030}));//   XX      XX

        selectLayers = new Layer[2];
		/** P Select layer */
		selectLayers[P_TYPE] = Layer.newInstance(this, "P-Select",
			new EGraphics(true, true, null, 0, 255,255,0, 1,false,
			new int[] { 0x1010,   //    X       X
						0x2020,   //   X       X
						0x4040,   //  X       X
						0x8080,   // X       X
						0x0101,   //        X       X
						0x0202,   //       X       X
						0x0404,   //      X       X
						0x0808,   //     X       X
						0x1010,   //    X       X
						0x2020,   //   X       X
						0x4040,   //  X       X
						0x8080,   // X       X
						0x0101,   //        X       X
						0x0202,   //       X       X
						0x0404,   //      X       X
						0x0808}));//     X       X

		/** N Select layer */
		selectLayers[N_TYPE] = Layer.newInstance(this, "N-Select",
			new EGraphics(true, true, null, 0, 255,255,0, 1,false,
			new int[] { 0x0101,   //        X       X
						0x0000,   //
						0x1010,   //    X       X
						0x0000,   //
						0x0101,   //        X       X
						0x0000,   //
						0x1010,   //    X       X
						0x0000,   //
						0x0101,   //        X       X
						0x0000,   //
						0x1010,   //    X       X
						0x0000,   //
						0x0101,   //        X       X
						0x0000,   //
						0x1010,   //    X       X
						0x0000}));//

		/** P Well layer */
		wellLayers[P_TYPE] = Layer.newInstance(this, "P-Well",
			new EGraphics(true, true, null, 0, 139,99,46, 1,false,
			new int[] { 0x0202,   //       X       X
						0x0101,   //        X       X
						0x8080,   // X       X
						0x4040,   //  X       X
						0x2020,   //   X       X
						0x1010,   //    X       X
						0x0808,   //     X       X
						0x0404,   //      X       X
						0x0202,   //       X       X
						0x0101,   //        X       X
						0x8080,   // X       X
						0x4040,   //  X       X
						0x2020,   //   X       X
						0x1010,   //    X       X
						0x0808,   //     X       X
						0x0404}));//      X       X

		/** N Well implant */
		wellLayers[N_TYPE] = Layer.newInstance(this, "N-Well",
			new EGraphics(true, true, null, 0, 139,99,46, 1,false,
			new int[] { 0x0202,   //       X       X
						0x0000,   //
						0x2020,   //   X       X
						0x0000,   //
						0x0202,   //       X       X
						0x0000,   //
						0x2020,   //   X       X
						0x0000,   //
						0x0202,   //       X       X
						0x0000,   //
						0x2020,   //   X       X
						0x0000,   //
						0x0202,   //       X       X
						0x0000,   //
						0x2020,   //   X       X
						0x0000}));//

		/** poly cut layer */
		polyCutLayer = Layer.newInstance(this, "Poly-Cut",
			new EGraphics(false, false, null, 0, 100,100,100, 1,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** active cut layer */
		activeCutLayer = Layer.newInstance(this, "Active-Cut",
			new EGraphics(false, false, null, 0, 100,100,100, 1,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via1 layer */
		Layer via1_lay = Layer.newInstance(this, "Via1",
			new EGraphics(false, false, null, 0, 180,180,180, 1,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via2 layer */
		Layer via2_lay = Layer.newInstance(this, "Via2",
			new EGraphics(false, false, null, 0, 180,180,180, 1,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via3 layer */
		Layer via3_lay = Layer.newInstance(this, "Via3",
			new EGraphics(false, false, null, 0, 180,180,180, 1,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via4 layer */
		Layer via4_lay = Layer.newInstance(this, "Via4",
			new EGraphics(false, false, null, 0, 180,180,180, 1,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via5 layer */
		Layer via5_lay = Layer.newInstance(this, "Via5",
			new EGraphics(false, false, null, 0, 180,180,180, 1,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** passivation layer */
		Layer passivation_lay = Layer.newInstance(this, "Passivation",
			new EGraphics(true, true, null, 0, 100,100,100, 1,true,
			new int[] { 0x1C1C,   //    XXX     XXX
						0x3E3E,   //   XXXXX   XXXXX
						0x3636,   //   XX XX   XX XX
						0x3E3E,   //   XXXXX   XXXXX
						0x1C1C,   //    XXX     XXX
						0x0000,   //
						0x0000,   //
						0x0000,   //
						0x1C1C,   //    XXX     XXX
						0x3E3E,   //   XXXXX   XXXXX
						0x3636,   //   XX XX   XX XX
						0x3E3E,   //   XXXXX   XXXXX
						0x1C1C,   //    XXX     XXX
						0x0000,   //
						0x0000,   //
						0x0000}));//

		/** poly/trans layer */
		transistorPolyLayer = Layer.newInstance(this, "Transistor-Poly",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_2, 255,155,192, 1,true,
			new int[] { 0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555}));//  X X X X X X X X

		/** poly cap layer */
		Layer polyCap_lay = Layer.newInstance(this, "Poly-Cap",
			new EGraphics(false, false, null, 0, 0,0,0, 1,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P act well layer */
		pActiveWellLayer = Layer.newInstance(this, "P-Active-Well",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_3, 107,226,96, 1,false,
			new int[] { 0x0000,   //
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X
						0x0303,   //       XX      XX
						0x0000,   //
						0x3030,   //   XX      XX
						0x8484,   // X    X  X    X
						0x3030,   //   XX      XX
						0x0000,   //
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X
						0x0303,   //       XX      XX
						0x0000,   //
						0x3030,   //   XX      XX
						0x8484,   // X    X  X    X
						0x3030}));//   XX      XX

		/** Silicide block */
        /** Resist Protection Oxide (RPO) Same graphics as in 90nm tech */
		silicideBlockLayer = Layer.newInstance(this, "Silicide-Block",
            new EGraphics(true, true, null, EGraphics.TRANSPARENT_2, 192,255,255, 1,true,
            new int[] { 0x1010,  /*    X       X     */
                        0x2828,   /*   X X     X X    */
                        0x4444,   /*  X   X   X   X   */
                        0x8282,   /* X     X X     X  */
                        0x0101,   /*        X       X */
                        0x0000,   /*                  */
                        0x0000,   /*                  */
                        0x0000,   /*                  */
                        0x1010,   /*    X       X     */
                        0x2828,   /*   X X     X X    */
                        0x4444,   /*  X   X   X   X   */
                        0x8282,   /* X     X X     X  */
                        0x0101,   /*        X       X */
                        0x0000,   /*                  */
                        0x0000,   /*                  */
                        0x0000}));/*                  */


		/** Thick active */
		Layer thickActive_lay = Layer.newInstance(this, "Thick-Active",
			new EGraphics(true, true, null, 0, 0,0,0, 1,false,
			new int[] { 0x4040,   //  X       X
						0x8080,   // X       X
						0x0101,   //        X       X
						0x0202,   //       X       X
						0x0101,   //        X       X
						0x8080,   // X       X
						0x4040,   //  X       X
						0x2020,   //   X       X
						0x4040,   //  X       X
						0x8080,   // X       X
						0x0101,   //        X       X
						0x0202,   //       X       X
						0x0101,   //        X       X
						0x8080,   // X       X
						0x4040,   //  X       X
						0x2020}));//   X       X

		/** pseudo metal 1 */
		Layer pseudoMetal1_lay = Layer.newInstance(this, "Pseudo-Metal-1",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_1, 96,209,255, 1,true,
			new int[] { 0x2222,   //   X   X   X   X
						0x0000,   //
						0x8888,   // X   X   X   X
						0x0000,   //
						0x2222,   //   X   X   X   X
						0x0000,   //
						0x8888,   // X   X   X   X
						0x0000,   //
						0x2222,   //   X   X   X   X
						0x0000,   //
						0x8888,   // X   X   X   X
						0x0000,   //
						0x2222,   //   X   X   X   X
						0x0000,   //
						0x8888,   // X   X   X   X
						0x0000}));//

		/** pseudo metal-2 */
		Layer pseudoMetal2_lay = Layer.newInstance(this, "Pseudo-Metal-2",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_4, 224,95,255, 1,true,
			new int[] { 0x1010,   //    X       X
						0x2020,   //   X       X
						0x4040,   //  X       X
						0x8080,   // X       X
						0x0101,   //        X       X
						0x0202,   //       X       X
						0x0404,   //      X       X
						0x0808,   //     X       X
						0x1010,   //    X       X
						0x2020,   //   X       X
						0x4040,   //  X       X
						0x8080,   // X       X
						0x0101,   //        X       X
						0x0202,   //       X       X
						0x0404,   //      X       X
						0x0808}));//     X       X

		/** pseudo metal-3 */
		Layer pseudoMetal3_lay = Layer.newInstance(this, "Pseudo-Metal-3",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_5, 247,251,20, 1,true,
			new int[] { 0x1010,   //    X       X
						0x2020,   //   X       X
						0x4040,   //  X       X
						0x8080,   // X       X
						0x0101,   //        X       X
						0x0202,   //       X       X
						0x0404,   //      X       X
						0x0808,   //     X       X
						0x1010,   //    X       X
						0x2020,   //   X       X
						0x4040,   //  X       X
						0x8080,   // X       X
						0x0101,   //        X       X
						0x0202,   //       X       X
						0x0404,   //      X       X
						0x0808}));//     X       X

		/** pseudo metal-4 */
		Layer pseudoMetal4_lay = Layer.newInstance(this, "Pseudo-Metal-4",
			new EGraphics(true, true, null, 0, 150,150,255, 1,true,
			new int[] { 0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000,   //
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x0000}));//

		/** pseudo metal-5 */
		Layer pseudoMetal5_lay = Layer.newInstance(this, "Pseudo-Metal-5",
			new EGraphics(true, true, EGraphics.Outline.PAT_S, 0, 255,190,6, 1,true,
			new int[] { 0x8888,   // X   X   X   X
						0x1111,   //    X   X   X   X
						0x2222,   //   X   X   X   X
						0x4444,   //  X   X   X   X
						0x8888,   // X   X   X   X
						0x1111,   //    X   X   X   X
						0x2222,   //   X   X   X   X
						0x4444,   //  X   X   X   X
						0x8888,   // X   X   X   X
						0x1111,   //    X   X   X   X
						0x2222,   //   X   X   X   X
						0x4444,   //  X   X   X   X
						0x8888,   // X   X   X   X
						0x1111,   //    X   X   X   X
						0x2222,   //   X   X   X   X
						0x4444}));//  X   X   X   X

		/** pseudo metal-6 */
		Layer pseudoMetal6_lay = Layer.newInstance(this, "Pseudo-Metal-6",
			new EGraphics(true, true, null, 0, 0,255,255, 1,true,
			new int[] { 0x8888,   // X   X   X   X
						0x4444,   //  X   X   X   X
						0x2222,   //   X   X   X   X
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X
						0x4444,   //  X   X   X   X
						0x2222,   //   X   X   X   X
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X
						0x4444,   //  X   X   X   X
						0x2222,   //   X   X   X   X
						0x1111,   //    X   X   X   X
						0x8888,   // X   X   X   X
						0x4444,   //  X   X   X   X
						0x2222,   //   X   X   X   X
						0x1111}));//    X   X   X   X

		/** pseudo poly layer */
		Layer pseudoPoly1_lay = Layer.newInstance(this, "Pseudo-Polysilicon",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_2, 255,155,192, 1,true,
			new int[] { 0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555}));//  X X X X X X X X

		/** pseudo poly2 layer */
		Layer pseudoPoly2_lay = Layer.newInstance(this, "Pseudo-Electrode",
			new EGraphics(true, true, null, 0, 255,190,6, 1,true,
			new int[] { 0xAFAF,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X
						0xFAFA,   // XXXXX X XXXXX X
						0x8888,   // X   X   X   X
						0xAFAF,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X
						0xFAFA,   // XXXXX X XXXXX X
						0x8888,   // X   X   X   X
						0xAFAF,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X
						0xFAFA,   // XXXXX X XXXXX X
						0x8888,   // X   X   X   X
						0xAFAF,   // X X XXXXX X XXXX
						0x8888,   // X   X   X   X
						0xFAFA,   // XXXXX X XXXXX X
						0x8888}));// X   X   X   X

		/** pseudo P active */
		Layer pseudoPActive_lay = Layer.newInstance(this, "Pseudo-P-Active",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_3, 107,226,96, 1,true,
			new int[] { 0x0000,   //
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X
						0x0303,   //       XX      XX
						0x0000,   //
						0x3030,   //   XX      XX
						0x8484,   // X    X  X    X
						0x3030,   //   XX      XX
						0x0000,   //
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X
						0x0303,   //       XX      XX
						0x0000,   //
						0x3030,   //   XX      XX
						0x8484,   // X    X  X    X
						0x3030}));//   XX      XX

		/** pseudo N active */
		Layer pseudoNActive_lay = Layer.newInstance(this, "Pseudo-N-Active",
			new EGraphics(false, true, null, EGraphics.TRANSPARENT_3, 107,226,96, 1,true,
			new int[] { 0x0000,   //
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X
						0x0303,   //       XX      XX
						0x0000,   //
						0x3030,   //   XX      XX
						0x8484,   // X    X  X    X
						0x3030,   //   XX      XX
						0x0000,   //
						0x0303,   //       XX      XX
						0x4848,   //  X  X    X  X
						0x0303,   //       XX      XX
						0x0000,   //
						0x3030,   //   XX      XX
						0x8484,   // X    X  X    X
						0x3030}));//   XX      XX

		/** pseudo P Select */
		Layer pseudoPSelect_lay = Layer.newInstance(this, "Pseudo-P-Select",
			new EGraphics(true, true, null, 0, 255,255,0, 1,false,
			new int[] { 0x1010,   //    X       X
						0x2020,   //   X       X
						0x4040,   //  X       X
						0x8080,   // X       X
						0x0101,   //        X       X
						0x0202,   //       X       X
						0x0404,   //      X       X
						0x0808,   //     X       X
						0x1010,   //    X       X
						0x2020,   //   X       X
						0x4040,   //  X       X
						0x8080,   // X       X
						0x0101,   //        X       X
						0x0202,   //       X       X
						0x0404,   //      X       X
						0x0808}));//     X       X

		/** pseudo N Select */
		Layer pseudoNSelect_lay = Layer.newInstance(this, "Pseudo-N-Select",
			new EGraphics(true, true, null, 0, 255,255,0, 1,false,
			new int[] { 0x0101,   //        X       X
						0x0000,   //
						0x1010,   //    X       X
						0x0000,   //
						0x0101,   //        X       X
						0x0000,   //
						0x1010,   //    X       X
						0x0000,   //
						0x0101,   //        X       X
						0x0000,   //
						0x1010,   //    X       X
						0x0000,   //
						0x0101,   //        X       X
						0x0000,   //
						0x1010,   //    X       X
						0x0000}));//

		/** pseudo P Well */
		Layer pseudoPWell_lay = Layer.newInstance(this, "Pseudo-P-Well",
			new EGraphics(true, true, null, 0, 139,99,46, 1,false,
			new int[] { 0x0202,   //       X       X
						0x0101,   //        X       X
						0x8080,   // X       X
						0x4040,   //  X       X
						0x2020,   //   X       X
						0x1010,   //    X       X
						0x0808,   //     X       X
						0x0404,   //      X       X
						0x0202,   //       X       X
						0x0101,   //        X       X
						0x8080,   // X       X
						0x4040,   //  X       X
						0x2020,   //   X       X
						0x1010,   //    X       X
						0x0808,   //     X       X
						0x0404}));//      X       X

		/** pseudo N Well */
		Layer pseudoNWell_lay = Layer.newInstance(this, "Pseudo-N-Well",
			new EGraphics(true, true, null, 0, 139,99,46, 1,false,
			new int[] { 0x0202,   //       X       X
						0x0000,   //
						0x2020,   //   X       X
						0x0000,   //
						0x0202,   //       X       X
						0x0000,   //
						0x2020,   //   X       X
						0x0000,   //
						0x0202,   //       X       X
						0x0000,   //
						0x2020,   //   X       X
						0x0000,   //
						0x0202,   //       X       X
						0x0000,   //
						0x2020,   //   X       X
						0x0000}));//

		/** pad frame */
		Layer padFrame_lay = Layer.newInstance(this, "Pad-Frame",
			new EGraphics(false, true, null, 0, 255,0,0, 1,false,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		// The layer functions
		metalLayers[0].setFunction(Layer.Function.METAL1);									// Metal-1
		metalLayers[1].setFunction(Layer.Function.METAL2);									// Metal-2
		metalLayers[2].setFunction(Layer.Function.METAL3);									// Metal-3
		metalLayers[3].setFunction(Layer.Function.METAL4);									// Metal-4
		metalLayers[4].setFunction(Layer.Function.METAL5);									// Metal-5
		metalLayers[5].setFunction(Layer.Function.METAL6);									// Metal-6
		poly1Layer.setFunction(Layer.Function.POLY1);									// Polysilicon-1
		poly2_lay.setFunction(Layer.Function.POLY2);									// Polysilicon-2
		activeLayers[P_TYPE].setFunction(Layer.Function.DIFFP);									// P-Active
		activeLayers[N_TYPE].setFunction(Layer.Function.DIFFN);									// N-Active
		selectLayers[P_TYPE].setFunction(Layer.Function.IMPLANTP);								// P-Select
		selectLayers[N_TYPE].setFunction(Layer.Function.IMPLANTN);								// N-Select
		wellLayers[P_TYPE].setFunction(Layer.Function.WELLP);									// P-Well
		wellLayers[N_TYPE].setFunction(Layer.Function.WELLN);									// N-Well
		polyCutLayer.setFunction(Layer.Function.CONTACT1, Layer.Function.CONPOLY);		// Poly-Cut
		activeCutLayer.setFunction(Layer.Function.CONTACT1, Layer.Function.CONDIFF);		// Active-Cut
		via1_lay.setFunction(Layer.Function.CONTACT2, Layer.Function.CONMETAL);			// Via-1
		via2_lay.setFunction(Layer.Function.CONTACT3, Layer.Function.CONMETAL);			// Via-2
		via3_lay.setFunction(Layer.Function.CONTACT4, Layer.Function.CONMETAL);			// Via-3
		via4_lay.setFunction(Layer.Function.CONTACT5, Layer.Function.CONMETAL);			// Via-4
		via5_lay.setFunction(Layer.Function.CONTACT6, Layer.Function.CONMETAL);			// Via-5
		passivation_lay.setFunction(Layer.Function.OVERGLASS);							// Passivation
		transistorPolyLayer.setFunction(Layer.Function.GATE);							// Transistor-Poly
		polyCap_lay.setFunction(Layer.Function.CAP);									// Poly-Cap
		pActiveWellLayer.setFunction(Layer.Function.DIFFP);								// P-Active-Well
		silicideBlockLayer.setFunction(Layer.Function.ART);								// Silicide-Block

		thickActive_lay.setFunction(Layer.Function.DIFF, Layer.Function.THICK);			// Thick-Active
		pseudoMetal1_lay.setFunction(Layer.Function.METAL1, Layer.Function.PSEUDO);		// Pseudo-Metal-1
		pseudoMetal2_lay.setFunction(Layer.Function.METAL2, Layer.Function.PSEUDO);		// Pseudo-Metal-2
		pseudoMetal3_lay.setFunction(Layer.Function.METAL3, Layer.Function.PSEUDO);		// Pseudo-Metal-3
		pseudoMetal4_lay.setFunction(Layer.Function.METAL4, Layer.Function.PSEUDO);		// Pseudo-Metal-4
		pseudoMetal5_lay.setFunction(Layer.Function.METAL5, Layer.Function.PSEUDO);		// Pseudo-Metal-5
		pseudoMetal6_lay.setFunction(Layer.Function.METAL6, Layer.Function.PSEUDO);		// Pseudo-Metal-6
		pseudoPoly1_lay.setFunction(Layer.Function.POLY1, Layer.Function.PSEUDO);		// Pseudo-Polysilicon-1
		pseudoPoly2_lay.setFunction(Layer.Function.POLY2, Layer.Function.PSEUDO);		// Pseudo-Polysilicon-2
		pseudoPActive_lay.setFunction(Layer.Function.DIFFP, Layer.Function.PSEUDO);		// Pseudo-P-Active
		pseudoNActive_lay.setFunction(Layer.Function.DIFFN, Layer.Function.PSEUDO);		// Pseudo-N-Active
		pseudoPSelect_lay.setFunction(Layer.Function.IMPLANTP, Layer.Function.PSEUDO);	// Pseudo-P-Select
		pseudoNSelect_lay.setFunction(Layer.Function.IMPLANTN, Layer.Function.PSEUDO);	// Pseudo-N-Select
		pseudoPWell_lay.setFunction(Layer.Function.WELLP, Layer.Function.PSEUDO);		// Pseudo-P-Well
		pseudoNWell_lay.setFunction(Layer.Function.WELLN, Layer.Function.PSEUDO);		// Pseudo-N-Well
		padFrame_lay.setFunction(Layer.Function.ART);									// Pad-Frame

		// The CIF names
		metalLayers[0].setFactoryCIFLayer("CMF");				// Metal-1
		metalLayers[1].setFactoryCIFLayer("CMS");				// Metal-2
		metalLayers[2].setFactoryCIFLayer("CMT");				// Metal-3
		metalLayers[3].setFactoryCIFLayer("CMQ");				// Metal-4
		metalLayers[4].setFactoryCIFLayer("CMP");				// Metal-5
		metalLayers[5].setFactoryCIFLayer("CM6");				// Metal-6
		poly1Layer.setFactoryCIFLayer("CPG");				// Polysilicon-1
		poly2_lay.setFactoryCIFLayer("CEL");				// Polysilicon-2
		activeLayers[P_TYPE].setFactoryCIFLayer("CAA");				// P-Active
		activeLayers[N_TYPE].setFactoryCIFLayer("CAA");				// N-Active
		selectLayers[P_TYPE].setFactoryCIFLayer("CSP");				// P-Select
		selectLayers[N_TYPE].setFactoryCIFLayer("CSN");				// N-Select
		wellLayers[P_TYPE].setFactoryCIFLayer("CWP");				// P-Well
		wellLayers[N_TYPE].setFactoryCIFLayer("CWN");				// N-Well
		polyCutLayer.setFactoryCIFLayer("CCC");				// Poly-Cut
		activeCutLayer.setFactoryCIFLayer("CCC");			// Active-Cut
		via1_lay.setFactoryCIFLayer("CVA");					// Via-1
		via2_lay.setFactoryCIFLayer("CVS");					// Via-2
		via3_lay.setFactoryCIFLayer("CVT");					// Via-3
		via4_lay.setFactoryCIFLayer("CVQ");					// Via-4
		via5_lay.setFactoryCIFLayer("CV5");					// Via-5
		passivation_lay.setFactoryCIFLayer("COG");			// Passivation
		transistorPolyLayer.setFactoryCIFLayer("CPG");		// Transistor-Poly
		polyCap_lay.setFactoryCIFLayer("CPC");				// Poly-Cap
		pActiveWellLayer.setFactoryCIFLayer("CAA");			// P-Active-Well
		silicideBlockLayer.setFactoryCIFLayer("CSB");		// Silicide-Block
		thickActive_lay.setFactoryCIFLayer("CTA");			// Thick-Active
		pseudoMetal1_lay.setFactoryCIFLayer("");			// Pseudo-Metal-1
		pseudoMetal2_lay.setFactoryCIFLayer("");			// Pseudo-Metal-2
		pseudoMetal3_lay.setFactoryCIFLayer("");			// Pseudo-Metal-3
		pseudoMetal4_lay.setFactoryCIFLayer("");			// Pseudo-Metal-4
		pseudoMetal5_lay.setFactoryCIFLayer("");			// Pseudo-Metal-5
		pseudoMetal6_lay.setFactoryCIFLayer("");			// Pseudo-Metal-6
		pseudoPoly1_lay.setFactoryCIFLayer("");				// Pseudo-Polysilicon-1
		pseudoPoly2_lay.setFactoryCIFLayer("");				// Pseudo-Polysilicon-2
		pseudoPActive_lay.setFactoryCIFLayer("");			// Pseudo-P-Active
		pseudoNActive_lay.setFactoryCIFLayer("");			// Pseudo-N-Active
		pseudoPSelect_lay.setFactoryCIFLayer("CSP");		// Pseudo-P-Select
		pseudoNSelect_lay.setFactoryCIFLayer("CSN");		// Pseudo-N-Select
		pseudoPWell_lay.setFactoryCIFLayer("CWP");			// Pseudo-P-Well
		pseudoNWell_lay.setFactoryCIFLayer("CWN");			// Pseudo-N-Well
		padFrame_lay.setFactoryCIFLayer("XP");				// Pad-Frame

		// The GDS names
		metalLayers[0].setFactoryGDSLayer("49, 80p, 80t", Foundry.Type.MOSIS.name());				// Metal-1 Mosis
        metalLayers[0].setFactoryGDSLayer("16, 40p, 40t", Foundry.Type.TSMC.name());				// Metal-1 TSMC
		metalLayers[1].setFactoryGDSLayer("51, 82p, 82t", Foundry.Type.MOSIS.name());				// Metal-2
        metalLayers[1].setFactoryGDSLayer("18, 41p, 41t", Foundry.Type.TSMC.name());				// Metal-2
		metalLayers[2].setFactoryGDSLayer("62, 93p, 93t", Foundry.Type.MOSIS.name());				// Metal-3
        metalLayers[2].setFactoryGDSLayer("28, 42p, 42t", Foundry.Type.TSMC.name());				// Metal-3
		metalLayers[3].setFactoryGDSLayer("31, 63p, 63t", Foundry.Type.MOSIS.name());				// Metal-4
        metalLayers[3].setFactoryGDSLayer("31, 43p, 43t", Foundry.Type.TSMC.name());
		metalLayers[4].setFactoryGDSLayer("33, 64p, 64t", Foundry.Type.MOSIS.name());				// Metal-5
        metalLayers[4].setFactoryGDSLayer("33, 44p, 44t", Foundry.Type.TSMC.name());				// Metal-5
		metalLayers[5].setFactoryGDSLayer("37, 68p, 68t", Foundry.Type.MOSIS.name());				// Metal-6
        metalLayers[5].setFactoryGDSLayer("38, 45p, 45t", Foundry.Type.TSMC.name());				// Metal-6
		poly1Layer.setFactoryGDSLayer("46, 77p, 77t", Foundry.Type.MOSIS.name());					// Polysilicon-1
        poly1Layer.setFactoryGDSLayer("13, 47p, 47t", Foundry.Type.TSMC.name());					// Polysilicon-1
		transistorPolyLayer.setFactoryGDSLayer("46", Foundry.Type.MOSIS.name());		// Transistor-Poly
        transistorPolyLayer.setFactoryGDSLayer("13", Foundry.Type.TSMC.name());		// Transistor-Poly
		poly2_lay.setFactoryGDSLayer("56", Foundry.Type.MOSIS.name());					// Polysilicon-2
		activeLayers[P_TYPE].setFactoryGDSLayer("43", Foundry.Type.MOSIS.name());				// P-Active
        activeLayers[P_TYPE].setFactoryGDSLayer("3", Foundry.Type.TSMC.name());				// P-Active
		activeLayers[N_TYPE].setFactoryGDSLayer("43", Foundry.Type.MOSIS.name());				// N-Active
        activeLayers[N_TYPE].setFactoryGDSLayer("3", Foundry.Type.TSMC.name());				// N-Active
		pActiveWellLayer.setFactoryGDSLayer("43", Foundry.Type.MOSIS.name());			// P-Active-Well
        pActiveWellLayer.setFactoryGDSLayer("3", Foundry.Type.TSMC.name());			// P-Active-Well
		selectLayers[P_TYPE].setFactoryGDSLayer("44", Foundry.Type.MOSIS.name());				// P-Select
        selectLayers[P_TYPE].setFactoryGDSLayer("7", Foundry.Type.TSMC.name());				// P-Select
		selectLayers[N_TYPE].setFactoryGDSLayer("45", Foundry.Type.MOSIS.name());				// N-Select
        selectLayers[N_TYPE].setFactoryGDSLayer("8", Foundry.Type.TSMC.name());				// N-Select
		wellLayers[P_TYPE].setFactoryGDSLayer("41", Foundry.Type.MOSIS.name());					// P-Well
        wellLayers[P_TYPE].setFactoryGDSLayer("41", Foundry.Type.TSMC.name());					// P-Well
		wellLayers[N_TYPE].setFactoryGDSLayer("42", Foundry.Type.MOSIS.name());					// N-Well
        wellLayers[N_TYPE].setFactoryGDSLayer("2", Foundry.Type.TSMC.name());					// N-Well
		polyCutLayer.setFactoryGDSLayer("25", Foundry.Type.MOSIS.name());				// Poly-Cut
        polyCutLayer.setFactoryGDSLayer("15", Foundry.Type.TSMC.name());				// Poly-Cut
		activeCutLayer.setFactoryGDSLayer("25", Foundry.Type.MOSIS.name());				// Active-Cut
		activeCutLayer.setFactoryGDSLayer("15", Foundry.Type.TSMC.name());				// Active-Cut
		via1_lay.setFactoryGDSLayer("50", Foundry.Type.MOSIS.name());					// Via-1
        via1_lay.setFactoryGDSLayer("17", Foundry.Type.TSMC.name());					// Via-1
		via2_lay.setFactoryGDSLayer("61", Foundry.Type.MOSIS.name());					// Via-2
        via2_lay.setFactoryGDSLayer("27", Foundry.Type.TSMC.name());					// Via-2
		via3_lay.setFactoryGDSLayer("30", Foundry.Type.MOSIS.name());					// Via-3
        via3_lay.setFactoryGDSLayer("29", Foundry.Type.TSMC.name());					// Via-3
		via4_lay.setFactoryGDSLayer("32", Foundry.Type.MOSIS.name());					// Via-4
        via4_lay.setFactoryGDSLayer("32", Foundry.Type.TSMC.name());					// Via-4
		via5_lay.setFactoryGDSLayer("36", Foundry.Type.MOSIS.name());					// Via-5
        via5_lay.setFactoryGDSLayer("39", Foundry.Type.TSMC.name());					// Via-5
		passivation_lay.setFactoryGDSLayer("52", Foundry.Type.MOSIS.name());			// Passivation
        passivation_lay.setFactoryGDSLayer("19", Foundry.Type.TSMC.name());			// Passivation
		polyCap_lay.setFactoryGDSLayer("28", Foundry.Type.MOSIS.name());				// Poly-Cap
        polyCap_lay.setFactoryGDSLayer("28", Foundry.Type.TSMC.name());				// Poly-Cap
		silicideBlockLayer.setFactoryGDSLayer("29", Foundry.Type.MOSIS.name());			// Silicide-Block
        silicideBlockLayer.setFactoryGDSLayer("34", Foundry.Type.TSMC.name());			// Silicide-Block
        thickActive_lay.setFactoryGDSLayer("60", Foundry.Type.MOSIS.name());			// Thick-Active
        thickActive_lay.setFactoryGDSLayer("4", Foundry.Type.TSMC.name());			// Thick-Active
		pseudoMetal1_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());			// Pseudo-Metal-1
		pseudoMetal2_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());			// Pseudo-Metal-2
		pseudoMetal3_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());			// Pseudo-Metal-3
		pseudoMetal4_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());			// Pseudo-Metal-4
		pseudoMetal5_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());			// Pseudo-Metal-5
		pseudoMetal6_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());			// Pseudo-Metal-6
		pseudoPoly1_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());				// Pseudo-Polysilicon-1
		pseudoPoly2_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());				// Pseudo-Polysilicon-2
		pseudoPActive_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());			// Pseudo-P-Active
		pseudoNActive_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());			// Pseudo-N-Active
		pseudoPSelect_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());			// Pseudo-P-Select
		pseudoNSelect_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());			// Pseudo-N-Select
		pseudoPWell_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());				// Pseudo-P-Well
		pseudoNWell_lay.setFactoryGDSLayer("", Foundry.Type.MOSIS.name());				// Pseudo-N-Well
		padFrame_lay.setFactoryGDSLayer("26", Foundry.Type.MOSIS.name());				// Pad-Frame
        padFrame_lay.setFactoryGDSLayer("26", Foundry.Type.TSMC.name());				// Pad-Frame
		// The Skill names
		metalLayers[0].setFactorySkillLayer("metal1");			// Metal-1
		metalLayers[1].setFactorySkillLayer("metal2");			// Metal-2
		metalLayers[2].setFactorySkillLayer("metal3");			// Metal-3
		metalLayers[3].setFactorySkillLayer("metal4");			// Metal-4
		metalLayers[4].setFactorySkillLayer("metal5");			// Metal-5
		metalLayers[5].setFactorySkillLayer("metal6");			// Metal-6
		poly1Layer.setFactorySkillLayer("poly");				// Polysilicon-1
		poly2_lay.setFactorySkillLayer("");					// Polysilicon-2
		activeLayers[P_TYPE].setFactorySkillLayer("aa");				// P-Active
		activeLayers[N_TYPE].setFactorySkillLayer("aa");				// N-Active
		selectLayers[P_TYPE].setFactorySkillLayer("pplus");			// P-Select
		selectLayers[N_TYPE].setFactorySkillLayer("nplus");			// N-Select
		wellLayers[P_TYPE].setFactorySkillLayer("pwell");			// P-Well
		wellLayers[N_TYPE].setFactorySkillLayer("nwell");			// N-Well
		polyCutLayer.setFactorySkillLayer("pcont");			// Poly-Cut
		activeCutLayer.setFactorySkillLayer("acont");		// Active-Cut
		via1_lay.setFactorySkillLayer("via");				// Via-1
		via2_lay.setFactorySkillLayer("via2");				// Via-2
		via3_lay.setFactorySkillLayer("via3");				// Via-3
		via4_lay.setFactorySkillLayer("via4");				// Via-4
		via5_lay.setFactorySkillLayer("via5");				// Via-5
		passivation_lay.setFactorySkillLayer("glasscut");	// Passivation
		transistorPolyLayer.setFactorySkillLayer("poly");	// Transistor-Poly
		polyCap_lay.setFactorySkillLayer("");				// Poly-Cap
		pActiveWellLayer.setFactorySkillLayer("aa");			// P-Active-Well
		silicideBlockLayer.setFactorySkillLayer("");	    // Silicide-Block
        thickActive_lay.setFactorySkillLayer("");			// Thick-Active
		pseudoMetal1_lay.setFactorySkillLayer("");			// Pseudo-Metal-1
		pseudoMetal2_lay.setFactorySkillLayer("");			// Pseudo-Metal-2
		pseudoMetal3_lay.setFactorySkillLayer("");			// Pseudo-Metal-3
		pseudoMetal4_lay.setFactorySkillLayer("");			// Pseudo-Metal-4
		pseudoMetal5_lay.setFactorySkillLayer("");			// Pseudo-Metal-5
		pseudoMetal6_lay.setFactorySkillLayer("");			// Pseudo-Metal-6
		pseudoPoly1_lay.setFactorySkillLayer("");			// Pseudo-Polysilicon-1
		pseudoPoly2_lay.setFactorySkillLayer("");			// Pseudo-Polysilicon-2
		pseudoPActive_lay.setFactorySkillLayer("");			// Pseudo-P-Active
		pseudoNActive_lay.setFactorySkillLayer("");			// Pseudo-N-Active
		pseudoPSelect_lay.setFactorySkillLayer("pplus");	// Pseudo-P-Select
		pseudoNSelect_lay.setFactorySkillLayer("nplus");	// Pseudo-N-Select
		pseudoPWell_lay.setFactorySkillLayer("pwell");		// Pseudo-P-Well
		pseudoNWell_lay.setFactorySkillLayer("nwell");		// Pseudo-N-Well
		padFrame_lay.setFactorySkillLayer("");				// Pad-Frame

		// The layer distance
		// Data base on 18nm technology with 200nm as grid unit.
		double BULK_LAYER = 10;
		double DIFF_LAYER = 1; // dummy distance for now 0.2/0.2
		double ILD_LAYER = 3.5; // 0.7/0.2     convertLength()
		double IMD_LAYER = 5.65; // 1.13um/0.2
		double METAL_LAYER = 2.65; // 0.53um/0.2
		activeLayers[P_TYPE].setFactory3DInfo(0.85, BULK_LAYER + 2*DIFF_LAYER);				// P-Active 0.17um/0.2 =
		activeLayers[N_TYPE].setFactory3DInfo(0.8, BULK_LAYER + 2*DIFF_LAYER);				// N-Active 0.16um/0.2
		selectLayers[P_TYPE].setFactory3DInfo(DIFF_LAYER, BULK_LAYER + DIFF_LAYER);				// P-Select
		selectLayers[N_TYPE].setFactory3DInfo(DIFF_LAYER, BULK_LAYER + DIFF_LAYER);				// N-Select
		wellLayers[P_TYPE].setFactory3DInfo(DIFF_LAYER, BULK_LAYER);					// P-Well
		wellLayers[N_TYPE].setFactory3DInfo(DIFF_LAYER, BULK_LAYER);					// N-Well
        pActiveWellLayer.setFactory3DInfo(0.85, BULK_LAYER + 2*DIFF_LAYER);			// P-Active-Well
        thickActive_lay.setFactory3DInfo(0.5, BULK_LAYER + 0.5);			// Thick Active (between select and well)

		metalLayers[0].setFactory3DInfo(METAL_LAYER, ILD_LAYER + activeLayers[P_TYPE].getDepth());					// Metal-1   0.53um/0.2
		metalLayers[1].setFactory3DInfo(METAL_LAYER, IMD_LAYER + metalLayers[0].getDistance());					// Metal-2
		via1_lay.setFactory3DInfo(metalLayers[1].getDistance()-metalLayers[0].getDepth(), metalLayers[0].getDepth());					// Via-1

		metalLayers[2].setFactory3DInfo(METAL_LAYER, IMD_LAYER + metalLayers[1].getDistance());					// Metal-3
		via2_lay.setFactory3DInfo(metalLayers[2].getDistance()-metalLayers[1].getDepth(), metalLayers[1].getDepth());					// Via-2

		metalLayers[3].setFactory3DInfo(METAL_LAYER, IMD_LAYER + metalLayers[2].getDistance());					// Metal-4
        via3_lay.setFactory3DInfo(metalLayers[3].getDistance()-metalLayers[2].getDepth(), metalLayers[2].getDepth());					// Via-3

		metalLayers[4].setFactory3DInfo(METAL_LAYER, IMD_LAYER + metalLayers[3].getDistance());					// Metal-5
		via4_lay.setFactory3DInfo(metalLayers[4].getDistance()-metalLayers[3].getDepth(), metalLayers[3].getDepth());					// Via-4

		metalLayers[5].setFactory3DInfo(4.95, IMD_LAYER + metalLayers[4].getDistance());					// Metal-6 0.99um/0.2
        via5_lay.setFactory3DInfo(metalLayers[5].getDistance()-metalLayers[4].getDepth(), metalLayers[4].getDepth());					// Via-5

		double PASS_LAYER = 5; // 1um/0.2
		double PO_LAYER = 1; // 0.2/0.2
		double FOX_LAYER = 1.75; // 0.35/0.2
		double TOX_LAYER = 0; // Very narrow thin oxide in gate

		/* for displaying pins */
		pseudoMetal1_lay.setFactory3DInfo(0, metalLayers[0].getDistance());			// Pseudo-Metal-1
		pseudoMetal2_lay.setFactory3DInfo(0, metalLayers[1].getDistance());			// Pseudo-Metal-2
		pseudoMetal3_lay.setFactory3DInfo(0, metalLayers[2].getDistance());			// Pseudo-Metal-3
		pseudoMetal4_lay.setFactory3DInfo(0, metalLayers[3].getDistance());			// Pseudo-Metal-4
		pseudoMetal5_lay.setFactory3DInfo(0, metalLayers[4].getDistance());			// Pseudo-Metal-5
		pseudoMetal6_lay.setFactory3DInfo(0, metalLayers[5].getDistance());			// Pseudo-Metal-6

		// Poly layers
		poly1Layer.setFactory3DInfo(PO_LAYER, FOX_LAYER + activeLayers[P_TYPE].getDepth());					// Polysilicon-1
		transistorPolyLayer.setFactory3DInfo(PO_LAYER, TOX_LAYER + activeLayers[P_TYPE].getDepth());			// Transistor-Poly
        poly2_lay.setFactory3DInfo(PO_LAYER, transistorPolyLayer.getDepth());					// Polysilicon-2 // on top of transistor layer?
		polyCap_lay.setFactory3DInfo(PO_LAYER, FOX_LAYER + activeLayers[P_TYPE].getDepth());				// Poly-Cap @TODO GVG Ask polyCap

		polyCutLayer.setFactory3DInfo(metalLayers[0].getDistance()-poly1Layer.getDepth(), poly1Layer.getDepth());				// Poly-Cut between poly and metal1
		activeCutLayer.setFactory3DInfo(metalLayers[0].getDistance()-activeLayers[N_TYPE].getDepth(), activeLayers[N_TYPE].getDepth());				// Active-Cut betweent active and metal1

		// Other layers
		passivation_lay.setFactory3DInfo(PASS_LAYER, metalLayers[5].getDepth());			// Passivation
		silicideBlockLayer.setFactory3DInfo(0, BULK_LAYER);			// Silicide-Block
        padFrame_lay.setFactory3DInfo(0, passivation_lay.getDepth());				// Pad-Frame

		pseudoPoly1_lay.setFactory3DInfo(0, poly1Layer.getDistance());			// Pseudo-Polysilicon-1
		pseudoPoly2_lay.setFactory3DInfo(0, poly2_lay.getDistance());			// Pseudo-Polysilicon-2
		pseudoPActive_lay.setFactory3DInfo(0, activeLayers[P_TYPE].getDistance());			// Pseudo-P-Active
		pseudoNActive_lay.setFactory3DInfo(0, activeLayers[N_TYPE].getDistance());			// Pseudo-N-Active
		pseudoPSelect_lay.setFactory3DInfo(0, selectLayers[P_TYPE].getDistance());			// Pseudo-P-Select
		pseudoNSelect_lay.setFactory3DInfo(0, selectLayers[N_TYPE].getDistance());			// Pseudo-N-Select
		pseudoPWell_lay.setFactory3DInfo(0, wellLayers[P_TYPE].getDistance());				// Pseudo-P-Well
		pseudoNWell_lay.setFactory3DInfo(0, wellLayers[N_TYPE].getDistance());				// Pseudo-N-Well

		// The Spice parasitics
		metalLayers[0].setFactoryParasitics(0.078, 0.1209, 0.1104);			// Metal-1
		metalLayers[1].setFactoryParasitics(0.078, 0.0843, 0.0974);			// Metal-2
		metalLayers[2].setFactoryParasitics(0.078, 0.0843, 0.0974);			// Metal-3
		metalLayers[3].setFactoryParasitics(0.078, 0.0843, 0.0974);			// Metal-4
		metalLayers[4].setFactoryParasitics(0.078, 0.0843, 0.0974);			// Metal-5
		metalLayers[5].setFactoryParasitics(0.036, 0.0423, 0.1273);			// Metal-6
		poly1Layer.setFactoryParasitics(6.2, 0.1467, 0.0608);			// Polysilicon-1
		poly2_lay.setFactoryParasitics(50.0, 1.0, 0);			// Polysilicon-2
		activeLayers[P_TYPE].setFactoryParasitics(2.5, 0.9, 0);			// P-Active
		activeLayers[N_TYPE].setFactoryParasitics(3.0, 0.9, 0);			// N-Active
		selectLayers[P_TYPE].setFactoryParasitics(0, 0, 0);				// P-Select
		selectLayers[N_TYPE].setFactoryParasitics(0, 0, 0);				// N-Select
		wellLayers[P_TYPE].setFactoryParasitics(0, 0, 0);				// P-Well
		wellLayers[N_TYPE].setFactoryParasitics(0, 0, 0);				// N-Well
		polyCutLayer.setFactoryParasitics(2.2, 0, 0);			// Poly-Cut
		activeCutLayer.setFactoryParasitics(2.5, 0, 0);			// Active-Cut
		via1_lay.setFactoryParasitics(1.0, 0, 0);				// Via-1
		via2_lay.setFactoryParasitics(0.9, 0, 0);				// Via-2
		via3_lay.setFactoryParasitics(0.8, 0, 0);				// Via-3
		via4_lay.setFactoryParasitics(0.8, 0, 0);				// Via-4
		via5_lay.setFactoryParasitics(0.8, 0, 0);				// Via-5
		passivation_lay.setFactoryParasitics(0, 0, 0);			// Passivation
		transistorPolyLayer.setFactoryParasitics(2.5, 0.09, 0);	// Transistor-Poly
		polyCap_lay.setFactoryParasitics(0, 0, 0);				// Poly-Cap
		pActiveWellLayer.setFactoryParasitics(0, 0, 0);			// P-Active-Well
		silicideBlockLayer.setFactoryParasitics(0, 0, 0);		// Silicide-Block
        thickActive_lay.setFactoryParasitics(0, 0, 0);			// Thick-Active
		pseudoMetal1_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-1
		pseudoMetal2_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-2
		pseudoMetal3_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-3
		pseudoMetal4_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-4
		pseudoMetal5_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-5
		pseudoMetal6_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Metal-6
		pseudoPoly1_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Polysilicon-1
		pseudoPoly2_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-Polysilicon-2
		pseudoPActive_lay.setFactoryParasitics(0, 0, 0);		// Pseudo-P-Active
		pseudoNActive_lay.setFactoryParasitics(0, 0, 0);		// Pseudo-N-Active
		pseudoPSelect_lay.setFactoryParasitics(0, 0, 0);		// Pseudo-P-Select
		pseudoNSelect_lay.setFactoryParasitics(0, 0, 0);		// Pseudo-N-Select
		pseudoPWell_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-P-Well
		pseudoNWell_lay.setFactoryParasitics(0, 0, 0);			// Pseudo-N-Well
		padFrame_lay.setFactoryParasitics(0, 0, 0);				// Pad-Frame

		setFactoryParasitics(4, 0.1);

		String [] headerLevel1 =
		{
			"*CMOS/BULK-NWELL (PRELIMINARY PARAMETERS)",
			".OPTIONS NOMOD DEFL=3UM DEFW=3UM DEFAD=70P DEFAS=70P LIMPTS=1000",
			"+ITL5=0 RELTOL=0.01 ABSTOL=500PA VNTOL=500UV LVLTIM=2",
			"+LVLCOD=1",
			".MODEL N NMOS LEVEL=1",
			"+KP=60E-6 VTO=0.7 GAMMA=0.3 LAMBDA=0.05 PHI=0.6",
			"+LD=0.4E-6 TOX=40E-9 CGSO=2.0E-10 CGDO=2.0E-10 CJ=.2MF/M^2",
			".MODEL P PMOS LEVEL=1",
			"+KP=20E-6 VTO=0.7 GAMMA=0.4 LAMBDA=0.05 PHI=0.6",
			"+LD=0.6E-6 TOX=40E-9 CGSO=3.0E-10 CGDO=3.0E-10 CJ=.2MF/M^2",
			".MODEL DIFFCAP D CJO=.2MF/M^2"
		};
		setSpiceHeaderLevel1(headerLevel1);
		String [] headerLevel2 =
		{
			"* MOSIS 3u CMOS PARAMS",
			".OPTIONS NOMOD DEFL=2UM DEFW=6UM DEFAD=100P DEFAS=100P",
			"+LIMPTS=1000 ITL5=0 ABSTOL=500PA VNTOL=500UV",
			"* Note that ITL5=0 sets ITL5 to infinity",
			".MODEL N NMOS LEVEL=2 LD=0.3943U TOX=502E-10",
			"+NSUB=1.22416E+16 VTO=0.756 KP=4.224E-05 GAMMA=0.9241",
			"+PHI=0.6 UO=623.661 UEXP=8.328627E-02 UCRIT=54015.0",
			"+DELTA=5.218409E-03 VMAX=50072.2 XJ=0.4U LAMBDA=2.975321E-02",
			"+NFS=4.909947E+12 NEFF=1.001E-02 NSS=0.0 TPG=1.0",
			"+RSH=20.37 CGDO=3.1E-10 CGSO=3.1E-10",
			"+CJ=3.205E-04 MJ=0.4579 CJSW=4.62E-10 MJSW=0.2955 PB=0.7",
			".MODEL P PMOS LEVEL=2 LD=0.2875U TOX=502E-10",
			"+NSUB=1.715148E+15 VTO=-0.7045 KP=1.686E-05 GAMMA=0.3459",
			"+PHI=0.6 UO=248.933 UEXP=1.02652 UCRIT=182055.0",
			"+DELTA=1.0E-06 VMAX=100000.0 XJ=0.4U LAMBDA=1.25919E-02",
			"+NFS=1.0E+12 NEFF=1.001E-02 NSS=0.0 TPG=-1.0",
			"+RSH=79.10 CGDO=2.89E-10 CGSO=2.89E-10",
			"+CJ=1.319E-04 MJ=0.4125 CJSW=3.421E-10 MJSW=0.198 PB=0.66",
			".TEMP 25.0"
		};
		setSpiceHeaderLevel2(headerLevel2);

		//**************************************** ARCS ****************************************

		/** metal 1 arc */
		metalArcs[0] = ArcProto.newInstance(this, "Metal-1", 3.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metalLayers[0], 0, Poly.Type.FILLED)
		});
		metalArcs[0].setFunction(ArcProto.Function.METAL1);
		metalArcs[0].setFactoryFixedAngle(true);
		metalArcs[0].setWipable();
		metalArcs[0].setFactoryAngleIncrement(90);

		/** metal 2 arc */
		metalArcs[1] = ArcProto.newInstance(this, "Metal-2", 3.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metalLayers[1], 0, Poly.Type.FILLED)
		});
		metalArcs[1].setFunction(ArcProto.Function.METAL2);
		metalArcs[1].setFactoryFixedAngle(true);
		metalArcs[1].setWipable();
		metalArcs[1].setFactoryAngleIncrement(90);

		/** metal 3 arc */
		metalArcs[2] = ArcProto.newInstance(this, "Metal-3", 3.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metalLayers[2], 0, Poly.Type.FILLED)
		});
		metalArcs[2].setFunction(ArcProto.Function.METAL3);
		metalArcs[2].setFactoryFixedAngle(true);
		metalArcs[2].setWipable();
		metalArcs[2].setFactoryAngleIncrement(90);

		/** metal 4 arc */
		metalArcs[3] = ArcProto.newInstance(this, "Metal-4", 6.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metalLayers[3], 0, Poly.Type.FILLED)
		});
		metalArcs[3].setFunction(ArcProto.Function.METAL4);
		metalArcs[3].setFactoryFixedAngle(true);
		metalArcs[3].setWipable();
		metalArcs[3].setFactoryAngleIncrement(90);

		/** metal 5 arc */
		metalArcs[4] = ArcProto.newInstance(this, "Metal-5", 3.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metalLayers[4], 0, Poly.Type.FILLED)
		});
		metalArcs[4].setFunction(ArcProto.Function.METAL5);
		metalArcs[4].setFactoryFixedAngle(true);
		metalArcs[4].setWipable();
		metalArcs[4].setFactoryAngleIncrement(90);

		/** metal 6 arc */
		metalArcs[5] = ArcProto.newInstance(this, "Metal-6", 4.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metalLayers[5], 0, Poly.Type.FILLED)
		});
		metalArcs[5].setFunction(ArcProto.Function.METAL6);
		metalArcs[5].setFactoryFixedAngle(true);
		metalArcs[5].setWipable();
		metalArcs[5].setFactoryAngleIncrement(90);

		/** polysilicon 1 arc */
		poly1_arc = ArcProto.newInstance(this, "Polysilicon-1", 2.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(poly1Layer, 0, Poly.Type.FILLED)
		});
		poly1_arc.setFunction(ArcProto.Function.POLY1);
		poly1_arc.setFactoryFixedAngle(true);
		poly1_arc.setWipable();
		poly1_arc.setFactoryAngleIncrement(90);

		/** polysilicon 2 arc */
		poly2_arc = ArcProto.newInstance(this, "Polysilicon-2", 7.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(poly2_lay, 0, Poly.Type.FILLED)
		});
		poly2_arc.setFunction(ArcProto.Function.POLY2);
		poly2_arc.setFactoryFixedAngle(true);
		poly2_arc.setWipable();
		poly2_arc.setFactoryAngleIncrement(90);
		poly2_arc.setNotUsed();

		/** P-active arc */
		activeArcs[P_TYPE] = ArcProto.newInstance(this, "P-Active", 15.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(activeLayers[P_TYPE], 12, Poly.Type.FILLED),
			new Technology.ArcLayer(wellLayers[N_TYPE], 0, Poly.Type.FILLED),
			new Technology.ArcLayer(selectLayers[P_TYPE], 8, Poly.Type.FILLED)
		});
		activeArcs[P_TYPE].setFunction(ArcProto.Function.DIFFP);
		activeArcs[P_TYPE].setFactoryFixedAngle(true);
		activeArcs[P_TYPE].setWipable();
		activeArcs[P_TYPE].setFactoryAngleIncrement(90);
		activeArcs[P_TYPE].setWidthOffset(12.0);

		/** N-active arc */
		activeArcs[N_TYPE] = ArcProto.newInstance(this, "N-Active", 15.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(activeLayers[N_TYPE], 12, Poly.Type.FILLED),
			new Technology.ArcLayer(wellLayers[P_TYPE], 0, Poly.Type.FILLED),
			new Technology.ArcLayer(selectLayers[N_TYPE], 8, Poly.Type.FILLED)
		});
		activeArcs[N_TYPE].setFunction(ArcProto.Function.DIFFN);
		activeArcs[N_TYPE].setFactoryFixedAngle(true);
		activeArcs[N_TYPE].setWipable();
		activeArcs[N_TYPE].setFactoryAngleIncrement(90);
		activeArcs[N_TYPE].setWidthOffset(12.0);

		/** General active arc */
		active_arc = ArcProto.newInstance(this, "Active", 3.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(activeLayers[P_TYPE], 0, Poly.Type.FILLED),
			new Technology.ArcLayer(activeLayers[N_TYPE], 0, Poly.Type.FILLED)
		});
		active_arc.setFunction(ArcProto.Function.DIFF);
		active_arc.setFactoryFixedAngle(true);
		active_arc.setWipable();
		active_arc.setFactoryAngleIncrement(90);
		active_arc.setNotUsed();

		//**************************************** NODES ****************************************

		/** metal-1-pin */
		metalPinNodes[0] = PrimitiveNode.newInstance("Metal-1-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal1_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metalPinNodes[0].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalPinNodes[0], new ArcProto[] {metalArcs[0]}, "metal-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metalPinNodes[0].setFunction(PrimitiveNode.Function.PIN);
		metalPinNodes[0].setArcsWipe();
		metalPinNodes[0].setArcsShrink();

		/** metal-2-pin */
		metalPinNodes[1] = PrimitiveNode.newInstance("Metal-2-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal2_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metalPinNodes[1].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalPinNodes[1], new ArcProto[] {metalArcs[1]}, "metal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metalPinNodes[1].setFunction(PrimitiveNode.Function.PIN);
		metalPinNodes[1].setArcsWipe();
		metalPinNodes[1].setArcsShrink();

		/** metal-3-pin */
		metalPinNodes[2] = PrimitiveNode.newInstance("Metal-3-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal3_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metalPinNodes[2].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalPinNodes[2], new ArcProto[] {metalArcs[2]}, "metal-3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metalPinNodes[2].setFunction(PrimitiveNode.Function.PIN);
		metalPinNodes[2].setArcsWipe();
		metalPinNodes[2].setArcsShrink();

		/** metal-4-pin */
		metalPinNodes[3] = PrimitiveNode.newInstance("Metal-4-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal4_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metalPinNodes[3].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalPinNodes[3], new ArcProto[] {metalArcs[3]}, "metal-4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metalPinNodes[3].setFunction(PrimitiveNode.Function.PIN);
		metalPinNodes[3].setArcsWipe();
		metalPinNodes[3].setArcsShrink();

		/** metal-5-pin */
		metalPinNodes[4] = PrimitiveNode.newInstance("Metal-5-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal5_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metalPinNodes[4].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalPinNodes[4], new ArcProto[] {metalArcs[4]}, "metal-5", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metalPinNodes[4].setFunction(PrimitiveNode.Function.PIN);
		metalPinNodes[4].setArcsWipe();
		metalPinNodes[4].setArcsShrink();
		metalPinNodes[4].setNotUsed();

		/** metal-6-pin */
		metalPinNodes[5] = PrimitiveNode.newInstance("Metal-6-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal6_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metalPinNodes[5].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalPinNodes[5], new ArcProto[] {metalArcs[5]}, "metal-6", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metalPinNodes[5].setFunction(PrimitiveNode.Function.PIN);
		metalPinNodes[5].setArcsWipe();
		metalPinNodes[5].setArcsShrink();
		metalPinNodes[5].setNotUsed();

		/** polysilicon-1-pin */
		poly1Pin_node = PrimitiveNode.newInstance("Polysilicon-1-Pin", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoPoly1_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		poly1Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, poly1Pin_node, new ArcProto[] {poly1_arc}, "polysilicon-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		poly1Pin_node.setFunction(PrimitiveNode.Function.PIN);
		poly1Pin_node.setArcsWipe();
		poly1Pin_node.setArcsShrink();

		/** polysilicon-2-pin */
		poly2Pin_node = PrimitiveNode.newInstance("Polysilicon-2-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoPoly2_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		poly2Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, poly2Pin_node, new ArcProto[] {poly2_arc}, "polysilicon-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		poly2Pin_node.setFunction(PrimitiveNode.Function.PIN);
		poly2Pin_node.setArcsWipe();
		poly2Pin_node.setArcsShrink();
		poly2Pin_node.setNotUsed();

		/** P-active-pin */
		activePinNodes[P_TYPE] = PrimitiveNode.newInstance("P-Active-Pin", this, 15.0, 15.0, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoPActive_lay,  0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6)),
				new Technology.NodeLayer(pseudoNWell_lay,  0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(pseudoPSelect_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4))
			});
		activePinNodes[P_TYPE].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, activePinNodes[P_TYPE], new ArcProto[] {activeArcs[P_TYPE]}, "p-active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromBottom(7.5), EdgeH.fromRight(7.5), EdgeV.fromTop(7.5))
			});
		activePinNodes[P_TYPE].setFunction(PrimitiveNode.Function.PIN);
		activePinNodes[P_TYPE].setArcsWipe();
		activePinNodes[P_TYPE].setArcsShrink();

		/** N-active-pin */
		activePinNodes[N_TYPE] = PrimitiveNode.newInstance("N-Active-Pin", this, 15.0, 15.0, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoNActive_lay,  0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6)),
				new Technology.NodeLayer(pseudoPWell_lay,  0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(pseudoNSelect_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4))
			});
		activePinNodes[N_TYPE].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, activePinNodes[N_TYPE], new ArcProto[] {activeArcs[N_TYPE]}, "n-active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromBottom(7.5), EdgeH.fromRight(7.5), EdgeV.fromTop(7.5))
			});
		activePinNodes[N_TYPE].setFunction(PrimitiveNode.Function.PIN);
		activePinNodes[N_TYPE].setArcsWipe();
		activePinNodes[N_TYPE].setArcsShrink();

		/** General active-pin */
		activePin_node = PrimitiveNode.newInstance("Active-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoPActive_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(pseudoNActive_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		activePin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, activePin_node, new ArcProto[] {active_arc, activeArcs[P_TYPE], activeArcs[N_TYPE]}, "active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		activePin_node.setFunction(PrimitiveNode.Function.PIN);
		activePin_node.setArcsWipe();
		activePin_node.setArcsShrink();
        activePin_node.setNotUsed();

		/** metal-1-P-active-contact */
		metalActiveContactNodes[P_TYPE] = PrimitiveNode.newInstance("Metal-1-P-Active-Con", this, 17.0, 17.0, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6.5)),
				new Technology.NodeLayer(activeLayers[P_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6)),
				new Technology.NodeLayer(wellLayers[N_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX,Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(selectLayers[P_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4)),
				new Technology.NodeLayer(activeCutLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(7.5))
			});
		metalActiveContactNodes[P_TYPE].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalActiveContactNodes[P_TYPE], new ArcProto[] {activeArcs[P_TYPE], metalArcs[0]}, "metal-1-p-act", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(8), EdgeV.fromBottom(8), EdgeH.fromRight(8), EdgeV.fromTop(8))
			});
		metalActiveContactNodes[P_TYPE].setFunction(PrimitiveNode.Function.CONTACT);
		metalActiveContactNodes[P_TYPE].setSpecialType(PrimitiveNode.MULTICUT);
		metalActiveContactNodes[P_TYPE].setSpecialValues(new double [] {2, 2, 1.5, 1.5, 3, 3});
		metalActiveContactNodes[P_TYPE].setMinSize(17, 17, "6.2, 7.3");

		/** metal-1-N-active-contact */
		metalActiveContactNodes[N_TYPE] = PrimitiveNode.newInstance("Metal-1-N-Active-Con", this, 17.0, 17.0, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6.5)),
				new Technology.NodeLayer(activeLayers[N_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6)),
				new Technology.NodeLayer(wellLayers[P_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(selectLayers[N_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4)),
				new Technology.NodeLayer(activeCutLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(7.5))
			});
		metalActiveContactNodes[N_TYPE].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalActiveContactNodes[N_TYPE], new ArcProto[] {activeArcs[N_TYPE], metalArcs[0]}, "metal-1-n-act", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(8), EdgeV.fromBottom(8), EdgeH.fromRight(8), EdgeV.fromTop(8))
			});
		metalActiveContactNodes[N_TYPE].setFunction(PrimitiveNode.Function.CONTACT);
		metalActiveContactNodes[N_TYPE].setSpecialType(PrimitiveNode.MULTICUT);
		metalActiveContactNodes[N_TYPE].setSpecialValues(new double [] {2, 2, 1.5, 1.5, 3, 3});
		metalActiveContactNodes[N_TYPE].setMinSize(17, 17, "6.2, 7.3");

		/** metal-1-polysilicon-1-contact */
		metal1Poly1Contact_node = PrimitiveNode.newInstance("Metal-1-Polysilicon-1-Con", this, 5.0, 5.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(0.5)),
				new Technology.NodeLayer(poly1Layer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(polyCutLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1.5))
			});
		metal1Poly1Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1Poly1Contact_node, new ArcProto[] {poly1_arc, metalArcs[0]}, "metal-1-polysilicon-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		metal1Poly1Contact_node.setFunction(PrimitiveNode.Function.CONTACT);
		metal1Poly1Contact_node.setSpecialType(PrimitiveNode.MULTICUT);
		metal1Poly1Contact_node.setSpecialValues(new double [] {2, 2, 1.5, 1.5, 3, 3});
		metal1Poly1Contact_node.setMinSize(5, 5, "5.2, 7.3");

		/** metal-1-polysilicon-2-contact */
		metal1Poly2Contact_node = PrimitiveNode.newInstance("Metal-1-Polysilicon-2-Con", this, 10.0, 10.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(3)),
				new Technology.NodeLayer(poly2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(polyCutLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4))
			});
		metal1Poly2Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1Poly2Contact_node, new ArcProto[] {poly2_arc, metalArcs[0]}, "metal-1-polysilicon-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(4.5), EdgeV.fromBottom(4.5), EdgeH.fromRight(4.5), EdgeV.fromTop(4.5))
			});
		metal1Poly2Contact_node.setFunction(PrimitiveNode.Function.CONTACT);
		metal1Poly2Contact_node.setSpecialType(PrimitiveNode.MULTICUT);
		metal1Poly2Contact_node.setSpecialValues(new double [] {2, 2, 4, 4, 3, 3});
		metal1Poly2Contact_node.setNotUsed();
		metal1Poly2Contact_node.setMinSize(10, 10, "?");

		/** metal-1-polysilicon-1-2-contact */
		metal1Poly12Contact_node = PrimitiveNode.newInstance("Metal-1-Polysilicon-1-2-Con", this, 15.0, 15.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(5.5)),
				new Technology.NodeLayer(poly1Layer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(5)),
				new Technology.NodeLayer(poly2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(polyCutLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6.5))
			});
		metal1Poly12Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1Poly12Contact_node, new ArcProto[] {poly1_arc, poly2_arc, metalArcs[0]}, "metal-1-polysilicon-1-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(7), EdgeV.fromBottom(7), EdgeH.fromRight(7), EdgeV.fromTop(7))
			});
		metal1Poly12Contact_node.setFunction(PrimitiveNode.Function.CONTACT);
		metal1Poly12Contact_node.setSpecialType(PrimitiveNode.MULTICUT);
		metal1Poly12Contact_node.setSpecialValues(new double [] {2, 2, 6.5, 6.5, 3, 3});
		metal1Poly12Contact_node.setNotUsed();
		metal1Poly12Contact_node.setMinSize(15, 15, "?");

		/** P-Transistor */
        /** N-Transistor */
        String[] stdNames = {"p", "n"};
        for (int i = 0; i < 2; i++)
        {
            transistorPolyLayers[i] = new Technology.NodeLayer(transistorPolyLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(10)),
                new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(10))}, 1, 1, 2, 2);
            transistorPolyLLayers[i] = new Technology.NodeLayer(poly1Layer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(10)),
                new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromTop(10))}, 1, 1, 2, 2);
            transistorPolyRLayers[i] = new Technology.NodeLayer(poly1Layer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromBottom(10)),
                new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(10))}, 1, 1, 2, 2);
            transistorPolyCLayers[i] = new Technology.NodeLayer(transistorPolyLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(10)),
                new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(10))}, 1, 1, 2, 2);
            transistorActiveLayers[i] = new Technology.NodeLayer(activeLayers[i], 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(7)),
                new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(7))}, 4, 4, 0, 0);
            transistorActiveTLayers[i] = new Technology.NodeLayer(activeLayers[i], 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromTop(10)),
                new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(7))}, 4, 4, 0, 0);
            transistorActiveBLayers[i] = new Technology.NodeLayer(activeLayers[i], 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(7)),
                new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromBottom(10))}, 4, 4, 0, 0);
            transistorWellLayers[i] = new Technology.NodeLayer(wellLayers[(i+1)%transistorNodes.length], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(1)),
                new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(1))}, 10, 10, 6, 6);
            transistorSelectLayers[i] = new Technology.NodeLayer(selectLayers[i], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(5)),
                new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(5))}, 6, 6, 2, 2);
            transistorNodes[i] = PrimitiveNode.newInstance(stdNames[i].toUpperCase()+"-Transistor", this, 15.0, 22.0, new SizeOffset(6, 6, 10, 10),
                new Technology.NodeLayer [] {transistorActiveLayers[i], transistorPolyLayers[i], transistorWellLayers[i], transistorSelectLayers[i]});
            transistorNodes[i].setElectricalLayers(new Technology.NodeLayer [] {transistorActiveTLayers[i], transistorActiveBLayers[i],
                transistorPolyCLayers[i], transistorPolyLLayers[i], transistorPolyRLayers[i], transistorWellLayers[i], transistorSelectLayers[i]});
            transistorNodes[i].addPrimitivePorts(new PrimitivePort []
                {
                    PrimitivePort.newInstance(this, transistorNodes[i], new ArcProto[] {poly1_arc}, stdNames[i]+"-trans-poly-left", 180,90, 0, PortCharacteristic.UNKNOWN,
                        EdgeH.fromLeft(4), EdgeV.fromBottom(11), EdgeH.fromLeft(4), EdgeV.fromTop(11)),
                    PrimitivePort.newInstance(this, transistorNodes[i], new ArcProto[] {activeArcs[i]}, stdNames[i]+"-trans-diff-top", 90,90, 1, PortCharacteristic.UNKNOWN,
                        EdgeH.fromLeft(7.5), EdgeV.fromTop(7.5), EdgeH.fromRight(7.5), EdgeV.fromTop(7)),
                    PrimitivePort.newInstance(this, transistorNodes[i], new ArcProto[] {poly1_arc}, stdNames[i]+"-trans-poly-right", 0,90, 0, PortCharacteristic.UNKNOWN,
                        EdgeH.fromRight(4), EdgeV.fromBottom(11), EdgeH.fromRight(4), EdgeV.fromTop(11)),
                    PrimitivePort.newInstance(this, transistorNodes[i], new ArcProto[] {activeArcs[i]}, stdNames[i]+"-trans-diff-bottom", 270,90, 2, PortCharacteristic.UNKNOWN,
                        EdgeH.fromLeft(7.5), EdgeV.fromBottom(7), EdgeH.fromRight(7.5), EdgeV.fromBottom(7.5))
                });
            transistorNodes[i].setFunction((i==P_TYPE) ? PrimitiveNode.Function.TRAPMOS : PrimitiveNode.Function.TRANMOS);
            transistorNodes[i].setHoldsOutline();
            transistorNodes[i].setCanShrink();
            transistorNodes[i].setSpecialType(PrimitiveNode.SERPTRANS);
            transistorNodes[i].setSpecialValues(new double [] {7, 1.5, 2.5, 2, 1, 2});
            transistorNodes[i].setMinSize(15, 22, "2.1, 3.1");
        }

		/** Thick oxide transistors */
		String[] thickNames = {"Thick-P", "Thick-N"};
		Technology.NodeLayer[] thickActiveLayers = new Technology.NodeLayer[] {transistorActiveLayers[P_TYPE], transistorActiveLayers[N_TYPE]};
		Technology.NodeLayer[] thickPolyLayers = new Technology.NodeLayer[] {transistorPolyLayers[P_TYPE], transistorPolyLayers[N_TYPE]};
		Technology.NodeLayer[] thickWellLayers = new Technology.NodeLayer[] {transistorWellLayers[P_TYPE], transistorWellLayers[N_TYPE]};
		Technology.NodeLayer[] thickSelectLayers = new Technology.NodeLayer[] {transistorSelectLayers[P_TYPE], transistorSelectLayers[N_TYPE]};
		Technology.NodeLayer[] thickActiveTLayers = new Technology.NodeLayer[] {transistorActiveTLayers[P_TYPE], transistorActiveTLayers[N_TYPE]};
		Technology.NodeLayer[] thickActiveBLayers = new Technology.NodeLayer[] {transistorActiveBLayers[P_TYPE], transistorActiveBLayers[N_TYPE]};
        Technology.NodeLayer[] thickPolyCLayers = new Technology.NodeLayer[] {transistorPolyCLayers[P_TYPE], transistorPolyCLayers[N_TYPE]};
		Technology.NodeLayer[] thickPolyLLayers = new Technology.NodeLayer[] {transistorPolyLLayers[P_TYPE], transistorPolyLLayers[N_TYPE]};
		Technology.NodeLayer[] thickPolyRLayers = new Technology.NodeLayer[] {transistorPolyRLayers[P_TYPE], transistorPolyRLayers[N_TYPE]};
		Technology.NodeLayer[] thickLayers = new Technology.NodeLayer[2];

		for (int i = 0; i < thickLayers.length; i++)
		{
			thickLayers[i] = new Technology.NodeLayer(thickActive_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(1))}, 10, 10, 6, 6);
		}

		for (int i = 0; i < thickTransistorNodes.length; i++)
		{
			thickTransistorNodes[i] = PrimitiveNode.newInstance(thickNames[i] + "-Transistor", this, 15.0, 22.0, new SizeOffset(6, 6, 10, 10),
				new Technology.NodeLayer [] {thickActiveLayers[i], thickPolyLayers[i], thickWellLayers[i], thickSelectLayers[i], thickLayers[i]});
			thickTransistorNodes[i].setElectricalLayers(new Technology.NodeLayer [] {thickActiveTLayers[i], thickActiveBLayers[i],
				thickPolyCLayers[i], thickPolyLLayers[i], thickPolyRLayers[i], thickWellLayers[i], thickSelectLayers[i], thickLayers[i]});
			thickTransistorNodes[i].addPrimitivePorts(new PrimitivePort []
				{
					PrimitivePort.newInstance(this, thickTransistorNodes[i], new ArcProto[] {poly1_arc}, "poly-left", 180,90, 0, PortCharacteristic.UNKNOWN,
						EdgeH.fromLeft(4), EdgeV.fromBottom(11), EdgeH.fromLeft(4), EdgeV.fromTop(11)),
					PrimitivePort.newInstance(this, thickTransistorNodes[i], new ArcProto[] {activeArcs[i]}, "diff-top", 90,90, 1, PortCharacteristic.UNKNOWN,
						EdgeH.fromLeft(7.5), EdgeV.fromTop(7.5), EdgeH.fromRight(7.5), EdgeV.fromTop(7)),
					PrimitivePort.newInstance(this, thickTransistorNodes[i], new ArcProto[] {poly1_arc}, "poly-right", 0,90, 0, PortCharacteristic.UNKNOWN,
						EdgeH.fromRight(4), EdgeV.fromBottom(11), EdgeH.fromRight(4), EdgeV.fromTop(11)),
					PrimitivePort.newInstance(this, thickTransistorNodes[i], new ArcProto[] {activeArcs[i]}, "diff-bottom", 270,90, 2, PortCharacteristic.UNKNOWN,
						EdgeH.fromLeft(7.5), EdgeV.fromBottom(7), EdgeH.fromRight(7.5), EdgeV.fromBottom(7.5))
				});
			thickTransistorNodes[i].setFunction((i==P_TYPE) ? PrimitiveNode.Function.TRAPMOS : PrimitiveNode.Function.TRANMOS);
			thickTransistorNodes[i].setHoldsOutline();
			thickTransistorNodes[i].setCanShrink();
			thickTransistorNodes[i].setSpecialType(PrimitiveNode.SERPTRANS);
			thickTransistorNodes[i].setSpecialValues(new double [] {7, 1.5, 2.5, 2, 1, 2});
			thickTransistorNodes[i].setMinSize(15, 22, "2.1, 3.1");
			thickTransistorNodes[i].setSkipSizeInPalette(); // For display purposes
            thickTransistorNodes[i].setNodeBit(PrimitiveNode.OD18BIT);
		}

		/** Scalable-P-Transistor */
		scalableTransistorNodes[P_TYPE] = PrimitiveNode.newInstance("P-Transistor-Scalable", this, 17.0, 26.0, new SizeOffset(7, 7, 12, 12),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(activeLayers[P_TYPE], 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromTop(6)),
					new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(11))}),
				new Technology.NodeLayer(metalLayers[0], 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromTop(6.5)),
					new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromTop(10.5))}),
				new Technology.NodeLayer(activeLayers[P_TYPE], 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(11)),
					new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromBottom(6))}),
				new Technology.NodeLayer(metalLayers[0], 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromBottom(10.5)),
					new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromBottom(6.5))}),
				new Technology.NodeLayer(activeLayers[P_TYPE], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7), EdgeV.fromBottom(9)),
					new Technology.TechPoint(EdgeH.fromRight(7), EdgeV.fromTop(9))}),
				new Technology.NodeLayer(transistorPolyLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(5), EdgeV.fromBottom(12)),
					new Technology.TechPoint(EdgeH.fromRight(5), EdgeV.fromTop(12))}),
				new Technology.NodeLayer(wellLayers[N_TYPE], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(selectLayers[P_TYPE], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4)),
				new Technology.NodeLayer(activeCutLayer, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromBottom(9.5)),
					new Technology.TechPoint(EdgeH.fromLeft(9.5), EdgeV.fromBottom(7.5))}),
				new Technology.NodeLayer(activeCutLayer, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromTop(7.5)),
					new Technology.TechPoint(EdgeH.fromLeft(9.5), EdgeV.fromTop(9.5))})
			});
		scalableTransistorNodes[P_TYPE].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, scalableTransistorNodes[P_TYPE], new ArcProto[] {poly1_arc}, "p-trans-sca-poly-left", 180,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromCenter(-3.5), EdgeV.makeCenter(), EdgeH.fromCenter(-3.5), EdgeV.makeCenter()),
				PrimitivePort.newInstance(this, scalableTransistorNodes[P_TYPE], new ArcProto[] {activeArcs[P_TYPE], metalArcs[0]}, "p-trans-sca-diff-top", 90,90, 1, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.fromCenter(4.5), EdgeH.makeCenter(), EdgeV.fromCenter(4.5)),
				PrimitivePort.newInstance(this, scalableTransistorNodes[P_TYPE], new ArcProto[] {poly1_arc}, "p-trans-sca-poly-right", 0,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromCenter(3.5), EdgeV.makeCenter(), EdgeH.fromCenter(3.5), EdgeV.makeCenter()),
				PrimitivePort.newInstance(this, scalableTransistorNodes[P_TYPE], new ArcProto[] {activeArcs[P_TYPE], metalArcs[0]}, "p-trans-sca-diff-bottom", 270,90, 2, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.fromCenter(-4.5), EdgeH.makeCenter(), EdgeV.fromCenter(-4.5))
			});
		scalableTransistorNodes[P_TYPE].setFunction(PrimitiveNode.Function.TRAPMOS);
		scalableTransistorNodes[P_TYPE].setCanShrink();
		scalableTransistorNodes[P_TYPE].setMinSize(17, 26, "2.1, 3.1");

		/** Scalable-N-Transistor */
		scalableTransistorNodes[N_TYPE] = PrimitiveNode.newInstance("N-Transistor-Scalable", this, 17.0, 26.0, new SizeOffset(7, 7, 12, 12),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(activeLayers[N_TYPE], 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromTop(6)),
					new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(11))}),
				new Technology.NodeLayer(metalLayers[0], 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromTop(6.5)),
					new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromTop(10.5))}),
				new Technology.NodeLayer(activeLayers[N_TYPE], 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(11)),
					new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromBottom(6))}),
				new Technology.NodeLayer(metalLayers[0], 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromBottom(10.5)),
					new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromBottom(6.5))}),
				new Technology.NodeLayer(activeLayers[N_TYPE], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7), EdgeV.fromBottom(9)),
					new Technology.TechPoint(EdgeH.fromRight(7), EdgeV.fromTop(9))}),
				new Technology.NodeLayer(transistorPolyLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(5), EdgeV.fromBottom(12)),
					new Technology.TechPoint(EdgeH.fromRight(5), EdgeV.fromTop(12))}),
				new Technology.NodeLayer(wellLayers[P_TYPE], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(selectLayers[N_TYPE], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4)),
				new Technology.NodeLayer(activeCutLayer, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromBottom(9.5)),
					new Technology.TechPoint(EdgeH.fromLeft(9.5), EdgeV.fromBottom(7.5))}),
				new Technology.NodeLayer(activeCutLayer, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromTop(7.5)),
					new Technology.TechPoint(EdgeH.fromLeft(9.5), EdgeV.fromTop(9.5))})
			});
		scalableTransistorNodes[N_TYPE].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, scalableTransistorNodes[N_TYPE], new ArcProto[] {poly1_arc}, "n-trans-sca-poly-left", 180,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromCenter(-3.5), EdgeV.makeCenter(), EdgeH.fromCenter(-3.5), EdgeV.makeCenter()),
				PrimitivePort.newInstance(this, scalableTransistorNodes[N_TYPE], new ArcProto[] {activeArcs[N_TYPE], metalArcs[0]}, "n-trans-sca-diff-top", 90,90, 1, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.fromCenter(4.5), EdgeH.makeCenter(), EdgeV.fromCenter(4.5)),
				PrimitivePort.newInstance(this, scalableTransistorNodes[N_TYPE], new ArcProto[] {poly1_arc}, "n-trans-sca-poly-right", 0,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromCenter(3.5), EdgeV.makeCenter(), EdgeH.fromCenter(3.5), EdgeV.makeCenter()),
				PrimitivePort.newInstance(this, scalableTransistorNodes[N_TYPE], new ArcProto[] {activeArcs[N_TYPE], metalArcs[0]}, "n-trans-sca-diff-bottom", 270,90, 2, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.fromCenter(-4.5), EdgeH.makeCenter(), EdgeV.fromCenter(-4.5))
			});
		scalableTransistorNodes[N_TYPE].setFunction(PrimitiveNode.Function.TRANMOS);
		scalableTransistorNodes[N_TYPE].setCanShrink();
		scalableTransistorNodes[N_TYPE].setMinSize(17, 26, "2.1, 3.1");

		/** metal-1-metal-2-contact */
		metalContactNodes[0] = PrimitiveNode.newInstance("Metal-1-Metal-2-Con", this, 5.0, 5.0, new SizeOffset(0.5, 0.5, 0.5, 0.5),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(0.5)),
				new Technology.NodeLayer(metalLayers[1], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(0.5)),
				new Technology.NodeLayer(via1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1.5))
			});
		metalContactNodes[0].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalContactNodes[0], new ArcProto[] {metalArcs[0], metalArcs[1]}, "metal-1-metal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metalContactNodes[0].setFunction(PrimitiveNode.Function.CONTACT);
		metalContactNodes[0].setSpecialType(PrimitiveNode.MULTICUT);
		metalContactNodes[0].setSpecialValues(new double [] {2, 2, 1, 1, 3, 3});
		metalContactNodes[0].setMinSize(5, 5, "8.3, 9.3");

		/** metal-2-metal-3-contact */
		metalContactNodes[1] = PrimitiveNode.newInstance("Metal-2-Metal-3-Con", this, 6.0, 6.0, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[1], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1)),
				new Technology.NodeLayer(metalLayers[2], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1)),
				new Technology.NodeLayer(via2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2))
			});
		metalContactNodes[1].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalContactNodes[1], new ArcProto[] {metalArcs[1], metalArcs[2]}, "metal-2-metal-3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5), EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))
			});
		metalContactNodes[1].setFunction(PrimitiveNode.Function.CONTACT);
		metalContactNodes[1].setSpecialType(PrimitiveNode.MULTICUT);
		metalContactNodes[1].setSpecialValues(new double [] {2, 2, 1, 1, 3, 3});
		metalContactNodes[1].setMinSize(6, 6, "14.3, 15.3");

		/** metal-3-metal-4-contact */
		metalContactNodes[2] = PrimitiveNode.newInstance("Metal-3-Metal-4-Con", this, 6.0, 6.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[2], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1)),
				new Technology.NodeLayer(metalLayers[3], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(via3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2))
			});
		metalContactNodes[2].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalContactNodes[2], new ArcProto[] {metalArcs[2], metalArcs[3]}, "metal-3-metal-4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5), EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))
			});
		metalContactNodes[2].setFunction(PrimitiveNode.Function.CONTACT);
		metalContactNodes[2].setSpecialType(PrimitiveNode.MULTICUT);
		metalContactNodes[2].setSpecialValues(new double [] {2, 2, 2, 2, 3, 3});
		metalContactNodes[2].setMinSize(6, 6, "21.3, 22.3");

		/** metal-4-metal-5-contact */
		metalContactNodes[3] = PrimitiveNode.newInstance("Metal-4-Metal-5-Con", this, 7.0, 7.0, new SizeOffset(1.5, 1.5, 1.5, 1.5),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[3], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1.5)),
				new Technology.NodeLayer(metalLayers[4], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1.5)),
				new Technology.NodeLayer(via4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2.5))
			});
		metalContactNodes[3].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalContactNodes[3], new ArcProto[] {metalArcs[3], metalArcs[4]}, "metal-4-metal-5", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5), EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))
			});
		metalContactNodes[3].setFunction(PrimitiveNode.Function.CONTACT);
		metalContactNodes[3].setSpecialType(PrimitiveNode.MULTICUT);
		metalContactNodes[3].setSpecialValues(new double [] {2, 2, 1, 1, 3, 3});
		metalContactNodes[3].setNotUsed();
		metalContactNodes[3].setMinSize(7, 7, "25.3, 26.3");

		/** metal-5-metal-6-contact */
		metalContactNodes[4] = PrimitiveNode.newInstance("Metal-5-Metal-6-Con", this, 8.0, 8.0, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[4], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1)),
				new Technology.NodeLayer(metalLayers[5], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1)),
				new Technology.NodeLayer(via5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(3))
			});
		metalContactNodes[4].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalContactNodes[4], new ArcProto[] {metalArcs[4], metalArcs[5]}, "metal-5-metal-6", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5), EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))
			});
		metalContactNodes[4].setFunction(PrimitiveNode.Function.CONTACT);
		metalContactNodes[4].setSpecialType(PrimitiveNode.MULTICUT);
		metalContactNodes[4].setSpecialValues(new double [] {3, 3, 2, 2, 4, 4});
		metalContactNodes[4].setNotUsed();
		metalContactNodes[4].setMinSize(8, 8, "29.3, 30.3");

        /**************************************************************************
         * Metal-1-P-Well Contact/Metal-1-N-Well Contact
        **************************************************************************/
        for (int i = 0; i < metalWellContactNodes.length; i++)
        {
            PrimitiveNode.Function func = (i==P_TYPE) ? PrimitiveNode.Function.WELL : PrimitiveNode.Function.SUBSTRATE;
            Layer active = (i==P_TYPE) ? pActiveWellLayer : activeLayers[N_TYPE];
            metalWellContactNodes[i] = PrimitiveNode.newInstance(metalLayers[0].getName()+"-"+wellLayers[i].getName()+"-Con", this, 17.0, 17.0, new SizeOffset(6, 6, 6, 6),
                new Technology.NodeLayer []
                {
                    new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6.5)),
                    new Technology.NodeLayer(active, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6)),
                    new Technology.NodeLayer(wellLayers[i], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
                    new Technology.NodeLayer(selectLayers[i], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4)),
                    new Technology.NodeLayer(activeCutLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(7.5))
                });
            metalWellContactNodes[i].addPrimitivePorts(new PrimitivePort []
                {
                    PrimitivePort.newInstance(this, metalWellContactNodes[i], new ArcProto[] {metalArcs[0], active_arc},
                            ((i==P_TYPE)?"metal-1-well":"metal-1-substrate"),
                            0,180, 0, PortCharacteristic.UNKNOWN,
                        EdgeH.fromLeft(8), EdgeV.fromBottom(8), EdgeH.fromRight(8), EdgeV.fromTop(8))
                });
            metalWellContactNodes[i].setFunction(func);
            metalWellContactNodes[i].setSpecialType(PrimitiveNode.MULTICUT);
            metalWellContactNodes[i].setSpecialValues(new double [] {2, 2, 1.5, 1.5, 3, 3});
            metalWellContactNodes[i].setMinSize(17, 17, "4.2, 6.2, 7.3");
        }

        /**************************************************************************
         * Metal Nodes
        **************************************************************************/
		/** Metal-1-Node */
		metalNodes[0] = PrimitiveNode.newInstance("Metal-1-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metalNodes[0].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalNodes[0], new ArcProto[] {metalArcs[0]}, "metal-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		metalNodes[0].setFunction(PrimitiveNode.Function.NODE);
		metalNodes[0].setHoldsOutline();
		metalNodes[0].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-2-Node */
		metalNodes[1] = PrimitiveNode.newInstance("Metal-2-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[1], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metalNodes[1].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalNodes[1], new ArcProto[] {metalArcs[1]}, "metal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		metalNodes[1].setFunction(PrimitiveNode.Function.NODE);
		metalNodes[1].setHoldsOutline();
		metalNodes[1].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-3-Node */
		metalNodes[2] = PrimitiveNode.newInstance("Metal-3-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[2], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metalNodes[2].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalNodes[2], new ArcProto[] {metalArcs[2]}, "metal-3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		metalNodes[2].setFunction(PrimitiveNode.Function.NODE);
		metalNodes[2].setHoldsOutline();
		metalNodes[2].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-4-Node */
		metalNodes[3] = PrimitiveNode.newInstance("Metal-4-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[3], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metalNodes[3].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalNodes[3], new ArcProto[] {metalArcs[3]}, "metal-4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		metalNodes[3].setFunction(PrimitiveNode.Function.NODE);
		metalNodes[3].setHoldsOutline();
		metalNodes[3].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-5-Node */
		metalNodes[4] = PrimitiveNode.newInstance("Metal-5-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[4], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metalNodes[4].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalNodes[4], new ArcProto[] {metalArcs[4]}, "metal-5", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		metalNodes[4].setFunction(PrimitiveNode.Function.NODE);
		metalNodes[4].setHoldsOutline();
		metalNodes[4].setSpecialType(PrimitiveNode.POLYGONAL);
		metalNodes[4].setNotUsed();

		/** Metal-6-Node */
		metalNodes[5] = PrimitiveNode.newInstance("Metal-6-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[5], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metalNodes[5].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalNodes[5], new ArcProto[] {metalArcs[5]}, "metal-6", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		metalNodes[5].setFunction(PrimitiveNode.Function.NODE);
		metalNodes[5].setHoldsOutline();
		metalNodes[5].setSpecialType(PrimitiveNode.POLYGONAL);
		metalNodes[5].setNotUsed();

		/** Polysilicon-1-Node */
		poly1Node_node = PrimitiveNode.newInstance("Polysilicon-1-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(poly1Layer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		poly1Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, poly1Node_node, new ArcProto[] {poly1_arc}, "polysilicon-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		poly1Node_node.setFunction(PrimitiveNode.Function.NODE);
		poly1Node_node.setHoldsOutline();
		poly1Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Polysilicon-2-Node */
		poly2Node_node = PrimitiveNode.newInstance("Polysilicon-2-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(poly2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		poly2Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, poly2Node_node, new ArcProto[] {poly2_arc}, "polysilicon-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		poly2Node_node.setFunction(PrimitiveNode.Function.NODE);
		poly2Node_node.setHoldsOutline();
		poly2Node_node.setSpecialType(PrimitiveNode.POLYGONAL);
		poly2Node_node.setNotUsed();

		/** P-Active-Node/N-Active-Node */
        for (int i = 0; i < activeNodes.length; i++)
        {
            activeNodes[i] = PrimitiveNode.newInstance(activeLayers[i].getName()+"-Node", this, 3.0, 3.0, null,
                new Technology.NodeLayer []
                {
                    new Technology.NodeLayer(activeLayers[i], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
                });
            activeNodes[i].addPrimitivePorts(new PrimitivePort []
                {
                    PrimitivePort.newInstance(this, activeNodes[i], new ArcProto[] {active_arc, activeArcs[0], activeArcs[1]}, "active", 0,180, 0, PortCharacteristic.UNKNOWN,
                        EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
                });
            activeNodes[i].setFunction(PrimitiveNode.Function.NODE);
            activeNodes[i].setHoldsOutline();
            activeNodes[i].setSpecialType(PrimitiveNode.POLYGONAL);
        }

//		/** N-Active-Node */
//		activeNodes[N_TYPE] = PrimitiveNode.newInstance("N-Active-Node", this, 3.0, 3.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(activeLayers[N_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		activeNodes[N_TYPE].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, activeNodes[N_TYPE], new ArcProto[] {active_arc, activeArcs[P_TYPE], activeArcs[N_TYPE]}, "active", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		activeNodes[N_TYPE].setFunction(PrimitiveNode.Function.NODE);
//		activeNodes[N_TYPE].setHoldsOutline();
//		activeNodes[N_TYPE].setSpecialType(PrimitiveNode.POLYGONAL);

		/** P-Select-Node/N-Select-Node */
        for (int i = 0; i < selectNodes.length; i++)
        {
            selectNodes[i] = PrimitiveNode.newInstance(selectLayers[i].getName()+"-Node", this, 4.0, 4.0, null,
                new Technology.NodeLayer []
                {
                    new Technology.NodeLayer(selectLayers[i], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
                });
            selectNodes[i].addPrimitivePorts(new PrimitivePort []
                {
                    PrimitivePort.newInstance(this, selectNodes[i], new ArcProto[0], "select", 0,180, 0, PortCharacteristic.UNKNOWN,
                        EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
                });
            selectNodes[i].setFunction(PrimitiveNode.Function.NODE);
            selectNodes[i].setHoldsOutline();
            selectNodes[i].setSpecialType(PrimitiveNode.POLYGONAL);
        }

//		/** N-Select-Node */
//		selectNodes[N_TYPE] = PrimitiveNode.newInstance("N-Select-Node", this, 4.0, 4.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(selectLayers[N_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		selectNodes[N_TYPE].addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, selectNodes[N_TYPE], new ArcProto[0], "select", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
//			});
//		selectNodes[N_TYPE].setFunction(PrimitiveNode.Function.NODE);
//		selectNodes[N_TYPE].setHoldsOutline();
//		selectNodes[N_TYPE].setSpecialType(PrimitiveNode.POLYGONAL);

		/** PolyCut-Node */
		polyCutNode_node = PrimitiveNode.newInstance("Poly-Cut-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(polyCutLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		polyCutNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, polyCutNode_node, new ArcProto[0], "polycut", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		polyCutNode_node.setFunction(PrimitiveNode.Function.NODE);
		polyCutNode_node.setHoldsOutline();
		polyCutNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** ActiveCut-Node */
		activeCutNode_node = PrimitiveNode.newInstance("Active-Cut-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(activeCutLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		activeCutNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, activeCutNode_node, new ArcProto[0], "activecut", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		activeCutNode_node.setFunction(PrimitiveNode.Function.NODE);
		activeCutNode_node.setHoldsOutline();
		activeCutNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-1-Node */
		viaNodes[0] = PrimitiveNode.newInstance("Via-1-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		viaNodes[0].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, viaNodes[0], new ArcProto[0], "via-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		viaNodes[0].setFunction(PrimitiveNode.Function.NODE);
		viaNodes[0].setHoldsOutline();
		viaNodes[0].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-2-Node */
		viaNodes[1] = PrimitiveNode.newInstance("Via-2-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		viaNodes[1].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, viaNodes[1], new ArcProto[0], "via-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		viaNodes[1].setFunction(PrimitiveNode.Function.NODE);
		viaNodes[1].setHoldsOutline();
		viaNodes[1].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-3-Node */
		viaNodes[2] = PrimitiveNode.newInstance("Via-3-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		viaNodes[2].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, viaNodes[2], new ArcProto[0], "via-3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		viaNodes[2].setFunction(PrimitiveNode.Function.NODE);
		viaNodes[2].setHoldsOutline();
		viaNodes[2].setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-4-Node */
		viaNodes[3] = PrimitiveNode.newInstance("Via-4-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		viaNodes[3].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, viaNodes[3], new ArcProto[0], "via-4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		viaNodes[3].setFunction(PrimitiveNode.Function.NODE);
		viaNodes[3].setHoldsOutline();
		viaNodes[3].setSpecialType(PrimitiveNode.POLYGONAL);
		viaNodes[3].setNotUsed();

		/** Via-5-Node */
		viaNodes[4] = PrimitiveNode.newInstance("Via-5-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		viaNodes[4].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, viaNodes[4], new ArcProto[0], "via-5", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		viaNodes[4].setFunction(PrimitiveNode.Function.NODE);
		viaNodes[4].setHoldsOutline();
		viaNodes[4].setSpecialType(PrimitiveNode.POLYGONAL);
		viaNodes[4].setNotUsed();

		/** P-Well-Node */
		pWellNode_node = PrimitiveNode.newInstance("P-Well-Node", this, 12.0, 12.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(wellLayers[P_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		pWellNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pWellNode_node, new ArcProto[] {activeArcs[P_TYPE]}, "well", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pWellNode_node.setFunction(PrimitiveNode.Function.NODE);
		pWellNode_node.setHoldsOutline();
		pWellNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** N-Well-Node */
		nWellNode_node = PrimitiveNode.newInstance("N-Well-Node", this, 12.0, 12.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(wellLayers[N_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		nWellNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, nWellNode_node, new ArcProto[] {activeArcs[P_TYPE]}, "well", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		nWellNode_node.setFunction(PrimitiveNode.Function.NODE);
		nWellNode_node.setHoldsOutline();
		nWellNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Passivation-Node */
		passivationNode_node = PrimitiveNode.newInstance("Passivation-Node", this, 8.0, 8.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(passivation_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		passivationNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, passivationNode_node, new ArcProto[0], "passivation", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		passivationNode_node.setFunction(PrimitiveNode.Function.NODE);
		passivationNode_node.setHoldsOutline();
		passivationNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Pad-Frame-Node */
		padFrameNode_node = PrimitiveNode.newInstance("Pad-Frame-Node", this, 8.0, 8.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(padFrame_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		padFrameNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, padFrameNode_node, new ArcProto[0], "pad-frame", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		padFrameNode_node.setFunction(PrimitiveNode.Function.NODE);
		padFrameNode_node.setHoldsOutline();
		padFrameNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Poly-Cap-Node */
		polyCapNode_node = PrimitiveNode.newInstance("Poly-Cap-Node", this, 8.0, 8.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(polyCap_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		polyCapNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, polyCapNode_node, new ArcProto[0], "poly-cap", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		polyCapNode_node.setFunction(PrimitiveNode.Function.NODE);
		polyCapNode_node.setHoldsOutline();
		polyCapNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** P-Active-Well-Node */
		pActiveWellNode_node = PrimitiveNode.newInstance("P-Active-Well-Node", this, 8.0, 8.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pActiveWellLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		pActiveWellNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pActiveWellNode_node, new ArcProto[0], "p-active-well", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pActiveWellNode_node.setFunction(PrimitiveNode.Function.NODE);
		pActiveWellNode_node.setHoldsOutline();
		pActiveWellNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Polysilicon-1-Transistor-Node */
		polyTransistorNode_node = PrimitiveNode.newInstance("Transistor-Poly-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(transistorPolyLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		polyTransistorNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, polyTransistorNode_node, new ArcProto[] {poly1_arc}, "trans-poly-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		polyTransistorNode_node.setFunction(PrimitiveNode.Function.NODE);
		polyTransistorNode_node.setHoldsOutline();
		polyTransistorNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Silicide-Block-Node */
		silicideBlockNode_node = PrimitiveNode.newInstance("Silicide-Block-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(silicideBlockLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		silicideBlockNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, silicideBlockNode_node, new ArcProto[] {poly1_arc}, "silicide-block", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		silicideBlockNode_node.setFunction(PrimitiveNode.Function.NODE);
		silicideBlockNode_node.setHoldsOutline();
		silicideBlockNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Thick-Active-Node */
		thickActiveNode_node = PrimitiveNode.newInstance("Thick-Active-Node", this, 4.0, 4.0, null, // 4.0 is given by rule 24.1
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(thickActive_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		thickActiveNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, thickActiveNode_node, new ArcProto[] {poly1_arc}, "thick-active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		thickActiveNode_node.setFunction(PrimitiveNode.Function.NODE);
		thickActiveNode_node.setHoldsOutline();
		thickActiveNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		// The pure layer nodes
		metalLayers[0].setPureLayerNode(metalNodes[0]);					// Metal-1
		metalLayers[1].setPureLayerNode(metalNodes[1]);					// Metal-2
		metalLayers[2].setPureLayerNode(metalNodes[2]);					// Metal-3
		metalLayers[3].setPureLayerNode(metalNodes[3]);					// Metal-4
		metalLayers[4].setPureLayerNode(metalNodes[4]);					// Metal-5
		metalLayers[5].setPureLayerNode(metalNodes[5]);					// Metal-6
		poly1Layer.setPureLayerNode(poly1Node_node);						// Polysilicon-1
		poly2_lay.setPureLayerNode(poly2Node_node);						// Polysilicon-2
		activeLayers[P_TYPE].setPureLayerNode(activeNodes[P_TYPE]);					// P-Active
		activeLayers[N_TYPE].setPureLayerNode(activeNodes[N_TYPE]);					// N-Active
		selectLayers[P_TYPE].setPureLayerNode(selectNodes[P_TYPE]);					// P-Select
		selectLayers[N_TYPE].setPureLayerNode(selectNodes[N_TYPE]);					// N-Select
		wellLayers[P_TYPE].setPureLayerNode(pWellNode_node);						// P-Well
		wellLayers[N_TYPE].setPureLayerNode(nWellNode_node);						// N-Well
		polyCutLayer.setPureLayerNode(polyCutNode_node);					// Poly-Cut
		activeCutLayer.setPureLayerNode(activeCutNode_node);				// Active-Cut
		via1_lay.setPureLayerNode(viaNodes[0]);						// Via-1
		via2_lay.setPureLayerNode(viaNodes[1]);						// Via-2
		via3_lay.setPureLayerNode(viaNodes[2]);						// Via-3
		via4_lay.setPureLayerNode(viaNodes[3]);						// Via-4
		via5_lay.setPureLayerNode(viaNodes[4]);						// Via-5
		passivation_lay.setPureLayerNode(passivationNode_node);			// Passivation
		transistorPolyLayer.setPureLayerNode(polyTransistorNode_node);	// Transistor-Poly
		polyCap_lay.setPureLayerNode(polyCapNode_node);					// Poly-Cap
		pActiveWellLayer.setPureLayerNode(pActiveWellNode_node);			// P-Active-Well
		silicideBlockLayer.setPureLayerNode(silicideBlockNode_node);		// Silicide-Block
		thickActive_lay.setPureLayerNode(thickActiveNode_node);			// Thick-Active
		padFrame_lay.setPureLayerNode(padFrameNode_node);				// Pad-Frame

        // Information for palette
        int maxY = metalArcs.length + activeArcs.length + 1 /* poly*/ + 1 /* trans */ + 1 /*misc*/ + 1 /* well */;
        nodeGroups = new Object[maxY][3];
        int count = 0;
        List<NodeInst> tmp;

        // Transistor nodes first
        for (int i = 0; i < transistorNodes.length; i++)
        {
            tmp = new ArrayList<NodeInst>(2);
            String tmpVar = stdNames[i]+"Mos";
            tmp.add(makeNodeInst(transistorNodes[i], transistorNodes[i].getFunction(), 0, true, tmpVar, 9));
            tmp.add(makeNodeInst(thickTransistorNodes[i], thickTransistorNodes[i].getFunction(), 0, true, tmpVar, 9));
            tmp.add(makeNodeInst(scalableTransistorNodes[i], scalableTransistorNodes[i].getFunction(), 0, true, tmpVar, 9));
            nodeGroups[count][i+1] = tmp;
        }

        // Well second
        count++;
        for (int i = 0; i < metalWellContactNodes.length; i++)
        {
            String tmpVar = stdNames[i]+"Well";
            nodeGroups[count][i+1] = makeNodeInst(metalWellContactNodes[i], metalWellContactNodes[i].getFunction(),
                    0, true, tmpVar, 5.5);
        }

//        // RPO resistors
//        for (int i = 0; i < polyResistorNodes.length; i++)
//        {
//            String tmpVar = shortNames[i]+"R";
//            tmp = new ArrayList<NodeInst>(1);
//            tmp.add(makeNodeInst(polyResistorNodes[i], polyResistorNodes[i].getFunction(), 0, true, tmpVar, 10));
//            tmp.add(makeNodeInst(wellResistorNodes[i], wellResistorNodes[i].getFunction(), 0, true, tmpVar, 10));
//            nodeGroups[i][0] = tmp;
//        }

        // Active/Well first
        for (int i = 0; i < activeArcs.length; i++)
        {
            nodeGroups[++count][0] = activeArcs[i];
            nodeGroups[count][1] = activePinNodes[i];
            String tmpVar = stdNames[i]+"Act";
            nodeGroups[count][2] = makeNodeInst(metalActiveContactNodes[i], metalActiveContactNodes[i].getFunction(),
                    0, true, tmpVar, 5.55);
        }

        // Poly-related node insts
        nodeGroups[++count][0] = poly1_arc;
        nodeGroups[count][1] = poly1Pin_node;
        nodeGroups[count][2] = metal1Poly1Contact_node;

        // MXMY contacts
        for (int i = 0; i < metalArcs.length; i++)
        {
            nodeGroups[++count][0] = metalArcs[i];
            nodeGroups[count][1] = metalPinNodes[i];
            nodeGroups[count][2] = (i < metalArcs.length - 1) ? metalContactNodes[i] : null;
        }

        // On the side
        nodeGroups[++count][0] = "Pure";
        nodeGroups[count][1] = "Misc.";
        nodeGroups[count][2] = "Cell";
	}

    /**
     * This method resizes nodes according to selected foundry. It must be protected
     * so it could be overwriten by subclasses
     */
    protected void resizeNodes()
    {
        Technology.NodeLayer node = metal1Poly1Contact_node.getLayers()[2]; // Cut
        node.setPoints(Technology.TechPoint.makeIndented(1.5));
        metal1Poly1Contact_node.setSpecialValues(new double [] {2, 2, 1.5, 1.5, 3, 3});

        // Active contacts
        for (int i = 0; i < metalActiveContactNodes.length; i++)
        {
            node = metalActiveContactNodes[i].getLayers()[4]; // Cut
            node.setPoints(Technology.TechPoint.makeIndented(7.5));
//                node = metalActiveContactNodes[i].getLayers()[3]; // Select
//                node.setPoints(Technology.TechPoint.makeIndented(4)); // back to Mosis default=4
            metalActiveContactNodes[i].setSpecialValues(new double [] {2, 2, 1.5, 1.5, 3, 3});
        }

        // Well contacts
        for (int i = 0; i < metalWellContactNodes.length; i++)
        {
            node = metalWellContactNodes[i].getLayers()[4]; // Cut
            node.setPoints(Technology.TechPoint.makeIndented(7.5));
            metalWellContactNodes[i].setSpecialValues(new double [] {2, 2, 1.5, 1.5, 3, 3});
//                node = metalWellContactNodes[i].getLayers()[2]; // Well
//                node.setPoints(Technology.TechPoint.makeIndented(3));
        }

        // Via1 -> Via4. Some values depend on original node size
        double [] indentValues = {1.5, 2, 2, 2.5, 3};
        double [] cutValues = {1, 1, 2, 1, 2};
        double [] sizeValues = {2, 2, 2, 2, 3};
        double [] spaceValues = {3, 3, 3, 3, 4};
        for (int i = 0; i < 4; i++)
        {
            node = metalContactNodes[i].getLayers()[2];
            node.setPoints(Technology.TechPoint.makeIndented(indentValues[i]));
            metalContactNodes[i].setSpecialValues(new double [] {sizeValues[i], sizeValues[i], cutValues[i], cutValues[i], spaceValues[i], spaceValues[i]});
        }

        // Transistors
        /* Poly -> 3.2 top/bottom extension */
        for (int i = 0; i < 2; i++)
        {
            transistorWellLayers[i].getLeftEdge().setAdder(0); transistorWellLayers[i].getRightEdge().setAdder(0);
            transistorSelectLayers[i].getLeftEdge().setAdder(4); transistorSelectLayers[i].getRightEdge().setAdder(-4);
            transistorPolyLayers[i].getLeftEdge().setAdder(4); transistorPolyLayers[i].getRightEdge().setAdder(-4);
            transistorPolyLLayers[i].getLeftEdge().setAdder(4);
            transistorPolyRLayers[i].getRightEdge().setAdder(-4);
            transistorPolyLayers[i].getBottomEdge().setAdder(10); transistorPolyLayers[i].getTopEdge().setAdder(-10);
            transistorPolyLLayers[i].getBottomEdge().setAdder(10); transistorPolyLLayers[i].getTopEdge().setAdder(-10);
            transistorPolyRLayers[i].getBottomEdge().setAdder(10); transistorPolyRLayers[i].getTopEdge().setAdder(-10);
            transistorPolyCLayers[i].getBottomEdge().setAdder(10); transistorPolyCLayers[i].getTopEdge().setAdder(-10);
            transistorSelectLayers[i].getBottomEdge().setAdder(5); transistorSelectLayers[i].getTopEdge().setAdder(-5);
            transistorActiveLayers[i].getBottomEdge().setAdder(7); transistorActiveLayers[i].getTopEdge().setAdder(-7);
            transistorActiveBLayers[i].getBottomEdge().setAdder(7); transistorActiveBLayers[i].getTopEdge().setAdder(10);
            transistorActiveTLayers[i].getTopEdge().setAdder(-7); transistorActiveTLayers[i].getBottomEdge().setAdder(-10);
            transistorNodes[i].setSizeOffset(new SizeOffset(6, 6, 10, 10));
        }
        //Scalable transistors
        for (int i = 0; i < 2; i++)
        {
            // polysilicon
            node = scalableTransistorNodes[i].getLayers()[5];
            node.getTopEdge().setAdder(-12); node.getBottomEdge().setAdder(12);
            node.getLeftEdge().setAdder(5); node.getRightEdge().setAdder(-5);
            // select
            node = scalableTransistorNodes[i].getLayers()[7];
            node.getLeftEdge().setAdder(4); node.getRightEdge().setAdder(-4);
            // first cut
            node = scalableTransistorNodes[i].getLayers()[8];
            node.getTopEdge().setAdder(7.5); node.getBottomEdge().setAdder(9.5);
            node.getLeftEdge().setAdder(7.5); node.getRightEdge().setAdder(9.5);
            // second cut
            node = scalableTransistorNodes[i].getLayers()[9];
            node.getTopEdge().setAdder(-9.5); node.getBottomEdge().setAdder(-7.5);
            node.getLeftEdge().setAdder(7.5); node.getRightEdge().setAdder(9.5);
            scalableTransistorNodes[i].setSizeOffset(new SizeOffset(7, 7, 12.1, 12.1));
        }
        // Channel length 2
        poly1_arc.setDefaultWidth(2.0);
        poly1Pin_node.setDefSize(2, 2);
        PrimitivePort polyPort = (PrimitivePort)poly1Pin_node.getPort(0);
        polyPort.getLeft().setAdder(1); polyPort.getBottom().setAdder(1);
        polyPort.getRight().setAdder(-1); polyPort.getTop().setAdder(-1);
        // Metal 6 arc width 4. Original value
        metalArcs[5].setDefaultWidth(4);
    }

	/******************** SUPPORT METHODS ********************/

    /**
	 * Method to set the technology to state "newstate", which encodes the number of metal
	 * layers, whether it is a deep process, and other rules.
	 */
	public void setState()
	{
		// set rules
        cachedRules = getFactoryDesignRules(getSelectedFoundry());

		// disable Metal-3/4/5/6-Pin, Metal-2/3/4/5-Metal-3/4/5/6-Con, Metal-3/4/5/6-Node, Via-2/3/4/5-Node
		metalPinNodes[2].setNotUsed();
		metalPinNodes[3].setNotUsed();
		metalPinNodes[4].setNotUsed();
		metalPinNodes[5].setNotUsed();
		metalContactNodes[1].setNotUsed();
		metalContactNodes[2].setNotUsed();
		metalContactNodes[3].setNotUsed();
		metalContactNodes[4].setNotUsed();
		metalNodes[2].setNotUsed();
		metalNodes[3].setNotUsed();
		metalNodes[4].setNotUsed();
		metalNodes[5].setNotUsed();
		viaNodes[1].setNotUsed();
		viaNodes[2].setNotUsed();
		viaNodes[3].setNotUsed();
		viaNodes[4].setNotUsed();

		// disable Polysilicon-2
		poly2_arc.setNotUsed();
		poly2Pin_node.setNotUsed();
		metal1Poly2Contact_node.setNotUsed();
		metal1Poly12Contact_node.setNotUsed();
		poly2Node_node.setNotUsed();

		// disable metal 3-6 arcs
		metalArcs[2].setNotUsed();
		metalArcs[3].setNotUsed();
		metalArcs[4].setNotUsed();
		metalArcs[5].setNotUsed();

		// enable the desired nodes
		switch (getNumMetal())
		{
			case 6:
				metalPinNodes[5].clearNotUsed();
				metalContactNodes[4].clearNotUsed();
				metalNodes[5].clearNotUsed();
				viaNodes[4].clearNotUsed();
				metalArcs[5].clearNotUsed();
				// FALLTHROUGH 
			case 5:
				metalPinNodes[4].clearNotUsed();
				metalContactNodes[3].clearNotUsed();
				metalNodes[4].clearNotUsed();
				viaNodes[3].clearNotUsed();
				metalArcs[4].clearNotUsed();
				// FALLTHROUGH 
			case 4:
				metalPinNodes[3].clearNotUsed();
				metalContactNodes[2].clearNotUsed();
				metalNodes[3].clearNotUsed();
				viaNodes[2].clearNotUsed();
				metalArcs[3].clearNotUsed();
				// FALLTHROUGH 
			case 3:
				metalPinNodes[2].clearNotUsed();
				metalContactNodes[1].clearNotUsed();
				metalNodes[2].clearNotUsed();
				viaNodes[1].clearNotUsed();
				metalArcs[2].clearNotUsed();
				break;
		}
		if (getRuleSet() != DEEPRULES)
		{
			if (isSecondPolysilicon())
			{
				// non-DEEP: enable Polysilicon-2
				poly2_arc.clearNotUsed();
				poly2Pin_node.clearNotUsed();
				metal1Poly2Contact_node.clearNotUsed();
				metal1Poly12Contact_node.clearNotUsed();
				poly2Node_node.clearNotUsed();
			}
		}

		// now rewrite the description
		setTechDesc(describeState());
	}

	/**
	 * Method to describe the technology when it is in state "state".
	 */
	private String describeState()
	{
		int numMetals = getNumMetal();
		String rules = "";
		switch (getRuleSet())
		{
			case SCMOSRULES: rules = "now standard";    break;
			case DEEPRULES:  rules = "now deep";        break;
			case SUBMRULES:  rules = "now submicron";   break;
		}
		int numPolys = 1;
		if (isSecondPolysilicon()) numPolys = 2;
		String description = "MOSIS CMOS (2-6 metals [now " + numMetals + "], 1-2 polys [now " +
			numPolys + "], flex rules [" + rules + "]";
		if (isDisallowStackedVias()) description += ", stacked vias disallowed";
		if (isAlternateActivePolyRules()) description += ", alternate contact rules";
		return description + ")";
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
	 * @param wnd the window in which this node will be drawn.
	 * @param context the VarContext to this node in the hierarchy.
	 * @param electrical true to get the "electrical" layers.
	 * This makes no sense for Schematics primitives.
	 * @param reasonable true to get only a minimal set of contact cuts in large contacts.
	 * This makes no sense for Schematics primitives.
	 * @param primLayers an array of NodeLayer objects to convert to Poly objects.
	 * @param layerOverride the layer to use for all generated polygons (if not null).
	 * @return an array of Poly objects.
	 */
	public Poly [] getShapeOfNode(NodeInst ni, EditWindow0 wnd, VarContext context, boolean electrical, boolean reasonable,
		Technology.NodeLayer [] primLayers, Layer layerOverride)
	{
		NodeProto prototype = ni.getProto();
		if (prototype == scalableTransistorNodes[P_TYPE] || prototype == scalableTransistorNodes[N_TYPE])
            return getShapeOfNodeScalable(ni, wnd, context, reasonable);

        // Default
        return super.getShapeOfNode(ni, wnd, context, electrical, reasonable, primLayers, layerOverride);
    }

    /**
     * Special getShapeOfNode function for scalable transistors
     * @param ni
     * @param wnd
     * @param context
     * @param reasonable
     * @return
     */
    private Poly [] getShapeOfNodeScalable(NodeInst ni, EditWindow0 wnd, VarContext context, boolean reasonable)
    {
		// determine special configurations (number of active contacts, inset of active contacts)
		int numContacts = 2;
		boolean insetContacts = false;
		Variable var = ni.getVar(TRANS_CONTACT, String.class);
		if (var != null)
		{
			String pt = (String)var.getObject();
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
		var = ni.getVar(Schematics.ATTR_WIDTH);
		if (var != null)
		{
			VarContext evalContext = context;
			if (evalContext == null) evalContext = VarContext.globalContext;
			String extra = var.describe(evalContext, ni);
			Object o = evalContext.evalVar(var, ni);
			if (o != null) extra = o.toString();
// 			try
// 			{
// 				Object o = evalContext.evalVarRecurse(var, ni);
// 				if (o != null) extra = o.toString();
// 			} catch (VarContext.EvalException e) {}
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
		double polyInset = actInset - cachedRules.getPolyOverhang();
		double actContInset = 7 + extraInset;

		// contacts must be 5 wide at a minimum
		if (activeWid < 5) actContInset -= (5-activeWid)/2;
		double metContInset = actContInset + 0.5;

		// determine the multicut information
		double [] specialValues = metalActiveContactNodes[P_TYPE].getSpecialValues();
		double cutSize = specialValues[0];
		double cutIndent = specialValues[2];   // or specialValues[3]
		double cutSep = specialValues[4];      // or specialValues[5]
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
		return super.getShapeOfNode(ni, wnd, context, false, reasonable, newNodeLayers, null);
	}

	/******************** PARAMETERIZABLE DESIGN RULES ********************/

	/**
	 * Method to build "factory" design rules, given the current technology settings.
	 * @return the "factory" design rules for this Technology.
	 * Returns null if there is an error loading the rules.
	 */
	public DRCRules getFactoryDesignRules(Foundry foundry)
	{
        List<DRCTemplate> theRules = foundry.getRules();

		MOSRules rules = new MOSRules(this);

        if (foundry == null) foundry = getSelectedFoundry();

        // Resize primitives according to the foundry
        resizeNodes();

		// load the DRC tables from the explanation table
		rules.wideLimit = new Double(WIDELIMIT);
        int numMetals = getNumMetals();

		for(int pass=0; pass<2; pass++)
		{
			for(int i=0; i < theRules.size(); i++)
			{
                DRCTemplate rule = theRules.get(i);

				// see if the rule applies
				if (pass == 0)
				{
					if (rule.ruleType == DRCTemplate.DRCRuleType.NODSIZ) continue;
				} else
				{
					if (rule.ruleType != DRCTemplate.DRCRuleType.NODSIZ) continue;
				}

				int when = rule.when;
                if (when != DRCTemplate.DRCMode.ALL.mode())
                {
                    // One of the 2 is present. Absence means rule is valid for both

                    if ((when&Foundry.Type.MOSIS.mode()) != 0 && foundry.getType() == Foundry.Type.TSMC)
                    {
                        System.out.println("SHould I see this case?");
                        continue;
                    }
                    else if ((when&Foundry.Type.TSMC.mode()) != 0 && foundry.getType() == Foundry.Type.MOSIS)
                    {
                        System.out.println("SHould I see this case?");
                        continue; // skipping this rule
                    }
                }

                boolean goodrule = true;
				if ((when&(DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode())) != 0)
				{
					switch (getRuleSet())
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

				// find the layer names
				Layer lay1 = null;
				int layert1 = -1;
				if (rule.name1 != null)
				{
					lay1 = findLayer(rule.name1);
					if (lay1 == null)
					{
						System.out.println("Warning: no layer '" + rule.name1 + "' in mocmos technology");
						return null;
					}
					layert1 = lay1.getIndex();
				}
				Layer lay2 = null;
				int layert2 = -1;
				if (rule.name2 != null)
				{
					lay2 = findLayer(rule.name2);
					if (lay2 == null)
					{
						System.out.println("Warning: no layer '" + rule.name2 + "' in mocmos technology");
						return null;
					}
					layert2 = lay2.getIndex();
				}

				// find the index in a two-layer upper-diagonal table
				int index = -1;
				if (layert1 >= 0 && layert2 >= 0)
				{
					index = getRuleIndex(layert1, layert2);
				}

				// find the nodes and arcs associated with the rule
				PrimitiveNode nty = null;
				ArcProto aty = null;
				if (rule.nodeName != null)
				{
					if (rule.ruleType == DRCTemplate.DRCRuleType.ASURROUND)
					{
						aty = this.findArcProto(rule.nodeName);
						if (aty == null)
						{
							System.out.println("Warning: no arc '" + rule.nodeName + "' in mocmos technology");
							return null;
						}
					} else if (rule.ruleType != DRCTemplate.DRCRuleType.SPACING)
					{
						nty = this.findNodeProto(rule.nodeName);
						if (nty == null)
						{
							System.out.println("Warning: no node '" + rule.nodeName + "' in mocmos technology");
							return null;
						}
					}
				}

				// get more information about the rule
				double distance = rule.value1;
				String proc = "";
				if ((when&(DRCTemplate.DRCMode.DE.mode()|DRCTemplate.DRCMode.SU.mode()|DRCTemplate.DRCMode.SC.mode())) != 0)
				{
					switch (getRuleSet())
					{
						case DEEPRULES:  proc = "DEEP";   break;
						case SUBMRULES:  proc = "SUBM";   break;
						case SCMOSRULES: proc = "SCMOS";  break;
					}
				}
				String metal = "";
				if ((when&(DRCTemplate.DRCMode.M2.mode()|DRCTemplate.DRCMode.M3.mode()|DRCTemplate.DRCMode.M4.mode()|DRCTemplate.DRCMode.M5.mode()|DRCTemplate.DRCMode.M6.mode())) != 0)
				{
					switch (getNumMetal())
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
				if (extraString.length() > 0 && ruleName.indexOf(extraString) == -1)
					ruleName += ", " +  extraString;
				rule.ruleName = new String(ruleName);

				// set the rule
				double [] specValues;
				switch (rule.ruleType)
				{
					case MINWID:
						rules.minWidth[layert1] = new Double(distance);
						rules.minWidthRules[layert1] = ruleName;
						setLayerMinWidth(rule.name1, rule.ruleName, distance);
						break;
                    case MINAREA:
						rules.minArea[layert1] = new Double(distance);
						rules.minAreaRules[layert1] = ruleName;
						break;
					case SLOTSIZE:
						rules.slotSize[layert1] = new Double(distance);
						rules.slotSizeRules[layert1] = ruleName;
						break;
                    case NODSIZ:
						setDefNodeSize(nty, rule.ruleName, distance, distance, rules);
						break;
					case SURROUND:
						setLayerSurroundLayer(rule.ruleName, nty, lay1, lay2, distance,
						        rules.minWidth[lay1.getIndex()].doubleValue());
						break;
					case ASURROUND:
						setArcLayerSurroundLayer(aty, lay1, lay2, distance);
						break;
					case VIASUR:
						setLayerSurroundVia(nty, lay1, distance);
						specValues = nty.getSpecialValues();
						specValues[2] = distance;
						specValues[3] = distance;
						break;
					case TRAWELL:
						setTransistorWellSurround(distance);
						break;
					case TRAPOLY:
						setTransistorPolyOverhang(distance);
                        rules.transPolyOverhang = distance;
						break;
					case TRAACTIVE:
						setTransistorActiveOverhang(distance);
						break;
					case SPACING:
						rules.conList[index] = new Double(distance);
						rules.unConList[index] = new Double(distance);
						rules.conListRules[index] = ruleName;
						rules.unConListRules[index] = ruleName;
                        rules.conListNodes[index] = rule.nodeName;
                        rules.unConListNodes[index] = rule.nodeName;
						break;
					case SPACINGM:
						rules.conListMulti[index] = new Double(distance);
						rules.unConListMulti[index] = new Double(distance);
						rules.conListMultiRules[index] = ruleName;
						rules.unConListMultiRules[index] = ruleName;
						break;
					case SPACINGW:
						rules.conListWide[index] = new Double(distance);
						rules.unConListWide[index] = new Double(distance);
						rules.conListWideRules[index] = ruleName;
						rules.unConListWideRules[index] = ruleName;
						break;
					case SPACINGE:
						rules.edgeList[index] = new Double(distance);
						rules.edgeListRules[index] = ruleName;
						break;
					case CONSPA:
						rules.conList[index] = new Double(distance);
						rules.conListRules[index] = ruleName;
                        rules.conListNodes[index] = rule.nodeName;
						break;
					case UCONSPA:
						rules.unConList[index] = new Double(distance);
						rules.unConListRules[index] = ruleName;
                        rules.unConListNodes[index] = rule.nodeName;
						break;
					case CUTSPA:
						specValues = nty.getSpecialValues();
						specValues[4] = distance;
						break;
					case CUTSPA2D:
                        specValues = nty.getSpecialValues();
						specValues[5] = distance;
						break;
					case CUTSIZE:
						specValues = nty.getSpecialValues();
						specValues[0] = specValues[1] = distance;
                        int nodeIndex = nty.getPrimNodeIndexInTech();
                        rules.cutNodeSize[nodeIndex] = new Double(distance);
                        rules.cutNodeSizeRules[nodeIndex] = rule.ruleName;
						break;
					case CUTSUR:
						specValues = nty.getSpecialValues();
						specValues[2] = distance;
						specValues[3] = distance;
						break;
                    default:
                        System.out.println(rule.ruleName + " is an invalid rule type in " + this);
				}
			}
		}
		rules.calculateNumberOfRules();

		return rules;
	}

    /**
	 * Method to compare a Rules set with the "factory" set and construct an override string.
	 * @param origDRCRules
	 * @param newDRCRules
	 * @return a StringBuffer that describes any overrides.  Returns "" if there are none.
	 */
	public static StringBuffer getRuleDifferences(DRCRules origDRCRules, DRCRules newDRCRules)
	{
		StringBuffer changes = new StringBuffer();
		MOSRules origRules = (MOSRules)origDRCRules;
		MOSRules newRules = (MOSRules)newDRCRules;

		// include differences in the wide-rule limit
		if (!newRules.wideLimit.equals(origRules.wideLimit))
		{
			changes.append("w:"+newRules.wideLimit+";");
		}

		// include differences in layer spacings
		for(int l1=0; l1<tech.getNumLayers(); l1++)
			for(int l2=0; l2<=l1; l2++)
		{
			int i = tech.getRuleIndex(l2, l1);
			if (!newRules.conList[i].equals(origRules.conList[i]))
			{
				changes.append("c:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.conList[i]+";");
			}
			if (!newRules.conListRules[i].equals(origRules.conListRules[i]))
			{
				changes.append("cr:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.conListRules[i]+";");
			}
			if (!newRules.unConList[i].equals(origRules.unConList[i]))
			{
				changes.append("u:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.unConList[i]+";");
			}
			if (!newRules.unConListRules[i].equals(origRules.unConListRules[i]))
			{
				changes.append("ur:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.unConListRules[i]+";");
			}

			if (!newRules.conListWide[i].equals(origRules.conListWide[i]))
			{
				changes.append("cw:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.conListWide[i]+";");
			}
			if (!newRules.conListWideRules[i].equals(origRules.conListWideRules[i]))
			{
				changes.append("cwr:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.conListWideRules[i]+";");
			}
			if (!newRules.unConListWide[i].equals(origRules.unConListWide[i]))
			{
				changes.append("uw:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.unConListWide[i]+";");
			}
			if (!newRules.unConListWideRules[i].equals(origRules.unConListWideRules[i]))
			{
				changes.append("uwr:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.unConListWideRules[i]+";");
			}

			if (!newRules.conListMulti[i].equals(origRules.conListMulti[i]))
			{
				changes.append("cm:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.conListMulti[i]+";");
			}
			if (!newRules.conListMultiRules[i].equals(origRules.conListMultiRules[i]))
			{
				changes.append("cmr:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.conListMultiRules[i]+";");
			}
			if (!newRules.unConListMulti[i].equals(origRules.unConListMulti[i]))
			{
				changes.append("um:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.unConListMulti[i]+";");
			}
			if (!newRules.unConListMultiRules[i].equals(origRules.unConListMultiRules[i]))
			{
				changes.append("umr:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.unConListMultiRules[i]+";");
			}

			if (!newRules.edgeList[i].equals(origRules.edgeList[i]))
			{
				changes.append("e:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.edgeList[i]+";");
			}
			if (!newRules.edgeListRules[i].equals(origRules.edgeListRules[i]))
			{
				changes.append("er:"+tech.getLayer(l1).getName()+"/"+tech.getLayer(l2).getName()+"="+newRules.edgeListRules[i]+";");
			}
		}

		// include differences in minimum layer widths
		for(int i=0; i<newRules.numLayers; i++)
		{
			if (!newRules.minWidth[i].equals(origRules.minWidth[i]))
			{
				changes.append("m:"+tech.getLayer(i).getName()+"="+newRules.minWidth[i]+";");
			}
			if (!newRules.minWidthRules[i].equals(origRules.minWidthRules[i]))
			{
				changes.append("mr:"+tech.getLayer(i).getName()+"="+newRules.minWidthRules[i]+";");
			}
		}

		// include differences in minimum node sizes
		int j = 0;
		for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			if (!newRules.minNodeSize[j*2].equals(origRules.minNodeSize[j*2]) ||
				!newRules.minNodeSize[j*2+1].equals(origRules.minNodeSize[j*2+1]))
			{
				changes.append("n:"+np.getName()+"="+newRules.minNodeSize[j*2]+"/"+newRules.minNodeSize[j*2+1]+";");
			}
			if (!newRules.minNodeSizeRules[j].equals(origRules.minNodeSizeRules[j]))
			{
				changes.append("nr:"+np.getName()+"="+newRules.minNodeSizeRules[j]+";");
			}
			j++;
		}
		return changes;
	}

	/**
	 * Method to be called from DRC:setRules
	 * @param newDRCRules
	 */
	public void setRuleVariables(DRCRules newDRCRules)
	{
		MOSRules newRules = (MOSRules)newDRCRules;

		// update variables on the technology
// 		Variable var = newVar(DRCRules.WIDE_LIMIT, newRules.wideLimit);
// 		var = newVar(DRCRules.MIN_CONNECTED_DISTANCES, newRules.conList);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_CONNECTED_DISTANCES_RULE, newRules.conListRules);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_UNCONNECTED_DISTANCES, newRules.unConList);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_UNCONNECTED_DISTANCES_RULE, newRules.unConListRules);
// 		if (var != null) var.setDontSave();

// 		var = newVar(DRCRules.MIN_CONNECTED_DISTANCES_WIDE, newRules.conListWide);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_CONNECTED_DISTANCES_WIDE_RULE, newRules.conListWideRules);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_UNCONNECTED_DISTANCES_WIDE, newRules.unConListWide);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_UNCONNECTED_DISTANCES_WIDE_RULE, newRules.unConListWideRules);
// 		if (var != null) var.setDontSave();

// 		var = newVar(DRCRules.MIN_CONNECTED_DISTANCES_MULTI, newRules.conListMulti);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_CONNECTED_DISTANCES_MULTI_RULE, newRules.conListMultiRules);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_UNCONNECTED_DISTANCES_MULTI, newRules.unConListMulti);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_UNCONNECTED_DISTANCES_MULTI_RULE, newRules.unConListMultiRules);
// 		if (var != null) var.setDontSave();

// 		var = newVar(DRCRules.MIN_EDGE_DISTANCES, newRules.edgeList);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_EDGE_DISTANCES_RULE, newRules.edgeListRules);
// 		if (var != null) var.setDontSave();

// 		var = newVar(DRCRules.MIN_WIDTH, newRules.minWidth);
// 		if (var != null) var.setDontSave();
// 		var = newVar(DRCRules.MIN_WIDTH_RULE, newRules.minWidthRules);
// 		if (var != null) var.setDontSave();

		// update per-node information
		int j = 0;
		for(Iterator<PrimitiveNode> it = getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			np.setMinSize(newRules.minNodeSize[j*2].doubleValue(), newRules.minNodeSize[j*2+1].doubleValue(),
				newRules.minNodeSizeRules[j]);
			j++;
		}
	}

	/**
	 * Method to implement rule 3.3 which specifies the amount of poly overhang
	 * on a transistor.
	 */
	private void setTransistorPolyOverhang(double overhang)
	{
		// define the poly box in terms of the central transistor box
		TechPoint [] pPolyPoints = transistorPolyLayers[P_TYPE].getPoints();
		EdgeH pPolyLeft = pPolyPoints[0].getX();
		EdgeH pPolyRight = pPolyPoints[1].getX();
		pPolyLeft.setAdder(6-overhang);
		pPolyRight.setAdder(-6+overhang);
		transistorPolyLayers[P_TYPE].setSerpentineExtentT(overhang);
		transistorPolyLayers[P_TYPE].setSerpentineExtentB(overhang);

		TechPoint [] nPolyPoints = transistorPolyLayers[N_TYPE].getPoints();
		EdgeH nPolyLeft = nPolyPoints[0].getX();
		EdgeH nPolyRight = nPolyPoints[1].getX();
		nPolyLeft.setAdder(6-overhang);
		nPolyRight.setAdder(-6+overhang);
		transistorPolyLayers[N_TYPE].setSerpentineExtentT(overhang);
		transistorPolyLayers[N_TYPE].setSerpentineExtentB(overhang);

		// for the electrical rule versions with split active
		TechPoint [] pPolyLPoints = transistorPolyLLayers[P_TYPE].getPoints();
		TechPoint [] pPolyRPoints = transistorPolyRLayers[P_TYPE].getPoints();
		EdgeH pPolyLLeft = pPolyLPoints[0].getX();
		EdgeH pPolyRRight = pPolyRPoints[1].getX();
		pPolyLLeft.setAdder(6-overhang);
		pPolyRRight.setAdder(-6+overhang);
		transistorPolyLLayers[P_TYPE].setSerpentineExtentT(overhang);
		transistorPolyRLayers[P_TYPE].setSerpentineExtentB(overhang);

		TechPoint [] nPolyLPoints = transistorPolyLLayers[N_TYPE].getPoints();
		TechPoint [] nPolyRPoints = transistorPolyRLayers[N_TYPE].getPoints();
		EdgeH nPolyLLeft = nPolyLPoints[0].getX();
		EdgeH nPolyRRight = nPolyRPoints[1].getX();
		nPolyLLeft.setAdder(6-overhang);
		nPolyRRight.setAdder(-6+overhang);
		transistorPolyLLayers[N_TYPE].setSerpentineExtentT(overhang);
		transistorPolyRLayers[N_TYPE].setSerpentineExtentB(overhang);
	}

	/**
	 * Method to implement rule 3.4 which specifies the amount of active overhang
	 * on a transistor.
	 */
	private void setTransistorActiveOverhang(double overhang)
	{
		TechPoint [] pActivePoints = transistorActiveLayers[P_TYPE].getPoints();
		TechPoint [] nActivePoints = transistorActiveLayers[N_TYPE].getPoints();
		TechPoint [] pActiveTPoints = transistorActiveTLayers[P_TYPE].getPoints();
		TechPoint [] nActiveTPoints = transistorActiveTLayers[N_TYPE].getPoints();
		TechPoint [] pActiveBPoints = transistorActiveBLayers[P_TYPE].getPoints();
		TechPoint [] nActiveBPoints = transistorActiveBLayers[N_TYPE].getPoints();
		TechPoint [] pWellPoints = transistorWellLayers[P_TYPE].getPoints();
		TechPoint [] nWellPoints = transistorWellLayers[N_TYPE].getPoints();
		TechPoint [] pSelectPoints = transistorSelectLayers[P_TYPE].getPoints();
		TechPoint [] nSelectPoints = transistorSelectLayers[N_TYPE].getPoints();

		// pickup extension of well about active (2.3)
		EdgeH pActiveLeft = pActivePoints[0].getX();
		EdgeH pWellLeft = pWellPoints[0].getX();
		double wellOverhang = pActiveLeft.getAdder() - pWellLeft.getAdder();

		// define the active box in terms of the central transistor box
		EdgeV pActiveBottom = pActivePoints[0].getY();
		EdgeV pActiveTop = pActivePoints[1].getY();
		pActiveBottom.setAdder(10-overhang);
		pActiveTop.setAdder(-10+overhang);
		EdgeV nActiveBottom = nActivePoints[0].getY();
		EdgeV nActiveTop = nActivePoints[1].getY();
		nActiveBottom.setAdder(10-overhang);
		nActiveTop.setAdder(-10+overhang);

		// for the electrical rule versions with split active
		EdgeV pActiveBBottom = pActiveBPoints[0].getY();
		EdgeV pActiveTTop = pActiveTPoints[1].getY();
		pActiveBBottom.setAdder(10-overhang);
		pActiveTTop.setAdder(-10+overhang);
		EdgeV nActiveBBottom = nActiveBPoints[0].getY();
		EdgeV nActiveTTop = nActiveTPoints[1].getY();
		nActiveBBottom.setAdder(10-overhang);
		nActiveTTop.setAdder(-10+overhang);

		// extension of select about active = 2 (4.2)
		EdgeV pSelectBottom = pSelectPoints[0].getY();
		EdgeV pSelectTop = pSelectPoints[1].getY();
		pSelectBottom.setAdder(pActiveBottom.getAdder()-2);
		pSelectTop.setAdder(pActiveTop.getAdder()+2);
		EdgeV nSelectBottom = nSelectPoints[0].getY();
		EdgeV nSelectTop = nSelectPoints[1].getY();
		nSelectBottom.setAdder(nActiveBottom.getAdder()-2);
		nSelectTop.setAdder(nActiveTop.getAdder()+2);

		// extension of well about active (2.3)
		EdgeV pWellBottom = pWellPoints[0].getY();
		EdgeV pWellTop = pWellPoints[1].getY();
		pWellBottom.setAdder(pActiveBottom.getAdder()-wellOverhang);
		pWellTop.setAdder(pActiveTop.getAdder()+wellOverhang);
		EdgeV nWellBottom = nWellPoints[0].getY();
		EdgeV nWellTop = nWellPoints[1].getY();
		nWellBottom.setAdder(nActiveBottom.getAdder()-wellOverhang);
		nWellTop.setAdder(nActiveTop.getAdder()+wellOverhang);

		// the serpentine active overhang
		SizeOffset so = transistorNodes[P_TYPE].getProtoSizeOffset();
		double halfPolyWidth = (transistorNodes[P_TYPE].getDefHeight() - so.getHighYOffset() - so.getLowYOffset()) / 2;
		transistorActiveLayers[P_TYPE].setSerpentineLWidth(halfPolyWidth+overhang);
		transistorActiveLayers[P_TYPE].setSerpentineRWidth(halfPolyWidth+overhang);
		transistorActiveTLayers[P_TYPE].setSerpentineRWidth(halfPolyWidth+overhang);
		transistorActiveBLayers[P_TYPE].setSerpentineLWidth(halfPolyWidth+overhang);
		transistorActiveLayers[N_TYPE].setSerpentineLWidth(halfPolyWidth+overhang);
		transistorActiveLayers[N_TYPE].setSerpentineRWidth(halfPolyWidth+overhang);
		transistorActiveTLayers[N_TYPE].setSerpentineRWidth(halfPolyWidth+overhang);
		transistorActiveBLayers[N_TYPE].setSerpentineLWidth(halfPolyWidth+overhang);

		transistorSelectLayers[P_TYPE].setSerpentineLWidth(halfPolyWidth+overhang+2);
		transistorSelectLayers[P_TYPE].setSerpentineRWidth(halfPolyWidth+overhang+2);
		transistorSelectLayers[N_TYPE].setSerpentineLWidth(halfPolyWidth+overhang+2);
		transistorSelectLayers[N_TYPE].setSerpentineRWidth(halfPolyWidth+overhang+2);

		transistorWellLayers[P_TYPE].setSerpentineLWidth(halfPolyWidth+overhang+wellOverhang);
		transistorWellLayers[P_TYPE].setSerpentineRWidth(halfPolyWidth+overhang+wellOverhang);
		transistorWellLayers[N_TYPE].setSerpentineLWidth(halfPolyWidth+overhang+wellOverhang);
		transistorWellLayers[N_TYPE].setSerpentineRWidth(halfPolyWidth+overhang+wellOverhang);
	}

	/**
	 * Method to implement rule 2.3 which specifies the amount of well surround
	 * about active on a transistor.
	 */
	private void setTransistorWellSurround(double overhang)
	{
		// define the well box in terms of the active box
		TechPoint [] pActivePoints = transistorActiveLayers[P_TYPE].getPoints();
		TechPoint [] nActivePoints = transistorActiveLayers[N_TYPE].getPoints();
		TechPoint [] pWellPoints = transistorWellLayers[P_TYPE].getPoints();
		TechPoint [] nWellPoints = transistorWellLayers[N_TYPE].getPoints();

		EdgeH pWellLeft = pWellPoints[0].getX();
		EdgeH pWellRight = pWellPoints[1].getX();
		EdgeV pWellBottom = pWellPoints[0].getY();
		EdgeV pWellTop = pWellPoints[1].getY();

		EdgeH pActiveLeft = pActivePoints[0].getX();
		EdgeH pActiveRight = pActivePoints[1].getX();
		EdgeV pActiveBottom = pActivePoints[0].getY();
		EdgeV pActiveTop = pActivePoints[1].getY();

		EdgeH nWellLeft = nWellPoints[0].getX();
		EdgeH nWellRight = nWellPoints[1].getX();
		EdgeV nWellBottom = nWellPoints[0].getY();
		EdgeV nWellTop = nWellPoints[1].getY();

		EdgeH nActiveLeft = nActivePoints[0].getX();
		EdgeH nActiveRight = nActivePoints[1].getX();
		EdgeV nActiveBottom = nActivePoints[0].getY();
		EdgeV nActiveTop = nActivePoints[1].getY();

		pWellLeft.setAdder(pActiveLeft.getAdder()-overhang);
		pWellRight.setAdder(pActiveRight.getAdder()+overhang);
		pWellBottom.setAdder(pActiveBottom.getAdder()-overhang);
		pWellTop.setAdder(pActiveTop.getAdder()+overhang);

		nWellLeft.setAdder(nActiveLeft.getAdder()-overhang);
		nWellRight.setAdder(nActiveRight.getAdder()+overhang);
		nWellBottom.setAdder(nActiveBottom.getAdder()-overhang);
		nWellTop.setAdder(nActiveTop.getAdder()+overhang);

		// the serpentine poly overhang
		transistorWellLayers[P_TYPE].setSerpentineLWidth(overhang+4);
		transistorWellLayers[P_TYPE].setSerpentineRWidth(overhang+4);
		transistorWellLayers[N_TYPE].setSerpentineLWidth(overhang+4);
		transistorWellLayers[N_TYPE].setSerpentineRWidth(overhang+4);

		transistorWellLayers[P_TYPE].setSerpentineExtentT(overhang);
		transistorWellLayers[P_TYPE].setSerpentineExtentB(overhang);
		transistorWellLayers[N_TYPE].setSerpentineExtentT(overhang);
		transistorWellLayers[N_TYPE].setSerpentineExtentB(overhang);
	}

    /******************** OVERRIDES ********************/

	/**
	 * This method overrides the one in Technology because it knows about equivalence layers for MOCMOS.
	 */
	public boolean sameLayer(Layer layer1, Layer layer2)
	{
		if (layer1 == layer2) return true;
		if (layer1 == poly1Layer && layer2 == transistorPolyLayer) return true;
		if (layer2 == poly1Layer && layer1 == transistorPolyLayer) return true;
		return false;
	}

	/**
	 * Method to convert old primitive names to their proper NodeProtos.
	 * @param name the name of the old primitive.
	 * @return the proper PrimitiveNode to use (or null if none can be determined).
	 */
	public PrimitiveNode convertOldNodeName(String name)
	{
		if (name.equals("Metal-1-Substrate-Con")) return(metalWellContactNodes[N_TYPE]);
		if (name.equals("Metal-1-Well-Con")) return(metalWellContactNodes[P_TYPE]);
		return null;
	}

    public int getNumMetals() { return MoCMOS.getNumMetal(); }

	/******************** OPTIONS ********************/

    private static Pref cacheNumberOfMetalLayers = TechPref.makeIntPref(tech, "MoCMOSNumberOfMetalLayers", 4);
    static { cacheNumberOfMetalLayers.attachToObject(tech, "Technology/Technology tab", "MOSIS CMOS: Number of Metal Layers"); }
	/**
	 * Method to tell the number of metal layers in the MoCMOS technology.
	 * The default is "4".
	 * @return the number of metal layers in the MoCMOS technology (from 2 to 6).
	 */
	public static int getNumMetal() { return cacheNumberOfMetalLayers.getInt(); }
	/**
	 * Method to set the number of metal layers in the MoCMOS technology.
	 * @param num the number of metal layers in the MoCMOS technology (from 2 to 6).
	 */
	public static void setNumMetal(int num) { cacheNumberOfMetalLayers.setInt(num); }

	private static Pref cacheRuleSet = TechPref.makeIntPref("MoCMOSRuleSet", getTechnologyPreferences(), 1);
    static
    {
    	Pref.Meaning m = cacheRuleSet.attachToObject(tech, "Technology/Technology tab", "MOSIS CMOS rule set");
    	m.setTrueMeaning(new String[] {"SCMOS", "Submicron", "Deep"});
	}
	/**
	 * Method to tell the current rule set for this Technology if Mosis is the foundry.
	 * @return the current rule set for this Technology:<BR>
	 * 0: SCMOS rules<BR>
	 * 1: Submicron rules (the default)<BR>
	 * 2: Deep rules
	 */
	public static int getRuleSet() { return cacheRuleSet.getInt(); }
	/**
	 * Method to set the rule set for this Technology.
	 * @param set the new rule set for this Technology:<BR>
	 * 0: SCMOS rules<BR>
	 * 1: Submicron rules<BR>
	 * 2: Deep rules
	 */
	public static void setRuleSet(int set) { cacheRuleSet.setInt(set); }

	private static Pref cacheSecondPolysilicon = TechPref.makeBooleanPref("MoCMOSSecondPolysilicon", getTechnologyPreferences(), false);
    static { cacheSecondPolysilicon.attachToObject(tech, "Technology/Technology tab", "MOSIS CMOS: Second Polysilicon Layer"); }
	/**
	 * Method to tell the number of polysilicon layers in this Technology.
	 * The default is false.
	 * @return true if there are 2 polysilicon layers in this Technology.
	 * If false, there is only 1 polysilicon layer.
	 */
	public static boolean isSecondPolysilicon() { return cacheSecondPolysilicon.getBoolean(); }
	/**
	 * Method to set a second polysilicon layer in this Technology.
	 * @param on true if there are 2 polysilicon layers in this Technology.
	 */
	public static void setSecondPolysilicon(boolean on) { cacheSecondPolysilicon.setBoolean(on); }

	private static Pref cacheDisallowStackedVias = TechPref.makeBooleanPref("MoCMOSDisallowStackedVias", getTechnologyPreferences(), false);
    static { cacheDisallowStackedVias.attachToObject(tech, "Technology/Technology tab", "MOSIS CMOS: Disallow Stacked Vias"); }
	/**
	 * Method to determine whether this Technology disallows stacked vias.
	 * The default is false (they are allowed).
	 * @return true if the MOCMOS technology disallows stacked vias.
	 */
	public static boolean isDisallowStackedVias() { return cacheDisallowStackedVias.getBoolean(); }
	/**
	 * Method to set whether this Technology disallows stacked vias.
	 * @param on true if the MOCMOS technology will allow disallows vias.
	 */
	public static void setDisallowStackedVias(boolean on) { cacheDisallowStackedVias.setBoolean(on); }

	private static Pref cacheAlternateActivePolyRules = TechPref.makeBooleanPref("MoCMOSAlternateActivePolyRules", getTechnologyPreferences(), false);
    static { cacheAlternateActivePolyRules.attachToObject(tech, "Technology/Technology tab", "MOSIS CMOS: Alternate Active and Poly Contact Rules"); }
	/**
	 * Method to determine whether this Technology is using alternate Active and Poly contact rules.
	 * The default is false.
	 * @return true if the MOCMOS technology is using alternate Active and Poly contact rules.
	 */
	public static boolean isAlternateActivePolyRules() { return cacheAlternateActivePolyRules.getBoolean(); }
	/**
	 * Method to set whether this Technology is using alternate Active and Poly contact rules.
	 * @param on true if the MOCMOS technology is to use alternate Active and Poly contact rules.
	 */
	public static void setAlternateActivePolyRules(boolean on) { cacheAlternateActivePolyRules.setBoolean(on); }

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
	public Map<String,Object> convertOldVariable(String varName, Object value)
	{
		if (!varName.equalsIgnoreCase(TECH_LAST_STATE.getName())) return null;
		if (!(value instanceof Integer)) return null;
		int oldBits = ((Integer)value).intValue();

        HashMap<String,Object> meanings = new HashMap<String,Object>();
        
		boolean oldNoStackedVias = (oldBits&MOCMOSNOSTACKEDVIAS) != 0;
		meanings.put(cacheDisallowStackedVias.getPrefName(), new Integer(oldNoStackedVias?1:0));

		int numMetals = 0;
		switch (oldBits&MOCMOSMETALS)
		{
			case MOCMOS2METAL: numMetals = 2;   break;
			case MOCMOS3METAL: numMetals = 3;   break;
			case MOCMOS4METAL: numMetals = 4;   break;
			case MOCMOS5METAL: numMetals = 5;   break;
			case MOCMOS6METAL: numMetals = 6;   break;
		}
		meanings.put(cacheNumberOfMetalLayers.getPrefName(), new Integer(numMetals));

		int ruleSet = 0;
		switch (oldBits&MOCMOSRULESET)
		{
			case MOCMOSSUBMRULES:  ruleSet = SUBMRULES;   break;
			case MOCMOSDEEPRULES:  ruleSet = DEEPRULES;   break;
			case MOCMOSSCMOSRULES: ruleSet = SCMOSRULES;  break;
		}
		meanings.put(cacheRuleSet.getPrefName(), new Integer(ruleSet));

		boolean alternateContactRules = (oldBits&MOCMOSALTAPRULES) != 0;
		meanings.put(cacheAlternateActivePolyRules.getPrefName(), new Integer(alternateContactRules?1:0));

		boolean secondPoly = (oldBits&MOCMOSTWOPOLY) != 0;
		meanings.put(cacheSecondPolysilicon.getPrefName(), new Integer(secondPoly?1:0));
        
		return meanings;
	}
/******************** OVERRIDES ********************/
    /**
     * Method to set the size of a transistor NodeInst in this Technology.
     * Override because for MOCMOS sense of "width" and "length" are 
     * different for resistors and transistors.
     * @param ni the NodeInst
     * @param width the new width (positive values only)
     * @param length the new length (positive values only)
     */
    public void setPrimitiveNodeSize(NodeInst ni, double width, double length)
    {
        if (ni.getFunction().isResistor()) {
        	super.setPrimitiveNodeSize(ni, length, width);
        } else {
        	super.setPrimitiveNodeSize(ni, width, length);
        }
    }

/******************** NODE DESCRIPTION (GRAPHICAL) ********************/

    /**
	 * Method to set the surround distance of layer "outerlayer" from layer "innerlayer"
	 * in node "nty" to "surround".  The array "minsize" is the minimum size of each layer.
	 */
	private void setLayerSurroundLayer(String ruleName, PrimitiveNode nty, Layer outerLayer, Layer innerLayer,
	                                   double surround, double minSizeValue)
	{
		// find the inner layer
		Technology.NodeLayer inLayer = nty.findNodeLayer(innerLayer, false);
		if (inLayer == null)
		{
			System.out.println("Internal error in " + getTechDesc() + " surround computation. Layer '" +
                    innerLayer.getName() + "' is not valid in '" + nty.getName() + "'");
			return;
		}

		// find the outer layer
		Technology.NodeLayer outLayer = nty.findNodeLayer(outerLayer, false);
		if (outLayer == null)
		{
            System.out.println("Internal error in " + getTechDesc() + " surround computation. Layer '" +
                    outerLayer.getName() + "' is not valid in '" + nty.getName() + "'");
			return;
		}

		// determine if minimum size design rules are met
		TechPoint [] inPoints = inLayer.getPoints();
		EdgeH inLeft = inPoints[0].getX();
		EdgeH inRight = inPoints[1].getX();
		EdgeV inBottom = inPoints[0].getY();
		EdgeV inTop = inPoints[1].getY();
		double leftIndent = inLeft.getAdder() - surround;
		double rightIndent = inRight.getAdder() + surround;
		double bottomIndent = inBottom.getAdder() - surround;
		double topIndent = inTop.getAdder() + surround;
		double xSize = nty.getDefWidth() - leftIndent - rightIndent;
		double ySize = nty.getDefHeight() - bottomIndent - topIndent;
		//int outerLayerIndex = outerLayer.getIndex();
		//double minSizeValue = minSize[outerLayerIndex].doubleValue();
        //double minSizeValue = minSize[outerLayerIndex].doubleValue();
		if (xSize < minSizeValue || ySize < minSizeValue)
		{
			// make it irregular to force the proper minimum size
			if (xSize < minSizeValue) rightIndent -= minSizeValue - xSize;
			if (ySize < minSizeValue) topIndent -= minSizeValue - ySize;
		}

		TechPoint [] outPoints = outLayer.getPoints();
		EdgeH outLeft = outPoints[0].getX();
		EdgeH outRight = outPoints[1].getX();
		EdgeV outBottom = outPoints[0].getY();
		EdgeV outTop = outPoints[1].getY();
		boolean hasChanged = false;
		// describe the error
		String errorMessage = "Layer surround error of outer layer '" + outerLayer.getName()
		        + "' and inner layer '" + innerLayer.getName() + "'in '" + nty.getName() + "'('" +getTechDesc() + "'):";

        leftIndent = DBMath.round(leftIndent);
        rightIndent = DBMath.round(rightIndent);
        topIndent = DBMath.round(topIndent);
        bottomIndent = DBMath.round(bottomIndent);
		if (!DBMath.areEquals(outLeft.getAdder(), leftIndent))
		{
			outLeft.setAdder(leftIndent);
			hasChanged = true;
			errorMessage += " left=" + leftIndent;
		}
		if (!DBMath.areEquals(outRight.getAdder(), rightIndent))
		{
			outRight.setAdder(rightIndent);
			hasChanged = true;
			errorMessage += " right=" + rightIndent;
		}
		if (!DBMath.areEquals(outTop.getAdder(), topIndent))
		{
			outTop.setAdder(topIndent);
			hasChanged = true;
			errorMessage += " top=" + topIndent;
		}
		if (!DBMath.areEquals(outBottom.getAdder(), bottomIndent))
		{
			outBottom.setAdder(bottomIndent);
			hasChanged = true;
			errorMessage += " bottom=" + bottomIndent;
		}
        errorMessage += "(rule " + ruleName + ")";
        // Message printed only if developer turns local flag on
		if (hasChanged && Job.LOCALDEBUGFLAG) System.out.println(errorMessage);
	}
}
