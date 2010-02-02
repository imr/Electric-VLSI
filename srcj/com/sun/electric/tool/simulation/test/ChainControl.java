/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ChainControl.java
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

import javax.swing.tree.TreePath;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreeModel;
import java.util.ArrayList;

/**
 * Main API for scan chain programming, provides control of a single port on a
 * JTAG tester. The scan chain hieararchy is ("system" node) ->
 * <code>ChipNode</code>-><code>ChainNode</code>->
 * <code>SubchainNode</code>-><code>SubchainNode</code> ..., where there
 * can be multiple <code>ChainNode</code> entries per <code>ChipNode</code>
 * and multiple <code>SubchainNode</code> entries per <code>ChainNode</code>
 * or <code>SubchainNode</code>. The system node is a <code>TestNode</code>
 * object. The division of a scan chain into sub chains is for convenience in
 * accessing particular scan chain elements, and does not necessarily correspond
 * to any physical hierarchy on the chip.
 * <p>
 * To program a scan chain, one uses a {@link ChainControl#setInBits}
 * &nbsp;method to specify new values for the chain elements. Scan chain values
 * can be set using {@link BitVector}&nbsp;objects or strings. Strings are
 * appropriate for simple set commands, but the {@link BitVector}&nbsp;class
 * supports more sophisticated manipulations. As a starting point for
 * {@link BitVector}&nbsp;manipulations, one may wish to use the method
 * {@link ChainControl#getInBits}&nbsp;to retrieve the current value of the bit
 * sequence to scan in to the chips.
 * <p>
 * By convention, the first bit in a {@link BitVector}&nbsp;or character in a
 * string always represents the last bit scanned into or out of the chip. Thus
 * 1) the bit and character indices match the position of the corresponding scan
 * chain element along the s_in chain, 2) the strings match the left-to-right
 * order in which scan chain elements appear in most schematics, and 3) the
 * order is consistent with the order of scan chain nodes in the XML file.
 * <p>
 * Also by convention, the node path names used in this class start at the chip
 * node (e.g., "<tt>miniHeater.pScan.row</tt> "). I.e., they exclude the
 * system node.
 * <p>
 * The class also provides convenience methods like
 * {@link ChainControl#getChips}, which returns an array of the chips in the
 * system, and {@link ChainControl#getChainPaths}, which returns an array of
 * the chains in a single chip or in the entire system.
 * <p>
 * Here is an example of how to create a <code>ChainControl</code> object:
 * <BLOCKQUOTE><TT>JtagTester jtag = new Netscan4(JTAG_IP_ADDRESS,
 * JTAG_TAP_NUMBER); <BR>
 * ChainControl control = new ChainControl(XML_PATH, jtag, DEFAULT_VDD,
 * DEFAULT_TCK_KHZ); <BR>
 * </TT> </BLOCKQUOTE> The <TT>JTAG_TAP_NUMBER</TT> parameter must be omitted
 * when using the {@link Netscan}&nbsp;or {@link MockJtag}&nbsp constructors.
 * The user can then call {@link #setInBits}&nbsp;and {@link #shift}&nbsp;to
 * program scan chains.
 */

public class ChainControl extends Logger {

    /** The root of the scan chain hierarchy. */
    protected TestNode system;

    /** The JTAG tester object, if any. JtagTester is device-independent. */
    final JtagTester jtag;

    /**
     * Nominal frequency for JTAG TCK, in kHz. E.g., this is the value ChainTest
     * returns to after generating a Schmoo plot.
     */
    private int jtagKhz;

    /**
     * Nominal JTAG TAP voltage for chip power, in Volts. Also the default value
     * of Vdd. E.g., this is the value {@link ChainTest}&nbsp;returns to after
     * generating a Schmoo plot.
     */
    private float jtagVolts;

    /**
     * Default action when the bits shifted out of the IR register are bad.
     * Initial value is {@link Infrastructure#SEVERITY_FATAL}.
     */
    public int irBadSeverity = Infrastructure.SEVERITY_FATAL;

    /**
     * Default action when no consistency check is possible on bits shifted out
     * of the data register. Initial value is
     * {@link Infrastructure#SEVERITY_WARNING}.
     */
    public int noTestSeverity = Infrastructure.SEVERITY_WARNING;

    /**
     * Default action when the bits shifted out of the data register are
     * inconsistent with expectation, see {@link #getExpectedBits}. Initial
     * value is {@link Infrastructure#SEVERITY_FATAL}.
     */
    public int errTestSeverity = Infrastructure.SEVERITY_FATAL;

    /**
     * XML file read in
     */
    public String xmlFile = null;


    /**
     * Creates a new instance of ChainControl, with the scan chain hierarchy
     * specified in the XML file <code>fileName</code>. For more information
     * on the XML file, see <tt>ChainG.dtd</tt> and ``The Scan Chain XML File
     * Format'' by Tom O'Neill.
     * 
     * @param fileName
     *            Name of XML file containing scan chain description
     */
    ChainControl(String fileName) {
        openFile(fileName);
        jtag = null;
    }

    /**
     * Creates an object to program scan chains using the boundary scan
     * controller <code>jtagTester</code> and assuming the scan chain
     * hierarchy specified in the XML file <code>fileName</code>. For more
     * information on the XML file, see <tt>ChainG.dtd</tt> and <A
     * HREF="http://archivist/index.jsp?id=2004-1091"> "The Scan Chain XML File
     * Format" </A>.
     * <p>
     * Configures the provided JTAG tester to the TAP voltage
     * <code>jtagVolts</code>, frequency <code>jtagKhz</code>, and the
     * default stop state {@link NetscanGeneric#DEFAULT_STOP_STATE}. The
     * TRSTbar signal is also set <tt>LO</tt> briefly to reset the JTAG
     * controller for each chip on this JTAG tester.
     * <p>
     * The <code>jtagVolts</code> and <code>jtagKhz</code> parameters are
     * also used sometimes as the default Vdd and JTAG frequency values. E.g.,
     * after generating a Schmoo plot, {@link ChainTest}&nbsp;leaves Vdd at
     * <code>jtagVolts</code> and the JTAG tester at <code>jtagKhz</code>.
     * Nevertheless, this constructor does not itself modify Vdd.
     * 
     * @param fileName
     *            Name of XML file containing scan chain description
     * @param jtagTester
     *            JTAG tester object to use
     * @param jtagVolts
     *            Nominal JTAG TAP voltage and default value of Vdd, in Volts.
     * @param jtagKhz
     *            Nominal frequency for JTAG TCK, in kHz.
     */
    public ChainControl(String fileName, JtagTester jtagTester,
            float jtagVolts, int jtagKhz) {
        System.out.print("Reading xml file "+fileName+"...");
        System.out.flush();
        long ctime = System.currentTimeMillis();
        openFile(fileName);
        System.out.println("finished. Took "+Infrastructure.getElapsedTime(System.currentTimeMillis()-ctime));
        jtag = jtagTester;
        this.jtagVolts = jtagVolts;
        this.jtagKhz = jtagKhz;
        jtagTester.configure(jtagVolts, jtagKhz);
    }

    /**
     * Get the system node
     * @return the system node that contains all other nodes
     */
    public MyTreeNode getSystem() {
        return system;
    }

    /** Finalizer. Disconnects from JTAG tester if necessary */
    protected void finalize() throws Throwable {
        super.finalize();
        jtag.disconnect();
    }

    /**
     * Returns device-independent JTAG tester object
     * 
     * @return Device-independent JTAG tester object
     */
    public JtagTester getJtag() {
        return jtag;
    }

    /**
     * Returns nominal voltage for JTAG TAP and chip power, in Volts
     * 
     * @return Default JTAG TAP and chip V_DD, in Volts
     */
    public float getJtagVolts() {
        return jtagVolts;
    }

    /**
     * Configures the JTAG tester to use the requested TAP voltage. This voltage
     * is also used as the default value of the chip Vdd (e.g., the value set
     * after completion of a Schmoo plot), but the routine does not modify the
     * current Vdd setting.
     * 
     * @param defaultVdd
     *            JTAG tester TAP voltage/nominal chip power, in Volts
     */
    public void setJtagVolts(float defaultVdd) {
        this.jtagVolts = defaultVdd;
        jtag.configure(defaultVdd, jtagKhz);
    }

    /**
     * Returns nominal frequency for JTAG TCK, in kHz
     * 
     * @return Nominal frequency for JTAG TCK, in kHz
     */
    public int getJtagKhz() {
        return jtagKhz;
    }

    /**
     * Returns the xml file read by this chain control
     *
     * @return xml file read by this chain control
     */
    public String getXmlFile() {
        return xmlFile;
    }

    /**
     * Configures the JTAG tester to use the requested TCK frequency. This
     * frequency is also used as the default frequency (e.g., the value set
     * after completion of a Schmoo plot).
     * 
     * @param defaultKHz
     *            Nominal frequency for JTAG TCK, in kHz
     */
    public void setJtagKhz(int defaultKHz) {
        this.jtagKhz = defaultKHz;
        jtag.configure(jtagVolts, defaultKHz);
    }

    /**
     * Get the pin name for the selected <code>SubchainNode</code> object.
     * Currently the pin name is only used by {@link SamplerControl}.
     * 
     * @param path
     *            path name to the desired <code>SubchainNode</code> object
     * @return name of I/O pad associated with the selected subchain
     */
    public String getSubchainPin(String path) {
        SubchainNode chainNode = (SubchainNode) this.findNode(path,
                SubchainNode.class);
        return chainNode.pin;
    }

    /**
     * Set the pin name for the selected <code>SubchainNode</code> object.
     * Currently the pin name is only used by {@link SamplerControl}.
     * 
     * @param path
     *            path name to the desired <code>SubchainNode</code> object
     * @param pin
     *            name of I/O pad associated with the selected subchain
     */
    public void setSubchainPin(String path, String pin) {
        SubchainNode chainNode = (SubchainNode) this.findNode(path,
                SubchainNode.class);
        chainNode.pin = pin;
    }

    /**
     * Return the number of scan chain elements within a given (subchain) node
     * in the scan chain.
     * 
     * @param path
     *            path name to the desired node, starting at the chip node
     * @return number of scan chain elements in node and its descendents
     */
    public int getLength(String path) {
        SubchainNode chainNode = (SubchainNode) this.findNode(path,
                SubchainNode.class);
        return chainNode.getLength();
    }

    /**
     * Return a copy of the scan chain bit pattern that will be written to
     * specified node on chip during the next {@link ChainControl#shift}
     * &nbsp;call. The first element of the bit vector represents the scan chain
     * element that is scanned in last.
     * 
     * @param path
     *            path name to the desired node, starting at the chip node
     * @return Bit vector with pending bit pattern for current node
     */
    public BitVector getInBits(String path) {
        SubchainNode chainNode = (SubchainNode) this.findNode(path,
                SubchainNode.class);
        return chainNode.getInBits();
    }

    /**
     * Return a copy of the scan chain bit pattern that was written to specified
     * node on chip after the last {@link ChainControl#shift}&nbsp;call. The
     * first element of the bit vector represents the scan chain element that is
     * scanned out last.
     * 
     * @param path
     *            path name to the desired node, starting at the chip node
     * @return Bit vector with received bit pattern for current node
     */
    public BitVector getOutBits(String path) {
        SubchainNode chainNode = (SubchainNode) findNode(path,
                SubchainNode.class);
        return chainNode.getOutBits();
    }

    /**
     * Return a copy of the scan chain bit pattern that the library expected to
     * be shifted out of the specified node during the <em>previous</em> call
     * to {@link #shift}&nbsp;for the parent chain. This is useful for tracking
     * down problems when the scanned out bits don't equal the expectation. Bits
     * that are in the invalid state correspond to chain elements for which no
     * prediction was possible.
     * <p>
     * (Expert users: the method returns an archival copy of the state of
     * <code>outBitsExpected</code> during the previous consistency check.)
     * 
     * @param path
     *            path name to the desired node, starting at the chip node
     * @return Bit vector with expected bit pattern for current node
     */
    public BitVector getExpectedBits(String path) {
        SubchainNode chainNode = (SubchainNode) findNode(path,
                SubchainNode.class);
        return chainNode.getOldOutBitsExpected();
    }

    /**
     * Set scan chain bit pattern that will be written to specified node on chip
     * after the next shift() call. The first element of the bit vector
     * represents the scan chain element that is scanned in last.
     * 
     * @param path
     *            path name to the desired node, starting at the chip node
     * @param newBits
     *            Bit array containing new settings
     */
    public void setInBits(String path, BitVector newBits) {
        SubchainNode chainNode = (SubchainNode) this.findNode(path,
                SubchainNode.class);
        chainNode.setInBits(newBits);
    }

    /**
     * Set scan chain bit pattern that will be written to specified node on chip
     * after the next shift() call. The first character in the string represents
     * the scan chain element that is scanned in last.
     * 
     * @param path
     *            path name to the desired node, starting at the chip node
     * @param newBits
     *            Character string containing new settings (e.g., "111001")
     */
    public void setInBits(String path, String newBits) {
        SubchainNode chainNode = (SubchainNode) findNode(path,
                SubchainNode.class);
        chainNode.setInBits(newBits);
    }

    /**
     * Set scan chain bit pattern that will be written to specified node on chip
     * after the next shift() call. All elements receive value newValue.
     * 
     * @param chainPath
     *            path name to the desired node, starting at the chip node
     * @param newValue
     *            new bit value to set each scan chain element to
     */
    public void setInBits(String chainPath, boolean newValue) {
        int length = getLength(chainPath);
        BitVector bits = new BitVector(length, "setInBits()-bits");
        bits.set(0, length, newValue);
        setInBits(chainPath, bits);
    }

    /**
     * For each scan chain in the system (on every chip), set the bit pattern
     * that will be written during the next shift() call to zero, or to
     * the clears state if clearable.
     */
    public void resetInBits() {
        resetInBits(true);
    }

    /**
     * For each scan chain in the system (on every chip), set the bit pattern
     * that will be written during the next shift() call to zero.
     * @param useMasterClearState sets the bit to the clears state instead of
     * zero if clearable.
     */
    public void resetInBits(boolean useMasterClearState) {
        String[] roots = getChainPaths();
        for (int iroot = 0; iroot < roots.length; iroot++) {
            ChainNode chainRoot = (ChainNode) findNode(roots[iroot],
                    ChainNode.class);
            chainRoot.resetInBits(useMasterClearState);
        }
    }

    /**
     * Inform the test library about a change in the state of the master clear
     * signal on the specified chip. In particular, appropriately modifies the
     * <tt>outBitsExpected</tt> (cf. {@link #getExpectedBits}) bit for any
     * scan chain element that has a <tt>clears</tt> value of "<tt>H</tt>", "
     * <tt>L</tt>", or "<tt>?</tt>" in the scan chain XML file. For more
     * information on the <tt>clears</tt> parameter, see <A
     * HREF="http://archivist/index.jsp?id=2004-1091"> "The Scan Chain XML File
     * Format" </A>. Call this method on any change in the master clear state,
     * or you may get false complaints about data shifted out not equalling
     * expected results.
     * 
     * @param chipName
     *            chip name
     */
    public void processMasterClear(String chipName) {
        String[] roots = getChainPaths(chipName);
        for (int iroot = 0; iroot < roots.length; iroot++) {
            ChainNode root = (ChainNode) findNode(roots[iroot], ChainNode.class);
            root.processMasterClear();
        }
    }

    /**
     * Invalidate expected scan chain element values for every scan chain on the
     * chip. Call this after a period at a low voltage, or you may get false
     * complaints about data shifted out not equalling expected results.
     * 
     * @param chipName
     *            chip name
     */
    public void invalidate(String chipName) {
        String[] roots = getChainPaths(chipName);
        for (int iroot = 0; iroot < roots.length; iroot++) {
            ChainNode root = (ChainNode) findNode(roots[iroot], ChainNode.class);
            root.invalidate();
        }
    }

    /**
     * Shift <code>inBits</code> (cf. {@link #setInBits}) into a root scan
     * chain on the chip. Like {@link #shift(String, boolean, boolean)}, except
     * that the response to the possible error conditions are specified
     * explicitly using the Infrastructure.SEVERITY_* constants. The bits that
     * are shifted out (see {@link #getOutBits}) are compared with expectation
     * (see {@link #getExpectedBits}).
     * 
     * @param chainRoot
     *            path name to the root scan chain, starting at the chip node
     * @param readEnable
     *            true to enable reading from the scan chain latches.
     * @param writeEnable
     *            true to enable writing to the scan chain latches.
     * @param irBadSeverity
     *            action when bits scanned out of the instruction register are
     *            wrong
     * @param noTestSeverity
     *            action when no consistency check is possible
     * @param errTestSeverity
     *            action when consistency check on scan chain functioning fails
     * @return true if the bits scanned out equal their expected values
     * @see Infrastructure#SEVERITY_NOMESSAGE
     * @see Infrastructure#SEVERITY_WARNING
     * @see Infrastructure#SEVERITY_NONFATAL
     * @see Infrastructure#SEVERITY_FATAL
     */
    public boolean shift(String chainRoot, boolean readEnable,
            boolean writeEnable, int irBadSeverity, int noTestSeverity,
            int errTestSeverity) {
        MyTreeNode node = findNode(chainRoot);
        if (!ChainNode.class.isInstance(node)) {
            Infrastructure.fatal("Node '" + node + "' at path '" + chainRoot
                    + "' is of class " + node.getClass().getName()
                    + ", but shifts must be performed on members of class "
                    + ChainNode.class.getName());
        }
        boolean ret = ((ChainNode) node).shift(jtag, readEnable, writeEnable,
                irBadSeverity, noTestSeverity, errTestSeverity, this);
        return ret;
    }

    /**
     * Shift <code>inBits</code> (cf. {@link #setInBits}) into a root scan
     * chain on the chip. Like
     * {@link #shift(String, boolean, boolean, int, int, int)}, except the
     * response to the possible error conditions are specified the member
     * variables {@link #irBadSeverity},{@link #noTestSeverity}, and
     * {@link #errTestSeverity}. The bits that are shifted out (see
     * {@link #getOutBits}) are compared with expectation (see
     * {@link #getExpectedBits}).
     * 
     * @param chainRoot
     *            path name to the root scan chain, starting at the chip node
     * @param readEnable
     *            true to enable reading from the scan chain latches.
     * @param writeEnable
     *            true to enable writing to the scan chain latches.
     * @return true if the bits scanned out equal their expected values #see
     *         irBadSeverity #see noTestSeverity #see errTestSeverity
     */
    public boolean shift(String chainRoot, boolean readEnable,
            boolean writeEnable) {
        return shift(chainRoot, readEnable, writeEnable, irBadSeverity,
                noTestSeverity, errTestSeverity);
    }

    /**
     * Experts only: shift one bit of data from <code>inBits</code> (cf.
     * {@link #setInBits}) into a root scan chain on the chip. When this method
     * is used, the library does not attempt to keep track of the on-chip scan
     * chain states. Thus no consistency check is performed on the bit that is
     * shifted out, nor on the next shift.
     * 
     * @param chainRoot
     *            path name to the root scan chain, starting at the chip node
     * @param readEnable
     *            true to enable reading from the scan chain latches.
     * @param writeEnable
     *            true to enable writing to the scan chain latches.
     * @param irBadSeverity
     *            action when bits scanned out of the instruction register are
     *            wrong
     * @return the bit that got shifted out
     * @see Infrastructure#SEVERITY_NOMESSAGE
     * @see Infrastructure#SEVERITY_WARNING
     * @see Infrastructure#SEVERITY_NONFATAL
     * @see Infrastructure#SEVERITY_FATAL
     */
    public boolean shiftOneBit(String chainRoot, boolean readEnable,
            boolean writeEnable, int irBadSeverity) {
        MyTreeNode node = findNode(chainRoot);
        if (!ChainNode.class.isInstance(node)) {
            Infrastructure.fatal("Node '" + node + "' at path '" + chainRoot
                    + "' is of class " + node.getClass().getName()
                    + ", but shifts must be performed on members of class "
                    + ChainNode.class.getName());
        }
        return ((ChainNode) node).shiftOneBit(jtag, readEnable, writeEnable,
                irBadSeverity, this);
    }

    /**
     * Returns path strings to all of the chips in the system
     * 
     * @return path strings to the chips in the system
     */
    public String[] getChips() {
        ArrayList chips = new ArrayList();
        for (int i=0; i<system.getChildCount(); i++) {
            MyTreeNode child = system.getChildAt(i);
            if (child.getClass() == ChipNode.class) {
                chips.add(child.getPathString(1));
                continue;
            }
            if (child.getName().equals(XMLIO.SCAN_CHAIN_DATA_NETS))
                continue;
            // unknown node
            Infrastructure.nonfatal(child
                    + " is a child of the system node, but is not a chip");
        }
        String [] chips2 = new String[chips.size()];
        for (int i=0; i<chips2.length; i++) {
            chips2[i] = (String)chips.get(i);
        }
        return chips2;
    }

    /**
     * Returns path strings to all of the root scan chains for the specified
     * chip.
     * 
     * @param chipName
     *            name of the chip
     * @return path strings to the root scan chains of the chip.
     */
    public String[] getChainPaths(String chipName) {
        MyTreeNode chip = findNode(chipName, ChipNode.class);
        int nroot = chip.getChildCount();
        String[] roots = new String[nroot];

        for (int iroot = 0; iroot < nroot; iroot++) {
            MyTreeNode child = chip.getChildAt(iroot);
            if (!ChainNode.class.isInstance(child)) {
                Infrastructure.fatal(child +" (class="+child.getClass()+") is a child of chip " + chipName
                        + ", but is not a ChainNode");
            } else {
                roots[iroot] = child.getPathString(1);
            }
        }

        return roots;
    }

    /**
     * Returns path strings to all of the root scan chains in the system.
     * 
     * @return path strings to the root scan chains in the system.
     */
    public String[] getChainPaths() {
        int nrootTotal = 0;
        String[] chips = getChips();

        for (int ichip = 0; ichip < chips.length; ichip++) {
            nrootTotal += getChainPaths(chips[ichip]).length;
        }

        int irootTotal = 0;
        String[] rootsTotal = new String[nrootTotal];

        for (int ichip = 0; ichip < chips.length; ichip++) {
            String[] roots = getChainPaths(chips[ichip]);
            for (int iroot = 0; iroot < roots.length; iroot++) {
                rootsTotal[irootTotal] = roots[iroot];
                irootTotal++;
            }
        }

        return rootsTotal;
    }

    /**
     * Returns path strings to all of the nodes below the specified node in the
     * scan chain hierarchy.
     * 
     * @param path
     *            name of the node to find descendents of
     * @return path strings to the nodes below the specified node
     */
    public String[] getDescendents(String path) {
        MyTreeNode node = findNode(path);
        MyTreeNode[] nodes = node.getDescendents();

        // Generate array of path strings, excluding the "system" level
        String[] paths = new String[nodes.length];
        for (int ind = 0; ind < nodes.length; ind++) {
            paths[ind] = nodes[ind].getPathString(1);
        }
        return paths;
    }

    /**
     * Return the path to the scan chain that specified node is a sub-chain of
     * 
     * @param path
     *            path of sub-chain to find parent of
     * @return path to chain that the sub-chain is part of
     */
    public String getParentChain(String path) {
        MyTreeNode node = findNode(path, SubchainNode.class);
        SubchainNode subchain = (SubchainNode) node;
        ChainNode chain = subchain.getParentChain();
        return chain.getPathString();
    }

    /**
     * Return the length of a single chip's instruction register
     * 
     * @param chip
     *            name of chip
     * @return length of chip's instruction register
     */
    public int getLenIR(String chip) {
        ChipNode node = (ChipNode) findNode(chip, ChipNode.class);
        return node.getLengthIR();
    }

    /**
     * Return the sum of the instruction register lengths for all of the chips
     * in the test system.
     * 
     * @return sum of instruction register lengths for all chips
     */
    public int getLenIR() {
        int sum = 0;
        String[] chips = getChips();
        for (int ichip = 0; ichip < chips.length; ichip++) {
            sum += getLenIR(chips[ichip]);
        }
        return sum;
    }

    /**
     * Return the opcode of a root scan chain, with little endian bit ordering.
     * Bits 0-5 provide the address of the scan chain on the chip. Bits 6 and 7
     * are write and read enable, respectively, but are overridden in software.
     * 
     * @param chainRoot
     *            name of the root scan chain to query
     * @return opcode of the selected root scan chain
     */
    public String getOpcode(String chainRoot) {
        ChainNode node = (ChainNode) findNode(chainRoot, ChainNode.class);
        return node.getOpcode();
    }

    /*
     * ######################################################################
     * Here are some "power user" routines. If you need to use them, I haven't
     * done my job right above.
     * ######################################################################
     */

    /**
     * Find a node using the path relative to a specified root node.
     * 
     * @param path
     *            path name, starting below root
     * @param root
     *            root an ancestor to the node being looked up
     * @return node at path path relative to root node root
     */
    public MyTreeNode findNode(String path, MyTreeNode root) {
        if (path.equals("")) {
            return root;
        }
        MyTreeNode node = MyTreeNode.getNode(root, path);
        if (node == null) {
            Infrastructure.fatal("Can't find " + path
                    + ".  Hints: Paths start with a chip name. "
                    + " Hierarchy levels are separated with '.'.  "
                    + "See chip XML file for correct paths.");
        }
        return node;
    }

    /**
     * Find a node using its full path, excluding the name of the system node.
     * 
     * @param path
     *            path name, starting at the chip node
     * @return node at path path relative to root node root
     */
    public MyTreeNode findNode(String path) {
        return this.findNode(path, system);
    }

    /**
     * Find a node using its full path, excluding the name of the system node.
     * If the routine succeeds, the returned object may safely be cast to class
     * <code>expected</code>.
     * 
     * @param path
     *            path name, starting at the chip node
     * @param expected
     *            class of node that is expected
     * @return node at path path relative to root node root
     */
    public MyTreeNode findNode(String path, Class expected) {
        MyTreeNode node = findNode(path);
        if (!expected.isInstance(node)) {
            Infrastructure
                    .fatal("Node at path " + path + " is of class "
                            + node.getClass() + ", but was expecting class "
                            + expected);
        }
        return node;
    }

    /**
     * Read the scan chain XML file, building the internal representation of the
     * scan chain tree
     */
    void openFile(String name) {
        if (name == null) {
            return;
        }
        try {
            this.system = XMLIO.read(name);
            xmlFile = name;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Unit test */
    public static void main(String[] args) {
        ChainControl control = new ChainControl("heater.xml");
        MyTreeNode node = control.findNode("heater.pScan");
        System.out.println(node);
        MyTreeNode node2 = control.findNode("p0.column", node);
        System.out.println(node2);
        String[] chips = control.getChips();
        for (int ind = 0; ind < chips.length; ind++) {
            System.out.println(ind + ": " + chips[ind] + ", lengthIR="
                    + control.getLenIR(chips[ind]));
        }
        System.out.println("Total IR length = " + control.getLenIR());

        System.out.println("\nChains of heater:");
        String[] roots = control.getChainPaths();
        for (int ind = 0; ind < roots.length; ind++) {
            System.out.println(ind + ": " + roots[ind] + ", length="
                    + control.getLength(roots[ind]));
        }

        System.out.println("\nSubchains of heater:");
        String[] subchains = control.getDescendents("heater");
        for (int ind = 0; ind < subchains.length; ind++) {
            System.out.println(ind + ": " + subchains[ind]);
        }

        control.setSubchainPin("heater", "toad");

        // Test the "can't shift to a subchain" message
        control.shift("heater.pScan.p0", false, false);
    }

}
