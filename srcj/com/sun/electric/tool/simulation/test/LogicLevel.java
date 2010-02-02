/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LogicLevel.java
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
 * Class for setting Vdd and a logic level on a chip, when both values are
 * provided by power supplies. The logic level will be 0 V if false or equal to
 * Vdd if true. One use is for implementing master clear to an external pin. It
 * implements <code>PowerChannel</code> for Vdd, so that changes to Vdd in
 * <code>ChainTest</code> will also change the master clear voltage.
 * <p>
 * 
 * Using this class ensures that the logic voltage tracks changes to Vdd if the
 * logic state is true. The changes in the two voltage levels occur
 * simultaneously, preventing either of the failure modes likely if the two
 * voltages were set separately: 1) Vdd temporarily exceeds the logic voltage,
 * resulting in an artificial "false" interpretation of the logic level; 2) the
 * logic voltage temporarily exceeds Vdd, resulting in the logic voltage trying
 * to power up the chip through its ESD diodes. These failure modes are
 * prevented if Vdd and the logic voltage are never allowed to differ by more
 * than about 0.5 Vdd or 0.7 V, respectively.
 * <p>
 * 
 * Note: when using this class, the user should not use the
 * <code>PowerChannel</code> methods directly to control the voltages.
 * <p>
 * 
 * To synchronize two logic levels with Vdd, create a <code>LogicLevel</code>
 * object for one and then use that as the vddChannel for a second
 * <code>LogicLevel</code> object. For example, for <code>PowerChannel</code>
 * objects <code>vddChannel</code>,<code>logicChannel</code>, and
 * <code>logicChannel2</code>, one can use:
 * 
 * <pre>
 * LogicLevel level = new LogicLevel(vddChannel, logicChannel, false);LogicLevel level2 = new LogicLevel(level, logicChannel2, false); 
 *         
 *        
 *       
 *      
 *     
 *    
 *   
 *  
 * </pre>
 * 
 * In this case, the only methods one should invoke in the <code>level</code>
 * object are <code>isLogicStateHigh()</code> and <code>setLogicState()</code>.
 * I.e., control of Vdd should only be performed using <code>level2</code>.
 */
public class LogicLevel extends PowerChannel implements LogicSettable {

    /** Object for setting chip Vdd */
    private final PowerChannel vddChannel;

    /** Object for setting voltage of the logic level */
    private final PowerChannel logicChannel;

    /** Current state of the logic level */
    private boolean logicState;

    /**
     * Maximum difference between Vdd and logic voltages when logic level is
     * true, used while ramping Vdd up or down. We set it small enough so that
     * it doesn't matter whether Vdd leads or trails the logic level.
     */
    public final static float MAX_VOLTS_DEVIATION = 0.2f;

    /* Name of the logic signal */
    private String logicName;

    /**
     * Instantiate a LogicLevel object in the specified <code>logicState</code>.
     * That is, the voltage on <code>logicChannel</code> is set to 0 V (if
     * <code>logicState==false</code>) or to the voltage on vddChannel (if
     * <code>logicState=true</code>). Requirements: <code>vddChannel</code>
     * and <code>logicChannel</code> must be instantiated, but do not need to
     * be channels on the same power supply.
     * 
     * @param vddChannel
     *            Channel of a power supply that supplies V_DD
     * @param logicChannel
     *            Channel of a power supply that supplies V_DD
     * @param logicState
     *            Desired initial state (high or low) of the logic level
     */
    public LogicLevel(PowerChannel vddChannel, PowerChannel logicChannel,
            boolean logicState) {
        this.name = vddChannel.getName();
        this.vddChannel = vddChannel;
        this.logicName = logicChannel.getName();
        this.logicChannel = logicChannel;
        this.setLogicState(logicState);
        logInit("Initializing LogicLevel " + this.name + " with "
                + this.logicName + " in the " + logicState + " state");
    }

    /** Obsolete, use getState() instead */
    public String toString() {
        Infrastructure
                .nonfatal("Please change toString() reference to getState()");
        return getState();
    }

    /**
     * Returns the name and state (voltage, current) of the channel
     * 
     * @return the name and state of the channel
     */
    public String getState() {
        return super.getState() + "; " + getLogicName() + ": "
                + isLogicStateHigh();
    }

    /**
     * Returns the name of the logic signal
     * 
     * @return name of the logic signal
     */
    public String getLogicName() {
        return logicName;
    }

    /**
     * Reads back the voltage provided by the Vdd channel of the power supply.
     * 
     * @return voltage drawn over the Vdd channel, in Amps
     */
    public float readVoltage() {
        return vddChannel.readVoltage();
    }

    /**
     * Get the Vdd channel's voltage
     * 
     * @return current voltage for the Vdd channel, in Volts
     */
    public float getVoltageSetpoint() {
        return vddChannel.getVoltageSetpoint();
    }

    /**
     * Set the Vdd channel's voltage to the value provided. If the logic level
     * is high, then we must ramp up the two voltages simultaneously to prevent
     * either of two problems: 1) the level exceeds Vdd, in which case the logic
     * pin may be trying to power the chip; 2) the level is below Vdd, in which
     * case it may erroneously appear false.
     * 
     * @param volts
     *            new voltage for the channel, in Volts
     */
    public void setVoltageNoWait(float volts) {
        logSet("LogicLevel setting " + getName() + " to " + volts
                + " V (nowait)");

        // If logicLevel is false, no impediment to setting the final Vdd
        if (this.isLogicStateHigh() == false) {
            this.vddChannel.setVoltageNoWait(volts);
            return;
        }

        // If logicLevel is true, must ramp the voltages simultaneously
        float vdd = this.vddChannel.getVoltageSetpoint();
        float logicVolts = vdd;
        float step0;
        if ((volts - vdd) >= 0) {
            step0 = MAX_VOLTS_DEVIATION;
        } else {
            step0 = -MAX_VOLTS_DEVIATION;
        }
        float step = step0;
        System.out.print("LogicLevel setting " + getName() + " to " + volts
                + " V...");
        while (Math.abs(vdd - volts) > MAX_VOLTS_DEVIATION) {
            vdd = nextVoltage(vdd, step, volts);
            this.vddChannel.setVoltageNoWait(vdd);
            step = 2 * step0;
            logicVolts = nextVoltage(logicVolts, step, volts);
            this.logicChannel.setVoltageNoWait(logicVolts);
            System.out.print(".");
        }

        // Want to achieve "volts" exactly, so take out of loop
        this.logicChannel.setVoltageNoWait(volts);
        this.vddChannel.setVoltageNoWait(volts);
        System.out.println(".done");
    }

    /**
     * Compute next voltage value, without allowing overshoot.
     * 
     * @param old
     *            current voltage
     * @param step
     *            voltage step size (may be negative)
     * @param end
     *            final voltage
     */
    private float nextVoltage(float old, float step, float end) {
        float next = old + step;
        if (step > 0) {
            next = Math.min(next, end);
        } else {
            next = Math.max(next, end);
        }
        return next;
    }

    /**
     * Set the channel's voltage and wait until it reaches requested value.
     * 
     * @param volts
     *            new voltage for the channel, in Volts
     */
    public void setVoltageWait(float volts) {
        logSet("LogicLevel setting " + getName() + " to " + volts + " V (wait)");
        setVoltageNoWait(volts);
        if (this.isLogicStateHigh()) {
            logicChannel.waitForVoltage(volts);
        }
        vddChannel.waitForVoltage(volts);
    }

    /**
     * Reads back the current provided by the Vdd channel of the power supply.
     * 
     * @return current drawn over the Vdd channel, in Amps
     */
    public float readCurrent() {
        return vddChannel.readCurrent();
    }

    /**
     * Set the Vdd channel's current limit to the value provided
     * 
     * @param amps
     *            new current limit for the channel, in Amps
     */
    public void setCurrent(float amps) {
        logSet("LogicLevel set " + getName() + " current limit to " + amps);
        vddChannel.setCurrent(amps);
    }

    /**
     * Get the Vdd channel's current limit setpoint
     * 
     * @return current limit setpoint for the channel, in Amps
     */
    public float getCurrentSetpoint() {
        return vddChannel.getCurrentSetpoint();
    }

    /**
     * @return Current value for Logic State
     */
    public boolean isLogicStateHigh() {
        return this.logicState;
    }

    /**
     * If logicState is true, sets voltage for logic level to same values as
     * Vdd. Otherwise sets it to zero.
     * 
     * @param logicState
     *            New value for logic state
     */
    public void setLogicState(boolean logicState) {
        logSet("LogicLevel setting " + getLogicName() + " to " + logicState);
        this.logicState = logicState;
        if (logicState == false) {
            this.logicChannel.setVoltageWait(0.f);
        } else {
            float volts = this.vddChannel.getVoltageSetpoint();
            this.logicChannel.setVoltageWait(volts);
        }
    }

    /**
     * Reads back the current provided by the logic channel of the power supply.
     * 
     * @return current drawn over the logic channel, in Amps
     */
    public float readLogicCurrent() {
        return logicChannel.readCurrent();
    }

    /**
     * Get the logic channel's voltage
     * 
     * @return current voltage for the logic channel, in Volts
     */
    public float getLogicVoltageSetpoint() {
        return logicChannel.getVoltageSetpoint();
    }

    /**
     * Unit test exhibits use with one power and two logic channels, then
     * exercises a single power/logic combination with a real power supply.
     * 
     * @param args
     *            ignored
     */
    public static void main(String[] args) {

        // One power, two logic channels
        PowerChannel vddChannel = new ManualPowerChannel("vdd", false);
        PowerChannel logicChannel = new ManualPowerChannel("log1", false);
        PowerChannel logicChannel2 = new ManualPowerChannel("log2", false);
        LogicLevel level = new LogicLevel(vddChannel, logicChannel, false);
        LogicLevel level2 = new LogicLevel(level, logicChannel2, false);
        level.setLogicState(true);
        level2.setLogicState(true);
        level2.setVoltageWait(1.2f);
        level2.setLogicState(false);
        level.setLogicState(false);

        System.out.println("Hit return to move on to real supply:");
        Infrastructure.readln();

        Infrastructure.gpibControllers = new int[] { 1 };
	//        vddChannel = new Pst3202Channel("vdd", "hPst3202", 1);
        //logicChannel = new Pst3202Channel("log", "hPst3202", 3);
        vddChannel = new HP6624AChannel("vdd", "hHP6624A", 1);
        logicChannel = new HP6624AChannel("log", "hHP6624A", 3);
        //        vddChannel = new ManualPowerChannel("vdd", false);
        //        logicChannel = new ManualPowerChannel("log", false);
        level = new LogicLevel(vddChannel, logicChannel, false);

        System.out.println(level.getState());
        level.setLogicState(false);
        level.setVoltageWait(1.0f);
        System.out.println("After false ramp down: " + level.getState());
        System.out.println("  logic: " + logicChannel.getVoltageSetpoint()
                + " V, " + logicChannel.readCurrent() + " A");
        level.setLogicState(true);
        level.setVoltageWait(1.75f);
        System.out.println("After true ramp up: " + level.getState());
        System.out.println("  logic: " + logicChannel.getVoltageSetpoint()
                + " V, " + logicChannel.readCurrent() + " A");
        level.setVoltageWait(1.1f);
        System.out.println("After true ramp down: " + level.getState());
        System.out.println("  logic: " + logicChannel.getVoltageSetpoint()
                + " V, " + logicChannel.readCurrent() + " A");

    }
}
