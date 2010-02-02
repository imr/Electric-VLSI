/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JtagSubchainTesterModel.java
 * Written by Jonathan Gainsley, Sun Microsystems.
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
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

public class JtagSubchainTesterModel extends BypassJtagTester {

    private final String phi2;
    private final String phi1;
    private final String write;
    private final String read;
    private final String sin;
    private final String sout;

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
    JtagSubchainTesterModel(SimulationModel vm, String jtagInBus, String jtagOutBus) {
        super(vm);
        int i = jtagInBus.indexOf('[');
        if (i != -1) jtagInBus = jtagInBus.substring(0, i);
        phi2 = jtagInBus + "[2]";
        phi1 = jtagInBus + "[3]";
        write = jtagInBus + "[4]";
        read = jtagInBus + "[5]";
        sin = jtagInBus + "[1]";
        if (jtagOutBus != null && !jtagOutBus.equals("")) {
            i = jtagOutBus.indexOf('[');
            if (i != -1) jtagOutBus = jtagOutBus.substring(0, i);
            sout = jtagOutBus + "[1]";
        } else {
            sout = jtagInBus + "[8]";
        }
        configure((float)vm.getVdd(), 100000);   // 100MHz
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
    JtagSubchainTesterModel(SimulationModel vm, String phi2, String phi1, String write, String read, String sin, String sout) {
        super(vm);
        this.phi2 = phi2;
        this.phi1 = phi1;
        this.write = write;
        this.read = read;
        this.sin = sin;
        this.sout = sout;
        configure((float)vm.getVdd(), 100000);   // 100MHz
    }


    public void reset() {
        model.setNodeState(phi2, 1);
        model.setNodeState(phi1, 0);
        model.setNodeState(write, 0);
        model.setNodeState(read, 0);
        model.setNodeState(sin, 0);
    }

    public void tms_reset() {
       reset();
    }

    void shift(ChainNode chain, boolean readEnable, boolean writeEnable, int irBadSeverity) {
        if (isBypassScanning()) {
            doBypassScanning(chain, readEnable, writeEnable);
            return;
        }

        if (readEnable) {
            model.setNodeState(read, 1);
            model.waitNS(delay*4);             // assert read for 2 clock cycles
            model.setNodeState(read, 0);
            model.waitNS(delay*2);             // deassert for 1 clock cycle
        }

        BitVector in = chain.getInBits();
        BitVector out = new BitVector(in.getNumBits(), "scannedOut");
        for (int i=in.getNumBits()-1; i>=0; i--) {
            // get output bit
            int state = model.getNodeState(sout);
            if (state != 1 && state != 0) {
                System.out.println("Invalid state "+state+" scanned out, setting it to zero");
                state = 0;
            }
            out.set(i, (state==0 ? false : true));
            // set input, scan it in
            state = in.get(i) ? 1 : 0;
            model.setNodeState(sin, state);
            cycleClks(1);
        }
        chain.getOutBits().put(0, out);

        // write bits
        if (writeEnable) {
            model.setNodeState(write, 1);
            model.waitNS(delay*4);             // assert write for 2 clock cycles
            model.setNodeState(write, 0);
            model.waitNS(delay*2);             // deassert for 1 clock cycle

            BitVector bitsToCheck = new BitVector(chain.getInBits().getNumBits(), "bitsToCheck");
            bitsToCheck.set(0, chain.getInBits().getNumBits(), true);
            checkDataNets(chain, 0, bitsToCheck);
            checkDataNets(chain, 1, bitsToCheck);
        }
    }

    // --------------------------------------------------------------------------

    /**
     * Cycle phi2, phi1. Note this waits at the beginning of the method before
     * setting phi2 low (phi2 is high normally) to allow any change on scan in data
     * to propogate.
     * @param times the number of times to cycle phi2,phi1.
     */
    private void cycleClks(int times) {
        for (int i=0; i<times; i++) {
            model.waitNS(delay*0.45);
            model.setNodeState(phi2, 0);
            model.waitNS(delay*0.05);          // non-overlaping by 5% of half-freq
            model.setNodeState(phi1, 1);
            model.waitNS(delay*0.95);
            model.setNodeState(phi1, 0);
            model.waitNS(delay*0.05);          // non-overlaping by 5% of half-freq
            model.setNodeState(phi2, 1);
            model.waitNS(delay*0.50);
        }
    }

}
