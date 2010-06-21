/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RawSpiceOut.java
 * Input/output tool: reader for Raw Spice output (.raw)
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.simulation.Analysis;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.simulation.Analysis;
import com.sun.electric.tool.simulation.ScalarSample;
import com.sun.electric.tool.simulation.Stimuli;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.net.URL;

/**
 * Class for reading and displaying waveforms from Raw Spice output
 * (including LTSpice).  These are contained in .raw files.
 */
public class RawSpiceOut extends Input<Stimuli> {

	RawSpiceOut() {}

	private static final boolean DEBUG = false;

	private boolean complexValues;

	/**
	 * Class for handling swept signals.
	 */
	private static class SweepAnalysisLT extends Analysis
	{
		double [][] commonTime; // sweep, signal
		List<List<double[]>> theSweeps = new ArrayList<List<double[]>>(); // sweep, event, signal

		private SweepAnalysisLT(Stimuli sd, Analysis.AnalysisType type)
		{
			super(sd, type, false);
		}
	}

	/**
	 * Method to read an LTSpice output file.
	 */
	protected Stimuli processInput(URL fileURL, Cell cell)
		throws IOException
	{
        Stimuli sd = new Stimuli();

		// open the file
		if (openBinaryInput(fileURL)) return sd;

		// show progress reading .raw file
		startProgressDialog("LTSpice output", fileURL.getFile());

		// read the actual signal data from the .raw file
		readRawLTSpiceFile(cell, sd);

		// stop progress dialog, close the file
		stopProgressDialog();
		closeInput();
        return sd;
	}

	private void readRawLTSpiceFile(Cell cell, Stimuli sd)
		throws IOException
	{
		complexValues = false;
		boolean realValues = false;
		int signalCount = -1;
		String[] signalNames = null;
		int rowCount = -1;
		Analysis.AnalysisType aType = Analysis.ANALYSIS_TRANS;
        boolean isLTSpice = false;
		boolean first = true;

		sd.setCell(cell);
		Analysis an = null;

		double[][] values = null;
        double[] time = null;
		for(;;) {
			String line = getLineFromBinary();
			if (line == null) break;
			updateProgressDialog(line.length());

			// make sure this isn't an HSPICE deck (check first line)
			if (first) {
				first = false;
				if (line.length() >= 20) {
					String hsFormat = line.substring(16, 20);
					if (hsFormat.equals("9007") || hsFormat.equals("9601")) {
						System.out.println("This is an HSPICE file, not a RAWFILE file");
						System.out.println("Change the SPICE format (in Preferences) and reread");
						return;
					}
				}
			}

			// find the ":" separator
			int colonPos = line.indexOf(':');
			if (colonPos < 0) continue;
			String keyWord = line.substring(0, colonPos);
			String restOfLine = line.substring(colonPos+1).trim();

            if (keyWord.equals("Command")) {
                isLTSpice = restOfLine.indexOf("LTspice")!=-1;
                continue;
            }

			if (keyWord.equals("Plotname"))
                {
                    // see if known analysis is specified
                    // terminate any previous analysis
                    if (an != null)
                        {
                            signalCount = -1;
                            rowCount = -1;
                            signalNames = null;
                            values = null;
                        }

                    // start reading a new analysis
                    if (restOfLine.startsWith("Transient Analysis")) {
                        an = new Analysis(sd, Analysis.ANALYSIS_TRANS, false);
                    } else if (restOfLine.startsWith("DC ")) {
                        an = new Analysis(sd, Analysis.ANALYSIS_DC, false);
                    } else if (restOfLine.startsWith("AC ")) {
                        an = new Analysis(sd, Analysis.ANALYSIS_AC, false);
                        aType = Analysis.ANALYSIS_AC;
                    } else {
                        System.out.println("Warning: unknown analysis: " + restOfLine);
                        an = new Analysis(sd, Analysis.ANALYSIS_TRANS, false);
                    }
                    continue;
                }

			if (keyWord.equals("Flags"))
                {
                    // the first signal is Time
                    int complex = restOfLine.indexOf("complex");
                    if (complex >= 0) complexValues = true;
                    int r = restOfLine.indexOf("real");
                    if (r >= 0) realValues = true;
                    continue;
                }

			if (keyWord.equals("No. Variables"))
                {
                    // the first signal is Time
                    signalCount = TextUtils.atoi(restOfLine) - 1;
                    continue;
                }

			if (keyWord.equals("No. Points"))
                {
                    rowCount = TextUtils.atoi(restOfLine);
                    time = new double[rowCount];
                    continue;
                }

            if (!isLTSpice) {
                if (keyWord.equals("Variables")) {
                    if (signalCount < 0) {
                        System.out.println("Missing variable count in file");
                        return;
                    }
                    signalNames = new String[signalCount];
                    values = new double[signalCount][rowCount];
                    for(int i=0; i<=signalCount; i++) {
                        if (restOfLine.length() > 0) {
                            line = restOfLine;
                            restOfLine = "";
                        } else {
                            line = getLineAndUpdateProgressBinary();
                            if (line == null) {
                                System.out.println("Error: end of file during signal names");
                                return;
                            }
                        }
                        line = line.trim();
                        int numberOnLine = TextUtils.atoi(line);
                        if (numberOnLine != i)
                            System.out.println("Warning: Variable " + i + " has number " + numberOnLine);
                        int spacePos = line.indexOf(" ");   if (spacePos < 0) spacePos = line.length();
                        int tabPos = line.indexOf("\t");    if (tabPos < 0) tabPos = line.length();
                        int pos = Math.min(spacePos, tabPos);
                        String name = line.substring(pos).trim();
                        spacePos = name.indexOf(" ");   if (spacePos < 0) spacePos = name.length();
                        tabPos = name.indexOf("\t");    if (tabPos < 0) tabPos = name.length();
                        pos = Math.min(spacePos, tabPos);
                        name = name.substring(0, pos);
                        if (i == 0) {
                            if (!name.equals("time") && an.getAnalysisType() == Analysis.ANALYSIS_TRANS)
                                System.out.println("Warning: the first variable should be time, is '" + name + "'");
                        } else {
                            signalNames[i-1] = name;
                        }
                    }
                    continue;
                }
                if (keyWord.equals("Values")) {
                    if (signalCount < 0) {
                        System.out.println("Missing variable count in file");
                        return;
                    }
                    if (rowCount < 0) {
                        System.out.println("Missing point count in file");
                        return;
                    }
                    for(int j=0; j<rowCount; j++) {
                        for(int i = -1; i <= signalCount; ) {
                            line = getLineAndUpdateProgressBinary();
                            if (line == null) {
                                System.out.println("Error: end of file during data points (read " + j + " out of " + rowCount);
                                return;
                            }
                            line = line.trim();
                            if (line.length() == 0) continue;
                            int charPos = 0;
                            while (charPos <= line.length()) {
                                int tabPos = line.indexOf("\t", charPos);
                                if (tabPos < 0) tabPos = line.length();
                                String field = line.substring(charPos, tabPos);
                                charPos = tabPos+1;
                                while (charPos < line.length() && line.charAt(charPos) == '\t') charPos++;
                                if (i < 0) {
                                    int lineNumber = TextUtils.atoi(field);
                                    if (lineNumber != j)
                                        System.out.println("Warning: event " + j + " has wrong event number: " + lineNumber);
                                } else {
                                    double val = TextUtils.atof(field);
                                    if (i == 0)
                                        time[j] = val;
                                    else
                                        values[i-1][j] = val;
                                }
                                i++;
                                if (i > signalCount) break;
                            }
                        }
                    }
                    for (int i = 0; i < signalCount; i++)
                        ScalarSample.createSignal(an, signalNames[i], null, time, values[i]);
                    continue;
                }
                if (keyWord.equals("Binary")) {
                    if (signalCount < 0) {
                        System.out.println("Missing variable count in file");
                        return;
                    }
                    if (rowCount < 0) {
                        System.out.println("Missing point count in file");
                        return;
                    }

                    // read the data
                    for(int j=0; j<rowCount; j++) {
                        time[j] = dataInputStream.readDouble();
                        for(int i=0; i<signalCount; i++)
                            values[i][j] = dataInputStream.readDouble();
                    }
                    continue;
                }
            } else {
                if (keyWord.equals("Variables"))
                    {
                        if (signalCount < 0)
                            {
                                System.out.println("Missing variable count in file");
                                return;
                            }
                        signalNames = new String[signalCount];
                        for(int i=0; i<=signalCount; i++)
                            {
                                restOfLine = getLineFromBinary();
                                if (restOfLine == null) break;
                                updateProgressDialog(restOfLine.length());
                                restOfLine = restOfLine.trim();
                                int indexOnLine = TextUtils.atoi(restOfLine);
                                if (indexOnLine != i)
                                    System.out.println("Warning: Variable " + i + " has number " + indexOnLine);
                                int nameStart = 0;
                                while (nameStart < restOfLine.length() && !Character.isWhitespace(restOfLine.charAt(nameStart))) nameStart++;
                                while (nameStart < restOfLine.length() && Character.isWhitespace(restOfLine.charAt(nameStart))) nameStart++;
                                int nameEnd = nameStart;
                                while (nameEnd < restOfLine.length() && !Character.isWhitespace(restOfLine.charAt(nameEnd))) nameEnd++;
                                String name = restOfLine.substring(nameStart, nameEnd);
                                if (name.startsWith("V(") && name.endsWith(")"))
                                    {
                                        name = name.substring(2, name.length()-1);
                                    }
                                if (i > 0) signalNames[i-1] = name;
                            }
                        continue;
                    }

                if (keyWord.equals("Binary"))
                    {
                        if (signalCount < 0)
                            {
                                System.out.println("Missing variable count in file");
                                return;
                            }
                        if (rowCount < 0)
                            {
                                System.out.println("Missing point count in file");
                                return;
                            }

                        // initialize the stimuli object
                        SweepAnalysisLT lan = new SweepAnalysisLT(sd, aType);
                        sd.setCell(cell);
                        if (DEBUG)
                            {
                                System.out.println(signalCount+" VARIABLES, "+rowCount+" SAMPLES");
                                for(int i=0; i<signalCount; i++)
                                    System.out.println("VARIABLE "+i+" IS "+signalNames[i]);
                            }

                        // read all of the data in the RAW file
                        values = new double[signalCount][rowCount];
                        time = new double[rowCount];
                        for(int j=0; j<rowCount; j++)
                            {
                                double t = getNextDouble();
                                if (DEBUG) System.out.println("TIME AT "+j+" IS "+t);
                                time[j] = Math.abs(t);
                                for(int i=0; i<signalCount; i++)
                                    {
                                        double value = 0;
                                        if (realValues) value = getNextFloat(); else
                                            value = getNextDouble();
                                        if (DEBUG) System.out.println("   DATA POINT "+i+" ("+signalNames[i]+") IS "+value);
                                        values[i][j] = value;
                                    }
                            }

                        // figure out where the sweep breaks occur
                        List<Integer> sweepLengths = new ArrayList<Integer>();
                        //				int sweepLength = -1;
                        int sweepStart = 0;
                        int sweepCount = 0;
                        for(int j=1; j<=rowCount; j++)
                            {
                                if (j == rowCount || time[j] < time[j-1])
                                    {
                                        int sl = j - sweepStart;
                                        sweepLengths.add(new Integer(sl));
                                        sweepStart = j;
                                        sweepCount++;
                                        // XXX
                                        //lan.addSweep(Integer.toString(sweepCount));
                                    }
                            }
                        if (DEBUG) System.out.println("FOUND " + sweepCount + " SWEEPS");

                        // place data into the Analysis object
                        lan.commonTime = new double[sweepCount][];
                        int offset = 0;
                        for(int s=0; s<sweepCount; s++)
                            {
                                int sweepLength = sweepLengths.get(s).intValue();
                                lan.commonTime[s] = new double[sweepLength];
                                for (int j = 0; j < sweepLength; j++)
                                    lan.commonTime[s][j] = time[j + offset];

                                List<double[]> allTheData = new ArrayList<double[]>();
                                for(int i=0; i<signalCount; i++)
                                    {
                                        double[] oneSetOfData = new double[sweepLength];
                                        for(int j=0; j<sweepLength; j++)
                                            oneSetOfData[j] = values[i][j+offset];
                                        allTheData.add(oneSetOfData);
                                    }
                                lan.theSweeps.add(allTheData);
                                offset += sweepLength;
                            }

                        // add signal names to the analysis
                        for (int i = 0; i < signalCount; i++)
                            {
                                String name = signalNames[i];
                                int lastDotPos = name.lastIndexOf('.');
                                String context = null;
                                if (lastDotPos >= 0)
                                    {
                                        context = name.substring(0, lastDotPos);
                                        name = name.substring(lastDotPos + 1);
                                    }
                                double minTime = 0, maxTime = 0, minValues = 0, maxValues = 0;
                                ScalarSample.createSignal(lan, signalNames[i], context, time, values[i]);
                            }
                        return;
                    }
            }
        }
	}

	private double getNextDouble()
		throws IOException
	{
		// double values appear with reversed bytes
		long lt = dataInputStream.readLong();
		lt = Long.reverseBytes(lt);
		double t = Double.longBitsToDouble(lt);
		int amtRead = 8;

		// for complex plots, ignore imaginary part
		if (complexValues) { amtRead *= 2;   dataInputStream.readLong(); }

		updateProgressDialog(amtRead);
		return t;
	}

	private float getNextFloat()
		throws IOException
	{
		// float values appear with reversed bytes
		int lt = dataInputStream.readInt();
		lt = Integer.reverseBytes(lt);
		float t = Float.intBitsToFloat(lt);
		int amtRead = 4;

		// for complex plots, ignore imaginary part
		if (complexValues) { amtRead *= 2;   dataInputStream.readInt(); }

		updateProgressDialog(amtRead);
		return t;
	}

}
