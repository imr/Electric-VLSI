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
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.UserInterfaceExec;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.verilog.VerilogOut;
import com.sun.electric.tool.simulation.SimulationTool;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.swing.SwingUtilities;

/**
 * This class reads simulation output files and plots them.
 */
public final class SimulationData {

	private SimulationData() {}

    private static final String[] known_extensions = new String[]
        { 

          // Steve has specifically requested that tr0/ac0 files have top
          // priority (Bug #2815)
          "tr0", "ac0", 
          
          "vcd", "raw", "dump", "spo",

          // it's important that "out" and "txt" appear last because
          // they sometimes are present yet do not contain simulation
          // data (although in such situations the user should not
          // be using "plot from guessed" anyways)
          "out", "txt" };

    public static boolean isKnownSimulationFormatExtension(String extension) {
        return getInputForExtension(extension)!=null;
    }

	/**
	 * Based on the provided cell, make an educated guess
	 * about which simulation file the user has in mind, and plot
	 * that.  If WaveformWindow is null, one will be created.
	 */
	public static void plotGuessed(Cell cell, WaveformWindow ww) {
        if (cell==null) return;
        String[] paths = new String[] {
            FileType.SPICE.getGroupPath(),
            TextUtils.getFilePath(cell.getLibrary().getLibFile())
        };
        for (String path : paths)
            for (String ext : known_extensions)
                if (new File(path, cell.getName()+'.'+ext).exists()) {
                    plot(cell, TextUtils.makeURLToFile(new File(path, cell.getName()+'.'+ext).getPath()), ww);
                    return;
                }
        System.out.println("unable to guess any simulation file with a known extension; the following directories were checked: ");
        for(String path : paths)
            System.out.println("  " + path);
	}

	/**
	 * Plot the simulation data for cell found at url in ww.
     * If cell is null, no cell will be associated with the simulation data (crossprobing disabled).
     * If ww is null, a waveform window will be created.
	 */
	public static void plot(Cell cell, URL url, WaveformWindow ww) {
        new ReadSimulationOutput(cell, url, ww).start();
    }

    private static Input<Stimuli> getInputForExtension(String extension) {
        if (extension.indexOf('.') != -1)
            extension = extension.substring(extension.lastIndexOf('.')+1);
        if (extension.equals("dump") || extension.equals("vcd")) return new VerilogOut();
        if (extension.equals("txt")) return new PSpiceOut();
        if (extension.equals("raw")) return new RawSpiceOut();
        if (extension.equals("spo")) return new SpiceOut();
        if (extension.equals("out")) return new EpicOut();
        if (extension.startsWith("tr") || extension.startsWith("sw") || extension.startsWith("ic") ||
            extension.startsWith("ac") || extension.startsWith("mt") || extension.startsWith("pa"))
            	return new HSpiceOut();
        return null;
    }

	/**
	 * Class to read simulation output in a new thread.
	 */
	private static class ReadSimulationOutput extends Thread {
		private Input<Stimuli> is;
		private URL fileURL;
		private Cell cell;
		private WaveformWindow ww;
        private Stimuli sd;
        private final Environment launcherEnvironment;
        private final UserInterfaceExec userInterface;
        private String netDelimeter;

		private ReadSimulationOutput(Cell cell, URL fileURL, WaveformWindow ww) {
			this.fileURL = fileURL;
			this.cell = cell;
			this.ww = ww;
            this.is = getInputForExtension(fileURL.getPath());
            this.netDelimeter = SimulationTool.getSpiceExtractedNetDelimiter();
            if (this.is==null) throw new RuntimeException("unable to detect type");

            launcherEnvironment = Environment.getThreadEnvironment();
            userInterface = new UserInterfaceExec();
		}

		public void run() {
            if (is==null) return;
            if (Thread.currentThread() == this) {
                Environment.setThreadEnvironment(launcherEnvironment);
                Job.setUserInterface(userInterface);
            }
			try {
                sd = new Stimuli();
                sd.setNetDelimiter(netDelimeter);
                sd.setCell(cell);
                is.processInput(fileURL, cell, sd);
                if (sd == null) return;
                sd.setFileURL(fileURL);
                final Stimuli sdx = sd;
                assert cell.getDatabase() == EDatabase.clientDatabase();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (ww==null)
                            WaveformWindow.showSimulationDataInNewWindow(sdx);
                        else
                            WaveformWindow.refreshSimulationData(sdx, ReadSimulationOutput.this.ww);
                    }});
			} catch (IOException e) {
				System.out.println("End of file reached while reading " + fileURL);
			}
		}
	}

	public static Stimuli processInput(Cell cell, URL url) {
        ReadSimulationOutput job = new ReadSimulationOutput(cell, url, null);
        job.run();
        return job.sd;
    }

	public static Stimuli processInput(Cell cell, URL url, String netDelimeter) {
 		Input<Stimuli> is = getInputForExtension(url.getPath());
        if (is==null) throw new RuntimeException("unable to detect type");
        Stimuli sd = new Stimuli();
        sd.setNetDelimiter(netDelimeter);
        sd.setCell(cell);
        try {
            is.processInput(url, cell, sd);
        } catch (IOException e) {
			System.out.println("End of file reached while reading " + url);
            return null;
        }
        sd.setFileURL(url);
        return sd;
    }
}
