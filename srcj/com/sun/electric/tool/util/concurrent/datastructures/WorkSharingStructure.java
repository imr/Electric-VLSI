/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WorkSharingStructure.java
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

import com.sun.electric.tool.util.concurrent.runtime.ThreadID;

/**
 * @author Felix Schmidt
 * 
 */
@Deprecated
public class WorkSharingStructure<T> extends WorkStealingStructure<T> {

	/**
	 * @param numOfThreads
	 * @param clazz
	 */
	public WorkSharingStructure(int numOfThreads) {
		super(numOfThreads);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.util.concurrent.datastructures.WorkStealingStructure
	 * #remove()
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

			if (result == null) {
				int size = dataQueues.size();
				if (randomizer.getRandomizer().nextInt(size + 1) == size) {
					int victim = randomizer.getRandomizer().nextInt(dataQueues.size());
					Long min = (victim <= ThreadID.get()) ? victim : localQueueId;
					Long max = (victim <= ThreadID.get()) ? localQueueId : victim;
					balance(dataQueues.get(min), dataQueues.get(max));
				}
			}
		}

		return result;
	}

	private void balance(IDEStructure<T> q0, IDEStructure<T> q1) {

	}

}
