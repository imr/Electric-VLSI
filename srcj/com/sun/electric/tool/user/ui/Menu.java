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

import com.sun.electric.tool.user.UserMenuCommands;
import com.sun.electric.tool.user.dialogs.EditKeyBindings;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.prefs.Preferences;
import javax.swing.JMenu;
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
 */

public class Menu extends JMenu 
{
    /** Preferences for User Bindings */                            private static Preferences prefs = Preferences.userNodeForPackage(Menu.class);
    /** All menu items created, stores as ArrayLists in HashMap */  private static HashMap menuItems = new HashMap(40);
    
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
    
    /**
     * Set new user specified key binding.  Updates all JMenuItems with same name.
     */
    public static void setUserMenuItemKeyStroke(JMenuItem menuItem, KeyStroke accelerator)
    {
        menuItem.setAccelerator(accelerator);
        // set all other same menu items
        ArrayList list = (ArrayList)menuItems.get(menuItem.getText());
        // list == null should not happen, catch NullPointerException here if it does
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            JMenuItem m = (JMenuItem)it.next();
            m.setAccelerator(accelerator);
        }
        // save to prefs
        String keyBinding = "MenuKeyBinding-"+menuItem.getText();
        prefs.put(keyBinding, EditKeyBindings.keyStrokeToString(accelerator));
    }
    
    /**
     * Reset key binding to factory default.  Updates all JMenuItems with same name.
     */
    public static void resetToDefaultKeyStroke(JMenuItem menuItem)
    {
        menuItem.setAccelerator(getDefaultKeyStroke(menuItem));
        // set all other same menu items
        ArrayList list = (ArrayList)menuItems.get(menuItem.getText());
        // list == null should not happen, catch NullPointerException here if it does
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            JMenuItem m = (JMenuItem)it.next();
            m.setAccelerator(getDefaultKeyStroke(menuItem));
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

    //------------------------------PRIVATE METHODS--------------------------------------
    
    /** Add JMenuItem to Menu */
	private void addItem(JMenuItem item, KeyStroke accelerator, ActionListener action)
	{
        Menu.setDefaultKeyStroke(item, accelerator);
		item.setAccelerator(accelerator);
        // see if user specified key binding exists
        String keyBinding = prefs.get("MenuKeyBinding-"+item.getText(), null);
        if (keyBinding != null) {
            KeyStroke k = KeyStroke.getKeyStroke(keyBinding);
            if (k != null) item.setAccelerator(k);
        }
		item.addActionListener(action);
		this.add(item);
        // add to hash map
        ArrayList list = (ArrayList)menuItems.get(item.getText());
        if (list == null) {
            list = new ArrayList();
            menuItems.put(item.getText(), list);
        }
        list.add(item);
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
