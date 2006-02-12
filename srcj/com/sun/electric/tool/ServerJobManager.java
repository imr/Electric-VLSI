/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ServerJobManager.java
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
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.MessagesStream;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.ErrorLoggerTree;
import com.sun.electric.tool.user.ui.JobTree;
import com.sun.electric.tool.user.ui.TopLevel;
import java.awt.geom.Point2D;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.locks.Condition;
import java.util.logging.Level;
import javax.swing.SwingUtilities;

/**
 *
 */
public class ServerJobManager extends JobManager implements Observer, Runnable {
    private static final String CLASS_NAME = Job.class.getName();
    private static final int DEFAULT_NUM_THREADS = 2;
    /** mutex for database synchronization. */  private final Condition databaseChangesMutex = newCondition();
    
    private final ServerSocket serverSocket;
    private final ArrayList<EJob> finishedJobs = new ArrayList<EJob>();
    private final ArrayList<ServerConnection> serverConnections = new ArrayList<ServerConnection>();
    private final UserInterface redirectInterface = new UserInterfaceRedirect();
    private int numThreads;
    private final int maxNumThreads;
    private boolean runningChangeJob;
    private boolean guiChanged;
    private boolean signalledEThread;
    
    private Snapshot currentSnapshot = new Snapshot();
    
    /** Creates a new instance of JobPool */
    ServerJobManager(int recommendedNumThreads) {
        maxNumThreads = initThreads(recommendedNumThreads);
        serverSocket = null;
    }
    
    ServerJobManager(int recommendedNumThreads, int socketPort) {
        maxNumThreads = initThreads(recommendedNumThreads);
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(socketPort);
            System.out.println("ServerSocket waits for port " + socketPort);
        } catch (IOException e) {
            System.out.println("ServerSocket mode failure: " + e.getMessage());
        }
        this.serverSocket = serverSocket;
    }
    
    private int initThreads(int recommendedNumThreads) {
        int maxNumThreads = DEFAULT_NUM_THREADS;
        if (recommendedNumThreads > 0)
            maxNumThreads = recommendedNumThreads;
        Job.logger.logp(Level.FINE, CLASS_NAME, "initThreads", "maxNumThreads=" + maxNumThreads);
        return maxNumThreads;
    }
    
    /** Add job to list of jobs */
    void addJob(EJob ejob, boolean onMySnapshot) {
        lock();
        try {
            if (onMySnapshot)
                waitingJobs.add(0, ejob);
            else
                waitingJobs.add(ejob);
            setEJobState(ejob, EJob.State.WAITING, onMySnapshot ? EJob.WAITING_NOW : "waiting");
            invokeEThread();
        } finally {
            unlock();
        }
    }
    
    /** Remove job from list of jobs */
    void removeJob(Job j) {
        EJob ejob = j.ejob;
        lock();
        try {
            switch (j.ejob.state) {
                case WAITING:
                    setEJobState(ejob, EJob.State.SERVER_DONE, null);
                case SERVER_DONE:
                    setEJobState(ejob, EJob.State.CLIENT_DONE, null);
                case CLIENT_DONE:
                    finishedJobs.remove(j.ejob);
                    if (!Job.BATCHMODE && !guiChanged)
                        SwingUtilities.invokeLater(this);
                    guiChanged = true;
                    break;
            }
        } finally {
            unlock();
        }
    }
    
    void setProgress(EJob ejob, String progress) {
        lock();
        try {
            if (ejob.state == EJob.State.RUNNING)
                setEJobState(ejob, EJob.State.RUNNING, progress);
        } finally {
            unlock();
        }
    }
    
    /** get all jobs iterator */
    Iterator<Job> getAllJobs() {
        lock();
        try {
            ArrayList<Job> jobsList = new ArrayList<Job>();
            for (EJob ejob: finishedJobs) {
                Job job = ejob.getJob();
                if (job != null)
                    jobsList.add(job);
            }
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
        } finally {
            unlock();
        }
    }
    
    /**
     * This method is called whenever the observed object is changed. An
     * application calls an <tt>Observable</tt> object's
     * <code>notifyObservers</code> method to have all the object's
     * observers notified of the change.
     *
     * @param   o     the observable object.
     * @param   arg   an argument passed to the <code>notifyObservers</code>
     *                 method.
     */
    public void update(Observable o, Object arg) {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof EThread)
            ((EThread)currentThread).print((String)arg);
    }
        
    //--------------------------PRIVATE JOB METHODS--------------------------

   /** Run gets called after the calling thread calls our start method */
   private void runJob(EThread thread, EJob ejob) {
        Job job = ejob.getJob();
        job.startTime = System.currentTimeMillis();

        if (ejob.connection == null && !ejob.isExamine()) {
            Throwable e = ejob.serialize();
            if (e == null)
                e = ejob.deserialize();
        }
        Job serverJob = ejob.serverJob;
        ejob.changedFields = new ArrayList<Field>();
        
//        Cell cell = Job.getUserInterface().getCurrentCell();
//        if (connection == null)
//            ActivityLogger.logJobStarted(jobName, jobType, cell, savedHighlights, savedHighlightsOffset);
        Throwable jobException = null;
		try {
            if (!ejob.isExamine()) {
                thread.setCanChanging(true);
                thread.setCanComputeBounds(true);
                thread.setCanComputeNetlist(true);
                thread.setJob(ejob);
                thread.setUserInterface(redirectInterface);
            }
			if (ejob.jobType == Job.Type.CHANGE)	{
                Undo.startChanges(job.tool, ejob.jobName, ejob.savedHighlights);
            }
            try {
                if (!serverJob.doIt())
                    throw new JobException();
            } catch (JobException e) {
                jobException = e;
            }
			if (ejob.jobType == Job.Type.CHANGE)	{
                Undo.endChanges();
            }
		} catch (Throwable e) {
            jobException = e;
            e.printStackTrace(System.err);
            ActivityLogger.logException(e);
            if (e instanceof Error) throw (Error)e;
		} finally {
			if (!ejob.isExamine()) {
				thread.setCanChanging(false);
                thread.setCanComputeBounds(false);
                thread.setCanComputeNetlist(false);
                thread.setJob(null);
                thread.setUserInterface(Job.currentUI);
                updateSnapshot();
			}
		}
        if (jobException == null)
            ejob.serializeResult();
        else
            ejob.serializeExceptionResult(jobException);
    }

    private void invokeEThread() {
        if (signalledEThread || startedJobs.size() >= maxNumThreads) return;
        if (!canDoIt()) return;
        if (startedJobs.size() < numThreads)
            databaseChangesMutex.signal();
        else
            new DatabaseChangesThread(numThreads++);
        signalledEThread = true;
    }
    
    private EJob selectTerminateIt() {
        lock();
        try {
            for (int i = 0; i < finishedJobs.size(); i++) {
                EJob ejob = finishedJobs.get(i);
                if (ejob.state == EJob.State.CLIENT_DONE) continue;
//                finishedJobs.remove(i);
                return ejob;
            }
        } finally {
            unlock();
        }
        return null;
    }
    
    void wantUpdateGui() {
        lock();
        try {
            this.guiChanged = true;
        } finally {
            unlock();
        }
    }
    
    private boolean guiChanged() {
        lock();
        try {
            boolean b = this.guiChanged;
            this.guiChanged = false;
            return b;
        } finally {
            unlock();
        }
    }
    
    private boolean canDoIt() {
        if (waitingJobs.isEmpty()) return false;
        EJob ejob = waitingJobs.get(0);
        return startedJobs.isEmpty() || !runningChangeJob && ejob.isExamine();
    }
    
    private void updateSnapshot() {
        Snapshot oldSnapshot = currentSnapshot;
        Snapshot newSnapshot = new Snapshot(oldSnapshot);
        if (newSnapshot.equals(oldSnapshot)) return;
        lock();
        try {
            currentSnapshot = newSnapshot;
            for (ServerConnection conn: serverConnections)
                conn.updateSnapshot(newSnapshot);
        } finally {
            unlock();
        }
    }
    
    private void setEJobState(EJob ejob, EJob.State newState, String info) {
        Job.logger.logp(Level.FINE, CLASS_NAME, "setEjobState", newState + " "+ ejob.jobName);
        EJob.State oldState = ejob.state;
        switch (newState) {
            case WAITING:
                break;
            case RUNNING:
                if (oldState == EJob.State.RUNNING) {
                    assert oldState == EJob.State.RUNNING;
                    ejob.progress = info;
                    if (info.equals(EJob.ABORTING))
                        ejob.serverJob.scheduledToAbort = true;
                }
               break;
            case SERVER_DONE:
                boolean removed;
                if (oldState == EJob.State.WAITING) {
                    removed = waitingJobs.remove(ejob);
                } else {
                    assert oldState == EJob.State.RUNNING;
                    removed = startedJobs.remove(ejob);
                    if (startedJobs.isEmpty())
                        runningChangeJob = false;
                }
                assert removed;
                if (Job.threadMode != Job.Mode.BATCH && ejob.connection == null)
                    finishedJobs.add(ejob);
                break;
            case CLIENT_DONE:
                assert oldState == EJob.State.SERVER_DONE;
                if (ejob.clientJob.deleteWhenDone)
                    finishedJobs.remove(ejob);
        }
        ejob.state = newState;
        EJob.Event event = ejob.newEvent();
        for (ServerConnection conn: serverConnections)
            conn.sendEJobEvent(event);
        if (!Job.BATCHMODE && !guiChanged)
            SwingUtilities.invokeLater(this);
        guiChanged = true;
        Job.logger.exiting(CLASS_NAME, "setJobState");
    }
    
    private boolean isChangeJobQueuedOrRunning() {
        lock();
        try {
            for (EJob ejob: startedJobs) {
                Job job = ejob.getJob();
                if (job != null && job.finished) continue;
                if (ejob.jobType == Job.Type.CHANGE) return true;
            }
            for (EJob ejob: waitingJobs) {
                if (ejob.jobType == Job.Type.CHANGE) return true;
            }
            return false;
        } finally {
            unlock();
        }
    }
    
    public void runLoop() {
        if (serverSocket == null) return;
        MessagesStream.getMessagesStream().addObserver(this);
        try {
            // Wait for connections
            for (;;) {
                Socket socket = serverSocket.accept();
                ServerConnection conn;
                lock();
                try {
                    conn = new ServerConnection(serverConnections.size(), socket, currentSnapshot);
                    serverConnections.add(conn);
                } finally {
                    unlock();
                }
                System.out.println("Accepted connection " + conn.connectionId);
                conn.start();
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }
    
    /**
     * This method is executed in Swing thread.
     */
    public void run() {
        assert !Job.BATCHMODE;
        Job.logger.logp(Level.FINE, CLASS_NAME, "run", "ENTER");
        while (guiChanged()) {
            ArrayList<Job> jobs = new ArrayList<Job>();
            for (Iterator<Job> it = Job.getAllJobs(); it.hasNext();) {
                Job j = it.next();
                if (j.getDisplay()) {
                    jobs.add(j);
                }
            }
            JobTree.update(jobs);
            TopLevel.setBusyCursor(isChangeJobQueuedOrRunning());
            for (;;) {
                EJob ejob = selectTerminateIt();
                if (ejob == null) break;
                
                Job.logger.logp(Level.FINE, CLASS_NAME, "run", "terminate {0}", ejob.jobName);
                Job.runTerminate(ejob);
                setEJobState(ejob, EJob.State.CLIENT_DONE, null);
                Job.logger.logp(Level.FINE, CLASS_NAME, "run", "terminated {0}", ejob.jobName);
            }
            Job.logger.logp(Level.FINE, CLASS_NAME, "run", "wantToRedoJobTree");
        }
        Job.logger.logp(Level.FINE, CLASS_NAME, "run", "EXIT");
    }
    
    public static void setUndoRedoStatus(boolean undoEnabled, boolean redoEnabled) {
        assert Job.jobManager instanceof ServerJobManager;
        Job.currentUI.showUndoRedoStatus(undoEnabled, redoEnabled);
        // transmit to connection
    } 
    
	/**
	 * Thread which execute all database change Jobs.
	 */
	class DatabaseChangesThread extends EThread
	{
        private final String CLASS_NAME = getClass().getName();
        
        // Job Management
		DatabaseChangesThread(int id) {
			super("EThread-" + id);
            setUserInterface(Job.currentUI);
            Job.logger.logp(Level.FINER, CLASS_NAME, "constructor", getName());
			start();
		}

        public void run() {
            Job.logger.logp(Level.FINE, CLASS_NAME, "run", getName());
            EJob finishedEJob = null;
            for (;;) {
                EJob selectedEJob = null;
                lock();
                try {
                    if (finishedEJob != null)
                        setEJobState(finishedEJob, EJob.State.SERVER_DONE, "done");
                    for (;;) {
                        signalledEThread = false;
                        // Search for examine
                        if (canDoIt()) {
                            EJob ejob = waitingJobs.remove(0);
                            startedJobs.add(ejob);
                            if (ejob.isExamine()) {
                                assert !runningChangeJob;
                                invokeEThread();
                            } else {
                                assert startedJobs.size() == 1;
                                assert !runningChangeJob;
                                runningChangeJob = true;
                            }
                            setEJobState(ejob, EJob.State.RUNNING, "running");
                            selectedEJob = ejob;
                            break;
                        }
                        if (Job.threadMode == Job.Mode.BATCH && startedJobs.isEmpty()) {
                            ActivityLogger.finished();
                            System.exit(0);
                        }
                        Job.logger.logp(Level.FINE, CLASS_NAME, "selectConnection", "pause");
                        databaseChangesMutex.awaitUninterruptibly();
                        Job.logger.logp(Level.FINE, CLASS_NAME, "selectConnection", "resume");
                    }
                } finally {
                    unlock();
                }
                Job.logger.logp(Level.FINER, CLASS_NAME, "run", "selectedJob {0}", selectedEJob.jobName);
                ServerConnection connection = selectedEJob.connection;
                if (connection != null) {
                    Throwable e = selectedEJob.deserialize();
                    if (e != null) {
                        selectedEJob.serializeExceptionResult(e);
                        e.printStackTrace();
                        continue;
                    }
                }
                runJob(this, selectedEJob);
                finishedEJob = selectedEJob;
                Job.logger.logp(Level.FINER, CLASS_NAME, "run", "finishedJob {0}", finishedEJob.jobName);
            }
        }
	}

    /*private*/ static class UserInterfaceRedirect implements UserInterface
	{
		public EditWindow_ getCurrentEditWindow_() {
//            System.out.println("UserInterface.getCurrentEditWindow was called from DatabaseChangesThread");
            return Job.currentUI.getCurrentEditWindow_();
        }
		public EditWindow_ needCurrentEditWindow_()
		{
            System.out.println("UserInterface.needCurrentEditWindow was called from DatabaseChangesThread");
			return null; 
		}
        /** Get current cell from current library */
		public Cell getCurrentCell()
        {
            System.out.println("UserInterface.getCurrentCell was called from DatabaseChangesThread");
			Library lib = Library.getCurrent();
			if (lib == null) return null;
			return lib.getCurCell();
        }
		public Cell needCurrentCell()
		{
            System.out.println("UserInterface.needCurrentCell was called from DatabaseChangesThread");
            /** Current cell based on current library */
            Cell curCell = getCurrentCell();
            if (curCell == null)
            {
                System.out.println("There is no current cell for this operation.  To create one, use the 'New Cell' command from the 'Cell' menu.");
            }
            return curCell;
		}
		public void repaintAllEditWindows() { throw new IllegalStateException(); }
        
        public void adjustReferencePoint(Cell cell, double cX, double cY) {
//            System.out.println("UserInterface.adjustReferencePoint was called from DatabaseChangesThread");
        };
		public void alignToGrid(Point2D pt) {
            System.out.println("UserInterface.alignToGrid was called from DatabaseChangesThread");
        }
		public int getDefaultTextSize() { return 14; }
//		public Highlighter getHighlighter();
		public EditWindow_ displayCell(Cell cell) { throw new IllegalStateException(); }

        public void termLogging(final ErrorLogger logger, boolean explain) {
            Job.currentUI.termLogging(logger, explain);
            // transmit to client
        }

        public void updateNetworkErrors(Cell cell, List<ErrorLogger.MessageLog> errors) { throw new IllegalStateException(); }
    
        public void updateIncrementalDRCErrors(Cell cell, List<ErrorLogger.MessageLog> errors) { throw new IllegalStateException(); }

        /**
         * Method to return the error message associated with the current error.
         * Highlights associated graphics if "showhigh" is nonzero.  Fills "g1" and "g2"
         * with associated geometry modules (if nonzero).
         */
        public String reportLog(ErrorLogger.MessageLog log, boolean showhigh, Geometric [] gPair)
        {
            System.out.println("UserInterface.reportLog was called from DatabaseChangesThread");
            // return the error message
            return log.getMessageString();
        }

        /**
         * Method to show an error message.
         * @param message the error message to show.
         * @param title the title of a dialog with the error message.
         */
        public void showErrorMessage(Object message, String title)
        {
//            System.out.println("UserInterface.showErrorMessage was called from DatabaseChangesThread");
        	System.out.println(message);
        }

        /**
         * Method to show an informational message.
         * @param message the message to show.
         * @param title the title of a dialog with the message.
         */
        public void showInformationMessage(Object message, String title)
        {
//            System.out.println("UserInterface.showInformationMessage was called from DatabaseChangesThread");
        	System.out.println(message);
        }

        /**
         * Method to show a message and ask for confirmation.
         * @param message the message to show.
         * @return true if "yes" was selected, false if "no" was selected.
         */
        public boolean confirmMessage(Object message) {
            System.out.println("UserInterface.confirmMessage was called from DatabaseChangesThread");
            return true;
        }

        /**
         * Method to ask for a choice among possibilities.
         * @param message the message to show.
         * @param title the title of the dialog with the query.
         * @param choices an array of choices to present, each in a button.
         * @param defaultChoice the default choice.
         * @return the index into the choices array that was selected.
         */
        public int askForChoice(Object message, String title, String [] choices, String defaultChoice) {
            throw new IllegalStateException();
        }

        /**
         * Method to ask for a line of text.
         * @param message the prompt message.
         * @param title the title of a dialog with the message.
         * @param def the default response.
         * @return the string (null if cancelled).
         */
        public String askForInput(Object message, String title, String def) { throw new IllegalStateException(); }

        /** For Pref */
        public void restoreSavedBindings(boolean initialCall) {
            System.out.println("UserInterface.restoreSavedBindings was called from DatabaseChangesThread");
        }
        public void finishPrefReconcilation(String libName, List<Pref.Meaning> meaningsToReconcile)
        {
            System.out.println("UserInterface.finishPrefReconcilation was called from DatabaseChangesThread");
            Pref.finishPrefReconcilation(meaningsToReconcile);
        }

        /**
         * Method to import the preferences from an XML file.
         * Prompts the user and reads the file.
         */
        public void importPrefs() {
            System.out.println("UserInterface.importPrefs was called from DatabaseChangesThread");
        }

        /**
         * Method to export the preferences to an XML file.
         * Prompts the user and writes the file.
         */
        public void exportPrefs() {
            System.out.println("UserInterface.exportPrefs was called from DatabaseChangesThread");
        }

        /**
         * Save current state of highlights and return its ID.
         */
        public int saveHighlights() { return -1; }
        
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

        /**
         * Method is called when initialization was finished.
         */
        public void finishInitialization() {}
    }
    
}
