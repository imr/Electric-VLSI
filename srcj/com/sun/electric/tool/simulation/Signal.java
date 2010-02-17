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

	/**
	 * Method to return the Analysis in which this signal resides.
	 * @return the Analysis in which this signal resides.
	 */
	public final Analysis getAnalysis() { return analysis; }

	/**
	 * Method to return the name of this simulation signal.
	 * The name does not include any hierarchical path information: it is just a simple name.
	 * @return the name of this simulation signal.
	 */
	public final String getSignalName() { return signalName; }

	/**
	 * Method to return the context of this simulation signal.
	 * The context is the hierarchical path to the signal, and it usually contains
	 * instance names of cells farther up the hierarchy, all separated by dots.
	 * @return the context of this simulation signal.
	 * If there is no context, this returns null.
	 */
	public final String getSignalContext() { return signalContext; }

	/**
	 * Method to return the full name of this simulation signal.
	 * The full name includes the context, if any.
	 * @return the full name of this simulation signal.
	 */
	public final String getFullName() {
		if (signalContext != null) return signalContext + getAnalysis().getStimuli().getSeparatorChar() + signalName;
		return signalName;
	}


    protected Rectangle2D bounds;
	public double getMinTime() {
		if (bounds == null) calcBounds();
		return bounds.getMinX();
	}
	public double getMaxTime() {
		if (bounds == null) calcBounds();
		return bounds.getMaxX();
	}
	public double getMinValue() {
		if (bounds == null) calcBounds();
		return bounds.getMinY();
	}
	public double getMaxValue() {
		if (bounds == null) calcBounds();
		return bounds.getMaxY();
	}
	protected void calcBounds() {}
}
