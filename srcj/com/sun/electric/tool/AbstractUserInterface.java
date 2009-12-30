/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UserInterfaceExtended.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.Environment;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.LibId;
import com.sun.electric.database.id.TechId;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 */
public abstract class AbstractUserInterface extends Client implements UserInterface {
    private static final int DEFAULT_CONNECTION_ID = 0;
    private TechId curTechId;
    private LibId curLibId;
    private Job.Key jobKey = new Job.Key(DEFAULT_CONNECTION_ID, 0, false);
    private Job.Inform[] serverJobQueue = {};
    private final HashMap<Job.Key,EJob> processingEJobs = new HashMap<Job.Key,EJob>();
    private final ArrayList<EJob> clientJobs = new ArrayList<EJob>();
    private boolean goSleeping;
    private boolean waitChanging;
    private UIDispatcher uiDispatcher;

    protected AbstractUserInterface() {
        super(DEFAULT_CONNECTION_ID);
    }

    void patchConnectionId(int connectionId) {
        this.connectionId = connectionId;
        jobKey = new Job.Key(connectionId, 0, false);
    }

    void startDispatcher() {
        uiDispatcher = new UIDispatcher(connectionId);
        uiDispatcher.start();
    }

    public Job.Key getJobKey() {
        return jobKey;
    }

    public EDatabase getDatabase() {
        return EDatabase.clientDatabase();
    }
	public Technology getCurrentTechnology() {
        EDatabase database = getDatabase();
        Technology tech = null;
        if (curTechId != null)
            tech = database.getTech(curTechId);
        if (tech == null)
            tech = database.getTechPool().findTechnology(User.getDefaultTechnology());
        if (tech == null)
            tech = database.getTechPool().findTechnology("mocmos");
        return tech;
    }
    public TechId getCurrentTechId() { return curTechId; }
    public void setCurrentTechnology(Technology tech) {
        if (tech != null)
            curTechId = tech.getId();
    }
	/**
	 * Method to return the current Library.
	 * @return the current Library.
	 */
	public Library getCurrentLibrary() {
        return curLibId != null ? getDatabase().getLib(curLibId) : null;
    }
    public LibId getCurrentLibraryId() { return curLibId; }
    public void setCurrentLibrary(Library lib) {
        curLibId = lib != null ? lib.getId() : null;
    }

    protected abstract void addEvent(Client.ServerEvent serverEvent);

    public void finishInitialization() {}

    protected void updateNetworkErrors(Cell cell, List<ErrorLogger.MessageLog> errors) {
        if (!errors.isEmpty()) System.out.println(errors.size() + " network errors in " + cell);
    }

    protected void updateIncrementalDRCErrors(Cell cell, List<ErrorLogger.MessageLog> newErrors,
                                              List<ErrorLogger.MessageLog> delErrors) {
        if (!newErrors.isEmpty()) System.out.println(newErrors.size() + " drc errors in " + cell);
    }

    /**
     * Save current state of highlights and return its ID.
     */
    public int saveHighlights() { return 0; }

    /**
     * Restore state of highlights by its ID.
     * @param highlightsId id of saved highlights.
     */
    public void restoreHighlights(int highlightsId) {}

    /**
     * Show status of undo/redo buttons
     * @param newUndoEnabled new status of undo button.
     * @param newRedoEnabled new status of redo button.
     */
    public void showUndoRedoStatus(boolean newUndoEnabled, boolean newRedoEnabled) {}

    protected abstract void showJobQueue(Job.Inform[] jobQueue);

    void showJobQueue() {
        showJobQueue(getFullJobQueue());
    }

    void showServerJobQueue(Job.Inform[] jobQueue) {
        synchronized (this) {
            serverJobQueue = jobQueue;
        }
        showJobQueue();
    }

    private synchronized Job.Inform[] getFullJobQueue() {
        if (clientJobs.isEmpty())
            return serverJobQueue;
        Job.Inform[] sq = serverJobQueue;
        Job.Inform[] q = new Job.Inform[sq.length + clientJobs.size()];
        System.arraycopy(sq, 0, q, 0, sq.length);
        for (int i = 0; i < clientJobs.size(); i++)
            q[sq.length + i] = clientJobs.get(i).getInform();
        return q;
    }

    /**
     * Show new database snapshot.
     * @param newSnapshot new snapshot.
     */
    protected void showSnapshot(Snapshot newSnapshot, boolean undoRedo) {}

    protected abstract void terminateJob(Job.Key jobKey, String jobName, Tool tool,
            Job.Type jobType, byte[] serializedJob,
            boolean doItOk, byte[] serializedResult, Snapshot newSnapshot);

    public void beep() {}

    protected void setClientThread() {
        assert Job.clientThread == null;
        Job.clientThread = Thread.currentThread();
    }

    synchronized void putProcessingEJob(EJob ejob, boolean onMySnapshot) {
        Job.Key jobKey = ejob.jobKey;
        assert !jobKey.startedByServer();
        assert jobKey.clientId == connectionId;
        assert ejob.clientJob != null;
        EJob oldEJob = processingEJobs.put(jobKey, ejob);
        assert oldEJob == null;
        if (!ejob.jobKey.doItOnServer) {
            if (onMySnapshot)
                clientJobs.add(0, ejob);
            else
                clientJobs.add(ejob);
            showJobQueue();
            if (goSleeping) {
                uiDispatcher.interrupt();
            }
        }
    }

    protected synchronized EJob removeProcessingEJob(Job.Key jobKey) {
        EJob ejob = processingEJobs.remove(jobKey);
        if (ejob != null && !jobKey.doItOnServer) {
            boolean removed = clientJobs.remove(ejob);
//            assert removed;
            showJobQueue();
        }
        return ejob;
    }

    private synchronized EJob getClientJob() {
        return clientJobs.isEmpty() ? null : clientJobs.remove(0);
    }

    private synchronized boolean hasClientJobs() {
        return !clientJobs.isEmpty();
    }

    synchronized List<Job> getAllJobs() {
        ArrayList<Job> list = new ArrayList<Job>();
        for (EJob ejob: processingEJobs.values()) {
            list.add(ejob.getJob());
        }
        return list;
    }

    synchronized void setGoSleeping(boolean b) {
        assert goSleeping != b;
        goSleeping = b;
    }

    private synchronized void startChanging() {
        assert !waitChanging;
        waitChanging = true;
    }

    protected synchronized void endChanging() {
        assert waitChanging;
        waitChanging = false;
        notify();
    }

    private synchronized void waitChanging() {
        while (waitChanging) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
    }

    private class UIDispatcher extends Thread {
        private static final long STACK_SIZE_EVENT = 0;
        private Client.ServerEvent lastEvent = Client.getQueueTail();

        private UIDispatcher(int connectionId) {
            super(null, null, "UIDispatcher-" + connectionId, STACK_SIZE_EVENT);
        }

        private void doSafe() {
            EJob ejob = getClientJob();
            if (ejob != null) {
                doClientExamine(ejob);
                return;
            }
            Client.ServerEvent event;
            for (;;) {
                event = lastEvent.getNext();
                if (event == null) break;
                lastEvent = event;
                if (event instanceof EJobEvent) break;
                addEvent(event);
            }
            if (event == null) {
                setGoSleeping(true);
                if (hasClientJobs()) {
                    setGoSleeping(false);
                    interrupted();
                    return;
                }
                try {
                    lastEvent = getEvent(lastEvent);
                } catch (InterruptedException e) {
                    setGoSleeping(false);
                    interrupted();
                    return;
                }
                setGoSleeping(false);
                interrupted();
            }
            if (lastEvent instanceof EJobEvent && !((EJobEvent)lastEvent).jobType.isExamine()) {
                startChanging();
                addEvent(lastEvent);
                waitChanging();
            } else {
                addEvent(lastEvent);
            }
        }

        private void doClientExamine(EJob ejob) {
            assert ejob.jobType == Job.Type.CLIENT_EXAMINE;
            ejob.state = EJob.State.RUNNING;
            Job.currentUI.showJobQueue();
            ejob.changedFields = new ArrayList<Field>();
            EDatabase database = EDatabase.clientDatabase();
            Environment.setThreadEnvironment(database.getEnvironment());
            EditingPreferences.setThreadEditingPreferences(ejob.editingPreferences);
            ServerJobManager.UserInterfaceRedirect userInterface = new ServerJobManager.UserInterfaceRedirect(ejob.jobKey, Job.currentUI);
            Job.setUserInterface(userInterface);
            database.lock(!ejob.isExamine());
            ejob.oldSnapshot = database.backup();
            try {
                userInterface.setCurrents(ejob.clientJob);
                if (!ejob.clientJob.doIt()) {
                    throw new JobException("Job '" + ejob.jobName + "' failed");
                }
                ejob.serializeResult(database);
                ejob.newSnapshot = database.backup();
             } catch (Throwable e) {
                e.getStackTrace();
                if (!(e instanceof JobException))
                    e.printStackTrace();
                ejob.serializeExceptionResult(e, database);
            } finally {
                database.unlock();
                userInterface = null;
                Environment.setThreadEnvironment(null);
                EditingPreferences.setThreadEditingPreferences(null);
            }
            Job.currentUI.addEvent(new Client.EJobEvent(ejob.jobKey, ejob.jobName,
                    ejob.getJob().getTool(), ejob.jobType, ejob.serializedJob,
                    ejob.doItOk, ejob.serializedResult, database.backup(),
                    EJob.State.SERVER_DONE));
        }

        @Override
        public void run() {
            try {
                for (;;) {
                    doSafe();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lastEvent = null;
            }
        }
    }
}
