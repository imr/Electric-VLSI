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
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.user.dialogs.Progress;

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class manages reading files in different formats.
 * The class is subclassed by the different file readers.
 */
public class Input extends IOTool
{
	private static final int READ_BUFFER_SIZE = 65536;

	/** key of Varible holding true library of fake cell. */		public static final Variable.Key IO_TRUE_LIBRARY = ElectricObject.newKey("IO_true_library");
	
	/** Name of the file being input. */					protected String filePath;
	/** The Library being input. */							protected Library lib;
	/** The raw input stream. */							protected FileInputStream fileInputStream;
	/** The binary input stream. */							protected DataInputStream dataInputStream;
	/** The length of the file. */							protected long fileLength;
	/** The progress during input. */						protected static Progress progress = null;
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
	 * Method to read a Library.
	 * This method is never called.
	 * Instead, it is always overridden by the appropriate read subclass.
	 * @return true on error.
	 */
	protected boolean readLib() { return true; }

	// ----------------------- public methods -------------------------------

	/**
	 * Method to read a Library from disk.
	 * @param fileName the path to the disk file.
	 * @param type the type of library file (BINARY .elib, CIF, GDS, etc.)
	 * @return the read Library, or null if an error occurred.
	 */
	public static Library readLibrary(String fileName, ImportType type)
	{
		long startTime = System.currentTimeMillis();
        InputLibDirs.readLibDirs();
		//Undo.noUndoAllowed();
		Undo.changesQuiet(true);
		Library lib = readALibrary(fileName, null, type);
		Undo.changesQuiet(false);
		Network.reload();
		if (lib != null)
		{
			long endTime = System.currentTimeMillis();
			float finalTime = (endTime - startTime) / 1000F;
			System.out.println("Library " + fileName + " read, took " + finalTime + " seconds");
		}
		return lib;
	}

	/**
	 * Method to read a single disk file.
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

		// RKao: Test for file existence BEFORE we create a new Library
		in.filePath = fileName;
		File file = new File(fileName);
		in.fileLength = file.length(); 
		try
		{
			in.fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e)
		{
			System.out.println("Could not find file: " + fileName);
			if (topLevel) mainLibDirectory = null;
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

		in.lib = lib;

		BufferedInputStream bufStrm =
		    new BufferedInputStream(in.fileInputStream, READ_BUFFER_SIZE);
		in.dataInputStream = new DataInputStream(bufStrm);

		// show progress
		if (topLevel && progress == null)
		{
			progress = new Progress("Reading library "+lib.getLibName()+"...");
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
	 * Method to handle conversion of nodes read from disk that have outline information.
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
		Variable var = ni.getVar(NodeInst.TRACE, Integer[].class);
		if (var == null) return;

		// scale the outline information
		Integer [] outline = (Integer [])var.getObject();
		Float [] newOutline = new Float[outline.length];
		for(int j=0; j<outline.length; j++)
		{
			float oldValue = outline[j].intValue();
			newOutline[j] = new Float(oldValue/lambda);
		}
		//ni.delVar(NodeInst.TRACE);
		Variable newVar = ni.newVar(NodeInst.TRACE, newOutline);
		if (newVar == null)
			System.out.println("Could not preserve outline information on node in cell "+ni.getParent().describe());
	}

//	/**
//	 * Method to set name of Geometric object.
//	 * @param geom the Geometric object.
//	 * @param value name of object
//	 * @param td text descriptor.
//	 * @param type type mask.
//	 */
//	protected Name makeGeomName(Geometric geom, Object value, int type)
//	{
//		if (value == null || !(value instanceof String)) return null;
//		String str = (String)value;
//		Name name = Name.findName(str);
//		if ((type & BinaryConstants.VDISPLAY) != 0)
//		{
//			if (name.isTempname())
//			{
//				String newS = "";
//				for (int i = 0; i < str.length(); i++)
//				{
//					char c = str.charAt(i);
//					if (c == '@') c = '_';
//					newS += c;
//				}
//				name = Name.findName(newS);
//			}
//		} else if (!name.isTempname()) return null;
//		return name;
//	}
//
//	private static String [] fontNames = null;
//
//	/**
//	 * Method to grab font associations that were stored on a Library.
//	 * The font associations are used later to convert indices to true font names and numbers.
//	 * @param lib the Library to examine.
//	 */
//	public static void getFontAssociationVariable(Library lib)
//	{
//		fontNames = null;
//		Variable var = lib.getVar(Library.FONT_ASSOCIATIONS, String[].class);
//		if (var == null) return;
//
//		String [] associationArray = (String [])var.getObject();
//		int maxAssociation = 0;
//		for(int i=0; i<associationArray.length; i++)
//		{
//			int fontNumber = TextUtils.atoi(associationArray[i]);
//			if (fontNumber > maxAssociation) maxAssociation = fontNumber;
//		}
//		if (maxAssociation <= 0) return;
//
//		fontNames = new String[maxAssociation];
//		for(int i=0; i<maxAssociation; i++) fontNames[i] = null;
//		for(int i=0; i<associationArray.length; i++)
//		{
//			int fontNumber = TextUtils.atoi(associationArray[i]);
//			if (fontNumber <= 0) continue;
//			int slashPos = associationArray[i].indexOf('/');
//			if (slashPos < 0) continue;
//			fontNames[fontNumber-1] = associationArray[i].substring(slashPos+1);
//		}
//
//		// data cached: delete the association variable
//		lib.delVar(Library.FONT_ASSOCIATIONS);
//	}
//
//	public static void fixVariableFont(ElectricObject eobj)
//	{
//		for(Iterator it = eobj.getVariables(); it.hasNext(); )
//		{
//			Variable var = (Variable)it.next();
//			fixTextDescriptorFont(var.getTextDescriptor());
//		}
//	}
//
//	/**
//	 * Method to convert the font number in a TextDescriptor to the proper value as
//	 * cached in the Library.  The caching is examined by "getFontAssociationVariable()".
//	 * @param td the TextDescriptor to convert.
//	 */
//	public static void fixTextDescriptorFont(TextDescriptor td)
//	{
//		int fontNumber = td.getFace();
//		if (fontNumber == 0) return;
//
//		if (fontNames == null) fontNumber = 0; else
//		{
//			if (fontNumber <= fontNames.length)
//			{
//				String fontName = fontNames[fontNumber-1];
//				TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(fontName);
//				if (af == null) fontNumber = 0; else
//					fontNumber = af.getIndex();
//			}
//		}
//		td.setFace(fontNumber);
//	}
}
