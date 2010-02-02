/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Netscan4Driver.java
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
 * Low-level drvier providing initialization, configuration, and connection for
 * all four ports on Corelis NETUSB-1149.1/E boundary scan controller (JTAG
 * tester device). Shifting data in and out should instead be performed using
 * {@link ChainControl}. Class is static and non-instantiable.
 */
class Netscan4Driver extends Logger {

    /** Minumum allowed value of the JTAG port (TAP) parameter */
    public static final int MIN_TAP = 1;

    /** Maximum allowed value of the JTAG port (TAP) parameter */
    public static final int MAX_TAP = 4;

    /** Number of GPIO pins per JTAG port (TAP) */
    public static final int NUM_OUTPUT_PINS = 3;

    /** Identifier for accessing GPIO in output mode */
    public static final int POD_IO_OUTPUT = 0x01;

    /** Identifiers for the 3 GPIO pins */
    public static final int POD_GPIO[] = new int[] { 0x0D, 0x0E, 0x0F };

    /** IP address of JTAG tester. */
    private static String addressIP;

    /** Number of ports in use on the Netscan4 device. */
    private static int numPortsRegistered = 0;

    /** Suppress default constructor to make class non-instantiable */
    private Netscan4Driver() {
    }

    /**
     * Registers to use a port on the Netscan4 JTAG tester and, if it hasn't
     * done so already, connects to it by using the NetUSB_Connect routine in
     * the scan function library. The Corelis Netscan SFL library only supports
     * one JTAG tester at a time, a restriction we enforce in Java for better
     * error messages. The tester must then be configured before use, by calling
     * {@link #configure}.
     * 
     * @param addressIP
     *            IP address of JTAG tester
     */
    static void registerPort(String addressIP, int jtagPort) {
        checkJtagPort(jtagPort);
        numPortsRegistered++;

        // If already connected to the device, we can return
        if (Netscan4Driver.addressIP != null) {
            if (addressIP.equals(Netscan4Driver.addressIP)) {
                return;
            }
            Infrastructure.fatal("Attempt to connect to Netscan4 at IP"
                    + " address " + addressIP + ", when one at "
                    + Netscan4Driver.addressIP
                    + " is already connected.  Corelis Netscan SFL"
                    + " library only supports one JTAG tester at a time");
        }

        Netscan4Driver.addressIP = addressIP;
        NetscanGeneric.incrementNumTesters();

        // Connect to tester via ethernet
        Logger.logInit("Connecting to Netscan4 at " + addressIP);
        int status = Netscan4JNI.netUSB_Connect(addressIP);
        if (status <= 0) {
            Infrastructure.fatal("Netscan4JNI.net_connect returned error code "
                    + status);
        }
    }

    /**
     * Check if jtagPort is in the allowed range. Routine should be overridden
     * if multiple ports are supported.
     * 
     * @param jtagPort
     *            proposed port on the JTAG tester
     */
    static void checkJtagPort(int jtagPort) {
        if (jtagPort < MIN_TAP || jtagPort > MAX_TAP) {
            Infrastructure.fatal("JTAG port number " + jtagPort
                    + " not in allowed range " + MIN_TAP + ".." + MAX_TAP);
        }
    }

    /**
     * Configures the JTAG tester, setting its parameters, and resets the JTAG
     * controller (clears TRSTb briefly). Can be run at any time after
     * initialization to change settings.
     * 
     * @param tapVolt
     *            signal (TAP) voltage in Volts
     * @param kiloHerz
     *            the TCK frequency in kHz (from 391 kHz to 40 MHz)
     * @param logger
     *            Object with logging properties to use
     */
    static void configure(float tapVolt, long kiloHerz, Logger logger) {
        int milliVolt = Math.round(tapVolt * 1000.f);

        logger.logSet("Netscan4 configuring " + kiloHerz + " kHz and "
                + milliVolt + " mV");
        int status = Netscan4JNI.netUSB_hard_reset(kiloHerz, milliVolt);

        // N.B.: For some reason, Corelis used ZERO for the ERROR condition!
        if (status == 0)
            Infrastructure.fatal("Netscan4JNI.netUSB_hard_reset returned "
                    + status);
        status = Netscan4JNI.netUSB_set_scan_clk(kiloHerz);

        if (status != 0)
            Infrastructure.fatal("Netscan4JNI.netUSB_set_scan_clk returned "
                    + status);

        // Reset the JTAG controller
        reset();
    }

    /**
     * Return the IP address of the Netscan4
     * 
     * @return Returns the IP address of the Netscan4
     */
    static String getAddressIP() {
        return addressIP;
    }

    /**
     * Reset the finite state machine of the chip's JTAG controller by briefly
     * setting the TRSTb signal <tt>LO</tt>. The IR becomes bypass
     * automatically.
     */
    static void reset() {
        int status = Netscan4JNI.netUSB_set_trst(0);
        if (status != 0)
            Infrastructure
                    .fatal("Netscan4JNI.net_set_trst(0) returned error code "
                            + status);
        status = Netscan4JNI.netUSB_set_trst(1);
        if (status != 0)
            Infrastructure
                    .fatal("Netscan4JNI.net_set_trst(1) returned error code "
                            + status);
    }

    /**
     * Reset the finite state machine of the chip's JTAG controller by briefly
     * setting the TMS signal <tt>HI</tt> for five cycles. The IR becomes bypass
     * automatically.
     */
    static void tms_reset(int jtagPort) {
        int status = Netscan4JNI.netUSB_tms_reset(jtagPort);
        if (status != 0)
            Infrastructure
                    .fatal("Netscan4JNI.net_tms_reset("+jtagPort+") returned error code "
                            + status);
    }

    /**
     * Deregisters to use a port on the JTAG tester. If no more ports are
     * registered, disconnect from the JTAG tester. Should be called for each
     * registered port before exiting JVM.
     */
    static void deregisterPort() {
        numPortsRegistered--;
        if (numPortsRegistered == 0) {
            int status = Netscan4JNI.netUSB_Disconnect();
            if (status != 0) {
                Infrastructure.fatal("Netscan4JNI.net_disconnect() "
                        + "returned error code " + status);
            }
        }
    }

    /**
     * Write the bits <code>scanIn</code> to the JTAG controller's instruction
     * register. The first bit scanned in to the chip is the LSB of
     * <code>scanIn[0]</code>, and the first bit scanned out from the chip is
     * the LSB of <code>scanOut[0]</code>.
     * 
     * @param jtagPort
     *            TAP port to use
     * @param numBits
     *            The number of bits to shift
     * @param scanIn
     *            Bit sequence to write to instruction register
     * @param scanOut
     *            Bits scanned out of instruction register
     * @param logger
     *            Object with logging properties to use
     * 
     * @return 0x00 (success), 0x11 (transmit error), 0x33 (receive error)
     */
    protected static int hw_net_scan_ir(int jtagPort, int numBits,
            short[] scanIn, short[] scanOut, Logger logger) {
        checkJtagPort(jtagPort);
        logger.logOther("IR into TAP " + jtagPort + ": # shorts="
                + scanIn.length + ", # bits="+ numBits+", " + NetscanGeneric.shortsToString(scanIn));
        int result = Netscan4JNI.netUSB_scan_ir(scanIn, numBits, scanOut, jtagPort);
        logger.logOther("IR out: # shorts=" + scanOut.length + ", "
                + NetscanGeneric.shortsToString(scanOut));
        return result;
    }

    /**
     * Write the bits <code>scanIn</code> to the JTAG controller's data
     * register, and read back the bits <code>scanOut</code>. The first bit
     * scanned in to the chip is the LSB of <code>scanIn[0]</code>, and the
     * first bit scanned out from the chip is the LSB of <code>scanOut[0]</code>.
     * 
     * @param jtagPort
     *            TAP port to use
     * @param numBits
     *            The number of bits to shift
     * @param scanIn
     *            Bit sequence to write to data register
     * @param scanOut
     *            Bits scanned out of data register
     * @param logger
     *            Object with logging properties to use
     * 
     * @return 0x00 (success), 0x11 (transmit error), 0x33 (receive error)
     */
    protected static int hw_net_scan_dr(int jtagPort, int numBits,
            short[] scanIn, short[] scanOut, Logger logger) {
        checkJtagPort(jtagPort);
        logger.logOther("DR into TAP " + jtagPort + ":  # shorts="
                + scanIn.length + ", " + NetscanGeneric.shortsToString(scanIn));
        logger.logOther("   lentgth=" + numBits);
        int result = Netscan4JNI.netUSB_scan_dr(scanIn, numBits,
                scanOut, jtagPort);
        logger.logOther("DR out: # shorts=" + scanOut.length + ", "
                + NetscanGeneric.shortsToString(scanOut));
        return result;
    }

    /** Checks if parallel I/O port and index are in the allowed ranges */
    static void checkGpioID(int jtagPort, int index) {
        Netscan4Driver.checkJtagPort(jtagPort);
        if (index < 0 || index >= POD_GPIO.length) {
            Infrastructure.fatal("Index " + index + " not in allowed range 0.."
                    + (POD_GPIO.length - 1));
        }
    }

    /**
     * Set the logic level for a single channel of the parallel programmable
     * output signals from the JTAG tester to the chip.
     * 
     * @param jtagPort
     *            Which JTAG port ({@link #MIN_TAP}..{@link #MAX_TAP}) to
     *            set output on
     * @param index
     *            Which of the TAP's parallel outputs (0..2) to set
     * @param newLevel
     *            set parallel output <tt>HI</tt>?
     */
    static void setParallelIO(int jtagPort, int index, boolean newLevel) {
        checkGpioID(jtagPort, index);
        int value;
        if (newLevel) {
            value = 1;
        } else {
            value = 0;
        }
        int status = Netscan4JNI.netUSB_AccessScanGPIO(jtagPort,
                POD_GPIO[index], POD_IO_OUTPUT, value);
        if (status != 0) {
            Infrastructure
                    .fatal("net_access_gpio returned error code" + status);
        }
    }
}
