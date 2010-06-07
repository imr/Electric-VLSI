/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BDEQueue.java
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
package com.sun.electric.tool.util.concurrent.datastructures;

import java.util.List;
import java.util.concurrent.atomic.AtomicStampedReference;

import com.sun.electric.tool.util.CollectionFactory;
import com.sun.electric.tool.util.IDEStructure;

/**
 * 
 * Bounded double ended queue
 * 
 * @param <T>
 * 
 * @author Felix Schmidt
 */
public class BDEQueue<T> extends IDEStructure<T> {

	private List<T> objects;
	private volatile int bottom;
	private AtomicStampedReference<Integer> top;
	private int capacity;
	private volatile int amount;

	/**
	 * Constructor
	 * 
	 * @param capacity
	 *            of bounded double ended queue
	 */
	public BDEQueue(int capacity) {
		objects = CollectionFactory.createArrayList();
		top = new AtomicStampedReference<Integer>(0, 0);
		this.capacity = capacity;
		bottom = 0;
		amount = 0;
	}

	/**
	 * @return element from the top of the data structure
	 */
	@Override
	public T getFromTop() {
		int[] stamp = new int[1];
		int oldTop = top.get(stamp);
		int newTop = oldTop + 1;
		int oldStamp = stamp[0];
		int newStamp = oldStamp + 1;
		if (bottom <= oldTop)
			return null;
		T tmp = objects.get(oldTop);
		if (top.compareAndSet(oldTop, newTop, oldStamp, newStamp)) {
			amount--;
			return tmp;
		}
		return null;
	}

	/**
	 * Add a item to the data structure
	 */
	@Override
	public void add(T item) {
		this.tryAdd(item);
	}

	/**
	 * return true if the data structure is empty; otherwise false
	 */
	@Override
	public boolean isEmpty() {
		return !(top.getReference() < bottom);
	}

	@Override
	public T remove() {
		if (bottom == 0) {
			return null;
		}
		bottom--;
		T tmp = objects.get(bottom);
		int[] stamp = new int[1];
		int oldTop = top.get(stamp);
		int newTop = oldTop + 1;
		int oldStamp = stamp[0];
		int newStamp = oldStamp + 1;
		if (bottom > oldTop) {
			amount--;
			return tmp;
		}
		if (bottom == oldTop) {
			bottom = 0;
			if (top.compareAndSet(oldTop, newTop, oldStamp, newStamp)) {
				amount--;
				return tmp;
			}

		}
		top.set(newTop, newStamp);
		return null;
	}

	@Override
	public boolean isFull() {
		return (amount == capacity);
	}

	public boolean tryAdd(T item) {
		if (this.isFull())
			return false;
		objects.add(item);
		bottom++;
		amount++;
		return true;
	}

}
