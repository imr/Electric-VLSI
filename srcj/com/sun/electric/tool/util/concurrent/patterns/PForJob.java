/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PForJob.java
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
package com.sun.electric.tool.util.concurrent.patterns;

import java.util.List;

import com.sun.electric.tool.util.concurrent.runtime.taskParallel.ThreadPool;
import com.sun.electric.tool.util.concurrent.utils.ConcurrentCollectionFactory;

/**
 * 
 * Runtime for parallel for
 * 
 * @author Felix Schmidt
 * 
 */
public class PForJob extends PJob {

	/**
	 * Constructor for 1- and 2-dimensional parallel for loops
	 * 
	 * @param range
	 * @param task
	 */
	public PForJob(BlockedRange range, PForTask task) {
		super();
		this.add(new SplitIntoTasks(this, range, task), PJob.SERIAL);
	}

	public PForJob(BlockedRange range, PForTask task, ThreadPool pool) {
		super(pool);
		this.add(new SplitIntoTasks(this, range, task), PJob.SERIAL);
	}

	/**
	 * 
	 * Base task for parallel for
	 * 
	 */
	public abstract static class PForTask extends PTask implements Cloneable {

		protected BlockedRange range;

		public PForTask(PJob job, BlockedRange1D range) {
			super(job);
			this.range = range;
		}

		public PForTask() {
			super(null);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.sun.electric.tool.util.concurrent.patterns.PTask#execute()
		 */
		@Override
		public final void execute() {

		}

		/**
		 * Abstract method: this method should contain the body of the task. The
		 * thread pool will call this function
		 * 
		 * @param range
		 */
		public abstract void execute(BlockedRange range);

		/**
		 * set current job
		 * 
		 * @param job
		 */
		public void setPJob(PJob job) {
			this.job = job;
		}

	}

	/**
	 * 
	 * Task to create parallel for tasks (internal)
	 * 
	 */
	public final static class SplitIntoTasks extends PTask {

		private BlockedRange range;
		private PForTask task;

		public SplitIntoTasks(PJob job, BlockedRange range, PForTask task) {
			super(job);
			this.range = range;
			this.task = task;
		}

		/**
		 * This is the executor method of SplitIntoTasks. New for tasks will be
		 * created while a new range is available
		 */
		@Override
		public void execute() {
			int threadNum = job.getThreadPool().getPoolSize();
			for (int i = 0; i < threadNum; i++) {
				job.add(new SplitterTask(job, range, task, i, threadNum), i);
			}
		}
	}

	public final static class SplitterTask extends PTask {
		private BlockedRange range;
		private PForTask task;

		public SplitterTask(PJob job, BlockedRange range, PForTask task, int number, int total) {
			super(job);
			this.range = range.createInstance(number, total);
			this.task = task;
		}

		/**
		 * This is the executor method of SplitIntoTasks. New for tasks will be
		 * created while a new range is available
		 */
		@Override
		public void execute() {
			List<BlockedRange> tmpRange;

			int step = job.getThreadPool().getPoolSize();
			while (((tmpRange = range.splitBlockedRange(step))) != null) {
				for (BlockedRange tr : tmpRange) {
					try {
						PForTaskWrapper taskObj = new PForTaskWrapper(job, (PForTask) task.clone(), tr);
						job.add(taskObj, PJob.SERIAL);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Wrapper object for PForTask objects. This wrapper provides a PTask
	 * interface for PForTasks (internal)
	 */
	public final static class PForTaskWrapper extends PTask {

		private PForTask task;
		private BlockedRange range;

		/**
		 * @param job
		 */
		public PForTaskWrapper(PJob job, PForTask task, BlockedRange range) {
			super(job);
			this.task = task;
			this.range = range;
			task.job = job;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.sun.electric.tool.util.concurrent.patterns.PTask#execute()
		 */
		@Override
		public void execute() {
			task.execute(range);
		}

		public PForTask getTask() {
			return task;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.sun.electric.tool.util.concurrent.patterns.PTask#before()
		 */
		@Override
		public void before() {
			task.before();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.sun.electric.tool.util.concurrent.patterns.PTask#after()
		 */
		@Override
		public void after() {
			task.after();
		}

	}

	/**
	 * 
	 * This class provides a interface for parallel for parameters
	 * 
	 */
	public static class Range {
		private int start;
		private int end;
		private int step;

		public Range(int start, int end, int step) {
			super();
			this.start = start;
			this.end = end;
			this.step = step;
		}

		public int start() {
			return start;
		}

		public int end() {
			return end;
		}

		public int step() {
			return step;
		}

	}

	/**
	 * 
	 * Base interface for ranges
	 * 
	 */
	public interface BlockedRange {
		public List<BlockedRange> splitBlockedRange(int step);

		public BlockedRange createInstance(int number, int total);
	}

	/**
	 * 
	 * 1 dimensional block range. Use this range for 1 dimensional for loops
	 * 
	 */
	public static class BlockedRange1D implements BlockedRange {

		private Range range;
		private Integer current = null;

		public BlockedRange1D(int start, int end, int step) {
			this.range = new Range(start, end, step);
		}

		public int start() {
			return range.start;
		}

		public int end() {
			return range.end;
		}

		public int step() {
			return range.end;
		}

		/**
		 * split the current block range into smaller pieces according to step
		 * width
		 */
		public List<BlockedRange> splitBlockedRange(int step) {

			if (current != null && current >= range.end)
				return null;

			List<BlockedRange> result = ConcurrentCollectionFactory.createArrayList();
			for (int i = 0; i < step; i++) {
				if (current == null)
					current = range.start;
				if (current >= range.end)
					return result;

				result.add(new BlockedRange1D(current, Math.min(current + range.step, this.range.end),
						range.step));
				current += range.step;
			}
			return result;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange
		 * #createInstance(int, int)
		 */
		public BlockedRange createInstance(int number, int total) {
			int size = this.range.end - this.range.start;
			int split = size / total;
			BlockedRange1D result = new BlockedRange1D(number * split, (number + 1 == total) ? this.range.end
					: (number + 1) * split, this.range.step);
			return result;
		}
	}

	/**
	 * 
	 * 2 dimensional range. Use this for 2 nested for loops.
	 * 
	 */
	public static class BlockedRange2D implements BlockedRange {

		private Range col;
		private Range row;

		private Integer currentCol = null;
		private Integer currentRow = null;

		public BlockedRange2D(int startRow, int endRow, int stepRow, int startCol, int endCol, int stepCol) {
			this.col = new Range(startCol, endCol, stepCol);
			this.row = new Range(startRow, endRow, stepRow);
		}

		public Range col() {
			return col;
		}

		public Range row() {
			return row;
		}

		/**
		 * split current 2-dimensional blocked range into smaller pieces
		 * according to both step widths
		 */
		public List<BlockedRange> splitBlockedRange(int step) {

			if (currentRow != null && currentRow >= row.end) {
				return null;
			}

			List<BlockedRange> result = ConcurrentCollectionFactory.createArrayList();
			for (int i = 0; i < step; i++) {
				if (currentRow == null) {
					currentRow = row.start;
				}

				if (currentCol == null) {
					currentCol = col.start;
				}

				if (currentCol >= col.end) {
					currentCol = col.start;
					currentRow += row.step;

					if (currentRow >= row.end) {
						return result;
					}
				}

				result.add(new BlockedRange2D(currentRow, Math.min(currentRow + row.step, row.end), row.step,
						currentCol, Math.min(currentCol + col.step, col.end), col.step));

				currentCol += col.step;

			}
			return result;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange
		 * #createInstance(int, int)
		 */
		public BlockedRange createInstance(int number, int total) {
			int size = this.row.end - this.row.start;
			int split = size / total;
			BlockedRange2D result = new BlockedRange2D(number * split, (number + 1 == total) ? this.row.end
					: (number + 1) * split, this.row.step, this.col.start, this.col.end, this.col.step);
			return result;
		}
	}
}
