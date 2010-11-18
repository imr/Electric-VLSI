/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PForJob.java
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

import java.util.List;

import com.sun.electric.tool.util.concurrent.runtime.taskParallel.IThreadPool;
import com.sun.electric.tool.util.concurrent.utils.BlockedRange;
import com.sun.electric.tool.util.concurrent.utils.ConcurrentCollectionFactory;
import com.sun.electric.tool.util.concurrent.utils.Range;

/**
 * 
 * Runtime for parallel for
 * 
 * @author Felix Schmidt
 * 
 */
public class PForJob<T extends BlockedRange<T>> extends PJob {

	/**
	 * Constructor for 1- and 2-dimensional parallel for loops
	 * 
	 * @param range
	 * @param task
	 */
	public PForJob(T range, PForTask<T> task) {
		super();
		this.add(new SplitIntoTasks<T>(this, range, task), PJob.SERIAL);
	}

	public PForJob(T range, PForTask<T> task, IThreadPool pool) {
		super(pool);
		this.add(new SplitIntoTasks<T>(this, range, task), PJob.SERIAL);
	}

	/**
	 * 
	 * Base task for parallel for
	 * 
	 */
	public abstract static class PForTask<T extends BlockedRange<T>> extends PTask implements Cloneable {

		protected T range;

		public PForTask(PJob job, T range) {
			super(job);
			this.range = range;
		}

		public PForTask() {
			super(null);
		}

		protected void setRange(T range) {
			this.range = range;
		}

		/**
		 * set current job
		 * 
		 * @param job
		 */
		public void setPJob(PJob job) {
			this.job = job;
		}

	}

	/**
	 * 
	 * Task to create parallel for tasks (internal)
	 * 
	 */
	public final static class SplitIntoTasks<T extends BlockedRange<T>> extends PTask {

		private T range;
		private PForTask<T> task;

		public SplitIntoTasks(PJob job, T range, PForTask<T> task) {
			super(job);
			this.range = range;
			this.task = task;
		}

		/**
		 * This is the executor method of SplitIntoTasks. New for tasks will be
		 * created while a new range is available
		 */
		@Override
		public void execute() {
			int threadNum = job.getThreadPool().getPoolSize();
			for (int i = 0; i < threadNum; i++) {
				job.add(new SplitterTask<T>(job, range, task, i, threadNum));
			}
		}
	}

	public final static class SplitterTask<T extends BlockedRange<T>> extends PTask {
		private T range;
		private PForTask<T> task;

		public SplitterTask(PJob job, T range, PForTask<T> task, int number, int total) {
			super(job);
			this.range = range.createInstance(number, total);
			this.task = task;
		}

		/**
		 * This is the executor method of SplitIntoTasks. New for tasks will be
		 * created while a new range is available
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void execute() {
			List<T> tmpRange;

			int step = job.getThreadPool().getPoolSize();
			while (((tmpRange = range.splitBlockedRange(step))) != null) {
				for (T tr : tmpRange) {
					try {
						PForTask<T> taskObj = (PForTask<T>) task.clone();
						taskObj.setRange(tr);
						taskObj.setPJob(job);
						job.add(taskObj, PJob.SERIAL);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

}
