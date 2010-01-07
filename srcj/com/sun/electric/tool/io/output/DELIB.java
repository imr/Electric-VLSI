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

package com.sun.electric.tool.io.output;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellRevision;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.CellUsage;
import com.sun.electric.database.id.LibId;
import com.sun.electric.database.text.Version;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * User: gainsley
 * Date: Mar 8, 2006
 */
public class DELIB extends JELIB {

    private HashMap<String,CellFileState> cellFileMap;
    private List<String> deletedCellFiles;
    private String headerFile;
    private boolean wroteSearchForCells;
    private boolean writeHeaderOnly;    // overwrite header if it has cvs conflicts

    DELIB(boolean writeHeaderOnly) {
        // keep list of all associated cell files
        cellFileMap = new HashMap<String,CellFileState>();
        // list of cell files deleted (renamed) because they no longer exist in the library
        deletedCellFiles = new ArrayList<String>();
        wroteSearchForCells = false;
        this.writeHeaderOnly = writeHeaderOnly;
    }

    private static class CellFileState {
        public boolean modified = false;
        public boolean appendFile = false;
    }

    // last version to use subdirs to hold cells, instead of putting them all in delib dir.
    public static final String SEARCH_FOR_CELL_FILES = "____SEARCH_FOR_CELL_FILES____";
    public static final char PLATFORM_INDEPENDENT_FILE_SEPARATOR = '/';

    protected boolean writeLib(Snapshot snapshot, LibId libId, Set<CellId> oldCells) {
        Set<String> oldCellFiles = new HashSet<String>();
        for (CellId cellId: oldCells)
            oldCellFiles.add(getCellFile(cellId));
        // sanity check: make sure we are not writing inside another delib file, this is bad for cvs
        // and just bad and confusing in general
        File delibDir = new File(filePath);
        File parent = delibDir.getParentFile();
        if (parent.getName().endsWith(".delib")) {
            System.out.println("Error: Cannot write "+snapshot.getLib(libId).d.libId.libName+" inside of another DELIB directory");
            return true;
        }

        // decide what files should be written
        for (CellBackup cellBackup : snapshot.cellBackups) {
            if (cellBackup == null) continue;
            CellRevision cellRevision = cellBackup.cellRevision;
            if (cellRevision.d.getLibId() != libId) continue;
            String cellFile = getCellFile(cellRevision.d.cellId);
            String file = filePath + File.separator + cellFile;
            // different cells are in different files, with the exception of
            // different versions of the same cell, which map to the same file name
            CellFileState state = cellFileMap.get(file);
            if (state == null) {
                state = new CellFileState();
                cellFileMap.put(file, state);
            }
            // if any versions are modified or do not exist on disk like they should,
            // mark the file to be modified
            File fd = new File(file);
            if (cellBackup.modified || !fd.exists()) state.modified = true;
        }
        // See for deleted versions (Bug #1945)
        for (CellId cellId: oldCells) {
            if (snapshot.getCell(cellId) == null) {
                String cellFile = getCellFile(cellId);
                String file = filePath + File.separator + cellFile;
                CellFileState state = cellFileMap.get(file);
                if (state != null)
                    state.modified = true;
            }
        }

        boolean b = super.writeLib(snapshot, libId, null, false);
        if (!b && !writeHeaderOnly) {
            // rename cell files that are no longer in the library
            deletedCellFiles.clear();
            for (File file : delibDir.listFiles()) {
                checkIfDeleted(file, oldCellFiles);
            }
//            if (oldCellFiles != null) {
//                oldCellFiles.clear();
//                oldCellFiles.addAll(cellFileMap.keySet());
//            }
        }
        return b;
    }

    /**
     * Rename (backup) any files for cells that have been deleted. This will prevent them
     * being found by version 8.04n and greater, which searches the delib dir for cell files.
     * @param cellFile
     */
    private void checkIfDeleted(File cellFile, Set<String> oldCellFiles) {
        if (cellFileMap.containsKey(cellFile.getAbsolutePath())) return;
        String name = cellFile.getName();
        int dot = name.lastIndexOf('.');
        if (dot == -1) return;
        //String cellName = name.substring(0, dot);
        View view = View.findView(name.substring(dot+1));
        if (view == null) return;

        if (oldCellFiles == null || !oldCellFiles.contains(cellFile.getName())) return;
//        if (oldCellFiles == null || !oldCellFiles.contains(cellFile.getAbsolutePath())) return;
        System.out.println("Renaming unlinked (possibly deleted) cell file "+name+" to "+name+".deleted");
        deletedCellFiles.add(cellFile.getAbsolutePath());
        File deletedFileName = new File(cellFile.getAbsolutePath()+".deleted");
        if (!cellFile.renameTo(deletedFileName)) {
            System.out.println("  Error: Unable to rename unlinked cell file "+name+" to "+name+".deleted!");
        }
    }

    /**
     * Write a cell. Instead of writing it to the jelib file,
     * write a reference to an external file, and write the contents there
     * @param cellBackup
     */
    @Override
    void writeCell(CellRevision cellRevision) {
        if (writeHeaderOnly) {
            if (!wroteSearchForCells) {
                printWriter.println("C"+SEARCH_FOR_CELL_FILES);
                wroteSearchForCells = true;
            }
            return;
        }

        // create cell file in directory
        String cellFile = getCellFile(cellRevision.d.cellId);
        String cellFileAbs = filePath + File.separator + cellFile;
        // save old printWriter
        CellFileState state = cellFileMap.get(cellFileAbs);
        boolean append = state.appendFile;

        // if append is null, do not write this file.
        if (state.modified) {

            // set current print writer to cell file
            PrintWriter headerWriter = printWriter;
            try {
                printWriter = new PrintWriter(new BufferedWriter(new FileWriter(cellFileAbs, append)));
            } catch (IOException e) {
                System.out.println("Error opening "+cellFileAbs+", skipping cell: "+e.getMessage());
                printWriter = headerWriter;
                return;
            }

            // write out external references for this cell
            HashSet<LibId> usedLibs = new HashSet<LibId>();
            int[] instCounts = cellRevision.getInstCounts();
            for (int i = 0; i < instCounts.length; i++) {
                int instCount = instCounts[i];
                if (instCount == 0) continue;
                CellUsage u = cellRevision.d.cellId.getUsageIn(i);
                usedLibs.add(u.protoId.libId);
            }

             // write short header information (library, version)
            LibId libId = cellRevision.d.getLibId();
            printWriter.println("H" + convertString(libId.libName) + "|" + Version.getVersion());

            super.writeExternalLibraryInfo(libId, usedLibs);

            // write out the cell into the new file
            super.writeCell(cellRevision);

            printWriter.close();
            // set the print writer back
            printWriter = headerWriter;
        }

        if (!append) state.appendFile = true;       // next time around file will be appended

        if (!wroteSearchForCells) {
            printWriter.println("C"+SEARCH_FOR_CELL_FILES);
            wroteSearchForCells = true;
        }
    }

    @Override
    void writeExternalLibraryInfo(LibId libId,  Set<LibId> usedLibs) {
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

    public List<String> getDeletedCellFiles() { return deletedCellFiles; }

    public List<String> getWrittenCellFiles() {
        List<String> files = new ArrayList<String>();
        for (String s : cellFileMap.keySet())
            files.add(s);
        return files;
    }

    /**
     * Cell subdirectory name. This is the directory inside the
     * .delib directory containing the Cell files for the specified cell.
     * @param cellId
     * @return the Cell subdirectory name.
     */
    public static String getCellSubDir(CellId cellId) {
        return "";
    }

    /**
     * Cell file name.  This is the path, relative to the .delib directory
     * path, of the file for the specified cell.  Note it is a relative path,
     * not an absolute path. Ex: LEsettings/LEsettings.sch
     * @param cellId
     * @return the file with the Cellin it.
     */
    private static String getCellFile(CellId cellId) {
        // versions 8.04n and above write files to .delib dir
        String cellName = cellId.cellName.getName();
        View view = cellId.cellName.getView();
        cellName = cellName + "." + view.getAbbreviation();
        cellName = cellName.replace(File.separatorChar, ':');
        return cellName;
    }

    /**
     * Method used by other tools to find out relative path for cell.
     * This is the path, relative to the .delib directory
     * path, of the file for the specified cell.  Note it is a relative path,
     * not an absolute path. Ex: LEsettings.sch or LEsettings/LEsettings.sch
     * @param cell the Cell.
     * @return the file with the Cell in it.
     */
    public static String getCellFile(Cell cell) {
        return getCellFile(cell.getId());
    }

    /**
     * Get relative path to header file from .delib directory
     * @return the name of the header file in the .delib directory.
     */
    public static final String getHeaderFile() {
        return "header";
    }
}
