/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Signal.java
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

/**
 *  A sample consisting of a range (a min and a max).  Example usage:
 *  a Signal<RangeSample<S>> gives the bounds of a Signal<S> over a
 *  period of time.  Signal.View<RangeSample<S>> is a very useful type.
 */
public class RangeSample<S extends Sample> implements Sample
{
    private final S min;
    private final S max;

    public RangeSample(S min, S max) { this.min = min; this.max = max; }

    public S getMin() { return min; }
    public S getMax() { return max; }

    public boolean equals(Object o)
    {
        if (o==null || !(o instanceof RangeSample<?>)) return false;
        RangeSample<?> rs = (RangeSample<?>)o;
        return rs.min.equals(min) && rs.max.equals(max);
    }

    public int hashCode() { return min.hashCode() ^ max.hashCode(); }

    public boolean isLogicX() { return false; }
    public boolean isLogicZ() { return false; }

    /** 
     *  There's a question here as to what the order on Ranges should
     *  be; logically the lub is the union of the ranges and the glb
     *  is the intersection, but that's not likely to be very useful
     *  in practice.
     */
    public Sample lub(Sample s)
    {
        throw new RuntimeException("not implemented");
    }

    /** 
     *  There's a question here as to what the order on Ranges should
     *  be; logically the lub is the union of the ranges and the glb
     *  is the intersection, but that's not likely to be very useful
     *  in practice.
     */
    public Sample glb(Sample s)
    {
        throw new RuntimeException("not implemented");
    }

    public double getMinValue() { return min.getMinValue(); }

    public double getMaxValue() { return max.getMaxValue(); }

}


