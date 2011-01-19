/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Point.java
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

import com.sun.electric.api.minarea.LayoutCell;
import com.sun.electric.api.minarea.ManhattanOrientation;
import java.io.Serializable;

/**
 * Immutable point implementation
 * 
 * @author Felix Schmidt
 * 
 */
@SuppressWarnings("serial")
public class Point implements Serializable {

	protected final int xCoord;
	protected final int yCoord;

	/**
	 * 
	 * @param x [-LayoutCell.MAX_COORD, LayoutCell.MAX_COORD]
	 * @param y [-LayoutCell.MAX_COORD, LayoutCell.MAX_COORD]
	 */
	public Point(int x, int y) {
		if (x < -LayoutCell.MAX_COORD || x > LayoutCell.MAX_COORD) {
			throw new IllegalArgumentException(
					"x has to be in range [-LayoutCell.MAX_COORD, LayoutCell.MAX_COORD]");
		}

		if (y < -LayoutCell.MAX_COORD || y > LayoutCell.MAX_COORD) {
			throw new IllegalArgumentException(
					"y has to be in range [-LayoutCell.MAX_COORD, LayoutCell.MAX_COORD]");
		}

		this.xCoord = x;
		this.yCoord = y;
	}

	/**
	 * @return the xCoord
	 */
	public int getX() {
		return xCoord;
	}

	/**
	 * 
	 * @param xCoord
	 * @return
	 */
	public Point withX(int xCoord) {
		return new Point(xCoord, yCoord);
	}

	/**
	 * @return the yCoord
	 */
	public int getY() {
		return yCoord;
	}

	/**
	 * 
	 * @param xCoord
	 * @return
	 */
	public Point withY(int yCoord) {
		return new Point(xCoord, yCoord);
	}

	public int[] toArray() {
		return new int[] { xCoord, yCoord };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("Point = (");
		builder.append(xCoord);
		builder.append(", ");
		builder.append(yCoord);
		builder.append(")");

		return builder.toString();
	}

	// ********************* Some helper functions **********************

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + xCoord;
		result = prime * result + yCoord;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Point other = (Point) obj;
		if (xCoord != other.xCoord)
			return false;
		if (yCoord != other.yCoord)
			return false;
		return true;
	}

	/**
	 * @param other
	 * @return
	 */
	public Point add(Point other) {
		return new Point(this.xCoord + other.xCoord, this.yCoord + other.yCoord);
	}

	/**
	 * 
	 * @param scaleFactor
	 * @return
	 */
	public Point scale(int scaleFactor) {
		return scale(scaleFactor, scaleFactor);
	}

	/**
	 * 
	 * @param scaleFactorX
	 * @param scaleFactorY
	 * @return
	 */
	public Point scale(int scaleFactorX, int scaleFactorY) {
		return new Point(this.xCoord * scaleFactorX, this.yCoord * scaleFactorY);
	}

	public Point mirror() {
		return new Point(this.yCoord, this.xCoord);
	}

	/**
	 * 
	 * @param orientation
	 * @return
	 */
	public Point transform(ManhattanOrientation orientation) {
            switch (orientation) {
                case R0:
                    return this;
                case R90:
                    return new Point(-getY(), getX());
                case R180:
                    return new Point(-getX(), -getY());
                case R270:
                    return new Point(getY(), -getX());
                case MY:
                    return new Point(-getX(), getY());
                case MYR90:
                    return new Point(-getY(), -getX());
                case MX:
                    return new Point(getX(), -getY());
                case MXR90:
                    return new Point(getY(), getX());
                default:
                    throw new AssertionError();
            }
	}

	// ********************* Some helper classes **********************

	/**
	 * Use objects of type NullPoint as a equivalent to null
	 */
	public static final class NullPoint extends Point {

		/**
		 * @param x
		 * @param y
		 */
		public NullPoint() {
			super(0, 0);
		}

	}

	public static final class Vector extends Point {

		/**
		 * @param x
		 * @param y
		 */
		public Vector(int x, int y) {
			super(x, y);
			// TODO Auto-generated constructor stub
		}

		public Vector(Point head, Point tail) {
			super(head.xCoord - tail.xCoord, head.yCoord - tail.yCoord);
		}
		
		/**
		 * 
		 * @param other
		 * @return
		 */
		public long determinant(Vector other) {
			return (long)this.xCoord * (long)other.yCoord - (long)this.yCoord * (long)other.xCoord;
		}

	}
}
