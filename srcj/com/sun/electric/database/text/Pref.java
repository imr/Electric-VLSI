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
//
//import com.sun.electric.database.change.Undo;
//import com.sun.electric.database.geometry.EMath;
//import com.sun.electric.database.geometry.Geometric;
//import com.sun.electric.database.geometry.Poly;
//import com.sun.electric.database.hierarchy.Cell;
//import com.sun.electric.database.hierarchy.Export;
//import com.sun.electric.database.text.TextUtils;
//import com.sun.electric.database.topology.NodeInst;
//import com.sun.electric.database.topology.ArcInst;
//import com.sun.electric.database.variable.TextDescriptor;
//import com.sun.electric.database.variable.Variable;
//import com.sun.electric.tool.Job;
//import com.sun.electric.tool.user.ui.EditWindow;
//
//import java.awt.Font;
//import java.awt.font.GlyphVector;
//import java.awt.geom.Point2D;
//import java.awt.geom.Rectangle2D;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Iterator;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

/**
 * This class is the base class of all Electric objects that can be extended with "Variables".
 */
public class Pref
{
	String  name;
	Preferences prefs;
	boolean cachedBool;
	int     cachedInt;
	double  cachedDouble;
	String  cachedString;

	public Pref(String name, Preferences prefs)
	{
		this.name = name;
		this.prefs = prefs;
	}

	public boolean getBoolean() { return cachedBool; }
	public int getInt() { return cachedInt; }
	public double getDouble() { return cachedDouble; }
	public String getString() { return cachedString; }
	public String getPrefName() { return name; }
	public void setBoolean(boolean v)
	{
		if (v != cachedBool)
		{
			prefs.putBoolean(name, cachedBool = v);
			flushOptions();
		}
	}
	public void setInt(int v)
	{
		if (v != cachedInt)
		{
			prefs.putInt(name, cachedInt = v);
			flushOptions();
		}
	}
	public void setDouble(double v)
	{
		if (v != cachedDouble)
		{
			prefs.putDouble(name, cachedDouble = v);
			flushOptions();
		}
	}
	public void setString(String str)
	{
		if (!str.equals(cachedString))
		{
			prefs.put(name, cachedString = str);
			flushOptions();
		}
	}

	public static Pref makeBooleanPref(String name, Preferences prefs, boolean factory)
	{
		Pref pref = new Pref(name, prefs);
		pref.cachedBool = prefs.getBoolean(name, factory);
		return pref;
	}
	public static Pref makeIntPref(String name, Preferences prefs, int factory)
	{
		Pref pref = new Pref(name, prefs);
		pref.cachedInt = prefs.getInt(name, factory);
		return pref;
	}
	public static Pref makeDoublePref(String name, Preferences prefs, double factory)
	{
		Pref pref = new Pref(name, prefs);
		pref.cachedDouble = prefs.getDouble(name, factory);
		return pref;
	}
	public static Pref makeStringPref(String name, Preferences prefs, String factory)
	{
		Pref pref = new Pref(name, prefs);
		pref.cachedString = prefs.get(name, factory);
		return pref;
	}

	/**
	 * Method to force all Preferences to be saved.
	 */
	private void flushOptions()
	{
		try
		{
			prefs.flush();
		} catch (BackingStoreException e)
		{
			System.out.println("Failed to save " + name + " options");
		}
	}
}
