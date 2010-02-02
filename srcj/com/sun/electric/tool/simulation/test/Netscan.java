/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Netscan.java
 * Written by Eric Kim and Tom O'Neill, Sun Microsystems.
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
 * Initialization, configuration, and connection API for Corelis NET-1149.1/E
 * one-port boundary scan controller (JTAG tester device). Shifting data in and
 * out should instead be performed using {@link ChainControl}. All of the
 * methods could have been static, but we made them non-static to allow
 * device-independent JTAG control.
 * <p>
 * Here is an example of how to use <code>Netscan</code>:<BLOCKQUOTE><TT>
 * JtagTester jtag = new Netscan(JTAG_IP_ADDRESS); <BR>
 * ChainControl control = new ChainControl(XML_PATH, jtag, DEFAULT_VDD,
 * DEFAULT_TCK_KHZ); <BR>
 * </TT> </BLOCKQUOTE> The user can then call {@link ChainControl#setInBits}
 * &nbsp;and {@link ChainControl#shift}&nbsp;to program scan chains.
 */
public class Netscan extends NetscanGeneric {

    /** Default stop state for scan. Value equals 1: Run-Test/Idle */
    public static final short DEFAULT_STOP_STATE = 1;

    /** Number of channels of programmable parallel output */
    public static final int NUM_OUTPUT_PINS = 2;

    /**
     * Creates the <code>Netscan</code> object. Only one <code>Netscan</code>
     * or {@link Netscan4}&nbsp;instance is allowed, because the Netscan
     * library only supports one JTAG controller at a time. Connects to the JTAG
     * tester (by using the Net_Connect routine in the scan function library).
     * The tester must then be configured before use, by passing the new
     * <code>Netscan</code> object to
     * {@link ChainControl#ChainControl(String, JtagTester, float, int)}.
     * 
     * @param addressIP
     *            IP address of JTAG tester
     */
    public Netscan(String addressIP) {
        incrementNumTesters();

        // Connect to tester via ethernet
        logInit("Connecting to Netscan at " + addressIP);
        int status = NetscanJNI.net_connect(addressIP);
        if (status <= 0) {
            Infrastructure.fatal("NetscanJNI.net_connect(" + addressIP
                    + ") returned error code " + status);
        }

        // Parallel outputs have pull-ups so initial state is probably true
        logicOutput = new LogicSettableArray(NUM_OUTPUT_PINS);
        setParallelIO(logicOutput.getLogicStates());
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
     */
    void configure(float tapVolt, long kiloHerz) {
        int milliVolt = Math.round(tapVolt * 1000.f);

        logSet("Netscan configuring " + kiloHerz + " kHz and " + milliVolt
                + " mV");
        int status = NetscanJNI.net_configure(kiloHerz, DEFAULT_STOP_STATE,
                milliVolt);
        if (status != 0) {
            Infrastructure
                    .fatal("NetscanJNI.net_configure returned error code "
                            + status);
        }

        // Reset the JTAG controller
        reset();
    }

    /**
     * Reset the finite state machine of the chip's JTAG controller by briefly
     * setting the TRSTb signal <tt>LO</tt>. The IR becomes bypass
     * automatically.
     */
    public void reset() {
        int status = NetscanJNI.net_set_trst(0);
        if (status != 0)
            Infrastructure
                    .fatal("NetscanJNI.net_set_trst(0) returned error code "
                            + status);
        status = NetscanJNI.net_set_trst(1);
        if (status != 0)
            Infrastructure
                    .fatal("NetscanJNI.net_set_trst(1) returned error code "
                            + status);
    }

    /**
     * Reset the finite state machine of the chip's JTAG controller by briefly
     * setting the TMS signal <tt>HI</tt> for five cycles. The IR becomes bypass
     * automatically.
     */
    public void tms_reset() {
        int status = Netscan4JNI.netUSB_tms_reset(1);
        if (status != 0)
            Infrastructure
                    .fatal("Netscan4JNI.net_tms_reset("+1+") returned error code "
                            + status);
    }

    /**
     * Disconnect from the JTAG tester. Should be called before exiting JVM.
     */
    void disconnect() {
        int status = NetscanJNI.net_disconnect();
        if (status != 0) {
            Infrastructure
                    .fatal("NetscanJNI.net_disconnect() returned error code "
                            + status);
        }
    }

    /**
     * Set the logic levels for the two programmable output signals from the
     * JTAG tester to the chip.
     * 
     * @param newLevel
     *            set parallel outputs 0, 1 <tt>HI</tt>?
     */
    private void setParallelIO(boolean[] newLevel) {
        if (newLevel.length != NUM_OUTPUT_PINS) {
            Infrastructure.fatal("newLevel.length=" + newLevel.length
                    + ", expected " + NUM_OUTPUT_PINS);
        }
        short port_data = 0;

        if (newLevel[0])
            port_data += 1;
        if (newLevel[1])
            port_data += 2;

        int returnValue = NetscanJNI.net_set_parallel_io(port_data);
        if (returnValue != 0)
            Infrastructure.fatal("net_set_parallel_io returned error code "
                    + returnValue);
    }

    /**
     * Set the logic level for a single channel of the parallel programmable
     * output signals from the JTAG tester to the chip. Must update
     * {@link #logicOutput}.
     * 
     * @param index
     *            Which parallel output to set
     * @param newLevel
     *            set parallel output <tt>HI</tt>?
     */
    void setLogicOutput(int index, boolean newLevel) {
        logicOutput.setLogicState(index, newLevel);
        setParallelIO(logicOutput.getLogicStates());
    }

    /**
     * Write the bits <code>scanIn</code> to the JTAG controller's instruction
     * register. The first bit scanned in to the chip is the LSB of
     * <code>scanIn[0]</code>, and the first bit scanned out from the chip is
     * the LSB of <code>scanOut[0]</code>.
     * 
     * @param numBits
     *            The number of bits to shift
     * @param scanIn
     *            Bit sequence to write to instruction register
     * @param scanOut
     *            Bits scanned out of instruction register
     * @return 0x00 (success), 0x11 (transmit error), 0x33 (receive error)
     */
    protected int hw_net_scan_ir(int numBits, short[] scanIn,
            short[] scanOut, int drBits) {
        logOther("IR in:  # shorts=" + scanIn.length + ", "
                + shortsToString(scanIn));
        int result = NetscanJNI.net_scan_ir(scanIn, numBits, scanOut);
        logOther("IR out: # shorts=" + scanOut.length + ", "
                + shortsToString(scanOut));
        return result;
    }

    /**
     * Write the bits <code>scanIn</code> to the JTAG controller's data
     * register, and read back the bits <code>scanOut</code>. The first bit
     * scanned in to the chip is the LSB of <code>scanIn[0]</code>, and the
     * first bit scanned out from the chip is the LSB of <code>scanOut[0]</code>.
     * <p>
     * Extracted from netScan_DR to simplify overriding for different hardware,
     * e.g., in class <code>Netscan4</code>.
     * 
     * @param numBits
     *            The number of bits to shift
     * @param scanIn
     *            Bit sequence to write to data register
     * @param scanOut
     *            Bits scanned out of data register
     * @return 0x00 (success), 0x11 (transmit error), 0x33 (receive error)
     */
    protected int hw_net_scan_dr(int numBits, short[] scanIn,
            short[] scanOut) {
        logOther("DR in:  # shorts=" + scanIn.length + ", "
                + shortsToString(scanIn));
        logOther("   chain length=" + numBits);
        int result = NetscanJNI.net_scan_dr(scanIn, numBits, scanOut);
        logOther("DR out: # shorts=" + scanOut.length + ", "
                + shortsToString(scanOut));
        return result;
    }

}
