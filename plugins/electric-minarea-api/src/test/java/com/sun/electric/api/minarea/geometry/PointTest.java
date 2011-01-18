/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PointTest.java
 *
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.api.minarea.geometry;

import junit.framework.Assert;

import org.junit.Test;

import com.sun.electric.api.minarea.geometry.Point.Vector;

/**
 * @author Felix Schmidt
 * 
 */
public class PointTest {

	@Test
	public void testScale() {
		{
			int x = 2, y = 3, scaleX = 2, scaleY = 5;
			Point pt = new Point(x, y).scale(scaleX, scaleY);
			Assert.assertEquals(x * scaleX, pt.getX());
			Assert.assertEquals(y * scaleY, pt.getY());
		}

		{
			int x = 2, y = 3, scale = -1;
			Point pt = new Point(x, y).scale(scale);
			Assert.assertEquals(x * scale, pt.getX());
			Assert.assertEquals(y * scale, pt.getY());
		}
	}
	
	@Test
	public void testMirror() {
		int x = 2, y = 7;
		Point pt = new Point(x, y).mirror();
		Assert.assertEquals(y, pt.getX());
		Assert.assertEquals(x, pt.getY());
	}
	
	@Test
	public void testAdd() {
		int x1 = 2, y1 = 3, x2 = 7, y2 = 10;
		Point pt = new Point(x1, y1).add(new Point(x2, y2));
		Assert.assertEquals(x1 + x2, pt.getX());
		Assert.assertEquals(y1 + y2, pt.getY());
	}
	
	@Test
	public void testDeterminant() {
		int x1 = 2, y1 = 3, x2 = 4, y2 = 5;
		long expected = x1 * y2 - x2 * y1;
		long det = new Vector(x1, y1).determinant(new Vector(x2, y2));
		Assert.assertEquals(expected, det);
	}
	
	@Test
	public void testEquals() {
		Point p1 = new Point(1, 2);
		Point p2 = new Point(1, 2);
		Point p3 = new Point(1, 4);
		
		Assert.assertTrue(p1.equals(p1));
		Assert.assertTrue(p1.equals(p2));
		Assert.assertFalse(p1.equals(p3));
		Assert.assertFalse(p1.equals(null));
	}

}
