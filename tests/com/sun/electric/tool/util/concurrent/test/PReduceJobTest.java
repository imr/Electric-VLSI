/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PReduceJobTest.java
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

import com.sun.electric.tool.util.concurrent.exceptions.PoolExistsException;
import com.sun.electric.tool.util.concurrent.patterns.PReduceJob;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange1D;
import com.sun.electric.tool.util.concurrent.patterns.PReduceJob.PReduceTask;
import com.sun.electric.tool.util.concurrent.runtime.taskParallel.ThreadPool;

/**
 * 
 * @author Felix Schmidt
 */
public class PReduceJobTest {

	@Test
	public void testPReduce() throws PoolExistsException, InterruptedException, CloneNotSupportedException {
		ThreadPool pool = ThreadPool.initialize();
		int stepW = 1000000;
		double step = 1.0 / stepW;
		PReduceJob<Double> pReduceJob = new PReduceJob<Double>(new BlockedRange1D(0, stepW, 100), new PITask(
				step));
		pReduceJob.execute();

		System.out.println("calc pi = " + pReduceJob.getResult());
		System.out.println("math pi = " + Math.PI);

		pool.shutdown();

		Assert.assertEquals(Math.PI, pReduceJob.getResult(), 0.0001);
	}

	@Test
	public void testPerformancePReduce() throws PoolExistsException, InterruptedException {

		int stepW = 100000000;
		double step = 1.0 / stepW;

		ThreadPool pool = ThreadPool.initialize(1);

		long start = System.currentTimeMillis();
		PReduceJob<Double> pReduceJobSer = new PReduceJob<Double>(new BlockedRange1D(0, stepW, 128),
				new PITask(step));

		pReduceJobSer.execute();

		long endSer = System.currentTimeMillis() - start;

		pool.shutdown();
		pool = ThreadPool.initialize(8);

		System.out.println(ThreadPool.getThreadPool().getPoolSize());

		start = System.currentTimeMillis();
		PReduceJob<Double> pReduceJobPar = new PReduceJob<Double>(new BlockedRange1D(0, stepW, 128),
				new PITask(step));

		pReduceJobPar.execute();
		long endPar = System.currentTimeMillis() - start;

		pool.shutdown();

		Assert.assertEquals(pReduceJobPar.getResult(), pReduceJobSer.getResult(), 0.000000001);
		System.out.println("Ser:     " + endSer);
		System.out.println("Par:     " + endPar);
		System.out.println("Speedup: " + (double) endSer / (double) endPar);

	}

	public static class PITask extends PReduceTask<Double> {

		private double pi;
		private double step;

		public PITask(double step) {
			this.step = step;
		}

		@Override
		public synchronized Double reduce(PReduceTask<Double> other) {
			PITask task = (PITask) other;

			if (!this.equals(task)) {
				this.pi += task.pi;
			}

			return this.pi * step;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.sun.electric.tool.util.concurrent.patterns.PForTask#execute(com
		 * .sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange)
		 */
		@Override
		public void execute(BlockedRange range) {
			BlockedRange1D tmpRange = (BlockedRange1D) range;
			this.pi = 0.0;

			for (int i = tmpRange.start(); i < tmpRange.end(); i++) {
				double x = step * ((double) i - 0.5);
				this.pi += 4.0 / (1.0 + x * x);
			}
		}
	}
}
