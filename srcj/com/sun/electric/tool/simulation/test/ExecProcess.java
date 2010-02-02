/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ExecProcess.java
 * Written by Jonathan Gainsley, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation.test;

import java.io.*;

/**
 * Runtime.exec() has many pitfalls to it's proper use.  This class
 * wraps external executes to make it easier to use.
 */
class ExecProcess extends Thread {

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
            setDaemon(true);
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
                char [] cbuf = new char[256];
                int read = 0;
                while ((read = reader.read(cbuf, 0, 256)) > -1) {
                    if (pw != null) {
                        pw.write(cbuf, 0, read);
                        pw.flush();
                    }
                }

                reader.close();
                input.close();

            } catch (java.io.IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private final String command;
    private final String [] exec;
    private final String [] envVars;
    private final File dir;                       // working directory
    private final OutputStream outStreamRedir;    // output of process redirected to this stream
    private final OutputStream errStreamRedir;    // error messages of process redirected to this stream
    private PrintWriter processWriter;      // connect to input of process
    private ExecProcessReader outReader;
    private ExecProcessReader errReader;
    private Process p = null;

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
    public ExecProcess(String command, String [] envVars, File dir, OutputStream outStreamRedir, OutputStream errStreamRedir) {
        this.command = command;
        this.exec = null;
        this.envVars = envVars;
        this.dir = dir;
        this.outStreamRedir = outStreamRedir;
        this.errStreamRedir = errStreamRedir;
        this.processWriter = null;
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
    public ExecProcess(String [] exec, String [] envVars, File dir, OutputStream outStreamRedir, OutputStream errStreamRedir) {
        this.command = null;
        this.exec = exec;
        this.envVars = envVars;
        this.dir = dir;
        this.outStreamRedir = outStreamRedir;
        this.errStreamRedir = errStreamRedir;
        this.processWriter = null;
        setName(exec[0]);
    }

    public void run() {
        try {
            Runtime rt = Runtime.getRuntime();

            outReader = null;
            errReader = null;

            // run program
            synchronized(this) {
                if (command != null)
                    p = rt.exec(command, envVars, dir);
                else
                    p = rt.exec(exec, envVars, dir);

                // eat output (stdout) and stderr from program so it doesn't block
                outReader = new ExecProcessReader(p.getInputStream(), outStreamRedir);
                errReader = new ExecProcessReader(p.getErrorStream(), errStreamRedir);
                outReader.start();
                errReader.start();

                // attach to input of process
                BufferedOutputStream bufout = new BufferedOutputStream(p.getOutputStream());
                processWriter = new PrintWriter(bufout);
            }

            // wait for exit status
            int exitVal = p.waitFor();

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

            System.out.println("Process finished [exit: "+exitVal+"]: "+com.toString());

            synchronized(this) {
                if (processWriter != null) {
                    processWriter.close();
                    processWriter = null;
                }
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
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
                System.out.println("No process to write to: "+line);
                return;
            }
            processWriter.println(line);
            processWriter.flush();
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

}
