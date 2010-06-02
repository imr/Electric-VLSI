/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IStructure.java
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
package com.sun.electric.tool.util;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.electric.tool.util.concurrent.datastructures.IWorkStealing;

/**
 * 
 * Common base class for concurrent data structures. This class provides also
 * some useful helpers for concurrent datastructures, such as internal node
 * objects and a backoff algorithm
 * 
 * @param <T>
 */
public abstract class IStructure<T> implements IWorkStealing {

	/**
	 * add a object of type T
	 * 
	 * @throws FullException
	 */
	public abstract void add(T item);

	/**
	 * retrieve a object of type T
	 * 
	 * @return object of type T
	 */
	public abstract T remove();

	/**
	 * true if the data structure is empty (contains no elements); otherwise
	 * false
	 * 
	 * @return
	 */
	public abstract boolean isEmpty();

	/**
	 * 
	 * Internal data structure for storing objects and link them together
	 * 
	 * @param <T>
	 */
	protected static class Node<T> {
		public T value;
		public AtomicReference<Node<T>> next = new AtomicReference<Node<T>>(null);

		public Node(T value) {
			this.value = value;
		}
	}

	/**
	 * 
	 * Backoff algorithm for delays
	 * 
	 */
	protected static class Backoff {
		private final int minDelay, maxDelay;
		private int limit;
		private final Random random;

		public Backoff(int min, int max) {
			minDelay = min;
			maxDelay = max;
			limit = minDelay;
			random = new Random(System.currentTimeMillis());
		}

		public void backoff() throws InterruptedException {
			int delay = random.nextInt(limit);
			limit = Math.min(maxDelay, 2 * limit);
			Thread.sleep(delay);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.sun.electric.tool.util.concurrent.datastructures.IWorkStealing#
	 * registerThread()
	 */
	public void registerThread() {
	}

}
