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
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.io.InputLibrary;
import com.sun.electric.tool.user.dialogs.Progress;
import com.sun.electric.tool.user.MenuCommands;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
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
	/** The raw input stream. */							protected InputStream inputStream;
	/** The binary input stream. */							protected DataInputStream dataInputStream;
	/** The length of the file. */							protected long fileLength;
	/** The progress during input. */						protected static Progress progress = null;
	/** the path to the library being read. */				protected static String mainLibDirectory = null;

    
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

	// ----------------------- public methods -------------------------------

	/**
	 * Method to read a Library from disk.
	 * @param fileURL the URL to the disk file.
	 * @param type the type of library file (BINARY .elib, CIF, GDS, etc.)
	 * @return the read Library, or null if an error occurred.
	 */
	public static Library readLibrary(URL fileURL, ImportType type)
	{
		if (fileURL == null) return null;
		long startTime = System.currentTimeMillis();
		InputLibDirs.readLibDirs();
		//Undo.noUndoAllowed();
		InputLibrary.initializeLibraryInput();
		Undo.changesQuiet(true);

		// show progress
		String fileName = fileURL.getFile();
		progress = new Progress("Reading library " + fileName + "...");
		progress.setProgress(0);

		InputStream stream = TextUtils.getURLStream(fileURL);
		Library lib = readALibrary(fileURL, stream, null, type);
		if (InputLibrary.VERBOSE)
			System.out.println("Done reading data for all libraries");

		InputLibrary.cleanupLibraryInput();
		if (InputLibrary.VERBOSE)
			System.out.println("Done instantiating data for all libraries");

		progress.close();
		progress = null;

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
	 * Method to read a single library file.
	 * @param fileURL the URL to the file.
	 * @param stream the InputStream to the data.
	 * @param lib the Library to read.
	 * If the "lib" is null, this is an entry-level library read, and one is created.
	 * If "lib" is not null, this is a recursive read caused by a cross-library
	 * reference from inside another library.
	 * @param type the type of library file (BINARY .elib, CIF, GDS, etc.)
	 * @return the read Library, or null if an error occurred.
	 */
	protected static Library readALibrary(URL fileURL, InputStream stream, Library lib, ImportType type)
	{
		// get the library file name and path
		String fileName = fileURL.getFile();
		Library.Name n = Library.Name.newInstance(fileName);

		// determine whether this is top-level
		boolean topLevel = false;
		if (lib == null)
		{
			mainLibDirectory = n.getPath() + File.separator;
			topLevel = true;
		}

		// handle different file types
		InputLibrary in;
		if (type == ImportType.BINARY)
		{
			in = (InputLibrary)new InputBinary();
		} else if (type == ImportType.TEXT)
		{
			in = (InputLibrary)new InputText();
		} else
		{
			System.out.println("Unknown import type: " + type);
			return null;
		}

		// get information about the file
		in.filePath = fileName;
		in.inputStream = stream;
		URLConnection urlCon = null;
		try
		{
			urlCon = fileURL.openConnection();
		} catch (IOException e)
		{
			System.out.println("Could not find file: " + fileName);
			if (topLevel) mainLibDirectory = null;
			return null;
		}
		in.fileLength = urlCon.getContentLength();

		if (lib == null)
		{
			lib = Library.findLibrary(n.getName());
			if (lib != null)
			{
				// library already exists, prompt for save
				if (MenuCommands.preventLoss(lib, 2)) return null;
				lib.erase();
			} else
			{
				lib = Library.newInstance(n.getName(), fileName);
			}
		}

		in.lib = lib;

		BufferedInputStream bufStrm =
			new BufferedInputStream(in.inputStream, READ_BUFFER_SIZE);
		in.dataInputStream = new DataInputStream(bufStrm);

		// read the library
		boolean error = in.readInputLibrary();
		try
		{
			in.inputStream.close();
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
		if (var != null)
		{
			// scale the outline information
			Integer [] outline = (Integer [])var.getObject();
			int newLength = outline.length / 2;
			Point2D [] newOutline = new Point2D[newLength];
			for(int j=0; j<newLength; j++)
			{
				double oldX = outline[j*2].intValue();
				double oldY = outline[j*2+1].intValue();
				newOutline[j] = new Point2D.Double(oldX / lambda, oldY / lambda);
			}
			//ni.delVar(NodeInst.TRACE);
			Variable newVar = ni.newVar(NodeInst.TRACE, newOutline);
			if (newVar == null)
				System.out.println("Could not preserve outline information on node in cell "+ni.getParent().describe());
			return;
		}

		// see if there really is outline information
		var = ni.getVar(NodeInst.TRACE, Float[].class);
		if (var != null)
		{
			// scale the outline information
			Float [] outline = (Float [])var.getObject();
			int newLength = outline.length / 2;
			Point2D [] newOutline = new Point2D[newLength];
			for(int j=0; j<newLength; j++)
			{
				double oldX = outline[j*2].floatValue();
				double oldY = outline[j*2+1].floatValue();
				newOutline[j] = new Point2D.Double(oldX, oldY);
			}
			//ni.delVar(NodeInst.TRACE);
			Variable newVar = ni.newVar(NodeInst.TRACE, newOutline);
			if (newVar == null)
				System.out.println("Could not preserve outline information on node in cell "+ni.getParent().describe());
			return;
		}
	}

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
