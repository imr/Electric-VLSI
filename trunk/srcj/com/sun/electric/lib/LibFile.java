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
package com.sun.electric.lib;

import com.sun.electric.database.text.TextUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to manage files in the "library area" of Electric
 */
public class LibFile
{
	/**
	 * The constructor for LibFile is never called.
	 */
	private LibFile() {}

	/**
	 * Singleton instance of this class used to find the package.
	 */
	private static final LibFile theOne = new LibFile();

	/**
	 * Method to find a library file.
	 * @param fileName the name of the file in the library area.
	 * These files are typically readable dumps of essential files used by everyone.
	 * @return the file path.
	 */
	public static URL getLibFile(String fileName)
	{
		URL url = theOne.getClass().getResource(fileName);
		return url;
	}

	/**
	 * Method to find all files that are SPICE parts libraries.
	 * These are the resources in this package that begin with the name "spiceparts".
	 * @return an array of strings that name the SPICE parts libraries.
	 */
	public static String [] getSpicePartsLibraries()
	{
		// build a list of resources in the library package
		List<String> spiceLibNames = new ArrayList<String>();
		String classPath = theOne.getClass().getPackage().getName();
		List<String> spiceLibs = TextUtils.getAllResources(classPath);
		for(String s : spiceLibs)
		{
			if (s.startsWith("spiceparts"))
			{
				int dotPos = s.indexOf('.');
				if (dotPos > 0) s = s.substring(0, dotPos);
				spiceLibNames.add(s);
			}
		}

		// if no names are found, build a fake list
		if (spiceLibNames.size() == 0)
		{
			System.out.println("Warning: Could not find list of built-in Spice libraries");
			spiceLibNames.add("spiceparts");
			spiceLibNames.add("spicepartsS3");
		}

		// make the array
		String [] libNames = new String[spiceLibNames.size()];
		for(int i=0; i<spiceLibNames.size(); i++)
			libNames[i] = spiceLibNames.get(i);
		return libNames;
	}

}
