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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.simulation.Simulation;

import java.io.IOException;
import java.net.URL;

/**
 * Class for reading and displaying waveforms from SmartSpice Raw output.
 * Thease are contained in .dump files.
 */
public class SmartSpiceOut extends Simulate
{
	private boolean eofReached;

	SmartSpiceOut() {}

	/**
	 * Method to read an Smart Spice output file.
	 */
	protected Simulation.SimData readSimulationOutput(URL fileURL, Cell cell)
		throws IOException
	{
		// open the file
		if (openBinaryInput(fileURL)) return null;

		// show progress reading .tr0 file
		startProgressDialog("SmartSpice output", fileURL.getFile());

		// read the actual signal data from the .tr0 file
		Simulation.SimData sd = readRawSmartSpiceFile(cell);

		// stop progress dialog, close the file
		stopProgressDialog();
		closeInput();

		// return the simulation data
		return sd;
	}

	private Simulation.SimData readRawSmartSpiceFile(Cell cell)
		throws IOException
	{
		boolean first = true;
		String sim_spice_cellname = null;
//		numend = NONUMBERS;
		int sim_spice_signals = -1;
		String [] sim_spice_signames;
		int rowcount = -1;
		for(;;)
		{
			String line = getLineFromBinary();
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

			// find the ":" separator
			int colonPos = line.indexOf(':');
			if (colonPos < 0) continue;
			String keyWord = line.substring(0, colonPos);
			String restOfLine = line.substring(colonPos+1).trim();

			if (keyWord.equals("Plotname"))
			{
				if (sim_spice_cellname == null) sim_spice_cellname = restOfLine;
				continue;
			}

			if (keyWord.equals("No. Variables"))
			{
				sim_spice_signals = TextUtils.atoi(restOfLine) - 1;
				continue;
			}

			if (keyWord.equals("No. Points"))
			{
				rowcount = TextUtils.atoi(restOfLine);
				continue;
			}

			if (keyWord.equals("Variables"))
			{
				if (sim_spice_signals < 0)
				{
					System.out.println("Missing variable count in file");
					return null;
				}
				sim_spice_signames = new String[sim_spice_signals];
				for(int i=0; i<=sim_spice_signals; i++)
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
					if (i == 0)
					{
						if (!name.equals("time"))
							System.out.println("Warning: the first variable should be time, is '" + name + "'");
					} else
					{
						sim_spice_signames[i-1] = name;
					}
				}
				continue;
			}
//			if (keyWord.equals("Values"))
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
//			if (keyWord.equals("Binary"))
//			{
//				if (sim_spice_signals < 0)
//				{
//					System.out.println("Missing variable count in file");
//					return null;
//				}
//				if (rowcount < 0)
//				{
//					System.out.println("Missing point count in file");
//					return null;
//				}
//				double [] inputbuffer = new double[sim_spice_signals];
//
//				// read the data
//				for(int j=0; j<rowcount; j++)
//				{
//					num = sim_spice_allocnumbers(sim_spice_signals);
//					if (num == NONUMBERS) return(-1);
//					if (numend == NONUMBERS) sim_spice_numbers = num; else
//						numend->nextnumbers = num;
//					num->nextnumbers = NONUMBERS;
//					numend = num;
//
//					i = xfread((UCHAR1 *)(&num->time), sizeof(double), 1, sim_spice_streamfromsim);
//					if (i != 1)
//					{
//						System.out.println("Error: end of file during data points (read %ld out of %ld)"),
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
//						System.out.println("Error: end of file during data points (read only %ld of %ld signals on row %ld out of %ld)"),
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
		}
//
//		if (sim_spice_signals > 0)
//		{
//			sim_spice_sigtypes = (INTSML *)emalloc(sim_spice_signals * SIZEOFINTSML, sim_tool->cluster);
//			if (sim_spice_sigtypes == 0) return(-1);
//		}
//		return(sim_spice_signals);

//		SimData sd = new SimData();
//		sd.setCell(cell);
//		sd.signalNames = sim_spice_signames;
//		sd.events = sim_spice_numbers;
		System.out.println("CANNOT READ SMARTSPICE RAW OUTPUT YET");
		return null;
	}

}
