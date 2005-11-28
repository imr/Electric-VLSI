/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PSpiceOut.java
 * Input/output tool: reader for PSpice text output (.txt)
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
import com.sun.electric.tool.simulation.AnalogSignal;
import com.sun.electric.tool.simulation.Analysis;
import com.sun.electric.tool.simulation.Stimuli;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for reading and displaying waveforms from PSpice and Spice3 output.
 * These are contained in .txt files.
 * 
 * Here is an example file:
 *  Time                  I(Q2:e)               I(C1)                 IB(Q1)                IC(Q1)
 *  0.000000000000e+000  -1.326552010141e-003  0.000000000000e+000  7.693014595134e-006  1.152132987045e-003
 *  2.000000000000e-010  -1.327024307102e-003  -6.724173662320e-009  7.738638487353e-006  1.152158598416e-003
 */
public class PSpiceOut extends Simulate
{
//	private boolean eofReached;

	PSpiceOut() {}

	/**
	 * Method to read an PSpice output file.
	 */
	protected Stimuli readSimulationOutput(URL fileURL, Cell cell)
		throws IOException
	{
		// open the file
		if (openTextInput(fileURL)) return null;

		// show progress reading .spo file
		startProgressDialog("PSpice output", fileURL.getFile());

		// read the actual signal data from the .spo file
		Stimuli sd = readPSpiceFile(cell);

		// stop progress dialog, close the file
		stopProgressDialog();
		closeInput();

		// return the simulation data
		return sd;
	}

	private Stimuli readPSpiceFile(Cell cell)
		throws IOException
	{
		boolean first = true;
//		boolean knows = false;
		Stimuli sd = new Stimuli();
		Analysis an = new Analysis(sd, Analysis.ANALYSIS_SIGNALS);
		sd.setCell(cell);
		AnalogSignal [] signals = null;
		List<Double> [] values = null;
		int numSignals = 0;
		for(;;)
		{
			String line = getLine();
			if (line == null) break;

			if (first)
			{
				// check the first line for HSPICE format possibility
				first = false;
				if (line.length() >= 20)
				{
					String hsFormat = line.substring(16, 20);
					if (hsFormat.equals("9007") || hsFormat.equals("9601"))
					{
						System.out.println("This is an HSPICE file, not a SPICE3/PSPICE file");
						System.out.println("Change the SPICE format (in Preferences) and reread");
						return null;
					}
				}

				// parse the signal names on the first line
				int ptr = 0;
				List<String> signalNames = new ArrayList<String>();
				for(;;)
				{
					while (ptr < line.length() && Character.isWhitespace(line.charAt(ptr))) ptr++;
					if (ptr >= line.length()) break;
					int start = ptr;
					while (ptr < line.length() && !Character.isWhitespace(line.charAt(ptr))) ptr++;
					signalNames.add(line.substring(start, ptr));
				}
				numSignals = signalNames.size();
				signals = new AnalogSignal[numSignals-1];
				values = new List[numSignals];
				for(int i=0; i<numSignals; i++)
				{
					if (i != 0)
					{
						signals[i-1] = new AnalogSignal(an);
						signals[i-1].setSignalName((String)signalNames.get(i));
					}
					values[i] = new ArrayList<Double>();
				}
				continue;
			}

			// skip first word if there is an "=" in the line
			int equalPos = line.indexOf("=");

			if (equalPos >= 0)
            {
                if (line.length() > (equalPos+3))
                    line = line.substring(equalPos+3);
                else
                {
                    System.out.println("Missing value after '='.  This may not be a PSpice output file.");
                    return null;
                }
            }

			// read the data values
			int ptr = 0;
			int position = 0;
			for(;;)
			{
				while (ptr < line.length() && Character.isWhitespace(line.charAt(ptr))) ptr++;
				if (ptr >= line.length() || line.charAt(ptr) == ')') break;
				int start = ptr;
				while (ptr < line.length() && !Character.isWhitespace(line.charAt(ptr))) ptr++;
				double value = TextUtils.atof(line.substring(start, ptr));
				values[position++].add(new Double(value));
			}
			if (position != numSignals)
			{
				System.out.println("Line of data has " + position + " values, but expect " + numSignals +
                        ". Unable to recover from error.  This may not be a PSpice output file.");
                return null;
			}
		}

		// convert lists to arrays
		if (numSignals == 0)
		{
            System.out.println("No data found in the file.  This may not be a PSpice output file.");
			return null;
		}
		int numEvents = values[0].size(); 
		an.buildCommonTime(numEvents);
		for(int i=0; i<numEvents; i++)
		{
			an.setCommonTime(i, ((Double)values[0].get(i)).doubleValue());
		}
		for(int j=1; j<numSignals; j++)
		{
			AnalogSignal as = signals[j-1];
			as.buildValues(numEvents);
			for(int i=0; i<numEvents; i++)
			{
				as.setValue(i, ((Double)values[j].get(i)).doubleValue());
			}
		}
		return sd;
	}

}
