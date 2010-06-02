/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CollectionFactory.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.electric.tool.util.concurrent.datastructures.BDEQueue;
import com.sun.electric.tool.util.concurrent.datastructures.LockFreeQueue;
import com.sun.electric.tool.util.concurrent.datastructures.LockFreeStack;

/**
 * This class provides factory methods for creating data structures. The
 * intension is that the generic generation of data structures <T> should be
 * hidden to make the code readable.
 * 
 */
public class CollectionFactory {

	/**
	 * Create a new array list.
	 */
	public static <T> ArrayList<T> createArrayList() {
		return new ArrayList<T>();
	}

	/**
	 * Create a new hash set
	 * 
	 * @param <T>
	 * @return HashSet of type T
	 */
	public static <T> HashSet<T> createHashSet() {
		return new HashSet<T>();
	}

	/**
	 * Create a new linked list.
	 */
	public static <T> LinkedList<T> createLinkedList() {
		return new LinkedList<T>();
	}

	/**
	 * Create a new lock free queue (concurrent).
	 */
	public static <T> LockFreeQueue<T> createLockFreeQueue() {
		return new LockFreeQueue<T>();
	}

	/**
	 * Create a new lock free stack (concurrent).
	 */
	public static <T> LockFreeStack<T> createLockFreeStack() {
		return new LockFreeStack<T>();
	}

	/**
	 * Create a new double ended queue (concurrent).
	 */
	public static <T> BDEQueue<T> createBoundedDoubleEndedQueue(int capacity) {
		return new BDEQueue<T>(capacity);
	}

	/**
	 * create a new concurrent hash map
	 */
	public static <T, K> ConcurrentHashMap<T, K> createConcurrentHashMap() {
		return new ConcurrentHashMap<T, K>();
	}

	/**
	 * create concurrent linked list
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> createConcurrentLinkedList() {
		return (List<T>) Collections.synchronizedList(createArrayList());
	}
}
