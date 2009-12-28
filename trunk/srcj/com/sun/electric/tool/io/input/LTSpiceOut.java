/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LTSpiceOut.java
 * Input/output tool: reader for LTSpice output (.raw)
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.simulation.AnalogAnalysis;
import com.sun.electric.tool.simulation.AnalogSignal;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.simulation.Waveform;
import com.sun.electric.tool.simulation.WaveformImpl;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for reading and displaying waveforms from LTSpice Raw output.
 * These are contained in .raw files.
 */
public class LTSpiceOut extends Simulate
{
	private static final boolean DEBUG = false;

	private boolean complexValues;

	/**
	 * Class for handling swept signals.
	 */
	private static class SweepAnalysisLT extends AnalogAnalysis
	{
		double [][] commonTime; // sweep, signal
		List<List<double[]>> theSweeps = new ArrayList<List<double[]>>(); // sweep, event, signal

		private SweepAnalysisLT(Stimuli sd, AnalogAnalysis.AnalysisType type)
		{
			super(sd, type, false);
		}

		protected Waveform[] loadWaveforms(AnalogSignal signal)
		{
			int sigIndex = signal.getIndexInAnalysis();
			Waveform[] waveforms = new Waveform[commonTime.length];
			for (int sweep = 0; sweep < waveforms.length; sweep++)
			{
				double[] times = commonTime[sweep];
				List<double[]> theSweep = theSweeps.get(sweep);
				Waveform waveform = new WaveformImpl(times, theSweep.get(sigIndex));
				waveforms[sweep] = waveform;
			}
			return waveforms;
		}
	}

	LTSpiceOut() {}

	/**
	 * Method to read an LTSpice output file.
	 */
	protected void readSimulationOutput(Stimuli sd, URL fileURL, Cell cell)
		throws IOException
	{
		// open the file
		if (openBinaryInput(fileURL)) return;

		// show progress reading .raw file
		startProgressDialog("LTSpice output", fileURL.getFile());

		// read the actual signal data from the .raw file
		readRawLTSpiceFile(cell, sd);

		// stop progress dialog, close the file
		stopProgressDialog();
		closeInput();
	}

	private void readRawLTSpiceFile(Cell cell, Stimuli sd)
		throws IOException
	{
		complexValues = false;
		boolean realValues = false;
		int signalCount = -1;
		String[] signalNames = null;
		int rowCount = -1;
		AnalogAnalysis.AnalysisType aType = AnalogAnalysis.ANALYSIS_TRANS;
		for(;;)
		{
			String line = getLineFromBinary();
			if (line == null) break;
			updateProgressDialog(line.length());

			// find the ":" separator
			int colonPos = line.indexOf(':');
			if (colonPos < 0) continue;
			String keyWord = line.substring(0, colonPos);
			String restOfLine = line.substring(colonPos+1).trim();

			if (keyWord.equals("Plotname"))
			{
				// see if known analysis is specified
				if (restOfLine.equals("AC Analysis")) aType = AnalogAnalysis.ANALYSIS_AC;
				continue;
			}

			if (keyWord.equals("Flags"))
			{
				// the first signal is Time
				int complex = restOfLine.indexOf("complex");
				if (complex >= 0) complexValues = true;
				int r = restOfLine.indexOf("real");
				if (r >= 0) realValues = true;
				continue;
			}

			if (keyWord.equals("No. Variables"))
			{
				// the first signal is Time
				signalCount = TextUtils.atoi(restOfLine) - 1;
				continue;
			}

			if (keyWord.equals("No. Points"))
			{
				rowCount = TextUtils.atoi(restOfLine);
				continue;
			}

			if (keyWord.equals("Variables"))
			{
				if (signalCount < 0)
				{
					System.out.println("Missing variable count in file");
					return;
				}
				signalNames = new String[signalCount];
				for(int i=0; i<=signalCount; i++)
				{
					restOfLine = getLineFromBinary();
					if (restOfLine == null) break;
					updateProgressDialog(restOfLine.length());
					restOfLine = restOfLine.trim();
					int indexOnLine = TextUtils.atoi(restOfLine);
					if (indexOnLine != i)
						System.out.println("Warning: Variable " + i + " has number " + indexOnLine);
					int nameStart = 0;
					while (nameStart < restOfLine.length() && !Character.isWhitespace(restOfLine.charAt(nameStart))) nameStart++;
					while (nameStart < restOfLine.length() && Character.isWhitespace(restOfLine.charAt(nameStart))) nameStart++;
					int nameEnd = nameStart;
					while (nameEnd < restOfLine.length() && !Character.isWhitespace(restOfLine.charAt(nameEnd))) nameEnd++;
					String name = restOfLine.substring(nameStart, nameEnd);
					if (name.startsWith("V(") && name.endsWith(")"))
					{
						name = name.substring(2, name.length()-1);
					}
					if (i > 0) signalNames[i-1] = name;
				}
				continue;
			}

			if (keyWord.equals("Binary"))
			{
				if (signalCount < 0)
				{
					System.out.println("Missing variable count in file");
					return;
				}
				if (rowCount < 0)
				{
					System.out.println("Missing point count in file");
					return;
				}

				// initialize the stimuli object
				SweepAnalysisLT an = new SweepAnalysisLT(sd, aType);
				sd.setCell(cell);
				if (DEBUG)
				{
					System.out.println(signalCount+" VARIABLES, "+rowCount+" SAMPLES");
					for(int i=0; i<signalCount; i++)
						System.out.println("VARIABLE "+i+" IS "+signalNames[i]);
				}

				// read all of the data in the RAW file
				double[][] values = new double[signalCount][rowCount];
				double [] timeValues = new double[rowCount];
				for(int j=0; j<rowCount; j++)
				{
					double time = getNextDouble();
					if (DEBUG) System.out.println("TIME AT "+j+" IS "+time);
					timeValues[j] = Math.abs(time);
					for(int i=0; i<signalCount; i++)
					{
						double value = 0;
						if (realValues) value = getNextFloat(); else
							value = getNextDouble();
						if (DEBUG) System.out.println("   DATA POINT "+i+" ("+signalNames[i]+") IS "+value);
						values[i][j] = value;
					}
				}

				// figure out where the sweep breaks occur
				List<Integer> sweepLengths = new ArrayList<Integer>();
//				int sweepLength = -1;
				int sweepStart = 0;
				int sweepCount = 0;
				for(int j=1; j<=rowCount; j++)
				{
					if (j == rowCount || timeValues[j] < timeValues[j-1])
					{
						int sl = j - sweepStart;
						sweepLengths.add(new Integer(sl));
						sweepStart = j;
						sweepCount++;
						an.addSweep(Integer.toString(sweepCount));
					}
				}
				if (DEBUG) System.out.println("FOUND " + sweepCount + " SWEEPS");

				// place data into the Analysis object
				an.commonTime = new double[sweepCount][];
				int offset = 0;
				for(int s=0; s<sweepCount; s++)
				{
					int sweepLength = sweepLengths.get(s).intValue();
					an.commonTime[s] = new double[sweepLength];
					for (int j = 0; j < sweepLength; j++)
						an.commonTime[s][j] = timeValues[j + offset];

					List<double[]> allTheData = new ArrayList<double[]>();
					for(int i=0; i<signalCount; i++)
					{
						double[] oneSetOfData = new double[sweepLength];
						for(int j=0; j<sweepLength; j++)
							oneSetOfData[j] = values[i][j+offset];
						allTheData.add(oneSetOfData);
					}
					an.theSweeps.add(allTheData);
					offset += sweepLength;
				}

				// add signal names to the analysis
				for (int i = 0; i < signalCount; i++)
				{
					String name = signalNames[i];
					int lastDotPos = name.lastIndexOf('.');
					String context = null;
					if (lastDotPos >= 0)
					{
						context = name.substring(0, lastDotPos);
						name = name.substring(lastDotPos + 1);
					}
					double minTime = 0, maxTime = 0, minValues = 0, maxValues = 0;
					an.addSignal(signalNames[i], context, minTime, maxTime, minValues, maxValues);
				}
				return;
			}
		}
	}

	private double getNextDouble()
		throws IOException
	{
		// double values appear with reversed bytes
		long lt = dataInputStream.readLong();
		lt = Long.reverseBytes(lt);
		double t = Double.longBitsToDouble(lt);
		int amtRead = 8;

		// for complex plots, ignore imaginary part
		if (complexValues) { amtRead *= 2;   dataInputStream.readLong(); }

		updateProgressDialog(amtRead);
		return t;
	}

	private float getNextFloat()
		throws IOException
	{
		// float values appear with reversed bytes
		int lt = dataInputStream.readInt();
		lt = Integer.reverseBytes(lt);
		float t = Float.intBitsToFloat(lt);
		int amtRead = 4;

		// for complex plots, ignore imaginary part
		if (complexValues) { amtRead *= 2;   dataInputStream.readInt(); }

		updateProgressDialog(amtRead);
		return t;
	}
}
