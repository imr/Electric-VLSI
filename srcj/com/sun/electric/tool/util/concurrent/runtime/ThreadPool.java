/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ThreadPool.java
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
import com.sun.electric.tool.util.UniqueIDGenerator;
import com.sun.electric.tool.util.concurrent.exceptions.PoolExistsException;
import com.sun.electric.tool.util.concurrent.patterns.PTask;
import java.util.ArrayList;

/**
 * 
 * Magic thread pool
 *
 */
public class ThreadPool {

    /**
     * states of the thread pool. This is very similar to states of processes or tasks.
     */
	private enum ThreadPoolState {
		New, Init, Started, Closed;
	}

	private IStructure<PTask> taskPool = null;
	private int numOfThreads = 0;
	private ArrayList<Worker> workers = null;
	private ThreadPoolState state;
	private UniqueIDGenerator generator;

    /**
     * prevent from creating thread pools via constructor
     * @param taskPool
     * @param numOfThreads
     */
	private ThreadPool(IStructure<PTask> taskPool, int numOfThreads) {
		state = ThreadPoolState.New;
		this.taskPool = taskPool;
		this.numOfThreads = numOfThreads;
		this.generator = new UniqueIDGenerator(0);

		workers = CollectionFactory.createArrayList();

		for (int i = 0; i < numOfThreads; i++) {
			workers.add(new Worker(this));
		}
		state = ThreadPoolState.Init;
	}

    /**
     * start the thread pool
     */
	public void start() {
		if (state == ThreadPoolState.Init) {
			for (Worker worker : workers) {
				worker.start();
			}
		}
		state = ThreadPoolState.Started;
	}

    /**
     * shutdown the thread pool
     */
	public void shutdown() throws InterruptedException {
		for (Worker worker : workers) {
			worker.shutdown();
		}

		this.join();
		state = ThreadPoolState.Closed;
	}

    /**
     * wait for termination
     * @throws InterruptedException
     */
	public void join() throws InterruptedException {
		for (Worker worker : workers) {
			worker.join();
		}
	}

    /**
     * add a task to the pool
     * @param item
     */
	public void add(PTask item) {
		taskPool.add(item);
	}

    public int getPoolSize() {
        return this.numOfThreads;
    }

	/**** worker ****/
    /**
     * Worker class. This class uses a worker strategy to determine how to process
     * tasks in the pool.
     */
	protected class Worker extends Thread {

		@SuppressWarnings("unused")
		private ThreadPool pool;
		private int threadId;
		private PoolWorkerStrategy strategy;

		public Worker(ThreadPool pool) {
			this.pool = pool;
			threadId = generator.getUniqueId();
			strategy = PoolWorkerStrategyFactory.createStrategy(threadId, taskPool);
		}

		@Override
		public void run() {
			strategy.execute();
		}

		public void shutdown() {
			strategy.shutdown();
		}

	}

    /**
     * Factory class for worker strategy
     */
	private static class PoolWorkerStrategyFactory {
		public static PoolWorkerStrategy createStrategy(int threadId, IStructure<PTask> taskPool) {
			return new SimpleWorker(threadId, taskPool);
			//return new WorkStealingWorker(threadId, (IWorkStealingStructure<PTask>) taskPool);
		}
	}

	/**** static factory methods ****/
	private static ThreadPool instance = null;

	public static ThreadPool initialize() throws PoolExistsException {
		return ThreadPool.initialize(ThreadPool.getNumOfThreads());
	}

	public static ThreadPool initialize(int num) throws PoolExistsException {
		IStructure<PTask> taskPool = CollectionFactory.createLockFreeStack();
		return ThreadPool.initialize(taskPool, num);
	}

	public static ThreadPool initialize(IStructure<PTask> taskPool) throws PoolExistsException {
		return ThreadPool.initialize(taskPool, ThreadPool.getNumOfThreads());
	}

	public static synchronized ThreadPool initialize(IStructure<PTask> taskPool, int numOfThreads)
			throws PoolExistsException {
		if (ThreadPool.instance == null || instance.state != ThreadPoolState.Started) {
			instance = new ThreadPool(taskPool, numOfThreads);
		} else {
			return instance;
		}

		return instance;
	}

	public static synchronized void killPool() {
		try {
			ThreadPool.instance.shutdown();
		} catch (InterruptedException e) {
		}
		ThreadPool.instance = null;
	}

	private static int getNumOfThreads() {
		return Runtime.getRuntime().availableProcessors();
	}

	public static ThreadPool getThreadPool() {
		return instance;
	}
}
