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
	protected SimData readSimulationOutput(URL fileURL)
		throws IOException
	{
		// open the file
		InputStream stream = TextUtils.getURLStream(fileURL, null);
		if (stream == null) return null;
		if (openBinaryInput(fileURL, stream)) return null;

		// show progress reading .tr0 file
		startProgressDialog("Spice output", fileURL.getFile());

		// read the actual signal data from the .tr0 file
		SimData sd = readSpiceFile();

		// stop progress dialog, close the file
		stopProgressDialog();
		closeInput();

		// return the simulation data
		return sd;
	}

	private SimData readSpiceFile()
		throws IOException
	{
//		datamode = FALSE;
//		first = TRUE;
//		numend = NONUMBERS;
//		past_end = FALSE;
//		sim_spice_signames = 0;
//		numbers_limit = 256;
//		numbers =  (float*)emalloc(numbers_limit * (sizeof (float)), sim_tool->cluster);
//		if (numbers == NULL)
//		{
//			ttyputnomemory();
//			return(-1);
//		}
//		for(;;)
//		{
//			if (stopping(STOPREASONSPICE)) break;
//			if (sim_spice_getlinefromsimulator(line)) break;
//			if (sim_spice_filelen > 0)
//				DiaSetProgress(dia, sim_spice_filepos, sim_spice_filelen);
//			if (first)
//			{
//				// check the first line for HSPICE format possibility
//				first = FALSE;
//				if (estrlen(line) >= 20 && line[16] == '9' && line[17] == '0' &&
//					line[18] == '0' && line[19] == '7')
//				{
//					ttyputerr(_("This is an HSPICE file, not a SPICE2 or Gnucap file"));
//					ttyputerr(_("Change the SPICE format and reread"));
//					efree((CHAR*)numbers);
//					return(-1);
//				}
//			}
//			if (running != 0)
//			{
//				if (outputfile == NULL)
//				{
//					if (parsemode == SIMRUNYESPARSE)
//						ttyputmsg(x_("%s"), (isprint(*line) ? line : &line[1]));
//				} else sim_spice_xprintf(outputfile, FALSE, x_("%s\n"), line);
//			}
//
//			// look for a cell name
//			if ((sim_spice_cellname[0] == '\0') && (namesamen(line, x_("*** SPICE deck for cell "),25) == 0))
//				if ((i = esscanf(line+25,x_("%s"),sim_spice_cellname)) != 1)
//					sim_spice_cellname[0] = '\0';
//			if (namesamen(line, x_(".END"), 4) == 0 && namesamen(line, x_(".ENDS"), 5) != 0)
//			{
//				past_end = TRUE;
//				continue;
//			}
//			if (namesamen(line, x_("#Time"), 5) == 0)
//			{
//				sim_spice_signals = 0;
//				ptr = line + 5;
//				for(;;)
//				{
//					while (isspace(*ptr)) ptr++;
//					if (*ptr == 0) break;
//					CHAR *pt = ptr;
//					while (!isspace(*pt)) pt++;
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
//				past_end = TRUE;
//				continue;
//			}
//			if (past_end && !datamode)
//			{
//				if ((isspace(line[0]) || line[0] == '-') && isdigit(line[1]))
//					datamode = TRUE;
//			}
//			if (past_end && datamode)
//			{
//				if (!((isspace(line[0]) || line[0] == '-') && isdigit(line[1])))
//				{
//					datamode = FALSE;
//					past_end = FALSE;
//				}
//			}
//			if (datamode)
//			{
//				ptr = line;
//				sim_spice_signals = 0;
//				for(;;)
//				{
//					while (isspace(*ptr)) ptr++;
//					if (*ptr == 0) break;
//					CHAR *pt = ptr;
//					while (isalnum(*pt) || *pt == '.' || *pt == '+' || *pt == '-') pt++;
//					CHAR save = *pt;
//					*pt = 0;
//					if (sim_spice_signals >= numbers_limit)
//					{
//						float *newnumbers =  (float*)emalloc(numbers_limit * 2 * (sizeof (float)), sim_tool->cluster);
//						if (newnumbers == NULL)
//						{
//							ttyputnomemory();
//							return(-1);
//						}
//						for (i = 0; i < sim_spice_signals; i++) newnumbers[i] = numbers[i];
//						efree((CHAR*)numbers);
//						numbers = newnumbers;
//						numbers_limit *= 2;
//
//					}
//					numbers[sim_spice_signals++] = (float)figureunits(ptr, VTUNITSNONE, 0);
//					*pt = save;
//					ptr = pt;
//					while (*ptr != ' ' && *ptr != 0) ptr++;
//				}
//				if (sim_spice_signals > 1)
//				{
//					sim_spice_signals--;
//					num = sim_spice_allocnumbers(sim_spice_signals);
//					if (num == NONUMBERS) break;
//					num->time = numbers[0];
//					for(i=0; i<sim_spice_signals; i++) num->list[i] = numbers[i+1];
//					if (numend == NONUMBERS) sim_spice_numbers = num; else
//						numend->nextnumbers = num;
//					num->nextnumbers = NONUMBERS;
//					numend = num;
//				}
//			}
//		}
//		efree((CHAR*)numbers);
//
//		// generate dummy names
//		if (sim_spice_signames == 0)
//		{
//			sim_spice_signames = (CHAR **)emalloc(sim_spice_signals * (sizeof (CHAR *)), sim_tool->cluster);
//			if (sim_spice_signames == 0)
//			{
//				// terminate execution so we can restart simulator
//				return(-1);
//			}
//			for(i=0; i<sim_spice_signals; i++)
//			{
//				(void)esnprintf(line, MAXLINE+5, x_("Signal %ld"), i+1);
//				(void)allocstring(&sim_spice_signames[i], line, sim_tool->cluster);
//			}
//		}
//		sim_spice_sigtypes = (INTSML *)emalloc(sim_spice_signals * SIZEOFINTSML, sim_tool->cluster);
//		if (sim_spice_sigtypes == 0)
//		{
//			// terminate execution so we can restart simulator
//			return(-1);
//		}
//		return(sim_spice_signals);

//		SimData sd = new SimData();
//		sd.signalNames = sim_spice_signames;
//		sd.events = sim_spice_numbers;
		System.out.println("CANNOT READ SPICE OUTPUT YET");
		return null;
	}

}
