package com.sun.electric.tool.simulation.test;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: May 12, 2008
 * Time: 2:22:35 PM
 * To change this template use File | Settings | File Templates.
 *
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 *
 * Control a section of a scan chain
 */
public class VerilogJtagSubchainTester extends JtagSubchainTesterModel {

    /**
     * Create a subchain tester based on the 8- or 9-wire jtag interface.
     * jtag[8:0] = {scan_data_return, phi2_return, phi1_return, rd, wr, phi1, phi2, sin, mc*}
     * Note that mc is not present on older designs, so they are jtag[8:1].
     * If jtagOutBus is null or "", it assumes the chain has been capped off with
     * an endcap, and scanout is actually jtagIn[8].
     * @param vm the Verilog simulation model
     * @param jtagInBus the name of the 9-bit wide input bus, i.e. "jtagIn" or "jtagIn[8:0]"
     * @param jtagOutBus the name of the 9-bit wide output bus, i.e. "jtagOut" or "jtagOut[8:0]".
     * This may be null if the chain ends in an endCap.
     */
    VerilogJtagSubchainTester(VerilogModel vm, String jtagInBus, String jtagOutBus) {
        super(vm, jtagInBus, jtagOutBus);
    }

    /**
     * Create a subchain tester based on the 5-wire jtag interface.
     * @param vm the Verilog simulation model
     * @param phi2 name of the phi2 signal
     * @param phi1 name of the phi1 signal
     * @param write name of the write signal
     * @param read name of the read signal
     * @param sin name of the scan data in signal
     * @param sout name of the scan data out signal
     */
    VerilogJtagSubchainTester(VerilogModel vm, String phi2, String phi1, String write, String read, String sin, String sout) {
        super(vm, phi2, phi1, write, read, sin, sout);
    }

    String formatDataNetName(String dataNetName) {
        return VerilogModel.formatDataNetName(dataNetName);
    }
}
