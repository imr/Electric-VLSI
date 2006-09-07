/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ProjSettings.java
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
package com.sun.electric.tool.user.projectSettings;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.swing.*;
import java.net.URL;
import java.net.URLConnection;
import java.io.*;
import java.util.Stack;
import java.util.HashSet;
import java.util.Set;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Job;
import com.sun.electric.technology.Technology;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Jul 25, 2006
 * Time: 5:18:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProjSettings {

    private static ProjSettingsNode settings;
    private static File lastProjectSettingsFile = null;
    private static final String entry = "e";

    public static ProjSettingsNode getSettings() {
        if (settings == null) {
            settings = new ProjSettingsNode();
            lastProjectSettingsFile = null;
        }
        return settings;
    }

    public static void writeSettings(File file) {
        write(file.getPath(), getSettings());
    }

    public static void readSettings(File file) {
        ProjSettingsNode readNode = read(file.getPath());
        if (readNode == null) return; // error reading file

        if (lastProjectSettingsFile == null) {
            // first file read in, accept it
            settings = readNode;
            lastProjectSettingsFile = file;
            // update any changes to technologies including UI refresh
            Technology.TechPref.allTechnologiesChanged();
        } else {
            // not first file read in, check for conflicts,
            // and do not use it
            if (getSettings().printDifferences(readNode)) {
                SwingUtilities.invokeLater(new Runnable() { public void run() {
                    Job.getUserInterface().showInformationMessage("Warning: Project Settings conflicts; ignoring new settings. See messages window", "Project Settings Conflict"); }
                });
                System.out.println("Project Setting conflicts found: "+lastProjectSettingsFile.getPath()+" vs "+file.getPath());
            }
        }
    }

    // ------------------------------ Utility -------------------------------

    /**
     * Returns true if the project settings reader/writer/storage supports
     * the class type of the object.  Currently, only Integer, Long, Double,
     * Boolean, String, and ProjSettingsNode classes are supported.
     * @param object
     * @return true if supported, false otherwise
     */
    public static boolean isSupportedClass(Object object) {
        if ((object instanceof Boolean) || (object instanceof Double) ||
            (object instanceof Integer) || (object instanceof String) ||
            (object instanceof Long) || (object instanceof ProjSettingsNode)) {
            return true;
        }
        return false;
    }

    // ----------------------------- Private -------------------------------

    private static void write(String file, ProjSettingsNode node) {
        Writer wr = new Writer(new File(file));
        if (wr.write("ProjectSettings", node))
            System.out.println("Wrote Project Settings to "+file);
        wr.close();
    }

    private static ProjSettingsNode read(String file) {
        Reader rd = new Reader(TextUtils.makeURLToFile(file));
        if (!rd.read()) return null;
        System.out.println("Read Project Settings from "+file);
        return rd.getRootNode();
    }

    // ------------------------- ProjSettings Writer ----------------------------

    private static class Writer {
        private File file;
        PrintWriter out;
        private int indent;
        private static final int indentVal = 4;
        Set<Object> visited = new HashSet<Object>();

        private Writer(File file) {
            this.file = file;
        }

        private boolean write(String nodeName, ProjSettingsNode node) {
            out = new PrintWriter(new BufferedOutputStream(System.out));
            if (file != null) {
                try {
                    out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(file)));
                } catch (java.io.IOException e) {
                    System.out.println("Error opening "+file+" for write: "+e.getMessage());
                    file = null;
                    return false;
                }
            }
            writeNode(nodeName, node);
            return true;
        }


        private void prIndent(String msg) {
            for (int i=0; i<indent*indentVal; i++) { out.print(" "); }
            out.println(msg);
        }

        private void writeNode(String name, ProjSettingsNode node) {
            if (visited.contains(node)) {
                // already wrote this object out, must be recursion.
                System.out.println("ERROR: recursive loop in Project Settings; "+
                        "ignoring second instantiation of ProjSettingsNode \""+name+"\".");
                return;
            }
            visited.add(node);

            String classDef = "";
            if (node.getClass() != ProjSettingsNode.class) {
                classDef = " class=\""+node.getClass().getName()+"\">";
            }

            if (node.getKeys().size() == 0) {
                // if nothing in node, skip
                //prIndent("<node key=\""+name+"\""+classDef+"/>");
            } else {
                prIndent("<node key=\""+name+"\""+classDef+">");
                indent++;
                for (String key : node.getKeys()) {
                    writeSetting(key, node.get(key));
                }
                indent--;
                prIndent("</node>");
            }
        }

        private void writeSetting(String key, Object value) {
            if (value instanceof ProjSettingsNode) {
                ProjSettingsNode node = (ProjSettingsNode)value;
                writeNode(key, node);
            } else if (isSupportedClass(value)) {
                if (value instanceof Integer) {
                    prIndent("<"+entry+" key=\""+key+"\"\t int=\""+value.toString()+"\" />");
                } else if (value instanceof Double) {
                    prIndent("<"+entry+" key=\""+key+"\"\t double=\""+value.toString()+"\" />");
                } else if (value instanceof Long) {
                    prIndent("<"+entry+" key=\""+key+"\"\t long=\""+value.toString()+"\" />");
                } else if (value instanceof Boolean) {
                    prIndent("<"+entry+" key=\""+key+"\"\t boolean=\""+value.toString()+"\" />");
                } else {
                    prIndent("<"+entry+" key=\""+key+"\"\t string=\""+value.toString()+"\" />");
                }
            } else {
                System.out.println("Ignoring unsupported class "+value.getClass().getName()+" for key "+key+
                        " in project settings");
            }
        }

        private void close() {
            if (file != null) out.close();
        }
    }

    // ------------------------- ProjSettings Reader ----------------------------

    private static class Reader extends DefaultHandler {
        private URL url;
        private Stack<ProjSettingsNode> context;
        private Locator locator;
        private ProjSettingsNode lastPopped;

        private static final boolean DEBUG = false;

        private Reader(URL url) {
            this.url = url;
            this.context = new Stack<ProjSettingsNode>();
            this.locator = null;
            lastPopped = null;
        }

        private boolean read() {
            try {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setValidating(true);
                factory.setNamespaceAware(true);
                URLConnection conn = url.openConnection();
                InputStream is = conn.getInputStream();
                factory.newSAXParser().parse(is, this);
                return true;
            } catch (IOException e) {
                System.out.println("Error reading file "+url.toString()+": "+e.getMessage());
            } catch (SAXParseException e) {
                System.out.println("Error parsing file "+url.toString()+" on line "+e.getLineNumber()+
                        ", column "+e.getColumnNumber()+": "+e.getMessage());
            } catch (ParserConfigurationException e) {
                System.out.println("Configuration error reading file "+url.toString()+": "+e.getMessage());
            } catch (SAXException e) {
                System.out.println("Exception reading file "+url.toString()+": "+e.getMessage());
            }
            return false;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {

            // get the name of the key
            String key = attributes.getValue("key");
            if (key == null) {
                throw parseException("Entry "+qName+" is missing \"key\" attribute");
            }

            // see if this is a project settings node
            if (qName.equals("node")) {
                String nodeClassStr = attributes.getValue("class");
                if (nodeClassStr == null)
                    nodeClassStr = ProjSettingsNode.class.getName();
                try {
                    Class nodeClass = Class.forName(nodeClassStr);
                    ProjSettingsNode node = (ProjSettingsNode)nodeClass.newInstance();
                    if (!context.isEmpty()) {
                        ProjSettingsNode parent = context.peek();
                        parent.put(key, node);
                    }
                    context.push(node);
                } catch (ClassNotFoundException e) {
                    throw parseException(e.getMessage());
                } catch (InstantiationException e) {
                    throw parseException(e.getMessage());
                } catch (IllegalAccessException e) {
                    throw parseException(e.getMessage());
                }
            } else if (qName.equals(entry)) {

                // must be a key-value pair
                if (context.isEmpty()) {
                    throw parseException("No node for key-value pair "+qName);
                }
                ProjSettingsNode currentNode = context.peek();
                String value = null;
                try {
                    if ((value = attributes.getValue("string")) != null) {
                        currentNode.put(key, value);
                    } else if ((value = attributes.getValue("int")) != null) {
                        currentNode.put(key, Integer.parseInt(value));
                    } else if ((value = attributes.getValue("double")) != null) {
                        currentNode.put(key, Double.parseDouble(value));
                    } else if ((value = attributes.getValue("boolean")) != null) {
                        currentNode.put(key, Boolean.parseBoolean(value));
                    } else if ((value = attributes.getValue("long")) != null) {
                        currentNode.put(key, Long.parseLong(value));
                    } else {
                        System.out.println("Error: Unsupported value for key "+key+", at line "+locator.getLineNumber()+".");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Error converting "+value+" to a number at line "+locator.getLineNumber()+": "+e.getMessage());
                }
            }

            if (DEBUG) {
                System.out.print("Read "+qName+", Attributes: ");
                for (int i=0; i<attributes.getLength(); i++) {
                    System.out.print("["+attributes.getQName(i)+"="+attributes.getValue(i)+"], ");
                }
                System.out.println();
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("node")) {
                // pop node context
                if (context.isEmpty()) {
                    throw parseException("Empty context, too many closing </> brackets");
                }
                lastPopped = context.pop();
            }

            if (DEBUG) {
                System.out.println("End "+qName);
            }
        }

        public ProjSettingsNode getRootNode() { return lastPopped; }

        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }
        private SAXParseException parseException(String msg) {
            return new SAXParseException(msg, locator);
        }
    }


    // ------------------------------ Testing -------------------------------

    public static void main(String args[]) {
        test();
    }

    public static void test() {
        write("/tmp/projsettings.xml", getSettings());
        ProjSettingsNode readNode = read("/tmp/projsettings.xml");
        if (getSettings().printDifferences(readNode)) {
            System.out.println("Node read does not match node written");
        } else {
            System.out.println("Node read matches node written");
        }
        // write-read a couple times, make sure no cumulative error in doubles
        write("/tmp/projsettings.xml", readNode);
        readNode = read("/tmp/projsettings.xml");
        write("/tmp/projsettings.xml", readNode);
        readNode = read("/tmp/projsettings.xml");
        write("/tmp/projsettings.xml", readNode);
        readNode = read("/tmp/projsettings.xml");

        if (getSettings().printDifferences(readNode)) {
            System.out.println("Node read does not match node written");
        } else {
            System.out.println("Node read matches node written");
        }
    }

    public static void test2() {
        ProjSettingsNode node = new ProjSettingsNode();
        node.putInteger("an int", 1);
        node.putString("a string", "this is a string");
        ProjSettingsNode node2 = new ProjSettingsNode();
        node.putNode("node2", node2);
        node2.putDouble("a double", 43.56);
        node2.putBoolean("a boolean", true);
        ProjSettingsNode node3 = new TestExtendNode();
        node2.putNode("extended node", node3);
        node3.putLong("a long", 302030049);
        node3.putString("a string", "some string");

        write("/tmp/projsettings.xml", node);
        ProjSettingsNode readNode = read("/tmp/projsettings.xml");
        if (node.printDifferences(readNode)) {
            System.out.println("Node read does not match node written");
        } else {
            System.out.println("Node read matches node written");
        }
    }

    protected static class TestExtendNode extends ProjSettingsNode {

    }
}
