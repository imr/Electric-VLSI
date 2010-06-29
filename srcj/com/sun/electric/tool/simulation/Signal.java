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
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.user.waveform.*;
import com.sun.electric.tool.user.waveform.Panel.WaveSelection;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import com.sun.electric.database.geometry.PolyBase;
import java.util.*;

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

    public Signal(HashMap<String,Signal> analysis, Stimuli sd, String signalName, String signalContext) {
		this.signalName = signalName;
		this.signalContext = signalContext;
		this.extrapolateToRight = true;
        this.analysisTitle = analysis==null ? "SIGNALS" : analysis.toString();
        this.fullSignalName = signalContext==null
            ? signalName
            : signalContext + (analysis==null ? '.' : sd.getSeparatorChar()) + signalName;
        this.stimuli = sd;
        if (analysis!=null) {
            String name = TextUtils.canonicString(fullSignalName);
            // simulators may strip off last "_"
            if (name.indexOf('_') >= 0 && !name.endsWith("_"))
                analysis.put(name + "_", this);
            else
                analysis.put(name, this);
        }
    }

	/** the name of this signal */									private final String signalName;
	/** the context of this signal (qualifications to name) */		private final String signalContext;
	/** the context of this signal (qualifications to name) */		private final String fullSignalName;
    /** the HashMap<String,Signal> to which this signal belongs */                private final String analysisTitle;
    /** the extrapolateToRight setting of the HashMap<String,Signal> */           private final boolean extrapolateToRight;
    /** the stimuli of the HashMap<String,Signal> */                              private final Stimuli stimuli;

    // methods relocated from other classes
    public void clearControlPoints() { stimuli.clearControlPoints(this); }
    public void removeControlPoint(double time) { stimuli.removeControlPoint(this, time); }
    public void addControlPoint(double time) { stimuli.addControlPoint(this, time); }
    public Double[] getControlPoints() { return stimuli.getControlPoints(this); }
    public boolean extrapolateValues() { return extrapolateToRight; }
	public final String getAnalysisTitle() { return analysisTitle; }

	/** The name of this simulation signal, not including hierarchical path information */
	public final String getSignalName() { return signalName; }

	/** Return the context (hierarchical path information) of the signal, or null if none */
	public final String getSignalContext() { return signalContext; }

	/** Return the full name (context+signalName) */
	public final String getFullName() { return fullSignalName; }

    /**
     *  A View is a collection of events indexed by natural
     *  numbers.  An event is an ordered pair of a rational number for
     *  the time and an SS for the value.  All times share a common
     *  denominator.  Times are guaranteed to be monotonic.
     *
     *  The following is true except for rounding errors:
     *
     *    getTime(i)  = getTime(0)  + getTimeNumerator(i)/getTimeDenominator()
     *
     *  The time-distance between events is NOT guaranteed to be
     *  uniform.  However, the instances of View returned by
     *  getApproximation(DDIDDI) <i>do</i> make this guarantee --
     *  in particular, those instances promise that for all x,
     *  getTimeNumerator(i)==i.  Instances returned by other methods
     *  do not offer this guarantee.
     */
    public static interface View<SS extends Sample> {
        /** the number of indices ("events") in this view */            int    getNumEvents();
        /** the absolute time of the event in question */               double getTime(int event);
        /** the absolute value of the event in question */              SS     getSample(int event);
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
     *
     *  If extrapolate is true, the resulting data is guaranteed to
     *  have exactly one data point less than or equal to t0 and
     *  exactly one greater than or equal to t1.
     */
    public abstract Signal.View<RangeSample<SS>> getRasterView(double t0, double t1, int numPixels, boolean extrapolate);

    /** Returns a view with all the data, no loss in fidelity. */
    public abstract Signal.View<SS> getExactView();

	public abstract double getMinTime();
	public abstract double getMaxTime();

    /**
     * There are a lot of methods which will return null only if the
     * signal has no samples in it whatsoever; this method can be used
     * to check for that case just once at the top of a function.
     */
    public abstract boolean isEmpty();

    public abstract void plot(Panel panel, Graphics g, WaveSignal ws, Color light, List<PolyBase> forPs,
                              Rectangle2D bounds, List<WaveSelection> selectedObjects);

    public String getBaseNameFromExtractedNet(String signalFullName) {
        String delim = stimuli.getNetDelimiter();
        int hashPos = signalFullName.indexOf(delim);
        return hashPos > 0 ? signalFullName.substring(0, hashPos) : signalFullName;
    }
}
