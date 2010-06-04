/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WorkStealingStructure.java
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

import java.util.Map;
import java.util.Random;

import com.sun.electric.tool.util.CollectionFactory;
import com.sun.electric.tool.util.IDEStructure;
import com.sun.electric.tool.util.IStructure;

/**
 * @author fs239085
 * 
 */
public class WorkStealingStructure<T> extends IStructure<T> implements IWorkStealing {

	private Map<Long, IDEStructure<T>> dataQueues;
	private Map<Long, Random> rand;

	public WorkStealingStructure(int numOfThreads) {
		dataQueues = CollectionFactory.createConcurrentHashMap();
		rand = CollectionFactory.createConcurrentHashMap();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.electric.tool.util.IStructure#add(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void add(T item) {
		IDEStructure<T> ownQueue = dataQueues.get(getThreadId());
		if (ownQueue != null) {
			ownQueue.add(item);
		} else {
			Random myRandomizer = rand.get(getThreadId());
			if (myRandomizer == null) {
				myRandomizer = new Random(System.currentTimeMillis());
			}
			int foreignQueue = myRandomizer.nextInt(Math.max(dataQueues.size() - 1, 1));
			((IDEStructure<T>) dataQueues.values().toArray()[foreignQueue]).add(item);
		}
	}

	private Long getThreadId() {
		return Thread.currentThread().getId();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.electric.tool.util.IStructure#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		IDEStructure<T> ownQueue = dataQueues.get(getThreadId());
		if (ownQueue != null) {
			return ownQueue.isEmpty();
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.electric.tool.util.IStructure#remove()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T remove() {
		IDEStructure<T> ownQueue = dataQueues.get(getThreadId());
		if (ownQueue != null) {
			return ownQueue.remove();
		} else {
			Random myRandomizer = rand.get(getThreadId());
			int foreignQueue = myRandomizer.nextInt(dataQueues.size() - 1);
			return ((IDEStructure<T>) dataQueues.values().toArray()[foreignQueue]).getFromTop();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.sun.electric.tool.util.concurrent.datastructures.IWorkStealing#
	 * registerThread()
	 */
	public void registerThread() {
		IDEStructure<T> bdequeue = CollectionFactory.createBoundedDoubleEndedQueue(200);
		Long threadId = getThreadId();
		dataQueues.put(threadId, bdequeue);
		rand.put(threadId, new Random(System.currentTimeMillis()));
	}

}
