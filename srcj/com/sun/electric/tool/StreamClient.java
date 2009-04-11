/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ServerConnection.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.tool.Client.ServerEvent;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class for maintaining Connection on Server side.
 */
public class StreamClient extends Client {
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition queueChanged = lock.newCondition();
    private static ServerEvent queueTail = new JobQueueEvent(IdManager.stdIdManager.getInitialSnapshot(), new Job.Inform[0]);

    private final IdWriter writer;
    private Snapshot currentSnapshot = EDatabase.serverDatabase().getInitialSnapshot();
    private final ServerEventDispatcher dispatcher;
    private final ClientReader reader;
    private static final long STACK_SIZE_EVENT = isOSMac()?0:20*(1 << 10);
    private final static int STACK_SIZE_READER = isOSMac()?0:1024;

    StreamClient(int connectionId, InputStream inputStream, OutputStream outputStream) {
        super(connectionId);
//        writer = new ClientWriter(outputStream, initialSnapshot);
        writer = new IdWriter(IdManager.stdIdManager, new DataOutputStream(outputStream));
        dispatcher = new ServerEventDispatcher();
        reader = inputStream != null ? new ClientReader(inputStream) : null;
    }

    void start() { dispatcher.start(); }

    protected void dispatchServerEvent(ServerEvent serverEvent) throws Exception {
    }

    class ServerEventDispatcher extends Thread {
//        private Snapshot currentSnapshot = EDatabase.serverDatabase().getInitialSnapshot();
        private ServerEvent lastEvent = getQueueTail();

        private ServerEventDispatcher() {
            super(null, null, "Dispatcher-" + connectionId, STACK_SIZE_EVENT);
        }

        public void run() {
            try {
                if (reader != null)
                    reader.start();
                writer.writeInt(Job.PROTOCOL_VERSION);
                writeSnapshot(lastEvent.snapshot);
                for (;;) {
                    lock.lock();
                    try {
                        while (lastEvent.next == null)
                            queueChanged.await();
                        lastEvent = lastEvent.next;
                    } finally {
                        lock.unlock();
                    }
                    lastEvent.dispatch(StreamClient.this);
                    writer.flush();
                }
            } catch (InterruptedException e) {
                e.printStackTrace(System.out);
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
    }

    static void addEvent(ServerEvent newEvent) {
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

    static ServerEvent getQueueTail() {
        lock.lock();
        try {
            return queueTail;
        } finally {
            lock.unlock();
        }
    }
    
    protected void consume(EJobEvent e) throws Exception {
        writeEJobEvent(e.ejob, e.newState, e.timeStamp);
    }
    
    protected void consume(PrintEvent e) throws Exception {
        writeString(e.s);
    }
    
    protected void consume(JobQueueEvent e) throws Exception {
        writeJobQueue(e.jobQueue);
    }

    void writeSnapshot(Snapshot newSnapshot) throws IOException {
        writer.writeByte((byte)1);
        newSnapshot.writeDiffs(writer, currentSnapshot);
        currentSnapshot = newSnapshot;
    }

    void writeEJobEvent(EJob ejob, EJob.State newState, long timeStamp) throws IOException {
        if (ejob.newSnapshot != null && ejob.newSnapshot != currentSnapshot) {
            writeSnapshot(ejob.newSnapshot);
        }
        switch (newState) {
            case WAITING:
            case RUNNING:
            case SERVER_DONE:
//                if (ejob.client == StreamClient.this) {
                writer.writeByte((byte)2);
                writer.writeInt(ejob.jobKey.jobId);
                writer.writeString(ejob.jobName);
                writer.writeString(ejob.jobType.toString());
                writer.writeString(newState.toString());
                writer.writeLong(timeStamp);
                if (newState == EJob.State.WAITING) {
                    writer.writeBoolean(ejob.serializedJob != null);
                    if (ejob.serializedJob != null)
                        writer.writeBytes(ejob.serializedJob);
                }
                if (newState == EJob.State.SERVER_DONE)
                    writer.writeBytes(ejob.serializedResult);
//                }
                break;
        }
    }

    void writeString(String s) throws IOException {
        writer.writeByte((byte)3);
        writer.writeString(s);
    }

    void writeJobQueue(Job.Inform[] jobQueue) throws IOException {
        writer.writeByte((byte)4);
        writer.writeInt(jobQueue.length);
        for (Job.Inform j: jobQueue)
            j.write(writer);
    }

    private class ClientReader extends Thread {
        private final DataInputStream in;

        private ClientReader(InputStream inputStream) {
            super(null, null, "ClientReader-" + connectionId, STACK_SIZE_READER);
            in = new DataInputStream(new BufferedInputStream(inputStream));
        }

        public void run() {
            try {
                for (;;) {
                    int jobId = in.readInt();
                    Job.Type jobType = Job.Type.valueOf(in.readUTF());
                    String jobName = in.readUTF();
                    int len = in.readInt();
                    byte[] bytes = new byte[len];
                    in.readFully(bytes);
                    Job.jobManager.addJob(new EJob(StreamClient.this, jobId, jobType, jobName, bytes), false);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
