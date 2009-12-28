/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ESandBox.java
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * This abstract class is a framework for stand-alone process which loads "electric.jar (possibly with old Electric version),
 * (partially) initializes it and executes there different test Jobs.
 * The results of test Jobs is sent to stdout in the folowng format:
 *
 * StdOut = (JobResult | PrintOutput)*
 *
 * JobResult = HEADER '\n' JobSuccess LengthString '\n' Byte+ '\n' '!' '\n'
 * HEADER = '\x7F'
 * JobSuccess = Ok | Exception
 * Ok = 'R'
 * Exception = 'E'
 * LenghtString = DIGIT+            // 10-digit number
 *
 * PrintOutput = LF | CR | PrintString
 * LF = '\n'
 * CR = '\r'
 * PrintString = LengthByte Byte+   // contains (LengthByte - ' ' + 1) bytes
 *
 * The subclass should contain methods with signature "commandName(String args), which implement commands,
 * and a main method like this:
 *
 *  public static void main(String[] args) {
 *      try {
 *          File electricJar = new File(args[0]);
 *          MySandBox m = new MySandBox(electricJar);
 *          m.loop(System.in);
 *      } catch (Exception e) {
 *          e.printStackTrace();
 *      }
 *  }
 *
 * Only single instance of subclasses of this method is allowed in a process.
 *
 */
public abstract class ESandBox extends EClassLoader {
    private static final PrintStream stdOut = System.out;
    private static final OutputStream redirectedStdOut = new OutputStream() {
        @Override public void write(int b) { write(new byte[] { (byte)b }, 0, 1); }
        @Override public void write(byte b[], int off, int len) { writeStdOut(b, off, len); }
    };
    static { System.setOut(new PrintStream(redirectedStdOut, false)); }

    private static final RunnableTask task = new RunnableTask();
    private static ESandBox theSandBox;
    private static String command;

    private static final Object lock = new Object();
    private static byte[] serializedResultOrException;
    private static boolean isException;

    private final Constructor UniversalJob_constructor = getDeclaredConstructor(defineClass("com.sun.electric.tool.UniversalJob"), String.class, Runnable.class);

    /*
     * Abstract constructor of a ESandBox.
     * It loads classes from specified URL.
     * Also first call of this constructor redirects stdout as a size effect.
     * @param electricJar URL of "electric.jar" file.
     */
    protected ESandBox(URL electricJar) throws IOException, IllegalAccessException, ClassNotFoundException {
        super(electricJar);
    }

    public static void redirectStdOut(String args) {
        System.setOut(new PrintStream(redirectedStdOut, false));
    }

    /**
     * Command interpreter which executes commands from command stream.
     * Each command is placed at the beginning of command line. Its arguments are after one or more spaces.
     * Lines beginning with spaces and empty lines are considered as comments.
     * A method like "commandName(String args)" is found by reflexion in subclass of ESandBox.
     * @param commandStream a stream with commands.
     */
    protected void loop(InputStream commandStream) throws InstantiationException, IllegalAccessException, InvocationTargetException, IOException  {
        synchronized (lock) {
            if (theSandBox != null)
                throw new IllegalStateException("SandBox already instantiated");
            theSandBox = this;
        }
        command = "redirectStdOut";
        (new ServerManagerThread()).start();
        BufferedReader commandReader = new BufferedReader(new InputStreamReader(commandStream));
        for (int i = 0;;) {
            synchronized (lock) {
                try {
                    while (serializedResultOrException == null)
                        lock.wait();
                } catch (InterruptedException e) {
                }
            }
            Object result = null;
            if (serializedResultOrException.length != 0) {
                try {
                    ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(serializedResultOrException));
                    result = in.readObject();
                    in.close();
                } catch (Throwable e) {
                    result = new Exception(e);
                }
            }
            writeStdOut(serializedResultOrException, isException);
            serializedResultOrException = null;

            command = commandReader.readLine();
            if (command == null)
                System.exit(0);
            Object job = createJob(command);
            Job_startJob.invoke(job);
       }
    }

    private class ServerManagerThread extends Thread {
        ServerManagerThread() {
            super("ServerManager");
        }

        @Override
        public void run() {
            try {
                if (Job_initJobManager1 != null || Job_initJobManager2 != null || Job_initJobManager3 != null) {
                    Object ui = MainUserInterfaceDummy_constructor.newInstance();
                    Object jobMode = JobMode_SERVER.get(null);
//            Object jobMode = JobMode_BATCH.get(null);
                    if (Job_setThreadMode1 != null)
                        Job_setThreadMode1.invoke(null, jobMode, ui);
                    else if (Job_setThreadMode2 != null)
                        Job_setThreadMode2.invoke(null, jobMode, ui);
                    Object job = createJob("SandBox");
                    if (Job_initJobManager1 != null)
                        Job_initJobManager1.invoke(null, 1, job, null, null);
                    else if (Job_initJobManager2 != null)
                        Job_initJobManager2.invoke(null, 1, job, null);
                    else if (Job_initJobManager3 != null)
                        Job_initJobManager3.invoke(null, 1, job);
                } else {
                    Object job = createJob("SandBox");
                    Job_startJob.invoke(job);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Object createJob(String jobName) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        return UniversalJob_constructor.newInstance(jobName, task);
    }

    private static class RunnableTask implements Runnable, Serializable {
        public void run() {
            String cmd, args;
            int i = command.indexOf(' ');
            if (i >= 0 ) {
                cmd = command.substring(0, i);
                args = command.substring(i);
            } else {
                cmd = command;
                args = "";
            }
            while (args.startsWith(" "))
                args = args.substring(1);
            Object result = null;
            Exception exception = null;
            try {
                if (cmd.length() > 0) {
                    Method method = theSandBox.getClass().getMethod(cmd, String.class);
                    result = method.invoke(theSandBox, args);
                }
            } catch (Exception e) {
                exception = e;
            } catch (Throwable e) {
                exception = new Exception("Error", e);
                exception.fillInStackTrace();
            }

            byte[] serialized = null;
            try {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(byteStream);
                out.writeObject(exception != null ? exception : result);
                out.flush();
                serialized = byteStream.toByteArray();
            } catch (Throwable e) {
                exception = new Exception("Serialization Error", e);
                exception.fillInStackTrace();
                try {
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    ObjectOutputStream out = new ObjectOutputStream(byteStream);
                    out.writeObject(exception);
                    out.flush();
                    serialized = byteStream.toByteArray();
                } catch (Throwable e2) {
                    serialized = new byte[0];
                }
            }
            synchronized (lock) {
                try {
                    while (ESandBox.serializedResultOrException != null)
                        lock.wait();
                } catch (InterruptedException e) {
                    System.out.println("Interrupted");
                    Thread.currentThread().interrupt();
                }
                isException = exception != null;
                ESandBox.serializedResultOrException = serialized;
                lock.notify();
            }
        }
    }

    private static final char MIN_LENGTH_CHAR = ' ';
    private static final char HEADER_CHAR = 0x7F;
    private static final int STRLEN_WIDTH = Integer.toString(Integer.MAX_VALUE).length();
    private static final int HEADER_LEN = 3 + STRLEN_WIDTH + 1;
    private static final int TRAILER_LEN = 3;

    private static synchronized void writeStdOut(byte b[], int off, int len) {
        while (len > 0 && (b[off] == '\n' || b[off] == '\r')) {
            stdOut.write(b[off]);
            off++;
            len--;
        }
        while (len > 0) {
            int l = Math.min(len, HEADER_CHAR - MIN_LENGTH_CHAR);
            stdOut.write(MIN_LENGTH_CHAR + l - 1);
            stdOut.write(b, off, l);
            off += l;
            len -= l;
        }
    }

    private static synchronized void writeStdOut(byte[] b, boolean isException) {
        stdOut.write(HEADER_CHAR);
        stdOut.write('\n');
        stdOut.write(isException ? 'E' : 'R');
        String s = Integer.toString(b.length);
        while (s.length() < STRLEN_WIDTH)
            s = '0' + s;
        for (int i = 0; i < s.length(); i++)
            stdOut.write(s.charAt(i));
        stdOut.write('\n');
        stdOut.write(b, 0, b.length);
        stdOut.write('\n');
        stdOut.write('!');
        stdOut.write('\n');
    }
}
