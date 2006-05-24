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

package com.sun.electric.tool.user;


import com.sun.electric.tool.user.ui.KeyBindings;
import com.sun.electric.tool.user.ui.KeyStrokePair;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

/**
 * The KeyBindingManager manages key bindings and their associated actions. It
 * implements a <code>KeyListener</code> so it can be added as a key listener
 * to any component.
 * <p><p>
 * The <i>inputMap</i> uses <code>KeyStrokes</code> as it's keys, and stores Objects
 * of type Set.  The Set contains Strings and the set will guarantee they are not repetead.
 * <p>
 * Each String is then used as a key into the HashMap <i>actionMap</i> to retrieve
 * a KeyBindings object.  Each key bindings object has a list of actions which can then be
 * performed.
 * <p>
 * This model is similar to jawa.swing.InputMap and java.swing.ActionMap.
 * However, secondary InputMaps allow two-stroke key bindings.  Additionally,
 * the KeybindingManager has been enveloped in an object which can
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
public class KeyBindingManager {

    // ----------------------------- object stuff ---------------------------------
    /** Hash table of lists all key bindings */     private HashMap<KeyStroke,Set<String>> inputMap;
    /** Hash table of all actions */                private HashMap<String,Object> actionMap;
    /** last prefix key pressed */                  private KeyStroke lastPrefix;
    /** Hash table of hash of lists of prefixed key bindings */ private HashMap<KeyStroke,HashMap<KeyStroke,Set<String>>> prefixedInputMapMaps;
    /** action to take on prefix key hit */         private PrefixAction prefixAction;
    /** where to store Preferences */               private Preferences prefs;
    /** prefix on pref key, if desired */           private String prefPrefix;

    // ----------------------------- global stuff ----------------------------------
    /** Listener to register for catching keys */   //public static KeyBindingListener listener = new KeyBindingListener();
    /** All key binding manangers */                private static List<KeyBindingManager> allManagers = new ArrayList<KeyBindingManager>();

    /** debug preference saving */                  private static final boolean debugPrefs = false;
    /** debug key bindings */                       private static final boolean DEBUG = false;

    /**
     * Construct a new KeyBindingManager that can act as a KeyListener
     * on a Component.
     */
    public KeyBindingManager(String prefPrefix, Preferences prefs) {
        inputMap = new HashMap<KeyStroke,Set<String>>();
        actionMap = new HashMap<String,Object>();
        prefixedInputMapMaps = new HashMap<KeyStroke,HashMap<KeyStroke,Set<String>>>();
        lastPrefix = null;
        prefixAction = new PrefixAction(this);
        this.prefs = prefs;
        this.prefPrefix = prefPrefix;

        // add prefix action to action map
        actionMap.put(PrefixAction.actionDesc, prefixAction);

        // register this with KeyboardFocusManager
        // so we receive all KeyEvents
        //KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
        //KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(this);

        // add to list of all managers
        synchronized(allManagers) {
            allManagers.add(this);
        }
        initialize();
    }

    /**
     * Called when disposing of this manager, allows memory used to
     * be reclaimed by removing static references to this.
     */
    public void finished() {
        synchronized(allManagers) {
            allManagers.remove(this);
        }
        //KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventPostProcessor(this);
    }

    private boolean initialized = false;
    /**
     * Initialize: Reads all stored key bindings from preferences
     */
    private synchronized void initialize() {
/*        String [] allKeys;
        try {
            allKeys = prefs.keys();
        } catch (BackingStoreException e) {
            e.printStackTrace();
            return;
        }
        for (int i = 0; i < allKeys.length; i++) {
            // read bindings
            String key = allKeys[i].replaceFirst(prefPrefix, "");
            // old binding format and new format conflict, add check to avoid duplicates
            if (actionMap.containsKey(key)) continue;
            if (debugPrefs) System.out.println("looking for prefs key "+key);
            KeyBindings keys = new KeyBindings(key);
            List pairs = getBindingsFromPrefs(key);
            // if any bindings, set usingDefaults false, and add them
            if (pairs != null) {
                keys.setUsingDefaultKeys(false); // set usingDefaults false
                actionMap.put(key, keys);
                for (Iterator it = pairs.iterator(); it.hasNext(); ) {
                    KeyStrokePair pair = (KeyStrokePair)it.next();
                    if (pair == null) continue;
                    addKeyBinding(key, pair);
                }
            }
        }*/
    }

    private static class KeyBindingColumn
    {
        int hits = 0; // how many key bindings use this modifier
        int maxLength = 0;
        String name;

        KeyBindingColumn(String n)
        {
            name = n;
        }

        public String toString() { return name; }

        public String getHeader()
        {
            String n = name;
            for (int l = name.length(); l < maxLength; l++)
                n += " ";
            return n + " | ";
        }

        public String getColumn(Object value)
        {
            String column = "";
            int fillStart = 0;

            if (value != null)
            {
                String n = value.toString();
                fillStart = n.length();
                column += n;
            }
            // filling
            for (int l = fillStart; l < maxLength; l++)
                column +=" ";
            return column + " | ";
        }

        public void addHit(Set<String> set)
        {
            hits++;
            int len = set.toString().length();
            if (len > maxLength)
                maxLength = len;
        }

        public boolean equals(Object obj)
        {
            String key = obj.toString();
            boolean found = key.equals(name);
            return found;
        }
    }

    private static class KeyBindingColumnSort implements Comparator<KeyBindingColumn>
    {
		public int compare(KeyBindingColumn s1, KeyBindingColumn s2)
        {
			int bb1 = s1.hits;
			int bb2 = s2.hits;
            if (bb1 < bb2) return 1;    // sorting from max to min
            else if (bb1 > bb2) return -1;
            return (0); // identical
        }
    }

    /** Method to print existing KeyStrokes in std output for help
     */
    public void printKeyBindings()
    {
        Map<String,HashMap<String,Set<String>>> set = new HashMap<String,HashMap<String,Set<String>>>();
        List<String> keyList = new ArrayList<String>(); // has to be a list so it could be sorted.
        List<KeyBindingColumn> columnList = new ArrayList<KeyBindingColumn>(); // has to be a list so it could be sorted.
        KeyBindingColumn row = new KeyBindingColumn("Keys");
        Set<String> tmpSet = new HashSet<String>();
        columnList.add(row); // inserting the first row with key names as column

        for (Map.Entry<KeyStroke,Set<String>> map : inputMap.entrySet())
        {
            KeyStroke keyS = map.getKey();
            String key = KeyStrokePair.getStringFromKeyStroke(keyS);
            HashMap<String,Set<String>> m = set.get(key);
            if (m == null)
            {
                m = new HashMap<String,Set<String>>();
                set.put(key, m);
                keyList.add(key);
                tmpSet.clear();
                tmpSet.add(key);
                row.addHit(tmpSet);
            }
            String modifier = KeyEvent.getKeyModifiersText(keyS.getModifiers());
            KeyBindingColumn newCol = new KeyBindingColumn(modifier);
            int index = columnList.indexOf(newCol);
            KeyBindingColumn col = (index > -1) ? columnList.get(index) : null;
            if (col == null)
            {
                col = newCol;
                columnList.add(col);
            }
            col.addHit(map.getValue());
            m.put(modifier, map.getValue());
        }
        Collections.sort(keyList);
        Collections.sort(columnList, new KeyBindingColumnSort());

        // Header
        String headerLine = "\n";
        for (KeyBindingColumn column : columnList)
        {
            String header = column.getHeader();
            System.out.print(header);
            for (int i = 0; i < header.length(); i++)
                headerLine += "-";
        }
        System.out.println(headerLine);

        for (String key : keyList)
        {
//            System.out.print(key);
            for (KeyBindingColumn column : columnList)
            {
                Object value = (column == row) ? key : (Object)set.get(key).get(column.name);
                System.out.print(column.getColumn(value));
            }
            System.out.println();
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
        /** The action description analagous to KeyBinding */ public static final String actionDesc = "KeyBindingManager prefix action";
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
    private synchronized void setPrefixKey(KeyStroke prefix) {
        this.lastPrefix = prefix;
    }

    // ------------------------------ Key Processing ---------------------------------


/*
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
*/

    /**
     * Says whether or not KeyBindingManager will bind to this key event
     * @param e the KeyEvent
     * @return true if the KeyBindingManager can bind to this event
     */
    public static boolean validKeyEvent(KeyEvent e) {

        // only look at key pressed events
        if ((e.getID() != KeyEvent.KEY_PRESSED) && (e.getID() != KeyEvent.KEY_TYPED)) return false;

        // ignore modifier only events (CTRL, SHIFT etc just by themselves)
        if (e.getKeyCode() == KeyEvent.VK_CONTROL) return false;
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) return false;
        if (e.getKeyCode() == KeyEvent.VK_ALT) return false;
        if (e.getKeyCode() == KeyEvent.VK_META) return false;

        return true;
    }

    /**
     * Process a KeyEvent by finding what actionListeners should be
     * activated as a result of the event.  The keyBindingManager keeps
     * one stroke of history so that two-stroke events can be distinguished.
     * @param e the KeyEvent
     * @return true if event consumed, false if not and nothing done.
     */
    public synchronized boolean processKeyEvent(KeyEvent e) {

        if (DEBUG) System.out.println("got event (consumed="+e.isConsumed()+") "+e);

        // see if this is a valid key event
        if (!validKeyEvent(e)) return false;

        // get KeyStroke
        KeyStroke stroke = KeyStroke.getKeyStrokeForEvent(e);

        // remove modifiers from Events with undefined KeyCode (Key Typed events)
        // This lets KeyStrokes like '<' register correctly, because they are always
        // delivered as SHIFT-'<'.
        if (stroke.getKeyCode() == KeyEvent.VK_UNDEFINED) {
            stroke = KeyStroke.getKeyStroke(stroke.getKeyChar());
        }
        if (DEBUG) System.out.println("  Current key is "+stroke+", last prefix key is "+lastPrefix);

        // ignore if consumed
        if (e.isConsumed()) {
            lastPrefix = null;              // someone did something with it, null prefix key
            return false;
        }

        HashMap<KeyStroke,Set<String>> inputMapToUse = inputMap;

        // check if we should use prefixed key map instead of regular inputMap
        if (lastPrefix != null) {
            // get input map based on prefix key
            inputMapToUse = prefixedInputMapMaps.get(lastPrefix);
            if (inputMapToUse == null) { lastPrefix = null; return false; }
        }

        ActionListener action = null;
        ActionEvent evt = new ActionEvent(e, ActionEvent.ACTION_PERFORMED, stroke.toString(), stroke.getModifiers());

        boolean actionPerformed = false;
        boolean prefixActionPerformed = false;
        // get set of action strings, iterate over them
        Set<String> keyBindingList = inputMapToUse.get(stroke);
        if (keyBindingList != null) {
            for (String actionDesc : keyBindingList) {
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
            HashMap prefixMap = prefixedInputMapMaps.get(stroke);
            if (prefixMap != null) {
                // valid prefix key, fire prefix event
                prefixAction.actionPerformed(evt);
                actionPerformed = true;
            } else {
                lastPrefix = null;              // nothing to do
            }
        }

        if (DEBUG) System.out.println(" actionPerformed="+actionPerformed);
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
    public static List<KeyBindings> getConflictsAllManagers(KeyStrokePair pair) {
        List<KeyBindings> conflicts = new ArrayList<KeyBindings>();
        synchronized(allManagers) {
            for (KeyBindingManager m : allManagers) {
                conflicts.addAll(m.getConflictingKeyBindings(pair));
            }
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
/*
        if (keys.getUsingDefaultKeys()) {
            // using default keys, add default key to active maps
            addKeyBinding(actionDesc, pair);
        }
*/
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

        HashMap<KeyStroke,Set<String>> inputMapToUse = inputMap;
        // if prefix stroke exists, remove one prefixAction key string
        // (may be more than one if more than one binding has prefixStroke as it's prefix)
        if (k.getPrefixStroke() != null) {
            Set<String> set = inputMap.get(k.getPrefixStroke());
            if (set != null) {
                for (String str : set) {
                    if (str.equals(PrefixAction.actionDesc)) {
                        set.remove(str);
                        break;
                    }
                }
            }
            // get input map to use
            inputMapToUse = prefixedInputMapMaps.get(k.getPrefixStroke());
        }
        // remove stroke
        if (inputMapToUse != null) {
            Set<String> set = inputMapToUse.get(k.getStroke());
            if (set != null) set.remove(actionDesc);
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
                Iterator<KeyStrokePair> it = keys.getKeyStrokePairs();
                if (!it.hasNext()) break;
                KeyStrokePair pair = it.next();
                removeKeyBinding(actionDesc, pair);
            }
        }
        // remove any user saved preferences
        //prefs.remove(actionDesc);
        prefs.remove(prefPrefix+actionDesc);
        // add in default key bindings
        for (Iterator<KeyStrokePair> it = keys.getDefaultKeyStrokePairs(); it.hasNext(); ) {
            KeyStrokePair k = it.next();
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
     * Class that converts internal key mappings to InputMap and ActionMap objects.
     */
    public static class KeyMaps
    {
    	private InputMap im;
    	private ActionMap am;

    	KeyMaps(KeyBindingManager kbm, HashMap<KeyStroke,Set<String>> inputMap, HashMap<String,Object> actionMap)
    	{
        	im = new InputMap();
        	am = new ActionMap();
        	for(KeyStroke ks : inputMap.keySet())
        	{
        		Set<String> theSet = inputMap.get(ks);
        		String actionName = theSet.iterator().next();
        		im.put(ks, actionName);
        		am.put(actionName, new MyAbstractAction(actionName, kbm));
        	}
        }

    	public InputMap getInputMap() { return im; }

    	public ActionMap getActionMap() { return am; }
    }

    private static class MyAbstractAction extends AbstractAction
    {
    	private String actionName;
    	private KeyBindingManager kbm;

    	MyAbstractAction(String actionName, KeyBindingManager kbm)
    	{
    		this.actionName = actionName;
    		this.kbm = kbm;
    	}

    	public void actionPerformed(ActionEvent event)
    	{
    		KeyBindings kb = kbm.getKeyBindings(actionName);
    		kb.actionPerformed(event);
    	}
    }

    /**
     * Method to return an object that has real InputMap and ActionMap objects.
     * @return a KeyMaps object.
     */
    public KeyMaps getKeyMaps()
    {
    	KeyMaps km = new KeyMaps(this, inputMap, actionMap);
    	return km;
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
    public synchronized List<KeyBindings> getConflictingKeyBindings(KeyStrokePair pair) {

        List<KeyBindings> conflicts = new ArrayList<KeyBindings>();               // list of actual KeyBindings
        List<String> conflictsStrings = new ArrayList<String>();        // list of action strings

        HashMap<KeyStroke,Set<String>> inputMapToUse = inputMap;

        if (pair.getPrefixStroke() != null) {
            // check if conflicts with any single key Binding
            Set<String> set = inputMap.get(pair.getPrefixStroke());
            if (set != null) {
                for (String str : set) {
                    if (str.equals(PrefixAction.actionDesc)) continue;
                    // add to conflicts
                    conflictsStrings.add(str);
                }
            }
            inputMapToUse = prefixedInputMapMaps.get(pair.getPrefixStroke());
        }
        // find stroke conflicts
        if (inputMapToUse != null) {
            Set<String> set = inputMapToUse.get(pair.getStroke());
            if (set != null) {
                for (String str : set) {
                    if (str.equals(PrefixAction.actionDesc)) {
                        // find all string associated with prefix in prefix map
                        // NOTE: this condition is never true if prefixStroke is valid
                        // and we are using a prefixed map...prefixActions are only in primary inputMap.
                        HashMap<KeyStroke,Set<String>> prefixMap = prefixedInputMapMaps.get(pair.getStroke());
                        if (prefixMap != null) {
                            for (Iterator<Set<String>> it2 = prefixMap.values().iterator(); it2.hasNext(); ) {
                                // all existing prefixStroke,stroke combos conflict, so add them all
                                Set<String> prefixList = it2.next(); // this is a set of strings
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
        for (String aln : conflictsStrings) {
            ActionListener action = (ActionListener)actionMap.get(aln);
            if (action == null) continue;
            if (action instanceof PrefixAction) continue;
            KeyBindings keys = (KeyBindings)action;
            KeyBindings conflicting = new KeyBindings(keys.getActionDesc());
            for (Iterator<KeyStrokePair> it2 = keys.getKeyStrokePairs(); it2.hasNext(); ) {
                // Unfortunately, any keyBinding can map to this action, including
                // ones that don't actually conflict.  So we need to double check
                // if binding really conflicts.
                KeyStrokePair pair2 = it2.next();
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

    /**
     * Sets the enabled state of the action to 'b'. If b is false, it
     * disables all events that occur when actionDesc takes place. If b is
     * true, it enables all resulting events.
     * @param actionDesc the describing action
     * @param b true to enable, false to disable.
     */
    public synchronized void setEnabled(String actionDesc, boolean b) {
        ActionListener action = (ActionListener)actionMap.get(actionDesc);
        if (action == null) return;
        if (action instanceof PrefixAction) return;
        KeyBindings k = (KeyBindings)action;
        k.setEnabled(b);
    }

    /**
     * Get the enabled state of the action described by 'actionDesc'.
     * @param actionDesc the describing action.
     * @return true if the action is enabled, false otherwise.
     */
    public synchronized boolean getEnabled(String actionDesc) {
        ActionListener action = (ActionListener)actionMap.get(actionDesc);
        if (action == null) return false;
        if (action instanceof PrefixAction) return false;
        KeyBindings k = (KeyBindings)action;
        return k.getEnabled();
    }

    /**
     * Check if there are any bindings that do not have any
     * associated actions.
     */
    public synchronized void deleteEmptyBindings() {
        Set<String> keys = actionMap.keySet();
        for (String key : keys) {
            ActionListener action = (ActionListener)actionMap.get(key);
            if (action instanceof KeyBindings) {
                KeyBindings bindings = (KeyBindings)action;
                Iterator listenersIt = bindings.getActionListeners();
                if (!listenersIt.hasNext()) {
                    // no listeners on the action
                    System.out.println("Warning: Deleting defunct binding for "+key+" [ "+bindings.bindingsToString()+ " ]...action does not exist anymore");
                    // delete bindings
                    removeBindingsFromPrefs(key);
                }
            }
        }
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

        // warn if conflicting key bindings created
        List<KeyBindings> conflicts = getConflictingKeyBindings(pair);
        if (conflicts.size() > 0) {
            System.out.println("WARNING: Key binding for "+actionDesc+" [ " +pair.toString()+" ] conflicts with:");
            for (KeyBindings k : conflicts) {
                System.out.println("  > "+k.getActionDesc()+" [ "+k.bindingsToString()+" ]");
            }
        }

        if (DEBUG) System.out.println("Adding binding for "+actionDesc+": "+pair.toString());
        KeyStroke prefixStroke = pair.getPrefixStroke();
        KeyStroke stroke = pair.getStroke();

        HashMap<KeyStroke,Set<String>> inputMapToUse = inputMap;
        if (prefixStroke != null) {
            // find HashMap based on prefixAction
            inputMapToUse = prefixedInputMapMaps.get(prefixStroke);
            if (inputMapToUse == null) {
                inputMapToUse = new HashMap<KeyStroke,Set<String>>();
                prefixedInputMapMaps.put(prefixStroke, inputMapToUse);
            }
            // add prefix action to primary input map
            Set<String> set = inputMap.get(prefixStroke);
            if (set == null) {
                set = new HashSet<String>();
                inputMap.put(prefixStroke, set);
            }
            set.add(PrefixAction.actionDesc);
        }
        // add stroke to input map to use
        Set<String> set = inputMapToUse.get(stroke);
        if (set == null) {
            set = new HashSet<String>();
            inputMapToUse.put(stroke, set);
        }
        set.add(actionDesc);
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
        String actionDescAbbrev = actionDesc;
        if ((actionDesc.length() + prefPrefix.length()) > Preferences.MAX_KEY_LENGTH) {
            int start = actionDesc.length() + prefPrefix.length() - Preferences.MAX_KEY_LENGTH;
            actionDescAbbrev = actionDesc.substring(start, actionDesc.length());
        }
        if (debugPrefs) System.out.println("Writing to pref '"+prefPrefix+actionDescAbbrev+"': "+keyBindings.bindingsToString());
        prefs.put(prefPrefix+actionDescAbbrev, keyBindings.bindingsToString());
    }


    /**
     * Get KeyBindings for <code>actionDesc</code> from Preferences.
     * Returns null if actionDesc not present in prefs.
     * @param actionDesc the action description associated with these bindings
     * @return a list of KeyStrokePairs
     */
    private synchronized List<KeyStrokePair> getBindingsFromPrefs(String actionDesc) {
        if (prefs == null) return null;
        if (actionDesc == null || actionDesc.equals("")) return null;

        String actionDescAbbrev = actionDesc;
        if ((actionDesc.length() + prefPrefix.length()) > Preferences.MAX_KEY_LENGTH) {
            int start = actionDesc.length() + prefPrefix.length() - Preferences.MAX_KEY_LENGTH;
            actionDescAbbrev = actionDesc.substring(start, actionDesc.length());
        }
        String keys = prefs.get(prefPrefix+actionDescAbbrev, null);
        if (keys == null) return null;
        if (debugPrefs) System.out.println("Read from prefs for "+prefPrefix+actionDescAbbrev+": "+keys);
        KeyBindings k = new KeyBindings(actionDesc);
        k.addKeyBindings(keys);
        if (debugPrefs) System.out.println("  turned into: "+k.describe());
        List<KeyStrokePair> bindings = new ArrayList<KeyStrokePair>();
        for (Iterator<KeyStrokePair> it = k.getKeyStrokePairs(); it.hasNext(); ) {
            bindings.add(it.next());
        }
        return bindings;
    }

    /**
     * Restored saved bindings from preferences.  Usually called after
     * menu has been created.
     */
    public synchronized void restoreSavedBindings(boolean initialCall) {
        if (initialCall && initialized == true) return;
        initialized = true;
        if (prefs == null) return;
        // try to see if binding saved in preferences for each action
        for (Map.Entry<String,Object> entry : actionMap.entrySet()) {
            String actionDesc = (String)entry.getKey();
            if (actionDesc == null || actionDesc.equals("")) continue;
            // clear current bindings
            if (entry.getValue() instanceof PrefixAction) {
                continue;
            }
            KeyBindings bindings = (KeyBindings)entry.getValue();
            bindings.clearKeyBindings();
            // look up bindings in prefs
            List<KeyStrokePair> keyPairs = getBindingsFromPrefs(bindings.getActionDesc());
            if (keyPairs == null) {
                // no entry found, use default settings
                bindings.setUsingDefaultKeys(true);
                for (Iterator<KeyStrokePair> it2 = bindings.getDefaultKeyStrokePairs(); it2.hasNext(); ) {
                    KeyStrokePair pair = it2.next();
                    addKeyBinding(actionDesc, pair);
                }
            } else {
                // otherwise, add bindings found
                bindings.setUsingDefaultKeys(false);
                for (KeyStrokePair pair : keyPairs) {
                    addKeyBinding(actionDesc, pair);
                }
            }
        }

    }

    /**
     * Remove any bindings stored for actionDesc.
     */
    private synchronized void removeBindingsFromPrefs(String actionDesc) {
        if (prefs == null) return;
        if (actionDesc == null || actionDesc.equals("")) return;

        prefs.remove(prefPrefix+actionDesc);
    }

}
