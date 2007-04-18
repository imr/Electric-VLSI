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
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
        Preferences prefs;
        Group(Preferences prefs) { this.prefs = prefs; }
        public String absolutePath() { return prefs.absolutePath(); }
    }

    public static Group groupForPackage(Class classFromPackage) {
        Preferences prefs = Preferences.userNodeForPackage(classFromPackage);
        return new Group(prefs);
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
	private   Object      cachedObj;
	private   Object      factoryObj;
//    private   boolean changed = false;

	private static final List<Pref> allPrefs = new ArrayList<Pref>();
	private static boolean doFlushing = true;
//    private static PrefChangeBatch changes = null;
	private static Set<Preferences> queueForFlushing;

    /**
	 * The constructor for the Pref.
	 * @param name the name of this Pref.
	 */
	protected Pref(Preferences prefs, String name) {
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
			delayPrefFlushing();
            synchronized (allPrefs) {
                for(Pref pref : allPrefs) {
                    switch (pref.type) {
                        case BOOLEAN:
                            pref.setBoolean(pref.prefs.getBoolean(pref.name, pref.getBooleanFactoryValue()));
                            break;
                        case INTEGER:
                            pref.setInt(pref.prefs.getInt(pref.name, pref.getIntFactoryValue()));
                            break;
                        case LONG:
                            pref.setLong(pref.prefs.getLong(pref.name, pref.getLongFactoryValue()));
                            break;
                        case DOUBLE:
                            pref.setDouble(pref.prefs.getDouble(pref.name, pref.getDoubleFactoryValue()));
                            break;
                        case STRING:
                            pref.setString(pref.prefs.get(pref.name, pref.getStringFactoryValue()));
                            break;
                    }
                }
                Setting.saveAllSettingsToPreferences();
            }
			resumePrefFlushing();

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
	 * Factory methods to create an integer Pref objects.
	 * The proper way to create an integer Pref is with makeIntPref;
	 * use of this method is only for subclasses.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	protected void initInt(int factory)
	{
		this.type = PrefType.INTEGER;
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
	 * Factory methods to create a long Pref objects.
	 * The proper way to create a long Pref is with makeLongPref;
	 * use of this method is only for subclasses.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	protected void initLong(long factory)
	{
		this.type = PrefType.LONG;
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
	 * Factory methods to create a double Pref objects.
	 * The proper way to create a double Pref is with makeDoublePref;
	 * use of this method is only for subclasses.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	protected void initDouble(double factory)
	{
		this.type = PrefType.DOUBLE;
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
	 * Factory methods to create a string Pref objects.
	 * The proper way to create a string Pref is with makeStringPref;
	 * use of this method is only for subclasses.
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	protected void initString(String factory)
	{
		this.type = PrefType.STRING;
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
//            changed = true;
            if (prefs != null)
			{
				prefs.putBoolean(name, v);
				if (doFlushing) flushOptions(prefs); else
					queueForFlushing.add(prefs);
			}
		}
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
//            changed = true;
			if (prefs != null)
			{
				prefs.putInt(name, v);
				if (doFlushing) flushOptions(prefs); else
					queueForFlushing.add(prefs);
			}
		}
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
//            changed = true;
			if (prefs != null)
			{
				prefs.putLong(name, v);
				if (doFlushing) flushOptions(prefs); else
					queueForFlushing.add(prefs);
			}
		}
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
//            this.changed = true;
			if (prefs != null)
			{
				prefs.putDouble(name, v);
				if (doFlushing) flushOptions(prefs); else
					queueForFlushing.add(prefs);
			}
			changed = true;
		}
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
//            changed = true;
            if (prefs != null)
			{
				prefs.put(name, str);
				if (doFlushing) flushOptions(prefs); else
					queueForFlushing.add(prefs);
			}
		}
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
        Setting.printAllSettings(out);
        out.println("ELECTRIC USER PREFERENCES");
        int  i = 0;
        for (Pref pref: sortedPrefs.values())
            out.println((i++) + pref.prefs.absolutePath() + " " + pref.name + " " + pref.cachedObj);
    }

    private static void gatherPrefs(PrintStream out, int level, Preferences topNode, List<String> ks) throws BackingStoreException {
        for (int i = 0; i < level; i++) out.print("  ");
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
