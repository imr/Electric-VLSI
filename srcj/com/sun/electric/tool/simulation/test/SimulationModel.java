/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SimulationModel.java
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
import java.util.Iterator;
import java.io.*;

/**
 * Defines a model that replaces the actual chip (device under test).  This software
 * model can simulate the behavior of the actual chip.
 * <P>
 * For the most part, this allows you to create devices that replace the
 * measurement devices that would normally probe a Device Under Test.  Instead
 * these devices will now probe the software model.
 * <P>
 * Most of the process control code has been moved into this Class, because
 * it is fairly similar between the various simulators.
 */
public abstract class SimulationModel implements ChipModel {


    /** if using simulation model, disable all GPIB devices */
    private static boolean inUse = false;

    private static List models = new ArrayList();

    /** The simulator name */
    protected final String simulatorName;
    /** The simulator's interactive quit command (used when processing output) */
    protected final String quitCommand;
    /** The simulator's error string on the interactive prompt when an error occurs */
    protected final String errorFlag;
    /** The simulator's interactive prompt (used when processing output) */
    private String outputPrompt;
    /** The simulator's interactive Error message */

    /** The Verilog Process. Null if not started, or terminated due to error */
    private ExecProcess process;
    /** The output log file */
    private PrintWriter logfile;
    private String logfileName = null;
    /** A pipe connected to the output of the process that allows us to read the output */
    private BufferedReader processReader;
    /** Stores the output from Verilog for the last command issued */
    private StringBuffer lastCommandOutput;
    /** Current interactive command number */
    private int currentCommand;
    /** see now long simulation took */
    private long startTime;
    /** any additional command line arguments */
    private String additionalCommandLineArgs = "";
    // optimized reads and writes for bypassScanning mode
    private boolean optimizedDirectReadsWrites = false;

    private boolean suppressErrorMsgs = false;
    private int numIssueCommandErrors = 0;

    /** true if any assertions failed */
    private boolean assertionFailed = false;


    /**
     * Enable or disable bypassing the scan chain. If true, instead of
     * scanning in bits serially and applying the data on "write",
     * data is applied directly to the data bits, without a need
     * for scanning in the bits. Note that this means no
     * data was scanned in, so no data was scanned out.  I.e.,
     * if you need to "read", you should not use this mode.
     * <P>
     * Read and write are not applied in this mode. The jtag controller
     * is not used in this mode. Only bits who have their "dataNet"
     * property set in the XML file will have data applied.
     * <P>
     * The state of this flag must not change during simulation,
     * otherwise undesirable behavior will occur.
     */
    private boolean bypassScanning = false;

    /**
     * Default constructor.  Should be called from extending classes.
     * Sets flag to disable GPIB devices.
     * @param simulatorName the name of this simulator (Verilog, Nanosim, etc)
     * @param quitCommand the command used to quit the simulator ($finish;, quit, etc)
     * @param errorFlag the error String found in the simulator's output on error
     * @param outputPrompt the prompt from the interactive process.  If this contains %%,
     * then %% will be replaced with the current command count.  This can also be set
     * later with setPrompt()
     */
    public SimulationModel(String simulatorName, String quitCommand, String errorFlag, String outputPrompt) {
        this.simulatorName = simulatorName;
        this.quitCommand = quitCommand;
        this.errorFlag = errorFlag;
        this.outputPrompt = outputPrompt;
        process = null;
        logfile = null;
        logfileName = null;
        processReader = null;
        currentCommand = 1;
        lastCommandOutput = null;
        setInUse(true);
        models.add(this);
        startTime = System.currentTimeMillis();
    }

    /**
     * Create a {@link JtagTester} that can be used to drive the JtagController on the
     * Software model of the chip.  The arguments specify the port names specific
     * to the software model that correspond to the Jtag Controller ports.
     * @param tckName name of the input port for TCK
     * @param tmsName name of the input port for TMS
     * @param trstbName name of the input port for TRSTb
     * @param tdiName name of the input port for TDI
     * @param tdobName name of the input port for TDOb
     * @return a JtagTester than can be used to test the chip
     */
    public abstract JtagTester createJtagTester(String tckName, String tmsName,
                                       String trstbName, String tdiName, String tdobName);

    /**
     * Create a {@link JtagTester} that can be used to drive the JtagController on the
     * Software model of the chip.  This uses the default port names of
     * TCK, TMS, TRSTb, TDI, and TDOb.
     * @return a JtagTester than can be used to test the chip
     */
    public JtagTester createJtagTester() {
        return createJtagTester("TCK", "TMS", "TRSTb", "TDI", "TDOb");
    }

    /**
     * Create a subchain tester based on the 8- or 9-wire jtag interface.
     * jtag[8:0] = {scan_data_return, phi2_return, phi1_return, rd, wr, phi1, phi2, sin, mc*}
     * Note that mc is not present on older designs, so they are jtag[8:1].
     * @param jtagInBus the name of the 9-bit wide input bus, i.e. "jtagIn" or "jtagIn[8:0]"
     * @param jtagOutBus the name of the 9-bit wide output bus, i.e. "jtagOut" or "jtagOut[8:0]"
     */
    public abstract JtagTester createJtagSubchainTester(String jtagInBus, String jtagOutBus);

    /**
     * Create a subchain tester based on the 5-wire jtag interface.
     * @param phi2 name of the phi2 signal
     * @param phi1 name of the phi1 signal
     * @param write name of the write signal
     * @param read name of the read signal
     * @param sin name of the scan data in signal
     * @param sout name of the scan data out signal
     */
    public abstract JtagTester createJtagSubchainTester(String phi2, String phi1, String write, String read, String sin, String sout);

    /**
     * Create a {@link LogicSettable} that can be used to control a port on
     * the Software model of the chip.
     * @param portName the name of the port to control.
     * @return a LogicSettable tied to the given port.
     */
    public abstract LogicSettable createLogicSettable(String portName);

    /**
     * Create a {@link LogicSettable} that can be used to control a set of ports on
     * the Software model of the chip.  The ports then act as if they have
     * been tied together.
     * @param portNames a list of Strings of port names to be controlled.
     * @return a LogicSettable tied to the given port.
     */
    public abstract LogicSettable createLogicSettable(List portNames);

    /**
     * Force node to a state.  Note that 1 is high and 0 is low.
     * The node is
     * case-insensitive, and may be a hierarchical spice name, such as 'Xtop.Xfoo.net@12'.
     * It should match the name from the spice file that nanosim is simulating.
     * @param node the hierarchical spice node name
     * @param state the state to set to, must be 1 or 0.
     */
    public abstract void setNodeState(String node, int state);

    /**
     * Get the state of a node.  Returns 1 or 0.
     * The node is
     * case-insensitive, and may be a hierarchical spice name, such as 'Xtop.Xfoo.net@12'.
     * It should match the name from the spice file that nanosim is simulating.
     * May return -1 if not a valid state.
     * @param node the hierarchical spice node name
     */
    public abstract int getNodeState(String node);

    /**
     * Release any nodes being forced to a value using set node state
     * @param nodes a list of node names (strings)
     */
    public abstract void releaseNodes(List nodes);

    /**
     * Get the voltage value for vdd
     * @return the voltage value for vdd
     */
    protected abstract double getVdd();

    /**
     * Get the current simulation time, used for error reporting
     * @return the current simulation time as tracked by the underlying simulation model
     */
    protected abstract double getSimulationTime();

    /**
     * Set a bus of nodes to a state.  See setNodeState(String node, int state).
     * @param bus the bus
     * @param state the state to set the bus to
     */
    public void setNodeState(BussedIO bus, BitVector state) {
        for (int i=0; i<bus.getWidth(); i++) {
            setNodeState(bus.getSignal(i), state.get(i)?1:0);
        }
    }

    /**
     * Get the state of a bus.  Set getNodeState(String node).
     * @param bus the bus
     * @return the state of the bus.
     */
    public BitVector getNodeState(BussedIO bus) {
        BitVector state = new BitVector(bus.getWidth(), bus.getName());
        for (int i=0; i<bus.getWidth(); i++) {
            int b = getNodeState(bus.getSignal(i));
            if (b == 1) state.set(i);
            if (b == 0) state.clear(i);
        }
        return state;
    }

    /**
     * Assert that a node is at a given state. If not,
     * an error message will be printed, and the
     * simulation will continue on. The exit value
     * of the process will be non-zero if any assertions fail.
     * @param node the hierarchical node name
     * @param expectedState the expected state, 1 is high and 0 is low.
     */
    public void assertNodeState(String node, int expectedState) {
        assertNodeState(node, expectedState, "");
    }

    /**
     * Assert that a node is at a given state. If not,
     * an error message will be printed, and the
     * simulation will continue on. The exit value
     * of the process will be non-zero if any assertions fail.
     * @param node the hierarchical node name
     * @param expectedState the expected state, 1 is high and 0 is low.
     * @param errMsg additional error message
     */
    public void assertNodeState(String node, int expectedState, String errMsg) {
        int value = getNodeState(node);
        if (value != expectedState) {
            System.out.println("Assertion failed for node \""+node+"\", state was "+
                    value+" (expected: "+expectedState+") "+errMsg);
            assertionFailed = true;
        }
    }

    /**
     * Set any additional command line arguments that will be appended
     * to the simulation command when run
     * @param args the args to append
     */
    public void setAdditionalCommandLineArgs(String args) {
        additionalCommandLineArgs = args;
    }

    /**
     * Start the Chip Model simulation, and have it ready to accept
     * commands from the test software.
     * @param command the command to start the simulation.
     * @param simFile the file to simulate.
     * @param recordSim the level of simulation recording used.
     */
    public void start(String command, String simFile, int recordSim) {
        start(command, simFile, recordSim, false);
    }

    /**
     * Start the Chip Model simulation, and have it ready to accept
     * commands from the test software.
     * @param command the command to start the simulation.
     * @param simFile the file to simulate.
     * @param recordSim the level of simulation recording used.
     * @param bypassScanning true to bypass scanning in of data. See {@link #bypassScanning}.
     */
    public void start(String command, String simFile, int recordSim, boolean bypassScanning) {
        this.bypassScanning = bypassScanning;
        if (!start_(command, simFile, recordSim))
            Infrastructure.exit(1);
    }

    abstract boolean start_(String command, String simFile, int recordSim);
    /**
     * Tell the Chip Model simulation to continue simulating for the
     * time specified
     * @param seconds number of seconds to continue simulating
     */
    public abstract void wait(float seconds);

    public abstract void waitNS(double nanoseconds);
    public abstract void waitPS(double picoseconds);
    /**
     * Return the current simulation time in nanoseconds
     * @return current simulation time in nanoseconds
     */
    public abstract double getTimeNS();

    /**
     * If supported, disable a node (forces it to 0)
     * @param node the name of the node
     */
    public abstract void disableNode(String node);
    /**
     * If supported, enable a node (allows it to be driven)
     * @param node the name of the node
     */
    public abstract void enableNode(String node);

    public void setBypassScanning(boolean enabled) { bypassScanning = enabled; }

    /**
     * Enable optimized direct reads and writes. This only has effect
     * if bypass scanning mode is enabled. With this enabled, the test
     * code keeps track of the state of shadow registers in the
     * scan chain elements, and does not perform a read if it already
     * knows what will be read, and does not perform a write if it
     * knows the current state matches the state to write.
     * @param enabled true to enable optimized direct reads and writes
     */
    public void setOptimizedDirectReadsWrites(boolean enabled) {
        optimizedDirectReadsWrites = enabled;
    }

    /**
     * See {@link #setOptimizedDirectReadsWrites(boolean)} 
     */
    public boolean getOptimizedDirectReadsWrites() { return optimizedDirectReadsWrites; }

    /**
     * Check the state of bypass scanning.
     *
     * See {@link SimulationModel#bypassScanning}.
     * @return true if enabled, false if not.
     */
    public boolean isBypassScanning() { return bypassScanning; }

    /**
     * Tell any Chip Model simulators currently running to wait
     * for the specified time.  If any simulators are running,
     * return true.
     * @param seconds the time to wait.
     * @return true if any simulators were active.
     */
    public static boolean waitSeconds(float seconds) {
        if (!isInUse()) return false;

        for (Iterator it = models.iterator(); it.hasNext(); ) {
            SimulationModel cm = (SimulationModel)it.next();
            cm.wait(seconds);
        }
        return true;
    }

    /**
     * Tell the Chip Model simulation to end, and stop accepting
     * input from the test software.
     */
    public void finish() {
        long time = System.currentTimeMillis() - startTime;
        System.out.println("Simulation took "+getElapsedTime(time));
        issueCommand(quitCommand);
        if (assertionFailed)
            Infrastructure.exit(1);
    }


    /**
     * Terminate all models. Used when program encounters error
     * and needs to exit.
     */
    public static void finishAll() {
        for (Iterator it = models.iterator(); it.hasNext(); ) {
            SimulationModel cm = (SimulationModel)it.next();
            cm.finish();
        }
    }


    /**
     * Set whether the chip model is in use.
     * @param e
     */
    private static synchronized void setInUse(boolean e) {
        inUse = e;
    }

    /**
     * Get whether or not a chip model is in use.
     * @return if the chip model is in use
     */
    static synchronized boolean isInUse() { return inUse; }

    /**
     * Method to describe a time value as a String.
     * @param milliseconds the time span in milli-seconds.
     * @return a String describing the time span with the
     * format: days : hours : minutes : seconds
     */
    public static String getElapsedTime(long milliseconds)
    {
        if (milliseconds < 60000)
        {
            // less than a minute: show fractions of a second
            return (milliseconds / 1000.0) + " secs";
        }
        StringBuffer buf = new StringBuffer();
        int seconds = (int)milliseconds/1000;
        if (seconds < 0) seconds = 0;
        int days = seconds/86400;
        if (days > 0) buf.append(days + " days, ");
        seconds = seconds - (days*86400);
        int hours = seconds/3600;
        if (hours > 0) buf.append(hours + " hrs, ");
        seconds = seconds - (hours*3600);
        int minutes = seconds/60;
        if (minutes > 0) buf.append(minutes + " mins, ");
        seconds = seconds - (minutes*60);
        buf.append(seconds + " secs");
        return buf.toString();
    }

    // ====================================================================
    //
    //                     Process Control
    //
    // ====================================================================



    private static final boolean DEBUGPROCESS = false;      // general debugging messages
    private static final boolean DEBUGOUTPUT = false;      // output parsing debug messages

    /**
     * Start a simulator's process.
     * @param command the command to start the process
     * @param envVars environment variables, or null if none
     * @param dir working dir, null for current
     * @param logfileName name of log file of interaction with process
     * @return true if process started, false otherwise
     */
    protected boolean startProcess(String command, String [] envVars, File dir,
                                   String logfileName) {
        // setup the log file
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(logfileName, false);
            logfile = new PrintWriter(fout);
            this.logfileName = logfileName;
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
            suppressErrorMsgs = true;
            return false;
        }

        // setup the reader which will read the output of the process
        PipedOutputStream ostream = new PipedOutputStream();
        try {
            PipedInputStream istream = new PipedInputStream(ostream);
            processReader = new BufferedReader(new InputStreamReader(istream));
        } catch (IOException e) {
            System.out.println("Unable to create pipe to process output: "+e.getMessage());
            suppressErrorMsgs = true;
            return false;
        }

        // setup and start the process
        if (additionalCommandLineArgs.length() > 0)
            command = command + " " + additionalCommandLineArgs;
            process = new ExecProcess(command, null, null, ostream, ostream);
        System.out.println("  Starting process: "+command);
        if (process == null) {
            suppressErrorMsgs = true;
            return false;
        }

        long startTime = System.currentTimeMillis();
        process.start();

        // wait for process to start before returning
        if (!readProcessOutputUntilReady()) {
            process = null;
            return false;
        }
        long duration = System.currentTimeMillis() - startTime;
        if (duration > 60000) 
            System.out.println("  external process is ready, took "+Infrastructure.getElapsedTime(duration));
        return true;
    }

    /**
     * For children of this class to see if the process is currently running
     * @return true if it is, false if it is not running.
     */
    protected boolean isProcessRunning() { return (process == null) ? false : true; }

    /**
     * Sets the simulator's interactive prompt. If there is a %% in the
     * String, it will be replaced by the current command number.
     * @param prompt the prompt
     */
    protected void setPrompt(String prompt) { outputPrompt = prompt; }

    /**
     * Issues a command to the process. This method will block until the
     * process has finished processing the command, hit a $stop or breakpoint, and is
     * waiting for another interactive command.  It will return if the process exits.
     * @param command the command to send to the process.
     */
    protected void issueCommand(String command) {
        issueCommand(command, true);
    }
    /**
     * Issues a command to the process. This method will block until the
     * process has finished processing the command, hit a stop or breakpoint, and is
     * waiting for another interactive command.  It will return if the process exits.
     * @param command the command to send to the process.
     * @param incrPrompt true if this command will increment the interactive prompt
     */
    protected void issueCommand(String command, boolean incrPrompt) {
        if (!incrPrompt) currentCommand--;

        if (process == null) {
            if (!suppressErrorMsgs) {
                System.out.println("Error: command "+command+" issue when no "+simulatorName+" process running!");
                numIssueCommandErrors++;
                if (numIssueCommandErrors > 5) {
                    System.out.println("Warning: too many command issue errors, suppressing all remaining errors");
                    suppressErrorMsgs = true;
                }
            }
            return;
        }
        if (command == null) return;
        if (DEBUGPROCESS) System.out.println("Running command "+command);
        //if (command.indexOf("get_node_info") == -1)
        //    System.out.println("Running command "+command);
        process.writeln(command);
        if (logfile != null) {
            logfile.println(command);
            logfile.flush();
        }
        // don't worry about readProcessOutputUntilReady returning false on '$finish',
        // as process will just exit and the method will return false.
        if (command.equals(quitCommand)) {
            readProcessOutputUntilReady();
            process = null;
            return;
        }
        // read output until simulator is ready.  If there was an error, end simulation.
        if (!readProcessOutputUntilReady()) {
            process.writeln(quitCommand);
            readProcessOutputUntilReady();
            Infrastructure.exit(1);
        }
    }

    /**
     * Get the output of Verilog caused by the last command issued to verilog.
     * @return a StringBuffer containing the output from the last command.
     */
    StringBuffer getLastCommandOutput() {
        StringBuffer buf = new StringBuffer();
        buf.append(lastCommandOutput);
        return buf;
    }

    /**
     * Read the output of the Verilog process until it is ready to receive more input.
     * This only works when running verilog in interactive mode, because this waits
     * for the next prompt.  This blocks until the Verilog process is ready to read more input.
     * @return true if ready, false on error
     */
            private final int bufsize = 256 * 1024 * 4;
            private char [] cbuf = new char[bufsize];
    private boolean readProcessOutputUntilReady() {
        /**
         * Note this function is quite complicated because the prompt is not followed by a new line.
         * Therefore we cannot use readLine(), but must instead just use read().  Because the prompt
         * we are trying to match, "C# > " may be split across two reads, we need to concatenate bits
         * from both the previous and current read and check that as well.
         */
        if (processReader == null) {
            return false;
        }
        boolean ready = true;

        // this will hold the some of the chars we read from the verilog process' stdout

        int offset = 0;
        int len = bufsize;
        int read = 0;
        StringBuffer lastString = new StringBuffer();
        // make the entire set of chars sent to stdout from the command
        // available for devices to parse
        lastCommandOutput = new StringBuffer();
        try {
            while ((read = processReader.read(cbuf, offset, len)) > 0) {
                if (logfile != null) {
                    logfile.write(cbuf, offset, read);
                    logfile.flush();
                }
                lastCommandOutput.append(cbuf, offset, read);

                // grab the last line in the char buffer - we need to save this for the next read
                if (read > 0) {
                    int i;
                    for (i=read-1; i>=0; i--) {
                        if (cbuf[i] == '\n') {
                            lastString = new StringBuffer();
                            i++;        // move past \n
                            lastString.append(cbuf, i, read-i);
                            if (DEBUGOUTPUT) System.out.println("Setting last line to: "+lastString);
                            break;
                        }
                    }
                    if (i < 0) {
                        // no \n found, append this to last known line
                        if (DEBUGOUTPUT) {
                            StringBuffer sb = new StringBuffer();
                            sb.append(cbuf, 0, read);
                            System.out.println("No end-line found, appending "+sb);
                        }
                        lastString.append(cbuf, 0, read);
                    }
                }
                // error checking
                StringBuffer errbuf = new StringBuffer();
                errbuf.append(cbuf, 0, read);
                if (errbuf.indexOf(errorFlag) != -1 || lastString.indexOf(errorFlag) != -1) {
                    System.out.println("Error in "+simulatorName+" simulation, aborting. Please check log file "+logfileName);
                    ready = false;
                    // command # is not incremented on errors, so subtract. Unless this is an error
                    // before the first command, in which case the command number remains 1.
                    if (currentCommand > 1) currentCommand--;
                }

                // check if lastString matches input prompt
                String prompt = getPrompt();
                if (lastString.toString().indexOf(prompt) != -1) {
                    currentCommand++;
                    if (DEBUGOUTPUT)
                        System.out.println("Matched: "+lastString);
                    return ready;
                }
                // sometimes process writes out info after writing
                // out the prompt (looks like threading issue), in
                // this case there is no last line (chars not ending in \n),
                // so we should check the entire thing for a prompt
                // this happens for nanosim sometimes
                //if (lastString.length() == 0) {
                    if (lastCommandOutput.toString().indexOf(prompt) != -1) {
                        currentCommand++;
                        if (DEBUGOUTPUT)
                            System.out.println("Matched: "+lastCommandOutput);
                        return ready;
                    }
                //}
                if (DEBUGOUTPUT) System.out.println("Last line read from process: "+lastString);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    private String getPrompt() {
        return outputPrompt.replaceAll("%%", String.valueOf(currentCommand));
    }

}
