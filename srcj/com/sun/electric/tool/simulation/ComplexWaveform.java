/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Waveform.java
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
 * Complex Waveform - piecewise linear complex function of time
 * There are methods which return realwaveforms which are
 * real-part, imag-part, amplitude and phase of this complex waveform.
 * This class also implements Waveform class as real waveform of amplitude.
 */
public class ComplexWaveform extends WaveformImpl {
    private final WaveformImpl realWaveform;
    private final WaveformImpl imagWaveform;
    private final WaveformImpl phaseWaveform;
    
    /**
     * Constructs complex waveform by time/real/imag tripples.
     * All three array arguments must have the same length. 
     * @param time time array
     * @param realValue array of real parts
     * @param imagValue array of imag parts
     * @throws IllegalArgumentException if arrays don't have the same length
     */
    public ComplexWaveform(double[] time, double[] realValue, double[] imagValue) {
        super(time, calcAmplitude(realValue, imagValue));
        realWaveform = new WaveformImpl(time, realValue);
        imagWaveform = new WaveformImpl(time, imagValue);
        
        // calc phase
        double[] phaseValue = new double[time.length];
        for (int i = 0; i < phaseValue.length; i++)
            phaseValue[i] = Math.atan2(imagValue[i], realValue[i]);
        phaseWaveform = new WaveformImpl(time, phaseValue);
    }
    
    /**
     * Return real waveform which is real part of this complex waveform.
     * @return real waveform which is real part of this complex waveform.
     */
    public Waveform getReal() { return realWaveform; }
    
    /**
     * Return real waveform which is imag part of this complex waveform.
     * @return real waveform which is imag part of this complex waveform.
     */
    public Waveform getImag() { return imagWaveform; }
    
    /**
     * Return real waveform which is amplitude of this complex waveform.
     * @return real waveform which is amplitude of this complex waveform.
     */
    public Waveform getAmplitude() { return this; }
    
    /**
     * Return real waveform which is phase of this complex waveform.
     * The phase is in the range of -<i>pi</i> to <i>pi</i>
     * @return real waveform which is phase of this complex waveform.
     */
    public Waveform getPhase() { return phaseWaveform; }
    
    private static double[] calcAmplitude(double[] realValue, double[] imagValue) {
        if (realValue.length != imagValue.length)
            throw new IllegalArgumentException();
        double[] amplitude = new double[realValue.length];
        for (int i = 0; i < amplitude.length; i++)
            amplitude[i] = Math.hypot(realValue[i], imagValue[i]);
        return amplitude;
    }
}
