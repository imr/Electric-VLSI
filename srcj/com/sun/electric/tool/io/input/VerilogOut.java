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

import java.io.InputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.DataInputStream;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Class for reading and displaying waveforms from Verilog output.
 * Thease are contained in .spo files.
 */
public class VerilogOut extends Simulate
{
	static class VerilogStimuli
	{
		double time;
		int state;

		VerilogStimuli(double time, int state)
		{
			this.time = time;
			this.state = state;
		}
	}

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
		if (openTextInput(fileURL, stream)) return null;

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
		Simulate.SimData sd = new Simulate.SimData();
		double sim_timescale = 1.0;
		String sim_vercurscope = "";
		int sim_vercurlevel = 0;
		int numSignals = 0;
		HashMap symbolTable = new HashMap();
		List curArray = null;
		for(;;)
		{
			String keyword = getNextKeyword();
			if (keyword == null) break;

			// ignore "$date", "$version" or "$timescale"
			if (keyword.equals("$date") || keyword.equals("$version"))
			{
				sim_verparsetoend();
				continue;
			}
			if (keyword.equals("$timescale"))
			{
				String units = getNextKeyword();
//				units = -1;
//				pt = sim_verline;
//				keyword = getkeyword(&pt, x_(" "));
//				for(pt = keyword; *pt != 0; pt++)
//					if (!isdigit(*pt)) break;
//				if (*pt == 0)
//				{
//					ttyputerr(_("No time units on line %ld"), pt, sim_verlineno);
//				} else
//				{
//					if (namesame(pt, "ps") == 0) units = INTTIMEUNITPSEC; else
//					if (namesame(pt, "s") == 0) units = INTTIMEUNITSEC; else
//						ttyputerr(_("Unknown time units: '%s' on line %ld"), pt, sim_verlineno);
//				}
//				if (units >= 0)
//				{
//					*pt = 0;
//					sim_timescale = figureunits(keyword, VTUNITSTIME, units);
//				}
				sim_verparsetoend();
				continue;
			}
			if (keyword.equals("$scope"))
			{
				String scope = getNextKeyword();
				if (scope == null) break;
				if (scope.equals("module") || scope.equals("task") || scope.equals("function"))
				{
					// scan for arrays
					cleanUpScope(curArray, sd);
					curArray = new ArrayList();

					String scopeName = getNextKeyword();
					if (scopeName == null) break;
					if (sim_vercurscope.length() > 0) sim_vercurscope += ".";
					sim_vercurscope += scopeName;
					sim_vercurlevel++;
					curArray = new ArrayList();
				}
				sim_verparsetoend();
				continue;
			}

			if (keyword.equals("$upscope"))
			{
				if (sim_vercurlevel <= 0 || sim_vercurscope.length() == 0)
				{
					System.out.println("Unbalanced $upscope on line " + lineReader.getLineNumber());
					continue;
				}

				// scan for arrays
				cleanUpScope(curArray, sd);
				curArray = new ArrayList();

				int dotPos = sim_vercurscope.lastIndexOf('.');
				if (dotPos >= 0)
				{
					sim_vercurscope = sim_vercurscope.substring(0, dotPos);
					sim_vercurlevel--;
				}
				sim_verparsetoend();
				continue;
			}

			if (keyword.equals("$var"))
			{
				String varName = getNextKeyword();
				if (varName == null) break;
				if (varName.equals("wire") || varName.equals("reg") ||
					varName.equals("supply0") || varName.equals("supply1") ||
					varName.equals("parameter") || varName.equals("trireg"))
				{
					// get the bus width
					String widthText = getNextKeyword();
					if (widthText == null) break;
					int width = TextUtils.atoi(widthText);

					// get the symbol name for this signal
					String symbol = getNextKeyword();
					if (symbol == null) break;

					// get the signal name
					String signalName = getNextKeyword();
					if (signalName == null) break;

					// see if there is an index
					String index = getNextKeyword();
					if (index == null) break;
					if (index.equals("$end")) index = ""; else
					{
						sim_verparsetoend();
					}
					numSignals++;

					Simulate.SimDigitalSignal sig = new Simulate.SimDigitalSignal(null);
					sig.signalName = signalName + index;
					sig.signalContext = sim_vercurscope;
					sig.tempList = new ArrayList();

					if (index.length() > 0 && width == 1)
					{
						curArray.add(sig);
					} else
					{
						sd.signals.add(sig);
					}

					if (width > 1)
					{
						// create fake signals for the individual entries
						sig.bussedSignals = new ArrayList();
						for(int i=0; i<width; i++)
						{
							Simulate.SimDigitalSignal subSig = new Simulate.SimDigitalSignal(null);
							subSig.signalName = signalName + "[" + i + "]";
							subSig.signalContext = sim_vercurscope;
							subSig.tempList = new ArrayList();
							sig.bussedSignals.add(subSig);
							addSignalToHashMap(subSig, symbol + "[" + i + "]", symbolTable);
							numSignals++;
						}
					}

					// put it in the symbol table
					addSignalToHashMap(sig, symbol, symbolTable);
				}
				continue;
			}

			if (keyword.equals("$enddefinitions"))
			{
				sim_verparsetoend();
				System.out.println("Found " + numSignals + " signal names");
//				DiaSetTextProgress(sim_verprogressdialog, _("Reading stimulus..."));
				continue;
			}
			if (keyword.equals("$dumpvars"))
			{
				System.out.println("dumpvars");

				double curtime = 0.0;
				for(;;)
				{
					String sim_verline = getLineFromSimulator();
					if (sim_verline == null) break;
					char chr = sim_verline.charAt(0);
					String restOfLine = sim_verline.substring(1);
					if (chr == '0' || chr == '1' || chr == 'x' || chr == 'z')
					{
						Object entry = symbolTable.get(restOfLine);
						if (entry == null)
						{
							System.out.println("Unknown symbol '" + restOfLine + "' on line " + lineReader.getLineNumber());
							continue;
						}
						if (entry instanceof List) entry = ((List)entry).get(0);
						Simulate.SimDigitalSignal sig = (Simulate.SimDigitalSignal)entry;

						// insert the stimuli
						int state = 0;
						switch (chr)
						{
							case '0': state = Simulate.SimData.LOGIC_LOW  | Simulate.SimData.GATE_STRENGTH;  break;
							case '1': state = Simulate.SimData.LOGIC_HIGH | Simulate.SimData.GATE_STRENGTH;  break;
							case 'x': state = Simulate.SimData.LOGIC_X    | Simulate.SimData.GATE_STRENGTH;  break;
							case 'z': state = Simulate.SimData.LOGIC_Z    | Simulate.SimData.GATE_STRENGTH;  break;
						}
						VerilogStimuli vs = new VerilogStimuli(curtime, state);
						sig.tempList.add(vs);
						continue;
					}
					if (chr == '$')
					{
						if (restOfLine.equals("end")) continue;
						System.out.println("Unknown directive on line " + lineReader.getLineNumber() + ": " + sim_verline);
						continue;
					}
					if (chr == '#')
					{
						curtime = TextUtils.atoi(restOfLine) * sim_timescale;
						continue;
					}
					if (chr == 'b')
					{
						int spacePos = restOfLine.indexOf(' ');
						if (spacePos < 0)
						{
							System.out.println("Bus has missing signal name on line " + lineReader.getLineNumber() + ": " + sim_verline);
							continue;
						}
						String symname = restOfLine.substring(spacePos+1);
						Object entry = symbolTable.get(symname);
						if (entry == null)
						{
							System.out.println("Unknown symbol '" + symname + "' on line " + lineReader.getLineNumber());
							continue;
						}
						if (entry instanceof List) entry = ((List)entry).get(0);
						Simulate.SimDigitalSignal sig = (Simulate.SimDigitalSignal)entry;
						int i = 0;
						for(Iterator it = sig.bussedSignals.iterator(); it.hasNext(); )
						{
							Simulate.SimDigitalSignal subSig = (Simulate.SimDigitalSignal)it.next();
							char bit = restOfLine.charAt(i++);
							int state = 0;
							switch (bit)
							{
								case '0': state = Simulate.SimData.LOGIC_LOW  | Simulate.SimData.GATE_STRENGTH;  break;
								case '1': state = Simulate.SimData.LOGIC_HIGH | Simulate.SimData.GATE_STRENGTH;  break;
								case 'x': state = Simulate.SimData.LOGIC_X    | Simulate.SimData.GATE_STRENGTH;  break;
								case 'z': state = Simulate.SimData.LOGIC_Z    | Simulate.SimData.GATE_STRENGTH;  break;
							}
							VerilogStimuli vs = new VerilogStimuli(curtime, state);
							subSig.tempList.add(vs);
						}
						continue;
					}
					System.out.println("Unknown stimulus on line " + lineReader.getLineNumber() + ": " + sim_verline);
				}
			}
			continue;
		}

		// convert the stimuli
		for(Iterator it = symbolTable.values().iterator(); it.hasNext(); )
		{
			Object entry = it.next();
			List fullList = null;
			if (entry instanceof List)
			{
				fullList = (List)entry;
				entry = fullList.get(0);
			} 
			Simulate.SimDigitalSignal sig = (Simulate.SimDigitalSignal)entry;
			int numStimuli = sig.tempList.size();
			if (numStimuli == 0) continue;
			sig.time = new double[numStimuli];
			sig.state = new int[numStimuli];
			int i = 0;
			for(Iterator sIt = sig.tempList.iterator(); sIt.hasNext(); )
			{
				VerilogStimuli vs = (VerilogStimuli)sIt.next();
				sig.useCommonTime = false;
				sig.time[i] = vs.time;
				sig.state[i] = vs.state;
				i++;
			}
			sig.tempList = null;
			if (fullList != null)
			{
				for(Iterator lIt = fullList.iterator(); lIt.hasNext(); )
				{
					Simulate.SimDigitalSignal oSig = (Simulate.SimDigitalSignal)lIt.next();
					if (oSig.time == null) oSig.time = sig.time;
					if (oSig.state == null) oSig.state = sig.state;
					oSig.useCommonTime = false;
				}
			}
		}
		return sd;
	}

	private void cleanUpScope(List curArray, Simulate.SimData sd)
	{
		if (curArray == null) return;

		String last = null;
		String scope = null;
		int firstEntry = 0;
		int firstIndex = 0;
		int lastIndex = 0;
		int numSignalsInArray = curArray.size();
		for(int j=0; j<numSignalsInArray; j++)
		{
			Simulate.SimDigitalSignal sig = (Simulate.SimDigitalSignal)curArray.get(j);
			int squarePos = sig.signalName.indexOf('[');
			if (squarePos < 0) continue;
			String purename = sig.signalName.substring(0, squarePos);
			int index = TextUtils.atoi(sig.signalName.substring(squarePos+1));
			if (last == null)
			{
				firstEntry = j;
				last = purename;
				firstIndex = lastIndex = index;
				scope = sig.signalContext;
			} else
			{
				if (last.equals(purename)) lastIndex = index; else
				{
					Simulate.SimDigitalSignal arraySig = new Simulate.SimDigitalSignal(sd);
					arraySig.signalName = last + "[" + firstIndex + ":" + lastIndex + "]";
					arraySig.signalContext = scope;
					arraySig.bussedSignals = new ArrayList();
					int width = j - firstEntry;
					for(int i=0; i<width; i++)
					{
						Simulate.SimDigitalSignal subSig = (Simulate.SimDigitalSignal)curArray.get(firstEntry+i);
						arraySig.bussedSignals.add(subSig);
					}
					last = null;
				}
			}
		}
		if (last != null)
		{
			Simulate.SimDigitalSignal arraySig = new Simulate.SimDigitalSignal(sd);
			arraySig.signalName = last + "[" + firstIndex + ":" + lastIndex + "]";
			arraySig.signalContext = scope;
			arraySig.bussedSignals = new ArrayList();
			int width = numSignalsInArray - firstEntry;
			for(int i=0; i<width; i++)
			{
				Simulate.SimDigitalSignal subSig = (Simulate.SimDigitalSignal)curArray.get(firstEntry+i);
				arraySig.bussedSignals.add(subSig);
			}
		}
	}

	private void addSignalToHashMap(Simulate.SimDigitalSignal sig, String symbol, HashMap symbolTable)
	{
		Object entry = symbolTable.get(symbol);
		if (entry == null)
		{
			symbolTable.put(symbol, sig);
		} else if (entry instanceof Simulate.SimDigitalSignal)
		{
			List manySigs = new ArrayList();
			manySigs.add(entry);
			manySigs.add(sig);
			symbolTable.put(symbol, manySigs);
		} else if (entry instanceof List)
		{
			((List)entry).add(sig);
		}
	}

	private void sim_verparsetoend()
		throws IOException
	{
		for(;;)
		{
			String keyword = getNextKeyword();
			if (keyword == null || keyword.equals("$end")) break;
		}
	}

	private String lastLine = null;
	private int linePos;
	private int lineLen;

	private String getNextKeyword()
		throws IOException
	{
		String keyword = null;
		for(;;)
		{
			if (lastLine == null)
			{
				lastLine = getLineFromSimulator();
				if (lastLine == null) break;
				lineLen = lastLine.length();
				linePos = 0;
			}
			if (linePos < lineLen)
			{
				char ch = lastLine.charAt(linePos);
				if (ch == ' ')
				{
					linePos++;
				} else
				{
					int startOfKeyword = linePos;
					for(linePos++; linePos < lineLen; linePos++)
					{
						if (lastLine.charAt(linePos) == ' ') break;
					}
					keyword = lastLine.substring(startOfKeyword, linePos);
				}
			}
			if (linePos >= lineLen) lastLine = null;
			if (keyword != null) break;
		}
		return keyword;
	}

}
