/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.electric.tool.util.concurrent;

import com.sun.electric.tool.util.IStructure;
import com.sun.electric.tool.util.UniqueIDGenerator;
import com.sun.electric.tool.util.concurrent.patterns.PForJob;
import com.sun.electric.tool.util.concurrent.patterns.PReduceJob;
import com.sun.electric.tool.util.concurrent.patterns.PWhileJob;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange1D;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.PForTask;
import com.sun.electric.tool.util.concurrent.patterns.PReduceJob.PReduceTask;
import com.sun.electric.tool.util.concurrent.patterns.PWhileJob.PWhileTask;

/**
 * This class simplifies the interface for the parallel base patterns
 */
public class Parallel {

	/**
	 * Parallel For Loop (1- and 2-dimensional)
	 * 
	 * @param range
	 *            1- or 2-dimensional
	 * @param task
	 *            task object (body of for loop)
	 */
	public static void For(BlockedRange range, PForTask task) {
		(new PForJob(range, task)).execute();
	}

	/**
	 * Parallel reduce. Reduce is a parallel for loop with a result aggregation
	 * at the end of processing.
	 * 
	 * @param <T>
	 *            return type (implicit)
	 * @param range
	 *            1- or 2-dimensional
	 * @param task
	 *            body of reduce loop
	 * @return aggregated result
	 */
	public static <T> T Reduce(BlockedRange range, PReduceTask<T> task) {
		PReduceJob<T> pReduceJob = new PReduceJob<T>(range, task);
		pReduceJob.execute();

		return pReduceJob.getResult();
	}

	/**
	 * Parallel while loop: iterates while elements in the data structure
	 * 
	 * @param <T>
	 *            return type (implicit)
	 * @param data
	 *            data structure for work
	 * @param task
	 *            while loop body
	 */
	public static <T> void While(IStructure<T> data, PWhileTask<T> task) {
		PWhileJob<T> pWhileJob = new PWhileJob<T>(data, task);
		pWhileJob.execute();
	}

}
