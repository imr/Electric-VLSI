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

import com.sun.electric.database.text.Pref;

import java.util.*;
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

    private LinkedHashMap<String, Object> data;

    public static class UninitializedPref {
        public final Object value;
        public UninitializedPref(Object value) {
            this.value = value;
        }
    }

    /**
     * Create a new default proj settings node
     */
    public ProjSettingsNode() {
        data = new LinkedHashMap<String,Object>();
    }

    /**
     * Returns a set of keys, whose order is the
     * order in which keys were added.
     * @return a set of keys in deterministic order
     */
    public Set<String> getKeys() {
        return data.keySet();
    }

    /**
     * Set the value for a key.
     * @param key a string key
     * @param pref a value
     */
    public void putValue(String key, Pref pref) {
        Object v = data.get(key);
        Object previousVal = null;
        if (v instanceof UninitializedPref) {
            // this overrides pref value, when pref was uninitialized so we couldn't set it before
            previousVal = ((UninitializedPref)v).value;
        }
        data.put(key, pref);

        if (previousVal != null && !equal(previousVal, pref)) {
            System.out.println("Warning: For key "+key+": project setting value of "+previousVal+" overrides default of "+pref.getValue());
            if (previousVal instanceof Boolean)
                pref.setBoolean(((Boolean)previousVal).booleanValue());
            else if (previousVal instanceof Integer)
                pref.setInt(((Integer)previousVal).intValue());
            else if (previousVal instanceof Double)
                pref.setDouble(((Double)previousVal).doubleValue());
            else if (previousVal instanceof String)
                pref.setString(previousVal.toString());
            else if (previousVal instanceof Long)
                pref.setLong(((Long)previousVal).longValue());
        }
    }

    public Pref getValue(String key) {
        Object obj = data.get(key);
        if (obj instanceof Pref)
            return (Pref)obj;
        if (obj == null) return null;
        prIllegalRequestError(key);
        return null;
    }

    public void putNode(String key, ProjSettingsNode node) {
        data.put(key, node);
    }

    public ProjSettingsNode getNode(String key) {
        Object obj = data.get(key);
        if (obj == null) {
            obj = new ProjSettingsNode();
            data.put(key, obj);
        }
        if (obj instanceof ProjSettingsNode)
            return (ProjSettingsNode)obj;
        prIllegalRequestError(key);
        return null;
    }

    private void prIllegalRequestError(String key) {
        System.out.println("ERROR! Project Settings key conflict: "+key);
    }

    // ----------------------------- Protected --------------------------------

    protected Object get(String key) {
        return data.get(key);
    }

    protected void put(String key, Object node) {
        data.put(key, node);
    }

    // ----------------------------- Utility ----------------------------------

    /**
     * Compare a project settings value against a prefValue object value.
     * You can't just use .equals() because the prefValue object does
     * not store booleans as Booleans.
     * @param value
     * @param pref
     * @return true if values equal, false otherwise
     */
    public static boolean equal(Object value, Pref pref) {
        if (value == null || pref.getValue() == null)
            return false;
        if (pref.getType() == Pref.PrefType.BOOLEAN && (value instanceof Boolean))
            return pref.getBoolean() == ((Boolean)value).booleanValue();
        if (value.getClass() != pref.getValue().getClass())
            return false;
        return value.equals(pref.getValue());
    }

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
