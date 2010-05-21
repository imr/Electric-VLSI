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

import com.sun.electric.tool.util.IStructure;

/**
 * 
 * Thread safe, lock free, concurrent queue 
 *
 * @param <T>
 */
public class LockFreeQueue<T> extends IStructure<T> {

	AtomicReference<Node<T>> head = new AtomicReference<Node<T>>(null);
	AtomicReference<Node<T>> tail = new AtomicReference<Node<T>>(null);

	@Override
	public void add(T item) {
		Node<T> node = new Node<T>(item);
		while (true) {
			Node<T> last = tail.get();
			Node<T> next = last.next;
			if (last == tail.get()) {
				if (next == null) {
					last.next = node;
					tail.compareAndSet(last, node);
				} else {
					tail.compareAndSet(last, next);
				}
			}
		}
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public T remove() {
		// TODO Auto-generated method stub
		return null;
	}

}
