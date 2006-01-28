/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EpicReaderProcess.java
 * Input/output tool: external reader for EPIC output (.out)
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
 * This class is a launched in external JVM to read Epic output file.
 * It doesn't import other Electric modules.
 * Waveforms are stored in temporary file.
 * Signal names are passed to std out using the following syntax:
 * StdOut :== signames resolutions signalInfo* fileName
 * signames :== ( up | down | signal )* 'F' 
 * up :== 'U'
 * down :== 'D' stringRef
 * signal :== ( 'V' | 'I' ) stringRef
 * stringRef= INT [ UDF ]
 * resolutions :== timeResolution voltageResolution currentResolution timeMax
 * timeResolution :== DOUBLE
 * voltageResolution :== DOUBLE
 * currentResolution :== DOUBLE
 * timeMax :== DOUBLE
 * signalInfo :== minV maxV packedLength
 * minV :== INT
 * maxV :== INT
 * packedLength :== INT
 * fileName :== UDF
 *
 * Inititially current context i sempty.
 * 'D' pushes a string to it.
 * 'U' pops a string.
 * 'V' and 'I' use the current context.
 *
 * stringRef  starts with a number. If this number is a new number then it is followed by definition of this string,
 * otherwise it is reference of previously defined string.
 * Number of signalInfo is equal to number of defined signals.
 * fileName is a name of temporary file on local machine with packed waveform data.
 * length in signalInfo is a number of bytes occupied in the file by this signal.
 */
class EpicReaderProcess {
    /** Input stream with Epic data. */                         private InputStream inputStream;
    /** File length of inputStream. */                          private long fileLength;
    /** Number of bytes read from stream to buffer. */          private long byteCount;
    /** Buffer for parsing. */                                  private byte[] buf = new byte[65536];
    /** Count of valid bytes in buffer. */                      private int bufL;
    /** Count of parsed bytes in buffer. */                     private int bufP;
    /** Count of parsed lines. */                               private int lineNum;
    /** String builder to build input line. */                  private StringBuilder builder = new StringBuilder();
    /** Pattern used to split input line into pieces. */        private Pattern whiteSpace = Pattern.compile("[ \t]+");
    /** Last value of progress indicater (percents(.*/          private byte lastProgress;
     
    /** A map from Strings to Integer ids. */                   private HashMap<String,Integer> stringIds = new HashMap<String,Integer>();
    /** Chronological list of signals. */                       private ArrayList<EpicReaderSignal> signals = new ArrayList<EpicReaderSignal>();
    /** Sparce list to access signals by their Epic indices. */ private ArrayList<EpicReaderSignal> signalsByEpicIndex = new ArrayList<EpicReaderSignal>();
    /** Time resolution from input file. */                     private double timeResolution;
    /** Voltage resolution from input file. */                  private double voltageResolution;
    /** Current resolution from input file. */                  private double currentResolution;
    /** Current time (in integer units). */                     private int curTime = 0;
    /** A stack of signal context pieces. */                    private ArrayList<String> contextStack = new ArrayList<String>();
    /** Count of timepoints for statistics. */                  private int timesC = 0;
    /** Count of signal events for statistics. */               private int eventsC = 0;
    
    /* DataOutputStream view of standard output. */             private DataOutputStream stdOut = new DataOutputStream(System.out);

    /** Epic format we are able to read. */                     private static final String VERSION_STRING = ";! output_format 5.3";
    /** Epic separator char. */                                 private static final char separator = '.';
    
    /** Private constructor. */
    private EpicReaderProcess() {}

    /**
     * Main program of external JVM for reading Epic files.
     */
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
    
    /**
     * Reads Epic file into memory.
     * Writes signal names to stdout. 
     * @param urlName url name of Epic file.
     */
    private void readEpic(String urlName) throws IOException {
        URL fileURL = new URL(urlName);
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
    }

    /**
     * Parses line from Epic file.
     */
    private void parseNumLine(String line) throws IOException {
        if (line.length() == 0) return;
        char ch = line.charAt(0);
        if (ch == '.') {
            String[] split = whiteSpace.split(line);
            if (split[0].equals(".index") && split.length == 4) {
                byte type;
                if (split[3].equals("v"))
                    type = 'V';
                else if (split[3].equals("i"))
                    type = 'I';
                else {
                    message("Unknown waveform type: " + line);
                    return;
                }
                String name = split[1];
                int sigNum = atoi(split[2]);
                while (signalsByEpicIndex.size() <= sigNum)
                    signalsByEpicIndex.add(null);
                EpicReaderSignal s = signalsByEpicIndex.get(sigNum);
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
                if (type == 'I') name = "i(" + name + ")";
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
    
    /**
     * Writes new context as diff to previous context.
     * @param s string with new context.
     */
    private void writeContext(String s) throws IOException {
        int matchSeps = 0;
        int pos = 0;
        matchLoop:
        while (matchSeps < contextStack.size()) {
            String si = contextStack.get(matchSeps);
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
        while (contextStack.size() > matchSeps) {
            stdOut.writeByte('U');
            contextStack.remove(contextStack.size() - 1);
        }
        assert contextStack.size() == matchSeps;
        while (pos < s.length()) {
            int indexOfSep = s.indexOf(separator, pos);
            assert indexOfSep >= pos;
            stdOut.writeByte('D');
            if (pos < indexOfSep && s.charAt(pos) == 'x')
                pos++;
            String si = s.substring(pos, indexOfSep);
            writeString(si);
            contextStack.add(si);
            pos = indexOfSep + 1;
        }
        assert pos == s.length();
    }
    
    /**
     * Writes string to stdOut.
     * It writes its chronological number.
     * It writes string itself, if it is a new string.
     * @param s string to write.
     */
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
    
    /**
     * Fast routine to parse a line from buffer.
     * It either recognizes a simple line or rejects.
     * @return true if this method recognized a line.
     */
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
    
    /**
     * Sets new current time.
     * This current time will be used in PutBValue method.
     * @param time time as int value.
     */
    private void putTime(int time) {
        timesC++;
        curTime = Math.max(curTime, time);
    }
    
    /**
     * Puts event into packed waveforms.
     * @param sigNum Epic signal index of signal.
     * @param value new value of signal.
     */
    private void putValue(int sigNum, int value) {
        EpicReaderSignal s = signalsByEpicIndex.get(sigNum);
        if (s == null) {
            message("Signal " + sigNum + " not defined");
            return;
        }
        s.putEvent(curTime, value);
        eventsC++;
    }
    
    /**
     * Gets new line from buffer.
     * @return String line read.
     */
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
    
    /**
     * ASCII to double.
     * @param s ASCII string. 
     * @return double value of string.
     */
    private double atof(String s) {
        double value = 0;
        try {
            value = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            message("Bad float format: " + s);
        }
        return value;
    }
    
    /**
     * ASCII to int.
     * @param s ASCII string. 
     * @return int value of string.
     */
    private int atoi(String s) {
        int value = 0;
        try {
            value = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            message("Bad integer format: " + s);
        }
        return value;
    }
    
    /**
     * Reads buffer from Epic file.
     */
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
    
    /**
     * Writes resolutions, signalInfo and fileName to stdOut.
     */
    private void writeOut() throws IOException {
        File tempFile = null;
        BufferedOutputStream waveStream = null;
        boolean ok = false;
        try {
            long startTime = System.currentTimeMillis();
            tempFile = File.createTempFile("elec", ".epic");
            waveStream = new BufferedOutputStream(new FileOutputStream(tempFile));
            
            showProgressNote("Writing " + tempFile);
            stdOut.writeByte('F');
            
            stdOut.writeDouble(timeResolution);
            stdOut.writeDouble(voltageResolution);
            stdOut.writeDouble(currentResolution);
            stdOut.writeDouble(curTime*timeResolution);
            int size = 0;
            for (EpicReaderSignal s: signals)
                size += s.len;
            int start = 0;
            for (EpicReaderSignal s: signals) {
                waveStream.write(s.waveform, 0, s.len);
                
                stdOut.writeInt(s.minV);
                stdOut.writeInt(s.maxV);
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
    
    /**
     * Puts progress mark into stdErr.
     */
    private void showProgress(double ratio) {
        byte progress = (byte)(ratio*100);
        if (progress == lastProgress) return;
        System.err.println("**PROGRESS " + progress);
        lastProgress = progress;
    }
    
    /**
     * Puts progress not mark into stdErr.
     */
    private void showProgressNote(String note) {
        System.err.println("**PROGRESS !" + note);
        lastProgress = 0;
    }

    /**
     * Puts message int stdErr.
     */
    private void message(String s) {
        System.err.println(s + " in line " + (lineNum + 1));
    }
}

/**
 * This class is a buffer to pack waveform of Epic signal.
 */
class EpicReaderSignal {
    /** Time of last event.*/               int lastT;
    /** Value of last event. */             int lastV;
    /** Minimal value among events. */      int minV = Integer.MAX_VALUE;
    /** Maximal value among events. */      int maxV = Integer.MIN_VALUE;

    /** Packed waveform. */                 byte[] waveform = new byte[512];
    /** Count of bytes used in waveform. */ int len;

    /**
     * Puts event into waveform. 
     * @param t time of event.
     * @param v value of event.
     */
    void putEvent(int t, int v) {
        putUnsigned(t - lastT);
        putSigned(v - lastV);
        lastT = t;
        lastV = v;
        minV = Math.min(minV, v);
        maxV = Math.max(maxV, v);
    }

    /**
     * Packes unsigned int.
     * @param value value to pack.
     */
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
    
    /**
     * Packes signed int.
     * @param value value to pack.
     */
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
    
    /**
     * Unpackes waveform.
     * @return array with time/value pairs.
     */    
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
    
    /**
     * Puts byte int packed array.
     * @param value byte to but.
     */
    void putByte(int value) { waveform[len++] = (byte)value; }
    
    /**
     * Ensures that it is possible to add specified number of bytes to packed waveform.
     * @param l number of bytes.
     */
    void ensureCapacity(int l) {
        if (len + l <= waveform.length) return;
        byte[] newWaveform = new byte[waveform.length*3/2];
        System.arraycopy(waveform, 0, newWaveform, 0, waveform.length);
        waveform = newWaveform;
        assert len + l <= waveform.length;
    }
}
