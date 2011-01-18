/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Polygon.java
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

import java.util.Arrays;
import java.util.List;

/**
 * Immutable Polygon implementation
 * 
 * @author Felix Schmidt
 * 
 */
public class Polygon {
	
	public static interface PolygonUnionStrategy {
		public Polygon union(Polygon poly1, Polygon poly2);
	}

	protected List<Point> points;

	public Polygon(Point... points) {
		this.points = Arrays.asList(points);
	}

	public Rectangle getBoundingBox() {
		int lx = Integer.MAX_VALUE;
		int ly = Integer.MAX_VALUE;
		int hx = Integer.MIN_VALUE;
		int hy = Integer.MIN_VALUE;

		for (Point pt : points) {
			if (pt.getX() < lx)
				lx = pt.getX();
			if (pt.getY() < ly)
				ly = pt.getY();
			if (pt.getX() > hx)
				hx = pt.getX();
			if (pt.getY() > hy)
				hy = pt.getY();
		}

		return new Rectangle(new Point(lx, ly), new Point(hx, hy));
	}

	/**
	 * 
	 * @author Felix Schmidt
	 * 
	 */
	public static class Rectangle extends Polygon {

		/**
		 * 
		 * @param min
		 * @param max
		 */
		public Rectangle(Point min, Point max) {
			if(min.getX() >= max.getX() || min.getY() >= max.getY())
				throw new IllegalArgumentException();
			
			this.points = Arrays.asList(min, max);
		}

		/**
		 * 
		 * @return
		 */
		public Point getMin() {
			return points.get(0);
		}

		/**
		 * 
		 * @return
		 */
		public Point getMax() {
			return points.get(1);
		}

		/**
		 * 
		 * @return
		 */
		public int width() {
			return points.get(1).getX() - points.get(0).getX();
		}

		/**
		 * 
		 * @return
		 */
		public int height() {
			return points.get(1).getY() - points.get(0).getY();
		}

	}

	/**
	 * 
	 * @author Felix Schmidt
	 * 
	 */
	public static class PolygonHole extends Polygon {

		/**
		 * 
		 * @param points
		 */
		public PolygonHole(Point... points) {
			super(points);
		}
	}

}
