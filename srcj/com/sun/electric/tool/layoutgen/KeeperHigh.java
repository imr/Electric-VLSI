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
package com.sun.electric.tool.layoutgen;

import java.io.*;
import java.util.*;
import java.awt.*;

import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.network.*;
import com.sun.electric.database.prototype.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.variable.*;
import com.sun.electric.technology.*;

import com.sun.electric.tool.layoutgen.gategen.*;

public class KeeperHigh {
	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

	public static Cell makePart(Cell schem, VarContext context,
								StdCellParams stdCell) {
		schem.rebuildNetworks(null, false);

		Iterator nodes = schem.getNodes();

		// extract size information
		double szMc = -1, szPmos = -1, szK = -1, szI = -1;
		while (nodes.hasNext()) {
			NodeInst ni = (NodeInst) nodes.next();
			String nm = ni.getProto().getProtoName();
			if (nm.equals("pms1K{ic}")) {
				szPmos = stdCell.getSize(ni, context);
			} else if (nm.equals("invK{ic}")) {
				szK = stdCell.getSize(ni, context);
			} else if (nm.equals("inv{ic}")) {
				JNetwork net = ni.findPortInst("in").getNetwork();
				if (net.hasName("mc")) {
					szMc = stdCell.getSize(ni, context);
				} else if (net.hasName("d")) {
					szI = stdCell.getSize(ni, context);
				} else {
					System.out.println("Unrecognized net: ");
					Iterator it = net.getNames();
					while (it.hasNext()) {
						System.out.println((String) it.next());
					}
					error(true, "unrecognized net");
				}
			}
		}
		error(szMc == -1, "KeeperHigh: inv bufferring mc not found");
		error(szPmos == -1, "KeeperHigh: pmos not found");
		error(szK == -1, "KeeperHigh: invK not found");
		error(szI == -1, "KeeperHigh: inv driving invK not found");

		double sz = stdCell.roundSize(szK);
		Cell keep = stdCell.findPart("keeper_high", sz);
		if (keep != null)
			return keep;
		keep = stdCell.newPart("keeper_high", sz);

		NodeProto invKProto = Inv.makePart(szK, stdCell);
		NodeInst invK = LayoutLib.newNodeInst(invKProto, 0, 0, 0, 0, 0, keep);
		NodeProto invIProto = Inv.makePart(szI, stdCell);
		NodeInst invI = LayoutLib.newNodeInst(invIProto, 0, 0, 0, 0, 0, keep);
		NodeProto pmosProto = Pms1.makePart(szPmos, stdCell);
		NodeInst pmos = LayoutLib.newNodeInst(pmosProto, 0, 0, 0, 0, 0, keep);
		NodeProto mcProto = Inv.makePart(szMc, stdCell);
		NodeInst mc = LayoutLib.newNodeInst(mcProto, 0, 0, 0, 0, 0, keep);

		ArrayList l = new ArrayList();
		l.add(mc);
		l.add(pmos);
		l.add(invK);
		l.add(invI);
		LayoutLib.abutLeftRight(l);

		/* doesn't work, either it never worked or it stopped working
		// Mirror invK left to right so I don't have to use a metal-2 track.
		invK.mirrorLeftToRight();
		*/

		// connect up power and ground
		TrackRouter vdd = new TrackRouterH(Tech.m2, 10, keep);
		vdd.connect(new NodeInst[] { mc, pmos, invK, invI }, "vdd");

		TrackRouter gnd = new TrackRouterH(Tech.m2, 10, keep);
		gnd.connect(new NodeInst[] { mc, invK, invI }, "gnd");

		// connect up signal wires
		TrackRouter d =
			new TrackRouterH(Tech.m2, 4,
							 pmos.findPortInst("d").getBounds().getCenterY(),
							 keep);
		d.connect(new PortInst[] {pmos.findPortInst("d"),
								  invK.findPortInst("out"),
								  invI.findPortInst("in")});

		/* Use this when I finally get mirroring to work (again?)
		//double trackY = -11;
		*/
		double trackY = stdCell.getTrackY(-1);
		TrackRouter d_bar = new TrackRouterH(Tech.m2, 4, trackY, keep);
		d_bar.connect(new PortInst[] {invK.findPortInst("in"),
									  invI.findPortInst("out")});
		TrackRouter mc_bar =
			new TrackRouterH(Tech.m1, 4,
							 pmos.findPortInst("g").getBounds().getCenterY(),
							 keep);
		mc_bar.connect(new PortInst[] {mc.findPortInst("out"),
									   pmos.findPortInst("g")});

		// exports
		Export.newInstance(keep, mc.findPortInst("in"), "mc")
			.setCharacteristic(Export.Characteristic.IN);
		Export.newInstance(keep, pmos.findPortInst("d"), "d")
			.setCharacteristic(Export.Characteristic.BIDIR);
		Export.newInstance(keep, invK.findPortInst("vdd"), "vdd")
			.setCharacteristic(Export.Characteristic.PWR);
		Export.newInstance(keep, invK.findPortInst("gnd"), "gnd")
			.setCharacteristic(Export.Characteristic.GND);

		// patch well over pullup
		stdCell.addNmosWell(
			mc.getBounds().getWidth(),
			mc.getBounds().getWidth() + pmos.getBounds().getWidth(),
			keep);

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
			error(mismatch, "KeeperHigh topological mismatch");
		}
		*/

		return keep;
	}
}
