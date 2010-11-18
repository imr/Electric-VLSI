/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PJob.java
 *
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.atomic.AtomicReference;

import com.sun.electric.tool.util.concurrent.utils.BlockedRange;

/**
 * Parallel Reduce Job. Parallel reduce is a parallel for loop with a result
 * aggregation at the end of the parallel execution.
 * 
 * @author Felix Schmidt
 */
public class PReduceJob<T, K extends BlockedRange<K>> extends PForJob<K> {

	// TODO use concurrent data structure
	private AtomicReference<PReduceTask<T, K>> mainTask = new AtomicReference<PReduceTask<T, K>>(null);
	private T result;

	public PReduceJob(K range, PReduceTask<T, K> task) {
		super(range, task);
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
	 * @author Felix Schmidt
	 */
	public abstract static class PReduceTask<T, K extends BlockedRange<K>> extends PForTask<K> implements Cloneable {

		/**
		 * reduce function. This function is called after the execution of the
		 * parallel for loop
		 * 
		 * @param other
		 * @return
		 */
		public abstract T reduce(PReduceTask<T, K> other);

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.sun.electric.tool.util.concurrent.patterns.PTask#after()
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void after() {

			PReduceJob<T, K> rjob = (PReduceJob<T, K>) job;

			rjob.mainTask.compareAndSet(null, this);
			rjob.result = rjob.mainTask.get().reduce(this);

			super.after();
		}
	}
}
