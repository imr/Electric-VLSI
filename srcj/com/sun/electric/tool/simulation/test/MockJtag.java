/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MockJtag.java
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
 * Mock up of the control of a {@link Netscan}&nbsp;JTAG tester, allows
 * simulated execution of test software even in the absence of a chip. Provides
 * configuration, shifting data, etc.
 */

public class MockJtag extends JtagTester {

    /** Number of GPIO pins on the tester */
    public static final int NUM_OUTPUT_PINS = 3;

    /** State of the parallel outputs */
    private LogicSettableArray logicOutput;

    public MockJtag() {
        logicOutput = new LogicSettableArray(NUM_OUTPUT_PINS);
        setParallelIO(logicOutput.getLogicStates());
    }

    /**
     * Configures the JTAG tester, setting its parameters, and resets the JTAG
     * controller (clears TRST* briefly). Can be run at any time after
     * initialization to change settings.
     * 
     * @param tapVolt
     *            signal (TAP) voltage in Volts
     * @param kiloHerz
     *            the TCK frequency in kHz (from 391 kHz to 40 MHz)
     */
    void configure(float tapVolt, long kiloHerz) {
        logSet("MockJTAG configure: set tapVolt=" + tapVolt + ", kiloHerz="
                + kiloHerz);
    }

    /**
     * Reset the finite state machine of the chip's JTAG controller by briefly
     * setting the TRST signal <tt>LO</tt>. The IR becomes bypass
     * automatically.
     */
    public void reset() {
        logSet("MockJTAG reset()");
    }

    /**
     * Reset the finite state machine of the chip's JTAG controller by briefly
     * setting the TMS signal <tt>HI</tt> for 5 cycles. The IR becomes bypass
     * automatically.
     */
    public void tms_reset() {
        logSet("MockJTAG reset()");
    }

    /**
     * Disconnect from the JTAG tester. Should be called before exiting JVM.
     */
    void disconnect() {
        logSet("MockJTAG disconnect()");
    }

    /**
     * Shift data in chain.inBits into the selected scan chain on the chip. The
     * previous data on the chip is shifted out into chain.outBits. Should only
     * be called by ChainNode.
     * 
     * @param chain
     *            Root scan chain to shift data into
     * @param readEnable
     *            whether to set opcode's read-enable bit
     * @param writeEnable
     *            whether to set opcode's write-enable bit
     * @param irBadSeverity
     *            action when bits scanned out of IR are wrong
     * @see ChainNode
     * @see Infrastructure#SEVERITY_NOMESSAGE
     * @see Infrastructure#SEVERITY_WARNING
     * @see Infrastructure#SEVERITY_NONFATAL
     * @see Infrastructure#SEVERITY_FATAL
     */
    void shift(ChainNode chain, boolean readEnable, boolean writeEnable,
            int irBadSeverity) {
        logOther("MockJTAG shift: opcode=" + chain.getOpcode() + "\n "
                + chain.getInBits());

        int length = chain.getLength();
        BitVector outBits = new BitVector(length, "MockJtag.shift().outBits");
        BitVector outBitsExpected = chain.getOutBitsExpected();
        for (int ibit = 0; ibit < length; ibit++) {
            if (outBitsExpected.isValid(ibit) == true) {
              /* need to handle shadowState carefully - for scanBB */
              if (!writeEnable) {
                outBits.set(ibit, outBitsExpected.get(ibit));
              } else {
                outBits.putIndiscriminate(ibit, chain.getShadowState().getIndiscriminate(ibit,1));
              }
            } else {
                outBits.clear(ibit);
            }
        }
        chain.getOutBits().putIndiscriminate(0, outBits);
    }

    /**
     * Set the logic levels for the parallel programmable output signals from
     * the JTAG tester to the chip.
     * 
     * @param newLevel
     *            set parallel outputs 0, 1 <tt>HI</tt>?
     */
    private void setParallelIO(boolean[] newLevel) {
        String msg = "Parallel output state now:";
        for (int ind = 0; ind < newLevel.length; ind++) {
            msg += " " + newLevel[ind];
        }
        System.out.println(msg);
    }

    /**
     * Set the logic level for a single channel of the parallel programmable
     * output signals from the JTAG tester to the chip. Updates
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
}
