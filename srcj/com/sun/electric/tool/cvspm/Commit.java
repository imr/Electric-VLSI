/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Commit.java
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
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.io.FileType;

import javax.swing.JOptionPane;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Mar 15, 2006
 * Time: 4:24:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class Commit {

    public static void commitAllLibraries() {
        List<Library> libs = new ArrayList<Library>();
        for (Iterator<Library> it = Library.getLibraries(); it.hasNext(); ) {
            Library lib = it.next();
            if (lib.isHidden()) continue;
            if (!lib.isFromDisk()) continue;
            if (lib.getName().equals("spiceparts")) continue;
            libs.add(lib);
        }
        commit(libs, null);
    }

    /**
     * Commit a library into CVS.
     * @param lib
     */
    public static void commit(Library lib) {
        List<Library> libs = new ArrayList<Library>();
        libs.add(lib);
        commit(libs, null);
    }

    /**
     * Commit a Cell into CVs. Only works with DELIB file format.
     * Does not commit the header file for the library.
     * @param cell
     */
    public static void commit(Cell cell) {
        List<Cell> cellsCommitted = new ArrayList<Cell>();
        cellsCommitted.add(cell);
        commit(null, cellsCommitted);
    }

    public static void commit(List<Library> libs, List<Cell> cells) {
        if (libs == null) libs = new ArrayList<Library>();
        if (cells == null) cells = new ArrayList<Cell>();

        // make sure cells are part of a DELIB
        CVSLibrary.LibsCells bad = CVSLibrary.notFromDELIB(cells);
        if (bad.cells.size() > 0) {
            CVS.showError("Error: the following Cells are not part of a DELIB library and cannot be acted upon individually",
                    "CVS Commit Error", bad.libs, bad.cells);
            return;
        }
        bad = CVSLibrary.getNotInCVS(libs, cells);
        if (bad.libs.size() > 0 || bad.cells.size() > 0) {
            CVS.showError("Error: the following Libraries and Cells are not in CVS",
                    "CVS Commit error", bad.libs, bad.cells);
            return;
        }
        bad = CVSLibrary.getModified(libs, cells);
        if (bad.libs.size() > 0 || bad.cells.size() > 0) {
            String [] choices = new String [] { "Continue Anyway", "Cancel" };
            int choice = CVS.askForChoice("Warning: Unsaved changes will not be committed!  For:",
                    "CVS Commit Warning!",
                    bad.libs, bad.cells, choices, choices[1]);
            if (choice == 1) return;
        }

        // optimize a little, remove cells from cells list if cell's lib in libs list
        CVSLibrary.LibsCells good = CVSLibrary.consolidate(libs, cells);

        // get commit message
        String lastCommitMessage = CVS.getCVSLastCommitMessage();
        String commitMessage = JOptionPane.showInputDialog(
                TopLevel.getCurrentJFrame(),
                "Commit message: ",
                lastCommitMessage
        );
        if (commitMessage == null) return;  // user cancelled
        if (!commitMessage.equals(lastCommitMessage))
            CVS.setCVSLastCommitMessage(commitMessage);

        (new CommitJob(commitMessage, good.libs, good.cells)).startJob();
    }

    private static class CommitJob extends Job {
        private String message;
        private List<Library> libsToCommit;
        private List<Cell> cellsToCommit;
        private int exitVal;
        /**
         * Commit cells and/or libraries.
         * @param message the commit log message
         * @param libsToCommit
         * @param cellsToCommit
         */
        private CommitJob(String message, List<Library> libsToCommit, List<Cell> cellsToCommit) {
            super("CVS Commit", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.message = message;
            this.libsToCommit = libsToCommit;
            this.cellsToCommit = cellsToCommit;
            exitVal = -1;
            if (this.libsToCommit == null) this.libsToCommit = new ArrayList<Library>();
            if (this.cellsToCommit == null) this.cellsToCommit = new ArrayList<Cell>();
        }
        public boolean doIt() {
            // list of library files to commit
            String useDir = CVS.getUseDir(libsToCommit, cellsToCommit);
            StringBuffer libs = CVS.getLibraryFiles(libsToCommit, useDir);
            StringBuffer cells = CVS.getCellFiles(cellsToCommit, useDir);
            //StringBuffer lastModifiedFiles = CVS.getLastModifiedFilesForCommit(libsToCommit, cellsToCommit, useDir);
            StringBuffer headerFiles = CVS.getHeaderFilesForCommit(libsToCommit, cellsToCommit, useDir);
            String commitFiles = libs + " " + headerFiles + " " + cells;
            if (commitFiles.trim().equals("")) {
                System.out.println("Nothing to commit");
                exitVal = 0;
                return true;
            }

            // check if any header file conflicts
            // if so, re-write header file for that library
            if (!headerFiles.toString().trim().equals("")) {
                Update.StatusResult result = Update.update(headerFiles.toString(), useDir, Update.UPDATE);
                if (result.getExitVal() == 0) {
                    List<Library> headerlibs = result.getLibraryHeaderFiles(State.CONFLICT);
                    for (Library lib : headerlibs) {
                        // rewrite the header file
                        Output.writeLibrary(lib, FileType.DELIB, false, true, true);
                    }
                }
            }

            exitVal = CVS.runCVSCommandWithQuotes("-q commit -m \""+message+"\" "+commitFiles, "Committing files to CVS", useDir, System.out);
            System.out.println("Commit complete");
            fieldVariableChanged("exitVal");
            return true;
        }
        public void terminateOK() {
            if (exitVal != 0) {
                Job.getUserInterface().showErrorMessage("CVS Commit Failed!  Please see messages window (exit status "+exitVal+")", "CVS Commit Failed!");
                return;
            }

            // remove editors for lib
            for (Library lib : libsToCommit) {
                CVSLibrary.setEditing(lib, false);
                if (CVSLibrary.getState(lib) == State.REMOVED)
                    CVSLibrary.setState(lib, State.UNKNOWN);
                else
                    CVSLibrary.setState(lib, State.NONE);
            }
            if (cellsToCommit != null) {
                for (Cell cell : cellsToCommit) {
                    CVSLibrary.setEditing(cell, false);
                    if (CVSLibrary.getState(cell) == State.REMOVED)
                        CVSLibrary.setState(cell, State.UNKNOWN);
                    else
                        CVSLibrary.setState(cell, State.NONE);
                }
            }
        }
    }

}
