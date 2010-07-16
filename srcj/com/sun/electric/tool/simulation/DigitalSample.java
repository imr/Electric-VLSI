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

import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.geometry.btree.unboxed.LatticeOperation;
import com.sun.electric.database.geometry.btree.unboxed.Unboxed;
import com.sun.electric.tool.user.waveform.Panel;
import com.sun.electric.tool.user.waveform.WaveSignal;
import com.sun.electric.tool.user.waveform.Panel.WaveSelection;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;

/**
 *  An implementation of Sample for digital data; supports HIGH/LOW
 *  (at the IEEE-standard drive strengths) as well as X and Z (at only
 *  a single drive strength).
 *
 *  Instances of DigitalSample are interned for efficiency; there is
 *  only ever one instance of DigitalSample with any given
 *  value/strength, so == can be used for equality tests.
 */
public class DigitalSample implements Sample {

    // no need for more than one instance of each...
    public static final DigitalSample LOGIC_0;
    public static final DigitalSample LOGIC_1;
    public static final DigitalSample LOGIC_X;
    public static final DigitalSample LOGIC_Z;

    private static DigitalSample[][] cache;

    static {
        cache = new DigitalSample[4][];
        for(int i=0; i<4; i++) cache[i] = new DigitalSample[8];
        for(Value value : Value.values())
            for(Strength strength : Strength.values())
                if (!((value == Value.Z) ^ (strength == Strength.HIGH_IMPEDANCE)))
                    cache[value.v+1][strength.v] = new DigitalSample(value, strength);
        LOGIC_0 = getSample(Value.LOW,  Strength.STRONG_PULL);
        LOGIC_1 = getSample(Value.HIGH, Strength.STRONG_PULL);
        LOGIC_X = getSample(Value.X,    Strength.STRONG_PULL);
        LOGIC_Z = getSample(Value.Z,    Strength.HIGH_IMPEDANCE);
    }

    public static DigitalSample getSample(Value value, Strength strength) {
        if ((value == Value.Z) ^ (strength == Strength.HIGH_IMPEDANCE))
            throw new RuntimeException("Logic=Z and Strength=HIGH_IMPEDANCE may only be used together");
        return cache[value.v+1][strength.v];
    }

    private Value value;
    private Strength strength;

    private DigitalSample(Value value, Strength strength) {
        if ((value == Value.Z) ^ (strength == Strength.HIGH_IMPEDANCE))
            throw new RuntimeException("Logic=Z and Strength=HIGH_IMPEDANCE may only be used together");
        this.value = value;
        this.strength = strength;
    }

    /** 
     *  Possible signal values.
     */
    public static enum Value {
        HIGH(1),
        X(0),
        LOW(-1),
        Z(-1);
        private final int v;
        private Value(int v) { this.v = v; }
    }

    /** 
     *  These are the strength levels from the IEEE Verilog standard;
     *  they weren't just arbitrarily made up; sort order is
     *  weak-to-strong.
     */
    public static enum Strength {
        SUPPLY_DRIVE(7),
        STRONG_PULL(6),         // IEEE standard says "STRONG_PULL" is the default
        PULL_DRIVE(5),
        LARGE_CAPACITANCE(4),
        WEAK_DRIVE(3),
        MEDIUM_CAPACITANCE(2),
        SMALL_CAPACITANCE(1),
        HIGH_IMPEDANCE(0);      // valid only for LOGIC_Z
        private final int v;
        private Strength(int v) { this.v = v; }
    };

    public boolean equals(Object o) { return this==o; }
    public int hashCode() { return getByteRepresentation() & 0xff; }

    public Sample lub(Sample s) {
        if (!(s instanceof DigitalSample)) throw new RuntimeException("tried to call DigitalSample.lub("+s.getClass().getName()+")");
        DigitalSample ds = (DigitalSample)s;
        if (ds.value.v >= value.v && ds.strength.v >= strength.v) return ds;
        if (ds.value.v <= value.v && ds.strength.v <= strength.v) return this;
        return cache[Math.max(ds.value.v, value.v)+1][Math.max(ds.strength.v, strength.v)+1];
    }

    public Sample glb(Sample s) {
        if (!(s instanceof DigitalSample)) throw new RuntimeException("tried to call DigitalSample.glb("+s.getClass().getName()+")");
        DigitalSample ds = (DigitalSample)s;
        if (ds.value.v >= value.v && ds.strength.v >= strength.v) return this;
        if (ds.value.v <= value.v && ds.strength.v <= strength.v) return ds;
        return cache[Math.min(ds.value.v, value.v)+1][Math.min(ds.strength.v, strength.v)+1];
    }

    public boolean isLogic0() { return value==Value.LOW; }
    public boolean isLogic1() { return value==Value.HIGH; }
    public boolean isLogicX() { return value==Value.X; }
    public boolean isLogicZ() { return value==Value.Z; }

    private byte getByteRepresentation() { return (byte)(((value.v+1) << 3) | strength.v); }
    private static DigitalSample fromByteRepresentation(byte b) { return cache[(b >> 3) & 3][b & 7]; }

    public static final Unboxed<DigitalSample> unboxer = new Unboxed<DigitalSample>() {
        public int getSize() { return 1; }
        public DigitalSample deserialize(byte[] buf, int ofs) { return fromByteRepresentation(buf[ofs]); }
        public void serialize(DigitalSample v, byte[] buf, int ofs) { buf[ofs] = v.getByteRepresentation(); }
    };

    private static final LatticeOperation<DigitalSample> latticeOp =
        new LatticeOperation<DigitalSample>(unboxer) {
        public void lub(byte[] buf1, int ofs1, byte[] buf2, int ofs2, byte[] dest, int dest_ofs) {
            if (((buf1[ofs1]&0xff)-(buf2[ofs2]&0xff)) < 0)
                System.arraycopy(buf2, ofs2, dest, dest_ofs, unboxer.getSize());
            else
                System.arraycopy(buf1, ofs1, dest, dest_ofs, unboxer.getSize());
        }
        public void glb(byte[] buf1, int ofs1, byte[] buf2, int ofs2, byte[] dest, int dest_ofs) {
            if (((buf1[ofs1]&0xff)-(buf2[ofs2]&0xff)) < 0)
                System.arraycopy(buf1, ofs1, dest, dest_ofs, unboxer.getSize());
            else
                System.arraycopy(buf2, ofs2, dest, dest_ofs, unboxer.getSize());
        }
    };

    /**
     * Method for converting ALS levels to new DigitalSample values,
     * for backward compatibility.
     * The only thing that really matters to ALS is that the four strengths:
     *           OFF_STRENGTH
     *           NODE_STRENGTH
     *           GATE_STRENGTH
     *           VDD_STRENGTH
     * be in ascending strength.
     *
     * Clearly, VDD_STRENGTH is a supply-level, so your choice of
     * SUPPLY_DRIVE seems right. Node strength is (I believe) what
     * used to be appropriate for nMOS depletion transistors
     * (i.e. stronger than a normal gate signal).  Probably your
     * choice of STRONG_PULL is also right.  Gate strength is a
     * regular signal, so your choice of LARGE_CAPACITANCE may be
     * right, or it may be PULL_DIRVE.  I don't know.  As far as
     * OFF_STRENGTH goes, why not use HIGH_IMPEDENCE?  I really don't
     * know the difference between that and SMALL_CAPACITANCE, so it's
     * your choice here.
     *    -SMR
     */
    public static DigitalSample fromOldStyle(int i)
    {
        Strength strength = null;
        Value value = null;
        switch(i & Stimuli.LOGIC)
        {
            case Stimuli.LOGIC_LOW:  value = Value.LOW; break;
            case Stimuli.LOGIC_HIGH: value = Value.HIGH; break;
            case Stimuli.LOGIC_X:    value = Value.X; break;
            case Stimuli.LOGIC_Z:    return getSample(Value.Z, Strength.HIGH_IMPEDANCE);
            default: throw new RuntimeException("unknown value: " + (i & Stimuli.LOGIC));
        }
        switch(i & Stimuli.STRENGTH)
        {
            case Stimuli.OFF_STRENGTH:  strength = Strength.SMALL_CAPACITANCE; break;
            case Stimuli.NODE_STRENGTH: strength = Strength.STRONG_PULL;       break;
            case Stimuli.GATE_STRENGTH: strength = Strength.LARGE_CAPACITANCE; break;
            case Stimuli.VDD_STRENGTH:  strength = Strength.SUPPLY_DRIVE;      break;
            default: throw new RuntimeException("unknown strength: " + (i & Stimuli.STRENGTH));
        }
        return getSample(value, strength);
    }

    public static int getState(Signal.View<DigitalSample> view, int index)
    {
        DigitalSample ds = view.getSample(index);
        return getState(ds);
    }

	public static int getState(DigitalSample ds)
	{
        if (ds.isLogic0()) return Stimuli.LOGIC_LOW;
        if (ds.isLogic1()) return Stimuli.LOGIC_HIGH;
        if (ds.isLogicX()) return Stimuli.LOGIC_X;
        if (ds.isLogicZ()) return Stimuli.LOGIC_Z;
        throw new RuntimeException("ack!");
    }

    public static MutableSignal<DigitalSample> createSignal(SignalCollection sc, Stimuli sd, String signalName,
    	String signalContext)
    {
        return new BTreeSignal<DigitalSample>(sc, sd, signalName, signalContext, true,
        	BTreeSignal.getTree(unboxer, latticeOp))
        {
            public void plot(Panel panel, Graphics g, WaveSignal ws, Color light, List<PolyBase> forPs,
            	Rectangle2D bounds, List<WaveSelection> selectedObjects, Signal<?> xAxisSignal)
            {
                Dimension sz = panel.getSize();
                int hei = sz.height;

				// a simple digital signal
				int lastx = panel.getVertAxisPos();
				int lastState = 0;
                Signal<DigitalSample> ds = (Signal<DigitalSample>)ws.getSignal();
                Signal.View<DigitalSample> view = ds.getExactView();
				int numEvents = view.getNumEvents();
				int lastLowy = 0, lastHighy = 0;
				for(int i=0; i<numEvents; i++)
				{
					double xValue = view.getTime(i);
					int x = panel.convertXDataToScreen(xValue);
					if (SimulationTool.isWaveformDisplayMultiState() && g != null)
					{
						if (panel.getWaveWindow().getPrintingMode() == 2) g.setColor(Color.BLACK); else
						{
							switch (getState(view, i) & Stimuli.STRENGTH)
							{
								case Stimuli.OFF_STRENGTH:  g.setColor(panel.getWaveWindow().getOffStrengthColor());    break;
								case Stimuli.NODE_STRENGTH: g.setColor(panel.getWaveWindow().getNodeStrengthColor());   break;
								case Stimuli.GATE_STRENGTH: g.setColor(panel.getWaveWindow().getGateStrengthColor());   break;
								case Stimuli.VDD_STRENGTH:  g.setColor(panel.getWaveWindow().getPowerStrengthColor());  break;
							}
						}
					}
					int state = getState(view, i) & Stimuli.LOGIC;
					int lowy = 0, highy = 0;
					switch (state)
					{
						case Stimuli.LOGIC_HIGH:
							lowy = highy = 5;
							break;
						case Stimuli.LOGIC_LOW:
							lowy = highy = hei-5;
							break;
						case Stimuli.LOGIC_X:
							lowy = 5;   highy = hei-5;
							break;
						case Stimuli.LOGIC_Z:
							lowy = (hei-10) / 3 + 5;   highy = hei - (hei-10) / 3 - 5;
							break;
					}
					if (g != null && !SimulationTool.isWaveformDisplayMultiState()) g.setColor(Color.RED);
					if (i != 0)
					{
						if (state != lastState)
						{
							if (panel.processALine(g, x, Math.min(lowy, lastLowy), x, Math.max(lowy, lastLowy), bounds, forPs, selectedObjects, ws, -1)) return;
						}
					}
					if (g != null && !SimulationTool.isWaveformDisplayMultiState())
					{
						if (lastState == Stimuli.LOGIC_Z) g.setColor(Color.GREEN);
					}
					if (lastLowy == lastHighy)
					{
						if (panel.processALine(g, lastx, lastLowy, x, lastLowy, bounds, forPs, selectedObjects, ws, -1)) return;
					} else
					{
						if (panel.processABox(g, lastx, lastLowy, x, lastHighy, bounds, forPs, selectedObjects, ws, false, 0)) return;
					}
					if (i >= numEvents-1)
					{
						if (g != null && !SimulationTool.isWaveformDisplayMultiState())
						{
							if (state == Stimuli.LOGIC_Z) g.setColor(Color.GREEN); else g.setColor(Color.RED);
						}
						int wid = sz.width;
						if (lowy == highy)
						{
							if (panel.processALine(g, x, lowy, wid-1, lowy, bounds, forPs, selectedObjects, ws, -1)) return;
						} else
						{
							if (panel.processABox(g, x, lowy, wid-1, highy, bounds, forPs, selectedObjects, ws, false, 0)) return;
						}
					}
					lastx = x;
					lastLowy = lowy;
					lastHighy = highy;
					lastState = state;
				}
            }
        };
    }
}
