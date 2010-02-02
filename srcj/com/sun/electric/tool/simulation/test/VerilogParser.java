/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VerilogParser.java
 * Written by Jonathan Gainsley, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StreamTokenizer;
import java.io.IOException;

/**
 * A brain-dead, extremely limited verilog parser.  In fact, it really
 * doesn't do anything except return a module name with it's port defintions.
 *
 * <P>Again, this is not meant to be a fully featured parser.  It's error handling
 * is also not very useful.  It is only meant to extract module names and their ports.
 */
public class VerilogParser {

    /**
     * A Verilog Module. Contains a list of Ports. Does not
     * contain a list of sub-modules, always statements, or anything else!
     */
    public static final class Module {
        public final String name;
        private final List inports;
        private final List outports;
        private final List inoutports;

        public Module(String name) {
            this.name = name;
            inports = new ArrayList();
            outports = new ArrayList();
            inoutports = new ArrayList();
        }

        void addPort(Port port) {
            if (port.type == Port.INPUT) inports.add(port);
            else if (port.type == Port.OUTPUT) outports.add(port);
            else if (port.type == Port.INOUT) inoutports.add(port);
            else { System.out.println("Unknown port type ("+port.type+") for port "+port.name); }
        }
        List getInports() { return inports; }
        List getOutports() { return outports; }
        List getInoutports() { return inoutports; }
        List getPorts() {
            List ports = new ArrayList();
            ports.addAll(inports);
            ports.addAll(outports);
            ports.addAll(inoutports);
            return ports;
        }

        StringBuffer print() {
            StringBuffer buf = new StringBuffer();
            buf.append("module "+name);
            buf.append(printPorts(null));
            return buf;
        }

        StringBuffer printPorts(VerilogModel.AllAliasedNames aliased) {
            StringBuffer buf = new StringBuffer();
            int lineno = 0;
            buf.append("(");
            for (Iterator it = getPorts().iterator(); it.hasNext(); ) {
                Port p = (Port)it.next();
                String alias = p.name;
                if (aliased != null) alias = aliased.getAliasFor(alias);
                buf.append("."+p.name+"("+alias+")");
                if (it.hasNext()) buf.append(", ");
                if ((int)(buf.length() / 60) > lineno) {
                    buf.append("\n\t");
                    lineno++;
                }
            }
            buf.append(");\n");
            return buf;
        }
    }

    /**
     * A Verilog port.  May be bussed.
     */
    public static final class Port {
        public final String name;
        public final int type;
        public final int start;
        public final int end;
        public final boolean containsSpecialChars;
        public static final int INPUT = 0;
        public static final int OUTPUT = 1;
        public static final int INOUT = 2;

        private static final Pattern bussed = Pattern.compile("(\\w+)\\[(\\d+):(\\d+)\\]");
        private static final Pattern bussedSig = Pattern.compile("(\\w+)\\[(\\d+)\\]");

        public Port(String name, int type, int start, int end, boolean containsSpecialChars) {
            this.name = name;
            this.type = type;
            this.start = start;
            this.end = end;
            this.containsSpecialChars = containsSpecialChars;
            //System.out.println("Created port "+name+", "+type+", "+start+", "+end);
        }

        public Port(String name, int type) {
            this(name, type, 1, 1, false);
        }

        public boolean contains(String name) {
            if (name.equals(this.name)) return true;
            Matcher m = bussedSig.matcher(name);
            if (start != end && m.matches()) {
                String n = m.group(1);
                int idx = Integer.parseInt(m.group(2));
                if (n.equals(this.name) && (idx >= start && idx <= end))
                    return true;
            }
            return false;
        }
    }

    private List modules;
    private BufferedReader fin;

    public VerilogParser() {
        modules = new ArrayList();
        fin = null;
    }

    public List getModules() { return modules; }


    /**
     * Parse a verilog file. Very limited functionality at this time.
     * Only builds list of modules and their ports
     * @param verilogFile
     * @return list of modules parsed
     */
    public boolean parse(String verilogFile) {
        // try to open the file
        try {
            FileReader reader = new FileReader(verilogFile);
            fin = new BufferedReader(reader);
        } catch (java.io.IOException e) {
            System.out.println("Failed to open file for read: "+verilogFile+": "+e.getMessage());
        }
        if (fin == null) return false;

        StreamTokenizer st = new StreamTokenizer(fin);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.wordChars('_', '_');

        try {
            int token = st.nextToken();
            while (token != StreamTokenizer.TT_EOF) {
                switch(token) {
                    case StreamTokenizer.TT_WORD: {
                        // Parse Modules
                        if (st.sval.equals("module")) {
                            if (!parseModule(st)) return false;
                        } else if (st.sval.equals("primitive")) {
                            if (!parsePrimitive(st)) return false;
                        } else {
                            error(st, "expected module/primitive definition");
                        }
                        break;
                    }
                    case '`': {
                        int t = st.nextToken();
                        if (st.sval.equals("include")) {
                            st.nextToken();     // grab file name
                        } else if (st.sval.equals("define")) {
                            st.nextToken();
                            st.nextToken();
                        } else {
                            error(st, "expected valid defintion after `");
                        }
                        break;
                    }
                    default: {
                        error(st, "expected word");
                    }
                }
                token = st.nextToken();
            }
        } catch (java.io.IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    private boolean parseModule(StreamTokenizer st) {
        try {
            int token = st.nextToken();
            if (token != StreamTokenizer.TT_WORD) {
                error(st, "expected module name");
                return false;
            }
            Module module = new Module(st.sval);

            expect(st, '(');
            while (token != ')') {
                // gobble ports in module definition
                token = st.nextToken();
            }
            expect(st, ';');
            // parse ports
            boolean processPorts = true;
            while (processPorts) {
                token = st.nextToken();
                switch(token) {
                    case StreamTokenizer.TT_WORD: {
                        if (st.sval.equalsIgnoreCase("input")) {
                            parsePorts(st, Port.INPUT, module);
                        } else if (st.sval.equalsIgnoreCase("output")) {
                            parsePorts(st, Port.OUTPUT, module);
                        } else if (st.sval.equalsIgnoreCase("inout")) {
                            parsePorts(st, Port.INOUT, module);
                        } else {
                            processPorts = false;
                            st.pushBack();
                        }
                        break;
                    }
                }
            }
            // eat rest of module
            while(true) {
                token = st.nextToken();
                if (token == StreamTokenizer.TT_EOF) break;
                if (token == StreamTokenizer.TT_WORD && st.sval.equals("endmodule")) break;
            }
            modules.add(module);

        } catch (java.io.IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    private boolean parsePrimitive(StreamTokenizer st) {
        try {
            while (true) {
                int token = st.nextToken();
                if (token == StreamTokenizer.TT_EOF) break;
                if (token == StreamTokenizer.TT_WORD && st.sval.equals("endprimitive")) break;
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    private void parsePorts(StreamTokenizer st, int type, Module module) {
        try {
            int token;
            while ( (token = st.nextToken()) != ';') {
                if (token == ',') continue;
                String name = st.sval;
                int start = 1, end = 1;
                boolean quoted = false;
                if (token == '[') {
                    // parse bus
                    st.nextToken();
                    start = (int)st.nval;
                    expect(st, ':');
                    st.nextToken();
                    end = (int)st.nval;
                    expect(st, ']');
                    token = st.nextToken();
                    name = st.sval;
                }
                if (token == '\\') {
                    // parse special string delimited by \ at start and single whitespace at end
                    StringBuffer buf = new StringBuffer("\\");
                    st.ordinaryChar(' ');
                    while ( (token = st.nextToken()) != ' ') {
                        if (token == StreamTokenizer.TT_WORD) buf.append(st.sval);
                        else if (token == StreamTokenizer.TT_NUMBER) buf.append((int)st.nval);
                        else buf.append((char)token);
                    }
                    st.whitespaceChars(' ', ' ');
                    buf.append(" ");
                    name = buf.toString();
                    quoted = true;
                }
                module.addPort(new Port(name, type, start, end, quoted));
            }

        } catch (java.io.IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private boolean expect(StreamTokenizer st, int nextToken) {
        try {
            int token = st.nextToken();
            if (token != nextToken) {
                error(st, "expected "+(char)nextToken);
                return false;
            }
        } catch (java.io.IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    private void error(StreamTokenizer st, String msg) {
        System.out.print("Parse error on token ");
        switch(st.ttype) {
            case StreamTokenizer.TT_WORD: System.out.print("\""+st.sval+"\"");
            case StreamTokenizer.TT_NUMBER: System.out.print("\""+st.nval+"\"");
            case StreamTokenizer.TT_EOF: System.out.print("EOF");
            case StreamTokenizer.TT_EOL: System.out.print("EOL");
            default: System.out.print("\""+(char)st.ttype+"\"");
        }
        System.out.println(", line "+st.lineno()+": "+msg);
    }



    /** unit test */
    public static void main(String args[]) {
        VerilogParser vp = new VerilogParser();
        if (!vp.parse(VerilogModel.getExampleVerilogChipFile())) {
            System.out.println("Parsing failed.");
            return;
        }
        System.out.println("Parsing succeeded: modules are:");
        for (Iterator it = vp.getModules().iterator(); it.hasNext(); ) {
            Module m = (Module)it.next();
            System.out.println(m.print());
        }
    }
}
