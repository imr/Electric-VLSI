/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ChainControlFake.java
 * Written by Jonathan Gainsley, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

import org.w3c.dom.Element;

/**
 * Create a fake Chain Control when no XML file is available.
 */
public class ChainControlFake extends ChainControl {

    private ChipNode chipNode;

    public ChainControlFake(String chipName, int lengthIR, JtagTester jtagTester,
                            float jtagVolts, int jtagKhz) {
        super(null, jtagTester, jtagVolts, jtagKhz);
        system = new TestNode("System", "System Top Level");
        chipNode = new ChipNode(chipName, lengthIR, "Chip Top Level");
        system.addChild(chipNode);
    }

    /**
     * Add 1 or more scan chain bits to the specified chain. If the chain does not
     * exists, it is created, and the specified bits are at the beginning of the chain.
     * Subsequent calls using the same chain name will append the added bits to the end
     * of the chain.
     * @param chain the name of the chain
     * @param scanElementName the name of the scan chain element
     * @param length the number of bits in the element (it could by an arrayed instance)
     * @param access the access of the element (see configureXML.bsh)
     * @param clears the clears of the element (see configureXML.bsh)
     * @param dataNet the name of the data network that the scan chain writes to, or null if none.
     * Note that dataNet can no longer be an internal net to the scan chain element, so
     * the scanElementName is not used in the hierarchical path to the spice net.
     * @param dataNet2 the name of the data-bar network that the scan chain writes to, or null if none
     * Note that dataNet can no longer be an internal net to the scan chain element, so
     * the scanElementName is not used in the hierarchical path to the spice net.
     */
    public void addScanBits(String chain, String scanElementName, int length, String access,
                            String clears, String dataNet, String dataNet2) {
        TestNode chainNode = getChainNode(chain);
        if (chainNode ==  null) {
            // create new chain
            chainNode = createTreeNodeChain(chain, "fakeChain", 0);     // don't need opcode for FakeChainControl
            if (chainNode == null) return;
            chipNode.addChild(chainNode);
        }
        Element dummyXML = new javax.imageio.metadata.IIOMetadataNode("subchain");
        dummyXML.setAttribute("name", scanElementName);
        dummyXML.setAttribute("length", String.valueOf(length));
        dummyXML.setAttribute("access", access);
        dummyXML.setAttribute("clears", clears);
        dummyXML.setAttribute("dataNet", dataNet);
        dummyXML.setAttribute("dataNet2", dataNet2);
        TestNode subchainNode;
        try {
            subchainNode = XMLIO.createTreeNode(dummyXML, chainNode);
        } catch (Exception e) {
            System.out.println("Exception trying to create ChainControlFake");
            e.printStackTrace(System.out);
            return;
        }
        if (subchainNode != null)
            chainNode.addChild(subchainNode);
    }

    public boolean shift(String chainRoot, boolean readEnable,
            boolean writeEnable, int irBadSeverity, int noTestSeverity,
            int errTestSeverity) {
        if (!(jtag instanceof BypassJtagTester)) {
            System.out.println("Error! JtagTester must be a NanosimJtagTester or NanosimJtagSubchainTester to use a Fake ChainControl.");
            return false;
        }
        if (!((BypassJtagTester)jtag).isBypassScanning()) {
            System.out.println("Error! ChainControlFake can only be used in bypass scanning (direct read/write) mode. See NanosimModel.start()");
            return false;
        }
        return super.shift(chainRoot, readEnable, writeEnable, irBadSeverity, noTestSeverity, errTestSeverity);
    }

    // ==================================================================================
    //                   Create Fake Scan Chain Hierarchy

    private TestNode createTreeNodeChain(String name, String opcode, int length) {
        Element dummyXML = new javax.imageio.metadata.IIOMetadataNode("chain");
        dummyXML.setAttribute("name", name);
        dummyXML.setAttribute("opcode", opcode);
        dummyXML.setAttribute("length", String.valueOf(length));
        TestNode chainNode;
        try {
            chainNode = XMLIO.createTreeNode(dummyXML, chipNode);
        } catch (Exception e) {
            System.out.println("Exception trying to create ChainControlFake");
            e.printStackTrace(System.out);
            return null;
        }
        return chainNode;
    }

    /**
     * Get the chainNode by name from the chipNode
     * @param chainName the name of the chainNode
     * @return the chainNode, or null if none found
     */
    private TestNode getChainNode(String chainName) {
        for (int i=0; i<chipNode.getChildCount(); i++) {
            TestNode chainNode = (TestNode)chipNode.getChildAt(i);
            if (chainNode.getName().equals(chainName))
                return chainNode;
        }
        return null;
    }

}
