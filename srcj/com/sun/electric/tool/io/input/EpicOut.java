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
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.simulation.MutableSignal;
import com.sun.electric.tool.simulation.ScalarSample;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.simulation.SignalCollection;
import com.sun.electric.tool.simulation.Stimuli;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
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

	protected Stimuli processInput(URL fileURL, Cell cell, Stimuli sd)
		throws IOException
	{
		sd.setNetDelimiter(" ");

		// open the file
		if (openTextInput(fileURL)) return sd;

		// show progress reading file
		System.out.println("Reading Epic output file: " + fileURL.getFile());
		startProgressDialog("Epic output", fileURL.getFile());

		// read the actual signal data from the file
		readEpicFile(cell, sd);

		// stop progress dialog, close the file
		stopProgressDialog();
		closeInput();
		return sd;
	}

	private void readEpicFile(Cell cell, Stimuli sd)
		throws IOException
	{
		double curTime = 0;
		List<Signal<ScalarSample>> signalsByEpicIndex = new ArrayList<Signal<ScalarSample>>();
		SignalCollection sc = Stimuli.newSignalCollection(sd, "TRANS SIGNALS");
		Set<Integer> currentSignals = new HashSet<Integer>();
		double timeResolution = 1;
		double voltageResolution = 1;
		double currentResolution = 1;

		for (;;)
		{
			String line = getLine();
			if (line == null) break;

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

					// get the signal number
					int sigNum = TextUtils.atoi(split[2]);
					while (signalsByEpicIndex.size() <= sigNum)
						signalsByEpicIndex.add(null);

					// see if it is current or voltage
					boolean isCurrent;
					if (split[3].equals("v")) isCurrent = false; else
					if (split[3].equals("i")) isCurrent = true; else
					{
						System.out.println("Error in line " + lineReader.getLineNumber() + ": Unknown waveform type: " + line);
						break;
					}

					// create the signal
					Signal<ScalarSample> s = signalsByEpicIndex.get(sigNum);
					if (s == null)
					{
						String context = null;
						int sepPos = name.lastIndexOf(separator);
						if (sepPos >= 0)
						{
							context  = name.substring(0, sepPos);
							name = name.substring(sepPos+1);
						}
						s = ScalarSample.createSignal(sc, sd, name, context);
						signalsByEpicIndex.set(sigNum, s);
						if (isCurrent) currentSignals.add(Integer.valueOf(sigNum));
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
				int num = TextUtils.atoi(split[0]);
				if (split.length > 1)
				{
					double value = TextUtils.atof(split[1]);
					if (currentSignals.contains(Integer.valueOf(num))) value *= currentResolution; else
						value *= voltageResolution;
					MutableSignal<ScalarSample> s = (MutableSignal<ScalarSample>)signalsByEpicIndex.get(num);
					if (s == null)
					{
						System.out.println("Error in line " + lineReader.getLineNumber() + ": Signal " + num + " not defined");
						break;
					}
					s.addSample(curTime, new ScalarSample(value));
				} else
				{
					curTime = num * timeResolution;
				}
				continue;
			}

			System.out.println("Error in line " + lineReader.getLineNumber() + ": Unrecognized Epic line: " + line);
			break;
		}
	}
}
