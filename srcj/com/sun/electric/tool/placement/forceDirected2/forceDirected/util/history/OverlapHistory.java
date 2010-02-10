/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OverlapHistory.java
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

import java.util.HashMap;
import java.util.Map;

/**
 * Parallel Placement
 */
public abstract class OverlapHistory<T> implements IOverlapHistory<T> {

	private Map<T, HistoryEntry<T>> history = new HashMap<T, HistoryEntry<T>>();

	public boolean isMovementInHistory(T node1, T node2) {

		HistoryEntry<T> entry = new HistoryEntry<T>(node1, node2);

		HistoryEntry<T> e1 = this.history.get(node1);
		HistoryEntry<T> e2 = this.history.get(node2);

		if ((e1 == null) || (e2 == null)) {
			return false;
		}

		if (entry.equals(e1) || entry.equals(e2)) {
			return true;
		}

		return false;
	}

	public void removeHistory(T node1, T node2) {
		this.history.remove(node1);
		this.history.remove(node2);
	}

	public void saveHistory(T node1, T node2) {
		HistoryEntry<T> entry = new HistoryEntry<T>(node1, node2);
		this.history.put(node1, entry);
		this.history.put(node2, entry);
	}
}
