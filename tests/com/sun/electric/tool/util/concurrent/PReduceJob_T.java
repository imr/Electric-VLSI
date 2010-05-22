/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
