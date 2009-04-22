/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ClientJobManager.java
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
import com.sun.electric.tool.user.ui.TopLevel;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 *
 */
class ClientJobManager extends JobManager {
    private static final String CLASS_NAME = Job.class.getName();
    private static final Logger logger = Logger.getLogger("com.sun.electric.tool.job");

    private Job.Inform[] serverJobQueue = {};
    /** started jobs */ private final ArrayList<EJob> serverJobs = new ArrayList<EJob>();
    /** waiting jobs */ private final ArrayList<EJob> clientJobs = new ArrayList<EJob>();
    /** stream for cleint read Snapshots. */    private final IdReader reader;
    /** stream for cleint to send Jobs. */      private final DataOutputStream clientOutputStream;
    /** Process that launched this. */          private final Process process;
    /** Count of started Jobs. */               private static int numStarted;
//    private volatile boolean jobTreeChanged;
//    private static Job clientJob;

    /** Creates a new instance of ClientJobManager */
    public ClientJobManager(String serverMachineName, int serverPort) throws IOException {
        process = null;
        System.out.println("Attempting to connect to port " + serverPort + " ...");
        Socket socket = new Socket(serverMachineName, serverPort);
        reader = new IdReader(new DataInputStream(new BufferedInputStream(socket.getInputStream())), IdManager.stdIdManager);
        clientOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    public ClientJobManager(Process process) throws IOException {
        this.process = process;
        System.out.println("Attempting to connect to server subprocess ...");
        reader = new IdReader(new DataInputStream(new BufferedInputStream(process.getInputStream())), IdManager.stdIdManager);
        clientOutputStream = new DataOutputStream(new BufferedOutputStream(process.getOutputStream()));
    }

    void writeEJob(EJob ejob) throws IOException {
        clientOutputStream.writeByte((byte)1);
        clientOutputStream.writeInt(ejob.jobKey.jobId);
        clientOutputStream.writeUTF(ejob.jobType.toString());
        clientOutputStream.writeUTF(ejob.jobName);
        clientOutputStream.writeInt(ejob.serializedJob.length);
        clientOutputStream.write(ejob.serializedJob);
        clientOutputStream.flush();
    }

    public void runLoop(final Job initialJob) {
        logger.entering(CLASS_NAME, "clinetLoop");
        Snapshot oldSnapshot = EDatabase.clientDatabase().getInitialSnapshot();
        Snapshot currentSnapshot = EDatabase.clientDatabase().backup();
        assert currentSnapshot == oldSnapshot;
        try {
            int protocolVersion = reader.readInt();
            if (protocolVersion != Job.PROTOCOL_VERSION) {
                System.out.println("Client's protocol version " + Job.PROTOCOL_VERSION + " is incompatible with Server's protocol version " + protocolVersion);
                System.exit(1);
            }
            Job.currentUI.connectionId = reader.readInt();
            System.out.println("Connected");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                initialJob.startJob();
            }
        });
//        logger.logp(Level.FINER, CLASS_NAME, "clientLoop", "initTechnologies begin");
//        logger.logp(Level.FINER, CLASS_NAME, "clientLoop", "initTechnologies end");
//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//                // remove the splash screen
//                //if (sw != null) sw.removeNotify();
//                logger.entering(CLASS_NAME, "InitializeWindows");
//                TopLevel.InitializeWindows();
//                WindowFrame.wantToOpenCurrentLibrary(true, null);
//                logger.exiting(CLASS_NAME, "InitializeWindows");
//            }
//        });

//        boolean firstSnapshot = false;
        for (;;) {
            try {
                logger.logp(Level.FINEST, CLASS_NAME, "clientLoop", "readTag");
                byte tag = reader.readByte();
                if (tag == 1) {
//                    logger.logp(Level.FINER, CLASS_NAME, "clientLoop", "readSnapshot begin {0}", Integer.valueOf(clientFifo.numPut));
                    currentSnapshot = Snapshot.readSnapshot(reader, currentSnapshot);
//                    logger.logp(Level.FINER, CLASS_NAME, "clientLoop", "readSnapshot end");
//                    if (!firstSnapshot) {
                        SwingUtilities.invokeLater(new SnapshotDatabaseChangeRun(clientSnapshot, currentSnapshot));
//                        if (Job.currentUI != null)
//                            Job.getExtendedUserInterface().showSnapshot(currentSnapshot, true);
                        clientSnapshot = currentSnapshot;
//                        firstSnapshot = true;
//                    }
                } else {
                    Client.ServerEvent serverEvent = Client.read(reader, tag, Job.currentUI);
                    if (serverEvent instanceof Client.EJobEvent) {
                        Client.EJobEvent ejobEvent = (Client.EJobEvent)serverEvent;
//                        logger.logp(Level.FINER, CLASS_NAME, "clientLoop", "readResult begin {0}", Integer.valueOf(clientFifo.numPut));
                        EJob ejob = ejobEvent.ejob;
                        assert ejob.state == ejobEvent.newState;
                        assert ejob.state == EJob.State.SERVER_DONE;
                        ejob.oldSnapshot = oldSnapshot;
                        ejob.newSnapshot = currentSnapshot;
                        oldSnapshot = currentSnapshot;
//                        long timeStamp = reader.readLong();
//                        if (ejob.state == EJob.State.WAITING) {
//                            boolean hasSerializedJob = reader.readBoolean();
//                            if (hasSerializedJob) {
//                                ejob.serializedJob = reader.readBytes();
//                            }
//                        }
//                        if (ejob.state == EJob.State.SERVER_DONE) {
//                            ejob.serializedResult = reader.readBytes();
//                        }
                        Job.currentUI.addEvent(ejobEvent);
                        logger.logp(Level.FINER, CLASS_NAME, "clientLoop", "readResult end {0}", ejob.jobKey.jobId);
                    } else if (serverEvent instanceof Client.JobQueueEvent) {
                        serverJobQueue = ((Client.JobQueueEvent)serverEvent).jobQueue;
                        showJobQueue();
                    } else {
                        Job.currentUI.addEvent(serverEvent);
                    }
                }
            } catch (IOException e) {
                // reader.in.close();
//                reader = null;
                logger.logp(Level.INFO, CLASS_NAME, "clientLoop", "failed", e);
                System.out.println("END OF FILE reading from server");
                if (process != null)
                    printErrorStream(process);
                return;
            }
        }
    }

    private static void printErrorStream(Process process) {
        try {
            process.getOutputStream().close();
            InputStream errStream = new BufferedInputStream(process.getErrorStream());
            System.err.println("StdErr:");
            for (;;) {
                if (errStream.available() == 0) break;
                int c = errStream.read();
                if (c < 0) break;
                System.err.print((char)c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Add job to list of jobs */
    void addJob(EJob ejob, boolean onMySnapshot) {
        assert SwingUtilities.isEventDispatchThread();
        if (ejob.jobType == Job.Type.EXAMINE) {
            if (onMySnapshot)
                clientJobs.add(0, ejob);
            else
                clientJobs.add(ejob);
            showJobQueue();
        } else {
            serverJobs.add(ejob);
            try {
                writeEJob(ejob);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        jobTreeChanged = true;
//        SwingUtilities.invokeLater(clientInvoke);
//        Job.currentUI.invokeLaterBusyCursor(isChangeJobQueuedOrRunning()); // Not here !!!!
        SwingUtilities.invokeLater(new Runnable() { public void run() { TopLevel.setBusyCursor(isChangeJobQueuedOrRunning()); }});
        return;
    }

    /** Remove job from list of jobs */
    void removeJob(Job j) { // synchronization !!!
        if (j.started) {
            for (Iterator<EJob> it = serverJobs.iterator(); it.hasNext(); ) {
                EJob ejob = it.next();
                if (ejob.getJob() == j) {
                    it.remove();
                }
            }
        } else {
            for (Iterator<EJob> it = clientJobs.iterator(); it.hasNext(); ) {
                EJob ejob = it.next();
                if (ejob.getJob() == j) {
                    it.remove();
                }
            }
        }
        //System.out.println("Removed Job "+j+", index was "+index+", numStarted now="+numStarted+", allJobs="+allJobs.size());
//        jobTreeChanged = true;
//        SwingUtilities.invokeLater(clientInvoke);
    }

    EJob selectEJob(EJob finishedEJob) { return null; }

    void setProgress(EJob ejob, String progress) {
        ejob.progress = progress;
    }

    private void showJobQueue() {
        Job.Inform[] jobQueue;
        lock();
        try {
            jobQueue =  new Job.Inform[serverJobQueue.length + clientJobs.size()];
            System.arraycopy(serverJobQueue, 0, jobQueue, 0, serverJobQueue.length);
            for (int i = 0; i < clientJobs.size(); i++)
                jobQueue[serverJobQueue.length + i] = clientJobs.get(i).getJob().getInform();
        } finally {
            unlock();
        }
        Job.currentUI.addEvent(new Client.JobQueueEvent(clientSnapshot, jobQueue));
    }

    private boolean isChangeJobQueuedOrRunning() { // synchronization !!!
        for (EJob ejob: serverJobs) {
            Job job = ejob.getJob();
            if (job != null && job.finished) continue;
            if (ejob.jobType == Job.Type.CHANGE) return true;
        }
        for (EJob ejob: clientJobs) {
            if (ejob.jobType == Job.Type.CHANGE) return true;
        }
        return false;
    }

    /** get all jobs iterator */
    Iterator<Job> getAllJobs() { // synchronization !!!
        ArrayList<Job> jobsList = new ArrayList<Job>();
        for (EJob ejob: serverJobs) {
            Job job = ejob.getJob();
            if (job != null)
                jobsList.add(job);
        }
        for (EJob ejob: clientJobs) {
            Job job = ejob.getJob();
            if (job != null)
                jobsList.add(job);
        }
        return jobsList.iterator();
    }

    List<Job.Inform> getAllJobInforms() {
        return Collections.emptyList();
    }

//    private class FIFO {
//        private final String CLASS_NAME = ClientJobManager.CLASS_NAME + ".FIFO";
//        private final ArrayList<Client.ServerEvent> queueF = new ArrayList<Client.ServerEvent>();
//        private final ArrayList<Client.ServerEvent> queueT = new ArrayList<Client.ServerEvent>();
//        private boolean getC = false;
//        private int getIndex = 0;
//        private int numGet;
//        private int numPut;
//
//        private synchronized void put(Client.ServerEvent o) {
//            logger.logp(Level.FINEST, CLASS_NAME, "put", "ENTRY");
////            ArrayList<Client.ServerEvent> thisQ;
//            ArrayList<Client.ServerEvent> thatQ;
//            if (getC) {
////                thisQ = queueT;
//                thatQ = queueF;
//            } else {
////                thisQ = queueF;
//                thatQ = queueT;
//            }
//            boolean empty = numGet == numPut;
//            thatQ.add(o);
//            numPut++;
//            if (empty) {
//                logger.logp(Level.FINEST, CLASS_NAME, "put", "invokeLater(clientInvoke)");
//                SwingUtilities.invokeLater(clientInvoke);
//            }
//            logger.logp(Level.FINEST, CLASS_NAME, "put", "RETURN");
//        }
//
//        private synchronized Client.ServerEvent get() {
//            logger.logp(Level.FINEST, CLASS_NAME, "get", "ENTRY");
//            if (numGet == numPut) return null;
//            ArrayList<Client.ServerEvent> thisQ;
//            ArrayList<Client.ServerEvent> thatQ;
//            if (getC) {
//                thisQ = queueT;
//                thatQ = queueF;
//            } else {
//                thisQ = queueF;
//                thatQ = queueT;
//            }
//            Client.ServerEvent o = null;
//            if (getIndex < thisQ.size()) {
//                o = thisQ.set(getIndex++, null);
//            } else {
//                o = thatQ.set(0, null);
//                getIndex = 1;
//                getC = !getC;
//                thisQ.clear();
//            }
//            numGet++;
//            logger.logp(Level.FINEST, CLASS_NAME, "get", "RETURN");
//            return o;
//        }
//    }
//
//    private final FIFO clientFifo = new FIFO();
//
////    private static volatile int clientNumExamine = 0;
    private static Snapshot clientSnapshot = EDatabase.clientDatabase().getInitialSnapshot();
//
//    private final ClientInvoke clientInvoke = new ClientInvoke();
//    private class ClientInvoke implements Runnable {
//        private final String CLASS_NAME = getClass().getName();
//        public void run() {
//            logger.entering(CLASS_NAME, "run");
//            assert SwingUtilities.isEventDispatchThread();
//            for (;;) {
//                logger.logp(Level.FINEST, CLASS_NAME, "run", "before get");
//                if (jobTreeChanged) {
//                    jobTreeChanged = false;
//                    ArrayList<Job.Inform> jobs = new ArrayList<Job.Inform>();
//                    for (Iterator<Job> it = Job.getAllJobs(); it.hasNext();) {
//                        Job j = it.next();
//                        jobs.add(j.getInform());
//                    }
//                    JobTree.update(jobs);
//              }
//                int numGet = clientFifo.numGet;
//                Client.ServerEvent o = clientFifo.get();
//                if (o == null) break;
//                if (o instanceof Client.EJobEvent) {
//                    Client.EJobEvent ejobEvent = (Client.EJobEvent)o;
//                    EJob ejob_ = ejobEvent.ejob;
//                    if (false) {
//                        System.out.print("Job " + ejob_.jobKey.jobId + " " + ejob_.jobName + " " + ejob_.jobType + " " + ejob_.state +
//                                " old=" + ejob_.oldSnapshot.snapshotId + " new=" + ejob_.newSnapshot.snapshotId +
//                                " t=" + ejobEvent.timeStamp + "(" + (System.currentTimeMillis() - ejobEvent.timeStamp) + ")");
//                        if (ejob_.serializedJob != null)
//                            System.out.print(" ser=" + ejob_.serializedJob.length);
//                        if (ejob_.serializedResult != null)
//                            System.out.print(" res=" + ejob_.serializedResult.length);
//                        System.out.println();
//                    }
//                    int jobId = ejob_.jobKey.jobId;
//                    logger.logp(Level.FINER, CLASS_NAME, "run", "result begin {0}", Integer.valueOf(numGet));
//                    if (ejob_.newSnapshot != clientSnapshot) {
//                        (new SnapshotDatabaseChangeRun(clientSnapshot, ejob_.newSnapshot)).run();
//                        clientSnapshot = ejob_.newSnapshot;
//                    }
//                    if (Job.currentUI != null) {
//                        Job.getExtendedUserInterface().showSnapshot(ejobEvent.ejob.newSnapshot, ejobEvent.ejob.jobType == Job.Type.UNDO);
////                        Job.currentUI.addEvent(o);
//                        continue;
//                    }
//                    if (ejob_.state != EJob.State.SERVER_DONE) continue;
//                    EJob ejob = null;
//                    for (EJob ej: startedJobs) {
//                        if (ej.jobKey.jobId == jobId) {
//                            ejob = ej;
//                            break;
//                        }
//                    }
//                    if (ejob == null) {
//                        System.out.println("Can't find EJob " + jobId);
//                        continue;
//                    }
//                    ejob.serializedResult = ejob.serializedResult;
//                    Job job = ejob.getJob();
//                    if (job != null) {
//                        Job.runTerminate(ejob);
//                        // delete
//                        if (job.deleteWhenDone) {
//                            startedJobs.remove(job);
//                            TopLevel.setBusyCursor(isChangeJobQueuedOrRunning());
//                        }
//                        logger.logp(Level.FINER, CLASS_NAME, "run", "result end {0}", ejob.jobName);
//                    } else {
//                        logger.logp(Level.WARNING, CLASS_NAME, "run", "result of unknown job {0}", o);
//                        System.out.println("Job " + jobId + " was not found in startedJobs");
//                    }
//                } else if (o instanceof Client.PrintEvent) {
//                    logger.logp(Level.FINEST, CLASS_NAME, "run", "string begin");
//                    if (Job.currentUI != null)
//                        Job.currentUI.addEvent(o);
//                    logger.logp(Level.FINEST, CLASS_NAME, "run", "string end {0}", o);
//                }
//            }
//            if (waitingJobs.isEmpty()) {
//                logger.exiting(CLASS_NAME, "run");
//                return;
//            }
//            EJob ejob = waitingJobs.remove(0);
//            Job job = ejob.clientJob;
//            if (ejob.jobType == Job.Type.EXAMINE) {
//                logger.logp(Level.FINER, CLASS_NAME, "run", "Schedule EXAMINE {0}", job);
//                try {
//                    job.doIt();
//                } catch (JobException e) {
//                    e.printStackTrace();
//                }
//            } else {
//                logger.logp(Level.FINER, CLASS_NAME, "run", "Schedule {0}", job);
////                ejob.jobKey.jobId = ++numStarted;
//                Throwable e = ejob.serialize(EDatabase.clientDatabase());
//                if (e != null) {
//                    System.out.println("Job " + this + " was not launched in CLIENT mode");
//                    e.printStackTrace(System.out);
//                } else {
//                    try {
//                        writeEJob(ejob);
//
//                        ejob.getJob().started = true;
//                        startedJobs.add(ejob);
////                        clientJob = job;
//                    } catch (IOException ee) {
//                        System.out.println("Job " + this + " was not launched in CLIENT mode");
//                        ee.printStackTrace(System.out);
//                    }
//                }
//            }
//            logger.exiting(CLASS_NAME, "run");
//       }
//    };


    private static class SnapshotDatabaseChangeRun implements Runnable
	{
		private Snapshot oldSnapshot;
        private Snapshot newSnapshot;
		private SnapshotDatabaseChangeRun(Snapshot oldSnapshot, Snapshot newSnapshot) {
            this.oldSnapshot = oldSnapshot;
            this.newSnapshot = newSnapshot;
        }
        public void run() {
            EDatabase database = EDatabase.clientDatabase();
            database.lock(true);
            try {
                database.checkFresh(oldSnapshot);
                database.lowLevelSetCanUndoing(true);
                database.getNetworkManager().startBatch();
                database.undo(newSnapshot);
                database.getNetworkManager().endBatch();
                database.lowLevelSetCanUndoing(false);
            } finally {
                database.unlock();
            }
        }
	}
}
