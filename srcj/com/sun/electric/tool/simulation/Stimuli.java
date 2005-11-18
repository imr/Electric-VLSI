/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Stimuli.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.ui.WaveformWindow;

import java.awt.geom.Rectangle2D;
import java.net.URL;
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
public class Stimuli
{
	// logic levels and signal strengths for digital signals
	public static final int LOGIC         =  03;
	public static final int LOGIC_LOW     =   0;
	public static final int LOGIC_X       =   1;
	public static final int LOGIC_HIGH    =   2;
	public static final int LOGIC_Z       =   3;
	public static final int STRENGTH      = 014;
	public static final int OFF_STRENGTH  =   0;
	public static final int NODE_STRENGTH =  04;
	public static final int GATE_STRENGTH = 010;
	public static final int VDD_STRENGTH  = 014;

	/** the WaveformWindow associated with this Stimuli */		private WaveformWindow ww;
	/** the simulation engine associated with this Stimuli */	private Engine engine;
	/** the cell attached to this Stimuli information */		private Cell cell;
	/** the type of data in this Stimuli */						private FileType type;
	/** the disk file associated with this Stimuli */			private URL fileURL;
	/** a list of all signals in this Stimuli */				private List<Signal> signals;
	/** a map of all signal names in this Stimuli */			private HashMap<String,Signal> signalNames;
	/** a list of all bussed signals in this Stimuli */			private List<Signal> allBussedSignals;
	/** all sweeps in this Stimuli */							private List<Object> sweeps;
	/** the separator character that breaks names */			private char separatorChar;
	/** the common time array (if there is common time) */		private double [] commonTime;
	/** a list of time arrays for each sweep */					private List<double[]> sweepCommonTime;
	/** a List of measurements */								private List<Measurement> measurements;

	/**
	 * Constructor to build a new Simulation Data object.
	 */
	public Stimuli()
	{
		signals = new ArrayList<Signal>();
		signalNames = new HashMap<String,Signal>();
		sweeps = new ArrayList<Object>();
		allBussedSignals = new ArrayList<Signal>();
		sweepCommonTime = new ArrayList<double[]>();
		separatorChar = '.';
		measurements = null;
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
	}

	/**
	 * Method to construct an array of time values that are common to all signals.
	 * Some simulation data has all of its stimuli at the same time interval for every signal.
	 * To save space, such data can use a common time array, kept in the Simulation Data.
	 * If a signal wants to use its own time values, that can be done by placing the time
	 * array in the signal.
	 * @param numEvents the number of time events in the common time array.
	 */
	public void buildCommonTime(int numEvents) { commonTime = new double[numEvents]; }

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
	}

	/**
	 * Method to load an entry in the common time array.
	 * @param index the entry number.
	 * @param time the time value at
	 */
	public void setCommonTime(int index, double time) { commonTime[index] = time; }

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
		double [] sct = (double [])sweepCommonTime.get(sweep);
		sct[index] = time;
	}

	/**
	 * Method to get the array of time entries for a sweep on this signal.
	 * @param sweep the sweep number.
	 * @return the array of time entries for a sweep on this signal.
	 */
	public double [] getCommonTimeArray(int sweep) { return (double [])sweepCommonTime.get(sweep); }

	/**
	 * Method to add information about another sweep in this simulation data.
	 * @param obj sweep information (typically a Double).
	 */
	public void addSweep(Object obj) { sweeps.add(obj); }

	/**
	 * Method to return the list of sweep information in this simulation data.
	 * @return a list of sweep information in this simulation data.
	 * If there is no sweep information, the list is empty.
	 */
	public List<Object> getSweepList() { return sweeps; }
	
	/**
	 * Method to set the measurement data on this Stimuli.
	 * @param data a List of Measurement objects.
	 */
	public void setMeasurementData(List<Measurement> data)
	{
		measurements = data;
	}

	/**
	 * Method to get the measurements.
	 * @return a List of measurements.
	 */
	public List<Measurement> getMeasurements() { return measurements; }

	/**
	 * Method to set the Cell associated with this simulation data.
	 * The associated Cell is the top-level cell in the hierarchy,
	 * and is usually the Cell that was used to generate the simulation input deck.
	 * @param cell the Cell associated with this simulation data.
	 */
	public void setCell(Cell cell) { this.cell = cell; }

	/**
	 * Method to return the Cell associated with this simulation data.
	 * The associated Cell is the top-level cell in the hierarchy,
	 * and is usually the Cell that was used to generate the simulation input deck.
	 * @return the Cell associated with this simulation data.
	 */
	public Cell getCell() { return cell; }

	/**
	 * Method to set the simulation Engine associated with this simulation data.
	 * This is only for data associated with built-in simulators (ALS and IRSIM).
	 * @param engine the simulation Engine associated with this simulation data.
	 */
	public void setEngine(Engine engine) { this.engine = engine; }

	/**
	 * Method to return the simulation Engine associated with this simulation data.
	 * This is only for data associated with built-in simulators (ALS and IRSIM).
	 * @return the simulation Engine associated with this simulation data.
	 */
	public Engine getEngine() { return engine; }

	public void setWaveformWindow(WaveformWindow ww) { this.ww = ww; }
	
	/**
	 * Method to return the separator character for names in this simulation.
	 * The separator character separates levels of hierarchy.  It is usually a "."
	 * @return the separator character for names in this simulation.
	 */
	public char getSeparatorChar() { return separatorChar; }

	/**
	 * Method to set the separator character for names in this simulation.
	 * The separator character separates levels of hierarchy.  It is usually a "."
	 * @param sep the separator character for names in this simulation.
	 */
	public void setSeparatorChar(char sep) { separatorChar = sep; }

	/**
	 * Method to set the type of this simulation data.
	 * Data types are file types, which are unique among the different simulation output formats.
	 * For example, OpenFile.Type.HSPICEOUT is the output of HSpice, whereas
	 * OpenFile.Type.SPICEOUT is the output of Spice3/GNUCap.
	 * @param type the type of this simulation data.
	 */
	public void setDataType(FileType type) { this.type = type; }

	/**
	 * Method to return the type of this simulation data.
	 * Data types are file types, which are unique among the different simulation output formats.
	 * For example, OpenFile.Type.HSPICEOUT is the output of HSpice, whereas
	 * OpenFile.Type.SPICEOUT is the output of Spice3/GNUCap.
	 * @return the type of this simulation data.
	 */
	public FileType getDataType() { return type; }

	/**
	 * Method to set a URL to the file containing this simulation data.
	 * @param fileURL a URL to the file containing this simulation data.
	 */
	public void setFileURL(URL fileURL) { this.fileURL = fileURL; }

	/**
	 * Method to return a URL to the file containing this simulation data.
	 * @return a URL to the file containing this simulation data.
	 */
	public URL getFileURL() { return fileURL; }

	/**
	 * Method to return the WaveformWindow that displays this simulation data.
	 * @return the WaveformWindow that displays this simulation data.
	 */
	public WaveformWindow getWaveformWindow() { return ww; }

	/**
	 * Method to compute the time and value bounds of this simulation data.
	 * @return a Rectangle2D that has time bounds in the X part and
	 * value bounds in the Y part.
	 */
	public Rectangle2D getBounds()
	{
		// determine extent of the data
		Rectangle2D bounds = new Rectangle2D.Double();
		boolean first = true;
		for(Iterator<Signal> it = signals.iterator(); it.hasNext(); )
		{
			Signal sig = (Signal)it.next();
			Rectangle2D sigBounds = sig.getBounds();
			if (first)
			{
				bounds = sigBounds;
				first = false;
			} else
			{
				Rectangle2D.union(bounds, sigBounds, bounds);
			}
		}
		return bounds;
	}

	/**
	 * Method to tell whether this simulation data is analog or digital.
	 * @return true if this simulation data is analog.
	 */
	public boolean isAnalog()
	{
		if (getSignals().size() > 0)
		{
			Signal sSig = (Signal)getSignals().get(0);
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
		Signal sSig = (Signal)signalNames.get(lookupName);
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
		return findSignalForNetworkQuickly(netName);
//		// look at all signal names in the cell
//		for(Iterator it = getSignals().iterator(); it.hasNext(); )
//		{
//			Signal sSig = (Signal)it.next();
//
//			String signalName = sSig.getFullName();
//			if (netName.equalsIgnoreCase(signalName)) return sSig;
//
//			// if the signal name has underscores, see if all alphabetic characters match
//			if (signalName.length() + 1 == netName.length() && netName.charAt(signalName.length()) == ']')
//			{
//				signalName += "_";
//			}
//			if (signalName.length() == netName.length() && signalName.indexOf('_') >= 0)
//			{
//				boolean matches = true;
//				for(int i=0; i<signalName.length(); i++)
//				{
//					char sigChar = signalName.charAt(i);
//					char netChar = netName.charAt(i);
//					if (TextUtils.isLetterOrDigit(sigChar) != TextUtils.isLetterOrDigit(netChar))
//					{
//						matches = false;
//						break;
//					}
//					if (TextUtils.isLetterOrDigit(sigChar) &&
//						TextUtils.canonicChar(sigChar) != TextUtils.canonicChar(netChar))
//					{
//						matches = false;
//						break;
//					}
//				}
//				if (matches) return sSig;
//			}
//		}
//		return null;
	}

	/**
	 * Method to convert a strength to an index value.
	 * The strengths are OFF_STRENGTH, NODE_STRENGTH, GATE_STRENGTH, and VDD_STRENGTH.
	 * The indices are integers that can be saved to disk.
	 * @param strength strength level.
	 * @return the index for that strength (0-based).
	 */
	public static int strengthToIndex(int strength) { return strength / 4; }

	/**
	 * Method to convert a strength index to a strength value.
	 * The strengths are OFF_STRENGTH, NODE_STRENGTH, GATE_STRENGTH, and VDD_STRENGTH.
	 * The indices of the strengths are integers that can be saved to disk.
	 * @param index a strength index (0-based).
	 * @return the equivalent strength.
	 */
	public static int indexToStrength(int index) { return index * 4; }

	/**
	 * Method to describe the level in a given state.
	 * A 'state' is a combination of a level and a strength.
	 * The levels are LOGIC_LOW, LOGIC_HIGH, LOGIC_X, and LOGIC_Z.
	 * @param state the given state.
	 * @return a description of the logic level in that state.
	 */
	public static String describeLevel(int state)
	{
		switch (state&Stimuli.LOGIC)
		{
			case Stimuli.LOGIC_LOW: return "low";
			case Stimuli.LOGIC_HIGH: return "high";
			case Stimuli.LOGIC_X: return "undefined";
			case Stimuli.LOGIC_Z: return "floating";
		}
		return "?";
	}

	/**
	 * Method to describe the level in a given state, with only 1 character.
	 * A 'state' is a combination of a level and a strength.
	 * The levels are LOGIC_LOW, LOGIC_HIGH, LOGIC_X, and LOGIC_Z.
	 * @param state the given state.
	 * @return a description of the logic level in that state.
	 */
	public static String describeLevelBriefly(int state)
	{
		switch (state&Stimuli.LOGIC)
		{
			case Stimuli.LOGIC_LOW: return "L";
			case Stimuli.LOGIC_HIGH: return "H";
			case Stimuli.LOGIC_X: return "X";
			case Stimuli.LOGIC_Z: return "Z";
		}
		return "?";
	}

	/**
	 * Method to convert a state representation (L, H, X, Z) to a state
	 * @param s1 character string that contains state value.
	 * @return the state value.
	 */
	public static int parseLevel(String s1)
	{
		if (s1.length() > 0)
		{
			switch (s1.charAt(0))
			{
				case 'L': case 'l': return Stimuli.LOGIC_LOW;
				case 'X': case 'x': return Stimuli.LOGIC_X;
				case 'H': case 'h': return Stimuli.LOGIC_HIGH;
				case 'Z': case 'z': return Stimuli.LOGIC_Z;
			}
		}
		return Stimuli.LOGIC_X;
	}

	/**
	 * Method to describe the strength in a given state.
	 * A 'state' is a combination of a level and a strength.
	 * The strengths are OFF_STRENGTH, NODE_STRENGTH, GATE_STRENGTH, and VDD_STRENGTH.
	 * @param strength the given strength.
	 * @return a description of the strength in that state.
	 */
	public static String describeStrength(int strength)
	{
		switch (strength&Stimuli.STRENGTH)
		{
			case Stimuli.OFF_STRENGTH: return "off";
			case Stimuli.NODE_STRENGTH: return "node";
			case Stimuli.GATE_STRENGTH: return "gate";
			case Stimuli.VDD_STRENGTH: return "power";
		}
		return "?";
	}
}
