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
import com.sun.electric.tool.simulation.Stimuli;

import java.io.IOException;
import java.net.URL;

/**
 * Class for reading and displaying waveforms from LTSpice Raw output.
 * These are contained in .raw files.
 */
public class LTSpiceOut extends Simulate
{
	LTSpiceOut() {}

	/**
	 * Method to read an LTSpice output file.
	 */
	protected Stimuli readSimulationOutput(URL fileURL, Cell cell)
		throws IOException
	{
		// open the file
		if (openBinaryInput(fileURL)) return null;

		// show progress reading .raw file
		startProgressDialog("LTSpice output", fileURL.getFile());

		// read the actual signal data from the .raw file
		Stimuli sd = readRawLTSpiceFile(cell);

		// stop progress dialog, close the file
		stopProgressDialog();
		closeInput();

		// return the simulation data
		return sd;
	}

	private Stimuli readRawLTSpiceFile(Cell cell)
		throws IOException
	{
		int signalCount = -1;
		String[] signalNames = null;
		int rowCount = -1;
		AnalogAnalysis an = null;
		double[][] values = null;
		for(;;)
		{
			String line = getLineFromBinary();
			if (line == null) break;

			// find the ":" separator
			int colonPos = line.indexOf(':');
			if (colonPos < 0) continue;
			String keyWord = line.substring(0, colonPos);
			String restOfLine = line.substring(colonPos+1).trim();

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
					return null;
				}
				Stimuli sd = new Stimuli();
				an = new AnalogAnalysis(sd, AnalogAnalysis.ANALYSIS_SIGNALS, false);
				sd.setCell(cell);
				signalNames = new String[signalCount];
				values = new double[signalCount][rowCount];
				for(int i=0; i<=signalCount; i++)
				{
					restOfLine = getLineFromBinary();
					if (restOfLine == null) break;
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
					if (name.startsWith("v(") && name.endsWith(")"))
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
					return null;
				}
				if (rowCount < 0)
				{
					System.out.println("Missing point count in file");
					return null;
				}

				an.buildCommonTime(rowCount);

				// read the data
				for(int j=0; j<rowCount; j++)
				{
					double time = getNextDouble();
					an.setCommonTime(j, time);
					for(int i=0; i<signalCount; i++)
					{
						double value = getNextDouble();
						values[i][j] = value;
					}
				}
			}
		}
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
			an.addSignal(signalNames[i], context, values[i]);
		}
		return an.getStimuli();
	}

	private double getNextDouble()
		throws IOException
	{
		// double values appear with reversed bytes
		long lt = dataInputStream.readLong();
		lt = Long.reverseBytes(lt);
		double t = Double.longBitsToDouble(lt);

		// for some reason, there is an extra Double value after each number in the file...ignore it
		dataInputStream.readLong();

		return t;
	}
}
