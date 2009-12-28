/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RawSpiceOut.java
 * Input/output tool: reader for Raw Spice output (.raw)
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
 * Class for reading and displaying waveforms from Raw Spice output.
 * These are contained in .raw files.
 */
public class RawSpiceOut extends Simulate
{
	RawSpiceOut() {}

	/**
	 * Method to read an Raw Spice output file.
	 */
	protected void readSimulationOutput(Stimuli sd, URL fileURL, Cell cell)
		throws IOException
	{
		// open the file
		if (openTextInput(fileURL)) return;

		// show progress reading .raw file
		startProgressDialog("Raw Spice output", fileURL.getFile());

		// read the actual signal data from the .raw file
		readRawFile(cell, sd);

		// stop progress dialog, close the file
		stopProgressDialog();
		closeInput();
	}

	private void readRawFile(Cell cell, Stimuli sd)
		throws IOException
	{
		// once per deck
		boolean first = true;
		sd.setCell(cell);

		// once per analysis in the deck
		AnalogAnalysis an = null; // new AnalogAnalysis(sd, AnalogAnalysis.ANALYSIS_SIGNALS);
		int numSignals = -1;
		int eventCount = -1;
		String[] signalNames = null;
		double[][] values = null;
		for(;;)
		{
			String line = getLineFromSimulator();
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
						System.out.println("This is an HSPICE file, not a RAWFILE file");
						System.out.println("Change the SPICE format (in Preferences) and reread");
						return;
					}
				}
			}

			// find the ":" separator
			int colonPos = line.indexOf(":");
			if (colonPos < 0) continue;
			String preColon = line.substring(0, colonPos);
			String postColon = line.substring(colonPos+1).trim();

			if (preColon.equals("Plotname"))
			{
				// terminate any previous analysis
				if (an != null)
				{
					numSignals = -1;
					eventCount = -1;
					signalNames = null;
					values = null;
				}

				// start reading a new analysis
				if (postColon.startsWith("Transient Analysis"))
				{
					an = new AnalogAnalysis(sd, AnalogAnalysis.ANALYSIS_TRANS, false);
				} else if (postColon.startsWith("DC Analysis"))
				{
					an = new AnalogAnalysis(sd, AnalogAnalysis.ANALYSIS_DC, false);
				} else if (postColon.startsWith("AC Analysis"))
				{
					an = new AnalogAnalysis(sd, AnalogAnalysis.ANALYSIS_AC, false);
				} else
				{
					System.out.println("ERROR: Unknown analysis: " + postColon);
					return;
				}
				continue;
			}

			if (preColon.equals("No. Variables"))
			{
				numSignals = TextUtils.atoi(postColon) - 1;
				continue;
			}

			if (preColon.equals("No. Points"))
			{
				eventCount = TextUtils.atoi(postColon);
				an.buildCommonTime(eventCount);
				continue;
			}

			if (preColon.equals("Variables"))
			{
				if (numSignals < 0)
				{
					System.out.println("Missing variable count in file");
					return;
				}
				signalNames = new String[numSignals];
				values = new double[numSignals][eventCount];
				for(int i=0; i<=numSignals; i++)
				{
					if (postColon.length() > 0)
					{
						line = postColon;
						postColon = "";
					} else
					{
						line = getLineFromSimulator();
						if (line == null)
						{
							System.out.println("Error: end of file during signal names");
							return;
						}
					}
					line = line.trim();
					int numberOnLine = TextUtils.atoi(line);
					if (numberOnLine != i)
						System.out.println("Warning: Variable " + i + " has number " + numberOnLine);
					int spacePos = line.indexOf(" ");   if (spacePos < 0) spacePos = line.length();
					int tabPos = line.indexOf("\t");    if (tabPos < 0) tabPos = line.length();
					int pos = Math.min(spacePos, tabPos);
					String name = line.substring(pos).trim();
					spacePos = name.indexOf(" ");   if (spacePos < 0) spacePos = name.length();
					tabPos = name.indexOf("\t");    if (tabPos < 0) tabPos = name.length();
					pos = Math.min(spacePos, tabPos);
					name = name.substring(0, pos);
					if (i == 0)
					{
						if (!name.equals("time") && an.getAnalysisType() == AnalogAnalysis.ANALYSIS_TRANS)
							System.out.println("Warning: the first variable should be time, is '" + name + "'");
					} else
					{
						signalNames[i-1] = name;
					}
				}
				continue;
			}
			if (preColon.equals("Values"))
			{
				if (numSignals < 0)
				{
					System.out.println("Missing variable count in file");
					return;
				}
				if (eventCount < 0)
				{
					System.out.println("Missing point count in file");
					return;
				}
				for(int j=0; j<eventCount; j++)
				{
					for(int i = -1; i <= numSignals; )
					{
						line = getLineFromSimulator();
						if (line == null)
						{
							System.out.println("Error: end of file during data points (read " + j + " out of " + eventCount);
							return;
						}
						line = line.trim();
						if (line.length() == 0) continue;
						int charPos = 0;
						while (charPos <= line.length())
						{
							int tabPos = line.indexOf("\t", charPos);
							if (tabPos < 0) tabPos = line.length();
							String field = line.substring(charPos, tabPos);
							charPos = tabPos+1;
							while (charPos < line.length() && line.charAt(charPos) == '\t') charPos++;
							if (i < 0)
							{
								int lineNumber = TextUtils.atoi(field);
								if (lineNumber != j)
									System.out.println("Warning: event " + j + " has wrong event number: " + lineNumber);
							} else
							{
								double val = TextUtils.atof(field);
								if (i == 0) an.setCommonTime(j, val); else
									values[i-1][j] = val;
							}
							i++;
							if (i > numSignals) break;
						}
					}
				}
				for (int i = 0; i < numSignals; i++)
					an.addSignal(signalNames[i], null, values[i]);
				continue;
			}
			if (preColon.equals("Binary"))
			{
				if (numSignals < 0)
				{
					System.out.println("Missing variable count in file");
					return;
				}
				if (eventCount < 0)
				{
					System.out.println("Missing point count in file");
					return;
				}

				// read the data
				for(int j=0; j<eventCount; j++)
				{
					an.setCommonTime(j, dataInputStream.readDouble());
					for(int i=0; i<numSignals; i++)
						values[i][j] = dataInputStream.readDouble();
				}
				continue;
			}
		}
	}

}
