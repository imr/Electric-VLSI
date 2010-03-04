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
 * A Signal represents simulation data captured for a particular node
 * over a stretch of time.  Internally, it associates Samples to
 * points in time (measured by a double).  Each Signal also belongs to
 * an Analysis, and has a Name and a Context (which are both Strings).
 *
 * Because the simulation data set may be extremely large, one does
 * not access the data directly through the Signal class.  Instead,
 * one asks for a View of the signal.  Views offer access to the
 * simulation data in various summarized forms.
 *
 * Subsequent enhancements are likely to include the
 * ability to invoke methods on the class while the streaming
 * conversion is taking place; attempts to invoke methods which
 * require data not yet read will simply block until that part of the
 * stream is processed.
 */
public abstract class Signal<SS extends Sample> {

    public Signal(Analysis analysis, String signalName, String signalContext) {
        this.analysis = analysis;
		this.signalName = signalName;
		this.signalContext = signalContext;
		if (analysis!=null) analysis.nameSignal(this, getFullName());
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

    /**
     *  An Approximation is a collection of events indexed by natural
     *  numbers.  An event is an ordered pair of a rational number for
     *  the time and an SS for the value.  All times share a common
     *  denominator.  Times are guaranteed to be monotonic.
     *
     *  The following is true except for rounding errors:
     *
     *    getTime(i)  = getTime(0)  + getTimeNumerator(i)/getTimeDenominator()
     *
     *  The time-distance between events is NOT guaranteed to be
     *  uniform.  However, the instances of Approximation returned by
     *  getApproximation(DDIDDI) <i>do</i> make this guarantee --
     *  in particular, those instances promise that for all x,
     *  getTimeNumerator(i)==i.  Instances returned by other methods
     *  do not offer this guarantee.
     */
    public static interface View<SS extends Sample> {
        /** the number of indices ("events") in this approximation */   int    getNumEvents();
        /** the absolute time of the event in question */               double getTime(int event);
        /** the absolute value of the event in question */              SS     getSample(int event);
        /** the numerator of the time of the specified event */         int    getTimeNumerator(int event);
        /** the common denominator of all times */                      int    getTimeDenominator();
        /** returns the index of the event having the least value */    int    getEventWithMinValue();
        /** returns the index of the event having the greatest value */ int    getEventWithMaxValue();
    }

    /**
     *  Returns a View appropriate for rasterization, including data
     *  from time t0 to time t1, optimized for rasterization on a
     *  display region numPixels wide.  Note that the View returned is
     *  of type View<RangeSample<SS>> -- this means that getSample(i)
     *  will give a CONSERVATIVE approximation of the true value of
     *  the signal between getTime(i) and getTime(i+1).  In other
     *  words, the signal is guaranteed not to exceed
     *  getSample(i).getMax() in that window, but the actual value
     *  returned by getMax() might be much larger than the true
     *  maximum over that range.
     */
    public abstract Signal.View<RangeSample<SS>> getRasterView(double t0, double t1, int numPixels);

    /** Returns a view with all the data, no loss in fidelity. */
    public abstract Signal.View<SS> getExactView();

	public double getMinTime()  { return getExactView().getTime(0); }
	public double getMaxTime()  { return getExactView().getTime(getExactView().getNumEvents()-1); }
	public SS getMinValue() { return getExactView().getSample(getExactView().getEventWithMinValue()); }
	public SS getMaxValue() { return getExactView().getSample(getExactView().getEventWithMaxValue()); }

    protected static class DumbRasterView<SS extends Sample> implements Signal.View<RangeSample<SS>> {
        private final Signal.View<SS> exactView;
        public DumbRasterView(Signal.View<SS> exactView) { this.exactView = exactView; }
        public int getNumEvents() { return exactView.getNumEvents(); }
        public double getTime(int index) { return exactView.getTime(index); }
        public RangeSample<SS> getSample(int index) {
            SS ss = exactView.getSample(index);
            return new RangeSample(ss, ss);
        }
        public int getTimeNumerator(int index) { return exactView.getTimeNumerator(index); }
        public int getTimeDenominator() { return exactView.getTimeDenominator(); }
        public int getEventWithMaxValue() { return exactView.getEventWithMaxValue(); }
        public int getEventWithMinValue() { return exactView.getEventWithMinValue(); }
    }

}
