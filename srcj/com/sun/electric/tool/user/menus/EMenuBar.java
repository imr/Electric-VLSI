/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EMenuBar.java
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
package com.sun.electric.tool.user.menus;

import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.KeyBindingManager;
import com.sun.electric.tool.user.KeyBindingManager.KeyMaps;
import com.sun.electric.tool.user.ui.KeyBindings;
import com.sun.electric.tool.user.ui.KeyStrokePair;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.*;

/**
 * EMenuBar is a menu bar template. It associates several menu bars together.
 * This maintains consistency of state and key bindings.
 * It also acts as an listener for updating all menu items on a state change.
 */
public class EMenuBar extends EMenu {
    
//    /** all groups */                                               private static final HashMap<String,EMenuBar> menuBarGroups = new HashMap<String,EMenuBar>();
    
    /** Preferences for User Bindings */                            private Preferences prefs;
    /** All menu items created, stores as ArrayLists in HashMap */  HashMap<String,EMenuItem> menuItems = new HashMap<String,EMenuItem>();
    /** Key Binding Manager for menu items */                       public final KeyBindingManager keyBindingManager;
    /** Hidden menu */                                              private final EMenu hiddenMenu;
    /** Updatable items. */                                         private final ArrayList<EMenuItem> updatableItems = new ArrayList<EMenuItem>();  
    
//    /** Factory method to create/get a group */
//    public static EMenuBar newInstance(String name, EMenu hiddenMenu, EMenu... items) {
//        
//        synchronized(menuBarGroups) {
//            if (menuBarGroups.containsKey(name)) {
//                return menuBarGroups.get(name);
//            }
//            EMenuBar menuBar = new EMenuBar(name, hiddenMenu, items);
//            menuBarGroups.put(name, menuBar);
//            return menuBar;
//        }
//    }
    
    /**
	 *@param name name of generic menu bar.
     *@param hiddenMenu menu which items are invoked by shortcuts only.
     *@param items var-arg menu items. Null arguments are skipped.
     */
    EMenuBar(String name, EMenu hiddenMenu, List<EMenuItem> items) {
        super(name, items);
        this.hiddenMenu = hiddenMenu;
        prefs = Preferences.userNodeForPackage(EMenuBar.class);
        keyBindingManager = new KeyBindingManager(name+"MenuKeyBinding-", prefs);
        // add to hashmap of existing groups
        this.path = new int[0];
        int indexInParent = 0;
        for (EMenuItem item: items)
        {
            if (item != null)
                item.registerTree(this, path, indexInParent++);
        }
        if (hiddenMenu != null)
            hiddenMenu.registerTree(this, path, -1);
    }

    /**
     * Returns hidden menu whcih items are invoked by shortcuts only.
     */
    public EMenu getHiddenMenu() {
        return hiddenMenu;
    }
    
    /** Get a string description of the menu item.
     * Takes the form <p>
     * Menu | SubMenu | SubMenu | item
     * <p>
     * @param path a path to EMenuItem
     * @return a string of the description.
     */
    public String getDescription(int[] path) {
        StringBuilder sb = new StringBuilder();
        int topIndex = path[0];
        EMenuItem item = topIndex >= 0 ? items.get(topIndex) : hiddenMenu;
        sb.append(item.getText());
        for (int i = 1; i < path.length; i++) {
            EMenu menu = (EMenu)item;
            item = menu.items.get(path[i]);
            sb.append(" | ");
            sb.append(item.getText());
        }
        return sb.toString();
    }
    
    /**
     * Register key binding of menu item.
     */
    void registerKeyBindings(EMenuItem menuItem) {
        String key = menuItem.getDescription();
        menuItems.put(key, menuItem);
        
        // add default binding
        KeyStroke [] bindings = menuItem.getAccelerators();
        for (int i=0; i<bindings.length; i++)
            addDefaultKeyBinding(menuItem, bindings[i]);
        
        // The generic menu item is an action listener for instance menu items.
        keyBindingManager.addActionListener(key, menuItem);
    } 
    
    /**
     * Register updatable menu item.
     */
    void registerUpdatable(EMenuItem menuItem) {
        updatableItems.add(menuItem);
    }
        
    /**
     * Update updatable buttons of this menu.
     */
    public void updateAllButtons() {
        for (EMenuItem item: updatableItems)
            item.updateJMenuItems();
    }
    
    /**
     * Get the key bindings for the menu item.
     * @return key bindings for the menu item, or null if no item or no bindings.
     */
    public KeyBindings getKeyBindings(EMenuItem item) {
        return keyBindingManager.getKeyBindings(item.getDescription());
    }
    
    /**
     * Adds a default KeyBinding.
     * @param actionDesc the action description
     * @param pair a key stroke pair
     */
    private void addDefaultKeyBinding(EMenuItem item, KeyStroke stroke) {
        String description = item.getDescription();
        keyBindingManager.addDefaultKeyBinding(description, KeyStrokePair.getKeyStrokePair(null, stroke));
    }
    
    /**
     * Add a user defined Key binding. This gets stored to preferences.
     * @param item the menu item
     * @param stroke the key stroke bound to menu item
     * @param prefixStroke an option prefix stroke (may be null)
     */
    public void addUserKeyBinding(EMenuItem item, KeyStroke stroke, KeyStroke prefixStroke) {
        String description = item.getDescription();
        keyBindingManager.addUserKeyBinding(description, KeyStrokePair.getKeyStrokePair(prefixStroke, stroke));
        updateAccelerator(item);
     }
    
    /**
     * Sets <code>item<code> back to default Key Bindings
     * @param item the item to reset to default bindings
     */
    public void resetKeyBindings(EMenuItem item) {
        // tell KeyBindingManager to reset to default KeyBindings
        keyBindingManager.resetKeyBindings(item.getDescription());
        updateAccelerator(item);
    }
    
    /**
     * Sets *All* menu items back to their default key bindings
     */
    public void resetAllKeyBindings() {
        for (EMenuItem item: menuItems.values())
            resetKeyBindings(item);
    }
    
    /**
     * Removes a key binding.
     * @param actionDesc the item to remove the binding from
     * @param pair the key stroke pair to remove
     */
    public void removeKeyBinding(String actionDesc, KeyStrokePair pair) {
        // remove binding from binding manager
        keyBindingManager.removeKeyBinding(actionDesc, pair);
        EMenuItem item = menuItems.get(actionDesc);
        if (item != null)
            updateAccelerator(item);
    }
    
    public void restoreSavedBindings(boolean initialCall) {
        keyBindingManager.restoreSavedBindings(initialCall);
        for (EMenuItem item: menuItems.values())
            updateAccelerator(item);
    }
    
    /**
     * Updates a menu item's accelerator.  Menu item is specified by
     * actionDesc (MenuItem.getDescription()).  This is usually called after a menu item's bindings
     * have changed (binding removed, added, or reset to default). This
     * updates the accelerator to a valid binding, which is displayed when
     * the user opens the menu.
     * @param actionDesc key for menu item
     */
    private void updateAccelerator(EMenuItem item) {
        // get valid key binding, update menus with it
        String actionDesc = item.getDescription();
        KeyBindings bindings = keyBindingManager.getKeyBindings(actionDesc);
        KeyStroke accelerator = null;
        if (bindings != null) {
            Iterator<KeyStrokePair> it;
            if (bindings.getUsingDefaultKeys()) it = bindings.getDefaultKeyStrokePairs(); else
                it = bindings.getKeyStrokePairs();
            while (it.hasNext()) {
                KeyStrokePair pair = it.next();
                if (pair.getPrefixStroke() != null) continue;         // menus can't display two-stroke key bindings
                accelerator = pair.getStroke();
                break;
            }
        }
        item.setAccelerator(accelerator);
    }

    /**
     * Method to return an object that has real InputMap and ActionMap objects.
     * @return a KeyMaps object.
     */
    public KeyMaps getKeyMaps()
    {
    	return keyBindingManager.getKeyMaps();
    }
    
    public Instance genInstance(WindowFrame frame) {
        return new Instance(frame);
    }

    public class Instance extends JMenuBar {
        /** whether to ignore all shortcuts keys */ boolean ignoreKeyBindings = false;
        /** whether to ignore text editing keys */  boolean ignoreTextEditKeys = false;
        
        /**
         * Create a new MenuBar that belongs to group "name". All MenuBars
         * in the group should have the same set of menu items. The group
         * serves to maintain consistency of state and key bindings across
         * multiple instances of MenuBars that share the same layout of
         * menu items.
         * @param frame
         */
        public Instance(WindowFrame frame) {
            for (EMenuItem item: getItems())
                add(item.genMenu(frame));
        }
        
        public EMenuBar getMenuBarGroup() { return EMenuBar.this; }
        
        /**
         * Overrides JMenuBar's processKeyBinding, which distributes event
         * to it's menu items.  Instead, we pass the event to the keyBindingManager
         * to process the event.
         * @param ks the KeyStroke of the event
         * @param e the KeyEvent
         * @param condition condition (focused/not etc)
         * @param pressed
         */
        @Override
        protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,
                int condition, boolean pressed) {
            int code = e.getKeyCode();
            char ch = e.getKeyChar();
            ActivityLogger.logMessage("ProcessKeyBinding " + e.getID() + " when=" + e.getWhen() +
                    " modifiers=" + Integer.toHexString(e.getModifiersEx()) +
                    " code=" + Integer.toHexString(code) + " char=" + Integer.toHexString(ch) +
                    (' ' < ch && ch < 0x7f ? "(" + ch + ")": "") +
                    " " + ignoreKeyBindings + " " + ignoreTextEditKeys);
            // ignore key bindings if set
            if (ignoreKeyBindings) return false;
            
            // if ignoreTextEditKeys, ignore anything that does not have CTRL
            if (ignoreTextEditKeys) {
                if (!e.isControlDown() && !e.isMetaDown() && !e.isAltDown())
                    return false;                   // ignore
            }
            // see if we have a local binding (InputMap on JComponent)
            //boolean retValue = processKeyBinding(ks, e, condition, pressed);
            boolean retValue = false;
            
            // otherwise, pass to our keyBindingManager
            if (!retValue)
                retValue = keyBindingManager.processKeyEvent(e);
            // *do not* pass it to menus
            
            return retValue;
        }
        
        public void setIgnoreKeyBindings(boolean b) { ignoreKeyBindings = b; }
        
        public boolean getIgnoreKeyBindings() { return ignoreKeyBindings; }
        
        public void setIgnoreTextEditKeys(boolean b) { ignoreTextEditKeys = b; }
        
        public boolean getIgnoreTextEditKeys() { return ignoreTextEditKeys; }
        
       /** Get a string description of the menu item.
         * Takes the form <p>
         * Menu | SubMenu | SubMenu | item
         * <p>
         * @param path a path to EMenuItem
         * @return a string of the description.
         */
        JMenuItem findMenuItem(int[] path) {
            int topIndex = path[0];
            if (topIndex < 0) return null;
            JMenuItem item = (JMenuItem)getComponent(topIndex);
            for (int i = 1; i < path.length; i++) {
                JMenu menu = (JMenu)item;
                item = (JMenuItem)menu.getMenuComponent(path[i]);
            }
            return item;
        }
        
        // ------------------------------ Clean Up Methods ----------------------------
        
        /**
         * Called when a TopLevel (in SDI mode) is disposed. This gets rid
         * of references to freed menu items, so that memory allocated to them
         * can be reclaimed.
         */
        public void finished() {
            // all menus
            for (int i=0; i<getMenuCount(); i++) {
                JMenu menu = getMenu(i);
                if (menu == null) continue;
                disposeofMenu(menu);
            }
            removeAll();
        }

        private void disposeofMenu(JMenu menu) {
            for (int j=0; j<menu.getItemCount(); j++) {
                JMenuItem item = menu.getItem(j);
                if (item == null) continue;
                if (item instanceof JMenu) {
                    disposeofMenu((JMenu)item);
                } else {
                    disposeofMenuItem(item);
                }
            }
            menu.removeAll();
        }

        private void disposeofMenuItem(JMenuItem item) {
            // remove all listeners (which contain references to item)
            ActionListener [] listeners = item.getActionListeners();
            for (int k = 0; k < listeners.length; k++) {
                ActionListener listener = listeners[k];
                item.removeActionListener(listener);
            }
        }
    }
}
