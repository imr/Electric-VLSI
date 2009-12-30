/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EThread.java
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

import com.sun.electric.StartupPrefs;
import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.Environment;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.constraint.Constraints;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.tool.user.ActivityLogger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * Thread for execution Jobs in Electric.
 */
class EThread extends Thread {

    private static final String CLASS_NAME = EThread.class.getName();
    private static final ArrayList<Snapshot> snapshotCache = new ArrayList<Snapshot>();
    private static int maximumSnapshots = StartupPrefs.getMaxUndoHistory();
    /** EJob which Thread is executing now. */
    EJob ejob;
    /** True if this EThread is execution server job. */
    boolean isServerThread;
    /* Database in which thread is executing. */
    EDatabase database;
    ServerJobManager.UserInterfaceRedirect userInterface;

    /** Creates a new instance of EThread */
    EThread(int id) {
        super("EThread-" + id);
        //       setUserInterface(Job.currentUI);
        Job.logger.logp(Level.FINER, CLASS_NAME, "constructor", getName());
        start();
    }

    EThread(String name) {
        super(name);
    }

    public void run() {
        Job.logger.logp(Level.FINE, CLASS_NAME, "run", getName());
        EJob finishedEJob = null;
        for (;;) {
            ejob = Job.serverJobManager.selectEJob(finishedEJob);
            Job.logger.logp(Level.FINER, CLASS_NAME, "run", "selectedJob {0}", ejob.jobName);
            isServerThread = ejob.jobType != Job.Type.CLIENT_EXAMINE;
            database = isServerThread ? EDatabase.serverDatabase() : EDatabase.clientDatabase();
            ejob.changedFields = new ArrayList<Field>();
//            Throwable jobException = null;
            Environment.setThreadEnvironment(database.getEnvironment());
            EditingPreferences.setThreadEditingPreferences(ejob.editingPreferences);
            userInterface = new ServerJobManager.UserInterfaceRedirect(ejob.jobKey);
            database.lock(!ejob.isExamine());
            ejob.oldSnapshot = database.backup();
            try {
                if (ejob.jobType != Job.Type.CLIENT_EXAMINE && !ejob.jobKey.startedByServer()) {
                    Throwable e = ejob.deserializeToServer();
                    if (e != null) {
                        throw e;
                    }
                }
                switch (ejob.jobType) {
                    case CHANGE:
                        database.lowLevelBeginChanging(ejob.serverJob.tool);
                        Constraints.getCurrent().startBatch(ejob.oldSnapshot);
                        userInterface.setCurrents(ejob.serverJob);
                        if (!ejob.serverJob.doIt()) {
                            throw new JobException("Job '" + ejob.jobName + "' failed");
                        }
                        Constraints.getCurrent().endBatch(ejob.client.userName);
                        database.lowLevelEndChanging();
                        ejob.newSnapshot = database.backup();
                        break;
                    case UNDO:
                        database.lowLevelSetCanUndoing(true);
//                        userInterface.curTechId = null;
//                        userInterface.curLibId = null;
//                        userInterface.curCellId = null;
                        int snapshotId = ((Undo.UndoJob) ejob.serverJob).getSnapshotId();
                        Snapshot undoSnapshot = findInCache(snapshotId);
                        if (undoSnapshot == null) {
                            throw new JobException("Snapshot " + snapshotId + " not found");
                        }
                        database.undo(undoSnapshot);
                        database.lowLevelSetCanUndoing(false);
                        break;
                    case SERVER_EXAMINE:
                        userInterface.setCurrents(ejob.serverJob);
                        if (!ejob.serverJob.doIt()) {
                            throw new JobException("Job '" + ejob.jobName + "' failed");
                        }
                        break;
                    case CLIENT_EXAMINE:
                        if (ejob.jobKey.startedByServer()) {
                            Throwable e = ejob.deserializeToClient();
                            if (e != null) {
                                throw e;
                            }
                        }
                        userInterface.setCurrents(ejob.clientJob);
                        if (!ejob.clientJob.doIt()) {
                            throw new JobException("Job '" + ejob.jobName + "' failed");
                        }
                        break;
                }
                ejob.serializeResult(database);
                ejob.newSnapshot = database.backup();
//                database.checkFresh(ejob.newSnapshot);
//                ejob.state = EJob.State.SERVER_DONE;
            } catch (Throwable e) {

                // Batch mode is used from scripts in which it is VERY
                // important for the JVM to exit with a nonzero error
                // code whenever something goes wrong.
                if (Main.isBatch()) {
                    e.printStackTrace();
                    System.exit(-1);
                }

                e.getStackTrace();
                if (!(e instanceof JobException)) {
                    e.printStackTrace();
                }
                if (!ejob.isExamine()) {
                    recoverDatabase(e instanceof JobException);
                    database.lowLevelEndChanging();
                    database.lowLevelSetCanUndoing(false);
                }
                ejob.serializeExceptionResult(e, database);
//                ejob.state = EJob.State.SERVER_FAIL;
            } finally {
                database.unlock();
                userInterface = null;
                Environment.setThreadEnvironment(null);
                EditingPreferences.setThreadEditingPreferences(null);
            }
            putInCache(ejob.oldSnapshot, ejob.newSnapshot);

            finishedEJob = ejob;
            ejob = null;
            isServerThread = false;
            database = null;

            Job.logger.logp(Level.FINER, CLASS_NAME, "run", "finishedJob {0}", finishedEJob.jobName);
        }
    }

    private void recoverDatabase(boolean quick) {
        database.lowLevelSetCanUndoing(true);
        try {
            if (quick) {
                database.undo(ejob.oldSnapshot);
            } else {
                database.recover(ejob.oldSnapshot);
            }
            ejob.newSnapshot = ejob.oldSnapshot;
            return;
        } catch (Throwable e) {
            ActivityLogger.logException(e);
        }
        for (;;) {
            try {
                Snapshot snapshot = findValidSnapshot();
                database.recover(snapshot);
                ejob.newSnapshot = snapshot;
                return;
            } catch (Throwable e) {
                ActivityLogger.logException(e);
            }
        }
    }

    UserInterface getUserInterface() {
        return userInterface;
    }

    /**
     * Find some valid snapshot in cache.
     */
    static Snapshot findValidSnapshot() {
        for (;;) {
            Snapshot snapshot;
            synchronized (snapshotCache) {
                if (snapshotCache.isEmpty()) {
                    return EDatabase.serverDatabase().getInitialSnapshot();
                }
                snapshot = snapshotCache.remove(snapshotCache.size() - 1);
            }
            try {
                snapshot.check();
                return snapshot;
            } catch (Throwable e) {
                ActivityLogger.logException(e);
            }
        }
    }

    private static Snapshot findInCache(int snapshotId) {
        synchronized (snapshotCache) {
            for (int i = snapshotCache.size() - 1; i >= 0; i--) {
                Snapshot snapshot = snapshotCache.get(i);
                if (snapshot.snapshotId == snapshotId) {
                    return snapshot;
                }
            }
        }
        return null;
    }

    private static void putInCache(Snapshot oldSnapshot, Snapshot newSnapshot) {
        synchronized (snapshotCache) {
            if (!snapshotCache.contains(newSnapshot)) {
                while (!snapshotCache.isEmpty() && snapshotCache.get(snapshotCache.size() - 1) != oldSnapshot) {
                    snapshotCache.remove(snapshotCache.size() - 1);
                }
                snapshotCache.add(newSnapshot);
            }
            while (snapshotCache.size() > maximumSnapshots) {
                snapshotCache.remove(0);
            }
        }
    }

    /**
     * Method to set the size of the history list and return the former size.
     * @param newSize the new size of the history list (number of batches of changes).
     * If not positive, the list size is not changed.
     * @return the former size of the history list.
     */
    public static int setHistoryListSize(int newSize) {
        if (newSize <= 0) {
            return maximumSnapshots;
        }

        int oldSize = maximumSnapshots;
        maximumSnapshots = newSize;
        while (snapshotCache.size() > maximumSnapshots) {
            snapshotCache.remove(0);
        }
        return oldSize;
    }

    /**
     * If this EThread is running a Job return it.
     * Return null otherwise.
     * @return a running Job or null
     */
    Job getRunningJob() {
        if (ejob == null) {
            return null;
        }
        return ejob.jobType == Job.Type.CLIENT_EXAMINE ? ejob.clientJob : ejob.serverJob;
    }

    EJob getRunningEJob() {
        return ejob;
    }
}
