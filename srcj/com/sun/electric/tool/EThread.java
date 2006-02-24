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
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.constraint.Constraints;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.tool.user.User;
import java.lang.reflect.Field;
import java.util.ArrayList;

import java.util.logging.Level;

/**
 * Thread for execution Jobs in Electric.
 */
class EThread extends Thread {
    private static final String CLASS_NAME = EThread.class.getName();

    private static final ArrayList<Snapshot> snapshotCache = new ArrayList<Snapshot>();
	private static int maximumSnapshots = User.getMaxUndoHistory();
    
    /** EJob which Thread is executing now. */
    EJob ejob;
    /* Database in which thread is executing. */
    EDatabase database;
    
    private final UserInterface userInterface = new ServerJobManager.UserInterfaceRedirect();
    
    /** Creates a new instance of EThread */
    EThread(int id) {
        super("EThread-" + id);
 //       setUserInterface(Job.currentUI);
        Job.logger.logp(Level.FINER, CLASS_NAME, "constructor", getName());
        start();
    }

    public void run() {
        Job.logger.logp(Level.FINE, CLASS_NAME, "run", getName());
        EJob finishedEJob = null;
        for (;;) {
            ejob = Job.jobManager.selectEJob(finishedEJob);
            Job.logger.logp(Level.FINER, CLASS_NAME, "run", "selectedJob {0}", ejob.jobName);
            database = ejob.jobType != Job.Type.EXAMINE ? EDatabase.serverDatabase() : EDatabase.clientDatabase();
            ejob.changedFields = new ArrayList<Field>();
            Throwable jobException = null;
            database.lock(!ejob.isExamine());
            ejob.oldSnapshot = database.backup();
            try {
                if (ejob.jobType != Job.Type.EXAMINE && !ejob.startedByServer) {
                    Throwable e = ejob.deserializeToServer();
                    if (e != null)
                        throw e;
                }
                switch (ejob.jobType) {
                    case CHANGE:
                        database.lowLevelSetCanChanging(true);
                        database.getNetworkManager().startBatch();
                        Constraints.getCurrent().startBatch(ejob.oldSnapshot);
                        if (!ejob.serverJob.doIt())
                            throw new JobException();
                        Constraints.getCurrent().endBatch(ejob.client.userName);
                        database.getNetworkManager().endBatch();
                        database.lowLevelSetCanChanging(false);
                        ejob.newSnapshot = database.backup();
                        ejob.serializeResult();
                        break;
                    case UNDO:
                        database.lowLevelSetCanUndoing(true);
                        database.getNetworkManager().startBatch();
                        int snapshotId = ((Undo.UndoJob)ejob.serverJob).getSnapshotId();
                        Snapshot undoSnapshot = findInCache(snapshotId);
                        if (undoSnapshot == null)
                            throw new JobException("Snapshot " + snapshotId + " not found");
                        database.undo(undoSnapshot, false);
                        database.getNetworkManager().endBatch();
                        database.lowLevelSetCanUndoing(false);
                        ejob.serializeResult();
                        break;
                    case REMOTE_EXAMINE:
                        if (!ejob.serverJob.doIt())
                            throw new JobException();
                        ejob.serializeResult();
                        break;
                    case EXAMINE:
                        if (!ejob.clientJob.doIt())
                            throw new JobException();
                        break;
                }
                ejob.newSnapshot = database.backup();
                database.checkFresh(ejob.newSnapshot);
//                ejob.state = EJob.State.SERVER_DONE;
            } catch (Throwable e) {
                e.getStackTrace();
                if (!ejob.isExamine()) {
                    database.lowLevelSetCanUndoing(true);
                    database.undo(ejob.oldSnapshot, true);
                    ejob.newSnapshot = ejob.oldSnapshot;
                    database.getNetworkManager().endBatch();
                }
                ejob.serializeExceptionResult(e);
//                ejob.state = EJob.State.SERVER_FAIL;
            } finally {
                database.unlock();
            }
            putInCache(ejob.oldSnapshot, ejob.newSnapshot);
            
            finishedEJob = ejob;
            ejob = null;
            database = null;
            
            Job.logger.logp(Level.FINER, CLASS_NAME, "run", "finishedJob {0}", finishedEJob.jobName);
        }
    }
    
    UserInterface getUserInterface() { return userInterface; }

    void print(String str) {
        Client client = ejob != null ? ejob.client : null;
        Client.print(client, str);
    }
    
    private static Snapshot findInCache(int snapshotId) {
        for (int i = snapshotCache.size() - 1; i >= 0; i--) {
            Snapshot snapshot = snapshotCache.get(i);
            if (snapshot.snapshotId == snapshotId)
                return snapshot;
        }
        return null;
    }
    
    
    private static void putInCache(Snapshot oldSnapshot, Snapshot newSnapshot) {
        if (!snapshotCache.contains(newSnapshot)) {
            while (!snapshotCache.isEmpty() && snapshotCache.get(snapshotCache.size() - 1) != oldSnapshot)
                snapshotCache.remove(snapshotCache.size() - 1);
            snapshotCache.add(newSnapshot);
        }
        while (snapshotCache.size() > maximumSnapshots)
            snapshotCache.remove(0);
    }
    
	/**
	 * Method to set the size of the history list and return the former size.
	 * @param newSize the new size of the history list (number of batches of changes).
	 * If not positive, the list size is not changed.
	 * @return the former size of the history list.
	 */
	public static int setHistoryListSize(int newSize) {
		if (newSize <= 0) return maximumSnapshots;

		int oldSize = maximumSnapshots;
		maximumSnapshots = newSize;
		while (snapshotCache.size() > maximumSnapshots)
			snapshotCache.remove(0);
		return oldSize;
	}
}
