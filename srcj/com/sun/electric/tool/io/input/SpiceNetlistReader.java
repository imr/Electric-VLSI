package com.sun.electric.tool.io.input;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Aug 3, 2006
 * Time: 4:18:45 PM
 * To change this template use File | Settings | File Templates.
 */

/**
 * Parse a spice netlist. Ignores comments, and
 * coalesces split lines into single lines
 */
public class SpiceNetlistReader {

    //private String fileName;
    BufferedReader reader;
    private StringBuffer lines;

    public SpiceNetlistReader(String fileName) throws FileNotFoundException {
        //this.fileName = fileName;
        reader = new BufferedReader(new FileReader(fileName));
        lines = new StringBuffer();
    }

    /**
     * Read one line of the spice file. This concatenates continuation lines
     * that start with '+' to the first line, replacing '+' with ' '.  It
     * also ignores comment lines (lines that start with '*').
     * @return one spice line
     * @throws IOException
     */
    public String readLine() throws IOException {
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                // EOF
                if (lines.length() == 0) return null;
                return removeString();
            }
            line = line.trim();
            if (line.startsWith("*")) {
                // comment line
                continue;
            }
            if (line.startsWith("+")) {
                // continuation line
                lines.append(" ");
                lines.append(line.substring(1));
                continue;
            }
            // normal line
            if (lines.length() == 0) {
                // this is the first line read, read next line to see if continued
                lines.append(line);
            } else {
                // beginning of next line, save it and return completed line
                String ret = removeString();
                lines.append(line);
                return ret;
            }
        }
    }

    private String removeString() {
        String ret = lines.toString();
        lines.delete(0, lines.length());
        return ret;
    }

}
