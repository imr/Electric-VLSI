/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NanosimModel.java
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

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.*;

public class NanosimModel extends SimulationModel {

    protected static final boolean DEBUG = false;

    /** list of jtag testers */
    protected final List jtagTesters;
    /** list of logic settables */
    protected final List logicSettables;

    protected double simTime;         // current simulation time in ns
    protected double vdd;
    protected double timeStep;        // simulation time step in ns
    protected final HashMap nodesToSet = new HashMap();   // key: node name, value: Double voltage
    protected boolean assuraRCXNetlist = false;
    protected boolean starRCXTNetlist = false;

    /**
     * Create a new NanosimModel to simulate the behavior of the real chip
     */
    public NanosimModel() {
        super("Nanosim", "quit", "ERROR", "");
        jtagTesters = new ArrayList();
        logicSettables = new ArrayList();
        simTime = 0;
        vdd = 0;
    }

    public JtagTester createJtagTester(String tckName, String tmsName, String trstbName, String tdiName, String tdobName) {
        if (isProcessRunning()) {
            System.out.println("Error: JtagTester test device must be created before process is started.");
            return null;
        }
        NanosimJtagTester tester = new NanosimJtagTester(this, tckName, tmsName, trstbName, tdiName, tdobName);
        jtagTesters.add(tester);
        return tester;
    }

    /**
     * Create a subchain tester based on the 8- or 9-wire jtag interface.
     * jtag[8:0] = {scan_data_return, phi2_return, phi1_return, rd, wr, phi1, phi2, sin, mc*}
     * Note that mc is not present on older designs, so they are jtag[8:1].
     * @param jtagInBus the name of the 9-bit wide input bus, i.e. "jtagIn" or "jtagIn[8:0]"
     * @param jtagOutBus the name of the 9-bit wide output bus, i.e. "jtagOut" or "jtagOut[8:0]"
     */
    public JtagTester createJtagSubchainTester(String jtagInBus, String jtagOutBus) {
        if (isProcessRunning()) {
            System.out.println("Error: JtagTester test device must be created before process is started.");
            return null;
        }
        NanosimJtagSubchainTester tester = new NanosimJtagSubchainTester(this, jtagInBus, jtagOutBus);
        jtagTesters.add(tester);
        return tester;
    }

    /**
     * Create a subchain tester based on the 5-wire jtag interface.
     * @param phi2 name of the phi2 signal
     * @param phi1 name of the phi1 signal
     * @param write name of the write signal
     * @param read name of the read signal
     * @param sin name of the scan data in signal
     * @param sout name of the scan data out signal
     */
    public JtagTester createJtagSubchainTester(String phi2, String phi1, String write, String read, String sin, String sout) {
        if (isProcessRunning()) {
            System.out.println("Error: JtagTester test device must be created before process is started.");
            return null;
        }
        NanosimJtagSubchainTester tester = new NanosimJtagSubchainTester(this, phi2, phi1, write, read, sin, sout);
        jtagTesters.add(tester);
        return tester;
    }

    public LogicSettable createLogicSettable(String portName) {
        if (isProcessRunning()) {
            System.out.println("Error: LogicSettable test device must be created before process is started.");
            return null;
        }
        NanosimLogicSettable ls = new NanosimLogicSettable(this, portName);
        logicSettables.add(ls);
        return ls;
    }

    public LogicSettable createLogicSettable(List portNames) {
        if (portNames == null || portNames.size() < 1) {
            System.out.println("Error: createLogicSettable given null or empty list of ports");
            return null;
        }
        System.out.println("Error: createLogicSettable(List) is not supported by Nanosim, only using first port "+portNames.get(0));
        return new NanosimLogicSettable(this, (String)portNames.get(0));
    }

    public void disableNode(String node) {
        issueCommand("force_node_v v=0 no="+node);
        waitNS(timeStep);
    }

    public void enableNode(String node) {
        issueCommand("rel_node_v no="+node);
        waitNS(timeStep);
    }

    public double getSimulationTime() {
        return simTime;
    }

    boolean start_(String command, String simFile, int recordSim) {
        // first, find out the nanosim version, as that determines the prompt in interactive mode
        // setup the reader which will read the output of the process
        PipedOutputStream ostream = new PipedOutputStream();
        BufferedReader reader;
        try {
            PipedInputStream istream = new PipedInputStream(ostream);
            reader = new BufferedReader(new InputStreamReader(istream));
        } catch (java.io.IOException e) {
            System.out.println("Unable to create pipe to process output: "+e.getMessage());
            return false;
        }
        ExecProcess process = new ExecProcess(command+" --version", null, null, ostream, ostream);
        process.start();
        boolean found = false;
        String version = null;
        StringBuffer buf = new StringBuffer();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                buf.append(line+"\n");
                if (found) continue;
                String [] args = line.split("\\s+");
                if (args.length < 4) continue;
                if (args[1].equals("Version")) {
                    version = args[3];
                    setPrompt("Ver "+version+" >");
                    found = true;
                }
            }
            reader.close();
        } catch (java.io.IOException e) {
            if (!found) {
                System.out.println("Error determining nanosim version: "+e.getMessage());
                System.out.println(buf);
                return false;
            }
        }
        if (!found) {
            System.out.println("Error determining nanosim version");
            System.out.println(buf);
            return false;
        }
        if (DEBUG)
            System.out.println("Using nanosim version "+version);

        // check if simfile is from assura RCX
        // if it is, we will have to replace hierarchy delimiter '.' with '/'
        try {
            BufferedReader freader = new BufferedReader(new FileReader(simFile));
            // PROGRAM will be in the first 10 or so lines, so search up till line 20
            for (int i=0; i<20; i++) {
                String line = freader.readLine();
                if (line == null) break;
                if (line.matches("\\*  PROGRAM .*?assura.*")) {
                    assuraRCXNetlist = true;
                    System.out.println("Info: Running on Assura extracted netlist, will replace all '.' in net names with '/'");
                    break;
                } else if (line.matches("\\*|PROGRAM .*?Star-RCXT.*")) {
                    starRCXTNetlist = true;
                    System.out.println("Info: Running on Star-RCXT extracted netlist, will replace all '.x' in net names with '/'");
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }

        // max sim time (-t) is 90071s
        String cmd = command+" -n "+simFile+" -i -t 90071s -o "+simFile;
        if (!startProcess(cmd, null, null, simFile+".run"))
            return false;

        // get value of vdd
        vdd = getNodeVoltage("vdd");
        if (DEBUG)
            System.out.println("Using VDD of "+vdd);
        timeStep = getSimTres();
        if (DEBUG)
            System.out.println("Using time step of "+timeStep+" ns");

        // initialize test devices
        for (Iterator it = logicSettables.iterator(); it.hasNext(); ) {
            NanosimLogicSettable ls = (NanosimLogicSettable)it.next();
            if (!ls.init()) {
                System.out.println("LogicSettable initialization failed, aborting.");
                return false;
            }
        }
        for (Iterator it = jtagTesters.iterator(); it.hasNext(); ) {
            JtagTester tester = (JtagTester)it.next();
            tester.reset();
        }
        return true;
    }

    /**
     * Get the voltage of VDD for the simulation.  The simulation must have been
     * started for this method to return a valid value.
     * @return the VDD voltage, in volts.
     */
    public double getVdd() { return vdd; }

    protected static final Pattern patSimTime = Pattern.compile("The simulation time is\\s+: ([0-9\\.]+) ns");

    public void wait(float seconds) {
        waitNS(seconds*1.0e9);
    }

    /**
     * Wait the specified number of nano-seconds.  In this case, this means
     * run the simulator until that point in time, when control will return
     * to the test software.
     * @param nanoseconds the time to wait (simulate) in nanoseconds
     */
    public void waitNS(double nanoseconds) {
        waitNS(nanoseconds, true);
    }

    // internal only method; applyVoltages calls this with the boolean false so no infinite recursion
    protected void waitNS(double nanoseconds, boolean applyVoltages) {
/*
        issueCommand("get_sim_time");
        StringBuffer buf = getLastCommandOutput();
        Matcher m = patSimTime.matcher(buf.toString());
        double cur_time = 0;
        if (m.find()) {
            try {
                cur_time = Double.parseDouble(m.group(1));
            } catch (NumberFormatException e) {
                System.out.println("Error in get_sim_time, could not parse time "+m.group(1)+" from:\n"+buf);
                return;
            }
        } else {
            System.out.println("Error determining current time, get_sim_time returned: "+buf);
            return;
        }
*/
        // first apply any voltages that have been set
        if (applyVoltages)
            applyVoltages();

        if (nanoseconds < timeStep) {
            System.out.println("Warning: cannot run simulator in increments less than time step (currently "+timeStep+" ns), setting it to "+timeStep+" ns");
            nanoseconds = timeStep;
        }
        // round wait time to the nearest time step, otherwise nanosim gets confused
        nanoseconds = (int)(nanoseconds/timeStep) * timeStep;

        // calculate stop time (time at which to set breakpoint)
        double stopTime = simTime + nanoseconds;
        // set break point
        issueCommand("set_time_break "+stopTime+"ns");
        issueCommand("cont_sim");
        simTime = stopTime;
    }

    public void waitPS(double ps) {
        waitNS(ps/1000.0);
    }

    public double getTimeNS() {
        return simTime;
/*
        issueCommand("get_sim_time");
        StringBuffer buf = getLastCommandOutput();
        Matcher m = patSimTime.matcher(buf.toString());
        double cur_time = 0;
        if (m.find()) {
            try {
                cur_time = Double.parseDouble(m.group(1));
            } catch (NumberFormatException e) {
                System.out.println("Error in get_sim_time, could not parse time "+m.group(1)+" from:\n"+buf);
                return cur_time;
            }
        } else {
            System.out.println("Error determining current time, get_sim_time returned: "+buf);
            return cur_time;
        }
        return cur_time;
*/
    }

    protected static final Pattern getSimTime_tres = Pattern.compile("The engine time resolution is\\s+: ([0-9\\.]+) ns");
    /**
     * Get the simulation's time step
     * @return the simulation time step in nanoseconds
     */
    protected double getSimTres() {
        issueCommand("get_sim_time");
        StringBuffer buf = getLastCommandOutput();
        Matcher m = getSimTime_tres.matcher(buf);
        if (m.find()) {
            try {
                double d = Double.parseDouble(m.group(1));
                return d;
            } catch (NumberFormatException e) {
                System.out.println("Error converting string to double in "+m.group(0)+": "+e.getMessage());
            }
        }
        double d = 0.01;    // default is 10ps
        System.out.println("Cannot determine time step, using default of "+d+" ns");
        return d;
    }

    // ====================================================================
    // ====================================================================

    //                      Get/Set voltages

    // ====================================================================
    // ====================================================================

    /**
     * Force node to a state.  Note that 1 is high and 0 is low.
     * The node is
     * case-insensitive, and may be a hierarchical spice name, such as 'Xtop.Xfoo.net@12'.
     * It should match the name from the spice file that nanosim is simulating.
     * @param node the hierarchical spice node name
     * @param state the state to set to, must be 1 or 0.
     */
    public void setNodeState(String node, int state) {
        node = node.toLowerCase();
        if (state != 0 && state != 1) {
            System.out.println("Illegal state passed to setNodeState: "+state+". Expected 0 or 1.");
            return;
        }
        setNodeVoltage(node, state*vdd);
    }

    /**
     * Set a node in the nanosim simulation to the specified voltage.
     * The node is
     * case-insensitive, and may be a hierarchical spice name, such as 'Xtop.Xfoo.net@12'.
     * It should match the name from the spice file that nanosim is simulating.
     * @param node the hierarchical spice node name
     * @param voltage the voltage to set the node to
     */
    public void setNodeVoltage(String node, double voltage) {
        node = node.toLowerCase();
        //issueCommand("set_node_v "+node+" "+voltage+" r=5 l=1n");
        nodesToSet.put(node, new Double(voltage));
    }

    /**
     * This is used to apply all voltages at the same time for the current time step.
     */
    protected void applyVoltages() {
        // release all forced nodes
        releaseNodes(new ArrayList(nodesToSet.keySet()));

        // all nodes are released, now apply all the values
        for (Iterator it = nodesToSet.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            String node = (String)entry.getKey();
            Double voltage = (Double)entry.getValue();
            if (assuraRCXNetlist) {
                node = node.replaceAll("\\.", "/");
            } else if (starRCXTNetlist) {
                if (node.startsWith("x")) node = node.substring(1);
                node = node.replaceAll("\\.x?", "/");
            }
            issueCommand("force_node_v v="+voltage.doubleValue()+" no="+node);
        }
        nodesToSet.clear();
        waitNS(timeStep, false);
    }

    // release the list of nodes (Strings)
    public void releaseNodes(List nodes) {
        // NOTE JKG 7/21/05:
        // Ajanta and I have found that release does not always work,
        // and sometimes the node remains "forced". Issuing another
        // release seems to remove the forced state, though, so the
        // following code issues releases until it is released
        // (or stops after 10 tries)
        int i=0;
        int nodesReleased = 0;
        while (nodesReleased < nodes.size() && i<10) {
            // make sure all nodes to set are not forced
            nodesReleased = 0;
            boolean releasedIssued = false;
            for (Iterator it = nodes.iterator(); it.hasNext(); ) {
                String node = (String)it.next();
                if (assuraRCXNetlist) {
                    node = node.replaceAll("\\.", "/");
                } else if (starRCXTNetlist) {
                    if (node.startsWith("x")) node = node.substring(1);
                    node = node.replaceAll("\\.x?", "/");
                }
                issueCommand("get_node_info detail=on "+node);
                StringBuffer output = getLastCommandOutput();
                if (output.indexOf("IS_FORCED: 1") != -1) {
                    // we need to release node
                    issueCommand("rel_node_v no="+node);
                    releasedIssued = true;
                } else {
                    nodesReleased++;            // this node is already released
                }
            }
            if (releasedIssued) {
                // advance time to try apply release
                waitNS(timeStep, false);
            }
            i++;
        }
    }

    List getNodeVoltages(List nodes) {
        return getNodeInfo(nodes, false);
    }

    List getNodeStates(List nodes) {
        return getNodeInfo(nodes, true);
    }

    /**
     * Get the state of a node.  Returns 1 or 0.
     * The node is
     * case-insensitive, and may be a hierarchical spice name, such as 'Xtop.Xfoo.net@12'.
     * It should match the name from the spice file that nanosim is simulating.
     * May return -1 if not a valid state.
     * @param node the hierarchical spice node name
     */
    public int getNodeState(String node) {
        List list = new ArrayList();
        list.add(node);
        List vals = getNodeInfo(list, true);
        if (vals != null && vals.size() > 0)
            return ((Number)vals.get(0)).intValue();
        return -1;
    }

    /**
     * Get the voltage on a node.
     * The node is
     * case-insensitive, and may be a hierarchical spice name, such as 'Xtop.Xfoo.net@12'.
     * It should match the name from the spice file that nanosim is simulating.
     * @param node the hierarchical spice node name
     * @return the voltage, or -1 if error
     */
    public double getNodeVoltage(String node) {
        List list = new ArrayList();
        list.add(node);
        List vals = getNodeInfo(list, false);
        if (vals != null && vals.size() > 0)
            return ((Double)vals.get(0)).doubleValue();
        return -1;
    }

    protected static final Pattern patNodeInfo = Pattern.compile("Node status of (.*?)\\((\\d+)\\): (.*?) \\(([0-9\\.\\-]+) V\\)");
    // digital only
    protected static final Pattern patNodeInfo2 = Pattern.compile("Node status of (.*?)\\((\\d+)\\): (\\d+)");

    /**
     * Gets the voltages on the nodes, unless returnState is true, in
     * which case it returns a list of 1's and 0's representing the logic state,
     * rather than the actual voltages.  Also returns -2 for undefined logic state, or -1 on error.
     * @param nodes the nodes to query
     * @param returnState true to convert voltages to 1 or 0 logic states
     * @return a list of voltages/states, or null on error
     */
    protected List getNodeInfo(List nodes, boolean returnState) {
        // apply any outstanding voltages
        if (nodesToSet.size() > 0)
            applyVoltages();
        if (assuraRCXNetlist || starRCXTNetlist) {
            List nodesfixed = new ArrayList();
            for (Iterator it = nodes.iterator(); it.hasNext(); ) {
                String node = (String)it.next();
                if (assuraRCXNetlist) {
                    node = node.replaceAll("\\.", "/");
                } else if (starRCXTNetlist) {
                    if (node.startsWith("x")) node = node.substring(1);
                    node = node.replaceAll("\\.x?", "/");
                }
                nodesfixed.add(node);
            }
            nodes = nodesfixed;
        }


        List nodeslc = new ArrayList();
        StringBuffer cmd = new StringBuffer();
        cmd.append("get_node_info ");
        for (Iterator it = nodes.iterator(); it.hasNext(); ) {
            String node = (String)it.next();
            node = node.toLowerCase();
            nodeslc.add(node);
            cmd.append(node+" ");
        }
        nodes = nodeslc;

        issueCommand(cmd.toString());
        StringBuffer result = getLastCommandOutput();
        String [] results = result.toString().trim().split("\n");

        List voltages = new ArrayList();
        List states = new ArrayList();
        int nodeIndex = 0;
        for (int i=0; i<results.length; i++) {
            if (!results[i].startsWith("Node status")) continue;        // quick check
            Matcher m = patNodeInfo.matcher(results[i]);
            Matcher m2 = patNodeInfo2.matcher(results[i]);
            String node = (String)nodes.get(nodeIndex);
            if (m.find()) {
                // don't do this check anymore, as nanosim can map the hierarchical name to the real name
                //if (!m.group(1).equals(node)) {
                //    System.out.println("Error on get_info_node: expected info for node "+node+" but got info for node "+m.group(1));
                //    return null;
                //}
                Integer state;
                // state can be one of 1, 0, or U
                if (m.group(3).equals("1"))
                    state = new Integer(1);
                else if (m.group(3).equals("0"))
                    state = new Integer(0);
                else if (m.group(3).equals("U"))
                    state = new Integer(-2);
                else {
                    System.out.println("Uknown state of "+node+": "+m.group(3)+", setting it to -1 (Undefined)");
                    state = new Integer(-1);
                }
                Double voltage;
                try {
                    voltage = new Double(m.group(4));
                } catch (NumberFormatException e) {
                    System.out.println("Error on get_info_node: NumberFormatException converting node "+node+" state/voltage ("+m.group(3)+"/"+m.group(4)+") to integer/double");
                    return null;
                }

                voltages.add(voltage);
                states.add(state);
                nodeIndex++;
                if (DEBUG) System.out.println("get_info_node "+node+": \tstate="+state+"\tvoltage="+voltage);
            } else if (m2.find() && returnState) {
                // digial only
                if (!m2.group(1).equals(node)) {
                    System.out.println("Error on get_info_node: expected info for node "+node+" but got info for node "+m.group(1));
                    return null;
                }
                Integer state;
                // state can be one of 1, 0, or U
                if (m2.group(3).equals("1"))
                    state = new Integer(1);
                else if (m2.group(3).equals("0"))
                    state = new Integer(0);
                else if (m2.group(3).equals("U"))
                    state = new Integer(-2);
                else {
                    System.out.println("Uknown state of "+node+": "+m2.group(3)+", setting it to -1 (Undefined)");
                    state = new Integer(-1);
                }
                states.add(state);
                nodeIndex++;
                if (DEBUG) System.out.println("get_info_node "+node+": \tstate="+state);
            }
        }
        if (returnState) return states;
        return voltages;
    }

    /**
     * Saves the state of all nodes at the given time to an external .ic file,
     * using the report_node_ic command.
     * @param time time in nanoseconds
     */
    public void reportNodeIC(double time) {
        issueCommand("report_node_ic all "+time+"ns");
    }

    /**
     * Saves the state of all nodes at the current time to an external .ic file,
     * using the report_node_ic command.
     */
    public void reportNodeIC() {
        issueCommand("report_node_ic all");
    }

    /** Unit Test
     * This test requires the file sim.spi in your working dir
     * */
    public static void main(String[] args) {
        NanosimModel nm = new NanosimModel();
        // the example file sim.spi needs to be in your working dir
        nm.start("nanosim", "sim.spi", 0);
        nm.issueCommand("get_sim_time");
        nm.finish();
    }
}
