package com.sun.electric.tool.simulation.test;

import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Jun 30, 2005
 * Time: 2:21:04 PM
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 *
 */
public class NanosimJtagTester extends JtagTesterModel {

    NanosimJtagTester(NanosimModel nm, String tck, String tms, String trstb, String tdi, String tdob) {
        super(nm, tck, tms, trstb, tdi, tdob);
    }

    /** Unit Test
     * This test requires the file loco_core.hsp in your working dir
     *  */
    public static void main(String args[]) {
        NanosimModel nm = new NanosimModel();
        JtagTesterModel tester = (JtagTesterModel)nm.createJtagTester("TCK", "TMS", "TRSTb", "TDI", "TDOb");
        nm.start("nanosim", "loco_core.hsp", 0);

        // test private methods separately
        tester.task_load_instruction("11000101");
        tester.task_scan_data("1000100010001111");

        //tester.task_go_idle();

        tester.task_load_instruction("11001100");
        tester.task_scan_data("100010001000111101011100011");
        tester.task_scan_data("100010001000111101011100011");
        tester.task_scan_data("100010001000111101011100011");
        tester.task_scan_data("100010001000111101011100011");

        // test public methods which use private methods
        if (true) {
            ChipNode cn = new ChipNode("test", 8, "none");
            ChainNode testNode = new ChainNode("testNode", "1001", 156, "node for unit test");
            cn.addChild(testNode);
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
        }
        tester.model.finish();
    }

}
