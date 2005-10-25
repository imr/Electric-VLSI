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

import com.sun.electric.Main;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

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
public abstract class Job implements ActionListener, Runnable {

    private static final boolean DEBUG = false;

    /**
	 * Type is a typesafe enum class that describes the type of job (CHANGE or EXAMINE).
	 */
	public static class Type
	{
		private final String name;

		private Type(String name) { this.name = name; }

		/**
		 * Returns a printable version of this Type.
		 * @return a printable version of this Type.
		 */
		public String toString() { return name; }

		/** Describes a database change. */			public static final Type CHANGE  = new Type("change");
		/** Describes a database undo/redo. */		public static final Type UNDO    = new Type("undo");
		/** Describes a database examination. */	public static final Type EXAMINE = new Type("examine");
	}

	/**
	 * Priority is a typesafe enum class that describes the priority of a job.
	 */
	public static class Priority
	{
		private final String name;
		private final int level;

		private Priority(String name, int level) { this.name = name;   this.level = level; }

		/**
		 * Returns a printable version of this Priority.
		 * @return a printable version of this Priority.
		 */
		public String toString() { return name; }

		/**
		 * Returns a level of this Priority.
		 * @return a level of this Priority.
		 */
		public int getLevel() { return level; }

		/** The highest priority: from the user. */		public static final Priority USER         = new Priority("user", 1);
		/** Next lower priority: visible changes. */	public static final Priority VISCHANGES   = new Priority("visible-changes", 2);
		/** Next lower priority: invisible changes. */	public static final Priority INVISCHANGES = new Priority("invisble-changes", 3);
		/** Lowest priority: analysis. */				public static final Priority ANALYSIS     = new Priority("analysis", 4);
	}

	/**
	 * Thread which execute all database change Jobs.
	 */
	private static class DatabaseChangesThread extends Thread
	{
        // Job Management
        /** all jobs */                             private static ArrayList<Job> allJobs = new ArrayList<Job>();
        /** number of started jobs */               private static int numStarted = 0;
        /** number of examine jobs */               private static int numExamine = 0;

		DatabaseChangesThread() {
			super("Database");
			start();
		}

		public void run()
		{
			for (;;)
			{
				Job job = waitChangeJob();

                job.run();
                // turn off busy cursor if no more change jobs
                synchronized(this) {
                    if (!isChangeJobQueuedOrRunning())
                        SwingUtilities.invokeLater(new Runnable() { public void run() { TopLevel.setBusyCursor(false); }});
                }
			}
		}

		private synchronized Job waitChangeJob() {
			for (;;)
			{
				if (numStarted < allJobs.size())
				{
					Job job = (Job)allJobs.get(numStarted);
                    if (job.scheduledToAbort || job.aborted) {
                        // remove jobs that have been aborted
                        removeJob(job);
                        continue;
                    }
					if (job.jobType == Type.EXAMINE)
					{
						job.started = true;
                        if (job instanceof InthreadExamineJob) {
                            // notify thread that it can run.
                            final InthreadExamineJob ijob = (InthreadExamineJob)job;
                            boolean started = false;
                            synchronized(ijob.mutex) {
                                if (ijob.waiting) {
                                    ijob.waiting = false;
                                    ijob.mutex.notify();
                                    started = true;
                                }
                            }
                            if (started) {
                                assert(false);
                                numStarted++;
                                numExamine++;                                
                            }
                            continue;
                        }
                        numStarted++;
                        numExamine++;
                        //System.out.println("Started Job "+job+", numStarted="+numStarted+", numExamine="+numExamine+", allJobs="+allJobs.size());
						Thread t = new Thread(job, job.jobName);
                        job.thread = t;
						t.start();
						continue;
					}
					// job.jobType == Type.CHANGE || jobType == Type.REDO
					if (numExamine == 0)
					{
						job.started = true;
						numStarted++;
						return job;
					}
				}
				try {
                    //System.out.println("Waiting, numStarted="+numStarted+", numExamine="+numExamine+", allJobs="+allJobs.size());
					wait();
				} catch (InterruptedException e) {}
			}
		}
    
		/** Add job to list of jobs */
		private synchronized void addJob(Job j) {
			if (numStarted == allJobs.size())
				notify();
            allJobs.add(j);
            if (j.jobType == Type.CHANGE) {
                SwingUtilities.invokeLater(new Runnable() { public void run() { TopLevel.setBusyCursor(true); }});
            }
            if (j.getDisplay()) {
                WindowFrame.wantToRedoJobTree();
            }
		}

        /**
         * This method is intentionally not synchronized. We must preserve the order of locking:
         * databaseChangesThread monitor -> InthreadExamineJob.mutex monitor.
         * However, the databaseChangesThread monitor needs to be released before the call to
         * j.mutex.wait(), otherwise we will have deadlock because this thread did not give up
         * the databaseChangeThread monitor.  This is because notify() on the j.mutex is only
         * called after both the databaseChangeThread and mutex monitor have been acquired, but in a
         * separate thread.
         * @param j
         * @param wait true to wait for lock if not available now, false to not wait
         * @return true if lock acquired, false otherwise (if wait is true, this method always returns true)
         */
        private boolean addInthreadExamineJob(final InthreadExamineJob j, boolean wait) {
            synchronized(this) {
                // check if change job running or queued: if so, can't run immediately
                if (isChangeJobQueuedOrRunning()) {
                    if (!wait) return false;
                    // need to queue because it may not be able to run immediately
                    addJob(j);
                    synchronized(j.mutex) {
                        j.waiting = true;
                    }
                } else {
                    // grant examine lock
                    allJobs.add(j);
                    numExamine++;
                    numStarted++;
                    //System.out.println("Granted Examine Lock for Job "+j+", numStarted="+numStarted+", numExamine="+numExamine+", allJobs="+allJobs.size());
                    j.incrementLockCount();
                    return true;
                }
            }

            // we are queued
            synchronized(j.mutex) {
                // note that j.waiting *could* have been set false already if Job queue was processed
                // before this code was processed.
                if (j.waiting) {
                    try { j.mutex.wait(); } catch (InterruptedException e) { System.out.println("Interrupted in databaseChangesThread"); }
                }
            }
            j.incrementLockCount();
            return true;
        }

		/** Remove job from list of jobs */
		private synchronized void removeJob(Job j) { 
			int index = allJobs.indexOf(j);
			if (index != -1) {
				allJobs.remove(index);
				if (index == numStarted)
					notify();
				if (index < numStarted) numStarted--;
			}
            //System.out.println("Removed Job "+j+", index was "+index+", numStarted now="+numStarted+", allJobs="+allJobs.size());
            if (j.getDisplay()) {
                WindowFrame.wantToRedoJobTree();        
            }
		}

		/**
		 * A static object is used so that its open/closed tree state can be maintained.
		 */
		private static String jobNode = "JOBS";
	
		/** Build Job explorer tree */
		public synchronized DefaultMutableTreeNode getExplorerTree() {
			DefaultMutableTreeNode explorerTree = new DefaultMutableTreeNode(jobNode);
			for (Iterator<Job> it = allJobs.iterator(); it.hasNext();) {
                Job j = (Job)it.next();
                if (j.getDisplay()) {
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(j);
                    j.myNode.setUserObject(null);       // remove reference to job on old node
                    j.myNode = node;                    // get rid of old node, point to new node
                    explorerTree.add(node);
                }
			}
			return explorerTree;
		}
    
		private synchronized void endExamine(Job j) {
			numExamine--;
            //System.out.println("EndExamine Job "+j+", numExamine now="+numExamine+", allJobs="+allJobs.size());
			if (numExamine == 0)
				notify();
		}

        /** Get job running in the specified thread */
        private synchronized Job getJob(Thread t) {
            for (Iterator<Job> it = allJobs.iterator(); it.hasNext(); ) {
                Job j = (Job)it.next();
                if (j.thread == t) return j;
            }
            return null;
        }

        /** get all jobs iterator */
        public synchronized Iterator<Job> getAllJobs() {
            List<Job> jobsList = new ArrayList<Job>();
            for (Iterator<Job> it = allJobs.iterator(); it.hasNext() ;) {
                jobsList.add(it.next());
            }
            return jobsList.iterator();
        }

        private synchronized boolean isChangeJobQueuedOrRunning() {
            Iterator<Job> it;
            for (it = allJobs.iterator(); it.hasNext(); ) {
                Job j = (Job)it.next();
                if (j.finished) continue;               // ignore finished jobs
                if (j.jobType == Type.CHANGE) return true;
            }
            return false;
        }
	}

	/** default execution time in milis */      private static final int MIN_NUM_SECONDS = 60000;
	/** database changes thread */              public static final DatabaseChangesThread databaseChangesThread = new DatabaseChangesThread();
	/** changing job */                         private static Job changingJob;
    /** my tree node */                         private DefaultMutableTreeNode myNode;
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
    /** name of job */                          private String jobName;
    /** tool running the job */                 private Tool tool;
    /** type of job (change or examine) */      private Type jobType;
//    /** priority of job */                      private Priority priority;
    /** bottom of "up-tree" of cells affected */private Cell upCell;
//    /** top of "down-tree" of cells affected */ private Cell downCell;
//    /** status */                               private String status = null;
    /** progress */                             private String progress = null;
    /** list of saved Highlights */             private List<Highlight> savedHighlights;
    /** saved Highlight offset */               private Point2D savedHighlightsOffset;
    /** Thread job will run in (null for new thread) */
                                                private Thread thread;

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
		this.upCell = upCell;
//		this.downCell = downCell;
        this.display = true;
        this.deleteWhenDone = true;
        startTime = endTime = 0;
        started = finished = aborted = scheduledToAbort = false;
        myNode = null;
        thread = null;
        savedHighlights = new ArrayList<Highlight>();
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
        startJob(true, true);
    }
	
    /**
     * Start the job by placing it on the JobThread queue.
     * If <code>display</code> is true, display job on Job List UI.
     * If <code>deleteWhenDone</code> is true, Job will be deleted
     * after it is done (frees all data and references it stores/created)
     * @param deleteWhenDone delete when job is done if true, otherwise leave it around
     */
    public void startJob(boolean display, boolean deleteWhenDone)
    {
        this.display = display;
        this.deleteWhenDone = deleteWhenDone;

        if (display)
            myNode = new DefaultMutableTreeNode(this);

        if (Main.NOTHREADING) {
            // turn off threading if needed for debugging
            TopLevel.setBusyCursor(true);
            run();
            TopLevel.setBusyCursor(false);
        } else {
            databaseChangesThread.addJob(this);
        }
    }

    /**
     * Method to access scheduled abort flag in Job and
     * set the flag to abort if scheduled flag is true.
     * This is because setAbort and getScheduledToAbort
     * are protected in Job.
     * @return true if job is scheduled for abort or aborted.
     * and it will report it to std output
     */
//    public boolean checkForAbort()
//    {
//        if (getAborted()) return (true);
//        boolean abort = getScheduledToAbort();
//        if (abort)
//        {
//            setAborted();
//            setReportExecutionFlag(true); // Force reporting
//            System.out.println(jobName +" aborted");
//        }
//        return (abort);
//    }

	//--------------------------ABSTRACT METHODS--------------------------
    
    /** This is the main work method.  This method should
     * perform all needed tasks.
     */
    public abstract boolean doIt();
    
    //--------------------------PRIVATE JOB METHODS--------------------------

//    /** Locks the database to prevent changes to it */
//    private void lockDatabase() {
//
//    }
//
//    /** Unlocks database */
//    private void unlockDatabase() {
//
//    }

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

        if (DEBUG) System.out.println(jobType+" Job: "+jobName+" started");
        ActivityLogger.logJobStarted(jobName, jobType, upCell, savedHighlights, savedHighlightsOffset);
		try {
            if (jobType != Type.EXAMINE) changingJob = this;
			if (jobType == Type.CHANGE)	Undo.startChanges(tool, jobName, upCell, savedHighlights, savedHighlightsOffset);
			doIt();
			if (jobType == Type.CHANGE)	Undo.endChanges();
		} catch (Throwable e) {
            endTime = System.currentTimeMillis();
            e.printStackTrace(System.err);
            ActivityLogger.logException(e);
            if (e instanceof Error) throw (Error)e;
		} finally {
			if (jobType == Type.EXAMINE)
			{
				databaseChangesThread.endExamine(this);
			} else {
				changingJob = null;
			}
            endTime = System.currentTimeMillis();
		}
        if (DEBUG) System.out.println(jobType+" Job: "+jobName +" finished");

		finished = true;                        // is this redundant with Thread.isAlive()?
//        Job.removeJob(this);
        //WindowFrame.wantToRedoJobTree();

		// say something if it took more than a minute by default
		if (reportExecution || (endTime - startTime) >= MIN_NUM_SECONDS)
		{
			if (User.isBeepAfterLongJobs())
			{
				Toolkit.getDefaultToolkit().beep();
			}
			System.out.println(this.getInfo());
		}

        // delete
        if (deleteWhenDone) {
            databaseChangesThread.removeJob(this);
        }
    }

    /**
     * Method to end the current batch of changes and start another.
     * Besides starting a new batch, it cleans up the constraint system, and 
     */
    protected void flushBatch()
    {
		if (jobType != Type.CHANGE)	return;
		Undo.endChanges();
		Undo.startChanges(tool, jobName, upCell, savedHighlights, savedHighlightsOffset);
    }

    protected synchronized void setProgress(String progress) {
        this.progress = progress;
        WindowFrame.wantToRedoJobTree();
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
        WindowFrame.wantToRedoJobTree();
    }

    /** Save current Highlights */
    private void saveHighlights() {
        savedHighlights.clear();

        // for now, just save highlights in current window
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();
        if (highlighter == null) return;

        for (Iterator<Highlight> it = highlighter.getHighlights().iterator(); it.hasNext(); ) {
            savedHighlights.add(it.next());
        }
        savedHighlightsOffset = highlighter.getHighlightOffset();
    }

	/** Confirmation that thread is aborted */
    protected synchronized void setAborted() { aborted = true; WindowFrame.wantToRedoJobTree(); }
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
    public static Iterator getAllJobs() { return databaseChangesThread.getAllJobs(); }
    
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
    public void remove() {
        if (!finished && !aborted) {
            //System.out.println("Cannot delete running jobs.  Wait till finished or abort");
            return;
        }
        databaseChangesThread.removeJob(this);
    }

    /**
     * This Job serves as a locking mechanism for acquireExamineLock()
     */
    private static class InthreadExamineJob extends Job {
        private boolean waiting;
        private int lockCount;
        protected final Object mutex = new Object();

        private InthreadExamineJob() {
            super("Inthread Examine", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
            waiting = false;
            lockCount = 0;
        }
        public boolean doIt() {
            // this should never be called
            assert(false);
            return true;
        }
        private void incrementLockCount() { lockCount++; }
        private void decrementLockCount() { lockCount--; }
        private int getLockCount() { return lockCount; }
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
        if (true) return true;      // disabled
        Thread thread = Thread.currentThread();

        // first check to see if we already have the lock
        Job job = databaseChangesThread.getJob(thread);
        if (job != null) {
            assert(job instanceof InthreadExamineJob);
            ((InthreadExamineJob)job).incrementLockCount();
            return true;
        }
        // create new Job to get examine lock
        InthreadExamineJob dummy = new InthreadExamineJob();
        ((Job)dummy).display = false;
        ((Job)dummy).deleteWhenDone = true;
        ((Job)dummy).thread = thread;
        return databaseChangesThread.addInthreadExamineJob(dummy, block);
    }

    /**
     * Release the lock to examine the database.  The lock is the lock
     * associated with the current thread.  This should only be called if a
     * lock was acquired for the current thread.
     * @see #acquireExamineLock(boolean)
     * @see #invokeExamineLater(Runnable, Object)
     */
    public static synchronized void releaseExamineLock() {
        if (true) return;      // disabled
        Job dummy = databaseChangesThread.getJob(Thread.currentThread());
        assert(dummy != null);
        assert(dummy instanceof InthreadExamineJob);
        InthreadExamineJob job = (InthreadExamineJob)dummy;
        job.decrementLockCount();
        if (job.getLockCount() == 0) {
            databaseChangesThread.endExamine(job);
            databaseChangesThread.removeJob(job);
        }
    }

    /**
     * See if the current thread already has an Examine Lock.
     * This is useful when you want to assert that the running
     * thread has successfully acquired an Examine lock.
     * This methods returns true if the current thread is a Job
     * thread, or if the current thread has successfully called
     * acquireExamineLock.
     * @return true if the current thread has an active examine
     * lock on the database, false otherwise.
     */
    public static synchronized boolean hasExamineLock() {
        Thread thread = Thread.currentThread();
        // change job is a valid examine job
        if (thread == databaseChangesThread) return true;
        // check for any examine jobs
        Job job = databaseChangesThread.getJob(thread);
        if (job != null) {
            return true;
        }
        return false;
    }

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
        if (singularKey != null) {
            SwingExamineJob priorJob = SwingExamineJob.getWaitingJobFor(singularKey);
            if (priorJob != null)
                priorJob.abort();
        }
        SwingExamineJob job = new SwingExamineJob(task, singularKey);
        job.startJob(false, true);
    }

    private static class SwingExamineJob extends Job {
        /** Map of Runnables to waiting Jobs */         private static final Map<Object,Job> waitingJobs = new HashMap<Object,Job>();
        /** The runnable to run in the Swing thread */  private Runnable task;
        /** the singular key */                         private Object singularKey;

        private SwingExamineJob(Runnable task, Object singularKey) {
            super("ReserveExamineSlot", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
            this.task = task;
            this.singularKey = singularKey;
            synchronized(waitingJobs) {
                if (singularKey != null)
                    waitingJobs.put(singularKey, this);
            }
        }

        public boolean doIt() {
            synchronized(waitingJobs) {
                if (singularKey != null)
                    waitingJobs.remove(singularKey);
            }
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (InterruptedException e) {
            } catch (java.lang.reflect.InvocationTargetException ee) {
            }
            return true;
        }

        private static SwingExamineJob getWaitingJobFor(Object singularKey) {
            if (singularKey == null) return null;
            synchronized(waitingJobs) {
                return (SwingExamineJob)waitingJobs.get(singularKey);
            }
        }
    }

	/**
	 * Method to check whether examining of database is allowed.
	 */
    public static void checkExamine() {
        if (!Main.getDebug()) return;
        if (Main.NOTHREADING) return;
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
	 * Returns thread which activated current changes or null if no changes.
	 * @return thread which activated current changes or null if no changes.
	 */
	public static Thread getChangingThread() { return changingJob != null ? databaseChangesThread : null; }

    /**
     * Returns the current Changing job (there can be only one)
     * @return the current Changing job (there can be only one). Returns null if no changing job.
     */
    public static Job getChangingJob() { return changingJob; }


	/**
	 * Returns cell which is root of up-tree of current changes or null, if no changes or whole database changes.
	 * @return cell which is root of up-tree of current changes or null, if no changes or whole database changes.
	 */
	public static Cell getChangingCell() { return changingJob != null ? changingJob.upCell : null; }

	/**
	 * Method to check whether changing of whole database is allowed.
	 * Issues an error if it is not.
	 */
	public static void checkChanging()
	{
		if (Thread.currentThread() != databaseChangesThread)
		{
			if (Main.NOTHREADING) return;
			String msg = "Database is being changed by another thread";
            System.out.println(msg);
			throw new IllegalStateException(msg);
		} else if (changingJob == null)
		{
			if (Main.NOTHREADING) return;
			String msg = "Database is changing but no change job is running";
            System.out.println(msg);
			throw new IllegalStateException(msg);
		}
/*       else if (changingJob.upCell != null)
		{
			String msg = "Database is changing which only up-tree of "+changingJob.upCell+" is locked";
            System.out.println(msg);
            Error e = new Error(msg);
            throw e;
			//throw new IllegalStateException("Undo.checkChanging()");
		}*/
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
        
    /** Build Job explorer tree */
    public static DefaultMutableTreeNode getExplorerTree() {
		return databaseChangesThread.getExplorerTree();
    }
    
    /** popup menu when user right-clicks on job in explorer tree */
    public JPopupMenu getPopupStatus() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem m;
        m = new JMenuItem("Get Info"); m.addActionListener(this); popup.add(m);
        m = new JMenuItem("Abort"); m.addActionListener(this); popup.add(m);
        m = new JMenuItem("Delete"); m.addActionListener(this); popup.add(m);
        return popup;
    }
    
    /** respond to menu item command */
    public void actionPerformed(ActionEvent e) {
        JMenuItem source = (JMenuItem)e.getSource();
        // extract library and cell from string
        if (source.getText().equals("Get Info"))
            System.out.println(getInfo());
        if (source.getText().equals("Abort"))
            abort();
        if (source.getText().equals("Delete")) {
            if (!finished && !aborted) {
                System.out.println("Cannot delete running jobs.  Wait till it is finished, or abort it");
                return;
            }
            databaseChangesThread.removeJob(this);
        }
    }

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
        
}
