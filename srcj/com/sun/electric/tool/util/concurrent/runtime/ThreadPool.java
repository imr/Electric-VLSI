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

import java.util.ArrayList;

import com.sun.electric.database.Environment;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.util.CollectionFactory;
import com.sun.electric.tool.util.IStructure;
import com.sun.electric.tool.util.UniqueIDGenerator;
import com.sun.electric.tool.util.concurrent.debug.LoadBalancing;
import com.sun.electric.tool.util.concurrent.exceptions.PoolExistsException;
import com.sun.electric.tool.util.concurrent.patterns.PTask;

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
	private enum ThreadPoolState {
		New, Init, Started, Closed;
	}

	private IStructure<PTask> taskPool = null;
	private int numOfThreads = 0;
	private ArrayList<Worker> workers = null;
	private ThreadPoolState state;
	private UniqueIDGenerator generator;
	private UserInterface userInterface;
	private boolean debug;

	/**
	 * prevent from creating thread pools via constructor
	 * 
	 * @param taskPool
	 * @param numOfThreads
	 */
	private ThreadPool(IStructure<PTask> taskPool, int numOfThreads, boolean debug) {
		state = ThreadPoolState.New;
		this.taskPool = taskPool;
		this.numOfThreads = numOfThreads;
		this.generator = new UniqueIDGenerator(0);
		this.debug = debug;

		// reset thread id
		ThreadID.reset();

		workers = CollectionFactory.createArrayList();

		setUserInterface(Job.getUserInterface());

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

		// print statistics in debug mode
		if (this.debug) {
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
	 * add a task to the pool
	 * 
	 * @param item
	 */
	public void add(PTask item) {
		taskPool.add(item);
	}

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
			strategy = PoolWorkerStrategyFactory.createStrategy(taskPool);
			if (pool.debug) {
				LoadBalancing.getInstance().registerWorker(strategy);
			}
		}

		@Override
		public void run() {

			pool.taskPool.registerThread();

			try {
				Job.setUserInterface(pool.getUserInterface());
				Environment.setThreadEnvironment(Job.getUserInterface().getDatabase()
						.getEnvironment());
			} catch (Exception ex) {

			}

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
		public static PoolWorkerStrategy createStrategy(IStructure<PTask> taskPool) {
			return new SimpleWorker(taskPool);
		}
	}

	private static ThreadPool instance = null;

	/**
	 * initialize thread pool, default initialization
	 * 
	 * @return initialized thread pool
	 * @throws PoolExistsException
	 */
	public static ThreadPool initialize() throws PoolExistsException {
		return ThreadPool.initialize(false);
	}

	/**
	 * initialize thread pool, default initialization
	 * 
	 * @return initialized thread pool
	 * @throws PoolExistsException
	 */
	public static ThreadPool initialize(boolean debug) throws PoolExistsException {
		return ThreadPool.initialize(ThreadPool.getNumOfThreads(), debug);
	}

	/**
	 * initialize thread pool with number of threads
	 * 
	 * @param num
	 *            of threads
	 * @return initialized thread pool
	 * @throws PoolExistsException
	 */
	public static ThreadPool initialize(int num, boolean debug) throws PoolExistsException {
		IStructure<PTask> taskPool = CollectionFactory.createLockFreeQueue();
		return ThreadPool.initialize(taskPool, num, debug);
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
		IStructure<PTask> taskPool = CollectionFactory.createLockFreeQueue();
		return ThreadPool.initialize(taskPool, num, false);
	}

	/**
	 * initialize thread pool with specific task pool
	 * 
	 * @param taskPool
	 *            to be used
	 * @return initialized thread pool
	 * @throws PoolExistsException
	 */
	public static ThreadPool initialize(IStructure<PTask> taskPool, boolean debug)
			throws PoolExistsException {
		return ThreadPool.initialize(taskPool, ThreadPool.getNumOfThreads(), debug);
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
		return ThreadPool.initialize(taskPool, false);
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
		return ThreadPool.initialize(taskPool, numOfThreads, false);
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
	public static synchronized ThreadPool initialize(IStructure<PTask> taskPool, int numOfThreads,
			boolean debug) throws PoolExistsException {
		if (ThreadPool.instance == null || instance.state != ThreadPoolState.Started) {
			instance = new ThreadPool(taskPool, numOfThreads, debug);
			instance.start();
		} else {
			return instance;
		}

		return instance;
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
	 * set the user interface for the thread pool
	 * 
	 * @param userInterface
	 */
	public void setUserInterface(UserInterface userInterface) {
		this.userInterface = userInterface;
	}

	/**
	 * get the user interface of the thread pool
	 * 
	 * @return
	 */
	public UserInterface getUserInterface() {
		return userInterface;
	}
}
