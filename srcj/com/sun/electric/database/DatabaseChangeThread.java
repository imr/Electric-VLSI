/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DatabaseChangeThread.java
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
package com.sun.electric.database;

public class DatabaseChangeThread extends DatabaseThread
{
	private int[] allocCounts = {};
	
	int allocCellId() {
		int cellId = allocCounts.length;
		int[] newCounts = new int[cellId + 1];
		System.arraycopy(allocCounts, 0, newCounts, 0, allocCounts.length);
		allocCounts = newCounts;
		return cellId;
	}

	int allocNodeId(int cellId) {
		return allocCounts[cellId]++;
	}

	DatabaseChangeThread checkChanging() {
		if (this != Thread.currentThread())
			throw new IllegalStateException("Other thread");
		if (!valid)
			throw new IllegalStateException("database invalid");
		valid = false;
		return this;
	}
	
	void endChanging() {
		assert !valid;
		valid = true;
	}
}
