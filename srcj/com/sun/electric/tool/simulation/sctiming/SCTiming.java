/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SCTiming.java
 * Written by Jonathan Gainsley, Sun Microsystems.
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation.sctiming;

import com.sun.electric.tool.io.input.spicenetlist.SpiceNetlistReader;
import com.sun.electric.tool.io.input.spicenetlist.SpiceSubckt;
import com.sun.electric.tool.user.MessagesStream;
import com.sun.electric.tool.user.Exec;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Netlist;

import java.util.*;
import java.io.*;

/**
 * Runs a characterization of a gate.
 * User: gainsley
 * Date: Oct 30, 2006
 */
public class SCTiming {


    /**
     * This holds the param names to be swept, and their values.
     * Typically this is the input drive strength, output load strength,
     * and clock buffer strength.
     */
    private static class SweepParam {
        String param;
        String[] sweep;
        SweepParam(String param, String sweep) {
            this.param = param;
            this.sweep = sweep.trim().split("\\s+");
        }
    }

    private String inputFile = null;
    private Cell   topCell = null;
    public String topCellName = null;
    private String topCellNameLiberty = null;
    public String outputDir = ".";
    private String topCellParams = "";

    public SCSettings settings;

    // parameter names that will be swept or measured
    private String inbufStr = "inbufStr";
    private String outloadStr = "outloadStr";
    private String clkbufStr = "clkbufStr";
    private String setupTimeName = "tmsetup";
    private String setupTimeSweep = "tmsetupsweep";
    private String holdTimeSweep = "tmholdsweep";
    private String holdTimeName = "tmhold";
    private String clk2q = "clk2q";
    private String setupclk2q = "setupclk2q";

    private SpiceSubckt dutSubckt;
    private SpiceSubckt bufferSubckt;
    private SpiceSubckt clkbufSubckt;
    private SpiceSubckt loadSubckt;

    private List<Arc> timingArcs;
    private List<SweepParam> sweeps;
    private Map<String,String> combinationalFunctions;
    private FlipFlopFunction functionFlipFlop = null;
    private FlipFlopFunctionSDRtoDDR functionFlipFlopSDRtoDDR = null;
    private FlipFlopFunctionDDRtoSDR functionFlipFlopDDRtoSDR = null;
    private LatchFunction functionLatch = null;
    private TestCell testCell = null;
    private PrintWriter out;
    private PrintStream msg = System.out;
    private SpiceNetlistReader netlistReader = null;
    private boolean interfaceTiming = false;
    private List<String> ignorableSubckts;

    private boolean verbose = true;
    private boolean printStatistics = true;
    public boolean characterizationFailed = false;
    private boolean useAutoStop = true;
    private boolean noTiming = false;

    private static final String lineComment = "*****************************************************";

    /**
     * Create a new SCTiming object to run timing characterizations.
     * New Arcs need to be created and specified, and then added
     * to the SCTiming object.  SCTiming.characterize() is then
     * called to run all the timing arcs and write the resulting
     * data to a Liberty file.
     */
    public SCTiming() {
        timingArcs = new ArrayList<Arc>();
        sweeps = new ArrayList<SweepParam>();
        combinationalFunctions = new HashMap<String, String>();
        ignorableSubckts = new ArrayList<String>();
    }

    /* *******************************************************
     * Settings
     * *******************************************************/

    /**
     * Set the settings for this characterization
     * @param settings
     */
    public void setSettings(SCSettings settings) { this.settings = settings; }

    /**
     * Set the input spice netlist file. This file
     * should contain the top cell subckt, the input buffer
     * subckt, and the output load subckt. It should also contain
     * the clock buffer subkct if running a sequential test.
     * @param inputFile the input file path
     */
    public void setInputFile(String inputFile) { this.inputFile = inputFile; }

    /**
     * Set the top cell. This is the cell that
     * will be characterized. All pins defined on arcs should be pins
     * on this cell.
     * @param topCell
     */
    public void setTopCell(Cell topCell) { this.topCell = topCell; }

    /**
     * Set the top cell name. This is the name of the subckt that
     * will be characterized. All pins defined on arcs should be pins
     * on this cell.
     * @param topCellName
     */
    public void setTopCellName(String topCellName) { this.topCellName = topCellName; }

    /**
     * Set the top cell name for the Liberty file.
     * @param topCellName
     */
    public void setTopCellNameLiberty(String topCellName) { this.topCellNameLiberty = topCellName; }

    /**
     * A string of name=value parameter values for the top cell instance,
     * if needed.
     * @param list space separated list of name=value pairs
     */
    public void setTopCellParams(String list) { this.topCellParams = list; }

    /**
     * Set the output directory.  All files will be written to this directory.
     * @param dir
     */
    public void setOutputDir(String dir) { this.outputDir = dir; }

    /**
     * Add a timing arc to be characterized
     * @param arc
     */
    public void addTimingArc(Arc arc) { timingArcs.add(arc); }

    /**
     * Set the function for an output pin in terms of other pins.
     * For example: A & B, A + B, !A
     * @param outputPin the output pin name
     * @param function a string describing the function.
     */
    public void setFunctionCombinational(String outputPin, String function) {
        combinationalFunctions.put(outputPin, function);
    }

    /**
     * Set the function as a simple flip flop. Note this model does not support DDR flops.
     * @param outputPosPin output positive (true) pin
     * @param outputNegPin output negative (false) pin
     * @param inputPin input data pin
     * @param clockedOnPin clock pin
     */
    public void setFunctionFlipFlop(String outputPosPin, String outputNegPin, String inputPin, String clockedOnPin) {
        functionFlipFlop = new FlipFlopFunction(outputPosPin, outputNegPin,
                inputPin, clockedOnPin, false);
        combinationalFunctions.put(outputPosPin, "i"+outputPosPin);
    }

    /**
     * Set the function as a simple DDR flip flop.
     * @param outputPosPin output positive (true - Q) pin (this output changes on both pos and neg edge clk)
     * @param outputNegPin output negative (false - Q bar) pin (this output changes on both pos and neg edge clk)
     * @param inputPin input data pin
     * @param clockedOnPin clock pin
     */
    public void setFunctionFlipFlopDDR(String outputPosPin, String outputNegPin, String inputPin, String clockedOnPin) {
        functionFlipFlop = new FlipFlopFunction(outputPosPin, outputNegPin,
                inputPin, clockedOnPin, true);
        combinationalFunctions.put(outputPosPin, "i"+outputPosPin);
    }

    /**
     * Set the function as a simple SDR to DDR flop
     * @param outputPosPin output positive (Q) pin
     * @param outputNegPin output negative (Q-bar) pin
     * @param inputRise input clocked on the rising edge of the clk
     * @param inputFall input clocked on the falling edge of the clk
     * @param clockedOnPin the clock pin
     */
    public void setFunctionFlipFlopSDRtoDDR(String outputPosPin, String outputNegPin, String inputRise, String inputFall, String clockedOnPin) {
        functionFlipFlopSDRtoDDR = new FlipFlopFunctionSDRtoDDR(outputPosPin, outputNegPin, inputRise, inputFall, clockedOnPin);
        combinationalFunctions.put(outputPosPin, "i"+outputPosPin);
    }

    /**
     * Set the function as a simple DDR to SDR flop
     * @param outputRise the output that changes on clock rising
     * @param outputFall the output that changes on clock falling
     * @param inputPin the input pin
     * @param clockedOnPin the clock pin
     */
    public void setFunctionFlipFlopDDRtoSDR(String outputRise, String outputFall, String inputPin, String clockedOnPin) {
        functionFlipFlopDDRtoSDR = new FlipFlopFunctionDDRtoSDR(outputRise, outputFall, inputPin, clockedOnPin);
        combinationalFunctions.put(outputRise, "i"+outputRise);
        combinationalFunctions.put(outputFall, "i"+outputFall);
    }


    /**
     * Set the function as a latch.
     * @param outputPosPin output positive (true) pin
     * @param outputNegPin output negative (false) pin
     * @param inputPin input data pin
     * @param enablePin enables in to out
     */
    public void setFunctionLatch(String outputPosPin, String outputNegPin, String inputPin, String enablePin) {
        functionLatch = new LatchFunction(outputPosPin, outputNegPin, inputPin, enablePin);
        combinationalFunctions.put(outputPosPin, "i"+outputPosPin);
    }

    /**
     * Set the function of the cell as an abstracted macro model.
     * See documentation for interface timing. This allows modelling of
     * complex sequential macro blocks.
     */
    public void setFunctionInterfaceTiming() {
        interfaceTiming = true;
    }

    /**
     * Set the test cell block for blocks with scan
     * @param scanInPin scan in pin
     * @param scanOutPin scan out pin
     * @param scanEnPin scan en pin
     */
    public void setTestCell(String scanInPin, String scanOutPin, String scanEnPin) {
        testCell = new TestCell(scanInPin, scanOutPin, scanEnPin);
    }

    /**
     * Tells spice to use .option autostop, which stops a transient simulation once (all?)
     * .measurement statements have completed.
     * May not work well if it doesn't wait for all, or in cases of optimizations
     * @param t true to set autostop on, false to not set it.
     */
    public void setAutoStop(boolean t) {
        useAutoStop = t;
    }

    /**
     * Add an ignorable subckt. The characterization code checks that all subckts are
     * defined, but sometimes spice models use subckts for transistors, which are
     * defined in the model file. This suppresses the error for subckts that are not
     * defined.
     * @param subcktName the name of the subckt
     */
    public void addIgnorableSubckt(String subcktName) {
        ignorableSubckts.add(subcktName);
    }

    /**
     * For debug only, turn off timing runs
     */
    public void setNoTimingMode() {
        noTiming = true;
    }

    /* *******************************************************
     * Characterization
     * *******************************************************/

    /**
     * Run characterization on all timing arcs added to this SCTiming object,
     * and write the resulting data to a Liberty file.
     * @return true on success, false on error
     */
    public boolean characterize(SCRunBase.DelayType delayType) {
        try {
            characterize_(delayType);
        } catch (SCTimingException e) {
            System.out.println("SCTiming: Exception: "+e.getMessage());
            characterizationFailed = true;
            return false;
        }
        return true;
    }
    public void characterize_(SCRunBase.DelayType delayType) throws SCTimingException {
        // redirect standard output
        MessagesStream.getMessagesStream().save((new File(outputDir, "SCTiming.log")).getPath());

        err(inputFile == null, "Input spice file not specified");
        err(topCellName == null, "Top (Test) cell not specified");
        err(timingArcs.size() == 0, "No timing arcs specified");

        boolean sequentialTest = false;
        for (Arc a : timingArcs) {
            if (a.clk != null) sequentialTest = true;
        }

        settings.checkSettings(sequentialTest);

        msg.println("--------------------------------------------------------");
        msg.println("Characterization:");
        msg.println("   Cell \""+topCellName+"\"");
        msg.println("   "+new Date(System.currentTimeMillis()));

        msg.println();
        msg.println("--------------------------------------------------------");
        msg.println("Reading spice netlist '"+inputFile+"'...");
        netlistReader = new SpiceNetlistReader();
        try {
            netlistReader.readFile(inputFile, false);
        } catch (java.io.FileNotFoundException e) {
            throw new SCTimingException(e.getMessage());
        }
        msg.println();
        msg.println("Spice netlist read complete. Checking netlist...");

        // check for the device to be characterized (device under test)
        dutSubckt = netlistReader.getSubckt(topCellName);
        err(dutSubckt == null, "Test Cell "+topCellName+" not found in spice netlist");

        // check for the buffer subckt
        bufferSubckt = netlistReader.getSubckt(settings.bufferCell);
        err(bufferSubckt == null, "Buffer cell "+settings.bufferCell+" not found in spice netlist");
        err(bufferSubckt.getParamValue(settings.bufferCellStrengthParam) == null,
                "Strength param "+settings.bufferCellStrengthParam+" not found in buffer cell "+settings.bufferCell);
        err(!bufferSubckt.hasPort(settings.bufferCellInputPort),
                "Input port "+settings.bufferCellInputPort+" not found in buffer cell "+settings.bufferCell);
        err(!bufferSubckt.hasPort(settings.bufferCellOutputPort),
                "Output port "+settings.bufferCellOutputPort+" not found in buffer cell "+settings.bufferCell);

        // check for the load subckt
        loadSubckt = netlistReader.getSubckt(settings.loadCell);
        err(loadSubckt == null, "Load cell "+settings.loadCell+" not found in spice netlist");
        err(loadSubckt.getParamValue(settings.loadCellStrengthParam) == null,
                "Strength param "+settings.loadCellStrengthParam+" not found in load cell "+settings.loadCell);
        err(!loadSubckt.hasPort(settings.loadCellPort),
                "Load port "+settings.loadCellPort+" not found in load cell "+settings.loadCell);

        // if this is a sequential test, the clock buffer subckt must be present
        clkbufSubckt = null;
        if (sequentialTest) {
            clkbufSubckt = netlistReader.getSubckt(settings.clkBufferCell);
            err(clkbufSubckt == null, "Clock buffer cell"+settings.clkBufferCell+" not found in spice netlist");
            err(clkbufSubckt.getParamValue(settings.clkBufferCellStrengthParam) == null,
                     "Strength param "+settings.clkBufferCellStrengthParam+" not found in clock buffer cell "+settings.clkBufferCell);
            err(!clkbufSubckt.hasPort(settings.clkBufferCellInputPort),
                    "Input port "+settings.clkBufferCellInputPort+" not found in buffer cell "+settings.clkBufferCell);
            err(!clkbufSubckt.hasPort(settings.clkBufferCellOutputPort),
                    "Output port "+settings.clkBufferCellOutputPort+" not found in buffer cell "+settings.clkBufferCell);
        }
        msg.println("   Netlist OK");

        // verify ports
        for (Arc arc : timingArcs) {
            verifyPorts(topCell, dutSubckt, arc, netlistReader.getGlobalNets());
        }

        if (noTiming) return;
        
        // Run timing for each arc
        for (Arc arc : timingArcs) {
            if (arc.clk == null) {
                runCombinational(arc);
            } else {
                if (settings.simpleSequentialCharacterization)
                    runSequentialSimple(arc, delayType);
                else
                    runSequential(arc);
            }
        }
    }

    /**
     * Write a common header to the spice file
     * @param out the spice file output writer
     */
    private void writeHeader(PrintWriter out) {
        out.println(lineComment);
        out.println("* Date: "+new Date(System.currentTimeMillis()));
        out.println("* Written by Electric "+ Version.getVersion());
        out.println(lineComment);
        out.println();
        writeCommentHeader("Spice Netlist");
        out.println(".include '"+inputFile+"'");
        out.println();
        writeCommentHeader("Options");
        out.println("* VDD = " + settings.vdd);
        out.println("* temp = " + settings.temp);
        out.println("* Input ramp time (ps) = "+settings.inputRampTimePS);
        out.println("* Sim resolution time (ps) = "+settings.simResolutionPS);
        out.println("* Sim duration (ps) = "+settings.simTimePS);
        out.println("* input low threshold (%) = "+settings.inputLow);
        out.println("* input high threshold (%) = "+settings.inputHigh);
        out.println("* input delay threshold (%) = "+settings.inputDelayThresh);
        out.println("* output low threshold (%) = "+settings.outputLow);
        out.println("* output high threshold (%) = "+settings.outputHigh);
        out.println("* output delay threshold (%) = "+settings.outputDelayThresh);
        out.println("* edge cap measure start (%) = "+settings.edgePercentForCapStart);
        out.println("* edge cap measure end (%) = "+settings.edgePercentForCapEnd);
        out.println("* setup time range min (ps) = "+settings.tmsetupMinGuessPS);
        out.println("* setup time range guess (ps) = "+settings.tmsetupGuessPS);
        out.println("* setup time range max (ps) = "+settings.tmsetupMaxGuessPS);
        out.println(lineComment);
        out.println();
        writeCommentHeader("Option statements");
        out.println(".option post");
        out.println(".option optlst=1");
        if (useAutoStop) out.println(".option autostop");
        out.println();
        writeCommentHeader("Power Supplies");
        out.println(".global vdd gnd");
        out.println(".param vsupply="+settings.vdd);
        //out.println("Vdd vdd gnd vsupply");
        out.println(".temp "+settings.temp);
        out.println();
    }

    /**
     * Run a combinational timing arc. This generates a spice deck,
     * runs spice, reads the resulting measure statements, and
     * saves the data for writing to the Liberty file.
     * @param arc the timing arc to run
     * @throws SCTimingException on error
     */
    private void runCombinational(Arc arc) throws SCTimingException {
        String arcDesc = arc.toString();
        String outputFileName = topCellName + "_delay_"+arcDesc;
        File outputFile = new File(outputDir, outputFileName+".sp");
        msg.println();
        msg.println("--------------------------------------------------------");
        msg.println("Characterizing timing arc \""+arcDesc+"\":");
        msg.println();
        msg.println("Writing spice netlist to");
        msg.println("   '"+outputFile.getPath()+"'...");
        try {
            out = new PrintWriter(new FileOutputStream(outputFile));
        } catch (java.io.FileNotFoundException e) {
            throw new SCTimingException(e.getMessage());
        }
        out.println("* "+arc.toString()+" *");
        writeHeader(out);
        out.println(lineComment);
        out.println("* Delay Arc");
        out.print("* "); out.println(arcDesc);
        out.println(lineComment);
        out.println();
        writeCommentHeader("Parameters");
        // these parameters are swept during transient sim, so it does not
        // matter what they are set to here
        out.println(".param "+inbufStr+"=1");
        out.println(".param "+outloadStr+"=1");
        out.println(".param "+clkbufStr+"=1");
        out.println();
        writeCommentHeader("Test Bench");
        // write stable voltage source(s)
        for (PinEdge in : arc.stableInputs) {
            writeVoltSource(in);
        }
        // write input voltage source
        writeVoltSource(arc.input);
        // write buffer
        writeBuffer(arc.input, bufferSubckt);
        // write voltage sources to measure current (for capacitance test)
        out.println("Vcurrent_in "+arc.input.pin+" "+arc.input.pin+"_i 0");
        out.println("Vcurrent_out "+arc.output.pin+" "+arc.output.pin+"_i 0");
        // instantiate the test cell
        out.print("Xdut ");
        for (String port : dutSubckt.getPorts()) {
            if (port.equalsIgnoreCase(arc.input.pin) || port.equalsIgnoreCase(arc.output.pin))
                out.print(port+"_i ");
            else
                out.print(port+" ");
        }
        out.print(topCellName);
        out.println(" "+topCellParams);
        // write load
        writeLoad(arc.output, loadSubckt);
        writeIC(arc.output);
        out.println();

        sweeps.clear();
        String outputLoads = settings.loadCellSweep;
        String inputBuffers = settings.bufferCellSweep;
        if (arc.getOutputLoadSweep() != null) outputLoads = arc.getOutputLoadSweep();
        if (arc.getInputBufferSweep() != null) inputBuffers = arc.getInputBufferSweep();
        sweeps.add(new SweepParam(inbufStr, inputBuffers));
        sweeps.add(new SweepParam(outloadStr, outputLoads));

        // simulation
        writeCommentHeader("Transient statement");
        out.println(".tran "+settings.simResolutionPS+"ps "+settings.simTimePS+"ps SWEEP DATA = DATA_TIM");
        out.println();
        // write all the parameter values to be swept
        writeSweepData();
        out.println();

        writeCommentHeader("Measure statements");
        String inputSlew = arc.input.pin+"_slew";
        String propDelay = "prop_delay";
        String outputSlew = arc.output.pin+"_slew";
        String inputCap = arc.input.pin+"_cap";
        String outputCap = arc.output.pin+"_cap";
        writeMeasDelay(propDelay, arc.input, arc.output);
        writeMeasSlew(inputSlew, arc.input, settings.inputLow, settings.inputHigh);
        writeMeasSlew(outputSlew, arc.output, settings.outputLow, settings.outputHigh);
        writeMeasCap(inputCap, arc.input, "Vcurrent_in");
        writeMeasCap(outputCap, arc.output, "Vcurrent_out");
        out.println();

        out.println(".END");
        out.close();
        msg.println("   Finished writing netlist.");

        // run spice
        runSpice(outputFileName, true);

        // read output file
        File mt0file = new File(outputDir, outputFileName+".mt0");
        msg.println("Reading measurements file "+outputFileName+".mt0...");
        TableData data = TableData.readSpiceMeasResults(mt0file.getPath());
        arc.data = data;
        arc.data2d_inbuf_outload = data.getTable2D(inbufStr.toLowerCase(), outloadStr.toLowerCase(), null, null);
        if (verbose) {
            msg.println();
            msg.println("   Read data:");
            data.printData();
            if (arc.data2d_inbuf_outload != null)
                arc.data2d_inbuf_outload.print();
        }
        if (printStatistics) {
            msg.println();
            msg.println("Statistical checks:");
            msg.println("-------------------");
            msg.println();
            msg.println("Input slew should depend only on input buffer strength:");
            printRowMeanStdDev(arc.data2d_inbuf_outload, arc.input.pin+"_slew", msg, 1e12, "ps");
            msg.println();
            msg.println("Output capacitance should depend only on output load size");
            printColumnMeanStdDev(arc.data2d_inbuf_outload, arc.output.pin+"_cap", msg, 1e15, "fF");
            msg.println();
            msg.println("Input capacitance should not depend on either input buffer strength or output load:");
            printMeanStdDev(arc.data2d_inbuf_outload, arc.input.pin+"_cap", msg, 1e15, "fF");

        }
        msg.println();
        msg.println("Characterization of arc \""+arc+"\" complete.");
    }

    /**
     * Run a sequential timing arc.  This extracts setup and clock-to-Q times
     * for the specified sweeps.  Note that resulting data is stored
     * on the Arc object.
     * @param arc the timing arc to characterize
     * @throws SCTimingException
     */
    private void runSequential(Arc arc) throws SCTimingException {

        String arcDesc = arc.toString();

        // First, we get ideal clk to q delay wrt to clk slew
        // This will provide a reference which will be used to evaluate setup times
        msg.println();
        msg.println("--------------------------------------------------------");
        msg.println("Characterizing timing arc \""+arcDesc+"\":");
        msg.println();
        msg.println("Finding minimum setup, hold, and clk-to-Q time for \""+arcDesc+"\"");

        String bufStrSweep = settings.bufferCellSweep;
        String loadStrSweep = settings.loadCellSweep;
        if (arc.getInputBufferSweep() != null) bufStrSweep = arc.getInputBufferSweep();
        if (arc.getOutputLoadSweep() != null) loadStrSweep = arc.getOutputLoadSweep();

        String [] bufStrs = bufStrSweep.trim().split("\\s+");
        String [] loadStrs = loadStrSweep.trim().split("\\s+");
        String [] clkStrs = settings.clkBufferCellSweep.trim().split("\\s+");

        err(bufStrs.length == 0, "Buffer cell strength values empty");
        err(loadStrs.length == 0, "Load cell strength values empty");
        err(clkStrs.length == 0, "Clock buffer cell strength values empty");

        String fileName = topCellName + "_setup_" + arcDesc;
        String fileNameHold = topCellName + "_hold_" + arcDesc;

        // max number of optimization iterations performed by spice
        int maxSpiceIterations = 30;
        // all result data will be stored in this table
        TableData allData = null;

        boolean runHold = true;
        // We need data results for every combination
        // not all results will vary wrt to each parameter
        for (int c=0; c<clkStrs.length; c++) {
            for (int l=0; l<loadStrs.length; l++) {
                for (int b=0; b<bufStrs.length; b++) {
                    // Run a spice optimization to find the minimum setup time
                    // at which clk-to-Q can be measured.  Clk-to-Q measurement
                    // cannot fail for the next spice optimization run.
                    TableData data = runSetupTimeSpiceTest(fileName, arc, bufStrs[b], clkStrs[c], loadStrs[l],
                            settings.tmsetupMinGuessPS, settings.tmsetupMaxGuessPS, settings.tmsetupGuessPS,
                            verbose, maxSpiceIterations, 0);
                    double tmsetupValidminPS = data.getValue(0, setupTimeSweep);
                    err(Double.isNaN(tmsetupValidminPS), "Error running spice simulation to get minimum setup time");
                    // scale back to ps
                    tmsetupValidminPS *= 1e12;
                    // add 1% to add some margin
                    tmsetupValidminPS = tmsetupValidminPS + Math.abs(0.1*tmsetupValidminPS);
                    if (verbose) {
                        msg.println();
                        msg.println("Bufstr="+bufStrs[b]+", Clkstr="+clkStrs[c]+", Loadstr="+loadStrs[l]);
                        msg.println("Using minimum setup time of "+tmsetupValidminPS+"ps");
                        msg.println();
                    }
                    // guess must be greater than or equal to min of range
                    double tmsetupPS = settings.tmsetupGuessPS;
                    if (tmsetupPS < tmsetupValidminPS) {
                        tmsetupPS = tmsetupValidminPS;
                    }
                    // Run a spice optimization test to find the minimum
                    // value of setup+clk2q.  This value will determine
                    // our setup and clk2q values.
                    data = runSetupTimeSpiceTest(fileName+"_1", arc, bufStrs[b], clkStrs[c], loadStrs[l],
                            tmsetupValidminPS, settings.tmsetupMaxGuessPS, tmsetupPS,
                            verbose, maxSpiceIterations, 1);
                    if (verbose) {
                        msg.println();
                        data.printData();
                        msg.println();
                    }
                    if (data.getNumRows() == maxSpiceIterations) {
                        throw new SCTimingException("Max number of iterations of optimization in spice reached, result may not be valid");
                    }
                    double tmholdValidMaxPS = 1e-12;
                    if (runHold) {
                        // Run a spice optimization to find the maximum hold time
                        // at which clk-to-Q can be measured.
                        TableData dataHold = runHoldTimeSpiceTest(fileNameHold, arc, bufStrs[b], clkStrs[c], loadStrs[l],
                                settings.tmsetupMinGuessPS, settings.tmsetupMaxGuessPS, settings.tmsetupGuessPS,
                                verbose, maxSpiceIterations);
                        tmholdValidMaxPS = dataHold.getValue(0, holdTimeSweep);
                        err(Double.isNaN(tmholdValidMaxPS), "Error running spice simulation to get maximum hold time");
                        if (verbose) {
                            msg.println();
                            msg.println("Bufstr="+bufStrs[b]+", Clkstr="+clkStrs[c]+", Loadstr="+loadStrs[l]);
                            msg.println("Hold time is "+(tmholdValidMaxPS*1e12)+"ps");
                            msg.println();
                        }
                    }
                    TableData latchClk2Q = null;
                    if (functionLatch != null) {
                        latchClk2Q = runLatchClk2QSpiceTest(fileName+"_q", arc, bufStrs[b], clkStrs[c],
                                loadStrs[l], verbose);
                    }
                    // record results into allData object
                    if (allData == null) {
                        List<String> headers = new ArrayList<String>();
                        headers.add(inbufStr);
                        headers.add(clkbufStr);
                        headers.add(outloadStr);
                        headers.add(holdTimeName);
                        headers.addAll(data.getHeaders());
                        allData = new TableData(headers);
                    }
                    if (data.getNumRows() > 0) {
                        double [] datarow = data.getRow(data.getNumRows()-1);
                        double [] newrow = new double[datarow.length+4];
                        newrow[0] = Double.parseDouble(bufStrs[b]);
                        newrow[1] = Double.parseDouble(clkStrs[c]);
                        newrow[2] = Double.parseDouble(loadStrs[l]);
                        newrow[3] = tmholdValidMaxPS;
                        for (int i=4; i<newrow.length; i++) {
                            newrow[i] = datarow[i-4];
                        }
                        if (latchClk2Q != null) {
                            // replace clk-to-Q value
                            int cc = allData.getHeaders().indexOf("clk2q");
                            int cc2 = latchClk2Q.getHeaders().indexOf("clk2q");
                            newrow[cc] = latchClk2Q.getRow(latchClk2Q.getNumRows()-1)[cc2];
                        }
                        allData.addRow(newrow);
                    }
                }
            }
        }
        msg.println();
        arc.data = allData;
        Table2D data2d = allData.getTable2D(inbufStr.toLowerCase(), outloadStr.toLowerCase(), null, null);
        Table2D data2d_clkbuf_outload = allData.getTable2D(clkbufStr.toLowerCase(), outloadStr.toLowerCase(),
                inbufStr.toLowerCase(), settings.bufferCellSweepExcludeFromAveraging);
        Table2D data2d_inbuf_clkbuf = allData.getTable2D(inbufStr.toLowerCase(), clkbufStr.toLowerCase(),
                outloadStr.toLowerCase(), settings.loadCellSweepExcludeFromAveraging);
        arc.data2d_inbuf_outload = data2d;
        arc.data2d_clkbuf_outload = data2d_clkbuf_outload;
        arc.data2d_inbuf_clkbuf = data2d_inbuf_clkbuf;
        if (verbose) {
            allData.printData();
            System.out.println("----------------------------------------------------------------------------");
            System.out.println("----------------------------------------------------------------------------");
            System.out.println("-------------------- Input Slew vs Output Load Table2D ---------------------");
            System.out.println("----------------------------------------------------------------------------");
            System.out.println("----------------------------------------------------------------------------");
            data2d.print();
            System.out.println("----------------------------------------------------------------------------");
            System.out.println("----------------------------------------------------------------------------");
            System.out.println("-------------------- Clock Slew vs Output Load Table2D ---------------------");
            System.out.println("----------------------------------------------------------------------------");
            System.out.println("----------------------------------------------------------------------------");
            data2d_clkbuf_outload.print();
            System.out.println("----------------------------------------------------------------------------");
            System.out.println("----------------------------------------------------------------------------");
            System.out.println("-------------------- Input Slew vs Clk Slew Table2D ------------------------");
            System.out.println("----------------------------------------------------------------------------");
            System.out.println("----------------------------------------------------------------------------");
            data2d_inbuf_clkbuf.print();
            msg.println();
        }
        if (printStatistics) {
            msg.println("Statistical checks:");
            msg.println("-------------------");
            msg.println();

            msg.println("Clock-to-Q should depend only on clock buffer and output load size:");
            //printColumnMeanStdDev(data2dClk, clk2q, msg, 1e12, "ps");
            print2DMeanStdDev(data2d_clkbuf_outload, clk2q, msg, 1e12, "ps");
            msg.println();

            msg.println("Setup time should depend only on input buffer strength and clock buffer strength:");
            //printRowMeanStdDev(data2dClk, setupTimeName, msg, 1e12, "ps");
            print2DMeanStdDev(data2d_inbuf_clkbuf, setupTimeName, msg, 1e12, "ps");
            msg.println();

            msg.println("Input slew should depend only on input buffer strength:");
            printRowMeanStdDev(data2d, arc.input.pin+"_slew", msg, 1e12, "ps");
            msg.println();

            msg.println("Output capacitance should depend only on output load size");
            printColumnMeanStdDev(data2d, arc.output.pin+"_cap", msg, 1e15, "fF");
            msg.println();

            msg.println("Input capacitance should not be dependent on either input slew or output load");
            printMeanStdDev(data2d, arc.input.pin+"_cap", msg, 1e15, "fF");
            msg.println();

            msg.println("Hold time should depend only on input buffer strength and clock buffer strength:");
            //printRowMeanStdDev(data2dClk, holdTimeName, msg, 1e12, "ps");
            print2DMeanStdDev(data2d_inbuf_clkbuf, holdTimeName, msg, 1e12, "ps");
            msg.println();
        }
        msg.println();
        msg.println("Characterization of arc \""+arc+"\" complete.");

    }

    /**
     * This runs one of two spice optimizations.  The first
     * varies the setup time to find the minimum setup time at which
     * the output is valid (i.e., a clock-to-Q measurement will not
     * return "failed").  The second test varies the setup time to
     * find the setup time which produces the minimum setup+clk2q time.
     * The second test requires that the clock-to-Q measurement never fails,
     * which is why the first optimization is run first.
     * @param outputFileName the output spice file name (no extension)
     * @param arc the arc to characterize
     * @param inputStr the input buffer strength
     * @param clkStr the clock buffer strength
     * @param outputLoad output load strength
     * @param tmsetupminPS min possible value of setup time
     * @param tmsetupmaxPS max possible value of setup time
     * @param tmsetupguessPS starting point of setup time
     * @param verbose true to print messages
     * @param maxSpiceIterations max number of spice iterations for the
     * second optimization phase
     * @param optphase 0 for finding min valid setup time, 1 for finding
     * min of setup+clk2q
     * @return the resulting measure data
     * @throws SCTimingException
     */
    private TableData runSetupTimeSpiceTest(String outputFileName, Arc arc,
                                            String inputStr, String clkStr,
                                            String outputLoad,
                                            double tmsetupminPS, double tmsetupmaxPS, double tmsetupguessPS,
                                            boolean verbose, int maxSpiceIterations,
                                            int optphase) throws SCTimingException {
        String arcDesc = arc.toString();
        if (verbose) {
            msg.println();
            msg.println("Writing spice netlist to:");
            msg.println("   '"+outputFileName+".sp'...");
            msg.println("   in directory: "+outputDir);
        }
        try {
            out = new PrintWriter(new FileOutputStream(new File(outputDir, outputFileName+".sp")));
        } catch (java.io.FileNotFoundException e) {
            throw new SCTimingException(e.getMessage());
        }
        out.println("* "+arc.toString()+" *");
        writeHeader(out);
        out.println(lineComment);
        out.println("* Setup Time plus Clock-to-Q measurement");
        out.print("* "); out.println(arcDesc);
        out.println(lineComment);
        out.println();
        writeCommentHeader("Parameters");
        out.println(".param "+inbufStr+"="+inputStr);
        out.println(".param "+outloadStr+"="+outputLoad);
        out.println(".param "+clkbufStr+"="+clkStr);
        out.println(".param "+setupTimeSweep+"=0ps");
        out.println();
        writeCommentHeader("Test Bench");
        // write stable voltage sources
        for (PinEdge stable : arc.stableInputs) {
            writeVoltSource(stable);
        }
        // input and clk buffers
        writeBuffer(arc.input, bufferSubckt);
        writeClkBuffer(arc.clk, clkbufSubckt);
        writeVoltSource(arc.input);
        writeVoltSource(arc.clk, setupTimeSweep);
        if (arc.clkFalse != null) {
            writeClkBuffer(arc.clkFalse, clkbufSubckt);
            writeVoltSource(arc.clkFalse, setupTimeSweep);
        }
        // device under test
        out.println("Vcurrent_in "+arc.input.pin+" "+arc.input.pin+"_i 0");
        out.println("Vcurrent_out "+arc.output.pin+" "+arc.output.pin+"_i 0");
        out.println("Vcurrent_clk "+arc.clk.pin+" "+arc.clk.pin+"_i 0");
        out.print("Xdut ");
        for (String port : dutSubckt.getPorts()) {
            if (port.equalsIgnoreCase(arc.input.pin) || port.equalsIgnoreCase(arc.output.pin) ||
                    port.equalsIgnoreCase(arc.clk.pin))
                out.print(port+"_i ");
            else
                out.print(port+" ");
        }
        out.print(topCellName);
        out.println(" "+topCellParams);
        // write load
        writeLoad(arc.output, loadSubckt);
        writeIC(arc.output);
        for (PinEdge ic : arc.initialConditions) {
            writeIC(new PinEdge("Xdut."+ic.pin, ic.stableVoltage));
        }

        out.println();
        writeCommentHeader("Measure statements");
        String inputSlew = arc.input.pin+"_slew";
        String clkslew = arc.clk.pin+"_slew";
        String outputSlew = arc.output.pin+"_slew";
        String inputCap = arc.input.pin+"_cap";
        String clkCap = arc.clk.pin+"_cap";
        String outputCap = arc.output.pin+"_cap";
        writeMeasSlew(clkslew, arc.clk, settings.inputLow, settings.inputHigh);
        writeMeasSlew(inputSlew, arc.input, settings.inputLow, settings.inputHigh);
        writeMeasSlew(outputSlew, arc.output, settings.outputLow, settings.outputHigh);
        writeMeasCap(clkCap, arc.clk, "Vcurrent_clk");
        writeMeasCap(inputCap, arc.input, "Vcurrent_in");
        writeMeasCap(outputCap, arc.output, "Vcurrent_out");
        writeMeasDelay(clk2q, arc.clk, arc.output, "0");
        writeMeasDelay(setupTimeName, arc.input, arc.clk);
        out.println(".meas TRAN "+setupclk2q+" PARAM='"+clk2q+"+"+setupTimeName+"' goal=0");
        out.println();

        sweeps.clear();

        writeCommentHeader("Transient simulation");
        out.println("*.tran "+settings.simResolutionPS+"ps "+settings.simTimePS+
                    "ps SWEEP "+setupTimeSweep+" LIN 20 "+tmsetupminPS+"ps "+tmsetupmaxPS+"ps");
        out.println(".param "+setupTimeSweep+"=optrange("+tmsetupguessPS+"ps, "+
                    tmsetupminPS+"ps, "+tmsetupmaxPS+"ps)");
        if (optphase == 0) {
            // in optimization phase zero, we use passfail to
            // find the minimum setup time that yields a non-failing
            // clk-to-Q measurement
            out.println(".model optmod OPT method=passfail");
            out.println(".tran "+settings.simResolutionPS+"ps "+settings.simTimePS+
                    "ps SWEEP OPTIMIZE=optrange RESULTS="+clk2q+" MODEL=optmod");
        } else {
            // in optimization phase one, we use bisection to
            // find the minimum of setup plus clk-to-Q
            out.println(".model optmod OPT itropt="+maxSpiceIterations+" relin=0.001 relout=0.001");
            out.println(".tran "+settings.simResolutionPS+"ps "+settings.simTimePS+
                    "ps SWEEP OPTIMIZE=optrange RESULTS="+setupclk2q+" MODEL=optmod");
        }

        // transient simulation
        out.println();
        out.println(".END");
        out.close();
        if (verbose) {
            msg.println("   Finished writing netlist.");
        }

        runSpice(outputFileName, verbose);

        File mt0file = new File(outputDir, outputFileName+".mt0");
        if (verbose) {
            msg.println("Reading measurements file "+mt0file.getName()+"...");
        }
        return TableData.readSpiceMeasResults(mt0file.getPath());
    }

    /**
     * Flops latch and copy data to the output on a single edge, which
     * means we can use one spice run to get both setup and clk-to-Q times.
     * However, latches copy data on the rising edge, and latch on the falling
     * edge (for transparent-when-high latches), so we need separate runs to
     * determine setup and clock-to-Q times. The method for getting setup time
     * for flops is used to get setup time for latches, and this method is
     * used to get the clock-to-Q time for the latch
     * @param outputFileName the output spice file name (no extension)
     * @param arc the arc to characterize
     * @param inputStr the input buffer strength
     * @param clkStr the clock buffer strength
     * @param outputLoad output load strength
     * @return the resulting measure data
     * @throws SCTimingException
     */
    private TableData runLatchClk2QSpiceTest(String outputFileName, Arc arc,
                                            String inputStr, String clkStr,
                                            String outputLoad,
                                            boolean verbose) throws SCTimingException {
        String arcDesc = arc.toString();
        if (verbose) {
            msg.println();
            msg.println("Writing spice netlist to:");
            msg.println("   '"+outputFileName+".sp'...");
            msg.println("   in directory: "+outputDir);
        }
        try {
            out = new PrintWriter(new FileOutputStream(new File(outputDir, outputFileName+".sp")));
        } catch (java.io.FileNotFoundException e) {
            throw new SCTimingException(e.getMessage());
        }
        out.println("* "+arc.toString()+" *");
        writeHeader(out);
        out.println(lineComment);
        out.println("* Latch Clock-to-Q measurement");
        out.print("* "); out.println(arcDesc);
        out.println(lineComment);
        out.println();
        writeCommentHeader("Parameters");
        out.println(".param "+inbufStr+"="+inputStr);
        out.println(".param "+outloadStr+"="+outputLoad);
        out.println(".param "+clkbufStr+"="+clkStr);
        out.println(".param "+setupTimeSweep+"=0ps");
        out.println();
        writeCommentHeader("Test Bench");
        // write stable voltage sources
        for (PinEdge stable : arc.stableInputs) {
            writeVoltSource(stable);
        }
        // input and clk buffers
        //writeBuffer(arc.input, bufferSubckt);
        writeClkBuffer(arc.clk, clkbufSubckt);
        writeVoltSource(arc.input.getFinalState());
        writeVoltSource(arc.clk.getOpposite(), setupTimeSweep);
        if (arc.clkFalse != null) {
            writeClkBuffer(arc.clkFalse, clkbufSubckt);
            writeVoltSource(arc.clkFalse.getOpposite(), setupTimeSweep);
        }
        // device under test
        out.println("Vcurrent_in "+arc.input.pin+" "+arc.input.pin+"_i 0");
        out.println("Vcurrent_out "+arc.output.pin+" "+arc.output.pin+"_i 0");
        out.println("Vcurrent_clk "+arc.clk.pin+" "+arc.clk.pin+"_i 0");
        out.print("Xdut ");
        for (String port : dutSubckt.getPorts()) {
            if (port.equalsIgnoreCase(arc.input.pin) || port.equalsIgnoreCase(arc.output.pin) ||
                    port.equalsIgnoreCase(arc.clk.pin))
                out.print(port+"_i ");
            else
                out.print(port+" ");
        }
        out.print(topCellName);
        out.println(" "+topCellParams);
        // write load
        writeLoad(arc.output, loadSubckt);
        writeIC(arc.output);
        for (PinEdge ic : arc.initialConditions) {
            writeIC(new PinEdge("Xdut."+ic.pin, ic.stableVoltage));
        }

        out.println();
        writeCommentHeader("Measure statements");
        String inputSlew = arc.input.pin+"_slew";
        String clkslew = arc.clk.pin+"_slew";
        String outputSlew = arc.output.pin+"_slew";
        String inputCap = arc.input.pin+"_cap";
        String clkCap = arc.clk.pin+"_cap";
        String outputCap = arc.output.pin+"_cap";
        writeMeasSlew(clkslew, arc.clk, settings.inputLow, settings.inputHigh);
        writeMeasSlew(inputSlew, arc.input, settings.inputLow, settings.inputHigh);
        writeMeasSlew(outputSlew, arc.output, settings.outputLow, settings.outputHigh);
        writeMeasCap(clkCap, arc.clk, "Vcurrent_clk");
        writeMeasCap(inputCap, arc.input, "Vcurrent_in");
        writeMeasCap(outputCap, arc.output, "Vcurrent_out");
        writeMeasDelay(clk2q, arc.clk, arc.output, "0");
        writeMeasDelay(setupTimeName, arc.input, arc.clk);
        out.println(".meas TRAN "+setupclk2q+" PARAM='"+clk2q+"+"+setupTimeName+"' goal=0");
        out.println();

        sweeps.clear();

        writeCommentHeader("Transient simulation");
        out.println(".tran "+settings.simResolutionPS+"ps "+settings.simTimePS+"ps");

        // transient simulation
        out.println();
        out.println(".END");
        out.close();
        if (verbose) {
            msg.println("   Finished writing netlist.");
        }

        runSpice(outputFileName, verbose);

        File mt0file = new File(outputDir, outputFileName+".mt0");
        if (verbose) {
            msg.println("Reading measurements file "+mt0file.getName()+"...");
        }
        return TableData.readSpiceMeasResults(mt0file.getPath());
    }

    /**
     * This runs one of two spice optimizations.  The first
     * varies the setup time to find the minimum setup time at which
     * the output is valid (i.e., a clock-to-Q measurement will not
     * return "failed").  The second test varies the setup time to
     * find the setup time which produces the minimum setup+clk2q time.
     * The second test requires that the clock-to-Q measurement never fails,
     * which is why the first optimization is run first.
     * @param outputFileName the output spice file name (no extension)
     * @param arc the arc to characterize
     * @param inputStr the input buffer strength
     * @param clkStr the clock buffer strength
     * @param outputLoad output load strength
     * @param tmholdminPS min possible value of setup time
     * @param tmholdmaxPS max possible value of setup time
     * @param tmholdguessPS starting point of setup time
     * @param verbose true to print messages
     * @param maxSpiceIterations max number of spice iterations for the
     * second optimization phase
     * @return the resulting measure data
     * @throws SCTimingException timing exception
     */
    private TableData runHoldTimeSpiceTest(String outputFileName, Arc arc,
                                            String inputStr, String clkStr,
                                            String outputLoad,
                                            double tmholdminPS, double tmholdmaxPS, double tmholdguessPS,
                                            boolean verbose, int maxSpiceIterations
                                            ) throws SCTimingException {
        int optphase = 0;
        String arcDesc = arc.toString();
        if (verbose) {
            msg.println();
            msg.println("Writing spice netlist to:");
            msg.println("   '"+outputFileName+".sp'...");
            msg.println("   in directory: "+outputDir);
        }
        try {
            out = new PrintWriter(new FileOutputStream(new File(outputDir, outputFileName+".sp")));
        } catch (java.io.FileNotFoundException e) {
            throw new SCTimingException(e.getMessage());
        }
        out.println("* "+arc.toString()+" *");
        writeHeader(out);
        out.println(lineComment);
        out.println("* Hold Time measurement");
        out.print("* "); out.println(arcDesc);
        out.println(lineComment);
        out.println();
        writeCommentHeader("Parameters");
        out.println(".param "+inbufStr+"="+inputStr);
        out.println(".param "+outloadStr+"="+outputLoad);
        out.println(".param "+clkbufStr+"="+clkStr);
        out.println(".param "+holdTimeSweep+"=0ps");
        out.println();
        writeCommentHeader("Test Bench");
        // write stable voltage sources
        for (PinEdge stable : arc.stableInputs) {
            writeVoltSource(stable);
        }
        // input and clk buffers
        writeBuffer(arc.input, bufferSubckt);
        writeClkBuffer(arc.clk, clkbufSubckt);
        writeVoltSource(arc.input, holdTimeSweep);
        writeVoltSource(arc.clk);
        if (arc.clkFalse != null) {
            writeClkBuffer(arc.clkFalse, clkbufSubckt);
            writeVoltSource(arc.clkFalse);
        }
        // device under test
        out.println("Vcurrent_in "+arc.input.pin+" "+arc.input.pin+"_i 0");
        out.println("Vcurrent_out "+arc.output.pin+" "+arc.output.pin+"_i 0");
        out.println("Vcurrent_clk "+arc.clk.pin+" "+arc.clk.pin+"_i 0");
        out.print("Xdut ");
        for (String port : dutSubckt.getPorts()) {
            if (port.equalsIgnoreCase(arc.input.pin) || port.equalsIgnoreCase(arc.output.pin) ||
                    port.equalsIgnoreCase(arc.clk.pin))
                out.print(port+"_i ");
            else
                out.print(port+" ");
        }
        out.print(topCellName);
        out.println(" "+topCellParams);
        // write load
        writeLoad(arc.output, loadSubckt);
        if (functionFlipFlop != null) {
            writeIC(arc.output.getOpposite());
            for (PinEdge ic : arc.initialConditions) {
                ic = ic.getOpposite();
                writeIC(new PinEdge("Xdut."+ic.pin, settings.vdd - ic.stableVoltage));
            }
        } else {
            writeIC(arc.output);
            for (PinEdge ic : arc.initialConditions) {
                writeIC(new PinEdge("Xdut."+ic.pin, ic.stableVoltage));
            }
        }

        out.println();
        writeCommentHeader("Measure statements");
        String inputSlew = arc.input.pin+"_slew";
        String clkslew = arc.clk.pin+"_slew";
        String outputSlew = arc.output.pin+"_slew";
        String inputCap = arc.input.pin+"_cap";
        String clkCap = arc.clk.pin+"_cap";
        String outputCap = arc.output.pin+"_cap";
        writeMeasSlew(clkslew, arc.clk, settings.inputLow, settings.inputHigh);
        writeMeasSlew(inputSlew, arc.input, settings.inputLow, settings.inputHigh);
        writeMeasSlew(outputSlew, arc.output.getOpposite(), settings.outputLow, settings.outputHigh);
        writeMeasCap(clkCap, arc.clk, "Vcurrent_clk");
        writeMeasCap(inputCap, arc.input, "Vcurrent_in");
        writeMeasCap(outputCap, arc.output.getOpposite(), "Vcurrent_out");
        writeMeasDelay(clk2q, arc.clk, arc.output.getOpposite(), "0");
        writeMeasDelay(holdTimeName, arc.clk, arc.input);
        out.println();

        sweeps.clear();

        writeCommentHeader("Transient simulation");
        out.println("*.tran "+settings.simResolutionPS+"ps "+settings.simTimePS+
                    "ps SWEEP "+holdTimeSweep+" LIN 20 "+tmholdminPS+"ps "+tmholdmaxPS+"ps");
        out.println(".param "+holdTimeSweep+"=optrange("+tmholdguessPS+"ps, "+
                    tmholdminPS+"ps, "+tmholdmaxPS+"ps)");
        if (optphase == 0) {
            // in optimization phase zero, we use passfail to
            // find the maximum hold time that yields a non-failing
            // clk-to-Q measurement
            out.println(".model optmod OPT method=passfail");
            out.println(".tran "+settings.simResolutionPS+"ps "+settings.simTimePS+
                    "ps SWEEP OPTIMIZE=optrange RESULTS="+clk2q+" MODEL=optmod");
        } else {
            // in optimization phase one, we use bisection to
            // find the minimum of setup plus clk-to-Q
            out.println(".model optmod OPT itropt="+maxSpiceIterations+" relin=0.001 relout=0.001");
            out.println(".tran "+settings.simResolutionPS+"ps "+settings.simTimePS+
                    "ps SWEEP OPTIMIZE=optrange RESULTS="+setupclk2q+" MODEL=optmod");
        }

        // transient simulation
        out.println();
        out.println(".END");
        out.close();
        if (verbose) {
            msg.println("   Finished writing netlist.");
        }

        runSpice(outputFileName, verbose);

        File mt0file = new File(outputDir, outputFileName+".mt0");
        if (verbose) {
            msg.println("Reading measurements file "+mt0file.getName()+"...");
        }
        return TableData.readSpiceMeasResults(mt0file.getPath());
    }

    /**
     * Run a sequential timing arc (simple version).  This extracts setup and clock-to-Q times
     * for the specified sweeps.  Note that resulting data is stored
     * on the Arc object.
     * @param arc the timing arc to characterize
     * @param delayType the delay type
     * @throws SCTimingException thrown for an exception
     */
    private void runSequentialSimple(Arc arc, SCRunBase.DelayType delayType) throws SCTimingException {

        String arcDesc = arc.toString();

        msg.println();
        msg.println("--------------------------------------------------------");
        msg.println("Characterizing timing arc \""+arcDesc+"\":");
        msg.println();
        msg.println("Finding setup, hold, and clk-to-Q time for \""+arcDesc+"\"");

        String bufStrSweep = settings.bufferCellSweep;
        String loadStrSweep = settings.loadCellSweep;
        String loadStrSweepSetupHold = settings.loadCellSweepForSetupHold;
        String clkStrSweep = settings.clkBufferCellSweep;
        if (delayType == SCRunBase.DelayType.MIN) {
            bufStrSweep = settings.bufferCellSweepMinTime;
            loadStrSweep = settings.loadCellSweepMinTime;
            loadStrSweepSetupHold = settings.loadCellSweepForSetupHoldMinTime;
            clkStrSweep = settings.clkBufferCellSweepMinTime;
        }

        if (arc.getInputBufferSweep() != null) bufStrSweep = arc.getInputBufferSweep();
        if (arc.getOutputLoadSweep() != null) {
            loadStrSweep = arc.getOutputLoadSweep();
            loadStrSweepSetupHold = arc.getOutputLoadSweep();
        }

        String [] bufStrs = bufStrSweep.trim().split("\\s+");
        String [] loadStrs = loadStrSweep.trim().split("\\s+");
        String [] loadStrsSetupHold = loadStrSweepSetupHold.trim().split("\\s+");
        String [] clkStrs = clkStrSweep.trim().split("\\s+");

        err(bufStrs.length == 0, "Buffer cell strength values empty");
        err(loadStrs.length == 0, "Load cell strength values empty");
        err(loadStrsSetupHold.length == 0, "Load cell strength values for Setup and Hold empty");
        err(clkStrs.length == 0, "Clock buffer cell strength values empty");

        String fileNameClk2q = topCellName + "_clk2q_" + arcDesc;
        String fileNameSetup = topCellName + "_setup_" + arcDesc;
        String fileNameHold = topCellName + "_hold_" + arcDesc;

        // max number of optimization iterations performed by spice
        int maxSpiceIterations = 30;
        // all result data will be stored in this table
        TableData setupHoldData = null;

        boolean runHold = true;

        // find clk2q for stable input, wrt to load and clk rise time
        TableData clk2qData = null;
        for (int c=0; c<clkStrs.length; c++) {
            TableData data = runSimpleClk2Q(fileNameClk2q, arc, clkStrs[c], loadStrSweep,
                    settings.tmsetupMaxGuessPS, verbose);
            if (clk2qData == null) {
                List<String> headers = new ArrayList<String>();
                headers.add(clkbufStr);
                headers.add(outloadStr);
                headers.addAll(data.getHeaders());
                clk2qData = new TableData(headers);
            }
            for (int row=0; row<data.getNumRows(); row++) {
                double [] datarow = data.getRow(row);
                double [] newrow = new double[datarow.length+2];
                newrow[0] = Double.parseDouble(clkStrs[c]);
                newrow[1] = Double.parseDouble(loadStrs[row]);
                for (int i=2; i<newrow.length; i++) {
                    newrow[i] = datarow[i-2];
                }
                clk2qData.addRow(newrow);
            }
        }
        Table2D data2d_clkbuf_outload = clk2qData.getTable2D(clkbufStr.toLowerCase(), outloadStr.toLowerCase(), null, null);

        // now find setup time that pushes out clk2q by specified amount
        for (int c=0; c<clkStrs.length; c++) {
            for (int l=0; l<loadStrsSetupHold.length; l++) {
                for (int b=0; b<bufStrs.length; b++) {
                    // Run a spice optimization to find the specified clk2q pushout
                    TableData data = runSimpleSetupTimeSpiceTest(fileNameSetup, arc, bufStrs[b], clkStrs[c], loadStrsSetupHold[l],
                            settings.tmsetupMinGuessPS, settings.tmsetupMaxGuessPS, settings.tmsetupGuessPS,
                            verbose, maxSpiceIterations);
                    if (verbose) {
                        msg.println();
                        data.printData();
                        msg.println();
                    }
                    if (data.getNumRows() == maxSpiceIterations) {
                        throw new SCTimingException("Max number of iterations of optimization in spice reached, result may not be valid");
                    }
                    double tmholdValidMaxPS = 1e-12;
                    if (runHold) {
                        // Run a spice optimization to find the maximum hold time
                        // at which clk-to-Q can be measured.
                        TableData dataHold = runHoldGlitchTimeSpiceTest(fileNameHold, arc, bufStrs[b], clkStrs[c], loadStrs[l],
                                settings.tmsetupMinGuessPS, settings.tmsetupMaxGuessPS, settings.tmsetupGuessPS,
                                verbose, maxSpiceIterations);
                        tmholdValidMaxPS = dataHold.getValue(0, holdTimeName);
                        err(Double.isNaN(tmholdValidMaxPS), "Error running spice simulation to get maximum hold time");
                        if (verbose) {
                            msg.println();
                            msg.println("Bufstr="+bufStrs[b]+", Clkstr="+clkStrs[c]+", Loadstr="+loadStrs[l]);
                            msg.println("Hold time is "+(tmholdValidMaxPS*1e12)+"ps");
                            msg.println();
                        }
                    }
                    TableData latchClk2Q = null;
                    if (functionLatch != null) {
                        latchClk2Q = runLatchClk2QSpiceTest(fileNameSetup+"_q", arc, bufStrs[b], clkStrs[c],
                                loadStrs[l], verbose);
                    }
                    // record results into allData object
                    if (setupHoldData == null) {
                        List<String> headers = new ArrayList<String>();
                        headers.add(inbufStr);
                        headers.add(clkbufStr);
                        headers.add(outloadStr);
                        headers.add(holdTimeName);
                        headers.addAll(data.getHeaders());
                        setupHoldData = new TableData(headers);
                    }
                    if (data.getNumRows() > 0) {
                        double [] datarow = data.getRow(data.getNumRows()-1);
                        double [] newrow = new double[datarow.length+4];
                        newrow[0] = Double.parseDouble(bufStrs[b]);
                        newrow[1] = Double.parseDouble(clkStrs[c]);
                        newrow[2] = Double.parseDouble(loadStrs[l]);
                        newrow[3] = tmholdValidMaxPS;
                        for (int i=4; i<newrow.length; i++) {
                            newrow[i] = datarow[i-4];
                        }
                        if (latchClk2Q != null) {
                            // replace clk-to-Q value
                            int cc = setupHoldData.getHeaders().indexOf("clk2q");
                            int cc2 = latchClk2Q.getHeaders().indexOf("clk2q");
                            newrow[cc] = latchClk2Q.getRow(latchClk2Q.getNumRows()-1)[cc2];
                        }
                        setupHoldData.addRow(newrow);
                    }
                }
            }
        }
        msg.println();
        arc.data = setupHoldData;
        Table2D data2d_inbuf_clkbuf = setupHoldData.getTable2D(inbufStr.toLowerCase(), clkbufStr.toLowerCase(),
                null, null);
        arc.data2d_inbuf_outload = null;
        arc.data2d_clkbuf_outload = data2d_clkbuf_outload;
        arc.data2d_inbuf_clkbuf = data2d_inbuf_clkbuf;
        if (verbose) {
            clk2qData.printData();
            setupHoldData.printData();
            System.out.println("----------------------------------------------------------------------------");
            System.out.println("----------------------------------------------------------------------------");
            System.out.println("-------------------- Clock Slew vs Output Load Table2D ---------------------");
            System.out.println("----------------------------------------------------------------------------");
            System.out.println("----------------------------------------------------------------------------");
            data2d_clkbuf_outload.print();
            System.out.println("----------------------------------------------------------------------------");
            System.out.println("----------------------------------------------------------------------------");
            System.out.println("-------------------- Input Slew vs Clk Slew Table2D ------------------------");
            System.out.println("----------------------------------------------------------------------------");
            System.out.println("----------------------------------------------------------------------------");
            data2d_inbuf_clkbuf.print();
            msg.println();
        }
        if (printStatistics) {
            msg.println("Statistical checks:");
            msg.println("-------------------");
            msg.println();

            msg.println("Clock-to-Q should depend only on clock buffer and output load size:");
            //printColumnMeanStdDev(data2dClk, clk2q, msg, 1e12, "ps");
            print2DMeanStdDev(data2d_clkbuf_outload, clk2q, msg, 1e12, "ps");
            msg.println();

            msg.println("Pushed-out Clock-to-Q should depend only on clock buffer and output load size:");
            printColumnMeanStdDev(data2d_inbuf_clkbuf, clk2q, msg, 1e12, "ps");
            msg.println();

            msg.println("Setup time should depend only on input buffer strength and clock buffer strength:");
            //printRowMeanStdDev(data2dClk, setupTimeName, msg, 1e12, "ps");
            print2DMeanStdDev(data2d_inbuf_clkbuf, setupTimeName, msg, 1e12, "ps");
            msg.println();

            msg.println("Input slew should depend only on input buffer strength:");
            printRowMeanStdDev(data2d_inbuf_clkbuf, arc.input.pin+"_slew", msg, 1e12, "ps");
            msg.println();

            msg.println("Output capacitance should depend only on output load size");
            printColumnMeanStdDev(data2d_clkbuf_outload, arc.output.pin+"_cap", msg, 1e15, "fF");
            msg.println();

            msg.println("Input capacitance should not be dependent on either input slew or output load");
            printMeanStdDev(data2d_inbuf_clkbuf, arc.input.pin+"_cap", msg, 1e15, "fF");
            msg.println();

            msg.println("Hold time should depend only on input buffer strength and clock buffer strength:");
            //printRowMeanStdDev(data2dClk, holdTimeName, msg, 1e12, "ps");
            print2DMeanStdDev(data2d_inbuf_clkbuf, holdTimeName, msg, 1e12, "ps");
            msg.println();
        }
        msg.println();
        msg.println("Characterization of arc \""+arc+"\" complete.");
    }

    /**
     * Get the clk2q given a stable input, while varying the clk slew and output load.
     * Please note that the output load string passed to this function should be the
     * full set of values, i.e. "1 2 3 4 5 6". The clkStr should be one value, such as "1".
     * @param outputFileName output file name
     * @param arc arc to characterize
     * @param clkStr clock strength to use
     * @param outputLoadSweep list of output loads to use, of the form "1 2 3 4 5 6"
     * @param clkPulseTime the time after simulation starts that the clock is pulsed
     * @param verbose true to print out messages
     * @return Table of measure results
     * @throws SCTimingException
     */
    private TableData runSimpleClk2Q(String outputFileName, Arc arc,
                                            String clkStr,
                                            String outputLoadSweep,
                                            double clkPulseTime,
                                            boolean verbose ) throws SCTimingException {
        String [] loads = outputLoadSweep.split("\\s+");
        if (loads.length == 0)
            throw new SCTimingException("No output load sweep in runSimpleClk2q");

        String arcDesc = arc.toString();
        if (verbose) {
            msg.println();
            msg.println("Writing spice netlist to:");
            msg.println("   '"+outputFileName+".sp'...");
            msg.println("   in directory: "+outputDir);
        }
        try {
            out = new PrintWriter(new FileOutputStream(new File(outputDir, outputFileName+".sp")));
        } catch (java.io.FileNotFoundException e) {
            throw new SCTimingException(e.getMessage());
        }
        out.println("* "+arc.toString()+" *");
        writeHeader(out);
        out.println(lineComment);
        out.println("* Clock-to-Q measurement");
        out.print("* "); out.println(arcDesc);
        out.println(lineComment);
        out.println();
        writeCommentHeader("Parameters");
        out.println(".param "+outloadStr+"="+loads[0]);
        out.println(".param "+clkbufStr+"="+clkStr);
        out.println(".param "+setupTimeSweep+"="+clkPulseTime+"ps");
        out.println();
        writeCommentHeader("Test Bench");
        // write stable voltage sources
        for (PinEdge stable : arc.stableInputs) {
            writeVoltSource(stable);
        }
        // input and clk buffers
        writeClkBuffer(arc.clk, clkbufSubckt);
        writeVoltSource(arc.input.getFinalState());
        writeVoltSource(arc.clk, setupTimeSweep);
        if (arc.clkFalse != null) {
            writeClkBuffer(arc.clkFalse, clkbufSubckt);
            writeVoltSource(arc.clkFalse, setupTimeSweep);
        }
        // device under test
        out.println("Vcurrent_in "+arc.input.pin+" "+arc.input.pin+"_i 0");
        out.println("Vcurrent_out "+arc.output.pin+" "+arc.output.pin+"_i 0");
        out.println("Vcurrent_clk "+arc.clk.pin+" "+arc.clk.pin+"_i 0");
        out.print("Xdut ");
        for (String port : dutSubckt.getPorts()) {
            if (port.equalsIgnoreCase(arc.input.pin) || port.equalsIgnoreCase(arc.output.pin) ||
                    port.equalsIgnoreCase(arc.clk.pin))
                out.print(port+"_i ");
            else
                out.print(port+" ");
        }
        out.print(topCellName);
        out.println(" "+topCellParams);
        // write load
        writeLoad(arc.output, loadSubckt);
        writeIC(arc.output);
        for (PinEdge ic : arc.initialConditions) {
            writeIC(new PinEdge("Xdut."+ic.pin, ic.stableVoltage));
        }

        out.println();
        writeCommentHeader("Measure statements");
        String clkslew = arc.clk.pin+"_slew";
        String outputSlew = arc.output.pin+"_slew";
        String clkCap = arc.clk.pin+"_cap";
        String outputCap = arc.output.pin+"_cap";
        writeMeasSlew(clkslew, arc.clk, settings.inputLow, settings.inputHigh);
        writeMeasSlew(outputSlew, arc.output, settings.outputLow, settings.outputHigh);
        writeMeasCap(clkCap, arc.clk, "Vcurrent_clk");
        writeMeasCap(outputCap, arc.output, "Vcurrent_out");
        writeMeasDelay(clk2q, arc.clk, arc.output, "0");
        out.println();

        sweeps.clear();

        writeCommentHeader("Transient simulation");
        out.println(".tran "+settings.simResolutionPS+"ps "+settings.simTimePS+
                    "ps SWEEP "+outloadStr+" POI "+loads.length+" "+outputLoadSweep);

        // transient simulation
        out.println();
        out.println(".END");
        out.close();
        if (verbose) {
            msg.println("   Finished writing netlist.");
        }

        runSpice(outputFileName, verbose);

        File mt0file = new File(outputDir, outputFileName+".mt0");
        if (verbose) {
            msg.println("Reading measurements file "+mt0file.getName()+"...");
        }
        return TableData.readSpiceMeasResults(mt0file.getPath());
    }

    /**
     * This runs one spice optimization. It varies the setup time to
     * find the setup time whereby the push out on clk2q reaches the specified value.
     * @param outputFileName the output spice file name (no extension)
     * @param arc the arc to characterize
     * @param inputStr the input buffer strength
     * @param clkStr the clock buffer strength
     * @param outputLoad output load strength
     * @param tmsetupminPS min possible value of setup time
     * @param tmsetupmaxPS max possible value of setup time
     * @param tmsetupguessPS starting point of setup time
     * @param verbose true to print messages
     * @param maxSpiceIterations max number of spice iterations for the
     * second optimization phase
     * @return the resulting measure data
     * @throws SCTimingException if there was an error
     */
    private TableData runSimpleSetupTimeSpiceTest(String outputFileName, Arc arc,
                                            String inputStr, String clkStr,
                                            String outputLoad,
                                            double tmsetupminPS, double tmsetupmaxPS, double tmsetupguessPS,
                                            boolean verbose, int maxSpiceIterations
                                            ) throws SCTimingException {
        String arcDesc = arc.toString();
        if (verbose) {
            msg.println();
            msg.println("Writing spice netlist to:");
            msg.println("   '"+outputFileName+".sp'...");
            msg.println("   in directory: "+outputDir);
        }
        try {
            out = new PrintWriter(new FileOutputStream(new File(outputDir, outputFileName+".sp")));
        } catch (java.io.FileNotFoundException e) {
            throw new SCTimingException(e.getMessage());
        }
        out.println("* "+arc.toString()+" *");
        writeHeader(out);
        out.println(lineComment);
        out.println("* Setup Time plus Clock-to-Q measurement");
        out.print("* "); out.println(arcDesc);
        out.println(lineComment);
        out.println();
        writeCommentHeader("Parameters");
        out.println(".param "+inbufStr+"="+inputStr);
        out.println(".param "+outloadStr+"="+outputLoad);
        out.println(".param "+clkbufStr+"="+clkStr);
        out.println(".param "+setupTimeSweep+"=0ps");
        out.println();
        writeCommentHeader("Test Bench");
        // write stable voltage sources
        for (PinEdge stable : arc.stableInputs) {
            writeVoltSource(stable);
        }
        // input and clk buffers
        writeBuffer(arc.input, bufferSubckt);
        writeClkBuffer(arc.clk, clkbufSubckt);
        writeVoltSource(arc.input, setupTimeSweep);
        writeVoltSource(arc.clk);
        if (arc.clkFalse != null) {
            writeClkBuffer(arc.clkFalse, clkbufSubckt);
            writeVoltSource(arc.clkFalse);
        }
        // device under test
        out.println("Vcurrent_in "+arc.input.pin+" "+arc.input.pin+"_i 0");
        out.println("Vcurrent_out "+arc.output.pin+" "+arc.output.pin+"_i 0");
        out.println("Vcurrent_clk "+arc.clk.pin+" "+arc.clk.pin+"_i 0");
        out.print("Xdut ");
        for (String port : dutSubckt.getPorts()) {
            if (port.equalsIgnoreCase(arc.input.pin) || port.equalsIgnoreCase(arc.output.pin) ||
                    port.equalsIgnoreCase(arc.clk.pin))
                out.print(port+"_i ");
            else
                out.print(port+" ");
        }
        out.print(topCellName);
        out.println(" "+topCellParams);
        // write load
        writeLoad(arc.output, loadSubckt);
        writeIC(arc.output);
        for (PinEdge ic : arc.initialConditions) {
            writeIC(new PinEdge("Xdut."+ic.pin, ic.stableVoltage));
        }

        out.println();
        writeCommentHeader("Measure statements");
        String inputSlew = arc.input.pin+"_slew";
        String clkslew = arc.clk.pin+"_slew";
        String outputSlew = arc.output.pin+"_slew";
        String inputCap = arc.input.pin+"_cap";
        String clkCap = arc.clk.pin+"_cap";
        String outputCap = arc.output.pin+"_cap";
        String pushout = "pushout";
        writeMeasSlew(clkslew, arc.clk, settings.inputLow, settings.inputHigh);
        writeMeasSlew(inputSlew, arc.input, settings.inputLow, settings.inputHigh);
        writeMeasSlew(outputSlew, arc.output, settings.outputLow, settings.outputHigh);
        writeMeasCap(clkCap, arc.clk, "Vcurrent_clk");
        writeMeasCap(inputCap, arc.input, "Vcurrent_in");
        writeMeasCap(outputCap, arc.output, "Vcurrent_out");
        writeMeasDelay(clk2q, arc.clk, arc.output);
        writeMeasDelay(setupTimeName, arc.input, arc.clk);
        writeMeasPushout(pushout, arc.output, settings.clk2qpushout);
        out.println();

        sweeps.clear();

        writeCommentHeader("Transient simulation");
        out.println("*.tran "+settings.simResolutionPS+"ps "+settings.simTimePS+
                    "ps SWEEP "+setupTimeSweep+" LIN 20 "+tmsetupminPS+"ps "+tmsetupmaxPS+"ps");
        out.println(".param "+setupTimeSweep+"=optrange("+tmsetupguessPS+"ps, "+
                    tmsetupminPS+"ps, "+tmsetupmaxPS+"ps)");
        // use spice's passfail bisection optimization to find best clk2q, then push that out by "clk2qpushout" ps
        out.println(".model optmod OPT method=passfail itropt="+maxSpiceIterations+" relin=0.0001 relout=0.001");
        out.println(".tran "+settings.simResolutionPS+"ps "+settings.simTimePS+
                "ps SWEEP OPTIMIZE=optrange RESULTS="+pushout+" MODEL=optmod");

        // transient simulation
        out.println();
        out.println(".END");
        out.close();
        if (verbose) {
            msg.println("   Finished writing netlist.");
        }

        runSpice(outputFileName, verbose);

        File mt0file = new File(outputDir, outputFileName+".mt0");
        if (verbose) {
            msg.println("Reading measurements file "+mt0file.getName()+"...");
        }
        return TableData.readSpiceMeasResults(mt0file.getPath());
    }

    /**
     * Finds the hold time by starting at a point that is clearly past the trigger
     * point, then moving backwards until it passes through the trigger point and
     * violates the hold time. Violation is defined as a glitch on the node after
     * the first pass gate (usually denoted as the master node).
     * @param outputFileName the output spice file name (no extension)
     * @param arc the arc to characterize
     * @param inputStr the input buffer strength
     * @param clkStr the clock buffer strength
     * @param outputLoad output load strength
     * @param tmholdminPS min possible value of setup time
     * @param tmholdmaxPS max possible value of setup time
     * @param tmholdguessPS starting point of setup time
     * @param verbose true to print messages
     * @param maxSpiceIterations max number of spice iterations for the
     * second optimization phase
     * @return the resulting measure data
     * @throws SCTimingException timing exception
     */
    private TableData runHoldGlitchTimeSpiceTest(String outputFileName, Arc arc,
                                            String inputStr, String clkStr,
                                            String outputLoad,
                                            double tmholdminPS, double tmholdmaxPS, double tmholdguessPS,
                                            boolean verbose, int maxSpiceIterations
                                            ) throws SCTimingException {
        int optphase = 0;
        String arcDesc = arc.toString();
        if (verbose) {
            msg.println();
            msg.println("Writing spice netlist to:");
            msg.println("   '"+outputFileName+".sp'...");
            msg.println("   in directory: "+outputDir);
        }
        try {
            out = new PrintWriter(new FileOutputStream(new File(outputDir, outputFileName+".sp")));
        } catch (java.io.FileNotFoundException e) {
            throw new SCTimingException(e.getMessage());
        }
        out.println("* "+arc.toString()+" *");
        writeHeader(out);
        out.println(lineComment);
        out.println("* Hold Time measurement");
        out.print("* "); out.println(arcDesc);
        out.println(lineComment);
        out.println();
        writeCommentHeader("Parameters");
        out.println(".param "+inbufStr+"="+inputStr);
        out.println(".param "+outloadStr+"="+outputLoad);
        out.println(".param "+clkbufStr+"="+clkStr);
        out.println(".param "+holdTimeSweep+"=0ps");
        out.println();
        writeCommentHeader("Test Bench");
        // write stable voltage sources
        for (PinEdge stable : arc.stableInputs) {
            writeVoltSource(stable);
        }
        // input and clk buffers
        writeBuffer(arc.input, bufferSubckt);
        writeClkBuffer(arc.clk, clkbufSubckt);
        writeVoltSource(arc.input, holdTimeSweep);
        writeVoltSource(arc.clk);
        if (arc.clkFalse != null) {
            writeClkBuffer(arc.clkFalse, clkbufSubckt);
            writeVoltSource(arc.clkFalse);
        }
        // device under test
        out.println("Vcurrent_in "+arc.input.pin+" "+arc.input.pin+"_i 0");
        out.println("Vcurrent_out "+arc.output.pin+" "+arc.output.pin+"_i 0");
        out.println("Vcurrent_clk "+arc.clk.pin+" "+arc.clk.pin+"_i 0");
        out.print("Xdut ");
        for (String port : dutSubckt.getPorts()) {
            if (port.equalsIgnoreCase(arc.input.pin) || port.equalsIgnoreCase(arc.output.pin) ||
                    port.equalsIgnoreCase(arc.clk.pin))
                out.print(port+"_i ");
            else
                out.print(port+" ");
        }
        out.print(topCellName);
        out.println(" "+topCellParams);
        // write load
        writeLoad(arc.output, loadSubckt);
        writeIC(new PinEdge("Xdut."+arc.glitchNode.pin, arc.glitchNode.getInitialState().transition));

        out.println();
        writeCommentHeader("Measure statements");
        String inputSlew = arc.input.pin+"_slew";
        String clkslew = arc.clk.pin+"_slew";
        String outputSlew = arc.output.pin+"_slew";
        String inputCap = arc.input.pin+"_cap";
        String clkCap = arc.clk.pin+"_cap";
        String outputCap = arc.output.pin+"_cap";
        String glitch = "glitch";
        writeMeasSlew(clkslew, arc.clk, settings.inputLow, settings.inputHigh);
        writeMeasSlew(inputSlew, arc.input, settings.inputLow, settings.inputHigh);
        writeMeasSlew(outputSlew, arc.output.getOpposite(), settings.outputLow, settings.outputHigh);
        writeMeasCap(clkCap, arc.clk, "Vcurrent_clk");
        writeMeasCap(inputCap, arc.input, "Vcurrent_in");
        writeMeasCap(outputCap, arc.output.getOpposite(), "Vcurrent_out");
        writeMeasDelay(clk2q, arc.clk, arc.output, "0");
        writeMeasDelay(holdTimeName, arc.clk, arc.input);
        writeMeasGlitch(glitch, arc.glitchNode);
        out.println();

        sweeps.clear();

        writeCommentHeader("Transient simulation");
        out.println("*.tran "+settings.simResolutionPS+"ps "+settings.simTimePS+
                    "ps SWEEP "+holdTimeSweep+" LIN 20 "+tmholdminPS+"ps "+tmholdmaxPS+"ps");
        out.println(".param "+holdTimeSweep+"=optrange("+tmholdguessPS+"ps, "+
                    tmholdminPS+"ps, "+tmholdmaxPS+"ps)");
        // use spice's passfail bisection optimization to find time when signal glitches
        out.println(".model optmod OPT method=passfail itropt="+maxSpiceIterations+" relin=0.0001 relout=0.001");
        out.println(".tran "+settings.simResolutionPS+"ps "+settings.simTimePS+
                "ps SWEEP OPTIMIZE=optrange RESULTS="+glitch+" MODEL=optmod");

        // transient simulation
        out.println();
        out.println(".END");
        out.close();
        if (verbose) {
            msg.println("   Finished writing netlist.");
        }

        runSpice(outputFileName, verbose);

        File mt0file = new File(outputDir, outputFileName+".mt0");
        if (verbose) {
            msg.println("Reading measurements file "+mt0file.getName()+"...");
        }
        return TableData.readSpiceMeasResults(mt0file.getPath());
    }


    private static class SpiceResultChecker extends OutputStream {
        private byte [] buf = new byte[256];
        private int count;
        private boolean failed = false;
        private OutputStream out;

        public SpiceResultChecker(OutputStream out) {
            this.out = out;
        }

        public void write(int b) throws IOException {
            if (b == '\n' || b == Character.LINE_SEPARATOR) {
                checkLine();
                count = 0;
            } else {
                if (count > buf.length) count = 0;
                buf[count] = (byte)b;
                count++;
            }
            if (out != null)
                out.write(b);
        }

        private void checkLine() {
            String line = new String(buf, 0, count);
            if (line.indexOf("***** hspice job aborted") != -1) {
                failed = true;
            }
        }

        private boolean getFailed() { return failed; }

        public void close() throws IOException { if (out != null) out.close(); }
        public void flush() throws IOException { if (out != null) out.flush(); }
    }

    /**
     * Runs an external spice job on the given file name,
     * returns when the external spice job has finished.
     * @param outputFileName the spice file (no path, no extension)
     * @param verbose true to print messages
     * @throws SCTimingException
     */
    private void runSpice(String outputFileName, boolean verbose) throws SCTimingException {
        String command = settings.simulator+ " "+outputFileName+".sp";
        if (verbose) {
            msg.println();
            msg.println("Running spice: "+command);
            msg.println("   In directory "+outputDir);
            msg.println("   Logging output to "+outputFileName+".out");
            msg.println("   "+new Date(System.currentTimeMillis()));
            msg.println();
        }
        OutputStream outlog = null;
        try {
            outlog = new BufferedOutputStream(new FileOutputStream(new File(outputDir, outputFileName+".out")));
        } catch (java.io.FileNotFoundException e) {
            throw new SCTimingException(e.getMessage());
        }
        SpiceResultChecker checker = new SpiceResultChecker(verbose ? System.out : null);
        Exec exec = new Exec(command, null, new File(outputDir), outlog, checker);
        exec.run();
        try {
            outlog.close();
        } catch (java.io.IOException e) {
            throw new SCTimingException(e.getMessage());
        }
        if (exec.getExitVal() != 0 || checker.getFailed()) {
            msg.println();
            msg.println("Spice job Aborted");
            msg.println("   "+new Date(System.currentTimeMillis()));
            throw new SCTimingException("Spice run failed, please check output file");
        }
        if (verbose) {
            msg.println();
            msg.println("Spice job completed");
            msg.println("   "+new Date(System.currentTimeMillis()));
            msg.println();
        }
    }

    private void writeCommentHeader(String msg) {
        out.println(lineComment);
        out.print("* "); out.println(msg);
        out.println(lineComment);
    }

    private void writeVoltSource(PinEdge edge) {
        writeVoltSource(edge, null);
    }
    private void writeVoltSource(PinEdge edge, String timeStartParam) {
        int numPeriods = 1;
        double vhigh = settings.vdd;
        double vlow = 0;
        switch(edge.transition) {
            case STABLE0: {
                out.println("V"+edge.pin+" "+edge.pin+" gnd 0");
                break;
            }
            case STABLE1: {
                out.println("V"+edge.pin+" "+edge.pin+" gnd vsupply");
                break;
            }
            case STABLEV: {
                out.println("V"+edge.pin+" "+edge.pin+" gnd "+edge.stableVoltage);
                break;
            }
            case RISE: {
                double temp = vlow;
                vlow = vhigh;
                vhigh = temp;
                // fall through to FALL case
            }
            case FALL: {
                out.println("V"+edge.pin+"_pre "+edge.pin+"_pre gnd ");
                out.print("+  PWL ");
                out.print(0.0+" "+vhigh+" ");
                double timeStart = settings.timeStartPS;
                if (timeStart + settings.tmsetupMinGuessPS < 0) // initial point would be at negative time
                    timeStart = -1.0*settings.tmsetupMinGuessPS;
                for (int i=0; i<numPeriods; i++) {
                    double t = timeStart + i*settings.periodPS;
                    if (timeStartParam != null) {
                        out.print("'"+timeStartParam+"+"+t+"ps' "+vhigh+" ");
                        out.print("'"+timeStartParam+"+"+(t+settings.inputRampTimePS)+"ps' "+vlow+" ");
                        //out.print("'"+timeStartParam+"+"+(t+periodPS/2)+"ps' "+vlow+" ");
                        //out.print("'"+timeStartParam+"+"+(t+periodPS/2+inputRampTimePS)+"ps' "+vhigh+" ");
                    } else {
                        out.print(t+"ps "+vhigh+" ");
                        out.print((t+settings.inputRampTimePS)+"ps "+vlow+" ");
                        //out.print((t+periodPS/2)+"ps "+vlow+" ");
                        //out.print((t+periodPS/2+inputRampTimePS)+"ps "+vhigh+" ");
                    }
                }
                out.print((settings.periodPS*numPeriods+settings.simTimePS)+"ps "+vlow);
                out.println();
                break;
            }
        }
    }

    private void writeIC(PinEdge edge) {
        out.print(".ic V("+edge.pin+") = ");
        switch(edge.transition) {
            case STABLE0:
            case RISE:
                out.println(0);
                break;
            case STABLE1:
            case FALL:
                out.println("vsupply");
                break;
            case STABLEV:
                out.println(edge.stableVoltage);
        }
    }

    private void writeBuffer(PinEdge edge, SpiceSubckt bufferSubckt) {
        if (isStable(edge.transition)) {
            return;  // already tied to gnd or vdd
        }
        out.print("Xbuf"+edge.pin);
        out.print(" ");
        for (String port : bufferSubckt.getPorts()) {
            if (port.equalsIgnoreCase(settings.bufferCellInputPort)) {
                out.print(edge.pin+"_pre ");
            }
            else if (port.equalsIgnoreCase(settings.bufferCellOutputPort)) {
                out.print(edge.pin+" ");
            }
            else {
                // unknown port, just tie to gnd
                out.print("0 ");
            }
        }
        out.print(settings.bufferCell+" ");
        out.println(settings.bufferCellStrengthParam+"="+inbufStr);
    }

    private void writeClkBuffer(PinEdge edge, SpiceSubckt clkbufSubckt) {
        if (isStable(edge.transition)) {
            return;  // already tied to gnd or vdd
        }
        out.print("Xbuf"+edge.pin);
        out.print(" ");
        for (String port : clkbufSubckt.getPorts()) {
            if (port.equalsIgnoreCase(settings.clkBufferCellInputPort)) {
                out.print(edge.pin+"_pre ");
            }
            else if (port.equalsIgnoreCase(settings.clkBufferCellOutputPort)) {
                out.print(edge.pin+" ");
            }
            else {
                // unknown port, just tie to gnd
                out.print("0 ");
            }
        }
        out.print(settings.clkBufferCell+" ");
        out.println(settings.clkBufferCellStrengthParam+"="+clkbufStr);
    }

    private void writeLoad(PinEdge edge, SpiceSubckt loadSubckt) {
        out.print("Xload ");
        for (String port : loadSubckt.getPorts()) {
            if (port.equalsIgnoreCase(settings.loadCellPort)) {
                out.print(edge.pin+" ");
            }
            else {
                // unknown port, just tie to gnd
                out.print("0 ");
            }
        }
        out.print(settings.loadCell+" ");
        out.println(settings.loadCellStrengthParam+"="+outloadStr);
    }

    private void writeSweepData() {
        out.println(".DATA DATA_TIM");
        Stack<String> pvals = new Stack<String>();
        out.print("+");
        for (int i=0; i<sweeps.size(); i++) {
            SweepParam sp = sweeps.get(i);
            out.print(" "+sp.param);
        }
        out.println();

        iterateList(sweeps, 0, pvals);
        out.println(".ENDDATA");
    }

    private void iterateList(List<SweepParam> list, int index, Stack<String> stack) {
        if (index >= list.size()) {
            index = 0;
        }
        SweepParam sp = list.get(index);
        for (int i=0; i<sp.sweep.length; i++) {
            stack.push(sp.sweep[i]);
            if (list.size()-1 == index) {
                // last one, print
                out.print("+ ");
                for (String s : stack) out.print(s+" ");
                out.println();
            }
            else {
                iterateList(list, index+1, stack);
            }
            stack.pop();
        }
    }

    private void writeMeasDelay(String measname, PinEdge in, PinEdge output) {
        writeMeasDelay(measname, in, output, null);
    }
    private void writeMeasDelay(String measname, PinEdge in, PinEdge output, String goal) {
        if (isStable(in.transition))
            return;
        out.println(".meas TRAN "+measname);
        out.println("+  TRIG V("+in.pin+") VAL='"+settings.inputDelayThresh+"*vsupply' CROSS=1");
        out.println("+  TARG V("+output.pin+") VAL='"+settings.outputDelayThresh+"*vsupply' CROSS=1");
        if (goal != null)
            out.println("+  goal="+goal);
    }

    private void writeMeasSlew(String measname, PinEdge edge, double low, double high) {
        if (isStable(edge.transition))
            return;
        double start = low;
        double end = high;
        if (edge.transition == PinEdge.Transition.FALL) {
            start = high;
            end = low;
        }
        out.println(".meas TRAN "+measname);
        out.println("+  TRIG V("+edge.pin+") VAL='"+start+"*vsupply' CROSS=1");
        out.println("+  TARG V("+edge.pin+") VAL='"+end+"*vsupply' CROSS=1");
    }

    private void writeMeasCap(String measname, PinEdge edge, String voltageSource) {
        if (isStable(edge.transition)) return;
        double start = settings.edgePercentForCapStart;
        double end = settings.edgePercentForCapEnd;
        if (edge.transition == PinEdge.Transition.FALL) {
            start = 1-settings.edgePercentForCapStart;
            end = 1-settings.edgePercentForCapEnd;
        }
        String vstart = measname+"vstart";
        String vend = measname+"vend";
        String tstart = measname+"tstart";
        String tend = measname+"tend";
        out.println(".param "+vstart+"='"+start+"*vsupply'");
        out.println(".param "+vend+"='"+end+"*vsupply'");
        out.println(".meas TRAN "+tstart+" WHEN V("+edge.pin+") = '"+vstart+"'");
        out.println(".meas TRAN "+tend+" WHEN V("+edge.pin+") = '"+vend+"'");
        out.println(".meas TRAN "+measname+"_avgi");
        out.println("+  AVG i("+voltageSource+") FROM='"+tstart+"' TO='"+tend+"'");
        out.println(".meas TRAN "+measname);
        out.println("+  PARAM='abs("+measname+"_avgi*("+tend+"-"+tstart+")/("+vend+"-"+vstart+"))'");
    }

    private void writeMeasPushout(String measname, PinEdge edge, double pushoutPS) {
        if (isStable(edge.transition)) return;
        out.println(".meas TRAN "+measname+" WHEN V("+edge.pin+") = '"+settings.outputDelayThresh+
                "*vsupply' CROSS=1 pushout='"+pushoutPS+"ps'");
    }

    private void writeMeasGlitch(String measname, PinEdge edge) {
        if (isStable(edge.transition)) return;
        if (edge.transition == PinEdge.Transition.RISE)
            out.println(".meas TRAN "+measname+" WHEN V(Xdut."+edge.pin+") = '"+settings.holdGlitchLowPercent+"*vsupply' RISE=1");
        if (edge.transition == PinEdge.Transition.FALL)
            out.println(".meas TRAN "+measname+" WHEN V(Xdut."+edge.pin+") = '"+settings.holdGlitchHighPercent+"*vsupply' FALL=1");
    }

    private void verifyPorts(Cell topCell, SpiceSubckt testCell, Arc arc, List<String> globalNets) throws SCTimingException {
        // check that ports specified are present on test cell
        //List<String> ports = new ArrayList<String>(testCell.getPorts());
        List<String> ports = new ArrayList<String>();
        Netlist netlist = topCell.getNetlist(Netlist.ShortResistors.ALL);
        for (Iterator<Export> it = topCell.getExports(); it.hasNext(); ) {
            Export ex = it.next();
            String name = netlist.getNetwork(ex, 0).getName();
            if (!ports.contains(name)) ports.add(name);
        }

        for (PinEdge p : arc.stableInputs) {
            if (!testCell.hasPort(p.pin))
                throw new SCTimingException("Pin "+p.pin+" not found on test cell "+testCell.getName()+" for arc "+arc);
            ports.remove(p.pin);
        }
        if (!testCell.hasPort(arc.output.pin))
            throw new SCTimingException("Pin "+arc.output.pin+" not found on test cell "+testCell.getName()+" for arc "+arc);
        if (!testCell.hasPort(arc.input.pin))
            throw new SCTimingException("Pin "+arc.input.pin+" not found on test cell "+testCell.getName()+" for arc "+arc);

        if (arc.clk != null) {
            if (!testCell.hasPort(arc.clk.pin))
                throw new SCTimingException("Pin "+arc.clk.pin+" not found on test cell "+testCell.getName()+" for arc "+arc);
            if (arc.clk.transition != PinEdge.Transition.RISE && arc.clk.transition != PinEdge.Transition.FALL) {
                throw new SCTimingException("Clock pin "+arc.clk.pin+" transition must be RISE or FALL for arc "+arc);
            }
            ports.remove(arc.clk.pin);
        }
        if (arc.clkFalse != null) {
            if (!testCell.hasPort(arc.clkFalse.pin))
                throw new SCTimingException("Pin "+arc.clkFalse.pin+" not found on test cell "+testCell.getName()+" for arc "+arc);
            if (arc.clkFalse.transition != PinEdge.Transition.RISE && arc.clkFalse.transition != PinEdge.Transition.FALL) {
                throw new SCTimingException("Clock pin "+arc.clkFalse.pin+" transition must be RISE or FALL for arc "+arc);
            }
            ports.remove(arc.clkFalse.pin);
        }

        ports.remove(arc.output.pin);
        ports.remove(arc.input.pin);
        for (String s : globalNets) ports.remove(s);
        for (String s : arc.unusedOutputs) ports.remove(s);
        if (ports.size() > 0) {
            StringBuffer pins = new StringBuffer();
            for (String s : ports) pins.append(s+" ");
            throw new SCTimingException("Pins "+pins+" on test cell "+testCell.getName()+" not specified on arc "+arc);
        }

        // check that input, output, and stable transitions are specified correctly
        if (arc.input.transition != PinEdge.Transition.RISE && arc.input.transition != PinEdge.Transition.FALL) {
            throw new SCTimingException("Input pin "+arc.input.pin+" transition must be RISE or FALL for arc "+arc);
        }
        if (arc.output.transition != PinEdge.Transition.RISE && arc.output.transition != PinEdge.Transition.FALL) {
            throw new SCTimingException("Output pin "+arc.output.pin+" transition must be RISE or FALL for arc "+arc);
        }
        for (PinEdge p : arc.stableInputs) {
            if (!isStable(p.transition)) {
                throw new SCTimingException("Stable Input pin "+p.pin+" transition must be STABLE0 or STABLE1 for arc "+arc);
            }
        }
    }

    private static void printColumnMeanStdDev(Table2D data2d, String key, PrintStream msg, double scaleFactor, String units) {
        double [] colIndexVals = data2d.getColIndexVals();
        for (int i=0; i<colIndexVals.length; i++) {
            double [] colVals = data2d.getColumnValues(key, i);
            if (colVals == null) {
                System.out.println("Cannot find col vals for key: "+key);
                return;
            }
            double avg = Table2D.getAverage(colVals) * scaleFactor;
            double stddev = Table2D.getStandardDeviation(colVals) * scaleFactor;
            double stddevp = stddev/avg*100;
            msg.println(key+" for "+data2d.getColName()+"="+colIndexVals[i]+": mean="+
                    TextUtils.formatDouble(avg,4)+units+"  stddev="+
                    TextUtils.formatDouble(stddev,4)+units+"  stddev%="+
                    TextUtils.formatDouble(stddevp,4)+"%");
        }
    }

    private static void printRowMeanStdDev(Table2D data2d, String key, PrintStream msg, double scaleFactor, String units) {
        double [] rowIndexVals = data2d.getRowIndexVals();
        for (int i=0; i<rowIndexVals.length; i++) {
            double [] rowVals = data2d.getRowValues(key, i);
            if (rowVals == null) {
                System.out.println("Cannot find row vals for key: "+key);
                return;
            }
            double avg = Table2D.getAverage(rowVals) * scaleFactor;
            double stddev = Table2D.getStandardDeviation(rowVals) * scaleFactor;
            double stddevp = stddev/avg*100;
            msg.println(key+" for "+data2d.getRowName()+"="+rowIndexVals[i]+": mean="+
                    TextUtils.formatDouble(avg,4)+units+"  stddev="+
                    TextUtils.formatDouble(stddev,4)+units+"  stddev%="+
                    TextUtils.formatDouble(stddevp,4)+"%");
        }
    }

    private static void printMeanStdDev(Table2D data2d, String key, PrintStream msg, double scaleFactor, String units) {
        double [][] values = data2d.getValues(key);
        if (values == null) {
            System.out.println("Cannot find values for key: "+key);
            return;
        }
        double avg = Table2D.getAverage(data2d.getValues(key)) * scaleFactor;
        double stddev = Table2D.getStandardDeviation(data2d.getValues(key)) * scaleFactor;
        double stddevp = stddev/avg*100;
        msg.println(key+": mean="+
                TextUtils.formatDouble(avg,4)+units+"  stddev="+
                TextUtils.formatDouble(stddev,4)+units+"  stddev%="+
                TextUtils.formatDouble(stddevp,4)+"%");
    }

    private static void print2DMeanStdDev(Table2D data2d, String key, PrintStream msg, double scaleFactor, String units) {
        double [] rowIndexVals = data2d.getRowIndexVals();
        double [] colIndexVals = data2d.getColIndexVals();
        double [][] values = data2d.getValues(key);
        double [][] stddev = data2d.getValues(key+"_stddev");
        if (values == null) {
            System.out.println("Cannot find values for key: "+key);
            return;
        }
        if (stddev == null) {
            System.out.println("Cannot find values for key: "+key+"_stddev");
        }

        for (int r=0; r<rowIndexVals.length; r++) {
            for (int c=0; c<colIndexVals.length; c++) {
                double avg = values[r][c] * scaleFactor;
                double dev = stddev == null ? 0 : stddev[r][c] * scaleFactor;
                double devp = stddev == null ? 0 : dev/avg*100;
                msg.println(key+" for "+data2d.getRowName()+"="+rowIndexVals[r]+", "+
                        data2d.getColName()+"="+colIndexVals[c]+": mean="+
                        TextUtils.formatDouble(avg,4)+units+"  stddev="+
                        TextUtils.formatDouble(dev,4)+units+"  stddev%="+
                        TextUtils.formatDouble(devp,4)+"%");
            }
        }


    }

    static boolean isStable(PinEdge.Transition tran) {
        if (tran == PinEdge.Transition.STABLE0 || tran == PinEdge.Transition.STABLE1 ||
            tran == PinEdge.Transition.STABLEV)
            return true;
        return false;
    }

    void err(boolean print, String msg) throws SCTimingException {
        if (print) {
            //System.out.println("SCTiming: Error: "+msg);
            throw new SCTimingException(msg);
        }
    }

    /* *******************************************************
     * Liberty file data
     * *******************************************************/


    /**
     * Get the characeterized cell's data. It is added to the library group specified.
     * @param library the library group to add it to
     * @return the cell group data the cell group created
     */
    public LibData.Group getCellLibData(LibData.Group library) {
        if (characterizationFailed) return null;

        String libertyCellName = topCellNameLiberty;
        if (libertyCellName == null)
            libertyCellName = topCellName;
        List<LibData.Group> groups = library.getGroups("cell", libertyCellName);
        if (groups.size() > 0) {
            System.out.println("Warning, cell for "+libertyCellName+" already present in liberty data, replacing it with new data");
            for (LibData.Group g : groups) {
                library.removeGroup(g);
            }
        }

        LibData.Group cell = new LibData.Group("cell", libertyCellName, library);
        if (interfaceTiming) {
            cell.putAttribute("timing_model_type", "abstracted");
        }

        // Make pin group for each pin, and each timing group for a pin+related pin combo
        Map<String,LibData.Group> pinMap = new HashMap<String,LibData.Group>();
        Map<String,LibData.Group> pinTimingMap = new HashMap<String,LibData.Group>();

        List<String> ports = new ArrayList<String>();
        Netlist netlist = topCell.getNetlist(Netlist.ShortResistors.ALL);
        for (Iterator<Export> it = topCell.getExports(); it.hasNext(); ) {
            Export ex = it.next();
            String name = netlist.getNetwork(ex, 0).getName();
            if (!ports.contains(name)) ports.add(name);
        }

        // make pin groups
        //for (String s : dutSubckt.getPorts()) {
        HashMap<String,Integer> pinTypes = new HashMap<String,Integer>();
        for (String s : ports) {
            if (netlistReader != null &&
                netlistReader.getGlobalNets().contains(s)) continue;

            LibData.Group pin = new LibData.Group("pin", s, cell);
            pinMap.put(s, pin);

            int type = 0;  // 0 in, 1 out, 2 clk
            // find type
            for (Arc arc : timingArcs) {
                if (arc.input.pin.equalsIgnoreCase(s)) {
                    break;
                }
                else if (arc.output.pin.equalsIgnoreCase(s)) {
                    type = 1; break;
                }
                else if (arc.clk != null && arc.clk.pin.equalsIgnoreCase(s)) {
                    type = 2; break;
                }
            }
            pinTypes.put(s, type);
            if (type == 1) {
                pin.putAttribute("direction", "output");
                String function = combinationalFunctions.get(s);
                if (function != null) {
                    function = "\""+function+"\"";
                    pin.putAttribute("function", function);
                }
            } else {
                pin.putAttribute("direction", "input");
            }

            if (testCell != null) {
                if (testCell.isScanIn(s))
                    pin.putAttribute("nextstate_type", "scan_in");
                if (testCell.isScanEn(s))
                    pin.putAttribute("nextstate_type", "scan_enable");
                if (functionFlipFlop != null && testCell.isScanOut(s))
                    pin.putAttribute("function", "i"+functionFlipFlop.getOutputPosPin());
                if (functionFlipFlopSDRtoDDR != null && functionFlipFlopSDRtoDDR.getInputFall().equals(s))
                    pin.putAttribute("nextstate_type", "data");
                if (functionFlipFlopSDRtoDDR != null && functionFlipFlopSDRtoDDR.getInputRise().equals(s))
                    pin.putAttribute("nextstate_type", "data");
                if (functionFlipFlopDDRtoSDR != null && functionFlipFlopDDRtoSDR.getInput().equals(s))
                    pin.putAttribute("nextstate_type", "data");
            }

            if (type == 2) {
                pin.putAttribute("clock", "true");
            }
            // set connection class - always universal?
            pin.putAttribute("connection_class", "universal");

            // put capacitance if input
            // for now just average
            // really should be relative to input buffer (slew) strength, though
            // the Liberty format does not support that?
            if (type == 0 || type == 2) {
                double cap = 0;
                int count = 0;
                for (Arc arc : timingArcs) {
                    int col = getColumn(arc, s+"_cap");
                    if (col > 0) {
                        // valid capacitance measure found
                        cap += arc.data.getAverage(col);
                        count++;
                    }
                }
                if (count != 0)
                    pin.putAttribute("capacitance", cap/count/settings.capUnit);
            }
        }

        // Now, make pin timing groups and put in timing data
        for (Arc arc : timingArcs) {
            String inputPin = arc.input.pin;
            String outputPin = arc.output.pin;
            LibData.Group inpin = pinMap.get(inputPin);
            LibData.Group outpin = pinMap.get(outputPin);

            if (arc.clk == null) {
                // combinational cell, write out propogation delay and slew times
                // on output pin
                // key for timing group is pin plus related pin
                String timingkey = arc.output.pin+"_"+arc.input.pin;
                if (arc.dependentStableInputs.size() > 0) {
                    for (PinEdge p : arc.dependentStableInputs) {
                        timingkey = timingkey + "_"+
                                (p.transition == PinEdge.Transition.STABLE0 ? "!" : "") + p.pin;
                    }
                }
                LibData.Group timing = pinTimingMap.get(timingkey);
                if (timing == null) {
                    timing = new LibData.Group("timing", "", outpin);
                    pinTimingMap.put(timingkey, timing);
                    timing.putAttribute("related_pin", inputPin);
                    String sense = "negative_unate";
                    if (arc.input.transition == arc.output.transition)
                        sense = "positive_unate";
                    timing.putAttribute("timing_sense", sense);
                }

                if (arc.dependentStableInputs.size() > 0) {
                    StringBuffer whenPins = new StringBuffer();
                    StringBuffer sdf = new StringBuffer();
                    for (PinEdge p : arc.dependentStableInputs) {
                        whenPins.append(p.transition == PinEdge.Transition.STABLE0 ? "!" : "");
                        whenPins.append(p.pin);
                        whenPins.append(" & ");
                        sdf.append(p.pin);
                        sdf.append(" == ");
                        sdf.append(p.transition == PinEdge.Transition.STABLE0 ? "1'B0" : "1'B1");
                        sdf.append(" & ");
                    }
                    whenPins.setLength(whenPins.length()-2);
                    sdf.setLength(sdf.length()-2);
                    timing.putAttribute("when", "\""+whenPins.toString().trim()+"\"");
                    timing.putAttribute("sdf_cond", "\""+sdf.toString().trim()+"\"");
                }

                if (noTiming) {
                    timing.putAttribute("intrinsic_rise", "0.02");
                    timing.putAttribute("intrinsic_fall", "0.02");
                    timing.putAttribute("rise_resistance", "0.01");
                    timing.putAttribute("fall_resistance", "0.01");
                    continue;
                }

                String delay = "cell_fall";
                String trans = "fall_transition";
                if (arc.output.transition == PinEdge.Transition.RISE) {
                    delay = "cell_rise";
                    trans = "rise_transition";
                }

                Table2D data2D = arc.data2d_inbuf_outload;
                // propogation delay vs input slew & output load
                LibData.Group delayg = new LibData.Group(delay, settings.tableSlewVsLoads, timing);
                // index 1 is input slew, which is dependent on inbufStr
                double [] inslew = data2D.getAvgRowValues(inputPin+"_slew");

                // index 2 is output cap, which is dependent on output load
                double [] outload = data2D.getAvgColumnValues(outputPin+"_cap");
                String index_1 = toStringQuote(inslew, settings.timeUnit);
                String index_2 = toStringQuote(outload, settings.capUnit);

                // propogation delay
                double [][] propdelay = data2D.getValues("prop_delay");
                delayg.putAttributeComplex("index_1", index_1);
                delayg.putAttributeComplex("index_2", index_2);
                List<String> delays = new ArrayList<String>();
                for (int i=0; i<propdelay.length; i++)
                    delays.add(toStringQuote(propdelay[i], settings.timeUnit));
                delayg.putAttribute("values", delays);

                // output slew vs input slew & output load
                LibData.Group transg = new LibData.Group(trans, settings.tableSlewVsLoads, timing);
                double [][] transtime = data2D.getValues(outputPin+"_slew");
                transg.putAttributeComplex("index_1", index_1);
                transg.putAttributeComplex("index_2", index_2);
                List<String> ttimes = new ArrayList<String>();
                for (int i=0; i<transtime.length; i++)
                    ttimes.add(toStringQuote(transtime[i], settings.timeUnit));
                transg.putAttribute("values", ttimes);
            }
            else {
                if (noTiming) continue;

                String clkPin = arc.clk.pin;
                String edge = "R";
                if (arc.clk.transition == PinEdge.Transition.FALL) edge = "F";
                // sequential cell
                // First, write out setup times on input pin
                // key for timing group is pin plus related pin
                String intimingkeySetup = arc.input.pin+"_"+arc.clk.pin+edge+"_Setup";
                String intimingkeyHold = arc.input.pin+"_"+arc.clk.pin+edge+"_Hold";
                LibData.Group intimingSetup = pinTimingMap.get(intimingkeySetup);
                LibData.Group intimingHold = pinTimingMap.get(intimingkeyHold);
                if (intimingSetup == null) {
                    intimingSetup = new LibData.Group("timing", "", inpin);
                    pinTimingMap.put(intimingkeySetup, intimingSetup);
                    intimingSetup.putAttribute("related_pin", clkPin);
                    if (arc.clk.transition == PinEdge.Transition.FALL) {
                        intimingSetup.putAttribute("timing_type", "setup_falling");
                    } else {
                        intimingSetup.putAttribute("timing_type", "setup_rising");
                    }
                }
                if (intimingHold == null) {
                    intimingHold = new LibData.Group("timing", "", inpin);
                    pinTimingMap.put(intimingkeyHold, intimingHold);
                    intimingHold.putAttribute("related_pin", clkPin);
                    if (arc.clk.transition == PinEdge.Transition.FALL) {
                        intimingHold.putAttribute("timing_type", "hold_falling");
                    } else {
                        intimingHold.putAttribute("timing_type", "hold_rising");
                    }
                }

                Table2D data2D_clkbuf_outload = arc.data2d_clkbuf_outload;
                Table2D data2D_inbuf_clkbuf = arc.data2d_inbuf_clkbuf;

                String trans = "fall_constraint";
                if (arc.input.transition == PinEdge.Transition.RISE) {
                    trans = "rise_constraint";
                }

                // setup time vs (input slew and clock slew)
                LibData.Group setup = new LibData.Group(trans, settings.tableSetupHold, intimingSetup);
                // hold time vs (input slew and clock slew)
                LibData.Group hold = new LibData.Group(trans, settings.tableSetupHold, intimingHold);

                // index 1 is input slew, which is dependent on inbufStr
                double [] inslew = data2D_inbuf_clkbuf.getAvgRowValues(inputPin+"_slew");
                setup.putAttributeComplex("index_1", toStringQuote(inslew, settings.timeUnit));
                hold.putAttributeComplex("index_1", toStringQuote(inslew, settings.timeUnit));

                // index 2 is clk slew, which is dependent on clkbufStr
                double [] clkslew = data2D_inbuf_clkbuf.getAvgColumnValues(clkPin+"_slew");
                if (clkslew.length > 1) {
                    setup.putAttributeComplex("index_2", toStringQuote(clkslew, settings.timeUnit));
                    hold.putAttributeComplex("index_2", toStringQuote(clkslew, settings.timeUnit));
                }

                // setup time
                double [][] setups = data2D_inbuf_clkbuf.getValues(setupTimeName);
                List<String> setupVals = new ArrayList<String>();
                for (int i=0; i<setups.length; i++) {
                    setupVals.add(toStringQuote(setups[i], settings.timeUnit));
                }
                if (clkslew.length > 1)
                    setup.putAttribute("values", setupVals);
                else
                    setup.putAttributeComplex("values", toStringQuote(setups[0], settings.timeUnit));

                // hold time
                double [][] holds = data2D_inbuf_clkbuf.getValues(holdTimeName);
                List<String> holdVals = new ArrayList<String>();
                for (int i=0; i<holds.length; i++) {
                    holdVals.add(toStringQuote(holds[i], settings.timeUnit));
                }
                if (clkslew.length > 1)
                    hold.putAttribute("values", holdVals);
                else
                    hold.putAttributeComplex("values", toStringQuote(holds[0], settings.timeUnit));

                // Second, write out clk2q times on output pin
                // key for timing group is pin plus related pin
                String outtimingkey = arc.output.pin+"_"+arc.clk.pin+edge;
                LibData.Group outtiming = pinTimingMap.get(outtimingkey);
                if (outtiming == null) {
                    outtiming = new LibData.Group("timing", "", outpin);
                    pinTimingMap.put(outtimingkey, outtiming);
                    outtiming.putAttribute("related_pin", clkPin);
                    outtiming.putAttribute("timing_sense", "non_unate");
                    PinEdge.Transition clkTrans = arc.clk.transition;
                    if (functionLatch != null) clkTrans = arc.clk.getOpposite().transition;
                    if (clkTrans == PinEdge.Transition.FALL) {
                        outtiming.putAttribute("timing_type", "falling_edge");
                    } else {
                        outtiming.putAttribute("timing_type", "rising_edge");
                    }
                }

                String clk2qname = "cell_fall";
                String outtrans = "fall_transition";
                if (arc.output.transition == PinEdge.Transition.RISE) {
                    clk2qname = "cell_rise";
                    outtrans = "rise_transition";
                }

                // Clock to Q vs (clock slew and output load)
                LibData.Group clk2q = new LibData.Group(clk2qname, settings.tableClk2Q, outtiming);
                // index 1 is clk slew
                clkslew = data2D_clkbuf_outload.getAvgRowValues(clkPin+"_slew");
                if (clkslew.length > 1)
                    clk2q.putAttributeComplex("index_1", toStringQuote(clkslew, settings.timeUnit));
                // index 2 is output load
                double [] outload = data2D_clkbuf_outload.getAvgColumnValues(outputPin+"_cap");
                if (clkslew.length > 1)
                    clk2q.putAttributeComplex("index_2", toStringQuote(outload, settings.capUnit));
                else
                    clk2q.putAttributeComplex("index_1", toStringQuote(outload, settings.capUnit));
                // clock to q values
                double [][] clk2qVals = data2D_clkbuf_outload.getValues(this.clk2q);
                List<String> clk2qStrings = new ArrayList<String>();
                for (int i=0; i<clk2qVals.length; i++) {
                    clk2qStrings.add(toStringQuote(clk2qVals[i], settings.timeUnit));
                }
                if (clkslew.length > 1)
                    clk2q.putAttribute("values", clk2qStrings);
                else
                    clk2q.putAttributeComplex("values", toStringQuote(clk2qVals[0], settings.timeUnit));

                // Output slew vs (clock slew and output load)
                LibData.Group outslew = new LibData.Group(outtrans, settings.tableClk2Q, outtiming);
                // index 1 is clk slew
                if (clkslew.length > 1)
                    outslew.putAttributeComplex("index_1", toStringQuote(clkslew, settings.timeUnit));
                // index 2 is output load
                if (clkslew.length > 1)
                    outslew.putAttributeComplex("index_2", toStringQuote(outload, settings.capUnit));
                else
                    outslew.putAttributeComplex("index_1", toStringQuote(outload, settings.capUnit));
                // output slew times
                double [][] outslewVals = data2D_clkbuf_outload.getValues(outputPin+"_slew");
                List<String> outslewStrings = new ArrayList<String>();
                for (int i=0; i<outslewVals.length; i++) {
                    outslewStrings.add(toStringQuote(outslewVals[i], settings.timeUnit));
                }
                if (clkslew.length > 1)
                    outslew.putAttribute("values", outslewStrings);
                else
                    outslew.putAttributeComplex("values", toStringQuote(outslewVals[0], settings.timeUnit));

                // make ff group for flip flop
                String ffname = "i"+outputPin+", i"+outputPin+"b";
                List<LibData.Group> ffs = cell.getGroups("ff", ffname);
                if (ffs.size() == 0) {
                }
            }
        }

        if (functionFlipFlop != null) {
            String ffname = "i"+functionFlipFlop.getOutputPosPin()+", i"+functionFlipFlop.getOutputNegPin();
            LibData.Group ff = new LibData.Group("ff", ffname, cell);
            if (testCell != null) {
                ff.putAttribute("next_state", "\"("+testCell.getScanEnPin()+"&"+testCell.getScanInPin()+")|(!"+
                                testCell.getScanEnPin()+"&"+functionFlipFlop.getInputPin()+")\" ");
            } else
                ff.putAttribute("next_state", functionFlipFlop.getInputPin());
            ff.putAttribute("clocked_on", functionFlipFlop.getClockedOnPin());
        }
        if (functionFlipFlopSDRtoDDR != null) {
            String ffname = "i"+functionFlipFlopSDRtoDDR.getOutputPos()+", i"+functionFlipFlopSDRtoDDR.getOutputNeg();
            LibData.Group ff = new LibData.Group("ff", ffname, cell);
            if (testCell != null) {
                ff.putAttribute("next_state", "\"("+testCell.getScanEnPin()+"&"+testCell.getScanInPin()+")|(!"+
                                testCell.getScanEnPin()+"&("+functionFlipFlopSDRtoDDR.getInputRise()+"|"+
                                functionFlipFlopSDRtoDDR.getInputFall()+"))\" ");
            } else
                ff.putAttribute("next_state", "\""+ functionFlipFlopSDRtoDDR.getInputRise()+"|"+
                                functionFlipFlopSDRtoDDR.getInputFall()+"\" ");
            ff.putAttribute("clocked_on", functionFlipFlopSDRtoDDR.getClockedOnPin());
        }
        if (functionFlipFlopDDRtoSDR != null) {
            String ffname = "i"+functionFlipFlopDDRtoSDR.getOutputRisePos()+", i"+functionFlipFlopDDRtoSDR.getOutputRiseNeg();
            LibData.Group ff = new LibData.Group("ff", ffname, cell);
            if (testCell != null) {
                ff.putAttribute("next_state", "\"("+testCell.getScanEnPin()+"&"+testCell.getScanInPin()+")|(!"+
                                testCell.getScanEnPin()+"&"+functionFlipFlopDDRtoSDR.getInput()+")\" ");
            } else
                ff.putAttribute("next_state", functionFlipFlopDDRtoSDR.getInput());
            ff.putAttribute("clocked_on", functionFlipFlopDDRtoSDR.getClockedOnPin());
        }

        if (testCell != null) {
            LibData.Group tcell = new LibData.Group("test_cell", "", cell);
            if (functionFlipFlop != null) {
                String ffname = "i"+functionFlipFlop.getOutputPosPin()+", i"+functionFlipFlop.getOutputNegPin();
                LibData.Group ff = new LibData.Group("ff", ffname, tcell);
                ff.putAttribute("next_state", functionFlipFlop.getInputPin());
                ff.putAttribute("clocked_on", functionFlipFlop.getClockedOnPin());
            }
            for (String s : ports) {
                if (netlistReader != null &&
                    netlistReader.getGlobalNets().contains(s)) continue;

                LibData.Group pin = new LibData.Group("pin", s, tcell);
                Integer type = pinTypes.get(s);
                if (type != null) {
                    if (type.intValue() == 0 || type.intValue() == 2)
                        pin.putAttribute("direction", "input");
                    if (type.intValue() == 1)
                        pin.putAttribute("direction", "output");
                }
                if (testCell.isScanIn(s))
                    pin.putAttribute("signal_type", "test_scan_in");
                if (testCell.isScanOut(s)) {
                    pin.putAttribute("signal_type", "test_scan_out");
                    pin.putAttribute("function", "i"+functionFlipFlop.getOutputPosPin());
                    pin.putAttribute("test_output_only", "true");
                }
                if (testCell.isScanEn(s))
                    pin.putAttribute("signal_type", "test_scan_enable");
                if (functionFlipFlop != null) {
                    if (s.equals(functionFlipFlop.getOutputPosPin()))
                        pin.putAttribute("function", "i"+functionFlipFlop.getOutputPosPin());
                    if (s.equals(functionFlipFlop.getOutputNegPin()))
                        pin.putAttribute("function", "i"+functionFlipFlop.getOutputNegPin());
                }
            }
        }
        if (functionLatch != null && interfaceTiming == false) {
            String ffname = "i"+functionLatch.getOutputPosPin()+", i"+functionLatch.getOutputNegPin();
            LibData.Group ff = new LibData.Group("latch", ffname, cell);
            ff.putAttribute("data_in", functionLatch.getInputPin());
            ff.putAttribute("enable", functionLatch.getEnablePin());
        }
        return cell;
    }

    private static class FlipFlopFunction {
        private String outputPos;
        private String outputNeg;
        private String inputPin;
        private String clockedOnPin;
        private boolean ddr = false;
        public FlipFlopFunction(String outputPin, String outputNegPin, String inputPin, String clockedOnPin, boolean ddr) {
            this.outputPos = outputPin;
            this.outputNeg = outputNegPin;
            this.inputPin = inputPin;
            this.clockedOnPin = clockedOnPin;
            this.ddr = ddr;
        }
        public String getOutputPosPin() { return outputPos; }
        public String getOutputNegPin() { return outputNeg; }
        public String getInputPin() { return inputPin; }
        public String getClockedOnPin() { return clockedOnPin; }
        public boolean isDDR() { return ddr; }
    }

    private static class FlipFlopFunctionSDRtoDDR {
        private String outputPos;
        private String outputNeg;
        private String inputRise;
        private String inputFall;
        private String clockedOnPin;
        public FlipFlopFunctionSDRtoDDR(String outputPos, String outputNeg,
                                   String inputRise, String inputFall, String clockedOnPin) {
            this.outputPos = outputPos;
            this.outputNeg = outputNeg;
            this.inputRise = inputRise;
            this.inputFall = inputFall;
            this.clockedOnPin = clockedOnPin;
        }
        public String getOutputPos() { return outputPos; }
        public String getOutputNeg() { return outputNeg; }
        public String getInputRise() { return inputRise; }
        public String getInputFall() { return inputFall; }
        public String getClockedOnPin() { return clockedOnPin; }
    }

    private static class FlipFlopFunctionDDRtoSDR {
        private String outputRisePos;
        private String outputRiseNeg;
        private String outputFallPos;
        private String outputFallNeg;
        private String input;
        private String clockedOnPin;
        public FlipFlopFunctionDDRtoSDR(String outputRisePos, String outputRiseNeg, String outputFallPos, String outputFallNeg,
                                   String input, String clockedOnPin) {
            this.outputRisePos = outputRisePos;
            this.outputRiseNeg = outputRiseNeg;
            this.outputFallPos = outputFallPos;
            this.outputFallNeg = outputFallNeg;
            this.input = input;
            this.clockedOnPin = clockedOnPin;
        }
        public FlipFlopFunctionDDRtoSDR(String outputRise, String outputFall, String input, String clockedOnPin) {
            this(outputRise, outputRise+"_n", outputFall, outputFall+"_n", input, clockedOnPin);
        }
        public String getOutputRisePos() { return outputRisePos; }
        public String getOutputRiseNeg() { return outputRiseNeg; }
        public String getOutputFallPos() { return outputFallPos; }
        public String getOutputFallNeg() { return outputFallNeg; }
        public String getInput() { return input; }
        public String getClockedOnPin() { return clockedOnPin; }
    }

    private static class LatchFunction {
        private String outputPos;
        private String outputNeg;
        private String inputPin;
        private String enablePin;
        public LatchFunction(String outputPin, String outputNegPin, String inputPin, String enablePin) {
            this.outputPos = outputPin;
            this.outputNeg = outputNegPin;
            this.inputPin = inputPin;
            this.enablePin = enablePin;
        }
        public String getOutputPosPin() { return outputPos; }
        public String getOutputNegPin() { return outputNeg; }
        public String getInputPin() { return inputPin; }
        public String getEnablePin() { return enablePin; }
    }

    private static class TestCell {
        private String scanInPin;
        private String scanOutPin;
        private String scanEnPin;
        public TestCell(String scanInPin, String scanOutPin, String scanEnPin) {
            this.scanInPin = scanInPin;
            this.scanOutPin = scanOutPin;
            this.scanEnPin = scanEnPin;
        }
        public String getScanInPin() { return scanInPin; }
        public String getScanOutPin() { return scanOutPin; }
        public String getScanEnPin() { return scanEnPin; }
        public boolean isScanIn(String s) { return (scanInPin != null) && s.equals(scanInPin); }
        public boolean isScanOut(String s) { return (scanOutPin != null) && s.equals(scanOutPin); }
        public boolean isScanEn(String s) { return (scanEnPin != null) && s.equals(scanEnPin); }
    }

    private String toStringQuote(double val, double scale) {
        double [] a = {val};
        return toStringQuote(a, scale);
    }

    private String toStringQuote(double [] vals, double scale) {
        StringBuffer buf = new StringBuffer("\"");
        for (double d : vals) {
            buf.append(TextUtils.formatDouble(d/scale, 5)); buf.append(" ");
        }
        buf.append("\"");
        return buf.toString();
    }

    private int getColumn(Arc arc, String name) {
        if (arc.data == null) return -1;
        return arc.data.getColumn(name);
    }
}
