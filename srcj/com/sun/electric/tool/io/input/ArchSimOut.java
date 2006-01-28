/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ArchSimOut.java
 * Input/output tool: reader for ArchSim output (.asj)
 * Written by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.simulation.Analysis;
import com.sun.electric.tool.simulation.DigitalSignal;
import com.sun.electric.tool.simulation.Stimuli;

import java.awt.Point;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Class for reading and displaying waveforms from ArchSim output.
 * Thease are contained in .asj files.
 */
public class ArchSimOut extends Simulate
{
	ArchSimOut() {}

	/**
	 * Method to read an ArchSim output file.
	 */
	protected Stimuli readSimulationOutput(URL fileURL, Cell cell)
		throws IOException
	{
		// open the file
		if (openTextInput(fileURL)) return null;

		// show progress reading .dump file
		startProgressDialog("ArchSim output", fileURL.getFile());

		// read the actual signal data from the .dump file
		Stimuli sd = readArchSimFile(cell);

		// stop progress dialog, close the file
		stopProgressDialog();
		closeInput();

		// return the simulation data
		return sd;
	}

	private Stimuli readArchSimFile(Cell cell)
		throws IOException
	{
		// read all of the stimuli
		HashMap<String,List<Point>> symbolTable = new HashMap<String,List<Point>>();
		int greatestTime = 0;
		for(;;)
		{
			String line = getLineFromSimulator();
			if (line == null) break;
			int firstColon = line.indexOf(':');
			int secondColon = line.indexOf(':', firstColon+1);
			if (firstColon < 0 || secondColon < 0)
			{
				System.out.println("Line " + lineReader.getLineNumber() + " is missing two colons: " + line);
				continue;
			}
			int time = TextUtils.atoi(line.substring(0, firstColon));
			if (time > greatestTime) greatestTime = time;
			String signalName = line.substring(firstColon+1, secondColon);
			int value = TextUtils.atoi(line.substring(secondColon+1));
			List<Point> values = symbolTable.get(signalName);
			if (values == null)
			{
				values = new ArrayList<Point>();
				symbolTable.put(signalName, values);
			}
			values.add(new Point(time, value));			
			continue;
		}

		// make a data structure for it
		Stimuli sd = new Stimuli();
		Analysis an = new Analysis(sd, Analysis.ANALYSIS_SIGNALS);
		sd.setCell(cell);
		for(String signalName : symbolTable.keySet())
		{
			List<Point> values = symbolTable.get(signalName);

			int numStimuli = values.size();
			DigitalSignal sig = new DigitalSignal(an);
			sig.setSignalName(signalName);
			sig.buildTime(numStimuli);
			sig.buildState(numStimuli);
			int i = 0;
			for(Point pt : values)
			{
				sig.setTime(i, pt.x);
				int state = Stimuli.LOGIC_LOW | Stimuli.GATE_STRENGTH;
				if (pt.y != 0) state = Stimuli.LOGIC_HIGH | Stimuli.GATE_STRENGTH;
				sig.setState(i, state);
				i++;
			}
		}
		return sd;
	}

}
