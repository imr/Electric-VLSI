package com.sun.electric.tool.util.concurrent;

import com.sun.electric.tool.util.CollectionFactory;
import com.sun.electric.tool.util.concurrent.datastructures.LockFreeStack;
import com.sun.electric.tool.util.concurrent.exceptions.PoolExistsException;
import com.sun.electric.tool.util.concurrent.patterns.PJob;
import com.sun.electric.tool.util.concurrent.patterns.PTask;
import com.sun.electric.tool.util.concurrent.runtime.ThreadPool;
import org.junit.Test;



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
