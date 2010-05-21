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

/**
 * 
 * Runtime for parallel for
 * 
 */
public class PForJob extends PJob {

    /**
     * Constructor for 1- and 2-dimensional parallel for loops
     * @param range
     * @param task
     */
	public PForJob(BlockedRange range, Class<? extends PForTask> task) {
		super();
		this.add(new SplitIntoTasks(this, range, task));
	}

	/**
	 * 
	 * Task to create parallel for tasks
	 * 
	 */
	public static class SplitIntoTasks extends PTask {

		private BlockedRange range;
		private Class<? extends PForTask> task;

		public SplitIntoTasks(PJob job, BlockedRange range, Class<? extends PForTask> task) {
			super(job);
			this.range = range;
			this.task = task;
		}

        /**
         * This is the executor method of SplitIntoTasks. New for tasks will be created
         * while a new range is available
         */
		@Override
		public void execute() {
			BlockedRange tmpRange;
			while ((tmpRange = range.splitBlockedRange()) != null) {
				try {
					PForTask taskObj = task.newInstance();
					taskObj.setBlockedRange(tmpRange);
					taskObj.setPJob(job);
					job.add(taskObj);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
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

		public int getStart() {
			return start;
		}

		public int getEnd() {
			return end;
		}

		public int getStep() {
			return step;
		}

	}

	/**
	 * 
	 * Base interface for ranges
	 * 
	 */
	public interface BlockedRange {
		public BlockedRange splitBlockedRange();
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

		public int getStart() {
			return range.start;
		}

		public int getEnd() {
			return range.end;
		}

        public int getStep() {
			return range.end;
		}

        /**
         * split the current block range into smaller pieces according to step width
         */
        @Override
		public BlockedRange1D splitBlockedRange() {

			if (current == null)
				current = range.start;
			if (current >= range.end)
				return null;

			BlockedRange1D result = new BlockedRange1D(current, Math
					.min(current + range.step, this.range.end), range.step);
			current += range.step;
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

		public Range getCol() {
			return col;
		}

		public Range getRow() {
			return row;
		}

        /**
         * split current 2-dimensional blocked range into smaller pieces according to both step widths
         */
        @Override
		public BlockedRange2D splitBlockedRange() {

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
					return null;
				}
			}

			BlockedRange2D result = new BlockedRange2D(currentRow, Math.min(currentRow + row.step, row.end),
					row.step, currentCol, Math.min(currentCol + col.step, col.end), col.step);

			currentCol += col.step;

			return result;
		}
	}
}
