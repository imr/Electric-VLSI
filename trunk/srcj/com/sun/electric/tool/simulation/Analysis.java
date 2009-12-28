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
public abstract class Analysis<S extends Signal>
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
	/** a list of all signals in this Analysis */				private List<S> signals = new ArrayList<S>();
	/** a map of all signal names in this Analysis */			private HashMap<String,S> signalNames = new HashMap<String,S>();
	/** the range of values in this Analysis */					private Rectangle2D bounds;
	/** the left and right side of the Analysis */				private double leftEdge, rightEdge;
	/** true to extrapolate last value in waveform window */	private boolean extrapolateToRight;
    /** group of signals from extracted netlist of same net */  private HashMap<String,List<S>> signalGroup = new HashMap<String,List<S>>();

    /**
	 * Constructor for a collection of simulation data.
	 * @param sd Stimuli that this analysis is part of.
	 * @param type the type of this analysis.
	 * @param extrapolateToRight true to draw the last value to the right
	 * (useful for IRSIM and other digital simulations).
	 * False to stop drawing signals after their last value
	 * (useful for Spice and other analog simulations).
	 */
	public Analysis(Stimuli sd, AnalysisType type, boolean extrapolateToRight)
	{
		this.sd = sd;
		this.type = type;
		this.extrapolateToRight = extrapolateToRight;
		sd.addAnalysis(this);
	}

	/**
	 * Free allocated resources before closing.
	 */
	public void finished()
	{
		for (S s : signals)
			s.finished();
		signals.clear();
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
	 * Method to tell whether signal values should be extrapolated to the
	 * right side of the waveform window.
	 * @return true to draw the last value to the right (useful for IRSIM and
	 * other digital simulations).  False to stop drawing signals after their
	 * last value (useful for Spice and other analog simulations).
	 */
	public boolean extrapolateValues()
	{
		return extrapolateToRight;
	}

    public boolean isUseLegacySimulationCode()
    {
        return sd.isUseLegacySimulationCode();
    }

    /**
	 * Method to get the list of signals in this Simulation Data object.
	 * @return a List of signals.
	 */
	public List<S> getSignals() { return signals; }

	public void nameSignal(S ws, String sigName)
	{
		String name = TextUtils.canonicString(sigName);
		signalNames.put(name, ws);

		// simulators may strip off last "_"
		if (name.indexOf('_') >= 0 && !name.endsWith("_"))
			signalNames.put(name + "_", ws);

        // keep track of groups of signals that represent one extracted net
        String baseName = getBaseNameFromExtractedNet(name, sd.getNetDelimiter());
        List<S> sigs = signalGroup.get(baseName);
        if (sigs == null) {
            sigs = new ArrayList<S>();
            signalGroup.put(baseName, sigs);
        }
        sigs.add(ws);
    }

	/**
	 * Method to add a new signal to this Simulation Data object.
	 * Signals can be either digital or analog.
	 * @param ws the signal to add.
	 * Instead of a "Signal", use either DigitalSignal or AnalogSignal.
	 */
	public void addSignal(S ws)
	{
		signals.add(ws);
		String sigName = ws.getFullName();
		if (sigName != null) nameSignal(ws, sigName);
		setBoundsDirty();
	}

    private static String getBaseNameFromExtractedNet(String signalFullName, String delim) {
//        String delim = Simulation.getSpiceExtractedNetDelimiter();
        int hashPos = signalFullName.indexOf(delim);
        if (hashPos > 0)
        {
            return signalFullName.substring(0, hashPos);
        } else {
            return signalFullName;
        }
    }

    /**
     * Get a list of signals that are from the same network.
     * Extracted nets are the original name + delimiter + some junk
     * @param ws the signal
     * @return a list of signals
     */
    public List<S> getSignalsFromExtractedNet(Signal ws) {
        String sigName = ws.getFullName();
        if (sigName == null) return new ArrayList<S>();
        sigName = TextUtils.canonicString(sigName);
        sigName = getBaseNameFromExtractedNet(sigName, sd.getNetDelimiter());
        return signalGroup.get(sigName);
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
					leftEdge = sig.getLeftEdge();
					rightEdge = sig.getRightEdge();
				} else
				{
					Rectangle2D.union(bounds, sigBounds, bounds);
					if (leftEdge < rightEdge)
					{
						leftEdge = Math.min(leftEdge, sig.getLeftEdge());
						rightEdge = Math.max(rightEdge, sig.getRightEdge());
					} else
					{
						// backwards time values
						leftEdge = Math.max(leftEdge, sig.getLeftEdge());
						rightEdge = Math.min(rightEdge, sig.getRightEdge());
					}
				}
			}
		}
		return bounds;
	}

	/**
	 * Method to return the leftmost X coordinate of this Analysis.
	 * This value may not be the same as the minimum-x of the bounds, because
	 * the data may not be monotonically increasing (may run backwards, for example).
	 * @return the leftmost X coordinate of this Analysis.
	 */
	public double getLeftEdge()
	{
		getBounds();
		return leftEdge;
	}

	/**
	 * Method to return the rightmost X coordinate of this Analysis.
	 * This value may not be the same as the maximum-x of the bounds, because
	 * the data may not be monotonically increasing (may run backwards, for example).
	 * @return the rightmost X coordinate of this Analysis.
	 */
	public double getRightEdge()
	{
		getBounds();
		return rightEdge;
	}

	public void setBoundsDirty() { bounds = null; }

	/**
	 * Method to tell whether this simulation data is analog or digital.
	 * @return true if this simulation data is analog.
	 */
	public abstract boolean isAnalog();

	/**
	 * Method to quickly return the signal that corresponds to a given Network name.
	 * Not all names may be found (because of name mangling, which this method does not handle).
	 * But the lookup is faster than "findSignalForNetwork".
	 * @param netName the Network name to find.
	 * @return the Signal that corresponds with the Network.
	 * Returns null if none can be found.
	 */
	public S findSignalForNetworkQuickly(String netName)
	{
		String lookupName = TextUtils.canonicString(netName);
		S sSig = signalNames.get(lookupName);
		return sSig;
	}

	/**
	 * Method to return the signal that corresponds to a given Network name.
	 * @param netName the Network name to find.
	 * @return the Signal that corresponds with the Network.
	 * Returns null if none can be found.
	 */
	public S findSignalForNetwork(String netName)
	{
		// look at all signal names in the cell
		for(Iterator<S> it = getSignals().iterator(); it.hasNext(); )
		{
			S sSig = it.next();

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
