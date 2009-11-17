/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Setting.java
 *
 * Copyright (c) 2007 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.Environment;
import com.sun.electric.database.id.IdReader;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.database.variable.Variable;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.prefs.Preferences;

/**
 * This class manages meaning options.
 * There are two types of options: <I>appearance</I> and <I>meaning</I>.
 * Appearance options affect the way that the design is presented to the user.
 * Meaning options affect the way that a design is produced (for fabrication,
 * simulation, and other outputs).  Examples are CIF layer names, technology
 * options, etc.)
 * Settings are grouped in a Setting Trees. Each Tree consists of a RootGroup and lower Groups.
 */
public class Setting {

    /**
     * This class manages a group of Settings.
     */
    public static class Group {

        public final String xmlPath;
        private final RootGroup root;
        private final LinkedHashMap<String, Group> children = new LinkedHashMap<String, Group>();
        private final LinkedHashMap<String, Setting> settings = new LinkedHashMap<String, Setting>();

        private Group(RootGroup root, String xmlPath) {
            this.root = root;
            this.xmlPath = xmlPath;
        }

        private Group() {
            root = (RootGroup) this;
            xmlPath = "";
        }

        /**
         * Returns subnode with specified node name
         * @param nodeName simple node name
         * @return subnode with specified node name
         * @throws IllegalStateException if the Setting
         */
        public Group node(String nodeName) {
            assert nodeName.indexOf('.') == -1;
            if (nodeName.length() == 0) {
                return this;
            }
            Group child = children.get(nodeName);
            if (child == null) {
                if (root.isLocked()) {
                    throw new IllegalStateException();
                }
                child = new Group(root, xmlPath + nodeName + '.');
                children.put(nodeName, child);
            }
            return child;
        }

        /**
         * Dot-spearated path from the Root of the tree to this Group
         * @return path from the Root of the tree to this Group
         */
        public String getXmlPath() {
            return xmlPath;
        }

        /**
         * Factory methods to create a boolean project preferences objects.
         * @param prefName preference name of this Setting.
         * @param prefGroup preference Group of this Setting.
         * @param xmlName Xml name of this Setting.
         * @param location the user-command that can affect this meaning option.
         * @param description the description of this meaning option.
         * @param factory the "factory" default value (if nothing is stored).
         */
        public Setting makeBooleanSetting(String prefName, String prefGroup, String xmlName,
                String location, String description, boolean factory) {
            return new Setting(prefName, prefGroup, this, xmlName, location, description, Boolean.valueOf(factory));
        }

        /**
         * Factory methods to create an integer project preferences objects.
         * @param prefName preference name of this Setting.
         * @param prefGroup preference Group of this Setting.
         * @param xmlName Xml name of this Setting.
         * @param location the user-command that can affect this meaning option.
         * @param description the description of this meaning option.
         * @param factory the "factory" default value (if nothing is stored).
         */
        public Setting makeIntSetting(String prefName, String prefGroup, String xmlName,
                String location, String description, int factory, String... trueMeaning) {
            return new Setting(prefName, prefGroup, this, xmlName, location, description, Integer.valueOf(factory), trueMeaning);
        }

        /**
         * Factory methods to create a long project preferences objects.
         * @param prefName preference name of this Setting.
         * @param prefGroup preference Group of this Setting.
         * @param xmlName Xml name of this Setting.
         * @param location the user-command that can affect this meaning option.
         * @param description the description of this meaning option.
         * @param factory the "factory" default value (if nothing is stored).
         */
        public Setting makeLongSetting(String prefName, String prefGroup, String xmlName,
                String location, String description, long factory) {
            return new Setting(prefName, prefGroup, this, xmlName, location, description, Long.valueOf(factory));
        }

        /**
         * Factory methods to create a double project preferences objects.
         * @param prefName preference name of this Setting.
         * @param prefGroup preference Group of this Setting.
         * @param xmlName Xml name of this Setting.
         * @param location the user-command that can affect this meaning option.
         * @param description the description of this meaning option.
         * @param factory the "factory" default value (if nothing is stored).
         */
        public Setting makeDoubleSetting(String prefName, String prefGroup, String xmlName,
                String location, String description, double factory) {
            return new Setting(prefName, prefGroup, this, xmlName, location, description, Double.valueOf(factory));
        }

        /**
         * Factory methods to create a string project preferences objects.
         * @param prefName preference name of this Setting.
         * @param prefGroup preference Group of this Setting.
         * @param xmlName Xml name of this Setting.
         * @param location the user-command that can affect this meaning option.
         * @param description the description of this meaning option.
         * @param factory the "factory" default value (if nothing is stored).
         */
        public Setting makeStringSetting(String prefName, String prefGroup, String xmlName,
                String location, String description, String factory) {
            return new Setting(prefName, prefGroup, this, xmlName, location, description, factory);
        }

        /**
         * Returns Setting from this Group or a subgroup by its relative path
         * @param xmlPath dot-separated relative path
         * @return Setting by relative path or null
         */
        public Setting getSetting(String xmlPath) {
            int pos = xmlPath.indexOf('.');
            if (pos < 0) {
                return settings.get(xmlPath);
            }
            Group child = children.get(xmlPath.substring(0, pos));
            if (child == null) {
                return null;
            }
            return child.getSetting(xmlPath.substring(pos + 1));
        }

        /**
         * Returns all Settings from this Group and its subgroups
         * @return all Settings from this Group and its subgroups
         */
        public Collection<Setting> getSettings() {
            ArrayList<Setting> list = new ArrayList<Setting>();
            gatherSettings(list);
            return list;
        }

        private void gatherSettings(ArrayList<Setting> list) {
            list.addAll(settings.values());
            for (Group child : children.values()) {
                child.gatherSettings(list);
            }
        }

        /**
         * Method to get a list of project preferences from this Group
         * which should be written to disk libraries
         * @return a collection of project preferences
         */
        public Map<Setting, Object> getDiskSettings(Map<Setting, Object> settingValues) {
            Map<Setting, Object> result = new TreeMap<Setting, Object>(SETTINGS_BY_PREF_NAME);
            for (Setting setting : getSettings()) {
                Object value = settingValues.get(setting);
                if (!setting.isValidOption()) {
                    continue;
                }
                if (value.equals(setting.getFactoryValue())) {
                    continue;
                }
                result.put(setting, value);
            }
            return result;
        }

        @Override
        public String toString() {
            return xmlPath;
        }

        void write(IdWriter writer) throws IOException {
            writer.writeInt(settings.size());
            for (Map.Entry<String, Setting> e : settings.entrySet()) {
                String key = e.getKey();
                Setting setting = e.getValue();
                writer.writeString(key);
                setting.writeSetting(writer);
            }
            writer.writeInt(children.size());
            for (Map.Entry<String, Group> e : children.entrySet()) {
                String key = e.getKey();
                Group child = e.getValue();
                writer.writeString(key);
                child.write(writer);
            }
        }

        void read(IdReader reader) throws IOException {
            int numSettings = reader.readInt();
            for (int i = 0; i < numSettings; i++) {
                String key = reader.readString();
                Setting.readSetting(reader, this, key);
            }
            int numChildren = reader.readInt();
            for (int i = 0; i < numChildren; i++) {
                String key = reader.readString();
                Group child = node(key);
                child.read(reader);
            }
        }
    }

    /**
     * This class manages a tree of Settings.
     */
    public static class RootGroup extends Group {

        private boolean locked;

        /**
         * Constructs a root of empty tree of Settings
         */
        public RootGroup() {
        }

        /**
         * Returns empty locked RootGroup
         * @return empty locked RootGroup
         */
        public static RootGroup newEmptyGroup() {
            RootGroup rootGroup = new RootGroup();
            rootGroup.lock();
            return rootGroup;
        }

        /**
         * Returns true if tree can't be modified anymore
         * @return true if tree is locked
         */
        public boolean isLocked() {
            return locked;
        }

        /**
         * Locks the tree
         */
        public void lock() {
            locked = true;
        }

        /**
         * Writes this Tree of Settings to IdManager writer
         * @param writer IdManager writer
         * @throws java.io.IOException om writer error
         */
        @Override
        public void write(IdWriter writer) throws IOException {
            super.write(writer);
        }
    }

    /**
     * Reads a Tree of Settings fro, IdManager reader
     * @param reader IdManager reader
     * @throws java.io.IOException om reader error
     */
    public static RootGroup read(IdReader reader) throws IOException {
        RootGroup root = new RootGroup();
        root.read(reader);
        root.lock();
        return root;
    }
    private final Group xmlGroup;
    private final String xmlPath;
    private final Object factoryObj;
    private final String prefNode;
    private final String prefName;
    private boolean valid;
    private final String description, location;
    private final String[] trueMeaning;

    /** Creates a new instance of Setting */
    protected Setting(String prefName, String prefGroup, Group xmlGroup, String xmlName,
            String location, String description, Object factoryObj, String... trueMeaning) {
        if (xmlGroup.root.isLocked()) {
            throw new IllegalStateException();
        }
        if (xmlGroup == null) {
            throw new NullPointerException();
        }
        if (xmlName == null) {
            xmlName = prefName;
        }
        assert xmlName.length() > 0;
        assert xmlName.indexOf('.') == -1;
        assert !xmlGroup.settings.containsKey(xmlName);
        xmlPath = xmlGroup + xmlName;

        this.xmlGroup = xmlGroup;
        this.factoryObj = factoryObj;
        this.prefName = prefName;
        prefNode = prefGroup;

        xmlGroup.settings.put(xmlName, this);

        valid = true;
        this.description = description;
        this.location = location;
        this.trueMeaning = trueMeaning != null && trueMeaning.length > 0 ? trueMeaning.clone() : null;
    }

    /**
     * Method to get the boolean value on this Setting object.
     * The object must have been created as "boolean".
     * @return the boolean value on this TechSetting object.
     */
    public boolean getBoolean() {
        return ((Boolean) getValue()).booleanValue();
    }

    /**
     * Method to get the integer value on this Setting object.
     * The object must have been created as "integer".
     * @return the integer value on this TechSetting object.
     */
    public int getInt() {
        return ((Integer) getValue()).intValue();
    }

    /**
     * Method to get the long value on this Setting object.
     * The object must have been created as "long".
     * @return the long value on this TechSetting object.
     */
    public long getLong() {
        return ((Long) getValue()).longValue();
    }

    /**
     * Method to get the double value on this Setting object.
     * The object must have been created as "double".
     * @return the double value on this TechSetting object.
     */
    public double getDouble() {
        return ((Double) getValue()).doubleValue();
    }

    /**
     * Method to get the string value on this Setting object.
     * The object must have been created as "string".
     * @return the string value on this TechSetting object.
     */
    public String getString() {
        String s = (String) getValue();
        if (s == null) {
            throw new NullPointerException();
        }
        return s;
    }

    /**
     * Method to get the value of this Setting object as an Object.
     * The proper way to get the current value is to use one of the type-specific
     * methods such as getInt(), getBoolean(), etc.
     * @return the Object value of this Setting object.
     */
    public Object getValue() {
        return Environment.getThreadEnvironment().getValue(this);
    }

    @Override
    public String toString() {
        return getXmlPath();
    }

    /**
     * Method to get the xml name of this Setting object.
     * @return the xml name of this Setting object.
     */
    public String getXmlPath() {
        return xmlPath;
    }

    /**
     * Method to get the name of this Setting object.
     * @return the name of this Setting object.
     */
    public String getPrefName() {
        return prefName;
    }

    /**
     * Method to get the pref name of this Setting object.
     * @return the name of this Setting object.
     */
    public String getPrefPath() {
        return prefNode + "/" + prefName;
    }

    /**
     * Method to return the user-command that can affect this Meaning option.
     * @return the user-command that can affect this Meaning option.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Method to return the description of this Meaning option.
     * @return the Pref description of this Meaning option.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Method to set whether this Meaning option is valid and should be reconciled.
     * Some should not, for example, the scale value on technologies that
     * don't use scaling (such as Schematics, Artwork, etc.)
     * @param valid true if this Meaning option is valid and should be reconciled.
     */
    public void setValidOption(boolean valid) {
        if (xmlGroup.root.isLocked()) {
            throw new IllegalStateException();
        }
        this.valid = valid;
    }

    /**
     * Method to tell whether this Meaning option is valid and should be reconciled.
     * Some should not, for example, the scale value on technologies that
     * don't use scaling (such as Schematics, Artwork, etc.)
     * @return true if this Meaning option is valid and should be reconciled.
     */
    public boolean isValidOption() {
        return valid;
    }

    /**
     * Method to return an array of strings to be used for integer Meaning options.
     * Some options are multiple-choice, for example the MOSIS CMOS rule set which can be
     * 0, 1, or 2 depending on whether the set is SCMOS, Submicron, or Deep.
     * By giving an array of 3 strings to this method, a proper description of the option
     * can be given to the user.
     * @return the array of strings that should be used for this integer Meaning option.
     */
    public String[] getTrueMeaning() {
        return trueMeaning != null ? trueMeaning.clone() : null;
    }

    /**
     * Method to get the factory-default value of this Pref object.
     * @return the factory-default value of this Pref object.
     */
    public Object getFactoryValue() {
        return factoryObj;
    }

    /**
     * Method to get the factory-default double value of this Pref object.
     * @return the factory-default double value of this Pref object.
     */
    public double getDoubleFactoryValue() {
        return ((Double) factoryObj).doubleValue();
    }
    private static Comparator<Setting> SETTINGS_BY_PREF_NAME = new Comparator<Setting>() {

        public int compare(Setting s1, Setting s2) {
            String n1 = s1.getPrefName();
            String n2 = s2.getPrefName();
            return n1.compareTo(n2);
        }
    };

    public void saveToPreferences(Preferences prefRoot, Object v) {
        assert v.getClass() == factoryObj.getClass();
        Preferences prefs = prefRoot.node(prefNode);
        if (v.equals(factoryObj)) {
            prefs.remove(prefName);
            return;
        }
        if (v instanceof Boolean) {
            prefs.putBoolean(prefName, ((Boolean) v).booleanValue());
        } else if (v instanceof Integer) {
            prefs.putInt(prefName, ((Integer) v).intValue());
        } else if (v instanceof Long) {
            prefs.putLong(prefName, ((Long) v).longValue());
        } else if (v instanceof Double) {
            prefs.putDouble(prefName, ((Double) v).doubleValue());
        } else if (v instanceof String) {
            prefs.put(prefName, (String) v);
        } else {
            assert false;
        }
    }

    public Object getValueFromPreferences(Preferences prefRoot) {
        Preferences prefs = prefRoot.node(prefNode);
        Object cachedObj = null;
        if (factoryObj instanceof Boolean) {
            cachedObj = Boolean.valueOf(prefs.getBoolean(prefName, ((Boolean) factoryObj).booleanValue()));
        } else if (factoryObj instanceof Integer) {
            cachedObj = Integer.valueOf(prefs.getInt(prefName, ((Integer) factoryObj).intValue()));
        } else if (factoryObj instanceof Long) {
            cachedObj = Long.valueOf(prefs.getLong(prefName, ((Long) factoryObj).longValue()));
        } else if (factoryObj instanceof Double) {
            cachedObj = Double.valueOf(prefs.getDouble(prefName, ((Double) factoryObj).doubleValue()));
        } else if (factoryObj instanceof String) {
            cachedObj = prefs.get(prefName, (String) factoryObj);
        }
        assert cachedObj != null;
        return cachedObj;
    }

    private void writeSetting(IdWriter writer) throws IOException {
        // xmlPath
        Variable.writeObject(writer, factoryObj);
        writer.writeString(prefNode);
        writer.writeString(prefName);
        writer.writeBoolean(valid);
        writer.writeString(description);
        writer.writeString(location);
        boolean hasTrueMeaning = trueMeaning != null;
        writer.writeBoolean(hasTrueMeaning);
        if (hasTrueMeaning) {
            writer.writeInt(trueMeaning.length);
            for (String s : trueMeaning) {
                writer.writeString(s);
            }
        }
    }

    private static Setting readSetting(IdReader reader, Group group, String xmlName) throws IOException {
        Object factoryObj = Variable.readObject(reader);
        String prefGroup = reader.readString();
        String prefName = reader.readString();
        boolean valid = reader.readBoolean();
        String description = reader.readString();
        String location = reader.readString();
        String[] trueMeaning = null;
        boolean hasTrueMeaning = reader.readBoolean();
        if (hasTrueMeaning) {
            trueMeaning = new String[reader.readInt()];
            for (int i = 0; i < trueMeaning.length; i++) {
                trueMeaning[i] = reader.readString();
            }
        }
        Setting setting = new Setting(prefName, prefGroup, group, xmlName, location, description, factoryObj, trueMeaning);
        setting.setValidOption(valid);
        return setting;
    }

    public static class SettingChangeBatch implements Serializable {

        public HashMap<String, Object> changesForSettings = new HashMap<String, Object>();

        public void add(Setting setting, Object newValue) {
            changesForSettings.put(setting.xmlPath, newValue);
        }
    }
}
