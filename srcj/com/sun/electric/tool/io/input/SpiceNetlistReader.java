package com.sun.electric.tool.io.input;

import java.io.*;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;

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
    private List<Instance> topLevelInstances = new ArrayList<Instance>();
    private HashMap<String,Subckt> subckts = new LinkedHashMap<String,Subckt>();
    private List<String> globalNets = new ArrayList<String>();
    private Subckt currentSubckt;

    public SpiceNetlistReader() {
        reader = null;
        lines = null;
    }

    public HashMap<String,String> getOptions() { return options; }
    public HashMap<String,String> getGlobalParams() { return globalParams; }
    public List<Instance> getTopLevelInstances() { return topLevelInstances; }
    public HashMap<String,Subckt> getSubckts() { return subckts; }
    public List<String> getGlobalNets() { return globalNets; }

    private static final boolean DEBUG = true;

    // ============================== Parsing ==================================

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

                if (line.startsWith(".include")) {
                    String [] parts = line.split("\\s+");
                    if (parts.length < 2) {
                        System.out.println("Error: No file specified for .include at "+getLocation());
                        continue;
                    }
                    String ifile = parts[1];
                    if (ifile.startsWith("'") && ifile.endsWith("'") ||
                        ifile.startsWith("\"") && ifile.endsWith("\""))
                        ifile = ifile.substring(1, ifile.length()-1);
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
                        readFile(ifile, verbose);
                    } catch (FileNotFoundException e) {
                        file = saveFile;
                        lineno = saveLine;
                        System.out.println("Error: Include file does not exist ("+getLocation()+"): "+ifile);
                    }
                    file = saveFile;
                    reader = saveReader;
                    lines = saveLines;
                    lineno = saveLine;
                }


                line = line.toLowerCase();
                line = line.replaceAll("\\s*=\\s*", "=");

                if (line.startsWith(".options ") || line.startsWith(".opt ")) {
                    line = removeFirstWord(line);
                    parseParams(options, line);
                }
                else if (line.startsWith(".param ")) {
                    line = removeFirstWord(line);
                    parseParams(globalParams, line);
                }
                else if (line.startsWith(".subckt")) {
                    line = removeFirstWord(line);
                    currentSubckt = parseSubckt(line);
                    if (currentSubckt != null && subckts.containsKey(currentSubckt.getName())) {
                        System.out.println("Error: Subckt "+currentSubckt.getName()+
                                " already defined at line "+lineno);
                        continue;
                    }
                    subckts.put(currentSubckt.getName(), currentSubckt);
                }
                else if (line.startsWith(".global ")) {
                    line = removeFirstWord(line);
                    String [] nets = line.split("\\s+");
                    for (int i=0; i<nets.length; i++)
                        globalNets.add(nets[i]);
                }
                else if (line.startsWith(".ends")) {
                    currentSubckt = null;
                }
                else if (line.startsWith(".end")) {
                    // end of file
                }
                else if (line.startsWith(".")) {
                    if (verbose)
                        System.out.println("Warning: Parser does not recognize ("+getLocation()+"): "+line);
                }
                else if (line.startsWith("x")) {
                    Instance inst = parseSubcktInstance(line);
                    addInstance(inst);
                }
                else if (line.startsWith("r")) {
                    Instance inst = parseResistor(line);
                    addInstance(inst);
                }
                else if (line.startsWith("c")) {
                    Instance inst = parseCapacitor(line);
                    addInstance(inst);
                }
                else if (line.startsWith("m")) {
                    Instance inst = parseMosfet(line);
                    addInstance(inst);
                }
                else {
                    System.out.println("Warning: Parser does not recognize ("+getLocation()+"): "+line);
                }
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("Error reading file "+file.getPath()+": "+e.getMessage());
        }
    }

    private String getLocation() {
        return file.getName()+":"+lineno;
    }

    private void addInstance(Instance inst) {
        if (inst == null) return;
        if (currentSubckt != null)
            currentSubckt.addInstance(inst);
        else
            topLevelInstances.add(inst);
    }

    /** Remove the first word on a line */
    private String removeFirstWord(String line) {
        int i = line.indexOf(' ');
        return line.substring(i).trim();
    }

    private void parseParams(HashMap<String,String> map, String line) {
        String [] parts = line.split("\\s+");
        for (int i=0; i<parts.length; i++) {
            parseParam(map, parts[i]);
        }
    }

    private void parseParam(HashMap<String,String> map, String paramAndValue) {
        String [] str = paramAndValue.split("=");
        if (str.length == 1) {
            map.put(str[0].toLowerCase(), null);
        } else {
            map.put(str[0].toLowerCase(), str[1]);
        }
    }

    private Subckt parseSubckt(String line) {
        String [] parts = line.split("\\s+");
        Subckt subckt = new Subckt(parts[0]);
        int i=1;
        for (; i<parts.length; i++) {
            if (parts[i].contains("=")) break;  // parameter
            subckt.addPort(parts[i]);
        }
        for (; i<parts.length; i++) {
            parseParam(subckt.getParams(), parts[i]);
        }
        return subckt;
    }

    private Instance parseSubcktInstance(String line) {
        String [] parts = line.split("\\s+");
        String name = parts[0].substring(1);
        List<String> nets = new ArrayList<String>();
        int i=1;
        for (; i<parts.length; i++) {
            if (parts[i].contains("=")) break;  // parameter
            nets.add(parts[i]);
        }
        String subcktName = nets.remove(nets.size()-1); // last one is subckt reference
        Subckt subckt = subckts.get(subcktName);
        if (subckt == null) {
            System.out.println("Error: Cannot find subckt for "+subcktName+" at line "+lineno);
            return null;
        }
        Instance inst = new Instance(subckt, name);
        for (String net : nets)
            inst.addNet(net);
        for (; i<parts.length; i++) {
            parseParam(inst.getParams(), parts[i]);
        }
        // consistency check
        if (inst.getNets().size() != subckt.getPorts().size()) {
            System.out.println("Error: Instance "+inst.getName()+" on line "+lineno+" has "+
            inst.getNets().size()+" ports, while the subckt definition has "+subckt.getPorts().size());
        }
        return inst;
    }

    private Instance parseResistor(String line) {
        String [] parts = line.split("\\s+");
        if (parts.length < 4) {
            System.out.println("Parse Error: Not enough arguments on line "+lineno+": "+line);
            return null;
        }
        Instance inst = new Instance(parts[0]);
        int i=1;
        for (; i<3; i++) {
            inst.addNet(parts[i]);
        }
        inst.getParams().put("r", parts[i]);
        return inst;
    }

    private Instance parseCapacitor(String line) {
        String [] parts = line.split("\\s+");
        if (parts.length < 4) {
            System.out.println("Parse Error: Not enough arguments on line "+lineno+": "+line);
            return null;
        }
        Instance inst = new Instance(parts[0]);
        int i=1;
        for (; i<3; i++) {
            inst.addNet(parts[i]);
        }
        inst.getParams().put("c", parts[i]);
        return inst;
    }

    private Instance parseMosfet(String line) {
        String [] parts = line.split("\\s+");
        if (parts.length < 8) {
            System.out.println("Parse Error: Not enough arguments on line "+lineno+": "+line);
            return null;
        }
        Instance inst = new Instance(parts[0]);
        int i=1;
        for (; i<5; i++) {
            inst.addNet(parts[i]);
        }
        String model = parts[i];
        inst.getParams().put("model", model);
        i++;
        for (; i<parts.length; i++) {
            parseParam(inst.getParams(), parts[i]);
        }
        return inst;
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
            Subckt subckt = subckts.get(subcktName);
            subckt.write(out);
            out.println();
        }
        out.println(".end");
    }

    private static void multiLinePrint(PrintStream out, boolean isComment, String str)
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

    public static class Instance {
        private char type;                  // spice type
        private String name;
        private List<String> nets;
        private Subckt subckt;              // may be null if primitive element
        private HashMap<String,String> params;

        public Instance(String typeAndName) {
            this.type = typeAndName.charAt(0);
            this.name = typeAndName.substring(1);
            this.nets = new ArrayList<String>();
            this.subckt = null;
            this.params = new HashMap<String,String>();
        }
        public Instance(Subckt subckt, String name) {
            this.type = 'X';
            this.name = name;
            this.nets = new ArrayList<String>();
            this.subckt = subckt;
            this.params = new LinkedHashMap<String,String>();
            for (String key : subckt.getParams().keySet()) {
                // set default param values
                this.params.put(key, subckt.getParams().get(key));
            }
        }
        public char getType() { return type; }
        public String getName() { return name; }
        public List<String> getNets() { return nets; }
        private void addNet(String net) { nets.add(net); }
        public HashMap<String,String> getParams() { return params; }
        public Subckt getSubckt() { return subckt; }
        public void write(PrintStream out) {
            StringBuffer buf = new StringBuffer(type);
            buf.append(name);
            buf.append(" ");
            for (String net : nets) {
                buf.append(net); buf.append(" ");
            }
            if (subckt != null) {
                buf.append(subckt.getName());
                buf.append(" ");
            }
            for (String key : params.keySet()) {
                buf.append(key);
                String value = params.get(key);
                if (value != null) {
                    buf.append("=");
                    buf.append(value);
                }
                buf.append(" ");
            }
            multiLinePrint(out, false, buf.toString());
        }
    }

    public static class Subckt {
        private String name;
        private List<String> ports;
        private HashMap<String,String> params;
        private List<Instance> instances;
        private Subckt(String name) {
            this.name = name;
            this.ports = new ArrayList<String>();
            this.params = new LinkedHashMap<String,String>();
            this.instances = new ArrayList<Instance>();
        }
        public String getName() { return name; }
        private void addPort(String port) { ports.add(port); }
        public List<String> getPorts() { return ports; }
        public HashMap<String,String> getParams() { return params; }
        private void addInstance(Instance inst) { instances.add(inst); }
        public List<Instance> getInstances() { return instances; }
        public void write(PrintStream out) {
            StringBuffer buf = new StringBuffer(".subckt ");
            buf.append(name);
            buf.append(" ");
            for (String port : ports) {
                buf.append(port);
                buf.append(" ");
            }
            for (String key : params.keySet()) {
                buf.append(key);
                buf.append("=");
                buf.append(params.get(key));
                buf.append(" ");
            }
            multiLinePrint(out, false, buf.toString());
            for (Instance inst : instances) {
                inst.write(out);
            }
            out.println(".ends "+name);
        }
    }

    // =================================== test ================================

    public static void main(String [] args) {
        SpiceNetlistReader reader = new SpiceNetlistReader();

        // test
        reader = new SpiceNetlistReader();
        try {
            reader.readFile("/import/async/cad/2006/bic/jkg/bic/testSims/test_clk_regen.spi", true);
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }

        reader.writeFile("/tmp/output.spi");
    }
}
