/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Pref.java
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
package com.sun.electric.database.text;

import com.sun.electric.Main;
import com.sun.electric.database.id.LibId;
import com.sun.electric.tool.Client;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ActivityLogger;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.SAXParseException;

/**
 * This class manages appearance options.
 * There are two types of options: <I>appearance</I> and <I>meaning</I>.
 * Appearance options affect the way that the design is presented to the user.
 * Examples are grid dot spacing, layer stipple patterns, etc.
 * Meaning options affect the way that a design is produced (for fabrication,
 * simulation, and other outputs).  Examples are CIF layer names, technology
 * options, etc.)
 * Meaning options are managed by Setting class now.
 * <P>
 * Some Prefs can be used in server Jobs, other are client only.
 * Server Prefs are created by special factory methods during Electric initialization.
 * They can't be created later.
 * <P>
 * All options are saved in a machine-specific way by the Java Preferences class.
 * In addition, "meaning" options are stored in libraries.  When the libraries
 * are read back into Electric, the stored meaning options are checked against
 * the current meaning options, the two are reconciled.
 * <P>
 * Where are these options stored?  It varies with each operating system.
 * <UL>
 * <LI><B>Windows:</B>
 *   In the registry.
 *   Look in: HKEY_CURRENT_USER / Software / JavaSoft / Prefs / com / sun / electric.
 * </LI>
 * <LI><B>UNIX/Linux:</B>
 *   In your home directory.
 *   Look in: ~/.java/.userPrefs/com/sun/electric
 * </LI>
 * <LI><B>Macintosh System 10:</B>
 *   In your home directory, under Library/Preferences.
 *   Look at: ~/Library/Preferences/com.sun.electric.plist
 * </LI>
 * </UL>
 */
public class Pref {

    public final static boolean FROM_THREAD_ENVIRONMENT = false;
    private static boolean forbidPreferences;

    public static class Group {

        private final String relativePath;
        private boolean lockCreation;
        Preferences preferences;
        private final TreeMap<String, Pref> prefs = new TreeMap<String, Pref>();

        private Group(String relativePath) {
            this.relativePath = relativePath;
        }

        public String relativePath() {
            return relativePath;
        }

        public Collection<Pref> getPrefs() {
            return Collections.unmodifiableCollection(prefs.values());
        }

        /**
         * Prefs can be created only at initialization phase.
         * This method forbids further cration of Prefs.
         */
        public void lockCreation() {
            lockCreation = true;
        }

        public void setCachedObjsFromPreferences() {
            for (Pref pref : prefs.values()) {
                pref.setCachedObjFromPreferences();
            }
        }

        private void putValue(String key, Object value) {
            if (preferences == null) {
                preferences = getPrefRoot().node(relativePath);
            }
            putValue(preferences, key, value);
            if (doFlushing) {
                flushOptions(preferences);
            } else {
                queueForFlushing.add(preferences);
            }
        }

        private void putValue(Preferences preferences, String key, Object value) {
            if (value instanceof Boolean) {
                preferences.putBoolean(key, ((Boolean) value).booleanValue());
            } else if (value instanceof Integer) {
                preferences.putInt(key, ((Integer) value).intValue());
            } else if (value instanceof Long) {
                preferences.putLong(key, ((Long) value).longValue());
            } else if (value instanceof Double) {
                preferences.putDouble(key, ((Double) value).doubleValue());
            } else {
                assert value instanceof String;
                preferences.put(key, (String) value);
            }
        }

        private Object getValue(String key, Object def) {
            if (preferences == null) {
                preferences = getPrefRoot().node(relativePath);
            }
            return getValueImpl(preferences, key, def);
        }

        private void remove(String key) {
            if (preferences == null) {
                preferences = getPrefRoot().node(relativePath);
            }
            remove(preferences, key);
            if (doFlushing) {
                flushOptions(preferences);
            } else {
                queueForFlushing.add(preferences);
            }

        }

        private void remove(Preferences preferences, String key) {
            preferences.remove(key);
        }
    }

    public static Group groupForPackage(Class classFromPackage) {

        String className = classFromPackage.getName();
        String mainClassName = "com.sun.electric.";
        if (!className.startsWith(mainClassName)) {
            throw new IllegalArgumentException("Class is not in Electric tree");
        }
        className = className.substring(mainClassName.length());
        int pkgEndIndex = className.lastIndexOf('.');
        String packageName = className.substring(0, pkgEndIndex);
        return groupForPackage(packageName.replace('.', '/'));
    }

    public static Group groupForPackage(String relativePath) {
        synchronized (allGroups) {
            for (Group group : allGroups) {
                if (group.relativePath.equals(relativePath)) {
                    return group;
                }
            }
            Group newGroup = new Group(relativePath);
            allGroups.add(newGroup);
            return newGroup;
        }
    }
    private final String name;
    private final Group group;
    private final boolean serverAccessible;
    private Object cachedObj;
    private Object factoryObj;
    private static final ArrayList<Group> allGroups = new ArrayList<Group>();
    private static boolean doFlushing = true;
    private static Set<Preferences> queueForFlushing;
    private static boolean lockCreation;
    private static final HashSet<Pref> reportedAccess = new HashSet<Pref>();

    /**
     * The constructor for the Pref.
     * @param name the name of this Pref.
     */
    protected Pref(Group group, String name, boolean serverAccessible, Object factoryObj) {
        if (lockCreation && serverAccessible) {
            throw new IllegalStateException("Pref " + group.relativePath() + "/" + name + " is created from improper place");
        }
        this.name = name;
        this.group = group;
        this.serverAccessible = serverAccessible;
        this.factoryObj = factoryObj;
        synchronized (group.prefs) {
            assert !group.prefs.containsKey(name);
            group.prefs.put(name, this);
        }
//        setCachedObjFromPreferences();
    }

    /**
     * Currently Setting can be created only at initialization phase.
     * This method forbids further cration of Settings.
     */
    public static void lockCreation() {
        lockCreation = true;
    }

    public static void forbidPreferences() {
        forbidPreferences = true;
    }

    public static void setCachedObjsFromPreferences() {
        synchronized (allGroups) {
            for (Group group : allGroups) {
                group.setCachedObjsFromPreferences();
            }
        }
    }

    public static void importPrefs(URL fileURL) {
        // import preferences
        try {
            URLConnection urlCon = fileURL.openConnection();
            InputStream inputStream = urlCon.getInputStream();

            // reset all preferences to factory values
            try {
                clearPrefs(getPrefRoot());
            } catch (BackingStoreException e) {
                System.out.println("Error resetting Electric preferences");
                e.printStackTrace();
            }

            // import preferences
            Preferences.importPreferences(inputStream);
            inputStream.close();

        } catch (InvalidPreferencesFormatException e) {
            String message = "Invalid preferences format";
            if (e.getCause() instanceof SAXParseException) {
                SAXParseException se = (SAXParseException) e.getCause();
                message += " (line " + se.getLineNumber() + ")";
            }
            System.out.println(message + ": " + e.getMessage());
            return;
        } catch (IOException e) {
            System.out.println("Error reading preferences file");
            e.printStackTrace();
            return;
        }
    }

    private static void clearPrefs(Preferences topNode) throws BackingStoreException {
        topNode.clear();
        for (String child : topNode.childrenNames()) {
            clearPrefs(topNode.node(child));
        }
    }

    /**
     * Method to export the preferences to an XML file. This function is public due to the regressions.
     * @param fileName the file to write.
     */
    public static void exportPrefs(String fileName) {
        exportPrefs(fileName, getPrefRoot());
    }

    /**
     * Method to export the preferences to an XML file. This function is public due to the regressions.
     * @param fileName the file to write.
     */
    public static void exportPrefs(String fileName, Preferences prefRoot) {
        if (fileName == null) {
            return;
        }

        // save preferences there
        try {
            // dump the preferences as a giant XML string (unformatted)
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            prefRoot.exportSubtree(bs);
            String xmlDump = bs.toString();

            // remove the DTD statement (causes trouble)
            int sunPos = xmlDump.indexOf("java.sun.com");
            String insertDTD = "";
            if (sunPos >= 0) {
                int openPos = xmlDump.lastIndexOf('<', sunPos);
                int closePos = xmlDump.indexOf('>', sunPos);
                if (openPos >= 0 && closePos >= 0) {
                    insertDTD = xmlDump.substring(openPos, closePos + 1);
                    xmlDump = xmlDump.substring(0, openPos) + xmlDump.substring(closePos + 1);
                }
            }

            // reformat the XML
            StreamSource source = new StreamSource(new StringReader(xmlDump));
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setAttribute("indent-number", new Integer(2));
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            OutputStreamWriter outSW = (Client.isOSMac()) ? new OutputStreamWriter(bos) : new OutputStreamWriter(bos, "utf-8");
//            OutputStreamWriter outSW = new OutputStreamWriter(bos, "UTF-8");
            StreamResult result = new StreamResult(outSW);
            transformer.transform(source, result);

            // add the removed DTD line back into the XML
            String xmlFormatted = bos.toString();
            int closePos = xmlFormatted.indexOf('>');
            if (closePos >= 0) {
                xmlFormatted = xmlFormatted.substring(0, closePos + 1) + "\n" + insertDTD + xmlFormatted.substring(closePos + 1);
            }

            // save the XML to disk
            PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
            printWriter.print(xmlFormatted);
            printWriter.close();
        } catch (Exception e) {
            if (Job.getDebug()) {
                e.printStackTrace();
            }
            System.out.println("Error exporting Preferences");
            return;
        }

        System.out.println("Preferences saved to " + fileName);
    }

    /**
     * Factory methods to create a boolean Pref objects.
     * @param name the name of this Pref.
     * @param group group of preferences to which a new Pref belongs
     * @param factory the "factory" default value (if nothing is stored).
     */
    public static Pref makeBooleanPref(String name, Group group, boolean factory) {
        return new Pref(group, name, false, Boolean.valueOf(factory));
    }

    /**
     * Factory methods to create an integer Pref objects.
     * @param name the name of this Pref.
     * @param group group of preferences to which a new Pref belongs
     * @param factory the "factory" default value (if nothing is stored).
     */
    public static Pref makeIntPref(String name, Group group, int factory) {
        return new Pref(group, name, false, Integer.valueOf(factory));
    }

    /**
     * Factory methods to create a long Pref objects.
     * @param name the name of this Pref.
     * @param group group of preferences to which a new Pref belongs
     * @param factory the "factory" default value (if nothing is stored).
     */
    public static Pref makeLongPref(String name, Group group, long factory) {
        return new Pref(group, name, false, Long.valueOf(factory));
    }

    /**
     * Factory methods to create a double Pref objects.
     * @param name the name of this Pref.
     * @param group group of preferences to which a new Pref belongs
     * @param factory the "factory" default value (if nothing is stored).
     */
    public static Pref makeDoublePref(String name, Group group, double factory) {
        return new Pref(group, name, false, Double.valueOf(factory));
    }

    /**
     * Factory methods to create a string Pref objects.
     * @param name the name of this Pref.
     * @param group group of preferences to which a new Pref belongs
     * @param factory the "factory" default value (if nothing is stored).
     */
    public static Pref makeStringPref(String name, Group group, String factory) {
        if (factory == null) {
            throw new NullPointerException();
        }
        return new Pref(group, name, false, factory);
    }

    /**
     * Factory methods to create a boolean Pref objects.
     * The Pref is accessible from server Jobs.
     * @param name the name of this Pref.
     * @param group group of preferences to which a new Pref belongs
     * @param factory the "factory" default value (if nothing is stored).
     */
    public static Pref makeBooleanServerPref(String name, Group group, boolean factory) {
        return new Pref(group, name, true, Boolean.valueOf(factory));
    }

    /**
     * Factory methods to create an integer Pref objects.
     * The Pref is accessible from server Jobs.
     * @param name the name of this Pref.
     * @param group group of preferences to which a new Pref belongs
     * @param factory the "factory" default value (if nothing is stored).
     */
    public static Pref makeIntServerPref(String name, Group group, int factory) {
        return new Pref(group, name, true, Integer.valueOf(factory));
    }

    /**
     * Factory methods to create a string Pref objects.
     * The Pref is accessible from server Jobs.
     * @param name the name of this Pref.
     * @param group group of preferences to which a new Pref belongs
     * @param factory the "factory" default value (if nothing is stored).
     */
    public static Pref makeStringServerPref(String name, Group group, String factory) {
        if (factory == null) {
            throw new NullPointerException();
        }
        return new Pref(group, name, true, factory);
    }

    /**
     * Method to get the boolean value on this Pref object.
     * The object must have been created as "boolean".
     * @return the boolean value on this Pref object.
     */
    public boolean getBoolean() {
        return ((Boolean) getValue()).booleanValue();
    }

    /**
     * Method to get the integer value on this Pref object.
     * The object must have been created as "integer".
     * @return the integer value on this Pref object.
     */
    public int getInt() {
        return ((Integer) getValue()).intValue();
    }

    /**
     * Method to get the long value on this Pref object.
     * The object must have been created as "long".
     * @return the long value on this Pref object.
     */
    public long getLong() {
        return ((Long) getValue()).longValue();
    }

    /**
     * Method to get the double value on this Pref object.
     * The object must have been created as "double".
     * @return the double value on this Pref object.
     */
    public double getDouble() {
        return ((Number) getValue()).doubleValue();
    }

    /**
     * Method to get the string value on this Pref object.
     * The object must have been created as "string".
     * @return the string value on this Pref object.
     */
    public String getString() {
        return (String) getValue();
    }

    /**
     * Method to get the boolean value on this Pref object.
     * The object must have been created as "boolean".
     * @param prefRoot root Preferences node
     * @return the boolean value on this Pref object.
     */
    public boolean getBoolean(Preferences prefRoot) {
        return ((Boolean) getValue(prefRoot)).booleanValue();
    }

    /**
     * Method to get the integer value on this Pref object.
     * The object must have been created as "integer".
     * @param prefRoot root Preferences node
     * @return the integer value on this Pref object.
     */
    public int getInt(Preferences prefRoot) {
        return ((Integer) getValue(prefRoot)).intValue();
    }

    /**
     * Method to get the long value on this Pref object.
     * The object must have been created as "long".
     * @param prefRoot root Preferences node
     * @return the long value on this Pref object.
     */
    public long getLong(Preferences prefRoot) {
        return ((Long) getValue(prefRoot)).longValue();
    }

    /**
     * Method to get the double value on this Pref object.
     * The object must have been created as "double".
     * @param prefRoot root Preferences node
     * @return the double value on this Pref object.
     */
    public double getDouble(Preferences prefRoot) {
        return ((Double) getValue(prefRoot)).doubleValue();
    }

    /**
     * Method to get the string value on this Pref object.
     * The object must have been created as "string".
     * @param prefRoot root Preferences node
     * @return the string value on this Pref object.
     */
    public String getString(Preferences prefRoot) {
        return (String) getValue(prefRoot);
    }

    /**
     * Method to get the factory-default value of this Pref object.
     * @return the factory-default value of this Pref object.
     */
    public Object getFactoryValue() {
        return factoryObj;
    }

    /**
     * Method to get the factory-default boolean value of this Pref object.
     * @return the factory-default boolean value of this Pref object.
     */
    public boolean getBooleanFactoryValue() {
        return ((Boolean) factoryObj).booleanValue();
    }

    /**
     * Method to get the factory-default integer value of this Pref object.
     * @return the factory-default integer value of this Pref object.
     */
    public int getIntFactoryValue() {
        return ((Integer) factoryObj).intValue();
    }

    /**
     * Method to get the factory-default long value of this Pref object.
     * @return the factory-default long value of this Pref object.
     */
    public long getLongFactoryValue() {
        return ((Long) factoryObj).longValue();
    }

    /**
     * Method to get the factory-default double value of this Pref object.
     * @return the factory-default double value of this Pref object.
     */
    public double getDoubleFactoryValue() {
        return ((Double) factoryObj).doubleValue();
    }

    /**
     * Method to get the factory-default String value of this Pref object.
     * @return the factory-default String value of this Pref object.
     */
    public String getStringFactoryValue() {
        return (String) factoryObj;
    }

    /**
     * Method to get the name of this Pref object.
     * @return the name of this Pref object.
     */
    public String getPrefName() {
        return name;
    }

    /**
     * Method to get the pref name of this Pref object.
     * @return the name of this Setting object.
     */
    public String getPrefPath() {
        return group.relativePath() + "/" + name;
    }

    /**
     * Method to get the value of this Pref object as an Object.
     * The proper way to get the current value is to use one of the type-specific
     * methods such as getInt(), getBoolean(), etc.
     * @return the Object value of this Pref object.
     */
    public Object getValue() {
        if (!Job.isClientThread()) {
            if (!serverAccessible && !reportedAccess.contains(this)) {
                String msg = getPrefName() + " is accessed from " + Job.getRunningJob();
                if (Job.getDebug()) {
                    ActivityLogger.logMessage(msg);
                    System.out.println(msg);
                }
                reportedAccess.add(this);
            }
        }
        if (cachedObj == null) {
            setCachedObjFromPreferences();
        }
        return cachedObj;
    }

    public Object getValue(Preferences prefRoot) {
        return getValueImpl(prefRoot.node(group.relativePath), name, factoryObj);
    }

    private static Object getValueImpl(Preferences preferences, String key, Object def) {
        Object value = def;
        if (def instanceof Boolean) {
            boolean defV = ((Boolean) def).booleanValue();
            boolean v = preferences.getBoolean(key, defV);
            if (v != defV) {
                value = Boolean.valueOf(v);
            }
        } else if (def instanceof Integer) {
            int defV = ((Integer) def).intValue();
            int v = preferences.getInt(key, defV);
            if (v != defV) {
                value = Integer.valueOf(v);
            }
        } else if (def instanceof Long) {
            long defV = ((Long) def).longValue();
            long v = preferences.getLong(key, defV);
            if (v != defV) {
                value = Long.valueOf(v);
            }
        } else if (def instanceof Double) {
            double defV = ((Double) def).doubleValue();
            double v = preferences.getDouble(key, defV);
            if (v != defV) {
                value = Double.valueOf(v);
            }
        } else {
            assert def instanceof String;
            value = preferences.get(key, (String) def);
        }
        return value;
    }

    private void setValue(Object value) {
        assert value.getClass() == factoryObj.getClass();
        cachedObj = value.equals(factoryObj) ? factoryObj : value;
        group.putValue(name, cachedObj);
    }

    private void setCachedObjFromPreferences() {
        cachedObj = group.getValue(name, factoryObj);
    }

    /**
     * Method to delay the saving of preferences to disk.
     * Since individual saving is time-consuming, batches of preference
     * changes are wrapped with this, and "resumePrefFlushing()".
     */
    public static void delayPrefFlushing() {
        doFlushing = false;
        queueForFlushing = new HashSet<Preferences>();
    }

    /**
     * Method to resume the saving of preferences to disk.
     * Since individual saving is time-consuming, batches of preference
     * changes are wrapped with this, and "resumePrefFlushing()".
     * Besides resuming saving, this method also saves anything queued
     * while saving was delayed.
     */
    public static void resumePrefFlushing() {
        doFlushing = true;
        for (Preferences p : queueForFlushing) {
            flushOptions(p);
        }
        flushAll();
    }

    /**
     * Returns the root of Preferences subtree with Electric options.
     * Currently this is "/com/sun/electric/" subtree.
     * @return the root of Preferences subtree with Electric options.
     */
    public static Preferences getPrefRoot() {
        if (forbidPreferences) {
            return getFactoryPrefRoot();
        }
        return Preferences.userNodeForPackage(Main.class);
    }

    /**
     * Returns the root of Preferences subtree with Electric options for a specified LibId.
     * @param libId specified LibId
     * @return the root of Preferences subtree with Electric options for a spiceified LibId.
     */
    public static Preferences getLibraryPreferences(LibId libId) {
        return Pref.getPrefRoot().node("database/hierarchy/" + libId.libName);
    }

    /**
     * Returns the root of dummy Preferences subtree with factory default Electric options.
     * @return the root of Preferences subtree with factory default Electric options.
     */
    public static Preferences getFactoryPrefRoot() {
        return EmptyPreferencesFactory.factoryPrefRoot;
    }

    /**
     * Method to immediately flush all Electric preferences to disk.
     */
    public static void flushAll() {
        flushOptions(getPrefRoot());
    }

    /**
     * Method to set a new boolean value on this Pref object.
     * @param v the new boolean value of this Pref object.
     */
    public void setBoolean(boolean v) {
        checkModify();
        boolean cachedBool = getBoolean();
        if (v != cachedBool) {
            setValue(Boolean.valueOf(v));
        }
    }

    /**
     * Method to set a new integer value on this Pref object.
     * @param v the new integer value of this Pref object.
     */
    public void setInt(int v) {
        checkModify();
        int cachedInt = getInt();
        if (v != cachedInt) {
            setValue(Integer.valueOf(v));
        }
    }

    /**
     * Method to set a new long value on this Pref object.
     * @param v the new long value of this Pref object.
     */
    public void setLong(long v) {
        checkModify();
        long cachedLong = getLong();
        if (v != cachedLong) {
            setValue(Long.valueOf(v));
        }
    }

    /**
     * Method to set a new double value on this Pref object.
     * @param v the new double value of this Pref object.
     */
    public void setDouble(double v) {
        checkModify();
        double cachedDouble = getDouble();
        if (v != cachedDouble) {
            setValue(Double.valueOf(v));
        }
    }

    /**
     * Method to set a new string value on this Pref object.
     * @param str the new string value of this Pref object.
     */
    public void setString(String str) {
        checkModify();
        String cachedString = getString();
        if (!str.equals(cachedString)) {
            setValue(str);
        }
    }

    /**
     * Method to set a new boolean value on this Pref object.
     * @param v the new boolean value of this Pref object.
     */
    public void putBoolean(Preferences prefRoot, boolean removeDefaults, boolean v) {
        Preferences prefs = prefRoot.node(group.relativePath);
        if (removeDefaults && v == ((Boolean) factoryObj).booleanValue()) {
            prefs.remove(name);
        } else {
            prefs.putBoolean(name, v);
        }
    }

    /**
     * Method to set a new integer value on this Pref object.
     * @param v the new integer value of this Pref object.
     */
    public void putInt(Preferences prefRoot, boolean removeDefaults, int v) {
        Preferences prefs = prefRoot.node(group.relativePath);
        if (removeDefaults && v == ((Integer) factoryObj).intValue()) {
            prefs.remove(name);
        } else {
            prefs.putInt(name, v);
        }
    }

    /**
     * Method to set a new long value on this Pref object.
     * @param v the new long value of this Pref object.
     */
    public void putLong(Preferences prefRoot, boolean removeDefaults, long v) {
        Preferences prefs = prefRoot.node(group.relativePath);
        if (removeDefaults && v == ((Long) factoryObj).longValue()) {
            prefs.remove(name);
        } else {
            prefs.putLong(name, v);
        }
    }

    /**
     * Method to set a new double value on this Pref object.
     * @param v the new double value of this Pref object.
     */
    public void putDouble(Preferences prefRoot, boolean removeDefaults, double v) {
        Preferences prefs = prefRoot.node(group.relativePath);
        if (removeDefaults && v == ((Double) factoryObj).doubleValue()) {
            prefs.remove(name);
        } else {
            prefs.putDouble(name, v);
        }
    }

    /**
     * Method to set a new string value on this Pref object.
     * @param str the new string value of this Pref object.
     */
    public void putString(Preferences prefRoot, boolean removeDefaults, String str) {
        Preferences prefs = prefRoot.node(group.relativePath);
        if (removeDefaults && str.equals(factoryObj)) {
            prefs.remove(name);
        } else {
            prefs.put(name, str);
        }
    }

    /**
     * Method to reset Pref value to factory default.
     */
    public void factoryReset() {
        if (!Job.isClientThread()) {
            throw new IllegalStateException();
        }
        cachedObj = factoryObj;
        group.remove(name);
    }

    private void checkModify() {
        if (!Job.isClientThread()) {
            if (!serverAccessible) {
                String msg = getPrefName() + " is modified in " + Job.getRunningJob();
                if (Job.getDebug()) {
                    ActivityLogger.logMessage(msg);
                    System.out.println(msg);
                }
            }
        }
    }

    public static Collection<Group> getAllGroups() {
        return Collections.unmodifiableCollection(allGroups);
    }

//    private static int numStrings;
//    private static int lenStrings;
//    private static int numValueStrings = 0;
//    private static int lenValueStrings = 0;
//
//    public static void printAllPrefs(PrintStream out, Environment environment, boolean all) {
//        numValueStrings = lenValueStrings = 0;
//        TreeMap<String,Pref> sortedPrefs = new TreeMap<String,Pref>();
//        synchronized (allGroups) {
//            for (Group group: allGroups) {
//                for (Pref pref: group.prefs.values()) {
//                    if (!all && !pref.serverAccessible) continue;
//                    sortedPrefs.put(pref.group.relativePath() + "/" + pref.name, pref);
//                }
//            }
//        }
//        Preferences rootNode = getPrefRoot();
//        numStrings = lenStrings = 0;
//        try {
//            gatherPrefs(out, 0, rootNode, null);
//        } catch (BackingStoreException e) {
//            e.printStackTrace();
//        }
//        out.println(lenStrings + " chars in " + numStrings + " strings");
//        out.println(lenValueStrings + " chars in " + numValueStrings + " value strings");
//        printAllSettings(out, environment);
//        out.println("ELECTRIC USER PREFERENCES");
//        int  i = 0;
//        for (Pref pref: sortedPrefs.values())
//            out.println((i++) + pref.group.relativePath() + " " + pref.name + " " + pref.cachedObj);
//        for (Technology tech: environment.techPool.values()) {
//            i = 0;
//            for (Pref.Group group: tech.getTechnologyAllPreferences()) {
//                for (Pref pref: group.prefs.values()) {
//                    if (!all && !pref.serverAccessible) continue;
//                    out.println((i++) + pref.group.relativePath() + " " + tech + " " + pref.name + " " + pref.cachedObj);
//                }
//            }
//        }
//    }
//
//    private static void gatherPrefs(PrintStream out, int level, Preferences topNode, List<String> ks) throws BackingStoreException {
//        for (int i = 0; i < level; i++) out.print("  ");
//        String[] keys = topNode.keys();
//        for (int i = 0; i < keys.length; i++) {
//            numStrings++;
//            lenStrings += keys.length;
//            String value = topNode.get(keys[i], null);
//            numValueStrings++;
//            lenValueStrings += value.length();
//        }
//        out.println(topNode.name() + " " + keys.length);
//        if (topNode.absolutePath().equals("/com/sun/electric/database/hierarchy")) return;
//        String[] children = topNode.childrenNames();
//        for (int i = 0; i < children.length; i++) {
//            String childName = children[i];
//            numStrings++;
//            lenStrings += children[i].length();
//            Preferences childNode = topNode.node(childName);
//            gatherPrefs(out, level + 1, childNode, ks);
//        }
//    }
//
//    private static void printAllSettings(PrintStream out, Environment environment) {
//        Map<Setting,Object> settings = environment.getSettings();
//        TreeMap<String,Setting> sortedSettings = new TreeMap<String,Setting>();
//        for (Setting setting: settings.keySet())
//            sortedSettings.put(setting.getXmlPath(), setting);
//        out.println("PROJECT PREFERENCES");
//        int i = 0;
//        for (Setting setting: sortedSettings.values())
//            out.println((i++) + "\t" + setting.getXmlPath() + " " + settings.get(setting));
//    }
    /****************************** private methods ******************************/
    /**
     * Method to force all Preferences to be saved.
     */
    private static void flushOptions(Preferences p) {
        try {
            p.flush();
        } catch (Exception e) {
            System.out.println("Failed to save preferences");
        }
    }
}
