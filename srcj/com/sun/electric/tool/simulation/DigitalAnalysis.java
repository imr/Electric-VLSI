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
import java.util.HashMap;
import java.awt.geom.Rectangle2D;

/**
 * Analysis which contains digital signals
 */
public class DigitalAnalysis extends Analysis<Signal<DigitalSample>> {
	/** a list of all bussed signals in this Analysis */		    private List<Signal<DigitalSample>> allBussedSignals = new ArrayList<Signal<DigitalSample>>();

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
	public boolean isAnalog() { return false; }
    
	/**
	 * Method to get the list of bussed signals in this Simulation Data object.
	 * @return a List of signals.
	 */
	public List<Signal<DigitalSample>> getBussedSignals() { return allBussedSignals; }

    private static HashMap<Signal,Integer> busCount = new HashMap<Signal,Integer>();
    private static HashMap<Signal,List<Signal<DigitalSample>>> bussedSignals = new HashMap<Signal,List<Signal<DigitalSample>>>();

	/**
	 * Method to request that this signal be a bus.
	 * Builds the necessary data structures to hold bus information.
	 */
	public void buildBussedSignalList(Signal<DigitalSample> ds) {
        bussedSignals.put(ds, new ArrayList<Signal<DigitalSample>>());
		getBussedSignals().add(ds);
	}

	/**
	 * Method to request that this bussed signal be cleared of all signals on it.
	 */
	public static void clearBussedSignalList(Signal<DigitalSample> ds) {
		for(Signal<DigitalSample> ws : bussedSignals.get(ds)) {
            Integer i = busCount.get(ws);
            int ii = i==null ? 0 : i.intValue();
            ii++;
            busCount.put(ws, ii);
        }
		bussedSignals.get(ds).clear();
	}

	/**
	 * Method to add a signal to this bus signal.
	 * @param ws a single-wire signal to be added to this bus signal.
	 */
	public static void addToBussedSignalList(Signal<DigitalSample> ds, Signal<DigitalSample> ws) {
		bussedSignals.get(ds).add(ws);
		Integer i = busCount.get(ws);
        int ii = i==null ? 0 : i.intValue();
        ii++;
        busCount.put(ws, ii);
	}

	/**
	 * Method to tell whether this signal is part of a bus.
	 * @return true if this signal is part of a bus.
	 */
	public static boolean isInBus(Signal<DigitalSample> ds) {
		Integer i = busCount.get(ds);
        return i!=0 && i.intValue()!=0;
    }

	/**
	 * Method to return a List of signals on this bus signal.
	 * Each entry in the List points to another simulation signal that is on this bus.
	 * @return a List of signals on this bus signal.
	 */
	public static List<Signal<DigitalSample>> getBussedSignals(Signal<DigitalSample> ds) {
        return bussedSignals.get(ds);
    }

}
