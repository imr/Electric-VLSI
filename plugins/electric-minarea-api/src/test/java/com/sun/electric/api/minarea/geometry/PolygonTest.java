/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PolygonTest.java
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

import com.sun.electric.api.minarea.geometry.Polygon.Rectangle;

/**
 * @author Felix Schmidt
 *
 */
public class PolygonTest {
	
	@Test
	public void testBoundingBox() {
		Polygon poly = new Polygon(new Point(1, 2), new Point(3, 3), new Point(6, 1));
		Rectangle expected = new Rectangle(new Point(1,1), new Point(6,3));
		
		Rectangle boundingBox = poly.getBoundingBox();
		Assert.assertEquals(expected, boundingBox);
	}
	
	@Test
	public void testEquals() {
		Polygon poly1 = new Polygon(new Point(1, 2), new Point(3, 3), new Point(6, 1));
		Polygon poly2 = new Polygon(new Point(1, 2), new Point(3, 3), new Point(6, 1));
		Polygon poly3 = new Polygon(new Point(1, 2), new Point(4, 3), new Point(6, 1));
		
		Assert.assertTrue(poly1.equals(poly1));
		Assert.assertTrue(poly1.equals(poly2));
		Assert.assertFalse(poly1.equals(poly3));
		Assert.assertFalse(poly1.equals(null));
	}
	
	@Test
	public void testRectangleTransform() {
		Rectangle rect = new Rectangle(new Point(1,1), new Point(6, 3));
		Polygon expected = new Polygon(new Point(1,1), new Point(1, 3), new Point(6, 3), new Point(6, 1));
		Polygon transformed = rect.transformToPolygon();
		
		Assert.assertEquals(expected, transformed);
	}
	
	@Test
	public void testToString() {
		Polygon poly = new Polygon(new Point(1,1), new Point(1, 3), new Point(6, 3), new Point(6, 1));
		System.out.println(poly.toString());
		
		Rectangle rect = new Rectangle(new Point(1,1), new Point(6, 3));
		System.out.println(rect.toString());
	}

}
