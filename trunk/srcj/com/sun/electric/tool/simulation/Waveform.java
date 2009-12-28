/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Waveform.java
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
package com.sun.electric.tool.simulation;

/**
 * Waveform - piecewise linear real function of time
 */
public interface Waveform extends NewSignal<ScalarSample> {

    /**
     * Method to return the number of events in this signal.
     * This is the number of events along the horizontal axis, usually "time".
     * @return the number of events in this signal.
     */
    public int getNumEvents();

    /**
     * Method to return the value of this signal at a given event index.
     * @param index the event index (0-based).
     * @param result double array of length 3 to return (time, lowValue, highValue)
     * If this signal is not a basic signal, return 0 and print an error message.
     */
    public void getEvent(int index, double[] result);

}
