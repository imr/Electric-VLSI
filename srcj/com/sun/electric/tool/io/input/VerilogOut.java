/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VerilogOut.java
 * Input/output tool: reader for Verilog output (.v)
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
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.simulation.DigitalSignal;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Class for reading and displaying waveforms from Verilog output.
 * Thease are contained in .v files.
 */
public class VerilogOut extends Simulate
{
	private static class VerilogStimuli
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
	protected Stimuli readSimulationOutput(URL fileURL, Cell cell)
		throws IOException
	{
		// open the file
		if (openTextInput(fileURL)) return null;

		// show progress reading .dump file
		startProgressDialog("Verilog output", fileURL.getFile());

		// read the actual signal data from the .dump file
		Stimuli sd = readVerilogFile(cell);

		// stop progress dialog, close the file
		stopProgressDialog();
		closeInput();

		// return the simulation data
		return sd;
	}

	private Stimuli readVerilogFile(Cell cell)
		throws IOException
	{
		Stimuli sd = new Stimuli();
		sd.setCell(cell);
		double timeScale = 1.0;
		String currentScope = "";
		int curLevel = 0;
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
				parseToEnd();
				continue;
			}
			if (keyword.equals("$timescale"))
			{
				String units = getNextKeyword();
				timeScale = TextUtils.atof(units);
				if (units.endsWith("ms")) timeScale /= 1000.0; else
				if (units.endsWith("us")) timeScale /= 1000000.0; else
				if (units.endsWith("ns")) timeScale /= 1000000000.0; else
				if (units.endsWith("ps")) timeScale /= 1000000000000.0; else
				if (units.endsWith("fs")) timeScale /= 1000000000000000.0;
				parseToEnd();
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
					if (currentScope.length() > 0) currentScope += ".";
					currentScope += scopeName;
					curLevel++;
					curArray = new ArrayList();
				}
				parseToEnd();
				continue;
			}

			if (keyword.equals("$upscope"))
			{
				if (curLevel <= 0 || currentScope.length() == 0)
				{
					System.out.println("Unbalanced $upscope on line " + lineReader.getLineNumber());
					continue;
				}

				// scan for arrays
				cleanUpScope(curArray, sd);
				curArray = new ArrayList();

				int dotPos = currentScope.lastIndexOf('.');
				if (dotPos >= 0)
				{
					currentScope = currentScope.substring(0, dotPos);
					curLevel--;
				}
				parseToEnd();
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
						parseToEnd();
					}
					numSignals++;

					DigitalSignal sig = new DigitalSignal(sd);
					sig.setSignalName(signalName + index);
					sig.setSignalContext(currentScope);
					sig.tempList = new ArrayList();

					if (index.length() > 0 && width == 1)
					{
						curArray.add(sig);
					} else
					{
						sd.addSignal(sig);
					}

					if (width > 1)
					{
						// create fake signals for the individual entries
						sig.setSignalName(signalName + "[0:" + (width-1) + "]");
						sig.buildBussedSignalList();
						for(int i=0; i<width; i++)
						{
							DigitalSignal subSig = new DigitalSignal(sd);
							subSig.setSignalName(signalName + "[" + i + "]");
							subSig.setSignalContext(currentScope);
							subSig.tempList = new ArrayList();
							sig.addToBussedSignalList(subSig);
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
				parseToEnd();
				System.out.println("Found " + numSignals + " signal names");
				continue;
			}
			if (keyword.equals("$dumpvars"))
			{
				double curTime = 0.0;
				for(;;)
				{
					String currentLine = getLineFromSimulator();
					if (currentLine == null) break;
					char chr = currentLine.charAt(0);
					String restOfLine = currentLine.substring(1);
					if (chr == '0' || chr == '1' || chr == 'x' || chr == 'z')
					{
						Object entry = symbolTable.get(restOfLine);
						if (entry == null)
						{
							System.out.println("Unknown symbol '" + restOfLine + "' on line " + lineReader.getLineNumber());
							continue;
						}
						if (entry instanceof List) entry = ((List)entry).get(0);
						DigitalSignal sig = (DigitalSignal)entry;

						// insert the stimuli
						int state = 0;
						switch (chr)
						{
							case '0': state = Stimuli.LOGIC_LOW  | Stimuli.GATE_STRENGTH;  break;
							case '1': state = Stimuli.LOGIC_HIGH | Stimuli.GATE_STRENGTH;  break;
							case 'x': state = Stimuli.LOGIC_X    | Stimuli.GATE_STRENGTH;  break;
							case 'z': state = Stimuli.LOGIC_Z    | Stimuli.GATE_STRENGTH;  break;
						}
						VerilogStimuli vs = new VerilogStimuli(curTime, state);
						sig.tempList.add(vs);
						continue;
					}
					if (chr == '$')
					{
						if (restOfLine.equals("end")) continue;
						System.out.println("Unknown directive on line " + lineReader.getLineNumber() + ": " + currentLine);
						continue;
					}
					if (chr == '#')
					{
						curTime = TextUtils.atoi(restOfLine) * timeScale;
						continue;
					}
					if (chr == 'b')
					{
						int spacePos = restOfLine.indexOf(' ');
						if (spacePos < 0)
						{
							System.out.println("Bus has missing signal name on line " + lineReader.getLineNumber() + ": " + currentLine);
							continue;
						}
						String symName = restOfLine.substring(spacePos+1);
						Object entry = symbolTable.get(symName);
						if (entry == null)
						{
							System.out.println("Unknown symbol '" + symName + "' on line " + lineReader.getLineNumber());
							continue;
						}
						if (entry instanceof List) entry = ((List)entry).get(0);
						DigitalSignal sig = (DigitalSignal)entry;
						int i = 0;
						for(Iterator it = sig.getBussedSignals().iterator(); it.hasNext(); )
						{
							DigitalSignal subSig = (DigitalSignal)it.next();
							char bit = restOfLine.charAt(i++);
							int state = 0;
							switch (bit)
							{
								case '0': state = Stimuli.LOGIC_LOW  | Stimuli.GATE_STRENGTH;  break;
								case '1': state = Stimuli.LOGIC_HIGH | Stimuli.GATE_STRENGTH;  break;
								case 'x': state = Stimuli.LOGIC_X    | Stimuli.GATE_STRENGTH;  break;
								case 'z': state = Stimuli.LOGIC_Z    | Stimuli.GATE_STRENGTH;  break;
							}
							VerilogStimuli vs = new VerilogStimuli(curTime, state);
							subSig.tempList.add(vs);
						}
						continue;
					}
					System.out.println("Unknown stimulus on line " + lineReader.getLineNumber() + ": " + currentLine);
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
			DigitalSignal sig = (DigitalSignal)entry;
			int numStimuli = sig.tempList.size();
			if (numStimuli == 0) continue;
			sig.buildTime(numStimuli);
			sig.buildState(numStimuli);
			int i = 0;
			for(Iterator sIt = sig.tempList.iterator(); sIt.hasNext(); )
			{
				VerilogStimuli vs = (VerilogStimuli)sIt.next();
				sig.setTime(i, vs.time);
				sig.setState(i, vs.state);
				i++;
			}
			sig.tempList = null;
			if (fullList != null)
			{
				for(Iterator lIt = fullList.iterator(); lIt.hasNext(); )
				{
					DigitalSignal oSig = (DigitalSignal)lIt.next();
					if (oSig.getTimeVector() == null) oSig.setTimeVector(sig.getTimeVector());
					if (oSig.getStateVector() == null) oSig.setStateVector(sig.getStateVector());
				}
			}
		}

		// remove singular top-level signal name
		String singularPrefix = null;
		for(Iterator it = sd.getSignals().iterator(); it.hasNext(); )
		{
			Signal sSig = (Signal)it.next();
			String context = sSig.getSignalContext();
			if (context == null) { singularPrefix = null;   break; }
			int dotPos = context.indexOf('.');
			if (dotPos >= 0) context = context.substring(0, dotPos);
			if (singularPrefix == null) singularPrefix = context; else
			{
				if (!singularPrefix.equals(context)) { singularPrefix = null;   break; }
			} 
		}
		if (singularPrefix != null)
		{
			int len = singularPrefix.length();
			for(Iterator it = sd.getSignals().iterator(); it.hasNext(); )
			{
				Signal sSig = (Signal)it.next();
				String context = sSig.getSignalContext();
				if (context == null || context.length() <= len) sSig.setSignalContext(null); else
					sSig.setSignalContext(context.substring(len+1));
			}
		}
		return sd;
	}

	private void cleanUpScope(List curArray, Stimuli sd)
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
			DigitalSignal sig = (DigitalSignal)curArray.get(j);
			int squarePos = sig.getSignalName().indexOf('[');
			if (squarePos < 0) continue;
			String purename = sig.getSignalName().substring(0, squarePos);
			int index = TextUtils.atoi(sig.getSignalName().substring(squarePos+1));
			if (last == null)
			{
				firstEntry = j;
				last = purename;
				firstIndex = lastIndex = index;
				scope = sig.getSignalContext();
			} else
			{
				if (last.equals(purename)) lastIndex = index; else
				{
					DigitalSignal arraySig = new DigitalSignal(sd);
					arraySig.setSignalName(last + "[" + firstIndex + ":" + lastIndex + "]");
					arraySig.setSignalContext(scope);
					arraySig.buildBussedSignalList();
					int width = j - firstEntry;
					for(int i=0; i<width; i++)
					{
						DigitalSignal subSig = (DigitalSignal)curArray.get(firstEntry+i);
						arraySig.addToBussedSignalList(subSig);
					}
					last = null;
				}
			}
		}
		if (last != null)
		{
			DigitalSignal arraySig = new DigitalSignal(sd);
			arraySig.setSignalName(last + "[" + firstIndex + ":" + lastIndex + "]");
			arraySig.setSignalContext(scope);
			arraySig.buildBussedSignalList();
			int width = numSignalsInArray - firstEntry;
			for(int i=0; i<width; i++)
			{
				DigitalSignal subSig = (DigitalSignal)curArray.get(firstEntry+i);
				arraySig.addToBussedSignalList(subSig);
			}
		}
	}

	private void addSignalToHashMap(DigitalSignal sig, String symbol, HashMap symbolTable)
	{
		Object entry = symbolTable.get(symbol);
		if (entry == null)
		{
			symbolTable.put(symbol, sig);
		} else if (entry instanceof DigitalSignal)
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

	private void parseToEnd()
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
