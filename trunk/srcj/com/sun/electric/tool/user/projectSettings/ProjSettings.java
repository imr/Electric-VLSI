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
package com.sun.electric.tool.user.projectSettings;

import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Setting;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.OpenFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Stack;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;



/**
 * User: gainsley
 * Date: Jul 25, 2006
 */
public class ProjSettings {

    private static ProjSettings settings;
    private ProjSettingsNode rootNode = new ProjSettingsNode();
    private File lastProjectSettingsFile = null;
    private static final String entry = "e";

    public static ProjSettings getSettings() {
        return settings;
    }

    public void putValue(String xmlPath, Object value) {
        int pos;
        ProjSettingsNode node = rootNode;
        while ((pos = xmlPath.indexOf('.')) >= 0) {
            String key = xmlPath.substring(0, pos);
            node = node.getNode(key);
            xmlPath = xmlPath.substring(pos + 1);
        }
        node.data.put(xmlPath, value);
    }

    public Object getValue(String xmlPath) {
        int pos;
        ProjSettingsNode node = rootNode;
        while ((pos = xmlPath.indexOf('.')) >= 0) {
            String key = xmlPath.substring(0, pos);
            Object n = node.data.get(key);
            if (!(n instanceof ProjSettingsNode))
                return null;
            node = (ProjSettingsNode)n;
            xmlPath = xmlPath.substring(pos + 1);
        }
        Object v = node.data.get(xmlPath);
        if (v instanceof ProjSettingsNode)
            return null;
        return v;
    }

    public void putAllSettings(Map<Setting,Object> settings) {
        for (Map.Entry<Setting,Object> e: settings.entrySet()) {
            Setting setting = e.getKey();
            Object value = e.getValue();
            putValue(setting.getXmlPath(), value);
        }
    }

    public static void writeSettings(Map<Setting,Object> addSettings, File file) {
        if (settings == null)
            settings = new ProjSettings();
        settings.putAllSettings(addSettings);
        settings.write(file.getPath(), settings.rootNode);
    }

    public static File getLastProjectSettingsFile() {
        return settings != null ? settings.lastProjectSettingsFile : null;
    }

    /**
     * Read project preferences and apply them
     * @param file the file to read
     * @param allowOverride true to allow overriding current settings,
     * false to disallow and warn if different.
     */
    public static void readSettings(File file, EDatabase database, boolean allowOverride) {
        ProjSettings newSettings = read(file);
        if (newSettings == null) return;
        System.out.println("Read Project Preferences from "+file);
        if (settings == null) allowOverride = true;
        if (newSettings.commit(database, allowOverride) && settings != null) {
            File oldFile = settings.lastProjectSettingsFile;
            String message = "Warning: Project Preferences conflicts;" + (allowOverride ? "" : " ignoring new settings.") + " See messages window";
            Job.getUserInterface().showInformationMessage(message, "Project Preferences Conflict");
            System.out.println("Project Preferences conflicts found: "+oldFile.getPath()+" vs "+file.getPath());
        }
        settings = newSettings;
    }

    private boolean commit(EDatabase database, boolean allowOverride) {
        Setting.SettingChangeBatch commitBatch = new Setting.SettingChangeBatch();
        for (Map.Entry<Setting,Object> e: database.getSettings().entrySet()) {
            Setting setting = e.getKey();
            Object oldVal = e.getValue();
            Object xmlVal = getValue(setting.getXmlPath());
            if (xmlVal == null)
                xmlVal = setting.getFactoryValue();
            if (xmlVal.equals(oldVal)) continue;
            if (xmlVal.getClass() != oldVal.getClass()) {
                System.out.println("Setting type mismatch " + setting);
                continue;
            }
            commitBatch.add(setting, xmlVal);
            // Don't print mismatch caused by rounding error
            if (xmlVal instanceof Double && ((Double)xmlVal).floatValue() == ((Double)oldVal).floatValue())
                continue;
            if (allowOverride) {
                System.out.println("Warning: Setting \""+setting.getPrefName()+"\" set to \""+xmlVal+
                    "\", overrides current value of \""+oldVal + "\"");
            } else {
                System.out.println("Warning: Setting \""+setting.getPrefName()+"\" retains current value of \""+oldVal+
                    "\", while ignoring projectsettings.xml value of \""+xmlVal+"\"");
            }
        }
        if (allowOverride)
            database.implementSettingChanges(commitBatch);
        return !commitBatch.changesForSettings.isEmpty();
    }

    /**
     * Write settings to disk.  Pops up a dialog to prompt for file location
     */
    public static void exportSettings() {
/*
        int ret = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(), "Changes to Project Preferences may affect all users\n\nContinue Anyway?",
                "Warning!", JOptionPane.YES_NO_OPTION);
        if (ret == JOptionPane.NO_OPTION) return;
*/
        File outputFile = new File(FileType.LIBRARYFORMATS.getGroupPath(), "projsettings.xml");
        String ofile = OpenFile.chooseOutputFile(FileType.XML, "Export Project Preferences", outputFile.getPath());
        if (ofile == null) return;
        outputFile = new File(ofile);

        writeSettings(EDatabase.clientDatabase().getSettings(), outputFile);
    }

    public static void importSettings() {
        String ifile = OpenFile.chooseInputFile(FileType.XML, "Import Project Preferences", false, FileType.LIBRARYFORMATS.getGroupPath(), false);
        if (ifile == null) return;
        new ImportSettingsJob(ifile);
    }

    /**
     * Class to read a library in a new thread.
     * For a non-interactive script, use ReadLibrary job = new ReadLibrary(filename, format).
     */
    public static class ImportSettingsJob extends Job {
        private String fileName;

        public ImportSettingsJob(String fileName) {
            super("Import Settings", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.fileName = fileName;
            startJob();
        }

        @Override
        public boolean doIt() throws JobException {
            readSettings(new File(fileName), getDatabase(), true);
            return true;
        }

        @Override
        public void terminateOK() {
            getDatabase().getEnvironment().saveToPreferences();
        }
    }

    // ------------------------- ProjSettings Writer ----------------------------

    public void write(String file) {
        write(file, rootNode);
    }

    private void write(String file, ProjSettingsNode node) {
        File ofile = new File(file);
        Writer wr = new Writer(ofile);
        if (wr.write("ProjectSettings", node)) {
            System.out.println("Wrote Project Preferences to "+file);
            lastProjectSettingsFile = ofile;
        }
        wr.close();
    }

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
                System.out.println("ERROR: recursive loop in Project Preferences; "+
                        "ignoring second instantiation of ProjSettingsNode \""+name+"\".");
                return;
            }
            visited.add(node);

            String classDef = "";
/*
            if (node.getClass() != ProjSettingsNode.class) {
                classDef = " class=\""+node.getClass().getName()+"\">";
            }
*/

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
            } else {
                writeValue(key, value);
            }
        }

        private void writeValue(String key, Object value) {
            if (value instanceof Integer) {
                prIndent("<"+entry+" key=\""+key+"\"\t int=\""+value.toString()+"\" />");
            } else if (value instanceof Double) {
                prIndent("<"+entry+" key=\""+key+"\"\t double=\""+value.toString()+"\" />");
            } else if (value instanceof Long) {
                prIndent("<"+entry+" key=\""+key+"\"\t long=\""+value.toString()+"\" />");
            } else if (value instanceof Boolean) {
                prIndent("<"+entry+" key=\""+key+"\"\t boolean=\""+value.toString()+"\" />");
            } else if (value instanceof String) {
                String str = (String)value;
                prIndent("<"+entry+" key=\""+key+"\"\t string=\""+replaceSpecialChars(str)+"\" />");
            } else {
                prIndent("<"+entry+" key=\""+key+"\"\t string=\""+value+"\" />");
            }
        }

        private void close() {
            if (file != null) out.close();
        }
    }

    private static String replaceSpecialChars(String str) {
        str = str.replace("&", "&amp;");
        str = str.replace("=", "&eq;");
        str = str.replace(">", "&gt;");
        str = str.replace("<", "&lt;");
        str = str.replace("'", "&apos;");
        str = str.replace("\"", "&quot;");
        str = str.replace("\n", "&#xA;");
        return str;
    }

    // ------------------------- ProjSettings Reader ----------------------------

    public static ProjSettings read(File file) {
        Reader rd = new Reader(file);
        return rd.read();
    }

    private static class Reader extends DefaultHandler {
        private URL url;
        private Stack<ProjSettingsNode> context;
        private Locator locator;
        private ProjSettings settings = new ProjSettings();

        private static final boolean DEBUG = false;

        private Reader(File file) {
            this.url = TextUtils.makeURLToFile(file.getPath());
            settings.lastProjectSettingsFile = file;
            this.context = new Stack<ProjSettingsNode>();
            this.locator = null;
        }

        private ProjSettings read() {
            try {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setValidating(true);
                factory.setNamespaceAware(true);
                URLConnection conn = url.openConnection();
                InputStream is = conn.getInputStream();
                factory.newSAXParser().parse(is, this);
                return settings;
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
            return null;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {

            // get the name of the key
            String key = attributes.getValue("key");
            if (key == null) {
                throw parseException("Entry "+qName+" is missing \"key\" attribute");
            }

            // see if this is a project preferences node
            if (qName.equals("node")) {
                if (context.isEmpty()) {
                    // first node is root node
                    context.push(settings.rootNode);
                } else {
                    ProjSettingsNode node = context.peek().getNode(key);
                    if (node == null)
                        System.out.println("Error: No Project Preferences Node named "+key+" in Electric");
                    else
                        context.push(node);
                }
            } else if (qName.equals(entry)) {

                // must be a key-value pair
                if (context.isEmpty()) {
                    throw parseException("No node for key-value pair "+qName);
                }
                ProjSettingsNode currentNode = context.peek();
                String valueStr = null;
                Object value = null;
                try {
                    if ((valueStr = attributes.getValue("string")) != null)
                        value = valueStr;
                    else if ((valueStr = attributes.getValue("int")) != null)
                        value = Integer.valueOf(valueStr);
                    else if ((valueStr = attributes.getValue("double")) != null)
                        value = Double.valueOf(valueStr);
                    else if ((valueStr = attributes.getValue("boolean")) != null)
                        value = Boolean.valueOf(valueStr);
                    else if ((valueStr = attributes.getValue("long")) != null)
                        value = Long.valueOf(valueStr);
                    else
                        System.out.println("Error: Unsupported value for key "+key+", at line "+locator.getLineNumber()+": must be string, int, double, boolean, or long");
                } catch (NumberFormatException e) {
                    System.out.println("Error converting "+valueStr+" to a number at line "+locator.getLineNumber()+": "+e.getMessage());
                }
                if (value != null)
                    currentNode.put(key, value);
            }

            if (DEBUG) {
                System.out.print("Read "+qName+", Attributes: ");
                for (int i=0; i<attributes.getLength(); i++) {
                    System.out.print("["+attributes.getQName(i)+"="+attributes.getValue(i)+"], ");
                }
                System.out.println();
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("node")) {
                // pop node context
                if (context.isEmpty()) {
                    throw parseException("Empty context, too many closing </> brackets");
                }
                context.pop();
            }
            if (DEBUG) {
                System.out.println("End "+qName);
            }
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }
        private SAXParseException parseException(String msg) {
            return new SAXParseException(msg, locator);
        }
    }

    public static String describeContext(Stack<String> context) {
        StringBuffer buf = new StringBuffer();
        boolean first = true;
        for (String name : context) {
            if (first)
                first = false;
            else
                buf.append(".");
            buf.append(name);
        }
        if (buf.length() == 0) return "RootContext";
        return buf.toString();
    }

    // ------------------------------ Testing -------------------------------

    public static void main(String args[]) {
        test();
    }

    public static void test() {
//        settings.write("/tmp/projsettings.xml", settings.rootNode);
//        readSettings(new File("/tmp/projsettings.xml"), false);
    }
}
