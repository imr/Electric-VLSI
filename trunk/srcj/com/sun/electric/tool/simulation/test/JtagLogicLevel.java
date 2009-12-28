/*
 * Created on Aug 26, 2004
 *
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 *
 */
package com.sun.electric.tool.simulation.test;

/**
 * Settable logic level provided by a single pin on one port of a JTAG tester.
 * If the tester is a 1-port {@link Netscan}, then the pin is one of the 2
 * parallel output channels and allowed <code>index</code> values are
 * <tt>0</tt> and <tt>1</tt>. If it is a 4-port {@link Netscan4}, then the
 * pin is one of the three GPIO pins and allowed <code>index</code> values are
 * <tt>0</tt> ,&nbsp; <tt>1</tt>, and <tt>2</tt>.
 * 
 * @author Tom O'Neill (toneill)
 */
public class JtagLogicLevel implements LogicSettable {

    /* JTAG tester providing parallel output */
    private JtagTester jtag;

    /* Index identifying which parallel output provides the logic level */
    private int index;

    /**
     * Creates a {@link LogicSettable}&nbsp;that controls a single parallel
     * output or GPIO pin on the TAP connector for a JTAG tester port. Allowed
     * index values are <tt>0..1</tt> for {@link Netscan}&nbsp;and
     * {@link MockJtag}, and <tt>0..2</tt> for {@link Netscan4}.
     * 
     * @param control
     *            Control for JTAG tester in question
     * @param index
     *            Index of output on the TAP, 0..(N-1)
     *
     * @deprecated instead use {@link JtagLogicLevel#JtagLogicLevel(JtagTester, int)}
     */
    public JtagLogicLevel(ChainControl control, int index) {
        this.jtag = control.getJtag();
        this.index = index;
    }

    /**
     * Creates a {@link LogicSettable}&nbsp;that controls a single parallel
     * output or GPIO pin on the TAP connector for a JTAG tester port. Allowed
     * index values are <tt>0..1</tt> for {@link Netscan}&nbsp;and
     * {@link MockJtag}, and <tt>0..2</tt> for {@link Netscan4}.
     * 
     * @param jtag
     *            JTAG tester in question
     * @param index
     *            Index of output on the TAP, 0..(N-1)
     */
    public JtagLogicLevel(JtagTester jtag, int index) {
        this.jtag = jtag;
        this.index = index;
    }

    /**
     * For the love of god, don't use this constructor! Mountains may crumble,
     * rivers may flood, fires may rage!
     * 
     * @deprecated
     */
    public JtagLogicLevel(ChainControl control, String chipPath, int index) {
        this(control.getJtag(), index);
    }

    /**
     * Gets logic state for this parallel output channel
     * 
     * @see com.sun.electric.tool.simulation.test.LogicSettable#isLogicStateHigh()
     */
    public boolean isLogicStateHigh() {
        return jtag.getOutputState(index);
    }

    /**
     * Sets logic level for this parallel output channel
     * 
     * @see com.sun.electric.tool.simulation.test.LogicSettable#setLogicState(boolean)
     */
    public void setLogicState(boolean logicState) {
        jtag.setLogicOutput(index, logicState);
    }

    public static void main(String[] args) {
    }
}
