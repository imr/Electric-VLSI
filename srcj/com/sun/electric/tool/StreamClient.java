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
package com.sun.electric.tool;

import com.sun.electric.database.Snapshot;
import com.sun.electric.database.SnapshotWriter;
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
    private static ServerEvent queueTail = new ServerEvent();
    
    private final SnapshotWriter writer;
    private Snapshot currentSnapshot = Snapshot.EMPTY;
    private Snapshot initialSnapshot;
    private final ServerEventDispatcher dispatcher; 
    private final ClientReader reader;
    
    StreamClient(int connectionId, InputStream inputStream, OutputStream outputStream, Snapshot initialSnapshot) {
        super(connectionId);
//        writer = new ClientWriter(outputStream, initialSnapshot);
        writer = new SnapshotWriter(new DataOutputStream(outputStream));
        this.initialSnapshot = initialSnapshot;
        dispatcher = new ServerEventDispatcher();
        reader = inputStream != null ? new ClientReader(inputStream) : null;
    }
    
    void start() { dispatcher.start(); }
    
    protected void dispatchServerEvent(ServerEvent serverEvent) throws Exception {
    }
    
    class ServerEventDispatcher extends Thread {
        private static final long STACK_SIZE = 20*(1 << 10);
        private Snapshot currentSnapshot = Snapshot.EMPTY;
        private ServerEvent lastEvent = getQueueTail();
    
        private ServerEventDispatcher() {
            super(null, null, "Dispatcher-" + connectionId, STACK_SIZE);
        }
        
        public void run() {
            try {
                if (reader != null)
                    reader.start();
                writer.out.writeInt(Job.PROTOCOL_VERSION);
                writeSnapshot(initialSnapshot, false);
                initialSnapshot = null;
                for (;;) {
                    lock.lock();
                    try {
                        while (lastEvent.next == null)
                            queueChanged.await();
                        lastEvent = lastEvent.next;
                    } finally {
                        lock.unlock();
                    }
                    lastEvent.dispatchOnStreamClient(StreamClient.this);
                    writer.out.flush();
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
    void writeSnapshot(Snapshot newSnapshot, boolean undoRedo) throws IOException {
        writer.out.writeByte(1);
        newSnapshot.writeDiffs(writer, currentSnapshot);
        currentSnapshot = newSnapshot;
    }
    
    void writeEJobEvent(EJob ejob, EJob.State newState, long timeStamp) throws IOException {
        if (ejob.newSnapshot != null && ejob.newSnapshot != currentSnapshot) {
            writeSnapshot(ejob.newSnapshot, ejob.jobType == Job.Type.UNDO);
        }
        switch (newState) {
            case WAITING:
            case RUNNING:
            case SERVER_DONE:
//                if (ejob.client == StreamClient.this) {
                writer.out.writeByte(2);
                writer.out.writeInt(ejob.jobId);
                writer.out.writeUTF(ejob.jobName);
                writer.out.writeUTF(newState.toString());
                writer.out.writeLong(timeStamp);
                if (newState == EJob.State.WAITING) {
                    writer.out.writeBoolean(ejob.serializedJob != null);
                    if (ejob.serializedJob != null) {
                        writer.out.writeInt(ejob.serializedJob.length);
                        writer.out.write(ejob.serializedJob);
                    }
                }
                if (newState == EJob.State.SERVER_DONE) {
                    writer.out.writeInt(ejob.serializedResult.length);
                    writer.out.write(ejob.serializedResult);
                }
//                }
                break;
        }
    }
    
    void writeString(String s) throws IOException {
        writer.out.writeByte(3);
        writer.out.writeUTF(s);
    }
    
    private class ClientReader extends Thread {
        private final static int STACK_SIZE = 1024;
        private final DataInputStream in;
        
        private ClientReader(InputStream inputStream) {
            super(null, null, "ClientReader-" + connectionId, STACK_SIZE);
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
