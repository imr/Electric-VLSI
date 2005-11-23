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

import com.sun.electric.database.change.DatabaseChangeEvent;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.Job;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

/**
 * Class for logging errors.
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
public class ErrorLogger implements ActionListener, DatabaseChangeListener
{
    private enum ErrorLoggerType
    {
        ERRORTYPEGEOM, ERRORTYPEEXPORT , ERRORTYPELINE, ERRORTYPETHICKLINE, ERRORTYPEPOINT
    };

    private static class ErrorHighlight
    {
        ErrorLoggerType         type;
        Geometric   geom;
        Export      pp;
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
            if (geom instanceof NodeInst) msg = "Node " + geom.describe(true); else
                msg = "Arc " + geom.describe(true);
            msg += " in " + context.getInstPath(".");
            return msg;
        }

        public boolean isValid() {
            if (type == ErrorLoggerType.ERRORTYPEEXPORT) return pp.isLinked();
            if (type == ErrorLoggerType.ERRORTYPEGEOM) return geom.isLinked();
	        //return true;
	        return (cell.isLinked()); // Still have problems with minAre DRC errors
        }
    };

    /**
     * Create a Log of a single message.
     */
    public static class MessageLog implements Comparable<MessageLog>, Serializable {
        private String message;
        private int    sortKey;
        protected int    index;
        protected Cell    logCell;                // cell associated with log (not really used)
        protected List<ErrorHighlight>   highlights;

        private MessageLog(String message, Cell cell, int sortKey) {
            this.message = message;
            this.sortKey = sortKey;
            this.logCell = cell;
            index = 0;
            highlights = new ArrayList<ErrorHighlight>();
        }

	    /**
		 * Compare objects lexicographically based on string comparator CASE_INSENSITIVE_ORDER
		 * This method doesn't guarantee (compare(x, y)==0) == (x.equals(y))
		 * @param log1
		 * @return Returns a negative integer, zero, or a positive integer as the
		 * first message has smaller than, equal to, or greater than the second lexicographically
		 */
/*5*/    public int compareTo(MessageLog log1)
//4*/    public int compareTo(Object o1)
	    {
//4*/	    MessageLog log1 = (MessageLog)o1;
		    return (String.CASE_INSENSITIVE_ORDER.compare(message, log1.message));
	    }

        /**
         * Method to add "geom" to the error in "errorlist".  Also adds a
         * hierarchical traversal path "path" (which is "pathlen" long).
         */
        public void addGeom(Geometric geom, boolean showit, Cell cell, VarContext context)
        {
            ErrorHighlight eh = new ErrorHighlight();
            eh.type = ErrorLoggerType.ERRORTYPEGEOM;
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
            eh.type = ErrorLoggerType.ERRORTYPEEXPORT;
            eh.pp = pp;
            eh.showgeom = showit;
            eh.cell = cell;
            eh.context = context;
            highlights.add(eh);
        }

        /**
         * Method to add line (x1,y1)=>(x2,y2) to the error in "errorlist".
         */
        public ErrorHighlight addLine(double x1, double y1, double x2, double y2, Cell cell)
        {
            ErrorHighlight eh = new ErrorHighlight();
            eh.type = ErrorLoggerType.ERRORTYPELINE;
            eh.x1 = x1;
            eh.y1 = y1;
            eh.x2 = x2;
            eh.y2 = y2;
            eh.cell = cell;
            eh.context = null;
            highlights.add(eh);
            return eh;
        }

        /**
         * Method to add polygon "poly" to the error in "errorlist".
         */
        public void addPoly(PolyBase poly, boolean thick, Cell cell)
        {
            Point2D [] points = poly.getPoints();
            Point2D center = new Point2D.Double(poly.getCenterX(), poly.getCenterY());
            for(int i=0; i<points.length; i++)
            {
                int prev = i-1;
                if (i == 0) prev = points.length-1;
                ErrorHighlight eh = new ErrorHighlight();
                if (thick) eh.type = ErrorLoggerType.ERRORTYPETHICKLINE; else
                    eh.type = ErrorLoggerType.ERRORTYPELINE;
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
            eh.type = ErrorLoggerType.ERRORTYPEPOINT;
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
            for(Iterator<ErrorHighlight> it = highlights.iterator(); it.hasNext(); )
            {
                ErrorHighlight eh = (ErrorHighlight)it.next();
                if (eh.type == ErrorLoggerType.ERRORTYPEGEOM) total++;
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
            for(Iterator<ErrorHighlight> it = highlights.iterator(); it.hasNext(); )
            {
                ErrorHighlight eh = (ErrorHighlight)it.next();
                if (eh.type != ErrorLoggerType.ERRORTYPEGEOM) continue;
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

	        for(Iterator<ErrorHighlight> it = highlights.iterator(); it.hasNext(); )
            {
                ErrorHighlight eh = (ErrorHighlight)it.next();
                if (eh.type != ErrorLoggerType.ERRORTYPEGEOM) continue;
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
        public String getMessage()
        {
            return "["+index+"] "+message;
        }

        protected void xmlDescription(PrintStream msg)
        {
            String className = this.getClass().getSimpleName();
            msg.append("\t<" + className + " message=\"" + message + "\" "
                    + "cellName=\"" + logCell.describe(false) + "\">\n");
            for(Iterator<ErrorHighlight> it = highlights.iterator(); it.hasNext(); )
            {
                ErrorHighlight eh = it.next();

                switch (eh.type)
                {
                    case ERRORTYPEGEOM:
                        if (eh.showgeom)
                        {
                            msg.append("\t\t<"+eh.type+" ");
                            if (eh.geom instanceof NodeInst)
                                msg.append("geomName=\"" + ((NodeInst)eh.geom).getD().name + "\" ");
                            else
                                msg.append("geomName=\"" + ((ArcInst)eh.geom).getD().name + "\" ");
                            msg.append("cellName=\"" + eh.cell.describe(false) + "\"");
                            msg.append(" />\n");
                        }
                        break;
                    case ERRORTYPELINE:
                    case ERRORTYPETHICKLINE:
                        msg.append("\t\t<"+eh.type+" ");
                        msg.append("p1=\"(" + eh.x1 + "," + eh.y1 + ")\" ");
                        msg.append("p2=\"(" + eh.x2 + "," + eh.y2 + ")\" ");
                        msg.append("center=\"(" + eh.cX + "," + eh.cY + ")\" ");
                        msg.append("cellName=\"" + eh.cell.describe(false) + "\"");
                        msg.append(" />\n");
                        break;
                    default:
                        System.out.println("Not implemented in xmlDescription");
                }
            }
            msg.append("\t</" + className + ">\n");
        }

        /**
         * Returns true if this error log is still valid
         * (In a linked Cell, and all highlights are still valid)
         */
        public boolean isValid() {
        	if (logCell == null) return true;
            if (!logCell.isLinked()) return false;
            // check validity of highlights
            boolean allValid = true;
            for (Iterator<ErrorHighlight> it = highlights.iterator(); it.hasNext(); ) {
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
                for(Iterator<ErrorHighlight> it = highlights.iterator(); it.hasNext(); )
                {
                    ErrorHighlight eh = (ErrorHighlight)it.next();
                    if (eh.type == ErrorLoggerType.ERRORTYPEGEOM)
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
                EditWindow wnd = null;

                // first show the geometry associated with this error
                for(Iterator<ErrorHighlight> it = highlights.iterator(); it.hasNext(); )
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
                        for(Iterator<WindowFrame> it2 = WindowFrame.getWindows(); it2.hasNext(); )
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

                if (highlighter != null)
				{
					highlighter.ensureHighlightingSeen();
					highlighter.finished();

					// make sure the selection is visible
					Rectangle2D hBounds = highlighter.getHighlightedArea(wnd);
					Rectangle2D shown = wnd.getDisplayedBounds();
					if (!shown.intersects(hBounds))
					{
				        wnd.focusOnHighlighted();
					}
				}
            }

            // return the error message
            return message;
        }

    }

	/**
     * Create a Log of a single warning.
     */
    public static class WarningLog extends MessageLog
    {
       private WarningLog(String message, Cell cell, int sortKey) {super(message, cell, sortKey);}
    }

    /** Current Logger */               private static ErrorLogger currentLogger;
    /** List of all loggers */          private static List<ErrorLogger> allLoggers = new ArrayList<ErrorLogger>();

	private boolean alreadyExplained;
    private int errorLimit;
    private List<MessageLog> allErrors;
	private List<WarningLog> allWarnings;
    private int currentLogNumber;
    private boolean limitExceeded;
    private String errorSystem;
    private boolean terminated;
    private boolean persistent; // cannot be deleted
    private HashMap<Integer,String> sortKeysToGroupNames; // association of sortKeys to GroupNames

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
        logger.allErrors = new ArrayList<MessageLog>();
	    logger.allWarnings = new ArrayList<WarningLog>();
        logger.limitExceeded = false;
        logger.currentLogNumber = -1;
        logger.errorSystem = system;
        logger.errorLimit = User.getErrorLimit();
        logger.terminated = false;
        logger.persistent = persistent;
        logger.alreadyExplained = false;
        logger.sortKeysToGroupNames = null;
        addErrorLogger(logger);
        return logger;
    }

    private static void addErrorLogger(ErrorLogger logger)
    {
        synchronized(allLoggers) {
            if (currentLogger == null) currentLogger = logger;
            allLoggers.add(logger);
        }
        Undo.addDatabaseChangeListener(logger);
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
        MessageLog el = new MessageLog(message, cell, sortKey);

        // store information about the error
        el.highlights = new ArrayList<ErrorHighlight>();

        // add the ErrorLog into the global list
        allErrors.add(el);
//        currentLogNumber = allErrors.size()-1;

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
        WarningLog el = new WarningLog(message, cell, sortKey);

        // store information about the error
        el.highlights = new ArrayList<ErrorHighlight>();

        // add the ErrorLog into the global list
        allWarnings.add(el);

        if (persistent) WindowFrame.wantToRedoErrorTree();
        return el;
    }

	/**
	 * Method to determine if existing report was not looged already
	 * as error or warning
	 */
	public synchronized boolean findMessage(Cell cell, Geometric geom1, Cell cell2, Geometric geom2, boolean searchInError)
	{
        if (searchInError)
        {
            for (int i = 0; i < allErrors.size(); i++)
            {
                MessageLog el = (MessageLog)allErrors.get(i);

                if (el.findGeometries(geom1, cell, geom2, cell2))
                    return (true);
		    }
        }
        else
        {
            for (int i = 0; i < allWarnings.size(); i++)
            {
                MessageLog el = (MessageLog)allWarnings.get(i);

                if (el.findGeometries(geom1, cell, geom2, cell2))
                    return (true);
            }
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
       ArrayList<MessageLog> errLogs = new ArrayList<MessageLog>();
        // Errors
        for (Iterator<MessageLog> it = allErrors.iterator(); it.hasNext(); ) {
            MessageLog log = (MessageLog)it.next();
            if (log.logCell != cell) errLogs.add(log);
        }
        allErrors = errLogs;

	    ArrayList<WarningLog> warndLogs = new ArrayList<WarningLog>();
        // Warnings
        for (Iterator<WarningLog> it = allWarnings.iterator(); it.hasNext(); ) {
            WarningLog log = (WarningLog)it.next();
            if (log.logCell != cell) warndLogs.add(log);
        }
        allWarnings = warndLogs;
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

    public static void load()
    {
        String fileName = OpenFile.chooseInputFile(FileType.XML, "Read ErrorLogger");
        try {
            XMLParser parser = new XMLParser();
            parser.process(TextUtils.makeURLToFile(fileName));
        } catch (Exception e)
		{
			System.out.println("Error loading " + fileName);
			return;
		}
    }

    public void save() {
	    PrintStream buffWriter = null;
	    String filePath = null;

	    try
        {
		    filePath = OpenFile.chooseOutputFile(FileType.XML, null, "ErrorLoggerSave.xml");
            if (filePath == null) return; // cancel operation
		    buffWriter = new PrintStream(new FileOutputStream(filePath));

	    } catch (IOException e)
		{
			System.out.println("Error creating " + filePath);
			return;
		}

        // Creating header
        buffWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        buffWriter.println();
        buffWriter.println("<!DOCTYPE ErrorLogger");
        buffWriter.println(" [");
        buffWriter.println(" <!ELEMENT ErrorLogger (MessageLog|WarningLog)*>");
        buffWriter.println(" <!ELEMENT MessageLog (ERRORTYPEGEOM|ERRORTYPETHICKLINE)* >");
        buffWriter.println(" <!ELEMENT WarningLog ANY >");
        buffWriter.println(" <!ELEMENT ERRORTYPEGEOM ANY>");
        buffWriter.println(" <!ELEMENT ERRORTYPETHICKLINE ANY>");
        buffWriter.println("<!ATTLIST ErrorLogger");
        buffWriter.println("    errorSystem CDATA #REQUIRED");
        buffWriter.println(" >");
        buffWriter.println(" <!ATTLIST MessageLog");
        buffWriter.println("    message CDATA #REQUIRED");
        buffWriter.println("    cellName CDATA #REQUIRED");
        buffWriter.println(" >");
        buffWriter.println(" <!ATTLIST WarningLog");
        buffWriter.println("    message CDATA #REQUIRED");
        buffWriter.println("    cellName CDATA #REQUIRED");
        buffWriter.println(" >");
        buffWriter.println(" <!ATTLIST ERRORTYPEGEOM");
        buffWriter.println("    geomName CDATA #REQUIRED");
        buffWriter.println("    cellName CDATA #REQUIRED");
        buffWriter.println(" >");
        buffWriter.println(" <!ATTLIST ERRORTYPETHICKLINE");
        buffWriter.println("    p1 CDATA #REQUIRED");
        buffWriter.println("    p2 CDATA #REQUIRED");
        buffWriter.println("    center CDATA #REQUIRED");
        buffWriter.println("    cellName CDATA #REQUIRED");
        buffWriter.println(" >");
        buffWriter.println(" ]>");
        buffWriter.println();
        String className = this.getClass().getSimpleName();
        buffWriter.println("<" + className + " errorSystem=\"" + errorSystem + "\">");
	    for (Iterator<MessageLog> it = allErrors.iterator(); it.hasNext(); ) {
            MessageLog log = (MessageLog)it.next();
            log.xmlDescription(buffWriter);
        }
        // Warnings
        for (Iterator<WarningLog> it = allWarnings.iterator(); it.hasNext(); ) {
            WarningLog log = (WarningLog)it.next();
            log.xmlDescription(buffWriter);
        }
        buffWriter.println("</" + className + ">");
    }

    public String describe() {
        synchronized(allLoggers) {
            if (currentLogger == this) return errorSystem + " [Current]";
        }
        return errorSystem;
    }

    /**
     * Set a group name for a sortKey.  Doing so causes all errors with
     * this sort key to be put in a sub-tree of the error tree with
     * the groupName as the title of the sub-tree.
     * @param sortKey the error log sortKey
     * @param groupName the group name
     */
    public void setGroupName(int sortKey, String groupName) {
        if (sortKeysToGroupNames == null) {
            sortKeysToGroupNames = new HashMap<Integer,String>();
        }
        sortKeysToGroupNames.put(new Integer(sortKey), groupName);
    }

    /**
     * Method called when all errors are logged.  Initializes pointers for replay of errors.
     */
    public synchronized void termLogging(boolean explain)
    {
        // enumerate the errors
        int errs = 0;
        for(Iterator<MessageLog> it = allErrors.iterator(); it.hasNext(); )
        {
            MessageLog el = (MessageLog)it.next();
            el.index = ++errs;
        }
	    for(Iterator<WarningLog> it = allWarnings.iterator(); it.hasNext(); )
        {
            WarningLog el = (WarningLog)it.next();
            el.index = ++errs;
        }

        // Set as assigned before removing it
        synchronized(allLoggers) {
            currentLogger = this;
        }

        if (errs == 0) {
            delete();
            return;
        }

//		if (db_errorchangedroutine != 0) (*db_errorchangedroutine)();

        if (errs > 0 && explain)
        {
            if (!alreadyExplained)
            {
				alreadyExplained = true;
                if (Job.BATCHMODE)
                {
                    System.out.println(getInfo());
                }
                else
                {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            // To print consistent message in message window
                            String extraMsg = "errors/warnings";
                            if (getNumErrors() == 0) extraMsg = "warnings";
                            else  if (getNumWarnings() == 0) extraMsg = "errors";
                            String msg = getInfo();
                            System.out.println(msg);
                            if (getNumLogs() > 0)
                            {
                                System.out.println("Type > and < to step through " + extraMsg + ", or open the ERRORS view in the explorer");
                            }
                            if (getNumErrors() > 0 && !Job.BATCHMODE)
                            {
                                JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), msg,
                                    errorSystem + " finished with Errors", JOptionPane.INFORMATION_MESSAGE);
                            }
                        }
                    });
                }
            }
        }

        if (!Job.BATCHMODE)
        {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {WindowFrame.wantToRedoErrorTree(); }
            });
        }
        terminated = true;
    }

    /**
     * Method to retrieve general information about the errorLogger
     * @return
     */
    private String getInfo()
    {
        return (errorSystem + " found "+getNumErrors()+" errors, "+getNumWarnings()+" warnings!");
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

    private static class ErrorLogOrder implements Comparator<MessageLog>
    {
/*5*/   public int compare(MessageLog el1, MessageLog el2)
//4*/   public int compare(Object o1, Object o2)
        {
//4*/       MessageLog el1 = (MessageLog)o1;
//4*/       MessageLog el2 = (MessageLog)o2;
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
            el = (MessageLog)allWarnings.get(logNumber-allErrors.size());
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
    private synchronized Iterator<MessageLog> getLogs() {
        List<MessageLog> copy = new ArrayList<MessageLog>();
        for (Iterator<MessageLog> it = allErrors.iterator(); it.hasNext(); ) {
            copy.add(it.next());
        }
	    for (Iterator<WarningLog> it = allWarnings.iterator(); it.hasNext(); ) {
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

    public static void deleteAllLoggers()
    {
        ArrayList<ErrorLogger> loggersCopy = new ArrayList<ErrorLogger>();
        synchronized(allLoggers) {
            loggersCopy.addAll(allLoggers);
        }
        for (Iterator<ErrorLogger> eit = loggersCopy.iterator(); eit.hasNext(); )
        {
            ErrorLogger log = (ErrorLogger)eit.next();
            log.delete();
        }
    }

    public static DefaultMutableTreeNode getExplorerTree()
    {
        DefaultMutableTreeNode explorerTree = new DefaultMutableTreeNode(errorNode);
        ArrayList<ErrorLogger> loggersCopy = new ArrayList<ErrorLogger>();
        synchronized(allLoggers) {
            loggersCopy.addAll(allLoggers);
        }
        for (Iterator<ErrorLogger> eit = loggersCopy.iterator(); eit.hasNext(); ) {
            ErrorLogger logger = (ErrorLogger)eit.next();
            if (logger.getNumErrors() == 0 && logger.getNumWarnings() == 0) continue;
            DefaultMutableTreeNode loggerNode = new DefaultMutableTreeNode(logger);
            DefaultMutableTreeNode groupNode = loggerNode;
            int currentSortKey = -1;
            for (Iterator<MessageLog> it = logger.getLogs(); it.hasNext();)
            {
                MessageLog el = (MessageLog)it.next();
                // by default, groupNode is entire loggerNode
                // but, groupNode could be sub-node:
                if (logger.sortKeysToGroupNames != null) {
                    if (currentSortKey != el.sortKey) {
                        // create new sub-tree node
                        currentSortKey = el.sortKey;
                        String groupName = (String)logger.sortKeysToGroupNames.get(new Integer(el.sortKey));
                        if (groupName != null) {
                            groupNode = new DefaultMutableTreeNode(groupName);
                            loggerNode.add(groupNode);
                        } else {
                            // not found, put in loggerNode
                            groupNode = loggerNode;
                        }
                    }
                }
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(el);
                groupNode.add(node);
            }
            explorerTree.add(loggerNode);
        }
        return explorerTree;
    }

    public JPopupMenu getPopupMenu() {
        JPopupMenu p = new JPopupMenu();
        JMenuItem m;
        m = new JMenuItem("Delete"); m.addActionListener(this); p.add(m);
        m = new JMenuItem("Get Info"); m.addActionListener(this); p.add(m);
	    m = new JMenuItem("Save"); m.addActionListener(this); p.add(m);
        m = new JMenuItem("Set Current"); m.addActionListener(this); p.add(m);
        return p;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JMenuItem) {
            JMenuItem m = (JMenuItem)e.getSource();
            if (m.getText().equals("Delete")) delete();
            else if (m.getText().equals("Save")) save();
            else if (m.getText().equals("Get Info")) {
                System.out.println("ErrorLogger Information: " +  getInfo());
            }
            else if (m.getText().equals("Set Current")) {
                synchronized(allLoggers) { currentLogger = this; }
                WindowFrame.wantToRedoErrorTree();
            }
        }
    }

    public void databaseChanged(DatabaseChangeEvent e) {
        // check if any errors need to be deleted
        boolean changed = false;
        for (Iterator<MessageLog> it = getLogs(); it.hasNext(); ) {
            MessageLog err = (MessageLog)it.next();
            if (!err.isValid()) {
                deleteLog(err);
                changed = true;
            }
        }
        if (changed)
            WindowFrame.wantToRedoErrorTree();
    }

//     public void databaseEndChangeBatch(Undo.ChangeBatch batch) {
//         // check if any errors need to be deleted
//         boolean changed = false;
//         for (Iterator<MessageLog> it = getLogs(); it.hasNext(); ) {
//             MessageLog err = (MessageLog)it.next();
//             if (!err.isValid()) {
//                 deleteLog(err);
//                 changed = true;
//             }
//         }
//         if (changed)
//             WindowFrame.wantToRedoErrorTree();
//     }

//     public void databaseChanged(Undo.Change evt) {}

//     public boolean isGUIListener() {
//         return true;
//     }

    private static class XMLParser
    {
        public void process(URL fileURL)
        {
            try
            {
                // Factory call
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setValidating(true);
                // create the parser
                SAXParser parser = factory.newSAXParser();
                URLConnection urlCon = fileURL.openConnection();
                InputStream inputStream = urlCon.getInputStream();
                System.out.println("Parsing XML file \"" + fileURL + "\"");
                parser.parse(inputStream, new XMLHandler());

                System.out.println("End Parsing XML file ...");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        private class XMLHandler extends DefaultHandler
        {
            private ErrorLogger logger = null;
            private MessageLog currentLog = null;

            XMLHandler()
            {
            }

            public InputSource resolveEntity (String publicId, String systemId) throws IOException, SAXException
            {
                System.out.println("It shouldn't reach this point!");
                return null;
            }

            /**
             * Method to finish the logger including counting of elements.
             * @throws SAXException
             */
            public void endDocument () throws SAXException
            {
                logger.termLogging(true);
            }

            public void startElement (String uri, String localName, String qName, Attributes attributes)
            {
                boolean loggerBody = qName.equals("ErrorLogger");
                boolean errorLogBody = qName.equals("MessageLog");
                boolean warnLogBody = qName.equals("WarningLog");
                boolean geoTypeBody = qName.equals("ERRORTYPEGEOM");
                boolean thickTypeBody = qName.equals("ERRORTYPETHICKLINE");

                if (!loggerBody && !errorLogBody && !warnLogBody && !geoTypeBody && !thickTypeBody) return;

                String message = "", cellName = null, geomName = null, viewName = null;
                Point2D p1 = null, p2 = null, center = null;

                for (int i = 0; i < attributes.getLength(); i++)
                {
                    if (attributes.getQName(i).equals("errorSystem"))
                    {
                        // Ignore the rest of the attribute and generate the logger
                        logger = ErrorLogger.newInstance(attributes.getValue(i), true);
                        return;
                    }
                    else if (attributes.getQName(i).startsWith("message"))
                        message = attributes.getValue(i);
                    else if (attributes.getQName(i).startsWith("cell"))
                    {
                        String[] names = TextUtils.parseLine(attributes.getValue(i), "{}");
                        cellName = names[0];
                        viewName = names[1];
                    }
                    else if (attributes.getQName(i).startsWith("geom"))
                        geomName = attributes.getValue(i);
                    else if (attributes.getQName(i).startsWith("p1"))
                    {
                        String[] points = TextUtils.parseLine(attributes.getValue(i), "(,)");
                        double x = Double.parseDouble(points[0]);
                        double y = Double.parseDouble(points[1]);
                        p1 = new Point2D.Double(x, y);
                    }
                    else if (attributes.getQName(i).startsWith("p2"))
                    {
                        String[] points = TextUtils.parseLine(attributes.getValue(i), "(,)");
                        double x = Double.parseDouble(points[0]);
                        double y = Double.parseDouble(points[1]);
                        p2 = new Point2D.Double(x, y);
                    }
                    else if (attributes.getQName(i).startsWith("center"))
                    {
                        String[] points = TextUtils.parseLine(attributes.getValue(i), "(,)");
                        double x = Double.parseDouble(points[0]);
                        double y = Double.parseDouble(points[1]);
                        center = new Point2D.Double(x, y);
                    }
                    else
                        new Error("Invalid attribute in XMLParser");
                }
                View view = View.findView(viewName);
                Cell cell = Library.findCellInLibraries(cellName, view);
                if (errorLogBody)
                {
                    int sortLayer = cell.hashCode();
                    currentLog = logger.logError(message, cell, sortLayer);
                }
                else if (warnLogBody)
                {
                    int sortLayer = cell.hashCode();
                    currentLog = logger.logWarning(message, cell, sortLayer);
                }
                else if (geoTypeBody)
                {
                    Geometric geom = cell.findNode(geomName);
                    if (geom == null) // try arc instead
                        geom = cell.findArc(geomName);
                    currentLog.addGeom(geom, true, cell, null);
                }
                else if (thickTypeBody)
                {
                    ErrorHighlight eh = currentLog.addLine(p1.getX(), p1.getY(), p2.getX(), p2.getY(), cell);
                    eh.type = ErrorLoggerType.valueOf(qName);
                    eh.cX = center.getX();
                    eh.cY = center.getY();
                }
                else
                    new Error("Invalid attribute in XMLParser");
            }

            public void fatalError(SAXParseException e)
            {
                System.out.println("Parser Fatal Error");
                e.printStackTrace();
            }

            public void warning(SAXParseException e)
            {
                System.out.println("Parser Warning");
                e.printStackTrace();
            }

            public void error(SAXParseException e)
            {
                System.out.println("Parser Error");
                e.printStackTrace();
            }
        }
    }
}
