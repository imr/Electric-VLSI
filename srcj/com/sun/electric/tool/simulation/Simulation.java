/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Simulation.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.io.output.Verilog;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.ui.WaveformWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is the Simulation Interface tool.
 */
public class Simulation extends Tool
{
	/** the Simulation tool. */		public static Simulation tool = new Simulation();

	/** key of Variable holding rise time. */				public static final Variable.Key RISE_DELAY_KEY = ElectricObject.newKey("SIM_rise_delay");
	/** key of Variable holding fall time. */				public static final Variable.Key FALL_DELAY_KEY = ElectricObject.newKey("SIM_fall_delay");
	/** key of Variable holding flag for weak nodes. */		public static final Variable.Key WEAK_NODE_KEY = ElectricObject.newKey("SIM_weak_node");

	/**
	 * Class to define a set of simulation data.
	 * This class encapsulates all of the simulation data that is displayed in a waveform window.
	 * It includes the labels and values.
	 * It can handle digital, analog, and many variations (intervals, sweeps).
	 */
	public static class SimData
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
		private double [] commonTime;
		private List sweepCommonTime;

		/**
		 * Constructor to build a new Simulation Data object.
		 */
		public SimData()
		{
			signals = new ArrayList();
			sweeps = new ArrayList();
			sweepCommonTime = new ArrayList();
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
		 * Instead of a "SimSignal", use either SimDigitalSignal or SimAnalogSignal.
		 */
		public void addSignal(SimSignal ws) { signals.add(ws); }

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
		 * @param numSweeps the number of sweeps in the simulation data.
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
				SimSignal sig = (SimSignal)it.next();
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
				SimSignal sSig = (SimSignal)getSignals().get(0);
				if (sSig instanceof SimAnalogSignal) return true;
			}
			return false;
		}

		/**
		 * Method to return the signal that corresponds to a given Network.
		 * @param net the Network to find.
		 * @param context the context of these networks
		 * (a string to prepend to them to get the actual simulation signal name).
		 * @return the SimSignal that corresponds with the Network.
		 * Returns null if none can be found.
		 */
		public SimSignal findSignalForNetwork(String netName)
		{
			// look at all signal names in the cell
			for(Iterator it = getSignals().iterator(); it.hasNext(); )
			{
				SimSignal sSig = (SimSignal)it.next();

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

	/**
	 * Class to define a signal in the simulation waveform window.
	 * This is a superclass for specific signal types: digital or analog.
	 */
	public static class SimSignal
	{
		private String signalName;
		private String signalContext;
		private SimData sd;
		private boolean useCommonTime;
		private double [] time;
		private List bussedSignals;
		public List tempList;		// used only in the Verilog reader

		/**
		 * Constructor for a simulation signal.
		 * @param sd the Simulation Data object in which this signal will reside.
		 */
		private SimSignal(SimData sd)
		{
			this.sd = sd;
			useCommonTime = true;
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
		public void addToBussedSignalList(SimSignal ws) { bussedSignals.add(ws); }

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
		 */
		public Rectangle2D getBounds()
		{
			// determine extent of the data
			double lowTime=0, highTime=0, lowValue=0, highValue=0;
			boolean first = true;
			if (this instanceof SimAnalogSignal)
			{
				SimAnalogSignal as = (SimAnalogSignal)this;
				if (as.isBasic())
				{
					for(int i=0; i<as.values.length; i++)
					{
						double time = 0;
						time = as.getTime(i);
						if (first)
						{
							first = false;
							lowTime = highTime = time;
							lowValue = highValue = as.values[i];
						} else
						{
							if (time < lowTime) lowTime = time;
							if (time > highTime) highTime = time;
							if (as.values[i] < lowValue) lowValue = as.values[i];
							if (as.values[i] > highValue) highValue = as.values[i];
						}
					}
				} else if (as.isSweep())
				{
					for(int s=0; s<as.sweepValues.length; s++)
					{
						for(int i=0; i<as.sweepValues[s].length; i++)
						{
							double time = 0;
							time = as.getTime(i, s);
							double value = as.sweepValues[s][i];
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
				} else if (as.isInterval())
				{
					for(int i=0; i<as.values.length; i++)
					{
						double time = 0;
						time = as.getTime(i);
						double lowVal = as.values[i];
						double highVal = as.values[i];
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
			} else if (this instanceof SimDigitalSignal)
			{
				SimDigitalSignal ds = (SimDigitalSignal)this;
				if (ds.state != null)
				{
					for(int i=0; i<ds.state.length; i++)
					{
						double time = 0;
						time = ds.getTime(i);
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
			}
			return new Rectangle2D.Double(lowTime, lowValue, highTime-lowTime, highValue-lowValue);
		}
	}

	/**
	 * Class to define an analog signal in the simulation waveform window.
	 */
	public static class SimAnalogSignal extends SimSignal
	{
		/** a simple analog signal */			private static final int BASICSIGNAL = 0;
		/** a swept analog analog signal */		private static final int SWEEPSIGNAL = 1;
		/** an interval analog signal */		private static final int INTERVALSIGNAL = 2;

		private double [] values;
		private double [][] sweepValues;
		private double [] highIntervalValues;
		private int signalType;

		/**
		 * Constructor for an analog signal.
		 * @param sd the Simulation Data object in which this signal will reside.
		 */
		public SimAnalogSignal(SimData sd) { super(sd); }

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
		 * Method to return the number of events in this signal.
		 * This is the number of events along the horizontal axis, usually "time".
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
		 * Method to compute the low and high range of values on this signal.
		 * @return a Point2D where X is the low value and Y is the high value.
		 */
		public Point2D getRangeOfValues()
		{
			double lowValue = 0, highValue = 0;
			switch (signalType)
			{
				case BASICSIGNAL:
					for(int i=0; i<getNumEvents(); i++)
					{
						double val = getValue(i);
						if (i == 0) lowValue = highValue = val; else
						{
							if (val < lowValue) lowValue = val;
							if (val > highValue) highValue = val;
						}
					}
					break;
				case SWEEPSIGNAL:
					for(int s=0; s<getNumSweeps(); s++)
					{
						for(int i=0; i<getNumEvents(); i++)
						{
							double val = getSweepValue(s, i);
							if (s == 0 && i == 0) lowValue = highValue = val; else
							{
								if (val < lowValue) lowValue = val;
								if (val > highValue) highValue = val;
							}
						}
					}
					break;
				case INTERVALSIGNAL:
					for(int i=0; i<getNumEvents(); i++)
					{
						double lowVal = getIntervalLowValue(i);
						double highVal = getIntervalHighValue(i);
						if (i == 0)
						{
							lowValue = lowVal;
							highValue = highVal;
						} else
						{
							if (lowVal < lowValue) lowValue = lowVal;
							if (highVal > highValue) highValue = highVal;
						}
					}
					break;
			}
			return new Point2D.Double(lowValue, highValue);
		}
	}

	/**
	 * Class to define a digital signal in the simulation waveform window.
	 */
	public static class SimDigitalSignal extends SimSignal
	{
		private int [] state;

		/**
		 * Constructor for a digital signal.
		 * @param sd the Simulation Data object in which this signal will reside.
		 */
		public SimDigitalSignal(SimData sd) { super(sd); }

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
	}

	/**
	 * The constructor sets up the Simulation tool.
	 */
	private Simulation()
	{
		super("simulation");
	}

	/**
	 * Method to initialize the Simulation tool.
	 */
	public void init()
	{
//		setOn();
	}

	/**
	 * Method to set a Spice model on the selected node.
	 */
	public static void setSpiceModel()
	{
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;
        Highlighter highlighter = wf.getContent().getHighlighter();
        if (highlighter == null) return;

		NodeInst ni = (NodeInst)highlighter.getOneElectricObject(NodeInst.class);
		if (ni == null) return;
		SetSpiceModel job = new SetSpiceModel(ni);
	}

	/**
	 * Class to set a Spice Model in a new thread.
	 */
	private static class SetSpiceModel extends Job
	{
		NodeInst ni;
		protected SetSpiceModel(NodeInst ni)
		{
			super("Set Spice Model", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			startJob();
		}

		public boolean doIt()
		{
			Variable var = ni.newVar(Spice.SPICE_MODEL_KEY, "SPICE-Model");
			var.setDisplay(true);
			return true;
		}
	}

	/**
	 * Method to set the type of the currently selected wires.
	 * This is used by the Verilog netlister.
	 * @param type 0 for wire; 1 for trireg; 2 for default.
	 */
	public static void setVerilogWireCommand(int type)
	{
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;
        Highlighter highlighter = wf.getContent().getHighlighter();
        if (highlighter == null) return;

		List list = highlighter.getHighlightedEObjs(false, true);
		if (list.size() == 0)
		{
			System.out.println("Must select arcs before setting their type");
			return;
		}
		SetWireType job = new SetWireType(list, type);
	}

	/**
	 * Class to set Verilog wire types in a new thread.
	 */
	private static class SetWireType extends Job
	{
		List list;
		int type;
		protected SetWireType(List list, int type)
		{
			super("Change Verilog Wire Types", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.list = list;
			this.type = type;
			startJob();
		}

		public boolean doIt()
		{
			for(Iterator it = list.iterator(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				switch (type)
				{
					case 0:		// set to "wire"
						Variable var = ai.newVar(Verilog.WIRE_TYPE_KEY, "wire");
						var.setDisplay(true);
						break;
					case 1:		// set to "trireg"
						var = ai.newVar(Verilog.WIRE_TYPE_KEY, "trireg");
						var.setDisplay(true);
						break;
					case 2:		// set to default
						if (ai.getVar(Verilog.WIRE_TYPE_KEY) != null)
							ai.delVar(Verilog.WIRE_TYPE_KEY);
						break;
				}
			}
			return true;
		}
	}

	/**
	 * Method to set the strength of the currently selected transistor.
	 * This is used by the Verilog netlister.
	 * @param weak true to set the currently selected transistor to be weak.
	 * false to make it normal strength.
	 */
	public static void setTransistorStrengthCommand(boolean weak)
	{
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;
        Highlighter highlighter = wf.getContent().getHighlighter();
        if (highlighter == null) return;

		NodeInst ni = (NodeInst)highlighter.getOneElectricObject(NodeInst.class);
		if (ni == null) return;
		SetTransistorStrength job = new SetTransistorStrength(ni, weak);
	}

	/**
	 * Class to set transistor strengths in a new thread.
	 */
	private static class SetTransistorStrength extends Job
	{
		NodeInst ni;
		boolean weak;
		protected SetTransistorStrength(NodeInst ni, boolean weak)
		{
			super("Change Transistor Strength", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			this.weak = weak;
			startJob();
		}

		public boolean doIt()
		{
			if (weak)
			{
				Variable var = ni.newVar(Simulation.WEAK_NODE_KEY, "Weak");
				var.setDisplay(true);
			} else
			{
				if (ni.getVar(Simulation.WEAK_NODE_KEY) != null)
					ni.delVar(Simulation.WEAK_NODE_KEY);
			}
			return true;
		}
	}

	public static void showSimulationData(SimData sd, WaveformWindow ww)
	{
		// if the window already exists, update the data
		if (ww != null)
		{
			ww.setSimData(sd);
			return;
		}

		// determine extent of the data
		Rectangle2D bounds = sd.getBounds();
		double lowTime = bounds.getMinX();
		double highTime = bounds.getMaxX();
		double lowValue = bounds.getMinY();
		double highValue = bounds.getMaxY();
		double timeRange = highTime - lowTime;

		WindowFrame wf = WindowFrame.createWaveformWindow(sd);
		ww = (WaveformWindow)wf.getContent();
		ww.setMainTimeCursor(timeRange*0.2 + lowTime);
		ww.setExtensionTimeCursor(timeRange*0.8 + lowTime);
		ww.setDefaultTimeRange(lowTime, highTime);

		if (sd.cell != null)
		{
			Variable var = sd.cell.getVar(WaveformWindow.WINDOW_SIGNAL_ORDER);
			if (var != null && var.getObject() instanceof String[])
			{
				// load the window with previous signal set
				String [] signalNames = (String [])var.getObject();
				boolean isAnalog = sd.isAnalog();
				boolean showedSomething = false;
				for(int i=0; i<signalNames.length; i++)
				{
					String signalName = signalNames[i];
					WaveformWindow.Panel wp = null;
					boolean firstSignal = true;

					// add signals to the panel
					int start = 0;
					for(;;)
					{
						int tabPos = signalName.indexOf('\t', start);
						String sigName = null;
						if (tabPos < 0) sigName = signalName.substring(start); else
						{
							sigName = signalName.substring(start, tabPos);
							start = tabPos+1;
						}
						for(int j=0; j<sd.signals.size(); j++)
						{
							Simulation.SimSignal sSig = (Simulation.SimSignal)sd.signals.get(j);
							String aSigName = sSig.getSignalName();
							if (sSig.getSignalContext() != null) aSigName = sSig.getSignalContext() + aSigName;
							if (sigName.equals(aSigName))
							{
								if (firstSignal)
								{
									firstSignal = false;
									wp = new WaveformWindow.Panel(ww, isAnalog);
									if (isAnalog) wp.setValueRange(lowValue, highValue);
									wp.makeSelectedPanel();
									showedSomething = true;
								}
								new WaveformWindow.Signal(wp, sSig);
								break;
							}
						}
						if (tabPos < 0) break;
					}
				}
				if (showedSomething) return;
			}
		}

		// put the first waveform panels in it
		if (sd.signals.size() > 0)
		{
			Simulation.SimSignal sSig = (Simulation.SimSignal)sd.signals.get(0);
			boolean isAnalog = false;
			if (sSig instanceof SimAnalogSignal) isAnalog = true;
			if (isAnalog)
			{
				WaveformWindow.Panel wp = new WaveformWindow.Panel(ww, isAnalog);
				wp.setValueRange(lowValue, highValue);
				wp.makeSelectedPanel();
			} else
			{
				// put all top-level signals in
				for(int i=0; i<sd.signals.size(); i++)
				{
					Simulation.SimDigitalSignal sDSig = (Simulation.SimDigitalSignal)sd.signals.get(i);
					if (sDSig.getSignalContext() != null) continue;
					WaveformWindow.Panel wp = new WaveformWindow.Panel(ww, false);
					wp.makeSelectedPanel();
					new WaveformWindow.Signal(wp, sDSig);
				}
			}
		}
		ww.getPanel().validate();
	}

	/****************************** FAST HENRY OPTIONS ******************************/

	private static Pref cacheFastHenryUseSingleFrequency = Pref.makeBooleanPref("FastHenryUseSingleFrequency", Simulation.tool.prefs, false);
	/**
	 * Method to tell whether FastHenry deck generation should use a single frequency.
	 * The default is false.
	 * @return true if FastHenry deck generation should use a single frequency.
	 */
	public static boolean isFastHenryUseSingleFrequency() { return cacheFastHenryUseSingleFrequency.getBoolean(); }
	/**
	 * Method to set whether FastHenry deck generation should use a single frequency.
	 * @param s true if FastHenry deck generation should use a single frequency.
	 */
	public static void setFastHenryUseSingleFrequency(boolean s) { cacheFastHenryUseSingleFrequency.setBoolean(s); }

	private static Pref cacheFastHenryStartFrequency = Pref.makeDoublePref("FastHenryStartFrequency", Simulation.tool.prefs, 0);
	/**
	 * Method to return the FastHenry starting frequency (or only if using a single frequency).
	 * The default is 0.
	 * @return the FastHenry starting frequency (or only if using a single frequency).
	 */
	public static double getFastHenryStartFrequency() { return cacheFastHenryStartFrequency.getDouble(); }
	/**
	 * Method to set the FastHenry starting frequency (or only if using a single frequency).
	 * @param s the FastHenry starting frequency (or only if using a single frequency).
	 */
	public static void setFastHenryStartFrequency(double s) { cacheFastHenryStartFrequency.setDouble(s); }

	private static Pref cacheFastHenryEndFrequency = Pref.makeDoublePref("FastHenryEndFrequency", Simulation.tool.prefs, 0);
	/**
	 * Method to return the FastHenry ending frequency.
	 * The default is 0.
	 * @return the FastHenry ending frequency.
	 */
	public static double getFastHenryEndFrequency() { return cacheFastHenryEndFrequency.getDouble(); }
	/**
	 * Method to set the FastHenry ending frequency.
	 * @param e the FastHenry ending frequency.
	 */
	public static void setFastHenryEndFrequency(double e) { cacheFastHenryEndFrequency.setDouble(e); }

	private static Pref cacheFastHenryRunsPerDecade = Pref.makeIntPref("FastHenryRunsPerDecade", Simulation.tool.prefs, 1);
	/**
	 * Method to return the number of runs per decade for FastHenry deck generation.
	 * The default is 1.
	 * @return the number of runs per decade for FastHenry deck generation.
	 */
	public static int getFastHenryRunsPerDecade() { return cacheFastHenryRunsPerDecade.getInt(); }
	/**
	 * Method to set the number of runs per decade for FastHenry deck generation.
	 * @param r the number of runs per decade for FastHenry deck generation.
	 */
	public static void setFastHenryRunsPerDecade(int r) { cacheFastHenryRunsPerDecade.setInt(r); }
	
	private static Pref cacheFastHenryMultiPole = Pref.makeBooleanPref("FastHenryMultiPole", Simulation.tool.prefs, false);
	/**
	 * Method to tell whether FastHenry deck generation should make a multipole subcircuit.
	 * The default is false.
	 * @return true if FastHenry deck generation should make a multipole subcircuit.
	 */
	public static boolean isFastHenryMultiPole() { return cacheFastHenryMultiPole.getBoolean(); }
	/**
	 * Method to set whether FastHenry deck generation should make a multipole subcircuit.
	 * @param mp true if FastHenry deck generation should make a multipole subcircuit.
	 */
	public static void setFastHenryMultiPole(boolean mp) { cacheFastHenryMultiPole.setBoolean(mp); }

	private static Pref cacheFastHenryNumPoles = Pref.makeIntPref("FastHenryNumPoles", Simulation.tool.prefs, 20);
	/**
	 * Method to return the number of poles for FastHenry deck generation.
	 * The default is 20.
	 * @return the number of poles for FastHenry deck generation.
	 */
	public static int getFastHenryNumPoles() { return cacheFastHenryNumPoles.getInt(); }
	/**
	 * Method to set the number of poles for FastHenry deck generation.
	 * @param p the number of poles for FastHenry deck generation.
	 */
	public static void setFastHenryNumPoles(int p) { cacheFastHenryNumPoles.setInt(p); }

	private static Pref cacheFastHenryDefThickness = Pref.makeDoublePref("FastHenryDefThickness", Simulation.tool.prefs, 2);
	/**
	 * Method to return the FastHenry default wire thickness.
	 * The default is 2.
	 * @return the FastHenry default wire thickness.
	 */
	public static double getFastHenryDefThickness() { return cacheFastHenryDefThickness.getDouble(); }
	/**
	 * Method to set the FastHenry default wire thickness.
	 * @param t the FastHenry default wire thickness.
	 */
	public static void setFastHenryDefThickness(double t) { cacheFastHenryDefThickness.setDouble(t); }

	private static Pref cacheFastHenryWidthSubdivisions = Pref.makeIntPref("FastHenryWidthSubdivisions", Simulation.tool.prefs, 1);
	/**
	 * Method to return the default number of width subdivisions for FastHenry deck generation.
	 * The default is 1.
	 * @return the default number of width subdivisions for FastHenry deck generation.
	 */
	public static int getFastHenryWidthSubdivisions() { return cacheFastHenryWidthSubdivisions.getInt(); }
	/**
	 * Method to set the default number of width subdivisions for FastHenry deck generation.
	 * @param w the default number of width subdivisions for FastHenry deck generation.
	 */
	public static void setFastHenryWidthSubdivisions(int w) { cacheFastHenryWidthSubdivisions.setInt(w); }

	private static Pref cacheFastHenryHeightSubdivisions = Pref.makeIntPref("FastHenryHeightSubdivisions", Simulation.tool.prefs, 1);
	/**
	 * Method to return the default number of height subdivisions for FastHenry deck generation.
	 * The default is 1.
	 * @return the default number of height subdivisions for FastHenry deck generation.
	 */
	public static int getFastHenryHeightSubdivisions() { return cacheFastHenryHeightSubdivisions.getInt(); }
	/**
	 * Method to set the default number of height subdivisions for FastHenry deck generation.
	 * @param h the default number of height subdivisions for FastHenry deck generation.
	 */
	public static void setFastHenryHeightSubdivisions(int h) { cacheFastHenryHeightSubdivisions.setInt(h); }

	private static Pref cacheFastHenryMaxSegLength = Pref.makeDoublePref("FastHenryMaxSegLength", Simulation.tool.prefs, 0);
	/**
	 * Method to return the maximum segment length for FastHenry deck generation.
	 * The default is 0.
	 * @return the maximum segment length for FastHenry deck generation.
	 */
	public static double getFastHenryMaxSegLength() { return cacheFastHenryMaxSegLength.getDouble(); }
	/**
	 * Method to set the maximum segment length for FastHenry deck generation.
	 * @param s the maximum segment length for FastHenry deck generation.
	 */
	public static void setFastHenryMaxSegLength(double s) { cacheFastHenryMaxSegLength.setDouble(s); }

	/****************************** VERILOG OPTIONS ******************************/

	private static Pref cacheVerilogUseAssign = Pref.makeBooleanPref("VerilogUseAssign", Simulation.tool.prefs, false);
    static { cacheVerilogUseAssign.attachToObject(Simulation.tool, "Tools/Verilog tab", "Verilog uses Assign construct"); }
	/**
	 * Method to tell whether Verilog deck generation should use the Assign statement.
	 * The default is false.
	 * @return true if Verilog deck generation should use the Assign statement.
	 */
	public static boolean getVerilogUseAssign() { return cacheVerilogUseAssign.getBoolean(); }
	/**
	 * Method to set whether Verilog deck generation should use the Assign statement.
	 * @param use true if Verilog deck generation should use the Assign statement.
	 */
	public static void setVerilogUseAssign(boolean use) { cacheVerilogUseAssign.setBoolean(use); }

	private static Pref cacheVerilogUseTrireg = Pref.makeBooleanPref("VerilogUseTrireg", Simulation.tool.prefs, false);
    static { cacheVerilogUseTrireg.attachToObject(Simulation.tool, "Tools/Verilog tab", "Verilog presumes wire is Trireg"); }
	/**
	 * Method to tell whether Verilog deck generation should use Trireg by default.
	 * The alternative is to use the "wire" statement.
	 * The default is false.
	 * @return true if Verilog deck generation should use Trireg by default.
	 */
	public static boolean getVerilogUseTrireg() { return cacheVerilogUseTrireg.getBoolean(); }
	/**
	 * Method to set whether Verilog deck generation should use Trireg by default.
	 * The alternative is to use the "wire" statement.
	 * @param use true if Verilog deck generation should use Trireg by default.
	 */
	public static void setVerilogUseTrireg(boolean use) { cacheVerilogUseTrireg.setBoolean(use); }

	/****************************** CDL OPTIONS ******************************/

	private static Pref cacheCDLLibName = Pref.makeStringPref("CDLLibName", Simulation.tool.prefs, "");
//    static { cacheCDLLibName.attachToObject(Simulation.tool, "IO/CDL tab", "Cadence library name"); }
	/**
	 * Method to return the CDL library name.
	 * CDL is a weak form of a Spice deck, and it includes a Cadence library name.
	 * @return the CDL library name.
	 */
	public static String getCDLLibName() { return cacheCDLLibName.getString(); }
	/**
	 * Method to set the CDL library name.
	 * CDL is a weak form of a Spice deck, and it includes a Cadence library name.
	 * @param libName the CDL library name.
	 */
	public static void setCDLLibName(String libName) { cacheCDLLibName.setString(libName); }

	private static Pref cacheCDLLibPath = Pref.makeStringPref("CDLLibPath", Simulation.tool.prefs, "");
//    static { cacheCDLLibPath.attachToObject(Simulation.tool, "IO/CDL tab", "Cadence library path"); }
	/**
	 * Method to return the CDL library path.
	 * CDL is a weak form of a Spice deck, and it includes a Cadence library.
	 * @return the CDL library path.
	 */
	public static String getCDLLibPath() { return cacheCDLLibPath.getString(); }
	/**
	 * Method to set the CDL library path.
	 * CDL is a weak form of a Spice deck, and it includes a Cadence library.
	 * @param libName the CDL library path.
	 */
	public static void setCDLLibPath(String libName) { cacheCDLLibPath.setString(libName); }

	private static Pref cacheCDLConvertBrackets = Pref.makeBooleanPref("CDLConvertBrackets", Simulation.tool.prefs, false);
//    static { cacheCDLConvertBrackets.attachToObject(Simulation.tool, "IO/CDL tab", "CDL converts brackets"); }
	/**
	 * Method to tell whether CDL converts square bracket characters.
	 * CDL is a weak form of a Spice deck, and it includes a Cadence library.
	 * @return true if CDL converts square bracket characters.
	 */
	public static boolean isCDLConvertBrackets() { return cacheCDLConvertBrackets.getBoolean(); }
	/**
	 * Method to set if CDL converts square bracket characters.
	 * CDL is a weak form of a Spice deck, and it includes a Cadence library.
	 * @param c true if CDL converts square bracket characters.
	 */
	public static void setCDLConvertBrackets(boolean c) { cacheCDLConvertBrackets.setBoolean(c); }

	/****************************** SPICE OPTIONS ******************************/

	/** Spice 2 engine. */		public static final int SPICE_ENGINE_2 = 0;
	/** Spice 3 engine. */		public static final int SPICE_ENGINE_3 = 1;
	/** HSpice engine. */		public static final int SPICE_ENGINE_H = 2;
	/** PSpice engine. */		public static final int SPICE_ENGINE_P = 3;
	/** GNUCap engine. */		public static final int SPICE_ENGINE_G = 4;
	/** SmartSpice engine. */	public static final int SPICE_ENGINE_S = 5;

	private static Pref cacheSpiceEngine = Pref.makeIntPref("SpiceEngine", Simulation.tool.prefs, 1);
//	static
//	{
//		Pref.Meaning m = cacheSpiceEngine.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice engine");
//		m.setTrueMeaning(new String[] {"Spice 2", "Spice 3", "HSpice", "PSpice", "GNUCap", "SmartSpice"});
//	}
	/**
	 * Method to tell which SPICE engine is being used.
	 * Since different versions of SPICE have slightly different syntax,
	 * this is needed to tell the deck generator which variation to target.
	 * @return which SPICE engine is being used.
	 * These constants are available: <BR>
	 * Simulation.SPICE_ENGINE_2 for Spice 2.<BR>
	 * Simulation.SPICE_ENGINE_3 for Spice 3.<BR>
	 * Simulation.SPICE_ENGINE_H for HSpice.<BR>
	 * Simulation.SPICE_ENGINE_P for PSpice.<BR>
	 * Simulation.SPICE_ENGINE_G for GNUCap.<BR>
	 * Simulation.SPICE_ENGINE_S for Smart Spice.
	 */
	public static int getSpiceEngine() { return cacheSpiceEngine.getInt(); }
	/**
	 * Method to set which SPICE engine is being used.
	 * Since different versions of SPICE have slightly different syntax,
	 * this is needed to tell the deck generator which variation to target.
	 * @param engine which SPICE engine is being used.
	 * These constants are available: <BR>
	 * Simulation.SPICE_ENGINE_2 for Spice 2.<BR>
	 * Simulation.SPICE_ENGINE_3 for Spice 3.<BR>
	 * Simulation.SPICE_ENGINE_H for HSpice.<BR>
	 * Simulation.SPICE_ENGINE_P for PSpice.<BR>
	 * Simulation.SPICE_ENGINE_G for GNUCap.<BR>
	 * Simulation.SPICE_ENGINE_S for Smart Spice.
	 */
	public static void setSpiceEngine(int engine) { cacheSpiceEngine.setInt(engine); }

	private static Pref cacheSpiceLevel = Pref.makeStringPref("SpiceLevel", Simulation.tool.prefs, "1");
//    static { cacheSpiceLevel.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice level"); }
	/**
	 * Method to tell which SPICE level is being used.
	 * SPICE can use 3 different levels of simulation.
	 * @return which SPICE level is being used (1, 2, or 3).
	 */
	public static String getSpiceLevel() { return cacheSpiceLevel.getString(); }
	/**
	 * Method to set which SPICE level is being used.
	 * SPICE can use 3 different levels of simulation.
	 * @param level which SPICE level is being used (1, 2, or 3).
	 */
	public static void setSpiceLevel(String level) { cacheSpiceLevel.setString(level); }

	private static Pref cacheSpiceOutputFormat = Pref.makeStringPref("SpiceOutputFormat", Simulation.tool.prefs, "Standard");
//    static { cacheSpiceOutputFormat.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice output format"); }
	/**
	 * Method to tell the type of output files expected from Spice.
	 * @return the type of output files expected from Spice.
	 * The values are:<BR>
	 * "Standard": Standard output (the default)<BR>
	 * "Raw" Raw output<BR>
	 * "Raw/Smart": Raw output from SmartSpice<BR>
	 */
	public static String getSpiceOutputFormat() { return cacheSpiceOutputFormat.getString(); }
	/**
	 * Method to set the type of output files expected from Spice.
	 * @param format the type of output files expected from Spice.
	 * The values are:<BR>
	 * "Standard": Standard output (the default)<BR>
	 * "Raw" Raw output<BR>
	 * "Raw/Smart": Raw output from SmartSpice<BR>
	 */
	public static void setSpiceOutputFormat(String format) { cacheSpiceOutputFormat.setString(format); }

    public static final String spiceRunChoiceDontRun = "Don't Run";
    public static final String spiceRunChoiceRunIgnoreOutput = "Run, Ingore Output";
    public static final String spiceRunChoiceRunReportOutput = "Run, Report Output";
    private static final String [] spiceRunChoices = {spiceRunChoiceDontRun, spiceRunChoiceRunIgnoreOutput, spiceRunChoiceRunReportOutput};

    private static Pref cacheSpiceRunChoice = Pref.makeIntPref("SpiceRunChoice", Simulation.tool.prefs, 0);
//    static {
//        Pref.Meaning m = cacheSpiceRunChoice.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Spice Run Choice");
//        m.setTrueMeaning(new String[] {spiceRunChoiceDontRun, spiceRunChoiceRunIgnoreOutput, spiceRunChoiceRunReportOutput});
//    }
    /** Determines possible settings for the Spice Run Choice */
    public static String [] getSpiceRunChoiceValues() { return spiceRunChoices; }
    /** Get the current setting for the Spice Run Choice preference */
    public static String getSpiceRunChoice() { return spiceRunChoices[cacheSpiceRunChoice.getInt()]; }
    /** Set the setting for the Spice Run Choice preference. Ignored if invalid */
    public static void setSpiceRunChoice(String choice) {
        String [] values = getSpiceRunChoiceValues();
        for (int i=0; i<values.length; i++) {
            if (values[i].equals(choice)) { cacheSpiceRunChoice.setInt(i); return; }
        }
    }

    private static Pref cacheSpiceRunDir = Pref.makeStringPref("SpiceRunDir", Simulation.tool.prefs, "");
//    static { cacheSpiceRunDir.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Spice Run Dir"); }
    /** Get the spice run directory */
    public static String getSpiceRunDir() { return cacheSpiceRunDir.getString(); }
    /** Set the spice run directory */
    public static void setSpiceRunDir(String dir) { cacheSpiceRunDir.setString(dir); }

    private static Pref cacheSpiceUseRunDir = Pref.makeBooleanPref("SpiceUseRunDir", Simulation.tool.prefs, false);
//    static { cacheSpiceUseRunDir.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Use Run Dir"); }
    /** Get whether or not to use the user-specified spice run dir */
    public static boolean getSpiceUseRunDir() { return cacheSpiceUseRunDir.getBoolean(); }
    /** Set whether or not to use the user-specified spice run dir */
    public static void setSpiceUseRunDir(boolean b) { cacheSpiceUseRunDir.setBoolean(b); }

    private static Pref cacheSpiceOutputOverwrite = Pref.makeBooleanPref("SpiceOverwriteOutputFile", Simulation.tool.prefs, false);
//    static { cacheSpiceOutputOverwrite.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Overwrite Output Spice File"); }
    /** Get whether or not we automatically overwrite the spice output file */
    public static boolean getSpiceOutputOverwrite() { return cacheSpiceOutputOverwrite.getBoolean(); }
    /** Set whether or not we automatically overwrite the spice output file */
    public static void setSpiceOutputOverwrite(boolean b) { cacheSpiceOutputOverwrite.setBoolean(b); }

    private static Pref cacheSpiceRunProbe = Pref.makeBooleanPref("SpiceRunProbe", Simulation.tool.prefs, false);
    /** Get whether or not to run the spice probe after running spice */
    public static boolean getSpiceRunProbe() { return cacheSpiceRunProbe.getBoolean(); }
    /** Set whether or not to run the spice probe after running spice */
    public static void setSpiceRunProbe(boolean b) { cacheSpiceRunProbe.setBoolean(b); }

    private static Pref cacheSpiceRunProgram = Pref.makeStringPref("SpiceRunProgram", Simulation.tool.prefs, "");
//    static { cacheSpiceRunProgram.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Spice Run Program"); }
    /** Get the spice run program */
    public static String getSpiceRunProgram() { return cacheSpiceRunProgram.getString(); }
    /** Set the spice run program */
    public static void setSpiceRunProgram(String c) { cacheSpiceRunProgram.setString(c); }

    private static Pref cacheSpiceRunProgramArgs = Pref.makeStringPref("SpiceRunProgramArgs", Simulation.tool.prefs, "");
//    static { cacheSpiceRunProgramArgs.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Spice Run Program Args"); }
    /** Get the spice run program args */
    public static String getSpiceRunProgramArgs() { return cacheSpiceRunProgramArgs.getString(); }
    /** Set the spice run program args */
    public static void setSpiceRunProgramArgs(String c) { cacheSpiceRunProgramArgs.setString(c); }

	private static Pref cacheSpicePartsLibrary = null;
	/**
	 * Method to return the name of the current Spice parts library.
	 * The Spice parts library is a library of icons that are used in Spice.
	 * @return the name of the current Spice parts library.
	 */
	public static String getSpicePartsLibrary()
	{
		if (cacheSpicePartsLibrary == null)
		{
			String [] libNames = LibFile.getSpicePartsLibraries();
			cacheSpicePartsLibrary = Pref.makeStringPref("SpicePartsLibrary", Simulation.tool.prefs, libNames[0]);
		}
		return cacheSpicePartsLibrary.getString();
	}
	/**
	 * Method to set the name of the current Spice parts library.
	 * The Spice parts library is a library of icons that are used in Spice.
	 * @param parts the name of the new current Spice parts library.
	 */
	public static void setSpicePartsLibrary(String parts)
	{
		if (cacheSpicePartsLibrary == null)
		{
			String [] libNames = LibFile.getSpicePartsLibraries();
			cacheSpicePartsLibrary = Pref.makeStringPref("SpicePartsLibrary", Simulation.tool.prefs, libNames[0]);
		}
		cacheSpicePartsLibrary.setString(parts);
	}

	private static Pref cacheSpiceHeaderCardInfo = Pref.makeStringPref("SpiceHeaderCardInfo", Simulation.tool.prefs, "");
//    static { cacheSpiceHeaderCardInfo.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice header card information"); }
	/**
	 * Method to get the Spice header card specification.
	 * Header cards can come from one of three places, depending on the specification:<BR>
	 * Specification="" means use built-in header cards.<BR>
	 * Specification="Extension XXX" means use header cards from the file TOPCELL.XXX
	 * where TOPCELL is the name of the top-level cell name and XXX is a specified extension.<BR>
	 * Specification="XXX" means use header cards from the file XXX.
	 * @return the Spice header card specification.
	 */
	public static String getSpiceHeaderCardInfo() { return cacheSpiceHeaderCardInfo.getString(); }
	/**
	 * Method to set the Spice header card specification.
	 * Header cards can come from one of three places, depending on the specification:<BR>
	 * Specification="" means use built-in header cards.<BR>
	 * Specification="Extension XXX" means use header cards from the file TOPCELL.XXX
	 * where TOPCELL is the name of the top-level cell name and XXX is a specified extension.<BR>
	 * Specification="XXX" means use header cards from the file XXX.
	 * @param spec the Spice header card specification.
	 */
	public static void setSpiceHeaderCardInfo(String spec) { cacheSpiceHeaderCardInfo.setString(spec); }

	private static Pref cacheSpiceTrailerCardInfo = Pref.makeStringPref("SpiceTrailerCardInfo", Simulation.tool.prefs, "");
//    static { cacheSpiceTrailerCardInfo.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice trailer card information"); }
	/**
	 * Method to get the Spice trailer card specification.
	 * Trailer cards can come from one of three places, depending on the specification:<BR>
	 * Specification="" means use built-in trailer cards.<BR>
	 * Specification="Extension XXX" means use trailer cards from the file TOPCELL.XXX
	 * where TOPCELL is the name of the top-level cell name and XXX is a specified extension.<BR>
	 * Specification="XXX" means use trailer cards from the file XXX.
	 * @return the Spice trailer card specification.
	 */
	public static String getSpiceTrailerCardInfo() { return cacheSpiceTrailerCardInfo.getString(); }
	/**
	 * Method to set the Spice trailer card specification.
	 * Trailer cards can come from one of three places, depending on the specification:<BR>
	 * Specification="" means use built-in trailer cards.<BR>
	 * Specification="Extension XXX" means use trailer cards from the file TOPCELL.XXX
	 * where TOPCELL is the name of the top-level cell name and XXX is a specified extension.<BR>
	 * Specification="XXX" means use trailer cards from the file XXX.
	 * @param spec the Spice trailer card specification.
	 */
	public static void setSpiceTrailerCardInfo(String spec) { cacheSpiceTrailerCardInfo.setString(spec); }

	private static Pref cacheSpiceUseParasitics = Pref.makeBooleanPref("SpiceUseParasitics", Simulation.tool.prefs, true);
//    static { cacheSpiceUseParasitics.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice uses parasitics"); }
	/**
	 * Method to tell whether or not to use parasitics in Spice output.
	 * The default is true.
	 * @return true to use parasitics in Spice output.
	 */
	public static boolean isSpiceUseParasitics() { return cacheSpiceUseParasitics.getBoolean(); }
	/**
	 * Method to set whether or not to use parasitics in Spice output.
	 * @param p true to use parasitics in Spice output.
	 */
	public static void setSpiceUseParasitics(boolean p) { cacheSpiceUseParasitics.setBoolean(p); }

	private static Pref cacheSpiceUseNodeNames = Pref.makeBooleanPref("SpiceUseNodeNames", Simulation.tool.prefs, true);
//    static { cacheSpiceUseNodeNames.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice uses node names"); }
	/**
	 * Method to tell whether or not to use node names in Spice output.
	 * If node names are off, then numbers are used.
	 * The default is true.
	 * @return true to use node names in Spice output.
	 */
	public static boolean isSpiceUseNodeNames() { return cacheSpiceUseNodeNames.getBoolean(); }
	/**
	 * Method to set whether or not to use node names in Spice output.
	 * If node names are off, then numbers are used.
	 * @param u true to use node names in Spice output.
	 */
	public static void setSpiceUseNodeNames(boolean u) { cacheSpiceUseNodeNames.setBoolean(u); }

	private static Pref cacheSpiceForceGlobalPwrGnd = Pref.makeBooleanPref("SpiceForceGlobalPwrGnd", Simulation.tool.prefs, false);
//    static { cacheSpiceForceGlobalPwrGnd.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice forces global VDD/GND"); }
	/**
	 * Method to tell whether or not to write global power and ground in Spice output.
	 * If this is off, then individual power and ground references are made.
	 * The default is false.
	 * @return true to write global power and ground in Spice output.
	 */
	public static boolean isSpiceForceGlobalPwrGnd() { return cacheSpiceForceGlobalPwrGnd.getBoolean(); }
	/**
	 * Method to set whether or not to write global power and ground in Spice output.
	 * If this is off, then individual power and ground references are made.
	 * @param g true to write global power and ground in Spice output.
	 */
	public static void setSpiceForceGlobalPwrGnd(boolean g) { cacheSpiceForceGlobalPwrGnd.setBoolean(g); }

	private static Pref cacheSpiceUseCellParameters = Pref.makeBooleanPref("SpiceUseCellParameters", Simulation.tool.prefs, false);
//    static { cacheSpiceForceGlobalPwrGnd.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice uses cell parameters"); }
	/**
	 * Method to tell whether or not to use cell parameters in Spice output.
	 * When cell parameters are used, any parameterized cell is written many times,
	 * once for each combination of parameter values.
	 * The default is false.
	 * @return true to use cell parameters in Spice output.
	 */
	public static boolean isSpiceUseCellParameters() { return cacheSpiceUseCellParameters.getBoolean(); }
	/**
	 * Method to set whether or not to use cell parameters in Spice output.
	 * When cell parameters are used, any parameterized cell is written many times,
	 * once for each combination of parameter values.
	 * @param p true to use cell parameters in Spice output.
	 */
	public static void setSpiceUseCellParameters(boolean p) { cacheSpiceUseCellParameters.setBoolean(p); }

	private static Pref cacheSpiceWriteTransSizeInLambda = Pref.makeBooleanPref("SpiceWriteTransSizeInLambda", Simulation.tool.prefs, false);
//    static { cacheSpiceWriteTransSizeInLambda.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice writes transistor sizes in lambda"); }
	/**
	 * Method to tell whether or not to write transistor sizes in "lambda" grid units in Spice output.
	 * Lambda grid units are the basic units of design.
	 * When writing in these units, the values are simpler, but an overriding scale factor brings them to the proper size.
	 * The default is false.
	 * @return true to write transistor sizes in "lambda" grid units in Spice output.
	 */
	public static boolean isSpiceWriteTransSizeInLambda() { return cacheSpiceWriteTransSizeInLambda.getBoolean(); }
	/**
	 * Method to set whether or not to write transistor sizes in "lambda" grid units in Spice output.
	 * Lambda grid units are the basic units of design.
	 * When writing in these units, the values are simpler, but an overriding scale factor brings them to the proper size.
	 * @param l true to write transistor sizes in "lambda" grid units in Spice output.
	 */
	public static void setSpiceWriteTransSizeInLambda(boolean l) { cacheSpiceWriteTransSizeInLambda.setBoolean(l); }
}
