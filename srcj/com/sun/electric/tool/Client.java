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
import com.sun.electric.database.id.IdReader;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.tool.user.ErrorLogger;

import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 */
public abstract class Client {
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition queueChanged = lock.newCondition();
    private static ServerEvent queueTail = new JobQueueEvent(IdManager.stdIdManager.getInitialSnapshot(), new Job.Inform[0]);

    int connectionId;
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

    public synchronized Job.Key newJobId(boolean isServer, boolean doItOnServer) {
        int jobId = isServer ? ++serverJobId : --clientJobId;
        return new Job.Key(this, jobId, doItOnServer);
    }

    static void putEvent(ServerEvent newEvent) {
        lock.lock();
        try {
            assert queueTail.next == null;
            assert newEvent.next == null;
            if (newEvent.snapshot == null)
                newEvent.snapshot = queueTail.snapshot;
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

    public static abstract class ServerEvent implements Runnable {
        private Snapshot snapshot;
        final long timeStamp;
        ServerEvent next;

        ServerEvent(Snapshot snapshot) {
            this(snapshot, System.currentTimeMillis());
        }

        ServerEvent(Snapshot snapshot, long timeStamp) {
            this.snapshot = snapshot;
            this.timeStamp = timeStamp;
        }

        Snapshot getSnapshot() {
            return snapshot;
        }

        public void run() {
            try {
                show(Job.currentUI);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        abstract void write(IdWriter writer) throws IOException;

        abstract void show(AbstractUserInterface ui);
    }

    static void print(String s) {
        fireServerEvent(new PrintEvent(null, Job.currentUI, s));
    }

    static void fireServerEvent(ServerEvent serverEvent) {
        putEvent(serverEvent);
        if (Job.currentUI != null)
            Job.currentUI.addEvent(serverEvent);
    }

    public static class EJobEvent extends ServerEvent {
        public final EJob ejob;
        public final EJob.State newState;

        EJobEvent(EJob ejob, EJob.State newState) {
            super(ejob.oldSnapshot);
            this.ejob = ejob;
            this.newState = newState;
        }

        EJobEvent(EJob ejob, EJob.State newState, long timeStamp) {
            super(ejob.oldSnapshot, timeStamp);
            this.ejob = ejob;
            this.newState = newState;
        }

        @Override
        void write(IdWriter writer) throws IOException {
            assert newState == EJob.State.SERVER_DONE;
            writer.writeByte((byte)2);
            writer.writeInt(ejob.jobKey.jobId);
            writer.writeString(ejob.jobName);
            writer.writeString(ejob.jobType.toString());
            writer.writeString(newState.toString());
            writer.writeLong(timeStamp);
            writer.writeBoolean(ejob.doItOk);
            if (newState == EJob.State.WAITING) {
                writer.writeBoolean(ejob.serializedJob != null);
                if (ejob.serializedJob != null)
                    writer.writeBytes(ejob.serializedJob);
            }
            if (newState == EJob.State.SERVER_DONE)
                writer.writeBytes(ejob.serializedResult);
        }

        @Override
        void show(AbstractUserInterface ui) {
            assert newState == EJob.State.SERVER_DONE;
            ui.terminateJob(ejob);
        }
    }

    public static class PrintEvent extends ServerEvent {
//        private final Client client;
        public final String s;

        PrintEvent(Snapshot snapshot, Client client, String s) {
            super(snapshot);
//            this.client = client;
            this.s = s;
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writer.writeByte((byte)3);
            writer.writeString(s);
        }

        @Override
        void show(AbstractUserInterface ui) {
            ui.printMessage(s, false);
        }
    }

    public static class SavePrintEvent extends ServerEvent {
//        private final Client client;
        public final String filePath;

        SavePrintEvent(Snapshot snapshot, Client client, String filePath) {
            super(snapshot);
//            this.client = client;
            this.filePath = filePath;
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writer.writeByte((byte)5);
            writer.writeString(filePath != null ? filePath : "");
        }

        @Override
        void show(AbstractUserInterface ui) {
            ui.saveMessages(filePath);
        }
    }

    public static class ShowMessageEvent extends ServerEvent {
        public final String message;
        public final String title;
        public final boolean isError;

        ShowMessageEvent(Snapshot snapshot, Client client, String message, String title, boolean isError) {
            super(snapshot);
            this.message = message;
            this.title = title;
            this.isError = isError;
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writer.writeByte((byte)6);
            writer.writeString(message);
            writer.writeString(title);
            writer.writeBoolean(isError);
        }

        @Override
        void show(AbstractUserInterface ui) {
            if (isError)
                ui.showErrorMessage(message, title);
            else
                ui.showInformationMessage(message, title);
        }
    }

    public static class JobQueueEvent extends ServerEvent {
        public final Job.Inform[] jobQueue;

        JobQueueEvent(Snapshot snapshot, Job.Inform[] jobQueue) {
            super(snapshot);
            this.jobQueue = jobQueue;
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writer.writeByte((byte)4);
            writer.writeInt(jobQueue.length);
            for (Job.Inform j: jobQueue)
                j.write(writer);
        }

        @Override
        void show(AbstractUserInterface ui) {
            ui.showJobQueue(jobQueue);
        }
    }

    public static class StartProgressDialogEvent extends ServerEvent {
        public final String msg;
        public final String filePath;

        StartProgressDialogEvent(Snapshot snapshot, String msg, String filePath) {
            super(snapshot);
            this.msg = msg;
            this.filePath = filePath;
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writer.writeByte((byte)7);
            writer.writeString(msg);
            writer.writeBoolean(filePath != null);
            if (filePath != null)
                writer.writeString(filePath);
        }

        @Override
        void show(AbstractUserInterface ui) {
            ui.startProgressDialog(msg, filePath);
        }
    }

    public static class StopProgressDialogEvent extends ServerEvent {

        StopProgressDialogEvent(Snapshot snapshot) {
            super(snapshot);
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writer.writeByte((byte)8);
        }

        @Override
        void show(AbstractUserInterface ui) {
            ui.stopProgressDialog();
        }
    }

    public static class ProgressValueEvent extends ServerEvent {
        public final int pct;

        ProgressValueEvent(Snapshot snapshot, int pct) {
            super(snapshot);
            this.pct = pct;
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writer.writeByte((byte)9);
            writer.writeInt(pct);
        }

        @Override
        void show(AbstractUserInterface ui) {
            ui.setProgressValue(pct);
        }
    }

    public static class ProgressNoteEvent extends ServerEvent {
        public final String note;

        ProgressNoteEvent(Snapshot snapshot, String note) {
            super(snapshot);
            this.note = note;
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writer.writeByte((byte)10);
            writer.writeBoolean(note != null);
            if (note != null)
                writer.writeString(note);
        }

        @Override
        void show(AbstractUserInterface ui) {
            ui.setProgressNote(note);
        }
    }

    public static class TermLoggingEvent extends ServerEvent {
        final ErrorLogger logger;
        public final boolean explain;
        public final boolean terminate;

        TermLoggingEvent(Snapshot snapshot, ErrorLogger logger, boolean explain, boolean terminate) {
            super(snapshot);
            this.logger = logger;
            this.explain = explain;
            this.terminate = terminate;
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writer.writeByte((byte)11);
            logger.write(writer);
            writer.writeBoolean(explain);
            writer.writeBoolean(terminate);
        }

        @Override
        void show(AbstractUserInterface ui) {
            ui.termLogging(logger, explain, terminate);
        }
    }

    public static class BeepEvent extends ServerEvent {

        BeepEvent(Snapshot snapshot) {
            super(snapshot);
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writer.writeByte((byte)12);
        }

        @Override
        void show(AbstractUserInterface ui) {
            ui.beep();
        }
    }

    static ServerEvent read(IdReader reader, byte tag, Client connection) throws IOException {
        Snapshot snapshot = null;
        long timeStamp = 0;
        switch (tag) {
            case 2:
                int jobId = Integer.valueOf(reader.readInt());
                String jobName = reader.readString();
                Job.Type jobType = Job.Type.valueOf(reader.readString());
                EJob.State newState = EJob.State.valueOf(reader.readString());
                timeStamp = reader.readLong();
                boolean doItOk = reader.readBoolean();
                assert newState == EJob.State.SERVER_DONE;
                byte[] bytes = reader.readBytes();
                EJob ejob = new EJob(connection, jobId, jobType, jobName, null);
                ejob.doItOk = doItOk;
                ejob.serializedResult = bytes;
                return new EJobEvent(ejob, newState, timeStamp);
            case 3:
                String str = reader.readString();
                return new PrintEvent(snapshot, connection, str);
            case 4:
                int jobQueueSize = reader.readInt();
                Job.Inform[] jobInforms = new Job.Inform[jobQueueSize];
                for (int jobIndex = 0; jobIndex < jobQueueSize; jobIndex++)
                    jobInforms[jobIndex] = Job.Inform.read(reader);
                return new JobQueueEvent(snapshot, jobInforms);
            case 5:
                String filePath = reader.readString();
                if (filePath.length() == 0)
                    filePath = null;
                return new SavePrintEvent(snapshot, connection, filePath);
            case 6:
                String message = reader.readString();
                String title = reader.readString();
                boolean isError = reader.readBoolean();
                return new ShowMessageEvent(snapshot, connection, message, title, isError);
            case 7:
                String progressMsg = reader.readString();
                boolean hasFilePath = reader.readBoolean();
                String progressFilePath = hasFilePath ? reader.readString() : null;
                return new StartProgressDialogEvent(snapshot, progressMsg, progressFilePath);
            case 8:
                return new StopProgressDialogEvent(snapshot);
            case 9:
                int pct = reader.readInt();
                return new ProgressValueEvent(snapshot, pct);
            case 10:
                boolean hasNote = reader.readBoolean();
                String note = hasNote ? reader.readString() : null;
                return new ProgressNoteEvent(snapshot, note);
            case 11:
                ErrorLogger logger = ErrorLogger.read(reader);
                boolean explain = reader.readBoolean();
                boolean terminate = reader.readBoolean();
                return new TermLoggingEvent(snapshot, logger, explain, terminate);
            case 12:
                return new BeepEvent(snapshot);
            default:
                throw new AssertionError();
        }
    }
}
