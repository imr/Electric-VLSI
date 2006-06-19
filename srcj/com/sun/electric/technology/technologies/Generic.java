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

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.EditWindow0;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.*;

import java.util.Iterator;
import java.awt.*;

/**
 * This is the Generic technology.
 */
public class Generic extends Technology
{
	/** the Generic Technology object. */	public static final Generic tech = new Generic();
	/** the Universal Layer. */				private Layer universalLay;
	/** the Glyph Layer. */					public Layer glyphLay;
	/** the DRC exclusion Layer. */			public Layer drcLay;
	/** the AFG exclusion Layer. */			public Layer afgLay;
	/** the Universal Pin node. */			public PrimitiveNode universalPinNode;
	/** the Invisible Pin node. */			public PrimitiveNode invisiblePinNode;
	/** the Unrouted Pin node. */			public PrimitiveNode unroutedPinNode;
	/** the Cell-Center node. */			public PrimitiveNode cellCenterNode;
	/** the Port-definition node. */		public PrimitiveNode portNode;
	/** the DRC exclusion node. */			public PrimitiveNode drcNode;
	/** the AFG exclusion node. */			public PrimitiveNode afgNode;
	/** the Essential-bounds node. */		public PrimitiveNode essentialBoundsNode;
	/** the Simulation-Probe node. */		public PrimitiveNode simProbeNode;
	/** the Universal arc. */				public ArcProto universal_arc;
	/** the Invisible arc. */				public ArcProto invisible_arc;
	/** the Unrouted arc. */				public ArcProto unrouted_arc;

	private PrimitivePort univPinPort, invisPinPort, simProbePort;

	// -------------------- private and protected methods ------------------------
	private Generic()
	{
		super("generic");
		setTechShortName("Generic");
		setTechDesc("Useful primitives");
		setNonStandard();

        //Foundry
        Foundry noFoundry = new Foundry(Foundry.Type.NONE);
        foundries.add(noFoundry);
        
		setFactoryScale(1000, false);			// in nanometers: really 1 micron

		//**************************************** LAYERS ****************************************

		/** Universal layer */
		universalLay = Layer.newInstance(this, "Universal",
			new EGraphics(false, true, null, 0, 0,0,0,1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** Invisible layer */
		Layer invisible_lay = Layer.newInstance(this, "Invisible",
			new EGraphics(false, true, null, 0, 180,180,180,1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** Unrouted layer */
		Layer unrouted_lay = Layer.newInstance(this, "Unrouted",
			new EGraphics(false, true, null, 0, 100,100,100,1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** Glyph layer */
		glyphLay = Layer.newInstance(this, "Glyph",
			new EGraphics(false, true, null, 0, 0,0,0,1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** DRC layer */
		drcLay = Layer.newInstance(this, "DRC",
			new EGraphics(false, true, null, 0, 255,190,6,1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

        /** AFG layer */
		afgLay = Layer.newInstance(this, "AFG",
			new EGraphics(false, true, null, 0, 255,6,190,1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** Simulation Probe layer */
		Layer simprobe_lay = Layer.newInstance(this, "Sim-Probe",
			new EGraphics(false, true, null, 0, 0,255,0,1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		// The layer functions
		universalLay.setFunction(Layer.Function.UNKNOWN);											// Universal
		invisible_lay.setFunction(Layer.Function.UNKNOWN, Layer.Function.NONELEC);					// Invisible
		unrouted_lay.setFunction(Layer.Function.UNKNOWN);											// Unrouted
		glyphLay.setFunction(Layer.Function.ART, Layer.Function.NONELEC);							// Glyph
		drcLay.setFunction(Layer.Function.ART, Layer.Function.NONELEC);							// DRC
		afgLay.setFunction(Layer.Function.ART, Layer.Function.NONELEC);							// AFG
		simprobe_lay.setFunction(Layer.Function.ART, Layer.Function.NONELEC);						// Sim probe

		//**************************************** ARCS ****************************************

		/** Universal arc */
		universal_arc = ArcProto.newInstance(this, "Universal", 0.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(universalLay, 0, Poly.Type.FILLED)
		});
		universal_arc.setFunction(ArcProto.Function.UNKNOWN);
		universal_arc.setFactoryFixedAngle(true);
		universal_arc.setFactoryAngleIncrement(45);

		/** Invisible arc */
		invisible_arc = ArcProto.newInstance(this, "Invisible", 0.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(invisible_lay, 0, Poly.Type.FILLED)
		});
		invisible_arc.setFunction(ArcProto.Function.NONELEC);
		invisible_arc.setFactoryFixedAngle(true);
		invisible_arc.setFactoryAngleIncrement(45);

		/** Unrouted arc */
		unrouted_arc = ArcProto.newInstance(this, "Unrouted", 0.0, new Technology.ArcLayer []
		{
			new Technology.ArcLayer(unrouted_lay, 0, Poly.Type.FILLED)
		});
		unrouted_arc.setFactoryFixedAngle(false);
		unrouted_arc.setFunction(ArcProto.Function.UNROUTED);
		unrouted_arc.setFactoryAngleIncrement(0);

		//**************************************** NODES ****************************************

		/** Universal pin */
		universalPinNode = PrimitiveNode.newInstance("Universal-Pin", this, 1.0, 1.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(universalLay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())})
			});
		univPinPort = PrimitivePort.newInstance(this, universalPinNode, new ArcProto[] {universal_arc}, "univ", 0,180, 0, PortCharacteristic.UNKNOWN,
			EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge());
		universalPinNode.addPrimitivePorts(new PrimitivePort [] {univPinPort});
		universalPinNode.setFunction(PrimitiveNode.Function.PIN);
		universalPinNode.setWipeOn1or2();
		universalPinNode.setHoldsOutline();
		universalPinNode.setCanBeZeroSize();

		/** Invisible pin */
		invisiblePinNode = PrimitiveNode.newInstance("Invisible-Pin", this, 1.0, 1.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(invisible_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		invisPinPort = PrimitivePort.newInstance(this, invisiblePinNode, new ArcProto[] {invisible_arc,universal_arc}, "center", 0,180, 0, PortCharacteristic.UNKNOWN,
			EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter());
		invisiblePinNode.addPrimitivePorts(new PrimitivePort [] {invisPinPort});
		invisiblePinNode.setFunction(PrimitiveNode.Function.PIN);
		invisiblePinNode.setWipeOn1or2();
		invisiblePinNode.setCanBeZeroSize();

		/** Unrouted pin */
		unroutedPinNode = PrimitiveNode.newInstance("Unrouted-Pin", this, 1.0, 1.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(unrouted_lay, 0, Poly.Type.DISC, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeRightEdge(), EdgeV.makeCenter())})
			});
		unroutedPinNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, unroutedPinNode, new ArcProto[] {unrouted_arc,invisible_arc,universal_arc}, "unrouted", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		unroutedPinNode.setFunction(PrimitiveNode.Function.PIN);
		unroutedPinNode.setWipeOn1or2();
		unroutedPinNode.setCanBeZeroSize();

		/** Cell Center */
		cellCenterNode = PrimitiveNode.newInstance("Facet-Center", this, 0.0, 0.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(glyphLay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox()),
				new Technology.NodeLayer(glyphLay, 0, Poly.Type.BIGCROSS, Technology.NodeLayer.POINTS, Technology.TechPoint.makeCenterBox())
			});
		cellCenterNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, cellCenterNode, new ArcProto[] {invisible_arc,universal_arc}, "center", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		cellCenterNode.setFunction(PrimitiveNode.Function.ART);
		cellCenterNode.setCanBeZeroSize();

		/** Port */
		portNode = PrimitiveNode.newInstance("Port", this, 6.0, 6.0, new SizeOffset(2, 2, 2, 2),
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(glyphLay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeIndented(2))
			});
		portNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, portNode, new ArcProto[] {invisible_arc,universal_arc}, "center", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		portNode.setFunction(PrimitiveNode.Function.ART);
		portNode.setCanBeZeroSize();

		/** DRC Node */
		drcNode = PrimitiveNode.newInstance("DRC-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(drcLay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		drcNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, drcNode, new ArcProto[] {invisible_arc,universal_arc}, "center", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		drcNode.setFunction(PrimitiveNode.Function.NODE);
		drcNode.setHoldsOutline();

        /** AFG Node */
		afgNode = PrimitiveNode.newInstance("AFG-Node", this, 2.0, 2.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(afgLay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		afgNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, afgNode, new ArcProto[] {invisible_arc,universal_arc}, "center", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		afgNode.setFunction(PrimitiveNode.Function.NODE);
		afgNode.setHoldsOutline();

		/** Essential Bounds Node */
		essentialBoundsNode = PrimitiveNode.newInstance("Essential-Bounds", this, 0.0, 0.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(glyphLay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-1), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.fromCenter(-1))})
			});
		essentialBoundsNode.addPrimitivePorts(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, essentialBoundsNode, new ArcProto[] {invisible_arc,universal_arc}, "center", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge())
			});
		essentialBoundsNode.setFunction(PrimitiveNode.Function.ART);
		essentialBoundsNode.setCanBeZeroSize();

		/** Simulation Probe Node */
		simProbeNode = PrimitiveNode.newInstance("Simulation-Probe", this, 10.0, 10.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(simprobe_lay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});

		simProbePort = PrimitivePort.newInstance(this, simProbeNode, new ArcProto[] {invisible_arc,universal_arc}, "center", 0,180, 0, PortCharacteristic.UNKNOWN,
				EdgeH.makeLeftEdge(), EdgeV.makeBottomEdge(), EdgeH.makeRightEdge(), EdgeV.makeTopEdge());
		simProbeNode.addPrimitivePorts(new PrimitivePort [] {simProbePort});
		simProbeNode.setFunction(PrimitiveNode.Function.ART);
		simProbeNode.setCanBeZeroSize();

		// The pure layer nodes
		drcLay.setPureLayerNode(drcNode);
        afgLay.setPureLayerNode(afgNode);
	}

    public static void setBackgroudColor(Color c)
    {
		tech.universalLay.getGraphics().setColor(c);
		tech.glyphLay.getGraphics().setColor(c);
    }

	private static Technology.NodeLayer[] NULLNODELAYER = new Technology.NodeLayer [] {};
	/**
	 * Method to return a list of Polys that describe a given NodeInst.
	 * This method overrides the general one in the Technology object
	 * because of the unusual primitives in the Schematics Technology.
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
	public Poly [] getShapeOfNode(NodeInst ni, EditWindow0 wnd, VarContext context, boolean electrical, boolean reasonable,
		Technology.NodeLayer [] primLayers, Layer layerOverride)
	{
		NodeProto prototype = ni.getProto();

		PrimitiveNode np = (PrimitiveNode)prototype;
		if (np == invisiblePinNode)
		{
			if (ni.isInvisiblePinWithText())
				primLayers = NULLNODELAYER;
		}
		return super.getShapeOfNode(ni, wnd, context, electrical, reasonable, primLayers, layerOverride);
	}
	
	/**
	 * Method to update the connecitivity list for universal and invisible pins so that
	 * they can connect to ALL arcs.  This is called at initialization and again
	 * whenever the number of technologies changes.
	 */
	public void makeUnivList()
	{
		// count the number of arcs in all technologies
		int tot = 0;
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator<ArcProto> ait = tech.getArcs(); ait.hasNext(); )
			{
				ArcProto ap = (ArcProto)ait.next();
				tot++;
			}
		}

		// make an array for each arc
		ArcProto [] upconn = new ArcProto[tot];

		// fill the array
		tot = 0;
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator<ArcProto> ait = tech.getArcs(); ait.hasNext(); )
			{
				ArcProto ap = (ArcProto)ait.next();
				upconn[tot++] = ap;
			}
		}

		// store the array in this technology
		univPinPort.setConnections(upconn);
		invisPinPort.setConnections(upconn);
		simProbePort.setConnections(upconn);
	}

	/**
	 * Method to convert old primitive names to their proper NodeProtos.
	 * @param name the name of the old primitive.
	 * @return the proper PrimitiveNode to use (or null if none can be determined).
	 */
	public PrimitiveNode convertOldNodeName(String name)
	{
		if (name.equals("Cell-Center")) return(cellCenterNode);
		return null;
	}

    /**
	 * Method to detect if this Generic proto is not relevant for some tool calculation and therefore
	 * could be skip. E.g. cellCenter, drcNodes, essential bounds.
	 * Similar for layer generation and automatic fill.
	 * @param ni the NodeInst in question.
	 * @return true if it is a special node (cell center, etc.)
	 */
	public static boolean isSpecialGenericNode(NodeInst ni)
	{
		NodeProto np = ni.getProto();
		return (np == tech.cellCenterNode || np == tech.drcNode ||
		        np == tech.essentialBoundsNode || np == tech.afgNode);
	}
}
