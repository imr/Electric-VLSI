/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TestNode.java
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
 * Represent a node in the scan chain hierarchy. The system node can use this
 * class without modification. All other nodes require additional information,
 * and so must use classes that extend this class.
 * <p>
 * The default readable, writeable, clearBehavior settings are passed down
 * through the hierarchy but can be overridden at any level. The default value
 * that obtains at a given leaf node is the actual value for the scan chain
 * elements contained therein.
 */
class TestNode extends MyTreeNode {

    /** clearBehavior value for scan chain elements which do not clear */
    public final static int CLEARS_NOT = 0;

    /** clearBehavior value for scan chain elements which clear high */
    public final static int CLEARS_LO = 1;

    /** clearBehavior value for scan chain elements which clear low */
    public final static int CLEARS_HI = 2;

    /** clearBehavior value for scan chain elements with unknown clearing */
    public final static int CLEARS_UNKNOWN = 3;

    /**
     * String representation of the values {@link #CLEARS_NOT},
     * {@link #CLEARS_LO}, etc.
     */
    public static final String[] CLEARS_STRINGS = { XMLIO.CLEARS_NOT_STRING,
            XMLIO.CLEARS_LO_STRING, XMLIO.CLEARS_HI_STRING,
            XMLIO.CLEARS_UNKNOWN_STRING };

    /**
     * Default unpredictable status for the scan chain elements below this node.
     * An unpredictable element has unpredictable scan out bits.
     */
    private boolean unpredictable = false;

    /**
     * Default readable capability for the scan chain elements below this node.
     * A readable element can read data, either from a shadow register or from
     * other parts of the chip. Default: true
     */
    private boolean readable = true;

    /**
     * Default writeable capability for the scan chain elements below this node.
     * A writeable element can write data, either to a shadow register or to
     * other parts of the chip. Default: true
     */
    private boolean writeable = true;

    /**
     * See {@link XMLIO#SHADOW_ACCESS_STRING}
     */
    private boolean usesShadow = false;

    /**
     * Whether the scan chain elements in this node write to a shadow register,
     * but read from a different location. No other circuits besides the
     * scan chain element write to the shadow register.
     */
    private boolean usesDualPortedShadow = false;

    /**
     * Default behavior of the scan chain elements below this node, when the
     * chip's Master Clear is set HI. Elements can clear to LO, HI, or not at
     * all. Default: clearing behavior unknown
     */
    private int clearBehavior = CLEARS_UNKNOWN;

    /**
     * Default constructor.
     * 
     * @param name
     *            name identifying the node
     * @param comment
     *            comment attached to this node, if any
     */
    public TestNode(String name, String comment) {
        super(name, comment);
    }

    public String toString() {
        return super.toString() + " " + getState();
    }

    /**
     * Return string representation of access and clears values.
     */
    String getState() {
        StringBuffer buffer = new StringBuffer("[");
        if (unpredictable)
            buffer.append("U");
        if (readable)
            buffer.append("R");
        if (writeable)
            buffer.append("W");
        if (usesShadow)
            buffer.append("S");
        if (usesDualPortedShadow)
            buffer.append("D");
        buffer.append("/" + CLEARS_STRINGS[clearBehavior] + "]");
        return buffer.toString();
    }

    /**
     * @return behavior of chain elements within node during master clear
     */
    public int getClearBehavior() {
        return clearBehavior;
    }

    /**
     * @return Returns whether chain elements within node have unpredictable
     *         scan out values
     */
    public boolean isUnpredictable() {
        return unpredictable;
    }

    /**
     * @return whether chain elements within node have readable access
     */
    public boolean isReadable() {
        return readable;
    }

    /**
     * @return whether chain elements within node have writeable access
     */
    public boolean isWriteable() {
        return writeable;
    }

    /**
     * @return true if the scan chain element has a predictable shadow
     *         register. Both read and write access this register, and
     *         no other circuits write to this register.
     */
    public boolean usesShadow() {
        return usesShadow;
    }

    /**
     * @return true if the scan chain element has a shadow register
     *         which only it writes to, and no other circuits. A read
     *         does NOT read from the shadow register in this case. 
     */
    public boolean usesDualPortedShadow() {
        return usesDualPortedShadow;
    }

    /**
     * Set behavior of chain elements within node during master clear
     * 
     * @param clearBehavior
     *            behavior of chain elements within node during master clear
     */
    void setClearBehavior(int clearBehavior) {
        this.clearBehavior = clearBehavior;
    }

    /**
     * @param unpredictable
     *            whether chain elements within node have unpredictable scan out
     *            values
     */
    void setUnpredictable(boolean unpredictable) {
        this.unpredictable = unpredictable;
    }

    /**
     * @param writeable
     *            whether chain elements within node have writeable access
     */
    void setWriteable(boolean writeable) {
        this.writeable = writeable;
    }

    /**
     * @param readable
     *            whether chain elements within node have readable access
     */
    void setReadable(boolean readable) {
        this.readable = readable;
    }

    /**
     * @param usesDualPortedShadow
     *            whether scan read and write ports access separate bits
     */
    void setUsesDualPortedShadow(boolean usesDualPortedShadow) {
        this.usesDualPortedShadow = usesDualPortedShadow;
    }

    /**
     * @param usesShadow
     *            whether chain elements within node have predictable shadow
     *            registers
     */
    public void setUsesShadow(boolean usesShadow) {
        this.usesShadow = usesShadow;
        if (usesShadow && isWriteable() == false) {
            Infrastructure.nonfatal("WARNING: non-writeable shadow register");
        }
    }

    /**
     * Compare access and clears, but not name of node. This can be used to
     * compare subchains without regard to name. See {@link #compare}. Function
     * not complete, may not be necessary
     */
    void compareAccessAndClears(TestNode that, String thisFile, String thatFile) {
        if (getClearBehavior() != that.getClearBehavior()) {
            System.out.println("**** Node " + thisFile + ":" + getPathString(1)
                    + " has clears '" + CLEARS_STRINGS[getClearBehavior()]
                    + "', but " + thatFile + ":" + that.getPathString(1)
                    + " has clears '" + CLEARS_STRINGS[that.getClearBehavior()]
                    + "'");
        }
    }

    /** Helper for CompareXML */
    void compare(TestNode that, String thisFile, String thatFile) {
        super.compare(that, thisFile, thatFile);
        //        compareAccessAndClears(that, thisFile, thatFile);
    }

    public static void main(String[] args) {
        TestNode node = new TestNode("sys1", "com1");
        System.out.println(node);
        node = new TestNode("sys2", "com2");

        System.out.println("Should complain about non-writeable shadow reg");
        node.setReadable(true);
        node.setWriteable(false);
        node.setUsesShadow(true);
        node.setClearBehavior(CLEARS_LO);
        System.out.println(node);
    }

}
