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
package com.sun.electric.tool.user;

import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.Version;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.menus.MenuBar;
import com.sun.electric.tool.user.ui.ToolBarButton;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.Point2D;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

public class ActivityLogger {

    /** log menu activations */         private static boolean logMenuActivations;
    /** log jobs starting */            private static boolean logJobs;
    /** timestamp everything */         private static boolean logTimeStamps;
    /** if an exception was logged */   private static boolean exceptionLogged;

    /** Writer */                       private static PrintWriter out = null;
    /** the output file */              private static String outputFile = "electric.log";
    /** log count */                    private static int loggedCount = 0;

    /**
     * Initialize the Activity Logger
     * @param logMenuActivations true to log menu activations
     * @param logJobs true to log jobs starting
     * @param useTimeStamps true to include time stamps (not recommended, makes file harder to read)
     */
    public static synchronized void initialize(boolean logMenuActivations, boolean logJobs, boolean useTimeStamps) {
        ActivityLogger.logMenuActivations = logMenuActivations;
        ActivityLogger.logJobs = logJobs;
        ActivityLogger.logTimeStamps = useTimeStamps;

        outputFile = System.getProperty("user.dir") + File.separator + outputFile;
        try {
            FileOutputStream fos = new FileOutputStream(outputFile, false);
            BufferedOutputStream bout = new BufferedOutputStream(fos);
            out = new PrintWriter(bout);
            // redirect stderr to the log file
            //System.setErr(new PrintStream(bout, true));
        } catch (IOException e) {
            System.out.println("Warning: Activity Log disabled: "+e.getMessage());
        }
        if (out != null) out.println("Electric "+Version.getVersionInformation());
    }

    /**
     * Call to close output writer and warn user if any exceptions were logged.
     */
    public static synchronized void finished() {
        if (out != null) out.close();
        if (exceptionLogged) {
            JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), new String []
            { "Exception logged.  Please send ", "   \""+outputFile+"\"", "to the developers"},
                    "Exception Logged", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Log a menu activation
     * @param m the menu activated
     */
    public static synchronized void logMenuActivated(JMenuItem m) {
        if (out == null) return;
        if (!logMenuActivations) return;
        printDelimeter(true);
        out.println("Menu Activated: "+((MenuBar.MenuItemInterface)m).getDescription());
        //System.out.println("Menu Activated: "+((MenuBar.MenuItemInterface)m).getDescription());
        WindowFrame frame = WindowFrame.getCurrentWindowFrame();
        if (frame != null) {
            WindowContent content = frame.getContent();
            if (content != null) {
                Highlighter h = content.getHighlighter();
                if (h != null) {
                    logHighlights(h.getHighlights(), h.getHighlightOffset());
                }
            }
        }
    }

    /**
     * Log a tool bar button activation
     * @param b the tool bar button activated
     */
    public static synchronized void logToolBarButtonActivated(ToolBarButton b) {
        if (out == null) return;
        if (!logMenuActivations) return;
        printDelimeter(true);
        out.println("ToolBarButton Activated: "+b.getName());
    }

    /**
     * Log a Job. Logs at the start of the job, and also logs the highlights at the time
     * the job started.
     * @param jobName the job name
     * @param jobType the job type
     * @param upCell the upCell cell
     * @param savedHighlights the starting highlights
     * @param savedHighlightsOffset the starting highlight offset (currently not used)
     */
    public static synchronized void logJobStarted(String jobName, Job.Type jobType, Cell upCell,
                                                  List savedHighlights, Point2D savedHighlightsOffset) {
        if (out == null) return;
        if (!logJobs) return;
        printDelimeter(true);
        Cell cell = WindowFrame.getCurrentCell();
        String cellName = (cell == null) ? "none" : cell.libDescribe();
        Exception e = new Exception("stack trace");
        out.println("Job Started [Current Cell: "+cellName+"] "+jobName+", "+jobType);
        //System.out.println("Job Started [Current Cell: "+cellName+"] "+jobName+", "+jobType);
        logHighlights(savedHighlights, savedHighlightsOffset);
    }

    /**
     * Log a list of Highlight objects and their offset
     * @param highlights a list of Highlight objects
     * @param offset the offset
     */
    public static synchronized void logHighlights(List highlights, Point2D offset) {
        if (out == null) return;
        if (highlights.size() == 0) return;
        out.println("Currently highlighted: ");
        for (Iterator it = highlights.iterator(); it.hasNext(); ) {
            Highlight h = (Highlight)it.next();
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
        String [] msg = {"Exception Caught!!!", "The exception below has been logged.",
                         "Please send \""+outputFile+ "\" to the developers",
                         "   " + e.toString() };
	    if (!Main.BATCHMODE)
            JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), msg, "Exception Caught", JOptionPane.ERROR_MESSAGE);
	    else
	        System.out.println(msg);
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
        Exception e = new Exception("stack trace");
        //if (printThreadInfo) out.println("(Thread: "+Thread.currentThread()+", stack size: "+e.getStackTrace().length+")");
        if (logTimeStamps) {
            Date date = new Date(System.currentTimeMillis());
            out.println("  "+date);
        }
        loggedCount++;
    }
}
