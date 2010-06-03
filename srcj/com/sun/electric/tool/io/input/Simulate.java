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
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.waveform.WaveformWindow;
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
public final class Simulate {

	private Simulate() {}

    private static final String[] known_extensions = new String[]
        { "raw", "dump", "spo", "out", "tr0", "ac0", "txt", "vcd" };

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
        File file = null;
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
        if (ww==null) ww = new WindowFrame().createWaveformWindow(new Stimuli()).getWaveformWindow();
        new ReadSimulationOutput(cell, url, ww).start();
    }

    private static Input<Stimuli> getInputForExtension(String extension) {
        if (extension.indexOf('.')!=-1)
            extension = extension.substring(extension.lastIndexOf('.')+1);
        if      (extension.equals("txt"))  return new SmartSpiceOut();
        else if (extension.equals("raw"))  return new RawSpiceOut();
        else if (extension.equals("dump")) return new VerilogOut();
        else if (extension.equals("spo"))  return new SpiceOut();
        else if (extension.equals("out"))  return new EpicOut.EpicOutProcess();
        else if (extension.equals("vcd"))  return new VerilogOut();
        else if (extension.startsWith("tr") || extension.startsWith("sw") || extension.startsWith("ic") ||
                 extension.startsWith("ac") || extension.startsWith("mt") || extension.startsWith("pa"))
            return new HSpiceOut();
        return null;
    }

	/**
	 * Class to read simulation output in a new thread.
	 */
	private static class ReadSimulationOutput extends Thread {
		private FileType type;
		private Input<Stimuli> is;
		private URL fileURL;
		private Cell cell;
		private WaveformWindow ww;
        private Stimuli sd;
        private final Environment launcherEnvironment;
        private final UserInterfaceExec userInterface;

		private ReadSimulationOutput(Cell cell, URL fileURL, WaveformWindow ww) {
			this.type = type;
			this.fileURL = fileURL;
			this.cell = cell;
			this.ww = ww;
            this.sd = null;
            this.is = getInputForExtension(fileURL.getPath());
            if (this.is==null) throw new RuntimeException("unable to detect type");
            // future feature: try to guess the file type from the first few lines
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
                sd = is.processInput(fileURL, cell);
				if (sd == null) return;
                sd.setFileURL(fileURL);
                final Stimuli sdx = sd;
                SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            Simulation.showSimulationData(sdx, ReadSimulationOutput.this.ww);
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
}
