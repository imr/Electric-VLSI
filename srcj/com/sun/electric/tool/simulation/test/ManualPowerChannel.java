package com.sun.electric.tool.simulation.test;

/*
 * Created on Jul 19, 2004
 *
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 *
 */

/**
 * Simple class for setting a power supply channel, when the voltage is supplied
 * by a manually-controlled DC power supply. If promptsUser is false, then it is
 * a mock power supply for debugging.
 * 
 * @author Tom O'Neill (toneill)
 * @version 1.0 8/16/04
 */
public class ManualPowerChannel extends PowerChannel {

    /** Whether or not to prompt the user for voltage changes */
    private boolean promptsUser = true;

    /** Voltage setpoint */
    private float volts;

    /** Current setpoint */
    private float amps;

    /**
     * @param channelName
     *            name of signal on this channel of the power supply
     * @param promptsUser
     *            Whether or not to prompt the user for voltage changes
     */
    public ManualPowerChannel(String channelName, boolean promptsUser) {
        this.name = channelName;
        this.promptsUser = promptsUser;
    }

    /**
     * Reads back the voltage provided by this channel of the power supply.
     * 
     * @return voltage drawn over this channel, in Amps
     */
    public float readVoltage() {
        return volts;
    }

    /**
     * Get the channel's voltage set point.
     * 
     * @return current voltage for the channel, in Volts
     */
    public float getVoltageSetpoint() {
        return volts;
    }

    /**
     * Set the channel's voltage to the value provided
     * 
     * @param volts
     *            new voltage for the channel, in Volts
     */
    public void setVoltageNoWait(float volts) {
        if (promptsUser) {
            System.out.print("Set voltage " + getName() + " to " + volts
                    + " V and hit return:");
            Infrastructure.readln();
        } else {
            System.out.println("Setting voltage " + getName() + " to " + volts
                    + " V");
        }
        this.volts = volts;
    }

    /**
     * Set the channel's current limit to the value provided
     * 
     * @param amps
     *            new current limit for the channel, in Amps
     */
    public void setCurrent(float amps) {
        if (promptsUser) {
            System.out.print("Set current limit " + getName() + " to " + amps
                    + " A and hit return:");
            Infrastructure.readln();
        } else {
            System.out.println("Setting current limit " + getName() + " to "
                    + amps + " A");
        }
        this.amps = amps;
    }

    /**
     * Get the channel's current limit setpoint
     * 
     * @return current limit setpoint for the channel, in Amps
     */
    public float getCurrentSetpoint() {
        return amps;
    }

    /**
     * Reads back the current provided by this channel of the power supply.
     * 
     * @return current drawn over this channel, in Amps
     */
    public float readCurrent() {
        float amps = Float.parseFloat(Infrastructure.readln("Enter current on "
                + getName() + ": "));
        return amps;
    }

    /**
     * Unit tests, prints current as function of voltage
     * 
     * @param args
     *            Ignored
     */
    public static void main(String[] args) {
        PowerChannel channel = new ManualPowerChannel("chan", false);

        for (float volts = 1.0f; volts < 1.85f; volts += 0.1f) {
            channel.setVoltageWait(volts);
            System.out.println(channel);
        }

        channel = new ManualPowerChannel("chan_prompts", true);

        for (float volts = 1.0f; volts < 1.85f; volts += 0.1f) {
            channel.setVoltageWait(volts);
            System.out.println(channel);
        }
    }
}
