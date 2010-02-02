/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NanosimJtag.java
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

import java.util.List;
import java.util.ArrayList;

/**
 * Parent class for {@link NanosimJtagTester} and {@link NanosimJtagSubchainTester}.
 * Contains common code shared between the child classes.
 */
public abstract class NanosimJtag extends JtagTester {

    protected final NanosimModel nm;
    protected float tapVolt;      // ignored at this time
    protected double delay;       // delay in ns

    private static final boolean DEBUG = false;

    NanosimJtag(NanosimModel nm) {
        this.nm = nm;
    }

    void configure(float tapVolt, long kiloHerz) {
        this.tapVolt = tapVolt;
        this.delay = (1.0/kiloHerz * 1e6) / 2;
    }

    void disconnect() {}

    void setLogicOutput(int index, boolean newLevel) {
        System.out.println("Nanosim JtagTester does not support 'setLogicOutput("+index+", "+newLevel+"). Use LogicSettable instead.");
    }

    public boolean isBypassScanning() {
        return nm.isBypassScanning();
    }

    // ==========================================================

    protected void doBypassScanning(ChainNode chain, boolean readEnable, boolean writeEnable) {
        // this mode bypasses scanning, and reads/writes directly
        // to the bits the scan chain controls, as long as those
        // nets have been defined in the XML file.
        if (DEBUG) System.out.println("Scanning in "+chain.getName()+" in bypass-scanning mode");

        // initialize out bits last data scanned in
        // this prevents problems with uninitialized bit vectors if the user does not read
        if (chain.getOutBitsExpected().isInvalid())
            chain.getOutBits().set(0, chain.getOutBitsExpected().getNumBits(), false);
        else
            chain.getOutBits().put(0, chain.getOutBitsExpected());

        if (readEnable) {
            chain.getOutBits().put(0, readDirect(chain));
        }
        if (writeEnable) {
            writeDirect(chain);
            checkDataNets(chain, 0);
            checkDataNets(chain, 1);
        }
    }

    /**
     * Get a list of DataNets from the chain.
     * @param chain the chain to read
     * @param set which set to get. Currently only two supported, so only 0 or 1
     * @return a list of DataNets of the (hierarchical) data out net names.
     * May contain null for undefined nets
     */
    protected static List getDataNets(SubchainNode chain, int set) {
        // get the scan chain data nets node corresponding to the chain
        MyTreeNode system = chain.getParent().getParent();    // this is the system node
        MyTreeNode scanchainnets = MyTreeNode.getNode(system, XMLIO.SCAN_CHAIN_DATA_NETS);
        if (scanchainnets == null) return getDataNetsOld(chain, set);       // must be using old version

        SubchainNode datachain = (SubchainNode)MyTreeNode.getNode(scanchainnets, chain.getName());
        if (datachain == null) {
            // if more than one jtag contoller, datanet chain name is prepended with chip name
            MyTreeNode chip = chain.getParent();
            datachain = (SubchainNode)MyTreeNode.getNode(scanchainnets, chip.getName()+"_"+chain.getName());
        }
        if (datachain == null) return getDataNetsOld(chain, set);
        
        List datanets = new ArrayList();
        for (int i=0; i<datachain.getChildCount(); i++) {
            SubchainNode subnode = (SubchainNode)datachain.getChildAt(i);
            if (set == 0) {
                datanets.add(subnode.getDataNet());
            } else {
                datanets.add(subnode.getDataNet2());
            }
        }
        return datanets;
    }

    /**
     * Get a list of DataNets from the chain.
     * @param chain the chain to read
     * @param set which set to get. Currently only two supported, so only 0 or 1
     * @return a list of DataNets of the (hierarchical) data out net names.
     * May contain null for undefined nets
     * @deprecated this was used when the xml file contains data nets specified hierarchically
     * along with the scan chain bits. I have since split the data nets out into a separate,
     * flat listing.
     */
    protected static List getDataNetsOld(SubchainNode chain, int set) {
        // get names of data out bits
        if (chain.getChildCount() == 0) {
            List list = new ArrayList();
            SubchainNode.DataNet dataNet;
            if (set == 0) dataNet = chain.getDataNet();
            else dataNet = chain.getDataNet2();

            if (dataNet == null || dataNet.getName().equals("")) {
                // pad list with correct number of null entries
                for (int i=0; i<chain.getLength(); i++)
                    list.add(null);
                return list;
            }

            MyTreeNode [] hier = chain.getParent().getHierarchy();
            StringBuffer newPath = new StringBuffer();
            for (int j=0; j<hier.length; j++) {
                // remove the chip name and the chain name (3), as they are
                // not part of the spice hierarchy.
                if (j<3) continue;
                newPath.append("X"+hier[j].getName()+".");
            }
            //newPath.append("X"+chain.getName()+".");

            // special case: if Fake Chain, do not use the name in the
            // hierarchical path to the dataNet. This is because schematics
            // that do not have scanChainElements must refer to data nets
            // in the schematic, which are at a level above where a scanChain.dataNet
            // net would be
            boolean fakeChain = false;
            if (chain.getParentChain().getOpcode().equals("fakeChain")) {
                // this is a fake chain
                fakeChain = true;
            }

            Name netName = Name.findName(dataNet.getName());
            if (fakeChain) {
                for (int j=0; j<netName.busWidth(); j++) {
                    String net = netName.subname(j).toString();
                    // create hierarchical spice net name
                    SubchainNode.DataNet singleNet = new SubchainNode.DataNet(newPath.toString() + net,
                            dataNet.isReadable(), dataNet.isWriteable(), dataNet.isInverted());
                    list.add(singleNet);
                    //System.out.println("DataNet added: "+singleNet);
                }
            } else {
                // chain may be bussed, bussed name should match length of chain
                Name dataName = Name.findName(chain.getName());
                for (int i=0; i<dataName.busWidth(); i++) {
                    netName = Name.findName(dataNet.getName());
                    for (int j=0; j<netName.busWidth(); j++) {
                        String net = "x" + dataName.subname(i).toString() + "." + netName.subname(j).toString();
                        // create hierarchical spice net name
                        SubchainNode.DataNet singleNet = new SubchainNode.DataNet(newPath.toString() + net,
                                dataNet.isReadable(), dataNet.isWriteable(), dataNet.isInverted());
                        list.add(singleNet);
                        //System.out.println("DataNet added: "+singleNet);
                    }
                }
            }

            if (list.size() != chain.getLength()) {
                System.out.println("Error: data net list of size "+list.size()+" does not match length of chain "+chain.getName()+" of length "+chain.getLength());
                list.clear();
                for (int i=0; i<chain.getLength(); i++)
                    list.add(null);
                return list;
            }
            return list;
        } else {
            // this just contains more sub nodes
            List list = new ArrayList();
            for (int i=0; i<chain.getChildCount(); i++) {
                SubchainNode subnode = (SubchainNode)chain.getChildAt(i);
                list.addAll(getDataNets(subnode, set));
            }
            return list;
        }
    }


    /**
     * Check that the bits in the chain have been applied to the dataNets for the
     * scan chain. This only applies for scan bits that have their "dataNet" or
     * "dataNet2" property in the XML file.
     * @param chain the scan chain
     * @param set which set of nets to check.  Only 0 and 1 currently.
     * @return true if a discrepancy found, false otherwise.
     */
    protected boolean checkDataNets(ChainNode chain, int set) {
        boolean foundDiscrepancy = false;
        List dataNets = getDataNets(chain, set);

        for (int i=0; i<dataNets.size(); i++) {
            SubchainNode.DataNet dataNet = (SubchainNode.DataNet)dataNets.get(i);
            //System.out.println(i+":\t"+netName);
            if (dataNet == null) continue;        // undefined net
            if (dataNet.isWriteable()) {
                // check that data in inbits was written
                int simState = nm.getNodeState(dataNet.getName());
                int setState = chain.getInBits().get(i) ? 1 : 0;
                if (dataNet.isInverted())
                    setState = (setState==1 ? 0 : 1);
                if (simState != setState) {
                    System.out.println("Error! Attempted to set bit '"+dataNet.getName()+"' to "+setState+" via the scan chain, but its state is "+simState);
                    foundDiscrepancy = true;
                } else {
                    if (DEBUG) {
                        System.out.println("Checked "+dataNet.getName()+" read "+simState+": ok");
                    }
                }
            }
        }
        return foundDiscrepancy;
    }

    /**
     * Read directly from the data bits the scan chain controls, rather than
     * applying "read" and scanning out the data. This is much faster than
     * scanning them out.
     * @param chain the scan chain
     * @return a BitVector of the bits read directly out
     */
    protected BitVector readDirect(ChainNode chain) {
        List dataNets = getDataNets(chain, 0);
        List dataNets2 = getDataNets(chain, 1);

        BitVector outBits = new BitVector(chain.getOutBits().getNumBits(), "outBits");
        int bitsRead = 0;
        for (int i=0; i<outBits.getNumBits(); i++) {
            SubchainNode.DataNet dataNet = (SubchainNode.DataNet)dataNets.get(i);
            SubchainNode.DataNet dataNet2 = (SubchainNode.DataNet)dataNets2.get(i);

            int state = readDirect(dataNet);
            int state2 = readDirect(dataNet2);

            if (state >=0 && state2 >= 0) {
                // both were read and are valid, check that they match
                if (state != state2) {
                    System.out.println("Error! Inconsistency reading directly from scan chain data bit "+i+
                            " of chain '"+chain.getName()+"', "+dataNet.getName()+" is "+state+" and "+
                            dataNet2.getName()+" is "+state2);
                }
            }
            if (state < 0) {
                state = state2;                 // state holds any valid state
            }
            if (state < 0) {
                // nothing valid read, set it to whatever was shifted in
                state = chain.getInBits().get(i) ? 1 : 0;
                if (state == -2) bitsRead++;        // bit was read, just in undefined state
            } else {
                bitsRead++;
            }
            outBits.set(i, state==1 ? true : false);
        }
        if (printInfo) 
        	System.out.println("Info: Read directly "+bitsRead+" bits from chain '"+chain.getName()+
        					   "' of length "+chain.getOutBits().getNumBits()+
                               " bits (others unchanged).");
        return outBits;
    }

    /**
     * Read directly from a scan chain data output
     * @param dataNet the data net to read from
     * @return the value read. 0 or 1 valid values, -1 on error
     */
    private int readDirect(SubchainNode.DataNet dataNet) {
        if (dataNet == null) return -1;
        if (!dataNet.isReadable()) return -1;

        int state = nm.getNodeState(dataNet.getName());
        if (DEBUG) System.out.println("Read directly "+(dataNet.isInverted()?"(inverted)":"")+" from net "+dataNet.getName()+": "+state);
        if (state == -2) System.out.println("Warning, read intermediate (undefined) voltage state from net "+dataNet.getName());
        if (state < 0) return state;
        if (dataNet.isInverted()) state = state==1 ? 0 : 1;
        return state;
    }

    /**
     * Write scan chain data directly to the data bits, rather than
     * scanning them in and then applying "write".  This should be much
     * faster than scanning them in.
     * @param chain the scan chain
     */
    protected void writeDirect(ChainNode chain) {
        List dataNets = getDataNets(chain, 0);
        List dataNets2 = getDataNets(chain, 1);

        int dataNetsWritten = 0;
        int dataNet2sWritten = 0;
        for (int i=0; i<chain.getInBits().getNumBits(); i++) {
            int state = chain.getInBits().get(i) ? 1 : 0;
            SubchainNode.DataNet dataNet = (SubchainNode.DataNet)dataNets.get(i);
            SubchainNode.DataNet dataNet2 = (SubchainNode.DataNet)dataNets2.get(i);
            if (writeDirect(dataNet, state)) dataNetsWritten++;
            if (writeDirect(dataNet2, state)) dataNet2sWritten++;
        }
        // apply write (above writes)
        nm.waitNS(delay*4);
        // release writes, unless it is a fake chain control
        if (!chain.getOpcode().equals("fakeChain")) {
            nm.releaseNodes(getNames(dataNets));
            nm.releaseNodes(getNames(dataNets2));
        }
        if (printInfo) 
        	System.out.println("Info: Wrote directly "+dataNetsWritten+" bits and "+
        			dataNet2sWritten+" secondary bits from scan chain '"+chain.getName()+ "' of length "+
        			chain.getInBits().getNumBits()+" bits.");
    }

    /**
     * Write directly to a scan chain data output
     * @param dataNet the data net to write to
     * @param state the state to write (0 or 1)
     * @return true if written, false if not
     */
    private boolean writeDirect(SubchainNode.DataNet dataNet, int state) {
        if (dataNet == null) return false;
        if (!dataNet.isWriteable()) return false;
        int setState = state;
        if (dataNet.isInverted()) state = (state==1) ? 0 : 1;
        nm.setNodeState(dataNet.getName(), setState);
        if (DEBUG) System.out.println("Wrote directly "+state+" to net "+dataNet.getName());
        return true;
    }

    private static List getNames(List dataNets) {
        List names = new ArrayList();
        for (int i=0; i<dataNets.size(); i++) {
            SubchainNode.DataNet dataNet = (SubchainNode.DataNet)dataNets.get(i);
            if (dataNet == null) continue;
            names.add(dataNet.getName());
        }
        return names;
    }
}
