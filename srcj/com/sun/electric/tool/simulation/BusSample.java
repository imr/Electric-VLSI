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

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.tool.user.waveform.Panel;
import com.sun.electric.tool.user.waveform.WaveSignal;
import com.sun.electric.tool.user.waveform.Panel.WaveSelection;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

/**
 * A bus of many Signal<S> is represented as a single
 * Signal<BusSample<S>>.
 */
public class BusSample<S extends Sample> implements Sample
{
    private final Sample[] vals;

    public BusSample(Sample[] vals)
    {
        int numValids = 0;
        for(int i=0; i<vals.length; i++) if (vals[i] != null) numValids++;
        this.vals = new Sample[numValids];
        int fill = 0;
        for(int i=0; i<vals.length; i++)
            if (vals[i] != null) this.vals[fill++] = vals[i];
    }

    public int getWidth() { return vals.length; }
    public S   getTrace(int i) { return (S)vals[i]; }

    public boolean equals(Object o)
    {
        if (o==null) return false;
        if (!(o instanceof BusSample<?>)) return false;
        BusSample<?> bo = (BusSample<?>)o;
        if (bo.vals.length != vals.length) return false;
        for(int i=0; i<vals.length; i++)
            if (!vals[i].equals(bo.vals[i]))
                return false;
        return true;
    }
    public int hashCode()
    {
        int ret = 0;
        for(int i=0; i<vals.length; i++)
            ret ^= vals[i].hashCode();
        return ret;
    }

    public boolean isLogicX() { for(Sample s : vals) if (!s.isLogicX()) return false; return true; }
    public boolean isLogicZ() { for(Sample s : vals) if (!s.isLogicZ()) return false; return true; }

    public Sample lub(Sample s)
    {
        if (!(s instanceof BusSample<?>)) throw new RuntimeException("tried to call BusSample.lub("+s.getClass().getName()+")");
        BusSample<?> ds = (BusSample<?>)s;
        if (ds.vals.length != vals.length) throw new RuntimeException("tried to call lub() on BusSamples of different width");
        Sample[] ret = new Sample[vals.length];
        for(int i=0; i<ret.length; i++)
            ret[i] = vals[i].lub(ds.vals[i]);
        return new BusSample<DigitalSample>(ret);
    }

    public Sample glb(Sample s)
    {
        if (!(s instanceof BusSample<?>)) throw new RuntimeException("tried to call BusSample.glb("+s.getClass().getName()+")");
        BusSample<?> ds = (BusSample<?>)s;
        if (ds.vals.length != vals.length) throw new RuntimeException("tried to call glb() on BusSamples of different width");
        Sample[] ret = new Sample[vals.length];
        for(int i=0; i<ret.length; i++)
            ret[i] = vals[i].glb(ds.vals[i]);
        return new BusSample<DigitalSample>(ret);
    }

    public double getMinValue()
    {
    	double min = Double.MAX_VALUE;
    	for(int i=0; i<vals.length; i++)
    		min = Math.min(min, vals[i].getMinValue());
    	return min;
    }

    public double getMaxValue()
    {
    	double max = -Double.MAX_VALUE;
    	for(int i=0; i<vals.length; i++)
    		max = Math.max(max, vals[i].getMaxValue());
    	return max;
    }

    /** create a MutableSignal<BusSample<SS>> */
    public static <SS extends Sample> Signal<BusSample<SS>> createSignal(SignalCollection sc, Stimuli sd, String signalName,
    	String signalContext, int width)
    {
        /*
        final Unboxed<BusSample<SS>> unboxer = new Unboxed<BusSample<SS>>() {
            public int getSize() { return 1; }
            public DigitalSample deserialize(byte[] buf, int ofs) { return fromByteRepresentation(buf[ofs]); }
            public void serialize(DigitalSample v, byte[] buf, int ofs) { buf[ofs] = v.getByteRepresentation(); }
            public int compare(byte[] buf1, int ofs1, byte[] buf2, int ofs2) { return (buf1[ofs1]&0xff)-(buf2[ofs2]&0xff); }
        };
        Signal<ScalarSample> ret =
            new BTreeSignal<ScalarSample>(an, signalName, signalContext, BTreeSignal.getTree(unboxer, latticeOp)) {
            public boolean isAnalog() { return true; }
        };
        an.addSignal(ret);
        return ret;
        */
        throw new RuntimeException("not implemented");
    }

    /** create a Signal<BusSample<S>> from preexisting Signal<S>'s */
    public static <SS extends Sample> Signal<BusSample<SS>> createSignal(SignalCollection sc, Stimuli sd, String signalName,
    	String signalContext, boolean digital, final Signal<SS>[] subsignals)
    {
        return new Signal<BusSample<SS>>(sc, sd, signalName, signalContext, digital)
        {
            public boolean isEmpty() { for(Signal<SS> sig : subsignals) if (!sig.isEmpty()) return false; return true; }

            public Signal<?>[] getBusMembers() { return subsignals; }

            public Signal.View<RangeSample<BusSample<SS>>> getRasterView(final double t0, final double t1, final int numPixels)
            {
                final Signal.View<RangeSample<SS>>[] subviews = new Signal.View[subsignals.length];

                for(int i=0; i<subviews.length; i++)
                    subviews[i] = subsignals[i].getRasterView(t0, t1, numPixels);

                // the subviews' getRasterView() methods might have differing getNumEvents() values or different
                // getTime() values for a given index.  Therefore, we must "collate" them.  By using a sorted treemap
                // here we ensure that this takes only O(n log n) time.
                
                // INVARIANT: tm.get(t).contains(i) ==> (exists j such that subviews[i].getTime(j)==t)
                TreeMap<Double,HashSet<Integer>> tm = new TreeMap<Double,HashSet<Integer>>();
                for (int i=0; i<subviews.length; i++)
                {
                    Signal.View<RangeSample<SS>> view = subviews[i];
                    for(int j=0; j<view.getNumEvents(); j++)
                    {
                        Double t = new Double(view.getTime(j));
                        HashSet<Integer> hs = tm.get(t);
                        if (hs == null) tm.put(t, hs = new HashSet<Integer>());
                        hs.add(new Integer(i));
                    }
                }

                // now we know, for each point in time, which signals changed at that point
                final double[] times = new double[tm.size()];
                final RangeSample<BusSample<SS>>[] vals = new RangeSample[tm.size()];
                int i = 0;
                int[] event = new int[subviews.length];
                SS[] minvals = (SS[])new Sample[subviews.length];
                SS[] maxvals = (SS[])new Sample[subviews.length];
                for(Double t : tm.keySet())
                {
                	times[i] = t.doubleValue();
                    HashSet<Integer> hs = tm.get(t);
                    for(int v : hs)
                    {
                    	// this used to be an assertion, but since it is possible for two successive events
                    	// to have the same time, the assertion may fail
                    	if (subviews[v].getTime(event[v]) != t.doubleValue()) continue;
                        RangeSample<SS> rs = subviews[v].getSample(event[v]);
						if (rs != null)
						{
	                        minvals[v] = rs.getMin();
	                        maxvals[v] = rs.getMax();
						}
                        event[v]++;
                    }
                    vals[i] = new RangeSample<BusSample<SS>>( new BusSample<SS>(minvals), new BusSample<SS>(maxvals) );
                    i++;
                }

                return new Signal.View<RangeSample<BusSample<SS>>>()
                {
                    public int                        getNumEvents() { return times.length; }
                    public double                     getTime(int event) { return times[event]; }
                    public RangeSample<BusSample<SS>> getSample(int event) { return vals[event]; }
                };
            }

            public Signal.View<BusSample<SS>> getExactView()
            {
                final Signal.View<SS>[] subviews = new Signal.View[subsignals.length];
                for(int i=0; i<subviews.length; i++) subviews[i] = subsignals[i].getExactView();

                // the subviews' getRasterView() methods might have differing getNumEvents() values or different
                // getTime() values for a given index.  Therefore, we must "collate" them.  By using a sorted treemap
                // here we ensure that this takes only O(n log n) time.
                
                // INVARIANT: tm.get(t).contains(i) ==> (exists j such that subviews[i].getTime(j)==t)
                TreeMap<Double,HashSet<Integer>> tm = new TreeMap<Double,HashSet<Integer>>();
                for (int i=0; i<subviews.length; i++)
                {
                    Signal.View<SS> view = subviews[i];
                    for(int j=0; j<view.getNumEvents(); j++)
                    {
                        Double t = new Double(view.getTime(j));
                        HashSet<Integer> hs = tm.get(t);
                        if (hs == null) tm.put(t, hs = new HashSet<Integer>());
                        hs.add(new Integer(i));
                    }
                }

                // now we know, for each point in time, which signals changed at that point
                final double[] times = new double[tm.size()];
                final BusSample<SS>[] vals = new BusSample[tm.size()];
                int i = 0;
                int[] event = new int[subviews.length];
                SS[] sampleVals = (SS[])new Sample[subviews.length];
                for(Double t : tm.keySet())
                {
                	times[i] = t.doubleValue();
                    HashSet<Integer> hs = tm.get(t);
                    for(int v : hs)
                    {
                        assert subviews[v].getTime(event[v]) == t.doubleValue();  // sanity check
                        SS rs = subviews[v].getSample(event[v]);
						if (rs != null) sampleVals[v] = rs;
                        event[v]++;
                    }
                    vals[i] = new BusSample<SS>(sampleVals);
                    i++;
                }

                return new Signal.View<BusSample<SS>>()
                {
                    public int           getNumEvents() { return times.length; }
                    public double        getTime(int event) { return times[event]; }
                    public BusSample<SS> getSample(int event) { return vals[event]; }
                };
            }

            public double getMinTime()
            {
                double min = Double.MAX_VALUE;
                for(Signal<SS> sig : subsignals) min = Math.min(min, sig.getMinTime());
                return min;
            }

            public double getMaxTime()
            {
                double max = -Double.MAX_VALUE;
                for(Signal<SS> sig : subsignals) max = Math.max(max, sig.getMaxTime());
                return max;
            }

            public double getMinValue()
            {
                double min = Double.MAX_VALUE;
                for(Signal<SS> sig : subsignals) min = Math.min(min, sig.getMinValue());
                return min;
            }

            public double getMaxValue()
            {
                double max = -Double.MAX_VALUE;
                for(Signal<SS> sig : subsignals) max = Math.max(max, sig.getMaxValue());
                return max;
            }

            public void plot(Panel panel, Graphics g, WaveSignal ws, Color light, List<PolyBase> forPs,
            	Rectangle2D bounds, List<WaveSelection> selectedObjects, Signal<?> xAxisSignal)
            {
                Dimension sz = panel.getSize();
                int hei = sz.height;

                // draw digital traces
				Signal<BusSample<DigitalSample>> ds = (Signal<BusSample<DigitalSample>>)ws.getSignal();
                Signal.View<RangeSample<BusSample<DigitalSample> > > view = ds.getRasterView(panel.convertXScreenToData(0),
                    panel.convertXScreenToData(sz.width), sz.width);
                int lastX = 0;
                int trueLen = (subsignals.length + 3) / 4 * 4;
                int [] values = new int[trueLen];
                for(int i=0; i<view.getNumEvents(); i++)
                {
                    double xValue = view.getTime(i);
                    RangeSample<BusSample<DigitalSample> > rs = view.getSample(i);
                    // XXX: shouldn't ignore the max!
                    BusSample<DigitalSample> bs = rs.getMin();
                    int invert = trueLen - 1;
                    for(int j=0; j<bs.getWidth(); j++)
                    {
                        switch (DigitalSample.getState(bs.getTrace(j)) & Stimuli.LOGIC)
                        {
                            case Stimuli.LOGIC_LOW:  values[invert] = 0;   break;
                            case Stimuli.LOGIC_HIGH: values[invert] = 1;   break;
                            case Stimuli.LOGIC_X:    values[invert] = 2;   break;
                            case Stimuli.LOGIC_Z:    values[invert] = 3;   break;
                            default:                 values[invert] = 4;   break;
                        }
                        invert--;
                    }

                    int x = panel.convertXDataToScreen(xValue);
                    if (x >= panel.getVertAxisPos())
                    {
                        if (x < panel.getVertAxisPos()+5)
                        {
                            // on the left edge: just draw the "<"
                            if (panel.processALine(g, x, hei/2, x+5, hei-5, bounds, forPs, selectedObjects, ws, -1)) return;
                            if (panel.processALine(g, x, hei/2, x+5, 5, bounds, forPs, selectedObjects, ws, -1)) return;
                        } else
                        {
                            // bus change point: draw the "X"
                            if (panel.processALine(g, x-5, 5, x+5, hei-5, bounds, forPs, selectedObjects, ws, -1)) return;
                            if (panel.processALine(g, x+5, 5, x-5, hei-5, bounds, forPs, selectedObjects, ws, -1)) return;
                        }
                        if (lastX+5 < x-5)
                        {
                            // previous bus change point: draw horizontal bars to connect
                            if (panel.processALine(g, lastX+5, 5, x-5, 5, bounds, forPs, selectedObjects, ws, -1)) return;
                            if (panel.processALine(g, lastX+5, hei-5, x-5, hei-5, bounds, forPs, selectedObjects, ws, -1)) return;
                        }

                        String valString = "0x";
                        for(int j=0; j<trueLen; j++)
                        {
                        	if (values[j] == 4) { valString = "?";   break; }
                        	if (values[j] == 3) { valString = "Z";   break; }
                        	if (values[j] == 2) { valString = "X";   break; }
                        }
                        if (valString.length() > 1)
                        {
                        	for(int j=0; j<trueLen; j += 4)
                        	{
                        		int hexDigit = 0;
                        		if (values[j] == 1) hexDigit += 8;
                        		if (values[j+1] == 1) hexDigit += 4;
                        		if (values[j+2] == 1) hexDigit += 2;
                        		if (values[j+3] == 1) hexDigit += 1;
                        		if (hexDigit < 10) valString += (char)('0'+hexDigit); else
                        			valString += (char)('A'+hexDigit-10);
                        	}
                        }
                        if (g != null)
                        {
                            g.setFont(panel.getWaveWindow().getFont());
                            GlyphVector gv = panel.getWaveWindow().getFont().createGlyphVector(panel.getWaveWindow().getFontRenderContext(), valString);
                            Rectangle2D glyphBounds = gv.getLogicalBounds();
                            int textHei = (int)glyphBounds.getHeight();
                            g.drawString(valString, x+2, hei/2+textHei/2);
                        }
                        if (forPs != null)
                        {
                            Point2D [] pts = new Point2D[1];
                            pts[0] = new Point2D.Double(x+2, hei/2);
                            Poly poly = new Poly(pts);
                            poly.setStyle(Poly.Type.TEXTLEFT);
                            poly.setTextDescriptor(TextDescriptor.EMPTY.withAbsSize(8));
                            poly.setString(valString);
                            forPs.add(poly);
                        }
                    }
                    lastX = x;
                }

                // connect lines to the right
				int wid = sz.width;
                if (lastX+5 < wid)
                {
	                if (panel.processALine(g, lastX+5, 5, wid, 5, bounds, forPs, selectedObjects, ws, -1)) return;
	                if (panel.processALine(g, lastX+5, hei-5, wid, hei-5, bounds, forPs, selectedObjects, ws, -1)) return;
                }
            }
        };
    }
}