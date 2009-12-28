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

public class BTreeNewSignal extends NewSignalSimpleImpl implements Waveform {

    public final int numEvents;
    public final int eventWithMinValue;
    public final int eventWithMaxValue;
    private NewSignal.Approximation<ScalarSample> preferredApproximation = null;
    private final BTree<Double,Double,Serializable> tree;
    
    public BTreeNewSignal(int eventWithMinValue,
                          int eventWithMaxValue,
                          BTree<Double,Double,Serializable> tree
                          ) {
        this.numEvents = tree.size();
        this.eventWithMinValue = eventWithMinValue;
        this.eventWithMaxValue = eventWithMaxValue;
        if (tree==null) throw new RuntimeException();
        this.tree = tree;
        this.preferredApproximation = new BTreeNewSignalApproximation();
    }

    public synchronized NewSignal.Approximation<ScalarSample> getPreferredApproximation() {
        return preferredApproximation;
    }

    protected ScalarSample getSampleForTime(double t, boolean justLessThan) {
        Double d = tree.getValFromKeyFloor(t);
        if (d==null) throw new RuntimeException("index out of bounds");
        return new ScalarSample(d.doubleValue());
    }

    public int getNumEvents() { return numEvents; }
    protected int getEventForTime(double t, boolean justLessThan) {
        return tree.getOrdFromKeyFloor(t);
    }
    public void getEvent(int index, double[] result) {
        result[0] = getPreferredApproximation().getTime(index);
        result[1] = result[2] = getPreferredApproximation().getSample(index).getValue();
    }

    public NewSignal.Approximation<ScalarSample>
        getPixelatedApproximation(double t0, double t1, int numRegions) {
        return new BTreePixelatedApproximation(t0, t1, numRegions);
    }

    private class BTreeNewSignalApproximation implements NewSignal.Approximation<ScalarSample> {
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

    private class BTreePixelatedApproximation implements NewSignal.Approximation<ScalarSample> {
        int[] events;
        public BTreePixelatedApproximation(double t0, double t1, int numRegions) {
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
        public ScalarSample getSample(int index) {
            Double d = tree.getValFromOrd(events[index]);
            if (d==null) throw new RuntimeException("index out of bounds");
            return new ScalarSample(d.doubleValue());
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
