/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: KeyBindingManager.java
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
/*
 * KeyBinding.java
 *
 * Created on April 1, 2004, 3:44 PM
 */

package com.sun.electric.tool.user;


import javax.swing.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.ActionListener;

/**
 * The KeyBindingManager manages key bindings and their associated actions. It
 * implements a <code>KeyListener</code> so it can be added as a key listener
 * to any component.
 * <p><p>
 * The <i>inputMap</i> uses <code>KeyStrokes</code> as it's keys, and stores Objects
 * of type List.  The List contains Strings.
 * <p>
 * Each String is then used as a key into the HashMap <i>actionMap</i> to retrieve
 * a List of KeyBinding objects.  Each object has an action which can then be
 * executed.
 * <p>
 * This model is similar to jawa.swing.InputMap and java.swing.ActionMap, but has
 * been modified to use lists so that multiple keys can be bound to the same action,
 * and a single key can spawn multiple actions.
 * <p><p>
 * The KeyBindingManager also has a HashMap <i>prefixedInputMapMaps</i>. A prefixStroke
 * is used as a key to this table to obtain an inputMap (HashMap) based on the prefixStroke.
 * From here it is the same as before with the inputMap and actionMap:
 * A KeyStroke is then used as a key to find a List of Strings.  The Strings are
 * then used as a key into <i>actionMap</i> to get a list of KeyBindings
 * (and their actions).
 * <p>
 * While this is horribly complicated it allows for two-stroke key combinations to
 * be caught (ala emacs).
 *
 * @author  gainsley
 */
public class KeyBindingManager implements KeyListener {

    // ----------------------------- object stuff ---------------------------------
    /** Hash table of lists all key bindings */     private HashMap inputMap;
    /** Hash table of lists of all actions */       private HashMap actionMap;
    /** last prefix key pressed */                  private KeyStroke lastPrefix;
    /** Hash table of hash of lists of prefixed key bindings */ private HashMap prefixedInputMapMaps;
    /** action to take on prefix key hit */         private PrefixAction prefixAction;
    /** where to store Preferences */               private Preferences prefs;
    /** Hash table of lists of default key bindings*/ private HashMap defaultActionMap;

    /**
     * Construct a new KeyBindingManager that can act as a KeyListener
     * on a Component.
     */
    public KeyBindingManager(Preferences prefs) {
        inputMap = new HashMap();
        actionMap = new HashMap();
        prefixedInputMapMaps = new HashMap();
        defaultActionMap = new HashMap();
        lastPrefix = null;
        prefixAction = new PrefixAction(this);
        this.prefs = prefs;
        try {
            prefs.clear();
        } catch (BackingStoreException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        // add prefix action to action map
        actionMap.put(prefixAction.actionDesc, prefixAction);
    }

    /**
     * Class to store a key binding
     */
    public static class KeyBinding
    {
        /** description of action */            public String actionDesc;
        /** respond to this KeyStroke */        public KeyStroke stroke;
        /** prefix stroke (null if none) */     public KeyStroke prefixStroke;
        /** action to perform */                public ActionListener action;

        /**
         * Constructs a new KeyBinding.
         * @param actionDesc description of the key binding to show to the user
         * @param stroke key stroke of key binding
         * @param prefixStroke optional prefix key stroke of key binding. May be null.
         * @param action the action to perform when the key binding is activated
         */
        public KeyBinding(String actionDesc, KeyStroke stroke, KeyStroke prefixStroke, ActionListener action) {
            this.actionDesc = actionDesc;
            this.stroke = stroke;
            this.prefixStroke = prefixStroke;
            this.action = action;
        }

        /**
         * Compare to KeyBindings.  Returns true if they are equal.
         * @param binding the binding to compare to this.
         * @return true if equal, false otherwise.
         */
        public boolean equals(KeyBinding binding) {
            if (!keyStrokeToString(binding.stroke).equals(keyStrokeToString(stroke)))
                return false;
            if (!keyStrokeToString(binding.prefixStroke).equals(keyStrokeToString(prefixStroke)))
                return false;
            return true;
        }

        public String toString() {
            if (stroke == null) return "";
            if (prefixStroke == null) return keyStrokeToString(stroke); else
                return keyStrokeToString(prefixStroke)+", " + keyStrokeToString(stroke);
        }
    }

    /**
     * PrefixAction is a an action performed when a prefix key is hit.
     * This then registers that prefix key with the KeyBindingManager.
     * This allows key bindings to consist of two-key sequences.
     */
    protected class PrefixAction extends AbstractAction
    {
        /** The action description analagous to KeyBinding */ public String actionDesc = "KeyBindingManager prefix action";
        /** the key binding manager using this aciton */    private KeyBindingManager manager;

        public PrefixAction(KeyBindingManager manager) {
            super();
            this.manager = manager;
        }

        public void actionPerformed(ActionEvent e) {
            KeyEvent keyEvent = (KeyEvent)e.getSource();
            KeyStroke stroke = KeyStroke.getKeyStrokeForEvent(keyEvent);
            manager.setPrefixKey(stroke);
            System.out.println("prefix key "+keyStrokeToString(stroke)+" hit...");
        }
    }


    // ------------------------------ Key Listener ---------------------------------

    public void keyPressed(KeyEvent e) {

        System.out.println("got event (consumed="+e.isConsumed()+")"+e);
        // ignore if consumed
        if (e.isConsumed()) return;

        // get KeyStroke
        KeyStroke stroke = KeyStroke.getKeyStrokeForEvent(e);
        HashMap inputMapToUse = inputMap;

        // check if we should use prefixed key map instead of regular inputMap
        if (lastPrefix != null) {
            // get input map based on prefix key
            inputMapToUse = (HashMap)prefixedInputMapMaps.get(lastPrefix);
            if (inputMapToUse == null) return;
        }

        PrefixAction prefixActionToDo = null;
        ActionEvent evt = new ActionEvent(e, ActionEvent.ACTION_PERFORMED, stroke.toString(), stroke.getModifiers());

        // get list of action strings, iterate over them
        List keyBindingList = (List)inputMapToUse.get(stroke);
        if (keyBindingList == null) return;             // nothing bound to key
        for (Iterator it = keyBindingList.iterator(); it.hasNext(); ) {
            String actionDesc = (String)it.next();

            // get KeyBinding objects from action map, activate their actions
            List bindings = (List)actionMap.get(actionDesc);
            if (bindings == null) continue;             // nothing bound to actionDesc string
            for (Iterator it2 = bindings.iterator(); it2.hasNext(); ) {
                Object obj = it2.next();

                if (obj instanceof PrefixAction) {
                    prefixActionToDo = (PrefixAction)obj; // setup to perform action later
                } else {
                    KeyBinding k = (KeyBinding)obj;
                    k.action.actionPerformed(evt);      // perform action
                }
            }
        }

        if (prefixActionToDo != null) {
            // do this last, so other actions cannot trigger off of it
            prefixActionToDo.actionPerformed(evt);
        }

        // finally, remove last prefix key
        lastPrefix = null;
    }

    public void keyReleased(KeyEvent e) {
        System.out.println("got event (consumed="+e.isConsumed()+")"+e);
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void keyTyped(KeyEvent e) {
        System.out.println("got event (consumed="+e.isConsumed()+")"+e);
        //To change body of implemented methods use File | Settings | File Templates.
    }

    // --------------------------------------------------------------------------------

    /**
     * Adds a default KeyBinding. If any keyBindings are found for
     * <code>k.actionDesc</code>, those are used instead.  Note that <code>k</code>
     * cannot be null, but it's stroke and prefixStroke can be null.  However,
     * it's actionDesc and action must be valid.
     * @param k the default KeyBinding
     */
    public void addDefaultKeyBinding(KeyBinding k) {
        boolean prefsLoaded = true; // if k.actionDesc has already had user prefs loaded

        // sanity checks
        if (k.actionDesc == null || k.action == null) {
            System.out.println("  Attempting to add invalid default key binding");
            return;
        }

        // add to default list so we can restore if needed
        List list = (List)defaultActionMap.get(k.actionDesc);
        if (list == null) {
            list = new ArrayList();
            //defaultActionMap.put(k.actionDesc, list);
            prefsLoaded = false;
        }
        if (k.stroke != null)
            list.add(k);

        List userKeyBindings = getBindingsFromPrefs(k.actionDesc);
        if (userKeyBindings == null) {
            // using default bindings, add default binding
            addKeyBinding(k);
        } else {
            if (!prefsLoaded) {
                // this is the first time a default key binding has been
                // attempted to add to the manager for this actionDesc.
                // Use user key bindings (if any)
                for (Iterator it = userKeyBindings.iterator(); it.hasNext(); ) {
                    KeyBinding key = (KeyBinding)it.next();
                    key.action = k.action;          // update actionListener
                    addKeyBinding(key);
                }
            }
            // else actionDesc already checked, user key bindings already loaded
        }
    }

    /**
     * Adds a user specified KeyBinding. Also adds it to stored user preference.
     * @param k the KeyBinding to add.
     */
    public void addUserKeyBinding(KeyBinding k) {
        // sanity checks
        if (k.actionDesc == null || k.action == null) {
            System.out.println("  Attempting to add invalid user key binding");
            return;
        }

        // add to bindings
        addKeyBinding(k);
        // register in preferences
        if (prefs == null) return;
        String keys = prefs.get(k.actionDesc, "");
        String newkey = keyStrokeToString(k.prefixStroke) + ", " + keyStrokeToString(k.stroke);
        if (keys.equals("")) keys = newkey; else keys = keys + "; "+ newkey;
        prefs.put(k.actionDesc, keys);
    }

    /**
     * Adds a new KeyBinding.  Note that an action bound to a key that is also
     * a prefix key for another action will result in both the action occuring
     * and the prefix key enabling the prefixed action.
     * <p>This is a private method.  addDefault- or addUserKeyBindings should be
     * called from external code.
     * @param k the KeyBinding to add.
     */
    private void addKeyBinding(KeyBinding k) {

        //System.out.println("adding binding for "+k.actionDesc);

        HashMap inputMapToUse = inputMap;

        // if prefix stroke is non-null, use prefixInputMap
        if (k.prefixStroke != null) {
            // find HashMap based on prefixAction
            inputMapToUse = (HashMap)prefixedInputMapMaps.get(k.prefixStroke);
            if (inputMapToUse == null) {
                inputMapToUse = new HashMap();
                prefixedInputMapMaps.put(k.prefixStroke, inputMapToUse);
            }
            // add prefix action to primary input map
            List list = (List)inputMap.get(k.prefixStroke);
            if (list == null) {
                list = new ArrayList();
                inputMap.put(k.prefixStroke, list);
            }
            list.add(prefixAction.actionDesc);
        }

        // add stroke to input map to use
        List list = (List)inputMapToUse.get(k.stroke);
        if (list == null) {
            list = new ArrayList();
            inputMapToUse.put(k.stroke, list);
        }
        list.add(k.actionDesc);

        // add actionDesc:KeyBinding to action map
        // this is the same whether or not a prefix stroke exists
        list = (List)actionMap.get(k.actionDesc);
        if (list == null) {
            list = new ArrayList();
            actionMap.put(k.actionDesc, list);
        }
        list.add(k);
        System.out.println("adding to actionMap "+k.actionDesc);
    }


    /**
     * Get bindings for action string
     * @param actionDesc string describing action (KeyBinding.actionDesc)
     * @return a list of KeyBinding objects, or null.
     */
    public List getBindingsFor(String actionDesc) {
        return (List)actionMap.get(actionDesc);
    }


    /**
     * Removes a key binding.
     * @param k
     */
    public void removeKeyBinding(KeyBinding k) {

        HashMap inputMapToUse = inputMap;

        // remove any prefix stroke
        if (k.prefixStroke != null) {
            List list = (List)inputMap.get(k.prefixStroke);
            for (Iterator it = list.iterator(); it.hasNext(); ) {
                Object obj = it.next();
                if (obj instanceof PrefixAction) {
                    list.remove(obj);
                    break;
                }
            }
            // get input map to use
            inputMapToUse = (HashMap)prefixedInputMapMaps.get(k.prefixStroke);
        }

        // remove stroke
        List list = (List)inputMapToUse.get(k.stroke);
        list.remove(k.actionDesc);

        // remove action
        list = (List)actionMap.get(k.actionDesc);
        list.remove(k);
    }

    /**
     * Get a list of KeyBindings that conflict with the key combo
     * <code>prefixStroke, stroke</code>.  A conflict is registered if:
     * an existing stroke is the same as <code>prefixStroke</code>; or
     * an existing stroke is the same <code>stroke</code>
     * (if <code>prefixStroke</code> is null);
     * or an existing prefixStroke,stroke combo is the same as
     * <code>prefixStroke,stroke</code>.  Returns an empty list if there are
     * no conflicts.
     * @param stroke the key stroke
     * @param prefixStroke the prefix key stroke, may be null
     * @return a list of conflicting <code>KeyBinding</code>s.  Empty list if no conflicts.
     */
    public List getConflictingKeyBindings(KeyStroke stroke, KeyStroke prefixStroke) {

        List conflicts = new ArrayList();
        List conflictsStrings = new ArrayList();

        if (prefixStroke == null) {
            // check if stroke conflicts with anything
            List list = (List)inputMap.get(stroke);
            for (Iterator it = list.iterator(); it.hasNext(); ) {
                String str = (String)it.next();

                if (str.equals(prefixAction.actionDesc)) {
                    // find all string associated with prefix in prefix map
                    HashMap prefixMap = (HashMap)prefixedInputMapMaps.get(stroke);
                    for (Iterator it2 = prefixMap.values().iterator(); it2.hasNext(); ) {
                        // all existing prefixStroke,stroke combos conflict, so add them all
                        List prefixList = (List)it2.next(); // this is a list of strings
                        conflictsStrings.addAll(prefixList);
                    }
                } else {
                    conflictsStrings.add(str);              // otherwise this is a key actionDesc
                }
            }
        } else {
            // get all bindings associated with prefixStroke
            HashMap prefixMap = (HashMap)prefixedInputMapMaps.get(prefixStroke);
            // get list associated with stroke, add all in list to conflicts
            if (prefixMap != null) {
                List list = (List)prefixMap.get(stroke);
                conflictsStrings.addAll(list);
            }
        }

        // get all KeyBindings from ActionMap
        for (Iterator it = conflictsStrings.iterator(); it.hasNext(); ) {
            List list = (List)actionMap.get((String)it.next());
            conflicts.addAll(list);
        }

        return conflicts;
    }


    // -------------------------- Static Utility Methods ----------------------------------

    /**
     * Converts KeyStroke to String that can be parsed by
     * KeyStroke.getKeyStroke(String s).  For some reason the
     * KeyStroke class has a method to parse a KeyStroke String
     * identifier, but not one to make one.
     */
    public static String keyStrokeToString(KeyStroke key) {
        if (key == null) return "";
        String mods = KeyEvent.getKeyModifiersText(key.getModifiers());
        mods = mods.replace('+', ' ');
        mods = mods.toLowerCase();
        String id = KeyEvent.getKeyText(key.getKeyCode());
        return mods + " " + id;
    }

    public static KeyStroke stringToKeyStroke(String str) {
        return KeyStroke.getKeyStroke(str);
    }


    // ------------------------------ Private Methods ----------------------------------

    /**
     * Add KeyBinding to stored user preferences.
     * @param actionDesc the action description under which to store all the key bindings
     * @param keyBindings the KeyBindings to write to prefs
     */
    private void setBindingsToPrefs(String actionDesc, List keyBindings) {
        if (prefs == null) return;
        if (keyBindings == null) return;

        StringBuffer keys = new StringBuffer();
        for (Iterator it = keyBindings.iterator(); it.hasNext(); ) {
            KeyBinding binding = (KeyBinding)it.next();
            keys.append(keyStrokeToString(binding.prefixStroke) + ", " + keyStrokeToString(binding.stroke));
            if (it.hasNext()) keys.append("; ");
        }
        prefs.put(actionDesc, keys.toString());
    }


    /**
     * Get list of KeyBindings for <code>actionDesc</code> from Preferences.
     * Returns null if actionDesc not present in prefs; returns empty list if
     * actionDesc found but no valid bindings.  This distinguishes between
     * two cases: when the default bindings should be used,
     * or when the user has removed all default bindings and no
     * bindings should be present.
     * @param actionDesc the action description associated with these bindings
     * @return a list of KeyBindings. null if no preference found, empty
     * list if user has removed all default bindings.
     */
    private List getBindingsFromPrefs(String actionDesc) {
        if (prefs == null) return null;

        List list = new ArrayList();
        String keys = prefs.get(actionDesc, null);
        if (keys == null) return null;                  // actionDesc not found in prefs
        if (keys.equals("")) return list;               // actionDesc found, but no bindings

        String [] bindings = keys.split("; ");
        for (int i = 0; i < bindings.length; i++) {
            String binding = bindings[i];
            KeyStroke stroke, prefixStroke;
            String [] splitKeys = binding.split(", ");
            if (splitKeys[0].equals("")) {
                stroke = stringToKeyStroke(splitKeys[1]);
                prefixStroke = null;
            } else {
                prefixStroke = stringToKeyStroke(splitKeys[0]);
                stroke = stringToKeyStroke(splitKeys[1]);
            }
            list.add(new KeyBinding(actionDesc, stroke, prefixStroke, null));
        }
        return list;
    }

    /**
     * Called by the KeyBindingManager's prefixAction to register
     * that a prefix key has been hit.
     * @param prefix the prefix key
     */
    protected void setPrefixKey(KeyStroke prefix) {
        this.lastPrefix = prefix;
    }

}
