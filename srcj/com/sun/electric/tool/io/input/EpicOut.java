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
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Class for reading and displaying waveforms from Epic output.
 * These are contained in .out and .pa0 files.
 */
public class EpicOut extends Simulate {
    
    private static class EpicStimuli extends Stimuli {
        EpicReader reader;
        double timeResolution;
        double voltageResolution;
        double currentResolution;
    }
    
    private static class EpicAnalogSignal extends AnalogSignal {
        int sigNum;
        double[] data;
        
        EpicAnalogSignal(EpicStimuli sd, int sigNum) {
            super(sd);
            this.sigNum = sigNum;
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
            if (data == null)
                makeData();
            result[0] = data[index*2];
            result[1] = result[2] = data[index*2 + 1];
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
            if (data == null)
                makeData();
            return data.length/2;
        }
        
        private void makeData() {
            EpicStimuli sd = (EpicStimuli)this.sd;
            EpicSignal s = (EpicSignal)sd.reader.signals.get(sigNum);
            double resolution = 1;
            switch (s.type) {
                case EpicSignal.VOLTAGE_TYPE:
                    resolution = sd.voltageResolution;
                    break;
                case EpicSignal.CURRENT_TYPE:
                    resolution = sd.currentResolution;
                    break;
            }
            int[] intData = s.getWaveform();
            int len = intData.length/2;
            data = new double[len * 2];
            for (int i = 0; i < len; i++) {
                data[i*2] = intData[i*2] * sd.timeResolution;
                data[i*2+1] = intData[i*2+1] * resolution;
            }
        }
        
        private void setBounds(Rectangle2D bounds) {
            this.bounds = bounds;
            this.boundsCurrent = true;
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
		Stimuli sd = readEpicFile(new MyEpicReader(fileURL));
        sd.setCell(cell);

		// stop progress dialog
		stopProgressDialog();

		// return the simulation data
		return sd;
	}

    private static String VERSION_STRING = ";! output_format 5.3";
    
	private Stimuli readEpicFile(MyEpicReader reader)
		throws IOException
	{
        char separator = '.';
        EpicStimuli sd = new EpicStimuli();
        sd.reader = reader;
        sd.setSeparatorChar(separator);
        sd.timeResolution = reader.timeResolution;
        sd.voltageResolution = reader.voltageResolution;
        sd.currentResolution = reader.currentResolution;
        ArrayList/*<EpicAnalogSignal>*/ signals = new ArrayList/*<EpicAnalogSignal>*/();
        for (int i = 0; i < reader.signals.size(); i++) {
            EpicSignal s = (EpicSignal)reader.signals.get(i);
            if (s == null) continue;
            String name = s.name;
            if (name == null) continue;
            EpicAnalogSignal as = new EpicAnalogSignal(sd, i);
            if (name.startsWith("v(") && name.endsWith(")"))
                name = name.substring(2, name.length() - 1);
            else if (name.startsWith("i(") && name.endsWith(")")) {
                name = name.substring(2, name.length() - 1);
            }
            else if (name.startsWith("i1(") && name.endsWith(")")) {
                name = name.substring(3, name.length() - 1);
            }
            int lastSlashPos = name.lastIndexOf(separator);
            if (lastSlashPos > 0) {
                as.setSignalContext(name.substring(0, lastSlashPos));
                name = name.substring(lastSlashPos + 1);
            }
            double resolution = 1;
            switch (s.type) {
                case EpicSignal.VOLTAGE_TYPE:
                    resolution = reader.voltageResolution;
                    break;
                case EpicSignal.CURRENT_TYPE:
                    resolution = reader.currentResolution;
                    name = "i(" + name + ")";
                    break;
            }
            as.setSignalName(name);
            Rectangle2D bounds = new Rectangle2D.Double(0, s.minV*resolution, reader.maxT*sd.timeResolution, (s.maxV - s.minV)*resolution);
            as.setBounds(bounds);
        }
        
        return sd;
    }
    
    class MyEpicReader extends EpicReader {
        MyEpicReader(URL fileURL) { super(fileURL); }

        void showProgress(double ratio) { progress.setProgress((int)(ratio*100)); }
    }
}

class EpicReader {
    String filePath;
    InputStream inputStream;
    long fileLength;
    long byteCount;
    byte[] buf = new byte[65536];
    int bufL;
    int bufP;
//    StringBuilder builder = new StringBuilder();
    StringBuffer builder = new StringBuffer();
    Pattern whiteSpace = Pattern.compile("[ \t]+");
    
    int timesC = 0;
    int eventsC = 0;
    ArrayList/*<EpicSignal>*/ signals = new ArrayList/*<EpicSignal>*/();
    int numSignals = 0;
    double timeResolution;
    double voltageResolution;
    double currentResolution;
    int curTime = 0;
    int maxT = 0;
    
    private static String VERSION_STRING = ";! output_format 5.3";
        
    EpicReader(URL fileURL) {
        try {
            readFile(fileURL);
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {}
        }
    }

    boolean readFile(URL fileURL) {
    	filePath = fileURL.getFile();
        long startTime = System.currentTimeMillis();
		URLConnection urlCon = null;
		try
		{
			urlCon = fileURL.openConnection();
//            urlCon.setConnectTimeout(10000);
//            urlCon.setReadTimeout(1000);
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
                if (line == null) break;
                parseNumLine(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        long stopTime = System.currentTimeMillis();
        long memLength = 0;
        for (int i = 0; i < signals.size(); i++) {
            EpicSignal s = (EpicSignal)signals.get(i);
            if (s == null) continue;
            memLength += s.len;
        }
        System.out.println((stopTime - startTime)/1000.0 + " sec " + byteCount + " bytes " + numSignals + " signals " +
                timesC + " timepoints " +  eventsC + " events " + memLength + " bytes in memory");

		return false;
    }
    
    private void parseNumLine(String line) {
        if (line.length() == 0) return;
        char ch = line.charAt(0);
        if (ch == '.') {
            String[] split = whiteSpace.split(line);
            if (split[0].equals(".index") && split.length == 4) {
                int type;
                if (split[3].equals("v"))
                    type = EpicSignal.VOLTAGE_TYPE;
                else if (split[3].equals("i"))
                    type = EpicSignal.CURRENT_TYPE;
                else {
                    System.out.println("Unknown waveform type: " + line);
                    return;
                }
                String name = split[1];
                int sigNum = TextUtils.atoi(split[2]);
                while (signals.size() <= sigNum)
                    signals.add(null);
                EpicSignal s = (EpicSignal)signals.get(sigNum);
                if (s == null) {
                    s = new EpicSignal();
                    signals.set(sigNum, s);
                    numSignals++;
                }
                s.name = name;
                s.type = type;
            } else if (split[0].equals(".vdd") && split.length == 2) {
            } else if (split[0].equals(".time_resolution") && split.length == 2) {
                timeResolution = TextUtils.atof(split[1]) * 1e-9;
            } else if (split[0].equals(".current_resolution") && split.length == 2) {
                currentResolution = TextUtils.atof(split[1]);
            } else if (split[0].equals(".voltage_resolution") && split.length == 2) {
                voltageResolution = TextUtils.atof(split[1]);
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
        if (bufP + 20 >= bufL) return false;
        int ch = buf[bufP++];
        if (ch < '0' || ch > '9') return false;
        int num1 = ch - '0';
        for (int lim = bufP + 9; bufP < lim; ) {
            ch = buf[bufP++];
            if (ch < '0' || ch > '9') break;
            num1 = num1*10 + (ch - '0');
        }
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
            for (int lim = bufP + 9; bufP < lim; ) {
                ch = buf[bufP++];
                if (ch < '0' || ch > '9') break;
                num2 = num2*10 + (ch - '0');
            }
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
        EpicSignal s = (EpicSignal)signals.get(sigNum);
        if (s == null) {
            s = new EpicSignal();
            signals.set(sigNum, s);
            numSignals++;
        }
        s.putEvent(curTime, value);
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
        if (bufL <= 0) return true;
        byteCount += bufL;
        showProgress(fileLength != 0 ? byteCount/(double)fileLength : 0);
        return false;
    }
    
    void showProgress(double ratio) {}
}

class EpicSignal {
    String name;
    int type;
    int lastT;
    int lastV;
    int minV = Integer.MAX_VALUE;
    int maxV = Integer.MIN_VALUE;
    byte[] waveform = new byte[512];
    int len;

    static final int VOLTAGE_TYPE = 1;
    static final int CURRENT_TYPE = 2;
    
    void putEvent(int t, int v) {
        putUnsigned(t - lastT);
        putSigned(v - lastV);
        lastT = t;
        lastV = v;
        minV = Math.min(minV, v);
        maxV = Math.max(maxV, v);
    }

    void putUnsigned(int value) {
        if (value < 0xC0) {
            ensureCapacity(1);
            putByte(value);
        } else if (value < 0x3F00) {
            ensureCapacity(2);
            putByte((value + 0xC000) >> 8);
            putByte(value);
        } else {
            ensureCapacity(5);
            putByte(0xFF);
            putByte(value >> 24);
            putByte(value >> 16);
            putByte(value >> 8);
            putByte(value);
        }
    }
    
    void putSigned(int value) {
        if (-0x60 <= value && value < 0x60) {
            ensureCapacity(1);
            putByte(value + 0x60);
        } else if (-0x1F00 <= value && value < 0x2000) {
            ensureCapacity(2);
            putByte((value + 0xDF00) >> 8);
            putByte(value);
        } else {
            ensureCapacity(5);
            putByte(0xFF);
            putByte(value >> 24);
            putByte(value >> 16);
            putByte(value >> 8);
            putByte(value);
        }
            
    }
    
    int[] getWaveform() {
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
    
    void putByte(int value) { waveform[len++] = (byte)value; }
    
    void ensureCapacity(int l) {
        if (len + l <= waveform.length) return;
        byte[] newWaveform = new byte[waveform.length*3/2];
        System.arraycopy(waveform, 0, newWaveform, 0, waveform.length);
        waveform = newWaveform;
        assert len + l <= waveform.length;
    }
}