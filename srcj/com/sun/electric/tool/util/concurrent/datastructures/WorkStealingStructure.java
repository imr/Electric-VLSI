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

import java.util.List;
import java.util.Map;
import java.util.Random;

import com.sun.electric.tool.util.CollectionFactory;
import com.sun.electric.tool.util.IDEStructure;
import com.sun.electric.tool.util.IStructure;
import com.sun.electric.tool.util.concurrent.patterns.PJob;

/**
 * This data structure is a wrapper for work stealing. Each worker has a own
 * data queue. The methods add and remove pick one data queue (own, specific,
 * random) to retrieve or add a element. This data structure is thread-safe.
 * 
 * @author Felix Schmidt
 * 
 */
public class WorkStealingStructure<T> extends IStructure<T> implements IWorkStealing {

	// data queues: each worker has its own worker queue
	private Map<Long, IDEStructure<T>> dataQueues;
	// map a operating system thread ID to a data queue
	private Map<Long, Long> dataQueuesMapping;
	// free internal ids are used for assigning operating system thread IDs to
	// data queues
	private List<Long> freeInternalIds;
	// each worker gets its own randomizer
	private Map<Long, Random> rand;
	private Class<T> clazz;

	public WorkStealingStructure(int numOfThreads, Class<T> clazz) {
		dataQueues = CollectionFactory.createConcurrentHashMap();
		dataQueuesMapping = CollectionFactory.createConcurrentHashMap();
		rand = CollectionFactory.createConcurrentHashMap();
		freeInternalIds = CollectionFactory.createConcurrentList();
		this.clazz = clazz;

		for (long i = 0; i < numOfThreads; i++) {
			freeInternalIds.add(i);
			dataQueues.put(i, CollectionFactory.createUnboundedDoubleEndedQueue(this.clazz));
			rand.put(i, new Random(System.currentTimeMillis()));
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

	/**
	 * Add a item to a data queue <br>
	 * <b>Algorithm</b><br>
	 * <ul>
	 * <li>get operating system thread ID</li>
	 * <li>get local queue (mapping)</li>
	 * <li>assign item to own queue if available</li>
	 * <li>otherwise pick a random data queue</li><br>
	 * <li>if i != -1, then add item to given data queue</li>
	 * </ul>
	 * 
	 * @param item
	 *            add this item to one data queue
	 * @param i
	 *            if you want to add the item to a data queue of your choice use
	 *            the number of the data queue [0..numOfThreads], otherwise (-1,
	 *            PJob.SERIAL) add it to the own data queue or pick a random one
	 */
	@Override
	public void add(T item, int i) {

		Long osThreadId = getThreadId();
		Long localQueueId = dataQueuesMapping.get(osThreadId);

		if (i == PJob.SERIAL) {
			if (localQueueId != null) {
				IDEStructure<T> ownQueue = dataQueues.get(localQueueId);
				if (ownQueue != null) {
					ownQueue.add(item);
					return;
				}
			}
			Random randomizer = new Random(System.currentTimeMillis());
			int foreignQueue = randomizer.nextInt(dataQueues.size());
			dataQueues.get(Long.valueOf(foreignQueue)).add(item);

		} else {
			dataQueues.get(i).add(item);
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

	/**
	 * Remove a item from one data queue
	 * 
	 * <b>Algorithm</b><br>
	 * <ul>
	 * <li>get operating system thread ID</li>
	 * <li>get local queue (mapping)</li>
	 * <li>remove item from own queue</li>
	 * <li>if the item is equal to null pick a random victim queue (iterate over
	 * all queues)</li>
	 * <li>return item</li>
	 * </ul>
	 * 
	 * @return a item from one queue, or null, if all queues are empty
	 */
	@Override
	public T remove() {
		Long osThreadId = getThreadId();
		Long localQueueId = dataQueuesMapping.get(osThreadId);

		if (localQueueId == null) {
			throw new Error("Thread not registered");
		}

		T result = null;

		IDEStructure<T> ownQueue = dataQueues.get(localQueueId);
		if (ownQueue != null) {
			result = ownQueue.remove();
		}

		for (int i = 1; result == null && i < dataQueues.size(); i++) {
			result = dataQueues.get(Long.valueOf(i + localQueueId) % dataQueues.size()).getFromTop();
		}

		return result;
	}

	/**
	 * Add a thread to the data queue mapping
	 */
	@Override
	public synchronized void registerThread() {
		if (freeInternalIds.size() > 0) {
			Long myId = freeInternalIds.remove(0);
			dataQueuesMapping.put(getThreadId(), myId);
		}
	}
}
