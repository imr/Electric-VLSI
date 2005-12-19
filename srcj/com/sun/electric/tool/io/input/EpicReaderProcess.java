/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EpicReaderProcess.java
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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 *
 */
class EpicReaderProcess {
    private InputStream inputStream;
    private long fileLength;
    private long byteCount;
    private byte[] buf = new byte[65536];
    private int bufL;
    private int bufP;
    private int lineNum;
    private StringBuilder builder = new StringBuilder();
    private Pattern whiteSpace = Pattern.compile("[ \t]+");
    private byte lastProgress;
     
    private int timesC = 0;
    private int eventsC = 0;
    private HashMap<String,Integer> stringIds = new HashMap<String,Integer>();
    private ArrayList<EpicReaderSignal> signals = new ArrayList<EpicReaderSignal>();
    private ArrayList<EpicReaderSignal> signalsByEpicIndex = new ArrayList<EpicReaderSignal>();
    private double timeResolution;
    private double voltageResolution;
    private double currentResolution;
    private int curTime = 0;
    private int maxT = 0;
    private String[] currentContext = new String[1];
    private int currentContextSeps = 0;
   
    private DataOutputStream stdOut = new DataOutputStream(System.out);

    private static final String VERSION_STRING = ";! output_format 5.3";
    private static final char separator = '.';
    
    private EpicReaderProcess() {}

    public static void main(String args[]) {
        try {
            EpicReaderProcess process = new EpicReaderProcess();
            try {
                process.readEpic(args[0]);
            } catch (IOException e) {
                System.err.println("Failed to read " + args[0]);
                e.printStackTrace(System.err);
                System.exit(1);
            } 

            process.writeOut();
        } catch (OutOfMemoryError e) {
            System.err.println("Out of memory. Increase memory limit in preferences.");
            e.printStackTrace(System.err);
            System.exit(2);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            System.exit(3);
        }
    }
    
    private boolean readEpic(String urlName) throws IOException {
        URL fileURL = new URL(urlName);
//    	String filePath = fileURL.getFile();
        long startTime = System.currentTimeMillis();
		URLConnection urlCon = fileURL.openConnection();
        urlCon.setConnectTimeout(10000);
        urlCon.setReadTimeout(1000);
        String contentLength = urlCon.getHeaderField("content-length");
        fileLength = -1;
        try {
            fileLength = Long.parseLong(contentLength);
        } catch (Exception e) {}
        inputStream = urlCon.getInputStream();
		byteCount = 0;
        String firstLine = getLine();
        if (firstLine == null || !firstLine.equals(VERSION_STRING)) {
            message("Unknown Epic Version: " + firstLine);
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
        inputStream.close();
        
        long stopTime = System.currentTimeMillis();
        System.err.println((stopTime - startTime)/1000.0 + " sec to read " + byteCount + " bytes " + signals.size() + " signals " + stringIds.size() + " strings " + 
                timesC + " timepoints " +  eventsC + " events from " + urlName);
                
		return false;
    }

    private void writeOut() throws IOException {
        File tempFile = null;
        BufferedOutputStream waveStream = null;
        boolean ok = false;
        try {
            long startTime = System.currentTimeMillis();
            tempFile = File.createTempFile("elec", ".epic");
            waveStream = new BufferedOutputStream(new FileOutputStream(tempFile));
        
            showProgressNote("Writing " + tempFile);
            stdOut.writeByte(0);
            
            stdOut.writeDouble(timeResolution);
            stdOut.writeDouble(voltageResolution);
            stdOut.writeDouble(currentResolution);
            stdOut.writeInt(maxT);
            stdOut.writeInt(signals.size());
            int size = 0;
            for (EpicReaderSignal s: signals)
                size += s.len;
            int start = 0;
            for (EpicReaderSignal s: signals) {
                waveStream.write(s.waveform, 0, s.len);
                
                stdOut.writeInt(s.minV);
                stdOut.writeInt(s.maxV);
                stdOut.writeInt(start);
                stdOut.writeInt(s.len);
                start += s.len;
                showProgress(size != 0 ? start/(double)size : 0);
            }
            
            waveStream.close();
        
            stdOut.writeUTF(tempFile.toString());
            stdOut.close();
            ok = true;
            long stopTime = System.currentTimeMillis();
            System.err.println((stopTime - startTime)/1000.0 + " sec to write " + tempFile.length() + " bytes to " + tempFile);
        } finally {
            if (ok)
                return;
            if (waveStream != null)
                waveStream.close();
            if (tempFile != null)
                tempFile.delete();
        }
   }
    
    private void parseNumLine(String line) throws IOException {
        if (line.length() == 0) return;
        char ch = line.charAt(0);
        if (ch == '.') {
            String[] split = whiteSpace.split(line);
            if (split[0].equals(".index") && split.length == 4) {
                int type;
                if (split[3].equals("v"))
                    type = EpicReaderSignal.VOLTAGE_TYPE;
                else if (split[3].equals("i"))
                    type = EpicReaderSignal.CURRENT_TYPE;
                else {
                    message("Unknown waveform type: " + line);
                    return;
                }
                String name = split[1];
                int sigNum = atoi(split[2]);
                while (signalsByEpicIndex.size() <= sigNum)
                    signalsByEpicIndex.add(null);
                EpicReaderSignal s = (EpicReaderSignal)signalsByEpicIndex.get(sigNum);
                if (s == null) {
                    s = new EpicReaderSignal();
                    signalsByEpicIndex.set(sigNum, s);
                    signals.add(s);
                }
                
                // name the signal
                if (name.startsWith("v(") && name.endsWith(")")) {
                    name = name.substring(2, name.length() - 1);
                } else if (name.startsWith("i(") && name.endsWith(")")) {
                    name = name.substring(2, name.length() - 1);
                } else if (name.startsWith("i1(") && name.endsWith(")")) {
                    name = name.substring(3, name.length() - 1);
                }
                int lastSlashPos = name.lastIndexOf(separator);
                String contextName = "";
                if (lastSlashPos > 0) {
                    contextName = name.substring(0, lastSlashPos + 1);
                }
                name = name.substring(lastSlashPos + 1);
                if (type == EpicReaderSignal.CURRENT_TYPE) name = "i(" + name + ")";
                writeContext(contextName);
                stdOut.writeByte(type);
                writeString(name);
            } else if (split[0].equals(".vdd") && split.length == 2) {
            } else if (split[0].equals(".time_resolution") && split.length == 2) {
                timeResolution = atof(split[1]) * 1e-9;
            } else if (split[0].equals(".current_resolution") && split.length == 2) {
                currentResolution = atof(split[1]);
            } else if (split[0].equals(".voltage_resolution") && split.length == 2) {
                voltageResolution = atof(split[1]);
            } else if (split[0].equals(".high_threshold") && split.length == 2) {
            } else if (split[0].equals(".low_threshold") && split.length == 2) {
            } else if (split[0].equals(".nnodes") && split.length == 2) {
            } else if (split[0].equals(".nelems") && split.length == 2) {
            } else if (split[0].equals(".extra_nodes") && split.length == 2) {
            } else if (split[0].equals(".bus_notation") && split.length == 4) {
            } else if (split[0].equals(".hier_separator") && split.length == 2) {
            } else if (split[0].equals(".case") && split.length == 2) {
            } else {
                message("Unrecognized Epic line: " + line);
            }
        } else if (ch >= '0' && ch <= '9') {
            String[] split = whiteSpace.split(line);
            int num = atoi(split[0]);
            if (split.length  > 1) {
                putValue(num, atoi(split[1]));
            } else {
                putTime(num);
            }
        } else if (ch == ';' || Character.isSpaceChar(ch)) {
        } else {
            message("Unrecognized Epic line: " + line);
        }
    }
    
    private void writeContext(String s) throws IOException {
        int matchSeps = 0;
        int pos = 0;
        matchLoop:
        while (matchSeps < currentContextSeps) {
            String si = currentContext[matchSeps];
            if (pos < s.length() && s.charAt(pos) == 'x')
                pos++;
            if (pos + si.length() >= s.length() || s.charAt(pos + si.length()) != separator)
                break;
            for (int k = 0; k < si.length(); k++)
                if (s.charAt(pos + k) != si.charAt(k))
                    break matchLoop;
            matchSeps++;
            pos += si.length() + 1;
        }
        while (currentContextSeps > matchSeps) {
            stdOut.writeByte('U');
            currentContext[--currentContextSeps] = null;
        }
        assert currentContextSeps == matchSeps;
        while (pos < s.length()) {
            int indexOfSep = s.indexOf(separator, pos);
            assert indexOfSep >= pos;
            stdOut.writeByte('D');
            if (pos < indexOfSep && s.charAt(pos) == 'x')
                pos++;
            String si = s.substring(pos, indexOfSep);
            writeString(si);
            if (currentContextSeps >= currentContext.length) {
                String[] newCurrentContext = new String[currentContext.length*2];
                System.arraycopy(currentContext, 0, newCurrentContext, 0, currentContext.length);
                currentContext = newCurrentContext;
            }
            currentContext[currentContextSeps++] = si;
            pos = indexOfSep + 1;
        }
        assert pos == s.length();
    }
    
    private void writeString(String s) throws IOException {
        if (s == null) {
            stdOut.writeInt(-1);
            return;
        }
        Integer i = stringIds.get(s);
        if (i != null) {
            stdOut.writeInt(i.intValue());
            return;
        }
        stdOut.writeInt(stringIds.size());
        i = new Integer(stringIds.size());
        s = new String(s); // To avoid long char array of substrings
        stringIds.put(s, i);
        stdOut.writeUTF(s);
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
        lineNum++;
        return true;
    }
    
    private void putTime(int time) {
        timesC++;
        maxT = curTime = Math.max(curTime, time);
    }
    
    private void putValue(int sigNum, int value) {
        EpicReaderSignal s = (EpicReaderSignal)signalsByEpicIndex.get(sigNum);
        if (s == null) {
            message("Signal " + sigNum + " not defined");
            return;
        }
        s.putEvent(curTime, value);
        eventsC++;
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
                if (builder.length() == 0) return null;
                lineNum++;
                return builder.toString();
            }
        }
    }
    
    private double atof(String s) {
        double value = 0;
        try {
            value = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            message("Bad float format: " + s);
        }
        return value;
    }
    
    private int atoi(String s) {
        int value = 0;
        try {
            value = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            message("Bad integer format: " + s);
        }
        return value;
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
        showProgress(fileLength != 0 ? byteCount/(double)fileLength : 0);
        return false;
    }
    
    private void showProgress(double ratio) {
        byte progress = (byte)(ratio*100);
        if (progress == lastProgress) return;
        System.err.println("**PROGRESS " + progress);
        lastProgress = progress;
    }
    
    private void showProgressNote(String note) {
        System.err.println("**PROGRESS !" + note);
        lastProgress = 0;
    }

    private void message(String s) {
        System.err.println(s + " in line " + (lineNum + 1));
    }
}

class EpicReaderSignal {
    int type;
    int lastT;
    int lastV;
    int minV = Integer.MAX_VALUE;
    int maxV = Integer.MIN_VALUE;
    byte[] waveform = new byte[512];
//  byte[] waveform = new byte[40];
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
