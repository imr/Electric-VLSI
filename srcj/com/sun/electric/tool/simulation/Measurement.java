/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Measurement.java
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
package com.sun.electric.tool.simulation;

import java.awt.geom.Rectangle2D;

/**
 * Class to define a measurement in the simulation waveform window.
 */
public class Measurement extends Signal
{
	private double [] values;

	/**
	 * Constructor for a Measurement.
	 * @param name the name of the Measurement.
	 * @param values an array of doubles for the Measurement.
	 */
	public Measurement(Stimuli sd)
	{
		super(sd);
	}

	/**
	 * Method to set the values on this Measurement.
	 * @param values an array of doubles.
	 */
	public void setValues(double [] values) { this.values = values; }

	/**
	 * Method to get the numeric values associated with this Measurement.
	 * @return the numeric values associated with this Measurement.
	 * @return
	 */
	public double [] getValues() { return values; }

	/**
	 * Method to compute the low and high range of values on this Measurement.
	 * The result is stored in the "bounds" field variable.
	 */
	protected void calcBounds()
	{
		// determine extent of the data
		double lowValue=0, highValue=0;
		boolean first = true;
        for (int i=0; i<values.length; i++)
		{
			if (first)
			{
				first = false;
				lowValue = values[i];
				highValue = values[i];
			} else
			{
				if (values[i] < lowValue) lowValue = values[i];
				if (values[i] > highValue) highValue = values[i];
			}
		}
		bounds = new Rectangle2D.Double(0, lowValue, 0, highValue-lowValue);
	}
}
