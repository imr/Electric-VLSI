/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EpicOutProcess.java
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
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.ActivityLogger;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for reading and displaying waveforms from Epic output.
 * These are contained in .out files.
 * This class invokes external JVM to read the EpicFile.
 */
public class EpicOutProcess extends Simulate implements Runnable
{
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
        Stimuli sd = null;
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
            Job.getUserInterface().showErrorMessage("EpicReaderProcess exited with code " + exitCode + " (" + exitMsg + ")", "EpicReaderProcess");
        }
        
        // stop progress dialog
        stopProgressDialog();
       
        // free memory
        strings = null;
        stdOut.close();
        stdOut = null;
        readerProcess = null;
        
        // return the simulation data
        return sd;
    }
    
    private static String VERSION_STRING = ";! output_format 5.3";
    
	private Stimuli readEpicFile()
		throws IOException
	{
        char separator = '.';
        Stimuli sd = new Stimuli();
        sd.setSeparatorChar(separator);
        EpicAnalysis an = new EpicAnalysis(sd);
        int numSignals = 0;
        ContextBuilder contextBuilder = new ContextBuilder();
        ArrayList<ContextBuilder> contextStack = new ArrayList<ContextBuilder>();
        contextStack.add(contextBuilder);
        int contextStackDepth = 1;
        
        for (;;) {
            byte b = stdOut.readByte();
            if (b == 'F') break;
            switch (b) {
                case 'V':
                case 'I':
                    String name = readString();
                    contextBuilder.strings.add(name);
                    byte type = b == 'V' ? EpicAnalysis.VOLTAGE_TYPE: EpicAnalysis.CURRENT_TYPE;
                    contextBuilder.contexts.add(EpicAnalysis.getContext(type));
                    EpicAnalysis.EpicSignal s = new EpicAnalysis.EpicSignal(an, type, numSignals++);
                    s.setSignalName(name);
                    break;
                case 'D':
                    String down = readString();
                    contextBuilder.strings.add(down);
                    
                    if (contextStackDepth >= contextStack.size())
                        contextStack.add(new ContextBuilder());
                    contextBuilder = contextStack.get(contextStackDepth++);
                    break;
                case 'U':
                    EpicAnalysis.Context newContext = an.getContext(contextBuilder.strings, contextBuilder.contexts);
                    contextBuilder.clear();

                    contextStackDepth--;
                    contextBuilder = contextStack.get(contextStackDepth - 1);
                    
                    contextBuilder.contexts.add(newContext);
                    break;
                default:
                    assert false;
            }
            assert contextBuilder == contextStack.get(contextStackDepth - 1);
        }
        assert contextStackDepth == 1;
        an.setRootContext(an.getContext(contextBuilder.strings, contextBuilder.contexts));
       
        an.setTimeResolution(stdOut.readDouble());
        an.setVoltageResolution(stdOut.readDouble());
        an.setCurrentResolution(stdOut.readDouble());
        an.setMaxTime(stdOut.readDouble());
        an.initSignals();
        List<Signal> signals = an.getSignals();
        assert numSignals == signals.size();
        an.waveStarts = new int[numSignals + 1];
        int start = 0;
        for (int i = 0; i < numSignals; i++) {
            int minV = stdOut.readInt();
            int maxV = stdOut.readInt();
            int len = stdOut.readInt();

            EpicAnalysis.EpicSignal s = (EpicAnalysis.EpicSignal)signals.get(i);
            s.setBounds(minV, maxV);
            start += len;
            an.waveStarts[i + 1] = start;
        }
        an.setWaveFile(new File(stdOut.readUTF()));

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
                        setProgressValue(0);
                        setProgressNote(line.substring(1));
                    } else {
                        setProgressValue(TextUtils.atoi(line));
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
    
    /**
     * Class which is used to restore Contexts from flat list of Signals.
     */
    private static class ContextBuilder {
        ArrayList<String> strings = new ArrayList<String>();
        ArrayList<EpicAnalysis.Context> contexts = new ArrayList<EpicAnalysis.Context>();
        
        void clear() { strings.clear(); contexts.clear(); }
    }
}
