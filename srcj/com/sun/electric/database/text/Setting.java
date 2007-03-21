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
package com.sun.electric.database.text;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.projectSettings.ProjSettings;
import com.sun.electric.tool.user.projectSettings.ProjSettingsNode;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;

/**
 *
 */
public class Setting {
    private static final HashMap<String,Setting> allSettingsByXmlPath = new HashMap<String,Setting>();
    private static final HashMap<String,Setting> allSettingsByPrefPath = new HashMap<String,Setting>();
    private static final ArrayList<Object> values = new ArrayList<Object>();
    
    private final ProjSettingsNode xmlNode;
    private final String xmlName;
    private final String xmlPath;
	private final Object factoryObj;
    private final Preferences prefs;
    private final String prefName;
    private final String prefPath;
    private final int index;
    private boolean valid;
    private final String description, location;
    private String [] trueMeaning;
    
    /** Creates a new instance of Setting */
    public Setting(String prefName, Pref.Group group, ProjSettingsNode xmlNode, String xmlName, String location, String description, Object factoryObj) {
        EDatabase.theDatabase.checkChanging();
        if (xmlNode == null)
            throw new NullPointerException();
        this.xmlNode = xmlNode;
        if (xmlName == null)
            xmlName = prefName;
        this.xmlName = xmlName;
        xmlPath = xmlNode.getPath() + xmlName;
        assert !allSettingsByXmlPath.containsKey(xmlPath);
        
        this.factoryObj = factoryObj;
        this.prefName = prefName;
        prefs = group.prefs;
        prefPath = prefs.absolutePath() + "/" + prefName;
        assert !allSettingsByPrefPath.containsKey(prefPath);
        
        index = values.size();
        values.add(factoryObj);
        allSettingsByXmlPath.put(xmlPath, this);
        allSettingsByPrefPath.put(prefPath, this);
        assert allSettingsByXmlPath.size() == allSettingsByPrefPath.size();
        assert allSettingsByXmlPath.size() == values.size();
        
        valid = true;
        this.description = description;
        this.location = location;
        if (xmlName == null) xmlName = getPrefName();
        xmlNode.putValue(xmlName, this);
        
        setCachedObjFromPreferences();
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
    
    /**
     * Method to get the boolean value on this Setting object in a specified context.
     * The object must have been created as "boolean".
     * @param context specified context.
     * @return the boolean value on this Setting object in the context.
     */
    public boolean getBoolean(List<Object> context) { return ((Boolean)getValue(context)).booleanValue(); }
    
    /**
     * Method to get the integer value on this Setting object in a specified context.
     * The object must have been created as "integer".
     * @param context specified context.
     * @return the integer value on this TechSetting object.
     */
    public int getInt(List<Object> context) { return ((Integer)getValue(context)).intValue(); }
    
    /**
     * Method to get the long value on this Setting object in a specified context.
     * The object must have been created as "long".
     * @param context specified context.
     * @return the long value on this TechSetting object.
     */
    public long getLong(List<Object> context) { return ((Long)getValue(context)).longValue(); }
    
    /**
     * Method to get the double value on this Setting object in a specified context.
     * The object must have been created as "double".
     * @param context specified context.
     * @return the double value on this TechSetting object.
     */
    public double getDouble(List<Object> context) { return ((Double)getValue(context)).doubleValue(); }
    
    /**
     * Method to get the string value on this Setting object in a specified context.
     * The object must have been created as "string".
     * @param context specified context.
     * @return the string value on this TechSetting object.
     */
    public String getString(List<Object> context) { return (String)getValue(context); }
    
    /**
     * Method to set a new boolean value on this TechSetting object.
     * @param v the new boolean value of this TechSetting object.
     */
    public void setBoolean(boolean v) {
        if (v != getBoolean())
            set(Boolean.valueOf(v));
    }
    
    /**
     * Method to set a new integer value on this TechSetting object.
     * @param v the new integer value of this TechSetting object.
     */
    public void setInt(int v) {
        if (v != getInt())
            set(Integer.valueOf(v));
     }
    
    /**
     * Method to set a new long value on this TechSetting object.
     * @param v the new long value of this TechSetting object.
     */
    public void setLong(long v) {
        if (v != getInt())
            set(Long.valueOf(v));
    }
    
    /**
     * Method to set a new double value on this TechSetting object.
     * @param v the new double value of this Pref object.
     */
    public void setDouble(double v) {
        if (v != getDouble())
            set(Double.valueOf(v));
    }
    
    /**
     * Method to set a new string value on this TechSetting object.
     * @param str the new string value of this TechSetting object.
     */
    public void setString(String str) {
        if (!str.equals(getString()))
            set(str);
    }
    
    public void set(Object v) {
//        if (changeBatch != null) {
//            if (SwingUtilities.isEventDispatchThread()) {
//                if (!v.equals(getValue()))
//                    changeBatch.add(this, v);
//                return;
//            }
//            changeBatch = null;
//        }
        EDatabase.theDatabase.checkChanging();
        if (getValue().equals(v)) return;
        if (v.getClass() != factoryObj.getClass())
            throw new ClassCastException();
        values.set(index, factoryObj.equals(v) ? factoryObj : v);
        saveToPreferences(v);
        setSideEffect();
    }
    
    /**
     * Method to set a new boolean value on this TechSetting object.
     * @param v the new boolean value of this TechSetting object.
     */
    public void setBoolean(List<Object> context, boolean v) {
        if (v != getBoolean(context))
            set(context, Boolean.valueOf(v));
    }
    
    /**
     * Method to set a new integer value on this TechSetting object.
     * @param v the new integer value of this TechSetting object.
     */
    public void setInt(List<Object> context, int v) {
        if (v != getInt(context))
            set(context, Integer.valueOf(v));
     }
    
    /**
     * Method to set a new long value on this TechSetting object.
     * @param v the new long value of this TechSetting object.
     */
    public void setLong(List<Object> context, long v) {
        if (v != getInt(context))
            set(context, Long.valueOf(v));
    }
    
    /**
     * Method to set a new double value on this TechSetting object.
     * @param v the new double value of this Pref object.
     */
    public void setDouble(List<Object> context, double v) {
        if (v != getDouble(context))
            set(context, Double.valueOf(v));
    }
    
    /**
     * Method to set a new string value on this TechSetting object.
     * @param str the new string value of this TechSetting object.
     */
    public void setString(List<Object> context, String str) {
        if (!str.equals(getString(context)))
            set(context, str);
    }
    
    public void set(List<Object> context, Object v) {
        if (getValue(context).equals(v)) return;
        if (v.getClass() != factoryObj.getClass())
            throw new ClassCastException();
        context.set(index, factoryObj.equals(v) ? factoryObj : v);
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
        return values.get(index);
    }

	/**
	 * Method to get the value of this Setting object as an Object in a specified context.
	 * The proper way to get the current value is to use one of the type-specific
	 * methods such as getInt(), getBoolean(), etc.
     * @param context specified context
	 * @return the Object value of this Setting object in the specfied context.
	 */
	public Object getValue(List<Object> context) {
        return context.get(index);
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
                                          ProjSettingsNode xmlNode, String xmlName,
                                          String location, String description, boolean factory) {
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
                                      ProjSettingsNode xmlNode, String xmlName,
                                      String location, String description, int factory) {
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
                                       ProjSettingsNode xmlNode, String xmlName,
                                       String location, String description, long factory) {
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
                                         ProjSettingsNode xmlNode, String xmlName,
                                         String location, String description, double factory) {
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
                                         ProjSettingsNode xmlNode, String xmlName,
                                         String location, String description, String factory) {
        return new Setting(name, group, xmlNode, xmlName, location, description, (String)factory);
    }
    
	/**
	 * Method to find the project Setting object by its xml path.
	 * @param xmlPath the xml path of the desired project Setting object.
	 * @return the project Setting object.
	 */
    public static Setting getSetting(String xmlPath) { return allSettingsByXmlPath.get(xmlPath); }
    
	/**
	 * Method to get a list of projecy Settings assiciatiated with the given owner object.
	 * @param ownerObj owner object
	 * @return a list of project Settings
	 */
    public static List<Setting> getSettings(ProjSettingsNode node) {
        ArrayList<Setting> settings = new ArrayList<Setting>();
        getSettings(node, settings);
        Collections.sort(settings, SETTINGS_BY_PREF_NAME);
        return settings;
    }
    
    private static void getSettings(ProjSettingsNode node, ArrayList<Setting> settings) {
        Set<String> keys = node.getKeys();
        for (String key: keys) {
            Setting setting = node.getValue(key);
            if (setting != null) {
                if (!setting.isValidOption()) continue;
                if (setting.getValue().equals(setting.getFactoryValue())) continue;
                settings.add(setting);
                continue;
            }
            ProjSettingsNode subNode = node.getNode(key);
            if (subNode != null) {
                getSettings(subNode, settings);
            }
        }
    }
    
    private static Comparator<Setting> SETTINGS_BY_PREF_NAME = new Comparator<Setting> () {
        public int compare(Setting s1, Setting s2) {
            String n1 = s1.getPrefName();
            String n2 = s2.getPrefName();
            return n1.compareTo(n2);
        }
    };
    
    /**
     * Comparator class for sorting Preferences by their name.
     */
    private static final Comparator<Setting> SettingsByName = new Comparator<Setting>() {
        /**
         * Method to sort Setting by their prefName.
         */
        public int compare(Setting setting1, Setting setting2) {
            String s1 = setting1.getPrefName();
            String s2 = setting2.getPrefName();
            return s1.compareToIgnoreCase(s2);
        }
    };

	/**
	 * Method to adjust project that were saved with a library.
	 * Presents the user with a dialog to help reconcile the difference
	 * between project settings stored in a library and the original values.
	 */
	public static void reconcileSettings(String libName, Map<String,Object> projectSettings)
	{
        HashSet<Setting> markedSettings = new HashSet<Setting>();
		Map<Setting,Object> settingsToReconcile = new HashMap<Setting,Object>();
        for (Map.Entry<String,Object> e: projectSettings.entrySet()) {
            String prefPath = e.getKey();
            Setting setting = allSettingsByPrefPath.get(prefPath);
            if (setting == null) continue;
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

		if (settingsToReconcile.size() == 0) return;

        Job.getExtendedUserInterface().finishSettingReconcilation(libName, settingsToReconcile);
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
            if (setting.factoryObj instanceof Boolean) {
                if (obj instanceof Boolean) setting.setBoolean(((Boolean)obj).booleanValue()); else
                    if (obj instanceof Integer) setting.setBoolean(((Integer)obj).intValue() != 0);
            } else if (setting.factoryObj instanceof Integer) {
                setting.setInt(((Integer)obj).intValue());
            } else if (setting.factoryObj instanceof Long) {
                setting.setLong(((Long)obj).longValue());
            } else if (setting.factoryObj instanceof Double) {
                if (obj instanceof Double) setting.setDouble(((Double)obj).doubleValue()); else
                    if (obj instanceof Float) setting.setDouble((double)((Float)obj).floatValue());
            } else if (setting.factoryObj instanceof String) {
                setting.setString((String)obj);
            } else {
                continue;
            }
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
    
    private void saveToPreferences(Object v) {
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
        values.set(index, cachedObj);
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
	 * @param obj the collection of project setting changes.
	 */
    public static void implementSettingChanges(SettingChangeBatch batch) {
        for (Map.Entry<String,Object> e: batch.changesForSettings.entrySet()) {
            String xmlPath = e.getKey();
            Object newValue = e.getValue();
            Setting setting = getSetting(xmlPath);
            setting.set(newValue);
        }
    }

    public static List<Object> getContext() { return values; }
    public static Collection<Setting> getSettings() { return allSettingsByXmlPath.values(); }
    
    static void printAllSettings(PrintStream out) {
        TreeMap<String,Setting> sortedSettings = new TreeMap<String,Setting>();
        for (Setting setting: allSettingsByXmlPath.values())
            sortedSettings.put(setting.xmlPath, setting);
        out.println("PROJECT SETTINGS");
        i = 0;
 //       for (Setting setting: sortedSettings.values())
 //           out.println((i++) + "\t" + setting.xmlPath + " " + setting.cachedObj);
        printSettings(out, ProjSettings.getSettings(), 0);
    }

    private static int i;
    
    private static void printSettings(PrintStream out, ProjSettingsNode node, int level) {
        Set<String> keys = node.getKeys();
        for (String key: keys) {
            out.print((i++) + "\t");
            for (int i = 0; i < level; i++) out.print("  ");
            out.print(key);
            ProjSettingsNode subNode = node.getNode(key);
            if (subNode != null) {
                out.println(".");
                printSettings(out, subNode, level + 1);
                continue;
            }
            Setting setting = node.getValue(key);
            out.println(" " + setting.prefName + " " + setting.getValue());
        }
   }
}
