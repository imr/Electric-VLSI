/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DigitalSignal.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to define a digital signal in the simulation waveform window.
 */
public class DigitalSignal extends Signal
{
	/** the DigitalAnalysis object in which this DigitalSignal resides. */		private DigitalAnalysis an;
	/** a list of signals on this bussed signal */					private List<DigitalSignal> bussedSignals;
	/** the number of busses that reference this signal */			private int busCount;
	/** an array of time values on this signal (null to use common time) */	private double [] time;
	private int [] state;

	/**
	 * Constructor for a digital signal.
	 * @param an the DigitalAnalysis object in which this signal will reside.
	 */
	public DigitalSignal(DigitalAnalysis an) {
		this.an = an;
		an.addSignal(this);
	}

	@Override
	public void finished()
	{
		super.finished();
		if (bussedSignals != null)
		{
			for (Signal s : bussedSignals)
				s.finished();
			bussedSignals.clear();
		}
		busCount = 0;
		time = null;
		state = null;
	}

	/**
	 * Method to return the DigitalAnalysis in which this signal resides.
	 * @return the DigitalAnalysis in which this signal resides.
	 */
	@Override
	public DigitalAnalysis getAnalysis() { return an; }

	/**
	 * Method to request that this signal be a bus.
	 * Builds the necessary data structures to hold bus information.
	 */
	public void buildBussedSignalList()
	{
		bussedSignals = new ArrayList<DigitalSignal>();
		an.getBussedSignals().add(this);
	}

	/**
	 * Method to return a List of signals on this bus signal.
	 * Each entry in the List points to another simulation signal that is on this bus.
	 * @return a List of signals on this bus signal.
	 */
	public List<DigitalSignal> getBussedSignals() { return bussedSignals; }

	/**
	 * Method to request that this bussed signal be cleared of all signals on it.
	 */
	public void clearBussedSignalList()
	{
		for(DigitalSignal sig : bussedSignals)
			sig.busCount--;
		bussedSignals.clear();
	}

	/**
	 * Method to add a signal to this bus signal.
	 * @param ws a single-wire signal to be added to this bus signal.
	 */
	public void addToBussedSignalList(DigitalSignal ws)
	{
		bussedSignals.add(ws);
		ws.busCount++;
	}

	/**
	 * Method to tell whether this signal is part of a bus.
	 * @return true if this signal is part of a bus.
	 */
	public boolean isInBus() { return busCount != 0; }

	// time

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

	// state values

	/**
	 * Method to initialize this simulation signal with a specified number of events.
	 * Allocates an array to hold those events.
	 * @param numEvents the number of events in this signal.
	 */
	public void buildState(int numEvents) { state = new int[numEvents]; }

	/**
	 * Method to set the state of this signal at a given event.
	 * @param index the event index (0-based).
	 * @param st the state of the signal at that event.
	 */
	public void setState(int index, int st) { state[index] = st;   bounds = null; }

	/**
	 * Method to get the state of this signal at a given event.
	 * @param index the event index (0-based).
	 * @return the state of the signal at that event.
	 */
	public int getState(int index) { return state[index]; }

	/**
	 * Method to return the state information for all events in this signal.
	 * @return the state array for this signal.
	 */
	public int [] getStateVector() { return state; }

	/**
	 * Method to set the state information for all events in this signal.
	 * @param state an array of state information for every event on this signal.
	 */
	public void setStateVector(int [] state)
	{
		this.state = state;
		bounds = null;
	}

	/**
	 * Method to return the number of events in this signal.
	 * This is the number of events along the horizontal axis, usually "time".
	 * @return the number of events in this signal.
	 */
	public int getNumEvents()
	{
		if (state == null) return 0;
		return state.length;
	}

	/**
	 * Method to compute the low and high range of time value on this signal.
	 * The result is stored in the "bounds", "leftEdge", and "rightEdge" field variables.
	 */
	protected void calcBounds()
	{
		boolean first = true;
		double lowTime = 0, highTime = 0;
		if (state != null)
		{
			for(int i=0; i<state.length; i++)
			{
				double time = getTime(i);
				if (i == 0) leftEdge = time; else
					if (i == state.length-1) rightEdge = time;
				if (first)
				{
					first = false;
					lowTime = highTime = time;
				} else
				{
					if (time < lowTime) lowTime = time;
					if (time > highTime) highTime = time;
				}
			}
		}
		bounds = new Rectangle2D.Double(lowTime, 0, highTime-lowTime, 1);
	}
}
