/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Vertex.java
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

/**
 * @author Felix Schmidt
 *
 */
public class VertexTest {
	
	@Test
	public void testLength() {
		Edge vert = new Edge(new Point(0, 0), new Point(2, 0));
		Assert.assertEquals(2, vert.length());
	}
	
	@Test
	public void testEquals() {
		Edge vert1 = new Edge(new Point(0, 0), new Point(2, 0));
		Edge vert2 = new Edge(new Point(0, 0), new Point(2, 0));
		Edge vert3 = new Edge(new Point(0, 0), new Point(2, 1));
		Edge vert4 = new Edge(new Point(2, 0), new Point(0, 0));
		
		Assert.assertTrue(vert1.equals(vert1));
		Assert.assertTrue(vert1.equals(vert2));
		Assert.assertTrue(vert1.equals(vert4));
		Assert.assertFalse(vert1.equals(vert3));
		Assert.assertFalse(vert1.equals(null));
	}

}
