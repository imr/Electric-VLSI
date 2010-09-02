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
package com.sun.electric.tool.util.concurrent.runtime.taskParallel;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import com.sun.electric.tool.util.concurrent.datastructures.IStructure;
import com.sun.electric.tool.util.concurrent.debug.Debug;
import com.sun.electric.tool.util.concurrent.debug.LoadBalancing;
import com.sun.electric.tool.util.concurrent.exceptions.PoolExistsException;
import com.sun.electric.tool.util.concurrent.patterns.PTask;
import com.sun.electric.tool.util.concurrent.runtime.Scheduler;
import com.sun.electric.tool.util.concurrent.runtime.ThreadID;
import com.sun.electric.tool.util.concurrent.runtime.Scheduler.SchedulingStrategy;
import com.sun.electric.tool.util.concurrent.runtime.Scheduler.UnknownSchedulerException;
import com.sun.electric.tool.util.concurrent.utils.ConcurrentCollectionFactory;
import com.sun.electric.tool.util.concurrent.utils.UniqueIDGenerator;

/**
 * 
 * Magic thread pool
 * 
 * @author Felix Schmidt
 * 
 */
public class ThreadPool {

	/**
	 * states of the thread pool. This is very similar to states of processes or
	 * tasks.
	 */
	public enum ThreadPoolState {
		New, Init, Started, Closed, Sleeps;
	}

	public enum ThreadPoolType {
		simplePool, synchronizedPool, userDefined
	}

	private IStructure<PTask> taskPool = null;
	private int numOfThreads = 0;
	private ArrayList<Worker> workers = null;
	private ThreadPoolState state;
	private UniqueIDGenerator generator;
	private ThreadPoolType type;

	/**
	 * prevent from creating thread pools via constructor
	 * 
	 * @param taskPool
	 * @param numOfThreads
	 */
	private ThreadPool(IStructure<PTask> taskPool, int numOfThreads, ThreadPoolType type) {
		state = ThreadPoolState.New;
		this.taskPool = taskPool;
		this.numOfThreads = numOfThreads;
		this.generator = new UniqueIDGenerator(0);
		this.type = type;

		// reset thread id
		ThreadID.reset();

		workers = ConcurrentCollectionFactory.createArrayList();

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

		taskPool.shutdown();

		if (workers.size() > 0)
			workers.get(0).strategy.trigger();

		this.join();
		state = ThreadPoolState.Closed;

		// print statistics in debug mode
		if (Debug.isDebug()) {
			LoadBalancing.getInstance().printStatistics();
			LoadBalancing.getInstance().reset();
		}
	}

	/**
	 * wait for termination
	 * 
	 * @throws InterruptedException
	 */
	public void join() throws InterruptedException {
		for (Worker worker : workers) {
			worker.join();
		}
	}

	/**
	 * Set thread pool to state sleep. Constraint: current State = started
	 */
	public void sleep() {
		if (state == ThreadPoolState.Started) {
			for (Worker worker : workers) {
				worker.sleep();
			}
			this.state = ThreadPoolState.Sleeps;
		}
	}

	/**
	 * Wake up the thread pool. Constraint: current State = sleeps
	 */
	public void weakUp() {
		if (this.state == ThreadPoolState.Sleeps) {
			for (Worker worker : workers) {
				worker.weakUp();
			}
			this.state = ThreadPoolState.Started;
		}
	}

	/**
	 * trigger workers (used for the synchronization)
	 */
	public void trigger() {
		if (workers.size() > 0)
			workers.get(0).strategy.trigger();
	}

	/**
	 * add a task to the pool
	 * 
	 * @param item
	 */
	public void add(PTask item) {
		taskPool.add(item);
	}

	/**
	 * add a task to the pool
	 * 
	 * @param item
	 */
	public void add(PTask item, int threadId) {
		taskPool.add(item, threadId);
	}

	/**
	 * 
	 * @return the current thread pool size (#threads)
	 */
	public int getPoolSize() {
		return this.numOfThreads;
	}

	/**
	 * Worker class. This class uses a worker strategy to determine how to
	 * process tasks in the pool.
	 */
	protected class Worker extends Thread {

		private ThreadPool pool;
		private PoolWorkerStrategy strategy;

		public Worker(ThreadPool pool) {
			this.pool = pool;
			ThreadID.set(generator.getUniqueId());
			strategy = PoolWorkerStrategyFactory.createStrategy(taskPool, type);
			if (Debug.isDebug()) {
				LoadBalancing.getInstance().registerWorker(strategy);
			}
		}

		@Override
		public void run() {

			pool.taskPool.registerThread();

			// execute worker strategy (all process of a worker is defined in a
			// strategy)
			strategy.execute();
		}

		/**
		 * shutdown the current worker
		 */
		public void shutdown() {
			strategy.shutdown();
			this.interrupt();
		}

		/**
		 * Danger: Could cause deadlocks
		 */
		public void sleep() {
			strategy.pleaseWait();
		}

		/**
		 * Danger: Could cause deadlocks
		 */
		public void weakUp() {
			strategy.pleaseWakeUp();
			synchronized (strategy) {
				strategy.notifyAll();
			}
		}

	}

	/**
	 * Factory class for worker strategy
	 */
	public static class PoolWorkerStrategyFactory {
		private static Semaphore trigger = new Semaphore(0);

		public static PoolWorkerStrategy userDefinedStrategy = null;

		public static PoolWorkerStrategy createStrategy(IStructure<PTask> taskPool, ThreadPoolType type) {
			if (type == ThreadPoolType.synchronizedPool)
				return new SynchronizedWorker(taskPool, trigger);
			else if (type == ThreadPoolType.simplePool) {
				return new SimpleWorker(taskPool);
			} else {
				if (userDefinedStrategy == null) {
					return createStrategy(taskPool, ThreadPoolType.simplePool);
				}

				userDefinedStrategy.setTaskPool(taskPool);

				return userDefinedStrategy;
			}
		}
	}

	private static ThreadPool instance = null;

	/**
	 * initialize thread pool, default initialization
	 * 
	 * @return initialized thread pool
	 * @throws PoolExistsException
	 * @throws UnknownSchedulerException 
	 */
	public static ThreadPool initialize() throws PoolExistsException, UnknownSchedulerException {
		IStructure<PTask> scheduler = Scheduler.createScheduler(SchedulingStrategy.workStealing, getNumOfThreads());
		return ThreadPool.initialize(scheduler);
	}

	/**
	 * initialize thread pool with number of threads
	 * 
	 * @param num
	 *            of threads
	 * @return initialized thread pool
	 * @throws PoolExistsException
	 */
	public static ThreadPool initialize(int num) throws PoolExistsException {
		IStructure<PTask> taskPool = ConcurrentCollectionFactory.createLockFreeQueue();
		return ThreadPool.initialize(taskPool, num);
	}

	/**
	 * initialize thread pool with specific task pool
	 * 
	 * @param taskPool
	 *            to be used
	 * @return initialized thread pool
	 * @throws PoolExistsException
	 */
	public static ThreadPool initialize(IStructure<PTask> taskPool) throws PoolExistsException {
		return ThreadPool.initialize(taskPool);
	}

	public static synchronized ThreadPool initialize(SchedulingStrategy taskPool, int numOfThreads)
			throws UnknownSchedulerException, PoolExistsException {
		IStructure<PTask> scheduler = Scheduler.createScheduler(taskPool, numOfThreads);
		return ThreadPool.initialize(scheduler, numOfThreads);
	}

	public static synchronized ThreadPool initialize(SchedulingStrategy taskPool, int numOfThreads, ThreadPoolType type)
			throws UnknownSchedulerException, PoolExistsException {
		IStructure<PTask> scheduler = Scheduler.createScheduler(taskPool, numOfThreads);
		return ThreadPool.initialize(scheduler, numOfThreads, type);
	}

	/**
	 * initialize thread pool with specific task pool and number of threads
	 * 
	 * @param taskPool
	 *            to be used
	 * @param numOfThreads
	 * @return initialized thread pool
	 * @throws PoolExistsException
	 */
	public static synchronized ThreadPool initialize(IStructure<PTask> taskPool, int numOfThreads)
			throws PoolExistsException {
		return ThreadPool.initialize(taskPool, numOfThreads);
	}

	/**
	 * initialize thread pool with specific task pool and number of threads
	 * 
	 * @param taskPool
	 *            to be used
	 * @param numOfThreads
	 * @return initialized thread pool
	 * @throws PoolExistsException
	 */
	public static synchronized ThreadPool initialize(IStructure<PTask> taskPool, int numOfThreads, ThreadPoolType type)
			throws PoolExistsException {
		if (ThreadPool.instance == null || instance.state != ThreadPoolState.Started) {
			instance = new ThreadPool(taskPool, numOfThreads, type);
			instance.start();
		} else {
			return instance;
		}

		return instance;
	}

	/**
	 * create a double thread pool (two thread pool side by side)
	 * 
	 * @param taskPool1
	 * @param numOfThreads1
	 * @param type1
	 * @param taskPool2
	 * @param numOfThreads2
	 * @param type2
	 * @param debug
	 * @return
	 */
	public static synchronized ThreadPool[] initialize(IStructure<PTask> taskPool1, int numOfThreads1,
			ThreadPoolType type1, IStructure<PTask> taskPool2, int numOfThreads2, ThreadPoolType type2) {

		ThreadPool[] result = new ThreadPool[2];

		result[0] = new ThreadPool(taskPool1, numOfThreads1, type1);
		result[1] = new ThreadPool(taskPool2, numOfThreads2, type2);

		result[0].start();
		result[1].start();

		return result;

	}

	/**
	 * hard shutdown of thread pool
	 */
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

	/**
	 * returns the current thread pool
	 * 
	 * @return thread pool
	 */
	public static ThreadPool getThreadPool() {
		return instance;
	}

	/**
	 * Get current state of the thread pool
	 * 
	 * @return
	 */
	public ThreadPoolState getState() {
		return state;
	}
}
