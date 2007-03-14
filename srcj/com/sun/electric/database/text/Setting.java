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
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.projectSettings.ProjSettingsNode;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 *
 */
public class Setting {
    private static final HashMap<Object,HashMap<String,Setting>> allSettings = new HashMap<Object,HashMap<String,Setting>>();
    
	private final Object factoryObj;
    private final Object ownerObj;
    private final Preferences prefs;
    private final String prefName;
    private boolean valid;
    private final String description, location;
    private String [] trueMeaning;
    
	private Object cachedObj;
    
    /** Creates a new instance of Setting */
    public Setting(String prefName, Pref.Group group, Object ownerObj, ProjSettingsNode xmlNode, String xmlName, String location, String description, Object factoryObj) {
        this.factoryObj = factoryObj;
        this.ownerObj = ownerObj;
        this.prefName = prefName;
        prefs = group.prefs;
        HashMap<String,Setting> ownerSettings = allSettings.get(ownerObj);
        if (ownerSettings == null) {
            ownerSettings = new HashMap<String,Setting>();
            allSettings.put(ownerObj, ownerSettings);
        }
        if (ownerSettings.put(getPrefName(), this) != null)
            throw new IllegalArgumentException("Duplicate project setting " + getPrefName() + " in " + ownerObj);
        valid = true;
        this.description = description;
        this.location = location;
        if (xmlName == null) xmlName = getPrefName();
        xmlNode.putValue(xmlName, this);
        
        setCachedObjFromPreferences();
    }
    
    private void setCachedObjFromPreferences() {
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
        } else {
            assert false;
        }
    }
    
    /**
     * Method to get the boolean value on this TechSetting object.
     * The object must have been created as "boolean".
     * @return the boolean value on this TechSetting object.
     */
    public boolean getBoolean() { return ((Boolean)getValue()).booleanValue(); }
    
    /**
     * Method to get the integer value on this TechSetting object.
     * The object must have been created as "integer".
     * @return the integer value on this TechSetting object.
     */
    public int getInt() { return ((Integer)getValue()).intValue(); }
    
    /**
     * Method to get the long value on this TechSetting object.
     * The object must have been created as "long".
     * @return the long value on this TechSetting object.
     */
    public long getLong() { return ((Long)getValue()).longValue(); }
    
    /**
     * Method to get the double value on this TechSetting object.
     * The object must have been created as "double".
     * @return the double value on this TechSetting object.
     */
    public double getDouble() { return ((Double)getValue()).doubleValue(); }
    
    /**
     * Method to get the string value on this TechSetting object.
     * The object must have been created as "string".
     * @return the string value on this TechSetting object.
     */
    public String getString() { return (String)getValue(); }
    
    private static boolean changed;
    
    /**
     * Method to set a new boolean value on this TechSetting object.
     * @param v the new boolean value of this TechSetting object.
     */
    public void setBoolean(boolean v) {
        if (v == getBoolean()) return;
        cachedObj = Boolean.valueOf(v);
        prefs.putBoolean(prefName, v);
        Pref.flushOptions_(prefs);
        changed = true;
        setSideEffect();
    }
    
    /**
     * Method to set a new integer value on this TechSetting object.
     * @param v the new integer value of this TechSetting object.
     */
    public void setInt(int v) {
        if (v == getInt()) return;
        cachedObj = Integer.valueOf(v);
        prefs.putInt(prefName, v);
        Pref.flushOptions_(prefs);
        changed = true;
        setSideEffect();
     }
    
    /**
     * Method to set a new long value on this TechSetting object.
     * @param v the new long value of this TechSetting object.
     */
    public void setLong(long v) {
        if (v == getInt()) return;
        cachedObj = Long.valueOf(v);
        prefs.putLong(prefName, v);
        Pref.flushOptions_(prefs);
        changed = true;
        setSideEffect();
    }
    
    /**
     * Method to set a new double value on this TechSetting object.
     * @param v the new double value of this Pref object.
     * @return true if preference was really changed.
     */
    public boolean setDouble(double v) {
        if (v == getDouble()) return false;
        cachedObj = Double.valueOf(v);
        prefs.putDouble(prefName, v);
        Pref.flushOptions_(prefs);
        changed = true;
        setSideEffect();
        return true;
    }
    
    /**
     * Method to set a new string value on this TechSetting object.
     * @param str the new string value of this TechSetting object.
     */
    public void setString(String str) {
        if (str.equals(getString())) return;
        cachedObj = str;
        prefs.put(prefName, str);
        Pref.flushOptions_(prefs);
        changed = true;
        setSideEffect();
    }
    
    /**
	 * Method called when this Pref is changed.
	 * This method is overridden in subclasses that want notification.
	 */
    protected void setSideEffect() {
    }
    
    /**
     * Method to get the name of this Setting object.
     * @return the name of this Setting object.
     */
    public String getPrefName() { return prefName; }
    
	/**
	 * Method to get the value of this Pref object as an Object.
	 * The proper way to get the current value is to use one of the type-specific
	 * methods such as getInt(), getBoolean(), etc.
	 * @return the Object value of this Pref object.
	 */
	public Object getValue() { return cachedObj; }

    /**
     * Method to return the owner Object associated with this Meaning option.
     * @return the owner Object associated with this Meaning option.
     */
    public Object getOwnerObject() { return ownerObj; }
    
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
	 * @param ownerObj the Object to attach this Pref to.
	 * @param location the user-command that can affect this meaning option.
	 * @param description the description of this meaning option.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
    public static Setting makeBooleanSetting(String name, Pref.Group group, Object ownerObj,
                                          ProjSettingsNode xmlNode, String xmlName,
                                          String location, String description, boolean factory) {
        return new Setting(name, group, ownerObj, xmlNode, xmlName, location, description, Boolean.valueOf(factory));
    }
    
	/**
	 * Factory methods to create an integerproject setting objects.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param ownerObj the Object to attach this Pref to.
	 * @param location the user-command that can affect this meaning option.
	 * @param description the description of this meaning option.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
    public static Setting makeIntSetting(String name, Pref.Group group, Object ownerObj,
                                      ProjSettingsNode xmlNode, String xmlName,
                                      String location, String description, int factory) {
        return new Setting(name, group, ownerObj, xmlNode, xmlName, location, description, Integer.valueOf(factory));
    }
    
	/**
	 * Factory methods to create a long project setting objects.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param ownerObj the Object to attach this Pref to.
	 * @param location the user-command that can affect this meaning option.
	 * @param description the description of this meaning option.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
    public static Setting makeLongSetting(String name, Pref.Group group, Object ownerObj,
                                       ProjSettingsNode xmlNode, String xmlName,
                                       String location, String description, long factory) {
        return new Setting(name, group, ownerObj, xmlNode, xmlName, location, description, Long.valueOf(factory));
    }
    
	/**
	 * Factory methods to create a double project setting objects.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param ownerObj the Object to attach this Pref to.
	 * @param location the user-command that can affect this meaning option.
	 * @param description the description of this meaning option.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
    public static Setting makeDoubleSetting(String name, Pref.Group group, Object ownerObj,
                                         ProjSettingsNode xmlNode, String xmlName,
                                         String location, String description, double factory) {
        return new Setting(name, group, ownerObj, xmlNode, xmlName, location, description, Double.valueOf(factory));
    }

	/**
	 * Factory methods to create a string project setting objects.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param ownerObj the Object to attach this Pref to.
	 * @param location the user-command that can affect this meaning option.
	 * @param description the description of this meaning option.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
    public static Setting makeStringSetting(String name, Pref.Group group, Object ownerObj,
                                         ProjSettingsNode xmlNode, String xmlName,
                                         String location, String description, String factory) {
        return new Setting(name, group, ownerObj, xmlNode, xmlName, location, description, (String)factory);
    }
    
	/**
	 * Method to find the project Setting object associated with a given part of the
	 * Electric database.
	 * @param ownerObj the Object on which to find a project Setting object.
	 * @param name the name of the desired project Setting object.
	 * @return the project Setting object on that part of the database.
	 */
    public static Setting getSetting(Object ownerObj, String name) {
        HashMap<String,Setting> ownerSettings = allSettings.get(ownerObj);
        return ownerSettings != null ? ownerSettings.get(name) : null;
    }
    
	/**
	 * Method to get a list of projecy Settings assiciatiated with the given owner object.
	 * @param ownerObj owner object
	 * @return a list of project Settings
	 */
	public static List<Setting> getSettings(Object ownerObj)
	{
        HashMap<String,Setting> ownerSettings = allSettings.get(ownerObj);
        if (ownerSettings == null) return Collections.emptyList();
        ArrayList<Setting> settings = new ArrayList<Setting>();
        for (Setting setting: ownerSettings.values()) {
            if (!setting.isValidOption()) continue;
            if (setting.getValue().equals(setting.getFactoryValue())) continue;
            settings.add(setting);
        }
		Collections.sort(settings, SettingsByName);
		return settings;
	}
    
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
	public static void reconcileSettings(String libName, Map<Object,Map<String,Object>> meaningVariables)
	{
        HashSet<Setting> markedSettings = new HashSet<Setting>();
		Map<Setting,Object> settingsToReconcile = new HashMap<Setting,Object>();
        for (Object obj: meaningVariables.keySet()) {
            Map<String,Object> meanings = meaningVariables.get(obj);
            for (String prefName: meanings.keySet()) {
                Setting setting = Setting.getSetting(obj, prefName);
                if (setting == null) continue;
                Object value = meanings.get(prefName);
                markedSettings.add(setting);
                if (DBMath.objectsReallyEqual(value, setting.getValue())) continue;
                if (!setting.isValidOption()) continue;
//System.out.println("Meaning variable "+meaning.pref.name+" found on " + meaning.ownerObj+" is "+value+" but is cached as "+meaning.pref.cachedObj);
                settingsToReconcile.put(setting, value);
            }
		}
        for (HashMap<String,Setting> ownerSettings: allSettings.values()) {
            for (Setting setting: ownerSettings.values()) {
                if (markedSettings.contains(setting)) continue;
                
                // this one is not mentioned in the library: make sure it is at factory defaults
                if (DBMath.objectsReallyEqual(setting.getValue(), setting.getFactoryValue())) continue;

//System.out.println("Adding fake meaning variable "+pref.name+" where current="+pref.cachedObj+" but should be "+pref.factoryObj);
                if (!setting.isValidOption()) continue;
                settingsToReconcile.put(setting, null);
            }
        }

		if (settingsToReconcile.size() == 0) return;

        Job.getExtendedUserInterface().finishSettingReconcilation(libName, settingsToReconcile);
//		if (Job.BATCHMODE)
//		{
//            finishPrefReconcilation(meaningsToReconcile);
//			return;
//		}
// 		OptionReconcile dialog = new OptionReconcile(TopLevel.getCurrentJFrame(), true, meaningsToReconcile, libName);
//		dialog.setVisible(true);
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
            System.out.println("Project Setting "+setting.getPrefName()+" on " + setting.getOwnerObject()+" changed to "+obj);
        }
        
        // resume flushing, and save everything just set
        Pref.resumePrefFlushing();
    }
    
    static void setPreferencesToFactoryValue() {
        for (Map<String,Setting> ownerSettings: allSettings.values()) {
            for (Setting setting: ownerSettings.values()) {
                if (setting.getValue().equals(setting.getFactoryValue())) continue;
                Preferences prefs = setting.prefs;
                String key = setting.prefName;
                Object obj = setting.getFactoryValue();
                if (obj instanceof Boolean)
                    prefs.putBoolean(key, ((Boolean)obj).booleanValue());
                else if (obj instanceof Integer)
                    prefs.putInt(key, ((Integer)obj).intValue());
                else if (obj instanceof Long)
                    prefs.putLong(key, ((Long)obj).longValue());
                else if (obj instanceof Double)
                    prefs.putDouble(key, ((Double)obj).doubleValue());
                else if (obj instanceof String)
                    prefs.put(key, (String)obj);
                else
                    assert false;
            }
        }
    }
    
    static void setFromPreferences() {
        for (Map<String,Setting> ownerSettings: allSettings.values()) {
            for (Setting setting: ownerSettings.values()) {
                setting.setCachedObjFromPreferences();
            }
        }
    }
    
    /**
     * Mark all preferences as "unchanged"
     */
    public static void clearChangedAllPrefs() {
        changed = false;
    }

    /**
     * Return true if any pref has changed since the last
     * call to clearChangedAllPrefs()
     * @return true if any pref has changed since changes were cleared
     */
    public static boolean anyPrefChanged() {
        return changed;
    }
    
    public static void printAllSettings(PrintStream out) {
        TreeMap<String,Setting> sortedSettings = new TreeMap<String,Setting>();
        for (Map<String,Setting> ownerSettings: allSettings.values()) {
            for (Setting setting: ownerSettings.values()) {
                sortedSettings.put(setting.prefs.absolutePath() + "/" + setting.prefName, setting);
            }
        }
        out.println("PROJECT SETTINGS");
        int  i = 0;
        for (Setting setting: sortedSettings.values())
            out.println((i++) + setting.prefs.absolutePath() + " " + setting.prefName + " " + setting.cachedObj);
    }

}
