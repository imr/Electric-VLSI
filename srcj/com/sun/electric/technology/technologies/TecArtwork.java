package com.sun.electric.technology.technologies;

/**
 * Electric(tm) VLSI Design System
 *
 * File: TecArtwork.java
 * artwork technology description
 * Generated automatically from a library
 *
 * Copyright (c) 2003 Static Free Software.
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
 *
 * Static Free Software
 * 4119 Alpine Road
 * Portola Valley, California 94028
 * info@staticfreesoft.com
 */
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;

import java.awt.Point;
import java.awt.geom.Point2D;

/**
 * This is the General Purpose Sketchpad Facility technology.
 */
public class TecArtwork extends Technology
{
	public static final TecArtwork tech = new TecArtwork();

	/** Pin */						private PrimitiveNode p_node;
	/** Box */						private PrimitiveNode b_node;
	/** Crossed-Box */				private PrimitiveNode cb_node;
	/** Filled-Box */				private PrimitiveNode fb_node;
	/** Circle */					private PrimitiveNode c_node;
	/** Filled-Circle */			private PrimitiveNode fc_node;
	/** Spline */					private PrimitiveNode s_node;
	/** Triangle */					private PrimitiveNode t_node;
	/** Filled-Triangle */			private PrimitiveNode ft_node;
	/** Arrow */					private PrimitiveNode a_node;
	/** Opened-Polygon */			private PrimitiveNode op_node;
	/** Opened-Dotted-Polygon */	private PrimitiveNode odp_node;
	/** Opened-Dashed-Polygon */	private PrimitiveNode odp0_node;
	/** Opened-Thicker-Polygon */	private PrimitiveNode otp_node;
	/** Closed-Polygon */			private PrimitiveNode cp_node;
	/** Filled-Polygon */			private PrimitiveNode fp_node;
	/** Thick-Circle */				private PrimitiveNode tc_node;

	/** Solid arc */				private PrimitiveArc Solid_arc;
	/** Dotted arc */				private PrimitiveArc Dotted_arc;
	/** Dashed arc */				private PrimitiveArc Dashed_arc;
	/** Thick arc */				private PrimitiveArc Thicker_arc;

	// -------------------- private and protected methods ------------------------
	private TecArtwork()
	{
		setTechName("artwork");
		setTechDesc("General Purpose Sketchpad Facility");

		//**************************************** LAYERS ****************************************

		/** G layer */
		Layer G_lay = Layer.newInstance("Graphics",
			new EGraphics(EGraphics.LAYERO, EGraphics.BLACK, EGraphics.SOLIDC, EGraphics.SOLIDC, 0,0,0,0.8,1,
			new int[] { 0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff,   // XXXXXXXXXXXXXXXX
						0xffff}));// XXXXXXXXXXXXXXXX

		// The layer functions
		G_lay.setFunction(Layer.Function.ART);		// Graphics

		// The CIF names
		G_lay.setCIFLayer("");		// Graphics

		// The DXF names
		G_lay.setDXFLayer("OBJECT");		// Graphics

		// The GDS names
		G_lay.setGDSLayer("1");		// Graphics

		//******************** ARCS ********************

		/** Solid arc */
		Solid_arc = PrimitiveArc.newInstance(this, "Solid", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(G_lay, 0, Poly.Type.CLOSED)
		});
		Solid_arc.setFunction(PrimitiveArc.Function.NONELEC);
		Solid_arc.setWipable();
		Solid_arc.setAngleIncrement(0);

		/** Dotted arc */
		Dotted_arc = PrimitiveArc.newInstance(this, "Dotted", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(G_lay, 0, Poly.Type.CLOSED)
		});
		Dotted_arc.setFunction(PrimitiveArc.Function.NONELEC);
		Dotted_arc.setWipable();
		Dotted_arc.setAngleIncrement(0);

		/** Dashed arc */
		Dashed_arc = PrimitiveArc.newInstance(this, "Dashed", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(G_lay, 0, Poly.Type.CLOSED)
		});
		Dashed_arc.setFunction(PrimitiveArc.Function.NONELEC);
		Dashed_arc.setWipable();
		Dashed_arc.setAngleIncrement(0);

		/** Thicker arc */
		Thicker_arc = PrimitiveArc.newInstance(this, "Thicker", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(G_lay, 0, Poly.Type.CLOSED)
		});
		Thicker_arc.setFunction(PrimitiveArc.Function.NONELEC);
		Thicker_arc.setWipable();
		Thicker_arc.setAngleIncrement(0);

		//******************** RECTANGLE DESCRIPTIONS ********************

		Technology.TechPoint [] box_1 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.LeftEdge, EdgeV.AtCenter),
			new Technology.TechPoint(EdgeH.AtCenter, EdgeV.TopEdge),
			new Technology.TechPoint(EdgeH.RightEdge, EdgeV.BottomEdge),
			new Technology.TechPoint(EdgeH.AtCenter, EdgeV.BottomEdge),
		};
		Technology.TechPoint [] box_2 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.LeftEdge, EdgeV.BottomEdge),
			new Technology.TechPoint(new EdgeH(-0.125, 0), EdgeV.TopEdge),
			new Technology.TechPoint(new EdgeH(0.125, 0), EdgeV.BottomEdge),
			new Technology.TechPoint(EdgeH.RightEdge, EdgeV.TopEdge),
		};
		Technology.TechPoint [] box_3 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.LeftEdge, EdgeV.TopEdge),
			new Technology.TechPoint(EdgeH.RightEdge, EdgeV.AtCenter),
			new Technology.TechPoint(EdgeH.LeftEdge, EdgeV.BottomEdge),
		};
		Technology.TechPoint [] box_4 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.LeftEdge, EdgeV.BottomEdge),
			new Technology.TechPoint(EdgeH.RightEdge, EdgeV.BottomEdge),
			new Technology.TechPoint(EdgeH.AtCenter, EdgeV.TopEdge),
		};
		Technology.TechPoint [] box_5 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.LeftEdge, EdgeV.BottomEdge),
			new Technology.TechPoint(new EdgeH(-0.475, 0), new EdgeV(-0.45, 0)),
			new Technology.TechPoint(new EdgeH(-0.458333, 0), new EdgeV(-0.4, 0)),
			new Technology.TechPoint(new EdgeH(-0.441667, 0), new EdgeV(-0.35, 0)),
			new Technology.TechPoint(new EdgeH(-0.425, 0), new EdgeV(-0.3, 0)),
			new Technology.TechPoint(new EdgeH(-0.4, 0), new EdgeV(-0.25, 0)),
			new Technology.TechPoint(new EdgeH(-0.383333, 0), new EdgeV(-0.208333, 0)),
			new Technology.TechPoint(new EdgeH(-0.366667, 0), new EdgeV(-0.158333, 0)),
			new Technology.TechPoint(new EdgeH(-0.35, 0), new EdgeV(-0.116667, 0)),
			new Technology.TechPoint(new EdgeH(-0.325, 0), new EdgeV(-0.075, 0)),
			new Technology.TechPoint(new EdgeH(-0.308333, 0), new EdgeV(-0.0416667, 0)),
			new Technology.TechPoint(new EdgeH(-0.291667, 0), EdgeV.AtCenter),
			new Technology.TechPoint(new EdgeH(-0.275, 0), new EdgeV(0.025, 0)),
			new Technology.TechPoint(new EdgeH(-0.258333, 0), new EdgeV(0.0583333, 0)),
			new Technology.TechPoint(new EdgeH(-0.241667, 0), new EdgeV(0.0833333, 0)),
			new Technology.TechPoint(new EdgeH(-0.225, 0), new EdgeV(0.108333, 0)),
			new Technology.TechPoint(new EdgeH(-0.208333, 0), new EdgeV(0.125, 0)),
			new Technology.TechPoint(new EdgeH(-0.191667, 0), new EdgeV(0.141667, 0)),
			new Technology.TechPoint(new EdgeH(-0.175, 0), new EdgeV(0.15, 0)),
			new Technology.TechPoint(new EdgeH(-0.158333, 0), new EdgeV(0.158333, 0)),
			new Technology.TechPoint(new EdgeH(-0.141667, 0), new EdgeV(0.166667, 0)),
			new Technology.TechPoint(new EdgeH(-0.125, 0), new EdgeV(0.158333, 0)),
			new Technology.TechPoint(new EdgeH(-0.108333, 0), new EdgeV(0.15, 0)),
			new Technology.TechPoint(new EdgeH(-0.1, 0), new EdgeV(0.141667, 0)),
			new Technology.TechPoint(new EdgeH(-0.0833333, 0), new EdgeV(0.125, 0)),
			new Technology.TechPoint(new EdgeH(-0.0666667, 0), new EdgeV(0.108333, 0)),
			new Technology.TechPoint(new EdgeH(-0.05, 0), new EdgeV(0.0916667, 0)),
			new Technology.TechPoint(new EdgeH(-0.0416667, 0), new EdgeV(0.0666667, 0)),
			new Technology.TechPoint(new EdgeH(-0.025, 0), new EdgeV(0.0416667, 0)),
			new Technology.TechPoint(new EdgeH(-0.00833333, 0), new EdgeV(0.0166667, 0)),
			new Technology.TechPoint(EdgeH.AtCenter, EdgeV.AtCenter),
			new Technology.TechPoint(new EdgeH(0.00833333, 0), new EdgeV(-0.0166667, 0)),
			new Technology.TechPoint(new EdgeH(0.025, 0), new EdgeV(-0.0416667, 0)),
			new Technology.TechPoint(new EdgeH(0.0416667, 0), new EdgeV(-0.0666667, 0)),
			new Technology.TechPoint(new EdgeH(0.05, 0), new EdgeV(-0.0916667, 0)),
			new Technology.TechPoint(new EdgeH(0.0666667, 0), new EdgeV(-0.108333, 0)),
			new Technology.TechPoint(new EdgeH(0.0833333, 0), new EdgeV(-0.125, 0)),
			new Technology.TechPoint(new EdgeH(0.1, 0), new EdgeV(-0.141667, 0)),
			new Technology.TechPoint(new EdgeH(0.108333, 0), new EdgeV(-0.15, 0)),
			new Technology.TechPoint(new EdgeH(0.125, 0), new EdgeV(-0.158333, 0)),
			new Technology.TechPoint(new EdgeH(0.141667, 0), new EdgeV(-0.166667, 0)),
			new Technology.TechPoint(new EdgeH(0.158333, 0), new EdgeV(-0.158333, 0)),
			new Technology.TechPoint(new EdgeH(0.175, 0), new EdgeV(-0.15, 0)),
			new Technology.TechPoint(new EdgeH(0.191667, 0), new EdgeV(-0.141667, 0)),
			new Technology.TechPoint(new EdgeH(0.208333, 0), new EdgeV(-0.125, 0)),
			new Technology.TechPoint(new EdgeH(0.225, 0), new EdgeV(-0.108333, 0)),
			new Technology.TechPoint(new EdgeH(0.241667, 0), new EdgeV(-0.0833333, 0)),
			new Technology.TechPoint(new EdgeH(0.258333, 0), new EdgeV(-0.0583333, 0)),
			new Technology.TechPoint(new EdgeH(0.275, 0), new EdgeV(-0.025, 0)),
			new Technology.TechPoint(new EdgeH(0.291667, 0), EdgeV.AtCenter),
			new Technology.TechPoint(new EdgeH(0.308333, 0), new EdgeV(0.0416667, 0)),
			new Technology.TechPoint(new EdgeH(0.325, 0), new EdgeV(0.075, 0)),
			new Technology.TechPoint(new EdgeH(0.35, 0), new EdgeV(0.116667, 0)),
			new Technology.TechPoint(new EdgeH(0.366667, 0), new EdgeV(0.158333, 0)),
			new Technology.TechPoint(new EdgeH(0.383333, 0), new EdgeV(0.208333, 0)),
			new Technology.TechPoint(new EdgeH(0.4, 0), new EdgeV(0.25, 0)),
			new Technology.TechPoint(new EdgeH(0.425, 0), new EdgeV(0.3, 0)),
			new Technology.TechPoint(new EdgeH(0.441667, 0), new EdgeV(0.35, 0)),
			new Technology.TechPoint(new EdgeH(0.458333, 0), new EdgeV(0.4, 0)),
			new Technology.TechPoint(new EdgeH(0.475, 0), new EdgeV(0.45, 0)),
			new Technology.TechPoint(EdgeH.RightEdge, EdgeV.TopEdge),
		};
		Technology.TechPoint [] box_6 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.AtCenter, EdgeV.AtCenter),
			new Technology.TechPoint(EdgeH.RightEdge, EdgeV.AtCenter),
		};

		//******************** NODES ********************

		/** Pin */
		p_node = PrimitiveNode.newInstance("Pin", this, 1, 1, 0, 0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_6)
			});
		p_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, p_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Function.UNKNOWN,
					EdgeH.AtCenter, EdgeV.AtCenter, EdgeH.AtCenter, EdgeV.AtCenter)
			});
		p_node.setFunction(NodeProto.Function.PIN);
		p_node.setArcsWipe();
		p_node.setArcsShrink();

		/** Box */
		b_node = PrimitiveNode.newInstance("Box", this, 6, 6, 0, 0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.CLOSEDRECT, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		b_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, b_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "box", 180,0, 0, PortProto.Function.UNKNOWN,
					EdgeH.LeftEdge, EdgeV.BottomEdge, EdgeH.RightEdge, EdgeV.TopEdge)
			});
		b_node.setFunction(NodeProto.Function.ART);

		/** Crossed-Box */
		cb_node = PrimitiveNode.newInstance("Crossed-Box", this, 6, 6, 0, 0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		cb_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, cb_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "fbox", 180,0, 0, PortProto.Function.UNKNOWN,
					EdgeH.LeftEdge, EdgeV.BottomEdge, EdgeH.RightEdge, EdgeV.TopEdge)
			});
		cb_node.setFunction(NodeProto.Function.ART);

		/** Filled-Box */
		fb_node = PrimitiveNode.newInstance("Filled-Box", this, 6, 6, 0, 0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.FILLEDRECT, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		fb_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, fb_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "fbox", 180,0, 0, PortProto.Function.UNKNOWN,
					EdgeH.LeftEdge, EdgeV.BottomEdge, EdgeH.RightEdge, EdgeV.TopEdge)
			});
		fb_node.setFunction(NodeProto.Function.ART);

		/** Circle */
		c_node = PrimitiveNode.newInstance("Circle", this, 6, 6, 0, 0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, box_6)
			});
		c_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, c_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Function.UNKNOWN,
					EdgeH.LeftEdge, EdgeV.BottomEdge, EdgeH.RightEdge, EdgeV.TopEdge)
			});
		c_node.setFunction(NodeProto.Function.ART);


		/** Filled-Circle */
		fc_node = PrimitiveNode.newInstance("Filled-Circle", this, 6, 6, 0, 0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_6)
			});
		fc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, fc_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Function.UNKNOWN,
					EdgeH.LeftEdge, EdgeV.BottomEdge, EdgeH.RightEdge, EdgeV.TopEdge)
			});
		fc_node.setFunction(NodeProto.Function.ART);
		fc_node.setSquare();

		/** Spline */
		s_node = PrimitiveNode.newInstance("Spline", this, 6, 6, 0, 0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_5)
			});
		s_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, s_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Function.UNKNOWN,
					EdgeH.LeftEdge, EdgeV.BottomEdge, EdgeH.RightEdge, EdgeV.TopEdge)
			});
		s_node.setFunction(NodeProto.Function.ART);

		/** Triangle */
		t_node = PrimitiveNode.newInstance("Triangle", this, 6, 6, 0, 0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.POINTS, box_4)
			});
		t_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, t_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "triangle", 180,0, 0, PortProto.Function.UNKNOWN,
					EdgeH.LeftEdge, EdgeV.BottomEdge, EdgeH.RightEdge, EdgeV.TopEdge)
			});
		t_node.setFunction(NodeProto.Function.ART);

		/** Filled-Triangle */
		ft_node = PrimitiveNode.newInstance("Filled-Triangle", this, 6, 6, 0, 0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.POINTS, box_4)
			});
		ft_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ft_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "ftriangle", 180,0, 0, PortProto.Function.UNKNOWN,
					EdgeH.LeftEdge, EdgeV.BottomEdge, EdgeH.RightEdge, EdgeV.TopEdge)
			});
		ft_node.setFunction(NodeProto.Function.ART);

		/** Arrow */
		a_node = PrimitiveNode.newInstance("Arrow", this, 2, 2, 0, 0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_3)
			});
		a_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, a_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "arrow", 0,180, 0, PortProto.Function.UNKNOWN,
					EdgeH.RightEdge, EdgeV.AtCenter, EdgeH.RightEdge, EdgeV.AtCenter)
			});
		a_node.setFunction(NodeProto.Function.ART);

		/** Opened-Polygon */
		op_node = PrimitiveNode.newInstance("Opened-Polygon", this, 6, 6, 0, 0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_2)
			});
		op_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, op_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Function.UNKNOWN,
					EdgeH.LeftEdge, EdgeV.BottomEdge, EdgeH.RightEdge, EdgeV.TopEdge)
			});
		op_node.setFunction(NodeProto.Function.ART);

		/** Opened-Dotted-Polygon */
		odp_node = PrimitiveNode.newInstance("Opened-Dotted-Polygon", this, 6, 6, 0, 0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.OPENEDT1, Technology.NodeLayer.POINTS, box_2)
			});
		odp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, odp_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Function.UNKNOWN,
					EdgeH.LeftEdge, EdgeV.BottomEdge, EdgeH.RightEdge, EdgeV.TopEdge)
			});
		odp_node.setFunction(NodeProto.Function.ART);

		/** Opened-Dashed-Polygon */
		odp0_node = PrimitiveNode.newInstance("Opened-Dashed-Polygon", this, 6, 6, 0, 0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.OPENEDT2, Technology.NodeLayer.POINTS, box_2)
			});
		odp0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, odp0_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Function.UNKNOWN,
					EdgeH.LeftEdge, EdgeV.BottomEdge, EdgeH.RightEdge, EdgeV.TopEdge)
			});
		odp0_node.setFunction(NodeProto.Function.ART);

		/** Opened-Thicker-Polygon */
		otp_node = PrimitiveNode.newInstance("Opened-Thicker-Polygon", this, 6, 6, 0, 0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.OPENEDT3, Technology.NodeLayer.POINTS, box_2)
			});
		otp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, otp_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Function.UNKNOWN,
					EdgeH.LeftEdge, EdgeV.BottomEdge, EdgeH.RightEdge, EdgeV.TopEdge)
			});
		otp_node.setFunction(NodeProto.Function.ART);

		/** Closed-Polygon */
		cp_node = PrimitiveNode.newInstance("Closed-Polygon", this, 6, 6, 0, 0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.MINBOX, box_1)
			});
		cp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, cp_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Function.UNKNOWN,
					EdgeH.LeftEdge, EdgeV.BottomEdge, EdgeH.RightEdge, EdgeV.TopEdge)
			});
		cp_node.setFunction(NodeProto.Function.ART);

		/** Filled-Polygon */
		fp_node = PrimitiveNode.newInstance("Filled-Polygon", this, 6, 6, 0, 0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, box_1)
			});
		fp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, fp_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Function.UNKNOWN,
					EdgeH.LeftEdge, EdgeV.BottomEdge, EdgeH.RightEdge, EdgeV.TopEdge)
			});
		fp_node.setFunction(NodeProto.Function.ART);

		/** Thick-Circle */
		tc_node = PrimitiveNode.newInstance("Thick-Circle", this, 6, 6, 0, 0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.THICKCIRCLE, Technology.NodeLayer.POINTS, box_6)
			});
		tc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, tc_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Function.UNKNOWN,
					EdgeH.LeftEdge, EdgeV.BottomEdge, EdgeH.RightEdge, EdgeV.TopEdge)
			});
		tc_node.setFunction(NodeProto.Function.ART);
	};

	/*
	 * Routine to convert old primitive node names to their proper type.
	 */
	public PrimitiveNode convertOldNodeName(String name)
	{
//		if (name == "Message" || name == "Centered-Message" ||
//			name == "Left-Message" || name == "Right-Message") return gen_invispinprim;
		if (name == "Opened-FarDotted-Polygon") return otp_node;
		return null;
	}

	/*
	 * Routine to convert old primitive arc names to their proper type.
	 */
	public PrimitiveArc convertOldArcName(String name)
	{
		if (name == "Dash-1") return Dotted_arc;
		if (name == "Dash-2") return Dashed_arc;
		if (name == "Dash-3") return Thicker_arc;
		return null;
	}
}
