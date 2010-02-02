/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VerilogJtagSubchainTester.java
 * Written by Jonathan Gainsley, Sun Microsystems.
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
