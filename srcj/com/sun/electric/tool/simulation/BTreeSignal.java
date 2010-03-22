/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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

import java.io.*;
import java.util.*;
import com.sun.electric.database.geometry.btree.*;
import com.sun.electric.database.geometry.btree.unboxed.*;
import com.sun.electric.tool.simulation.*;

abstract class BTreeSignal<S extends Sample & Comparable> extends Signal<S> {

    public int eventWithMinValue; // FIXME: get rid of this
    public int eventWithMaxValue; // FIXME: get rid of this
    private S minValue = null;
    private S maxValue = null;
    private Signal.View<S> preferredApproximation = null;
    private final BTree<Double,S,Pair<Pair<Double,S>,Pair<Double,S>>> tree;

    public BTreeSignal(Analysis analysis, String signalName, String signalContext,
                       BTree<Double,S,Pair<Pair<Double,S>,Pair<Double,S>>> tree
                       ) {
        super(analysis, signalName, signalContext);
        if (tree==null) throw new RuntimeException();
        this.tree = tree;
        this.preferredApproximation = new BTreeSignalApproximation();
    }

    private Signal.View<S> pa = null;
    private double tmin;
    private double tmax;
    private int    emax;

    public static int misses = 0;
    public static int steps = 0;
    public static int numLookups = 0;

    public Signal.View<S> getApproximation(double t0, double t1, int numEvents, S v0, S v1, int vd) {
        if (vd!=0) throw new RuntimeException("not implemented");

        // FIXME: currently ignoring v0/v1
        int e0 = getEventForTime(t0, true);
        int e1 = getEventForTime(t1, false);
        //System.err.println("t0="+t0+", e0="+e0+", getExactView().getTime(e0)="+getExactView().getTime(e0));
        //System.err.println("t1="+t1+", e1="+e1+", getExactView().getTime(e1)="+getExactView().getTime(e1));
        if (numEvents==0) throw new RuntimeException("invalid!");
        return new ApproximationSimpleImpl(e0, e1, numEvents, t0, t1);
    }

    private class ApproximationSimpleImpl implements Signal.View<S> {
        private final int    minEvent;
        private final int    maxEvent;
        private final int    numEvents;
        private final double t0;
        private final double t1;
        public ApproximationSimpleImpl(int minEvent, int maxEvent, int numEvents, double t0, double t1) {
            this.minEvent=minEvent;
            this.maxEvent=maxEvent;
            this.t0 = t0;
            this.t1 = t1;
            this.numEvents = numEvents;
            //System.err.println("minEvent="+minEvent+", maxEvent="+maxEvent+", ne="+numEvents + " t0="+t0+ " t1="+t1);
        }
        public int    getNumEvents() { return numEvents; }
        public double getTime(int event) { return t0 + (event*(t1-t0))/(numEvents-1); }
        public S getSample(int event) {
            return getSampleForTime(getTime(event), /* FIXME: should interpolate */ true);
        }
        public int    getTimeDenominator() { throw new RuntimeException("not implemented"); }
        public int    getEventWithMinValue() { throw new RuntimeException("not implemented"); }
        public int    getEventWithMaxValue() { throw new RuntimeException("not implemented"); }
        public int    getTimeNumerator(int event) { throw new RuntimeException("not implemented"); }
    }

    public void addSample(double time, S sample) {
        tree.insert(time, sample);
        if (minValue==null || sample.compareTo(minValue) < 0) {
            this.eventWithMinValue = getEventForTime(time, false);
            minValue = sample;
        }
        if (maxValue==null || sample.compareTo(maxValue) > 0) {
            this.eventWithMaxValue = getEventForTime(time, false);
            maxValue = sample;
        }
    }

    public synchronized Signal.View<S> getExactView() {
        return preferredApproximation;
    }

    protected S getSampleForTime(double t, boolean justLessThan) {
        S ret = tree.getValFromKeyFloor(t);
        if (ret==null) throw new RuntimeException("index out of bounds");
        return ret;
    }

    protected int getEventForTime(double t, boolean justLessThan) {
        return tree.getOrdFromKeyFloor(t);
    }

    public Signal.View<RangeSample<S>> getRasterView(double t0, double t1, int numPixels) {
        return new BTreeRasterView(t0, t1, numPixels);
    }

    private class BTreeSignalApproximation implements Signal.View<S> {
        public int getNumEvents() { return tree.size(); }
        public double             getTime(int index) {
            Double d = tree.getKeyFromOrd(index);
            if (d==null) throw new RuntimeException("index out of bounds");
            return d.doubleValue();
        }
        public S       getSample(int index) {
            S ret = tree.getValFromOrd(index);
            if (ret==null) throw new RuntimeException("index out of bounds");
            return ret;
        }
        public int getTimeNumerator(int index) { throw new RuntimeException("not implemented"); }
        public int getTimeDenominator() { throw new RuntimeException("not implemented"); }
        public int getEventWithMaxValue() { return eventWithMaxValue; }
        public int getEventWithMinValue() { return eventWithMinValue; }
    }

    private class BTreeRasterView implements Signal.View<RangeSample<S>> {
        private final double t0, t1;
        private final int numRegions;
        public BTreeRasterView(double t0, double t1, int numRegions) {
            this.t0 = Math.min(t0, t1);
            this.t1 = Math.max(t0, t1);
            this.numRegions = numRegions;
        }
        public int getNumEvents() { return numRegions; }
        public double getTime(int index) { return t0+(((t1-t0)*index)/numRegions); }
        public RangeSample<S> getSample(int index) {
            Pair<Pair<Double,S>,Pair<Double,S>> highlow =
                tree.getSummaryFromKeys(getTime(index), getTime(index+1));
            return highlow==null
                ? null
                : new RangeSample<S>(highlow.getKey().getValue(),
                                     highlow.getValue().getValue());
        }
        public int getTimeNumerator(int index) { throw new RuntimeException("not implemented"); }
        public int getTimeDenominator() { throw new RuntimeException("not implemented"); }
        public int getEventWithMaxValue() { throw new RuntimeException("not implemented"); }
        public int getEventWithMinValue() { throw new RuntimeException("not implemented"); }
    }

    private static CachingPageStorage ps = null;
    static <SS extends Sample & Comparable>
        BTree<Double,SS,Pair<Pair<Double,SS>,Pair<Double,SS>>> getTree(UnboxedComparable<SS> unboxer) {
        if (ps==null)
            try {
                long highWaterMarkInBytes = 50 * 1024 * 1024;
                PageStorage fps = FilePageStorage.create();
                PageStorage ops = new OverflowPageStorage(new MemoryPageStorage(fps.getPageSize()), fps, highWaterMarkInBytes);
                ps = new CachingPageStorageWrapper(ops, 16 * 1024, false);
                //ps = new MemoryPageStorage(256);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        return new BTree<Double,SS,Pair<Pair<Double,SS>,Pair<Double,SS>>>
            (ps, UnboxedHalfDouble.instance, unboxer, new Summary<SS>(UnboxedHalfDouble.instance, unboxer));
    }

    private static class Summary<SS extends Sample & Comparable>
        extends MinMaxOperation<Double,SS>
        implements BTree.Summary<Double,SS,Pair<Pair<Double,SS>,Pair<Double,SS>>> {
        public Summary(Unboxed<Double> uk, UnboxedComparable<SS> uv) { super(uk, uv); }
    };

}

/*
    // This binary search is done carefully so that it takes the
    // same search path as far as possible even for different
    // t0/t1 inputs.  That is, for example, why we don't use e0 to
    // help start the search for e1.  This ensures that any
    // caching done by the subclass is exploited as fully as
    // possible.
    protected int getEventForTime(double t, boolean justLessThan) {
        if (pa==null) {
            this.pa = getExactView();
            this.emax = pa.getNumEvents()-1;
            this.tmin = pa.getTime(0);
            this.tmax = pa.getTime(this.emax);
        }
        numLookups++;
        int emin  = 0;
        int emax  = this.emax;
        double tmin = this.tmin;
        double tmax = this.tmax;
        boolean last = true;
        while(true) {
            if (emin==emax) return emin;
            if (emin+1 == emax) return justLessThan ? emin : emax;
            double est = ((t-tmin)*(emax-emin))/(tmax-tmin);
            //int e = (emin+emax)/2;
            int e = emin + (last ? ((int)Math.ceil(est)) : ((int)Math.floor(est)));
            last = !last;
            if (e<=emin) return emin;
            if (e>=emax) return emax;
            double te = pa.getTime(e);
            steps++;
            if      (te < t) { emin = e; tmin = te; }
            else if (te > t) { emax = e; tmax = te; }
            else             return e;
        }
    }
*/
