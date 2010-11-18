/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BlockedRange2D.java
 *
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.util.concurrent.utils;

import java.util.List;

/**
 * 
 * 2 dimensional range. Use this for 2 nested for loops.
 * 
 * @author Felix Schmidt
 * 
 */
public class BlockedRange2D implements BlockedRange<BlockedRange2D> {

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
	public List<BlockedRange2D> splitBlockedRange(int step) {

		if (currentRow != null && currentRow >= row.end()) {
			return null;
		}

		List<BlockedRange2D> result = ConcurrentCollectionFactory.createArrayList();
		for (int i = 0; i < step; i++) {
			if (currentRow == null) {
				currentRow = row.start();
			}

			if (currentCol == null) {
				currentCol = col.start();
			}

			if (currentCol >= col.end()) {
				currentCol = col.start();
				currentRow += row.step();

				if (currentRow >= row.end()) {
					return result;
				}
			}

			result.add(new BlockedRange2D(currentRow, Math.min(currentRow + row.step(), row.end()), row
					.step(), currentCol, Math.min(currentCol + col.step(), col.end()), col.step()));

			currentCol += col.step();

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
	public BlockedRange2D createInstance(int number, int total) {
		int size = this.row.end() - this.row.start();
		int split = size / total;
		BlockedRange2D result = new BlockedRange2D(number * split, (number + 1 == total) ? this.row.end()
				: (number + 1) * split, this.row.step(), this.col.start(), this.col.end(),
				this.col.step());
		return result;
	}
}

