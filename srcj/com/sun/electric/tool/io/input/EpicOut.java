/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EpicOut.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.simulation.AnalogSignal;
import com.sun.electric.tool.simulation.Stimuli;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Class for reading and displaying waveforms from Epic output.
 * These are contained in .out and .pa0 files.
 */
public class EpicOut extends Simulate {
    
    private static class EpicStimuli extends Stimuli {
        double timeResolution;
        double voltageResolution;
    }
    
    private static class EpicAnalogSignal extends AnalogSignal {
        int[] data;
        
        EpicAnalogSignal(EpicStimuli sd) {
            super(sd);
        }
        /**
         * Method to return the value of this signal at a given event index.
         * @param sweep sweep index
         * @param index the event index (0-based).
         * @param result double array of length 3 to return (time, lowValue, highValue)
         * If this signal is not a basic signal, return 0 and print an error message.
         */
        public void getEvent(int sweep, int index, double[] result) {
            if (sweep != 0)
                throw new IndexOutOfBoundsException();
            EpicStimuli sd = (EpicStimuli)this.sd;
            result[0] = data[index*2] * sd.timeResolution;
            result[1] = result[2] = data[index*2 + 1] * sd.voltageResolution;
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
            return data.length/2;
        }
    }
    
	EpicOut() {}

	/**
	 * Method to read an Spice output file.
	 */
	protected Stimuli readSimulationOutput(URL fileURL, Cell cell)
		throws IOException
	{
		// open the file
		if (openTextInput(fileURL)) return null;

		// show progress reading .spo file
		startProgressDialog("EPIC output", fileURL.getFile());

		// read the actual signal data from the .spo file
		Stimuli sd = readEpicFile(cell);

		// stop progress dialog, close the file
		stopProgressDialog();
		closeInput();

		// return the simulation data
		return sd;
	}

    private static String VERSION_STRING = ";! output_format 5.3";
    
	private Stimuli readEpicFile(Cell cell)
		throws IOException
	{
        String firstLine = getLine();
        if (firstLine == null || !firstLine.equals(VERSION_STRING)) {
            System.out.println("Unknown Epic Version: " + firstLine);
        }

        EpicStimuli sd = new EpicStimuli();
        sd.setSeparatorChar('/');
        ArrayList/*<EpicAnalogSignal>*/ signals = new ArrayList/*<EpicAnalogSignal>*/();
        Pattern whiteSpace = Pattern.compile("[ \t]+");
        int[] timeVal = new int[1024];
        int[] timePtr = new int[1024];
        int timesC = 0;
        int[] eventSig = new int[1024];
        int[] eventVal = new int[eventSig.length];
        int eventsC = 0;
        for (;;) {
            String line = getLine();
            if (line == null) break;
            if (line.length() == 0) continue;
            char ch = line.charAt(0);
            if ('0' <= ch && ch <= '9') {
                String[] split = whiteSpace.split(line);
                int num = TextUtils.atoi(split[0]);
                if (split.length  > 1) {
                    if (eventsC >= eventSig.length) {
                        int[] newEventSig = new int[eventSig.length*2];
                        System.arraycopy(eventSig, 0, newEventSig, 0, eventSig.length);
                        eventSig = newEventSig;
                        int[] newEventVal = new int[eventVal.length*2];
                        System.arraycopy(eventVal, 0, newEventVal, 0, eventVal.length);
                        eventVal = newEventVal;
                    }
                    eventSig[eventsC] = num;
                    eventVal[eventsC] = TextUtils.atoi(split[1]);
                    eventsC++;
                } else {
                    if (timesC*2 >= timeVal.length) {
                        int[] newTimeVal = new int[timeVal.length*2];
                        System.arraycopy(timeVal, 0, newTimeVal, 0, timeVal.length);
                        timeVal = newTimeVal;
                        int[] newTimePtr = new int[timePtr.length*2];
                        System.arraycopy(timePtr, 0, newTimePtr, 0, timePtr.length);
                        timePtr = newTimePtr;
                    }
                    timeVal[timesC] = num;
                    timePtr[timesC] = eventsC;
                    timesC++;
                }
                continue;
            }
            if (ch == '.') {
                String[] split = whiteSpace.split(line);
                if (split[0].equals(".index") && split.length == 4) {
                    if (!split[3].equals("v")) {
                        System.out.println("Can handle only voltage variables: " + line);
                        continue;
                    }
                    String name = split[1];
                    int index = TextUtils.atoi(split[2]);
                    EpicAnalogSignal as = new EpicAnalogSignal(sd);
                    if (name.startsWith("v(") && name.endsWith(")"))
                        name = name.substring(2, name.length() - 1);
                    int lastSlashPos = name.lastIndexOf('/');
                    if (lastSlashPos > 0) {
                        as.setSignalContext(name.substring(0, lastSlashPos));
                        name = name.substring(lastSlashPos + 1);
                    }
                    as.setSignalName(name);
                    while (signals.size() <= index) signals.add(null);
                    signals.set(index, as);
                } else if (split[0].equals(".vdd") && split.length == 2) {
                } else if (split[0].equals(".time_resolution") && split.length == 2) {
                    sd.timeResolution = TextUtils.atof(split[1]) * 1e-9;
                } else if (split[0].equals(".current_resolution") && split.length == 2) {
                } else if (split[0].equals(".voltage_resolution") && split.length == 2) {
                    sd.voltageResolution = TextUtils.atof(split[1]);
                } else if (split[0].equals(".high_threshold") && split.length == 2) {
                } else if (split[0].equals(".low_threshold") && split.length == 2) {
                } else if (split[0].equals(".nnodes") && split.length == 2) {
                } else if (split[0].equals(".nelems") && split.length == 2) {
                } else if (split[0].equals(".extra_nodes") && split.length == 2) {
                } else if (split[0].equals(".bus_notation") && split.length == 4) {
                } else if (split[0].equals(".hier_separator") && split.length == 2) {
                } else if (split[0].equals(".case") && split.length == 2) {
                } else {
                    System.out.println("Unrecognized Epic line: " + line);
                }
                continue;
            }
            if (ch == ';' || Character.isSpaceChar(ch)) {
                continue;
            }
            System.out.println("Unrecognized Epic line: " + line);
        }
        
        // transpose
        int[] eventCounts = new int[signals.size()];
        for (int i = 0; i < eventsC; i++) {
            int sig = eventSig[i];
            if (0 <= sig && sig < eventCounts.length)
                eventCounts[sig]++;
        }
        for (int i = 0; i < signals.size(); i++) {
            EpicAnalogSignal as = (EpicAnalogSignal)signals.get(i);
            if (as != null)
                as.data = new int[eventCounts[i]*2];
            eventCounts[i] = 0;
        }
        int ev = 0;
        for (int i = 0; i < timesC; i++) {
            int t = timeVal[i];
            int nextEv = i < timesC - 1 ? timePtr[i+1] : eventsC;
            for (;ev < nextEv; ev++) {
                int sig = eventSig[ev];
                if (sig < 0 || sig >= signals.size()) continue;
                EpicAnalogSignal as = (EpicAnalogSignal)signals.get(sig);
                if (as == null) continue;
                as.data[eventCounts[sig]*2] = t;
                as.data[eventCounts[sig]*2 + 1] = eventVal[ev];
                eventCounts[sig]++;
            }
        }
        for (int i = 0; i < signals.size(); i++) {
            EpicAnalogSignal as = (EpicAnalogSignal)signals.get(i);
            assert eventCounts[i]*2 == (as != null ? as.data.length : 0);  
        }
        return sd;
    }
}
