/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NewSignal.java
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

/**
 *  An implementation of SimulationSample for scalar data.  Holds a
 *  double internally.
 */
public class ScalarSample implements SimulationSample {

    private double value;

    public ScalarSample() { this(0); }
    public ScalarSample(double value) { this.value = value; }

    public double getValue() {
        return value;
    }

    public boolean equals(Object o) {
        if (o==null || !(o instanceof SimulationSample)) return false;
        ScalarSample ss = (ScalarSample)o;
        return ss.value == value;
    }

    public int hashCode() {
        long l = Double.doubleToLongBits(value);
        return ((int)(l & 0xffffffff)) ^ ((int)((l >> 32) & 0xffffffff));
    }

    public int compareTo(Object o) {
        if (o==null || !(o instanceof SimulationSample))
            throw new RuntimeException("impossible!");
        ScalarSample ss = (ScalarSample)o;
        return Double.compare(value, ss.value);
    }

    public boolean isLogicX() {
        return value == Double.POSITIVE_INFINITY;
    }

    public boolean isLogicZ() {
        return value == Double.NEGATIVE_INFINITY;
    }

}


