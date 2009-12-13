/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ExecProcess.java
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io;

import java.net.*;
import java.util.*;
import java.io.*;

/**
 * This class provides the same functionality as Runtime.exec(), but
 * with extra safeguards and utilities.  Includes the ability to
 * execute a command on a remote machine via ssh, optionally rsync'ing
 * the working directory to the remote machine before execution and
 * back afterwards.
 *
 * This class should not depend on other Electric classes.
 *
 * @author megacz (heavily influenced by gainsley's ExecProcess)
 */
public class ExecProcess {

    /** an OutputStream that discards anything written to it */
    public static final OutputStream devNull = new OutputStream() {
            public void write(int b) { }
            public void write(byte[] b, int ofs, int len) { }
        };

    /** an InputStream that always returns EOF */
    public static final InputStream eofInputStream = new InputStream() {
            public int  read() { return -1; }
            public int  read(byte[] buf, int ofs, int len) { return -1; }
            public long skip(long ofs) { return 0; }
            public int  available() { return 0; }
        };

    /**
     *  @param command the command to run (separated into argv[])
     *  @param workingDirectory the working directory on the host
     *         where the command executes (might be a remote machine)
     */
    public void ExecProcess(String[] command, File workingDirectory) {
        this.command = command;
        this.workingDirectory = workingDirectory;
    }

    /**
     *  @param host the hostname to run on
     *  @param user the username on the remote machine (or null to use
     *         whatever default ssh chooses)
     *  @param localDirToSync if non-null, this directory will be
     *         written over the remote directory via "rsync --delete"
     *  @param syncBack if true and the command terminates with exit
     *         code zero, the remote workingDirectory will be synced back via
     *         "rsync --delete"
     */
    public void setRemote(String host, String user, File localDirToSync, boolean syncBack) {
        if (proc!=null) throw new RuntimeException("you cannot invoke ExecProcess.setRemote() after ExecProcess.start()");
        throw new RuntimeException("not implemented");
    }

    /** undoes setRemote() */
    public void setLocal() { }

    public void redirectStdin(InputStream in) {
        if (proc!=null) throw new RuntimeException("you cannot invoke ExecProcess.redirectStdin() after ExecProcess.start()");
        this.redirectStdin = in;
    }

    public void redirectStdout(OutputStream os) {
        if (proc!=null) throw new RuntimeException("you cannot invoke ExecProcess.redirectStdout() after ExecProcess.start()");
        this.redirectStdout = os;
    }

    public void redirectStderr(OutputStream os) {
        if (proc!=null) throw new RuntimeException("you cannot invoke ExecProcess.redirectStderr() after ExecProcess.start()");
        this.redirectStderr = os;
    }

    public void start() throws IOException {
        if (proc!=null) throw new RuntimeException("you cannot invoke ExecProcess.start() twice");
        throw new RuntimeException("not implemented");
    }

    public void destroy() throws IOException {
        if (proc==null) throw new RuntimeException("you must invoke ExecProcess.start() first");
        proc.destroy();
    }

    public int waitFor() throws IOException {
        if (proc==null) throw new RuntimeException("you must invoke ExecProcess.start() first");
        try {
            return proc.waitFor();
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

    public OutputStream getStdin() {
        if (proc==null) throw new RuntimeException("you must invoke ExecProcess.start() first");
        if (redirectStdin!=null) throw new RuntimeException("you cannot invoke getStdin() after redirectStdin()");
        return proc.getOutputStream();
    }

    public InputStream getStdout() {
        if (proc==null) throw new RuntimeException("you must invoke ExecProcess.start() first");
        if (redirectStdout!=null) throw new RuntimeException("you cannot invoke getStdout() after redirectStdout()");
        return proc.getInputStream();
    }

    public InputStream getStderr() {
        if (proc==null) throw new RuntimeException("you must invoke ExecProcess.start() first");
        if (redirectStderr!=null) throw new RuntimeException("you cannot invoke getStderr() after redirectStderr()");
        return proc.getErrorStream();
    }

    private Process      proc;
    private InputStream  redirectStdin;
    private OutputStream redirectStdout;
    private OutputStream redirectStderr;
    private String[]     command;
    private File         workingDirectory;

}
