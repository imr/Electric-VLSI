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

import javax.swing.*;
import java.io.*;

/**
 * Runtime.exec() has many pitfalls to it's proper use.  This class
 * wraps external executes to make it easier to use.
 */
public class Exec extends Thread {

    public static class ExecProcessReader extends Thread {

        private InputStream in;
        private OutputStream redirect;

        /**
         * Create a stream reader that will read from the stream
         * @param in the input stream
         */
        public ExecProcessReader(InputStream in) {
            this.in = in;
            redirect = null;
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
                    if (pw != null) pw.println(line);
                }
                if (pw != null) pw.flush();

            } catch (java.io.IOException e) {
                e.printStackTrace(System.out);
            }
        }
    }

    private String command;
    private String [] exec;
    private String [] envVars;
    private OutputStream outStreamRedir;    // output of process redirected to this stream
    private OutputStream errStreamRedir;    // error messages of process redirected to this stream
    private OutputStream outStream;         // stream to send messages to input of process.
    private Process p;

    /**
     * Note: command is not a shell command line command, it is a single program and arguments.
     * Therefore, <code>/bin/sh -c /bin/ls > file.txt</code> will NOT work.
     * <p>
     * Instead, use String[] exec = {"/bin/sh", "-c", "/bin/ls > file.txt"};
     * and use the other constructor.
     * @param command the command to run.
     * @param envVars of the form name=value.
     */
    public Exec(String command, String [] envVars, OutputStream outStreamRedir, OutputStream errStreamRedir) {
        this.command = command;
        this.exec = null;
        this.envVars = envVars;
        this.outStreamRedir = outStreamRedir;
        this.errStreamRedir = errStreamRedir;
        this.outStream = null;
    }

    public Exec(String [] exec, String [] envVars, OutputStream outStreamRedir, OutputStream errStreamRedir) {
        this.command = null;
        this.exec = exec;
        this.envVars = envVars;
        this.outStreamRedir = outStreamRedir;
        this.errStreamRedir = errStreamRedir;
        this.outStream = null;
    }

    private void startProcess() {
        try {
            Runtime rt = Runtime.getRuntime();

            // run program
            if (command != null)
                p = rt.exec(command, envVars);
            else
                p = rt.exec(exec, envVars);

            // eat output (stdout) and stderr from program so it doesn't block
            ExecProcessReader outReader = new ExecProcessReader(p.getInputStream(), outStreamRedir);
            ExecProcessReader errReader = new ExecProcessReader(p.getErrorStream(), errStreamRedir);
            outReader.start();
            errReader.start();

            // attach to input of process
            outStream = p.getOutputStream();


        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public void run() {
        try {
            startProcess();

            // wait for exit status
            int exitVal = p.waitFor();
            if (exitVal != 0) {
                JOptionPane.showMessageDialog(null, exec, "Exec failed: return value: "+exitVal, JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

}
