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

import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.geom.Point2D;
import java.awt.Dimension;
import java.util.Iterator;


/**
 * This is the Schematics technology.
 */
public class Schematics extends Technology
{
	/** the Schematics Technology object. */			public static final Schematics tech = new Schematics();

	/** Defines the Flip-flop type. */					private static final int FFTYPE =    07;
	/** Defines an RS Flip-flop. */						private static final int FFTYPERS =   0;
	/** Defines a JK Flip-flop. */						private static final int FFTYPEJK =   1;
	/** Defines a D Flip-flop. */						private static final int FFTYPED =    2;
	/** Defines a T Flip-flop. */						private static final int FFTYPET =    3;
	/** Defines the Flip-flop clocking bits. */			private static final int FFCLOCK =  014;
	/** Defines a Master/Slave Flip-flop. */			private static final int FFCLOCKMS =  0;
	/** Defines a Positive clock Flip-flop. */			private static final int FFCLOCKP =  04;
	/** Defines a Negative clock Flip-flop. */			private static final int FFCLOCKN = 010;

	/** Defines an nMOS transistor. */					private static final int TRANNMOS =   0;
	/** Defines a DMOS transistor. */					private static final int TRANDMOS =   1;
	/** Defines a PMOS transistor. */					private static final int TRANPMOS =   2;
	/** Defines an NPN Junction transistor. */			private static final int TRANNPN =    3;
	/** Defines a PNP Junction transistor. */			private static final int TRANPNP =    4;
	/** Defines an N Junction FET transistor. */		private static final int TRANNJFET =  5;
	/** Defines a P Junction FET transistor. */			private static final int TRANPJFET =  6;
	/** Defines a Depletion MESFET transistor. */		private static final int TRANDMES =   7;
	/** Defines an Enhancement MESFET transistor. */	private static final int TRANEMES =   8;

	/** Defines a normal Diode. */						private static final int DIODENORM =  0;
	/** Defines a Zener Diode. */						private static final int DIODEZENER = 1;

	/** Defines a normal Capacitor. */					private static final int CAPACNORM =  0;
	/** Defines an Electrolytic Capacitor. */			private static final int CAPACELEC =  1;

	/** Defines a Transconductance two-port (VCCS). */	private static final int TWOPVCCS =  0;
	/** Defines a Transresistance two-port (CCVS). */	private static final int TWOPCCVS =  1;
	/** Defines a Voltage gain two-port (VCVS). */		private static final int TWOPVCVS =  2;
	/** Defines a Current gain two-port (CCCS). */		private static final int TWOPCCCS =  3;
	/** Defines a Transmission Line two-port. */		private static final int TWOPTLINE = 4;

	/** wire arc */						public PrimitiveArc wire_arc;
	/** bus arc */						public PrimitiveArc bus_arc;

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

	private Layer arc_lay, bus_lay, node_lay, text_lay;
	private Technology.NodeLayer [] ffLayersRSMS, ffLayersRSP, ffLayersRSN;
	private Technology.NodeLayer [] ffLayersJKMS, ffLayersJKP, ffLayersJKN;
	private Technology.NodeLayer [] ffLayersDMS, ffLayersDP, ffLayersDN;
	private Technology.NodeLayer [] ffLayersTMS, ffLayersTP, ffLayersTN;
	private Technology.NodeLayer [] tranLayersN, tranLayersP, tranLayersD;
	private Technology.NodeLayer [] tranLayersNPN, tranLayersPNP;
	private Technology.NodeLayer [] tranLayersNJFET, tranLayersPJFET;
	private Technology.NodeLayer [] tranLayersDMES, tranLayersEMES;
	private Technology.NodeLayer [] twoLayersDefault, twoLayersVCVS, twoLayersVCCS, twoLayersCCVS, twoLayersCCCS, twoLayersTran;
	private Technology.NodeLayer [] tran4LayersN, tran4LayersP, tran4LayersD;
	private Technology.NodeLayer [] tran4LayersNPN, tran4LayersPNP;
	private Technology.NodeLayer [] tran4LayersNJFET, tran4LayersPJFET;
	private Technology.NodeLayer [] tran4LayersDMES, tran4LayersEMES;
	private Technology.NodeLayer [] diodeLayersNorm, diodeLayersZener;
	private Technology.NodeLayer [] capacitorLayersNorm, capacitorLayersElectrolytic;

	// this much from the center to the left edge
	/* 0.1 */			private final EdgeH LEFTBYP1 = new EdgeH(-0.1/2,0);
	/* 0.1333... */		private final EdgeH LEFTBYP125 = new EdgeH(-0.1333/2,0);
	/* 0.1666... */		private final EdgeH LEFTBYP166 = new EdgeH(-0.1666/2,0);
	/* 0.2 */			private final EdgeH LEFTBYP2 = new EdgeH(-0.2/2,0);
	/* 0.25 */			private final EdgeH LEFTBYP25 = new EdgeH(-0.25/2,0);
	/* 0.3 */			private final EdgeH LEFTBYP3 = new EdgeH(-0.3/2,0);
	/* 0.3333... */		private final EdgeH LEFTBYP33 = new EdgeH(-0.3333/2,0);
	/* 0.35 */			private final EdgeH LEFTBYP35 = new EdgeH(-0.35/2,0);
	/* 0.3666... */		private final EdgeH LEFTBYP3666 = new EdgeH(-0.3666/2,0);
	/* 0.4 */			private final EdgeH LEFTBYP4 = new EdgeH(-0.4/2,0);
	/* 0.45 */			private final EdgeH LEFTBYP45 = new EdgeH(-0.45/2,0);
	/* 0.5 */			private final EdgeH LEFTBYP5 = new EdgeH(-0.5/2,0);
	/* 0.6 */			private final EdgeH LEFTBYP6 = new EdgeH(-0.6/2,0);
	/* 0.6333... */		private final EdgeH LEFTBYP6333 = new EdgeH(-0.6333/2,0);
	/* 0.6666... */		private final EdgeH LEFTBYP66 = new EdgeH(-0.6666/2,0);
	/* 0.7 */			private final EdgeH LEFTBYP7 = new EdgeH(-0.7/2,0);
	/* 0.75 */			private final EdgeH LEFTBYP75 = new EdgeH(-0.75/2,0);
	/* 0.8 */			private final EdgeH LEFTBYP8 = new EdgeH(-0.8/2,0);
	/* 0.875 */			private final EdgeH LEFTBYP875 = new EdgeH(-0.875/2,0);
	/* 0.9 */			private final EdgeH LEFTBYP9 = new EdgeH(-0.9/2,0);
	/* 1.2 */			private final EdgeH LEFTBYP12 = new EdgeH(-1.2/2,0);
	/* 1.4 */			private final EdgeH LEFTBYP14 = new EdgeH(-1.4/2,0);
	/* 1.6 */			private final EdgeH LEFTBY1P6 = new EdgeH(-1.6/2,0);

	// this much from the center to the right edge
	/* 0.1 */			private final EdgeH RIGHTBYP1 = new EdgeH(0.1/2,0);
	/* 0.1333... */		private final EdgeH RIGHTBYP125 = new EdgeH(0.1333/2,0);
	/* 0.1666... */		private final EdgeH RIGHTBYP166 = new EdgeH(0.1666/2,0);
	/* 0.2 */			private final EdgeH RIGHTBYP2 = new EdgeH(0.2/2,0);
	/* 0.25 */			private final EdgeH RIGHTBYP25 = new EdgeH(0.25/2,0);
	/* 0.3 */			private final EdgeH RIGHTBYP3 = new EdgeH(0.3/2,0);
	/* 0.3333... */		private final EdgeH RIGHTBYP33 = new EdgeH(0.3333/2,0);
	/* 0.35 */			private final EdgeH RIGHTBYP35 = new EdgeH(0.35/2,0);
	/* 0.3666... */		private final EdgeH RIGHTBYP3666 = new EdgeH(0.3666/2,0);
	/* 0.3833... */		private final EdgeH RIGHTBYP3833 = new EdgeH(0.3833/2,0);
	/* 0.4 */			private final EdgeH RIGHTBYP4 = new EdgeH(0.4/2,0);
	/* 0.4333... */		private final EdgeH RIGHTBYP433 = new EdgeH(0.4333/2,0);
	/* 0.45 */			private final EdgeH RIGHTBYP45 = new EdgeH(0.45/2,0);
	/* 0.5 */			private final EdgeH RIGHTBYP5 = new EdgeH(0.5/2,0);
	/* 0.5166... */		private final EdgeH RIGHTBYP5166 = new EdgeH(0.5166/2,0);
	/* 0.55 */			private final EdgeH RIGHTBYP55 = new EdgeH(0.55/2,0);
	/* 0.5666... */		private final EdgeH RIGHTBYP566 = new EdgeH(0.5666/2,0);
	/* 0.6 */			private final EdgeH RIGHTBYP6 = new EdgeH(0.6/2,0);
	/* 0.6166... */		private final EdgeH RIGHTBYP6166 = new EdgeH(0.6166/2,0);
	/* 0.6333... */		private final EdgeH RIGHTBYP6333 = new EdgeH(0.6333/2,0);
	/* 0.6666... */		private final EdgeH RIGHTBYP66 = new EdgeH(0.6666/2,0);
	/* 0.7 */			private final EdgeH RIGHTBYP7 = new EdgeH(0.7/2,0);
	/* 0.75 */			private final EdgeH RIGHTBYP75 = new EdgeH(0.75/2,0);
	/* 0.8 */			private final EdgeH RIGHTBYP8 = new EdgeH(0.8/2,0);
	/* 0.875 */			private final EdgeH RIGHTBYP875 = new EdgeH(0.875/2,0);
	/* 0.9 */			private final EdgeH RIGHTBYP9 = new EdgeH(0.9/2,0);

	// this much from the center to the bottom edge
	/* 0.1 */			private final EdgeV BOTBYP1 = new EdgeV(-0.1/2,0); 
	/* 0.125 */			private final EdgeV BOTBYP125 = new EdgeV(-0.125/2,0);   
	/* 0.166...  */		private final EdgeV BOTBYP166 = new EdgeV(-0.166/2,0);	
	/* 0.2 */			private final EdgeV BOTBYP2 = new EdgeV(-0.2/2,0); 
	/* 0.25 */			private final EdgeV BOTBYP25 = new EdgeV(-0.25/2,0);	
	/* 0.3 */			private final EdgeV BOTBYP3 = new EdgeV(-0.3/2,0); 
	/* 0.3333... */		private final EdgeV BOTBYP33 = new EdgeV(-0.3333/2,0);	
	/* 0.375 */			private final EdgeV BOTBYP375 = new EdgeV(-0.375/2,0);        
	/* 0.4 */			private final EdgeV BOTBYP4 = new EdgeV(-0.4/2,0); 
	/* 0.5 */			private final EdgeV BOTBYP5 = new EdgeV(-0.5/2,0); 
	/* 0.6 */			private final EdgeV BOTBYP6 = new EdgeV(-0.6/2,0); 
	/* 0.6666... */		private final EdgeV BOTBYP66 = new EdgeV(-0.6666/2,0);	
	/* 0.7 */			private final EdgeV BOTBYP7 = new EdgeV(-0.7/2,0); 
	/* 0.75 */			private final EdgeV BOTBYP75 = new EdgeV(-0.75/2,0);	
	/* 0.8 */			private final EdgeV BOTBYP8 = new EdgeV(-0.8/2,0); 
	/* 0.875 */			private final EdgeV BOTBYP875 = new EdgeV(-0.875/2,0);        
	/* 0.9 */			private final EdgeV BOTBYP9 = new EdgeV(-0.9/2,0);	

	// this much from the center to the top edge
	/* 0.1 */			private final EdgeV TOPBYP1 = new EdgeV(0.1/2,0);	
	/* 0.2 */			private final EdgeV TOPBYP2 = new EdgeV(0.2/2,0);	
	/* 0.25 */			private final EdgeV TOPBYP25 = new EdgeV(0.25/2,0);	
	/* 0.3 */			private final EdgeV TOPBYP3 = new EdgeV(0.3/2,0);	
	/* 0.3333... */		private final EdgeV TOPBYP33 = new EdgeV(0.3333/2,0);	
	/* 0.4 */			private final EdgeV TOPBYP4 = new EdgeV(0.4/2,0);	
	/* 0.5 */			private final EdgeV TOPBYP5 = new EdgeV(0.5/2,0);	
	/* 0.5833... */		private final EdgeV TOPBYP5833 = new EdgeV(0.5833/2,0);	
	/* 0.6 */			private final EdgeV TOPBYP6 = new EdgeV(0.6/2,0);	
	/* 0.6666... */		private final EdgeV TOPBYP66 = new EdgeV(0.6666/2,0);	
	/* 0.7 */			private final EdgeV TOPBYP7 = new EdgeV(0.7/2,0);	
	/* 0.75 */			private final EdgeV TOPBYP75 = new EdgeV(0.75/2,0);	
	/* 0.8 */			private final EdgeV TOPBYP8 = new EdgeV(0.8/2,0);	
	/* 0.8666... */		private final EdgeV TOPBYP866 = new EdgeV(0.8666/2,0);	
	/* 0.875 */			private final EdgeV TOPBYP875 = new EdgeV(0.875/2,0);	
	/* 0.9 */			private final EdgeV TOPBYP9 = new EdgeV(0.9/2,0);	

	// -------------------- private and protected methods ------------------------

	private Schematics()
	{
		setTechName("schematic");
		setTechDesc("Schematic Capture");
		setScale(4000);
		setNonStandard();
		setStaticTechnology();

		//**************************************** LAYERS ****************************************

		/** arc layer */
		arc_lay = Layer.newInstance(this, "Arc",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0,0,255,0.8,1,
			new int[] {0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,
				0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF}));

		/** bus layer */
		bus_lay = Layer.newInstance(this, "Bus",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 107,226,96,0.8,1,
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

		/** node layer */
		node_lay = Layer.newInstance(this, "Node",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 255,0,0,0.8,1,
			new int[] {0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,
				0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF}));

		/** text layer */
		text_lay = Layer.newInstance(this, "Text",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0,0,0,0.8,1,
			new int[] {0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,
				0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF,0xFFFF}));

		// The layer functions
		arc_lay.setFunction(Layer.Function.METAL1);														// arc
		bus_lay.setFunction(Layer.Function.BUS);														// bus
		node_lay.setFunction(Layer.Function.ART);														// node
		text_lay.setFunction(Layer.Function.ART);														// text


		//**************************************** ARCS ****************************************

		/** wire arc */
		wire_arc = PrimitiveArc.newInstance(this, "wire", 0.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(arc_lay, 0, Poly.Type.FILLED)
		});
		wire_arc.setFunction(PrimitiveArc.Function.METAL1);
		wire_arc.setFixedAngle();
		wire_arc.clearSlidable();
		wire_arc.setAngleIncrement(45);

		/** bus arc */
		bus_arc = PrimitiveArc.newInstance(this, "bus", 1.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(bus_lay, 0, Poly.Type.FILLED)
		});
		bus_arc.setFunction(PrimitiveArc.Function.BUS);
		bus_arc.setFixedAngle();
		bus_arc.clearSlidable();
		bus_arc.setAngleIncrement(45);


		//**************************************** NODES ****************************************
		
		// this text descriptor is used for all text on nodes
		TextDescriptor tdBig = TextDescriptor.newBlankDescriptor().setRelSize(2);
		TextDescriptor tdSmall = TextDescriptor.newBlankDescriptor().setRelSize(1);

		/** wire pin */
		wirePinNode = PrimitiveNode.newInstance("Wire_Pin", this, 0.5, 0.5, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(arc_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.CENTER)})
			});
		wirePinNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, wirePinNode, new ArcProto[] {wire_arc}, "wire", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.CENTER, EdgeH.CENTER, EdgeV.CENTER)
			});
		wirePinNode.setFunction(NodeProto.Function.PIN);
		wirePinNode.setSquare();
		wirePinNode.setWipeOn1or2();

		/** bus pin */
		busPinNode = PrimitiveNode.newInstance("Bus_Pin", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(bus_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.CENTER)}),
				new Technology.NodeLayer(arc_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.CENTER)})
			});
		busPinNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, busPinNode, new ArcProto[] {wire_arc, bus_arc}, "bus", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.CENTER, EdgeH.CENTER, EdgeV.CENTER)
			});
		busPinNode.setFunction(NodeProto.Function.PIN);
		busPinNode.setSquare();
		busPinNode.setWipeOn1or2();

		/** wire con */
		Technology.NodeLayer letterJ;
		wireConNode = PrimitiveNode.newInstance("Wire_Con", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				letterJ = new Technology.NodeLayer(text_lay, 0, Poly.Type.TEXTCENT, Technology.NodeLayer.POINTS, Technology.TechPoint.ATCENTER)
			});
		PrimitivePort wireCon_port = PrimitivePort.newInstance(this, wireConNode, new ArcProto[] {wire_arc, bus_arc}, "wire", 0,180, 0, PortProto.Characteristic.UNKNOWN,
			EdgeH.fromLeft(0.5), EdgeV.fromBottom(0.5), EdgeH.fromRight(0.5), EdgeV.fromTop(0.5));
		wireCon_port.setIsolated();
		wireConNode.addPrimitivePorts(new PrimitivePort [] {wireCon_port});
		wireConNode.setFunction(NodeProto.Function.CONNECT);
		letterJ.setMessage("J");
		letterJ.setDescriptor(tdBig);

		/** general buffer */
		bufferNode = PrimitiveNode.newInstance("Buffer", this, 6.0, 6.0, new SizeOffset(0, 1, 0.5, 0.5),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.POINTS,
					new Technology.TechPoint [] {
						new Technology.TechPoint(RIGHTBYP66, EdgeV.CENTER),
						new Technology.TechPoint(EdgeH.LEFTEDGE, TOPBYP875),
						new Technology.TechPoint(EdgeH.LEFTEDGE, BOTBYP875)
					})
			});
		bufferNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, bufferNode, new ArcProto[] {wire_arc, bus_arc}, "a", 180,0, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.CENTER, EdgeH.LEFTEDGE, EdgeV.CENTER),
				PrimitivePort.newInstance(this, bufferNode, new ArcProto[] {wire_arc}, "c", 270,0, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, BOTBYP33, EdgeH.CENTER, BOTBYP33),
				PrimitivePort.newInstance(this, bufferNode, new ArcProto[] {wire_arc, bus_arc}, "y", 0,0, 2, PortProto.Characteristic.UNKNOWN,
					RIGHTBYP66, EdgeV.CENTER, RIGHTBYP66, EdgeV.CENTER)
			});
		bufferNode.setFunction(NodeProto.Function.BUFFER);

		/** general and */
		andNode = PrimitiveNode.newInstance("And", this, 8.0, 6.0, new SizeOffset(0, 0.5, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(0.5), EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.fromCenter(0.5), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(0.5), EdgeV.fromCenter(-3))}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS,
					new Technology.TechPoint [] {
						new Technology.TechPoint(EdgeH.fromCenter(0.5), EdgeV.fromCenter(3)),
						new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(3)),
						new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.TOPEDGE),
						new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.BOTTOMEDGE),
						new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(-3)),
						new Technology.TechPoint(EdgeH.fromCenter(0.5), EdgeV.fromCenter(-3))
					})
			});
		PrimitivePort and_port = PrimitivePort.newInstance(this, andNode, new ArcProto[] {wire_arc, bus_arc}, "a", 180,0, 0, PortProto.Characteristic.IN,
			EdgeH.fromCenter(-4), EdgeV.BOTTOMEDGE, EdgeH.fromCenter(-4), EdgeV.TOPEDGE);
		and_port.setIsolated();
		andNode.addPrimitivePorts(new PrimitivePort []
			{
				and_port,
				PrimitivePort.newInstance(this, andNode, new ArcProto[] {wire_arc, bus_arc}, "y", 0,0, 1, PortProto.Characteristic.OUT,
					EdgeH.fromCenter(3.5), EdgeV.BOTTOMEDGE, EdgeH.fromCenter(3.5), EdgeV.TOPEDGE),
				PrimitivePort.newInstance(this, andNode, new ArcProto[] {wire_arc, bus_arc}, "yt", 0,0, 2, PortProto.Characteristic.OUT,
					EdgeH.fromCenter(2.75), EdgeV.fromCenter(2), EdgeH.fromCenter(2.75), EdgeV.fromCenter(2)),
				PrimitivePort.newInstance(this, andNode, new ArcProto[] {wire_arc, bus_arc}, "yc", 0,0, 3, PortProto.Characteristic.OUT,
					EdgeH.fromCenter(2.75), EdgeV.fromCenter(-2), EdgeH.fromCenter(2.75), EdgeV.fromCenter(-2))
			});
		andNode.setFunction(NodeProto.Function.GATEAND);

		/** general or */
		orNode = PrimitiveNode.newInstance("Or", this, 10.0, 6.0, new SizeOffset(1, 0.5, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-9), EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(-3))}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(-3)),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(4.5), EdgeV.CENTER)}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(4.5), EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(-3))}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.TOPEDGE),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.BOTTOMEDGE),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(-3)),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(-3)),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(-3))
				})
			});
		PrimitivePort or_port = PrimitivePort.newInstance(this, orNode, new ArcProto[] {wire_arc, bus_arc}, "a", 180,0, 0, PortProto.Characteristic.IN,
			EdgeH.fromCenter(-4), EdgeV.BOTTOMEDGE, EdgeH.fromCenter(-3), EdgeV.TOPEDGE);
		or_port.setIsolated();
		orNode.addPrimitivePorts(new PrimitivePort []
			{
				or_port,
				PrimitivePort.newInstance(this, orNode, new ArcProto[] {wire_arc, bus_arc}, "y", 0,0, 1, PortProto.Characteristic.OUT,
					EdgeH.fromCenter(4.5), EdgeV.CENTER, EdgeH.fromCenter(4.5), EdgeV.CENTER),
				PrimitivePort.newInstance(this, orNode, new ArcProto[] {wire_arc, bus_arc}, "yt", 0,0, 2, PortProto.Characteristic.OUT,
					EdgeH.fromCenter(2.65), EdgeV.fromCenter(2), EdgeH.fromCenter(2.65), EdgeV.fromCenter(2)),
				PrimitivePort.newInstance(this, orNode, new ArcProto[] {wire_arc, bus_arc}, "yc", 0,0, 3, PortProto.Characteristic.OUT,
					EdgeH.fromCenter(2.65), EdgeV.fromCenter(-2), EdgeH.fromCenter(2.65), EdgeV.fromCenter(-2))
			});
		orNode.setFunction(NodeProto.Function.GATEOR);

		/** general xor */
		xorNode = PrimitiveNode.newInstance("Xor", this, 10.0, 6.0, new SizeOffset(0, 0.5, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-9), EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(-3))}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(-3)),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(4.5), EdgeV.CENTER)}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(4.5), EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(-3))}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-10), EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.fromCenter(-5), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(-5), EdgeV.fromCenter(-3))}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.TOPEDGE),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(3)),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.BOTTOMEDGE),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(-3)),
					new Technology.TechPoint(EdgeH.fromCenter(-4), EdgeV.fromCenter(-3)),
					new Technology.TechPoint(EdgeH.fromCenter(-0.75), EdgeV.fromCenter(-3))
				})
			});
		PrimitivePort xor_port = PrimitivePort.newInstance(this, xorNode, new ArcProto[] {wire_arc, bus_arc}, "a", 180,0, 0, PortProto.Characteristic.IN,
			EdgeH.fromCenter(-4), EdgeV.BOTTOMEDGE, EdgeH.fromCenter(-3), EdgeV.TOPEDGE);
		xor_port.setIsolated();
		xorNode.addPrimitivePorts(new PrimitivePort []
			{
				xor_port,
				PrimitivePort.newInstance(this, xorNode, new ArcProto[] {wire_arc, bus_arc}, "y", 0,0, 1, PortProto.Characteristic.OUT,
					EdgeH.fromCenter(4.5), EdgeV.CENTER, EdgeH.fromCenter(4.5), EdgeV.CENTER),
				PrimitivePort.newInstance(this, xorNode, new ArcProto[] {wire_arc, bus_arc}, "yt", 0,0, 2, PortProto.Characteristic.OUT,
					EdgeH.fromCenter(2.65), EdgeV.fromCenter(2), EdgeH.fromCenter(2.65), EdgeV.fromCenter(2)),
				PrimitivePort.newInstance(this, xorNode, new ArcProto[] {wire_arc, bus_arc}, "yc", 0,0, 3, PortProto.Characteristic.OUT,
					EdgeH.fromCenter(2.65), EdgeV.fromCenter(-2), EdgeH.fromCenter(2.65), EdgeV.fromCenter(-2))
			});
		xorNode.setFunction(NodeProto.Function.GATEXOR);

		/** general flip flop */
		Technology.NodeLayer ffBox = new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX);
		Technology.NodeLayer ffArrow = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
				new Technology.TechPoint(EdgeH.LEFTEDGE, BOTBYP2),
				new Technology.TechPoint(LEFTBYP7, EdgeV.CENTER),
				new Technology.TechPoint(EdgeH.LEFTEDGE, TOPBYP2)});
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
			new Technology.TechPoint(EdgeH.CENTER, BOTBYP2)});
		Technology.NodeLayer ffLetterD = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, TOPBYP4),
			new Technology.TechPoint(EdgeH.LEFTEDGE, TOPBYP8),
			new Technology.TechPoint(LEFTBYP4,       TOPBYP8),
			new Technology.TechPoint(LEFTBYP4,       TOPBYP4)});
		ffLetterD.setMessage("D");
		ffLetterD.setDescriptor(tdSmall);
		Technology.NodeLayer ffLetterR = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, TOPBYP4),
			new Technology.TechPoint(EdgeH.LEFTEDGE, TOPBYP8),
			new Technology.TechPoint(LEFTBYP4,       TOPBYP8),
			new Technology.TechPoint(LEFTBYP4,       TOPBYP4)});
		ffLetterR.setMessage("R");
		ffLetterR.setDescriptor(tdSmall);
		Technology.NodeLayer ffLetterJ = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, TOPBYP4),
			new Technology.TechPoint(EdgeH.LEFTEDGE, TOPBYP8),
			new Technology.TechPoint(LEFTBYP4,       TOPBYP8),
			new Technology.TechPoint(LEFTBYP4,       TOPBYP4)});
		ffLetterJ.setMessage("J");
		ffLetterJ.setDescriptor(tdSmall);
		Technology.NodeLayer ffLetterT = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, TOPBYP4),
			new Technology.TechPoint(EdgeH.LEFTEDGE, TOPBYP8),
			new Technology.TechPoint(LEFTBYP4,       TOPBYP8),
			new Technology.TechPoint(LEFTBYP4,       TOPBYP4)});
		ffLetterT.setMessage("T");
		ffLetterT.setDescriptor(tdSmall);
		Technology.NodeLayer ffLetterE = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, BOTBYP4),
			new Technology.TechPoint(EdgeH.LEFTEDGE, BOTBYP8),
			new Technology.TechPoint(LEFTBYP4,       BOTBYP8),
			new Technology.TechPoint(LEFTBYP4,       BOTBYP4)});
		ffLetterE.setMessage("E");
		ffLetterE.setDescriptor(tdSmall);
		Technology.NodeLayer ffLetterS = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, BOTBYP4),
			new Technology.TechPoint(EdgeH.LEFTEDGE, BOTBYP8),
			new Technology.TechPoint(LEFTBYP4,       BOTBYP8),
			new Technology.TechPoint(LEFTBYP4,       BOTBYP4)});
		ffLetterS.setMessage("S");
		ffLetterS.setDescriptor(tdSmall);
		Technology.NodeLayer ffLetterK = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, BOTBYP4),
			new Technology.TechPoint(EdgeH.LEFTEDGE, BOTBYP8),
			new Technology.TechPoint(LEFTBYP4,       BOTBYP8),
			new Technology.TechPoint(LEFTBYP4,       BOTBYP4)});
		ffLetterK.setMessage("K");
		ffLetterK.setDescriptor(tdSmall);
		Technology.NodeLayer ffLetterQ = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.RIGHTEDGE, TOPBYP4),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, TOPBYP8),
			new Technology.TechPoint(RIGHTBYP4,       TOPBYP8),
			new Technology.TechPoint(RIGHTBYP4,       TOPBYP4)});
		ffLetterQ.setMessage("Q");
		ffLetterQ.setDescriptor(tdSmall);
		Technology.NodeLayer ffLetterQB = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.RIGHTEDGE, BOTBYP4),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, BOTBYP8),
			new Technology.TechPoint(RIGHTBYP4,       BOTBYP8),
			new Technology.TechPoint(RIGHTBYP4,       BOTBYP4)});
		ffLetterQB.setMessage("QB");
		ffLetterQB.setDescriptor(tdSmall);
		Technology.NodeLayer ffLetterPR = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP6,        TOPBYP6),
			new Technology.TechPoint(LEFTBYP6,        EdgeV.TOPEDGE),
			new Technology.TechPoint(RIGHTBYP6,       EdgeV.TOPEDGE),
			new Technology.TechPoint(RIGHTBYP6,       TOPBYP6)});
		ffLetterPR.setMessage("PR");
		ffLetterPR.setDescriptor(tdSmall);
		Technology.NodeLayer ffLetterCLR = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP6,        BOTBYP6),
			new Technology.TechPoint(LEFTBYP6,        EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(RIGHTBYP6,       EdgeV.BOTTOMEDGE),
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
		flipflopNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, flipflopNode, new ArcProto[] {wire_arc}, "i1", 180,45, 0, PortProto.Characteristic.IN,
					EdgeH.LEFTEDGE, TOPBYP6, EdgeH.LEFTEDGE, TOPBYP6),
				PrimitivePort.newInstance(this, flipflopNode, new ArcProto[] {wire_arc}, "i2", 180,45, 1, PortProto.Characteristic.IN,
					EdgeH.LEFTEDGE, BOTBYP6, EdgeH.LEFTEDGE, BOTBYP6),
				PrimitivePort.newInstance(this, flipflopNode, new ArcProto[] {wire_arc}, "q", 0,45, 2, PortProto.Characteristic.OUT,
					EdgeH.RIGHTEDGE, TOPBYP6, EdgeH.RIGHTEDGE, TOPBYP6),
				PrimitivePort.newInstance(this, flipflopNode, new ArcProto[] {wire_arc}, "qb", 0,45, 3, PortProto.Characteristic.OUT,
					EdgeH.RIGHTEDGE, BOTBYP6, EdgeH.RIGHTEDGE, BOTBYP6),
				PrimitivePort.newInstance(this, flipflopNode, new ArcProto[] {wire_arc}, "ck", 180,45, 4, PortProto.Characteristic.IN,
					EdgeH.LEFTEDGE, EdgeV.CENTER, EdgeH.LEFTEDGE, EdgeV.CENTER),
				PrimitivePort.newInstance(this, flipflopNode, new ArcProto[] {wire_arc}, "preset", 90,45, 5, PortProto.Characteristic.IN,
					EdgeH.CENTER, EdgeV.TOPEDGE, EdgeH.CENTER, EdgeV.TOPEDGE),
				PrimitivePort.newInstance(this, flipflopNode, new ArcProto[] {wire_arc}, "clear", 270,45, 6, PortProto.Characteristic.IN,
					EdgeH.CENTER, EdgeV.BOTTOMEDGE, EdgeH.CENTER, EdgeV.BOTTOMEDGE)
			});
		flipflopNode.setFunction(NodeProto.Function.FLIPFLOP);

		/** mux */
		muxNode = PrimitiveNode.newInstance("Mux", this, 8.0, 10.0, new SizeOffset(0.5, 0.5, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.POINTS,
					new Technology.TechPoint [] {
						new Technology.TechPoint(RIGHTBYP8, TOPBYP75),
						new Technology.TechPoint(RIGHTBYP8, BOTBYP75),
						new Technology.TechPoint(LEFTBYP8, EdgeV.BOTTOMEDGE),
						new Technology.TechPoint(LEFTBYP8, EdgeV.TOPEDGE)
					})
			});
		PrimitivePort mux_port = PrimitivePort.newInstance(this, muxNode, new ArcProto[] {wire_arc, bus_arc}, "a", 180,0, 0, PortProto.Characteristic.IN,
			LEFTBYP8, EdgeV.BOTTOMEDGE, LEFTBYP8, EdgeV.TOPEDGE);
		mux_port.setIsolated();
		muxNode.addPrimitivePorts(new PrimitivePort []
			{
				mux_port,
				PrimitivePort.newInstance(this, muxNode, new ArcProto[] {wire_arc}, "s", 270,0, 2, PortProto.Characteristic.IN,
					EdgeH.CENTER, BOTBYP875, EdgeH.CENTER, BOTBYP875),
				PrimitivePort.newInstance(this, muxNode, new ArcProto[] {wire_arc, bus_arc}, "y", 0,0, 1, PortProto.Characteristic.OUT,
					RIGHTBYP8, EdgeV.CENTER, RIGHTBYP8, EdgeV.CENTER)
			});
		muxNode.setFunction(NodeProto.Function.MUX);

		/** black box */
		bboxNode = PrimitiveNode.newInstance("Bbox", this, 10.0, 10.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		PrimitivePort bbox_port1 = PrimitivePort.newInstance(this, bboxNode, new ArcProto[] {wire_arc, bus_arc}, "a", 0,45, 0, PortProto.Characteristic.UNKNOWN,
			EdgeH.RIGHTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE);
		bbox_port1.setIsolated();
		PrimitivePort bbox_port2 = PrimitivePort.newInstance(this, bboxNode, new ArcProto[] {wire_arc, bus_arc}, "b", 90,45, 1, PortProto.Characteristic.UNKNOWN,
			EdgeH.LEFTEDGE, EdgeV.TOPEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE);
		bbox_port2.setIsolated();
		PrimitivePort bbox_port3 = PrimitivePort.newInstance(this, bboxNode, new ArcProto[] {wire_arc, bus_arc}, "c", 180,45, 2, PortProto.Characteristic.UNKNOWN,
			EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.LEFTEDGE, EdgeV.TOPEDGE);
		bbox_port3.setIsolated();
		PrimitivePort bbox_port4 = PrimitivePort.newInstance(this, bboxNode, new ArcProto[] {wire_arc, bus_arc}, "d", 270,45, 3, PortProto.Characteristic.UNKNOWN,
			EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.BOTTOMEDGE);
		bbox_port4.setIsolated();
		bboxNode.addPrimitivePorts(new PrimitivePort [] {bbox_port1, bbox_port2, bbox_port3, bbox_port4});
		bboxNode.setFunction(NodeProto.Function.UNKNOWN);

		/** switch */
		switchNode = PrimitiveNode.newInstance("Switch", this, 6.0, 2.0, new SizeOffset(0.5, 0.5, 0.5, 0.5),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.fromRight(1.25), EdgeV.CENTER)}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.CENTER)}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromRight(1), EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromCenter(1))})
			});
		PrimitivePort switch_port = PrimitivePort.newInstance(this, switchNode, new ArcProto[] {wire_arc, bus_arc}, "a", 180,90, 0, PortProto.Characteristic.UNKNOWN,
			EdgeH.fromLeft(1), EdgeV.fromBottom(1), EdgeH.fromLeft(1), EdgeV.fromTop(1));
		switch_port.setIsolated();
		switchNode.addPrimitivePorts(new PrimitivePort []
			{
				switch_port,
				PrimitivePort.newInstance(this, switchNode, new ArcProto[] {wire_arc, bus_arc}, "y", 0,90, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.fromRight(1), EdgeV.CENTER, EdgeH.fromRight(1), EdgeV.CENTER)
			});
		switchNode.setFunction(NodeProto.Function.UNKNOWN);

		/** off page connector */
		offpageNode = PrimitiveNode.newInstance("Off-Page", this, 2.0, 1.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE),
					new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.TOPEDGE),
					new Technology.TechPoint(RIGHTBYP5, EdgeV.TOPEDGE),
					new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.CENTER),
					new Technology.TechPoint(RIGHTBYP5, EdgeV.BOTTOMEDGE)})
			});
		offpageNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, offpageNode, new ArcProto[] {wire_arc, bus_arc}, "a", 180,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.CENTER, EdgeH.LEFTEDGE, EdgeV.CENTER),
				PrimitivePort.newInstance(this, offpageNode, new ArcProto[] {wire_arc, bus_arc}, "y", 0,45, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.RIGHTEDGE, EdgeV.CENTER, EdgeH.RIGHTEDGE, EdgeV.CENTER)
			});
		offpageNode.setFunction(NodeProto.Function.CONNECT);

		/** power */
		powerNode = PrimitiveNode.newInstance("Power", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.TOPEDGE)}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, TOPBYP75)})
			});
		powerNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, powerNode, new ArcProto[] {wire_arc}, "pwr", 0,180, 0, PortProto.Characteristic.PWR,
					EdgeH.CENTER, EdgeV.CENTER, EdgeH.CENTER, EdgeV.CENTER)
			});
		powerNode.setFunction(NodeProto.Function.CONPOWER);
		powerNode.isSquare();

		/** ground */
		groundNode = PrimitiveNode.newInstance("Ground", this, 3.0, 4.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER), new Technology.TechPoint(EdgeH.CENTER, EdgeV.TOPEDGE),
					new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.CENTER), new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.CENTER),
					new Technology.TechPoint(LEFTBYP75, BOTBYP25), new Technology.TechPoint(RIGHTBYP75, BOTBYP25),
					new Technology.TechPoint(LEFTBYP5, BOTBYP5), new Technology.TechPoint(RIGHTBYP5, BOTBYP5),
					new Technology.TechPoint(LEFTBYP25, BOTBYP75), new Technology.TechPoint(RIGHTBYP25, BOTBYP75),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.BOTTOMEDGE), new Technology.TechPoint(EdgeH.CENTER, EdgeV.BOTTOMEDGE)})
			});
		groundNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, groundNode, new ArcProto[] {wire_arc}, "gnd", 90,90, 0, PortProto.Characteristic.GND,
					EdgeH.CENTER, EdgeV.TOPEDGE, EdgeH.CENTER, EdgeV.TOPEDGE)
			});
		groundNode.setFunction(NodeProto.Function.CONGROUND);

		/** source */
		sourceNode = PrimitiveNode.newInstance("Source", this, 6.0, 6.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.CENTER)}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(LEFTBYP3, TOPBYP6), new Technology.TechPoint(RIGHTBYP3, TOPBYP6),
					new Technology.TechPoint(EdgeH.CENTER, TOPBYP3), new Technology.TechPoint(EdgeH.CENTER, TOPBYP9)})
			});
		sourceNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, sourceNode, new ArcProto[] {wire_arc}, "plus", 90,0, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.TOPEDGE, EdgeH.CENTER, EdgeV.TOPEDGE),
				PrimitivePort.newInstance(this, sourceNode, new ArcProto[] {wire_arc}, "minus", 270,0, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.BOTTOMEDGE, EdgeH.CENTER, EdgeV.BOTTOMEDGE)
			});
		sourceNode.setFunction(NodeProto.Function.SOURCE);
		sourceNode.isSquare();

		/** transistor */
		Technology.NodeLayer tranLayerMOS = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP75, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP75, BOTBYP5),
			new Technology.TechPoint(RIGHTBYP75, BOTBYP5),
			new Technology.TechPoint(RIGHTBYP75, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.BOTTOMEDGE)});
		Technology.NodeLayer tranLayerTranTop = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP75, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP75, BOTBYP25)});
		Technology.NodeLayer tranLayerNMOS = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.CENTER, BOTBYP25),
			new Technology.TechPoint(EdgeH.CENTER, EdgeV.fromTop(1))});
		Technology.NodeLayer tranLayerPMOS = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.CENTER, TOPBYP25),
			new Technology.TechPoint(EdgeH.CENTER, EdgeV.fromTop(1))});
		Technology.NodeLayer tranLayerPMOSCircle = new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
			new Technology.TechPoint(EdgeH.CENTER, BOTBYP25)});
		Technology.NodeLayer tranLayerDMOS = new Technology.NodeLayer(node_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP75, BOTBYP75),
			new Technology.TechPoint(RIGHTBYP75, BOTBYP5)});
		Technology.NodeLayer tranLayerBTran1 = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP75, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP25, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP25, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP75, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.BOTTOMEDGE)});
		Technology.NodeLayer tranLayerBTran2 = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP75, BOTBYP75),
			new Technology.TechPoint(LEFTBYP75, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP5, BOTBYP875)});
		Technology.NodeLayer tranLayerBTran3 = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP5, BOTBYP375),
			new Technology.TechPoint(LEFTBYP25, BOTBYP25),
			new Technology.TechPoint(LEFTBYP25, BOTBYP5)});
		Technology.NodeLayer tranLayerBTran4 = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP75, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP75, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP75, BOTBYP25),
			new Technology.TechPoint(LEFTBYP875, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP875, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP75, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP75, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(RIGHTBYP75, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.BOTTOMEDGE)});
		Technology.NodeLayer tranLayerBTran5 = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP125, EdgeV.CENTER),
			new Technology.TechPoint(EdgeH.CENTER, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP125, EdgeV.CENTER)});
		Technology.NodeLayer tranLayerBTran6 = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP125, EdgeV.CENTER),
			new Technology.TechPoint(EdgeH.CENTER, TOPBYP25),
			new Technology.TechPoint(RIGHTBYP125, EdgeV.CENTER)});
		Technology.NodeLayer tranLayerBTran7 = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP75, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP75, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP75, BOTBYP25),
			new Technology.TechPoint(LEFTBYP875, BOTBYP25),
			new Technology.TechPoint(LEFTBYP5, BOTBYP25),
			new Technology.TechPoint(LEFTBYP25, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP25, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP5, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP875, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP75, BOTBYP25),
			new Technology.TechPoint(RIGHTBYP75, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(RIGHTBYP75, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.BOTTOMEDGE)});
		tranLayersN = new Technology.NodeLayer [] {tranLayerMOS, tranLayerTranTop, tranLayerNMOS};
		tranLayersP = new Technology.NodeLayer [] {tranLayerMOS, tranLayerTranTop, tranLayerPMOS, tranLayerPMOSCircle};
		tranLayersD = new Technology.NodeLayer [] {tranLayerMOS, tranLayerTranTop, tranLayerNMOS, tranLayerDMOS};
		tranLayersNPN = new Technology.NodeLayer [] {tranLayerBTran1, tranLayerTranTop, tranLayerNMOS, tranLayerBTran2};
		tranLayersPNP = new Technology.NodeLayer [] {tranLayerBTran1, tranLayerTranTop, tranLayerNMOS, tranLayerBTran3};
		tranLayersNJFET = new Technology.NodeLayer [] {tranLayerBTran4, tranLayerTranTop, tranLayerNMOS, tranLayerBTran5};
		tranLayersPJFET = new Technology.NodeLayer [] {tranLayerBTran4, tranLayerTranTop, tranLayerNMOS, tranLayerBTran6};
		tranLayersDMES = new Technology.NodeLayer [] {tranLayerBTran4, tranLayerTranTop, tranLayerNMOS};
		tranLayersEMES = new Technology.NodeLayer [] {tranLayerBTran7, tranLayerNMOS};
		transistorNode = PrimitiveNode.newInstance("Transistor", this, 4.0, 4.0, new SizeOffset(0, 0, 0, 1), tranLayersN);
		transistorNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, transistorNode, new ArcProto[] {wire_arc}, "g", 0,180, 0, PortProto.Characteristic.IN,
					EdgeH.CENTER, EdgeV.fromTop(1), EdgeH.CENTER, EdgeV.fromTop(1)),
				PrimitivePort.newInstance(this, transistorNode, new ArcProto[] {wire_arc}, "s", 180,90, 1, PortProto.Characteristic.BIDIR,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE),
				PrimitivePort.newInstance(this, transistorNode, new ArcProto[] {wire_arc}, "d", 0,90, 2, PortProto.Characteristic.BIDIR,
					EdgeH.RIGHTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.BOTTOMEDGE)
			});
		transistorNode.setFunction(NodeProto.Function.TRANS);

		/** resistor */
		resistorNode = PrimitiveNode.newInstance("Resistor", this, 2.0, 1.0, new SizeOffset(1, 1, 0, 0),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(LEFTBYP66, EdgeV.CENTER),
					new Technology.TechPoint(LEFTBYP6, EdgeV.CENTER),
					new Technology.TechPoint(LEFTBYP5, EdgeV.TOPEDGE),
					new Technology.TechPoint(LEFTBYP3, EdgeV.BOTTOMEDGE),
					new Technology.TechPoint(LEFTBYP1, EdgeV.TOPEDGE),
					new Technology.TechPoint(RIGHTBYP1, EdgeV.BOTTOMEDGE),
					new Technology.TechPoint(RIGHTBYP3, EdgeV.TOPEDGE),
					new Technology.TechPoint(RIGHTBYP5, EdgeV.BOTTOMEDGE),
					new Technology.TechPoint(RIGHTBYP6, EdgeV.CENTER),
					new Technology.TechPoint(RIGHTBYP66, EdgeV.CENTER)})
			});
		resistorNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, resistorNode, new ArcProto[] {wire_arc}, "a", 180,90, 0, PortProto.Characteristic.UNKNOWN,
					LEFTBYP66, EdgeV.CENTER, LEFTBYP66, EdgeV.CENTER),
				PrimitivePort.newInstance(this, resistorNode, new ArcProto[] {wire_arc}, "b", 0,90, 1, PortProto.Characteristic.UNKNOWN,
					RIGHTBYP66, EdgeV.CENTER, RIGHTBYP66, EdgeV.CENTER)
			});
		resistorNode.setFunction(NodeProto.Function.RESIST);

		/** capacitor */
		Technology.NodeLayer capacitorLayer = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, TOPBYP2),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, TOPBYP2),
			new Technology.TechPoint(EdgeH.LEFTEDGE, BOTBYP2),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, BOTBYP2),
			new Technology.TechPoint(EdgeH.CENTER, TOPBYP2),
			new Technology.TechPoint(EdgeH.CENTER, EdgeV.TOPEDGE),
			new Technology.TechPoint(EdgeH.CENTER, BOTBYP2),
			new Technology.TechPoint(EdgeH.CENTER, EdgeV.BOTTOMEDGE)});
		Technology.NodeLayer capacitorLayerEl = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(RIGHTBYP2, BOTBYP6),
			new Technology.TechPoint(RIGHTBYP6, BOTBYP6),
			new Technology.TechPoint(RIGHTBYP4, BOTBYP4),
			new Technology.TechPoint(RIGHTBYP4, BOTBYP8)});
		capacitorLayersNorm = new Technology.NodeLayer [] {capacitorLayer};
		capacitorLayersElectrolytic = new Technology.NodeLayer [] {capacitorLayer, capacitorLayerEl};
		capacitorNode = PrimitiveNode.newInstance("Capacitor", this, 3.0, 4.0, null, capacitorLayersNorm);
		capacitorNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, capacitorNode, new ArcProto[] {wire_arc}, "a", 90,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.TOPEDGE, EdgeH.CENTER, EdgeV.TOPEDGE),
				PrimitivePort.newInstance(this, capacitorNode, new ArcProto[] {wire_arc}, "b", 270,90, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.BOTTOMEDGE, EdgeH.CENTER, EdgeV.BOTTOMEDGE)
			});
		capacitorNode.setFunction(NodeProto.Function.CAPAC);

		/** diode */
		Technology.NodeLayer diodeLayer1 = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, TOPBYP5),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, TOPBYP5),
			new Technology.TechPoint(EdgeH.CENTER, TOPBYP5),
			new Technology.TechPoint(EdgeH.CENTER, EdgeV.TOPEDGE),
			new Technology.TechPoint(EdgeH.CENTER, BOTBYP5),
			new Technology.TechPoint(EdgeH.CENTER, EdgeV.BOTTOMEDGE)});
		Technology.NodeLayer diodeLayer2 = new Technology.NodeLayer(node_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, BOTBYP5),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, BOTBYP5),
			new Technology.TechPoint(EdgeH.CENTER, TOPBYP5)});
		Technology.NodeLayer diodeLayer3 = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, TOPBYP75),
			new Technology.TechPoint(EdgeH.LEFTEDGE, TOPBYP5),
			new Technology.TechPoint(EdgeH.LEFTEDGE, TOPBYP5),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, TOPBYP5),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, TOPBYP5),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, TOPBYP25),
			new Technology.TechPoint(EdgeH.CENTER, TOPBYP5),
			new Technology.TechPoint(EdgeH.CENTER, EdgeV.TOPEDGE),
			new Technology.TechPoint(EdgeH.CENTER, BOTBYP5),
			new Technology.TechPoint(EdgeH.CENTER, EdgeV.BOTTOMEDGE)});
		diodeLayersNorm = new Technology.NodeLayer [] {diodeLayer1, diodeLayer2};
		diodeLayersZener = new Technology.NodeLayer [] {diodeLayer3, diodeLayer2};
		diodeNode = PrimitiveNode.newInstance("Diode", this, 2.0, 4.0, null,diodeLayersNorm);
		diodeNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, diodeNode, new ArcProto[] {wire_arc}, "a", 90,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.TOPEDGE, EdgeH.CENTER, EdgeV.TOPEDGE),
				PrimitivePort.newInstance(this, diodeNode, new ArcProto[] {wire_arc}, "b", 270,90, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.BOTTOMEDGE, EdgeH.CENTER, EdgeV.BOTTOMEDGE)
			});
		diodeNode.setFunction(NodeProto.Function.DIODE);

		/** inductor */
		inductorNode = PrimitiveNode.newInstance("Inductor", this, 2.0, 4.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.TOPEDGE),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.BOTTOMEDGE)}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(LEFTBYP5, TOPBYP33),
					new Technology.TechPoint(EdgeH.CENTER, TOPBYP33)}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(LEFTBYP5, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER)}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(LEFTBYP5, BOTBYP33),
					new Technology.TechPoint(EdgeH.CENTER, BOTBYP33)})
			});
		inductorNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, inductorNode, new ArcProto[] {wire_arc}, "a", 90,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.TOPEDGE, EdgeH.CENTER, EdgeV.TOPEDGE),
				PrimitivePort.newInstance(this, inductorNode, new ArcProto[] {wire_arc}, "b", 270,90, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.BOTTOMEDGE, EdgeH.CENTER, EdgeV.BOTTOMEDGE)
			});
		inductorNode.setFunction(NodeProto.Function.INDUCT);

		/** meter */
		Technology.NodeLayer meterLetterV = new Technology.NodeLayer(node_lay, 0, Poly.Type.TEXTBOX, Technology.NodeLayer.POINTS, Technology.TechPoint.FULLBOX);
		meterLetterV.setMessage("V");
		meterLetterV.setDescriptor(tdBig);
		meterNode = PrimitiveNode.newInstance("Meter", this, 6.0, 6.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.CENTER)}),
				meterLetterV
			});
		meterNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, meterNode, new ArcProto[] {wire_arc}, "a", 90,0, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.TOPEDGE, EdgeH.CENTER, EdgeV.TOPEDGE),
				PrimitivePort.newInstance(this, meterNode, new ArcProto[] {wire_arc}, "b", 270,0, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.BOTTOMEDGE, EdgeH.CENTER, EdgeV.BOTTOMEDGE)
			});
		meterNode.setFunction(NodeProto.Function.METER);
		meterNode.isSquare();

		/** well contact */
		wellNode = PrimitiveNode.newInstance("Well", this, 4.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE),
					new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.BOTTOMEDGE),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.TOPEDGE),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.BOTTOMEDGE)})
			});
		wellNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, wellNode, new ArcProto[] {wire_arc}, "well", 90,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.TOPEDGE, EdgeH.CENTER, EdgeV.TOPEDGE)
			});
		wellNode.setFunction(NodeProto.Function.WELL);

		/** substrate contact */
		substrateNode = PrimitiveNode.newInstance("Substrate", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.TOPEDGE),
					new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.BOTTOMEDGE),
					new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.BOTTOMEDGE)})
			});
		substrateNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, substrateNode, new ArcProto[] {wire_arc}, "substrate", 90,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.TOPEDGE, EdgeH.CENTER, EdgeV.TOPEDGE)
			});
		substrateNode.setFunction(NodeProto.Function.SUBSTRATE);

		/** two-port */
		Technology.NodeLayer twoLayerBox = new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP8, EdgeV.TOPEDGE),
			new Technology.TechPoint(RIGHTBYP8, EdgeV.BOTTOMEDGE)});
		Technology.NodeLayer twoLayerNormWire = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, TOPBYP66),
			new Technology.TechPoint(LEFTBYP6, TOPBYP66),
			new Technology.TechPoint(EdgeH.LEFTEDGE, BOTBYP66),
			new Technology.TechPoint(LEFTBYP6, BOTBYP66),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, TOPBYP66),
			new Technology.TechPoint(RIGHTBYP6, TOPBYP66),
			new Technology.TechPoint(RIGHTBYP6, TOPBYP66),
			new Technology.TechPoint(RIGHTBYP6, TOPBYP3),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, BOTBYP66),
			new Technology.TechPoint(RIGHTBYP6, BOTBYP66),
			new Technology.TechPoint(RIGHTBYP6, BOTBYP66),
			new Technology.TechPoint(RIGHTBYP6, BOTBYP3)});
		Technology.NodeLayer twoLayerVSC = new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(RIGHTBYP6, EdgeV.CENTER),
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
			new Technology.TechPoint(EdgeH.LEFTEDGE, TOPBYP66),
			new Technology.TechPoint(LEFTBYP8, TOPBYP66),
			new Technology.TechPoint(EdgeH.LEFTEDGE, BOTBYP66),
			new Technology.TechPoint(LEFTBYP8, BOTBYP66),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, TOPBYP66),
			new Technology.TechPoint(RIGHTBYP8, TOPBYP66),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, BOTBYP66),
			new Technology.TechPoint(RIGHTBYP8, BOTBYP66)});
		Technology.NodeLayer twoLayerCSWire = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(RIGHTBYP6, TOPBYP3),
			new Technology.TechPoint(RIGHTBYP45, EdgeV.CENTER),
			new Technology.TechPoint(RIGHTBYP45, EdgeV.CENTER),
			new Technology.TechPoint(RIGHTBYP6, BOTBYP3),
			new Technology.TechPoint(RIGHTBYP6, BOTBYP3),
			new Technology.TechPoint(RIGHTBYP75, EdgeV.CENTER),
			new Technology.TechPoint(RIGHTBYP75, EdgeV.CENTER),
			new Technology.TechPoint(RIGHTBYP6, TOPBYP3)});
		Technology.NodeLayer twoLayerCCWire = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENEDT1, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP6, TOPBYP66),
			new Technology.TechPoint(LEFTBYP6, BOTBYP66)});
		Technology.NodeLayer twoLayerTrBox = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP8, EdgeV.TOPEDGE),
			new Technology.TechPoint(RIGHTBYP8, EdgeV.TOPEDGE),
			new Technology.TechPoint(LEFTBYP8, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(RIGHTBYP8, EdgeV.BOTTOMEDGE)});
		Technology.NodeLayer twoLayerTr1 = new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
			new Technology.TechPoint(LEFTBYP8, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP8, EdgeV.TOPEDGE)});
		Technology.NodeLayer twoLayerTr2 = new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBY1P6, EdgeV.CENTER),
			new Technology.TechPoint(LEFTBYP8, EdgeV.TOPEDGE),
			new Technology.TechPoint(LEFTBYP8, EdgeV.BOTTOMEDGE)});
		Technology.NodeLayer twoLayerTr3 = new Technology.NodeLayer(node_lay, 0, Poly.Type.CIRCLEARC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
			new Technology.TechPoint(RIGHTBYP8, EdgeV.TOPEDGE),
			new Technology.TechPoint(RIGHTBYP8, EdgeV.BOTTOMEDGE)});
		Technology.NodeLayer twoLayerTrWire = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, TOPBYP66),
			new Technology.TechPoint(LEFTBYP8, TOPBYP66),
			new Technology.TechPoint(EdgeH.LEFTEDGE, BOTBYP66),
			new Technology.TechPoint(LEFTBYP8, BOTBYP66),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, TOPBYP66),
			new Technology.TechPoint(RIGHTBYP9, TOPBYP66),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, BOTBYP66),
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
		twoportNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, twoportNode, new ArcProto[] {wire_arc}, "a", 180,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, TOPBYP66, EdgeH.LEFTEDGE, TOPBYP66),
				PrimitivePort.newInstance(this, twoportNode, new ArcProto[] {wire_arc}, "b", 180,90, 1, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, BOTBYP66, EdgeH.LEFTEDGE, BOTBYP66),
				PrimitivePort.newInstance(this, twoportNode, new ArcProto[] {wire_arc}, "x", 0,90, 2, PortProto.Characteristic.UNKNOWN,
					EdgeH.RIGHTEDGE, TOPBYP66, EdgeH.RIGHTEDGE, TOPBYP66),
				PrimitivePort.newInstance(this, twoportNode, new ArcProto[] {wire_arc}, "y", 0,90, 3, PortProto.Characteristic.UNKNOWN,
					EdgeH.RIGHTEDGE, BOTBYP66, EdgeH.RIGHTEDGE, BOTBYP66)
			});
		twoportNode.setFunction(NodeProto.Function.TLINE);

		/** 4-port transistor */
		Technology.NodeLayer tranLayerNMOS4 = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP5, BOTBYP5),
			new Technology.TechPoint(LEFTBYP5, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP5, BOTBYP5),
			new Technology.TechPoint(LEFTBYP35, BOTBYP75),
			new Technology.TechPoint(LEFTBYP5, BOTBYP5),
			new Technology.TechPoint(LEFTBYP66, BOTBYP75)});
		Technology.NodeLayer tranLayerPMOS4 = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP5, BOTBYP5),
			new Technology.TechPoint(LEFTBYP5, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP5, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP35, BOTBYP75),
			new Technology.TechPoint(LEFTBYP5, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP66, BOTBYP75)});
		Technology.NodeLayer tranLayerDMOS4 = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP5, BOTBYP75),
			new Technology.TechPoint(LEFTBYP5, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP5, BOTBYP75),
			new Technology.TechPoint(LEFTBYP35, BOTBYP9),
			new Technology.TechPoint(LEFTBYP5, BOTBYP75),
			new Technology.TechPoint(LEFTBYP66, BOTBYP9)});
		Technology.NodeLayer tranLayerBIP4 = new Technology.NodeLayer(node_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP5, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(EdgeH.CENTER, BOTBYP25)});
		Technology.NodeLayer tranLayerPMES4 = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP5, BOTBYP25),
			new Technology.TechPoint(LEFTBYP5, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP5, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP35, BOTBYP75),
			new Technology.TechPoint(LEFTBYP5, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP66, BOTBYP75)});
		Technology.NodeLayer tranLayerNMES4 = new Technology.NodeLayer(node_lay, 0, Poly.Type.VECTORS, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
			new Technology.TechPoint(LEFTBYP5, BOTBYP25),
			new Technology.TechPoint(LEFTBYP5, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(LEFTBYP5, BOTBYP25),
			new Technology.TechPoint(LEFTBYP35, BOTBYP5),
			new Technology.TechPoint(LEFTBYP5, BOTBYP25),
			new Technology.TechPoint(LEFTBYP66, BOTBYP5)});
		tran4LayersN = new Technology.NodeLayer [] {tranLayerMOS, tranLayerTranTop, tranLayerNMOS, tranLayerNMOS4};
		tran4LayersP = new Technology.NodeLayer [] {tranLayerMOS, tranLayerTranTop, tranLayerPMOS, tranLayerPMOSCircle, tranLayerPMOS4};
		tran4LayersD = new Technology.NodeLayer [] {tranLayerMOS, tranLayerTranTop, tranLayerNMOS, tranLayerDMOS, tranLayerDMOS4};
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
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.TOPEDGE),
					new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.BOTTOMEDGE),
					new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.BOTTOMEDGE)})
			});
		transistor4Node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, transistor4Node, new ArcProto[] {wire_arc}, "g", 0,180, 0, PortProto.Characteristic.IN,
					EdgeH.CENTER, EdgeV.fromTop(1), EdgeH.CENTER, EdgeV.fromTop(1)),
				PrimitivePort.newInstance(this, transistor4Node, new ArcProto[] {wire_arc}, "s", 180,90, 1, PortProto.Characteristic.BIDIR,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE),
				PrimitivePort.newInstance(this, transistor4Node, new ArcProto[] {wire_arc}, "d", 0,90, 2, PortProto.Characteristic.BIDIR,
					EdgeH.RIGHTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.BOTTOMEDGE),
				PrimitivePort.newInstance(this, transistor4Node, new ArcProto[] {wire_arc}, "b", 270,90, 3, PortProto.Characteristic.BIDIR,
					LEFTBYP5, EdgeV.BOTTOMEDGE, LEFTBYP5, EdgeV.BOTTOMEDGE)
			});
		transistor4Node.setFunction(NodeProto.Function.TRANS4);

		/** global signal */
		globalNode = PrimitiveNode.newInstance("Global-Signal", this, 3.0, 3.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.TOPEDGE),
					new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.BOTTOMEDGE)}),
				new Technology.NodeLayer(node_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(LEFTBYP9, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, TOPBYP9),
					new Technology.TechPoint(RIGHTBYP9, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, BOTBYP9)})
			});
		globalNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, globalNode, new ArcProto[] {wire_arc}, "global", 270,90, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.BOTTOMEDGE, EdgeH.CENTER, EdgeV.BOTTOMEDGE)
			});
		globalNode.setFunction(NodeProto.Function.CONNECT);
	}

	//**************************************** ROUTINES ****************************************

	/**
	 * Routine to return a list of Polys that describe a given NodeInst.
	 * This routine overrides the general one in the Technology object
	 * because of the unusual primitives in the Artwork Technology.
	 * @param ni the NodeInst to describe.
	 * @param wnd the window in which this node will be drawn.
	 * @return an array of Poly objects.
	 */
	public Poly [] getShapeOfNode(NodeInst ni, EditWindow wnd)
	{
		NodeProto prototype = ni.getProto();
		if (!(prototype instanceof PrimitiveNode)) return null;

		// see if the node is "wiped" (not drawn)
		if (ni.isWiped()) return null;

		PrimitiveNode np = (PrimitiveNode)prototype;
		Technology.NodeLayer [] primLayers = np.getLayers();
		if (np == wirePinNode)
		{
			if (ni.pinUseCount()) return null;
		} else if (np == busPinNode)
		{
			// bus pins get bigger in "T" configurations, disappear when alone and exported
			int busCon = 0, nonBusCon = 0;
			for(Iterator it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				if (con.getArc().getProto() == bus_arc) busCon++; else
					nonBusCon++;
			}
			int implicitCon = 0;
			if (busCon == 0 && nonBusCon == 0) implicitCon = 1;

//			/* if the next level up the hierarchy is visible, consider arcs connected there */
//			if (win != NOWINDOWPART && ni->firstportexpinst != NOPORTEXPINST)
//			{
//				db_gettraversalpath(ni->parent, NOWINDOWPART, &nilist, &depth);
//				if (depth == 1)
//				{
//					upni = nilist[0];
//					if (upni->proto == ni->parent && upni->parent == win->curnodeproto)
//					{
//						for(pe = ni->firstportexpinst; pe != NOPORTEXPINST; pe = pe->nextportexpinst)
//						{
//							for (pi = upni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//							{
//								if (pi->proto != pe->exportproto) continue;
//								if (pi->conarcinst->proto == sch_busarc) busCon++; else
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
						if (ni.getNumExports() != 0)
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
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.fromCenter(busDiscSize))});
			}
			if (wireDiscSize > 0)
			{
				busPinLayers[totalLayers++] = new Technology.NodeLayer(arc_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.fromCenter(wireDiscSize))});
			}
			primLayers = busPinLayers;
		} else if (np == flipflopNode)
		{
			int ffBits = ni.getTechSpecific();
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
		} else if (np == transistorNode)
		{
			int tranBits = ni.getTechSpecific();
			switch (tranBits)
			{
				case TRANNMOS:  primLayers = tranLayersN;      break;
				case TRANDMOS:  primLayers = tranLayersD;      break;
				case TRANPMOS:  primLayers = tranLayersP;      break;
				case TRANNPN:   primLayers = tranLayersNPN;    break;
				case TRANPNP:   primLayers = tranLayersPNP;    break;
				case TRANNJFET: primLayers = tranLayersNJFET;  break;
				case TRANPJFET: primLayers = tranLayersPJFET;  break;
				case TRANDMES:  primLayers = tranLayersDMES;   break;
				case TRANEMES:  primLayers = tranLayersEMES;   break;
			}
		} else if (np == twoportNode)
		{
			int tranBits = ni.getTechSpecific();
			switch (tranBits)
			{
				case TWOPVCCS:  primLayers = twoLayersVCCS;   break;
				case TWOPCCVS:  primLayers = twoLayersCCVS;   break;
				case TWOPVCVS:  primLayers = twoLayersVCVS;   break;
				case TWOPCCCS:  primLayers = twoLayersCCCS;   break;
				case TWOPTLINE: primLayers = twoLayersTran;   break;
			}
		} else if (np == diodeNode)
		{
			int diodeBits = ni.getTechSpecific();
			switch (diodeBits)
			{
				case DIODENORM:  primLayers = diodeLayersNorm;    break;
				case DIODEZENER: primLayers = diodeLayersZener;   break;
			}
		} else if (np == capacitorNode)
		{
			int capacitorBits = ni.getTechSpecific();
			switch (capacitorBits)
			{
				case CAPACNORM: primLayers = capacitorLayersNorm;           break;
				case CAPACELEC: primLayers = capacitorLayersElectrolytic;   break;
			}
		} else if (np == switchNode)
		{
			int numLayers = 3;
			if (ni.getYSize() >= 4) numLayers += ((int)ni.getYSize()/2) - 1; 
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
		} else if (np == transistor4Node)
		{
			int tranBits = ni.getTechSpecific();
			switch (tranBits)
			{
				case TRANNMOS:  primLayers = tran4LayersN;      break;
				case TRANDMOS:  primLayers = tran4LayersD;      break;
				case TRANPMOS:  primLayers = tran4LayersP;      break;
				case TRANNPN:   primLayers = tran4LayersNPN;    break;
				case TRANPNP:   primLayers = tran4LayersPNP;    break;
				case TRANNJFET: primLayers = tran4LayersNJFET;  break;
				case TRANPJFET: primLayers = tran4LayersPJFET;  break;
				case TRANDMES:  primLayers = tran4LayersDMES;   break;
				case TRANEMES:  primLayers = tran4LayersEMES;   break;
			}
		}
		return getShapeOfNode(ni, wnd, primLayers);
	}

	/**
	 * Routine to return a list of Polys that describe a given ArcInst.
	 * This routine overrides the general one in the Technology object
	 * because of the unusual primitives in the Artwork Technology.
	 * @param ai the ArcInst to describe.
	 * @param wnd the window in which this arc will be drawn.
	 * @return an array of Poly objects.
	 */
	public Poly [] getShapeOfArc(ArcInst ai, EditWindow wnd)
	{
		// Bus arcs are handled in a standard way
		PrimitiveArc ap = (PrimitiveArc)ai.getProto();
		if (ap == bus_arc)
			return super.getShapeOfArc(ai, wnd);

		// Wire arc: if not negated, handle in a standard way
		if (!ai.isNegated() || ai.isSkipTail())
			return super.getShapeOfArc(ai, wnd);

		// draw a negated Wire arc
		Point2D headLoc = ai.getHead().getLocation();
		Point2D tailLoc = ai.getTail().getLocation();
		double headX = headLoc.getX();   double headY = headLoc.getY();
		double tailX = tailLoc.getX();   double tailY = tailLoc.getY();
		int angle = ai.getAngle();
		double bubbleSize = 1.2;
		double cosDist = EMath.cos(angle) * bubbleSize;
		double sinDist = EMath.sin(angle) * bubbleSize;
		double bubbleX, bubbleY;
		Point2D bubbleEdge;
		if (ai.isReverseEnds())
		{
			bubbleX = headX + cosDist/2;
			bubbleY = headY + sinDist/2;
			bubbleEdge = headLoc;
			headX += cosDist;
			headY += sinDist;
		} else
		{
			bubbleX = tailX - cosDist/2;
			bubbleY = tailY - sinDist/2;
			bubbleEdge = tailLoc;
			tailX -= cosDist;
			tailY -= sinDist;
		}

		Poly [] polys = new Poly[2];
		Point2D newHead = new Point2D.Double(headX, headY);
		Point2D newTail = new Point2D.Double(tailX, tailY);
		Point2D newBubble = new Point2D.Double(bubbleX, bubbleY);

		Point2D [] points = new Point2D.Double[2];
		points[0] = newHead;
		points[1] = newTail;
		polys[0] = new Poly(points);
		polys[0].setStyle(Poly.Type.OPENED);
		polys[0].setLayer(arc_lay);

		points = new Point2D.Double[2];
		points[0] = newBubble;
		points[1] = bubbleEdge;
		polys[1] = new Poly(points);
		polys[1].setStyle(Poly.Type.CIRCLE);
		polys[1].setLayer(arc_lay);

		// construct the polygons
		return polys;
	}

	/**
	 * Routine to convert old primitive port names to their proper PortProtos.
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
			if (portName.equals("upperleft"))getIndexedPort(0, np);
			if (portName.equals("lowerleft")) getIndexedPort(1, np);
			if (portName.equals("upperright")) getIndexedPort(2, np);
			if (portName.equals("lowerright")) getIndexedPort(3, np);
		}

		return super.convertOldPortName(portName,np);
	}

	private PrimitivePort getIndexedPort(int index, PrimitiveNode np)
	{
		for(Iterator it = np.getPorts(); it.hasNext(); )
		{
			PrimitivePort pp = (PrimitivePort)it.next();
			if (index == 0) return pp;
			index--;
		}
		return null;
	}

	/**
	 * Routine to return the pure "NodeProto Function" a primitive NodeInst in this Technology.
	 * The Schematics technology allows primitives to have parameterized functions.
	 * @param ni the NodeInst to check.
	 * @return the NodeProto.Function that describes the NodeInst.
	 */
	public NodeProto.Function getPrimitiveFunction(NodeInst ni)
	{
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		int techBits = ni.getTechSpecific();
		if (np == capacitorNode)
		{
			if (techBits == CAPACELEC) return NodeProto.Function.ECAPAC;
			return NodeProto.Function.CAPAC;
		}
		if (np == diodeNode)
		{
			if (techBits == DIODEZENER) return NodeProto.Function.DIODEZ;
			return NodeProto.Function.DIODE;
		}
		if (np == transistorNode)
		{
			switch (techBits)
			{
				case TRANNMOS:  return NodeProto.Function.TRANMOS;
				case TRANDMOS:  return NodeProto.Function.TRADMOS;
				case TRANPMOS:  return NodeProto.Function.TRAPMOS;
				case TRANNPN:   return NodeProto.Function.TRANPN;
				case TRANPNP:   return NodeProto.Function.TRAPNP;
				case TRANNJFET: return NodeProto.Function.TRANJFET;
				case TRANPJFET: return NodeProto.Function.TRAPJFET;
				case TRANDMES:  return NodeProto.Function.TRADMES;
				case TRANEMES:  return NodeProto.Function.TRAEMES;
			}
			return NodeProto.Function.TRANMOS;
		}
		if (np == transistor4Node)
		{
			switch (techBits)
			{
				case TRANNMOS:  return NodeProto.Function.TRA4NMOS;
				case TRANDMOS:  return NodeProto.Function.TRA4DMOS;
				case TRANPMOS:  return NodeProto.Function.TRA4PMOS;
				case TRANNPN:   return NodeProto.Function.TRA4NPN;
				case TRANPNP:   return NodeProto.Function.TRA4PNP;
				case TRANNJFET: return NodeProto.Function.TRA4NJFET;
				case TRANPJFET: return NodeProto.Function.TRA4PJFET;
				case TRANDMES:  return NodeProto.Function.TRA4DMES;
				case TRANEMES:  return NodeProto.Function.TRA4EMES;
			}
			return NodeProto.Function.TRA4NMOS;
		}
		if (np == twoportNode)
		{
			switch (techBits)
			{
				case TWOPVCCS:  return NodeProto.Function.VCCS;
				case TWOPCCVS:  return NodeProto.Function.CCVS;
				case TWOPVCVS:  return NodeProto.Function.VCVS;
				case TWOPCCCS:   return NodeProto.Function.CCCS;
			}
			return NodeProto.Function.TLINE;
		}
		return ni.getProto().getFunction();
	}

	/**
	 * Routine to set the pure "NodeProto Function" for a primitive NodeInst in this Technology.
	 * This routine is overridden by technologies (such as Schematics) that can change a node's function.
	 * @param ni the NodeInst to check.
	 * @param function the NodeProto.Function to set on the NodeInst.
	 */
	public void setPrimitiveFunction(NodeInst ni, NodeProto.Function function)
	{
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		if (np == capacitorNode)
		{
			if (function == NodeProto.Function.ECAPAC) ni.setTechSpecific(CAPACELEC); else
				ni.setTechSpecific(CAPACNORM);
		}
		if (np == diodeNode)
		{
			if (function == NodeProto.Function.DIODEZ) ni.setTechSpecific(DIODEZENER); else
				ni.setTechSpecific(DIODENORM);
		}
		if (np == transistorNode)
		{
			if (function == NodeProto.Function.TRANMOS) ni.setTechSpecific(TRANNMOS); else
			if (function == NodeProto.Function.TRADMOS) ni.setTechSpecific(TRANDMOS); else
			if (function == NodeProto.Function.TRAPMOS) ni.setTechSpecific(TRANPMOS); else
			if (function == NodeProto.Function.TRANPN) ni.setTechSpecific(TRANNPN); else
			if (function == NodeProto.Function.TRAPNP) ni.setTechSpecific(TRANPNP); else
			if (function == NodeProto.Function.TRANJFET) ni.setTechSpecific(TRANNJFET); else
			if (function == NodeProto.Function.TRAPJFET) ni.setTechSpecific(TRANPJFET); else
			if (function == NodeProto.Function.TRADMES) ni.setTechSpecific(TRANDMES); else
			if (function == NodeProto.Function.TRAEMES) ni.setTechSpecific(TRANEMES);
		}
		if (np == transistor4Node)
		{
			if (function == NodeProto.Function.TRA4NMOS) ni.setTechSpecific(TRANNMOS); else
			if (function == NodeProto.Function.TRA4DMOS) ni.setTechSpecific(TRANDMOS); else
			if (function == NodeProto.Function.TRA4PMOS) ni.setTechSpecific(TRANPMOS); else
			if (function == NodeProto.Function.TRA4NPN) ni.setTechSpecific(TRANNPN); else
			if (function == NodeProto.Function.TRA4PNP) ni.setTechSpecific(TRANPNP); else
			if (function == NodeProto.Function.TRA4NJFET) ni.setTechSpecific(TRANNJFET); else
			if (function == NodeProto.Function.TRA4PJFET) ni.setTechSpecific(TRANPJFET); else
			if (function == NodeProto.Function.TRA4DMES) ni.setTechSpecific(TRANDMES); else
			if (function == NodeProto.Function.TRA4EMES) ni.setTechSpecific(TRANEMES);
		}
		if (np == twoportNode)
		{
			if (function == NodeProto.Function.VCCS) ni.setTechSpecific(TWOPVCCS); else
			if (function == NodeProto.Function.CCVS) ni.setTechSpecific(TWOPCCVS); else
			if (function == NodeProto.Function.VCVS) ni.setTechSpecific(TWOPVCVS); else
			if (function == NodeProto.Function.CCCS) ni.setTechSpecific(TWOPCCCS); else
				ni.setTechSpecific(TWOPTLINE);
		}
	}
    
	/**
	 * Routine to return the size of a transistor NodeInst in this Technology.
     * You should most likely be calling NodeInst.getTransistorSize instead of this.
	 * @param ni the NodeInst.
     * @param context the VarContext, set to VarContext.globalContext if not needed.
	 * @return the size of the NodeInst.
	 */
	public Dimension getTransistorSize(NodeInst ni, VarContext context)
	{
        double length = VarContext.objectToDouble(context.evalVar(ni.getVar("ATTR_length")), 0);
        double width = VarContext.objectToDouble(context.evalVar(ni.getVar("ATTR_width")), 0);
        Dimension dim = new Dimension();
        dim.setSize(width, length);
        return dim;
    }
    
//static CHAR *sch_node_vhdlstring[NODEPROTOCOUNT] = {
//	x_(""), x_(""), x_(""),										/* pins */
//	x_("buffer/inverter"), x_("and%ld/nand%ld"), x_("or%ld/nor%ld"), x_("xor%ld/xnor%ld"),	/* gates */
//	x_("ff"), x_("mux%ld"),										/* flipflop, mux */
//	x_(""), x_(""),												/* box/switch */
//	x_(""),														/* offpage */
//	x_(""), x_(""), x_(""),										/* pwr/gnd/source */
//	x_(""), x_(""), x_(""),										/* trans/resist/capac */
//	x_(""), x_(""),												/* diode/inductor */
//	x_(""),														/* meter */
//	x_(""), x_(""),												/* well/substrate */
//	x_(""), x_(""), x_("")										/* twoport/4-port/global */
//};

/******************** ROUTINES ********************/

//void sch_setmode(INTBIG count, CHAR *par[])
//{
//	REGISTER CHAR *pp;
//	REGISTER INTBIG l;
//
//	if (count == 0)
//	{
//		/* report size of negating bubbles */
//		ttyputmsg(M_("Diameter of negating bubbles is %s"), frtoa(sch_bubblediameter));
//		return;
//	}
//
//	l = estrlen(pp = par[0]);
//	if (namesamen(pp, x_("negating-bubble-diameter"), l) == 0)
//	{
//		/* get new negating bubble diameter */
//		if (count <= 1)
//		{
//			ttyputmsg(M_("Diameter of negating bubbles is %s"), frtoa(sch_bubblediameter));
//			return;
//		}
//		l = atofr(par[1]);
//		if (l > 0) sch_bubblediameter = l; else
//			ttyputerr(M_("Bubble diameter must be positive and nonzero"));
//		return;
//	}
//	if (namesamen(pp, x_("disable-differential-ports"), l) == 0)
//	{
//		if (sch_anddiffports != NOPORTPROTO)
//		{
//			ttyputerr(M_("Differential ports are already disabled"));
//			return;
//		}
//		sch_anddiffports = sch_andprim->firstportproto->nextportproto->nextportproto;
//		sch_andprim->firstportproto->nextportproto->nextportproto = NOPORTPROTO;
//
//		sch_ordiffports = sch_orprim->firstportproto->nextportproto->nextportproto;
//		sch_orprim->firstportproto->nextportproto->nextportproto = NOPORTPROTO;
//
//		sch_xordiffports = sch_xorprim->firstportproto->nextportproto->nextportproto;
//		sch_xorprim->firstportproto->nextportproto->nextportproto = NOPORTPROTO;
//		net_redoprim();
//		return;
//	}
//	if (namesamen(pp, x_("enable-differential-ports"), l) == 0)
//	{
//		if (sch_anddiffports == NOPORTPROTO)
//		{
//			ttyputerr(M_("Differential ports are already enabled"));
//			return;
//		}
//		sch_andprim->firstportproto->nextportproto->nextportproto = sch_anddiffports;
//		sch_orprim->firstportproto->nextportproto->nextportproto = sch_ordiffports;
//		sch_xorprim->firstportproto->nextportproto->nextportproto = sch_xordiffports;
//		sch_anddiffports = sch_ordiffports = sch_xordiffports = NOPORTPROTO;
//		net_redoprim();
//		return;
//	}
//	ttyputbadusage(x_("technology tell schematic"));
//}

//INTBIG sch_request(CHAR *command, va_list ap)
//{
//	REGISTER PORTPROTO *pp;
//
//	if (namesame(command, x_("ignoring-resistor-topology")) == 0)
//	{
//		pp = sch_resistorprim->firstportproto->nextportproto;
//		if ((pp->userbits&PORTNET) == 0) return(1);
//		return(0);
//	}
//	if (namesame(command, x_("ignore-resistor-topology")) == 0)
//	{
//		pp = sch_resistorprim->firstportproto->nextportproto;
//		pp->userbits = (pp->userbits & ~PORTNET);
//		net_redoprim();
//		return(0);
//	}
//	if (namesame(command, x_("include-resistor-topology")) == 0)
//	{
//		pp = sch_resistorprim->firstportproto->nextportproto;
//		pp->userbits = (pp->userbits & ~PORTNET) | (1 << PORTNETSH);
//		net_redoprim();
//		return(0);
//	}
//
//	if (namesame(command, x_("get-bubble-size")) == 0)
//	{
//		return(sch_bubblediameter);
//	}
//	if (namesame(command, x_("set-bubble-size")) == 0)
//	{
//		sch_bubblediameter = va_arg(ap, INTBIG);
//		return(0);
//	}
//	return(0);
//}

//INTBIG sch_nodepolys(NODEINST *ni, INTBIG *reasonable, WINDOWPART *win, POLYLOOP *pl, SCHPOLYLOOP *schpl)
//{
//	REGISTER INTBIG total, pindex, buscon, nonbuscon, hei, arcs, i, implicitcon;
//	INTBIG depth;
//	NODEINST **nilist, *upni;
//	REGISTER PORTARCINST *pi;
//	REGISTER PORTEXPINST *pe;
//	REGISTER PORTPROTO *pp;
//
//	/* get the default number of polygons and list of layers */
//	pindex = ni->proto->primindex;
//	total = sch_nodeprotos[pindex-1]->layercount;
//	schpl->layerlist = sch_nodeprotos[pindex-1]->layerlist;
//
//	schpl->extrasteinerpoint = total;
//	switch (pindex)
//	{
//		case NSWITCH:
//		case NOFFPAGE:
//		case NPWR:
//		case NGND:
//		case NSOURCE:
//		case NTRANSISTOR:
//		case NRESISTOR:
//		case NCAPACITOR:
//		case NDIODE:
//		case NINDUCTOR:
//		case NMETER:
//		case NWELL:
//		case NSUBSTRATE:
//		case NTWOPORT:
//		case NTRANSISTOR4:
//			for(i=0; i<sch_nodeprotos[pindex-1]->portcount; i++)
//			{
//				pp = sch_nodeprotos[pindex-1]->portlist[i].addr;
//				arcs = 0;
//				for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//					if (pi->proto == pp) arcs++;
//				if (arcs > 1)
//				{
//					schpl->extrasteinerport[total - schpl->extrasteinerpoint] = &sch_nodeprotos[pindex-1]->portlist[i];
//					total++;
//				}
//			}
//			break;
//	}
//
//	/* add in displayable variables */
//	pl->realpolys = total;
//	total += tech_displayablenvars(ni, pl->curwindowpart, pl);
//	if (reasonable != 0) *reasonable = total;
//	return(total);
//}

//void sch_shapenodepoly(NODEINST *ni, INTBIG box, POLYGON *poly, POLYLOOP *pl, SCHPOLYLOOP *schpl)
//{
//	if (box >= schpl->extrasteinerpoint)
//	{
//		/* handle extra steiner points */
//		tp = schpl->extrasteinerport[box - schpl->extrasteinerpoint];
//		if (poly->limit < 2) (void)extendpolygon(poly, 2);
//		poly->xv[0] = (getrange(ni->lowx, ni->highx, tp->lowxmul, tp->lowxsum, lambda) +
//			getrange(ni->lowx, ni->highx, tp->highxmul, tp->highxsum, lambda)) / 2;
//		poly->yv[0] = (getrange(ni->lowy, ni->highy, tp->lowymul, tp->lowysum, lambda) +
//			getrange(ni->lowy, ni->highy, tp->highymul, tp->highysum, lambda)) / 2;
//		poly->xv[1] = poly->xv[0] + sch_wirepinsizex/2;
//		poly->yv[1] = poly->yv[0];
//		poly->count = 2;
//		poly->style = DISC;
//		poly->layer = arc_lay;
//		poly->tech = sch_tech;
//		poly->desc = sch_layers[poly->layer];
//		return;
//	}
//
//	lay = &schpl->layerlist[box];
//	tech_fillpoly(poly, lay, ni, lambda, FILLED);
//	TDCLEAR(poly->textdescript);
//	TDSETSIZE(poly->textdescript, lay->portnum);
//	poly->tech = sch_tech;
//	poly->desc = sch_layers[poly->layer];
//}

//void sch_nodesizeoffset(NODEINST *ni, INTBIG *lx, INTBIG *ly, INTBIG *hx, INTBIG *hy)
//{
//	REGISTER INTBIG index, width, height, unitsize;
//
//	index = ni->proto->primindex;
//	switch (index)
//	{
//		case NAND:
//			width = ni->highx - ni->lowx;
//			height = ni->highy - ni->lowy;
//			unitsize = width / 8;
//			if (height < unitsize * 6) unitsize = height / 6;
//			*lx = 0;
//			*hx = unitsize/2;
//			*ly = *hy = 0;
//			break;
//		case NOR:
//			width = ni->highx - ni->lowx;
//			height = ni->highy - ni->lowy;
//			unitsize = width / 10;
//			if (height < unitsize * 6) unitsize = height / 6;
//			*lx = unitsize;
//			*hx = unitsize/2;
//			*ly = *hy = 0;
//			break;
//		case NXOR:
//			width = ni->highx - ni->lowx;
//			height = ni->highy - ni->lowy;
//			unitsize = width / 10;
//			if (height < unitsize * 6) unitsize = height / 6;
//			*lx = 0;
//			*hx = unitsize/2;
//			*ly = *hy = 0;
//			break;
//		default:
//			tech_nodeprotosizeoffset(ni->proto, lx, ly, hx, hy, lambdaofnode(ni));
//			break;
//	}
//}
//
//void sch_shapeportpoly(NODEINST *ni, PORTPROTO *pp, POLYGON *poly, XARRAY trans,
//	BOOLEAN purpose)
//{
//	REGISTER INTBIG pindex, i, e, total, besti, xposition, yposition, x, y, lambda,
//		wantx, wanty, bestdist, bestx, besty, dist, width, height;
//	REGISTER PORTARCINST *pi;
//	REGISTER ARCINST *ai;
//	REGISTER WINDOWPART *w;
//
//	pindex = ni->proto->primindex;
//
//	switch (ni->proto->primindex)
//	{
//		case NAND:
//			width = ni->highx - ni->lowx;
//			height = ni->highy - ni->lowy;
//			lambda = width / 8;
//			if (height < lambda * 6) lambda = height / 6;
//			break;
//		case NOR:
//		case NXOR:
//			width = ni->highx - ni->lowx;
//			height = ni->highy - ni->lowy;
//			lambda = width / 10;
//			if (height < lambda * 6) lambda = height / 6;
//			break;
//		default:
//			lambda = lambdaofnode(ni);
//			break;
//	}
//
//	/* special case for extendible primitives */
//	if (purpose && sch_nodeprotos[pindex-1]->portlist[0].addr == pp)
//	{
//		/* initialize */
//		wantx = poly->xv[0];   wanty = poly->yv[0];
//		poly->count = 1;
//		poly->style = FILLED;
//		bestdist = MAXINTBIG;
//		besti = bestx = besty = 0;
//
//		/* schematic gates must keep connections discrete and separate */
//		if (pindex == NAND || pindex == NOR || pindex == NXOR || pindex == NMUX)
//		{
//			/* determine total number of arcs already on this port */
//			for(total=0, pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//				if (pi->proto == pp) total++;
//
//			/* cycle through the arc positions */
//			total = maxi(total+2, 3);
//			for(i=0; i<total; i++)
//			{
//				/* compute the position along the left edge */
//				yposition = (i+1)/2 * WHOLE * 2;
//				if ((i&1) != 0) yposition = -yposition;
//
//				/* compute indentation (for OR and XOR) */
//				if (pindex != NMUX) xposition = -K4; else
//					xposition = -(ni->highx - ni->lowx) * 4 / 10 * WHOLE / lambda;
//				if (pindex == NOR || pindex == NXOR) switch (i)
//				{
//					case 0: xposition += T0;   break;
//					case 1:
//					case 2: xposition += H0;   break;
//				}
//
//				/* fill the polygon with that point */
//				x = getrange(ni->lowx, ni->highx, 0, xposition, lambda);
//				y = getrange(ni->lowy, ni->highy, 0, yposition, lambda);
//				xform(x, y, &poly->xv[0], &poly->yv[0], trans);
//				x = poly->xv[0];   y = poly->yv[0];
//
//				/* check for duplication */
//				for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//				{
//					ai = pi->conarcinst;
//					if (ai->end[0].portarcinst == pi) e = 0; else e = 1;
//					if (ai->end[e].xpos == x && ai->end[e].ypos == y) break;
//				}
//
//				/* if there is no duplication, this is a possible position */
//				if (pi == NOPORTARCINST)
//				{
//					dist = abs(wantx - x) + abs(wanty - y);
//					if (dist < bestdist)
//					{
//						bestdist = dist;   bestx = x;   besty = y;   besti = i;
//					}
//				}
//			}
//			if (bestdist == MAXINTBIG) ttyputerr(_("Warning: cannot find gate port"));
//
//			/* set the closest port */
//			poly->xv[0] = bestx;   poly->yv[0] = besty;
//
//			/* make sure the node is large enough */
//			if (besti*lambda*2 >= ni->highy - ni->lowy)
//			{
//				startobjectchange((INTBIG)ni, VNODEINST);
//				modifynodeinst(ni, 0, -lambda*2, 0, lambda*2, 0, 0);
//				endobjectchange((INTBIG)ni, VNODEINST);
//
//				/* make this gate change visible if it is in a window */
//				for(w = el_topwindowpart; w != NOWINDOWPART; w = w->nextwindowpart)
//					if (w->curnodeproto == ni->parent) break;
//				if (w != NOWINDOWPART) (void)asktool(us_tool, x_("flush-changes"));
//			}
//			return;
//		}
//
//		/* switches must discretize the location of connections */
//		if (pindex == NSWITCH)
//		{
//			/* cycle through the possible positions */
//			total = (ni->highy - ni->lowy) / lambda / 2;
//			for(i=0; i<total; i++)
//			{
//				yposition = i * 2 * WHOLE + K1;
//				xposition = -K2;
//				x = getrange(ni->lowx, ni->highx, 0, xposition, lambda);
//				y = getrange(ni->lowy, ni->highy, -H0, yposition, lambda);
//				xform(x, y, &poly->xv[0], &poly->yv[0], trans);
//				x = poly->xv[0];   y = poly->yv[0];
//				dist = abs(wantx - x) + abs(wanty - y);
//				if (dist < bestdist)
//				{
//					bestdist = dist;   bestx = x;   besty = y;   besti = i;
//				}
//			}
//			if (bestdist == MAXINTBIG) ttyputerr(_("Warning: cannot find switch port"));
//
//			/* set the closest port */
//			poly->xv[0] = bestx;   poly->yv[0] = besty;
//			return;
//		}
//	}
//	tech_fillportpoly(ni, pp, poly, trans, sch_nodeprotos[pindex-1], CLOSED, lambda);
//}
}
