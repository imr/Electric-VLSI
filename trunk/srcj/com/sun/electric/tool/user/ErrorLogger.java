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

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.IdReader;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.Job;

import java.awt.geom.Point2D;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
     * Method to replace invalid characters in XML such as > or <
     * @param message
     * @return
     */
    private static String correctXmlString(String message)
    {
        String m = message.replaceAll(">", "&gt;");
            m = m.replaceAll("<", "&lt;");
        return m;
    }

    /**
     * Function to write all headers related to ErrorLogger classes.
     * @param indent
     * @param ps
     */
    public static void writeXmlHeader(String indent, PrintStream ps)
    {
        // ErrorLogger and GroupLog
        ps.println(indent + "<!ELEMENT ErrorLogger (GroupLog|MessageLog|WarningLog)*>");
        ps.println(indent + "<!ELEMENT GroupLog (MessageLog|WarningLog)*>");

        ps.println(indent + "<!ATTLIST ErrorLogger");
        ps.println(indent + "   errorSystem CDATA #REQUIRED");
        ps.println(indent + ">");

        ps.println(indent + "<!ATTLIST GroupLog");
        ps.println(indent + "   message CDATA #REQUIRED");
        ps.println(indent + ">");

        // For MessageLog and WarningLog
        ps.println(indent + "<!ELEMENT MessageLog ("+ ErrorHighlight.getImplementedXmlHeaders() +")*>");

        ps.println(indent + "<!ATTLIST MessageLog");
        ps.println(indent + "   message CDATA #REQUIRED");
        ps.println(indent + "   cellName CDATA #REQUIRED");
        ps.println(indent + ">");

        ps.println(indent + "<!ELEMENT WarningLog ANY>");
        ps.println(indent + "<!ATTLIST WarningLog");
        ps.println(indent + "   message CDATA #REQUIRED");
        ps.println(indent + "   cellName CDATA #IMPLIED"); // only WarningLogs can have no cells
        ps.println(indent + ">");
    }

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
            this(message, cell != null ? (CellId)cell.getId() : null, sortKey, highlights);
        }

        public MessageLog(String message, CellId logCellId, int sortKey, List<ErrorHighlight> highlights) {
            this.message = message;
            this.logCellId = logCellId;
            this.sortKey = sortKey;
            this.highlights = highlights.toArray(ErrorHighlight.NULL_ARRAY);
            index = 0;
        }

        public Cell getCell() { return (logCellId!=null)?EDatabase.clientDatabase().getCell(logCellId):null;}

        public String getMessageString() { return message; }

        public int getNumHighlights() {return highlights.length;}

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

        protected void writeXmlDescription(PrintStream msg)
        {
            String className = this.getClass().getSimpleName();
            String cellInfo = "";

            if (logCellId != null)
            {
                Cell logCell = getCell();
                if (logCell != null)
                    cellInfo = "cellName=\"" + logCell.describe(false) + "\"";
            }
            // replace those characters that XML defines as special such as ">" and "&"
            String m = correctXmlString(message);
            msg.append("\t<" + className + " message=\"" + m + "\" " + cellInfo + ">\n");
            for(ErrorHighlight eh : highlights)
            {
                eh.writeXmlDescription("\t\t", msg, EDatabase.clientDatabase());
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

        void write(IdWriter writer) throws IOException {
            boolean isWarning = this instanceof WarningLog;
            writer.writeBoolean(isWarning);
            writer.writeString(message);
            boolean hasCellId = logCellId != null;
            writer.writeBoolean(hasCellId);
            if (hasCellId)
                writer.writeNodeProtoId(logCellId);
            writer.writeInt(sortKey);
            writer.writeInt(highlights.length);
            for (int i = 0; i < highlights.length; i++)
                highlights[i].write(writer);
            writer.writeInt(index);
        }

        private static MessageLog read(IdReader reader) throws IOException {
            boolean isWarning = reader.readBoolean();
            String message = reader.readString();
            boolean hasCellId = reader.readBoolean();
            CellId cellId = hasCellId ? (CellId)reader.readNodeProtoId() : null;
            int sortKey = reader.readInt();
            ErrorHighlight[] highlights = new ErrorHighlight[reader.readInt()];
            for (int i = 0; i < highlights.length; i++)
                highlights[i] = ErrorHighlight.read(reader);
            MessageLog log;
            if (isWarning)
                log = new WarningLog(message, cellId, sortKey, Arrays.asList(highlights));
            else
                log = new MessageLog(message, cellId, sortKey, Arrays.asList(highlights));
            log.index = reader.readInt();
            return log;
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
       public WarningLog(String message, CellId cellId, int sortKey, List<ErrorHighlight> highlights) { super(message, cellId, sortKey, highlights); }
    }

//	private boolean alreadyExplained;
    private int errorLimit;
    private List<MessageLog> allErrors = new ArrayList<MessageLog>();
	private List<WarningLog> allWarnings = new ArrayList<WarningLog>();
    private boolean limitExceeded;
    private String errorSystem;
    private boolean terminated;
    private boolean persistent; // cannot be deleted
    private Map<Integer,String> sortKeysToGroupNames; // association of sortKeys to GroupNames

    public Map<Integer,String> getSortKeyToGroupNames() { return sortKeysToGroupNames; }

    public String getSystem() { return errorSystem; }

    public boolean isPersistent() {return persistent;}

    public ErrorLogger() {}

    public void write(IdWriter writer) throws IOException {
        writer.writeDiffs();
        writer.writeInt(errorLimit);
        int numErrors = allErrors.size();
        writer.writeInt(numErrors);
        for (int i = 0; i < numErrors; i++)
            allErrors.get(i).write(writer);
        int numWarnings = allWarnings.size();
        writer.writeInt(numWarnings);
        for (int i = 0; i < numWarnings; i++)
            allWarnings.get(i).write(writer);
        writer.writeString(errorSystem);
        writer.writeBoolean(terminated);
        writer.writeBoolean(persistent);
        if (sortKeysToGroupNames != null) {
            writer.writeInt(sortKeysToGroupNames.size());
            for (Map.Entry<Integer,String> e: sortKeysToGroupNames.entrySet()) {
                writer.writeInt(e.getKey().intValue());
                writer.writeString(e.getValue());
            }
        } else {
            writer.writeInt(-1);
        }
    }

    public static ErrorLogger read(IdReader reader) throws IOException {
        reader.readDiffs();
        ErrorLogger logger = new ErrorLogger();
        logger.errorLimit = reader.readInt();
        int numErrors = reader.readInt();
        for (int i = 0; i < numErrors; i++)
            logger.allErrors.add(MessageLog.read(reader));
        int numWarnings = reader.readInt();
        for (int i = 0; i < numWarnings; i++)
            logger.allWarnings.add((WarningLog)MessageLog.read(reader));
        logger.errorSystem = reader.readString();
        logger.terminated = reader.readBoolean();
        logger.persistent = reader.readBoolean();
        int numGroups = reader.readInt();
        if (numGroups >= 0) {
            logger.sortKeysToGroupNames = new HashMap<Integer,String>();
            for (int i = 0; i < numGroups; i++) {
                Integer sortKey = Integer.valueOf(reader.readInt());
                String groupName = reader.readString();
                logger.sortKeysToGroupNames.put(sortKey, groupName);
            }
        }
        return logger;
    }

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
        logger.limitExceeded = false;
        logger.errorSystem = system;
        logger.errorLimit = User.getErrorLimit();
        logger.terminated = false;
        logger.persistent = persistent;
//        logger.alreadyExplained = false;
        logger.sortKeysToGroupNames = null;
        return logger;
    }

    public void addMessages(List<MessageLog> messages)
    {
        if (messages == null) return; // to avoid to increate empty lists during incremental checking
        for (MessageLog m: messages) {
            if (m instanceof WarningLog)
                allWarnings.add((WarningLog)m);
            else
                allErrors.add(m);
        }
//        if (persistent) Job.getUserInterface(). wantToRedoErrorTree();
    }

    public void addMessages(ErrorLogger logger)
    {
        allWarnings.addAll(logger.allWarnings);
        allErrors.addAll(logger.allErrors);
    }

    public void deleteMessages(List<MessageLog> messages)
    {
        if (messages == null) return; // to avoid to increate empty lists during incremental checking
        for (MessageLog m: messages) {
            if (m instanceof WarningLog)
                allWarnings.remove(m);
            else
                allErrors.remove(m);
        }
//        if (persistent) Job.getUserInterface(). wantToRedoErrorTree();
    }

    /**
     * Factory method to create an error message and log.
     * with the given text "message" applying to cell "cell".
     * Returns a pointer to the message (0 on error) which can be used to add highlights.
     */
    private synchronized MessageLog logAnError(String message, Cell cell, int sortKey, List<ErrorHighlight> highlights) {
        CellId cellId = cell != null ? cell.getId() : null;
        return logAnError(message, cellId, sortKey, highlights);
    }

    /**
     * Factory method to create an error message and log.
     * with the given text "message" applying to cell "cell".
     * Returns a pointer to the message (0 on error) which can be used to add highlights.
     */
    private synchronized MessageLog logAnError(String message, CellId cellId, int sortKey, List<ErrorHighlight> highlights)
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
        MessageLog el = new MessageLog(message, cellId, sortKey, highlights);

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
    	logAnError(message, (CellId)null, sortKey, h);
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
     * @param cellId the Id of the cell in which this message applies.
     * @param sortKey the sorting order of this message.
     */
    public synchronized void logError(String message, CellId cellId, int sortKey)
    {
    	List<ErrorHighlight> h = new ArrayList<ErrorHighlight>();
    	logAnError(message, cellId, sortKey, h);
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
        Cell cell = pp.getParent();
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
     * Factory method to log an error or warning message.
     * @param message the string to display.
     * @param list a list of nodes, arcs, exports or polygons, points to display. Must be no null.
     * @param cell the cell in which this message applies.
     * @param sortKey the sorting order of this message.
     * @param isErrorMsg true if an error message is logged
     */
    public synchronized void logMessage(String message, List<?> list, Cell cell, int sortKey, boolean isErrorMsg)
    {
        logMessageWithLines(message, list, null, cell, sortKey, isErrorMsg);
    }

    /**
     * Factory method to log an error or warning message with extra lines.
     * @param message the string to display.
     * @param list a list of nodes, arcs, exports or polygons, points to display. Must be no null.
     * @param lineList a list of points defining a set of lines (may be null)
     * @param cell the cell in which this message applies.
     * @param sortKey the sorting order of this message.
     * @param isErrorMsg true if an error message is logged
     */
    public synchronized void logMessageWithLines(String message, List<?> list, List<EPoint> lineList,
                                                 Cell cell, int sortKey, boolean isErrorMsg)
    {
    	List<ErrorHighlight> h = new ArrayList<ErrorHighlight>();

        if (list != null)
        {
            for (Object obj : list)
            {
                if (obj instanceof Geometric)
                {
                     h.add(ErrorHighlight.newInstance(null, (Geometric)obj));
                }
                else if (obj instanceof Export)
                {
                    h.add(new ErrorHighExport(null, (Export)obj));
                }
                else if (obj instanceof EPoint)
                {
                    h.add(new ErrorHighPoint(cell, (EPoint)obj));
                }
                else if (obj instanceof PolyBase)
                {
                    PolyBase poly = (PolyBase)obj;
                    Point2D [] points = poly.getPoints();
                    List<ErrorHighlight> l = new ArrayList<ErrorHighlight>();

                    for(int i=0; i<points.length; i++)
                    {
                        int prev = i-1;
                        if (i == 0) prev = points.length-1;
                        l.add(new ErrorHighLine(cell, new EPoint(points[prev].getX(), points[prev].getY()),
                                                new EPoint(points[i].getX(), points[i].getY()), true));
                    }
                    h.add(new ErrorHighPoly(cell, null, l));
                }
                else
                    assert(false);
            }
        }
        if (lineList != null)
        {
    		for(int i=0; i<lineList.size(); i += 2)
                h.add(new ErrorHighLine(cell, lineList.get(i), lineList.get(i+1), true));
        }
        if (isErrorMsg)
            logAnError(message, cell, sortKey, h);
        else
            logAWarning(message, cell, sortKey, h);
    }

    /**
     * Factory method to log an error or a warning message.
     * @param message the string to display.
     * @param geomList a list of nodes or arcs to display (may be null).
     * @param polyList a list of polygons to display (may be null).
     * @param cell the cell in which this message applies.
     * @param sortKey the sorting order of this message.
     * @param errorMsg
     */
    public synchronized void logMessage(String message, List<Geometric> geomList, List<PolyBase> polyList,
                                        Cell cell, int sortKey, boolean errorMsg)
    {
    	List<ErrorHighlight> h = new ArrayList<ErrorHighlight>();
        boolean matches = geomList != null && polyList != null && geomList.size() == polyList.size();
    	if (geomList != null && !matches)
    	{
    		for(Geometric geom : geomList)
                h.add(ErrorHighlight.newInstance(null, geom));
    	}
    	if (polyList != null)
    	{
            // must match number of elements
            for(PolyBase poly : polyList)
    		{
    	        Point2D [] points = poly.getPoints();
                List<ErrorHighlight> list = new ArrayList<ErrorHighlight>();

                for(int i=0; i<points.length; i++)
    	        {
    	            int prev = i-1;
    	            if (i == 0) prev = points.length-1;
    	            list.add(new ErrorHighLine(cell, new EPoint(points[prev].getX(), points[prev].getY()),
    	            	new EPoint(points[i].getX(), points[i].getY()), true));
                }
                h.add(new ErrorHighPoly(cell, null, list));
            }
    	}

        if (errorMsg)
            logAnError(message, cell, sortKey, h);
        else
            logAWarning(message, cell, sortKey, h);
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

    public synchronized int getNumMessages(Cell cell, boolean searchInError) {
        int numErrors = 0;

        if (searchInError)
        {
            for (int i=0; i<allErrors.size(); i++)
            {
                MessageLog el = allErrors.get(i);
                if (el.logCellId == cell.getId())
                    numErrors++;
            }
        }
        else
        {
            for (int i=0; i<allWarnings.size(); i++)
            {
                MessageLog el = allWarnings.get(i);
                if (el.logCellId == cell.getId())
                    numErrors++;
            }
        }

        return numErrors;
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
     * Method to remove all errors and warnings
     */
    public synchronized void clearAllLogs()
    {
        allErrors.clear();
        allWarnings.clear();
    }

    /**
     * Method to retrieve all MessageLogs associated with a given Cell
     * @param cell the Cell to examine.
     * @return all MessageLogs associated with the Cell.
     */
    public synchronized List<MessageLog> getAllLogs(Cell cell)
    {
        CellId cellId = cell.getId();
        List<MessageLog> msgLogs = new ArrayList<MessageLog>();
        // Searching errors
        for (MessageLog log : allErrors) {
            if (log.logCellId == cellId)
                msgLogs.add(log);
        }
        // Searching warnings
        for (WarningLog log : allWarnings) {
            if (log.logCellId == cellId)
                msgLogs.add(log);
        }
        return msgLogs;
    }

    /**
     * Removes all errors and warnings associated with Cell cell.
     * @param cell the cell for which errors and warnings will be removed
     * @return true if any log was removed.
     */
    public synchronized boolean clearLogs(Cell cell) {
        CellId cellId = cell.getId();
        List<MessageLog> errLogs = new ArrayList<MessageLog>();
        // Errors
        boolean removed = false;
        for (MessageLog log : allErrors) {
            if (log.logCellId != cellId)
                errLogs.add(log);
            else
                removed = true;
        }
        allErrors = errLogs;

	    List<WarningLog> warndLogs = new ArrayList<WarningLog>();
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

    public void exportErrorLogger(String filePath)
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

        // ErrorHighArc.class is treaded with same header as  ErrorHighNode
        Class[] errorTypes = {ErrorHighLine.class, ErrorHighPoint.class, ErrorHighPoly.class, ErrorHighNode.class, ErrorLogger.class};

        // Creating header
        buffWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        buffWriter.println();
        buffWriter.println("<!DOCTYPE ErrorLogger");
        buffWriter.println(" [");

        for (int i = 0; i < errorTypes.length; i++)
        {
            Class<?> c = errorTypes[i];
            try {
                String indent = " ";
                java.lang.reflect.Method set = c.getMethod("writeXmlHeader", new Class[] {String.class, PrintStream.class});
                set.invoke(c, new Object[] {indent, buffWriter});
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        buffWriter.println(" ]>");
        buffWriter.println();
        String className = this.getClass().getSimpleName();
        buffWriter.println("<" + className + " errorSystem=\"" + errorSystem + "\">");

        if (sortKeysToGroupNames != null)
        {
            // The keys must be sorted to keep same order as in the original ErrorLogger
            Set<Integer> set = sortKeysToGroupNames.keySet();
            List<Integer> sortedInt = new ArrayList<Integer>(set.size());
            sortedInt.addAll(set); // adding to a list to sort them
            Collections.sort(sortedInt);
            for (Integer i : sortedInt)
            {
                String groupName = sortKeysToGroupNames.get(i);
                buffWriter.println("    <GroupLog message=\"" + correctXmlString(groupName) + "\">");
                // Errors
                for (MessageLog log : allErrors) {
                    if (log.getSortKey() == i.intValue())
                        log.writeXmlDescription(buffWriter);
                }
                // Warnings
                for (WarningLog log : allWarnings) {
                    if (log.getSortKey() == i.intValue())
                        log.writeXmlDescription(buffWriter);
                }
                buffWriter.println("    </GroupLog>");
            }
        }
        else // plain style
        {
            // Errors
            for (MessageLog log : allErrors) {
                log.writeXmlDescription(buffWriter);
            }
            // Warnings
            for (WarningLog log : allWarnings) {
                log.writeXmlDescription(buffWriter);
            }
        }
        buffWriter.println("</" + className + ">");
        buffWriter.close();
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
     * Get a group name for a sortKey. It creates the hash map if it
     * doesn't exist
     * @param sortKey the error log sortKey
     * @return the group name. Null if no group name was found
     */
    public String getGroupName(int sortKey)
    {
        if (sortKeysToGroupNames == null) {
            sortKeysToGroupNames = new HashMap<Integer,String>();
        }
        return sortKeysToGroupNames.get(new Integer(sortKey));
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

    public int getLogIndex(MessageLog log)
    {
        int index = allErrors.indexOf(log);
        if (index != -1)
            return index;
        index = allWarnings.indexOf(log);
        if (index != -1)
            return index + allErrors.size();
        assert(false); // it should not reach one
        return -1;
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
        public ErrorLogger process(URL fileURL, boolean verbose) throws Exception
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

        private class XMLHandler extends DefaultHandler
        {
            private ErrorLogger logger = null;
            private Cell curCell;
            private String message = "";
        	private List<ErrorHighlight> highlights;
            private List<ErrorHighlight> currentList;
            private Set<String> badCellNames = new HashSet<String>();
            private int theSortLayer = -1, sortGroups = 1; // start from 1 so the errors without group would be all together

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
                boolean grpLogBody = qName.equals("GroupLog");
                boolean errorPolyBody = ErrorHighlight.isErrorPoly(qName);

                if (errorLogBody || warnLogBody)
                {
                    int sortLayer = 0;
                    if (theSortLayer != -1) // use the group information from the file
                       sortLayer = theSortLayer;
                    else if (curCell != null) sortLayer = curCell.hashCode(); // sort by cell
                    if (errorLogBody)
                        logger.logAnError(message, curCell, sortLayer, highlights);
                    else
                        logger.logAWarning(message, curCell, sortLayer, highlights);
                    message = "";
                }
                else if (grpLogBody)
                {
                    theSortLayer = -1; // reset to null again
                }
                else if (errorPolyBody)
                {
                    this.currentList = this.highlights;
                }
            }

            public void startElement (String uri, String localName, String qName, Attributes attributes)
            {
                boolean loggerBody = qName.equals("ErrorLogger");
                boolean groupBody = qName.equals("GroupLog");
                boolean errorLogBody = qName.equals("MessageLog");
                boolean warnLogBody = qName.equals("WarningLog");
                boolean errorHighlighBody = ErrorHighlight.isErrorHighlightBody(qName);

                if (!loggerBody && !errorLogBody && !warnLogBody && !errorHighlighBody
                        && !groupBody) return;

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
                    else if (attributes.getQName(i).startsWith("p1") || attributes.getQName(i).startsWith("pt"))
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
//                        String[] points = TextUtils.parseString(attributes.getValue(i), "(,)");
                    }
                    else
                        new Error("Invalid attribute in XMLParser");
                }
                if (groupBody)
                {
                    assert (message != null);
                    theSortLayer = sortGroups;
                    logger.setGroupName(sortGroups++, message);
                }
                else
                {
                    if (viewName != null)
                    {
                        View view = View.findView(viewName);
                        curCell = Library.findCellInLibraries(cellName, view, libraryName);
                        if ((curCell == null || !curCell.isLinked()))
                        {
                            if (!badCellNames.contains(cellName))
                            {
                                badCellNames.add(cellName);
                                System.out.println("Cannot find cell: " + cellName);
                            }
                            //return;
                        }
                    }
                    if (errorLogBody || warnLogBody)
                    {
                        highlights = new ArrayList<ErrorHighlight>();
                        currentList = highlights;
                    }
                    else if (errorHighlighBody)
                    {
                        currentList = ErrorHighlight.addErrorHighlight(qName, curCell, geomName, p1, p2, currentList);
                    }
                    else
                        new Error("Invalid attribute in XMLParser");
                }
            }

            public void fatalError(SAXParseException e)
            {
                System.out.println("Parser Fatal Error on line " + e.getLineNumber() + ": " + e.getMessage());
                e.printStackTrace();
            }

            public void warning(SAXParseException e)
            {
                System.out.println("Parser Warning on line " + e.getLineNumber() + ": " + e.getMessage());
                e.printStackTrace();
            }

            public void error(SAXParseException e)
            {
                System.out.println("Parser Error on line " + e.getLineNumber() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
