/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Prefs.java
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
package com.sun.electric.tool.user;

import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

/**
 * Class for saving options.
 */
public class Prefs
{
	private Preferences prefs;
	private static Prefs only = new Prefs();

	// constructor
	private Prefs()
	{
		prefs = Preferences.userNodeForPackage(getClass());
	}

	/**
	 * Method to get an integer option value.
	 * @param optionName the name of the option.
	 * @return the value of the option (0 if not found).
	 */
	public static int getIntegerOption(String optionName)
	{
        int value = only.prefs.getInt(optionName, 0);
		return value;
	}

	/**
	 * Method to get a double-precision option value.
	 * @param optionName the name of the option.
	 * @return the value of the option (0 if not found).
	 */
	public static double getDoubleOption(String optionName)
	{
        double value = only.prefs.getDouble(optionName, 0);
		return value;
	}

	/**
	 * Method to get a boolean option value.
	 * @param optionName the name of the option.
	 * @return the value of the option (false if not found).
	 */
	public static boolean getBooleanOption(String optionName)
	{
        boolean value = only.prefs.getBoolean(optionName, false);
		return value;
	}

	/**
	 * Method to get a String option value.
	 * @param optionName the name of the option.
	 * @return the value of the option ("" if not found).
	 */
	public static String getStringOption(String optionName)
	{
        String value = only.prefs.get(optionName, "");
		return value;
	}

	/**
	 * Method to set an integer option value.
	 * @param optionName the name of the option.
	 * @param value the value of the option to save.
	 */
	public static void setIntegerOption(String optionName, int value)
	{
		only.prefs.putInt(optionName, value);
		try
		{
	        only.prefs.flush();
		} catch (BackingStoreException e)
		{
			System.out.println("Failed to save option " + optionName);
		}
	}

	/**
	 * Method to set a double-precision option value.
	 * @param optionName the name of the option.
	 * @param value the value of the option to save.
	 */
	public static void setDoubleOption(String optionName, double value)
	{
		only.prefs.putDouble(optionName, value);
		try
		{
	        only.prefs.flush();
		} catch (BackingStoreException e)
		{
			System.out.println("Failed to save option " + optionName);
		}
	}

	/**
	 * Method to set a boolean option value.
	 * @param optionName the name of the option.
	 * @param value the value of the option to save.
	 */
	public static void setBooleanOption(String optionName, boolean value)
	{
		only.prefs.putBoolean(optionName, value);
		try
		{
	        only.prefs.flush();
		} catch (BackingStoreException e)
		{
			System.out.println("Failed to save option " + optionName);
		}
	}

	/**
	 * Method to set a String option value.
	 * @param optionName the name of the option.
	 * @param value the value of the option to save.
	 */
	public static void setStringOption(String optionName, String value)
	{
		only.prefs.put(optionName, value);
		try
		{
	        only.prefs.flush();
		} catch (BackingStoreException e)
		{
			System.out.println("Failed to save option " + optionName);
		}
	}

	/**
	 * Method to tell whether a given option exists.
	 * @param optionName the name of the option.
	 * @return true if this option exists.
	 */
	public static boolean exists(String optionName)
	{
		String [] allKeys;
		try
		{
			allKeys = only.prefs.keys();
		} catch (BackingStoreException e)
		{
			return false;
		}
		for(int i=0; i<allKeys.length; i++)
			if (allKeys[i].equals(optionName)) return true;
		return false;
	}
}
