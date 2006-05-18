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

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;

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
        addremove(libs, null, true);
    }

    public static void add(Cell cell) {
        List<Cell> cells = new ArrayList<Cell>();
        cells.add(cell);
        addremove(null, cells, true);
    }

    public static void remove(Library lib) {
        List<Library> libs = new ArrayList<Library>();
        libs.add(lib);
        addremove(libs, null, false);
    }

    public static void remove(Cell cell) {
        List<Cell> cells = new ArrayList<Cell>();
        cells.add(cell);
        addremove(null, cells, false);
    }

    /**
     * Add or Remove libs and cells from CVS.
     * @param libs
     * @param cells
     * @param add true to add to cvs, false to remove from cvs
     */
    public static void addremove(List<Library> libs, List<Cell> cells, boolean add) {
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
        if (add) {
            bad = CVSLibrary.getInCVS(libs, cells);
            // all libs and cells must not be in cvs
            if (bad.libs.size() > 0 || bad.cells.size() > 0) {
                CVS.showError("Error: Some libraries or cells are already in CVS:",
                        "CVS Add Error", bad.libs, bad.cells);
                return;
            }
        } else {
            bad = CVSLibrary.getNotInCVS(libs, cells);
            // all libs and cells must be in cvs
            if (bad.libs.size() > 0 || bad.cells.size() > 0) {
                CVS.showError("Error: Some libraries or cells are not in CVS:",
                        "CVS Remove Error", bad.libs, bad.cells);
                return;
            }
        }
        // optimize a little, remove cells from cells list if cell's lib in libs list
        CVSLibrary.LibsCells good = CVSLibrary.consolidate(libs, cells);

        // issue final warning for Remove
        if (!add) {
            StringBuffer list = new StringBuffer("Warning! CVS Remove will delete disk files for these Libraries and Cells!!!");
            for (Library lib : good.libs) list.append("\n  "+lib.getName());
            for (Cell cell : good.cells) list.append("\n  "+cell.libDescribe());
            if (!Job.getUserInterface().confirmMessage(list.toString()))
                return;
        }

        (new AddRemoveJob(good.libs, good.cells, add)).startJob();
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
            String command = add ? "add" : "remove";
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
            String libfile = TextUtils.getFile(lib.getLibFile()).getPath();
            add(buf, libfile, useDir);
            if (CVS.isDELIB(lib)) {
                // see if cell directories are in CVS
                for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
                    generate(buf, it.next(), useDir);
                }
                // add header file
                File headerFile = new File(libfile, DELIB.getHeaderFile());
                add(buf, headerFile.getPath(), useDir);
                // add lastModified file
                File lastModifiedFile = new File(libfile, DELIB.getLastModifiedFile());
                if (lastModifiedFile.exists())
                    add(buf, lastModifiedFile.getPath(), useDir);
            }
        }
        private void generate(StringBuffer buf, Cell cell, String useDir) {
            if (!CVS.isDELIB(cell.getLibrary())) return;
            String libfile = TextUtils.getFile(cell.getLibrary().getLibFile()).getPath();
            // get cell directory if not already added before
            File celldirFile = new File(libfile, DELIB.getCellSubDir(cell.backup()));
            String celldir = celldirFile.getPath();
            if (!addedCellDirs.containsKey(celldir)) {
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
            if ((add && !CVS.isFileInCVS(FD)) ||
                (!add && CVS.isFileInCVS(FD))) {

                if (file.startsWith(useDir)) {
                    file = file.substring(useDir.length()+1, file.length());
                }
                buf.append(file+" ");
                if (!add) {
                    // need to delete local file first
                    if (!FD.isDirectory()) {
                        if (!FD.delete()) {
                            System.out.println("Error: could not delete file to removed: "+FD.getPath());
                        }
                    }
                }
            }
        }
        public void terminateOK() {
            if (exitVal != 0) {
                Job.getUserInterface().showErrorMessage("CVS "+(add?"Add":"Remove")+
                        " Failed!  Please see messages window (exit status "+exitVal+")","CVS "+(add?"Add":"Remove")+" Failed!");
            }
        }
    }
}
