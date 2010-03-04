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
 *  An implementation of Sample for digital data.
 */
public class DigitalSample implements Sample, Comparable {

    private byte value;
    private DigitalSample(byte value) { this.value = value; }

    // no need for more than one instance of each...
    public static final DigitalSample LOGIC_0 = new DigitalSample((byte)0);
    public static final DigitalSample LOGIC_1 = new DigitalSample((byte)1);
    public static final DigitalSample LOGIC_X = new DigitalSample((byte)2);
    public static final DigitalSample LOGIC_Z = new DigitalSample((byte)3);

    public boolean equals(Object o) { return this==o; }
    public int hashCode() { return value & 0xff; }
    public int compareTo(Object o) {
        return (value & 0xff) - (((DigitalSample)o).value & 0xff);
    }

    public boolean isLogic0() { return value==0; }
    public boolean isLogic1() { return value==1; }
    public boolean isLogicX() { return value==2; }
    public boolean isLogicZ() { return value==3; }

    public static DigitalSample fromOldStyle(int i) {
        switch(i) {
            case Stimuli.LOGIC_LOW: return LOGIC_0;
            case Stimuli.LOGIC_HIGH: return LOGIC_1;
            case Stimuli.LOGIC_X: return LOGIC_X;
            case Stimuli.LOGIC_Z: return LOGIC_Z;
            default: return null;
        }
    }

    public static final Unboxed<DigitalSample> unboxer = new Unboxed<DigitalSample>() {
        public int getSize() { return UnboxedByte.instance.getSize(); }
        public DigitalSample deserialize(byte[] buf, int ofs) {
            return new DigitalSample(UnboxedByte.instance.deserialize(buf, ofs));
        }
        public void serialize(DigitalSample v, byte[] buf, int ofs) {
            UnboxedByte.instance.serialize(v.value, buf, ofs);
        }
    };
}


