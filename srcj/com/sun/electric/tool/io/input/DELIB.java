package com.sun.electric.tool.io.input;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.hierarchy.Cell;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Mar 8, 2006
 * Time: 4:24:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class DELIB extends JELIB {
    
    private static String[] revisions =
    {
        // revision 1
        "8.04f"
    };

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
        // get the file location; remove 'C' at start
        String cellFile = line.substring(1, line.length());
        File cellFD = new File(filePath + File.separator + cellFile);
        LineNumberReader cellReader;
        try {
            FileInputStream fin = new FileInputStream(cellFD);
            InputStreamReader is = new InputStreamReader(fin);
            cellReader = new LineNumberReader(is);
        } catch (IOException e) {
            System.out.println("Error opening file "+cellFD+": "+e.getMessage());
            return;
        }
        lineReader = cellReader;

        // read cell line of cell file
        for(;;) {
            line = lineReader.readLine();
            if (line == null) break;
            if (line.length() == 0) continue;
            if (line.charAt(0) == 'C') break;
        }
        if (line == null) {
            System.out.println("Error reading file "+cellFD);
            lineReader = headerReader;
            return;
        }

        super.readCell(line);
        lineReader.close();
        lineReader = headerReader;
    }

}
