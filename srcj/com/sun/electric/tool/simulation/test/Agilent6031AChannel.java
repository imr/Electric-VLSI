/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Agilent6031AChannel.java
 * Written by Tom O'Neill and Nathaniel Pinckney, Sun Microsystems.
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
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
 * Class for setting a voltage level supplied by an Agilent 6031A 20V/120A
 * programmable DC power supply, using the device-independent interface
 * {@link PowerChannel}.
 * <P>
 * This class should now be instantiated from a {@link Model#createPowerChannel(String, String, int, int, String)}
 */
public class Agilent6031AChannel extends PowerChannel {

    /**
     * The Agilent 6031A programmable DC power supply that provides the voltage level
     * in question
     */
    private final Agilent6031A supply;

    /**
     * Creates an object to control a Agilent 6031A power supply using the methods of
     * the device-independent PowerChannel abstract class.
     * <P>
     * Instead of this, you should use
     * {@link Model#createPowerChannel(String, String, int, int, String)}.
     * I have left it public for backwards compatability. 
     * 
     * @param channelName
     *            name of signal on this power supply
     * @param supplyName
     *            <code>gpibconf</code> identifier for the power supply
     */
    public Agilent6031AChannel(String channelName, String supplyName) {
        this.name = channelName + " (" + supplyName + ")";
        supply = new Agilent6031A(supplyName);
        logInit("Initialized Agilent6031AChannel " + this.name);
    }

    /** @return Returns the name of the Agilent6031A */
    public String getSupplyName() {
        return supply.getName();
    }

    /**
     * Returns string indicating state of the channels
     */
    public String getState() {
        return supply.getState();
    }

    /**
     * Reads back the voltage provided by this channel of the power supply.
     * 
     * @return voltage drawn over this channel, in Volts
     */
    public float readVoltage() {
        logOther("Reading voltage on " + getName());
        return this.supply.readVoltage();
    }

    /**
     * Set the channel's voltage to the value provided
     * 
     * @param volts
     *            new voltage for the channel, in Volts
     */
    public void setVoltageNoWait(float volts) {
        logSet("Agilent6031AChannel setting voltage on " + getName() + " to "
                + volts + " V");
        supply.setVoltage(volts);
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
        return this.supply.getVoltageSetpoint();
    }

    /**
     * Returns voltage resolution of power supply.
     * 
     * @return voltage resolution of power supply
     */
    public float getVoltageResolution() {
        logOther("Getting voltage resolution on " + getName());
        return Agilent6031A.getVoltageResolution();
    }

    /**
     * Reads back the current provided by this channel of the power supply.
     * 
     * @return current drawn over this channel, in Amps
     */
    public float readCurrent() {
        logOther("Reading current on " + getName());
        return this.supply.readCurrent();
    }

    /**
     * Set the channel's current limit to the value provided
     * 
     * @param amps
     *            new current limit for the channel, in Amps
     */
    public void setCurrent(float amps) {
        logSet("Setting current limit on " + getName() + " to " + amps);
        supply.setCurrent(amps);
    }

    /**
     * Get the channel's current setpoint
     * 
     * @return current setpoint for the channel, in Amps
     */
    public float getCurrentSetpoint() {
        logOther("Reading current setpoint on " + getName());
        return supply.getCurrentSetpoint();
    }

    /**
     * Gets the foldback mode or turns it off.
     *
     */
    public int getFoldback() {
        return supply.getFoldback();
    }
  
    /**
     * Sets the foldback mode or turns it off.
     *
     * @param mode
     *      Foldback mode to set.  Can be <code>Agilent6031A.FOLDBACK_CV</code>,
     *   <code>FOLDBACK_CC</code>, or <code>FOLDBACK_OFF</code>.
     * @throws IllegalArgumentException
     *             if mode not in 0..2
     */
    public void setFoldback(int mode) {
		supply.setFoldback(mode);
    }

	/**
	 * Gets the over voltage protection value.
	 *
	 */
	public float getOverVoltageProtection() {
        return supply.getOverVoltageProtection();
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
        Agilent6031AChannel channel = new Agilent6031AChannel("aAgilent6031A", "power");
	System.out.println(channel.getState());
	System.out.println("res " + channel.getVoltageResolution() + "\n");
    }
}
