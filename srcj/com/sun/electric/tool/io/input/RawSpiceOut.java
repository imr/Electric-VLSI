/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RawSpiceOut.java
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Class for reading and displaying waveforms from Raw Spice output.
 * Thease are contained in .raw files.
 */
public class RawSpiceOut extends Simulate
{
	RawSpiceOut() {}

	/**
	 * Method to read an Raw Spice output file.
	 */
	protected SimData readSimulationOutput(URL fileURL, Cell cell)
		throws IOException
	{
		// open the file
		if (openTextInput(fileURL)) return null;

		// show progress reading .tr0 file
		startProgressDialog("Raw Spice output", fileURL.getFile());

		// read the actual signal data from the .tr0 file
		SimData sd = readRawFile(cell);

		// stop progress dialog, close the file
		stopProgressDialog();
		closeInput();

		// return the simulation data
		return sd;
	}

	private SimData readRawFile(Cell cell)
		throws IOException
	{
		boolean first = true;
//		List events = new ArrayList();
		int numSignals = -1;
		int eventCount = -1;
		SimData sd = new SimData();
		sd.setCell(cell);
		Simulate.SimAnalogSignal [] signals = null;
		for(;;)
		{
			String line = getLineFromSimulator();
			if (line == null) break;
			if (first)
			{
				// check the first line for HSPICE format possibility
				first = false;
				if (line.length() >= 20 && line.substring(16, 20).equals("9007"))
				{
					System.out.println("This is an HSPICE file, not a RAWFILE file");
					System.out.println("Change the SPICE format and reread");
					return null;
				}
			}

			// find the ":" separator
			int colonPos = line.indexOf(":");
			if (colonPos < 0) continue;
			String preColon = line.substring(0, colonPos);
			String postColon = line.substring(colonPos+1).trim();

			if (preColon.equals("Plotname"))
			{
//				if (sim_spice_cellname[0] == '\0')
//					estrcpy(sim_spice_cellname, ptr);
				continue;
			}

			if (preColon.equals("No. Variables"))
			{
				numSignals = TextUtils.atoi(postColon) - 1;
				signals = new Simulate.SimAnalogSignal[numSignals];
				for(int i=0; i<numSignals; i++)
				{
					signals[i] = new Simulate.SimAnalogSignal(sd);
					signals[i].setCommonTimeUse(true);
				}
				continue;
			}

			if (preColon.equals("No. Points"))
			{
				eventCount = TextUtils.atoi(postColon);
				sd.buildCommonTime(eventCount);
				for(int i=0; i<numSignals; i++)
					signals[i].buildValues(eventCount);
				continue;
			}

			if (preColon.equals("Variables"))
			{
				if (numSignals < 0)
				{
					System.out.println("Missing variable count in file");
					return null;
				}
				for(int i=0; i<=numSignals; i++)
				{
					line = getLineFromSimulator();
					if (line == null)
					{
						System.out.println("Error: end of file during signal names");
						return null;
					}
					line = line.trim();
					int numberOnLine = TextUtils.atoi(line);
					if (numberOnLine != i)
						System.out.println("Warning: Variable " + i + " has number " + numberOnLine);
					int spacePos = line.indexOf(" ");   if (spacePos < 0) spacePos = line.length();
					int tabPos = line.indexOf("\t");    if (tabPos < 0) tabPos = line.length();
					int pos = Math.min(spacePos, tabPos);
					String name = line.substring(pos).trim();
					spacePos = name.indexOf(" ");   if (spacePos < 0) spacePos = line.length();
					tabPos = name.indexOf("\t");    if (tabPos < 0) tabPos = line.length();
					pos = Math.min(spacePos, tabPos);
					name = name.substring(0, pos);
					if (i == 0)
					{
						if (!name.equals("time"))
							System.out.println("Warning: the first variable should be time, is '" + name + "'");
					} else
					{
						signals[i-1].setSignalName(name);
					}
				}
				continue;
			}
			if (preColon.equals("Values"))
			{
				if (numSignals < 0)
				{
					System.out.println("Missing variable count in file");
					return null;
				}
				if (eventCount < 0)
				{
					System.out.println("Missing point count in file");
					return null;
				}
				for(int j=0; j<eventCount; j++)
				{
					for(int i=0; i<=numSignals; i++)
					{
						line = getLineFromSimulator();
						if (line == null)
						{
							System.out.println("Error: end of file during data points (read " + j + " out of " + eventCount);
							return null;
						}
						if (i == 0)
						{
							int lineNumber = TextUtils.atoi(line);
							if (lineNumber != j)
								System.out.println("Warning: event " + j + " has wrong event number: " + lineNumber);
							while (Character.isDigit(line.charAt(0))) line = line.substring(1);
						}
						line = line.trim();
						if (i == 0) sd.setCommonTime(j, TextUtils.atof(line)); else
							signals[i-1].setValue(j, TextUtils.atof(line));
					}
				}
			}
			if (preColon.equals("Binary"))
			{
				if (numSignals < 0)
				{
					System.out.println("Missing variable count in file");
					return null;
				}
				if (eventCount < 0)
				{
					System.out.println("Missing point count in file");
					return null;
				}

				// read the data
				for(int j=0; j<eventCount; j++)
				{
					sd.setCommonTime(j, dataInputStream.readDouble());
					for(int i=0; i<numSignals; i++)
						signals[i].setValue(j, dataInputStream.readDouble());
				}
			}
		}

		return sd;
	}

}
