/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SCSettings.java
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

import com.sun.electric.database.text.TextUtils;

import java.util.List;
import java.util.ArrayList;

/**
 * Library-wide settings
 * User: gainsley
 * Date: Nov 16, 2006
 */
public class SCSettings {

    String simulator = "hspice";
    public String libName = "sclib";
    public String commonHeaderFile = "";

    String bufferCell = null;
    String bufferCellStrengthParam = null;
    String bufferCellInputPort = null;
    String bufferCellOutputPort = null;
    String bufferCellSweep = null;
    String bufferCellSweepMinTime = null;
    String bufferCellSweepExcludeFromAveraging = null;
    String loadCell = null;
    String loadCellStrengthParam = null;
    String loadCellPort = null;
    String loadCellSweepExcludeFromAveraging = null;
    String loadCellSweep = null;
    String loadCellSweepMinTime = null;
    String loadCellSweepForSetupHold = null;
    String loadCellSweepForSetupHoldMinTime = null;
    String clkBufferCell = null;
    String clkBufferCellStrengthParam = null;
    String clkBufferCellInputPort = null;
    String clkBufferCellOutputPort = null;
    String clkBufferCellSweep = null;
    String clkBufferCellSweepMinTime = null;

    String operatingPointName = null;
    public double vdd = 0;
    double temp = 25;
    double tech = 1;
    double inputRampTimePS = 50;
    double simResolutionPS = 1;
    double simTimePS = 10000;
    double timeStartPS = 200;
    double periodPS = 1000;
    double inputLow = 0.2;
    double inputHigh = 0.8;
    double inputDelayThresh = 0.5;
    double outputLow = 0.2;
    double outputHigh = 0.8;
    double outputDelayThresh = 0.5;
    double edgePercentForCapStart = 0.05;
    double edgePercentForCapEnd = 0.55;
    double holdGlitchHighPercent = 0.8;
    double holdGlitchLowPercent = 0.2;

    double tmsetupMinGuessPS = 0;
    double tmsetupMaxGuessPS = 300;
    double tmsetupGuessPS = 100;
    double clk2qpushout = 20;

    double capUnit = 1e-12;       // if you change the cap or time units, change them below as well
    double timeUnit = 1e-9;

    boolean simpleSequentialCharacterization = false;

    /**
     * Set the simulator (typically hspice). If the simulator is not on your
     * PATH, you should specify the path here as well.
     * @param pathandname full path to executable
     */
    public void setSimulator(String pathandname) { this.simulator = pathandname; }

    /**
     * Set the common header file to be included in all characterization netlists.
     * This should include the spice model file .lib statement and the buffer,
     * clock buffer, and output load subckts.
     * @param pathandname full path to file
     */
    public void setCommonHeaderFile(String pathandname) { this.commonHeaderFile = pathandname; }

    /**
     * Set VDD for the simulation
     * @param name a name of this operating point
     * @param vdd the voltage
     * @param temp the temperature
     */
    public void setOperatingPoint(String name, double vdd, double temp) {
        this.operatingPointName = name;
        this.vdd = vdd;
        this.temp = temp;
    }

    /**
     * Get Vdd setting
     * @return vdd setting
     */
    public double getVdd() { return vdd; }

    /**
     * Set the name of library. This only affects the library name
     * used when writing the Liberty file.
     * @param libName name of library
     */
    public void setLibrary(String libName) { this.libName = libName; }

    /**
     * Set the buffer cell used to drive the input. This should be a
     * buffer (non-inverting), with one input and one output, and
     * a parameter used to vary its drive strength.
     * All unspecified pins are tied to ground.
     * @param cellname the name of the subckt
     * @param strengthParam the name of the strength parameter
     * @param inputPort the name of the input port
     * @param outputPort the name of the output port
     */
    public void setBufferCell(String cellname, String strengthParam,
                              String inputPort, String outputPort) {
        this.bufferCell = cellname;
        this.bufferCellStrengthParam = strengthParam;
        this.bufferCellInputPort = inputPort;
        this.bufferCellOutputPort = outputPort;
    }

    /**
     * Set the clock buffer cell used to drive the clock pin input
     * (and clock false input if specified on the arc).
     * This should be a buffer (non-inverting), with one input and
     * one output, and a parameter used to vary its drive strength.
     * All unspecified pins are tied to ground.
     * @param cellname the name of the subckt
     * @param strengthParam the name of the strength parameter
     * @param inputPort the name of the input port
     * @param outputPort the name of the output port
     */
    public void setClkBufferCell(String cellname, String strengthParam, String inputPort, String outputPort) {
        this.clkBufferCell = cellname;
        this.clkBufferCellStrengthParam = strengthParam;
        this.clkBufferCellInputPort = inputPort;
        this.clkBufferCellOutputPort = outputPort;
    }

    /**
     * Set the load cell used to load the output of the test cell.
     * There should be one input pin, and a parameter used to
     * vary its drive strength.
     * All unspecified pins are tied to ground.
     * @param cellname the name of the subckt
     * @param strengthParam the name of the strength parameter
     * @param loadPort the name of the load port
     */
    public void setLoadCell(String cellname, String strengthParam, String loadPort) {
        this.loadCell = cellname;
        this.loadCellStrengthParam = strengthParam;
        this.loadCellPort = loadPort;
    }

    /**
     * Set the time the simulation will run, and the resolution in ps.
     * Typically the resolution should be 1ps, and the duration should
     * be 1000ps.
     * @param resolutionPS the resolution in ps
     * @param durationPS the duration in ps
     */
    public void setSimulationTime(double resolutionPS, double durationPS) {
        this.simResolutionPS = resolutionPS;
        this.simTimePS = durationPS;
    }

    /**
     * Set the input buffer sweep. This will set the values of
     * the input buffer strength param. The string argument should
     * be a space separated list of the form "1 2 4 8".
     * @param sweep the list of strengths to sweep
     */
    public void setInputBufferSweep(String sweep) {
        bufferCellSweep = sweep;
    }

    /**
     * Set the input buffer sweep for min time. This will set the values of
     * the input buffer strength param. The string argument should
     * be a space separated list of the form "1 2 4 8".
     * @param sweep the list of strengths to sweep
     */
    public void setInputBufferSweepMinTime(String sweep) {
        bufferCellSweepMinTime = sweep;
    }

    /**
     * Set the load cell sweep. This will set the values of
     * the load cell strength param. The string argument should
     * be a space separated list of the form "1 2 4 8".
     * @param sweep the list of strengths to sweep
     */
    public void setLoadSweep(String sweep) {
        loadCellSweep = sweep;
        if (loadCellSweepForSetupHold == null)
            loadCellSweepForSetupHold = sweep;
    }

    /**
     * Set the load cell sweep for min time. This will set the values of
     * the load cell strength param. The string argument should
     * be a space separated list of the form "1 2 4 8".
     * @param sweep the list of strengths to sweep
     */
    public void setLoadSweepMinTime(String sweep) {
        loadCellSweepMinTime = sweep;
        if (loadCellSweepForSetupHoldMinTime == null)
            loadCellSweepForSetupHoldMinTime = sweep;
    }

    /**
     * Set the load cell sweep for setup and hold, otherwise
     * it defaults to whatever you set for the normal load cell sweep
     * (used for clk2q in sequential tests).
     * The string argument should
     * be a space separated list of the form "1 2 4 8".
     * @param sweep the list of strengths to sweep
     */
    public void setLoadSweepForSetupHold(String sweep) {
        loadCellSweepForSetupHold = sweep;
    }

    /**
     * Set the load cell sweep for setup and hold (min time), otherwise
     * it defaults to whatever you set for the normal load cell sweep
     * (used for clk2q in sequential tests).
     * The string argument should
     * be a space separated list of the form "1 2 4 8".
     * @param sweep the list of strengths to sweep
     */
    public void setLoadSweepForSetupHoldMinTime(String sweep) {
        loadCellSweepForSetupHoldMinTime = sweep;
    }

    /**
     * Set the clock buffer size. This will set the value of
     * the clock buffer strength param.
     * Normally, for the clock, only one value is used,
     * as the timing analyzer does not analyze the clock
     * tree (assumes ideal clock). Therefore, it cannot
     * predict the clock edge rate, so it assumes
     * the clock tree is designed to produce this
     * edge rate everywhere.
     * @param size the size of the clock buffer
     */
    public void setClkBufferSize(String size) {
        clkBufferCellSweep = size;
    }

    /**
     * Set the clock buffer size for min time. This will set the value of
     * the clock buffer strength param.
     * Normally, for the clock, only one value is used,
     * as the timing analyzer does not analyze the clock
     * tree (assumes ideal clock). Therefore, it cannot
     * predict the clock edge rate, so it assumes
     * the clock tree is designed to produce this
     * edge rate everywhere.
     * @param size the size of the clock buffer
     */
    public void setClkBufferSizeMinTime(String size) {
        clkBufferCellSweepMinTime = size;
    }

    /**
     * Set the input low, high, and delay measurement thresholds,
     * as a percentage of vdd.  These numbers must be between 0 and 1.
     * @param low low level threshold (usually 0.2)
     * @param high high level threshold (usually 0.8)
     * @param delay delay level threshold (usually 0.5)
     */
    public void setInputThresholds(double low, double high, double delay) {
        this.inputLow = low;
        this.inputHigh = high;
        this.inputDelayThresh = delay;
    }
    /**
     * Set the output low, high, and delay measurement thresholds,
     * as a percentage of vdd.  These numbers must be between 0 and 1.
     * @param low low level threshold (usually 0.2)
     * @param high high level threshold (usually 0.8)
     * @param delay delay level threshold (usually 0.5)
     */
    public void setOutputThresholds(double low, double high, double delay) {
        this.outputLow = low;
        this.outputHigh = high;
        this.outputDelayThresh = delay;
    }

    /**
     * Set the ramp time used by input and clock edges of
     * PWL voltage sources used to drive input and clock
     * buffers.
     * @param inputRampTimePS the edge rate, 0 to 100%, in ps.
     */
    public void setInputRampTimePS(double inputRampTimePS) {
        this.inputRampTimePS = inputRampTimePS;
    }

    /**
     * Set the setup time range when doing sequential characterization.
     * Note that characterization will run faster if the range is smaller.
     * Values are in picoseconds. Default is 0, 100ps, 300ps.
     * @param low minimum possible value (typically 0 or negative)
     * @param guess a guess as to the actual value (though it will vary wrt to input drive strength)
     * @param high maximum possible value
     */
    public void setSetupTimeRangePS(double low, double guess, double high) {
        tmsetupGuessPS = guess;
        tmsetupMaxGuessPS = high;
        tmsetupMinGuessPS = low;
    }

    /**
     * Set the values of the buffer cell sweep to
     * ignore for clock-to-Q times. Clk-to-Q is assumed to be independent
     * of input rise/fall times, but that is not strictly true - especially
     * if the range of input rise/fall times used is quite large. This method allows
     * you to mask out the outliers to get a more reasonable clock-to-q value.
     * <P>
     * The string passed in should be a space separated set of values, such
     * as "1 2 3 4".
     * @param values the values to exclude
     */
    public void setInputBufferSweepExcludeFromAveraging(String values) {
        this.bufferCellSweepExcludeFromAveraging = values;
    }

    /**
     * Set the values of the load cell sweep to exclude from the calculation of
     *  setup and hold times.  Setup and Hold times
     * are assumed to be independent of output load, but that is not strictly true -
     * especially if the range of loads is quite large. This method allows you
     * to mask out the outliers to get a more reasonable setup and hold value.
     * <P>
     * The string passed in should be a space separated set of values, such
     * as "1 2 3 4".
     * @param values the values to exclude
     */
    public void setLoadSweepExcludeFromAveraging(String values) {
        this.loadCellSweepExcludeFromAveraging = values;
    }

    /**
     * Set the setup, hold, and clock-2-q characterization to simple
     * mode (matches Sun Microelectronics). Rather than optimizing
     * for a minimal setup+clk2q delay, this measures clk2q on a
     * static input, and then decreases the setup time until the clk2q
     * delay gets pushed out (moved) by an amount specified.
     * @param b true to set to simple mode
     */
    public void setSimpleSequentialCharacterization(boolean b) {
        this.simpleSequentialCharacterization = b;
    }

    /**
     * For simple setup sequential characterization mode, set the amount
     * of clk2q push out (degredation) that defines the setup time.
     * @param ps the amount of push out in ps
     */
    public void setClk2QPushOut(double ps) {
        this.clk2qpushout = ps;
    }

    /**
     * Set the percentages of vdd at which the hold test considers the signal
     * to be glitched - high for a glitch on vdd, and low for a glitch on gnd.
     * @param high the high value glitch (eg 0.8 for 80% of vdd)
     * @param low the low value glitch (eg 0.2 for 20% of vdd)
     */
    public void setHoldTimeGlitchPercentages(double high, double low) {
        this.holdGlitchHighPercent = high;
        this.holdGlitchLowPercent = low;
    }

    void checkSettings(boolean sequentialTest) throws SCTimingException {

        err(libName == null, "Library name not specified");
        err(bufferCell == null, "Buffer cell not specified");
        err(bufferCellInputPort == null, "Buffer cell input port not specified");
        err(bufferCellOutputPort == null, "Buffer cell output port not specified");
        err(bufferCellStrengthParam == null, "Buffer strength param not specified");
        err(bufferCellSweep == null, "Sweep for input buffer cell not specified");
        err(loadCell == null, "Load cell not specified");
        err(loadCellPort == null, "Load cell load port not specified");
        err(loadCellStrengthParam == null, "Load cell strength param not specified");
        err(loadCellSweep == null, "Sweep for load cell not specified");
        err(loadCellSweepForSetupHold == null, "Sweep for load cell not specified for setup and hold");
        err(operatingPointName == null, "Operating point (vdd, temp) not specified");

        if (sequentialTest) {
            if (bufferCellSweepMinTime == null) bufferCellSweepMinTime = bufferCellSweep;
            if (loadCellSweepMinTime == null) loadCellSweepMinTime = loadCellSweep;
            if (clkBufferCellSweepMinTime == null) clkBufferCellSweepMinTime = clkBufferCellSweep;
            if (loadCellSweepForSetupHoldMinTime == null) loadCellSweepForSetupHoldMinTime = loadCellSweepForSetupHold;

            err(clkBufferCell == null, "Clock buffer cell not specified");
            err(clkBufferCellInputPort == null, "Clock buffer input port not specfieid");
            err(clkBufferCellOutputPort == null, "Clock buffer output port not specfieid");
            err(clkBufferCellStrengthParam == null, "Clock buffer strength param not specified");
            err(clkBufferCellSweep == null, "Sweep for clock buffer cell not specified");
            if (simpleSequentialCharacterization) {
            }
        }
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

    String tableSlewVsLoads = "tableInputSlewVsCLoad";
    String tableSetupHold = "tableInputSlewVsClkSlew";
    String tableClk2Q = "tableInputClkSlewVsCLoad";
    int numClkSlews = 0;

    /**
     * Get a Liberty Data Group that is the Library with
     * all settings used for this characterization.
     * @return a Liberty Data Group
     */
    public LibData.Group getLibrary() {
        LibData.Group library = new LibData.Group("library", libName, null);

        // parameters and units etc
        library.putAttributeComplex("technology", "cmos");
        library.putAttribute("delay_model", "table_lookup");
        List<LibData.Value> attrList = new ArrayList<LibData.Value>();
        attrList.add(new LibData.Value(LibData.ValueType.INT, new Integer(1)));
        attrList.add(new LibData.Value(LibData.ValueType.STRING, getUnitScale(capUnit)+"f"));
        LibData.Head h = new LibData.Head("capacitive_load_unit", attrList);
        library.putAttribute(new LibData.Attribute("capacitive_load_unit", h));
        library.putAttribute("time_unit", "\"1"+ getUnitScale(timeUnit)+"s\"");
        library.putAttribute("voltage_unit", "\"1V\"");
        library.putAttribute("current_unit", "\"1A\"");
        library.putAttribute("input_threshold_pct_rise", inputDelayThresh*100);
        library.putAttribute("input_threshold_pct_fall", inputDelayThresh*100);
        library.putAttribute("output_threshold_pct_rise", outputDelayThresh*100);
        library.putAttribute("output_threshold_pct_fall", outputDelayThresh*100);
        library.putAttribute("slew_lower_threshold_pct_rise", inputLow*100);
        library.putAttribute("slew_upper_threshold_pct_rise", inputHigh*100);
        library.putAttribute("slew_lower_threshold_pct_fall", inputLow*100);
        library.putAttribute("slew_upper_threshold_pct_fall", inputHigh*100);

        library.putAttribute("pulling_resistance_unit", "\"1ohm\"");

        library.putAttribute("default_fanout_load", "1.0");
        library.putAttribute("default_inout_pin_cap", "1.0");
        library.putAttribute("default_input_pin_cap", "1.0");
        library.putAttribute("default_output_pin_cap", "0.0");

        LibData.Group op = new LibData.Group("operating_conditions", operatingPointName, library);
        op.putAttribute("voltage", vdd);
        op.putAttribute("temperature", temp);
        op.putAttribute("process", 1.0);

        // sc timing specific (not liberty supported) options
        /*
        library.putAttribute("simulator", simulator);
        library.putAttribute("commonHeaderFile", commonHeaderFile);
        library.putAttribute("bufferCell", bufferCell);
        library.putAttribute("bufferCellStrengthParam", bufferCellStrengthParam);
        library.putAttribute("bufferCellInputPort", bufferCellInputPort);
        library.putAttribute("bufferCellOutputPort", bufferCellOutputPort);
        library.putAttribute("bufferCellSweep", bufferCellSweep);
        library.putAttribute("loadCell", loadCell);
        library.putAttribute("loadCellStrengthParam", loadCellStrengthParam);
        library.putAttribute("loadCellPort", loadCellPort);
        library.putAttribute("loadCellSweep", loadCellSweep);
        library.putAttribute("clkBufferCell", clkBufferCell);
        library.putAttribute("clkBufferCellStrengthParam", clkBufferCellStrengthParam);
        library.putAttribute("clkBufferCellInputPort", clkBufferCellInputPort);
        library.putAttribute("clkBufferCellOutputPort", clkBufferCellOutputPort);
        library.putAttribute("clkBufferCellSweep", clkBufferCellSweep);
        library.putAttribute("vdd", vdd);
        library.putAttribute("inputRampTimePS", inputRampTimePS);
        library.putAttribute("simTimePS", simTimePS);
        library.putAttribute("simResolutionPS", simResolutionPS);
        library.putAttribute("tmsetupMinGuessPS", tmsetupMinGuessPS);
        library.putAttribute("tmsetupGuessPS", tmsetupGuessPS);
        library.putAttribute("tmsetupMaxGuessPS", tmsetupMaxGuessPS);
        */

        // table definitions
        // table slew vs loads
        int numSlews = bufferCellSweep.trim().split("\\s+").length;
        int numLoads = loadCellSweep.trim().split("\\s+").length;
        StringBuffer slewsCount = new StringBuffer("\"");
        for (int i=1; i<=numSlews; i++) slewsCount.append(i+" ");
        slewsCount.append("\"");
        StringBuffer loadsCount = new StringBuffer("\"");
        for (int i=1; i<=numLoads; i++) loadsCount.append(i+" ");
        loadsCount.append("\"");
        LibData.Group luTableSlewVsLoads = new LibData.Group("lu_table_template", tableSlewVsLoads, library);
        luTableSlewVsLoads.putAttribute("variable_1", "input_net_transition");
        luTableSlewVsLoads.putAttribute("variable_2", "total_output_net_capacitance");
        luTableSlewVsLoads.putAttributeComplex("index_1", slewsCount.toString());
        luTableSlewVsLoads.putAttributeComplex("index_2", loadsCount.toString());

        // table setup and hold
        numClkSlews = clkBufferCellSweep.trim().split("\\s+").length;
        StringBuffer clksCount = new StringBuffer("\"");
        for (int i=1; i<=numClkSlews; i++) clksCount.append(i+" ");
        clksCount.append("\"");
        LibData.Group luTableSetupHold = new LibData.Group("lu_table_template", tableSetupHold, library);
        luTableSetupHold.putAttribute("variable_1", "constrained_pin_transition");
        if (numClkSlews > 1)
            luTableSetupHold.putAttribute("variable_2", "related_pin_transition");
        luTableSetupHold.putAttributeComplex("index_1", slewsCount.toString());
        if (numClkSlews > 1)
            luTableSetupHold.putAttributeComplex("index_2", clksCount.toString());

        // table clock to Q
        LibData.Group luTableClk2Q = new LibData.Group("lu_table_template", tableClk2Q, library);
        if (numClkSlews > 1) {
            luTableClk2Q.putAttribute("variable_1", "input_net_transition");
            luTableClk2Q.putAttribute("variable_2", "total_output_net_capacitance");
            luTableClk2Q.putAttributeComplex("index_1", clksCount.toString());
            luTableClk2Q.putAttributeComplex("index_2", loadsCount.toString());
        } else {
            luTableClk2Q.putAttribute("variable_1", "total_output_net_capacitance");
            luTableClk2Q.putAttributeComplex("index_1", loadsCount.toString());
        }

        return library;
    }

    /**
     * Set the settings of this object from some liberty data
     * @param data
     */
    public void getSettingsFromLibData(LibData data) {
        if (data == null) return;
        LibData.Group library = data.getLibrary();
        if (library == null) return;

        inputDelayThresh = getDoubleAttribute(library, "input_threshold_pct_rise", inputDelayThresh*100)/100;
        outputDelayThresh = getDoubleAttribute(library, "output_threshold_pct_rise", outputDelayThresh*100)/100;
        inputLow = getDoubleAttribute(library, "slew_lower_threshold_rise", inputLow*100)/100;
        inputHigh = getDoubleAttribute(library, "slew_upper_threshold_rise", inputHigh*100)/100;

        simulator = getStringAttribute(library, "simulator", simulator);
        commonHeaderFile = getStringAttribute(library, "commonHeaderFile", commonHeaderFile);
        bufferCell = getStringAttribute(library, "bufferCell", bufferCell);
        bufferCellStrengthParam = getStringAttribute(library, "bufferCellStrengthParam", bufferCellStrengthParam);
        bufferCellInputPort = getStringAttribute(library, "bufferCellInputPort", bufferCellInputPort);
        bufferCellOutputPort = getStringAttribute(library, "bufferCellOutputPort", bufferCellOutputPort);
        bufferCellSweep = getStringAttribute(library, "bufferCellSweep", bufferCellSweep);
        loadCell = getStringAttribute(library, "loadCell", loadCell);
        loadCellStrengthParam = getStringAttribute(library, "loadCellStrengthParam", loadCellStrengthParam);
        loadCellPort = getStringAttribute(library, "loadCellPort", loadCellPort);
        loadCellSweep = getStringAttribute(library, "loadCellSweep", loadCellSweep);
        clkBufferCell = getStringAttribute(library, "clkBufferCell", clkBufferCell);
        clkBufferCellStrengthParam = getStringAttribute(library, "clkBufferCellStrengthParam", clkBufferCellStrengthParam);
        clkBufferCellInputPort = getStringAttribute(library, "clkBufferCellInputPort", clkBufferCellInputPort);
        clkBufferCellOutputPort = getStringAttribute(library, "clkBufferCellOutputPort", clkBufferCellOutputPort);
        clkBufferCellSweep = getStringAttribute(library, "clkBufferCellSweep", clkBufferCellSweep);
        vdd = getDoubleAttribute(library, "vdd", vdd);
        inputRampTimePS = getDoubleAttribute(library, "inputRampTimePS", inputRampTimePS);
        simTimePS = getDoubleAttribute(library, "simTimePS", simTimePS);
        simResolutionPS = getDoubleAttribute(library, "simResolutionPS", simResolutionPS);
        tmsetupGuessPS = getDoubleAttribute(library, "tmsetupGuessPS", tmsetupGuessPS);
        tmsetupMinGuessPS = getDoubleAttribute(library, "tmsetupMinGuessPS", tmsetupMinGuessPS);
        tmsetupMaxGuessPS = getDoubleAttribute(library, "tmsetupMaxGuessPS", tmsetupMaxGuessPS);
    }

    private static String getStringAttribute(LibData.Group group, String name, String defaultVal) {
        LibData.Attribute attr = group.getAttribute(name);
        if (attr == null) return defaultVal;
        String val = attr.getString();
        if (val == null) return defaultVal;
        return val;
    }

    private static double getDoubleAttribute(LibData.Group group, String name, double defaultVal) {
        LibData.Attribute attr = group.getAttribute(name);
        if (attr == null) return defaultVal;
        Double val = attr.getDouble();
        if (val == null) {
            // try converting string
            String sval = attr.getString();
            if (sval == null) return defaultVal;
            try {
                return Double.parseDouble(sval);
            } catch (NumberFormatException e) {
                return defaultVal;
            }
        }
        return val.doubleValue();
    }

    private static String getUnitScale(double d) {
        String s = TextUtils.formatDoublePostFix(d);
        for (int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c))
                return String.valueOf(c);
        }
        return "";
    }
}
