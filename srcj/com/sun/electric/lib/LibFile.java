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

import com.sun.electric.database.text.TextUtils;

import java.io.File;
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
	public static URL getLibFile(String fileName)
	{
		URL url = theOne.getClass().getResource(fileName);
		return url;
	}

	/**
	 * Method to find all files that are SPICE parts libraries.
	 * @return an array of strings that name the SPICE parts libraries.
	 */
	public static String [] getSpicePartsLibraries()
	{
//		String sampleSpiceParts = "spiceparts.jelib";
//		URL url = theOne.getClass().getResource(sampleSpiceParts);
//		String pathToResources = TextUtils.getFilePath(url);
//		int pos = pathToResources.indexOf(sampleSpiceParts);
//		if (pos >= 0)
//			pathToResources = pathToResources.substring(0, pos);
//		File resourceDir = new File(pathToResources);
//		String [] files = resourceDir.list();
//
//		List<String> spiceLibs = new ArrayList<String>();
//		for(int i=0; i<files.length; i++)
//		{
//			if (files[i].startsWith("spiceparts") && files[i].endsWith(".jelib"))
//				spiceLibs.add(files[i].substring(0, files[i].length()-6));
//		}
//
//		// until we find a way to search the resources, this will have to do
//		String [] libNames = new String[spiceLibs.size()];
//		for(int i=0; i<spiceLibs.size(); i++)
//			libNames[i] = spiceLibs.get(i);
		String [] libNames = new String[2];
		libNames[0] = "spiceparts";
		libNames[1] = "spicepartsS3";
		return libNames;
	}

}
