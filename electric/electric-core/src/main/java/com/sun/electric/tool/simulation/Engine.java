/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Engine.java
 *
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.electric.tool.io.FileType;
import java.io.File;
import java.io.IOException;
import java.net.URL;


/**
 * This is a Simulation Engine (such as IRSIM).
 */
public abstract class Engine
{
    /**
     * Returns FileType of vectors file.
     */
    public abstract FileType getVectorsFileType();
    
    /**
     * Returns current Stimuli.
     */
    public abstract Stimuli getStimuli();

    
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
	 * @return true if stimuli were deleted.
	 */
	public boolean removeSelectedStimuli() { return false; }

	/**
	 * Method to remove all stimuli from the simulation.
	 */
	public void removeAllStimuli() {}

	/**
	 * Method to save the current stimuli information to disk.
     * @param stimuliFile file to save stimuli information
	 */
	public void saveStimuli(File stimuliFile) throws IOException {}

	/**
	 * Method to restore the current stimuli information from URL.
     * @param stimuliURL URL of stimuli information
	 */
	public void restoreStimuli(URL stimuliURL) throws IOException {}
}
