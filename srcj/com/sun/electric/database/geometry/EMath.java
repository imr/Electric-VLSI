/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EMath.java
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

/**
 * This class is a collection of math utilities.
 */
public class EMath
{
	/**
	 * Routine to return the angle between two points.
	 * @param end1 the first point.
	 * @param end2 the second point.
	 * @return the angle between the points (in degrees).
	 */
	public static double figureAngle(Point2D.Double end1, Point2D.Double end2)
	{
		double angle = Math.atan2(end2.getY()-end1.getY(), end2.getX()-end1.getX()) * 180.0 / Math.PI;
		if (angle < 0) angle += 360.0;
		return angle;		
	}

	/**
	 * Routine to return the distance between two points.
	 * @param end1 the first point.
	 * @param end2 the second point.
	 * @return the distance between the points.
	 */
	public static double computeDistance(Point2D.Double end1, Point2D.Double end2)
	{
		double xDist = end2.getX() - end1.getX();
		double yDist = end2.getY() - end1.getY();
		if (xDist == 0) return Math.abs(yDist);
		if (yDist == 0) return Math.abs(xDist);
		return Math.sqrt(yDist*yDist + xDist*xDist);
	}

	/**
	 * Routine to tell whether a point is on a given line segment.
	 * @param end1 the first end of the line segment.
	 * @param end2 the second end of the line segment.
	 * @param pt the point in question.
	 * @return true if the point is on the line segment.
	 */
	public static boolean isOnLine(Point2D.Double end1, Point2D.Double end2, Point2D.Double pt)
	{
		// trivial rejection if point not in the bounding box of the line
		if (pt.getX() < Math.min(end1.getX(), end2.getX())) return false;
		if (pt.getX() > Math.max(end1.getX(), end2.getX())) return false;
		if (pt.getY() < Math.min(end1.getY(), end2.getY())) return false;
		if (pt.getY() > Math.max(end1.getY(), end2.getY())) return false;

		// handle manhattan cases specially
		if (end1.getX() == end2.getX())
		{
			if (pt.getX() == end1.getX()) return true;
			return false;
		}
		if (end1.getY() == end2.getY())
		{
			if (pt.getY() == end1.getY()) return true;
			return false;
		}

		// handle nonmanhattan
		if ((pt.getX()-end1.getX()) * (end2.getY()-end1.getY()) == (pt.getY()-end1.getY()) * (end2.getX()-end1.getX())) return true;
		return false;
	}
}
