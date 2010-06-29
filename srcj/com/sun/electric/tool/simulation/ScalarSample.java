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
import com.sun.electric.tool.user.waveform.*;
import java.io.*;
import java.util.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.tool.user.waveform.Panel.WaveSelection;

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

    public static MutableSignal<ScalarSample> createSignal(HashMap<String,Signal> an,
                                                           Stimuli sd, String signalName, String signalContext) {
        MutableSignal<ScalarSample> ret =
            new BTreeSignal<ScalarSample>(an, sd, signalName, signalContext, BTreeSignal.getTree(unboxer, latticeOp)) {
            public void plot(Panel panel, Graphics g, WaveSignal ws, Color light,
                             List<PolyBase> forPs, Rectangle2D bounds, List<WaveSelection> selectedObjects) {
                int linePointMode = panel.getWaveWindow().getLinePointMode();
                Dimension sz = panel.getSize();
                int hei = sz.height;

                // draw analog trace
                Signal as = this;
                int s = 0;
                //for (int s = 0, numSweeps = as.getNumSweeps(); s < numSweeps; s++)
                //{
                //boolean included = waveWindow.isSweepSignalIncluded(an, s);
                //if (!included)
                //continue;
                //Signal wave = as.getWaveform(s);
                Signal wave = as;
                if (wave.isEmpty()) return;
                Signal.View<RangeSample<ScalarSample>> waveform =
                ((Signal<ScalarSample>)wave).getRasterView(panel.convertXScreenToData(0),
                                                           panel.convertXScreenToData(sz.width),
                                                           sz.width,
                                                           true);
                Signal xWaveform = null;
                int lastX = 0, lastLY = 0, lastHY = 0;
                boolean first = true;
                for(int i=0; i<waveform.getNumEvents(); i++)
                    {
                        int x = panel.convertXDataToScreen(waveform.getTime(i));
                        RangeSample<ScalarSample> samp =
                            (RangeSample<ScalarSample>)waveform.getSample(i);
                        if (samp==null) continue;
                        int lowY = panel.convertYDataToScreen(samp.getMin().getValue());
                        int highY = panel.convertYDataToScreen(samp.getMax().getValue());
                        if (xWaveform != null)
                            x = panel.convertXDataToScreen(((ScalarSample)xWaveform.getExactView().getSample(i)).getValue());

                        // draw lines if requested and line is on-screen
                        if (linePointMode <= 1) {
                            if (!first) {
                                // drawing has lines
                                if (lastLY != lastHY || lowY != highY) {
                                    if (g!=null) g.setColor(light);
                                    panel.processALine(g, lastX, lastHY, lastX, lastLY, bounds, forPs, selectedObjects, ws, s);
                                    panel.processALine(g, x, highY, x, lowY, bounds, forPs, selectedObjects, ws, s);
                                    if (g!=null) g.setColor(ws.getColor());
                                    panel.processALine(g, lastX, lastHY, x, highY, bounds, forPs, selectedObjects, ws, s);
                                    //if (panel.processALine(g, lastX, lastHY, x, lowY, bounds, forPs, selectedObjects, ws, s)) break;
                                    //if (panel.processALine(g, lastX, lastLY, x, highY, bounds, forPs, selectedObjects, ws, s)) break;
                                }
                                panel.processALine(g, lastX, lastLY, x, lowY, bounds, forPs, selectedObjects, ws, s);
                            }
                        } else {
                            // show points if requested and point is on-screen
                            panel.processABox(g, x-2, lowY-2, x+2, lowY+2, bounds, forPs, selectedObjects, ws, false, 0);
                        }
                        lastX = x;
                        lastLY = lowY;
                        lastHY = highY;
                        first = false;
                    }
            }
        };
        return ret;
    }

	public static MutableSignal<ScalarSample> createSignal(HashMap<String,Signal> an, Stimuli sd, String signalName, String signalContext,
                                                           double[] time, double[] values) {
        if (values.length==0) throw new RuntimeException("attempt to create an empty signal");
        MutableSignal<ScalarSample> as = ScalarSample.createSignal(an, sd, signalName, signalContext);
        for(int i=0; i<time.length; i++)
            if (((MutableSignal)as).getSample(time[i])==null)
                as.addSample(time[i], new ScalarSample(values[i]));
		return as;
	}

}


