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
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.EditWindow0;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.*;



/**
 * This is the Temporal Specification Facility (from Lansky) Technology.
 */
public class GEM extends Technology
{
	/** the Temporal Specification Facility (from Lansky) Technology object. */	public static final GEM tech = new GEM();

	/** Variable key for GEM element name. */		public static final Variable.Key ELEMENT_NAME = Variable.newKey("GEM_element");
	/** Variable key for GEM event 1. */			public static final Variable.Key EVENT_1 = Variable.newKey("GEM_event1");
	/** Variable key for GEM event 2. */			public static final Variable.Key EVENT_2 = Variable.newKey("GEM_event2");
	/** Variable key for GEM event 3. */			public static final Variable.Key EVENT_3 = Variable.newKey("GEM_event3");
	/** Variable key for GEM event 4. */			public static final Variable.Key EVENT_4 = Variable.newKey("GEM_event4");

	private Layer E_lay;
	private Technology.TechPoint [] box_7;
	private PrimitiveNode e_node;

	// -------------------- private and protected methods ------------------------
	private GEM()
	{
		super("gem");
		setTechDesc("Temporal Specification Facility (from Lansky)");
		setFactoryScale(1000, false);   // in nanometers: really 1 microns
		setNoNegatedArcs();

        //Foundry
        Foundry noFoundry = new Foundry(Foundry.Type.NONE);
        foundries.add(noFoundry);
        
		setStaticTechnology();
		setNonStandard();

		//**************************************** LAYERS ****************************************

		/** E layer */
		E_lay = Layer.newInstance(this, "Element",
			new EGraphics(false, false, null, 0, 255,0,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** GA layer */
		Layer GA_lay = Layer.newInstance(this, "General-arc",
			new EGraphics(false, false, null, 0, 0,0,255, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** TA layer */
		Layer TA_lay = Layer.newInstance(this, "Temporal-arc",
			new EGraphics(false, false, null, 0, 0,255,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** CA layer */
		Layer CA_lay = Layer.newInstance(this, "Causal-arc",
			new EGraphics(false, false, null, 0, 0,0,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** PA layer */
		Layer PA_lay = Layer.newInstance(this, "Prereq-arc",
			new EGraphics(false, false, null, 0, 255,190,6, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** NA layer */
		Layer NA_lay = Layer.newInstance(this, "Nondet-arc",
			new EGraphics(false, false, null, 0, 255,255,0, 0.8,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** FA layer */
		Layer FA_lay = Layer.newInstance(this, "Fork-arc",
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
		ArcProto General_arc = ArcProto.newInstance(this, "General", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(GA_lay, 0, Poly.Type.FILLED)
		});
		General_arc.setFunction(ArcProto.Function.NONELEC);
		General_arc.setWipable();
		General_arc.setFactoryAngleIncrement(0);

		/** Temporal arc */
		ArcProto Temporal_arc = ArcProto.newInstance(this, "Temporal", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(TA_lay, 0, Poly.Type.FILLED)
		});
		Temporal_arc.setFunction(ArcProto.Function.NONELEC);
		Temporal_arc.setWipable();
		Temporal_arc.setFactoryAngleIncrement(0);

		/** Causal arc */
		ArcProto Causal_arc = ArcProto.newInstance(this, "Causal", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(CA_lay, 0, Poly.Type.FILLED)
		});
		Causal_arc.setFunction(ArcProto.Function.NONELEC);
		Causal_arc.setWipable();
		Causal_arc.setFactoryAngleIncrement(0);

		/** Prerequisite arc */
		ArcProto Prerequisite_arc = ArcProto.newInstance(this, "Prerequisite", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(PA_lay, 0, Poly.Type.FILLED)
		});
		Prerequisite_arc.setFunction(ArcProto.Function.NONELEC);
		Prerequisite_arc.setWipable();
		Prerequisite_arc.setFactoryAngleIncrement(0);

		/** Nondeterministic arc */
		ArcProto Nondeterministic_arc = ArcProto.newInstance(this, "Nondeterministic", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(NA_lay, 0, Poly.Type.FILLED)
		});
		Nondeterministic_arc.setFunction(ArcProto.Function.NONELEC);
		Nondeterministic_arc.setWipable();
		Nondeterministic_arc.setFactoryAngleIncrement(0);

		/** Nondeterministic-fork arc */
		ArcProto Nondeterministic_fork_arc = ArcProto.newInstance(this, "Nondeterministic-fork", 0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(FA_lay, 0, Poly.Type.FILLED)
		});
		Nondeterministic_fork_arc.setFunction(ArcProto.Function.NONELEC);
		Nondeterministic_fork_arc.setWipable();
		Nondeterministic_fork_arc.setFactoryAngleIncrement(0);

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
		PrimitivePort pinPort = PrimitivePort.newInstance(this, gp_node, new ArcProto [] {General_arc}, "general", 0,180, 0, PortCharacteristic.UNKNOWN,
			EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter());
		gp_node.addPrimitivePorts(new PrimitivePort[] {pinPort});
		gp_node.setFunction(PrimitiveNode.Function.PIN);
		gp_node.setArcsWipe();
		gp_node.setArcsShrink();

		/** Temporal-Pin */
		PrimitiveNode tp_node = PrimitiveNode.newInstance("Temporal-Pin", this, 1, 1, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(TA_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, box_7)
			});
		tp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, tp_node, new ArcProto [] {Temporal_arc}, "temporal", 0,180, 0, PortCharacteristic.UNKNOWN,
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
		cp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, cp_node, new ArcProto [] {Causal_arc}, "cause", 0,180, 0, PortCharacteristic.UNKNOWN,
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
		pp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, pp_node, new ArcProto [] {Prerequisite_arc}, "prereq", 0,180, 0, PortCharacteristic.UNKNOWN,
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
		np_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, np_node, new ArcProto [] {Nondeterministic_arc}, "nondet", 0,180, 0, PortCharacteristic.UNKNOWN,
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
		fp_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, fp_node, new ArcProto [] {Nondeterministic_fork_arc}, "fork", 0,180, 0, PortCharacteristic.UNKNOWN,
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
		e_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, e_node, new ArcProto [] {General_arc, Temporal_arc, Causal_arc, Prerequisite_arc, Nondeterministic_arc, Nondeterministic_fork_arc}, "port1", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromCenter(0.5), EdgeH.fromLeft(2), EdgeV.fromCenter(0.5)),
				PrimitivePort.newInstance(this, e_node, new ArcProto [] {General_arc, Temporal_arc, Causal_arc, Prerequisite_arc, Nondeterministic_arc, Nondeterministic_fork_arc}, "port2", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromCenter(-0.5), EdgeH.fromLeft(2), EdgeV.fromCenter(-0.5)),
				PrimitivePort.newInstance(this, e_node, new ArcProto [] {General_arc, Temporal_arc, Causal_arc, Prerequisite_arc, Nondeterministic_arc, Nondeterministic_fork_arc}, "port3", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromCenter(-1.5), EdgeH.fromLeft(2), EdgeV.fromCenter(-1.5)),
				PrimitivePort.newInstance(this, e_node, new ArcProto [] {General_arc, Temporal_arc, Causal_arc, Prerequisite_arc, Nondeterministic_arc, Nondeterministic_fork_arc}, "port4", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.fromLeft(2), EdgeV.fromCenter(-2.5), EdgeH.fromLeft(2), EdgeV.fromCenter(-2.5))
			});
		e_node.setFunction(PrimitiveNode.Function.UNKNOWN);

		/** Group */
		PrimitiveNode g_node = PrimitiveNode.newInstance("Group", this, 10, 10, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(E_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, box_6)
			});
		g_node.addPrimitivePorts(new PrimitivePort[]
			{
				PrimitivePort.newInstance(this, g_node, new ArcProto [] {General_arc, Temporal_arc, Causal_arc, Prerequisite_arc, Nondeterministic_arc, Nondeterministic_fork_arc}, "group", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		g_node.setFunction(PrimitiveNode.Function.UNKNOWN);

        // Building information for palette
        nodeGroups = new Object[9][2];
        int count = -1;

        nodeGroups[++count][0] = General_arc; nodeGroups[count][1] = gp_node;
        nodeGroups[++count][0] = Temporal_arc; nodeGroups[count][1] = tp_node;
        nodeGroups[++count][0] = Causal_arc; nodeGroups[count][1] = cp_node;
        nodeGroups[++count][0] = Prerequisite_arc; nodeGroups[count][1] = pp_node;
        nodeGroups[++count][0] = Nondeterministic_arc; nodeGroups[count][1] = np_node;
        nodeGroups[++count][0] = Nondeterministic_fork_arc; nodeGroups[count][1] = fp_node;
        nodeGroups[++count][0] = "Cell"; nodeGroups[count][1] = e_node;
        nodeGroups[++count][0] = "Misc."; nodeGroups[count][1] = g_node;
        nodeGroups[++count][0] = "Pure.";
	};

	//**************************************** METHODS ****************************************

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
	public Poly [] getShapeOfNode(NodeInst ni, EditWindow0 wnd, VarContext context, boolean electrical, boolean reasonable, Technology.NodeLayer [] primLayers, Layer layerOverride)
	{
		if (ni.getProto() == e_node)
		{
			Technology.NodeLayer [] eventLayers = new Technology.NodeLayer[6];
			eventLayers[0] = new Technology.NodeLayer(E_lay, 0, Poly.Type.CIRCLE, Technology.NodeLayer.POINTS, box_7);

			String title = "";
			Variable varTitle = ni.getVar(ELEMENT_NAME);
			if (varTitle != null) title = varTitle.getPureValue(-1);
			eventLayers[1] = new Technology.NodeLayer(E_lay, 0, Poly.Type.TEXTCENT, Technology.NodeLayer.POINTS, new Technology.TechPoint[] {
				new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.fromTop(1))});
			eventLayers[1].setMessage(title);

			String event1 = "";
			Variable varEvent1 = ni.getVar(EVENT_1);
			if (varEvent1 != null) event1 = varEvent1.getPureValue(-1);
			eventLayers[2] = new Technology.NodeLayer(E_lay, 0, Poly.Type.TEXTLEFT, Technology.NodeLayer.POINTS, new Technology.TechPoint[] {
				new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromCenter(0.5))});
			eventLayers[2].setMessage(event1);

			String event2 = "";
			Variable varEvent2 = ni.getVar(EVENT_2);
			if (varEvent2 != null) event2 = varEvent2.getPureValue(-1);
			eventLayers[3] = new Technology.NodeLayer(E_lay, 0, Poly.Type.TEXTLEFT, Technology.NodeLayer.POINTS, new Technology.TechPoint[] {
				new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromCenter(-0.5))});
			eventLayers[3].setMessage(event2);

			String event3 = "";
			Variable varEvent3 = ni.getVar(EVENT_3);
			if (varEvent3 != null) event3 = varEvent3.getPureValue(-1);
			eventLayers[4] = new Technology.NodeLayer(E_lay, 0, Poly.Type.TEXTLEFT, Technology.NodeLayer.POINTS, new Technology.TechPoint[] {
				new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromCenter(-1.5))});
			eventLayers[4].setMessage(event3);

			String event4 = "";
			Variable varEvent4 = ni.getVar(EVENT_4);
			if (varEvent4 != null) event4 = varEvent4.getPureValue(-1);
			eventLayers[5] = new Technology.NodeLayer(E_lay, 0, Poly.Type.TEXTLEFT, Technology.NodeLayer.POINTS, new Technology.TechPoint[] {
				new Technology.TechPoint(EdgeH.fromLeft(2), EdgeV.fromCenter(-2.5))});
			eventLayers[5].setMessage(event4);

			primLayers = eventLayers;
		}
		return super.getShapeOfNode(ni, wnd, context, electrical, reasonable, primLayers, layerOverride);
	}
}
