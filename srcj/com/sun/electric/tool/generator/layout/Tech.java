/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Tech.java
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
package com.sun.electric.tool.generator.layout;

import java.util.HashMap;
import java.util.Iterator;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.*;

/** The Tech class holds technology dependent information for the layout
 * generators. Most of the information is available from public static methods.
 * <p> The Tech class queries the appropriate Technology object to get references
 * to prototypes commonly used by the layout generators such as the metal 1 arc
 * proto or the metal 1 pin proto. The Tech class also holds technology dependant
 * dimensions such as the width of a diffusion contact.
 * <p>  The Tech class contains only global data. The Tech class is global 
 * because otherwise almost every procedure in the layout generators would have
 * to pass a pointer to a Tech object. */
public class Tech {
	// ----------------------- public class --------------------------------------
	/** Hide the differences between technologies. A MosInst's gate is always 
	 * vertical. */
	public static class MosInst {
		private boolean is90nm;
		private NodeInst mos;
		boolean ntype;
		protected MosInst(boolean ntype, double x, double y, double w, double l, 
				          Cell parent) {
			this.is90nm = Tech.is90nm();
			this.ntype = ntype;
			NodeProto np = ntype ? nmos : pmos;
			double angle = is90nm ? 0 : 90;
			double xSize = is90nm ? l : w;
			double ySize = is90nm ? w : l;
			mos = LayoutLib.newNodeInst(np, x, y, xSize, ySize, angle, parent);
		}
		private String mosTypeString() {
			return ntype ? "n-trans" : "p-trans";
		}
		private PortInst getPort(String portNm) {
			PortInst pi = mos.findPortInst(portNm);
			error(pi==null, "MosInst can't find port!");
			return pi;
		}
		public PortInst leftDiff() {
            String type = is90nm ? "" : (mosTypeString()+"-");
			String portNm = type + "diff" +
			                (is90nm ? "-left" : "-top");
			return getPort(portNm);
		}
		public PortInst rightDiff() {
            String type = is90nm ? "" : (mosTypeString()+"-");
			String portNm = type + "diff" +
			                (is90nm ? "-right" : "-bottom");
			return getPort(portNm);
		}
		public PortInst topPoly() {
            String type = is90nm ? "" : (mosTypeString()+"-");
			String portNm = type + "poly" +
			                (is90nm ? "-top" : "-right");
			return getPort(portNm);
		}
		public PortInst botPoly() {
            String type = is90nm ? "" : (mosTypeString()+"-");
			String portNm = type + "poly" + 
			                (is90nm ? "-bottom" : "-left");
			return getPort(portNm);
		}
	}

	//---------------------------- private data ----------------------------------
	private static Type techType;
	private static final String[] LAYER_NAMES_MOCMOS = {"Polysilicon-1", "Metal-1", 
	    "Metal-2", "Metal-3", "Metal-4", "Metal-5", "Metal-6"};
	private static final String[] LAYER_NAMES_90NM = {"Polysilicon", "Metal-1", 
		"Metal-2", "Metal-3", "Metal-4", "Metal-5", "Metal-6", "Metal-7", "Metal-8", "Metal-9"};
	private static String[] layerNms;
	private static int nbLay;
	private static ArcProto[] layers;
	private static PrimitiveNode[] vias;
	private static HashMap<Integer,PrimitiveNode> viaMap = new HashMap<Integer,PrimitiveNode>();
	private static Technology tech;

    // Gilda: gate length depending on foundry
    private static double gateLength;
    // Wire offset from center of the poly contact to form a L-shape arc to gate
    private static double offsetLShapePolyContact, offsetTShapePolyContact;
    // denote Select spacing rule
	private static double selectSpace;
    // surround distance of select from active in transistor
	private static double selectSurroundDiffInTrans, selectSurroundDiffAlongGateInTrans;
    // select surround over poly. PP/NP.R.1 in 90nm
    private static double selectSurround;

	// RKao my first attempt to embed technology specific dimensions
    private static double 
		wellWidth,
		wellSurroundDiff,
		gateExtendPastMOS,
		p1Width,
		p1ToP1Space,
		p1M1Width,
		gateToGateSpace,
		gateToDiffContSpace,
		diffContWidth,
		selectSurroundDiffInActiveContact;  // select surround in active contacts

	/** layers
	 *
	 * Poly and metal are considered to be routing layers.  In contrast
	 * we assume we never want to route in diffusion or well.  This
	 * allows us to assign a unique "height" to each of the routing
	 * layers.  Layers at the same height can connect directly.  Layers
	 * at adjacent heights can connect using vias.
	 *
	 * For now, well and diffusion don't have heights. */
	private static ArcProto pdiff, ndiff, p1, m1, m2, m3, m4, m5, m6, m7, m8, m9,
		ndiff18, pdiff18, ndiff25, pdiff25, ndiff33, pdiff33;

	/** layer pins */
	private static PrimitiveNode ndpin, pdpin, p1pin, m1pin, m2pin, m3pin, 
		m4pin, m5pin, m6pin, m7pin, m8pin, m9pin;

	/** vias */
	private static PrimitiveNode nwm1, pwm1, nwm1Y, pwm1Y, ndm1, pdm1, p1m1,
		m1m2, m2m3, m3m4, m4m5, m5m6, m6m7, m7m8, m8m9;
	
	/** Transistors */
	public static PrimitiveNode nmos, pmos, nmos18, pmos18, nmos25, pmos25, 
		nmos33, pmos33, nvth, pvth, nvtl, pvtl, nnat, pnat;
	
    /** special threshold transistor contacts */
    private static PrimitiveNode nmos18contact, pmos18contact, nmos25contact,
        pmos25contact, nmos33contact, pmos33contact;

	/** Well */
	private static PrimitiveNode nwell, pwell;

	/** Layer nodes are sometimes used to patch notches */
	private static PrimitiveNode m1Node, m2Node, m3Node, m4Node, m5Node, m6Node,
        m7Node, m8Node, m9Node, p1Node, pdNode, ndNode, pselNode, nselNode,
        pwellNode, nwellNode;

	/** Transistor layer nodes */
    private static PrimitiveNode od18, od25, od33, vth, vtl;
    
	/** Essential-Bounds */
	private static PrimitiveNode essentialBounds;
	
	/** Facet-Center */
	private static PrimitiveNode facetCenter;

	//----------------------------- public data ----------------------------------
	public static final Variable.Key ATTR_X = Variable.newKey("ATTR_X");
	public static final Variable.Key ATTR_S = Variable.newKey("ATTR_S");
	public static final Variable.Key ATTR_SN = Variable.newKey("ATTR_SN");
	public static final Variable.Key ATTR_SP = Variable.newKey("ATTR_SP");
	
    /** These are the Electric technology/foundry combinations understood by the 
     * gate layout generators */
    public enum Type {
    	INVALID,	// not initialized 
    	MOCMOS,		// MOCMOS technology with MOSIS foundry
    	TSMC180,	// MOCMOS technology with TSMC180 foundry
    	TSMC90, 	// TSMC90 technology with TSMC90 foundry
    	ST90}; 		// TSMC90 technology with ST90 foundry 

	//----------------------------- private methods  -----------------------------
	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}
	//----------------------------- public methods  ------------------------------

    /** layers */
	public static ArcProto pdiff() {return pdiff;}
	public static ArcProto ndiff() {return ndiff;}
	public static ArcProto p1() {return p1;}
	public static ArcProto m1() {return m1;}
	public static ArcProto m2() {return m2;}
	public static ArcProto m3() {return m3;}
	public static ArcProto m4() {return m4;}
	public static ArcProto m5() {return m5;}
	public static ArcProto m6() {return m6;}
	public static ArcProto m7() {return m7;}
	public static ArcProto m8() {return m8;}
	public static ArcProto m9() {return m9;}
	public static ArcProto ndiff18() {return ndiff18;}
	public static ArcProto pdiff18() {return pdiff18;}
	public static ArcProto ndiff25() {return ndiff25;}
	public static ArcProto pdiff25() {return pdiff25;}
	public static ArcProto ndiff33() {return ndiff33;}
	public static ArcProto pdiff33() {return pdiff33;}

	/** pins */
	public static PrimitiveNode ndpin() {return ndpin;}
	public static PrimitiveNode pdpin() {return pdpin;}
	public static PrimitiveNode p1pin() {return p1pin;}
	public static PrimitiveNode m1pin() {return m1pin;}
	public static PrimitiveNode m2pin() {return m2pin;}
	public static PrimitiveNode m3pin() {return m3pin;}
	public static PrimitiveNode m4pin() {return m4pin;}
	public static PrimitiveNode m5pin() {return m5pin;}
	public static PrimitiveNode m6pin() {return m6pin;}
	public static PrimitiveNode m7pin() {return m7pin;}
	public static PrimitiveNode m8pin() {return m8pin;}
	public static PrimitiveNode m9pin() {return m9pin;}

	/** vias */
	public static PrimitiveNode nwm1() {return nwm1;}
	public static PrimitiveNode pwm1() {return pwm1;}
	public static PrimitiveNode nwm1Y() {return nwm1Y;}
	public static PrimitiveNode pwm1Y() {return pwm1Y;}
	public static PrimitiveNode ndm1() {return ndm1;}
	public static PrimitiveNode pdm1() {return pdm1;}
	public static PrimitiveNode p1m1() {return p1m1;}
	public static PrimitiveNode m1m2() {return m1m2;}
	public static PrimitiveNode m2m3() {return m2m3;}
	public static PrimitiveNode m3m4() {return m3m4;}
	public static PrimitiveNode m4m5() {return m4m5;}
	public static PrimitiveNode m5m6() {return m5m6;}
	public static PrimitiveNode m6m7() {return m6m7;}
	public static PrimitiveNode m7m8() {return m7m8;}
	public static PrimitiveNode m8m9() {return m8m9;}

	/** Transistors */
	public static PrimitiveNode nmos() {return nmos;}
	public static PrimitiveNode pmos() {return pmos;}
	public static PrimitiveNode nmos18() {return nmos18;}
	public static PrimitiveNode pmos18() {return pmos18;}
	public static PrimitiveNode nmos25() {return nmos25;}
	public static PrimitiveNode pmos25() {return pmos25;}
	public static PrimitiveNode nmos33() {return nmos33;}
	public static PrimitiveNode pmos33() {return pmos33;}
	public static PrimitiveNode nvth() {return nvth;}
	public static PrimitiveNode pvth() {return pvth;}
	public static PrimitiveNode nvtl() {return nvtl;}
	public static PrimitiveNode pvtl() {return pvtl;}
	public static PrimitiveNode nnat() {return nnat;}
	public static PrimitiveNode pnat() {return pnat;}
	
    /** special threshold transistor contacts */
    public static PrimitiveNode nmos18contact() {return nmos18contact;}
    public static PrimitiveNode pmos18contact() {return pmos18contact;}
    public static PrimitiveNode nmos25contact() {return nmos25contact;}
    public static PrimitiveNode pmos25contact() {return pmos25contact;}
    public static PrimitiveNode nmos33contact() {return nmos33contact;}
    public static PrimitiveNode pmos33contact() {return pmos33contact;}

	/** Well */
	public static PrimitiveNode nwell() {return nwell;} 
	public static PrimitiveNode pwell() {return pwell;} 

	/** Layer nodes are sometimes used to patch notches */
	public static PrimitiveNode m1Node() {return m1Node;} 
	public static PrimitiveNode m2Node() {return m2Node;} 
	public static PrimitiveNode m3Node() {return m3Node;} 
	public static PrimitiveNode m4Node() {return m4Node;} 
	public static PrimitiveNode m5Node() {return m5Node;} 
	public static PrimitiveNode m6Node() {return m6Node;} 
	public static PrimitiveNode m7Node() {return m7Node;} 
	public static PrimitiveNode m8Node() {return m8Node;} 
	public static PrimitiveNode m9Node() {return m9Node;} 
	public static PrimitiveNode p1Node() {return p1Node;} 
	public static PrimitiveNode pdNode() {return pdNode;} 
	public static PrimitiveNode ndNode() {return ndNode;} 
	public static PrimitiveNode pselNode() {return pselNode;} 
	public static PrimitiveNode nselNode() {return nselNode;} 
	public static PrimitiveNode pwellNode() {return pwellNode;} 
	public static PrimitiveNode nwellNode() {return nwellNode;} 

	/** Transistor layer nodes */
	public static PrimitiveNode od18() {return od18;} 
	public static PrimitiveNode od25() {return od25;} 
	public static PrimitiveNode od33() {return od33;} 
	public static PrimitiveNode vth() {return vth;} 
	public static PrimitiveNode vtl() {return vtl;} 
	
	/** Essential-Bounds */
	public static PrimitiveNode essentialBounds() {return essentialBounds;} 

	/** Facet-Center */
	public static PrimitiveNode facetCenter() {return facetCenter;} 

//	/** Generic: Universal Arcs are used to fool Electric's NCC into
//	 *  paralleling MOS stacks.*/
//	public static ArcProto universalArc;
	
	/** round to avoid MOCMOS CIF resolution errors */
	public static double roundToGrid(double x) {
		return (is90nm()) ? x : (Math.rint(x * 2) / 2);
	}
	public static MosInst newNmosInst(double x, double y, 
			                          double w, double l, Cell parent) {
		return new MosInst(true, x, y, w, l, parent);
	}
	public static MosInst newPmosInst(double x, double y, 
                                      double w, double l, Cell parent) {
		return new MosInst(false, x, y, w, l, parent);
}

    public static Type getTechnology() { return techType; }

	public static void setTechnology(Type techTyp) {
		if (techTyp == techType) return;
		
        techType = techTyp;

        if (techType==Type.MOCMOS || techType==Type.TSMC180) {
			tech = Technology.findTechnology("MOCMOS");
		} else if (is90nm()){
			tech = Technology.findTechnology(techType.name());
		} else {
			error(true, "Unrecognized Tech.Type: "+techType);
		}
		layerNms = is90nm() ? LAYER_NAMES_90NM : LAYER_NAMES_MOCMOS;
		nbLay = layerNms.length;
		layers = new ArcProto[nbLay];
		vias = new PrimitiveNode[nbLay - 1];

		// initialize layers
		for (int i=0; i<nbLay; i++) {
			layers[i] = tech.findArcProto(layerNms[i]);
			error(layers[i]==null, "No such layer: " + layerNms[i]);
		}
		p1 = layers[0];
		m1 = layers[1];
		m2 = layers[2];
		m3 = layers[3];
		m4 = layers[4];
		m5 = layers[5];
		m6 = layers[6];
        if ((techType == Type.TSMC90)) {
            m7 = layers[7];
            m8 = layers[8];
            m9 = layers[9];
        }
		pdiff = tech.findArcProto("P-Active");
		ndiff = tech.findArcProto("N-Active");
        ndiff18 = tech.findArcProto("thick-OD18-N-Active");
        pdiff18 = tech.findArcProto("thick-OD18-P-Active");
        ndiff25 = tech.findArcProto("thick-OD25-N-Active");
        pdiff25 = tech.findArcProto("thick-OD25-P-Active");
        ndiff33 = tech.findArcProto("thick-OD33-N-Active");
        pdiff33 = tech.findArcProto("thick-OD33-P-Active");

		pdpin = pdiff.findOverridablePinProto();
		ndpin = ndiff.findOverridablePinProto();
		p1pin = p1.findOverridablePinProto();
		m1pin = m1.findOverridablePinProto();
		m2pin = m2.findOverridablePinProto();
		m3pin = m3.findOverridablePinProto();
		m4pin = m4.findOverridablePinProto();
		m5pin = m5.findOverridablePinProto();
		m6pin = m6.findOverridablePinProto();
        if (is90nm()) {
            m7pin = m7.findOverridablePinProto();
            m8pin = m8.findOverridablePinProto();
            m9pin = m9.findOverridablePinProto();
        }

		// initialize vias
		for (int i = 0; i < nbLay - 1; i++) {
			vias[i] = findNode(PrimitiveNode.Function.CONTACT,
					           new ArcProto[] {layers[i], layers[i+1]},
					           tech);
			error(vias[i] == null, "No via for layer: " + layerNms[i]);
		}
		p1m1 = vias[0];
		m1m2 = vias[1];
		m2m3 = vias[2];
		m3m4 = vias[3];
		m4m5 = vias[4];
		m5m6 = vias[5];
        if (is90nm()) {
            m6m7 = vias[6];
            m7m8 = vias[7];
            m8m9 = vias[8];
        }
		ndm1 = tech.findNodeProto("Metal-1-N-Active-Con");
		pdm1 = tech.findNodeProto("Metal-1-P-Active-Con");
		nwm1 = tech.findNodeProto("Metal-1-N-Well-Con");
		pwm1 = tech.findNodeProto("Metal-1-P-Well-Con");
        nwm1Y = tech.findNodeProto("Y-Metal-1-N-Well-Con");
        pwm1Y = tech.findNodeProto("Y-Metal-1-P-Well-Con");

		// initialize transistors
		nmos = tech.findNodeProto("N-Transistor");
		pmos = tech.findNodeProto("P-Transistor");
        nmos18 = tech.findNodeProto("OD18-N-Transistor");
        pmos18 = tech.findNodeProto("OD18-P-Transistor");
        nmos25 = tech.findNodeProto("OD25-N-Transistor");
        pmos25 = tech.findNodeProto("OD25-P-Transistor");
        nmos33 = tech.findNodeProto("OD33-N-Transistor");
        pmos33 = tech.findNodeProto("OD33-P-Transistor");
        nvth = null;
        pvth = null;
        nvtl = null;
        pvtl = null;
        nnat = null;
        pnat = null;

        // initialize special threshold transistor contacts
        nmos18contact = tech.findNodeProto("thick-OD18-Metal-1-N-Active-Con");
        pmos18contact = tech.findNodeProto("thick-OD18-Metal-1-P-Active-Con");
        nmos25contact = tech.findNodeProto("thick-OD25-Metal-1-N-Active-Con");
        pmos25contact = tech.findNodeProto("thick-OD25-Metal-1-P-Active-Con");
        nmos33contact = tech.findNodeProto("thick-OD33-Metal-1-N-Active-Con");
        pmos33contact = tech.findNodeProto("thick-OD33-Metal-1-P-Active-Con");

        // transistor layers
        od18 = tech.findNodeProto("OD18-Node");
        od25 = tech.findNodeProto("OD25-Node");
        od33 = tech.findNodeProto("OD33-Node");
        vth = tech.findNodeProto("VTH-Node");
        vtl = tech.findNodeProto("VTL-Node");

		// intialize well
		nwell = tech.findNodeProto("N-Well-Node");
		pwell = tech.findNodeProto("P-Well-Node");
        nwellNode = tech.findNodeProto("Metal-1-N-Well-Con");
        pwellNode = tech.findNodeProto("Metal-1-P-Well-Con");
        if (is90nm()) {
            nwellNode = tech.findNodeProto("Y-Metal-1-N-Well-Con");
            pwellNode = tech.findNodeProto("Y-Metal-1-P-Well-Con");
        }

		// Layer Nodes
		m1Node = tech.findNodeProto("Metal-1-Node");
		m2Node = tech.findNodeProto("Metal-2-Node");
		m3Node = tech.findNodeProto("Metal-3-Node");
		m4Node = tech.findNodeProto("Metal-4-Node");
		m5Node = tech.findNodeProto("Metal-5-Node");
		m6Node = tech.findNodeProto("Metal-6-Node");
        m7Node = tech.findNodeProto("Metal-7-Node");
        m8Node = tech.findNodeProto("Metal-8-Node");
        m9Node = tech.findNodeProto("Metal-9-Node");
		p1Node = tech.findNodeProto("Polysilicon-1-Node");
		pdNode = tech.findNodeProto("P-Active-Node");
		ndNode = tech.findNodeProto("N-Active-Node");
		nselNode = tech.findNodeProto("N-Select-Node");
		pselNode = tech.findNodeProto("P-Select-Node");

		// essential bounds
		Technology generic = Technology.findTechnology("generic");
		essentialBounds = generic.findNodeProto("Essential-Bounds");
		facetCenter = generic.findNodeProto("Facet-Center");

		//universalArc = generic.findArcProto("Universal");

		// initialize map from layers to vias
		viaMap.put(new Integer(m1.hashCode() * m2.hashCode()), m1m2);
		viaMap.put(new Integer(m2.hashCode() * m3.hashCode()), m2m3);
		viaMap.put(new Integer(m3.hashCode() * m4.hashCode()), m3m4);
		viaMap.put(new Integer(m4.hashCode() * m5.hashCode()), m4m5);
		viaMap.put(new Integer(m5.hashCode() * m6.hashCode()), m5m6);
        if (is90nm()) {
            viaMap.put(new Integer(m6.hashCode() * m7.hashCode()), m6m7);
            viaMap.put(new Integer(m7.hashCode() * m8.hashCode()), m7m8);
            viaMap.put(new Integer(m8.hashCode() * m9.hashCode()), m8m9);
        }

		viaMap.put(new Integer(ndiff.hashCode() * m1.hashCode()), ndm1);
		viaMap.put(new Integer(pdiff.hashCode() * m1.hashCode()), pdm1);
		viaMap.put(new Integer(p1.hashCode() * m1.hashCode()), p1m1);

        PrimitiveNode wellCon = tech.findNodeProto("Metal-1-N-Well-Con");
        wellWidth = wellCon.getMinSizeRule().getWidth();

		// initialize design rules (RKao first cut)
		if (is90nm()) {
            if (wellWidth != 14)
                new Error("wrong value in Tech");
//		    wellWidth = 14;
		    wellSurroundDiff = Double.NaN;
		    gateExtendPastMOS = 3.25;
		    p1Width = 2;
		    p1ToP1Space = 3.6;
		    p1M1Width = Double.NaN;
		    gateToGateSpace = 4;
		    gateToDiffContSpace = 5.6 - 5.2/2 - 2/2;
		    diffContWidth = 5.2;
            gateLength = 2;
            offsetLShapePolyContact = 2.5 /* half poly contact height */ - 1 /*half poly arc width*/;
            offsetTShapePolyContact = 2.5 /* half poly contact height */ + 1 /*half poly arc width*/;
            selectSpace = 4.8; // TSMC rule, see TSMC90.java
            selectSurroundDiffInTrans = 1.3;
            selectSurround = 4.4;
            selectSurroundDiffInActiveContact = Double.NaN;
            selectSurroundDiffAlongGateInTrans = 3.6;
		} else if (techType==Type.TSMC180) {
            if (wellWidth != 17)
                new Error("wrong value in Tech");
//            wellWidth = 17;
		    wellSurroundDiff = 4.3;
		    gateExtendPastMOS = 2.5;
		    p1Width = 1.8;
		    p1ToP1Space = 4.5 - .9 - .9;
		    p1M1Width = 5;
		    gateToGateSpace = 3;
		    gateToDiffContSpace = 4.5 - 2.5 - .9;
		    diffContWidth = 5;
            gateLength = 1.8;
            offsetLShapePolyContact = 2.5 /* half poly contact height */ - 0.9 /*half poly arc width*/;
            offsetTShapePolyContact = 2.5 /* half poly contact height */ + 0.9 /*half poly arc width*/;
            selectSpace = 4.4;
            selectSurroundDiffInTrans = 1.8;
            selectSurround = 0;
            selectSurroundDiffInActiveContact = 1;
            selectSurroundDiffAlongGateInTrans = 3.6;
		} else {
			// default to MoCMOS
            if (wellWidth != 17)
                new Error("wrong value in Tech");
//		    wellWidth = 17;
		    wellSurroundDiff = 3;
		    gateExtendPastMOS = 2;
		    p1Width = 2;
		    p1ToP1Space = 3;
		    p1M1Width = 5;
		    gateToGateSpace = 3;
		    gateToDiffContSpace = .5;
		    diffContWidth = 5;
            gateLength = 2;
            offsetLShapePolyContact = 2.5 /* half poly contact height */ - 1 /*half poly arc width*/;
            offsetTShapePolyContact = 2.5 /* half poly contact height */ + 1 /*half poly arc width*/;
            selectSpace = 2;
            selectSurroundDiffInTrans = 2;
            selectSurround = -Double.NaN; // no valid value
            selectSurroundDiffInActiveContact = 2;
            selectSurroundDiffAlongGateInTrans = 2;
		}
	}

    public static boolean is90nm() { return techType==Type.TSMC90 || techType==Type.ST90; }

	public static PrimitiveNode getViaFor(ArcProto a1, ArcProto a2) {
		int code = a1.hashCode() * a2.hashCode();
		return viaMap.get(new Integer(code));		
	}

	/** layer may only be poly or metal */
	public static ArcProto closestLayer(PortProto port, ArcProto layer) {
		int h = layerHeight(layer);
		for (int dist = 0; dist < nbLay; dist++) {
			int lookUp = h + dist;
			int lookDn = h - dist;
			if (lookUp < nbLay) {
				ArcProto lay = layerAtHeight(lookUp);
				if (port.connectsTo(lay))
					return lay;
			}
			if (lookDn >= 0) {
				ArcProto lay = layerAtHeight(lookDn);
				if (port.connectsTo(lay))
					return lay;
			}
		}
		error(true, "port can't connect to any layer?!!");
		return null;
	}

	public static ArcProto layerAtHeight(int layHeight) {
		return layers[layHeight];
	}
	public static int layerHeight(ArcProto p) {
		for (int i = 0; i < nbLay; i++) {
			if (layers[i] == p)
				return i;
		}
		error(true, "Can't find layer: " + p);
		return -1;
	}
	public static PrimitiveNode viaAbove(ArcProto lay) {
		return viaAbove(layerHeight(lay));
	}
	public static PrimitiveNode viaAbove(int layHeight) {
		return vias[layHeight];
	}
	public static PrimitiveNode viaBelow(ArcProto lay) {
		return viaBelow(layerHeight(lay));
	}
	public static PrimitiveNode viaBelow(int layHeight) {
		return vias[layHeight - 1];
	}
	/**
	 * get the PrimitiveNode of a particular type that connects to the
	 * complete set of wires given
	 */
	public static PrimitiveNode findNode(PrimitiveNode.Function type, 
	                                     ArcProto[] arcs, Technology tech) {
		for (Iterator<PrimitiveNode> it=tech.getNodes(); it.hasNext();) {
			PrimitiveNode pn = it.next();
			boolean found = true;
			if (pn.getFunction() == type) {
				for (int j=0; j<arcs.length; j++) {
					if (pn.connectsTo(arcs[j]) == null) {
						found = false;
						break;
					}
				}
				if (found) return pn;
			}
		}
		return null;
	}

	/** Here is my first attempt to embed design rules into Tech. This is useful for
	 * distinguising MoCMOS from tsmc180. RKao */

	/** @return min width of Well */
	public static double getWellWidth() {return wellWidth;}
	/** @return amount that well surrounds diffusion */
	public static double getWellSurroundDiff() {return wellSurroundDiff;}
	/** @return MOS edge to gate edge */
    public static double getGateExtendPastMOS() {return gateExtendPastMOS;}
    /** @return min width of polysilicon 1 */
    public static double getP1Width() {return p1Width;}
    /** @return min spacing between polysilicon 1 */
    public static double getP1ToP1Space() {return p1ToP1Space;}
    /** @return min spacing between gates of series transistors */ 
    public static double getGateToGateSpace() {return gateToGateSpace;}
    /** @return min spacing between MOS gate and diffusion edge of diff contact */
    public static double getGateToDiffContSpace() {return gateToDiffContSpace;}
    /** @return min width of diffusion surrounding diff contact */
    public static double getDiffContWidth() {return diffContWidth;}
    /** @return min width of poly contact */
    public static double getP1M1Width() {return p1M1Width;}
    /** @return gate length that depends on foundry */
    public static double getGateLength() {return gateLength;}
    /** @return amount that select surrounds diffusion in well? */
    public static double selectSurroundDiffInActiveContact() {return selectSurroundDiffInActiveContact;}
    /** @return amount that Select surrounds MOS, along gate width dimension */
    public static double selectSurroundDiffAlongGateInTrans() {return selectSurroundDiffAlongGateInTrans;}
    /** @return y offset of poly arc connecting poly contact and gate in a L-Shape case */
    public static double getPolyLShapeOffset() {return offsetLShapePolyContact;}
    /** @return y offset of poly arc connecting poly contact and gate in a T-Shape case */
    public static double getPolyTShapeOffset() {return offsetTShapePolyContact;}
    /** @return select spacing rule */
    public static double getSelectSpacingRule() {return selectSpace;}
    /** @return select surround active in transistors but not along the gate */
    public static double getSelectSurroundDiffInTrans() {return selectSurroundDiffInTrans;}
    /** @return selecct surround over poly */
    public static double getSelectSurroundOverPoly() {return selectSurround;}
}

