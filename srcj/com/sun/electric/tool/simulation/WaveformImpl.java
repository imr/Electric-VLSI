/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WaveformImpl.java
 *
 * Copyright (c) 2007 Sun Microsystems and Static Free Software
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
 * Simple implementation of Waveform interface
 */
public class WaveformImpl implements Waveform {
    private double[] time;
    private double[] value;
    
    /**
     * Constructs waveform from time and value arrays. They must
     * have the same length.
     * @param time time array
     * @param value value array
     * @throws IllegalArgumentException if time and value arrays have different size
     */
    public WaveformImpl(double[] time, double[] value) {
        if (time.length != value.length)
            throw new IllegalArgumentException();
        this.time = time;
        this.value = value;
    }
    
    /**
     * Method to return the number of events in this signal.
     * This is the number of events along the horizontal axis, usually "time".
     * @return the number of events in this signal.
     */
    public int getNumEvents() {
        return time.length;
    }

    /**
     * Method to return the value of this signal at a given event index.
     * @param index the event index (0-based).
     * @param result double array of length 3 to return (time, lowValue, highValue)
     * If this signal is not a basic signal, return 0 and print an error message.
     */
    public void getEvent(int index, double[] result) {
        result[0] = time[index];
        result[1] = result[2] = value[index];
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Methods below are strictly for transitional purposes //////////////////////////////////////////////////////////////////////////////
    // INCREDIBLY INEFFICIENT!!!                            //////////////////////////////////////////////////////////////////////////////

    private NewSignal.Approximation<ScalarSample> approximation = null;

    public NewSignal.Approximation<ScalarSample> getApproximation(double t0, double t1, int tn,
                                                                     ScalarSample v0, ScalarSample v1, int vd) {
        throw new RuntimeException("not implemented");
    }

    public NewSignal.Approximation<ScalarSample>
        getPixelatedApproximation(double t0, double t1, int numRegions) {
        throw new RuntimeException("not implemented");
    }

    public synchronized NewSignal.Approximation<ScalarSample> getPreferredApproximation() {
        if (approximation==null) approximation = new ApproximationImpl();
        return approximation;
    }

    /**
     *  An approximation of the intersection of v(t) and the window
     *  [t0,t1]x[y0,y1] within integer grid "[0..tn]x[0..yn]".
     */
    private class ApproximationImpl implements NewSignal.Approximation<ScalarSample> {
        private final float[] times;
        private final float[] values;
        private final int eventWithMinValue;
        private final int eventWithMaxValue;

        public ApproximationImpl() {
            /*
            if (!(Signal.this instanceof AnalogSignal) && !(Signal.this instanceof DigitalSignal))
                throw new RuntimeException("impossible!");
                Waveform waveform = Signal.this instanceof AnalogSignal ? ((AnalogSignal)Signal.this).getWaveform() : null;
            */
            Waveform waveform = WaveformImpl.this;
            /*
            int count = waveform!=null
                ? waveform.getNumEvents()
                : ((DigitalSignal)Signal.this).getNumEvents();
            */
            int count = waveform.getNumEvents();

            // we fake a single point at (0,0) if there are no samples
            this.times  = new float[count==0 ? 1 : count];
            this.values = new float[count==0 ? 1 : count];

            double minValue = Double.MAX_VALUE;
            double maxValue = Double.MIN_VALUE;
            int    evmin = 0;
            int    evmax = 0;
            double[] result = new double[10 /*why?*/ ];
            for(int i=0; i<count; i++) {
                if (waveform != null) {
                    waveform.getEvent(i, result);
                    times[i]  = (float)result[0];
                    values[i] = (float)result[1];
                } else {
                    /*
                    times[i]  = (float)((DigitalSignal)Signal.this).getTime(i);
                    values[i] = (float)((DigitalSignal)Signal.this).getState(i);
                    */
                }
                if (values[i] < minValue) minValue = values[evmin = i];
                if (values[i] > maxValue) maxValue = values[evmax = i];
                if (i>0 && times[i] < times[i-1])
                    throw new RuntimeException("got non-monotonic sample data, and I haven't implement sorting yet");
            }
            this.eventWithMinValue = evmin;
            this.eventWithMaxValue = evmax;
        }

        public int getNumEvents() {
            return times.length;
        }
        public double             getTime(int index) {
            return times[index];
        }
        public ScalarSample       getSample(int index) {
            return new ScalarSample(values[index]);
        }
        public int getTimeNumerator(int index) {
            throw new RuntimeException("not implemented");
        }
        public int getTimeDenominator() {
            throw new RuntimeException("not implemented");
        }
        public int getEventWithMaxValue() {
            return eventWithMaxValue;
        }
        public int getEventWithMinValue() {
            return eventWithMinValue;
        }
    }

	//public Analysis.AnalysisType getAnalysisType() { return getAnalysis().getAnalysisType(); }
}
