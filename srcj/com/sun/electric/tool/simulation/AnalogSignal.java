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
public class AnalogSignal extends Signal
{
	/** a simple analog signal */			private static final int BASICSIGNAL    = 0;
	/** a swept analog analog signal */		private static final int SWEEPSIGNAL    = 1;
	/** an interval analog signal */		private static final int INTERVALSIGNAL = 2;

	private int signalType;
	private double [] values;
	private double [][] sweepValues;
	private double [] highIntervalValues;

	/**
	 * Constructor for an analog signal.
	 * @param sd the Simulation Data object in which this signal will reside.
	 */
	public AnalogSignal(Stimuli sd) { super(sd); }

	/**
	 * Method to initialize this as a basic simulation signal with a specified number of events.
	 * Allocates an array to hold those events.
	 * @param numEvents the number of events in this signal.
	 */
	public void buildValues(int numEvents)
	{
		signalType = BASICSIGNAL;
		values = new double[numEvents];
	}

	/**
	 * Method to initialize this as a sweep simulation signal with a specified number of sweeps and events.
	 * Allocates arrays to hold those events.
	 * @param numSweeps the number of sweeps in this signal.
	 */
	public void setNumSweeps(int numSweeps)
	{
		signalType = SWEEPSIGNAL;
		sweepValues = new double[numSweeps][];
	}

	/**
	 * Method to initialize this as a sweep simulation signal with a specified number of sweeps and events.
	 * Allocates arrays to hold those events.
	 * @param sweep the sweep number in this signal.
	 * @param numEvents the number of events in this sweep of this signal.
	 */
	public void buildSweepValues(int sweep, int numEvents)
	{
		sweepValues[sweep] = new double[numEvents];
	}

	/**
	 * Method to initialize this as an interval simulation signal with a specified number of events.
	 * Allocates arrays to hold those events.
	 * @param numEvents the number of events in this signal.
	 */
	public void buildIntervalValues(int numEvents)
	{
		signalType = INTERVALSIGNAL;
		values = new double[numEvents];
		highIntervalValues = new double[numEvents];
	}

	/**
	 * Method to set the value of this signal at a given event index.
	 * @param index the event index (0-based).
	 * @param value the value to set at the given event index.
	 * If this signal is not a basic signal, print an error message.
	 */
	public void setValue(int index, double value)
	{
		if (signalType != BASICSIGNAL)
		{
			System.out.println("Setting complex data into basic signal");
			return;
		}
		values[index] = value;
		boundsCurrent = false;
	}

	/**
	 * Method to set the value of this signal at a given sweep and event index.
	 * @param sweep the sweep number (0-based).
	 * @param index the event index (0-based).
	 * @param value the value to set at the given sweep and event index.
	 * If this signal is not a sweep signal, print an error message.
	 */
	public void setSweepValue(int sweep, int index, double value)
	{
		if (signalType != SWEEPSIGNAL)
		{
			System.out.println("Setting sweep data into non-sweep signal");
			return;
		}
		sweepValues[sweep][index] = value;
		boundsCurrent = false;
	}

	/**
	 * Method to set the low and high values of this signal at a given event index.
	 * @param index the event index (0-based).
	 * @param lowValue the low value to set at the given event index.
	 * @param highValue the high value to set at the given event index.
	 * If this signal is not an interval signal, print an error message.
	 */
	public void setIntervalValue(int index, double lowValue, double highValue)
	{
		if (signalType != INTERVALSIGNAL)
		{
			System.out.println("Setting interval data into non-interval signal");
			return;
		}
		values[index] = lowValue;
		highIntervalValues[index] = highValue;
		boundsCurrent = false;
	}

	/**
	 * Method to return the value of this signal at a given event index.
	 * @param index the event index (0-based).
	 * @return the value of this signal at the given event index.
	 * If this signal is not a basic signal, return 0 and print an error message.
	 */
	public double getValue(int index)
	{
		if (signalType != BASICSIGNAL)
		{
			System.out.println("Getting basic data from non-basic signal");
			return 0;
		}
		return values[index];
	}

	/**
	 * Method to return the value of this signal for a given sweep and event index.
	 * @param sweep the sweep number (0-based).
	 * @param index the event index (0-based).
	 * @return the value of this signal at the given sweep and event index.
	 * If this signal is not a sweep signal, return 0 and print an error message.
	 */
	public double getSweepValue(int sweep, int index)
	{
		if (signalType != SWEEPSIGNAL)
		{
			System.out.println("Getting sweep data from non-sweep signal");
			return 0;
		}
		return sweepValues[sweep][index];
	}

	/**
	 * Method to return the low end of the interval range for this signal at a given event index.
	 * @param index the event index (0-based).
	 * @return the low end of the interval range at that event index.
	 * If this signal is not an interval signal, return 0 and print an error message.
	 */
	public double getIntervalLowValue(int index)
	{
		if (signalType != INTERVALSIGNAL)
		{
			System.out.println("Getting interval data from non-interval signal");
			return 0;
		}
		return values[index];
	}

	/**
	 * Method to return the high end of the interval range for this signal at a given event index.
	 * @param index the event index (0-based).
	 * @return the high end of the interval range at that event index.
	 * If this signal is not an interval signal, return 0 and print an error message.
	 */
	public double getIntervalHighValue(int index)
	{
		if (signalType != INTERVALSIGNAL)
		{
			System.out.println("Getting interval data from non-interval signal");
			return 0;
		}
		return highIntervalValues[index];
	}

	/**
	 * Method to return the number of events in this signal.
	 * This is the number of events along the horizontal axis, usually "time".
	 * @return the number of events in this signal.
	 */
	public int getNumEvents()
	{
		switch (signalType)
		{
			case BASICSIGNAL:
				return values.length;
			case SWEEPSIGNAL:
				return sweepValues[0].length;
			case INTERVALSIGNAL:
				return values.length;
		}
		return 0;
	}

	/**
	 * Method to return the number of events in one sweep of this signal.
	 * This is the number of events along the horizontal axis, usually "time".
	 * The method only works for sweep signals.
	 * @param sweep the sweep number to query.
	 * @return the number of events in this signal.
	 */
	public int getNumEvents(int sweep)
	{
		if (signalType != SWEEPSIGNAL) return 0;
		return sweepValues[sweep].length;
	}

	/**
	 * Method to return the number of sweeps in this signal.
	 * @return the number of sweeps in this signal.
	 * If this signal is not a sweep signal, returns 0.
	 */
	public int getNumSweeps()
	{
		if (signalType != SWEEPSIGNAL) return 0;
		return sweepValues.length;
	}

	/**
	 * Method to tell whether this signal is a basic analog waveform signal.
	 * Signals can be basic (single value), sweep (multiple value), or interval (value range).
	 * @return true if this is a basic analog waveform signal.
	 */
	public boolean isBasic() { return signalType == BASICSIGNAL; }

	/**
	 * Method to tell whether this signal is a sweep analog waveform signal.
	 * Signals can be basic (single value), sweep (multiple value), or interval (value range).
	 * @return true if this is a sweep analog waveform signal.
	 */
	public boolean isSweep() { return signalType == SWEEPSIGNAL; }

	/**
	 * Method to tell whether this signal is an interval analog waveform signal.
	 * Signals can be basic (single value), sweep (multiple value), or interval (value range).
	 * @return true if this is an interval analog waveform signal.
	 */
	public boolean isInterval() { return signalType == INTERVALSIGNAL; }

	/**
	 * Method to compute the low and high range of time and value on this signal.
	 * The result is stored in the "bounds" field variable.
	 */
	protected void calcBounds()
	{
		// determine extent of the data
		double lowTime=0, highTime=0, lowValue=0, highValue=0;
		boolean first = true;
		if (isBasic())
		{
			for(int i=0; i<values.length; i++)
			{
				double time = getTime(i);
				double value = values[i];
				if (first)
				{
					first = false;
					lowTime = highTime = time;
					lowValue = highValue = value;
				} else
				{
					if (time < lowTime) lowTime = time;
					if (time > highTime) highTime = time;
					if (value < lowValue) lowValue = value;
					if (value > highValue) highValue = value;
				}
			}
		} else if (isSweep())
		{
			for(int s=0; s<sweepValues.length; s++)
			{
				for(int i=0; i<sweepValues[s].length; i++)
				{
					double time = getTime(i, s);
					double value = sweepValues[s][i];
					if (first)
					{
						first = false;
						lowTime = highTime = time;
						lowValue = highValue = value;
					} else
					{
						if (time < lowTime) lowTime = time;
						if (time > highTime) highTime = time;
						if (value < lowValue) lowValue = value;
						if (value > highValue) highValue = value;
					}
				}
			}
		} else if (isInterval())
		{
			for(int i=0; i<values.length; i++)
			{
				double time = getTime(i);
				double lowVal = values[i];
				double highVal = values[i];
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
