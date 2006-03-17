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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.tool.Job;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Date;
import java.text.DateFormat;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Mar 15, 2006
 * Time: 4:31:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class Edit {

    // ------------------------ Edit -----------------------------

    /** Establish a lock on an entire library and all it's cells.
     * Returns true if the Library was locked for your edit,
     * false if it cannot be because someone else has locked it
     * for edit.
     * If 'showDialog' is true, an error dialog pops if the edit
     * was unsuccessful, otherwise the error message is printed to System.out.
     * @param lib
     * @param showDialog
     * @return
     */
    public static boolean edit(Library lib, boolean showDialog) {
        File file = new File(lib.getLibFile().getPath());
        if (!CVS.isFileInCVS(file)) return true;

        String dir = file.getParent();
        String delib = file.getName();

        boolean success = edit(delib, dir, showDialog);
        if (success) {
            CVSLibrary.setEditing(lib, true);
        }
        return success;
    }

    /**
     * Establish a lock on the Cell that only you can edit it.
     * Returns true if the Cell was locked for your edit,
     * false if it cannot be because someone else has locked it for edit.
     * If 'showDialog' is true, an error dialog pops if the edit
     * was unsuccessful, otherwise the error message is printed to System.out.
     * @param cell
     * @return
     */
    public static boolean edit(Cell cell, boolean showDialog) {
        File file = CVS.getCellFile(cell);
        if (!CVS.isDELIB(cell.getLibrary())) {
            // cannot lock single cell, attempt to lock entire library
            System.out.println("Attempting to Edit entire library for Cell "+cell.describe(false));
            return edit(cell.getLibrary(), showDialog);
        }
        if (!CVS.isFileInCVS(CVS.getCellFile(cell))) return false;

        String dir = file.getParent();
        String c = file.getName();

        boolean success = edit(c, dir, showDialog);
        if (success) {
            CVSLibrary.setEditing(cell, true);
            
        }
        return success;
    }

    /**
     * Establish a lock the file in dir for your exclusive edit.
     * If anyone else is editing the file, this returns false, and
     * if 'showDialog' is true, pops up an error dialog, otherwise it
     * just prints to System.out.
     * @return true if the edit lock is now yours, false otherwise.
     */
    public static boolean edit(String file, String dir, boolean showDialog) {

        // check if anyone else is editing the file
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CVS.runModalCVSCommand("editors "+file, "Checking for editors of "+file,
                dir, out);
        LineNumberReader result = new LineNumberReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));
        List<Editor> editors = parseOutput(result);
        if (editors.size() != 0) {
            // someone editing, show who
            List<String> message = new ArrayList<String>();
            message.add("Error: Cannot edit "+file+" because it is already being edited by:");
            for (Editor editor : editors) {
                message.add(editor.getUser()+"@"+editor.getComputer()+" at "+editor.getDate());
            }
            if (showDialog) {
                Job.getUserInterface().showErrorMessage(message.toArray(), "Edit Unavailable");
            } else {
                for (String s : message) System.out.println(s);
            }
            return false;
        }
        // no one editing, set edit lock
        CVS.runModalCVSCommand("edit -a none "+file, "Edit "+file,
                dir, System.out);
        return true;
    }


    // ------------------------ Unedit -----------------------------

    public static void uneditAllLibraries() {
        for (Iterator<Library> it = Library.getLibraries(); it.hasNext(); ) {
            Library lib = it.next();
            unedit(lib);
        }
    }

    public static void unedit(Library lib) {
        File file = new File(lib.getLibFile().getPath());
        if (!CVS.isFileInCVS(file)) return;

        String dir = file.getParent();
        String delib = file.getName();

        unedit(delib, dir);
        CVSLibrary.setEditing(lib, false);
    }

    public static void unedit(Cell cell) {
        File file = CVS.getCellFile(cell);
        if (!CVS.isFileInCVS(CVS.getCellFile(cell))) {
            System.out.println("Can't Unedit "+cell.describe(false)+", Cell file not in CVS");
            return;
        }
        String dir = file.getParent();
        String c = file.getName();

        unedit(c, dir);
        CVSLibrary.setEditing(cell, false);
    }

    /**
     * Unedit a file and it's sub-directories and sub-files.
     * @param file
     * @param dir
     */
    public static void unedit(String file, String dir) {
        CVS.runCVSCommand("unedit "+file, "Unediting "+file,
                dir, System.out);
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

    public static class Editor {
        private final String abbreviatedFile;
        private final String user;
        private final Date date;
        private final String computer;
        private final File file;
        private Editor(String abbrevFile, String user, Date date, String computer, File file) {
            this.abbreviatedFile = abbrevFile;
            this.user = user;
            this.date = date;
            this.computer = computer;
            this.file = file;
        }
        private static Editor parse(String editorResultLine) {
            // parse editor command result
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
                File file = new File(parts[4]);
                return new Editor(abbreviatedFile, user, date, computer, file);
            } else {
                System.out.println("Bad Editor result line format: "+editorResultLine);
            }
            return null;
        }
        public String getAbbrevFile() { return abbreviatedFile; }
        public String getUser() { return user; }
        public Date getDate() { return date; }
        public String getComputer() { return computer; }
        public File getFile() { return file; }
    }

}
