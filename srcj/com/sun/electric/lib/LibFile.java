/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LibFile.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.lib;

import java.net.URL;

/**
 * This class represents a Tool in Electric.  It's here mostly for the name
 * of the tool and the variables attached.  The User holds
 * variables that keep track of the currently selected object, and other
 * useful information.
 */
public class LibFile
{
	/**
	 * The constructor for LibFile is never called.
	 */
	private LibFile()
	{
	}

	private static final LibFile theOne = new LibFile();

	/**
	 * Method to find a library file.
	 * @param fileName the name of the file in the library area.
	 * These files are typically readable dumps of essential files used by everyone.
	 * @return the file path.
	 */
	public static String getLibFile(String fileName)
	{
		URL url = theOne.getClass().getResource(fileName);
		if (url == null) return null;
		// should do url.getInputStream() to open it directly
		String file = url.getFile();
		return file;
	}

	/**
	 * Method to find all files that are SPICE parts libraries.
	 * @return an array of strings that name the SPICE parts libraries.
	 */
	public static String [] getSpicePartsLibraries()
	{
		// until we find a way to search the resources, this will have to do
		String [] libNames = new String[2];
		libNames[0] = "spiceparts";
		libNames[1] = "spicepartsS3";
		return libNames;
	}

}
