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

import com.sun.electric.database.CellId;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.Job;

import java.awt.geom.Point2D;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Class for logging errors.
 * Holds a log of errors:
 * <p>ErrorLogger errorLogger = ErrorLogger.newInstance(String s): get new logger for s
 * <p>MessageLog errorLog = errorLogger.logError(string msg, cell c, int k):
 * Create a new log with message 'msg', for cell 'c', with sortKey 'k'.
 * <p>Various methods for adding highlights to errorLog:
 * <p>To end logging, call errorLogger.termLogging(boolean explain).
 */
public class ErrorLogger implements Serializable
{
    /**
     * Create a Log of a single message.
     */
    public static class MessageLog implements Comparable<MessageLog>, Serializable {
        private final String message;
        final CellId logCellId;                // cell associated with log (not really used)
        private final int    sortKey;
        private final ErrorHighlight[] highlights;
        protected int    index;

        public MessageLog(String message, Cell cell, int sortKey, List<ErrorHighlight> highlights) {
            this.message = message;
            this.logCellId = cell != null ? (CellId)cell.getId() : null;
            this.sortKey = sortKey;
            this.highlights = highlights.toArray(ErrorHighlight.NULL_ARRAY);
            index = 0;
        }

        public String getMessageString() { return message; }

        public Iterator<ErrorHighlight> getHighlights()
        {
        	return ArrayIterator.iterator(highlights);
        }

        public int getSortKey() { return sortKey; }

        /**
         * Compare objects lexicographically based on string comparator CASE_INSENSITIVE_ORDER
         * This method doesn't guarantee (compare(x, y)==0) == (x.equals(y))
         * @param log1
         * @return Returns a negative integer, zero, or a positive integer as the
         * first message has smaller than, equal to, or greater than the second lexicographically
         */
        public int compareTo(MessageLog log1)
        {
            return (String.CASE_INSENSITIVE_ORDER.compare(message, log1.message));
        }

//        /**
//         * Method to add "geom" to the error in "errorlist".  Also adds a
//         * hierarchical traversal path "path" (which is "pathlen" long).
//         */
//        private void addGeom(Geometric geom, boolean showit, Cell cell, VarContext context)
//        {
//            highlights.add(new ErrorHighGeom(cell, context, geom, showit));
//        }
//
//        /**
//         * Method to add line (x1,y1)=>(x2,y2) to the error in "errorlist".
//         */
//        private void addLine(EPoint pt1, EPoint pt2, Cell cell, boolean thick)
//        {
//            highlights.add(new ErrorHighLine(cell, pt1, pt2, thick));
//        }

        public boolean findGeometries(Geometric geo1, Cell cell1, Geometric geo2, Cell cell2)
        {
            boolean eh1found = false;
            boolean eh2found = false;

            for(ErrorHighlight eh : highlights)
            {
                if (eh.containsObject(cell1, geo1))
                    eh1found = true;
                if (eh.containsObject(cell2, geo2))
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
            if (logCellId == null) return;
            Cell logCell = EDatabase.clientDatabase().getCell(logCellId);
            if (logCell == null) return;
            // replace those characters that XML defines as special such as ">" and "&"
            String m = message.replaceAll(">", "&gt;");
            m = m.replaceAll("<", "&lt;");
            msg.append("\t<" + className + " message=\"" + m + "\" "
                    + "cellName=\"" + logCell.describe(false) + "\">\n");
            for(ErrorHighlight eh : highlights)
            {
                eh.xmlDescription(msg, EDatabase.clientDatabase());
            }
            msg.append("\t</" + className + ">\n");
        }

        /**
         * Returns true if this error log is still valid
         * (In a linked Cell, and all highlights are still valid)
         */
        public boolean isValid(EDatabase database) {
            if (logCellId == null) return true;
            if (database.getCell(logCellId) == null) return false;
            // check validity of highlights
            boolean allValid = true;
            for (ErrorHighlight erh : highlights) {
                if (!erh.isValid(database)) { allValid = false; break; }
            }
            return allValid;
        }

    }

    private static class ErrorLogOrder implements Comparator<MessageLog>
    {
    	public int compare(MessageLog el1, MessageLog el2)
        {
            int sortedKey = el1.sortKey - el2.sortKey;
            if (sortedKey == 0) // Identical, compare lexicographically
                sortedKey = el1.compareTo(el2);
            return sortedKey; //el1.sortKey - el2.sortKey;
        }
    }

    /**
     * Create a Log of a single warning.
     */
    public static class WarningLog extends MessageLog
    {
       public WarningLog(String message, Cell cell, int sortKey, List<ErrorHighlight> highlights) { super(message, cell, sortKey, highlights); }
    }

//	private boolean alreadyExplained;
    private int errorLimit;
    private List<MessageLog> allErrors;
	private List<WarningLog> allWarnings;
    private boolean limitExceeded;
    private String errorSystem;
    private boolean terminated;
    private boolean persistent; // cannot be deleted
    private HashMap<Integer,String> sortKeysToGroupNames; // association of sortKeys to GroupNames

    public HashMap<Integer,String> getSortKeyToGroupNames() { return sortKeysToGroupNames; }

    public String getSystem() { return errorSystem; }

    public ErrorLogger() {}

    /**
     * Create a new ErrorLogger instance.
     * @return a new ErrorLogger for logging errors
     */
    public static ErrorLogger newInstance(String system) {
        return newInstance(system, false);
    }
    
    /**
     * Create a new ErrorLogger instance.
     * @return a new ErrorLogger for logging errors
     */
    public static ErrorLogger newInstance(String system, boolean persistent)
    {
        ErrorLogger logger = new ErrorLogger();
        logger.allErrors = new ArrayList<MessageLog>();
	    logger.allWarnings = new ArrayList<WarningLog>();
        logger.limitExceeded = false;
        logger.errorSystem = system;
        logger.errorLimit = User.getErrorLimit();
        logger.terminated = false;
        logger.persistent = persistent;
//        logger.alreadyExplained = false;
        logger.sortKeysToGroupNames = null;
        return logger;
    }

    public void addMessages(List<MessageLog> messages) {
        for (MessageLog m: messages) {
            if (m instanceof WarningLog)
                allWarnings.add((WarningLog)m);
            else
                allErrors.add(m);
        }
//        if (persistent) Job.getUserInterface().wantToRedoErrorTree();
    }
    
    /**
     * Factory method to create an error message and log.
     * with the given text "message" applying to cell "cell".
     * Returns a pointer to the message (0 on error) which can be used to add highlights.
     */
    private synchronized MessageLog logAnError(String message, Cell cell, int sortKey, List<ErrorHighlight> highlights)
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
        MessageLog el = new MessageLog(message, cell, sortKey, highlights);

        // add the ErrorLog into the global list
        allErrors.add(el);
//        currentLogNumber = allErrors.size()-1;

//        if (persistent) Job.getUserInterface().wantToRedoErrorTree();
        return el;
    }

    /**
     * Factory method to log an error message.
     * @param message the string to display.
     * @param sortKey the sorting order of this message.
     */
    public synchronized void logError(String message, int sortKey)
    {
    	List<ErrorHighlight> h = new ArrayList<ErrorHighlight>();
    	logAnError(message, null, sortKey, h);
    }

    /**
     * Factory method to log an error message.
     * @param message the string to display.
     * @param cell the cell in which this message applies.
     * @param sortKey the sorting order of this message.
     */
    public synchronized void logError(String message, Cell cell, int sortKey)
    {
    	List<ErrorHighlight> h = new ArrayList<ErrorHighlight>();
    	logAnError(message, cell, sortKey, h);
    }

    /**
     * Factory method to log an error message.
     * @param message the string to display.
     * @param geom the node or arc to display
     * @param cell the cell in which this message applies.
     * @param context the VarContext of the Cell.
     * @param sortKey the sorting order of this message.
     */
    public synchronized void logError(String message, Geometric geom, Cell cell, VarContext context, int sortKey)
    {
    	List<ErrorHighlight> h = new ArrayList<ErrorHighlight>();
        h.add(ErrorHighlight.newInstance(context, geom));
    	logAnError(message, cell, sortKey, h);
    }

    /**
     * Factory method to log an error message.
     * @param message the string to display.
     * @param pp the Export to display
     * @param sortKey the sorting order of this message.
     */
    public synchronized void logError(String message, Export pp, int sortKey)
    {
        Cell cell = (Cell)pp.getParent();
    	List<ErrorHighlight> h = new ArrayList<ErrorHighlight>();
        h.add(new ErrorHighExport(null, pp));
    	logAnError(message, cell, sortKey, h);
    }

    /**
     * Factory method to log an error message.
     * @param message the string to display.
     * @param pt the point to display
     * @param cell the cell in which this message applies.
     * @param sortKey the sorting order of this message.
     */
    public synchronized void logError(String message, EPoint pt, Cell cell, int sortKey)
    {
    	List<ErrorHighlight> h = new ArrayList<ErrorHighlight>();
        h.add(new ErrorHighPoint(cell, pt));
    	logAnError(message, cell, sortKey, h);
    }

    /**
     * Factory method to log an error message.
     * @param message the string to display.
     * @param poly the polygon to display
     * @param cell the cell in which this message applies.
     * @param sortKey the sorting order of this message.
     */
    public synchronized void logError(String message, PolyBase poly, Cell cell, int sortKey)
    {
    	List<ErrorHighlight> h = new ArrayList<ErrorHighlight>();
        Point2D [] points = poly.getPoints();
        for(int i=0; i<points.length; i++)
        {
            int prev = i-1;
            if (i == 0) prev = points.length-1;
            h.add(ErrorHighlight.newInstance(cell, points[prev], points[i]));
        }
    	logAnError(message, cell, sortKey, h);
    }

    /**
     * Factory method to log an error message.
     * @param message the string to display.
     * @param geomList a list of nodes or arcs to display (may be null).
     * @param exportList a list of Exports to display (may be null).
     * @param cell the cell in which this message applies.
     * @param sortKey the sorting order of this message.
     */
    public synchronized void logError(String message, List<Geometric> geomList, List<Export> exportList,
    	Cell cell, int sortKey)
    {
    	List<ErrorHighlight> h = new ArrayList<ErrorHighlight>();
    	if (geomList != null)
    	{
    		for(Geometric geom : geomList)
    	        h.add(ErrorHighlight.newInstance(null, geom));
    	}
    	if (exportList != null)
    	{
    		for(Export e : exportList)
                h.add(new ErrorHighExport(null, e));
    	}
    	logAnError(message, cell, sortKey, h);
    }

    /**
     * Factory method to log an error message.
     * @param message the string to display.
     * @param geomList a list of nodes or arcs to display (may be null).
     * @param exportList a list of Exports to display (may be null).
     * @param lineList a list of lines (pairs of points) to display (may be null).
     * @param pointList a list of points to display (may be null).
     * @param polyList a list of polygons to display (may be null).
     * @param cell the cell in which this message applies.
     * @param sortKey the sorting order of this message.
     */
    public synchronized void logError(String message, List<Geometric> geomList, List<Export> exportList, List<EPoint> lineList, List<EPoint> pointList,
    	List<PolyBase> polyList, Cell cell, int sortKey)
    {
    	List<ErrorHighlight> h = new ArrayList<ErrorHighlight>();
    	if (geomList != null)
    	{
    		for(Geometric geom : geomList)
                h.add(ErrorHighlight.newInstance(null, geom));
    	}
    	if (exportList != null)
    	{
    		for(Export e : exportList)
                h.add(new ErrorHighExport(null, e));
    	}
    	if (lineList != null)
    	{
    		for(int i=0; i<lineList.size(); i += 2)
                h.add(new ErrorHighLine(cell, lineList.get(i), lineList.get(i+1), false));
    	}
    	if (pointList != null)
    	{
    		for(EPoint pt : pointList)
                h.add(new ErrorHighPoint(cell, pt));
    	}
    	if (polyList != null)
    	{
    		for(PolyBase poly : polyList)
    		{
    	        Point2D [] points = poly.getPoints();
    	        for(int i=0; i<points.length; i++)
    	        {
    	            int prev = i-1;
    	            if (i == 0) prev = points.length-1;
    	            h.add(new ErrorHighLine(cell, new EPoint(points[prev].getX(), points[prev].getY()),
    	            	new EPoint(points[i].getX(), points[i].getY()), true));
    	        }
    		}
    	}
    	logAnError(message, cell, sortKey, h);
    }

    /**
     * Factory method to log a warning message.
     * @param message the string to display.
     * @param cell the cell in which this message applies.
     * @param sortKey the sorting order of this message.
     */
    private synchronized MessageLog logAWarning(String message, Cell cell, int sortKey, List<ErrorHighlight> highlights)
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
        WarningLog el = new WarningLog(message, cell, sortKey, highlights);

//        // store information about the error
//        el.highlights = new ArrayList<ErrorHighlight>();

        // add the ErrorLog into the global list
        allWarnings.add(el);

//        if (persistent) Job.getUserInterface().wantToRedoErrorTree();
        return el;
    }

    /**
     * Factory method to log a warning message.
     * @param message the string to display.
     * @param cell the cell in which this message applies.
     * @param sortKey the sorting order of this message.
     */
    public synchronized void logWarning(String message, Cell cell, int sortKey)
    {
    	List<ErrorHighlight> h = new ArrayList<ErrorHighlight>();
    	logAWarning(message, cell, sortKey, h);
    }

    /**
     * Factory method to log a warning message.
     * @param message the string to display.
     * @param geom a node or arc to display.
     * @param cell the cell in which this message applies.
     * @param context the VarContext of the Cell.
     * @param sortKey the sorting order of this message.
     */
    public synchronized void logWarning(String message, Geometric geom, Cell cell, VarContext context, int sortKey)
    {
    	List<ErrorHighlight> h = new ArrayList<ErrorHighlight>();
        h.add(ErrorHighlight.newInstance(context, geom));
    	logAWarning(message, cell, sortKey, h);
    }

    /**
     * Factory method to log a warning message.
     * @param message the string to display.
     * @param pp an Exports to display.
     * @param cell the cell in which this message applies.
     * @param context the VarContext of the Cell.
     * @param sortKey the sorting order of this message.
     */
    public synchronized void logWarning(String message, Export pp, Cell cell, VarContext context, int sortKey)
    {
    	List<ErrorHighlight> h = new ArrayList<ErrorHighlight>();
        h.add(new ErrorHighExport(context, pp));
    	logAWarning(message, cell, sortKey, h);
    }

    /**
     * Factory method to log a warning message.
     * @param message the string to display.
     * @param geomList a list of nodes or arcs to display (may be null).
     * @param exportList a list of Exports to display (may be null).
     * @param lineList a list of lines (pairs of points) to display (may be null).
     * @param pointList a list of points to display (may be null).
     * @param polyList a list of polygons to display (may be null).
     * @param cell the cell in which this message applies.
     * @param sortKey the sorting order of this message.
     */
    public synchronized void logWarning(String message, List<Geometric> geomList, List<Export> exportList, List<EPoint> lineList, List<EPoint> pointList,
    	List<PolyBase> polyList, Cell cell, int sortKey)
    {
    	List<ErrorHighlight> h = new ArrayList<ErrorHighlight>();
    	if (geomList != null)
    	{
    		for(Geometric geom : geomList)
                h.add(ErrorHighlight.newInstance(null, geom));
    	}
    	if (exportList != null)
    	{
    		for(Export e : exportList)
                h.add(new ErrorHighExport(null, e));
    	}
    	if (lineList != null)
    	{
    		for(int i=0; i<lineList.size(); i += 2)
                h.add(new ErrorHighLine(cell, lineList.get(i), lineList.get(i+1), false));
    	}
    	if (pointList != null)
    	{
    		for(EPoint pt : pointList)
                h.add(new ErrorHighPoint(cell, pt));
    	}
    	if (polyList != null)
    	{
    		for(PolyBase poly : polyList)
    		{
    	        Point2D [] points = poly.getPoints();
    	        for(int i=0; i<points.length; i++)
    	        {
    	            int prev = i-1;
    	            if (i == 0) prev = points.length-1;
    	            h.add(new ErrorHighLine(cell, new EPoint(points[prev].getX(), points[prev].getY()),
    	            	new EPoint(points[i].getX(), points[i].getY()), false));
    	        }
    		}
    	}
    	logAWarning(message, cell, sortKey, h);
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
                MessageLog el = allErrors.get(i);

                if (el.findGeometries(geom1, cell, geom2, cell2))
                    return (true);
		    }
        }
        else
        {
            for (int i = 0; i < allWarnings.size(); i++)
            {
                MessageLog el = allWarnings.get(i);

                if (el.findGeometries(geom1, cell, geom2, cell2))
                    return (true);
            }
        }
		return (false);
	}

    /**
     * Removes all errors associated with Cell cell.
     * @param cell the cell for which errors will be removed
     * @return true if some logs were removed.
     */
    public synchronized boolean clearLogs(Cell cell) {
        CellId cellId = (CellId)cell.getId();
        ArrayList<MessageLog> errLogs = new ArrayList<MessageLog>();
        // Errors
        boolean removed = false;
        for (MessageLog log : allErrors) {
            if (log.logCellId != cellId)
                errLogs.add(log);
            else
                removed = true;
        }
        allErrors = errLogs;

	    ArrayList<WarningLog> warndLogs = new ArrayList<WarningLog>();
        // Warnings
        for (WarningLog log : allWarnings) {
            if (log.logCellId != cellId)
                warndLogs.add(log);
            else
                removed = true;
        }
        allWarnings = warndLogs;
        
        return removed;
    }

     public void save(String filePath)
    {
         PrintStream buffWriter = null;
         try
         {
            buffWriter = new PrintStream(new FileOutputStream(filePath));
         } catch (Exception e)
         {
             e.printStackTrace();
             System.out.println("Error opening " + filePath);
             return; // error opening the file
         }

        // Creating header
        buffWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        buffWriter.println();
        buffWriter.println("<!DOCTYPE ErrorLogger");
        buffWriter.println(" [");
        buffWriter.println(" <!ELEMENT ErrorLogger (MessageLog|WarningLog)*>");
        buffWriter.println(" <!ELEMENT MessageLog (ERRORTYPEGEOM|ERRORTYPETHICKLINE|ERRORTYPELINE)* >");
        buffWriter.println(" <!ELEMENT WarningLog ANY >");
        buffWriter.println(" <!ELEMENT ERRORTYPEGEOM ANY>");
        buffWriter.println(" <!ELEMENT ERRORTYPELINE ANY>");
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
        buffWriter.println("    cellName CDATA #REQUIRED");
        buffWriter.println(" >");
        buffWriter.println(" <!ATTLIST ERRORTYPELINE");
        buffWriter.println("    p1 CDATA #REQUIRED");
        buffWriter.println("    p2 CDATA #REQUIRED");
        buffWriter.println("    cellName CDATA #REQUIRED");
        buffWriter.println(" >");
        buffWriter.println(" ]>");
        buffWriter.println();
        String className = this.getClass().getSimpleName();
        buffWriter.println("<" + className + " errorSystem=\"" + errorSystem + "\">");
	    for (MessageLog log : allErrors) {
            log.xmlDescription(buffWriter);
        }
        // Warnings
        for (WarningLog log : allWarnings) {
            log.xmlDescription(buffWriter);
        }
        buffWriter.println("</" + className + ">");
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
//        termLogging_(true);
        Job.getUserInterface().termLogging(this, explain, true);
//        alreadyExplained = true;
    }

    public synchronized void termLogging_(boolean terminate)
    {
        // enumerate the errors
        int errs = 0;
        for(MessageLog el : allErrors)
        {
            el.index = ++errs;
        }
	    for(WarningLog el : allWarnings)
        {
            el.index = ++errs;
        }
        if (terminate)
            terminated = true;
    }

    /**
     * Method to retrieve general information about the errorLogger.
     * @return general information about the errorLogger.
     */
    public String getInfo()
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

    public MessageLog getLog(int i) {
        return i < allErrors.size() ? allErrors.get(i) : allWarnings.get(i - allErrors.size());
    }
    
    /**
     * Method to list all logged errors and warnings.
     * @return an Iterator over all of the "ErrorLog" objects.
     */
    public synchronized Iterator<MessageLog> getLogs() {
        List<MessageLog> copy = new ArrayList<MessageLog>();
        for (MessageLog ml : allErrors) {
            copy.add(ml);
        }
	    for (WarningLog wl : allWarnings) {
            copy.add(wl);
        }
        return copy.iterator();
    }

    public synchronized void deleteLog(int i) {
        if (i < allErrors.size())
            allErrors.remove(i);
        else
            allWarnings.remove(i - allErrors.size());
    }

    // ----------------------------- Explorer Tree Stuff ---------------------------

//     public void databaseEndChangeBatch(Undo.ChangeBatch batch) {
//         // check if any errors need to be deleted
//         boolean changed = false;
//         for (Iterator<MessageLog> it = getLogs(); it.hasNext(); ) {
//             MessageLog err = it.next();
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

    public static class XMLParser
    {
        public ErrorLogger process(URL fileURL, boolean verbose)
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
                if (verbose) System.out.println("Parsing XML file \"" + fileURL + "\"");
                XMLHandler handler = new XMLHandler();
                parser.parse(inputStream, handler);
                if (verbose) System.out.println("End Parsing XML file ...");
                return handler.logger;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return null;
            }
        }

        private class XMLHandler extends DefaultHandler
        {
            private ErrorLogger logger = null;
            private Cell curCell;
            private String message = "";
        	private List<ErrorHighlight> highlights;
        	private Set<String> badCellNames = new HashSet<String>();

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

            public void endElement (String uri, String localName, String qName)
            {
                boolean errorLogBody = qName.equals("MessageLog");
                boolean warnLogBody = qName.equals("WarningLog");
                if (errorLogBody)
                {
                    int sortLayer = 0;
                    if (curCell != null) sortLayer = curCell.hashCode();
                    logger.logAnError(message, curCell, sortLayer, highlights);
                    message = "";
                }
                else if (warnLogBody)
                {
                    int sortLayer = 0;
                    if (curCell != null) sortLayer = curCell.hashCode();
                    logger.logAWarning(message, curCell, sortLayer, highlights);
                    message = "";
                }
            }

            public void startElement (String uri, String localName, String qName, Attributes attributes)
            {
                boolean loggerBody = qName.equals("ErrorLogger");
                boolean errorLogBody = qName.equals("MessageLog");
                boolean warnLogBody = qName.equals("WarningLog");
                boolean geoTypeBody = qName.equals("ERRORTYPEGEOM");
                boolean thickLineTypeBody = qName.equals("ERRORTYPETHICKLINE");
                boolean thinLineTypeBody = qName.equals("ERRORTYPELINE");

                if (!loggerBody && !errorLogBody && !warnLogBody && !geoTypeBody && !thinLineTypeBody && !thickLineTypeBody) return;

                String cellName = null, geomName = null, viewName = null, libraryName = null;
                EPoint p1 = null, p2 = null;

                for (int i = 0; i < attributes.getLength(); i++)
                {
                    if (attributes.getQName(i).equals("errorSystem"))
                    {
                        // Ignore the rest of the attribute and generate the logger
                        logger = ErrorLogger.newInstance(attributes.getValue(i));
                        return;
                    }
                    else if (attributes.getQName(i).startsWith("message"))
                        message = attributes.getValue(i);
                    else if (attributes.getQName(i).startsWith("cell"))
                    {
                        String origName = attributes.getValue(i);
                        String[] names = TextUtils.parseString(attributes.getValue(i), "{}");
                        cellName = origName; // names[0];
                        viewName = names[1];
                        // cellName might contain library name
                        names = TextUtils.parseString(cellName, ":");
                        if (names.length > 1)
                        {
                            libraryName = names[0];
                            cellName = names[1];
                        }
                    }
                    else if (attributes.getQName(i).startsWith("geom"))
                        geomName = attributes.getValue(i);
                    else if (attributes.getQName(i).startsWith("p1"))
                    {
                        String[] points = TextUtils.parseString(attributes.getValue(i), "(,)");
                        double x = Double.parseDouble(points[0]);
                        double y = Double.parseDouble(points[1]);
                        p1 = new EPoint(x, y);
                    }
                    else if (attributes.getQName(i).startsWith("p2"))
                    {
                        String[] points = TextUtils.parseString(attributes.getValue(i), "(,)");
                        double x = Double.parseDouble(points[0]);
                        double y = Double.parseDouble(points[1]);
                        p2 = new EPoint(x, y);
                    }
                    else if (attributes.getQName(i).startsWith("center"))
                    {
                        String[] points = TextUtils.parseString(attributes.getValue(i), "(,)");
                    }
                    else
                        new Error("Invalid attribute in XMLParser");
                }
                View view = View.findView(viewName);
                curCell = Library.findCellInLibraries(cellName, view, libraryName);
                if (curCell == null || !curCell.isLinked())
                {
                	if (!badCellNames.contains(cellName))
                	{
                		badCellNames.add(cellName);
                		System.out.println("Cannot find cell: " + cellName);
                	}
                	return;
                }
                if (errorLogBody)
                {
                	highlights = new ArrayList<ErrorHighlight>();
                }
                else if (warnLogBody)
                {
                    highlights = new ArrayList<ErrorHighlight>();
                }
                else if (geoTypeBody)
                {
                    Geometric geom = curCell.findNode(geomName);
                    if (geom == null) // try arc instead
                        geom = curCell.findArc(geomName);
                    if (geom != null)
                        highlights.add(ErrorHighlight.newInstance(null, geom));
                    else
                        System.out.println("Invalid geometry " + geomName + " in " + curCell);
                }
                else if (thinLineTypeBody || thickLineTypeBody)
                {
                    highlights.add(new ErrorHighLine(curCell, p1, p2, thickLineTypeBody));
                }
                else
                    new Error("Invalid attribute in XMLParser");
            }

            public void fatalError(SAXParseException e)
            {
                System.out.println("Parser Fatal Error in " + e.getLineNumber() + " " + e.getPublicId() + " " + e.getSystemId());
                e.printStackTrace();
            }

            public void warning(SAXParseException e)
            {
                System.out.println("Parser Warning in " + e.getLineNumber() + " " + e.getPublicId() + " " + e.getSystemId());
                e.printStackTrace();
            }

            public void error(SAXParseException e)
            {
                System.out.println("Parser Error in " + e.getLineNumber() + " " + e.getPublicId() + " " + e.getSystemId());
                e.printStackTrace();
            }
        }
    }
}
