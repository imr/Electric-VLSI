/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Parallel.java
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

import com.sun.electric.tool.util.concurrent.datastructures.IStructure;
import com.sun.electric.tool.util.concurrent.patterns.PForJob;
import com.sun.electric.tool.util.concurrent.patterns.PJob;
import com.sun.electric.tool.util.concurrent.patterns.PReduceJob;
import com.sun.electric.tool.util.concurrent.patterns.PWhileJob;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.PForTask;
import com.sun.electric.tool.util.concurrent.patterns.PReduceJob.PReduceTask;
import com.sun.electric.tool.util.concurrent.patterns.PWhileJob.PWhileTask;

/**
 * This class simplifies the interface for the parallel base patterns
 * 
 * @author Felix Schmidt
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
		PJob pWhileJob = new PWhileJob<T>(data, task);
		pWhileJob.execute();
	}

}
