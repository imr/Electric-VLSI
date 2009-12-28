package com.sun.electric.tool.simulation.test;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Sep 16, 2005
 * Time: 12:45:49 PM
 *
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 *
 * Control a section of a scan chain
 */
public class NanosimJtagSubchainTester extends JtagSubchainTesterModel {

    /**
     * Create a subchain tester based on the 8- or 9-wire jtag interface.
     * jtag[8:0] = {scan_data_return, phi2_return, phi1_return, rd, wr, phi1, phi2, sin, mc*}
     * Note that mc is not present on older designs, so they are jtag[8:1].
     * If jtagOutBus is null or "", it assumes the chain has been capped off with
     * an endcap, and scanout is actually jtagIn[8].
     * @param nm the simulation model
     * @param jtagInBus the name of the 9-bit wide input bus, i.e. "jtagIn" or "jtagIn[8:0]"
     * @param jtagOutBus the name of the 9-bit wide output bus, i.e. "jtagOut" or "jtagOut[8:0]".
     * This may be null if the chain ends in an endCap.
     */
    NanosimJtagSubchainTester(NanosimModel nm, String jtagInBus, String jtagOutBus) {
        super(nm, jtagInBus, jtagOutBus);
    }

    /**
     * Create a subchain tester based on the 5-wire jtag interface.
     * @param nm the simulation model
     * @param phi2 name of the phi2 signal
     * @param phi1 name of the phi1 signal
     * @param write name of the write signal
     * @param read name of the read signal
     * @param sin name of the scan data in signal
     * @param sout name of the scan data out signal
     */
    NanosimJtagSubchainTester(NanosimModel nm, String phi2, String phi1, String write, String read, String sin, String sout) {
        super(nm, phi2, phi1, write, read, sin, sout);
    }
}
