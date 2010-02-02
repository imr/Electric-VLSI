/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IndirectSet.java
 * Written by Tom O'Neill, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation.test;

/**
 * Adjusts voltage on a provided power supply channel until the specified
 * current reaches the desired set point. The current and voltage must be
 * correlated. Assumes the current is a monotonic function of voltage, and that
 * the derivative is always nonzero.
 * <p>
 * 
 * We use a class instead of just a method so that caller can obtain resulting
 * voltage and current without performing time-consuming readVoltage() and
 * readCurrent() invocataions.
 */
public class IndirectSet {

    /** Maximum number of convergence steps */
    public static final int MAX_VOLTS_STEPS = 100;

    /**
     * Maximum fraction by which we ever expect to overshoot the target used to
     * set range for <code>readCurrent()</code>.
     */
    public static final float MAX_CURRENT_OVERSHOOT = 5.f;

    /**
     * Fraction of <code>ampsError</code> to set <code>readCurrent()</code>
     * resolution to.
     */
    public static final float EXCESS_RESOLUTION = 0.01f;

    /** Best guess so far at voltage required to produce <code>setAmps</code> */
    public float volts;

    /** Current measured when voltage equals <code>volts</code> */
    public float amps;

    /**
     * True if successfully set <code>amps</code> to approximately
     * <code>setAmps</code>
     */
    public boolean achievedCurrent = false;

    // Desired current set point
    private float setAmps;

    // Current and voltage pairs bounding setAmps. Voltage should stay
    // in range 0..maxVolts
    private float ampsLow = -Float.MAX_VALUE, voltsLow = 0.f;

    private float ampsHigh = Float.MAX_VALUE, voltsHigh;

    // The current, voltage pairs closest to setAmps (warmer=closer).
    // Initially assume 0 Volts produces 0 A, won't hurt if it isn't true.
    private float ampsHot = 0.f, voltsHot = 0.f;

    private float ampsWarm = -Float.MAX_VALUE, voltsWarm = 0.f;

    // Derivative of voltage wrt current, and its value in previous iteration
    private float voltsPerAmp, oldVoltsPerAmp = 0.f;

    // Resolution of voltage setting
    private float voltsResolution;

    // Previous value of volts
    private float oldVolts;

    /**
     * Adjust the voltage on <code>knob</code> until current on
     * <code>dial</code> reaches the setpoint. A safe non-zero starting value
     * <code>initVolts</code> must be provided.
     * <p>
     * Assumes <code>knob</code> provides the correct resolution for the power
     * supply. If the current readback is noisier than <code>ampsError</code>,
     * the routine may fail.
     * 
     * @param dial
     *            object providing current readback
     * @param setAmps
     *            desired current setpoint
     * @param ampsError
     *            allowed deviation from setAmps
     * @param knob
     *            object providing voltage control
     * @param initVolts
     *            initial guess at correct voltage, must be non-zero
     * @param maxVolts
     *            maximum allowed voltage
     * @see PowerChannel#getVoltageResolution
     */
    public IndirectSet(CurrentReadable dial, float setAmps, float ampsError,
            PowerChannel knob, float initVolts, float maxVolts) {

        this.setAmps = setAmps;
        volts = initVolts;
        voltsHigh = maxVolts;

        // Safety limit on size of voltage change in a single iteration
        float maxVoltsStep = Math.abs(0.2f * initVolts);
        
        // Range and resolution for readCurrent()
        float ampsMax = MAX_CURRENT_OVERSHOOT * setAmps;
        float ampsResolution = EXCESS_RESOLUTION * ampsError;

        voltsResolution = knob.getVoltageResolution();

        for (int ind = 0; ind < 100; ind++) {
            volts = Math.round(volts / voltsResolution) * voltsResolution;
            knob.setVoltageWait(volts);
            amps = dial.readCurrent(ampsMax, ampsResolution);
            System.out.println(ind + ": " + volts + " V, " + amps + " A");
            float ampsDiff = Math.abs(amps - setAmps);

            //System.out.println(volts + " V, " + amps + " A");

            if (ampsDiff <= ampsError) {
                achievedCurrent = true;
                return;
            }

            // Tighten bounds around setAmps if possible. If bounds within
            // voltage resolution, choose better endpoint and return.
            if (updateBounds(knob)) {
                achievedCurrent = true;
                return;
            }

            // Update record of current, voltage pairs to setAmps
            updateClosePairs(setAmps, ampsDiff);

            // Find slope of current-voltage relationship
            if (!updateVoltsPerAmp(ind)) {
                return;
            }

            // Compute new voltage assuming linear current-voltage relationship
            computeNextVolts(maxVoltsStep);

            // If new voltage outside bound around setAmps, then try the
            // midpoint of the bounded region instead
            if (volts < voltsLow || volts > voltsHigh) {
                System.err.println("Warning: volts=" + volts
                        + " outside bounds " + voltsLow + ".." + voltsHigh);
                volts = 0.5f * (voltsLow + voltsHigh);
            }

            System.out.println(ind + ": " + getState() + "\n");
            if (volts <= 0.f || volts > maxVolts) {
                Infrastructure.nonfatal("Voltage not converging, reached "
                        + volts + "V");
                return;
            }
        }

        Infrastructure.nonfatal("Voltage did not converge in "
                + IndirectSet.MAX_VOLTS_STEPS + " steps");
        return;
    }

    /**
     * Returns a string giving the complete state of the
     * {@link IndirectSet} object.
     *  
     * @return string giving the complete state of the object
     */
    public String getState() {
        String state = volts + " V, " + amps + " A; old=" + oldVolts + " V";
        state += "\n  low: " + voltsLow + " V, " + ampsLow + " A;";
        state += " high: " + voltsHigh + "V, " + ampsHigh + " A;";
        state += "\n  hot: " + voltsHot + "V, " + ampsHot + " A;";
        state += " warm: " + voltsWarm + "V, " + ampsWarm + " A;";
        state += "\n  voltsPerAmp: " + voltsPerAmp;
        state += "V, oldVoltsPerAmp: " + oldVoltsPerAmp;

        return state;
    }

    // Compute new voltage assuming linear current-voltage relationship.
    // Ensures that the magnitued of the change is at most maxVoltsStep, and at
    // least one voltage resolution unit
    private void computeNextVolts(float maxVoltsStep) {
        float voltsStep = (setAmps - amps) * voltsPerAmp;

        if (Math.abs(voltsStep) > maxVoltsStep) {
            if (voltsStep >= 0.f) {
                voltsStep = maxVoltsStep;
            } else {
                voltsStep = -maxVoltsStep;
            }
        }

        oldVolts = volts;
        volts += voltsStep;
        volts = Math.round(volts / voltsResolution) * voltsResolution;
        if (volts == oldVolts) {
            if (voltsStep > 0.f) {
                volts += voltsResolution;
            } else {
                volts -= voltsResolution;
            }
        }
    }

    // Find slope of current-voltage relationship. Returns true if
    // voltsPerAmp has same sign as previous reading.
    private boolean updateVoltsPerAmp(int ind) {
        boolean status = true;
        voltsPerAmp = (voltsHot - voltsWarm) / (ampsHot - ampsWarm);
        if (ind > 1 && (voltsPerAmp > 0.f && oldVoltsPerAmp < 0.f)
                || (voltsPerAmp < 0.f && oldVoltsPerAmp > 0.f)) {
            Infrastructure.nonfatal("voltsPerAmp (dV/dI) =" + voltsPerAmp
                    + ", was " + oldVoltsPerAmp);
            status = false;
        }
        oldVoltsPerAmp = voltsPerAmp;
        return status;
    }

    // Update record of current, voltage pairs to setAmps
    private void updateClosePairs(float setAmps, float ampsDiff) {
        /* Note actual data always overrides assumed value at (0 V, 0 A) */
        if (ampsDiff < Math.abs(ampsHot - setAmps)
                || (ampsHot == 0.f && voltsHot == 0.f)) {
            ampsWarm = ampsHot;
            voltsWarm = voltsHot;
            ampsHot = amps;
            voltsHot = volts;
        } else if (ampsDiff < Math.abs(ampsWarm - setAmps)) {
            ampsWarm = amps;
            voltsWarm = volts;
        }
    }

    // Tighten bounds around setAmps if possible. If we have
    // gotten voltage as close as it can be, choose closer bounds endpoint
    // and return true. Otherwise return false.
    private boolean updateBounds(PowerChannel knob) {
        if (amps <= setAmps && amps > ampsLow) {
            ampsLow = amps;
            voltsLow = volts;
        }
        if (amps > setAmps && amps < ampsHigh) {
            ampsHigh = amps;
            voltsHigh = volts;
        }
        if (Math.abs(voltsHigh - voltsLow) <= (1.001f * voltsResolution)) {
            if (Math.abs(ampsLow - setAmps) < Math.abs(ampsHigh - setAmps)) {
                volts = voltsLow;
                amps = ampsLow;
            } else {
                volts = voltsHigh;
                amps = ampsHigh;
            }
            knob.setVoltageWait(volts);
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
    }

}
