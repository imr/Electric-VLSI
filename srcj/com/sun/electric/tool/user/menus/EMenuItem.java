/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EMenuItem.java
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
import com.sun.electric.tool.user.MessagesStream;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

/**
 * Generic Electric menu item.
 * It generates real menu buttons and tool buttons with shared state.
 */
public abstract class EMenuItem implements ActionListener {

    public static final EMenuItem[] NULL_ARRAY = {};
    private static final int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    /**
     * A constant to represent separator in menus.
     */
    public static final EMenuItem SEPARATOR = new EMenuItem("---------------") {
        @Override void setParent(EMenuBar menuBar, EMenu parent) {}
        @Override public JMenuItem genMenu() { throw new UnsupportedOperationException(); }
        @Override public void run() { throw new UnsupportedOperationException(); }
    };

    /**
     * Last activate command.
     */
    private static EMenuItem lastActivated = null;

    /**
     * A text description of this EMenuItem.
     * This text is used as a key when key bindings are saved in preferences.
     * This text also apperas on menu buttons and as a tool tip text on tool buttons.
     */ 
    private final String text;
    /**
     * Mnemonic position in text description of this EMenuItem.
     * The chanracter at this position is underlined, an Alt shortcut key is created.
     */
    private final int mnemonicsPos;
    /**
     * Accelerator for this EMenuItem.
     */
    private KeyStroke accelerator;
    /**
     * Additional accelerator for this EMenuItem.
     */
    private KeyStroke accelerator2;
    /**
     * Parent EMenu for this EMenuItem.
     */
    EMenu parent;
    
    /**
     * @param text the menu item's displayed text.  An "_" in the string
     * indicates the location of the "mnemonic" key for that entry.
     * @param accelerator the shortcut key, or null if none specified.
     * @param accelerator2 the second shortcut key, or null if none specified.
     */
    EMenuItem(String text, KeyStroke accelerator, KeyStroke accelerator2) {
        mnemonicsPos = text.indexOf('_');
        this.text = mnemonicsPos >= 0 ? text.substring(0, mnemonicsPos) + text.substring(mnemonicsPos+1) : text;
        this.accelerator = accelerator;
        this.accelerator2 = accelerator2;
    }
    
    /**
     * @param text the menu item's displayed text.  An "_" in the string
     * indicates the location of the "mnemonic" key for that entry.
     * @param accelerator the shortcut key, or null if none specified.
     */
    public EMenuItem(String text, KeyStroke accelerator) {
        this(text, accelerator, null);
    }
    
    /**
     * @param text the menu item's displayed text.  An "_" in the string
     * indicates the location of the "mnemonic" key for that entry.
     */
    public EMenuItem(String text) {
        this(text, null);
    }
    
    /**
     * @param text the menu item's displayed text.  An "_" in the string
     * indicates the location of the "mnemonic" key for that entry.
     * @param acceleratorChar the shortcut char.
     */
    public EMenuItem(String text, char acceleratorChar) {
        this(text, shortcut(acceleratorChar));
    }
    
    /**
     * @param text the menu item's displayed text.  An "_" in the string
     * indicates the location of the "mnemonic" key for that entry.
     * @param acceleratorChar the shortcut char.
     * @param keyCode the second shortcut key.
     */
    EMenuItem(String text, char acceleratorChar, int keyCode) {
        this(text, shortcut(acceleratorChar), shortcut(keyCode));
    }

    /**
     * Determines which modifier key is the appropriate accelerator
     * key for menu shortcuts.
     * @param keyCode key code without modifier.
     * @return key stroke whih approptiate modified
     */
    public static KeyStroke shortcut(int keyCode) {
        return KeyStroke.getKeyStroke(keyCode, buckyBit);
    } 
    
	/**
     * Repeat the last Command
     */
    public static void repeatLastCommand() {
        if (lastActivated != null)
        	lastActivated.run();
    }

    /**
     * Returns hort text description of this EMenuElement.
     */
    public String getText() { return text; }
    
    KeyStroke getAccelerator() { return accelerator; }
    KeyStroke getAccelerator2() { return accelerator2; }
    void setAccelerator(KeyStroke accelerator) { this.accelerator = accelerator; }
    
    
    public String toString() { return text; }
    
    /** Get a string description of the menu item.
     * Takes the form <p>
     * Menu | SubMenu | SubMenu | item
     * <p>
     * @return a string of the description.
     */
    public String getDescription() {
        String s = getText();
        for (EMenu p = parent; p != null && !(p instanceof EMenuBar); p = p.parent)
            s = p.getText() + " | " + s;
        return s;
    }
    
    /**
     * Register menu item tree in EMenuBar.
     * @param menuBar EMenuBar where to register.
     * @param parent parent EMenu of 
     */
    void setParent(EMenuBar menuBar, EMenu parent) {
        this.parent = parent;
        menuBar.registerItem(this);
    }

    /**
     * Generates menu item by this this generic EMenuItem.
     * @return generated instance.
     */
    JMenuItem genMenu() {
        JMenuItem item = createMenuItem();
        initMenuItem(item);
        updateMenuItem(item);
        return item;
    }
    
    /**
     * Init create instance of menu item.
     */
    void initMenuItem(JMenuItem item) {
        item.setText(text);
        if (mnemonicsPos >= 0) {
            item.setMnemonic(text.charAt(mnemonicsPos));
            item.setDisplayedMnemonicIndex(mnemonicsPos);
        }
        // add action listeners so when user selects menu, actions will occur
        item.addActionListener(this);
    }
    
    /**
     * Creates fresh GUI instance of this generic EMenuItem.
     * Override in subclasses.
     * @return GUI instance
     */
    protected JMenuItem createMenuItem() {
        return new JMenuItem();
    }
    
    /**
     * Updates appearance of menu item instance before popping up.
     * @param item item to update.
     */
    protected void updateMenuItem(JMenuItem item) {
        item.setAccelerator(accelerator);
        item.setEnabled(isEnabled());
        item.setSelected(isSelected());
    }
    
    /**
     * Returns enable state of this generic EMenuItem.
     * Override in subclasses.
     * @return true is this generic EMenuItem is enabled.
     */
    public boolean isEnabled() { return true; }
    
    /**
     * Returns selection state of this generic EMenuItem.
     * Override in subclasses.
     * @return true is this generic EMenuItem is selected.
     */
    public boolean isSelected() { return false; }
    
    /**
     * Invoked when an action occurs.
     * It can be envoked form menu button, tool bar button or shortcut key.
     */
    public void actionPerformed(ActionEvent e) {
        MessagesStream.userCommandIssued();
        ActivityLogger.logMenuActivated(getDescription());
        if (!text.startsWith("Repeat Last"))
            lastActivated = this;
        run();
        updateButtons();
    }

    /**
     * Abstract method which executes command.
     */
    public abstract void run();
    
    /**
     * Updates GUI buttons after change of state of generic button.
     * Override in subclasses.
     */
    protected void updateButtons() {} 

    /**
     * A subclass of EMenuItem to represent toggle button.
     */
    public abstract static class CheckBox extends EMenuItem {
        public CheckBox(String text) { super(text); }
        @Override public void run() { setSelected(!isSelected()); }
        public abstract void setSelected(boolean b);
        @Override protected JMenuItem createMenuItem() { return new JCheckBoxMenuItem(); }
    }
}
