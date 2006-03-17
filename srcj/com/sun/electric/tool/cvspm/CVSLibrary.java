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
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.dialogs.OpenFile;

import java.util.*;
import java.io.File;
import java.awt.Color;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Mar 16, 2006
 * Time: 9:52:50 AM
 * Track the state of a library that has been checked into CVS.
 */
public class CVSLibrary {

    private Library lib;
    private FileType type;
    private State libState;
    private Map<Cell,State> cellStates;
    private Map<Cell,Cell> editing;     // cells user has edit lock for

    private static Map<Library,CVSLibrary> CVSLibraries = new HashMap<Library,CVSLibrary>();

    /**
     * Add a library to the list of CVS libraries, that will maintain
     * the known state of the library with respect to CVS.  The known
     * state specifies the color mapping of libraries and cells in
     * the explorer tree.
     * @param lib
     */
    public static void addLibrary(Library lib) {
        if (lib.isHidden()) return;
        if (!lib.isFromDisk()) return;
        String libFile = lib.getLibFile().getPath();
        if (!CVS.isFileInCVS(new File(libFile))) {
            return;
        }
        CVSLibrary cvslib = new CVSLibrary(lib);
        CVSLibraries.put(lib,cvslib);
    }

    /**
     * Remove a library from the list of CVS libraries.
     * @param lib
     */
    public static void removeLibrary(Library lib) {
        CVSLibraries.remove(lib);
    }

    /**
     * Update maintained state of libraries by checking CVS
     */
    public static void updateStateAllLibraries() {
        for (Iterator<Library> it = Library.getLibraries(); it.hasNext(); ) {
            Library lib = it.next();
            updateState(lib);
        }
    }

    /**
     * Update maintained state of library by checking CVS
     * @param lib
     */
    public static void updateState(Library lib) {
        CVSLibrary cvslib = CVSLibraries.get(lib);
        if (cvslib == null) return;
        cvslib.updateState();
    }

    private void updateState() {
        Update.StatusResult result = Update.status(lib);
        Map<String,State> fileStates = getFileStates(result);
        if (type != FileType.DELIB) {
            // look for library state
            State state = fileStates.get(lib.getName()+type.getExtensions()[0]);
            if (state == null) state = State.NONE;
            libState = state;
        } else {
            // get states for all Cells
            Set<State> states = new TreeSet<State>();
            for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
                Cell cell = it.next();
                State state = fileStates.get(cell.getName()+"."+cell.getView().getAbbreviation());
                if (state == null) state = State.NONE;
                cellStates.put(cell, state);
                states.add(state);
            }
            libState = State.NONE;
            if (states.size() > 0) libState = states.iterator().next();
        }
    }

    private static Map<String,State> getFileStates(Update.StatusResult result) {
        Map<String,State> states = new HashMap<String,State>();
        for (String s : result.getUpdated()) {
            states.put(getFileFromPath(s), State.UPDATE);
        }
        for (String s : result.getAdded()) {
            states.put(getFileFromPath(s), State.ADDED);
        }
        for (String s : result.getRemoved()) {
            states.put(getFileFromPath(s), State.REMOVED);
        }
        for (String s : result.getConflicts()) {
            states.put(getFileFromPath(s), State.CONFLICT);
        }
        for (String s : result.getModified()) {
            states.put(getFileFromPath(s), State.MODIFIED);
        }
        for (String s : result.getUnknown()) {
            states.put(getFileFromPath(s), State.UNKNOWN);
        }
        return states;
    }

    private static String getFileFromPath(String path) {
        int i = path.lastIndexOf(File.separator);
        if (i == path.length()-1) {
            // last char, path is a directory, chop it off
            path = path.substring(0, path.length()-1);
            i = path.lastIndexOf(File.separator);
        }
        return path.substring(i+1, path.length());
    }


    private CVSLibrary(Library lib) {
        this.lib = lib;
        String libFile = lib.getLibFile().getPath();
        type = OpenFile.getOpenFileType(libFile, FileType.JELIB);
        libState = State.NONE;

        cellStates = null;
        editing = new HashMap<Cell,Cell>();
        if (type == FileType.DELIB) {
            cellStates = new HashMap<Cell,State>(); // only DELIBs have separate files for each cell
            for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
                Cell cell = it.next();
                cellStates.put(cell, State.NONE);
            }
        }
    }

    // ------------------ Color Mapping for Explorer Tree -----------------

    public static Color getColor(Library lib) {
        return getColor(getState(lib));
    }

    public static Color getColor(Cell cell) {
        return getColor(getState(cell));
    }

    static State getState(Library lib) {
        CVSLibrary cvslib = CVSLibraries.get(lib);
        if (cvslib == null) return State.NONE;
        return cvslib.libState;
    }

    static State getState(Cell cell) {
        CVSLibrary cvslib = CVSLibraries.get(cell.getLibrary());
        if (cvslib == null) return State.NONE;
        State state = cvslib.cellStates.get(cell);
        if (state == null) return State.NONE;
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
        if (state == State.UPDATE) return Color.black;
        if (state == State.MODIFIED) return Color.blue;
        if (state == State.CONFLICT) return Color.red;
        if (state == State.ADDED) return Color.orange;
        if (state == State.REMOVED) return Color.orange;
        if (state == State.PATCHED) return Color.black;
        if (state == State.UNKNOWN) return Color.lightGray;
        return Color.black;
    }

    public static Color getColor(Set<State> states) {
        Iterator<State> it = states.iterator();
        if (it.hasNext()) return getColor(it.next());
        return Color.black;
    }

    // ------------------------- Edit Lock ---------------------------

    /**
     * Lock or Unlock edit lock for the cell
     * @param cell
     * @param setLocked
     */
    static void setEditing(Cell cell, boolean setLocked) {
        CVSLibrary cvslib = CVSLibraries.get(cell.getLibrary());
        if (cvslib == null) return;
        if (setLocked) {
            cvslib.editing.put(cell, cell);
        } else {
            cvslib.editing.remove(cell);
        }
    }

    /**
     * Lock or Unlock edit lock for the entire library
     */
    static void setEditing(Library lib, boolean setLocked) {
        CVSLibrary cvslib = CVSLibraries.get(lib);
        if (cvslib == null) return;
        for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
            Cell cell = it.next();
            if (setLocked) {
                cvslib.editing.put(cell, cell);
            } else {
                cvslib.editing.remove(cell);
            }
        }
    }
}
