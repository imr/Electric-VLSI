/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Generic.java
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
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;

import java.util.Iterator;

/**
 * This is the Generic technology.
 */
public class Generic extends Technology
{
	/** the Generic Technology object. */	public static final Generic tech = new Generic();
	/** the Universal Pin node. */			public PrimitiveNode universalPin_node;
	/** the Invisible Pin node. */			public PrimitiveNode invisiblePin_node;
	/** the Unrouted Pin node. */			public PrimitiveNode unroutedPin_node;
	/** the Cell-Center node. */			public PrimitiveNode cellCenter_node;
	/** the Port-definition node. */		public PrimitiveNode port_node;
	/** the DRC exclusion node. */			public PrimitiveNode drc_node;
	/** the Essential-bounds node. */		public PrimitiveNode essentialBounds_node;
	/** the Simulation-Probe node. */		public PrimitiveNode simProbe_node;
	/** the Universal arc. */				public PrimitiveArc universal_arc;
	/** the Invisible arc. */				public PrimitiveArc invisible_arc;
	/** the Unrouted arc. */				public PrimitiveArc unrouted_arc;

	private PrimitivePort univPinPort;
	private PrimitivePort invisPinPort;

	// -------------------- private and protected methods ------------------------
	private Generic()
	{
		setTechName("generic");
		setTechDesc("Useful primitives");
		setScale(2000);

		//**************************************** LAYERS ****************************************

		/** Universal layer */
		Layer universal_lay = Layer.newInstance("Universal",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 0,0,0,1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** Invisible layer */
		Layer invisible_lay = Layer.newInstance("Invisible",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 0,0,0,1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** Unrouted layer */
		Layer unrouted_lay = Layer.newInstance("Unrouted",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 0,0,0,1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** Glyph layer */
		Layer glyph_lay = Layer.newInstance("Glyph",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 0,0,0,1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** DRC layer */
		Layer drc_lay = Layer.newInstance("DRC",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 0,0,0,1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** Simulation Probe layer */
		Layer simprobe_lay = Layer.newInstance("Sim-Probe",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 0,0,0,1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		// The layer functions
		universal_lay.setFunction(Layer.Function.UNKNOWN);											// Universal
		invisible_lay.setFunction(Layer.Function.UNKNOWN, Layer.Function.NONELEC);					// Invisible
		unrouted_lay.setFunction(Layer.Function.UNKNOWN);											// Unrouted
		glyph_lay.setFunction(Layer.Function.ART, Layer.Function.NONELEC);							// Glyph
		drc_lay.setFunction(Layer.Function.ART, Layer.Function.NONELEC);							// DRC
		simprobe_lay.setFunction(Layer.Function.ART, Layer.Function.NONELEC);						// Sim probe

		//**************************************** ARCS ****************************************

		/** Universal arc */
		universal_arc = PrimitiveArc.newInstance(this, "Universal", 0.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(universal_lay, 0, Poly.Type.FILLED)
		});
		universal_arc.setFunction(PrimitiveArc.Function.UNKNOWN);
		universal_arc.setFixedAngle();
		universal_arc.setAngleIncrement(45);

		/** Invisible arc */
		invisible_arc = PrimitiveArc.newInstance(this, "Invisible", 0.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(invisible_lay, 0, Poly.Type.FILLED)
		});
		invisible_arc.setFunction(PrimitiveArc.Function.NONELEC);
		invisible_arc.setFixedAngle();
		invisible_arc.setAngleIncrement(45);

		/** Unrouted arc */
		unrouted_arc = PrimitiveArc.newInstance(this, "Unrouted", 0.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(unrouted_lay, 0, Poly.Type.FILLED)
		});
		unrouted_arc.setFunction(PrimitiveArc.Function.UNROUTED);
		unrouted_arc.setFixedAngle();
		unrouted_arc.setAngleIncrement(0);

		//**************************************** NODES ****************************************

		/** Universal pin */
		univPinPort = PrimitivePort.newInstance(this, universalPin_node, new ArcProto[] {universal_arc}, "univ", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.CENTER, EdgeH.CENTER, EdgeV.CENTER);
		universalPin_node = PrimitiveNode.newInstance("Universal-Pin", this, 1.0, 1.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(universal_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.CENTER)})
			});
		universalPin_node.addPrimitivePorts(new PrimitivePort [] {univPinPort});
		universalPin_node.setFunction(NodeProto.Function.PIN);
		universalPin_node.setWipeOn1or2();
		universalPin_node.setHoldsOutline();

		/** Invisible pin */
		invisPinPort = PrimitivePort.newInstance(this, invisiblePin_node, new ArcProto[] {invisible_arc}, "center", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.CENTER, EdgeH.CENTER, EdgeV.CENTER);
		invisiblePin_node = PrimitiveNode.newInstance("Invisible-Pin", this, 1.0, 1.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(invisible_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		invisiblePin_node.addPrimitivePorts(new PrimitivePort [] {invisPinPort});
		invisiblePin_node.setFunction(NodeProto.Function.PIN);
		invisiblePin_node.setWipeOn1or2();

		/** Unrouted pin */
		unroutedPin_node = PrimitiveNode.newInstance("Unrouted-Pin", this, 1.0, 1.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(unrouted_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.CENTER)})
			});
		unroutedPin_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, unroutedPin_node, new ArcProto[] {unrouted_arc}, "unrouted", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.CENTER, EdgeH.CENTER, EdgeV.CENTER)
			});
		unroutedPin_node.setFunction(NodeProto.Function.PIN);
		unroutedPin_node.setWipeOn1or2();

		/** Cell Center */
		cellCenter_node = PrimitiveNode.newInstance("Facet-Center", this, 0.0, 0.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(glyph_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(glyph_lay, 0, Poly.Type.BIGCROSS, Technology.NodeLayer.POINTS, Technology.TechPoint.ATCENTER)
			});
		cellCenter_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, cellCenter_node, new ArcProto[] {invisible_arc, universal_arc}, "center", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		cellCenter_node.setFunction(NodeProto.Function.ART);

		/** Port */
		port_node = PrimitiveNode.newInstance("Port", this, 6.0, 6.0, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(glyph_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.IN2BOX)
//				new Technology.NodeLayer(glyph_lay, 0, 3, Poly.Type.OPENED, Technology.NodeLayer.POINTS, Technology.TechPoint.FULLBOX)
			});
		port_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, port_node, new ArcProto[] {invisible_arc, universal_arc}, "center", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		port_node.setFunction(NodeProto.Function.ART);

		/** DRC Node */
		drc_node = PrimitiveNode.newInstance("DRC-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(glyph_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		drc_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, drc_node, new ArcProto[] {invisible_arc, universal_arc}, "center", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.CENTER, EdgeH.CENTER, EdgeV.CENTER)
			});
		drc_node.setFunction(NodeProto.Function.NODE);
		drc_node.setHoldsOutline();

		/** Essential Bounds Node */
		essentialBounds_node = PrimitiveNode.newInstance("Essential-Bounds", this, 0.0, 0.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(glyph_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-1), EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.fromCenter(-1))})
			});
		essentialBounds_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, essentialBounds_node, new ArcProto[] {invisible_arc, universal_arc}, "center", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		essentialBounds_node.setFunction(NodeProto.Function.ART);

		/** Simulation Probe Node */
		simProbe_node = PrimitiveNode.newInstance("Simulation-Probe", this, 10.0, 10.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(simprobe_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		simProbe_node.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, simProbe_node, new ArcProto[] {invisible_arc, universal_arc}, "center", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		simProbe_node.setFunction(NodeProto.Function.ART);
	}

	/**
	 * Routine to update the connecitivity list for universal and invisible pins so that
	 * they can connect to ALL arcs.  This is called at initialization and again
	 * whenever the number of technologies changes.
	 */
	public void makeUnivList()
	{
		// count the number of arcs in all technologies
		int tot = 0;
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator ait = tech.getArcs(); ait.hasNext(); )
			{
				ArcProto ap = (ArcProto)ait.next();
				tot++;
			}
		}

		// make an array for each arc
		ArcProto [] upconn = new ArcProto[tot];

		// fill the array
		tot = 0;
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator ait = tech.getArcs(); ait.hasNext(); )
			{
				ArcProto ap = (ArcProto)ait.next();
				upconn[tot++] = ap;
			}
		}

		// store the array in this technology
		univPinPort.setConnections(upconn);
		invisPinPort.setConnections(upconn);
	}

	/**
	 * Routine to convert old primitive names to their proper NodeProtos.
	 * @param name the name of the old primitive.
	 * @return the proper PrimitiveNode to use (or null if none can be determined).
	 */
	public PrimitiveNode convertOldNodeName(String name)
	{
		if (name.equals("Cell-Center")) return(cellCenter_node);
		return null;
	}
}
