/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PWhileJobTest.java
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

import org.junit.Test;

import com.sun.electric.tool.util.CollectionFactory;
import com.sun.electric.tool.util.IStructure;
import com.sun.electric.tool.util.concurrent.exceptions.PoolExistsException;
import com.sun.electric.tool.util.concurrent.patterns.PJob;
import com.sun.electric.tool.util.concurrent.patterns.PWhileJob;
import com.sun.electric.tool.util.concurrent.patterns.PWhileJob.PWhileTask;
import com.sun.electric.tool.util.concurrent.runtime.taskParallel.ThreadPool;

/**
 * @author Felix Schmidt
 * 
 */
public class PWhileJobTest {

	@Test
	public void testPWhileJob() throws PoolExistsException, InterruptedException {
		ThreadPool.initialize();

		IStructure<Integer> data = CollectionFactory.createLockFreeStack();

		for (int i = 0; i < 100; i++) {
			data.add(new Integer(i));
		}

		PJob whileJob = new PWhileJob<Integer>(data, new WhileTestTask());
		whileJob.execute();

		ThreadPool.getThreadPool().shutdown();
	}

	public static class WhileTestTask extends PWhileTask<Integer> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.sun.electric.tool.util.concurrent.patterns.PWhileJob.PWhileTask
		 * #execute(java.lang.Object)
		 */
		@Override
		public void execute(Integer item) {

			System.out.println(item);

		}

	}

}
