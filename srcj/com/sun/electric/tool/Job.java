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
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.IdReader;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.database.id.LibId;
import com.sun.electric.database.id.TechId;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.CantEditException;
import com.sun.electric.tool.user.ErrorLogger;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
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
 * <p>On normal termination in CHANGE or REMOTE_EXAMINE mode "fieldVariableChanged" variables are serialized.
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
    static final int PROTOCOL_VERSION = 19; // Apr 17
    public static boolean LOCALDEBUGFLAG; // Gilda's case
//    private static final String CLASS_NAME = Job.class.getName();
    static final Logger logger = Logger.getLogger("com.sun.electric.tool.job");


    /**
	 * Method to tell whether Electric is running in "debug" mode.
	 * If the program is started with the "-debug" switch, debug mode is enabled.
	 * @return true if running in debug mode.
	 */
    public static boolean getDebug() { return GLOBALDEBUG; }

    public static void setDebug(boolean f) { GLOBALDEBUG = f; }

    // ---------------------------- public methods ---------------------------

    /**
	 * Type is a typesafe enum class that describes the type of job (CHANGE or EXAMINE).
	 */
	public static enum Type {
		/** Describes a server database change. */      CHANGE,
		/** Describes a server database undo/redo. */   UNDO,
		/** Describes a server database examination. */	SERVER_EXAMINE,

		/** Describes a client database examination. */ CLIENT_EXAMINE;

        public boolean isExamine() {
            return this == Job.Type.CLIENT_EXAMINE || this == Job.Type.SERVER_EXAMINE;
        }
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


	/** default execution time in milis */      /*private*/ public static final int MIN_NUM_SECONDS = 60000;
    /** job manager */                          /*private*/ static ServerJobManager serverJobManager;
    /** job manager */                          /*private*/ static ClientJobManager clientJobManager;
	static AbstractUserInterface currentUI;
    static Thread clientThread;

    /** delete when done if true */             /*private*/ boolean deleteWhenDone;

    // Job Status
    /** job start time */                       public /*protected*/ long startTime;
    /** job end time */                         public /*protected*/ long endTime;
    /** was job started? */                     /*private*/boolean started;
    /** is job finished? */                     /*private*/ public boolean finished;
    /** thread aborted? */                      /*private*/ boolean aborted;
    /** schedule thread to abort */             /*private*/ boolean scheduledToAbort;
	/** report execution time regardless MIN_NUM_SECONDS */
												/*private*/ public boolean reportExecution = false;
    /** tool running the job */                 /*private*/ Tool tool;
    /** current technology */                   final TechId curTechId;
    /** current library */                      final LibId curLibId;
    /** current Cell */                         final CellId curCellId;
//    /** priority of job */                      private Priority priority;
//    /** bottom of "up-tree" of cells affected */private Cell upCell;
//    /** top of "down-tree" of cells affected */ private Cell downCell;
//    /** status */                               private String status = null;

    transient EJob ejob;
    transient EDatabase database;

    public static void initJobManager(int numThreads, String loggingFilePath, int socketPort, AbstractUserInterface ui, Job initDatabaseJob) {
        currentUI = ui;
        serverJobManager = new ServerJobManager(numThreads, loggingFilePath, false, socketPort);
        serverJobManager.runLoop(initDatabaseJob);
    }

    public static void pipeServer(int numThreads, String loggingFilePath, int socketPort) {
        Pref.forbidPreferences();
        EDatabase.setServerDatabase(new EDatabase(IdManager.stdIdManager.getInitialSnapshot()));
        Tool.initAllTools();
        serverJobManager = new ServerJobManager(numThreads, loggingFilePath, true, socketPort);
    }

    public static void socketClient(String serverMachineName, int socketPort, AbstractUserInterface ui, Job initDatabaseJob) {
        currentUI = ui;
        try {
            clientJobManager = new ClientJobManager(serverMachineName, socketPort);
            clientJobManager.runLoop(initDatabaseJob);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void pipeClient(Process process, AbstractUserInterface ui, Job initDatabaseJob, boolean skipOneLine) {
        currentUI = ui;
        try {
            clientJobManager = new ClientJobManager(process, skipOneLine);
            clientJobManager.runLoop(initDatabaseJob);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public static Mode getRunMode() { return threadMode; }

//    public static int getNumThreads() { return recommendedNumThreads; }

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
        ejob = new EJob(this, jobType, jobName, EditingPreferences.getThreadEditingPreferences());
        UserInterface ui = getUserInterface();
        database = ui != null ? ui.getDatabase() : EDatabase.clientDatabase();
		this.tool = tool;
        this.deleteWhenDone = true;
        startTime = endTime = 0;
        Technology curTech = ui != null ? ui.getCurrentTechnology() : null;
        curTechId = curTech != null ? curTech.getId() : null;
        Library curLib = ui != null ? ui.getCurrentLibrary() : null;
        curLibId = curLib != null ? curLib.getId() : null;
        Cell curCell = ui != null ? ui.getCurrentCell() : null;
        curCellId = curCell != null ? curCell.getId() : null;
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
        startJob(true);
    }

    /**
     * Start a job on snapshot obtained at the end of current job. By default displays Job on Job List UI, and
     * delete Job when done.
     */
	public void startJobOnMyResult()
	{
        startJob(true, true);
    }

    /**
     * Start the job by placing it on the JobThread queue.
     * If <code>deleteWhenDone</code> is true, Job will be deleted
     * after it is done (frees all data and references it stores/created)
     * @param deleteWhenDone delete when job is done if true, otherwise leave it around
     */
    public void startJob(boolean deleteWhenDone) {
        startJob(deleteWhenDone, false);
    }

    /**
     * Start the job by placing it on the JobThread queue.
     * If <code>deleteWhenDone</code> is true, Job will be deleted
     * after it is done (frees all data and references it stores/created)
     * @param deleteWhenDone delete when job is done if true, otherwise leave it around
     */
    private void startJob(boolean deleteWhenDone, boolean onMySnapshot)
    {
        this.deleteWhenDone = deleteWhenDone;

        UserInterface ui = getUserInterface();
        Job.Key curJobKey = ui.getJobKey();
        boolean startedByServer = curJobKey.doItOnServer;
        boolean doItOnServer = ejob.jobType != Job.Type.CLIENT_EXAMINE;
        if (startedByServer) {
            assert doItOnServer;
            ejob.client = Job.serverJobManager.serverConnections.get(curJobKey.clientId);
            ejob.jobKey = ejob.client.newJobId(startedByServer, doItOnServer);
            ejob.serverJob.startTime = System.currentTimeMillis();
            ejob.serialize(EDatabase.serverDatabase());
            ejob.clientJob = null;
            Job.serverJobManager.addJob(ejob, onMySnapshot);
        } else {
            ejob.client = Job.currentUI;
            ejob.jobKey = ejob.client.newJobId(startedByServer, doItOnServer);
            ejob.clientJob.startTime = System.currentTimeMillis();
            ejob.serverJob = null;
            if (doItOnServer) {
                ejob.serialize(EDatabase.clientDatabase());
                Job.currentUI.putProcessingEJob(ejob, onMySnapshot);
                if (serverJobManager != null) {
                    serverJobManager.addJob(ejob, onMySnapshot);
                } else {
                    assert SwingUtilities.isEventDispatchThread();
                    try {
                        clientJobManager.writeEJob(ejob);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Job.currentUI.putProcessingEJob(ejob, onMySnapshot);
            }
        }
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
        if (serverJobManager != null)
            serverJobManager.setProgress(ejob, progress);
        else
            ejob.progress = progress;
    }

    private synchronized String getProgress() { return ejob.progress; }

    /** Return run status */
    public boolean isFinished() { return finished; }

    /** Tell thread to abort. Extending class should check
     * abort when/where applicable
     */
    public synchronized void abort() {
//        if (ejob.jobType != Job.Type.EXAMINE) return;
        if (aborted) {
            System.out.println("Job already aborted: "+getStatus());
            return;
        }
        scheduledToAbort = true;
        if (ejob.jobType != Job.Type.CLIENT_EXAMINE && ejob.serverJob != null)
            ejob.serverJob.scheduledToAbort = true;
//        Job.getUserInterface().wantToRedoJobTree();
    }

	/** Confirmation that thread is aborted */
    private synchronized void setAborted() { aborted = true; /*Job.getUserInterface().wantToRedoJobTree();*/ }
    /** get scheduled to abort status */
    protected synchronized boolean getScheduledToAbort() { return scheduledToAbort; }
    /**
     * Method to get abort status. Please leave this function as private.
     * To retrieve abort status from another class, use checkAbort which also
     * checks if job is scheduled to be aborted.
     * @return
     */
    private synchronized boolean getAborted() { return aborted; }
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
//        if (ejob.jobType != Job.Type.EXAMINE) return false;
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
        return serverJobManager != null ? serverJobManager.getAllJobs() : currentUI.getAllJobs().iterator();
    }

    /**
     * If this current thread is a EThread running a Job return the Job.
     * Return null otherwise.
     * @return a running Job or null
     */
    public static Job getRunningJob() {
        Thread thread = Thread.currentThread();
        return thread instanceof EThread ? ((EThread)thread).getRunningJob() : null;
    }

    /**
     * Returns true if this current thread is a EThread running a server Job.
     * @return true if this current thread is a EThread running a server Job.
     */
    public static boolean inServerThread() {
        Thread thread = Thread.currentThread();
        return thread instanceof EThread && ((EThread)thread).isServerThread;
    }

    public static void setCurrentLibraryInJob(Library lib) {
        EThread thread = (EThread)Thread.currentThread();
        assert thread.ejob.jobType == Type.CHANGE;
        thread.userInterface.curLibId = lib != null ? lib.getId() : null;
    }

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
        if (serverJobManager != null)
            serverJobManager.removeJob(this);
        else
           currentUI.removeProcessingEJob(getKey());
        return true;
    }

    private static final ThreadLocal<UserInterface> threadUserInterface = new ThreadLocal<UserInterface>() {
//        @Override
//        protected UserInterface initialValue() {
//            throw new IllegalStateException();
//        }
    };

    public static UserInterface getUserInterface() {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof EThread)
            return ((EThread)currentThread).getUserInterface();
        else if (currentThread == clientThread)
            return currentUI;
        else
            return threadUserInterface.get();
    }

    public static void setUserInterface(UserInterface ui) {
        Thread currentThread = Thread.currentThread();
        assert !(currentThread instanceof EThread) && currentThread != clientThread;
        if (ui == null)
            throw new UnsupportedOperationException();
        threadUserInterface.set(ui);
    }

    /**
     * Low-level method.
     */
    public static AbstractUserInterface getExtendedUserInterface() {
//        if (!isClientThread())
//            ActivityLogger.logException(new IllegalStateException());
        return currentUI;
    }

    public static boolean isClientThread() {
        return Thread.currentThread() == clientThread;
    }

    public EDatabase getDatabase() {
        return database;
    }

    public Environment getEnvironment() {
        return database.getEnvironment();
    }

    public TechPool getTechPool() {
        return database.getTechPool();
    }

    public Tool getTool() {
        return tool;
    }

    public EditingPreferences getEditingPreferences() {
        return ejob.editingPreferences;
    }

    public static void updateNetworkErrors(Cell cell, List<ErrorLogger.MessageLog> errors) {
        if (currentUI != null)
            currentUI.updateNetworkErrors(cell, errors);
    }

    public static void updateIncrementalDRCErrors(Cell cell, List<ErrorLogger.MessageLog> newErrors,
                                                  List<ErrorLogger.MessageLog> delErrors) {
        currentUI.updateIncrementalDRCErrors(cell, newErrors, delErrors);
    }

    /**
     * Find some valid snapshot in cache.
     * @return some valid snapshot
     */
    public static Snapshot findValidSnapshot() {
        return EThread.findValidSnapshot();
    }

	//-------------------------------JOB UI--------------------------------

    public String toString() { return ejob.jobName+" ("+getStatus()+")"; }

    /**
     * Print a message, dump a stack trace, and throw a RuntimeException if
     * errorHasOccurred argument is true.
     *
     * @param errorHasOccurred indicates a runtime error has been detected
     * @param msg the message to print when an error occurs
     * @throws RuntimeException if errorHasOccurred is true
     */
    public static void error(boolean errorHasOccurred, String msg) {
        if (!errorHasOccurred) return;
        RuntimeException e = new RuntimeException(msg);
        // The following prints a stack trace on the console
        ActivityLogger.logException(e);

        // The following prints a stack trace in the Electric messages window
        throw e;
    }

    /** Get info on Job */
    public String getInfo() {
        StringBuffer buf = new StringBuffer();
        buf.append("Job "+toString());
        //buf.append("  start time: "+start+"\n");
        if (finished) {
//            Date end = new Date(endTime);
            //buf.append("  end time: "+end+"\n");
            long time = endTime - startTime;
            buf.append(" took: "+TextUtils.getElapsedTime(time));
            Date start = new Date(startTime);
            buf.append(" (started at "+start+")");
        } else if (getProgress() == null) {
            long time = System.currentTimeMillis()-startTime;
	        buf.append(" has not finished. Current running time: " + TextUtils.getElapsedTime(time));
        } else {
            buf.append(" did not successfully finish.");
        }
        return buf.toString();
    }

//    static void runTerminate(EJob ejob) {
//        Throwable jobException = ejob.deserializeResult();
//        Job job = ejob.clientJob;
//        try {
//            job.terminateIt(jobException);
//        } catch (Throwable e) {
//            System.out.println("Exception executing terminateIt");
//            e.printStackTrace(System.out);
//        }
//        job.endTime = System.currentTimeMillis();
//        job.finished = true;                        // is this redundant with Thread.isAlive()?
//
//        // say something if it took more than a minute by default
//        if (job.reportExecution || (job.endTime - job.startTime) >= MIN_NUM_SECONDS) {
//            if (User.isBeepAfterLongJobs()) {
//                Toolkit.getDefaultToolkit().beep();
//            }
//            System.out.println(job.getInfo());
//        }
//    }

    public Key getKey() {
        return ejob.jobKey;
    }

    public Inform getInform() {
        return new Inform(this);
    }

    /**
     * Identifies a Job in a given Electric client/server session.
     * Job obtains its Key in startJob method.
     * Also can identify Jobless context (for example Client's Gui)
     */
    public static class Key implements Serializable {
        /**
         * Client which launched the Job
         */
        public final int clientId;
        /**
         * Job id.
         * 0         - Jobless context
         * positive  - Job started from server side
         * negative  - Job started from client side
         */
        public final int jobId;
        public final boolean doItOnServer;

        Key(int clientId, int jobId, boolean doItOnServer) {
            this.clientId = clientId;
            this.jobId = jobId;
            this.doItOnServer = doItOnServer;
        }

        Key(Client client, int jobId, boolean doItOnServer) {
            this(client.connectionId, jobId, doItOnServer);
        }

        public boolean startedByServer() {
            return jobId > 0;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o instanceof Key) {
                Key that = (Key)o;
                if (this.clientId == that.clientId && this.jobId == that.jobId) {
                    assert this.doItOnServer == that.doItOnServer;
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return jobId;
        }

        public void write(IdWriter writer) throws IOException {
            writer.writeInt(clientId);
            writer.writeInt(jobId);
            writer.writeBoolean(doItOnServer);
        }

        public static Key read(IdReader reader) throws IOException {
            int clientId = reader.readInt();
            int jobId = reader.readInt();
            boolean doItOnServer = reader.readBoolean();
            return new Key(clientId, jobId, doItOnServer);
        }
    }

    public static class Inform implements Serializable {
        private final Key jobKey;
        private final boolean isChange;
        private final String toString;
        private final long startTime;
        private final long endTime;
        private final int finished;

        Inform(Job job) {
            jobKey = job.getKey();
            isChange = (job.ejob.jobType == Type.CHANGE) || (job.ejob.jobType == Type.UNDO);
            toString = job.toString();
            startTime = job.startTime;
            endTime = job.endTime;
            if (job.finished)
                finished = 1;
            else if (job.getProgress() == null)
                finished = 0;
            else
                finished = -1;
        }

        Inform(Key jobKey, boolean isChange, String toString, long startTime, long endTime, int finished) {
            this.jobKey = jobKey;
            this.isChange = isChange;
            this.toString = toString;
            this.startTime = startTime;
            this.endTime = endTime;
            this.finished = finished;
        }

        public void abort() {
            for (Iterator<Job> it = getAllJobs(); it.hasNext(); ) {
                Job job = it.next();
                if (job.getKey().equals(jobKey)) {
                    job.abort();
                    break;
                }
            }
        }
        public boolean remove() { return false; /*return job.remove();*/ }

        public Key getKey() { return jobKey; }

        @Override
        public String toString() { return toString; }

        public String getInfo() {
            StringBuilder buf = new StringBuilder();
            buf.append("Job "+toString());
            //buf.append("  start time: "+start+"\n");
            if (finished == 1) {
    //            Date end = new Date(endTime);
                //buf.append("  end time: "+end+"\n");
                long time = endTime - startTime;
                buf.append(" took: "+TextUtils.getElapsedTime(time));
                Date start = new Date(startTime);
                buf.append(" (started at "+start+")");
            } else if (finished == 0) {
                long time = System.currentTimeMillis()-startTime;
                buf.append(" has not finished. Current running time: " + TextUtils.getElapsedTime(time));
            } else {
                buf.append(" did not successfully finish.");
            }
            return buf.toString();
        }

        public boolean isChangeJobQueuedOrRunning() {
            return finished != 1 && isChange;
        }

        public void write(IdWriter writer) throws IOException {
            jobKey.write(writer);
            writer.writeBoolean(isChange);
            writer.writeString(toString);
            writer.writeLong(startTime);
            writer.writeLong(endTime);
            writer.writeInt(finished);
        }

        public static Inform read(IdReader reader) throws IOException {
            Key jobKey = Key.read(reader);
            boolean isChange = reader.readBoolean();
            String toString = reader.readString();
            long startTime = reader.readLong();
            long endTime = reader.readLong();
            int finished = reader.readInt();
            return new Inform(jobKey, isChange, toString, startTime, endTime, finished);
        }
    }
}
