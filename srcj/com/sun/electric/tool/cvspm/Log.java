/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Log.java
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
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.user.dialogs.CVSLog;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.DELIB;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.LineNumberReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Mar 23, 2006
 * Time: 3:10:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class Log {

    public static void showLog(Cell cell) {
        if (!CVS.isDELIB(cell.getLibrary())) {
            System.out.println("Cannot show log file for non-DELIB library");
            return;
        }
        if (!CVS.isFileInCVS(CVS.getCellFile(cell))) {
            System.out.println("Cell "+cell.describe(false)+" is not in CVS");
            return;
        }
        List<Cell> cells = new ArrayList<Cell>();
        cells.add(cell);
        String useDir = CVS.getUseDir(null, cells);
        StringBuffer cellsBuf = CVS.getCellFiles(cells, useDir);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CVS.runCVSCommand("status "+cellsBuf, "Show CVS Status", useDir, out);
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));
        String workingVersion = getWorkingRevision(reader);

        out = new ByteArrayOutputStream();
        CVS.runCVSCommand("log "+cellsBuf, "Show CVS Log", useDir, out);
        reader = new LineNumberReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));
        System.out.println("Show CVS Log complete.");
        Log log = new Log(cell);
        log.parseLogOutput(reader);


        CVSLog dialog = new CVSLog(log.entries, "CVS Log for "+cell.libDescribe(), workingVersion);
        dialog.setVisible(true);
    }

    public static void showLog(Library lib) {
        if (!CVS.isFileInCVS(TextUtils.getFile(lib.getLibFile()))) {
            System.out.println("Library "+lib.getName()+" is not in CVS");
            return;
        }
        List<Library> libs = new ArrayList<Library>();
        libs.add(lib);
        String useDir = CVS.getUseDir(libs, null);

        StringBuffer libsBuf;
        if (CVS.isDELIB(lib)) {
            // show log for header file
            libsBuf = new StringBuffer();
            File libFile = TextUtils.getFile(lib.getLibFile());
            if (libFile == null) return;
            String file = libFile.getPath();
            if (file.startsWith(useDir)) {
                file = file.substring(useDir.length()+1, file.length());
            }
            libsBuf.append(file+File.separator+DELIB.getHeaderFile()+" ");
        } else {
            libsBuf = CVS.getLibraryFiles(libs, useDir);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CVS.runCVSCommand("status "+libsBuf, "Show CVS Status", useDir, out);
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));
        String workingVersion = getWorkingRevision(reader);

        out = new ByteArrayOutputStream();
        CVS.runCVSCommand("log "+libsBuf, "Show CVS Log", useDir, out);
        reader = new LineNumberReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));
        System.out.println("Show CVS Log complete.");
        Log log = new Log(lib);
        log.parseLogOutput(reader);

        CVSLog dialog = new CVSLog(log.entries, "CVS Log for "+lib.getName(), workingVersion);
        dialog.setVisible(true);
    }

    /**
     * Compare the specified version with the local copy
     * @param entry
     */
    public static void compareWithLocal(LogEntry entry) {
        StringBuffer args;
        String useDir;
        if (entry.obj instanceof Cell) {
            List<Cell> cells = new ArrayList<Cell>();
            cells.add((Cell)entry.obj);
            useDir = CVS.getUseDir(null, cells);
            args = CVS.getCellFiles(cells, useDir);
        } else if (entry.obj instanceof Library) {
            List<Library> libs = new ArrayList<Library>();
            libs.add((Library)entry.obj);
            useDir = CVS.getUseDir(libs, null);
            args = CVS.getLibraryFiles(libs, useDir);
        } else {
            System.out.println("Cannot compare Electric Object "+entry.obj);
            return;
        }

        String version = entry.version;

        CVS.runCVSCommand("diff -r "+version+" "+args, "Compare with Local", useDir, System.out);
        System.out.println("Compare with Local complete.");
    }

    public static void compare(LogEntry entry1, LogEntry entry2) {
        StringBuffer args;
        String useDir;
        if (entry1.obj instanceof Cell) {
            List<Cell> cells = new ArrayList<Cell>();
            cells.add((Cell)entry1.obj);
            useDir = CVS.getUseDir(null, cells);
            args = CVS.getCellFiles(cells, useDir);
        } else if (entry1.obj instanceof Library) {
            List<Library> libs = new ArrayList<Library>();
            libs.add((Library)entry1.obj);
            useDir = CVS.getUseDir(libs, null);
            args = CVS.getLibraryFiles(libs, useDir);
        } else {
            System.out.println("Cannot compare Electric Object "+entry1.obj);
            return;
        }

        String version1 = entry1.version;
        String version2 = entry2.version;

        CVS.runCVSCommand("diff -r "+version1+" -r "+version2+" "+args, "Compare Versions", useDir, System.out);
        System.out.println("Compare Versions complete.");
    }

    public static void getVersion(LogEntry entry) {
        (new RevertToVersion(entry)).startJob();
    }

    public static class RevertToVersion extends Job {
        private LogEntry entry;
        public RevertToVersion(LogEntry entry) {
            super("Revert to CVS Version", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.entry = entry;
        }
        public boolean doIt() {
            String version = entry.version;

            StringBuffer args;
            String useDir;
            if (entry.obj instanceof Cell) {
                List<Cell> cells = new ArrayList<Cell>();
                cells.add((Cell)entry.obj);
                useDir = CVS.getUseDir(null, cells);
                args = CVS.getCellFiles(cells, useDir);
            } else if (entry.obj instanceof Library) {
                List<Library> libs = new ArrayList<Library>();
                libs.add((Library)entry.obj);
                useDir = CVS.getUseDir(libs, null);
                args = CVS.getLibraryFiles(libs, useDir);
            } else {
                System.out.println("Cannot compare Electric Object "+entry.obj);
                return false;
            }

            File aFile = new File(useDir, args.toString().trim());
            File aFileBackup = new File(useDir, args.toString().trim()+"___version"+version);
            String aFilePath = aFile.getPath();
            String aFileBackupPath = aFileBackup.getPath();
            if (!aFile.exists()) {
                System.out.println("Error: File does not exist: "+aFile.getPath());
                return false;
            }

            CVS.runCVSCommand("update -r "+version+" "+args, "Get Version", useDir, System.out);
            // copy old file away
            if (aFileBackup.exists()) {
                if (!aFileBackup.delete()) {
                    System.out.println("Error: Could not delete backup file: "+aFileBackup.getPath());
                    return false;
                }
            }
            if (!aFile.renameTo(aFileBackup)) {
                System.out.println("Error: Could not rename file "+aFile.getPath()+" to "+aFileBackup.getPath());
                return false;
            }
            // update with current version, remove sticky tag
            CVS.runCVSCommand("update -A "+args, "Remove Sticky Tag", useDir, System.out);

            // delete updated file, and move back old version file
            File oldCellFile = new File(aFilePath);
            if (!oldCellFile.delete()) {
                System.out.println("Error: Could not delete file: "+aFile.getPath());
                return false;
            }
            if (!aFileBackup.renameTo(new File(aFilePath))) {
                System.out.println("Error: Could not rename file "+aFileBackup.getPath()+" to "+aFilePath);
                return false;
            }
            // reload cell
            //LibraryFiles.reloadLibraryCells(cells);
            if (entry.obj instanceof Cell)
                LibraryFiles.reloadLibrary(((Cell)entry.obj).getLibrary());
            else if (entry.obj instanceof Library)
                LibraryFiles.reloadLibrary((Library)entry.obj);

            System.out.println("Get Version complete.");
            return true;
        }
        public void terminateOK() {
            if (entry.obj instanceof Cell) {
                Cell cell = (Cell)entry.obj;
                Library newLib = Library.findLibrary(cell.getLibrary().getName());
                if (newLib == null) return;
                Cell newCell = newLib.findNodeProto(cell.noLibDescribe());
                if (newCell == null) return;
                showLog(newCell);
                CVS.fixStaleCellReferences(cell.getLibrary());
            } else if (entry.obj instanceof Library) {
                Library lib = (Library)entry.obj;
                Library newLib = Library.findLibrary(lib.getName());
                if (newLib == null) return;
                showLog(newLib);
                CVS.fixStaleCellReferences(lib);
            }
        }
    }

    // -------------------------------------------------------

    private HashMap<String,String> versionsToTags;
    private List<LogEntry> entries;
    private ElectricObject obj;
    private String headVersion;

    /**
     * Create a new log for either a Cell or Library
     * @param obj the library or cell
     */
    private Log(ElectricObject obj) {
        this.obj = obj;
        versionsToTags = new HashMap<String,String>();
        entries = new ArrayList<LogEntry>();
        headVersion = "";
    }

    private void parseLogOutput(LineNumberReader reader) {
        try {
            parseLog(reader);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return;
        }
    }


    private void parseLog(LineNumberReader reader) throws IOException {
        // parse header, don't care about stuff until symbolic names (tags)
        for (;;) {
            String line = reader.readLine();
            if (line == null) return;
            if (line.equals("")) continue;

            if (line.startsWith("head:")) {
                headVersion = line.substring(5).trim();
            }

            if (line.startsWith("symbolic names:")) {
                parseSymbolicNames(reader);
            }
            if (line.startsWith("revision")) {
                LogEntry entry = parseLogEntry(line, reader);
                if (entry != null)
                    entries.add(entry);
            }
        }
    }

    private void parseSymbolicNames(LineNumberReader reader) throws IOException {
        for (;;) {
            String line = reader.readLine();
            if (line == null) return;
            if (line.equals("")) continue;
            if (line.startsWith("keyword substitution")) return;

            String parts[] = line.trim().split(":\\s+");
            if (parts.length != 2) {
                System.out.println("Bad format for symbolic name: "+line);
                continue;
            }
            versionsToTags.put(parts[1], parts[0]);
        }
    }
    private LogEntry parseLogEntry(String line, LineNumberReader reader)
        throws IOException {
        // get revision
        String parts[] = line.trim().split("\\s+");
        if (parts.length != 2) {
            System.out.println("Bad revision line format: "+line);
            return null;
        }
        String version = parts[1];
        String tag = versionsToTags.get(version);
        if (tag == null) tag = "";

        line = reader.readLine();
        if (line == null) {
            System.out.println("Unexpected end of file");
            return null;
        }
        parts = line.trim().split(";\\s+");
        String branch = "";
        String date = "";
        String author = "";
        String state = "";
        StringBuffer commitMessage = new StringBuffer();
        for (int i=0; i<parts.length; i++) {
            if (parts[i].startsWith("date: ")) {
                date = parts[i].substring(6);
            }
            else if (parts[i].startsWith("author: ")) {
                author = parts[i].substring(7).trim();
            }
            else if (parts[i].startsWith("state: ")) {
                state = parts[i].substring(6).trim();
            }
        }

        line = reader.readLine();
        while (!line.startsWith("-----------------------") &&
               !line.startsWith("==========================")) {
            if (line.startsWith("branches: ")) {
                branch = line.substring(9).trim();
                line = reader.readLine();
                continue;
            }
            // commit message...may span multiple lines
            commitMessage.append(line+"\n");
            line = reader.readLine();
        }

        return new LogEntry(obj, version, branch, date, author, commitMessage.toString(),
                state, tag, headVersion);
    }


    public static class LogEntry implements Serializable {
        public final ElectricObject obj;
        public final String version;
        public final String branch;
        public final String date;
        public final String author;
        public final String commitMessage;
        public final String state;
        public final String tag;
        public final String headVersion;        // latest available version in cvs
        private LogEntry(ElectricObject obj,
                         String version,
                         String branch,
                         String date,
                         String author,
                         String commitMessage,
                         String state,
                         String tag,
                         String headVersion) {
            this.obj = obj;
            this.version = version;
            this.branch = branch;
            this.date = date;
            this.author = author;
            this.commitMessage = commitMessage;
            this.state = state;
            this.tag = tag;
            this.headVersion = headVersion;
        }
        public void print() {
            System.out.println(version + "\t" +
                    branch + "\t" +
                    date + "\t" +
                    author + "\t" +
                    commitMessage.replaceAll("\\n", " ") +
                    state + "\t" +
                    tag);
        }
    }

    // get the working revision from a cvs status output
    private static String getWorkingRevision(LineNumberReader reader) {
        try {
            for (;;) {
                String line = reader.readLine();
                if (line == null) return "";
                if (line.equals("")) continue;
                line = line.trim();
                if (line.startsWith("Working revision:")) {
                    String parts[] = line.trim().split("\t");
                    if (parts.length >= 2) {
                        return parts[1].trim();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return "";
    }
}
