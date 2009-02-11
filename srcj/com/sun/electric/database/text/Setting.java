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

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.tool.user.projectSettings.ProjSettings;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.prefs.Preferences;

/**
 *
 */
public class Setting {
    private static final HashMap<String,Setting> allSettingsByXmlPath = new HashMap<String,Setting>();
    private static final HashMap<String,Setting> allSettingsByPrefPath = new HashMap<String,Setting>();

    public static class Group {
        private final RootGroup root;
        public final String xmlPath;
        private final LinkedHashMap<String,Setting> groupSettings = new LinkedHashMap<String,Setting>();

        private Group(RootGroup root, String xmlPath) {
            this.root = root;
            this.xmlPath = xmlPath;
        }

        public Setting getSetting(String name) {
            return groupSettings.get(name);
        }

        public Setting makeBooleanSetting(String name, String curPrefGroup, String prefName, String location, String description, boolean factory) {
            return makeSetting(name, curPrefGroup, prefName, location, description, Boolean.valueOf(factory));
        }

        public Setting makeIntSetting(String name, String curPrefGroup, String prefName, String location, String description, int factory) {
            return makeSetting(name, curPrefGroup, prefName, location, description, Integer.valueOf(factory));
        }

        public Setting makeLongSetting(String name, String curPrefGroup, String prefName, String location, String description, long factory) {
            return makeSetting(name, curPrefGroup, prefName, location, description, Long.valueOf(factory));
        }

        public Setting makeDoubleSetting(String name, String curPrefGroup, String prefName, String location, String description, double factory) {
            return makeSetting(name, curPrefGroup, prefName, location, description, Double.valueOf(factory));
        }

        public Setting makeStringSetting(String name, String curPrefGroup, String prefName, String location, String description, String factory) {
            return makeSetting(name, curPrefGroup, prefName, location, description, factory);
        }

        private Setting makeSetting(String name, String curPrefGroup, String prefName, String location, String description, Object factory) {
            assert !root.locked;
            Setting setting = groupSettings.get(name);
            assert setting == null;
            setting = new Setting(prefName, Preferences.userRoot().node(curPrefGroup), xmlPath, name, location, description, factory);
            groupSettings.put(name, setting);
            return setting;
        }
    }

    public static class RootGroup {
        public LinkedHashMap<String,Group> allGroups = new LinkedHashMap<String,Group>();
        private boolean locked;

        public Group node(String groupName) {
            Group group = allGroups.get(groupName);
            if (group == null) {
                assert !locked;
                group = new Group(this, groupName + ".");
                allGroups.put(groupName, group);
            }
            return group;
        }

        public void lock() {
            locked = true;
        }
    }

//    private final ProjSettingsNode xmlNode;
//    private final String xmlName;
    private final String xmlPath;
	private final Object factoryObj;
    private Object currentObj;
    private final Preferences prefs;
    private final String prefName;
    public final String prefPath;
    private boolean valid;
    private final String description, location;
    private String [] trueMeaning;
    private static boolean lockCreation;

    /** Creates a new instance of Setting */
    public Setting(String prefName, Pref.Group group, String xmlNode, String xmlName, String location, String description, Object factoryObj) {
        this(prefName, group.preferences, xmlNode, xmlName, location, description, factoryObj);

    }
    /** Creates a new instance of Setting */
    private Setting(String prefName, Preferences preferences, String xmlNode, String xmlName, String location, String description, Object factoryObj) {
//        EDatabase.serverDatabase().checkChanging();
//        if (lockCreation)
//            throw new IllegalStateException();
        if (xmlNode == null)
            throw new NullPointerException();
//        this.xmlNode = xmlNode;
        if (xmlName == null)
            xmlName = prefName;
//        this.xmlName = xmlName;
        xmlPath = xmlNode + xmlName;
        assert !allSettingsByXmlPath.containsKey(xmlPath);

        this.factoryObj = factoryObj;
        currentObj = factoryObj;
        this.prefName = prefName;
        prefs = preferences;
        prefPath = prefs.absolutePath() + "/" + prefName;
        assert !allSettingsByPrefPath.containsKey(prefPath);

        allSettingsByXmlPath.put(xmlPath, this);
        allSettingsByPrefPath.put(prefPath, this);
        assert allSettingsByXmlPath.size() == allSettingsByPrefPath.size();

        valid = true;
        this.description = description;
        this.location = location;
        setCachedObjFromPreferences();
        ProjSettings.putValue(this);
//        xmlNode.putValue(xmlName, this);
    }

    /**
     * Currently Setting can be created only at initialization phase.
     * This method forbids further cration of Settings.
     */
    public static void lockCreation() {
        lockCreation = true;
    }

    /**
     * Method to get the boolean value on this Setting object.
     * The object must have been created as "boolean".
     * @return the boolean value on this TechSetting object.
     */
    public boolean getBoolean() { return ((Boolean)getValue()).booleanValue(); }

    /**
     * Method to get the integer value on this Setting object.
     * The object must have been created as "integer".
     * @return the integer value on this TechSetting object.
     */
    public int getInt() { return ((Integer)getValue()).intValue(); }

    /**
     * Method to get the long value on this Setting object.
     * The object must have been created as "long".
     * @return the long value on this TechSetting object.
     */
    public long getLong() { return ((Long)getValue()).longValue(); }

    /**
     * Method to get the double value on this Setting object.
     * The object must have been created as "double".
     * @return the double value on this TechSetting object.
     */
    public double getDouble() { return ((Double)getValue()).doubleValue(); }

    /**
     * Method to get the string value on this Setting object.
     * The object must have been created as "string".
     * @return the string value on this TechSetting object.
     */
    public String getString() { return (String)getValue(); }

    public void set(Object v) {
//        if (changeBatch != null) {
//            if (SwingUtilities.isEventDispatchThread()) {
//                if (!v.equals(getValue()))
//                    changeBatch.add(this, v);
//                return;
//            }
//            changeBatch = null;
//        }
        EDatabase.serverDatabase().checkChanging();
        if (getValue().equals(v)) return;
        if (v.getClass() != factoryObj.getClass())
            throw new ClassCastException();
        currentObj = factoryObj.equals(v) ? factoryObj : v;
        saveToPreferences(v);
        setSideEffect();
    }

    /**
	 * Method called when this Pref is changed.
	 * This method is overridden in subclasses that want notification.
	 */
    protected void setSideEffect() {
    }

     /**
     * Method to get the xml name of this Setting object.
     * @return the xml name of this Setting object.
     */
    public String getXmlPath() { return xmlPath; }

   /**
     * Method to get the name of this Setting object.
     * @return the name of this Setting object.
     */
    public String getPrefName() { return prefName; }

	/**
	 * Method to get the value of this Setting object as an Object.
	 * The proper way to get the current value is to use one of the type-specific
	 * methods such as getInt(), getBoolean(), etc.
	 * @return the Object value of this Setting object.
	 */
	public Object getValue() {
//        if (changeBatch != null) {
//            if (SwingUtilities.isEventDispatchThread()) {
//                Object pendingChange = changeBatch.changesForSettings.get(this);
//                if (pendingChange != null)
//                    return pendingChange;
//            }
//        }
        return currentObj;
    }

    /**
     * Method to return the user-command that can affect this Meaning option.
     * @return the user-command that can affect this Meaning option.
     */
    public String getLocation() { return location; }

    /**
     * Method to return the description of this Meaning option.
     * @return the Pref description of this Meaning option.
     */
    public String getDescription() { return description; }

    /**
     * Method to set whether this Meaning option is valid and should be reconciled.
     * Some should not, for example, the scale value on technologies that
     * don't use scaling (such as Schematics, Artwork, etc.)
     * @param valid true if this Meaning option is valid and should be reconciled.
     */
    public void setValidOption(boolean valid) { this.valid = valid; }

    /**
     * Method to tell whether this Meaning option is valid and should be reconciled.
     * Some should not, for example, the scale value on technologies that
     * don't use scaling (such as Schematics, Artwork, etc.)
     * @return true if this Meaning option is valid and should be reconciled.
     */
    public boolean isValidOption() { return valid; }

    /**
     * Method to associate an array of strings to be used for integer Meaning options.
     * @param trueMeaning the array of strings that should be used for this integer Meaning option.
     * Some options are multiple-choice, for example the MOSIS CMOS rule set which can be
     * 0, 1, or 2 depending on whether the set is SCMOS, Submicron, or Deep.
     * By giving an array of 3 strings to this method, a proper description of the option
     * can be given to the user.
     */
    public void setTrueMeaning(String [] trueMeaning) { this.trueMeaning = trueMeaning; }

    /**
     * Method to return an array of strings to be used for integer Meaning options.
     * Some options are multiple-choice, for example the MOSIS CMOS rule set which can be
     * 0, 1, or 2 depending on whether the set is SCMOS, Submicron, or Deep.
     * By giving an array of 3 strings to this method, a proper description of the option
     * can be given to the user.
     * @return the array of strings that should be used for this integer Meaning option.
     */
    public String [] getTrueMeaning() { return trueMeaning; }

	/**
	 * Method to get the factory-default value of this Pref object.
	 * @return the factory-default value of this Pref object.
	 */
	public Object getFactoryValue() { return factoryObj; }

	/**
	 * Method to get the factory-default double value of this Pref object.
	 * @return the factory-default double value of this Pref object.
	 */
	public double getDoubleFactoryValue() { return ((Double)factoryObj).doubleValue(); }

	/**
	 * Factory methods to create a boolean project setting objects.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param location the user-command that can affect this meaning option.
	 * @param description the description of this meaning option.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
    public static Setting makeBooleanSetting(String name, Pref.Group group,
                                          String xmlNode, String xmlName,
                                          String location, String description, boolean factory) {
        Setting setting = Setting.getSetting(xmlNode + xmlName);
        if (setting != null) return setting;
        return new Setting(name, group, xmlNode, xmlName, location, description, Boolean.valueOf(factory));
    }

	/**
	 * Factory methods to create an integerproject setting objects.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param location the user-command that can affect this meaning option.
	 * @param description the description of this meaning option.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
    public static Setting makeIntSetting(String name, Pref.Group group,
                                      String xmlNode, String xmlName,
                                      String location, String description, int factory) {
        Setting setting = Setting.getSetting(xmlNode + xmlName);
        if (setting != null) return setting;
        return new Setting(name, group, xmlNode, xmlName, location, description, Integer.valueOf(factory));
    }

	/**
	 * Factory methods to create a long project setting objects.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param location the user-command that can affect this meaning option.
	 * @param description the description of this meaning option.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
    public static Setting makeLongSetting(String name, Pref.Group group,
                                       String xmlNode, String xmlName,
                                       String location, String description, long factory) {
        Setting setting = Setting.getSetting(xmlNode + xmlName);
        if (setting != null) return setting;
        return new Setting(name, group, xmlNode, xmlName, location, description, Long.valueOf(factory));
    }

	/**
	 * Factory methods to create a double project setting objects.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param location the user-command that can affect this meaning option.
	 * @param description the description of this meaning option.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
    public static Setting makeDoubleSetting(String name, Pref.Group group,
                                         String xmlNode, String xmlName,
                                         String location, String description, double factory) {
        Setting setting = Setting.getSetting(xmlNode + xmlName);
        if (setting != null) return setting;
        return new Setting(name, group, xmlNode, xmlName, location, description, Double.valueOf(factory));
    }

	/**
	 * Factory methods to create a string project setting objects.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param location the user-command that can affect this meaning option.
	 * @param description the description of this meaning option.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
    public static Setting makeStringSetting(String name, Pref.Group group,
                                         String xmlNode, String xmlName,
                                         String location, String description, String factory) {
        Setting setting = Setting.getSetting(xmlNode + xmlName);
        if (setting != null) return setting;
        return new Setting(name, group, xmlNode, xmlName, location, description, factory);
    }

	/**
	 * Method to find the project Setting object by its xml path.
	 * @param xmlPath the xml path of the desired project Setting object.
	 * @return the project Setting object.
	 */
    public static Setting getSetting(String xmlPath) { return allSettingsByXmlPath.get(xmlPath); }

	/**
	 * Method to find the project Setting object by its pref path.
	 * @param prefPath the pref path of the desired project Setting object.
	 * @return the project Setting object.
	 */
    public static Setting getSettingByPrefPath(String prefPath) { return allSettingsByPrefPath.get(prefPath); }

    public static Comparator<Setting> SETTINGS_BY_PREF_NAME = new Comparator<Setting> () {
        public int compare(Setting s1, Setting s2) {
            String n1 = s1.getPrefName();
            String n2 = s2.getPrefName();
            return n1.compareTo(n2);
        }
    };

//    /**
//     * Comparator class for sorting Preferences by their name.
//     */
//    private static final Comparator<Setting> SettingsByName = new Comparator<Setting>() {
//        /**
//         * Method to sort Setting by their prefName.
//         */
//        public int compare(Setting setting1, Setting setting2) {
//            String s1 = setting1.getPrefName();
//            String s2 = setting2.getPrefName();
//            return s1.compareToIgnoreCase(s2);
//        }
//    };

	/**
	 * Method to adjust project that were saved with a library.
	 * Presents the user with a dialog to help reconcile the difference
	 * between project settings stored in a library and the original values.
	 */
	public static Map<Setting,Object> reconcileSettings(Map<Setting,Object> projectSettings)
	{
        HashSet<Setting> markedSettings = new HashSet<Setting>();
		Map<Setting,Object> settingsToReconcile = new HashMap<Setting,Object>();
        for (Map.Entry<Setting,Object> e: projectSettings.entrySet()) {
            Setting setting = e.getKey();
            Object value = e.getValue();
            markedSettings.add(setting);
            if (DBMath.objectsReallyEqual(value, setting.getValue())) continue;
            if (!setting.isValidOption()) continue;
            settingsToReconcile.put(setting, value);
        }
        for (Setting setting: allSettingsByXmlPath.values()) {
            if (markedSettings.contains(setting)) continue;

            // this one is not mentioned in the library: make sure it is at factory defaults
            if (DBMath.objectsReallyEqual(setting.getValue(), setting.getFactoryValue())) continue;

            if (!setting.isValidOption()) continue;
            settingsToReconcile.put(setting, null);
        }
        return settingsToReconcile;
	}

    /**
     * This method is called after reconciling project settings with OptionReconcile dialog or in a batch mode
     */
    public static void finishSettingReconcilation(Map<Setting,Object> settingsToReconcile) {
        // delay flushing of preferences until all chanages are made
        Pref.delayPrefFlushing();
        for (Map.Entry<Setting,Object> e: settingsToReconcile.entrySet()) {
            Setting setting = e.getKey();
            Object obj = e.getValue();
            if (obj == null)
                obj = setting.factoryObj;

            // set the option
            if (obj.getClass() != setting.factoryObj.getClass()) {
                if (obj instanceof Integer && setting.factoryObj instanceof Boolean)
                    obj = Boolean.valueOf(((Integer)obj).intValue() != 0);
                else if (obj instanceof Float && setting.factoryObj instanceof Double)
                    obj = Double.valueOf(((Float)obj).doubleValue());
                else
                    continue;
            }
            setting.set(obj);
            System.out.println("Project Setting "+setting.xmlPath+" changed to "+obj);
        }

        // resume flushing, and save everything just set
        Pref.resumePrefFlushing();
    }

    static void saveAllSettingsToPreferences() {
        for (Setting setting: allSettingsByXmlPath.values()) {
            Object value = setting.getValue();
            setting.saveToPreferences(value);
        }
    }

    void saveToPreferences(Object v) {
        assert v.getClass() == factoryObj.getClass();
        if (v.equals(factoryObj)) {
             prefs.remove(prefName);
             return;
        }
        if (v instanceof Boolean)
            prefs.putBoolean(prefName, ((Boolean)v).booleanValue());
        else if (v instanceof Integer)
            prefs.putInt(prefName, ((Integer)v).intValue());
        else if (v instanceof Long)
            prefs.putLong(prefName, ((Long)v).longValue());
        else if (v instanceof Double)
            prefs.putDouble(prefName, ((Double)v).doubleValue());
        else if (v instanceof String)
            prefs.put(prefName, (String)v);
        else {
            assert false;
        }
    }

    private void setCachedObjFromPreferences() {
        Object cachedObj = null;
        if (factoryObj instanceof Boolean) {
            cachedObj = Boolean.valueOf(prefs.getBoolean(prefName, ((Boolean)factoryObj).booleanValue()));
        } else if (factoryObj instanceof Integer) {
            cachedObj = Integer.valueOf(prefs.getInt(prefName, ((Integer)factoryObj).intValue()));
        } else if (factoryObj instanceof Long) {
            cachedObj = Long.valueOf(prefs.getLong(prefName, ((Long)factoryObj).longValue()));
        } else if (factoryObj instanceof Double) {
            cachedObj = Double.valueOf(prefs.getDouble(prefName, ((Double)factoryObj).doubleValue()));
        } else if (factoryObj instanceof String) {
            cachedObj = prefs.get(prefName, (String)factoryObj);
        }
        assert cachedObj != null;
        this.currentObj = cachedObj;
    }

    public static class SettingChangeBatch implements Serializable {
        public HashMap<String,Object> changesForSettings = new HashMap<String,Object>();

        public void add(Setting setting, Object newValue) {
            changesForSettings.put(setting.xmlPath, newValue);
        }
    }

	/**
	 * Method to make a collection of project settings changes.
	 * In order to make project settings changes on the server,
	 * it is necessary to gather them on the client, and send
	 * the changes to the server for actual change.
	 * This method runs on the server.
	 * @param batch the collection of project setting changes.
	 */
    public static void implementSettingChanges(SettingChangeBatch batch) {
        for (Map.Entry<String,Object> e: batch.changesForSettings.entrySet()) {
            String xmlPath = e.getKey();
            Object newValue = e.getValue();
            Setting setting = getSetting(xmlPath);
            setting.set(newValue);
        }
    }

//    public static List<Object> getContext() { return new ArrayList<Object>(values); }
    public static Map<Setting,Object> resetContext() {
        HashMap<Setting,Object> savedContext = new HashMap<Setting,Object>();
        for (Setting setting: allSettingsByXmlPath.values()) {
            savedContext.put(setting, setting.getValue());
            setting.set(setting.getFactoryValue());
        }
        return savedContext;
    }
    public  static void restoreContext(Map<Setting,Object> savedContext) {
        for (Map.Entry<Setting,Object> e: savedContext.entrySet()) {
            Setting setting = e.getKey();
            setting.set(e.getValue());
        }
    }
    public static Collection<Setting> getSettings() { return allSettingsByXmlPath.values(); }

    static void printAllSettings(PrintStream out) {
        TreeMap<String,Setting> sortedSettings = new TreeMap<String,Setting>();
        for (Setting setting: allSettingsByXmlPath.values())
            sortedSettings.put(setting.xmlPath, setting);
        out.println("PROJECT SETTINGS");
        int i = 0;
        for (Setting setting: sortedSettings.values())
            out.println((i++) + "\t" + setting.xmlPath + " " + setting.getValue());
//        printSettings(out, ProjSettings.getSettings(), 0);
    }

//    private static int i;
//
//    private static void printSettings(PrintStream out, ProjSettingsNode node, int level) {
//        Set<String> keys = node.getKeys();
//        for (String key: keys) {
//            out.print((i++) + "\t");
//            for (int i = 0; i < level; i++) out.print("  ");
//            out.print(key);
//            ProjSettingsNode subNode = node.getNode(key);
//            if (subNode != null) {
//                out.println(".");
//                printSettings(out, subNode, level + 1);
//                continue;
//            }
//            Setting setting = node.getValue(key);
//            out.println(" " + setting.prefName + " " + setting.getValue());
//        }
//   }
}
