/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Exec.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user;

import java.io.*;
import java.util.ArrayList;

/**
 * Runtime.exec() has many pitfalls to it's proper use.  This class
 * wraps external executes to make it easier to use.
 * <P>
 * Usage:
 * <pre>
 * Exec exec = new Exec("ls", null, User.getWorkingDirectory(), System.out, System.out);
 * exec.start(); // run in a new Thread
 * </pre>
 * You can also use exec.run() to run in the current thread.
 */
public class Exec extends Thread {

    /**
     * This class is used to read data from an external process.
     * If something does not consume the data, it will fill up the default
     * buffer and deadlock.  This class also redirects data read
     * from the process (the process' output) to another stream,
     * if specified.
     */
    public static class ExecProcessReader extends Thread {

        private InputStream in;
        private OutputStream redirect;

        /**
         * Create a stream reader that will read from the stream
         * @param in the input stream
         */
        public ExecProcessReader(InputStream in) {
            this(in, null);
        }

        /**
         * Create a stream reader that will read from the stream, and
         * store the read text into buffer.
         * @param in the input stream
         * @param redirect read text is redirected to this
         */
        public ExecProcessReader(InputStream in, OutputStream redirect) {
            this.in = in;
            this.redirect = redirect;
            setName("ExecProcessReader");
        }

        public void run() {
            try {
                PrintWriter pw = null;
                if (redirect != null) pw = new PrintWriter(redirect);

                // read from stream
                InputStreamReader input = new InputStreamReader(in);
                BufferedReader reader = new BufferedReader(input);
                String line = null;
                while ((line = reader.readLine()) != null) {
                    if (pw != null) {
                        pw.println(line);
                        pw.flush();
                    }
                }

                reader.close();
                input.close();

            } catch (java.io.IOException e) {
                ActivityLogger.logException(e);
            }
        }
    }

    /**
     * Objects that want to be notified of the process finishing should
     * implement this interface, and add themselves as a listener to the
     * process.
     */
    public interface FinishedListener {
        public void processFinished(FinishedEvent e);
    }

    /**
     * The event passed to listeners when the process finishes
     */
    public static class FinishedEvent {
        private Object source;
        private String exec;
        private int exitValue;
        private File dir;                   // working directory

        public FinishedEvent(Object source, String exec, File dir, int exitValue) {
            this.source = source;
            this.exec = exec;
            this.exitValue = exitValue;
            this.dir = dir;
        }

        public Object getSource() { return source; }
        public String getExec() { return exec; }
        public int getExitValue() { return exitValue; }
        public File getWorkingDir() { return dir; }
    }

    private final String command;
    private final String [] exec;
    private final String [] envVars;
    private final File dir;                       // working directory
    private final OutputStream outStreamRedir;    // output of process redirected to this stream
    private final OutputStream errStreamRedir;    // error messages of process redirected to this stream
    private PrintWriter processWriter;      // connect to input of process
    //private ExecProcessReader outReader;
    //private ExecProcessReader errReader;
    private Process p = null;
    private int exitVal;
    private final ArrayList<FinishedListener> finishedListeners;    // list of listeners waiting for process to finish

    /**
     * Execute an external process.
     * Note: command is not a shell command line command, it is a single program and arguments.
     * Therefore, <code>/bin/sh -c /bin/ls > file.txt</code> will NOT work.
     * <p>
     * Instead, use String[] exec = {"/bin/sh", "-c", "/bin/ls > file.txt"};
     * and use the other constructor.
     * @param command the command to run.
     * @param envVars environment variables of the form name=value. If null, inherits vars from current process.
     * @param dir the working directory. If null, uses the working dir from the current process
     * @param outStreamRedir stdout of the process will be redirected to this stream if not null
     * @param errStreamRedir stderr of the process will be redirected to this stream if not null
     */
    public Exec(String command, String [] envVars, File dir, OutputStream outStreamRedir, OutputStream errStreamRedir) {
        this.command = command;
        this.exec = null;
        this.envVars = envVars;
        this.dir = dir;
        this.outStreamRedir = outStreamRedir;
        this.errStreamRedir = errStreamRedir;
        this.processWriter = null;
        this.finishedListeners = new ArrayList<FinishedListener>();
        this.exitVal = -1;
        setName(command);
    }

    /**
     * Execute an external process.
     * Note: this is not a command-line command, it is a single program and arguments.
     * @param exec the executable and arguments of the process
     * @param envVars environment variables of the form name=value. If null, inherits vars from current process.
     * @param dir the working directory. If null, uses the working dir from the current process
     * @param outStreamRedir stdout of the process will be redirected to this stream if not null
     * @param errStreamRedir stderr of the process will be redirected to this stream if not null
     */
    public Exec(String [] exec, String [] envVars, File dir, OutputStream outStreamRedir, OutputStream errStreamRedir) {
        this.command = null;
        this.exec = exec;
        this.envVars = envVars;
        this.dir = dir;
        this.outStreamRedir = outStreamRedir;
        this.errStreamRedir = errStreamRedir;
        this.processWriter = null;
        this.finishedListeners = new ArrayList<FinishedListener>();
        this.exitVal = -1;
        setName(exec[0]);
    }

    public void run() {
        try {
            Runtime rt = Runtime.getRuntime();

            ExecProcessReader outReader = null;
            ExecProcessReader errReader = null;
            
            // run program
            synchronized(this) {
                try {
                    if (command != null)
                        p = rt.exec(command, envVars, dir);
                    else
                        p = rt.exec(exec, envVars, dir);
                } catch (IOException e) {
                    System.out.println("Error running "+command+": "+e.getMessage());
                    return;
                }

                // eat output (stdout) and stderr from program so it doesn't block
                outReader = new ExecProcessReader(p.getInputStream(), outStreamRedir);
                errReader = new ExecProcessReader(p.getErrorStream(), errStreamRedir);
                outReader.start();
                errReader.start();

                // attach to input of process
                processWriter = new PrintWriter(p.getOutputStream());
            }

            // wait for exit status
            exitVal = p.waitFor();

            // also wait for redir threads to die, if doing redir
            if (outStreamRedir != null) outReader.join();
            if (errStreamRedir != null) errReader.join();

            StringBuffer com = new StringBuffer();
            if (command != null)
                com.append(command);
            else {
                for (int i=0; i<exec.length; i++)
                    com.append(exec[i]+" ");
            }

            //System.out.println("Process finished [exit: "+exitVal+"]: "+com.toString());
            synchronized(finishedListeners) {
                FinishedEvent e = new FinishedEvent(this, com.toString(), dir, exitVal);
                ArrayList<FinishedListener> copy = new ArrayList<FinishedListener>();
                // make copy cause listeners may want to remove themselves if process finished
                for (FinishedListener l : finishedListeners) {
                    copy.add(l);
                }
                for (FinishedListener l : copy) {
                    l.processFinished(e);
                }
            }

            synchronized(this) {
                if (processWriter != null) {
                    processWriter.close();
                    processWriter = null;
                }
            }

        } catch (Exception e) {
            ActivityLogger.logException(e);
        }
    }

    /**
     * Send a line of text to the process. This is not useful
     * if the process is not expecting any input.
     * @param line a line of text to send to the process
     */
    public void writeln(String line) {
        synchronized(this) {
            if (processWriter == null) {
                System.out.println("Can't write to process: No valid process running.");
                return;
            }
            processWriter.println(line);
            processWriter.flush();
        }
    }

    /**
     * Add a Exec.FinishedListener
     * @param a the listener
     */
    public void addFinishedListener(FinishedListener a) {
        synchronized(finishedListeners) {
            finishedListeners.add(a);
        }
    }

    /**
     * Remove a Exec.FinishedListener
     * @param a the listener
     */
    public void removeFinishedListener(FinishedListener a) {
        synchronized(finishedListeners) {
            finishedListeners.remove(a);
        }
    }

    /**
     * End this process, if it is running. Otherwise, does nothing
     */
    public synchronized void destroyProcess() {
        if (p != null) {
            p.destroy();
        }
    }

    public int getExitVal() { return exitVal; }
}
