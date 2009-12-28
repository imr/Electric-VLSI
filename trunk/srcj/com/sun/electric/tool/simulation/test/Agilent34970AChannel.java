package com.sun.electric.tool.simulation.test;

/* Agilent34970AChannel.java
 * 
 * Copyright (c) 2004 by Sun Microsystems, Inc.
 *
 * Created on December 16, 2004
 */

/**
 * Class for reading a voltage level on a single channel of an Agilent 34970A
 * data acquisition/switch unit.
 * 
 * @author Tom O'Neill (toneill)
 */
public class Agilent34970AChannel implements VoltageReadable {

    /** The Agilent34970A device to measure the voltage on */
    private final Agilent34970A dvm;

    /** Channel of power supply that supplies the voltage */
    private final int channel;

    /** Name of signal connected to the channel */
    public final String name;

    private String voltageRange = "DEF";

    private String voltageResolution = "DEF";

    /** Number of power line cycles to integrate over. Default is 10. */
    private String voltageNPLC = "10";

    /** Identifier for measurement function during previous read */
    private int lastFunction = FUNCTION_UNDEFINED;

    /**
     * Value when the function must be configured before a measurement can be
     * made. Occurs before first measurement, and after a change in
     * configuration.
     */
    public static final int FUNCTION_UNDEFINED = -1;

    public static final int FUNCTION_VOLTAGE = 0;

    public static final int FUNCTION_CURRENT = 1;

    /**
     * Creates an object to read voltages on a single channel of an Agilent
     * 34970A data acquisition unit, using the device-independent
     * VoltageReadable interface.
     * 
     * @param channelName
     *            name of signal on this channel of the Agilent 34970A
     * @param dvmName
     *            <code>gpibconf</code> identifier for the Agilent 34970A
     * @param channel
     *            Channel number of the Agilent 34970A to measure on
     */
    public Agilent34970AChannel(String channelName, String dvmName, int channel) {
        this.name = channelName;
        dvm = new Agilent34970A(dvmName);
        this.channel = channel;
    }

    /**
     * @param voltageRange
     *            The voltageRange to set.
     */
    public void setVoltageRange(String voltageRange) {
        this.voltageRange = voltageRange;
        if (lastFunction == FUNCTION_VOLTAGE)
            lastFunction = FUNCTION_UNDEFINED;
    }

    /**
     * @return Returns the voltageRange.
     */
    public String getVoltageRange() {
        return voltageRange;
    }

    /**
     * @param voltageResolution
     *            The voltageResolution to set.
     */
    public void setVoltageResolution(String voltageResolution) {
        this.voltageResolution = voltageResolution;
        if (lastFunction == FUNCTION_VOLTAGE)
            lastFunction = FUNCTION_UNDEFINED;
    }

    /**
     * @return Returns the voltageResolution.
     */
    public String getVoltageResolution() {
        return voltageResolution;
    }

    /**
     * Set the number of power line cycles to integrate the voltage measurement
     * over. Can greatly speed up the measurement, at the cost of accuracy.
     * 
     * @param voltageNPLC
     *            The voltageNPLC to set.
     */
    public void setVoltageNPLC(String voltageNPLC) {
        this.voltageNPLC = voltageNPLC;
        if (lastFunction == FUNCTION_VOLTAGE)
            lastFunction = FUNCTION_UNDEFINED;
    }

    /**
     * @return Returns the voltageNPLC.
     */
    public String getVoltageNPLC() {
        return voltageNPLC;
    }

    /**
     * Measure voltage on this channel using the measurement parameters
     * specified by the methods {@link #setVoltageRange},&nbsp;
     * {@link #setVoltageResolution}, and {@link #setVoltageNPLC}. For fast
     * but possibly inaccurate measurements, choose {@link #setVoltageRange}
     * <tt>("DEF")</tt>,{@link #setVoltageResolution}<tt>("MAX")</tt>,
     * and {@link #setVoltageNPLC}<tt>(0.02)</tt>.
     * 
     * @return voltage measured on this channel, in Volts
     */
    public float readVoltage() {
        if (lastFunction != FUNCTION_VOLTAGE || dvm.lastChannel != channel) {
            System.out.println("Configuring voltage on channel " + channel);
            dvm.write("CONF:VOLT:DC " + getVoltageRange() + ","
                    + getVoltageResolution() + ", (@" + channel
                    + ");:VOLT:DC:NPLC " + getVoltageNPLC() + ", (@" + channel
                    + ")");
        }
        dvm.write("READ?");
        dvm.lastChannel = channel;
        lastFunction = FUNCTION_VOLTAGE;
        return dvm.readFloat(40);
    }
    
    /**
     *
     */
    private long timeReadVoltage() {
        long startTime = java.lang.System.currentTimeMillis();
        for (int ind = 0; ind < 200; ind++) {
            System.out.println(readVoltage());
        }
        long endTime = java.lang.System.currentTimeMillis();
        System.out.println("dt = " + (endTime - startTime));
        return (endTime - startTime);
    }

    /**
     * 
     */
    private void measureReadVoltageTimes() {
        System.out.println("Default 4-digit:");
        setVoltageResolution("MAX");
        setVoltageNPLC("1");
        long defaultTime = timeReadVoltage();
        setVoltageResolution("MAX");
        setVoltageNPLC("0.02");
        System.out.println("Fast:");
        long fastTime = timeReadVoltage();
        System.out.println("MAX resolution times for 200 reads,"
                + " as function of NPLC:");
        System.out.println("1.00: " + defaultTime);
        System.out.println("0.02: " + fastTime);
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
        Agilent34970AChannel channel = new Agilent34970AChannel("test",
                "HP34970", 101);
        Agilent34970AChannel channel2 = new Agilent34970AChannel("test2",
                "HP34970", 102);
        System.out.println(channel.name + ": " + channel.readVoltage());
        System.out.println(channel2.name + ": " + channel2.readVoltage());
        
        channel2.measureReadVoltageTimes();
    }
}
