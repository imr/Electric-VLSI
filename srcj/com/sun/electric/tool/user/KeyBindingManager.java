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


import com.sun.electric.tool.user.ui.KeyStrokePair;
import com.sun.electric.tool.user.ui.KeyBindings;

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
import java.awt.*;

/**
 * The KeyBindingManager manages key bindings and their associated actions. It
 * implements a <code>KeyListener</code> so it can be added as a key listener
 * to any component.
 * <p><p>
 * The <i>inputMap</i> uses <code>KeyStrokes</code> as it's keys, and stores Objects
 * of type List.  The List contains Strings.
 * <p>
 * Each String is then used as a key into the HashMap <i>actionMap</i> to retrieve
 * a KeyBindings object.  Each key bindings object has a list of actions which can then be
 * performed.
 * <p>
 * This model is similar to jawa.swing.InputMap and java.swing.ActionMap.
 * However, secondary InputMaps allow two-stroke key bindings.  Additionally,
 * everything has been enveloped in an object which can
 * then be inserted into the event hierarchy in different ways, instead of having
 * to set a Component's InputMap and ActionMap.
 * <p><p>
 * Two-stroke bindings:<p>
 * The KeyBindingManager has a HashMap <i>prefixedInputMapMaps</i>. A prefixStroke
 * is used as a key to this table to obtain an inputMap (HashMap) based on the prefixStroke.
 * From here it is the same as before with the inputMap and actionMap:
 * A KeyStroke is then used as a key to find a List of Strings.  The Strings are
 * then used as a key into <i>actionMap</i> to get a KeyBindings object and
 * perform the associated action.  There is only one actionMap.
 * <p>
 *
 * @author  gainsley
 */
public class KeyBindingManager implements KeyEventPostProcessor {

    // ----------------------------- object stuff ---------------------------------
    /** Hash table of lists all key bindings */     private HashMap inputMap;
    /** Hash table of all actions */                private HashMap actionMap;
    /** last prefix key pressed */                  private KeyStroke lastPrefix;
    /** Hash table of hash of lists of prefixed key bindings */ private HashMap prefixedInputMapMaps;
    /** action to take on prefix key hit */         private PrefixAction prefixAction;
    /** where to store Preferences */               private Preferences prefs;
    /** prefix on pref key, if desired */           private String prefPrefix;

    // ----------------------------- global stuff ----------------------------------
    /** Listener to register for catching keys */   public static KeyBindingListener listener = new KeyBindingListener();
    /** All key binding manangers */                private static List allManagers = new ArrayList();

    /**
     * Construct a new KeyBindingManager that can act as a KeyListener
     * on a Component.
     */
    public KeyBindingManager(String prefPrefix, Preferences prefs) {
        inputMap = new HashMap();
        actionMap = new HashMap();
        prefixedInputMapMaps = new HashMap();
        lastPrefix = null;
        prefixAction = new PrefixAction(this);
        this.prefs = prefs;
        this.prefPrefix = prefPrefix;

        // add prefix action to action map
        actionMap.put(prefixAction.actionDesc, prefixAction);

        // register this with KeyboardFocusManager
        // so we receive all KeyEvents
        //KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
        //KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(this);

        // add to list of all managers
        allManagers.add(this);
        initialize();
    }

    /**
     * Called when disposing of this manager, allows memory used to
     * be reclaimed by removing static references to this.
     */
    public void finished() {
        allManagers.remove(this);
        //KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventPostProcessor(this);
    }

    /**
     * Initialize: Reads all stored key bindings from preferences
     */
    private void initialize() {
        String [] allKeys;
        try {
            allKeys = prefs.keys();
        } catch (BackingStoreException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return;
        }
        for (int i = 0; i < allKeys.length; i++) {
            // read bindings
            String key = allKeys[i].replaceFirst(prefPrefix, "");
            // old binding format and new format conflict, add check to avoid duplicates
            if (actionMap.containsKey(key)) continue;
            //System.out.println("looking for prefs key "+key);
            KeyBindings keys = new KeyBindings(key);
            List pairs = getBindingsFromPrefs(key);
            // if any bindings, set usingDefaults false, and add them
            if (pairs != null) {
                keys.setUsingDefaultKeys(false); // set usingDefaults false
                for (Iterator it = pairs.iterator(); it.hasNext(); ) {
                    KeyStrokePair pair = (KeyStrokePair)it.next();
                    if (pair == null) continue;
                    addKeyBinding(key, pair);
                }
            }
        }
    }

    // ---------------------------- Prefix Action Class -----------------------------

    /**
     * Class PrefixAction is an action performed when a prefix key is hit.
     * This then registers that prefix key with the KeyBindingManager.
     * This allows key bindings to consist of two-key sequences.
     */
    private static class PrefixAction extends AbstractAction
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
            System.out.println("prefix key '"+KeyStrokePair.keyStrokeToString(stroke)+"' hit...");
        }
    }

    /**
     * Called by the KeyBindingManager's prefixAction to register
     * that a prefix key has been hit.
     * @param prefix the prefix key
     */
    protected void setPrefixKey(KeyStroke prefix) {
        this.lastPrefix = prefix;
    }

    // ------------------------------ Key Processing ---------------------------------


    public static class KeyBindingListener implements KeyListener
    {
        public void keyPressed(KeyEvent e) {
            for (Iterator it = allManagers.iterator(); it.hasNext(); ) {
                KeyBindingManager m = (KeyBindingManager)it.next();
                if (m.processKeyEvent(e)) return;
            }
        }

        public void keyReleased(KeyEvent e) {}

        public void keyTyped(KeyEvent e) {}
    }

    public boolean postProcessKeyEvent(KeyEvent e) {
        return processKeyEvent(e);
    }

    /**
     * Process a KeyEvent by finding what actionListeners should be
     * activated as a result of the event.  The keyBindingManager keeps
     * one stroke of history so that two-stroke events can be distinguished.
     * @param e the KeyEvent
     * @return true if event consumed, false if not and nothing done.
     */
    public synchronized boolean processKeyEvent(KeyEvent e) {

        //System.out.println("got event (consumed="+e.isConsumed()+")"+e);

        // only look at key pressed events
        if (e.getID() != KeyEvent.KEY_PRESSED) return false;
        // ignore modifier only events (CTRL, SHIFT etc just by themselves)
        if (e.getKeyCode() == KeyEvent.VK_CONTROL) return false;
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) return false;
        if (e.getKeyCode() == KeyEvent.VK_ALT) return false;
        if (e.getKeyCode() == KeyEvent.VK_META) return false;

        //System.out.println("last Prefix key is "+lastPrefix);
        // ignore if consumed
        if (e.isConsumed()) {
            lastPrefix = null;              // someone did something with it, null prefix key
            return false;
        }

        // get KeyStroke
        KeyStroke stroke = KeyStroke.getKeyStrokeForEvent(e);
        HashMap inputMapToUse = inputMap;

        // check if we should use prefixed key map instead of regular inputMap
        if (lastPrefix != null) {
            // get input map based on prefix key
            inputMapToUse = (HashMap)prefixedInputMapMaps.get(lastPrefix);
            if (inputMapToUse == null) { lastPrefix = null; return false; }
        }

        ActionListener action = null;
        ActionEvent evt = new ActionEvent(e, ActionEvent.ACTION_PERFORMED, stroke.toString(), stroke.getModifiers());

        boolean actionPerformed = false;
        boolean prefixActionPerformed = false;
        // get list of action strings, iterate over them
        List keyBindingList = (List)inputMapToUse.get(stroke);
        if (keyBindingList != null) {
            for (Iterator it = keyBindingList.iterator(); it.hasNext(); ) {
                String actionDesc = (String)it.next();

                // get KeyBinding object from action map, activate its action
                // note that if this is a prefixed action, this could actually be a
                // PrefixAction object instead of a KeyBinding object.
                action = (ActionListener)actionMap.get(actionDesc);
                if (action instanceof PrefixAction) {
                    if (!prefixActionPerformed) {
                        action.actionPerformed(evt);        // only do this once
                        prefixActionPerformed = true;
                    }
                } else {
                    action.actionPerformed(evt);
                    lastPrefix = null;
                }
                actionPerformed = true;
            }
        }
        if (!actionPerformed) {
            // if no action to perform, perhaps the user hit a prefix key, then
            // decided to start another prefix-key-combo (that does not result in
            // a valid binding with the first prefix, obviously).  We'll be nice
            // and check for this case
            HashMap prefixMap = (HashMap)prefixedInputMapMaps.get(stroke);
            if (prefixMap != null) {
                // valid prefix key, fire prefix event
                prefixAction.actionPerformed(evt);
                actionPerformed = true;
            } else {
                lastPrefix = null;              // nothing to do
            }
        }

        //System.out.println(" actionPerformed="+actionPerformed);
        if (actionPerformed) {
            e.consume();                // consume event if we did something useful with it
            return true;                // let KeyboardFocusManager know we consumed event
        }
        // otherwise, do not consume, and return false to let KeyboardFocusManager
        // know that we did nothing with Event, and to pass it on
        return false;
    }

    // -------------- Static Methods Applied to All KeyBindingManagers ----------------

    /**
     * Get a list of conflicting key bindings from all KeyBindingManagers.
     * @param pair the keystrokepair
     * @return a list of conflicting KeyBindings from all KeyBindingManagers
     */
    public static List getConflictsAllManagers(KeyStrokePair pair) {
        List conflicts = new ArrayList();
        for (Iterator it = allManagers.iterator(); it.hasNext(); ) {
            KeyBindingManager m = (KeyBindingManager)it.next();
            conflicts.addAll(m.getConflictingKeyBindings(pair));
        }
        return conflicts;
    }

    // --------------------- Public Methods to Manage Bindings ----------------------

    /**
     * Adds a default KeyBinding. If any keyBindings are found for
     * <code>k.actionDesc</code>, those are used instead.  Note that <code>k</code>
     * cannot be null, but it's stroke and prefixStroke can be null.  However,
     * it's actionDesc and action must be valid.
     * @param actionDesc the action description
     * @param pair a key stroke pair
     */
    public synchronized void addDefaultKeyBinding(String actionDesc, KeyStrokePair pair) {
        if (pair == null) return;
        // add to default bindings
        KeyBindings keys = (KeyBindings)actionMap.get(actionDesc);
        if (keys == null) {
            keys = new KeyBindings(actionDesc);
            actionMap.put(actionDesc, keys);
        }
        keys.addDefaultKeyBinding(pair);
        if (keys.getUsingDefaultKeys()) {
            // using default keys, add default key to active maps
            addKeyBinding(actionDesc, pair);
        }
    }

    /**
     * Adds a user specified KeyBindings. Also adds it to stored user preference.
     * @param actionDesc the action description
     * @param pair a key stroke pair
     */
    public synchronized void addUserKeyBinding(String actionDesc, KeyStrokePair pair) {
        if (pair == null) return;
        // add to active bindings (also adds to KeyBindings object)
        KeyBindings keys = addKeyBinding(actionDesc, pair);
        // now using user specified key bindings, set usingDefaults false
        keys.setUsingDefaultKeys(false);
        // user has modified bindings, write all current bindings to prefs
        setBindingsToPrefs(keys.getActionDesc());
    }

    /**
     * Add an action listener on actionDesc
     * @param actionDesc the action description
     * @param action the action listener to add
     */
    public synchronized void addActionListener(String actionDesc, ActionListener action) {
        // add to default set of KeyBindings
        KeyBindings keys = (KeyBindings)actionMap.get(actionDesc);
        if (keys == null) {
            keys = new KeyBindings(actionDesc);
            actionMap.put(actionDesc, keys);
        }
        keys.addActionListener(action);
    }

    /**
     * Removes a key binding from the active bindings, and writes new bindings
     * set to preferences.
     * @param actionDesc the describing action
     * @param k the KeyStrokePair to remove
     */
    public synchronized void removeKeyBinding(String actionDesc, KeyStrokePair k) {

        HashMap inputMapToUse = inputMap;
        // if prefix stroke exists, remove one prefixAction key string
        // (may be more than one if more than one binding has prefixStroke as it's prefix)
        if (k.getPrefixStroke() != null) {
            List list = (List)inputMap.get(k.getPrefixStroke());
            if (list != null) {
                for (Iterator it = list.iterator(); it.hasNext(); ) {
                    String str = (String)it.next();
                    if (str.equals(prefixAction.actionDesc)) {
                        list.remove(str);
                        break;
                    }
                }
            }
            // get input map to use
            inputMapToUse = (HashMap)prefixedInputMapMaps.get(k.getPrefixStroke());
        }
        // remove stroke
        if (inputMapToUse != null) {
            List list = (List)inputMapToUse.get(k.getStroke());
            if (list != null) list.remove(actionDesc);
        }
        // remove action
        KeyBindings bindings = (KeyBindings)actionMap.get(actionDesc);
        bindings.removeKeyBinding(k);
        bindings.setUsingDefaultKeys(false);

        // user has modified bindings, write all current bindings to prefs
        setBindingsToPrefs(actionDesc);
    }

    /**
     * Get list of default KeyBindings for <code>actionDesc</code>
     * @param actionDesc the action description
     * @return list of KeyStrokePairs.
     */
    /*
    public synchronized List getDefaultKeyBindingsFor(String actionDesc) {
        KeyBindings keys = (KeyBindings)defaultActionMap.get(actionDesc);
        List bindings = new ArrayList();
        for (Iterator it = keys.getKeyStrokePairs(); it.hasNext(); ) {
            bindings.add((KeyStrokePair)it.next());
        }
        return bindings;
    }
*/

    /**
     * Set <code>actionDesc<code> to use default KeyBindings
     * @param actionDesc the action description
     */
    public synchronized void resetKeyBindings(String actionDesc) {
        // remove all previous bindings
        KeyBindings keys = (KeyBindings)actionMap.get(actionDesc);
        if (keys != null) {
            // get new iterator each time, because removeKeyStrokePair modifies the list
            while(true) {
                Iterator it = keys.getKeyStrokePairs();
                if (!it.hasNext()) break;
                KeyStrokePair pair = (KeyStrokePair)it.next();
                removeKeyBinding(actionDesc, pair);
            }
        }
        // remove any user saved preferences
        prefs.remove(actionDesc);
        // add in default key bindings
        for (Iterator it = keys.getDefaultKeyStrokePairs(); it.hasNext(); ) {
            KeyStrokePair k = (KeyStrokePair)it.next();
            addKeyBinding(actionDesc, k);
        }
        keys.setUsingDefaultKeys(true);
    }

    /**
     * Get bindings for action string
     * @param actionDesc string describing action (KeyBinding.actionDesc)
     * @return a KeyBindings object, or null.
     */
    public synchronized KeyBindings getKeyBindings(String actionDesc) {
        return (KeyBindings)actionMap.get(actionDesc);
    }

    /**
     * Set the faked event source of the KeyBindings object.  See
     * KeyBindings.setEventSource() for details.
     * @param actionDesc the action description used to find the KeyBindings object.
     * @param source the object to use as the source of the event. (Event.getSource()).
     */
    public synchronized void setEventSource(String actionDesc, Object source) {
        KeyBindings keys = (KeyBindings)actionMap.get(actionDesc);
        keys.setEventSource(source);
    }

    /**
     * Returns true if KeyBindings for the action described by
     * actionDesc are already present in hash tables.
     * @param actionDesc the action description of the KeyBindings
     * @return true if key binding found in manager, false otherwise.
     */
    /*
    public synchronized boolean hasKeyBindings(String actionDesc) {
        KeyBindings k = (KeyBindings)actionMap.get(actionDesc);
        if (k != null) return true;
        return false;
    }
*/

    /**
     * Get a list of KeyBindings that conflict with the key combo
     * <code>prefixStroke, stroke</code>.  A conflict is registered if:
     * an existing stroke is the same as <code>prefixStroke</code>; or
     * an existing stroke is the same <code>stroke</code>
     * (if <code>prefixStroke</code> is null);
     * or an existing prefixStroke,stroke combo is the same as
     * <code>prefixStroke,stroke</code>.
     * <p>
     * The returned list consists of newly created KeyBindings objects, not
     * KeyBindings objects that are used in the key manager database.
     * This is because not all KeyStrokePairs in an existing KeyBindings
     * object will necessarily conflict.  However, there may be more than
     * one KeyStrokePair in a returned KeyBindings object from the list if
     * more than one KeyStrokePair does actually conflict.
     * <p>
     * Returns an empty list if there are no conflicts.
     * @param pair the KeyStrokePair
     * @return a list of conflicting <code>KeyBindings</code>.  Empty list if no conflicts.
     */
    public synchronized List getConflictingKeyBindings(KeyStrokePair pair) {

        List conflicts = new ArrayList();               // list of actual KeyBindings
        List conflictsStrings = new ArrayList();        // list of action strings

        HashMap inputMapToUse = inputMap;

        if (pair.getPrefixStroke() != null) {
            // check if conflicts with any single key Binding
            List list = (List)inputMap.get(pair.getPrefixStroke());
            if (list != null) {
                for (Iterator it = list.iterator(); it.hasNext(); ) {
                    String str = (String)it.next();
                    if (str.equals(prefixAction.actionDesc)) continue;
                    // add to conflicts
                    conflictsStrings.add(str);
                }
            }
            inputMapToUse = (HashMap)prefixedInputMapMaps.get(pair.getPrefixStroke());
        }
        // find stroke conflicts
        if (inputMapToUse != null) {
            List list = (List)inputMapToUse.get(pair.getStroke());
            if (list != null) {
                for (Iterator it = list.iterator(); it.hasNext(); ) {
                    String str = (String)it.next();

                    if (str.equals(prefixAction.actionDesc)) {
                        // find all string associated with prefix in prefix map
                        // NOTE: this condition is never true if prefixStroke is valid
                        // and we are using a prefixed map...prefixActions are only in primary inputMap.
                        HashMap prefixMap = (HashMap)prefixedInputMapMaps.get(pair.getStroke());
                        if (prefixMap != null) {
                            for (Iterator it2 = prefixMap.values().iterator(); it2.hasNext(); ) {
                                // all existing prefixStroke,stroke combos conflict, so add them all
                                List prefixList = (List)it2.next(); // this is a list of strings
                                conflictsStrings.addAll(prefixList);
                            }
                        }
                    } else {
                        conflictsStrings.add(str);              // otherwise this is a key actionDesc
                    }
                }
            }
        }
        // get all KeyBindings from ActionMap
        for (Iterator it = conflictsStrings.iterator(); it.hasNext(); ) {
            ActionListener action = (ActionListener)actionMap.get((String)it.next());
            if (action == null) continue;
            if (action instanceof PrefixAction) continue;
            KeyBindings keys = (KeyBindings)action;
            KeyBindings conflicting = new KeyBindings(keys.getActionDesc());
            for (Iterator it2 = keys.getKeyStrokePairs(); it2.hasNext(); ) {
                // Unfortunately, any keyBinding can map to this action, including
                // ones that don't actually conflict.  So we need to double check
                // if binding really conflicts.
                KeyStrokePair pair2 = (KeyStrokePair)it2.next();
                if (pair.getPrefixStroke() != null) {
                    // check prefix conflict
                    if (pair2.getPrefixStroke() != null) {
                        // only conflict is if both prefix and stroke match
                        if (pair.getStroke() == pair2.getStroke())
                            conflicting.addKeyBinding(pair2);
                    } else {
                        // conflict if prefixStroke matches pair2.stroke
                        if (pair.getPrefixStroke() == pair2.getStroke())
                            conflicting.addKeyBinding(pair2);
                    }
                } else {
                    // no prefixStroke
                    if (pair2.getPrefixStroke() != null) {
                        // conflict if stroke matches pair2.prefixStroke
                        if (pair.getStroke() == pair2.getPrefixStroke())
                            conflicting.addKeyBinding(pair2);
                    } else {
                        // no prefixStroke, both only have stroke
                        if (pair.getStroke() == pair2.getStroke())
                            conflicting.addKeyBinding(pair2);
                    }
                }
            }
            // add conflicting KeyBindings to list if it has bindings in it
            Iterator conflictingIt = conflicting.getKeyStrokePairs();
            if (conflictingIt.hasNext()) conflicts.add(conflicting);
        }
        return conflicts;
    }

    // --------------------------------- Private -------------------------------------

    /**
     * Adds a KeyStrokePair <i>pair</i> as an active binding for action <i>actionDesc</i>.
     * @param actionDesc the action description
     * @param pair a key stroke pair
     * @return the new KeyBindings object, or an existing KeyBindings object for actionDesc
     */
    private synchronized KeyBindings addKeyBinding(String actionDesc, KeyStrokePair pair) {
        if (pair == null) return null;

        System.out.println("Adding binding for "+actionDesc+": "+pair.toString());
        KeyStroke prefixStroke = pair.getPrefixStroke();
        KeyStroke stroke = pair.getStroke();

        HashMap inputMapToUse = inputMap;
        if (prefixStroke != null) {
            // find HashMap based on prefixAction
            inputMapToUse = (HashMap)prefixedInputMapMaps.get(prefixStroke);
            if (inputMapToUse == null) {
                inputMapToUse = new HashMap();
                prefixedInputMapMaps.put(prefixStroke, inputMapToUse);
            }
            // add prefix action to primary input map
            List list = (List)inputMap.get(prefixStroke);
            if (list == null) {
                list = new ArrayList();
                inputMap.put(prefixStroke, list);
            }
            list.add(prefixAction.actionDesc);
        }
        // add stroke to input map to use
        List list = (List)inputMapToUse.get(stroke);
        if (list == null) {
            list = new ArrayList();
            inputMapToUse.put(stroke, list);
        }
        list.add(actionDesc);
        // add stroke to KeyBindings
        KeyBindings keys = (KeyBindings)actionMap.get(actionDesc);
        if (keys == null) {
            // no bindings for actionDesc
            keys = new KeyBindings(actionDesc);
            actionMap.put(actionDesc, keys);
        }
        keys.addKeyBinding(pair);

        return keys;
    }

    // ---------------------------- Preferences Storage ------------------------------

    /**
     * Add KeyBinding to stored user preferences.
     * @param actionDesc the action description under which to store all the key bindings
     */
    private synchronized void setBindingsToPrefs(String actionDesc) {
        if (prefs == null) return;
        if (actionDesc == null || actionDesc.equals("")) return;

        KeyBindings keyBindings = (KeyBindings)actionMap.get(actionDesc);
        if (keyBindings == null) return;
        prefs.put(prefPrefix+actionDesc, keyBindings.bindingsToString());
    }


    /**
     * Get KeyBindings for <code>actionDesc</code> from Preferences.
     * Returns null if actionDesc not present in prefs.
     * @param actionDesc the action description associated with these bindings
     * @return a list of KeyStrokePairs
     */
    private synchronized List getBindingsFromPrefs(String actionDesc) {
        if (prefs == null) return null;
        if (actionDesc == null || actionDesc.equals("")) return null;

        String keys = prefs.get(prefPrefix+actionDesc, null);
        if (keys == null) return null;
        //System.out.println("Read from prefs for "+actionDesc+": "+keys);
        KeyBindings k = new KeyBindings(actionDesc);
        k.addKeyBindings(keys);
        //System.out.println("  turned into: "+k.describe());
        List bindings = new ArrayList();
        for (Iterator it = k.getKeyStrokePairs(); it.hasNext(); ) {
            bindings.add((KeyStrokePair)it.next());
        }
        return bindings;
    }

}
