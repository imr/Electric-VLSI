/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Output.java
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
package com.sun.electric.tool.io;

import com.sun.electric.database.hierarchy.Library;

import java.io.*;

public class Output
{
	String filePath;
	FileOutputStream fileOutputStream;
	DataOutputStream dataOutputStream;

	/**
	 * Function is a typesafe enum class that describes the types of files that can be written.
	 */
	public static class ExportType
	{
		private final String name;

		private ExportType(String name)
		{
			this.name = name;
		}

		public String toString() { return name; }

		/** binary output */		public static final ExportType BINARY=   new ExportType("binary");
		/** text output */			public static final ExportType TEXT=     new ExportType("text");
		/** CIF output */			public static final ExportType CIF=      new ExportType("CIF");
		/** GDS output */			public static final ExportType GDS=      new ExportType("GDS");
	}

	// ------------------------- private data ----------------------------

	// ---------------------- private and protected methods -----------------

	protected Output()
	{
	}

	// ----------------------- public methods -------------------------------

	public boolean writeLib(Library lib) { return true; }

	/**
	 * Routine to write Library "lib" with type "type".
	 */
	public static boolean writeLibrary(Library lib, ExportType type)
	{
		Output out;

		// handle different file types
		Library.Name n;
		if (lib.getLibFile() != null) n = Library.Name.newInstance(lib.getLibFile()); else
			n = Library.Name.newInstance(lib.getLibName());
		if (type == ExportType.BINARY)
		{
			out = (Output)new OutputBinary();
			n.setExtension("elib");
		} else if (type == ExportType.TEXT)
		{
//			out = (Output)new OutputText();
//			n.setExtension("txt");

			// no text reader yet, see if an elib can be found
			out = (Output)new OutputBinary();
			n.setExtension("elib");
		} else
		{
			System.out.println("Unknown export type: " + type);
			return true;
		}

		out.filePath = n.makeName();
		try
		{
			out.fileOutputStream = new FileOutputStream(out.filePath);
		} catch (FileNotFoundException e)
		{
			System.out.println("Could not write file " + out.filePath);
			return true;
		}
		out.dataOutputStream = new DataOutputStream(out.fileOutputStream);
		boolean error = out.writeLib(lib);
		try
		{
			out.fileOutputStream.close();
		} catch (IOException e)
		{
			System.out.println("Error closing " + out.filePath);
			return true;
		}
		if (error)
		{
			System.out.println("Error writing library");
			return true;
		}
		return false;
	}
}
