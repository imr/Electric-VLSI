/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Input.java
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
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class Input
{
	String filePath;
	Library lib;
	FileInputStream fileInputStream;
	DataInputStream dataInputStream;
	/** static list of all libraries in Electric */			protected static List newLibraries = new ArrayList();

	/**
	 * Function is a typesafe enum class that describes the types of files that can be read.
	 */
	public static class ImportType
	{
		private final String name;

		private ImportType(String name)
		{
			this.name = name;
		}

		public String toString() { return name; }

		/** binary input */			public static final ImportType BINARY=   new ImportType("binary");
		/** text input */			public static final ImportType TEXT=     new ImportType("text");
		/** CIF input */			public static final ImportType CIF=      new ImportType("CIF");
		/** GDS input */			public static final ImportType GDS=      new ImportType("GDS");
	}

	public static class FakeCell
	{
		String cellName;
		NodeProto firstInCell;
	}

	// ------------------------- private data ----------------------------

	// ---------------------- private and protected methods -----------------

	protected Input()
	{
	}

	static String mainLibDirectory = null;

	// ----------------------- public methods -------------------------------

	public boolean ReadLib() { return true; }

	/**
	 * Routine to read disk file "fileName" which is of type "type".
	 * The file is read into library "lib".  If "lib" is null, one is created.
	 * Also, if "lib" is null, this is an entry-level library read.
	 * If "lib" is not null, this is a recursive read caused by a cross-library
	 * reference from inside another library.
	 */
	public static Library ReadLibrary(String fileName, Library lib, ImportType type)
	{
		Input in;

		// break file name into library name and path; determine whether this is top-level
		Library.LibraryName n = Library.LibraryName.newInstance(fileName);
		boolean topLevel = false;
		if (lib == null)
		{
			mainLibDirectory = n.getPath();
			topLevel = true;
		}

		// handle different file types
		if (type == ImportType.BINARY)
		{
			in = (Input)new InputBinary();
		} else if (type == ImportType.TEXT)
		{
//			in = (Input)new InputText();
			
			// no text reader yet, see if an elib can be found
			n.setExtension("elib");
			fileName = n.makeName();
			in = (Input)new InputBinary();
		} else
		{
			System.out.println("Unknown import type: " + type);
			return null;
		}
		if (lib == null)
			lib = Library.newInstance(n.getName(), fileName);

		// add to the list of libraries read at once
		if (topLevel) newLibraries.clear();
		newLibraries.add(lib);

		in.filePath = fileName;
		in.lib = lib;
		try
		{
			in.fileInputStream = new FileInputStream(fileName);
		} catch (FileNotFoundException e)
		{
			System.out.println("Could not find file " + fileName);
			if (topLevel) mainLibDirectory = null;
			return null;
		}
		in.dataInputStream = new DataInputStream(in.fileInputStream);
		boolean error = in.ReadLib();
		try
		{
			in.fileInputStream.close();
		} catch (IOException e)
		{
			System.out.println("Error closing " + fileName);
			return null;
		}
		if (error)
		{
			System.out.println("Error reading library");
			if (topLevel) mainLibDirectory = null;
			return null;
		}
//		if (topLevel)
//		{
//			mainLibDirectory = null;
//			System.out.println("Top level done.  Libraries are");
//			for(Iterator it = newLibraries.iterator(); it.hasNext(); )
//			{
//				Library l = (Library)it.next();
//				System.out.println("   Library " + l.getLibName());
//				for(Iterator cit = l.getCells(); cit.hasNext(); )
//				{
//					Cell cell = (Cell)cit.next();
//					for(Iterator eit = cell.getPorts(); eit.hasNext(); )
//					{
//						Export pp = (Export)eit.next();
//						if (pp.getOriginalNode() != null && pp.getOriginalPort() != null) continue;
//
//						// must do final conversion
//						Object subPort = pp.getPureOriginalPort();
//						if (subPort instanceof PortProto)
//						{
//							System.out.println("correcting export " + pp.getProtoName() + " of cell " + cell.getProtoName());
//							NodeInst subNode = pp.getOriginalNode();
//							PortInst pi = subNode.findPortInst(((PortProto)subPort).getProtoName());
//							pp.lowLevelSetOriginalPort(pi);
//						}
//					}
//				}
//			}
//		}
		return in.lib;
	}
}
