/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JtagLogicLevel.java
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
 * Settable logic level provided by a single pin on one port of a JTAG tester.
 * If the tester is a 1-port {@link Netscan}, then the pin is one of the 2
 * parallel output channels and allowed <code>index</code> values are
 * <tt>0</tt> and <tt>1</tt>. If it is a 4-port {@link Netscan4}, then the
 * pin is one of the three GPIO pins and allowed <code>index</code> values are
 * <tt>0</tt> ,&nbsp; <tt>1</tt>, and <tt>2</tt>.
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
