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

import java.awt.geom.Point2D;

/**
 * This is the general purpose sketching technology.
 */
public class Artwork extends Technology
{
	/** the Artwork Technology object. */	public static final Artwork tech = new Artwork();

	/** number of lines in an ellipse */	private static final int ELLIPSEPOINTS =        30;
	/** granularity of a spline */			private static final int SPLINEGRAIN   =        20;

	/** Pin */								private PrimitiveNode p_node;
	/** Box */								private PrimitiveNode b_node;
	/** Crossed-Box */						private PrimitiveNode cb_node;
	/** Filled-Box */						private PrimitiveNode fb_node;
	/** Circle */							private PrimitiveNode c_node;
	/** Filled-Circle */					private PrimitiveNode fc_node;
	/** Spline */							private PrimitiveNode s_node;
	/** Triangle */							private PrimitiveNode t_node;
	/** Filled-Triangle */					private PrimitiveNode ft_node;
	/** Arrow */							private PrimitiveNode a_node;
	/** Opened-Polygon */					private PrimitiveNode op_node;
	/** Opened-Dotted-Polygon */			private PrimitiveNode odp_node;
	/** Opened-Dashed-Polygon */			private PrimitiveNode odp0_node;
	/** Opened-Thicker-Polygon */			private PrimitiveNode otp_node;
	/** Closed-Polygon */					private PrimitiveNode cp_node;
	/** Filled-Polygon */					private PrimitiveNode fp_node;
	/** Thick-Circle */						private PrimitiveNode tc_node;

	/** Solid arc */						private PrimitiveArc Solid_arc;
	/** Dotted arc */						private PrimitiveArc Dotted_arc;
	/** Dashed arc */						private PrimitiveArc Dashed_arc;
	/** Thick arc */						private PrimitiveArc Thicker_arc;
	/** Thick arc */						private Layer G_lay;

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
		G_lay = Layer.newInstance("Graphics",
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
		Solid_arc = PrimitiveArc.newInstance(this, "Solid", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(G_lay, 0, Poly.Type.OPENED)
		});
		Solid_arc.setFunction(PrimitiveArc.Function.NONELEC);
		Solid_arc.setWipable();
		Solid_arc.setAngleIncrement(0);

		/** Dotted arc */
		Dotted_arc = PrimitiveArc.newInstance(this, "Dotted", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(G_lay, 0, Poly.Type.OPENEDT1)
		});
		Dotted_arc.setFunction(PrimitiveArc.Function.NONELEC);
		Dotted_arc.setWipable();
		Dotted_arc.setAngleIncrement(0);

		/** Dashed arc */
		Dashed_arc = PrimitiveArc.newInstance(this, "Dashed", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(G_lay, 0, Poly.Type.OPENEDT2)
		});
		Dashed_arc.setFunction(PrimitiveArc.Function.NONELEC);
		Dashed_arc.setWipable();
		Dashed_arc.setAngleIncrement(0);

		/** Thicker arc */
		Thicker_arc = PrimitiveArc.newInstance(this, "Thicker", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(G_lay, 0, Poly.Type.OPENEDT3)
		});
		Thicker_arc.setFunction(PrimitiveArc.Function.NONELEC);
		Thicker_arc.setWipable();
		Thicker_arc.setAngleIncrement(0);

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
		p_node = PrimitiveNode.newInstance("Pin", this, 1, 1, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_6)
			});
		p_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, p_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.CENTER, EdgeH.CENTER, EdgeV.CENTER)
			});
		p_node.setFunction(NodeProto.Function.PIN);
		p_node.setArcsWipe();
		p_node.setArcsShrink();

		/** Box */
		b_node = PrimitiveNode.newInstance("Box", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		b_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, b_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "box", 180,0, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		b_node.setFunction(NodeProto.Function.ART);

		/** Crossed-Box */
		cb_node = PrimitiveNode.newInstance("Crossed-Box", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.CROSSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		cb_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, cb_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "fbox", 180,0, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		cb_node.setFunction(NodeProto.Function.ART);

		/** Filled-Box */
		fb_node = PrimitiveNode.newInstance("Filled-Box", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		fb_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, fb_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "fbox", 180,0, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		fb_node.setFunction(NodeProto.Function.ART);

		/** Circle */
		c_node = PrimitiveNode.newInstance("Circle", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, box_6)
			});
		c_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, c_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		c_node.setFunction(NodeProto.Function.ART);


		/** Filled-Circle */
		fc_node = PrimitiveNode.newInstance("Filled-Circle", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_6)
			});
		fc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, fc_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		fc_node.setFunction(NodeProto.Function.ART);
		fc_node.setSquare();

		/** Spline */
		s_node = PrimitiveNode.newInstance("Spline", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_2)
			});
		s_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, s_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		s_node.setFunction(NodeProto.Function.ART);
		s_node.setHoldsOutline();

		/** Triangle */
		t_node = PrimitiveNode.newInstance("Triangle", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.POINTS, box_4)
			});
		t_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, t_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "triangle", 180,0, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		t_node.setFunction(NodeProto.Function.ART);

		/** Filled-Triangle */
		ft_node = PrimitiveNode.newInstance("Filled-Triangle", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.POINTS, box_4)
			});
		ft_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, ft_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "ftriangle", 180,0, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		ft_node.setFunction(NodeProto.Function.ART);

		/** Arrow */
		a_node = PrimitiveNode.newInstance("Arrow", this, 2, 2, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_3)
			});
		a_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, a_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "arrow", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.RIGHTEDGE, EdgeV.CENTER, EdgeH.RIGHTEDGE, EdgeV.CENTER)
			});
		a_node.setFunction(NodeProto.Function.ART);

		/** Opened-Polygon */
		op_node = PrimitiveNode.newInstance("Opened-Polygon", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, box_2)
			});
		op_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, op_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		op_node.setFunction(NodeProto.Function.ART);
		op_node.setHoldsOutline();

		/** Opened-Dotted-Polygon */
		odp_node = PrimitiveNode.newInstance("Opened-Dotted-Polygon", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.OPENEDT1, Technology.NodeLayer.POINTS, box_2)
			});
		odp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, odp_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		odp_node.setFunction(NodeProto.Function.ART);
		odp_node.setHoldsOutline();

		/** Opened-Dashed-Polygon */
		odp0_node = PrimitiveNode.newInstance("Opened-Dashed-Polygon", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.OPENEDT2, Technology.NodeLayer.POINTS, box_2)
			});
		odp0_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, odp0_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		odp0_node.setFunction(NodeProto.Function.ART);
		odp0_node.setHoldsOutline();

		/** Opened-Thicker-Polygon */
		otp_node = PrimitiveNode.newInstance("Opened-Thicker-Polygon", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.OPENEDT3, Technology.NodeLayer.POINTS, box_2)
			});
		otp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, otp_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		otp_node.setFunction(NodeProto.Function.ART);
		otp_node.setHoldsOutline();

		/** Closed-Polygon */
		cp_node = PrimitiveNode.newInstance("Closed-Polygon", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.MINBOX, box_1)
			});
		cp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, cp_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		cp_node.setFunction(NodeProto.Function.ART);
		cp_node.setHoldsOutline();

		/** Filled-Polygon */
		fp_node = PrimitiveNode.newInstance("Filled-Polygon", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.MINBOX, box_1)
			});
		fp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, fp_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		fp_node.setFunction(NodeProto.Function.ART);
		fp_node.setHoldsOutline();

		/** Thick-Circle */
		tc_node = PrimitiveNode.newInstance("Thick-Circle", this, 6, 6, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(G_lay, 0, Poly.Type.THICKCIRCLE, Technology.NodeLayer.POINTS, box_6)
			});
		tc_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, tc_node, new ArcProto [] {Solid_arc, Dotted_arc, Dashed_arc, Thicker_arc}, "site", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		tc_node.setFunction(NodeProto.Function.ART);
	};

	/**
	 * Routine to return a list of Polys that describe a given NodeInst.
	 * This routine overrides the general one in the Technology object
	 * because of the unusual primitives in the Artwork Technology.
	 * @param ni the NodeInst to describe.
	 * @return an array of Poly objects.
	 */
	public Poly [] getShape(NodeInst ni)
	{
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		getGraphics(ni);

		if (np == c_node || np == tc_node)
		{
			double [] angles = getarcdegrees(ni);
			if (ni.getXSize() != ni.getYSize())
			{
				// handle ellipses
				Point2D.Double [] pointList = fillEllipse(ni.getCenterX(), ni.getCenterY(), ni.getXSize(), ni.getYSize(),
					angles[0], angles[1]);
				Poly [] polys = new Poly[1];
				polys[0] = new Poly(pointList);
				if (np == c_node) polys[0].setStyle(Poly.Type.OPENED); else
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
				Point2D.Double [] pointList = new Point2D.Double[3];
				double cX = ni.getCenterX();
				double cY = ni.getCenterY();
				double dist = ni.getXSize() / 2;
				pointList[0] = new Point2D.Double(cX, cY);
				pointList[1] = new Point2D.Double(cX + Math.cos(angles[0]+angles[1])*dist, cY + Math.sin(angles[0]+angles[1])*dist);
				pointList[2] = new Point2D.Double(cX + Math.cos(angles[0])*dist, cY + Math.sin(angles[0])*dist);
				polys[0] = new Poly(pointList);
				if (np == c_node) polys[0].setStyle(Poly.Type.CIRCLEARC); else
					polys[0].setStyle(Poly.Type.THICKCIRCLEARC);
				Technology.NodeLayer [] primLayers = np.getLayers();
				Technology.NodeLayer primLayer = primLayers[0];
				polys[0].setLayer(primLayer.getLayer());
				return polys;
			}
		} else if (np == s_node)
		{
			Integer [] tracePoints = ni.getTrace();
			if (tracePoints != null)
			{
				double cX = ni.getCenterX();
				double cY = ni.getCenterY();
				Point2D.Double [] pointList = fillSpline(cX, cY, tracePoints);
				Poly [] polys = new Poly[1];
				polys[0] = new Poly(pointList);
				polys[0].setStyle(Poly.Type.OPENED);
				Technology.NodeLayer [] primLayers = np.getLayers();
				Technology.NodeLayer primLayer = primLayers[0];
				polys[0].setLayer(primLayer.getLayer());
				return polys;
			}
		}
		return super.getShape(ni);
	}

	/**
	 * Routine to return a list of Polys that describe a given ArcInst.
	 * This routine overrides the general one in the Technology object
	 * because of the unusual primitives in the Artwork Technology.
	 * @param ai the ArcInst to describe.
	 * @return an array of Poly objects.
	 */
	public Poly [] getShape(ArcInst ai)
	{
		getGraphics(ai);
		return super.getShape(ai);
	}

	/**
	 * Routine to return an array of Point2D.Double that describe an ellipse.
	 * @param cX the center X coordinate of the ellipse.
	 * @param cY the center Y coordinate of the ellipse.
	 * @param sX the X size of the ellipse.
	 * @param sY the Y size of the ellipse.
	 * @param startoffset the starting angle of the ellipse, in radians.
	 * @param endangle the ending angle of the ellipse, in radians.
	 * If both startoffset and endangle are zero, draw the full ellipse.
	 * @return an array of points that describes the ellipse.
	 */
	private Point2D.Double [] fillEllipse(double cX, double cY, double sX, double sY, double startoffset, double endangle)
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

		Point2D.Double [] points = new Point2D.Double[pts];

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
				points[m] = new Point2D.Double(cX + a * c3, cY + b * s3);
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
				points[m] = new Point2D.Double(cY + a * c2, cY + b * s2);
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
	private double getTracePointX(Integer [] tracePoints, int index, double cX)
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
	private double getTracePointY(Integer [] tracePoints, int index, double cY)
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
	private Point2D.Double [] fillSpline(double cX, double cY, Integer [] tracePoints)
	{
		int steps = SPLINEGRAIN;
		int count = tracePoints.length / 2;
		int outPoints = (count - 1) * steps + 1;
		Point2D.Double [] points = new Point2D.Double[outPoints];
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
	 * Routine to return the starting and ending angle of an arc described by the given NodeInst.
	 * These values can be found in the "ART_degrees" variable on the NodeInst.
	 * @param ni the NodeInst with the curvature.
	 * @return a 2-long double array with the starting offset in the first entry (a value in radians)
	 * and the amount of curvature in the second entry (in radians).
	 * If the NodeInst does not have circular information, both values are set to zero.
	 */
	private double [] getarcdegrees(NodeInst ni)
	{
		double [] returnValues = new double[2];
		returnValues[0] = returnValues[1] = 0.0;

		NodeProto np = ni.getProto();
		if (!(np instanceof PrimitiveNode)) return returnValues;
		if (np != c_node && np != tc_node) return returnValues;

		Variable var = ni.getVal("ART_degrees");
		if (var != null)
		{
			Object addr = var.getObject();
			if (addr instanceof Integer)
			{
				Integer iAddr = (Integer)addr;
				returnValues[0] = 0.0;
				returnValues[1] = (double)iAddr.intValue() * Math.PI / 1800.0;
			} else if (addr instanceof Float[])
			{
				Float [] fAddr = (Float [])addr;
				returnValues[0] = fAddr[0].doubleValue();
				returnValues[1] = fAddr[1].doubleValue();
			}
		}
		return returnValues;
	}

	/**
	 * Routine to find examine the ElectricObject and prepare for any Graphics found there.
	 * @param obj the object with graphics specifications.
	 */
	private void getGraphics(ElectricObject obj)
	{
		// get the color information
		Variable var = obj.getVal("ART_color", Integer.class);
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
		if (name == "Message" || name == "Centered-Message" ||
			name == "Left-Message" || name == "Right-Message")
				return (PrimitiveNode)NodeProto.findNodeProto("generic:Invisible-Pin");
		if (name == "Opened-FarDotted-Polygon") return otp_node;
		return null;
	}

	/**
	 * Routine to convert old primitive names to their proper ArcProtos.
	 * @param name the name of the old primitive.
	 * @return the proper PrimitiveArc to use (or null if none can be determined).
	 */
	public PrimitiveArc convertOldArcName(String name)
	{
		if (name == "Dash-1") return Dotted_arc;
		if (name == "Dash-2") return Dashed_arc;
		if (name == "Dash-3") return Thicker_arc;
		return null;
	}
}
