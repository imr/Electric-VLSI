/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Engine.java
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
 * This is a Simulation Engine (such as IRSIM).
 */
public class Engine
{
	/**
	 * Method to reload the circuit data.
	 */
	public void refresh() {}

	/**
	 * Method to update the simulation (because some stimuli have changed).
	 */
	public void update() {}

	/**
	 * Method to set the currently-selected signal high at the current time.
	 */
	public void setSignalHigh() {}

	/**
	 * Method to set the currently-selected signal low at the current time.
	 */
	public void setSignalLow() {}

	/**
	 * Method to set the currently-selected signal undefined at the current time.
	 */
	public void setSignalX() {}

	/**
	 * Method to set the currently-selected signal to have a clock with a given period.
	 */
	public void setClock(double period) {}

	/**
	 * Method to show information about the currently-selected signal.
	 */
	public void showSignalInfo() {}

	/**
	 * Method to remove all stimuli from the currently-selected signal.
	 */
	public void removeStimuliFromSignal() {}

	/**
	 * Method to remove the selected stimuli.
	 */
	public void removeSelectedStimuli() {}

	/**
	 * Method to remove all stimuli from the simulation.
	 */
	public void removeAllStimuli() {}

	/**
	 * Method to save the current stimuli information to disk.
	 */
	public void saveStimuli() {}

	/**
	 * Method to restore the current stimuli information from disk.
	 */
	public void restoreStimuli() {}
}
