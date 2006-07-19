/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AssuraDrcErrors.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

package com.sun.electric.tool.drc;

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;

/**
 * Class to read DRC errors from Assura.
 */
public class AssuraDrcErrors {

    private static class DrcRuleViolation {
        private final int number;
        private final String desc;
        private final List<DrcError> errors;         // list of DrcErrors
        private int realErrorCount;
        private int flatErrorCount;
        private DrcRuleViolation(int number, String desc) {
            this.number = number;
            this.desc = desc;
            errors = new ArrayList<DrcError>();
        }
        private void addError(DrcError error) {
            errors.add(error);
        }
        public Iterator<DrcError> getErrors() {
            ArrayList<DrcError> copy = new ArrayList<DrcError>(errors);
            return copy.iterator();
        }
        private void setErrorCounts(int real, int flat) {
            realErrorCount = real;
            flatErrorCount = flat;
        }
    }

    private static class DrcError {
        private final Cell cell;
        private final Shape marker;
        private DrcError(Cell cell, Shape marker) {
            this.cell = cell;
            this.marker = marker;
        }
        public Cell getCell() { return cell; }
        public Shape getMarker() { return marker; }
    }

	/**
	 * Method to import Assura DRC errors from a file.
	 * @param filename the file to read.
	 */
    public static void importErrors(String filename, HashMap<Cell,String> mangledNames, String type) {
        BufferedReader in;
        try {
            FileReader reader = new FileReader(filename);
            in = new BufferedReader(reader);
        } catch (IOException e) {
            System.out.println("Error importing "+type+" Errors: "+e.getMessage());
            return;
        }

        String line;
        DrcRuleViolation rule = null;
        DrcError error = null;
        Cell cell = null;
        ErrorLogger logger = ErrorLogger.newInstance("Assura "+type+" Errors");
        int count = 0;
        int num = 1;
        try {
            while ((line = in.readLine()) != null) {
                String [] strings = line.split("\\s+");
                if (strings.length == 0) continue;
                if (strings[0].equals("Rule") && strings.length > 4) {
                    int number = Integer.valueOf(strings[2]).intValue();
                    int remove = 9 + strings[2].length() + 3;
                    rule = new DrcRuleViolation(number, line.substring(remove, line.length()));
                    continue;
                }

                if (rule == null) continue;

                if (strings[0].equals("Real")) {
                    int real = Integer.valueOf(strings[4].replaceAll(";", "")).intValue();
                    int flat = Integer.valueOf(strings[9]).intValue();
                    rule.setErrorCounts(real, flat);
                    logger.setGroupName(rule.number, "("+real+") "+rule.desc);
                    num = 1;
                    continue;
                }
                if (strings[0].equals("Cell")) {
                    cell = CalibreDrcErrors.getCell(strings[3], mangledNames);
                    if (cell == null) {
                        System.out.println("Couldn't find cell "+strings[3]+"{lay}");
                    }
                    continue;
                }

                if (cell == null) continue;

                double scale = cell.getTechnology().getScale();
                if (strings[0].equals("Shape")) {
                    // get rid of next 3 lines
                    line = in.readLine();
                    line = in.readLine();
                    line = in.readLine();
                    while (!line.startsWith("---") && !line.startsWith("===") ) {
                        line = in.readLine();
                        if (line == null) break;
                        strings = line.split("\\s+");
                        if (strings.length < 6) continue;  // skip empty lines
                        int last = strings.length-1;
                        double x1 = Double.parseDouble(strings[last-3]) / scale * 1000;
                        double y1 = Double.parseDouble(strings[last-2]) / scale * 1000;
                        double x2 = Double.parseDouble(strings[last-1]) / scale * 1000;
                        double y2 = Double.parseDouble(strings[last]) / scale * 1000;
                        Rectangle2D rect = new Rectangle2D.Double(x1, y1, x2-x1, y2-y1);
                        error = new DrcError(cell, rect);
                        List<EPoint> ptList = new ArrayList<EPoint>();
                        ptList.add(new EPoint(x1, y1));   ptList.add(new EPoint(x2, y1));
                        ptList.add(new EPoint(x1, y1));   ptList.add(new EPoint(x1, y2));
                        ptList.add(new EPoint(x2, y2));   ptList.add(new EPoint(x2, y1));
                        ptList.add(new EPoint(x2, y2));   ptList.add(new EPoint(x1, y2));
                        ptList.add(new EPoint(x1, y1));   ptList.add(new EPoint(x2, y2));
                        ptList.add(new EPoint(x1, y2));   ptList.add(new EPoint(x2, y1));
                        logger.logError(num+". "+cell.getName()+": "+rule.desc, null, null, ptList, null, null, cell, rule.number);
                        count++;
                        num++;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error importing "+type+" Errors: "+e.getMessage());
            return;
        }
        System.out.println("Imported "+count+" errors from file "+filename);
        if (count == 0) {
        	Job.getUserInterface().showInformationMessage("Imported Zero "+type+" Errors", type+" Import Complete");
        }
        logger.termLogging(true);
    }
}
