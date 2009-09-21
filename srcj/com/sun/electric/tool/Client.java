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
    private static ServerEvent queueTail = new JobQueueEvent(new Job.Inform[0]);
    static { queueTail.snapshot = IdManager.stdIdManager.getInitialSnapshot(); }

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

    public int getConnectionId() {
        return connectionId;
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
        private long timeStamp;
        private volatile ServerEvent next;

        ServerEvent() {
            this(null, System.currentTimeMillis());
        }

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

        long getTimeStamp() {
            return timeStamp;
        }

        ServerEvent getNext() {
            return next;
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

        void writeHeader(IdWriter writer, int tag) throws IOException {
            writer.writeByte((byte)tag);
            writer.writeLong(timeStamp);
        }
    }

    static void fireServerEvent(ServerEvent serverEvent) {
        Client.putEvent(serverEvent);
//        if (Job.currentUI != null)
//            Job.currentUI.addEvent(serverEvent);
    }

    public static class EJobEvent extends ServerEvent {
        public final Job.Key jobKey;
        public final String jobName;
        public final Tool tool;
        public final Job.Type jobType;
        public final byte[] serializedJob;
        public final boolean doItOk;
        public final byte[] serializedResult;
        public final EJob.State newState;

        EJobEvent(Job.Key jobKey, String jobName, Tool tool, Job.Type jobType, byte[] serializedJob,
                boolean doItOk, byte[] serializedResult, Snapshot newSnapshot, EJob.State newState) {
            super(newSnapshot);
            assert jobKey != null;
            this.jobKey = jobKey;
            this.jobName = jobName;
            this.tool = tool;
            this.jobType = jobType;
            this.serializedJob = serializedJob;
            this.doItOk = doItOk;
            this.serializedResult = serializedResult;
            this.newState = newState;
            assert newState == EJob.State.SERVER_DONE;
        }

        EJobEvent(Job.Key jobKey, String jobName, Tool tool, Job.Type jobType, byte[] serializedJob,
                boolean doItOk, byte[] serializedResult, Snapshot newSnapshot, EJob.State newState, long timeStamp) {
            super(newSnapshot, timeStamp);
            assert jobKey != null;
            this.jobKey = jobKey;
            this.jobName = jobName;
            this.tool = tool;
            this.jobType = jobType;
            this.serializedJob = serializedJob;
            this.doItOk = doItOk;
            this.serializedResult = serializedResult;
            this.newState = newState;
            assert newState == EJob.State.SERVER_DONE;
        }

        @Override
        void write(IdWriter writer) throws IOException {
            assert newState == EJob.State.SERVER_DONE;
            writeHeader(writer, 2);
            jobKey.write(writer);
            writer.writeString(jobName);
            writer.writeBoolean(tool != null);
            if (tool != null)
                writer.writeTool(tool);
            writer.writeString(jobType.toString());
            writer.writeBoolean(doItOk);
            writer.writeBoolean(serializedJob != null);
            if (serializedJob != null)
                writer.writeBytes(serializedJob);
            writer.writeBytes(serializedResult);
        }

        @Override
        void show(AbstractUserInterface ui) {
            assert newState == EJob.State.SERVER_DONE;
            ui.terminateJob(jobKey, jobName, tool, jobType, serializedJob,
                    doItOk, serializedResult, getSnapshot());
        }
    }

    public static class PrintEvent extends ServerEvent {
//        private final Client client;
        public final String s;

        PrintEvent(Client client, String s) {
//            this.client = client;
            this.s = s;
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writeHeader(writer, 3);
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

        SavePrintEvent(Client client, String filePath) {
//            this.client = client;
            this.filePath = filePath;
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writeHeader(writer, 5);
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

        ShowMessageEvent(Client client, String message, String title, boolean isError) {
            this.message = message;
            this.title = title;
            this.isError = isError;
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writeHeader(writer, 6);
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

        JobQueueEvent(Job.Inform[] jobQueue) {
            this.jobQueue = jobQueue;
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writeHeader(writer, 4);
            writer.writeInt(jobQueue.length);
            for (Job.Inform j: jobQueue)
                j.write(writer);
        }

        @Override
        void show(AbstractUserInterface ui) {
            ui.showServerJobQueue(jobQueue);
        }
    }

    public static class StartProgressDialogEvent extends ServerEvent {
        public final String msg;
        public final String filePath;

        StartProgressDialogEvent(String msg, String filePath) {
            this.msg = msg;
            this.filePath = filePath;
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writeHeader(writer, 7);
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

        StopProgressDialogEvent() {
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writeHeader(writer, 8);
        }

        @Override
        void show(AbstractUserInterface ui) {
            ui.stopProgressDialog();
        }
    }

    public static class ProgressValueEvent extends ServerEvent {
        public final int pct;

        ProgressValueEvent(int pct) {
            this.pct = pct;
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writeHeader(writer, 9);
            writer.writeInt(pct);
        }

        @Override
        void show(AbstractUserInterface ui) {
            ui.setProgressValue(pct);
        }
    }

    public static class ProgressNoteEvent extends ServerEvent {
        public final String note;

        ProgressNoteEvent(String note) {
            this.note = note;
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writeHeader(writer, 10);
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

        TermLoggingEvent(ErrorLogger logger, boolean explain, boolean terminate) {
            this.logger = logger;
            this.explain = explain;
            this.terminate = terminate;
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writeHeader(writer, 11);
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

        BeepEvent() {
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writeHeader(writer, 12);
        }

        @Override
        void show(AbstractUserInterface ui) {
            ui.beep();
        }
    }

    public static class ShutdownEvent extends ServerEvent {

        ShutdownEvent() {
        }

        @Override
        void write(IdWriter writer) throws IOException {
            writeHeader(writer, 13);
        }

        @Override
        void show(AbstractUserInterface ui) {
            ui.printMessage("SHUTDOWN !", true);
        }
    }

    static ServerEvent read(IdReader reader, byte tag, long timeStamp, Client connection, Snapshot snapshot) throws IOException {
        ServerEvent event;
        switch (tag) {
            case 2:
                Job.Key jobKey = Job.Key.read(reader);
                String jobName = reader.readString();
                Tool tool = null;
                if (reader.readBoolean())
                    tool = reader.readTool();
                Job.Type jobType = Job.Type.valueOf(reader.readString());
                boolean doItOk = reader.readBoolean();
                EJob.State newState = EJob.State.SERVER_DONE;
                byte[] serializedJob = null;
                if (reader.readBoolean())
                    serializedJob = reader.readBytes();
                byte[] serializedResult = reader.readBytes();
                EJob ejob = new EJob(connection, jobKey.jobId, jobType, jobName, serializedJob);
                ejob.state = newState;
                ejob.doItOk = doItOk;
                ejob.serializedResult = serializedResult;
                event = new EJobEvent(jobKey, jobName, tool, jobType, serializedJob,
                        doItOk, serializedResult, snapshot, newState, timeStamp);
                break;
            case 3:
                String str = reader.readString();
                event = new PrintEvent(connection, str);
                break;
            case 4:
                int jobQueueSize = reader.readInt();
                Job.Inform[] jobInforms = new Job.Inform[jobQueueSize];
                for (int jobIndex = 0; jobIndex < jobQueueSize; jobIndex++)
                    jobInforms[jobIndex] = Job.Inform.read(reader);
                event = new JobQueueEvent(jobInforms);
                break;
            case 5:
                String filePath = reader.readString();
                if (filePath.length() == 0)
                    filePath = null;
                event = new SavePrintEvent(connection, filePath);
                break;
            case 6:
                String message = reader.readString();
                String title = reader.readString();
                boolean isError = reader.readBoolean();
                event = new ShowMessageEvent(connection, message, title, isError);
                break;
            case 7:
                String progressMsg = reader.readString();
                boolean hasFilePath = reader.readBoolean();
                String progressFilePath = hasFilePath ? reader.readString() : null;
                event = new StartProgressDialogEvent(progressMsg, progressFilePath);
                break;
            case 8:
                event = new StopProgressDialogEvent();
                break;
            case 9:
                int pct = reader.readInt();
                event = new ProgressValueEvent(pct);
                break;
            case 10:
                boolean hasNote = reader.readBoolean();
                String note = hasNote ? reader.readString() : null;
                event = new ProgressNoteEvent(note);
                break;
            case 11:
                ErrorLogger logger = ErrorLogger.read(reader);
                boolean explain = reader.readBoolean();
                boolean terminate = reader.readBoolean();
                event = new TermLoggingEvent(logger, explain, terminate);
                break;
            case 12:
                event = new BeepEvent();
                break;
            case 13:
                event = new ShutdownEvent();
                break;
            default:
                System.err.println("Unknown tag="+tag);
                for (int i = 0; i < 20; i++) {
                    char c = (char)reader.readByte();
                    System.err.print(" " + Integer.toHexString(c) + "(" + c + ")");
                }
                System.err.println();
                throw new AssertionError();
        }
        event.timeStamp = timeStamp;
        event.snapshot = snapshot;
        return event;
    }
}
