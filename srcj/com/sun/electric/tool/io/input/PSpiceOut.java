/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PSpiceOut.java
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

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.io.input.Simulate;

import java.io.InputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.DataInputStream;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Class for reading and displaying waveforms from PSpice and Spice3 output.
 * Thease are contained in .tr0 and .pa0 files.
 */
public class PSpiceOut extends Simulate
{
	private boolean eofReached;

	PSpiceOut() {}

	/**
	 * Method to read an PSpice output file.
	 */
	protected SimData readSimulationOutput(URL fileURL)
		throws IOException
	{
		// open the file
		InputStream stream = TextUtils.getURLStream(fileURL);
		if (stream == null) return null;
		if (openBinaryInput(fileURL, stream)) return null;

		// show progress reading .tr0 file
		startProgressDialog("PSpice output", fileURL.getFile());

		// read the actual signal data from the .tr0 file
		SimData sd = readPSpiceFile();

		// stop progress dialog, close the file
		stopProgressDialog();
		closeInput();

		// return the simulation data
		return sd;
	}

	private SimData readPSpiceFile()
		throws IOException
	{
//		numend = NONUMBERS;
//		first = TRUE;
//		knows = FALSE;
//		for(;;)
//		{
//			if (stopping(STOPREASONSPICE)) break;
//			if (sim_spice_getlinefromsimulator(line)) break;
//			if (sim_spice_filelen > 0)
//				DiaSetProgress(dia, sim_spice_filepos, sim_spice_filelen);
//
//			if (first)
//			{
//				// check the first line for HSPICE format possibility
//				first = FALSE;
//				if (estrlen(line) >= 20 && line[16] == '9' && line[17] == '0' &&
//					line[18] == '0' && line[19] == '7')
//				{
//					ttyputerr(_("This is an HSPICE file, not a SPICE3/PSPICE file"));
//					ttyputerr(_("Change the SPICE format and reread"));
//					return(-1);
//				}
//			}
//
//			// skip first word if there is an "=" in the line
//			for(ptr = line; *ptr != 0; ptr++) if (*ptr == '=') break;
//			if (*ptr == 0) ptr = line; else ptr += 3;
//
//			// read the data values
//			lastt = 0.0;
//			for(;;)
//			{
//				while (*ptr == ' ' || *ptr == '\t') ptr++;
//				if (*ptr == 0 || *ptr == ')') break;
//				if (sim_spice_signals == 0)
//				{
//					num = sim_spice_allocnumbers(MAXTRACES);
//					num->time = eatof(ptr);
//					if (numend == NONUMBERS) sim_spice_numbers = num; else
//					{
//						numend->nextnumbers = num;
//						if (num->time <= lastt && !knows)
//						{
//							ttyputerr(_("First trace (should be 'time') is not increasing in value"));
//							knows = TRUE;
//						}
//					}
//					lastt = num->time;
//					num->nextnumbers = NONUMBERS;
//					numend = num;
//				} else
//				{
//					if (num == NONUMBERS) ttyputmsg(_("Line %ld of data has too many values"),
//						sim_spice_signals); else
//					{
//						if (sim_spice_signals <= MAXTRACES)
//							num->list[sim_spice_signals-1] = (float)eatof(ptr);
//						num = num->nextnumbers;
//					}
//				}
//				while (*ptr != ' ' && *ptr != '\t' && *ptr != 0) ptr++;
//			}
//
//			// see if there is an ")" at the end of the line
//			if (line[estrlen(line)-1] == ')')
//			{
//				// advance to the next value for subsequent reads
//				if (sim_spice_signals != 0 && num != NONUMBERS)
//					ttyputmsg(_("Line %ld of data has too few values"), sim_spice_signals);
//				sim_spice_signals++;
//				num = sim_spice_numbers;
//			}
//
//			if (running != 0)
//			{
//				if (outputfile == NULL)
//				{
//					if (parsemode == SIMRUNYESPARSE)
//						ttyputmsg(x_("%s"), (isprint(*line) ? line : &line[1]));
//				} else sim_spice_xprintf(outputfile, FALSE, x_("%s\n"), line);
//			}
//		}
//
//		// generate dummy names
//		sim_spice_signames = (CHAR **)emalloc(sim_spice_signals * (sizeof (CHAR *)), sim_tool->cluster);
//		sim_spice_sigtypes = (INTSML *)emalloc(sim_spice_signals * SIZEOFINTSML, sim_tool->cluster);
//		if (sim_spice_signames == 0 || sim_spice_sigtypes == 0)
//		{
//			// terminate execution so we can restart simulator
//			return(-1);
//		}
//		for(i=0; i<sim_spice_signals; i++)
//		{
//			(void)esnprintf(line, MAXLINE+5, x_("Signal %ld"), i+1);
//			(void)allocstring(&sim_spice_signames[i], line, sim_tool->cluster);
//		}
//		return(sim_spice_signals);

//		SimData sd = new SimData();
//		sd.signalNames = sim_spice_signames;
//		sd.events = sim_spice_numbers;
		System.out.println("CANNOT READ PSPICE OUTPUT YET");
		return null;
	}

}
