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
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.MessagesStream;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.JobTree;
import com.sun.electric.tool.user.ui.TopLevel;
import java.awt.geom.Point2D;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import java.io.IOException;
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
//    private final ArrayList<EJob> finishedJobs = new ArrayList<EJob>();
    private final ArrayList<Client> serverConnections = new ArrayList<Client>();
    private final UserInterface redirectInterface = new UserInterfaceRedirect();
    private int numThreads;
    private final int maxNumThreads;
    private boolean runningChangeJob;
    private boolean guiChanged;
    private boolean signalledEThread;
    
    private Snapshot currentSnapshot = EDatabase.serverDatabase().getInitialSnapshot();
    
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
        if (User.isSbapshotLogging())
            initSnapshotLogging();
    }
    
    private int initThreads(int recommendedNumThreads) {
        int maxNumThreads = DEFAULT_NUM_THREADS;
        if (recommendedNumThreads > 0)
            maxNumThreads = recommendedNumThreads;
        Job.logger.logp(Level.FINE, CLASS_NAME, "initThreads", "maxNumThreads=" + maxNumThreads);
        return maxNumThreads;
    }

    void initSnapshotLogging() {
        int connectionId = serverConnections.size();
        StreamClient conn;
        lock();
        try {
            File tempFile = File.createTempFile("elec", ".slog");
            FileOutputStream out = new FileOutputStream(tempFile);
            System.out.println("Writing snapshot log to " + tempFile);
            ActivityLogger.logMessage("Writing snapshot log to " + tempFile);
            conn = new StreamClient(connectionId, null, new BufferedOutputStream(out), currentSnapshot);
            serverConnections.add(conn);
        } catch (IOException e) {
            System.out.println("Failed to create snapshot log file:" + e.getMessage());
            return;
        } finally {
            unlock();
        }
        System.out.println("Accepted connection " + connectionId);
        conn.start();
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
//                    finishedJobs.remove(j.ejob);
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
//            for (EJob ejob: finishedJobs) {
//                Job job = ejob.getJob();
//                if (job != null)
//                    jobsList.add(job);
//            }
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

    private void invokeEThread() {
        if (signalledEThread || startedJobs.size() >= maxNumThreads) return;
        if (!canDoIt()) return;
        if (startedJobs.size() < numThreads)
            databaseChangesMutex.signal();
        else
            new EThread(numThreads++);
        signalledEThread = true;
    }
    
//    private EJob selectTerminateIt() {
//        lock();
//        try {
//            for (int i = 0; i < finishedJobs.size(); i++) {
//                EJob ejob = finishedJobs.get(i);
//                if (ejob.state == EJob.State.CLIENT_DONE) continue;
////                finishedJobs.remove(i);
//                return ejob;
//            }
//        } finally {
//            unlock();
//        }
//        return null;
//    }
    
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
                currentSnapshot = ejob.newSnapshot;
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
//                if (Job.threadMode != Job.Mode.BATCH && ejob.client == null)
//                    finishedJobs.add(ejob);
                break;
            case CLIENT_DONE:
                assert oldState == EJob.State.SERVER_DONE;
//                if (ejob.clientJob.deleteWhenDone)
//                    finishedJobs.remove(ejob);
        }
        ejob.state = newState;
        Client.fireEJobEvent(ejob);
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
                int connectionId = serverConnections.size();
                StreamClient conn;
                lock();
                try {
                    conn = new StreamClient(connectionId, socket.getInputStream(), socket.getOutputStream(), currentSnapshot);
                    serverConnections.add(conn);
                } finally {
                    unlock();
                }
                System.out.println("Accepted connection " + connectionId);
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
//            for (;;) {
//                EJob ejob = selectTerminateIt();
//                if (ejob == null) break;
//                
//                Job.logger.logp(Level.FINE, CLASS_NAME, "run", "terminate {0}", ejob.jobName);
//                Job.runTerminate(ejob);
//                setEJobState(ejob, EJob.State.CLIENT_DONE, null);
//                Job.logger.logp(Level.FINE, CLASS_NAME, "run", "terminated {0}", ejob.jobName);
//            }
            Job.logger.logp(Level.FINE, CLASS_NAME, "run", "wantToRedoJobTree");
        }
        Job.logger.logp(Level.FINE, CLASS_NAME, "run", "EXIT");
    }
    
    public static void setUndoRedoStatus(boolean undoEnabled, boolean redoEnabled) {
        assert Job.jobManager instanceof ServerJobManager;
        Job.currentUI.showUndoRedoStatus(undoEnabled, redoEnabled);
        // transmit to connection
    } 
    
    EJob selectEJob(EJob finishedEJob) {
        EJob selectedEJob = null;
        lock();
        try {
            if (finishedEJob != null) {
                EJob.State state = finishedEJob.state;
                setEJobState(finishedEJob, EJob.State.SERVER_DONE, "done");
            }
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
        return selectedEJob;
    }
    
    /*private*/ static class UserInterfaceRedirect implements UserInterface
	{
    	private static void printStackTrace(String methodName) {
            if (true) return;
            if (!Job.getDebug()) return;
            System.out.println("UserInterface." + methodName + " was called from DatabaseChangesThread");
    		Exception e = new Exception();
			e.printStackTrace(System.out);
    	}

        public void startProgressDialog(String type, String filePath)
        {
//            printStackTrace("startProgressDialog");
            Job.currentUI.startProgressDialog(type, filePath);
        }

        /**
         * Method to stop the progress bar
         */
        public void stopProgressDialog()
        {
//            printStackTrace("stopProgressDialog");
            Job.currentUI.stopProgressDialog();
        }

        /**
         * Method to update the progress bar
         */
        public void setProgressValue(long pct)
        {
//            printStackTrace("updateProgressDialog");
            Job.currentUI.setProgressValue(pct);
        }

        /**
         * Method to set a text message in the progress dialog.
         * @param message
         */
        public void setProgressNote(String message)
        {
//            printStackTrace("setProgressNote");
            Job.currentUI.setProgressNote(message);
        }

        /**
         * Method to get text message in the progress dialgo.
         * @return
         */
        public String getProgressNote()
        {
//            printStackTrace("setProgressNote");
            return Job.currentUI.getProgressNote();
        }

		public EditWindow_ getCurrentEditWindow_() {
            printStackTrace("getCurrentEditWindow");
            return Job.currentUI.getCurrentEditWindow_();
        }
		public EditWindow_ needCurrentEditWindow_()
		{
            printStackTrace("needCurrentEditWindow");
			return null; 
		}
        /** Get current cell from current library */
		public Cell getCurrentCell()
        {
            printStackTrace("getCurrentCell");
			Library lib = Library.getCurrent();
			if (lib == null) return null;
			return lib.getCurCell();
        }
        /** Get current cell from current library */
		public Cell getCurrentCell(Library lib)
        {
            printStackTrace("getCurrentCell(lib)");
			return Job.currentUI.getCurrentCell(lib);
        }

		public Cell needCurrentCell()
		{
            printStackTrace("needCurrentCell");
            /** Current cell based on current library */
            Cell curCell = getCurrentCell();
            if (curCell == null)
            {
                System.out.println("There is no current cell for this operation.  To create one, use the 'New Cell' command from the 'Cell' menu.");
            }
            return curCell;
		}

		/**
		 * Method to set the current Cell in a Library.
		 * @param lib the library in which to set a current cell.
		 * @param curCell the new current Cell in the Library (can be null).
		 */
		public void setCurrentCell(Library lib, Cell curCell)
		{
            printStackTrace("setCurrentCell");
            Job.currentUI.setCurrentCell(lib, curCell);
		}

		public void repaintAllEditWindows() {
            printStackTrace("repaintAllEditWindows");
            Job.currentUI.repaintAllEditWindows();
        }

        public void loadComponentMenuForTechnology()
        {
            printStackTrace("loadComponentMenuForTechnology");
            Job.currentUI.loadComponentMenuForTechnology();
        }

        public void adjustReferencePoint(Cell cell, double cX, double cY) {
//            System.out.println("UserInterface.adjustReferencePoint was called from DatabaseChangesThread");
        };
		public void alignToGrid(Point2D pt) {
            printStackTrace("alignToGrid");
        }
		public int getDefaultTextSize() { return 14; }
//		public Highlighter getHighlighter();
		public EditWindow_ displayCell(Cell cell) { throw new IllegalStateException(); }

        public void termLogging(final ErrorLogger logger, boolean explain, boolean terminate) {
            Job.currentUI.termLogging(logger, explain, terminate);
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
            printStackTrace("reportLog");
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
            printStackTrace("confirmMessage");
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
        public int askForChoice(String message, String title, String [] choices, String defaultChoice) {
            throw new IllegalStateException(message);
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
            printStackTrace("restoreSavedBindings");
        }
        public void finishPrefReconcilation(String libName, List<Pref.Meaning> meaningsToReconcile)
        {
            printStackTrace("finishPrefReconcilation");
            Pref.finishPrefReconcilation(meaningsToReconcile);
        }

        /**
         * Method to import the preferences from an XML file.
         * Prompts the user and reads the file.
         */
        public void importPrefs() {
            printStackTrace("importPrefs");
        }

        /**
         * Method to export the preferences to an XML file.
         * Prompts the user and writes the file.
         */
        public void exportPrefs() {
            printStackTrace("exportPrefs");
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
    }
    
}
