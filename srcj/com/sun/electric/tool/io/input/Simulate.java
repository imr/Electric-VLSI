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

import com.sun.electric.database.Environment;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.UserInterfaceExec;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.verilog.VerilogOut;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.dialogs.CellBrowser;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.swing.SwingUtilities;

/**
 * This class reads simulation output files and plots them.
 */
public abstract class Simulate extends Input
{
	public Simulate() {}

	/**
	 * Method called from the pulldown menus to read Spice output and plot it.
	 */
	public static void plotSpiceResults()
	{
		plotSimulationResults(FileType.SPICE, null, null, null);
	}

	/**
	 * Method called from the pulldown menus to read Spice output for the current cell.
	 */
	public static void plotSpiceResultsThisCell()
	{
		UserInterface ui = Job.getUserInterface();
		Cell cell = ui.needCurrentCell();
		if (cell == null) return;
		plotSimulationResults(FileType.SPICE, cell, null, null);
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

    private static Simulate getSimulate(FileType type, URL fileURL) {
        Simulate is = null;
        if (type == FileType.SPICE) do { // autodetect
                String extension = fileURL.getPath();
                if (extension.indexOf('.')!=-1) extension = extension.substring(extension.lastIndexOf('.')+1);
                if (extension.equals("txt")) { type = FileType.PSPICEOUT; break; }
                if (extension.equals("raw")) { type = FileType.RAWLTSPICEOUT; break; }
                if (extension.equals("dump")) { type = FileType.RAWSPICEOUT; break; }
                if (extension.equals("spo")) { type = FileType.SPICEOUT; break; }
                if (extension.equals("out")) { type = FileType.EPIC; break; }
                if (extension.startsWith("tr") || extension.startsWith("sw") || extension.startsWith("ic") ||
                    extension.startsWith("ac") || extension.startsWith("mt") || extension.startsWith("pa"))
                    { type = FileType.HSPICEOUT; break; }
                // future feature: try to guess the file type from the first few lines
                throw new RuntimeException("unable to detect type for extension \""+extension+"\"");
            } while (false);

        if (type == FileType.HSPICEOUT)           is = new HSpiceOut();
        else if (type == FileType.PSPICEOUT)      is = new PSpiceOut();
        else if (type == FileType.RAWSPICEOUT)    is = new RawSpiceOut();
        else if (type == FileType.RAWLTSPICEOUT)  is = new LTSpiceOut();
        else if (type == FileType.RAWSSPICEOUT)   is = new SmartSpiceOut();
        else if (type == FileType.SPICEOUT)       is = new SpiceOut();
        else if (type == FileType.EPIC)           is = new EpicOut.EpicOutProcess();
        else if (type == FileType.VERILOGOUT)     is = new VerilogOut();
        return is;
    }

	/**
	 * Method to read simulation output of a given type.
	 */
	public static void plotSimulationResults(FileType type, Cell cell, URL fileURL, WaveformWindow ww)
	{
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
				CellBrowser dialog = new CellBrowser(TopLevel.getCurrentJFrame(), true, CellBrowser.DoAction.selectCellToAssoc);
		        dialog.setVisible(true);
				cell = dialog.getSelectedCell();
				if (cell == null) return;
			}
		} else
		{
			if (fileURL == null)
			{
                if (type == FileType.SPICE) {
                    if      (new File(type.getGroupPath(), cell.getName()+".raw").exists()) type = FileType.RAWLTSPICEOUT;
                    else if (new File(type.getGroupPath(), cell.getName()+".dump").exists()) type = FileType.RAWSPICEOUT;
                    else if (new File(type.getGroupPath(), cell.getName()+".spo").exists()) type = FileType.SPICEOUT;
                    else if (new File(type.getGroupPath(), cell.getName()+".out").exists()) type = FileType.EPIC;
                    else if (new File(type.getGroupPath(), cell.getName()+".tr0").exists()) type = FileType.HSPICEOUT;
                    else if (new File(type.getGroupPath(), cell.getName()+".txt").exists()) type = FileType.PSPICEOUT;
                }

                String fileName = cell.getName() + "." + type.getFirstExtension();

                // look for file in library path
                String filePath = TextUtils.getFilePath(cell.getLibrary().getLibFile());
                File file = new File(filePath, fileName);
                if (!file.exists())
                {
                    // look for file in spice working directory
                    String dir = type.getGroupPath();
                    File altFile = new File(dir, fileName);
                    if (altFile.exists()) file = altFile;
                }
                fileURL = TextUtils.makeURLToFile(file.getPath());
			}
		}
		Simulate is = getSimulate(type, fileURL);
		if (is == null)
		{
			System.out.println("Cannot handle " + type.getName() + " files yet");
			return;
		}
		(new ReadSimulationOutput(type, is, fileURL, cell, ww)).start();
	}

    public static Stimuli readSimulationResults(FileType type, Cell cell, URL fileURL) {
        Simulate is = getSimulate(type, fileURL);
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
        job.run();
        return job.sd;
    }

	/**
	 * Class to read simulation output in a new thread.
	 */
	private static class ReadSimulationOutput extends Thread
	{
		private FileType type;
		private Simulate is;
		private URL fileURL;
		private Cell cell;
		private WaveformWindow ww;
        private Stimuli sd;
        private final Environment launcherEnvironment;
        private final UserInterfaceExec userInterface;

		private ReadSimulationOutput(FileType type, Simulate is, URL fileURL, Cell cell, WaveformWindow ww)
		{
			this.type = type;
			this.is = is;
			this.fileURL = fileURL;
			this.cell = cell;
			this.ww = ww;
            sd = new Stimuli();
            launcherEnvironment = Environment.getThreadEnvironment();
            userInterface = new UserInterfaceExec();
		}

		public void run()
		{
            if (Thread.currentThread() == this) {
                Environment.setThreadEnvironment(launcherEnvironment);
                Job.setUserInterface(userInterface);
            }

			try
			{
                is.readSimulationOutput(sd, fileURL, cell);
				if (sd != null)
				{
					sd.setDataType(type);
					sd.setFileURL(fileURL);
                    final Stimuli sdx = sd;
                    final WaveformWindow wwx = ww;
                    SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                Simulation.showSimulationData(sdx, wwx);
                            }});
				}
			} catch (IOException e)
			{
				System.out.println("End of file reached while reading " + fileURL);
			}
		}
	}

	/**
	 * Method that is overridden by subclasses to actually do the work.
	 */
	protected abstract void readSimulationOutput(Stimuli sd, URL fileURL, Cell cell) throws IOException;

	public static FileType getSpiceOutputType(String format, Simulation.SpiceEngine engine)
	{
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
		if (format.equalsIgnoreCase("RawLT"))
		{
			return FileType.RAWLTSPICEOUT;
		}
        if (format.equalsIgnoreCase("Epic"))
        {
            return FileType.EPIC;
        }
		return null;
	}

	/**
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
		while (name.indexOf('.', dotPos+1) >= 0)
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
