/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ViaStack.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;

class ViaStack {
	// ------------------------------private data -----------------------------
	private PortInst port1, port2;

	// ---------------------------- private methods --------------------------
	private void swap() {
		PortInst tp = port1;
		port1 = port2;
		port2 = tp;
	}
	private void buildStack(int hLo, int hHi, double x, double y, double width,
		                    double height, Cell f) {
		PortInst viaBelow = null;
		for (int h=hLo; h<hHi; h++) {
			PrimitiveNode via = Tech.viaAbove(h);

			// Don't let via width or height drop below minimum required for
			// 1 cut.
			double wid = Math.max(width, LayoutLib.getNodeProtoWidth(via));
			double hei = Math.max(height, LayoutLib.getNodeProtoHeight(via));

			PortInst viaAbove = LayoutLib.newNodeInst(via, x, y, wid, hei, 0, 
			                                          f).getOnlyPortInst();
			if (viaBelow == null) {
				// First via in stack
				port1 = viaAbove;
			} else {
				// connect to lower via
				LayoutLib.newArcInst(Tech.layerAtHeight(h), 1, viaBelow,
					                 viaAbove);
			}
			port2 = viaAbove;
			viaBelow = viaAbove;
		}
	}

	// ----------------------------- public methods --------------------------

	// square vias
	public ViaStack(ArcProto arc1, ArcProto arc2, double x, double y, 
	                double width, Cell f) {
		this(arc1, arc2, x, y, width, width, f);
	}

	// rectangular vias
	public ViaStack(ArcProto arc1, ArcProto arc2, double x, double y,
		            double width, double height, Cell f) {
		int h1 = Tech.layerHeight(arc1);
		int h2 = Tech.layerHeight(arc2);
		int deltaZ = h2 - h1;
		if (arc1==arc2) {
			NodeProto pin = arc1.findOverridablePinProto(); 
			double defSz = LayoutLib.DEF_SIZE;
			NodeInst pinInst = LayoutLib.newNodeInst(pin,x,y,defSz,defSz,0,f); 
			port1 =	port2 = pinInst.getOnlyPortInst();
		} else if (deltaZ>0) {
			// arc2 higher than arc1
			buildStack(h1, h2, x, y, width, height, f);
		} else {
			buildStack(h2, h1, x, y, width, height, f);
			swap();
		}
	}

	public PortInst getPort1() {return port1;}
	public PortInst getPort2() {return port2;}
	public double getCenterX() {return port1.getCenter().getX() /*LayoutLib.roundCenterX(port1)*/;}
	public double getCenterY() {return port1.getCenter().getY() /*LayoutLib.roundCenterY(port1)*/;}
}
