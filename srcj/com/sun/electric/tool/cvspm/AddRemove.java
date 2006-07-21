/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AddRemove.java
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

import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.output.DELIB;
import com.sun.electric.tool.user.User;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.ElectricObject;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Mar 22, 2006
 * Time: 10:26:37 AM
 * To change this template use File | Settings | File Templates.
 */
public class AddRemove {

    public static void add(Library lib) {
        List<Library> libs = new ArrayList<Library>();
        libs.add(lib);
        addremove(libs, null, true, false);
    }

    public static void add(Cell cell) {
        List<Cell> cells = new ArrayList<Cell>();
        cells.add(cell);
        addremove(null, cells, true, false);
    }

    public static void remove(Library lib) {
        List<Library> libs = new ArrayList<Library>();
        libs.add(lib);
        addremove(libs, null, false, false);
    }

    public static void remove(Cell cell) {
        List<Cell> cells = new ArrayList<Cell>();
        cells.add(cell);
        addremove(null, cells, false, false);
    }

    /**
     * Add or Remove libs and cells from CVS.
     * @param libs
     * @param cells
     * @param add true to add to cvs, false to remove from cvs
     * @param undo true to undo add/remove (boolean add ignored in this case).
     */
    public static void addremove(List<Library> libs, List<Cell> cells, boolean add, boolean undo) {
        if (libs == null) libs = new ArrayList<Library>();
        if (cells == null) cells = new ArrayList<Cell>();

        // make sure cells are part of a DELIB
        CVSLibrary.LibsCells bad = CVSLibrary.notFromDELIB(cells);
        if (bad.cells.size() > 0) {
            CVS.showError("Error: the following Cells are not part of a DELIB library and cannot be acted upon individually",
                    "CVS Add/Remove Error", bad.libs, bad.cells);
            return;
        }
        bad = CVSLibrary.getModified(libs, cells);
        if (bad.libs.size() > 0 || bad.cells.size() > 0) {
            CVS.showError("Error: the following Libraries or Cells must be saved first",
                    "CVS "+(add?"Add":"Remove")+" Error", bad.libs, bad.cells);
            return;
        }

        // delib cells must have library added first
        List<Library> assertLibsInCVS = new ArrayList<Library>();
        for (Cell cell : cells) {
            Library lib = cell.getLibrary();
            if (libs.contains(lib) || assertLibsInCVS.contains(lib)) continue;
            assertLibsInCVS.add(lib);
        }
        bad = CVSLibrary.getNotInCVS(assertLibsInCVS, null);
        if (bad.libs.size() > 0) {
            CVS.showError("Error: cannot add DELIB cells if cell's DELIB library is not in cvs",
                    "CVS "+(add?"Add":"Remove")+" Error", bad.libs, bad.cells);
            return;
        }


        // optimize a little, remove cells from cells list if cell's lib in libs list
        CVSLibrary.LibsCells good = CVSLibrary.consolidate(libs, cells);

        if (!undo) {
            // when job generates files to add/remove, it will do the check to see if they are in
            // cvs or not. Don't do it here because we may specify lib to add unadded cells.
/*
            if (add) {
                good = CVSLibrary.getNotInCVS(libs, cells);
            } else {
                good = CVSLibrary.getInCVS(libs, cells);
            }
*/
            // issue final warning for Remove
            if (!add) {
                StringBuffer list = new StringBuffer("Warning! CVS Remove will delete disk files for these Libraries and Cells!!!");
                for (Library lib : good.libs) list.append("\n  "+lib.getName());
                for (Cell cell : good.cells) list.append("\n  "+cell.libDescribe());
                if (!Job.getUserInterface().confirmMessage(list.toString()))
                    return;
            }

            (new AddRemoveJob(good.libs, good.cells, add)).startJob();
        } else {
            (new UndoAddRemoveJob(good.libs, good.cells)).startJob();
        }
    }

    public static class AddRemoveJob extends Job {
        private List<Library> libs;
        private List<Cell> cells;
        private boolean add;
        private int exitVal;
        private HashMap<String,Integer> addedCellDirs;
        private AddRemoveJob(List<Library> libs, List<Cell> cells, boolean add) {
            super("CVS Add/Remove", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
            this.libs = libs;
            this.cells = cells;
            this.add = add;
            exitVal = -1;
            if (this.libs == null) this.libs = new ArrayList<Library>();
            if (this.cells == null) this.cells = new ArrayList<Cell>();
            addedCellDirs = new HashMap<String,Integer>();
        }
        public boolean doIt() {
            String useDir = CVS.getUseDir(libs, cells);

            List<Library> stateChangeLibs = new ArrayList<Library>();
            List<Cell> stateChangeCells = new ArrayList<Cell>();
            // mark files as added/removed
            for (Library lib : libs) {
                if (CVS.isDELIB(lib)) {
                    for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
                        Cell cell = it.next();
                        if (add) {
                            if (!CVS.isFileInCVS(CVS.getCellFile(cell)))
                                stateChangeCells.add(cell);
                        } else {
                            if (CVS.isFileInCVS(CVS.getCellFile(cell)))
                                stateChangeCells.add(cell);
                        }
                    }
                } else {
                    // jelib or elib file
                    if (add) {
                        if (!CVS.isFileInCVS(new File(lib.getLibFile().getPath())))
                            stateChangeLibs.add(lib);
                    } else {
                        if (!CVS.isFileInCVS(new File(lib.getLibFile().getPath())))
                            stateChangeLibs.add(lib);
                    }
                }
            }
            for (Cell cell : cells) {
                if (add) {
                    if (!CVS.isFileInCVS(CVS.getCellFile(cell)))
                        stateChangeCells.add(cell);
                } else {
                    if (CVS.isFileInCVS(CVS.getCellFile(cell)))
                        stateChangeCells.add(cell);
                }
            }

            // unfortunately add/remove are not recursive, so we have
            // to specify directories as well as files to add/remove
            StringBuffer buf = new StringBuffer();
            for (Library lib : libs) {
                generate(buf, lib, useDir);
            }
            for (Cell cell : cells) {
                generate(buf, cell, useDir);
            }

            String addRemoveFiles = buf.toString();
            if (addRemoveFiles.trim().equals("")) {
                System.out.println("Nothing to "+(add ? "Add" : "Remove"));
                exitVal = 0;
                return true;
            }
            String command = add ? "add" : "remove -f";
            String message = "Running CVS " + (add ? "Add" : "Remove");
            exitVal = CVS.runCVSCommand("-q "+command+" "+addRemoveFiles, message,
                    useDir, System.out);
            fieldVariableChanged("exitVal");
            if (exitVal != 0) return true;

            System.out.println("CVS "+command+" complete");
            for (Cell cell : stateChangeCells) {
                if (add) CVSLibrary.setState(cell, State.ADDED);
                else CVSLibrary.setState(cell, State.REMOVED);
            }
            for (Library lib : stateChangeLibs) {
                if (add) CVSLibrary.setState(lib, State.ADDED);
                else CVSLibrary.setState(lib, State.REMOVED);
            }

            return true;
        }
        private void generate(StringBuffer buf, Library lib, String useDir) {
            // see if library file is in CVS
            File libraryFile = TextUtils.getFile(lib.getLibFile());
            if (libraryFile == null) return;
            String libfile = libraryFile.getPath();

            add(buf, libfile, useDir);
            if (CVS.isDELIB(lib)) {
                // see if cell directories are in CVS
                for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
                    generate(buf, it.next(), useDir);
                }
                // add header file
                File headerFile = new File(libfile, DELIB.getHeaderFile());
                add(buf, headerFile.getPath(), useDir);
            }
        }
        private void generate(StringBuffer buf, Cell cell, String useDir) {
            if (!CVS.isDELIB(cell.getLibrary())) return;
            File libraryFile = TextUtils.getFile(cell.getLibrary().getLibFile());
            if (libraryFile == null) return;
            String libfile = libraryFile.getPath();
            // get cell directory if not already added before
            File celldirFile = new File(libfile, DELIB.getCellSubDir(cell.backup()));
            String celldir = celldirFile.getPath();
            if (!addedCellDirs.containsKey(celldir) && !libfile.equals(celldir)) {
                if (celldirFile.exists())
                    add(buf, celldir, useDir);
                addedCellDirs.put(celldir, null);
            }
            // check cell files
            File cellFile = new File(libfile, DELIB.getCellFile(cell));
            add(buf, cellFile.getPath(), useDir);
            // check that header is added or in cvs, or library is going to be added
            if (add) {
                File headerFile = new File(libfile, DELIB.getHeaderFile());
                if (!libs.contains(cell.getLibrary()) && !CVS.isFileInCVS(headerFile)) {
                    add(buf, headerFile.getPath(), useDir);
                }
            }
        }
        private void add(StringBuffer buf, String file, String useDir) {
            File FD = new File(file);
            if ((add && !CVS.isFileInCVS(FD, false, false)) ||
                (!add && CVS.isFileInCVS(FD, false, false))) {

                if (file.startsWith(useDir)) {
                    file = file.substring(useDir.length()+1, file.length());
                }
                buf.append(file+" ");
            }
        }
        public void terminateOK() {
            if (exitVal != 0) {
                Job.getUserInterface().showErrorMessage("CVS "+(add?"Add":"Remove")+
                        " Failed!  Please see messages window (exit status "+exitVal+")","CVS "+(add?"Add":"Remove")+" Failed!");
            }
        }
    }

    public static class UndoAddRemoveJob extends Job {
        private List<Library> libs;
        private List<Cell> cells;
        HashMap<File,ElectricObject> filesToUndo;
        private UndoAddRemoveJob(List<Library> libs, List<Cell> cells) {
            super("CVS Undo Add/Remove", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
            this.libs = libs;
            this.cells = cells;
            if (this.libs == null) this.libs = new ArrayList<Library>();
            if (this.cells == null) this.cells = new ArrayList<Cell>();
        }
        public boolean doIt() {
            StringBuffer buf = new StringBuffer();
            for (Library lib : libs) {
                undo(lib);
            }
            for (Cell cell : cells) {
                undo(cell);
            }
            System.out.println("Undo CVS Add/Remove complete");
            return true;
        }
        private void undo(Library lib) {
            // see if library file is in CVS
            File libraryFile = TextUtils.getFile(lib.getLibFile());
            if (libraryFile == null) return;
            String libfile = libraryFile.getPath();
            if (!CVS.isDELIB(lib)) {
                if (undo(new File(libfile))) {
                    State state = CVSLibrary.getState(lib);
                    if (state == State.ADDED)
                        CVSLibrary.setState(lib, State.UNKNOWN);
                    if (state == State.REMOVED)
                        CVSLibrary.setState(lib, State.NONE);
                }
            } else {
                for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
                    undo(it.next());
                }
                // undo header file
                File headerFile = new File(libfile, DELIB.getHeaderFile());
                undo(headerFile);
            }
        }
        private void undo(Cell cell) {
            if (!CVS.isDELIB(cell.getLibrary())) return;
            File libraryFile = TextUtils.getFile(cell.getLibrary().getLibFile());
            if (libraryFile == null) return;
            String libfile = libraryFile.getPath();
            // get cell directory if not already added before
            // check cell files
            File cellFile = new File(libfile, DELIB.getCellFile(cell));
            if (undo(cellFile)) {
                State state = CVSLibrary.getState(cell);
                if (state == State.ADDED)
                    CVSLibrary.setState(cell, State.UNKNOWN);
                if (state == State.REMOVED)
                    CVSLibrary.setState(cell, State.NONE);
            }
        }
        /**
         * Return true if succeeded, false otherwise
         * @param FD
         * @return
         */
        private boolean undo(File FD) {
            if (FD.isDirectory()) return false;
            File parent = FD.getParentFile();
            File CVSDIR = new File(parent, "CVS");
            if (!CVSDIR.exists()) return false;
            File entries = new File(CVSDIR, "Entries");
            if (!entries.exists()) return false;
            File entriestemp = new File(CVSDIR, "Entries.temp");
            String filename = FD.getName();
            boolean success = false;
            try {
                LineNumberReader reader = new LineNumberReader(new FileReader(entries));
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(entriestemp, false)));
                for (;;) {
                    String line = reader.readLine();
                    if (line == null) break;
                    if (line.equals("")) continue;
                    String parts[] = line.split("/");
                    boolean skip = false;
                    if (parts.length >= 2 && parts[1].equals(filename)) {
                        // make sure it is scheduled for add/remove
                        if (parts.length >= 3 && parts[2].equals("0")) {
                            // scheduled for add, remove entry
                            skip = true;
                            success = true;
                        }
                        if (parts.length >= 3 && parts[2].startsWith("-")) {
                            // schedule to remove, remove entry
                            line = line.replaceAll("/"+parts[2]+"/", "/"+parts[2].substring(1)+"/");
                            success = true;
                        }
                    }
                    if (!skip) pw.println(line);
                }
                reader.close();
                pw.close();
                // replace original with modified
                if (!entriestemp.renameTo(entries)) {
                    System.out.println("Unable to move "+entriestemp+" to "+entries+", cannot undo add/remove of "+filename);
                    return false;
                }
            } catch (IOException e) {
            }
            return success;
        }
    }
}
