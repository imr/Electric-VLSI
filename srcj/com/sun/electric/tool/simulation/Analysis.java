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
    /** group of signals from extracted netlist of same net */  HashMap<String,List<S>> signalGroup = new HashMap<String,List<S>>();
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

    public String getTitle() { return title; }

    /**
	 * Method to get the list of signals in this Simulation Data object.
	 * @return a List of signals.
	 */
	public List<S> getSignals() { return signals; }
	public boolean hasSignal(Signal s) { return signals.contains(s); }

    /**
     * Get a list of signals that are from the same network.
     * Extracted nets are the original name + delimiter + some junk
     * @param ws the signal
     * @return a list of signals
     */
    public List<S> getSignalsFromExtractedNet(Signal ws) {
        String sigName = ws.getFullName();
        if (sigName == null) return new ArrayList<S>();
        sigName = TextUtils.canonicString(sigName);
        sigName = ws.getBaseNameFromExtractedNet(sigName);
        return signalGroup.get(sigName);
    }

	/**
	 * Method to quickly return the signal that corresponds to a given Network name.
	 * Not all names may be found (because of name mangling, which this method does not handle).
	 * But the lookup is faster than "findSignalForNetwork".
	 * @param netName the Network name to find.
	 * @return the Signal that corresponds with the Network.
	 * Returns null if none can be found.
	 */
	public S findSignalForNetworkQuickly(String netName)
	{
		String lookupName = TextUtils.canonicString(netName);
		S sSig = signalNames.get(lookupName);
		return sSig;
	}

	/**
	 * Method to return the signal that corresponds to a given Network name.
	 * @param netName the Network name to find.
	 * @return the Signal that corresponds with the Network.
	 * Returns null if none can be found.
	 */
	public S findSignalForNetwork(String netName)
	{
		// look at all signal names in the cell
		for(Iterator<S> it = getSignals().iterator(); it.hasNext(); )
		{
			S sSig = it.next();

			String signalName = sSig.getFullName();
			if (netName.equalsIgnoreCase(signalName)) return sSig;

			// if the signal name has underscores, see if all alphabetic characters match
			if (signalName.length() + 1 == netName.length() && netName.charAt(signalName.length()) == ']')
			{
				signalName += "_";
			}
			if (signalName.length() == netName.length() && signalName.indexOf('_') >= 0)
			{
				boolean matches = true;
				for(int i=0; i<signalName.length(); i++)
				{
					char sigChar = signalName.charAt(i);
					char netChar = netName.charAt(i);
					if (TextUtils.isLetterOrDigit(sigChar) != TextUtils.isLetterOrDigit(netChar))
					{
						matches = false;
						break;
					}
					if (TextUtils.isLetterOrDigit(sigChar) &&
						TextUtils.canonicChar(sigChar) != TextUtils.canonicChar(netChar))
					{
						matches = false;
						break;
					}
				}
				if (matches) return sSig;
			}
		}
		return null;
	}
}
