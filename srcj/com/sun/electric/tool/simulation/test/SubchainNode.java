/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SubchainNode.java
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

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Represent a single node of a scan chain. This is a subset of a scan chain,
 * chosen according to a hierarchy imposed in software. The division of the scan
 * chain into subchains is for convenience in accessing specific scan chain
 * elements, and does not have a direct functional significance with respect to
 * the chip's scan chain.
 * <p>
 * By convention, the first bit in a BitVector or in a string always represents
 * the last bit scanned into or out of the chip. Thus 1) the bit and character
 * indices match the position of the corresponding scan chain element along the
 * s_in chain, 2) the strings match the left-to-right order in which scan chain
 * elements appear in most schematics, and 3) the order is consistent with the
 * order of scan chain nodes in the XML file.
 */
public class SubchainNode extends TestNode {

    /**
     * Number of scan chain elements in or below this node. Includes
     * contributions from all scan chain nodes underneath the current one.
     */
    private int length;

    /**
     * Optional: name of I/O pad associated with this node. Currently used by
     * {@link SamplerControl}&nbsp;to provide non-conflicting access to sampler
     * output pins.
     */
    public String pin;

    /**
     * Optional: name of data out wire or bus. This is the wire that the scan
     * data is applied to on "write". For simulation purposes, the scan chain
     * can be bypassed, and scan values can be applied directly to the data
     * out bits.
     */
    private DataNet dataNet;
    private DataNet dataNet2;

    /**
     * Default constructor.
     * 
     * @param name
     *            name identifying the node
     * @param length
     *            number of scan chain elements in node
     * @param comment
     *            comment attached to this node, if any
     */
    public SubchainNode(String name, int length, String comment) {
        super(name, comment);
        setLength(length);
        pin = null;
    }

    /**
     * Default constructor.
     * 
     * @param name
     *            name identifying the node
     * @param length
     *            number of scan chain elements in node
     * @param pin
     *            name of I/O pad associated with this node
     * @param comment
     *            comment attached to this node, if any
     * @param dataNet
     *            net (may be bus) name that data is written and read to.
     * May contains options (RWI). ex: net1(RW).
     * @param dataNet2
     *            net (may be bus) name that data is written and read to.
     * May contains options (RWI). ex: net1(RW).
     */
    public SubchainNode(String name, int length, String pin, String comment,
                        String dataNet, String dataNet2) {
        super(name, comment);
        setLength(length);
        this.pin = pin;
        if (dataNet == null) dataNet = "";
        if (dataNet2 == null) dataNet2 = "";
        this.dataNet = new DataNet(dataNet);
        this.dataNet2 = new DataNet(dataNet2);
    }

    public String toString() {
        return super.toString() + " (len=" + getLength() + ")";
    }

    /** Return number of scan chain elements in or below this node */
    protected void setLength(int length) {
        this.length = length;
    }

    /** Return number of scan chain elements in or below this node */
    int getLength() {
        return length;
    }

    public DataNet getDataNet() { return dataNet; }
    public DataNet getDataNet2() { return dataNet2; }

    /**
     * Override addChild method: update length so that it includes the scan
     * chain elements in the new child.
     */
    void addChild(MyTreeNode newNode) {
        super.addChild(newNode);
        lengthChanged();
    }

    /**
     * Called when the length of the current node changes, recomputes all of the
     * affected lengths up to the scan chain root
     */
    void lengthChanged() {

        // Recompute current node's length, taking children into account
        this.computeLength();

        // If appropriate, (recursively) update the length of the parent
        if (getParent() == null || getParent().getClass() == ChipNode.class) {
            return;
        }
        ((SubchainNode) getParent()).lengthChanged();
    }

    /**
     * Return a copy of the scan chain bit pattern that will be written to
     * current node on chip after the next call to {@link ChainNode#shift}
     * &nbsp;for this chain. The first element of the bit vector represents the
     * scan chain element that is scanned in last.
     * 
     * @return Bit vector with pending bit pattern for current node
     */
    BitVector getInBits() {
        ChainNode root = getParentChain();
        BitVector bitVector = root.getInBits().get(getBitIndex(), getLength());
        return bitVector;
    }

    /**
     * Return a copy of the scan chain bit pattern that was read from current
     * node on chip after the last call to {@link ChainNode#shift}&nbsp;for
     * this chain. The first element of the bit vector represents the scan chain
     * element that is scanned out last.
     * 
     * @return Bit vector with previous bit pattern for current node
     */
    BitVector getOutBits() {
        ChainNode root = getParentChain();
        return root.getOutBits().get(getBitIndex(), getLength());
    }

    /**
     * Return a copy of the scan chain bit pattern that was expected to be
     * shifted out of the current node during the last call to
     * {@link ChainNode#shift}&nbsp;for the parent chain. Often this is just
     * the previous value of <code>inBits</code> from the second-to last call
     * to shift(), but it can be modified by processMasterClear() and
     * read-enabled shifts. The first element of the bit vector represents the
     * scan chain element that would be scanned out last.
     * <p>
     * Note {@link ChainNode#oldOutBitsExpected}is more useful for debugging a
     * scan chain than {@link ChainNode#outBitsExpected}, because in general
     * does not generally correspond to what was used in the consistency
     * checking.
     * 
     * @return Bit vector with expected bit pattern for current node
     */
    BitVector getOldOutBitsExpected() {
        ChainNode root = getParentChain();
        return root.getOldOutBitsExpected().getIndiscriminate(getBitIndex(),
                getLength());
    }

    /**
     * Set scan chain bit pattern that will be written to current node on chip
     * after the next call to {@link ChainNode#shift}&nbsp;for this chain. The
     * first element of the bit vector represents the scan chain element that is
     * scanned in last.
     * 
     * @param newBits
     *            Bit array containing new settings
     */
    void setInBits(BitVector newBits) {
        int nbits = newBits.getNumBits();
        int nbitsNode = this.getLength();
        if (nbits != nbitsNode) {
            Infrastructure.fatal("Length of input BitVector " + newBits
                    + " is " + nbits + ", but it must equal length "
                    + nbitsNode + " of SubchainNode " + this);
        }

        ChainNode chainRoot = getParentChain();
        int fromIndex = this.getBitIndex();
        chainRoot.getInBits().put(fromIndex, newBits);
    }

    /**
     * Set scan chain bit pattern that will be written to current node on chip
     * after the next call to {@link ChainNode#shift}&nbsp;for this chain. The
     * first character in the string represents the scan chain element that is
     * scanned in last.
     * 
     * @param newBits
     *            Character string containing new settings (e.g., "111001")
     */
    void setInBits(String newBits) {
        int nbits = newBits.length();
        int nbitsNode = this.getLength();
        if (nbits != nbitsNode) {
            Infrastructure.fatal("Length of input BitVector " + newBits
                    + " is " + nbits + ", but it must equal length "
                    + nbitsNode + " of SubchainNode " + this);
        }

        ChainNode chainRoot = getParentChain();
        int fromIndex = this.getBitIndex();
        chainRoot.getInBits().put(fromIndex, newBits);
    }

    /**
     * Recompute the length of the current node, by adding up the contributions
     * from all of its children
     */
    protected void computeLength() {
        int childCount = getChildCount();
        int newLength;
        if (childCount > 0) {
            newLength = 0;
            for (int ind = 0; ind < childCount; ind++) {
                newLength += ((SubchainNode) getChildAt(ind)).getLength();
            }
            setLength(newLength);
        }
    }

    /**
     * This is only used for ChainG display
     * @return the outbits
     */
    protected BitVector getOutBitsIndiscriminate() {
        ChainNode root = getParentChain();
        return root.getOutBits().getIndiscriminate(getBitIndex(), getLength());
    }

    /**
     * This is only used for ChainG display
     * @return the inbits
     */
    protected BitVector getInBitsIndiscriminate() {
        ChainNode root = getParentChain();
        return root.getInBits().getIndiscriminate(getBitIndex(), getLength());
    }

    /**
     * Return index along the scan chain of the first element of the node. The
     * address is computed as the address of the parent plus the siblings before
     * the node in the MyTreeNode hierarchy.
     * 
     * @return absolute address of the node wrt the chain root
     */
    public int getBitIndex() {
        if (getParent() == null || getParent().getClass() == ChipNode.class || getParent().getClass() == TestNode.class) {
            return 0;
        } //chainRoot
        int sumSibling = 0;
        for (int iChild = 0; iChild < getParent().getIndex(this); iChild++) {
            sumSibling += ((SubchainNode) getParent().getChildAt(iChild))
                    .getLength();
        }
        int addr = ((SubchainNode) getParent()).getBitIndex() + sumSibling;
        return addr;
    }

    /** Return the chainRoot node that this node is a descendent of */
    public ChainNode getParentChain() {
        MyTreeNode node;
        for (node = this; !ChainNode.class.isInstance(node); node = node
                .getParent())
            ;
        return (ChainNode) node;
    }

    /**
     * Return path from chainRoot to this node. Suppresses the name of the
     * system (root) node, since it is always the same. I.e., path starts at the
     * chip level.
     */
    public String getPathString() {
        return getPathString(1);
    }

    // =======================================================================

    public static class DataNet {
        private final String name;            // full path name of the net
        private final boolean readable;       // if scan chain can read from this net
        private final boolean writeable;      // if scan chain can write to this net
        private final boolean inverted;       // if sense of data is inverted

        private static final Pattern namePat = Pattern.compile("(.+?)\\((.+?)\\)");

        protected DataNet(String xmlNetName) {
            Matcher m = namePat.matcher(xmlNetName);
            String netname;
            if (m.matches()) {
                netname = m.group(1);
                String options = m.group(2).toLowerCase();
                readable = (options.indexOf('r') != -1) ? true : false;
                writeable = (options.indexOf('w') != -1) ? true : false;
                inverted = (options.indexOf('i') != -1) ? true : false;
            } else {
                netname = xmlNetName;
                readable = false;
                writeable = false;
                inverted = false;
            }
            name = netname;
        }
        protected DataNet(String netName, boolean readable, boolean writeable, boolean inverted) {
            this.name = netName;
            this.readable = readable;
            this.writeable = writeable;
            this.inverted = inverted;
        }
        public String getName() { return name; }
        public boolean isReadable() { return readable; }
        public boolean isWriteable() { return writeable; }
        public boolean isInverted() { return inverted; }
        public String toString() { return "DataNet "+name+"("+(readable?"R":"")+(writeable?"W":"")+
                (inverted?"I":"")+")"; }
    }

    // =======================================================================


    /**
     * Unit test. Creates a scan chain hierarchy: level0 -> {level1a, level1b};
     * level1b -> {level2a, level2b}. Uses it to test the setBits() and
     * getBits() methods.
     */
    public static void main(String[] args) {

        ChainNode level0 = new ChainNode("level0", "001", 0, "");
        SubchainNode level1a = new SubchainNode("level1a", 5, "frog");
        SubchainNode level1b = new SubchainNode("level1b", 0, "frog");
        SubchainNode level2a = new SubchainNode("level2a", 7, "frog");
        SubchainNode level2b = new SubchainNode("level2b", 3, "frog");

        level0.addChild(level1a);
        level0.addChild(level1b);
        level1b.addChild(level2a);
        level1b.addChild(level2b);

        String path = level2a.getPathString();
        System.out.println("path string, starting at level 1: " + path);
        System.out.println("  length = " + level2a.getLength());

        level2a.setInBits("1000111");
        System.out.println("Set bits in level2a = " + level2a.getInBits());
        System.out.println("Full bit sequence in level0 = " + level0.getInBits());
    }
}
