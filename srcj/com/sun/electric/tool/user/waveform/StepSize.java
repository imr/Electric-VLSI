/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StepSize.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.waveform;

/**
 * Class to determine a sensible range of values to display for a given real range.
 * Used by rulers, which want to display properly "gridded" values.
 */
public class StepSize
{
	private double separation;
	private double low, high;
	private int rangeScale;
	private int stepScale;

	/**
	 * Method to return the low value to display for this range.
	 * @return the low value to display for this range.
	 */
	public double getLowValue() { return low; }

	/**
	 * Method to return the high value to display for this range.
	 * @return the high value to display for this range.
	 */
	public double getHighValue() { return high; }

	/**
	 * Method to return the separation between ticks in this range.
	 * @return the separation between ticks in this range.
	 */
	public double getSeparation() { return separation; }

	/**
	 * Method to return the power of 10 used for steps in this range.
	 * @return the power of 10 used for steps in this range.
	 */
	public int getStepScale() { return stepScale; }

	/**
	 * Method to return the power of 10 used for this range.
	 * @return the power of 10 used for this range.
	 */
	public int getRangeScale() { return rangeScale; }

	/**
	 * Constructor to analyze a range of values and determine sensible displayable values.
	 * @param h the high value in the range.
	 * @param l the low value in the range.
	 * @param n the number of steps in the range.
	 * This object contains the adjusted values of "l" and "h"
	 * as well as the integers rangeScale and stepScale, which are the
	 * powers of 10 that belong to the largest value in the interval and the step size.
	 */
	public StepSize(double h, double l, int n)
	{
		low = l;   high = h;
		rangeScale = stepScale = 0;

		double range = Math.max(Math.abs(l), Math.abs(h));
		if (range == 0.0)
		{
			separation = 0;
			return;
		}
        if (Double.isInfinite(l) || Double.isInfinite(h)) {
            System.out.println("Error: Inifite low or high range detected");
            separation = 0;
            return;
        }

		// determine powers of ten in the range
		while (range >= 10.0) { range /= 10.0;   rangeScale++; }
		while (range <= 1.0 ) { range *= 10.0;   rangeScale--; }

		// determine powers of ten in the step size
		double d = Math.abs(h - l)/(double)n;
		if (Math.abs(d/(h+l)) < 0.0000001) d = 0.1;
		int mp = 0;
		while (d >= 10.0) { d /= 10.0;   mp++;   stepScale++; }
		while (d <= 1.0 ) { d *= 10.0;   mp--;   stepScale--; }
		double m = Math.pow(10, mp);

		int di = (int)d;
		if (di > 2 && di <= 5) di = 5; else 
			if (di > 5) di = 10;
		int li = (int)(l / m);
		int hi = (int)(h / m);
		li = (li/di) * di;
		hi = (hi/di) * di;
		if (li < 0) li -= di;
		if (hi > 0) hi += di;
		low = (double)li * m;
		high = (double)hi * m;
		separation = di * m;
	}
}
