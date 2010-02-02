/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Pst3202.java
 * Written by Ajanta Chakraborty, Sun Microsystems.
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
 * Low-level control of Instek PST 3202 power supply. You
 * should instantiate {@link Pst3202Channel} instead of this class.
 * That will allow easier switching between
 * power supplies.
 */
class Pst3202 extends Equipment {

    String s = new String("null");

    /** Resolution of each channel in Volts */
    private static final float[] VOLTAGE_RESOLUTION = { 0.01f, 0.01f, 0.002f };

    /** Number of channels (value = 3) */
    public static final int NUM_CHANNELS = 3;

    /** Delay after each command, in seconds */
    public static final float GPIB_DELAY = 0.2f;
    
    /** Maximum number of errors to print out before stopping */
    public static final int MAX_NUM_ERRORS = 20;

    /**
     * Creates a new instance of power suppy. Clears the error and other status
     * registers.
     */
    public Pst3202(String name) {
        super(name);
        logInit("  Initializing Pst3202 " + name);
        try { Thread.sleep((int)(1000*GPIB_DELAY)); } catch (InterruptedException _) { }
        writeAndWait(":*CLS");
        //        System.out.println("Error4: " + readError());
        //System.out.println("Error5: " + readError());
        testConnection();

    }

    /**
     * Returns string indicating state of one of the channels
     * 
     * @throws IllegalArgumentException
     *             if channel not in range 1..3
     */
    public String getState(int channel) {
        checkChannel(channel);
        String result;
        result = "Set " + getVoltageSetpoint(channel) + " V, "
                + getCurrentSetpoint(channel) + " A\n";
        result += "Got " + readVoltage(channel) + " V, " + readCurrent(channel)
                + " A";
        return result;
    }

    static void checkChannel(int channel) {
        if (channel < 1 || channel > NUM_CHANNELS) {
            throw new IllegalArgumentException("Channel " + channel
                    + " outside allowed range 1.." + NUM_CHANNELS);
        }
    }

    private void writeAndWait(String data) {
        super.write(data);
        try { Thread.sleep((int)(1000*GPIB_DELAY)); } catch (InterruptedException _) { }
    }
    
    public void write(String data) {
        writeAndWait(data);
    }
        
    /**
     * Returns voltage resolution of channel
     * 
     * @param channel
     *            Supply channel number
     * @return Voltage resoluton of channel
     * @throws IllegalArgumentException
     *             if channel not in range 1..3
     */
    public static float getVoltageResolution(int channel) {
        checkChannel(channel);
        return VOLTAGE_RESOLUTION[channel - 1];
    }

    void testConnection() {
        writeAndWait("*idn?");
        s = read(200).trim();
        //s = s.substring(0,s.length()-1);
        System.out.println("idn " + s);
    } //end testConnection

    String readError() {
        write("SYST:ERR?");
        s = read(200);
        return s;
    }

    void handleError() {
        if (isDisabled())
            return;
        String error;
        for (int ind = 0; ind < MAX_NUM_ERRORS; ind++) {
            error = readError();
            if (error.length() <= 0) {
                System.err
                        .println("Pst3202.handleError() no reply from supply");
            }
            System.err.println("PST 3202 reports error " + error);
            if (error.equals("0, \"No error\""))
                return;
        }
        Infrastructure.nonfatal("Printed " + MAX_NUM_ERRORS
                + " errors from PST 3202");
    }

    /**
     * Measures the voltage on one of the 3 channels
     * 
     * @throws IllegalArgumentException
     *             if channel not in range 1..3
     */
    public float readVoltage(int channel) {
        checkChannel(channel);
        write(":CHAN" + channel + ":MEAS:VOLT ?");
        s = read(200);
        if (s.length() <= 0) {
            handleError();
            return -1.0f;
        }
        return Float.parseFloat(s);
    }

    /**
     * Reads back the voltage setpoint from one of the 3 channels
     * 
     * @throws IllegalArgumentException
     *             if channel not in range 1..3
     */
    public float getVoltageSetpoint(int channel) {
        checkChannel(channel);
        logOther("Reading voltage setpoint on Pst3202 " + getName()
                + ", channel " + channel);
        write(":CHAN" + channel + ":VOLT?");
        s = read(200).trim();
        if (s.length() <= 0) {
            handleError();
            return -1.0f;
        }
        return Float.parseFloat(s);
    }

    /**
     * Changes the voltage setpoint on channel <code>channel</code> to
     * <code>setVolts</code> Volts.
     * 
     * @throws IllegalArgumentException
     *             if channel not in range 1..3
     */
    public boolean setVoltage(int channel, float setVolts) {
        checkChannel(channel);
        writeAndWait(":CHAN" + channel + ":VOLT " + setVolts);
        return true;
    }

    /**
     * Changes the voltage setpoint on channel <code>channel</code> to
     * <code>setVolts</code> Volts. Verifies that it the setpoint was changed
     * successfully. Bonus points: the delay for verification ensures that a
     * rapid sequence of invocations do not overwhelm the PST 3202's command
     * buffer.
     * <p>
     * I don't expect anybody to actually use this method, I'm just keeping this
     * code around in case we need it later.
     * 
     * @throws IllegalArgumentException
     *             if channel not in range 1..3
     */
    public boolean setVoltageCheck(int channel, float setVolts) {
        checkChannel(channel);
        write(":CHAN" + channel + ":VOLT " + setVolts + ";VOLT?");
        s = read(200);
        if (s.length() <= 0) {
            handleError();
            return false;
        }
        float volts = Float.parseFloat(s);
        if (Math.abs(volts - setVolts) > 1.0001 * getVoltageResolution(channel)) {
            Infrastructure
                    .nonfatal(this + ": requested voltage setpoint is"
                            + setVolts + " V, but setpoint obtained is "
                            + volts + " V");
            return false;
        }
        return true;
    }

    /**
     * Measures the current on one of the 3 channels
     * 
     * @throws IllegalArgumentException
     *             if channel not in range 1..3
     */
    public float readCurrent(int channel) {
        checkChannel(channel);
        write(":CHAN" + channel + ":MEAS:CURR ?");
        String s = read(10);
        if (s.length() <= 0) {
            handleError();
            return -1.0f;
        }
        return Float.parseFloat(s);
    }

    /**
     * Reads back the current setpoint from one of the three channels
     * 
     * @throws IllegalArgumentException
     *             if channel not in range 1..3
     */
    public float getCurrentSetpoint(int channel) {
        checkChannel(channel);
        write(":CHAN" + channel + ":CURR ?");
        String s = read(10);
        if (s.length() <= 0) {
            handleError();
            return -1.0f;
        }
        return Float.parseFloat(s);
    }
    
    /**
     * Clear internal or device functions of the device. Among other things,
     * this should clear device GPIB error conditions and allow its use again.
     * For some devices, it appears that the clear does not work if it is too
     * soon after the error occurs.
     */
    public void clear() {
        super.clear();
        try { Thread.sleep((int)(1000*GPIB_DELAY)); } catch (InterruptedException _) { }
    }


    /**
     * Changes the current setpoint on channel <code>channel</code> to
     * <code>setAmps</code> Amps.
     * 
     * @throws IllegalArgumentException
     *             if channel not in range 1..3
     */
    public void setCurrent(int channel, float setAmps) {
        checkChannel(channel);
        writeAndWait(":CHAN" + channel + ":CURR " + setAmps);
    }

    public void switchOnOff(int state) {
        write(":OUTPUT:STATE " + state);
        try { Thread.sleep((int)(1000*GPIB_DELAY)); } catch (InterruptedException _) { }
        write(":OUTPUT:STATE?");
        s = read(200).trim();
        System.out.println("state " + s);
    }

    /**
     * Unit test verifies that voltage setpoint resolution is 0.01 V. Should
     * disconnect supply from any chips before running.
     */
    public static void main(String args[]) {
        Infrastructure.gpibControllers = new int[] { 1 };
        Logger.setLogInits(true);
        Pst3202 supply = new Pst3202("hPst3202");
        //Infrastructure.readln("Hit return to clear device:");
        supply.clear();
        //        Infrastructure.readln("Cleared");
        System.out.println("Error1: " + supply.readError());
        System.out.println("Error2: " + supply.readError());
        System.out.println("Error3: " + supply.readError());
        System.out.println("Error4: " + supply.readError());
        System.out.println("Error5: " + supply.readError());
        supply.testConnection();

        float voltageSetpoint = supply.getVoltageSetpoint(1);
        System.out.println(supply.getState(1));
        if (voltageSetpoint < 0.2f) {
            voltageSetpoint = 0.2f;
        }

        for (int chan = 1; chan <= 3; chan++) {
            System.out.println("Chan " + chan + " resolution: "
                    + Pst3202.getVoltageResolution(chan) + " V");
        }

        // Demonstrate that resolution is 0.01 V
        for (float dv = 0.f; dv < 0.014; dv += 0.002) {
            supply.setVoltage(1, voltageSetpoint - dv);
            System.out.println((voltageSetpoint - dv) + ": "
                    + supply.getState(1) + "\n");
        }

        // See if supply can tolerate rapid voltage setting
        for (float volts = 1.8f; volts >= 1.60f; volts -= 0.01f) {
            System.out.println(volts);
            supply.setVoltage(1, volts);
            supply.setVoltage(3, volts);
        }

        System.out.println("Error6: " + supply.readError());

        System.out.println(supply.getState(1));

        supply.setVoltage(1, voltageSetpoint);
        supply.setVoltage(3, voltageSetpoint);

        System.out.println("Error7: " + supply.readError());

        System.out.println(supply.getState(1));
        System.out.println("Error8: " + supply.readError());
    }

} //end class
