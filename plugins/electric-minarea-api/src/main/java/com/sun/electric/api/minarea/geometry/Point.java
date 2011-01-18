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

/**
 * Immutable point implementation
 * 
 * @author Felix Schmidt
 *
 */
public class Point {
	
	private final int xCoord;
	private final int yCoord;
	
	public Point(int x, int y) {
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
	
	/* (non-Javadoc)
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
	
	/* (non-Javadoc)
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

	/* (non-Javadoc)
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
	
	/**
	 * 
	 * @param other
	 * @return
	 */
	public int determinant(Point other) {
		return this.xCoord * other.yCoord - this.yCoord * other.yCoord;
	}
	
	public Point mirror() {
		return new Point(this.yCoord,this.xCoord);
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

}
