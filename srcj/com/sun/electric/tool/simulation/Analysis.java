/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Analysis.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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

import com.sun.electric.database.text.TextUtils;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Class to define a set of simulation data.
 * This class encapsulates all of the simulation data that is displayed in a waveform window.
 * It includes the labels and values.
 * It can handle digital, analog, and many variations (intervals, sweeps).
 */
public class Analysis
{
	public static class AnalysisType
	{
		private String name;
		private static List<AnalysisType> allTypes = new ArrayList<AnalysisType>();

		AnalysisType(String name)
		{
			this.name = name;
			allTypes.add(this);
		}

		public String toString() { return name; }

		public static AnalysisType findAnalysisType(String analysisName)
		{
			for(AnalysisType at : allTypes)
			{
				if (at.name.equals(analysisName)) return at;
			}
			return null;
		}
	}
	/** indicates general signals */	public static final AnalysisType ANALYSIS_SIGNALS = new AnalysisType("Signals");
	/** indicates transient analysis */	public static final AnalysisType ANALYSIS_TRANS = new AnalysisType("Transient");
	/** indicates AC analysis */		public static final AnalysisType ANALYSIS_AC = new AnalysisType("AC");
	/** indicates DC analysis */		public static final AnalysisType ANALYSIS_DC = new AnalysisType("DC");
	/** indicates Measurement data */	public static final AnalysisType ANALYSIS_MEAS = new AnalysisType("Measurement");

	/** the Stimuli in which this Analysis resides */			private Stimuli sd;
	/** the type of analysis data here */						private AnalysisType type;
	/** a list of all signals in this Analysis */				private List<Signal> signals;
	/** a map of all signal names in this Analysis */			private HashMap<String,Signal> signalNames;
	/** a list of all bussed signals in this Analysis */		private List<Signal> allBussedSignals;
	/** all sweeps in this Analysis */							private List<Object> sweeps;
	/** the common time array (if there is common time) */		private double [] commonTime;
	/** a list of time arrays for each sweep */					private List<double[]> sweepCommonTime;
	/** the range of values in this Analysis */					private Rectangle2D bounds;

	public Analysis(Stimuli sd, AnalysisType type)
	{
		this.sd = sd;
		this.type = type;
		signals = new ArrayList<Signal>();
		signalNames = new HashMap<String,Signal>();
		allBussedSignals = new ArrayList<Signal>();
		if (type != ANALYSIS_MEAS)
		{
			sweeps = new ArrayList<Object>();
			sweepCommonTime = new ArrayList<double[]>();
		}
		sd.addAnalysis(this);
	}

    /**
     * Free allocated resources before closing.
     */
    public void finished()
    {
        for (Signal s : signals)
            s.finished();
        signals.clear();
        for (Signal s : allBussedSignals)
            s.finished();
        if (sweeps != null) sweeps.clear();
        if (sweepCommonTime != null) sweepCommonTime.clear();
        signalNames.clear();
    }
    
	/**
	 * Method to return the Stimuli in which this Analysis resides.
	 * @return the Stimuli in which this Analysis resides.
	 */
	public Stimuli getStimuli()
	{
		return sd;
	}

	/**
	 * Method to return the type of data currently being manipulated.
	 * Possibilities are ANALYSIS_TRANS, ANALYSIS_AC, ANALYSIS_DC, or ANALYSIS_MEAS.
	 * @return the type of data currently being manipulated.
	 */
	public AnalysisType getAnalysisType()
	{
		return type;
	}

	/**
	 * Method to get the list of signals in this Simulation Data object.
	 * @return a List of signals.
	 */
	public List<Signal> getSignals() { return signals; }

	/**
	 * Method to get the list of bussed signals in this Simulation Data object.
	 * @return a List of signals.
	 */
	public List<Signal> getBussedSignals() { return allBussedSignals; }

	public void nameSignal(Signal ws, String sigName)
	{
		String name = TextUtils.canonicString(sigName);
		signalNames.put(name, ws);

		// simulators may strip off last "_"
		if (name.indexOf('_') >= 0 && !name.endsWith("_"))
			signalNames.put(name + "_", ws);
	}

	/**
	 * Method to add a new signal to this Simulation Data object.
	 * Signals can be either digital or analog.
	 * @param ws the signal to add.
	 * Instead of a "Signal", use either DigitalSignal or AnalogSignal.
	 */
	public void addSignal(Signal ws)
	{
		signals.add(ws);
		String sigName = ws.getFullName();
		if (sigName != null) nameSignal(ws, sigName);
		setBoundsDirty();
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
	 * Method to construct an array of time values that are common to all signals, but different
	 * for the next sweep.
	 * This method must be called in the order of sweeps.
	 * Some simulation data has all of its stimuli at the same time interval for every signal.
	 * To save space, such data can use a common time array, kept in the Simulation Data.
	 * If a signal wants to use its own time values, that can be done by placing the time
	 * array in the signal.
	 * @param numEvents the number of time events in the common time array.
	 */
	public void addCommonTime(int numEvents)
	{
		double [] sct = new double[numEvents];
		sweepCommonTime.add(sct);
		setBoundsDirty();
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
	 * Method to load an entry in the common time array for a particular sweep.
	 * @param index the entry number.
	 * @param sweep the sweep number.
	 * @param time the time value at
	 */
	public void setCommonTime(int index, int sweep, double time)
	{
		double [] sct = sweepCommonTime.get(sweep);
		sct[index] = time;
		setBoundsDirty();
	}

	/**
	 * Method to get the array of time entries for a sweep on this signal.
	 * @param sweep the sweep number.
	 * @return the array of time entries for a sweep on this signal.
	 */
	public double [] getCommonTimeArray(int sweep) { return sweepCommonTime.get(sweep); }

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
	public int getNumSweeps() { return (sweeps != null) ? sweeps.size() : 0; }

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
	 * Method to compute the time and value bounds of this simulation data.
	 * @return a Rectangle2D that has time bounds in the X part and
	 * value bounds in the Y part.
	 */
	public Rectangle2D getBounds()
	{
		if (bounds == null)
		{
			bounds = null;
			for(Signal sig : signals)
			{
				Rectangle2D sigBounds = sig.getBounds();
				if (bounds == null)
				{
					bounds = new Rectangle2D.Double(sigBounds.getMinX(), sigBounds.getMinY(), sigBounds.getWidth(), sigBounds.getHeight());
				} else
				{
					Rectangle2D.union(bounds, sigBounds, bounds);
				}
			}
		}
		return bounds;
	}

	public void setBoundsDirty() { bounds = null; }

	/**
	 * Method to tell whether this simulation data is analog or digital.
	 * @return true if this simulation data is analog.
	 */
	public boolean isAnalog()
	{
		if (getSignals().size() > 0)
		{
			TimedSignal sSig = (TimedSignal)getSignals().get(0);
			if (sSig instanceof AnalogSignal) return true;
		}
		return false;
	}

	/**
	 * Method to quickly return the signal that corresponds to a given Network name.
	 * Not all names may be found (because of name mangling, which this method does not handle).
	 * But the lookup is faster than "findSignalForNetwork".
	 * @param netName the Network name to find.
	 * @return the Signal that corresponds with the Network.
	 * Returns null if none can be found.
	 */
	public Signal findSignalForNetworkQuickly(String netName)
	{
		String lookupName = TextUtils.canonicString(netName);
		Signal sSig = signalNames.get(lookupName);
		return sSig;
	}

	/**
	 * Method to return the signal that corresponds to a given Network name.
	 * @param netName the Network name to find.
	 * @return the Signal that corresponds with the Network.
	 * Returns null if none can be found.
	 */
	public Signal findSignalForNetwork(String netName)
	{
		// look at all signal names in the cell
		for(Iterator it = getSignals().iterator(); it.hasNext(); )
		{
			Signal sSig = (Signal)it.next();

			String signalName = sSig.getFullName();
			if (netName.equalsIgnoreCase(signalName)) return sSig;

			// if the signal name has underscores, see if all alphabetic characters match
			if (signalName.length() + 1 == netName.length() && netName.charAt(signalName.length()) == ']')
			{
				signalName += "_";
			}
			if (signalName.length() == netName.length() && signalName.indexOf('_') >= 0)
			{
				boolean matches = true;
				for(int i=0; i<signalName.length(); i++)
				{
					char sigChar = signalName.charAt(i);
					char netChar = netName.charAt(i);
					if (TextUtils.isLetterOrDigit(sigChar) != TextUtils.isLetterOrDigit(netChar))
					{
						matches = false;
						break;
					}
					if (TextUtils.isLetterOrDigit(sigChar) &&
						TextUtils.canonicChar(sigChar) != TextUtils.canonicChar(netChar))
					{
						matches = false;
						break;
					}
				}
				if (matches) return sSig;
			}
		}
		return null;
	}
}
