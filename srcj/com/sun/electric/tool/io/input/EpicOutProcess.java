/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EpicOutProcess.java
 * Input/output tool: reader for EPIC output (.out)
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

import com.sun.electric.Launcher;
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.simulation.AnalogSignal;
import com.sun.electric.tool.simulation.Analysis;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.ActivityLogger;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Class for reading and displaying waveforms from Epic output.
 * These are contained in .out files.
 */
public class EpicOutProcess extends Simulate implements Runnable
{
    private static class EpicStimuli extends Stimuli
    {
        private File waveFileName;
        private RandomAccessFile waveFile;
        private double timeResolution;
        private double voltageResolution;
        private double currentResolution;

        /**
         * Free allocated resources before closing.
        */
        public void finished() {
            try {
                waveFile.close();
                waveFileName.delete();
            } catch (IOException e) {
            }
        }
    
    }

    private static class EpicAnalogSignal extends AnalogSignal
	{
		static final byte VOLTAGE_TYPE = 1;
		static final byte CURRENT_TYPE = 2;

		byte type;
        int start;
        int len;

		EpicAnalogSignal(Analysis an) { super(an); }
		
		/**
		 * Method to return the value of this signal at a given event index.
		 * @param sweep sweep index
		 * @param index the event index (0-based).
		 * @param result double array of length 3 to return (time, lowValue, highValue)
		 * If this signal is not a basic signal, return 0 and print an error message.
		 */
		public void getEvent(int sweep, int index, double[] result)
		{
			if (sweep != 0)
				throw new IndexOutOfBoundsException();
			if (getTimeVector() == null)
                makeData();
			super.getEvent(sweep, index, result);
		}
		
		/**
		 * Method to return the number of events in one sweep of this signal.
		 * This is the number of events along the horizontal axis, usually "time".
		 * The method only works for sweep signals.
		 * @param sweep the sweep number to query.
		 * @return the number of events in this signal.
		 */
		public int getNumEvents(int sweep) {
		    if (sweep != 0)
				throw new IndexOutOfBoundsException();
            if (getTimeVector() == null)
                makeData();
            return super.getNumEvents(0);
        }
        
        private void makeData()
        {
            EpicStimuli sd = (EpicStimuli)this.an.getStimuli();
            double resolution = 1;
            switch (type)
            {
                case VOLTAGE_TYPE:
                    resolution = sd.voltageResolution;
                    break;
                case CURRENT_TYPE:
                    resolution = sd.currentResolution;
                    break;
            }

            byte[] waveform = new byte[len];
            try {
                sd.waveFile.seek(start);
                sd.waveFile.readFully(waveform);
            } catch (IOException e) {
                buildTime(0);
                buildValues(0);
                ActivityLogger.logException(e);
            }
            
            int count = 0;
            for (int i = 0; i < len; count++) {
                int l;
                int b = waveform[i++] & 0xff;
                if (b < 0xC0)
                    l = 0;
                else if (b < 0xFF)
                    l = 1;
                else
                    l = 4;
                i += l;
                b = waveform[i++] & 0xff;
                if (b < 0xC0)
                    l = 0;
                else if (b < 0xFF)
                    l = 1;
                else
                    l = 4;
                i += l;
            }
            
            buildTime(count);
            buildValues(count);
            int[] w = new int[count*2];
            count = 0;
            int t = 0;
            int v = 0;
            for (int i = 0; i < len; count++) {
                int l;
                int b = waveform[i++] & 0xff;
                if (b < 0xC0) {
                    l = 0;
                } else if (b < 0xFF) {
                    l = 1;
                    b -= 0xC0;
                } else {
                    l = 4;
                }
                while (l > 0) {
                    b = (b << 8) | waveform[i++] & 0xff;
                    l--;
                }
                t = t + b;
                setTime(count, t * sd.timeResolution);
                
                b = waveform[i++] & 0xff;
                if (b < 0xC0) {
                    l = 0;
                    b -= 0x60;
                } else if (b < 0xFF) {
                    l = 1;
                    b -= 0xDF;
                } else {
                    l = 4;
                }
                while (l > 0) {
                    b = (b << 8) | waveform[i++] & 0xff;
                    l--;
                }
                v = v + b;
                setValue(count, v * resolution);
            }
            assert count*2 == w.length;
        }

        private void setBounds(Rectangle2D bounds)
        {
            this.bounds = bounds;
        } 
    }
    
    private Process readerProcess;
    private DataInputStream stdOut;
    private ArrayList<String> strings = new ArrayList<String>();

    EpicOutProcess() {}

	/**
	 * Method to read an Spice output file.
	 */
	protected Stimuli readSimulationOutput(URL fileURL, Cell cell)
		throws IOException
	{
		// show progress reading .spo file
		startProgressDialog("EPIC output", fileURL.getFile());

		// read the actual signal data from the .spo file
        EpicStimuli sd = null;
        boolean eof = false;
        try {
            readerProcess = invokeEpicReader(fileURL);
            stdOut = new DataInputStream(readerProcess.getInputStream());
            (new Thread(this, "EpicReaderErrors")).start();
            sd = readEpicFile();
            sd.setCell(cell);
                
        } catch (EOFException e) {
            eof = true;
        }

        int exitCode = 0;
        if (readerProcess != null) {
            try {
                exitCode = readerProcess.waitFor();
            } catch (InterruptedException e) {}
        }
        
        if (eof || exitCode != 0) {
            String exitMsg = null;
            switch (exitCode) {
                case 0:
                    exitMsg = "Ok";
                    break;
                case 1:
                    exitMsg = "File not found";
                    break;
                case 2:
                    exitMsg = "Out of memory";
                    break;
                default:
                    exitMsg = "Error"; 
            }
            Main.getUserInterface().showErrorMessage("EpicReaderProcess exited with code " + exitCode + " (" + exitMsg + ")", "EpicReaderProcess");
        }

        // stop progress dialog
        stopProgressDialog();
            
        // return the simulation data
        return sd;
    }
    
    private static String VERSION_STRING = ";! output_format 5.3";
    
	private EpicStimuli readEpicFile()
		throws IOException
	{
        char separator = '.';
        EpicStimuli sd = new EpicStimuli();
        Analysis an = new Analysis(sd, Analysis.ANALYSIS_TRANS);
        sd.setSeparatorChar(separator);
        StringBuilder currentContextBuilder = new StringBuilder();
        String currentContext = null;
        int[] sepPos = new int[1];
        int numSeps = 0;
        
        for (;;) {
            byte type = stdOut.readByte();
            if (type == 0) break;
            switch (type) {
                case EpicAnalogSignal.VOLTAGE_TYPE:
                case EpicAnalogSignal.CURRENT_TYPE:
                    String name = readString();
                    EpicAnalogSignal s = new EpicAnalogSignal(an);
                    s.type = type;
                    s.setSignalName(name);
                    if (currentContext != null)
                        s.setSignalContext(currentContext);
                    break;
                case 'D':
                    String down = readString();
                    if (numSeps >= sepPos.length) {
                        int[] newSepPos = new int[sepPos.length*2];
                        System.arraycopy(sepPos, 0, newSepPos, 0, sepPos.length);
                        sepPos = newSepPos;
                    }
                    sepPos[numSeps] = currentContextBuilder.length();
                    if (numSeps != 0)
                        currentContextBuilder.append(separator);
                    currentContextBuilder.append(down);
                    numSeps++;
                    currentContext = currentContextBuilder.toString();
                    break;
                case 'U':
                    assert numSeps > 0 && currentContext != null;
                    int pos = sepPos[--numSeps];
                    currentContextBuilder.setLength(pos);
                    currentContext = numSeps > 0 ? currentContext.substring(0, pos) : null;
                    break;
                default:
                    assert false;
            }
        }
       
        sd.timeResolution = stdOut.readDouble();
        sd.voltageResolution = stdOut.readDouble();
        sd.currentResolution = stdOut.readDouble();
        int maxT = stdOut.readInt();
        List<Signal> signals = an.getSignals();
        int numSignals = stdOut.readInt();
        assert numSignals == signals.size();
        for (int i = 0; i < numSignals; i++) {
            int minV = stdOut.readInt();
            int maxV = stdOut.readInt();
            int start = stdOut.readInt();
            int len = stdOut.readInt();

            EpicAnalogSignal s = (EpicAnalogSignal)signals.get(i);
            s.start = start;
            s.len = len;
            
            double resolution = 1;
            switch (s.type)
            {
                case EpicAnalogSignal.VOLTAGE_TYPE:
                    resolution = sd.voltageResolution;
                    break;
                case EpicAnalogSignal.CURRENT_TYPE:
                    resolution = sd.currentResolution;
                    break;
            }
            Rectangle2D bounds = new Rectangle2D.Double(0, minV*resolution, maxT*sd.timeResolution, (maxV - minV)*resolution);
            s.setBounds(bounds);
        }
        sd.waveFileName = new File(stdOut.readUTF());
        sd.waveFileName.deleteOnExit();
        sd.waveFile = new RandomAccessFile(sd.waveFileName, "r");

        return sd;
    }
    
    private String readString() throws IOException {
        int stringIndex = stdOut.readInt();
        if (stringIndex == -1) return null;
        if (stringIndex == strings.size()) {
            String s = stdOut.readUTF();
            strings.add(s);
        }
        return strings.get(stringIndex);
    }
    
	private Process invokeEpicReader(URL fileURL)
	{
		// see if the required amount of memory is already present
		Runtime runtime = Runtime.getRuntime();
        String program = "java";
        int maxMemWanted = Simulation.getSpiceEpicMemorySize();

        // get location of jar file
        String jarfile = "electric.jar";
        URL electric = Launcher.class.getResource("Main.class");
        if (electric.getProtocol().equals("jar")) {
            String file = electric.getFile();
            file = file.replaceAll("file:", "");
            file = file.replaceAll("!.*", "");
            jarfile = file;
        }

		String command = program;
		command += " -cp " + System.getProperty("java.class.path",".");
        command += " -ss2m";
		command += " -ea"; // enable assertions
		command += " -mx" + maxMemWanted + "m com.sun.electric.tool.io.input.EpicReaderProcess";
        command += " " + fileURL;
        Process process = null;
		try
		{
			process = runtime.exec(command);
            System.out.println("EpicReaderProcess launched with memory limit " + maxMemWanted + "m");
		} catch (java.io.IOException e)
		{
            System.out.println("EpicReaderProcess failed to launch");
		}
        return process;
	}
    
    /**
     * This methods implements Runnable interface for thread which polls stdErr of EpicReaderProcess
     * and redirects it to System.out and progress indicator.
     */
    public void run() {
        final String progressKey = "**PROGRESS ";
        BufferedReader stdErr = new BufferedReader(new InputStreamReader(readerProcess.getErrorStream()));
        try {
            // read from stream
            String line = null;
            while ((line = stdErr.readLine()) != null) {
                if (line.startsWith(progressKey)) {
                    line = line.substring(progressKey.length());
                    if (line.startsWith("!"))  {
                        progress.setNote(line.substring(1));
                        progress.setProgress(0);
                    } else {
                        progress.setProgress(TextUtils.atoi(line));
                    }
                    continue;
                }
                System.out.println("EpicReader: " + line);
            }
            stdErr.close();
        } catch (java.io.IOException e) {
            ActivityLogger.logException(e);
        }
    }
}
