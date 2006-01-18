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

import com.sun.electric.database.EObjectOutputStream;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.SnapshotWriter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

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
        
        ServerConnection(int connectionId, Socket socket) {
            super(null, null, "ServerConnection-" + connectionId, STACK_SIZE);
            this.connectionId = connectionId;
            this.socket = socket;
            newSnapshot = currentSnapshot = new Snapshot();
            reader = new ConnectionReader("ConndectionReader-" + connectionId, socket);
        }
        
        synchronized void updateSnapshot(Snapshot newSnapshot) {
            if (this.result != null) return;
            this.newSnapshot = newSnapshot;
            notify();
        }
        
        synchronized void sendTerminateJob(String jobName, byte[] result) {
            assert this.result == null;
            assert result != null;
            this.result = result;
            jobName = jobName != null ? jobName : "Unknown";
            notify();
        }  
        
       public void run() {
            try {
                SnapshotWriter writer = new SnapshotWriter(new DataOutputStream(new BufferedOutputStream(socket.getOutputStream())));
                for (;;) {
                    Snapshot newSnapshot;
                    synchronized (this) {
                        while (this.newSnapshot == currentSnapshot && result == null)
                            wait();
                        newSnapshot = this.newSnapshot;
                    }
                    if (newSnapshot != currentSnapshot) {
                        writer.out.writeByte(1);
                        newSnapshot.writeDiffs(writer, currentSnapshot);
                        writer.out.flush();
                        currentSnapshot = newSnapshot;
                    }
                    if (result != null) {
                        writer.out.writeByte(2);
                        writer.out.writeUTF(jobName);
                        writer.out.writeInt(result.length);
                        writer.out.write(result);
                        writer.out.flush();
                        result = null;
                        reader.enable();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace(System.out);
            } catch (InterruptedException e) {
                e.printStackTrace(System.out);
            }
        }

    static class ConnectionReader extends Thread {
        private final static int STACK_SIZE = 1024;
        private final Socket socket;
        byte[] bytes;
        
        ConnectionReader(String name, Socket socket) {
            super(null, null, name, STACK_SIZE);
            this.socket = socket;
        }
        
        byte[] getBytes() {
            if (bytes == null) return null;
            byte[] bytes = this.bytes;
            notify();
            return bytes;
        }
        
        synchronized void enable() {
            assert this.bytes != null;
            this.bytes = null;
            notify();
        }
        
        public void run() {
            try {
                DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                for (;;) {
                    int len = in.readInt();
                    byte[] bytes = new byte[len];
                    in.readFully(bytes);
                    this.bytes = bytes;
                    synchronized (Job.databaseChangesThread) {
                        Job.databaseChangesThread.notify();
                    }
                    synchronized (this) {
                        while (this.bytes != null) {
                            try {
                                wait();
                            } catch (InterruptedException e) {}
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
