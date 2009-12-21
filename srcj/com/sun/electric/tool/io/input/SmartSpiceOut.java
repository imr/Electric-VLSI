/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SmartSpiceOut.java
 * Input/output tool: reader for Smart Spice output (.dump)
 * Written by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.simulation.AnalogAnalysis;
import com.sun.electric.tool.simulation.Stimuli;

import java.io.IOException;
import java.net.URL;

/**
 * Class for reading and displaying waveforms from SmartSpice Raw output.
 * These are contained in .dump files.
 */
public class SmartSpiceOut extends Simulate
{
	SmartSpiceOut() {}

	/**
	 * Method to read an Smart Spice output file.
	 */
	protected void readSimulationOutput(Stimuli sd, URL fileURL, Cell cell)
		throws IOException
	{
		// open the file
		if (openBinaryInput(fileURL)) return;

		// show progress reading .dump file
		startProgressDialog("SmartSpice output", fileURL.getFile());

		// read the actual signal data from the .dump file
		readRawSmartSpiceFile(cell, sd);

		// stop progress dialog, close the file
		stopProgressDialog();
		closeInput();
	}

	private void readRawSmartSpiceFile(Cell cell, Stimuli sd)
		throws IOException
	{
		boolean first = true;
		int signalCount = -1;
		String[] signalNames = null;
		int rowCount = -1;
		AnalogAnalysis an = null;
		double[][] values = null;
		for(;;)
		{
			String line = getLineFromBinary();
			if (line == null) break;

			// make sure this isn't an HSPICE deck (check first line)
			if (first)
			{
				first = false;
				if (line.length() >= 20)
				{
					String hsFormat = line.substring(16, 20);
					if (hsFormat.equals("9007") || hsFormat.equals("9601"))
					{
						System.out.println("This is an HSPICE file, not a SMARTSPICE file");
						System.out.println("Change the SPICE format (in Preferences) and reread");
						return;
					}
				}
			}

			// find the ":" separator
			int colonPos = line.indexOf(':');
			if (colonPos < 0) continue;
			String keyWord = line.substring(0, colonPos);
			String restOfLine = line.substring(colonPos+1).trim();

			if (keyWord.equals("No. Variables"))
			{
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
				an = new AnalogAnalysis(sd, AnalogAnalysis.ANALYSIS_SIGNALS, false);
				sd.setCell(cell);
				signalNames = new String[signalCount];
				values = new double[signalCount][rowCount];
				for(int i=0; i<=signalCount; i++)
				{
					if (i != 0)
					{
						restOfLine = getLineFromBinary();
						if (restOfLine == null) break;
						restOfLine = restOfLine.trim();
					}
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
					if (i == 0)
					{
						if (!name.equals("time"))
							System.out.println("Warning: the first variable (the sweep variable) should be time, is '" + name + "'");
					} else
					{
						signalNames[i - 1] = name;
					}
				}
				continue;
			}
			if (keyWord.equals("Values"))
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
				an.buildCommonTime(rowCount);

				// read the data
				for(int j=0; j<rowCount; j++)
				{
					line = getLineFromBinary();
					if (line == null) break;
					if (TextUtils.atoi(line) != j)
					{
						System.out.println("Warning: data point " + j + " has number " + TextUtils.atoi(line));
					}
					int spacePos = line.indexOf(' ');
					if (spacePos >= 0) line = line.substring(spacePos+1);
					double time = TextUtils.atof(line.trim());
					an.setCommonTime(j, time);

					for(int i=0; i<signalCount; i++)
					{
						line = getLineFromBinary();
						if (line == null) break;
						double value = TextUtils.atof(line.trim());
						values[i][j] = value;
					}
				}
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
				an.buildCommonTime(rowCount);

				// read the data
				for(int j=0; j<rowCount; j++)
				{
					long lval = dataInputStream.readLong();
					lval = Long.reverseBytes(lval);
					double time = Double.longBitsToDouble(lval);
					an.setCommonTime(j, time);
					for(int i=0; i<signalCount; i++)
					{
						double value = 0;
						if (true) {
							// swap bytes, for smartspice raw files generated on linux.
							// with ".options post" (the default) yields this binary format
							lval = dataInputStream.readLong();
							lval = Long.reverseBytes(lval);
							value = Double.longBitsToDouble(lval);
						} else {
							// do smartspice output files on other OS's have this byte order?
							value = dataInputStream.readDouble();
						}
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
	}
}
