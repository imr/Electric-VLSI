/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DEQueue.java
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

import java.util.concurrent.atomic.AtomicReference;

/**
 * 
 * Parallel Placement
 * 
 * <br />
 * <b>How to use this class?</b> <br />
 * This class is intentionaly implemented for work stealing. It provides a add
 * and get method. This methods works like a stack. add pushes a item into the
 * queue and get retrieves this item in a LIFO way. Work stealing needs a double
 * ended version of a queue or stack. So this class provides also a getFromTop
 * method. This method returns a element from the top of the stack. In addition
 * with the add method this class works like a FIFO queue.
 * 
 * @param <T>
 *            type of elements
 */
public class DEQueue<T> extends IDEStructure<T> {

	// start capcacity of the circular array
	// the array uses: 1 << LOG_CAPCITY
	// for LOG_CAPACITY = 4: 1 << 4: b10000 = d16
	private final static int LOG_CAPACITY = 4;
	private volatile CircularArray items;
	private volatile int bottom;
	private AtomicReference<Integer> top;

	/**
	 * Constructor
	 */
	public DEQueue() {
		this.items = new CircularArray(LOG_CAPACITY);
		this.top = new AtomicReference<Integer>(Integer.valueOf(0));
		this.bottom = 0;
	}

	/**
	 * add a element at the bottom
	 */
	@Override
	public void add(T item) {
		int oldBottom = this.bottom;
		int oldTop = this.top.get().intValue();

		CircularArray currentTasks = this.items;
		int size = oldBottom - oldTop;
		if (size >= currentTasks.getCapacity() - 1) {
			currentTasks = currentTasks.resize(oldBottom, oldTop);
			this.items = currentTasks;
		}
		this.items.add(oldBottom, (Runnable) item);
		this.bottom = oldBottom + 1;
		this.size = new Integer(this.size.intValue() + 1);
	}

	/**
	 * retrieves a element from the bottom
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T get() throws EmptyException {

		this.bottom--;
		int oldTop = this.top.get().intValue();
		int newTop = oldTop + 1;
		int size = this.bottom - oldTop;
		if (size < 0) {
			this.bottom = oldTop;
			return null;
		}
		T item = (T) this.items.get(this.bottom);

		if (size > 0) {
			this.size = new Integer(this.size.intValue() - 1);
			return item;
		}
		if (!this.top.compareAndSet(new Integer(oldTop), new Integer(newTop))) {
			item = null;
		}
		this.bottom = oldTop + 1;
		this.size = new Integer(this.size.intValue() - 1);
		return item;
	}

	/**
	 * @return element from the top
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T getFromTop() throws EmptyException {

		int oldTop = this.top.get().intValue();
		int newTop = oldTop + 1;
		int oldBottom = this.bottom;

		int size = oldBottom - oldTop;
		if (size <= 0) {
			return null;
		}
		T item = (T) this.items.get(oldTop);

		if (this.top.compareAndSet(new Integer(oldTop), new Integer(newTop))) {
			this.size = new Integer(this.size.intValue() - 1);
			return item;
		}
		return null;
	}

	@Override
	public boolean isEmpty() {
		int localTop = this.top.get().intValue();
		int localBottom = this.bottom;
		return (localBottom <= localTop);
	}

}
