/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Pst3202Channel.java
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
 * Class for setting a voltage level supplied by a single channel of an Instek
 * PST-3202 programmable DC power supply, using the device-independent interface
 * {@link PowerChannel}.
 * <P>
 * This class should now be instantiated from a {@link Model#createPowerChannel(String, String, int, int, String)}
 */
public class Pst3202Channel extends PowerChannel {

    /**
     * The Instek PST-3202 programmable DC power supply that provides the
     * voltage level in question
     */
    private final Pst3202 supply;

    /** Channel of power supply that supplies the voltage */
    private final int channel;

    /**
     * Creates an object to control a PST-3202 power supply using the methods of
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
    public Pst3202Channel(String channelName, String supplyName, int channel) {
        Pst3202.checkChannel(channel);
        this.name = channelName + " (" + supplyName + " channel " + channel
                + ")";
        supply = new Pst3202(supplyName);
        this.channel = channel;
        logInit("Initialized Pst3202Channel " + this.name);
    }

    /** @return Returns the name of the Pst3202 */
    public String getSupplyName() {
        return supply.getName();
    }

    /**
     * @return Returns the channel number within the Pst3202 device.
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
        logSet("Pst3202Channel setting voltage on " + getName() + " to "
                + volts + " V");
        if (this.supply.setVoltage(this.channel, volts) == false) {
            try { Thread.sleep(1000); } catch (InterruptedException _) { }
            if (this.supply.setVoltage(this.channel, volts) == false) {
                Infrastructure.fatal(this.getName()
                        + " power supply error setting voltage " + volts);
            }
        }
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
        return Pst3202.getVoltageResolution(this.channel);
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
     * Unit tests, prints current as function of voltage for channel 1. Should
     * disconnect supply from any chips before waiting.
     * 
     * @param args
     *            Ignored
     */
    public static void main(String[] args) {
        Infrastructure.gpibControllers = new int[] { 1 };
        Pst3202Channel channel1 = new Pst3202Channel("chan1", "hPst3202", 1);
        Pst3202Channel channel2 = new Pst3202Channel("chan2", "hPst3202", 2);
        Pst3202Channel channel3 = new Pst3202Channel("chan3", "hPst3202", 3);

        System.out.println("Voltage setpoints: "
                + channel1.getVoltageSetpoint() + ", "
                + channel2.getVoltageSetpoint() + ", "
                + channel3.getVoltageSetpoint());
        System.out.println("Voltages read: " + channel1.readVoltage() + ", "
                + channel2.readVoltage() + ", " + channel3.readVoltage());
        System.out.println("Current limits: " + channel1.getCurrentSetpoint()
                + ", " + channel2.getCurrentSetpoint() + ", "
                + channel3.getCurrentSetpoint());
        System.out.println("Currents read: " + channel1.readCurrent() + ", "
                + channel2.readCurrent() + ", " + channel3.readCurrent());
        channel3.setCurrent(2.0f);

        System.out.println("Chan 1 resolution: "
                + channel1.getVoltageResolution() + " V");
        System.out.println("Chan 2 resolution: "
                + channel2.getVoltageResolution() + " V");
        System.out.println("Chan 3 resolution: "
                + channel3.getVoltageResolution() + " V");
        
        if (false) {
            int nsweep = 0;
            do {
                for (float volts = 1.0f; volts < 1.85f; volts += 0.01f) {
                    channel1.setVoltageNoWait(volts);
                    channel3.setVoltageWait(volts);
                    channel1.waitForVoltage(volts);
                    System.out.print(nsweep + ", " + volts + " V; ");
                    System.out.println(channel1.getState());
                }
                nsweep++;
            } while (true);
        }
    }
}
