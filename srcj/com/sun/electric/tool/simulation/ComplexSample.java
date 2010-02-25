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
import java.io.*;

/**
 *  An implementation of Sample for complex data.  Holds 
 *  two doubles internally: real and imaginary.
 */
public class ComplexSample implements Sample {

    private double real;
    private double imag;

    public ComplexSample() { this(0,0); }
    public ComplexSample(double real, double imag) { this.real = real; this.imag = imag; }

    public double getReal() { return real; }
    public double getImag() { return imag; }
    public double getAmplitude() { return Math.atan2(getImag(), getReal()); }

    public boolean equals(Object o) {
        if (o==null || !(o instanceof ComplexSample)) return false;
        ComplexSample cs = (ComplexSample)o;
        return cs.real==real && cs.imag==imag;
    }

    public int hashCode() {
        long l = Double.doubleToLongBits(real) ^ Double.doubleToLongBits(imag);
        return ((int)(l & 0xffffffff)) ^ ((int)((l >> 32) & 0xffffffff));
    }

    public boolean isLogicX() { return false; }
    public boolean isLogicZ() { return false; }

}


