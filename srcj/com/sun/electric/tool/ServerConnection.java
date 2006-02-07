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
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Class for maintaining Connection on Server side.
 */
public class ServerConnection extends Thread {
    
    private static final int STACK_SIZE = 20*1024;
    final int connectionId;
    private final Socket socket;
    SnapshotWriter writer;
    final ConnectionReader reader;
    private Snapshot currentSnapshot = new Snapshot();
    private Snapshot initialSnapshot;
    volatile String jobName;
    private final LinkedBlockingQueue<Object> writeQueue = new LinkedBlockingQueue<Object>();
    
    ServerConnection(int connectionId, Socket socket, Snapshot initialSnapshot) {
        super(null, null, "ServerConnection-" + connectionId, STACK_SIZE);
        this.connectionId = connectionId;
        this.socket = socket;
        this.initialSnapshot = initialSnapshot;
        reader = new ConnectionReader(this);
    }
    
    public void start() {
        super.start();
        reader.start();
    }
    
    void updateSnapshot(Snapshot newSnapshot) {
        writeQueue.add(newSnapshot);
    }
    
    void sendEJobEvent(EJob.Event event) {
        writeQueue.add(event);
    }
    
    void addMessage(String str) {
        writeQueue.add(str);
    }
    
    private void writeSnapshot(Snapshot newSnapshot) throws IOException {
        writer.out.writeByte(1);
        newSnapshot.writeDiffs(writer, currentSnapshot);
        writer.out.flush();
        currentSnapshot = newSnapshot;
    }
    
    private void writeEJobEvent(EJob.Event event) throws IOException {
        EJob ejob = event.getEJob();
        switch (event.newState) {
            case SERVER_DONE:
                if (ejob.connection == this) {
                    writer.out.writeByte(2);
                    writer.out.writeInt(ejob.jobId);
                    writer.out.writeUTF(event.newState.toString());
                    writer.out.writeInt(ejob.serializedResult.length);
                    writer.out.write(ejob.serializedResult);
                    writer.out.flush();
                }
                break;
        }
        
    }
    
    public void run() {
        try {
            writer = new SnapshotWriter(new DataOutputStream(new BufferedOutputStream(socket.getOutputStream())));
            writer.out.writeInt(Job.PROTOCOL_VERSION); 
            writeSnapshot(initialSnapshot);
            initialSnapshot = null;
            for (;;) {
                Object o = writeQueue.take();
                if (o instanceof Snapshot) {
                    writeSnapshot((Snapshot)o);
                } else if (o instanceof EJob.Event) {
                    writeEJobEvent((EJob.Event)o);
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
                for (;;) {
                    int jobId = in.readInt();
                    Job.Type jobType = Job.Type.valueOf(in.readUTF());
                    String jobName = in.readUTF();
                    int len = in.readInt();
                    byte[] bytes = new byte[len];
                    in.readFully(bytes);
                    Job.jobManager.addJob(new EJob(connection, jobId, jobType, jobName, bytes), false);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
