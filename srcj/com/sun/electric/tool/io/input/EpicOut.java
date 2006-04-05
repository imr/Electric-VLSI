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
import com.sun.electric.tool.simulation.Analysis;
import com.sun.electric.tool.simulation.Stimuli;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Class for reading and displaying waveforms from Epic output.
 * These are contained in .out files.
 */
public class EpicOut extends Simulate
{
    private static class EpicStimuli extends Stimuli
    {
        double timeResolution;
        double voltageResolution;
        double currentResolution;
	}

    private static class EpicProcessing
    {
		int lastT;
		int lastV;
		int minV = Integer.MAX_VALUE;
		int maxV = Integer.MIN_VALUE;
    }

    private static class EpicAnalogSignal extends AnalogSignal
	{
		static final byte VOLTAGE_TYPE = 1;
		static final byte CURRENT_TYPE = 2;

		byte type;
		byte[] waveform = null;
        int position = -1;

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
            int[] intData = getWaveform();
            int len = intData.length/2;
            buildTime(len);
            buildValues(len);
            for (int i = 0; i < len; i++)
            {
                double timeValue = intData[i*2] * sd.timeResolution;
                setTime(i, timeValue);
                double dataValue = intData[i*2+1] * resolution;
                setValue(i, dataValue);
            }
        }

        private void setBounds(Rectangle2D bounds)
        {
            this.bounds = bounds;
        }

        private void putEvent(EpicProcessing ep, int t, int v)
        {
            int val1 = t - ep.lastT;
            int val2 = v - ep.lastV;
            int totalSpace = calculateUnsignedSpace(val1) + calculateSignedSpace(val2);
            makeCapacitity(totalSpace);
            putUnsigned(val1);
            putSigned(val2);
            ep.lastT = t;
            ep.lastV = v;
            ep.minV = Math.min(ep.minV, v);
            ep.maxV = Math.max(ep.maxV, v);
        }

        private int calculateUnsignedSpace(int value)
        {
            if (value < 0xC0) return 1;
            else if (value < 0x3F00) return 2;
            return 5;
        }

        private void putUnsigned(int value) {
            if (value < 0xC0) {
                put1Byte(value);
            } else if (value < 0x3F00) {
                put2Bytes((value + 0xC000) >> 8, value);
            } else {
                put5Bytes(0xFF, value >> 24, value >> 16, value >> 8, value);
            }
        }

        private int calculateSignedSpace(int value)
        {
            if (-0x60 <= value && value < 0x60) return 1;
            else if (-0x1F00 <= value && value < 0x2000) return 2;
            return 5;
        }

        private void putSigned(int value) {
            if (-0x60 <= value && value < 0x60) {
                put1Byte(value + 0x60);
            } else if (-0x1F00 <= value && value < 0x2000) {
                put2Bytes((value + 0xDF00) >> 8, value);
            } else {
                put5Bytes(0xFF, value >> 24, value >> 16, value >> 8, value);
            }
        }
        
        private int[] getWaveform() {
            int count = 0;
            for (int i = 0; i < position; count++) {
//            for (int i = 0; i < waveform.length; count++) {
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
            int[] w = new int[count*2];
            count = 0;
            int t = 0;
            int v = 0;
            for (int i = 0; i < position; count++) {
//            for (int i = 0; i < waveform.length; count++) {
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
                w[count*2] = t = t + b;
                
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
                w[count*2 + 1] = v = v + b;
            }
            assert count*2 == w.length;
            return w;
        }

        private void put1Byte(int value)
        {
        	int fillPos = ensureCapacity(1);
        	waveform[fillPos] = (byte)value;
        }

        private void put2Bytes(int value1, int value2)
        {
        	int fillPos = ensureCapacity(2);
        	waveform[fillPos++] = (byte)value1;
        	waveform[fillPos] = (byte)value2;
        }

        private void put5Bytes(int value1, int value2, int value3, int value4, int value5)
        {
        	int fillPos = ensureCapacity(5);
        	waveform[fillPos++] = (byte)value1;
        	waveform[fillPos++] = (byte)value2;
        	waveform[fillPos++] = (byte)value3;
        	waveform[fillPos++] = (byte)value4;
        	waveform[fillPos] = (byte)value5;
        }

        private void makeCapacitity(int l)
        {
            int waveformLen = 0;
        	if (waveform != null) waveformLen = waveform.length;
            byte[] newWaveform = new byte[waveformLen + l];
            if (waveformLen > 0)
            	System.arraycopy(waveform, 0, newWaveform, 0, waveformLen);
            waveform = newWaveform;
        }

        private int ensureCapacity(int l)
        {
            assert (position + l < waveform.length);
            int pos = position;
            position += l;
            return pos + 1;
//        	int waveformLen = 0;
//        	if (waveform != null) waveformLen = waveform.length;
//            byte[] newWaveform = new byte[waveformLen + l];
//            if (waveformLen > 0)
//            	System.arraycopy(waveform, 0, newWaveform, 0, waveformLen);
//            waveform = newWaveform;
//            return waveformLen;
        }
    }
    
	EpicOut() {}

	/**
	 * Method to read an Spice output file.
	 */
	protected Stimuli readSimulationOutput(URL fileURL, Cell cell)
		throws IOException
	{
		// show progress reading .spo file
		startProgressDialog("EPIC output", fileURL.getFile());

		// read the actual signal data from the .spo file
		Stimuli sd = readEpicFile(new EpicReader(fileURL));
        sd.setCell(cell);

		// stop progress dialog
		stopProgressDialog();

		// return the simulation data
		return sd;
	}

    private static String VERSION_STRING = ";! output_format 5.3";
    
	private Stimuli readEpicFile(EpicReader reader)
		throws IOException
	{
        char separator = '.';
        EpicStimuli sd = reader.sd;
        Analysis an = reader.an;
        sd.setSeparatorChar(separator);
        for (int i = 0; i < reader.signals.size(); i++)
        {
        	EpicAnalogSignal s = reader.signals.get(i);
            if (s == null) continue;
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
            EpicProcessing ep = reader.processingData.get(s);
            Rectangle2D bounds = new Rectangle2D.Double(0, ep.minV*resolution, reader.maxT*sd.timeResolution, (ep.maxV - ep.minV)*resolution);
            s.setBounds(bounds);
        }
        
        return sd;
    }

	static class EpicReader
    {
    	EpicStimuli sd;
    	Analysis an;
        String filePath;
        InputStream inputStream;
        long fileLength;
        long byteCount;
        byte[] buf = new byte[65536];
        int bufL;
        int bufP;
        StringBuilder builder = new StringBuilder();
        Pattern whiteSpace = Pattern.compile("[ \t]+");
    	int timesC = 0;
        int eventsC = 0;
        ArrayList<EpicAnalogSignal> signals = new ArrayList<EpicAnalogSignal>();
        HashMap<EpicAnalogSignal,EpicProcessing> processingData = new HashMap<EpicAnalogSignal,EpicProcessing>();
        int numSignals = 0;
        int curTime = 0;
        int maxT = 0;
        HashMap<String,String> contextNames;
//        Progress progress;
        
        private static String VERSION_STRING = ";! output_format 5.3";
            
        EpicReader(URL fileURL)
        {
            sd = new EpicStimuli();
        	an = new Analysis(sd, Analysis.ANALYSIS_TRANS);

        	try {
                readFile(fileURL);
            } finally {
                try {
                    if (inputStream != null)
                        inputStream.close();
                } catch (IOException e) {}
            }
        }

//        boolean readFileTime(URL fileURL) {
//        	filePath = fileURL.getFile();
//            long startTime = System.currentTimeMillis();
//    		URLConnection urlCon = null;
//    		try
//    		{
//    			urlCon = fileURL.openConnection();
//    			urlCon.setConnectTimeout(10000);
//    			urlCon.setReadTimeout(1000);
//                String contentLength = urlCon.getHeaderField("content-length");
//                fileLength = -1;
//                try {
//                    fileLength = Long.parseLong(contentLength);
//                } catch (Exception e) {}
//    			inputStream = urlCon.getInputStream();
//    		} catch (IOException e)
//    		{
//    			System.out.println("Could not find file: " + filePath);
//    			return true;
//    		}
//    		byteCount = 0;
//            try {
//                String firstLine = getLine();
//                if (firstLine == null || !firstLine.equals(VERSION_STRING)) {
//                    System.out.println("Unknown Epic Version: " + firstLine);
//                }
//                for (;;) {
//                    if (bufP >= bufL && readBuf()) break;
//                    int startLine = bufP;
//                    if (parseNumLineFast()) continue;
//                    bufP = startLine;
//                    String line = getLine();
//                    assert bufP <= bufL;
//                    if (line == null) break;
//                    parseNumLine(line);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            long stopTime = System.currentTimeMillis();
//            System.out.println((stopTime - startTime)/1000.0 + " sec; file size is " + byteCount + " bytes");
//            System.out.println("Found " + numSignals + " signals (" + timesC + " timepoints and " +  eventsC + " events)");
//    		return false;
//        }

        boolean readFile(URL fileURL) {
        	filePath = fileURL.getFile();
            long startTime = System.currentTimeMillis();
    		URLConnection urlCon = null;
    		try
    		{
    			urlCon = fileURL.openConnection();
    			urlCon.setConnectTimeout(10000);
    			urlCon.setReadTimeout(1000);
                String contentLength = urlCon.getHeaderField("content-length");
                fileLength = -1;
                try {
                    fileLength = Long.parseLong(contentLength);
                } catch (Exception e) {}
    			inputStream = urlCon.getInputStream();
    		} catch (IOException e)
    		{
    			System.out.println("Could not find file: " + filePath);
    			return true;
    		}
    		contextNames = new HashMap<String,String>();
    		byteCount = 0;
            try {
                String firstLine = getLine();
                if (firstLine == null || !firstLine.equals(VERSION_STRING)) {
                    System.out.println("Unknown Epic Version: " + firstLine);
                }
                for (;;) {
                    if (bufP >= bufL && readBuf()) break;
                    int startLine = bufP;
                    if (parseNumLineFast()) continue;
                    bufP = startLine;
                    String line = getLine();
                    assert bufP <= bufL;
                    if (line == null) break;
                    parseNumLine(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            long stopTime = System.currentTimeMillis();
            System.out.println((stopTime - startTime)/1000.0 + " sec; file size is " + byteCount + " bytes");
            System.out.println("Found " + numSignals + " signals (" + timesC + " timepoints and " +  eventsC + " events)");
    		return false;
        }

        private void parseNumLine(String line)
        {
            if (line.length() == 0) return;
            char ch = line.charAt(0);
            if (ch == '.')
            {
                String[] split = whiteSpace.split(line);
                if (split[0].equals(".index") && split.length == 4)
                {
                    byte type;
                    if (split[3].equals("v"))
                        type = EpicAnalogSignal.VOLTAGE_TYPE;
                    else if (split[3].equals("i"))
                        type = EpicAnalogSignal.CURRENT_TYPE;
                    else {
                        System.out.println("Unknown waveform type: " + line);
                        return;
                    }
                    String name = split[1];
                    int sigNum = TextUtils.atoi(split[2]);
                    while (signals.size() <= sigNum)
                        signals.add(null);
                    EpicAnalogSignal s = signals.get(sigNum);
                    if (s == null)
                    {
                        s = new EpicAnalogSignal(an);
                        EpicProcessing ep = new EpicProcessing();
                        processingData.put(s, ep);
                        signals.set(sigNum, s);
                        numSignals++;
                    }

                    // name the signal
                    if (name.startsWith("v(") && name.endsWith(")"))
                    {
                        name = name.substring(2, name.length() - 1);
                    } else if (name.startsWith("i(") && name.endsWith(")"))
                    {
                        name = name.substring(2, name.length() - 1);
                    } else if (name.startsWith("i1(") && name.endsWith(")"))
                    {
                        name = name.substring(3, name.length() - 1);
                    }
                    name = removeLeadingX(name);
                    int lastSlashPos = name.lastIndexOf(sd.getSeparatorChar());
                    if (lastSlashPos > 0)
                    {
                    	String contextName = name.substring(0, lastSlashPos);
                    	String contextNameToSet = contextNames.get(contextName);
                    	if (contextNameToSet == null)
                    	{
                    		contextNames.put(contextName, contextName);
                    		contextNameToSet = contextName;
                    	}
                        s.setSignalContext(contextNameToSet);
                        name = name.substring(lastSlashPos + 1);
                    }
                    if (s.type == EpicAnalogSignal.CURRENT_TYPE) name = "i(" + name + ")";
                    s.setSignalName(name);
                    s.type = type;
                } else if (split[0].equals(".vdd") && split.length == 2) {
                } else if (split[0].equals(".time_resolution") && split.length == 2) {
                    sd.timeResolution = TextUtils.atof(split[1]) * 1e-9;
                } else if (split[0].equals(".current_resolution") && split.length == 2) {
                    sd.currentResolution = TextUtils.atof(split[1]);
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
            } else if (ch >= '0' || ch <= '9') {
                String[] split = whiteSpace.split(line);
                int num = TextUtils.atoi(split[0]);
                if (split.length  > 1) {
                    putValue(num, TextUtils.atoi(split[1]));
                } else {
                    putTime(num);
                }
            } else if (ch == ';' || Character.isSpaceChar(ch)) {
            } else {
                System.out.println("Unrecognized Epic line: " + line);
            }
        }
        
        private boolean parseNumLineFast() {
            final int MAX_DIGITS = 9;
            if (bufP + (MAX_DIGITS*2 + 4) >= bufL) return false;
            int ch = buf[bufP++];
            if (ch < '0' || ch > '9') return false;
            int num1 = ch - '0';
            ch = buf[bufP++];
            for (int lim = bufP + (MAX_DIGITS - 1); '0' <= ch && ch <= '9' && bufP < lim; ch = buf[bufP++])
                num1 = num1*10 + (ch - '0');
            boolean twoNumbers = false;
            int num2 = 0;
            if (ch == ' ') {
                ch = buf[bufP++];
                boolean sign = false;
                if (ch == '-') {
                    sign = true;
                    ch = buf[bufP++];
                }
                if (ch < '0' || ch > '9') return false;
                num2 = ch - '0';
                ch = buf[bufP++];
                for (int lim = bufP + (MAX_DIGITS - 1); '0' <= ch && ch <= '9' && bufP < lim; ch = buf[bufP++])
                    num2 = num2*10 + (ch - '0');
                if (sign) num2 = -num2;
                twoNumbers = true;
            }
            if (ch == '\n') {
            } else if (ch == '\r') {
                if (buf[bufP] == '\n')
                    bufP++;
            } else {
                return false;
            }
            if (twoNumbers)
                putValue(num1, num2);
            else
                putTime(num1);
            return true;
        }
        
        private void putTime(int time) {
            timesC++;
            maxT = curTime = Math.max(curTime, time);
        }
        
        void putValue(int sigNum, int value) {
            eventsC++;
            
            while (signals.size() <= sigNum)
                signals.add(null);
            EpicAnalogSignal s = signals.get(sigNum);
            if (s == null) {
                s = new EpicAnalogSignal(an);
                EpicProcessing ep = new EpicProcessing();
                processingData.put(s, ep);
                s.setSignalName("Signal " + sigNum);
                signals.set(sigNum, s);
                numSignals++;
            }
        	EpicProcessing ep = processingData.get(s);
            s.putEvent(ep, curTime, value);
        }
        
        private String getLine() throws IOException {
            builder.setLength(0);
            for (;;) {
                while (bufP < bufL) {
                    int ch = buf[bufP++] & 0xff;
                    if (ch == '\n') {
                        return builder.toString();
                    }
                    if (ch == '\r') {
                        if (bufP == bufL) readBuf();
                        if (bufP < bufL && buf[bufP] == '\n')
                            bufP++;
                        return builder.toString();
                    }
                    builder.append((char)ch);
                }
                if (readBuf()) {
                    return builder.length() != 0 ? builder.toString() : null; 
                }
            }
        }
        
        private boolean readBuf() throws IOException {
            assert bufP == bufL;
            bufP = bufL = 0;
            bufL = inputStream.read(buf, 0, buf.length);
            if (bufL <= 0) {
                bufL = 0;
                return true;
            }
            byteCount += bufL;
            setProgressValue(fileLength != 0 ? (int)(byteCount/(double)fileLength * 100) : 0);
            return false;
        }
    }
}
