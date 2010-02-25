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

public class BTreeSignal extends Signal<ScalarSample> {

    public final int numEvents;
    public final int eventWithMinValue;
    public final int eventWithMaxValue;
    private Signal.View<ScalarSample> preferredApproximation = null;
    private final BTree<Double,Double,Serializable> tree;

    public BTreeSignal(Analysis analysis, String signalName, String signalContext,
                          int eventWithMinValue,
                          int eventWithMaxValue,
                          BTree<Double,Double,Serializable> tree
                          ) {
        super(analysis, signalName, signalContext);
        this.numEvents = tree.size();
        this.eventWithMinValue = eventWithMinValue;
        this.eventWithMaxValue = eventWithMaxValue;
        if (tree==null) throw new RuntimeException();
        this.tree = tree;
        this.preferredApproximation = new BTreeSignalApproximation();
    }

    /** produces a BTreeSignal from a preexisting pair of double[]s */
    /*
          public static BTreeSignal buildSignalForArray(Analysis analysis, String signalName, String signalContext,
                                                  double[] time, double[] val) {
            double minValue = Double.MAX_VALUE;
            double maxValue = Double.MIN_VALUE;
            int    evmin = 0;
            int    evmax = 0;
            double[] result = new double[10];
            for(int i=0; i<count; i++) {
                waveform.getEvent(i, result);
                times[i]  = (float)result[0];
                values[i] = (float)result[1];
                if (values[i] < minValue) minValue = values[evmin = i];
                if (values[i] > maxValue) maxValue = values[evmax = i];
                if (i>0 && times[i] < times[i-1])
                    throw new RuntimeException("got non-monotonic sample data, and I haven't implement sorting yet");
            }
            this.eventWithMinValue = evmin;
            this.eventWithMaxValue = evmax;
    }
        */

    private Signal.View<ScalarSample> pa = null;
    private double tmin;
    private double tmax;
    private int    emax;

    public static int misses = 0;
    public static int steps = 0;
    public static int numLookups = 0;

    public Signal.View<ScalarSample> getApproximation(double t0, double t1, int numEvents,
                                                                  ScalarSample v0, ScalarSample v1, int vd) {
        if (vd!=0) throw new RuntimeException("not implemented");

        // FIXME: currently ignoring v0/v1
        int e0 = getEventForTime(t0, true);
        int e1 = getEventForTime(t1, false);
        //System.err.println("t0="+t0+", e0="+e0+", getExactView().getTime(e0)="+getExactView().getTime(e0));
        //System.err.println("t1="+t1+", e1="+e1+", getExactView().getTime(e1)="+getExactView().getTime(e1));
        if (numEvents==0) throw new RuntimeException("invalid!");
        return new ApproximationSimpleImpl(e0, e1, numEvents, t0, t1);
    }

    private class ApproximationSimpleImpl implements Signal.View<ScalarSample> {
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
        public ScalarSample getSample(int event) {
            return getSampleForTime(getTime(event), /* FIXME: should interpolate */ true);
        }
        public int    getTimeDenominator() { throw new RuntimeException("not implemented"); }
        public int    getEventWithMinValue() { throw new RuntimeException("not implemented"); }
        public int    getEventWithMaxValue() { throw new RuntimeException("not implemented"); }
        public int    getTimeNumerator(int event) { throw new RuntimeException("not implemented"); }
    }

    public synchronized Signal.View<ScalarSample> getExactView() {
        return preferredApproximation;
    }

    protected ScalarSample getSampleForTime(double t, boolean justLessThan) {
        Double d = tree.getValFromKeyFloor(t);
        if (d==null) throw new RuntimeException("index out of bounds");
        return new ScalarSample(d.doubleValue());
    }

    protected int getEventForTime(double t, boolean justLessThan) {
        return tree.getOrdFromKeyFloor(t);
    }

    public Signal.View<RangeSample<ScalarSample>> getRasterView(double t0, double t1, int numPixels) {
        return new BTreeRasterView(t0, t1, numPixels);
    }

    private class BTreeSignalApproximation implements Signal.View<ScalarSample> {
        public int getNumEvents() { return numEvents; }
        public double             getTime(int index) {
            Double d = tree.getKeyFromOrd(index);
            if (d==null) throw new RuntimeException("index out of bounds");
            return d.doubleValue();
        }
        public ScalarSample       getSample(int index) {
            Double d = tree.getValFromOrd(index);
            if (d==null) throw new RuntimeException("index out of bounds");
            return new ScalarSample(d.doubleValue());
        }
        public int getTimeNumerator(int index) { throw new RuntimeException("not implemented"); }
        public int getTimeDenominator() { throw new RuntimeException("not implemented"); }
        public int getEventWithMaxValue() { return eventWithMaxValue; }
        public int getEventWithMinValue() { return eventWithMinValue; }
    }

    private class BTreeRasterView implements Signal.View<RangeSample<ScalarSample>> {
        int[] events;
        public BTreeRasterView(double t0, double t1, int numRegions) {
            int[] events = new int[numRegions];
            int j = 0;
            double stride = (t1-t0)/(numRegions*2);
            for(int i=0; i<numRegions; i++) {
                double t = t0 + (2*i+1)*stride;
                int idx = i==numRegions-1
                    ? tree.getOrdFromKeyCeiling(t)
                    : tree.getOrdFromKeyFloor(t);
                if (j>0 && events[j-1]==idx) idx = tree.getOrdFromKeyCeiling(t);
                if (j>0 && events[j-1]==idx) continue;
                if (idx==tree.size()) continue;
                if (idx==-1) continue;
                if (j < numRegions-1 && tree.getKeyFromOrd(idx) > t+2*stride) continue;
                events[j++] = idx;
            }
            this.events = new int[j];
            System.arraycopy(events, 0, this.events, 0, j);
        }
        public int getNumEvents() { return events.length; }
        public double getTime(int index) {
            Double d = tree.getKeyFromOrd(events[index]);
            if (d==null) throw new RuntimeException("index "+index+"/"+events[index]+" out of bounds, size="+tree.size());
            return d.doubleValue();
        }
        public RangeSample<ScalarSample> getSample(int index) {
            Double d = tree.getValFromOrd(events[index]);
            if (d==null) throw new RuntimeException("index out of bounds");
            ScalarSample ss = new ScalarSample(d.doubleValue());
            return new RangeSample(ss, ss);
        }
        public int getTimeNumerator(int index) { throw new RuntimeException("not implemented"); }
        public int getTimeDenominator() { throw new RuntimeException("not implemented"); }
        public int getEventWithMaxValue() {
            //return eventWithMaxValue;
            throw new RuntimeException("not implemented");
        }
        public int getEventWithMinValue() { 
            //return eventWithMinValue;
            throw new RuntimeException("not implemented");
        }
    }
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
