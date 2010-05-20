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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.constraint.Layout;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.Main;
import com.sun.electric.database.id.CellId;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.input.verilog.VerilogReader;
import com.sun.electric.tool.user.ErrorLogger;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.BitSet;
import java.util.Map;

import javax.swing.SwingUtilities;

/**
 * This class manages reading files in different formats.
 * The class is subclassed by the different file readers.
 */
public class Input
{
	protected static final int READ_BUFFER_SIZE = 65536;

	/** Log errors. Static because shared between many readers */   public static ErrorLogger errorLogger;
	private static boolean doChangesQuietly = false;
	private static boolean newLibraryCreated = true;

	/** Name of the file being input. */					protected String filePath;
	/** The raw input stream. */							protected InputStream inputStream;
	/** The line number reader (text only). */				protected LineNumberReader lineReader;
	/** The input stream. */								protected DataInputStream dataInputStream;
	/** The length of the file. */							protected long fileLength;
	/** the number of bytes of data read so far */			protected long byteCount;
	/** editing preferences */								protected EditingPreferences ep = EditingPreferences.getThreadEditingPreferences();

	// ---------------------- private and protected methods -----------------

	public Input() {}

	// ----------------------- public methods -------------------------------

	/**
	 * Method to tell if a new library was created for this import operation.
	 * @return true if a new library was created.
	 */
	public static boolean isNewLibraryCreated() { return(newLibraryCreated); }

	/**
	 * Method to import Cells from disk.
	 * This method is for reading external file formats such as CIF, GDS, etc.
	 * @param prefs the packaged preferences for reading the type of file.
	 * @param fileURL the URL to the disk file.
	 * @param type the type of library file (CIF, GDS, etc.)
	 * @param lib the library in which to place the circuitry (null to create a new one).
	 * @param tech the technology to use for import.
	 * @param currentCells this map will be filled with currentCells in Libraries found in library file.
     * @param nodesToExpand this map will be filled with nodes to expand in Libraries found in library file.
	 * @param job the Job that is doing the import.
	 * @return the imported Library, or null if an error occurred.
	 */
	public static Library importLibrary(InputPreferences prefs, URL fileURL, FileType type, Library lib,
		Technology tech, Map<Library,Cell> currentCells, Map<CellId,BitSet> nodesToExpand, Job job)
	{
		// make sure the file exists
		if (fileURL == null) return null;
		StringBuilder errmsg = new StringBuilder();
		if (!TextUtils.URLExists(fileURL, errmsg))
		{
			System.out.print(errmsg.toString());
			return null;
		}

		// note whether the read created a library or not
		newLibraryCreated = (lib == null);

		// create a new library (but not for EDIF...it makes its own)
		if (lib == null && type != FileType.EDIF)
		{
			String libName = TextUtils.getFileNameWithoutExtension(fileURL);
			lib = Library.newInstance(libName, fileURL);
		}

		// initialize timer and error logger
		long startTime = System.currentTimeMillis();
		errorLogger = ErrorLogger.newInstance("File Import");

		File f = new File(fileURL.getPath());
		if (f != null && f.exists()) {
			LibDirs.readLibDirs(f.getParent());
		}

		try {
			// initialize progress
			startProgressDialog("import", fileURL.getFile());

			if (prefs != null)
				lib = prefs.doInput(fileURL, lib, tech, currentCells, nodesToExpand, job);
		} finally
		{
			// clean up
			stopProgressDialog();
			errorLogger.termLogging(true);
		}

		if (lib == null)
		{
			System.out.println("Error importing " + fileURL.getFile() + " as " + type + " format.");
		} else
		{
			long endTime = System.currentTimeMillis();
			float finalTime = (endTime - startTime) / 1000F;
			System.out.println("Library " + fileURL.getFile() + " read, took " + finalTime + " seconds");
		}
		return lib;
	}

	/**
	 * Return OutputPreferences for a specified FileType.
	 * This includes currnet default values of IO ProjectSettings
	 * and either factory default or current default values of Prefs
	 * Current default value of Prefs can be obtained only from client thread
	 * @param type specified file type.
	 * @param factory get factory default values of Prefs
	 * @return an OutputPreferences object for the given file type.
	 * @throws InvalidStateException on attemt to get current default values of Prefs from server thread
	 */
	public static InputPreferences getInputPreferences(FileType type, boolean factory)
	{
		if (!factory && !SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("Current default Prefs can be accessed only from client thread");

		if (type == FileType.APPLICON860) return new Applicon860.Applicon860Preferences(factory);
		if (type == FileType.CIF) return new CIF.CIFPreferences(factory);
		if (type == FileType.DAIS) return new IOTool.DaisPreferences(factory, isNewLibraryCreated());
		if (type == FileType.DEF) return new DEF.DEFPreferences(factory);
		if (type == FileType.DXF) return new DXF.DXFPreferences(factory);
		if (type == FileType.EDIF) return new EDIF.EDIFPreferences(factory);
		if (type == FileType.GDS) return new GDS.GDSPreferences(factory);
		if (type == FileType.GERBER) return new Gerber.GerberPreferences(factory);
		if (type == FileType.LEF) return new LEF.LEFPreferences(factory);
		if (type == FileType.SPICE) return new Spice.SpicePreferences(factory);
		if (type == FileType.SUE) return new Sue.SuePreferences(factory);
		if (type == FileType.VERILOG) return new VerilogReader.VerilogPreferences(factory);
		if (type == FileType.DSPF) return new DSPFReader.DSPFReaderPreferences(factory);
		return null;
	}

	public abstract static class InputPreferences implements Serializable
	{
		protected InputPreferences(boolean factory)
		{
			if (!factory && !SwingUtilities.isEventDispatchThread() && !Main.isBatch())
				throw new IllegalStateException("Current default Prefs can be accessed only from client thread");
		}

		public void initFromUserDefaults() {}

		public abstract Library doInput(URL fileURL, Library lib, Technology tech, Map<Library,Cell> currentCells, Map<CellId,BitSet> nodesToExpand, Job job);
	}

	/**
	 * Method to import a library from disk.
	 * This method must be overridden by the various import modules.
	 * @param lib the library to fill
	 * @param currentCells this map will be filled with currentCells in Libraries found in library file
	 * @return the created library (null on error).
	 */
	protected Library importALibrary(Library lib, Technology tech, Map<Library,Cell> currentCells) { return lib; }

	protected boolean openBinaryInput(URL fileURL)
	{
		filePath = fileURL.getFile();

		try
		{
			URLConnection urlCon = fileURL.openConnection();
			String contentLength = urlCon.getHeaderField("content-length");
			fileLength = -1;
			try {
				fileLength = Long.parseLong(contentLength);
			} catch (Exception e) {}
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

	protected boolean openStringsInput(String[] lines)
	{
		StringBuffer buffer = new StringBuffer();

		try
		{
			for (String l : lines)
			{
				String s = l + "\n";
				buffer.append(s);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		fileLength = lines.length;
		StringReader reader = new StringReader(buffer.toString());
		lineReader = new LineNumberReader(reader);
		return false;
	}

	protected boolean openTextInput(URL fileURL)
	{
		if (openBinaryInput(fileURL)) return true;
		InputStreamReader is = new InputStreamReader(inputStream);
		lineReader = new LineNumberReader(is);
		return false;
	}

	protected static void setProgressNote(String msg)
	{
		Job.getUserInterface().setProgressNote(msg);
	}

	protected static String getProgressNote()
	{
		return Job.getUserInterface().getProgressNote();
	}

	protected static void startProgressDialog(String type, String filePath)
	{
		Job.getUserInterface().startProgressDialog(type, filePath);
	}

	protected static void stopProgressDialog()
	{
		Job.getUserInterface().stopProgressDialog();
	}

	protected static void setProgressValue(int value)
	{
		Job.getUserInterface().setProgressValue(value);
	}

	protected void updateProgressDialog(int bytesRead)
	{
		byteCount += bytesRead;
		if (fileLength == 0) return;
		long pct = byteCount * 100L / fileLength;
		UserInterface ui = Job.getUserInterface();
		if (ui != null)
			ui.setProgressValue((int)pct);
	}

	protected void closeInput()
	{
		try {
			dataInputStream = null;
			if (lineReader != null)
			{
				lineReader.close();
				lineReader = null;
			}
			if (inputStream != null)
			{
				inputStream.close();
				inputStream = null;
			}
		} catch (IOException e ) {}
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

	private String lineBuffer;
	private int	lineBufferPosition;

	protected void initKeywordParsing()
	{
		lineBufferPosition = 0;
		lineBuffer = "";
	}

	/**
	 * Method to allow getAKeyword() to read next line next time is invocated.
	 */
	protected String getRestOfLine() throws IOException
	{
		// +1 to skip the space
		int next = lineBufferPosition+1;
		String rest = (next < lineBuffer.length()) ? lineBuffer.substring(next, lineBuffer.length()) : "";
		lineBufferPosition = lineBuffer.length();
		readNewLine();
		return rest;
	}

    /**
	 * Method to read comments in the form of /*
	 */
	protected void getRestOfComment() throws IOException
	{
        lineBufferPosition++; // only increment if it is the first time
        for (;;)
        {
            // +1 to skip the space
		    int next = lineBufferPosition;
		    String rest = (next < lineBuffer.length()) ? lineBuffer.substring(next, lineBuffer.length()) : "";
            int index = rest.indexOf("*/");

            if (index != -1)
            {
                // found end
                lineBufferPosition = index + 2; // or plus 1?
                return;
            }
            lineBufferPosition = lineBuffer.length();
            readNewLine();
        }
	}

    private void readNewLine() throws IOException
	{
		lineBuffer = lineReader.readLine();

		// manage special comment situations
		if (lineBuffer != null)
		{
			updateProgressDialog(lineBuffer.length());
			lineBuffer = preprocessLine(lineBuffer);
		}

		// look for the first text on the line
		lineBufferPosition = 0;
	}

	protected String readWholeLine() throws IOException
	{
		readNewLine();
		return lineBuffer;
	}

	protected String getAKeyword()
		throws IOException
	{
		// keep reading from file until something is found on a line
		for(;;)
		{
			if (lineBuffer == null)
                return null;
			if (lineBufferPosition >= lineBuffer.length())
			{
				readNewLine();
				continue;
			}

			// look for the first text on the line
			while (lineBufferPosition < lineBuffer.length())
			{
				char chr = lineBuffer.charAt(lineBufferPosition);
				if (chr != ' ' && chr != '\t') break;
				lineBufferPosition++;
			}
			if (lineBufferPosition >= lineBuffer.length()) continue;

			// remember where the keyword begins
			int start = lineBufferPosition;

			// recognize characters that are entire keywords
			char chr = lineBuffer.charAt(lineBufferPosition);
			if (isBreakCharacter(chr))
			{
				lineBufferPosition++;
				return Character.toString(chr);
			}

			// scan to the end of the keyword
			while (lineBufferPosition < lineBuffer.length())
			{
				chr = lineBuffer.charAt(lineBufferPosition);
				if (chr == ' ' || chr == '\t' || isBreakCharacter(chr)) break;
				lineBufferPosition++;
			}

			// advance to the start of the next keyword
			return lineBuffer.substring(start, lineBufferPosition);
		}
	}

	/**
	 * Helper method for keyword processing which decides whether a character is its own keyword.
	 * @param chr the character in question.
	 * @return true if this character should be its own keyword.
	 */
	protected boolean isBreakCharacter(char chr)
	{
		return false;
	}

	/**
	 * Helper method for keyword processing which removes comments.
	 * @param line a line of text just read.
	 * @return the line after comments have been removed.
	 */
	protected String preprocessLine(String line)
	{
		return line;
	}

	/**
	 * Method to tell whether changes are being made quietly.
	 * Quiet changes are not passed to constraint satisfaction.
	 * @return true if changes are being made quietly.
	 */
	public static boolean isChangeQuiet() { return doChangesQuietly; }

	/**
	 * Method to set the subsequent changes to be "quiet".
	 * Quiet changes are not passed to constraint satisfaction.
	 * @return the previous value of the "quiet" state.
	 */
	public static boolean changesQuiet(boolean quiet) {
//		EDatabase.serverDatabase().checkInvariants();
		Layout.changesQuiet(quiet);
//		NetworkTool.changesQuiet(quiet);
		boolean formerQuiet = doChangesQuietly;
		doChangesQuietly = quiet;
		return formerQuiet;
	}

	/**
	 * Method to display an error message because end-of-file was reached.
	 * @param when the statement being read when EOF was reached.
	 * @return false (and prints an error message).
	 */
	protected boolean eofDuring(String when)
	{
		System.out.println("File " + filePath + ", line " + lineReader.getLineNumber() +": End of file while reading " + when);
		return false;
	}
}
