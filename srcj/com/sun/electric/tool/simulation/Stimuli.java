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
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.waveform.WaveformWindow;

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
	/** the separator character that breaks names */			private char separatorChar;
	/** the analyses in this Stimuli */							private HashMap<Analysis.AnalysisType,Analysis> analyses;
	/** the list of analyses in this Stimuli */					private List<Analysis> analysisList;
	/** control points when signals are selected */				private HashMap<Signal,Double[]> controlPointMap;

	/**
	 * Constructor to build a new Simulation Data object.
	 */
	public Stimuli()
	{
		separatorChar = '.';
		analyses = new HashMap<Analysis.AnalysisType,Analysis>();
		analysisList = new ArrayList<Analysis>();
		controlPointMap = new HashMap<Signal,Double[]>();
	}

    /**
     * Free allocated resources before closing.
     */
    public void finished()
    {
        for (Analysis an: analysisList)
            an.finished();
        controlPointMap.clear();
        for (Analysis an: analyses.values())
            an.finished();
        analyses.clear();
    }
    
    public void addAnalysis(Analysis an)
	{
		analyses.put(an.getAnalysisType(), an);
		analysisList.add(an);
	}

	/**
	 * Method to find an Analysis of a given type.
	 * @param type the stimulus type being queried.
	 * @return the Analysis of that type (null if not found).
	 */
	public Analysis findAnalysis(Analysis.AnalysisType type)
	{
		Analysis an = analyses.get(type);
		return an;
	}

	public int getNumAnalyses() { return analysisList.size(); }

	public Iterator<Analysis> getAnalyses() { return analysisList.iterator(); }

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
	 * Method to return an array of control points associated with a signal.
	 * Control points are places where the user has added stimuli to the signal (set a level or strength).
	 * These points can be selected for change of the stimuli.
	 * @param sig the signal in question.
	 * @return an array of times where there are control points.
	 * Null if no control points are defined.
	 */
	public Double [] getControlPoints(Signal sig) { return controlPointMap.get(sig); }

	/**
	 * Method to clear the list of control points associated with a signal.
	 * Control points are places where the user has added stimuli to the signal (set a level or strength).
	 * These points can be selected for change of the stimuli.
	 * @param sig the signal to clear.
	 */
	public void clearControlPoints(Signal sig) { controlPointMap.remove(sig); }

	/**
	 * Method to add a new control point to the list on a signal.
	 * Control points are places where the user has added stimuli to the signal (set a level or strength).
	 * These points can be selected for change of the stimuli.
	 * @param sig the signal in question.
	 * @param time the time of the new control point.
	 */
	public void addControlPoint(Signal sig, double time)
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
	public void removeControlPoint(Signal sig, double time)
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
	public Rectangle2D getBounds()
	{
		// determine extent of the data
		Rectangle2D bounds = null;
		for(Analysis an : analysisList)
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

	/**
	 * Method to tell whether this simulation data is analog or digital.
	 * @return true if this simulation data is analog.
	 */
	public boolean isAnalog()
	{
		for(Analysis an : analysisList)
		{
			if (an.getSignals().size() > 0)
			{
				Signal sSig = an.getSignals().get(0);
				if (sSig instanceof AnalogSignal) return true;
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
}
