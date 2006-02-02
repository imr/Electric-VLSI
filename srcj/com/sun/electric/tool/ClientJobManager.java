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

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellId;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.SnapshotReader;
import com.sun.electric.database.change.DatabaseChangeEvent;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.JobTree;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 *
 */
class ClientJobManager extends JobManager {
    private static final String CLASS_NAME = Job.class.getName();
    private static final Logger logger = Logger.getLogger("com.sun.electric.tool.job");
    private final int port;
    
    /** stream for cleint to send Jobs. */      private static DataOutputStream clientOutputStream;
    /** Count of started Jobs. */               private static int numStarted;
    private volatile boolean jobTreeChanged;
    private static Job clientJob;
    
    /** Creates a new instance of ClientJobManager */
    public ClientJobManager(int serverPort) {
        port = serverPort;
    }
 
    void writeEJob(EJob ejob) throws IOException {
        clientOutputStream.writeInt(ejob.jobId);
        clientOutputStream.writeUTF(ejob.jobType.toString());
        clientOutputStream.writeUTF(ejob.jobName);
        clientOutputStream.writeInt(ejob.serializedJob.length);
        clientOutputStream.write(ejob.serializedJob);
        clientOutputStream.flush();
    }
    
    public void runLoop() {
        logger.entering(CLASS_NAME, "clinetLoop", port);
        SnapshotReader reader = null;
        Snapshot currentSnapshot = new Snapshot();
        try {
            System.out.println("Attempting to connect to port " + port + " ...");
            Socket socket = new Socket((String)null, port);
            reader = new SnapshotReader(new DataInputStream(new BufferedInputStream(socket.getInputStream())));
            clientOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            System.out.println("Connected");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
        logger.logp(Level.FINER, CLASS_NAME, "clientLoop", "initTechnologies begin");
        Technology.initAllTechnologies();
        User.getUserTool().init();
        NetworkTool.getNetworkTool().init();
        logger.logp(Level.FINER, CLASS_NAME, "clientLoop", "initTechnologies end");
        //Tool.initAllTools();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // remove the splash screen
                //if (sw != null) sw.removeNotify();
                logger.entering(CLASS_NAME, "InitializeWindows");
                TopLevel.InitializeWindows();
                WindowFrame.wantToOpenCurrentLibrary(true);
                logger.exiting(CLASS_NAME, "InitializeWindows");
            }
        });
        
        for (;;) {
            try {
                logger.logp(Level.FINEST, CLASS_NAME, "clientLoop", "readTag");
                byte tag = reader.in.readByte();
                switch (tag) {
                    case 1:
                        logger.logp(Level.FINER, CLASS_NAME, "clientLoop", "readSnapshot begin {0}", Integer.valueOf(clientFifo.numPut));
                        Snapshot newSnapshot = Snapshot.readSnapshot(reader, currentSnapshot);
                        logger.logp(Level.FINER, CLASS_NAME, "clientLoop", "readSnapshot end");
                        clientFifo.put(newSnapshot, null);
                        currentSnapshot = newSnapshot;
                        break;
                    case 2:
                        logger.logp(Level.FINER, CLASS_NAME, "clientLoop", "readResult begin {0}", Integer.valueOf(clientFifo.numPut));
                        Integer jobId = Integer.valueOf(reader.in.readInt());
                        EJob.State newState = EJob.State.valueOf(reader.in.readUTF());
                        int len = reader.in.readInt();
                        byte[] bytes = new byte[len];
                        reader.in.readFully(bytes);
                        logger.logp(Level.FINER, CLASS_NAME, "clientLoop", "readResult end {0}", jobId);
                        clientFifo.put(jobId, bytes);
                        break;
                    case 3:
                        logger.logp(Level.FINEST, CLASS_NAME, "clientLoop", "readStr begin");
                        String str = reader.in.readUTF();
                        logger.logp(Level.FINEST, CLASS_NAME, "clientLoop", "readStr end {0}", str);
                        clientFifo.put(str, null);
                        break;
                    default:
                        logger.logp(Level.SEVERE, CLASS_NAME, "clientLoop", "bad tag {0}", Byte.valueOf(tag));
                        assert false;
                }
            } catch (IOException e) {
                // reader.in.close();
                reader = null;
                logger.logp(Level.INFO, CLASS_NAME, "clientLoop", "failed", e);
                System.out.println("END OF FILE reading from server");
                return;
            }
        }
    }
    
    /** Add job to list of jobs */
    void addJob(EJob ejob, boolean onMySnapshot) {
        assert SwingUtilities.isEventDispatchThread();
        if (onMySnapshot)
            waitingJobs.add(0, ejob);
        else
            waitingJobs.add(ejob);
        if (ejob.getJob().getDisplay())
            jobTreeChanged = true;
        SwingUtilities.invokeLater(clientInvoke);
//        Job.currentUI.invokeLaterBusyCursor(isChangeJobQueuedOrRunning()); // Not here !!!!
        SwingUtilities.invokeLater(new Runnable() { public void run() { TopLevel.setBusyCursor(isChangeJobQueuedOrRunning()); }});
        return;
    }
    
    /** Remove job from list of jobs */
    void removeJob(Job j) { // synchronization !!!
        if (j.started) {
            for (Iterator<EJob> it = startedJobs.iterator(); it.hasNext(); ) {
                EJob ejob = it.next();
                if (ejob.getJob() == j) {
                    it.remove();
                }
            }
        } else {
//            if (!Job.waitingJobs.isEmpty() && Job.waitingJobs.get(0).getJob() == j)
//                Job.databaseChangesMutex.notify();
            for (Iterator<EJob> it = waitingJobs.iterator(); it.hasNext(); ) {
                EJob ejob = it.next();
                if (ejob.getJob() == j) {
                    it.remove();
                }
            }
        }
        //System.out.println("Removed Job "+j+", index was "+index+", numStarted now="+numStarted+", allJobs="+allJobs.size());
        if (j.getDisplay()) {
            jobTreeChanged = true;
            SwingUtilities.invokeLater(clientInvoke);
        }
    }
    
    void setProgress(EJob ejob, String progress) {
        ejob.progress = progress;
    }
    
    private boolean isChangeJobQueuedOrRunning() { // synchronization !!!
        for (EJob ejob: startedJobs) {
            Job job = ejob.getJob();
            if (job != null && job.finished) continue;
            if (ejob.jobType == Job.Type.CHANGE) return true;
        }
        for (EJob ejob: waitingJobs) {
            if (ejob.jobType == Job.Type.CHANGE) return true;
        }
        return false;
    }
    
    /** get all jobs iterator */
    Iterator<Job> getAllJobs() { // synchronization !!!
        ArrayList<Job> jobsList = new ArrayList<Job>();
        for (EJob ejob: startedJobs) {
            Job job = ejob.getJob();
            if (job != null)
                jobsList.add(job);
        }
        for (EJob ejob: waitingJobs) {
            Job job = ejob.getJob();
            if (job != null)
                jobsList.add(job);
        }
        return jobsList.iterator();
    }
    
    private class FIFO {
        private final String CLASS_NAME = ClientJobManager.CLASS_NAME + ".FIFO";
        private final ArrayList<Object> queueF = new ArrayList<Object>();
        private final ArrayList<Object> queueT = new ArrayList<Object>();
        private boolean getC = false;
        private int getIndex = 0;
        private int numGet;
        private int numPut;
        
        private synchronized void put(Object o, Object o1) {
            logger.logp(Level.FINEST, CLASS_NAME, "put", "ENTRY");
            ArrayList<Object> thisQ;
            ArrayList<Object> thatQ;
            if (getC) {
                thisQ = queueT;
                thatQ = queueF;
            } else {
                thisQ = queueF;
                thatQ = queueT;
            }
            boolean empty = numGet == numPut;
            thatQ.add(o);
            numPut++;
            if (o1 != null) {
                thatQ.add(o1);
                numPut++;
            }
            if (empty) {
                logger.logp(Level.FINEST, CLASS_NAME, "put", "invokeLater(clientInvoke)");
                SwingUtilities.invokeLater(clientInvoke);
            }
            logger.logp(Level.FINEST, CLASS_NAME, "put", "RETURN");
        }
        
        private synchronized Object get() {
            logger.logp(Level.FINEST, CLASS_NAME, "get", "ENTRY");
            if (numGet == numPut) return null;
            ArrayList<Object> thisQ;
            ArrayList<Object> thatQ;
            if (getC) {
                thisQ = queueT;
                thatQ = queueF;
            } else {
                thisQ = queueF;
                thatQ = queueT;
            }
            Object o = null;
            if (getIndex < thisQ.size()) {
                o = thisQ.set(getIndex++, null);
            } else {
                o = thatQ.set(0, null);
                getIndex = 1;
                getC = !getC;
                thisQ.clear();
            }
            numGet++;
            logger.logp(Level.FINEST, CLASS_NAME, "get", "RETURN");
            return o;
        }
    }
    
    private final FIFO clientFifo = new FIFO();
    
    private static volatile int clientNumExamine = 0;
    private static Snapshot clientSnapshot = new Snapshot();
    
    private final ClientInvoke clientInvoke = new ClientInvoke();
    private class ClientInvoke implements Runnable {
        private final String CLASS_NAME = getClass().getName();
        public void run() {
            logger.entering(CLASS_NAME, "run");
            assert SwingUtilities.isEventDispatchThread();
            for (;;) {
                logger.logp(Level.FINEST, CLASS_NAME, "run", "before get");
                if (jobTreeChanged) {
                    jobTreeChanged = false;
                    JobTree.update();
                }
                int numGet = clientFifo.numGet;
                Object o = clientFifo.get();
                if (o == null) break;
                if (o instanceof Snapshot) {
                    Snapshot newSnapshot = (Snapshot)o;
                    logger.logp(Level.FINER, CLASS_NAME, "run", "snapshot begin {0}", Integer.valueOf(numGet));
                    (new SnapshotDatabaseChangeRun(clientSnapshot, newSnapshot)).run();
                    clientSnapshot = newSnapshot;
                    logger.logp(Level.FINER, CLASS_NAME, "run", "snapshot end");
                } else if (o instanceof Integer) {
                    int jobId = ((Integer)o).intValue();
                    logger.logp(Level.FINER, CLASS_NAME, "run", "result begin {0}", Integer.valueOf(numGet));
                    byte[] bytes = (byte[])clientFifo.get();
                    EJob ejob = null;
                    for (EJob ej: startedJobs) {
                        if (ej.jobId == jobId) {
                            ejob = ej;
                            break;
                        }
                    }
                    if (ejob == null) {
                        System.out.println("Can't find EJob " + jobId);
                    }
                    ejob.serializedResult = bytes;
                    Job job = ejob.getJob();
                    if (job != null) {
                        Job.runTerminate(ejob);
                        // delete
                        if (job.deleteWhenDone) {
                            startedJobs.remove(job);
                            TopLevel.setBusyCursor(isChangeJobQueuedOrRunning());
                        }
                        logger.logp(Level.FINER, CLASS_NAME, "run", "result end {0}", ejob.jobName);
                    } else {
                        logger.logp(Level.WARNING, CLASS_NAME, "run", "result of unknown job {0}", o);
                        System.out.println("Job " + jobId + " was not found in startedJobs");
                    }
                } else if (o instanceof String) {
                    logger.logp(Level.FINEST, CLASS_NAME, "run", "string begin");
                    System.out.print((String)o);
                    logger.logp(Level.FINEST, CLASS_NAME, "run", "string end {0}", o);
                }
            }
            if (waitingJobs.isEmpty()) {
                logger.exiting(CLASS_NAME, "run");
                return;
            }
            EJob ejob = waitingJobs.remove(0);
            Job job = ejob.clientJob;
            if (ejob.jobType == Job.Type.EXAMINE) {
                logger.logp(Level.FINER, CLASS_NAME, "run", "Schedule EXAMINE {0}", job);
                try {
                    job.doIt();
                } catch (JobException e) {
                    e.printStackTrace();
                }
            } else {
                logger.logp(Level.FINER, CLASS_NAME, "run", "Schedule {0}", job);
                ejob.jobId = ++numStarted;
                Throwable e = ejob.serialize();
                if (e != null) {
                    System.out.println("Job " + this + " was not launched in CLIENT mode");
                    e.printStackTrace(System.out);
                } else {
                    try {
                        writeEJob(ejob);
                        
                        ejob.getJob().started = true;
                        startedJobs.add(ejob);
                        clientJob = job;
                    } catch (IOException ee) {
                        System.out.println("Job " + this + " was not launched in CLIENT mode");
                        ee.printStackTrace(System.out);
                    }
                }
            }
            logger.exiting(CLASS_NAME, "run");
       }
    };
    
    
    private static class SnapshotDatabaseChangeRun implements Runnable 
	{
		private Snapshot oldSnapshot;
        private Snapshot newSnapshot;
		private SnapshotDatabaseChangeRun(Snapshot oldSnapshot, Snapshot newSnapshot) {
            this.oldSnapshot = oldSnapshot;
            this.newSnapshot = newSnapshot;
        }
        public void run() {
            boolean cellTreeChanged = Library.updateAll(oldSnapshot, newSnapshot);
            NetworkTool.updateAll(oldSnapshot, newSnapshot);
            for (int i = 0; i < newSnapshot.cellBackups.size(); i++) {
            	CellBackup newBackup = newSnapshot.getCell(i);
            	CellBackup oldBackup = oldSnapshot.getCell(i);
                ERectangle newBounds = newSnapshot.getCellBounds(i);
                ERectangle oldBounds = oldSnapshot.getCellBounds(i);
            	if (newBackup != oldBackup || newBounds != oldBounds) {
            		Cell cell = (Cell)CellId.getByIndex(i).inCurrentThread();
            		User.markCellForRedraw(cell, true);
            	}
            }
            SnapshotDatabaseChangeEvent event = new SnapshotDatabaseChangeEvent(cellTreeChanged);
            Undo.fireDatabaseChangeEvent(event);
        }
	}
    
    private static class SnapshotDatabaseChangeEvent extends DatabaseChangeEvent {
        private boolean cellTreeChanged;
        
        SnapshotDatabaseChangeEvent(boolean cellTreeChanged) {
            super(null);
            this.cellTreeChanged = cellTreeChanged;
        }
        
        /**
         * Returns true if ElectricObject eObj was created, killed or modified
         * in the new database state.
         * @param eObj ElectricObject to test.
         * @return true if the ElectricObject was changed.
         */
        public boolean objectChanged(ElectricObject eObj) { return true; }
        
        /**
         * Returns true if cell explorer tree was changed
         * in the new database state.
         * @return true if cell explorer tree was changed.
         */
        public boolean cellTreeChanged() {
            return cellTreeChanged;
//                if (change.getType() == Undo.Type.VARIABLESMOD && change.getObject() instanceof Cell) {
//                    ImmutableElectricObject oldImmutable = (ImmutableElectricObject)change.getO1();
//                    ImmutableElectricObject newImmutable = (ImmutableElectricObject)change.getObject().getImmutable();
//                    return oldImmutable.getVar(Cell.MULTIPAGE_COUNT_KEY) != newImmutable.getVar(Cell.MULTIPAGE_COUNT_KEY);
//                }
        }
    }
    
}
