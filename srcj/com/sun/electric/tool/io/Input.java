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

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.user.ui.ProgressDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.ArrayList;

/**
 * This class manages reading files in different formats.
 * The class is subclassed by the different file readers.
 */
public class Input
{
	private static final int READ_BUFFER_SIZE = 65536;
	
	/** Name of the file being input. */					protected String filePath;
	/** The Library being input. */							protected Library lib;
	/** The raw input stream. */							protected FileInputStream fileInputStream;
	/** The binary input stream. */							protected DataInputStream dataInputStream;
	/** The length of the file. */							protected long fileLength;
	/** The progress during input. */						protected static ProgressDialog progress = null;
	/** the path to the library being read. */				protected static String mainLibDirectory = null;
	/** static list of all libraries in Electric */			private static List newLibraries = new ArrayList();

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

		/**
		 * Returns a printable version of this ImportType.
		 * @return a printable version of this ImportType.
		 */
		public String toString() { return name; }

		/** Defines binary input. */		public static final ImportType BINARY =   new ImportType("binary");
		/** Defines text input. */			public static final ImportType TEXT =     new ImportType("text");
		/** Defines CIF input. */			public static final ImportType CIF =      new ImportType("CIF");
		/** Defines GDS input. */			public static final ImportType GDS =      new ImportType("GDS");
	}

	/**
	 * This class is used to convert old "facet" style Libraries to pure Cell Libraries.
	 */
	protected static class FakeCell
	{
		String cellName;
		NodeProto firstInCell;
	}

	// ---------------------- private and protected methods -----------------

	Input()
	{
	}

	/**
	 * Routine to read a Library.
	 * This method is never called.
	 * Instead, it is always overridden by the appropriate read subclass.
	 * @return true on error.
	 */
	protected boolean readLib() { return true; }

	// ----------------------- public methods -------------------------------

	/**
	 * Routine to read a Library from disk.
	 * @param fileName the path to the disk file.
	 * @param type the type of library file (BINARY .elib, CIF, GDS, etc.)
	 * @return the read Library, or null if an error occurred.
	 */
	public static Library readLibrary(String fileName, ImportType type)
	{
		long startTime = System.currentTimeMillis();
		//Undo.noUndoAllowed();
		Undo.changesQuiet(true);
		Library lib = readALibrary(fileName, null, type);
		Undo.changesQuiet(false);
		if (lib != null)
		{
			long endTime = System.currentTimeMillis();
			float finalTime = (endTime - startTime) / 1000F;
			System.out.println("Library " + fileName + " read, took " + finalTime + " seconds");
		}
		return lib;
	}

	/**
	 * Routine to read a single disk file.
	 * @param fileName the path to the disk file.
	 * @param lib the Library to read.
	 * If the "lib" is null, this is an entry-level library read, and one is created.
	 * If "lib" is not null, this is a recursive read caused by a cross-library
	 * reference from inside another library.
	 * @param type the type of library file (BINARY .elib, CIF, GDS, etc.)
	 * @return the read Library, or null if an error occurred.
	 */
	protected static Library readALibrary(String fileName, Library lib, ImportType type)
	{
		Input in;

		// break file name into library name and path; determine whether this is top-level
		Library.Name n = Library.Name.newInstance(fileName);
		boolean topLevel = false;
		if (lib == null)
		{
			mainLibDirectory = n.getPath() + File.separator;
			topLevel = true;
		}

		// handle different file types
		if (type == ImportType.BINARY)
		{
			in = (Input)new InputBinary();
		} else if (type == ImportType.TEXT)
		{
			in = (Input)new InputText();
		} else
		{
			System.out.println("Unknown import type: " + type);
			return null;
		}
		
		if (lib == null)
		{
			lib = Library.findLibrary(n.getName());
			if (lib != null)
			{
				// library already exists, prompt for save
				System.out.println("Library already exists: overwriting");
				lib.erase();
			} else
			{
				lib = Library.newInstance(n.getName(), fileName);
			}
		}

		// add to the list of libraries read at once
		if (topLevel) newLibraries.clear();
		newLibraries.add(lib);

		in.filePath = fileName;
		in.lib = lib;
		File file = new File(fileName);
		in.fileLength = file.length(); 
		try
		{
			in.fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e)
		{
			System.out.println("Could not find file " + fileName);
			if (topLevel) mainLibDirectory = null;
			return null;
		}
		BufferedInputStream bufStrm =
		    new BufferedInputStream(in.fileInputStream, READ_BUFFER_SIZE);
		in.dataInputStream = new DataInputStream(bufStrm);

		// show progress
		if (topLevel && progress == null)
		{
			progress = new ProgressDialog("Reading library "+lib.getLibName()+"...");
			progress.setProgress(0);
		}
		boolean error = in.readLib();
		if (topLevel && progress != null)
		{
			progress.close();
			progress = null;
		}
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
	
	/**
	 * Routine to handle conversion of nodes read from disk that have outline information.
	 * @param ni the NodeInst being converted.
	 * @param np the prototype of the NodeInst being converted.
	 * @param lambda the conversion factor.
	 */
	protected void scaleOutlineInformation(NodeInst ni, NodeProto np, double lambda)
	{
		// ignore if not a primitive
		if (!(np instanceof PrimitiveNode)) return;

		// ignore if it doesn't hold outline information
		if (!np.isHoldsOutline()) return;

		// see if there really is outline information
		Variable var = ni.getVar("trace", Integer[].class);
		if (var == null) return;

		// scale the outline information
		Integer [] outline = (Integer [])var.getObject();
		Float [] newOutline = new Float[outline.length];
		for(int j=0; j<outline.length; j++)
		{
			float oldValue = outline[j].intValue();
			newOutline[j] = new Float(oldValue/lambda);
		}
		ni.delVal("trace");
		Variable newVar = ni.setVal("trace", newOutline);
		if (newVar == null)
			System.out.println("Could not preserve outline information on node in cell "+ni.getParent().describe());
	}
}
