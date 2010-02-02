/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AmpsVsVolts.java
 * Written by Tom O'Neill, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

import java.io.PrintWriter;

/**
 * Measures current as a function of voltage, writing results to file. The user
 * is free to change any of the member variables at any time, in order to
 * perform a measurement with different characteristics.
 */

public class AmpsVsVolts {

    /** Power supply channel supplying the voltage. */
    public PowerChannel supply;

    /** Current measurement device */
    public CurrentReadable ammeter;

    /** Minimum voltage in sweep, in Volts */
    public float voltsMin = 0.f;

    /**
     * Maximum voltage in sweep, in Volts. Must be greater than
     * <code>voltsMin</code>
     */
    public float voltsMax = 2.001f;

    /** Voltage step size in Volts, must be greater than zero */
    public float voltsStep = 0.2f;

    /** Number of seconds between voltage setting and current reading */
    public float settleTime = 1.f;

    /**
     * Specifies a current vs voltage measurement using the default voltage
     * range and <code>settleTime</code>.
     * 
     * @param supply
     *            Power supply channel supplying the voltage
     * @param ammeter
     *            Current measurement device
     */
    public AmpsVsVolts(PowerChannel supply, CurrentReadable ammeter) {
        this.supply = supply;
        this.ammeter = ammeter;
    }

    /**
     * Specifies a current vs voltage measurement.
     * 
     * @param supply
     *            Power supply channel supplying the voltage
     * @param ammeter
     *            Current measurement device
     * @param voltsMin
     *            Minimum voltage in sweep, in Volts
     * @param voltsMax
     *            Maximum voltage in sweep, in Volts
     * @param voltsStep
     *            Voltage step size in Volts, must be greater than zero
     * @param settleTime
     *            Number of seconds between voltage setting and current reading
     */
    public AmpsVsVolts(PowerChannel supply, CurrentReadable ammeter,
            float voltsMin, float voltsMax, float voltsStep, float settleTime) {
        if (voltsMax < voltsMin || voltsStep < 0.f) {
            Infrastructure.fatal("Voltage min=" + voltsMin + ", max="
                    + voltsMax + ", step=" + voltsStep
                    + ".  Require max>min and step>0");
        }
        this.supply = supply;
        this.ammeter = ammeter;
        this.voltsMin = voltsMin;
        this.voltsMax = voltsMax;
        this.voltsStep = voltsStep;
        this.settleTime = settleTime;
    }

    /**
     * Write the IV curve specified by this object to the provided file. At end,
     * sets voltage to 0 V.
     * 
     * @param file
     *            File to write the IV curve to
     */
    public void measure(PrintWriter file) {
        file.println("# (volts, amps) samples");
        for (float volts = voltsMin; volts < voltsMax; volts += voltsStep) {
            supply.setVoltageWait(volts);
            try { Thread.sleep( (int)(1000*settleTime) ); } catch (InterruptedException _) { }
            float amps = ammeter.readCurrent();
            System.out.println(volts + " V:  " + amps + " A");
            file.println(volts + " " + amps);
        }
        supply.setVoltageWait(0.f);
    }

    /** Unit test */
    public static void main(String[] args) {
    }

}
