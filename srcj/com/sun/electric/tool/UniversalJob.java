/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UniversalJob.java
 *
 * Copyright (c) 2007 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool;

/**
 * Job executing given tasks on a server and then on a client
 */
public class UniversalJob extends Job {
    private final Runnable doIt;
    private final transient Runnable terminateOK;
    
    /**
     * @param jobName a name of the Job
     * @param doIt the task to run on a server
     */
    public UniversalJob(String jobName, Runnable doIt) {
        this(jobName, doIt, null);
    }
    
    /**
     * @param jobName a name of the Job
     * @param doIt the task to run on a server
     * @param terminateOK the task to run on a client
     */
    public UniversalJob(String jobName, Runnable doIt, Runnable terminateOK) {
        super(jobName, null, Job.Type.CHANGE, null, null, Priority.USER);
        this.doIt = doIt;
        this.terminateOK = terminateOK;
    }
    
    public boolean doIt() {
        doIt.run();
        return true;
    }
    
    public void terminateOK() {
        if (terminateOK != null)
            terminateOK.run();
    }
}
