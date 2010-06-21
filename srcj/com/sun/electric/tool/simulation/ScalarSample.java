/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Signal.java
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
import com.sun.electric.database.geometry.btree.unboxed.*;
import java.io.*;

/**
 *  An implementation of Sample for scalar data.  Holds a
 *  double internally.
 */
public class ScalarSample implements Sample, Comparable {

    private double value;

    public ScalarSample() { this(0); }
    public ScalarSample(double value) { this.value = value; }

    public double getValue() {
        return value;
    }

    public boolean equals(Object o) {
        if (o==null || !(o instanceof ScalarSample)) return false;
        ScalarSample ss = (ScalarSample)o;
        return ss.value == value;
    }

    public int hashCode() {
        long l = Double.doubleToLongBits(value);
        return ((int)(l & 0xffffffff)) ^ ((int)((l >> 32) & 0xffffffff));
    }

    public int compareTo(Object o) {
        if (o==null || !(o instanceof ScalarSample))
            throw new RuntimeException("impossible!");
        ScalarSample ss = (ScalarSample)o;
        return Double.compare(value, ss.value);
    }

    public String toString() {
        return Double.toString(value);
    }

    public boolean isLogicX() {
        return value == Double.POSITIVE_INFINITY;
    }

    public boolean isLogicZ() {
        return value == Double.NEGATIVE_INFINITY;
    }

    public Sample glb(Sample s) {
        if (!(s instanceof ScalarSample)) throw new RuntimeException("tried to call ScalarSample.glb("+s.getClass().getName()+")");
        return value < ((ScalarSample)s).value ? this : s;
    }

    public Sample lub(Sample s) {
        if (!(s instanceof ScalarSample)) throw new RuntimeException("tried to call ScalarSample.lub("+s.getClass().getName()+")");
        return value > ((ScalarSample)s).value ? this : s;
    }

    public static final UnboxedComparable<ScalarSample> unboxer = new UnboxedComparable<ScalarSample>() {
        public int getSize() { return UnboxedHalfDouble.instance.getSize(); }
        public ScalarSample deserialize(byte[] buf, int ofs) {
            return new ScalarSample(UnboxedHalfDouble.instance.deserialize(buf, ofs));
        }
        public void serialize(ScalarSample v, byte[] buf, int ofs) {
            UnboxedHalfDouble.instance.serialize(v.value, buf, ofs);
        }
        public int compare(byte[] buf1, int ofs1, byte[] buf2, int ofs2) {
            return UnboxedHalfDouble.instance.compare(buf1, ofs1, buf2, ofs2);
        }
    };

    static final LatticeOperation<ScalarSample> latticeOp =
        new LatticeOperation<ScalarSample>(unboxer) {
        public void glb(byte[] buf1, int ofs1, byte[] buf2, int ofs2, byte[] dest, int dest_ofs) {
            if (UnboxedHalfDouble.instance.compare(buf1, ofs1, buf2, ofs2) < 0)
                System.arraycopy(buf1, ofs1, dest, dest_ofs, unboxer.getSize());
            else
                System.arraycopy(buf2, ofs2, dest, dest_ofs, unboxer.getSize());
        }
        public void lub(byte[] buf1, int ofs1, byte[] buf2, int ofs2, byte[] dest, int dest_ofs) {
            if (UnboxedHalfDouble.instance.compare(buf1, ofs1, buf2, ofs2) < 0)
                System.arraycopy(buf2, ofs2, dest, dest_ofs, unboxer.getSize());
            else
                System.arraycopy(buf1, ofs1, dest, dest_ofs, unboxer.getSize());
        }
    };

    public static Signal<ScalarSample> createSignal(Analysis an, String signalName, String signalContext) {
        Signal<ScalarSample> ret =
            new BTreeSignal<ScalarSample>(an, signalName, signalContext, BTreeSignal.getTree(unboxer, latticeOp)) {
            public boolean isDigital() { return false; }
            public boolean isAnalog() { return true; }
        };
        an.addSignal(ret);
        return ret;
    }

	public static Signal<ScalarSample> createSignal(Analysis an, String signalName, String signalContext,
                                                    double[] time, double[] values) {
        if (values.length==0) throw new RuntimeException("attempt to create an empty signal");
        Signal<ScalarSample> as = ScalarSample.createSignal(an, signalName, signalContext);
        for(int i=0; i<time.length; i++)
            if (((MutableSignal)as).getSample(time[i])==null)
                as.addSample(time[i], new ScalarSample(values[i]));
		return as;
	}
}


