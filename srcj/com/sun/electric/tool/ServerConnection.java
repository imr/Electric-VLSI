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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Class for maintaining Connection on Server side.
 */
public class ServerConnection extends Thread {
    
    private static final int STACK_SIZE = 20*1024;
    final int connectionId;
    private final Socket socket;
    final ConnectionReader reader;
    private Snapshot currentSnapshot;
    private volatile Snapshot newSnapshot;
    volatile String jobName;
    private volatile byte[] result;
    private final ArrayList<Object> writeQueue = new ArrayList<Object>();
    private final ArrayList<ReceivedJob> receivedJobs = new ArrayList<ReceivedJob>();
    
    ServerConnection(int connectionId, Socket socket) {
        super(null, null, "ServerConnection-" + connectionId, STACK_SIZE);
        this.connectionId = connectionId;
        this.socket = socket;
        newSnapshot = currentSnapshot = new Snapshot();
        reader = new ConnectionReader(this);
    }
    
    public void start() {
        super.start();
        reader.start();
    }
    
    synchronized void updateSnapshot(Snapshot newSnapshot) {
        if (writeQueue.isEmpty())
            notify();
        writeQueue.add(newSnapshot);
    }
    
    synchronized void sendTerminateJob(int jobId, byte[] result) {
        if (writeQueue.isEmpty())
            notify();
        writeQueue.add(new Integer(jobId));
        writeQueue.add(result);
    }
    
    synchronized void addMessage(String str) {
        if (writeQueue.isEmpty())
            notify();
        writeQueue.add(str);
        
    }
    
    synchronized ReceivedJob peekJob() {
        return receivedJobs.isEmpty() ? null : receivedJobs.get(0);
    }
    
    synchronized void addJob(ReceivedJob rj) {
        if (receivedJobs.isEmpty()) {
            synchronized (Job.databaseChangesMutex) {
                Job.databaseChangesMutex.notify();
            }
        }
        receivedJobs.add(rj);
    }

    synchronized ReceivedJob getJob() {
        if (receivedJobs.isEmpty()) return null;
        return receivedJobs.remove(0);
    }
    
    public void run() {
        try {
            SnapshotWriter writer = new SnapshotWriter(new DataOutputStream(new BufferedOutputStream(socket.getOutputStream())));
            for (;;) {
                Object o;
                synchronized (this) {
                    while (writeQueue.isEmpty())
                        wait();
                    o = writeQueue.remove(0);
                }
                if (o instanceof Snapshot) {
                    Snapshot newSnapshot = (Snapshot)o;
                    writer.out.writeByte(1);
                    newSnapshot.writeDiffs(writer, currentSnapshot);
                    writer.out.flush();
                    currentSnapshot = newSnapshot;
                } else if (o instanceof Integer) {
                    int jobId  = ((Integer)o).intValue();
                    writer.out.writeByte(2);
                    writer.out.writeInt(jobId);
                } else if (o instanceof byte[]) {
                    byte[] result = (byte[])o;
                    writer.out.writeInt(result.length);
                    writer.out.write(result);
                    writer.out.flush();
                } else if (o instanceof String) {
                    String str = (String)o;
                    writer.out.writeByte(3);
                    writer.out.writeUTF(str);
                    writer.out.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        } catch (InterruptedException e) {
            e.printStackTrace(System.out);
        }
    }
    
    static class ReceivedJob {
        final ServerConnection connection;
        final int jobId;
        final byte[] bytes;

        ReceivedJob(ServerConnection connection, int jobId, byte[] bytes) {
            this.connection = connection;
            this.jobId = jobId;
            this.bytes = bytes;
        }
    }
    
    private static class ConnectionReader extends Thread {
        private final static int STACK_SIZE = 1024;
        private final ServerConnection connection;
        
        private ConnectionReader(ServerConnection connection) {
            super(null, null, "ConnectionReader-" + connection.connectionId, STACK_SIZE);
            this.connection = connection;
        }
        
        public void run() {
            try {
                DataInputStream in = new DataInputStream(new BufferedInputStream(connection.socket.getInputStream()));
                int numStarted = 0;
                for (;;) {
                    int len = in.readInt();
                    byte[] bytes = new byte[len];
                    in.readFully(bytes);
                    connection.addJob(new ReceivedJob(connection, ++numStarted, bytes));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
