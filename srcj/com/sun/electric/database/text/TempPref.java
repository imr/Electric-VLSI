/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TempPref.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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

/**
 * Non-persistent preference. Used in dialogs.
 */
public class TempPref {
	private final Object factoryObj;
	private Object cachedObj;
    
    /** Creates a new instance of TempPref */
    private TempPref(Object factoryObj) {
        this.factoryObj = factoryObj;
        this.cachedObj = factoryObj;
    }
    
	/**
	 * Factory methods to create a boolean TempPref objects.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
    public static TempPref makeBooleanPref(boolean factory) {
        return new TempPref(Boolean.valueOf(factory));
    }
    
	/**
	 * Factory methods to create an integer TempPref objects.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static TempPref makeIntPref(int factory) {
        return new TempPref(Integer.valueOf(factory));
    }
    
	/**
	 * Factory methods to create a long TempPref objects.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static TempPref makeLongPref(long factory) {
        return new TempPref(Long.valueOf(factory));
    }
    
	/**
	 * Factory methods to create a double TempPref objects.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static TempPref makeDoublePref(double factory) {
        return new TempPref(Double.valueOf(factory));
    }
    
	/**
	 * Factory methods to create a string TempPref objects.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static TempPref makeStringPref(String factory) {
        return new TempPref(factory);
    }
    
	/**
	 * Method to get the boolean value on this TempPref object.
	 * The object must have been created as "boolean".
	 * @return the boolean value on this TempPref object.
	 */
	public boolean getBoolean() { return ((Boolean)cachedObj).booleanValue(); }

	/**
	 * Method to get the integer value on this TempPref object.
	 * The object must have been created as "integer".
	 * @return the integer value on this TempPref object.
	 */
	public int getInt() { return ((Integer)cachedObj).intValue(); }

	/**
	 * Method to get the long value on this TempPref object.
	 * The object must have been created as "long".
	 * @return the long value on this TempPref object.
	 */
	public long getLong() { return ((Long)cachedObj).longValue(); }

	/**
	 * Method to get the double value on this TempPref object.
	 * The object must have been created as "double".
	 * @return the double value on this TempPref object.
	 */
	public double getDouble() { return ((Double)cachedObj).doubleValue(); }

	/**
	 * Method to get the string value on this TempPref object.
	 * The object must have been created as "string".
	 * @return the string value on this TempPref object.
	 */
	public String getString() { return (String)cachedObj; }

	/**
	 * Method to get the factory-default value of this TempPref object.
	 * @return the factory-default value of this TempPref object.
	 */
	public Object getFactoryValue() { return factoryObj; }

	/**
	 * Method to get the factory-default boolean value of this TempPref object.
	 * @return the factory-default boolean value of this TempPref object.
	 */
	public boolean getBooleanFactoryValue() { return ((Boolean)factoryObj).booleanValue(); }

	/**
	 * Method to get the factory-default integer value of this TempPref object.
	 * @return the factory-default integer value of this TempPref object.
	 */
	public int getIntFactoryValue() { return ((Integer)factoryObj).intValue(); }

	/**
	 * Method to get the factory-default long value of this TempPref object.
	 * @return the factory-default long value of this TempPref object.
	 */
	public long getLongFactoryValue() { return ((Long)factoryObj).longValue(); }

	/**
	 * Method to get the factory-default double value of this TempPref object.
	 * @return the factory-default double value of this TempPref object.
	 */
	public double getDoubleFactoryValue() { return ((Double)factoryObj).doubleValue(); }

	/**
	 * Method to get the factory-default String value of this Pref object.
	 * @return the factory-default String value of this Pref object.
	 */
	public String getStringFactoryValue() { return (String)factoryObj; }

	/**
	 * Method to set a new boolean value on this TempPref object.
	 * @param v the new boolean value of this TempPref object.
	 */
	public void setBoolean(boolean v) { cachedObj = Boolean.valueOf(v); }

	/**
	 * Method to set a new integer value on this TempPref object.
	 * @param v the new integer value of this TempPref object.
	 */
	public void setInt(int v) { cachedObj = Integer.valueOf(v); }

	/**
	 * Method to set a new long value on this TempPref object.
	 * @param v the new long value of this TempPref object.
	 */
	public void setLong(long v) { cachedObj = Long.valueOf(v); }

	/**
	 * Method to set a new double value on this TempPref object.
	 * @param v the new double value of this TempPref object.
	 */
	public void setDouble(double v) { cachedObj = Double.valueOf(v); }

	/**
	 * Method to set a new string value on this TempPref object.
	 * @param str the new string value of this TempPref object.
	 */
	public void setString(String str) { cachedObj = str; }
}
