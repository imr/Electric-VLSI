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
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.dialogs.Progress;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.Point2D;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.File;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

/**
 * This class manages reading files in different formats.
 * The class is subclassed by the different file readers.
 */
public class Input
{
	protected static final int READ_BUFFER_SIZE = 65536;

	/** key of Varible holding true library of fake cell. */		public static final Variable.Key IO_TRUE_LIBRARY = ElectricObject.newKey("IO_true_library");
	/** key of Variable to denote a dummy cell or library */        public static final Variable.Key IO_DUMMY_OBJECT = ElectricObject.newKey("IO_dummy_object");

    /** Log errors. Static because shared between many readers */   public static ErrorLogger errorLogger;

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
	 * This method is for reading full Electric libraries in ELIB, JELIB, and Readable Dump format.
	 * @param fileURL the URL to the disk file.
	 * @param libName the name to give the library (null to derive it from the file path)
	 * @param type the type of library file (ELIB, JELIB, etc.)
	 * @param quick true to read the library without verbosity or "meaning variable" reconciliation
	 * (used when reading a library internally).
	 * @return the read Library, or null if an error occurred.
	 */
	public static synchronized Library readLibrary(URL fileURL, String libName, FileType type, boolean quick)
	{
		if (fileURL == null) return null;
		long startTime = System.currentTimeMillis();
        errorLogger = ErrorLogger.newInstance("Library Read");

        File f = new File(fileURL.getPath());
        if (f != null && f.exists()) {
            LibDirs.readLibDirs(f.getParent());
        }
		LibraryFiles.initializeLibraryInput();

		Library lib = null;
		boolean formerQuiet = Undo.changesQuiet(true);
		try {
			// show progress
			startProgressDialog("library", fileURL.getFile());

			Cell.setAllowCircularLibraryDependences(true);
			Pref.initMeaningVariableGathering();

			StringBuffer errmsg = new StringBuffer();
			boolean exists = TextUtils.URLExists(fileURL, errmsg);
			if (!exists)
			{
				System.out.print(errmsg.toString());
				// if doesn't have extension, assume DEFAULTLIB as extension
				String fileName = fileURL.toString();
				if (fileName.indexOf(".") == -1)
				{
					fileURL = TextUtils.makeURLToFile(fileName+"."+type.getExtensions()[0]);
					System.out.print("Attempting to open " + fileURL+"\n");
					errmsg.setLength(0);
					exists = TextUtils.URLExists(fileURL, errmsg);
					if (!exists) System.out.print(errmsg.toString());
				}
			}
			if (exists)
			{
				// get the library name
				if (libName == null) libName = TextUtils.getFileNameWithoutExtension(fileURL);
				lib = readALibrary(fileURL, null, libName, type);
			}
			if (LibraryFiles.VERBOSE)
				System.out.println("Done reading data for all libraries");

			LibraryFiles.cleanupLibraryInput();
			if (LibraryFiles.VERBOSE)
				System.out.println("Done instantiating data for all libraries");
		} finally {
			stopProgressDialog();
			Cell.setAllowCircularLibraryDependences(false);
		}
		Undo.changesQuiet(formerQuiet);

		if (lib != null && !quick)
		{
			long endTime = System.currentTimeMillis();
			float finalTime = (endTime - startTime) / 1000F;
			System.out.println("Library " + fileURL.getFile() + " read, took " + finalTime + " seconds");
            Pref.reconcileMeaningVariables(lib.getName());
		}

		errorLogger.termLogging(true);

		return lib;
	}

	/**
	 * Method to import a Library from disk.
	 * This method is for reading external file formats such as CIF, GDS, etc.
	 * @param fileURL the URL to the disk file.
	 * @param type the type of library file (CIF, GDS, etc.)
	 * @return the read Library, or null if an error occurred.
	 */
	public static Library importLibrary(URL fileURL, FileType type)
	{
		// make sure the file exists
		if (fileURL == null) return null;
        StringBuffer errmsg = new StringBuffer();
		if (!TextUtils.URLExists(fileURL, errmsg))
		{
			System.out.print(errmsg.toString());
			return null;
		}

		// get the name of the imported library
		String libName = TextUtils.getFileNameWithoutExtension(fileURL);

		// create a new library
		Library lib = Library.newInstance(libName, fileURL);
		lib.setChangedMajor();

		// initialize timer, error log, etc
		long startTime = System.currentTimeMillis();
        errorLogger = ErrorLogger.newInstance("File Import");

        File f = new File(fileURL.getPath());
        if (f != null && f.exists()) {
            LibDirs.readLibDirs(f.getParent());
        }

		boolean formerQuiet = Undo.changesQuiet(true);
		try {

			// initialize progress
			startProgressDialog("import", fileURL.getFile());

			Input in;
			if (type == FileType.CIF)
			{
				in = (Input)new CIF();
				if (in.openTextInput(fileURL)) return null;
			} else if (type == FileType.DEF)
			{
				in = (Input)new DEF();
				if (in.openTextInput(fileURL)) return null;
			} else if (type == FileType.DXF)
			{
				in = (Input)new DXF();
				if (in.openTextInput(fileURL)) return null;
			} else if (type == FileType.EDIF)
			{
				in = (Input)new EDIF();
				if (in.openTextInput(fileURL)) return null;
			} else if (type == FileType.GDS)
			{
				in = (Input)new GDS();
				if (in.openBinaryInput(fileURL)) return null;
			} else if (type == FileType.LEF)
			{
				in = (Input)new LEF();
				if (in.openTextInput(fileURL)) return null;
			} else if (type == FileType.SUE)
			{
				in = (Input)new Sue();
				if (in.openTextInput(fileURL)) return null;
			} else
			{
				System.out.println("Unsupported input format");
				return null;
			}

			// import the library
			in.importALibrary(lib);
			in.closeInput();

		} finally {
			// clean up
			stopProgressDialog();
			errorLogger.termLogging(true);
		}
		Undo.changesQuiet(formerQuiet);

		if (lib == null)
		{
			System.out.println("Error importing library " + lib.getName());
		} else
		{
			long endTime = System.currentTimeMillis();
			float finalTime = (endTime - startTime) / 1000F;
			System.out.println("Library " + fileURL.getFile() + " read, took " + finalTime + " seconds");
		}
		return lib;
	}

	/**
	 * Method to import a library from disk.
	 * This method must be overridden by the various import modules.
	 * @param lib the library to fill
	 * @return true on error.
	 */
	protected boolean importALibrary(Library lib) { return true; }

	/**
	 * Method to read a single library file.
	 * @param fileURL the URL to the file.
	 * @param lib the Library to read.
	 * If the "lib" is null, this is an entry-level library read, and one is created.
	 * If "lib" is not null, this is a recursive read caused by a cross-library
	 * reference from inside another library.
	 * @param type the type of library file (ELIB, CIF, GDS, etc.)
	 * @return the read Library, or null if an error occurred.
	 */
	protected static Library readALibrary(URL fileURL, Library lib, String libName, FileType type)
	{
		// handle different file types
		LibraryFiles in;
		if (type == FileType.ELIB)
		{
			in = new ELIB();
			if (in.openBinaryInput(fileURL)) return null;
		} else if (type == FileType.JELIB)
		{
			in = new JELIB();
			if (in.openTextInput(fileURL)) return null;
		} else if (type == FileType.READABLEDUMP)
		{
			in = new ReadableDump();
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
			// create a new library
			lib = Library.newInstance(libName, fileURL);
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
			long pct = byteCount * 100L / fileLength;
			progress.setProgress((int)pct);
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
		inputStream = null;
		dataInputStream = null;
		lineReader = null;
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
}
