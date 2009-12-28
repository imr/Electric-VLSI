package com.sun.electric.tool.drc;

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads RDB error database, typically used for reporting antenna rule
 * violations. A read of a new file accumulates to the same error report,
 * as typically multiple RDB databases are generated for a single
 * antenna rule drc run.
 */
public class CalibreRdbErrors {
    private int scale;
    private String topCellName;
    private Cell topCell;
    private ErrorLogger logger;
    private BufferedReader in;
    private int lineno;
    private Map<Cell,String> mangledNames;
    private int errorCount;

    private static final String spaces = "[\\s\\t ]+";

    /**
     * Create a new error object to read errors in from file
     * This creates a new error logger instance to add errors to,
     * but you must call {@link CalibreRdbErrors#termLogging(boolean)}
     * to display the errors in the error log.
     */
    public CalibreRdbErrors() {
        logger = ErrorLogger.newInstance("Calibre Antenna DRC Errors");
        errorCount = 0;
    }

    /**
     * Import errors from an RDB database to the current logger
     * @param filename the filename to import errors from
     * @param mangledNames mangled GDS cell names
     */
    public void importErrors(String filename, Map<Cell,String> mangledNames) {
        lineno = 1;
        this.mangledNames = mangledNames;
        try {
            System.out.println("Reading RDB file "+filename);
            FileReader reader = new FileReader(filename);
            in = new BufferedReader(reader);

            // read header
            String line = in.readLine();
            if (line == null) return;
            String [] parts = line.trim().split(spaces);
            if (parts.length != 2) {
                System.out.println("Error: Invalid header in RDB file at line "+lineno+": "+line);
                return;
            }
            topCellName = parts[0];
            topCell = CalibreDrcErrors.getCell(topCellName, mangledNames);
            if (topCell == null) {
                System.out.println("Error: Cell '"+topCellName+"' specified in file does not exist in current hierarchy: "+filename);
                return;
            }
            try {
                scale = Integer.valueOf(parts[1]).intValue();
            } catch (NumberFormatException e) {
                System.out.println("Warning: Invalid scale value "+parts[1]+" at line "+lineno);
                scale = 1000;
            }
            lineno++;
            boolean more = true;
            while (more) {
                more = readRuleViolation();
            }
            in.close();
        } catch (IOException e) {
            System.out.println("Error importing RDB database: "+e.getMessage());
            return;
        }
    }

    /**
     * Finish reading errors and display error logger
     * @param explain true to pop up display to user
     */
    public void termLogging(boolean explain) {
        logger.termLogging(explain);
        Job.getUserInterface().showInformationMessage("Calibre Antenna DRC Imported "+errorCount+" errors", "Calibre Antenna DRC");
    }

    /**
     * Read a violation from the current opened database file
     * @return false if nothing left to read
     * @throws IOException on read exception
     */
    private boolean readRuleViolation() throws IOException {
        if (in == null) return false;
        double lambdaScale = topCell.getTechnology().getScale() / 1000;

        // first line is rule_name::some value
        String line = in.readLine();
        if (line == null) return false;
        int ruleEnd = line.indexOf("::");
        if (ruleEnd < 0) {
            System.out.println("Error: expected :: in rule name line at "+lineno+": "+line);
            return false;
        }
        String rule = line.substring(0, ruleEnd);
        lineno++;

        // second line is date, ignore
        line = in.readLine();
        if (line == null) {
            System.out.println("Error: premature end of file at line "+lineno);
            return false;
        }
        lineno++;

        // third line is rule check code
        line = in.readLine();
        if (line == null) {
            System.out.println("Error: premature end of file at line "+lineno);
            return false;
        }
        String ruleText = line;
        boolean ruleTextAnnotatedWithAR = false;
        lineno++;

        // from now on, line is either a list of name=value pairs,
        // or it is a polygon (p) or edge (e) geometry list
        List<EPoint> lineList = new ArrayList<EPoint>();
        List<PolyBase> polyList = new ArrayList<PolyBase>();
        List<Property> properties = new ArrayList<Property>();
        StringBuffer propLines = new StringBuffer();
        Cell cell = null;

        while (true) {
            in.mark(200);
            line = in.readLine();
            if (line == null) break;
            if (line.startsWith(rule)) {
                in.reset();
                break; // next error found, report this error and return true
            }
            if (line.contains("=")) {
                String parts [] = line.trim().split(spaces);
                String commonprefix = "";
                int i=0;
                if (!parts[0].contains("=")) {
                    // this is a common prefix to all names in the subsequent name=value pairs
                    commonprefix = parts[0]+" ";
                    i++;
                }
                for (; i<parts.length; i++) {
                    String [] namevalue = parts[i].split("=");
                    if (namevalue.length != 2) continue;
                    properties.add(new Property(commonprefix+namevalue[0], namevalue[1]));
/*
                    if (namevalue[0].equalsIgnoreCase("cell")) {
                        cell = CalibreDrcErrors.getCell(namevalue[1], mangledNames);
                        if (cell == null) continue;
                        lambdaScale = cell.getTechnology().getScale() / 1000;
                    }
*/
                }
                propLines.append(line);
                propLines.append("\n");
                lineno++;
            }
            else if (line.startsWith("p")) {
                String [] parts = line.trim().split(spaces);
                if (parts.length != 3) {
                    System.out.println("Error parsing polygon list at line"+lineno+": "+line);
                    return false;
                }
                int count = 0;
                try {
                    count = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    System.out.println("Error: expected number as third argument in polygon at line "+lineno+": "+parts[2]);
                    return false;
                }
                Point2D[] points = new Point2D[count];
                for (int i=0; i<count; i++) {
                    line = in.readLine();
                    if (line == null) {
                        System.out.println("Error: premature end of file at line "+lineno);
                        return false;
                    }
                    if (line.startsWith("AR")) {
                        i=-1; // reset, AR line seems to be new thing for newer version of Calibre.
                        if (!ruleTextAnnotatedWithAR) {
                            ruleText = line+"; "+ruleText;
                            ruleTextAnnotatedWithAR = true;
                        }
                        continue;
                    }
                    lineno++;
                    String [] coords = line.trim().split(spaces);
                    if (coords.length != 2) {
                        System.out.println("Error: expected two fields for poly coord at "+lineno+": "+line);
                        return false;
                    }
                    double x=0, y=0;
                    try {
                        x = Double.parseDouble(coords[0])/scale/lambdaScale;
                        y = Double.parseDouble(coords[1])/scale/lambdaScale;
                    } catch (NumberFormatException e) {
                        System.out.println("Error: polygon coordinates should be numbers at line "+lineno+": "+line);
                        return false;
                    }
                    points[i] = new Point2D.Double(x, y);
                }
                polyList.add(new PolyBase(points));
            }
            else if (line.startsWith("e")) {
                System.out.println("Error: edge spec not supported yet, didn't have example. Please tell JKG. Thanks");
                return false;
            }
        }

        // Log error
        StringBuffer props = new StringBuffer();
        for (Property prop : properties) {
            props.append(prop.toString());
        }
        if (cell == null) cell = topCell;
        logger.logMessageWithLines(ruleText+"\n"+propLines.toString(), polyList, lineList, cell, 0, true);
        errorCount++;
        return true;
    }

    private static class Property {
        private final String name;
        private final String value;
        private Property(String name, String value) {
            this.name = name;
            this.value = value;
        }
        public String toString() {
            return name+"="+value;
        }
    }
}
