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

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;

//import com.sun.dbmirror.*;

import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.prototype.*;
import com.sun.electric.database.network.*;
import com.sun.electric.technology.*;

/** IC Layer information database gives information about various IC
 *  layers */
public class Tech {
	//  private static class Info {
	//    final boolean isVia;
	//    final double invisWidth, invisHeight;
	//    Info(boolean isvia, double invisWid, double invisHei) {
	//      isVia=isvia; invisWidth=invisWid;  invisHeight=invisHei;
	//    }
	//  }

	//---------------------------- private data ----------------------------------
	// The info map holds special information needed to construct
	// Electric Nodes and Arcs.  This information isn't otherwise
	// available from Jose.
	//private static HashMap infos = new HashMap();
	private static String[] layerNms =
		{
			"Polysilicon-1",
			"Metal-1",
			"Metal-2",
			"Metal-3",
			"Metal-4",
			"Metal-5",
			"Metal-6" };
	private static int nbLay = layerNms.length;
	private static PrimitiveArc[] layers = new PrimitiveArc[nbLay];
	private static PrimitiveNode[] vias = new PrimitiveNode[nbLay - 1];

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
	/*
	// Temporary: protoSizeOffset doesn't yet exist in Jose
	private static double invisWid(NodeProto proto) {
	  Info info = (Info) infos.get(proto);
	  return info==null ? 0 : info.invisWidth;    
	}
	private static double invisHei(NodeProto proto) {
	  Info info = (Info) infos.get(proto);
	  return info==null ? 0 : info.invisHeight;
	}
	private static double invisWid(ArcProto proto) {
	  Info info = (Info) infos.get(proto);
	  return info==null ? 0 : info.invisWidth;    
	}
	private static boolean isVia(NodeProto proto) {
	  Info info = (Info) infos.get(proto);
	  return info!=null ? info.isVia : false;
	}
	*/
	static {
		Technology tech = Technology.findTechnology("mocmos");

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
		//                       (isVia, invisWid, invisHeight)
		//infos.put(ndiff, new Info(false, 12,       0));
		//infos.put(pdiff, new Info(false, 12,       0));

		pdpin = pdiff.findPinProto();
		ndpin = ndiff.findPinProto();
		p1pin = p1.findPinProto();
		m1pin = m1.findPinProto();
		m2pin = m2.findPinProto();
		m3pin = m3.findPinProto();
		m4pin = m4.findPinProto();
		m5pin = m5.findPinProto();
		m6pin = m6.findPinProto();

		// initialize vias
		for (int i = 0; i < nbLay - 1; i++) {
			vias[i] = findNode(NodeProto.Function.CONTACT,
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

		// The default width of vias reported by via.getBounds() is larger
		// than the width of the visible layers.  From the GUI I determined
		// the width of the visible layers for default sized vias.
		//      default          default         visible
		//      bound box        visible          layer
		//       width            layer           width
		//                        width        with 2 cuts
		// p1m1      10            10               10
		// m1m2      10             9               9
		// m2m3      12            10               9
		// m3m4      12            10               9               
		// m4m5      14            11               9
		// m5m6      16            13               12
		//                     (isVia, invisWid, invisHeight)
		/*
		infos.put(ndm1, new Info(true, 12,       12));
		infos.put(pdm1, new Info(true, 12,       12));
		infos.put(nwm1, new Info(true, 12,       12));
		infos.put(pwm1, new Info(true, 12,       12));
		infos.put(p1m1, new Info(true, 0,        0));
		infos.put(m1m2, new Info(true, 1,        1));
		infos.put(m2m3, new Info(true, 2,        2));
		infos.put(m3m4, new Info(true, 2,        2));
		infos.put(m4m5, new Info(true, 3,        3));
		infos.put(m5m6, new Info(true, 3,        3));
		*/

		// initialize transistors
		nmos = tech.findNodeProto("N-Transistor");
		pmos = tech.findNodeProto("P-Transistor");
		// for scale=1 reported NMOS width = 15 lambda, length = 22 lambda    
		//               actual NMOS width = 3 lambda, length = 2 lambda
		// adjust length by 20, width by 12
		//                      (isVia, invisWid, invisHeight)
		//infos.put(nmos, new Info(false, 12,       20));
		//infos.put(pmos, new Info(false, 12,       20));

		// intialize well
		nwell = tech.findNodeProto("N-Well-Node");
		pwell = tech.findNodeProto("P-Well-Node");
		//                      (isVia, invisWid, invisHeight)
		//infos.put(nwell,new Info(false, 0,        0));
		//infos.put(pwell,new Info(false, 0,        0));

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
	}

	//----------------------------- public methods  ------------------------------
	/*
	public static int lambda2base(double lam) {
	  return (int) Math.round(lam*200);
	}
	public static double base2lambda(int base) {return (double) base/200;}
	
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
	public static PrimitiveNode findNode(NodeProto.Function type, 
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
