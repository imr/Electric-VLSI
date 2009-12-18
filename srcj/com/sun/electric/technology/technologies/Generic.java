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

import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.AbstractShapeBuilder;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.Foundry;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.TechFactory;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;

import java.awt.Color;
import java.util.Collections;
import java.util.Iterator;

/**
 * This is the Generic technology.
 */
public class Generic extends Technology
{
	/** the Generic Technology object. */	public static Generic tech() { return TechPool.getThreadTechPool().getGeneric(); }

	/** the Universal Layer. */				private final Layer universalLay;
	/** the Glyph Layer. */					public final Layer glyphLay;
	/** the DRC exclusion Layer. */			public final Layer drcLay;
	/** the AFG exclusion Layer. */			public final Layer afgLay;
	/** the Universal Pin node, which connects to every type of arc. */
		public final PrimitiveNode universalPinNode;
	/** the Invisible Pin node, which connects to every type of arc and produces no layout. */
		public final PrimitiveNode invisiblePinNode;
	/** the Unrouted Pin node, for making bends in unrouted arc paths. */
		public final PrimitiveNode unroutedPinNode;
	/** the Cell-Center node, used for defining the origin of the cell's coordinate space. */
		public final PrimitiveNode cellCenterNode;
	/** the Port-definition node, used in technology editing to define node ports. */
		public final PrimitiveNode portNode;
	/** the DRC exclusion node, all design-rule errors covered by this node are ignored. */
		public final PrimitiveNode drcNode;
	/** the AFG exclusion node, tells auto-fill generator to ignore the area. */
		public final PrimitiveNode afgNode;
	/** the Essential-bounds node, used (in pairs) to define the important area of a cell. */
		public final PrimitiveNode essentialBoundsNode;
	/** the Simulation-Probe node, used for highlighting the state of a network. */
		public final PrimitiveNode simProbeNode;
	/** the Universal arc, connects to any node. */
		public final ArcProto universal_arc;
	/** the Invisible arc, connects to any node and produces no layout. */
		public final ArcProto invisible_arc;
	/** the Unrouted arc, connects to any node and specifies desired routing topology. */
		public final ArcProto unrouted_arc;

	private final PrimitivePort univPinPort, invisPinPort, simProbePort;

	// -------------------- private and protected methods ------------------------
    public static Generic newInstance(IdManager idManager) {
        Generic generic = new Generic(idManager);
        generic.setup();
        return generic;
    }

	private Generic(IdManager idManager)
	{
		super(idManager, null, TechFactory.getGenericFactory(), Collections.<TechFactory.Param,Object>emptyMap(), Foundry.Type.NONE, 0);
		setTechShortName("Generic");
		setTechDesc("Useful primitives");
		setNonStandard();

		setFactoryScale(1000, false);			// in nanometers: really 1 micron

		//**************************************** LAYERS ****************************************

		/** Universal layer */
		universalLay = Layer.newInstance(this, "Universal",
			new EGraphics(false, false, null, 0, 0,0,0,1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** Invisible layer */
		Layer invisible_lay = Layer.newInstance(this, "Invisible",
			new EGraphics(false, false, null, 0, 180,180,180,1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** Unrouted layer */
		Layer unrouted_lay = Layer.newInstance(this, "Unrouted",
			new EGraphics(false, false, null, 0, 100,100,100,1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** Glyph layer */
		glyphLay = Layer.newInstance(this, "Glyph",
			new EGraphics(false, false, null, 0, 0,0,0,1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** DRC layer */
		drcLay = Layer.newInstance(this, "DRC",
			new EGraphics(false, false, null, 0, 255,190,6,1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

        /** AFG layer */
		afgLay = Layer.newInstance(this, "AFG",
			new EGraphics(false, false, null, 0, 255,6,190,1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));

		/** Simulation Probe layer */
		Layer simprobe_lay = Layer.newInstance(this, "Sim-Probe",
			new EGraphics(false, false, null, 0, 0,255,0,1.0,true,
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
		universal_arc = newArcProto("Universal", 0, 0.0, ArcProto.Function.UNKNOWN,
			new Technology.ArcLayer(universalLay, 0, Poly.Type.FILLED)
		);
		universal_arc.setFactoryFixedAngle(true);
		universal_arc.setFactoryAngleIncrement(45);

		/** Invisible arc */
		invisible_arc = newArcProto("Invisible", 0, 0.0, ArcProto.Function.NONELEC,
			new Technology.ArcLayer(invisible_lay, 0, Poly.Type.FILLED)
		);
		invisible_arc.setFactoryFixedAngle(true);
		invisible_arc.setFactoryAngleIncrement(45);

		/** Unrouted arc */
		unrouted_arc = newArcProto("Unrouted", 0, 0.0, ArcProto.Function.UNROUTED,
			new Technology.ArcLayer(unrouted_lay, 0, Poly.Type.FILLED)
		);
		unrouted_arc.setFactoryFixedAngle(false);
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
		universalPinNode.addPrimitivePortsFixed(new PrimitivePort [] {univPinPort});
		universalPinNode.setFunction(PrimitiveNode.Function.PIN);
		universalPinNode.setWipeOn1or2();
//		universalPinNode.setHoldsOutline();
		universalPinNode.setCanBeZeroSize();

		/** Invisible pin */
		invisiblePinNode = PrimitiveNode.newInstance0("Invisible-Pin", this, 1.0, 1.0,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(invisible_lay, 0, Poly.Type.CLOSED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
			});
		invisPinPort = PrimitivePort.newInstance(this, invisiblePinNode, new ArcProto[] {invisible_arc,universal_arc}, "center", 0,180, 0, PortCharacteristic.UNKNOWN,
			EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter());
		invisiblePinNode.addPrimitivePortsFixed(new PrimitivePort [] {invisPinPort});
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
		unroutedPinNode.addPrimitivePortsFixed(new PrimitivePort []
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
		cellCenterNode.addPrimitivePortsFixed(new PrimitivePort []
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
		portNode.addPrimitivePortsFixed(new PrimitivePort []
			{
				PrimitivePort.newInstance(this, portNode, new ArcProto[] {invisible_arc,universal_arc}, "center", 0,180, 0, PortCharacteristic.UNKNOWN,
					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
			});
		portNode.setFunction(PrimitiveNode.Function.ART);
		portNode.setCanBeZeroSize();

//		/** DRC Node */
//		drcNode = PrimitiveNode.newInstance("DRC-Node", this, 2.0, 2.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(drcLay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		drcNode.addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, drcNode, new ArcProto[] {invisible_arc,universal_arc}, "center", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
//			});
//		drcNode.setFunction(PrimitiveNode.Function.NODE);
//		drcNode.setHoldsOutline();
//        drcNode.setSpecialType(PrimitiveNode.POLYGONAL);
//
//        /** AFG Node */
//		afgNode = PrimitiveNode.newInstance("AFG-Node", this, 2.0, 2.0, null,
//			new Technology.NodeLayer []
//			{
//				new Technology.NodeLayer(afgLay, 0, Poly.Type.FILLED, Technology.NodeLayer.BOX, Technology.TechPoint.makeFullBox())
//			});
//		afgNode.addPrimitivePorts(new PrimitivePort []
//			{
//				PrimitivePort.newInstance(this, afgNode, new ArcProto[] {invisible_arc,universal_arc}, "center", 0,180, 0, PortCharacteristic.UNKNOWN,
//					EdgeH.makeCenter(), EdgeV.makeCenter(), EdgeH.makeCenter(), EdgeV.makeCenter())
//			});
//		afgNode.setFunction(PrimitiveNode.Function.NODE);
//		afgNode.setHoldsOutline();
//        afgNode.setSpecialType(PrimitiveNode.POLYGONAL);
//
		/** Essential Bounds Node */
		essentialBoundsNode = PrimitiveNode.newInstance("Essential-Bounds", this, 0.0, 0.0, null,
			new Technology.NodeLayer []
			{
				new Technology.NodeLayer(glyphLay, 0, Poly.Type.OPENED, Technology.NodeLayer.POINTS, new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-1), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.makeCenter()),
					new Technology.TechPoint(EdgeH.makeCenter(), EdgeV.fromCenter(-1))})
			});
		essentialBoundsNode.addPrimitivePortsFixed(new PrimitivePort []
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
		simProbeNode.addPrimitivePortsFixed(new PrimitivePort [] {simProbePort});
		simProbeNode.setFunction(PrimitiveNode.Function.ART);
		simProbeNode.setCanBeZeroSize();

		// The pure layer nodes
        drcNode = drcLay.makePureLayerNode("DRC-Node", 2.0, Poly.Type.FILLED, "center", invisible_arc, universal_arc);
        afgNode = afgLay.makePureLayerNode("AFG-Node", 2.0, Poly.Type.FILLED, "center", invisible_arc, universal_arc);
//		drcLay.setPureLayerNode(drcNode);
//        afgLay.setPureLayerNode(afgNode);

        //Foundry
        newFoundry(Foundry.Type.NONE, null);

		oldNodeNames.put("Cell-Center", cellCenterNode);
	}

    public void setBackgroudColor(Color c)
    {
		universalLay.setGraphics(universalLay.getGraphics().withColor(c));
		glyphLay.setGraphics(universalLay.getGraphics().withColor(c));
    }

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
		if (pn == invisiblePinNode)
		{
            boolean hasDisplayVars = false;
            for (Iterator<Variable> it = n.getVariables(); it.hasNext(); ) {
                Variable var = it.next();
                if (var.isDisplay())
                    hasDisplayVars = true;
            }
            if (hasDisplayVars || n.isUsernamed() || b.getMemoization().hasExports(n))
                return;
//			if (ni.isInvisiblePinWithText())
//				primLayers = NULLNODELAYER;
		}
        b.genShapeOfNode(n, pn, primLayers, null);
    }

//	/**
//	 * Method to update the connecitivity list for universal and invisible pins so that
//	 * they can connect to ALL arcs.  This is called at initialization and again
//	 * whenever the number of technologies changes.
//	 */
//	public void makeUnivList()
//	{
//		// count the number of arcs in all technologies
//		int tot = 0;
//		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
//		{
//			Technology tech = it.next();
//			tot += tech.getNumArcs();
//		}
//
//		// make an array for each arc
//		ArcProto [] upconn = new ArcProto[tot];
//
//		// fill the array
//		tot = 0;
//		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
//		{
//			Technology tech = it.next();
//			for(Iterator<ArcProto> ait = tech.getArcs(); ait.hasNext(); )
//			{
//				ArcProto ap = ait.next();
//				upconn[tot++] = ap;
//			}
//		}
//
//		// store the array in this technology
//		univPinPort.setConnections(upconn);
//		invisPinPort.setConnections(upconn);
//		simProbePort.setConnections(upconn);
//	}

    /**
     * Tells if all ArcProtos can connect to the PrimitivePort
     * @param pp PrimitivePort to test
     * @return true if all ArcProtos can connect to the PrimitivePort
     */
    @Override
    public boolean isUniversalConnectivityPort(PrimitivePort pp) {
        return pp == univPinPort || pp == invisPinPort || pp == simProbePort;
    }

//	/**
//	 * Method to convert old primitive names to their proper NodeProtos.
//	 * @param name the name of the old primitive.
//	 * @return the proper PrimitiveNode to use (or null if none can be determined).
//	 */
//	public PrimitiveNode convertOldNodeName(String name)
//	{
//		if (name.equals("Cell-Center")) return(cellCenterNode);
//		return null;
//	}

    /**
	 * Method to detect if this Generic proto is not relevant for some tool calculation and therefore
	 * could be skip. E.g. cellCenter, drcNodes, essential bounds.
	 * Similar for layer generation and automatic fill.
	 * @param ni the NodeInst in question.
	 * @return true if it is a special node (cell center, etc.)
	 */
	public static boolean isSpecialGenericNode(NodeInst ni)
	{
        if (ni.isCellInstance()) return false;
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
        if (!(np.getTechnology() instanceof Generic)) return false;
        Generic tech = (Generic)np.getTechnology();
		return (np == tech.cellCenterNode || np == tech.drcNode ||
		        np == tech.essentialBoundsNode || np == tech.afgNode);
	}
}
