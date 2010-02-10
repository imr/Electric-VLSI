/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LibData.java
 * Written by Jonathan Gainsley, Sun Microsystems.
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation.sctiming;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.PrintStream;
import java.io.FileOutputStream;


/**
 * LibertyObjects.java
 * Jonathan Gainsley
 * 24 Oct 2006
 *
 * Objects to store parsed data from LibertyParser
 */

public class LibData
{
    public Group library;

    private static final int MAX_LINE_LENGTH = 80; // when writing to disk

    /**
     * Create a new LibData object to hold Liberty format data.
     */
    public LibData() {}

    /**
     * Write the data stored in this object to a Liberty format file.
     * @param filename the file to write to
     */
    public void write(String filename) {
        try {
            write(new PrintStream(new FileOutputStream(filename)));
        } catch (java.io.FileNotFoundException e) {
            System.out.println("LibData: Error writing to file "+filename+": "+e.getMessage());
        }
    }

    /**
     * Write the data stored in this object to a Liberty format file.
     * @param out the output stream to write to
     */
    public void write(PrintStream out) {
        if (library == null) {
            System.out.println("LibData: No Data to write");
            return;
        }
        library.write(out, 0);
    }

    /**
     * Get the library from the liberty file read.
     * Note that there is only one library per file.
     * @return the library data
     */
    public Group getLibrary() {
        return library;
    }

    /**
     * Set the library for the liberty file.
     * Note that there is only one library per file.
     */
    public void setLibrary(Group group) {
        this.library = group;
    }

    // ---------------------------------------------------------
    // Liberty Object Definitions
    // ---------------------------------------------------------

    /**
     * Head object
     */
    public static class Head {
        public String name;
        int lineno;
        String filename;
        List<Value> valuesList;

        public Head(String name, List attrList) {
            this.name = name;
            valuesList = new ArrayList<Value>();
            for (Object obj : attrList) {
                if (obj instanceof Value)
                    valuesList.add((Value)obj);
            }
        }

        public List<Value> getValues() { return valuesList; }
    }

    /**
     * Group object
     */
    public static class Group {
        private String type;
        private List<String> names;
        private Group owner;

        Map<String,Attribute> attrList;
        Map<String,Define> defineList;
        List<Group> groupList;  // multiple groups of the same type and no name may exist

        public Group(String type, String name, Group owner) {
            this.type = type;
            this.names = new ArrayList<String>();
            if (name != null)
                this.names.add(name);
            this.owner = owner;
            this.attrList = new LinkedHashMap<String,Attribute>();
            this.defineList = new LinkedHashMap<String,Define>();
            this.groupList = new ArrayList<Group>();
            if (owner != null)
                owner.putGroup(this);
        }

        public Group(Head h, Group owner) {
            this.type = h.name;
            this.names = new ArrayList<String>();
            this.owner = owner;
            this.attrList = new LinkedHashMap<String,Attribute>();
            this.defineList = new LinkedHashMap<String,Define>();
            this.groupList = new ArrayList<Group>();
            for (Value a : h.getValues()) {
                names.add(a.value.toString());
            }
            if (owner != null)
                owner.putGroup(this);
        }

        public String getType() { return type; }
        public String getName() { if (names.size() > 0) return names.get(0); return null; }
        public List<String> getNames() { return names; }
        public Group getOwner() { return owner; }
        public String getHName() {
            String parentName = owner == null ? "" : owner.getHName()+".";
            String thisname = type + (names.size() == 0 ? "" : "-"+names.get(0));
            return parentName + thisname;
        }

        public void putAttribute(Attribute a) {
            if (attrList.containsKey(a.name)) {
                System.out.println("Duplicate Attribute name "+a.name+" in group "+getHName());
            }
            attrList.put(a.name, a);
        }

        public void putGroup(Group g) {
            groupList.add(g);
        }

        public void putDefine(Define d) {
            if (defineList.containsKey(d.name)) {
                System.out.println("Duplicate Define name "+d.name+" in group "+getHName());
            }
            defineList.put(d.name, d);
        }

        public Attribute getAttribute(String name) {
            return attrList.get(name);
        }

        public void putAttribute(String name, double value) {
            Value val = new Value(ValueType.DOUBLE, new Double(value));
            putAttribute(new Attribute(name, val));
        }

        public void putAttribute(String name, String value) {
            Value val = new Value(ValueType.STRING, value);
            putAttribute(new Attribute(name, val));
        }

        public void putAttribute(String name, int value) {
            Value val = new Value(ValueType.INT, new Integer(value));
            putAttribute(new Attribute(name, val));
        }

        public void putAttribute(String name, List<String> values) {
            List<Value> vals = new ArrayList<Value>();
            for (String s : values) vals.add(new Value(ValueType.STRING, s));
            Head h = new Head(name, vals);
            putAttribute(new Attribute(name, h));
        }

        public void putAttributeComplex(String name, String value) {
            List<String> vals = new ArrayList<String>();
            vals.add(value);
            putAttribute(name, vals);
        }

        /**
         * Get all groups in this group
         * @return all groups in this group
         */
        public List<Group> getGroups() {
            return groupList;
        }

        /**
         * Get the group of the given type and name,
         * in this group.  Name can be null.
         * More than one group may exist; all matching
         * the criteria are returned.
         * @param type the group type: type(name) {}
         * @param name the group name
         * @return any group in this group matching the criteria
         */
        public List<Group> getGroups(String type, String name) {
            List<Group> groups = new ArrayList<Group>();
            if (type == null) return groups;
            for (Group g : groupList) {
                if (!type.equals(g.type)) continue;
                if (name == null && g.names.size() == 0) {
                    groups.add(g); continue;
                }
                if (name != null && g.names.contains(name)) {
                    groups.add(g);
                }
            }
            return groups;
        }

        public Define getDefine(String name) {
            return defineList.get(name);
        }

        public void removeGroup(Group group) {
            for (Group g : groupList) {
                if (g == group) {
                    groupList.remove(g);
                    return;
                }
            }
        }

        private void write(PrintStream out, int tabs) {
            prtabs(out, tabs);
            out.print(type+" (");
            for (Iterator<String> it = names.iterator(); it.hasNext(); ) {
                out.print(it.next());
                if (it.hasNext()) out.print(", ");
            }
            out.println(") {");
            tabs++;
            for (Attribute attr : attrList.values()) {
                attr.write(out, tabs);
            }
            for (Define define : defineList.values()) {
                define.write(out, tabs);
            }
            for (Group group : groupList) {
                group.write(out, tabs);
            }
            tabs--;
            prtabs(out, tabs);
            out.println("}");
        }
    }

    /**
     * Attribute Types
     */
    public enum AttrType { SIMPLE, COMPLEX }

    /**
     * Attribute object
     */
    public static class Attribute {
        AttrType type;
        String name;
        List<Value> values;

        public Attribute(String name, Value value) {
            this.name = name;
            this.type = AttrType.SIMPLE;
            this.values = new ArrayList<Value>();
            values.add(value);
        }

        public Attribute(String name, Head complexAttributeValues) {
            this.name = name;
            this.type = AttrType.COMPLEX;
            this.values = complexAttributeValues.getValues();
        }

        public AttrType getType() { return type; }
        public String getName() { return name; }
        public List<Value> getValues() { return values; }

        /** Get the integer value. Returns null if not an integer */
        public Integer getInt() {
            if (values.size() == 0) return null;
            return values.get(0).getInt();
        }

        /** Get the double value. Returns null if not a double */
        public Double getDouble() {
            if (values.size() == 0) return null;
            return values.get(0).getDouble();
        }

        /** Get the String value. Returns null if not a String */
        public String getString() {
            if (values.size() == 0) return null;
            return values.get(0).getString();
        }

        /** Get the Boolean value. Returns null if not a Boolean */
        public Boolean getBoolean() {
            if (values.size() == 0) return null;
            return values.get(0).getBoolean();
        }

        private void write(PrintStream out, int tabs) {
            prtabs(out, tabs);
            out.print(name);
            int i = tabs*2 + name.length();
            switch(type) {
                case SIMPLE: out.print(" : "); i+=3; break;
                case COMPLEX: out.print(" ("); i+=2; break;
            }
            int start = i;
            for (Iterator<Value> it = values.iterator(); it.hasNext(); ) {
                Value v = it.next();
                String s = v.getValueAsString();
                if ((i+s.length()) > MAX_LINE_LENGTH) {
                    out.println(" \\");
                    i=start;
                    prtabs(out, start/2);
                }
                out.print(s);
                i += s.length();
                if (it.hasNext()) { out.print(", "); i+=2; }
            }
            if (type == AttrType.COMPLEX) out.print(")");
            out.println(" ;");
        }
    }

    /**
     * Value types
     */
    public enum ValueType { STRING, DOUBLE, BOOLEAN, INT, EXPR, UNDEFINED }

    /**
     * Value objects
     */
    public static class Value {
        ValueType type;
        Object value;

        public Value(ValueType type, Object value) {
            this.type = type;
            this.value = value;
            if (value instanceof String) {
                String s = (String)value;
                if (s.startsWith("\"") && s.endsWith("\"")) {
                    s = s.substring(1, s.length()-1);   // strip quotes
                    this.value = s;
                }
            }
        }

        public ValueType getType() { return type; }
        public Object getValue() { return value; }

        public static ValueType getType(String s) {
            if (s == null) return ValueType.UNDEFINED;
            try {
                return ValueType.valueOf(s.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ValueType.UNDEFINED;
            }
        }

        /** Get the integer value. Returns null if not an integer */
        public Integer getInt() {
            if (value == null || type != ValueType.DOUBLE) return null;
            return new Integer(((Double)value).intValue());
        }

        /** Get the double value. Returns null if not a double */
        public Double getDouble() {
            if (value == null || type != ValueType.DOUBLE) return null;
            return (Double)value;
        }

        /** Get the String value. Returns null if not a String */
        public String getString() {
            if (value == null || type != ValueType.STRING) return null;
            return (String)value;
        }

        /** Get the Boolean value. Returns null if not a Boolean */
        public Boolean getBoolean() {
            if (value == null || type != ValueType.BOOLEAN) return null;
            return (Boolean)value;
        }

        private String getValueAsString() {
            String s = value.toString().trim();
            if (s.indexOf(' ') != -1 || s.indexOf('/') != -1 || s.indexOf('\\') != -1) {
                if (!s.startsWith("\"") && !s.endsWith("\""))
                    return "\""+s+"\"";
            }
            return s;
        }
    }

    /**
     * Definition objects
     */
    public static class Define {
        String name;
        String groupType;
        ValueType valueType;
        String comment;
        Group owner;

        Define(String name, Group owner, ValueType valueType) {
            this.name = name;
            this.owner = owner;
            this.valueType = valueType;
            if (owner != null)
                owner.putDefine(this);
        }

        private void write(PrintStream out, int tabs) {

        }
    }

    /**
     * Error types
     */
    public enum Error {
        NO_ERROR,
        INTERNAL_SYSTEM_ERROR,
        INVALID_VALUE,
        INVALID_NAME,
        INVALID_OBJECTTYPE,
        INVALID_ATTRTYPE,
        UNUSABLE_OID,
        OBJECT_ALREADY_EXISTS,
        OBJECT_NOT_FOUND,
        SYNTAX_ERROR,
        TRACE_FILES_CANNOT_BE_OPENED,
        PIINIT_NOT_CALLED,
        SEMANTIC_ERROR,
        REFERENCE_ERROR,
        MAX_ERROR
    }

    /**
     * Get error string for the given type
     * @param error
     * @return the error string
     */
    public static String getMessage(Error error) {
        switch(error) {
            case NO_ERROR: return "";
            case INTERNAL_SYSTEM_ERROR: return "Internal System Error";
            case INVALID_VALUE: return "Invalid Value";
            case INVALID_NAME: return "Invalid Name";
            case INVALID_OBJECTTYPE: return "Invalid Object Type";
            case INVALID_ATTRTYPE: return "Invalid Attribute Type";
            case UNUSABLE_OID: return "Unusable Object Identifier?";
            case OBJECT_ALREADY_EXISTS: return "Object Already Exists";
            case OBJECT_NOT_FOUND: return "Object Not Found";
            case SYNTAX_ERROR: return "Syntax Error";
            case TRACE_FILES_CANNOT_BE_OPENED: return "Trace Files Cannot be Opened";
            case PIINIT_NOT_CALLED: return "PI init() not called";
            case SEMANTIC_ERROR: return "Semantic Error";
            case REFERENCE_ERROR: return "Reference Error";
            case MAX_ERROR: return "Maximum Error";
        }
        return "Unknown Error Number: "+error;
    }

    private static void prtabs(PrintStream out, int tabs) {
        for (int i=0; i<tabs; i++) out.print("  ");
    }
}
