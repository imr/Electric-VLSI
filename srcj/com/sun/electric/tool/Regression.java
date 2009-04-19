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
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.variable.EvalJavaBsh;

import com.sun.electric.technology.TechFactory;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;

/**
 * Simple client for regressions.
 */
public class Regression {

    public static boolean runScript(Process process, String script) {
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
            int connectionId = reader.readInt();
            System.out.println("Connected");

            int curJobId = 0;
            Job job = new InitJob();
            job.ejob.jobKey = new Job.Key(connectionId, ++curJobId, true);
            writeJob(clientOutputStream, job);

//            Technology.initAllTechnologies();

            PrintWriter printWriter = null;
            for (;;) {
                byte tag = reader.readByte();
                switch (tag) {
                    case 1:
                        currentSnapshot = Snapshot.readSnapshot(reader, currentSnapshot);
//                        System.out.println("Snapshot received");
                        break;
                    case 2:
                        int jobId = Integer.valueOf(reader.readInt());		// ignore jobID
                        assert jobId == curJobId;
                        String jobName = reader.readString();		// ignore jobName
                        Job.Type jobType = Job.Type.valueOf(reader.readString());		// ignore jobType
                        EJob.State newState = EJob.State.valueOf(reader.readString());
                        long timeStamp = reader.readLong();		// ignore timestamp
                        assert newState == EJob.State.SERVER_DONE;
                        job.ejob.serializedResult = reader.readBytes();
                        Throwable result = job.ejob.deserializeResult();
                        if (result != null) {
                            System.out.println("Job " + jobName + " result:");
                            System.out.println(result);
                            printErrorStream(process);
                            return false;
                        }
                        switch (jobId) {
                            case 1:
                                job = EvalJavaBsh.runScriptJob(script);
                                job.ejob.jobKey = new Job.Key(connectionId, ++curJobId, true);
                                writeJob(clientOutputStream, job);
                                break;
                            default:
                                job = job;
                                return true;
                        }
                        break;
                    case 3:
                        String str = reader.readString();
                        System.out.print("#" + str);
                        if (printWriter != null)
                            printWriter.print(str);
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
                    case 5:
                        String filePath = reader.readString();
                        if (printWriter != null) {
                            printWriter.close();
                            printWriter = null;
                        }
                        if (filePath.length() > 0)
                            printWriter = new PrintWriter(new BufferedWriter(new FileWriter(filePath)));
                        System.out.println("Save messages to " + filePath);
                        break;
                    case 6:
                        String message = reader.readString();
                        String title = reader.readString();
                        boolean isError = reader.readBoolean();
                        System.out.println((isError ? "Error" : "Inform") + "Message " + message);
                        break;
                    default:
                        System.out.println("Bad tag " + tag);
                }
            }
        } catch (IOException e) {
            reader = null;
            System.out.println("END OF FILE reading from server");
            return false;
        }
    }

    private static void printErrorStream(Process process) throws IOException {
        process.getOutputStream().close();
        InputStream errStream = new BufferedInputStream(process.getErrorStream());
        System.out.println("StdErr:");
        for (;;) {
            if (errStream.available() == 0) break;
            int c = errStream.read();
            if (c < 0) break;
            System.out.print((char)c);
        }
    }

    private static class InitJob extends Job {
        private InitJob() {
            super("InitJob", null, Job.Type.CHANGE, null, null, Job.Priority.USER);
        }

        public boolean doIt() throws JobException {
            database.setToolSettings((Setting.RootGroup)ToolSettings.getToolSettings(""));
            assert database.getGeneric() == null;
            Generic generic = Generic.newInstance(database.getIdManager());
            database.addTech(generic);
            for (TechFactory techFactory: TechFactory.getKnownTechs("").values()) {
                Map<TechFactory.Param,Object> paramValues = Collections.emptyMap();
                Technology tech = techFactory.newInstance(generic, paramValues);
                if (tech != null)
                    database.addTech(tech);
            }
            return true;
       }
    }

    private static void writeJob(DataOutputStream clientOutputStream, Job job) throws IOException {
        EJob ejob = job.ejob;
        ejob.serialize(EDatabase.clientDatabase());
        clientOutputStream.writeInt(ejob.jobKey.jobId);
        clientOutputStream.writeUTF(ejob.jobType.toString());
        clientOutputStream.writeUTF(ejob.jobName);
        clientOutputStream.writeInt(ejob.serializedJob.length);
        clientOutputStream.write(ejob.serializedJob);
        clientOutputStream.flush();
    }
}
