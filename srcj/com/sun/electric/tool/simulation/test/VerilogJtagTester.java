/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VerilogJtagTester.java
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

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Iterator;

/**
 * A JtagTester that interfaces with a verilog model of the Device Under Test.
 */
public class VerilogJtagTester extends JtagTesterModel {

    /**
     * Create a new Verilog JtagTester.  This implements a JtagTester, but does so
     * as an interface to a Verilog simulation of the chip under test.  This should only
     * be called by VerilogModel.
     * @param vm the verilog model that the tester will interface with
     * @param tck the name of the tck port
     * @param tms the name of the tms port
     * @param trstb the name of the trstb port
     * @param tdi the name of the tdi port
     * @param tdob the name of the tdob port
     */
    VerilogJtagTester(VerilogModel vm, String tck, String tms, String trstb, String tdi, String tdob) {
        super(vm, tck, tms, trstb, tdi, tdob);
    }

    String formatDataNetName(String dataNetName) {
        return VerilogModel.formatDataNetName(dataNetName);
    }


    /** Unit test */
    public static void main(String args[]) {
        VerilogModel vm = new VerilogModel();
        VerilogJtagTester tester = (VerilogJtagTester)vm.createJtagTester("TCK", "TMS", "TRSTb", "TDI", "TDOb");
        vm.start("verilog", VerilogModel.getExampleVerilogChipFile(), VerilogModel.NORECORD);

        // test private methods separately
        tester.reset();
        tester.task_load_instruction("11000010");
        tester.task_scan_data("1000100010001111");

        // test public methods which use private methods
        ChainNode testNode = new ChainNode("testNode", "1001", 156, "node for unit test");
        Random rand = new Random(309402934);
        for (int i=0; i<testNode.getInBits().getNumBits(); i++) {
            testNode.getInBits().set(i, rand.nextBoolean());
        }
        System.out.println("Note that data shifted out is inverted sense of data shifted in,");
        System.out.println("  unless it goes through one of our inverting output pads first.");
        System.out.println("Unit Test: Shifting in: "+testNode.getInBits().getState());
        tester.shift(testNode, true, true, 0);
        System.out.println("Unit Test: Shifted out: "+testNode.getOutBits().getState());

        // We sent to a chain that had no elements, so scan in == scan out.
        boolean match = true;
        for (int i=0; i<testNode.getInBits().getNumBits(); i++) {
            if (testNode.getInBits().get(i) != !testNode.getOutBits().get(i)) {
                match = false;
                break;
            }
        }
        if (!match) {
            System.out.println("Unit Test Error: scan data in should match scan data out when chain is looped back.");
        } else {
            System.out.println("Unit Test OK.");
        }
        vm.finish();
    }
}
