/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TimedSignal.java
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


/**
 * Class to define a signal in a simulation that has time information.
 * This is a superclass for specific signal types: DigitalSignal and AnalogSignal.
 */
public class TimedSignal extends Signal
{
	/** an array of time values on this signal (null to use common time) */	private double [] time;

	/**
	 * Constructor for a simulation signal.
	 * @param an the Analysis object in which this TimedSignal will reside.
	 */
	protected TimedSignal(Analysis an)
	{
		super(an);
		time = null;
	}

    public void finished()
    {
        time = null;
    }

	/**
	 * Method to build a time vector for this TimedSignal.
	 * TimedSignals can have their own time information, or they can use a "common time" array
	 * that is part of the simulation data.
	 * If using common time, then each TimedSignal must have the same number of entries, and
	 * each entry must be at the same time as the corresponding entries in other TimedSignals.
	 * Using common time saves memory, because the time information does not have to be
	 * stored with each TimedSignal.
	 * This method requests that the TimedSignal have its own time array, and not use common time data.
	 * @param numEvents the number of events on this TimedSignal (the length of the time array).
	 */
	public void buildTime(int numEvents) { time = new double[numEvents]; }

	/**
	 * Method to return the value of time for a given event on this TimedSignal.
	 * Depending on whether common time data is being used, the time information is
	 * found on this TimedSignal or on the overall simulation data.
	 * @param index the event being querried (0-based).
	 * @return the value of time at that event.
	 */
	public double getTime(int index)
	{
		if (time == null)
		{
			double [] commonTimeArray = an.getCommonTimeArray();
			if (commonTimeArray == null) return 0;
			return commonTimeArray[index];
		}
		return time[index];
	}

	/**
	 * Method to return the value of time for a given event on this TimedSignal.
	 * Depending on whether common time data is being used, the time information is
	 * found on this TimedSignal or on the overall simulation data.
	 * @param index the event being querried (0-based).
	 * @return the value of time at that event.
	 */
	public double getTime(int index, int sweep)
	{
		if (time == null)
		{
			double [] sct = (double [])an.getCommonTimeArray(sweep);
			return sct[index];
		}
		return time[index];
	}

	/**
	 * Method to return the time vector for this TimedSignal.
	 * The vector is only valid if this TimedSignal is NOT using common time.
	 * TimedSignals can have their own time information, or they can use a "common time" array
	 * that is part of the simulation data.
	 * If using common time, then each TimedSignal must have the same number of entries, and
	 * each entry must be at the same time as the corresponding entries in other TimedSignals.
	 * @return the time array for this TimedSignal.
	 * Returns null if this TimedSignal uses common time.
	 */
	public double [] getTimeVector() { return time; }

	/**
	 * Method to set the time vector for this TimedSignal.
	 * Overrides any previous time vector that may be on this TimedSignal.
	 * TimedSignals can have their own time information, or they can use a "common time" array
	 * that is part of the simulation data.
	 * If using common time, then each TimedSignal must have the same number of entries, and
	 * each entry must be at the same time as the corresponding entries in other TimedSignals.
	 * @param time a new time vector for this TimedSignal.
	 */
	public void setTimeVector(double [] time) { bounds = null;   this.time = time; }

	/**
	 * Method to set an individual time entry for this TimedSignal.
	 * Only applies if common time is NOT being used for this TimedSignal.
	 * TimedSignals can have their own time information, or they can use a "common time" array
	 * that is part of the simulation data.
	 * If using common time, then each TimedSignal must have the same number of entries, and
	 * each entry must be at the same time as the corresponding entries in other TimedSignals.
	 * @param entry the entry in the event array of this TimedSignal (0-based).
	 * @param t the new value of time at this event.
	 */
	public void setTime(int entry, double t) { bounds = null;   time[entry] = t; }
}
