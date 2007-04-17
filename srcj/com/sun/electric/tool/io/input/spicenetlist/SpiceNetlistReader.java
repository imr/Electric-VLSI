/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SpiceNetlistReader.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.input.spicenetlist;

import java.io.*;
import java.util.*;

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

    private File file;
    BufferedReader reader;
    private StringBuffer lines;
    private int lineno;

    private HashMap<String,String> options = new LinkedHashMap<String,String>();
    private HashMap<String,String> globalParams = new LinkedHashMap<String,String>();
    private List<SpiceInstance> topLevelInstances = new ArrayList<SpiceInstance>();
    private HashMap<String,SpiceSubckt> subckts = new LinkedHashMap<String,SpiceSubckt>();
    private List<String> globalNets = new ArrayList<String>();
    private SpiceSubckt currentSubckt;

    public SpiceNetlistReader() {
        reader = null;
        lines = null;
    }

    public HashMap<String,String> getOptions() { return options; }
    public HashMap<String,String> getGlobalParams() { return globalParams; }
    public List<SpiceInstance> getTopLevelInstances() { return topLevelInstances; }
    public Collection<SpiceSubckt> getSubckts() { return subckts.values(); }
    public List<String> getGlobalNets() { return globalNets; }
    public SpiceSubckt getSubckt(String name) {
        return subckts.get(name.toLowerCase());
    }

//    private static final boolean DEBUG = true;

    // ============================== Parsing ==================================

//    enum TType { PAR, PARVAL, WORD }

    public void readFile(String fileName, boolean verbose) throws FileNotFoundException {
        file = new File(fileName);
        reader = new BufferedReader(new FileReader(fileName));
        lines = new StringBuffer();
        currentSubckt = null;

        String line;
        lineno = 0;
        try {
            while ((line = readLine()) != null) {
                line = line.trim();
                String [] tokens = getTokens(line);
                if (tokens.length == 0) continue;
                String keyword = tokens[0].toLowerCase();

                if (keyword.equals(".include")) {
                    if (tokens.length < 2) {
                        prErr("No file specified for .include");
                        continue;
                    }
                    String ifile = tokens[1];
                    if (!ifile.startsWith("/") && !ifile.startsWith("\\")) {
                        // relative path, add to current path
                        File newFile = new File(file.getParent(), ifile);
                        ifile = newFile.getPath();
                    }
                    File saveFile = file;
                    BufferedReader saveReader = reader;
                    StringBuffer saveLines = lines;
                    int saveLine = lineno;

                    try {
                        if (verbose)
                            System.out.println("Reading include file "+ifile);
                        readFile(ifile, verbose);
                    } catch (FileNotFoundException e) {
                        file = saveFile;
                        lineno = saveLine;
                        prErr("Include file does not exist: "+ifile);
                    }
                    file = saveFile;
                    reader = saveReader;
                    lines = saveLines;
                    lineno = saveLine;
                }
                else if (keyword.startsWith(".opt")) {
                    parseOptions(options, 1, tokens);
                }
                else if (keyword.equals(".param")) {
                    parseParams(globalParams, 1, tokens);
                }
                else if (keyword.equals(".subckt")) {
                    currentSubckt = parseSubckt(tokens);
                    if (currentSubckt != null && subckts.containsKey(currentSubckt.getName().toLowerCase())) {
                        prErr("Subckt "+currentSubckt.getName()+
                                " already defined");
                        continue;
                    }
                    subckts.put(currentSubckt.getName().toLowerCase(), currentSubckt);
                }
                else if (keyword.equals(".global")) {
                    for (int i=1; i<tokens.length; i++) {
                        if (!globalNets.contains(tokens[i]))
                            globalNets.add(tokens[i]);
                    }
                }
                else if (keyword.startsWith(".ends")) {
                    currentSubckt = null;
                }
                else if (keyword.startsWith(".end")) {
                    // end of file
                }
                else if (keyword.startsWith("x")) {
                    SpiceInstance inst = parseSubcktInstance(tokens);
                    addInstance(inst);
                }
                else if (keyword.startsWith("r")) {
                    SpiceInstance inst = parseResistor(tokens);
                    addInstance(inst);
                }
                else if (keyword.startsWith("c")) {
                    SpiceInstance inst = parseCapacitor(tokens);
                    addInstance(inst);
                }
                else if (keyword.startsWith("m")) {
                    SpiceInstance inst = parseMosfet(tokens);
                    addInstance(inst);
                }
                else if (keyword.equals(".protect") || keyword.equals(".unprotect")) {
                    // ignore
                }
                else {
                    prWarn("Parser does not recognize: "+line);
                }
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("Error reading file "+file.getPath()+": "+e.getMessage());
        }
    }

    private void prErr(String msg) {
        System.out.println("Error ("+getLocation()+"): "+msg);
    }
    private void prWarn(String msg) {
        System.out.println("Warning ("+getLocation()+"): "+msg);
    }
    private String getLocation() {
        return file.getName()+":"+lineno;
    }

    /**
     * Get the tokens in a line.  Tokens are separated by whitespace,
     * unless that whitespace is surrounded by single quotes, or parentheses.
     * When quotes are used, those quotes are removed from the string literal.
     * The construct <code>name=value</code> is returned as three tokens,
     * the second being the char '='.
     * @param line the line to parse
     * @return an array of tokens
     */
    private String [] getTokens(String line) {
        List<String> tokens = new ArrayList<String>();
        int start = 0;
        boolean inquotes = false;
        int inparens = 0;
        int i;
        for (i=0; i<line.length(); i++) {
            char c = line.charAt(i);
            if (inquotes) {
                if (c == '\'') {
                    if (inparens > 0) continue;
                    // end string literal
                    tokens.add(line.substring(start, i));
                    start = i+1;
                    inquotes = false;
                }
            }
            else if (c == '\'') {
                if (inparens > 0) continue;
                inquotes = true;
                if (start != i) {
                    prErr("Improper use of open quote '");
                    break;
                }
                start = i+1;
            }
            // else !inquotes:
            else if (Character.isWhitespace(c) && inparens == 0) {
                // end of token (unless just more whitespace)
                if (start < i)
                    tokens.add(line.substring(start, i));
                start = i+1;
            }
            else if (c == '(') {
                inparens++;
            }
            else if (c == ')') {
                if (inparens == 0) {
                    prErr("Too many ')'s");
                    break;
                }
                inparens--;
            }
            else if (c == '=') {
                if (start < i)
                    tokens.add(line.substring(start, i));
                tokens.add("=");
                start = i+1;
            }
            else if (c == '*') {
                break; // rest of line is comment
            }
        }
        if (start < i) {
            tokens.add(line.substring(start, i));
        }

        if (inparens != 0)
            prErr("Unmatched parentheses");

        // join {par, =, val} to {par=val}
        List<String> joined = new ArrayList<String>();
        for (int j=0; j<tokens.size(); j++) {
            if (tokens.get(j).equals("=")) {
                if (j == 0) {
                    prErr("No right hand side to assignment");
                } else if (j == tokens.size()-1) {
                    prErr("No left hand side to assignment");
                } else {
                    int last = joined.size() - 1;
                    joined.set(last, joined.get(last)+"="+tokens.get(++j));
                }
            } else {
                joined.add(tokens.get(j));
            }
        }
        String ret [] = new String[joined.size()];
        for (int k=0; k<joined.size(); k++)
            ret[k] = joined.get(k);
        return ret;
    }

    private void parseOptions(HashMap<String,String> map, int start, String [] tokens) {
        for (int i=start; i<tokens.length; i++) {
            int e = tokens[i].indexOf('=');
            String pname = tokens[i];
            String value = "true";
            if (e > 0) {
                pname = tokens[i].substring(0, e);
                value = tokens[i].substring(e+1);
            }
            if (pname == null || value == null) {
                prErr("Bad option value: "+tokens[i]);
                continue;
            }
            map.put(pname.toLowerCase(), value);
        }
    }

    private void parseParams(HashMap<String,String> map, int start, String [] tokens) {
        for (int i=start; i<tokens.length; i++) {
            parseParam(map, tokens[i], null);
        }
    }

    private void parseParam(HashMap<String,String> map, String parval, String defaultParName) {
        int e = parval.indexOf('=');
        String pname = defaultParName;
        String value = parval;
        if (e > 0) {
            pname = parval.substring(0, e);
            value = parval.substring(e+1);
            if (defaultParName != null && !defaultParName.equalsIgnoreCase(pname)) {
                prWarn("Expected param "+defaultParName+", but got "+pname);
            }
        }
        if (pname == null || value == null) {
            prErr("Badly formatted param=val: "+parval);
            return;
        }
        map.put(pname.toLowerCase(), value);
    }

    private SpiceSubckt parseSubckt(String [] parts) {
        SpiceSubckt subckt = new SpiceSubckt(parts[1]);
        int i=2;
        for (; i<parts.length; i++) {
            if (parts[i].indexOf('=') > 0) break;  // parameter
            subckt.addPort(parts[i]);
        }
        parseParams(subckt.getParams(), i, parts);
        return subckt;
    }

    private SpiceInstance parseSubcktInstance(String [] parts) {
        String name = parts[0].substring(1);
        List<String> nets = new ArrayList<String>();
        int i=1;
        for (; i<parts.length; i++) {
            if (parts[i].contains("=")) break;  // parameter
            nets.add(parts[i]);
        }
        String subcktName = nets.remove(nets.size()-1); // last one is subckt reference
        SpiceSubckt subckt = subckts.get(subcktName.toLowerCase());
        if (subckt == null) {
            prErr("Cannot find subckt for "+subcktName);
            return null;
        }
        SpiceInstance inst = new SpiceInstance(subckt, name);
        for (String net : nets)
            inst.addNet(net);
        parseParams(inst.getParams(), i, parts);
        // consistency check
        if (inst.getNets().size() != subckt.getPorts().size()) {
            prErr("Number of ports do not match: "+inst.getNets().size()+
                  " (instance "+name+") vs "+subckt.getPorts().size()+
                  " (subckt "+subckt.getName()+")");
        }
        return inst;
    }

    private void addInstance(SpiceInstance inst) {
        if (inst == null) return;
        if (currentSubckt != null)
            currentSubckt.addInstance(inst);
        else
            topLevelInstances.add(inst);
    }

    private SpiceInstance parseResistor(String [] parts) {
        if (parts.length < 4) {
            prErr("Not enough arguments for resistor");
            return null;
        }
        SpiceInstance inst = new SpiceInstance(parts[0]);
        for (int i=1; i<3; i++) {
            inst.addNet(parts[i]);
        }
        parseParam(inst.getParams(), parts[3], "r");
        return inst;
    }

    private SpiceInstance parseCapacitor(String [] parts) {
        if (parts.length < 4) {
            prErr("Not enough arguments for capacitor");
            return null;
        }
        SpiceInstance inst = new SpiceInstance(parts[0]);
        for (int i=1; i<3; i++) {
            inst.addNet(parts[i]);
        }
        parseParam(inst.getParams(), parts[3], "c");
        return inst;
    }

    private SpiceInstance parseMosfet(String [] parts) {
        if (parts.length < 8) {
            prErr("Not enough arguments for mosfet");
            return null;
        }
        SpiceInstance inst = new SpiceInstance(parts[0]);
        int i=1;
        for (; i<5; i++) {
            inst.addNet(parts[i]);
        }
        String model = parts[i];
        inst.getParams().put("model", model);
        i++;
        parseParams(inst.getParams(), i, parts);
        return inst;
    }

    private void parseComment(String line) {
        if (currentSubckt == null) return;
        String [] parts = line.split("\\s+");
        for (int i=0; i<parts.length; i++) {
            if (parts[i].equalsIgnoreCase("PORT") && i+2 < parts.length) {
                String dir = parts[i+1];
                String port = parts[i+2];
                SpiceSubckt.PortType type = SpiceSubckt.PortType.valueOf(dir);
                if (type != null)
                    currentSubckt.setPortType(port, type);
                return;
            }
        }
    }

    /**
     * Read one line of the spice file. This concatenates continuation lines
     * that start with '+' to the first line, replacing '+' with ' '.  It
     * also ignores comment lines (lines that start with '*').
     * @return one spice line
     * @throws IOException
     */
    private String readLine() throws IOException {
        while (true) {
            lineno++;
            String line = reader.readLine();
            if (line == null) {
                // EOF
                if (lines.length() == 0) return null;
                return removeString();
            }
            line = line.trim();
            if (line.startsWith("*")) {
                // comment line
                parseComment(line);
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

    // ================================= Writing ====================================

    public void writeFile(String fileName) {
        if (fileName == null) {
            write(System.out);
        } else {
            try {
                PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(fileName)));
                write(out);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void write(PrintStream out) {
        out.println("* Spice netlist");
        for (String key : options.keySet()) {
            String value = options.get(key);
            if (value == null) {
                out.println(".option "+key);
            } else {
                out.println(".option "+key+"="+options.get(key));
            }
        }
        out.println();
        for (String key : globalParams.keySet()) {
            out.println(".param "+key+"="+globalParams.get(key));
        }
        out.println();
        for (String net : globalNets) {
            out.println(".global "+net);
        }
        out.println();
        for (String subcktName : subckts.keySet()) {
            SpiceSubckt subckt = subckts.get(subcktName);
            subckt.write(out);
            out.println();
        }
        for (SpiceInstance inst : topLevelInstances) {
            inst.write(out);
        }
        out.println();
        out.println(".end");
        if (out != System.out) out.close();
    }

    static void multiLinePrint(PrintStream out, boolean isComment, String str)
    {
        // put in line continuations, if over 78 chars long
        char contChar = '+';
        if (isComment) contChar = '*';
        int lastSpace = -1;
        int count = 0;
        boolean insideQuotes = false;
        int lineStart = 0;
        for (int pt = 0; pt < str.length(); pt++)
        {
            char chr = str.charAt(pt);
//			if (sim_spice_machine == SPICE2)
//			{
//				if (islower(*pt)) *pt = toupper(*pt);
//			}
            if (chr == '\n')
            {
                out.print(str.substring(lineStart, pt+1));
                count = 0;
                lastSpace = -1;
                lineStart = pt+1;
            } else
            {
                if (chr == ' ' && !insideQuotes) lastSpace = pt;
                if (chr == '\'') insideQuotes = !insideQuotes;
                count++;
                if (count >= 78 && !insideQuotes && lastSpace > -1)
                {
                    String partial = str.substring(lineStart, lastSpace+1);
                    out.print(partial + "\n" + contChar);
                    count = count - partial.length();
                    lineStart = lastSpace+1;
                    lastSpace = -1;
                }
            }
        }
        if (lineStart < str.length())
        {
            String partial = str.substring(lineStart);
            out.print(partial);
        }
    }

    // ======================== Spice Netlist Information ============================

    // =================================== test ================================

    public static void main(String [] args) {
        SpiceNetlistReader reader = new SpiceNetlistReader();

        try {
            reader.readFile("/import/async/cad/2006/bic/jkg/bic/testSims/test_clk_regen.spi", true);
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }

        reader.writeFile("/tmp/output.spi");
    }

//    private static void testLineParserTests() {
//        SpiceNetlistReader reader = new SpiceNetlistReader();
//        reader = new SpiceNetlistReader();
//        reader.file = new File("/none");
//        reader.lineno = 1;
//        testLineParser(reader, ".measure tran vmin min v( data) from=0ns to=1.25ns  ");
//        testLineParser(reader, ".param poly_res_corner='1.0 * p' * 0.8 corner");
//        testLineParser(reader, ".param poly_res_corner   =    '1.0 * p' * 0.8 corner");
//        testLineParser(reader, ".param AVT0N = AGAUSS(0.0,  '0.01 / 0.1' , 1)");
//    }
//
//    private static void testLineParser(SpiceNetlistReader reader, String line) {
//        System.out.println("Parsing: "+line);
//        String [] tokens = reader.getTokens(line);
//        for (int i=0; i<tokens.length; i++) {
//            System.out.println(i+": "+tokens[i]);
//        }
//    }
}
