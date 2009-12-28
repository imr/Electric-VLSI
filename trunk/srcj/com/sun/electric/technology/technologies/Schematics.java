/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Schematics.java
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

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.PortProtoId;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.EditWindow0;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.AbstractShapeBuilder;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.Foundry;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveNodeSize;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.TechFactory;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.TransistorSize;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;
import static com.sun.electric.database.text.ArrayIterator.i2i;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;



/**
 * This is the Schematics technology.
 */
public class Schematics extends Technology
{
	/** key of Variable holding global signal name. */		public static final Variable.Key SCHEM_GLOBAL_NAME = Variable.newKey("SCHEM_global_name");
	/** key of Variable holding resistance. */				public static final Variable.Key SCHEM_RESISTANCE = Variable.newKey("SCHEM_resistance");
	/** key of Variable holding capacitance. */				public static final Variable.Key SCHEM_CAPACITANCE = Variable.newKey("SCHEM_capacitance");
	/** key of Variable holding inductance. */				public static final Variable.Key SCHEM_INDUCTANCE = Variable.newKey("SCHEM_inductance");
	/** key of Variable holding diode area. */				public static final Variable.Key SCHEM_DIODE = Variable.newKey("SCHEM_diode");
	/** key of Variable holding black-box function. */		public static final Variable.Key SCHEM_FUNCTION = Variable.newKey("SCHEM_function");
	/** key of Variable holding transistor width. */		public static final Variable.Key ATTR_WIDTH = Variable.newKey("ATTR_width");
	/** key of Variable holding transistor length. */		public static final Variable.Key ATTR_LENGTH = Variable.newKey("ATTR_length");
	/** key of Variable holding transistor area. */			public static final Variable.Key ATTR_AREA = Variable.newKey("ATTR_area");

	/** the Schematics Technology object. */				public static Schematics tech() { return TechPool.getThreadTechPool().getSchematics(); }

//	/** Defines the Flip-flop type. */						private static final int FFTYPE =    07;
	/** Defines an RS Flip-flop. */							private static final int FFTYPERS =   0;
	/** Defines a JK Flip-flop. */							private static final int FFTYPEJK =   1;
	/** Defines a D Flip-flop. */							private static final int FFTYPED =    2;
	/** Defines a T Flip-flop. */							private static final int FFTYPET =    3;
//	/** Defines the Flip-flop clocking bits. */				private static final int FFCLOCK =  014;
	/** Defines a Master/Slave Flip-flop. */				private static final int FFCLOCKMS =  0;
	/** Defines a Positive clock Flip-flop. */				private static final int FFCLOCKP =  04;
	/** Defines a Negative clock Flip-flop. */				private static final int FFCLOCKN = 010;

	/** Defines an nMOS transistor. */						private static final int TRANNMOS      =  0;
	/** Defines a nMOS depletion transistor. */				private static final int TRANNMOSD     =  1;
	/** Defines a pMOS transistor. */						private static final int TRANPMOS      =  2;
	/** Defines an NPN Junction transistor. */				private static final int TRANNPN       =  3;
	/** Defines a PNP Junction transistor. */				private static final int TRANPNP       =  4;
	/** Defines an N Junction FET transistor. */			private static final int TRANNJFET     =  5;
	/** Defines a P Junction FET transistor. */				private static final int TRANPJFET     =  6;
	/** Defines a Depletion MESFET transistor. */			private static final int TRANDMES      =  7;
	/** Defines an Enhancement MESFET transistor. */		private static final int TRANEMES      =  8;
	/** Defines a pMOS depletion transistor. */				private static final int TRANPMOSD     =  9;
	/** Defines a nMOS native transistor. */				private static final int TRANNMOSNT    = 10;
	/** Defines a pMOS native transistor. */				private static final int TRANPMOSNT    = 11;
	/** Defines a nMOS floating gate transistor. */			private static final int TRANNMOSFG    = 12;
	/** Defines a pMOS floating gate transistor. */			private static final int TRANPMOSFG    = 13;
	/** Defines a nMOS low threshold transistor. */			private static final int TRANNMOSVTL   = 14;
	/** Defines a pMOS low threshold transistor. */			private static final int TRANPMOSVTL   = 15;
	/** Defines a nMOS high threshold transistor. */		private static final int TRANNMOSVTH   = 16;
	/** Defines a pMOS high threshold transistor. */		private static final int TRANPMOSVTH   = 17;
	/** Defines a nMOS high voltage-1 transistor. */		private static final int TRANNMOSHV1   = 18;
	/** Defines a pMOS high voltage-1 transistor. */		private static final int TRANPMOSHV1   = 19;
	/** Defines a nMOS high voltage-2 transistor. */		private static final int TRANNMOSHV2   = 20;
	/** Defines a pMOS high voltage-2 transistor. */		private static final int TRANPMOSHV2   = 21;
	/** Defines a nMOS high voltage-3 transistor. */		private static final int TRANNMOSHV3   = 22;
	/** Defines a pMOS high voltage-3 transistor. */		private static final int TRANPMOSHV3   = 23;
	/** Defines a nMOS native high voltage-1 transistor. */	private static final int TRANNMOSNTHV1 = 24;
	/** Defines a pMOS native high voltage-1 transistor. */	private static final int TRANPMOSNTHV1 = 25;
	/** Defines a nMOS native high voltage-2 transistor. */	private static final int TRANNMOSNTHV2 = 26;
	/** Defines a pMOS native high voltage-2 transistor. */	private static final int TRANPMOSNTHV2 = 27;
	/** Defines a nMOS native high voltage-3 transistor. */	private static final int TRANNMOSNTHV3 = 28;
	/** Defines a pMOS native high voltage-3 transistor. */	private static final int TRANPMOSNTHV3 = 29;
	/** Defines a NMOS Carbon Nanotube transistor. */		private static final int TRANNMOSCN    = 30;
	/** Defines a PMOS Carbon Nanotube transistor. */		private static final int TRANPMOSCN    = 31;

	/** Defines a normal Diode. */							private static final int DIODENORM =  0;
	/** Defines a Zener Diode. */							private static final int DIODEZENER = 1;

	/** Defines a normal Capacitor. */						private static final int CAPACNORM =  0;
	/** Defines an Electrolytic Capacitor. */				private static final int CAPACELEC =  1;
	/** Defines a Poly2 Capacitor. */				        private static final int CAPACPOLY2 =  2;

    /** Defines a normal Resistor. */                   	private static final int RESISTNORM  =  0;
    /** Defines an n-poly Resistor. */                     	private static final int RESISTNPOLY =  1;
    /** Defines a p-poly Resistor. */                     	private static final int RESISTPPOLY =  2;
    /** Defines an n-well Resistor. */                     	private static final int RESISTNWELL =  3;
    /** Defines a p-well Resistor. */                     	private static final int RESISTPWELL =  4;
    /** Defines a n-active Resistor. */                     private static final int RESISTNACTIVE =  5;
    /** Defines a p-active Resistor. */                     private static final int RESISTPACTIVE =  6;
    /** Defines an n-poly non silicide Resistor. */         private static final int RESISTNNSPOLY =  7;
    /** Defines a p-poly non silicide Resistor. */          private static final int RESISTPNSPOLY =  8;
    /** Defines a hi resisistant poly2 Resistor. */         private static final int RESISTHIRESPOLY2 =  9;

	/** Defines a Transconductance two-port (VCCS). */		private static final int TWOPVCCS =  0;
	/** Defines a Transresistance two-port (CCVS). */		private static final int TWOPCCVS =  1;
	/** Defines a Voltage gain two-port (VCVS). */			private static final int TWOPVCVS =  2;
	/** Defines a Current gain two-port (CCCS). */			private static final int TWOPCCCS =  3;
	/** Defines a Transmission Line two-port. */			private static final int TWOPTLINE = 4;

	/** the node layer */				public Layer node_lay;

	/** wire arc */						public ArcProto wire_arc;
	/** bus arc */						public ArcProto bus_arc;

	/** wire-pin */						public PrimitiveNode wirePinNode;
	/** bus-pin */						public PrimitiveNode busPinNode;
	/** wire-con */						public PrimitiveNode wireConNode;
	/** buffer */						public PrimitiveNode bufferNode;
	/** and */							public PrimitiveNode andNode;
	/** or */							public PrimitiveNode orNode;
	/** xor */							public PrimitiveNode xorNode;
	/** flipflop */						public PrimitiveNode flipflopNode;
	/** mux */							public PrimitiveNode muxNode;
	/** bbox */							public PrimitiveNode bboxNode;
	/** switch */						public PrimitiveNode switchNode;
	/** offpage */						public PrimitiveNode offpageNode;
	/** power */						public PrimitiveNode powerNode;
	/** ground */						public PrimitiveNode groundNode;
	/** source */						public PrimitiveNode sourceNode;
	/** transistor */					public PrimitiveNode transistorNode;
	/** resistor */						public PrimitiveNode resistorNode;
	/** capacitor */					public PrimitiveNode capacitorNode;
	/** diode */						public PrimitiveNode diodeNode;
	/** inductor */						public PrimitiveNode inductorNode;
	/** meter */						public PrimitiveNode meterNode;
	/** well */							public PrimitiveNode wellNode;
	/** substrate */					public PrimitiveNode substrateNode;
	/** twoport */						public PrimitiveNode twoportNode;
	/** transistor-4 */					public PrimitiveNode transistor4Node;
	/** global */						public PrimitiveNode globalNode;
	/** global partition */				public PrimitiveNode globalPartitionNode;

    // Tech params
    private Double paramNegatingBubbleSize;

	private Layer arc_lay, bus_lay, text_lay;
	private Technology.NodeLayer [] ffLayersRSMS, ffLayersRSP, ffLayersRSN;
	private Technology.NodeLayer [] ffLayersJKMS, ffLayersJKP, ffLayersJKN;
	private Technology.NodeLayer [] ffLayersDMS, ffLayersDP, ffLayersDN;
	private Technology.NodeLayer [] ffLayersTMS, ffLayersTP, ffLayersTN;
	private Technology.NodeLayer [] tranLayersN, tranLayersP;
	private Technology.NodeLayer [] tranLayersNd, tranLayersPd;
	private Technology.NodeLayer [] tranLayersNnT, tranLayersPnT;
	private Technology.NodeLayer [] tranLayersNfG, tranLayersPfG;
	private Technology.NodeLayer [] tranLayersNCN, tranLayersPCN;
	private Technology.NodeLayer [] tranLayersNvtL, tranLayersPvtL;
	private Technology.NodeLayer [] tranLayersNvtH, tranLayersPvtH;
	private Technology.NodeLayer [] tranLayersNht1, tranLayersPht1;
	private Technology.NodeLayer [] tranLayersNht2, tranLayersPht2;
	private Technology.NodeLayer [] tranLayersNht3, tranLayersPht3;
	private Technology.NodeLayer [] tranLayersNnTht1, tranLayersPnTht1;
	private Technology.NodeLayer [] tranLayersNnTht2, tranLayersPnTht2;
	private Technology.NodeLayer [] tranLayersNnTht3, tranLayersPnTht3;
	private Technology.NodeLayer [] tranLayersNPN, tranLayersPNP;
	private Technology.NodeLayer [] tranLayersNJFET, tranLayersPJFET;
	private Technology.NodeLayer [] tranLayersDMES, tranLayersEMES;
	private Technology.NodeLayer [] twoLayersDefault, twoLayersVCVS, twoLayersVCCS, twoLayersCCVS, twoLayersCCCS, twoLayersTran;
	private Technology.NodeLayer [] offPageInputLayers;
	private Technology.NodeLayer [] offPageOutputLayers;
	private Technology.NodeLayer [] offPageBidirectionalLayers;
	private Technology.NodeLayer [] offPageLayers;
	private Technology.NodeLayer [] tran4LayersN, tran4LayersP;
	private Technology.NodeLayer [] tran4LayersNd, tran4LayersPd;
	private Technology.NodeLayer [] tran4LayersNnT, tran4LayersPnT;
	private Technology.NodeLayer [] tran4LayersNfG, tran4LayersPfG;
	private Technology.NodeLayer [] tran4LayersNCN, tran4LayersPCN;
	private Technology.NodeLayer [] tran4LayersNvtL, tran4LayersPvtL;
	private Technology.NodeLayer [] tran4LayersNvtH, tran4LayersPvtH;
	private Technology.NodeLayer [] tran4LayersNht1, tran4LayersPht1;
	private Technology.NodeLayer [] tran4LayersNht2, tran4LayersPht2;
	private Technology.NodeLayer [] tran4LayersNht3, tran4LayersPht3;
	private Technology.NodeLayer [] tran4LayersNnTht1, tran4LayersPnTht1;
	private Technology.NodeLayer [] tran4LayersNnTht2, tran4LayersPnTht2;
	private Technology.NodeLayer [] tran4LayersNnTht3, tran4LayersPnTht3;
	private Technology.NodeLayer [] tran4LayersNPN, tran4LayersPNP;
	private Technology.NodeLayer [] tran4LayersNJFET, tran4LayersPJFET;
	private Technology.NodeLayer [] tran4LayersDMES, tran4LayersEMES;
	private Technology.NodeLayer [] diodeLayersNorm, diodeLayersZener;
	private Technology.NodeLayer [] capacitorLayersNorm, capacitorLayersElectrolytic, capacitorLayersPoly2;
    private Technology.NodeLayer [] resistorLayersNorm, resistorLayersNPoly, resistorLayersPPoly, resistorLayersHiResPoly2;
    private Technology.NodeLayer [] resistorLayersNNSPoly, resistorLayersPNSPoly;
    private Technology.NodeLayer [] resistorLayersNActive, resistorLayersPActive, resistorLayersNWell, resistorLayersPWell;

	// this much from the center to the left edge
	/* 0.1 */			private final EdgeH LEFTBYP1 = new EdgeH(-0.1/2,0);
	/* 0.1333... */		private final EdgeH LEFTBYP125 = new EdgeH(-0.125/2,0);
	/* 0.2 */			private final EdgeH LEFTBYP2 = new EdgeH(-0.2/2,0);
	/* 0.25 */			private final EdgeH LEFTBYP25 = new EdgeH(-0.25/2,0);
	/* 0.3 */			private final EdgeH LEFTBYP3 = new EdgeH(-0.3/2,0);
	/* 0.35 */			private final EdgeH LEFTBYP35 = new EdgeH(-0.35/2,0);
	/* 0.6166... */		private final EdgeH LEFTBYP375 = new EdgeH(-0.375/2,0);
	/* 0.4 */			private final EdgeH LEFTBYP4 = new EdgeH(-0.4/2,0);
	/* 0.45 */			private final EdgeH LEFTBYP45 = new EdgeH(-0.45/2,0);
	/* 0.5 */			private final EdgeH LEFTBYP5 = new EdgeH(-0.5/2,0);
	/* 0.6 */			private final EdgeH LEFTBYP6 = new EdgeH(-0.6/2,0);
	/* 0.6166... */		private final EdgeH LEFTBYP625 = new EdgeH(-0.625/2,0);
	/* 0.6666... */		private final EdgeH LEFTBYP66 = new EdgeH(-0.6666666666/2,0);
	/* 0.7 */			private final EdgeH LEFTBYP7 = new EdgeH(-0.7/2,0);
	/* 0.75 */			private final EdgeH LEFTBYP75 = new EdgeH(-0.75/2,0);
	/* 0.8 */			private final EdgeH LEFTBYP8 = new EdgeH(-0.8/2,0);
	/* 0.875 */			private final EdgeH LEFTBYP875 = new EdgeH(-0.875/2,0);
	/* 0.9 */			private final EdgeH LEFTBYP9 = new EdgeH(-0.9/2,0);
//	/* 1.2 */			private final EdgeH LEFTBYP12 = new EdgeH(-1.2/2,0);
//	/* 1.4 */			private final EdgeH LEFTBYP14 = new EdgeH(-1.4/2,0);
	/* 1.6 */			private final EdgeH LEFTBY1P6 = new EdgeH(-1.6/2,0);

	// this much from the center to the right edge
	/* 0.1 */			private final EdgeH RIGHTBYP1 = new EdgeH(0.1/2,0);
	/* 0.1333... */		private final EdgeH RIGHTBYP125 = new EdgeH(0.125/2,0);
	/* 0.2 */			private final EdgeH RIGHTBYP2 = new EdgeH(0.2/2,0);
	/* 0.25 */			private final EdgeH RIGHTBYP25 = new EdgeH(0.25/2,0);
	/* 0.3 */			private final EdgeH RIGHTBYP3 = new EdgeH(0.3/2,0);
	/* 0.3333... */		private final EdgeH RIGHTBYP33 = new EdgeH(0.3333333333/2,0);
	/* 0.35 */			private final EdgeH RIGHTBYP35 = new EdgeH(0.35/2,0);
	/* 0.6166... */		private final EdgeH RIGHTBYP375 = new EdgeH(0.375/2,0);
	/* 0.3833... */		private final EdgeH RIGHTBYP3833 = new EdgeH(0.3833333333/2,0);
	/* 0.4 */			private final EdgeH RIGHTBYP4 = new EdgeH(0.4/2,0);
	/* 0.4333... */		private final EdgeH RIGHTBYP433 = new EdgeH(0.4333333333/2,0);
	/* 0.45 */			private final EdgeH RIGHTBYP45 = new EdgeH(0.45/2,0);
	/* 0.5 */			private final EdgeH RIGHTBYP5 = new EdgeH(0.5/2,0);
	/* 0.5166... */		private final EdgeH RIGHTBYP5166 = new EdgeH(0.5166666666/2,0);
//	/* 0.55 */			private final EdgeH RIGHTBYP55 = new EdgeH(0.55/2,0);
	/* 0.5666... */		private final EdgeH RIGHTBYP566 = new EdgeH(0.5666666666/2,0);
	/* 0.6 */			private final EdgeH RIGHTBYP6 = new EdgeH(0.6/2,0);
	/* 0.6166... */		private final EdgeH RIGHTBYP6166 = new EdgeH(0.6166666666/2,0);
	/* 0.6166... */		private final EdgeH RIGHTBYP625 = new EdgeH(0.625/2,0);
	/* 0.6666... */		private final EdgeH RIGHTBYP66 = new EdgeH(0.6666666666/2,0);
//	/* 0.7 */			private final EdgeH RIGHTBYP7 = new EdgeH(0.7/2,0);
	/* 0.75 */			private final EdgeH RIGHTBYP75 = new EdgeH(0.75/2,0);
	/* 0.8 */			private final EdgeH RIGHTBYP8 = new EdgeH(0.8/2,0);
	/* 0.875 */			private final EdgeH RIGHTBYP875 = new EdgeH(0.875/2,0);
	/* 0.9 */			private final EdgeH RIGHTBYP9 = new EdgeH(0.9/2,0);

	// this much from the center to the bottom edge
//	/* 0.1 */			private final EdgeV BOTBYP1 = new EdgeV(-0.1/2,0);
//	/* 0.125 */			private final EdgeV BOTBYP125 = new EdgeV(-0.125/2,0);
	/* 0.166...  */		private final EdgeV BOTBYP166 = new EdgeV(-0.166666666/2,0);
	/* 0.2 */			private final EdgeV BOTBYP2 = new EdgeV(-0.2/2,0);
	/* 0.25 */			private final EdgeV BOTBYP25 = new EdgeV(-0.25/2,0);
	/* 0.3 */			private final EdgeV BOTBYP3 = new EdgeV(-0.3/2,0);
	/* 0.3333... */		private final EdgeV BOTBYP33 = new EdgeV(-0.3333333333/2,0);
	/* 0.375 */			private final EdgeV BOTBYP375 = new EdgeV(-0.375/2,0);
	/* 0.4 */			private final EdgeV BOTBYP4 = new EdgeV(-0.4/2,0);
	/* 0.5 */			private final EdgeV BOTBYP5 = new EdgeV(-0.5/2,0);
	/* 0.6 */			private final EdgeV BOTBYP6 = new EdgeV(-0.6/2,0);
	/* 0.6666... */		private final EdgeV BOTBYP66 = new EdgeV(-0.6666666666/2,0);
//	/* 0.7 */			private final EdgeV BOTBYP7 = new EdgeV(-0.7/2,0);
	/* 0.75 */			private final EdgeV BOTBYP75 = new EdgeV(-0.75/2,0);
	/* 0.8 */			private final EdgeV BOTBYP8 = new EdgeV(-0.8/2,0);
	/* 0.875 */			private final EdgeV BOTBYP875 = new EdgeV(-0.875/2,0);
	/* 0.9 */			private final EdgeV BOTBYP9 = new EdgeV(-0.9/2,0);

	// this much from the center to the top edge
//	/* 0.1 */			private final EdgeV TOPBYP1 = new EdgeV(0.1/2,0);
	/* 0.2 */			private final EdgeV TOPBYP2 = new EdgeV(0.2/2,0);
	/* 0.25 */			private final EdgeV TOPBYP25 = new EdgeV(0.25/2,0);
	/* 0.3 */			private final EdgeV TOPBYP3 = new EdgeV(0.3/2,0);
	/* 0.3333... */		private final EdgeV TOPBYP33 = new EdgeV(0.3333333333/2,0);
	/* 0.4 */			private final EdgeV TOPBYP4 = new EdgeV(0.4/2,0);
	/* 0.5 */			private final EdgeV TOPBYP5 = new EdgeV(0.5/2,0);
	/* 0.5833... */		private final EdgeV TOPBYP5833 = new EdgeV(0.5833333333/2,0);
	/* 0.6 */			private final EdgeV TOPBYP6 = new EdgeV(0.6/2,0);
	/* 0.6666... */		private final EdgeV TOPBYP66 = new EdgeV(0.6666666666/2,0);
	/* 0.75 */			private final EdgeV TOPBYP75 = new EdgeV(0.75/2,0);
	/* 0.8 */			private final EdgeV TOPBYP8 = new EdgeV(0.8/2,0);
	/* 0.9 */			private final EdgeV TOPBYP9 = new EdgeV(0.9/2,0);

	// -------------------- private and protected methods ------------------------

	public Schematics(Generic generic, TechFactory techFactory)
	{
		super(generic, techFactory, Foundry.Type.NONE, 1);

		setTechShortName("Schematics");
		setTechDesc("Schematic Capture");
		setFactoryScale(2000, false);			// in nanometers: really 2 micron
		setNonStandard();
		setStaticTechnology();

//		setFactoryTransparentLayers(new Color []
//   		{
//   			new Color(107, 226, 96)  // Bus
//   		});

		//**************************************** LAYERS ****************************************

		/** arc layer */
		arc_lay = Layer.newInstance(this, "Arc",
			new EGraphics(false, false, null, 0, 0,0,255,0.8,true,
			new int[] {0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,
				0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF}));

		/** bus layer */
//		bus_lay = Layer.newInstance(this, "Bus",
//			new EGraphics(false, true, null, EGraphics.TRANSPARENT_1, 107,226,96, 0.8,true,
//			new int[] { 0x2222,   //   X   X   X   X
//				0x0000,   //
//				0x8888,   // X   X   X   X
//				0x0000,   //
//				0x2222,   //   X   X   X   X
//				0x0000,   //
//				0x8888,   // X   X   X   X
//				0x0000,   //
//				0x2222,   //   X   X   X   X
//				0x0000,   //
//				0x8888,   // X   X   X   X
//				0x0000,   //
//				0x2222,   //   X   X   X   X
//				0x0000,   //
//				0x8888,   // X   X   X   X
//				0x0000}));//
		bus_lay = Layer.newInstance(this, "Bus",
			new EGraphics(true, true, null, 0, 0,255,0, 0.8,true,
			new int[] { 0xAAAA,   // X X X X X X X X
						0x5555,   //  X X X X X X X X
						0xAAAA,   // X X X X X X X X
						0x5555,   //  X X X X X X X X
						0xAAAA,   // X X X X X X X X
						0x5555,   //  X X X X X X X X
						0xAAAA,   // X X X X X X X X
						0x5555,   //  X X X X X X X X
						0xAAAA,   // X X X X X X X X
						0x5555,   //  X X X X X X X X
						0xAAAA,   // X X X X X X X X
						0x5555,   //  X X X X X X X X
						0xAAAA,   // X X X X X X X X
						0x5555,   //  X X X X X X X X
						0xAAAA,   // X X X X X X X X
						0x5555}));//  X X X X X X X X

		/** node layer */
		node_lay = Layer.newInstance(this, "Node",
			new EGraphics(false, false, null, 0, 255,0,0, 0.8,true,
			new int[] {0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,
				0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF}));

		/** text layer */
		text_lay = Layer.newInstance(this, "Text",
			new EGraphics(false, false, null, 0, 0,0,0, 0.8,true,
			new int[] {0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,
				0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF}));

		// The layer functions
		arc_lay.setFunction(Layer.Function.METAL1);														// arc
		bus_lay.setFunction(Layer.Function.BUS);														// bus
		node_lay.setFunction(Layer.Function.ART);														// node
		text_lay.setFunction(Layer.Function.ART);														// text


		//**************************************** ARCS ****************************************

		/** wire arc */
		wire_arc = newArcProto("wire", 0, 0.0, ArcProto.Function.METAL1,
			new Technology.ArcLayer(arc_lay, 0, Poly.Type.FILLED)
		);
		wire_arc.setFactoryFixedAngle(true);
		wire_arc.setFactorySlidable(false);
		wire_arc.setFactoryAngleIncrement(45);

		/** bus arc */
		bus_arc = newArcProto("bus", 0, 1.0, ArcProto.Function.BUS,
			new Technology.ArcLayer(bus_lay, 1, Poly.Type.FILLED)
		);
		bus_arc.setFactoryFixedAngle(true);
		bus_arc.setFactorySlidable(false);
		bus_arc.setFactoryExtended(false);
		bus_arc.setFactoryAngleIncrement(45);


		//**************************************** NODES ****************************************

		// this text descriptor is used for all text on nodes
		TextDescriptor tdBig = TextDescriptor.EMPTY.withRelSize(2);
		TextDescriptor tdSmall = TextDescriptor.EMPTY.withRelSize(1);

		/** wire pin */
		wirePinNode = PrimitiveNode.newInstance("Wire_Pin", this, 0.5, 0.5, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(arc_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())})
			});
		wirePinNode.addPrimitivePortsFixed(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, wirePinNode, new ArcProto[] {wire_arc}, "wire", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		wirePinNode.setFunction(PrimitiveNode.Function.PIN);
		wirePinNode.setSquare();
		wirePinNode.setWipeOn1or2();
		wirePinNode.setCanBeZeroSize();

		/** bus pin */
		busPinNode = PrimitiveNode.newInstance("Bus_Pin", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(bus_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())}),
				new Technology.NodeLayer(arc_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())})
			});
		busPinNode.addPrimitivePortsFixed(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, busPinNode, new ArcProto[] {wire_arc, bus_arc}, "bus", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		busPinNode.setFunction(PrimitiveNode.Function.PIN);
		busPinNode.setSquare();
//		busPinNode.setWipeOn1or2();  Bus Pin has more complicated wipe rules
		busPinNode.setCanBeZeroSize();

		/** wire con */
		Technology.NodeLayer letterJ;
		wireConNode = PrimitiveNode.newInstance("Wire_Con", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				letterJ = new Technology.NodeLayer(text_lay, 0, Poly.Type.TEXTCENT, Technology.NodeLayer.POINTS, Technology.TechPoint.makeCenterBox())
			});
		PrimitivePort wireCon_port = PrimitivePort.newInstance(this, wireConNode, new ArcProto[] {wire_arc, bus_arc}, "wire", 0,180, 0, PortCharacteristic.UNKNOWN,
			EdgeH.fromLeft(0.5), EdgeV.fromBottom(0.5), EdgeH.fromRight(0.5), EdgeV.fromTop(0.5));
		wireCon_port.setIsolated();
		wireConNode.addPrimitivePortsFixed(new PrimitivePort [] {wireCon_port});
		wireConNode.setFunction(PrimitiveNode.Function.CONNECT);
		letterJ.setMessage("J");
		letterJ.setDescriptor(tdBig);

		/** general buffer */
		bufferNode = PrimitiveNode.newInstance("Buffer", this, 6.0, 6.0, new SizeOffset(0, 1, 0.25, 0.25),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.POINTS,
					new Technology.TechPoint [] {
						new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.makeCenter()),
						new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromTop(0.25)),
						new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.fromBottom(0.25))
					})
			});
		PrimitivePort bufferInPort = PrimitivePort.newInstance(this, bufferNode, new ArcProto[] {wire_arc, bus_arc}, "a", 180,0, 0,
			PortCharacteristic.IN, EdgeH.makeLeftEdge(), EdgeV.makeCenter(), EdgeH.makeLeftEdge(), EdgeV.makeCenter());
		bufferInPort.setNegatable(true);
		PrimitivePort bufferSidePort = PrimitivePort.newInstance(this, bufferNode, new ArcProto[] {wire_arc}, "c", 270,0, 1,
			PortCharacteristic.IN, EdgeH.makeCenter(), EdgeV.fromBottom(2), EdgeH.makeCenter(), EdgeV.fromBottom(2));
		bufferSidePort.setNegatable(true);
		PrimitivePort bufferOutPort = PrimitivePort.newInstance(this, bufferNode, new ArcProto[] {wire_arc, bus_arc}, "y", 0,0, 2,
			PortCharacteristic.OUT, EdgeH.fromRight(1), EdgeV.makeCenter(), EdgeH.fromRight(1), EdgeV.makeCenter());
		bufferOutPort.setNegatable(true);
		bufferNode.addPrimitivePortsFixed(new PrimitivePort [] { bufferInPort, bufferSidePort, bufferOutPort});
		bufferNode.setFunction(PrimitiveNode.Function.BUFFER);

		/** general and */
		andNode = PrimitiveNode.newInstance("And", this, 8.0, 6.0, new SizeOffset(0, 0.5, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(0.5), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromCenter(0.5), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(0.5), EdgeV.fromCenter(-3))}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS,
					new Technology.TechPoint [] {
						new Technology.TechPoint(EdgeH.fromCenter(0.5), EdgeV.fromCenter(3)),
						new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(3)),
						new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.makeTopEdge()),
						new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.makeBottomEdge()),
						new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(-3)),
						new Technology.TechPoint(EdgeH.fromCenter(0.5), EdgeV.fromCenter(-3))
					})
			});
		PrimitivePort andInPort = PrimitivePort.newInstance(this, andNode, new ArcProto[] {wire_arc, bus_arc}, "a", 180,0, 0,
			PortCharacteristic.IN, EdgeH.fromCenter(-4), EdgeV.makeBottomEdge(), EdgeH.fromCenter(-4), EdgeV.makeTopEdge());
		andInPort.setIsolated();
		andInPort.setNegatable(true);
		PrimitivePort andOutPort = PrimitivePort.newInstance(this, andNode, new ArcProto[] {wire_arc, bus_arc}, "y", 0,0, 1,
			PortCharacteristic.OUT, EdgeH.fromCenter(3.5), EdgeV.makeCenter(), EdgeH.fromCenter(3.5), EdgeV.makeCenter());
		andOutPort.setNegatable(true);

		PrimitivePort andTopPort = PrimitivePort.newInstance(this, andNode, new ArcProto[] {wire_arc, bus_arc}, "yt", 0,0, 2,
			PortCharacteristic.OUT, EdgeH.fromCenter(2.75), EdgeV.fromCenter(2), EdgeH.fromCenter(2.75), EdgeV.fromCenter(2));
		andTopPort.setNegatable(true);
		PrimitivePort andBottomPort = PrimitivePort.newInstance(this, andNode, new ArcProto[] {wire_arc, bus_arc}, "yc", 0,0, 3,
			PortCharacteristic.OUT, EdgeH.fromCenter(2.75), EdgeV.fromCenter(-2), EdgeH.fromCenter(2.75), EdgeV.fromCenter(-2));
		andBottomPort.setNegatable(true);
		andNode.addPrimitivePortsFixed(new PrimitivePort [] { andInPort, andOutPort, andTopPort, andBottomPort});
		andNode.setFunction(PrimitiveNode.Function.GATEAND);
		andNode.setAutoGrowth(0, 4);

		/** general or */
		orNode = PrimitiveNode.newInstance("Or", this, 10.0, 6.0, new SizeOffset(1, 0.5, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-9), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(-3))}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(-3)),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(4.5), EdgeV.makeCenter())}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(4.5), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(-3))}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.makeTopEdge()),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.makeBottomEdge()),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(-3)),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(-3)),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(-3))
				})
			});
		PrimitivePort orInPort = PrimitivePort.newInstance(this, orNode, new ArcProto[] {wire_arc, bus_arc}, "a", 180,0, 0,
			PortCharacteristic.IN, EdgeH.fromCenter(-4), EdgeV.makeBottomEdge(), EdgeH.fromCenter(-3), EdgeV.makeTopEdge());
		orInPort.setIsolated();
		orInPort.setNegatable(true);
		PrimitivePort orOutPort = PrimitivePort.newInstance(this, orNode, new ArcProto[] {wire_arc, bus_arc}, "y", 0,0, 1,
			PortCharacteristic.OUT, EdgeH.fromCenter(4.5), EdgeV.makeCenter(), EdgeH.fromCenter(4.5), EdgeV.makeCenter());
		orOutPort.setNegatable(true);
		PrimitivePort orTopPort = PrimitivePort.newInstance(this, orNode, new ArcProto[] {wire_arc, bus_arc}, "yt", 0,0, 2,
			PortCharacteristic.OUT, EdgeH.fromCenter(2.65), EdgeV.fromCenter(2), EdgeH.fromCenter(2.65), EdgeV.fromCenter(2));
		orTopPort.setNegatable(true);
		PrimitivePort orBottomPort = PrimitivePort.newInstance(this, orNode, new ArcProto[] {wire_arc, bus_arc}, "yc", 0,0, 3,
			PortCharacteristic.OUT, EdgeH.fromCenter(2.65), EdgeV.fromCenter(-2), EdgeH.fromCenter(2.65), EdgeV.fromCenter(-2));
		orBottomPort.setNegatable(true);
		orNode.addPrimitivePortsFixed(new PrimitivePort [] {orInPort, orOutPort, orTopPort, orBottomPort});
		orNode.setFunction(PrimitiveNode.Function.GATEOR);
		orNode.setAutoGrowth(0, 4);

		/** general xor */
		xorNode = PrimitiveNode.newInstance("Xor", this, 10.0, 6.0, new SizeOffset(0, 0.5, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-9), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(-3))}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(-3)),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(4.5), EdgeV.makeCenter())}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(4.5), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(-3))}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-10), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromCenter(-5), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(-5), EdgeV.fromCenter(-3))}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.makeTopEdge()),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.makeBottomEdge()),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(-3)),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(-3)),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(-3))
				})
			});
		PrimitivePort xorInPort = PrimitivePort.newInstance(this, xorNode, new ArcProto[] {wire_arc, bus_arc}, "a", 180,0, 0, PortCharacteristic.IN,
			EdgeH.fromCenter(-4), EdgeV.makeBottomEdge(), EdgeH.fromCenter(-3), EdgeV.makeTopEdge());
		xorInPort.setIsolated();
		xorInPort.setNegatable(true);
		PrimitivePort xorOutPort = PrimitivePort.newInstance(this, xorNode, new ArcProto[] {wire_arc, bus_arc}, "y", 0,0, 1,
			PortCharacteristic.OUT, EdgeH.fromCenter(4.5), EdgeV.makeCenter(), EdgeH.fromCenter(4.5), EdgeV.makeCenter());
		xorOutPort.setNegatable(true);
		PrimitivePort xorTopPort = PrimitivePort.newInstance(this, xorNode, new ArcProto[] {wire_arc, bus_arc}, "yt", 0,0, 2,
			PortCharacteristic.OUT, EdgeH.fromCenter(2.65), EdgeV.fromCenter(2), EdgeH.fromCenter(2.65), EdgeV.fromCenter(2));
		xorTopPort.setNegatable(true);
		PrimitivePort xorBottomPort = PrimitivePort.newInstance(this, xorNode, new ArcProto[] {wire_arc, bus_arc}, "yc", 0,0, 3,
			PortCharacteristic.OUT, EdgeH.fromCenter(2.65), EdgeV.fromCenter(-2), EdgeH.fromCenter(2.65), EdgeV.fromCenter(-2));
		xorBottomPort.setNegatable(true);
		xorNode.addPrimitivePortsFixed(new PrimitivePort [] {xorInPort, xorOutPort, xorTopPort, xorBottomPort});
		xorNode.setFunction(PrimitiveNode.Function.GATEXOR);
		xorNode.setAutoGrowth(0, 4);

		/** general flip flop */
		Technology.NodeLayer ffBox = new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox());
		Technology.NodeLayer ffArrow = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
				new Technology.TechPoint(EdgeH.makeLeftEdge(), BOTBYP2),
				new Technology.TechPoint(LEFTBYP7, EdgeV.makeCenter()),
				new Technology.TechPoint(EdgeH.makeLeftEdge(), TOPBYP2)});
		Technology.NodeLayer ffWaveformN = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP6, TOPBYP2),
			new Technology.TechPoint(LEFTBYP4, TOPBYP2),
			new Technology.TechPoint(LEFTBYP4, BOTBYP2),
			new Technology.TechPoint(LEFTBYP2, BOTBYP2)});
		Technology.NodeLayer ffWaveformP = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP6, BOTBYP2),
			new Technology.TechPoint(LEFTBYP4, BOTBYP2),
			new Technology.TechPoint(LEFTBYP4, TOPBYP2),
			new Technology.TechPoint(LEFTBYP2, TOPBYP2)});
		Technology.NodeLayer ffWaveformMS = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP6, BOTBYP2),
			new Technology.TechPoint(LEFTBYP4, BOTBYP2),
			new Technology.TechPoint(LEFTBYP4, TOPBYP2),
			new Technology.TechPoint(LEFTBYP2, TOPBYP2),
			new Technology.TechPoint(LEFTBYP2, BOTBYP2),
			new Technology.TechPoint(EdgeH.makeCenter(), BOTBYP2)});
		Technology.NodeLayer ffLetterD = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), TOPBYP4),
			new Technology.TechPoint(EdgeH.makeLeftEdge(), TOPBYP8),
			new Technology.TechPoint(LEFTBYP4,       TOPBYP8),
			new Technology.TechPoint(LEFTBYP4,       TOPBYP4)});
		ffLetterD.setMessage("D");
		ffLetterD.setDescriptor(tdSmall);
		Technology.NodeLayer ffLetterR = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), TOPBYP4),
			new Technology.TechPoint(EdgeH.makeLeftEdge(), TOPBYP8),
			new Technology.TechPoint(LEFTBYP4,       TOPBYP8),
			new Technology.TechPoint(LEFTBYP4,       TOPBYP4)});
		ffLetterR.setMessage("R");
		ffLetterR.setDescriptor(tdSmall);
		Technology.NodeLayer ffLetterJ = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), TOPBYP4),
			new Technology.TechPoint(EdgeH.makeLeftEdge(), TOPBYP8),
			new Technology.TechPoint(LEFTBYP4,       TOPBYP8),
			new Technology.TechPoint(LEFTBYP4,       TOPBYP4)});
		ffLetterJ.setMessage("J");
		ffLetterJ.setDescriptor(tdSmall);
		Technology.NodeLayer ffLetterT = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), TOPBYP4),
			new Technology.TechPoint(EdgeH.makeLeftEdge(), TOPBYP8),
			new Technology.TechPoint(LEFTBYP4,       TOPBYP8),
			new Technology.TechPoint(LEFTBYP4,       TOPBYP4)});
		ffLetterT.setMessage("T");
		ffLetterT.setDescriptor(tdSmall);
		Technology.NodeLayer ffLetterE = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), BOTBYP4),
			new Technology.TechPoint(EdgeH.makeLeftEdge(), BOTBYP8),
			new Technology.TechPoint(LEFTBYP4,       BOTBYP8),
			new Technology.TechPoint(LEFTBYP4,       BOTBYP4)});
		ffLetterE.setMessage("E");
		ffLetterE.setDescriptor(tdSmall);
		Technology.NodeLayer ffLetterS = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), BOTBYP4),
			new Technology.TechPoint(EdgeH.makeLeftEdge(), BOTBYP8),
			new Technology.TechPoint(LEFTBYP4,       BOTBYP8),
			new Technology.TechPoint(LEFTBYP4,       BOTBYP4)});
		ffLetterS.setMessage("S");
		ffLetterS.setDescriptor(tdSmall);
		Technology.NodeLayer ffLetterK = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), BOTBYP4),
			new Technology.TechPoint(EdgeH.makeLeftEdge(), BOTBYP8),
			new Technology.TechPoint(LEFTBYP4,       BOTBYP8),
			new Technology.TechPoint(LEFTBYP4,       BOTBYP4)});
		ffLetterK.setMessage("K");
		ffLetterK.setDescriptor(tdSmall);
		Technology.NodeLayer ffLetterQ = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeRightEdge(), TOPBYP4),
			new Technology.TechPoint(EdgeH.makeRightEdge(), TOPBYP8),
			new Technology.TechPoint(RIGHTBYP4,       TOPBYP8),
			new Technology.TechPoint(RIGHTBYP4,       TOPBYP4)});
		ffLetterQ.setMessage("Q");
		ffLetterQ.setDescriptor(tdSmall);
		Technology.NodeLayer ffLetterQB = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeRightEdge(), BOTBYP4),
			new Technology.TechPoint(EdgeH.makeRightEdge(), BOTBYP8),
			new Technology.TechPoint(RIGHTBYP4,       BOTBYP8),
			new Technology.TechPoint(RIGHTBYP4,       BOTBYP4)});
		ffLetterQB.setMessage("QB");
		ffLetterQB.setDescriptor(tdSmall);
		Technology.NodeLayer ffLetterPR = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP6,        TOPBYP6),
			new Technology.TechPoint(LEFTBYP6,        EdgeV.makeTopEdge()),
			new Technology.TechPoint(RIGHTBYP6,       EdgeV.makeTopEdge()),
			new Technology.TechPoint(RIGHTBYP6,       TOPBYP6)});
		ffLetterPR.setMessage("PR");
		ffLetterPR.setDescriptor(tdSmall);
		Technology.NodeLayer ffLetterCLR = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP6,        BOTBYP6),
			new Technology.TechPoint(LEFTBYP6,        EdgeV.makeBottomEdge()),
			new Technology.TechPoint(RIGHTBYP6,       EdgeV.makeBottomEdge()),
			new Technology.TechPoint(RIGHTBYP6,       BOTBYP6)});
		ffLetterCLR.setMessage("CLR");
		ffLetterCLR.setDescriptor(tdSmall);
		ffLayersRSMS = new Technology.NodeLayer []
		{
			ffWaveformMS, ffLetterR, ffLetterS,
			ffBox, ffArrow, ffLetterQ, ffLetterQB, ffLetterPR, ffLetterCLR
		};
		ffLayersRSP = new Technology.NodeLayer []
		{
			ffWaveformP, ffLetterR, ffLetterS,
			ffBox, ffArrow, ffLetterQ, ffLetterQB, ffLetterPR, ffLetterCLR
		};
		ffLayersRSN = new Technology.NodeLayer []
		{
			ffWaveformN, ffLetterR, ffLetterS,
			ffBox, ffArrow, ffLetterQ, ffLetterQB, ffLetterPR, ffLetterCLR
		};
		ffLayersJKMS = new Technology.NodeLayer []
		{
			ffWaveformMS, ffLetterJ, ffLetterK,
			ffBox, ffArrow, ffLetterQ, ffLetterQB, ffLetterPR, ffLetterCLR
		};
		ffLayersJKP = new Technology.NodeLayer []
		{
			ffWaveformP, ffLetterJ, ffLetterK,
			ffBox, ffArrow, ffLetterQ, ffLetterQB, ffLetterPR, ffLetterCLR
		};
		ffLayersJKN = new Technology.NodeLayer []
		{
			ffWaveformN, ffLetterJ, ffLetterK,
			ffBox, ffArrow, ffLetterQ, ffLetterQB, ffLetterPR, ffLetterCLR
		};
		ffLayersDMS = new Technology.NodeLayer []
		{
			ffWaveformMS, ffLetterD, ffLetterE,
			ffBox, ffArrow, ffLetterQ, ffLetterQB, ffLetterPR, ffLetterCLR
		};
		ffLayersDP = new Technology.NodeLayer []
		{
			ffWaveformP, ffLetterD, ffLetterE,
			ffBox, ffArrow, ffLetterQ, ffLetterQB, ffLetterPR, ffLetterCLR
		};
		ffLayersDN = new Technology.NodeLayer []
		{
			ffWaveformN, ffLetterD, ffLetterE,
			ffBox, ffArrow, ffLetterQ, ffLetterQB, ffLetterPR, ffLetterCLR
		};
		ffLayersTMS = new Technology.NodeLayer []
		{
			ffWaveformMS, ffLetterT,
			ffBox, ffArrow, ffLetterQ, ffLetterQB, ffLetterPR, ffLetterCLR
		};
		ffLayersTP = new Technology.NodeLayer []
		{
			ffWaveformP, ffLetterT,
			ffBox, ffArrow, ffLetterQ, ffLetterQB, ffLetterPR, ffLetterCLR
		};
		ffLayersTN = new Technology.NodeLayer []
		{
			ffWaveformN, ffLetterT,
			ffBox, ffArrow, ffLetterQ, ffLetterQB, ffLetterPR, ffLetterCLR
		};
		flipflopNode = PrimitiveNode.newInstance("Flip-Flop", this, 6.0, 10.0, null, ffLayersRSMS);
		PrimitivePort flipflopI1 = PrimitivePort.newInstance(this, flipflopNode, new ArcProto[] {wire_arc}, "i1", 180,45, 0,
			PortCharacteristic.IN, EdgeH.makeLeftEdge(), TOPBYP6, EdgeH.makeLeftEdge(), TOPBYP6);
		flipflopI1.setNegatable(true);
		PrimitivePort flipflopI2 = PrimitivePort.newInstance(this, flipflopNode, new ArcProto[] {wire_arc}, "i2", 180,45, 1,
			PortCharacteristic.IN, EdgeH.makeLeftEdge(), BOTBYP6, EdgeH.makeLeftEdge(), BOTBYP6);
		flipflopI2.setNegatable(true);
		PrimitivePort flipflopQ = PrimitivePort.newInstance(this, flipflopNode, new ArcProto[] {wire_arc}, "q", 0,45, 2,
			PortCharacteristic.OUT, EdgeH.makeRightEdge(), TOPBYP6, EdgeH.makeRightEdge(), TOPBYP6);
		flipflopQ.setNegatable(true);
		PrimitivePort flipflopQB = PrimitivePort.newInstance(this, flipflopNode, new ArcProto[] {wire_arc}, "qb", 0,45, 3,
			PortCharacteristic.OUT, EdgeH.makeRightEdge(), BOTBYP6, EdgeH.makeRightEdge(), BOTBYP6);
		flipflopQB.setNegatable(true);
		PrimitivePort flipflopCK = PrimitivePort.newInstance(this, flipflopNode, new ArcProto[] {wire_arc}, "ck", 180,45, 4,
			PortCharacteristic.IN, EdgeH.makeLeftEdge(), EdgeV.makeCenter(), EdgeH.makeLeftEdge(), EdgeV.makeCenter());
		flipflopCK.setNegatable(true);
		PrimitivePort flipflopPRE = PrimitivePort.newInstance(this, flipflopNode, new ArcProto[] {wire_arc}, "preset", 90,45, 5,
			PortCharacteristic.IN, EdgeH.makeCenter(), EdgeV.makeTopEdge(), EdgeH.makeCenter(), EdgeV.makeTopEdge());
		flipflopPRE.setNegatable(true);
		PrimitivePort flipflopCLR = PrimitivePort.newInstance(this, flipflopNode, new ArcProto[] {wire_arc}, "clear", 270,45, 6,
			PortCharacteristic.IN, EdgeH.makeCenter(), EdgeV.makeBottomEdge(), EdgeH.makeCenter(), EdgeV.makeBottomEdge());
		flipflopCLR.setNegatable(true);
		flipflopNode.addPrimitivePortsFixed(new PrimitivePort []
			{flipflopI1, flipflopI2, flipflopQ, flipflopQB, flipflopCK, flipflopPRE, flipflopCLR});
		flipflopNode.setFunction(PrimitiveNode.Function.FLIPFLOPRSMS);

		/** mux */
		muxNode = PrimitiveNode.newInstance("Mux", this, 8.0, 10.0, new SizeOffset(0.5, 0.5, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.POINTS,
					new Technology.TechPoint [] {
						new Technology.TechPoint(RIGHTBYP8, TOPBYP75),
						new Technology.TechPoint(RIGHTBYP8, BOTBYP75),
						new Technology.TechPoint(LEFTBYP8, EdgeV.makeBottomEdge()),
						new Technology.TechPoint(LEFTBYP8, EdgeV.makeTopEdge())
					})
			});
		PrimitivePort muxInPort = PrimitivePort.newInstance(this, muxNode, new ArcProto[] {wire_arc, bus_arc}, "a", 180,0, 0,
			PortCharacteristic.IN, LEFTBYP8, EdgeV.makeBottomEdge(), LEFTBYP8, EdgeV.makeTopEdge());
		muxInPort.setIsolated();
		muxInPort.setNegatable(true);
		PrimitivePort muxSidePort = PrimitivePort.newInstance(this, muxNode, new ArcProto[] {wire_arc}, "s", 270,0, 2,
			PortCharacteristic.IN, EdgeH.makeCenter(), BOTBYP875, EdgeH.makeCenter(), BOTBYP875);
		muxSidePort.setNegatable(true);
		PrimitivePort muxOutPort = PrimitivePort.newInstance(this, muxNode, new ArcProto[] {wire_arc, bus_arc}, "y", 0,0, 1,
			PortCharacteristic.OUT, RIGHTBYP8, EdgeV.makeCenter(), RIGHTBYP8, EdgeV.makeCenter());
		muxOutPort.setNegatable(true);
		muxNode.addPrimitivePortsFixed(new PrimitivePort [] {muxInPort, muxSidePort, muxOutPort});
		muxNode.setFunction(PrimitiveNode.Function.MUX);
		muxNode.setAutoGrowth(0, 4);

		/** black box */
		bboxNode = PrimitiveNode.newInstance("Bbox", this, 10.0, 10.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		PrimitivePort bbox_port1 = PrimitivePort.newInstance(this, bboxNode, new ArcProto[] {wire_arc, bus_arc}, "a", 0,45, 0, PortCharacteristic.UNKNOWN,
			EdgeH.makeRightEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge());
		bbox_port1.setIsolated();
		bbox_port1.setNegatable(true);
		PrimitivePort bbox_port2 = PrimitivePort.newInstance(this, bboxNode, new ArcProto[] {wire_arc, bus_arc}, "b", 90,45, 1, PortCharacteristic.UNKNOWN,
			EdgeH.makeLeftEdge(), EdgeV.makeTopEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge());
		bbox_port2.setIsolated();
		bbox_port2.setNegatable(true);
		PrimitivePort bbox_port3 = PrimitivePort.newInstance(this, bboxNode, new ArcProto[] {wire_arc, bus_arc}, "c", 180,45, 2, PortCharacteristic.UNKNOWN,
			EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeLeftEdge(), EdgeV.makeTopEdge());
		bbox_port3.setIsolated();
		bbox_port3.setNegatable(true);
		PrimitivePort bbox_port4 = PrimitivePort.newInstance(this, bboxNode, new ArcProto[] {wire_arc, bus_arc}, "d", 270,45, 3, PortCharacteristic.UNKNOWN,
			EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeBottomEdge());
		bbox_port4.setIsolated();
		bbox_port4.setNegatable(true);
		bboxNode.addPrimitivePortsFixed(new PrimitivePort [] {bbox_port1, bbox_port2, bbox_port3, bbox_port4});
		bboxNode.setFunction(PrimitiveNode.Function.UNKNOWN);

		/** switch */
		switchNode = PrimitiveNode.newInstance("Switch", this, 6.0, 2.0, new SizeOffset(0.5, 0.5, 0.5, 0.5),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromRight(1.25), EdgeV.makeCenter())}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.makeCenter())}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromCenter(1))})
			});
		PrimitivePort switch_port = PrimitivePort.newInstance(this, switchNode, new ArcProto[] {wire_arc, bus_arc}, "a", 180,90, 0, PortCharacteristic.UNKNOWN,
			EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromLeft(1), EdgeV.fromTop(1));
		switch_port.setIsolated();
		switchNode.addPrimitivePortsFixed(new PrimitivePort []
			{
				switch_port,
				PrimitivePort.newInstance(this, switchNode, new ArcProto[] {wire_arc, bus_arc}, "y", 0,90, 1, PortCharacteristic.UNKNOWN,
					EdgeH.fromRight(1), EdgeV.makeCenter(), EdgeH.fromRight(1), EdgeV.makeCenter())
			});
		switchNode.setFunction(PrimitiveNode.Function.UNKNOWN);
		switchNode.setAutoGrowth(0, 4);

		/** off page connector */
        offPageLayers =
			new Technology.NodeLayer [] {
            new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
                    new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
                    new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeTopEdge()),
                    new Technology.TechPoint(RIGHTBYP5, EdgeV.makeTopEdge()),
                    new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
                    new Technology.TechPoint(RIGHTBYP5, EdgeV.makeBottomEdge()),
                }),
        };
        offPageOutputLayers =
			new Technology.NodeLayer [] {
            new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
                    new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
                    new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
                    new Technology.TechPoint(LEFTBYP5, EdgeV.makeCenter()),
                    new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
                    new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeTopEdge()),
                    new Technology.TechPoint(RIGHTBYP5, EdgeV.makeTopEdge()),
                    new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
                    new Technology.TechPoint(RIGHTBYP5, EdgeV.makeBottomEdge()),
                }),
        };
        offPageInputLayers =
			new Technology.NodeLayer [] {
            new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
                    new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
                    new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeTopEdge()),
                    new Technology.TechPoint(RIGHTBYP5, EdgeV.makeTopEdge()),
                    new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
                    new Technology.TechPoint(RIGHTBYP5, EdgeV.makeCenter()),
                    new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
                    new Technology.TechPoint(RIGHTBYP5, EdgeV.makeBottomEdge()),
                }),
        };
        offPageBidirectionalLayers =
			new Technology.NodeLayer [] {
            new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
                    new Technology.TechPoint(LEFTBYP5, EdgeV.makeBottomEdge()),
                    new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
                    new Technology.TechPoint(LEFTBYP5, EdgeV.makeTopEdge()),
                    new Technology.TechPoint(RIGHTBYP5, EdgeV.makeTopEdge()),
                    new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
                    new Technology.TechPoint(RIGHTBYP5, EdgeV.makeBottomEdge()),
                }),
        };
        offpageNode = PrimitiveNode.newInstance("Off-Page", this, 4.0, 2.0, null, offPageLayers);

		offpageNode.addPrimitivePortsFixed(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, offpageNode, new ArcProto[] {wire_arc, bus_arc}, "a", 180,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeCenter(), EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
				PrimitivePort.newInstance(this, offpageNode, new ArcProto[] {wire_arc, bus_arc}, "y", 0,45, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeRightEdge(), EdgeV.makeCenter(), EdgeH.makeRightEdge(), EdgeV.makeCenter())
			});
		offpageNode.setFunction(PrimitiveNode.Function.CONNECT);

		/** power */
		powerNode = PrimitiveNode.newInstance("Power", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeTopEdge())}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeCenter(), TOPBYP75)})
			});
		powerNode.addPrimitivePortsFixed(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, powerNode, new ArcProto[] {wire_arc}, "vdd", 0,180, 0, PortCharacteristic.PWR,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		powerNode.setFunction(PrimitiveNode.Function.CONPOWER);
		powerNode.setSquare();

		/** ground */
		groundNode = PrimitiveNode.newInstance("Ground", this, 3.0, 4.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()), new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeTopEdge()),
					new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeCenter()), new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
					new Technology.TechPoint(LEFTBYP75, BOTBYP25), new Technology.TechPoint(RIGHTBYP75, BOTBYP25),
					new Technology.TechPoint(LEFTBYP5, BOTBYP5), new Technology.TechPoint(RIGHTBYP5, BOTBYP5),
					new Technology.TechPoint(LEFTBYP25, BOTBYP75), new Technology.TechPoint(RIGHTBYP25, BOTBYP75),
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeBottomEdge()), new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeBottomEdge())})
			});
		groundNode.addPrimitivePortsFixed(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, groundNode, new ArcProto[] {wire_arc}, "gnd", 90,90, 0, PortCharacteristic.GND,
					EdgeH.makeCenter(), EdgeV.makeTopEdge(), EdgeH.makeCenter(), EdgeV.makeTopEdge())
			});
		groundNode.setFunction(PrimitiveNode.Function.CONGROUND);

		/** source */
		sourceNode = PrimitiveNode.newInstance("Source", this, 6.0, 6.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(LEFTBYP3, TOPBYP6), new Technology.TechPoint(RIGHTBYP3, TOPBYP6),
					new Technology.TechPoint(EdgeH.makeCenter(), TOPBYP3), new Technology.TechPoint(EdgeH.makeCenter(), TOPBYP9)})
			});
		sourceNode.addPrimitivePortsFixed(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, sourceNode, new ArcProto[] {wire_arc}, "plus", 90,0, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeTopEdge(), EdgeH.makeCenter(), EdgeV.makeTopEdge()),
				PrimitivePort.newInstance(this, sourceNode, new ArcProto[] {wire_arc}, "minus", 270,0, 1, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeBottomEdge(), EdgeH.makeCenter(), EdgeV.makeBottomEdge())
			});
		sourceNode.setFunction(PrimitiveNode.Function.SOURCE);
		sourceNode.setSquare();

		/** transistor */
		Technology.NodeLayer tranLayerTranTop = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP75, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP75, BOTBYP25)});
		Technology.NodeLayer tranLayerNMOS = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeCenter(), BOTBYP25),
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.fromTop(1))});
		Technology.NodeLayer tranLayerBTran1 = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(LEFTBYP75, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(LEFTBYP25, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP25, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP75, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeBottomEdge())});
		Technology.NodeLayer tranLayerBTran2 = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP75, BOTBYP75),
			new Technology.TechPoint(LEFTBYP75, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(LEFTBYP5, BOTBYP875)});
		Technology.NodeLayer tranLayerBTran3 = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP5, BOTBYP375),
			new Technology.TechPoint(LEFTBYP25, BOTBYP25),
			new Technology.TechPoint(LEFTBYP25, BOTBYP5)});
		Technology.NodeLayer tranLayerBTran4 = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(LEFTBYP75, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(LEFTBYP75, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(LEFTBYP75, BOTBYP25),
			new Technology.TechPoint(LEFTBYP875, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP875, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP75, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP75, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(RIGHTBYP75, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeBottomEdge())});
		Technology.NodeLayer tranLayerBTran5 = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP125, EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.makeCenter(), BOTBYP25),
			new Technology.TechPoint(RIGHTBYP125, EdgeV.makeCenter())});
		Technology.NodeLayer tranLayerBTran6 = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP125, EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.makeCenter(), TOPBYP25),
			new Technology.TechPoint(RIGHTBYP125, EdgeV.makeCenter())});
		Technology.NodeLayer tranLayerBTran7 = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(LEFTBYP75, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(LEFTBYP75, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(LEFTBYP75, BOTBYP25),
			new Technology.TechPoint(LEFTBYP875, BOTBYP25),
			new Technology.TechPoint(LEFTBYP5, BOTBYP25),
			new Technology.TechPoint(LEFTBYP25, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP25, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP5, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP875, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP75, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP75, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(RIGHTBYP75, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeBottomEdge())});
		tranLayersN = buildTransistorDescription(true, false, false, false, 0, 0, false, false);
		tranLayersP = buildTransistorDescription(false, false, false, false, 0, 0, false, false);
		tranLayersNd = buildTransistorDescription(true, true, false, false, 0, 0, false, false);
		tranLayersPd = buildTransistorDescription(false, true, false, false, 0, 0, false, false);
		tranLayersNnT = buildTransistorDescription(true, false, true, false, 0, 0, false, false);
		tranLayersPnT = buildTransistorDescription(false, false, true, false, 0, 0, false, false);
		tranLayersNfG = buildTransistorDescription(true, false, false, true, 0, 0, false, false);
		tranLayersPfG = buildTransistorDescription(false, false, false, true, 0, 0, false, false);
		tranLayersNCN = buildTransistorDescription(true, false, false, false, 0, 0, false, true);
		tranLayersPCN = buildTransistorDescription(false, false, false, false, 0, 0, false, true);
		tranLayersNvtL = buildTransistorDescription(true, false, false, false, -1, 0, false, false);
		tranLayersPvtL = buildTransistorDescription(false, false, false, false, -1, 0, false, false);
		tranLayersNvtH = buildTransistorDescription(true, false, false, false, 1, 0, false, false);
		tranLayersPvtH = buildTransistorDescription(false, false, false, false, 1, 0, false, false);
		tranLayersNht1 = buildTransistorDescription(true, false, false, false, 0, 1, false, false);
		tranLayersPht1 = buildTransistorDescription(false, false, false, false, 0, 1, false, false);
		tranLayersNht2 = buildTransistorDescription(true, false, false, false, 0, 2, false, false);
		tranLayersPht2 = buildTransistorDescription(false, false, false, false, 0, 2, false, false);
		tranLayersNht3 = buildTransistorDescription(true, false, false, false, 0, 3, false, false);
		tranLayersPht3 = buildTransistorDescription(false, false, false, false, 0, 3, false, false);
		tranLayersNnTht1 = buildTransistorDescription(true, false, true, false, 0, 1, false, false);
		tranLayersPnTht1 = buildTransistorDescription(false, false, true, false, 0, 1, false, false);
		tranLayersNnTht2 = buildTransistorDescription(true, false, true, false, 0, 2, false, false);
		tranLayersPnTht2 = buildTransistorDescription(false, false, true, false, 0, 2, false, false);
		tranLayersNnTht3 = buildTransistorDescription(true, false, true, false, 0, 3, false, false);
		tranLayersPnTht3 = buildTransistorDescription(false, false, true, false, 0, 3, false, false);
		tranLayersNPN = new Technology.NodeLayer [] {tranLayerBTran1, tranLayerTranTop, tranLayerNMOS, tranLayerBTran2};
		tranLayersPNP = new Technology.NodeLayer [] {tranLayerBTran1, tranLayerTranTop, tranLayerNMOS, tranLayerBTran3};
		tranLayersNJFET = new Technology.NodeLayer [] {tranLayerBTran4, tranLayerTranTop, tranLayerNMOS, tranLayerBTran5};
		tranLayersPJFET = new Technology.NodeLayer [] {tranLayerBTran4, tranLayerTranTop, tranLayerNMOS, tranLayerBTran6};
		tranLayersDMES = new Technology.NodeLayer [] {tranLayerBTran4, tranLayerTranTop, tranLayerNMOS};
		tranLayersEMES = new Technology.NodeLayer [] {tranLayerBTran7, tranLayerNMOS};
		transistorNode = PrimitiveNode.newInstance("Transistor", this, 4.0, 4.0, new SizeOffset(0, 0, 0, 1), tranLayersN);
		transistorNode.addPrimitivePortsFixed(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, transistorNode, new ArcProto[] {wire_arc}, "g", 0,180, 0, PortCharacteristic.IN,
					EdgeH.makeCenter(), EdgeV.fromTop(1), EdgeH.makeCenter(), EdgeV.fromTop(1)),
				PrimitivePort.newInstance(this, transistorNode, new ArcProto[] {wire_arc}, "s", 180,90, 1, PortCharacteristic.BIDIR,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
				PrimitivePort.newInstance(this, transistorNode, new ArcProto[] {wire_arc}, "d", 0,90, 2, PortCharacteristic.BIDIR,
					EdgeH.makeRightEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeBottomEdge())
			});
		transistorNode.setFunction(PrimitiveNode.Function.TRANS);

		/** resistor */
        Technology.NodeLayer resistorLayer = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(LEFTBYP66, EdgeV.makeCenter()),
					new Technology.TechPoint(LEFTBYP6, EdgeV.makeCenter()),
					new Technology.TechPoint(LEFTBYP5, EdgeV.makeTopEdge()),
					new Technology.TechPoint(LEFTBYP3, EdgeV.makeBottomEdge()),
					new Technology.TechPoint(LEFTBYP1, EdgeV.makeTopEdge()),
					new Technology.TechPoint(RIGHTBYP1, EdgeV.makeBottomEdge()),
					new Technology.TechPoint(RIGHTBYP3, EdgeV.makeTopEdge()),
					new Technology.TechPoint(RIGHTBYP5, EdgeV.makeBottomEdge()),
					new Technology.TechPoint(RIGHTBYP6, EdgeV.makeCenter()),
					new Technology.TechPoint(RIGHTBYP66, EdgeV.makeCenter())});
        /* bold resistor */
        Technology.NodeLayer resistorLayerBold = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENEDT3, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(LEFTBYP66, EdgeV.makeCenter()),
					new Technology.TechPoint(LEFTBYP6, EdgeV.makeCenter()),
					new Technology.TechPoint(LEFTBYP5, EdgeV.makeTopEdge()),
					new Technology.TechPoint(LEFTBYP3, EdgeV.makeBottomEdge()),
					new Technology.TechPoint(LEFTBYP1, EdgeV.makeTopEdge()),
					new Technology.TechPoint(RIGHTBYP1, EdgeV.makeBottomEdge()),
					new Technology.TechPoint(RIGHTBYP3, EdgeV.makeTopEdge()),
					new Technology.TechPoint(RIGHTBYP5, EdgeV.makeBottomEdge()),
					new Technology.TechPoint(RIGHTBYP6, EdgeV.makeCenter()),
					new Technology.TechPoint(RIGHTBYP66, EdgeV.makeCenter())});
        Technology.NodeLayer resistorLayerWell = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENEDT2, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP6, EdgeV.makeTopEdge()),
			new Technology.TechPoint(LEFTBYP6, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(RIGHTBYP6, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(RIGHTBYP6, EdgeV.makeTopEdge()),
			new Technology.TechPoint(LEFTBYP6, EdgeV.makeTopEdge())});
        Technology.NodeLayer resistorLayerActive = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENEDT3, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP6, EdgeV.makeTopEdge()),
			new Technology.TechPoint(LEFTBYP6, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(RIGHTBYP6, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(RIGHTBYP6, EdgeV.makeTopEdge()),
			new Technology.TechPoint(LEFTBYP6, EdgeV.makeTopEdge())});
        /* P letter */
        Technology.NodeLayer resistorLayerP = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(RIGHTBYP4, EdgeV.makeCenter()),
			new Technology.TechPoint(RIGHTBYP4, EdgeV.makeTopEdge()),
			new Technology.TechPoint(RIGHTBYP6, EdgeV.makeTopEdge()),
			new Technology.TechPoint(RIGHTBYP6, EdgeV.makeCenter())});
        resistorLayerP.setMessage("P");
        resistorLayerP.setDescriptor(tdBig);
        /* N letter */
        Technology.NodeLayer resistorLayerN = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(RIGHTBYP4, EdgeV.makeCenter()),
			new Technology.TechPoint(RIGHTBYP4, EdgeV.makeTopEdge()),
			new Technology.TechPoint(RIGHTBYP6, EdgeV.makeTopEdge()),
			new Technology.TechPoint(RIGHTBYP6, EdgeV.makeCenter())});
        resistorLayerN.setMessage("N");
        resistorLayerN.setDescriptor(tdBig);
        /* US-N string */
        Technology.NodeLayer resistorUSP = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(RIGHTBYP4, EdgeV.makeCenter()),
			new Technology.TechPoint(RIGHTBYP4, EdgeV.makeTopEdge()),
			new Technology.TechPoint(RIGHTBYP6, EdgeV.makeTopEdge()),
			new Technology.TechPoint(RIGHTBYP6, EdgeV.makeCenter())});
        resistorUSP.setMessage("US-P");
        resistorUSP.setDescriptor(tdBig);
         /* US-N string */
        Technology.NodeLayer resistorUSN = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(RIGHTBYP4, EdgeV.makeCenter()),
			new Technology.TechPoint(RIGHTBYP4, EdgeV.makeTopEdge()),
			new Technology.TechPoint(RIGHTBYP6, EdgeV.makeTopEdge()),
			new Technology.TechPoint(RIGHTBYP6, EdgeV.makeCenter())});
        resistorUSN.setMessage("US-N");
        resistorUSN.setDescriptor(tdBig);
        resistorLayersNorm = new Technology.NodeLayer [] {resistorLayer};
        resistorLayersHiResPoly2 = new Technology.NodeLayer [] {resistorLayerBold}; // bold icon
        resistorLayersNPoly = new Technology.NodeLayer [] {resistorLayer, resistorLayerN};
        resistorLayersPPoly = new Technology.NodeLayer [] {resistorLayer, resistorLayerP};
        resistorLayersNNSPoly = new Technology.NodeLayer [] {resistorLayer, resistorUSN};
        resistorLayersPNSPoly = new Technology.NodeLayer [] {resistorLayer, resistorUSP};
        resistorLayersNWell = new Technology.NodeLayer [] {resistorLayer, resistorLayerN, resistorLayerWell};
        resistorLayersPWell = new Technology.NodeLayer [] {resistorLayer, resistorLayerP, resistorLayerWell};
        resistorLayersNActive = new Technology.NodeLayer [] {resistorLayer, resistorLayerN, resistorLayerActive};
        resistorLayersPActive = new Technology.NodeLayer [] {resistorLayer, resistorLayerP, resistorLayerActive};
		resistorNode = PrimitiveNode.newInstance("Resistor", this, 6.0, 1.0, new SizeOffset(1, 1, 0, 0), resistorLayersNorm);
		resistorNode.addPrimitivePortsFixed(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, resistorNode, new ArcProto[] {wire_arc}, "a", 180,90, 0, PortCharacteristic.UNKNOWN,
					LEFTBYP66, EdgeV.makeCenter(), LEFTBYP66, EdgeV.makeCenter()),
				PrimitivePort.newInstance(this, resistorNode, new ArcProto[] {wire_arc}, "b", 0,90, 1, PortCharacteristic.UNKNOWN,
					RIGHTBYP66, EdgeV.makeCenter(), RIGHTBYP66, EdgeV.makeCenter())
			});
		resistorNode.setFunction(PrimitiveNode.Function.RESIST);

		/** capacitor */
		Technology.NodeLayer capacitorLayer = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), TOPBYP2),
			new Technology.TechPoint(EdgeH.makeRightEdge(), TOPBYP2),
			new Technology.TechPoint(EdgeH.makeLeftEdge(), BOTBYP2),
			new Technology.TechPoint(EdgeH.makeRightEdge(), BOTBYP2),
			new Technology.TechPoint(EdgeH.makeCenter(), TOPBYP2),
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeTopEdge()),
			new Technology.TechPoint(EdgeH.makeCenter(), BOTBYP2),
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeBottomEdge())});
		Technology.NodeLayer capacitorLayerEl = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(RIGHTBYP2, BOTBYP6),
			new Technology.TechPoint(RIGHTBYP6, BOTBYP6),
			new Technology.TechPoint(RIGHTBYP4, BOTBYP4),
			new Technology.TechPoint(RIGHTBYP4, BOTBYP8)});
        Technology.NodeLayer capacitorLayerPoly2 = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENEDT3, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), TOPBYP2),
			new Technology.TechPoint(EdgeH.makeRightEdge(), TOPBYP2)}); // thick top bar
        capacitorLayersNorm = new Technology.NodeLayer [] {capacitorLayer};
		capacitorLayersElectrolytic = new Technology.NodeLayer [] {capacitorLayer, capacitorLayerEl};
        capacitorLayersPoly2 = new Technology.NodeLayer [] {capacitorLayer, capacitorLayerPoly2};
        capacitorNode = PrimitiveNode.newInstance("Capacitor", this, 3.0, 4.0, null, capacitorLayersNorm);
		capacitorNode.addPrimitivePortsFixed(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, capacitorNode, new ArcProto[] {wire_arc, bus_arc}, "a", 90,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeTopEdge(), EdgeH.makeCenter(), EdgeV.makeTopEdge()),
				PrimitivePort.newInstance(this, capacitorNode, new ArcProto[] {wire_arc, bus_arc}, "b", 270,90, 1, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeBottomEdge(), EdgeH.makeCenter(), EdgeV.makeBottomEdge())
			});
		capacitorNode.setFunction(PrimitiveNode.Function.CAPAC);

		/** diode */
		Technology.NodeLayer diodeLayer1 = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), TOPBYP5),
			new Technology.TechPoint(EdgeH.makeRightEdge(), TOPBYP5),
			new Technology.TechPoint(EdgeH.makeCenter(), TOPBYP5),
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeTopEdge()),
			new Technology.TechPoint(EdgeH.makeCenter(), BOTBYP5),
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeBottomEdge())});
		Technology.NodeLayer diodeLayer2 = new Technology.NodeLayer(node_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), BOTBYP5),
			new Technology.TechPoint(EdgeH.makeRightEdge(), BOTBYP5),
			new Technology.TechPoint(EdgeH.makeCenter(), TOPBYP5)});
		Technology.NodeLayer diodeLayer3 = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), TOPBYP75),
			new Technology.TechPoint(EdgeH.makeLeftEdge(), TOPBYP5),
			new Technology.TechPoint(EdgeH.makeLeftEdge(), TOPBYP5),
			new Technology.TechPoint(EdgeH.makeRightEdge(), TOPBYP5),
			new Technology.TechPoint(EdgeH.makeRightEdge(), TOPBYP5),
			new Technology.TechPoint(EdgeH.makeRightEdge(), TOPBYP25),
			new Technology.TechPoint(EdgeH.makeCenter(), TOPBYP5),
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeTopEdge()),
			new Technology.TechPoint(EdgeH.makeCenter(), BOTBYP5),
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeBottomEdge())});
		diodeLayersNorm = new Technology.NodeLayer [] {diodeLayer1, diodeLayer2};
		diodeLayersZener = new Technology.NodeLayer [] {diodeLayer3, diodeLayer2};
		diodeNode = PrimitiveNode.newInstance("Diode", this, 2.0, 4.0, null,diodeLayersNorm);
		diodeNode.addPrimitivePortsFixed(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, diodeNode, new ArcProto[] {wire_arc}, "a", 90,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeTopEdge(), EdgeH.makeCenter(), EdgeV.makeTopEdge()),
				PrimitivePort.newInstance(this, diodeNode, new ArcProto[] {wire_arc}, "b", 270,90, 1, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeBottomEdge(), EdgeH.makeCenter(), EdgeV.makeBottomEdge())
			});
		diodeNode.setFunction(PrimitiveNode.Function.DIODE);

		/** inductor */
		inductorNode = PrimitiveNode.newInstance("Inductor", this, 2.0, 4.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeTopEdge()),
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeBottomEdge())}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(LEFTBYP5, TOPBYP33),
					new Technology.TechPoint(EdgeH.makeCenter(), TOPBYP33)}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(LEFTBYP5, EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter())}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(LEFTBYP5, BOTBYP33),
					new Technology.TechPoint(EdgeH.makeCenter(), BOTBYP33)})
			});
		inductorNode.addPrimitivePortsFixed(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, inductorNode, new ArcProto[] {wire_arc}, "a", 90,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeTopEdge(), EdgeH.makeCenter(), EdgeV.makeTopEdge()),
				PrimitivePort.newInstance(this, inductorNode, new ArcProto[] {wire_arc}, "b", 270,90, 1, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeBottomEdge(), EdgeH.makeCenter(), EdgeV.makeBottomEdge())
			});
		inductorNode.setFunction(PrimitiveNode.Function.INDUCT);

		/** meter */
		Technology.NodeLayer meterLetterV = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, Technology.TechPoint.makeFullBox());
		meterLetterV.setMessage("V");
		meterLetterV.setDescriptor(tdBig);
		meterNode = PrimitiveNode.newInstance("Meter", this, 6.0, 6.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())}),
				meterLetterV
			});
		meterNode.addPrimitivePortsFixed(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, meterNode, new ArcProto[] {wire_arc}, "a", 90,0, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeTopEdge(), EdgeH.makeCenter(), EdgeV.makeTopEdge()),
				PrimitivePort.newInstance(this, meterNode, new ArcProto[] {wire_arc}, "b", 270,0, 1, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeBottomEdge(), EdgeH.makeCenter(), EdgeV.makeBottomEdge())
			});
		meterNode.setFunction(PrimitiveNode.Function.METER);
		meterNode.setSquare();

		/** well contact */
		wellNode = PrimitiveNode.newInstance("Well", this, 4.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeBottomEdge()),
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeTopEdge()),
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeBottomEdge())})
			});
		wellNode.addPrimitivePortsFixed(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, wellNode, new ArcProto[] {wire_arc}, "well", 90,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeTopEdge(), EdgeH.makeCenter(), EdgeV.makeTopEdge())
			});
		wellNode.setFunction(PrimitiveNode.Function.WELL);

		/** substrate contact */
		substrateNode = PrimitiveNode.newInstance("Substrate", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeTopEdge()),
					new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeBottomEdge()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeBottomEdge())})
			});
		substrateNode.addPrimitivePortsFixed(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, substrateNode, new ArcProto[] {wire_arc}, "substrate", 90,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeTopEdge(), EdgeH.makeCenter(), EdgeV.makeTopEdge())
			});
		substrateNode.setFunction(PrimitiveNode.Function.SUBSTRATE);

		/** two-port */
		Technology.NodeLayer twoLayerBox = new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP8, EdgeV.makeTopEdge()),
			new Technology.TechPoint(RIGHTBYP8, EdgeV.makeBottomEdge())});
		Technology.NodeLayer twoLayerNormWire = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), TOPBYP66),
			new Technology.TechPoint(LEFTBYP6, TOPBYP66),
			new Technology.TechPoint(EdgeH.makeLeftEdge(), BOTBYP66),
			new Technology.TechPoint(LEFTBYP6, BOTBYP66),
			new Technology.TechPoint(EdgeH.makeRightEdge(), TOPBYP66),
			new Technology.TechPoint(RIGHTBYP6, TOPBYP66),
			new Technology.TechPoint(RIGHTBYP6, TOPBYP66),
			new Technology.TechPoint(RIGHTBYP6, TOPBYP3),
			new Technology.TechPoint(EdgeH.makeRightEdge(), BOTBYP66),
			new Technology.TechPoint(RIGHTBYP6, BOTBYP66),
			new Technology.TechPoint(RIGHTBYP6, BOTBYP66),
			new Technology.TechPoint(RIGHTBYP6, BOTBYP3)});
		Technology.NodeLayer twoLayerVSC = new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(RIGHTBYP6, EdgeV.makeCenter()),
			new Technology.TechPoint(RIGHTBYP6, TOPBYP3)});
		Technology.NodeLayer twoLayerURPl = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(RIGHTBYP35, TOPBYP66),
			new Technology.TechPoint(RIGHTBYP45, TOPBYP66),
			new Technology.TechPoint(RIGHTBYP4, TOPBYP5833),
			new Technology.TechPoint(RIGHTBYP4, TOPBYP75)});
		Technology.NodeLayer twoLayerULPl = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP35, TOPBYP66),
			new Technology.TechPoint(LEFTBYP45, TOPBYP66),
			new Technology.TechPoint(LEFTBYP4, TOPBYP5833),
			new Technology.TechPoint(LEFTBYP4, TOPBYP75)});
		Technology.NodeLayer twoLayerCSArr = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(RIGHTBYP3833, TOPBYP33),
			new Technology.TechPoint(RIGHTBYP3833, BOTBYP33),
			new Technology.TechPoint(RIGHTBYP3833, BOTBYP33),
			new Technology.TechPoint(RIGHTBYP33, BOTBYP166),
			new Technology.TechPoint(RIGHTBYP3833, BOTBYP33),
			new Technology.TechPoint(RIGHTBYP433, BOTBYP166)});
		Technology.NodeLayer twoLayerGWire = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), TOPBYP66),
			new Technology.TechPoint(LEFTBYP8, TOPBYP66),
			new Technology.TechPoint(EdgeH.makeLeftEdge(), BOTBYP66),
			new Technology.TechPoint(LEFTBYP8, BOTBYP66),
			new Technology.TechPoint(EdgeH.makeRightEdge(), TOPBYP66),
			new Technology.TechPoint(RIGHTBYP8, TOPBYP66),
			new Technology.TechPoint(EdgeH.makeRightEdge(), BOTBYP66),
			new Technology.TechPoint(RIGHTBYP8, BOTBYP66)});
		Technology.NodeLayer twoLayerCSWire = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(RIGHTBYP6, TOPBYP3),
			new Technology.TechPoint(RIGHTBYP45, EdgeV.makeCenter()),
			new Technology.TechPoint(RIGHTBYP45, EdgeV.makeCenter()),
			new Technology.TechPoint(RIGHTBYP6, BOTBYP3),
			new Technology.TechPoint(RIGHTBYP6, BOTBYP3),
			new Technology.TechPoint(RIGHTBYP75, EdgeV.makeCenter()),
			new Technology.TechPoint(RIGHTBYP75, EdgeV.makeCenter()),
			new Technology.TechPoint(RIGHTBYP6, TOPBYP3)});
		Technology.NodeLayer twoLayerCCWire = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENEDT1, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP6, TOPBYP66),
			new Technology.TechPoint(LEFTBYP6, BOTBYP66)});
		Technology.NodeLayer twoLayerTrBox = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP8, EdgeV.makeTopEdge()),
			new Technology.TechPoint(RIGHTBYP8, EdgeV.makeTopEdge()),
			new Technology.TechPoint(LEFTBYP8, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(RIGHTBYP8, EdgeV.makeBottomEdge())});
		Technology.NodeLayer twoLayerTr1 = new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
			new Technology.TechPoint(LEFTBYP8, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(LEFTBYP8, EdgeV.makeTopEdge())});
		Technology.NodeLayer twoLayerTr2 = new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBY1P6, EdgeV.makeCenter()),
			new Technology.TechPoint(LEFTBYP8, EdgeV.makeTopEdge()),
			new Technology.TechPoint(LEFTBYP8, EdgeV.makeBottomEdge())});
		Technology.NodeLayer twoLayerTr3 = new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
			new Technology.TechPoint(RIGHTBYP8, EdgeV.makeTopEdge()),
			new Technology.TechPoint(RIGHTBYP8, EdgeV.makeBottomEdge())});
		Technology.NodeLayer twoLayerTrWire = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), TOPBYP66),
			new Technology.TechPoint(LEFTBYP8, TOPBYP66),
			new Technology.TechPoint(EdgeH.makeLeftEdge(), BOTBYP66),
			new Technology.TechPoint(LEFTBYP8, BOTBYP66),
			new Technology.TechPoint(EdgeH.makeRightEdge(), TOPBYP66),
			new Technology.TechPoint(RIGHTBYP9, TOPBYP66),
			new Technology.TechPoint(EdgeH.makeRightEdge(), BOTBYP66),
			new Technology.TechPoint(RIGHTBYP9, BOTBYP66)});
		Technology.NodeLayer twoLayerURRPl = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(RIGHTBYP5166, TOPBYP66),
			new Technology.TechPoint(RIGHTBYP6166, TOPBYP66),
			new Technology.TechPoint(RIGHTBYP566, TOPBYP5833),
			new Technology.TechPoint(RIGHTBYP566, TOPBYP75)});
		twoLayersDefault = new Technology.NodeLayer [] {twoLayerBox, twoLayerGWire, twoLayerULPl, twoLayerURPl};
		twoLayersVCVS = new Technology.NodeLayer [] {twoLayerBox, twoLayerNormWire, twoLayerVSC, twoLayerURPl, twoLayerULPl};
		twoLayersVCCS = new Technology.NodeLayer [] {twoLayerBox, twoLayerNormWire, twoLayerCSWire, twoLayerCSArr, twoLayerULPl};
		twoLayersCCVS = new Technology.NodeLayer [] {twoLayerBox, twoLayerCCWire, twoLayerNormWire, twoLayerVSC, twoLayerURPl, twoLayerULPl};
		twoLayersCCCS = new Technology.NodeLayer [] {twoLayerBox, twoLayerCCWire, twoLayerNormWire, twoLayerCSWire, twoLayerCSArr, twoLayerULPl};
		twoLayersTran = new Technology.NodeLayer [] {twoLayerTrBox, twoLayerTr1, twoLayerTr2, twoLayerTr3, twoLayerTrWire, twoLayerULPl, twoLayerURRPl};
		twoportNode = PrimitiveNode.newInstance("Two-Port", this, 10.0, 6.0, null, twoLayersDefault);
		twoportNode.addPrimitivePortsFixed(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, twoportNode, new ArcProto[] {wire_arc}, "a", 180,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), TOPBYP66, EdgeH.makeLeftEdge(), TOPBYP66),
				PrimitivePort.newInstance(this, twoportNode, new ArcProto[] {wire_arc}, "b", 180,90, 1, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), BOTBYP66, EdgeH.makeLeftEdge(), BOTBYP66),
				PrimitivePort.newInstance(this, twoportNode, new ArcProto[] {wire_arc}, "x", 0,90, 2, PortCharacteristic.UNKNOWN,
					EdgeH.makeRightEdge(), TOPBYP66, EdgeH.makeRightEdge(), TOPBYP66),
				PrimitivePort.newInstance(this, twoportNode, new ArcProto[] {wire_arc}, "y", 0,90, 3, PortCharacteristic.UNKNOWN,
					EdgeH.makeRightEdge(), BOTBYP66, EdgeH.makeRightEdge(), BOTBYP66)
			});
		twoportNode.setFunction(PrimitiveNode.Function.TLINE);

		/** 4-port transistor */
		Technology.NodeLayer tranLayerBIP4 = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP5, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeCenter(), BOTBYP25)});
		Technology.NodeLayer tranLayerPMES4 = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP5, BOTBYP25),
			new Technology.TechPoint(LEFTBYP5, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(LEFTBYP5, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(LEFTBYP35, BOTBYP75),
			new Technology.TechPoint(LEFTBYP5, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(LEFTBYP66, BOTBYP75)});
		Technology.NodeLayer tranLayerNMES4 = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP5, BOTBYP25),
			new Technology.TechPoint(LEFTBYP5, EdgeV.makeBottomEdge()),
			new Technology.TechPoint(LEFTBYP5, BOTBYP25),
			new Technology.TechPoint(LEFTBYP35, BOTBYP5),
			new Technology.TechPoint(LEFTBYP5, BOTBYP25),
			new Technology.TechPoint(LEFTBYP66, BOTBYP5)});
		tran4LayersN = buildTransistorDescription(true, false, false, false, 0, 0, true, false);
		tran4LayersP = buildTransistorDescription(false, false, false, false, 0, 0, true, false);
		tran4LayersNd = buildTransistorDescription(true, true, false, false, 0, 0, true, false);
		tran4LayersPd = buildTransistorDescription(false, true, false, false, 0, 0, true, false);
		tran4LayersNnT = buildTransistorDescription(true, false, true, false, 0, 0, true, false);
		tran4LayersPnT = buildTransistorDescription(false, false, true, false, 0, 0, true, false);
		tran4LayersNfG = buildTransistorDescription(true, false, false, true, 0, 0, true, false);
		tran4LayersPfG = buildTransistorDescription(false, false, false, true, 0, 0, true, false);
		tran4LayersNCN = buildTransistorDescription(true, false, false, false, 0, 0, true, true);
		tran4LayersPCN = buildTransistorDescription(false, false, false, false, 0, 0, true, true);
		tran4LayersNvtL = buildTransistorDescription(true, false, false, false, -1, 0, true, false);
		tran4LayersPvtL = buildTransistorDescription(false, false, false, false, -1, 0, true, false);
		tran4LayersNvtH = buildTransistorDescription(true, false, false, false, 1, 0, true, false);
		tran4LayersPvtH = buildTransistorDescription(false, false, false, false, 1, 0, true, false);
		tran4LayersNht1 = buildTransistorDescription(true, false, false, false, 0, 1, true, false);
		tran4LayersPht1 = buildTransistorDescription(false, false, false, false, 0, 1, true, false);
		tran4LayersNht2 = buildTransistorDescription(true, false, false, false, 0, 2, true, false);
		tran4LayersPht2 = buildTransistorDescription(false, false, false, false, 0, 2, true, false);
		tran4LayersNht3 = buildTransistorDescription(true, false, false, false, 0, 3, true, false);
		tran4LayersPht3 = buildTransistorDescription(false, false, false, false, 0, 3, true, false);
		tran4LayersNnTht1 = buildTransistorDescription(true, false, true, false, 0, 1, true, false);
		tran4LayersPnTht1 = buildTransistorDescription(false, false, true, false, 0, 1, true, false);
		tran4LayersNnTht2 = buildTransistorDescription(true, false, true, false, 0, 2, true, false);
		tran4LayersPnTht2 = buildTransistorDescription(false, false, true, false, 0, 2, true, false);
		tran4LayersNnTht3 = buildTransistorDescription(true, false, true, false, 0, 3, true, false);
		tran4LayersPnTht3 = buildTransistorDescription(false, false, true, false, 0, 3, true, false);
		tran4LayersNPN = new Technology.NodeLayer [] {tranLayerBTran1, tranLayerTranTop, tranLayerNMOS, tranLayerBTran2, tranLayerBIP4};
		tran4LayersPNP = new Technology.NodeLayer [] {tranLayerBTran1, tranLayerTranTop, tranLayerNMOS, tranLayerBTran3, tranLayerBIP4};
		tran4LayersNJFET = new Technology.NodeLayer [] {tranLayerBTran4, tranLayerTranTop, tranLayerNMOS, tranLayerBTran5, tranLayerPMES4};
		tran4LayersPJFET = new Technology.NodeLayer [] {tranLayerBTran4, tranLayerTranTop, tranLayerNMOS, tranLayerBTran6, tranLayerNMES4};
		tran4LayersDMES = new Technology.NodeLayer [] {tranLayerBTran4, tranLayerTranTop, tranLayerNMOS, tranLayerNMES4};
		tran4LayersEMES = new Technology.NodeLayer [] {tranLayerBTran7, tranLayerNMOS, tranLayerNMES4};
		transistor4Node = PrimitiveNode.newInstance("4-Port-Transistor", this, 4.0, 4.0, new SizeOffset(0, 0, 0, 1),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeTopEdge()),
					new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeBottomEdge()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeBottomEdge())})
			});
		transistor4Node.addPrimitivePortsFixed(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, transistor4Node, new ArcProto[] {wire_arc}, "g", 0,180, 0, PortCharacteristic.IN,
					EdgeH.makeCenter(), EdgeV.fromTop(1), EdgeH.makeCenter(), EdgeV.fromTop(1)),
				PrimitivePort.newInstance(this, transistor4Node, new ArcProto[] {wire_arc}, "s", 180,90, 1, PortCharacteristic.BIDIR,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
				PrimitivePort.newInstance(this, transistor4Node, new ArcProto[] {wire_arc}, "d", 0,90, 2, PortCharacteristic.BIDIR,
					EdgeH.makeRightEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeBottomEdge()),
				PrimitivePort.newInstance(this, transistor4Node, new ArcProto[] {wire_arc}, "b", 270,90, 3, PortCharacteristic.BIDIR,
					LEFTBYP5, EdgeV.makeBottomEdge(), LEFTBYP5, EdgeV.makeBottomEdge())
			});
		transistor4Node.setFunction(PrimitiveNode.Function.TRANS4);

		/** global signal */
		globalNode = PrimitiveNode.newInstance("Global-Signal", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeTopEdge()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeBottomEdge())}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(LEFTBYP9, EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeCenter(), TOPBYP9),
					new Technology.TechPoint(RIGHTBYP9, EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeCenter(), BOTBYP9)})
			});
		globalNode.addPrimitivePortsFixed(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, globalNode, new ArcProto[] {wire_arc}, "global", 270,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeBottomEdge(), EdgeH.makeCenter(), EdgeV.makeBottomEdge())
			});
		globalNode.setFunction(PrimitiveNode.Function.CONNECT);

		/** global partition */
		Technology.NodeLayer letterGP = new Technology.NodeLayer(text_lay, 0, Poly.Type.TEXTCENT, Technology.NodeLayer.POINTS,
				Technology.TechPoint.makeCenterBox());
		letterGP.setMessage("GP");
		letterGP.setDescriptor(tdBig);
		globalPartitionNode = PrimitiveNode.newInstance("Global-Partition", this, 4.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
					new Technology.TechPoint(LEFTBYP5, EdgeV.makeTopEdge()),
					new Technology.TechPoint(RIGHTBYP5, EdgeV.makeTopEdge()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
					new Technology.TechPoint(RIGHTBYP5, EdgeV.makeBottomEdge()),
					new Technology.TechPoint(LEFTBYP5, EdgeV.makeBottomEdge())}),
				letterGP
			});
		globalPartitionNode.addPrimitivePortsFixed(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, globalPartitionNode, new ArcProto[] {wire_arc, bus_arc}, "top", 90,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeTopEdge(), EdgeH.makeCenter(), EdgeV.makeTopEdge()),
				PrimitivePort.newInstance(this, globalPartitionNode, new ArcProto[] {wire_arc, bus_arc}, "bottom", 270,90, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeBottomEdge(), EdgeH.makeCenter(), EdgeV.makeBottomEdge())
			});
		globalPartitionNode.setFunction(PrimitiveNode.Function.CONNECT);

        loadFactoryMenuPalette(Schematics.class.getResource("schematicMenu.xml"));

        //Foundry
        newFoundry(Foundry.Type.NONE, null);
	}

	private Technology.NodeLayer [] buildTransistorDescription(boolean nmos, boolean depletion,
		boolean nt, boolean floating, int threshold, int highVoltage, boolean fourPort, boolean carbonNanotube)
	{
		List<Technology.NodeLayer> layers = new ArrayList<Technology.NodeLayer>();

		// first add the base
		if (carbonNanotube)
		{
			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
				new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
				new Technology.TechPoint(LEFTBYP75, EdgeV.makeBottomEdge()),
				new Technology.TechPoint(LEFTBYP75, BOTBYP5),
				new Technology.TechPoint(LEFTBYP625, BOTBYP5)}));
			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
				new Technology.TechPoint(RIGHTBYP625, BOTBYP5),
				new Technology.TechPoint(RIGHTBYP75, BOTBYP5),
				new Technology.TechPoint(RIGHTBYP75, EdgeV.makeBottomEdge()),
				new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeBottomEdge())}));
			double r = 0.125 / Math.sqrt(3);
			EdgeV ringUp1 = new EdgeV(-0.25+r/2,0);
			EdgeV ringUp2 = new EdgeV(-0.25+r,0);
			EdgeV ringDown1 = new EdgeV(-0.25-r/2,0);
			EdgeV ringDown2 = new EdgeV(-0.25-r,0);
			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
				new Technology.TechPoint(RIGHTBYP625, ringUp1),
				new Technology.TechPoint(RIGHTBYP5, ringUp2),
				new Technology.TechPoint(RIGHTBYP375, ringUp1),
				new Technology.TechPoint(RIGHTBYP375, ringDown1),
				new Technology.TechPoint(RIGHTBYP5, ringDown2),
				new Technology.TechPoint(RIGHTBYP625, ringDown1),
				new Technology.TechPoint(RIGHTBYP625, ringUp1)
				}));
			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
				new Technology.TechPoint(RIGHTBYP375, ringUp1),
				new Technology.TechPoint(RIGHTBYP25, ringUp2),
				new Technology.TechPoint(RIGHTBYP125, ringUp1),
				new Technology.TechPoint(RIGHTBYP125, ringDown1),
				new Technology.TechPoint(RIGHTBYP25, ringDown2),
				new Technology.TechPoint(RIGHTBYP375, ringDown1)
				}));
			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
				new Technology.TechPoint(RIGHTBYP125, ringUp1),
				new Technology.TechPoint(EdgeH.makeCenter(), ringUp2),
				new Technology.TechPoint(LEFTBYP125, ringUp1),
				new Technology.TechPoint(LEFTBYP125, ringDown1),
				new Technology.TechPoint(EdgeH.makeCenter(), ringDown2),
				new Technology.TechPoint(RIGHTBYP125, ringDown1)
				}));
			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
				new Technology.TechPoint(LEFTBYP125, ringUp1),
				new Technology.TechPoint(LEFTBYP25, ringUp2),
				new Technology.TechPoint(LEFTBYP375, ringUp1),
				new Technology.TechPoint(LEFTBYP375, ringDown1),
				new Technology.TechPoint(LEFTBYP25, ringDown2),
				new Technology.TechPoint(LEFTBYP125, ringDown1)
				}));
			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
				new Technology.TechPoint(LEFTBYP375, ringUp1),
				new Technology.TechPoint(LEFTBYP5, ringUp2),
				new Technology.TechPoint(LEFTBYP625, ringUp1),
				new Technology.TechPoint(LEFTBYP625, ringDown1),
				new Technology.TechPoint(LEFTBYP5, ringDown2),
				new Technology.TechPoint(LEFTBYP375, ringDown1)
				}));

//			EdgeV bubbleTop = new EdgeV(-0.25+0.0625,0);
//			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
//				new Technology.TechPoint(EdgeH.makeCenter(), BOTBYP5),
//				new Technology.TechPoint(EdgeH.makeCenter(), bubbleTop)}));
//			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
//				new Technology.TechPoint(LEFTBYP25, BOTBYP5),
//				new Technology.TechPoint(LEFTBYP25, bubbleTop)}));
//			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
//				new Technology.TechPoint(LEFTBYP5, BOTBYP5),
//				new Technology.TechPoint(LEFTBYP5, bubbleTop)}));
//			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
//				new Technology.TechPoint(RIGHTBYP25, BOTBYP5),
//				new Technology.TechPoint(RIGHTBYP25, bubbleTop)}));
//			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
//				new Technology.TechPoint(RIGHTBYP5, BOTBYP5),
//				new Technology.TechPoint(RIGHTBYP5, bubbleTop)}));
		} else
		{
			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
				new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
				new Technology.TechPoint(LEFTBYP75, EdgeV.makeBottomEdge()),
				new Technology.TechPoint(LEFTBYP75, BOTBYP5),
				new Technology.TechPoint(RIGHTBYP75, BOTBYP5),
				new Technology.TechPoint(RIGHTBYP75, EdgeV.makeBottomEdge()),
				new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeBottomEdge())}));
		}
		double vertBase = -0.25;

		// if depletion, add a solid bar at the base
		if (depletion)
		{
			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
				new Technology.TechPoint(LEFTBYP75, BOTBYP75),
				new Technology.TechPoint(RIGHTBYP75, BOTBYP5)}));
		}

		// add extra horizontal line if "floating"
		if (floating)
		{
			EdgeV ntHeight = new EdgeV(vertBase+0.0625,0);
			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
				new Technology.TechPoint(LEFTBYP75, ntHeight),
				new Technology.TechPoint(RIGHTBYP75, ntHeight)}));
		}

		// adjust space if variable threshold
		if (threshold < 0)
		{
			// low threshold: move closer to base
			vertBase -= 0.07;
		} else if (threshold > 0)
		{
			// high threshold: move farther to base
			vertBase += 0.07;
		}

		// draw gate bar if not native
		if (!nt)
		{
			vertBase += 0.125;
			EdgeV gateLoc = new EdgeV(vertBase,0);
			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
				new Technology.TechPoint(LEFTBYP75, gateLoc),
				new Technology.TechPoint(RIGHTBYP75, gateLoc)}));
		}

		if (nmos)
		{
			// draw the stick to the gate
			EdgeV gateBot = new EdgeV(vertBase,0);
			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
				new Technology.TechPoint(EdgeH.makeCenter(), gateBot),
				new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.fromTop(1))}));
		} else
		{
			// draw the stick to the gate
			EdgeV bubbleBot = new EdgeV(vertBase,0);
			EdgeV bubbleCtr = new EdgeV(vertBase+0.125,0);
			EdgeV bubbleTop = new EdgeV(vertBase+0.25,0);
			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
				new Technology.TechPoint(EdgeH.makeCenter(), bubbleCtr),
				new Technology.TechPoint(EdgeH.makeCenter(), bubbleBot)}));
			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
				new Technology.TechPoint(EdgeH.makeCenter(), bubbleTop),
				new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.fromTop(1))}));
		}

		// add battery if high-voltage
		if (highVoltage > 0)
		{
			double batteryLoc = 0.45;
			EdgeH batteryHorPos = new EdgeH(batteryLoc,0);

			// height of battery from top of node
			double batteryCtr = 0.125;
			EdgeV batteryCtrEdge = new EdgeV(batteryCtr,0);
			EdgeV batteryCtrShortTop = new EdgeV(batteryCtr+0.05,0);
			EdgeV batteryCtrShortBot = new EdgeV(batteryCtr-0.05,0);
			EdgeV batteryCtrLongTop = new EdgeV(batteryCtr+0.1,0);
			EdgeV batteryCtrLongBot = new EdgeV(batteryCtr-0.1,0);

			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
				new Technology.TechPoint(EdgeH.makeRightEdge(), batteryCtrEdge),
				new Technology.TechPoint(batteryHorPos, batteryCtrEdge)}));

			for(int i=0; i<highVoltage; i++)
			{
				layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(batteryHorPos, batteryCtrLongBot),
					new Technology.TechPoint(batteryHorPos, batteryCtrLongTop)}));
				batteryLoc -= 0.05;
				batteryHorPos = new EdgeH(batteryLoc,0);

				layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(batteryHorPos, batteryCtrShortBot),
					new Technology.TechPoint(batteryHorPos, batteryCtrShortTop)}));
				batteryLoc -= 0.05;
				batteryHorPos = new EdgeH(batteryLoc,0);
			}
			batteryLoc += 0.05;
			EdgeH batteryHorEnd = new EdgeH(batteryLoc,0);
			layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
				new Technology.TechPoint(batteryHorPos, batteryCtrEdge),
				new Technology.TechPoint(batteryHorEnd, batteryCtrEdge)}));
		}

		// add base connection if requested
		if (fourPort)
		{
			if (nmos)
			{
				if (depletion || carbonNanotube)
				{
					layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
						new Technology.TechPoint(LEFTBYP5, BOTBYP75),
						new Technology.TechPoint(LEFTBYP5, EdgeV.makeBottomEdge()),
						new Technology.TechPoint(LEFTBYP5, BOTBYP75),
						new Technology.TechPoint(LEFTBYP4, BOTBYP875),
						new Technology.TechPoint(LEFTBYP5, BOTBYP75),
						new Technology.TechPoint(LEFTBYP6, BOTBYP875)}));
				} else
				{
					layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
						new Technology.TechPoint(LEFTBYP5, BOTBYP5),
						new Technology.TechPoint(LEFTBYP5, EdgeV.makeBottomEdge()),
						new Technology.TechPoint(LEFTBYP5, BOTBYP5),
						new Technology.TechPoint(LEFTBYP35, BOTBYP75),
						new Technology.TechPoint(LEFTBYP5, BOTBYP5),
						new Technology.TechPoint(LEFTBYP66, BOTBYP75)}));
				}
			} else
			{
				if (depletion || carbonNanotube)
				{
					layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
						new Technology.TechPoint(LEFTBYP5, BOTBYP75),
						new Technology.TechPoint(LEFTBYP5, EdgeV.makeBottomEdge()),
						new Technology.TechPoint(LEFTBYP5, EdgeV.makeBottomEdge()),
						new Technology.TechPoint(LEFTBYP4, BOTBYP875),
						new Technology.TechPoint(LEFTBYP5, EdgeV.makeBottomEdge()),
						new Technology.TechPoint(LEFTBYP6, BOTBYP875)}));
				} else
				{
					layers.add(new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
						new Technology.TechPoint(LEFTBYP5, BOTBYP5),
						new Technology.TechPoint(LEFTBYP5, EdgeV.makeBottomEdge()),
						new Technology.TechPoint(LEFTBYP5, EdgeV.makeBottomEdge()),
						new Technology.TechPoint(LEFTBYP35, BOTBYP75),
						new Technology.TechPoint(LEFTBYP5, EdgeV.makeBottomEdge()),
						new Technology.TechPoint(LEFTBYP66, BOTBYP75)}));
				}
			}
		}
		Technology.NodeLayer [] descr = new Technology.NodeLayer[layers.size()];
        EPoint fixupCorrector = EPoint.fromLambda(4, 4);
		for(int i=0; i<layers.size(); i++) {
            descr[i] = layers.get(i);
            descr[i].fixup(fixupCorrector);
        }
		return descr;
	}

	private static Technology.NodeLayer[] NULLNODELAYER = new Technology.NodeLayer [] {};

    /**
	 * Puts into shape builder s the polygons that describe node "n", given a set of
	 * NodeLayer objects to use.
	 * This method is overridden by specific Technologys.
     * @param b shape builder where to put polygons
	 * @param n the ImmutableNodeInst that is being described.
     * @param pn proto of the ImmutableNodeInst in this Technology
	 * @param primLayers an array of NodeLayer objects to convert to Poly objects.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 */
    @Override
    protected void genShapeOfNode(AbstractShapeBuilder b, ImmutableNodeInst n, PrimitiveNode pn, Technology.NodeLayer[] primLayers) {
        EditWindow0 wnd = null;
        VarContext varContext = null;
        CellBackup.Memoization m = b.getMemoization();
		boolean extraBlobs = false;
		if (pn == wirePinNode)
		{
			if (m.pinUseCount(n)) primLayers = NULLNODELAYER;
		} else if (pn == busPinNode)
		{
			// bus pins get bigger in "T" configurations, disappear when alone and exported
			int busCon = 0, nonBusCon = 0;
            for (ImmutableArcInst a: m.getConnections(null, n, null)) {
				if (a.protoId == bus_arc.getId()) busCon++; else
					nonBusCon++;
			}
			int implicitCon = 0;
			if (busCon == 0 && nonBusCon == 0) implicitCon = 1;

//			// if the next level up the hierarchy is visible, consider arcs connected there
//			if (context != null && m.hasExports(n))
//			{
//				Nodable no = context.getNodable();
//				if (no != null && no instanceof NodeInst)
//				{
//					NodeInst upni = (NodeInst)no;
//					if (upni.getProto() == ni.getParent() && wnd != null /*&& upni.getParent() == wnd.getCell()*/)
//					{
//						for(Iterator<Export> it = ni.getExports(); it.hasNext(); )
//						{
//							Export pp = it.next();
//							for(Iterator<Connection> pIt = upni.getConnections(); pIt.hasNext(); )
//							{
//								Connection con = pIt.next();
//								if (con.getPortInst().getPortProto() != pp) continue;
//								if (con.getArc().getProto() == bus_arc) busCon++; else
//									nonBusCon++;
//							}
//						}
//					}
//				}
//			}

			// bus pins don't show wire pin in center if not tapped
			double wireDiscSize = 0.125;
			if (nonBusCon == 0) wireDiscSize = 0;

			double busDiscSize;
			if (busCon+implicitCon > 2)
			{
				// larger pin because it is connected to 3 or more bus arcs
				busDiscSize = 0.5;
			} else
			{
				// smaller pin because it has 0, 1, or 2 connections
				busDiscSize = 0.25;
				if (busCon == 0)
				{
					if (nonBusCon+implicitCon > 2)
					{
						busDiscSize = 0;
					} else
					{
						if (m.hasExports(n))
							wireDiscSize = busDiscSize = 0;
					}
				}
			}
			int totalLayers = 0;
			if (busDiscSize > 0) totalLayers++;
			if (wireDiscSize > 0) totalLayers++;
			Technology.NodeLayer [] busPinLayers = new Technology.NodeLayer[totalLayers];
			totalLayers = 0;
			if (busDiscSize > 0)
			{
				busPinLayers[totalLayers++] = new Technology.NodeLayer(bus_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeCenter(), new EdgeV(busDiscSize, 0))});
			}
			if (wireDiscSize > 0)
			{
				busPinLayers[totalLayers++] = new Technology.NodeLayer(arc_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeCenter(), new EdgeV(wireDiscSize, 0))});
			}
            primLayers = busPinLayers;
		} else if (pn == andNode)
		{
			double lambda = n.size.getLambdaX() / 8;
			if (n.size.getLambdaY() < lambda * 6) lambda = n.size.getLambdaY() / 6;
			if (lambda != 0)
			{
                lambda += 1;
				Technology.NodeLayer [] andLayers = new Technology.NodeLayer[2];
				andLayers[0] = new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(0.5 * lambda), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromCenter(0.5 * lambda), EdgeV.fromCenter(3 * lambda)),
					new Technology.TechPoint(EdgeH.fromCenter(0.5 * lambda), EdgeV.fromCenter(-3 * lambda))});
				andLayers[1] = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS,
					new Technology.TechPoint [] {
						new Technology.TechPoint(EdgeH.fromCenter(0.5 * lambda), EdgeV.fromCenter(3 * lambda)),
						new Technology.TechPoint(EdgeH.fromCenter(-4 * lambda), EdgeV.fromCenter(3 * lambda)),
						new Technology.TechPoint(EdgeH.fromCenter(-4 * lambda), EdgeV.makeTopEdge()),
						new Technology.TechPoint(EdgeH.fromCenter(-4 * lambda), EdgeV.makeBottomEdge()),
						new Technology.TechPoint(EdgeH.fromCenter(-4 * lambda), EdgeV.fromCenter(-3 * lambda)),
						new Technology.TechPoint(EdgeH.fromCenter(0.5 * lambda), EdgeV.fromCenter(-3 * lambda))
					});
				primLayers = andLayers;
			}
		} else if (pn == orNode)
		{
			double lambda = n.size.getLambdaX() / 10;
			if (n.size.getLambdaY() < lambda * 6) lambda = n.size.getLambdaY() / 6;
			if (lambda != 0)
			{
                lambda += 1;
				Technology.NodeLayer [] orLayers = new Technology.NodeLayer[4];
				orLayers[0] = new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-9 * lambda), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromCenter(-4 * lambda), EdgeV.fromCenter(3 * lambda)),
					new Technology.TechPoint(EdgeH.fromCenter(-4 * lambda), EdgeV.fromCenter(-3 * lambda))});
				orLayers[1] = new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-0.75 * lambda), EdgeV.fromCenter(-3 * lambda)),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75 * lambda), EdgeV.fromCenter(3 * lambda)),
					new Technology.TechPoint(EdgeH.fromCenter(4.5 * lambda), EdgeV.makeCenter())});
				orLayers[2] = new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-0.75 * lambda), EdgeV.fromCenter(3 * lambda)),
					new Technology.TechPoint(EdgeH.fromCenter(4.5 * lambda), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75 * lambda), EdgeV.fromCenter(-3 * lambda))});
				orLayers[3] = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-4 * lambda), EdgeV.makeTopEdge()),
					new Technology.TechPoint(EdgeH.fromCenter(-4 * lambda), EdgeV.fromCenter(3 * lambda)),
					new Technology.TechPoint(EdgeH.fromCenter(-4 * lambda), EdgeV.fromCenter(3 * lambda)),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75 * lambda), EdgeV.fromCenter(3 * lambda)),
					new Technology.TechPoint(EdgeH.fromCenter(-4 * lambda), EdgeV.makeBottomEdge()),
					new Technology.TechPoint(EdgeH.fromCenter(-4 * lambda), EdgeV.fromCenter(-3 * lambda)),
					new Technology.TechPoint(EdgeH.fromCenter(-4 * lambda), EdgeV.fromCenter(-3 * lambda)),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75 * lambda), EdgeV.fromCenter(-3 * lambda))});
				primLayers = orLayers;
			}
		} else if (pn == xorNode)
		{
			double lambda = n.size.getLambdaX() / 10;
			if (n.size.getLambdaY() < lambda * 6) lambda = n.size.getLambdaY() / 6;
			if (lambda != 0)
			{
                lambda += 1;
				Technology.NodeLayer [] xorLayers = new Technology.NodeLayer[5];
				xorLayers[0] = new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-9 * lambda), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromCenter(-4 * lambda), EdgeV.fromCenter(3 * lambda)),
					new Technology.TechPoint(EdgeH.fromCenter(-4 * lambda), EdgeV.fromCenter(-3 * lambda))});
				xorLayers[1] = new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-0.75 * lambda), EdgeV.fromCenter(-3 * lambda)),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75 * lambda), EdgeV.fromCenter(3 * lambda)),
					new Technology.TechPoint(EdgeH.fromCenter(4.5 * lambda), EdgeV.makeCenter())});
				xorLayers[2] = new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-0.75 * lambda), EdgeV.fromCenter(3 * lambda)),
					new Technology.TechPoint(EdgeH.fromCenter(4.5 * lambda), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75 * lambda), EdgeV.fromCenter(-3 * lambda))});
				xorLayers[3] = new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-10 * lambda), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.fromCenter(-5 * lambda), EdgeV.fromCenter(3 * lambda)),
					new Technology.TechPoint(EdgeH.fromCenter(-5 * lambda), EdgeV.fromCenter(-3 * lambda))});
				xorLayers[4] = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-4 * lambda), EdgeV.makeTopEdge()),
					new Technology.TechPoint(EdgeH.fromCenter(-4 * lambda), EdgeV.fromCenter(3 * lambda)),
					new Technology.TechPoint(EdgeH.fromCenter(-4 * lambda), EdgeV.fromCenter(3 * lambda)),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75 * lambda), EdgeV.fromCenter(3 * lambda)),
					new Technology.TechPoint(EdgeH.fromCenter(-4 * lambda), EdgeV.makeBottomEdge()),
					new Technology.TechPoint(EdgeH.fromCenter(-4 * lambda), EdgeV.fromCenter(-3 * lambda)),
					new Technology.TechPoint(EdgeH.fromCenter(-4 * lambda), EdgeV.fromCenter(-3 * lambda)),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75 * lambda), EdgeV.fromCenter(-3 * lambda))});
				primLayers = xorLayers;
			}
		} else if (pn == flipflopNode)
		{
			int ffBits = n.techBits;
			switch (ffBits)
			{
				case FFTYPERS|FFCLOCKMS: primLayers = ffLayersRSMS;  break;
				case FFTYPERS|FFCLOCKP:  primLayers = ffLayersRSP;   break;
				case FFTYPERS|FFCLOCKN:  primLayers = ffLayersRSN;   break;

				case FFTYPEJK|FFCLOCKMS: primLayers = ffLayersJKMS;  break;
				case FFTYPEJK|FFCLOCKP:  primLayers = ffLayersJKP;   break;
				case FFTYPEJK|FFCLOCKN:  primLayers = ffLayersJKN;   break;

				case FFTYPED|FFCLOCKMS: primLayers = ffLayersDMS;    break;
				case FFTYPED|FFCLOCKP:  primLayers = ffLayersDP;     break;
				case FFTYPED|FFCLOCKN:  primLayers = ffLayersDN;     break;

				case FFTYPET|FFCLOCKMS: primLayers = ffLayersTMS;    break;
				case FFTYPET|FFCLOCKP:  primLayers = ffLayersTP;     break;
				case FFTYPET|FFCLOCKN:  primLayers = ffLayersTN;     break;
			}
		} else if (pn == transistorNode)
		{
			extraBlobs = true;
			int tranBits = n.techBits;
			switch (tranBits)
			{
				case TRANNMOS:      primLayers = tranLayersN;      break;
				case TRANPMOS:      primLayers = tranLayersP;      break;
				case TRANNMOSD:     primLayers = tranLayersNd;     break;
				case TRANPMOSD:     primLayers = tranLayersPd;     break;
				case TRANNMOSNT:    primLayers = tranLayersNnT;    break;
				case TRANPMOSNT:    primLayers = tranLayersPnT;    break;
				case TRANNMOSFG:    primLayers = tranLayersNfG;    break;
				case TRANPMOSFG:    primLayers = tranLayersPfG;    break;
				case TRANNMOSCN:    primLayers = tranLayersNCN;    break;
				case TRANPMOSCN:    primLayers = tranLayersPCN;    break;
				case TRANNMOSVTL:   primLayers = tranLayersNvtL;   break;
				case TRANPMOSVTL:   primLayers = tranLayersPvtL;   break;
				case TRANNMOSVTH:   primLayers = tranLayersNvtH;   break;
				case TRANPMOSVTH:   primLayers = tranLayersPvtH;   break;
				case TRANNMOSHV1:   primLayers = tranLayersNht1;   break;
				case TRANPMOSHV1:   primLayers = tranLayersPht1;   break;
				case TRANNMOSHV2:   primLayers = tranLayersNht2;   break;
				case TRANPMOSHV2:   primLayers = tranLayersPht2;   break;
				case TRANNMOSHV3:   primLayers = tranLayersNht3;   break;
				case TRANPMOSHV3:   primLayers = tranLayersPht3;   break;
				case TRANNMOSNTHV1: primLayers = tranLayersNnTht1; break;
				case TRANPMOSNTHV1: primLayers = tranLayersPnTht1; break;
				case TRANNMOSNTHV2: primLayers = tranLayersNnTht2; break;
				case TRANPMOSNTHV2: primLayers = tranLayersPnTht2; break;
				case TRANNMOSNTHV3: primLayers = tranLayersNnTht3; break;
				case TRANPMOSNTHV3: primLayers = tranLayersPnTht3; break;
				case TRANNPN:       primLayers = tranLayersNPN;    break;
				case TRANPNP:       primLayers = tranLayersPNP;    break;
				case TRANNJFET:     primLayers = tranLayersNJFET;  break;
				case TRANPJFET:     primLayers = tranLayersPJFET;  break;
				case TRANDMES:      primLayers = tranLayersDMES;   break;
				case TRANEMES:      primLayers = tranLayersEMES;   break;
			}
		} else if (pn == twoportNode)
		{
			extraBlobs = true;
			int tranBits = n.techBits;
			switch (tranBits)
			{
				case TWOPVCCS:  primLayers = twoLayersVCCS;   break;
				case TWOPCCVS:  primLayers = twoLayersCCVS;   break;
				case TWOPVCVS:  primLayers = twoLayersVCVS;   break;
				case TWOPCCCS:  primLayers = twoLayersCCCS;   break;
				case TWOPTLINE: primLayers = twoLayersTran;   break;
			}
		} else if (pn == diodeNode)
		{
			extraBlobs = true;
			int diodeBits = n.techBits;
			switch (diodeBits)
			{
				case DIODENORM:  primLayers = diodeLayersNorm;    break;
				case DIODEZENER: primLayers = diodeLayersZener;   break;
			}
		} else if (pn == capacitorNode)
		{
			extraBlobs = true;
			int capacitorBits = n.techBits;
			switch (capacitorBits)
			{
				case CAPACNORM: primLayers = capacitorLayersNorm;           break;
				case CAPACELEC: primLayers = capacitorLayersElectrolytic;   break;
                case CAPACPOLY2: primLayers = capacitorLayersPoly2;   break;
            }
        } else if (pn == resistorNode)
		{
			extraBlobs = true;
			int resistorBits = n.techBits;
			switch (resistorBits)
			{
				case RESISTNORM:  primLayers = resistorLayersNorm;    break;
				case RESISTNPOLY: primLayers = resistorLayersNPoly;   break;
				case RESISTPPOLY: primLayers = resistorLayersPPoly;   break;
				case RESISTNNSPOLY: primLayers = resistorLayersNNSPoly;   break;  // unsilicide n poly
				case RESISTPNSPOLY: primLayers = resistorLayersPNSPoly;   break;  // unsilicide p poly
				case RESISTNWELL: primLayers = resistorLayersNWell;   break;
				case RESISTPWELL: primLayers = resistorLayersPWell;   break;
				case RESISTNACTIVE: primLayers = resistorLayersNActive;   break;
				case RESISTPACTIVE: primLayers = resistorLayersPActive;   break;
				case RESISTHIRESPOLY2: primLayers = resistorLayersHiResPoly2;   break;
			}
		} else if (pn == switchNode)
		{
			int numLayers = 3;
			if (n.size.getLambdaY() >= 2) numLayers += ((int)n.size.getLambdaY()/2);
			Technology.NodeLayer [] switchLayers = new Technology.NodeLayer[numLayers];
			switchLayers[0] = primLayers[0];
			if ((numLayers%2) == 0) switchLayers[1] = primLayers[1]; else
				switchLayers[1] = primLayers[2];
			for(int i=2; i<numLayers; i++)
			{
				double yValue = 2 * (i-1) - 1;
				switchLayers[i] = new Technology.NodeLayer(node_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(yValue)),
					new Technology.TechPoint(EdgeH.fromLeft(1.25), EdgeV.fromBottom(yValue))});
			}
			primLayers = switchLayers;
		} else if (pn == transistor4Node)
		{
			extraBlobs = true;
			int tranBits = n.techBits;
			switch (tranBits)
			{
				case TRANNMOS:      primLayers = tran4LayersN;      break;
				case TRANPMOS:      primLayers = tran4LayersP;      break;
				case TRANNMOSD:     primLayers = tran4LayersNd;     break;
				case TRANPMOSD:     primLayers = tran4LayersPd;     break;
				case TRANNMOSNT:    primLayers = tran4LayersNnT;    break;
				case TRANPMOSNT:    primLayers = tran4LayersPnT;    break;
				case TRANNMOSFG:    primLayers = tran4LayersNfG;    break;
				case TRANPMOSFG:    primLayers = tran4LayersPfG;    break;
				case TRANNMOSCN:    primLayers = tran4LayersNCN;    break;
				case TRANPMOSCN:    primLayers = tran4LayersPCN;    break;
				case TRANNMOSVTL:   primLayers = tran4LayersNvtL;   break;
				case TRANPMOSVTL:   primLayers = tran4LayersPvtL;   break;
				case TRANNMOSVTH:   primLayers = tran4LayersNvtH;   break;
				case TRANPMOSVTH:   primLayers = tran4LayersPvtH;   break;
				case TRANNMOSHV1:   primLayers = tran4LayersNht1;   break;
				case TRANPMOSHV1:   primLayers = tran4LayersPht1;   break;
				case TRANNMOSHV2:   primLayers = tran4LayersNht2;   break;
				case TRANPMOSHV2:   primLayers = tran4LayersPht2;   break;
				case TRANNMOSHV3:   primLayers = tran4LayersNht3;   break;
				case TRANPMOSHV3:   primLayers = tran4LayersPht3;   break;
				case TRANNMOSNTHV1: primLayers = tran4LayersNnTht1; break;
				case TRANPMOSNTHV1: primLayers = tran4LayersPnTht1; break;
				case TRANNMOSNTHV2: primLayers = tran4LayersNnTht2; break;
				case TRANPMOSNTHV2: primLayers = tran4LayersPnTht2; break;
				case TRANNMOSNTHV3: primLayers = tran4LayersNnTht3; break;
				case TRANPMOSNTHV3: primLayers = tran4LayersPnTht3; break;
				case TRANNPN:       primLayers = tran4LayersNPN;    break;
				case TRANPNP:       primLayers = tran4LayersPNP;    break;
				case TRANNJFET:     primLayers = tran4LayersNJFET;  break;
				case TRANPJFET:     primLayers = tran4LayersPJFET;  break;
				case TRANDMES:      primLayers = tran4LayersDMES;   break;
				case TRANEMES:      primLayers = tran4LayersEMES;   break;
			}
		} else if (pn == offpageNode) {
            boolean input = false;
            boolean output = false;
            boolean bidirectional = false;
            for(ImmutableExport e : i2i(m.getExports(n.nodeId))) {
                if      (e.characteristic==PortCharacteristic.IN)    input = true;
                else if (e.characteristic==PortCharacteristic.OUT)   output = true;
                else if (e.characteristic==PortCharacteristic.BIDIR) bidirectional = true;
            }
            if      ( input && !output && !bidirectional) primLayers = offPageInputLayers;
            else if (!input &&  output && !bidirectional) primLayers = offPageOutputLayers;
            else if (!input && !output &&  bidirectional) primLayers = offPageBidirectionalLayers;
            else                                          primLayers = offPageLayers;
			extraBlobs = true;
		} else if (pn == powerNode || pn == groundNode || pn == sourceNode || pn == resistorNode ||
			pn == inductorNode || pn == meterNode || pn == wellNode || pn == substrateNode)
		{
			extraBlobs = true;
		}
        // apply correction
        ERectangle fullRectangle = pn.getFullRectangle();
        EPoint fixupCorrection = EPoint.fromGrid(fullRectangle.getGridWidth(), fullRectangle.getGridHeight());
        for (Technology.NodeLayer nodeLayer: primLayers)
            nodeLayer.fixup(fixupCorrection);

		// check for extra blobs (on nodes that can handle it)
		if (extraBlobs)
		{
			// make a list of extra blobs that need to be drawn
			List<PrimitivePort> extraBlobList = null;
            BitSet headEnds = new BitSet();
            List<ImmutableArcInst> connArcs = m.getConnections(headEnds, n, null);
            PortProtoId prevPortId = null;
            int arcsCount = 0;
            for (int i = 0; i < connArcs.size(); i++) {
                ImmutableArcInst a = connArcs.get(i);
                PortProtoId portId = headEnds.get(i) ? a.headPortId : a.tailPortId;
                assert portId.parentId == n.protoId;
                if (portId == prevPortId) {
                    arcsCount++;
                    if (arcsCount == 2) {
    					if (extraBlobList == null) extraBlobList = new ArrayList<PrimitivePort>();
        				extraBlobList.add(pn.getPort(portId));
                    }
                } else {
                    prevPortId = portId;
                    arcsCount = 1;
                }
            }
			if (extraBlobList != null)
			{
				// must add extra blobs to this node
				double blobSize = wirePinNode.getFactoryDefaultLambdaBaseWidth() / 2;
//				double blobSize = wirePinNode.getDefWidth() / 2;
				Technology.NodeLayer [] blobLayers = new Technology.NodeLayer[primLayers.length + extraBlobList.size()];
				int fill = 0;
				for(int i=0; i<primLayers.length; i++)
					blobLayers[fill++] = primLayers[i];
				for(PrimitivePort pp : extraBlobList)
				{
					EdgeH xEdge = new EdgeH(pp.getLeft().getMultiplier(), pp.getLeft().getAdder() + blobSize);
					blobLayers[fill++] = new Technology.NodeLayer(arc_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
						new Technology.TechPoint(pp.getLeft(), pp.getTop()),
						new Technology.TechPoint(xEdge, pp.getTop())});
				}
				primLayers = blobLayers;
			}
		}
		b.genShapeOfNode(n, pn, primLayers, null);
	}

	/**
	 * Puts into shape builder s the polygons that describe node "n", given a set of
	 * NodeLayer objects to use.
	 * This method is overridden by specific Technologys.
     * @param b shape builder where to put polygons
	 * @param n the ImmutableNodeInst that is being described.
     * @param pn proto of the ImmutableNodeInst in this Technology
	 * @param selectPt if not null, it requests a new location on the port,
	 * away from existing arcs, and close to this point.
	 * This is useful for "area" ports such as the left side of AND and OR gates.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 */
    @Override
    protected void genShapeOfPort(AbstractShapeBuilder b, ImmutableNodeInst n, PrimitiveNode pn, PrimitivePort pp, Point2D selectPt) {
		// determine the grid size
		double lambda = 0;
		if (pn == andNode)
		{
			lambda = n.size.getLambdaX() / 8;
			if (n.size.getLambdaY() < lambda * 6) lambda = n.size.getLambdaY() / 6;
//			lambda = width / 8;
//			if (height < lambda * 6) lambda = height / 6;
		} else if (pn == orNode || pn == xorNode)
		{
			lambda = n.size.getLambdaX() / 10;
			if (n.size.getLambdaY() < lambda * 6) lambda = n.size.getLambdaY() / 6;
//			lambda = width / 10;
//			if (height < lambda * 6) lambda = height / 6;
		}
        lambda += 1;

		// only care if special selection is requested
		if (selectPt != null)
		{
			// special selection only works for AND, OR, XOR, MUX, SWITCH
			if (pn == andNode || pn == orNode || pn == xorNode || pn == muxNode || pn == switchNode)
			{
				// special selection only works for the input port (the first port, 0)
				if (pp.getId().chronIndex == 0)
				{
					// initialize
//					PortInst pi = ni.findPortInstFromProto(pp);
					double wantX = selectPt.getX();
					double wantY = selectPt.getY();
					double bestDist = Double.MAX_VALUE;
					double bestX = 0, bestY = 0;

					// determine total number of arcs already on this port
					int total = 0;
                    BitSet headEnds = new BitSet();
                    List<ImmutableArcInst> connArcs = b.getMemoization().getConnections(headEnds, n, pp.getId());
//					for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
//					{
//						Connection con = it.next();
//						if (con.getPortInst() == pi) total++;
//					}
                    total = connArcs.size();

					// cycle through the arc positions
					total = Math.max(total+2, 3);
					for(int i=0; i<total; i++)
					{
						// compute the position along the left edge
						double yPosition = (i+1)/2 * 2;
						if ((i&1) != 0) yPosition = -yPosition;

						// compute indentation
						double xPosition = -4;
						if (pn == switchNode)
						{
							xPosition = -2;
						} else if (pn == muxNode)
						{
							xPosition = -(8.0 + n.size.getLambdaX()) * 4 / 10;
						} else if (pn == orNode || pn == xorNode)
						{
							switch (i)
							{
								case 0: xPosition += 0.75;   break;
								case 1:
								case 2: xPosition += 0.5;    break;
							}
						}

						// fill the polygon with that point
                        Point2D.Double pt = new Point2D.Double(xPosition * lambda, yPosition * lambda);
                        Point2D.Double pt1 = new Point2D.Double();
                        n.orient.pureRotate().transform(pt, pt1);
                        pt1.setLocation(n.anchor.getLambdaX() + pt1.getX(), n.anchor.getLambdaY() + pt1.getY());
//						double x = n.anchor.getLambdaX() + xPosition * lambda;
//						double y = n.anchor.getLambdaY() + yPosition * lambda;

						// check for duplication
						boolean found = false;
                        for (int j = 0; j < connArcs.size(); j++) {
                            ImmutableArcInst a = connArcs.get(j);
                            boolean isHead = headEnds.get(j);
                            EPoint connLocation = isHead ? a.headLocation : a.tailLocation;
                            if (connLocation.equals(pt1)) {
                                found = true;
                                break;
                            }
                        }
//						for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
//						{
//							Connection con = it.next();
//							if (con.getLocation().getX() == x && con.getLocation().getY() == y)
//							{
//								found = true;
//								break;
//							}
//						}

						// if there is no duplication, this is a possible position
						if (!found)
						{
							double dist = Math.abs(wantX - pt1.getX()) + Math.abs(wantY - pt1.getY());
							if (dist < bestDist)
							{
								bestDist = dist;   bestX = pt.getX();   bestY = pt.getY();   //bestIndex = i;
							}
						}
					}
					if (bestDist == Double.MAX_VALUE) System.out.println("Warning: cannot find gate port");

					// set the closest port
//					Point2D [] points = new Point2D[1];
//					points[0] = new Point2D.Double(bestX, bestY);
                    b.pushPoint(bestX*DBMath.GRID, bestY*DBMath.GRID);
                    b.pushPoly(Poly.Type.FILLED, null, null, null);
                    return;
//					Poly poly = new Poly(points);
//					poly.setStyle(Poly.Type.FILLED);
//					return poly;
				}
			}
		}
		if (lambda != 1)
		{
			// standard port computation
            double sizeX = n.size.getGridX();
            double sizeY = n.size.getGridY();
			double portLowX = pp.getLeft().getMultiplier() * sizeX + pp.getLeft().getGridAdder() * lambda;
			double portHighX = pp.getRight().getMultiplier() * sizeX + pp.getRight().getGridAdder() * lambda;
			double portLowY = pp.getBottom().getMultiplier() * sizeY + pp.getBottom().getGridAdder() * lambda;
			double portHighY = pp.getTop().getMultiplier() * sizeY + pp.getTop().getGridAdder() * lambda;
            b.pushPoint(portLowX, portLowY);
            b.pushPoint(portHighX, portLowY);
            b.pushPoint(portHighX, portHighY);
            b.pushPoint(portLowX, portHighY);
//			double portX = (portLowX + portHighX) / 2;
//			double portY = (portLowY + portHighY) / 2;
            b.pushPoly(Poly.Type.FILLED, null, null, null);
//			Poly portPoly = new Poly(portX, portY, portHighX-portLowX, portHighY-portLowY);
//			portPoly.setStyle(Poly.Type.FILLED);
//			portPoly.setTextDescriptor(TextDescriptor.getExportTextDescriptor()/*pp.getTextDescriptor()*/);
//			return portPoly;
            return;
		}

		// special selection did not apply: do normal port computation
		super.genShapeOfPort(b, n, pn, pp, selectPt);
    }

	/**
	 * Method to get the base (highlight) ERectangle associated with a NodeInst
     * in this PrimitiveNode.
     * Base ERectangle is a highlight rectangle of standard-size NodeInst of
     * this PrimtiveNode
	 * By having this be a method of Technology, it can be overridden by
	 * individual Technologies that need to make special considerations.
	 * @param ni the NodeInst to query.
	 * @return the base ERectangle of this PrimitiveNode.
	 */
    @Override
    public ERectangle getNodeInstBaseRectangle(NodeInst ni) {
		NodeProto np = ni.getProto();
		if (np == andNode)
		{
			double width = ni.getD().size.getLambdaX() + 8;
			double height = ni.getD().size.getLambdaY() + 6;
            double unitSize = Math.min(width/8, height/6);
			return ERectangle.fromLambda(-0.5*width, -0.5*height, width - unitSize/2, height);
		} else if (np == orNode)
		{
			double width = ni.getD().size.getLambdaX() + 10;
			double height = ni.getD().size.getLambdaY() + 6;
            double unitSize = Math.min(width/10, height/6);
			return ERectangle.fromLambda(-0.5*width, -0.5*height, width - unitSize/2, height);
		} else if (np == xorNode)
		{
			double width = ni.getD().size.getLambdaX() + 10;
			double height = ni.getD().size.getLambdaY() + 6;
            double unitSize = Math.min(width/10, height/6);
			return ERectangle.fromLambda(-0.5*width, -0.5*height, width - unitSize/2, height);
		}
		return super.getNodeInstBaseRectangle(ni);
    }

	/**
	 * Method to convert old primitive port names to their proper PortProtos.
	 * This method overrides the general Technology version and attempts Schematic-specific tests first.
	 * @param portName the unknown port name, read from an old Library.
	 * @param np the PrimitiveNode on which this port resides.
	 * @return the proper PrimitivePort to use for this name.
	 */
	public PrimitivePort convertOldPortName(String portName, PrimitiveNode np)
	{
		if (np == sourceNode || np == meterNode)
		{
			if (portName.equals("top")) return getIndexedPort(0, np);
			if (portName.equals("bottom")) return getIndexedPort(1, np);
		}
		if (np == twoportNode)
		{
			if (portName.equals("upperleft")) return getIndexedPort(0, np);
			if (portName.equals("lowerleft")) return getIndexedPort(1, np);
			if (portName.equals("upperright")) return getIndexedPort(2, np);
			if (portName.equals("lowerright")) return getIndexedPort(3, np);
		}
        if (np == powerNode)
        {
            if (portName.equals("pwr")) return getIndexedPort(0, np);
        }

		return super.convertOldPortName(portName,np);
	}

	private PrimitivePort getIndexedPort(int index, PrimitiveNode np)
	{
		for(Iterator<PrimitivePort> it = np.getPrimitivePorts(); it.hasNext(); )
		{
			PrimitivePort pp = it.next();
			if (index == 0) return pp;
			index--;
		}
		return null;
	}

	/**
	 * Method to return the pure "PrimitiveNode Function" a primitive NodeInst in this Technology.
	 * The Schematics technology allows primitives to have parameterized functions.
	 * @param pn PrimitiveNode to check.
     * @param techBits tech bits
	 * @return the PrimitiveNode.Function that describes the PrinitiveNode with specific tech bits.
	 */
	public PrimitiveNode.Function getPrimitiveFunction(PrimitiveNode pn, int techBits)
	{
        if (pn == resistorNode)
		{
			if (techBits == RESISTNPOLY) return PrimitiveNode.Function.RESNPOLY;
			if (techBits == RESISTPPOLY) return PrimitiveNode.Function.RESPPOLY;
			if (techBits == RESISTNNSPOLY) return PrimitiveNode.Function.RESNNSPOLY;
			if (techBits == RESISTPNSPOLY) return PrimitiveNode.Function.RESPNSPOLY;
			if (techBits == RESISTNWELL) return PrimitiveNode.Function.RESNWELL;
			if (techBits == RESISTPWELL) return PrimitiveNode.Function.RESPWELL;
			if (techBits == RESISTNACTIVE) return PrimitiveNode.Function.RESNACTIVE;
			if (techBits == RESISTPACTIVE) return PrimitiveNode.Function.RESPACTIVE;
			if (techBits == RESISTHIRESPOLY2) return PrimitiveNode.Function.RESHIRESPOLY2;
			return PrimitiveNode.Function.RESIST;
		}
		if (pn == capacitorNode)    //
		{
			if (techBits == CAPACELEC) return PrimitiveNode.Function.ECAPAC;
			if (techBits == CAPACPOLY2) return PrimitiveNode.Function.POLY2CAPAC;
			return PrimitiveNode.Function.CAPAC;
		}
		if (pn == diodeNode)
		{
			if (techBits == DIODEZENER) return PrimitiveNode.Function.DIODEZ;
			return PrimitiveNode.Function.DIODE;
		}
		if (pn == transistorNode)
		{
			switch (techBits)
			{
				case TRANNMOS:      return PrimitiveNode.Function.TRANMOS;
				case TRANPMOS:      return PrimitiveNode.Function.TRAPMOS;
				case TRANNMOSD:     return PrimitiveNode.Function.TRADMOS;
				case TRANPMOSD:     return PrimitiveNode.Function.TRAPMOSD;
				case TRANNMOSNT:    return PrimitiveNode.Function.TRANMOSNT;
				case TRANPMOSNT:    return PrimitiveNode.Function.TRAPMOSNT;
				case TRANNMOSFG:    return PrimitiveNode.Function.TRANMOSFG;
				case TRANPMOSFG:    return PrimitiveNode.Function.TRAPMOSFG;
				case TRANNMOSCN:    return PrimitiveNode.Function.TRANMOSCN;
				case TRANPMOSCN:    return PrimitiveNode.Function.TRAPMOSCN;
				case TRANNMOSVTL:   return PrimitiveNode.Function.TRANMOSVTL;
				case TRANPMOSVTL:   return PrimitiveNode.Function.TRAPMOSVTL;
				case TRANNMOSVTH:   return PrimitiveNode.Function.TRANMOSVTH;
				case TRANPMOSVTH:   return PrimitiveNode.Function.TRAPMOSVTH;
				case TRANNMOSHV1:   return PrimitiveNode.Function.TRANMOSHV1;
				case TRANPMOSHV1:   return PrimitiveNode.Function.TRAPMOSHV1;
				case TRANNMOSHV2:   return PrimitiveNode.Function.TRANMOSHV2;
				case TRANPMOSHV2:   return PrimitiveNode.Function.TRAPMOSHV2;
				case TRANNMOSHV3:   return PrimitiveNode.Function.TRANMOSHV3;
				case TRANPMOSHV3:   return PrimitiveNode.Function.TRAPMOSHV3;
				case TRANNMOSNTHV1: return PrimitiveNode.Function.TRANMOSNTHV1;
				case TRANPMOSNTHV1: return PrimitiveNode.Function.TRAPMOSNTHV1;
				case TRANNMOSNTHV2: return PrimitiveNode.Function.TRANMOSNTHV2;
				case TRANPMOSNTHV2: return PrimitiveNode.Function.TRAPMOSNTHV2;
				case TRANNMOSNTHV3: return PrimitiveNode.Function.TRANMOSNTHV3;
				case TRANPMOSNTHV3: return PrimitiveNode.Function.TRAPMOSNTHV3;
				case TRANNPN:       return PrimitiveNode.Function.TRANPN;
				case TRANPNP:       return PrimitiveNode.Function.TRAPNP;
				case TRANNJFET:     return PrimitiveNode.Function.TRANJFET;
				case TRANPJFET:     return PrimitiveNode.Function.TRAPJFET;
				case TRANDMES:      return PrimitiveNode.Function.TRADMES;
				case TRANEMES:      return PrimitiveNode.Function.TRAEMES;
			}
			return PrimitiveNode.Function.TRANMOS;
		}
		if (pn == transistor4Node)
		{
			switch (techBits)
			{
				case TRANNMOS:      return PrimitiveNode.Function.TRA4NMOS;
				case TRANPMOS:      return PrimitiveNode.Function.TRA4PMOS;
				case TRANNMOSD:     return PrimitiveNode.Function.TRA4DMOS;
				case TRANPMOSD:     return PrimitiveNode.Function.TRA4PMOSD;
				case TRANNMOSNT:    return PrimitiveNode.Function.TRA4NMOSNT;
				case TRANPMOSNT:    return PrimitiveNode.Function.TRA4PMOSNT;
				case TRANNMOSFG:    return PrimitiveNode.Function.TRA4NMOSFG;
				case TRANPMOSFG:    return PrimitiveNode.Function.TRA4PMOSFG;
				case TRANNMOSCN:    return PrimitiveNode.Function.TRA4NMOSCN;
				case TRANPMOSCN:    return PrimitiveNode.Function.TRA4PMOSCN;
				case TRANNMOSVTL:   return PrimitiveNode.Function.TRA4NMOSVTL;
				case TRANPMOSVTL:   return PrimitiveNode.Function.TRA4PMOSVTL;
				case TRANNMOSVTH:   return PrimitiveNode.Function.TRA4NMOSVTH;
				case TRANPMOSVTH:   return PrimitiveNode.Function.TRA4PMOSVTH;
				case TRANNMOSHV1:   return PrimitiveNode.Function.TRA4NMOSHV1;
				case TRANPMOSHV1:   return PrimitiveNode.Function.TRA4PMOSHV1;
				case TRANNMOSHV2:   return PrimitiveNode.Function.TRA4NMOSHV2;
				case TRANPMOSHV2:   return PrimitiveNode.Function.TRA4PMOSHV2;
				case TRANNMOSHV3:   return PrimitiveNode.Function.TRA4NMOSHV3;
				case TRANPMOSHV3:   return PrimitiveNode.Function.TRA4PMOSHV3;
				case TRANNMOSNTHV1: return PrimitiveNode.Function.TRA4NMOSNTHV1;
				case TRANPMOSNTHV1: return PrimitiveNode.Function.TRA4PMOSNTHV1;
				case TRANNMOSNTHV2: return PrimitiveNode.Function.TRA4NMOSNTHV2;
				case TRANPMOSNTHV2: return PrimitiveNode.Function.TRA4PMOSNTHV2;
				case TRANNMOSNTHV3: return PrimitiveNode.Function.TRA4NMOSNTHV3;
				case TRANPMOSNTHV3: return PrimitiveNode.Function.TRA4PMOSNTHV3;
				case TRANNPN:       return PrimitiveNode.Function.TRA4NPN;
				case TRANPNP:       return PrimitiveNode.Function.TRA4PNP;
				case TRANNJFET:     return PrimitiveNode.Function.TRA4NJFET;
				case TRANPJFET:     return PrimitiveNode.Function.TRA4PJFET;
				case TRANDMES:      return PrimitiveNode.Function.TRA4DMES;
				case TRANEMES:      return PrimitiveNode.Function.TRA4EMES;
			}
			return PrimitiveNode.Function.TRA4NMOS;
		}
		if (pn == flipflopNode)
		{
			switch (techBits)
			{
				case FFTYPERS|FFCLOCKMS: return PrimitiveNode.Function.FLIPFLOPRSMS;
				case FFTYPERS|FFCLOCKP:  return PrimitiveNode.Function.FLIPFLOPRSP;
				case FFTYPERS|FFCLOCKN:  return PrimitiveNode.Function.FLIPFLOPRSN;

				case FFTYPEJK|FFCLOCKMS: return PrimitiveNode.Function.FLIPFLOPJKMS;
				case FFTYPEJK|FFCLOCKP:  return PrimitiveNode.Function.FLIPFLOPJKP;
				case FFTYPEJK|FFCLOCKN:  return PrimitiveNode.Function.FLIPFLOPJKN;

				case FFTYPED|FFCLOCKMS:  return PrimitiveNode.Function.FLIPFLOPDMS;
				case FFTYPED|FFCLOCKP:   return PrimitiveNode.Function.FLIPFLOPDP;
				case FFTYPED|FFCLOCKN:   return PrimitiveNode.Function.FLIPFLOPDN;

				case FFTYPET|FFCLOCKMS:  return PrimitiveNode.Function.FLIPFLOPTMS;
				case FFTYPET|FFCLOCKP:   return PrimitiveNode.Function.FLIPFLOPTP;
				case FFTYPET|FFCLOCKN:   return PrimitiveNode.Function.FLIPFLOPTN;
			}
			return PrimitiveNode.Function.FLIPFLOPRSMS;
		}
		if (pn == twoportNode)
		{
			switch (techBits)
			{
				case TWOPVCCS:  return PrimitiveNode.Function.VCCS;
				case TWOPCCVS:  return PrimitiveNode.Function.CCVS;
				case TWOPVCVS:  return PrimitiveNode.Function.VCVS;
				case TWOPCCCS:  return PrimitiveNode.Function.CCCS;
			}
			return PrimitiveNode.Function.TLINE;
		}
		return pn.getFunction();
	}

	/**
	 * Method to return the technology-specific function bits for a given PrimitiveNode.Function.
	 * @param function the universal function description
	 * @return the technology-specific bits to use for that function in this technology.
	 */
	public static int getPrimitiveFunctionBits(PrimitiveNode.Function function)
	{
		if (function == PrimitiveNode.Function.POLY2CAPAC)    return CAPACPOLY2;
		if (function == PrimitiveNode.Function.ECAPAC)        return CAPACELEC;
		if (function == PrimitiveNode.Function.CAPAC)         return CAPACNORM;

        if (function == PrimitiveNode.Function.RESNPOLY)      return RESISTNPOLY;
        if (function == PrimitiveNode.Function.RESPPOLY)      return RESISTPPOLY;
        if (function == PrimitiveNode.Function.RESNNSPOLY)    return RESISTNNSPOLY;
        if (function == PrimitiveNode.Function.RESPNSPOLY)    return RESISTPNSPOLY;
        if (function == PrimitiveNode.Function.RESNWELL)      return RESISTNWELL;
        if (function == PrimitiveNode.Function.RESPWELL)      return RESISTPWELL;
        if (function == PrimitiveNode.Function.RESNACTIVE)    return RESISTNACTIVE;
        if (function == PrimitiveNode.Function.RESPACTIVE)    return RESISTPACTIVE;
        if (function == PrimitiveNode.Function.RESHIRESPOLY2) return RESISTHIRESPOLY2;
        if (function == PrimitiveNode.Function.RESIST)        return RESISTNORM;

		if (function == PrimitiveNode.Function.DIODEZ)        return DIODEZENER;
		if (function == PrimitiveNode.Function.DIODE)         return DIODENORM;

		if (function == PrimitiveNode.Function.TRANMOS)       return TRANNMOS;
		if (function == PrimitiveNode.Function.TRAPMOS)       return TRANPMOS;
		if (function == PrimitiveNode.Function.TRADMOS)       return TRANNMOSD;
		if (function == PrimitiveNode.Function.TRAPMOSD)      return TRANPMOSD;
		if (function == PrimitiveNode.Function.TRANMOSNT)     return TRANNMOSNT;
		if (function == PrimitiveNode.Function.TRAPMOSNT)     return TRANPMOSNT;
		if (function == PrimitiveNode.Function.TRANMOSFG)     return TRANNMOSFG;
		if (function == PrimitiveNode.Function.TRAPMOSFG)     return TRANPMOSFG;
		if (function == PrimitiveNode.Function.TRANMOSCN)     return TRANNMOSCN;
		if (function == PrimitiveNode.Function.TRAPMOSCN)     return TRANPMOSCN;
		if (function == PrimitiveNode.Function.TRANMOSVTL)    return TRANNMOSVTL;
		if (function == PrimitiveNode.Function.TRAPMOSVTL)    return TRANPMOSVTL;
		if (function == PrimitiveNode.Function.TRANMOSVTH)    return TRANNMOSVTH;
		if (function == PrimitiveNode.Function.TRAPMOSVTH)    return TRANPMOSVTH;
		if (function == PrimitiveNode.Function.TRANMOSHV1)    return TRANNMOSHV1;
		if (function == PrimitiveNode.Function.TRAPMOSHV1)    return TRANPMOSHV1;
		if (function == PrimitiveNode.Function.TRANMOSHV2)    return TRANNMOSHV2;
		if (function == PrimitiveNode.Function.TRAPMOSHV2)    return TRANPMOSHV2;
		if (function == PrimitiveNode.Function.TRANMOSHV3)    return TRANNMOSHV3;
		if (function == PrimitiveNode.Function.TRAPMOSHV3)    return TRANPMOSHV3;
		if (function == PrimitiveNode.Function.TRANMOSNTHV1)  return TRANNMOSNTHV1;
		if (function == PrimitiveNode.Function.TRAPMOSNTHV1)  return TRANPMOSNTHV1;
		if (function == PrimitiveNode.Function.TRANMOSNTHV2)  return TRANNMOSNTHV2;
		if (function == PrimitiveNode.Function.TRAPMOSNTHV2)  return TRANPMOSNTHV2;
		if (function == PrimitiveNode.Function.TRANMOSNTHV3)  return TRANNMOSNTHV3;
		if (function == PrimitiveNode.Function.TRAPMOSNTHV3)  return TRANPMOSNTHV3;
		if (function == PrimitiveNode.Function.TRANPN)        return TRANNPN;
		if (function == PrimitiveNode.Function.TRAPNP)        return TRANPNP;
		if (function == PrimitiveNode.Function.TRANJFET)      return TRANNJFET;
		if (function == PrimitiveNode.Function.TRAPJFET)      return TRANPJFET;
		if (function == PrimitiveNode.Function.TRADMES)       return TRANDMES;
		if (function == PrimitiveNode.Function.TRAEMES)       return TRANEMES;

		if (function == PrimitiveNode.Function.TRA4NMOS)      return TRANNMOS;
		if (function == PrimitiveNode.Function.TRA4PMOS)      return TRANPMOS;
		if (function == PrimitiveNode.Function.TRA4DMOS)      return TRANNMOSD;
		if (function == PrimitiveNode.Function.TRA4PMOSD)     return TRANPMOSD;
		if (function == PrimitiveNode.Function.TRA4NMOSNT)    return TRANNMOSNT;
		if (function == PrimitiveNode.Function.TRA4PMOSNT)    return TRANPMOSNT;
		if (function == PrimitiveNode.Function.TRA4NMOSFG)    return TRANNMOSFG;
		if (function == PrimitiveNode.Function.TRA4PMOSFG)    return TRANPMOSFG;
		if (function == PrimitiveNode.Function.TRA4NMOSCN)    return TRANNMOSCN;
		if (function == PrimitiveNode.Function.TRA4PMOSCN)    return TRANPMOSCN;
		if (function == PrimitiveNode.Function.TRA4NMOSVTL)   return TRANNMOSVTL;
		if (function == PrimitiveNode.Function.TRA4PMOSVTL)   return TRANPMOSVTL;
		if (function == PrimitiveNode.Function.TRA4NMOSVTH)   return TRANNMOSVTH;
		if (function == PrimitiveNode.Function.TRA4PMOSVTH)   return TRANPMOSVTH;
		if (function == PrimitiveNode.Function.TRA4NMOSHV1)   return TRANNMOSHV1;
		if (function == PrimitiveNode.Function.TRA4PMOSHV1)   return TRANPMOSHV1;
		if (function == PrimitiveNode.Function.TRA4NMOSHV2)   return TRANNMOSHV2;
		if (function == PrimitiveNode.Function.TRA4PMOSHV2)   return TRANPMOSHV2;
		if (function == PrimitiveNode.Function.TRA4NMOSHV3)   return TRANNMOSHV3;
		if (function == PrimitiveNode.Function.TRA4PMOSHV3)   return TRANPMOSHV3;
		if (function == PrimitiveNode.Function.TRA4NMOSNTHV1) return TRANNMOSNTHV1;
		if (function == PrimitiveNode.Function.TRA4PMOSNTHV1) return TRANPMOSNTHV1;
		if (function == PrimitiveNode.Function.TRA4NMOSNTHV2) return TRANNMOSNTHV2;
		if (function == PrimitiveNode.Function.TRA4PMOSNTHV2) return TRANPMOSNTHV2;
		if (function == PrimitiveNode.Function.TRA4NMOSNTHV3) return TRANNMOSNTHV3;
		if (function == PrimitiveNode.Function.TRA4PMOSNTHV3) return TRANPMOSNTHV3;
		if (function == PrimitiveNode.Function.TRA4NPN)       return TRANNPN;
		if (function == PrimitiveNode.Function.TRA4PNP)       return TRANPNP;
		if (function == PrimitiveNode.Function.TRA4NJFET)     return TRANNJFET;
		if (function == PrimitiveNode.Function.TRA4PJFET)     return TRANPJFET;
		if (function == PrimitiveNode.Function.TRA4DMES)      return TRANDMES;
		if (function == PrimitiveNode.Function.TRA4EMES)      return TRANEMES;

		if (function == PrimitiveNode.Function.FLIPFLOPRSMS)  return FFTYPERS | FFCLOCKMS;
		if (function == PrimitiveNode.Function.FLIPFLOPRSP)   return FFTYPERS | FFCLOCKP;
		if (function == PrimitiveNode.Function.FLIPFLOPRSN)   return FFTYPERS | FFCLOCKN;
		if (function == PrimitiveNode.Function.FLIPFLOPJKMS)  return FFTYPEJK | FFCLOCKMS;
		if (function == PrimitiveNode.Function.FLIPFLOPJKP)   return FFTYPEJK | FFCLOCKP;
		if (function == PrimitiveNode.Function.FLIPFLOPJKN)   return FFTYPEJK | FFCLOCKN;
		if (function == PrimitiveNode.Function.FLIPFLOPDMS)   return FFTYPED  | FFCLOCKMS;
		if (function == PrimitiveNode.Function.FLIPFLOPDP)    return FFTYPED  | FFCLOCKP;
		if (function == PrimitiveNode.Function.FLIPFLOPDN)    return FFTYPED  | FFCLOCKN;
		if (function == PrimitiveNode.Function.FLIPFLOPTMS)   return FFTYPET  | FFCLOCKMS;
		if (function == PrimitiveNode.Function.FLIPFLOPTP)    return FFTYPET  | FFCLOCKP;
		if (function == PrimitiveNode.Function.FLIPFLOPTN)    return FFTYPET  | FFCLOCKN;

		if (function == PrimitiveNode.Function.VCCS)          return TWOPVCCS;
		if (function == PrimitiveNode.Function.CCVS)          return TWOPCCVS;
		if (function == PrimitiveNode.Function.VCVS)          return TWOPVCVS;
		if (function == PrimitiveNode.Function.CCCS)          return TWOPCCCS;
		if (function == PrimitiveNode.Function.TLINE)         return TWOPTLINE;

		return 0;
	}

	/**
	 * Method to set the pure "PrimitiveNode Function" for a primitive NodeInst in this Technology.
	 * This method is overridden by technologies (such as Schematics) that can change a node's function.
	 * @param ni the NodeInst to check.
	 * @param function the PrimitiveNode.Function to set on the NodeInst.
	 */
	public void setPrimitiveFunction(NodeInst ni, PrimitiveNode.Function function)
	{
		ni.setTechSpecific(getPrimitiveFunctionBits(function));

	}
                             /**
	 * Method to return the size of a resistor-type NodeInst in this Technology.
	 * @param ni the NodeInst.
     * @param context the VarContext in which any vars will be evaluated,
     * pass in VarContext.globalContext if no context needed, or set to null
     * to avoid evaluation of variables (if any).
	 * @return the size of the NodeInst.
	 */
    public PrimitiveNodeSize getResistorSize(NodeInst ni, VarContext context)
    {
		if (!ni.getFunction().isResistor()) return null;
        Object lengthObj = null;
        Variable var = ni.getVar(ATTR_LENGTH);
        if (var != null) {
            if (context != null) {
                lengthObj = context.evalVar(var, ni);
            } else {
                lengthObj = var.getObject();
            }
            double length = VarContext.objectToDouble(lengthObj, -1);
            if (length != -1)
                lengthObj = new Double(length);
        }

        Object widthObj = null;
        var = ni.getVar(ATTR_WIDTH);
        if (var != null) {
            if (context != null) {
                widthObj = context.evalVar(var, ni);
            } else {
                widthObj = var.getObject();
            }
            double width = VarContext.objectToDouble(widthObj, -1);
            if (width != -1)
                widthObj = new Double(width);
        }
        PrimitiveNodeSize size = new PrimitiveNodeSize(widthObj, lengthObj, true);
        return size;
    }

	/**
	 * Method to return the size of a transistor NodeInst in this Technology.
     * You should most likely be calling NodeInst.getTransistorSize instead of this.
	 * @param ni the NodeInst.
     * @param context the VarContext, set to VarContext.globalContext if not needed.
     * set to Null to avoid evaluation of variable.
	 * @return the size of the NodeInst.
	 * For FET transistors, the width of the Dimension is the width of the transistor
	 * and the height of the Dimension is the length of the transistor.
	 * For non-FET transistors, the width of the dimension is the area of the transistor.
	 */
	public TransistorSize getTransistorSize(NodeInst ni, VarContext context)
	{
		if (ni.getFunction().isFET())
		{
            Object lengthObj = null;
            Variable var = ni.getVar(ATTR_LENGTH);
            if (var != null) {
                if (context != null) {
                    lengthObj = context.evalVar(var, ni);
                } else {
                    lengthObj = var.getObject();
                }
                double length = VarContext.objectToDouble(lengthObj, -1);
                if (length != -1)
                    lengthObj = new Double(length);
            }

            Object widthObj = null;
            var = ni.getVar(ATTR_WIDTH);
            if (var != null) {
                if (context != null) {
                    widthObj = context.evalVar(var, ni);
                } else {
                    widthObj = var.getObject();
                }
                double width = VarContext.objectToDouble(widthObj, -1);
                if (width != -1)
                    widthObj = new Double(width);
            }


            Object mFactorObj = null;
            var = ni.getVar(Simulation.M_FACTOR_KEY);
            if (var != null) {
                if (context != null) {
                    mFactorObj = context.evalVar(var, ni);
                } else {
                    mFactorObj = var.getObject();
                }
                double mFactor = VarContext.objectToDouble(mFactorObj, -1);
                if (mFactor != -1)
                    mFactorObj = new Double(mFactor);
            }

            TransistorSize size = new TransistorSize(widthObj, lengthObj, new Double(1.0), mFactorObj, true);
            return size;
		}
        Object areaObj = new Double(0);
        if (context != null) {
            areaObj = context.evalVar(ni.getVar(ATTR_AREA));
            double area = VarContext.objectToDouble(areaObj, -1);
            if (area != -1) areaObj = new Double(area);
        }
        TransistorSize size = new TransistorSize(areaObj, new Double(1.0), new Double(1.0), null, true);
        return size;
    }

    /**
     * Method to set the size of a transistor NodeInst in this technology.
     * You should be calling NodeInst.setTransistorSize instead of this.
     * Width may be the area for non-FET transistors, in which case length is ignored.
     * You may also want to call setTransistorSize(NodeInst, Object, Object) to
     * set the variables to a non-double value (such as a String).
     * @param ni the NodeInst
     * @param width the new width
     * @param length the new length
     */
    @Override
    public void setPrimitiveNodeSize(NodeInst ni, double width, double length)
    {
        setPrimitiveNodeSize(ni, new Double(width), new Double(length));
//        if (ni.isFET())
//        {
//            Variable var = ni.getVar(ATTR_LENGTH);
//            if (var == null) {
//                var = ni.newVar(ATTR_LENGTH, new Double(length));
//            } else {
//                var = ni.updateVar(var.getKey(), new Double(length));
//            }
//            if (var != null) var.setDisplay(true);
//
//            var = ni.getVar(ATTR_WIDTH);
//            if (var == null) {
//                var = ni.newVar(ATTR_WIDTH, new Double(width));
//            } else {
//                var = ni.updateVar(var.getKey(), new Double(width));
//            }
//            if (var != null) var.setDisplay(true);
//        } else {
//            Variable var = ni.getVar(ATTR_AREA);
//            if (var != null) {
//                var = ni.updateVar(var.getKey(), new Double(width));
//            }
//            if (var != null) var.setDisplay(true);
//        }
    }

    /**
     * Method to set the size of a transistor NodeInst in this technology.
     * You should be calling NodeInst.setTransistorSize(Object, Object) instead.
     * Width may be the area for non-FET transistors, in which case length is ignored.
     * @param ni the NodeInst
     * @param width the new width
     * @param length the new length
     */
    public void setPrimitiveNodeSize(NodeInst ni, Object width, Object length)
    {
        if (ni.getFunction().isFET() || ni.getFunction().isResistor())
        {
            Variable var = ni.getVar(ATTR_LENGTH);
            if (var == null)
                ni.newDisplayVar(ATTR_LENGTH, length);
            else
                ni.addVar(var.withObject(length).withDisplay(true));
//            if (var == null) {
//                var = ni.newVar(ATTR_LENGTH, length);
//            } else {
//                var = ni.updateVar(var.getKey(), length);
//            }
//            if (var != null) var.setDisplay(true);

            var = ni.getVar(ATTR_WIDTH);
            if (var == null)
                ni.newDisplayVar(ATTR_WIDTH, width);
            else
                ni.addVar(var.withObject(width).withDisplay(true));
//            if (var == null) {
//                var = ni.newVar(ATTR_WIDTH, width);
//            } else {
//                var = ni.updateVar(var.getKey(), width);
//            }
//            if (var != null) var.setDisplay(true);
        } else {
            Variable var = ni.getVar(ATTR_AREA);
            if (var != null)
                ni.addVar(var.withObject(width).withDisplay(true));
//            if (var != null) {
//                var = ni.updateVar(var.getKey(), width);
//            }
//            if (var != null) var.setDisplay(true);
        }
    }

	/****************************** GENERAL PREFERENCES ******************************/

	/**
	 * Method to determine the default schematic technology.
	 * This is the technology to really use when the current technology is "schematics" and you want a layout technology.
 	 * This is important in Spice deck generation (for example) because the Spice primitives may
	 * say "2x3" on them, but a real technology (such as "mocmos") must be found to convert these pure
	 * numbers to real spacings for the deck.
	 */
	public static Technology getDefaultSchematicTechnology()
	{
		// see if the default schematics technology is already set
		Technology schemTech = User.getSchematicTechnology();
		if (schemTech != null) return schemTech;

		// look at all circuitry and see which technologies are in use
		Map<Technology,DBMath.MutableInteger> usedTechnologies = new HashMap<Technology,DBMath.MutableInteger>();
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
			usedTechnologies.put(it.next(), new DBMath.MutableInteger(0));
		for(Iterator<Library> lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library lib = lIt.next();
			if (lib.isHidden()) continue;
			for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = cIt.next();
				Technology tech = cell.getTechnology();
				if (tech == null) continue;
				DBMath.MutableInteger mi = usedTechnologies.get(tech);
				mi.increment();
			}
		}

		// ignore nonlayout technologies
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			DBMath.MutableInteger mi = usedTechnologies.get(tech);
			if (!tech.isLayout() ||
				tech.isNonElectrical() || tech.isNoPrimitiveNodes()) mi.setValue(-1);
		}

		// figure out the most popular technology
		int bestAmount = -1;
		Technology bestTech = null;
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			DBMath.MutableInteger mi = usedTechnologies.get(tech);
			if (mi.intValue() <= bestAmount) continue;
			bestAmount = mi.intValue();
			bestTech = tech;
		}
		if (bestTech == null)
		{
			// presume mosis cmos
			bestTech = getMocmosTechnology();
		}

//		User.setSchematicTechnology(bestTech);
		return bestTech;
	}

    /**
     * Method to return a gate PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.  Note: You should be calling
     * NodeInst.getTransistorDrainPort() instead of this, most likely.
     * @param ni the NodeInst
     * @return a PortInst for the gate of the transistor
     */
	@Override
	public PortInst getTransistorDrainPort(NodeInst ni) { return ni.getPortInst(2); }

	/** Return a substrate PortInst for this transistor NodeInst
     * @param ni the NodeInst
     * @return a PortInst for the substrate contact of the transistor
	 */
	@Override
	public PortInst getTransistorBiasPort(NodeInst ni)
	{
		if (ni.getNumPortInsts() < 4) return null;
		return ni.getPortInst(3);
	}


	/**
	 * Method to tell the size of negating bubbles.
     * The default is 1.2 .
	 * @return the size of negating bubbles (the diameter).
	 */
	public double getNegatingBubbleSize() { return 1.2; }

	/**
	 * Method to tell the VHDL names for a primitive in this technology, by default.
	 * These names have the form REGULAR/NEGATED, where REGULAR is the name to use
	 * for regular uses of the primitive, and NEGATED is the name to use for negated uses.
	 * @param np the primitive to query.
	 * @return the the VHDL names for the primitive, by default.
	 */
	public String getFactoryVHDLNames(PrimitiveNode np) {
        if (np == bufferNode) return "buffer/inverter";
		if (np == andNode) return "and/nand";
		if (np == orNode) return "or/nor";
		if (np == xorNode) return "xor/xnor";
		if (np == muxNode) return "mux";
        return "";
    }

}
