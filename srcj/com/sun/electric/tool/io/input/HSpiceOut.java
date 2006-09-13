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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.simulation.AnalogSignal;
import com.sun.electric.tool.simulation.Analysis;
import com.sun.electric.tool.simulation.Stimuli;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Class for reading and displaying waveforms from HSpice output.
 * This includes transient information in .tr and .pa files (.pa0/.tr0, .pa1/.tr1, ...)
 * It also includes AC analysis in .ac files; DC analysis in .sw files;
 * and Measurements in .mt files.
 */
public class HSpiceOut extends Simulate
{
	/** true if tr/ac/sw file is binary */						private boolean isTRACDCBinary;
	/** true if binary tr/ac/sw file has bytes swapped */		private boolean isTRACDCBinarySwapped;
	/** the raw file base */									private String fileBase;
	/** the "tr" file extension (tr0, tr1, ...): transient */	private String trExtension;
	/** the "sw" file extension (sw0, sw1, ...): DC */			private String swExtension;
	/** the "ic" file extension (ic0, ic1, ...): old DC */		private String icExtension;
	/** the "ac" file extension (ac0, ac1, ...): AC */			private String acExtension;
	/** the "mt" file extension (mt0, mt1, ...): measurement */	private String mtExtension;
	/** the "pa" file extension (pa0, pa1, ...): long names */	private String paExtension;
	private int binaryTRACDCSize, binaryTRACDCPosition;
	private boolean eofReached;
	private byte [] binaryTRACDCBuffer;

	// HSpice name associations from the .paX file
	private static class PALine
	{
		int     number;
		String  string;
	};

    private static class HSpiceAnalogSignal extends AnalogSignal
    {
		float [][] sigData;

        HSpiceAnalogSignal(Analysis an)
        {
            super(an);
        }

        /**
         * Free allocated resources before closing.
         */
        public void finished()
        {
            sigData = null; // making memory available for GC
        }

        /**
         * Method to return the value of this signal at a given event index.
         * @param sweep sweep index
         * @param index the event index (0-based).
         * @param result double array of length 3 to return (time, lowValue, highValue)
         * If this signal is not a basic signal, return 0 and print an error message.
         */
        public void getEvent(int sweep, int index, double[] result)
		{
		  result[0] = getTime(index, sweep);
          result[1] = result[2] = sigData[sweep][index];
        }

        /**
         * Method to return the number of events in one sweep of this signal.
         * This is the number of events along the horizontal axis, usually "time".
         * The method only works for sweep signals.
         * @param sweep the sweep number to query.
         * @return the number of events in this signal.
         */
        public int getNumEvents(int sweep)
		{
			return sigData[sweep].length;
        }
        
        /**
         * Method to return the number of sweeps in this signal.
         * @return the number of sweeps in this signal.
         * If this signal is not a sweep signal, returns 1.
         */
        public int getNumSweeps()
		{
        	if (sigData == null) return 0;
			return sigData.length;
        }
    }
    
    HSpiceOut() {}

	/**
	 * Method to read HSpice output files.
	 * @param fileURL the URL to one of the output files.
	 * @param cell the Cell associated with these HSpice output files.
	 */
	protected Stimuli readSimulationOutput(URL fileURL, Cell cell)
		throws IOException
	{
		// create the Stimuli object
		Stimuli sd = new Stimuli();
		sd.setCell(cell);

		// figure out file names
		fileBase = fileURL.getFile();
		trExtension = "tr0";
		swExtension = "sw0";
		icExtension = "ic0";
		acExtension = "ac0";
		mtExtension = "mt0";
		paExtension = "pa0";
		int dotPos = fileBase.lastIndexOf('.');
		if (dotPos > 0)
		{
			String extension = fileBase.substring(dotPos+1);
			fileBase = fileBase.substring(0, dotPos);
			if (extension.startsWith("tr") || extension.startsWith("sw") || extension.startsWith("ic") ||
				extension.startsWith("ac") || extension.startsWith("mt") || extension.startsWith("pa"))
			{
				trExtension = "tr" + extension.substring(2);
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
        System.out.println("Reading HSpice measurements '" + mtURL.getFile() + "'");

		Analysis an = new Analysis(sd, Analysis.ANALYSIS_MEAS);
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
			String [] keywords = breakMTLine(nextLine);
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
					keywords = breakMTLine(lastLine);
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
				keywords = breakMTLine(lastLine);
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
		List<AnalogSignal> measData = new ArrayList<AnalogSignal>();
		for(String mName : measurementNames)
		{
			List<Double> mData = measurementData.get(mName);
			AnalogSignal as = new AnalogSignal(an);
			as.setSignalName(mName);
			as.buildValues(mData.size());
			for(int i=0; i<mData.size(); i++) as.setValue(i, mData.get(i).doubleValue());
			measData.add(as);
		}

		closeInput();
	}

	private String[] breakMTLine(String line)
	{
		List<String> strings = new ArrayList<String>();
		for(int i=1; ; )
		{
			if (line.length() <= i+1) break;
			int end = i+17;
			if (end > line.length()) end = line.length();
			while (end < line.length() && line.charAt(end-1) != ' ') end++;
			String part = line.substring(i, end);
			strings.add(part.trim());
			i = end;
		}
		String [] retVal = new String[strings.size()];
		for(int i=0; i<strings.size(); i++) retVal[i] = strings.get(i);
		return retVal;
	}

	/**
	 * Method to find the ".sw" file and read DC data.
	 * @param sd the Stimuli to add this DC data to.
	 * @param fileURL the URL to the ".tr" file.
	 * @throws IOException
	 */
	private void addTRData(Stimuli sd, List<PALine> paList, URL fileURL)
		throws IOException
	{
		// find the associated ".sw" name file
		URL swURL = null;
		try
		{
			swURL = new URL(fileURL.getProtocol(), fileURL.getHost(), fileURL.getPort(), fileBase + "." + trExtension);
		} catch (java.net.MalformedURLException e)
		{
		}
		if (swURL == null) return;
        if (!TextUtils.URLExists(swURL)) return;

		// process the DC data
		readTRDCACFile(sd, swURL, paList, Analysis.ANALYSIS_TRANS);
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
		} catch (java.net.MalformedURLException e)
		{
		}
		if (swURL != null && TextUtils.URLExists(swURL))
		{
			// process the DC data
			readTRDCACFile(sd, swURL, paList, Analysis.ANALYSIS_DC);
			return;
		}

		// no associated ".sw" file, look for an ".ic" name file
		URL icURL = null;
		try
		{
			icURL = new URL(fileURL.getProtocol(), fileURL.getHost(), fileURL.getPort(), fileBase + "." + icExtension);
		} catch (java.net.MalformedURLException e)
		{
		}
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
		} catch (java.net.MalformedURLException e)
		{
		}
		if (acURL == null) return;
        if (!TextUtils.URLExists(acURL)) return;

		// process the AC data
		readTRDCACFile(sd, acURL, paList, Analysis.ANALYSIS_AC);
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
		} catch (java.net.MalformedURLException e)
		{
		}
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

	private void readTRDCACFile(Stimuli sd, URL fileURL, List<PALine> paList, Analysis.AnalysisType analysisType)
		throws IOException
	{
		if (openBinaryInput(fileURL)) return;
		eofReached = false;
		resetBinaryTRACDCReader();

		Analysis an = new Analysis(sd, analysisType);
		startProgressDialog("HSpice " + analysisType.toString() + " analysis", fileURL.getFile());
        System.out.println("Reading HSpice " + analysisType.toString() + " analysis '" + fileURL.getFile() + "'");

		// get number of nodes
		int nodcnt = getHSpiceInt();

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
		for(int j=0; j<76; j++)
		{
			int k = getByteFromFile();
			if (!isTRACDCBinary && k == '\n') j--;
		}

		// ignore the date/time information (16 characters)
		for(int j=0; j<16; j++) getByteFromFile();

		// ignore the copywrite information (72 characters over line break)
		for(int j=0; j<72; j++)
		{
			int k = getByteFromFile();
			if (!isTRACDCBinary && k == '\n') j--;
		}

		// get number of sweeps
		int sweepcnt = getHSpiceInt();
		if (cndcnt == 0) sweepcnt = 0;

		// ignore the Monte Carlo information (76 characters over line break)
		for(int j=0; j<76; j++)
		{
			int k = getByteFromFile();
			if (!isTRACDCBinary && k == '\n') j--;
		}

		// get the type of each signal
		String [] signalNames = new String[numSignals];
		int [] signalTypes = new int[numSignals];
		for(int k=0; k<=numSignals; k++)
		{
			line = new StringBuffer();
			for(int j=0; j<8; j++)
			{
				int l = getByteFromFile();
				line.append((char)l);
				if (!isTRACDCBinary && l == '\n') j--;
			}
			if (k == 0) continue;
			int l = k - nodcnt;
			if (k < nodcnt) l = k + numnoi - 1;
			signalTypes[l] = TextUtils.atoi(line.toString(), 0, 10);
		}
		boolean paMissingWarned = false;
		for(int k=0; k<=numSignals; k++)
		{
			int j = 0;
			line = new StringBuffer();
			for(;;)
			{
				int l = getByteFromFile();
				if (l == '\n') continue;
				if (l == ' ') break;
				line.append((char)l);
				j++;
				if (version == 9007 && j >= 16) break;
			}
			int l = (j+16) / 16 * 16 - 1;
			if (version == 9007)
			{
				l = (j+15) / 16 * 16 - 1;
			}
			for(; j<l; j++)
			{
				int i = getByteFromFile();
				if (!isTRACDCBinary && i == '\n') { j--;   continue; }
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
				int lastDot = line.lastIndexOf(".");
				if (lastDot >= 0)
				{
					StringBuffer newSB = new StringBuffer();
					newSB.append(line.substring(openPos+1, lastDot+1));
					newSB.append(line.substring(0, openPos+1));
					newSB.append(line.substring(lastDot+1));
					line = newSB;
				}
			}

			if (k < nodcnt) l = k + numnoi - 1; else l = k - nodcnt;
			signalNames[l] = line.toString();
		}

		// read sweep information
		if (cndcnt != 0)
		{
			int j = 0;
			line = new StringBuffer();
			for(;;)
			{
				int l = getByteFromFile();
				if (l == '\n') continue;
				if (l == ' ') break;
				line.append((char)l);
				j++;
				if (j >= 16) break;
			}
			int l = (j+15) / 16 * 16 - 1;
			for(; j<l; j++)
			{
				int i = getByteFromFile();
				if (!isTRACDCBinary && i == '\n') { j--;   continue; }
			}
		}

		// read the end-of-header marker
		line = new StringBuffer();
		if (!isTRACDCBinary)
		{
			// finish line, ensure the end-of-header
			for(int j=0; ; j++)
			{
				int l = getByteFromFile();
				if (l == '\n') break;
				if (j < 4) line.append(l);
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

		// setup the simulation information
		HSpiceAnalogSignal [] allSignals = new HSpiceAnalogSignal[numSignals];
		for(int k=0; k<numSignals; k++)
		{
			HSpiceAnalogSignal as = new HSpiceAnalogSignal(an);
			allSignals[k] = as;
			int lastDotPos = signalNames[k].lastIndexOf('.');
			if (lastDotPos >= 0)
			{
				as.setSignalContext(signalNames[k].substring(0, lastDotPos));
				as.setSignalName(signalNames[k].substring(lastDotPos+1));
			} else
			{
				as.setSignalName(signalNames[k]);
			}
		}

		List<List<float[]>> theSweeps = new ArrayList<List<float[]>>();
		int sweepCounter = sweepcnt;
		for(;;)
		{
			// get sweep info
			if (sweepcnt > 0)
			{
				float sweepValue = getHSpiceFloat();
				if (eofReached) break;
				an.addSweep(new Double(sweepValue));
			}

			// now read the data
			List<float[]> allTheData = new ArrayList<float[]>();
			for(;;)
			{
				float [] oneSetOfData = new float[numSignals+1];

				// get the first number, see if it terminates
				float time = getHSpiceFloat();
				if (eofReached) break;
				oneSetOfData[0] = time;

				// get a row of numbers
				for(int k=0; k<numSignals; k++)
				{
					float value = getHSpiceFloat();
					if (eofReached) break;
					oneSetOfData[(k+numnoi)%numSignals+1] = value;
				}
				if (eofReached) break;
				allTheData.add(oneSetOfData);
			}
			int numEvents = allTheData.size();
			an.addCommonTime(numEvents);
			theSweeps.add(allTheData);
			sweepCounter--;
			if (sweepCounter <= 0) break;
			eofReached = false;
		}
		closeInput();
        
        // Put data to Stimuli
		for(int sigNum=0; sigNum<numSignals; sigNum++)
		{
			HSpiceAnalogSignal as = allSignals[sigNum];
			as.sigData = new float[theSweeps.size()][];
			for(int sweepNum=0; sweepNum<theSweeps.size(); sweepNum++)
			{
	            List<float[]> allTheData = theSweeps.get(sweepNum);
				as.sigData[sweepNum] = new float[allTheData.size()];
	            for (int eventNum = 0; eventNum < allTheData.size(); eventNum++)
				{
					float [] oneSetOfData = allTheData.get(eventNum);
					as.sigData[sweepNum][eventNum] = oneSetOfData[sigNum+1];
					if (sigNum == 0) an.setCommonTime(eventNum, sweepNum, oneSetOfData[0]);
				}
			}
		}
		an.setBoundsDirty();
		stopProgressDialog();
        System.out.println("Done reading " + analysisType.toString() + " analysis");
	}

	/*
	 * Method to reset the binary block pointer (done between the header and
	 * the data).
	 */
	private void resetBinaryTRACDCReader()
	{
		binaryTRACDCSize = 0;
		binaryTRACDCPosition = 0;
	}

	/*
	 * Method to read the next block of tr, sw, or ac data.  Skips the first byte if "firstbyteread"
	 * is true.  Returns true on EOF.
	 */
	private boolean readBinaryTRACDCBlock(boolean firstbyteread)
		throws IOException
	{
		// read the first word of a binary block
		if (!firstbyteread)
		{
			if (dataInputStream.read() == -1) return true;
		}
		for(int i=0; i<3; i++)
			if (dataInputStream.read() == -1) return true;
		updateProgressDialog(4);

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
		if (amtread != bytes) return true;
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
		if (trailer != bytes) return true;
		updateProgressDialog(4);

		// set pointers for the buffer
		binaryTRACDCPosition = 0;
		binaryTRACDCSize = bytes;
		return false;
	}

	/**
	 * Method to get the next character from the simulator (file or pipe).
	 * Returns EOF at end of file.
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

	private int getHSpiceInt()
		throws IOException
	{
		StringBuffer line = new StringBuffer();
		for(int j=0; j<4; j++) line.append((char)getByteFromFile());
		return TextUtils.atoi(line.toString().trim(), 0, 10);
	}

	/**
	 * Method to read the next floating point number from the HSpice file into "val".
	 * Returns positive on error, negative on EOF, zero if OK.
	 */
	private float getHSpiceFloat()
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
				if (l == '\n') j--;
			}
			String result = line.toString();
			if (result.equals("0.10000E+31")) { eofReached = true;   return 0; }
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

		if (f > 1.00000000E30 && f < 1.00000002E30)
		{
			eofReached = true;
			return 0;
		}
		return f;
	}

}
