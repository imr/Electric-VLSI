/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IO.java
 * MOSIS CMOS PLA Generator.
 * Originally written by Wallace Kroeker at the University of Calgary
 * Translated to Java by Steven Rubin, Sun Microsystems.
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
package com.sun.electric.tool.generator.cmosPLA;

import com.sun.electric.database.text.TextUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * Class to generate the I/O part of MOSIS CMOS PLAs.
 */
public class IO
{
	private int width, height;
	private int widthIn, heightIn;
	private LineNumberReader lnr;
	private String curLine;

	IO() {}

	int getWidth() { return width; }

	int getHeight() { return height; }

	int getWidthIn() { return widthIn; }

	int getHeightIn() { return heightIn; }

	/**
	 * Method to read the header from a PLA personality file.
	 * @param fileName the name of the file.
	 * @return false on error.
	 */
	boolean readHeader(String fileName)
	{
		URL url = TextUtils.makeURLToFile(fileName);
		try
		{
			URLConnection urlCon = url.openConnection();
			InputStreamReader is = new InputStreamReader(urlCon.getInputStream());
			lnr = new LineNumberReader(is);

			// read a line from the file
			String line = lnr.readLine();
			if (line == null) return false;
			line = line.trim();
			height = TextUtils.atoi(line);
			int sep = line.indexOf(' ');
			if (sep < 0) return false;
			line = line.substring(sep).trim();
			width = TextUtils.atoi(line);

			System.out.println("PLA table height=" + height + ", width=" + width);

			widthIn = width;
			heightIn = height;
			height = (((((height - 1)/4)+1)*5)+1);
			if (height > PLA.MAX_COL_SIZE)
			{
				System.out.println("PLA height exceeded");
				return false;
			}
			width = (((((width - 1)/4)+1)*5)+1);
			if (width > PLA.MAX_COL_SIZE)
			{
				System.out.println("PLA width exceeded");
				return false;
			}
			curLine = "";
			return true;
		} catch (IOException e)
		{
			System.out.println("Error reading " + fileName);
			return false;
		}
	}

	int [] readRow()
	{
		int read = 0;
		int [] row = new int[width];
		for (int i = 0; i < width; i++)
		{
			// Ground Strapping slot
			if ((i % 5) == 0) row[i] = -2; else
			{
				if (read < widthIn)
				{
					try
					{
						row[i] = getNextInt();
					} catch (IOException e) { return null; }
					read++;
				} else row[i] = -1;
			}
		}

		return row;
	}

	void done()
	{
		try
		{
			lnr.close();
		} catch (IOException e) {}
	}

	private int getNextInt()
		throws IOException
	{
		while (curLine.length() == 0)
		{
			curLine = lnr.readLine();
			curLine = curLine.trim();
			if (curLine.length() > 0) break;
		}
		int value = TextUtils.atoi(curLine);
		int sepPos = curLine.indexOf(' ');
		if (sepPos < 0) curLine = ""; else
			curLine = curLine.substring(sepPos).trim();
		return value;
	}
}
