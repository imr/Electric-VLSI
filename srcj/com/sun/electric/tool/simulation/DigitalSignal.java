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
public class DigitalSignal extends BTreeSignal<DigitalSample> {

	/** the DigitalAnalysis object in which this DigitalSignal resides. */		private DigitalAnalysis an;
	/** a list of signals on this bussed signal */					private List<DigitalSignal> bussedSignals;
	/** the number of busses that reference this signal */			private int busCount;

	/**
	 * Constructor for a digital signal.
	 * @param an the DigitalAnalysis object in which this signal will reside.
	 */
	public DigitalSignal(DigitalAnalysis an, String signalName, String signalContext) {
        super(an, signalName, signalContext, BTreeSignal.getTree(DigitalSample.unboxer));
		this.an = an;
		an.addSignal(this);
	}

    public void reset() {
        // temporary workaround
    }

    public void addSample(double time, DigitalSample sample) {
        if (sample==null) return; // temporary workaround
        super.addSample(time, sample);
    }

	/**
	 * Method to request that this signal be a bus.
	 * Builds the necessary data structures to hold bus information.
	 */
	public void buildBussedSignalList() {
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
	public void clearBussedSignalList() {
		for(DigitalSignal sig : bussedSignals)
			sig.busCount--;
		bussedSignals.clear();
	}

	/**
	 * Method to add a signal to this bus signal.
	 * @param ws a single-wire signal to be added to this bus signal.
	 */
	public void addToBussedSignalList(DigitalSignal ws) {
		bussedSignals.add(ws);
		ws.busCount++;
	}

	/**
	 * Method to tell whether this signal is part of a bus.
	 * @return true if this signal is part of a bus.
	 */
	public boolean isInBus() { return busCount != 0; }

	public double getTime(int index) {
        return getExactView().getTime(index);
	}

	public int getState(int index) {
        DigitalSample ds = getExactView().getSample(index);
        if (ds.isLogic0()) return Stimuli.LOGIC_LOW;
        if (ds.isLogic1()) return Stimuli.LOGIC_HIGH;
        if (ds.isLogicX()) return Stimuli.LOGIC_X;
        if (ds.isLogicZ()) return Stimuli.LOGIC_Z;
        throw new RuntimeException("ack!");
    }

}
