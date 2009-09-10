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
import com.sun.electric.tool.btree.*;
import com.sun.electric.tool.btree.unboxed.*;
import com.sun.electric.tool.simulation.*;

public class BTreeNewSignal extends NewSignalSimpleImpl implements Waveform {

    private final String signal;
    private final int numEvents;
    private final int eventWithMinValue;
    private final int eventWithMaxValue;
    private NewSignal.Approximation<ScalarSample> preferredApproximation = null;
    private final BTree<Integer,Pair<Double,Double>,Serializable> tree;
    
    public BTreeNewSignal(String signal,
                          int numEvents,
                          int eventWithMinValue,
                          int eventWithMaxValue,
                          BTree<Integer,Pair<Double,Double>,Serializable> tree
                          ) {
        this.signal = signal;
        this.numEvents = numEvents;
        this.eventWithMinValue = eventWithMinValue;
        this.eventWithMaxValue = eventWithMaxValue;
        if (tree==null) throw new RuntimeException();
        this.tree = tree;
        this.preferredApproximation = new BTreeNewSignalApproximation();
    }

    public synchronized NewSignal.Approximation<ScalarSample> getPreferredApproximation() {
        return preferredApproximation;
    }

    public int getNumEvents() { return numEvents; }
    public void getEvent(int index, double[] result) {
        result[0] = getPreferredApproximation().getTime(index);
        result[1] = result[2] = getPreferredApproximation().getSample(index).getValue();
    }

    private class BTreeNewSignalApproximation implements NewSignal.Approximation<ScalarSample> {
        public int getNumEvents() { return numEvents; }
        public double             getTime(int index) {
            Double d = tree.get(index).getKey();
            if (d==null) throw new RuntimeException("index out of bounds");
            return d.doubleValue();
        }
        public ScalarSample       getSample(int index) {
            Double d = tree.get(index).getValue();
            if (d==null) throw new RuntimeException("index out of bounds");
            return new ScalarSample(d.doubleValue());
        }
        public int getTimeNumerator(int index) { throw new RuntimeException("not implemented"); }
        public int getTimeDenominator() { throw new RuntimeException("not implemented"); }
        public int getEventWithMaxValue() { return eventWithMaxValue; }
        public int getEventWithMinValue() { return eventWithMinValue; }
    }
}
