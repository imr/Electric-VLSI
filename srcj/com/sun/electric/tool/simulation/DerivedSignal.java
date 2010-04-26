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

/**
 * A Signal which is derived in a *pointwise* fashion from other
 * signals.  This means that the min/max estimate for the signal over
 * the time period [t0:t1] must depend only on the min/max estimate
 * for the source signals over that same range.  So, for example, a
 * convolution cannot be represented using this class.
 */
public abstract class DerivedSignal<SS extends Sample> extends Signal<SS> {

    private final Signal<SS>[] sources;

    public DerivedSignal(Analysis analysis, String signalName, String signalContext,
                         Signal<SS>[] sources) {
        super(analysis, signalName, signalContext);
        this.sources = sources;
    }

    public Signal.View<RangeSample<SS>> getRasterView(double t0, double t1, int numPixels) {
        View<RangeSample<SS>>[] views = new View[sources.length];
        for(int i=0; i<views.length; i++)
            views[i] = sources[i].getRasterView(t0, t1, numPixels);
        return new DerivedSignalRasterView(views);
    }
    private class DerivedSignalRasterView implements View<RangeSample<SS>> {
        private View<RangeSample<SS>>[] views;
        private RangeSample<SS>[]       scratch;
        public DerivedSignalRasterView(View<RangeSample<SS>>[] views) {
            this.views = views;
            scratch = new RangeSample[views.length];
        }
        public int             getNumEvents() { return views[0].getNumEvents(); }
        public double          getTime(int event) { return views[0].getTime(event); }
        public RangeSample<SS> getSample(int event) {
            for(int i=0; i<scratch.length; i++)
                scratch[i] = views[i].getSample(event);
            RangeSample<SS> ret = getDerivedRange(scratch);
            for(int i=0; i<scratch.length; i++) scratch[i] = null;
            return ret;
        }
        public int             getTimeNumerator(int event) { throw new RuntimeException("not implemented"); }
        public int             getTimeDenominator() { throw new RuntimeException("not implemented"); }
    }

    public Signal.View<SS> getExactView() {
        throw new RuntimeException("Exact views of DerivedSignal's are not supported");
        /*
        View<SS>[] views = new View<SS>[sources.length];
        for(int i=0; i<views.length; i++)
            views[i] = sources[i].getExactView();
        return new DerivedSignalExactView(views);
        */
    }
    /*
    private class DerivedSignalExactView implements View<SS> {
        private View<SS>[] views;
        public DerivedSignalExactView(View<SS>[] views) { this.views = views; }
        public int             getNumEvents() { }
        public double          getTime(int event) { }
        public RangeSample<SS> getSample(int event) { }
        public int             getTimeNumerator(int event) { throw new RuntimeException("not implemented"); }
        public int             getTimeDenominator() { throw new RuntimeException("not implemented"); }
        public int             getEventWithMinValue() { throw new RuntimeException("not implemented"); }
        public int             getEventWithMaxValue() { throw new RuntimeException("not implemented"); }
    }
    */

    //abstract SS              getDerivedSample(SS[] sourceSamples);

    protected abstract RangeSample<SS> getDerivedRange(RangeSample<SS>[] sourceRanges);

}