/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: HSpiceOut.java
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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class for reading and displaying waveforms from HSpice output.
 * Thease are contained in .tr0 and .pa0 files.
 */
public class HSpiceOut extends Simulate
{
	private boolean isTR0Binary;				/* true if tr0 file is binary */
	private int binaryTR0Size, binaryTR0Position;
	private boolean eofReached;
	private byte [] binaryTR0Buffer;

	// HSPICE name associations from the .pa0 file
	private static class PA0Line
	{
		int     number;
		String  string;
	};

	HSpiceOut() {}

	/**
	 * Method to read an HSpice output file.
	 */
	protected Simulation.SimData readSimulationOutput(URL fileURL, Cell cell)
		throws IOException
	{
		// the .pa0 file has name information
		List pa0List = readPA0File(fileURL);

		// show progress reading .tr0 file
		startProgressDialog("HSpice output", fileURL.getFile());

		// read the actual signal data from the .tr0 file
		Simulation.SimData sd = readTR0File(fileURL, pa0List, cell);

		// stop progress dialog
		stopProgressDialog();

		// return the simulation data
		return sd;
	}

	private List readPA0File(URL fileURL)
		throws IOException
	{
		String tr0File = fileURL.getFile();
		String pa0File = null;
		if (tr0File.endsWith(".tr0"))
		{
			pa0File = tr0File.substring(0, tr0File.length()-4) + ".pa0";
		} else
		{
			pa0File = tr0File + ".pa0";
		}
		URL pa0URL = null;
		try
		{
			pa0URL = new URL(fileURL.getProtocol(), fileURL.getHost(), fileURL.getPort(), pa0File);
		} catch (java.net.MalformedURLException e)
		{
		}
		if (pa0URL == null) return null;
        if (!TextUtils.URLExists(pa0URL)) return null;
		if (openTextInput(pa0URL)) return null;

		List pa0List = new ArrayList();
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
				// save it in a PA0Line object
				PA0Line pl = new PA0Line();
				pl.number = TextUtils.atoi(trimLine, 0, 10);
				pl.string = trimLine.substring(spacePos+1).trim();

				// remove all of the "x" characters at the start of every instance name
				int dotPos = -1;
				for(;;)
				{
					int xPos = dotPos + 1;
					if (pl.string.length() > xPos && pl.string.charAt(xPos) == 'x')
					{
						pl.string = pl.string.substring(0, xPos) + pl.string.substring(xPos+1);
					}
					dotPos = pl.string.indexOf('.', xPos);
					if (dotPos < 0) break;
				}
				pa0List.add(pl);
			}
		}
		closeInput();
		return pa0List;
	}

	private Simulation.SimData readTR0File(URL fileURL, List pa0List, Cell cell)
		throws IOException
	{
		if (openBinaryInput(fileURL)) return null;

		// get number of nodes
		StringBuffer line = new StringBuffer();
		for(int j=0; j<4; j++) line.append((char)getByteFromFile());
		int nodcnt = TextUtils.atoi(line.toString(), 0, 10);

		// get number of special items
		line = new StringBuffer();
		for(int j=0; j<4; j++) line.append((char)getByteFromFile());
		int numnoi = TextUtils.atoi(line.toString(), 0, 10);

		// get number of conditions
		line = new StringBuffer();
		for(int j=0; j<4; j++) line.append((char)getByteFromFile());
		// cndcnt = atoi(line);

		/*
		 * Although this isn't documented anywhere, it appears that the 4th
		 * number in the file is a multiplier for the first, which allows
		 * there to be more than 10000 nodes.
		 */
		line = new StringBuffer();
		for(int j=0; j<4; j++) line.append((char)getByteFromFile());
		int multiplier = TextUtils.atoi(line.toString(), 0, 10);
		nodcnt += multiplier * 10000;
		int numSignals = numnoi + nodcnt - 1;

		// get version number (known to work with 9007, 9601)
		line = new StringBuffer();
		for(int j=0; j<4; j++) line.append((char)getByteFromFile());
		int version = TextUtils.atoi(line.toString(), 0, 10);

		// ignore the unused/title information (4+72 characters over line break)
		for(int j=0; j<76; j++)
		{
			int k = getByteFromFile();
			if (!isTR0Binary && k == '\n') j--;
		}

		// ignore the date/time information (16 characters)
		for(int j=0; j<16; j++) getByteFromFile();

		// ignore the copywrite information (72 characters over line break)
		for(int j=0; j<72; j++)
		{
			int k = getByteFromFile();
			if (!isTR0Binary && k == '\n') j--;
		}

		// get number of sweeps
		line = new StringBuffer();
		for(int j=0; j<4; j++) line.append((char)getByteFromFile());
		int sweepcnt = TextUtils.atoi(line.toString(), 0, 10);

		// ignore the Monte Carlo information (76 characters over line break)
		for(int j=0; j<76; j++)
		{
			int k = getByteFromFile();
			if (!isTR0Binary && k == '\n') j--;
		}

		// get the type of each signal
		int important = numnoi;
		String [] signalNames = new String[numSignals];
		int [] signalTypes = new int[numSignals];
		for(int k=0; k<=numSignals; k++)
		{
			line = new StringBuffer();
			for(int j=0; j<8; j++)
			{
				int l = getByteFromFile();
				line.append((char)l);
				if (!isTR0Binary && l == '\n') j--;
			}
			if (k == 0) continue;
			int l = k - nodcnt;
			if (k < nodcnt) l = k + numnoi - 1;
			signalTypes[l] = TextUtils.atoi(line.toString(), 0, 10);
		}
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
				if (j >= 16) break;
			}
			int l = (j+15) / 16 * 16 - 1;
			for(; j<l; j++)
			{
				int i = getByteFromFile();
				if (!isTR0Binary && i == '\n') { j--;   continue; }
			}
			if (k == 0) continue;

			// convert name if there is a colon in it
			for(j=0; j<line.length(); j++)
			{
				if (line.charAt(j) == ':') break;
				if (!Character.isDigit(line.charAt(j))) break;
			}
			if (j < line.length() && line.charAt(j) == ':')
			{
				l = TextUtils.atoi(line.toString(), 0, 10);
				PA0Line foundPA0Line = null;
				if (pa0List != null)
				{
					for(Iterator it = pa0List.iterator(); it.hasNext(); )
					{
						PA0Line pa0Line = (PA0Line)it.next();
						if (pa0Line.number == l) { foundPA0Line = pa0Line;   break; }
					}
				}
				if (foundPA0Line != null)
				{
					StringBuffer newSB = new StringBuffer();
					newSB.append(foundPA0Line.string);
					newSB.append(line.substring(j+1));
					line = new StringBuffer();
					line.append(newSB.toString());
				}
			}

			if (k < nodcnt) l = k + numnoi - 1; else l = k - nodcnt;
			signalNames[l] = line.toString();
		}

		line = new StringBuffer();
		if (!isTR0Binary)
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
			System.out.println("HSPICE header improperly terminated (got "+line.toString()+")");
			closeInput();
			return null;
		}
		resetBinaryTR0Reader();

		// now read the data
		Simulation.SimData sd = new Simulation.SimData();
		sd.setCell(cell);
		eofReached = false;
		List timeValues = new ArrayList();
		for(int k=0; k<numSignals; k++)
		{
			Simulation.SimAnalogSignal as = new Simulation.SimAnalogSignal(sd);
			as.setCommonTimeUse(true);
			int lastDotPos = signalNames[k].lastIndexOf('.');
			if (lastDotPos >= 0)
			{
				as.setSignalContext(signalNames[k].substring(0, lastDotPos));
				as.setSignalName(signalNames[k].substring(lastDotPos+1));
			} else
			{
				as.setSignalName(signalNames[k]);
			}
			as.tempList = new ArrayList();
		}
		for(;;)
		{
			// get the first number, see if it terminates
			double time = getHSpiceFloat();
			if (eofReached) break;
			timeValues.add(new Double(time));

			// get a row of numbers
			for(int k=1; k<=numSignals; k++)
			{
				double value = getHSpiceFloat();
				if (eofReached) break;
				int l = k - nodcnt;
				if (k < nodcnt) l = k + numnoi - 1;
				Simulation.SimAnalogSignal as = (Simulation.SimAnalogSignal)sd.getSignals().get(l);
				as.tempList.add(new Double(value));
			}
			if (eofReached) break;
		}
		closeInput();

		// convert lists to arrays
		int numEvents = timeValues.size();
		sd.buildCommonTime(numEvents);
		for(int i=0; i<numEvents; i++)
			sd.setCommonTime(i, ((Double)timeValues.get(i)).doubleValue());
		for(int j=0; j<numSignals; j++)
		{
			Simulation.SimAnalogSignal as = (Simulation.SimAnalogSignal)sd.getSignals().get(j);
			as.buildValues(numEvents);
			for(int i=0; i<numEvents; i++)
				as.setValue(i, ((Double)as.tempList.get(i)).doubleValue());
			as.tempList = null;
		}
		return sd;
	}

	/*
	 * Method to reset the binary tr0 block pointer (done between the header and
	 * the data).
	 */
	private void resetBinaryTR0Reader()
	{
		binaryTR0Size = 0;
		binaryTR0Position = 0;
	}

	/*
	 * Method to read the next block of tr0 data.  Skips the first byte if "firstbyteread"
	 * is true.  Returns true on EOF.
	 */
	private boolean readBinaryTR0Block(boolean firstbyteread)
		throws IOException
	{
		// read the first word of a binary tr0 block
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
			bytes = (bytes << 8) | uval;
		}
		updateProgressDialog(4);

		// now read the data
		int amtread = dataInputStream.read(binaryTR0Buffer, 0, bytes);
		if (amtread != bytes) return true;
		updateProgressDialog(bytes);

		// read the trailer count
		int trailer = 0;
		for(int i=0; i<4; i++)
		{
			int uval = dataInputStream.read();
			if (uval == -1) return true;
			trailer = (trailer << 8) | uval;
		}
		if (trailer != bytes) return true;
		updateProgressDialog(4);

		// set pointers for the buffer
		binaryTR0Position = 0;
		binaryTR0Size = bytes;
		return false;
	}

	/*
	 * Method to get the next character from the simulator (file or pipe).
	 * Returns EOF at end of file.
	 */
	private int getByteFromFile()
		throws IOException
	{
		if (byteCount == 0)
		{
			// start of HSPICE file: see if it is binary or ascii
			int i = dataInputStream.read();
			if (i == -1) return(i);
			updateProgressDialog(1);
			if (i == 0)
			{
				isTR0Binary = true;
				binaryTR0Buffer = new byte[8192];
				if (readBinaryTR0Block(true)) return(-1);
			} else
			{
				isTR0Binary = false;
				return(i);
			}
		}
		if (isTR0Binary)
		{
			if (binaryTR0Position >= binaryTR0Size)
			{
				if (readBinaryTR0Block(false))
					return(-1);
			}
			int val = binaryTR0Buffer[binaryTR0Position];
			binaryTR0Position++;
			return val&0xFF;
		}
		int i = dataInputStream.read();
		updateProgressDialog(1);
		return i;
	}

	/*
	 * Method to read the next floating point number from the HSPICE file into "val".
	 * Returns positive on error, negative on EOF, zero if OK.
	 */
	private double getHSpiceFloat()
		throws IOException
	{
		if (!isTR0Binary)
		{
			StringBuffer line = new StringBuffer();
			for(int j=0; j<11; j++)
			{
				int l = getByteFromFile();
				if (l == -1) { eofReached = true;   return 0; }
				line.append((char)l);
				if (l == '\n') j--;
			}
			String result = line.toString();
			if (result.equals("0.10000E+31")) { eofReached = true;   return 0; }
			return TextUtils.atof(result);
		}

		// binary format
		int fi0 = getByteFromFile() & 0xFF;
		int fi1 = getByteFromFile() & 0xFF;
		int fi2 = getByteFromFile() & 0xFF;
		int fi3 = getByteFromFile() & 0xFF;
		int fi = (fi0 << 24) | (fi1 << 16) | (fi2 << 8) | fi3;
		float f = Float.intBitsToFloat(fi);
		
		if (f > 1.00000000E30 && f < 1.00000002E30) { eofReached = true;   return 0; }
		return (double)f;
	}

}
