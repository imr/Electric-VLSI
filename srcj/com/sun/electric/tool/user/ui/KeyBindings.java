/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: KeyBindings.java
 * Written by: Jonathan Gainsley, Sun Microsystems.
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
package com.sun.electric.tool.user.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.KeyStroke;

/**
 * The KeyBindings object holds information about an action's key bindings.
 * There can be 0 or more default bindings which are set by the program.
 * There can be 0 or more user defined bindings which are stored as preferences by
 * the KeyBindingManager. Finally, there is a set of action listeners that get
 * activated when the action occurs.
 *
 * <p>The action can also be disabled/enabled.
 */
public class KeyBindings implements ActionListener {
    /** description of action */            private String actionDesc;
    /** list of KeyStrokePairs */           private List<KeyStrokePair> keyStrokePairs;
    /** default KeyStrokePairs */           private List<KeyStrokePair> defaultKeyStrokePairs;
    /** actions to perform */               private List<ActionListener> actionListeners;
    /** source to use when sending event to listeners */ private Object eventSource;
    /** true if KeyBindingManager using default keys */  private boolean usingDefaultKeys;
    /** if these bindings are enabled */    private boolean enabled;

    /** separator for keyBindingsToString */private static final String sep = "; ";

    /**
     * Constructs a new KeyBinding.
     * @param actionDesc description of the key binding to show to the user
     */
    public KeyBindings(String actionDesc) {
        this.actionDesc = actionDesc;
        keyStrokePairs = new ArrayList<KeyStrokePair>();
        defaultKeyStrokePairs = new ArrayList<KeyStrokePair>();
        actionListeners = new ArrayList<ActionListener>();
        eventSource = null;
        usingDefaultKeys = true;
        enabled = true;
    }

    // ------------------------ Add/Remove KeyBindings --------------------------

    /**
     * Add a one or two-stroke key binding.
     * @param prefixStroke optional prefix stroke. may be null
     * @param stroke required stroke
     */
    public void addKeyBinding(KeyStroke prefixStroke, KeyStroke stroke) {
        KeyStrokePair k = KeyStrokePair.getKeyStrokePair(prefixStroke, stroke);
        keyStrokePairs.add(k);
    }

    /**
     * Add a key stroke pair
     * @param k the key stroke pair
     */
    public void addKeyBinding(KeyStrokePair k) {
        keyStrokePairs.add(k);
    }

    /**
     * Add key bindings. The string can be a list of KeyStrokePairs.toString(),
     * delimited by <code>KeyBindings.sep</code>.
     * @param str string representing a list of key stroke pairs.
     */
    public void addKeyBindings(String str) {
        // split by separator
        String [] pairs = str.split(sep);
        for (int i = 0; i < pairs.length; i++) {
            String pair = pairs[i];
            KeyStrokePair k = KeyStrokePair.getKeyStrokePair(pair);
            if (k != null)
                keyStrokePairs.add(k);
        }
    }

    /**
     * Removes a key binding
     * @param k the key stroke pair to remove
     */
    public void removeKeyBinding(KeyStrokePair k) {
        keyStrokePairs.remove(k);
    }

    /**
     * Add a one or two-stroke key binding to the default bindings list
     * @param prefixStroke optional prefix stroke. may be null
     * @param stroke required stroke
     */
    public void addDefaultKeyBinding(KeyStroke prefixStroke, KeyStroke stroke) {
        KeyStrokePair k = KeyStrokePair.getKeyStrokePair(prefixStroke, stroke);
        defaultKeyStrokePairs.add(k);
    }

    /**
     * Add a key stroke pair to the default bindings list
     * @param k the key stroke pair
     */
    public void addDefaultKeyBinding(KeyStrokePair k) {
        defaultKeyStrokePairs.add(k);
    }

    /**
     * Clear all active bindings
     */
    public void clearKeyBindings() {
        keyStrokePairs.clear();
    }

    // ----------------------- Action Listener stuff -------------------------

    /**
     * Add an action listener.
     * @param a the action listener to add
     */
    public void addActionListener(ActionListener a) {
        actionListeners.add(a);
    }

    /** Remove an action listener */
    public void removeActionListener(ActionListener a) {
        actionListeners.remove(a);
    }

    /**
     * Perform all registered actions
     * @param e the ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        if (!enabled) return;
        if (eventSource != null)
            e.setSource(eventSource);               // fake the source of the event
        for (ActionListener action : actionListeners) {
            action.actionPerformed(e);
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Convert key bindings to a string
     */
    public String bindingsToString() {
        StringBuffer buf = new StringBuffer("");
        for (Iterator<KeyStrokePair> it = keyStrokePairs.iterator(); it.hasNext(); ) {
            KeyStrokePair k = it.next();
            buf.append(k.toString());
            if (it.hasNext()) buf.append(sep);
        }
        return buf.toString();
    }

    public String describe() {
        return "KeyBindings for '"+actionDesc+"': [ "+bindingsToString()+" ]";
    }

    /** Get the action description that describes this key binding */
    public String getActionDesc() { return actionDesc; }

    /** Get an iterator over the actionListeners */
    public Iterator getActionListeners() { return actionListeners.iterator(); }

    /** Get an iterator over the user key stroke pairs */
    public Iterator<KeyStrokePair> getKeyStrokePairs() { return keyStrokePairs.iterator(); }

    /** Get an iterator over the default key stroke pairs */
    public Iterator<KeyStrokePair> getDefaultKeyStrokePairs() { return defaultKeyStrokePairs.iterator(); }

    /**
     * Set the object that will used as the "source" when the actions in
     * this KeyBindings object are activated.  This is useful for faking
     * the source of the event, in the case where the actionListeners may
     * be sensitive to that value. (Event.getSource() value).
     * @param source the object to be set as the source of the event passed
     * to the actionListeners on activation.
     */
    public void setEventSource(Object source) { eventSource = source; }

    /**
     * Set the value of usingDefaultKeys. This is a flag used by the
     * KeyBindingManager to keep track of which set of key bindings is
     * active for this KeyBindings object (i.e. the user-modified set
     * or the default set).
     * @param b true if using default keys, false otherwise
     */
    public void setUsingDefaultKeys(boolean b) { usingDefaultKeys = b; }

    public boolean getUsingDefaultKeys() { return usingDefaultKeys; }

    public void setEnabled(boolean b) { enabled = b; }

    public boolean getEnabled() { return enabled; }
}
