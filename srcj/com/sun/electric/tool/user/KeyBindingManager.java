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
 * a list of KeyBinding objects.  Each key binding object has an action which can then be
 * executed.  HOWEVER, only the first object in the list is activated. This means that
 * only that latest action bound to an action description will be activated.  This means
 * one key stroke cannot activate multiple actions.
 * <p>
 * This model is similar to jawa.swing.InputMap and java.swing.ActionMap, but has
 * been modified to use lists so that multiple keys can be bound to the same action.
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
public class KeyBindingManager implements KeyEventPostProcessor {

    // ----------------------------- object stuff ---------------------------------
    /** Hash table of lists all key bindings */     private HashMap inputMap;
    /** Hash table of all actions */                private HashMap actionMap;
    /** last prefix key pressed */                  private KeyStroke lastPrefix;
    /** Hash table of hash of lists of prefixed key bindings */ private HashMap prefixedInputMapMaps;
    /** action to take on prefix key hit */         private PrefixAction prefixAction;
    /** where to store Preferences */               private Preferences prefs;
    /** Hash table of lists of default key bindings*/ private HashMap defaultActionMap;

    // ----------------------------- global stuff ----------------------------------
    /** Listener to register for catching keys */   public static KeyBindingListener listener = new KeyBindingListener();
    /** All key binding manangers */                private static List allManagers = new ArrayList();

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

        // add prefix action to action map
        List prefixList = new ArrayList();
        prefixList.add(prefixAction);           // this list will always be size 1
        actionMap.put(prefixAction.actionDesc, prefixList);

        // register this with KeyboardFocusManager
        // so we receive all KeyEvents
        //KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
        //KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(this);

        // add to list of all managers
        allManagers.add(this);
    }

    /**
     * Called when disposing of this manager, allows memory used to
     * be reclaimed by removing static references to this.
     */
    public void finished() {
        allManagers.remove(this);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventPostProcessor(this);
    }

    /**
     * Class to store a key binding
     */
    public static class KeyBinding implements ActionListener
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

        public void actionPerformed(ActionEvent e) {
            action.actionPerformed(e);
        }
    }

    /**
     * Class PrefixAction is an action performed when a prefix key is hit.
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
            System.out.println("prefix key '"+keyStrokeToString(stroke)+"' hit...");
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

    public synchronized boolean processKeyEvent(KeyEvent e) {

        // only look at key pressed events
        if (e.getID() != KeyEvent.KEY_PRESSED) return false;
        // ignore modifier only events (CTRL, SHIFT etc just by themselves)
        if (e.getKeyChar() == KeyEvent.CHAR_UNDEFINED) return false;


        System.out.println("last Prefix key is "+lastPrefix);
        System.out.println("got event (consumed="+e.isConsumed()+")"+e);
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
        // get list of action strings, iterate over them
        List keyBindingList = (List)inputMapToUse.get(stroke);
        if (keyBindingList == null) { lastPrefix = null; return false; }            // nothing bound to key
        for (Iterator it = keyBindingList.iterator(); it.hasNext(); ) {
            String actionDesc = (String)it.next();

            // get KeyBinding object from action map, activate its action
            // note that if this is a prefixed action, this could actually be a
            // PrefixAction object instead of a KeyBinding object.
            List list = (List)actionMap.get(actionDesc);
            // only activate first object
            if (list != null && (list.size() > 0)) {
                action = (ActionListener)list.get(0);
                if (action instanceof PrefixAction) {
                    action.actionPerformed(evt);
                } else {
                    action.actionPerformed(evt);
                    lastPrefix = null;
                }
                e.consume();                // consume event
                actionPerformed = true;
            }
        }

        System.out.println(" actionPerformed="+actionPerformed);
        // if no action performed, perhaps the user hit a prefix key, then
        // decided to start another prefix-key-combo (that does not result in
        // a valid binding with the first prefix, obviously).  We'll be nice
        // and check for this case
        if (!actionPerformed) {
            HashMap prefixMap = (HashMap)prefixedInputMapMaps.get(stroke);
            if (prefixMap != null) {
                // valid prefix key, fire prefix event
                prefixAction.actionPerformed(evt);
                actionPerformed = true;
            }
        }

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
     * @param stroke the main stroke
     * @param prefixStroke the prefix stroke (ignored if null)
     * @return a list of conflicting KeyBindings from all KeyBindingManagers
     */
    public static List getConflictsAllManagers(KeyStroke stroke, KeyStroke prefixStroke) {
        List conflicts = new ArrayList();
        for (Iterator it = allManagers.iterator(); it.hasNext(); ) {
            KeyBindingManager m = (KeyBindingManager)it.next();
            conflicts.addAll(m.getConflictingKeyBindings(stroke, prefixStroke));
        }
        return conflicts;
    }

    // --------------------------------------------------------------------------------

    /**
     * Adds a default KeyBinding. If any keyBindings are found for
     * <code>k.actionDesc</code>, those are used instead.  Note that <code>k</code>
     * cannot be null, but it's stroke and prefixStroke can be null.  However,
     * it's actionDesc and action must be valid.
     * @param k the default KeyBinding
     */
    public synchronized void addDefaultKeyBinding(KeyBinding k) {
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
            defaultActionMap.put(k.actionDesc, list);
            prefsLoaded = false;
        }
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
     * Get list of default KeyBindings for <code>actionDesc</code>
     * @param actionDesc specifies action
     * @return list of default KeyBinding objects for actionDesc
     */
    public synchronized List getDefaultKeyBindings(String actionDesc) {
        return (List)defaultActionMap.get(actionDesc);
    }

    /**
     * Set <code>actionDesc<code> to use default KeyBindings
     * @param actionDesc the action to reset to default KeyBindings
     */
    public synchronized void setToDefaultBindings(String actionDesc) {
        // remove all previous bindings
        List list = (List)actionMap.get(actionDesc);
        if (list != null) {
            // copy list, cause it will be modified as we process it's elements
            List listcopy = new ArrayList();
            listcopy.addAll(list);
            for (Iterator it = listcopy.iterator(); it.hasNext(); ) {
                KeyBinding k = (KeyBinding)it.next();
                removeKeyBinding(k);
            }
        }
        // remove any user saved preferences
        prefs.remove(actionDesc);
        // add in default key bindings
        List defaults = (List)defaultActionMap.get(actionDesc);
        if (defaults != null) {
            for (Iterator it = defaults.iterator(); it.hasNext(); ) {
                KeyBinding k = (KeyBinding)it.next();
                addKeyBinding(k);
            }
        }
    }

    /**
     * Adds a user specified KeyBinding. Also adds it to stored user preference.
     * @param k the KeyBinding to add.
     */
    public synchronized void addUserKeyBinding(KeyBinding k) {
        // sanity checks
        if (k.actionDesc == null || k.action == null) {
            System.out.println("  Attempting to add invalid user key binding");
            return;
        }

        // add to bindings
        addKeyBinding(k);
        // user has modified bindings, write all current bindings to prefs
        setBindingsToPrefs(k.actionDesc);
    }

    /**
     * Adds a new KeyBinding.  Note that an action bound to a key that is also
     * a prefix key for another action will result in both the action occuring
     * and the prefix key enabling the prefixed action.
     * <p>This is a private method.  addDefault- or addUserKeyBindings should be
     * called from external code.
     * @param k the KeyBinding to add.
     */
    private synchronized void addKeyBinding(KeyBinding k) {

        //System.out.println("adding binding for "+k.actionDesc);
        // ignore duplicate bindings
        if (hasKeyBinding(k)) return;
        if (k.stroke == null) return;

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
    }

    /**
     * Get bindings for action string
     * @param actionDesc string describing action (KeyBinding.actionDesc)
     * @return a list of KeyBinding objects, or null.
     */
    public synchronized List getBindingsFor(String actionDesc) {
        return (List)actionMap.get(actionDesc);
    }

    /**
     * Returns true if KeyBinding k is already contained
     * in the KeyBindingManager.  Note that equality of
     * actions are not checked; only the actionDesc, and
     * the two KeyStrokes.  This is because many actions are
     * anonymous inner classes that instantiated over and over again.
     * @param k KeyBinding to check for
     * @return true if key binding found in manager, false otherwise.
     */
    public synchronized boolean hasKeyBinding(KeyBinding k) {
        List list = (List)actionMap.get(k.actionDesc);
        if (list == null) return false;
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            KeyBinding key = (KeyBinding)it.next();
            if (key.equals(k)) return true;
        }
        return false;
    }

    /**
     * Removes a key binding.
     * @param k
     */
    public synchronized void removeKeyBinding(KeyBinding k) {

        HashMap inputMapToUse = inputMap;

        // remove any prefix stroke
        if (k.prefixStroke != null) {
            List list = (List)inputMap.get(k.prefixStroke);
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
            inputMapToUse = (HashMap)prefixedInputMapMaps.get(k.prefixStroke);
        }

        // remove stroke
        if (inputMapToUse != null) {
            List list = (List)inputMapToUse.get(k.stroke);
            if (list != null) list.remove(k.actionDesc);
        }

        // remove action
        List list = (List)actionMap.get(k.actionDesc);
        if (list != null) list.remove(k);

        // user has modified bindings, write all current bindings to prefs
        setBindingsToPrefs(k.actionDesc);
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
    public synchronized List getConflictingKeyBindings(KeyStroke stroke, KeyStroke prefixStroke) {

        List conflicts = new ArrayList();
        List conflictsStrings = new ArrayList();

        HashMap inputMapToUse = inputMap;

        if (prefixStroke != null) {
            // check if conflicts with any single key Binding
            List list = (List)inputMap.get(prefixStroke);
            if (list != null) {
                for (Iterator it = list.iterator(); it.hasNext(); ) {
                    String str = (String)it.next();
                    if (str.equals(prefixAction.actionDesc)) continue;
                    // add to conflicts
                    conflictsStrings.add(str);
                }
            }
            inputMapToUse = (HashMap)prefixedInputMapMaps.get(prefixStroke);
        }
        // find stroke conflicts
        if (inputMapToUse != null) {
            List list = (List)inputMapToUse.get(stroke);
            if (list != null) {
                for (Iterator it = list.iterator(); it.hasNext(); ) {
                    String str = (String)it.next();

                    if (str.equals(prefixAction.actionDesc)) {
                        // find all string associated with prefix in prefix map
                        // NOTE: this condition is never true if prefixStroke is valid
                        // and we are using a prefixed map...prefixActions are only in primary inputMap.
                        HashMap prefixMap = (HashMap)prefixedInputMapMaps.get(stroke);
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
            List list2 = (List)actionMap.get((String)it.next());
            if (list2 != null && list2.size() > 0)
                conflicts.add(list2.get(0));            // only first "action" is valid
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
     */
    private synchronized void setBindingsToPrefs(String actionDesc) {
        if (prefs == null) return;
        List keyBindings = getBindingsFor(actionDesc);
        if (keyBindings == null) {
            prefs.put(actionDesc, "");            // write empty list, basically
            return;
        }
        prefs.put(actionDesc, keyBindingsToString(keyBindings));
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
    private synchronized List getBindingsFromPrefs(String actionDesc) {
        if (prefs == null) return null;

        String keys = prefs.get(actionDesc, null);
        return stringToKeyBindings(actionDesc, keys);
    }

    /** KeyEvent separator */           public static final String keyEventSeparator = ", ";
    /** KeyBinding separator */         public static final String keyBindingSeparator = "; ";

    /**
     * Convert string to list of KeyBindings.  KeyBindings are
     * separated by "; ", and KeyEvents within a KeyBinding are
     * separated by ", ".  Note that actionDesc must be supplied,
     * and action is null on the KeyBindings.
     * @param actionDesc the actionDesc for the KeyBindings
     * @param keys a string specifying the key bindings
     * @return a list of KeyBindings
     */
    public static List stringToKeyBindings(String actionDesc, String keys) {
        List list = new ArrayList();

        if (keys == null) return null;                  // null str, null result
        if (keys.equals("")) return list;               // empty str, empty result

        // special case for now: conform to old style
        if (keys.indexOf(keyBindingSeparator) == -1) {
            KeyStroke k = stringToKeyStroke(keys);
            if (k != null) {
                list.add(new KeyBinding(actionDesc, stringToKeyStroke(keys), null, null));
                return list;
            }
        }

        String [] bindings = keys.split(keyBindingSeparator);
        for (int i = 0; i < bindings.length; i++) {
            String binding = bindings[i];
            KeyStroke stroke, prefixStroke;
            String [] splitKeys = binding.split(keyEventSeparator);
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
     * Convert list of KeyBindings to string.  See stringToKeyBindings().
     * @param keyBindings list of KeyBindings
     * @return a string representing a list of key bindings
     */
    public static String keyBindingsToString(List keyBindings) {
        StringBuffer keys = new StringBuffer();
        for (Iterator it = keyBindings.iterator(); it.hasNext(); ) {
            KeyBinding binding = (KeyBinding)it.next();
            keys.append(keyStrokeToString(binding.prefixStroke) + keyEventSeparator +
                    keyStrokeToString(binding.stroke));
            if (it.hasNext()) keys.append(keyBindingSeparator);
        }
        return keys.toString();
    }
}
