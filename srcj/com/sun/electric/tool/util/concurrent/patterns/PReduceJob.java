/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PJob.java
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
package com.sun.electric.tool.util.concurrent.patterns;

import java.util.List;

import com.sun.electric.tool.util.CollectionFactory;

/**
 * Parallel Reduce Job. Parallel reduce is a parallel for loop with a result
 * aggregation at the end of the parallel execution.
 */
public class PReduceJob<T> extends PForJob {

	// TODO use concurrent data structure
	private List<PReduceTask<T>> tasks = CollectionFactory.createArrayList();
	private T result;

	public PReduceJob(BlockedRange range, PReduceTask<T> task) {
		super(range, task);
	}

	@Override
	public void execute() {
		long start = System.currentTimeMillis();
		super.execute();

		// TODO debug
		System.out.println("par part: " + (System.currentTimeMillis() - start));

		result = null;
		PReduceTask<T> oldTask = null;

		start = System.currentTimeMillis();
		// TODO parallize it
		if (tasks.size() > 0) {
			if (tasks.size() == 1) {
				result = tasks.get(0).reduce(null);
			} else {
				for (PReduceTask<T> task : tasks) {
					if (oldTask != null) {
						result = task.reduce(oldTask);
					}
					oldTask = task;
				}
			}
		}

		// TODO debug
		System.out.println("red part: " + (System.currentTimeMillis() - start));
	}

	/**
	 * get the aggregated result
	 * 
	 * @return
	 */
	public T getResult() {
		return result;
	}

	/**
	 * 
	 * @author fs239085
	 */
	public abstract static class PReduceTask<T> extends PForTask implements Cloneable {

		/**
		 * reduce function. This function is called after the execution of the
		 * parallel for loop
		 * 
		 * @param other
		 * @return
		 */
		public abstract T reduce(PReduceTask<T> other);

	}

	@SuppressWarnings("unchecked")
	@Override
	public void add(PTask task) {
		super.add(task);
		if (PForTaskWrapper.class.isInstance(task))
			tasks.add((PReduceTask<T>) ((PForTaskWrapper) task).getTask());
	}
}
