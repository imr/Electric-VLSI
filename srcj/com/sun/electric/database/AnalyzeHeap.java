/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AnalyzeHeap.java
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

package com.sun.electric.database;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


/**
 * Class to analyze dump of JVM heap.
 */
public class AnalyzeHeap {
    private static final boolean REFERENCES = true;
    
    ArrayList<MyObject> objs = new ArrayList<MyObject>();
    
    private AnalyzeHeap() {}
     
    public static void analyze(String fileName) {
        AnalyzeHeap dumpHeap = new AnalyzeHeap();
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
            dumpHeap.read(in);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println((dumpHeap.objs.size() - 1) + " objects");
        
        dumpHeap.garbageCollect();
        dumpHeap.dump("heapdump.txt");
        dumpHeap.makePaths();
        dumpHeap.dump("heapdump2.txt");
    }
    
    private void read(DataInputStream in) throws IOException {
        int numObjs = in.readInt();
        objs.clear();
        while (objs.size() <= numObjs)
            objs.add(null);
        ArrayList<String> staticFields = new ArrayList<String>();
        ArrayList<String> fields = new ArrayList<String>();
        for (;;) {
            int h = in.readInt();
            if (h == 0) break;
            String className = in.readUTF();
            int mode = in.readByte();
            int staticLength = in.readInt();
            staticFields.clear();
            fields.clear();
            for (int i = 0; i < staticLength; i++)
                staticFields.add(in.readUTF());
            int length = in.readInt();
            for (int i = 0; i < length; i++)
                fields.add(in.readUTF());
            MyClass cls = new MyClass(className, mode, staticFields, fields);
            objs.set(h, cls);
        }
        for (;;) {
            int h = in.readInt();
            if (h == 0) break;
            MyString s = new MyString(in.readUTF());
            objs.set(h, s);
        }
        for (int h = 1; h < objs.size(); h++) {
            MyObject obj = (MyObject)objs.get(h);
            if (obj == null)
                objs.set(h, new MyObject());
        }
        for (int h = 1;; h++) {
            int classH = in.readInt();
            if (classH == 0) break;
            MyObject obj = (MyObject)objs.get(h);
            MyClass cls = (MyClass)objs.get(classH);
            obj.id = h;
            obj.setClass(cls);
            switch (cls.mode) {
                case MyClass.ARRAY:
                    int length = in.readInt();
                    for (int i = 0; i < length; i++) {
                        int elem = in.readInt();
                        new Link(obj, MyField.getElem(i), (MyObject)objs.get(elem));
                        //                          System.out.println("\t" + elem);
                    }
                    break;
                case MyClass.MAP:
                    int mapLength = in.readInt();
                    for (int i = 0; i < mapLength; i++) {
                        int key = in.readInt();
                        new Link(obj, MyField.getKey(i), (MyObject)objs.get(key));
                        int value = in.readInt();
                        new Link(obj, MyField.getElem(i), (MyObject)objs.get(value));
                    }
                    break;
                case MyClass.STRING:
                    break;
                case MyClass.CLASS:
                    obj.pathLink = new Link(null, ((MyClass)obj).classField, obj);
                    for (int i = 0; i < cls.fields.length; i++) {
                        int value = in.readInt();
                        new Link(obj, cls.fields[i], (MyObject)objs.get(value));
                    }
                    cls = (MyClass)objs.get(h);
                    for (int i = 0; i < cls.staticFields.length; i++) {
                        int value = in.readInt();
                        new Link(obj, cls.staticFields[i], (MyObject)objs.get(value));
                    }
                    break;
                case MyClass.NORMAL:
                    for (int i = 0; i < cls.fields.length; i++) {
                        int value = in.readInt();
                        new Link(obj, cls.fields[i], (MyObject)objs.get(value));
                    }
                    break;
            }
        }
    }
    
    private void garbageCollect(MyObject obj, HashSet<MyObject> visited) {
        if (obj == null || visited.contains(obj)) return;
        visited.add(obj);
        for (Iterator<Link> it = obj.linksFrom.iterator(); it.hasNext(); ) {
            Link l = (Link)it.next();
            if (!REFERENCES && l.field.referent)
                continue;
            garbageCollect(l.to, visited);
        }
    }
    
    private void garbageCollect() {
        HashSet<MyObject> visited = new HashSet<MyObject>();
        for (int h = 1; h < objs.size(); h++) {
            MyObject obj = (MyObject)objs.get(h);
            if (obj instanceof MyClass)
                garbageCollect(obj, visited);
        }
        int collected = 0, remained = 0;
        for (int h = 1; h < objs.size(); h++) {
            MyObject obj = (MyObject)objs.get(h);
            if (obj == null) continue;
            if (!visited.contains(obj)) {
                objs.set(h, null);
                collected++;
            }
            for (Iterator<Link> it = obj.linksTo.iterator(); it.hasNext(); ) {
                Link l = (Link)it.next();
                if (l.from != null && !visited.contains(l.from))
                    it.remove();
            }
            remained++;
        }
        System.out.println(collected + " objects collected  " + remained + " remained");
    }
    
    private void makePaths() {
        for (int k = 0; k < 100; k++) {
            int named = stepPath(true, false, false);
            if (named == 0) break;
        }
        if (REFERENCES) {
            for (int k = 0; k < 100; k++) {
                int named = stepPath(true, true, false);
                if (named == 0) break;
            }
        }
//        stepPath(true, false);
//        for (int k = 0; k < 100; k++) {
//            int named = stepPath(false, false, true);
//            if (named == 0) break;
//        }
        countUnnamed();

        int singleRefered = 0;
        for (int h = 0; h < objs.size(); h++) {
            MyObject obj = (MyObject)objs.get(h);
            if (obj == null) continue;
            if (!obj.isSingleOwned()) continue;
            if (obj.linksTo.size() == 0) {
                System.out.println(obj + " has no access");
                continue;
            }
            singleRefered++;
            Link pathLink = (Link)obj.linksTo.get(0);
            assert obj.pathLink == null || obj.pathLink == pathLink;
            obj.pathLink = pathLink;;
        }
        System.out.println(singleRefered + " single-refered");
        countUnnamed();
    }
    
    private int stepPath(boolean doMaps, boolean trackReferents, boolean verbose) {
        HashSet<MyObject> named = new HashSet<MyObject>();
        for (int h = 1; h < objs.size(); h++) {
            MyObject obj = (MyObject)objs.get(h);
            if (obj == null || obj.pathLink == null) continue;
            if (named.contains(obj)) continue;
            boolean doAll = doMaps || obj.cls.mode != MyClass.MAP && obj.cls.mode != MyClass.ARRAY;
            for (Iterator<Link> it = obj.linksFrom.iterator(); it.hasNext(); ) {
                Link l = (Link)it.next();
                if (l.to == null || l.to.pathLink != null) continue;
                if (!trackReferents && l.field.referent) continue;
                boolean single = l.to.isSingleOwned();
                if (doAll || single) {
                    l.to.pathLink = l;
                    named.add(l.to);
                    if (verbose && !single)
                        System.out.println(l.to.toString());
                }
            }
        }
        System.out.println(named.size() + " named");
//        countUnnamed();
        return named.size();
    }
    
    private void countUnnamed() {
        int unnamed = 0;
        for (int h = 0; h < objs.size(); h++) {
            MyObject obj = (MyObject)objs.get(h);
            if (obj != null && obj.pathLink == null)
                unnamed++;
        }
        System.out.println(unnamed + " unnamed");
    }
    
    private void dump(String dumpName) {
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(dumpName)));
            for (int h = 0; h < objs.size(); h++) {
                MyObject obj = (MyObject)objs.get(h);
                if (obj == null) continue;
                out.println(obj.toString());
                for (Iterator<Link> it = obj.linksFrom.iterator(); it.hasNext(); ) {
                    Link l = (Link)it.next();
                    if (l.to == null) continue;
                    out.println("\t" + l.field.name + "\t" + (l.to != null ? l.to.toString() : "null"));
                }
                out.println("\t-");
                for (Iterator<Link> it = obj.linksTo.iterator(); it.hasNext(); ) {
                    Link l = (Link)it.next();
                    if (l == obj.pathLink) continue;
                    out.println("\t" + (l.from != null ? l.from.path() + "." : "") + l.field.name);
                }
                out.println();
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class MyObject {
    MyClass cls;
    int id;
    Link pathLink;
    ArrayList<Link> linksFrom = new ArrayList<Link>();
    ArrayList<Link> linksTo = new ArrayList<Link>(1);
    
    void setClass(MyClass cls) {
        this.cls = cls;
    }
    
    boolean isSingleOwned() { return linksTo.size() <= 1; }
    
    public String path() {
        if (pathLink == null)
            return (isSingleOwned() ? "!" : "*") + id;
        else if (pathLink.from == null)
            return pathLink.field.name;
        else
            return pathLink.from.path() + "." + pathLink.field.name;
    }
    
    public String toString() {
        return path() + "(" + (isSingleOwned() ? "!" : "*") + id + ") " + cls.className;
    } 
}

class MyString extends MyObject {
    String value;
    
    MyString(String value) {
        this.value = value;
    }
    
    public String toString() {
        return super.toString() + " \"" + value + "\"";
    }
}

class MyClass extends MyObject {
    static final int NORMAL = 0;
    static final int STRING = 1;
    static final int ARRAY = 2;
    static final int MAP = 3;
    static final int CLASS = 4;
    
    String className;
    int mode;
    MyField[] staticFields;
    MyField[] fields;
    MyField classField;
    
    MyClass(String className, int mode, List<String> staticFieldList, List<String> fieldList) {
        this.className = className;
        classField = new MyField(0, MyField.CLASS, className);
        this.mode = mode;
        staticFields = new MyField[staticFieldList.size()];
        for (int i = 0; i < staticFields.length; i++)
            staticFields[i] = new MyField(i, MyField.STATICFIELD, staticFieldList.get(i));
        fields = new MyField[fieldList.size()];
        for (int i = 0; i < fields.length; i++) {
            String fieldName = fieldList.get(i);
            MyField f = new MyField(i, MyField.FIELD, fieldName);
            if (fieldName.equals("referent") && className.startsWith("java.lang.ref."))
                f.referent = true;
            fields[i] = f;
        }
    }
    
    public String toString() {
        String modeString = "";
        switch (mode) {
            case STRING: modeString = " (STRING)"; break;
            case ARRAY: modeString = " (ARRAY)"; break;
            case MAP: modeString = " (MAP)"; break;
            case CLASS: modeString = " (CLASS)"; break;
        }
        return super.toString() + modeString;
    }
    
}

class MyField {
    static final int FIELD = 0;
    static final int STATICFIELD = 1;
    static final int ELEM = 2;
    static final int KEY = 3;
    static final int CLASS = 5;
    
    int index;
    int mode;
    String name;
    boolean referent;
    
    static final ArrayList<MyField> elems = new ArrayList<MyField>();
    static final ArrayList<MyField> keys = new ArrayList<MyField>();
    
    MyField(int index, int mode, String name) {
        this.index = index;
        this.mode = mode;
        this.name = name;
    }
    
    static MyField getElem(int index) {
        while (elems.size() <= index)
            elems.add(new MyField(elems.size(), ELEM, elems.size() + ""));
        return (MyField)elems.get(index);
    }

    static MyField getKey(int index) {
        while (keys.size() <= index)
            keys.add(new MyField(keys.size(), KEY, keys.size() + "k"));
        return (MyField)keys.get(index);
    }
}

class Link {
    MyObject from;
    MyField field;
    MyObject to;
    
    Link(MyObject from, MyField field, MyObject to) {
        this.from = from;
        this.field = field;
        this.to = to;
        if (from != null)
            from.linksFrom.add(this);
        if (to != null)
            to.linksTo.add(this);
    }
}