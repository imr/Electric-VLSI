/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SogRouterAdapter.java
 *
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.generator.flag.router;

import java.util.ArrayList;
import java.util.List;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.routing.SeaOfGates;
import com.sun.electric.tool.routing.seaOfGates.SeaOfGatesEngine;
import com.sun.electric.tool.routing.seaOfGates.SeaOfGatesEngineFactory;

/**
 * The SogRouter is an adapter between FLAG and Electric's built in Sea of Gates
 * router.
 */
public class SogRouterAdapter {
	private static final boolean SORT_UNROUTED_ARCS = true;
	private static final boolean DUMP_UNROUTED_ARCS = false;
	private final Job job;
	private final SeaOfGatesEngine seaOfGates = SORT_UNROUTED_ARCS ? null : SeaOfGatesEngineFactory
			.createSeaOfGatesEngine();
	private final Technology generic = Technology.findTechnology("Generic");
	private final ArcProto unroutedArc = generic.findArcProto("Unrouted");

	private Cell findParent(List<ToConnect> toConns) {
		for (ToConnect tc : toConns) {
			for (PortInst pi : tc.getPortInsts()) {
				return pi.getNodeInst().getParent();
			}
		}
		return null;
	}

	private List<ArcInst> addUnroutedArcs(Cell cell, List<ToConnect> toConns) {
		Netlist nl = cell.getNetlist();

		List<ArcInst> unroutedArcs = new ArrayList<ArcInst>();
		for (ToConnect tc : toConns) {
			PortInst firstPi = null;
			Network firstNet = null;
			for (PortInst pi : tc.getPortInsts()) {
				if (firstPi == null) {
					firstPi = pi;
					firstNet = nl.getNetwork(firstPi);
					continue;
				}
				Network net = nl.getNetwork(pi);
				if (firstNet == net)
					continue;
				ArcInst ai = ArcInst.newInstanceBase(unroutedArc, 1, firstPi, pi);
				unroutedArcs.add(ai);
			}
		}
		return unroutedArcs;
	}

	// ----------------------------- public methods ---------------------------
	public SogRouterAdapter(Job job) {
		this.job = job;
	}

	public void route(List<ToConnect> toConns, SeaOfGates.SeaOfGatesOptions prefs) {
		Cell cell = findParent(toConns);
		if (cell == null)
			return; // no work to do

		List<ArcInst> arcsToRoute = addUnroutedArcs(cell, toConns);
		if (DUMP_UNROUTED_ARCS) {
			String newName = cell.getName() + "_unrouted{lay}";
			Cell.copyNodeProto(cell, cell.getLibrary(), newName, true);
		}

		if (SORT_UNROUTED_ARCS)
			SeaOfGates.seaOfGatesRoute(cell, prefs);
		else seaOfGates.routeIt(job, cell, arcsToRoute, prefs);
	}
}
