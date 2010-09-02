/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LockFreeQueue.java
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

import java.util.concurrent.atomic.AtomicReference;


/**
 * 
 * Thread safe, lock free, concurrent queue
 * 
 * @param <T>
 * 
 * @author Felix Schmidt
 */
public class LockFreeQueue<T> extends IStructure<T> {

	AtomicReference<Node<T>> head = null;
	AtomicReference<Node<T>> tail = null;

	/**
	 * Constructor
	 */
	public LockFreeQueue() {
		Node<T> dummy = new Node<T>(null);
		head = new AtomicReference<Node<T>>(dummy);
		tail = new AtomicReference<Node<T>>(dummy);
	}

	@Override
	public void add(T item) {
		Node<T> node = new Node<T>(item);
		while (true) {
			Node<T> last = tail.get();
			Node<T> next = last.next.get();
			if (last == tail.get()) {
				if (next == null) {
					if (last.next.compareAndSet(next, node)) {
						tail.compareAndSet(last, node);
						return;
					}
				} else {
					tail.compareAndSet(last, next);
				}
			}
		}
	}

	@Override
	public boolean isEmpty() {
		Node<T> first = head.get();
		Node<T> last = tail.get();
		Node<T> next = first.next.get();
		if (first == head.get()) {
			if (first == last) {
				if (next == null) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public T remove() {
		while (true) {
			Node<T> first = head.get();
			Node<T> last = tail.get();
			Node<T> next = first.next.get();
			if (first == head.get()) {
				if (first == last) {
					if (next == null) {
						return null;
					}
					tail.compareAndSet(last, next);
				} else {
					T value = next.value;
					if (head.compareAndSet(first, next))
						return value;
				}
			}
		}
	}

}
