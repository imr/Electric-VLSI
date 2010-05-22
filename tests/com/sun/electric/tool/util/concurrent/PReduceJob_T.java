/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PReduceJob.java
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
package com.sun.electric.tool.util.concurrent;

import com.sun.electric.tool.util.concurrent.exceptions.PoolExistsException;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange1D;
import com.sun.electric.tool.util.concurrent.patterns.PReduceJob;
import com.sun.electric.tool.util.concurrent.patterns.PReduceTask;
import com.sun.electric.tool.util.concurrent.runtime.ThreadPool;
import org.junit.Ignore;
import org.junit.Test;



/**
 * 
 * @author fs239085
 */
public class PReduceJob_T {

    @Ignore
	@Test
	public void testPReduce() throws PoolExistsException, InterruptedException {
		ThreadPool pool = ThreadPool.initialize();
		PReduceJob<Double> pReduceJob = new PReduceJob<Double>(new BlockedRange1D(0, 10000, 100),
				ReduceTask.class);
		pReduceJob.execute();

		System.out.println("pi = " + pReduceJob.getResult());
		
		pool.shutdown();
	}

	public class ReduceTask extends PReduceTask<Double> {

		private double pi;

		public ReduceTask() {

		}

		@Override
		public Double reduce(PReduceTask<Double> other) {
			ReduceTask task = (ReduceTask) other;
			this.pi += task.pi;
			return this.pi;
		}

		@Override
		public void execute() {
			BlockedRange1D tmpRange = (BlockedRange1D) range;
			this.pi = 0.0;
			double step_width = 1.0 / tmpRange.getStep();

			for (int i = tmpRange.getStart(); i < tmpRange.getEnd(); i++) {
				double x = step_width * ((double) i - 0.5);
				pi += 4.0 / (1.0 + x * x);
			}
		}
	}
}
