/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ManualPowerChannel.java
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
 * Simple class for setting a power supply channel, when the voltage is supplied
 * by a manually-controlled DC power supply. If promptsUser is false, then it is
 * a mock power supply for debugging.
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
