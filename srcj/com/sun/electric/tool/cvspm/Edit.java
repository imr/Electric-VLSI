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
import com.sun.electric.tool.user.User;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Date;
import java.text.DateFormat;
import java.net.InetAddress;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Mar 15, 2006
 * Time: 4:31:21 PM
 * To change this template use File | Settings | File Templates.
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
        CVS.runCVSCommand("edit -a none "+file, "Edit",
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
        public ListEditorsJob(List<Library> libs, List<Cell> cells, boolean forProject) {
            super("List CVS Editors", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
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
            CVS.runCVSCommand("editors "+args, "List CVS Editors", useDir, System.out);
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

    public static class Editor {
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
