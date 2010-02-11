/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IStructure.java
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
 * Parallel Placement
 * 
 * Abstract Class for data structures
 * 
 * @param <T>
 *            type
 */
public abstract class IStructure<T> {

	/**
	 * Internal class. This class holds the elements of the datastructure
	 * 
	 * @author fschmidt
	 * 
	 * @param <T>
	 */
	@SuppressWarnings("hiding")
	protected class Node<T> {
		public T value;
		public AtomicReference<Node<T>> next = new AtomicReference<Node<T>>(null);

		public Node(T value) {
			this.value = value;
		}
	}

	protected AtomicReference<Node<T>> tail;
	protected AtomicReference<Node<T>> head;

	protected volatile Integer size = Integer.valueOf(0);

	/**
	 * 
	 * @param item
	 */
	public abstract void add(T item);

	/**
	 * Clear the given data structure
	 * 
	 * @throws EmptyException
	 */
	public void clear() throws EmptyException {
		while (!this.isEmpty()) {
			@SuppressWarnings("unused")
			T tmp = this.get();
			tmp = null;
		}

		Node<T> dummy = new Node<T>(null);
		this.tail = new AtomicReference<Node<T>>(dummy);
		this.head = new AtomicReference<Node<T>>(dummy);
		this.size = Integer.valueOf(0);
	}

	/**
	 * @throws EmptyException
	 */
	public abstract T get() throws EmptyException;

	/**
	 * Get the size of the data structure
	 */
	public int getSize() {
		return this.size.intValue();
	}

	/**
	 */
	public abstract boolean isEmpty();

}
