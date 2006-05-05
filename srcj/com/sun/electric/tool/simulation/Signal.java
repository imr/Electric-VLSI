/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Signal.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
import java.util.List;

/**
 * Class to define the basic parts of a signal in the simulation waveform window.
 * This is a superclass for specific signal types: Measurement and TimedSignal
 * (which has under it DigitalSignal and AnalogSignal).
 */
public class Signal
{
	/** the name of this signal */									private String signalName;
	/** the context of this signal (qualifications to name) */		private String signalContext;
	/** the Analysis object in which this Signal resides. */		protected Analysis an;
	/** a list of signals on this bussed signal */					private List<Signal> bussedSignals;
	/** the number of busses that reference this signal */			private int busCount;
	/** the range of values in the X and Y axes */					protected Rectangle2D bounds;

	/**
	 * Constructor for a simulation signal.
	 * @param an the Simulation Data object in which this signal will reside.
	 */
	protected Signal(Analysis an)
	{
		this.an = an;
		bounds = null;
		busCount = 0;
		if (an != null) an.addSignal(this);
	}

    public void finished()
    {
        System.out.println("Signal finish");
        for (Signal s : bussedSignals)
            s.finished();
        bussedSignals.clear();
    }

	/**
	 * Method to return the Analysis in which this signal resides.
	 * @return the Analysis in which this signal resides.
	 */
	public Analysis getAnalysis() { return an; }

	/**
	 * Method to set the name of this simulation signal.
	 * The name does not include any hierarchical path information: it is just a simple name.
	 * @param signalName the name of this simulation signal.
	 */
	public void setSignalName(String signalName)
	{
		this.signalName = signalName;
		an.nameSignal(this, getFullName());
	}

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
		if (signalContext != null) return signalContext + an.getStimuli().getSeparatorChar() + signalName;
		return signalName;
	}

	/**
	 * Method to request that this signal be a bus.
	 * Builds the necessary data structures to hold bus information.
	 */
	public void buildBussedSignalList()
	{
		bussedSignals = new ArrayList<Signal>();
		an.getBussedSignals().add(this);
	}

	/**
	 * Method to return a List of signals on this bus signal.
	 * Each entry in the List points to another simulation signal that is on this bus.
	 * @return a List of signals on this bus signal.
	 */
	public List<Signal> getBussedSignals() { return bussedSignals; }

	/**
	 * Method to request that this bussed signal be cleared of all signals on it.
	 */
	public void clearBussedSignalList()
	{
		for(Signal sig : bussedSignals)
			sig.busCount--;
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
	 * Note that the control point structures are not stored on this object because that would consume
	 * too much memory for such little-used information.
	 * Instead, the control point data is stored in HashMaps on the Stimuli.
	 * @return an array of times where there are control points.
	 * Null if no control points are defined.
	 */
	public Double [] getControlPoints() { return an.getStimuli().getControlPoints(this); }

	/**
	 * Method to clear the list of control points associated with this signal.
	 * Control points are places where the user has added stimuli to the signal (set a level or strength).
	 * These points can be selected for change of the stimuli.
	 * Note that the control point structures are not stored on this object because that would consume
	 * too much memory for such little-used information.
	 * Instead, the control point data is stored in HashMaps on the Stimuli.
	 */
	public void clearControlPoints() { an.getStimuli().clearControlPoints(this); }

	/**
	 * Method to add a new control point to the list on this signal.
	 * Control points are places where the user has added stimuli to the signal (set a level or strength).
	 * These points can be selected for change of the stimuli.
	 * Note that the control point structures are not stored on this object because that would consume
	 * too much memory for such little-used information.
	 * Instead, the control point data is stored in HashMaps on the Stimuli.
	 * @param time the time of the new control point.
	 */
	public void addControlPoint(double time)
	{
		an.getStimuli().addControlPoint(this, time);
	}

	/**
	 * Method to remove control points the list on this signal.
	 * Control points are places where the user has added stimuli to the signal (set a level or strength).
	 * These points can be selected for change of the stimuli.
	 * Note that the control point structures are not stored on this object because that would consume
	 * too much memory for such little-used information.
	 * Instead, the control point data is stored in HashMaps on the Stimuli.
	 * @param time the time of the control point to delete.
	 */
	public void removeControlPoint(double time)
	{
		an.getStimuli().removeControlPoint(this, time);
	}

	/**
	 * Method to return the number of events in this signal.
	 * This is the number of events along the horizontal axis, usually "time".
	 * This superclass method must be overridden by a subclass that actually has data.
	 * @return the number of events in this signal.
	 */
	public int getNumEvents() { return 0; }

	/**
	 * Method to compute the time and value bounds of this simulation signal.
	 * @return a Rectangle2D that has time bounds in the X part and
	 * value bounds in the Y part.
	 * For digital signals, the Y part is simply 0 to 1.
	 */
	public Rectangle2D getBounds()
	{
		if (bounds == null)
		{
			calcBounds();
		}
		return bounds;
	}

	protected void calcBounds() {}
}
