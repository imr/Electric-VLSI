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
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.simulation.AnalogSignal;
import com.sun.electric.tool.simulation.Analysis;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.User;

import java.awt.geom.Rectangle2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class for reading and displaying waveforms from Epic output.
 * These are contained in .out files.
 */
public class EpicOutProcess extends Simulate
{
    private static class EpicStimuli extends Stimuli
    {
        Process readerProcess;
        DataOutputStream readerStdIn;
        DataInputStream readerStdOut;
        double timeResolution;
        double voltageResolution;
        double currentResolution;

        /**
         * Free allocated resources before closing.
        */
        public void finished() {
            try {
                readerStdIn.writeInt(-1);
                readerStdOut.close();
                readerStdIn.close();
            } catch (IOException e) {
            }
        }
    
    }

    private static class EpicAnalogSignal extends AnalogSignal
	{
		static final byte VOLTAGE_TYPE = 1;
		static final byte CURRENT_TYPE = 2;

		byte type;
        int index;

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
                try {
                    makeData();
                } catch (IOException e) {
                    result[0] = result[1] = 0;
                }
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
                try {
                    makeData();
                } catch (IOException e) {
                    return 0;
                }
            return super.getNumEvents(0);
        }
        
        private void makeData() throws IOException
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
            try {
                sd.readerStdIn.writeInt(index);
                sd.readerStdIn.flush();
                int dataLength = sd.readerStdOut.readInt();
                int len = dataLength/2;
                buildTime(len);
                buildValues(len);
                for (int i = 0; i < len; i++) {
                    int timeInt = sd.readerStdOut.readInt();
                    int valueInt = sd.readerStdOut.readInt();
                    double timeValue = timeInt * sd.timeResolution;
                    setTime(i, timeValue);
                    double dataValue = valueInt * resolution;
                    setValue(i, dataValue);
                }
            } catch (Exception e) {
                ActivityLogger.logException(e);
            }
        }
        
        private void setBounds(Rectangle2D bounds)
        {
            this.bounds = bounds;
        } 
    }
    
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
        try {
            Process process = invokeEpicReader(fileURL);
            DataInputStream stdOut = new DataInputStream(process.getInputStream());
            EpicStimuli sd = null;
            ArrayList<String> strings = new ArrayList<String>();
            loop:
                for (;;) {
                    byte b = stdOut.readByte();
                    switch (b) {
                        case 'M':
                            String message = stdOut.readUTF();
                            System.out.println(message);
                            break;
                        case 'P':
                            byte percent = stdOut.readByte();
                            progress.setProgress(percent);
                            break;
                        case 'C':
                            String s = stdOut.readUTF();
                            strings.add(s);
                            break;
                        case 'S':
                            long startTime = System.currentTimeMillis();
                            sd = readEpicFile(stdOut, strings);
                            long stopTime = System.currentTimeMillis();
                            System.out.println((stopTime - startTime)/1000.0 + " sec to transmit data");
                            sd.readerProcess = process;
                            sd.readerStdOut = stdOut;
                            sd.readerStdIn = new DataOutputStream(process.getOutputStream());
                            break loop;
                        default:
                            for (;;) {
                                int c = stdOut.readByte();
                                System.out.print(c);
                            }
                    }
                }
                sd.setCell(cell);
                
                
                // stop progress dialog
                stopProgressDialog();
                
                // return the simulation data
                return sd;
        } catch (Exception e) {
            ActivityLogger.logException(e);
            return null;
        }
    }
    
    private static String VERSION_STRING = ";! output_format 5.3";
    
	private EpicStimuli readEpicFile(DataInputStream stdOut, ArrayList<String> strings)
		throws IOException
	{
        char separator = '.';
        EpicStimuli sd = new EpicStimuli();
        Analysis an = new Analysis(sd, Analysis.ANALYSIS_TRANS);
        sd.setSeparatorChar(separator);
        sd.timeResolution = stdOut.readDouble();
        sd.voltageResolution = stdOut.readDouble();
        sd.currentResolution = stdOut.readDouble();
        int maxT = stdOut.readInt();
        int numSignals = stdOut.readInt();
        HashMap<String,String> contextNames = new HashMap<String,String>();
        for (int i = 0; i < numSignals; i++) {
            int contextIndex = stdOut.readInt();
            int nameIndex = stdOut.readInt();
            byte type = stdOut.readByte();
            int minV = stdOut.readInt();
            int maxV = stdOut.readInt();

            EpicAnalogSignal s = new EpicAnalogSignal(an);
            s.index = i;
            
            // name the signal
            String context = strings.get(contextIndex);
            String name = strings.get(nameIndex);
            if (context.length() > 0)
                s.setSignalContext(context);
            s.setSignalName(name);
                    
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
        
        return sd;
    }
    
	private Process invokeEpicReader(URL fileURL)
	{
		// see if the required amount of memory is already present
		Runtime runtime = Runtime.getRuntime();
        String program = "java";
        int maxMemWanted = User.getMemorySize();

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
            System.out.println("EpicReader Process launched");
		} catch (java.io.IOException e)
		{
            System.out.println("EpicReader Process failed");
		}
        return process;
	}
}
