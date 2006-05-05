/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AnalogSignal.java
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
 * Class to define an analog signal in the simulation waveform window.
 */
public class AnalogSignal extends TimedSignal
{
	/** the array of values on this signal */		private double [] values;

	/**
	 * Constructor for an analog signal.
	 * @param an the Analysis object in which this signal will reside.
	 */
	public AnalogSignal(Analysis an)
	{
		super(an);
	}

    public void finished()
    {
        values = null;
    }

	/**
	 * Method to initialize this as a basic simulation signal with a specified number of events.
	 * Allocates an array to hold those events.
	 * @param numEvents the number of events in this signal.
	 */
	public void buildValues(int numEvents)
	{
		values = new double[numEvents];
	}

	/**
	 * Method to set the value of this signal at a given event index.
	 * @param index the event index (0-based).
	 * @param value the value to set at the given event index.
	 * If this signal is not a basic signal, print an error message.
	 */
	public void setValue(int index, double value)
	{
		values[index] = value;
		bounds = null;
	}

	/**
	 * Method to return the value of this signal at a given event index.
     * @param sweep sweep index
	 * @param index the event index (0-based).
     * @param result double array of length 3 to return (time, lowValue, highValue)
	 * If this signal is not a basic signal, return 0 and print an error message.
	 */
    public void getEvent(int sweep, int index, double[] result) {
        if (sweep != 0)
            throw new IndexOutOfBoundsException();
        result[0] = getTime(index);
        result[1] = result[2] = values[index];
    }
    
	/**
	 * Method to return the number of events in this signal.
	 * This is the number of events along the horizontal axis, usually "time".
	 * @return the number of events in this signal.
	 */
	public int getNumEvents() { return getNumEvents(0); }

	/**
	 * Method to return the number of events in one sweep of this signal.
	 * This is the number of events along the horizontal axis, usually "time".
	 * The method only works for sweep signals.
	 * @param sweep the sweep number to query.
	 * @return the number of events in this signal.
	 */
	public int getNumEvents(int sweep)
	{
        if (sweep != 0)
            throw new IndexOutOfBoundsException();
        return values.length;
	}

	/**
	 * Method to return the number of sweeps in this signal.
	 * @return the number of sweeps in this signal.
	 * If this signal is not a sweep signal, returns 1.
	 */
	public int getNumSweeps()
	{
		return 1;
	}

	/**
	 * Method to compute the low and high range of time and value on this signal.
	 * The result is stored in the "bounds" field variable.
	 */
	protected void calcBounds()
	{
		// determine extent of the data
		double lowTime=0, highTime=0, lowValue=0, highValue=0;
		boolean first = true;
        double[] result = new double[3];
        for (int sweep = 0, numSweeps = getNumSweeps(); sweep < numSweeps; sweep++) {
			for(int i=0, numEvents=getNumEvents(sweep); i<numEvents; i++)
			{
                getEvent(sweep, i, result);
				double time = result[0];
				double lowVal = result[1];
				double highVal = result[2];
				if (first)
				{
					first = false;
					lowTime = highTime = time;
					lowValue = lowVal;
					highValue = highVal;
				} else
				{
					if (time < lowTime) lowTime = time;
					if (time > highTime) highTime = time;
					if (lowVal < lowValue) lowValue = lowVal;
					if (highVal > highValue) highValue = highVal;
				}
			}
		}
		bounds = new Rectangle2D.Double(lowTime, lowValue, highTime-lowTime, highValue-lowValue);
	}
}
