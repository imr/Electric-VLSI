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

import com.sun.electric.Main;
import com.sun.electric.database.EObjectOutputStream;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.IdReader;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.technology.TechFactory;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;

import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.MessagesStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * Simple client for regressions.
 */
public class Regression {

    public static boolean runScript(Process process, String script) {
        Pref.forbidPreferences();
        IdReader reader = null;
        Snapshot currentSnapshot = IdManager.stdIdManager.getInitialSnapshot();
//        EDatabase database = new EDatabase(currentSnapshot);
//        EDatabase.setClientDatabase(database);
        System.out.println("Running " + script);

        try {
            InputStream inStream = process.getInputStream();
            OutputStream outStream = process.getOutputStream();
            InputStream errStream = process.getErrorStream();
            new ExecProcessReader(errStream).start();
            reader = new IdReader(new DataInputStream(new BufferedInputStream(inStream)), IdManager.stdIdManager);
            int protocolVersion = reader.readInt();
            if (protocolVersion != Job.PROTOCOL_VERSION) {
                System.out.println("Client's protocol version " + Job.PROTOCOL_VERSION + " is incompatible with Server's protocol version " + protocolVersion);
//                for (int i = 0; i < 100; i++)
//                    System.out.print((char)reader.readByte());
//                System.out.println();
                return false;
            }
            int connectionId = reader.readInt();
            System.out.format("%1$tT.%1$tL ", Calendar.getInstance());
            System.out.println("Connected id="+connectionId);

            DataOutputStream clientOutputStream = new DataOutputStream(new BufferedOutputStream(outStream));
            writeServerJobs(clientOutputStream, connectionId, script);
            clientOutputStream.close();

            int curJobId = -1;
            AbstractUserInterface ui = new Main.UserInterfaceDummy();
            ui.patchConnectionId(connectionId);
            boolean passed = true;
            for (;;) {
                byte tag = reader.readByte();
                long timeStamp = reader.readLong();
//                System.out.format("%1$tT.%1$tL->%2$tT.%2$tL %3$2d ", timeStamp, Calendar.getInstance(), tag);
                if (tag == 1) {
                        currentSnapshot = Snapshot.readSnapshot(reader, currentSnapshot);
                        System.out.println("Snapshot received " + currentSnapshot.snapshotId);
//                        database.lock(true);
//                        try {
//                            database.lowLevelSetCanUndoing(true);
//                            database.undo(currentSnapshot);
//                            database.lowLevelSetCanUndoing(false);
//                        } finally {
//                            database.unlock();
//                        }
//                        System.out.format("            ->%1$tT.%1$tL Database updated to snapshot %2$d\n", Calendar.getInstance(), currentSnapshot.snapshotId);
                } else {
                    Client.ServerEvent serverEvent = Client.read(reader, tag, timeStamp, ui, currentSnapshot);
                    if (serverEvent instanceof Client.EJobEvent) {
                        Client.EJobEvent e = (Client.EJobEvent)serverEvent;
                        int jobId = e.jobKey.jobId;
                        assert e.newState == EJob.State.SERVER_DONE;
                        if (jobId > 0) {
                            if (!e.doItOk) {
                                System.out.println("Job " + e.jobName + " failed");
//                                printErrorStream(process);
//                                ui.saveMessages(null);
                                passed = false;
                            }
                            continue;
                        }
                        assert jobId == curJobId;
//                        EJob ejob = new EJob(client, jobId, e.jobType, e.jobName, e.serializedJob);
//                        ejob.serializedResult = e.serializedResult;
//                        Throwable result = ejob.deserializeResult();
//                        assert e.doItOk == (result == null);
                        if (!e.doItOk) {
                            System.out.println("Job " + e.jobName + " exception");
//                            System.out.println(result);
//                            result.printStackTrace(System.out);
//                            printErrorStream(process);
//                            ui.saveMessages(null);
                            passed = false;
                        } else {
                            System.out.println("Job " + jobId + " ok");
                        }
                        switch (jobId) {
                            case -1:
                                curJobId = -2;
//                                job = script.equals("CRASH") ? new CrashJob() : EvalJavaBsh.runScriptJob(script);
//                                job.ejob.jobKey = new Job.Key(connectionId, --curJobId, true);
//                                writeJob(clientOutputStream, job);
                                break;
                            case -2:
                                curJobId = -3;
//                                job = new QuitJob();
//                                job.ejob.jobKey = new Job.Key(connectionId, --curJobId, true);
//                                writeJob(clientOutputStream, job);
                                break;
//                            case -3:
//                                ui.saveMessages(null);
//                                return passed;
                            default:
                        }
                    } else {
                        serverEvent.show(ui);
                        if (serverEvent instanceof Client.ShutdownEvent) {
                            assert curJobId == -3;
                            ui.saveMessages(null);
                            return passed;
                        }
                    }
                }
            }
        } catch (IOException e) {
            reader = null;
            System.out.println("END OF FILE reading from server");
//            printErrorStream(process);
            try {
                Thread.sleep(1000);
                System.out.println("Server exit code="+process.exitValue());
                process.getOutputStream().close();
            } catch (Exception e1) {
                e1.printStackTrace(System.out);
            }
            return false;
        }
    }

    private static void printErrorStream(Process process) {
        try {
//            process.getOutputStream().close();
            InputStream errStream = new BufferedInputStream(process.getErrorStream());
            System.out.println("<StdErr>");
            for (;;) {
                if (errStream.available() == 0) break;
                int c = errStream.read();
                if (c < 0) break;
                System.out.print((char)c);
            }
            System.out.println("</StdErr>");
        } catch (IOException e) {
            e.printStackTrace();
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
//            EditingPreferences.setThreadEditingPreferences(new EditingPreferences(true, database.getTechPool()));
            return true;
       }
    }

    private static class CrashJob extends Job {

        protected CrashJob() {
            super("CrashTest", null, Job.Type.CHANGE, null, null, Job.Priority.USER);
        }

        public boolean doIt() throws JobException {
            String regressionname = "qFourP2-electric-final-jelib";
            String libname = "qFourP2.jelib";
            String cellname = "qFourP1top";
            String rootPath = "../../";
            boolean caching = true;

            String logname = "output/"+libname+"_"+cellname+"_LE_"+(caching ? "C" : "NC")+"-"+Version.getVersion()+".log";

            MessagesStream.getMessagesStream().save(logname);

            Library rootLib = LayoutLib.openLibForRead(rootPath+"data/"+regressionname+"/"+libname);
            ErrorLogger repairLogger = ErrorLogger.newInstance("Repair Libraries");
            for (Iterator it = Library.getLibraries(); it.hasNext(); ) {
                Library lib = (Library)it.next();
                lib.checkAndRepair(true, repairLogger);
            }

            System.out.println("Repair Libraries: " + repairLogger.getNumErrors() + " errors," + repairLogger.getNumWarnings() + " warnings");
            Cell lay = rootLib.findNodeProto(cellname+"{sch}");
            System.out.println("Cell = "+lay);
            return true;
        }
    }

    private static class QuitJob extends Job {
        private QuitJob() {
            super("QuitJob", null, Job.Type.CHANGE, null, null, Job.Priority.USER);
        }

        public boolean doIt() throws JobException {
            Client.fireServerEvent(new Client.ShutdownEvent());
            return true;
       }
    }

    public static void main(String[] args) {
        makeCrashInput(args[0]);
    }

    private static void makeCrashInput(String fileName) {
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)));
            writeServerJobs(out, 0, "CRASH");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeServerJobs(DataOutputStream clientOutputStream, int connectionId, String script) throws IOException {
            EDatabase database = new EDatabase(IdManager.stdIdManager.getInitialEnvironment());
            Job.setUserInterface(new UserInterfaceInitial(database));

            Job job1 = new InitJob();
            job1.ejob.jobKey = new Job.Key(connectionId, -1, true);

            Job job2 = script.equals("CRASH") ? new CrashJob() : EvalJavaBsh.runScriptJob(script);
            job2.ejob.jobKey = new Job.Key(connectionId, -2, true);

            Job job3 = new QuitJob();
            job3.ejob.jobKey = new Job.Key(connectionId, -3, true);

            writeEditingPreferences(clientOutputStream, database);
            writeJob(clientOutputStream, job1);
            writeJob(clientOutputStream, job2);
            writeJob(clientOutputStream, job3);
    }

    private static void writeEditingPreferences(DataOutputStream clientOutputStream, EDatabase database) throws IOException {
        EditingPreferences ep = new EditingPreferences(true, database.getTechPool());
        byte[] serializedEp;
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream out = new EObjectOutputStream(byteStream, database);
            out.writeObject(ep);
            out.flush();
            serializedEp = byteStream.toByteArray();
        } catch (Throwable e) {
            e.printStackTrace();
            return;
        }
        clientOutputStream.writeByte((byte)2);
        clientOutputStream.writeInt(serializedEp.length);
        clientOutputStream.write(serializedEp);
    }

    private static void writeJob(DataOutputStream clientOutputStream, Job job) throws IOException {
        EJob ejob = job.ejob;
        ejob.serialize(EDatabase.clientDatabase());
        clientOutputStream.writeByte((byte)1);
        clientOutputStream.writeInt(ejob.jobKey.jobId);
        clientOutputStream.writeUTF(ejob.jobType.toString());
        clientOutputStream.writeUTF(ejob.jobName);
        clientOutputStream.writeInt(ejob.serializedJob.length);
        clientOutputStream.write(ejob.serializedJob);
        clientOutputStream.flush();
    }

    /**
     * This class is used to read data from an external process.
     * If something does not consume the data, it will fill up the default
     * buffer and deadlock.  This class also redirects data read
     * from the process (the process' output) to another stream,
     * if specified.
     */
    public static class ExecProcessReader extends Thread {

        private InputStream in;
        private char [] buf;

        /**
         * Create a stream reader that will read from the stream, and
         * store the read text into buffer.
         * @param in the input stream
         */
        public ExecProcessReader(InputStream in) {
            this.in = in;
            buf = new char[256];
            setName("ExecProcessReader");
        }

        public void run() {
            try {
                // read from stream
                InputStreamReader reader/*input*/ = new InputStreamReader(in);
 //               BufferedReader reader = new BufferedReader(input);
                int read = 0;
                while ((read = reader.read(buf)) >= 0) {
                    String s = new String(buf, 0, read);
                    Calendar c = Calendar.getInstance();
                    System.err.print(s);
                    System.out.format("%1$tT.%1$tL <err> %2$s </err>\n", c, s);
                }

                reader.close();
 //               input.close();

            } catch (java.io.IOException e) {
                e.printStackTrace();
                e.printStackTrace(System.out);
            }
        }
    }
}
