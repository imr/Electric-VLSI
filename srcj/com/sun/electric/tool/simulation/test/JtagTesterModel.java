/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JtagTesterModel.java
 * Written by Jonathan Gainsley, Sun Microsystems.
 *
 * Copyright (c) 2007 Sun Microsystems and Static Free Software
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

import java.util.Random;

public class JtagTesterModel extends BypassJtagTester {

    private final String tck;
    private final String tms;
    private final String trstb;
    private final String tdi;
    private final String tdob;

    // IR instructions
    private static final String SHIFT_IR = "1100";
    private static final String SHIFT_DR = "100";
    private static final String CAPTURE_DR = "10";
    private static final String IDLE = "110";

    private static final boolean DEBUG = true;

    JtagTesterModel(SimulationModel nm, String tck, String tms, String trstb, String tdi, String tdob) {
        super(nm);
        this.tck = tck;
        this.tms = tms;
        this.trstb = trstb;
        this.tdi = tdi;
        this.tdob = tdob;
        configure((float)nm.getVdd(), 100000);   // 100MHz
    }

    public void reset() {
        if (model.isBypassScanning()) {
            // just leave the controller in reset mode
            model.setNodeState(trstb, 0);
            model.setNodeState(tck, 0);
            model.setNodeState(tms, 1);
            model.setNodeState(tdi, 0);
            model.waitNS(delay);
        } else {
            // set the controller in reset state
            model.setNodeState(trstb, 0);
            model.setNodeState(tck, 0);
            model.setNodeState(tms, 1);
            model.setNodeState(tdi, 0);
            model.waitNS(delay);
            // set the controller in idle state
            model.setNodeState(trstb, 1);
            model.setNodeState(tms, 0);
            cycle_tck(1);
        }
        if (DEBUG) System.out.println("Finished resetting JtagTester");
    }

    public void tms_reset() {
        reset();
    }

    void shift(ChainNode chain, boolean readEnable, boolean writeEnable, int irBadSeverity) {
        if (isBypassScanning()) {
            doBypassScanning(chain, readEnable, writeEnable);
            return;
        }

        // create the instruction
        String instruction = NetscanGeneric.getInstructionRegister(chain,
                readEnable, writeEnable);
        task_load_instruction(instruction);

        // Add an extra bit for any jtag controller in bypass mode
        MyTreeNode root = chain.getParentChip().getParent();

        int numPrebits = 0;
        int numPostbits = 0;
        boolean foundChain = false;
        for (int i=0; i<root.getChildCount(); i++) {
            MyTreeNode child = root.getChildAt(i);
            if (child instanceof ChipNode) {
                ChipNode chip = (ChipNode)child;
                if (chip == chain.getParentChip()) {
                    foundChain = true; continue;
                }
                if (foundChain)
                    numPostbits++;
                else
                    numPrebits++;
            }
        }

        // Construct, and optionally report, the bit sequence to write
        BitVector scanInBits = NetscanGeneric.padBitVector(chain.getInBits(), numPrebits, numPostbits);
        // get data to send.
        String inbits = scanInBits.getState();
        String outbits = task_scan_data(inbits);
        // remove extra bits from scanned out data
        BitVector scanOutBits = new BitVector(outbits.substring(numPrebits, outbits.length()-numPostbits), "outbits");
        // invert if specified to account for inverting output
        if (isScanOutInverted()) {
            scanOutBits.flip(0, scanOutBits.getNumBits());
        }

        chain.getOutBits().put(0, scanOutBits);
        if (writeEnable) {
            // check that data was written correctly
            BitVector bitsToCheck = new BitVector(chain.getInBits().getNumBits(), "bitsToCheck");
            bitsToCheck.set(0, chain.getInBits().getNumBits(), true);
            checkDataNets(chain, 0, bitsToCheck);
            checkDataNets(chain, 1, bitsToCheck);
        }

    }

    // ================================================================

    private void cycle_tck(int times) {
        for (int i=0; i<times; i++) {
            model.waitNS(delay);
            model.setNodeState(tck, 1);
            model.waitNS(delay);
            model.setNodeState(tck, 0);
        }
    }

    // Steer the tap controller to the specified state
    private void task_goto(String IR) {
        int [] arr = stringToIntArray(IR);
        for (int i=0; i<arr.length; i++) {
            model.setNodeState(tms, arr[i]);
            model.waitNS(1);
            cycle_tck(1);
        }
    }

    // Steers the tap controller to the specified state,
    // but retrieves the last bit of scanned out data
    private int task_goto_send_tdo(String IR) {
        int [] arr = stringToIntArray(IR);
        boolean sendLastBit = true;
        int out = -1;

        for (int i=0; i<arr.length; i++) {
            model.setNodeState(tms, arr[i]);
            if (sendLastBit) {
                sendLastBit = false;
                model.waitNS(delay+1);
                model.setNodeState(tck, 1);
                out = model.getNodeState(tdob);
                model.waitNS(delay);
                model.setNodeState(tck, 0);
            } else {
                model.waitNS(1);
                cycle_tck(1);
            }
        }
        return out;
    }

    // forces tap controller to idle state regardless of current state
    private void task_go_idle() {
        model.setNodeState(tms, 1);
        cycle_tck(5);
        model.setNodeState(tms, 0);
        cycle_tck(1);
    }

    void task_load_instruction(String opcode) {
        // Note that we scan in from the end of the string to the front of the string
        int [] arr = stringToIntArray(reverse(opcode));
        System.out.print("  Loading instruction "+opcode+".");
        System.out.flush();

        task_goto(SHIFT_IR);
        model.setNodeState(tdi, arr[0]);
        System.out.print(".");
        System.out.flush();
        for (int i=1; i<arr.length; i++) {
            cycle_tck(1);
            model.setNodeState(tdi, arr[i]);
            System.out.print(".");
            System.out.flush();
        }
        task_goto(IDLE);
        System.out.println("...done.");
    }

    // scan data in and scan data out. Note that data scanned in
    // starts with the end of the string, not the beginning, and
    // so some reversal is necessary
    String task_scan_data(String data) {
        task_goto(SHIFT_DR);
        // Note that we scan in from the end of the string to the front of the string
        data = reverse(data);
        int [] arr = stringToIntArray(data);
        StringBuffer buf = new StringBuffer();

        System.out.println("  Scanning in  (reversed): "+data);
        System.out.print  ("  Scanning out (reversed): ");
        int i;
        for (i=0; i<arr.length-1; i++) {
            model.setNodeState(tdi, arr[i]);
            model.waitNS(0.5*delay);
            model.setNodeState(tck, 1);
            model.waitNS(0.5*delay);
            int n = model.getNodeState(tdob);

            String s = String.valueOf(n);
            if (n < 0) s = "X";
            buf.append(s);
            System.out.print(s);
            System.out.flush();

            model.waitNS(0.5*delay);
            model.setNodeState(tck, 0);
            model.waitNS(0.5*delay);
        }
        // scan in last bit
        model.setNodeState(tdi, arr[arr.length-1]);
        model.waitNS(delay);
        int ret = task_goto_send_tdo(IDLE);
        String s = String.valueOf(ret);
        if (ret < 0) s = "X";
        buf.append(s);
        System.out.println(s+"...done");
        System.out.flush();

        String outbits = reverse(buf.toString());
        outbits = outbits.replace('X', '0');
        return outbits;
    }

    // ===================================================================

    private static String reverse(String str) {
        StringBuffer buf = new StringBuffer();
        for (int i=str.length()-1; i>=0; i--) {
            buf.append(str.charAt(i));
        }
        //System.out.println("Reversed: "+str+" --> "+buf.toString());
        return buf.toString();
    }

    private static int [] stringToIntArray(String str) {
        int [] arr = new int[str.length()];
        for (int i=0; i<str.length(); i++) {
            if (str.charAt(i) == '1')
                arr[i] = 1;
            else if (str.charAt(i) == '0')
                arr[i] = 0;
            else {
                System.out.println("Warning: Unknown char in string, setting to 0: "+str.charAt(i));
                arr[i] = 0;
            }
        }
        return arr;
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
