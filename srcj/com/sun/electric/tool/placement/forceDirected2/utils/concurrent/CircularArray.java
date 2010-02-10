/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CircularArray.java
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
package com.sun.electric.tool.placement.forceDirected2.utils.concurrent;

/**
 * Parallel Placement
 * 
 * Internal data structure for work stealing
 */
public class CircularArray extends IStructure<Runnable> {

	private int logCapacity;
	private Runnable[] currentItems;

	public CircularArray(int myLogCapacity) {
		this.logCapacity = myLogCapacity;
		this.currentItems = new Runnable[1 << this.logCapacity];
	}

	public void add(int i, Runnable item) {
		this.currentItems[i % this.getCapacity()] = item;

	}

	@Override
	@Deprecated
	public void add(Runnable item) {

		this.add(0, item);

	}

	@Override
	public Runnable get() throws EmptyException {
		return this.get(0);
	}

	public Runnable get(int i) throws EmptyException {
		return this.currentItems[i % this.getCapacity()];
	}

	public int getCapacity() {
		return 1 << this.logCapacity;
	}

	@Override
	@Deprecated
	public boolean isEmpty() {
		return false;
	}

	public CircularArray resize(int bottom, int top) {
		CircularArray newTasks = new CircularArray(this.logCapacity + 1);
		for (int i = top; i < bottom; i++) {
			try {
				newTasks.add(i, this.get(i));
			} catch (EmptyException e) {
				e.printStackTrace();
			}
		}
		return newTasks;
	}

}
