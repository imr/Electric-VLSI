package com.sun.electric.tool.simulation.test;

/*
 * JtagTester.java
 * 
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 *
 * Created on September 17, 2004
 */

/**
 * Device-independent control of a single port on a JTAG tester. Provides
 * configuration, shifting data, etc. Also provides independent control of
 * multiple logic output levels. Constructor should connect to the JTAG
 * tester.
 * 
 * @author Tom O'Neill (toneill)
 */

public abstract class JtagTester extends Logger {
	public boolean printInfo = true;

    /** State of the logic output pins */
    protected LogicSettableArray logicOutput;

    private boolean scannedOutDataIsInverted = false;

    /**
     * Configures the JTAG tester, setting its parameters, and resets the JTAG
     * controller (clears TRST* briefly). Can be run at any time after
     * initialization to change settings.
     * <p>
     * Test programs should use {@link ChainControl#setJtagKhz(int)}&nbsp;and
     * {@link ChainControl#setJtagVolts(float)}&nbsp;instead of calling this
     * routine directly.
     * 
     * @param tapVolt
     *            signal (TAP) voltage in Volts
     * @param kiloHerz
     *            the TCK frequency in kHz (from 391 kHz to 40 MHz)
     */
    abstract void configure(float tapVolt, long kiloHerz);

    /**
     * Reset the finite state machine of the chip's JTAG controller by briefly
     * setting the TRST signal <tt>LO</tt>. The IR becomes bypass
     * automatically.
     */
    public abstract void reset();

    /**
     * Reset the finite state machine of the chip's JTAG controller by briefly
     * setting the TMS signal <tt>HI</tt> for 5 cycles. The IR becomes bypass
     * automatically.
     */
    public abstract void tms_reset();

    /**
     * Disconnect from the JTAG tester. Should be called before exiting JVM.
     */
    abstract void disconnect();

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
    abstract void shift(ChainNode chain, boolean readEnable,
            boolean writeEnable, int irBadSeverity);

    /**
     * Set the logic level for a single channel of the parallel programmable
     * output signals from the JTAG tester to the chip.
     * 
     * @param index
     *            Which parallel output to set
     * @param newLevel
     *            set parallel output <tt>HI</tt>?
     */
    abstract void setLogicOutput(int index, boolean newLevel);

    /**
     * Returns the logic state of the specified parallel output
     * 
     * @param index
     *            Which parallel output to query
     * 
     * @return state of specified parallel output
     */
    boolean getOutputState(int index) {
        return logicOutput.isLogicStateHigh(index);
    }

    /**
     * Tells that jtag controller that the scanned out data
     * it receives is inverted, so it must uninvert them.
     *
     * @param outputInverted
     *           if the output is inverted
     */
    public void setScanOutInverted(boolean outputInverted) {
        scannedOutDataIsInverted = outputInverted;
    }

    /**
     * Check that to see whether the scanned out data expected
     * to be inverted.
     *
     * @return if the data scanned out is inverted
     */
    public boolean isScanOutInverted() {
        return scannedOutDataIsInverted;
    }
}
