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
 * Class to define a digital signal in the simulation waveform window.
 */
public class DigitalSignal extends TimedSignal
{
	private int [] state;

	/**
	 * Constructor for a digital signal.
	 * @param an the Analysis object in which this signal will reside.
	 */
	public DigitalSignal(Analysis an) { super(an); }

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
	 * The result is stored in the "bounds" field variable.
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
