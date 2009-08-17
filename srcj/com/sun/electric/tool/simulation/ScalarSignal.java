/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ScalarSignal.java
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

/**
 *  A ScalarSignal holds a signal which has a scalar value at any
 *  given point in time, and for which a piecewise linear
 *  approximation can be obtained for any given [t0,t1]x[v0,v1]
 *  window.
 *
 *  The API exposes doubles for most methods, but subclasses will
 *  often store only floats in order to preserve memory.
 *
 *  Eventually an implementation of this class will be offered which
 *  reads data in random-access fashion from an indexed file on disk
 *  (probably a B-Tree); this indexed file will be built in streaming
 *  fashion (constant memory usage) from a non-indexed simulation
 *  input format.  Subsequent enhancements are likely to include the
 *  ability to invoke methods on the class while the streaming
 *  conversion is taking place; attempts to invoke methods which
 *  require data not yet read will simply block until that part of the
 *  stream is processed.
 */
public interface ScalarSignal {

    /** value used to represent logic "X" (unknown) */
    public static final double LOGIC_X = Double.POSITIVE_INFINITY;

    /** value used to represent logic "Z" (high impedence) */
    public static final double LOGIC_Z = Double.NEGATIVE_INFINITY;

    /**
     *  An Approximation is a collection of events indexed by natural
     *  numbers.  An event is an ordered pair of rational numbers
     *  consisting of a time and a value.  All times share a common
     *  denominator, as do all values.  Times are guaranteed to be
     *  monotonic.
     *
     *  The following are true except for rounding errors:
     *
     *    getTime(i)  = getTime(0)  + getTimeNumerator(i)/getTimeDenominator()
     *    getValue(i) = getValue(0) + getValueNumerator(i)/getValueDenominator()
     *
     *  The time-distance between events is NOT guaranteed to be
     *  uniform.  However, the instances of Approximation returned by
     *  getApproximation(DDIDDI) <i>do</i> make this guarantee --
     *  in particular, those instances promise that for all x,
     *  getTimeNumerator(i)==i.  Instances returned by other methods
     *  do not offer this guarantee.
     */
    public static interface Approximation {
        /** the number of indices ("events") in this approximation */   int    getNumEvents();
        /** the absolute time of the event in question */               double getTime(int event);
        /** the absolute value of the event in question */              double getValue(int event);
        /** the numerator of the time of the specified event */         int    getTimeNumerator(int event);
        /** the numerator of the value of the specified event */        int    getValueNumerator(int event);
        /** the common denominator of all times */                      int    getTimeDenominator();
        /** the common denominator of all values */                     int    getValueDenominator();
        /** returns the index of the event having the least value */    int    getEventWithMinValue();
        /** returns the index of the event having the greatest value */ int    getEventWithMaxValue();
    }

    /**
     * Returns an Approximation in which:
     *
     *       getValueDenominator() = vd
     *              getNumEvents() = td + 1
     *                  getTime(0) = t0
     *   getTime(getNumEvents()-1) = t1
     *         getTimeNumerator(i) = i
     *        getTimeDenominator() = td
     *
     * Together, the last two guarantees ensure that the time
     * components of events are uniformly spaced, with the first event
     * at t0 and the last event at t1.
     *
     * Subject to these constraints, the Approximation returned will
     * be the one which most accurately represents the data in the
     * window [t0,t1]x[v0,v1].
     *
     * If td==0, the number of time points returned will be that which
     * is "most natural" for the underlying data.
     *
     * If vd==0, the value denominator will be that which is "most
     * natural" for the underlying data.
     */
    ScalarSignal.Approximation getApproximation(double t0, double t1, int tr,
                                                double v0, double v1, int vr);

    /**
     *  Returns an Approximation which is "most natural" for
     *  the data; this should be the Approximation which
     *  causes no loss in data fidelity.
     */
    ScalarSignal.Approximation getPreferredApproximation();
}


