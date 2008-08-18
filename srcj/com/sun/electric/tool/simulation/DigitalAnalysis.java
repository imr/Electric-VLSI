/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DigitalAnalysis.java
 *
 * Copyright (c) 2007 Sun Microsystems and Static Free Software
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

import java.util.ArrayList;
import java.util.List;

/**
 * Analysis which contains digital signals
 */
public class DigitalAnalysis extends Analysis<DigitalSignal> {
	/** a list of all bussed signals in this Analysis */		    private List<DigitalSignal> allBussedSignals = new ArrayList<DigitalSignal>();

	/**
	 * Constructor for a collection of digital simulation data.
	 * @param sd Stimuli that this analysis is part of.
	 * @param extrapolateToRight true to draw the last value to the right
	 * (useful for IRSIM and other digital simulations).
	 * False to stop drawing signals after their last value
	 * (useful for Spice and other analog simulations).
	 */
    public DigitalAnalysis(Stimuli sd, boolean extrapolateToRight) {
        super(sd, ANALYSIS_SIGNALS, extrapolateToRight);
    }
    
    @Override
    public void finished() {
        super.finished();
        for (DigitalSignal s : allBussedSignals)
            s.finished();
    }
    
    @Override
	public boolean isAnalog() { return false; }
    
	/**
	 * Method to get the list of bussed signals in this Simulation Data object.
	 * @return a List of signals.
	 */
	public List<DigitalSignal> getBussedSignals() { return allBussedSignals; }

}
