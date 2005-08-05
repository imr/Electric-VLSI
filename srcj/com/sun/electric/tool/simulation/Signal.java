/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Signal.java
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class to define a signal in the simulation waveform window.
 * This is a superclass for specific signal types: digital or analog.
 */
public class Signal
{
	/** the name of this signal */									private String signalName;
	/** the context of this signal (qualifications to name) */		private String signalContext;
	/** the Stimuli object in which this Signal resides. */			protected Stimuli sd;
	/** true to use the common time array in the Stimuli */			private boolean useCommonTime;
	/** the range of values in the X and Y axes */					protected Rectangle2D bounds;
	/** true if the bounds data is valid */							protected boolean boundsCurrent;
	/** an array of time values on this signal (if not common) */	private double [] time;
	/** an array of control points on this signal */				private double [] controlPoints;
	/** a list of signals on this bussed signal */					private List bussedSignals;
	/** the number of busses that reference this signal */			private int busCount;
	/** application-specific object associated with this signal */	private Object appObject;
	/** application-specific flags for this signal */				public int flags;
	/** used only in the Verilog reader */							public List tempList;

	/**
	 * Constructor for a simulation signal.
	 * @param sd the Simulation Data object in which this signal will reside.
	 */
	protected Signal(Stimuli sd)
	{
		this.sd = sd;
		useCommonTime = true;
		boundsCurrent = false;
		busCount = 0;
		controlPoints = null;
		if (sd != null) sd.getSignals().add(this);
	}

	/**
	 * Method to set the name of this simulation signal.
	 * The name does not include any hierarchical path information: it is just a simple name.
	 * @param signalName the name of this simulation signal.
	 */
	public void setSignalName(String signalName) { this.signalName = signalName; }

	/**
	 * Method to return the name of this simulation signal.
	 * The name does not include any hierarchical path information: it is just a simple name.
	 * @return the name of this simulation signal.
	 */
	public String getSignalName() { return signalName; }

	/**
	 * Method to return the context of this simulation signal.
	 * The context is the hierarchical path to the signal, and it usually contains
	 * instance names of cells farther up the hierarchy, all separated by dots.
	 * @param signalContext the context of this simulation signal.
	 */
	public void setSignalContext(String signalContext) { this.signalContext = signalContext; }

	/**
	 * Method to return the context of this simulation signal.
	 * The context is the hierarchical path to the signal, and it usually contains
	 * instance names of cells farther up the hierarchy, all separated by dots.
	 * @return the context of this simulation signal.
	 * If there is no context, this returns null.
	 */
	public String getSignalContext() { return signalContext; }

	/**
	 * Method to return the full name of this simulation signal.
	 * The full name includes the context, if any.
	 * @return the full name of this simulation signal.
	 */
	public String getFullName()
	{
		if (signalContext != null) return signalContext + sd.getSeparatorChar() + signalName;
		return signalName;
	}

	/**
	 * Method to return the number of events in this signal.
	 * This is the number of events along the horizontal axis, usually "time".
	 * This superclass method must be overridden by a subclass that actually has data.
	 * @return the number of events in this signal.
	 */
	public int getNumEvents() { return 0; }

	/**
	 * Method to request that this signal be a bus.
	 * Builds the necessary data structures to hold bus information.
	 */
	public void buildBussedSignalList()
	{
		bussedSignals = new ArrayList();
if (sd == null) System.out.println("SD IS NULL!!!!!!!");
		sd.getBussedSignals().add(this);
	}

	/**
	 * Method to return a List of signals on this bus signal.
	 * Each entry in the List points to another simulation signal that is on this bus.
	 * @return a List of signals on this bus signal.
	 */
	public List getBussedSignals() { return bussedSignals; }

	/**
	 * Method to request that this bussed signal be cleared of all signals on it.
	 */
	public void clearBussedSignalList()
	{
		for(Iterator it = bussedSignals.iterator(); it.hasNext(); )
		{
			Signal sig = (Signal)it.next();
			sig.busCount--;
		}
		bussedSignals.clear();
	}

	/**
	 * Method to add a signal to this bus signal.
	 * @param ws a single-wire signal to be added to this bus signal.
	 */
	public void addToBussedSignalList(Signal ws)
	{
		bussedSignals.add(ws);
		ws.busCount++;
	}

	/**
	 * Method to tell whether this signal is part of a bus.
	 * @return true if this signal is part of a bus.
	 */
	public boolean isInBus() { return busCount != 0; }

	/**
	 * Method to return a list of control points associated with this signal.
	 * Control points are places where the user has added stimuli to the signal (set a level or strength).
	 * These points can be selected for change of the stimuli.
	 * @return an array of times where there are control points.
	 * Null if no control points are defined.
	 */
	public double [] getControlPoints() { return controlPoints; }

	/**
	 * Method to clear the list of control points associated with this signal.
	 * Control points are places where the user has added stimuli to the signal (set a level or strength).
	 * These points can be selected for change of the stimuli.
	 */
	public void clearControlPoints() { controlPoints = null; }

	/**
	 * Method to add a new control point to the list on this signal.
	 * Control points are places where the user has added stimuli to the signal (set a level or strength).
	 * These points can be selected for change of the stimuli.
	 * @param time the time of the new control point.
	 */
	public void addControlPoint(double time)
	{
		if (controlPoints == null)
		{
			controlPoints = new double[1];
			controlPoints[0] = time;
		} else
		{
			// see if it is in the list already
			for(int i=0; i<controlPoints.length; i++)
				if (controlPoints[i] == time) return;

			// extend the list
			double [] newCP = new double[controlPoints.length + 1];
			for(int i=0; i<controlPoints.length; i++)
				newCP[i] = controlPoints[i];
			newCP[controlPoints.length] = time;
			controlPoints = newCP;
		}
	}

	/**
	 * Method to remove control points the list on this signal.
	 * Control points are places where the user has added stimuli to the signal (set a level or strength).
	 * These points can be selected for change of the stimuli.
	 * @param time the time of the control point to delete.
	 */
	public void removeControlPoint(double time)
	{
		if (controlPoints == null) return;

		// see if it is in the list already
		boolean found = false;
		for(int i=0; i<controlPoints.length; i++)
			if (controlPoints[i] == time) { found = true;   break; }
		if (!found) return;

		// shrink the list
		double [] newCP = new double[controlPoints.length - 1];
		int j = 0;
		for(int i=0; i<controlPoints.length; i++)
		{
			if (controlPoints[i] != time)
				newCP[j++] = controlPoints[i];
		}
		controlPoints = newCP;
	}

	/**
	 * Method to set an application-specific object pointer on this Signal.
	 * @param appObject an application-specific object pointer on this Signal.
	 */
	public void setAppObject(Object appObject) { this.appObject = appObject; }

	/**
	 * Method to get an application-specific object pointer on this Signal.
	 * @return the application-specific object pointer on this Signal.
	 */
	public Object getAppObject() { return appObject; }

	/**
	 * Method to build a time vector for this signal.
	 * Signals can have their own time information, or they can use a "common time" array
	 * that is part of the simulation data.
	 * If using common time, then each signal must have the same number of entries, and
	 * each entry must be at the same time as the corresponding entries in other signals.
	 * Using common time saves memory, because the time information does not have to be
	 * stored with each signal.
	 * This method requests that the signal have its own time array, and not use common time data.
	 * @param numEvents the number of events on this signal (the length of the time array).
	 */
	public void buildTime(int numEvents) { useCommonTime = false;   time = new double[numEvents]; }

	/**
	 * Method to return the value of time for a given event on this signal.
	 * Depending on whether common time data is being used, the time information is
	 * found on this signal or on the overall simulation data.
	 * @param index the event being querried (0-based).
	 * @return the value of time at that event.
	 */
	public double getTime(int index)
	{
		if (useCommonTime) return sd.getCommonTimeArray()[index];
		return time[index];
	}

	/**
	 * Method to return the value of time for a given event on this signal.
	 * Depending on whether common time data is being used, the time information is
	 * found on this signal or on the overall simulation data.
	 * @param index the event being querried (0-based).
	 * @return the value of time at that event.
	 */
	public double getTime(int index, int sweep)
	{
		if (useCommonTime)
		{
			double [] sct = (double [])sd.getCommonTimeArray(sweep);
			return sct[index];
		}
		return time[index];
	}

	/**
	 * Method to return the time vector for this signal.
	 * The vector is only valid if this signal is NOT using common time.
	 * Signals can have their own time information, or they can use a "common time" array
	 * that is part of the simulation data.
	 * If using common time, then each signal must have the same number of entries, and
	 * each entry must be at the same time as the corresponding entries in other signals.
	 * @return the time array for this signal.
	 * Returns null if this signal uses common time.
	 */
	public double [] getTimeVector() { return time; }

	/**
	 * Method to set the time vector for this signal.
	 * Overrides any previous time vector that may be on this signal.
	 * Signals can have their own time information, or they can use a "common time" array
	 * that is part of the simulation data.
	 * If using common time, then each signal must have the same number of entries, and
	 * each entry must be at the same time as the corresponding entries in other signals.
	 * @param time a new time vector for this signal.
	 */
	public void setTimeVector(double [] time) { useCommonTime = false;   boundsCurrent = false;   this.time = time; }

	/**
	 * Method to set an individual time entry for this signal.
	 * Only applies if common time is NOT being used for this signal.
	 * Signals can have their own time information, or they can use a "common time" array
	 * that is part of the simulation data.
	 * If using common time, then each signal must have the same number of entries, and
	 * each entry must be at the same time as the corresponding entries in other signals.
	 * @param entry the entry in the event array of this signal (0-based).
	 * @param t the new value of time at this event.
	 */
	public void setTime(int entry, double t) { boundsCurrent = false;   time[entry] = t; }

	/**
	 * Method to compute the time and value bounds of this simulation signal.
	 * @return a Rectangle2D that has time bounds in the X part and
	 * value bounds in the Y part.
	 * For digital signals, the Y part is simply 0 to 1.
	 */
	public Rectangle2D getBounds()
	{
		if (!boundsCurrent)
		{
			calcBounds();
			boundsCurrent = true;
		}
		return bounds;
	}

	protected void calcBounds() {}
}
