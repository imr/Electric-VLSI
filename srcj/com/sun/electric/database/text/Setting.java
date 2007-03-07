/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Pref.java
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

import com.sun.electric.database.text.Pref.Meaning;
import com.sun.electric.tool.user.projectSettings.ProjSettingsNode;

/**
 *
 */
public class Setting {
    private final Pref pref;
    
    /** Creates a new instance of Setting */
    public Setting(Pref pref) {
        this.pref = pref;
    }
    
    /**
     * Method to get the boolean value on this TechSetting object.
     * The object must have been created as "boolean".
     * @return the boolean value on this TechSetting object.
     */
    public boolean getBoolean() { return pref.getBoolean(); }
    
    /**
     * Method to get the integer value on this TechSetting object.
     * The object must have been created as "integer".
     * @return the integer value on this TechSetting object.
     */
    public int getInt() { return pref.getInt(); }
    
    /**
     * Method to get the long value on this TechSetting object.
     * The object must have been created as "long".
     * @return the long value on this TechSetting object.
     */
    public long getLong() { return pref.getLong(); }
    
    /**
     * Method to get the double value on this TechSetting object.
     * The object must have been created as "double".
     * @return the double value on this TechSetting object.
     */
    public double getDouble() { return pref.getDouble(); }
    
    /**
     * Method to get the string value on this TechSetting object.
     * The object must have been created as "string".
     * @return the string value on this TechSetting object.
     */
    public String getString() { return pref.getString(); }
    
    /**
     * Method to set a new boolean value on this TechSetting object.
     * @param v the new boolean value of this TechSetting object.
     */
    public void setBoolean(boolean v) { pref.setBoolean(v); }
    
    /**
     * Method to set a new integer value on this TechSetting object.
     * @param v the new integer value of this TechSetting object.
     */
    public void setInt(int v) { pref.setInt(v); }
    
    /**
     * Method to set a new long value on this TechSetting object.
     * @param v the new long value of this TechSetting object.
     */
    public void setLong(long v) { pref.setLong(v); }
    
    /**
     * Method to set a new double value on this TechSetting object.
     * @param v the new double value of this Pref object.
     * @return true if preference was really changed.
     */
    public boolean setDouble(double v) { return pref.setDouble(v); }
    
    /**
     * Method to set a new string value on this TechSetting object.
     * @param str the new string value of this TechSetting object.
     */
    public void setString(String str) { pref.setString(str); }
    
    /**
     * Method to get the name of this Pref object.
     * @return the name of this Pref object.
     */
    public String getPrefName() { return pref.getPrefName(); }
    
//    /**
//     * Method to get the Meaning associated with this Pref object.
//     * Not all Pref objects have a meaning, and those that don't are not
//     * meaning options, but instead are appearance options.
//     * @return the Meaning associated with this Pref object.
//     * Returns null if this Pref is not a meaning option.
//     */
//    public Meaning getMeaning() { return pref.getMeaning(); }
//
//	/**
//	 * Method to get the factory-default double value of this Pref object.
//	 * @return the factory-default double value of this Pref object.
//	 */
//	public double getDoubleFactoryValue() { return pref.getDoubleFactoryValue(); }
//
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
        return new Setting(Pref.makeBooleanSetting(name, group, ownerObj, xmlNode, xmlName, location, description, factory));
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
        return new Setting(Pref.makeIntSetting(name, group, ownerObj, xmlNode, xmlName, location, description, factory));
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
        return new Setting(Pref.makeLongSetting(name, group, ownerObj, xmlNode, xmlName, location, description, factory));
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
        return new Setting(Pref.makeDoubleSetting(name, group, ownerObj, xmlNode, xmlName, location, description, factory));
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
        return new Setting(Pref.makeStringSetting(name, group, ownerObj, xmlNode, xmlName, location, description, factory));
    }

}
