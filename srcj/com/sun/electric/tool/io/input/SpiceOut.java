/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SpiceOut.java
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
import java.io.LineNumberReader;
import java.io.DataInputStream;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Class for reading and displaying waveforms from Spice2 or GNUCap output.
 * Thease are contained in .spo files.
 */
public class SpiceOut extends Simulate
{
	private boolean eofReached;

	SpiceOut() {}

	/**
	 * Method to read an Spice output file.
	 */
	protected SimData readSimulationOutput(URL fileURL, Cell cell)
		throws IOException
	{
		// open the file
		if (openTextInput(fileURL)) return null;

		// show progress reading .tr0 file
		startProgressDialog("Spice output", fileURL.getFile());

		// read the actual signal data from the .tr0 file
		SimData sd = readSpiceFile(cell);

		// stop progress dialog, close the file
		stopProgressDialog();
		closeInput();

		// return the simulation data
		return sd;
	}

	private final static String CELLNAME_HEADER = "*** SPICE deck for cell ";

	private SimData readSpiceFile(Cell cell)
		throws IOException
	{
		boolean datamode = false;
		boolean first = true;
		boolean past_end = false;
		String sim_spice_cellname = null;
		int mostSignals = 0;
		List allNumbers = new ArrayList();
		for(;;)
		{
			String line = getLine();
			if (line == null) break;
			if (first)
			{
				// check the first line for HSPICE format possibility
				first = false;
				if (line.length() >= 20 && line.substring(16,20).equals("9007"))
				{
					System.out.println("This is an HSPICE file, not a SPICE2 file");
					System.out.println("Change the SPICE format and reread");
					return null;
				}
			}
			int len = line.length();
			if (len < 2) continue;

			// look for a cell name
			if (sim_spice_cellname == null && line.startsWith(CELLNAME_HEADER))
				sim_spice_cellname = line.substring(CELLNAME_HEADER.length());
			if (line.startsWith(".END") && !line.startsWith(".ENDS"))
			{
				past_end = true;
				continue;
			}
			if (line.startsWith("#Time"))
			{
//				sim_spice_signals = 0;
//				ptr = line + 5;
//				for(;;)
//				{
//					while (isWhitespace(*ptr)) ptr++;
//					if (*ptr == 0) break;
//					CHAR *pt = ptr;
//					while (!isWhitespace(*pt)) pt++;
//					CHAR save = *pt;
//					*pt = 0;
//					CHAR **newsignames = (CHAR **)emalloc((sim_spice_signals + 1) * (sizeof (CHAR *)), sim_tool->cluster);
//					for (i = 0; i < sim_spice_signals; i++) newsignames[i] = sim_spice_signames[i];
//					(void)allocstring(&newsignames[sim_spice_signals], ptr, sim_tool->cluster);
//					if (sim_spice_signames != 0) efree((CHAR*)sim_spice_signames);
//					sim_spice_signames = newsignames;
//					sim_spice_signals++;
//					*pt = save;
//					ptr = pt;
//					while (*ptr != ' ' && *ptr != 0) ptr++;
//				}
//				if (sim_spice_signals == sim_spice_printlistlen)
//				{
//					for (i = 0; i < sim_spice_signals; i++)
//					{
//						efree(sim_spice_signames[i]);
//						(void)allocstring(&sim_spice_signames[i], sim_spice_printlist[i], sim_tool->cluster);
//					}
//				}
				past_end = true;
				continue;
			}
			if (past_end && !datamode)
			{
				if ((Character.isWhitespace(line.charAt(0)) || line.charAt(0) == '-') && Character.isDigit(line.charAt(1)))
					datamode = true;
			}
			if (past_end && datamode)
			{
				if (!((Character.isWhitespace(line.charAt(0)) || line.charAt(0) == '-') && Character.isDigit(line.charAt(1))))
				{
					datamode = false;
					past_end = false;
				}
			}
			if (datamode)
			{
				List numbers = new ArrayList();
				int ptr = 0;
				while (ptr < len)
				{
					while (ptr < len && Character.isWhitespace(line.charAt(ptr))) ptr++;
					int start = ptr;
					while (ptr < len && !Character.isWhitespace(line.charAt(ptr))) ptr++;
					numbers.add(new Double(TextUtils.atof(line.substring(start, ptr))));
					ptr++;
				}
				if (numbers.size() > mostSignals) mostSignals = numbers.size();
				allNumbers.add(numbers);
			}
		}

		// generate dummy names
		mostSignals--;
		if (mostSignals <= 0)
		{
			System.out.println("No data found in the file");
			return null;
		}

		SimData sd = new SimData();
		sd.setCell(cell);

		// convert lists to arrays
		int numEvents = allNumbers.size();
		sd.buildCommonTime(numEvents);
		for(int i=0; i<numEvents; i++)
		{
			List row = (List)allNumbers.get(i);
			sd.setCommonTime(i, ((Double)row.get(0)).doubleValue());
		}
		for(int j=0; j<mostSignals; j++)
		{
			Simulate.SimAnalogSignal as = new Simulate.SimAnalogSignal(sd);
			as.setSignalName("Signal " + (j+1));
			as.setCommonTimeUse(true);
			as.buildValues(numEvents);
			for(int i=0; i<numEvents; i++)
			{
				List row = (List)allNumbers.get(i);
				as.setValue(i, ((Double)row.get(j+1)).doubleValue());
			}
		}
		return sd;
	}

}
