/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GEM.java
 * gem technology description
 * Generated automatically from a library
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.AbstractShapeBuilder;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.Foundry;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.TechFactory;
import com.sun.electric.technology.Technology;

/**
 * This is the Temporal Specification Facility (from Lansky) Technology.
 */
public class GEM extends Technology
{
	/** Variable key for GEM element name. */		public static final Variable.Key ELEMENT_NAME = Variable.newKey("GEM_element");
	/** Variable key for GEM event 1. */			public static final Variable.Key EVENT_1 = Variable.newKey("GEM_event1");
	/** Variable key for GEM event 2. */			public static final Variable.Key EVENT_2 = Variable.newKey("GEM_event2");
	/** Variable key for GEM event 3. */			public static final Variable.Key EVENT_3 = Variable.newKey("GEM_event3");
	/** Variable key for GEM event 4. */			public static final Variable.Key EVENT_4 = Variable.newKey("GEM_event4");

	private Layer E_lay, GA_lay, TA_lay, CA_lay, PA_lay, NA_lay, FA_lay;
	private ArcProto generalArc, temporalArc, causalArc, prerequisiteArc, nondeterministicArc, nondeterministicForkArc;
	private Technology.TechPoint [] box_7;
	private PrimitiveNode e_node;

	// -------------------- private and protected methods ------------------------
	public GEM(Generic generic, TechFactory techFactory)
	{
		super(generic, techFactory);
		setTechDesc("Temporal Specification Facility (from Lansky)");
		setFactoryScale(1000, false);   // in nanometers: really 1 microns
		setNoNegatedArcs();

		setStaticTechnology();
		setNonStandard();

		//**************************************** LAYERS ****************************************

		/** E layer */
		E_lay = Layer.newInstance(this, "Element",
			new EGraphics(false, false, null, 0, 255,0,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** GA layer */
		GA_lay = Layer.newInstance(this, "General-arc",
			new EGraphics(false, false, null, 0, 0,0,255, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** TA layer */
		TA_lay = Layer.newInstance(this, "Temporal-arc",
			new EGraphics(false, false, null, 0, 0,255,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** CA layer */
		CA_lay = Layer.newInstance(this, "Causal-arc",
			new EGraphics(false, false, null, 0, 0,0,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** PA layer */
		PA_lay = Layer.newInstance(this, "Prereq-arc",
			new EGraphics(false, false, null, 0, 255,190,6, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** NA layer */
		NA_lay = Layer.newInstance(this, "Nondet-arc",
			new EGraphics(false, false, null, 0, 255,255,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** FA layer */
		FA_lay = Layer.newInstance(this, "Fork-arc",
			new EGraphics(false, false, null, 0, 186,0,255, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		// The layer functions
		E_lay.setFunction(Layer.Function.ART, Layer.Function.NONELEC);			// Element
		GA_lay.setFunction(Layer.Function.CONTROL, Layer.Function.NONELEC);		// General-arc
		TA_lay.setFunction(Layer.Function.CONTROL, Layer.Function.NONELEC);		// Temporal-arc
		CA_lay.setFunction(Layer.Function.CONTROL, Layer.Function.NONELEC);		// Causal-arc
		PA_lay.setFunction(Layer.Function.CONTROL, Layer.Function.NONELEC);		// Prereq-arc
		NA_lay.setFunction(Layer.Function.CONTROL, Layer.Function.NONELEC);		// Nondet-arc
		FA_lay.setFunction(Layer.Function.CONTROL, Layer.Function.NONELEC);		// Fork-arc

		//******************** ARCS ********************

		/** General arc */
		generalArc = newArcProto("General", 0, 0, ArcProto.Function.NONELEC,
			new Technology.ArcLayer(GA_lay, 0, Poly.Type.FILLED)
		);
		generalArc.setWipable();
		generalArc.setFactoryAngleIncrement(0);

		/** Temporal arc */
		temporalArc = newArcProto("Temporal", 0, 0, ArcProto.Function.NONELEC,
			new Technology.ArcLayer(TA_lay, 0, Poly.Type.FILLED)
		);
		temporalArc.setWipable();
		temporalArc.setFactoryAngleIncrement(0);
		temporalArc.setFactoryDirectional(5);

		/** Causal arc */
		causalArc = newArcProto("Causal", 0, 0, ArcProto.Function.NONELEC,
			new Technology.ArcLayer(CA_lay, 0, Poly.Type.FILLED)
		);
		causalArc.setWipable();
		causalArc.setFactoryAngleIncrement(0);

		/** Prerequisite arc */
		prerequisiteArc = newArcProto("Prerequisite", 0, 0, ArcProto.Function.NONELEC,
			new Technology.ArcLayer(PA_lay, 0, Poly.Type.FILLED)
		);
		prerequisiteArc.setWipable();
		prerequisiteArc.setFactoryAngleIncrement(0);
		prerequisiteArc.setFactoryDirectional(5);

		/** Nondeterministic arc */
		nondeterministicArc = newArcProto("Nondeterministic", 0, 0, ArcProto.Function.NONELEC,
			new Technology.ArcLayer(NA_lay, 0, Poly.Type.FILLED)
		);
		nondeterministicArc.setWipable();
		nondeterministicArc.setFactoryAngleIncrement(0);
		nondeterministicArc.setFactoryDirectional(5);

		/** Nondeterministic-fork arc */
		nondeterministicForkArc = newArcProto("Nondeterministic-fork", 0, 0, ArcProto.Function.NONELEC,
			new Technology.ArcLayer(FA_lay, 0, Poly.Type.FILLED)
		);
		nondeterministicForkArc.setWipable();
		nondeterministicForkArc.setFactoryAngleIncrement(0);
		nondeterministicForkArc.setFactoryDirectional(7);

		//******************** RECTANGLE DESCRIPTIONS ********************

		Technology.TechPoint [] box_6 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeTopEdge()),
		};
		box_7 = new Technology.TechPoint[] {
			new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
			new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter()),
		};

		//******************** NODES ********************

		/** General-Pin */
		PrimitiveNode gp_node = PrimitiveNode.newInstance("General-Pin", this, 1, 1, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(GA_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_7)
			});
		PrimitivePort pinPort = PrimitivePort.newInstance(this, gp_node, new ArcProto [] {generalArc}, "general", 0,180, 0, PortCharacteristic.UNKNOWN,
			EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter());
		gp_node.addPrimitivePortsFixed(new PrimitivePort[] {pinPort});
		gp_node.setFunction(PrimitiveNode.Function.PIN);
		gp_node.setArcsWipe();
		gp_node.setArcsShrink();

		/** Temporal-Pin */
		PrimitiveNode tp_node = PrimitiveNode.newInstance("Temporal-Pin", this, 1, 1, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(TA_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_7)
			});
		tp_node.addPrimitivePortsFixed(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, tp_node, new ArcProto [] {temporalArc}, "temporal", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		tp_node.setFunction(PrimitiveNode.Function.PIN);
		tp_node.setArcsWipe();
		tp_node.setArcsShrink();

		/** Cause-Pin */
		PrimitiveNode cp_node = PrimitiveNode.newInstance("Cause-Pin", this, 1, 1, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(CA_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_7)
			});
		cp_node.addPrimitivePortsFixed(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, cp_node, new ArcProto [] {causalArc}, "cause", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		cp_node.setFunction(PrimitiveNode.Function.PIN);
		cp_node.setArcsWipe();
		cp_node.setArcsShrink();

		/** Prereq-Pin */
		PrimitiveNode pp_node = PrimitiveNode.newInstance("Prereq-Pin", this, 1, 1, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(PA_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_7)
			});
		pp_node.addPrimitivePortsFixed(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp_node, new ArcProto [] {prerequisiteArc}, "prereq", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		pp_node.setFunction(PrimitiveNode.Function.PIN);
		pp_node.setArcsWipe();
		pp_node.setArcsShrink();

		/** Nondet-Pin */
		PrimitiveNode np_node = PrimitiveNode.newInstance("Nondet-Pin", this, 1, 1, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(NA_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_7)
			});
		np_node.addPrimitivePortsFixed(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, np_node, new ArcProto [] {nondeterministicArc}, "nondet", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		np_node.setFunction(PrimitiveNode.Function.PIN);
		np_node.setArcsWipe();
		np_node.setArcsShrink();

		/** Fork-Pin */
		PrimitiveNode fp_node = PrimitiveNode.newInstance("Fork-Pin", this, 1, 1, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(FA_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_7)
			});
		fp_node.addPrimitivePortsFixed(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, fp_node, new ArcProto [] {nondeterministicForkArc}, "fork", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		fp_node.setFunction(PrimitiveNode.Function.PIN);
		fp_node.setArcsWipe();
		fp_node.setArcsShrink();

		/** Element */
		e_node = PrimitiveNode.newInstance("Element", this, 8, 8, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(E_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, box_7)
			});
		e_node.addPrimitivePortsFixed(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, e_node, new ArcProto [] {generalArc, temporalArc, causalArc, prerequisiteArc, nondeterministicArc, nondeterministicForkArc}, "port1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromCenter(0.5), EdgeH.fromLeft(2), EdgeV.fromCenter(0.5)),
				PrimitivePort.newInstance(this, e_node, new ArcProto [] {generalArc, temporalArc, causalArc, prerequisiteArc, nondeterministicArc, nondeterministicForkArc}, "port2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromCenter(-0.5), EdgeH.fromLeft(2), EdgeV.fromCenter(-0.5)),
				PrimitivePort.newInstance(this, e_node, new ArcProto [] {generalArc, temporalArc, causalArc, prerequisiteArc, nondeterministicArc, nondeterministicForkArc}, "port3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromCenter(-1.5), EdgeH.fromLeft(2), EdgeV.fromCenter(-1.5)),
				PrimitivePort.newInstance(this, e_node, new ArcProto [] {generalArc, temporalArc, causalArc, prerequisiteArc, nondeterministicArc, nondeterministicForkArc}, "port4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromCenter(-2.5), EdgeH.fromLeft(2), EdgeV.fromCenter(-2.5))
			});
		e_node.setFunction(PrimitiveNode.Function.UNKNOWN);

		/** Group */
		PrimitiveNode g_node = PrimitiveNode.newInstance("Group", this, 10, 10, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(E_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_6)
			});
		g_node.addPrimitivePortsFixed(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, g_node, new ArcProto [] {generalArc, temporalArc, causalArc, prerequisiteArc, nondeterministicArc, nondeterministicForkArc}, "group", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		g_node.setFunction(PrimitiveNode.Function.UNKNOWN);

		// Building information for palette
		loadFactoryMenuPalette(GEM.class.getResource("gemMenu.xml"));

		//Foundry
		newFoundry(Foundry.Type.NONE, null);
	};

	//**************************************** METHODS ****************************************

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
 		if (pn == e_node)
		{
			Technology.NodeLayer [] eventLayers = new Technology.NodeLayer[6];
			eventLayers[0] = new Technology.NodeLayer(E_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, box_7);

			String title = "";
			Variable varTitle = n.getVar(ELEMENT_NAME);
			if (varTitle != null) title = varTitle.getPureValue(-1);
			eventLayers[1] = new Technology.NodeLayer(E_lay, 0, Poly.Type.TEXTCENT, Technology.NodeLayer.POINTS, new Technology.TechPoint[] {
				new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.fromTop(1))});
			eventLayers[1].setMessage(title);

			String event1 = "";
			Variable varEvent1 = n.getVar(EVENT_1);
			if (varEvent1 != null) event1 = varEvent1.getPureValue(-1);
			eventLayers[2] = new Technology.NodeLayer(E_lay, 0, Poly.Type.TEXTLEFT, Technology.NodeLayer.POINTS, new Technology.TechPoint[] {
				new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromCenter(0.5))});
			eventLayers[2].setMessage(event1);

			String event2 = "";
			Variable varEvent2 = n.getVar(EVENT_2);
			if (varEvent2 != null) event2 = varEvent2.getPureValue(-1);
			eventLayers[3] = new Technology.NodeLayer(E_lay, 0, Poly.Type.TEXTLEFT, Technology.NodeLayer.POINTS, new Technology.TechPoint[] {
				new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromCenter(-0.5))});
			eventLayers[3].setMessage(event2);

			String event3 = "";
			Variable varEvent3 = n.getVar(EVENT_3);
			if (varEvent3 != null) event3 = varEvent3.getPureValue(-1);
			eventLayers[4] = new Technology.NodeLayer(E_lay, 0, Poly.Type.TEXTLEFT, Technology.NodeLayer.POINTS, new Technology.TechPoint[] {
				new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromCenter(-1.5))});
			eventLayers[4].setMessage(event3);

			String event4 = "";
			Variable varEvent4 = n.getVar(EVENT_4);
			if (varEvent4 != null) event4 = varEvent4.getPureValue(-1);
			eventLayers[5] = new Technology.NodeLayer(E_lay, 0, Poly.Type.TEXTLEFT, Technology.NodeLayer.POINTS, new Technology.TechPoint[] {
				new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromCenter(-2.5))});
			eventLayers[5].setMessage(event4);

			primLayers = eventLayers;
			ERectangle fullRectangle = e_node.getFullRectangle();
			EPoint fixupCorrection = EPoint.fromGrid(fullRectangle.getGridWidth(), fullRectangle.getGridHeight());
			for (Technology.NodeLayer nodeLayer: eventLayers)
				nodeLayer.fixup(fixupCorrection);
		}
		b.genShapeOfNode(n, pn, primLayers, null);
	}

	private final double lambdaArrowSize = 1.0;
	private final double lambdaPlusSize = 0.75;
	private final double lambdaPlusGap = 0.5;
	private final double lambdaZigZagWidth = 0.5;
	private final double lambdaDoubleArcWidth = 0.5;

	private final double gridArrowSize = lambdaArrowSize * DBMath.GRID;
	private final double gridPlusSize = lambdaPlusSize * DBMath.GRID;

	/**
	 * Fill the polygons that describe arc "a".
	 * @param b AbstractShapeBuilder to fill polygons.
	 * @param a the ImmutableArcInst that is being described.
	 * @param graphicsOverride the graphics to use for all generated polygons (if not null).
	 */
	@Override
	protected void getShapeOfArc(AbstractShapeBuilder b, ImmutableArcInst a, EGraphics graphicsOverride)
	{
		ArcProto ap = getArcProto(a.protoId);
		assert ap.getTechnology() == this;

		if (ap == generalArc)
		{
			Layer layer = GA_lay;
			if (b.skipLayer(layer)) return;

			b.pushPoint(a.headLocation);
			b.pushPoint(a.tailLocation);
			b.pushPoly(Poly.Type.VECTORS, layer, graphicsOverride, null);
			return;
		}

		if (ap == prerequisiteArc)
		{
			Layer layer = PA_lay;
			if (b.skipLayer(layer)) return;

			b.pushPoint(a.headLocation);
			b.pushPoint(a.tailLocation);
			b.pushPoly(Poly.Type.VECTORS, layer, graphicsOverride, null);
			if (a.isHeadArrowed())
				addHeadArrow(b, a, graphicsOverride, layer, a.headLocation);
			return;
		}

		if (ap == nondeterministicForkArc)
		{
			Layer layer = FA_lay;
			if (b.skipLayer(layer)) return;

			EPoint newTail = a.tailLocation;
			if (a.isTailArrowed())
				newTail = addTailPlus(b, a, graphicsOverride, layer);

			b.pushPoint(a.headLocation);
			b.pushPoint(newTail);
			b.pushPoly(Poly.Type.VECTORS, layer, graphicsOverride, null);

			if (a.isHeadArrowed())
				addHeadArrow(b, a, graphicsOverride, layer, a.headLocation);
			return;
		}

		if (ap == nondeterministicArc)
		{
			Layer layer = NA_lay;
			if (b.skipLayer(layer)) return;

			EPoint newHead = a.headLocation;
			if (a.isHeadArrowed())
			{
				newHead = addHeadPlus(b, a, graphicsOverride, layer);
				addHeadArrow(b, a, graphicsOverride, layer, newHead);
			}

			b.pushPoint(a.tailLocation);
			b.pushPoint(newHead);
			b.pushPoly(Poly.Type.VECTORS, layer, graphicsOverride, null);

			return;
		}

		if (ap == temporalArc)
		{
			Layer layer = TA_lay;
			if (b.skipLayer(layer)) return;

			addDoubleLineArc(b, a, graphicsOverride, layer);
			return;
		}

		if (ap == causalArc)
		{
			Layer layer = CA_lay;
			if (b.skipLayer(layer)) return;

			addZigZagArc(b, a, graphicsOverride, layer);
			return;
		}
	}

	/**
	 * Tells if arc can be drawn by simplified algorithm
	 * Arcs with user-specified color or pattern are not easy
	 * @param a arc to test
	 * @param explain if true then print explanation why arc is not easy
	 * @return true if arc can be drawn by simplified algorithm
	 */
	@Override
	public boolean isEasyShape(ImmutableArcInst a, boolean explain)
	{
		ArcProto ap = getArcProto(a.protoId);
		if (ap != generalArc)
		{
			if (explain) System.out.println("GEM_ARC_SHAPE");
			return false;
		}
		return super.isEasyShape(a, explain);
	}

	private void addHeadArrow(AbstractShapeBuilder b, ImmutableArcInst a, EGraphics graphicsOverride, Layer layer, EPoint headLoc)
	{
		int angle = a.getAngle();
		angle = (angle + 1800) % 3600;
		int angleOfArrow = 300;		// 30 degrees
		int backAngle1 = angle - angleOfArrow;
		int backAngle2 = angle + angleOfArrow;
		b.pushPoint(headLoc);
		b.pushPoint(headLoc, DBMath.cos(backAngle1)*gridArrowSize, DBMath.sin(backAngle1)*gridArrowSize);
		b.pushPoint(headLoc);
		b.pushPoint(headLoc, DBMath.cos(backAngle2)*gridArrowSize, DBMath.sin(backAngle2)*gridArrowSize);
		b.pushPoly(Poly.Type.VECTORS, layer, graphicsOverride, null);
	}

	private EPoint addTailPlus(AbstractShapeBuilder b, ImmutableArcInst a, EGraphics graphicsOverride, Layer layer)
	{
		int angle = a.getAngle();
		int angleOfPlus = 900;		// 90 degrees
		int backAngleLeft = angle - angleOfPlus;
		int backAngleRight = angle + angleOfPlus;
		EPoint plusCenter = new EPoint(a.tailLocation.getX() + DBMath.cos(angle)*lambdaPlusSize,
			a.tailLocation.getY() + DBMath.sin(angle)*lambdaPlusSize);
		b.pushPoint(plusCenter, DBMath.cos(backAngleLeft)*gridPlusSize, DBMath.sin(backAngleLeft)*gridPlusSize);
		b.pushPoint(plusCenter, DBMath.cos(backAngleRight)*gridPlusSize, DBMath.sin(backAngleRight)*gridPlusSize);
		b.pushPoint(a.tailLocation);
		b.pushPoint(a.tailLocation, DBMath.cos(angle)*gridPlusSize*2, DBMath.sin(angle)*gridPlusSize*2);
		b.pushPoly(Poly.Type.VECTORS, layer, graphicsOverride, null);
		EPoint newTail = new EPoint(a.tailLocation.getX() + DBMath.cos(angle)*(lambdaPlusSize*2+lambdaPlusGap),
			a.tailLocation.getY() + DBMath.sin(angle)*(lambdaPlusSize*2+lambdaPlusGap));
		return newTail;
	}

	private EPoint addHeadPlus(AbstractShapeBuilder b, ImmutableArcInst a, EGraphics graphicsOverride, Layer layer)
	{
		int angle = a.getAngle();
		angle = (angle + 1800) % 3600;
		int angleOfPlus = 900;		// 90 degrees
		int backAngleLeft = angle - angleOfPlus;
		int backAngleRight = angle + angleOfPlus;
		EPoint plusCenter = new EPoint(a.headLocation.getX() + DBMath.cos(angle)*lambdaPlusSize,
			a.headLocation.getY() + DBMath.sin(angle)*lambdaPlusSize);
		b.pushPoint(plusCenter, DBMath.cos(backAngleLeft)*gridPlusSize, DBMath.sin(backAngleLeft)*gridPlusSize);
		b.pushPoint(plusCenter, DBMath.cos(backAngleRight)*gridPlusSize, DBMath.sin(backAngleRight)*gridPlusSize);
		b.pushPoint(a.headLocation);
		b.pushPoint(a.headLocation, DBMath.cos(angle)*gridPlusSize*2, DBMath.sin(angle)*gridPlusSize*2);
		b.pushPoly(Poly.Type.VECTORS, layer, graphicsOverride, null);
		EPoint newHead = new EPoint(a.headLocation.getX() + DBMath.cos(angle)*(lambdaPlusSize*2+lambdaPlusGap),
			a.headLocation.getY() + DBMath.sin(angle)*(lambdaPlusSize*2+lambdaPlusGap));
		return newHead;
	}

	private void addDoubleLineArc(AbstractShapeBuilder b, ImmutableArcInst a, EGraphics graphicsOverride, Layer layer)
	{
		int angle = a.getAngle();
		int angleOfArrow = 900;		// 90 degrees
		angle = (angle + 1800) % 3600;
		int backAngle1 = angle - angleOfArrow;
		int backAngle2 = angle + angleOfArrow;
		double indentHeadX = a.headLocation.getX();
		double indentHeadY = a.headLocation.getY();
		if (a.isHeadArrowed())
		{
			indentHeadX += DBMath.cos(angle)*lambdaDoubleArcWidth;
			indentHeadY += DBMath.sin(angle)*lambdaDoubleArcWidth;
		}
		EPoint headLeft = new EPoint(indentHeadX + DBMath.cos(backAngle1)*lambdaDoubleArcWidth,
			indentHeadY + DBMath.sin(backAngle1)*lambdaDoubleArcWidth);
		EPoint headRight = new EPoint(indentHeadX + DBMath.cos(backAngle2)*lambdaDoubleArcWidth,
			indentHeadY + DBMath.sin(backAngle2)*lambdaDoubleArcWidth);
		EPoint tailLeft = new EPoint(a.tailLocation.getX() + DBMath.cos(backAngle1)*lambdaDoubleArcWidth,
			a.tailLocation.getY() + DBMath.sin(backAngle1)*lambdaDoubleArcWidth);
		EPoint tailRight = new EPoint(a.tailLocation.getX() + DBMath.cos(backAngle2)*lambdaDoubleArcWidth,
			a.tailLocation.getY() + DBMath.sin(backAngle2)*lambdaDoubleArcWidth);
		b.pushPoint(tailLeft);
		b.pushPoint(headLeft);
		b.pushPoint(tailRight);
		b.pushPoint(headRight);
		if (a.isHeadArrowed())
		{
			angleOfArrow = 450;		// 45 degrees
			backAngle1 = angle - angleOfArrow;
			backAngle2 = angle + angleOfArrow;
			EPoint headArrowLeft = new EPoint(a.headLocation.getX() + DBMath.cos(backAngle1)*lambdaDoubleArcWidth*2,
				a.headLocation.getY() + DBMath.sin(backAngle1)*lambdaDoubleArcWidth*2);
			EPoint headArrowRight = new EPoint(a.headLocation.getX() + DBMath.cos(backAngle2)*lambdaDoubleArcWidth*2,
				a.headLocation.getY() + DBMath.sin(backAngle2)*lambdaDoubleArcWidth*2);
			b.pushPoint(a.headLocation);
			b.pushPoint(headArrowLeft);
			b.pushPoint(a.headLocation);
			b.pushPoint(headArrowRight);
		}
		b.pushPoly(Poly.Type.VECTORS, layer, graphicsOverride, null);
	}

	private void addZigZagArc(AbstractShapeBuilder b, ImmutableArcInst a, EGraphics graphicsOverride, Layer layer)
	{
		int angle = a.getAngle();
		int angleOfArrow = 900;		// 90 degrees
		int backAngle1 = angle - angleOfArrow;
		int backAngle2 = angle + angleOfArrow;
		double centerX = (a.headLocation.getX() + a.tailLocation.getX()) / 2;
		double centerY = (a.headLocation.getY() + a.tailLocation.getY()) / 2;
		EPoint centerLeft = new EPoint(centerX + DBMath.cos(backAngle1)*lambdaZigZagWidth,
			centerY + DBMath.sin(backAngle1)*lambdaZigZagWidth);
		EPoint centerRight = new EPoint(centerX + DBMath.cos(backAngle2)*lambdaZigZagWidth,
			centerY + DBMath.sin(backAngle2)*lambdaZigZagWidth);
		b.pushPoint(a.headLocation);
		b.pushPoint(centerLeft);
		b.pushPoint(centerLeft);
		b.pushPoint(centerRight);
		b.pushPoint(centerRight);
		b.pushPoint(a.tailLocation);
		b.pushPoly(Poly.Type.VECTORS, layer, graphicsOverride, null);
	}
}
