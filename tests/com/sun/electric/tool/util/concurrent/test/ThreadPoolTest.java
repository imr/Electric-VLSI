/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ThreadPoolTest.java
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

import junit.framework.Assert;

import org.junit.Test;

import com.sun.electric.tool.Job;
import com.sun.electric.tool.util.concurrent.datastructures.IStructure;
import com.sun.electric.tool.util.concurrent.datastructures.LockFreeStack;
import com.sun.electric.tool.util.concurrent.datastructures.WorkStealingStructure;
import com.sun.electric.tool.util.concurrent.debug.StealTracker;
import com.sun.electric.tool.util.concurrent.exceptions.PoolExistsException;
import com.sun.electric.tool.util.concurrent.patterns.PJob;
import com.sun.electric.tool.util.concurrent.patterns.PTask;
import com.sun.electric.tool.util.concurrent.runtime.Scheduler.SchedulingStrategy;
import com.sun.electric.tool.util.concurrent.runtime.Scheduler.UnknownSchedulerException;
import com.sun.electric.tool.util.concurrent.runtime.taskParallel.ThreadPool;
import com.sun.electric.tool.util.concurrent.runtime.taskParallel.ThreadPool.ThreadPoolState;
import com.sun.electric.tool.util.concurrent.runtime.taskParallel.ThreadPool.ThreadPoolType;
import com.sun.electric.tool.util.concurrent.utils.ElapseTimer;
import com.sun.electric.util.CollectionFactory;
import com.sun.electric.util.TextUtils;

public class ThreadPoolTest {

	@Test
	public void testThreadPool() throws PoolExistsException, InterruptedException, UnknownSchedulerException {
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

		job.add(new TestTask(1700, job), PJob.SERIAL);

		job.execute();

		pool.shutdown();
		System.out.println("time: " + (System.currentTimeMillis() - start));
	}

	@Test
	public void testThreadPoolWorkStealing() throws PoolExistsException, InterruptedException {
		Job.setDebug(true);
		long start = System.currentTimeMillis();
		IStructure<PTask> taskPool = WorkStealingStructure.createForThreadPool(2);
		ThreadPool pool = ThreadPool.initialize(taskPool, 2);

		Thread.sleep(1000);

		PJob job = new PJob();
		job.add(new TestTask(1700, job), PJob.SERIAL);
		job.execute();

		pool.shutdown();
		System.out.println("time: " + (System.currentTimeMillis() - start));
		
		StealTracker.getInstance().printStatistics();
	}

	@Test
	public void testSleepAndWakeUp() throws PoolExistsException, InterruptedException, UnknownSchedulerException {

		ThreadPool pool = ThreadPool.initialize();
		pool.start();

		ElapseTimer timer = ElapseTimer.createInstance();
		timer.start();

		PJob job = new PJob();
		job.add(new TestTask(0, job), PJob.SERIAL);
		job.execute(false);

		Thread.sleep(20000);
		
		Assert.assertEquals(ThreadPoolState.Started, pool.getState());

		pool.sleep();
		
		Assert.assertEquals(ThreadPoolState.Sleeps, pool.getState());

		Thread.sleep(10000);
		
		Assert.assertEquals(ThreadPoolState.Sleeps, pool.getState());

		pool.weakUp();
		
		Assert.assertEquals(ThreadPoolState.Started, pool.getState());

		Thread.sleep(20000);
		
		Assert.assertEquals(ThreadPoolState.Started, pool.getState());

		timer.end();

		pool.shutdown();

		System.out.println("test took: " + timer.toString());

	}
	
	@Test
	public void testInitMethods() throws PoolExistsException, UnknownSchedulerException {
		IStructure<PTask> scheduler = CollectionFactory.createLockFreeQueue();
		
		ThreadPool.initialize();
		ThreadPool.killPool();
		
		ThreadPool.initialize(2);
		ThreadPool.killPool();
		
		ThreadPool.initialize(scheduler);
		ThreadPool.killPool();
		
		ThreadPool.initialize(scheduler, 2);
		ThreadPool.killPool();
		
		ThreadPool.initialize(SchedulingStrategy.queue, 2);
		ThreadPool.killPool();
		
		ThreadPool.initialize(scheduler, 2, ThreadPoolType.simplePool);
		ThreadPool.killPool();
		
		ThreadPool.initialize(SchedulingStrategy.queue, 2, ThreadPoolType.simplePool);
		ThreadPool.killPool();
		
		ThreadPool.initialize(scheduler, 2, ThreadPoolType.simplePool, scheduler, 2, ThreadPoolType.simplePool);
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
			job.add(new TestTask(-2, job), PJob.SERIAL);
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
			if (n + 1 <= 2000) job.add(new TestTask(n + 1, job), PJob.SERIAL);

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
