/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TrackRouterH.java
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
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.technology.ArcProto;


public class TrackRouterH extends TrackRouter {
	// ---------------------------- public methods ----------------------------
	// all ports lie on the same routing track
	public TrackRouterH(ArcProto lay, double wid, Cell parnt) {
		super(lay, wid, parnt);
	}

	// ports may be offset from routing track
	public TrackRouterH(ArcProto lay, double wid, double centerVal,
		                Cell parnt) {
		super(lay, wid, centerVal, parnt);
	}

	/** Place Via at newPort.X + viaOffset.  If an 'L' shaped connection
	 * is necessary place the horizontal part of the wire at track.CENTER +
	 * wireOffset */
	public void connect(PortInst newPort, double viaOffset, 
	                    double wireOffset) {
		error(newPort==null, "can't connect to null port");

        EPoint centerP = newPort.getCenter();
		// if center isn't set explicitly then infer from first pin
		if (center==null)
			center = new Double(centerP.getY()); // LayoutLib.roundCenterY(newPort));

		ArcProto portLyr = Tech.closestLayer(newPort.getPortProto(), layer);

		double newWid = LayoutLib.widestWireWidth(newPort);

		if (newWid==LayoutLib.DEF_SIZE)  newWid=portLyr.getWidth();

		/* if (viaOffset==0 && wireOffset==0 && center==newPort.getCenterY() &&
		    portLyr==layer) {
		  // Detect when you can connect directly to the port.  This
		  // allows Jon to move a gasp cell by moving any gate.
		  if (prevElbow!=null) portLyr.newInst(width, prevElbow, newPort);
		  prevElbow = newPort;
		} else {   */

		// place a ViaStack at newPort.X + viaOffset
		double x = centerP.getX() /* LayoutLib.roundCenterX(newPort)*/ + viaOffset;

		// 1) If the port layer is the same as the track layer then just
		// drop down a pin. Don't reuse a pin because the part of the
		// connection parallel to the track should have the track width,
		// not the port width.
		//
		// 2) If the port layer differs from the track layer then we need
		// a via. If a via is already close by then use it. Otherwise
		// we'll create a DRC error because contact cuts won't line up.
		PortInst lastPort = null;
		ViaStack closeVia =
			portLyr == layer ? null : findClosestVia(x, portLyr);
		if (closeVia != null) {
			// connect to already existing via
			lastPort = closeVia.getPort2();
		} else {
			// create a new via or pin
			ViaStack vs = new ViaStack(layer, portLyr, x, center.doubleValue(), 
			                           newWid, width, parent);
			insertVia(vs);
			lastPort = vs.getPort2();
		}

		// Connect to new port.
		if (wireOffset != 0) {
			NodeProto pin = portLyr.findOverridablePinProto();
			double defSz = LayoutLib.DEF_SIZE; 
			NodeInst pinInst = 
			  LayoutLib.newNodeInst(pin, LayoutLib.roundCenterX(lastPort), 
			  		                center.doubleValue() + wireOffset,
			                        defSz,
			                        defSz,
			                        0, parent);
			PortInst jog = pinInst.getOnlyPortInst();
			LayoutLib.newArcInst(portLyr, newWid, lastPort, jog);
			lastPort = jog;
		}

		LayoutLib.newArcInst(portLyr, newWid, lastPort, newPort);
	}
}
