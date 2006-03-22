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
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.TopLevel;

import javax.swing.JOptionPane;
import java.io.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Mar 13, 2006
 * Time: 3:30:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class Update {

    public static final int UPDATE = 0;
    public static final int STATUS = 1;
    public static final int ROLLBACK = 2;

    // ------------------ Update/Status ---------------------

    /**
     * Update all libraries.
     * @param type the type of update to do
     */
    public static void updateAllLibraries(int type) {
        boolean dialog = true;
        if (type == STATUS) dialog = false;

        List<Library> allLibs = new ArrayList<Library>();
        for (Iterator<Library> it = Library.getLibraries(); it.hasNext(); ) {
            Library lib = it.next();
            if (lib.isHidden()) continue;
            if (!lib.isFromDisk()) continue;
            if (!CVS.assertInCVS(lib, getMessage(type), dialog)) continue;
            if (type != STATUS && !CVS.assertNotModified(lib, getMessage(type), dialog)) continue;
            allLibs.add(lib);
        }
        (new UpdateJob(null, allLibs, type)).startJob();
    }

    /**
     * Update all Cells from a library.
     * @param lib
     * @param type the type of update to do
     */
    public static void updateLibrary(Library lib, int type) {
        boolean dialog = true;
        if (type == STATUS) dialog = false;

        if (!CVS.assertInCVS(lib, getMessage(type), dialog)) return;
        if (type == UPDATE && !CVS.assertNotModified(lib, getMessage(type), dialog)) return;

        List<Library> libsToUpdate = new ArrayList<Library>();
        libsToUpdate.add(lib);
        (new UpdateJob(null, libsToUpdate, type)).startJob();
    }

    /**
     * Update a Cell.
     * @param cell
     * @param type the type of update to do
     */
    public static void updateCell(Cell cell, int type) {
        boolean dialog = true;
        if (type == STATUS) dialog = false;

        if (!CVS.assertInCVS(cell, getMessage(type), dialog)) return;
        if (type != STATUS && !CVS.assertNotModified(cell, getMessage(type), dialog)) return;

        List<Cell> cellsToUpdate = new ArrayList<Cell>();
        cellsToUpdate.add(cell);
        (new UpdateJob(cellsToUpdate, null, type)).startJob();
    }

    private static class UpdateJob extends Job {
        private List<Cell> cellsToUpdate;
        private List<Library> librariesToUpdate;
        private int type;
        /**
         * Update cells and/or libraries.
         * @param cellsToUpdate
         * @param librariesToUpdate
         */
        private UpdateJob(List<Cell> cellsToUpdate, List<Library> librariesToUpdate,
                          int type) {
            super("CVS Update Library", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.cellsToUpdate = cellsToUpdate;
            this.librariesToUpdate = librariesToUpdate;
            this.type = type;
            if (this.cellsToUpdate == null) this.cellsToUpdate = new ArrayList<Cell>();
            if (this.librariesToUpdate == null) this.librariesToUpdate = new ArrayList<Library>();
        }
        public boolean doIt() {
            String useDir = CVS.getUseDir(librariesToUpdate, cellsToUpdate);
            StringBuffer libs = CVS.getLibraryFiles(librariesToUpdate, useDir);
            StringBuffer cells = CVS.getCellFiles(cellsToUpdate, useDir);
            String updateFiles = libs.toString() + " " + cells.toString();
            if (updateFiles.trim().equals("")) return true;

            StatusResult result = update(updateFiles, useDir, type);
            commentStatusResult(result, type);

            // reload libs if needed
            if (type != STATUS) {
                List<Library> libsToReload = new ArrayList<Library>();
                for (Cell cell : result.getCells(State.UPDATE)) {
                    Library lib = cell.getLibrary();
                    if (!libsToReload.contains(lib))
                        libsToReload.add(lib);
                }
                for (Library lib : libsToReload) {
                    LibraryFiles.reloadLibrary(lib);
                }
            }
            if (type == ROLLBACK) {
                // turn off edit for rolled back cells
                for (Cell cell : result.getCells(State.UPDATE)) {
                    CVSLibrary.setEditing(cell, false);
                }
            }
            // update states
            updateStates(result);
            System.out.println(getMessage(type)+" complete.");
            return true;
        }
    }

    /**
     * Update the given file in the given directory.
     * @param file
     * @param dir
     * @return
     */
    private static StatusResult update(String file, String dir, int type) {
        String command = "-q update -P ";
        String message = "Running CVS Update";
        if (type == STATUS) {
            command = "-nq update ";
            message = "Running CVS Status";
        }
        if (type == ROLLBACK) {
            command = "-q update -C -P ";
            message = "Rollback from CVS";
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CVS.runCVSCommand(command+file, message,
                    dir, out);
        LineNumberReader result = new LineNumberReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));
        return parseOutput(result);
    }

    private static String getMessage(int type) {
        switch(type) {
            case 0: return "Update";
            case 1: return "Status";
            case 2: return "Rollback";
        }
        return "";
    }

    private static void updateStates(StatusResult result) {
        for (Cell cell : result.getCells(State.ADDED)) {
            CVSLibrary.setState(cell, State.ADDED);
        }
        for (Cell cell : result.getCells(State.REMOVED)) {
            CVSLibrary.setState(cell, State.REMOVED);
        }
        for (Cell cell : result.getCells(State.MODIFIED)) {
            CVSLibrary.setState(cell, State.MODIFIED);
        }
        for (Cell cell : result.getCells(State.CONFLICT)) {
            CVSLibrary.setState(cell, State.CONFLICT);
        }
        for (Cell cell : result.getCells(State.UPDATE)) {
            CVSLibrary.setState(cell, State.UPDATE);
        }
        for (Cell cell : result.getCells(State.UNKNOWN)) {
            CVSLibrary.setState(cell, State.UNKNOWN);
        }

    }

    // -------------------- Rollback ----------------------------

    public static void rollback(Cell cell) {
        int ret = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(),
                "WARNING! Disk file for Cell "+cell.libDescribe()+" will revert to latest CVS version!\n"+
                "All uncommited changes will be lost!!!  Continue anyway?", "Rollback Cell", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (ret == JOptionPane.NO_OPTION) return;

        updateCell(cell, ROLLBACK);
    }

    public static void rollback(Library lib) {
        int ret = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(),
                "WARNING! Disk file(s) for Library"+lib.getName()+" will revert to latest CVS version!\n"+
                "All uncommited changes will be lost!!!  Continue anyway?", "Rollback Library", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (ret == JOptionPane.NO_OPTION) return;

        updateLibrary(lib, ROLLBACK);
    }

    // ---------------------- Output Parsing -------------------------

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
            if (state == State.PATCHED) state = State.UPDATE;

            // find Cell for filename
            String filename = parts[1];
            File file = new File(filename);
            if (filename.toLowerCase().endsWith(".jelib")) {
                // jelib library file, set state of all cells
                Library lib = Library.findLibrary(file.getName());
                if (lib == null) continue;
                for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
                    Cell cell = it.next();
                    result.addCell(state, cell);
                }
            }
            Cell cell = CVS.getCellFromPath(filename);
            if (cell != null) {
                result.addCell(state, cell);
            }
        }
        return result;
    }

    /**
     * Parse the output of an 'cvs -nq update' command, which
     * checks the status of the given files.
     * Returns true if all files are up-to-date, false otherwise
     */
    public static void commentStatusResult(StatusResult result, int type) {
        boolean allFilesUpToDate = true;
        for (Cell cell : result.getCells(State.ADDED)) {
            System.out.println("Added\t"+cell.libDescribe());
            allFilesUpToDate = false;
        }
        for (Cell cell : result.getCells(State.REMOVED)) {
            System.out.println("Removed\t"+cell.libDescribe());
            allFilesUpToDate = false;
        }
        for (Cell cell : result.getCells(State.MODIFIED)) {
            System.out.println("Modified\t"+cell.libDescribe());
            allFilesUpToDate = false;
        }
        for (Cell cell : result.getCells(State.CONFLICT)) {
            System.out.println("Conflicts\t"+cell.libDescribe());
            allFilesUpToDate = false;
        }
        for (Cell cell : result.getCells(State.UPDATE)) {
            if (type == STATUS)
                System.out.println("NeedsUpdate\t"+cell.libDescribe());
            if (type == UPDATE)
                System.out.println("Updated\t"+cell.libDescribe());
            allFilesUpToDate = false;
        }
        if (type == STATUS) {
            if (allFilesUpToDate) System.out.println("All files up-to-date");
            else System.out.println("All other files up-to-date");
        }
    }

    public static class StatusResult {
        private Map<State,List<Cell>> cells;

        private StatusResult() {
            cells = new HashMap<State,List<Cell>>();
        }
        private void addCell(State state, Cell cell) {
            List<Cell> statecells = cells.get(state);
            if (statecells == null) {
                statecells = new ArrayList<Cell>();
                cells.put(state, statecells);
            }
            statecells.add(cell);
        }
        public List<Cell> getCells(State state) {
            List<Cell> statecells = cells.get(state);
            if (statecells == null)
                statecells = new ArrayList<Cell>();
            return statecells;
        }
    }

}
