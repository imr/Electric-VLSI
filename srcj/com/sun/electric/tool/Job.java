/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Job.java
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
import com.sun.electric.database.EObjectOutputStream;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.SnapshotReader;
import com.sun.electric.database.change.DatabaseChangeEvent;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.CantEditException;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Toolkit;
import java.awt.geom.Point2D;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * Jobs are processes that will run in the background, such as 
 * DRC, NCC, Netlisters, etc.  Each Job gets placed in a Job
 * window, and reports its status.  A job can be cancelled.
 * 
 * <p>To start a new job, do:
 * <p>Job job = new Job(name);
 * <p>job.start();
 * 
 * <p>The extending class must implement doIt(), which does the job and
 * returns status (true on success; false on failure);
 * and getProgress(), which returns a string indicating the current status.
 * Job also contains boolean abort, which gets set when the user decides
 * to abort the Job.  The extending class' code should check abort when/where
 * applicable.
 *
 * <p>Note that if your Job calls methods outside of this thread
 * that access shared data, those called methods should be synchronized.
 *
 * @author  gainsley
 */
public abstract class Job implements Serializable {

    private static boolean DEBUG = false;
    private static boolean GLOBALDEBUG = false;
    private static Mode threadMode;
    private static int socketPort = 35742; // socket port for client/server
    public static boolean BATCHMODE = false; // to run it in batch mode
    public static boolean NOTHREADING = false;             // to turn off Job threading
    public static boolean LOCALDEBUGFLAG; // Gilda's case
    private static final String CLASS_NAME = Job.class.getName();
    static final Logger logger = Logger.getLogger("com.sun.electric.tool.job");
    
    /**
	 * Method to tell whether Electric is running in "debug" mode.
	 * If the program is started with the "-debug" switch, debug mode is enabled.
	 * @return true if running in debug mode.
	 */
    public static boolean getDebug() { return GLOBALDEBUG; }

    public static void setDebug(boolean f) { GLOBALDEBUG = f; }

    /**
     * Mode of Job manager
     */
    public static enum Mode {
        /** Full screen run of Electric. */         FULL_SCREEN,
        /** Batch mode. */                          BATCH,
        /** Server side. */                         SERVER,
        /** Client side. */                         CLIENT;
    }
    
    /**
	 * Type is a typesafe enum class that describes the type of job (CHANGE or EXAMINE).
	 */
	public static enum Type {
		/** Describes a database change. */             CHANGE,
		/** Describes a database undo/redo. */          UNDO,
		/** Describes a database examination. */        EXAMINE,
		/** Describes a remote database examination. */	REMOTE_EXAMINE;
	}


	/**
	 * Priority is a typesafe enum class that describes the priority of a job.
	 */
	public static enum Priority {
		/** The highest priority: from the user. */		USER,
		/** Next lower priority: visible changes. */	VISCHANGES,
		/** Next lower priority: invisible changes. */	INVISCHANGES,
		/** Lowest priority: analysis. */				ANALYSIS;
	}

    public static Iterator<Job> getDatabaseThreadJobs() {
        if (threadMode == Mode.CLIENT) {
            ArrayList<Job> jobs = new ArrayList<Job>(startedJobs);
            jobs.addAll(waitingJobs);
            return jobs.iterator();
        }
        return getAllJobs();
    }

	/**
	 * Thread which execute all database change Jobs.
	 */
	static class DatabaseChangesThread extends EThread
	{
        // Job Management
		DatabaseChangesThread() {
			super("Database");
            setUserInterface(new UserInterfaceRedirect());
			start();
		}

        public void run() {
            setCanComputeBounds(true);
            setCanComputeNetlist(true);
            for (;;) {
                UserInterface userInterface = Job.getUserInterface();
                if (userInterface != null)
                    userInterface.invokeLaterBusyCursor(isChangeJobQueuedOrRunning());
                ServerConnection connection = selectConnection();
                Job job;
                if (connection != null) {
                    job = deserializeJob(connection);
                    if (job == null)
                        continue;
                } else {
                    synchronized (databaseChangesMutex) {
                        job = waitingJobs.remove(0);
                    }
                    if (job.scheduledToAbort || job.aborted)
                        continue;
                }
                if (job.jobType == Type.EXAMINE) {
                    synchronized (databaseChangesMutex) {
                        job.started = true;
                        startedJobs.add(job);
                        numExamine++;
                    }
                    //System.out.println("Started Job "+job+", numStarted="+numStarted+", numExamine="+numExamine+", allJobs="+allJobs.size());
                    Thread t = new ExamineThread(job);
                    t.start();
                } else { 
                    synchronized (databaseChangesMutex) {
                        assert numExamine == 0;
                        job.started = true;
                        startedJobs.add(job);
                    }
                    job.run();
                }
            }
        }
        
	}

    private static class ExamineThread extends EThread {
        private Job job;
        
        ExamineThread(Job job) {
            super(job.jobName);
            setUserInterface(currentUI);
            assert job.jobType == Type.EXAMINE;
            this.job = job;
 //           job.thread = this;
        }
        
        public void run() {
//            System.out.println("Started ExamineThread " + this);
            job.run();
        }
    }
    
        /** started jobs */                         private static final ArrayList<Job> startedJobs = new ArrayList<Job>();
        /** waiting jobs */                         private static final ArrayList<Job> waitingJobs = new ArrayList<Job>();
        /** number of examine jobs */               private static int numExamine = 0;

	/** default execution time in milis */      private static final int MIN_NUM_SECONDS = 60000;
	/** database changes thread */              static DatabaseChangesThread databaseChangesThread;
    /** mutex for database synchronization. */  static final Object databaseChangesMutex = new Object();
	/** changing job */                         private static Job changingJob;
    /** server job manager */                   private static ServerJobManager serverJobManager; 
    /** stream for cleint to send Jobs. */      private static DataOutputStream clientOutputStream;
    /** True if preferences are accessible. */  private static boolean preferencesAccessible = true;
    /** Count of started Jobs. */               private static int numStarted;

    /** id unique in this client. */            private int jobId;
    /** delete when done if true */             private boolean deleteWhenDone;
    /** display on job list if true */          private boolean display;
    
    // Job Status
    /** job start time */                       protected long startTime;
    /** job end time */                         protected long endTime;
    /** was job started? */                     private boolean started;
    /** is job finished? */                     private boolean finished;
    /** thread aborted? */                      private boolean aborted;
    /** schedule thread to abort */             private boolean scheduledToAbort;
	/** report execution time regardless MIN_NUM_SECONDS */
												private boolean reportExecution = false;
    /** name of job */                          String jobName;
    /** tool running the job */                 private Tool tool;
    /** type of job (change or examine) */      Type jobType;
//    /** priority of job */                      private Priority priority;
//    /** bottom of "up-tree" of cells affected */private Cell upCell;
//    /** top of "down-tree" of cells affected */ private Cell downCell;
//    /** status */                               private String status = null;
    /** progress */                             private String progress = null;
    /** list of saved Highlights */             private transient List<Object> savedHighlights;
    /** saved Highlight offset */               private transient Point2D savedHighlightsOffset;
    /** Fields changed on server side. */       transient ArrayList<Field> changedFields;
//    /** Thread job will run in (null for new thread) */
//                                                private transient Thread thread;
    /** Connection from which we accepted */    transient ServerConnection connection = null;
    transient EJob ejob;
	private static UserInterface currentUI;
    private static Job clientJob;

    public static void setThreadMode(Mode mode, UserInterface userInterface) {
        threadMode = mode;
        BATCHMODE = (mode == Mode.BATCH);
        currentUI = userInterface;
    }
   
    public static void initJobManager() {
        switch (threadMode) {
            case FULL_SCREEN:
            case BATCH:
                databaseChangesThread = new DatabaseChangesThread();
                break;
            case SERVER:
                databaseChangesThread = new DatabaseChangesThread();
                serverJobManager = new ServerJobManager(socketPort);
                TopLevel.getMessagesStream().addObserver(serverJobManager);
                serverJobManager.start();
                break;
            case CLIENT:
                logger.finer("setThreadMode");
                clientLoop(socketPort);
                // unreachable
                break;
        }
    }
    
    
    public static Mode getRunMode() { return threadMode; }
    
    public Job() {}
                                                
    /**
	 * Constructor creates a new instance of Job.
	 * @param jobName a string that describes this Job.
	 * @param tool the Tool that originated this Job.
	 * @param jobType the Type of this Job (EXAMINE or CHANGE).
	 * @param upCell the Cell at the bottom of a hierarchical "up cone" of change.
	 * If this and "downCell" are null, the entire database is presumed.
	 * @param downCell the Cell at the top of a hierarchical "down tree" of changes/examinations.
	 * If this and "upCell" are null, the entire database is presumed.
	 * @param priority the priority of this Job.
	 */
    public Job(String jobName, Tool tool, Type jobType, Cell upCell, Cell downCell, Priority priority) {
		if (downCell != null) upCell = null; // downCell not implemented. Lock whole database
		this.jobName = jobName;
		this.tool = tool;
		this.jobType = jobType;
//		this.priority = priority;
//		this.upCell = upCell;
//		this.downCell = downCell;
        this.display = true;
        this.deleteWhenDone = true;
        startTime = endTime = 0;
        started = finished = aborted = scheduledToAbort = false;
//        thread = null;
        savedHighlights = new ArrayList<Object>();
        if (jobType == Job.Type.CHANGE || jobType == Job.Type.UNDO)
            saveHighlights();
	}
	
    /**
     * Start a job. By default displays Job on Job List UI, and
     * delete Job when done.  Jobs that have state the user would like to use
     * after the Job finishes (like DRC, NCC, etc) should call
     * <code>startJob(true, true)</code>.
     */
	public void startJob()
	{
        startJob(!BATCHMODE, true);
    }
	
    /**
     * Start a job on snapshot obtained at the end of current job. By default displays Job on Job List UI, and
     * delete Job when done.
     */
	public void startJobOnMyResult()
	{
        startJob(!BATCHMODE, true, true);
    }
	
    /**
     * Start the job by placing it on the JobThread queue.
     * If <code>display</code> is true, display job on Job List UI.
     * If <code>deleteWhenDone</code> is true, Job will be deleted
     * after it is done (frees all data and references it stores/created)
     * @param deleteWhenDone delete when job is done if true, otherwise leave it around
     */
    public void startJob(boolean display, boolean deleteWhenDone) {
        startJob(display, deleteWhenDone, false);
    }
    
    /**
     * Start the job by placing it on the JobThread queue.
     * If <code>display</code> is true, display job on Job List UI.
     * If <code>deleteWhenDone</code> is true, Job will be deleted
     * after it is done (frees all data and references it stores/created)
     * @param deleteWhenDone delete when job is done if true, otherwise leave it around
     */
    private void startJob(boolean display, boolean deleteWhenDone, boolean onMySnapshot)
    {
        this.display = display;
        this.deleteWhenDone = deleteWhenDone;

        if (threadMode == Mode.CLIENT) {
            assert SwingUtilities.isEventDispatchThread() || Thread.currentThread() instanceof ExamineThread;
            if (onMySnapshot)
                waitingJobs.add(0, this);
            else
                waitingJobs.add(this);
            if (this.getDisplay()) {
                Job.getUserInterface().wantToRedoJobTree();
            }
            SwingUtilities.invokeLater(clientInvoke);
            return;
      }

        if (NOTHREADING) {
            // turn off threading if needed for debugging
            Job.getUserInterface().setBusyCursor(true);
            run();
            Job.getUserInterface().setBusyCursor(false);
        } else {
            addJob(this, onMySnapshot);
        }
    }

    /**
     * Method to remember that a field variable of the Job has been changed by the doIt() method.
     * @param variableName the name of the variable that changed.
     */
    protected void fieldVariableChanged(String variableName) {
        try {
            Field fld = getClass().getDeclaredField(variableName);
            fld.setAccessible(true);
            changedFields.add(fld);
        } catch (NoSuchFieldException e) {
            e.printStackTrace(System.out);
        }
    }

    Job testSerialization() {
        byte[] bytes = serialize();
        if (bytes == null) return null;
        Job newJob;
        try {
            ByteArrayInputStream byteInputStream = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(byteInputStream);
            return (Job)in.readObject();
        } catch (Throwable e) {
            System.out.println("Can't deserialize Job " + this);
            e.printStackTrace(System.out);
            return null;
        }
    }
    
    byte[] serialize() {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream out = new EObjectOutputStream(byteStream);
            out.writeObject(this);
            out.flush();
            return byteStream.toByteArray();
        } catch (Throwable e) {
            System.out.println("Can't serialize Job " + this);
            e.printStackTrace(System.out);
            return null;
        }
    }
    
	//--------------------------ABSTRACT METHODS--------------------------
    
    /** This is the main work method.  This method should
     * perform all needed tasks.
     * @throws JobException TODO
     */
    public abstract boolean doIt() throws JobException;
    
    /**
     * This method executes in the Client side after termination of doIt method.
     * This method should perform all needed termination actions.
     * @param jobException null if doIt terminated normally, otherwise exception thrown by doIt.
     */
    public void terminateIt(Throwable jobException) {
        if (jobException == null)
            terminateOK();
        else
            terminateFail(jobException);
    }
    
    /**
     * This method executes in the Client side after normal termination of doIt method.
     * This method should perform all needed termination actions.
     */
    public void terminateOK() {}
    
    /**
     * This method executes in the Client side after exceptional termination of doIt method.
     * @param jobException null exception thrown by doIt.
     */
    public void terminateFail(Throwable jobException) {
        if (jobException instanceof CantEditException) {
            ((CantEditException)jobException).presentProblem();
        } else if (jobException instanceof JobException) {
            System.out.println(jobException.getMessage());
        }
    }
    
    //--------------------------PRIVATE JOB METHODS--------------------------

    private static ServerConnection selectConnection() {
        synchronized (databaseChangesMutex) {
            for (;;) {
                // Search for examine
                if (!waitingJobs.isEmpty()) {
                    Job job = waitingJobs.get(0);
                    if (job.scheduledToAbort || job.aborted || job.jobType == Type.EXAMINE)
                        return null;
                }
                if (numExamine == 0) {
                    if (!waitingJobs.isEmpty())
                        return null;
                    if (threadMode == Mode.SERVER && serverJobManager != null) {
                        for (ServerConnection conn: serverJobManager.serverConnections) {
                            if (conn.peekJob() != null)
                                return conn;
                        }
                    }
                }
                try {
                    databaseChangesMutex.wait();
                } catch (InterruptedException e) {}
            }
        }
    }
    
    private static Job deserializeJob(ServerConnection connection) {
        EJob ejob = connection.getJob();
        try {
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(ejob.serializedJob));
            Job job = (Job)in.readObject();
            assert job.jobId == ejob.jobId;
            job.connection = connection;
            in.close();
            return job;
        } catch (Throwable e) {
            e.printStackTrace();
            connection.sendTerminateJob(ejob.jobId, serializeException(e));
            return null;
        }
    }
    
    /** Add job to list of jobs */
    private static void addJob(Job j, boolean onMySnapshot) {
        synchronized (databaseChangesMutex) {
            if (waitingJobs.isEmpty())
                databaseChangesMutex.notify();
            if (onMySnapshot)
                waitingJobs.add(0, j);
            else
                waitingJobs.add(j);
            if (!BATCHMODE && j.jobType == Type.CHANGE) {
                Job.getUserInterface().invokeLaterBusyCursor(true);
            }
            if (j.getDisplay()) {
                Job.getUserInterface().wantToRedoJobTree();
            }
        }
    }
    
    /** Remove job from list of jobs */
    private static void removeJob(Job j) {
        synchronized (databaseChangesMutex) {
            if (j.started) {
                startedJobs.remove(j);
            } else {
                if (!waitingJobs.isEmpty() && waitingJobs.get(0) == j)
                    databaseChangesMutex.notify();
                waitingJobs.remove(j);
            }
            //System.out.println("Removed Job "+j+", index was "+index+", numStarted now="+numStarted+", allJobs="+allJobs.size());
            if (j.getDisplay()) {
                Job.getUserInterface().wantToRedoJobTree();
            }
        }
    }
    
    private static void endExamine(Job j) {
        synchronized (databaseChangesMutex) {
            numExamine--;
            //System.out.println("EndExamine Job "+j+", numExamine now="+numExamine+", allJobs="+allJobs.size());
            if (numExamine == 0)
                databaseChangesMutex.notify();
        }
    }
    
     private static boolean isChangeJobQueuedOrRunning() {
        synchronized (databaseChangesMutex) {
            for (Job j: startedJobs) {
                if (j.finished) continue;
                if (j.jobType == Type.CHANGE) return true;
            }
            for (Job j: waitingJobs) {
                if (j.jobType == Type.CHANGE) return true;
            }
            return false;
        }
    }
    
	//--------------------------PROTECTED JOB METHODS-----------------------
	/** Set reportExecution flag on/off */
	protected void setReportExecutionFlag(boolean flag)
	{
		reportExecution = flag;
	}
    //--------------------------PUBLIC JOB METHODS--------------------------

    /** Run gets called after the calling thread calls our start method */
    public void run() {
        startTime = System.currentTimeMillis();

        Job serverJob = this;
        String className = getClass().getName();
        if (getDebug() && connection == null && jobType != Type.EXAMINE && jobType != Type.REMOTE_EXAMINE) {
            serverJob = testSerialization();
            if (serverJob != null) {
                // transient fields ???
//                serverJob.savedHighlights = this.savedHighlights;
//                serverJob.savedHighlightsOffset = this.savedHighlightsOffset;
            } else {
                serverJob = this;
            }
        }
        serverJob.changedFields = new ArrayList<Field>();
        
        if (DEBUG) System.out.println(jobType+" Job: "+jobName+" started");

//        Cell cell = Job.getUserInterface().getCurrentCell();
//        if (connection == null)
//            ActivityLogger.logJobStarted(jobName, jobType, cell, savedHighlights, savedHighlightsOffset);
        Throwable jobException = null;
		try {
            if (jobType != Type.EXAMINE) {
                databaseChangesThread.setCanChanging(true);
                databaseChangesThread.setJob(this);
            }
			if (jobType == Type.CHANGE)	{
                Undo.startChanges(tool, jobName, /*upCell,*/ savedHighlights, savedHighlightsOffset);
                if (!className.endsWith("InitDatabase"))
                    preferencesAccessible = false;
            }
            try {
                if (!serverJob.doIt())
                    throw new JobException();
            } catch (JobException e) {
                jobException = e;
            }
			if (jobType == Type.CHANGE)	{
                preferencesAccessible = true;
                Undo.endChanges();
            }
		} catch (Throwable e) {
            jobException = e;
            e.printStackTrace(System.err);
            ActivityLogger.logException(e);
            if (e instanceof Error) throw (Error)e;
		} finally {
			if (jobType == Type.EXAMINE)
			{
				endExamine(this);
			} else {
				databaseChangesThread.setCanChanging(false);
                databaseChangesThread.setJob(null);
                if (threadMode == Mode.SERVER)
                    serverJobManager.updateSnapshot();
			}
		}
        byte[] result = null;
        if (jobException == null) {
            try {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                ObjectOutputStream out = new EObjectOutputStream(byteStream);
                out.writeObject(null); // No exception
                out.writeInt(serverJob.changedFields.size());
                for (Field f: serverJob.changedFields) {
                    Object value = f.get(serverJob);
                    out.writeUTF(f.getName());
                    out.writeObject(value);
                }
                out.close();
                result = byteStream.toByteArray();
            } catch (Throwable e) {
                jobException = e;
            }
        }
        if (jobException != null)
            result = serializeException(jobException);
        if (connection != null) {
            assert threadMode == Mode.SERVER;
            connection.sendTerminateJob(serverJob.jobId, result);
        } else {
            SwingUtilities.invokeLater(new JobTerminateRun(this, result));
        }
    }

    private static byte[] serializeException(Throwable e) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream out = new EObjectOutputStream(byteStream);
            out.writeObject(e);
            out.writeInt(0);
            out.close();
            return byteStream.toByteArray();
        } catch (Throwable ee) {
            return new byte[0];
        }
    }

//    /**
//     * Method to end the current batch of changes and start another.
//     * Besides starting a new batch, it cleans up the constraint system, and 
//     */
//    protected void flushBatch()
//    {
//		if (jobType != Type.CHANGE)	return;
//		Undo.endChanges();
//		Undo.startChanges(tool, jobName, /*upCell,*/ savedHighlights, savedHighlightsOffset);
//    }

    protected synchronized void setProgress(String progress) {
        this.progress = progress;
        Job.getUserInterface().wantToRedoJobTree();
    }        
    
    private synchronized String getProgress() { return progress; }

    /** Return run status */
    public boolean isFinished() { return finished; }
    
    /** Tell thread to abort. Extending class should check
     * abort when/where applicable
     */
    public synchronized void abort() {
        if (aborted) { 
            System.out.println("Job already aborted: "+getStatus());
            return;
        }
        scheduledToAbort = true;
        Job.getUserInterface().wantToRedoJobTree();
    }

    /** Save current Highlights */
    private void saveHighlights() {
        savedHighlights.clear();

        // for now, just save highlights in current window
        UserInterface ui = Job.getUserInterface();
        EditWindow_ wnd = ui.getCurrentEditWindow_();
        if (wnd == null) return;

        savedHighlights = wnd.saveHighlightList();
        savedHighlightsOffset = wnd.getHighlightOffset();
    }

	/** Confirmation that thread is aborted */
    protected synchronized void setAborted() { aborted = true; Job.getUserInterface().wantToRedoJobTree(); }
    /** get scheduled to abort status */
    protected synchronized boolean getScheduledToAbort() { return scheduledToAbort; }
    /**
     * Method to get abort status. Please leave this function as private.
     * To retrieve abort status from another class, use checkAbort which also
     * checks if job is scheduled to be aborted.
     * @return
     */
    private synchronized boolean getAborted() { return aborted; }
    /** get display status */
    public boolean getDisplay() { return display; }
    /** get deleteWhenDone status */
    public boolean getDeleteWhenDone() { return deleteWhenDone; }

	/**
	* Check if we are scheduled to abort. If so, print msg if non null
	 * and return true.
     * This is because setAbort and getScheduledToAbort
     * are protected in Job.
	 * @return true on abort, false otherwise. If job is scheduled for abort or aborted.
     * and it will report it to std output
	 */
	public boolean checkAbort()
	{
		if (getAborted()) return (true);
		boolean scheduledAbort = getScheduledToAbort();
		if (scheduledAbort)
		{
			System.out.println(this + ": aborted");  // should call Job.toString()
            setReportExecutionFlag(true); // Force reporting
			setAborted();                   // Job has been aborted
		}
		return scheduledAbort;
	}

    /** get all jobs iterator */
    public static Iterator<Job> getAllJobs() {
        synchronized (databaseChangesMutex) {
            ArrayList<Job> jobsList = new ArrayList<Job>(startedJobs);
            jobsList.addAll(waitingJobs);
            return jobsList.iterator();
        }
    }
    
    /** get status */
    public String getStatus() {
		if (!started) return "waiting";
        if (aborted) return "aborted";
        if (finished) return "done";
        if (scheduledToAbort) return "scheduled to abort";
        if (getProgress() == null) return "running";
        return getProgress();
    }

    /** Remove job from Job list if it is done */
    public boolean remove() {
        if (!finished && !aborted) {
            //System.out.println("Cannot delete running jobs.  Wait till finished or abort");
            return false;
        }
        removeJob(this);
        return true;
    }

    /**
     * Unless you need to code to execute quickly (such as in the GUI thread)
     * you should be using a Job to examine the database instead of this method.
     * The suggested format is as follows:
     * <p>
     * <pre><code>
     * if (Job.acquireExamineLock(block)) {
     *     try {
     *         // do stuff
     *         Job.releaseExamineLock();    // release lock
     *     } catch (Error e) {
     *         Job.releaseExamineLock();    // release lock if error/exception thrown
     *         throw e;                     // rethrow error/exception
     *     }
     * }
     * </code></pre>
     * <p>
     * This method tries to acquire a lock to allow the current thread to
     * safely examine the database.
     * If "block" is true, this call blocks until a lock is acquired.  If block
     * is false, this call returns immediately, returning true if a lock was
     * acquired, or false if not.  You must call Job.releaseExamineLock
     * when done if you acquired a lock.
     * <p>
     * Subsequent nested calls to this method from the same thread must
     * have matching calls to releaseExamineLock.
     * @param block True to block (wait) until lock can be acquired. False to
     * return immediately with a return value that denotes if a lock was acquired.
     * @return true if lock acquired, false otherwise. If block is true,
     * the return value is always true.
     * @see #releaseExamineLock()
     * @see #invokeExamineLater(Runnable, Object)
     */
    public static synchronized boolean acquireExamineLock(boolean block) {
        return true;
//        if (true) return true;      // disabled
//        Thread thread = Thread.currentThread();
//
//        // first check to see if we already have the lock
//        Job job = databaseChangesThread.getJob(thread);
//        if (job != null) {
//            assert(job instanceof InthreadExamineJob);
//            ((InthreadExamineJob)job).incrementLockCount();
//            return true;
//        }
//        // create new Job to get examine lock
//        InthreadExamineJob dummy = new InthreadExamineJob();
//        ((Job)dummy).display = false;
//        ((Job)dummy).deleteWhenDone = true;
//        ((Job)dummy).thread = thread;
//        return databaseChangesThread.addInthreadExamineJob(dummy, block);
    }

    /**
     * Release the lock to examine the database.  The lock is the lock
     * associated with the current thread.  This should only be called if a
     * lock was acquired for the current thread.
     * @see #acquireExamineLock(boolean)
     * @see #invokeExamineLater(Runnable, Object)
     */
    public static synchronized void releaseExamineLock() {
//        if (true) return;      // disabled
//        Job dummy = databaseChangesThread.getJob(Thread.currentThread());
//        assert(dummy != null);
//        assert(dummy instanceof InthreadExamineJob);
//        InthreadExamineJob job = (InthreadExamineJob)dummy;
//        job.decrementLockCount();
//        if (job.getLockCount() == 0) {
//            databaseChangesThread.endExamine(job);
//            databaseChangesThread.removeJob(job);
//        }
    }

//    /**
//     * See if the current thread already has an Examine Lock.
//     * This is useful when you want to assert that the running
//     * thread has successfully acquired an Examine lock.
//     * This methods returns true if the current thread is a Job
//     * thread, or if the current thread has successfully called
//     * acquireExamineLock.
//     * @return true if the current thread has an active examine
//     * lock on the database, false otherwise.
//     */
//    public static synchronized boolean hasExamineLock() {
//        Thread thread = Thread.currentThread();
//        // change job is a valid examine job
//        if (thread == databaseChangesThread) return true;
//        // check for any examine jobs
//        Job job = databaseChangesThread.getJob(thread);
//        if (job != null) {
//            return true;
//        }
//        return false;
//    }

    /**
     * A common pattern is that the GUI needs to examine the database, but does not
     * want to wait if it cannot immediately get an Examine lock via acquireExamineLock.
     * In this case the GUI can call invokeExamineLater to have the specified SwingExamineTask
     * run in the context of the swing thread where it will be *guaranteed* that it will
     * be able to acquire an Examine Lock via acquireExamineLock().
     * <p>
     * This method basically reserves a slot in the Job queue with an Examine Job,
     * calls the runnable with SwingUtilities.invokeAndWait when the Job starts, and
     * ends the Job only after the runnable finishes.
     * <P>
     * IMPORTANT!  Note that this ties up both the Job queue and the Swing event queue.
     * It is possible to deadlock if the SwingExamineJob waits on a Change Job thread (unlikely,
     * but possible).  Note that this also runs examines sequentially, because that is
     * how the Swing event queue runs events.  This is less efficient than the Job queue examines,
     * but also maintains sequential ordering and process of events, which may be necessary
     * if state is being shared/modified between events (such as between mousePressed and
     * mouseReleased events).
     * @param task the Runnable to run in the swing thread. A call to
     * Job.acquireExamineLock from within run() is guaranteed to return true.
     * @param singularKey if not null, this specifies a key by which
     * subsequent calls to this method using the same key will be consolidated into
     * one invocation instead of many.  Only calls that have not already resulted in a
     * call back to the runnable will be ignored.  Only the last runnable will be used.
     * @see SwingExamineTask  for a common pattern using this method
     * @see #acquireExamineLock(boolean)
     * @see #releaseExamineLock()
     */
    public static void invokeExamineLater(Runnable task, Object singularKey) {
        assert false;
//        if (singularKey != null) {
//            SwingExamineJob priorJob = SwingExamineJob.getWaitingJobFor(singularKey);
//            if (priorJob != null)
//                priorJob.abort();
//        }
//        SwingExamineJob job = new SwingExamineJob(task, singularKey);
//        job.startJob(false, true);
    }

//    private static class SwingExamineJob extends Job {
//        /** Map of Runnables to waiting Jobs */         private static final Map<Object,Job> waitingJobs = new HashMap<Object,Job>();
//        /** The runnable to run in the Swing thread */  private Runnable task;
//        /** the singular key */                         private Object singularKey;
//
//        private SwingExamineJob(Runnable task, Object singularKey) {
//            super("ReserveExamineSlot", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
//            this.task = task;
//            this.singularKey = singularKey;
//            synchronized(waitingJobs) {
//                if (singularKey != null)
//                    waitingJobs.put(singularKey, this);
//            }
//        }
//
//        public boolean doIt() throws JobException {
//            synchronized(waitingJobs) {
//                if (singularKey != null)
//                    waitingJobs.remove(singularKey);
//            }
//            try {
//                SwingUtilities.invokeAndWait(task);
//            } catch (InterruptedException e) {
//            } catch (java.lang.reflect.InvocationTargetException ee) {
//            }
//            return true;
//        }
//
//        private static SwingExamineJob getWaitingJobFor(Object singularKey) {
//            if (singularKey == null) return null;
//            synchronized(waitingJobs) {
//                return (SwingExamineJob)waitingJobs.get(singularKey);
//            }
//        }
//    }

	/**
	 * Method to check whether examining of database is allowed.
	 */
    public static void checkExamine() {
        if (!getDebug()) return;
        if (NOTHREADING) return;
	    /*
	    // disabled by Gilda on Oct 18
        if (!hasExamineLock()) {
            String msg = "Database is being examined without an Examine Job or Examine Lock";
            System.out.println(msg);
            Error e = new Error(msg);
            throw e;
        }
        */
    }

	/**
	 * Method to check whether changing of whole database is allowed.
	 * @throws IllegalStateException if changes are not allowed.
	 */
	public static void checkChanging() {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof EThread) {
            ((EThread)currentThread).checkChanging();
        } else {
            IllegalStateException e = new IllegalStateException("Database changes are forbidden");
            Job.logger.logp(Level.WARNING, CLASS_NAME, "checkChanging", e.getMessage(), e);
            throw e;
        }
	}

    /**
     * Checks if cell bounds can be computed in this thread.
     * @return true if cell bounds can be computed.
     */
    public static boolean canComputeBounds() {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof EThread)
            return ((EThread)currentThread).canComputeBounds();
        else
            return Job.NOTHREADING || threadMode == Mode.CLIENT;
    }
    
    /**
     * Checks if netlist can be computed in this thread.
     * @return true if netlist can be computed.
     */
    public static boolean canComputeNetlist() {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof EThread)
            return ((EThread)currentThread).canComputeNetlist();
        else
            return Job.NOTHREADING || threadMode == Mode.CLIENT;
    }
    
    public static UserInterface getUserInterface() {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof EThread)
            return ((EThread)currentThread).getUserInterface();
        else
            return currentUI;
    }

    /**
     * Asserts that this is the Swing Event thread
     */
    public static void checkSwingThread()
    {
        if (!SwingUtilities.isEventDispatchThread()) {
            Exception e = new Exception("Job.checkSwingThread is not in the AWT Event Thread, it is in Thread "+Thread.currentThread());
            ActivityLogger.logException(e);
        }
    }

	//-------------------------------JOB UI--------------------------------
    
    public String toString() { return jobName+" ("+getStatus()+")"; }


    /** Get info on Job */
    public String getInfo() {
        StringBuffer buf = new StringBuffer();
        buf.append("Job "+toString());
        Date start = new Date(startTime);
        //buf.append("  start time: "+start+"\n");
        if (finished) {
//            Date end = new Date(endTime);
            //buf.append("  end time: "+end+"\n");
            long time = endTime - startTime;
            buf.append(" took: "+TextUtils.getElapsedTime(time));
            buf.append(" (started at "+start+")");
        } else if (getProgress() == null) {
            long time = System.currentTimeMillis()-startTime;
	        buf.append(" has not finished. Current running time: " + TextUtils.getElapsedTime(time));
        } else {
            buf.append(" did not successfully finish.");
        }
        return buf.toString();
    }
        
	private static class ServerJobManager extends Thread implements Observer {
        private final int port;
        private final ArrayList<ServerConnection> serverConnections = new ArrayList<ServerConnection>();
        private int cursor;
        private volatile Snapshot currentSnapshot = new Snapshot();
        
        ServerJobManager(int port) {
            super(null, null, "ConnectionWaiter", 0/*10*1020*/);
            this.port = port;
        }
        
        public void updateSnapshot() {
            Snapshot oldSnapshot = currentSnapshot;
            Snapshot newSnapshot = new Snapshot(oldSnapshot);
            if (newSnapshot.equals(oldSnapshot)) return;
            synchronized (this) {
                currentSnapshot = newSnapshot;
                for (ServerConnection conn: serverConnections)
                    conn.updateSnapshot(newSnapshot);
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
    
        public void run() {
            try {
                ServerSocket ss = new ServerSocket(port);
                System.out.println("ServerSocket waits for port " + port);
                for (;;) {
                    Socket socket = ss.accept();
                    ServerConnection conn;
                    synchronized (this) {
                        conn = new ServerConnection(serverConnections.size(), socket);
                        serverConnections.add(conn);
                    }
                    System.out.println("Accepted connection " + conn.connectionId);
                    conn.updateSnapshot(currentSnapshot);
                    conn.start();
                }
            } catch (IOException e) {
                e.printStackTrace(System.out);
            }
        }
    }
    
    private static class FIFO {
        private static final String CLASS_NAME = Job.CLASS_NAME + ".FIFO";
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
    
    private static FIFO clientFifo = new FIFO();
    
    private static volatile int clientNumExamine = 0;
    private static Snapshot clientSnapshot = new Snapshot();
    
    private static final String CLIENT_INVOKE_CLASS_NAME = Job.CLASS_NAME + ".clientInvoke";
    private static Runnable clientInvoke = new Runnable() {
        public void run() {
            logger.entering(CLIENT_INVOKE_CLASS_NAME, "run");
            assert SwingUtilities.isEventDispatchThread();
            for (;;) {
                logger.logp(Level.FINEST, CLIENT_INVOKE_CLASS_NAME, "run", "before get");
                int numGet = clientFifo.numGet;
                Object o = clientFifo.get();
                if (o == null) break;
                if (o instanceof Snapshot) {
                    Snapshot newSnapshot = (Snapshot)o;
                    logger.logp(Level.FINER, CLIENT_INVOKE_CLASS_NAME, "run", "snapshot begin {0}", Integer.valueOf(numGet));
                    (new SnapshotDatabaseChangeRun(clientSnapshot, newSnapshot)).run();
                    clientSnapshot = newSnapshot;
                    logger.logp(Level.FINER, CLIENT_INVOKE_CLASS_NAME, "run", "snapshot end");
                } else if (o instanceof Integer) {
                    int jobId = ((Integer)o).intValue();
                    logger.logp(Level.FINER, CLIENT_INVOKE_CLASS_NAME, "run", "result begin {0}", Integer.valueOf(numGet));
                    byte[] bytes = (byte[])clientFifo.get();
                    Job job = null;
                    for (Job j: startedJobs) {
                        if (j.jobId == jobId) {
                            job = j;
                            break;
                        }
                    }
                    if (job != null) {
                        (new JobTerminateRun(clientJob, bytes)).run();
                        logger.logp(Level.FINER, CLIENT_INVOKE_CLASS_NAME, "run", "result end {0}", job.jobName);
                    } else {
                        logger.logp(Level.WARNING, CLIENT_INVOKE_CLASS_NAME, "run", "result of unknown job {0}", o);
                        System.out.println("Job " + jobId + " was not found in startedJobs");
                    }
                } else if (o instanceof String) {
                    logger.logp(Level.FINEST, CLIENT_INVOKE_CLASS_NAME, "run", "string begin");
                    System.out.print((String)o);
                    logger.logp(Level.FINEST, CLIENT_INVOKE_CLASS_NAME, "run", "string end {0}", o);
                }
            }
            if (waitingJobs.isEmpty()) {
                logger.exiting(CLIENT_INVOKE_CLASS_NAME, "run");
                return;
            }
            Job job = waitingJobs.remove(0);
            if (job.jobType == Type.EXAMINE) {
                logger.logp(Level.FINER, CLIENT_INVOKE_CLASS_NAME, "run", "Schedule EXAMINE {0}", job);
                try {
                    job.doIt();
                } catch (JobException e) {
                    e.printStackTrace();
                }
            } else {
                logger.logp(Level.FINER, CLIENT_INVOKE_CLASS_NAME, "run", "Schedule {0}", job);
                Job tmp = job.testSerialization();
                if (tmp != null) {
                    try {
                        job.jobId = ++numStarted;
                        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                        EObjectOutputStream out = new EObjectOutputStream(byteStream);
                        out.writeObject(job);
                        out.close();
                        byte[] bytes = byteStream.toByteArray();
                        clientOutputStream.writeInt(job.jobId);
                        clientOutputStream.writeUTF(job.jobType.toString());
                        clientOutputStream.writeUTF(job.jobName);
                        clientOutputStream.writeInt(bytes.length);
                        clientOutputStream.write(bytes);
                        clientOutputStream.flush();
                        
                        job.started = true;
                        startedJobs.add(job);
                        clientJob = job;
                    } catch (IOException e) {
                        System.out.println("Job " + this + " was not launched in CLIENT mode");
                        e.printStackTrace(System.out);
                    }
                }
            }
            logger.exiting(CLIENT_INVOKE_CLASS_NAME, "run");
       }
    };
    
    private static void clientLoop(int port) {
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
    
    private static class JobTerminateRun implements Runnable {
        private Job job;
        private byte[] result;
        
        JobTerminateRun(Job job, byte[] result) {
            this.job = job;
            this.result = result;
        }
        
        public void run() {
            try {
                Class jobClass = job.getClass();
                ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(result));
                Throwable jobException = (Throwable)in.readObject();
//                System.out.println("\tjobException = " + jobException);
                int numFields = in.readInt();
                for (int i = 0; i < numFields; i++) {
                    String fieldName = in.readUTF();
                    Object value = in.readObject();
//                    System.out.println("\tField " + fieldName + " = " + value);
                    Field f = jobClass.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    f.set(job, value);
                }
                in.close();
                job.terminateIt(jobException);
            } catch (Throwable e) {
                System.out.println("Exception executing terminateIt");
                e.printStackTrace(System.out);
            }
            job.endTime = System.currentTimeMillis();
            job.finished = true;                        // is this redundant with Thread.isAlive()?

    		// say something if it took more than a minute by default
        	if (job.reportExecution || (job.endTime - job.startTime) >= MIN_NUM_SECONDS)
            {
                if (User.isBeepAfterLongJobs())
                {   
                    Toolkit.getDefaultToolkit().beep();
                }
                System.out.println(job.getInfo());
            }

            // delete
            if (job.deleteWhenDone) {
                if (threadMode == Mode.CLIENT) {
                    startedJobs.remove(job);
                } else {
                    removeJob(job);
                    UserInterface userInterface = Job.getUserInterface();
                    if (userInterface != null)
                        userInterface.invokeLaterBusyCursor(isChangeJobQueuedOrRunning());
                }
            }
            
        }
    }

    private static class UserInterfaceRedirect implements UserInterface
	{
		public EditWindow_ getCurrentEditWindow_() {
//            System.out.println("UserInterface.getCurrentEditWindow was called from DatabaseChangesThread");
            return currentUI.getCurrentEditWindow_();
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
            currentUI.wantToRedoErrorTree();
        }
        public void wantToRedoJobTree() {
//            System.out.println("UserInterface.wantToRedoJobTree was called from DatabaseChangesThread");
            currentUI.wantToRedoJobTree();
        }

        public void termLogging(final ErrorLogger logger, boolean explain) {
            System.out.println("UserInterface.termLogging was called from DatabaseChangesThread");
        }

        /* Job related **/
        public void invokeLaterBusyCursor(final boolean state){
//            System.out.println("UserInterface.invokeLaterBusyCursor was called from DatabaseChangesThread");
            currentUI.invokeLaterBusyCursor(state);
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
            System.out.println("UserInterface.showErrorMessage was called from DatabaseChangesThread");
        	System.out.println(message);
        }

        /**
         * Method to show an informational message.
         * @param message the message to show.
         * @param title the title of a dialog with the message.
         */
        public void showInformationMessage(Object message, String title)
        {
            System.out.println("UserInterface.showInformationMessage was called from DatabaseChangesThread");
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