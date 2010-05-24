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

import junit.framework.Assert;

import org.junit.Test;

import com.sun.electric.tool.util.concurrent.PForJob_T.TestForTask;
import com.sun.electric.tool.util.concurrent.PReduceJob_T.PITask;
import com.sun.electric.tool.util.concurrent.exceptions.PoolExistsException;
import com.sun.electric.tool.util.concurrent.patterns.Parallel;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange1D;
import com.sun.electric.tool.util.concurrent.runtime.ThreadPool;

/**
 * @author fschmidt
 * 
 */
public class Parallel_T {

	@Test
	public void testParallelFor() throws InterruptedException, PoolExistsException {
		ThreadPool pool = ThreadPool.initialize();
		Parallel.For(new BlockedRange1D(0, 100, 4), new TestForTask());
		pool.shutdown();
	}
	
	@Test
	public void testParallelReduce() throws InterruptedException, PoolExistsException {
		ThreadPool pool = ThreadPool.initialize();
		int stepW = 1000000;
		double step = 1.0 / stepW;
		double pi = Parallel.Reduce(new BlockedRange1D(0, stepW, 4), new PITask(step));
		pool.shutdown();
		Assert.assertEquals(Math.PI, pi, 0.00001);
		System.out.println("pi = " + pi);
	}

}
