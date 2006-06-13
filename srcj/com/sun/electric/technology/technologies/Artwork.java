/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Artwork.java
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
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.EditWindow0;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.*;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * This is the general purpose sketching technology.
 */
public class Artwork extends Technology
{
	/**
	 * Key of Variable holding starting and ending angles.
	 * As a special case, NodeInst.checkPossibleVariableEffects()
	 * updates the node when this variable changes.
	 */
	public static final Variable.Key ART_DEGREES = Variable.newKey("ART_degrees");
	/** key of Variable holding message text. */				public static final Variable.Key ART_MESSAGE = Variable.newKey("ART_message");
	/** key of Variable holding color information */			public static final Variable.Key ART_COLOR = Variable.newKey("ART_color");
	/** key of Variable holding color information */			public static final Variable.Key ART_PATTERN = Variable.newKey("ART_pattern");

	/** the Artwork Technology object. */			public static final Artwork tech = new Artwork();

	/** number of lines in an ellipse */			private static final int ELLIPSEPOINTS =        30;
	/** granularity of a spline */					private static final int SPLINEGRAIN   =        20;

	/** Defines a Pin node. */						public PrimitiveNode pinNode;
	/** Defines a Box node. */						public PrimitiveNode boxNode;
	/** Defines a Crossed-Box node. */				public PrimitiveNode crossedBoxNode;
	/** Defines a Filled-Box node. */				public PrimitiveNode filledBoxNode;
	/** Defines a Circle node. */					public PrimitiveNode circleNode;
	/** Defines a Filled-Circle node. */			public PrimitiveNode filledCircleNode;
	/** Defines a Spline node. */					public PrimitiveNode splineNode;
	/** Defines a Triangle node. */					public PrimitiveNode triangleNode;
	/** Defines a Filled-Triangle node. */			public PrimitiveNode filledTriangleNode;
	/** Defines a Arrow node. */					public PrimitiveNode arrowNode;
	/** Defines a Opened-Polygon node. */			public PrimitiveNode openedPolygonNode;
	/** Defines a Opened-Dotted-Polygon node. */	public PrimitiveNode openedDottedPolygonNode;
	/** Defines a Opened-Dashed-Polygon node. */	public PrimitiveNode openedDashedPolygonNode;
	/** Defines a Opened-Thicker-Polygon node. */	public PrimitiveNode openedThickerPolygonNode;
	/** Defines a Closed-Polygon node. */			public PrimitiveNode closedPolygonNode;
	/** Defines a Filled-Polygon node. */			public PrimitiveNode filledPolygonNode;
	/** Defines a Thick-Circle node. */				public PrimitiveNode thickCircleNode;

	/** Defines a Solid arc. */						public ArcProto solidArc;
	/** Defines a Dotted arc. */					public ArcProto dottedArc;
	/** Defines a Dashed arc. */					public ArcProto dashedArc;
	/** Defines a Thick arc. */						public ArcProto thickerArc;
	/** the layer */								public Layer defaultLayer;

	// -------------------- private and protected methods ------------------------
	private Artwork()
	{
		super("artwork");
		setTechShortName("Artwork");
		setTechDesc("General-purpose artwork components");
		setFactoryScale(2000, false);			// in nanometers: really 2 micron
		setNonStandard();
		setNonElectrical();
		setNoNegatedArcs();
		setStaticTechnology();

        // Foundry
        Foundry noFoundry = new Foundry(Foundry.Type.NONE);
        foundries.add(noFoundry);

		//**************************************** LAYERS ****************************************

		/** Graphics layer */
		defaultLayer = Layer.newInstance(this, "Graphics",
			new EGraphics(false, false, null, 0, 0,0,0,0.8,true,
			new int[] {0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff,
				0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff}));

		// The layer functions
		defaultLayer.setFunction(Layer.Function.ART, Layer.Function.NONELEC);		// Graphics

		// The DXF names
		defaultLayer.setFactoryDXFLayer("OBJECT");		// Graphics

		// The GDS names
        noFoundry.setFactoryGDSLayer(defaultLayer, "1");
//		defaultLayer.setFactoryGDSLayer("1", Foundry.Type.MOSIS.name());		// Graphics

		//******************** ARCS ********************

		/** Solid arc */
		solidArc = ArcProto.newInstance(this, "Solid", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(defaultLayer, 0, Poly.Type.FILLED)
		});
		solidArc.setFunction(ArcProto.Function.NONELEC);
		solidArc.setFactoryFixedAngle(false);
		solidArc.setCurvable();
		solidArc.setWipable();
		solidArc.setFactoryAngleIncrement(0);

		/** Dotted arc */
		dottedArc = ArcProto.newInstance(this, "Dotted", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(defaultLayer, 0, Poly.Type.OPENEDT1)
		});
		dottedArc.setFunction(ArcProto.Function.NONELEC);
		dottedArc.setFactoryFixedAngle(false);
		dottedArc.setCurvable();
		dottedArc.setWipable();
		dottedArc.setFactoryAngleIncrement(0);

		/** Dashed arc */
		dashedArc = ArcProto.newInstance(this, "Dashed", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(defaultLayer, 0, Poly.Type.OPENEDT2)
		});
		dashedArc.setFunction(ArcProto.Function.NONELEC);
		dashedArc.setFactoryFixedAngle(false);
		dashedArc.setCurvable();
		dashedArc.setWipable();
		dashedArc.setFactoryAngleIncrement(0);

		/** Thicker arc */
		thickerArc = ArcProto.newInstance(this, "Thicker", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(defaultLayer, 0, Poly.Type.OPENEDT3)
		});
		thickerArc.setFunction(ArcProto.Function.NONELEC);
		thickerArc.setFactoryFixedAngle(false);
		thickerArc.setCurvable();
		thickerArc.setWipable();
		thickerArc.setFactoryAngleIncrement(0);

		//******************** RECTANGLE DESCRIPTIONS ********************

		Technology.TechPoint [] box_1 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeTopEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeBottomEdge()),
		};
		Technology.TechPoint [] box_2 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(new EdgeH(-0.125, 0), EdgeV.makeTopEdge()),
			new Technology.TechPoint(new EdgeH(0.125, 0), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_4 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeTopEdge()),
		};
		Technology.TechPoint [] box_6 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
		};

		//******************** NODES ********************

		/** Pin */
		pinNode = PrimitiveNode.newInstance("Pin", this, 1, 1, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(defaultLayer, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_6)
			});
		pinNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pinNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		pinNode.setFunction(PrimitiveNode.Function.PIN);
		pinNode.setArcsWipe();
		pinNode.setArcsShrink();

		/** Box */
		boxNode = PrimitiveNode.newInstance("Box", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(defaultLayer, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		boxNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, boxNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "box", 180,0, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		boxNode.setFunction(PrimitiveNode.Function.ART);
		boxNode.setEdgeSelect();

		/** Crossed-Box */
		crossedBoxNode = PrimitiveNode.newInstance("Crossed-Box", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(defaultLayer, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		crossedBoxNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, crossedBoxNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "fbox", 180,0, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		crossedBoxNode.setFunction(PrimitiveNode.Function.ART);

		/** Filled-Box */
		filledBoxNode = PrimitiveNode.newInstance("Filled-Box", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(defaultLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		filledBoxNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, filledBoxNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "fbox", 180,0, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		filledBoxNode.setFunction(PrimitiveNode.Function.ART);
		filledBoxNode.setEdgeSelect();

		/** Circle */
		circleNode = PrimitiveNode.newInstance("Circle", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(defaultLayer, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, box_6)
			});
		circleNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, circleNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		circleNode.setFunction(PrimitiveNode.Function.ART);
		circleNode.setEdgeSelect();

		/** Filled-Circle */
		filledCircleNode = PrimitiveNode.newInstance("Filled-Circle", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(defaultLayer, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_6)
			});
		filledCircleNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, filledCircleNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		filledCircleNode.setFunction(PrimitiveNode.Function.ART);
		filledCircleNode.setSquare();
		filledCircleNode.setEdgeSelect();

		/** Spline */
		splineNode = PrimitiveNode.newInstance("Spline", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(defaultLayer, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_2)
			});
		splineNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, splineNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		splineNode.setFunction(PrimitiveNode.Function.ART);
		splineNode.setHoldsOutline();
		splineNode.setEdgeSelect();

		/** Triangle */
		triangleNode = PrimitiveNode.newInstance("Triangle", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(defaultLayer, 0, Poly.Type.CLOSED, Technology.NodeLayer.POINTS, box_4)
			});
		triangleNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, triangleNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "triangle", 180,0, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		triangleNode.setFunction(PrimitiveNode.Function.ART);
		triangleNode.setEdgeSelect();

		/** Filled-Triangle */
		filledTriangleNode = PrimitiveNode.newInstance("Filled-Triangle", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(defaultLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.POINTS, box_4)
			});
		filledTriangleNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, filledTriangleNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "ftriangle", 180,0, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		filledTriangleNode.setFunction(PrimitiveNode.Function.ART);
		filledTriangleNode.setEdgeSelect();

		/** Arrow */
		arrowNode = PrimitiveNode.newInstance("Arrow", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(defaultLayer, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS,
					new Technology.TechPoint[]
					{
						new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeTopEdge()),
						new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
						new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
					})
			});
		arrowNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, arrowNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "arrow", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeRightEdge(), EdgeV.makeCenter(), EdgeH.makeRightEdge(), EdgeV.makeCenter())
			});
		arrowNode.setFunction(PrimitiveNode.Function.ART);
		arrowNode.setEdgeSelect();

		/** Opened-Polygon */
		openedPolygonNode = PrimitiveNode.newInstance("Opened-Polygon", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(defaultLayer, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_2)
			});
		openedPolygonNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, openedPolygonNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		openedPolygonNode.setFunction(PrimitiveNode.Function.ART);
		openedPolygonNode.setHoldsOutline();
		openedPolygonNode.setEdgeSelect();

		/** Opened-Dotted-Polygon */
		openedDottedPolygonNode = PrimitiveNode.newInstance("Opened-Dotted-Polygon", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(defaultLayer, 0, Poly.Type.OPENEDT1, Technology.NodeLayer.POINTS, box_2)
			});
		openedDottedPolygonNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, openedDottedPolygonNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		openedDottedPolygonNode.setFunction(PrimitiveNode.Function.ART);
		openedDottedPolygonNode.setHoldsOutline();
		openedDottedPolygonNode.setEdgeSelect();

		/** Opened-Dashed-Polygon */
		openedDashedPolygonNode = PrimitiveNode.newInstance("Opened-Dashed-Polygon", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(defaultLayer, 0, Poly.Type.OPENEDT2, Technology.NodeLayer.POINTS, box_2)
			});
		openedDashedPolygonNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, openedDashedPolygonNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		openedDashedPolygonNode.setFunction(PrimitiveNode.Function.ART);
		openedDashedPolygonNode.setHoldsOutline();
		openedDashedPolygonNode.setEdgeSelect();

		/** Opened-Thicker-Polygon */
		openedThickerPolygonNode = PrimitiveNode.newInstance("Opened-Thicker-Polygon", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(defaultLayer, 0, Poly.Type.OPENEDT3, Technology.NodeLayer.POINTS, box_2)
			});
		openedThickerPolygonNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, openedThickerPolygonNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		openedThickerPolygonNode.setFunction(PrimitiveNode.Function.ART);
		openedThickerPolygonNode.setHoldsOutline();
		openedThickerPolygonNode.setEdgeSelect();

		/** Closed-Polygon */
		closedPolygonNode = PrimitiveNode.newInstance("Closed-Polygon", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(defaultLayer, 0, Poly.Type.CLOSED, Technology.NodeLayer.MINBOX, box_1)
			});
		closedPolygonNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, closedPolygonNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		closedPolygonNode.setFunction(PrimitiveNode.Function.ART);
		closedPolygonNode.setHoldsOutline();
		closedPolygonNode.setEdgeSelect();

		/** Filled-Polygon */
		filledPolygonNode = PrimitiveNode.newInstance("Filled-Polygon", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(defaultLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, box_1)
			});
		filledPolygonNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, filledPolygonNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		filledPolygonNode.setFunction(PrimitiveNode.Function.ART);
		filledPolygonNode.setHoldsOutline();
		filledPolygonNode.setEdgeSelect();

		/** Thick-Circle */
		thickCircleNode = PrimitiveNode.newInstance("Thick-Circle", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(defaultLayer, 0, Poly.Type.THICKCIRCLE, Technology.NodeLayer.POINTS, box_6)
			});
		thickCircleNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, thickCircleNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		thickCircleNode.setFunction(PrimitiveNode.Function.ART);
		thickCircleNode.setEdgeSelect();
	};

	/**
	 * Method to return a list of Polys that describe a given NodeInst.
	 * This method overrides the general one in the Technology object
	 * because of the unusual primitives in this Technology.
	 * @param ni the NodeInst to describe.
	 * @param wnd the window in which this node will be drawn.
	 * @param context the VarContext to this node in the hierarchy.
	 * @param electrical true to get the "electrical" layers.
	 * This makes no sense for Artwork primitives.
	 * @param reasonable true to get only a minimal set of contact cuts in large contacts.
	 * This makes no sense for Artwork primitives.
	 * @param primLayers an array of NodeLayer objects to convert to Poly objects.
	 * @param layerOverride the layer to use for all generated polygons (if not null).
	 * @return an array of Poly objects.
	 */
	public Poly [] getShapeOfNode(NodeInst ni, EditWindow0 wnd, VarContext context, boolean electrical, boolean reasonable,
		Technology.NodeLayer [] primLayers, Layer layerOverride)
	{
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		layerOverride = getProperLayer(ni);

		if (np == circleNode || np == thickCircleNode)
		{
			double [] angles = ni.getArcDegrees();
			if (ni.getXSize() != ni.getYSize())
			{
				// handle ellipses
				Point2D [] pointList = fillEllipse(ni.getAnchorCenter(), ni.getXSize(), ni.getYSize(),
					angles[0], angles[1]);
				Poly [] polys = new Poly[1];
				polys[0] = new Poly(pointList);
				if (np == circleNode) polys[0].setStyle(Poly.Type.OPENED); else
					polys[0].setStyle(Poly.Type.OPENEDT3);
				Technology.NodeLayer primLayer = primLayers[0];
				polys[0].setLayer(layerOverride);
				return polys;
			}

			// if there is arc information here, make it an arc of a circle
			if (angles[0] != 0.0 || angles[1] != 0.0)
			{
				// fill an arc of a circle here
				Poly [] polys = new Poly[1];
				Point2D [] pointList = new Point2D.Double[3];
				double cX = ni.getAnchorCenterX();
				double cY = ni.getAnchorCenterY();
				double dist = ni.getXSize() / 2;
				pointList[0] = new Point2D.Double(cX, cY);
				pointList[1] = new Point2D.Double(cX + Math.cos(angles[0]+angles[1])*dist, cY + Math.sin(angles[0]+angles[1])*dist);
				pointList[2] = new Point2D.Double(cX + Math.cos(angles[0])*dist, cY + Math.sin(angles[0])*dist);
				polys[0] = new Poly(pointList);
				if (np == circleNode) polys[0].setStyle(Poly.Type.CIRCLEARC); else
					polys[0].setStyle(Poly.Type.THICKCIRCLEARC);
				Technology.NodeLayer primLayer = primLayers[0];
				polys[0].setLayer(layerOverride);
				return polys;
			}
		} else if (np == splineNode)
		{
			Point2D [] tracePoints = ni.getTrace();
			if (tracePoints != null)
			{
				double cX = ni.getAnchorCenterX();
				double cY = ni.getAnchorCenterY();
				Point2D [] pointList = fillSpline(cX, cY, tracePoints);
				Poly [] polys = new Poly[1];
				polys[0] = new Poly(pointList);
				polys[0].setStyle(Poly.Type.OPENED);
				Technology.NodeLayer primLayer = primLayers[0];
				polys[0].setLayer(layerOverride);
				return polys;
			}
		} else if (np == arrowNode)
		{
			if (isFilledArrowHeads())
			{
				primLayers = new Technology.NodeLayer[2];
				primLayers[0] = new Technology.NodeLayer(defaultLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.POINTS,
					new Technology.TechPoint[]
					{
						new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeTopEdge()),
						new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
						new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					});
				primLayers[1] = new Technology.NodeLayer(defaultLayer, 0, Poly.Type.FILLED, Technology.NodeLayer.POINTS,
					new Technology.TechPoint[]
					{
						new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
						new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
						new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					});
			}
		}
		return super.getShapeOfNode(ni, wnd, context, electrical, reasonable, primLayers, layerOverride);
	}

	/**
	 * Returns a polygon that describes a particular port on a NodeInst.
	 * @param ni the NodeInst that has the port of interest.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @param pp the PrimitivePort on that NodeInst that is being described.
	 * @return a Poly object that describes this PrimitivePort graphically.
	 */
	public Poly getShapeOfPort(NodeInst ni, PrimitivePort pp, Point2D selectPt)
	{
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		if (np == pinNode || np == arrowNode || np == circleNode || np == thickCircleNode || np == filledCircleNode)
		{
			return super.getShapeOfPort(ni, pp, selectPt);
		}
		Poly [] polys = getShapeOfNode(ni);
		return polys[0];
	}

	/**
	 * Method to return a list of Polys that describe a given ArcInst.
	 * This method overrides the general one in the Technology object
	 * because of the unusual primitives in this Technology.
	 * @param ai the ArcInst to describe.
	 * @param wnd the window in which this arc will be drawn.
	 * @param onlyTheseLayers to filter the only required layers
	 * @return an array of Poly objects.
	 */
	public Poly [] getShapeOfArc(ArcInst ai, EditWindow0 wnd, Layer layerOverride, List<Layer.Function> onlyTheseLayers)
	{
		layerOverride = getProperLayer(ai);
		return super.getShapeOfArc(ai, wnd, layerOverride, onlyTheseLayers);
	}

	/**
	 * Method to return an array of Point2D that describe an ellipse.
	 * @param center the center coordinate of the ellipse.
	 * @param sX the X size of the ellipse.
	 * @param sY the Y size of the ellipse.
	 * @param startoffset the starting angle of the ellipse, in radians.
	 * @param endangle the ending angle of the ellipse, in radians.
	 * If both startoffset and endangle are zero, draw the full ellipse.
	 * @return an array of points that describes the ellipse.
	 */
	public static Point2D [] fillEllipse(Point2D center, double sX, double sY, double startoffset, double endangle)
	{
		// ensure that the polygon can hold the vectors
		boolean closed = true;
		if (startoffset == 0 && endangle == 0)
		{
			// full ellipse
			endangle = Math.PI * 2.0;
		} else
		{
			// partial ellipse
			closed = false;
		}
		int pts = (int)(endangle * ELLIPSEPOINTS / (Math.PI * 2.0));
		if (pts < 3) pts = 3;
		if (closed) pts++;

		Point2D [] points = new Point2D.Double[pts];

		// compute the length of the semi-major and semi-minor axes
		double a = sX / 2;
		double b = sY / 2;

		if (closed)
		{
			// more efficient algorithm used for full ellipse drawing
			double p = 2.0 * Math.PI / (ELLIPSEPOINTS-1);
			double c2 = Math.cos(p);    double s2 = Math.sin(p);
			double c3 = 1.0;            double s3 = 0.0;
			for(int m=0; m<ELLIPSEPOINTS; m++)
			{
				points[m] = new Point2D.Double(center.getX() + a * c3, center.getY() + b * s3);
				double t1 = c3*c2 - s3*s2;
				s3 = s3*c2 + c3*s2;
				c3 = t1;
			}
		} else
		{
			// less efficient algorithm for partial ellipse drawing
			for(int m=0; m<pts; m++)
			{
				double p = startoffset + m * endangle / (pts-1);
				double c2 = Math.cos(p);   double s2 = Math.sin(p);
				points[m] = new Point2D.Double(center.getX() + a * c2, center.getY() + b * s2);
			}
		}
		return points;
	}

	/**
	 * Method to extract an X coordinate from an array.
	 * @param tracePoints the array of coordinate values.
	 * @param index the entry in the array to retrieve.
	 * @param cX an offset value to add to the retrieved value.
	 * @return the X coordinate value.
	 */
	private double getTracePointX(Point2D [] tracePoints, int index, double cX)
	{
		double v = tracePoints[index].getX();
		return v + cX;
	}

	/**
	 * Method to extract an Y coordinate from an array.
	 * @param tracePoints the array of coordinate values.
	 * @param index the entry in the array to retrieve.
	 * @param cY an offset value to add to the retrieved value.
	 * @return the Y coordinate value.
	 */
	private double getTracePointY(Point2D [] tracePoints, int index, double cY)
	{
		double v = tracePoints[index].getY();
		return v + cY;
	}

    /**
	 * Method to set default outline information on a NodeInst.
	 * Very few primitives have default outline information (usually just in the Artwork Technology).
	 * This method overrides the one in Technology.
	 * @param ni the NodeInst to load with default outline information.
	 */
	public void setDefaultOutline(NodeInst ni)
	{
		if (ni.isCellInstance()) return;
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		if (np == openedPolygonNode || np == openedDottedPolygonNode ||
			np == openedDashedPolygonNode ||  np == openedThickerPolygonNode ||
			np == splineNode)
		{
			EPoint [] outline = new EPoint[4];
			outline[0] = new EPoint(-3, -3);
			outline[1] = new EPoint(-1, 3);
			outline[2] = new EPoint(1, -3);
			outline[3] = new EPoint(3, 3);
			ni.newVar(NodeInst.TRACE, outline);
		}
		if (np == closedPolygonNode || np == filledPolygonNode)
		{
			Point2D [] outline = new EPoint[4];
			outline[0] = new EPoint(0, -3);
			outline[1] = new EPoint(-3, 0);
			outline[2] = new EPoint(0, 3);
			outline[3] = new EPoint(3, -3);
			ni.newVar(NodeInst.TRACE, outline);
		}
	}

	/**
	 * Method to convert the given spline control points into a spline curve.
	 * @param cX the center X coordinate of the spline.
	 * @param cY the center Y coordinate of the spline.
	 * @param tracePoints the array of control point values, alternating X/Y/X/Y.
	 * @return an array of points that describes the spline.
	 */
	public Point2D [] fillSpline(double cX, double cY, Point2D [] tracePoints)
	{
		int steps = SPLINEGRAIN;
		int count = tracePoints.length;
		int outPoints = (count - 1) * steps + 1;
		Point2D [] points = new Point2D.Double[outPoints];
		int out = 0;

		double splineStep = 1.0 / (double)steps;
		double x2 = getTracePointX(tracePoints, 0, cX)*2 - getTracePointX(tracePoints, 1, cX);
		double y2 = getTracePointY(tracePoints, 0, cY)*2 - getTracePointY(tracePoints, 1, cY);
		double x3 = getTracePointX(tracePoints, 0, cX);
		double y3 = getTracePointY(tracePoints, 0, cY);
		double x4 = getTracePointX(tracePoints, 1, cX);
		double y4 = getTracePointY(tracePoints, 1, cY);
		for(int k = 2; k <= count; k++)
		{
			double x1 = x2;   x2 = x3;   x3 = x4;
			double y1 = y2;   y2 = y3;   y3 = y4;
			if (k == count)
			{
			   x4 = getTracePointX(tracePoints, k-1, cX)*2 - getTracePointX(tracePoints, k-2, cX);
			   y4 = getTracePointY(tracePoints, k-1, cY)*2 - getTracePointY(tracePoints, k-2, cY);
			} else
			{
			   x4 = getTracePointX(tracePoints, k, cX);
			   y4 = getTracePointY(tracePoints, k, cY);
			}

			int i=0;
			for(double t=0.0; i<steps; i++, t+= splineStep)
			{
				double tsq = t * t;
				double t4 = tsq * t;
				double t3 = -3.0*t4 + 3.0*tsq + 3.0*t + 1.0;
				double t2 = 3.0*t4 - 6.0*tsq + 4.0;
				double t1 = -t4 + 3.0*tsq - 3.0*t + 1.0;

				double x = (x1*t1 + x2*t2 + x3*t3 + x4*t4) / 6.0;
				double y = (y1*t1 + y2*t2 + y3*t3 + y4*t4) / 6.0;
				points[out++] = new Point2D.Double(x, y);
			}
		}

		// close the spline
		points[out++] = new Point2D.Double(getTracePointX(tracePoints, count-1, cX),
			getTracePointY(tracePoints, count-1, cY));
		return points;
	}

	/**
	 * Method to return the Layer to use for an Artwork ElectricObject.
	 * If there are individualized color and pattern Variables on the ElectricObject,
	 * they are used to construct a new Layer (with a new EGraphics that captures
	 * the color and pattern).
	 * @param eObj the ElectricObject with individualized color and pattern information.
	 * @return a Layer that has the color and pattern.
	 */
	private Layer getProperLayer(ElectricObject eObj)
	{
		EGraphics graphics = makeGraphics(eObj);
		if (graphics == null) return defaultLayer;
		Layer thisLayer = Layer.newInstance("Graphics", graphics);
		return thisLayer;
	}
	
	/**
	 * Method to create an EGraphics for an ElectricObject with color and pattern Variables.
	 * @param eObj the ElectricObject with graphics specifications.
	 * @return a new EGraphics that has the color and pattern.
	 */
	public static EGraphics makeGraphics(ElectricObject eObj)
	{
		// get the color and pattern information
		Variable colorVar = eObj.getVar(ART_COLOR, Integer.class);
		Variable patternVar = eObj.getVar(ART_PATTERN);
		if (colorVar == null && patternVar == null) return null;

		// make a fake layer with graphics
		EGraphics graphics = new EGraphics(false, false, null, 0, 0,0,0, 0.8,true,
			new int[] {0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff,
				0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff});

		// set the color if specified
		if (colorVar != null)
		{
			Integer color = (Integer)colorVar.getObject();
			graphics.setColorIndex(color.intValue());
		}

		// set the stipple pattern if specified
		if (patternVar != null)
		{
			int len = patternVar.getLength();
			if (len != 8 && len != 16 && len != 17)
			{
				System.out.println("'ART_pattern' length is incorrect");
				return null;
			}

			graphics.setPatternedOnDisplay(true);
			graphics.setPatternedOnPrinter(true);
			graphics.setOutlined(null);
			int [] pattern = graphics.getPattern();
			Object obj = patternVar.getObject();
			if (obj instanceof Integer[])
			{
				Integer [] pat = (Integer [])obj;
				if (len == 17)
				{
					// the last entry specifies the outline texture
					int outlineIndex = pat[16].intValue();
					graphics.setOutlined(EGraphics.Outline.findOutline(outlineIndex));
					len = 16;
				}
				for(int i=0; i<len; i++)
					pattern[i] = pat[i].intValue();
			} else if (obj instanceof Short[])
			{
				Short [] pat = (Short [])obj;
				for(int i=0; i<len; i++)
					pattern[i] = pat[i].shortValue();
				graphics.setOutlined(EGraphics.Outline.PAT_S);
			}
			if (len == 8)
			{
				for(int i=0; i<8; i++) pattern[i+8] = pattern[i];
			}
		}
		return graphics;
	}
	
//	/**
//	 * Method to set Variables on an ElectricObject to capture information in an EGraphics.
//	 * @param graphics the EGraphics to store on the ElectricObject.
//	 * @param eObj the ElectricObject that will have new graphics information.
//	 */
//	public static void setGraphics(EGraphics graphics, ElectricObject eObj)
//	{
//		// see what is already on the object
//		Variable colorVar = eObj.getVar(ART_COLOR, Integer.class);
//		Variable patternVar = eObj.getVar(ART_PATTERN);
//
//		// set the color if specified
//		int transparent = graphics.getTransparentLayer();
//		Color newColor = graphics.getColor();
//		if (transparent == 0 && newColor == Color.BLACK)
//		{
//			if (colorVar != null) eObj.delVar(ART_COLOR);
//		} else
//		{
//			int index = 0;
//			if (transparent > 0) index = EGraphics.makeIndex(transparent); else
//				index = EGraphics.makeIndex(newColor);
//			eObj.newVar(ART_COLOR, new Integer(index));
//		}
//
//		// set the stipple pattern if specified
//		if (graphics.isPatternedOnDisplay())
//		{
//			// set the pattern
//			int [] pattern = graphics.getPattern();
//			Integer [] pat = new Integer[17];
//			for(int i=0; i<16; i++)
//				pat[i] = new Integer(pattern[i]);
//			pat[16] = new Integer(graphics.getOutlined().getIndex());
//			eObj.newVar(ART_PATTERN, pat);
//		} else
//		{
//			if (patternVar != null) eObj.delVar(ART_PATTERN);
//		}
//	}

	/**
	 * Method to convert old primitive names to their proper NodeProtos.
	 * @param name the name of the old primitive.
	 * @return the proper PrimitiveNode to use (or null if none can be determined).
	 */
	public PrimitiveNode convertOldNodeName(String name)
	{
		if (name.equals("Message") || name.equals("Centered-Message") ||
			name.equals("Left-Message") || name.equals("Right-Message"))
				return Generic.tech.invisiblePinNode;
		if (name.equals("Opened-FarDotted-Polygon")) return openedThickerPolygonNode;
		return null;
	}

	/**
	 * Method to convert old primitive names to their proper ArcProtos.
	 * @param name the name of the old primitive.
	 * @return the proper ArcProto to use (or null if none can be determined).
	 */
	public ArcProto convertOldArcName(String name)
	{
		if (name.equals("Dash-1")) return dottedArc;
		if (name.equals("Dash-2")) return dashedArc;
		if (name.equals("Dash-3")) return thickerArc;
		return null;
	}

	/******************** OPTIONS ********************/

	private static Pref cacheFillArrows = Pref.makeBooleanPref("ArtworkFillArrows", getTechnologyPreferences(), false);
	/**
	 * Method to tell whether arrow heads are filled-in.
	 * @return true if arrow heads are filled-in.
	 */
	public static boolean isFilledArrowHeads()
	{
		return cacheFillArrows.getBoolean();
	}
	/**
	 * Method to set whether arrow heads are filled-in.
	 * @param f true if arrow heads are filled-in.
	 */
	public static void setFilledArrowHeads(boolean f)
	{
		cacheFillArrows.setBoolean(f);
	}
}
