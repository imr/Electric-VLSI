/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: KeeperHigh.java
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

import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.network.*;
import com.sun.electric.database.prototype.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.variable.*;
import com.sun.electric.technology.*;

import com.sun.electric.tool.generator.layout.gates.*;

public class KeeperLow {
	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

	public static Cell makePart(Cell schem, VarContext context,
								StdCellParams stdCell) {
		Iterator nodes = schem.getNodes();

		// extract size information
		double szNmos = -1, szK = -1, szI = -1;
		while (nodes.hasNext()) {
			NodeInst ni = (NodeInst) nodes.next();
			String nm = ni.getProto().getProtoName();
			if (nm.equals("nms1K{ic}")) {
				szNmos = stdCell.getSize(ni, context);
			} else if (nm.equals("invK{ic}")) {
				szK = stdCell.getSize(ni, context);
			} else if (nm.equals("inv{ic}")) {
				szI = stdCell.getSize(ni, context);
			}
		}
		error(szNmos == -1, "KeeperLow: nmos not found");
		error(szK == -1, "KeeperLow: invK not found");
		error(szI == -1, "KeeperLow: inv not found");

		double sz = stdCell.roundSize(szK);
		Cell keep = stdCell.findPart("keeper_low", sz);
		if (keep != null)
			return keep;
		keep = stdCell.newPart("keeper_low", sz);

		NodeProto invKProto = Inv.makePart(szK, stdCell);
		NodeInst invK = LayoutLib.newNodeInst(invKProto, 0, 0, 0, 0, 0, keep);
		NodeProto invIProto = Inv.makePart(szI, stdCell);
		NodeInst invI =LayoutLib.newNodeInst(invIProto, 0, 0, 0, 0, 0, keep);
		NodeProto nmosProto = Nms1.makePart(szNmos, stdCell);
		NodeInst nmos = LayoutLib.newNodeInst(nmosProto, 0, 0, 0, 0, 0, keep);
		ArrayList l = new ArrayList();
		l.add(nmos);
		l.add(invK);
		l.add(invI);
		LayoutLib.abutLeftRight(l);

		// connect up power and ground
		TrackRouter vdd = new TrackRouterH(Tech.m2, 10, keep);
		vdd.connect(new NodeInst[] { invK, invI }, "vdd");

		TrackRouter gnd = new TrackRouterH(Tech.m2, 10, keep);
		gnd.connect(new NodeInst[] { nmos, invK, invI }, "gnd");

		// connect up signal wires
		TrackRouter d =
			new TrackRouterH(Tech.m2, 4,
							 nmos.findPortInst("d").getBounds().getCenterY(),
							 keep);
		d.connect(new PortInst[] {nmos.findPortInst("d"),
								  invK.findPortInst("out"),
								  invI.findPortInst("in")});

		double trackY = stdCell.getTrackY(-1);
		TrackRouter d_bar = new TrackRouterH(Tech.m2, 4, trackY, keep);
		d_bar.connect(new PortInst[] {invK.findPortInst("in"),
									  invI.findPortInst("out")});

		// exports
		Export.newInstance(keep, nmos.findPortInst("g"), "mc")
			.setCharacteristic(Export.Characteristic.IN);
		Export.newInstance(keep, nmos.findPortInst("d"), "d")
			.setCharacteristic(Export.Characteristic.BIDIR);
		Export.newInstance(keep, invK.findPortInst("vdd"), "vdd")
			.setCharacteristic(Export.Characteristic.PWR);
		Export.newInstance(keep, invK.findPortInst("gnd"), "gnd")
			.setCharacteristic(Export.Characteristic.GND);

		// patch well over pulldown
		stdCell.addPmosWell(0, nmos.getBounds().getWidth(), keep);

		// add essential bounds
		stdCell.addEssentialBounds(0, invI.getBounds().getMaxX(), keep);

		// Compare schematic to layout
		/*
		if (stdCell.nccEnabled()) {
			NccOptions options = new NccOptions();
			options.checkExportNames = true;
			options.hierarchical = true;
			options.mergeParallel = true;
			boolean mismatch =
				Electric.networkConsistencyCheck(schem, context, keep, options);
			error(mismatch, "Keeper: gasp cell topological mismatch");
		}
		*/

		return keep;
	}
}
