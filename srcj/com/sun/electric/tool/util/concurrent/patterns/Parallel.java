/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.electric.tool.util.concurrent.patterns;

import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.PForTask;
import com.sun.electric.tool.util.concurrent.patterns.PReduceJob.PReduceTask;

/**
 * 
 * @author fs239085
 */
public class Parallel {

	/**
	 * Parallel For Loop (1- and 2-dimensional)
	 * 
	 * @param range
	 * @param task
	 */
	public static void For(BlockedRange range, PForTask task) {
		(new PForJob(range, task)).execute();
	}

	public static <T> T Reduce(BlockedRange range, PReduceTask<T> task) {
		PReduceJob<T> pReduceJob = new PReduceJob<T>(range, task);
		pReduceJob.execute();

		return pReduceJob.getResult();
	}

}
