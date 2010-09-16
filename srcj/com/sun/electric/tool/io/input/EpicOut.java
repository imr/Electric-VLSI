/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EpicOut.java
 * Module to read Nanosim simulation output
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.simulation.MutableSignal;
import com.sun.electric.tool.simulation.ScalarSample;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.simulation.SignalCollection;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.util.TextUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Class for reading and displaying waveforms from Epic output.
 */
public class EpicOut extends Input<Stimuli>
{
	/** Pattern used to split input line into pieces. */		private Pattern whiteSpace = Pattern.compile("[ \t]+");
	/** Epic separator character */								private static final char separator = '.';
	private List<Signal<ScalarSample>> signalsByEpicIndex;
	private Set<Integer> currentSignals;

	protected Stimuli processInput(URL fileURL, Cell cell, Stimuli sd)
		throws IOException
	{
		// find all ".out" files
		List<String> outFiles = new ArrayList<String>();
		outFiles.add(fileURL.getPath());
		String fileNameNoPath = TextUtils.getFileNameWithoutExtension(TextUtils.URLtoString(fileURL), true) + "_";
		String topDirName = TextUtils.getFilePath(fileURL);
		File topDir = new File(topDirName);
		String [] fileList = topDir.list();
		for(int i=0; i<fileList.length; i++)
		{
			if (!fileList[i].startsWith(fileNameNoPath)) continue;
			if (!fileList[i].endsWith(".out")) continue;
			String newFile = topDirName + fileList[i];
			if (!outFiles.contains(newFile))
				outFiles.add(newFile);
		}
		Collections.sort(outFiles, TextUtils.STRING_NUMBER_ORDER);
		if (outFiles.size() > 1)
			System.out.println("Reading " + outFiles.size() + " files...");

		// setup the delimiter for this stimuli
		sd.setNetDelimiter(" ");

		// open the file
		boolean first = true;
		for(String fileName : outFiles)
		{
			URL fURL = TextUtils.makeURLToFile(fileName);
			if (openTextInput(fURL)) return sd;

			// show progress reading file
			System.out.println("Reading Epic output file: " + fileName);
			startProgressDialog("Epic output", fileName);

			// read the actual signal data from the file
			readEpicFile(fileName, cell, sd, first);
			first = false;

			// stop progress dialog, close the file
			stopProgressDialog();
			closeInput();
		}
		return sd;
	}

	private void readEpicFile(String fileName, Cell cell, Stimuli sd, boolean first)
		throws IOException
	{
		double curTime = 0;
		SignalCollection sc;
		if (first)
		{
			signalsByEpicIndex = new ArrayList<Signal<ScalarSample>>();
			currentSignals = new HashSet<Integer>();
			sc = Stimuli.newSignalCollection(sd, "TRANS SIGNALS");
		} else
		{
			sc = sd.findSignalCollection("TRANS SIGNALS");
		}
		double timeResolution = 1;
		double voltageResolution = 1;
		double currentResolution = 1;

		for (;;)
		{
			String line = getLine();
			if (line == null) break;
			updateProgressDialog(line.length()+1);

			// ignore blank lines and comments
			if (line.length() == 0) continue;
			char ch = line.charAt(0);
			if (ch == ';' || Character.isSpaceChar(ch)) continue;

			// handle commands
			if (ch == '.')
			{
				String[] split = whiteSpace.split(line);
				if (split[0].equals(".index") && split.length == 4)
				{
					// get the signal name
					String name = split[1];
					if (name.startsWith("v(") && name.endsWith(")")) {
						name = name.substring(2, name.length() - 1);
					} else if (name.startsWith("i(") && name.endsWith(")")) {
						name = name.substring(2, name.length() - 1);
					} else if (name.startsWith("i1(") && name.endsWith(")")) {
						name = name.substring(3, name.length() - 1);
					}
					String context = null;
					String wholeName = name;
					int sepPos = name.lastIndexOf(separator);
					if (sepPos >= 0)
					{
						context  = name.substring(0, sepPos);
						name = name.substring(sepPos+1);
					}

					// get the signal number
					int sigNum = TextUtils.atoi(split[2]);

					// see if it is current or voltage
					boolean isCurrent;
					if (split[3].equals("v")) isCurrent = false; else
					if (split[3].equals("i")) isCurrent = true; else
					{
						System.out.println("Error in line " + lineReader.getLineNumber() + ": Unknown waveform type: " + line);
						break;
					}

					if (first)
					{
						// create the signal
						while (signalsByEpicIndex.size() <= sigNum)
							signalsByEpicIndex.add(null);
						Signal<ScalarSample> s = signalsByEpicIndex.get(sigNum);
						if (s == null)
						{
							s = ScalarSample.createSignal(sc, sd, name, context);
							signalsByEpicIndex.set(sigNum, s);
							if (isCurrent) currentSignals.add(Integer.valueOf(sigNum));
						}
					} else
					{
						// check the signal
						Signal<ScalarSample> s = signalsByEpicIndex.get(sigNum);
						if (s == null)
						{
							System.out.println("WARNING: File " + fileName + " has unknown signal " + sigNum);
						} else
						{
							boolean nameSame = name.equals(s.getSignalName());
							if (context != null) nameSame &= context.equals(s.getSignalContext());
							if (!nameSame)
							{
								System.out.println("WARNING: File " + fileName + " signal " + sigNum +
									" is called '" + wholeName + "' but in an earlier file is called '" + s.getFullName() + "'");
							}
						}
					}
					continue;
				}

				// handle number resolutions
				if (split[0].equals(".time_resolution") && split.length == 2)
				{
					timeResolution = TextUtils.atof(split[1]) * 1e-9;
					continue;
				}
				if (split[0].equals(".current_resolution") && split.length == 2)
				{
					currentResolution = TextUtils.atof(split[1]);
					continue;
				}
				if (split[0].equals(".voltage_resolution") && split.length == 2)
				{
					voltageResolution = TextUtils.atof(split[1]);
					continue;
				}

				// ignore known commands
				if (split[0].equals(".vdd") && split.length == 2) continue;
				if (split[0].equals(".simulation_time") && split.length == 2) continue;
				if (split[0].equals(".high_threshold") && split.length == 2) continue;
				if (split[0].equals(".low_threshold") && split.length == 2) continue;
				if (split[0].equals(".nnodes") && split.length == 2) continue;
				if (split[0].equals(".nelems") && split.length == 2) continue;
				if (split[0].equals(".extra_nodes") && split.length == 2) continue;
				if (split[0].equals(".bus_notation") && split.length == 4) continue;
				if (split[0].equals(".hier_separator") && split.length == 2) continue;
				if (split[0].equals(".case") && split.length == 2) continue;

				System.out.println("Error in line " + lineReader.getLineNumber() + ": Unrecognized Epic command: " + line);
				break;
			}

			// handle time and data
			if (ch >= '0' && ch <= '9')
			{
				String[] split = whiteSpace.split(line);
				if (split.length > 1)
				{
					int num = TextUtils.atoi(split[0]);
					double value = TextUtils.atof(split[1]);
					if (currentSignals.contains(Integer.valueOf(num))) value *= currentResolution; else
						value *= voltageResolution;
					MutableSignal<ScalarSample> s = (MutableSignal<ScalarSample>)signalsByEpicIndex.get(num);
					if (s == null)
					{
						System.out.println("Error in line " + lineReader.getLineNumber() + ": Signal " + num + " not defined");
						break;
					}
					try
					{
						s.addSample(curTime, new ScalarSample(value));
					} catch (RuntimeException e)
					{
					}
				} else
				{
					long num;
					try
					{
						num = Long.parseLong(split[0]);
					} catch (NumberFormatException e)
					{
						num = 0;
					}
					curTime = num * timeResolution;
				}
				continue;
			}

			System.out.println("Error in line " + lineReader.getLineNumber() + ": Unrecognized Epic line: " + line);
			break;
		}
	}
}
