/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AnalogAnalysis.java
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

import java.util.*;
import com.sun.electric.tool.io.input.*;
import com.sun.electric.database.geometry.btree.*;
import java.io.*;

/**
 * Analysis which contains analog signals
 */
public class AnalogAnalysis extends Analysis<AnalogSignal> {
	/** all sweeps in this Analysis */							private List<Object> sweeps;
	/** the common time array (if there is common time) */		private double [] commonTime;
	/** the common time array (if there is common time) */		private HashMap<AnalogSignal,Waveform[]> waveformCache = new HashMap<AnalogSignal,Waveform[]>();

	/**
	 * Constructor for a collection of analog simulation data.
	 * @param sd Stimuli that this analysis is part of.
	 * @param type the type of this analysis.
	 * @param extrapolateToRight true to draw the last value to the right
	 * (useful for IRSIM and other digital simulations).
	 * False to stop drawing signals after their last value
	 * (useful for Spice and other analog simulations).
	 */
	public AnalogAnalysis(Stimuli sd, AnalysisType type, boolean extrapolateToRight)
	{
		super(sd, type, extrapolateToRight);
		if (type != ANALYSIS_MEAS)
			sweeps = new ArrayList<Object>();
	}

	/**
	 * Free allocated resources before closing.
	 */
	@Override
	public void finished()
	{
		super.finished();
		if (sweeps != null) sweeps.clear();
	}

	@Override
	public boolean isAnalog() { return true; }

	/**
	 * Method to add information about another sweep in this simulation data.
	 * @param obj sweep information (typically a Double).
	 */
	public void addSweep(Object obj)
	{
		sweeps.add(obj);
		setBoundsDirty();
	}

	/**
	 * Method to return the number of sweep objects in this simulation data.
	 * @return number of sweep objects in this simulation data.
	 * If there is no sweep information, the list is empty.
	 */
	public int getNumSweeps() { return (sweeps != null) ? Math.max(sweeps.size(), 1) : 1; }

	/**
	 * Method to return sweep object in a given position.
	 * @param i the position to get.
	 * @return the sweep object for that position.
	 */
	public Object getSweep(int i)
	{
		if (sweeps == null || sweeps.size() == 0) return null;
		return sweeps.get(i);
	}

	/**
	 * Method to construct an array of time values that are common to all signals.
	 * Some simulation data has all of its stimuli at the same time interval for every signal.
	 * To save space, such data can use a common time array, kept in the Simulation Data.
	 * If a signal wants to use its own time values, that can be done by placing the time
	 * array in the signal.
	 * @param numEvents the number of time events in the common time array.
	 */
	public void buildCommonTime(int numEvents)
	{
		commonTime = new double[numEvents];
	}

	/**
	 * Method to load an entry in the common time array.
	 * @param index the entry number.
	 * @param time the time value at
	 */
	public void setCommonTime(int index, double time)
	{
		commonTime[index] = time;
		setBoundsDirty();
	}

	/**
	 * Method to get the array of time entries for this signal.
	 * @return the array of time entries for this signal.
	 */
	public double [] getCommonTimeArray() { return commonTime; }

	/**
	 * Create new AnalogSignal with specified name.
	 * Signal obtains waveform constructed from common time and specified values.
	 * @param signalName signal name.
	 * @param signalContext a common prefix for the signal name.
	 * @param values specified values
	 * @return new AnalogSignal of this AnalogAnalysis
	 */
	public AnalogSignal addSignal(String signalName, String signalContext, double[] values)
	{
		AnalogSignal as = addEmptySignal(signalName, signalContext);
        if (!isUseLegacySimulationCode()) {
            BTree<Double,Double,Serializable> tree = NewEpicAnalysis.getTree();
            int evmax = 0;
            int evmin = 0;
            double valmax = Double.MIN_VALUE;
            double valmin = Double.MAX_VALUE;
            for(int i=0; i<commonTime.length; i++) {
                tree.insert(new Double(commonTime[i]), new Double(values[i]));
                if (values[i] > valmax) { evmax = i; valmax = values[i]; }
                if (values[i] < valmin) { evmin = i; valmin = values[i]; }
            }
            Waveform[] waveforms = { new BTreeNewSignal(evmin, evmax, tree) };
            waveformCache.put(as, waveforms);
//            System.err.println("put a btree");
        } else {
            Waveform[] waveforms = { new WaveformImpl(getCommonTimeArray(), values) };
            waveformCache.put(as, waveforms);
        }
		return as;
	}

	/**
	 * Create new AnalogSignal with specified name.
	 * Signal obtains range constructed from common time range and specified value bounds.
	 * @param signalName signal name.
	 * @param signalContext a common prefix for the signal name.
	 * @param minValue the minimum value.
	 * @param maxValue the maximum value.
	 * @return new AnalogSignal of this AnalogAnalysis
	 */
	public AnalogSignal addSignal(String signalName, String signalContext, double minTime, double maxTime, 
                                  double minValue, double maxValue)
	{
		AnalogSignal as = addEmptySignal(signalName, signalContext);
		return as;
	}

	/**
	 * Create new AnalogSignal with specified name.
	 * @param signalName signal name.
	 * @param signalContext a common prefix for the signal name.
	 * @return new AnalogSignal of this AnalogAnalysis
	 */
	private AnalogSignal addEmptySignal(String signalName, String signalContext)
	{
		AnalogSignal as = new AnalogSignal(this);
		as.setSignalName(signalName, signalContext);
		return as;
	}

	/**
	 * Method to return the waveform of specified signal in specified sweep.
	 * @param signal specified signal
	 * @param sweep sweep index
	 * @return the waveform of this signal in specified sweep.
	 */
	public Waveform getWaveform(AnalogSignal signal, int sweep)
	{
		Waveform[] waveforms = waveformCache.get(signal);
		if (waveforms == null)
		{
			if (signal.getAnalysis() != this)
				throw new IllegalArgumentException();
			waveforms = loadWaveforms(signal);
			assert waveforms.length == getNumSweeps();
			waveformCache.put(signal, waveforms);
		}
		return waveforms[sweep];
	}

	protected Waveform[] loadWaveforms(AnalogSignal signal)
	{
		throw new UnsupportedOperationException();
	}
}
