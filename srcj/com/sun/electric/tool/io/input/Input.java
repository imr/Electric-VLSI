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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.ELIBConstants;
import com.sun.electric.tool.user.dialogs.Progress;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.menus.MenuCommands;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.menus.MenuCommands;
import com.sun.electric.tool.user.menus.FileMenu;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.WindowContent;

import java.awt.geom.Point2D;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

/**
 * This class manages reading files in different formats.
 * The class is subclassed by the different file readers.
 */
public class Input // extends IOTool
{
	protected static final int READ_BUFFER_SIZE = 65536;

	/** key of Varible holding true library of fake cell. */		public static final Variable.Key IO_TRUE_LIBRARY = ElectricObject.newKey("IO_true_library");
	/** key of Variable to denote a dummy cell or library */        public static final Variable.Key IO_DUMMY_OBJECT = ElectricObject.newKey("IO_dummy_object");

    /** Log errors. Static because shared between many readers */   protected static ErrorLogger errorLogger;

	/** Name of the file being input. */					protected String filePath;
	/** The Library being input. */							protected Library lib;
	/** The raw input stream. */							protected InputStream inputStream;
	/** The line number reader (text only). */				protected LineNumberReader lineReader;
	/** The input stream. */								protected DataInputStream dataInputStream;
	/** The length of the file. */							protected long fileLength;
	/** The progress during input. */						protected static Progress progress = null;
	/** the path to the library being read. */				protected static String mainLibDirectory = null;
	/** true if the library is the main one being read. */	protected boolean topLevelLibrary;
	/** the number of bytes of data read so far */			protected int byteCount;

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
	 * @param type the type of library file (ELIB, CIF, GDS, etc.)
	 * @return the read Library, or null if an error occurred.
	 */
	public static synchronized Library readLibrary(URL fileURL, OpenFile.Type type)
	{
		if (fileURL == null) return null;
		long startTime = System.currentTimeMillis();
        errorLogger = ErrorLogger.newInstance("Library Read");

		LibDirs.readLibDirs();
		//Undo.noUndoAllowed();
		LibraryFiles.initializeLibraryInput();
		Undo.changesQuiet(true);
		Pref.initMeaningVariableGathering();

		// show progress
		startProgressDialog("library", fileURL.getFile());

        StringBuffer errmsg = new StringBuffer();
		boolean exists = TextUtils.URLExists(fileURL, errmsg);
        Library lib = null;
        if (!exists) System.out.print(errmsg.toString()); else
		    lib = readALibrary(fileURL, null, type);
		if (LibraryFiles.VERBOSE)
			System.out.println("Done reading data for all libraries");

		LibraryFiles.cleanupLibraryInput();
		if (LibraryFiles.VERBOSE)
			System.out.println("Done instantiating data for all libraries");

		stopProgressDialog();

		Undo.changesQuiet(false);
		Network.reload();
		if (lib != null)
		{
			long endTime = System.currentTimeMillis();
			float finalTime = (endTime - startTime) / 1000F;
			System.out.println("Library " + fileURL.getFile() + " read, took " + finalTime + " seconds");
		}
		Pref.reconcileMeaningVariables();

        errorLogger.termLogging(true);

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
	 * @param type the type of library file (ELIB, CIF, GDS, etc.)
	 * @return the read Library, or null if an error occurred.
	 */
	protected static Library readALibrary(URL fileURL, Library lib, OpenFile.Type type)
	{
		// get the library file name and path
		String libName = TextUtils.getFileNameWithoutExtension(fileURL);
		String extension = TextUtils.getExtension(fileURL);

		// handle different file types
		LibraryFiles in;
		if (type == OpenFile.Type.ELIB)
		{
			in = (LibraryFiles)new ELIB();
			if (in.openBinaryInput(fileURL)) return null;
		} else if (type == OpenFile.Type.READABLEDUMP)
		{
			in = (LibraryFiles)new ReadableDump();
			if (in.openTextInput(fileURL)) return null;
		} else
		{
			System.out.println("Unknown import type: " + type);
			return null;
		}

		// determine whether this is top-level
		in.topLevelLibrary = false;
		if (lib == null)
		{
			mainLibDirectory = TextUtils.getFilePath(fileURL);
			in.topLevelLibrary = true;
		}

		if (lib == null)
		{
			lib = Library.findLibrary(libName);
			if (lib != null)
			{
				// library already exists, prompt for save
				if (FileMenu.preventLoss(lib, 2)) return null;
				WindowFrame.removeLibraryReferences(lib);
				lib.erase();
			} else
			{
				lib = Library.newInstance(libName, fileURL);
			}
		}

		in.lib = lib;

		// read the library
		boolean error = in.readInputLibrary();
		in.closeInput();
		if (error)
		{
			System.out.println("Error reading library " + lib.getName());
			if (in.topLevelLibrary) mainLibDirectory = null;
			return null;
		}
		return in.lib;
	}
	
	/**
	 * Method to handle conversion of nodes read from disk that have outline information.
	 * @param ni the NodeInst being converted.
	 * @param np the prototype of the NodeInst being converted.
	 * @param lambdaX the X conversion factor.
	 * @param lambdaY the Y conversion factor.
	 */
	protected void scaleOutlineInformation(NodeInst ni, NodeProto np, double lambdaX, double lambdaY)
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
				newOutline[j] = new Point2D.Double(oldX / lambdaX, oldY / lambdaY);
			}
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
			Variable newVar = ni.newVar(NodeInst.TRACE, newOutline);
			if (newVar == null)
				System.out.println("Could not preserve outline information on node in cell "+ni.getParent().describe());
			return;
		}
	}

	/**
	 * Method to set name of Geometric object.
	 * @param geom the Geometric object.
	 * @param value name of object
	 * @param type type mask.
	 */
	protected Name makeGeomName(Geometric geom, Object value, int type)
	{
		if (value == null || !(value instanceof String)) return null;
		String str = (String)value;
		Name name = Name.findName(str);
		if ((type & ELIBConstants.VDISPLAY) != 0)
		{
			if (name.isTempname())
			{
				String newS = "";
				for (int i = 0; i < str.length(); i++)
				{
					char c = str.charAt(i);
					if (c == '@') c = '_';
					newS += c;
				}
				name = Name.findName(newS);
			}
		} else if (!name.isTempname()) return null;
		return name;
	}

	private static String [] fontNames = null;

	/**
	 * Method to grab font associations that were stored on a Library.
	 * The font associations are used later to convert indices to true font names and numbers.
	 * @param lib the Library to examine.
	 */
	public static void getFontAssociationVariable(Library lib)
	{
		fontNames = null;
		Variable var = lib.getVar(Library.FONT_ASSOCIATIONS, String[].class);
		if (var == null) return;

		String [] associationArray = (String [])var.getObject();
		int maxAssociation = 0;
		for(int i=0; i<associationArray.length; i++)
		{
			int fontNumber = TextUtils.atoi(associationArray[i]);
			if (fontNumber > maxAssociation) maxAssociation = fontNumber;
		}
		if (maxAssociation <= 0) return;

		fontNames = new String[maxAssociation];
		for(int i=0; i<maxAssociation; i++) fontNames[i] = null;
		for(int i=0; i<associationArray.length; i++)
		{
			int fontNumber = TextUtils.atoi(associationArray[i]);
			if (fontNumber <= 0) continue;
			int slashPos = associationArray[i].indexOf('/');
			if (slashPos < 0) continue;
			fontNames[fontNumber-1] = associationArray[i].substring(slashPos+1);
		}

		// data cached: delete the association variable
		lib.delVar(Library.FONT_ASSOCIATIONS);
	}

	public static void fixVariableFont(ElectricObject eobj)
	{
		if (eobj == null) return;
		for(Iterator it = eobj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			fixTextDescriptorFont(var.getTextDescriptor());
		}
	}

	/**
	 * Method to convert the font number in a TextDescriptor to the proper value as
	 * cached in the Library.  The caching is examined by "getFontAssociationVariable()".
	 * @param td the TextDescriptor to convert.
	 */
	public static void fixTextDescriptorFont(TextDescriptor td)
	{
		int fontNumber = td.getFace();
		if (fontNumber == 0) return;

		if (fontNames == null) fontNumber = 0; else
		{
			if (fontNumber <= fontNames.length)
			{
				String fontName = fontNames[fontNumber-1];
				TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(fontName);
				if (af == null) fontNumber = 0; else
					fontNumber = af.getIndex();
			}
		}
		td.setFace(fontNumber);
	}

	javax.swing.ProgressMonitorInputStream is = null;
	
	protected boolean openBinaryInput(URL fileURL)
	{
		filePath = fileURL.getFile();
		URLConnection urlCon = null;
		try
		{
			urlCon = fileURL.openConnection();
			fileLength = urlCon.getContentLength();
			inputStream = urlCon.getInputStream();
		} catch (IOException e)
		{
			System.out.println("Could not find file: " + filePath);
			return true;
		}
		byteCount = 0;

		BufferedInputStream bufStrm = new BufferedInputStream(inputStream, READ_BUFFER_SIZE);
		dataInputStream = new DataInputStream(bufStrm);
		return false;
	}

	protected boolean openTextInput(URL fileURL)
	{
		if (openBinaryInput(fileURL)) return true;
		InputStreamReader is = new InputStreamReader(inputStream);
		lineReader = new LineNumberReader(is);
		return false;
	}

	protected static void startProgressDialog(String type, String filePath)
	{
		progress = new Progress("Reading " + type + " " + filePath + "...");
		progress.setProgress(0);
	}

	protected static void stopProgressDialog()
	{
		progress.close();
		progress = null;
	}
	
	protected void updateProgressDialog(int bytesRead)
	{
		byteCount += bytesRead;
		if (progress != null && fileLength > 0)
		{
			progress.setProgress((int)(byteCount * 100 / fileLength));
		}
	}

	protected void closeInput()
	{
		if (inputStream == null) return;
		try
		{
			inputStream.close();
		} catch (IOException e)
		{
			System.out.println("Error closing file");
		}
	}

	/**
	 * Method to read the next line of text from a file.
	 * @return the line (null on EOF).
	 * @throws IOException
	 */
	protected String getLine()
		throws IOException
	{
		StringBuffer sb = new StringBuffer();
		for(;;)
		{
			int c = lineReader.read();
			if (c == -1) return null;
			if (c == '\n') break;
			sb.append((char)c);
		}
		return sb.toString();
	}

	/**
	 * Method to read a line of text, when the file has been opened in binary mode.
	 * @return the line (null on EOF).
	 * @throws IOException
	 */
	protected String getLineFromBinary()
		throws IOException
	{
		StringBuffer sb = new StringBuffer();
		for(;;)
		{
			int c = dataInputStream.read();
			if (c == -1) return null;
			if (c == '\n' || c == '\r') break;
			sb.append((char)c);
		}
		return sb.toString();
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
