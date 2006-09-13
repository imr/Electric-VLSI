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

import com.sun.electric.database.constraint.Layout;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.Job;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.File;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * This class manages reading files in different formats.
 * The class is subclassed by the different file readers.
 */
public class Input
{
	protected static final int READ_BUFFER_SIZE = 65536;

    /** Log errors. Static because shared between many readers */   public static ErrorLogger errorLogger;
	private static boolean doChangesQuietly = false;

	/** Name of the file being input. */					protected String filePath;
	/** The raw input stream. */							protected InputStream inputStream;
	/** The line number reader (text only). */				protected LineNumberReader lineReader;
	/** The input stream. */								protected DataInputStream dataInputStream;
	/** The length of the file. */							protected long fileLength;
	/** the number of bytes of data read so far */			protected long byteCount;

	// ---------------------- private and protected methods -----------------

	public Input()
	{
	}

	// ----------------------- public methods -------------------------------

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
//		lib.setChanged();

		// initialize timer, error log, etc
		long startTime = System.currentTimeMillis();
        errorLogger = ErrorLogger.newInstance("File Import");

        File f = new File(fileURL.getPath());
        if (f != null && f.exists()) {
            LibDirs.readLibDirs(f.getParent());
        }

//		boolean formerQuiet = changesQuiet(true);
		try {

			// initialize progress
			startProgressDialog("import", fileURL.getFile());

			if (type == FileType.DAIS)
			{
				IOTool.readDais(lib);
			} else
			{
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
			}
		} finally {
			// clean up
			stopProgressDialog();
			errorLogger.termLogging(true);
		}
//		changesQuiet(formerQuiet);

		if (lib == null)
		{
			System.out.println("Error importing " + lib);
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

	protected boolean openBinaryInput(URL fileURL)
	{
		filePath = fileURL.getFile();
		URLConnection urlCon = null;
		try
		{
			urlCon = fileURL.openConnection();
//            urlCon.setConnectTimeout(10000);
//            urlCon.setReadTimeout(1000);
            String contentLength = urlCon.getHeaderField("content-length");
            fileLength = -1;
            try {
                fileLength = Long.parseLong(contentLength);
            } catch (Exception e) {}
//			fileLength = urlCon.getContentLength();
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
        Job.getUserInterface().setProgressValue(pct);
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

	private String lineBuffer;
	private int    lineBufferPosition;

	protected void initKeywordParsing()
	{
		lineBufferPosition = 0;
		lineBuffer = "";
	}

	protected String getAKeyword()
		throws IOException
	{
		// keep reading from file until something is found on a line
		for(;;)
		{
			if (lineBuffer == null) return null;
			if (lineBufferPosition >= lineBuffer.length())
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

}
