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
import com.sun.electric.database.id.IdReader;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.technology.Technology;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Simple client for regressions.
 */
public class Regression {
    private static final int port = 35742;

    public static void main(String[] args) {
//        writeBshIn(args[0]);
        runScript(null, args[0]);
    }

    private static void writeBshIn(String bshName) {
        String outFile = "bash.in";
        try {
            DataOutputStream out = new DataOutputStream(new FileOutputStream(outFile));
            writeJob(out, bshName);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void runScript(Process process, String script) {
        IdReader reader = null;
        Snapshot currentSnapshot = IdManager.stdIdManager.getInitialSnapshot();
        EDatabase database = EDatabase.theDatabase = new EDatabase(currentSnapshot);
        System.out.println("Running " + script);

        try {
//            System.out.println("Attempting to connect to port " + port + " ...");
//            Socket socket = null;
//            for (int i = 0; i < 100; i++) {
//                try {
//                    Thread.sleep(20);
//                    socket = new Socket((String)null, port);
//                } catch (IOException e) {
//                } catch (InterruptedException e) {
//                }
//                if (socket != null)
//                    break;
//            }
//            if (socket == null) {
//                System.out.println("Can't connect");
//                return;
//            }
//            InputStream inStream = socket.getInputStream();
//            OutputStrean outStream = socket.getOutputStream();
            InputStream inStream = process.getInputStream();
            OutputStream outStream = process.getOutputStream();
            reader = new IdReader(new DataInputStream(new BufferedInputStream(inStream)), database.getIdManager());
            DataOutputStream clientOutputStream = new DataOutputStream(new BufferedOutputStream(outStream));
            int protocolVersion = reader.readInt();
            if (protocolVersion != Job.PROTOCOL_VERSION) {
                System.out.println("Client's protocol version " + Job.PROTOCOL_VERSION + " is incompatible with Server's protocol version " + protocolVersion);
                System.exit(1);
            }
            System.out.println("Connected");

            writeJob(clientOutputStream, script);
            clientOutputStream.flush();

//            Technology.initAllTechnologies();

            for (;;) {
                byte tag = reader.readByte();
                switch (tag) {
                    case 1:
                        currentSnapshot = Snapshot.readSnapshot(reader, currentSnapshot);
//                        System.out.println("Snapshot received");
                        break;
                    case 2:
                        int jobId = Integer.valueOf(reader.readInt());		// ignore jobID
                        String jobName = reader.readString();		// ignore jobName
                        Job.Type jobType = Job.Type.valueOf(reader.readString());		// ignore jobType
                        EJob.State newState = EJob.State.valueOf(reader.readString());
                        long timeStamp = reader.readLong();		// ignore timestamp
                        if (newState == EJob.State.WAITING) {
                            boolean hasSerializedJob = reader.readBoolean();
                            if (hasSerializedJob) {
                                reader.readBytes();		// ignore serializedJob
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
                    case 4:
                        System.out.print("JobQueue");
                        int jobQueueSize = reader.readInt();
                        for (int jobIndex = 0; jobIndex < jobQueueSize; jobIndex++) {
                            Job.Inform jobInform = Job.Inform.read(reader);
                            System.out.print(" " + jobInform);
                        }
                        System.out.println();
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

    private static void writeJob(DataOutputStream clientOutputStream, String script) throws IOException {
        EJob ejob = EvalJavaBsh.runScriptJob(script).ejob;
        ejob.jobKey = new Job.Key(0, 0, true);
        ejob.serialize(EDatabase.clientDatabase());
        clientOutputStream.writeInt(ejob.jobKey.jobId);
        clientOutputStream.writeUTF(ejob.jobType.toString());
        clientOutputStream.writeUTF(ejob.jobName);
        clientOutputStream.writeInt(ejob.serializedJob.length);
        clientOutputStream.write(ejob.serializedJob);
        clientOutputStream.flush();
    }
}
