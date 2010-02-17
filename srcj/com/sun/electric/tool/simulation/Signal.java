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
 * Class to define the basic parts of a signal in the simulation waveform window.
 * This is a superclass for specific signal types: Measurement and TimedSignal
 * (which has under it DigitalSignal and AnalogSignal).
 */
public abstract class Signal {

    public Signal(Analysis analysis, String signalName, String signalContext) {
        this.analysis = analysis;
		this.signalName = signalName;
		this.signalContext = signalContext;
		getAnalysis().nameSignal(this, getFullName());
    }

	/** the name of this signal */									private final String signalName;
	/** the context of this signal (qualifications to name) */		private final String signalContext;
    /** the Analysis to which this signal belongs */                private final Analysis analysis;

	/** the Analysis in which this signal resides. */
	public final Analysis getAnalysis() { return analysis; }

	/** The name of this simulation signal, not including hierarchical path information */
	public final String getSignalName() { return signalName; }

	/** Return the context (hierarchical path information) of the signal, or null if none */
	public final String getSignalContext() { return signalContext; }

	/** Return the full name (context+signalName) */
	public final String getFullName() { return signalContext==null ? signalName : signalContext + getAnalysis().getStimuli().getSeparatorChar() + signalName; }

	public abstract double getMinTime();
	public abstract double getMaxTime();
	public abstract double getMinValue();
	public abstract double getMaxValue();

}
