/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Scheduler.java
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
package com.sun.electric.tool.util.concurrent.runtime;

import com.sun.electric.tool.util.CollectionFactory;
import com.sun.electric.tool.util.IStructure;
import com.sun.electric.tool.util.concurrent.datastructures.MultipleQueuesStructure;
import com.sun.electric.tool.util.concurrent.datastructures.WorkStealingStructure;
import com.sun.electric.tool.util.concurrent.patterns.PTask;

/**
 * @author fs239085
 * 
 */
public class Scheduler {

	public enum SchedulingStrategy {
		queue, stack, workStealing, multipleQueues, fcQueue;
	}

	public static IStructure<PTask> createScheduler(SchedulingStrategy strategy, int numOfThreads)
			throws UnknownSchedulerException {
		IStructure<PTask> result = null;

		if (strategy.equals(SchedulingStrategy.queue)) {
			result = CollectionFactory.createLockFreeQueue();
		} else if (strategy.equals(SchedulingStrategy.stack)) {
			result = CollectionFactory.createLockFreeStack();
		} else if (strategy.equals(SchedulingStrategy.workStealing)) {
			result = WorkStealingStructure.createForThreadPool(numOfThreads);
		} else if (strategy.equals(SchedulingStrategy.multipleQueues)) {
			result = new MultipleQueuesStructure<PTask>(numOfThreads);
		} else if (strategy.equals(SchedulingStrategy.fcQueue)) {
			result = CollectionFactory.createFCQueue();
		} else {
			throw new UnknownSchedulerException();
		}

		return result;
	}

	public static class UnknownSchedulerException extends Exception {

	}

	public static String getAvailableScheduler() {
		StringBuilder builder = new StringBuilder();
		for (SchedulingStrategy strategy : SchedulingStrategy.values()) {
			builder.append(strategy.toString());
			builder.append(", ");
		}
		return builder.substring(0, builder.length() - 2);
	}

}
