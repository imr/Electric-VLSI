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

import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;

/** IC Layer information database gives information about various IC
 *  layers */
public class Tech {
	//---------------------------- private data ----------------------------------
	// The info map holds special information needed to construct
	// Electric Nodes and Arcs.  This information isn't otherwise
	// available from Jose.
	//private static HashMap infos = new HashMap();
	private static String[] layerNms = {
		"Polysilicon-1",
		"Metal-1",
		"Metal-2",
		"Metal-3",
		"Metal-4",
		"Metal-5",
		"Metal-6" 
	};
	private static int nbLay = layerNms.length;
	private static PrimitiveArc[] layers = new PrimitiveArc[nbLay];
	private static PrimitiveNode[] vias = new PrimitiveNode[nbLay - 1];
	private static HashMap viaMap = new HashMap();
	private static Technology tech;

	//----------------------------- public data ----------------------------------
	/** layers 
	 *
	 * Poly and metal are considered to be routing layers.  In contrast
	 * we assume we never want to route in diffusion or well.  This
	 * allows us to assign a unique "height" to each of the routing
	 * layers.  Layers at the same height can connect directly.  Layers
	 * at adjacent heights can connect using vias.
	 *
	 * For now, well and diffusion don't have heights. */
	public static final PrimitiveArc pdiff, ndiff, p1, m1, m2, m3, m4, m5, m6;
	/** layer pins */
	public static final PrimitiveNode ndpin,
		pdpin,
		p1pin,
		m1pin,
		m2pin,
		m3pin,
		m4pin,
		m5pin,
		m6pin;
	/** vias */
	public static final PrimitiveNode nwm1,
		pwm1,
		ndm1,
		pdm1,
		p1m1,
		m1m2,
		m2m3,
		m3m4,
		m4m5,
		m5m6;
	/** Transistors */
	public static final PrimitiveNode nmos, pmos;
	/** Well */
	public static final PrimitiveNode nwell, pwell;
	/** Layer nodes are sometimes used to patch notches */
	public static final PrimitiveNode m1Node,
		m2Node,
		m3Node,
		m4Node,
		m5Node,
		m6Node,
		p1Node,
		pdNode,
		ndNode,
		pselNode,
		nselNode;
	/** Essential-Bounds */
	public static final PrimitiveNode essentialBounds;
	/** Facet-Center */
	public static final PrimitiveNode facetCenter;
	/** Generic: Universal Arcs are used to fool Electric's NCC into
	 *  paralleling MOS stacks.*/
	public static final ArcProto universalArc;

	//----------------------------- private methods  -----------------------------
	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}
	static {
		tech = Technology.findTechnology("mocmos");
		//setNanometersPerLambda(100);

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
		pdiff = tech.findArcProto("P-Active");
		ndiff = tech.findArcProto("N-Active");

		pdpin = pdiff.findOverridablePinProto();
		ndpin = ndiff.findOverridablePinProto();
		p1pin = p1.findOverridablePinProto();
		m1pin = m1.findOverridablePinProto();
		m2pin = m2.findOverridablePinProto();
		m3pin = m3.findOverridablePinProto();
		m4pin = m4.findOverridablePinProto();
		m5pin = m5.findOverridablePinProto();
		m6pin = m6.findOverridablePinProto();

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
		ndm1 = tech.findNodeProto("Metal-1-N-Active-Con");
		pdm1 = tech.findNodeProto("Metal-1-P-Active-Con");
		nwm1 = tech.findNodeProto("Metal-1-N-Well-Con");
		pwm1 = tech.findNodeProto("Metal-1-P-Well-Con");

		// initialize transistors
		nmos = tech.findNodeProto("N-Transistor");
		pmos = tech.findNodeProto("P-Transistor");

		// intialize well
		nwell = tech.findNodeProto("N-Well-Node");
		pwell = tech.findNodeProto("P-Well-Node");

		// Layer Nodes
		m1Node = tech.findNodeProto("Metal-1-Node");
		m2Node = tech.findNodeProto("Metal-2-Node");
		m3Node = tech.findNodeProto("Metal-3-Node");
		m4Node = tech.findNodeProto("Metal-4-Node");
		m5Node = tech.findNodeProto("Metal-5-Node");
		m6Node = tech.findNodeProto("Metal-6-Node");
		p1Node = tech.findNodeProto("Polysilicon-1-Node");
		pdNode = tech.findNodeProto("P-Active-Node");
		ndNode = tech.findNodeProto("N-Active-Node");
		nselNode = tech.findNodeProto("N-Select-Node");
		pselNode = tech.findNodeProto("P-Select-Node");

		// essential bounds
		Technology generic = Technology.findTechnology("generic");
		essentialBounds = generic.findNodeProto("Essential-Bounds");
		facetCenter = generic.findNodeProto("Facet-Center");

		universalArc = generic.findArcProto("Universal");

		// initialize map from layers to vias
		viaMap.put(new Integer(m1.hashCode() * m2.hashCode()), m1m2);
		viaMap.put(new Integer(m2.hashCode() * m3.hashCode()), m2m3);
		viaMap.put(new Integer(m3.hashCode() * m4.hashCode()), m3m4);
		viaMap.put(new Integer(m4.hashCode() * m5.hashCode()), m4m5);
		viaMap.put(new Integer(m5.hashCode() * m6.hashCode()), m5m6);

		viaMap.put(new Integer(ndiff.hashCode() * m1.hashCode()), ndm1);
		viaMap.put(new Integer(pdiff.hashCode() * m1.hashCode()), pdm1);
		viaMap.put(new Integer(p1.hashCode() * m1.hashCode()), p1m1);
	}

	//----------------------------- public methods  ------------------------------
	/*
	// this is useful for debugging only
	public static void dumpPoly(Poly p) {
	  // dump the poly
	  System.out.println("Begin Polygon:");
	  AffineTransform at = new AffineTransform(); // identity transform
	  PathIterator pi = p.getPathIterator(at);
	  double[] coords = {0,0,0,0,0,0};
	  for (; !pi.isDone(); pi.next()) {
	    int t = pi.currentSegment(coords);
	    switch (t) {
	    case PathIterator.SEG_CLOSE:
	      System.out.println(" close");
	      break;
	    case PathIterator.SEG_MOVETO:
	      System.out.println(" moveTo ("+base2lambda((int)coords[0])+", "
	                         +base2lambda((int)coords[1])+")");
	      break;
	    case PathIterator.SEG_LINETO:
	      System.out.println(" lineTo ("+base2lambda((int)coords[0])
	                         +", "+base2lambda((int)coords[1])+")");
	      break;
	    default:
	      System.out.println(" other");
	    }
	  }
	  System.out.println("End Polygon");
	}
	*/
	
	public static void setNanometersPerLambda(double nanoMeters) {
		tech.setScale(nanoMeters);
	}

	public static PrimitiveNode getViaFor(PrimitiveArc a1, PrimitiveArc a2) {
		int code = a1.hashCode() * a2.hashCode();
		return (PrimitiveNode) viaMap.get(new Integer(code));		
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
		for (Iterator it=tech.getNodes(); it.hasNext();) {
			PrimitiveNode pn = (PrimitiveNode) it.next();
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
}
