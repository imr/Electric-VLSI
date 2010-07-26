/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MultipleQueuesStructure.java
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
import java.util.Map;

import com.sun.electric.tool.util.CollectionFactory;
import com.sun.electric.tool.util.IStructure;
import com.sun.electric.tool.util.concurrent.patterns.PJob;
import com.sun.electric.tool.util.concurrent.runtime.MultiThreadedRandomizer;

/**
 * @author Felix Schmidt
 * 
 */
public class MultipleQueuesStructure<T> extends IStructure<T> implements IWorkStealing {

	// data queues: each worker has its own worker queue
	protected Map<Long, IStructure<T>> dataQueues;
	// map a operating system thread ID to a data queue
	protected Map<Long, Long> dataQueuesMapping;
	// free internal ids are used for assigning operating system thread IDs to
	// data queues
	protected MultiThreadedRandomizer randomizer;

	private List<Long> freeInternalIds;

	/**
	 * 
	 */
	public MultipleQueuesStructure(int numOfThreads) {
		dataQueues = CollectionFactory.createConcurrentHashMap();
		dataQueuesMapping = CollectionFactory.createConcurrentHashMap();
		this.randomizer = new MultiThreadedRandomizer(numOfThreads);
		freeInternalIds = CollectionFactory.createConcurrentList();

		for (long i = 0; i < numOfThreads; i++) {
			freeInternalIds.add(i);
			dataQueues.put(i, new LockFreeQueue<T>());
			dataQueues.put(i, new FCQueue<T>());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.electric.tool.util.IStructure#add(java.lang.Object)
	 */
	@Override
	public void add(T item) {
		this.add(item, PJob.SERIAL);
	}

	public void add(T item, int i) {
		Long osThreadId = getThreadId();
		Long localQueueId = dataQueuesMapping.get(osThreadId);

		if (i == PJob.SERIAL) {
			if (localQueueId != null) {
				IStructure<T> ownQueue = dataQueues.get(localQueueId);
				if (ownQueue != null) {
					ownQueue.add(item);
					return;
				}
			}
			int foreignQueue = randomizer.getRandomizer().nextInt(dataQueues.size());
			dataQueues.get(Long.valueOf(foreignQueue)).add(item);
		} else {
			dataQueues.get(Long.valueOf(i)).add(item);
		}
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.electric.tool.util.IStructure#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		IStructure<T> ownQueue = dataQueues.get(getThreadId());
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
	@Override
	public T remove() {
		Long osThreadId = getThreadId();
		Long localQueueId = dataQueuesMapping.get(osThreadId);

		if (localQueueId == null) {
			throw new Error("Thread not registered");
		}

		T result = null;

		IStructure<T> ownQueue = dataQueues.get(localQueueId);
		if (ownQueue != null) {
			result = ownQueue.remove();
		}

		return result;
	}

	protected Long getThreadId() {
		return Thread.currentThread().getId();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.electric.tool.util.IStructure#registerThread()
	 */
	@Override
	public void registerThread() {
		if (freeInternalIds.size() > 0) {
			Long myId = freeInternalIds.remove(0);
			dataQueuesMapping.put(getThreadId(), myId);
		}
	}

}
