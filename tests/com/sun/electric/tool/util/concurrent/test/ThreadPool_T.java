/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ThreadPool_T.java
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
package com.sun.electric.tool.util.concurrent.test;

import org.junit.Ignore;
import org.junit.Test;

import com.sun.electric.tool.util.CollectionFactory;
import com.sun.electric.tool.util.IStructure;
import com.sun.electric.tool.util.concurrent.datastructures.LockFreeStack;
import com.sun.electric.tool.util.concurrent.datastructures.WorkStealingStructure;
import com.sun.electric.tool.util.concurrent.exceptions.PoolExistsException;
import com.sun.electric.tool.util.concurrent.patterns.PJob;
import com.sun.electric.tool.util.concurrent.patterns.PTask;
import com.sun.electric.tool.util.concurrent.runtime.ThreadPool;

public class ThreadPool_T {

	@Test
	public void testThreadPool() throws PoolExistsException, InterruptedException {
		ThreadPool pool = ThreadPool.initialize();
		pool.start();

		pool.shutdown();
	}

	@Test
	public void testThreadPoolWithTasks() throws PoolExistsException, InterruptedException {
		long start = System.currentTimeMillis();
		LockFreeStack<PTask> taskPool = CollectionFactory.createLockFreeStack();
		ThreadPool pool = ThreadPool.initialize(taskPool, 2);

		PJob job = new PJob();

		job.add(new TestTask(-2, job));

		job.execute();

		pool.shutdown();
		System.out.println("time: " + (System.currentTimeMillis() - start));
	}

	@Ignore
	@Test
	public void testThreadPoolWorkStealing() throws PoolExistsException, InterruptedException {
		long start = System.currentTimeMillis();
		IStructure<PTask> taskPool = new WorkStealingStructure<PTask>(0);
		ThreadPool pool = ThreadPool.initialize(taskPool);
		
		Thread.sleep(1000);

		PJob job = new PJob();
		job.add(new TestTask(-2, job));
		job.execute();

		pool.shutdown();
		System.out.println("time: " + (System.currentTimeMillis() - start));
	}

	public static class CreateParallelJobs extends PTask {

		/**
		 * @param job
		 */
		public CreateParallelJobs(PJob job) {
			super(job);
			// TODO Auto-generated constructor stub
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.sun.electric.tool.util.concurrent.patterns.PTask#execute()
		 */
		@Override
		public void execute() {

			PJob job = new PJob();
			job.add(new TestTask(-2, job));
			job.execute();

		}

	}

	private static class TestTask extends PTask {

		private int n = 0;

		public TestTask(int n, PJob job) {
			super(job);
			this.n = n;
		}

		@Override
		public void execute() {
			System.out.println(this.threadId + ": " + n);
			if (n + 1 <= 300)
				job.add(new TestTask(n + 1, job));

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
