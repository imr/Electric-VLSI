/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Update.java
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

package com.sun.electric.tool.cvspm;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.CellBackup;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.menus.FileMenu;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Mar 13, 2006
 * Time: 3:30:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class Update {

    // -------------- Updating -----------------

    public static void updateAllLibraries() {

    }

    // -------------- Update Library -------------------

    public static void updateLibrary(Library lib) {
        (new UpdateLibraryJob(lib)).startJob();
    }

    private static class UpdateLibraryJob extends Job {
        private Library lib;
        public UpdateLibraryJob(Library lib) {
            super("CVS Update Library", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.lib = lib;
        }
        public boolean doIt() {
            if (!lib.isFromDisk()) {
                System.out.println("Cannot update libraries without disk files, and that have not been checked out from CVS");
                return true;
            }

            // check if library has been changed, ask if user wants to discard changes if so
            if (lib.isChanged()) {
                String [] choices = { "Save Changes then Update", "Discard Changes then Update", "Cancel" };
                int choice = Job.getUserInterface().askForChoice("Library "+lib.getName()+"has been modified in memory:",
                        "CVS Update", choices, choices[2]);
                if (choice == 2) return true;
                if (choice == 0) {
                    // save library
                    FileMenu.saveLibraryCommand(lib, FileType.DEFAULTLIB, false, true, false);
                }
            }
            LibraryFiles.reloadLibrary(lib);
            EDatabase.serverDatabase().checkInvariants();
            return true;
        }
    }

    // -------------- Update Cell ----------------------

    /**
     * Does not update header file from DELIB.
     * @param cell
     */
    public static void updateCell(Cell cell) {
        if (!CVS.isFileInCVS(CVS.getCellFile(cell))) {
            System.out.println("Cell "+cell.describe(true)+" is not in CVS");
            return;
        }
        Snapshot checkedSnapshot = EDatabase.clientDatabase().backup();
        CellBackup cellbackup = checkedSnapshot.getCell(cell.getCellIndex());
        // check if cell has been modified (don't care about other cells modifications)
        if (cellbackup.modified >= 1) {
            Job.getUserInterface().showErrorMessage("Cell is modified. Cell must be saved before doing Update",
                    "Cell Update Error");
            return;
        }
        Job job = new UpdateCellJob(cell, checkedSnapshot.snapshotId);
        job.startJob();
    }

    private static class UpdateCellJob extends Job {
        private Cell cell;
        private int snapshotId;
        private int updateResult;   // 1 to do nothing, 0 to reload from disk, -1 if conflicts
        public UpdateCellJob(Cell cell, int snapshotId) {
            super("CVS Update Cell", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.cell = cell;
            this.snapshotId = snapshotId;
        }
        public boolean doIt() throws JobException {
            // check if Cell is changed, ask if user wants to discard changes if so
            if (EDatabase.serverDatabase().backup().snapshotId != snapshotId) {
                // database has changed since we checked for Cell modification
                throw new JobException("Server modified database since checks on Client side");
            }

            // get cell file
            File cellFile = CVS.getCellFile(cell);
            if (cellFile == null) return true;

            String dir = cellFile.getParent();
            String c = cellFile.getName();

            // issue CVS update command
            StatusResult result = update(c, dir);
            updateResult = commentUpdate(result);
            if (updateResult == 0) {
                // reload data from disk
                List<Cell> list = new ArrayList<Cell>();
                list.add(cell);
                LibraryFiles.reloadLibraryCells(list);
            }

            EDatabase.serverDatabase().checkInvariants();
            return true;
        }
    }

    /**
     * Update the given file in the given directory.
     * @param file
     * @param dir
     * @return
     */
    public static StatusResult update(String file, String dir) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CVS.runCVSCommand("-nq update "+file, "Getting status for "+file,
                    dir, out);
        LineNumberReader result = new LineNumberReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));
        return parseOutput(result);
    }


    // ---------------- All Library Status ----------------

    public static void getStatusAllLibraries() {
        // check modification status of current state of database
        boolean modified = false;
        for (Iterator<Library> it = Library.getLibraries(); it.hasNext(); ) {
            Library lib = it.next();
            if (lib.isHidden()) continue;
            if (lib.isChanged()) {
                modified = true;
                break;
            }
        }
        if (modified) {
            // warn user
            boolean confirm = Job.getUserInterface().confirmMessage(
                    "One or more Libraries has changes in memory; CVS status compares only\n"+
                    "disk files to repository files. Continue anyway?");
            if (!confirm) return;
        }
        LibraryStatusJob job = new LibraryStatusJob(null);
        job.startJob();
    }

    // ---------------- Library Status ----------------

    public static void getStatus(Library lib) {
        // check modification status of current state of database
        if (lib.isChanged()) {
            // warn user
            boolean confirm = Job.getUserInterface().confirmMessage(
                    "Library has changes in memory; CVS status compares only\n"+
                    "disk files to repository files. Continue anyway?");
            if (!confirm) return;
        }
        LibraryStatusJob job = new LibraryStatusJob(lib);
        job.startJob();
    }


    private static class LibraryStatusJob extends Job {
        private Library lib;
        private LibraryStatusJob(Library lib) {
            super("CVS Status", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
            this.lib = lib;
        }
        public boolean doIt() {
            boolean allfilesuptodate = true;
            if (lib == null) {
                // do for all libraries
                for (Iterator<Library> it = Library.getLibraries(); it.hasNext(); ) {
                    Library lib = it.next();
                    if (lib.isHidden()) continue;
                    if (!doItOneLib(lib))
                        allfilesuptodate = false;
                }
            } else {
                if (!doItOneLib(lib))
                    allfilesuptodate = false;
            }
            if (allfilesuptodate) {
                System.out.println("All files up-to-date");
            } else {
                System.out.println("All other files up-to-date");
            }
            return true;
        }
        /** Return true if all files are up to date, false otherwise */
        private boolean doItOneLib(Library lib) {
            StatusResult result = status(lib);
            return commentStatus(result);
        }
    }


    // ---------------- Cell Status -------------------

    public static void getStatus(Cell cell) {
        if (!CVS.isFileInCVS(CVS.getCellFile(cell))) {
            System.out.println("Cell "+cell.describe(true)+" is not in CVS");
            return;
        }

        if (cell.isModified(true)) {
            boolean confirm = Job.getUserInterface().confirmMessage(
                    "Cell has changes in memory; CVS status compares only\n"+
                    "disk files to repository files. Continue anyway?");
            if (!confirm) return;
        }
        Job job = new CellStatusJob(cell);
        job.startJob();
    }

    private static class CellStatusJob extends Job {
        private Cell cell;
        private CellStatusJob(Cell cell) {
            super("CVS Status", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
            this.cell = cell;
        }
        public boolean doIt() {
            StatusResult result = status(cell);
            if (commentStatus(result)) {
                System.out.println("Cell is up-to-date");
            }
            return true;
        }
    }

    // ------------------------ Status Methods -----------------------

    /**
     * Get the status for the library. Returns null on error.
     * @param lib
     * @return
     */
    public static StatusResult status(Library lib) {
        File file = new File(lib.getLibFile().getPath());
        if (!CVS.isFileInCVS(file)) {
            // library not in CVS
            System.out.println(lib.getName()+" is not in CVS");
            return null;
        }
        String dir = file.getParent();
        String delib = file.getName();
        return status(delib, dir);
    }

    /**
     * Get the status for the Cell.  Returns null on error.
     * @param cell
     * @return
     */
    public static StatusResult status(Cell cell) {
        File file = CVS.getCellFile(cell);
        if (!CVS.isFileInCVS(CVS.getCellFile(cell))) return null;

        String dir = file.getParent();
        String c = file.getName();
        return status(c, dir);
    }

    /**
     * Get the status for a given file, in the given directory.
     * @param file
     * @param dir
     * @return
     */
    public static StatusResult status(String file, String dir) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CVS.runCVSCommand("-nq update "+file, "Getting status for "+file,
                    dir, out);
        LineNumberReader result = new LineNumberReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));
        return parseOutput(result);
    }

    /**
     * Parse the output of an 'cvs -nq update' command, which
     * checks the status of the given files.
     * Returns true if all files are up-to-date, false otherwise
     * @param reader
     * @return
     */
    private static StatusResult parseOutput(LineNumberReader reader) {
        StatusResult result = new StatusResult();
        for (;;) {
            String line;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                return result;
            }
            if (line == null) break;
            if (line.equals("")) continue;

            String parts[] = line.split("\\s");
            if (parts.length != 2) continue;
            State state = State.getState(parts[0]);
            if (state == null) continue;
            if (state == State.UPDATE || state == State.PATCHED) {
                result.updated.add(parts[1]);
                continue;
            }
            if (state == State.ADDED) {
                result.added.add(parts[1]);
                continue;
            }
            if (state == State.REMOVED) {
                result.removed.add(parts[1]);
                continue;
            }
            if (state == State.MODIFIED) {
                result.modified.add(parts[1]);
                continue;
            }
            if (state == State.CONFLICT) {
                result.conflict.add(parts[1]);
                continue;
            }
            if (state == State.UNKNOWN) {
                result.unknown.add(parts[1]);
                continue;
            }
        }
        return result;
    }

    /**
     * Parse the output of an 'cvs -nq update' command, which
     * checks the status of the given files.
     * Returns true if all files are up-to-date, false otherwise
     * @return
     */
    public static boolean commentStatus(StatusResult result) {
        if (result.added.size() > 0) {
            for (String s : result.added) System.out.println("Added\t"+s);
        }
        if (result.removed.size() > 0) {
            for (String s : result.removed) System.out.println("Removed\t"+s);
        }
        if (result.modified.size() > 0) {
            for (String s : result.modified) System.out.println("Modified\t"+s);
        }
        if (result.conflict.size() > 0) {
            for (String s : result.conflict) System.out.println("Conflicts\t"+s);
        }
        if (result.updated.size() > 0) {
            for (String s : result.updated) System.out.println("NeedsUpdate\t"+s);
        }

        if (result.getTotalMentioned() == 0) {
            return true;
            //System.out.println("All files up-to-date");
        } else {
            return false;
            //System.out.println("All other files up-to-date.");
        }
    }

    /**
     * Parse the output of an update command, 'cvs -q update'.
     * @return 1 if nothing needs to be done, 0 if library
     * should be reloaded from disk, -1 if conflicts were found.
     */
    private static int commentUpdate(StatusResult result) {
        if (result.added.size() > 0) {
            for (String s : result.added) System.out.println("Added\t"+s);
        }
        if (result.removed.size() > 0) {
            for (String s : result.removed) System.out.println("Removed\t"+s);
        }
        if (result.modified.size() > 0) {
            for (String s : result.modified) System.out.println("Modified\t"+s);
        }
        if (result.conflict.size() > 0) {
            for (String s : result.conflict) System.out.println("Conflicts\t"+s);
        }
        if (result.updated.size() > 0) {
            for (String s : result.updated) System.out.println("Updated\t"+s);
        }

        if (result.conflict.size() > 0) {
            return -1;  //conflicts found
        }
        if (result.updated.size() > 0) {
            return 0;   // need to reload from disk
        }
        return 1;
    }

    public static class StatusResult {
        private List<String> updated = new ArrayList<String>();
        private List<String> added = new ArrayList<String>();
        private List<String> removed = new ArrayList<String>();
        private List<String> modified = new ArrayList<String>();
        private List<String> conflict = new ArrayList<String>();
        private List<String> unknown = new ArrayList<String>();
        private StatusResult() {}
        public List<String> getUpdated() { return updated; }
        public List<String> getAdded() { return added; }
        public List<String> getRemoved() { return removed; }
        public List<String> getModified() { return modified; }
        public List<String> getConflicts() { return conflict; }
        public List<String> getUnknown() { return unknown; }
        public int getTotalMentioned() {
            return (added.size() + removed.size() + modified.size() +
                    conflict.size() + updated.size());
        }
    }

}
