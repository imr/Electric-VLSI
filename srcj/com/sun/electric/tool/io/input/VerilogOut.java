/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VerilogOut.java
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

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.io.input.Simulate;

import java.io.InputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.DataInputStream;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Class for reading and displaying waveforms from Verilog output.
 * Thease are contained in .spo files.
 */
public class VerilogOut extends Simulate
{
	VerilogOut() {}

	/**
	 * Method to read an Verilog output file.
	 */
	protected SimData readSimulationOutput(URL fileURL)
		throws IOException
	{
		// open the file
		InputStream stream = TextUtils.getURLStream(fileURL);
		if (stream == null) return null;
		if (openBinaryInput(fileURL, stream)) return null;

		// show progress reading .tr0 file
		startProgressDialog("Verilog output", fileURL.getFile());

		// read the actual signal data from the .tr0 file
		SimData sd = readVerilogFile();

		// stop progress dialog, close the file
		stopProgressDialog();
		closeInput();

		// return the simulation data
		return sd;
	}

	private SimData readVerilogFile()
		throws IOException
	{

//		SimData sd = new SimData();
//		sd.signalNames = sim_spice_signames;
//		sd.events = sim_spice_numbers;
		System.out.println("CANNOT READ VERILOG OUTPUT YET");
		return null;
	}

}
