/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LibDirs.java
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
package com.sun.electric.tool.io.input;

import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Class for reading a text file that specifies
 * additional library directories from which to
 * read in library (elib) files.
 *
 * <p>
 * LibDirs files have the following syntax:
 * <p>
 * * <comment text>
 * <p>
 * include <lib_dirs_file>
 * <p>
 * <library_directory>
 *
 * <p>
 */
public class LibDirs {

    /** Default LibDirs file name */                        private static String libDirsFile = "LIBDIRS";
    /** List of library directories from LibDirs file*/     private static ArrayList<String> dirs = new ArrayList<String>();
    /** List of libDirsFiles read (prevent recursion) */    private static ArrayList<String> libDirsFiles = new ArrayList<String>();

    /** Creates a new instance of LibDirs */
    LibDirs() {
    }

    /** return list of Lib Dirs 
     * @return ArrayList of lib dirs
     */
    public static synchronized Iterator<String> getLibDirs() {
        ArrayList<String> list = new ArrayList<String>(dirs);
        return list.iterator();
    }

    /**
     * Read in LibDirs file.
     * @param dir the directory that may contain a LIBDIRS file
     * @return true on error, or no LIBDIRS file found.
     */
    public static synchronized boolean readLibDirs(String dir)
    {
        if (dir == null) return true;
        String libDirFile = dir + File.separator + libDirsFile;
        // read current working dir first, if set to do so
        boolean error = false;
        dirs.clear();
        libDirsFiles.clear();

        if (parseFile(libDirFile)) error = true;
        return error;
    }
        
    /** 
     * parse a LIF file and read in libraries specified.
     * @return true on error.
     */
    private static boolean parseFile(String fileName)
    {
        if (fileName == null) return true;
        
        BufferedReader in = null;
        File file = new File(fileName);
		try {
            FileReader fr = new FileReader(file);
            in = new BufferedReader(fr);
		} catch (FileNotFoundException e) {
			//System.out.println("Could not find file: " + fileName);
			return true;
		}
        libDirsFiles.add(file.getAbsolutePath());

        // parse file
        boolean error = false;
        int lineNumber = 0;
        try {
            String line;
            while ( (line = in.readLine()) != null) {
                if (parseLine(line, file)) {
                    System.out.println("Parse error: "+fileName+":"+lineNumber);
                    error = true;
                    lineNumber++;
                }
            }
        } catch (IOException e) {
            System.out.println("IOError "+e.getMessage());
        }
        return error;
    }
    
    /**
     * Parse one line of LIF file.
     * @return true on error.
     */
    private static boolean parseLine(String line, File libdirFile)
    {
        line = line.trim();                             // remove leading/trailing whitespace
        String[] words = line.split("\\s+");            // split by whitespace
        if (words.length <= 0) return false;            // nothing on line
        if (words[0].equals("*")) return false;         // comment
        if (words[0].equals("include")) {               // include statement
            if (words.length != 2) return true;
            // prevent recursive includes
            File f = new File(words[1]);
            if (libDirsFiles.contains(f.getAbsolutePath())) return true;
            // read included LibDir file
            return parseFile(words[1]);
        }
        // add dir to list, prepend current dir if relative path
        String dir = words[0];
        if (dir.startsWith(".")) {
            dir = libdirFile.getParentFile().getAbsolutePath() + File.separator + dir;
        }
        dirs.add(dir);
        return false;
    }

    // --------------------------------------------------------------

    /**
     * Allow JFileChooser to see libdir references as files. This should only
     * be used when trying to open JELIB/ELIB libraries through a JFileChooser.
     * We can't extend UnixFileSystemView or WindowsFileSystemView because they
     * are private classes, so instead we must wrap them.
     */
    public static class LibDirFileSystemView extends FileSystemView {

        private final FileSystemView osView;        // at run time java determines an OS-specific view
        private final HashMap<String,File> libFiles;             // key: absolute path; value: File

        public LibDirFileSystemView(FileSystemView osView) {
            this.osView = osView;
            libFiles = new HashMap<String,File>();
        }

        public File createFileObject(File dir, String filename) { return osView.createFileObject(dir, filename); }
        public File createFileObject(String path) { return osView.createFileObject(path); }
        protected File createFileSystemRoot(File f) {
            throw new Error("Unsupported operation"); // can't access protected method of osView
        }
        public File createNewFolder(File containingDir) throws IOException {
            return osView.createNewFolder(containingDir);
        }
        public File getChild(File parent, String fileName) { return osView.getChild(parent, fileName); }
        public File getDefaultDirectory() { return osView.getDefaultDirectory(); }

        public synchronized File [] getFiles(File dir, boolean useFileHiding) {
            File [] usual = osView.getFiles(dir, useFileHiding);
            libFiles.clear();

            if (LibDirs.readLibDirs(dir.getPath())) {
                return usual;
            }
            // amalgamate all of the files
            ArrayList<File> alllibfiles = new ArrayList<File>();
            if (usual.length != 0) alllibfiles.addAll(Arrays.asList(usual));

            for (Iterator<String> it = LibDirs.getLibDirs(); it.hasNext(); ) {
                String str = it.next();
                File libdir = new File(str);
                if (!libdir.exists()) continue;
                if (!libdir.isDirectory()) continue;
                File [] files = osView.getFiles(libdir, useFileHiding);
                //System.out.println("Reading files from dir "+str+", found "+files.length+" files.");
                if (files.length == 0) continue;
                alllibfiles.addAll(Arrays.asList(files));
                for (int i=0; i<files.length; i++) {
                    libFiles.put(files[i].getAbsolutePath(), files[i]);
                }
            }
            File [] all = new File[alllibfiles.size()];
            for (int i=0; i<alllibfiles.size(); i++) {
                File f = alllibfiles.get(i);
                all[i] = f;
            }
            return all;
        }
        public synchronized boolean isLibFile(File f) {
            if (f == null) return false;
            if (libFiles.containsKey(f.getAbsolutePath())) return true;
            return false;
        }

        public File getHomeDirectory() { return osView.getHomeDirectory(); }
        public File getParentDirectory(File dir) { return osView.getParentDirectory(dir); }
        public File [] getRoots() { return osView.getRoots(); }
        public String getSystemDisplayName(File f) { return osView.getSystemDisplayName(f); }
        public Icon getSystemIcon(File f) { return osView.getSystemIcon(f); }
        public String getSystemTypeDescription(File f) { return osView.getSystemTypeDescription(f); }
        public boolean isComputerNode(File dir) { return osView.isComputerNode(dir); }
        public boolean isDrive(File dir) { return osView.isDrive(dir); }
        public boolean isFileSystem(File f) { return osView.isFileSystem(f); }
        public boolean isFileSystemRoot(File dir) { return osView.isFileSystemRoot(dir); }
        public boolean isFloppyDrive(File dir) { return osView.isFloppyDrive(dir); }
        public boolean isHiddenFile(File f) { return osView.isHiddenFile(f); }
        public boolean isParent(File folder, File file) {
            // all lib files should look like they are in the current dir
            if (isLibFile(file)) { return true; }
            return osView.isParent(folder, file);
        }
        public boolean isRoot(File f) { return osView.isRoot(f); }
        public Boolean isTraversable(File f) { return osView.isTraversable(f); }
    }

    public static LibDirFileSystemView newLibDirFileSystemView(FileSystemView osView) { return new LibDirFileSystemView(osView); }
    
    /**
     * Allow File Chooser to see libdir references as files.
     */
    public static class LibDirFileView extends FileView {
        private LibDirFileSystemView view;
        public LibDirFileView(LibDirFileSystemView view) {
            this.view = view;
        }

        public String getName(File f) {
            if (f == null) return null;
            if (view.isLibFile(f)) return f.getName() + " [REF]";
            return null;
        }
    }
}
