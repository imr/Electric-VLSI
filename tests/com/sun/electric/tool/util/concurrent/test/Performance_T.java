/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Performance_T.java
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

import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import com.sun.electric.tool.util.concurrent.Parallel;
import com.sun.electric.tool.util.concurrent.datastructures.WorkStealingStructure;
import com.sun.electric.tool.util.concurrent.exceptions.PoolExistsException;
import com.sun.electric.tool.util.concurrent.patterns.PTask;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange1D;
import com.sun.electric.tool.util.concurrent.patterns.PReduceJob.PReduceTask;
import com.sun.electric.tool.util.concurrent.runtime.ThreadPool;

/**
 * @author fs239085
 * 
 */
public class Performance_T {

	private int[] data;
	private static final int size = 500000000;

	@Test
	public void testSum() throws InterruptedException, PoolExistsException {

		System.out.println("init ...");

		Random rand = new Random(System.currentTimeMillis());
		data = new int[size];
		Integer sersum = 0;
		for (int i = 0; i < size; i++) {
			data[i] = rand.nextInt();
		}

		System.out.println("serial ...");

		long start = System.currentTimeMillis();

		for (int i = 0; i < size; i++) {
			sersum += data[i];
		}

		long ser = System.currentTimeMillis() - start;

		ThreadPool.initialize(new WorkStealingStructure<PTask>(1, PTask.class), 1);

		System.out.println("parallel ...");

		start = System.currentTimeMillis();

		Integer parsum = Parallel.Reduce(new BlockedRange1D(0, size, size
				/ ThreadPool.getThreadPool().getPoolSize()), new SumTask());

		long par = System.currentTimeMillis() - start;

		System.out.println("sersum " + sersum + " time: " + ser);
		System.out.println("parsum " + parsum + " time: " + par);
		System.out.println("speedup: " + ((double)ser/(double)par));

		ThreadPool.getThreadPool().shutdown();

		Assert.assertEquals(sersum, parsum);

	}

	public class SumTask extends PReduceTask<Integer> {

		private int localSum;

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.sun.electric.tool.util.concurrent.patterns.PReduceJob.PReduceTask
		 * #reduce(com.sun.electric.tool.util.concurrent.patterns.PReduceJob.
		 * PReduceTask)
		 */
		@Override
		public Integer reduce(PReduceTask other) {
			SumTask tmpOther = (SumTask) other;
			localSum += tmpOther.localSum;
			return localSum;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.sun.electric.tool.util.concurrent.patterns.PForJob.PForTask#execute
		 * (com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange)
		 */
		@Override
		public void execute(BlockedRange range) {
			BlockedRange1D tmpRange = (BlockedRange1D) range;
			for (int i = tmpRange.getStart(); i < tmpRange.getEnd(); i++) {
				localSum += data[i];
			}
		}

	}

}
