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

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.tool.user.dialogs.OptionReconcile;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.Main;

import java.util.*;
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
 * <P>
 * Where are these options stored?  It varies with each operating system.
 * <UL>
 * <LI><B>Windows:</B>
 *   In the registry.
 *   Look in: HKEY_CURRENT_USER / Software / JavaSoft / Prefs / com / sun / electric.
 * </LI>
 * <LI><B>UNIX/Linux:</B>
 *   In your home directory.
 *   Look in: ~/.java/.userPrefs/com/sun/electric
 * </LI>
 * <LI><B>Macintosh System 10:</B>
 *   In your home directory, under Library/Preferences.
 *   Look at: ~/Library/Preferences/com.sun.electric.plist
 * </LI>
 * </UL>
 */
public class Pref
{
	/**
	 * This class provides extra information for "meaning" options.
	 */
	public static class Meaning
	{
		private Object ownerObj;
		private Variable.Key key;
		private boolean valid;
		private Object desiredValue;
		private Pref pref;
		private String description, location;
		private boolean marked;
		private String [] trueMeaning;

		/**
		 * Constructor for Meaning options to attach them to an object in the database.
		 * @param ownerObj the Object in the database that this Meaning attaches to.
		 * @param pref the Pref object for storing the option value.
		 * @param location the user-command that can affect this Meaning option.
		 * @param description the description of this Meaning option.
		 */
		Meaning(Object ownerObj, Pref pref, String location, String description)
		{
			this.ownerObj = ownerObj;
			key = ElectricObject.newKey(pref.name);
			this.pref = pref;
			this.location = location;
			this.description = description;
			this.valid = true;
		}

		/**
		 * Method to return the Pref associated with this Meaning option.
		 * @return the Pref associated with this Meaning option.
		 */
		public Pref getPref() { return pref; }

		/**
		 * Method to return the Variable.Key of name of this Meaning option.
		 * @return the Variable.Key of name of this Meaning option.
		 */
		public Variable.Key getKey() { return key; }

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
		 * Method to set the desired value on this Meaning.
		 * The desired value is the one that is currently in effect, as opposed to
		 * the value that has been read with a Library.
		 * @param desiredValue the desired value on this Meaning.
		 */
		private void setDesiredValue(Object desiredValue) { this.desiredValue = desiredValue; }

		/**
		 * Method to get the desired value on this Meaning.
		 * The desired value is the one that is currently in effect, as opposed to
		 * the value that has been read with a Library.
		 * @return the desired value on this Meaning.
		 */
		public Object getDesiredValue() { return desiredValue; }
	}

	/** The value for boolean options. */		public static final int BOOLEAN = 0;
	/** The value for integer options. */		public static final int INTEGER = 1;
	/** The value for long options. */			public static final int LONG    = 2;
	/** The value for double options. */		public static final int DOUBLE  = 3;
	/** The value for string options. */		public static final int STRING  = 4;

	private   String      name;
	private   int         type;
	private   Preferences prefs;
	private   Meaning     meaning;
	protected Object      cachedObj;
	protected Object      factoryObj;

	private static List allPrefs = new ArrayList();
	private static Map meaningVariablesThatChanged;

	/**
	 * The constructor for the Pref.
	 */
	protected Pref() {}

	/**
	 * Factory methods to create a boolean Pref objects.
	 * The proper way to create a boolean Pref is with makeBooleanPref;
	 * use of this method is only for subclasses.
	 * @param name the name of this Pref.
	 * @param prefs the actual java.util.prefs.Preferences object to use for this Pref.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	protected void initBoolean(String name, Preferences prefs, boolean factory)
	{
		this.name = name;
		this.prefs = prefs;
		this.type = BOOLEAN;
		this.meaning = null;
		this.factoryObj = new Integer(factory ? 1 : 0);
		if (prefs != null) this.cachedObj = new Integer(prefs.getBoolean(name, factory) ? 1 : 0); else
			this.cachedObj = new Integer(factory ? 1 : 0);
		allPrefs.add(this);
	}
	/**
	 * Factory methods to create a boolean Pref objects.
	 * @param name the name of this Pref.
	 * @param prefs the actual java.util.prefs.Preferences object to use for this Pref.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static Pref makeBooleanPref(String name, Preferences prefs, boolean factory)
	{
		Pref pref = new Pref();
		pref.initBoolean(name, prefs, factory);
		return pref;
	}

	/**
	 * Factory methods to create an integer Pref objects.
	 * The proper way to create an integer Pref is with makeIntPref;
	 * use of this method is only for subclasses.
	 * @param name the name of this Pref.
	 * @param prefs the actual java.util.prefs.Preferences object to use for this Pref.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	protected void initInt(String name, Preferences prefs, int factory)
	{
		this.name = name;
		this.prefs = prefs;
		this.type = INTEGER;
		this.meaning = null;
		this.factoryObj = new Integer(factory);
		if (prefs != null) this.cachedObj = new Integer(prefs.getInt(name, factory)); else
			this.cachedObj = new Integer(factory);
		allPrefs.add(this);
	}
	/**
	 * Factory methods to create an integer Pref objects.
	 * @param name the name of this Pref.
	 * @param prefs the actual java.util.prefs.Preferences object to use for this Pref.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static Pref makeIntPref(String name, Preferences prefs, int factory)
	{
		Pref pref = new Pref();
		pref.initInt(name, prefs, factory);
		return pref;
	}

	/**
	 * Factory methods to create a long Pref objects.
	 * The proper way to create a long Pref is with makeLongPref;
	 * use of this method is only for subclasses.
	 * @param name the name of this Pref.
	 * @param prefs the actual java.util.prefs.Preferences object to use for this Pref.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	protected void initLong(String name, Preferences prefs, long factory)
	{
		this.name = name;
		this.prefs = prefs;
		this.type = LONG;
		this.meaning = null;
		this.factoryObj = new Long(factory);
		if (prefs != null) this.cachedObj = new Long(prefs.getLong(name, factory)); else
			this.cachedObj = new Long(factory);
		allPrefs.add(this);
	}
	/**
	 * Factory methods to create a long Pref objects.
	 * @param name the name of this Pref.
	 * @param prefs the actual java.util.prefs.Preferences object to use for this Pref.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static Pref makeLongPref(String name, Preferences prefs, long factory)
	{
		Pref pref = new Pref();
		pref.initLong(name, prefs, factory);
		return pref;
	}

	/**
	 * Factory methods to create a double Pref objects.
	 * The proper way to create a double Pref is with makeDoublePref;
	 * use of this method is only for subclasses.
	 * @param name the name of this Pref.
	 * @param prefs the actual java.util.prefs.Preferences object to use for this Pref.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	protected void initDouble(String name, Preferences prefs, double factory)
	{
		this.name = name;
		this.prefs = prefs;
		this.type = DOUBLE;
		this.meaning = null;
		this.factoryObj = new Double(factory);
		if (prefs != null) this.cachedObj = new Double(prefs.getDouble(name, factory)); else
			this.cachedObj = new Double(factory);
		allPrefs.add(this);
	}
	/**
	 * Factory methods to create a double Pref objects.
	 * @param name the name of this Pref.
	 * @param prefs the actual java.util.prefs.Preferences object to use for this Pref.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static Pref makeDoublePref(String name, Preferences prefs, double factory)
	{
		Pref pref = new Pref();
		pref.initDouble(name, prefs, factory);
		return pref;
	}

	/**
	 * Factory methods to create a string Pref objects.
	 * The proper way to create a string Pref is with makeStringPref;
	 * use of this method is only for subclasses.
	 * @param name the name of this Pref.
	 * @param prefs the actual java.util.prefs.Preferences object to use for this Pref.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	protected void initString(String name, Preferences prefs, String factory)
	{
		this.name = name;
		this.prefs = prefs;
		this.type = STRING;
		this.meaning = null;
		this.factoryObj = new String(factory);
		if (prefs != null) this.cachedObj = new String(prefs.get(name, factory)); else
			this.cachedObj = new String(factory);
		allPrefs.add(this);
	}

	/**
	 * Factory methods to create a string Pref objects.
	 * @param name the name of this Pref.
	 * @param prefs the actual java.util.prefs.Preferences object to use for this Pref.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static Pref makeStringPref(String name, Preferences prefs, String factory)
	{
		Pref pref = new Pref();
		pref.initString(name, prefs, factory);
		return pref;
	}

	/**
	 * Method to get the boolean value on this Pref object.
	 * The object must have been created as "boolean".
	 * @return the boolean value on this Pref object.
	 */
	public boolean getBoolean() { return ((Integer)cachedObj).intValue() != 0; }

	/**
	 * Method to get the integer value on this Pref object.
	 * The object must have been created as "integer".
	 * @return the integer value on this Pref object.
	 */
	public int getInt() { return ((Integer)cachedObj).intValue(); }

	/**
	 * Method to get the long value on this Pref object.
	 * The object must have been created as "long".
	 * @return the long value on this Pref object.
	 */
	public long getLong() { return ((Long)cachedObj).longValue(); }

	/**
	 * Method to get the double value on this Pref object.
	 * The object must have been created as "double".
	 * @return the double value on this Pref object.
	 */
	public double getDouble() { return ((Double)cachedObj).doubleValue(); }

	/**
	 * Method to get the string value on this Pref object.
	 * The object must have been created as "string".
	 * @return the string value on this Pref object.
	 */
	public String getString() { return (String)cachedObj; }

	/**
	 * Method to get the factory-default value of this Pref object.
	 * @return the factory-default value of this Pref object.
	 */
	public Object getFactoryValue() { return factoryObj; }

	/**
	 * Method to get the factory-default boolean value of this Pref object.
	 * @return the factory-default boolean value of this Pref object.
	 */
	public boolean getBooleanFactoryValue() { return ((Integer)factoryObj).intValue() != 0 ? true : false; }

	/**
	 * Method to get the factory-default integer value of this Pref object.
	 * @return the factory-default integer value of this Pref object.
	 */
	public int getIntFactoryValue() { return ((Integer)factoryObj).intValue(); }

	/**
	 * Method to get the factory-default long value of this Pref object.
	 * @return the factory-default long value of this Pref object.
	 */
	public long getLongFactoryValue() { return ((Long)factoryObj).longValue(); }

	/**
	 * Method to get the factory-default double value of this Pref object.
	 * @return the factory-default double value of this Pref object.
	 */
	public double getDoubleFactoryValue() { return ((Double)factoryObj).doubleValue(); }

	/**
	 * Method to get the factory-default String value of this Pref object.
	 * @return the factory-default String value of this Pref object.
	 */
	public String getStringFactoryValue() { return (String)factoryObj; }

	/**
	 * Method to get the name of this Pref object.
	 * @return the name of this Pref object.
	 */
	public String getPrefName() { return name; }

	/**
	 * Method to get the value of this Pref object as an Object.
	 * The proper way to get the current value is to use one of the type-specific
	 * methods such as getInt(), getBoolean(), etc.
	 * @return the Object value of this Pref object.
	 */
	public Object getValue() { return cachedObj; }

	/**
	 * Method to get the type of this Pref object.
	 * @return an integer type: either BOOLEAN, INTEGER, LONG, DOUBLE, or STRING.
	 */
	public int getType() { return type; }

	/**
	 * Method to get the Meaning associated with this Pref object.
	 * Not all Pref objects have a meaning, and those that don't are not
	 * meaning options, but instead are appearance options.
	 * @return the Meaning associated with this Pref object.
	 * Returns null if this Pref is not a meaning option.
	 */
	public Meaning getMeaning() { return meaning; }

	/**
	 * Method called when this Pref is changed.
	 * This method is overridden in subclasses that want notification.
	 */
	public void setSideEffect() {}

	/**
	 * Method to set a new boolean value on this Pref object.
	 * @param v the new boolean value of this Pref object.
	 */
	public void setBoolean(boolean v)
	{
		boolean cachedBool = ((Integer)cachedObj).intValue() != 0 ? true : false;
		if (v != cachedBool)
		{
			cachedObj = new Integer(v ? 1 : 0);
			if (prefs != null)
			{
				prefs.putBoolean(name, v);
				flushOptions();
			}
		}
		setSideEffect();
	}

	/**
	 * Method to set a new integer value on this Pref object.
	 * @param v the new integer value of this Pref object.
	 */
	public void setInt(int v)
	{
		int cachedInt = ((Integer)cachedObj).intValue();
		if (v != cachedInt)
		{
			cachedObj = new Integer(v);
			if (prefs != null)
			{
				prefs.putInt(name, v);
				flushOptions();
			}
		}
		setSideEffect();
	}

	/**
	 * Method to set a new long value on this Pref object.
	 * @param v the new long value of this Pref object.
	 */
	public void setLong(long v)
	{
		long cachedLong = ((Long)cachedObj).longValue();
		if (v != cachedLong)
		{
			cachedObj = new Long(v);
			if (prefs != null)
			{
				prefs.putLong(name, v);
				flushOptions();
			}
		}
		setSideEffect();
	}

	/**
	 * Method to set a new double value on this Pref object.
	 * @param v the new double value of this Pref object.
	 * @return true if preference was really changed.
	 */
	public boolean setDouble(double v)
	{
		double cachedDouble = ((Double)cachedObj).doubleValue();
		boolean changed = false;

		if (v != cachedDouble)
		{
			cachedObj = new Double(v);
			if (prefs != null)
			{
				prefs.putDouble(name, v);
				flushOptions();
			}
			changed = true;
		}
		setSideEffect();
		return (changed);
	}

	/**
	 * Method to set a new string value on this Pref object.
	 * @param str the new string value of this Pref object.
	 */
	public void setString(String str)
	{
		String cachedString = (String)cachedObj;
		if (!str.equals(cachedString))
		{
			cachedObj = new String(str);
			if (prefs != null)
			{
				prefs.put(name, str);
				flushOptions();
			}
		}
		setSideEffect();
	}

//    private static Map meaningPrefs = new HashMap();

	/**
	 * Method to make this Pref a "meaning" option.
	 * Meaning options are attached to Technology and Tool objects,
	 * and are saved with libraries.
	 * @param ownerObj the Object to attach this Pref to.
	 * @param location the user-command that can affect this meaning option.
	 * @param description the description of this meaning option.
	 * @return a Meaning object, now associated with this Pref object, that
	 * gives the option meaning.
	 */
	public Meaning attachToObject(Object ownerObj, String location, String description)
	{
		if (meaning == null)
		{
			meaning = new Meaning(ownerObj, this, location, description);
		} else {
			System.out.println("Meaning " + name + " already attached to " + ownerObj);
		}
 //       List list = (List)meaningPrefs.get(this.name);
 //       if (list == null) { list = new ArrayList(); }
 //       list.add(meaning);
		return meaning;
	}

	/**
	 * Method to find the Meaning object associated with a given part of the
	 * Electric database.
	 * @param ownerObj the Object on which to find a Meaning object.
	 * @param name the name of the desired Meaning object.
	 * @return the Meaning object on that part of the database.
	 */
	public static Meaning getMeaningVariable(Object ownerObj, String name)
	{
/*
        List list = (List)meaningPrefs.get(name);
        if (list == null) return null;
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            Meaning m = (Meaning)it.next();
            if (m.eObj == eObj) return m;
        }
*/
		for(Iterator it = allPrefs.iterator(); it.hasNext(); )
		{
			Pref pref = (Pref)it.next();
			if (pref.meaning == null) continue;
			if (pref.meaning.ownerObj != ownerObj) continue;
			if (pref.name.equals(name))
			{
				return pref.meaning;
			}
		}
		return null;
	}

	/**
	 * Method to get a list of "meaning" options assiciatiated with the given
	 * owner object or list of all "meaning" options, if object in not given
	 * @param ownerObj owner object, or null
	 * @return a list of "meaning" option
	 */
	public static List getMeaningVariables(Object ownerObj)
	{
		ArrayList prefs = new ArrayList();
		for(Iterator it = allPrefs.iterator(); it.hasNext(); )
		{
			Pref pref = (Pref)it.next();
			if (pref.meaning == null) continue;
			if (ownerObj != null && pref.meaning.ownerObj != ownerObj) continue;
			if (pref.cachedObj.equals(pref.factoryObj)) continue;
//System.out.println("Saving meaning variable "+pref.name+" on " + pref.meaning.ownerObj);
//System.out.println("   Current value="+pref.cachedObj+" factory value=" + pref.factoryObj);
			prefs.add(pref);
		}
		Collections.sort(prefs, new TextUtils.PrefsByName());
		return prefs;
	}

	/**
	 * Method to store all changed "meaning" options in the database.
	 * This is done before saving a library, so that the options related to that
	 * library get saved as well.
	 */
// 	public static void installMeaningVariables()
// 	{
// 		for(Iterator it = allPrefs.iterator(); it.hasNext(); )
// 		{
// 			Pref pref = (Pref)it.next();
// 			if (pref.meaning == null) continue;
// 			Variable.Key key = ElectricObject.newKey(pref.name);
// 			if (pref.cachedObj.equals(pref.factoryObj))
// 			{
// 				pref.meaning.eObj.delVar(key);
// 				continue;
// 			}
// 			pref.meaning.eObj.newVar(key, pref.cachedObj);
// 		}
// 	}

	/**
	 * Method to start the collection of meaning options that have changed.
	 * After this, calls to changedMeaningVariable() will be collected for
	 * reconciliation.
	 * This is typically done during library input, to help reconcile option
	 * changes caused by the library input with prior option values.
	 */
	public static void initMeaningVariableGathering()
	{
		meaningVariablesThatChanged = new HashMap();
	}

	/**
	 * Method to record a Meaning option was altered.
	 * This happens during library input, where changed meaning options
	 * must be reconciled with existing values.
	 * @param meaning the Meaning option that was altered.
	 */
	public static void changedMeaningVariable(Meaning meaning, Object value)
	{
		meaningVariablesThatChanged.put(meaning, value);
	}

	/**
	 * Method to adjust "meaning" options that were saved with a library.
	 * Presents the user with a dialog to help reconcile the difference
	 * between meaning options stored in a library and the original values.
	 */
	public static void reconcileMeaningVariables(String libName)
	{
		for(Iterator it = allPrefs.iterator(); it.hasNext(); )
		{
			Pref pref = (Pref)it.next();
			if (pref.meaning == null) continue;
			pref.meaning.marked = false;
		}
		List meaningsToReconcile = new ArrayList();
		for(Iterator it = meaningVariablesThatChanged.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry entry = (Map.Entry)it.next();
			Meaning meaning = (Meaning)entry.getKey();
			Object value = entry.getValue();
			meaning.marked = true;
			if (DBMath.objectsReallyEqual(value, meaning.pref.cachedObj)) continue;
			meaning.setDesiredValue(value);
			if (!meaning.isValidOption()) continue;
//System.out.println("Meaning variable "+meaning.pref.name+" found on " + meaning.ownerObj+" is "+value+" but is cached as "+meaning.pref.cachedObj);
			meaningsToReconcile.add(meaning);
		}
		for(Iterator it = allPrefs.iterator(); it.hasNext(); )
		{
			Pref pref = (Pref)it.next();
			if (pref.meaning == null) continue;
			if (pref.meaning.marked) continue;

			// this one is not mentioned in the library: make sure it is at factory defaults
			if (DBMath.objectsReallyEqual(pref.cachedObj, pref.factoryObj)) continue;

//System.out.println("Adding fake meaning variable "+pref.name+" where current="+pref.cachedObj+" but should be "+pref.factoryObj);
			pref.meaning.setDesiredValue(pref.factoryObj);
			if (!pref.meaning.isValidOption()) continue;
			meaningsToReconcile.add(pref.meaning);
		}

		if (meaningsToReconcile.size() == 0) return;
 		OptionReconcile dialog = new OptionReconcile(TopLevel.getCurrentJFrame(), true, meaningsToReconcile, libName);

		if (Main.BATCHMODE)
			dialog.termDialog();
		else
			dialog.setVisible(true);
	}

	/****************************** private methods ******************************/

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
