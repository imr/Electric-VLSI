/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: HistoryEntry.java
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

/**
 * Parallel Placement
 */
public class HistoryEntry<T> {

	private T node1;
	private T node2;

	public HistoryEntry(T node1, T node2) {
		this.node1 = node1;
		this.node2 = node2;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		HistoryEntry other = (HistoryEntry) obj;
		if (this.node1 == null) {
			if (other.node1 != null) {
				return false;
			}
		} else if (!(this.node1.equals(other.node1) || this.node1.equals(other.node2))) {
			return false;
		}
		if (this.node2 == null) {
			if (other.node2 != null) {
				return false;
			}
		} else if (!(this.node2.equals(other.node2) || this.node2.equals(other.node1))) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.node1 == null) ? 0 : this.node1.hashCode());
		result = prime * result + ((this.node2 == null) ? 0 : this.node2.hashCode());
		return result;
	}
}
