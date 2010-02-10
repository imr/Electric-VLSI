/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TableData.java
 * Written by Jonathan Gainsley, Sun Microsystems.
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation.sctiming;

import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StreamTokenizer;

/**
 * TableData provides storage for the measurement results read from
 * a spice mt0 file. Data is stored as it is encountered in the
 * mt0 file, with each row of the table corresponding to a row (sweep) in the
 * mt0 file. Columns correspond to a particular measurement.
 * User: gainsley
 * Date: Nov 16, 2006
 */
public class TableData {

    List<String> headers;
    List<double[]> rows;

    /**
     * Create a new TableData for storing spice measurement results.
     * Measurement names must be specified.
     * @param headers measurement names
     */
    public TableData(List<String> headers) {
        this.headers = headers;
        this.rows = new ArrayList<double[]>();
    }

    /**
     * Add a row to the table. The number of entries in the row
     * must equal the number of headers declared for the table.
     * @param row row to add to the table
     */
    public void addRow(double [] row) {
        if (row.length != headers.size()) {
            System.out.println("Cannot add row of length "+row.length+" to table of length "+headers.size());
            return;
        }
        rows.add(row);
    }

    /**
     * Get the number of columns (number of headers) for the table.
     * @return number of columns
     */
    public int getNumCols() { return headers.size(); }

    /**
     * Get the number of rows in the table.
     * @return number of rows in the table.
     */
    public int getNumRows() { return rows.size(); }

    /**
     * Get the headers for the table.
     * @return headers for the table.
     */
    public List<String> getHeaders() { return headers; }

    /**
     * Get the data for the given row. Will throw
     * an IndexOutOfBoundsException if the row is out of bounds
     * @param row the row number
     * @return data for that row
     */
    public double[] getRow(int row) {
        return rows.get(row);
    }

    /**
     * Get the index of the given header in the headers list
     * @param headername the header name
     * @return index of the given header in the headers list,
     * or -1 if not found
     */
    public int getColumn(String headername) {
        if (headername == null) return -1;
        for (int i=0; i<headers.size(); i++) {
            if (headers.get(i).equalsIgnoreCase(headername)) return i;
        }
        return -1;
    }

    /**
     * Get the value in a given row and column (header)
     * @param row the row
     * @param header the column header
     * @return the value, or Double.NaN if either row or column is out of bounds
     */
    public double getValue(int row, String header) {
        int col = getColumn(header);
        if (col < 0) return Double.NaN;
        if (row < 0 || row >= rows.size()) return Double.NaN;
        return (rows.get(row))[col];
    }

    /**
     * Print the data in this TableData as a pretty ASCII table.
     */
    public void printData() {
        int colwidth = 12;
        for (String s : headers) {
            if (s.length() > colwidth) colwidth = s.length() + 1;
        }
        StringBuffer line = new StringBuffer();
        for (String s : headers) {
            line.append(s);
            for (int i=s.length(); i<colwidth; i++) line.append(" ");
        }
        System.out.println(line);
        for (double [] row : rows) {
            line = new StringBuffer();
            for (int i=0; i<row.length; i++) {
                String s = Double.toString(row[i]);
                line.append(s);
                for (int j=s.length(); j<colwidth; j++) line.append(" ");
            }
            System.out.println(line);
        }
    }

    /**
     * Get the a Table2D object containing the data in this TableData.
     * Most data for characterization varies with respect to
     * one or two parameters being swept. Thus, it is more
     * intuitive to store that data where the rows and cols
     * are two of the parameters from this table.
     * @param index1 the name of the parameter/measure to be the row index
     * @param index2 the name of the parameter/measure to be the column index
     * @param index3 a third index that can be used to mask out averaging (see index3MaskValues)
     * @param index3ExcludeValues if the size of the table is greater than the unique values
     * of index1 * index2, getTable2D normally averages the extra values. To selectively
     * choose which values to exclude from this averaging, put those values in this array.
     * If null, all values will be averaged.
     * @return a Table2D object with the same data
     */
    public Table2D getTable2D(String index1, String index2, String index3, String index3ExcludeValues) {
        int i1 = getColumn(index1);
        int i2 = getColumn(index2);
        int i3 = getColumn(index3);
        double [] excludeValues = getDoubleValues(index3ExcludeValues);
        if (i1 < 0 || i2 < 0) return null;
        List<Double> values1 = new ArrayList<Double>();
        List<Double> values2 = new ArrayList<Double>();
        for (double [] row : rows) {
            Double d1 = new Double(row[i1]);
            Double d2 = new Double(row[i2]);
            if (!values1.contains(d1)) values1.add(d1);
            if (!values2.contains(d2)) values2.add(d2);
        }
        double [] di1 = new double[values1.size()];
        double [] di2 = new double[values2.size()];
        int i=0;
        for (Iterator<Double> it = values1.iterator(); it.hasNext(); ) {
            di1[i] = it.next().doubleValue();
            i++;
        }
        i=0;
        for (Iterator<Double> it = values2.iterator(); it.hasNext(); ) {
            di2[i] = it.next().doubleValue();
            i++;
        }
        Table2D table = new Table2D(di1, index1, di2, index2);
        boolean getStdDev = false;
        if (di1.length * di2.length < getNumRows()) {
            System.out.println("Warning: Conversion of TableData to Table2D will average" +
                    " some values because the number of unique values of "+index1+" times "+index2+
                    " is smaller than the dimensions of the TableData");
            getStdDev = true;
        }

        for (int j=0; j<headers.size(); j++) {
            String key = headers.get(j);
            if (key.equals(index1) || key.equals(index2)) continue;

            double [][] data = new double[di1.length][di2.length];
            int [][] count = new int[di1.length][di2.length];
            for (int x=0; x<di1.length; x++) {
                Arrays.fill(data[x], 0);
                Arrays.fill(count[x], 0);
            }
            for (double [] row : rows) {
                // get index
                double ii1 = row[i1];
                double ii2 = row[i2];

                // check if this value should be excluded
                boolean skipThisVal = false;
                if (i3 >= 0) {
                    double ii3 = row[i3];
                    for (double excludeval : excludeValues) {
                        if (excludeval == ii3) {
                            skipThisVal = true;
                            //System.out.println("Excluding value "+index3+"="+ii3+" from standard deviation of "+index3+" in table of "+index1+" vs "+index2);
                            break;
                        }
                    }
                }
                if (skipThisVal) continue;

                int r=0, c=0;
                for (r=0; r<di1.length; r++) {
                    if (di1[r] == ii1) break;
                }
                for (c=0; c<di2.length; c++) {
                    if (di2[c] == ii2) break;
                }
                data[r][c] += row[j];
                count[r][c]++;
            }
            for (int r=0; r<di1.length; r++) {
                for (int c=0; c<di2.length; c++) {
                    if (count[r][c] > 1) {
                        data[r][c] /= count[r][c];
                    }
                }
            }
            table.setData(key, data);
            if (getStdDev) {
                // we averaged the values, let's get the std dev
                double [][] stddev = new double[di1.length][di2.length];
                for (int x=0; x<di1.length; x++) {
                    Arrays.fill(stddev[x], 0);
                }
                for (double [] row : rows) {
                    // get index
                    double ii1 = row[i1];
                    double ii2 = row[i2];

                    // check if this value should be excluded
                    boolean skipThisVal = false;
                    if (i3 >= 0) {
                        double ii3 = row[i3];
                        for (double excludeval : excludeValues) {
                            if (excludeval == ii3) {
                                skipThisVal = true;
                                //System.out.println("Excluding value "+index3+"="+ii3+" from standard deviation of "+index3+" in table of "+index1+" vs "+index2);
                                break;
                            }
                        }
                    }
                    if (skipThisVal) continue;

                    int r=0, c=0;
                    for (r=0; r<di1.length; r++) {
                        if (di1[r] == ii1) break;
                    }
                    for (c=0; c<di2.length; c++) {
                        if (di2[c] == ii2) break;
                    }
                    double diff = data[r][c] - row[j]; // avg - value
                    stddev[r][c] += diff*diff; // running total of diff^2
                }
                int thirdD = getNumRows() / di1.length / di2.length;
                for (int r=0; r<di1.length; r++) {
                    for (int c=0; c<di2.length; c++) {
                        stddev[r][c] = Math.sqrt(stddev[r][c]/thirdD); // sqrt(sum of diffs / num diffs)
                    }
                }
                table.setData(key+"_stddev", stddev);
            }
        }
        return table;
    }

    /**
     * Get the average (mean) value of the data in a given column (header)
     * @param column the column
     * @return the mean
     */
    public double getAverage(int column) {
        if (column < 0 || column >= headers.size()) return 0;
        double total = 0;
        for (double[] row : rows) {
            total += row[column];
        }
        return (total/rows.size());
    }

    private double [] getDoubleValues(String list) {
        if (list == null) return new double [] {};
        String [] svals = list.split("\\s+");
        double [] dvals = new double[svals.length];
        Arrays.fill(dvals, -1);
        for (int i=0; i<svals.length; i++) {
            try {
                dvals[i] = Double.valueOf(svals[i]);
            } catch (NumberFormatException e) {
                return new double [] {};
            }
        }
        return dvals;
    }

    /**
     * Parse spice measurement results from the given mt0 file
     * and put them in a TableData object.
     * @param filename the spice mt0 file (path and extension)
     * @return the measurements in a TableData object
     * @throws SCTimingException
     */
    public static TableData readSpiceMeasResults(String filename) throws SCTimingException {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(filename));
        } catch (java.io.FileNotFoundException e) {
            throw new SCTimingException(e.getMessage());
        }
        try {
            int ttype;
            List<String> headers = new ArrayList<String>();
            StreamTokenizer s = new StreamTokenizer(reader);
            s.resetSyntax();
            s.wordChars(0, 255);
            s.whitespaceChars(' ', ' ');
            s.whitespaceChars('\t', '\t');
            s.whitespaceChars('\r', '\r');
            s.whitespaceChars('\n', '\n');
            s.whitespaceChars('\f', '\f');
            s.quoteChar('\'');
            s.quoteChar('"');
            s.eolIsSignificant(true);
            boolean title = false;
            // find start of header. it is after title line
            while ((ttype = s.nextToken()) != StreamTokenizer.TT_EOF) {
                if (!title && ttype == StreamTokenizer.TT_WORD && s.sval.equalsIgnoreCase(".TITLE")) {
                    title = true; continue;
                }
                if (title && ttype == StreamTokenizer.TT_EOL)
                    break;
            }
            // this is start of indices
            s.eolIsSignificant(false);
            while ((ttype = s.nextToken()) != StreamTokenizer.TT_EOF) {
                if (ttype != StreamTokenizer.TT_WORD)
                    throw new SCTimingException("Expected string, but got "+s.toString()+" while parsing index data of "+filename);
                headers.add(s.sval);
                if (s.sval.equals("alter#")) break;
            }
            TableData data = new TableData(headers);
            int i=0;
            double [] row = new double[headers.size()];
            while ((ttype = s.nextToken()) != StreamTokenizer.TT_EOF) {
                if (i == 0) {
                    row = new double[headers.size()];
                }
                if (ttype != StreamTokenizer.TT_WORD)
                    throw new SCTimingException("Expected value, but got '"+s.toString()+"' while parsing index data of "+filename);
                try {
                    row[i] = Double.parseDouble(s.sval);
                } catch (NumberFormatException e) {
                    if (s.sval.equalsIgnoreCase("failed")) {
                        row[i] = Double.NaN;
                    } else {
                        throw new SCTimingException("Expected numeric value, but got '"+s.toString()+"' while parsing spice measurement file "+filename);
                    }
                }
                i++;
                if (i == headers.size()) {
                    data.addRow(row);
                    i = 0;
                }
            }
            reader.close();
            return data;
        } catch (java.io.IOException e) {
            throw new SCTimingException(e.getMessage());
        }
    }

}
