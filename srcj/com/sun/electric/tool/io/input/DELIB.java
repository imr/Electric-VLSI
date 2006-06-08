/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DELIB.java
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


import com.sun.electric.database.text.Version;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.tool.io.FileType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Mar 8, 2006
 * Time: 4:24:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class DELIB extends JELIB {
    
//    private static String[] revisions =
//    {
//        // revision 1
//        "8.04f"
//    };

    private LineNumberReader headerReader;
    private HashSet<String> delibCellFiles = new HashSet<String>();

    DELIB() {}

    /**
     * Method to read a Library in new library file (.jelib) format.
     * @return true on error.
     */
    protected boolean readLib()
    {
        String header = filePath + File.separator + "header";
        try {
            FileInputStream fin = new FileInputStream(header);
            InputStreamReader is = new InputStreamReader(fin);
            headerReader = new LineNumberReader(is);
        } catch (IOException e) {
            System.out.println("Error opening file "+header+": "+e.getMessage());
            return true;
        }
        lineReader = headerReader;
        boolean b = super.readLib();
        if (!b)
            lib.setDelibCellFiles(delibCellFiles);
        return b;
    }

    protected void readCell(String line) throws IOException {
        if (lineReader != headerReader) {
            // already reading a separate cell file
            super.readCell(line);
            return;
        }

        // get the file location; remove 'C' at start
        String cellFile = line.substring(1, line.length());

        // New header file as of version 8.04n, no cell refs, searches delib dir for cell files
        if (version.compareTo(Version.parseVersion(com.sun.electric.tool.io.output.DELIB.newHeaderVersion)) >= 0) {
            if (cellFile.equals(com.sun.electric.tool.io.output.DELIB.SEARCH_FOR_CELL_FILES)) {
                File delibDir = new File(filePath);
                if (delibDir.exists() && delibDir.isDirectory()) {
                    for (File file : delibDir.listFiles()) {
                        if (file.isDirectory()) continue;
                        String name = file.getName();
                        int dot = name.lastIndexOf('.');
                        if (dot < 0) continue;
                        View view = View.findView(name.substring(dot+1));
                        if (view == null) continue;
                        try {
                            readFile(file);
                        } catch (Exception e) {
                            if (e instanceof IOException) throw (IOException)e;
                            // some other exception, probably invalid cell file
                            Input.errorLogger.logError("Exception reading file "+file, -1);
                        }
                    }
                }
            }
            return;
        }

        cellFile = cellFile.replace(com.sun.electric.tool.io.output.DELIB.PLATFORM_INDEPENDENT_FILE_SEPARATOR, File.separatorChar);
        File cellFD = new File(filePath, cellFile);
        readFile(cellFD);
    }

    private void readFile(File cellFD) throws IOException {

        LineNumberReader cellReader;
        try {
            FileInputStream fin = new FileInputStream(cellFD);
            InputStreamReader is = new InputStreamReader(fin);
            cellReader = new LineNumberReader(is);
        } catch (IOException e) {
            System.out.println("Error opening file "+cellFD+": "+e.getMessage());
            return;
        }
        Version savedVersion = version;
        int savedRevision = revision;
        char savedEscapeChar = escapeChar;
        String savedCurLibName = curLibName;
        lineReader = cellReader;
        curReadFile = cellFD.getAbsolutePath();
        try {
            readFromFile(true);
            delibCellFiles.add(curReadFile);
        } finally {
            version = savedVersion;
            revision = savedRevision;
            escapeChar = savedEscapeChar;
            curLibName = savedCurLibName;
            lineReader.close();
            lineReader = headerReader;
            curReadFile = filePath;
        }
    }


    protected FileType getPreferredFileType() { return FileType.DELIB; }
}
