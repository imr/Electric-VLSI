/* -*- tab-width: 4 -*-
 *
 * Job.java
 *
 * Created on November 18, 2003, 11:29 AM
 */

package com.sun.electric.tool;

import com.sun.electric.tool.user.ui.WindowFrame;

import java.lang.Thread;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.sql.Time;
import java.io.*;
import javax.swing.tree.*;
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
public abstract class Job extends Thread implements ActionListener {

    // Job Management
    /** all jobs */                             private static ArrayList allJobs = new ArrayList();
    /** job tree */                             private static DefaultMutableTreeNode explorerTree = new DefaultMutableTreeNode("JOBS");
    /** my tree node */                         private DefaultMutableTreeNode myNode;
    
    // Job Status
    /** job start time */                       protected long startTime;
    /** job end time */                         protected long endTime;
    /** is job finished? */                     private boolean finished;
    /** thread aborted? */                      private boolean aborted;
    /** schedule thread to abort */             private boolean scheduledToAbort;
    /** status */                               private String status = null;
    /** progress */                             private String progress = null;
    
    
    /** Creates a new instance of Job */
    public Job(String name) {
        super(name);
        
        startTime = endTime = 0;
        finished = aborted = scheduledToAbort = false;
        myNode = new DefaultMutableTreeNode(this);
        
        Job.addJob(this);
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

    /** Add job to list of jobs */
    private static synchronized void addJob(Job j) { 
        allJobs.add(j); 
        explorerTree.add(j.myNode);
        WindowFrame.explorerTreeChanged();
    }
    /** Remove job from list of jobs */
    private static synchronized void removeJob(Job j) { 
        if (allJobs.indexOf(j) != -1) {
            allJobs.remove(allJobs.indexOf(j));
        }
        explorerTree.remove(j.myNode);
        WindowFrame.explorerTreeChanged();        
    }
    
    
    //--------------------------PUBLIC JOB METHODS--------------------------

    /** Run gets called after the calling thread calls our start method */
    public void run() {
        startTime = System.currentTimeMillis();
        doIt();
        finished = true;                        // is this redundant with Thread.isAlive()?
        endTime = System.currentTimeMillis();
        // TODO : should 'ding' or make some noise so the user knows the job is done
        WindowFrame.explorerTreeMinorlyChanged();        
        System.out.println(this.getInfo());
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
    public synchronized void abort() { 
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
        if (finished) return "done";
        if (aborted) return "aborted";
        if (scheduledToAbort) return "scheduled to abort";
        if (getProgress() == null) return "running";
        return getProgress();
    }
    
    
    //-------------------------------JOB UI--------------------------------
    
    public String toString() { return getName()+" ("+getStatus()+")"; }
        
    /** Build Job explorer tree */
    public static DefaultMutableTreeNode getExplorerTree() {
        explorerTree.removeAllChildren();
        for (Iterator it = allJobs.iterator(); it.hasNext();) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode((Job)it.next());
            explorerTree.add(node);
        }
        return explorerTree;
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
            Job.removeJob(this);
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
