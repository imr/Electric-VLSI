/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VerilogModel.java
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

import java.io.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.net.URL;

/**
 * Create a Verilog Model that simulates the behavior of a Device Under Test.
 * This model replaces the actual chip under test by providing 'test devices'
 * that replace actual hardware test measurement devices, such as the jtag tester.
 * <P>
 * Example:
 * <code><pre>
 * // create a new VerilogModel
 * VerilogModel vm = new VerilogModel();
 *
 * // create a jtag tester which connects to the given port names on the model
 * JtagTester jtag = vm.createJtagTester("TCK", "TMS", "TRSTb", "TDI", "TDOb");
 *
 * // create a logic settable device to control the given port
 * LogicSettable enable = vm.createLogicSettable("enable");
 *
 * // start the verilog process
 * vm.start("verilog", "chipModel.v", false);
 *
 *   ....
 *
 * vm.finish();
 *
 * </pre>
 * </code>
 * Note that you need to call <i>finish()</i> to end the simulator.  Also, devices can
 * only be created before the process is started.
 */
public class VerilogModel extends SimulationModel {

    private static final boolean DEBUG = false;      // general debugging messages
    private static final boolean DEBUGOUTPUT = false; // for debugging the verilog stdout parser

    /** Do not record simulation */
    public static final int NORECORD = 0;
    /** Use 'dumpvars' to record simulation */
    public static final int DUMPVARS = 1;
    /** Use 'recordvars' to record simulation */
    public static final int RECORDVARS = 2;

    /** list of jtag testers */
    private final List jtagTesters;
    /** list of logic settables */
    private final List logicSettables;
    /** list of aliased signal names at top level */
    private final AllAliasedNames aliased;
    /** number of ticks since simulation started */
    private int ticks;

    public static final String jtagControllerFile = "jtagController.v";

    /**
     * Create a new VerilogModel to simulate the behavior of the real chip.
     */
    public VerilogModel() {
        super("Verilog", "$finish;", "Error!", "C%% > ");
        jtagTesters = new ArrayList();
        logicSettables = new ArrayList();
        aliased = new AllAliasedNames();
        ticks = 0;
    }

    public JtagTester createJtagTester(String tckName, String tmsName, String trstbName, String tdiName, String tdobName) {
        if (isProcessRunning()) {
            System.out.println("Error: JtagTester test device must be created before Verilog process is started.");
            return null;
        }
        VerilogJtagTester tester = new VerilogJtagTester(this, tckName, tmsName, trstbName, tdiName, tdobName);
        jtagTesters.add(tester);
        return tester;
    }

    public JtagTester createJtagSubchainTester(String jtagInBus, String jtagOutBus) {
        if (isProcessRunning()) {
            System.out.println("Error: JtagTester test device must be created before Verilog process is started.");
            return null;
        }
        VerilogJtagSubchainTester tester = new VerilogJtagSubchainTester(this, jtagInBus, jtagOutBus);
        jtagTesters.add(tester);
        return tester;
    }

    public JtagTester createJtagSubchainTester(String phi2, String phi1, String write, String read, String sin, String sout) {
        if (isProcessRunning()) {
            System.out.println("Error: JtagTester test device must be created before Verilog process is started.");
            return null;
        }
        VerilogJtagSubchainTester tester = new VerilogJtagSubchainTester(this, phi2, phi1, write, read, sin, sout);
        jtagTesters.add(tester);
        return tester;
    }

    public LogicSettable createLogicSettable(String portName) {
        if (isProcessRunning()) {
            System.out.println("Error: LogicSettable test device must be created before Verilog process is started.");
            return null;
        }
        VerilogLogicSettable ls = new VerilogLogicSettable(this, portName);
        logicSettables.add(ls);
        aliased.add(ls.getAliasedNames());
        return ls;
    }

    public LogicSettable createLogicSettable(List portNames) {
        if (isProcessRunning()) {
            System.out.println("Error: LogicSettable test device must be created before Verilog process is started.");
            return null;
        }
        VerilogLogicSettable ls = new VerilogLogicSettable(this, portNames);
        logicSettables.add(ls);
        aliased.add(ls.getAliasedNames());
        return ls;
    }

    public void disableNode(String node) {
        Exception e = new Exception("Unsupported feature");
        e.printStackTrace(System.out);
    }

    public void enableNode(String node) {
        Exception e = new Exception("Unsupported feature");
        e.printStackTrace(System.out);
    }

    public double getVdd() {
        return 1;
    }

    public double getSimulationTime() {
        return ticks;
    }

    /**
     * Start the Verilog process. Returns false if failed to start.
     * This performs three steps:
     * <P>1. Create a local copy of the test harness file with information specific to this chip, and test setup.
     * <P>2. Setup input/output streams for talking to/from the verilog process.
     * <P>3. Run the verilog process on the local test harness file.
     * @param verilogCommand the command to run Verilog.
     * @param verilogSource the chip netlist.
     * @param recordSim true to record simulation via $recordfile(), $recordvars; false to not do so.
     * @return true if verilog process started, false otherwise.
     */
    public boolean start_(String verilogCommand, String verilogSource, int recordSim) {
        // parse the verilog source file: find the definition of the top level
        // module so we can instantiate it in the test harness
        VerilogParser vp = new VerilogParser();
        if (!vp.parse(verilogSource)) {
            System.out.println("Failed parsing verilog source file "+verilogSource);
            return false;
        }
        // find the last module, and it's port definitions
        List modules = vp.getModules();
        VerilogParser.Module top = (VerilogParser.Module)modules.get(modules.size()-1);

        // setup the local test harness file
        if (!startProcess(verilogCommand+" -s "+verilogSource, null, null, verilogSource+".log"))
            return false;

        // set up waveform file
        switch (recordSim) {
            case RECORDVARS: {
                // recordfile causes verilog to exit -- not supported anymore?
                issueCommand("$recordfile(\""+top.name+"\");");
                issueCommand("$recordvars;");
                //vout.write("$shm_open(\""+top.name+"\");\n");
                //vout.write("$shm_probe(\"S\");\n");
                //issueCommand("FID=$fopen(\""+top.name+".journal\");\n");
                break;
            }
            case DUMPVARS: {
                issueCommand("$dumpfile(\""+top.name+".dump\");");
                issueCommand("$dumpvars;");
                //issueCommand("FID=$fopen(\""+top.name+".journal\");\n");
                break;
            }
            case NORECORD: { break; }
        }

        // initialize all testers
        for (Iterator it = jtagTesters.iterator(); it.hasNext(); ) {
            BypassJtagTester tester = (BypassJtagTester)it.next();
            tester.reset();
        }

        return true;
    }

    /**
     * Let the verilog simulation run for some period of time.
     * Currently, one second = 1 million verilog ticks. Use
     * waitTicks() if you want to specify the number of verilog
     * ticks.
     * @param seconds
     */
    public void wait(float seconds) {
        long ticks = (long)(seconds * 1e6);
        waitTicks(ticks);
    }

    public void waitNS(double ns) {
        long ticks = (long)(ns * 1e3);
        waitTicks(ticks);
    }

    public void waitPS(double ps) {
        waitTicks((long)ps);
    }

    /**
     * Let the verilog simulation for some number of verilog ticks.
     * @param ticks
     */
    public void waitTicks(long ticks) {
        issueCommand("$db_steptime("+ticks+");");
        this.ticks += ticks;
    }

    public double getTimeNS() {
        return ticks/1000.0;
    }

    /**
     * Force node to a state.  Note that 1 is high and 0 is low.
     * The node is
     * case-insensitive, and may be a hierarchical spice name, such as 'Xtop.Xfoo.net@12'.
     * It should match the name from the spice file that nanosim is simulating.
     * @param node the hierarchical spice node name
     * @param state the state to set to, must be 1 or 0.
     */
    public void setNodeState(String node, int state) {
        if (state != 0 && state != 1) {
            System.out.println("Illegal state passed to setNodeState: "+state+". Expected 0 or 1.");
            return;
        }
        // replace any disallowed characters
        node = node.replaceAll("@", "_");

        issueCommand("force "+node+" = "+state+";");
        StringBuffer ret = getLastCommandOutput();
        if (ret.toString().matches("(Error!).*?(not declared)"))
            System.out.println("Error! setNodeState: node "+node+" not found");
    }

    /**
     * Get the state of a node.  Returns 1 or 0.
     * The node is
     * case-insensitive, and may be a hierarchical spice name, such as 'Xtop.Xfoo.net@12'.
     * It should match the name from the spice file that nanosim is simulating.
     * May return -1 if not a valid state.
     * May return -2 if node is undefined (X) or HiZ (Z).
     * Note this only works for single bit nodes, not busses
     * @param node the hierarchical spice node name
     */
    public int getNodeState(String node) {
        // replace any disallowed characters
        node = node.replaceAll("@", "_");

        issueCommand("$showvars("+node+");");
        StringBuffer result = getLastCommandOutput();
        String [] results = result.toString().trim().split("\n");

        for (int i=0; i<results.length; i++) {
            String s = results[i];
            String [] parts = s.split("\\s+");
            // first str is node name (with hierarchy stripped away)
            if (node.endsWith(parts[0])) {
                // state is last str
                String val = parts[parts.length-1].toLowerCase();
                if (val.equals("0") || val.equals("st0") || val.equals("su0") || val.equals("we0"))
                    return 0;
                else if (val.equals("1") || val.equals("st1") || val.equals("su1") || val.equals("we1"))
                    return 1;
                else if (val.equals("scheduled")) {
                    System.out.println("Warning, attempting to read node '"+node+"' that has a scheduled event at time "+ticks+", you should advance time before reading the node");
                    return -2;
                }
                else
                    return -2;      // undefined or HiZ
            }
        }
        System.out.println("Error! getNodeState: node "+node+" not found");
        return -1;
    }

    public void releaseNode(String s) {
        // replace any disallowed characters
        s = s.replaceAll("@", "_");
        issueCommand("release "+s+";");
    }
    
    public void releaseNodes(List nodes) {
        for (Iterator it = nodes.iterator(); it.hasNext(); ) {
            Object o = it.next();
            String s = (String)o;
            releaseNode(s);
        }
    }
    
    /**
     * Provides an example Verilog 'chip' file for Unit Tests.
     * @return the path and file name of the file.  Exits the program if not found.
     */
    static String getExampleVerilogChipFile() {
        URL jtagController = VerilogModel.class.getResource(jtagControllerFile);
        if (jtagController == null) {
            System.out.println("Can't find resource "+jtagControllerFile+".  Test Failed.");
            Infrastructure.exit(1);
        }
        return jtagController.getFile();
    }

    /**
     * Signals cannot be explicitly tied together in verilog.  Rather, they
     * must be tied together by only using the same name in all places the two names
     * were once used.  The AliasedNames class maintains the relationship for a
     * single name tying together several ports, this class maintains all the
     * AliasedNames for a design.
     */
    static class AllAliasedNames {
        private List aliases;
        private AllAliasedNames() {
            aliases = new ArrayList();
        }
        private void add(AliasedNames names) {
            if (names == null) return;
            aliases.add(names);
        }
        String getAliasFor(String replacedName) {
            for (Iterator it = aliases.iterator(); it.hasNext(); ) {
                AliasedNames aliased = (AliasedNames)it.next();
                String alias = aliased.getAliasFor(replacedName);
                if (alias != null) {
                    return alias;
                }
            }
            return replacedName;
        }
    }

    /**
     * Signals cannot be explicitly tied together in verilog.  Rather, they
     * must be tied together by using the same name in all places the two names
     * were once used.  This class maintains which names have been replaced by
     * a single name.
     */
    static class AliasedNames {
        private final String alias;
        private final List replacedNames;
        AliasedNames(String name, List replacedNames) {
            this.alias = name;
            this.replacedNames = new ArrayList(replacedNames);
        }
        private String getAliasFor(String n) {
            if (replacedNames.contains(n)) return alias;
            return null;
        }
        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append("Alias '"+alias+"' for: ");
            for (Iterator it = replacedNames.iterator(); it.hasNext(); ) {
                String nn = (String)it.next();
                buf.append(nn);
                if (it.hasNext()) buf.append(", ");
            }
            return buf.toString();
        }
    }

    static String formatDataNetName(String dataNetName) {
        // need to convert []'s in instance names to _ (but not brackets in net names)
        // this converts brackets in inst names with unconnected nets, which electric netlists as instname[n]_portname
        //dataNetName = dataNetName.replaceAll("\\[(.*?)\\]_","_$1__");
        // this converts brackets in inst names in the hierarchy, so inst[n].morestuff -> inst_n_.morestuff
        //dataNetName = dataNetName.replaceAll("\\[(.*?)\\]\\.","_$1_.");
        // replace [X] with _X_ if X is non-numeric
        dataNetName = dataNetName.replaceAll("\\[([a-zA-Z_][^\\]]*)\\]", "_$1_");
        // replace [X] with _X_ if it is not the last index
        dataNetName = dataNetName.replaceAll("\\[(.*?)\\](?=.)", "_$1_");
        // also, need to get rid of @
        dataNetName = dataNetName.replace('@', '_');

        // convert spice-like name to verilog name
        String [] parts = dataNetName.split("\\.");
        StringBuffer newName = new StringBuffer();

        for (int i=0; i<parts.length-1; i++) {
            if (parts[i].startsWith("x") || parts[i].startsWith("X")) {
                parts[i] = parts[i].substring(1);
            }
            newName.append(parts[i]);
            newName.append(".");
        }
        if (parts.length>0)
            newName.append(parts[parts.length-1]);
        return newName.toString();
    }

    /** Unit test */
    public static void main(String[] args) {
        // start process
        VerilogModel vm = new VerilogModel();
        vm.start("verilog", getExampleVerilogChipFile(), VerilogModel.NORECORD);
        // send commands to process stdin
        vm.issueCommand("$showvars;");
        vm.finish();
    }

}
