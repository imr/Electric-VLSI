/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TechExplorerDriver.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.sandbox;

import com.sun.electric.Main;
import com.sun.electric.tool.user.ActivityLogger;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Class to launch a TechExplorer process and to communicate with it
 */
public class TechExplorerDriver {
    private final Process process;
    private final PrintWriter commandsWriter;

    /** Creates a new instance of TechExplorerDriver */
    public TechExplorerDriver(String jarPath, final OutputStream redirect) throws IOException {
        this(new ProcessBuilder("java",
                "-cp",
                Main.getJarLocation(),
                "-ss2m",
                "-ea",
                "-mx128m",
                "com.sun.electric.tool.sandbox.TechExplorer",
                jarPath),
            redirect);
        
    }
    
    /** Creates a new instance of TechExplorerDriver */
    public TechExplorerDriver(ProcessBuilder processBuilder, final OutputStream redirect) throws IOException {
        process = processBuilder.start();
        commandsWriter = new PrintWriter(process.getOutputStream());
        Thread stdErrReader = new Thread() {
            public void run() {
                PrintWriter pw = redirect != null ? new PrintWriter(redirect) : null;

                // read from stream
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String line = null;
                try {
                    while ((line = reader.readLine()) != null) {
                        if (pw != null) {
                            pw.println(line);
                            pw.flush();
                        }
                    }
                    reader.close();
                } catch (IOException e) {
                    ActivityLogger.logException(e);
                }
            }
        };
        stdErrReader.start();
        (new StdOutThread()).start();
    }

    public void putCommand(String cmd) {
        commandsWriter.println(cmd);
        commandsWriter.flush();
    }

    public void putCommand(String cmd, String args) {
        commandsWriter.println(cmd + " " + args);
        commandsWriter.flush();
    }
    
    protected void terminateOk(Object result) {
        System.out.println("Result " + result);
    }
    
    protected void terminateFail(Exception e) {
        System.out.println("Exception " + e);
        e.printStackTrace(System.out);
    }

    public void closeCommands() {
        commandsWriter.close();
    }

    private static final char MIN_LENGTH_CHAR = ' ';
    private static final char HEADER_CHAR = 0x7F;
    private static final int STRLEN_WIDTH = Integer.toString(Integer.MAX_VALUE).length();
    private static final int HEADER_LEN = 3 + STRLEN_WIDTH + 1;
    private static final int TRAILER_LEN = 3;

    private class StdOutThread extends Thread {
        private byte[] buf = new byte[128];
        private DataInputStream resultsStream = new DataInputStream(new BufferedInputStream(process.getInputStream()));

        public void run() {
            try {
                for (;;) {
                    int c = resultsStream.read();
                    if (c < 0) break;
                    if (c == HEADER_CHAR) {
                        if (resultsStream.read() != '\n') throw new IOException();
                        int status = resultsStream.read();
                        boolean isException;
                        if (status == 'R')
                            isException = false;
                        else if (status == 'E')
                            isException = true;
                        else
                            throw new IOException();
                        int len = 0;
                        for (int i = 0; i < STRLEN_WIDTH; i++) {
                            int cc = resultsStream.read();
                            if (cc < '0' || cc > '9') throw new IOException();
                            len = len * 10 + cc - '0';
                        }
                        if (resultsStream.read() != '\n') throw new IOException();
                        byte[] b = new byte[len];
                        resultsStream.readFully(b);
                        Object result = null;
                        try {
                            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(b));
                            result = in.readObject();
                            in.close();
                        } catch (Throwable e) {
                        }
                        if (resultsStream.read() != '\n') throw new IOException();
                        if (resultsStream.read() != '!') throw new IOException();
                        if (resultsStream.read() != '\n') throw new IOException();
                        if (isException) {
                            terminateFail((Exception)result);
                        } else {
                            terminateOk(result);
                        }
                    } else if (c >= ' ' && c < HEADER_CHAR) {
                        int len = c - ' ' + 1;
                        readBuf(len);
                        System.out.write(buf, 0, len);
                    } else {
                        System.out.write(c);
                    }
                }
                resultsStream.close();
            } catch (IOException e) {
                ActivityLogger.logException(e);
            }
        }

        private void readBuf(int len) throws IOException {
            if (buf.length < len)
                buf = new byte[Math.max(buf.length*2, len)];
            resultsStream.readFully(buf, 0, len);

        }
    }
}
