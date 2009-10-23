package com.sun.electric.tool.simulation.test;

/*
 * LogicSettableArray.java
 * 
 * Copyright (c) 2005 by Sun Microsystems, Inc.
 *
 * Created on February 11, 2005
 */

/**
 * Keeps track of the state of a set of logic outputs, whose states can be
 * queried independently, used by the various JTAG testers.
 * 
 * @author Tom O'Neill
 */

class LogicSettableArray {

    /*
     * State of parallel outputs. Constructor is responsible for creating and
     * filling the array, if any parallel outputs are available.
     */
    private boolean[] outputState;

    /**
     * Create the array of logic outputs, and initialize their state to HI
     * 
     * @param numOutputs
     *            Number of independent logic outputs
     */
    LogicSettableArray(int numOutputs) {
        outputState = new boolean[numOutputs];
        for (int gpio = 0; gpio < numOutputs; gpio++) {
            outputState[gpio] = true;
        }
    }

    /**
     * Sets the logic level for a single channel
     * 
     * @param index
     *            Which parallel output to set
     * @param newLevel
     *            set parallel output <tt>HI</tt>?
     */
    void setLogicState(int index, boolean newLevel) {
        checkIndex(index);
        outputState[index] = newLevel;
    }

    /**
     * Returns the logic state of the specified parallel output
     * 
     * @param index
     *            Which parallel output to query
     * @return state of specified parallel output
     */
    boolean isLogicStateHigh(int index) {
        checkIndex(index);
        return outputState[index];
    }

    /** Checks if parallel I/O index is in the allowed range */
    private void checkIndex(int index) {
        if (index < 0 || index >= outputState.length) {
            Infrastructure.fatal("Index " + index + " not in allowed range 0.."
                    + (outputState.length - 1));
        }
    }

    /**
     * @return Returns the outputState.
     */
    public boolean[] getLogicStates() {
        return outputState;
    }

    /**
     * @param outputState
     *            The outputState to set.
     */
    public void setLogicStates(boolean[] outputState) {
        this.outputState = outputState;
    }
}
