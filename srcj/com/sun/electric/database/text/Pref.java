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

import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.tool.user.dialogs.OptionReconcile;
import com.sun.electric.tool.user.ui.TopLevel;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

/**
 * This class manages options.
 * There are two types of options: <I>appearance</I> and <I>meaning</I>.
 * Appearance options affect the way that the design is presented to the user.
 * Examples are grid dot spacing, layer stipple patterns, etc.
 * Meaning options affect the way that a design is produced (for fabrication,
 * simulation, and other outputs).  Examples are CIF layer names, technology
 * options, etc.)
 * <P>
 * All options are saved in a machine-specific way by the Java Preferences class.
 * In addition, "meaning" options are stored in libraries.  When the libraries
 * are read back into Electric, the stored meaning options are checked against
 * the current meaning options, the two are reconciled.
 */
public class Pref
{
	/**
	 * This class provides extra information for "meaning" options.
	 */
	public static class Meaning
	{
		private ElectricObject eObj;
		private Object desiredValue;
		private Pref pref;
		private String description, location;
		private boolean marked;
		private String [] trueMeaning;

		Meaning(ElectricObject eObj, Pref pref, String location, String description)
		{
			this.eObj = eObj;
			this.pref = pref;
			this.location = location;
			this.description = description;
		}

		public Pref getPref() { return pref; }
		public ElectricObject getElectricObject() { return eObj; }
		public String getLocation() { return location; }
		public String getDescription() { return description; }
		public void setTrueMeaning(String [] trueMeaning) { this.trueMeaning = trueMeaning; }
		public String [] getTrueMeaning() { return trueMeaning; }
		public void setDesiredValue(Object desiredValue) { this.desiredValue = desiredValue; }
		public Object getDesiredValue() { return desiredValue; }
	}

	public static final int BOOLEAN = 0;
	public static final int INTEGER = 1;
	public static final int DOUBLE = 2;
	public static final int STRING = 3;

	private   String      name;
	private   int         type;
	private   Preferences prefs;
	private   Meaning     meaning;
	protected Object      cachedObj;
	protected Object      factoryObj;

	private static List allPrefs = new ArrayList();

	protected Pref() {}

	// factory methods to create these objects
	protected void initBoolean(String name, Preferences prefs, boolean factory)
	{
		this.name = name;
		this.prefs = prefs;
		this.type = BOOLEAN;
		this.meaning = null;
		this.factoryObj = new Integer(factory ? 1 : 0);
		this.cachedObj = new Integer(prefs.getBoolean(name, factory) ? 1 : 0);
		allPrefs.add(this);
	}
	public static Pref makeBooleanPref(String name, Preferences prefs, boolean factory)
	{
		Pref pref = new Pref();
		pref.initBoolean(name, prefs, factory);
		return pref;
	}

	protected void initInt(String name, Preferences prefs, int factory)
	{
		this.name = name;
		this.prefs = prefs;
		this.type = INTEGER;
		this.meaning = null;
		this.factoryObj = new Integer(factory);
		this.cachedObj = new Integer(prefs.getInt(name, factory));
		allPrefs.add(this);
	}
	public static Pref makeIntPref(String name, Preferences prefs, int factory)
	{
		Pref pref = new Pref();
		pref.initInt(name, prefs, factory);
		return pref;
	}

	protected void initDouble(String name, Preferences prefs, double factory)
	{
		this.name = name;
		this.prefs = prefs;
		this.type = DOUBLE;
		this.meaning = null;
		this.factoryObj = new Double(factory);
		this.cachedObj = new Double(prefs.getDouble(name, factory));
		allPrefs.add(this);
	}
	public static Pref makeDoublePref(String name, Preferences prefs, double factory)
	{
		Pref pref = new Pref();
		pref.initDouble(name, prefs, factory);
		return pref;
	}

	protected void initString(String name, Preferences prefs, String factory)
	{
		this.name = name;
		this.prefs = prefs;
		this.type = STRING;
		this.meaning = null;
		this.factoryObj = new String(factory);
		this.cachedObj = new String(prefs.get(name, factory));
		allPrefs.add(this);
	}
	public static Pref makeStringPref(String name, Preferences prefs, String factory)
	{
		Pref pref = new Pref();
		pref.initString(name, prefs, factory);
		return pref;
	}

	// methods for getting values from the objects
	public boolean getBoolean() { return ((Integer)cachedObj).intValue() != 0; }
	public int getInt() { return ((Integer)cachedObj).intValue(); }
	public double getDouble() { return ((Double)cachedObj).doubleValue(); }
	public String getString() { return (String)cachedObj; }

	public String getPrefName() { return name; }
	public Object getValue() { return cachedObj; }
	public Object getFactoryValue() { return factoryObj; }
	public int getType() { return type; }
	public Meaning getMeaning() { return meaning; }

	/**
	 * Method called when this Pref is changed.
	 * This method is overridden in subclasses that want notification.
	 */
	public void setSideEffect() {}

	// methods for setting values on the objects
	public void setBoolean(boolean v)
	{
		boolean cachedBool = ((Integer)cachedObj).intValue() != 0 ? true : false;
		if (v != cachedBool)
		{
			cachedObj = new Integer(v ? 1 : 0);
			prefs.putBoolean(name, v);
			flushOptions();
		}
		setSideEffect();
	}
	public void setInt(int v)
	{
		int cachedInt = ((Integer)cachedObj).intValue();
		if (v != cachedInt)
		{
			cachedObj = new Integer(v);
			prefs.putInt(name, v);
			flushOptions();
		}
		setSideEffect();
	}
	public void setDouble(double v)
	{
		double cachedDouble = ((Double)cachedObj).doubleValue();
		if (v != cachedDouble)
		{
			cachedObj = new Double(v);
			prefs.putDouble(name, v);
			flushOptions();
		}
		setSideEffect();
	}
	public void setString(String str)
	{
		String cachedString = (String)cachedObj;
		if (!str.equals(cachedString))
		{
			cachedObj = new String(str);
			prefs.put(name, str);
			flushOptions();
		}
		setSideEffect();
	}

	/**
	 * Method to make this Pref a "meaning" option.
	 * Options that relate to "meaning"
	 */
	public Meaning attachToObject(ElectricObject eObj, String location, String description)
	{
		meaning = new Meaning(eObj, this, location, description);
		return meaning;
	}

	public static Meaning getMeaningVariable(ElectricObject eObj, String name)
	{
		for(Iterator it = allPrefs.iterator(); it.hasNext(); )
		{
			Pref pref = (Pref)it.next();
			if (pref.meaning == null) continue;
			if (pref.meaning.eObj != eObj) continue;
			if (pref.name.equals(name))
			{
				return pref.meaning;
			}
		}
		return null;
	}

	/**
	 * Method to store all changed "meaning" options in the database.
	 * This is done before saving a library, so that the options related to that
	 * library get saved as well.
	 */
	public static void installMeaningVariables()
	{
		for(Iterator it = allPrefs.iterator(); it.hasNext(); )
		{
			Pref pref = (Pref)it.next();
			if (pref.meaning == null) continue;
			Variable.Key key = ElectricObject.newKey(pref.name);
			if (pref.cachedObj.equals(pref.factoryObj))
			{
				pref.meaning.eObj.delVar(key);
				continue;
			}
//System.out.println("Saving meaning variable "+pref.name+" on " + pref.meaning.eObj);
//System.out.println("   Current value="+pref.cachedObj+" factory value=" + pref.factoryObj);
			pref.meaning.eObj.newVar(key, pref.cachedObj);
		}
	}

	private static List meaningVariablesThatChanged;

	public static void initMeaningVariableGathering()
	{
		meaningVariablesThatChanged = new ArrayList();
	}

	public static void changedMeaningVariable(Meaning meaning)
	{
		meaningVariablesThatChanged.add(meaning);
	}

	/**
	 * Method to adjust "meaning" options that were saved with a library.
	 */
	public static void reconcileMeaningVariables()
	{
		for(Iterator it = allPrefs.iterator(); it.hasNext(); )
		{
			Pref pref = (Pref)it.next();
			if (pref.meaning == null) continue;
			pref.meaning.marked = false;
		}
		List meaningsToReconcile = new ArrayList();
		for(Iterator it = meaningVariablesThatChanged.iterator(); it.hasNext(); )
		{
			Meaning meaning = (Meaning)it.next();
			meaning.marked = true;
//System.out.println("Found meaning variable "+meaning.pref.name+" found on " + meaning.eObj);
			Variable var = meaning.eObj.getVar(meaning.pref.name);
			if (var == null) continue;
			Object obj = var.getObject();
			if (obj.equals(meaning.pref.cachedObj)) continue;
			meaning.setDesiredValue(obj);
			meaningsToReconcile.add(meaning);
		}
		for(Iterator it = allPrefs.iterator(); it.hasNext(); )
		{
			Pref pref = (Pref)it.next();
			if (pref.meaning == null) continue;
			if (pref.meaning.marked) continue;

			// this one is not mentioned in the library: make sure it is at factory defaults
			if (pref.cachedObj.equals(pref.factoryObj)) continue;

//System.out.println("Adding fake meaning variable "+pref.name+" where current="+pref.cachedObj+" but should be "+pref.factoryObj);
			pref.meaning.setDesiredValue(pref.factoryObj);
			meaningsToReconcile.add(pref.meaning);
		}

		if (meaningsToReconcile.size() == 0) return;
 		OptionReconcile dialog = new OptionReconcile(TopLevel.getCurrentJFrame(), true, meaningsToReconcile);
		dialog.show();
	}

	/****************************** FOR PREFERENCES ******************************/

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
