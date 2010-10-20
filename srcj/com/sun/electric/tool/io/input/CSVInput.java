/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CSVInput.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.input;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import com.sun.electric.tool.io.CSVFile;

/**
 * @author Felix Schmidt
 * 
 */
public class CSVInput extends FileInputStream {

    private BufferedReader inStream;

    /**
     * @param file
     * @throws FileNotFoundException
     */
    public CSVInput(File file) throws FileNotFoundException {

        super(file);
        this.inStream = new BufferedReader(new InputStreamReader(this));

    }

    public CSVFile readCSVFile(Character separator, boolean hasHeader) throws IOException {
        CSVFile result = new CSVFile(separator);

        if (hasHeader) {
            result.setHeader(splitLine(this.inStream.readLine(), separator));
        }

        String line = null;
        while ((line = this.inStream.readLine()) != null) {
            result.addLine(splitLine(line, separator));
        }

        return result;
    }

    private List<String> splitLine(String line, Character separator) {
        return Arrays.asList(line.split(separator.toString()));
    }

}
