/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: HP6624AChannel.java
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

/**
 * Class for setting a voltage level supplied by a single channel of an HP 6624A
 * programmable DC power supply, using the device-independent interface
 * {@link PowerChannel}.
 * <P>
 * This class should now be instantiated from a {@link Model#createPowerChannel(String, String, int, int, String)}
 */
public class HP6624AChannel extends PowerChannel {

    /**
     * The HP 6624A programmable DC power supply that provides the voltage level
     * in question
     */
    private final HP6624A supply;

    /** Channel of power supply that supplies the voltage */
    private final int channel;

    /**
     * Creates an object to control a HP 6624A power supply using the methods of
     * the device-independent PowerChannel abstract class.
     * <P>
     * Instead of this, you should use
     * {@link Model#createPowerChannel(String, String, int, int, String)}.
     * I have left it public for backwards compatability. 
     * 
     * @param channelName
     *            name of signal on this channel of the power supply
     * @param supplyName
     *            <code>gpibconf</code> identifier for the power supply
     * @param channel
     *            Channel of the supply to control
     */
    public HP6624AChannel(String channelName, String supplyName, int channel) {
        HP6624A.checkChannel(channel);
        this.name = channelName + " (" + supplyName + " channel " + channel
                + ")";
        supply = new HP6624A(supplyName);
        this.channel = channel;
        logInit("Initialized HP6624AChannel " + this.name);
    }

    /** @return Returns the name of the HP6624A */
    public String getSupplyName() {
        return supply.getName();
    }

    /**
     * @return Returns the channel number within the HP6624A device.
     */
    public int getChannel() {
        return channel;
    }

    /**
     * Returns string indicating state of the channels
     */
    public String getState() {
        return supply.getState(channel);
    }

    /**
     * Reads back the voltage provided by this channel of the power supply.
     * 
     * @return voltage drawn over this channel, in Volts
     */
    public float readVoltage() {
        logOther("Reading voltage on " + getName());
        return this.supply.readVoltage(this.channel);
    }

    /**
     * Set the channel's voltage to the value provided
     * 
     * @param volts
     *            new voltage for the channel, in Volts
     */
    public void setVoltageNoWait(float volts) {
        logSet("HP6624AChannel setting voltage on " + getName() + " to "
                + volts + " V");
        supply.setVoltage(this.channel, volts);
    }

    public void waitForVoltage(float setVolts) {
        if (supply.isDisabled())
            return;
        super.waitForVoltage(setVolts);
    }

    /**
     * Get the channel's voltage setpoint
     * 
     * @return voltage setpoint for the channel, in Volts
     */
    public float getVoltageSetpoint() {
        logOther("Reading voltage setpoint on " + getName());
        return this.supply.getVoltageSetpoint(this.channel);
    }

    /**
     * Returns voltage resolution of power supply.
     * 
     * @return voltage resolution of power supply
     */
    public float getVoltageResolution() {
        logOther("Getting voltage resolution on " + getName());
        return HP6624A.getVoltageResolution(this.channel);
    }

    /**
     * Reads back the current provided by this channel of the power supply.
     * 
     * @return current drawn over this channel, in Amps
     */
    public float readCurrent() {
        logOther("Reading current on " + getName());
        return this.supply.readCurrent(this.channel);
    }

    /**
     * Set the channel's current limit to the value provided
     * 
     * @param amps
     *            new current limit for the channel, in Amps
     */
    public void setCurrent(float amps) {
        logSet("Setting current limit on " + getName() + " to " + amps);
        supply.setCurrent(channel, amps);
    }

    /**
     * Get the channel's current setpoint
     * 
     * @return current setpoint for the channel, in Amps
     */
    public float getCurrentSetpoint() {
        logOther("Reading current setpoint on " + getName());
        return supply.getCurrentSetpoint(this.channel);
    }
    /**
     * Changes the voltage for the over voltage protection to 
     * <code>setVolts</code> Volts.
     * 
     * @param setVolts the new over-voltage limit
     * @throws IllegalArgumentException
     *             if channel not in range 1..4
     */
    public void setOverVoltageProtection(float setVolts) {
        logSet("Setting over voltage protection limit on " + getName() + " to " + setVolts);
    	supply.setOverVoltageProtection(this.channel, setVolts);
    }

    /**
     * Changes the current for the over current protection to
     * <code>setAmps</code> Amps.
     * 
     * @param setAmps the new over-current limit
     * @throws IllegalArgumentException
     *             if channel not in range 1..4
     */
    public void setOverCurrentProtection(float setAmps) {
        logSet("Setting over current protection limit on " + getName() + " to " + setAmps);
    	supply.setOverCurrentProtection(this.channel, setAmps);
    }

    /**
     * Unit tests, prints current as function of voltage for channel 1. Should
     * disconnect supply from any chips before waiting.
     * 
     * @param args
     *            Ignored
     */
    public static void main(String[] args) {
        Infrastructure.gpibControllers = new int[] { 1 };
        HP6624AChannel[] channels = new HP6624AChannel[HP6624A.NUM_CHANNELS];
        for (int chan = 1; chan <= HP6624A.NUM_CHANNELS; chan++) {
            int ichan = chan - 1;
            channels[ichan] = new HP6624AChannel("chan" + chan, "hHP6624A",
                    chan);
            System.out.println(chan + ": " + channels[ichan].getState());
            System.out.println("res " + channels[ichan].getVoltageResolution()
                    + "\n");
        }

    }
}
