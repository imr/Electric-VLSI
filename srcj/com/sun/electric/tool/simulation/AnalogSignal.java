/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AnalogSignal.java
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

/**
 * Class to define an analog signal in the simulation waveform window.
 */
public final class AnalogSignal extends ScalarSignal implements MultiSweepSignal {
	/** the Analysis object in which this DigitalSignal resides. */		private final AnalogAnalysis an;
	/** index of this signal in its AnalogAnalysis */					private final int index;

	/**
	 * Constructor for an analog signal.
	 * @param an the AnalogAnalysis object in which this signal will reside.
	 */
	public AnalogSignal(AnalogAnalysis an, String signalName, String signalContext) {
        super(an, signalName, signalContext);
		this.an = an;
		index = an.getSignals().size();
		an.addSignal(this);
	}

	/**
	 * Method to return the index of this AnalogSignal in its AnalogAnalysis.
	 * @return the index of this AnalogSignal in its AnalogAnalysis.
	 */
	public int getIndexInAnalysis() { return index; }

	public Signal getWaveform(int sweep) { return this; }
    public Signal<ScalarSample> getSweep(int sweep) { return this; }
	public int getNumSweeps() { return 1; }

}
