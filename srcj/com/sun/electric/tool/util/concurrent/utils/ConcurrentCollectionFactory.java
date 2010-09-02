/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ConcurrentCollectionFactory.java
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
package com.sun.electric.tool.util.concurrent.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.sun.electric.tool.util.concurrent.datastructures.BDEQueue;
import com.sun.electric.tool.util.concurrent.datastructures.FCQueue;
import com.sun.electric.tool.util.concurrent.datastructures.LockFreeQueue;
import com.sun.electric.tool.util.concurrent.datastructures.LockFreeStack;
import com.sun.electric.tool.util.concurrent.datastructures.UnboundedDEQueue;

/**
 * @author Felix Schmidt
 * 
 */
public class ConcurrentCollectionFactory {
	
	private static final int LOG_CAPACITY = 4;

	protected ConcurrentCollectionFactory() {

	}

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

	@SuppressWarnings("unchecked")
	public static <T> List<T> createConcurrentList() {
		return (List<T>) Collections.synchronizedList(createArrayList());
	}

	public static <T> ConcurrentLinkedQueue<T> createConcurrentLinkedQueue() {
		return new ConcurrentLinkedQueue<T>();
	}

	public static <T, V> ConcurrentHashMap<T, V> createConcurrentHashMap() {
		return new ConcurrentHashMap<T, V>();
	}

	public static <T> Set<T> createConcurrentHashSet() {
		return (Set<T>) Collections.synchronizedSet(ConcurrentCollectionFactory.createHashSet());
	}

	public static <T> Set<T> copySetToConcurrent(Set<T> source) {

		Set<T> result = ConcurrentCollectionFactory.createConcurrentHashSet();

		doCopyCollection(source, result);

		return result;

	}

	public static <T> LinkedList<T> createLinkedList() {
		return new LinkedList<T>();
	}

	public static <T> LockFreeQueue<T> createLockFreeQueue() {
		return new LockFreeQueue<T>();
	}

	public static <T> LockFreeStack<T> createLockFreeStack() {
		return new LockFreeStack<T>();
	}
	
	/**
	 * Create a new double ended queue (concurrent).
	 */
	public static <T> UnboundedDEQueue<T> createUnboundedDoubleEndedQueue() {
		return new UnboundedDEQueue<T>(LOG_CAPACITY);
	}
	
	/**
	 * Create a new double ended queue (concurrent).
	 */
	public static <T> BDEQueue<T> createBoundedDoubleEndedQueue(int capacity) {
		return new BDEQueue<T>(capacity);
	}

	public static <T> FCQueue<T> createFCQueue() {
		return new FCQueue<T>();
	}

	protected static <T> void doCopyCollection(Collection<T> source, Collection<T> dest) {
		for (Iterator<T> it = source.iterator(); it.hasNext();) {
			dest.add(it.next());
		}
	}
	
	public static <T> T threadSafeListGet(int index, List<T> list) {
		synchronized (list) {
			return list.get(index);
		}
	}

	public static <T> T threadSafeListRemove(int index, List<T> list) {
		synchronized (list) {
			return list.remove(index);
		}
	}
	
	/**
	 * 
	 * @param <T>
	 * @param item
	 * @param list
	 */
	public static <T> void threadSafeListAdd(T item, List<T> list) {
		synchronized (list) {
			list.add(item);
		}
	}

}
