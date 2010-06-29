/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Analysis.java
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

import com.sun.electric.database.text.TextUtils;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Class to define a set of simulation data.
 * This class encapsulates all of the simulation data that is displayed in a waveform window.
 * It includes the labels and values.
 * It can handle digital, analog, and many variations (intervals, sweeps).
 */
public class Analysis<S extends Signal> {


	/** a list of all signals in this Analysis */				List<S> signals = new ArrayList<S>();
	/** a map of all signal names in this Analysis */			HashMap<String,S> signalNames = new HashMap<String,S>();
    String title;

    /**
	 * Constructor for a collection of simulation data.
	 * @param sd Stimuli that this analysis is part of.
	 * @param type the type of this analysis.
	 * @param extrapolateToRight true to draw the last value to the right
	 * (useful for IRSIM and other digital simulations).
	 * False to stop drawing signals after their last value
	 * (useful for Spice and other analog simulations).
	 */
	public Analysis(Stimuli sd, String title, boolean extrapolateToRight) {
        this.title = title;
		sd.addAnalysis(this);
	}

    public String toString() { return title; }

    /**
	 * Method to get the list of signals in this Simulation Data object.
	 * @return a List of signals.
	 */
	public List<S> getSignals() { return signals; }
	public boolean hasSignal(Signal s) { return signals.contains(s); }
	public S get(String netName) { return signalNames.get(netName); }


}
