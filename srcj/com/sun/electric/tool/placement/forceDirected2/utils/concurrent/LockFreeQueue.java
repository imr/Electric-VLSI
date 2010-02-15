/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LockFreeQueue.java
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
 * LockFree Datastructure: <b>Queue</b>
 */
public class LockFreeQueue<T> extends IStructure<T> {

	/**
	 * Constructor
	 */
	public LockFreeQueue() {
		Node<T> dummy = new Node<T>(null);
		this.tail = new AtomicReference<Node<T>>(dummy);
		this.head = new AtomicReference<Node<T>>(dummy);

	}

	/**
	 * add a item
	 */
	@Override
	public void add(T item) {
		Node<T> node = new Node<T>(item);
		while (true) {
			Node<T> last = this.tail.get();
			Node<T> next = last.next.get();

			if (last == this.tail.get()) {
				if (next == null) {
					if (last.next.compareAndSet(next, node)) {
						this.tail.compareAndSet(last, node);
						this.size = new Integer(this.size.intValue() + 1);
						return;
					}
				} else {
					this.tail.compareAndSet(last, next);
				}
			}
		}
	}

	/**
	 * get and remove a item
	 */
	@Override
	public T get() throws EmptyException {
		while (true) {
			Node<T> first = this.head.get();
			Node<T> last = this.tail.get();
			Node<T> next = first.next.get();
			if (first == this.head.get()) {
				if (first == last) {
					if (next == null) {
						throw new EmptyException();
					}
					this.tail.compareAndSet(last, next);
				} else {
					T value = next.value;
					if (this.head.compareAndSet(first, next)) {
						this.size = new Integer(this.size.intValue() - 1);
						return value;
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.placement.utils.concurrent.IStructure#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		Node<T> first = this.head.get();
		Node<T> last = this.tail.get();
		Node<T> next = first.next.get();
		if (first == this.head.get()) {
			if (first == last) {
				if (next == null) {
					return true;
				}
			}
		}
		return false;
	}

}
