/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DBMath.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

/**
 * This class is a collection of math utilities used for
 * Database Units.  It overrides several important methods
 * from GenMath used when comparing doubles.
 */
public class DBMath extends GenMath {

    /**
     * epsilon is the largest amount of absolute difference
     * between two numbers in the database for which those numbers
     * will still be regarded as "equal".
     */
    private static double EPSILON = 0.01;

    /**
     * Method to tell whether a point is inside of a bounds, compensating
     * for possible database precision errors.
     * @param pt the point in question
     * @param bounds the bounds being tested
     * @return true if the point is basically within the bounds, within some
     * epsilon.
     */
    public static boolean pointInRect(Point2D pt, Rectangle2D bounds) {
        double epsilon = EPSILON;
        if (pt.getX() < (bounds.getMinX() - epsilon)) return false;
        if (pt.getX() > (bounds.getMaxX() + epsilon)) return false;
        if (pt.getY() < (bounds.getMinY() - epsilon)) return false;
        if (pt.getY() > (bounds.getMaxY() + epsilon)) return false;
        return true;
    }

    /**
    * Method to compare two double-precision database values.
    * @param a the first number.
    * @param b the second number.
    * @return true if the numbers are approximately equal (to a few decimal places).
    */
    public static boolean doublesClose(double a, double b) {
        if (Math.abs(a-b) < EPSILON) return true;
        return false;
    }

    /**
     * Method to compare two double-precision database coordinates within an approximate epsilon.
     * @param a the first point.
     * @param b the second point.
     * @return true if the points are approximately equal.
     */
    public static boolean pointsClose(Point2D a, Point2D b)
    {
        if (doublesClose(a.getX(), b.getX()) &&
                doublesClose(a.getY(), b.getY())) return true;
        return false;
    }

    /**
     * Method to tell whether a point is on a given line segment.
     * @param end1 the first end of the line segment.
     * @param end2 the second end of the line segment.
     * @param pt the point in question.
     * @return true if the point is on the line segment.
     */
    public static boolean isOnLine(Point2D end1, Point2D end2, Point2D pt)
    {
        Point2D closestPointOnSegment = closestPointToSegment(end1, end2, pt);
        return pointsClose(closestPointOnSegment, pt);
    }

}
