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
	 * @return the angle between the points (in radians).
	 */
	public static double figureAngle(Point2D.Double end1, Point2D.Double end2)
	{
		double angle = Math.atan2(end2.getY()-end1.getY(), end2.getX()-end1.getX());
		if (angle < 0) angle += Math.PI*2;
		return angle;		
	}

	/**
	 * Routine to return the sum of two points.
	 * @param p the first point.
	 * @param dx the X component of the second point
	 * @param dt the T component of the second point
	 * @return the sum of two points.
	 */
	public static Point2D.Double addPoints(Point2D.Double p, double dx, double dy)
	{
		return new Point2D.Double(p.getX()+dx, p.getY()+dy);
	}

	/**
	 * Routine to parse the number in a string.
	 * <P>
	 * There are many reasons to use this routine instead of Integer.parseInt...
	 * <UL>
	 * <LI>This routine can handle any radix.
	 *     If the number begins with "0", presume base 8.
	 *     If the number begins with "0x", presume base 16.
	 *     Otherwise presume base 10.
	 * <LI>This routine can handle numbers that affect the sign bit.
	 *     If you give 0xFFFFFFFF to Integer.parseInt, you get a numberFormat exception.
	 *     This routine properly returns -1.
	 * <LI>This routine does not require that the entire string be part of the number.
	 *     If there is extra text after the end, Integer.parseInt fails (for example "123xx").
	 * </UL>
	 * @param s the string with a number in it.
	 * @return the numeric value.
	 */
	public static int atoi(String s)
	{
		return atoi(s, 0);
	}

	/**
	 * Routine to parse the number in a string.
	 * See the comments for "atoi(String s)" for reasons why this routine exists.
	 * @param s the string with a number in it.
	 * @param pos the starting position in the string to find the number.
	 * @return the numeric value.
	 */
	public static int atoi(String s, int pos)
	{
		int base = 10;
		int num = 0;
		int sign = 1;
		int len = s.length();
		if (s.charAt(pos) == '-')
		{
			pos++;
			sign = -1;
		}
		if (s.charAt(pos) == '0')
		{
			pos++;
			base = 8;
			if (pos < len && (s.charAt(pos) == 'x' || s.charAt(pos) == 'X'))
			{
				pos++;
				base = 16;
			}
		}
		for(; pos < len; pos++)
		{
			char cat = s.charAt(pos);
			if ((cat >= 'a' && cat <= 'f') || (cat >= 'A' && cat <= 'F'))
			{
				if (base != 16) break;
				num = num * 16;
				if (cat >= 'a' && cat <= 'f') num += cat - 'a' + 10; else
					num += cat - 'A' + 10;
				continue;
			}
			if (!Character.isDigit(cat)) break;
			if (cat >= '8' && base == 8) break;
			num = num * base + cat - '0';
		}
		return(num * sign);
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

	/*
	 * routine to determine the intersection of two lines and return that point
	 * in (x,y).  The first line is at "ang1" radians and runs through (x1,y1)
	 * and the second line is at "ang2" radians and runs through (x2,y2).  The
	 * routine returns a negative value if the lines do not intersect.
	 */
	public static Point2D.Double intersect(Point2D.Double p1, double fang1, Point2D.Double p2, double fang2)
	{
		/* find the minimum and maximum angles */
		double fmin = fang2, fmax = fang1;
		if (fang1 < fang2)
		{
			fmin = fang1; fmax = fang2;
		}

		/* cannot handle lines if they are at the same angle */
		fang1 %= Math.PI * 2;
		fang2 %= Math.PI * 2;
		if (doublesEqual(fang1, fang2)) return null;
		if (doublesEqual(fmin + Math.PI, fmax)) return null;

		double fa1 = Math.sin(fang1);
		double fb1 = -Math.cos(fang1);
		double fc1 = -fa1 * p1.getX() - fb1 * p1.getY();
		double fa2 = Math.sin(fang2);
		double fb2 = -Math.cos(fang2);
		double fc2 = -fa2 * p2.getX() - fb2 * p2.getY();
		if (Math.abs(fa1) < Math.abs(fa2))
		{
			double fswap = fa1;   fa1 = fa2;   fa2 = fswap;
			fswap = fb1;   fb1 = fb2;   fb2 = fswap;
			fswap = fc1;   fc1 = fc2;   fc2 = fswap;
		}
		double fy = (fa2 * fc1 / fa1 - fc2) / (fb2 - fa2*fb1/fa1);
		return new Point2D.Double((-fb1 * fy - fc1) / fa1, fy);
	}

	private static double DBL_EPSILON = 2.2204460492503131e-016; /* smallest such that 1.0+DBL_EPSILON != 1.0 */

	/*
	 * Routine to return true if "a" is equal to "b" within the epsilon of double floating
	 * point arithmetic.
	 */
	public static boolean doublesEqual(double a, double b)
	{
		if (Math.abs(a-b) <= DBL_EPSILON) return true;
		return false;
	}
}
