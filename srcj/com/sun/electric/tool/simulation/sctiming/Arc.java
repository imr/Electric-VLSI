/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Arc.java
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

import java.util.List;
import java.util.ArrayList;

/**
 * A Timing Arc. Can be combinational or sequential.
 * A Timing arc consists of a single input, a single output,
 * zero or more stable (non-changing) inputs, and zero
 * or more initial condition for internal nodes.  Node and
 * pin names should correspond to the top cell (device to be
 * characterized).  For sequential timing, a clk (and optionally
 * a clkFalse) are also specified.
 */
public class Arc {

    List<PinEdge> stableInputs = new ArrayList<PinEdge>();
    PinEdge input;
    PinEdge output;
    TableData data;         // simulation measurement results
    Table2D data2d_inbuf_outload;
    Table2D data2d_clkbuf_outload;
    Table2D data2d_inbuf_clkbuf;
    PinEdge clk = null;
    PinEdge clkFalse = null;
    List<PinEdge> initialConditions = new ArrayList<PinEdge>();
    String outputLoadSweep = null; 
    String inputBufferSweep = null;
    List<String> unusedOutputs = new ArrayList<String>();
    List<PinEdge> dependentStableInputs = new ArrayList<PinEdge>();
    PinEdge glitchNode = null;

    /**
     * Set the transition on the input being stimulated
     * @param pin input pin name
     * @param transition transition type
     */
    public void setInputTransition(String pin, PinEdge.Transition transition) {
        if (input != null) {
            System.out.println("Warning: specified singular input pin twice, ignoring second assingment");
            return;
        }
        input = new PinEdge(pin, transition);
    }

    /**
     * Set the expected transition on the output
     * @param pin output pin name
     * @param transition transition type
     */
    public void setOutputTransition(String pin, PinEdge.Transition transition) {
        if (output != null) {
            System.out.println("Warning: specified singular output pin twice, ignoring second assingment");
            return;
        }
        output = new PinEdge(pin, transition);
    }

    /**
     * Add a stable signal to an unused input pin
     * @param pin input pin name
     * @param transition stable transition type
     */
    public void addStableInput(String pin, PinEdge.Transition transition) {
        stableInputs.add(new PinEdge(pin, transition));
    }

    /**
     * Add a stable signal to a dependent input pin - the state of the
     * input pin affects the type (rise/fall) of transition on the output.
     * This is used for example in an XOR gate, where an input rising edge
     * can cause both a rising and falling output edge, dependent on the
     * state of the other input.
     * @param pin input pin name
     * @param transition stable transition type
     */
    public void addDependentStableInput(String pin, PinEdge.Transition transition) {
        stableInputs.add(new PinEdge(pin, transition));
        dependentStableInputs.add(new PinEdge(pin, transition));
    }

    /**
     * Add a stable signal (voltage level) to an unused input pin
     * @param pin input pin name
     * @param voltage voltage value
     */
    public void addStableInput(String pin, double voltage) {
        stableInputs.add(new PinEdge(pin, voltage));
    }

    /**
     * Set the clock transition for a sequential cell
     * @param pin clock input pin
     * @param transition transition type
     */
    public void setClkTransition(String pin, PinEdge.Transition transition) {
        if (clk != null) {
            System.out.println("Warning: specified singular clk pin twice, ignoring second assingment");
            return;
        }
        clk = new PinEdge(pin, transition);
    }

    /**
     * If a clock-bar pin exists, set the transition for it. It
     * will occur at the same time as the clock transition
     * @param pin the clock-bar pin
     * @param transition transition type
     */
    public void setClkFalseTransition(String pin, PinEdge.Transition transition) {
        if (clkFalse != null) {
            System.out.println("Warning: specified singular clkFalse pin twice, ignoring second assingment");
            return;
        }
        clkFalse = new PinEdge(pin, transition);
    }

    /**
     * Add an initial condition on a node inside the device under test
     * @param node the internal node name
     * @param value voltage value of the initial condition
     */
    public void addDUTInitialCondition(String node, double value) {
        initialConditions.add(new PinEdge(node, value));
    }

    /**
     * Specify an intial condition on a node inside the device under test,
     * to be used during glitch-based Hold time tests.
     * The transition sets the glitch transition type. (Rising glitch or falling glitch)
     * @param node the internal node name
     * @param transition transition type
     */
    public void setHoldTimeGlitchNode(String node, PinEdge.Transition transition) {
        if (glitchNode != null) {
            System.out.println("Warning: specified singular glitch node twice, ignoring second assingment");
            return;
        }
        glitchNode = new PinEdge(node, transition);
    }

    /**
     * Specify an unused output pin. This pin will be ignored for characterization
     * of this arc.
     * @param pin the output pin
     */
    public void addUnusedOutput(String pin) {
        if (unusedOutputs.contains(pin)) return;
        unusedOutputs.add(pin);
    }

    public String toString() {
        StringBuffer desc = new StringBuffer();
        for (PinEdge in : stableInputs) {
            appendDesc(desc, in);
        }
        appendDesc(desc, input);
        if (clk != null) {
            appendDesc(desc, clk);
        }
        desc.append(output.pin); desc.append("_");
        desc.append(descTran(output.transition));
        return desc.toString();
    }

    private void appendDesc(StringBuffer buf, PinEdge pin) {
        buf.append(pin.pin); buf.append("_");
        buf.append(descTran(pin.transition)); buf.append("_");
    }

    /**
     * Get a single character description of the transition type
     * @param tran the transition type
     * @return a single character description
     */
    public static String descTran(PinEdge.Transition tran) {
        switch(tran) {
            case STABLE0: return "0";
            case STABLE1: return "1";
            case RISE: return "R";
            case FALL: return "F";
            case STABLEV: return "V";
        }
        return "";
    }

    /**
     * Set the sweep values (in X drive strength). The number
     * of values must be equal to the number of values specified in the
     * default settings
     * @param sweep space delimited list of values
     */
    public void setOutputLoadSweep(String sweep) {
        this.outputLoadSweep = sweep;
    }

    public String getOutputLoadSweep() { return outputLoadSweep; }

    /**
     * Set the sweep values (in X drive strength) for the input buffer.
     * @param sweep space delimited list of values
     */
    public void setInputBufferSweep(String sweep) {
        this.inputBufferSweep = sweep;
    }

    public String getInputBufferSweep() { return inputBufferSweep; }
}
