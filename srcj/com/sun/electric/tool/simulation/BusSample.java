/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BusSample.java
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
import com.sun.electric.database.geometry.btree.unboxed.*;
import java.io.*;

/**
 * A bus of many Signal<S> is represented as a single
 * Signal<BusSample<S>>.
 */
public class BusSample<S extends Sample> implements Sample {

    private final Sample[] vals;

    public BusSample(Sample[] vals) {
        this.vals = new Sample[vals.length];
        for(int i=0; i<vals.length; i++) {
            if (vals[i]==null) throw new RuntimeException("null values not allowed in buses");
            this.vals[i] = vals[i];
        }
    }

    public boolean equals(Object o) {
        if (o==null) return false;
        if (!(o instanceof BusSample)) return false;
        BusSample bo = (BusSample)o;
        if (bo.vals.length != vals.length) return false;
        for(int i=0; i<vals.length; i++)
            if (!vals[i].equals(bo.vals[i]))
                return false;
        return true;
    }
    public int hashCode() {
        int ret = 0;
        for(int i=0; i<vals.length; i++)
            ret ^= vals[i].hashCode();
        return ret;
    }

    public boolean isLogicX() { for(Sample s : vals) if (!s.isLogicX()) return false; return true; }
    public boolean isLogicZ() { for(Sample s : vals) if (!s.isLogicZ()) return false; return true; }

    public Sample lub(Sample s) {
        if (!(s instanceof BusSample)) throw new RuntimeException("tried to call BusSample.lub("+s.getClass().getName()+")");
        BusSample ds = (BusSample)s;
        if (ds.vals.length != vals.length) throw new RuntimeException("tried to call lub() on BusSamples of different width");
        Sample[] ret = new Sample[vals.length];
        for(int i=0; i<ret.length; i++)
            ret[i] = vals[i].lub(ds.vals[i]);
        return new BusSample(ret);
    }

    public Sample glb(Sample s) {
        if (!(s instanceof BusSample)) throw new RuntimeException("tried to call BusSample.glb("+s.getClass().getName()+")");
        BusSample ds = (BusSample)s;
        if (ds.vals.length != vals.length) throw new RuntimeException("tried to call glb() on BusSamples of different width");
        Sample[] ret = new Sample[vals.length];
        for(int i=0; i<ret.length; i++)
            ret[i] = vals[i].glb(ds.vals[i]);
        return new BusSample(ret);
    }


    /*
    private byte getByteRepresentation() { return (byte)(((value.v+1) << 3) | strength.v); }
    private static DigitalSample fromByteRepresentation(byte b) { return cache[(b >> 3) & 3][b & 7]; }


    public static Signal<BusSample<S>> createSignal(DigitalAnalysis an, String signalName, String signalContext) {
        UnboxedComparable<DigitalSample> unboxer = new UnboxedComparable<DigitalSample>() {
            public int getSize() { return 1; }
            public DigitalSample deserialize(byte[] buf, int ofs) { return fromByteRepresentation(buf[ofs]); }
            public void serialize(DigitalSample v, byte[] buf, int ofs) { buf[ofs] = v.getByteRepresentation(); }
            public int compare(byte[] buf1, int ofs1, byte[] buf2, int ofs2) { return (buf1[ofs1]&0xff)-(buf2[ofs2]&0xff); }
        };
        return new Signal<BusSample<S>>(an, signalName, signalContext, BTreeSignal.getTree(unboxer)) {
            public boolean isDigital() { return true; }
        };
        public abstract Signal.View<RangeSample<SS>> getRasterView(double t0, double t1, int numPixels);
        public abstract Signal.View<SS> getExactView();
        public abstract double getMinTime();
        public abstract double getMaxTime();
        public abstract SS     getMinValue();
        public abstract SS     getMaxValue();
        public boolean isDigital() { return false; }
        public boolean isAnalog() { return !isDigital(); }
        public void    addSample(double time, SS sample) {
            throw new RuntimeException(getClass().getName()+" does not support adding new samples");
        }
    }
    */

}
