/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BlockedRange1D.java
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
 * 1 dimensional block range. Use this range for 1 dimensional for loops
 * 
 * @author Felix Schmidt
 * 
 */
public class BlockedRange1D extends BlockedRange<BlockedRange1D> {

	private Range range;
	private Integer current = null;

	public BlockedRange1D(int start, int end, int step) {
		this.range = new Range(start, end, step);
	}

	public int start() {
		return range.start();
	}

	public int end() {
		return range.end();
	}

	public int step() {
		return range.step();
	}

	/**
	 * split the current block range into smaller pieces according to step
	 * width
	 */
	public List<BlockedRange1D> splitBlockedRange(int step) {

		if (current != null && current >= range.end())
			return null;

		List<BlockedRange1D> result = ConcurrentCollectionFactory.createArrayList();
		for (int i = 0; i < step; i++) {
			if (current == null)
				current = range.start();
			if (current >= range.end())
				return result;

			result.add(new BlockedRange1D(current, Math.min(current + range.step(), this.range.end()),
					range.step()));
			current += range.step();
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
	public BlockedRange1D createInstance(int number, int total) {
		int size = this.range.end() - this.range.start();
		int split = size / total;
		BlockedRange1D result = new BlockedRange1D(number * split,
				(number + 1 == total) ? this.range.end() : (number + 1) * split, this.range.step());
		return result;
	}
}
