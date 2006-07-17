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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.CantEditException;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

import java.awt.Toolkit;
import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Jobs are processes that will run in the background, such as 
 * DRC, NCC, Netlisters, etc.  Each Job gets placed in a Job
 * window, and reports its status.  A job can be cancelled.
 * 
 * <p>To start a new job, do:
 * <p>Job job = new Job(name);
 * <p>job.start();
 * 
 * <p>Job subclass must implement "doIt" method, it may override "terminateOK" method.
 * <p>Job subclass is activated by "Job.startJob()". In case of exception in constructor it is not activated.
 *    Hence "doIt" and "terminateOK" method are not called.
 * <p>Job subclass must be serializable for CHANGE and REMOTE_EXAMINE mode.
 *    Serialization occurs at the moment when "Job.startJob()" is called.
 *    Fields that are not needed on the server are escaped from serialization by "transient" keyword.
 *    "doIt" is executed on serverDatabase for CHANGE and REMOTE_EXAMINE mode.
 *    "doIt" is executed on clientDatabase for EXAMINE mode, Job subclass need not be serializable.
 * <p>"doIt" may return true, may return false, may throw JobException or any other Exception/Error.
 *    Return true is considered normal termination.
 *    Return false and throwing any Exception/Throwable are failure terminations.
 * <p>On normal termination in CHANGE or REMOTE_EXAMINE mode "fieldVariableChange" variables are serialized.
 *    In case of REMOTE_EXAMINE they are serialized on read-only database state.
 *    In case of CHANGE they are serialized on database state after Constraint propagation.
 *    In case of EXAMINE they are not serialized, but they are checked for valid field names.
 *    Some time later the changed variables are deserialized on client database.
 *    If serialization on server and deserialization on client was OK then terminateOK method is called on client database for
 *       all three modes CHANGE, REMOTE_EXAMINE, EXAMINE.
 *    If serialization/deserialization failed then terminateOK is not called, error message is issued.
 * <p>In case of failure termination no terminateOK is called, error message is issued,
 *
 * <p>The extendig class may override getProgress(),
 * which returns a string indicating the current status.
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

    private static boolean GLOBALDEBUG = false;
    /*private*/ static Mode threadMode;
    private static int socketPort = 35742; // socket port for client/server
    static final int PROTOCOL_VERSION = 13; // Jul 17
    public static boolean BATCHMODE = false; // to run it in batch mode
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

  
	/** default execution time in milis */      /*private*/ static final int MIN_NUM_SECONDS = 60000;
    /** job manager */                          /*private*/ static JobManager jobManager;
	static AbstractUserInterface currentUI;

    /** delete when done if true */             /*private*/ boolean deleteWhenDone;
    /** display on job list if true */          private boolean display;
    
    // Job Status
    /** job start time */                       protected long startTime;
    /** job end time */                         protected long endTime;
    /** was job started? */                     /*private*/boolean started;
    /** is job finished? */                     /*private*/ boolean finished;
    /** thread aborted? */                      /*private*/ boolean aborted;
    /** schedule thread to abort */             /*private*/ boolean scheduledToAbort;
	/** report execution time regardless MIN_NUM_SECONDS */
												/*private*/ boolean reportExecution = false;
    /** tool running the job */                 /*private*/ Tool tool;
//    /** priority of job */                      private Priority priority;
//    /** bottom of "up-tree" of cells affected */private Cell upCell;
//    /** top of "down-tree" of cells affected */ private Cell downCell;
//    /** status */                               private String status = null;
    
    transient EJob ejob;

    public static void setThreadMode(Mode mode, AbstractUserInterface userInterface) {
        threadMode = mode;
        BATCHMODE = (mode == Mode.BATCH || mode == Mode.SERVER);
        currentUI = userInterface;
    }
   
    public static void initJobManager(int numThreads, Job initDatabaseJob, Object mode, String serverMachineName) {
        switch (threadMode) {
            case FULL_SCREEN:
                if (User.isUseClientServer())
                    jobManager = new ServerJobManager(numThreads, socketPort);
                else
                    jobManager = new ServerJobManager(numThreads);

                // Calling external dependencies
                currentUI.initializeInitJob(initDatabaseJob, mode);
                initDatabaseJob.startJob();
                break;
            case BATCH:
            case SERVER:
                jobManager = new ServerJobManager(numThreads, socketPort);
                initDatabaseJob.startJob();
                break;
            case CLIENT:
                logger.finer("setThreadMode");
                jobManager = new ClientJobManager(serverMachineName, socketPort);
                // unreachable
                break;
        }
        jobManager.runLoop();
    }
    
    
    public static Mode getRunMode() { return threadMode; }
    
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
        ejob = new EJob(this, jobType, jobName);
		this.tool = tool;
//		this.priority = priority;
//		this.upCell = upCell;
//		this.downCell = downCell;
        this.display = true;
        this.deleteWhenDone = true;
        startTime = endTime = 0;
//        started = finished = aborted = scheduledToAbort = false;
//        thread = null;
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
       
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof EThread && ((EThread)currentThread).ejob.jobType != Job.Type.EXAMINE) {
            ejob.startedByServer = true;
            ejob.client = ((EThread)currentThread).ejob.client;
            ejob.serverJob.startTime = System.currentTimeMillis();
            ejob.serialize(EDatabase.serverDatabase());
            ejob.clientJob = null;
        } else {
            ejob.client = Job.getExtendedUserInterface();
            ejob.clientJob.startTime = System.currentTimeMillis();
            ejob.serverJob = null;
            if (ejob.jobType != Job.Type.EXAMINE)
                ejob.serialize(EDatabase.clientDatabase());
         }
        jobManager.addJob(ejob, onMySnapshot);
    }

    /**
     * Method to remember that a field variable of the Job has been changed by the doIt() method.
     * @param variableName the name of the variable that changed.
     */
    protected void fieldVariableChanged(String variableName) { ejob.fieldVariableChanged(variableName); }

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
            String message = jobException.getMessage();
            if (message == null)
                message = "Job " + ejob.jobName + " failed";
            System.out.println(message);
        } else {
            ActivityLogger.logException(jobException);
        }
    }
    
    //--------------------------PRIVATE JOB METHODS--------------------------

	//--------------------------PROTECTED JOB METHODS-----------------------
	/** Set reportExecution flag on/off */
	protected void setReportExecutionFlag(boolean flag)
	{
		reportExecution = flag;
	}
    //--------------------------PUBLIC JOB METHODS--------------------------

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
        jobManager.setProgress(ejob, progress);
    }        
    
    private synchronized String getProgress() { return ejob.progress; }

    /** Return run status */
    public boolean isFinished() { return finished; }
    
    /** Tell thread to abort. Extending class should check
     * abort when/where applicable
     */
    public synchronized void abort() {
        if (ejob.jobType != Job.Type.EXAMINE) return;
        if (aborted) { 
            System.out.println("Job already aborted: "+getStatus());
            return;
        }
        scheduledToAbort = true;
//        Job.getUserInterface().wantToRedoJobTree();
    }

	/** Confirmation that thread is aborted */
    protected synchronized void setAborted() { aborted = true; /*Job.getUserInterface().wantToRedoJobTree();*/ }
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
        if (ejob.jobType != Job.Type.EXAMINE) return false;
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
    public static Iterator<Job> getAllJobs() { return jobManager.getAllJobs(); }
    
    /** get status */
    public String getStatus() {
        switch (ejob.state) {
//            case CLIENT_WAITING: return "cwaiting";
            case WAITING:
                return "waiting";
            case RUNNING:
                return getProgress() == null ? "running" : getProgress();
            case SERVER_DONE:
                return getProgress() == null ? "done" : getProgress();
            case CLIENT_DONE:
                return "cdone";
        }
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
        jobManager.removeJob(this);
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

    public static UserInterface getUserInterface() {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof EThread)
            return ((EThread)currentThread).getUserInterface();
        else
            return currentUI;
    }

    /**
     * Low-level method. 
     */
    public static AbstractUserInterface getExtendedUserInterface() {
            return currentUI;
    }

    public static EDatabase threadDatabase() {
        return EDatabase.theDatabase;
    }
    
    public static void wantUpdateGui() {
        jobManager.wantUpdateGui();
    }
    
    public static void updateNetworkErrors(Cell cell, List<ErrorLogger.MessageLog> errors) {
        currentUI.updateNetworkErrors(cell, errors);
    }
    
    public static void updateIncrementalDRCErrors(Cell cell, List<ErrorLogger.MessageLog> errors) {
        currentUI.updateIncrementalDRCErrors(cell, errors);
    }

	//-------------------------------JOB UI--------------------------------
    
    public String toString() { return ejob.jobName+" ("+getStatus()+")"; }


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
        
    static void runTerminate(EJob ejob) {
        Throwable jobException = ejob.deserializeResult();
        Job job = ejob.clientJob;
        try {
            job.terminateIt(jobException);
        } catch (Throwable e) {
            System.out.println("Exception executing terminateIt");
            e.printStackTrace(System.out);
        }
        job.endTime = System.currentTimeMillis();
        job.finished = true;                        // is this redundant with Thread.isAlive()?
        
        // say something if it took more than a minute by default
        if (job.reportExecution || (job.endTime - job.startTime) >= MIN_NUM_SECONDS) {
            if (User.isBeepAfterLongJobs()) {
                Toolkit.getDefaultToolkit().beep();
            }
            System.out.println(job.getInfo());
        }
    }
    
}