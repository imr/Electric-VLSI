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

	private Cell cell;
	private FileType type;
	private URL fileURL;
	private List signals;
	private List sweeps;
	private char separatorChar;
	private double [] commonTime;
	private List sweepCommonTime;
	private WaveformWindow ww;

	/**
	 * Class to define a signal in the simulation waveform window.
	 * This is a superclass for specific signal types: digital or analog.
	 */
	public static class Signal
	{
		private String signalName;
		private String signalContext;
		private Stimuli sd;
		private boolean useCommonTime;
		protected Rectangle2D bounds;
		private boolean boundsCurrent;
		private double [] time;
		private List bussedSignals;
		public List tempList;		// used only in the Verilog reader
		private boolean partOfBus;

		/**
		 * Constructor for a simulation signal.
		 * @param sd the Simulation Data object in which this signal will reside.
		 */
		private Signal(Stimuli sd)
		{
			this.sd = sd;
			useCommonTime = true;
			boundsCurrent = false;
			partOfBus = false;
			if (sd != null) sd.signals.add(this);
		}

		/**
		 * Method to set the name of this simulation signal.
		 * The name does not include any hierarchical path information: it is just a simple name.
		 * @param signalName the name of this simulation signal.
		 */
		public void setSignalName(String signalName) { this.signalName = signalName; }

		/**
		 * Method to return the name of this simulation signal.
		 * The name does not include any hierarchical path information: it is just a simple name.
		 * @return the name of this simulation signal.
		 */
		public String getSignalName() { return signalName; }

		/**
		 * Method to return the context of this simulation signal.
		 * The context is the hierarchical path to the signal, and it usually contains
		 * instance names of cells farther up the hierarchy, all separated by dots.
		 * @param signalContext the context of this simulation signal.
		 */
		public void setSignalContext(String signalContext) { this.signalContext = signalContext; }

		/**
		 * Method to return the context of this simulation signal.
		 * The context is the hierarchical path to the signal, and it usually contains
		 * instance names of cells farther up the hierarchy, all separated by dots.
		 * @return the context of this simulation signal.
		 * If there is no context, this returns null.
		 */
		public String getSignalContext() { return signalContext; }

		/**
		 * Method to return the full name of this simulation signal.
		 * The full name includes the context, if any.
		 * @return the full name of this simulation signal.
		 */
		public String getFullName()
		{
			if (signalContext != null) return signalContext + "." + signalName;
			return signalName;
		}

		/**
		 * Method to return the number of events in this signal.
		 * This is the number of events along the horizontal axis, usually "time".
		 * This superclass method must be overridden by a subclass that actually has data.
		 * @return the number of events in this signal.
		 */
		public int getNumEvents() { return 0; }

		/**
		 * Method to request that this signal be a bus.
		 * Builds the necessary data structures to hold bus information.
		 */
		public void buildBussedSignalList() { bussedSignals = new ArrayList(); }

		/**
		 * Method to return a List of signals on this bus signal.
		 * Each entry in the List points to another simulation signal that is on this bus.
		 * @return a List of signals on this bus signal.
		 */
		public List getBussedSignals() { return bussedSignals; }

		/**
		 * Method to add a signal to this bus signal.
		 * @param ws a single-wire signal to be added to this bus signal.
		 */
		public void addToBussedSignalList(Signal ws)
		{
			bussedSignals.add(ws);
			ws.partOfBus = true;
		}

		/**
		 * Method to tell whether this signal is part of a bus.
		 * @return true if this signal is part of a bus.
		 */
		public boolean isInBus() { return partOfBus; }

		/**
		 * Method to build a time vector for this signal.
		 * Signals can have their own time information, or they can use a "common time" array
		 * that is part of the simulation data.
		 * If using common time, then each signal must have the same number of entries, and
		 * each entry must be at the same time as the corresponding entries in other signals.
		 * Using common time saves memory, because the time information does not have to be
		 * stored with each signal.
		 * This method requests that the signal have its own time array, and not use common time data.
		 * @param numEvents the number of events on this signal (the length of the time array).
		 */
		public void buildTime(int numEvents) { useCommonTime = false;   time = new double[numEvents]; }

		/**
		 * Method to return the value of time for a given event on this signal.
		 * Depending on whether common time data is being used, the time information is
		 * found on this signal or on the overall simulation data.
		 * @param index the event being querried (0-based).
		 * @return the value of time at that event.
		 */
		public double getTime(int index)
		{
			if (useCommonTime) return sd.commonTime[index];
			return time[index];
		}

		/**
		 * Method to return the value of time for a given event on this signal.
		 * Depending on whether common time data is being used, the time information is
		 * found on this signal or on the overall simulation data.
		 * @param index the event being querried (0-based).
		 * @return the value of time at that event.
		 */
		public double getTime(int index, int sweep)
		{
			if (useCommonTime)
			{
				double [] sct = (double [])sd.sweepCommonTime.get(sweep);
				return sct[index];
			}
			return time[index];
		}

		/**
		 * Method to return the time vector for this signal.
		 * The vector is only valid if this signal is NOT using common time.
		 * Signals can have their own time information, or they can use a "common time" array
		 * that is part of the simulation data.
		 * If using common time, then each signal must have the same number of entries, and
		 * each entry must be at the same time as the corresponding entries in other signals.
		 * @return the time array for this signal.
		 * Returns null if this signal uses common time.
		 */
		public double [] getTimeVector() { return time; }

		/**
		 * Method to set the time vector for this signal.
		 * Overrides any previous time vector that may be on this signal.
		 * Signals can have their own time information, or they can use a "common time" array
		 * that is part of the simulation data.
		 * If using common time, then each signal must have the same number of entries, and
		 * each entry must be at the same time as the corresponding entries in other signals.
		 * @param time a new time vector for this signal.
		 */
		public void setTimeVector(double [] time) { useCommonTime = false;   this.time = time; }

		/**
		 * Method to set an individual time entry for this signal.
		 * Only applies if common time is NOT being used for this signal.
		 * Signals can have their own time information, or they can use a "common time" array
		 * that is part of the simulation data.
		 * If using common time, then each signal must have the same number of entries, and
		 * each entry must be at the same time as the corresponding entries in other signals.
		 * @param entry the entry in the event array of this signal (0-based).
		 * @param t the new value of time at this event.
		 */
		public void setTime(int entry, double t) { time[entry] = t; }

		/**
		 * Method to compute the time and value bounds of this simulation signal.
		 * @return a Rectangle2D that has time bounds in the X part and
		 * value bounds in the Y part.
		 * For digital signals, the Y part is simply 0 to 1.
		 */
		public Rectangle2D getBounds()
		{
			if (!boundsCurrent)
			{
				calcBounds();
				boundsCurrent = true;
			}
			return bounds;
		}

		protected void calcBounds() {}
	}

	/**
	 * Class to define an analog signal in the simulation waveform window.
	 */
	public static class AnalogSignal extends Signal
	{
		/** a simple analog signal */			private static final int BASICSIGNAL    = 0;
		/** a swept analog analog signal */		private static final int SWEEPSIGNAL    = 1;
		/** an interval analog signal */		private static final int INTERVALSIGNAL = 2;

		private int signalType;
		private double [] values;
		private double [][] sweepValues;
		private double [] highIntervalValues;

		/**
		 * Constructor for an analog signal.
		 * @param sd the Simulation Data object in which this signal will reside.
		 */
		public AnalogSignal(Stimuli sd) { super(sd); }

		/**
		 * Method to initialize this as a basic simulation signal with a specified number of events.
		 * Allocates an array to hold those events.
		 * @param numEvents the number of events in this signal.
		 */
		public void buildValues(int numEvents)
		{
			signalType = BASICSIGNAL;
			values = new double[numEvents];
		}

		/**
		 * Method to initialize this as a sweep simulation signal with a specified number of sweeps and events.
		 * Allocates arrays to hold those events.
		 * @param numSweeps the number of sweeps in this signal.
		 */
		public void setNumSweeps(int numSweeps)
		{
			signalType = SWEEPSIGNAL;
			sweepValues = new double[numSweeps][];
		}

		/**
		 * Method to initialize this as a sweep simulation signal with a specified number of sweeps and events.
		 * Allocates arrays to hold those events.
		 * @param sweep the sweep number in this signal.
		 * @param numEvents the number of events in this sweep of this signal.
		 */
		public void buildSweepValues(int sweep, int numEvents)
		{
			sweepValues[sweep] = new double[numEvents];
		}

		/**
		 * Method to initialize this as an interval simulation signal with a specified number of events.
		 * Allocates arrays to hold those events.
		 * @param numEvents the number of events in this signal.
		 */
		public void buildIntervalValues(int numEvents)
		{
			signalType = INTERVALSIGNAL;
			values = new double[numEvents];
			highIntervalValues = new double[numEvents];
		}

		/**
		 * Method to set the value of this signal at a given event index.
		 * @param index the event index (0-based).
		 * @param value the value to set at the given event index.
		 * If this signal is not a basic signal, print an error message.
		 */
		public void setValue(int index, double value)
		{
			if (signalType != BASICSIGNAL)
			{
				System.out.println("Setting complex data into basic signal");
				return;
			}
			values[index] = value;
		}

		/**
		 * Method to set the value of this signal at a given sweep and event index.
		 * @param sweep the sweep number (0-based).
		 * @param index the event index (0-based).
		 * @param value the value to set at the given sweep and event index.
		 * If this signal is not a sweep signal, print an error message.
		 */
		public void setSweepValue(int sweep, int index, double value)
		{
			if (signalType != SWEEPSIGNAL)
			{
				System.out.println("Setting sweep data into non-sweep signal");
				return;
			}
			sweepValues[sweep][index] = value;
		}

		/**
		 * Method to set the low and high values of this signal at a given event index.
		 * @param index the event index (0-based).
		 * @param lowValue the low value to set at the given event index.
		 * @param highValue the high value to set at the given event index.
		 * If this signal is not an interval signal, print an error message.
		 */
		public void setIntervalValue(int index, double lowValue, double highValue)
		{
			if (signalType != INTERVALSIGNAL)
			{
				System.out.println("Setting interval data into non-interval signal");
				return;
			}
			values[index] = lowValue;
			highIntervalValues[index] = highValue;
		}

		/**
		 * Method to return the value of this signal at a given event index.
		 * @param index the event index (0-based).
		 * @return the value of this signal at the given event index.
		 * If this signal is not a basic signal, return 0 and print an error message.
		 */
		public double getValue(int index)
		{
			if (signalType != BASICSIGNAL)
			{
				System.out.println("Getting basic data from non-basic signal");
				return 0;
			}
			return values[index];
		}

		/**
		 * Method to return the value of this signal for a given sweep and event index.
		 * @param sweep the sweep number (0-based).
		 * @param index the event index (0-based).
		 * @return the value of this signal at the given sweep and event index.
		 * If this signal is not a sweep signal, return 0 and print an error message.
		 */
		public double getSweepValue(int sweep, int index)
		{
			if (signalType != SWEEPSIGNAL)
			{
				System.out.println("Getting sweep data from non-sweep signal");
				return 0;
			}
			return sweepValues[sweep][index];
		}

		/**
		 * Method to return the low end of the interval range for this signal at a given event index.
		 * @param index the event index (0-based).
		 * @return the low end of the interval range at that event index.
		 * If this signal is not an interval signal, return 0 and print an error message.
		 */
		public double getIntervalLowValue(int index)
		{
			if (signalType != INTERVALSIGNAL)
			{
				System.out.println("Getting interval data from non-interval signal");
				return 0;
			}
			return values[index];
		}

		/**
		 * Method to return the high end of the interval range for this signal at a given event index.
		 * @param index the event index (0-based).
		 * @return the high end of the interval range at that event index.
		 * If this signal is not an interval signal, return 0 and print an error message.
		 */
		public double getIntervalHighValue(int index)
		{
			if (signalType != INTERVALSIGNAL)
			{
				System.out.println("Getting interval data from non-interval signal");
				return 0;
			}
			return highIntervalValues[index];
		}

		/**
		 * Method to return the number of events in this signal.
		 * This is the number of events along the horizontal axis, usually "time".
		 * @return the number of events in this signal.
		 */
		public int getNumEvents()
		{
			switch (signalType)
			{
				case BASICSIGNAL:
					return values.length;
				case SWEEPSIGNAL:
					return sweepValues[0].length;
				case INTERVALSIGNAL:
					return values.length;
			}
			return 0;
		}

		/**
		 * Method to return the number of events in one sweep of this signal.
		 * This is the number of events along the horizontal axis, usually "time".
		 * The method only works for sweep signals.
		 * @param sweep the sweep number to query.
		 * @return the number of events in this signal.
		 */
		public int getNumEvents(int sweep)
		{
			if (signalType != SWEEPSIGNAL) return 0;
			return sweepValues[sweep].length;
		}

		/**
		 * Method to return the number of sweeps in this signal.
		 * @return the number of sweeps in this signal.
		 * If this signal is not a sweep signal, returns 0.
		 */
		public int getNumSweeps()
		{
			if (signalType != SWEEPSIGNAL) return 0;
			return sweepValues.length;
		}

		/**
		 * Method to tell whether this signal is a basic analog waveform signal.
		 * Signals can be basic (single value), sweep (multiple value), or interval (value range).
		 * @return true if this is a basic analog waveform signal.
		 */
		public boolean isBasic() { return signalType == BASICSIGNAL; }

		/**
		 * Method to tell whether this signal is a sweep analog waveform signal.
		 * Signals can be basic (single value), sweep (multiple value), or interval (value range).
		 * @return true if this is a sweep analog waveform signal.
		 */
		public boolean isSweep() { return signalType == SWEEPSIGNAL; }

		/**
		 * Method to tell whether this signal is an interval analog waveform signal.
		 * Signals can be basic (single value), sweep (multiple value), or interval (value range).
		 * @return true if this is an interval analog waveform signal.
		 */
		public boolean isInterval() { return signalType == INTERVALSIGNAL; }

		/**
		 * Method to compute the low and high range of time and value on this signal.
		 * The result is stored in the "bounds" field variable.
		 */
		protected void calcBounds()
		{
			// determine extent of the data
			double lowTime=0, highTime=0, lowValue=0, highValue=0;
			boolean first = true;
			if (isBasic())
			{
				for(int i=0; i<values.length; i++)
				{
					double time = getTime(i);
					double value = values[i];
					if (first)
					{
						first = false;
						lowTime = highTime = time;
						lowValue = highValue = value;
					} else
					{
						if (time < lowTime) lowTime = time;
						if (time > highTime) highTime = time;
						if (value < lowValue) lowValue = value;
						if (value > highValue) highValue = value;
					}
				}
			} else if (isSweep())
			{
				for(int s=0; s<sweepValues.length; s++)
				{
					for(int i=0; i<sweepValues[s].length; i++)
					{
						double time = getTime(i, s);
						double value = sweepValues[s][i];
						if (first)
						{
							first = false;
							lowTime = highTime = time;
							lowValue = highValue = value;
						} else
						{
							if (time < lowTime) lowTime = time;
							if (time > highTime) highTime = time;
							if (value < lowValue) lowValue = value;
							if (value > highValue) highValue = value;
						}
					}
				}
			} else if (isInterval())
			{
				for(int i=0; i<values.length; i++)
				{
					double time = getTime(i);
					double lowVal = values[i];
					double highVal = values[i];
					if (first)
					{
						first = false;
						lowTime = highTime = time;
						lowValue = lowVal;
						highValue = highVal;
					} else
					{
						if (time < lowTime) lowTime = time;
						if (time > highTime) highTime = time;
						if (lowVal < lowValue) lowValue = lowVal;
						if (highVal > highValue) highValue = highVal;
					}
				}
			}
			bounds = new Rectangle2D.Double(lowTime, lowValue, highTime-lowTime, highValue-lowValue);
		}
	}

	/**
	 * Class to define a digital signal in the simulation waveform window.
	 */
	public static class DigitalSignal extends Signal
	{
		private int [] state;

		/**
		 * Constructor for a digital signal.
		 * @param sd the Simulation Data object in which this signal will reside.
		 */
		public DigitalSignal(Stimuli sd) { super(sd); }

		/**
		 * Method to initialize this simulation signal with a specified number of events.
		 * Allocates an array to hold those events.
		 * @param numEvents the number of events in this signal.
		 */
		public void buildState(int numEvents) { state = new int[numEvents]; }

		/**
		 * Method to set the state of this signal at a given event.
		 * @param index the event index (0-based).
		 * @param st the state of the signal at that event.
		 */
		public void setState(int index, int st) { state[index] = st; }

		/**
		 * Method to get the state of this signal at a given event.
		 * @param index the event index (0-based).
		 * @return the state of the signal at that event.
		 */
		public int getState(int index) { return state[index]; }

		/**
		 * Method to return the state information for all events in this signal.
		 * @return the state array for this signal.
		 */
		public int [] getStateVector() { return state; }

		/**
		 * Method to set the state information for all events in this signal.
		 * @param state an array of state information for every event on this signal.
		 */
		public void setStateVector(int [] state) { this.state = state; }

		/**
		 * Method to return the number of events in this signal.
		 * This is the number of events along the horizontal axis, usually "time".
		 * @return the number of events in this signal.
		 */
		public int getNumEvents() { return state.length; }

		/**
		 * Method to compute the low and high range of time value on this signal.
		 * The result is stored in the "bounds" field variable.
		 */
		protected void calcBounds()
		{
			boolean first = true;
			double lowTime = 0, highTime = 0;
			if (state != null)
			{
				for(int i=0; i<state.length; i++)
				{
					double time = getTime(i);
					if (first)
					{
						first = false;
						lowTime = highTime = time;
					} else
					{
						if (time < lowTime) lowTime = time;
						if (time > highTime) highTime = time;
					}
				}
			}
			bounds = new Rectangle2D.Double(lowTime, 0, highTime-lowTime, 1);
		}
	}

	/**
	 * Constructor to build a new Simulation Data object.
	 */
	public Stimuli()
	{
		signals = new ArrayList();
		sweeps = new ArrayList();
		sweepCommonTime = new ArrayList();
		separatorChar = '.';
	}

	/**
	 * Method to get the list of signals in this Simulation Data object.
	 * @return a List of signals.
	 */
	public List getSignals() { return signals; }

	/**
	 * Method to add a new signal to this Simulation Data object.
	 * Signals can be either digital or analog.
	 * @param ws the signal to add.
	 * Instead of a "Signal", use either DigitalSignal or AnalogSignal.
	 */
	public void addSignal(Signal ws) { signals.add(ws); }

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
	 * Method to add information about another sweep in this simulation data.
	 * @param obj sweep information (typically a Double).
	 */
	public void addSweep(Object obj) { sweeps.add(obj); }

	/**
	 * Method to return the list of sweep information in this simulation data.
	 * @return a list of sweep information in this simulation data.
	 * If there is no sweep information, the list is empty.
	 */
	public List getSweepList() { return sweeps; }

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
		for(Iterator it = signals.iterator(); it.hasNext(); )
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
	 * Method to return the signal that corresponds to a given Network.
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
						Character.toLowerCase(sigChar) != Character.toLowerCase(netChar))
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
