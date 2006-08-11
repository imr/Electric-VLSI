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
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.dialogs.CellBrowser;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.io.IOException;
import java.io.File;
import java.net.URL;


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
		UserInterface ui = Job.getUserInterface();
		Cell cell = ui.needCurrentCell();
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
		UserInterface ui = Job.getUserInterface();
		Cell cell = ui.needCurrentCell();
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

    private static Simulate getSimulate(FileType type) {
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
        } else if (type == FileType.EPIC)
        {
            if (Simulation.isSpiceEpicReaderProcess())
                is = (Simulate)new EpicOutProcess();
            else
               is = (Simulate)new EpicOut();
        } else if (type == FileType.VERILOGOUT)
        {
            is = (Simulate)new VerilogOut();
        }
        return is;
    }

	/**
	 * Method to read simulation output of a given type.
	 */
	public static void plotSimulationResults(FileType type, Cell cell, URL fileURL, WaveformWindow ww)
	{
		Simulate is = getSimulate(type);
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
			if (cell == null)
			{
				CellBrowser dialog = new CellBrowser(TopLevel.getCurrentJFrame(), true, CellBrowser.DoAction.selectCell);
		        dialog.setVisible(true);
				cell = dialog.getSelectedCell();
				if (cell == null) return;
			}
		} else
		{
			if (fileURL == null)
			{
                String [] extensions = type.getExtensions();
                String fileName = cell.getName() + "." + extensions[0];
                // look for file in library path
                String filePath = TextUtils.getFilePath(cell.getLibrary().getLibFile());
                File file = new File(filePath, fileName);
                if (!file.exists()) {
                    // look for file in spice working directory
                    String dir = type.getGroupPath();
                    file = new File(dir, fileName);
                }
                fileURL = TextUtils.makeURLToFile(file.getPath());
			}
		}
		(new ReadSimulationOutput(type, is, fileURL, cell, ww)).startJob();
	}

    public static Stimuli readSimulationResults(FileType type, Cell cell, URL fileURL) {
        Simulate is = getSimulate(type);
        if (is == null)
        {
            System.out.println("Cannot handle " + type.getName() + " files yet");
            return null;
        }
        if (cell == null) {
            System.out.println("Error reading simulation results; specified Cell is null");
            return null;
        }
        if (fileURL == null) {
            System.out.println("Error reading simulation results; specified file is null");
            return null;
        }
        ReadSimulationOutput job = new ReadSimulationOutput(type, is, fileURL, cell, null);
        try {
            job.doIt();
        } catch (JobException e) {
            System.out.println(e.getMessage());
            return null;
        }
        return job.sd;
    }

	/**
	 * Class to read simulation output in a new thread.
     * This job is examine job, so it contain non-serializable fields.
     * However, showSimulationData should be shown in GUI thread (in terminateOK).
	 */
	private static class ReadSimulationOutput extends Job
	{
		private FileType type;
		private Simulate is;
		private URL fileURL;
		private Cell cell;
		private WaveformWindow ww;
        private Stimuli sd;

		private ReadSimulationOutput(FileType type, Simulate is, URL fileURL, Cell cell, WaveformWindow ww)
		{
			super("Read Simulation Output for " + cell, IOTool.getIOTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.type = type;
			this.is = is;
			this.fileURL = fileURL;
			this.cell = cell;
			this.ww = ww;
		}

		public boolean doIt() throws JobException
		{
			try
			{
				sd = is.readSimulationOutput(fileURL, cell);
				if (sd != null)
				{
					sd.setDataType(type);
					sd.setFileURL(fileURL);
//					Simulation.showSimulationData(sd, ww);
				}
			} catch (IOException e)
			{
				System.out.println("End of file reached while reading " + fileURL);
			}
			return true;
		}
        
        public void terminateOK() {
            if (sd != null)
                Simulation.showSimulationData(sd, ww);
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
		Simulation.SpiceEngine engine = Simulation.getSpiceEngine();
		if (format.equalsIgnoreCase("Standard"))
		{
			if (engine == Simulation.SpiceEngine.SPICE_ENGINE_H)
				return FileType.HSPICEOUT;
			if (engine == Simulation.SpiceEngine.SPICE_ENGINE_3 || engine == Simulation.SpiceEngine.SPICE_ENGINE_P)
				return FileType.PSPICEOUT;
			return FileType.SPICEOUT;
		}
		if (format.equalsIgnoreCase("Raw"))
		{
			return FileType.RAWSPICEOUT;
		}
		if (format.equalsIgnoreCase("RawSmart"))
		{
			return FileType.RAWSSPICEOUT;
		}
        if (format.equalsIgnoreCase("Epic"))
        {
            return FileType.EPIC;
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
    
	/**
	 * Method to remove the leading "x" character in each dotted part of a string.
	 * HSpice decides to add "x" in front of every cell name, so the path "me.you"
	 * appears as "xme.xyou".
	 * @param name the string from HSpice.
	 * @return the string without leading "X"s.
	 */
	static String removeLeadingX(String name)
	{
		// remove all of the "x" characters at the start of every instance name
		int dotPos = -1;
		for(;;)
		{
			int xPos = dotPos + 1;
			if (name.length() > xPos && name.charAt(xPos) == 'x')
			{
				name = name.substring(0, xPos) + name.substring(xPos+1);
			}
			dotPos = name.indexOf('.', xPos);
			if (dotPos < 0) break;
		}
		return name;
	}
}
