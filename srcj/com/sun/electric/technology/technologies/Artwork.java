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
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.geom.Point2D;

/**
 * This is the general purpose sketching technology.
 */
public class Artwork extends Technology
{
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

	/** Defines a Solid arc. */						public PrimitiveArc solidArc;
	/** Defines a Dotted arc. */					public PrimitiveArc dottedArc;
	/** Defines a Dashed arc. */					public PrimitiveArc dashedArc;
	/** Defines a Thick arc. */						public PrimitiveArc thickerArc;
	/** the layer */								private Layer G_lay;

	// -------------------- private and protected methods ------------------------
	private Artwork()
	{
		setTechName("artwork");
		setTechDesc("General Purpose Sketchpad Facility");
		setScale(4000);
		setNonStandard();
		setNonElectrical();
		setNoNegatedArcs();
		setStaticTechnology();

		//**************************************** LAYERS ****************************************

		/** Graphics layer */
		G_lay = Layer.newInstance(this, "Graphics",
			new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0,0,0,0.8,1,
			new int[] {0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff,
				0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff}));

		// The layer functions
		G_lay.setFunction(Layer.Function.ART);		// Graphics

		// The DXF names
		G_lay.setDXFLayer("OBJECT");		// Graphics

		// The GDS names
		G_lay.setGDSLayer("1");		// Graphics

		//******************** ARCS ********************

		/** Solid arc */
		solidArc = PrimitiveArc.newInstance(this, "Solid", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(G_lay, 0, Poly.Type.OPENED)
		});
		solidArc.setFunction(PrimitiveArc.Function.NONELEC);
		solidArc.setWipable();
		solidArc.setAngleIncrement(0);

		/** Dotted arc */
		dottedArc = PrimitiveArc.newInstance(this, "Dotted", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(G_lay, 0, Poly.Type.OPENEDT1)
		});
		dottedArc.setFunction(PrimitiveArc.Function.NONELEC);
		dottedArc.setWipable();
		dottedArc.setAngleIncrement(0);

		/** Dashed arc */
		dashedArc = PrimitiveArc.newInstance(this, "Dashed", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(G_lay, 0, Poly.Type.OPENEDT2)
		});
		dashedArc.setFunction(PrimitiveArc.Function.NONELEC);
		dashedArc.setWipable();
		dashedArc.setAngleIncrement(0);

		/** Thicker arc */
		thickerArc = PrimitiveArc.newInstance(this, "Thicker", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(G_lay, 0, Poly.Type.OPENEDT3)
		});
		thickerArc.setFunction(PrimitiveArc.Function.NONELEC);
		thickerArc.setWipable();
		thickerArc.setAngleIncrement(0);

		//******************** RECTANGLE DESCRIPTIONS ********************

		Technology.TechPoint [] box_1 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.CENTER),
			new Technology.TechPoint(EdgeH.CENTER, EdgeV.TOPEDGE),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(EdgeH.CENTER, EdgeV.BOTTOMEDGE),
		};
		Technology.TechPoint [] box_2 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(new EdgeH(-0.125, 0), EdgeV.TOPEDGE),
			new Technology.TechPoint(new EdgeH(0.125, 0), EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.TOPEDGE),
		};
		Technology.TechPoint [] box_3 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.TOPEDGE),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.CENTER),
			new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE),
		};
		Technology.TechPoint [] box_4 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.BOTTOMEDGE),
			new Technology.TechPoint(EdgeH.CENTER, EdgeV.TOPEDGE),
		};
		Technology.TechPoint [] box_6 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
			new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.CENTER),
		};

		//******************** NODES ********************

		/** Pin */
		pinNode = PrimitiveNode.newInstance("Pin", this, 1, 1, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_6)
			});
		pinNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pinNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.CENTER, EdgeH.CENTER, EdgeV.CENTER)
			});
		pinNode.setFunction(NodeProto.Function.PIN);
		pinNode.setArcsWipe();
		pinNode.setArcsShrink();

		/** Box */
		boxNode = PrimitiveNode.newInstance("Box", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		boxNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, boxNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "box", 180,0, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		boxNode.setFunction(NodeProto.Function.ART);

		/** Crossed-Box */
		crossedBoxNode = PrimitiveNode.newInstance("Crossed-Box", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		crossedBoxNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, crossedBoxNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "fbox", 180,0, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		crossedBoxNode.setFunction(NodeProto.Function.ART);

		/** Filled-Box */
		filledBoxNode = PrimitiveNode.newInstance("Filled-Box", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		filledBoxNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, filledBoxNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "fbox", 180,0, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		filledBoxNode.setFunction(NodeProto.Function.ART);

		/** Circle */
		circleNode = PrimitiveNode.newInstance("Circle", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, box_6)
			});
		circleNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, circleNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		circleNode.setFunction(NodeProto.Function.ART);


		/** Filled-Circle */
		filledCircleNode = PrimitiveNode.newInstance("Filled-Circle", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_6)
			});
		filledCircleNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, filledCircleNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		filledCircleNode.setFunction(NodeProto.Function.ART);
		filledCircleNode.setSquare();

		/** Spline */
		splineNode = PrimitiveNode.newInstance("Spline", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_2)
			});
		splineNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, splineNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		splineNode.setFunction(NodeProto.Function.ART);
		splineNode.setHoldsOutline();

		/** Triangle */
		triangleNode = PrimitiveNode.newInstance("Triangle", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.POINTS, box_4)
			});
		triangleNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, triangleNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "triangle", 180,0, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		triangleNode.setFunction(NodeProto.Function.ART);

		/** Filled-Triangle */
		filledTriangleNode = PrimitiveNode.newInstance("Filled-Triangle", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.POINTS, box_4)
			});
		filledTriangleNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, filledTriangleNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "ftriangle", 180,0, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		filledTriangleNode.setFunction(NodeProto.Function.ART);

		/** Arrow */
		arrowNode = PrimitiveNode.newInstance("Arrow", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_3)
			});
		arrowNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, arrowNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "arrow", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.RIGHTEDGE, EdgeV.CENTER, EdgeH.RIGHTEDGE, EdgeV.CENTER)
			});
		arrowNode.setFunction(NodeProto.Function.ART);

		/** Opened-Polygon */
		openedPolygonNode = PrimitiveNode.newInstance("Opened-Polygon", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_2)
			});
		openedPolygonNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, openedPolygonNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		openedPolygonNode.setFunction(NodeProto.Function.ART);
		openedPolygonNode.setHoldsOutline();

		/** Opened-Dotted-Polygon */
		openedDottedPolygonNode = PrimitiveNode.newInstance("Opened-Dotted-Polygon", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.OPENEDT1, Technology.NodeLayer.POINTS, box_2)
			});
		openedDottedPolygonNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, openedDottedPolygonNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		openedDottedPolygonNode.setFunction(NodeProto.Function.ART);
		openedDottedPolygonNode.setHoldsOutline();

		/** Opened-Dashed-Polygon */
		openedDashedPolygonNode = PrimitiveNode.newInstance("Opened-Dashed-Polygon", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.OPENEDT2, Technology.NodeLayer.POINTS, box_2)
			});
		openedDashedPolygonNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, openedDashedPolygonNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		openedDashedPolygonNode.setFunction(NodeProto.Function.ART);
		openedDashedPolygonNode.setHoldsOutline();

		/** Opened-Thicker-Polygon */
		openedThickerPolygonNode = PrimitiveNode.newInstance("Opened-Thicker-Polygon", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.OPENEDT3, Technology.NodeLayer.POINTS, box_2)
			});
		openedThickerPolygonNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, openedThickerPolygonNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		openedThickerPolygonNode.setFunction(NodeProto.Function.ART);
		openedThickerPolygonNode.setHoldsOutline();

		/** Closed-Polygon */
		closedPolygonNode = PrimitiveNode.newInstance("Closed-Polygon", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.MINBOX, box_1)
			});
		closedPolygonNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, closedPolygonNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		closedPolygonNode.setFunction(NodeProto.Function.ART);
		closedPolygonNode.setHoldsOutline();

		/** Filled-Polygon */
		filledPolygonNode = PrimitiveNode.newInstance("Filled-Polygon", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, box_1)
			});
		filledPolygonNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, filledPolygonNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		filledPolygonNode.setFunction(NodeProto.Function.ART);
		filledPolygonNode.setHoldsOutline();

		/** Thick-Circle */
		thickCircleNode = PrimitiveNode.newInstance("Thick-Circle", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.THICKCIRCLE, Technology.NodeLayer.POINTS, box_6)
			});
		thickCircleNode.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, thickCircleNode, new ArcProto [] {solidArc, dottedArc, dashedArc, thickerArc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		thickCircleNode.setFunction(NodeProto.Function.ART);
	};

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
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		getGraphics(ni);

		if (np == circleNode || np == thickCircleNode)
		{
			double [] angles = ni.getArcDegrees();
			if (ni.getXSize() != ni.getYSize())
			{
				// handle ellipses
				Point2D [] pointList = fillEllipse(ni.getCenter(), ni.getXSize(), ni.getYSize(),
					angles[0], angles[1]);
				Poly [] polys = new Poly[1];
				polys[0] = new Poly(pointList);
				if (np == circleNode) polys[0].setStyle(Poly.Type.OPENED); else
					polys[0].setStyle(Poly.Type.OPENEDT3);
				Technology.NodeLayer [] primLayers = np.getLayers();
				Technology.NodeLayer primLayer = primLayers[0];
				polys[0].setLayer(primLayer.getLayer());
				return polys;
			}

			// if there is arc information here, make it an arc of a circle
			if (angles[0] != 0.0 || angles[1] != 0.0)
			{
				// fill an arc of a circle here
				Poly [] polys = new Poly[1];
				Point2D [] pointList = new Point2D.Double[3];
				double cX = ni.getCenterX();
				double cY = ni.getCenterY();
				double dist = ni.getXSize() / 2;
				pointList[0] = new Point2D.Double(cX, cY);
				pointList[1] = new Point2D.Double(cX + Math.cos(angles[0]+angles[1])*dist, cY + Math.sin(angles[0]+angles[1])*dist);
				pointList[2] = new Point2D.Double(cX + Math.cos(angles[0])*dist, cY + Math.sin(angles[0])*dist);
				polys[0] = new Poly(pointList);
				if (np == circleNode) polys[0].setStyle(Poly.Type.CIRCLEARC); else
					polys[0].setStyle(Poly.Type.THICKCIRCLEARC);
				Technology.NodeLayer [] primLayers = np.getLayers();
				Technology.NodeLayer primLayer = primLayers[0];
				polys[0].setLayer(primLayer.getLayer());
				return polys;
			}
		} else if (np == splineNode)
		{
			Float [] tracePoints = ni.getTrace();
			if (tracePoints != null)
			{
				double cX = ni.getCenterX();
				double cY = ni.getCenterY();
				Point2D [] pointList = fillSpline(cX, cY, tracePoints);
				Poly [] polys = new Poly[1];
				polys[0] = new Poly(pointList);
				polys[0].setStyle(Poly.Type.OPENED);
				Technology.NodeLayer [] primLayers = np.getLayers();
				Technology.NodeLayer primLayer = primLayers[0];
				polys[0].setLayer(primLayer.getLayer());
				return polys;
			}
		}
		return super.getShapeOfNode(ni, wnd);
	}

	/**
	 * Returns a polygon that describes a particular port on a NodeInst.
	 * @param ni the NodeInst that has the port of interest.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @param pp the PrimitivePort on that NodeInst that is being described.
	 * @return a Poly object that describes this PrimitivePort graphically.
	 */
	public Poly getShapeOfPort(NodeInst ni, PrimitivePort pp)
	{
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		if (np == pinNode || np == arrowNode)
		{
			return super.getShapeOfPort(ni, pp);
		}
		Poly [] polys = getShapeOfNode(ni, null);
		return polys[0];
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
		getGraphics(ai);
		return super.getShapeOfArc(ai, wnd);
	}

	/**
	 * Routine to return an array of Point2D that describe an ellipse.
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
	 * Routine to extract an X coordinate from an array.
	 * @param tracePoints the array of coordinate values, alternating X/Y/X/Y.
	 * @param index the entry in the array to retrieve.
	 * @param cX an offset value to add to the retrieved value.
	 * @return the X coordinate value.
	 */
	private double getTracePointX(Float [] tracePoints, int index, double cX)
	{
		double v = tracePoints[index*2].doubleValue();
		return v + cX;
	}

	/**
	 * Routine to extract an Y coordinate from an array.
	 * @param tracePoints the array of coordinate values, alternating X/Y/X/Y.
	 * @param index the entry in the array to retrieve.
	 * @param cY an offset value to add to the retrieved value.
	 * @return the Y coordinate value.
	 */
	private double getTracePointY(Float [] tracePoints, int index, double cY)
	{
		double v = tracePoints[index*2+1].doubleValue();
		return v + cY;
	}

	/*
	 * Routine to convert the given spline control points into a spline curve.
	 * @param cX the center X coordinate of the spline.
	 * @param cY the center Y coordinate of the spline.
	 * @param tracePoints the array of control point values, alternating X/Y/X/Y.
	 * @return an array of points that describes the spline.
	 */
	private Point2D [] fillSpline(double cX, double cY, Float [] tracePoints)
	{
		int steps = SPLINEGRAIN;
		int count = tracePoints.length / 2;
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
	 * Routine to find examine the ElectricObject and prepare for any Graphics found there.
	 * @param obj the object with graphics specifications.
	 */
	private void getGraphics(ElectricObject obj)
	{
		// get the color information
		Variable var = obj.getVar("ART_color", Integer.class);
		EGraphics graphics = G_lay.getGraphics();
		if (var == null)
		{
			graphics.setColor(EGraphics.BLACK);
		} else
		{
			Integer color = (Integer)var.getObject();
			graphics.setColor(color.intValue());
		}

//		// get the stipple pattern information
//		artpl->patternvar = getvalkey(addr, type, -1, art_patternkey);
//		if (artpl->patternvar != NOVARIABLE)
//		{
//			len = getlength(artpl->patternvar);
//			if ((len != 8 && len != 16) ||
//				((artpl->patternvar->type&VTYPE) != VINTEGER && (artpl->patternvar->type&VTYPE) != VSHORT))
//			{
//				ttyputerr(_("'ART_pattern' must be a 16-member INTEGER or SHORT array"));
//				artpl->patternvar = NOVARIABLE;
//				return;
//			}
//
//			sty = PATTERNED;
//			if ((artpl->patternvar->type&VTYPE) == VINTEGER)
//			{
//				for(i=0; i<len; i++)
//					art_st_lay.raster[i] = (UINTSML)(((INTBIG *)artpl->patternvar->addr)[i]);
//			} else
//			{
//				for(i=0; i<len; i++)
//					art_st_lay.raster[i] = ((INTSML *)artpl->patternvar->addr)[i];
//				sty |= OUTLINEPAT;
//			}
//			if (len == 8)
//			{
//				for(i=0; i<8; i++) art_st_lay.raster[i+8] = art_st_lay.raster[i];
//			}
//
//			// set the outline style (outlined if SHORT used)
//			art_st_lay.colstyle = art_st_lay.bwstyle = (INTSML)sty;
//		}
	}

	/**
	 * Routine to convert old primitive names to their proper NodeProtos.
	 * @param name the name of the old primitive.
	 * @return the proper PrimitiveNode to use (or null if none can be determined).
	 */
	public PrimitiveNode convertOldNodeName(String name)
	{
		if (name.equals("Message") || name.equals("Centered-Message") ||
			name.equals("Left-Message") || name.equals("Right-Message"))
				return (PrimitiveNode)NodeProto.findNodeProto("generic:Invisible-Pin");
		if (name.equals("Opened-FarDotted-Polygon")) return openedThickerPolygonNode;
		return null;
	}

	/**
	 * Routine to convert old primitive names to their proper ArcProtos.
	 * @param name the name of the old primitive.
	 * @return the proper PrimitiveArc to use (or null if none can be determined).
	 */
	public PrimitiveArc convertOldArcName(String name)
	{
		if (name.equals("Dash-1")) return dottedArc;
		if (name.equals("Dash-2")) return dashedArc;
		if (name.equals("Dash-3")) return thickerArc;
		return null;
	}
}
