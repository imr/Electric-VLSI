/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Simulate.java
 * Input/output tool: superclass for simulation-output formats that display their results in a waveform window.
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.WaveformWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.net.URL;

import javax.swing.JOptionPane;


/**
 * This class reads simulation output files and plots them.
 */
public class Simulate extends Input
{
	Simulate() {}

	/**
	 * Method called from the pulldown menus to read Spice output and plot it.
	 */
	public static void plotSpiceResults()
	{
		FileType type = getCurrentSpiceOutputType();
		if (type == null) return;
		plotSimulationResults(type, null, null, null);
	}

	/**
	 * Method called from the pulldown menus to read Spice output for the current cell.
	 */
	public static void plotSpiceResultsThisCell()
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		FileType type = getCurrentSpiceOutputType();
		if (type == null) return;
		plotSimulationResults(type, cell, null, null);
	}

	/**
	 * Method called from the pulldown menus to read Verilog output and plot it.
	 */
	public static void plotVerilogResults()
	{
		plotSimulationResults(FileType.VERILOGOUT, null, null, null);
	}

	/**
	 * Method called from the pulldown menus to read Verilog output for the current cell.
	 */
	public static void plotVerilogResultsThisCell()
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		plotSimulationResults(FileType.VERILOGOUT, cell, null, null);
	}

	/**
	 * Method called from the pulldown menus to read ArchSim output and plot it.
	 */
	public static void plotArchSimResults()
	{
		plotSimulationResults(FileType.ARCHSIMOUT, null, null, null);
	}

	/**
	 * Method to read simulation output of a given type.
	 */
	public static void plotSimulationResults(FileType type, Cell cell, URL fileURL, WaveformWindow ww)
	{
		Simulate is = null;
		if (type == FileType.ARCHSIMOUT)
		{
			is = (Simulate)new ArchSimOut();
		} else if (type == FileType.HSPICEOUT)
		{
			is = (Simulate)new HSpiceOut();
		} else if (type == FileType.PSPICEOUT)
		{
			is = (Simulate)new PSpiceOut();
		} else if (type == FileType.RAWSPICEOUT)
		{
			is = (Simulate)new RawSpiceOut();
		} else if (type == FileType.RAWSSPICEOUT)
		{
			is = (Simulate)new SmartSpiceOut();
		} else if (type == FileType.SPICEOUT)
		{
			is = (Simulate)new SpiceOut();
		} else if (type == FileType.VERILOGOUT)
		{
			is = (Simulate)new VerilogOut();
		}
		if (is == null)
		{
			System.out.println("Cannot handle " + type.getName() + " files yet");
			return;
		}

		if (cell == null)
		{
			if (fileURL == null)
			{
				String fileName = OpenFile.chooseInputFile(type, null);
				if (fileName == null) return;
				fileURL = TextUtils.makeURLToFile(fileName);
			}
			String cellName = TextUtils.getFileNameWithoutExtension(fileURL);
			Library curLib = Library.getCurrent();
			cell = curLib.findNodeProto(cellName);
			if (cell != null)
			{
				// prompt for the cell
				ArrayList cellNames = new ArrayList();
				for(Iterator it = curLib.getCells(); it.hasNext(); )
				{
					Cell aCell = (Cell)it.next();
					cellNames.add(aCell.describe(false));
				}
				String defaultCell = (String)cellNames.get(0);
				if (cell != null) defaultCell = cell.describe(false);
				String selectedCell = (String)JOptionPane.showInputDialog(null, "Which cell is associated with this data",
					"Cells in " + curLib, JOptionPane.INFORMATION_MESSAGE, null, cellNames.toArray(), defaultCell);
				if (selectedCell != null)
					cell = curLib.findNodeProto(selectedCell);
			}
		} else
		{
			if (fileURL == null)
			{
				String [] extensions = type.getExtensions();
				String filePath = TextUtils.getFilePath(cell.getLibrary().getLibFile());
				String fileName = cell.getName() + "." + extensions[0];
				fileURL = TextUtils.makeURLToFile(filePath + fileName);
			}
		}
		ReadSimulationOutput job = new ReadSimulationOutput(type, is, fileURL, cell, ww);
	}

	/**
	 * Class to read simulation output in a new thread.
	 */
	private static class ReadSimulationOutput extends Job
	{
		FileType type;
		Simulate is;
		URL fileURL;
		Cell cell;
		WaveformWindow ww;

		protected ReadSimulationOutput(FileType type, Simulate is, URL fileURL, Cell cell, WaveformWindow ww)
		{
			super("Read Simulation Output for " + cell, IOTool.getIOTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.type = type;
			this.is = is;
			this.fileURL = fileURL;
			this.cell = cell;
			this.ww = ww;
			startJob();
		}

		public boolean doIt()
		{
			try
			{
				Stimuli sd = is.readSimulationOutput(fileURL, cell);
				if (sd != null)
				{
					sd.setDataType(type);
					sd.setFileURL(fileURL);
					Simulation.showSimulationData(sd, ww);
				}
			} catch (IOException e)
			{
				System.out.println("End of file reached while reading " + fileURL);
			}
			return true;
		}
	}

	/**
	 * Method that is overridden by subclasses to actually do the work.
	 */
	protected Stimuli readSimulationOutput(URL fileURL, Cell cell)
		throws IOException
	{
		return null;
	}

	public static FileType getCurrentSpiceOutputType()
	{
		String format = Simulation.getSpiceOutputFormat();
		int engine = Simulation.getSpiceEngine();
		if (format.equalsIgnoreCase("Standard"))
		{
			if (engine == Simulation.SPICE_ENGINE_H)
				return FileType.HSPICEOUT;
			if (engine == Simulation.SPICE_ENGINE_3 || engine == Simulation.SPICE_ENGINE_P)
				return FileType.PSPICEOUT;
			return FileType.SPICEOUT;
		}
		if (format.equalsIgnoreCase("Raw"))
		{
			return FileType.RAWSPICEOUT;
		}
		if (format.equalsIgnoreCase("Raw/Smart"))
		{
			return FileType.RAWSSPICEOUT;
		}
		return null;
	}

	/*
	 * Method to get the next line of text from the simulator.
	 * Returns null at end of file.
	 */
	protected String getLineFromSimulator()
		throws IOException
	{
		StringBuffer sb = new StringBuffer();
		int bytesRead = 0;
		for(;;)
		{
			int ch = lineReader.read();
			if (ch == -1) return null;
			bytesRead++;
			if (ch == '\n' || ch == '\r') break;
			sb.append((char)ch);
		}
		updateProgressDialog(bytesRead);
		return sb.toString();
	}
}
