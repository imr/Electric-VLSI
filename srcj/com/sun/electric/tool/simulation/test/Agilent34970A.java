package com.sun.electric.tool.simulation.test;

/*
 * Agilent34970A.java
 *
 * Copyright (c) 2004 by Sun Microsystems, Inc.
 *
 * Created: December 15, 2004
 */

/**
 * API for controlling the Agilent 34970A data acquisition/swith unit. At
 * present many capabilities of the device, such as scanning, are not supported,
 * and it is used as a simple multi-channel voltmeter. Error checking on channel
 * numbers is provided by the device itself (see <code>handleError</code>),
 * since the valid channel numbers depend on which modules are inserted.
 * 
 * @author Tom O'Neill (toneill)
 */
class Agilent34970A extends Equipment {

    /** Maximum current value that the DVM can measure, value: 3 A */
    public static final float MAX_AMPS = 3.0f;

    int lastChannel = CHANNEL_UNDEFINED;
    
    static final int CHANNEL_UNDEFINED = -1;
    
    /** Creates a new instance of HP34401A */
    public Agilent34970A(String name) {
        super(name);
    }

    /** Reset to factor configuration */
    public void reset() {
        write("*RST; *CLS");
    }

    /** Perform a self-test */
    public void selfTest() {
        System.out.println("Initiating self-test, please wait...");
        write("*TST?");
        try { Thread.sleep(5000); } catch (InterruptedException _) { }
        String s = read(40);
        System.out.println("Self-test output: " + s);
    }

    /**
     * Displays a message on the front panel. Provided to allow programmer to
     * taunt the user with insulting messages.
     */
    public void display(String text) {
        write("DISPLAY:TEXT '" + text + "'");
    }

    /**
     * Clears the message from the front panel. Provided to allow programmer to
     * clear inappropriate messages from the display when the boss shows up.
     */
    public void clearDisplay() {
        write("DISPLAY:TEXT:CLEAR");
    }

    /**
     * Measure voltage on specified channel
     * 
     * @param channel
     *            channel to measure voltage on (e.g., 101)
     */
    public float readVoltage(int channel) {
        write("MEASURE:VOLTAGE:DC? (@" + channel + ")");
        String s = read(20);
        if (s.length() == 0) {
            handleError();
            Infrastructure
                    .fatal("failed to read voltage on channel " + channel);
        }
        return Float.parseFloat(s);
    }

    String readError() {
        write("SYSTEM:ERROR?");
        String error = read(80);
        return error;
    }

    /**
     * Reports the contents of the device's error FIFO, with stack traces.
     * Should only be called in case of error.
     */
    void handleError() {
        String error = readError();
        do {
            if (error.length() <= 0) {
                Infrastructure.fatal("No reply from Agilent34970A named "
                        + this.toString());
            }
            Infrastructure.nonfatal("Agilent34970 named " + this.toString()
                    + " reports error " + error);
            error = readError();
        } while (error.equals("+0,\"No error\"") == false);
    }

    public static void main(String[] args) {
        Agilent34970A dvm = new Agilent34970A("HP34970");
        dvm.reset();
        //dvm.interactive();
        //dvm.selfTest();
        for (int channel = 101; channel < 120; channel++) {
            float volts = dvm.readVoltage(channel);
            System.out.println(channel + ": " + volts + " V");
        }
    }

}
