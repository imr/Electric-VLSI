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

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.Color;
import java.util.prefs.Preferences;

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

	// layers
	private Layer poly1_lay, transistorPoly_lay;

	// arcs
	/** metal 1 arc */							private PrimitiveArc metal1_arc;
	/** metal 2 arc */							private PrimitiveArc metal2_arc;
	/** metal 3 arc */							private PrimitiveArc metal3_arc;
	/** metal 4 arc */							private PrimitiveArc metal4_arc;
	/** metal 5 arc */							private PrimitiveArc metal5_arc;
	/** metal 6 arc */							private PrimitiveArc metal6_arc;
	/** polysilicon 1 arc */					private PrimitiveArc poly1_arc;
	/** polysilicon 2 arc */					private PrimitiveArc poly2_arc;
	/** P-active arc */							private PrimitiveArc pActive_arc;
	/** N-active arc */							private PrimitiveArc nActive_arc;
	/** General active arc */					private PrimitiveArc active_arc;

	// nodes
	/** metal-1-pin */							private PrimitiveNode metal1Pin_node;
	/** metal-2-pin */							private PrimitiveNode metal2Pin_node;
	/** metal-3-pin */							private PrimitiveNode metal3Pin_node;
	/** metal-4-pin */							private PrimitiveNode metal4Pin_node;
	/** metal-5-pin */							private PrimitiveNode metal5Pin_node;
	/** metal-6-pin */							private PrimitiveNode metal6Pin_node;
	/** polysilicon-1-pin */					private PrimitiveNode poly1Pin_node;
	/** polysilicon-2-pin */					private PrimitiveNode poly2Pin_node;
	/** P-active-pin */							private PrimitiveNode pActivePin_node;
	/** N-active-pin */							private PrimitiveNode nActivePin_node;
	/** General active-pin */					private PrimitiveNode activePin_node;
	/** metal-1-P-active-contact */				private PrimitiveNode metal1PActiveContact_node;
	/** metal-1-N-active-contact */				private PrimitiveNode metal1NActiveContact_node;
	/** metal-1-polysilicon-1-contact */		private PrimitiveNode metal1Poly1Contact_node;
	/** metal-1-polysilicon-2-contact */		private PrimitiveNode metal1Poly2Contact_node;
	/** metal-1-polysilicon-1-2-contact */		private PrimitiveNode metal1Poly12Contact_node;
	/** P-Transistor */							private PrimitiveNode pTransistor_node;
	/** N-Transistor */							private PrimitiveNode nTransistor_node;
	/** Scalable-P-Transistor */				private PrimitiveNode scalablePTransistor_node;
	/** Scalable-N-Transistor */				private PrimitiveNode scalableNTransistor_node;
	/** metal-1-metal-2-contact */				private PrimitiveNode metal1Metal2Contact_node;
	/** metal-2-metal-3-contact */				private PrimitiveNode metal2Metal3Contact_node;
	/** metal-3-metal-4-contact */				private PrimitiveNode metal3Metal4Contact_node;
	/** metal-4-metal-5-contact */				private PrimitiveNode metal4Metal5Contact_node;
	/** metal-5-metal-6-contact */				private PrimitiveNode metal5Metal6Contact_node;
	/** Metal-1-P-Well Contact */				private PrimitiveNode metal1PWellContact_node;
	/** Metal-1-N-Well Contact */				private PrimitiveNode metal1NWellContact_node;
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

	// for dynamically modifying the transistor geometry
	private Technology.NodeLayer pTransistorPolyLayer, nTransistorPolyLayer;
	private Technology.NodeLayer pTransistorActiveLayer, nTransistorActiveLayer;
	private Technology.NodeLayer pTransistorActiveTLayer, pTransistorActiveBLayer;
	private Technology.NodeLayer pTransistorPolyLLayer, pTransistorPolyRLayer, pTransistorPolyCLayer;
	private Technology.NodeLayer nTransistorActiveTLayer, nTransistorActiveBLayer;
	private Technology.NodeLayer nTransistorPolyLLayer, nTransistorPolyRLayer, nTransistorPolyCLayer;
	private Technology.NodeLayer pTransistorWellLayer, nTransistorWellLayer;
	private Technology.NodeLayer pTransistorSelectLayer, nTransistorSelectLayer;

	// design rule constants
	/** wide rules apply to geometry larger than this */				private static final double WIDELIMIT = 10;

	// the meaning of "when" in the DRC table
	/** only applies if there are 2 metal layers in process */			private static final int M2 =      01;
	/** only applies if there are 3 metal layers in process */			private static final int M3 =      02;
	/** only applies if there are 4 metal layers in process */			private static final int M4 =      04;
	/** only applies if there are 5 metal layers in process */			private static final int M5 =     010;
	/** only applies if there are 6 metal layers in process */			private static final int M6 =     020;
	/** only applies if there are 2-3 metal layers in process */		private static final int M23 =     03;
	/** only applies if there are 2-4 metal layers in process */		private static final int M234 =    07;
	/** only applies if there are 2-5 metal layers in process */		private static final int M2345 =  017;
	/** only applies if there are 4-6 metal layers in process */		private static final int M456 =   034;
	/** only applies if there are 5-6 metal layers in process */		private static final int M56 =    030;
	/** only applies if there are 3-6 metal layers in process */		private static final int M3456 =  036;

	/** only applies if alternate contact rules are in effect */		private static final int AC =     040;
	/** only applies if alternate contact rules are not in effect */	private static final int NAC =   0100;
	/** only applies if stacked vias are allowed */						private static final int SV =    0200;
	/** only applies if stacked vias are not allowed */					private static final int NSV =   0400;
	/** only applies if deep rules are in effect */						private static final int DE =   01000;
	/** only applies if submicron rules are in effect */				private static final int SU =   02000;
	/** only applies if scmos rules are in effect */					private static final int SC =   04000;

	// the meaning of "ruletype" in the DRC table
	/** a minimum-width rule */			private static final int MINWID =     1;
	/** a node size rule */				private static final int NODSIZ =     2;
	/** a general surround rule */		private static final int SURROUND =   3;
	/** a via surround rule */			private static final int VIASUR =     4;
	/** a transistor well rule */		private static final int TRAWELL =    5;
	/** a transistor poly rule */		private static final int TRAPOLY =    6;
	/** a transistor active rule */		private static final int TRAACTIVE =  7;
	/** a spacing rule */				private static final int SPACING =    8;
	/** a multi-cut spacing rule */		private static final int SPACINGM =   9;
	/** a wide spacing rule */			private static final int SPACINGW =  10;
	/** an edge spacing rule */			private static final int SPACINGE =  11;
	/** a connected spacing rule */		private static final int CONSPA =    12;
	/** an unconnected spacing rule */	private static final int UCONSPA =   13;
	/** a contact cut spacing rule */	private static final int CUTSPA =    14;
	/** a contact cut size rule */		private static final int CUTSIZE =   15;
	/** a contact cut surround rule */	private static final int CUTSUR =    16;
	/** arc surround rule */			private static final int ASURROUND = 17;

	//******************** DESIGN RULES ********************

	static class DRCRules
	{
		String rule;			/* the name of the rule */
		int when;				/* when the rule is used */
		int ruleType;			/* the type of the rule */
		String layer1, layer2;	/* two layers that are used by the rule */
		double distance;		/* the spacing of the rule */
		String nodeName;		/* the node that is used by the rule */

		DRCRules(String rule, int when, int ruleType, String layer1, String layer2, double distance, String nodeName)
		{
			this.rule = rule;
			this.when = when;
			this.ruleType = ruleType;
			this.layer1 = layer1;
			this.layer2 = layer2;
			this.distance = distance;
			this.nodeName = nodeName;
		}
	};

	DRCRules [] theRules = new DRCRules[]
	{
		new DRCRules("1.1",  DE|SU,           MINWID,   "P-Well",          null,            12, null),
		new DRCRules("1.1",  DE|SU,           MINWID,   "N-Well",          null,            12, null),
		new DRCRules("1.1",  DE|SU,           MINWID,   "Pseudo-P-Well",   null,            12, null),
		new DRCRules("1.1",  DE|SU,           MINWID,   "Pseudo-N-Well",   null,            12, null),
		new DRCRules("1.1",  SC,              MINWID,   "P-Well",          null,            10, null),
		new DRCRules("1.1",  SC,              MINWID,   "N-Well",          null,            10, null),
		new DRCRules("1.1",  SC,              MINWID,   "Pseudo-P-Well",   null,            10, null),
		new DRCRules("1.1",  SC,              MINWID,   "Pseudo-N-Well",   null,            10, null),

		new DRCRules("1.2",  DE|SU,           UCONSPA,  "P-Well",         "P-Well",         18, null),
		new DRCRules("1.2",  DE|SU,           UCONSPA,  "N-Well",         "N-Well",         18, null),
		new DRCRules("1.2",  SC,              UCONSPA,  "P-Well",         "P-Well",         9,  null),
		new DRCRules("1.2",  SC,              UCONSPA,  "N-Well",         "N-Well",         9,  null),

		new DRCRules("1.3",  0,               CONSPA,   "P-Well",         "P-Well",         6,  null),
		new DRCRules("1.3",  0,               CONSPA,   "N-Well",         "N-Well",         6,  null),

		new DRCRules("1.4",  0,               UCONSPA,  "P-Well",         "N-Well",         0,  null),

		new DRCRules("2.1",  0,               MINWID,   "P-Active",        null,            3,  null),
		new DRCRules("2.1",  0,               MINWID,   "N-Active",        null,            3,  null),

		new DRCRules("2.2",  0,               SPACING,  "P-Active",       "P-Active",       3,  null),
		new DRCRules("2.2",  0,               SPACING,  "N-Active",       "N-Active",       3,  null),
		new DRCRules("2.2",  0,               SPACING,  "P-Active-Well",  "P-Active-Well",  3,  null),
		new DRCRules("2.2",  0,               SPACING,  "P-Active",       "N-Active",       3,  null),
		new DRCRules("2.2",  0,               SPACING,  "P-Active",       "P-Active-Well",  3,  null),
		new DRCRules("2.2",  0,               SPACING,  "N-Active",       "P-Active-Well",  3,  null),

		new DRCRules("2.3",  DE|SU,           SURROUND, "N-Well",         "P-Active",       6,  "Metal-1-P-Active-Con"),
		new DRCRules("2.3",  DE|SU,           ASURROUND,"N-Well",         "P-Active",       6,  "P-Active"),
		new DRCRules("2.3",  DE|SU,           SURROUND, "P-Well",         "N-Active",       6,  "Metal-1-N-Active-Con"),
		new DRCRules("2.3",  DE|SU,           ASURROUND,"P-Well",         "N-Active",       6,  "N-Active"),
		new DRCRules("2.3",  DE|SU,           TRAWELL,   null,             null,            6,  null),
		new DRCRules("2.3",  SC,              SURROUND, "N-Well",         "P-Active",       5,  "Metal-1-P-Active-Con"),
		new DRCRules("2.3",  SC,              ASURROUND,"N-Well",         "P-Active",       5,  "P-Active"),
		new DRCRules("2.3",  SC,              SURROUND, "P-Well",         "N-Active",       5,  "Metal-1-N-Active-Con"),
		new DRCRules("2.3",  SC,              ASURROUND,"P-Well",         "N-Active",       5,  "N-Active"),
		new DRCRules("2.3",  SC,              TRAWELL,   null,             null,            5,  null),

		new DRCRules("3.1",  0,               MINWID,   "Polysilicon-1",   null,            2,  null),
		new DRCRules("3.1",  0,               MINWID,   "Transistor-Poly", null,            2,  null),

		new DRCRules("3.2",  DE|SU,           SPACING,  "Polysilicon-1",  "Polysilicon-1",  3,  null),
		new DRCRules("3.2",  DE|SU,           SPACING,  "Polysilicon-1",  "Transistor-Poly",3,  null),
		new DRCRules("3.2",  SC,              SPACING,  "Polysilicon-1",  "Polysilicon-1",  2,  null),
		new DRCRules("3.2",  SC,              SPACING,  "Polysilicon-1",  "Transistor-Poly",2,  null),

		new DRCRules("3.2a", DE,              SPACING,  "Transistor-Poly","Transistor-Poly",4,  null),
		new DRCRules("3.2a", SU,              SPACING,  "Transistor-Poly","Transistor-Poly",3,  null),
		new DRCRules("3.2a", SC,              SPACING,  "Transistor-Poly","Transistor-Poly",2,  null),

		new DRCRules("3.3",  DE,              TRAPOLY,   null,             null,            2.5,null),
		new DRCRules("3.3",  SU|SC,           TRAPOLY,   null,             null,            2,  null),

		new DRCRules("3.4",  DE,              TRAACTIVE, null,             null,            4,  null),
		new DRCRules("3.4",  SU|SC,           TRAACTIVE, null,             null,            3,  null),

		new DRCRules("3.5",  0,               SPACING,  "Polysilicon-1",  "P-Active",       1,  null),
		new DRCRules("3.5",  0,               SPACING,  "Transistor-Poly","P-Active",       1,  null),
		new DRCRules("3.5",  0,               SPACING,  "Polysilicon-1",  "N-Active",       1,  null),
		new DRCRules("3.5",  0,               SPACING,  "Transistor-Poly","N-Active",       1,  null),
		new DRCRules("3.5",  0,               SPACING,  "Polysilicon-1",  "P-Active-Well",  1,  null),
		new DRCRules("3.5",  0,               SPACING,  "Transistor-Poly","P-Active-Well",  1,  null),

		new DRCRules("4.4",  DE,              MINWID,   "P-Select",        null,            4,  null),
		new DRCRules("4.4",  DE,              MINWID,   "N-Select",        null,            4,  null),
		new DRCRules("4.4",  DE,              MINWID,   "Pseudo-P-Select", null,            4,  null),
		new DRCRules("4.4",  DE,              MINWID,   "Pseudo-N-Select", null,            4,  null),
		new DRCRules("4.4",  DE,              SPACING,  "P-Select",       "P-Select",       4,  null),
		new DRCRules("4.4",  DE,              SPACING,  "N-Select",       "N-Select",       4,  null),
		new DRCRules("4.4",  SU|SC,           MINWID,   "P-Select",        null,            2,  null),
		new DRCRules("4.4",  SU|SC,           MINWID,   "N-Select",        null,            2,  null),
		new DRCRules("4.4",  SU|SC,           MINWID,   "Pseudo-P-Select", null,            2,  null),
		new DRCRules("4.4",  SU|SC,           MINWID,   "Pseudo-N-Select", null,            2,  null),
		new DRCRules("4.4",  SU|SC,           SPACING,  "P-Select",       "P-Select",       2,  null),
		new DRCRules("4.4",  SU|SC,           SPACING,  "N-Select",       "N-Select",       2,  null),
		new DRCRules("4.4",  0,               SPACING,  "P-Select",       "N-Select",       0,  null),

		new DRCRules("5.1",  0,               MINWID,   "Poly-Cut",        null,            2,  null),

		new DRCRules("5.2",        NAC,       NODSIZ,    null,             null,            5,  "Metal-1-Polysilicon-1-Con"),
		new DRCRules("5.2",        NAC,       SURROUND, "Polysilicon-1",  "Metal-1",        0.5,"Metal-1-Polysilicon-1-Con"),
		new DRCRules("5.2",        NAC,       CUTSUR,    null,             null,            1.5,"Metal-1-Polysilicon-1-Con"),
		new DRCRules("5.2b",       AC,        NODSIZ,    null,             null,            4,  "Metal-1-Polysilicon-1-Con"),
		new DRCRules("5.2b",       AC,        SURROUND, "Polysilicon-1",  "Metal-1",        0,  "Metal-1-Polysilicon-1-Con"),
		new DRCRules("5.2b",       AC,        CUTSUR,    null,             null,            1,  "Metal-1-Polysilicon-1-Con"),

		new DRCRules("5.3",     DE,           CUTSPA,    null,             null,            4,  "Metal-1-Polysilicon-1-Con"),
		new DRCRules("5.3",     DE,           SPACING,  "Poly-Cut",       "Poly-Cut",       4,  null),
		new DRCRules("5.3,6.3", DE|NAC,       SPACING,  "Active-Cut",     "Poly-Cut",       4,  null),
		new DRCRules("5.3",     SU,           CUTSPA,    null,             null,            3,  "Metal-1-Polysilicon-1-Con"),
		new DRCRules("5.3",     SU,           SPACING,  "Poly-Cut",       "Poly-Cut",       3,  null),
		new DRCRules("5.3,6.3", SU|NAC,       SPACING,  "Active-Cut",     "Poly-Cut",       3,  null),
		new DRCRules("5.3",     SC,           CUTSPA,    null,             null,            2,  "Metal-1-Polysilicon-1-Con"),
		new DRCRules("5.3",     SC,           SPACING,  "Poly-Cut",       "Poly-Cut",       2,  null),
		new DRCRules("5.3,6.3", SC|NAC,       SPACING,  "Active-Cut",     "Poly-Cut",       2,  null),

		new DRCRules("5.4",  0,               SPACING,  "Poly-Cut",       "Transistor-Poly",2,  null),

		new DRCRules("5.5b", DE|SU|AC,        UCONSPA,  "Poly-Cut",       "Polysilicon-1",  5,  null),
		new DRCRules("5.5b", DE|SU|AC,        UCONSPA,  "Poly-Cut",       "Transistor-Poly",5,  null),
		new DRCRules("5.5b", SC|   AC,        UCONSPA,  "Poly-Cut",       "Polysilicon-1",  4,  null),
		new DRCRules("5.5b", SC|   AC,        UCONSPA,  "Poly-Cut",       "Transistor-Poly",4,  null),

		new DRCRules("5.6b",       AC,        SPACING,  "Poly-Cut",       "P-Active",       2,  null),
		new DRCRules("5.6b",       AC,        SPACING,  "Poly-Cut",       "N-Active",       2,  null),

		new DRCRules("5.7b",       AC,        SPACINGM, "Poly-Cut",       "P-Active",       3,  null),
		new DRCRules("5.7b",       AC,        SPACINGM, "Poly-Cut",       "N-Active",       3,  null),

		new DRCRules("6.1",  0,               MINWID,   "Active-Cut",      null,            2,  null),

		new DRCRules("6.2",        NAC,       NODSIZ,    null,             null,            5,  "Metal-1-P-Active-Con"),
		new DRCRules("6.2",        NAC,       SURROUND, "P-Active",       "Metal-1",        0.5,"Metal-1-P-Active-Con"),
		new DRCRules("6.2",        NAC,       SURROUND, "P-Select",       "P-Active",       2,  "Metal-1-P-Active-Con"),
		new DRCRules("6.2",  DE|SU|NAC,       SURROUND, "N-Well",         "P-Active",       6,  "Metal-1-P-Active-Con"),
		new DRCRules("6.2",  SC|   NAC,       SURROUND, "N-Well",         "P-Active",       5,  "Metal-1-P-Active-Con"),
		new DRCRules("6.2",        NAC,       CUTSUR,    null,             null,            1.5,"Metal-1-P-Active-Con"),
		new DRCRules("6.2b",       AC,        NODSIZ,    null,             null,            4,  "Metal-1-P-Active-Con"),
		new DRCRules("6.2b",       AC,        SURROUND, "P-Active",       "Metal-1",        0,  "Metal-1-P-Active-Con"),
		new DRCRules("6.2b",       AC,        SURROUND, "P-Select",       "P-Active",       2,  "Metal-1-P-Active-Con"),
		new DRCRules("6.2b", DE|SU|AC,        SURROUND, "N-Well",         "P-Active",       6,  "Metal-1-P-Active-Con"),
		new DRCRules("6.2b", SC|   AC,        SURROUND, "N-Well",         "P-Active",       5,  "Metal-1-P-Active-Con"),
		new DRCRules("6.2b",       AC,        CUTSUR,    null,             null,            1,  "Metal-1-P-Active-Con"),

		new DRCRules("6.2",        NAC,       NODSIZ,    null,             null,            5,  "Metal-1-N-Active-Con"),
		new DRCRules("6.2",        NAC,       SURROUND, "N-Active",       "Metal-1",        0.5,"Metal-1-N-Active-Con"),
		new DRCRules("6.2",        NAC,       SURROUND, "N-Select",       "N-Active",       2,  "Metal-1-N-Active-Con"),
		new DRCRules("6.2",  DE|SU|NAC,       SURROUND, "P-Well",         "N-Active",       6,  "Metal-1-N-Active-Con"),
		new DRCRules("6.2",  SC|   NAC,       SURROUND, "P-Well",         "N-Active",       5,  "Metal-1-N-Active-Con"),
		new DRCRules("6.2",        NAC,       CUTSUR,    null,             null,            1.5,"Metal-1-N-Active-Con"),
		new DRCRules("6.2b",       AC,        NODSIZ,    null,             null,            4,  "Metal-1-N-Active-Con"),
		new DRCRules("6.2b",       AC,        SURROUND, "N-Active",       "Metal-1",        0,  "Metal-1-N-Active-Con"),
		new DRCRules("6.2b",       AC,        SURROUND, "N-Select",       "N-Active",       2,  "Metal-1-N-Active-Con"),
		new DRCRules("6.2b", DE|SU|AC,        SURROUND, "P-Well",         "N-Active",       6,  "Metal-1-N-Active-Con"),
		new DRCRules("6.2b", SC|   AC,        SURROUND, "P-Well",         "N-Active",       5,  "Metal-1-N-Active-Con"),
		new DRCRules("6.2b",       AC,        CUTSUR,    null,             null,            1,  "Metal-1-N-Active-Con"),

		new DRCRules("6.2",        NAC,       NODSIZ,    null,             null,            5,  "Metal-1-P-Well-Con"),
		new DRCRules("6.2",        NAC,       SURROUND, "P-Active-Well",  "Metal-1",        0.5,"Metal-1-P-Well-Con"),
		new DRCRules("6.2",        NAC,       SURROUND, "P-Select",       "P-Active-Well",  2,  "Metal-1-P-Well-Con"),
		new DRCRules("6.2",        NAC,       SURROUND, "P-Well",         "P-Active-Well",  3,  "Metal-1-P-Well-Con"),
		new DRCRules("6.2",        NAC,       CUTSUR,    null,             null,            1.5,"Metal-1-P-Well-Con"),
		new DRCRules("6.2b",       AC,        NODSIZ,    null,             null,            4,  "Metal-1-P-Well-Con"),
		new DRCRules("6.2b",       AC,        SURROUND, "P-Active-Well",  "Metal-1",        0,  "Metal-1-P-Well-Con"),
		new DRCRules("6.2b",       AC,        SURROUND, "P-Select",       "P-Active-Well",  2,  "Metal-1-P-Well-Con"),
		new DRCRules("6.2b",       AC,        SURROUND, "P-Well",         "P-Active-Well",  3,  "Metal-1-P-Well-Con"),
		new DRCRules("6.2b",       AC,        CUTSUR,    null,             null,            1,  "Metal-1-P-Well-Con"),

		new DRCRules("6.2",        NAC,       NODSIZ,    null,             null,            5,  "Metal-1-N-Well-Con"),
		new DRCRules("6.2",        NAC,       SURROUND, "N-Active",       "Metal-1",        0.5,"Metal-1-N-Well-Con"),
		new DRCRules("6.2",        NAC,       SURROUND, "N-Select",       "N-Active",       2,  "Metal-1-N-Well-Con"),
		new DRCRules("6.2",        NAC,       SURROUND, "N-Well",         "N-Active",       3,  "Metal-1-N-Well-Con"),
		new DRCRules("6.2",        NAC,       CUTSUR,    null,             null,            1.5,"Metal-1-N-Well-Con"),
		new DRCRules("6.2b",       AC,        NODSIZ,    null,             null,            4,  "Metal-1-N-Well-Con"),
		new DRCRules("6.2b",       AC,        SURROUND, "N-Active",       "Metal-1",        0,  "Metal-1-N-Well-Con"),
		new DRCRules("6.2b",       AC,        SURROUND, "N-Select",       "N-Active",       2,  "Metal-1-N-Well-Con"),
		new DRCRules("6.2b",       AC,        SURROUND, "N-Well",         "N-Active",       3,  "Metal-1-N-Well-Con"),
		new DRCRules("6.2b",       AC,        CUTSUR,    null,             null,            1,  "Metal-1-N-Well-Con"),

		new DRCRules("6.3",  DE,              CUTSPA,    null,             null,            4,  "Metal-1-P-Active-Con"),
		new DRCRules("6.3",  DE,              CUTSPA,    null,             null,            4,  "Metal-1-N-Active-Con"),
		new DRCRules("6.3",  DE,              SPACING,  "Active-Cut",     "Active-Cut",     4,  null),
		new DRCRules("6.3",  SU,              CUTSPA,    null,             null,            3,  "Metal-1-P-Active-Con"),
		new DRCRules("6.3",  SU,              CUTSPA,    null,             null,            3,  "Metal-1-N-Active-Con"),
		new DRCRules("6.3",  SU,              SPACING,  "Active-Cut",     "Active-Cut",     3,  null),
		new DRCRules("6.3",  SC,              CUTSPA,    null,             null,            2,  "Metal-1-P-Active-Con"),
		new DRCRules("6.3",  SC,              CUTSPA,    null,             null,            2,  "Metal-1-N-Active-Con"),
		new DRCRules("6.3",  SC,              SPACING,  "Active-Cut",     "Active-Cut",     2,  null),

		new DRCRules("6.4",  0,               SPACING,  "Active-Cut",     "Transistor-Poly",2,  null),

		new DRCRules("6.5b",       AC,        UCONSPA,  "Active-Cut",     "P-Active",       5,  null),
		new DRCRules("6.5b",       AC,        UCONSPA,  "Active-Cut",     "N-Active",       5,  null),

		new DRCRules("6.6b",       AC,        SPACING,  "Active-Cut",     "Polysilicon-1",  2,  null),
		new DRCRules("6.8b",       AC,        SPACING,  "Active-Cut",     "Poly-Cut",       4,  null),

		new DRCRules("7.1",  0,               MINWID,   "Metal-1",         null,            3,  null),

		new DRCRules("7.2",  DE|SU,           SPACING,  "Metal-1",        "Metal-1",        3,  null),
		new DRCRules("7.2",  SC,              SPACING,  "Metal-1",        "Metal-1",        2,  null),

		new DRCRules("7.4",  DE|SU,           SPACINGW, "Metal-1",        "Metal-1",        6,  null),
		new DRCRules("7.4",  SC,              SPACINGW, "Metal-1",        "Metal-1",        4,  null),

		new DRCRules("8.1",  DE,              CUTSIZE,   null,             null,            3, "Metal-1-Metal-2-Con"),
		new DRCRules("8.1",  DE,              NODSIZ,    null,             null,            5, "Metal-1-Metal-2-Con"),
		new DRCRules("8.1",  SU|SC,           CUTSIZE,   null,             null,            2, "Metal-1-Metal-2-Con"),
		new DRCRules("8.1",  SU|SC,           NODSIZ,    null,             null,            4, "Metal-1-Metal-2-Con"),

		new DRCRules("8.2",  0,               SPACING,  "Via1",           "Via1",           3,  null),

		new DRCRules("8.3",  0,               VIASUR,   "Metal-1",         null,            1, "Metal-1-Metal-2-Con"),

		new DRCRules("8.4",        NSV,       SPACING,  "Poly-Cut",       "Via1",           2,  null),
		new DRCRules("8.4",        NSV,       SPACING,  "Active-Cut",     "Via1",           2,  null),

		new DRCRules("8.5",        NSV,       SPACINGE, "Via1",           "Polysilicon-1",  2,  null),
		new DRCRules("8.5",        NSV,       SPACINGE, "Via1",           "Transistor-Poly",2,  null),
		new DRCRules("8.5",        NSV,       SPACINGE, "Via1",           "Polysilicon-2",  2,  null),
		new DRCRules("8.5",        NSV,       SPACINGE, "Via1",           "P-Active",       2,  null),
		new DRCRules("8.5",        NSV,       SPACINGE, "Via1",           "N-Active",       2,  null),

		new DRCRules("9.1",  0,               MINWID,   "Metal-2",         null,            3,  null),

		new DRCRules("9.2",  DE,              SPACING,  "Metal-2",        "Metal-2",        4,  null),
		new DRCRules("9.2",  SU|SC,           SPACING,  "Metal-2",        "Metal-2",        3,  null),

		new DRCRules("9.3",  0,               VIASUR,   "Metal-2",         null,            1, "Metal-1-Metal-2-Con"),

		new DRCRules("9.4",  DE,              SPACINGW, "Metal-2",        "Metal-2",        8,  null),
		new DRCRules("9.4",  SU|SC,           SPACINGW, "Metal-2",        "Metal-2",        6,  null),

		new DRCRules("11.1", SU,              MINWID,   "Polysilicon-2",   null,            7,  null),
		new DRCRules("11.1", SC,              MINWID,   "Polysilicon-2",   null,            3,  null),

		new DRCRules("11.2", 0,               SPACING,  "Polysilicon-2",  "Polysilicon-2",  3,  null),

		new DRCRules("11.3", SU,              SURROUND, "Polysilicon-2",  "Polysilicon-1",  5,  "Metal-1-Polysilicon-1-2-Con"),
		new DRCRules("11.3", SU,              NODSIZ,    null,             null,            15, "Metal-1-Polysilicon-1-2-Con"),
		new DRCRules("11.3", SU,              CUTSUR,    null,             null,            6.5,"Metal-1-Polysilicon-1-2-Con"),
		new DRCRules("11.3", SC,              SURROUND, "Polysilicon-2",  "Polysilicon-1",  2,  "Metal-1-Polysilicon-1-2-Con"),
		new DRCRules("11.3", SC,              NODSIZ,    null,             null,            9,  "Metal-1-Polysilicon-1-2-Con"),
		new DRCRules("11.3", SC,              CUTSUR,    null,             null,            3.5,"Metal-1-Polysilicon-1-2-Con"),

		new DRCRules("14.1", DE,              CUTSIZE,   null,             null,            3,  "Metal-2-Metal-3-Con"),
		new DRCRules("14.1", DE,              MINWID,   "Via2",            null,            3,  null),
		new DRCRules("14.1", DE,              NODSIZ,    null,             null,            5,  "Metal-2-Metal-3-Con"),
		new DRCRules("14.1", SU|SC,           CUTSIZE,   null,             null,            2,  "Metal-2-Metal-3-Con"),
		new DRCRules("14.1", SU|SC,           MINWID,   "Via2",            null,            2,  null),
		new DRCRules("14.1", SU|SC|    M23,   NODSIZ,    null,             null,            6,  "Metal-2-Metal-3-Con"),
		new DRCRules("14.1", SU|SC|    M456,  NODSIZ,    null,             null,            4,  "Metal-2-Metal-3-Con"),

		new DRCRules("14.2", 0,               SPACING,  "Via2",           "Via2",           3,  null),

		new DRCRules("14.3", 0,               VIASUR,   "Metal-2",         null,            1,  "Metal-2-Metal-3-Con"),

		new DRCRules("14.4", SU|SC|NSV,       SPACING,  "Via1",           "Via2",           2,  null),

		new DRCRules("15.1", SC|       M3,    MINWID,   "Metal-3",         null,            6,  null),
		new DRCRules("15.1", SU|       M3,    MINWID,   "Metal-3",         null,            5,  null),
		new DRCRules("15.1", SC|       M456,  MINWID,   "Metal-3",         null,            3,  null),
		new DRCRules("15.1", SU|       M456,  MINWID,   "Metal-3",         null,            3,  null),
		new DRCRules("15.1", DE,              MINWID,   "Metal-3",         null,            3,  null),

		new DRCRules("15.2", DE,              SPACING,  "Metal-3",        "Metal-3",        4,  null),
		new DRCRules("15.2", SU,              SPACING,  "Metal-3",        "Metal-3",        3,  null),
		new DRCRules("15.2", SC|       M3,    SPACING,  "Metal-3",        "Metal-3",        4,  null),
		new DRCRules("15.2", SC|       M456,  SPACING,  "Metal-3",        "Metal-3",        3,  null),

		new DRCRules("15.3", DE,              VIASUR,   "Metal-3",         null,            1, "Metal-2-Metal-3-Con"),
		new DRCRules("15.3", SU|SC|    M3,    VIASUR,   "Metal-3",         null,            2, "Metal-2-Metal-3-Con"),
		new DRCRules("15.3", SU|SC|    M456,  VIASUR,   "Metal-3",         null,            1, "Metal-2-Metal-3-Con"),

		new DRCRules("15.4", DE,              SPACINGW, "Metal-3",        "Metal-3",        8,  null),
		new DRCRules("15.4", SU,              SPACINGW, "Metal-3",        "Metal-3",        6,  null),
		new DRCRules("15.4", SC|       M3,    SPACINGW, "Metal-3",        "Metal-3",        8,  null),
		new DRCRules("15.4", SC|       M456,  SPACINGW, "Metal-3",        "Metal-3",        6,  null),

		new DRCRules("21.1", DE,              CUTSIZE,   null,             null,            3, "Metal-3-Metal-4-Con"),
		new DRCRules("21.1", DE,              MINWID,   "Via3",            null,            3,  null),
		new DRCRules("21.1", DE,              NODSIZ,    null,             null,            5, "Metal-3-Metal-4-Con"),
		new DRCRules("21.1", SU|SC,           CUTSIZE,   null,             null,            2, "Metal-3-Metal-4-Con"),
		new DRCRules("21.1", SU|SC,           MINWID,   "Via3",            null,            2,  null),
		new DRCRules("21.1", SU|       M4,    NODSIZ,    null,             null,            6, "Metal-3-Metal-4-Con"),
		new DRCRules("21.1", SU|       M56,   NODSIZ,    null,             null,            4, "Metal-3-Metal-4-Con"),
		new DRCRules("21.1", SC,              NODSIZ,    null,             null,            6, "Metal-3-Metal-4-Con"),

		new DRCRules("21.2", 0,               SPACING,  "Via3",           "Via3",           3,  null),

		new DRCRules("21.3", 0,               VIASUR,   "Metal-3",         null,            1, "Metal-3-Metal-4-Con"),

		new DRCRules("22.1",           M4,    MINWID,   "Metal-4",         null,            6,  null),
		new DRCRules("22.1",           M56,   MINWID,   "Metal-4",         null,            3,  null),

		new DRCRules("22.2",           M4,    SPACING,  "Metal-4",        "Metal-4",        6,  null),
		new DRCRules("22.2", DE|       M56,   SPACING,  "Metal-4",        "Metal-4",        4,  null),
		new DRCRules("22.2", SU|       M56,   SPACING,  "Metal-4",        "Metal-4",        3,  null),

		new DRCRules("22.3",           M4,    VIASUR,   "Metal-4",         null,            2, "Metal-3-Metal-4-Con"),
		new DRCRules("22.3",           M56,   VIASUR,   "Metal-4",         null,            1, "Metal-3-Metal-4-Con"),

		new DRCRules("22.4",           M4,    SPACINGW, "Metal-4",        "Metal-4",        12, null),
		new DRCRules("22.4", DE|       M56,   SPACINGW, "Metal-4",        "Metal-4",        8,  null),
		new DRCRules("22.4", SU|       M56,   SPACINGW, "Metal-4",        "Metal-4",        6,  null),

		new DRCRules("25.1", DE,              CUTSIZE,   null,             null,            3, "Metal-4-Metal-5-Con"),
		new DRCRules("25.1", DE,              MINWID,   "Via4",            null,            3,  null),
		new DRCRules("25.1", SU,              CUTSIZE,   null,             null,            2, "Metal-4-Metal-5-Con"),
		new DRCRules("25.1", SU,              MINWID,   "Via4",            null,            2,  null),
		new DRCRules("25.1", SU,              NODSIZ,    null,             null,            4, "Metal-4-Metal-5-Con"),
		new DRCRules("25.1", DE|       M5,    NODSIZ,    null,             null,            7, "Metal-4-Metal-5-Con"),
		new DRCRules("25.1", DE|       M6,    NODSIZ,    null,             null,            5, "Metal-4-Metal-5-Con"),

		new DRCRules("25.2", 0,               SPACINGW, "Via4",           "Via4",           3,  null),

		new DRCRules("25.3", 0,               VIASUR,   "Metal-4",         null,            1, "Metal-4-Metal-5-Con"),

		new DRCRules("26.1",           M5,    MINWID,   "Metal-5",         null,            4,  null),
		new DRCRules("26.1",           M6,    MINWID,   "Metal-5",         null,            3,  null),

		new DRCRules("26.2",           M5,    SPACING,  "Metal-5",        "Metal-5",        4,  null),
		new DRCRules("26.2", DE|       M6,    SPACING,  "Metal-5",        "Metal-5",        4,  null),
		new DRCRules("26.2", SU|       M6,    SPACING,  "Metal-5",        "Metal-5",        3,  null),

		new DRCRules("26.3", DE|       M5,    VIASUR,   "Metal-5",         null,            2, "Metal-4-Metal-5-Con"),
		new DRCRules("26.3", SU|       M5,    VIASUR,   "Metal-5",         null,            1, "Metal-4-Metal-5-Con"),
		new DRCRules("26.3",           M6,    VIASUR,   "Metal-5",         null,            1, "Metal-4-Metal-5-Con"),

		new DRCRules("26.4",           M5,    SPACINGW, "Metal-5",        "Metal-5",        8,  null),
		new DRCRules("26.4", DE|       M6,    SPACINGW, "Metal-5",        "Metal-5",        8,  null),
		new DRCRules("26.4", SU|       M6,    SPACINGW, "Metal-5",        "Metal-5",        6,  null),

		new DRCRules("29.1", DE,              CUTSIZE,   null,             null,            4, "Metal-5-Metal-6-Con"),
		new DRCRules("29.1", DE,              MINWID,   "Via5",            null,            4,  null),
		new DRCRules("29.1", DE,              NODSIZ,    null,             null,            8, "Metal-5-Metal-6-Con"),
		new DRCRules("29.1", SU,              CUTSIZE,   null,             null,            3, "Metal-5-Metal-6-Con"),
		new DRCRules("29.1", SU,              MINWID,   "Via5",            null,            3,  null),
		new DRCRules("29.1", SU,              NODSIZ,    null,             null,            5, "Metal-5-Metal-6-Con"),

		new DRCRules("29.2", 0,               SPACING,  "Via5",           "Via5",           4,  null),

		new DRCRules("29.3", 0,               VIASUR,   "Metal-5",         null,            1, "Metal-5-Metal-6-Con"),

		new DRCRules("30.1", 0,               MINWID,   "Metal-6",         null,            4,  null),

		new DRCRules("30.2", 0,               SPACING,  "Metal-6",        "Metal-6",        4,  null),

		new DRCRules("30.3", DE,              VIASUR,   "Metal-6",         null,            2, "Metal-5-Metal-6-Con"),
		new DRCRules("30.3", SU,              VIASUR,   "Metal-6",         null,            1, "Metal-5-Metal-6-Con"),

		new DRCRules("30.4", 0,               SPACINGW, "Metal-6",        "Metal-6",        8,  null)
	};

	/** defines the first transparent layer (metal-1). */			private static final int TRANSPARENT_1 = 1;
	/** defines the second transparent layer (poly-1). */			private static final int TRANSPARENT_2 = 2;
	/** defines the third transparent layer (active). */			private static final int TRANSPARENT_3 = 3;
	/** defines the fourth transparent layer (metal-2). */			private static final int TRANSPARENT_4 = 4;
	/** defines the fifth transparent layer (metal-3). */			private static final int TRANSPARENT_5 = 5;

	// -------------------- private and protected methods ------------------------

	private MoCMOS()
	{
		setTechName("mocmos");
		setTechDesc("MOSIS CMOS");
		setFactoryScale(200);			// in nanometers: really 0.2 micron
		setNoNegatedArcs();
		setStaticTechnology();
		setNumTransparentLayers(5);
		setColorMap(new Color []
		{                           /*     Metal-3 Metal-2 Active Polysilicon-1 Metal-1 */
			new Color(200,200,200), /*  0:                                              */
			new Color( 96,209,255), /*  1:                                      Metal-1 */
			new Color(255,155,192), /*  2:                        Polysilicon-1         */
			new Color(111,144,177), /*  3:                        Polysilicon-1 Metal-1 */
			new Color(107,226, 96), /*  4:                 Active                       */
			new Color( 83,179,160), /*  5:                 Active               Metal-1 */
			new Color(161,151,126), /*  6:                 Active Polysilicon-1         */
			new Color(110,171,152), /*  7:                 Active Polysilicon-1 Metal-1 */
			new Color(224, 95,255), /*  8:         Metal-2                              */
			new Color(135,100,191), /*  9:         Metal-2                      Metal-1 */
			new Color(170, 83,170), /* 10:         Metal-2        Polysilicon-1         */
			new Color(152,104,175), /* 11:         Metal-2        Polysilicon-1 Metal-1 */
			new Color(150,124,163), /* 12:         Metal-2 Active                       */
			new Color(129,144,165), /* 13:         Metal-2 Active               Metal-1 */
			new Color(155,133,151), /* 14:         Metal-2 Active Polysilicon-1         */
			new Color(141,146,153), /* 15:         Metal-2 Active Polysilicon-1 Metal-1 */
			new Color(247,251, 20), /* 16: Metal-3                                      */
			new Color(154,186, 78), /* 17: Metal-3                              Metal-1 */
			new Color(186,163, 57), /* 18: Metal-3                Polysilicon-1         */
			new Color(167,164, 99), /* 19: Metal-3                Polysilicon-1 Metal-1 */
			new Color(156,197, 41), /* 20: Metal-3         Active                       */
			new Color(138,197, 83), /* 21: Metal-3         Active               Metal-1 */
			new Color(161,184, 69), /* 22: Metal-3         Active Polysilicon-1         */
			new Color(147,183, 97), /* 23: Metal-3         Active Polysilicon-1 Metal-1 */
			new Color(186,155, 76), /* 24: Metal-3 Metal-2                              */
			new Color(155,163,119), /* 25: Metal-3 Metal-2                      Metal-1 */
			new Color(187,142, 97), /* 26: Metal-3 Metal-2        Polysilicon-1         */
			new Color(165,146,126), /* 27: Metal-3 Metal-2        Polysilicon-1 Metal-1 */
			new Color(161,178, 82), /* 28: Metal-3 Metal-2 Active                       */
			new Color(139,182,111), /* 29: Metal-3 Metal-2 Active               Metal-1 */
			new Color(162,170, 97), /* 30: Metal-3 Metal-2 Active Polysilicon-1         */
			new Color(147,172,116)  /* 31: Metal-3 Metal-2 Active Polysilicon-1 Metal-1 */
		});

		//**************************************** LAYERS ****************************************

		/** metal-1 layer */
		Layer metal1_lay = Layer.newInstance(this, "Metal-1",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, TRANSPARENT_1, 96,209,255, 0.8,1,
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
		Layer metal2_lay = Layer.newInstance(this, "Metal-2",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, TRANSPARENT_4, 224,95,255, 0.7,1,
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
		Layer metal3_lay = Layer.newInstance(this, "Metal-3",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, TRANSPARENT_5, 247,251,20, 0.6,1,
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
		Layer metal4_lay = Layer.newInstance(this, "Metal-4",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 150,150,255, 0.5,1,
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
		Layer metal5_lay = Layer.newInstance(this, "Metal-5",
			new EGraphics(EGraphics.PATTERNED|EGraphics.OUTLINEPAT, EGraphics.PATTERNED|EGraphics.OUTLINEPAT, 0, 255,190,6, 0.4,1,
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
		Layer metal6_lay = Layer.newInstance(this, "Metal-6",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,255,255, 0.3,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, TRANSPARENT_2, 255,155,192, 0.5,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 255,190,6, 1.0,1,
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
		Layer pActive_lay = Layer.newInstance(this, "P-Active",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, TRANSPARENT_3, 107,226,96, 0.5,1,
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
		Layer nActive_lay = Layer.newInstance(this, "N-Active",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, TRANSPARENT_3, 107,226,96, 0.5,1,
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

		/** P Select layer */
		Layer pSelect_lay = Layer.newInstance(this, "P-Select",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 255,255,0, 0.2,0,
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
		Layer nSelect_lay = Layer.newInstance(this, "N-Select",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 255,255,0, 0.2,0,
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
		Layer pWell_lay = Layer.newInstance(this, "P-Well",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 139,99,46, 0.2,0,
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
		Layer nWell_lay = Layer.newInstance(this, "N-Well",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 139,99,46, 0.2,0,
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
		Layer polyCut_lay = Layer.newInstance(this, "Poly-Cut",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 100,100,100, 1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** active cut layer */
		Layer activeCut_lay = Layer.newInstance(this, "Active-Cut",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 100,100,100, 1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via1 layer */
		Layer via1_lay = Layer.newInstance(this, "Via1", 
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 180,180,180, 1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via2 layer */
		Layer via2_lay = Layer.newInstance(this, "Via2",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 180,180,180, 1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via3 layer */
		Layer via3_lay = Layer.newInstance(this, "Via3",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 180,180,180, 1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via4 layer */
		Layer via4_lay = Layer.newInstance(this, "Via4",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 180,180,180, 1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** via5 layer */
		Layer via5_lay = Layer.newInstance(this, "Via5",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 180,180,180, 1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** passivation layer */
		Layer passivation_lay = Layer.newInstance(this, "Passivation",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 100,100,100, 1.0,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, TRANSPARENT_2, 255,155,192, 0.5,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 0,0,0, 1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** P act well layer */
		Layer pActiveWell_lay = Layer.newInstance(this, "P-Active-Well",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, TRANSPARENT_3, 107,226,96, 1.0,0,
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
		Layer silicideBlock_lay = Layer.newInstance(this, "Silicide-Block",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 230,230,230, 1.0,0,
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

		/** pseudo metal 1 */
		Layer pseudoMetal1_lay = Layer.newInstance(this, "Pseudo-Metal-1",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, TRANSPARENT_1, 96,209,255, 0.8,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, TRANSPARENT_4, 224,95,255, 0.7,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, TRANSPARENT_5, 247,251,20, 0.6,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 150,150,255, 0.5,1,
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
			new EGraphics(EGraphics.PATTERNED|EGraphics.OUTLINEPAT, EGraphics.PATTERNED|EGraphics.OUTLINEPAT, 0, 255,190,6, 0.4,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 0,255,255, 0.3,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, TRANSPARENT_2, 255,155,192, 1.0,1,
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
						0x1111,   //  X X X X X X X X
						0x1111,   //    X   X   X   X
						0xFFFF,   // XXXXXXXXXXXXXXXX
						0x1111,   //    X   X   X   X
						0x5555}));//  X X X X X X X X

		/** pseudo poly2 layer */
		Layer pseudoPoly2_lay = Layer.newInstance(this, "Pseudo-Electrode",
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 255,190,6, 1.0,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, TRANSPARENT_3, 107,226,96, 1.0,1,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, TRANSPARENT_3, 107,226,96, 1.0,1,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 255,255,0, 1.0,0,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 255,255,0, 1.0,0,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 139,99,46, 1.0,0,
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
			new EGraphics(EGraphics.PATTERNED, EGraphics.PATTERNED, 0, 139,99,46, 1.0,0,
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 0, 255,0,0, 1.0,0,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		// The layer functions
		metal1_lay.setFunction(Layer.Function.METAL1);									// Metal-1
		metal2_lay.setFunction(Layer.Function.METAL2);									// Metal-2
		metal3_lay.setFunction(Layer.Function.METAL3);									// Metal-3
		metal4_lay.setFunction(Layer.Function.METAL4);									// Metal-4
		metal5_lay.setFunction(Layer.Function.METAL5);									// Metal-5
		metal6_lay.setFunction(Layer.Function.METAL6);									// Metal-6
		poly1_lay.setFunction(Layer.Function.POLY1);									// Polysilicon-1
		poly2_lay.setFunction(Layer.Function.POLY2);									// Polysilicon-2
		pActive_lay.setFunction(Layer.Function.DIFFP);									// P-Active
		nActive_lay.setFunction(Layer.Function.DIFFN);									// N-Active
		pSelect_lay.setFunction(Layer.Function.IMPLANTP);								// P-Select
		nSelect_lay.setFunction(Layer.Function.IMPLANTN);								// N-Select
		pWell_lay.setFunction(Layer.Function.WELLP);									// P-Well
		nWell_lay.setFunction(Layer.Function.WELLN);									// N-Well
		polyCut_lay.setFunction(Layer.Function.CONTACT1, Layer.Function.CONPOLY);		// Poly-Cut
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
		metal1_lay.setFactoryCIFLayer("CMF");				// Metal-1
		metal2_lay.setFactoryCIFLayer("CMS");				// Metal-2
		metal3_lay.setFactoryCIFLayer("CMT");				// Metal-3
		metal4_lay.setFactoryCIFLayer("CMQ");				// Metal-4
		metal5_lay.setFactoryCIFLayer("CMP");				// Metal-5
		metal6_lay.setFactoryCIFLayer("CM6");				// Metal-6
		poly1_lay.setFactoryCIFLayer("CPG");				// Polysilicon-1
		poly2_lay.setFactoryCIFLayer("CEL");				// Polysilicon-2
		pActive_lay.setFactoryCIFLayer("CAA");				// P-Active
		nActive_lay.setFactoryCIFLayer("CAA");				// N-Active
		pSelect_lay.setFactoryCIFLayer("CSP");				// P-Select
		nSelect_lay.setFactoryCIFLayer("CSN");				// N-Select
		pWell_lay.setFactoryCIFLayer("CWP");				// P-Well
		nWell_lay.setFactoryCIFLayer("CWN");				// N-Well
		polyCut_lay.setFactoryCIFLayer("CCC");				// Poly-Cut
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
		metal1_lay.setFactoryGDSLayer("49");				// Metal-1
		metal2_lay.setFactoryGDSLayer("51");				// Metal-2
		metal3_lay.setFactoryGDSLayer("62");				// Metal-3
		metal4_lay.setFactoryGDSLayer("31");				// Metal-4
		metal5_lay.setFactoryGDSLayer("33");				// Metal-5
		metal6_lay.setFactoryGDSLayer("37");				// Metal-6
		poly1_lay.setFactoryGDSLayer("46");					// Polysilicon-1
		poly2_lay.setFactoryGDSLayer("56");					// Polysilicon-2
		pActive_lay.setFactoryGDSLayer("43");				// P-Active
		nActive_lay.setFactoryGDSLayer("43");				// N-Active
		pSelect_lay.setFactoryGDSLayer("44");				// P-Select
		nSelect_lay.setFactoryGDSLayer("45");				// N-Select
		pWell_lay.setFactoryGDSLayer("41");					// P-Well
		nWell_lay.setFactoryGDSLayer("42");					// N-Well
		polyCut_lay.setFactoryGDSLayer("47");				// Poly-Cut
		activeCut_lay.setFactoryGDSLayer("48");				// Active-Cut
		via1_lay.setFactoryGDSLayer("50");					// Via-1
		via2_lay.setFactoryGDSLayer("61");					// Via-2
		via3_lay.setFactoryGDSLayer("30");					// Via-3
		via4_lay.setFactoryGDSLayer("32");					// Via-4
		via5_lay.setFactoryGDSLayer("36");					// Via-5
		passivation_lay.setFactoryGDSLayer("52");			// Passivation
		transistorPoly_lay.setFactoryGDSLayer("46");		// Transistor-Poly
		polyCap_lay.setFactoryGDSLayer("28");				// Poly-Cap
		pActiveWell_lay.setFactoryGDSLayer("43");			// P-Active-Well
		silicideBlock_lay.setFactoryGDSLayer("29");			// Silicide-Block
		pseudoMetal1_lay.setFactoryGDSLayer("");			// Pseudo-Metal-1
		pseudoMetal2_lay.setFactoryGDSLayer("");			// Pseudo-Metal-2
		pseudoMetal3_lay.setFactoryGDSLayer("");			// Pseudo-Metal-3
		pseudoMetal4_lay.setFactoryGDSLayer("");			// Pseudo-Metal-4
		pseudoMetal5_lay.setFactoryGDSLayer("");			// Pseudo-Metal-5
		pseudoMetal6_lay.setFactoryGDSLayer("");			// Pseudo-Metal-6
		pseudoPoly1_lay.setFactoryGDSLayer("");				// Pseudo-Polysilicon-1
		pseudoPoly2_lay.setFactoryGDSLayer("");				// Pseudo-Polysilicon-2
		pseudoPActive_lay.setFactoryGDSLayer("");			// Pseudo-P-Active
		pseudoNActive_lay.setFactoryGDSLayer("");			// Pseudo-N-Active
		pseudoPSelect_lay.setFactoryGDSLayer("");			// Pseudo-P-Select
		pseudoNSelect_lay.setFactoryGDSLayer("");			// Pseudo-N-Select
		pseudoPWell_lay.setFactoryGDSLayer("");				// Pseudo-P-Well
		pseudoNWell_lay.setFactoryGDSLayer("");				// Pseudo-N-Well
		padFrame_lay.setFactoryGDSLayer("26");				// Pad-Frame

		// The Skill names
		metal1_lay.setFactorySkillLayer("metal1");			// Metal-1
		metal2_lay.setFactorySkillLayer("metal2");			// Metal-2
		metal3_lay.setFactorySkillLayer("metal3");			// Metal-3
		metal4_lay.setFactorySkillLayer("metal4");			// Metal-4
		metal5_lay.setFactorySkillLayer("metal5");			// Metal-5
		metal6_lay.setFactorySkillLayer("metal6");			// Metal-6
		poly1_lay.setFactorySkillLayer("poly");				// Polysilicon-1
		poly2_lay.setFactorySkillLayer("");					// Polysilicon-2
		pActive_lay.setFactorySkillLayer("aa");				// P-Active
		nActive_lay.setFactorySkillLayer("aa");				// N-Active
		pSelect_lay.setFactorySkillLayer("pplus");			// P-Select
		nSelect_lay.setFactorySkillLayer("nplus");			// N-Select
		pWell_lay.setFactorySkillLayer("pwell");			// P-Well
		nWell_lay.setFactorySkillLayer("nwell");			// N-Well
		polyCut_lay.setFactorySkillLayer("pcont");			// Poly-Cut
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

		// The layer height
		metal1_lay.setHeight(0, 17);						// Metal-1
		metal2_lay.setHeight(0, 19);						// Metal-2
		metal3_lay.setHeight(0, 21);						// Metal-3
		metal4_lay.setHeight(0, 23);						// Metal-4
		metal5_lay.setHeight(0, 25);						// Metal-5
		metal6_lay.setHeight(0, 27);						// Metal-6
		poly1_lay.setHeight(0, 15);							// Polysilicon-1
		poly2_lay.setHeight(0, 16);							// Polysilicon-2
		pActive_lay.setHeight(0, 13);						// P-Active
		nActive_lay.setHeight(0, 13);						// N-Active
		pSelect_lay.setHeight(0, 12);						// P-Select
		nSelect_lay.setHeight(0, 12);						// N-Select
		pWell_lay.setHeight(0, 11);							// P-Well
		nWell_lay.setHeight(0, 11);							// N-Well
		polyCut_lay.setHeight(2, 16);						// Poly-Cut
		activeCut_lay.setHeight(4, 15);						// Active-Cut
		via1_lay.setHeight(2, 18);							// Via-1
		via2_lay.setHeight(2, 20);							// Via-2
		via3_lay.setHeight(2, 22);							// Via-3
		via4_lay.setHeight(2, 24);							// Via-4
		via5_lay.setHeight(2, 26);							// Via-5
		passivation_lay.setHeight(0, 30);					// Passivation
		transistorPoly_lay.setHeight(0, 15);				// Transistor-Poly
		polyCap_lay.setHeight(0, 28);						// Poly-Cap
		pActiveWell_lay.setHeight(0, 13);					// P-Active-Well
		silicideBlock_lay.setHeight(0, 10);					// Silicide-Block
		pseudoMetal1_lay.setHeight(0, 17);					// Pseudo-Metal-1
		pseudoMetal2_lay.setHeight(0, 19);					// Pseudo-Metal-2
		pseudoMetal3_lay.setHeight(0, 21);					// Pseudo-Metal-3
		pseudoMetal4_lay.setHeight(0, 23);					// Pseudo-Metal-4
		pseudoMetal5_lay.setHeight(0, 25);					// Pseudo-Metal-5
		pseudoMetal6_lay.setHeight(0, 27);					// Pseudo-Metal-6
		pseudoPoly1_lay.setHeight(0, 12);					// Pseudo-Polysilicon-1
		pseudoPoly2_lay.setHeight(0, 13);					// Pseudo-Polysilicon-2
		pseudoPActive_lay.setHeight(0, 11);					// Pseudo-P-Active
		pseudoNActive_lay.setHeight(0, 11);					// Pseudo-N-Active
		pseudoPSelect_lay.setHeight(0, 2);					// Pseudo-P-Select
		pseudoNSelect_lay.setHeight(0, 2);					// Pseudo-N-Select
		pseudoPWell_lay.setHeight(0, 0);					// Pseudo-P-Well
		pseudoNWell_lay.setHeight(0, 0);					// Pseudo-N-Well
		padFrame_lay.setHeight(0, 33);						// Pad-Frame

		// The layer height
		metal1_lay.setFactoryParasitics(0.06, 0.07, 0);			// Metal-1
		metal2_lay.setFactoryParasitics(0.06, 0.04, 0);			// Metal-2
		metal3_lay.setFactoryParasitics(0.06, 0.04, 0);			// Metal-3
		metal4_lay.setFactoryParasitics(0.03, 0.04, 0);			// Metal-4
		metal5_lay.setFactoryParasitics(0.03, 0.04, 0);			// Metal-5
		metal6_lay.setFactoryParasitics(0.03, 0.04, 0);			// Metal-6
		poly1_lay.setFactoryParasitics(2.5, 0.09, 0);			// Polysilicon-1
		poly2_lay.setFactoryParasitics(50.0, 1.0, 0);			// Polysilicon-2
		pActive_lay.setFactoryParasitics(2.5, 0.9, 0);			// P-Active
		nActive_lay.setFactoryParasitics(3.0, 0.9, 0);			// N-Active
		pSelect_lay.setFactoryParasitics(0, 0, 0);				// P-Select
		nSelect_lay.setFactoryParasitics(0, 0, 0);				// N-Select
		pWell_lay.setFactoryParasitics(0, 0, 0);				// P-Well
		nWell_lay.setFactoryParasitics(0, 0, 0);				// N-Well
		polyCut_lay.setFactoryParasitics(2.2, 0, 0);			// Poly-Cut
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
		metal1_arc = PrimitiveArc.newInstance(this, "Metal-1", 3.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal1_lay, 0, Poly.Type.FILLED)
		});
		metal1_arc.setFunction(PrimitiveArc.Function.METAL1);
		metal1_arc.setFactoryFixedAngle(true);
		metal1_arc.setWipable();
		metal1_arc.setFactoryAngleIncrement(90);

		/** metal 2 arc */
		metal2_arc = PrimitiveArc.newInstance(this, "Metal-2", 3.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal2_lay, 0, Poly.Type.FILLED)
		});
		metal2_arc.setFunction(PrimitiveArc.Function.METAL2);
		metal2_arc.setFactoryFixedAngle(true);
		metal2_arc.setWipable();
		metal2_arc.setFactoryAngleIncrement(90);

		/** metal 3 arc */
		metal3_arc = PrimitiveArc.newInstance(this, "Metal-3", 3.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal3_lay, 0, Poly.Type.FILLED)
		});
		metal3_arc.setFunction(PrimitiveArc.Function.METAL3);
		metal3_arc.setFactoryFixedAngle(true);
		metal3_arc.setWipable();
		metal3_arc.setFactoryAngleIncrement(90);

		/** metal 4 arc */
		metal4_arc = PrimitiveArc.newInstance(this, "Metal-4", 6.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal4_lay, 0, Poly.Type.FILLED)
		});
		metal4_arc.setFunction(PrimitiveArc.Function.METAL4);
		metal4_arc.setFactoryFixedAngle(true);
		metal4_arc.setWipable();
		metal4_arc.setFactoryAngleIncrement(90);

		/** metal 5 arc */
		metal5_arc = PrimitiveArc.newInstance(this, "Metal-5", 3.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal5_lay, 0, Poly.Type.FILLED)
		});
		metal5_arc.setFunction(PrimitiveArc.Function.METAL5);
		metal5_arc.setFactoryFixedAngle(true);
		metal5_arc.setWipable();
		metal5_arc.setFactoryAngleIncrement(90);

		/** metal 6 arc */
		metal6_arc = PrimitiveArc.newInstance(this, "Metal-6", 4.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(metal6_lay, 0, Poly.Type.FILLED)
		});
		metal6_arc.setFunction(PrimitiveArc.Function.METAL6);
		metal6_arc.setFactoryFixedAngle(true);
		metal6_arc.setWipable();
		metal6_arc.setFactoryAngleIncrement(90);

		/** polysilicon 1 arc */
		poly1_arc = PrimitiveArc.newInstance(this, "Polysilicon-1", 2.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(poly1_lay, 0, Poly.Type.FILLED)
		});
		poly1_arc.setFunction(PrimitiveArc.Function.POLY1);
		poly1_arc.setFactoryFixedAngle(true);
		poly1_arc.setWipable();
		poly1_arc.setFactoryAngleIncrement(90);

		/** polysilicon 2 arc */
		poly2_arc = PrimitiveArc.newInstance(this, "Polysilicon-2", 7.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(poly2_lay, 0, Poly.Type.FILLED)
		});
		poly2_arc.setFunction(PrimitiveArc.Function.POLY2);
		poly2_arc.setFactoryFixedAngle(true);
		poly2_arc.setWipable();
		poly2_arc.setFactoryAngleIncrement(90);
		poly2_arc.setNotUsed();

		/** P-active arc */
		pActive_arc = PrimitiveArc.newInstance(this, "P-Active", 15.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(pActive_lay, 12, Poly.Type.FILLED),
			new Technology.ArcLayer(nWell_lay, 0, Poly.Type.FILLED),
			new Technology.ArcLayer(pSelect_lay, 8, Poly.Type.FILLED)
		});
		pActive_arc.setFunction(PrimitiveArc.Function.DIFFP);
		pActive_arc.setFactoryFixedAngle(true);
		pActive_arc.setWipable();
		pActive_arc.setFactoryAngleIncrement(90);
		pActive_arc.setWidthOffset(12.0);

		/** N-active arc */
		nActive_arc = PrimitiveArc.newInstance(this, "N-Active", 15.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(nActive_lay, 12, Poly.Type.FILLED),
			new Technology.ArcLayer(pWell_lay, 0, Poly.Type.FILLED),
			new Technology.ArcLayer(nSelect_lay, 8, Poly.Type.FILLED)
		});
		nActive_arc.setFunction(PrimitiveArc.Function.DIFFN);
		nActive_arc.setFactoryFixedAngle(true);
		nActive_arc.setWipable();
		nActive_arc.setFactoryAngleIncrement(90);
		nActive_arc.setWidthOffset(12.0);

		/** General active arc */
		active_arc = PrimitiveArc.newInstance(this, "Active", 3.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(pActive_lay, 0, Poly.Type.FILLED),
			new Technology.ArcLayer(nActive_lay, 0, Poly.Type.FILLED)
		});
		active_arc.setFunction(PrimitiveArc.Function.DIFF);
		active_arc.setFactoryFixedAngle(true);
		active_arc.setWipable();
		active_arc.setFactoryAngleIncrement(90);
		active_arc.setNotUsed();

		//**************************************** NODES ****************************************

		/** metal-1-pin */
		metal1Pin_node = PrimitiveNode.newInstance("Metal-1-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal1_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal1Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1Pin_node, new ArcProto[] {metal1_arc}, "metal-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal1Pin_node.setFunction(NodeProto.Function.PIN);
		metal1Pin_node.setArcsWipe();
		metal1Pin_node.setArcsShrink();

		/** metal-2-pin */
		metal2Pin_node = PrimitiveNode.newInstance("Metal-2-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal2_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal2Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal2Pin_node, new ArcProto[] {metal2_arc}, "metal-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal2Pin_node.setFunction(NodeProto.Function.PIN);
		metal2Pin_node.setArcsWipe();
		metal2Pin_node.setArcsShrink();

		/** metal-3-pin */
		metal3Pin_node = PrimitiveNode.newInstance("Metal-3-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal3_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal3Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal3Pin_node, new ArcProto[] {metal3_arc}, "metal-3", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal3Pin_node.setFunction(NodeProto.Function.PIN);
		metal3Pin_node.setArcsWipe();
		metal3Pin_node.setArcsShrink();

		/** metal-4-pin */
		metal4Pin_node = PrimitiveNode.newInstance("Metal-4-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal4_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal4Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal4Pin_node, new ArcProto[] {metal4_arc}, "metal-4", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal4Pin_node.setFunction(NodeProto.Function.PIN);
		metal4Pin_node.setArcsWipe();
		metal4Pin_node.setArcsShrink();

		/** metal-5-pin */
		metal5Pin_node = PrimitiveNode.newInstance("Metal-5-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal5_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal5Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal5Pin_node, new ArcProto[] {metal5_arc}, "metal-5", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal5Pin_node.setFunction(NodeProto.Function.PIN);
		metal5Pin_node.setArcsWipe();
		metal5Pin_node.setArcsShrink();
		metal5Pin_node.setNotUsed();

		/** metal-6-pin */
		metal6Pin_node = PrimitiveNode.newInstance("Metal-6-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoMetal6_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal6Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal6Pin_node, new ArcProto[] {metal6_arc}, "metal-6", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal6Pin_node.setFunction(NodeProto.Function.PIN);
		metal6Pin_node.setArcsWipe();
		metal6Pin_node.setArcsShrink();
		metal6Pin_node.setNotUsed();

		/** polysilicon-1-pin */
		poly1Pin_node = PrimitiveNode.newInstance("Polysilicon-1-Pin", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoPoly1_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		poly1Pin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, poly1Pin_node, new ArcProto[] {poly1_arc}, "polysilicon-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		poly1Pin_node.setFunction(NodeProto.Function.PIN);
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
				PrimitivePort.newInstance(this, poly2Pin_node, new ArcProto[] {poly2_arc}, "polysilicon-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		poly2Pin_node.setFunction(NodeProto.Function.PIN);
		poly2Pin_node.setArcsWipe();
		poly2Pin_node.setArcsShrink();
		poly2Pin_node.setNotUsed();

		/** P-active-pin */
		pActivePin_node = PrimitiveNode.newInstance("P-Active-Pin", this, 15.0, 15.0, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoPActive_lay,  0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6)),
				new Technology.NodeLayer(pseudoNWell_lay,  0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(pseudoPSelect_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4))
			});
		pActivePin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pActivePin_node, new ArcProto[] {pActive_arc}, "p-active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromBottom(7.5), EdgeH.fromRight(7.5), EdgeV.fromTop(7.5))
			});
		pActivePin_node.setFunction(NodeProto.Function.PIN);
		pActivePin_node.setArcsWipe();
		pActivePin_node.setArcsShrink();

		/** N-active-pin */
		nActivePin_node = PrimitiveNode.newInstance("N-Active-Pin", this, 15.0, 15.0, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoNActive_lay,  0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6)),
				new Technology.NodeLayer(pseudoPWell_lay,  0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(pseudoNSelect_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4))
			});
		nActivePin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, nActivePin_node, new ArcProto[] {nActive_arc}, "n-active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromBottom(7.5), EdgeH.fromRight(7.5), EdgeV.fromTop(7.5))
			});
		nActivePin_node.setFunction(NodeProto.Function.PIN);
		nActivePin_node.setArcsWipe();
		nActivePin_node.setArcsShrink();

		/** General active-pin */
		activePin_node = PrimitiveNode.newInstance("Active-Pin", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pseudoPActive_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(pseudoNActive_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		activePin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, activePin_node, new ArcProto[] {active_arc, pActive_arc, nActive_arc}, "active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		activePin_node.setFunction(NodeProto.Function.PIN);
		activePin_node.setArcsWipe();
		activePin_node.setArcsShrink();

		/** metal-1-P-active-contact */
		metal1PActiveContact_node = PrimitiveNode.newInstance("Metal-1-P-Active-Con", this, 17.0, 17.0, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6.5)),
				new Technology.NodeLayer(pActive_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6)),
				new Technology.NodeLayer(nWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX,Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(pSelect_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4)),
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(7.5))
			});
		metal1PActiveContact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1PActiveContact_node, new ArcProto[] {pActive_arc, metal1_arc}, "metal-1-p-act", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(8), EdgeV.fromBottom(8), EdgeH.fromRight(8), EdgeV.fromTop(8))
			});
		metal1PActiveContact_node.setFunction(NodeProto.Function.CONTACT);
		metal1PActiveContact_node.setSpecialType(PrimitiveNode.MULTICUT);
		metal1PActiveContact_node.setSpecialValues(new double [] {2, 2, 1.5, 3});
		metal1PActiveContact_node.setMinSize(17, 17, "6.2, 7.3");

		/** metal-1-N-active-contact */
		metal1NActiveContact_node = PrimitiveNode.newInstance("Metal-1-N-Active-Con", this, 17.0, 17.0, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6.5)),
				new Technology.NodeLayer(nActive_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6)),
				new Technology.NodeLayer(pWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(nSelect_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4)),
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(7.5))
			});
		metal1NActiveContact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1NActiveContact_node, new ArcProto[] {nActive_arc, metal1_arc}, "metal-1-n-act", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(8), EdgeV.fromBottom(8), EdgeH.fromRight(8), EdgeV.fromTop(8))
			});
		metal1NActiveContact_node.setFunction(NodeProto.Function.CONTACT);
		metal1NActiveContact_node.setSpecialType(PrimitiveNode.MULTICUT);
		metal1NActiveContact_node.setSpecialValues(new double [] {2, 2, 1.5, 3});
		metal1NActiveContact_node.setMinSize(17, 17, "6.2, 7.3");

		/** metal-1-polysilicon-1-contact */
		metal1Poly1Contact_node = PrimitiveNode.newInstance("Metal-1-Polysilicon-1-Con", this, 5.0, 5.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(0.5)),
				new Technology.NodeLayer(poly1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(polyCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1.5))
			});
		metal1Poly1Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1Poly1Contact_node, new ArcProto[] {poly1_arc, metal1_arc}, "metal-1-polysilicon-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromBottom(2), EdgeH.fromRight(2), EdgeV.fromTop(2))
			});
		metal1Poly1Contact_node.setFunction(NodeProto.Function.CONTACT);
		metal1Poly1Contact_node.setSpecialType(PrimitiveNode.MULTICUT);
		metal1Poly1Contact_node.setSpecialValues(new double [] {2, 2, 1.5, 3});
		metal1Poly1Contact_node.setMinSize(5, 5, "5.2, 7.3");

		/** metal-1-polysilicon-2-contact */
		metal1Poly2Contact_node = PrimitiveNode.newInstance("Metal-1-Polysilicon-2-Con", this, 10.0, 10.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(3)),
				new Technology.NodeLayer(poly2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(polyCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4))
			});
		metal1Poly2Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1Poly2Contact_node, new ArcProto[] {poly2_arc, metal1_arc}, "metal-1-polysilicon-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4.5), EdgeV.fromBottom(4.5), EdgeH.fromRight(4.5), EdgeV.fromTop(4.5))
			});
		metal1Poly2Contact_node.setFunction(NodeProto.Function.CONTACT);
		metal1Poly2Contact_node.setSpecialType(PrimitiveNode.MULTICUT);
		metal1Poly2Contact_node.setSpecialValues(new double [] {2, 2, 4, 3});
		metal1Poly2Contact_node.setNotUsed();
		metal1Poly2Contact_node.setMinSize(10, 10, "?");

		/** metal-1-polysilicon-1-2-contact */
		metal1Poly12Contact_node = PrimitiveNode.newInstance("Metal-1-Polysilicon-1-2-Con", this, 15.0, 15.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(5.5)),
				new Technology.NodeLayer(poly1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(5)),
				new Technology.NodeLayer(poly2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(polyCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6.5))
			});
		metal1Poly12Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1Poly12Contact_node, new ArcProto[] {poly1_arc, poly2_arc, metal1_arc}, "metal-1-polysilicon-1-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(7), EdgeV.fromBottom(7), EdgeH.fromRight(7), EdgeV.fromTop(7))
			});
		metal1Poly12Contact_node.setFunction(NodeProto.Function.CONTACT);
		metal1Poly12Contact_node.setSpecialType(PrimitiveNode.MULTICUT);
		metal1Poly12Contact_node.setSpecialValues(new double [] {2, 2, 6.5, 3});
		metal1Poly12Contact_node.setNotUsed();
		metal1Poly12Contact_node.setMinSize(15, 15, "?");

		/** P-Transistor */
		pTransistorPolyLayer = new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(10)),
			new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(10))}, 1, 1, 2, 2);
		pTransistorPolyLLayer = new Technology.NodeLayer(poly1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(10)),
			new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromTop(10))}, 1, 1, 2, 2);
		pTransistorPolyRLayer = new Technology.NodeLayer(poly1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromBottom(10)),
			new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(10))}, 1, 1, 2, 2);
		pTransistorPolyCLayer = new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(10)),
			new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(10))}, 1, 1, 2, 2);
		pTransistorActiveLayer = new Technology.NodeLayer(pActive_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(7)),
			new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(7))}, 4, 4, 0, 0);
		pTransistorActiveTLayer = new Technology.NodeLayer(pActive_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromTop(10)),
			new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(7))}, 4, 4, 0, 0);
		pTransistorActiveBLayer = new Technology.NodeLayer(pActive_lay, 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(7)),
			new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromBottom(10))}, 4, 4, 0, 0);
		pTransistorWellLayer = new Technology.NodeLayer(nWell_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(1))}, 10, 10, 6, 6);
		pTransistorSelectLayer = new Technology.NodeLayer(pSelect_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(5)),
			new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(5))}, 6, 6, 2, 2);
		pTransistor_node = PrimitiveNode.newInstance("P-Transistor", this, 15.0, 22.0, new SizeOffset(6, 6, 10, 10),
			new Technology.NodeLayer [] {pTransistorActiveLayer, pTransistorPolyLayer, pTransistorWellLayer, pTransistorSelectLayer});
		pTransistor_node.setElectricalLayers(new Technology.NodeLayer [] {pTransistorActiveTLayer, pTransistorActiveBLayer,
			pTransistorPolyCLayer, pTransistorPolyLLayer, pTransistorPolyRLayer, pTransistorWellLayer, pTransistorSelectLayer});
		pTransistor_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pTransistor_node, new ArcProto[] {poly1_arc}, "p-trans-poly-left", 180,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(11), EdgeH.fromLeft(4), EdgeV.fromTop(11)),
				PrimitivePort.newInstance(this, pTransistor_node, new ArcProto[] {pActive_arc}, "p-trans-diff-top", 90,90, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromTop(7.5), EdgeH.fromRight(7.5), EdgeV.fromTop(7)),
				PrimitivePort.newInstance(this, pTransistor_node, new ArcProto[] {poly1_arc}, "p-trans-poly-right", 0,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(4), EdgeV.fromBottom(11), EdgeH.fromRight(4), EdgeV.fromTop(11)),
				PrimitivePort.newInstance(this, pTransistor_node, new ArcProto[] {pActive_arc}, "p-trans-diff-bottom", 270,90, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromBottom(7), EdgeH.fromRight(7.5), EdgeV.fromBottom(7.5))
			});
		pTransistor_node.setFunction(NodeProto.Function.TRAPMOS);
		pTransistor_node.setHoldsOutline();
		pTransistor_node.setCanShrink();
		pTransistor_node.setSpecialType(PrimitiveNode.SERPTRANS);
		pTransistor_node.setSpecialValues(new double [] {7, 1.5, 2.5, 2, 1, 2});
		pTransistor_node.setMinSize(15, 22, "2.1, 3.1");

		/** N-Transistor */
		nTransistorPolyLayer = new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(10)),
			new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(10))}, 1, 1, 2, 2);
		nTransistorPolyLLayer = new Technology.NodeLayer(poly1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(10)),
			new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromTop(10))}, 1, 1, 2, 2);
		nTransistorPolyRLayer = new Technology.NodeLayer(poly1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromBottom(10)),
			new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(10))}, 1, 1, 2, 2);
		nTransistorPolyCLayer = new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(10)),
			new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(10))}, 1, 1, 2, 2);
		nTransistorActiveLayer = new Technology.NodeLayer(nActive_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(7)),
			new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(7))}, 4, 4, 0, 0);
		nTransistorActiveTLayer = new Technology.NodeLayer(nActive_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromTop(10)),
			new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(7))}, 4, 4, 0, 0);
		nTransistorActiveBLayer = new Technology.NodeLayer(nActive_lay, 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(7)),
			new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromBottom(10))}, 4, 4, 0, 0);
		nTransistorWellLayer = new Technology.NodeLayer(pWell_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(1)),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.fromTop(1))}, 10, 10, 6, 6);
		nTransistorSelectLayer = new Technology.NodeLayer(nSelect_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.fromLeft(4), EdgeV.fromBottom(5)),
			new Technology.TechPoint(EdgeH.fromRight(4), EdgeV.fromTop(5))}, 6, 6, 2, 2);
		nTransistor_node = PrimitiveNode.newInstance("N-Transistor", this, 15.0, 22.0, new SizeOffset(6, 6, 10, 10),
			new Technology.NodeLayer [] {nTransistorActiveLayer, nTransistorPolyLayer, nTransistorWellLayer, nTransistorSelectLayer});
		nTransistor_node.setElectricalLayers(new Technology.NodeLayer [] {nTransistorActiveTLayer, nTransistorActiveBLayer,
			nTransistorPolyLLayer, nTransistorPolyRLayer, nTransistorPolyCLayer, nTransistorWellLayer, nTransistorSelectLayer});
		nTransistor_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, nTransistor_node, new ArcProto[] {poly1_arc}, "n-trans-poly-left", 180,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(4), EdgeV.fromBottom(11), EdgeH.fromLeft(4), EdgeV.fromTop(11)),
				PrimitivePort.newInstance(this, nTransistor_node, new ArcProto[] {nActive_arc}, "n-trans-diff-top", 90,90, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromTop(7.5), EdgeH.fromRight(7.5), EdgeV.fromTop(7)),
				PrimitivePort.newInstance(this, nTransistor_node, new ArcProto[] {poly1_arc}, "n-trans-poly-right", 0,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(4), EdgeV.fromBottom(11), EdgeH.fromRight(4), EdgeV.fromTop(11)),
				PrimitivePort.newInstance(this, nTransistor_node, new ArcProto[] {nActive_arc}, "n-trans-diff-bottom", 270,90, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(7.5), EdgeV.fromBottom(7), EdgeH.fromRight(7.5), EdgeV.fromBottom(7.5))
			});
		nTransistor_node.setFunction(NodeProto.Function.TRANMOS);
		nTransistor_node.setHoldsOutline();
		nTransistor_node.setCanShrink();
		nTransistor_node.setSpecialType(PrimitiveNode.SERPTRANS);
		nTransistor_node.setSpecialValues(new double [] {7, 1.5, 2.5, 2, 1, 2});
		nTransistor_node.setMinSize(15, 22, "2.1, 3.1");

		/** Scalable-P-Transistor */
		scalablePTransistor_node = PrimitiveNode.newInstance("P-Transistor-Scalable", this, 17.0, 26.0, new SizeOffset(7, 7, 12, 12),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pActive_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromTop(6)),
					new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(11))}),
				new Technology.NodeLayer(metal1_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromTop(6.5)),
					new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromTop(10.5))}),
				new Technology.NodeLayer(pActive_lay, 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(11)),
					new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromBottom(6))}),
				new Technology.NodeLayer(metal1_lay, 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromBottom(10.5)),
					new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromBottom(6.5))}),
				new Technology.NodeLayer(pActive_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7), EdgeV.fromBottom(9)),
					new Technology.TechPoint(EdgeH.fromRight(7), EdgeV.fromTop(9))}),
				new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(5), EdgeV.fromBottom(12)),
					new Technology.TechPoint(EdgeH.fromRight(5), EdgeV.fromTop(12))}),
				new Technology.NodeLayer(nWell_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(pSelect_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4)),
				new Technology.NodeLayer(activeCut_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromBottom(9.5)),
					new Technology.TechPoint(EdgeH.fromLeft(9.5), EdgeV.fromBottom(7.5))}),
				new Technology.NodeLayer(activeCut_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromTop(7.5)),
					new Technology.TechPoint(EdgeH.fromLeft(9.5), EdgeV.fromTop(9.5))})
			});
		scalablePTransistor_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, scalablePTransistor_node, new ArcProto[] {poly1_arc}, "p-trans-sca-poly-left", 180,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromCenter(-3.5), EdgeV.makeCenter(), EdgeH.fromCenter(-3.5), EdgeV.makeCenter()),
				PrimitivePort.newInstance(this, scalablePTransistor_node, new ArcProto[] {pActive_arc, metal1_arc}, "p-trans-sca-diff-top", 90,90, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.fromCenter(4.5), EdgeH.makeCenter(), EdgeV.fromCenter(4.5)),
				PrimitivePort.newInstance(this, scalablePTransistor_node, new ArcProto[] {poly1_arc}, "p-trans-sca-poly-right", 0,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromCenter(3.5), EdgeV.makeCenter(), EdgeH.fromCenter(3.5), EdgeV.makeCenter()),
				PrimitivePort.newInstance(this, scalablePTransistor_node, new ArcProto[] {pActive_arc, metal1_arc}, "p-trans-sca-diff-bottom", 270,90, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.fromCenter(-4.5), EdgeH.makeCenter(), EdgeV.fromCenter(-4.5))
			});
		scalablePTransistor_node.setFunction(NodeProto.Function.TRAPMOS);
		scalablePTransistor_node.setCanShrink();
		scalablePTransistor_node.setNotUsed();
		scalablePTransistor_node.setMinSize(17, 26, "2.1, 3.1");

		/** Scalable-N-Transistor */
		scalableNTransistor_node = PrimitiveNode.newInstance("N-Transistor-Scalable", this, 17.0, 26.0, new SizeOffset(7, 7, 12, 12),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(nActive_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromTop(6)),
					new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromTop(11))}),
				new Technology.NodeLayer(metal1_lay, 1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromTop(6.5)),
					new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromTop(10.5))}),
				new Technology.NodeLayer(nActive_lay, 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6), EdgeV.fromBottom(11)),
					new Technology.TechPoint(EdgeH.fromRight(6), EdgeV.fromBottom(6))}),
				new Technology.NodeLayer(metal1_lay, 3, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(6.5), EdgeV.fromBottom(10.5)),
					new Technology.TechPoint(EdgeH.fromRight(6.5), EdgeV.fromBottom(6.5))}),
				new Technology.NodeLayer(nActive_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7), EdgeV.fromBottom(9)),
					new Technology.TechPoint(EdgeH.fromRight(7), EdgeV.fromTop(9))}),
				new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(5), EdgeV.fromBottom(12)),
					new Technology.TechPoint(EdgeH.fromRight(5), EdgeV.fromTop(12))}),
				new Technology.NodeLayer(pWell_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(nSelect_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4)),
				new Technology.NodeLayer(activeCut_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromBottom(9.5)),
					new Technology.TechPoint(EdgeH.fromLeft(9.5), EdgeV.fromBottom(7.5))}),
				new Technology.NodeLayer(activeCut_lay, -1, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(7.5), EdgeV.fromTop(7.5)),
					new Technology.TechPoint(EdgeH.fromLeft(9.5), EdgeV.fromTop(9.5))})
			});
		scalableNTransistor_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, scalableNTransistor_node, new ArcProto[] {poly1_arc}, "n-trans-sca-poly-left", 180,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromCenter(-3.5), EdgeV.makeCenter(), EdgeH.fromCenter(-3.5), EdgeV.makeCenter()),
				PrimitivePort.newInstance(this, scalableNTransistor_node, new ArcProto[] {nActive_arc, metal1_arc}, "n-trans-sca-diff-top", 90,90, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.fromCenter(4.5), EdgeH.makeCenter(), EdgeV.fromCenter(4.5)),
				PrimitivePort.newInstance(this, scalableNTransistor_node, new ArcProto[] {poly1_arc}, "n-trans-sca-poly-right", 0,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromCenter(3.5), EdgeV.makeCenter(), EdgeH.fromCenter(3.5), EdgeV.makeCenter()),
				PrimitivePort.newInstance(this, scalableNTransistor_node, new ArcProto[] {nActive_arc, metal1_arc}, "n-trans-sca-diff-bottom", 270,90, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.fromCenter(-4.5), EdgeH.makeCenter(), EdgeV.fromCenter(-4.5))
			});
		scalableNTransistor_node.setFunction(NodeProto.Function.TRANMOS);
		scalableNTransistor_node.setCanShrink();
		scalableNTransistor_node.setNotUsed();
		scalableNTransistor_node.setMinSize(17, 26, "2.1, 3.1");

		/** metal-1-metal-2-contact */
		metal1Metal2Contact_node = PrimitiveNode.newInstance("Metal-1-Metal-2-Con", this, 5.0, 5.0, new SizeOffset(0.5, 0.5, 0.5, 0.5),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(0.5)),
				new Technology.NodeLayer(metal2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(0.5)),
				new Technology.NodeLayer(via1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1.5))
			});
		metal1Metal2Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1Metal2Contact_node, new ArcProto[] {metal1_arc, metal2_arc}, "metal-1-metal-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal1Metal2Contact_node.setFunction(NodeProto.Function.CONTACT);
		metal1Metal2Contact_node.setSpecialType(PrimitiveNode.MULTICUT);
		metal1Metal2Contact_node.setSpecialValues(new double [] {2, 2, 1, 3});
		metal1Metal2Contact_node.setMinSize(5, 5, "8.3, 9.3");

		/** metal-2-metal-3-contact */
		metal2Metal3Contact_node = PrimitiveNode.newInstance("Metal-2-Metal-3-Con", this, 6.0, 6.0, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1)),
				new Technology.NodeLayer(metal3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1)),
				new Technology.NodeLayer(via2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2))
			});
		metal2Metal3Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal2Metal3Contact_node, new ArcProto[] {metal2_arc, metal3_arc}, "metal-2-metal-3", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5), EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))
			});
		metal2Metal3Contact_node.setFunction(NodeProto.Function.CONTACT);
		metal2Metal3Contact_node.setSpecialType(PrimitiveNode.MULTICUT);
		metal2Metal3Contact_node.setSpecialValues(new double [] {2, 2, 1, 3});
		metal2Metal3Contact_node.setMinSize(6, 6, "14.3, 15.3");

		/** metal-3-metal-4-contact */
		metal3Metal4Contact_node = PrimitiveNode.newInstance("Metal-3-Metal-4-Con", this, 6.0, 6.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1)),
				new Technology.NodeLayer(metal4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(via3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2))
			});
		metal3Metal4Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal3Metal4Contact_node, new ArcProto[] {metal3_arc, metal4_arc}, "metal-3-metal-4", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5), EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))
			});
		metal3Metal4Contact_node.setFunction(NodeProto.Function.CONTACT);
		metal3Metal4Contact_node.setSpecialType(PrimitiveNode.MULTICUT);
		metal3Metal4Contact_node.setSpecialValues(new double [] {2, 2, 2, 3});
		metal3Metal4Contact_node.setMinSize(6, 6, "21.3, 22.3");

		/** metal-4-metal-5-contact */
		metal4Metal5Contact_node = PrimitiveNode.newInstance("Metal-4-Metal-5-Con", this, 7.0, 7.0, new SizeOffset(1.5, 1.5, 1.5, 1.5),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1.5)),
				new Technology.NodeLayer(metal5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1.5)),
				new Technology.NodeLayer(via4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2.5))
			});
		metal4Metal5Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal4Metal5Contact_node, new ArcProto[] {metal4_arc, metal5_arc}, "metal-4-metal-5", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5), EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))
			});
		metal4Metal5Contact_node.setFunction(NodeProto.Function.CONTACT);
		metal4Metal5Contact_node.setSpecialType(PrimitiveNode.MULTICUT);
		metal4Metal5Contact_node.setSpecialValues(new double [] {2, 2, 1, 3});
		metal4Metal5Contact_node.setNotUsed();
		metal4Metal5Contact_node.setMinSize(7, 8, "25.3, 26.3");

		/** metal-5-metal-6-contact */
		metal5Metal6Contact_node = PrimitiveNode.newInstance("Metal-5-Metal-6-Con", this, 8.0, 8.0, new SizeOffset(1, 1, 1, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1)),
				new Technology.NodeLayer(metal6_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(1)),
				new Technology.NodeLayer(via5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(3))
			});
		metal5Metal6Contact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal5Metal6Contact_node, new ArcProto[] {metal5_arc, metal6_arc}, "metal-5-metal-6", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(2.5), EdgeV.fromBottom(2.5), EdgeH.fromRight(2.5), EdgeV.fromTop(2.5))
			});
		metal5Metal6Contact_node.setFunction(NodeProto.Function.CONTACT);
		metal5Metal6Contact_node.setSpecialType(PrimitiveNode.MULTICUT);
		metal5Metal6Contact_node.setSpecialValues(new double [] {3, 3, 2, 4});
		metal5Metal6Contact_node.setNotUsed();
		metal5Metal6Contact_node.setMinSize(8, 8, "29.3, 30.3");

		/** Metal-1-P-Well Contact */
		metal1PWellContact_node = PrimitiveNode.newInstance("Metal-1-P-Well-Con", this, 17.0, 17.0, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6.5)),
				new Technology.NodeLayer(pActiveWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6)),
				new Technology.NodeLayer(pWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(pSelect_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4)),
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(7.5))
			});
		metal1PWellContact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1PWellContact_node, new ArcProto[] {metal1_arc, active_arc}, "metal-1-well", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(8), EdgeV.fromBottom(8), EdgeH.fromRight(8), EdgeV.fromTop(8))
			});
		metal1PWellContact_node.setFunction(NodeProto.Function.WELL);
		metal1PWellContact_node.setSpecialType(PrimitiveNode.MULTICUT);
		metal1PWellContact_node.setSpecialValues(new double [] {2, 2, 1.5, 3});
		metal1PWellContact_node.setMinSize(17, 17, "4.2, 6.2, 7.3");

		/** Metal-1-N-Well Contact */
		metal1NWellContact_node = PrimitiveNode.newInstance("Metal-1-N-Well-Con", this, 17.0, 17.0, new SizeOffset(6, 6, 6, 6),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6.5)),
				new Technology.NodeLayer(nActive_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(6)),
				new Technology.NodeLayer(nWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(nSelect_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(4)),
				new Technology.NodeLayer(activeCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(7.5))
			});
		metal1NWellContact_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1NWellContact_node, new ArcProto[] {metal1_arc, active_arc}, "metal-1-substrate", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(8), EdgeV.fromBottom(8), EdgeH.fromRight(8), EdgeV.fromTop(8))
			});
		metal1NWellContact_node.setFunction(NodeProto.Function.SUBSTRATE);
		metal1NWellContact_node.setSpecialType(PrimitiveNode.MULTICUT);
		metal1NWellContact_node.setSpecialValues(new double [] {2, 2, 1.5, 3});
		metal1NWellContact_node.setMinSize(17, 17, "4.2, 6.2, 7.3");

		/** Metal-1-Node */
		metal1Node_node = PrimitiveNode.newInstance("Metal-1-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal1_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal1Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal1Node_node, new ArcProto[] {metal1_arc}, "metal-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal1Node_node.setFunction(NodeProto.Function.NODE);
		metal1Node_node.setHoldsOutline();
		metal1Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-2-Node */
		metal2Node_node = PrimitiveNode.newInstance("Metal-2-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal2_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal2Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal2Node_node, new ArcProto[] {metal2_arc}, "metal-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal2Node_node.setFunction(NodeProto.Function.NODE);
		metal2Node_node.setHoldsOutline();
		metal2Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-3-Node */
		metal3Node_node = PrimitiveNode.newInstance("Metal-3-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal3_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal3Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal3Node_node, new ArcProto[] {metal3_arc}, "metal-3", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal3Node_node.setFunction(NodeProto.Function.NODE);
		metal3Node_node.setHoldsOutline();
		metal3Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-4-Node */
		metal4Node_node = PrimitiveNode.newInstance("Metal-4-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal4_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal4Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal4Node_node, new ArcProto[] {metal4_arc}, "metal-4", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal4Node_node.setFunction(NodeProto.Function.NODE);
		metal4Node_node.setHoldsOutline();
		metal4Node_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Metal-5-Node */
		metal5Node_node = PrimitiveNode.newInstance("Metal-5-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal5_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal5Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal5Node_node, new ArcProto[] {metal5_arc}, "metal-5", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal5Node_node.setFunction(NodeProto.Function.NODE);
		metal5Node_node.setHoldsOutline();
		metal5Node_node.setSpecialType(PrimitiveNode.POLYGONAL);
		metal5Node_node.setNotUsed();

		/** Metal-6-Node */
		metal6Node_node = PrimitiveNode.newInstance("Metal-6-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(metal6_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		metal6Node_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, metal6Node_node, new ArcProto[] {metal6_arc}, "metal-6", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		metal6Node_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, poly1Node_node, new ArcProto[] {poly1_arc}, "polysilicon-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		poly1Node_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, poly2Node_node, new ArcProto[] {poly2_arc}, "polysilicon-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		poly2Node_node.setFunction(NodeProto.Function.NODE);
		poly2Node_node.setHoldsOutline();
		poly2Node_node.setSpecialType(PrimitiveNode.POLYGONAL);
		poly2Node_node.setNotUsed();

		/** P-Active-Node */
		pActiveNode_node = PrimitiveNode.newInstance("P-Active-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pActive_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		pActiveNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pActiveNode_node, new ArcProto[] {active_arc, pActive_arc, nActive_arc}, "active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		pActiveNode_node.setFunction(NodeProto.Function.NODE);
		pActiveNode_node.setHoldsOutline();
		pActiveNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** N-Active-Node */
		nActiveNode_node = PrimitiveNode.newInstance("N-Active-Node", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(nActive_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		nActiveNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, nActiveNode_node, new ArcProto[] {active_arc, pActive_arc, nActive_arc}, "active", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1.5), EdgeV.fromBottom(1.5), EdgeH.fromRight(1.5), EdgeV.fromTop(1.5))
			});
		nActiveNode_node.setFunction(NodeProto.Function.NODE);
		nActiveNode_node.setHoldsOutline();
		nActiveNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** P-Select-Node */
		pSelectNode_node = PrimitiveNode.newInstance("P-Select-Node", this, 4.0, 4.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pSelect_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		pSelectNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pSelectNode_node, new ArcProto[0], "select", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pSelectNode_node.setFunction(NodeProto.Function.NODE);
		pSelectNode_node.setHoldsOutline();
		pSelectNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** N-Select-Node */
		nSelectNode_node = PrimitiveNode.newInstance("N-Select-Node", this, 4.0, 4.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(nSelect_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		nSelectNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, nSelectNode_node, new ArcProto[0], "select", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		nSelectNode_node.setFunction(NodeProto.Function.NODE);
		nSelectNode_node.setHoldsOutline();
		nSelectNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** PolyCut-Node */
		polyCutNode_node = PrimitiveNode.newInstance("Poly-Cut-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(polyCut_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		polyCutNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, polyCutNode_node, new ArcProto[0], "polycut", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		polyCutNode_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, activeCutNode_node, new ArcProto[0], "activecut", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		activeCutNode_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, via1Node_node, new ArcProto[0], "via-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		via1Node_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, via2Node_node, new ArcProto[0], "via-2", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		via2Node_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, via3Node_node, new ArcProto[0], "via-3", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		via3Node_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, via4Node_node, new ArcProto[0], "via-4", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		via4Node_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, via5Node_node, new ArcProto[0], "via-5", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		via5Node_node.setFunction(NodeProto.Function.NODE);
		via5Node_node.setHoldsOutline();
		via5Node_node.setSpecialType(PrimitiveNode.POLYGONAL);
		via5Node_node.setNotUsed();

		/** P-Well-Node */
		pWellNode_node = PrimitiveNode.newInstance("P-Well-Node", this, 12.0, 12.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(pWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		pWellNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, pWellNode_node, new ArcProto[] {pActive_arc}, "well", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		pWellNode_node.setFunction(NodeProto.Function.NODE);
		pWellNode_node.setHoldsOutline();
		pWellNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** N-Well-Node */
		nWellNode_node = PrimitiveNode.newInstance("N-Well-Node", this, 12.0, 12.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(nWell_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		nWellNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, nWellNode_node, new ArcProto[] {pActive_arc}, "well", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(3), EdgeV.fromBottom(3), EdgeH.fromRight(3), EdgeV.fromTop(3))
			});
		nWellNode_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, passivationNode_node, new ArcProto[0], "passivation", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		passivationNode_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, padFrameNode_node, new ArcProto[0], "pad-frame", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		padFrameNode_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, polyCapNode_node, new ArcProto[0], "poly-cap", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		polyCapNode_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, pActiveWellNode_node, new ArcProto[0], "p-active-well", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		pActiveWellNode_node.setFunction(NodeProto.Function.NODE);
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
				PrimitivePort.newInstance(this, polyTransistorNode_node, new ArcProto[] {poly1_arc}, "trans-poly-1", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromRight(1), EdgeV.fromTop(1))
			});
		polyTransistorNode_node.setFunction(NodeProto.Function.NODE);
		polyTransistorNode_node.setHoldsOutline();
		polyTransistorNode_node.setSpecialType(PrimitiveNode.POLYGONAL);

		/** Silicide-Block-Node */
		silicideBlockNode_node = PrimitiveNode.newInstance("Silicide-Block-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(transistorPoly_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		silicideBlockNode_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, silicideBlockNode_node, new ArcProto[] {poly1_arc}, "silicide-block", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		silicideBlockNode_node.setFunction(NodeProto.Function.NODE);
		silicideBlockNode_node.setHoldsOutline();
		silicideBlockNode_node.setSpecialType(PrimitiveNode.POLYGONAL);
	}

	/******************** SUPPORT METHODS ********************/

	/**
	 * Method for initializing this technology.
	 */
	public void init()
	{
		setState();
	}

	/**
	 * Method to set the design rules back to the "factory" state for the given options.
	 */
	public void factoryReset()
	{
	}

	/**
	 * Method to set the technology to state "newstate", which encodes the number of metal
	 * layers, whether it is a deep process, and other rules.
	 */
	private void setState()
	{
//		// set stick-figure state
//		if ((state&MOCMOSSTICKFIGURE) != 0)
//		{
//			// stick figure drawing
//			tech->nodesizeoffset = nodesizeoffset;
//			tech->arcpolys = arcpolys;
//			tech->shapearcpoly = shapearcpoly;
//			tech->allarcpolys = allarcpolys;
//			tech->arcwidthoffset = arcwidthoffset;
//		} else
//		{
//			// full figure drawing
//			tech->nodesizeoffset = 0;
//			tech->arcpolys = 0;
//			tech->shapearcpoly = 0;
//			tech->allarcpolys = 0;
//			tech->arcwidthoffset = 0;
//		}

		// set rules
		if (loadDRCtables()) return;

		// handle special transistors
		if (isSpecialTransistors())
		{
			// show special transistors
			scalablePTransistor_node.clearNotUsed();
			scalableNTransistor_node.clearNotUsed();
		} else
		{
			// hide special transistors
			scalablePTransistor_node.setNotUsed();
			scalableNTransistor_node.setNotUsed();
		}

		// disable Metal-3/4/5/6-Pin, Metal-2/3/4/5-Metal-3/4/5/6-Con, Metal-3/4/5/6-Node, Via-2/3/4/5-Node
		metal3Pin_node.setNotUsed();
		metal4Pin_node.setNotUsed();
		metal5Pin_node.setNotUsed();
		metal6Pin_node.setNotUsed();
		metal2Metal3Contact_node.setNotUsed();
		metal3Metal4Contact_node.setNotUsed();
		metal4Metal5Contact_node.setNotUsed();
		metal5Metal6Contact_node.setNotUsed();
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
		metal3_arc.setNotUsed();
		metal4_arc.setNotUsed();
		metal5_arc.setNotUsed();
		metal6_arc.setNotUsed();

		// enable the desired nodes
		switch (getNumMetal())
		{
			case 6:
				metal6Pin_node.clearNotUsed();
				metal5Metal6Contact_node.clearNotUsed();
				metal6Node_node.clearNotUsed();
				via5Node_node.clearNotUsed();
				metal6_arc.clearNotUsed();
				// FALLTHROUGH 
			case 5:
				metal5Pin_node.clearNotUsed();
				metal4Metal5Contact_node.clearNotUsed();
				metal5Node_node.clearNotUsed();
				via4Node_node.clearNotUsed();
				metal5_arc.clearNotUsed();
				// FALLTHROUGH 
			case 4:
				metal4Pin_node.clearNotUsed();
				metal3Metal4Contact_node.clearNotUsed();
				metal4Node_node.clearNotUsed();
				via3Node_node.clearNotUsed();
				metal4_arc.clearNotUsed();
				// FALLTHROUGH 
			case 3:
				metal3Pin_node.clearNotUsed();
				metal2Metal3Contact_node.clearNotUsed();
				metal3Node_node.clearNotUsed();
				via2Node_node.clearNotUsed();
				metal3_arc.clearNotUsed();
				break;
		}
		if (getRuleSet() != DEEPRULES)
		{
			if (!isSecondPolysilicon())
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
		String description = "Complementary MOS (from MOSIS, 2-6 metals [now " + numMetals + "], 1-2 polys [now " +
			numPolys + "], flex rules [" + rules + "]";
		if (isDisallowStackedVias()) description += ", stacked vias disallowed";
		if (isAlternateActivePolyRules()) description += ", alternate contact rules";
		if (isSpecialTransistors()) description += ", shows special transistors";
		if (isStickFigures()) description += ", stick-figures";
		return description + ")";
	}

	/******************** PARAMETERIZABLE DESIGN RULES ********************/

	/**
	 * Method to remove all information in the design rule tables.
	 * Returns true on error.
	 */
	private boolean loadDRCtables()
	{
		// allocate local copy of DRC tables
		int maxLayers = getNumLayers();
		int tot = (maxLayers * maxLayers + maxLayers) / 2;

		Double [] conDist = new Double[tot];
		String [] conDistRules = new String[tot];
		Double [] unConDist = new Double[tot];
		String [] unConDistRules = new String[tot];

		Double [] conDistWide = new Double[tot];
		String [] conDistWideRules = new String[tot];
		Double [] unConDistWide = new Double[tot];
		String [] unConDistWideRules = new String[tot];

		Double [] conDistMulti = new Double[tot];
		String [] conDistMultiRules = new String[tot];
		Double [] unConDistMulti = new Double[tot];
		String [] unConDistMultiRules = new String[tot];

		Double [] edgeDist = new Double[tot];
		String [] edgeDistRules = new String[tot];

		Double [] minSize = new Double[maxLayers];
		String [] minSizeRules = new String[maxLayers];

		// clear all rules
		for(int i=0; i<tot; i++)
		{
			conDist[i] = new Double(-1);         conDistRules[i] = "";
			unConDist[i] = new Double(-1);       unConDistRules[i] = "";

			conDistWide[i] = new Double(-1);     conDistWideRules[i] = "";
			unConDistWide[i] = new Double(-1);   unConDistWideRules[i] = "";

			conDistMulti[i] = new Double(-1);    conDistMultiRules[i] = "";
			unConDistMulti[i] = new Double(-1);  unConDistMultiRules[i] = "";

			edgeDist[i] = new Double(-1);        edgeDistRules[i] = "";
		}
		for(int i=0; i<maxLayers; i++)
		{
			minSize[i] = new Double(-1);         minSizeRules[i] = "";
		}

		// load the DRC tables from the explanation table
		boolean errorfound = false;
		for(int pass=0; pass<2; pass++)
		{
			for(int i=0; i < theRules.length; i++)
			{
				// see if the rule applies
				if (pass == 0)
				{
					if (theRules[i].ruleType == NODSIZ) continue;
				} else
				{
					if (theRules[i].ruleType != NODSIZ) continue;
				}

				int when = theRules[i].when;
				boolean goodrule = true;
				if ((when&(DE|SU|SC)) != 0)
				{
					switch (getRuleSet())
					{
						case DEEPRULES:  if ((when&DE) == 0) goodrule = false;   break;
						case SUBMRULES:  if ((when&SU) == 0) goodrule = false;   break;
						case SCMOSRULES: if ((when&SC) == 0) goodrule = false;   break;
					}
					if (!goodrule) continue;
				}
				if ((when&(M2|M3|M4|M5|M6)) != 0)
				{
					switch (getNumMetal())
					{
						case 2:  if ((when&M2) == 0) goodrule = false;   break;
						case 3:  if ((when&M3) == 0) goodrule = false;   break;
						case 4:  if ((when&M4) == 0) goodrule = false;   break;
						case 5:  if ((when&M5) == 0) goodrule = false;   break;
						case 6:  if ((when&M6) == 0) goodrule = false;   break;
					}
					if (!goodrule) continue;
				}
				if ((when&AC) != 0)
				{
					if (!isAlternateActivePolyRules()) continue;
				}
				if ((when&NAC) != 0)
				{
					if (isAlternateActivePolyRules()) continue;
				}
				if ((when&SV) != 0)
				{
					if (isDisallowStackedVias()) continue;
				}
				if ((when&NSV) != 0)
				{
					if (!isDisallowStackedVias()) continue;
				}

				// find the layer names
				Layer lay1 = null;
				int layert1 = -1;
				if (theRules[i].layer1 != null)
				{
					lay1 = findLayer(theRules[i].layer1);
					if (lay1 == null)
					{
						System.out.println("Warning: no layer '" + theRules[i].layer1 + "' in mocmos technology");
						errorfound = true;
						break;
					}
					layert1 = lay1.getIndex();
				}
				Layer lay2 = null;
				int layert2 = -1;
				if (theRules[i].layer2 != null)
				{
					lay2 = findLayer(theRules[i].layer2);
					if (lay2 == null)
					{
						System.out.println("Warning: no layer '" + theRules[i].layer2 + "' in mocmos technology");
						errorfound = true;
						break;
					}
					layert2 = lay2.getIndex();
				}

				// find the index in a two-layer upper-diagonal table
				int index = -1;
				if (layert1 >= 0 && layert2 >= 0)
				{
					if (layert1 > layert2) {int temp = layert1; layert1 = layert2;  layert2 = temp; }
					index = (layert1+1) * (layert1/2) + (layert1&1) * ((layert1+1)/2);
					index = layert2 + maxLayers * layert1 - index;
				}

				// find the nodes and arcs associated with the rule
				PrimitiveNode nty = null;
				PrimitiveArc aty = null;
				if (theRules[i].nodeName != null)
				{
					if (theRules[i].ruleType == ASURROUND)
					{
						aty = this.findArcProto(theRules[i].nodeName);
						if (aty == null)
						{
							System.out.println("Warning: no arc '" + theRules[i].nodeName + "' in mocmos technology");
							errorfound = true;
							break;
						}
					} else
					{
						nty = this.findNodeProto(theRules[i].nodeName);
						if (nty == null)
						{
							System.out.println("Warning: no node '" + theRules[i].nodeName + "' in mocmos technology");
							errorfound = true;
							break;
						}
					}
				}

				// get more information about the rule
				double distance = theRules[i].distance;
				String proc = "";
				if ((when&(DE|SU|SC)) != 0)
				{
					switch (getRuleSet())
					{
						case DEEPRULES:  proc = "DEEP";   break;
						case SUBMRULES:  proc = "SUBM";   break;
						case SCMOSRULES: proc = "SCMOS";  break;
					}
				}
				String metal = "";
				if ((when&(M2|M3|M4|M5|M6)) != 0)
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
				String rule = theRules[i].rule;
				if (proc.length() > 0 || metal.length() > 0)
					rule += ", " +  metal + proc;

				// set the rule
				double [] specValues;
				switch (theRules[i].ruleType)
				{
					case MINWID:
						minSize[layert1] = new Double(distance);
						minSizeRules[layert1] = rule;
						setLayerMinWidth(theRules[i].layer1, distance);
						break;
					case NODSIZ:
						int node=0;
						setDefNodeSize(nty, node, distance, distance);
						break;
					case SURROUND:
						setLayerSurroundLayer(nty, lay1, lay2, distance, minSize);
						break;
					case ASURROUND:
						setArcLayerSurroundLayer(aty, lay1, lay2, distance);
						break;
					case VIASUR:
						setLayerSurroundVia(nty, lay1, distance);
						specValues = nty.getSpecialValues();
						specValues[2] = distance;
						break;
					case TRAWELL:
						setTransistorWellSurround(distance);
						break;
					case TRAPOLY:
						setTransistorPolyOverhang(distance);
						break;
					case TRAACTIVE:
						setTransistorActiveOverhang(distance);
						break;
					case SPACING:
						conDist[index] = new Double(distance);
						unConDist[index] = new Double(distance);
						conDistRules[index] = rule;
						unConDistRules[index] = rule;
						break;
					case SPACINGM:
						conDistMulti[index] = new Double(distance);
						unConDistMulti[index] = new Double(distance);
						conDistMultiRules[index] = rule;
						unConDistMultiRules[index] = rule;
						break;
					case SPACINGW:
						conDistWide[index] = new Double(distance);
						unConDistWide[index] = new Double(distance);
						conDistWideRules[index] = rule;
						unConDistWideRules[index] = rule;
						break;
					case SPACINGE:
						edgeDist[index] = new Double(distance);
						edgeDistRules[index] = rule;
						break;
					case CONSPA:
						conDist[index] = new Double(distance);
						conDistRules[index] = rule;
						break;
					case UCONSPA:
						unConDist[index] = new Double(distance);
						unConDistRules[index] = rule;
						break;
					case CUTSPA:
						specValues = nty.getSpecialValues();
						specValues[3] = distance;
						break;
					case CUTSIZE:
						specValues = nty.getSpecialValues();
						specValues[0] = specValues[1] = distance;
						break;
					case CUTSUR:
						specValues = nty.getSpecialValues();
						specValues[2] = distance;
						break;
				}
			}
		}
		if (!errorfound)
		{
			// set the rules on the technology
			Variable var = newVar(DRC.MIN_CONNECTED_DISTANCES, conDist);
			if (var != null) var.setDontSave();
			var = newVar(DRC.MIN_CONNECTED_DISTANCES_RULE, conDistRules);
			if (var != null) var.setDontSave();
			var = newVar(DRC.MIN_UNCONNECTED_DISTANCES, unConDist);
			if (var != null) var.setDontSave();
			var = newVar(DRC.MIN_UNCONNECTED_DISTANCES_RULE, unConDistRules);
			if (var != null) var.setDontSave();

			var = newVar(DRC.MIN_CONNECTED_DISTANCES_WIDE, conDistWide);
			if (var != null) var.setDontSave();
			var = newVar(DRC.MIN_CONNECTED_DISTANCES_WIDE_RULE, conDistWideRules);
			if (var != null) var.setDontSave();
			var = newVar(DRC.MIN_UNCONNECTED_DISTANCES_WIDE, unConDistWide);
			if (var != null) var.setDontSave();
			var = newVar(DRC.MIN_UNCONNECTED_DISTANCES_WIDE_RULE, unConDistWideRules);
			if (var != null) var.setDontSave();
			
			var = newVar(DRC.MIN_CONNECTED_DISTANCES_MULTI, conDistMulti);
			if (var != null) var.setDontSave();
			var = newVar(DRC.MIN_CONNECTED_DISTANCES_MULTI_RULE, conDistMultiRules);
			if (var != null) var.setDontSave();
			var = newVar(DRC.MIN_UNCONNECTED_DISTANCES_MULTI, unConDistMulti);
			if (var != null) var.setDontSave();
			var = newVar(DRC.MIN_UNCONNECTED_DISTANCES_MULTI_RULE, unConDistMultiRules);
			if (var != null) var.setDontSave();

			var = newVar(DRC.MIN_EDGE_DISTANCES, edgeDist);
			if (var != null) var.setDontSave();
			var = newVar(DRC.MIN_EDGE_DISTANCES_RULE, edgeDistRules);
			if (var != null) var.setDontSave();

			var = newVar(DRC.MIN_WIDTH, minSize);
			if (var != null) var.setDontSave();
			var = newVar(DRC.MIN_WIDTH_RULE, minSizeRules);
			if (var != null) var.setDontSave();

			var = newVar(DRC.WIDE_LIMIT, new Double(WIDELIMIT));
			if (var != null) var.setDontSave();

			// reset valid DRC dates
			DRC.resetDRCDates();
		}

		return(errorfound);
	}

	/**
	 * Method to implement rule 3.3 which specifies the amount of poly overhang
	 * on a transistor.
	 */
	private void setTransistorPolyOverhang(double overhang)
	{
		// define the poly box in terms of the central transistor box
		TechPoint [] pPolyPoints = pTransistorPolyLayer.getPoints();
		EdgeH pPolyLeft = pPolyPoints[0].getX();
		EdgeH pPolyRight = pPolyPoints[1].getX();
		pPolyLeft.setAdder(6-overhang);
		pPolyRight.setAdder(-6+overhang);
		pTransistorPolyLayer.setSerpentineExtentT(overhang);
		pTransistorPolyLayer.setSerpentineExtentB(overhang);

		TechPoint [] nPolyPoints = nTransistorPolyLayer.getPoints();
		EdgeH nPolyLeft = nPolyPoints[0].getX();
		EdgeH nPolyRight = nPolyPoints[1].getX();
		nPolyLeft.setAdder(6-overhang);
		nPolyRight.setAdder(-6+overhang);
		nTransistorPolyLayer.setSerpentineExtentT(overhang);
		nTransistorPolyLayer.setSerpentineExtentB(overhang);

		// for the electrical rule versions with split active
		TechPoint [] pPolyLPoints = pTransistorPolyLLayer.getPoints();
		TechPoint [] pPolyRPoints = pTransistorPolyRLayer.getPoints();
		EdgeH pPolyLLeft = pPolyLPoints[0].getX();
		EdgeH pPolyRRight = pPolyRPoints[1].getX();
		pPolyLLeft.setAdder(6-overhang);
		pPolyRRight.setAdder(-6+overhang);
		pTransistorPolyLLayer.setSerpentineExtentT(overhang);
		pTransistorPolyRLayer.setSerpentineExtentB(overhang);

		TechPoint [] nPolyLPoints = nTransistorPolyLLayer.getPoints();
		TechPoint [] nPolyRPoints = nTransistorPolyRLayer.getPoints();
		EdgeH nPolyLLeft = nPolyLPoints[0].getX();
		EdgeH nPolyRRight = nPolyRPoints[1].getX();
		nPolyLLeft.setAdder(6-overhang);
		nPolyRRight.setAdder(-6+overhang);
		nTransistorPolyLLayer.setSerpentineExtentT(overhang);
		nTransistorPolyRLayer.setSerpentineExtentB(overhang);
	}

	/**
	 * Method to implement rule 3.4 which specifies the amount of active overhang
	 * on a transistor.
	 */
	private void setTransistorActiveOverhang(double overhang)
	{
		TechPoint [] pActivePoints = pTransistorActiveLayer.getPoints();
		TechPoint [] nActivePoints = nTransistorActiveLayer.getPoints();
		TechPoint [] pActiveTPoints = pTransistorActiveTLayer.getPoints();
		TechPoint [] nActiveTPoints = nTransistorActiveTLayer.getPoints();
		TechPoint [] pActiveBPoints = pTransistorActiveBLayer.getPoints();
		TechPoint [] nActiveBPoints = nTransistorActiveBLayer.getPoints();
		TechPoint [] pWellPoints = pTransistorWellLayer.getPoints();
		TechPoint [] nWellPoints = nTransistorWellLayer.getPoints();
		TechPoint [] pSelectPoints = pTransistorSelectLayer.getPoints();
		TechPoint [] nSelectPoints = nTransistorSelectLayer.getPoints();

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
		SizeOffset so = pTransistor_node.getSizeOffset();
		double halfPolyWidth = (pTransistor_node.getDefHeight() - so.getHighYOffset() - so.getLowYOffset()) / 2;
		pTransistorActiveLayer.setSerpentineLWidth(halfPolyWidth+overhang);
		pTransistorActiveLayer.setSerpentineRWidth(halfPolyWidth+overhang);
		pTransistorActiveTLayer.setSerpentineRWidth(halfPolyWidth+overhang);
		pTransistorActiveBLayer.setSerpentineLWidth(halfPolyWidth+overhang);
		nTransistorActiveLayer.setSerpentineLWidth(halfPolyWidth+overhang);
		nTransistorActiveLayer.setSerpentineRWidth(halfPolyWidth+overhang);
		nTransistorActiveTLayer.setSerpentineRWidth(halfPolyWidth+overhang);
		nTransistorActiveBLayer.setSerpentineLWidth(halfPolyWidth+overhang);

		pTransistorSelectLayer.setSerpentineLWidth(halfPolyWidth+overhang+2);
		pTransistorSelectLayer.setSerpentineRWidth(halfPolyWidth+overhang+2);
		nTransistorSelectLayer.setSerpentineLWidth(halfPolyWidth+overhang+2);
		nTransistorSelectLayer.setSerpentineRWidth(halfPolyWidth+overhang+2);

		pTransistorWellLayer.setSerpentineLWidth(halfPolyWidth+overhang+wellOverhang);
		pTransistorWellLayer.setSerpentineRWidth(halfPolyWidth+overhang+wellOverhang);
		nTransistorWellLayer.setSerpentineLWidth(halfPolyWidth+overhang+wellOverhang);
		nTransistorWellLayer.setSerpentineRWidth(halfPolyWidth+overhang+wellOverhang);
	}

	/**
	 * Method to implement rule 2.3 which specifies the amount of well surround
	 * about active on a transistor.
	 */
	private void setTransistorWellSurround(double overhang)
	{
		// define the well box in terms of the active box
		TechPoint [] pActivePoints = pTransistorActiveLayer.getPoints();
		TechPoint [] nActivePoints = nTransistorActiveLayer.getPoints();
		TechPoint [] pWellPoints = pTransistorWellLayer.getPoints();
		TechPoint [] nWellPoints = nTransistorWellLayer.getPoints();

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
		pTransistorWellLayer.setSerpentineLWidth(overhang+4);
		pTransistorWellLayer.setSerpentineRWidth(overhang+4);
		nTransistorWellLayer.setSerpentineLWidth(overhang+4);
		nTransistorWellLayer.setSerpentineRWidth(overhang+4);

		pTransistorWellLayer.setSerpentineExtentT(overhang);
		pTransistorWellLayer.setSerpentineExtentB(overhang);
		nTransistorWellLayer.setSerpentineExtentT(overhang);
		nTransistorWellLayer.setSerpentineExtentB(overhang);
	}

	/**
	 * Method to change the design rules for layer "layername" layers so that
	 * the layers are at least "width" wide.  Affects the default arc width
	 * and the default pin size.
	 */
	private void setLayerMinWidth(String layername, double width)
	{
		// find the arc and set its default width
		PrimitiveArc ap = findArcProto(layername);
		if (ap == null) return;
		ap.setDefaultWidth(width + ap.getWidthOffset());

		// find the arc's pin and set its size and port offset
		PrimitiveNode np = ap.findPinProto();
		if (np != null)
		{
			SizeOffset so = np.getSizeOffset();
			double newWidth = width + so.getLowXOffset() + so.getHighXOffset();
			double newHeight = width + so.getLowYOffset() + so.getHighYOffset();
			np.setDefSize(newWidth, newHeight);

			PrimitivePort pp = (PrimitivePort)np.getPorts().next();
			EdgeH left = pp.getLeft();
			EdgeH right = pp.getRight();
			EdgeV bottom = pp.getBottom();
			EdgeV top = pp.getTop();
			double indent = newWidth / 2;
			left.setAdder(indent);
			right.setAdder(-indent);
			top.setAdder(-indent);
			bottom.setAdder(indent);
		}
	}

	/**
	 * Method to set the surround distance of layer "layer" from the via in node "nodename" to "surround".
	 */
	private void setLayerSurroundVia(PrimitiveNode nty, Layer layer, double surround)
	{
		// find the via size
		double [] specialValues = nty.getSpecialValues();
		double viasize = specialValues[0];
		double layersize = viasize + surround*2;
		double indent = (nty.getDefWidth() - layersize) / 2;

		Technology.NodeLayer oneLayer = nty.findNodeLayer(layer);
		if (oneLayer != null)
		{
			TechPoint [] points = oneLayer.getPoints();
			EdgeH left = points[0].getX();
			EdgeH right = points[1].getX();
			EdgeV bottom = points[0].getY();
			EdgeV top = points[1].getY();
			left.setAdder(indent);
			right.setAdder(-indent);
			top.setAdder(-indent);
			bottom.setAdder(indent);
		}
	}

	/**
	 * Method to set the surround distance of layer "outerlayer" from layer "innerlayer"
	 * in node "nty" to "surround".  The array "minsize" is the minimum size of each layer.
	 */
	private void setLayerSurroundLayer(PrimitiveNode nty, Layer outerLayer, Layer innerLayer, double surround, Double [] minSize)
	{
		// find the inner layer
		Technology.NodeLayer inLayer = nty.findNodeLayer(innerLayer);
		if (inLayer == null)
		{
			System.out.println("Internal error in MOCMOS surround computation");
			return;
		}

		// find the outer layer
		Technology.NodeLayer outLayer = nty.findNodeLayer(outerLayer);
		if (outLayer == null)
		{
			System.out.println("Internal error in MOCMOS surround computation");
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
		int outerLayerIndex = outerLayer.getIndex();
		double minSizeValue = minSize[outerLayerIndex].doubleValue();
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
		outLeft.setAdder(leftIndent);
		outRight.setAdder(rightIndent);
		outTop.setAdder(topIndent);
		outBottom.setAdder(bottomIndent);
	}

	/**
	 * Method to set the surround distance of layer "outerlayer" from layer "innerlayer"
	 * in arc "aty" to "surround".
	 */
	private void setArcLayerSurroundLayer(PrimitiveArc aty, Layer outerLayer, Layer innerLayer, double surround)
	{
		// find the inner layer
		Technology.ArcLayer inLayer = aty.findArcLayer(innerLayer);
		if (inLayer == null)
		{
			System.out.println("Internal error in MOCMOS surround computation");
			return;
		}

		// find the outer layer
		Technology.ArcLayer outLayer = aty.findArcLayer(outerLayer);
		if (outLayer == null)
		{
			System.out.println("Internal error in MOCMOS surround computation");
			return;
		}

		// compute the indentation of the outer layer
		double indent = inLayer.getOffset() - surround*2;
		outLayer.setOffset(indent);
	}

	/**
	 * Method to set the true node size (the highlighted area) of node "nodename" to "wid" x "hei".
	 */
	private void setDefNodeSize(PrimitiveNode nty, int index, double wid, double hei)
	{
		SizeOffset so = nty.getSizeOffset();
		double xindent = (nty.getDefWidth() - wid) / 2;
		double yindent = (nty.getDefHeight() - hei) / 2;
		nty.setSizeOffset(new SizeOffset(xindent, xindent, yindent, yindent));
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
		if (name.equals("Metal-1-Substrate-Con")) return(metal1NWellContact_node);
		if (name.equals("Metal-1-Well-Con")) return(metal1PWellContact_node);
		return null;
	}

	/******************** OPTIONS ********************/

	/**
	 * Class to extend prefs so that changes to MOSIS CMOS options will update the display.
	 */
	public static class MoCMOSPref extends Pref
	{
		protected MoCMOSPref() {}

		public void setSideEffect()
		{
			tech.setState();
			TopLevel.getPaletteFrame().loadForTechnology();
			EditWindow.repaintAllContents();
		}

		public static Pref makeBooleanPref(String name, Preferences prefs, boolean factory)
		{
			MoCMOSPref pref = new MoCMOSPref();
			pref.initBoolean(name, prefs, factory);
			return pref;
		}

		public static Pref makeIntPref(String name, Preferences prefs, int factory)
		{
			MoCMOSPref pref = new MoCMOSPref();
			pref.initInt(name, prefs, factory);
			return pref;
		}
	}

	private static Pref cacheDrawStickFigures = Pref.makeBooleanPref("MoCMOSDrawStickFigures", getTechnologyPreferences(), false);
	/**
	 * Method to determine whether this Technology is drawing stick figures.
	 * @return true if the MOCMOS technology is drawing stick figures.
	 * Returns false if it is drawing full geometry (the default).
	 */
	public static boolean isStickFigures() { return cacheDrawStickFigures.getBoolean(); }
	/**
	 * Method to set whether this Technology is drawing stick figures.
	 * @param on true if the MOCMOS technology is to draw stick figures.
	 */
	public static void setStickFigures(boolean on) { cacheDrawStickFigures.setBoolean(on); }

	private static Pref cacheUseSpecialTransistors = Pref.makeBooleanPref("MoCMOSUseSpecialTransistors", getTechnologyPreferences(), false);
	/**
	 * Method to determine whether this Technology includes special transistors.
	 * The default is "false"
	 * The special transistors include the scalable transistors with built-in contacts.
	 * @return true if the MOCMOS technology includes special transistors.
	 */
	public static boolean isSpecialTransistors() { return cacheUseSpecialTransistors.getBoolean(); }
	/**
	 * Method to set whether this Technology includes special transistors.
	 * The special transistors include the scalable transistors with built-in contacts.
	 * @param on true if the MOCMOS technology will include special transistors.
	 */
	public static void setSpecialTransistors(boolean on) { cacheUseSpecialTransistors.setBoolean(on); }

	private static Pref cacheNumberOfMetalLayers = MoCMOSPref.makeIntPref("MoCMOSNumberOfMetalLayers", getTechnologyPreferences(), 4);
    static { cacheNumberOfMetalLayers.attachToObject(tech, "Edit Options, Technology tab", "Number of Metal Layers"); }
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

	private static Pref cacheRuleSet = MoCMOSPref.makeIntPref("MoCMOSRuleSet", getTechnologyPreferences(), 1);
    static { cacheRuleSet.attachToObject(tech, "Edit Options, Technology tab", "Rule set (0=SCMOS, 1=Submicron, 2=Deep)"); }
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

	private static Pref cacheSecondPolysilicon = MoCMOSPref.makeBooleanPref("MoCMOSSecondPolysilicon", getTechnologyPreferences(), false);
    static { cacheSecondPolysilicon.attachToObject(tech, "Edit Options, Technology tab", "Second Polysilicon Layer"); }
	/**
	 * Method to tell the number of polysilicon layers in this Technology.
	 * The default is false.
	 * @return the number of polysilicon layers in this Technology (1 or 2).
	 */
	public static boolean isSecondPolysilicon() { return cacheSecondPolysilicon.getBoolean(); }
	/**
	 * Method to set the number of polysilicon layers in this Technology.
	 * @param count the new number of polysilicon layers in this Technology.
	 * The number of polysilicon layers must be 1 or 2.
	 */
	public static void setSecondPolysilicon(boolean on) { cacheSecondPolysilicon.setBoolean(on); }

	private static Pref cacheDisallowStackedVias = MoCMOSPref.makeBooleanPref("MoCMOSDisallowStackedVias", getTechnologyPreferences(), false);
    static { cacheDisallowStackedVias.attachToObject(tech, "Edit Options, Technology tab", "Disallow Stacked Vias"); }
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

	private static Pref cacheAlternateActivePolyRules = MoCMOSPref.makeBooleanPref("MoCMOSAlternateActivePolyRules", getTechnologyPreferences(), false);
    static { cacheAlternateActivePolyRules.attachToObject(tech, "Edit Options, Technology tab", "Alternate Active and Poly Contact Rules"); }
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
	/** set for stick-figure display */				private static final int MOCMOSSTICKFIGURE =     02;
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
	/** set to show special transistors */			private static final int MOCMOSSPECIALTRAN =  01000;

	/**
	 * Method to convert any old-style state information to the new options.
	 */
	public void convertOldState()
	{
		Variable var = getVar(TECH_LAST_STATE);
		if (var == null) return;
		int oldBits = ((Integer)var.getObject()).intValue();
		delVar(TECH_LAST_STATE);

		boolean oldNoStackedVias = (oldBits&MOCMOSNOSTACKEDVIAS) != 0;
		if (oldNoStackedVias != isDisallowStackedVias())
		{
			newVar(cacheDisallowStackedVias.getPrefName(), new Integer(oldNoStackedVias?1:0));
			Pref.changedMeaningVariable(cacheDisallowStackedVias.getMeaning());
		}

		int numMetals = 0;
		switch (oldBits&MOCMOSMETALS)
		{
			case MOCMOS2METAL: numMetals = 2;   break;
			case MOCMOS3METAL: numMetals = 3;   break;
			case MOCMOS4METAL: numMetals = 4;   break;
			case MOCMOS5METAL: numMetals = 5;   break;
			case MOCMOS6METAL: numMetals = 6;   break;
		}
		if (numMetals != getNumMetal())
		{
			newVar(cacheNumberOfMetalLayers.getPrefName(), new Integer(numMetals));
			Pref.changedMeaningVariable(cacheNumberOfMetalLayers.getMeaning());
		}

		int ruleSet = 0;
		switch (oldBits&MOCMOSRULESET)
		{
			case MOCMOSSUBMRULES:  ruleSet = SUBMRULES;   break;
			case MOCMOSDEEPRULES:  ruleSet = DEEPRULES;   break;
			case MOCMOSSCMOSRULES: ruleSet = SCMOSRULES;  break;
		}
		if (ruleSet != getRuleSet())
		{
			newVar(cacheRuleSet.getPrefName(), new Integer(ruleSet));
			Pref.changedMeaningVariable(cacheRuleSet.getMeaning());
		}

		boolean alternateContactRules = (oldBits&MOCMOSALTAPRULES) != 0;
		if (alternateContactRules != isAlternateActivePolyRules())
		{
			newVar(cacheAlternateActivePolyRules.getPrefName(), new Integer(alternateContactRules?1:0));
			Pref.changedMeaningVariable(cacheAlternateActivePolyRules.getMeaning());
		}

		boolean secondPoly = (oldBits&MOCMOSTWOPOLY) != 0;
		if (secondPoly != isSecondPolysilicon())
		{
			newVar(cacheSecondPolysilicon.getPrefName(), new Integer(secondPoly?1:0));
			Pref.changedMeaningVariable(cacheSecondPolysilicon.getMeaning());
		}
	}

	/******************** SWITCHING N AND P LAYERS ********************/

//	/*
//	 * Method to switch N and P layers (not terribly useful)
//	 */
//	void switchnp(void)
//	{
//		REGISTER LIBRARY *lib;
//		REGISTER NODEPROTO *np;
//		REGISTER NODEINST *ni, *rni;
//		REGISTER ARCINST *ai;
//		REGISTER ARCPROTO *ap, *app, *apn;
//		REGISTER PORTPROTO *pp, *rpp;
//		REGISTER PORTARCINST *pi;
//		REGISTER PORTEXPINST *pe;
//		REGISTER INTBIG i, j, k;
//
//		// find the important node and arc prototypes
//		setupprimswap(NPACTP, NNACTP, &primswap[0]);
//		setupprimswap(NMETPACTC, NMETNACTC, &primswap[1]);
//		setupprimswap(NTRANSP, NTRANSN, &primswap[2]);
//		setupprimswap(NPWBUT, NNWBUT, &primswap[3]);
//		setupprimswap(NPACTIVEN, NNACTIVEN, &primswap[4]);
//		setupprimswap(NSELECTPN, NSELECTNN, &primswap[5]);
//		setupprimswap(NWELLPN, NWELLNN, &primswap[6]);
//		app = apn = NOARCPROTO;
//		for(ap = tech->firstarcproto; ap != NOARCPROTO; ap = ap->nextarcproto)
//		{
//			if (namesame(ap->protoname, "P-Active")) == 0) app = ap;
//			if (namesame(ap->protoname, "N-Active")) == 0) apn = ap;
//		}
//
//		for(lib = el_curlib; lib != NOLIBRARY; lib = lib->nextlibrary)
//		{
//			for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//			{
//				for(ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//				{
//					if (ni->proto->primindex == 0) continue;
//					if (ni->proto->tech != tech) continue;
//					for(i=0; i<7; i++)
//					{
//						for(k=0; k<2; k++)
//						{
//							if (ni->proto == primswap[i].np[k])
//							{
//								ni->proto = primswap[i].np[1-k];
//								for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//								{
//									for(j=0; j<primswap[i].portcount; j++)
//									{
//										if (pi->proto == primswap[i].pp[k][j])
//										{
//											pi->proto = primswap[i].pp[1-k][j];
//											break;
//										}
//									}
//								}
//								for(pe = ni->firstportexpinst; pe != NOPORTEXPINST; pe = pe->nextportexpinst)
//								{
//									for(j=0; j<primswap[i].portcount; j++)
//									{
//										if (pe->proto == primswap[i].pp[k][j])
//										{
//											pe->proto = primswap[i].pp[1-k][j];
//											pe->exportproto->subportproto = pe->proto;
//											break;
//										}
//									}
//								}
//								break;
//							}
//						}
//					}
//				}
//				for(ai = np->firstarcinst; ai != NOARCINST; ai = ai->nextarcinst)
//				{
//					if (ai->proto->tech != tech) continue;
//					if (ai->proto == app)
//					{
//						ai->proto = apn;
//					} else if (ai->proto == apn)
//					{
//						ai->proto = app;
//					}
//				}
//			}
//			for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//			{
//				for(pp = np->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//				{
//					// find the primitive at the bottom
//					rpp = pp->subportproto;
//					rni = pp->subnodeinst;
//					while (rni->proto->primindex == 0)
//					{
//						rni = rpp->subnodeinst;
//						rpp = rpp->subportproto;
//					}
//					pp->connects = rpp->connects;
//				}
//			}
//		}
//		for(i=0; i<7; i++)
//		{
//			ni = primswap[i].np[0]->firstinst;
//			primswap[i].np[0]->firstinst = primswap[i].np[1]->firstinst;
//			primswap[i].np[1]->firstinst = ni;
//		}
//	}
//
//	/*
//	 * Helper method for "switchnp()".
//	 */
//	void setupprimswap(INTBIG index1, INTBIG index2, PRIMSWAP *swap)
//	{
//		REGISTER NODEPROTO *np;
//		REGISTER PORTPROTO *pp;
//
//		swap->np[0] = swap->np[1] = NONODEPROTO;
//		for(np = tech->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//		{
//			if (np->primindex == index1) swap->np[0] = np;
//			if (np->primindex == index2) swap->np[1] = np;
//		}
//		if (swap->np[0] == NONODEPROTO || swap->np[1] == NONODEPROTO) return;
//		swap->portcount = 0;
//		for(pp = swap->np[0]->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//			swap->pp[0][swap->portcount++] = pp;
//		swap->portcount = 0;
//		for(pp = swap->np[1]->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//			swap->pp[1][swap->portcount++] = pp;
//	}

/******************** NODE DESCRIPTION (GRAPHICAL) ********************/

//INTBIG intnodepolys(NODEINST *ni, INTBIG *reasonable, WINDOWPART *win, POLYLOOP *pl, MOCPOLYLOOP *mocpl)
//{
//	REGISTER INTBIG pindex, count;
//	TECH_NODES *thistn;
//	REGISTER NODEPROTO *np;
//
//	np = ni->proto;
//	pindex = np->primindex;
//
//	// non-stick-figures: standard components
//	if ((state&MOCMOSSTICKFIGURE) == 0)
//	{
//		if (pindex == NSTRANSP || pindex == NSTRANSN)
//			return(initializescalabletransistor(ni, reasonable, win, pl, mocpl));
//		return(tech_nodepolys(ni, reasonable, win, pl));
//	}
//
//	// stick figures: special cases for special primitives
//	thistn = np->tech->nodeprotos[pindex-1];
//	count = thistn->layercount;
//	switch (pindex)
//	{
//		case NMETAL1P:
//		case NMETAL2P:
//		case NMETAL3P:
//		case NMETAL4P:
//		case NMETAL5P:
//		case NMETAL6P:
//		case NPOLY1P:
//		case NPOLY2P:
//		case NPACTP:
//		case NNACTP:
//		case NACTP:
//			// pins disappear with one or two wires
//			if (tech_pinusecount(ni, NOWINDOWPART)) count = 0;
//			break;
//		case NMETPACTC:
//		case NMETNACTC:
//		case NMETPOLY1C:
//		case NMETPOLY2C:
//		case NMETPOLY12C:
//		case NVIA1:
//		case NVIA2:
//		case NVIA3:
//		case NVIA4:
//		case NVIA5:
//		case NPWBUT:
//		case NNWBUT:
//			// contacts draw a box the size of the port
//			count = 1;
//			break;
//		case NTRANSP:
//		case NTRANSN:
//			// prepare for possible serpentine transistor
//			count = tech_inittrans(2, ni, pl);
//			break;
//	}
//
//	// add in displayable variables
//	pl->realpolys = count;
//	count += tech_displayablenvars(ni, pl->curwindowpart, pl);
//	if (reasonable != 0) *reasonable = count;
//	return(count);
//}
//
//void intshapenodepoly(NODEINST *ni, INTBIG box, POLYGON *poly, POLYLOOP *pl, MOCPOLYLOOP *mocpl)
//{
//	TECH_POLYGON *lay;
//	REGISTER INTBIG pindex, lambda, cx, cy;
//	REGISTER NODEPROTO *np;
//	REGISTER TECH_NODES *thistn;
//	REGISTER TECH_PORTS *portdata;
//	static Graphics contactdesc = {LAYERO, BLACK, Graphics.SOLIDC, Graphics.SOLIDC,
//		{0,0,0,0,0,0,0,0}, NOVARIABLE, 0};
//
//	lay = 0;
//	if ((state&MOCMOSSTICKFIGURE) == 0)
//	{
//		// non-stick-figures: standard components
//		np = ni->proto;
//		pindex = np->primindex;
//		if (pindex == NSTRANSP || pindex == NSTRANSN)
//		{
//			iteratescalabletransistor(ni, box, poly, pl, mocpl);
//			return;
//		}
//
//		tech_shapenodepoly(ni, box, poly, pl);
//		return;
//	}
//
//	// handle displayable variables
//	if (box >= pl->realpolys)
//	{
//		(void)tech_filldisplayablenvar(ni, poly, pl->curwindowpart, 0, pl);
//		return;
//	}
//
//	np = ni->proto;
//	pindex = np->primindex;
//	thistn = tech->nodeprotos[pindex-1];
//	lambda = lambdaofnode(ni);
//	switch (pindex)
//	{
//		case NMETAL1P:
//		case NMETAL2P:
//		case NMETAL3P:
//		case NMETAL4P:
//		case NMETAL5P:
//		case NMETAL6P:
//		case NPOLY1P:
//		case NPOLY2P:
//		case NPACTP:
//		case NNACTP:
//		case NACTP:
//			// pins disappear with one or two wires
//			lay = &thistn->layerlist[box];
//			poly->layer = polyCut_lay;
//			if (poly->limit < 2) (void)extendpolygon(poly, 2);
//			cx = (ni->lowx + ni->highx) / 2;
//			cy = (ni->lowy + ni->highy) / 2;
//			poly->xv[0] = cx;   poly->yv[0] = cy;
//			poly->xv[1] = cx;   poly->yv[1] = cy + lambda/2;
//			poly->count = 2;
//			poly->style = DISC;
//			poly->desc = tech->layers[poly->layer];
//			break;
//		case NMETPACTC:
//		case NMETNACTC:
//		case NMETPOLY1C:
//		case NMETPOLY2C:
//		case NMETPOLY12C:
//		case NVIA1:
//		case NVIA2:
//		case NVIA3:
//		case NVIA4:
//		case NVIA5:
//		case NPWBUT:
//		case NNWBUT:
//			// contacts draw a box the size of the port
//			lay = &thistn->layerlist[box];
//			poly->layer = polyCut_lay;
//			if (poly->limit < 2) (void)extendpolygon(poly, 2);
//			portdata = &thistn->portlist[0];
//			subrange(ni->lowx, ni->highx, portdata->lowxmul, portdata->lowxsum,
//				portdata->highxmul, portdata->highxsum, &poly->xv[0], &poly->xv[1], lambda);
//			subrange(ni->lowy, ni->highy, portdata->lowymul, portdata->lowysum,
//				portdata->highymul, portdata->highysum, &poly->yv[0], &poly->yv[1], lambda);
//			poly->count = 2;
//			poly->style = CLOSED;
//			poly->desc = &contactdesc;
//
//			// code cannot be called by multiple procesors: uses globals
//			NOT_REENTRANT;
//
//			switch (pindex)
//			{
//				case NMETPACTC:   contactdesc.bits = LAYERT1|LAYERT3;  contactdesc.col = COLORT1|COLORT3;   break;
//				case NMETNACTC:   contactdesc.bits = LAYERT1|LAYERT3;  contactdesc.col = COLORT1|COLORT3;   break;
//				case NMETPOLY1C:  contactdesc.bits = LAYERT1|LAYERT2;  contactdesc.col = COLORT1|COLORT2;   break;
//				case NMETPOLY2C:  contactdesc.bits = LAYERO;           contactdesc.col = ORANGE;            break;
//				case NMETPOLY12C: contactdesc.bits = LAYERO;           contactdesc.col = ORANGE;            break;
//				case NVIA1:       contactdesc.bits = LAYERT1|LAYERT4;  contactdesc.col = COLORT1|COLORT4;   break;
//				case NVIA2:       contactdesc.bits = LAYERT4|LAYERT5;  contactdesc.col = COLORT4|COLORT5;   break;
//				case NVIA3:       contactdesc.bits = LAYERO;           contactdesc.col = LBLUE;             break;
//				case NVIA4:       contactdesc.bits = LAYERO;           contactdesc.col = LRED;              break;
//				case NVIA5:       contactdesc.bits = LAYERO;           contactdesc.col = CYAN;              break;
//				case NPWBUT:      contactdesc.bits = LAYERO;           contactdesc.col = BROWN;             break;
//				case NNWBUT:      contactdesc.bits = LAYERO;           contactdesc.col = YELLOW;            break;
//			}
//			break;
//		case NTRANSP:
//		case NTRANSN:
//			// prepare for possible serpentine transistor
//			lay = &thistn->gra[box].basics;
//			poly->layer = lay->layernum;
//			if (poly->layer == transistorPoly_lay)
//			{
//				ni->lowy += lambda;
//				ni->highy -= lambda;
//			} else
//			{
//				ni->lowx += lambda + lambda/2;
//				ni->highx -= lambda + lambda/2;
//			}
//			tech_filltrans(poly, &lay, thistn->gra, ni, lambda, box, (TECH_PORTS *)0, pl);
//			if (poly->layer == transistorPoly_lay)
//			{
//				ni->lowy -= lambda;
//				ni->highy += lambda;
//			} else
//			{
//				ni->lowx -= lambda + lambda/2;
//				ni->highx += lambda + lambda/2;
//			}
//			poly->desc = tech->layers[poly->layer];
//			break;
//		default:
//			lay = &thistn->layerlist[box];
//			tech_fillpoly(poly, lay, ni, lambda, FILLED);
//			poly->desc = tech->layers[poly->layer];
//			break;
//	}
//}
//
//void nodesizeoffset(NODEINST *ni, INTBIG *lx, INTBIG *ly, INTBIG *hx, INTBIG *hy)
//{
//	REGISTER INTBIG pindex;
//	REGISTER NODEPROTO *np;
//	REGISTER INTBIG lambda, cx, cy;
//	INTBIG bx, by, ux, uy;
//	REGISTER TECH_NODES *thistn;
//	REGISTER TECH_PORTS *portdata;
//
//	np = ni->proto;
//	pindex = np->primindex;
//	lambda = lambdaofnode(ni);
//	switch (pindex)
//	{
//		case NMETAL1P:
//		case NMETAL2P:
//		case NMETAL3P:
//		case NMETAL4P:
//		case NMETAL5P:
//		case NMETAL6P:
//		case NPOLY1P:
//		case NPOLY2P:
//		case NPACTP:
//		case NNACTP:
//		case NACTP:
//			cx = (ni->lowx + ni->highx) / 2;
//			cy = (ni->lowy + ni->highy) / 2;
//			*lx = (cx - lambda) - ni->lowx;
//			*hx = ni->highx - (cx + lambda);
//			*ly = (cy - lambda) - ni->lowy;
//			*hy = ni->highy - (cy + lambda);
//			break;
//		case NMETPACTC:
//		case NMETNACTC:
//		case NMETPOLY1C:
//		case NMETPOLY2C:
//		case NMETPOLY12C:
//		case NVIA1:
//		case NVIA2:
//		case NVIA3:
//		case NVIA4:
//		case NVIA5:
//		case NPWBUT:
//		case NNWBUT:
//			// contacts draw a box the size of the port
//			thistn = tech->nodeprotos[pindex-1];
//			portdata = &thistn->portlist[0];
//			subrange(ni->lowx, ni->highx, portdata->lowxmul, portdata->lowxsum,
//				portdata->highxmul, portdata->highxsum, &bx, &ux, lambda);
//			subrange(ni->lowy, ni->highy, portdata->lowymul, portdata->lowysum,
//				portdata->highymul, portdata->highysum, &by, &uy, lambda);
//			*lx = bx - ni->lowx;
//			*hx = ni->highx - ux;
//			*ly = by - ni->lowy;
//			*hy = ni->highy - uy;
//			break;
//		default:
//			nodeprotosizeoffset(np, lx, ly, hx, hy, ni->parent);
//			if (pindex == NTRANSP || pindex == NTRANSN)
//			{
//				*lx += lambda + lambda/2;
//				*hx += lambda + lambda/2;
//				*ly += lambda;
//				*hy += lambda;
//			}
//			break;
//	}
//}

///******************** SCALABLE TRANSISTOR DESCRIPTION ********************/
//
//INTBIG initializescalabletransistor(NODEINST *ni, INTBIG *reasonable, WINDOWPART *win, POLYLOOP *pl, MOCPOLYLOOP *mocpl)
//{
//	REGISTER INTBIG pindex, count, lambda, activewid, requestedwid, extrainset, nodewid, extracuts;
//	REGISTER INTBIG cutsize, cutindent, cutsep;
//	INTBIG olx, ohx, oly, ohy;
//	REGISTER VARIABLE *var;
//	REGISTER CHAR *pt;
//	TECH_NODES *thistn;
//	REGISTER NODEPROTO *np;
//
//	// determine the width
//	np = ni->proto;
//	pindex = np->primindex;
//	lambda = lambdaofnode(ni);
//	nodewid = (ni->highx - ni->lowx) * WHOLE / lambda;
//	activewid = nodewid - K14;
//	extrainset = 0;
//
//	// determine special configurations (number of active contacts, inset of active contacts)
//	mocpl->numcontacts = 2;
//	mocpl->insetcontacts = FALSE;
//	var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, transcontactkey);
//	if (var != NOVARIABLE)
//	{
//		pt = (CHAR *)var->addr;
//		if (*pt == '0' || *pt == '1' || *pt == '2')
//		{
//			mocpl->numcontacts = *pt - '0';
//			pt++;
//		}
//		if (*pt == 'i' || *pt == 'I') mocpl->insetcontacts = TRUE;
//	}
//	mocpl->boxoffset = 4 - mocpl->numcontacts * 2;
//
//	// determine width
//	var = getvalkey((INTBIG)ni, VNODEINST, -1, el_attrkey_width);
//	if (var != NOVARIABLE)
//	{
//		pt = describevariable(var, -1, -1);
//		if (*pt == '-' || *pt == '+' || isdigit(*pt))
//		{
//			requestedwid = atofr(pt);
//			if (requestedwid > activewid)
//			{
//				ttyputmsg(_("Warning: cell %s, node %s requests width of %s but is only %s wide"),
//					describenodeproto(ni->parent), describenodeinst(ni), frtoa(requestedwid),
//						frtoa(activewid));
//			}
//			if (requestedwid < activewid && requestedwid > 0)
//			{
//				extrainset = (activewid - requestedwid) / 2;
//				activewid = requestedwid;
//			}
//		}
//	}
//	mocpl->actinset = (nodewid-activewid) / 2;
//	mocpl->polyinset = mocpl->actinset - K2;
//	mocpl->actcontinset = K7 + extrainset;
//
//	// contacts must be 5 wide at a minimum
//	if (activewid < K5) mocpl->actcontinset -= (K5-activewid)/2;
//	mocpl->metcontinset = mocpl->actcontinset + H0;
//
//	// determine the multicut information
//	mocpl->moscutsize = cutsize = mpa.f1;
//	cutindent = mpa.f3;
//	mocpl->moscutsep = cutsep = mpa.f4;
//	nodesizeoffset(ni, &olx, &oly, &ohx, &ohy);
//	mocpl->numcuts = (activewid-cutindent*2+cutsep) / (cutsize+cutsep);
//	if (mocpl->numcuts <= 0) mocpl->numcuts = 1;
//	if (mocpl->numcuts != 1)
//		mocpl->moscutbase = (activewid-cutindent*2 - cutsize*mocpl->numcuts -
//			cutsep*(mocpl->numcuts-1)) / 2 + (nodewid-activewid)/2 + cutindent;
//
//	// now compute the number of polygons
//	extracuts = (mocpl->numcuts-1)*2 - (2-mocpl->numcontacts) * mocpl->numcuts;
//	count = tech_nodepolys(ni, reasonable, win, pl) + extracuts - mocpl->boxoffset;
//	thistn = np->tech->nodeprotos[pindex-1];
//	pl->realpolys = thistn->layercount + extracuts;
//	return(count);
//}
//
//void iteratescalabletransistor(NODEINST *ni, INTBIG box, POLYGON *poly, POLYLOOP *pl, MOCPOLYLOOP *mocpl)
//{
//	TECH_POLYGON *lay;
//	REGISTER INTBIG i, lambda, count, cut, pindex, shift;
//	REGISTER TECH_NODES *thistn;
//	REGISTER NODEPROTO *np;
//	TECH_POLYGON localtp;
//	INTBIG mypoints[8];
//
//	np = ni->proto;
//	pindex = np->primindex;
//	thistn = tech->nodeprotos[pindex-1];
//	box += mocpl->boxoffset;
//	if (box <= 7)
//	{
//		lay = &thistn->layerlist[box];
//		lambda = lambdaofnode(ni);
//		localtp.layernum = lay->layernum;
//		localtp.portnum = lay->portnum;
//		localtp.count = lay->count;
//		localtp.style = lay->style;
//		localtp.representation = lay->representation;
//		localtp.points = mypoints;
//		for(i=0; i<8; i++) mypoints[i] = lay->points[i];
//		switch (box)
//		{
//			case 4:		// active that passes through gate
//				mypoints[1] = mocpl->actinset;
//				mypoints[5] = -mocpl->actinset;
//				break;
//			case 0:		// active surrounding contacts
//			case 2:
//				mypoints[1] = mocpl->actcontinset;
//				mypoints[5] = -mocpl->actcontinset;
//				if (mocpl->insetcontacts)
//				{
//					if (mypoints[3] < 0) shift = -H0; else shift = H0;
//					mypoints[3] += shift;
//					mypoints[7] += shift;
//				}
//				break;
//			case 5:		// poly
//				mypoints[1] = mocpl->polyinset;
//				mypoints[5] = -mocpl->polyinset;
//				break;
//			case 1:		// metal surrounding contacts
//			case 3:
//				mypoints[1] = mocpl->metcontinset;
//				mypoints[5] = -mocpl->metcontinset;
//				if (mocpl->insetcontacts)
//				{
//					if (mypoints[3] < 0) shift = -H0; else shift = H0;
//					mypoints[3] += shift;
//					mypoints[7] += shift;
//				}
//				break;
//			case 6:		// well and select
//			case 7:
//				if (mocpl->insetcontacts)
//				{
//					mypoints[3] += H0;
//					mypoints[7] -= H0;
//				}
//				break;
//		}
//		tech_fillpoly(poly, &localtp, ni, lambda, FILLED);
//		poly->desc = tech->layers[poly->layer];
//		if (lay->portnum < 0) poly->portproto = NOPORTPROTO; else
//			poly->portproto = thistn->portlist[lay->portnum].addr;
//		return;
//	}
//	if (box >= pl->realpolys)
//	{
//		// displayable variables
//		(void)tech_filldisplayablenvar(ni, poly, pl->curwindowpart, 0, pl);
//		return;
//	}
//
//	// multiple contact cuts
//	count = thistn->layercount - 2;
//	if (box >= count)
//	{
//		lambda = lambdaofnode(ni);
//		lay = &thistn->layerlist[count+(box-count) / mocpl->numcuts];
//		cut = (box-count) % mocpl->numcuts;
//		localtp.layernum = lay->layernum;
//		localtp.portnum = lay->portnum;
//		localtp.count = lay->count;
//		localtp.style = lay->style;
//		localtp.representation = lay->representation;
//		localtp.points = mypoints;
//		for(i=0; i<8; i++) mypoints[i] = lay->points[i];
//
//		if (mocpl->numcuts == 1)
//		{
//			mypoints[1] = (ni->highx-ni->lowx)/2 * WHOLE/lambda - mocpl->moscutsize/2;
//			mypoints[5] = (ni->highx-ni->lowx)/2 * WHOLE/lambda + mocpl->moscutsize/2;
//		} else
//		{
//			mypoints[1] = mocpl->moscutbase + cut * (mocpl->moscutsize + mocpl->moscutsep);
//			mypoints[5] = mypoints[1] + mocpl->moscutsize;
//		}
//		if (mocpl->insetcontacts)
//		{
//			if (mypoints[3] < 0) shift = -H0; else shift = H0;
//			mypoints[3] += shift;
//			mypoints[7] += shift;
//		}
//
//		tech_fillpoly(poly, &localtp, ni, lambda, FILLED);
//		poly->desc = tech->layers[poly->layer];
//		poly->portproto = NOPORTPROTO;
//		return;
//	}
//	tech_shapenodepoly(ni, box, poly, pl);
//}
//
///******************** ARC DESCRIPTION ********************/
//
//INTBIG intarcpolys(ARCINST *ai, WINDOWPART *win, POLYLOOP *pl, MOCPOLYLOOP *mocpl)
//{
//	REGISTER INTBIG i;
//
//	i = 1;
//	mocpl->arrowbox = -1;
//	if ((ai->userbits&ISDIRECTIONAL) != 0) mocpl->arrowbox = i++;
//
//	// add in displayable variables
//	pl->realpolys = i;
//	i += tech_displayableavars(ai, win, pl);
//	return(i);
//}
//
//void intshapearcpoly(ARCINST *ai, INTBIG box, POLYGON *poly, POLYLOOP *pl, MOCPOLYLOOP *mocpl)
//{
//	REGISTER INTBIG aindex;
//	REGISTER INTBIG angle;
//	REGISTER INTBIG x1,y1, x2,y2, i;
//	REGISTER TECH_ARCLAY *thista;
//	static Graphics intense = {LAYERO, RED, Graphics.SOLIDC, Graphics.SOLIDC,
//		{0,0,0,0,0,0,0,0}, NOVARIABLE, 0};
//
//	// handle displayable variables
//	if (box >= pl->realpolys)
//	{
//		(void)tech_filldisplayableavar(ai, poly, pl->curwindowpart, 0, pl);
//		return;
//	}
//
//	// initialize for the arc
//	aindex = ai->proto->arcindex;
//	thista = &arcprotos[aindex]->list[box];
//	poly->layer = thista->lay;
//	switch (ai->proto->arcindex)
//	{
//		case AMETAL1:
//		case AMETAL2:
//		case AMETAL3:
//		case AMETAL4:
//		case AMETAL5:
//		case AMETAL6:
//			intense.col = BLUE;
//			break;
//		case APOLY1:
//		case APOLY2:
//			intense.col = RED;
//			break;
//		case APACT:
//		case ANACT:
//		case AACT:
//			intense.col = DGREEN;
//			break;
//	}
//	if (mocpl->arrowbox < 0 || box == 0)
//	{
//		// simple arc
//		poly->desc = tech->layers[poly->layer];
//		makearcpoly(ai->length, ai->width-ai->proto->nominalwidth, ai, poly, thista->style);
//		return;
//	}
//
//	// prepare special information for directional arcs
//	poly->desc = &intense;
//	x1 = ai->end[0].xpos;   y1 = ai->end[0].ypos;
//	x2 = ai->end[1].xpos;   y2 = ai->end[1].ypos;
//	angle = ((ai->userbits&AANGLE) >> AANGLESH) * 10;
//	if ((ai->userbits&REVERSEEND) != 0)
//	{
//		i = x1;   x1 = x2;   x2 = i;
//		i = y1;   y1 = y2;   y2 = i;
//		angle = (angle+1800) % 3600;
//	}
//
//	// draw the directional arrow
//	poly->style = VECTORS;
//	poly->layer = -1;
//	if (poly->limit < 2) (void)extendpolygon(poly, 2);
//	poly->count = 0;
//	if ((ai->userbits&NOTEND1) == 0)
//		tech_addheadarrow(poly, angle, x2, y2, lambdaofarc(ai));
//}

}
