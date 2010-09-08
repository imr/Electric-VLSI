/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CVSLibrary.java
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

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.id.LibId;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.util.TextUtils;

import java.util.*;
import java.io.File;
import java.awt.Color;
import java.net.URL;

/**
 * Track the state of a library that has been checked into CVS.
 * User: gainsley
 * Date: Mar 16, 2006
 */
public class CVSLibrary {

//    private Library lib;
    private FileType type;
    private State libState;                 // only used for non-DELIB file types
    private Map<Cell,State> cellStates;
    private Map<Cell,Cell> editing;         // list of cells I am editing
    private boolean libEditing;                 // true if library is being edited (only for JELIB, ELIB)

    private static Map<LibId,CVSLibrary> CVSLibraries = new HashMap<LibId,CVSLibrary>();

    private CVSLibrary(Library lib) {
//        this.lib = lib;
        String libFile = lib.getLibFile().getPath();
        type = OpenFile.getOpenFileType(libFile, FileType.JELIB);
        libState = State.NONE;
        libEditing = false;

        cellStates = new HashMap<Cell,State>();
        for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
            Cell cell = it.next();
            cellStates.put(cell, State.NONE);
            if (CVS.isDELIB(lib)) {
                if (!CVS.isFileInCVS(CVS.getCellFile(cell))) cellStates.put(cell, State.UNKNOWN);
            }

        }
        editing = new HashMap<Cell,Cell>();
    }
    /**
     * Add a library to the list of CVS libraries, that will maintain
     * the known state of the library with respect to CVS.  The known
     * state specifies the color mapping of libraries and cells in
     * the explorer tree.
     * @param lib
     */
    public static void addLibrary(Library lib) {
        addLibrary(lib, false);
    }
    private static void addLibrary(Library lib, boolean added) {
        if (lib.isHidden()) return;
        if (!lib.isFromDisk()) return;
        String libFile = lib.getLibFile().getPath();
        if (!added && !CVS.isFileInCVS(new File(libFile))) {
            return;
        }
        CVSLibrary cvslib = new CVSLibrary(lib);
        CVSLibraries.put(lib.getId(),cvslib);
    }

    /**
     * Remove a library from the list of CVS libraries.
     * @param libId
     */
    public static void removeLibrary(LibId libId) {
        CVSLibraries.remove(libId);
    }

    protected static class LibsCells {
        public List<Library> libs = new ArrayList<Library>();
        public List<Cell> cells = new ArrayList<Cell>();
    }

    /**
     * Check the specified libraries and cells are in cvs.
     * Return any libs and cells that are not in cvs
     * @param libs a List of Libraries to check.
     * @param cells a List of Cells to check.
     * @return Libraries and Cells that are not in CVS.
     */
    public static LibsCells getNotInCVS(List<Library> libs, List<Cell> cells) {
        if (libs == null) libs = new ArrayList<Library>();
        if (cells == null) cells = new ArrayList<Cell>();

        LibsCells bad = new LibsCells();
        for (Library lib : libs) {
            if (!CVS.isInCVS(lib)) bad.libs.add(lib);
        }
        for (Cell cell : cells) {
            if (!CVS.isInCVS(cell)) bad.cells.add(cell);
        }
        return bad;
    }

    public static LibsCells getInCVS(List<Library> libs, List<Cell> cells) {
        if (libs == null) libs = new ArrayList<Library>();
        if (cells == null) cells = new ArrayList<Cell>();

        LibsCells bad = new LibsCells();
        for (Library lib : libs) {
            if (CVS.isInCVS(lib)) bad.libs.add(lib);
        }
        for (Cell cell : cells) {
            if (CVS.isInCVS(cell)) bad.cells.add(cell);
        }
        return bad;
    }

    public static LibsCells getModified(List<Library> libs, List<Cell> cells) {
        if (libs == null) libs = new ArrayList<Library>();
        if (cells == null) cells = new ArrayList<Cell>();

        LibsCells bad = new LibsCells();
        for (Library lib : libs) {
            if (lib.isChanged()) bad.libs.add(lib);
        }
        for (Cell cell : cells) {
            if (cell.isModified()) bad.cells.add(cell);
        }
        return bad;
    }

    /**
     * Remove cells from cell list if they are part of any library in libs list.
     * Return the consolidated list of libs and cells.
     * @param libs the List of Libraries to consolidate.
     * @param cells the List of Cells to consolidate.
     * @return a LibsCells with the all desired Libraries and Cells.
     */
    public static LibsCells consolidate(List<Library> libs, List<Cell> cells) {
        LibsCells consolidated = new LibsCells();
        consolidated.libs.addAll(libs);
        for (Cell cell : cells) {
            if (!libs.contains(cell.getLibrary())) consolidated.cells.add(cell);
        }
        return consolidated;
    }

    /**
     * Get cells from passed in list of cells that are not from DELIB libraries.
     * @param cells a List of Cells to examine.
     * @return LibsCells of cells that are not from DELIB libraries.
     */
    public static LibsCells notFromDELIB(List<Cell> cells) {
        if (cells == null) cells = new ArrayList<Cell>();
        LibsCells bad = new LibsCells();
        for (Cell cell : cells) {
            if (!CVS.isDELIB(cell.getLibrary())) bad.cells.add(cell);
        }
        return bad;
    }

    /**
     * Get a list of libraries and cells that are in CVS.  This is sorted
     * in the sense that the libraries returned will be JELIBS, and the
     * cells returned are cells from DELIBS that are part of the original
     * set of libs and cells.
     * @param libs a list of Libraries
     * @param cells a list of Cells
     * @return a LibsCells with JELIBS libraries and DELIB cells that are in CVS.
     */
    public static LibsCells getInCVSSorted(List<Library> libs, List<Cell> cells) {
        List<Cell> delibCells = new ArrayList<Cell>();
        List<Library> jelibs = new ArrayList<Library>();

        for (Library lib : libs) {
            if (CVS.isDELIB(lib)) {
                for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
                    cells.add(it.next());
                }
            }
            else if (!jelibs.contains(lib))
                jelibs.add(lib);
        }
        for (Cell c : cells) {
            if (CVS.isDELIB(c.getLibrary()))
                delibCells.add(c);
            else if (!jelibs.contains(c.getLibrary()))
                jelibs.add(c.getLibrary());
        }
        return getInCVS(jelibs, delibCells);
    }

    /**
     * Only for DELIBs, check if there are any cells in
     * library that have state "UNKNOWN", and need to be added to CVS.
     * @param lib the Library to examine.
     * @return true if such cells exist.
     */
    public static boolean hasUnknownCells(Library lib) {
        CVSLibrary cvslib = CVSLibraries.get(lib.getId());
        if (cvslib == null) return false;
        if (cvslib.type != FileType.DELIB) return false;
        for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
            Cell cell = it.next();
            State state = cvslib.cellStates.get(cell);
            if (state == null) return true;
            if (state == State.UNKNOWN) return true;
        }
        return false;
    }

    // --------------------- State recording ---------------------

    /**
     * Set state of a Cell
     * @param cell
     * @param state
     */
    public static void setState(Cell cell, State state) {
        CVSLibrary cvslib = CVSLibraries.get(cell.getLibrary().getId());
        if (cvslib == null && state == State.ADDED) {
            // if state is added, CVSLibrary should be created
            addLibrary(cell.getLibrary(), true);
            cvslib = CVSLibraries.get(cell.getLibrary().getId());
        }
        if (cvslib == null) return;
        if (state == null)
            return;
        if (!CVS.isDELIB(cell.getLibrary())) {
            cvslib.libState = state;
        } else {
            cvslib.cellStates.put(cell, state);
        }
    }

    /**
     * Set state of all Cells in a library
     * @param lib
     * @param state
     */
    public static void setState(Library lib, State state) {
        CVSLibrary cvslib = CVSLibraries.get(lib.getId());
        if (cvslib == null && state == State.ADDED) {
            // if state is added, CVSLibrary should be created
            addLibrary(lib, true);
            cvslib = CVSLibraries.get(lib.getId());
        }
        if (cvslib == null) return;
        if (state == null)
            return;
        if (cvslib.type != FileType.DELIB) {
            cvslib.libState = state;
            return;
        }
        for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
            Cell cell = it.next();
            State currentState = cvslib.cellStates.get(cell);
            // When cell is not in CVS and doing commit of library,
            // do not set state of unknown cells to NONE
            if (currentState == State.UNKNOWN) continue;
            cvslib.cellStates.put(cell, state);
        }
    }

    // ------------------ Color Mapping for Explorer Tree -----------------

    public static Color getColor(Library lib) {
        return getColor(getState(lib));
    }

    public static Color getColor(Cell cell) {
        return getColor(getState(cell));
    }

    public static State getState(Library lib) {
        CVSLibrary cvslib = CVSLibraries.get(lib.getId());
        if (cvslib == null) return State.UNKNOWN;
        if (cvslib.type != FileType.DELIB) {
            return cvslib.libState;
        }
        Set<State> states = new TreeSet<State>();
        for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
            Cell cell = it.next();
            State state = cvslib.cellStates.get(cell);
            if (state == null) continue;
            states.add(state);
        }
        Iterator<State> it = states.iterator();
        if (it.hasNext()) return it.next();
        return State.UNKNOWN;
    }

    public static State getState(Cell cell) {
        CVSLibrary cvslib = CVSLibraries.get(cell.getLibrary().getId());
        if (cvslib == null) return State.UNKNOWN;
        if (!CVS.isDELIB(cell.getLibrary())) {
            // return state for library
            return cvslib.libState;
        }
        State state = cvslib.cellStates.get(cell);
        if (state == null) return State.UNKNOWN;
        return state;
    }

    public static Color getColor(Cell.CellGroup cg) {
        Set<State> states = new TreeSet<State>();
        for (Iterator<Cell> it = cg.getCells(); it.hasNext(); ) {
            Cell cell = it.next();
            State state = getState(cell);
            states.add(state);
        }
        return getColor(states);
    }

    public static Color getColor(State state) {
        if (state == State.NONE) return Color.black;
        if (state == State.UPDATE) return Color.magenta;
        if (state == State.MODIFIED) return Color.blue;
        if (state == State.CONFLICT) return Color.red;
        if (state == State.ADDED) return Color.green;
        if (state == State.REMOVED) return Color.green;
        if (state == State.PATCHED) return Color.black;
        if (state == State.UNKNOWN) return Color.lightGray;
        return Color.black;
    }

    public static Color getColor(Set<State> states) {
        Iterator<State> it = states.iterator();
        if (it.hasNext()) return getColor(it.next());
        return Color.black;
    }

    // -------------------- Editing tracking ---------------------

    public static void setEditing(Cell cell, boolean editing) {
        CVSLibrary cvslib = CVSLibraries.get(cell.getLibrary().getId());
        if (cvslib == null) return;
        if (editing) {
            cvslib.editing.put(cell, cell);
        } else {
            cvslib.editing.remove(cell);
        }
    }

    public static boolean isEditing(Cell cell) {
        CVSLibrary cvslib = CVSLibraries.get(cell.getLibrary().getId());
        if (cvslib == null) return false;
        return cvslib.editing.containsKey(cell);
    }

    public static void setEditing(Library lib, boolean editing) {
        CVSLibrary cvslib = CVSLibraries.get(lib.getId());
        if (cvslib == null) return;
        if (!CVS.isDELIB(lib))
            cvslib.libEditing = editing;
        else {
            if (editing = false)
                cvslib.editing.clear();
        }
    }

    public static boolean isEditing(Library lib) {
        CVSLibrary cvslib = CVSLibraries.get(lib.getId());
        if (cvslib == null) return false;
        if (!CVS.isDELIB(lib))
            return cvslib.libEditing;
        else {
            return !cvslib.editing.isEmpty();
        }
    }

    /**
     * Method called when saving a library, BEFORE the library
     * file(s) are written.
     * @param libId
     * @param lib
     * @param libFile
     */
    private static void savingLibrary(LibId libId, Library lib, URL libFile) {
        // When doing "save as", library type may change. Update library type here
        if (libFile != null) {
            FileType type = OpenFile.getOpenFileType(libFile.getFile(), FileType.JELIB);
            CVSLibrary cvslib = CVSLibraries.get(libId);
            if (cvslib != null) {
                if (cvslib.type != type) {
                    // remove and re-add
                    removeLibrary(libId);
                    if (lib != null)
                        addLibrary(lib);
                }
            }
        }
    }

    /**
     * Hook for after a DELIB library was saved. This will do a CVS remove
     * on any cells files that have been deleted (renamed).
     * Note that this method is currently called only after a DELIB has been
     * written, not after any other type of library.
     * @param libId
     * @param libFile
     * @param deletedCellFiles
     */
    public static void savedLibrary(LibId libId, URL libFile, List<String> deletedCellFiles, List<String> writtenCellFiles) {
        Library lib = EDatabase.clientDatabase().getLib(libId);
        savingLibrary(libId, lib, libFile);
        String useDir = TextUtils.getFile(libFile).getParent();
//        List<Library> libs = new ArrayList<Library>();
//        libs.add(lib);
//        String useDir = CVS.getUseDir(libs, null);
        String cvsProgram = CVS.getCVSProgram();
        String repository = CVS.getRepository();
        if (CVS.isInCVS(libFile) && CVS.isDELIB(libFile)) {
            StringBuffer buf = new StringBuffer();
            for (String s : deletedCellFiles) {
                File file = new File(s);
                if (CVS.isFileInCVS(file) && !file.exists()) {
                    // original file should have been renamed to .deleted,
                    // issue remove on deleted file
                    if (s.startsWith(useDir))
                        buf.append(s.substring(useDir.length()+1)+" ");
                    else
                        buf.append(s+" ");
                }
            }
            String arg = buf.toString();
            if (!arg.trim().equals("")) {
                //System.out.println("Removing deleted cells from CVS");
                int exitVal = CVS.runCVSCommand(cvsProgram, repository, "-q remove "+arg, "Removing deleted cells from CVS",
                        useDir, System.out);
                if (exitVal != 0) {
                    System.out.println("  Error running CVS remove command (exit status "+exitVal+")");
                    return;
                }
                // run the commit, because if it is left in "remove" state, a new cell of the
                // same name cannot be added and committed.
                exitVal = CVS.runCVSCommandWithQuotes(cvsProgram, repository, "-q commit -m \"Automatic commit of removed cell file by Electric\" "+arg,
                        "Committing removed files to CVS", useDir, System.out);
                // since the file has been deleted and marked for removal, future updates
                // will not recreate the file.  However, a final commit is required to fully remove it,
                // but this is not really necessary.
            }

            // add any new cell files to cvs, if library is in cvs
            buf = new StringBuffer();
            for (String s : writtenCellFiles) {
                File file = new File(s);
                File parent = file.getParentFile();
                if (!CVS.isFileInCVS(parent) && parent.exists()) {
                    // for old style delib with cell file subdirs
                    if (parent.getAbsolutePath().startsWith(useDir))
                        buf.append(parent.getAbsolutePath().substring(useDir.length()+1)+" ");
                    else
                        buf.append(parent.getAbsolutePath()+" ");
                }

                if (!CVS.isFileInCVS(file) && file.exists()) {
                    if (s.startsWith(useDir))
                        buf.append(s.substring(useDir.length()+1)+" ");
                    else
                        buf.append(s+" ");
                }
            }
            arg = buf.toString();
            if (!arg.trim().equals("")) {
                int exitVal = CVS.runCVSCommand(cvsProgram, repository, "-q add "+buf.toString(), "Adding new cells to CVS",
                        useDir, System.out);
                if (exitVal != 0) {
                    System.out.println("  Error running CVS add command (exit status "+exitVal+")");
                    return;
                }
            }
        }

        // run update on the library to see if there are now any conflicts, and
        // recolor added cells
        if (lib != null && CVS.isInCVS(libFile)) {
            List<Library> libs = new ArrayList<Library>();
            libs.add(lib);
            Update.statusNoJob(libs, null, false);
        }
    }

//    /**
//     * Command to run after saving library for non-delib type libraries
//     * @param lib the library
//     * @param oldLibFile
//     */
//    public static void savedLibrary(Library lib, URL oldLibFile) {
//        savingLibrary(lib, oldLibFile);
//        // run update on the library to see if there are now any conflicts, and
//        // recolor added cells
//        List<Library> libs = new ArrayList<Library>();
//        libs.add(lib);
//        if (CVS.isInCVS(lib)) {
//            Update.statusNoJob(libs, null, false);
//        }
//    }

    /**
     * Method called when closing library.  Should be called
     * after library is closed.
     * @param lib
     */
    public static void closeLibrary(Library lib) {
        removeLibrary(lib.getId());
    }

}
