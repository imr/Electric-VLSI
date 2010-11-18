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
		for(int i = 0; i < 100; i++) {
			BlockedRange1D range = new BlockedRange1D(0, 1024*1024*16, 256);
			range.splitBlockedRange(1024*1024*16);
		}
		timer.end();
		System.out.println("1D Time: " + (timer.getTime() / 100) + " ms");
	}
	
	@Test
	public void testBlockedRange2D() {
		int size = 1024*1024;
		ElapseTimer timer = ElapseTimer.createInstance().start();
		for(int i = 0; i < 100; i++) {
			BlockedRange2D range = new BlockedRange2D(0, size, 256, 0, size, 256);
			range.splitBlockedRange(size);
		}
		timer.end();
		System.out.println("2D Time: " + (timer.getTime() / 100) + " ms");
	}
}
