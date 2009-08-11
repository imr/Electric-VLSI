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
     *              getNumEvents() = (t1-t0)/td + 1
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
     */
    ScalarSignal.Approximation getApproximation(double t0, double t1, int tn,
                                                double v0, double v1, int vd);

    /**
     *  Returns an Approximation which is "most natural" for
     *  the data; this should be the Approximation which
     *  causes no loss in data fidelity.
     */
    ScalarSignal.Approximation getPreferredApproximation();
}


