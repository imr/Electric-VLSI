/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Regression.java
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
import com.sun.electric.database.SnapshotReader;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.technology.Technology;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Simple client for regressions.
 */
public class Regression {
    private static final int port = 35742;
    
    public static void main(String[] args) {
        runScript(args[0]);
    }

    public static void runScript(String script) {
        SnapshotReader reader = null;
        EDatabase database = EDatabase.clientDatabase();
        Snapshot currentSnapshot = database.getInitialSnapshot();
        System.out.println("Running " + script);
        
        try {
            System.out.println("Attempting to connect to port " + port + " ...");
            Socket socket = null;
            for (int i = 0; i < 100; i++) {
                try {
                    Thread.currentThread().sleep(20);
                    socket = new Socket((String)null, port);
                } catch (IOException e) {
                } catch (InterruptedException e) {
                }
                if (socket != null)
                    break;
            }
            if (socket == null) {
                System.out.println("Can't connect");
                return;
            }
            reader = new SnapshotReader(new DataInputStream(new BufferedInputStream(socket.getInputStream())), database.getIdManager());
            DataOutputStream clientOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            int protocolVersion = reader.readInt();
            if (protocolVersion != Job.PROTOCOL_VERSION) {
                System.out.println("Client's protocol version " + Job.PROTOCOL_VERSION + " is incompatible with Server's protocol version " + protocolVersion);
                System.exit(1);
            }
            System.out.println("Connected");
            
            EJob ejob = EvalJavaBsh.runScriptJob(script).ejob;
            ejob.serialize(EDatabase.clientDatabase());
            clientOutputStream.writeInt(ejob.jobId);
            clientOutputStream.writeUTF(ejob.jobType.toString());
            clientOutputStream.writeUTF(ejob.jobName);
            clientOutputStream.writeInt(ejob.serializedJob.length);
            clientOutputStream.write(ejob.serializedJob);
            clientOutputStream.flush();
            
            Technology.initAllTechnologies();
            
            for (;;) {
                byte tag = reader.readByte();
                switch (tag) {
                    case 1:
                        currentSnapshot = Snapshot.readSnapshot(reader, currentSnapshot);
//                        System.out.println("Snapshot received");
                        break;
                    case 2:
                        Integer jobId = Integer.valueOf(reader.readInt());
                        String jobName = reader.readString();
                        Job.Type jobType = Job.Type.valueOf(reader.readString());
                        EJob.State newState = EJob.State.valueOf(reader.readString());
                        long timeStamp = reader.readLong();
                        if (newState == EJob.State.WAITING) {
                            boolean hasSerializedJob = reader.readBoolean();
                            if (hasSerializedJob) {
                                byte[] serializedJob = reader.readBytes();
                            }
                        }
                        if (newState == EJob.State.SERVER_DONE) {
                            reader.readBytes();
                        }
//                        System.out.println("Job " + jobId + " terminated " + bytes.length);
                        break;
                    case 3:
                        String str = reader.readString();
                        System.out.print("#" + str);
                        break;
                    default:
                        System.out.println("Bad tag " + tag);
                }
            }
        } catch (IOException e) {
            // reader.in.close();
            reader = null;
            System.out.println("END OF FILE reading from server");
            return;
        }
    }
}
