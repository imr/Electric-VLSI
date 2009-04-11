/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Client.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.Snapshot;
import com.sun.electric.database.change.Undo;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.User;

import com.sun.electric.tool.user.ui.JobTree;
import com.sun.electric.tool.user.ui.TopLevel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 *
 */
public abstract class Client {
    final int connectionId;
    int serverJobId;
    int clientJobId;
    final String userName = System.getProperty("user.name");
	/** The current operating system. */					private static final OS os = OSInitialize();

    /**
	 * OS is a typesafe enum class that describes the current operating system.
	 */
	public enum OS
	{
		/** Describes Windows. */							WINDOWS("Windows"),
		/** Describes UNIX/Linux. */						UNIX("UNIX"),
		/** Describes Macintosh. */							MACINTOSH("Macintosh");

		private String name;

		private OS(String name) { this.name = name; }

		/**
		 * Returns a printable version of this OS.
		 * @return a printable version of this OS.
		 */
		public String toString() { return name; }
    }

    private static OS OSInitialize()
    {
        try{
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.startsWith("windows"))
            {
                return Client.OS.WINDOWS;

            } else if (osName.startsWith("linux") ||
                    osName.startsWith("solaris") || osName.startsWith("sunos"))
            {
                return Client.OS.UNIX;
            } else if (osName.startsWith("mac"))
            {
                return Client.OS.MACINTOSH;
            }
        } catch(Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("No OS detected");
        return null;
    }

    /**
	 * Method to tell which operating system Electric is running on.
	 * @return the operating system Electric is running on.
	 */
	public static OS getOperatingSystem() { return os; }

    public static boolean isOSWindows() { return os == OS.WINDOWS; }
    public static boolean isOSMac() { return os == OS.MACINTOSH; }

    /** Creates a new instance of AbstractClient */
    public Client(int connectionId) {
        this.connectionId = connectionId;
    }

    public synchronized Job.Key newJobId(boolean isServer) {
        int jobId = isServer ? ++serverJobId : --clientJobId;
        return new Job.Key(this, jobId);
    }
    
    protected abstract void consume(EJobEvent e) throws Exception;
    protected abstract void consume(PrintEvent e) throws Exception;
    protected abstract void consume(JobQueueEvent e) throws Exception;

    public static abstract class ServerEvent implements Runnable {
        final Snapshot snapshot;
        final long timeStamp;
        ServerEvent next;

        ServerEvent(Snapshot snapshot) {
            this(snapshot, System.currentTimeMillis());
        }

        ServerEvent(Snapshot snapshot, long timeStamp) {
            this.snapshot = snapshot;
            this.timeStamp = timeStamp;
        }

        public void run() {}
        abstract void dispatch(Client client) throws Exception;
    }

    static void fireEJobEvent(EJob ejob) {
        fireServerEvent(new EJobEvent(ejob, ejob.state));
    }

    static void print(EJob ejob, String s) {
        fireServerEvent(new PrintEvent(ejob.oldSnapshot, ejob.client, s));
    }

    static void fireJobQueueEvent(Snapshot snapshot) {
        ArrayList<Job.Inform> jobs = new ArrayList<Job.Inform>();
        for (Iterator<Job> it = Job.getAllJobs(); it.hasNext();) {
            Job j = it.next();
            if (j.getDisplay()) {
                jobs.add(j.getInform());
            }
        }
        fireServerEvent(new JobQueueEvent(snapshot, jobs.toArray(new Job.Inform[jobs.size()])));
    }

    private static void fireServerEvent(ServerEvent serverEvent) {
        if (Job.currentUI != null)
            Job.currentUI.addEvent(serverEvent);
        StreamClient.addEvent(serverEvent);
    }

    static class EJobEvent extends ServerEvent {
        final EJob ejob;
        final EJob.State newState;

        private EJobEvent(EJob ejob, EJob.State newState) {
            super(ejob.oldSnapshot);
            this.ejob = ejob;
            this.newState = newState;
        }

        EJobEvent(EJob ejob, EJob.State newState, long timeStamp) {
            super(ejob.oldSnapshot, timeStamp);
            this.ejob = ejob;
            this.newState = newState;
        }

        public void run() {
            assert newState == EJob.State.SERVER_DONE;
            if (newState == EJob.State.SERVER_DONE) {
                boolean undoRedo = ejob.jobType == Job.Type.UNDO;
                if (!ejob.isExamine()) {
                    int restoredHighlights = Undo.endChanges(ejob.oldSnapshot, ejob.getJob().tool, ejob.jobName, ejob.newSnapshot);
                    Job.getExtendedUserInterface().showSnapshot(ejob.newSnapshot, undoRedo);
                    Job.getExtendedUserInterface().restoreHighlights(restoredHighlights);
                }

                //if (ejob.client == Job.getExtendedUserInterface())
                {
                    Throwable jobException = null;
                    if (ejob.startedByServer)
                        jobException = ejob.deserializeToClient();
                    if (jobException != null) {
                        System.out.println("Error deserializing " + ejob.jobName);
                        ActivityLogger.logException(jobException);
                        return;
                    }
                    jobException = ejob.deserializeResult();

                    Job job = ejob.clientJob;
                    if (job == null) {
                        ActivityLogger.logException(jobException);
                        return;
                    }
                    try {
                        job.terminateIt(jobException);
                    } catch (Throwable e) {
                        System.out.println("Exception executing terminateIt");
                        e.printStackTrace(System.out);
                    }
                    job.endTime = System.currentTimeMillis();
                    job.finished = true;                        // is this redundant with Thread.isAlive()?

                    // say something if it took more than a minute by default
                    if (job.reportExecution || (job.endTime - job.startTime) >= Job.MIN_NUM_SECONDS) {

                        if (User.isBeepAfterLongJobs())
                            Job.getExtendedUserInterface().beep();
                        System.out.println(job.getInfo());
                    }
                }
            }
        }

        void dispatch(Client client) throws Exception { client.consume(this); }
    }

    static class PrintEvent extends ServerEvent {
//        private final Client client;
        final String s;

        PrintEvent(Snapshot snapshot, Client client, String s) {
            super(snapshot);
//            this.client = client;
            this.s = s;
        }

        public void run() {
        }

        void dispatch(Client client) throws Exception { client.consume(this); }
    }

    static class JobQueueEvent extends ServerEvent {
        final Job.Inform[] jobQueue;

        JobQueueEvent(Snapshot snapshot, Job.Inform[] jobQueue) {
            super(snapshot);
            this.jobQueue = jobQueue;
        }

        public void run() {
            boolean busyCursor = false;
            for (Job.Inform jobInform: jobQueue) {
                if (jobInform.isChangeJobQueuedOrRunning())
                    busyCursor = true;
            }
            JobTree.update(Arrays.asList(jobQueue));
            TopLevel.setBusyCursor(busyCursor);
        }

        void dispatch(Client client) throws Exception { client.consume(this); }
    }
}
