/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Barrier_T.java
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

import org.junit.Ignore;
import org.junit.Test;

import com.sun.electric.tool.util.concurrent.barriers.Barrier;
import com.sun.electric.tool.util.concurrent.barriers.SenseBarrier;
import com.sun.electric.tool.util.concurrent.barriers.SimpleBarrier;
import com.sun.electric.tool.util.concurrent.barriers.StaticTreeBarrier;
import com.sun.electric.tool.util.concurrent.barriers.TreeBarrier;
import com.sun.electric.tool.util.concurrent.runtime.ThreadID;

/**
 * @author fs239085
 * 
 */
public class Barrier_T {

	private static final int TEST_NUM = 10;

	@Ignore
	@Test
	public void testSimpleBarrier() throws InterruptedException {
		this.testBarrier(new SimpleBarrier(10), 10);
	}

	@Test
	public void testTreeBarrier() throws InterruptedException {
		this.testBarrier(new TreeBarrier(9, 2), 9);
	}

	@Ignore
	@Test
	public void testSenseBarrier() throws InterruptedException {
		this.testBarrier(new SenseBarrier(4), 4);
	}
	
	@Ignore
	@Test
	public void testStaticTreeBarrier() throws InterruptedException {

		this.testBarrier(new StaticTreeBarrier(16, 8), 16);

	}

	private void testBarrier(Barrier barrier, int n) throws InterruptedException {

		Thread[] threads = new Thread[n];
		for (int i = 0; i < n; i++) {
			threads[i] = new BarrierTest(barrier);
			threads[i].start();
		}

		for (int i = 0; i < n; i++) {
			threads[i].join();
		}

		Assert.assertTrue(true);

	}

	private class BarrierTest extends Thread {

		private Barrier barrier;

		public BarrierTest(Barrier barrier) {
			this.barrier = barrier;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			int id = ThreadID.get();
			System.out.println("Thread: " + id + " awaits barrier");
			barrier.await();
			System.out.println("Thread: " + id + " is free");
		}

	}

}
