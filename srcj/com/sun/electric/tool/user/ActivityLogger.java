package com.sun.electric.tool.user;

import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.user.menus.MenuBar;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.ToolBarButton;
import com.sun.electric.tool.Job;

import javax.swing.*;
import java.io.*;
import java.util.List;
import java.util.Iterator;
import java.util.Date;
import java.awt.geom.Point2D;

/**
 * Created by IntelliJ IDEA.
 * User: root
 * Date: Aug 22, 2004
 * Time: 10:38:28 AM
 * To change this template use File | Settings | File Templates.
 */
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
        } catch (IOException e) {
            System.out.println("Warning: Activity Log disabled: "+e.getMessage());
        }
    }

    /**
     * Call to close output writer and warn user if any exceptions were logged.
     */
    public static synchronized void finished() {
        out.close();
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
        printDelimeter();
        out.println("Menu Activated: "+((MenuBar.MenuItemInterface)m).getDescription());
    }

    /**
     * Log a tool bar button activation
     * @param b the tool bar button activated
     */
    public static synchronized void logToolBarButtonActivated(ToolBarButton b) {
        if (out == null) return;
        if (!logMenuActivations) return;
        printDelimeter();
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
        printDelimeter();
        Cell cell = WindowFrame.getCurrentCell();
        String cellName = (cell == null) ? "none" : cell.libDescribe();
        out.println("Job Started [Current Cell: "+cellName+"] "+jobName+", "+jobType);
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
            printDelimeter();
            e.printStackTrace(out);
            out.flush();
        }
        e.printStackTrace(System.out);
        String [] msg = {"Exception Caught!!!", "The exception below has been logged.",
                         "Please send \""+outputFile+ "\" to the developers",
                         "   " + e.toString() };
        JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), msg, "Exception Caught", JOptionPane.ERROR_MESSAGE);
        exceptionLogged = true;
    }

    /**
     * Log some message
     * @param msg the message
     */
    public static synchronized void logMessage(String msg) {
        if (out == null) return;
        printDelimeter();
        out.println(msg);
    }

    /**
     * Print a delimiter between log events for easier reading of the log file
     */
    private static void printDelimeter() {
        if (out == null) return;
        out.println("--------------- "+loggedCount+ " --------------");
        if (logTimeStamps) {
            Date date = new Date(System.currentTimeMillis());
            out.println("  "+date);
        }
        loggedCount++;
    }
}
