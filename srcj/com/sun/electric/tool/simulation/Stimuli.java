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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 *  This class represents a set of simulation *inputs* -- that is,
 *  "force" and "release" events -- in contrast to Signal, which
 *  represents simulation *outputs*.
 */
public class Stimuli {
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
	/** the disk file associated with this Stimuli */			private URL fileURL;
	/** the separator character that breaks names */			private char separatorChar;
	/** the analyses in this Stimuli */							private HashMap<String,SignalCollection> scMap;
	/** the list of analyses in this Stimuli */					private List<SignalCollection> scList;
	/** control points when signals are selected */				private HashMap<Signal<?>,Double[]> controlPointMap;
    /** Cached version of net delimiter**/                      private String delim;

    /**
	 * Constructor to build a new Simulation Data object.
	 */
	public Stimuli()
	{
		separatorChar = '.';
		scMap = new HashMap<String,SignalCollection>();
		scList = new ArrayList<SignalCollection>();
		controlPointMap = new HashMap<Signal<?>,Double[]>();
		delim = " ";
	}

	/**
	 * Free allocated resources before closing.
	 */
	public void finished()
	{
		controlPointMap.clear();
		scMap.clear();
	}

	public void addSignalCollection(SignalCollection an)
	{
		scMap.put(an.getName(), an);
		scList.add(an);
	}

	/**
	 * Method to find a SignalCollection with a given name.
	 * @param type the stimulus type being queried.
	 * @return the SignalCollection with that name (null if not found).
	 */
	public SignalCollection findSignalCollection(String title)
	{
		SignalCollection an = scMap.get(title);
		return an;
	}

    public void setNetDelimiter(String d) { delim = d;}

    public String getNetDelimiter() { return delim;}

    public int getNumAnalyses() { return scList.size(); }

	public Iterator<SignalCollection> getSignalCollections() { return scList.iterator(); }

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
	 * Method to return an array of control points associated with a signal.
	 * Control points are places where the user has added stimuli to the signal (set a level or strength).
	 * These points can be selected for change of the stimuli.
	 * @param sig the signal in question.
	 * @return an array of times where there are control points.
	 * Null if no control points are defined.
	 */
	public Double [] getControlPoints(Signal<?> sig) { return controlPointMap.get(sig); }

	/**
	 * Method to clear the list of control points associated with a signal.
	 * Control points are places where the user has added stimuli to the signal (set a level or strength).
	 * These points can be selected for change of the stimuli.
	 * @param sig the signal to clear.
	 */
	public void clearControlPoints(Signal<?> sig) { controlPointMap.remove(sig); }

	/**
	 * Method to add a new control point to the list on a signal.
	 * Control points are places where the user has added stimuli to the signal (set a level or strength).
	 * These points can be selected for change of the stimuli.
	 * @param sig the signal in question.
	 * @param time the time of the new control point.
	 */
	public void addControlPoint(Signal<?> sig, double time)
	{
		Double [] controlPoints = controlPointMap.get(sig);
		if (controlPoints == null)
		{
			controlPoints = new Double[1];
			controlPoints[0] = new Double(time);
			controlPointMap.put(sig, controlPoints);
		} else
		{
			// see if it is in the list already
			for(int i=0; i<controlPoints.length; i++)
				if (controlPoints[i].doubleValue() == time) return;

			// extend the list
			Double [] newCP = new Double[controlPoints.length + 1];
			for(int i=0; i<controlPoints.length; i++)
				newCP[i] = controlPoints[i];
			newCP[controlPoints.length] = new Double(time);
			controlPointMap.put(sig, newCP);
		}
	}

	/**
	 * Method to remove control points the list on a signal.
	 * Control points are places where the user has added stimuli to the signal (set a level or strength).
	 * These points can be selected for change of the stimuli.
	 * @param sig the signal in question.
	 * @param time the time of the control point to delete.
	 */
	public void removeControlPoint(Signal<?> sig, double time)
	{
		Double [] controlPoints = controlPointMap.get(sig);
		if (controlPoints == null) return;

		// see if it is in the list already
		boolean found = false;
		for(int i=0; i<controlPoints.length; i++)
			if (controlPoints[i].doubleValue() == time) { found = true;   break; }
		if (!found) return;

		// shrink the list
		Double [] newCP = new Double[controlPoints.length - 1];
		int j = 0;
		for(int i=0; i<controlPoints.length; i++)
		{
			if (controlPoints[i].doubleValue() != time)
				newCP[j++] = controlPoints[i];
		}
		controlPointMap.put(sig, newCP);
	}

	/**
	 * Method to compute the time and value bounds of this simulation data.
	 * @return a Rectangle2D that has time bounds in the X part and
	 * value bounds in the Y part.
	 */
    /*
	public Rectangle2D getBounds()
	{
		// determine extent of the data
		Rectangle2D bounds = null;
		for(HashMap<String,Signal> an : analysisList)
		{
			Rectangle2D anBounds = an.getBounds();
			if (anBounds == null) continue;
			if (bounds == null)
			{
				bounds = new Rectangle2D.Double(anBounds.getMinX(), anBounds.getMinY(), anBounds.getWidth(), anBounds.getHeight());
			} else
			{
				Rectangle2D.union(bounds, anBounds, bounds);
			}
		}
		return bounds;
	}
    */

	/**
	 * Method to return the leftmost X coordinate of this Stimuli.
	 * This value may not be the same as the minimum-x of the bounds, because
	 * the data may not be monotonically increasing (may run backwards, for example).
	 * @return the leftmost X coordinate of this Stimuli.
	 */
	public double getMinTime()
	{
		double leftEdge = 0, rightEdge = 0;
		for(SignalCollection sc : scList) {
            for (Signal<?> sig : sc.getSignals()) {
                if (leftEdge == rightEdge) {
                        leftEdge = sig.getMinTime();
                        rightEdge = sig.getMaxTime();
                } else {
                    if (leftEdge < rightEdge) {
                        leftEdge = Math.min(leftEdge, sig.getMinTime());
                        rightEdge = Math.max(rightEdge, sig.getMaxTime());
                    } else {
                        leftEdge = Math.max(leftEdge, sig.getMinTime());
                        rightEdge = Math.min(rightEdge, sig.getMaxTime());
                    }
                }
			}
		}
		return leftEdge;
	}

	/**
	 * Method to return the rightmost X coordinate of this Stimuli.
	 * This value may not be the same as the maximum-x of the bounds, because
	 * the data may not be monotonically increasing (may run backwards, for example).
	 * @return the rightmost X coordinate of this Stimuli.
	 */
	public double getMaxTime()
	{
		double leftEdge = 0, rightEdge = 0;
		for(SignalCollection sc : scList) {
            for (Signal<?> sig : sc.getSignals()) {
                if (leftEdge == rightEdge) {
                        leftEdge = sig.getMinTime();
                        rightEdge = sig.getMaxTime();
                } else {
                    if (leftEdge < rightEdge) {
                        leftEdge = Math.min(leftEdge, sig.getMinTime());
                        rightEdge = Math.max(rightEdge, sig.getMaxTime());
                    } else {
                        leftEdge = Math.max(leftEdge, sig.getMinTime());
                        rightEdge = Math.min(rightEdge, sig.getMaxTime());
                    }
                }
			}
		}
		return rightEdge;
	}

	/**
	 * Method to tell whether this simulation data is analog or digital.
	 * @return true if this simulation data is analog.
	 */
	public boolean isAnalog()
	{
		for(SignalCollection sc : scList)
		{
            for (Signal<?> sig : sc.getSignals())
            {
            	if (!sig.isDigital()) return true;
            }
		}
		return false;
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

    public static SignalCollection newSignalCollection(Stimuli sd, final String title)
    {
    	SignalCollection sc = new SignalCollection(title);
		sd.addSignalCollection(sc);
        return sc;
    }

    /**
     * Method to find busses in a list of signals and create them.
     * @param curArray the list of signals.
     * @param sc the SignalCollection in which the signals reside.
     */
    public void makeBusSignals(List<Signal<?>> signalList, SignalCollection sc)
	{
		if (signalList == null) return;
        Collections.sort(signalList, new WaveformWindow.SignalsByName());
		List<Signal<?>> busSoFar = new ArrayList<Signal<?>>();
		for(Signal<?> sig : signalList)
		{
			String curSignalName = sig.getSignalName();
			int squarePos = curSignalName.indexOf('[');
			if (squarePos < 0)
			{
				makeBus(busSoFar, sc);
				continue;
			}
			boolean startNewBus = false;
			if (busSoFar.size() > 0)
			{
				String curBusName = curSignalName.substring(0, squarePos);
				int curIndex = TextUtils.atoi(curSignalName.substring(squarePos+1));
				String curScope = sig.getSignalContext();
				if (curScope == null) curScope = "";

				Signal<?> lastSig = busSoFar.get(busSoFar.size()-1);
				String lastSignalName = lastSig.getSignalName();
				squarePos = lastSignalName.indexOf('[');
				String lastBusName = lastSignalName.substring(0, squarePos);
				int lastIndex = TextUtils.atoi(lastSignalName.substring(squarePos+1));
				String lastScope = lastSig.getSignalContext();
				if (lastScope == null) lastScope = "";

				if (!lastBusName.equals(curBusName)) startNewBus = true; else
					if (!lastScope.equals(curScope)) startNewBus = true; else
						if (lastIndex+1 != curIndex) startNewBus = true;
			}
			if (startNewBus)
				makeBus(busSoFar, sc);
			busSoFar.add(sig);
		}
		makeBus(busSoFar, sc);
	}

	private void makeBus(List<Signal<?>> busSoFar, SignalCollection sc)
	{
		if (busSoFar.size() == 0) return;
		int width = busSoFar.size();
		Signal<DigitalSample>[] subsigs = (Signal<DigitalSample>[])new Signal[width];
		for(int i=0; i<width; i++)
            subsigs[i] = (Signal<DigitalSample>)busSoFar.get(i);

		// get first index
		String firstEntryName = subsigs[0].getSignalName();
		int firstSquarePos = firstEntryName.indexOf('[');
		int firstIndex = TextUtils.atoi(firstEntryName.substring(firstSquarePos+1));

		// get last index
		String lastEntryName = subsigs[width-1].getSignalName();
		int lastSquarePos = lastEntryName.indexOf('[');
		int lastIndex = TextUtils.atoi(lastEntryName.substring(lastSquarePos+1));

		// make the bus
		String busName = firstEntryName.substring(0, firstSquarePos) + "[" + firstIndex + ":" + lastIndex + "]";
		String scope = subsigs[0].getSignalContext();
        BusSample.createSignal(sc, this, busName, scope, true, subsigs);

        // reset the list
        busSoFar.clear();
	}

}
