package com.sun.electric.tool.util.concurrent;

import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import com.sun.electric.tool.util.concurrent.patterns.PForJob;
import com.sun.electric.tool.util.UniqueIDGenerator;
import com.sun.electric.tool.util.concurrent.exceptions.PoolExistsException;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange1D;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange2D;
import com.sun.electric.tool.util.concurrent.patterns.PForTask;
import com.sun.electric.tool.util.concurrent.runtime.ThreadPool;

public class PForJob_T {

	@Test
	public void testParallelFor() throws PoolExistsException, InterruptedException {

		ThreadPool pool = ThreadPool.initialize();

		PForJob pforjob = new PForJob(new BlockedRange1D(0, 10, 2), TestForTask.class);
		pforjob.execute();

		pool.shutdown();

	}

	public static class TestForTask extends PForTask {

		private static UniqueIDGenerator idGen = new UniqueIDGenerator(0);
		private int id = idGen.getUniqueId();

		@Override
		public void execute() {

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
	private static int size = 2000;

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
		PForJob pforjob = new PForJob(new BlockedRange2D(0, size, 10, 0, size, 10), MatrixMultTask.class);
		pforjob.execute();

		long endPar = System.currentTimeMillis() - start;
		System.out.println(endPar);
		pool.shutdown();

		start = System.currentTimeMillis();
		matrixMultSer();
		long endSer = System.currentTimeMillis() - start;
		System.out.println(endSer);
		System.out.println((double)endSer / (double)endPar);

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

		@Override
		public void execute() {
			BlockedRange2D tmpRange = (BlockedRange2D) range;

			for (int i = tmpRange.getRow().getStart(); i < tmpRange.getRow().getEnd(); i++) {
				for (int j = tmpRange.getCol().getStart(); j < tmpRange.getCol().getEnd(); j++) {
					for (int k = 0; k < size; k++) {
						matCPar[i][j] += matA[i][k] * matB[k][j];
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
				BlockedRange2D tmpRange = range.splitBlockedRange();

				Assert.assertEquals(i, tmpRange.getRow().getStart());
				Assert.assertEquals(j, tmpRange.getCol().getStart());

				Assert.assertEquals(i + 2, tmpRange.getRow().getEnd());
				Assert.assertEquals(j + 2, tmpRange.getCol().getEnd());
			}
		}

		Assert.assertNull(range.splitBlockedRange());

	}
}