/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Menu.java
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

/*
 * Created on Sep 30, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import com.sun.electric.tool.user.KeyBindingManager;
import com.sun.electric.tool.user.KeyBindingManager.KeyBinding;


import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.AbstractButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.KeyStroke;
import javax.swing.ButtonGroup;

/**
 * @author gainsley
 *
 * IMPORTANT NOTE:
 * <p>
 * JMenu, JRadioButtonMenuItem, and JCheckBoxMenuItem all extend JMenuItem.
 * This is convenient because they can all be treated as JMenuItems.
 * <p>
 * When we extend the above classes, we get Menu, RadioButtonMenuItem,
 * CheckBoxMenuItem, and MenuItem.  However, because they extend their
 * J-counterparts, they CANNOT extend MenuItem.  Therefore we must never use
 * MenuItem when storing objects and passing around objects, but rather use 
 * JMenuItem.  Otherwise we lose the convenience of treating all the 
 * menu item types as a single type (JMenuItem in this case).
 * <p>
 * To avoid confusion I have done the following:
 * <p>
 * All extended classes are private; the API deals only with 
 * J-type JMenuItems.  The rest of the code should not know about the 
 * existence of MenuItem, CheckBoxMenuItem, or RadioButtonMenuItem.
 * Internally the code deals with the extended classes, however.  It just 
 * hides this from the rest of the code.
 * <p>
 * A usage of this custom class is in SDI mode where there are 
 * multiple menubars and menus (for each window) that must maintain 
 * consistency across the multiple instances.  This is ensured by using a 
 * HashMap to store ArrayLists of associated menuitems, and adding this class 
 * as an ActionListener. This class then takes care of updating associated 
 * menus when one menu changes to state, to ensure consistency across all
 * the menubars.
 */

public class Menu extends JMenu implements ActionListener
{
    /** Preferences for User Bindings */                            private static Preferences prefs = Preferences.userNodeForPackage(Menu.class);
    /** All menu items created, stores as ArrayLists in HashMap */  private static HashMap menuItems = new HashMap(40);
    /** global object to be used as listener */                     public static Menu updater = Menu.createMenu(null);

    /** Key Binding Manager */                                      public static KeyBindingManager keyBindingManager = new KeyBindingManager(prefs);

    /** Extend JMenuItem so we can store default KeyBinding */
    private class MenuItem extends JMenuItem
    {
        /** default Key Binding */              private KeyStroke defaultKeyStroke;

        public MenuItem(String s) { super(s); }
        public MenuItem(String text, int mnemonic) { super( text, mnemonic); }

        public void setDefaultKeyStroke(KeyStroke key) { defaultKeyStroke = key; }
        public KeyStroke getDefaultKeyStroke() { return defaultKeyStroke; }
    }
    
    /** Extend JRadioButtonMenuItem so we can store default KeyBinding */
    private class RadioButtonMenuItem extends JRadioButtonMenuItem
    {
        /** default Key Binding */              private KeyStroke defaultKeyStroke;
        
        public RadioButtonMenuItem(String text, boolean selected) { super(text, selected); }
        
        public void setDefaultKeyStroke(KeyStroke key) { defaultKeyStroke = key; }
        public KeyStroke getDefaultKeyStroke() { return defaultKeyStroke; }
    }
    
    /** Extend JCheckBoxMenuItem so we can store default KeyBinding */
    private class CheckBoxMenuItem extends JCheckBoxMenuItem
    {
        /** default Key Binding */              private KeyStroke defaultKeyStroke;
        
        public CheckBoxMenuItem(String text, boolean state) { super(text, state); }
        
        public void setDefaultKeyStroke(KeyStroke key) { defaultKeyStroke = key; }
        public KeyStroke getDefaultKeyStroke() { return defaultKeyStroke; }        
    }
        
    
	// constructor
	private Menu(String s, char mnemonic)
	{
		super(s);
		if (mnemonic != 0) setMnemonic(mnemonic);
	}

	/** Factory function to create a Menu. 
     * @param s the menu item's displayed text.
     */
	public static Menu createMenu(String s)
	{
		return new Menu(s, (char)0);
	}

	/** Factory function to create a Menu.
     * @param s the menu item's displayed text.
     * @param mnemonic the shortcut key (alt+mnemonic accesses the menu)
     */
	public static Menu createMenu(String s, char mnemonic)
	{
		return new Menu(s, mnemonic);
	}

	/** 
     * Add a JMenuItem to the JMenu.
     * @param s the menu item's displayed text.
     * @param accelerator the shortcut key, or null if none specified.
     * @param action the action to be taken when menu is activated.
     */
    public JMenuItem addMenuItem(String s, KeyStroke accelerator, ActionListener action)
    {
        JMenuItem item = new MenuItem(s);
        addItem(item, accelerator, action);
		return item;
    }

	/** 
     * Add a JCheckBoxMenuItem to the JMenu.
     * @param s the menu item's displayed text.
     * @param state the initial state of the check box
     * @param accelerator the shortcut key, or null if none specified.
     * @param action the action to be taken when menu is activated.
     */
    public JMenuItem addCheckBox(String s, boolean state, KeyStroke accelerator, ActionListener action)
    {
        JMenuItem item = new CheckBoxMenuItem(s, state);
        addItem(item, accelerator, action);
		return item;
    }        
	
	/** 
     * Add a JRadioButtonMenuItem to the JMenu.
     * @param s the menu item's displayed text.
     * @param selected the initial selected state of the check box
     * @param group the button group to belong to; only one RadioButton per group can be selected.
     * @param accelerator the shortcut key, or null if none specified.
     * @param action the action to be taken when menu is activated.
     */
    public JMenuItem addRadioButton(String s, boolean selected, ButtonGroup group, KeyStroke accelerator, ActionListener action)
    {
        JMenuItem item = new RadioButtonMenuItem(s, selected);
        if (group != null) group.add((JRadioButtonMenuItem)item);
        addItem(item, accelerator, action);
		return item;
    }
    
    //------------------------------PUBLIC UTILITY METHODS-------------------------------

    public static List getKeyBindingsFor(JMenuItem item) {
        return keyBindingManager.getBindingsFor(item.getText());
    }

    /**
     * Set new user specified key binding.  Updates all JMenuItems with same name.
     */
    public static void setUserMenuItemKeyStroke(JMenuItem menuItem, KeyStroke accelerator)
    {
        menuItem.setAccelerator(accelerator);
        // set all other same menu items
        synchronized(menuItems) {
            ArrayList list = (ArrayList)menuItems.get(menuItem.getText());
            // list == null should not happen, catch NullPointerException here if it does
            for (Iterator it = list.iterator(); it.hasNext(); ) {
                JMenuItem m = (JMenuItem)it.next();
                m.setAccelerator(accelerator);
            }
        }
        // save to prefs
        String keyBinding = "MenuKeyBinding-"+menuItem.getText();
        prefs.put(keyBinding, KeyBindingManager.keyStrokeToString(accelerator));
    }
    
    /**
     * Reset key binding to factory default.  Updates all JMenuItems with same name.
     */
    public static void resetToDefaultKeyStroke(JMenuItem menuItem)
    {
        menuItem.setAccelerator(getDefaultKeyStroke(menuItem));
        // set all other same menu items
        synchronized(menuItems) {
            ArrayList list = (ArrayList)menuItems.get(menuItem.getText());
            // list == null should not happen, catch NullPointerException here if it does
            for (Iterator it = list.iterator(); it.hasNext(); ) {
                JMenuItem m = (JMenuItem)it.next();
                m.setAccelerator(getDefaultKeyStroke(menuItem));
            }
        }
        // erase pref
        String keyBinding = "MenuKeyBinding-"+menuItem.getText();
        prefs.remove(keyBinding);
    }
    
    /**
     * Get the default key stroke of the JMenuItem 
     */
    public static KeyStroke getDefaultKeyStroke(JMenuItem item)
    {
        if (item instanceof MenuItem) return ((MenuItem)item).getDefaultKeyStroke();
        if (item instanceof CheckBoxMenuItem) return ((CheckBoxMenuItem)item).getDefaultKeyStroke();
        if (item instanceof RadioButtonMenuItem) return ((RadioButtonMenuItem)item).getDefaultKeyStroke();
        return null;
    }
    
    /**
     * Update associated menuitems in any other menubars on a state change
     */
    public void actionPerformed(ActionEvent e)
    {
        AbstractButton source = (AbstractButton)e.getSource();
        String name;
        if (source instanceof ToolBarButton) name = ((ToolBarButton)source).getName(); else
            name = source.getText();
        //System.out.println("ActionPerformed on Menu "+name+", state is "+source.isSelected());
        synchronized(menuItems) {
            ArrayList list = (ArrayList)menuItems.get(name);
            if (list == null) return;
            for (Iterator it = list.iterator(); it.hasNext(); ) {
                AbstractButton b = (AbstractButton)it.next();
                if (b == source) continue;
                String name2;
                if (source instanceof ToolBarButton) name2 = ((ToolBarButton)source).getName(); else
                    name2 = source.getText();
                //System.out.println("   - SubactionPerformed on Menu "+name2+", state set to "+source.isSelected());
                // update state on other menus to match state on activated menu
                b.setSelected(source.isSelected());
            }
        }
    }

    /**
     * Called when a TopLevel (in SDI mode) is disposed. This gets rid
     * of references to freed menu items, so that memory allocated to them
     * can be reclaimed.
     * @param menuBar the JMenuBar being disposed of.
     */
    public static void disposeOf(JMenuBar menuBar) 
    {
        // all menus
        for (int i=0; i<menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu == null) continue;
            disposeofMenu(menu);
        }
    }

    public static void disposeofMenu(JMenu menu)
    {
        for (int j=0; j<menu.getItemCount(); j++) {
            JMenuItem item = menu.getItem(j);
            if (item == null) continue;
            if (item instanceof JMenu) {
                disposeofMenu((JMenu)item);
            } else {
                disposeofMenuItem(item);
            }
        }
    }

    public static void disposeofMenuItem(JMenuItem item)
    {
        // remove all listeners (which contain references to item)
        ActionListener [] listeners = item.getActionListeners();
        for (int k = 0; k < listeners.length; k++) {
            ActionListener listener = listeners[k];
            item.removeActionListener(listener);
        }
        synchronized(menuItems) {
            ArrayList list = (ArrayList)menuItems.get(item.getText());
            if (list == null) return;
            // remove reference to item
            list.remove(item);
        }
        //System.out.println("  removing menu item "+item);
    }

    //------------------------------PRIVATE METHODS--------------------------------------
    
    /** Add JMenuItem to Menu */
	private void addItem(JMenuItem item, KeyStroke accelerator, ActionListener action)
	{
        Menu.setDefaultKeyStroke(item, accelerator);
		item.setAccelerator(accelerator);
        // see if user specified key binding exists
        /*
        String keyBinding = prefs.get("MenuKeyBinding-"+item.getText(), null);
        if (keyBinding != null) {
            KeyStroke k = KeyStroke.getKeyStroke(keyBinding);
            if (k != null) item.setAccelerator(k);
        }*/
        //if (accelerator != null)
        //    keyBindingManager.addDefaultKeyBinding(new KeyBinding(item.getText(), accelerator, null, action));

		item.addActionListener(action);
        item.addActionListener(Menu.updater);
        item.addActionListener(ToolBarButton.updater);
		this.add(item);
        // add to hash map
        synchronized(menuItems) {
            ArrayList list = (ArrayList)menuItems.get(item.getText());
            if (list == null) {
                list = new ArrayList();
                menuItems.put(item.getText(), list);
            }
            list.add(item);
        }
    }
    
    /**
     * Set the default key stroke of the JMenuItem 
     */
    private static void setDefaultKeyStroke(JMenuItem item, KeyStroke key)
    {
        if (item instanceof MenuItem) ((MenuItem)item).setDefaultKeyStroke(key);
        if (item instanceof CheckBoxMenuItem) ((CheckBoxMenuItem)item).setDefaultKeyStroke(key);
        if (item instanceof RadioButtonMenuItem) ((RadioButtonMenuItem)item).setDefaultKeyStroke(key);
    }
    
}
