package com.sun.electric.tool.simulation.test;

/* PowerChannel.java
 * 
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 *
 * Created on Jul 19, 2004
 */

/**
 * Abstract class allowing generic control of a single power supply channel,
 * independent of the nature of the power supply. Primarily used for controlling
 * chip V_DD.
 * 
 * @author Tom O'Neill (toneill)
 * @version 1.0 8/16/04
 */
public abstract class PowerChannel extends Logger implements CurrentReadable,
        VoltageReadable {

    /**
     * Maximum allowed delay in milliseconds for voltage to reach within the
     * power supply's voltage resolution.
     */
    public long maxSettleMsec = 4000;

    /** Delay between each voltage check in {@link #waitForVoltage}. */
    public float settleResolution = 0.1f;

    /** Default voltage resolution for power supply */
    public static final float DEFAULT_VOLTAGE_RESOLUTION = 0.01f;

    /* Name of the signal on this power supply channel */
    protected String name;

    /** Obsolete, use getState() instead */
    public String toString() {
        Infrastructure
                .nonfatal("Please change toString() reference to getState()");
        return getName();
    }

    /**
     * Returns the name and state (voltage, current) of the channel
     * 
     * @return the name and state of the channel
     */
    public String getState() {
        return getName() + ": " + readVoltage() + " V, " + readCurrent() + " A";
    }

    /**
     * Returns the name of the signal on this channel of the power supply (e.g.,
     * vdd, master clear, etc.)
     * 
     * @return the name of the signal on this power supply channel
     */
    public String getName() {
        return name;
    }

    /**
     * Measures the voltage on this channel of the power supply
     * 
     * @return voltage provided on this channel, in Volts
     */
    public abstract float readVoltage();

    /**
     * Get the channel's voltage setpoint
     * 
     * @return voltage setpoint for the channel, in Volts
     */
    public abstract float getVoltageSetpoint();

    /**
     * Returns voltage resolution of power supply, if known. Otherwise returns a
     * "safe" typical value.
     * 
     * @return voltage resolution of power supply
     */
    public float getVoltageResolution() {
        return DEFAULT_VOLTAGE_RESOLUTION;
    }

    /**
     * Set the channel's voltage to the value provided
     * 
     * @param volts
     *            new voltage for the channel, in Volts
     */
    public abstract void setVoltageNoWait(float volts);

    /**
     * Set the channel's voltage and wait until it reaches requested value.
     * 
     * @param volts
     *            new voltage for the channel, in Volts
     */
    public void setVoltageWait(float volts) {
        setVoltageNoWait(volts);
        waitForVoltage(volts);
    }

    /**
     * Waits until voltage measured on the channel is equal to setVolts within
     * the resolution of the power supply. Issues a fatal error if voltage error
     * exceeds resolution after MAX_SETTLE_MSEC milliseconds have elapsed.
     */
    public void waitForVoltage(float setVolts) {
        long startTime = java.lang.System.currentTimeMillis();
        long endTime;
        float gotV;

        // Multiply by 1.001f to allow for rounding issues
        float voltsErr = 1.001f * getVoltageResolution();

        // Wait until voltage within allowed range, or maximum
        // settling time has elapsed.
        do {
            boolean checkedSetpoint = false;

            //Infrastructure.waitSeconds(0.1f);
            gotV = readVoltage();
            if (Math.abs(gotV - setVolts) <= voltsErr) {
                logOther("  " + getName() + ".waitForVoltage(): achieved "
                        + gotV + " V");
                return;
            }
            if (checkedSetpoint == false) {
                checkedSetpoint = true;
                float setpoint = getVoltageSetpoint();
                if (Math.abs(setpoint - setVolts) > (1.0001 * getVoltageResolution())) {
                    Infrastructure.fatal(this.getName()
                            + ".waitForVoltage: requested setpoint is "
                            + setVolts + " V, but actual setpoint is "
                            + setpoint + ".  Voltage is " + gotV
                            + " V and current is " + readCurrent() + " A.");
                }
            }
            try { Thread.sleep( (int)(1000 * settleResolution) ); } catch (InterruptedException _) { }
            endTime = java.lang.System.currentTimeMillis();
        } while ((endTime - startTime) <= maxSettleMsec);

        Infrastructure.fatal(this.getName()
                + ".waitForVoltage: requested setpoint is " + setVolts
                + " V, but voltage is " + gotV + " V after "
                + (endTime - startTime) + " msec.  Actual setpoint is "
                + getVoltageSetpoint() + " V.  Current is " + readCurrent()
                + " A, setpoint is " + getCurrentSetpoint() + " A.");
    }

    /**
     * Set the channel's current limit to the value provided
     * 
     * @param amps
     *            new current limit for the channel, in Amps
     */
    public abstract void setCurrent(float amps);

    /**
     * Get the channel's current limit setpoint
     * 
     * @return current limit setpoint for the channel, in Amps
     */
    public abstract float getCurrentSetpoint();

    /**
     * Measures current using range appropriate for <code>ampsExpected</code>,
     * and resolution of <code>ampsResolution</code>. Supplies that allow
     * range setting should override this default implementation.
     * 
     * @param ampsExpected
     *            expected value of current in amps, for range setting
     * @param ampsResolution
     *            desired resolution for measurement, in amps
     * @return current in Amps
     */
    public float readCurrent(float ampsExpected, float ampsResolution) {
        System.err.println("WARNING: " + getName()
                + " does not support manual range and resolution setting");
        return readCurrent();
    }

    /**
     * Measures the current on this channel of the power supply
     * 
     * @return current drawn over this channel, in Amps
     */
    public abstract float readCurrent();

}
