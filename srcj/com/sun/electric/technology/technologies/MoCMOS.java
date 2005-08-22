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

import com.sun.electric.Main;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.technology.Technology.TechPoint;
import com.sun.electric.technology.technologies.utils.MOSRules;
import com.sun.electric.technology.*;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * This is the MOSIS CMOS technology.
 */
public class MoCMOS extends Technology
{
	/** the MOSIS CMOS Technology object. */	public static final MoCMOS tech = new MoCMOS();

	/** Value for standard SCMOS rules. */		public static final int SCMOSRULES = 0;
	/** Value for submicron rules. */			public static final int SUBMRULES  = 1;
	/** Value for deep rules. */				public static final int DEEPRULES  = 2;

	/** key of Variable for saving technology state. */
	public static final Variable.Key TECH_LAST_STATE = ElectricObject.newKey("TECH_last_state");
	/** key of Variable for saving scalable transistor contact information. */
	public static final Variable.Key TRANS_CONTACT = ElectricObject.newKey("MOCMOS_transcontacts");

	// layers
	private Layer poly1_lay, transistorPoly_lay;

	// arcs
    /** metal 1->6 arc */						private ArcProto[] metalArcs = new ArcProto[6];
	/** polysilicon 1 arc */					private ArcProto poly1_arc;
	/** polysilicon 2 arc */					private ArcProto poly2_arc;
    /** P/N-active arc */                       ArcProto[] activeArcs = new ArcProto[2];
	/** General active arc */					private ArcProto active_arc;

	// nodes
    /** metal-1->6-pin */				        private PrimitiveNode[] metalPinNodes = new PrimitiveNode[6];
	/** polysilicon-1-pin */					private PrimitiveNode poly1Pin_node;
	/** polysilicon-2-pin */					private PrimitiveNode poly2Pin_node;
    /** P/N-active-pins */                      private PrimitiveNode[] activePinNodes = new PrimitiveNode[2];
	/** General active-pin */					private PrimitiveNode activePin_node;
    /** metal-1-P/N-active-contacts */          private PrimitiveNode[] metalActiveContactNodes = new PrimitiveNode[2];
	/** metal-1-polysilicon-1-contact */		private PrimitiveNode metal1Poly1Contact_node;
	/** metal-1-polysilicon-2-contact */		private PrimitiveNode metal1Poly2Contact_node;
	/** metal-1-polysilicon-1-2-contact */		private PrimitiveNode metal1Poly12Contact_node;
    /** P/N-Transistors */                      private PrimitiveNode[] transistorNodes = new PrimitiveNode[2];
	/** ThickOxide Transistors */				private PrimitiveNode[] thickTransistorNodes = new PrimitiveNode[2];
    /** Scalable Transistors */			        private PrimitiveNode[] scalableTransistorNodes = new PrimitiveNode[2];
    /** M1M2 -> M5M6 contacts */				private PrimitiveNode[] metalContactNodes = new PrimitiveNode[5];
    /** metal-1-P/N-Well-contacts */            private PrimitiveNode[] metalWellContactNodes = new PrimitiveNode[2];
    /** RPO poly resistors */                   private PrimitiveNode[] rpoResistorNodes = new PrimitiveNode[2];
	/** Metal-1-Node */							private PrimitiveNode metal1Node_node;
	/** Metal-2-Node */							private PrimitiveNode metal2Node_node;
	/** Metal-3-Node */							private PrimitiveNode metal3Node_node;
	/** Metal-4-Node */							private PrimitiveNode metal4Node_node;
	/** Metal-5-Node */							private PrimitiveNode metal5Node_node;
	/** Metal-6-Node */							private PrimitiveNode metal6Node_node;
	/** Polysilicon-1-Node */					private PrimitiveNode poly1Node_node;
	/** Polysilicon-2-Node */					private PrimitiveNode poly2Node_node;
	/** P-Active-Node */						private PrimitiveNode pActiveNode_node;
	/** N-Active-Node */						private PrimitiveNode nActiveNode_node;
	/** P-Select-Node */						private PrimitiveNode pSelectNode_node;
	/** N-Select-Node */						private PrimitiveNode nSelectNode_node;
	/** PolyCut-Node */							private PrimitiveNode polyCutNode_node;
	/** ActiveCut-Node */						private PrimitiveNode activeCutNode_node;
	/** Via-1-Node */							private PrimitiveNode via1Node_node;
	/** Via-2-Node */							private PrimitiveNode via2Node_node;
	/** Via-3-Node */							private PrimitiveNode via3Node_node;
	/** Via-4-Node */							private PrimitiveNode via4Node_node;
	/** Via-5-Node */							private PrimitiveNode via5Node_node;
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
	private Technology.NodeLayer[] transistorPolyLayers = new Technology.NodeLayer[2];
	private Technology.NodeLayer[] transistorActiveLayers = new Technology.NodeLayer[2];
	private Technology.NodeLayer[] transistorActiveTLayers = new Technology.NodeLayer[2];
    private Technology.NodeLayer[] transistorActiveBLayers = new Technology.NodeLayer[2];
	private Technology.NodeLayer[] transistorPolyLLayers = new Technology.NodeLayer[2];
    private Technology.NodeLayer[] transistorPolyRLayers = new Technology.NodeLayer[2];
    private Technology.NodeLayer[] transistorPolyCLayers = new Technology.NodeLayer[2];
	private Technology.NodeLayer[] transistorWellLayers = new Technology.NodeLayer[2];
	private Technology.NodeLayer[] transistorSelectLayers = new Technology.NodeLayer[2];

	// design rule constants
	/** wide rules apply to geometry larger than this */				private static final double WIDELIMIT = 100;

	private DRCTemplate [] theRules = new DRCTemplate[]
	{
		new DRCTemplate("1.1",  DRCTemplate.DE|DRCTemplate.SU, DRCTemplate.MINWID,   "P-Well",          null,            12, null),
		new DRCTemplate("1.1",  DRCTemplate.DE|DRCTemplate.SU, DRCTemplate.MINWID,   "N-Well",          null,            12, null),
		new DRCTemplate("1.1",  DRCTemplate.DE|DRCTemplate.SU, DRCTemplate.MINWID,   "Pseudo-P-Well",   null,            12, null),
		new DRCTemplate("1.1",  DRCTemplate.DE|DRCTemplate.SU, DRCTemplate.MINWID,   "Pseudo-N-Well",   null,            12, null),
		new DRCTemplate("1.1",  DRCTemplate.SC, DRCTemplate.MINWID,   "P-Well",          null,            10, null),
		new DRCTemplate("1.1",  DRCTemplate.SC, DRCTemplate.MINWID,   "N-Well",          null,            10, null),
		new DRCTemplate("1.1",  DRCTemplate.SC, DRCTemplate.MINWID,   "Pseudo-P-Well",   null,            10, null),
		new DRCTemplate("1.1",  DRCTemplate.SC, DRCTemplate.MINWID,   "Pseudo-N-Well",   null,            10, null),

		new DRCTemplate("1.2",  DRCTemplate.DE|DRCTemplate.SU, DRCTemplate.UCONSPA,  "P-Well",         "P-Well",         18, null),
		new DRCTemplate("1.2",  DRCTemplate.DE|DRCTemplate.SU, DRCTemplate.UCONSPA,  "N-Well",         "N-Well",         18, null),
		new DRCTemplate("1.2",  DRCTemplate.SC, DRCTemplate.UCONSPA,  "P-Well",         "P-Well",         9,  null),
		new DRCTemplate("1.2",  DRCTemplate.SC, DRCTemplate.UCONSPA,  "N-Well",         "N-Well",         9,  null),

		new DRCTemplate("1.3",  DRCTemplate.ALL, DRCTemplate.CONSPA,   "P-Well",         "P-Well",         6,  null),
		new DRCTemplate("1.3",  DRCTemplate.ALL, DRCTemplate.CONSPA,   "N-Well",         "N-Well",         6,  null),

		// Valid in case of unconnected node or connected node UCONSPA -> SPACING May 21, 05
		new DRCTemplate("1.4",  DRCTemplate.ALL, DRCTemplate.SPACING,  "P-Well",         "N-Well",         0,  null),

		new DRCTemplate("2.1",  DRCTemplate.ALL, DRCTemplate.MINWID,   "P-Active",        null,            3,  null),
		new DRCTemplate("2.1",  DRCTemplate.ALL, DRCTemplate.MINWID,   "N-Active",        null,            3,  null),

		new DRCTemplate("2.2",  DRCTemplate.ALL, DRCTemplate.SPACING,  "P-Active",       "P-Active",       3,  null),
		new DRCTemplate("2.2",  DRCTemplate.ALL, DRCTemplate.SPACING,  "N-Active",       "N-Active",       3,  null),
		new DRCTemplate("2.2",  DRCTemplate.ALL, DRCTemplate.SPACING,  "P-Active-Well",  "P-Active-Well",  3,  null),
		new DRCTemplate("2.2",  DRCTemplate.ALL, DRCTemplate.SPACING,  "P-Active",       "P-Active-Well",  3,  null),
		new DRCTemplate("2.2",  DRCTemplate.ALL, DRCTemplate.SPACING,  "N-Active",       "P-Active-Well",  3,  null),

		new DRCTemplate("2.3",  DRCTemplate.DE|DRCTemplate.SU, DRCTemplate.SURROUND, "N-Well",         "P-Active",       6,  "Metal-1-P-Active-Con"),
		new DRCTemplate("2.3",  DRCTemplate.DE|DRCTemplate.SU, DRCTemplate.ASURROUND,"N-Well",         "P-Active",       6,  "P-Active"),
		new DRCTemplate("2.3",  DRCTemplate.DE|DRCTemplate.SU, DRCTemplate.SURROUND, "P-Well",         "N-Active",       6,  "Metal-1-N-Active-Con"),
		new DRCTemplate("2.3",  DRCTemplate.DE|DRCTemplate.SU, DRCTemplate.ASURROUND,"P-Well",         "N-Active",       6,  "N-Active"),
		new DRCTemplate("2.3",  DRCTemplate.DE|DRCTemplate.SU, DRCTemplate.TRAWELL,   null,             null,            6,  null),
		new DRCTemplate("2.3",  DRCTemplate.SC, DRCTemplate.SURROUND, "N-Well",         "P-Active",       5,  "Metal-1-P-Active-Con"),
		new DRCTemplate("2.3",  DRCTemplate.SC, DRCTemplate.ASURROUND,"N-Well",         "P-Active",       5,  "P-Active"),
		new DRCTemplate("2.3",  DRCTemplate.SC, DRCTemplate.SURROUND, "P-Well",         "N-Active",       5,  "Metal-1-N-Active-Con"),
		new DRCTemplate("2.3",  DRCTemplate.SC, DRCTemplate.ASURROUND,"P-Well",         "N-Active",       5,  "N-Active"),
		new DRCTemplate("2.3",  DRCTemplate.SC, DRCTemplate.TRAWELL,   null,             null,            5,  null),

		// Rule 2.4 not implemented
		// In C-Electric it is implemented as 2.2 (min spacing=3) so we might discrepancies.
		new DRCTemplate("2.5 Mosis",  DRCTemplate.MOSIS, DRCTemplate.SPACING,  "P-Active",       "N-Active",       4,  null),
        new DRCTemplate("2.5 TSMC (OD.S.1)",  DRCTemplate.TSMC, DRCTemplate.SPACING,  "P-Active",       "N-Active",       2.8,  null),

		new DRCTemplate("3.1 Mosis",  DRCTemplate.MOSIS, DRCTemplate.MINWID,   "Polysilicon-1",   null,            2,  null),
		new DRCTemplate("3.1 TSMC",  DRCTemplate.TSMC, DRCTemplate.MINWID,   "Polysilicon-1",   null,            1.8,  null),
        new DRCTemplate("3.1 Mosis",  DRCTemplate.MOSIS, DRCTemplate.MINWID,   "Transistor-Poly", null,            2,  null),
        new DRCTemplate("3.1 TSMC",  DRCTemplate.TSMC, DRCTemplate.MINWID,   "Transistor-Poly", null,            1.8,  null),

		new DRCTemplate("3.2 Mosis",  DRCTemplate.MOSIS|DRCTemplate.DE|DRCTemplate.SU, DRCTemplate.SPACING,  "Polysilicon-1",  "Polysilicon-1",  3,  null),
		new DRCTemplate("3.2 TSMC",  DRCTemplate.TSMC|DRCTemplate.DE|DRCTemplate.SU, DRCTemplate.SPACING,  "Polysilicon-1",  "Polysilicon-1",  2.8,  null),
        new DRCTemplate("3.2 Mosis",   DRCTemplate.MOSIS|DRCTemplate.DE|DRCTemplate.SU, DRCTemplate.SPACING,  "Polysilicon-1",  "Transistor-Poly",3,  null),
		new DRCTemplate("3.2 TSMC",  DRCTemplate.TSMC|DRCTemplate.DE|DRCTemplate.SU, DRCTemplate.SPACING,  "Polysilicon-1",  "Transistor-Poly",2.8,  null),
        new DRCTemplate("3.2",  DRCTemplate.SC, DRCTemplate.SPACING,  "Polysilicon-1",  "Polysilicon-1",  2,  null),
		new DRCTemplate("3.2",  DRCTemplate.SC, DRCTemplate.SPACING,  "Polysilicon-1",  "Transistor-Poly",2,  null),

		new DRCTemplate("3.2a", DRCTemplate.DE, DRCTemplate.SPACING,  "Transistor-Poly","Transistor-Poly",4,  null),
		new DRCTemplate("3.2a", DRCTemplate.SU, DRCTemplate.SPACING,  "Transistor-Poly","Transistor-Poly",3,  null),
		new DRCTemplate("3.2a", DRCTemplate.SC, DRCTemplate.SPACING,  "Transistor-Poly","Transistor-Poly",2,  null),

		new DRCTemplate("3.3",  DRCTemplate.DE, DRCTemplate.TRAPOLY,   null,             null,            2.5,null),
		new DRCTemplate("3.3 Mosis",  DRCTemplate.MOSIS|DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.TRAPOLY,   null,             null,            2,  null),
        new DRCTemplate("3.3 TSMC",  DRCTemplate.TSMC|DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.TRAPOLY,   null,             null,            2.2,  null),

		new DRCTemplate("3.4",  DRCTemplate.DE, DRCTemplate.TRAACTIVE, null,             null,            4,  null),
		new DRCTemplate("3.4 Mosis",  DRCTemplate.MOSIS|DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.TRAACTIVE, null,             null,            3,  null),
//        new DRCTemplate("3.4 TSMC",  DRCTemplate.TSMC|DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.TRAACTIVE, null,             null,            3.2,  null),

		new DRCTemplate("3.5",  DRCTemplate.ALL, DRCTemplate.SPACING,  "Polysilicon-1",  "P-Active",       1,  null),
		new DRCTemplate("3.5",  DRCTemplate.ALL, DRCTemplate.SPACING,  "Transistor-Poly","P-Active",       1,  null),
		new DRCTemplate("3.5",  DRCTemplate.ALL, DRCTemplate.SPACING,  "Polysilicon-1",  "N-Active",       1,  null),
		new DRCTemplate("3.5",  DRCTemplate.ALL, DRCTemplate.SPACING,  "Transistor-Poly","N-Active",       1,  null),
		new DRCTemplate("3.5",  DRCTemplate.ALL, DRCTemplate.SPACING,  "Polysilicon-1",  "P-Active-Well",  1,  null),
		new DRCTemplate("3.5",  DRCTemplate.ALL, DRCTemplate.SPACING,  "Transistor-Poly","P-Active-Well",  1,  null),

		new DRCTemplate("4.4",  DRCTemplate.DE, DRCTemplate.MINWID,   "P-Select",        null,            4,  null),
		new DRCTemplate("4.4",  DRCTemplate.DE, DRCTemplate.MINWID,   "N-Select",        null,            4,  null),
		new DRCTemplate("4.4",  DRCTemplate.DE, DRCTemplate.MINWID,   "Pseudo-P-Select", null,            4,  null),
		new DRCTemplate("4.4",  DRCTemplate.DE, DRCTemplate.MINWID,   "Pseudo-N-Select", null,            4,  null),
		new DRCTemplate("4.4",  DRCTemplate.DE, DRCTemplate.SPACING,  "P-Select",       "P-Select",       4,  null),
		new DRCTemplate("4.4",  DRCTemplate.DE, DRCTemplate.SPACING,  "N-Select",       "N-Select",       4,  null),
		new DRCTemplate("4.4",  DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.MINWID,   "P-Select",        null,            2,  null),
		new DRCTemplate("4.4",  DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.MINWID,   "N-Select",        null,            2,  null),
		new DRCTemplate("4.4",  DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.MINWID,   "Pseudo-P-Select", null,            2,  null),
		new DRCTemplate("4.4",  DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.MINWID,   "Pseudo-N-Select", null,            2,  null),
		new DRCTemplate("4.4 Mosis",  DRCTemplate.MOSIS|DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.SPACING,  "P-Select",       "P-Select",       2,  null),
		new DRCTemplate("4.4 TSMC (N/PP.S.1)",  DRCTemplate.TSMC|DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.SPACING,  "P-Select",       "P-Select",       4.4,  null),
		new DRCTemplate("4.4 Mosis",  DRCTemplate.MOSIS|DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.SPACING,  "N-Select",       "N-Select",       2,  null),
		new DRCTemplate("4.4 TSMC (N/PP.S.1)",  DRCTemplate.TSMC|DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.SPACING,  "N-Select",       "N-Select",       4.4,  null),
		new DRCTemplate("4.4",  DRCTemplate.ALL, DRCTemplate.SPACING,  "P-Select",       "N-Select",       0,  null),

		new DRCTemplate("5.1 Mosis",  DRCTemplate.MOSIS, DRCTemplate.MINWID,   "Poly-Cut",        null,            2,  null),
        new DRCTemplate("5.1 TSMC",  DRCTemplate.TSMC, DRCTemplate.MINWID,   "Poly-Cut",        null,            2.2,  null),

		new DRCTemplate("5.2 Mosis/TSMC",        DRCTemplate.NAC,       DRCTemplate.NODSIZ,    null,             null,            5,  "Metal-1-Polysilicon-1-Con"),
		new DRCTemplate("5.2 Mosis/TSMC",        DRCTemplate.NAC,       DRCTemplate.SURROUND, "Polysilicon-1",  "Metal-1",        0.5,"Metal-1-Polysilicon-1-Con"),
		new DRCTemplate("5.2 Mosis",        DRCTemplate.MOSIS|DRCTemplate.NAC,       DRCTemplate.CUTSUR,    null,             null,            1.5,"Metal-1-Polysilicon-1-Con"),
		new DRCTemplate("CO.E.2-M1.E.2 TSMC",        DRCTemplate.TSMC|DRCTemplate.NAC,       DRCTemplate.CUTSUR,    null,             null,            1.4,"Metal-1-Polysilicon-1-Con"),
		new DRCTemplate("5.2b",       DRCTemplate.AC,        DRCTemplate.NODSIZ,    null,             null,            4,  "Metal-1-Polysilicon-1-Con"),
		new DRCTemplate("5.2b",       DRCTemplate.AC,        DRCTemplate.SURROUND, "Polysilicon-1",  "Metal-1",        0,  "Metal-1-Polysilicon-1-Con"),
		new DRCTemplate("5.2b",       DRCTemplate.AC,        DRCTemplate.CUTSUR,    null,             null,            1,  "Metal-1-Polysilicon-1-Con"),

		new DRCTemplate("5.3",     DRCTemplate.DE, DRCTemplate.CUTSPA,    null,             null,            4,  "Metal-1-Polysilicon-1-Con"),
		new DRCTemplate("5.3",     DRCTemplate.DE, DRCTemplate.SPACING,  "Poly-Cut",       "Poly-Cut",       4,  null),
		new DRCTemplate("5.3,6.3", DRCTemplate.DE|DRCTemplate.NAC,       DRCTemplate.SPACING,  "Active-Cut",     "Poly-Cut",       4,  null),
		new DRCTemplate("5.3",     DRCTemplate.SC, DRCTemplate.CUTSPA,    null,             null,            2,  "Metal-1-Polysilicon-1-Con"),
		new DRCTemplate("5.3",     DRCTemplate.SC, DRCTemplate.SPACING,  "Poly-Cut",       "Poly-Cut",       2,  null),
		new DRCTemplate("5.3,6.3", DRCTemplate.SC|DRCTemplate.NAC,       DRCTemplate.SPACING,  "Active-Cut",     "Poly-Cut",       2,  null),
        // Mosis Submicron
		new DRCTemplate("5.3 Mosis",     DRCTemplate.MOSIS|DRCTemplate.SU, DRCTemplate.CUTSPA,    null,             null,            3,  "Metal-1-Polysilicon-1-Con"),
		new DRCTemplate("5.3 Mosis",     DRCTemplate.MOSIS|DRCTemplate.SU, DRCTemplate.SPACING,  "Poly-Cut",       "Poly-Cut",       3,  null),
		new DRCTemplate("5.3,6.3 Mosis", DRCTemplate.MOSIS|DRCTemplate.SU|DRCTemplate.NAC,       DRCTemplate.SPACING,  "Active-Cut",     "Poly-Cut",       3,  null),
        // TSMC Submicron
        new DRCTemplate("5.3 TSMC",     DRCTemplate.TSMC|DRCTemplate.SU, DRCTemplate.CUTSPA,    null,             null,            2.8,  "Metal-1-Polysilicon-1-Con"),
		new DRCTemplate("5.3 TSMC",     DRCTemplate.TSMC|DRCTemplate.SU, DRCTemplate.SPACING,  "Poly-Cut",       "Poly-Cut",       2.8,  null),
		new DRCTemplate("5.3,6.3 TSMC", DRCTemplate.TSMC|DRCTemplate.SU|DRCTemplate.NAC,       DRCTemplate.SPACING,  "Active-Cut",     "Poly-Cut",       2.8,  null),

		new DRCTemplate("5.4 Mosis",  DRCTemplate.MOSIS, DRCTemplate.SPACING,  "Poly-Cut",       "Transistor-Poly", 2,  null),
        new DRCTemplate("5.4 TSMC",  DRCTemplate.TSMC, DRCTemplate.SPACING,  "Poly-Cut",       "Transistor-Poly", 2.2,  null),

		new DRCTemplate("5.5b", DRCTemplate.DE|DRCTemplate.SU|DRCTemplate.AC,        DRCTemplate.UCONSPA,  "Poly-Cut",       "Polysilicon-1",  5,  null),
		new DRCTemplate("5.5b", DRCTemplate.DE|DRCTemplate.SU|DRCTemplate.AC,        DRCTemplate.UCONSPA,  "Poly-Cut",       "Transistor-Poly",5,  null),
		new DRCTemplate("5.5b", DRCTemplate.SC|   DRCTemplate.AC,        DRCTemplate.UCONSPA,  "Poly-Cut",       "Polysilicon-1",  4,  null),
		new DRCTemplate("5.5b", DRCTemplate.SC|   DRCTemplate.AC,        DRCTemplate.UCONSPA,  "Poly-Cut",       "Transistor-Poly",4,  null),

		new DRCTemplate("5.6b",       DRCTemplate.AC,        DRCTemplate.SPACING,  "Poly-Cut",       "P-Active",       2,  null),
		new DRCTemplate("5.6b",       DRCTemplate.AC,        DRCTemplate.SPACING,  "Poly-Cut",       "N-Active",       2,  null),

		new DRCTemplate("5.7b",       DRCTemplate.AC,        DRCTemplate.SPACINGM, "Poly-Cut",       "P-Active",       3,  null),
		new DRCTemplate("5.7b",       DRCTemplate.AC,        DRCTemplate.SPACINGM, "Poly-Cut",       "N-Active",       3,  null),

		new DRCTemplate("6.1 Mosis",  DRCTemplate.MOSIS, DRCTemplate.MINWID,   "Active-Cut",      null,            2,  null),
        new DRCTemplate("6.1 TSMC",  DRCTemplate.TSMC, DRCTemplate.MINWID,   "Active-Cut",      null,            2.2,  null),

		new DRCTemplate("6.2",        DRCTemplate.NAC,       DRCTemplate.NODSIZ,    null,             null,            5,  "Metal-1-P-Active-Con"),
		new DRCTemplate("6.2",        DRCTemplate.NAC,       DRCTemplate.SURROUND, "P-Active",       "Metal-1",        0.5,"Metal-1-P-Active-Con"),
		new DRCTemplate("6.2 Mosis",        DRCTemplate.MOSIS|DRCTemplate.NAC,       DRCTemplate.SURROUND, "P-Select",       "P-Active",       2,  "Metal-1-P-Active-Con"),
        new DRCTemplate("6.2 TSMC (PP.C.1)",        DRCTemplate.TSMC|DRCTemplate.NAC,       DRCTemplate.SURROUND, "P-Select",       "P-Active",       2.6,  "Metal-1-P-Active-Con"),
		new DRCTemplate("6.2",  DRCTemplate.DE|DRCTemplate.SU|DRCTemplate.NAC,       DRCTemplate.SURROUND, "N-Well",         "P-Active",       6,  "Metal-1-P-Active-Con"),
		new DRCTemplate("6.2",  DRCTemplate.SC|   DRCTemplate.NAC,       DRCTemplate.SURROUND, "N-Well",         "P-Active",       5,  "Metal-1-P-Active-Con"),
		new DRCTemplate("6.2 Mosis",        DRCTemplate.MOSIS|DRCTemplate.NAC,       DRCTemplate.CUTSUR,    null,             null,            1.5,"Metal-1-P-Active-Con"),
        new DRCTemplate("6.2 TSMC",        DRCTemplate.TSMC|DRCTemplate.NAC,       DRCTemplate.CUTSUR,    null,             null,            1.4,"Metal-1-P-Active-Con"),
		new DRCTemplate("6.2b",       DRCTemplate.AC,        DRCTemplate.NODSIZ,    null,             null,            4,  "Metal-1-P-Active-Con"),
		new DRCTemplate("6.2b",       DRCTemplate.AC,        DRCTemplate.SURROUND, "P-Active",       "Metal-1",        0,  "Metal-1-P-Active-Con"),
		new DRCTemplate("6.2b",       DRCTemplate.AC,        DRCTemplate.SURROUND, "P-Select",       "P-Active",       2,  "Metal-1-P-Active-Con"),
		new DRCTemplate("6.2b", DRCTemplate.DE|DRCTemplate.SU|DRCTemplate.AC,        DRCTemplate.SURROUND, "N-Well",         "P-Active",       6,  "Metal-1-P-Active-Con"),
		new DRCTemplate("6.2b", DRCTemplate.SC|   DRCTemplate.AC,        DRCTemplate.SURROUND, "N-Well",         "P-Active",       5,  "Metal-1-P-Active-Con"),
		new DRCTemplate("6.2b",       DRCTemplate.AC,        DRCTemplate.CUTSUR,    null,             null,            1,  "Metal-1-P-Active-Con"),

		new DRCTemplate("6.2",        DRCTemplate.NAC,       DRCTemplate.NODSIZ,    null,             null,            5,  "Metal-1-N-Active-Con"),
		new DRCTemplate("6.2",        DRCTemplate.NAC,       DRCTemplate.SURROUND, "N-Active",       "Metal-1",        0.5,"Metal-1-N-Active-Con"),
        new DRCTemplate("6.2 Mosis",        DRCTemplate.MOSIS|DRCTemplate.NAC,       DRCTemplate.SURROUND, "N-Select",       "N-Active",       2,  "Metal-1-N-Active-Con"),
        new DRCTemplate("6.2 TSMC (PP.C.1)",        DRCTemplate.TSMC|DRCTemplate.NAC,       DRCTemplate.SURROUND, "N-Select",       "N-Active",       2.6,  "Metal-1-N-Active-Con"),
		new DRCTemplate("6.2",  DRCTemplate.DE|DRCTemplate.SU|DRCTemplate.NAC,       DRCTemplate.SURROUND, "P-Well",         "N-Active",       6,  "Metal-1-N-Active-Con"),
		new DRCTemplate("6.2",  DRCTemplate.SC|   DRCTemplate.NAC,       DRCTemplate.SURROUND, "P-Well",         "N-Active",       5,  "Metal-1-N-Active-Con"),
        new DRCTemplate("6.2 Mosis",        DRCTemplate.MOSIS|DRCTemplate.NAC,       DRCTemplate.CUTSUR,    null,             null,            1.5,"Metal-1-N-Active-Con"),
        new DRCTemplate("6.2 TSMC",        DRCTemplate.TSMC|DRCTemplate.NAC,       DRCTemplate.CUTSUR,    null,             null,            1.4,"Metal-1-N-Active-Con"),
		new DRCTemplate("6.2b",       DRCTemplate.AC,        DRCTemplate.NODSIZ,    null,             null,            4,  "Metal-1-N-Active-Con"),
		new DRCTemplate("6.2b",       DRCTemplate.AC,        DRCTemplate.SURROUND, "N-Active",       "Metal-1",        0,  "Metal-1-N-Active-Con"),
		new DRCTemplate("6.2b",       DRCTemplate.AC,        DRCTemplate.SURROUND, "N-Select",       "N-Active",       2,  "Metal-1-N-Active-Con"),
		new DRCTemplate("6.2b", DRCTemplate.DE|DRCTemplate.SU|DRCTemplate.AC,        DRCTemplate.SURROUND, "P-Well",         "N-Active",       6,  "Metal-1-N-Active-Con"),
		new DRCTemplate("6.2b", DRCTemplate.SC|   DRCTemplate.AC,        DRCTemplate.SURROUND, "P-Well",         "N-Active",       5,  "Metal-1-N-Active-Con"),
		new DRCTemplate("6.2b",       DRCTemplate.AC,        DRCTemplate.CUTSUR,    null,             null,            1,  "Metal-1-N-Active-Con"),

		new DRCTemplate("6.2",        DRCTemplate.NAC,       DRCTemplate.NODSIZ,    null,             null,            5,  "Metal-1-P-Well-Con"),
        new DRCTemplate("6.2",        DRCTemplate.NAC,       DRCTemplate.SURROUND, "P-Active-Well",  "Metal-1",        0.5,"Metal-1-P-Well-Con"),
//		new DRCTemplate("6.2 Mosis",        DRCTemplate.MOSIS|DRCTemplate.NAC,       DRCTemplate.SURROUND, "P-Active-Well",  "Metal-1",        0.5,"Metal-1-P-Well-Con"),
//		new DRCTemplate("6.2 TSMC",        DRCTemplate.TSMC|DRCTemplate.NAC,       DRCTemplate.SURROUND, "P-Active-Well",  "Metal-1",        0.1,"Metal-1-P-Well-Con"),
        new DRCTemplate("6.2",        DRCTemplate.NAC,       DRCTemplate.SURROUND, "P-Select",       "P-Active-Well",  2,  "Metal-1-P-Well-Con"),
//        new DRCTemplate("6.2 Mosis",        DRCTemplate.MOSIS|DRCTemplate.NAC,       DRCTemplate.SURROUND, "P-Select",       "P-Active-Well",  2,  "Metal-1-P-Well-Con"),
//        new DRCTemplate("6.2 TSMC",        DRCTemplate.TSMC|DRCTemplate.NAC,       DRCTemplate.SURROUND, "P-Select",       "P-Active-Well",  0.9,  "Metal-1-P-Well-Con"),
		new DRCTemplate("6.2",        DRCTemplate.NAC,       DRCTemplate.SURROUND, "P-Well",         "P-Active-Well",  3,  "Metal-1-P-Well-Con"),
//        new DRCTemplate("6.2 Mosis",        DRCTemplate.MOSIS|DRCTemplate.NAC,       DRCTemplate.SURROUND, "P-Well",         "P-Active-Well",  3,  "Metal-1-P-Well-Con"),
//        new DRCTemplate("6.2 TSMC",        DRCTemplate.TSMC|DRCTemplate.NAC,       DRCTemplate.SURROUND, "P-Well",         "P-Active-Well",  4.3,  "Metal-1-P-Well-Con"),
		new DRCTemplate("6.2",        DRCTemplate.NAC,       DRCTemplate.CUTSUR,    null,             null,            1.5,"Metal-1-P-Well-Con"),
		new DRCTemplate("6.2b",       DRCTemplate.AC,        DRCTemplate.NODSIZ,    null,             null,            4,  "Metal-1-P-Well-Con"),
		new DRCTemplate("6.2b",       DRCTemplate.AC,        DRCTemplate.SURROUND, "P-Active-Well",  "Metal-1",        0,  "Metal-1-P-Well-Con"),
		new DRCTemplate("6.2b",       DRCTemplate.AC,        DRCTemplate.SURROUND, "P-Select",       "P-Active-Well",  2,  "Metal-1-P-Well-Con"),
		new DRCTemplate("6.2b",       DRCTemplate.AC,        DRCTemplate.SURROUND, "P-Well",         "P-Active-Well",  3,  "Metal-1-P-Well-Con"),
		new DRCTemplate("6.2b",       DRCTemplate.AC,        DRCTemplate.CUTSUR,    null,             null,            1,  "Metal-1-P-Well-Con"),

		new DRCTemplate("6.2",        DRCTemplate.NAC,       DRCTemplate.NODSIZ,    null,             null,            5,  "Metal-1-N-Well-Con"),
		new DRCTemplate("6.2",        DRCTemplate.NAC,       DRCTemplate.SURROUND, "N-Active",       "Metal-1",        0.5,"Metal-1-N-Well-Con"),
		new DRCTemplate("6.2",        DRCTemplate.NAC,       DRCTemplate.SURROUND, "N-Select",       "N-Active",       2,  "Metal-1-N-Well-Con"),
        new DRCTemplate("6.2",        DRCTemplate.NAC,       DRCTemplate.SURROUND, "N-Well",         "N-Active",       3,  "Metal-1-N-Well-Con"),
		new DRCTemplate("6.2",        DRCTemplate.NAC,       DRCTemplate.CUTSUR,    null,             null,            1.5,"Metal-1-N-Well-Con"),
		new DRCTemplate("6.2b",       DRCTemplate.AC,        DRCTemplate.NODSIZ,    null,             null,            4,  "Metal-1-N-Well-Con"),
		new DRCTemplate("6.2b",       DRCTemplate.AC,        DRCTemplate.SURROUND, "N-Active",       "Metal-1",        0,  "Metal-1-N-Well-Con"),
		new DRCTemplate("6.2b",       DRCTemplate.AC,        DRCTemplate.SURROUND, "N-Select",       "N-Active",       2,  "Metal-1-N-Well-Con"),
		new DRCTemplate("6.2b",       DRCTemplate.AC,        DRCTemplate.SURROUND, "N-Well",         "N-Active",       3,  "Metal-1-N-Well-Con"),
		new DRCTemplate("6.2b",       DRCTemplate.AC,        DRCTemplate.CUTSUR,    null,             null,            1,  "Metal-1-N-Well-Con"),

		new DRCTemplate("6.3",  DRCTemplate.DE, DRCTemplate.CUTSPA,    null,             null,            4,  "Metal-1-P-Active-Con"),
		new DRCTemplate("6.3",  DRCTemplate.DE, DRCTemplate.CUTSPA,    null,             null,            4,  "Metal-1-N-Active-Con"),
		new DRCTemplate("6.3",  DRCTemplate.DE, DRCTemplate.SPACING,  "Active-Cut",     "Active-Cut",     4,  null),
		new DRCTemplate("6.3",  DRCTemplate.SC, DRCTemplate.CUTSPA,    null,             null,            2,  "Metal-1-P-Active-Con"),
		new DRCTemplate("6.3",  DRCTemplate.SC, DRCTemplate.CUTSPA,    null,             null,            2,  "Metal-1-N-Active-Con"),
		new DRCTemplate("6.3",  DRCTemplate.SC, DRCTemplate.SPACING,  "Active-Cut",     "Active-Cut",     2,  null),
        // Mosis
		new DRCTemplate("6.3 Mosis",  DRCTemplate.MOSIS|DRCTemplate.SU, DRCTemplate.CUTSPA,    null,             null,            3,  "Metal-1-P-Active-Con"),
		new DRCTemplate("6.3 Mosis",  DRCTemplate.MOSIS|DRCTemplate.SU, DRCTemplate.CUTSPA,    null,             null,            3,  "Metal-1-N-Active-Con"),
		new DRCTemplate("6.3 Mosis",  DRCTemplate.MOSIS|DRCTemplate.SU, DRCTemplate.SPACING,  "Active-Cut",     "Active-Cut",     3,  null),
        // TSMC
		new DRCTemplate("6.3 TSMC",  DRCTemplate.TSMC|DRCTemplate.SU, DRCTemplate.CUTSPA,    null,             null,            2.8,  "Metal-1-P-Active-Con"),
		new DRCTemplate("6.3 TSMC",  DRCTemplate.TSMC|DRCTemplate.SU, DRCTemplate.CUTSPA,    null,             null,            2.8,  "Metal-1-N-Active-Con"),
		new DRCTemplate("6.3 TSMC",  DRCTemplate.TSMC|DRCTemplate.SU, DRCTemplate.SPACING,  "Active-Cut",     "Active-Cut",     2.8,  null),

//		new DRCTemplate("6.4",  DRCTemplate.ALL, DRCTemplate.SPACING,  "Active-Cut",     "Transistor-Poly",2,  null),
        new DRCTemplate("6.4 Mosis",  DRCTemplate.MOSIS, DRCTemplate.SPACING,  "Active-Cut",     "Transistor-Poly",2,  null),
        new DRCTemplate("6.4 TSMC",  DRCTemplate.TSMC, DRCTemplate.SPACING,  "Active-Cut",     "Transistor-Poly",1.8,  null),

		new DRCTemplate("6.5b",       DRCTemplate.AC,        DRCTemplate.UCONSPA,  "Active-Cut",     "P-Active",       5,  null),
		new DRCTemplate("6.5b",       DRCTemplate.AC,        DRCTemplate.UCONSPA,  "Active-Cut",     "N-Active",       5,  null),

		new DRCTemplate("6.6b",       DRCTemplate.AC,        DRCTemplate.SPACING,  "Active-Cut",     "Polysilicon-1",  2,  null),
		// 6.7b is not implemented due to complexity. See manual
		new DRCTemplate("6.8b",       DRCTemplate.AC,        DRCTemplate.SPACING,  "Active-Cut",     "Poly-Cut",       4,  null),

		new DRCTemplate("7.1",  DRCTemplate.ALL, DRCTemplate.MINWID,   "Metal-1",         null,            3,  null),

		new DRCTemplate("7.2",  DRCTemplate.DE|DRCTemplate.SU, DRCTemplate.SPACING,  "Metal-1",        "Metal-1",        3,  null),
		new DRCTemplate("7.2",  DRCTemplate.SC, DRCTemplate.SPACING,  "Metal-1",        "Metal-1",        2,  null),

		new DRCTemplate("7.4",  DRCTemplate.DE|DRCTemplate.SU, DRCTemplate.SPACINGW, WIDELIMIT, 0, "Metal-1",        "Metal-1",        6, -1),
		new DRCTemplate("7.4",  DRCTemplate.SC, DRCTemplate.SPACINGW, WIDELIMIT, 0, "Metal-1",        "Metal-1",        4, -1),

		new DRCTemplate("8.1",  DRCTemplate.DE, DRCTemplate.CUTSIZE,   null,             null,            3, "Metal-1-Metal-2-Con"),
		new DRCTemplate("8.1",  DRCTemplate.DE, DRCTemplate.NODSIZ,    null,             null,            5, "Metal-1-Metal-2-Con"),
		new DRCTemplate("8.1 Mosis",  DRCTemplate.MOSIS|DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.CUTSIZE,   null,             null,            2, "Metal-1-Metal-2-Con"),
        new DRCTemplate("8.1 TSMC",  DRCTemplate.TSMC|DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.CUTSIZE,   null,             null,            2.6, "Metal-1-Metal-2-Con"),
		new DRCTemplate("8.1",  DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.NODSIZ,    null,             null,            4, "Metal-1-Metal-2-Con"),

		new DRCTemplate("8.2 Mosis",  DRCTemplate.MOSIS, DRCTemplate.SPACING,  "Via1",           "Via1",           3,  null),
        new DRCTemplate("8.2 TSMC",  DRCTemplate.TSMC, DRCTemplate.SPACING,  "Via1",           "Via1",           2.6,  null),

		new DRCTemplate("8.3 Mosis",  DRCTemplate.MOSIS,               DRCTemplate.VIASUR,   "Metal-1",         null,            1, "Metal-1-Metal-2-Con"),
        new DRCTemplate("8.3 TSMC",  DRCTemplate.TSMC,               DRCTemplate.VIASUR,   "Metal-1",         null,            0.7, "Metal-1-Metal-2-Con"),

		new DRCTemplate("8.4",        DRCTemplate.NSV,       DRCTemplate.SPACING,  "Poly-Cut",       "Via1",           2,  null),
		new DRCTemplate("8.4",        DRCTemplate.NSV,       DRCTemplate.SPACING,  "Active-Cut",     "Via1",           2,  null),

		new DRCTemplate("8.5",        DRCTemplate.NSV,       DRCTemplate.SPACINGE, "Via1",           "Polysilicon-1",  2,  null),
		new DRCTemplate("8.5",        DRCTemplate.NSV,       DRCTemplate.SPACINGE, "Via1",           "Transistor-Poly",2,  null),
		new DRCTemplate("8.5",        DRCTemplate.NSV,       DRCTemplate.SPACINGE, "Via1",           "Polysilicon-2",  2,  null),
		new DRCTemplate("8.5",        DRCTemplate.NSV,       DRCTemplate.SPACINGE, "Via1",           "P-Active",       2,  null),
		new DRCTemplate("8.5",        DRCTemplate.NSV,       DRCTemplate.SPACINGE, "Via1",           "N-Active",       2,  null),

		new DRCTemplate("9.1",  DRCTemplate.ALL, DRCTemplate.MINWID,   "Metal-2",         null,            3,  null),

		new DRCTemplate("9.2",  DRCTemplate.DE, DRCTemplate.SPACING,  "Metal-2",        "Metal-2",        4,  null),
		new DRCTemplate("9.2",  DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.SPACING,  "Metal-2",        "Metal-2",        3,  null),

		new DRCTemplate("9.3 Mosis",  DRCTemplate.MOSIS,               DRCTemplate.VIASUR,   "Metal-2",         null,            1, "Metal-1-Metal-2-Con"),
        new DRCTemplate("9.3 TSMC",  DRCTemplate.TSMC,               DRCTemplate.VIASUR,   "Metal-2",         null,            0.7, "Metal-1-Metal-2-Con"),

		new DRCTemplate("9.4",  DRCTemplate.DE, DRCTemplate.SPACINGW, WIDELIMIT, 0, "Metal-2",        "Metal-2",        8, -1),
		new DRCTemplate("9.4",  DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.SPACINGW, WIDELIMIT, 0, "Metal-2",        "Metal-2",        6, -1),

		new DRCTemplate("11.1", DRCTemplate.SU, DRCTemplate.MINWID,   "Polysilicon-2",   null,            7,  null),
		new DRCTemplate("11.1", DRCTemplate.SC, DRCTemplate.MINWID,   "Polysilicon-2",   null,            3,  null),

		new DRCTemplate("11.2", DRCTemplate.ALL, DRCTemplate.SPACING,  "Polysilicon-2",  "Polysilicon-2",  3,  null),

		new DRCTemplate("11.3", DRCTemplate.SU, DRCTemplate.SURROUND, "Polysilicon-2",  "Polysilicon-1",  5,  "Metal-1-Polysilicon-1-2-Con"),
		new DRCTemplate("11.3", DRCTemplate.SU, DRCTemplate.NODSIZ,    null,             null,            15, "Metal-1-Polysilicon-1-2-Con"),
		new DRCTemplate("11.3", DRCTemplate.SU, DRCTemplate.CUTSUR,    null,             null,            6.5,"Metal-1-Polysilicon-1-2-Con"),
		new DRCTemplate("11.3", DRCTemplate.SC, DRCTemplate.SURROUND, "Polysilicon-2",  "Polysilicon-1",  2,  "Metal-1-Polysilicon-1-2-Con"),
		new DRCTemplate("11.3", DRCTemplate.SC, DRCTemplate.NODSIZ,    null,             null,            9,  "Metal-1-Polysilicon-1-2-Con"),
		new DRCTemplate("11.3", DRCTemplate.SC, DRCTemplate.CUTSUR,    null,             null,            3.5,"Metal-1-Polysilicon-1-2-Con"),

		new DRCTemplate("14.1", DRCTemplate.DE, DRCTemplate.CUTSIZE,   null,             null,            3,  "Metal-2-Metal-3-Con"),
		new DRCTemplate("14.1", DRCTemplate.DE, DRCTemplate.MINWID,   "Via2",            null,            3,  null),
		new DRCTemplate("14.1", DRCTemplate.DE, DRCTemplate.NODSIZ,    null,             null,            5,  "Metal-2-Metal-3-Con"),
		new DRCTemplate("14.1 Mosis", DRCTemplate.MOSIS|DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.CUTSIZE,   null,             null,            2,  "Metal-2-Metal-3-Con"),
        new DRCTemplate("14.1 TSMC", DRCTemplate.TSMC|DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.CUTSIZE,   null,             null,            2.6,  "Metal-2-Metal-3-Con"),
		new DRCTemplate("14.1 Mosis", DRCTemplate.MOSIS|DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.MINWID,   "Via2",            null,            2,  null),
        new DRCTemplate("14.1 TSMC", DRCTemplate.TSMC|DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.MINWID,   "Via2",            null,            2.6,  null),
		new DRCTemplate("14.1", DRCTemplate.SU|DRCTemplate.SC|    DRCTemplate.M23,   DRCTemplate.NODSIZ,    null,             null,            6,  "Metal-2-Metal-3-Con"),
		new DRCTemplate("14.1", DRCTemplate.SU|DRCTemplate.SC|    DRCTemplate.M456,  DRCTemplate.NODSIZ,    null,             null,            4,  "Metal-2-Metal-3-Con"),

		new DRCTemplate("14.2 Mosis", DRCTemplate.MOSIS, DRCTemplate.SPACING,  "Via2",           "Via2",           3,  null),
        new DRCTemplate("14.2 TSMC", DRCTemplate.TSMC, DRCTemplate.SPACING,  "Via2",           "Via2",           2.6,  null),

		new DRCTemplate("14.3 Mosis", DRCTemplate.MOSIS, DRCTemplate.VIASUR,   "Metal-2",         null,            1,  "Metal-2-Metal-3-Con"),
        new DRCTemplate("14.3 TSMC", DRCTemplate.TSMC, DRCTemplate.VIASUR,   "Metal-2",         null,            0.7,  "Metal-2-Metal-3-Con"),

		new DRCTemplate("14.4", DRCTemplate.SU|DRCTemplate.SC|DRCTemplate.NSV,       DRCTemplate.SPACING,  "Via1",           "Via2",           2,  null),  /// ?? might need attention

		new DRCTemplate("15.1", DRCTemplate.SC| DRCTemplate.M3,    DRCTemplate.MINWID,   "Metal-3",         null,            6,  null),
		new DRCTemplate("15.1", DRCTemplate.SU| DRCTemplate.M3,    DRCTemplate.MINWID,   "Metal-3",         null,            5,  null),
		new DRCTemplate("15.1", DRCTemplate.SC| DRCTemplate.M456,  DRCTemplate.MINWID,   "Metal-3",         null,            3,  null),
		new DRCTemplate("15.1", DRCTemplate.SU| DRCTemplate.M456,  DRCTemplate.MINWID,   "Metal-3",         null,            3,  null),
		new DRCTemplate("15.1", DRCTemplate.DE, DRCTemplate.MINWID,   "Metal-3",         null,            3,  null),

		new DRCTemplate("15.2", DRCTemplate.DE, DRCTemplate.SPACING,  "Metal-3",        "Metal-3",        4,  null),
		new DRCTemplate("15.2", DRCTemplate.SU, DRCTemplate.SPACING,  "Metal-3",        "Metal-3",        3,  null),
        new DRCTemplate("15.2", DRCTemplate.SC|DRCTemplate.M3,    DRCTemplate.SPACING,  "Metal-3",        "Metal-3",        4,  null),
		new DRCTemplate("15.2", DRCTemplate.SC|DRCTemplate.M456,  DRCTemplate.SPACING,  "Metal-3",        "Metal-3",        3,  null),

		new DRCTemplate("15.3", DRCTemplate.DE, DRCTemplate.VIASUR,   "Metal-3",         null,            1, "Metal-2-Metal-3-Con"),
		new DRCTemplate("15.3", DRCTemplate.SU|DRCTemplate.SC|    DRCTemplate.M3,    DRCTemplate.VIASUR,   "Metal-3",         null,            2, "Metal-2-Metal-3-Con"),
		new DRCTemplate("15.3 Mosis", DRCTemplate.MOSIS|DRCTemplate.SU|DRCTemplate.SC|    DRCTemplate.M456,  DRCTemplate.VIASUR,   "Metal-3",         null,            1, "Metal-2-Metal-3-Con"),
        new DRCTemplate("15.3 TSMC", DRCTemplate.TSMC|DRCTemplate.SU|DRCTemplate.SC|    DRCTemplate.M456,  DRCTemplate.VIASUR,   "Metal-3",         null,            0.7, "Metal-2-Metal-3-Con"),

		new DRCTemplate("15.4", DRCTemplate.DE, DRCTemplate.SPACINGW, WIDELIMIT, 0, "Metal-3",        "Metal-3",        8, -1),
		new DRCTemplate("15.4", DRCTemplate.SU, DRCTemplate.SPACINGW, WIDELIMIT, 0, "Metal-3",        "Metal-3",        6, -1),
		new DRCTemplate("15.4", DRCTemplate.SC|DRCTemplate.M3,    DRCTemplate.SPACINGW, WIDELIMIT, 0, "Metal-3",        "Metal-3",        8, -1),
		new DRCTemplate("15.4", DRCTemplate.SC|DRCTemplate.M456,  DRCTemplate.SPACINGW, WIDELIMIT, 0, "Metal-3",        "Metal-3",        6, -1),

		new DRCTemplate("21.1", DRCTemplate.DE, DRCTemplate.CUTSIZE,   null,             null,            3, "Metal-3-Metal-4-Con"),
		new DRCTemplate("21.1", DRCTemplate.DE, DRCTemplate.MINWID,   "Via3",            null,            3,  null),
		new DRCTemplate("21.1", DRCTemplate.DE, DRCTemplate.NODSIZ,    null,             null,            5, "Metal-3-Metal-4-Con"),
		new DRCTemplate("21.1 Mosis", DRCTemplate.MOSIS|DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.CUTSIZE,   null,             null,            2, "Metal-3-Metal-4-Con"),
        new DRCTemplate("21.1 TSMC",  DRCTemplate.TSMC|DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.CUTSIZE,   null,             null,            2.6, "Metal-3-Metal-4-Con"),
		new DRCTemplate("21.1 Mosis", DRCTemplate.MOSIS|DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.MINWID,   "Via3",            null,            2,  null),
		new DRCTemplate("21.1 TSMC", DRCTemplate.TSMC|DRCTemplate.SU|DRCTemplate.SC, DRCTemplate.MINWID,   "Via3",            null,            2.6,  null),
        new DRCTemplate("21.1", DRCTemplate.SU|DRCTemplate.M4,    DRCTemplate.NODSIZ,    null,             null,            6, "Metal-3-Metal-4-Con"),
		new DRCTemplate("21.1", DRCTemplate.SU|DRCTemplate.M56,   DRCTemplate.NODSIZ,    null,             null,            4, "Metal-3-Metal-4-Con"),
		new DRCTemplate("21.1", DRCTemplate.SC, DRCTemplate.NODSIZ,    null,             null,            6, "Metal-3-Metal-4-Con"),

		new DRCTemplate("21.2 Mosis", DRCTemplate.MOSIS, DRCTemplate.SPACING,  "Via3",           "Via3",           3,  null),
        new DRCTemplate("21.2 TSMC", DRCTemplate.TSMC, DRCTemplate.SPACING,  "Via3",           "Via3",           2.6,  null),

		new DRCTemplate("21.3 Mosis", DRCTemplate.MOSIS,               DRCTemplate.VIASUR,   "Metal-3",         null,            1, "Metal-3-Metal-4-Con"),
        new DRCTemplate("21.3 TSMC", DRCTemplate.TSMC,               DRCTemplate.VIASUR,   "Metal-3",         null,            0.7, "Metal-3-Metal-4-Con"),

		new DRCTemplate("22.1", DRCTemplate.M4,    DRCTemplate.MINWID,   "Metal-4",         null,            6,  null),
		new DRCTemplate("22.1", DRCTemplate.M56,   DRCTemplate.MINWID,   "Metal-4",         null,            3,  null),

		new DRCTemplate("22.2", DRCTemplate.M4,    DRCTemplate.SPACING,  "Metal-4",        "Metal-4",        6,  null),
		new DRCTemplate("22.2", DRCTemplate.DE|DRCTemplate.M56,   DRCTemplate.SPACING,  "Metal-4",        "Metal-4",        4,  null),
		new DRCTemplate("22.2", DRCTemplate.SU|DRCTemplate.M56,   DRCTemplate.SPACING,  "Metal-4",        "Metal-4",        3,  null),

		new DRCTemplate("22.3", DRCTemplate.M4,    DRCTemplate.VIASUR,   "Metal-4",         null,            2, "Metal-3-Metal-4-Con"),
		new DRCTemplate("22.3 Mosis", DRCTemplate.MOSIS|DRCTemplate.M56,   DRCTemplate.VIASUR,   "Metal-4",         null,            1, "Metal-3-Metal-4-Con"),
        new DRCTemplate("22.3 TSMC", DRCTemplate.TSMC|DRCTemplate.M56,   DRCTemplate.VIASUR,   "Metal-4",         null,            0.7, "Metal-3-Metal-4-Con"),

		new DRCTemplate("22.4", DRCTemplate.M4,    DRCTemplate.SPACINGW, WIDELIMIT, 0, "Metal-4",        "Metal-4",        12, -1),
		new DRCTemplate("22.4", DRCTemplate.DE|DRCTemplate.M56,   DRCTemplate.SPACINGW, WIDELIMIT, 0, "Metal-4",        "Metal-4",        8, -1),
		new DRCTemplate("22.4", DRCTemplate.SU|DRCTemplate.M56,   DRCTemplate.SPACINGW, WIDELIMIT, 0, "Metal-4",        "Metal-4",        6, -1),

		new DRCTemplate("24.1", DRCTemplate.ALL, DRCTemplate.MINWID,  "Thick-Active",    null,            4,  null),
		new DRCTemplate("24.2", DRCTemplate.ALL, DRCTemplate.SPACING, "Thick-Active",   "Thick-Active",   4, null),

		new DRCTemplate("25.1", DRCTemplate.DE, DRCTemplate.CUTSIZE,   null,             null,            3, "Metal-4-Metal-5-Con"),
		new DRCTemplate("25.1", DRCTemplate.DE, DRCTemplate.MINWID,   "Via4",            null,            3,  null),
		new DRCTemplate("25.1 Mosis", DRCTemplate.MOSIS|DRCTemplate.SU, DRCTemplate.CUTSIZE,   null,             null,            2, "Metal-4-Metal-5-Con"),
		new DRCTemplate("25.1 TSMC", DRCTemplate.TSMC|DRCTemplate.SU, DRCTemplate.CUTSIZE,   null,             null,            2.6, "Metal-4-Metal-5-Con"),
        new DRCTemplate("25.1 Mosis", DRCTemplate.MOSIS|DRCTemplate.SU, DRCTemplate.MINWID,   "Via4",            null,            2,  null),
        new DRCTemplate("25.1 TSMC", DRCTemplate.TSMC|DRCTemplate.SU, DRCTemplate.NODSIZ,    null,             null,            4, "Metal-4-Metal-5-Con"),
		new DRCTemplate("25.1", DRCTemplate.DE|DRCTemplate.M5,    DRCTemplate.NODSIZ,    null,             null,            7, "Metal-4-Metal-5-Con"),
		new DRCTemplate("25.1", DRCTemplate.DE|DRCTemplate.M6,    DRCTemplate.NODSIZ,    null,             null,            5, "Metal-4-Metal-5-Con"),

		// Bug even in C-Electric It was SPACINGW originally
		new DRCTemplate("25.2 Mosis", DRCTemplate.MOSIS,               DRCTemplate.SPACING, "Via4",           "Via4",           3, null),
        new DRCTemplate("25.2 TSMC", DRCTemplate.TSMC,               DRCTemplate.SPACING, "Via4",           "Via4",           2.6, null),

		new DRCTemplate("25.3 Mosis", DRCTemplate.MOSIS,               DRCTemplate.VIASUR,   "Metal-4",         null,            1, "Metal-4-Metal-5-Con"),
        new DRCTemplate("25.3 TSMC", DRCTemplate.TSMC,               DRCTemplate.VIASUR,   "Metal-4",         null,            0.7, "Metal-4-Metal-5-Con"),

		new DRCTemplate("26.1", DRCTemplate.M5,    DRCTemplate.MINWID,   "Metal-5",         null,            4,  null),
		new DRCTemplate("26.1", DRCTemplate.M6,    DRCTemplate.MINWID,   "Metal-5",         null,            3,  null),

		new DRCTemplate("26.2", DRCTemplate.M5,    DRCTemplate.SPACING,  "Metal-5",        "Metal-5",        4,  null),
		new DRCTemplate("26.2", DRCTemplate.DE|DRCTemplate.M6,    DRCTemplate.SPACING,  "Metal-5",        "Metal-5",        4,  null),
		new DRCTemplate("26.2", DRCTemplate.SU|DRCTemplate.M6,    DRCTemplate.SPACING,  "Metal-5",        "Metal-5",        3,  null),

		new DRCTemplate("26.3", DRCTemplate.DE|DRCTemplate.M5,    DRCTemplate.VIASUR,   "Metal-5",         null,            2, "Metal-4-Metal-5-Con"),
		new DRCTemplate("26.3 Mosis", DRCTemplate.MOSIS|DRCTemplate.SU|DRCTemplate.M5,    DRCTemplate.VIASUR,   "Metal-5",         null,            1, "Metal-4-Metal-5-Con"),
		new DRCTemplate("26.3 TSMC", DRCTemplate.TSMC|DRCTemplate.SU|DRCTemplate.M5,    DRCTemplate.VIASUR,   "Metal-5",         null,            0.7, "Metal-4-Metal-5-Con"),
        new DRCTemplate("26.3 Mosis", DRCTemplate.MOSIS|DRCTemplate.M6, DRCTemplate.VIASUR,   "Metal-5",         null,            1, "Metal-4-Metal-5-Con"),
        new DRCTemplate("26.3 TSMC", DRCTemplate.TSMC|DRCTemplate.M6, DRCTemplate.VIASUR,   "Metal-5",         null,            0.7, "Metal-4-Metal-5-Con"),

		new DRCTemplate("26.4", DRCTemplate.M5, DRCTemplate.SPACINGW, WIDELIMIT, 0, "Metal-5",        "Metal-5",        8, -1),
		new DRCTemplate("26.4", DRCTemplate.DE|DRCTemplate.M6,    DRCTemplate.SPACINGW, WIDELIMIT, 0, "Metal-5",        "Metal-5",        8, -1),
		new DRCTemplate("26.4", DRCTemplate.SU|DRCTemplate.M6,    DRCTemplate.SPACINGW, WIDELIMIT, 0, "Metal-5",        "Metal-5",        6, -1),

		new DRCTemplate("29.1", DRCTemplate.DE, DRCTemplate.CUTSIZE,   null,             null,            4, "Metal-5-Metal-6-Con"),
		new DRCTemplate("29.1", DRCTemplate.DE, DRCTemplate.MINWID,   "Via5",            null,            4,  null),
		new DRCTemplate("29.1", DRCTemplate.DE, DRCTemplate.NODSIZ,    null,             null,            8, "Metal-5-Metal-6-Con"),
		new DRCTemplate("29.1 Mosis", DRCTemplate.MOSIS|DRCTemplate.SU, DRCTemplate.CUTSIZE,   null,             null,            3, "Metal-5-Metal-6-Con"),
        new DRCTemplate("29.1 TSMC", DRCTemplate.TSMC|DRCTemplate.SU, DRCTemplate.CUTSIZE,   null,             null,            3.6, "Metal-5-Metal-6-Con"),
		new DRCTemplate("29.1 Mosis", DRCTemplate.MOSIS|DRCTemplate.SU, DRCTemplate.MINWID,   "Via5",            null,            3,  null),
		new DRCTemplate("29.1 TSMC", DRCTemplate.TSMC|DRCTemplate.SU, DRCTemplate.MINWID,   "Via5",            null,            3.6,  null),
        new DRCTemplate("29.1", DRCTemplate.SU, DRCTemplate.NODSIZ,    null,             null,            5, "Metal-5-Metal-6-Con"),

		new DRCTemplate("29.2 Mosis", DRCTemplate.MOSIS, DRCTemplate.SPACING,  "Via5",           "Via5",           4,  null),
        new DRCTemplate("29.2 TSMC", DRCTemplate.TSMC, DRCTemplate.SPACING,  "Via5",           "Via5",           3.6,  null),

		new DRCTemplate("29.3 Mosis", DRCTemplate.MOSIS, DRCTemplate.VIASUR,   "Metal-5",         null,            1, "Metal-5-Metal-6-Con"),
        new DRCTemplate("29.3 TSMC", DRCTemplate.TSMC, DRCTemplate.VIASUR,   "Metal-5",         null,            0.9, "Metal-5-Metal-6-Con"),

		new DRCTemplate("30.1 Mosis", DRCTemplate.MOSIS, DRCTemplate.MINWID,   "Metal-6",         null,            5,  null),
        new DRCTemplate("30.1 TSMC", DRCTemplate.TSMC, DRCTemplate.MINWID,   "Metal-6",         null,            4.4,  null),

		new DRCTemplate("30.2 Mosis", DRCTemplate.MOSIS, DRCTemplate.SPACING,  "Metal-6",        "Metal-6",        5,  null),
        new DRCTemplate("30.2 TSMC (M6.S.1)", DRCTemplate.TSMC, DRCTemplate.SPACING,  "Metal-6",        "Metal-6",        4.6,  null),

		new DRCTemplate("30.3", DRCTemplate.DE, DRCTemplate.VIASUR,   "Metal-6",         null,            2, "Metal-5-Metal-6-Con"),
		new DRCTemplate("30.3 Mosis", DRCTemplate.MOSIS|DRCTemplate.SU, DRCTemplate.VIASUR,   "Metal-6",         null,            1, "Metal-5-Metal-6-Con"),
        new DRCTemplate("30.3 TSMC", DRCTemplate.TSMC|DRCTemplate.SU, DRCTemplate.VIASUR,   "Metal-6",         null,            0.9, "Metal-5-Metal-6-Con"),

		new DRCTemplate("30.4", DRCTemplate.ALL, DRCTemplate.SPACINGW, WIDELIMIT, 0, "Metal-6",        "Metal-6",        10, -1)
	};

	// -------------------- private and protected methods ------------------------

	private MoCMOS()
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

        setFactoryResolution(0.01); // value in lambdas

        foundries.add(new Foundry(Foundry.MOSIS_FOUNDRY, DRCTemplate.MOSIS));
        foundries.add(new Foundry("TSMC", DRCTemplate.TSMC));
        setFactorySelecedFound(Foundry.MOSIS_FOUNDRY);  // default

		//**************************************** LAYERS ****************************************
        Layer[] metalLayers = new Layer[6]; // 1 -> 6
		/** metal-1 layer */
		metalLayers[0] = Layer.newInstance(this, "Metal-1",
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_1, 96,209,255, 0.8,true,
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
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_4, 224,95,255, 0.7,true,
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
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_5, 247,251,20, 0.6,true,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 150,150,255, 0.5,true,
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
			new EGraphics(EGraphics.OUTLINEPAT, EGraphics.OUTLINEPAT, 0, 255,190,6, 0.4,true,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,255,255, 0.3,true,
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
		poly1_lay = Layer.newInstance(this, "Polysilicon-1",
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_2, 255,155,192, 0.5,true,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 255,190,6, 1.0,true,
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

        Layer[] activeLayers = new Layer[2];
		/** P active layer */
		activeLayers[P_TYPE] = Layer.newInstance(this, "P-Active",
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_3, 107,226,96, 0.5,true,
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
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_3, 107,226,96, 0.5,true,
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

        Layer[] selectLayers = new Layer[2];
		/** P Select layer */
		selectLayers[P_TYPE] = Layer.newInstance(this, "P-Select",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 255,255,0, 0.2,false,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 255,255,0, 0.2,false,
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

        Layer[] wellLayers = new Layer[2];

		/** P Well layer */
		wellLayers[P_TYPE] = Layer.newInstance(this, "P-Well",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 139,99,46, 0.2,false,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 139,99,46, 0.2,false,
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
		Layer polyCutLayer = Layer.newInstance(this, "Poly-Cut",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 100,100,100, 1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** active cut layer */
		Layer activeCut_lay = Layer.newInstance(this, "Active-Cut",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 100,100,100, 1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via1 layer */
		Layer via1_lay = Layer.newInstance(this, "Via1", 
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 180,180,180, 1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via2 layer */
		Layer via2_lay = Layer.newInstance(this, "Via2",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 180,180,180, 1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via3 layer */
		Layer via3_lay = Layer.newInstance(this, "Via3",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 180,180,180, 1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via4 layer */
		Layer via4_lay = Layer.newInstance(this, "Via4",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 180,180,180, 1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via5 layer */
		Layer via5_lay = Layer.newInstance(this, "Via5",
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 180,180,180, 1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** passivation layer */
		Layer passivation_lay = Layer.newInstance(this, "Passivation",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 100,100,100, 1.0,true,
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
		transistorPoly_lay = Layer.newInstance(this, "Transistor-Poly",
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_2, 255,155,192, 0.5,true,
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
			new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 0,0,0, 1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P act well layer */
		Layer pActiveWell_lay = Layer.newInstance(this, "P-Active-Well",
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_3, 107,226,96, 1.0,false,
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
		Layer silicideBlock_lay = Layer.newInstance(this, "Silicide-Block",
//			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 230,230,230, 1.0,false,
//			new int[] { 0x2222,   //   X   X   X   X
//						0x0000,   //
//						0x8888,   // X   X   X   X
//						0x0000,   //
//						0x2222,   //   X   X   X   X
//						0x0000,   //
//						0x8888,   // X   X   X   X
//						0x0000,   //
//						0x2222,   //   X   X   X   X
//						0x0000,   //
//						0x8888,   // X   X   X   X
//						0x0000,   //
//						0x2222,   //   X   X   X   X
//						0x0000,   //
//						0x8888,   // X   X   X   X
//						0x0000}));//
            new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, EGraphics.TRANSPARENT_2, 192,255,255, 0.5,true,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,0,0, 1.0,false,
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
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_1, 96,209,255, 0.8,true,
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
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_4, 224,95,255, 0.7,true,
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
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_5, 247,251,20, 0.6,true,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 150,150,255, 0.5,true,
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
			new EGraphics(EGraphics.OUTLINEPAT, EGraphics.OUTLINEPAT, 0, 255,190,6, 0.4,true,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,255,255, 0.3,true,
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
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_2, 255,155,192, 1.0,true,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 255,190,6, 1.0,true,
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
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_3, 107,226,96, 1.0,true,
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
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, EGraphics.TRANSPARENT_3, 107,226,96, 1.0,true,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 255,255,0, 1.0,false,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 255,255,0, 1.0,false,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 139,99,46, 1.0,false,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 139,99,46, 1.0,false,
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
			new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, 0, 255,0,0, 1.0,false,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		// The layer functions
		metalLayers[0].setFunction(Layer.Function.METAL1);									// Metal-1
		metalLayers[1].setFunction(Layer.Function.METAL2);									// Metal-2
		metalLayers[2].setFunction(Layer.Function.METAL3);									// Metal-3
		metalLayers[3].setFunction(Layer.Function.METAL4);									// Metal-4
		metalLayers[4].setFunction(Layer.Function.METAL5);									// Metal-5
		metalLayers[5].setFunction(Layer.Function.METAL6);									// Metal-6
		poly1_lay.setFunction(Layer.Function.POLY1);									// Polysilicon-1
		poly2_lay.setFunction(Layer.Function.POLY2);									// Polysilicon-2
		activeLayers[P_TYPE].setFunction(Layer.Function.DIFFP);									// P-Active
		activeLayers[N_TYPE].setFunction(Layer.Function.DIFFN);									// N-Active
		selectLayers[P_TYPE].setFunction(Layer.Function.IMPLANTP);								// P-Select
		selectLayers[N_TYPE].setFunction(Layer.Function.IMPLANTN);								// N-Select
		wellLayers[P_TYPE].setFunction(Layer.Function.WELLP);									// P-Well
		wellLayers[N_TYPE].setFunction(Layer.Function.WELLN);									// N-Well
		polyCutLayer.setFunction(Layer.Function.CONTACT1, Layer.Function.CONPOLY);		// Poly-Cut
		activeCut_lay.setFunction(Layer.Function.CONTACT1, Layer.Function.CONDIFF);		// Active-Cut
		via1_lay.setFunction(Layer.Function.CONTACT2, Layer.Function.CONMETAL);			// Via-1
		via2_lay.setFunction(Layer.Function.CONTACT3, Layer.Function.CONMETAL);			// Via-2
		via3_lay.setFunction(Layer.Function.CONTACT4, Layer.Function.CONMETAL);			// Via-3
		via4_lay.setFunction(Layer.Function.CONTACT5, Layer.Function.CONMETAL);			// Via-4
		via5_lay.setFunction(Layer.Function.CONTACT6, Layer.Function.CONMETAL);			// Via-5
		passivation_lay.setFunction(Layer.Function.OVERGLASS);							// Passivation
		transistorPoly_lay.setFunction(Layer.Function.GATE);							// Transistor-Poly
		polyCap_lay.setFunction(Layer.Function.CAP);									// Poly-Cap
		pActiveWell_lay.setFunction(Layer.Function.DIFFP);								// P-Active-Well
		silicideBlock_lay.setFunction(Layer.Function.ART);								// Silicide-Block
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
		poly1_lay.setFactoryCIFLayer("CPG");				// Polysilicon-1
		poly2_lay.setFactoryCIFLayer("CEL");				// Polysilicon-2
		activeLayers[P_TYPE].setFactoryCIFLayer("CAA");				// P-Active
		activeLayers[N_TYPE].setFactoryCIFLayer("CAA");				// N-Active
		selectLayers[P_TYPE].setFactoryCIFLayer("CSP");				// P-Select
		selectLayers[N_TYPE].setFactoryCIFLayer("CSN");				// N-Select
		wellLayers[P_TYPE].setFactoryCIFLayer("CWP");				// P-Well
		wellLayers[N_TYPE].setFactoryCIFLayer("CWN");				// N-Well
		polyCutLayer.setFactoryCIFLayer("CCC");				// Poly-Cut
		activeCut_lay.setFactoryCIFLayer("CCC");			// Active-Cut
		via1_lay.setFactoryCIFLayer("CVA");					// Via-1
		via2_lay.setFactoryCIFLayer("CVS");					// Via-2
		via3_lay.setFactoryCIFLayer("CVT");					// Via-3
		via4_lay.setFactoryCIFLayer("CVQ");					// Via-4
		via5_lay.setFactoryCIFLayer("CV5");					// Via-5
		passivation_lay.setFactoryCIFLayer("COG");			// Passivation
		transistorPoly_lay.setFactoryCIFLayer("CPG");		// Transistor-Poly
		polyCap_lay.setFactoryCIFLayer("CPC");				// Poly-Cap
		pActiveWell_lay.setFactoryCIFLayer("CAA");			// P-Active-Well
		silicideBlock_lay.setFactoryCIFLayer("CSB");		// Silicide-Block
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
		metalLayers[0].setFactoryGDSLayer("49, 80p, 80t", Foundry.MOSIS_FOUNDRY);				// Metal-1 Mosis
        metalLayers[0].setFactoryGDSLayer("16", Foundry.TSMC_FOUNDRY);				// Metal-1 TSMC
		metalLayers[1].setFactoryGDSLayer("51, 82p, 82t", Foundry.MOSIS_FOUNDRY);				// Metal-2
        metalLayers[1].setFactoryGDSLayer("18", Foundry.TSMC_FOUNDRY);				// Metal-2
		metalLayers[2].setFactoryGDSLayer("62, 93p, 93t", Foundry.MOSIS_FOUNDRY);				// Metal-3
        metalLayers[2].setFactoryGDSLayer("28", Foundry.TSMC_FOUNDRY);				// Metal-3
		metalLayers[3].setFactoryGDSLayer("31, 63p, 63t", Foundry.MOSIS_FOUNDRY);				// Metal-4
        metalLayers[3].setFactoryGDSLayer("31", Foundry.TSMC_FOUNDRY);
		metalLayers[4].setFactoryGDSLayer("33, 64p, 64t", Foundry.MOSIS_FOUNDRY);				// Metal-5
        metalLayers[4].setFactoryGDSLayer("33", Foundry.TSMC_FOUNDRY);				// Metal-5
		metalLayers[5].setFactoryGDSLayer("37, 68p, 68t", Foundry.MOSIS_FOUNDRY);				// Metal-6
        metalLayers[5].setFactoryGDSLayer("38", Foundry.TSMC_FOUNDRY);				// Metal-6
		poly1_lay.setFactoryGDSLayer("46", Foundry.MOSIS_FOUNDRY);					// Polysilicon-1
        poly1_lay.setFactoryGDSLayer("13", Foundry.TSMC_FOUNDRY);					// Polysilicon-1
		transistorPoly_lay.setFactoryGDSLayer("46", Foundry.MOSIS_FOUNDRY);		// Transistor-Poly
        transistorPoly_lay.setFactoryGDSLayer("13", Foundry.TSMC_FOUNDRY);		// Transistor-Poly
		poly2_lay.setFactoryGDSLayer("56", Foundry.MOSIS_FOUNDRY);					// Polysilicon-2
		activeLayers[P_TYPE].setFactoryGDSLayer("43", Foundry.MOSIS_FOUNDRY);				// P-Active
        activeLayers[P_TYPE].setFactoryGDSLayer("3", Foundry.TSMC_FOUNDRY);				// P-Active
		activeLayers[N_TYPE].setFactoryGDSLayer("43", Foundry.MOSIS_FOUNDRY);				// N-Active
        activeLayers[N_TYPE].setFactoryGDSLayer("3", Foundry.TSMC_FOUNDRY);				// N-Active
		pActiveWell_lay.setFactoryGDSLayer("43", Foundry.MOSIS_FOUNDRY);			// P-Active-Well
        pActiveWell_lay.setFactoryGDSLayer("3", Foundry.TSMC_FOUNDRY);			// P-Active-Well
		selectLayers[P_TYPE].setFactoryGDSLayer("44", Foundry.MOSIS_FOUNDRY);				// P-Select
        selectLayers[P_TYPE].setFactoryGDSLayer("7", Foundry.TSMC_FOUNDRY);				// P-Select
		selectLayers[N_TYPE].setFactoryGDSLayer("45", Foundry.MOSIS_FOUNDRY);				// N-Select
        selectLayers[N_TYPE].setFactoryGDSLayer("8", Foundry.TSMC_FOUNDRY);				// N-Select
		wellLayers[P_TYPE].setFactoryGDSLayer("41", Foundry.MOSIS_FOUNDRY);					// P-Well
        wellLayers[P_TYPE].setFactoryGDSLayer("41", Foundry.TSMC_FOUNDRY);					// P-Well
		wellLayers[N_TYPE].setFactoryGDSLayer("42", Foundry.MOSIS_FOUNDRY);					// N-Well
        wellLayers[N_TYPE].setFactoryGDSLayer("2", Foundry.TSMC_FOUNDRY);					// N-Well
		polyCutLayer.setFactoryGDSLayer("25", Foundry.MOSIS_FOUNDRY);				// Poly-Cut
        polyCutLayer.setFactoryGDSLayer("15", Foundry.TSMC_FOUNDRY);				// Poly-Cut
		activeCut_lay.setFactoryGDSLayer("25", Foundry.MOSIS_FOUNDRY);				// Active-Cut
		activeCut_lay.setFactoryGDSLayer("15", Foundry.TSMC_FOUNDRY);				// Active-Cut
		via1_lay.setFactoryGDSLayer("50", Foundry.MOSIS_FOUNDRY);					// Via-1
        via1_lay.setFactoryGDSLayer("17", Foundry.TSMC_FOUNDRY);					// Via-1
		via2_lay.setFactoryGDSLayer("61", Foundry.MOSIS_FOUNDRY);					// Via-2
        via2_lay.setFactoryGDSLayer("27", Foundry.TSMC_FOUNDRY);					// Via-2
		via3_lay.setFactoryGDSLayer("30", Foundry.MOSIS_FOUNDRY);					// Via-3
        via3_lay.setFactoryGDSLayer("29", Foundry.TSMC_FOUNDRY);					// Via-3
		via4_lay.setFactoryGDSLayer("32", Foundry.MOSIS_FOUNDRY);					// Via-4
        via4_lay.setFactoryGDSLayer("32", Foundry.TSMC_FOUNDRY);					// Via-4
		via5_lay.setFactoryGDSLayer("36", Foundry.MOSIS_FOUNDRY);					// Via-5
        via5_lay.setFactoryGDSLayer("39", Foundry.TSMC_FOUNDRY);					// Via-5
		passivation_lay.setFactoryGDSLayer("52", Foundry.MOSIS_FOUNDRY);			// Passivation
        passivation_lay.setFactoryGDSLayer("19", Foundry.TSMC_FOUNDRY);			// Passivation
		polyCap_lay.setFactoryGDSLayer("28", Foundry.MOSIS_FOUNDRY);				// Poly-Cap
        polyCap_lay.setFactoryGDSLayer("28", Foundry.TSMC_FOUNDRY);				// Poly-Cap
		silicideBlock_lay.setFactoryGDSLayer("29", Foundry.MOSIS_FOUNDRY);			// Silicide-Block
        silicideBlock_lay.setFactoryGDSLayer("34", Foundry.TSMC_FOUNDRY);			// Silicide-Block
		thickActive_lay.setFactoryGDSLayer("60", Foundry.MOSIS_FOUNDRY);			// Thick-Active
        thickActive_lay.setFactoryGDSLayer("4", Foundry.TSMC_FOUNDRY);			// Thick-Active
		pseudoMetal1_lay.setFactoryGDSLayer("", Foundry.MOSIS_FOUNDRY);			// Pseudo-Metal-1
		pseudoMetal2_lay.setFactoryGDSLayer("", Foundry.MOSIS_FOUNDRY);			// Pseudo-Metal-2
		pseudoMetal3_lay.setFactoryGDSLayer("", Foundry.MOSIS_FOUNDRY);			// Pseudo-Metal-3
		pseudoMetal4_lay.setFactoryGDSLayer("", Foundry.MOSIS_FOUNDRY);			// Pseudo-Metal-4
		pseudoMetal5_lay.setFactoryGDSLayer("", Foundry.MOSIS_FOUNDRY);			// Pseudo-Metal-5
		pseudoMetal6_lay.setFactoryGDSLayer("", Foundry.MOSIS_FOUNDRY);			// Pseudo-Metal-6
		pseudoPoly1_lay.setFactoryGDSLayer("", Foundry.MOSIS_FOUNDRY);				// Pseudo-Polysilicon-1
		pseudoPoly2_lay.setFactoryGDSLayer("", Foundry.MOSIS_FOUNDRY);				// Pseudo-Polysilicon-2
		pseudoPActive_lay.setFactoryGDSLayer("", Foundry.MOSIS_FOUNDRY);			// Pseudo-P-Active
		pseudoNActive_lay.setFactoryGDSLayer("", Foundry.MOSIS_FOUNDRY);			// Pseudo-N-Active
		pseudoPSelect_lay.setFactoryGDSLayer("", Foundry.MOSIS_FOUNDRY);			// Pseudo-P-Select
		pseudoNSelect_lay.setFactoryGDSLayer("", Foundry.MOSIS_FOUNDRY);			// Pseudo-N-Select
		pseudoPWell_lay.setFactoryGDSLayer("", Foundry.MOSIS_FOUNDRY);				// Pseudo-P-Well
		pseudoNWell_lay.setFactoryGDSLayer("", Foundry.MOSIS_FOUNDRY);				// Pseudo-N-Well
		padFrame_lay.setFactoryGDSLayer("26", Foundry.MOSIS_FOUNDRY);				// Pad-Frame
        padFrame_lay.setFactoryGDSLayer("26", Foundry.TSMC_FOUNDRY);				// Pad-Frame
		// The Skill names
		metalLayers[0].setFactorySkillLayer("metal1");			// Metal-1
		metalLayers[1].setFactorySkillLayer("metal2");			// Metal-2
		metalLayers[2].setFactorySkillLayer("metal3");			// Metal-3
		metalLayers[3].setFactorySkillLayer("metal4");			// Metal-4
		metalLayers[4].setFactorySkillLayer("metal5");			// Metal-5
		metalLayers[5].setFactorySkillLayer("metal6");			// Metal-6
		poly1_lay.setFactorySkillLayer("poly");				// Polysilicon-1
		poly2_lay.setFactorySkillLayer("");					// Polysilicon-2
		activeLayers[P_TYPE].setFactorySkillLayer("aa");				// P-Active
		activeLayers[N_TYPE].setFactorySkillLayer("aa");				// N-Active
		selectLayers[P_TYPE].setFactorySkillLayer("pplus");			// P-Select
		selectLayers[N_TYPE].setFactorySkillLayer("nplus");			// N-Select
		wellLayers[P_TYPE].setFactorySkillLayer("pwell");			// P-Well
		wellLayers[N_TYPE].setFactorySkillLayer("nwell");			// N-Well
		polyCutLayer.setFactorySkillLayer("pcont");			// Poly-Cut
		activeCut_lay.setFactorySkillLayer("acont");		// Active-Cut
		via1_lay.setFactorySkillLayer("via");				// Via-1
		via2_lay.setFactorySkillLayer("via2");				// Via-2
		via3_lay.setFactorySkillLayer("via3");				// Via-3
		via4_lay.setFactorySkillLayer("via4");				// Via-4
		via5_lay.setFactorySkillLayer("via5");				// Via-5
		passivation_lay.setFactorySkillLayer("glasscut");	// Passivation
		transistorPoly_lay.setFactorySkillLayer("poly");	// Transistor-Poly
		polyCap_lay.setFactorySkillLayer("");				// Poly-Cap
		pActiveWell_lay.setFactorySkillLayer("aa");			// P-Active-Well
		silicideBlock_lay.setFactorySkillLayer("");			// Silicide-Block
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
        pActiveWell_lay.setFactory3DInfo(0.85, BULK_LAYER + 2*DIFF_LAYER);			// P-Active-Well
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
		poly1_lay.setFactory3DInfo(PO_LAYER, FOX_LAYER + activeLayers[P_TYPE].getDepth());					// Polysilicon-1
		transistorPoly_lay.setFactory3DInfo(PO_LAYER, TOX_LAYER + activeLayers[P_TYPE].getDepth());			// Transistor-Poly
        poly2_lay.setFactory3DInfo(PO_LAYER, transistorPoly_lay.getDepth());					// Polysilicon-2 // on top of transistor layer?
		polyCap_lay.setFactory3DInfo(PO_LAYER, FOX_LAYER + activeLayers[P_TYPE].getDepth());				// Poly-Cap @TODO GVG Ask polyCap

		polyCutLayer.setFactory3DInfo(metalLayers[0].getDistance()-poly1_lay.getDepth(), poly1_lay.getDepth());				// Poly-Cut between poly and metal1
		activeCut_lay.setFactory3DInfo(metalLayers[0].getDistance()-activeLayers[N_TYPE].getDepth(), activeLayers[N_TYPE].getDepth());				// Active-Cut betweent active and metal1

		// Other layers
		passivation_lay.setFactory3DInfo(PASS_LAYER, metalLayers[5].getDepth());			// Passivation
		silicideBlock_lay.setFactory3DInfo(0, BULK_LAYER);			// Silicide-Block
		padFrame_lay.setFactory3DInfo(0, passivation_lay.getDepth());				// Pad-Frame

		pseudoPoly1_lay.setFactory3DInfo(0, poly1_lay.getDistance());			// Pseudo-Polysilicon-1
		pseudoPoly2_lay.setFactory3DInfo(0, poly2_lay.getDistance());			// Pseudo-Polysilicon-2
		pseudoPActive_lay.setFactory3DInfo(0, activeLayers[P_TYPE].getDistance());			// Pseudo-P-Active
		pseudoNActive_lay.setFactory3DInfo(0, activeLayers[N_TYPE].getDistance());			// Pseudo-N-Active
		pseudoPSelect_lay.setFactory3DInfo(0, selectLayers[P_TYPE].getDistance());			// Pseudo-P-Select
		pseudoNSelect_lay.setFactory3DInfo(0, selectLayers[N_TYPE].getDistance());			// Pseudo-N-Select
		pseudoPWell_lay.setFactory3DInfo(0, wellLayers[P_TYPE].getDistance());				// Pseudo-P-Well
		pseudoNWell_lay.setFactory3DInfo(0, wellLayers[N_TYPE].getDistance());				// Pseudo-N-Well

		// The Spice parasitics
		metalLayers[0].setFactoryParasitics(0.06, 0.07, 0);			// Metal-1
		metalLayers[1].setFactoryParasitics(0.06, 0.04, 0);			// Metal-2
		metalLayers[2].setFactoryParasitics(0.06, 0.04, 0);			// Metal-3
		metalLayers[3].setFactoryParasitics(0.03, 0.04, 0);			// Metal-4
		metalLayers[4].setFactoryParasitics(0.03, 0.04, 0);			// Metal-5
		metalLayers[5].setFactoryParasitics(0.03, 0.04, 0);			// Metal-6
		poly1_lay.setFactoryParasitics(2.5, 0.09, 0);			// Polysilicon-1
		poly2_lay.setFactoryParasitics(50.0, 1.0, 0);			// Polysilicon-2
		activeLayers[P_TYPE].setFactoryParasitics(2.5, 0.9, 0);			// P-Active
		activeLayers[N_TYPE].setFactoryParasitics(3.0, 0.9, 0);			// N-Active
		selectLayers[P_TYPE].setFactoryParasitics(0, 0, 0);				// P-Select
		selectLayers[N_TYPE].setFactoryParasitics(0, 0, 0);				// N-Select
		wellLayers[P_TYPE].setFactoryParasitics(0, 0, 0);				// P-Well
		wellLayers[N_TYPE].setFactoryParasitics(0, 0, 0);				// N-Well
		polyCutLayer.setFactoryParasitics(2.2, 0, 0);			// Poly-Cut
		activeCut_lay.setFactoryParasitics(2.5, 0, 0);			// Active-Cut
		via1_lay.setFactoryParasitics(1.0, 0, 0);				// Via-1
		via2_lay.setFactoryParasitics(0.9, 0, 0);				// Via-2
		via3_lay.setFactoryParasitics(0.8, 0, 0);				// Via-3
		via4_lay.setFactoryParasitics(0.8, 0, 0);				// Via-4
		via5_lay.setFactoryParasitics(0.8, 0, 0);				// Via-5
		passivation_lay.setFactoryParasitics(0, 0, 0);			// Passivation
		transistorPoly_lay.setFactoryParasitics(2.5, 0.09, 0);	// Transistor-Poly
		polyCap_lay.setFactoryParasitics(0, 0, 0);				// Poly-Cap
		pActiveWell_lay.setFactoryParasitics(0, 0, 0);			// P-Active-Well
		silicideBlock_lay.setFactoryParasitics(0, 0, 0);		// Silicide-Block
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

		setFactoryParasitics(50, 0.04);

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
			new Technology.ArcLayer(poly1_lay, 0, Poly.Type.FILLED)
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
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(7.5))
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
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(7.5))
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
				new Technology.NodeLayer(poly1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
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
				new Technology.NodeLayer(poly1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(5)),
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
            transistorPolyLayers[i] = new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(10)),
                new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(10))}, 1, 1, 2, 2);
            transistorPolyLLayers[i] = new Technology.NodeLayer(poly1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(10)),
                new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromTop(10))}, 1, 1, 2, 2);
            transistorPolyRLayers[i] = new Technology.NodeLayer(poly1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
                new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromBottom(10)),
                new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(10))}, 1, 1, 2, 2);
            transistorPolyCLayers[i] = new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
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
			thickTransistorNodes[i].setSpecialNode(); // For display purposes
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
				new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(5), EdgeV.fromBottom(12)),
					new Technology.TechPoint(EdgeH.fromRight(5), EdgeV.fromTop(12))}),
				new Technology.NodeLayer(wellLayers[N_TYPE], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(selectLayers[P_TYPE], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4)),
				new Technology.NodeLayer(activeCut_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromBottom(9.5)),
					new Technology.TechPoint(EdgeH.fromLeft(9.5), EdgeV.fromBottom(7.5))}),
				new Technology.NodeLayer(activeCut_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
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
				new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(5), EdgeV.fromBottom(12)),
					new Technology.TechPoint(EdgeH.fromRight(5), EdgeV.fromTop(12))}),
				new Technology.NodeLayer(wellLayers[P_TYPE], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(selectLayers[N_TYPE], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4)),
				new Technology.NodeLayer(activeCut_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromBottom(9.5)),
					new Technology.TechPoint(EdgeH.fromLeft(9.5), EdgeV.fromBottom(7.5))}),
				new Technology.NodeLayer(activeCut_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
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

		/** Metal-1-P-Well Contact */
		metalWellContactNodes[P_TYPE] = PrimitiveNode.newInstance("Metal-1-P-Well-Con", this, 17.0, 17.0, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6.5)),
				new Technology.NodeLayer(pActiveWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6)),
				new Technology.NodeLayer(wellLayers[P_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(selectLayers[P_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4)),
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(7.5))
			});
		metalWellContactNodes[P_TYPE].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalWellContactNodes[P_TYPE], new ArcProto[] {metalArcs[0], active_arc}, "metal-1-well", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(8), EdgeV.fromBottom(8), EdgeH.fromRight(8), EdgeV.fromTop(8))
			});
		metalWellContactNodes[P_TYPE].setFunction(PrimitiveNode.Function.WELL);
		metalWellContactNodes[P_TYPE].setSpecialType(PrimitiveNode.MULTICUT);
		metalWellContactNodes[P_TYPE].setSpecialValues(new double [] {2, 2, 1.5, 1.5, 3, 3});
		metalWellContactNodes[P_TYPE].setMinSize(17, 17, "4.2, 6.2, 7.3");

		/** Metal-1-N-Well Contact */
		metalWellContactNodes[N_TYPE] = PrimitiveNode.newInstance("Metal-1-N-Well-Con", this, 17.0, 17.0, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6.5)),
				new Technology.NodeLayer(activeLayers[N_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6)),
				new Technology.NodeLayer(wellLayers[N_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(selectLayers[N_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4)),
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(7.5))
			});
		metalWellContactNodes[N_TYPE].addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metalWellContactNodes[N_TYPE], new ArcProto[] {metalArcs[0], active_arc}, "metal-1-substrate", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(8), EdgeV.fromBottom(8), EdgeH.fromRight(8), EdgeV.fromTop(8))
			});
		metalWellContactNodes[N_TYPE].setFunction(PrimitiveNode.Function.SUBSTRATE);
		metalWellContactNodes[N_TYPE].setSpecialType(PrimitiveNode.MULTICUT);
		metalWellContactNodes[N_TYPE].setSpecialValues(new double [] {2, 2, 1.5, 1.5, 3, 3});
		metalWellContactNodes[N_TYPE].setMinSize(17, 17, "4.2, 6.2, 7.3");

        /********************** RPO Resistor-Node ********************************/
        double polySelectOffX = 1.8; /* NP.C.3 */
//        double rpoViaOffX = 2.2 /* RPO.C.2 */;
        double polyCutSize = 2.2; /* page 28 */
        double resistorViaOff = 1.0; /* page 28 */
        double resistorConW = polyCutSize + 2*resistorViaOff /*contact width viaW + 2*viaS = (2.2 + 2*1). It must be 1xN */;
        double resistorConH = resistorConW /*contact height*/;
        double resistorOffX = resistorConW + polySelectOffX + 1.2 /*0.22um-0.1 (poly extension in contact)*/;
        double resistorW = 20 + 2*resistorOffX;
        double resistorOffY = 2.2 /* RPO.C.2*/;
        double resistorH = 2*resistorOffY + resistorConH;

//        double resistorM1Off = 0.5 /*poly surround in poly contact */;
//        double resistorPolyOffX = 2.2; /* page 28 */
//        double resistorV2OffX = -polyCutSize - 0.7 + resistorPolyX + resistorConW;
//        double resistorPortOffX = resistorPolyX + (resistorConW-polyCutSize)/2; // for the port
        double resistorM1Off = resistorViaOff - 0.6; // 0.6 = M.E.2
        double resistorM1W = resistorConW-2*resistorM1Off;
        double resistorM1OffX = resistorM1Off + polySelectOffX;
        double resistorM1OffY = resistorM1Off + resistorOffY;
        double resistorV1OffX = resistorViaOff + polySelectOffX;
        double resistorV1OffY = resistorOffY + (resistorConH-polyCutSize)/2;

        rpoResistorNodes = new PrimitiveNode[2];

        for (int i = 0; i < rpoResistorNodes.length; i++)
        {
            rpoResistorNodes[i] = PrimitiveNode.newInstance(stdNames[i]+"-Poly-RPO-Resistor", this, resistorW, resistorH,
                    new SizeOffset(resistorOffX, resistorOffX, resistorOffY, resistorOffY),
				new Technology.NodeLayer []
				{
                    new Technology.NodeLayer(poly1_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
						new Technology.TechPoint(EdgeH.fromLeft(polySelectOffX), EdgeV.fromBottom(resistorOffY)),
						new Technology.TechPoint(EdgeH.fromRight(polySelectOffX), EdgeV.fromTop(resistorOffY))}),
                    new Technology.NodeLayer(silicideBlock_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
						new Technology.TechPoint(EdgeH.fromLeft(resistorOffX), EdgeV.makeBottomEdge()),
						new Technology.TechPoint(EdgeH.fromRight(resistorOffX), EdgeV.makeTopEdge())}),
                    new Technology.NodeLayer(selectLayers[i], -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
						new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(0.2)),
						new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(0.2))}),
                    // Left contact
                    new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
						new Technology.TechPoint(EdgeH.fromLeft(resistorM1OffX), EdgeV.fromBottom(resistorM1OffY)),
						new Technology.TechPoint(EdgeH.fromLeft(resistorM1OffX+resistorM1W), EdgeV.fromTop(resistorM1OffY))}),
                    // left via
                    new Technology.NodeLayer(polyCutLayer, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
						new Technology.TechPoint(EdgeH.fromLeft(resistorV1OffX), EdgeV.fromBottom(resistorV1OffY)),
						new Technology.TechPoint(EdgeH.fromLeft(resistorV1OffX+polyCutSize), EdgeV.fromBottom(resistorV1OffY+polyCutSize))}),
                    // Right contact
                    new Technology.NodeLayer(metalLayers[0], 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
						new Technology.TechPoint(EdgeH.fromRight(resistorM1OffX+resistorM1W), EdgeV.fromBottom(resistorM1OffY)),
						new Technology.TechPoint(EdgeH.fromRight(resistorM1OffX), EdgeV.fromTop(resistorM1OffY))}),
                    // right via
                    new Technology.NodeLayer(polyCutLayer, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
						new Technology.TechPoint(EdgeH.fromRight(resistorV1OffX+polyCutSize), EdgeV.fromBottom(resistorV1OffY)),
						new Technology.TechPoint(EdgeH.fromRight(resistorV1OffX), EdgeV.fromBottom(resistorV1OffY+polyCutSize))})
				});
            rpoResistorNodes[i].addPrimitivePorts(new PrimitivePort []
			{
                // left port
				PrimitivePort.newInstance(this, rpoResistorNodes[i], new ArcProto[] {poly1_arc, metalArcs[0]}, "left-rpo", 0,180, 0, PortCharacteristic.UNKNOWN,
                        EdgeH.fromLeft(resistorV1OffX), EdgeV.fromBottom(resistorV1OffY), EdgeH.fromLeft(resistorV1OffX+polyCutSize), EdgeV.fromTop(resistorV1OffY)),
                // right port
                PrimitivePort.newInstance(this, rpoResistorNodes[i], new ArcProto[] {poly1_arc, metalArcs[0]}, "right-rpo", 0,180, 1, PortCharacteristic.UNKNOWN,
                        EdgeH.fromRight(resistorV1OffX), EdgeV.fromBottom(resistorV1OffY), EdgeH.fromRight(resistorV1OffX+polyCutSize), EdgeV.fromTop(resistorV1OffY))
			});
            rpoResistorNodes[i].setFunction(PrimitiveNode.Function.PRESIST);
			rpoResistorNodes[i].setHoldsOutline();
			rpoResistorNodes[i].setSpecialType(PrimitiveNode.POLYGONAL);
        }

		/** Metal-1-Node */
		metal1Node_node = PrimitiveNode.newInstance("Metal-1-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[0], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal1Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1Node_node, new ArcProto[] {metalArcs[0]}, "metal-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal1Node_node.setFunction(PrimitiveNode.Function.NODE);
		metal1Node_node.setHoldsOutline();
		metal1Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-2-Node */
		metal2Node_node = PrimitiveNode.newInstance("Metal-2-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[1], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal2Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal2Node_node, new ArcProto[] {metalArcs[1]}, "metal-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal2Node_node.setFunction(PrimitiveNode.Function.NODE);
		metal2Node_node.setHoldsOutline();
		metal2Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-3-Node */
		metal3Node_node = PrimitiveNode.newInstance("Metal-3-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[2], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal3Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal3Node_node, new ArcProto[] {metalArcs[2]}, "metal-3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal3Node_node.setFunction(PrimitiveNode.Function.NODE);
		metal3Node_node.setHoldsOutline();
		metal3Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-4-Node */
		metal4Node_node = PrimitiveNode.newInstance("Metal-4-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[3], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal4Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal4Node_node, new ArcProto[] {metalArcs[3]}, "metal-4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal4Node_node.setFunction(PrimitiveNode.Function.NODE);
		metal4Node_node.setHoldsOutline();
		metal4Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-5-Node */
		metal5Node_node = PrimitiveNode.newInstance("Metal-5-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[4], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal5Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal5Node_node, new ArcProto[] {metalArcs[4]}, "metal-5", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal5Node_node.setFunction(PrimitiveNode.Function.NODE);
		metal5Node_node.setHoldsOutline();
		metal5Node_node.setSpecialType(PrimitiveNode.POLYGONAL);
		metal5Node_node.setNotUsed();

		/** Metal-6-Node */
		metal6Node_node = PrimitiveNode.newInstance("Metal-6-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metalLayers[5], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal6Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal6Node_node, new ArcProto[] {metalArcs[5]}, "metal-6", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal6Node_node.setFunction(PrimitiveNode.Function.NODE);
		metal6Node_node.setHoldsOutline();
		metal6Node_node.setSpecialType(PrimitiveNode.POLYGONAL);
		metal6Node_node.setNotUsed();

		/** Polysilicon-1-Node */
		poly1Node_node = PrimitiveNode.newInstance("Polysilicon-1-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(poly1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		poly1Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, poly1Node_node, new ArcProto[] {poly1_arc}, "polysilicon-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
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
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		poly2Node_node.setFunction(PrimitiveNode.Function.NODE);
		poly2Node_node.setHoldsOutline();
		poly2Node_node.setSpecialType(PrimitiveNode.POLYGONAL);
		poly2Node_node.setNotUsed();

		/** P-Active-Node */
		pActiveNode_node = PrimitiveNode.newInstance("P-Active-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(activeLayers[P_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		pActiveNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pActiveNode_node, new ArcProto[] {active_arc, activeArcs[P_TYPE], activeArcs[N_TYPE]}, "active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		pActiveNode_node.setFunction(PrimitiveNode.Function.NODE);
		pActiveNode_node.setHoldsOutline();
		pActiveNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** N-Active-Node */
		nActiveNode_node = PrimitiveNode.newInstance("N-Active-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(activeLayers[N_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		nActiveNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, nActiveNode_node, new ArcProto[] {active_arc, activeArcs[P_TYPE], activeArcs[N_TYPE]}, "active", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		nActiveNode_node.setFunction(PrimitiveNode.Function.NODE);
		nActiveNode_node.setHoldsOutline();
		nActiveNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** P-Select-Node */
		pSelectNode_node = PrimitiveNode.newInstance("P-Select-Node", this, 4.0, 4.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(selectLayers[P_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		pSelectNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pSelectNode_node, new ArcProto[0], "select", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pSelectNode_node.setFunction(PrimitiveNode.Function.NODE);
		pSelectNode_node.setHoldsOutline();
		pSelectNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** N-Select-Node */
		nSelectNode_node = PrimitiveNode.newInstance("N-Select-Node", this, 4.0, 4.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(selectLayers[N_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		nSelectNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, nSelectNode_node, new ArcProto[0], "select", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		nSelectNode_node.setFunction(PrimitiveNode.Function.NODE);
		nSelectNode_node.setHoldsOutline();
		nSelectNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

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
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
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
		via1Node_node = PrimitiveNode.newInstance("Via-1-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		via1Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, via1Node_node, new ArcProto[0], "via-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		via1Node_node.setFunction(PrimitiveNode.Function.NODE);
		via1Node_node.setHoldsOutline();
		via1Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-2-Node */
		via2Node_node = PrimitiveNode.newInstance("Via-2-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		via2Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, via2Node_node, new ArcProto[0], "via-2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		via2Node_node.setFunction(PrimitiveNode.Function.NODE);
		via2Node_node.setHoldsOutline();
		via2Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-3-Node */
		via3Node_node = PrimitiveNode.newInstance("Via-3-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		via3Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, via3Node_node, new ArcProto[0], "via-3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		via3Node_node.setFunction(PrimitiveNode.Function.NODE);
		via3Node_node.setHoldsOutline();
		via3Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Via-4-Node */
		via4Node_node = PrimitiveNode.newInstance("Via-4-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		via4Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, via4Node_node, new ArcProto[0], "via-4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		via4Node_node.setFunction(PrimitiveNode.Function.NODE);
		via4Node_node.setHoldsOutline();
		via4Node_node.setSpecialType(PrimitiveNode.POLYGONAL);
		via4Node_node.setNotUsed();

		/** Via-5-Node */
		via5Node_node = PrimitiveNode.newInstance("Via-5-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(via5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		via5Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, via5Node_node, new ArcProto[0], "via-5", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		via5Node_node.setFunction(PrimitiveNode.Function.NODE);
		via5Node_node.setHoldsOutline();
		via5Node_node.setSpecialType(PrimitiveNode.POLYGONAL);
		via5Node_node.setNotUsed();

		/** P-Well-Node */
		pWellNode_node = PrimitiveNode.newInstance("P-Well-Node", this, 12.0, 12.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(wellLayers[P_TYPE], 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		pWellNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pWellNode_node, new ArcProto[] {activeArcs[P_TYPE]}, "well", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
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
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
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
				new Technology.NodeLayer(pActiveWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
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
				new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		polyTransistorNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, polyTransistorNode_node, new ArcProto[] {poly1_arc}, "trans-poly-1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		polyTransistorNode_node.setFunction(PrimitiveNode.Function.NODE);
		polyTransistorNode_node.setHoldsOutline();
		polyTransistorNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Silicide-Block-Node */
		silicideBlockNode_node = PrimitiveNode.newInstance("Silicide-Block-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(silicideBlock_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
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
		metalLayers[0].setPureLayerNode(metal1Node_node);					// Metal-1
		metalLayers[1].setPureLayerNode(metal2Node_node);					// Metal-2
		metalLayers[2].setPureLayerNode(metal3Node_node);					// Metal-3
		metalLayers[3].setPureLayerNode(metal4Node_node);					// Metal-4
		metalLayers[4].setPureLayerNode(metal5Node_node);					// Metal-5
		metalLayers[5].setPureLayerNode(metal6Node_node);					// Metal-6
		poly1_lay.setPureLayerNode(poly1Node_node);						// Polysilicon-1
		poly2_lay.setPureLayerNode(poly2Node_node);						// Polysilicon-2
		activeLayers[P_TYPE].setPureLayerNode(pActiveNode_node);					// P-Active
		activeLayers[N_TYPE].setPureLayerNode(nActiveNode_node);					// N-Active
		selectLayers[P_TYPE].setPureLayerNode(pSelectNode_node);					// P-Select
		selectLayers[N_TYPE].setPureLayerNode(nSelectNode_node);					// N-Select
		wellLayers[P_TYPE].setPureLayerNode(pWellNode_node);						// P-Well
		wellLayers[N_TYPE].setPureLayerNode(nWellNode_node);						// N-Well
		polyCutLayer.setPureLayerNode(polyCutNode_node);					// Poly-Cut
		activeCut_lay.setPureLayerNode(activeCutNode_node);				// Active-Cut
		via1_lay.setPureLayerNode(via1Node_node);						// Via-1
		via2_lay.setPureLayerNode(via2Node_node);						// Via-2
		via3_lay.setPureLayerNode(via3Node_node);						// Via-3
		via4_lay.setPureLayerNode(via4Node_node);						// Via-4
		via5_lay.setPureLayerNode(via5Node_node);						// Via-5
		passivation_lay.setPureLayerNode(passivationNode_node);			// Passivation
		transistorPoly_lay.setPureLayerNode(polyTransistorNode_node);	// Transistor-Poly
		polyCap_lay.setPureLayerNode(polyCapNode_node);					// Poly-Cap
		pActiveWell_lay.setPureLayerNode(pActiveWellNode_node);			// P-Active-Well
		silicideBlock_lay.setPureLayerNode(silicideBlockNode_node);		// Silicide-Block
		thickActive_lay.setPureLayerNode(thickActiveNode_node);			// Thick-Active
		padFrame_lay.setPureLayerNode(padFrameNode_node);				// Pad-Frame

        // Information for palette
        int maxY = metalArcs.length + activeArcs.length + 1 /* poly*/ + 1 /* trans */ + 1 /*misc*/ + 1 /* well */;
        nodeGroups = new Object[maxY][3];
        int count = 0;
        String[] shortNames = {"p", "n"};
        List tmp;

        // Transistor nodes first
        for (int i = 0; i < transistorNodes.length; i++)
        {
            tmp = new ArrayList(2);
            String tmpVar = shortNames[i]+"Mos";
            tmp.add(makeNodeInst(transistorNodes[i], transistorNodes[i].getFunction(), 0, true, tmpVar, 9));
            tmp.add(makeNodeInst(thickTransistorNodes[i], thickTransistorNodes[i].getFunction(), 0, true, tmpVar, 9));
            tmp.add(makeNodeInst(scalableTransistorNodes[i], scalableTransistorNodes[i].getFunction(), 0, true, tmpVar, 9));            
            nodeGroups[count][i+1] = tmp;
        }

        // Well second
        count++;
        for (int i = 0; i < metalWellContactNodes.length; i++)
        {
            String tmpVar = shortNames[i]+"Well";
            nodeGroups[count][i+1] = makeNodeInst(metalWellContactNodes[i], metalWellContactNodes[i].getFunction(),
                    0, true, tmpVar, 5.5);
        }

        // RPO resistors
        for (int i = 0; i < rpoResistorNodes.length; i++)
        {
            String tmpVar = shortNames[i]+"R";
            tmp = new ArrayList(1);
            tmp.add(makeNodeInst(rpoResistorNodes[i], rpoResistorNodes[i].getFunction(), 0, true, tmpVar, 10));
            nodeGroups[i][0] = tmp;
        }

        // Active/Well first
        for (int i = 0; i < activeArcs.length; i++)
        {
            nodeGroups[++count][0] = activeArcs[i];
            nodeGroups[count][1] = activePinNodes[i];
            String tmpVar = shortNames[i]+"Act";
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

    private void resizeNodes()
    {
        String foundry = getSelectedFoundry();
        if (foundry.equals(Foundry.TSMC_FOUNDRY))
        {
            // Poly contacts
            Technology.NodeLayer node = metal1Poly1Contact_node.getLayers()[2]; // Cut
            node.setPoints(Technology.TechPoint.makeIndented(1.4));
            metal1Poly1Contact_node.setSpecialValues(new double [] {2.2, 2.2, 1.4, 1.4, 2.8, 2.8}); // 2.8 for 1xN and NxN cuts (CO.S.2).

            // Active contacts
            for (int i = 0; i < metalActiveContactNodes.length; i++)
            {
                node = metalActiveContactNodes[i].getLayers()[4]; // Cut
                node.setPoints(Technology.TechPoint.makeIndented(7.4));
                node = metalActiveContactNodes[i].getLayers()[3]; // Select
                node.setPoints(Technology.TechPoint.makeIndented(3.4)); // so it is 2.6 with respect to active
                metalActiveContactNodes[i].setSpecialValues(new double [] {2.2, 2.2, 1.4, 1.4, 2.8, 2.8});
            }

            // Well contacts
            for (int i = 0; i < metalWellContactNodes.length; i++)
            {
                node = metalWellContactNodes[i].getLayers()[4]; // Cut
                node.setPoints(Technology.TechPoint.makeIndented(7.4));
                node = metalWellContactNodes[i].getLayers()[2]; // Well 3 is not guarantee during construction!
                node.setPoints(Technology.TechPoint.makeIndented(3));
                metalWellContactNodes[i].setSpecialValues(new double [] {2.2, 2.2, 1.4, 1.4, 2.8, 2.8});
                 // to have 4.3 between well and active NP.E.4
            }

            // Via1 -> Via4
            double [] indentValues = {1.2, 1.7, 1.7, 2.2, 2.7};
            double [] cutValues = {0.6, 0.6, 0.6, 0.6, 0.9};  // 0.6 (VIAX.E.2) 0.9 VIA5.E.1
            double [] sizeValues = {2.6, 2.6, 2.6, 2.6, 3.6}; // 2.6 (VIAxS.1)
            double [] spaceValues = {2.6, 2.6, 2.6, 2.6, 3.6};   // it should be 3.5 instead of 3.6 but even number kept for symmetry

            for (int i = 0; i < metalContactNodes.length; i++)
            {
                node = metalContactNodes[i].getLayers()[2];
                node.setPoints(Technology.TechPoint.makeIndented(indentValues[i]));
                metalContactNodes[i].setSpecialValues(new double [] {sizeValues[i], sizeValues[i], cutValues[i], cutValues[i], spaceValues[i], spaceValues[i]});
            }

            // Transistors
            /* Poly -> 3.2 top/bottom extension */
            for (int i = 0; i < 2; i++)
            {
                transistorWellLayers[i].getLeftEdge().setAdder(1.7); transistorWellLayers[i].getRightEdge().setAdder(-1.7);
                transistorSelectLayers[i].getLeftEdge().setAdder(2.5); transistorSelectLayers[i].getRightEdge().setAdder(-2.5);
                // Poly X values
                transistorPolyLayers[i].getLeftEdge().setAdder(3.8); transistorPolyLayers[i].getRightEdge().setAdder(-3.8);
                transistorPolyLLayers[i].getLeftEdge().setAdder(3.8);
                transistorPolyRLayers[i].getRightEdge().setAdder(-3.8);
                // Poly Y values
                transistorPolyLayers[i].getBottomEdge().setAdder(10.1); transistorPolyLayers[i].getTopEdge().setAdder(-10.1);
                transistorPolyLLayers[i].getBottomEdge().setAdder(10.1); transistorPolyLLayers[i].getTopEdge().setAdder(-10.1);
                transistorPolyRLayers[i].getBottomEdge().setAdder(10.1); transistorPolyRLayers[i].getTopEdge().setAdder(-10.1);
                transistorPolyCLayers[i].getBottomEdge().setAdder(10.1); transistorPolyCLayers[i].getTopEdge().setAdder(-10.1);
                transistorActiveLayers[i].getBottomEdge().setAdder(6.9); transistorActiveLayers[i].getTopEdge().setAdder(-6.9);
                transistorActiveBLayers[i].getBottomEdge().setAdder(6.9);
                transistorActiveTLayers[i].getTopEdge().setAdder(-6.9);
                transistorNodes[i].setSizeOffset(new SizeOffset(6, 6, 10.1, 10.1));
//                PrimitivePort port = transistorNodes[i].getPort(0).getBasePort();
//                // trans-poly-left
//                port.getLeft().setAdder(4.2);
//                port.getRight().setAdder(4.2);
//                // trans-poly-right
//                port = transistorNodes[i].getPort(2).getBasePort();
//                port.getLeft().setAdder(-4.2);
//                port.getRight().setAdder(-4.2);
            }
            // Channel length 1.8
            poly1_arc.setDefaultWidth(1.8);
            poly1Pin_node.setDefSize(1.8, 1.8);
            // Metal 6 arc width 4.4
            metalArcs[5].setDefaultWidth(5);
        }
        else // Mosis
        {
            Technology.NodeLayer node = metal1Poly1Contact_node.getLayers()[2]; // Cut
            node.setPoints(Technology.TechPoint.makeIndented(1.5));
            metal1Poly1Contact_node.setSpecialValues(new double [] {2, 2, 1.5, 1.5, 3, 3});

            // Active contacts
            for (int i = 0; i < metalActiveContactNodes.length; i++)
            {
                node = metalActiveContactNodes[i].getLayers()[4]; // Cut
                node.setPoints(Technology.TechPoint.makeIndented(7.5));
                node = metalActiveContactNodes[i].getLayers()[3]; // Select
                node.setPoints(Technology.TechPoint.makeIndented(4)); // back to Mosis default=4
                metalActiveContactNodes[i].setSpecialValues(new double [] {2, 2, 1.5, 1.5, 3, 3});
            }

            // Well contacts
            for (int i = 0; i < metalWellContactNodes.length; i++)
            {
                node = metalWellContactNodes[i].getLayers()[4]; // Cut
                node.setPoints(Technology.TechPoint.makeIndented(7.5));
                metalWellContactNodes[i].setSpecialValues(new double [] {2, 2, 1.5, 1.5, 3, 3});
                node = metalWellContactNodes[i].getLayers()[2]; // Well
                node.setPoints(Technology.TechPoint.makeIndented(3));
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
                transistorActiveLayers[i].getBottomEdge().setAdder(7); transistorActiveLayers[i].getTopEdge().setAdder(-7);
                transistorActiveBLayers[i].getBottomEdge().setAdder(7);
                transistorActiveTLayers[i].getTopEdge().setAdder(-7);
                transistorNodes[i].setSizeOffset(new SizeOffset(6, 6, 10, 10));
//                PrimitivePort port = transistorNodes[i].getPort(0).getBasePort();
//                // trans-poly-left
//                port.getLeft().setAdder(4);
//                port.getRight().setAdder(4);
//                // trans-poly-right
//                port = transistorNodes[i].getPort(2).getBasePort();
//                port.getLeft().setAdder(-4);
//                port.getRight().setAdder(-4);
            }
            // Channel length 2
            poly1_arc.setDefaultWidth(2.0);
            poly1Pin_node.setDefSize(2, 2);
            // Metal 6 arc width 4. Original value
            metalArcs[5].setDefaultWidth(4);
        }
    }

    /**
     * Reset default width values. In 180nm the poly arcs are resized to default values
     * @param cell
     */
    public void resetDefaultValues(Cell cell)
    {
//        for (Iterator itNod = cell.getNodes(); itNod.hasNext(); )
//        {
//            NodeInst ni = (NodeInst)itNod.next();
//
            // Only valid for transistors in layout so VarContext=null is OK
//            TransistorSize size = ni.getTransistorSize(null);
//            if (size != null)
//            {
//                double length = size.getDoubleLength();
//                int mult = (int)(length / poly1_arc.getWidth());
//                if (mult < 1)
//                    System.out.println("Problems resizing transistor lengths");
//                double newLen = poly1_arc.getWidth() * mult;
//                if (!DBMath.areEquals(newLen,length))
//                {
//                    // Making wires from top/bottom ports fixed-angle otherwise they get twisted.
//                    PortInst pi = ni.getPortInst(1);
//                    List list = new ArrayList(2);
//                    // Not sure how many connections are so fix angles to all
//                    for (Iterator it = pi.getConnections(); it.hasNext();)
//                    {
//                        Connection c = (Connection)it.next();
//                        c.getArc().setFixedAngle(true);
//                        list.add(c.getArc());
//                    }
//                    pi = ni.getPortInst(3);
//                    // Not sure how many connections are so fix angles to all
//                    for (Iterator it = pi.getConnections(); it.hasNext();)
//                    {
//                        Connection c = (Connection)it.next();
//                        c.getArc().setFixedAngle(true);
//                        list.add(c.getArc());
//                    }
////                    ni.setPrimitiveNodeSize(size.getDoubleWidth(), newLen);
////                    for (int i = 0; i < list.size(); i++)
////                    {
////                        ArcInst arc = (ArcInst)list.get(i);
//////                        arc.setFixedAngle(false);
////                    }
//                }
//            }
//        }

//        for(Iterator itArc = cell.getArcs(); itArc.hasNext(); )
//        {
//            ArcInst ai = (ArcInst)itArc.next();
//            boolean found = false;
//            double maxLen = -Double.MIN_VALUE;
//            // Guessing arc thickness based on connections
//            // Default doesn't work in existing cells
//            // Valid for poly layers and M6 only!
//            // Metal 6 because the min M6 changed in Mosis
//
//            if (!(ai.getProto().getFunction().isPoly() ||
//                  ai.getProto().getFunction() == ArcProto.Function.METAL6)) continue;
////            for(int i=0; i<2; i++)
////            {
////                Connection thisCon = ai.getConnection(i);
////                NodeInst ni = thisCon.getPortInst().getNodeInst();
////                // covers transistors and resistors
////                PrimitiveNodeSize size = ni.getPrimitiveNodeSize(null); // This is only for layout
////                if (size != null)
////                {
////                    double length = size.getDoubleLength();
////                    if (DBMath.isGreaterThan(length, maxLen))
////                    {
////                        maxLen = length;
////                        found = true;
////                    }
////                }
////            }
//            // No transistor or resistor found
//            if (!found)
//            {
//                maxLen = ai.getProto().getDefaultWidth();
//                found = (!DBMath.areEquals(ai.getWidth(), maxLen)); // default must be applied
//            }
//            if (found)
//            {
//                ai.modify(maxLen - ai.getWidth(), 0, 0, 0, 0);
//            }
//        }
    }

	/******************** SUPPORT METHODS ********************/

    /**
	 * Method to set the technology to state "newstate", which encodes the number of metal
	 * layers, whether it is a deep process, and other rules.
	 */
	public void setState()
	{
		// set rules
        cachedRules = getFactoryDesignRules();

		// disable Metal-3/4/5/6-Pin, Metal-2/3/4/5-Metal-3/4/5/6-Con, Metal-3/4/5/6-Node, Via-2/3/4/5-Node
		metalPinNodes[2].setNotUsed();
		metalPinNodes[3].setNotUsed();
		metalPinNodes[4].setNotUsed();
		metalPinNodes[5].setNotUsed();
		metalContactNodes[1].setNotUsed();
		metalContactNodes[2].setNotUsed();
		metalContactNodes[3].setNotUsed();
		metalContactNodes[4].setNotUsed();
		metal3Node_node.setNotUsed();
		metal4Node_node.setNotUsed();
		metal5Node_node.setNotUsed();
		metal6Node_node.setNotUsed();
		via2Node_node.setNotUsed();
		via3Node_node.setNotUsed();
		via4Node_node.setNotUsed();
		via5Node_node.setNotUsed();

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
				metal6Node_node.clearNotUsed();
				via5Node_node.clearNotUsed();
				metalArcs[5].clearNotUsed();
				// FALLTHROUGH 
			case 5:
				metalPinNodes[4].clearNotUsed();
				metalContactNodes[3].clearNotUsed();
				metal5Node_node.clearNotUsed();
				via4Node_node.clearNotUsed();
				metalArcs[4].clearNotUsed();
				// FALLTHROUGH 
			case 4:
				metalPinNodes[3].clearNotUsed();
				metalContactNodes[2].clearNotUsed();
				metal4Node_node.clearNotUsed();
				via3Node_node.clearNotUsed();
				metalArcs[3].clearNotUsed();
				// FALLTHROUGH 
			case 3:
				metalPinNodes[2].clearNotUsed();
				metalContactNodes[1].clearNotUsed();
				metal3Node_node.clearNotUsed();
				via2Node_node.clearNotUsed();
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
	public Poly [] getShapeOfNode(NodeInst ni, EditWindow wnd, VarContext context, boolean electrical, boolean reasonable,
		Technology.NodeLayer [] primLayers, Layer layerOverride)
	{
		NodeProto prototype = ni.getProto();
		if (prototype == scalableTransistorNodes[P_TYPE] || prototype == scalableTransistorNodes[N_TYPE])
            return getShapeOfNodeScalable(ni, wnd, context, reasonable);
        else if (prototype == rpoResistorNodes[P_TYPE] || prototype == rpoResistorNodes[N_TYPE])
            return getShapeOfNodeResistor(ni, wnd, context, electrical, reasonable);

        // Default
        return super.getShapeOfNode(ni, wnd, context, electrical, reasonable, primLayers, layerOverride);
    }

    /**
     * Special getShapeOfNode function for RPO poly resistors
     * @param ni
     * @param wnd
     * @param context
     * @param electrical
     * @param reasonable
     * @return
     */
    private Poly [] getShapeOfNodeResistor(NodeInst ni, EditWindow wnd, VarContext context, boolean electrical,
                                           boolean reasonable)
    {
        // now compute the number of polygons
        PrimitiveNode np = (PrimitiveNode)ni.getProto();
		Technology.NodeLayer [] layers = np.getLayers();
        EdgeV polyV = null;
        EdgeH polyH = null, rpoH = null;

        // Number of cuts only varies along Y
        for (int i = 0; i < layers.length; i++)
        {
            TechPoint [] points = layers[i].getPoints();
            if (layers[i].getLayer().getFunction() == Layer.Function.POLY1) // found the poly layer
            {
                polyH = points[1].getX();
                polyV = points[1].getY();
            }
            else if (layers[i].getLayer().getFunction() == Layer.Function.ART) // RPO is defined as ART
            {
                rpoH = points[0].getX();
            }
        }
        if (polyV == null || polyH == null || rpoH == null)
            throw new Error("Error finding poly/rpo layers in " + rpoResistorNodes);

        double polyOffX = Math.abs(polyH.getAdder());
        double polyOffY = Math.abs(polyV.getAdder());
        double polyConX = 4.2; // 2.2 (via) + 2 * 1. GPage 28
        // same offset as in poly contact,
        // SizeOffset is zero for that primitive and special values are calculated from external boundary
        MultiCutData cutData = new MultiCutData(polyConX, ni.getYSize() - 2*polyOffY, 0, 0, 0, 0, 0, 0,
                metal1Poly1Contact_node.getSpecialValues());
        if (cutData.numCutsX() != 1)
            throw new Error("Error: onw cut should be the only value");
		Technology.NodeLayer [] newNodeLayers = layers;

        List list = new ArrayList(layers.length);
        Technology.NodeLayer cutTemplate = null;
        // Copy layers that are not poly cuts
        for (int i = 0; i < layers.length; i++)
        {
            if (layers[i].getLayer().getFunction() == Layer.Function.CONTACT1) // found the poly layer
            {
                cutTemplate = layers[i];
                continue;
            }
            list.add(new Technology.NodeLayer(layers[i]));
        }

        if (cutTemplate == null)
            throw new Error("No cut template found for " + rpoResistorNodes);

        // creating the cuts now
        double anchorX = polyConX/2;
        double anchorY = (ni.getYSize() - 2*polyOffY)/2;
        for (int i = 0; i < cutData.numCuts(); i++)
        {
            Poly poly = cutData.fillCutPoly(0, 0, i);
            Rectangle2D rect = poly.getBounds2D();
            double offsetX = anchorX+rect.getMinX()+polyOffX;
            double offsetY = anchorY+rect.getMinY()+polyOffY;

            // left cuts
            Technology.NodeLayer cut = new Technology.NodeLayer(cutTemplate);
            TechPoint [] points = cut.getPoints();
            points[0] = new Technology.TechPoint(EdgeH.fromLeft(offsetX),
                    EdgeV.fromBottom(offsetY));
            points[1] = new Technology.TechPoint(EdgeH.fromLeft(offsetX+cutData.getCutSizeX()),
                    EdgeV.fromBottom(offsetY+cutData.getCutSizeY()));
            list.add(cut);

            //right cuts
            cut = new Technology.NodeLayer(cutTemplate);
            points = cut.getPoints();
            points[0] = new Technology.TechPoint(EdgeH.fromRight(offsetX),
                    EdgeV.fromBottom(offsetY));
            points[1] = new Technology.TechPoint(EdgeH.fromRight(offsetX+cutData.getCutSizeX()),
                    EdgeV.fromBottom(offsetY+cutData.getCutSizeY()));
            list.add(cut);
        }
        newNodeLayers = new Technology.NodeLayer[list.size()];
        System.arraycopy(list.toArray(), 0, newNodeLayers, 0, list.size());

        // now let the superclass convert it to Polys
		return super.getShapeOfNode(ni, wnd, context, electrical, reasonable, newNodeLayers, null);
    }

    /**
     * Special getShapeOfNode function for scalable transistors
     * @param ni
     * @param wnd
     * @param context
     * @param reasonable
     * @return
     */
    private Poly [] getShapeOfNodeScalable(NodeInst ni, EditWindow wnd, VarContext context, boolean reasonable)
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
			try
			{
				Object o = evalContext.evalVarRecurse(var, ni);
				if (o != null) extra = o.toString();
			} catch (VarContext.EvalException e) {}
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
		double polyInset = actInset - 2;
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
	public DRCRules getFactoryDesignRules()
	{
		MOSRules rules = new MOSRules(this);

        // doesn't load rules that
        int foundryMode = getFoundry();

        // Resize primitives according to the foundry
        resizeNodes();

		// load the DRC tables from the explanation table
		rules.wideLimit = new Double(WIDELIMIT);
		for(int pass=0; pass<2; pass++)
		{
			for(int i=0; i < theRules.length; i++)
			{
				// see if the rule applies
				if (pass == 0)
				{
					if (theRules[i].ruleType == DRCTemplate.NODSIZ) continue;
				} else
				{
					if (theRules[i].ruleType != DRCTemplate.NODSIZ) continue;
				}

				int when = theRules[i].when;
                if (when != DRCTemplate.ALL)
                {
                    // One of the 2 is present. Absence means rule is valid for both

                    if ((when&DRCTemplate.MOSIS) != 0 && foundryMode == DRCTemplate.TSMC)
                        continue;
                    else if ((when&DRCTemplate.TSMC) != 0 && foundryMode == DRCTemplate.MOSIS)
                        continue; // skipping this rule
                }

                boolean goodrule = true;
				if ((when&(DRCTemplate.DE|DRCTemplate.SU|DRCTemplate.SC)) != 0)
				{
					switch (getRuleSet())
					{
						case DEEPRULES:  if ((when&DRCTemplate.DE) == 0) goodrule = false;   break;
						case SUBMRULES:  if ((when&DRCTemplate.SU) == 0) goodrule = false;   break;
						case SCMOSRULES: if ((when&DRCTemplate.SC) == 0) goodrule = false;   break;
					}
					if (!goodrule) continue;
				}
				if ((when&(DRCTemplate.M2|DRCTemplate.M3|DRCTemplate.M4|DRCTemplate.M5|DRCTemplate.M6)) != 0)
				{
					switch (getNumMetal())
					{
						case 2:  if ((when&DRCTemplate.M2) == 0) goodrule = false;   break;
						case 3:  if ((when&DRCTemplate.M3) == 0) goodrule = false;   break;
						case 4:  if ((when&DRCTemplate.M4) == 0) goodrule = false;   break;
						case 5:  if ((when&DRCTemplate.M5) == 0) goodrule = false;   break;
						case 6:  if ((when&DRCTemplate.M6) == 0) goodrule = false;   break;
					}
					if (!goodrule) continue;
				}
				if ((when&DRCTemplate.AC) != 0)
				{
					if (!isAlternateActivePolyRules()) continue;
				}
				if ((when&DRCTemplate.NAC) != 0)
				{
					if (isAlternateActivePolyRules()) continue;
				}
				if ((when&DRCTemplate.SV) != 0)
				{
					if (isDisallowStackedVias()) continue;
				}
				if ((when&DRCTemplate.NSV) != 0)
				{
					if (!isDisallowStackedVias()) continue;
				}

				// find the layer names
				Layer lay1 = null;
				int layert1 = -1;
				if (theRules[i].name1 != null)
				{
					lay1 = findLayer(theRules[i].name1);
					if (lay1 == null)
					{
						System.out.println("Warning: no layer '" + theRules[i].name1 + "' in mocmos technology");
						return null;
					}
					layert1 = lay1.getIndex();
				}
				Layer lay2 = null;
				int layert2 = -1;
				if (theRules[i].name2 != null)
				{
					lay2 = findLayer(theRules[i].name2);
					if (lay2 == null)
					{
						System.out.println("Warning: no layer '" + theRules[i].name2 + "' in mocmos technology");
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
				if (theRules[i].nodeName != null)
				{
					if (theRules[i].ruleType == DRCTemplate.ASURROUND)
					{
						aty = this.findArcProto(theRules[i].nodeName);
						if (aty == null)
						{
							System.out.println("Warning: no arc '" + theRules[i].nodeName + "' in mocmos technology");
							return null;
						}
					} else
					{
						nty = this.findNodeProto(theRules[i].nodeName);
						if (nty == null)
						{
							System.out.println("Warning: no node '" + theRules[i].nodeName + "' in mocmos technology");
							return null;
						}
					}
				}

				// get more information about the rule
				double distance = theRules[i].value1;
				String proc = "";
				if ((when&(DRCTemplate.DE|DRCTemplate.SU|DRCTemplate.SC)) != 0)
				{
					switch (getRuleSet())
					{
						case DEEPRULES:  proc = "DEEP";   break;
						case SUBMRULES:  proc = "SUBM";   break;
						case SCMOSRULES: proc = "SCMOS";  break;
					}
				}
				String metal = "";
				if ((when&(DRCTemplate.M2|DRCTemplate.M3|DRCTemplate.M4|DRCTemplate.M5|DRCTemplate.M6)) != 0)
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
				String rule = theRules[i].ruleName;
				String extraString = metal + proc;
				if (extraString.length() > 0 && rule.indexOf(extraString) == -1)
					rule += ", " +  extraString;
				theRules[i].ruleName = new String(rule);

				// set the rule
				double [] specValues;
				switch (theRules[i].ruleType)
				{
					case DRCTemplate.MINWID:
						if (Main.getDebug() && layert1 >= 0 && layert2 >= 0)
							System.out.println("Error in swap old tech");
						rules.minWidth[layert1] = new Double(distance);
						rules.minWidthRules[layert1] = rule;
						setLayerMinWidth(theRules[i].name1, theRules[i].ruleName, distance);
						break;
					case DRCTemplate.NODSIZ:
						setDefNodeSize(nty, theRules[i].ruleName, distance, distance, rules);
						break;
					case DRCTemplate.SURROUND:
						setLayerSurroundLayer(theRules[i].ruleName, nty, lay1, lay2, distance,
						        rules.minWidth[lay1.getIndex()].doubleValue());
						break;
					case DRCTemplate.ASURROUND:
						setArcLayerSurroundLayer(aty, lay1, lay2, distance);
						break;
					case DRCTemplate.VIASUR:
						setLayerSurroundVia(nty, lay1, distance);
						specValues = nty.getSpecialValues();
						specValues[2] = distance;
						specValues[3] = distance;
						break;
					case DRCTemplate.TRAWELL:
						setTransistorWellSurround(distance);
						break;
					case DRCTemplate.TRAPOLY:
						setTransistorPolyOverhang(distance);
						break;
					case DRCTemplate.TRAACTIVE:
						setTransistorActiveOverhang(distance);
						break;
					case DRCTemplate.SPACING:
						rules.conList[index] = new Double(distance);
						rules.unConList[index] = new Double(distance);
						rules.conListRules[index] = rule;
						rules.unConListRules[index] = rule;
						break;
					case DRCTemplate.SPACINGM:
						rules.conListMulti[index] = new Double(distance);
						rules.unConListMulti[index] = new Double(distance);
						rules.conListMultiRules[index] = rule;
						rules.unConListMultiRules[index] = rule;
						break;
					case DRCTemplate.SPACINGW:
						rules.conListWide[index] = new Double(distance);
						rules.unConListWide[index] = new Double(distance);
						rules.conListWideRules[index] = rule;
						rules.unConListWideRules[index] = rule;
						break;
					case DRCTemplate.SPACINGE:
						rules.edgeList[index] = new Double(distance);
						rules.edgeListRules[index] = rule;
						break;
					case DRCTemplate.CONSPA:
						rules.conList[index] = new Double(distance);
						rules.conListRules[index] = rule;
						break;
					case DRCTemplate.UCONSPA:
						rules.unConList[index] = new Double(distance);
						rules.unConListRules[index] = rule;
						break;
					case DRCTemplate.CUTSPA:
						specValues = nty.getSpecialValues();
						specValues[4] = distance;
						specValues[5] = distance;
						break;
					case DRCTemplate.CUTSIZE:
						specValues = nty.getSpecialValues();
						specValues[0] = specValues[1] = distance;
                        int nodeIndex = nty.getPrimNodeIndexInTech();
                        rules.cutNodeSize[nodeIndex] = new Double(distance);
                        rules.cutNodeSizeRules[nodeIndex] = theRules[i].ruleName;
						break;
					case DRCTemplate.CUTSUR:
						specValues = nty.getSpecialValues();
						specValues[2] = distance;
						specValues[3] = distance;
						break;
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
		for(Iterator it = tech.getNodes(); it.hasNext(); )
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
		for(Iterator it = getNodes(); it.hasNext(); )
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
		if (layer1 == poly1_lay && layer2 == transistorPoly_lay) return true;
		if (layer2 == poly1_lay && layer1 == transistorPoly_lay) return true;
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

    private static Pref cacheNumberOfMetalLayers = TechPref.makeIntPref(tech, "MoCMOSNumberOfMetalLayers", getTechnologyPreferences(), 4);
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
	 * Method to tell the current rule set for this Technology.
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
	public boolean convertOldVariable(String varName, Object value)
	{
		if (!varName.equalsIgnoreCase(TECH_LAST_STATE.getName())) return false;
		if (!(value instanceof Integer)) return false;
		int oldBits = ((Integer)value).intValue();

		boolean oldNoStackedVias = (oldBits&MOCMOSNOSTACKEDVIAS) != 0;
		Pref.changedMeaningVariable(cacheDisallowStackedVias.getMeaning(), new Integer(oldNoStackedVias?1:0));

		int numMetals = 0;
		switch (oldBits&MOCMOSMETALS)
		{
			case MOCMOS2METAL: numMetals = 2;   break;
			case MOCMOS3METAL: numMetals = 3;   break;
			case MOCMOS4METAL: numMetals = 4;   break;
			case MOCMOS5METAL: numMetals = 5;   break;
			case MOCMOS6METAL: numMetals = 6;   break;
		}
		Pref.changedMeaningVariable(cacheNumberOfMetalLayers.getMeaning(), new Integer(numMetals));

		int ruleSet = 0;
		switch (oldBits&MOCMOSRULESET)
		{
			case MOCMOSSUBMRULES:  ruleSet = SUBMRULES;   break;
			case MOCMOSDEEPRULES:  ruleSet = DEEPRULES;   break;
			case MOCMOSSCMOSRULES: ruleSet = SCMOSRULES;  break;
		}
		Pref.changedMeaningVariable(cacheRuleSet.getMeaning(), new Integer(ruleSet));

		boolean alternateContactRules = (oldBits&MOCMOSALTAPRULES) != 0;
		Pref.changedMeaningVariable(cacheAlternateActivePolyRules.getMeaning(), new Integer(alternateContactRules?1:0));

		boolean secondPoly = (oldBits&MOCMOSTWOPOLY) != 0;
		Pref.changedMeaningVariable(cacheSecondPolysilicon.getMeaning(), new Integer(secondPoly?1:0));
		return true;
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
		if (hasChanged && Main.LOCALDEBUGFLAG) System.out.println(errorMessage);
	}
}
