/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ErrorLogger.java
 *
 * Copyright (c) 2004 Sun Microsystems and Free Software
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

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.change.Undo;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.EditWindow;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.*;
import java.util.*;
import java.awt.geom.Point2D;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Holds a log of errors:
 * <p>ErrorLogger errorLogger = ErrorLogger.newInstance(String s): get new logger for s
 * <p>ErrorLog errorLog = errorLogger.logError(string msg, cell c, int k):
 * Create a new log with message 'msg', for cell 'c', with sortKey 'k'.
 * <p>Various methods for adding highlights to errorLog:
 * <pre>
 *   addGeom(Geometric g, boolean s, int l, NodeInst [] p)
 *                                          add geom "g" to error (show if "s" nonzero)
 *   addExport(Export p, boolean s)         add export "pp" to error
 *   addLine(x1, y1, x2, y2)                add line to error
 *   addPoly(POLYGON *p)                    add polygon to error
 *   addPoint(x, y)                         add point to error
 * </pre>
 * <p>To end logging, call errorLogger.termLogging(boolean explain).
 */
public class ErrorLogger implements ActionListener, DatabaseChangeListener {

    private static final int ERRORTYPEGEOM      = 1;
    private static final int ERRORTYPEEXPORT    = 2;
    private static final int ERRORTYPELINE      = 3;
    private static final int ERRORTYPETHICKLINE = 4;
    private static final int ERRORTYPEPOINT     = 5;

    private static class ErrorHighlight
    {
        int         type;
        Geometric   geom;
        PortProto   pp;
        boolean     showgeom;
        double      x1, y1;
        double      x2, y2;
        double      cX, cY;
        Cell        cell;
        VarContext  context;

        /**
         * Method to describe an object on an error.
         */
        public String describe()
        {
            String msg;
            if (geom instanceof NodeInst) msg = "Node " + geom.describe(); else
                msg = "Arc " + geom.describe();
            msg += " in " + context.getInstPath(".");
            return msg;
        }

        public boolean isValid() {
            if (type == ERRORTYPEEXPORT) return pp.isLinked();
            if (type == ERRORTYPEGEOM) return geom.isLinked();
            return true;
        }
    };

    /**
     * Create a Log of a single Error
     */
    public static class MessageLog implements Comparable {
        private String message;
        private int    sortKey;
        private int    index;
        private Cell    logCell;                // cell associated with log (not really used)
        private List   highlights;

        private MessageLog(String message, int sortKey) {
            this.message = message;
            this.sortKey = sortKey;
            index = 0;
            highlights = new ArrayList();
        }

	    /**
		 * Compare objects lexicographically based on string comparator CASE_INSENSITIVE_ORDER
		 * This method doesn't guarantee (compare(x, y)==0) == (x.equals(y))
		 * @param o1
		 * @return Returns a negative integer, zero, or a positive integer as the
		 * first message has smaller than, equal to, or greater than the second lexicographically
		 */
	    public int compareTo(Object o1)
	    {
		    MessageLog log1 = (MessageLog)o1;
		    return (String.CASE_INSENSITIVE_ORDER.compare(message, log1.message));
	    }

        /**
         * Method to add "geom" to the error in "errorlist".  Also adds a
         * hierarchical traversal path "path" (which is "pathlen" long).
         */
        public void addGeom(Geometric geom, boolean showit, Cell cell, VarContext context)
        {
            ErrorHighlight eh = new ErrorHighlight();
            eh.type = ERRORTYPEGEOM;
            eh.geom = geom;
            eh.showgeom = showit;
            eh.cell = cell;
            eh.context = context;
            highlights.add(eh);
        }

        /**
         * Method to add "pp" to the error in "errorlist".
         */
        public void addExport(Export pp, boolean showit, Cell cell, VarContext context)
        {
            ErrorHighlight eh = new ErrorHighlight();
            eh.type = ERRORTYPEEXPORT;
            eh.pp = pp;
            eh.showgeom = showit;
            eh.cell = cell;
            eh.context = context;
            highlights.add(eh);
        }

        /**
         * Method to add line (x1,y1)=>(x2,y2) to the error in "errorlist".
         */
        public void addLine(double x1, double y1, double x2, double y2, Cell cell)
        {
            ErrorHighlight eh = new ErrorHighlight();
            eh.type = ERRORTYPELINE;
            eh.x1 = x1;
            eh.y1 = y1;
            eh.x2 = x2;
            eh.y2 = y2;
            eh.cell = cell;
            eh.context = null;
            highlights.add(eh);
        }

        /**
         * Method to add polygon "poly" to the error in "errorlist".
         */
        public void addPoly(Poly poly, boolean thick, Cell cell)
        {
            Point2D [] points = poly.getPoints();
            Point2D center = new Point2D.Double(poly.getCenterX(), poly.getCenterY());
            for(int i=0; i<points.length; i++)
            {
                int prev = i-1;
                if (i == 0) prev = points.length-1;
                ErrorHighlight eh = new ErrorHighlight();
                if (thick) eh.type = ERRORTYPETHICKLINE; else
                    eh.type = ERRORTYPELINE;
                eh.x1 = points[prev].getX();
                eh.y1 = points[prev].getY();
                eh.x2 = points[i].getX();
                eh.y2 = points[i].getY();
                eh.cX = center.getX();
                eh.cY = center.getY();
                eh.cell = cell;
                eh.context = null;
                highlights.add(eh);
            }
        }

        /**
         * Method to add point (x,y) to the error in "errorlist".
         */
        public void addPoint(double x, double y, Cell cell)
        {
            ErrorHighlight eh = new ErrorHighlight();
            eh.type = ERRORTYPEPOINT;
            eh.x1 = x;
            eh.y1 = y;
            eh.cell = cell;
            eh.context = null;
            highlights.add(eh);
        }

        /**
         * Method to return the number of objects associated with error "e".  Only
         * returns "geom" objects
         */
        public int getNumGeoms()
        {
            int total = 0;
            for(Iterator it = highlights.iterator(); it.hasNext(); )
            {
                ErrorHighlight eh = (ErrorHighlight)it.next();
                if (eh.type == ERRORTYPEGEOM) total++;
            }
            return total;
        }

        /**
         * Method to return a specific object in a list of objects on this ErrorLog.
         * Returns null at the end of the list.
         */
        /*
        public ErrorHighlight getErrorGeom(int index)
        {
            int total = 0;
            for(Iterator it = highlights.iterator(); it.hasNext(); )
            {
                ErrorHighlight eh = (ErrorHighlight)it.next();
                if (eh.type != ERRORTYPEGEOM) continue;
                if (total == index) return eh;
                total++;
            }
            return null;
        }
        */

	    public boolean findGeometries(Geometric geo1, Cell cell1, Geometric geo2, Cell cell2)
        {
            boolean eh1found = false;
	        boolean eh2found = false;

	        for(Iterator it = highlights.iterator(); it.hasNext(); )
            {
                ErrorHighlight eh = (ErrorHighlight)it.next();
                if (eh.type != ERRORTYPEGEOM) continue;
		        if (!eh1found && eh.cell == cell1 && eh.geom == geo1)
		            eh1found = true;
		        if (!eh2found && eh.cell == cell2 && eh.geom == geo2)
		            eh2found = true;
		        if (eh1found && eh2found)
		            return (true);
            }
	        return (false);
        }
        /**
         * Method to describe error "elv".
         */
        public String getMessage() { return message; }

        /**
         * Returns true if this error log is still valid
         * (In a linked Cell, and all highlights are still valid)
         * @return
         */
        public boolean isValid() {
            if (!logCell.isLinked()) return false;
            // check validity of highlights
            boolean allValid = true;
            for (Iterator it = highlights.iterator(); it.hasNext(); ) {
                ErrorHighlight erh = (ErrorHighlight)it.next();
                if (!erh.isValid()) { allValid = false; break; }
            }
            return allValid;
        }

        /**
         * Method to return the error message associated with the current error.
         * Highlights associated graphics if "showhigh" is nonzero.  Fills "g1" and "g2"
         * with associated geometry modules (if nonzero).
         */
        public String reportLog(boolean showhigh, Geometric [] gPair)
        {
            // if two highlights are requested, find them
            if (gPair != null)
            {
                Geometric geom1 = null, geom2 = null;
                for(Iterator it = highlights.iterator(); it.hasNext(); )
                {
                    ErrorHighlight eh = (ErrorHighlight)it.next();
                    if (eh.type == ERRORTYPEGEOM)
                    {
                        if (geom1 == null) geom1 = eh.geom; else
                            if (geom2 == null) geom2 = eh.geom;
                    }
                }

                // return geometry if requested
                if (geom1 != null) gPair[0] = geom1;
                if (geom2 != null) gPair[1] = geom2;
            }

            // show the error
            if (showhigh)
            {
                Highlighter highlighter = null;

                // first show the geometry associated with this error
                for(Iterator it = highlights.iterator(); it.hasNext(); )
                {
                    ErrorHighlight eh = (ErrorHighlight)it.next();

                    Cell cell = eh.cell;
                    // validate the cell (it may have been deleted)
                    if (cell != null)
                    {
                        if (!cell.isLinked())
                        {
                            return "(cell deleted): " + message;
                        }

                        // make sure it is shown
                        boolean found = false;
                        EditWindow wnd = null;
                        for(Iterator it2 = WindowFrame.getWindows(); it2.hasNext(); )
                        {
                            WindowFrame wf = (WindowFrame)it2.next();
                            WindowContent content = wf.getContent();
                            if (!(content instanceof EditWindow)) continue;
                            wnd = (EditWindow)content;
                            if (wnd.getCell() == cell)
                            {
                                if (((eh.context != null) && eh.context.equals(wnd.getVarContext())) ||
                                        (eh.context == null)) {
                                    // already displayed.  should force window "wf" to front? yes
                                    wf.getFrame().toFront();
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found)
                        {
                            // make a new window for the cell
                            WindowFrame wf = WindowFrame.createEditWindow(cell);
                            wnd = (EditWindow)wf.getContent();
                            wnd.setCell(eh.cell, eh.context);
                        }
                        if (highlighter == null) {
                            highlighter = wnd.getHighlighter();
                            highlighter.clear();
                        }
                    }

                    if (highlighter == null) continue;

                    switch (eh.type)
                    {
                        case ERRORTYPEGEOM:
                            if (!eh.showgeom) break;
                            highlighter.addElectricObject(eh.geom, cell);
                            break;
                        case ERRORTYPEEXPORT:
    						highlighter.addText(eh.pp, cell, null, null);
//						if (havegeoms == 0) infstr = initinfstr(); else
//							addtoinfstr(infstr, '\n');
//						havegeoms++;
//						formatinfstr(infstr, x_("CELL=%s TEXT=0%lo;0%lo;-"),
//							describenodeproto(eh->pp->parent), (INTBIG)eh->pp->subnodeinst->geom,
//								(INTBIG)eh->pp);
                            break;
                        case ERRORTYPELINE:
                            highlighter.addLine(new Point2D.Double(eh.x1, eh.y1), new Point2D.Double(eh.x2, eh.y2), cell);
                            break;
                        case ERRORTYPETHICKLINE:
                            highlighter.addThickLine(new Point2D.Double(eh.x1, eh.y1), new Point2D.Double(eh.x2, eh.y2), new Point2D.Double(eh.cX, eh.cY), cell);
                            break;
                        case ERRORTYPEPOINT:
                            double consize = 5;
                            highlighter.addLine(new Point2D.Double(eh.x1-consize, eh.y1-consize), new Point2D.Double(eh.x1+consize, eh.y1+consize), cell);
                            highlighter.addLine(new Point2D.Double(eh.x1-consize, eh.y1+consize), new Point2D.Double(eh.x1+consize, eh.y1-consize), cell);
                            break;
                    }
                }

                highlighter.finished();
            }

            // return the error message
            return message;
        }

    }
    public static class WarningLog extends MessageLog
    {
       private WarningLog(String message, int sortKey) {super(message, sortKey);}
    }

    /** Current Logger */               private static ErrorLogger currentLogger;
    /** List of all loggers */          private static List allLoggers = new ArrayList();

	private boolean alreadyExplained;
    private int errorLimit;
    private List allErrors;
	private List allWarnings;
    private int currentLogNumber;
    private boolean limitExceeded;
    private String errorSystem;
    private boolean terminated;
    private boolean persistent; // cannot be deleted

    private ErrorLogger() {}

    /**
     * Create a new ErrorLogger instance, with persistent set false, so
     * it can be deleted from tree
     */
    public static synchronized ErrorLogger newInstance(String system)
    {
        return newInstance(system, false);
    }

    /**
     * Create a new ErrorLogger instance. If persistent is true, it cannot be
     * delete from the explorer tree (although deletes will clear it, thereby removing
     * it until another error is logged to it). This is useful for tools that report
     * errors incrementally, such as the Network tool.
     * @param system the name of the system logging errors
     * @param persistent if true, this error tree cannot be deleted
     * @return a new ErrorLogger for logging errors
     */
    public static ErrorLogger newInstance(String system, boolean persistent)
    {
        ErrorLogger logger = new ErrorLogger();
        logger.allErrors = new ArrayList();
	    logger.allWarnings = new ArrayList();
        logger.limitExceeded = false;
        logger.currentLogNumber = -1;
        logger.errorSystem = system;
        logger.errorLimit = User.getErrorLimit();
        logger.terminated = false;
        logger.persistent = persistent;
        logger.alreadyExplained = false;
        synchronized(allLoggers) {
            if (currentLogger == null) currentLogger = logger;
            allLoggers.add(logger);
        }
        Undo.addDatabaseChangeListener(logger);
        return logger;
    }

    /**
     * Factory method to create an error message and log.
     * with the given text "message" applying to cell "cell".
     * Returns a pointer to the message (0 on error) which can be used to add highlights.
     */
    public synchronized MessageLog logError(String message, Cell cell, int sortKey)
    {
        if (terminated && !persistent) {
            System.out.println("WARNING: "+errorSystem+" already terminated, should not log new error");
        }

        // if too many errors, don't save it
        if (errorLimit > 0 && getNumErrors() >= errorLimit)
        {
            if (!limitExceeded)
            {
                System.out.println("WARNING: more than " + errorLimit + " errors found, ignoring the rest");
                limitExceeded = true;
            }
            return null;
        }

        // create a new ErrorLog object
        MessageLog el = new MessageLog(message, sortKey);

        // store information about the error
        el.message = message;
        el.sortKey = sortKey;
        el.logCell = cell;
        el.highlights = new ArrayList();

        // add the ErrorLog into the global list
        allErrors.add(el);
        currentLogNumber = allErrors.size()-1;

        if (persistent) WindowFrame.wantToRedoErrorTree();
        return el;
    }

    /**
     * Factory method to create a warning message and log.
     * with the given text "message" applying to cell "cell".
     * Returns a pointer to the message which can be used to add highlights.
     */
    public synchronized MessageLog logWarning(String message, Cell cell, int sortKey)
    {
        if (terminated && !persistent) {
            System.out.println("WARNING: "+errorSystem+" already terminated, should not log new warning");
        }

        // if too many errors, don't save it
        if (errorLimit > 0 && getNumWarnings() >= errorLimit)
        {
            if (!limitExceeded)
            {
                System.out.println("WARNING: more than " + errorLimit + " warnings found, ignoring the rest");
                limitExceeded = true;
            }
            return null;
        }

        // create a new ErrorLog object
        MessageLog el = new WarningLog(message, sortKey);

        // store information about the error
        el.message = message;
        el.sortKey = sortKey;
        el.logCell = cell;
        el.highlights = new ArrayList();

        // add the ErrorLog into the global list
        allWarnings.add(el);

        if (persistent) WindowFrame.wantToRedoErrorTree();
        return el;
    }

	/**
	 * Method to determine if existing report was not looged already
	 * as error or warning
	 * @param cell
	 * @param geom1
	 * @param cell2
	 * @param geom2
	 * @return
	 */
	public synchronized boolean findMessage(Cell cell, Geometric geom1, Cell cell2, Geometric geom2)
	{
		for (int i = 0; i < allErrors.size(); i++)
		{
			MessageLog el = (MessageLog)allErrors.get(i);

			if (el.findGeometries(geom1, cell, geom2, cell2))
				return (true);
		}
		for (int i = 0; i < allWarnings.size(); i++)
		{
			MessageLog el = (MessageLog)allWarnings.get(i);

			if (el.findGeometries(geom1, cell, geom2, cell2))
				return (true);
		}
		return (false);
	}

    /** Get the current logger */
    public static ErrorLogger getCurrent() {
        synchronized(allLoggers) {
            if (currentLogger == null) return newInstance("Unknown");
            return currentLogger;
        }
    }

    /**
     * Removes all errors associated with Cell cell.
     * @param cell the cell for which errors will be removed
     */
    public synchronized void clearLogs(Cell cell) {
        ArrayList trimmedLogs = new ArrayList();
        // Errors
        for (Iterator it = allErrors.iterator(); it.hasNext(); ) {
            MessageLog log = (MessageLog)it.next();
            if (log.logCell != cell) trimmedLogs.add(log);
        }
        allErrors = trimmedLogs;
        trimmedLogs.clear();
        // Warnings
        for (Iterator it = allWarnings.iterator(); it.hasNext(); ) {
            MessageLog log = (MessageLog)it.next();
            if (log.logCell != cell) trimmedLogs.add(log);
        }
        allWarnings = trimmedLogs;
        currentLogNumber = getNumLogs()-1;
    }

    /** Delete this logger */
    public synchronized void delete() {

        if (persistent) {
            // just clear errors
            allErrors.clear();
			allWarnings.clear();
            currentLogNumber = -1;
            WindowFrame.wantToRedoErrorTree();
            return;
        }

        synchronized(allLoggers) {
            allLoggers.remove(this);
            if (currentLogger == this) {
                if (allLoggers.size() > 0) currentLogger = (ErrorLogger)allLoggers.get(0);
                else currentLogger = null;
            }
        }
        Undo.removeDatabaseChangeListener(this);

        WindowFrame.wantToRedoErrorTree();
    }

    public String describe() {
        synchronized(allLoggers) {
            if (currentLogger == this) return errorSystem + " [Current]";
        }
        return errorSystem;
    }

    /**
     * Method called when all errors are logged.  Initializes pointers for replay of errors.
     */
    public synchronized void termLogging(boolean explain)
    {
        // enumerate the errors
        int errs = 0;
        for(Iterator it = allErrors.iterator(); it.hasNext(); )
        {
            MessageLog el = (MessageLog)it.next();
            el.index = ++errs;
        }
	    for(Iterator it = allWarnings.iterator(); it.hasNext(); )
        {
            MessageLog el = (MessageLog)it.next();
            el.index = ++errs;
        }

        if (errs == 0) {
            delete();
            return;
        }

//		if (db_errorchangedroutine != 0) (*db_errorchangedroutine)();

        if (errs > 0 && explain)
        {
            //System.out.println(errorSystem+" FOUND "+getNumErrors()+" ERRORS");
            if (!alreadyExplained)
            {
				alreadyExplained = true;
                String extraMsg = "errors/warnings";
                if (getNumErrors() == 0) extraMsg = "warnings";
                else  if (getNumWarnings() == 0) extraMsg = "errors";
	            System.out.println("Type > and < to step through " + extraMsg + ", or open the ERRORS view in the explorer");
            }
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {WindowFrame.wantToRedoErrorTree(); }
        });
        synchronized(allLoggers) {
            currentLogger = this;
        }

        terminated = true;
    }


    /**
     * Method to sort the errors by their "key" (a value provided to "logerror()").
     * Obviously, this should be called after all errors have been reported.
     */
    public synchronized void sortLogs()
    {
        Collections.sort(allErrors, new ErrorLogOrder());
	    Collections.sort(allWarnings, new ErrorLogOrder());
    }

    private static class ErrorLogOrder implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            MessageLog el1 = (MessageLog)o1;
            MessageLog el2 = (MessageLog)o2;
	        int sortedKey = el1.sortKey - el2.sortKey;
	        if (sortedKey == 0) // Identical, compare lexicographically
	            sortedKey = el1.compareTo(el2);
            return sortedKey; //el1.sortKey - el2.sortKey;
        }
    }

    /**
     * Method to advance to the next error and report it.
     */
    public static String reportNextMessage()
    {
        return reportNextMessage(true, null);
    }

    /**
     * Method to advance to the next error and report it.
     */
    private static String reportNextMessage(boolean showhigh, Geometric [] gPair)
    {
        ErrorLogger logger;
        synchronized(allLoggers) {
            if (currentLogger == null) return "No errors to report";
            logger = currentLogger;
        }
        return logger.reportNextMessage_(showhigh, gPair);
    }

    private synchronized String reportNextMessage_(boolean showHigh, Geometric [] gPair) {
        if (currentLogNumber < getNumLogs()-1)
        {
            currentLogNumber++;
        } else
        {
            if (getNumLogs() <= 0) return "No "+errorSystem+" errors";
            currentLogNumber = 0;
        }
        return reportLog(currentLogNumber, showHigh, gPair);
    }

    /**
     * Method to back up to the previous error and report it.
     */
    public static String reportPrevMessage()
    {
        ErrorLogger logger;
        synchronized(allLoggers) {
            if (currentLogger == null) return "No errors to report";
            logger = currentLogger;
        }
        return logger.reportPrevMessage_();
    }

    private synchronized String reportPrevMessage_() {
        if (currentLogNumber > 0)
        {
            currentLogNumber--;
        } else
        {
            if (getNumLogs() <= 0) return "No "+errorSystem+" errors";
            currentLogNumber = getNumLogs() - 1;
        }
        return reportLog(currentLogNumber, true, null);
    }

    /**
     * Report an error
     * @param logNumber
     * @param showHigh
     * @param gPair
     * @return
     */
    private synchronized String reportLog(int logNumber, boolean showHigh, Geometric [] gPair) {

        if (logNumber < 0 || (logNumber >= getNumLogs())) {
            return errorSystem + ": no such error or warning "+(logNumber+1)+", only "+getNumLogs()+" errors.";
        }

        MessageLog el = null;
        String extraMsg = null;
        if (logNumber < getNumErrors())
        {
            el = (MessageLog)allErrors.get(logNumber);
            extraMsg = " error " + (logNumber+1) + " of " + allErrors.size();
        }
        else
        {
            el = (MessageLog)allWarnings.get(logNumber);
            extraMsg = " warning " + (logNumber+1-allErrors.size()) + " of " + allWarnings.size();
        }
        String message = el.reportLog(showHigh, gPair);
        return (errorSystem + extraMsg + ": " + message);
    }

    /**
     * Method to tell the number of logged errors.
     * @return the number of "ErrorLog" objects logged.
     */
    public synchronized int getNumErrors() { return allErrors.size(); }

    /**
     * Method to tell the number of logged errors.
     * @return the number of "ErrorLog" objects logged.
     */
    public synchronized int getNumWarnings() { return allWarnings.size(); }

    /**
     * Method to tell the number of logged errors.
     * @return the number of "ErrorLog" objects logged.
     */
    public synchronized int getNumLogs() { return getNumWarnings() + getNumErrors(); }

    /**
     * Method to list all logged errors and warnings.
     * @return an Iterator over all of the "ErrorLog" objects.
     */
    private synchronized Iterator getLogs() {
        List copy = new ArrayList();
        for (Iterator it = allErrors.iterator(); it.hasNext(); ) {
            copy.add(it.next());
        }
	    for (Iterator it = allWarnings.iterator(); it.hasNext(); ) {
            copy.add(it.next());
        }
        return copy.iterator();
    }

    private synchronized void deleteLog(MessageLog error) {
        boolean found = allErrors.remove(error);

        found = (!found) ? allWarnings.remove(error) : found;
        if (!found) {
            System.out.println(errorSystem+ ": Does not contain error/warning to delete");
        }
        if (currentLogNumber >= getNumLogs()) currentLogNumber = 0;
    }

    // ----------------------------- Explorer Tree Stuff ---------------------------

    /**
     * A static object is used so that its open/closed tree state can be maintained.
     */
    private static String errorNode = "ERRORS";

    public static DefaultMutableTreeNode getExplorerTree()
    {
        DefaultMutableTreeNode explorerTree = new DefaultMutableTreeNode(errorNode);
        ArrayList loggersCopy = new ArrayList();
        synchronized(allLoggers) {
            loggersCopy.addAll(allLoggers);
        }
        for (Iterator eit = loggersCopy.iterator(); eit.hasNext(); ) {
            ErrorLogger logger = (ErrorLogger)eit.next();
            if (logger.getNumErrors() == 0 && logger.getNumWarnings() == 0) continue;
            DefaultMutableTreeNode loggerNode = new DefaultMutableTreeNode(logger);
            for (Iterator it = logger.getLogs(); it.hasNext();)
            {
                MessageLog el = (MessageLog)it.next();
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(el);
                loggerNode.add(node);
            }
            explorerTree.add(loggerNode);
        }
        return explorerTree;
    }

    public JPopupMenu getPopupMenu() {
        JPopupMenu p = new JPopupMenu();
        JMenuItem m;
        m = new JMenuItem("Delete"); m.addActionListener(this); p.add(m);
        m = new JMenuItem("Set Current"); m.addActionListener(this); p.add(m);
        return p;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JMenuItem) {
            JMenuItem m = (JMenuItem)e.getSource();
            if (m.getText().equals("Delete")) delete();
            if (m.getText().equals("Set Current")) {
                synchronized(allLoggers) { currentLogger = this; }
                WindowFrame.wantToRedoErrorTree();
            }
        }
    }

    public void databaseEndChangeBatch(Undo.ChangeBatch batch) {
        // check if any errors need to be deleted
        boolean changed = false;
        for (Iterator it = getLogs(); it.hasNext(); ) {
            MessageLog err = (MessageLog)it.next();
            if (!err.isValid()) {
                deleteLog(err);
                changed = true;
            }
        }
        if (changed)
            WindowFrame.wantToRedoErrorTree();
    }

    public void databaseChanged(Undo.Change evt) {}

    public boolean isGUIListener() {
        return true;
    }


}
