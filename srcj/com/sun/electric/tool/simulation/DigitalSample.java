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
 *  An implementation of Sample for digital data; supports HIGH/LOW
 *  (at the IEEE-standard drive strengths) as well as X and Z (at only
 *  a single drive strength).
 *
 *  Instances of DigitalSample are interned for efficiency; there is
 *  only ever one instance of DigitalSample with any given
 *  value/strength, so == can be used for equality tests.
 */
public class DigitalSample implements Sample, Comparable {

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
     *  Possible signal values; sort order is low-to-high-to-unknown,
     *  with high-impedence between low and high.
     */
    public static enum Value {
        LOW(-1),
        Z(0),
        HIGH(1),
        X(2);
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
    public int compareTo(Object o) { return (getByteRepresentation() & 0xff) - (((DigitalSample)o).getByteRepresentation() & 0xff); }

    public boolean isLogic0() { return value==Value.LOW; }
    public boolean isLogic1() { return value==Value.HIGH; }
    public boolean isLogicX() { return value==Value.X; }
    public boolean isLogicZ() { return value==Value.Z; }

    private byte getByteRepresentation() { return (byte)(((value.v+1) << 3) | strength.v); }
    private static DigitalSample fromByteRepresentation(byte b) { return cache[(b >> 3) & 3][b & 7]; }

    public static final UnboxedComparable<DigitalSample> unboxer = new UnboxedComparable<DigitalSample>() {
        public int getSize() { return 1; }
        public DigitalSample deserialize(byte[] buf, int ofs) { return fromByteRepresentation(buf[ofs]); }
        public void serialize(DigitalSample v, byte[] buf, int ofs) { buf[ofs] = v.getByteRepresentation(); }
        public int compare(byte[] buf1, int ofs1, byte[] buf2, int ofs2) { return (buf1[ofs1]&0xff)-(buf2[ofs2]&0xff); }
    };

    // Backward-Compatibility //////////////////////////////////////////////////////////////////////////////

    /**
     * The only thing that really matters to ALS is that the four
     * strengths:
     *
     *           OFF_STRENGTH
     *           NODE_STRENGTH
     *           GATE_STRENGTH
     *           VDD_STRENGTH
     *
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
     *
     *    -Steve
     */
    public int toOldStyle() {
        int ret = 0;
        switch(value) {
            case LOW: ret |= Stimuli.LOGIC_LOW; break;
            case HIGH: ret |= Stimuli.LOGIC_HIGH; break;
            case X: ret |= Stimuli.LOGIC_X; break;
            case Z: ret |= Stimuli.LOGIC_Z; break;
        }
        switch(strength) {
            case SUPPLY_DRIVE:       ret |= Stimuli.VDD_STRENGTH;  break;
            case STRONG_PULL:        ret |= Stimuli.NODE_STRENGTH; break;
            case PULL_DRIVE:         ret |= Stimuli.NODE_STRENGTH; break;
            case LARGE_CAPACITANCE:  ret |= Stimuli.GATE_STRENGTH; break;
            case WEAK_DRIVE:         ret |= Stimuli.GATE_STRENGTH; break;
            case MEDIUM_CAPACITANCE: ret |= Stimuli.GATE_STRENGTH; break;
            case SMALL_CAPACITANCE:  ret |= Stimuli.OFF_STRENGTH;  break;
            case HIGH_IMPEDANCE:     ret |= Stimuli.OFF_STRENGTH;  break;
        }
        return ret;
    }
    
    public static DigitalSample fromOldStyle(int i) {
        Strength strength = null;
        Value value = null;
        switch(i & Stimuli.LOGIC) {
            case Stimuli.LOGIC_LOW:  value = Value.LOW; break;
            case Stimuli.LOGIC_HIGH: value = Value.HIGH; break;
            case Stimuli.LOGIC_X:    value = Value.X; break;
            case Stimuli.LOGIC_Z:    value = Value.Z; break;
            default: throw new RuntimeException("unknown value: " + (i & Stimuli.LOGIC));
        }
        switch(i & Stimuli.STRENGTH) {
            case Stimuli.OFF_STRENGTH:  strength = Strength.SMALL_CAPACITANCE; break;
            case Stimuli.NODE_STRENGTH: strength = Strength.STRONG_PULL;       break;
            case Stimuli.GATE_STRENGTH: strength = Strength.LARGE_CAPACITANCE; break;
            case Stimuli.VDD_STRENGTH:  strength = Strength.SUPPLY_DRIVE;      break;
            default: throw new RuntimeException("unknown strength: " + (i & Stimuli.STRENGTH));
        }
        return getSample(value, strength);
    }

}


