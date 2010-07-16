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
package com.sun.electric.tool.io.input.verilog;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.simulation.BusSample;
import com.sun.electric.tool.simulation.DigitalSample;
import com.sun.electric.tool.simulation.MutableSignal;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.simulation.SignalCollection;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.simulation.DigitalSample.Strength;
import com.sun.electric.tool.simulation.DigitalSample.Value;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for reading and displaying waveforms from Verilog output.
 * These are contained in .v files.
 */
public class VerilogOut extends Input<Stimuli>
{
	public VerilogOut() {}

	/**
	 * Method to read an Verilog output file.
	 */
	protected Stimuli processInput(URL fileURL, Cell cell, Stimuli sd)
		throws IOException
	{
        sd.setNetDelimiter(" ");

		// open the file
		if (openTextInput(fileURL)) return sd;

		// show progress reading .dump file
		startProgressDialog("Verilog output", fileURL.getFile());

		// read the actual signal data from the .dump file
		readVerilogFile(cell, sd);

		// stop progress dialog, close the file
		stopProgressDialog();
		closeInput();

        return sd;
	}

	private void readVerilogFile(Cell cell, Stimuli sd)
		throws IOException
	{
		SignalCollection sc = Stimuli.newSignalCollection(sd, "SIGNALS");
		Map<String,Signal<DigitalSample>[]> busMembers = new HashMap<String,Signal<DigitalSample>[]>();
		double timeScale = 1.0;
		String currentScope = "";
		int curLevel = 0;
		int numSignals = 0;
		Map<String,Object> symbolTable = new HashMap<String,Object>();
		List<Signal<?>> curArray = null;
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
//					cleanUpScope(curArray, an);
					sd.makeBusSignals(curArray, sc);
					curArray = new ArrayList<Signal<?>>();

					String scopeName = getNextKeyword();
					if (scopeName == null) break;
					if (currentScope.length() > 0) currentScope += ".";
					currentScope += scopeName;
					curLevel++;
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
//				cleanUpScope(curArray, an);
				sd.makeBusSignals(curArray, sc);
				curArray = new ArrayList<Signal<?>>();

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
					varName.equals("parameter") || varName.equals("trireg") ||
					varName.equals("in") || varName.equals("out") ||
					varName.equals("inout"))
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

                    Signal<?> sig = null;
                    if (width <= 1)
                    {
                        sig = DigitalSample.createSignal(sc, sd, signalName + index, currentScope);
                        if (index.length() > 0) curArray.add(sig);

                        // put it in the symbol table
    					addSignalToHashMap(sig, symbol, symbolTable);
                    } else
                    {
                        Signal<DigitalSample>[] subsigs = (Signal<DigitalSample>[])new Signal[width];
						for(int i=0; i<width; i++)
						{
                            subsigs[i] = DigitalSample.createSignal(sc, sd, signalName + "[" + i + "]", currentScope);
						}
                        sig = BusSample.createSignal(sc, sd, signalName + "[0:" + (width-1) + "]", currentScope, true, subsigs);

                        // put it in the bus symbol table
                		busMembers.put(symbol, subsigs);
					}
					numSignals++;

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
					String currentLine = getLineAndUpdateProgress();
					if (currentLine == null) break;
					char chr = currentLine.charAt(0);
					String restOfLine = currentLine.substring(1);
					if (chr == '0' || chr == '1' || chr == 'x' || chr == 'X' ||
						chr == 'z' || chr == 'Z')
					{
						Object entry = symbolTable.get(restOfLine);
						if (entry == null)
						{
							System.out.println("Unknown symbol '" + restOfLine + "' on line " + lineReader.getLineNumber());
							continue;
						}
						if (entry instanceof List<?>) entry = ((List<Signal<?>>)entry).get(0);
						Signal<DigitalSample> sig = (Signal<DigitalSample>)entry;

						// insert the stimuli
						DigitalSample ds = null;
						switch (chr)
						{
							case '0': ds = DigitalSample.getSample(Value.LOW, Strength.LARGE_CAPACITANCE);   break;
							case '1': ds = DigitalSample.getSample(Value.HIGH, Strength.LARGE_CAPACITANCE);  break;
							case 'X':
							case 'x': ds = DigitalSample.getSample(Value.X, Strength.LARGE_CAPACITANCE);     break;
							case 'Z':
							case 'z': ds = DigitalSample.getSample(Value.Z, Strength.HIGH_IMPEDANCE);        break;
						}
			            ((MutableSignal<DigitalSample>)sig).addSample(curTime, ds);
						continue;
					}

					if (chr == '$')
					{
						if (restOfLine.equals("end")) continue;
						if (restOfLine.equals("dumpon")) continue;
						if (restOfLine.equals("dumpoff")) continue;
						if (restOfLine.equals("dumpall")) continue;
						System.out.println("Unknown directive on line " + lineReader.getLineNumber() + ": " + currentLine);
						continue;
					}
					if (chr == '#')
					{
						curTime = TextUtils.atof(restOfLine) * timeScale;
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
                		Signal<DigitalSample>[] members = busMembers.get(symName);
						for(int i=0; i<members.length; i++)
						{
							Signal<DigitalSample> subSig = members[i];
							char bit = restOfLine.charAt(i++);
							DigitalSample ds = null;
							switch (bit)
							{
								case '0': ds = DigitalSample.getSample(Value.LOW, Strength.LARGE_CAPACITANCE);   break;
								case '1': ds = DigitalSample.getSample(Value.HIGH, Strength.LARGE_CAPACITANCE);  break;
								case 'X':
								case 'x': ds = DigitalSample.getSample(Value.X, Strength.LARGE_CAPACITANCE);     break;
								case 'Z':
								case 'z': ds = DigitalSample.getSample(Value.Z, Strength.HIGH_IMPEDANCE);        break;
							}
				            ((MutableSignal<DigitalSample>)subSig).addSample(curTime, ds);
						}
						continue;
					}
					System.out.println("Unknown stimulus on line " + lineReader.getLineNumber() + ": " + currentLine);
				}
			}
			continue;
		}

		// remove singular top-level signal name (this code also occurs in HSpiceOut.readTRDCACFile)
        /*
		String singularPrefix = null;
		for(Signal sSig : an.getSignals())
		{
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
			for(Signal sSig : an.getSignals())
			{
				String context = sSig.getSignalContext();
				if (context == null || context.length() <= len) sSig.setSignalContext(null); else
				{
					sSig.setSignalContext(context.substring(len+1));
				}
			}
		}
        */
	}

	private void addSignalToHashMap(Signal<?> sig, String symbol, Map<String,Object> symbolTable)
	{
		Object entry = symbolTable.get(symbol);
		if (entry == null)
		{
			symbolTable.put(symbol, sig);
		} else if (entry instanceof Signal<?>)
		{
			List<Signal<?>> manySigs = new ArrayList<Signal<?>>();
			manySigs.add((Signal<?>)entry);
			manySigs.add(sig);
			symbolTable.put(symbol, manySigs);
		} else if (entry instanceof List<?>)
		{
			((List<Signal<?>>)entry).add(sig);
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
				lastLine = getLineAndUpdateProgress();
				if (lastLine == null) break;
				lineLen = lastLine.length();
				linePos = 0;
			}
			if (linePos < lineLen)
			{
				char ch = lastLine.charAt(linePos);
				if (ch == ' ' || ch == '\t')
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
