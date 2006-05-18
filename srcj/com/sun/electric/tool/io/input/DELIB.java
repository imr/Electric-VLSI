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
import com.sun.electric.tool.io.FileType;

import java.io.*;

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
        return super.readLib();
    }

    protected void readCell(String line) throws IOException {
        if (lineReader != headerReader) {
            // already reading a separate cell file
            super.readCell(line);
            return;
        }

        // get the file location; remove 'C' at start
        String cellFile = line.substring(1, line.length());

        cellFile = cellFile.replace(com.sun.electric.tool.io.output.DELIB.PLATFORM_INDEPENDENT_FILE_SEPARATOR, File.separatorChar);
        File cellFD = new File(filePath, cellFile);
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
        try {
            readFromFile(true);
        } finally {
            version = savedVersion;
            revision = savedRevision;
            escapeChar = savedEscapeChar;
            curLibName = savedCurLibName;
            lineReader.close();
            lineReader = headerReader;
        }
    }


    protected FileType getPreferredFileType() { return FileType.DELIB; }
}
