/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Edit.java
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

package com.sun.electric.tool.cvspm;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.id.CellId;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Exec;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.text.DateFormat;
import java.net.InetAddress;
import java.awt.*;

/**
 * User: gainsley
 * Date: Mar 15, 2006
 */
public class Edit {

    // ------------------------ Edit -----------------------------

    /**
     * Mark the current user as an editor of the cell
     * @param cell the Cell to mark.
     * @return true if successful.
     */
    public static boolean edit(Cell cell) {
        File file = CVS.getCellFile(cell);
        if (!CVS.isDELIB(cell.getLibrary())) return false;
        if (!CVS.isFileInCVS(CVS.getCellFile(cell))) return false;

        String dir = file.getParent();
        String c = file.getName();

        boolean success = edit(c, dir);
        return success;
    }

    /**
     * Establish a lock the file in dir for your exclusive edit.
     * If anyone else is editing the file, this returns false, and
     * if 'showDialog' is true, pops up an error dialog, otherwise it
     * just prints to System.out.
     * @return true if the edit lock is now yours, false otherwise.
     */
    public static boolean edit(String file, String dir) {

        // check if anyone else is editing the file
        // no one editing, set edit lock
        CVS.runCVSCommand(CVS.getCVSProgram(), CVS.getRepository(), "edit -a none "+file, "Edit",
                dir, System.out);
        return true;
    }

    // ---------------------- Get Editors ----------------------------

    public static void listEditorsProject() {
        List<Library> allLibs = new ArrayList<Library>();
        for (Iterator<Library> it = Library.getLibraries(); it.hasNext(); ) {
            Library lib = it.next();
            if (lib.isHidden()) continue;
            if (!lib.isFromDisk()) continue;
            allLibs.add(lib);
        }
        (new ListEditorsJob(allLibs, null, true)).startJob();
    }

    public static void listEditorsOpenLibraries() {
        List<Library> allLibs = new ArrayList<Library>();
        for (Iterator<Library> it = Library.getLibraries(); it.hasNext(); ) {
            Library lib = it.next();
            if (lib.isHidden()) continue;
            if (!lib.isFromDisk()) continue;
            allLibs.add(lib);
        }
        listEditors(allLibs, null);
    }

    public static void listEditors(Library lib) {
        List<Library> libs = new ArrayList<Library>();
        libs.add(lib);
        listEditors(libs, null);
    }

    public static void listEditors(Cell cell) {
        List<Cell> cells = new ArrayList<Cell>();
        cells.add(cell);
        listEditors(null, cells);
    }

    public static void listEditors(List<Library> libs, List<Cell> cells) {
        (new ListEditorsJob(libs, cells, false)).startJob();
    }

    public static class ListEditorsJob extends Job {
        private List<Library> libs;
        private List<Cell> cells;
        private boolean forProject;
        private String cvsProgram = CVS.getCVSProgram();
        private String repository = CVS.getRepository();
        public ListEditorsJob(List<Library> libs, List<Cell> cells, boolean forProject) {
            super("List CVS Editors", User.getUserTool(), Job.Type.CLIENT_EXAMINE, null, null, Job.Priority.USER);
            this.libs = libs;
            this.cells = cells;
            this.forProject = forProject;
            if (this.libs == null) this.libs = new ArrayList<Library>();
            if (this.cells == null) this.cells = new ArrayList<Cell>();
        }
        public boolean doIt() {
            String useDir = CVS.getUseDir(libs, cells);
            StringBuffer libsBuf = CVS.getLibraryFiles(libs, useDir);
            StringBuffer cellsBuf = CVS.getCellFiles(cells, useDir);

            String args = libsBuf + " " + cellsBuf;
            if (args.trim().equals("")) return true;

            if (forProject) args = "";
            CVS.runCVSCommand(cvsProgram, repository, "editors "+args, "List CVS Editors", useDir, System.out);
            System.out.println("List CVS Editors complete.");
            return true;
        }
    }

/*
    static List<Editor> getEditors(Library lib) {
        File file = new File(lib.getLibFile().getPath());
        if (!CVS.isFileInCVS(file)) {
            // library not in CVS
            System.out.println(lib.getName()+" is not in CVS");
            return null;
        }
        String dir = file.getParent();
        String delib = file.getName();
        return getEditors(delib, dir);

    }

    static List<Editor> getEditors(Cell cell) {
        File file = CVS.getCellFile(cell);
        if (!CVS.isFileInCVS(CVS.getCellFile(cell))) return null;

        String dir = file.getParent();
        String c = file.getName();
        return getEditors(c, dir);
    }

    static List<Editor> getEditors(String file, String dir) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CVS.runCVSCommand("editors "+file, "Checking for editors of "+file,
                dir, out);
        LineNumberReader result = new LineNumberReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));
        return parseOutput(result);
    }
*/

    // ----------------------- Editing Modified Cells -----------------------

    private static Map<CellId,CellId> modifiedCells = new HashMap<CellId,CellId>();

    /**
      * Handles database changes of a Job.
      * @param oldSnapshot database snapshot before Job.
      * @param newSnapshot database snapshot after Job and constraint propagation.
      * @param undoRedo true if Job was Undo/Redo job.
      */
    static void endBatch(Snapshot oldSnapshot, Snapshot newSnapshot, boolean undoRedo) {
        //if (undoRedo) return;

        // keep track of which cells are newly modified
        List<Cell> newlyModifiedCells = new ArrayList<Cell>();
        List<Cell> newlyUnmodifiedCells = new ArrayList<Cell>();
        for (CellId cellId: newSnapshot.getChangedCells(oldSnapshot)) {
            Cell cell = Cell.inCurrentThread(cellId);
            if (cell == null) {
                modifiedCells.remove(cellId);
                continue;
            }
            if (cell.isModified() && !modifiedCells.containsKey(cellId)) {
                modifiedCells.put(cellId, cellId);
                newlyModifiedCells.add(cell);
            }
            if (!cell.isModified() && modifiedCells.containsKey(cellId)) {
                // undo or save
                modifiedCells.remove(cellId);
                newlyUnmodifiedCells.add(cell);
            }
        }

        // mark for edit any newly modified cells that are not modified in CVS
        // (if they are modified in CVS, they have already been marked for edit
        List<Cell> markForEdit = new ArrayList<Cell>();
        for (Cell cell : newlyModifiedCells) {
            State state = CVSLibrary.getState(cell);
            if (state == State.NONE || state == State.CONFLICT || state == State.UPDATE)
                markForEdit.add(cell);
        }

        // unmark for edit any newly unmodified cells that are not modified in CVS
        // (this will only happen if you undo to the point of being unmodified,
        //  and the cell is then consistent with what is in CVS)
        List<Cell> unmarkForEdit = new ArrayList<Cell>();
        for (Cell cell : newlyUnmodifiedCells) {
            State state = CVSLibrary.getState(cell);
            if (state == State.NONE || state == State.CONFLICT || state == State.UPDATE)
                unmarkForEdit.add(cell);
        }

        // condense jelibs cells into jelibs
        CVSLibrary.LibsCells modified = CVSLibrary.getInCVSSorted(new ArrayList<Library>(), markForEdit);
        if (modified.libs.size() != 0 || modified.cells.size() != 0) {
            (new MarkForEditJob(modified.libs, modified.cells, true, false)).startJob();
        }
        CVSLibrary.LibsCells unmodified = CVSLibrary.getInCVSSorted(new ArrayList<Library>(), unmarkForEdit);
        if (unmodified.libs.size() != 0 || unmodified.cells.size() != 0) {
            (new MarkForEditJob(unmodified.libs, unmodified.cells, false, true)).startJob();
        }
    }

    public static class MarkForEditJob extends Job {
        private List<Library> libs;
        private List<Cell> cells;
        private boolean unedit;         // true to unmark rather than mark
        private boolean checkConflicts;
        private List<String> uneditMatchedStrings;
        private List<Editor> editors;
        private String cvsProgram = CVS.getCVSProgram();
        private String repository = CVS.getRepository();

        public MarkForEditJob(List<Library> libs, List<Cell> cells, boolean checkConflicts, boolean unedit) {
            super("Check CVS Editors", User.getUserTool(), Job.Type.CLIENT_EXAMINE, null, null, Job.Priority.USER);
            this.libs = libs;
            this.cells = cells;
            this.checkConflicts = checkConflicts;
            this.unedit = unedit;
            this.editors = new ArrayList<Editor>();
            if (this.libs == null) this.libs = new ArrayList<Library>();
            if (this.cells == null) this.cells = new ArrayList<Cell>();
        }
        public boolean doIt() {
            String useDir = CVS.getUseDir(libs, cells);
            StringBuffer libsBuf = CVS.getLibraryFiles(libs, useDir);
            StringBuffer cellsBuf = CVS.getCellFiles(cells, useDir);

            if (unedit) {
                fieldVariableChanged("uneditMatchedStrings");
                // can only unedit files that are up-to-date
                // otherwise, you cvs will ask if you want to rollback local copy
                List<Library> libsToUnedit = new ArrayList<Library>();
                List<Cell> cellsToUnedit = new ArrayList<Cell>();
                for (Library lib : libs) {
                    State state = CVSLibrary.getState(lib);
                    if (state == State.NONE) libsToUnedit.add(lib);
                }
                for (Cell cell : cells) {
                    State state = CVSLibrary.getState(cell);
                    if (state == State.NONE) cellsToUnedit.add(cell);
                }
                libsBuf = CVS.getLibraryFiles(libsToUnedit, useDir);
                cellsBuf = CVS.getCellFiles(cellsToUnedit, useDir);

                String args = libsBuf + " " + cellsBuf;
                if (args.trim().equals("")) return true;

                Exec.OutputStreamChecker checker = new Exec.OutputStreamChecker(System.out, "has been modified; revert changes?", false, null);
                UneditResponder uneditResponder = new UneditResponder();
                checker.addOutputStreamCheckerListener(uneditResponder);
//                checker.addOutputStreamCheckerListener(new UneditResponder(libs, cells));

                //System.out.println("Unmarking CVS edit: "+args);
                CVS.runCVSCommand(cvsProgram, repository, "unedit -l "+args, "CVS Unedit", useDir, checker);
                uneditMatchedStrings = uneditResponder.matchedStrings;
                return true;
            }

            String args = libsBuf + " " + cellsBuf;
            if (args.trim().equals("")) return true;

            if (checkConflicts) {
                //System.out.println("Checking editors for: "+args);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                CVS.runCVSCommand(cvsProgram, repository, "editors "+args, "Check CVS Editors", useDir, out);
                LineNumberReader reader = new LineNumberReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));
                editors = parseOutput(reader);
                // also run status to see if they need an update
                Update.StatusResult status = Update.update(cvsProgram, repository, args, useDir, Update.UpdateEnum.STATUS);
                for (Cell cell : status.getCells(State.CONFLICT)) {
                    Editor e = new Editor(cell.describe(false), "CONFLICT", new Date(), "", "");
                    editors.add(e);
                }
                for (Cell cell : status.getCells(State.UPDATE)) {
                    Editor e = new Editor(cell.describe(false), "NEEDS UPDATE", new Date(), "", "");
                    editors.add(e);
                }
                fieldVariableChanged("editors");
            } else {
                //System.out.println("Marking for CVS edit: "+args);
                CVS.runCVSCommand(cvsProgram, repository, "edit -a none "+args, "CVS Edit", useDir, System.out);
            }
            return true;
        }

        @Override
        public void terminateOK() {
            if (unedit) {
                for (String matched: uneditMatchedStrings) {
                    // set status to modified
                    String [] parts = matched.split("\\s+");
                    if (parts[0].endsWith(".jelib")) {
                        Library lib = Library.findLibrary(parts[0].substring(0, parts[0].length()-6));
                        if (lib != null) {
                            CVSLibrary.setState(lib, State.MODIFIED);
                        }
                    } else {
                        // try delib
                        Cell cell = CVS.getCellFromPath(parts[0]);
                        if (cell != null) {
                            CVSLibrary.setState(cell, State.MODIFIED);
                        }
                    }
                }
            } else if (checkConflicts) {
                // if there are any editors, let the user know.
                List<Editor> filteredEditors = new ArrayList<Editor>();
                if (editors != null) {
                    for (Editor e : editors) {
                        if (e.getUser().equals(System.getProperty("user.name"))) continue;
                        filteredEditors.add(e);
                    }
                }
                editors = filteredEditors;

                if (editors.size() > 0) {
                    JPanel panel = new JPanel(new GridBagLayout());
                    GridBagConstraints constraints = new GridBagConstraints();
                    constraints.gridx = 0;
                    constraints.gridy = 0;
                    constraints.ipady = 20;
                    constraints.anchor = GridBagConstraints.WEST;
                    JLabel label = new JLabel("Other Users are already Editing the following:");
                    panel.add(label, constraints);

                    // table of edit conflicts
                    String [] headers = {"File", "User"};
                    Object [][] data = new Object[editors.size()][2];
                    for (int i=0; i<editors.size(); i++) {
                        Editor editor = editors.get(i);
                        data[i][0] = editor.getAbbrevFile();
                        data[i][1] = editor.getUser();
                    }

                    JTable table = new JTable(data, headers);
                    table.getColumnModel().getColumn(0).setPreferredWidth(300);
                    table.getColumnModel().getColumn(1).setPreferredWidth(100);
                    table.setPreferredScrollableViewportSize(new Dimension(400,100));
                    table.setFocusable(false);
                    JScrollPane scrollpane = new JScrollPane(table);
                    scrollpane.setPreferredSize(new Dimension(400,100));
                    Font font = new Font("Courier", Font.PLAIN, 12);
                    table.setFont(font);
                    constraints = new GridBagConstraints();
                    constraints.gridx = 0;
                    constraints.gridy = 1;
                    panel.add(scrollpane, constraints);

                    Object [] options = { "UNDO CHANGES", "CONTINUE ANYWAY" };
                    int ret = JOptionPane.showOptionDialog(null, panel, "Edit Conflict!",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                    if (ret == 0) {
                        // undo
                        Undo.undo();
                        return;
                    }
                }
                (new MarkForEditJob(libs, cells, false, unedit)).startJob();
            }
        }
    }

    private static class UneditResponder implements Exec.OutputStreamCheckerListener {
        private List<String> matchedStrings = new ArrayList<String>();

//        List<Library> libs;
//        List<Cell> cells;
//        private UneditResponder(List<Library> libs, List<Cell> cells) {
//            this.libs = libs;
//            this.cells = cells;
//        }

        public void matchFound(Exec process, String matched) {
            process.writeln("n\n");
            matchedStrings.add(matched);
//            // set status to modified
//            String [] parts = matched.split("\\s+");
//            if (parts[0].endsWith(".jelib")) {
//                Library lib = Library.findLibrary(parts[0].substring(0, parts[0].length()-6));
//                if (lib != null) {
//                    CVSLibrary.setState(lib, State.MODIFIED);
//                }
//            } else {
//                // try delib
//                Cell cell = CVS.getCellFromPath(parts[0]);
//                if (cell != null) {
//                    CVSLibrary.setState(cell, State.MODIFIED);
//                }
//            }
        }
    }

    // ---------------------- Edit State Consistency Check ------------------

    /**
     * Consistency check - a cell that is either modified in Electric,
     * or that is CVS modified, should be marked for Edit.  A cell
     * that both unmodified in Electric and CVS should not be marked
     * for Edit.
     */
    public static void editConsistencyCheck() {
        List<Library> allLibs = new ArrayList<Library>();
        for (Iterator<Library> it = Library.getLibraries(); it.hasNext(); ) {
            Library lib = it.next();
            if (lib.isHidden()) continue;
            if (!lib.isFromDisk()) continue;
            allLibs.add(lib);
        }
        editConsistencyCheck(allLibs, new ArrayList<Cell>());
    }

    public static void editConsistencyCheck(List<Library> libs, List<Cell> cells) {
/*
        // get jelibs and delib cells in cvs
        CVSLibrary.LibsCells incvs = CVSLibrary.getInCVSSorted(libs, cells);
        // get unmodified in both Electric and CVS
        CVSLibrary.LibsCells unmodified = new CVSLibrary.LibsCells();
        for (Library lib : incvs.libs) {
            if (!lib.isChanged() && CVSLibrary.getState(lib) == State.NONE)
                unmodified.libs.add(lib);
        }
        for (Cell cell : incvs.cells) {
            if (!cell.isModified(false) && CVSLibrary.getState(cell) == State.NONE)
                unmodified.cells.add(cell);
        }
        // make sure unmodified libs/cells are not being edited by me
        (new EditConsistencyCheckJob(unmodified.libs, unmodified.cells)).startJob();
        // I don't make sure that modified cells are marked Edit,
        // as that will be fixed once the user commits and then edits again
*/
        (new EditConsistencyCheckJob(libs, cells)).startJob();
    }

    public static class EditConsistencyCheckJob extends Job {
        private List<Library> libs;
        private List<Cell> cells;
        private List<Editor> editors;
        private String cvsProgram = CVS.getCVSProgram();
        private String repository = CVS.getRepository();

        public EditConsistencyCheckJob(List<Library> libs, List<Cell> cells) {
            super("CVS Editors Check", User.getUserTool(), Job.Type.CLIENT_EXAMINE, null, null, Job.Priority.USER);
            this.libs = libs;
            this.cells = cells;
            this.editors = new ArrayList<Editor>();
            if (this.libs == null) this.libs = new ArrayList<Library>();
            if (this.cells == null) this.cells = new ArrayList<Cell>();
        }
        public boolean doIt() {
            String useDir = CVS.getUseDir(libs, cells);
            StringBuffer libsBuf = CVS.getLibraryFiles(libs, useDir);
            StringBuffer cellsBuf = CVS.getCellFiles(cells, useDir);

            String args = libsBuf + " " + cellsBuf;
            if (args.trim().equals("")) return true;

            // get editors
            //System.out.println("Checking editors for: "+args);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CVS.runCVSCommand(cvsProgram, repository, "editors "+args, "Check CVS Editors", useDir, out);
            LineNumberReader reader = new LineNumberReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));
            editors = parseOutput(reader);

            // unmark if marked for edit and unmodified in both electric and cvs
            libs.clear();
            cells.clear();
            for (Editor e : editors) {
                if (!e.getUser().equals(System.getProperty("user.name"))) continue;
                ElectricObject eobj = e.findObject();
                if (eobj == null) continue;
                if (eobj instanceof Library) {
                    Library lib = (Library)eobj;
                    if (!lib.isChanged() && CVSLibrary.getState(lib) == State.NONE)
                        libs.add(lib);
                }
                if (eobj instanceof Cell) {
                    Cell cell = (Cell)eobj;
                    if (!cell.isModified() && CVSLibrary.getState(cell) == State.NONE)
                        cells.add(cell);
                }
            }
            libsBuf = CVS.getLibraryFiles(libs, useDir);
            cellsBuf = CVS.getCellFiles(cells, useDir);

            args = libsBuf + " " + cellsBuf;
            if (args.trim().equals("")) return true;
            //System.out.println("Unmarking CVS edit: "+args);
            CVS.runCVSCommand(cvsProgram, repository, "unedit -l "+args, "CVS Unedit", useDir, System.out);
            return true;
        }
    }


    // ----------------------- Edit Output Parsing --------------------------

    public static List<Editor> parseOutput(LineNumberReader result) {
        List<Editor> editors = new ArrayList<Editor>();
        for (;;) {
            String line;
            try {
                line = result.readLine();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                break;
            }
            if (line == null) break;
            if (line.equals("")) continue;
            Editor editor = Editor.parse(line);
            if (editor != null) editors.add(editor);
        }
        return editors;
    }

    /**
     * See if the specified Editor is referring to me on this host,
     * returns true if so, false otherwise.
     * @param editor
     * @return
     */
    static boolean isMe(Editor editor) {
        if (editor.getUser().equals(getUserName()) && editor.getHostname().equals(getHostName()))
            return true;
        return false;
    }

    public static class Editor implements Serializable {
        private final String file;
        private final String user;
        private final Date date;
        private final String hostname;
        private final String dir;
        private final File FD;
        private Editor(String file, String user, Date date, String hostname, String dir) {
            this.file = file;
            this.user = user;
            this.date = date;
            this.hostname = hostname;
            this.dir = dir;
            this.FD = new File(dir, file);
        }
        static Editor parse(String editorResultLine) {
            // parse editor command result
            if (editorResultLine.startsWith("?")) // running remotely lists unknown files
                return null;
            String parts[] = editorResultLine.split("\\t");
            if (parts.length == 5) {
                String abbreviatedFile = parts[0];
                String user = parts[1];
                DateFormat df = DateFormat.getDateInstance();
                Date date;
                try {
                    date = df.parse(parts[2]);
                } catch (java.text.ParseException e) {
                    date = new Date(0);
                }
                String computer = parts[3];
                String dir = parts[4];
                return new Editor(abbreviatedFile, user, date, computer, dir);
            } else {
                System.out.println("Bad Editor result line format: "+editorResultLine);
            }
            return null;
        }
        public String getAbbrevFile() { return file; }
        public String getUser() { return user; }
        public Date getDate() { return date; }
        public String getHostname() { return hostname; }
        public File getFile() { return FD; }
        public String getDir() { return dir; }
        public ElectricObject findObject() {
            String file = getAbbrevFile();
            Library lib = findLibraryWithExt(file);
            if (lib != null) return lib;
            // must be delib cell
            String [] parts = file.split("/");
            lib = findLibraryWithExt(parts[0]);
            if (lib == null) return null;
            int ext = parts[1].lastIndexOf('.');
            if (ext == -1) return lib.findNodeProto(parts[1]);
            String view = parts[1].substring(ext+1);
            String cellname = parts[1].substring(0, ext);
            cellname = cellname + "{" + view + "}";
            return lib.findNodeProto(cellname);
        }
        private static Library findLibraryWithExt(String libname) {
            if (libname.endsWith(".delib") || libname.endsWith(".jelib")) {
                return Library.findLibrary(libname.substring(0, libname.length()-6));
            }
            if (libname.endsWith(".elib")) {
                return Library.findLibrary(libname.substring(0, libname.length()-5));
            }
            return null;
        }
    }

    private static final String hostName;
    private static final String userName;
    static {
        String name = "unknownHost";
        try {
            InetAddress addr = InetAddress.getLocalHost();
            name = addr.getHostName();
        } catch (java.net.UnknownHostException e) {
        }
        hostName = name;
        userName = System.getProperty("user.name", "unknownUser");
    }
    public static final String getHostName() { return hostName; }
    public static final String getUserName() { return userName; }
}
