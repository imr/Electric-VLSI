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
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.SwingUtilities;

/**
 *
 */
class ServerJobManager extends JobManager implements Observer {
    private final ReentrantLock lock = new ReentrantLock();
    
    private final ServerSocket serverSocket;
    private final ArrayList<ServerConnection> serverConnections = new ArrayList<ServerConnection>();
    /** mutex for database synchronization. */  private final Object databaseChangesMutex = new Object();
	/** database changes thread */              private final DatabaseChangesThread databaseChangesThread = new DatabaseChangesThread();
    /** number of examine jobs */               private int numExamine = 0;

    
    private Snapshot currentSnapshot = new Snapshot();
    /** True if preferences are accessible. */  private static boolean preferencesAccessible = true;
    
    /** Creates a new instance of JobPool */
    ServerJobManager() {
        serverSocket = null;
    }
    
    ServerJobManager(int socketPort) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(socketPort);
            System.out.println("ServerSocket waits for port " + socketPort);
        } catch (IOException e) {
            System.out.println("ServerSocket mode failure: " + e.getMessage());
        }
        this.serverSocket = serverSocket;
    }
    
   /** Add job to list of jobs */
    void addJob(EJob ejob, boolean onMySnapshot) {
        synchronized (databaseChangesMutex) {
            if (waitingJobs.isEmpty())
                databaseChangesMutex.notify();
            if (onMySnapshot)
                waitingJobs.add(0, ejob);
            else
                waitingJobs.add(ejob);
            if (!Job.BATCHMODE && ejob.jobType == Job.Type.CHANGE) {
                Job.getUserInterface().invokeLaterBusyCursor(true);
            }
            if (ejob.getJob() != null && ejob.getJob().getDisplay()) {
                Job.getUserInterface().wantToRedoJobTree();
            }
        }
    }
    
    /** Remove job from list of jobs */
    void removeJob(Job j) {
        synchronized (databaseChangesMutex) {
            if (j.started) {
                for (Iterator<EJob> it = startedJobs.iterator(); it.hasNext(); ) {
                    EJob ejob = it.next();
                    if (ejob.getJob() == j) {
                        it.remove();
                    }
                }
            } else {
                if (!waitingJobs.isEmpty() && waitingJobs.get(0).getJob() == j)
                    databaseChangesMutex.notify();
                for (Iterator<EJob> it = waitingJobs.iterator(); it.hasNext(); ) {
                    EJob ejob = it.next();
                    if (ejob.getJob() == j) {
                        it.remove();
                    }
                }
            }
            //System.out.println("Removed Job "+j+", index was "+index+", numStarted now="+numStarted+", allJobs="+allJobs.size());
            if (j.getDisplay()) {
                Job.getUserInterface().wantToRedoJobTree();
            }
        }
    }
    
    /** get all jobs iterator */
    Iterator<Job> getAllJobs() {
        synchronized (databaseChangesMutex) {
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
    public synchronized void update(Observable o, Object arg) {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof EThread)
            ((EThread)currentThread).print((String)arg);
    }
        
    //--------------------------PRIVATE JOB METHODS--------------------------

   /** Run gets called after the calling thread calls our start method */
   private void runJob(EThread thread, EJob ejob) {
        Job job = ejob.getJob();
        job.startTime = System.currentTimeMillis();

        if (job.getDebug() && ejob.connection == null && ejob.jobType != Job.Type.EXAMINE && ejob.jobType != Job.Type.REMOTE_EXAMINE) {
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
            if (ejob.jobType != Job.Type.EXAMINE) {
                thread.setCanChanging(true);
                thread.setJob(ejob);
            }
			if (ejob.jobType == Job.Type.CHANGE)	{
                Undo.startChanges(job.tool, ejob.jobName, ejob.savedHighlights, ejob.savedHighlightsOffset);
                if (!job.getClass().getName().endsWith("InitDatabase"))
                    preferencesAccessible = false;
            }
            try {
                if (!serverJob.doIt())
                    throw new JobException();
            } catch (JobException e) {
                jobException = e;
            }
			if (ejob.jobType == Job.Type.CHANGE)	{
                preferencesAccessible = true;
                Undo.endChanges();
            }
		} catch (Throwable e) {
            jobException = e;
            e.printStackTrace(System.err);
            ActivityLogger.logException(e);
            if (e instanceof Error) throw (Error)e;
		} finally {
			if (ejob.jobType == Job.Type.EXAMINE)
			{
				endExamine(serverJob);
			} else {
				thread.setCanChanging(false);
                thread.setJob(null);
                if (Job.threadMode == Job.Mode.SERVER)
                    updateSnapshot();
			}
		}
        if (jobException == null)
            ejob.serializeResult();
        else
            ejob.serializeExceptionResult(jobException);
        if (ejob.connection != null) {
            assert Job.threadMode == Job.Mode.SERVER;
            ejob.connection.sendTerminateJob(ejob);
        } else {
            SwingUtilities.invokeLater(new JobTerminateRun(ejob));
        }
    }

    private EJob selectConnection() {
        synchronized (databaseChangesMutex) {
            for (;;) {
                // Search for examine
                if (!waitingJobs.isEmpty()) {
                    EJob ejob = waitingJobs.get(0);
                    Job job = ejob.getJob();
                    if (ejob.jobType == Job.Type.EXAMINE || job != null && (job.scheduledToAbort || job.aborted))
                        return waitingJobs.remove(0);
                }
                if (numExamine == 0) {
                    if (!waitingJobs.isEmpty())
                        return waitingJobs.remove(0);
//                    if (threadMode == Mode.SERVER && serverJobManager != null) {
//                        for (ServerConnection conn: serverJobManager.serverConnections) {
//                            if (conn.peekJob() != null)
//                                return conn.getJob();
//                        }
//                    }
                }
                try {
                    databaseChangesMutex.wait();
                } catch (InterruptedException e) {}
            }
        }
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
    
     private void endExamine(Job j) {
        synchronized (databaseChangesMutex) {
            numExamine--;
            //System.out.println("EndExamine Job "+j+", numExamine now="+numExamine+", allJobs="+allJobs.size());
            if (numExamine == 0)
                databaseChangesMutex.notify();
        }
    }
    
    private boolean isChangeJobQueuedOrRunning() {
        synchronized (databaseChangesMutex) {
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
    }
    
    private void lock() { lock.lock(); }
    private void unlock() { lock.unlock(); }
    
    public void runLoop() {
        if (serverSocket == null) return;
        TopLevel.getMessagesStream().addObserver(this);
        try {
            // Wait for connections
            for (;;) {
                Socket socket = serverSocket.accept();
                ServerConnection conn;
                lock.lock();
                try {
                    conn = new ServerConnection(serverConnections.size(), socket, currentSnapshot);
                    serverConnections.add(conn);
                } finally {
                    lock.unlock();
                }
                System.out.println("Accepted connection " + conn.connectionId);
                conn.start();
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }
    
	/**
	 * Thread which execute all database change Jobs.
	 */
	class DatabaseChangesThread extends EThread
	{
        // Job Management
		DatabaseChangesThread() {
			super("Database");
            setUserInterface(new ServerJobManager.UserInterfaceRedirect());
			start();
		}

        public void run() {
            setCanComputeBounds(true);
            setCanComputeNetlist(true);
            for (;;) {
                Job.currentUI.invokeLaterBusyCursor(isChangeJobQueuedOrRunning());
                EJob ejob = selectConnection();
                ServerConnection connection = ejob.connection;
                if (connection != null) {
                    Throwable e = ejob.deserialize();
                    if (e != null) {
                        ejob.serializeExceptionResult(e);
                        e.printStackTrace();
                        ejob.connection.sendTerminateJob(ejob);
                        continue;
                    }
                } else {
                    if (ejob.clientJob.scheduledToAbort || ejob.clientJob.aborted)
                        continue;
                }
                Job job = ejob.serverJob;
                if (ejob.jobType == Job.Type.EXAMINE) {
                    synchronized (databaseChangesMutex) {
                        job.started = true;
                        startedJobs.add(ejob);
                        numExamine++;
                    }
                    //System.out.println("Started Job "+job+", numStarted="+numStarted+", numExamine="+numExamine+", allJobs="+allJobs.size());
                    Thread t = new ExamineThread(ejob);
                    t.start();
                } else { 
                    synchronized (databaseChangesMutex) {
                        assert numExamine == 0;
                        job.started = true;
                        startedJobs.add(ejob);
                    }
                    runJob(this, ejob);
                }
            }
        }
	}

    private class ExamineThread extends EThread {
        private EJob ejob;
        
        ExamineThread(EJob ejob) {
            super(ejob.jobName);
            setUserInterface(Job.currentUI);
            assert ejob.jobType == Job.Type.EXAMINE;
            this.ejob = ejob;
        }
        
        public void run() {
//            System.out.println("Started ExamineThread " + this);
            runJob(this, ejob);
        }
    }
    
    private class JobTerminateRun implements Runnable {
        private EJob ejob;
        
        JobTerminateRun(EJob ejob) {
            this.ejob = ejob;
        }
        
        public void run() {
            Job.runTerminate(ejob);
            Job job = ejob.clientJob;
            
            // delete
            if (job.deleteWhenDone) {
                removeJob(job);
                Job.currentUI.invokeLaterBusyCursor(isChangeJobQueuedOrRunning());
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
		public void repaintAllEditWindows() {
            System.out.println("UserInterface.repaintAllEditWindow was called from DatabaseChangesThread");
        }
        
        public void adjustReferencePoint(Cell cell, double cX, double cY) {
            System.out.println("UserInterface.adjustReferencePoint was called from DatabaseChangesThread");
        };
		public void alignToGrid(Point2D pt) {
            System.out.println("UserInterface.alignToGrid was called from DatabaseChangesThread");
        }
		public int getDefaultTextSize() { return 14; }
//		public Highlighter getHighlighter();
		public EditWindow_ displayCell(Cell cell) {
            System.out.println("UserInterface.displayCell was called from DatabaseChangesThread");
            return null;
        }

		public void wantToRedoErrorTree() {
//            System.out.println("UserInterface.wantToRedoErrorTree was called from DatabaseChangesThread");
            Job.currentUI.wantToRedoErrorTree();
        }
        public void wantToRedoJobTree() {
//            System.out.println("UserInterface.wantToRedoJobTree was called from DatabaseChangesThread");
            Job.currentUI.wantToRedoJobTree();
        }

        public void termLogging(final ErrorLogger logger, boolean explain) {
            System.out.println("UserInterface.termLogging was called from DatabaseChangesThread");
            Job.currentUI.termLogging(logger, explain);
        }

        /* Job related **/
        public void invokeLaterBusyCursor(final boolean state){
//            System.out.println("UserInterface.invokeLaterBusyCursor was called from DatabaseChangesThread");
            Job.currentUI.invokeLaterBusyCursor(state);
        }
        public void setBusyCursor(boolean state) {
            System.out.println("UserInterface.setBusyCursor was called from DatabaseChangesThread");
        }

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
        public int askForChoice(Object message, String title, String [] choices, String defaultChoice)
        {
            System.out.println("UserInterface.askForChoice was called from DatabaseChangesThread");
        	System.out.println(message + " CHOOSING " + defaultChoice);
        	for(int i=0; i<choices.length; i++) if (choices[i].equals(defaultChoice)) return i;
        	return 0;
        }

        /**
         * Method to ask for a line of text.
         * @param message the prompt message.
         * @param title the title of a dialog with the message.
         * @param def the default response.
         * @return the string (null if cancelled).
         */
        public String askForInput(Object message, String title, String def) {
            System.out.println("UserInterface.askForInput was called from DatabaseChangesThread");
            return def;
        }

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

        /** For TextWindow */
        public String [] getEditedText(Cell cell) {
            System.out.println("UserInterface.getEditedText was called from DatabaseChangesThread");
            return null;
        }
        public void updateText(Cell cell, String [] strings) {
            System.out.println("UserInterface.updateText was called from DatabaseChangesThread");
        }
	}
}
