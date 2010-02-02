/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LogicSettableArray.java
 * Written by Tom O'Neill, Sun Microsystems.
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

/**
 * Keeps track of the state of a set of logic outputs, whose states can be
 * queried independently, used by the various JTAG testers.
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
