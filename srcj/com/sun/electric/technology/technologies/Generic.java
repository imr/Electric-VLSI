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
	/** the Universal Pin node. */			public PrimitiveNode universalPinNode;
	/** the Invisible Pin node. */			public PrimitiveNode invisiblePinNode;
	/** the Unrouted Pin node. */			public PrimitiveNode unroutedPinNode;
	/** the Cell-Center node. */			public PrimitiveNode cellCenterNode;
	/** the Port-definition node. */		public PrimitiveNode portNode;
	/** the DRC exclusion node. */			public PrimitiveNode drcNode;
	/** the Essential-bounds node. */		public PrimitiveNode essentialBoundsNode;
	/** the Simulation-Probe node. */		public PrimitiveNode simProbeNode;
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
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 180,180,180,1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** Unrouted layer */
		Layer unrouted_lay = Layer.newInstance("Unrouted",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 100,100,100,1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** Glyph layer */
		Layer glyph_lay = Layer.newInstance("Glyph",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 0,0,0,1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** DRC layer */
		Layer drc_lay = Layer.newInstance("DRC",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 255,190,6,1.0,1,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** Simulation Probe layer */
		Layer simprobe_lay = Layer.newInstance("Sim-Probe",
			new EGraphics(EGraphics.SOLIDC, EGraphics.PATTERNED, 0,255,0,1.0,1,
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
		univPinPort = PrimitivePort.newInstance(this, universalPinNode, new ArcProto[] {universal_arc}, "univ", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE);
		universalPinNode = PrimitiveNode.newInstance("Universal-Pin", this, 1.0, 1.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(universal_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.CENTER)})
			});
		universalPinNode.addPrimitivePorts(new PrimitivePort [] {univPinPort});
		universalPinNode.setFunction(NodeProto.Function.PIN);
		universalPinNode.setWipeOn1or2();
		universalPinNode.setHoldsOutline();

		/** Invisible pin */
		invisPinPort = PrimitivePort.newInstance(this, invisiblePinNode, new ArcProto[] {invisible_arc}, "center", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.CENTER, EdgeH.CENTER, EdgeV.CENTER);
		invisiblePinNode = PrimitiveNode.newInstance("Invisible-Pin", this, 1.0, 1.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(invisible_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		invisiblePinNode.addPrimitivePorts(new PrimitivePort [] {invisPinPort});
		invisiblePinNode.setFunction(NodeProto.Function.PIN);
		invisiblePinNode.setWipeOn1or2();

		/** Unrouted pin */
		unroutedPinNode = PrimitiveNode.newInstance("Unrouted-Pin", this, 1.0, 1.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(unrouted_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.RIGHTEDGE, EdgeV.CENTER)})
			});
		unroutedPinNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, unroutedPinNode, new ArcProto[] {unrouted_arc}, "unrouted", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.CENTER, EdgeH.CENTER, EdgeV.CENTER)
			});
		unroutedPinNode.setFunction(NodeProto.Function.PIN);
		unroutedPinNode.setWipeOn1or2();

		/** Cell Center */
		cellCenterNode = PrimitiveNode.newInstance("Facet-Center", this, 0.0, 0.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(glyph_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX),
				new Technology.NodeLayer(glyph_lay, 0, Poly.Type.BIGCROSS, Technology.NodeLayer.POINTS, Technology.TechPoint.ATCENTER)
			});
		cellCenterNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, cellCenterNode, new ArcProto[] {invisible_arc, universal_arc}, "center", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		cellCenterNode.setFunction(NodeProto.Function.ART);

		/** Port */
		portNode = PrimitiveNode.newInstance("Port", this, 6.0, 6.0, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(glyph_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.IN2BOX)
//				new Technology.NodeLayer(glyph_lay, 0, 3, Poly.Type.OPENED, Technology.NodeLayer.POINTS, Technology.TechPoint.FULLBOX)
			});
		portNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, portNode, new ArcProto[] {invisible_arc, universal_arc}, "center", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		portNode.setFunction(NodeProto.Function.ART);

		/** DRC Node */
		drcNode = PrimitiveNode.newInstance("DRC-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(drc_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		drcNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, drcNode, new ArcProto[] {invisible_arc, universal_arc}, "center", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.CENTER, EdgeV.CENTER, EdgeH.CENTER, EdgeV.CENTER)
			});
		drcNode.setFunction(NodeProto.Function.NODE);
		drcNode.setHoldsOutline();

		/** Essential Bounds Node */
		essentialBoundsNode = PrimitiveNode.newInstance("Essential-Bounds", this, 0.0, 0.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(glyph_lay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-1), EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.CENTER),
					new Technology.TechPoint(EdgeH.CENTER, EdgeV.fromCenter(-1))})
			});
		essentialBoundsNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, essentialBoundsNode, new ArcProto[] {invisible_arc, universal_arc}, "center", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		essentialBoundsNode.setFunction(NodeProto.Function.ART);

		/** Simulation Probe Node */
		simProbeNode = PrimitiveNode.newInstance("Simulation-Probe", this, 10.0, 10.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(simprobe_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.FULLBOX)
			});
		simProbeNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, simProbeNode, new ArcProto[] {invisible_arc, universal_arc}, "center", 0,180, 0, PortProto.Characteristic.UNKNOWN,
					EdgeH.LEFTEDGE, EdgeV.BOTTOMEDGE, EdgeH.RIGHTEDGE, EdgeV.TOPEDGE)
			});
		simProbeNode.setFunction(NodeProto.Function.ART);
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
		if (name.equals("Cell-Center")) return(cellCenterNode);
		return null;
	}
}
