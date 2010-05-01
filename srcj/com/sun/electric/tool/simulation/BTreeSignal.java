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

    private Signal.View<S> exactView = null;
    private final BTree<Double,S,Pair<Pair<Double,S>,Pair<Double,S>>> tree;
    private Signal.View<S> pa = null;
    private double tmin;
    private double tmax;
    private int    emax;

    public static int misses = 0;
    public static int steps = 0;
    public static int numLookups = 0;

    public BTreeSignal(Analysis analysis, String signalName, String signalContext,
                       BTree<Double,S,Pair<Pair<Double,S>,Pair<Double,S>>> tree
                       ) {
        super(analysis, signalName, signalContext);
        if (tree==null) throw new RuntimeException();
        this.tree = tree;
        this.exactView = new Signal.View<S>() {
            public int getNumEvents() { return BTreeSignal.this.tree.size(); }
            public double             getTime(int index) {
                Double d = BTreeSignal.this.tree.getKeyFromOrd(index);
                if (d==null) throw new RuntimeException("index out of bounds");
                return d.doubleValue();
            }
            public S       getSample(int index) {
                S ret = BTreeSignal.this.tree.getValFromOrd(index);
                if (ret==null) throw new RuntimeException("index out of bounds");
                return ret;
            }
            public int getTimeNumerator(int index) { throw new RuntimeException("not implemented"); }
            public int getTimeDenominator() { throw new RuntimeException("not implemented"); }
        };
    }

    public void addSample(double time, S sample) { tree.insert(time, sample); }

    public Signal.View<S> getExactView() { return exactView; }

    public Signal.View<RangeSample<S>> getRasterView(double t0, double t1, int numPixels) {
        return new BTreeRasterView(t0, t1, numPixels);
    }

	public double getMinTime()  { return tree.size()==0 ? 0 : getExactView().getTime(0); }
	public double getMaxTime()  { return tree.size()==0 ? 0 : getExactView().getTime(getExactView().getNumEvents()-1); }

    protected Pair<Pair<Double,S>,Pair<Double,S>> getSummaryFromKeys(Double t1, Double t2) {
        return tree.getSummaryFromKeys(t1, t2);
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
    }


    // Page Storage //////////////////////////////////////////////////////////////////////////////

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

