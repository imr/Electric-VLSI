/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SchedulingTest.java
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

import org.junit.Test;

import com.sun.electric.tool.Job;
import com.sun.electric.tool.util.concurrent.Parallel;
import com.sun.electric.tool.util.concurrent.datastructures.IStructure;
import com.sun.electric.tool.util.concurrent.datastructures.WorkStealingStructure;
import com.sun.electric.tool.util.concurrent.debug.StealTracker;
import com.sun.electric.tool.util.concurrent.exceptions.PoolExistsException;
import com.sun.electric.tool.util.concurrent.patterns.PTask;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange2D;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.PForTask;
import com.sun.electric.tool.util.concurrent.runtime.Scheduler;
import com.sun.electric.tool.util.concurrent.runtime.taskParallel.ThreadPool;
import com.sun.electric.tool.util.concurrent.utils.ElapseTimer;
import com.sun.electric.util.CollectionFactory;
import com.sun.electric.util.TextUtils;

/**
 * @author Felix Schmidt
 * 
 */
public class SchedulingTest {

	private static int[][] matA;
	private static int[][] matB;
	private static Integer[][] matCPar;
	private static Integer[][] matCSer;
	private static int size = 700;
	private static final int numOfThreads = 8;

	@Test
	public void balancingTest() throws PoolExistsException, InterruptedException {
		Job.setDebug(true);

		Random rand = new Random(System.currentTimeMillis());

		matA = new int[size][size];
		matB = new int[size][size];
		matCPar = new Integer[size][size];
		matCSer = new Integer[size][size];

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				matA[i][j] = rand.nextInt(100);
				matB[i][j] = rand.nextInt(100);
				matCPar[i][j] = 0;
				matCSer[i][j] = 0;
			}
		}

		System.out.println("==============================================");
		System.out.println("==                  Queue                   ==");

		IStructure<PTask> structure = CollectionFactory.createLockFreeQueue();
		ElapseTimer tQueue = this.runMatrixMultiplication(structure);

		System.out.println("==============================================");
		System.out.println("==============================================");
		System.out.println("==                  Stack                   ==");

		structure = CollectionFactory.createLockFreeStack();
		ElapseTimer tStack = this.runMatrixMultiplication(structure);

		System.out.println("==============================================");
		System.out.println("==============================================");
		System.out.println("==                Stealing                  ==");

		structure = WorkStealingStructure.createForThreadPool(numOfThreads);
		ElapseTimer tSteal = this.runMatrixMultiplication(structure);
		System.out.println("steals: " + StealTracker.getInstance().getStealCounter());

		System.out.println("==============================================");

		System.out.println("Queue:    " + tQueue.toString());
		System.out.println("Stack:    " + tStack.toString());
		System.out.println("Stealing: " + tSteal.toString());
	}

	private ElapseTimer runMatrixMultiplication(IStructure<PTask> structure) throws PoolExistsException, InterruptedException {
		ElapseTimer timer = ElapseTimer.createInstance();
		ThreadPool pool = ThreadPool.initialize(structure, numOfThreads);
		timer.start();
		Parallel.For(new BlockedRange2D(0, size, 64, 0, size, 64), new MatrixMultTask(size));
		timer.end();
		pool.shutdown();
		return timer;
	}

	public static class MatrixMultTask extends PForTask {

		private int size;

		public MatrixMultTask(int n) {
			this.size = n;
		}

		@Override
		public void execute(BlockedRange range) {
			BlockedRange2D tmpRange = (BlockedRange2D) range;

			for (int i = tmpRange.row().start(); i < tmpRange.row().end(); i++) {
				for (int j = tmpRange.col().start(); j < tmpRange.col().end(); j++) {
					for (int k = 0; k < this.size; k++) {
						synchronized (matCPar[i][j]) {
							matCPar[i][j] += matA[i][k] * matB[k][j];
						}
					}
				}
			}

		}

	}

	@Test
	public void testGetSchedulers() {
		System.out.println(Scheduler.getAvailableScheduler());
	}

}
