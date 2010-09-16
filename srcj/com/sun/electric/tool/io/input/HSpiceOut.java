/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: HSpiceOut.java
 * Input/output tool: reader for HSpice output (tr, pa, ac, sw, mt)
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
import com.sun.electric.tool.simulation.ComplexSample;
import com.sun.electric.tool.simulation.MutableSignal;
import com.sun.electric.tool.simulation.ScalarSample;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.simulation.SignalCollection;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.simulation.SweptSample;
import com.sun.electric.util.TextUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Class for reading and displaying waveforms from HSpice output.
 * This includes transient information in .tr and .pa files (.pa0/.tr0, .pa1/.tr1, ...)
 * It also includes AC analysis in .ac files; DC analysis in .sw files;
 * and Measurements in .mt files.
 *
 * While trying to debug the condition count handling, these test cases were observed:
 * CASE   VERSION  ANALYSIS   NUMNOI   SWEEPCNT   CNDCNT   CONDITIONS
 *  H01    9007       TR         0         4        1      bma_w
 *  H02    9007       TR        36        19        1      sweepv
 *  H03    9007       TR         2        30        1      MONTE_CARLO
 *                    DC       258        30        1      MONTE_CARLO
 *  H04    9007       TR         2         7        1      TEMPERATURE
 *                    DC         0         0        0
 *                    AC         0         6        1      bigcap
 *  H05    9601       TR         0         0        0
 *  H06    9601       TR         0         0        0
 *  H07    9601       TR         2        25        3      data_tim, inbufstr, outloadstr (sweep header has 2 numbers)
 *  H08    9601       TR         4         3        2      ccdata, cc
 *                    AC         4         2        1      lpvar (***CRASHES***)
 *  H09    9601       TR         0         3        3      rdata, r, c (sweep header has 2 numbers)
 *                    AC         0         3        3      rdata, r, c (sweep header has 2 numbers)
 *  H10    9601       TR         0         4        8      rdata, r0, r1, r2, r3, r4, c0, c1 (sweep header has 7 numbers)
 *                    AC         0         4        8      rdata, r0, r1, r2, r3, r4, c0, c1 (sweep header has 7 numbers)
 */
public class HSpiceOut extends Input<Stimuli>
{
	private static final boolean DEBUGCONDITIONS = false;
	/** true if tr/ac/sw file is binary */						private boolean isTRACDCBinary;
	/** true if binary tr/ac/sw file has bytes swapped */		private boolean isTRACDCBinarySwapped;
	/** the raw file base */									private String fileBase;
	/** the "tr" files (X.tr0, X.tr1, ...): transient */		private List<String> trFiles;
	/** the "sw" file extension (sw0, sw1, ...): DC */			private String swExtension;
	/** the "ic" file extension (ic0, ic1, ...): old DC */		private String icExtension;
	/** the "ac" file extension (ac0, ac1, ...): AC */			private String acExtension;
	/** the "mt" file extension (mt0, mt1, ...): measurement */	private String mtExtension;
	/** the "pa" file extension (pa0, pa1, ...): long names */	private String paExtension;
	/** signal names in file (may be continued in next) */		private String [] signalNames;
	/** signals created (may span multiple files) */			private Signal<?>[][] allSignals;
	/** sweep names (may span multiple files) */				private String [] sweepNames;
	private int binaryTRACDCSize, binaryTRACDCPosition;
	private boolean eofReached;
	private byte [] binaryTRACDCBuffer;

	/**
	 * Class to hold HSpice name associations from the .paX file
	 */
	private static class PALine
	{
		int	number;
		String string;
	}

	HSpiceOut() {}

	/**
	 * Method to read HSpice output files.
     * @param sd Stimuli associated to the reading.
	 * @param fileURL the URL to one of the output files.
	 * @param cell the Cell associated with these HSpice output files.
	 */
	protected Stimuli processInput(URL fileURL, Cell cell, Stimuli sd)
		throws IOException
	{
		// find all ".tr" files
		trFiles = new ArrayList<String>();
		String fileNameNoPath = TextUtils.getFileNameWithoutExtension(TextUtils.URLtoString(fileURL), true) +
			".tr";
		String topDirName = TextUtils.getFilePath(fileURL);
		File topDir = new File(topDirName);
		String [] fileList = topDir.list();
		for(int i=0; i<fileList.length; i++)
		{
			if (!fileList[i].startsWith(fileNameNoPath)) continue;
			trFiles.add(topDirName + fileList[i]);
		}
		Collections.sort(trFiles, TextUtils.STRING_NUMBER_ORDER);
		if (trFiles.size() > 1)
			System.out.println("Reading " + trFiles.size() + " 'tr' files...");

		// figure out other file names
		swExtension = "sw0";
		icExtension = "ic0";
		acExtension = "ac0";
		mtExtension = "mt0";
		paExtension = "pa0";
		fileBase = fileURL.getFile();
		int dotPos = fileBase.lastIndexOf('.');
		if (dotPos > 0)
		{
			String extension = fileBase.substring(dotPos+1);
			fileBase = fileBase.substring(0, dotPos);
			if (extension.startsWith("sw") || extension.startsWith("ic") ||
				extension.startsWith("ac") || extension.startsWith("mt") || extension.startsWith("pa"))
			{
				swExtension = "sw" + extension.substring(2);
				icExtension = "ic" + extension.substring(2);
				acExtension = "ac" + extension.substring(2);
				mtExtension = "mt" + extension.substring(2);
				paExtension = "pa" + extension.substring(2);
			}
		}



		// the .pa file has name information
		List<PALine> paList = readPAFile(fileURL);

		// read Transient analysis data (.tr file)
		addTRData(sd, paList, fileURL);

		// read DC analysis data (.sw file)
		addDCData(sd, paList, fileURL);

		// read AC analysis data (.ac file)
		addACData(sd, paList, fileURL);

		// read measurement data (.mt file)
		addMeasurementData(sd, fileURL);

		// return the simulation data
		return sd;
	}

	/**
	 * Method to find the ".mt" file and read measurement data.
	 * @param sd the Stimuli to add this measurement data to.
	 * @param fileURL the URL to the ".tr" file.
	 * @throws IOException
	 */
	private void addMeasurementData(Stimuli sd, URL fileURL)
		throws IOException
	{
		// find the associated ".mt" name file
		URL mtURL = null;
		try
		{
			mtURL = new URL(fileURL.getProtocol(), fileURL.getHost(), fileURL.getPort(), fileBase + "." + mtExtension);
		} catch (java.net.MalformedURLException e)
		{
		}
		if (mtURL == null) return;
		if (!TextUtils.URLExists(mtURL)) return;
		if (openTextInput(mtURL)) return;
		System.out.println("Reading HSpice measurements file: " + mtURL.getFile());

		SignalCollection sc = Stimuli.newSignalCollection(sd, "MEASUREMENTS");
		List<String> measurementNames = new ArrayList<String>();
		HashMap<String,List<Double>> measurementData = new HashMap<String,List<Double>>();
		String lastLine = null;
		for(;;)
		{
			// get line from file
			String nextLine = lastLine;
			if (nextLine == null)
			{
				nextLine = lineReader.readLine();
				if (nextLine == null) break;
			}
			if (nextLine.startsWith("$") || nextLine.startsWith(".")) continue;
			String [] keywords = breakMTLine(nextLine, false);
			if (keywords.length == 0) break;

			// gather measurement names on the first time out
			if (measurementNames.size() == 0)
			{
				for(int i=0; i<keywords.length; i++)
					measurementNames.add(keywords[i]);
				for(;;)
				{
					lastLine = lineReader.readLine();
					if (lastLine == null) break;
					keywords = breakMTLine(lastLine, true);
					if (keywords.length == 0) { lastLine = null;   break; }
					if (TextUtils.isANumber(keywords[0])) break;
					for(int i=0; i<keywords.length; i++)
						if (keywords[i].length() > 0)
							measurementNames.add(keywords[i]);
				}
				for(String mName : measurementNames)
				{
					measurementData.put(mName, new ArrayList<Double>());
				}
				continue;
			}

			// get data values
			int index = 0;
			for(int i=0; i<keywords.length; i++)
			{
				if (keywords[i].length() == 0) continue;
				String mName = measurementNames.get(index++);
				List<Double> mData = measurementData.get(mName);
				mData.add(new Double(TextUtils.atof(keywords[i])));
			}
			for(;;)
			{
				if (index >= measurementNames.size()) break;
				lastLine = lineReader.readLine();
				if (lastLine == null) break;
				keywords = breakMTLine(lastLine, true);
				if (keywords.length == 0) break;
				for(int i=0; i<keywords.length; i++)
				{
					if (keywords[i].length() == 0) continue;
					String mName = measurementNames.get(index++);
					List<Double> mData = measurementData.get(mName);
					mData.add(new Double(TextUtils.atof(keywords[i])));
				}
			}
			lastLine = null;
			continue;
		}

		// convert this to a list of Measurements
		List<Double> argMeas = measurementData.get(measurementNames.get(0));
        double[] time = new double[argMeas.size()];
		for (int i = 0; i < argMeas.size(); i++)
            time[i] = argMeas.get(i).doubleValue();
		List<Signal<ScalarSample>> measData = new ArrayList<Signal<ScalarSample>>();
		for(String mName : measurementNames)
		{
			List<Double> mData = measurementData.get(mName);
			double[] values = new double[mData.size()];
			for(int i=0; i<mData.size(); i++) values[i] = mData.get(i).doubleValue();

			// special case with the "alter#" name...remove the "#"
			if (mName.equals("alter#")) mName = "alter";
			Signal<ScalarSample> as = ScalarSample.createSignal(sc, sd, mName, null, time, values);
			measData.add(as);
		}

		closeInput();
	}

	/**
	 * Method to parse a line from a measurement (.mt0) file.
	 * @param line the line from the file.
	 * @param continuation true if the line is supposed to be a continuation
	 * after the first line.
	 * @return an array of strings on the line (zero-length if at end).
	 */
	private String[] breakMTLine(String line, boolean continuation)
	{
		List<String> strings = new ArrayList<String>();
		for(int i=1; ; )
		{
			if (line.length() <= i+1) break;
			int end = i+17;
			if (end > line.length()) end = line.length();
			while (end < line.length() && line.charAt(end-1) != ' ') end++;
			String part = line.substring(i, end).trim();
//			if (i == 1)
//			{
//				// first token: make sure it is blank if a continuation
//				if (continuation && part.length() > 0) return new String[0];
//			}
			if (part.length() > 0) strings.add(part.trim());
			i = end;
		}
		int actualSize = strings.size();
		String [] retVal = new String[actualSize];
		for(int i=0; i<actualSize; i++) retVal[i] = strings.get(i);
		return retVal;
	}

	/**
	 * Method to find the ".tr" file and read transient data.
	 * @param sd the Stimuli to add this transient data to.
	 * @param fileURL the URL to the ".tr" file.
	 * @throws IOException
	 */
	private void addTRData(Stimuli sd, List<PALine> paList, URL fileURL)
		throws IOException
	{
		int numFiles = trFiles.size();
		for(int i=0; i<numFiles; i++)
		{
			// find the ".tr" name file
			String trFile = trFiles.get(i);
			URL trURL = null;
			try
			{
				trURL = new URL(fileURL.getProtocol(), fileURL.getHost(), fileURL.getPort(), trFile);
			} catch (java.net.MalformedURLException e) {}
			if (trURL == null) return;
			if (!TextUtils.URLExists(trURL)) return;

			// process the DC data
			readTRDCACFile(sd, trURL, paList, "TRANS SIGNALS", i, numFiles);
		}
	}

	/**
	 * Method to find the ".sw" file and read DC data.
	 * @param sd the Stimuli to add this DC data to.
	 * @param fileURL the URL to the ".tr" file.
	 * @throws IOException
	 */
	private void addDCData(Stimuli sd, List<PALine> paList, URL fileURL)
		throws IOException
	{
		// find the associated ".sw" name file
		URL swURL = null;
		try
		{
			swURL = new URL(fileURL.getProtocol(), fileURL.getHost(), fileURL.getPort(), fileBase + "." + swExtension);
		} catch (java.net.MalformedURLException e) {}
		if (swURL != null && TextUtils.URLExists(swURL))
		{
			// process the DC data
			readTRDCACFile(sd, swURL, paList, "DC SIGNALS", 0, 1);
			return;
		}

		// no associated ".sw" file, look for an ".ic" name file
		URL icURL = null;
		try
		{
			icURL = new URL(fileURL.getProtocol(), fileURL.getHost(), fileURL.getPort(), fileBase + "." + icExtension);
		} catch (java.net.MalformedURLException e) {}
		if (icURL != null && TextUtils.URLExists(icURL))
		{
			// can't process the DC data
			System.out.println("WARNING: Cannot read old DC format file (." + icExtension +
				")...must provide new format (." + swExtension + "): " + fileBase + "." + icExtension);
			return;
		}
	}

	/**
	 * Method to find the ".ac" file and read AC data.
	 * @param sd the Stimuli to add this AC data to.
	 * @param fileURL the URL to the ".tr" file.
	 * @throws IOException
	 */
	private void addACData(Stimuli sd, List<PALine> paList, URL fileURL)
		throws IOException
	{
		// find the associated ".ac" name file
		URL acURL = null;
		try
		{
			acURL = new URL(fileURL.getProtocol(), fileURL.getHost(), fileURL.getPort(), fileBase + "." + acExtension);
		} catch (java.net.MalformedURLException e) {}
		if (acURL == null) return;
		if (!TextUtils.URLExists(acURL)) return;

		// process the AC data
		readTRDCACFile(sd, acURL, paList, "AC SIGNALS", 0, 1);
	}

	/**
	 * Method to read the "pa" file with full symbol names.
	 * These files can end in "0", "1", "2",...
	 * @param fileURL the URL to the simulation output file
	 * @return a list of PALine objects that describe the name mapping file entries.
	 */
	private List<PALine> readPAFile(URL fileURL)
		throws IOException
	{
		// find the associated ".pa" name file
		URL paURL = null;
		try
		{
			paURL = new URL(fileURL.getProtocol(), fileURL.getHost(), fileURL.getPort(), fileBase + "." + paExtension);
		} catch (java.net.MalformedURLException e) {}
		if (paURL == null) return null;
		if (!TextUtils.URLExists(paURL)) return null;
		if (openTextInput(paURL)) return null;

		List<PALine> paList = new ArrayList<PALine>();
		for(;;)
		{
			// get line from file
			String nextLine = lineReader.readLine();
			if (nextLine == null) break;

			// break into number and name
			String trimLine = nextLine.trim();
			int spacePos = trimLine.indexOf(' ');
			if (spacePos > 0)
			{
				// save it in a PALine object
				PALine pl = new PALine();
				pl.number = TextUtils.atoi(trimLine, 0, 10);
				pl.string = removeLeadingX(trimLine.substring(spacePos+1).trim());
				paList.add(pl);
			}
		}
		closeInput();
		return paList;
	}

	private void readTRDCACFile(Stimuli sd, URL fileURL, List<PALine> paList, String analysisTitle, int fileNum, int numFiles)
		throws IOException
	{
		if (openBinaryInput(fileURL)) return;
		eofReached = false;
		resetBinaryTRACDCReader();

		SignalCollection sc;
		if (fileNum == 0)
		{
			sc = Stimuli.newSignalCollection(sd, analysisTitle);
		} else
		{
			sc = sd.findSignalCollection(analysisTitle);
			if (sc == null) return;
		}
		startProgressDialog("HSpice " + analysisTitle + " analysis", fileURL.getFile());
		if (fileNum == 0)
		{
			System.out.println("Reading HSpice " + analysisTitle + " file: " + fileURL.getFile());
		} else
		{
			System.out.println("Also reading file: " + fileURL.getFile());
		}

		// get number of nodes
		int nodcnt = getHSpiceInt();

		// switch to text reading if the file is text
		if (!isTRACDCBinary)
		{
			closeInput();
			if (openTextInput(fileURL)) return;
			eofReached = false;
			resetBinaryTRACDCReader();
			int nodcntTxt = getHSpiceInt();
			if (nodcnt != nodcntTxt)
				System.out.println("WARNING: Binary interpretation saw " + nodcnt + " nodes but text sees " + nodcntTxt);
		}

		// get number of special items
		int numnoi = getHSpiceInt();

		// get number of conditions
		int cndcnt = getHSpiceInt();

		/*
		 * Although this isn't documented anywhere, it appears that the 4th
		 * number in the file is a multiplier for the first, which allows
		 * there to be more than 10000 nodes.
		 */
		StringBuffer line = new StringBuffer();
		for(int j=0; j<4; j++) line.append((char)getByteFromFile());
		int multiplier = TextUtils.atoi(line.toString(), 0, 10);
		nodcnt += multiplier * 10000;
		int numSignals = numnoi + nodcnt - 1;

		if (numSignals <= 0)
		{
			System.out.println("Error reading " + fileURL.getFile());
			closeInput();
			stopProgressDialog();
			return;
		}

		// get version number (known to work with 9007, 9601)
		int version = getHSpiceInt();
		if (version != 9007 && version != 9601)
			System.out.println("Warning: may not be able to read HSpice files of type " + version);

		// ignore the unused/title information (4+72 characters over line break)
		line = new StringBuffer();
		for(int j=0; j<76; j++)
		{
			int k = getByteFromFile();
			line.append((char)k);
			if (!isTRACDCBinary && (k == '\n' || k == '\r')) j--;
		}

		// ignore the date/time information (16 characters)
		line = new StringBuffer();
		for(int j=0; j<16; j++) line.append((char)getByteFromFile());

		// ignore the copyright information (72 characters over line break)
		line = new StringBuffer();
		for(int j=0; j<72; j++)
		{
			int k = getByteFromFile();
			line.append((char)k);
			if (!isTRACDCBinary && (k == '\n' || k == '\r')) j--;
		}

		// get number of sweeps
		int sweepcnt = getHSpiceInt();
		if (DEBUGCONDITIONS)
			System.out.println("++++++++++++++++++++ VERSION="+version+" SWEEPCNT="+sweepcnt+" CNDCNT="+cndcnt+" NUMNOI="+numnoi+" MULTIPLIER="+multiplier);
		if (cndcnt == 0) sweepcnt = 0;

		// ignore the Monte Carlo information (76 characters over line break)
		line = new StringBuffer();
		for(int j=0; j<76; j++)
		{
			int k = getByteFromFile();
			line.append((char)k);
			if (!isTRACDCBinary && (k == '\n' || k == '\r')) j--;
		}

		// get the type of each signal
		if (fileNum == 0)
		{
			signalNames = new String[numSignals];
		} else
		{
			if (numSignals != signalNames.length)
			{
				System.out.println("Error: file " + fileURL.getFile() + " has " + numSignals +
					" in it, but previous files have " + signalNames.length);
				closeInput();
				stopProgressDialog();
				return;				
			}
		}
		int [] signalTypes = new int[numSignals];
		for(int k=0; k<=numSignals; k++)
		{
			line = new StringBuffer();
			for(int j=0; j<8; j++)
			{
				int l = getByteFromFile();
				line.append((char)l);
				if (!isTRACDCBinary && (l == '\n' || l == '\r')) j--;
			}
			if (k == 0) continue;
			int l = k - nodcnt;
			if (k < nodcnt) l = k + numnoi - 1;
			String lineStr = line.toString().trim();
			signalTypes[l] = TextUtils.atoi(lineStr, 0, 10);
		}
		boolean paMissingWarned = false;
		for(int k=0; k<=numSignals; k++)
		{
			line = new StringBuffer();
			for(;;)
			{
				int l = getByteFromFile();
				if (l == '\n' || l == '\r') continue;
				if (l == ' ')
				{
					if (line.length() != 0) break;

					// if name starts with blank, skip until non-blank
					for(;;)
					{
						l = getByteFromFile();
						if (l != ' ') break;
					}
				}
				line.append((char)l);
				if (version == 9007 && line.length() >= 16) break;
			}
			int j = line.length();
			int l = (j+16) / 16 * 16 - 1;
			if (version == 9007)
			{
				l = (j+15) / 16 * 16 - 1;
			}
			for(; j<l; j++)
			{
				int i = getByteFromFile();
				if (!isTRACDCBinary && (i == '\n' || i == '\r')) { j--;   continue; }
			}
			if (k == 0) continue;

			// convert name if there is a colon in it
			int startPos = 0;
			int openPos = line.indexOf("(");
			if (openPos >= 0) startPos = openPos+1;
			for(j=startPos; j<line.length(); j++)
			{
				if (line.charAt(j) == ':') break;
				if (!TextUtils.isDigit(line.charAt(j))) break;
			}
			if (j < line.length() && line.charAt(j) == ':')
			{
				l = TextUtils.atoi(line.toString().substring(startPos), 0, 10);
				PALine foundPALine = null;
				if (paList == null)
				{
					if (!paMissingWarned)
						System.out.println("Warning: there should be a ." + paExtension + " file with extra signal names");
					paMissingWarned = true;
				} else
				{
					for(PALine paLine : paList)
					{
						if (paLine.number == l) { foundPALine = paLine;   break; }
					}
				}
				if (foundPALine != null)
				{
					StringBuffer newSB = new StringBuffer();
					newSB.append(line.substring(0, startPos));
					newSB.append(foundPALine.string);
					newSB.append(line.substring(j+1));
					line = newSB;
				}
			} else
			{
				if (line.indexOf(".") >= 0)
				{
					String fixedLine = removeLeadingX(line.toString());
					line = new StringBuffer();
					line.append(fixedLine);
				}
			}

			// move parenthesis from the start to the last name
			openPos = line.indexOf("(");
			if (openPos >= 0)
			{
				String parenPrefix = line.substring(0, openPos+1);
				int lastDot = line.lastIndexOf(".");
				if (lastDot >= 0)
				{
					StringBuffer newSB = new StringBuffer();
					if (parenPrefix.equalsIgnoreCase("v(") && line.charAt(line.length()-1) == ')')
					{
						// just ignore the V()
						newSB.append(line.substring(openPos+1, lastDot+1));
						newSB.append(line.substring(lastDot+1, line.length()-1));
					} else
					{
						// move the parenthetical wrapper to the last dotted piece
						newSB.append(line.substring(openPos+1, lastDot+1));
						newSB.append(parenPrefix);
						newSB.append(line.substring(lastDot+1));
					}
					line = newSB;
				} else if (parenPrefix.equalsIgnoreCase("v(") && line.charAt(line.length()-1) == ')')
				{
					StringBuffer newSB = new StringBuffer();
					// just ignore the V()
					newSB.append(line.substring(openPos+1, line.length()-1));
					line = newSB;
				}
			}

			if (k < nodcnt) l = k + numnoi - 1; else l = k - nodcnt;
			String sigName = line.toString();
			if (fileNum == 0)
			{
				signalNames[l] = sigName;
			} else
			{
				if (!sigName.equals(signalNames[l]))
				{
					System.out.println("Error: file " + fileURL.getFile() + " signal " + (l+1) + " is '" + sigName +
						"', but previous files call that signal '" + signalNames[l] + "'");
					closeInput();
					stopProgressDialog();
					return;				
				}
			}
		}

		// read (and ignore) condition information
		for(int c=0; c<cndcnt; c++)
		{
			int j = 0;
			line = new StringBuffer();
			for(;;)
			{
				int l = getByteFromFile();
				if (l == '\n' || l == '\r') continue;
				if (l == ' ') break;
				line.append((char)l);
				j++;
				if (j >= 16) break;
			}
			int l = (j+15) / 16 * 16 - 1;
			for(; j<l; j++)
			{
				int i = getByteFromFile();
				if (!isTRACDCBinary && (i == '\n' || i == '\r')) { j--;   continue; }
			}
			if (DEBUGCONDITIONS)
				System.out.println("CONDITION "+(c+1)+" IS "+line.toString());
		}

		// read the end-of-header marker
		line = new StringBuffer();
		if (!isTRACDCBinary)
		{
			// finish line, ensure the end-of-header
			for(int j=0; ; j++)
			{
				int l = getByteFromFile();
				if (l == '\n' || l == '\r') break;
				if (j < 4) line.append((char)l);
			}
		} else
		{
			// gather end-of-header string
			for(int j=0; j<4; j++)
				line.append((char)getByteFromFile());
		}
		if (!line.toString().equals("$&%#"))
		{
			System.out.println("HSpice header improperly terminated (got "+line.toString()+")");
			closeInput();
			stopProgressDialog();
			return;
		}
		resetBinaryTRACDCReader();

		// preprocess signal names to remove constant prefix (this code also occurs in VerilogOut.readVerilogFile)
		String constantPrefix = null;
		boolean hasPrefix = true;
		for(int k=0; k<numSignals; k++)
		{
			String name = signalNames[k];
			int dotPos = name.indexOf('.');
			if (dotPos < 0) continue;
			String prefix = name.substring(0, dotPos);
			if (constantPrefix == null) constantPrefix = prefix;
			if (!constantPrefix.equals(prefix)) { hasPrefix = false;   break; }
		}
		if (!hasPrefix) constantPrefix = null; else
		{
			String fileName = fileURL.getFile();
			int pos = fileName.lastIndexOf(File.separatorChar);
			if (pos >= 0) fileName = fileName.substring(pos+1);
			pos = fileName.lastIndexOf('/');
			if (pos >= 0) fileName = fileName.substring(pos+1);
			pos = fileName.indexOf('.');
			if (pos >= 0) fileName = fileName.substring(0, pos);
			if (fileName.equals(constantPrefix)) constantPrefix += "."; else
				constantPrefix = null;
		}

		boolean isComplex = analysisTitle.equals("AC SIGNALS");
		int sweepUpper = (sweepcnt > 0) ? sweepcnt : 1;
		if (numFiles > 1) sweepUpper = numFiles;
		if (fileNum == 0)
		{
			allSignals = new Signal[numSignals][sweepUpper];
	        sweepNames = new String[sweepUpper];
		}

		// setup the simulation information
		int sweepCounter = sweepcnt;
        int sweepIndex = 0;
		for(;;)
		{
			// get sweep info
            String sweepName = "";
			if (sweepcnt > 0)
			{
				float sweepValue = getHSpiceFloat(false);
				if (eofReached)  { System.out.println("EOF before sweep data");   break; }
				sweepName = TextUtils.formatDouble(sweepValue);
				if (DEBUGCONDITIONS) System.out.println("READING SWEEP NUMBER: "+sweepValue);
				// if there are more than 2 conditions, read extra sweep values
				for(int i=2; i<cndcnt; i++)
				{
					float anotherSweepValue = getHSpiceFloat(false);
					if (eofReached)  { System.out.println("EOF reading sweep header");   break; }
					sweepName += "," + TextUtils.formatDouble(anotherSweepValue);
					if (DEBUGCONDITIONS) System.out.println("  EXTRA SWEEP NUMBER: "+anotherSweepValue);
				}
                sweepNames[fileNum + sweepIndex] = sweepName;
			} else if (numFiles > 1) sweepNames[fileNum] = (fileNum+1) + "";
			for(int k=0; k<numSignals; k++)
			{
				String name = signalNames[k];
				if (constantPrefix != null && name.startsWith(constantPrefix))
					name = name.substring(constantPrefix.length());
				String context = null;
				int lastDotPos = name.lastIndexOf('.');
				if (lastDotPos >= 0)
				{
					context = name.substring(0, lastDotPos);
					name = name.substring(lastDotPos+1);
				}
				if (sweepcnt > 0) name += "[" + sweepName + "]";
				SignalCollection scToUse = sc;
				if (sweepcnt > 0) scToUse = null;
				allSignals[k][fileNum + sweepIndex] = isComplex
	                ? ComplexSample.createComplexSignal(scToUse, sd, name, context)
	                : ScalarSample.createSignal(scToUse, sd, name, context);
			}

			for(;;)
			{
				// get the first number, see if it terminates
				float time = getHSpiceFloat(true);
				if (eofReached) break;

				// get a row of numbers
				for(int k=0; k<numSignals; k++)
				{
					if (isComplex)
					{
                        MutableSignal<ComplexSample> signal = (MutableSignal<ComplexSample>)allSignals[(k + numnoi) % numSignals][fileNum + sweepIndex];
						float realPart = getHSpiceFloat(false);
						float imagPart = getHSpiceFloat(false);
                        if (signal.getSample(time) == null)
                            signal.addSample(time, new ComplexSample(realPart, imagPart));
					} else
					{
                        MutableSignal<ScalarSample> signal = (MutableSignal<ScalarSample>)allSignals[(k + numnoi) % numSignals][fileNum + sweepIndex];
                        double val = getHSpiceFloat(false);
                        if (signal.getSample(time) == null)
                            signal.addSample(time, new ScalarSample(val));
					}
					if (eofReached)
					{
						System.out.println("EOF in the middle of the data (at " + k + " out of " + numSignals +")");
						break;
					}
				}
				if (eofReached) { System.out.println("EOF before the end of the data");   break; }
			}
			sweepCounter--;
			if (sweepCounter <= 0) break;
			sweepIndex++;
			eofReached = false;
		}
		if (sweepcnt > 0 || (numFiles > 1 && fileNum == numFiles-1))
		{
			for(int k=0; k<numSignals; k++)
			{
				String name = signalNames[k];
				if (constantPrefix != null && name.startsWith(constantPrefix))
					name = name.substring(constantPrefix.length());
				String context = null;
				int lastDotPos = name.lastIndexOf('.');
				if (lastDotPos >= 0)
				{
					context = name.substring(0, lastDotPos);
					name = name.substring(lastDotPos+1);
				}

				int total = 0;
		        for(int i=0; i<allSignals[k].length; i++)
		        	if (allSignals[k][i] != null) total++;
		        Signal<?>[] signalCopy = new Signal[total];
		        int j = 0;
		        for(int i=0; i<allSignals[k].length; i++)
		        {
		        	if (allSignals[k][i] == null) continue;
		        	signalCopy[j] = allSignals[k][i];
		        	j++;
		        }
		        
				if (isComplex)
				{
					SweptSample.createSignal(sc, sd, name, context, false, (Signal<ComplexSample>[])signalCopy);
				} else
				{
					SweptSample.createSignal(sc, sd, name, context, false, (Signal<ScalarSample>[])signalCopy);
				}
			}
			sc.setSweepNames(sweepNames);
		}
		closeInput();

		stopProgressDialog();
		if (fileNum == numFiles-1)
			System.out.println("Done reading " + analysisTitle + " analysis");
	}

    /**
	 * Method to reset the binary block pointer (done between the header and
	 * the data).
	 */
	private void resetBinaryTRACDCReader()
	{
		binaryTRACDCSize = 0;
		binaryTRACDCPosition = 0;
	}

	/**
	 * Method to read the next block of tr, sw, or ac data.
	 * @param firstbyteread true to skip the first byte.
	 * @return true on EOF.
	 */
	private boolean readBinaryTRACDCBlock(boolean firstbyteread)
		throws IOException
	{
		// read the first word of a binary block
		if (!firstbyteread)
		{
			if (dataInputStream.read() == -1) return true;
			updateProgressDialog(1);
		}
		for(int i=0; i<3; i++)
			if (dataInputStream.read() == -1) return true;
		updateProgressDialog(3);

		// read the number of 8-byte blocks
		int blocks = 0;
		for(int i=0; i<4; i++)
		{
			int uval = dataInputStream.read();
			if (uval == -1) return true;
			if (isTRACDCBinarySwapped) blocks = ((blocks >> 8) & 0xFFFFFF) | ((uval&0xFF) << 24); else
				blocks = (blocks << 8) | uval;
		}
		updateProgressDialog(4);

		// skip the dummy word
		for(int i=0; i<4; i++)
			if (dataInputStream.read() == -1) return true;
		updateProgressDialog(4);

		// read the number of bytes
		int bytes = 0;
		for(int i=0; i<4; i++)
		{
			int uval = dataInputStream.read();
			if (uval == -1) return true;
			if (isTRACDCBinarySwapped) bytes = ((bytes >> 8) & 0xFFFFFF) | ((uval&0xFF) << 24); else
				bytes = (bytes << 8) | uval;
		}
		updateProgressDialog(4);

		// now read the data
		if (bytes > 8192)
		{
			System.out.println("ERROR: block is " + bytes + " long, but limit is 8192");
			bytes = 8192;
		}
		int amtread = dataInputStream.read(binaryTRACDCBuffer, 0, bytes);
		if (amtread != bytes)
		{
			System.out.println("Expected to read " + bytes + " bytes but got only " + amtread);
			return true;
		}
		updateProgressDialog(bytes);

		// read the trailer count
		int trailer = 0;
		for(int i=0; i<4; i++)
		{
			int uval = dataInputStream.read();
			if (uval == -1) return true;
			if (isTRACDCBinarySwapped) trailer = ((trailer >> 8) & 0xFFFFFF) | ((uval&0xFF) << 24); else
				trailer = (trailer << 8) | uval;
		}
		if (trailer != bytes)
		{
			System.out.println("Block trailer claims block had " + trailer + " bytes but block really had " + bytes);
			return true;
		}
		updateProgressDialog(4);

		// set pointers for the buffer
		binaryTRACDCPosition = 0;
		binaryTRACDCSize = bytes;
		return false;
	}

	/**
	 * Method to get the next character from the simulator.
	 * @return the next character (EOF at end of file).
	 */
	private int getByteFromFile()
		throws IOException
	{
		if (byteCount == 0)
		{
			// start of HSpice file: see if it is binary or ascii
			int i = dataInputStream.read();
			if (i == -1) return(i);
			updateProgressDialog(1);
			if (i == 0 || i == 4)
			{
				isTRACDCBinary = true;
				isTRACDCBinarySwapped = false;
				if (i == 4) isTRACDCBinarySwapped = true;
				binaryTRACDCBuffer = new byte[8192];
				if (readBinaryTRACDCBlock(true)) return(-1);
			} else
			{
				isTRACDCBinary = false;
				return(i);
			}
		}
		if (isTRACDCBinary)
		{
			if (binaryTRACDCPosition >= binaryTRACDCSize)
			{
				if (readBinaryTRACDCBlock(false))
					return(-1);
			}
			int val = binaryTRACDCBuffer[binaryTRACDCPosition];
			binaryTRACDCPosition++;
			return val&0xFF;
		}
		int i = dataInputStream.read();
		updateProgressDialog(1);
		return i;
	}

	/**
	 * Method to get the next 4-byte integer from the simulator.
	 * @return the next integer.
	 */
	private int getHSpiceInt()
		throws IOException
	{
		StringBuffer line = new StringBuffer();
		for(int j=0; j<4; j++) line.append((char)getByteFromFile());
		return TextUtils.atoi(line.toString().trim(), 0, 10);
	}

	/**
	 * Method to read the next floating point number from the HSpice file.
	 * @return the next number.  Sets the global "eofReached" true on EOF.
	 */
	private float getHSpiceFloat(boolean testEOFValue)
		throws IOException
	{
		if (!isTRACDCBinary)
		{
			StringBuffer line = new StringBuffer();
			for(int j=0; j<11; j++)
			{
				int l = getByteFromFile();
				if (l == -1)
				{
					eofReached = true;   return 0;
				}
				line.append((char)l);
				if (l == '\n' || l == '\r') j--;
			}
			String result = line.toString();
			if (testEOFValue && result.equals("0.10000E+31")) { eofReached = true;   return 0; }
			return (float)TextUtils.atof(result);
		}

		// binary format
		int fi0 = getByteFromFile();
		int fi1 = getByteFromFile();
		int fi2 = getByteFromFile();
		int fi3 = getByteFromFile();
		if (fi0 < 0 || fi1 < 0 || fi2 < 0 || fi3 < 0)
		{
			eofReached = true;
			return 0;
		}
		fi0 &= 0xFF;
		fi1 &= 0xFF;
		fi2 &= 0xFF;
		fi3 &= 0xFF;
		int fi = 0;
		if (isTRACDCBinarySwapped)
		{
			fi = (fi3 << 24) | (fi2 << 16) | (fi1 << 8) | fi0;
		} else
		{
			fi = (fi0 << 24) | (fi1 << 16) | (fi2 << 8) | fi3;
		}
		float f = Float.intBitsToFloat(fi);

		// the termination value (in hex) is 71 49 F2 CA
		if (testEOFValue && f > 1.00000000E30 && f < 1.00000002E30)
		{
			eofReached = true;
			return 0;
		}
		return f;
	}

	/**
	 * Method to remove the leading "x" character in each dotted part of a string.
	 * HSpice decides to add "x" in front of every cell name, so the path "me.you"
	 * appears as "xme.xyou".
	 * @param name the string from HSpice.
	 * @return the string without leading "X"s.
	 */
	static String removeLeadingX(String name)
	{
		// remove all of the "x" characters at the start of every instance name
		int dotPos = -1;
		while (name.indexOf('.', dotPos+1) >= 0)
		{
			int xPos = dotPos + 1;
			if (name.length() > xPos && name.charAt(xPos) == 'x')
			{
				name = name.substring(0, xPos) + name.substring(xPos+1);
			}
			dotPos = name.indexOf('.', xPos);
			if (dotPos < 0) break;
		}
		return name;
	}
}
