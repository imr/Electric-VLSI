/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SmartSpiceOut.java
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
 * Class for reading and displaying waveforms from SmartSpice Raw output.
 * Thease are contained in .??? files.
 */
public class SmartSpiceOut extends Simulate
{
	private boolean eofReached;

	SmartSpiceOut() {}

	/**
	 * Method to read an Smart Spice output file.
	 */
	protected SimData readSimulationOutput(URL fileURL)
		throws IOException
	{
		// open the file
		InputStream stream = TextUtils.getURLStream(fileURL);
		if (stream == null) return null;
		if (openBinaryInput(fileURL, stream)) return null;

		// show progress reading .tr0 file
		startProgressDialog("SmartSpice output", fileURL.getFile());

		// read the actual signal data from the .tr0 file
		SimData sd = readRawSmartSpiceFile();

		// stop progress dialog, close the file
		stopProgressDialog();
		closeInput();

		// return the simulation data
		return sd;
	}

	private SimData readRawSmartSpiceFile()
		throws IOException
	{
//		first = TRUE;
//		numend = NONUMBERS;
//		sim_spice_signals = -1;
//		rowcount = -1;
//		for(;;)
//		{
//			if (stopping(STOPREASONSPICE)) return(-1);
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
//					ttyputerr(_("This is an HSPICE file, not a RAWFILE file"));
//					ttyputerr(_("Change the SPICE format and reread"));
//					return(-1);
//				}
//			}
//
//			if (running != 0)
//			{
//				if (outputfile == NULL)
//				{
//					if (parsemode == SIMRUNYESPARSE) ttyputmsg(x_("%s"), line);
//				} else sim_spice_xprintf(outputfile, FALSE, x_("%s\n"), line);
//			}
//
//			// find the ":" separator
//			for(ptr = line; *ptr != 0; ptr++) if (*ptr == ':') break;
//			if (*ptr == 0) continue;
//			*ptr++ = 0;
//			while (*ptr == ' ' || *ptr == '\t') ptr++;
//
//			if (namesame(line, x_("Plotname")) == 0)
//			{
//				if (sim_spice_cellname[0] == '\0')
//					estrcpy(sim_spice_cellname, ptr);
//				continue;
//			}
//
//			if (namesame(line, x_("No. Variables")) == 0)
//			{
//				sim_spice_signals = eatoi(ptr) - 1;
//				continue;
//			}
//
//			if (namesame(line, x_("No. Points")) == 0)
//			{
//				rowcount = eatoi(ptr);
//				continue;
//			}
//
//			if (namesame(line, x_("Variables")) == 0)
//			{
//				if (sim_spice_signals < 0)
//				{
//					ttyputerr(_("Missing variable count in file"));
//					return(-1);
//				}
//				sim_spice_signames = (CHAR **)emalloc(sim_spice_signals * (sizeof (CHAR *)),
//					sim_tool->cluster);
//				if (sim_spice_signames == 0) return(-1);
//				for(i=0; i<=sim_spice_signals; i++)
//				{
//					if (stopping(STOPREASONSPICE)) return(-1);
//					if (i != 0)
//					{
//						if (sim_spice_getlinefromsimulator(line))
//						{
//							ttyputerr(_("Error: end of file during signal names"));
//							return(-1);
//						}
//						ptr = line;
//					}
//					while (*ptr == ' ' || *ptr == '\t') ptr++;
//					if (myatoi(ptr) != i)
//						ttyputerr(_("Warning: Variable %ld has number %ld"),
//							i, myatoi(ptr));
//					while (*ptr != 0 && *ptr != ' ' && *ptr != '\t') ptr++;
//					while (*ptr == ' ' || *ptr == '\t') ptr++;
//					start = ptr;
//					while (*ptr != 0 && *ptr != ' ' && *ptr != '\t') ptr++;
//					*ptr = 0;
//					if (i == 0)
//					{
//						if (namesame(start, x_("time")) != 0)
//							ttyputerr(_("Warning: the first variable should be time, is '%s'"),
//								start);
//					} else
//					{
//						(void)allocstring(&sim_spice_signames[i-1], start, sim_tool->cluster);
//					}
//				}
//				continue;
//			}
//			if (namesame(line, x_("Values")) == 0)
//			{
//				if (sim_spice_signals < 0)
//				{
//					ttyputerr(_("Missing variable count in file"));
//					return(-1);
//				}
//				if (rowcount < 0)
//				{
//					ttyputerr(_("Missing point count in file"));
//					return(-1);
//				}
//				for(j=0; j<rowcount; j++)
//				{
//					num = sim_spice_allocnumbers(sim_spice_signals);
//					if (num == NONUMBERS) return(-1);
//					if (numend == NONUMBERS) sim_spice_numbers = num; else
//						numend->nextnumbers = num;
//					num->nextnumbers = NONUMBERS;
//					numend = num;
//
//					if (sim_spice_filelen > 0)
//						DiaSetProgress(dia, sim_spice_filepos, sim_spice_filelen);
//
//					for(i=0; i<=sim_spice_signals; i++)
//					{
//						if (stopping(STOPREASONSPICE)) return(-1);
//						if (sim_spice_getlinefromsimulator(line))
//						{
//							ttyputerr(_("Error: end of file during data points (read %ld out of %ld)"),
//								j, rowcount);
//							return(-1);
//						}
//						ptr = line;
//						if (i == 0)
//						{
//							if (myatoi(line) != j)
//								ttyputerr(_("Warning: data point %ld has number %ld"),
//									j, myatoi(line));
//							while(*ptr != 0 && *ptr != ' ' && *ptr != '\t') ptr++;
//						}
//						while (*ptr == ' ' || *ptr == '\t') ptr++;
//						if (i == 0) num->time = eatof(ptr); else
//							num->list[i-1] = (float)eatof(ptr);
//					}
//				}
//			}
//			if (namesame(line, x_("Binary")) == 0)
//			{
//				if (sim_spice_signals < 0)
//				{
//					ttyputerr(_("Missing variable count in file"));
//					return(-1);
//				}
//				if (rowcount < 0)
//				{
//					ttyputerr(_("Missing point count in file"));
//					return(-1);
//				}
//				inputbuffer = (double *)emalloc(sim_spice_signals * (sizeof (double)), sim_tool->cluster);
//				if (inputbuffer == 0) return(-1);
//
//				// read the data
//				for(j=0; j<rowcount; j++)
//				{
//					num = sim_spice_allocnumbers(sim_spice_signals);
//					if (num == NONUMBERS) return(-1);
//					if (numend == NONUMBERS) sim_spice_numbers = num; else
//						numend->nextnumbers = num;
//					num->nextnumbers = NONUMBERS;
//					numend = num;
//
//					if (sim_spice_filelen > 0 )
//						DiaSetProgress(dia, sim_spice_filepos, sim_spice_filelen);
//
//					if (stopping(STOPREASONSPICE)) return(-1);
//					i = xfread((UCHAR1 *)(&num->time), sizeof(double), 1, sim_spice_streamfromsim);
//					if (i != 1)
//					{
//						ttyputerr(_("Error: end of file during data points (read %ld out of %ld)"),
//							j, rowcount);
//						return(-1);
//					}
//#ifdef WIN32
//					num->time = sim_spice_swapendian(num->time);
//					if (_finite(num->time) == 0) num->time = 0.0;
//#endif
//					i = xfread((UCHAR1 *)inputbuffer, sizeof(double), sim_spice_signals, sim_spice_streamfromsim);
//					if (i != sim_spice_signals)
//					{
//						ttyputerr(_("Error: end of file during data points (read only %ld of %ld signals on row %ld out of %ld)"),
//							i, sim_spice_signals, j, rowcount);
//						return(-1);
//					}
//					for(i=0; i<sim_spice_signals; i++)
//					{
//#if defined(_BSD_SOURCE) || defined(_GNU_SOURCE)
//						if (isinf(inputbuffer[i]) != 0) inputbuffer[i] = 0.0; else
//							if (isnan(inputbuffer[i]) != 0) inputbuffer[i] = 0.0;
//#endif
//#ifdef WIN32
//						// convert to little-endian
//						inputbuffer[i] = sim_spice_swapendian(inputbuffer[i]);
//						if (_finite(inputbuffer[i]) == 0) inputbuffer[i] = 0.0;
//#endif
//						num->list[i] = (float)inputbuffer[i];
//					}
//				}
//				efree((CHAR *)inputbuffer);
//			}
//		}
//
//		if (sim_spice_signals > 0)
//		{
//			sim_spice_sigtypes = (INTSML *)emalloc(sim_spice_signals * SIZEOFINTSML, sim_tool->cluster);
//			if (sim_spice_sigtypes == 0) return(-1);
//		}
//		return(sim_spice_signals);

//		SimData sd = new SimData();
//		sd.signalNames = sim_spice_signames;
//		sd.events = sim_spice_numbers;
		System.out.println("CANNOT READ SMARTSPICE RAW OUTPUT YET");
		return null;
	}

}
