/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: HP34401A.java
 * Written by Dave Hopkins and Tom O'Neill, Sun Microsystems.
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
 * API for controlling the HP 34401A digital multimeter.
 * <p>
 * Most users will be happy with the default voltage measurement parameters.
 * Some users may wish to use {@link #setVoltageRange}&nbsp;and {@link
 * #setVoltageResolution} to obtain better control of the measurement time and
 * resolution.
 * <p>
 * Expert users may even wish to control the integration time directly using
 * {@link #setVoltageNPLC}. For convenience, and to match the behavior of the
 * device itself, {@link #setVoltageResolution}&nbsp;overrides the integration
 * time set by {@link #setVoltageNPLC}. To control the two settings
 * independently, the user must call {@link #setVoltageNPLC}&nbsp;after
 * {@link #setVoltageResolution}.
 * <p>
 * Eventually the control of the resolution of the current measurement should be
 * the same as the control of the voltage.
 */
public class HP34401A extends Equipment implements CurrentReadable,
        VoltageReadable {

    /** Maximum current that the DVM can measure, value: 3 A */
    public static final float MAX_AMPS = 3.0f;

    /** The DC voltage measurement range, in Volts (or "MIN", "MAX", or "DEF") */
    private String voltageRange = "DEF";

    /** Number of times to read voltage in {@link #timeReadVoltage}. */
    private static final int NUM_TIMING_READS = 100;

    /**
     * The DC voltage measurement resolution, in Volts (or "MIN", "MAX", or
     * "DEF")
     */
    private String voltageResolution = "DEF";

    /**
     * Number of power line cycles to integrate over (or "MIN", "MAX", or
     * "DEF").
     */
    private String voltageNPLC = "DEF";

    /** Allowed non-numeric voltage measurement parameters */
    private static final String[] ALLOWED_WORDS = { "DEFAULT", "MINIMUM",
            "MAXIMUM" };

    /**
     * Minimum number of letters required for an {@link ALLOWED_WORDS}
     * &nbsp;member
     */
    private static final int MIN_WORD_LENGTH = 3;

    /** Identifier for measurement function during previous read */
    private int lastFunction = FUNCTION_UNDEFINED;

    /**
     * Value of {@link #lastFunction}&nbsp;when the function must be configured
     * before a measurement can be made. Occurs before first measurement, and
     * after a change in configuration.
     */
    private static final int FUNCTION_UNDEFINED = -1;

    /**
     * Value of {@link #lastFunction}&nbsp;when the device is configured for
     * voltage measurement
     */
    private static final int FUNCTION_VOLTAGE = 0;

    /**
     * Value of {@link #lastFunction}&nbsp;when the device is configured for
     * current measurement
     */
    private static final int FUNCTION_CURRENT = 1;

    /** Creates a new instance of HP34401A */
    public HP34401A(String name) {
        super(name);
        logInit("Initialized HP34401A named " + name);
    }

    /**
     * Set the DC voltage measurement range, in Volts. Supported values: "
     * <tt>MINimum</tt>", "<tt>MAXimum</tt>", or "<tt>DEFault</tt>",
     * or a number (lower-case characters optional). The range will be set to
     * the next highest supported value during the next call to
     * {@link #readVoltage}.
     * 
     * @param voltageRange
     *            The the DC voltage measurement range to set, in Volts.
     */
    public void setVoltageRange(String voltageRange) {
        testParameter(voltageResolution);
        this.voltageRange = voltageRange;
        if (lastFunction == FUNCTION_VOLTAGE)
            lastFunction = FUNCTION_UNDEFINED;
    }

    /**
     * Get the requested DC voltage measurement range, in Volts or as "
     * <tt>MINimum</tt>", "<tt>MAXimum</tt>", or "<tt>DEFault</tt>" .
     * 
     * @return Returns the requested DC voltage measurement range, in Volts
     */
    public String getVoltageRange() {
        return voltageRange;
    }

    /**
     * Queries the HP34401A device to obtain the DC voltage measurement range.
     * 
     * @return Returns the current DC voltage measurement range, in Volts
     */
    public float readVoltageRange() {
        write("VOLT:DC:RANG?");
        return readFloat(40);
    }

    /**
     * Set the DC voltage measurement resolution, in Volts. Supported values: "
     * <tt>MINimum</tt>", "<tt>MAXimum</tt>", or "<tt>DEFault</tt>",
     * or a number (lower-case characters optional). The resolution will be set
     * to the nearest supported value during the next call to
     * {@link #readVoltage}.
     * <p>
     * Use <tt>MAXimum</tt> for fast but low-accuracy measurements. For
     * convenience, sets integration time {@link #setVoltageNPLC}&nbsp;to
     * <tt>DEFault</tt> so that device chooses integration time appropriate
     * for the requested resolution.
     * 
     * @param voltageResolution
     *            The DC voltage measurement resolution to set.
     */
    public void setVoltageResolution(String voltageResolution) {
        testParameter(voltageResolution);
        this.voltageResolution = voltageResolution;
        this.voltageNPLC = "DEF";
        if (lastFunction == FUNCTION_VOLTAGE)
            lastFunction = FUNCTION_UNDEFINED;
    }

    /**
     * Get the requested DC voltage measurement resolution, in Volts.
     * 
     * @return Returns the requested DC voltage measurement resolution, in
     *         Volts.
     */
    public String getVoltageResolution() {
        return voltageResolution;
    }

    /**
     * Queries the HP34401A device to obtain the DC voltage measurement
     * resolution.
     * 
     * @return Returns the current DC voltage measurement resolution, in Volts.
     */
    public float readVoltageResolution() {
        write("VOLT:DC:RES?");
        return readFloat(40);
    }

    /**
     * Set the number of power line cycles to integrate the DC voltage
     * measurement over. Supported values: "<tt>MINimum</tt>", "
     * <tt>MAXimum</tt>", "<tt>DEFault</tt>", or a number (lower-case
     * characters optional). The actual integration time will be set to 0.02,
     * 0.2, 1, 10, or 100 cycles during the next call to {@link #readVoltage}.
     * <p>
     * The <tt>DEFault</tt> value causes the device to select an integration
     * time appropriate for the selected range and resolution, and is the best
     * value for most applications. Value is overridden by calls to
     * {@link #setVoltageResolution}. Change takes effect in next call to
     * {@link #readVoltage}.
     * 
     * @param voltageNPLC
     *            The requested DC voltage integration time in number of power
     *            cycles
     */
    public void setVoltageNPLC(String voltageNPLC) {
        testParameter(voltageResolution);
        this.voltageNPLC = voltageNPLC;
        if (lastFunction == FUNCTION_VOLTAGE)
            lastFunction = FUNCTION_UNDEFINED;
    }

    /**
     * Get the requested DC voltage integration time in number of power cycles.
     * 
     * @return Returns the requeseted DC voltage integration time in number of
     *         power cycles.
     */
    public String getVoltageNPLC() {
        return voltageNPLC;
    }

    /**
     * Queries the HP34401A device to obtain the DC voltage integration time in
     * number of power cycles.
     * 
     * @return Returns the current DC voltage integration time in number of
     *         power cycles.
     */
    public float readVoltageNPLC() {
        write("VOLT:DC:NPLC?");
        return readFloat(40);
    }

    /**
     * Issues fatal error message if an invalid voltage- or current- measurement
     * parameter is provided.
     * 
     * @param parameter
     *            Measurement parameter to test
     */
    private void testParameter(String parameter) {
        try {
            Float.parseFloat(parameter);
            return;
        } catch (NumberFormatException e) {
        }

        String cased = parameter.trim().toUpperCase();
        int length = cased.length();
        for (int ind = 0; ind < ALLOWED_WORDS.length; ind++) {
            if (cased.equals(ALLOWED_WORDS[ind].substring(0, length))) {
                return;
            }
        }
        Infrastructure.fatal("Invalid measurement parameter " + parameter
                + ".  Use \"MINimum\", \"MAXimum\", \"DEFault\", or a number.");
    }

    /**
     * Measure voltage using the measurement parameters specified by the methods
     * {@link #setVoltageRange},&nbsp; {@link #setVoltageResolution}, and
     * {@link #setVoltageNPLC}. For fast but less accurate measurements, call
     * {@link #setVoltageResolution}&nbsp;with parameter <tt>"MAX"</tt>
     * first.
     * 
     * @return measured voltage, in Volts
     */
    public float readVoltage() {
        if (lastFunction != FUNCTION_VOLTAGE) {
            logSet("Configuring voltage " + getVoltageRange() + ", "
                    + getVoltageResolution() + ", " + getVoltageNPLC());
            write("CONF:VOLT:DC " + getVoltageRange() + ","
                    + getVoltageResolution());
            if (getVoltageNPLC().startsWith("DEF") == false) {
                logSet("FYI, non-default NPLC: " + getVoltageNPLC());
                write("VOLT:DC:NPLC " + getVoltageNPLC());
            }
        }
        write("READ?");
        lastFunction = FUNCTION_VOLTAGE;
        return readFloat(40);
    }

    /**
     * Measures current using autoranging and default resolution (integration
     * time of 10 power line cycles--i.e., 1/6 sec)
     * 
     * @return current in Amps
     */
    public float readCurrent() {
        write("MEAS:CURR:DC?");
        lastFunction = FUNCTION_CURRENT;
        return readFloat(40);
    }

    /**
     * Measures current using range appropriate for <code>ampsExpected</code>,
     * and resolution of <code>ampsResolution</code>.
     * 
     * @param ampsExpected
     *            expected value of current in amps, for range setting
     * @param ampsResolution
     *            desired resolution for measurement, in amps
     * @return current in Amps
     */
    public float readCurrent(float ampsExpected, float ampsResolution) {
        //write("MEAS:CURR:DC? MIN,MIN");
        if (ampsExpected > MAX_AMPS) {
            Infrastructure.nonfatal("WARNING: ampsExpected=" + ampsExpected
                    + ", limiting to maximum: " + MAX_AMPS + " A");
            write("MEAS:CURR:DC? MAX," + ampsResolution);
        } else {
            write("MEAS:CURR:DC? " + ampsExpected + "," + ampsResolution);
        }
        String s = read(40);
        return Float.parseFloat(s);
    }

    /**
     * Measure how long it takes to measure the voltage
     * {@link #NUM_TIMING_READS}&nbsp;times for the current resolution etc.
     */
    private long timeReadVoltage() {
        System.out.println(readVoltageRange() + "," + readVoltageResolution()
                + "," + readVoltageNPLC());
        long startTime = java.lang.System.currentTimeMillis();
        for (int ind = 0; ind < NUM_TIMING_READS; ind++) {
            readVoltage();
        }
        long endTime = java.lang.System.currentTimeMillis();
        System.out.println("dt = " + (endTime - startTime));
        return (endTime - startTime);
    }

    /**
     * Measure how long it takes to measure the voltage
     * {@link #NUM_TIMING_READS}&nbsp;times for various resolutions etc.
     */
    private void timeVoltageReads() {
        System.out.println("\nFast:");
        setVoltageRange("DEF");
        setVoltageResolution("MAX");
        setVoltageNPLC("0.02");
        long time0_02 = timeReadVoltage();

        System.out.println("\nSame, but NPLC = 0.2");
        setVoltageNPLC("0.2");
        long time0_2 = timeReadVoltage();

        System.out.println("\nNPLC=0.2, resolution=DEF");
        setVoltageResolution("DEF");
        setVoltageNPLC("0.2");
        long timeDef0_02 = timeReadVoltage();

        System.out.println("\nSame, but NPLC = 1.0");
        setVoltageNPLC("1");
        long time1_0 = timeReadVoltage();

        System.out.println("\nMAX resolution times for 200 reads,"
                + " as function of NPLC:");
        System.out.println("0.02: " + time0_02);
        System.out.println("0.20: " + time0_2 + "; DEF-res: " + timeDef0_02);
        System.out.println("1.00: " + time1_0);
    }

    /**
     * Test the interplay between setting the various voltage-measurement
     * parameters.
     */
    private void testVoltageParameters() {
        System.out.println(readVoltageRange() + "," + readVoltageResolution()
                + "," + readVoltageNPLC());
        setVoltageRange("1");
        reportReadReport();
        setVoltageResolution("0.1");
        reportReadReport();
        setVoltageNPLC("1.353");
        reportReadReport();
        setVoltageNPLC("1");
        reportReadReport();
        setVoltageResolution("MAX");
        reportReadReport();
        setVoltageResolution("DEFRAG");
        reportReadReport();
        setVoltageResolution("0.00000J1");
        reportReadReport();
    }

    /**
     * Report the current voltage measurement parameters, read the voltage, then
     * report again.
     */
    private void reportReadReport() {
        System.out.println(readVoltageRange() + "," + readVoltageResolution()
                + "," + readVoltageNPLC());
        System.out.println(readVoltage() + " V");
        System.out.println(readVoltageRange() + "," + readVoltageResolution()
                + "," + readVoltageNPLC());
    }

    public static void main(String[] args) {
        Infrastructure.gpibControllers = new int[] { 1 };
        HP34401A dvm = new HP34401A("hBotDMM");
        dvm.setAllLogging(true);
        if (false) {
            dvm.timeVoltageReads();
            dvm.testVoltageParameters();
            float amps = 5.f;
            float resolution = 5.e-6f;
            for (int ind = 0; ind < 100000; ind++) {
                System.out.print(amps + ", " + resolution + ": ");
                System.out.println(dvm.readCurrent(amps, resolution));
                amps *= 0.1f;
                resolution *= 0.1f;
            }

            int nrep = 0;
            do {
                System.out.println(nrep + ": " + dvm.readCurrent());
                nrep++;
            } while (true);
        }
        dvm.testVoltageParameters();

    }
}
