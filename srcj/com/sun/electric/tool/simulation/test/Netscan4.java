/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Netscan4.java
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
 * Connection and reset API for a single port on the Corelis NETUSB-1149.1/E
 * four-port boundary scan controller (JTAG tester device). Configuration and
 * shifting are Shifting data in and out should instead be performed using
 * {@link ChainControl}.
 * <p>
 * Main difference from {@link Netscan}&nbsp;is that the hardware requires a
 * TAP number, which must be specified in the constructor.
 * <p>
 * Here is an example of how to use <code>Netscan4</code>:<BLOCKQUOTE><TT>
 * JtagTester jtag = new Netscan4(JTAG_IP_ADDRESS, JTAG_TAP_NUMBER); <BR>
 * ChainControl control = new ChainControl(XML_PATH, jtag, DEFAULT_VDD,
 * DEFAULT_TCK_KHZ); <BR>
 * </TT> </BLOCKQUOTE> The user can then call {@link ChainControl#setInBits}
 * &nbsp;and {@link ChainControl#shift}&nbsp;to program scan chains.
 */
public class Netscan4 extends NetscanGeneric {

    /** Port (TAP) on the 4-port tester to use */
    public final int jtagPort;

    /**
     * Creates the <code>Netscan4</code> object. Only one
     * <code>Netscan4</code> or {@link Netscan}&nbsp;instance is allowed,
     * because the Netscan library only supports one JTAG controller at a time.
     * Connects to the JTAG tester (by using the NetUSB_Connect routine in the
     * scan function library). The tester must then be configured before use, by
     * passing the new <code>Netscan4</code> object to
     * {@link ChainControl#ChainControl(String, JtagTester, float, int)}.
     * 
     * @param addressIP
     *            IP address of JTAG tester
     * @param jtagPort
     *            port (TAP) on the JTAG tester to use
     */
    public Netscan4(String addressIP, int jtagPort) {

        Netscan4Driver.registerPort(addressIP, jtagPort);
        this.jtagPort = jtagPort;

        // Parallel outputs have pull-ups so initial state is probably true
        logicOutput = new LogicSettableArray(Netscan4Driver.NUM_OUTPUT_PINS);
        for (int gpio = 0; gpio < Netscan4Driver.NUM_OUTPUT_PINS; gpio++) {
            Netscan4Driver.setParallelIO(jtagPort, gpio, logicOutput
                    .isLogicStateHigh(gpio));
        }
    }

    /**
     * Return the IP address of the Netscan4
     * 
     * @return Returns the IP address of the Netscan4
     */
    public String getAddressIP() {
        return Netscan4Driver.getAddressIP();
    }

    /**
     * Returns the JTAG port to use for this JTAG tester object
     * 
     * @return JTAG port to use for I/O with this scan chain
     */
    public int getJtagPort() {
        return jtagPort;
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
        Netscan4Driver.configure(tapVolt, kiloHerz, this);
    }

    /**
     * Reset the finite state machine of the chip's JTAG controller by briefly
     * setting the TRSTb signal <tt>LO</tt>. The IR becomes bypass
     * automatically.
     */
    public void reset() {
        Netscan4Driver.reset();
    }

    /**
     * Reset the finite state machine of the chip's JTAG controller by briefly
     * setting the TMS signal <tt>HI</tt> for 5 cycles. The IR becomes bypass
     * automatically.
     */
    public void tms_reset() {
        Netscan4Driver.tms_reset(jtagPort);
    }

    /**
     * Disconnect from the port on the JTAG tester. Should be called before
     * exiting JVM.
     */
    void disconnect() {
        Netscan4Driver.deregisterPort();
    }

    /**
     * Write the bits <code>scanIn</code> to the JTAG controller's instruction
     * register. The TAP port is chosen according to what chip the scan chain is
     * a member of.
     * <p>
     * The first bit scanned in to the chip is the LSB of <code>scanIn[0]</code>,
     * and the first bit scanned out from the chip is the LSB of
     * <code>scanOut[0]</code>.
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
        return Netscan4Driver.hw_net_scan_ir(jtagPort, numBits, scanIn, scanOut, this);
    }

    /**
     * Write the bits <code>scanIn</code> to the JTAG controller's data
     * register, and read back the bits <code>scanOut</code>. The TAP port is
     * chosen according to what chip the scan chain is a member of.
     * <p>
     * The first bit scanned in to the chip is the LSB of <code>scanIn[0]</code>,
     * and the first bit scanned out from the chip is the LSB of
     * <code>scanOut[0]</code>.
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
        return Netscan4Driver.hw_net_scan_dr(jtagPort, numBits, scanIn, scanOut, this);
    }

    /**
     * Set the logic level for a single channel of the parallel programmable
     * output signals from this port on the JTAG tester to the chip. Updates
     * {@link #logicOutput}.
     * 
     * @param index
     *            Which of the TAP's parallel outputs (0..2) to set
     * @param newLevel
     *            set parallel output <tt>HI</tt>?
     */
    void setLogicOutput(int index, boolean newLevel) {
        logicOutput.setLogicState(index, newLevel);
        Netscan4Driver.setParallelIO(jtagPort, index, newLevel);
    }
}
