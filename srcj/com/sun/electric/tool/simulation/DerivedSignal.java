/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DerivedSignal.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
import java.util.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.tool.user.waveform.Panel.WaveSelection;
import com.sun.electric.tool.user.waveform.*;
import com.sun.electric.database.geometry.Poly;
import java.awt.font.GlyphVector;
import com.sun.electric.database.variable.TextDescriptor;

/**
 * A Signal which is derived in a *pointwise* fashion from other
 * signals.  This means that the min/max estimate for the signal over
 * the time period [t0:t1] must depend only on the min/max estimate
 * for the source signals over that same range.  So, for example, a
 * convolution cannot be represented using this class.
 */
public abstract class DerivedSignal<SNew extends Sample, SOld extends Sample> extends Signal<SNew> {

    private final Signal<SOld>[] sources;

    public DerivedSignal(HashMap<String,Signal> analysis, Stimuli sd, String signalName, String signalContext,
                         Signal<SOld>[] sources) {
        super(analysis, sd, signalName, signalContext);
        this.sources = sources;
    }

    public Signal.View<RangeSample<SNew>> getRasterView(double t0, double t1, int numPixels, boolean extrapolate) {
        View<RangeSample<SOld>>[] views = new View[sources.length];
        for(int i=0; i<views.length; i++)
            views[i] = sources[i].getRasterView(t0, t1, numPixels, extrapolate);
        return new DerivedSignalRasterView(views);
    }
    private class DerivedSignalRasterView implements View<RangeSample<SNew>> {
        private View<RangeSample<SOld>>[] views;
        private RangeSample<SOld>[]       scratch;
        public DerivedSignalRasterView(View<RangeSample<SOld>>[] views) {
            this.views = views;
            scratch = new RangeSample[views.length];
        }
        public int             getNumEvents() { return views[0].getNumEvents(); }
        public double          getTime(int event) { return views[0].getTime(event); }
        public RangeSample<SNew> getSample(int event) {
            for(int i=0; i<scratch.length; i++)
                scratch[i] = views[i].getSample(event);
            RangeSample<SNew> ret = getDerivedRange(scratch);
            for(int i=0; i<scratch.length; i++) scratch[i] = null;
            return ret;
        }
    }

	public double getMinTime() {
        double min = Double.MIN_VALUE;
        for(int i=0; i<sources.length; i++)
            min = Math.max(min, sources[i].getMinTime());
        return min;
    }
	public double getMaxTime() {
        double max = Double.MAX_VALUE;
        for(int i=0; i<sources.length; i++)
            max = Math.min(max, sources[i].getMaxTime());
        return max;
    }

    public Signal.View<SNew> getExactView() {
        if (sources.length==1)
            return new DerivedSignalExactView(sources[0].getExactView());
        throw new RuntimeException("Exact views of DerivedSignal's with >1 source are not supported");
    }

    private class DerivedSignalExactView implements View<SNew> {
        private View<SOld> view;
        public DerivedSignalExactView(View<SOld> view) { this.view = view; }
        public int               getNumEvents() { return view.getNumEvents(); }
        public double            getTime(int event) { return view.getTime(event); }
        public SNew getSample(int event) {
            SOld old = view.getSample(event);
            return getDerivedRange(new RangeSample[] { new RangeSample<SOld>(old, old) }).getMin() /* FIXME: arbitrary */;
        }
    }

    protected abstract RangeSample<SNew> getDerivedRange(RangeSample<SOld>[] sourceRanges);

    public void plot(Panel panel, Graphics g, WaveSignal ws, Color light,
                     List<PolyBase> forPs, Rectangle2D bounds, List<WaveSelection> selectedObjects) {
        throw new RuntimeException("not implemented");
    }
}
