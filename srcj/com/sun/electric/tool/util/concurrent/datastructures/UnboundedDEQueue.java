/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UnboundedDEQueue.java
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

import java.util.concurrent.atomic.AtomicInteger;

import com.sun.electric.tool.util.IDEStructure;

/**
 * Unbounded double ended data structure - thread safe - unbounded
 * 
 * @author Felix Schmidt
 */
public class UnboundedDEQueue<T> extends IDEStructure<T> {

	private volatile CircularArray<T> elements;
	private AtomicInteger bottom;
	private AtomicInteger top;

	/**
	 * Constructor
	 */
	public UnboundedDEQueue(Class<T> clazz, int LOG_CAPACITY) {
		elements = new CircularArray<T>(clazz, LOG_CAPACITY);
		top = new AtomicInteger(0);
		bottom = new AtomicInteger(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.placement.forceDirected2.utils.concurrent.IDEStructure
	 * #getFromTop()
	 */
	@Override
	public T getFromTop() {
		int oldTop = top.get();
		int newTop = oldTop + 1;
		int oldBottom = bottom.get();
		int size = oldBottom - oldTop;
		if (size <= 0) {
			return null;
		}
		T elem = elements.get(oldTop);
		if (top.compareAndSet(oldTop, newTop))
			return elem;
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.placement.forceDirected2.utils.concurrent.IStructure
	 * #add(java.lang.Object)
	 */
	@Override
	public void add(T item) {
		while (true) {
			int oldBottom = bottom.get();
			int oldTop = top.get();
			CircularArray<T> currentElements = elements;
			int size = oldBottom - top.get();
			if (size >= currentElements.getCapacity() - 1) {
				currentElements = currentElements.resize(oldBottom, oldTop);
				elements = currentElements;
			}
			elements.add(item, oldBottom);
			if (bottom.compareAndSet(oldBottom, oldBottom + 1))
				break;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.placement.forceDirected2.utils.concurrent.IStructure
	 * #get()
	 */
	@Override
	public T remove() {
		bottom.decrementAndGet();
		int oldTop = top.get();
		int newTop = oldTop + 1;
		int size = bottom.get() - oldTop;
		if (size < 0) {
			bottom.set(oldTop);
			return null;
		}
		T item = elements.get(bottom.get());
		if (size > 0) {
			return item;
		}
		if (!top.compareAndSet(oldTop, newTop)) {
			item = null;
		}
		bottom.set(oldTop + 1);
		return item;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.placement.forceDirected2.utils.concurrent.IStructure
	 * #isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return (bottom.get() <= top.get());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.electric.tool.util.IDEStructure#isFull()
	 */
	@Override
	@Deprecated
	public boolean isFull() {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.electric.tool.util.IDEStructure#tryAdd(java.lang.Object)
	 */
	@Override
	@Deprecated
	public boolean tryAdd(T item) {
		throw new UnsupportedOperationException();
	}

}
