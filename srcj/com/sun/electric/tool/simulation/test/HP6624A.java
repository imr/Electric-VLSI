package com.sun.electric.tool.simulation.test;

/*
 * HP6624A.java
 *
 * Created on November 6, 2003, 3:06 PM
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 *
 */

/**
 * This class extends the generic equipment class to include methods specific to
 * the HP6624A 4-channel power supply.
 * <P>
 * Rather than using this class directly, you should use {@link HP6624AChannel}
 * 
 * @author rh141231
 * @author Tom O'Neill (toneill)
 */
public class HP6624A extends Equipment {

    String s = new String("null");

    /** Number of channels (value = 4) */
    public static final int NUM_CHANNELS = 4;

    /** Resolution of each channel in Volts */
    private static final float VOLTAGE_RESOLUTION = 0.01f;

    /** Creates a new instance of HP6624A */
    protected HP6624A(String newName) {
        super(newName);
    }

    static void checkChannel(int channel) {
        if (channel < 1 || channel > NUM_CHANNELS) {
            throw new IllegalArgumentException("Channel " + channel
                    + " outside allowed range 1.." + NUM_CHANNELS);
        }
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
        result = "Set: " + getVoltageSetpoint(channel) + " V, "
                + getCurrentSetpoint(channel) + " A\n";
        result += "Got " + readVoltage(channel) + " V, " + readCurrent(channel)
                + " A";
        return result;
    }

    /**
     * Returns voltage resolution of channel
     * 
     * @param channel
     *            Supply channel number
     * @return Voltage resoluton of channel
     * @throws IllegalArgumentException
     *             if channel not in range 1..4
     */
    public static float getVoltageResolution(int channel) {
        checkChannel(channel);
        return VOLTAGE_RESOLUTION;
    }

    /**
     * Measures the voltage on one of the 4 channels
     * 
     * @throws IllegalArgumentException
     *             if channel not in range 1..4
     */
    public float readVoltage(int channel) {
        checkChannel(channel);
        write("VOUT ?" + channel);
        s = read(200);
        return Float.parseFloat(s);
    }

    /**
     * Reads back the voltage setpoint from one of the 3 channels
     * 
     * @throws IllegalArgumentException
     *             if channel not in range 1..4
     */
    public float getVoltageSetpoint(int channel) {
        checkChannel(channel);
        logOther("Reading voltage setpoint on HP6624A " + getName()
                + ", channel " + channel);
        write("VSET?" + channel);
        //System.out.println("Setpoint " + s);
        s = read(200).trim();
        return Float.parseFloat(s);
    }

    /** Sets the voltage on one of the 4 channels */
    public void setVoltage(int channel, float voltage) {
        checkChannel(channel);
        write("VSET " + channel + " , " + voltage);
        //   Infrastructure.waitSeconds(.3f);
    }

    /** Reads back the current from one of the 4 channels */
    public float readCurrent(int channel) {
        checkChannel(channel);
        write("IOUT? " + channel);
        String s = read(200).trim();
        return Float.parseFloat(s);
    }

    /**
     * Reads back the current setpoint from one of the 4 channels
     * 
     * @throws IllegalArgumentException
     *             if channel not in range 1..4
     */
    public float getCurrentSetpoint(int channel) {
        checkChannel(channel);
        write("ISET? " + channel);
        String s = read(10);
        return Float.parseFloat(s);
    }

    /**
     * Changes the current setpoint on channel <code>channel</code> to
     * <code>setAmps</code> Amps.
     * 
     * @throws IllegalArgumentException
     *             if channel not in range 1..4
     */
    public void setCurrent(int channel, float setAmps) {
        checkChannel(channel);
        write("ISET " + channel + ", " + setAmps);
    }
    
    /**
     * Changes the voltage for the over voltage protection on channel
     *  <code>channel</code> to
     * <code>setVolts</code> Volts.
     * 
     * @param channel selects which channel
     * @param setVolts the new over-voltage limit
     * @throws IllegalArgumentException
     *             if channel not in range 1..4
     */
    public void setOverVoltageProtection(int channel, float setVolts) {
        checkChannel(channel);
        write("OVSET " + channel + ", " + setVolts);
    }

    /**
     * Changes the current for the over current protection on channel
     *  <code>channel</code> to
     * <code>setAmps</code> Amps.
     * 
     * @param channel selects which channel
     * @param setAmps the new over-current limit
     * @throws IllegalArgumentException
     *             if channel not in range 1..4
     */
    public void setOverCurrentProtection(int channel, float setAmps) {
        checkChannel(channel);
        write("OCP " + channel + ", " + setAmps);
    }

    public static void main(String args[]) {
        Infrastructure.gpibControllers = new int[] { 1 };
        HP6624A supply = new HP6624A("hHP6624A");
	//        supply.setVoltage(3, 1.8f);
        //System.out.println("current " + supply.readCurrent(3));
        for (int chan = 1; chan <= 4; chan++) {
            System.out.println(chan + ": " + supply.getState(chan));
        }
        supply.interactive();
    }//end main

}
