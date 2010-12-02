/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BlockedRangeTests.java
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
package com.sun.electric.tool.util.concurrent.test;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.sun.electric.tool.util.concurrent.utils.BlockedRange1D;
import com.sun.electric.tool.util.concurrent.utils.BlockedRange2D;
import com.sun.electric.tool.util.concurrent.utils.ElapseTimer;

/**
 * @author Felix Schmidt
 * 
 */
public class BlockedRangeTest {

	@Test
	public void testBlockedRange1D() {
		ElapseTimer timer = ElapseTimer.createInstance().start();
		for (int i = 0; i < 100; i++) {
			BlockedRange1D range = new BlockedRange1D(0, 1024 * 1024 * 16, 256);
			range.splitBlockedRange(1024 * 1024 * 16);
		}
		timer.end();
		System.out.println("1D Time: " + (timer.getTime() / 100) + " ms");
	}

	@Test
	public void testBlockedRange2D() {
//		int size = 1024 * 1024;
//		ElapseTimer timer = ElapseTimer.createInstance().start();
//		for (int i = 0; i < 100; i++) {
//			BlockedRange2D range = new BlockedRange2D(0, size, 256, 0, size, 256);
//			range.splitBlockedRange(size);
//		}
//		timer.end();
//		System.out.println("2D Time: " + (timer.getTime() / 100) + " ms");
	}

	@Test
	public void testBlockRange1D() {

		int[] testValues = { 2, 5, 7, 10, 13, 100, 20000000 };

		for (int i = 0; i < testValues.length; i++) {
			BlockedRange1D range = new BlockedRange1D(0, testValues[i], 2);
			BlockedRange1D range1 = (BlockedRange1D) range.createInstance(0, 2);
			BlockedRange1D range2 = (BlockedRange1D) range.createInstance(1, 2);

			Assert.assertEquals(0, range1.start());
			Assert.assertEquals(testValues[i] / 2, range2.start());
			Assert.assertEquals(testValues[i] / 2, range1.end());
			Assert.assertEquals(testValues[i], range2.end());
		}

		BlockedRange1D[] inst = new BlockedRange1D[8];
		BlockedRange1D range = new BlockedRange1D(0, 20000000, 128);

		for (int i = 0; i < 8; i++) {
			inst[i] = range.createInstance(i, 8);
		}

		int splitSize = 20000000 / 8;
		for (int i = 0; i < 8; i++) {
			BlockedRange1D tmp = inst[i];
			Assert.assertEquals(splitSize * i, tmp.start());
			Assert.assertEquals(splitSize * (i + 1), tmp.end());
			BlockedRange1D splitted;
			while (true) {
				List<BlockedRange1D> ranges = tmp.splitBlockedRange(1);
				if (ranges == null) {
					break;
				}
				splitted = ranges.get(0);
				Assert.assertTrue(splitted.end() <= tmp.end());
				System.out.println(splitted.end());
			}
		}
	}

	@Test
	public void testBlockRange2D() {

		int sizeX = 11;
		int sizeY = 11;
		BlockedRange2D range = new BlockedRange2D(0, sizeX, 2, 0, sizeY, 2);

		for (int i = 0; i < sizeX; i += 2) {
			for (int j = 0; j < sizeY; j += 2) {
				BlockedRange2D tmpRange = (BlockedRange2D) range.splitBlockedRange(1).get(0);

				Assert.assertEquals(i, tmpRange.row().start());
				Assert.assertEquals(j, tmpRange.col().start());
				if (i + 2 <= sizeX)
					Assert.assertEquals(i + 2, tmpRange.row().end());
				else
					Assert.assertEquals(sizeX, tmpRange.row().end());

				if (j + 2 <= sizeY)
					Assert.assertEquals(j + 2, tmpRange.col().end());
				else
					Assert.assertEquals(sizeY, tmpRange.col().end());

			}
		}

		Assert.assertTrue(range.splitBlockedRange(1).size() == 0);

	}
}
