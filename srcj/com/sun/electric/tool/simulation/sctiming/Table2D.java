/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Table2D.java
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

import java.util.Map;
import java.util.HashMap;

/**
 * Table2D stores data in a more intiuitive fashion than TableData for results
 * that are a function of two parameters being swept in the spice run.
 * The row and col indices are these two parameters. Because there
 * are several other measures, each 2D-array of data is assigned a key,
 * which is the name of the measure.  Thus a Table2D is really a 3D table,
 * but it is represented as a set of 2D tables.
 * <P>
 * Note that this table converts all keys to lower case to deal with
 * Spice, which converts everything to lower case.
 * User: gainsley
 * Date: Nov 16, 2006
 */
public class Table2D {

    private double [] rowVals;
    private double [] colVals;
    private String rowName;
    private String colName;
    private Map<String,double [][]> valuesMap;

    /**
     * Create a new Table2D.
     * Note: size is fixed once created.
     * @param rowVals row index values
     * @param rowName name of the row index param
     * @param colVals column index values
     * @param colName name of the column index param
     */
    public Table2D(double [] rowVals, String rowName, double [] colVals, String colName) {
        this.rowVals = rowVals;
        this.colVals = colVals;
        this.rowName = rowName.toLowerCase();
        this.colName = colName.toLowerCase();
        //Arrays.sort(this.rowVals);
        //Arrays.sort(this.colVals);
        valuesMap = new HashMap<String,double[][]>();
    }

    /**
     * Get the name of the row header name
     * @return name of the row header
     */
    public String getRowName() { return rowName; }

    /**
     * Get the name of the column header name
     * @return name of the column header
     */
    public String getColName() { return colName; }

    /**
     * Get the number of rows in the table
     * @return number of rows in the table
     */
    public int getNumRows() { return rowVals.length; }

    /**
     * Get the number of columns in the table
     * @return number of columns in the table
     */
    public int getNumCols() { return colVals.length; }

    /**
     * Get the index values for the columns
     * @return index values for the columns
     */
    public double [] getColIndexVals() { return colVals; }

    /**
     * Get the index values for the rows
     * @return index values for the rows
     */
    public double [] getRowIndexVals() { return rowVals; }

    /**
     * Set the value for a given row and column value. Table2D can
     * hold multiple sets of values for the same row and column values,
     * so a key is used to access a particular set of 2D data.
     * @param rowVal the row index value
     * @param colVal the column index value
     * @param dataKey the key for the data set
     * @param value the value at the row and column value
     */
    public void setValue(double rowVal, double colVal, String dataKey, double value) {
        dataKey = dataKey.toLowerCase();
        double [][] values = valuesMap.get(dataKey);
        if (values == null) {
            values = new double[rowVals.length][colVals.length];
            valuesMap.put(dataKey, values);
        }

        int i1, i2;
        for (i1=0; i1<rowVals.length; i1++) {
            if (rowVals[i1] == rowVal) break;
        }
        if (i1 == rowVals.length) {
            System.out.println("Cannot find value "+rowVal +" in row values");
            return;
        }
        for (i2=0; i2<colVals.length; i2++) {
            if (colVals[i2] == colVal) break;
        }
        if (i2 == colVals.length) {
            System.out.println("Cannot find value "+colVal +" in column values");
            return;
        }

        values[i1][i2] = value;
    }

    /**
     * Get the value for a given row and column value. Table2D can
     * hold multiple sets of values for the same row and column values, so
     * a key is used to access a particular set of 2D data.
     * @param rowVal the row index value
     * @param colVal the column index value
     * @param dataKey the key for the data set
     * @return the value at the row and column value
     */
    public double getValue(double rowVal, double colVal, String dataKey) {
        dataKey = dataKey.toLowerCase();
        double [][] values = valuesMap.get(dataKey);
        if (values == null) {
            System.out.println("Data key "+dataKey+" not found");
            return Double.MIN_VALUE;
        }

        int i1, i2;
        for (i1=0; i1<rowVals.length; i1++) {
            if (rowVals[i1] == rowVal) break;
        }
        if (i1 == rowVals.length) {
            System.out.println("Cannot find value "+rowVal +" in row values");
            return Double.MIN_VALUE;
        }
        for (i2=0; i2<colVals.length; i2++) {
            if (colVals[i2] == colVal) break;
        }
        if (i2 == colVals.length) {
            System.out.println("Cannot find value "+colVal +" in column values");
            return Double.MIN_VALUE;
        }

        return values[i1][i2];
    }

    /**
     * Set the data for a given data key.  The Data must size rows x cols.
     * This will replace any previous data stored under that key.
     * @param dataKey the data key
     * @param data the data
     */
    public void setData(String dataKey, double [][] data) {
        dataKey = dataKey.toLowerCase();
        if (data.length != rowVals.length) return;
        for (int i=0; i<data.length; i++) {
            if (data[i].length != colVals.length) return;
        }
        valuesMap.put(dataKey, data);
    }

    /**
     * Get the values in a column for a specific set of data.
     * @param key denotes the set of data to use
     * @param colIndex which column to get data
     * @return an array of values, the 0th index being from row 0,
     * and the Nth index from row N.
     */
    public double [] getColumnValues(String key, int colIndex) {
        key = key.toLowerCase();
        double [][] values = valuesMap.get(key);
        if (values == null) return null;
        double [] col = new double[values.length];
        for (int i=0; i<values.length; i++) {
            col[i] = values[i][colIndex];
        }
        return col;
    }

    /**
     * Get the values in a row for a specific set of data.
     * @param key denotes the set of data to use
     * @param rowIndex which row to get
     * @return an array of values, the 0th index being from col 0,
     * and the Nth index from col N.
     */
    public double [] getRowValues(String key, int rowIndex) {
        key = key.toLowerCase();
        double [][] values = valuesMap.get(key);
        if (values == null) return null;
        return values[rowIndex];
    }

    /**
     * Get the average (mean) of a set of values
     * @param vals the values
     * @return the mean
     */
    public static double getAverage(double [] vals) {
        double total = 0;
        for (int i=0; i<vals.length; i++) {
            total += vals[i];
        }
        return total/vals.length;
    }

    /**
     * Get the standard deviation of a set of values
     * @param vals the values
     * @return the mean
     */
    public static double getStandardDeviation(double [] vals) {
        double avg = getAverage(vals);
        double total = 0;
        for (int i=0; i<vals.length; i++) {
            double diff = avg - vals[i];
            total += diff*diff;
        }
        return Math.sqrt(total/vals.length);
    }

    /**
     * Get the average (mean) of all values
     * @param vals the values
     * @return the mean
     */
    public static double getAverage(double [][] vals) {
        double total = 0;
        int count = 0;
        for (int i=0; i<vals.length; i++) {
            double [] row = vals[i];
            for (int j=0; j<row.length; j++) {
                total += row[j];
                count++;
            }
        }
        return total/count;
    }

    /**
     * Get the standard deviation of a set of values
     * @param vals the values
     * @return the mean
     */
    public static double getStandardDeviation(double [][] vals) {
        double avg = getAverage(vals);
        double total = 0;
        int count = 0;
        for (int i=0; i<vals.length; i++) {
            double [] row = vals[i];
            for (int j=0; j<row.length; j++) {
                double diff = avg - row[j];
                total += diff*diff;
                count++;
            }
        }
        return Math.sqrt(total/count);
    }

    /**
     * Get the average (mean) value for a particular column.
     * Returns zero if no such data for the key is found.
     * @param key the data key
     * @param colIndex the column index (not column value)
     * @return the mean
     */
    public double getAvgColumnValue(String key, int colIndex) {
        key = key.toLowerCase();
        double [][] values = valuesMap.get(key);
        if (values == null) return 0;
        double total = 0;
        for (int i=0; i<values.length; i++) {
            double [] row = values[i];
            total += row[colIndex];
        }
        return total/values.length;
    }

    /**
     * Get the average (mean) values for columns.
     * Returns zero in all entries if no such data for the key is found.
     * @param key the data key
     * @return the means
     */
    public double [] getAvgColumnValues(String key) {
        key = key.toLowerCase();
        double [] avgvals = new double[colVals.length];
        for (int i=0; i<colVals.length; i++) {
            avgvals[i] = getAvgColumnValue(key, i);
        }
        return avgvals;
    }

    /**
     * Get the average (mean) value for a particular row.
     * Returns zero if no such data for the key is found.
     * @param key the data key
     * @param rowIndex the row index (not row value)
     * @return the mean
     */
    public double getAvgRowValue(String key, int rowIndex) {
        key = key.toLowerCase();
        double [][] values = valuesMap.get(key);
        if (values == null) return 0;
        double [] row = values[rowIndex];
        return getAverage(row);
    }

    /**
     * Get the average (mean) values for rows.
     * Returns zero in all entries if no such data for the key is found.
     * @param key the data key
     * @return the means
     */
    public double [] getAvgRowValues(String key) {
        key = key.toLowerCase();
        double [] avgvals = new double[rowVals.length];
        for (int i=0; i<rowVals.length; i++) {
            avgvals[i] = getAvgRowValue(key, i);
        }
        return avgvals;
    }

    /**
     * Get values[rows][cols] for the specified key
     * @param key which data set
     * @return values array
     */
    public double [][] getValues(String key) {
        key = key.toLowerCase();
        return valuesMap.get(key);
    }

    /**
     * Print to System.out a nicely formatted ASCII table
     * of all data in this Table2D.
     */
    public void print() {
        for (String s : valuesMap.keySet())
            print(s);
    }

    /**
     * Print a nicely formatted ASCII table of the data
     * for the given key
     * @param key the key
     */
    public void print(String key) {
        key = key.toLowerCase();
        System.out.println("---------------------------------------------------");
        System.out.println(key+":");
        System.out.println("---------------------------------------------------");
        double [][] values = valuesMap.get(key);
        if (values == null) return;

        System.out.println("\t"+colName);
        System.out.println(rowName);
        System.out.print("\t");
        for (int i=0; i<colVals.length; i++) {
            System.out.print(colVals[i]+"\t");
        }
        System.out.println();
        for (int i=0; i<rowVals.length; i++) {
            System.out.print(rowVals[i]+"\t");
            double [] vals = values[i];
            for (int i2=0; i2<colVals.length; i2++) {
                System.out.print(vals[i2]+"\t");
            }
            System.out.println();
        }
        System.out.println();
    }

}
