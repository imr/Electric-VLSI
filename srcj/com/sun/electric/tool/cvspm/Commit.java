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
        for (Iterator<Library> it = Library.getLibraries(); it.hasNext(); ) {
            Library lib = it.next();
            commit(lib);
        }
    }

    /**
     * Commit a library into CVS.
     * @param lib
     */
    public static void commit(Library lib) {
        if (!CVS.assertInCVS(lib, "Commit", true)) return;
        if (!CVS.assertNotModified(lib, "Commit", true)) return;

        String lastCommitMessage = CVS.getCVSLastCommitMessage();
        String commitMessage = JOptionPane.showInputDialog(
                TopLevel.getCurrentJFrame(),
                "Commit message for Library "+lib.getName()+":",
                lastCommitMessage
        );
        if (commitMessage == null) return;  // user cancelled
        if (!commitMessage.equals(lastCommitMessage))
            CVS.setCVSLastCommitMessage(commitMessage);

        List<Library> libsCommitted = new ArrayList<Library>();
        libsCommitted.add(lib);
        (new CommitJob(commitMessage, libsCommitted, null)).startJob();
    }

    /**
     * Commit a Cell into CVs. Only works with DELIB file format.
     * Does not commit the header file for the library.
     * @param cell
     */
    public static void commit(Cell cell) {
        if (!CVS.assertInCVS(cell, "Commit", true)) return;
        if (!CVS.assertNotModified(cell, "Commit", true)) return;

        String lastCommitMessage = CVS.getCVSLastCommitMessage();
        String commitMessage = JOptionPane.showInputDialog(
                TopLevel.getCurrentJFrame(),
                "Commit message for Cell "+cell.libDescribe()+":",
                lastCommitMessage
        );
        if (commitMessage == null) return;  // user cancelled
        if (!commitMessage.equals(lastCommitMessage))
            CVS.setCVSLastCommitMessage(commitMessage);

        List<Cell> cellsCommitted = new ArrayList<Cell>();
        cellsCommitted.add(cell);
        (new CommitJob(commitMessage, null, cellsCommitted)).startJob();
    }

    private static class CommitJob extends Job {
        private String message;
        private List<Library> libsToCommit;
        private List<Cell> cellsToCommit;
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
            if (this.libsToCommit == null) this.libsToCommit = new ArrayList<Library>();
            if (this.cellsToCommit == null) this.cellsToCommit = new ArrayList<Cell>();
        }
        public boolean doIt() {
            // list of library files to commit
            String useDir = CVS.getUseDir(libsToCommit, cellsToCommit);
            StringBuffer libs = CVS.getLibraryFiles(libsToCommit, useDir);
            StringBuffer cells = CVS.getCellFiles(cellsToCommit, useDir);
            StringBuffer lastModifiedFiles = CVS.getDELIBLastModifiedFiles(libsToCommit, cellsToCommit, useDir);
            String commitFiles = libs + " " + lastModifiedFiles + " " + cells;
            if (commitFiles.trim().equals("")) {
                System.out.println("Nothing to commit");
                return true;
            }

            CVS.runCVSCommandWithQuotes("-q commit -m \""+message+"\" "+commitFiles, "Committing files to CVS", useDir, System.out);
            System.out.println("Commit complete");
            return true;
        }
        public void terminateOK() {
            // remove editors for lib
            for (Library lib : libsToCommit) {
                CVSLibrary.setEditing(lib, false);
                CVSLibrary.setState(lib, State.NONE);
            }
            if (cellsToCommit != null) {
                for (Cell cell : cellsToCommit) {
                    CVSLibrary.setEditing(cell, false);
                    CVSLibrary.setState(cell, State.NONE);
                }
            }
        }
    }

}
