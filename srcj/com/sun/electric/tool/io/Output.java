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
import com.sun.electric.database.hierarchy.Cell;

import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * This class manages writing files in different formats.
 * The class is subclassed by the different file writers.
 */
public class Output
{
	/** file path */                            protected String filePath;
	/** for writing text files */               protected PrintWriter printWriter;
	/** for writing binary files */             protected DataOutputStream dataOutputStream;

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

		/**
		 * Returns a printable version of this ExportType.
		 * @return a printable version of this ExportType.
		 */
		public String toString() { return name; }

		/** Describes binary output .*/			public static final ExportType BINARY=   new ExportType("binary");
		/** Describes text output. */			public static final ExportType TEXT=     new ExportType("text");
		/** Describes CIF output. */			public static final ExportType CIF=      new ExportType("CIF");
		/** Describes GDS output. */			public static final ExportType GDS=      new ExportType("GDS");
	}

	// ------------------------- private data ----------------------------

	// ---------------------- private and protected methods -----------------

	Output()
	{
	}

	// ----------------------- public methods -------------------------------

	/**
	 * Routine to write a Library.
	 * This method is never called.
	 * Instead, it is always overridden by the appropriate write subclass.
	 * @param lib the Library to be written.
     * @return true on error.
	 */
	protected boolean writeLib(Library lib) { return true; }

    /**
     * Routine to write a cell.
     * This method is never called.
     * Instead, it is always overridden by the appropriate write subclass.
     * @param cell the Cell to be written.
     * @return true on error.
     */
    protected boolean writeCell(Cell cell) { return true; }
    
	/**
	 * Routine to write a Library with a particular format
	 * @param lib the Library to be written.
	 * @param type the format of the output file.
     * @return true on error.
	 */
	public static boolean writeLibrary(Library lib, ExportType type)
	{
		Output out;

		// handle different file types
		Library.Name n;
		if (lib.getLibFile() != null) n = Library.Name.newInstance(lib.getLibFile()); else
			n = Library.Name.newInstance(lib.getLibName());
        boolean error = false;
		if (type == ExportType.BINARY)
		{
			out = (Output)new OutputBinary();
			n.setExtension("elib");
            out.filePath = n.makeName();
            if (out.openDataOutputStream()) error = true;
            if (out.writeLib(lib)) error = true;
            if (out.closeDataOutputStream()) error = true;
		} else if (type == ExportType.TEXT)
		{
//			out = (Output)new OutputText();
//			n.setExtension("txt");
//          out.filePath = n.makeName();
//          if (out.openPrintWriter()) error = true;
//          if (out.writeLib(lib)) error = true;
//          if (out.closePrintWriter()) error = true;          
            
			// no text reader yet, see if an elib can be found
			out = (Output)new OutputBinary();
			n.setExtension("elib");
            out.filePath = n.makeName();
            if (out.openDataOutputStream()) error = true;
            if (out.writeLib(lib)) error = true;
            if (out.closeDataOutputStream()) error = true;
		} else
		{
			System.out.println("Unknown export type: " + type);
			return true;
		}

		if (error)
		{
			System.out.println("Error writing library");
			return true;
		}
		return false;
	}
    
    /**
     * Routine to write a Cell to a file with a particular format.
     * @param cell the Cell to be written
     * @param type the format of the output file
     * @return true on error.
     */
    public static boolean writeCell(Cell cell, String filePath, ExportType type)
    {
        boolean error = false;
        if (type == ExportType.CIF) {
            Output out = new OutputCIF();
            out.filePath = filePath;
            if (out.openPrintWriter()) error = true;
            if (out.writeCell(cell)) error = true;
            if (out.closePrintWriter()) error = true;
        }
        
		if (error)
		{
			System.out.println("Error writing "+type+" file");
			return true;
		}
		return false;        
    }

    
    /**
     * Opens the dataOutputStream for writing of binary files.
     * @return true on error.
     */
    protected boolean openDataOutputStream()
    {
        FileOutputStream fileOutputStream;
		try
		{
			fileOutputStream = new FileOutputStream(filePath);
		} catch (FileNotFoundException e)
		{
			System.out.println("Could not write file " + filePath);
			return true;
		}
		BufferedOutputStream bufStrm = new BufferedOutputStream(fileOutputStream);
		dataOutputStream = new DataOutputStream(bufStrm);
        return false;
    }
    
    /** 
     * Closes the dataOutputStream.
     * @return true on error.
     */
    protected boolean closeDataOutputStream()
    {
		try
		{
			dataOutputStream.close();
		} catch (IOException e)
		{
			System.out.println("Error closing " + filePath);
			return true;
		}
        return false;
    }
    
    /**
     * Open the printWriter for writing text files
     * @return true on error.
     */
    protected boolean openPrintWriter()
    {
        try {
            printWriter = new PrintWriter(new BufferedWriter(new FileWriter(filePath)));
        } catch (IOException e) {
            System.out.println("Error opening " + filePath);
            return true;
        }
        return false;
    }
    
    /** 
     * Close the printWriter.
     * @return true on error.
     */
    protected boolean closePrintWriter()
    {
        printWriter.close();
        return false;
    }
}
