/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ToolOptions.java
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
 *
 * Job.java
 *
 * Created on November 18, 2003, 11:29 AM
 */

package com.sun.electric.tool;

import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;

import java.lang.Thread;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.sql.Time;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JPopupMenu;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Point;

/**
 * Jobs are processes that will run in the background, such as 
 * DRC, NCC, Netlisters, etc.  Each Job gets placed in a Job
 * window, and reports its status.  A job can be cancelled.
 * 
 * <p>To start a new job, do:
 * <p>Job job = new Job(name);
 * <p>job.start();
 * 
 * <p>The extending class must implement doIt(), which does the job;
 * and getProgress(), which returns a string indicating the current status.
 * Job also contains boolean abort, which gets set when the user decides
 * to abort the Job.  The extending class' code should check abort when/where
 * applicable.
 *
 * <p>Note that if your Job calls methods outside of this thread
 * that access shared data, those called methods should be synchronized.
 *
 * <p>Jobs may use the included OutputStream to print to a temporary
 * file instead of cluttering the message window while the user is 
 * still working.  To do so, simply define PrintStream out = new 
 * PrintStream(ostream), and use 'out' in place of 'System.out'.
 *
 * <p>The UI can manipulate the contents of the temporary file to 
 * display it in various ways for the user.
 *
 * @author  gainsley
 */
public abstract class Job implements ActionListener, Runnable {

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
			}
		}

		private synchronized Job waitChangeJob() {
			for (;;)
			{
				if (numStarted < allJobs.size())
				{
					Job job = (Job)allJobs.get(numStarted);
					if (job.jobType == Type.EXAMINE)
					{
						job.started = true;
						numStarted++;
						numExamine++;
						Thread t = new Thread(job, job.jobName);
						t.start();
						continue;
					}
					// job.jobType == Type.CHANGE || jobType == Type.REDO
					if (numExamine == 0)
					{
						job.started = true;
						numStarted++;
						if (job.upCell != null) job.upCell.setChangeLock();
						return job;
					}
				}
				try {
					wait();
				} catch (InterruptedException e) {}
			}
		}
    
		/** Add job to list of jobs */
		private synchronized void addJob(Job j) { 
			if (numStarted == allJobs.size())
				notify();
			allJobs.add(j);
			explorerTree.add(j.myNode);
			WindowFrame.explorerTreeChanged();
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
			explorerTree.remove(j.myNode);
			WindowFrame.explorerTreeChanged();        
		}

		/** Build Job explorer tree */
		public synchronized DefaultMutableTreeNode getExplorerTree() {
			explorerTree.removeAllChildren();
			for (Iterator it = allJobs.iterator(); it.hasNext();) {
				DefaultMutableTreeNode node = new DefaultMutableTreeNode((Job)it.next());
				explorerTree.add(node);
			}
			return explorerTree;
		}
    
		private synchronized void endExamine(Job j) {
			numExamine--;
			if (numExamine == 0)
				notify();
		}
	}

	// Job Management
    /** all jobs */                             private static ArrayList allJobs = new ArrayList();
	/** number of started jobs */               private static int numStarted = 0;
	/** number of examine jobs */               private static int numExamine = 0;
	/** database changes thread */              private static DatabaseChangesThread databaseChangesThread = new DatabaseChangesThread();
	/** changing job */                         private static Job changingJob;
    /** job tree */                             private static DefaultMutableTreeNode explorerTree = new DefaultMutableTreeNode("JOBS");
    /** my tree node */                         private DefaultMutableTreeNode myNode;
    
    // Job Status
    /** job start time */                       protected long startTime;
    /** job end time */                         protected long endTime;
    /** was job started? */                     private boolean started;
    /** is job finished? */                     private boolean finished;
    /** thread aborted? */                      private boolean aborted;
    /** schedule thread to abort */             private boolean scheduledToAbort;
    /** name of job */                          private String jobName;
    /** tool running the job */                 private Tool tool;
    /** type of job (change or examine) */      private Type jobType;
    /** priority of job */                      private Priority priority;
    /** bottom of "up-tree" of cells affected */private Cell upCell;
    /** top of "down-tree" of cells affected */ private Cell downCell;
    /** status */                               private String status = null;
    /** progress */                             private String progress = null;
    
    
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
		this.priority = priority;
		this.upCell = upCell;
		this.downCell = downCell;
        startTime = endTime = 0;
        started = finished = aborted = scheduledToAbort = false;
        myNode = new DefaultMutableTreeNode(this);
        
        databaseChangesThread.addJob(this);

		// should figure out when to start the job properly...for now, just start it
		//Thread t = new Thread(this, jobName);
		//t.start();
    }
	//--------------------------ABSTRACT METHODS--------------------------
    
    /** This is the main work method.  This method should
     * perform all needed tasks.
     */
    public abstract void doIt();
    
    //--------------------------PRIVATE JOB METHODS--------------------------

    /** Locks the database to prevent changes to it */
    private void lockDatabase() {
        
    }
    
    /** Unlocks database */
    private void unlockDatabase() {
        
    }

    //--------------------------PUBLIC JOB METHODS--------------------------

    /** Run gets called after the calling thread calls our start method */
    public void run() {
        startTime = System.currentTimeMillis();

		try {
			if (jobType == Type.CHANGE)	Undo.startChanges(tool, jobName, upCell);
			if (jobType != Type.EXAMINE) changingJob = this;
			doIt();
			if (jobType == Type.CHANGE)	Undo.endChanges();
		} catch (Throwable e) {
			e.printStackTrace(System.out);
		} finally {
			if (jobType == Type.EXAMINE)
			{
				databaseChangesThread.endExamine(this);
			} else {
				changingJob = null;
				Library.clearChangeLocks();
			}
		}

		finished = true;                        // is this redundant with Thread.isAlive()?
        endTime = System.currentTimeMillis();
//        Job.removeJob(this);
        WindowFrame.explorerTreeMinorlyChanged();

		// say something if it took more than a minute
		if (endTime - startTime >= 60*1000)
		{
			// TODO : should 'ding' or make some noise so the user knows the job is done
			System.out.println(this.getInfo());
		}
    }

    protected void setProgress(String progress) {
        this.progress = progress;
        WindowFrame.explorerTreeMinorlyChanged();        
    }        
    
    private String getProgress() { return progress; }

    /** Return run status */
    public boolean isFinished() { return finished; }
    
    /** Tell thread to abort. Extending class should check
     * abort when/where applicable
     */
    public void abort() { 
        if (aborted) { 
            System.out.println("Job already aborted: "+getStatus());
            return;
        }
        scheduledToAbort = true;
        WindowFrame.explorerTreeMinorlyChanged();
    }

	/** Confirmation that thread is aborted */
    protected void setAborted() { aborted = true; WindowFrame.explorerTreeMinorlyChanged(); }
    /** get scheduled to abort status */
    protected boolean getScheduledToAbort() { return scheduledToAbort; }
    /** get abort status */
    public boolean getAborted() { return aborted; }
    
    /** get all jobs iterator */
    public static Iterator getAllJobs() { return allJobs.iterator(); }
    
    /** get status */
    public String getStatus() {
		if (!started) return "waiting";
        if (finished) return "done";
        if (aborted) return "aborted";
        if (scheduledToAbort) return "scheduled to abort";
        if (getProgress() == null) return "running";
        return getProgress();
    }
    
	/**
	 * Returns thread which activated current changes or null if no changes.
	 * @return thread which activated current changes or null if no changes.
	 */
	public static Thread getChangingThread() { return changingJob != null ? databaseChangesThread : null; }

	/**
	 * Returns cell which is root of up-tree of current changes or null, if no changes or whole database changes.
	 * @return cell which is root of up-tree of current changes or null, if no changes or whole database changes.
	 */
	public static Cell getChangingCell() { return changingJob != null ? changingJob.upCell : null; }

	/**
	 * Routing to check whether changing of whole database allowed or not.
	 */
	public static void checkChanging()
	{
		if (changingJob == null)
		{
			System.out.println("Database is changing out if change job");
			//throw new IllegalStateException("Job.checkChanging()");
		} else if (Thread.currentThread() != databaseChangesThread)
		{
			System.out.println("Database is changing by other thread");
			//throw new IllegalStateException("Job.checkChanging()");
		} else if (changingJob.upCell != null)
		{
			System.out.println("Database is changing which only up-tree of "+changingJob.upCell+" is locked");
			//throw new IllegalStateException("Undo.checkChanging()");
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
        JMenu menu;
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
                System.out.println("Cannot delete running jobs.  Wait till finished or abort");
                return;
            }
            databaseChangesThread.removeJob(this);
        }
    }
                
    /** Get info on Job */
    public String getInfo() {
        StringBuffer buf = new StringBuffer();
        buf.append(toString()+"\n");
        Date start = new Date(startTime);
        buf.append("  start time: "+start+"\n");
        if (finished) {
            Date end = new Date(endTime);
            buf.append("  end time: "+end+"\n");
            long time = endTime - startTime;
            buf.append("  time taken: "+Job.getElapsedTime(time)+"\n");
        }
        return buf.toString();
    }
    
    /** Get a string representing elapsed time.
     * format: days : hours : minutes : seconds */
    public static String getElapsedTime(long milliseconds) {
        StringBuffer buf = new StringBuffer();
        int seconds = (int)milliseconds/1000;
        if (seconds < 0) seconds = 0;
        int days = seconds/86400;
        buf.append(days+" days : ");
        seconds = seconds - (days*86400);
        int hours = seconds/3600;
        buf.append(hours+" hrs : ");
        seconds = seconds - (hours*3600);
        int minutes = seconds/60;
        buf.append(minutes+" mins : ");
        seconds = seconds - (minutes*60);
        buf.append(seconds+" secs");
        return buf.toString();
    }
        
}
