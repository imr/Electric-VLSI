package com.sun.electric.tool.drc;

import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * This reads an ASCII Calibre DRC error database
 * produced by running Calibre DRC.
 * It shows these errors on the specified cells in
 * Electric.
 */
public class CalibreDrcErrors {

    private int scale;
    private String topCellName;
    private Cell topCell;
    private ErrorLogger logger;
    private BufferedReader in;
    private List ruleViolations;            // list of DrcRuleViolations
    private int lineno;

    private static final String spaces = "[\\s\\t ]+";

    /**
     * Create a new CalibreDrcError class, read the errors in,
     * and then convert them to the ErrorLogger.
     * @param filename the ASCII calibre drc results database file
     */
    public static void importErrors(String filename) {
        BufferedReader in;
        try {
            FileReader reader = new FileReader(filename);
            in = new BufferedReader(reader);
        } catch (IOException e) {
            System.out.println("Error importing DRC Errors: "+e.getMessage());
            return;
        }

        if (in == null) return;
        CalibreDrcErrors errors = new CalibreDrcErrors(in);
        // read first line
        if (!errors.readTop()) return;
        if (!errors.readRules()) return;
        // finish
        errors.done();
    }

    // Constructor
    private CalibreDrcErrors(BufferedReader in) {
        assert(in != null);
        this.in = in;
        lineno = 1;
        logger = ErrorLogger.newInstance("Calibre DRC Errors");
    }

    // read the cell name and precision, if any
    private boolean readTop() {
        scale = 1000;
        String line;
        try {
            line = readLine(true);
        } catch (IOException e) {
            System.out.println("Error reading first line of file: "+e.getMessage());
            return false;
        }
        if (line == null) return false;

        String [] parts = line.trim().split(spaces);
        if (parts.length == 1) {
            topCellName = parts[0];
            return true;
        } else if (parts.length == 2) {
            topCellName = parts[0];
            try {
                scale = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                System.out.println("Error converting precision '"+parts[1]+"' to a number, using default of "+scale);
            }
            return true;
        }
        // error
        System.out.println("Error on first line: Expected cell name and precision, or 'drc'");
        return false;
    }

    private boolean readRules() {
        // read all errors
        try {
            while(true) {
                DrcRuleViolation v = readRule();
                if (v == null) break;
                ruleViolations.add(v);
            }
        } catch (IOException e) {
            System.out.println("Error reading file: "+e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Read a logged error.  Return false if there was a problem, or nothing left to read.
     * @return true if read ok, false if error or End of File
     */
    private DrcRuleViolation readRule() throws IOException {
        // read the first line: the rule name
        String ruleName = readLine(false);
        if (ruleName == null) return null;     // EOF, no more errors

        // read the header start line, tells us how many header lines there are
        String headerStart = readLine();
        if (headerStart == null) return null;
        Header header = Header.createHeader(headerStart, lineno);
        if (header == null) return null;

        // read the rest of the header
        for (int i=0; i<header.headerLength; i++) {
            String s = readLine();
            if (s == null) return null;
            header.addHeaderLine(s);
        }

        DrcRuleViolation v = new DrcRuleViolation(ruleName, header);

        // read errors: hashMap: key is integer, value is list of Shape objects
        for (int i=0; i<header.currentDrcResultsCount; i++) {
            DrcError drc = readErrorShape();
            if (drc == null) break;
            v.addError(drc);
        }
        return v;
    }

    // populate a list of error shapes. return false on error.
    private DrcError readErrorShape() throws IOException {
        // we need to peek ahead, as it is unspecified how many error shapes there
        // may be
        String nextLine = readLine().trim();
        boolean boole = nextLine.startsWith("e") ? true : false;
        boolean boolp = nextLine.startsWith("p") ? true : false;

        if (boole || boolp) {
            String [] parts = nextLine.split(spaces);
            if (parts.length != 3) {
                System.out.println("Error on shape: expected ordinal and count numbers, line number "+lineno+": "+nextLine);
                return null;
            }
            int ordinal = 0;
            int lines = 0;
            try {
                ordinal = Integer.parseInt(parts[1]);
                lines = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                System.out.println("Error on shape: expected ordinal and count numbers, line number "+lineno+": "+nextLine);
                return null;
            }
            // need to peek ahead to see if next line specifies a subcell
            DrcError drc = new DrcError(topCell);

            // parse list of edges if this is edges shape
            if (boole) {
                for (int i=0; i<lines; i++) {
                    nextLine = readLine();
                    Shape s = parseErrorEdge(nextLine);
                    if (s == null) return drc;
                    drc.addShape(s);
                }
            }
            // parse list of poly vertices if this is a poly
            else {
                // boolp
                Point2D [] points = new Point2D[lines];
                for (int i=0; i<lines; i++) {
                    nextLine = readLine();
                    if (!parseErrorPoint(nextLine, points, i))
                        return null;
                }
                Shape s = new Poly(points);
                drc.addShape(s);
            }
            return drc;
        }
        else {
            System.out.println("Error, expected Edge or Poly definition on line number "+lineno+": "+nextLine);
        }
        return null;
    }

    // parse a line specifying an edge, and add it to a list of shapes
    private Shape parseErrorEdge(String line) {
        String [] vals = line.trim().split(spaces);
        if (vals.length != 4) {
            System.out.println("Error, bad format for edge on line number "+lineno+": "+line);
            return null;
        }
        try {
            double x1 = (double)Integer.parseInt(vals[0])/(double)scale;
            double y1 = (double)Integer.parseInt(vals[1])/(double)scale;
            double x2 = (double)Integer.parseInt(vals[2])/(double)scale;
            double y2 = (double)Integer.parseInt(vals[3])/(double)scale;
            Shape line2d = new Line2D.Double(x1, y1, x2, y2);
            return line2d;
        } catch (NumberFormatException e) {
            System.out.println("Error, bad format for edge on line number "+lineno+": "+line);
            return null;
        }
    }

    // parse a line specifying a polygon vertex, and add it to a list of points
    private boolean parseErrorPoint(String line, Point2D [] points, int point) {
        String [] vals = line.trim().split(spaces);
        if (vals.length != 2) {
            System.out.println("Error, bad format for poly vertex on line number "+lineno+": "+line);
            return false;
        }
        try {
            double x1 = (double)Integer.parseInt(vals[0])/(double)scale;
            double y1 = (double)Integer.parseInt(vals[1])/(double)scale;
            Point2D p = new Point2D.Double(x1, y1);
            points[point] = p;
            return true;
        } catch (NumberFormatException e) {
            System.out.println("Error, bad format for poly vertex on line number "+lineno+": "+line);
            return false;
        }
    }

    private void done() {
        try {
            in.close();
        } catch (IOException e) {}
    }

    // -----------------------------------------------------------------------------

    private static class DrcRuleViolation {
        private final String ruleNumber;
        private final Header header;
        private final List errors;              // list of DrcErrors

        private DrcRuleViolation(String ruleNumber, Header header) {
            this.ruleNumber = ruleNumber;
            this.header = header;
            this.errors = new ArrayList();
        }
        private void addError(DrcError error) {
            errors.add(error);
        }
    }

    private static class DrcError {
        private final Cell cell;
        private final List shapes;              // list of shapes

        private DrcError(Cell cell) {
            this.cell = cell;
            this.shapes = new ArrayList();
        }
        private void addShape(Shape shape) {
            shapes.add(shape);
        }
    }

    private static class Header {
        private final int currentDrcResultsCount;
        private final int originalDrcResultsCount;
        private final int headerLength;         // does not include headerStart line
        private String ruleFilePath;
        private String ruleFileTitle;
        private StringBuffer comment;

        public static Header createHeader(String headerStart, int lineno) {
            StringTokenizer tokenizer = new StringTokenizer(headerStart);
            Header header = null;
            try {
                String cur = tokenizer.nextToken();
                String orig = tokenizer.nextToken();
                String len = tokenizer.nextToken();
                int icur = Integer.parseInt(cur);
                int iorig = Integer.parseInt(orig);
                int ilen = Integer.parseInt(len);
                header = new Header(icur, iorig, ilen);
            } catch (NoSuchElementException e) {
                System.out.println("Error parsing header start line, expected three integers on line number "+lineno+": "+headerStart);
                return null;
            } catch (NumberFormatException e) {
                System.out.println("Error converting count strings to integers on header start line, line number "+lineno+": "+headerStart);
                return null;
            }
            return header;
        }

        private Header(int currentDrcResultsCount, int originalDrcResultsCount, int headerLength) {
            this.currentDrcResultsCount = currentDrcResultsCount;
            this.originalDrcResultsCount = originalDrcResultsCount;
            this.headerLength = headerLength;
            comment = new StringBuffer();
        }

        public void addHeaderLine(String line) {
            if (line.startsWith("Rule File Pathname")) {
                ruleFilePath = line;
            } else if (line.startsWith("Rule File Title")) {
                ruleFileTitle = line;
            } else {
                if (comment.length() != 0) {
                    // already a line added
                    comment.append("\n");
                    comment.append(line);
                }
            }
        }
    }

    // ---------------------------------------------------------

    private String readLine() throws IOException {
        return readLine(true);
    }
    private String readLine(boolean errorOnEOF) throws IOException {
        // if in is null we ignore
        if (in == null) return null;

        String line = in.readLine();
        if (line == null && errorOnEOF) {
            System.out.println("Unexpected End of File!");
            in = null;          // ignore rest of readLine requests
            return null;
        }
        lineno++;
        return line;
    }
}
