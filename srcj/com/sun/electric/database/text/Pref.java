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

import com.sun.electric.Main;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.projectSettings.ProjSettingsNode;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

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
    public static class Group {
        private Preferences prefs;
        Group(Preferences prefs) { this.prefs = prefs; }
    }

    public static Group groupForPackage(Class classFromPackage) {
        Preferences prefs = Preferences.userNodeForPackage(classFromPackage);
        return new Group(prefs);
    }

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
			key = Variable.newKey(pref.name);
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

    public enum PrefType {
	/** The value for boolean options. */		BOOLEAN,
	/** The value for integer options. */		INTEGER,
	/** The value for long options. */			LONG,
	/** The value for double options. */		DOUBLE,
	/** The value for string options. */		STRING;
    }

	private   final String      name;
	private   PrefType         type;
	private   final Preferences prefs;
	private   Meaning     meaning;
	private   Object      cachedObj;
	private   Object      factoryObj;
    private   boolean changed = false;

	private static final List<Pref> allPrefs = new ArrayList<Pref>();
	private static boolean doFlushing = true;
    private static PrefChangeBatch changes = null;
	private static Set<Preferences> queueForFlushing;
    private static boolean allPreferencesCreated = false;

    /**
	 * The constructor for the Pref.
	 * @param name the name of this Pref.
	 */
	protected Pref(Preferences prefs, String name) {
//        if (allPreferencesCreated)
//            throw new IllegalStateException("no more preferences");
        this.name = name;
        this.prefs = prefs;
        synchronized (allPrefs) {
            allPrefs.add(this);
        }
    }

	/**
	 * The constructor for the Pref.
	 * @param name the name of this Pref.
	 */
	protected Pref(Group group, String name) {
//        if (allPreferencesCreated)
//            throw new IllegalStateException("no more preferences");
        this.name = name;
        this.prefs = group.prefs;
        synchronized (allPrefs) {
            allPrefs.add(this);
        }
    }

    /**
     * Method used in regressions so it has to be public.
     * @param fileName
     */
    public static void importPrefs(String fileName)
    {
        if (fileName == null) return;

        // import preferences
        importPrefs(TextUtils.makeURLToFile(fileName));
    }

    public static void importPrefs(URL fileURL)
    {
        if (fileURL == null) return;

        // import preferences
        try
		{
            URLConnection urlCon = fileURL.openConnection();
			InputStream inputStream = urlCon.getInputStream();
			System.out.println("Importing preferences...");

			// reset all preferences to factory values
			delayPrefFlushing();
            synchronized (allPrefs) {
                for(Pref pref : allPrefs) {
                    switch (pref.type) {
                        case BOOLEAN:
                            if (pref.getBoolean() != pref.getBooleanFactoryValue())
                                pref.setBoolean(pref.getBooleanFactoryValue());
                            break;
                        case INTEGER:
                            if (pref.getInt() != pref.getIntFactoryValue())
                                pref.setInt(pref.getIntFactoryValue());
                            break;
                        case LONG:
                            if (pref.getLong() != pref.getLongFactoryValue())
                                pref.setLong(pref.getLongFactoryValue());
                            break;
                        case DOUBLE:
                            if (pref.getDouble() != pref.getDoubleFactoryValue())
                                pref.setDouble(pref.getDoubleFactoryValue());
                            break;
                        case STRING:
                            if (!pref.getString().equals(pref.getStringFactoryValue()))
                                pref.setString(pref.getStringFactoryValue());
                            break;
                    }
                }
            }
			resumePrefFlushing();

			// import preferences
			Preferences.importPreferences(inputStream);
			inputStream.close();

			// recache all prefs
            synchronized (allPrefs) {
                for(Pref pref : allPrefs) {
                    switch (pref.type) {
                        case BOOLEAN:
//                            boolean curBoolean = pref.prefs.getBoolean(pref.name, pref.getBooleanFactoryValue());
//                            pref.cachedObj = new Integer(curBoolean ? 1 : 0);
                            pref.setBoolean(pref.prefs.getBoolean(pref.name, pref.getBooleanFactoryValue()));
                            break;
                        case INTEGER:
                            pref.setInt(pref.prefs.getInt(pref.name, pref.getIntFactoryValue()));
//                            pref.cachedObj = new Integer(pref.prefs.getInt(pref.name, pref.getIntFactoryValue()));
                            break;
                        case LONG:
                            pref.setLong(pref.prefs.getLong(pref.name, pref.getLongFactoryValue()));
//                            pref.cachedObj = new Long(pref.prefs.getLong(pref.name, pref.getLongFactoryValue()));
                            break;
                        case DOUBLE:
//                            pref.cachedObj = new Double(pref.prefs.getDouble(pref.name, pref.getDoubleFactoryValue()));
                            pref.setDouble(pref.prefs.getDouble(pref.name, pref.getDoubleFactoryValue()));
                            break;
                        case STRING:
                            pref.setString(pref.prefs.get(pref.name, pref.getStringFactoryValue()));
//                            pref.cachedObj = pref.prefs.get(pref.name, pref.getStringFactoryValue());
                            break;
                    }
                }
            }

			// recache technology color information
            Technology.cacheTransparentLayerColors();
		} catch (InvalidPreferencesFormatException e)
		{
			System.out.println("Invalid preferences format");
			return;
		} catch (IOException e)
		{
			System.out.println("Error reading preferences file");
            e.printStackTrace();
			return;
		}

        Job.getExtendedUserInterface().restoreSavedBindings(false);
        Job.getUserInterface().repaintAllEditWindows();
		System.out.println("...preferences imported from " + fileURL.getFile());
	}

	/**
	 * Method to export the preferences to an XML file. This function is public due to the regressions.
	 * @param fileName the file to write.
	 */
    public static void exportPrefs(String fileName)
    {
        if (fileName == null) return;

        // save preferences there
        try
		{
        	// dump the preferences as a giant XML string (unformatted)
			Preferences root = Preferences.userNodeForPackage(Main.class);
			ByteArrayOutputStream bs = new ByteArrayOutputStream();
			root.exportSubtree(bs);
			String xmlDump = bs.toString();

			// remove the DTD statement (causes trouble)
			int sunPos = xmlDump.indexOf("java.sun.com");
			String insertDTD = "";
			if (sunPos >= 0)
			{
				int openPos = xmlDump.lastIndexOf('<', sunPos);
				int closePos = xmlDump.indexOf('>', sunPos);
				if (openPos >= 0 && closePos >= 0)
				{
					insertDTD = xmlDump.substring(openPos, closePos+1);
					xmlDump = xmlDump.substring(0, openPos) + xmlDump.substring(closePos+1);
				}
			}

			// reformat the XML
			StreamSource source = new StreamSource(new StringReader(xmlDump));
	    	TransformerFactory factory = TransformerFactory.newInstance();
	    	factory.setAttribute("indent-number", new Integer(2));
	    	Transformer transformer = factory.newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			StreamResult result = new StreamResult(new OutputStreamWriter(bos, "utf-8"));
			transformer.transform(source, result);

			// add the removed DTD line back into the XML
			String xmlFormatted = bos.toString();
			int closePos = xmlFormatted.indexOf('>');
			if (closePos >= 0)
				xmlFormatted = xmlFormatted.substring(0, closePos+1) + "\n" + insertDTD + xmlFormatted.substring(closePos+1);

			// save the XML to disk
			PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
			printWriter.print(xmlFormatted);
			printWriter.close();

//			Preferences root = Preferences.userNodeForPackage(Main.class);
//			FileOutputStream outputStream = new PrivateFileOutputStream(fileName);
//			root.exportSubtree(outputStream);
//			outputStream.close();
		} catch (Exception e)
		{
            if (Job.getDebug())
                e.printStackTrace();
			System.out.println("Error exporting Preferences");
			return;
		}

		System.out.println("Preferences saved to " + fileName);
	}

	/**
	 * Factory methods to create a boolean Pref objects.
	 * The proper way to create a boolean Pref is with makeBooleanPref;
	 * use of this method is only for subclasses.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	protected void initBoolean(boolean factory)
	{
		this.type = PrefType.BOOLEAN;
//		this.meaning = null;
		this.factoryObj = new Integer(factory ? 1 : 0);
		if (prefs != null) this.cachedObj = new Integer(prefs.getBoolean(name, factory) ? 1 : 0); else
			this.cachedObj = new Integer(factory ? 1 : 0);
	}

	/**
	 * Factory methods to create a boolean Pref objects.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param factory the "factory" default value (if nothing is stored).
	 */
    public static Pref makeBooleanPref(String name, Group group, boolean factory) {
		Pref pref = new Pref(group.prefs, name);
		pref.initBoolean(factory);
		return pref;
	}

	/**
	 * Factory methods to create a boolean project setting objects.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param ownerObj the Object to attach this Pref to.
	 * @param location the user-command that can affect this meaning option.
	 * @param description the description of this meaning option.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
    public static Pref makeBooleanSetting(String name, Group group, Object ownerObj,
                                          ProjSettingsNode xmlNode, String xmlName,
                                          String location, String description, boolean factory) {
        Pref pref = makeBooleanPref(name, group, factory);
        pref.attachToObject(ownerObj, location, description);
        pref.linkProjectSettings(xmlNode, name, xmlName);
        return pref;
    }

	/**
	 * Factory methods to create an integer Pref objects.
	 * The proper way to create an integer Pref is with makeIntPref;
	 * use of this method is only for subclasses.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	protected void initInt(int factory)
	{
		this.type = PrefType.INTEGER;
//		this.meaning = null;
		this.factoryObj = new Integer(factory);
		if (prefs != null) this.cachedObj = new Integer(prefs.getInt(name, factory)); else
			this.cachedObj = new Integer(factory);
	}

	/**
	 * Factory methods to create an integer Pref objects.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static Pref makeIntPref(String name, Group group, int factory) {
		Pref pref = new Pref(group.prefs, name);
		pref.initInt(factory);
		return pref;
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
    public static Pref makeIntSetting(String name, Group group, Object ownerObj,
                                      ProjSettingsNode xmlNode, String xmlName,
                                      String location, String description, int factory) {
        Pref pref = makeIntPref(name, group, factory);
        pref.attachToObject(ownerObj, location, description);
        pref.linkProjectSettings(xmlNode, name, xmlName);
        return pref;
    }

	/**
	 * Factory methods to create a long Pref objects.
	 * The proper way to create a long Pref is with makeLongPref;
	 * use of this method is only for subclasses.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	protected void initLong(long factory)
	{
		this.type = PrefType.LONG;
//		this.meaning = null;
		this.factoryObj = new Long(factory);
		if (prefs != null) this.cachedObj = new Long(prefs.getLong(name, factory)); else
			this.cachedObj = new Long(factory);
	}

	/**
	 * Factory methods to create a long Pref objects.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static Pref makeLongPref(String name, Group group, long factory) {
		Pref pref = new Pref(group.prefs, name);
		pref.initLong(factory);
		return pref;
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
    public static Pref makeLongSetting(String name, Group group, Object ownerObj,
                                       ProjSettingsNode xmlNode, String xmlName,
                                       String location, String description, long factory) {
        Pref pref = makeLongPref(name, group, factory);
        pref.attachToObject(ownerObj, location, description);
        pref.linkProjectSettings(xmlNode, name, xmlName);
        return pref;
    }

	/**
	 * Factory methods to create a double Pref objects.
	 * The proper way to create a double Pref is with makeDoublePref;
	 * use of this method is only for subclasses.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	protected void initDouble(double factory)
	{
		this.type = PrefType.DOUBLE;
//		this.meaning = null;
		this.factoryObj = new Double(factory);
		if (prefs != null) this.cachedObj = new Double(prefs.getDouble(name, factory)); else
			this.cachedObj = new Double(factory);
	}

	/**
	 * Factory methods to create a double Pref objects.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static Pref makeDoublePref(String name, Group group, double factory) {
		Pref pref = new Pref(group.prefs, name);
		pref.initDouble(factory);
		return pref;
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
    public static Pref makeDoubleSetting(String name, Group group, Object ownerObj,
                                         ProjSettingsNode xmlNode, String xmlName,
                                         String location, String description, double factory) {
        Pref pref = makeDoublePref(name, group, factory);
        pref.attachToObject(ownerObj, location, description);
        pref.linkProjectSettings(xmlNode, name, xmlName);
        return pref;
    }

	/**
	 * Factory methods to create a string Pref objects.
	 * The proper way to create a string Pref is with makeStringPref;
	 * use of this method is only for subclasses.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	protected void initString(String factory)
	{
		this.type = PrefType.STRING;
//		this.meaning = null;
		this.factoryObj = new String(factory);
		if (prefs != null) this.cachedObj = new String(prefs.get(name, factory)); else
			this.cachedObj = new String(factory);
	}

	/**
	 * Factory methods to create a string Pref objects.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static Pref makeStringPref(String name, Group group, String factory) {
		Pref pref = new Pref(group.prefs, name);
		pref.initString(factory);
		return pref;
	}

	public static Pref makeStringPref(String name, Preferences prefs, String factory) {
		Pref pref = new Pref(prefs, name);
		pref.initString(factory);
		return pref;
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
    public static Pref makeStringSetting(String name, Group group, Object ownerObj,
                                         ProjSettingsNode xmlNode, String xmlName,
                                         String location, String description, String factory) {
        Pref pref = makeStringPref(name, group, factory);
        pref.attachToObject(ownerObj, location, description);
        pref.linkProjectSettings(xmlNode, name, xmlName);
        return pref;
    }

    protected void linkProjectSettings(ProjSettingsNode node, String name, String xmlName) {
        if (node == null) return;
        if (xmlName == null) xmlName = name;
        node.putValue(xmlName, this);
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
	public PrefType getType() { return type; }

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
	protected void setSideEffect() {}

	public static class PrefChangeBatch implements Serializable
	{
        private HashMap<String,HashMap<String,Object>> changesForNodes = new HashMap<String,HashMap<String,Object>>();

        private void add(Pref pref, Object newValue) {
            String nodeName = pref.prefs.absolutePath();
            HashMap<String,Object> changesForTheNode = changesForNodes.get(nodeName);
            if (changesForTheNode == null) {
                changesForTheNode = new HashMap<String,Object>();
                changesForNodes.put(nodeName, changesForTheNode);
            }
            changesForTheNode.put(pref.name, newValue);
        }
	}

	/**
	 * Method to start accumulation of Pref changes.
	 * All changes to preferences after this call are gathered,
	 * and not actually implemented.
	 * Call "getPrefChanges()" to get the gathered changes, and call
	 * "implementPrefChanges()" to actually make the changes.
	 */
	public static void gatherPrefChanges()
	{
	}

	/**
	 * Method to get the accumulated Pref changes.
	 * In order to make preference changes on the server,
	 * it is necessary to gather them on the client, and send
	 * the changes to the server for actual change.
	 * This method runs on the client and gets a serializable
	 * object that can be sent to the server.
	 * @return a collection of changes to preferences that have
	 * been made since the call to "gatherPrefChanges()".
	 * Call "implementPrefChanges()" with the returned collection
	 * to actually make the changes.
	 */
	public static PrefChangeBatch getPrefChanges()
	{
		return null;
	}

	/**
	 * Method to make a collection of preference changes.
	 * In order to make preference changes on the server,
	 * it is necessary to gather them on the client, and send
	 * the changes to the server for actual change.
	 * This method runs on the server.
	 * @param obj the collection of preference changes.
	 */
	public static void implementPrefChanges(PrefChangeBatch obj)
	{
	}

    /**
     * Mark all preferences as "unchanged"
     */
    public static void clearChangedAllPrefs() {
        synchronized (allPrefs) {
            for (Pref pref : allPrefs)
                pref.changed = false;
        }
    }

    /**
     * Return true if any pref has changed since the last
     * call to clearChangedAllPrefs()
     * @return true if any pref has changed since changes were cleared
     */
    public static boolean anyPrefChanged() {
        boolean changed = false;
        synchronized (allPrefs) {
            for (Pref pref : allPrefs) {
                if (pref.changed) {
                    changed = true; break;
                }
            }
        }
        return changed;
    }

    /**
	 * Method to delay the saving of preferences to disk.
	 * Since individual saving is time-consuming, batches of preference
	 * changes are wrapped with this, and "resumePrefFlushing()".
	 */
	public static void delayPrefFlushing()
	{
		doFlushing = false;
		queueForFlushing = new HashSet<Preferences>();
	}

	/**
	 * Method to resume the saving of preferences to disk.
	 * Since individual saving is time-consuming, batches of preference
	 * changes are wrapped with this, and "resumePrefFlushing()".
	 * Besides resuming saving, this method also saves anything queued
	 * while saving was delayed.
	 */
	public static void resumePrefFlushing()
	{
		doFlushing = true;
		for(Preferences p : queueForFlushing)
			flushOptions(p);
	}

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
            changed = true;
            if (prefs != null)
			{
				prefs.putBoolean(name, v);
				if (doFlushing) flushOptions(prefs); else
					queueForFlushing.add(prefs);
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
            changed = true;
			if (prefs != null)
			{
				prefs.putInt(name, v);
				if (doFlushing) flushOptions(prefs); else
					queueForFlushing.add(prefs);
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
            changed = true;
			if (prefs != null)
			{
				prefs.putLong(name, v);
				if (doFlushing) flushOptions(prefs); else
					queueForFlushing.add(prefs);
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
            this.changed = true;
			if (prefs != null)
			{
				prefs.putDouble(name, v);
				if (doFlushing) flushOptions(prefs); else
					queueForFlushing.add(prefs);
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
            changed = true;
            if (prefs != null)
			{
				prefs.put(name, str);
				if (doFlushing) flushOptions(prefs); else
					queueForFlushing.add(prefs);
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
        List list = meaningPrefs.get(name);
        if (list == null) return null;
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            Meaning m = it.next();
            if (m.eObj == eObj) return m;
        }
*/
        synchronized (allPrefs) {
            for(Pref pref : allPrefs) {
                if (pref.meaning == null) continue;
                if (pref.meaning.ownerObj != ownerObj) continue;
                if (pref.name.equals(name)) {
                    return pref.meaning;
                }
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
	public static List<Pref> getMeaningVariables(Object ownerObj)
	{
		ArrayList<Pref> prefs = new ArrayList<Pref>();
        synchronized (allPrefs) {
            for(Pref pref : allPrefs) {
                if (pref.meaning == null) continue;
                if (!pref.meaning.isValidOption()) continue;
                if (ownerObj != null && pref.meaning.ownerObj != ownerObj) continue;
                if (pref.cachedObj.equals(pref.factoryObj)) continue;
//System.out.println("Saving meaning variable "+pref.name+" on " + pref.meaning.ownerObj);
//System.out.println("   Current value="+pref.cachedObj+" factory value=" + pref.factoryObj);
                prefs.add(pref);
            }
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
// 			Pref pref = it.next();
// 			if (pref.meaning == null) continue;
// 			Variable.Key key = Variable.newKey(pref.name);
// 			if (pref.cachedObj.equals(pref.factoryObj))
// 			{
// 				pref.meaning.eObj.delVar(key);
// 				continue;
// 			}
// 			pref.meaning.eObj.newVar(key, pref.cachedObj);
// 		}
// 	}

	/**
	 * Method to adjust "meaning" options that were saved with a library.
	 * Presents the user with a dialog to help reconcile the difference
	 * between meaning options stored in a library and the original values.
	 */
	public static void reconcileMeaningVariables(String libName, Map<Object,Map<String,Object>> meaningVariables)
	{
        synchronized (allPrefs) {
            for(Pref pref : allPrefs) {
                if (pref.meaning == null) continue;
                pref.meaning.marked = false;
            }
        }
		List<Meaning> meaningsToReconcile = new ArrayList<Meaning>();
        for (Object obj: meaningVariables.keySet()) {
            Map<String,Object> meanings = meaningVariables.get(obj);
            for (String prefName: meanings.keySet()) {
                Pref.Meaning meaning = Pref.getMeaningVariable(obj, prefName);
                if (meaning == null) continue;
                Object value = meanings.get(prefName);
                meaning.marked = true;
                if (DBMath.objectsReallyEqual(value, meaning.pref.cachedObj)) continue;
                meaning.setDesiredValue(value);
                if (!meaning.isValidOption()) continue;
//System.out.println("Meaning variable "+meaning.pref.name+" found on " + meaning.ownerObj+" is "+value+" but is cached as "+meaning.pref.cachedObj);
                meaningsToReconcile.add(meaning);
            }
		}
        synchronized (allPrefs) {
            for(Pref pref : allPrefs) {
                if (pref.meaning == null) continue;
                if (pref.meaning.marked) continue;

                // this one is not mentioned in the library: make sure it is at factory defaults
                if (DBMath.objectsReallyEqual(pref.cachedObj, pref.factoryObj)) continue;

//System.out.println("Adding fake meaning variable "+pref.name+" where current="+pref.cachedObj+" but should be "+pref.factoryObj);
                pref.meaning.setDesiredValue(pref.factoryObj);
                if (!pref.meaning.isValidOption()) continue;
                meaningsToReconcile.add(pref.meaning);
            }
        }

		if (meaningsToReconcile.size() == 0) return;

        Job.getExtendedUserInterface().finishPrefReconcilation(libName, meaningsToReconcile);
//		if (Job.BATCHMODE)
//		{
//            finishPrefReconcilation(meaningsToReconcile);
//			return;
//		}
// 		OptionReconcile dialog = new OptionReconcile(TopLevel.getCurrentJFrame(), true, meaningsToReconcile, libName);
//		dialog.setVisible(true);
	}

    /**
     * This method is called after reconciling Prefs with OptionReconcile dialog or in a batch mode
     */
    public static void finishPrefReconcilation(List<Meaning> meaningsToReconcile)
    {
    	// delay flushing of preferences until all chanages are made
		delayPrefFlushing();
        for(Meaning meaning : meaningsToReconcile)
        {
            Pref pref = meaning.getPref();
            Object obj = meaning.getDesiredValue();

            // set the option
            switch (pref.getType())
            {
                case BOOLEAN: pref.setBoolean(((Integer)obj).intValue() != 0);   break;
                case INTEGER: pref.setInt(((Integer)obj).intValue());            break;
                case DOUBLE:
                    if (obj instanceof Double) pref.setDouble(((Double)obj).doubleValue()); else
                        if (obj instanceof Float) pref.setDouble((double)((Float)obj).floatValue());
                    break;
                case STRING:  pref.setString((String)obj);                       break;
                default: continue;
            }
            System.out.println("Project Setting "+meaning.pref.name+" on " + meaning.ownerObj+" changed to "+obj);
        }

        // resume flushing, and save everything just set
        resumePrefFlushing();
    }

    public static void allPreferencesCreated() {
        allPreferencesCreated = true;
    }

    private static int numStrings;
    private static int lenStrings;
    private static int numValueStrings = 0;
    private static int lenValueStrings = 0;
    
    public static void printAllPrefs(PrintStream out) {
        numValueStrings = lenValueStrings = 0;
        TreeMap<String,Pref> sortedPrefs = new TreeMap<String,Pref>();
        synchronized (allPrefs) {
            for (Pref pref: allPrefs)
                sortedPrefs.put(pref.prefs.absolutePath() + "/" + pref.name, pref);
        }
        Preferences rootNode = Preferences.userRoot().node("com/sun/electric");
        numStrings = lenStrings = 0;
        try {
            gatherPrefs(out, 0, rootNode, null);
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
        out.println(lenStrings + " chars in " + numStrings + " strings");
        out.println(lenValueStrings + " chars in " + numValueStrings + " value strings");
        int  i = 0;
        for (Pref pref: sortedPrefs.values())
            out.println((i++) + pref.prefs.absolutePath() + " " + pref.name + " " + pref.cachedObj + " " + (pref.meaning != null));
    }

    private static void gatherPrefs(PrintStream out, int level, Preferences topNode, List<String> ks) throws BackingStoreException {
        for (int i = 0; i < level; i++) System.out.print("  ");
        String[] keys = topNode.keys();
        for (int i = 0; i < keys.length; i++) {
            numStrings++;
            lenStrings += keys.length;
            String value = topNode.get(keys[i], null);
            numValueStrings++;
            lenValueStrings += value.length();
        }
        out.println(topNode.name() + " " + keys.length);
        if (topNode.absolutePath().equals("/com/sun/electric/database/hierarchy")) return;
        String[] children = topNode.childrenNames();
        for (int i = 0; i < children.length; i++) {
            String childName = children[i];
            numStrings++;
            lenStrings += children[i].length();
            Preferences childNode = topNode.node(childName);
            gatherPrefs(out, level + 1, childNode, ks);
        }
    }

    /****************************** private methods ******************************/

	/**
	 * Method to force all Preferences to be saved.
	 */
	private static void flushOptions(Preferences p)
	{
		try
		{
			p.flush();
		} catch (Exception e)
		{
            if (!Job.BATCHMODE) {
			    System.out.println("Failed to save preferences");
            }
		}
	}
}
