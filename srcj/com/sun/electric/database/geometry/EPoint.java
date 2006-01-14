/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EPoint.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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

package com.sun.electric.database.geometry;

import java.awt.geom.Point2D;
import java.io.Serializable;

/**
 * The <code>EPoint</code> immutable class defines a point representing
 * a location in (x,&nbsp;y) coordinate space. This class extends abstract
 * class Point2D. This calss is used in Electric database.
 * Coordiates are snapped to grid according to <code>DBMath.round</code> method.
 */
final public class EPoint extends Point2D implements Serializable {

	public static final EPoint ORIGIN = new EPoint(0, 0);

    /**
     * The X coordinate of this <code>EPoint</code>.
     */
    private final double x;

    /**
     * The Y coordinate of this <code>EPoint</code>.
     */
    private final double y;

    /**
     * Constructs and initializes a <code>EPoint</code> with the
     * specified coordinates snapped to the grid.
     * @param x the x-coordinate to which to set the newly
     * constructed <code>EPoint</code>
     * @param y the y-coordinate to which to set the newly
     * constructed <code>EPoint</code>
     */
    public EPoint(double x, double y) {
		x = DBMath.round(x);
		if (x == -0.0) x = +0.0;
		y = DBMath.round(y);
		if (y == -0.0) y = +0.0;
		this.x = x;
		this.y = y;
    }

    /**
     * Returns <code>EPoint</code> from specified <code>Point2D</code>
     * snapped to the grid.
     * @param p specified Point2D
	 * @return Snapped EPoint
     */
	public static EPoint snap(Point2D p) {
		return (p instanceof EPoint) ? (EPoint)p : new EPoint(p.getX(), p.getY());
	}

    /**
     * Returns the X coordinate of this <code>EPoint</code> 
     * in <code>double</code> precision.
     * @return the X coordinate of this <code>EPoint</code>.
     */
    public double getX() {
		return x;
    }

    /**
     * Returns the Y coordinate of this <code>EPoint</code> 
     * in <code>double</code> precision.
     * @return the Y coordinate of this <code>EPoint</code>.
     */
    public double getY() {
		return y;
    }

    /**
     * This method overrides <code>Point2D.setLocation</code> method.
     * It throws UnsupportedOperationException.
     * @param x the x-coordinate to which to set this <code>EPoint</code>
     * @param y the y-coordinate to which to set this <code>EPoint</code>
     * @throws UnsupportedOperationException
     */
    public void setLocation(double x, double y) {
		throw new UnsupportedOperationException();
    }

    /**
     * Creates mutable <code>Point2D.Double</code> from the <code>EPoint</code>.
     * @return mutable Point2D
     */
	public Point2D.Double mutable() {
		return new Point2D.Double(x, y);
	}

	/**
	 * Returns a <code>String</code> that represents the value 
	 * of this <code>EPoint</code>.
	 * @return a string representation of this <code>EPoint</code>.
	 */
	public String toString() {
	    return "EPoint2D["+x+", "+y+"]";
	}
}
