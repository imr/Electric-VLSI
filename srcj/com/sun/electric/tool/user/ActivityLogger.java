/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ActivityLogger.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Client;

import java.awt.geom.Point2D;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.security.SecureRandom;

/**
 * Class to log job activity.
 */
public class ActivityLogger {

	/** true if a test version */		private static final boolean TEST_VERSION = Version.getVersion().getDetail() != 999;
    /** log menu activations */         private static boolean logMenuActivations;
    /** log jobs starting */            private static boolean logJobs;
    /** timestamp everything */         private static boolean logTimeStamps;
    /** if an exception was logged */   private static boolean exceptionLogged;

    /** Writer */                       private static PrintWriter out = null;
    /** the output file */              private static String outputFile;
    /** log count */                    private static int loggedCount = 0;
    private static final SecureRandom random = new SecureRandom();

    /**
     * Initialize the Activity Logger
     * @param logMenuActivations true to log menu activations
     * @param logJobs true to log jobs starting
     * @param useTimeStamps true to include time stamps (not recommended, makes file harder to read)
     * @param enableLog true to enable logging of events in Electric's log file
     * @param multipleLog true to generate different logfile names
     */
    public static synchronized void initialize(String fileName, boolean logMenuActivations, boolean logJobs,
                                               boolean useTimeStamps, boolean enableLog, boolean multipleLog) {
        ActivityLogger.logMenuActivations = logMenuActivations;
        ActivityLogger.logJobs = logJobs;
        ActivityLogger.logTimeStamps = useTimeStamps;

        if (!enableLog) return; // nothing to do.

        String dirName = Client.isOSMac() ? System.getProperty("user.home") : System.getProperty("user.dir");
        outputFile = dirName + File.separator + fileName;
        File dir = new File(dirName);
        
        try {
//            outputFile = File.createTempFile(fileName+"-", ".log").getAbsolutePath();
            if (multipleLog)
            {
                long n = random.nextLong();
                n = (n == Long.MIN_VALUE) ? 0 : Math.abs(n); // to cover corner case
                outputFile = outputFile+"-"+ n+".log";
            }
            else
                outputFile = outputFile + ".log";
            File file = new File(outputFile);

            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bout = new BufferedOutputStream(fos);
            out = new PrintWriter(bout);
            // redirect stderr to the log file
            //System.setErr(new PrintStream(bout, true));
        } catch (IOException e) {
            System.out.println("Warning: Activity Log disabled: "+e.getMessage());
            System.out.println("Activity logs are sent to the standard output once you close Electric.");
            out = new PrintWriter(System.out);
        }
        if (out != null) out.println("Electric "+Version.getVersionInformation());
    }

    /**
     * Return the name of the log file so we can display it in the
     * messages window once the messages window is open.
     */
    public static String getLoggingInformation() 
    {
        if (outputFile != null)
            return "Electric's log file is " + outputFile + ".\n";
        return "Logging is disable. Electric must be restarted to resume logging.\n";
    }

    /**
     * Call to close output writer and warn user if any exceptions were logged.
     */
	public static synchronized void finished() {
		if (out != null) out.close();
		if (exceptionLogged && TEST_VERSION) {
			Job.getUserInterface().showInformationMessage(
			    "Exception logged.  Please send \n   \""+outputFile+"\"\n to the developers",
				"Exception Logged");
		}
	}

    /**
     * Log a menu activation
     * @param menuDescription description of the menu activated
     */
    public static synchronized void logMenuActivated(String menuDescription) {
        if (out == null) return;
        if (!logMenuActivations) return;
        printDelimeter(true);
        out.println("Menu Activated: "+menuDescription);
        //System.out.println("Menu Activated: "+((MenuBar.MenuItemInterface)m).getDescription());
        UserInterface ui = Job.getUserInterface();
        EditWindow_ wnd = ui.getCurrentEditWindow_();
        if (wnd != null)
        {
        	List<Highlight> savedContents = wnd.saveHighlightList();
            if (savedContents != null)
            {
                logHighlights(savedContents, wnd.getHighlightOffset());
            }
        }
    }

    /**
     * Log a tool bar button activation
     * @param buttonName the tool bar button activated
     */
    public static synchronized void logToolBarButtonActivated(String buttonName) {
        if (out == null) return;
        if (!logMenuActivations) return;
        printDelimeter(true);
        out.println("ToolBarButton Activated: "+buttonName);
    }

    /**
     * Log a Job. Logs at the start of the job, and also logs the highlights at the time
     * the job started.
     * @param jobName the job name
     * @param jobType the job type
     * @param cell the current cell
     * @param savedHighlights the starting highlights
     * @param savedHighlightsOffset the starting highlight offset (currently not used)
     */
    public static synchronized void logJobStarted(String jobName, Job.Type jobType, Cell cell,
                                                  List<Highlight> savedHighlights, Point2D savedHighlightsOffset) {
        if (out == null) return;
        if (!logJobs) return;
        printDelimeter(true);
        String cellName = (cell == null) ? "none" : cell.libDescribe();
        new Exception("stack trace");
        out.println("Job Started [Current Cell: "+cellName+"] "+jobName+", "+jobType);
        //System.out.println("Job Started [Current Cell: "+cellName+"] "+jobName+", "+jobType);
        logHighlights(savedHighlights, savedHighlightsOffset);
    }

    /**
     * Log a list of Highlight objects and their offset
     * @param highlights a list of Highlight objects
     * @param offset the offset
     */
    public static synchronized void logHighlights(List<Highlight> highlights, Point2D offset) {
        if (out == null) return;
        if (highlights.size() == 0) return;
        out.println("Currently highlighted: ");
        for (Highlight h: highlights) {
            out.println("    "+h.describe());
        }
    }

    /**
     * Log the time
     * @param time the current time in milliseconds (System.currentTimeMillis)
     */
    public static synchronized void logTime(long time) {
        if (out == null) return;
        if (!logTimeStamps) return;
        Date date = new Date(time);
        out.println("Time: "+date);
    }

    /**
     * Log an Exception. If an exception is logged, the user is prompted when
     * Electric exits to send the log file to a developer.
     * @param e the exeception
     */
    public static synchronized void logException(Throwable e) {
        if (out != null) {
            printDelimeter(true);
            e.printStackTrace(out);
            out.flush();
        }
        e.printStackTrace(System.out);
        String msg1 = "Exception Caught!!!";
        String msg2 = "The exception below has been logged in '" +outputFile+"'.";
		String msg3 = "Please help us and report error to developers using 'Bugzilla'. In case of no access, send logfile to the developers.";
		String msg4 = e.toString();
		String msg;
		if (TEST_VERSION)
		{
			msg = msg1 + "\n" + msg2 + "\n" + msg3 + "\n" + msg4;
		} else
		{
			msg = msg1 + "\n" + msg2 + "\n" + msg4;
		}
        if (Job.getUserInterface() != null) {
            Job.getUserInterface().showErrorMessage(msg, "Exception Caught");
        } else {
            System.out.println(msg);
        }
        exceptionLogged = true;
    }

    /**
     * Log some message
     * @param msg the message
     */
    public static synchronized void logMessage(String msg) {
        if (out == null) return;
        printDelimeter(true);
        out.println(msg);
    }

    /** Temp debug method */
    public static synchronized void logThreadMessage(String msg) {
        if (out == null) return;
        printDelimeter(false);
        out.println(msg);
    }

    /**
     * Print a delimiter between log events for easier reading of the log file
     */
    private static synchronized void printDelimeter(boolean printThreadInfo) {
        if (out == null) return;
        out.println("--------------- "+loggedCount+ " --------------");
        new Exception("stack trace");
        //if (printThreadInfo) out.println("(Thread: "+Thread.currentThread()+", stack size: "+e.getStackTrace().length+")");
        if (logTimeStamps) {
            Date date = new Date(System.currentTimeMillis());
            out.println("  "+date);
        }
        loggedCount++;
    }
}
