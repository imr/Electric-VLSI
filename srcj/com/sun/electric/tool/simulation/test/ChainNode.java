/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ChainNode.java
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

import java.util.List;
import java.util.ArrayList;

/**
 * Represents an entire scan chain, covering all of the scan chain elements at a
 * single address on the chip's JTAG controller. Sometimes called a "root" scan
 * chain, to emphasize distinction from a sub-chain. I/O to the chip occurs to
 * the entire root scan chain, rather than to individual
 * <code>SubchainNode</code>s.
 * <p>
 * 
 * By convention, the first bit in a BitVector or in a string always represents
 * the last bit scanned into or out of the chip. Thus 1) the bit and character
 * indices match the position of the corresponding scan chain element along the
 * s_in chain, 2) the strings match the left-to-right order in which scan chain
 * elements appear in most schematics, and 3) the order is consistent with the
 * order of scan chain nodes in the XML file.
 * <p>
 */

public class ChainNode extends SubchainNode {

    /**
     * Little-endian character string (e.g., "101010") representation of the
     * root scan chain's address at the JTAG controller. Bits 6 and 7 (the read
     * and write enable) are ignored during I/O with the chip.
     */
    final private String opcode;

    /**
     * Scan chain bit pattern to be shifted into the chip during the next call
     * to this.shift().
     */
    protected BitVector inBits;

    /**
     * Scan chain bit pattern read back from the chip after last call to
     * this.shift(). Should only be modified by Netscan.
     */
    protected BitVector outBits;

    /**
     * Expected value of outBits during the next call to this.shift(). Usually
     * this is just the previous value of inBits, but it can be modified by
     * Master Clears and reading from the chip.
     */
    protected BitVector outBitsExpected;

    /** Expected value of outBits for the previous call to this.shift(). */
    protected BitVector oldOutBitsExpected;

    /**
     * State of the scan chain elements' shadow register, for those that have
     * one.
     */
    protected BitVector shadowState;

    /** Whether any data has been shifted to this scan chain */
    private boolean initialized = false;

    /**
     * A list of shift listeners
     */
    private List<ShiftListener> listeners;


    /**
     * Constructor for a root scan chain.
     * 
     * @param name
     *            node name
     * @param opcode
     *            on-chip address of root scan chain
     * @param newLength
     *            number of scan chain elements in node
     * @param comment
     *            comment attached to this node
     */
    public ChainNode(String name, String opcode, int newLength, String comment) {
        super(name, newLength, comment);

        this.opcode = opcode;
        listeners = new ArrayList<ShiftListener>();

        createBitVectors();
    }

    public String toString() {
        return super.toString() + " (op=" + opcode + ")";
    }

    /**
     * Little-endian character string (e.g., "101010") representation of the
     * root scan chain's address at the JTAG controller. These are the low-order
     * 6 bits of the instruction register needed to access the chain.
     * 
     * @return Address of root scan chain (little endian)
     */
    String getOpcode() {
        return opcode;
    }

    /**
     * Set most recent bit sequence that was shifted out of the root scan chain
     * during this.shift(). Should only be used by
     * {@link NetscanGeneric#netScan_DR}.
     */
    void setOutBits(BitVector newOutBits) {
        outBits.put(0, newOutBits);
    }

    /**
     * Get the scan chain bit pattern to be shifted into the chip during the next call
     * to this.shift().
     * @return the in bits
     */
    public BitVector getInBits() { return inBits; }

    /**
     * Get the scan chain bit pattern read back from the chip after last call to
     * this.shift(). Should only be modified by Netscan.
     * @return the out bits
     */
    public BitVector getOutBits() { return outBits; }

    /**
     * Get expected value of outBits during the next call to this.shift(). Usually
     * this is just the previous value of inBits, but it can be modified by
     * Master Clears and reading from the chip.
     * @return the out bits expected
     */
    public BitVector getOutBitsExpected() { return outBitsExpected; }

    /**
     * Get the expected value of outBits for the previous call to this.shift().
     * @return the old out bits expected
     */
    public BitVector getOldOutBitsExpected() { return oldOutBitsExpected; }
    
    /**
     * Get the state of the scan chain elements' shadow register, for those that have
     * one.
     * @return the shadow state bits
     */
    public BitVector getShadowState() { return shadowState; }

    /**
     * This is for ChainG display
     * @return the outbits
     */
    protected BitVector getOutBitsIndiscriminate() { return outBits; }

    /**
     * Returns ancestor <code>ChipNode</code>.
     * 
     * @return Chip node that is the ancestor to this <code>ChainNode</code>
     */
    ChipNode getParentChip() {
        MyTreeNode node = this;
        while (node.getClass() != ChipNode.class) {
            node = node.getParent();
            if (node == null) {
                Infrastructure.fatal(node
                        + " does not have a ChipNode as an ancestor");
            }
        }
        return (ChipNode) node;
    }

    /**
     * Update shadowState data structure in response to a master clear. Call
     * this on a transition of master clear to <tt>HI</tt> _or_ <tt>LO</tt>,
     * or you may get false complaints about data shifted out not equalling
     * expected results.
     */
    void processMasterClear() {
        for (int ind = 0; ind < getLength(); ind++) {
            SubchainNode node = findNodeAtIndex(ind);
            if (node.usesShadow() || node.usesDualPortedShadow()) {
                int clears = node.getClearBehavior();

                // Note clearing has no effect on elements with CLEARS_NOT.
                // Otherwise sets the shadow register to HI, LO, or to an
                // unknown (invalid) state.
                if (clears == TestNode.CLEARS_LO) {
                    shadowState.clear(ind);
                } else if (clears == TestNode.CLEARS_HI) {
                    shadowState.set(ind);
                } else if (clears == TestNode.CLEARS_UNKNOWN) {
                    shadowState.invalidate(ind);
                }
            }
        }
    }

    /**
     * Resets all inBits to zero, or to clears state if clearable and specified to do so
     * @param useMasterClearState true to reset to clears state if clearable.
     */
    public void resetInBits(boolean useMasterClearState) {
        for (int i=0; i<getLength(); i++) {
            SubchainNode node = findNodeAtIndex(i);
            if (useMasterClearState && node.getClearBehavior() == TestNode.CLEARS_HI)
                inBits.set(i, true);
            else
                inBits.set(i, false);
        }
    }

    /**
     * Invalidate expected scan chain element values for this scan chain. Call
     * this after a period at a low voltage, or you may get false complaints
     * about data shifted out not equalling expected results. Sets
     * {@link ChainNode#initialized}&nbsp;to false to suppress "no bits being
     * compared" warning on next shift.
     */
    void invalidate() {
        outBitsExpected.invalidate();
        initialized = false;
    }

    /**
     * Shift data in this.inBits into the appropriate scan chain on the chip.
     * The previous data on the chip is shifted out into this.outBits, which is
     * then compared with expectation.
     * 
     * @param jtag
     *            JTAG tester object to perform shift with
     * @param readEnable
     *            whether to set opcode's read-enable bit
     * @param writeEnable
     *            whether to set opcode's write-enable bit
     * @param irBadSeverity
     *            action when bits scanned out of IR are wrong
     * @param noTestSeverity
     *            action when no consistency check is possible
     * @param errTestSeverity
     *            action when consistency check fails
     * @param logger
     *            Object with logging properties to use
     * 
     * @return true if the bits scanned out equal their expected values
     * @see Infrastructure#SEVERITY_NOMESSAGE
     * @see Infrastructure#SEVERITY_WARNING
     * @see Infrastructure#SEVERITY_NONFATAL
     * @see Infrastructure#SEVERITY_FATAL
     */
    boolean shift(JtagTester jtag, boolean readEnable, boolean writeEnable,
            int irBadSeverity, int noTestSeverity, int errTestSeverity,
            Logger logger) {
        logger.logOther("------ " + getPathString() + ", R=" + readEnable
                + ", W=" + writeEnable);

        jtag.shift(this, readEnable, writeEnable, irBadSeverity);

        boolean noErrors = checkOutBits(readEnable, noTestSeverity,
                errTestSeverity);

        // After shift, all elements should hold inBits
        oldOutBitsExpected.putIndiscriminate(0, outBitsExpected);
        outBitsExpected.put(0, inBits);

        // Update the expected bits from any shadow registers
        // Note: this implies that shadow registers are not modified by the chip
        if (readEnable) {
            for (int ind = 0; ind < getLength(); ind++) {
                SubchainNode node = findNodeAtIndex(ind);
                if (node.isReadable() && (node.usesShadow() && !node.usesDualPortedShadow())) {
                    if (shadowState.isValid(ind)) {
                        boolean state = shadowState.get(ind);
                        outBitsExpected.set(ind, state);
                    }
                }
            }
        }
        // Update the shadowState of any elements that wrote to their
        // shadow registers
        if (writeEnable) {
            for (int ind = 0; ind < getLength(); ind++) {
                SubchainNode node = findNodeAtIndex(ind);
                if (node.isWriteable() && (node.usesShadow() || node.usesDualPortedShadow())) {
                    boolean state = inBits.get(ind);
                    shadowState.set(ind, state);
                }
            }
        }
        shiftCompleted();

        return noErrors;
    }

    /**
     * Shift one bit of data in this.inBits into the appropriate scan chain on
     * the chip. No consistency checking is performed on bit scanned out. This
     * is provided only for measuring chain length in ChainTest, and should not
     * otherwise be used.
     * 
     * @param jtag
     *            JTAG tester object to perform shift with
     * @param readEnable
     *            whether to set opcode's read-enable bit
     * @param writeEnable
     *            whether to set opcode's write-enable bit
     * @param irBadSeverity
     *            action when bits scanned out of IR are wrong
     * @param logger
     *            Object with logging properties to use
     * @return the bit that got shifted out
     * 
     * @see Infrastructure#SEVERITY_NOMESSAGE
     * @see Infrastructure#SEVERITY_WARNING
     * @see Infrastructure#SEVERITY_NONFATAL
     * @see Infrastructure#SEVERITY_FATAL
     */
    boolean shiftOneBit(JtagTester jtag, boolean readEnable,
            boolean writeEnable, int irBadSeverity, Logger logger) {
        logger.logOther("***** " + getPathString() + ", inBits=" + inBits);

        /*
         * Temporarily change chain length to 1. Note outBits could potentially
         * be clobbered after each length change (currently it isn't), so safest
         * to store intermediate outbit.
         */
        int length = getLength();
        setLength(1);
        jtag.shift(this, readEnable, writeEnable, irBadSeverity);
        boolean outbit = outBits.get(0);
        setLength(length);

        /*
         * Since this is only needed during development of the scan chain XML
         * file, it's not worth figuring out resulting state of chain
         */
        outBitsExpected.invalidate();
        oldOutBitsExpected.invalidate();
        shadowState.invalidate();

        return outbit;
    }

    /**
     * Called when the length of a node underneath this root node changes,
     * recomputes the root node's length. In addition, resizes the inBits and
     * outBits arrays to account for the change. Previous array contents are
     * overwritten with 0. Note method overrides that in parent.
     */
    void lengthChanged() {

        // Recompute current node's length, taking children into account
        this.computeLength();

        createBitVectors();
    }

    /**
     * Find descendent of current <code>ChainNode</code> that contains the
     * scan chain element at index bitIndex.
     * 
     * @param bitIndex
     *            index of scan chain element
     * @return scan chain node containing the specified element.
     */
    SubchainNode findNodeAtIndex(int bitIndex) {
        if (bitIndex < 0 || bitIndex > this.getLength()) {
            throw new IllegalArgumentException("bitIndex " + bitIndex +
                    " not in allowed range 0.." + this.getLength());
        }
        SubchainNode node = this;
        SubchainNode parent = null;
        for (int indChild=0, nodeIndex = 0;; indChild++) {
            // Find index in scan chain of next sibling node
            int nextIndex = nodeIndex + node.getLength();

            if (nextIndex <= bitIndex) {
                // Target not within this node, accumlate length and try
                // next sibling
                nodeIndex = nextIndex;
            } else {
                // Within this node. If leaf node, we have found it. Else,
                // try first child.
                int nkids = node.getChildCount();
                if (nkids == 0) {
                    return node;
                }
                parent = node;
                indChild = 0;
            }
            node = (SubchainNode) parent.getChildAt(indChild);
        }
    }

 
    /*
     * Called by constructor or when length changed, replaces the BitVector
     * members with new versions of the correct length.
     */
    protected void createBitVectors() {
        int newLength = getLength();
        if (newLength < 0)
            newLength = 0;

        inBits = new BitVector(newLength, getPathString() + ".inBits");
        outBitsExpected = new BitVector(newLength, getPathString()
                + ".outBitsExpected");
        oldOutBitsExpected = new BitVector(newLength, getPathString()
                + ".oldOutBitsExpected");
        outBits = new BitVector(newLength, getPathString() + ".outBits");
        shadowState = new BitVector(newLength, getPathString() + ".shadowState");
    }

    /**
     * Checks if the bits scanned out of the scan chain equal their expected
     * values. Optionally prints a warning to stderr if deviations are found.
     * 
     * @param readEnable
     *            whether readable chain elements read values in
     * @param noTestSeverity
     *            action when no consistency check is possible
     * @param errTestSeverity
     *            action when consistency check fails
     * @return true if no deviations found
     */
    private boolean checkOutBits(boolean readEnable, int noTestSeverity,
            int errTestSeverity) {

        int length = getLength();

        /*
         * All elements, except those that are unpredictable or have read a
         * value from another part of the chip, should now have the value in the
         * previous inBits
         */
        for (int ind = 0; ind < length; ind++) {
            SubchainNode node = findNodeAtIndex(ind);
            if (node.isUnpredictable()) {
                outBitsExpected.invalidate(ind);
            } else if (readEnable && node.isReadable()) {

                /*
                 * Can only know read value of an element with a shadow register
                 * that is in a known state
                 */
                if (node.usesShadow() && shadowState.isValid(ind)) {
                    outBitsExpected.set(ind, shadowState.get(ind));
                } else {
                    outBitsExpected.invalidate(ind);
                }
            }
        }

        // If no bits being compared, optionally print warning message
        if (outBitsExpected.isInvalid()) {
            if (initialized || noTestSeverity == Infrastructure.SEVERITY_FATAL
                    || noTestSeverity == Infrastructure.SEVERITY_NONFATAL) {
                Infrastructure.error(noTestSeverity, getPathString()
                        + ".shift() warning: no bits being compared, "
                        + "see ${TEST_ROOT}/FAQ.html");
            }
        }
        initialized = true;

        // Create a BitVector that has a bit set for every deviation
        BitVector errors = new BitVector(length, "checkOutBits()-errors");
        for (int iBit = 0; iBit < length; iBit++) {
            if (outBitsExpected.isValid(iBit)
                    && outBits.get(iBit) != outBitsExpected.get(iBit)) {
                errors.set(iBit);
            } else {
                errors.clear(iBit);
            }
        }

        boolean noErrors = errors.isEmpty();
        if (noErrors == false) {
            Infrastructure.error(errTestSeverity, getPathString()
                    + ".shift() error:\n  expected: " + outBitsExpected
                    + "\n  outBits: " + outBits
                    + "\nFor details, see the Appendix in 'Using the Test"
                    + "\nSoftware Library' for details about this error.");
        }
        return noErrors;
    }

    int findRun(int indStart) {
        SubchainNode start = findNodeAtIndex(indStart);
        int clears = start.getClearBehavior();
        boolean read = start.isReadable();
        boolean write = start.isWriteable();
        boolean shadow = start.usesShadow();
        boolean unpredictable = start.isUnpredictable();

        int ind;
        for (ind = indStart; ind < getLength(); ind++) {
            SubchainNode subchain = findNodeAtIndex(ind);
            if (subchain.getClearBehavior() != clears
                    || subchain.isReadable() != read
                    || subchain.isWriteable() != write
                    || subchain.usesShadow() != shadow
                    || subchain.isUnpredictable() != unpredictable) {
                return ind;
            }
        }

        return ind;
    }

    /** Helper for CompareXML */
    void compare(ChainNode that, String thisFile, String thatFile) {
        //       System.out.println("Differences for chain " + this);
        super.compare(that, thisFile, thatFile);
        int length = getLength();
        if (that.getLength() != length) {
            System.out.println("**** Chain " + thisFile + ":" + this
                    + " has length " + length + ", but " + thatFile + ":"
                    + that + " has " + that.getLength()
                    + ".  Aborting comparison");
            Infrastructure.exit(1);
        }

        int thisIndex = 0, thatIndex = 0;
        while (thisIndex < length && thatIndex < length) {
            SubchainNode thisSubchain = findNodeAtIndex(thisIndex);
            SubchainNode thatSubchain = that.findNodeAtIndex(thatIndex);

            int thisStartIndex = thisIndex;
            int thatStartIndex = thatIndex;
            thisIndex = findRun(thisIndex);
            thatIndex = that.findRun(thatIndex);

            //            System.out.println("Comparing run from " + thisStartIndex
            //                    + " to " + thisIndex + ", starting at "
            //                    + thisSubchain.getPathString());
            if ((thisIndex - thisStartIndex) != (thatIndex - thatStartIndex)) {
                System.out.println("**** " + thisFile
                        + " has subchain run of length "
                        + +(thisIndex - thisStartIndex)
                        + " starting at subchain "
                        + thisSubchain.getPathString() + ", but " + thatFile
                        + " has run of length " + (thatIndex - thatStartIndex)
                        + " starting at subchain "
                        + thatSubchain.getPathString());
            }
            String thisState = thisSubchain.getState();
            String thatState = thatSubchain.getState();
            if (thisState.equals(thatState) == false) {
                System.out.println("**** Subchain run starting at " + thisFile
                        + ":" + thisSubchain.getPathString() + " has mode "
                        + thisState + ", but run starting at " + thatFile
                        + ": " + thatSubchain.getPathString() + " has mode "
                        + thatState);
            }
        }
    }

    public static interface ShiftListener {
        public void shiftCompleted(ChainNode node);
    }

    public void addListener(ShiftListener l) {
        listeners.add(l);
    }

    public void removeListener(ShiftListener l) {
        listeners.remove(l);
    }

    private void shiftCompleted() {
        for (ShiftListener l : listeners) {
            l.shiftCompleted(this);
        }
    }


    public static void main(String[] args) {
        String filename, path;
        int index;
        if (args.length >= 3) {
            filename = args[0];
            path = args[1];
            index = Integer.parseInt(args[2]);
        } else {
            filename = "heater.xml";
            path = "heater.pScan";
            index = 40;
        }
        ChainModel cm = new ChainModel(filename);
        ChainNode node = (ChainNode) cm.findNode(path);
        SubchainNode found = node.findNodeAtIndex(index);
        System.out.println(found.getPathString());
    }
}
