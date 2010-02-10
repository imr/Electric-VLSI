/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OverlapHistoryAtomics.java
 * Written by Team 7: Felix Schmidt, Daniel Lechner
 * 
 * This code has been developed at the Karlsruhe Institute of Technology (KIT), Germany, 
 * as part of the course "Multicore Programming in Practice: Tools, Models, and Languages".
 * Contact instructor: Dr. Victor Pankratius (pankratius@ipd.uka.de)
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.placement.forceDirected2.forceDirected.util.history;

import java.util.concurrent.atomic.AtomicReference;

import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;

/**
 * Parallel Placement
 */
public class OverlapHistoryAtomics implements IOverlapHistory<PlacementNode> {

	public static OverlapHistoryAtomics getInstance() {
		return instance;
	}

	private AtomicReference<OverlapHistoryNone> history;

	private static OverlapHistoryAtomics instance = new OverlapHistoryAtomics();

	private OverlapHistoryAtomics() {
		this.history.set(OverlapHistoryNone.getInstance());
	}

	public boolean isMovementInHistory(PlacementNode node1, PlacementNode node2) {
		return this.history.get().isMovementInHistory(node1, node2);
	}

	public void removeHistory(PlacementNode node1, PlacementNode node2) {
		this.history.get().removeHistory(node1, node2);
	}

	public void saveHistory(PlacementNode node1, PlacementNode node2) {
		this.history.get().saveHistory(node1, node2);
	}

}
