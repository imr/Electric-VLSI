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
import com.sun.electric.database.id.IdManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 */
public abstract class Client {
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition queueChanged = lock.newCondition();
    private static ServerEvent queueTail = new JobQueueEvent(IdManager.stdIdManager.getInitialSnapshot(), new Job.Inform[0]);

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

    static void putEvent(ServerEvent newEvent) {
        lock.lock();
        try {
            assert queueTail.next == null;
            assert newEvent.next == null;
            queueTail = queueTail.next = newEvent;
            queueChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    static ServerEvent getEvent(ServerEvent lastEvent) throws InterruptedException {
        lock.lock();
        try {
            while (lastEvent.next == null)
                queueChanged.await();
            return lastEvent.next;
        } finally {
            lock.unlock();
        }
    }

    static ServerEvent getQueueTail() {
        lock.lock();
        try {
            return queueTail;
        } finally {
            lock.unlock();
        }
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

        public void run() {
            try {
                dispatch(Job.currentUI);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
            jobs.add(j.getInform());
        }
        fireServerEvent(new JobQueueEvent(snapshot, jobs.toArray(new Job.Inform[jobs.size()])));
    }

    private static void fireServerEvent(ServerEvent serverEvent) {
        if (Job.currentUI != null)
            Job.currentUI.addEvent(serverEvent);
        StreamClient.putEvent(serverEvent);
    }

    public static class EJobEvent extends ServerEvent {
        public final EJob ejob;
        public final EJob.State newState;

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

        void dispatch(Client client) throws Exception { client.consume(this); }
    }

    public static class PrintEvent extends ServerEvent {
//        private final Client client;
        public final String s;

        PrintEvent(Snapshot snapshot, Client client, String s) {
            super(snapshot);
//            this.client = client;
            this.s = s;
        }

        void dispatch(Client client) throws Exception { client.consume(this); }
    }

    public static class JobQueueEvent extends ServerEvent {
        public final Job.Inform[] jobQueue;

        JobQueueEvent(Snapshot snapshot, Job.Inform[] jobQueue) {
            super(snapshot);
            this.jobQueue = jobQueue;
        }

        void dispatch(Client client) throws Exception { client.consume(this); }
    }
}
