/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DELIB.java
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

package com.sun.electric.tool.io.output;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellId;
import com.sun.electric.database.LibId;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Version;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Mar 8, 2006
 * Time: 5:00:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class DELIB extends JELIB {

    DELIB() {
        cellFileMap = new HashMap<String,Integer>();
    }

    private String headerFile;
    private HashMap<String,Integer> cellFileMap;

    protected boolean writeLib(Snapshot snapshot, LibId libId, Map<LibId,URL> libFiles) {
        boolean b = super.writeLib(snapshot, libId, libFiles);
        // write lastModified file for cvs update optimziations
        File lastModifiedFile = new File(filePath, getLastModifiedFile());
        if (!b) {
            try {
                PrintWriter writer;
                writer = new PrintWriter(new BufferedWriter(new FileWriter(lastModifiedFile, false)));
                writer.println(System.currentTimeMillis());
                writer.close();
            } catch (IOException e) {
                System.out.println("Error opening "+lastModifiedFile+": "+e.getMessage());
            }
        }
        return b;
    }

    /**
     * Write a cell. Instead of writing it to the jelib file,
     * write a reference to an external file, and write the contents there
     * @param cellBackup
     */
    void writeCell(CellBackup cellBackup) {
/*
        String cellDir = getCellSubDir(cellBackup);
        File cellFD = new File(filePath + File.separator + cellDir);
        if (cellFD.exists()) {
            if (!cellFD.isDirectory()) {
                System.out.println("Error, file "+cellFD+" is not a directory, moving it to "+cellDir+".old");
                if (!cellFD.renameTo(new File(cellDir+".old"))) {
                    System.out.println("Error, unable to rename file "+cellFD+" to "+cellDir+".old, skipping cell "+cellBackup.d.cellName);
                    return;
                }
            }
        } else {
            // create the directory
            if (!cellFD.mkdir()) {
                System.out.println("Failed to make directory: "+cellFD+", skipping cell "+cellBackup.d.cellName);
                return;
            }
        }
*/

        // create cell file in directory
        String cellFile = getCellFile(cellBackup);
        String cellFileAbs = filePath + File.separator + cellFile;
        // save old printWriter
        PrintWriter headerWriter = printWriter;
        // set current print writer to cell file
        boolean append = false;
        try {
            // check to see if this a version of a cell we've already written,
            // if so, append to the same file
            if (cellFileMap.containsKey(cellFileAbs))
                append = true;
            cellFileMap.put(cellFileAbs, null);

            printWriter = new PrintWriter(new BufferedWriter(new FileWriter(cellFileAbs, append)));
        } catch (IOException e) {
            System.out.println("Error opening "+cellFileAbs+", skipping cell: "+e.getMessage());
            printWriter = headerWriter;
            return;
        }

        // write out external references for this cell
        BitSet usedLibs = new BitSet();
        HashMap<CellId,BitSet> usedExports = new HashMap<CellId,BitSet>();
        cellBackup.gatherUsages(usedLibs, usedExports);
        gatherLibs(usedLibs, usedExports);
        
         // write short header information (library, version)
        LibId libId = cellBackup.d.libId;
        printWriter.println("H" + convertString(snapshot.getLib(libId).d.libName) + "|" + Version.getVersion());
        
        super.writeExternalLibraryInfo(libId, usedLibs, usedExports);

        // write out the cell into the new file
        super.writeCell(cellBackup);

        printWriter.close();
        // set the print writer back
        printWriter = headerWriter;
        if (!append) printWriter.println("C"+cellFile);
    }

    void writeExternalLibraryInfo(LibId libId,  BitSet usedLibs, HashMap<CellId,BitSet> usedExports) {
    }

    /**
     * Open the printWriter for writing text files
     * @return true on error.
     */
    protected boolean openTextOutputStream(String filePath) {
        // first, create a directory for the library
        File f = new File(filePath);
        this.filePath = filePath;
        if (f.exists()) {
            if (!f.isDirectory()) {
                // not a directory, issue error
                System.out.println("Error, file "+f+" is not a directory");
                return true;
            }
        } else {
            // create a directory
            if (!f.mkdir()) {
                System.out.println("Failed to make directory: "+f);
                return true;
            }
        }
        headerFile = filePath + File.separator + getHeaderFile();
        // open new printWriter for cell
        try {
            printWriter = new PrintWriter(new BufferedWriter(new FileWriter(headerFile)));
        } catch (IOException e)
		{
            System.out.println("Error opening " + headerFile+": "+e.getMessage());
            return true;
        }
        return false;
    }

    /**
     * Cell subdirectory name. This is the directory inside the
     * .delib directory containing the Cell files for the specified cell.
     * @param cellBackup
     * @return
     */
    public static String getCellSubDir(CellBackup cellBackup) {
        if (Version.getVersion().compareTo(Version.parseVersion("8.04m")) > 0) {
            return "";
        } else
            return cellBackup.d.cellName.getName();
    }

    /**
     * Cell file name.  This is the path, relative to the .delib directory
     * path, of the file for the specified cell.  Note it is a relative path,
     * not an absolute path. Ex: LEsettings/LEsettings.sch
     * @param cellBackup
     * @return
     */
    private static String getCellFile(CellBackup cellBackup) {
        // versions 8.04n and above write files to .delib dir
        String cellName = cellBackup.d.cellName.getName();
        View view = cellBackup.d.cellName.getView();
        return cellName + "." + view.getAbbreviation();
    }

    /**
     * Method used by other tools to find out relative path for cell.
     * This is the path, relative to the .delib directory
     * path, of the file for the specified cell.  Note it is a relative path,
     * not an absolute path. Ex: LEsettings.sch or LEsettings/LEsettings.sch
     * @param cell
     * @return
     */
    public static String getCellFile(Cell cell) {
        Library lib = cell.getLibrary();
        if (lib.getVersion() == null) return getCellFile(cell.backup());
        if (lib.getVersion().compareTo(Version.parseVersion("8.04m")) > 0) {
            // library version is greater than 8.04m
            return getCellFile(cell.backup());
        } else {
            // in version 8.04m and earlier, cell files were in subdirs
            CellBackup cellBackup = cell.backup();
            String dir = getCellSubDir(cellBackup);
            String cellName = cellBackup.d.cellName.getName();
            //int version = cellBackup.d.cellName.getVersion();
            View view = cellBackup.d.cellName.getView();
            //if (version > 1) cellName = cellName + "_" + version;
            return dir + File.separator + cellName + "." + view.getAbbreviation();
        }
    }

    /**
     * Get relative path to header file from .delib directory
     * @return
     */
    public static final String getHeaderFile() {
        return "header";
    }

    public static final String getLastModifiedFile() {
        return "lastModified";
    }
}
