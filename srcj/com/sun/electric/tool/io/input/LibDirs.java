/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LibDirs.java
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

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * Class for reading a text file that specifies
 * additional library directories from which to
 * read in library (elib) files.
 *
 * <p>
 * LibDirs files have the following syntax:
 * <p>
 * * <comment text>
 * <p>
 * include <lib_dirs_file>
 * <p>
 * <library_directory>
 *
 * <p>
 */
public class LibDirs {

    /** Default LibDirs file name */                        private static String libDirsFile = "LIBDIRS";
    /** List of library directories from LibDirs file*/     private static ArrayList dirs = new ArrayList();
    /** List of libDirsFiles read (prevent recursion) */    private static ArrayList libDirsFiles = new ArrayList();
    
    /** Creates a new instance of LibDirs */
    LibDirs() {
    }

    /** return list of Lib Dirs 
     * @return ArrayList of lib dirs
     */
    public static Iterator getLibDirs() { return dirs.iterator(); }
    
    /** 
     * Read in LibDirs file.
     * @return true on error.
     */
    public static boolean readLibDirs()
    {
        // read current working dir first, if set to do so
        boolean error = false;
        dirs.clear();
        libDirsFiles.clear();

        // if libDirsFile is not an absolute path, Java assumes
        // it resides in the current working dir
        if (parseFile(libDirsFile)) error = true;
        return error;
    }
        
    /** 
     * parse a LIF file and read in libraries specified.
     * @return true on error.
     */
    private static boolean parseFile(String fileName)
    {
        if (fileName == null) return true;
        
        BufferedReader in = null;
        File file = new File(fileName);
		try {
            FileReader fr = new FileReader(file);
            in = new BufferedReader(fr);
		} catch (FileNotFoundException e) {
			//System.out.println("Could not find file: " + fileName);
			return true;
		}
        libDirsFiles.add(file.getAbsolutePath());

        // parse file
        boolean error = false;
        int lineNumber = 0;
        try {
            String line;
            while ( (line = in.readLine()) != null) {
                if (parseLine(line)) {
                    System.out.println("Parse error: "+fileName+":"+lineNumber);
                    error = true;
                    lineNumber++;
                }
            }
        } catch (IOException e) {
            System.out.println("IOError "+e.getMessage());
        }
        return error;
    }
    
    /**
     * Parse one line of LIF file.
     * @return true on error.
     */
    private static boolean parseLine(String line)
    {
        line = line.trim();                             // remove leading/trailing whitespace
        String[] words = line.split("\\s+");            // split by whitespace
        if (words.length <= 0) return false;            // nothing on line
        if (words[0].equals("*")) return false;         // comment
        if (words[0].equals("include")) {               // include statement
            if (words.length != 2) return true;
            // prevent recursive includes
            File f = new File(words[1]);
            if (libDirsFiles.contains(f.getAbsolutePath())) return true;
            // read included LibDir file
            return parseFile(words[1]);
        }
        // add dir to list
        dirs.add(words[0]);
        return false;
    }

}
