/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ProjSettingsNode.java
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
package com.sun.electric.tool.user.projectSettings;

import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Stack;
import java.util.TreeSet;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Jul 25, 2006
 * Time: 5:32:36 PM
 */

/**
 * A basic class to hold information on project settings.
 * This node holds key-value pairs, who values are typically
 * restricted to a few primitive types, and other ProjSettingNodes.
 * <P>
 * This class may be used as is, or may be extended to provide additional,
 * more specific methods for setting/getting project settings.
 * Only settings accessible by this class' methods will be
 * written to disk.
 * Additionally, the extended class must be public, so that it can be
 * created when the settings are read from a file.
 * <P>
 * Settings are written to the file in the order they are
 * added to this class. This order is consistent and deterministic,
 * and is not sorted after being added.
 * <P>
 * This class also retains the first setting for any key.  This allows
 * the user to get the "initial" or "factory" setting, which is always
 * the first setting applied.
 */
public class ProjSettingsNode implements Serializable {

    private LinkedHashMap<String,Object> data;
    private LinkedHashMap<String,Object> dataInitial;

    /**
     * Create a new default proj settings node
     */
    public ProjSettingsNode() {
        data = new LinkedHashMap<String,Object>();
        dataInitial = new LinkedHashMap<String,Object>();
    }

    /**
     * Returns a set of keys, whose order is the
     * order in which keys were added.
     * @return a set of keys in deterministic order
     */
    public Set<String> getKeys() {
        return data.keySet();
    }

    public Object get(String key) {
        return data.get(key);
    }

    private Object getInitial(String key) {
        return dataInitial.get(key);
    }

    /**
     * Set the value for a key. Throws an exception if the value is
     * not a supported class.
     * @throws IllegalArgumentException if the class of value is not supported.
     * @param key a string key
     * @param value a value
     * @see ProjSettings#isSupportedClass(Object)
     */
    protected void put(String key, Object value) {
        if (ProjSettings.isSupportedClass(value)) {
            if ((value instanceof ProjSettingsNode) && data.containsKey(key)) {
                System.out.println("ERROR!!!! Trying to insert a ProjSettingsNode using key "+key+
                        ", which is already in use by object "+data.get(key).toString());
                return;
            }
            data.put(key, value);
            if (!dataInitial.containsKey(key)) {
                dataInitial.put(key, value);
            }
        } else {
            throw new IllegalArgumentException("Unsupported class "+value.getClass().getName()+" in ProjSettingsNode");
        }
    }

    // ------------------------ Supported Types -------------------------------

    public void putBoolean(String key, boolean value) { put(key, new Boolean(value)); }
    public boolean getBoolean(String key) {
        if (!(get(key) instanceof Boolean)) return false;
        return ((Boolean)get(key)).booleanValue();
    }
    public boolean getInitialBoolean(String key) { return ((Boolean)getInitial(key)).booleanValue(); }

    public void putDouble(String key, double value) { put(key, new Double(value)); }
    public double getDouble(String key) {
        if (!(get(key) instanceof Double)) return 0;
        return ((Double)get(key)).doubleValue();
    }
    public double getInitialDouble(String key) { return ((Double)getInitial(key)).doubleValue(); }

    public void putInteger(String key, int value) { put(key, new Integer(value)); }
    public int getInteger(String key) {
        if (!(get(key) instanceof Integer)) return 0;
        return ((Integer)get(key)).intValue();
    }
    public int getInitialInteger(String key) { return ((Integer)getInitial(key)).intValue(); }

    public void putLong(String key, long value) { put(key, new Long(value)); }
    public long getLong(String key) {
        if (!(get(key) instanceof Long)) return 0;
        return ((Long)get(key)).longValue();
    }
    public long getInitialLong(String key) { return ((Long)getInitial(key)).longValue(); }

    public void putString(String key, String value) { put(key, value); }
    public String getString(String key) {
        if (!(get(key) instanceof String)) return "";
        return (String)get(key);
    }
    public String getInitialString(String key) { return (String)getInitial(key); }

    public void putNode(String key, ProjSettingsNode node) { put(key, node); }
    public ProjSettingsNode getNode(String key) {
        if (!(get(key) instanceof ProjSettingsNode)) {
            ProjSettingsNode node = new ProjSettingsNode();
            putNode(key, node);
        }
        return (ProjSettingsNode)get(key);
    }


    // ----------------------------- Utility ----------------------------------

    public boolean equals(Object node) {
        if (!(node instanceof ProjSettingsNode)) return false;

        ProjSettingsNode otherNode = (ProjSettingsNode)node;
        Set<String> myKeys = getKeys();
        Set<String> otherKeys = otherNode.getKeys();
        if (myKeys.size() != otherKeys.size()) return false;

        for (String myKey : myKeys) {
            if (!(otherKeys.contains(myKey))) return false;
            Object myObj = get(myKey);
            Object otherObj = otherNode.get(myKey);
            if (myObj.getClass() != otherObj.getClass()) return false;
            if (!myObj.equals(otherObj)) return false;
        }
        return true;
    }

    /**
     * Print any differences between the two nodes
     * @param node the nodes to compare
     * @return true if differences found, false otherwise
     */
    public boolean printDifferences(Object node) {
        return printDifferences(node, new Stack<String>());
    }
    private boolean printDifferences(Object node, Stack<String> context) {
        if (!(node instanceof ProjSettingsNode)) return true;

        boolean differencesFound = false;
        ProjSettingsNode otherNode = (ProjSettingsNode)node;
        Set<String> myKeys = getKeys();
        Set<String> otherKeys = otherNode.getKeys();
        Set<String> allKeys = new TreeSet<String>();
        allKeys.addAll(myKeys);
        allKeys.addAll(otherKeys);

        for (String key : allKeys) {
            if (!myKeys.contains(key)) {
                System.out.println("Warning: Key "+getKey(context, key)+" is missing from other settings");
                differencesFound = true;
                continue;
            }
            if (!otherKeys.contains(key)) {
                System.out.println("Warning: Key "+getKey(context, key)+" is missing from current settings");
                differencesFound = true;
                continue;
            }
            Object myObj = get(key);
            Object otherObj = otherNode.get(key);
            if (myObj.getClass() != otherObj.getClass()) {
                System.out.println("Warning: Value type mismatch for key "+getKey(context, key)+": "+
                        myObj.getClass().getName()+" vs "+otherObj.getClass().getName());
                differencesFound = true;
                continue;
            }
            if (myObj instanceof ProjSettingsNode) {
                context.push(key);
                if (((ProjSettingsNode)myObj).printDifferences(otherObj, context))
                    differencesFound = true;
                context.pop();
            } else if (!myObj.equals(otherObj)) {
                System.out.println("Warning: Values not equal for key "+getKey(context, key)+": "+myObj+" vs "+otherObj);
                differencesFound = true;
            }
        }
        return differencesFound;
    }

    private String getKey(Stack<String> context, String key) {
        return describeContext(context)+"."+key;
    }

    public static String describeContext(Stack<String> context) {
        StringBuffer buf = new StringBuffer();
        boolean first = true;
        for (String name : context) {
            if (first)
                first = false;
            else
                buf.append(".");
            buf.append(name);
        }
        if (buf.length() == 0) return "RootContext";
        return buf.toString();
    }
}
