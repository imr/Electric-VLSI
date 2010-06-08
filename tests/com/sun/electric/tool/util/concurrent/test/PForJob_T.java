/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PForJob_T.java
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

import com.sun.electric.tool.util.IStructure;
import com.sun.electric.tool.util.UniqueIDGenerator;
import com.sun.electric.tool.util.concurrent.datastructures.WorkStealingStructure;
import com.sun.electric.tool.util.concurrent.exceptions.PoolExistsException;
import com.sun.electric.tool.util.concurrent.patterns.PForJob;
import com.sun.electric.tool.util.concurrent.patterns.PTask;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange1D;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange2D;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.PForTask;
import com.sun.electric.tool.util.concurrent.runtime.ThreadPool;

public class PForJob_T {

	@Test
	public void testParallelFor() throws PoolExistsException, InterruptedException {

		ThreadPool pool = ThreadPool.initialize();

		PForJob pforjob = new PForJob(new BlockedRange1D(0, 10, 2), new TestForTask());
		pforjob.execute();

		pool.shutdown();

	}

	public static class TestForTask extends PForTask {

		private static UniqueIDGenerator idGen = new UniqueIDGenerator(0);
		private int id = idGen.getUniqueId();

		@Override
		public void execute(BlockedRange range) {

			BlockedRange1D tmpRange = (BlockedRange1D) range;

			for (int i = tmpRange.getStart(); i < tmpRange.getEnd(); i++) {
				System.out.println("task: " + id + ", " + i);
			}

		}

	}

	private static int[][] matA;
	private static int[][] matB;
	private static Integer[][] matCPar;
	private static Integer[][] matCSer;
	private static int size = 20;

	@Test
	public void testMatrixMultiply() throws PoolExistsException, InterruptedException {
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

		ThreadPool pool = ThreadPool.initialize();

		long start = System.currentTimeMillis();
		PForJob pforjob = new PForJob(new BlockedRange2D(0, size, 10, 0, size, 10), new MatrixMultTask(size));
		pforjob.execute();

		long endPar = System.currentTimeMillis() - start;
		System.out.println(endPar);
		pool.shutdown();

		start = System.currentTimeMillis();
		matrixMultSer();
		long endSer = System.currentTimeMillis() - start;
		System.out.println(endSer);
		System.out.println((double) endSer / (double) endPar);

		int nullChecker = 0;
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				Assert.assertEquals(matCPar[i][j], matCSer[i][j]);
				if (matCPar[i][j] == 0)
					nullChecker++;
			}
		}

		Assert.assertTrue(nullChecker < size * size);
	}

	@Test
	public void testMatrixMultiplyPerformance() throws PoolExistsException, InterruptedException {
		Random rand = new Random(System.currentTimeMillis());

		int sizePerf = 600;

		matA = new int[sizePerf][sizePerf];
		matB = new int[sizePerf][sizePerf];
		matCPar = new Integer[sizePerf][sizePerf];
		matCSer = new Integer[sizePerf][sizePerf];

		for (int i = 0; i < sizePerf; i++) {
			for (int j = 0; j < sizePerf; j++) {
				matA[i][j] = rand.nextInt(100);
				matB[i][j] = rand.nextInt(100);
				matCPar[i][j] = 0;
				matCSer[i][j] = 0;
			}
		}

		ThreadPool pool = ThreadPool.initialize(8);

		long start = System.currentTimeMillis();
		PForJob pforjob = new PForJob(new BlockedRange2D(0, sizePerf, 64, 0, sizePerf, 64),
				new MatrixMultTask(sizePerf));
		pforjob.execute();

		long endPar = System.currentTimeMillis() - start;
		System.out.println(endPar);
		pool.shutdown();

		pool = ThreadPool.initialize(1);

		start = System.currentTimeMillis();

		pforjob = new PForJob(new BlockedRange2D(0, sizePerf, 64, 0, sizePerf, 64), new MatrixMultTask(
				sizePerf));
		pforjob.execute();

		long endSer = System.currentTimeMillis() - start;

		pool.shutdown();

		System.out.println(endSer);
		System.out.println((double) endSer / (double) endPar);
	}
	
	@Test
	public void testMatrixMultiplyPerformanceWorkStealing() throws PoolExistsException, InterruptedException {
		Random rand = new Random(System.currentTimeMillis());

		int sizePerf = 600;

		matA = new int[sizePerf][sizePerf];
		matB = new int[sizePerf][sizePerf];
		matCPar = new Integer[sizePerf][sizePerf];
		matCSer = new Integer[sizePerf][sizePerf];

		for (int i = 0; i < sizePerf; i++) {
			for (int j = 0; j < sizePerf; j++) {
				matA[i][j] = rand.nextInt(100);
				matB[i][j] = rand.nextInt(100);
				matCPar[i][j] = 0;
				matCSer[i][j] = 0;
			}
		}

		IStructure<PTask> taskPool = new WorkStealingStructure<PTask>(8, PTask.class);
		ThreadPool pool = ThreadPool.initialize(taskPool, 8);

		long start = System.currentTimeMillis();
		PForJob pforjob = new PForJob(new BlockedRange2D(0, sizePerf, 10, 0, sizePerf, 10),
				new MatrixMultTask(sizePerf));
		pforjob.execute();

		long endPar = System.currentTimeMillis() - start;
		System.out.println(endPar);
		pool.shutdown();

		taskPool = new WorkStealingStructure<PTask>(1, PTask.class);
		pool = ThreadPool.initialize(taskPool, 1);

		start = System.currentTimeMillis();

		pforjob = new PForJob(new BlockedRange2D(0, sizePerf, 64, 0, sizePerf, 64), new MatrixMultTask(
				sizePerf));
		pforjob.execute();

		long endSer = System.currentTimeMillis() - start;

		pool.shutdown();

		System.out.println(endSer);
		System.out.println((double) endSer / (double) endPar);
	}

	private void matrixMultSer() {
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				for (int k = 0; k < size; k++) {
					matCSer[i][j] += matA[i][k] * matB[k][j];
				}
			}
		}
	}

	public static class MatrixMultTask extends PForTask {

		private int size;

		public MatrixMultTask(int n) {
			this.size = n;
		}

		@Override
		public void execute(BlockedRange range) {
			BlockedRange2D tmpRange = (BlockedRange2D) range;

			for (int i = tmpRange.getRow().getStart(); i < tmpRange.getRow().getEnd(); i++) {
				for (int j = tmpRange.getCol().getStart(); j < tmpRange.getCol().getEnd(); j++) {
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
	public void testBlockRange2D() {

		BlockedRange2D range = new BlockedRange2D(0, 10, 2, 0, 10, 2);

		for (int i = 0; i < 10; i += 2) {
			for (int j = 0; j < 10; j += 2) {
				BlockedRange2D tmpRange = (BlockedRange2D) range.splitBlockedRange(1).get(0);

				Assert.assertEquals(i, tmpRange.getRow().getStart());
				Assert.assertEquals(j, tmpRange.getCol().getStart());

				Assert.assertEquals(i + 2, tmpRange.getRow().getEnd());
				Assert.assertEquals(j + 2, tmpRange.getCol().getEnd());
			}
		}

		Assert.assertTrue(range.splitBlockedRange(1).size() == 0);

	}
}
