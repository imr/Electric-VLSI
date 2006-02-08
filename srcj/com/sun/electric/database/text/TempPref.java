/*
 * TempPref.java
 *
 * Created on February 7, 2006, 7:43 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.electric.database.text;

import java.util.prefs.Preferences;

/**
 *
 * @author dn146861
 */
public class TempPref {
    private Pref pref;
    
    /** Creates a new instance of TempPref */
    private TempPref(Pref pref) {
        this.pref = pref;
    }
    
    public static TempPref makeBooleanPref(boolean factory) {
        return new TempPref(Pref.makeBooleanPref(null, (Preferences)null, factory));
    }
    
	public static TempPref makeIntPref(int factory) {
        return new TempPref(Pref.makeIntPref(null, (Preferences)null, factory));
    }
    
	public static TempPref makeLongPref(long factory) {
        return new TempPref(Pref.makeLongPref(null, (Preferences)null, factory));
    }
    
	public static TempPref makeDoublePref(double factory) {
        return new TempPref(Pref.makeDoublePref(null, (Preferences)null, factory));
    }
    
	public static TempPref makeStringPref(String factory) {
        return new TempPref(Pref.makeStringPref(null, (Preferences)null, factory));
    }
    
	/**
	 * Method to get the boolean value on this TempPref object.
	 * The object must have been created as "boolean".
	 * @return the boolean value on this TempPref object.
	 */
	public boolean getBoolean() { return pref.getBoolean(); }

	/**
	 * Method to get the integer value on this TempPref object.
	 * The object must have been created as "integer".
	 * @return the integer value on this TempPref object.
	 */
	public int getInt() { return pref.getInt(); }

	/**
	 * Method to get the long value on this TempPref object.
	 * The object must have been created as "long".
	 * @return the long value on this TempPref object.
	 */
	public long getLong() { return pref.getLong(); }

	/**
	 * Method to get the double value on this TempPref object.
	 * The object must have been created as "double".
	 * @return the double value on this TempPref object.
	 */
	public double getDouble() { return pref.getDouble(); }

	/**
	 * Method to get the string value on this TempPref object.
	 * The object must have been created as "string".
	 * @return the string value on this TempPref object.
	 */
	public String getString() { return pref.getString(); }

	/**
	 * Method to get the factory-default value of this TempPref object.
	 * @return the factory-default value of this TempPref object.
	 */
	public Object getFactoryValue() { return pref.getFactoryValue(); }

	/**
	 * Method to get the factory-default boolean value of this TempPref object.
	 * @return the factory-default boolean value of this TempPref object.
	 */
	public boolean getBooleanFactoryValue() { return pref.getBooleanFactoryValue(); }

	/**
	 * Method to get the factory-default integer value of this TempPref object.
	 * @return the factory-default integer value of this TempPref object.
	 */
	public int getIntFactoryValue() { return pref.getIntFactoryValue(); }

	/**
	 * Method to get the factory-default long value of this TempPref object.
	 * @return the factory-default long value of this TempPref object.
	 */
	public long getLongFactoryValue() { return pref.getLongFactoryValue(); }

	/**
	 * Method to get the factory-default double value of this TempPref object.
	 * @return the factory-default double value of this TempPref object.
	 */
	public double getDoubleFactoryValue() { return pref.getDoubleFactoryValue(); }

	/**
	 * Method to get the factory-default String value of this Pref object.
	 * @return the factory-default String value of this Pref object.
	 */
	public String getStringFactoryValue() { return pref.getStringFactoryValue(); }

	/**
	 * Method to set a new boolean value on this TempPref object.
	 * @param v the new boolean value of this TempPref object.
	 */
	public void setBoolean(boolean v) { pref.setBoolean(v); }

	/**
	 * Method to set a new integer value on this TempPref object.
	 * @param v the new integer value of this TempPref object.
	 */
	public void setInt(int v) { pref.setInt(v); }

	/**
	 * Method to set a new long value on this TempPref object.
	 * @param v the new long value of this TempPref object.
	 */
	public void setLong(long v) { pref.setLong(v); }

	/**
	 * Method to set a new double value on this TempPref object.
	 * @param v the new double value of this TempPref object.
	 * @return true if preference was really changed.
	 */
	public boolean setDouble(double v) { return pref.setDouble(v); }

	/**
	 * Method to set a new string value on this TempPref object.
	 * @param str the new string value of this TempPref object.
	 */
	public void setString(String str) { pref.setString(str); }
}
