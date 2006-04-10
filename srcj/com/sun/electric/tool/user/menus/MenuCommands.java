/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MenuCommands.java
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

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.user.menus.MenuBar.MenuItem;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.Job;
import java.awt.Toolkit;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.ArrayList;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;


/**
 * This class has all of the pulldown menu commands in Electric.
 * <p>
 * For SDI mode Swing requires that each window have it's own menu.
 * This means for consistency across windows that a change of state on
 * a menu item in one window's menu must occur in all other window's
 * menus as well (such as checking a check box).
 */
public final class MenuCommands
{
    public static final int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        
    public abstract static class EMenuElement {
        final String label;
        EMenuElement(String label) { this.label = label; }
        abstract JMenuItem genMenu(MenuBar menuBar, MenuBar.Menu menu);
    }
    
    public static class EMenu extends EMenuElement {
        private List<EMenuElement> elems = new ArrayList<EMenuElement>();
        
        EMenu(String text) {
            super(text);
        }
        
        public EMenu(String text, EMenuElement... elems) {
            super(text);
            for (EMenuElement item: elems)
                addMenuElem(item);
        }
        
        public void addMenuElems(EMenuElement... elems) {
            for (EMenuElement item: elems)
                addMenuElem(item);
        }
        
        public void addMenuElem(EMenuElement elem) {
            if (elem != null)
                elems.add(elem);
        }

        public void add(EMenu subMenu) { addMenuElem(subMenu); }
        
        public void addSeparator() { addMenuElem(SEPARATOR); }
        
        public JMenuItem genMenu(MenuBar menuBar, MenuBar.Menu menu) {
            MenuBar.Menu subMenu = MenuBar.makeMenu(label);
            menu.add(subMenu);
            genMenuElems(menuBar, subMenu);
            return subMenu;
        }
            
        public JMenuItem genMenu(MenuBar menuBar) {
            MenuBar.Menu subMenu = MenuBar.makeMenu(label);
            menuBar.add(subMenu);
            genMenuElems(menuBar, subMenu);
            return subMenu;
        }
        
        public JMenuItem genMenuHidden(MenuBar menuBar) {
            MenuBar.Menu subMenu = MenuBar.makeMenu(label);
            menuBar.addHidden(subMenu);
            genMenuElems(menuBar, subMenu);
            return subMenu;
        }
        
        private void genMenuElems(MenuBar menuBar, MenuBar.Menu menu) {
            ButtonGroup group = null;
            for (EMenuElement elem: elems) {
                if (elem instanceof ERadioButton) {
                    ERadioButton rb = (ERadioButton)elem;
                    if (group == null)
                        group = new ButtonGroup();
                    rb.genMenu(menuBar, menu, group);
                } else {
                    elem.genMenu(menuBar, menu);
                }
            }
        }
    }
    
    public abstract static class EMenuItem extends EMenuElement implements ActionListener {
        final KeyStroke accelerator;
        final KeyStroke accelerator2;
        
        public void actionPerformed(ActionEvent e) { run(); }
    
        public abstract void run();
        
        /**
         * @param s the menu item's displayed text.  An "_" in the string
         * indicates the location of the "mnemonic" key for that entry.
         */
        public EMenuItem(String label) {
            this(label, null);
        }
        
        /**
         * @param s the menu item's displayed text.  An "_" in the string
         * indicates the location of the "mnemonic" key for that entry.
         * @param acceleratorChar the shortcut char.
         */
        EMenuItem(String label, char acceleratorChar) {
            this(label, KeyStroke.getKeyStroke(acceleratorChar, buckyBit));
        }
        
        /**
         * @param s the menu item's displayed text.  An "_" in the string
         * indicates the location of the "mnemonic" key for that entry.
         * @param acceleratorChar the shortcut char.
         * @param key the second shortcut key.
         */
        EMenuItem(String label, char acceleratorChar, int keyCode) {
            this(label, KeyStroke.getKeyStroke(acceleratorChar, buckyBit), KeyStroke.getKeyStroke(keyCode, buckyBit));
        }
        
//        /**
//         * @param s the menu item's displayed text.  An "_" in the string
//         * indicates the location of the "mnemonic" key for that entry.
//         * @param keyCode an int specifying the numeric code for a keyboard key
//         * @param modifiers a bitwise-ored combination of any modifiers
//         */
//        EMenuItem(String label, int keyCode, int modifiers) {
//            this(label, KeyStroke.getKeyStroke(keyCode, modifiers));
//        }
        
        /**
         * @param s the menu item's displayed text.  An "_" in the string
         * indicates the location of the "mnemonic" key for that entry.
         * @param accelerator the shortcut key, or null if none specified.
         */
        EMenuItem(String label, KeyStroke accelerator) {
            this(label, accelerator, null);
        }
        
        /**
         * @param s the menu item's displayed text.  An "_" in the string
         * indicates the location of the "mnemonic" key for that entry.
         * @param accelerator the shortcut key, or null if none specified.
         */
        EMenuItem(String label, KeyStroke accelerator, KeyStroke accelerator2) {
            super(label);
            this.accelerator = accelerator;
            this.accelerator2 = accelerator2;
        }
        
        JMenuItem genMenu(MenuBar menuBar, MenuBar.Menu menu) { 
            JMenuItem menuItem = menu.addMenuItem(label, accelerator, this);
            if (accelerator2 != null)
                menuBar.addDefaultKeyBinding(menuItem, accelerator2, null);
            return menuItem;
        }
        
        public String toString() { return label; }
    }

    public abstract static class ERadioButton extends EMenuItem {
        ERadioButton(String label) { super(label); }
        ERadioButton(String label, KeyStroke accelerator) { super(label, accelerator); }
        public abstract boolean isSelected();
        public JMenuItem genMenu(MenuBar menuBar, MenuBar.Menu menu, ButtonGroup group) {
            return menu.addRadioButton(label, isSelected(), group, accelerator, this);
        } 
    }
    
    public abstract static class ECheckBoxButton extends EMenuItem {
        ECheckBoxButton(String label) { super(label); }
        public abstract boolean isSelected();
        public abstract void setSelected(boolean b);
        public void actionPerformed(ActionEvent e) { setSelected(((AbstractButton)e.getSource()).isSelected()); }
        public void run() { throw new UnsupportedOperationException(); }
        public JMenuItem genMenu(MenuBar menuBar, MenuBar.Menu menu) { return menu.addCheckBox(label, isSelected(), accelerator, this); } 
    }
    
    public static final EMenuElement SEPARATOR = new EMenuElement("-") {
        public JMenuItem genMenu(MenuBar menuBar, MenuBar.Menu menu) { menu.addSeparator(); return null; }
    };

    // It is never useful for anyone to create an instance of this class
	private MenuCommands() {}

    /**
     * Class to enable/disable menus based on a property change
     */
    public static class MenuEnabler implements PropertyChangeListener {
        private MenuItem item;
        private String property;
        protected MenuEnabler(MenuItem item, String prop) {
            this.item = item;
            this.property = prop;
        }
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getNewValue() instanceof Boolean) {
                if (evt.getPropertyName().equals(property)) {
                    boolean enabled = ((Boolean)evt.getNewValue()).booleanValue();
                    item.setEnabled(enabled);
                }
            }
        }
    }

    static EMenu makeExtraMenu(String plugin) {
        try {
            Class menuClass = Class.forName("com.sun.electric.plugins."+plugin);
            java.lang.reflect.Method makeMenu = menuClass.getMethod("makeMenu", new Class[] {});
            return (EMenu)makeMenu.invoke(null, new Object [] {});
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

	/**
	 * Method to create the pulldown menus.
	 */
	public static MenuBar createMenuBar()
	{
		// create the menu bar
		MenuBar menuBar = new MenuBar();

        EMenu[] menus = {
            FileMenu.makeMenu(),
            EditMenu.makeMenu(),
            CellMenu.makeMenu(),
            ExportMenu.makeMenu(),
            ViewMenu.makeMenu(),
            WindowMenu.makeMenu(),
            ToolMenu.makeMenu(),
            makeExtraMenu("menus.SunAsyncMenu"),
            HelpMenu.makeMenu(),
            Job.getDebug() ? makeExtraMenu("tests.TestMenu") : null,
            Job.getDebug() ? DebugMenuSteve.makeMenu() : null,
            Job.getDebug() ? DebugMenuRussell.makeMenu() : null,
            Job.getDebug() ? DebugMenuJonG.makeMenu() : null,
            Job.getDebug() ? DebugMenuGilda.makeMenu() : null,
            Job.getDebug() ? DebugMenuDima.makeMenu() : null
        };
        
        for (EMenu menu: menus) {
            if (menu != null)
                menu.genMenu(menuBar);
        }

        /********************************* Hidden Menus *******************************/

        EMenu wiringShortcuts = new EMenu("Circuit Editing",
            new EMenuItem("Wire to Poly", KeyStroke.getKeyStroke(KeyEvent.VK_0, 0)) { public void run() {
                ClickZoomWireListener.theOne.wireTo(0); }},
            new EMenuItem("Wire to M1", KeyStroke.getKeyStroke(KeyEvent.VK_1, 0)) { public void run() {
                ClickZoomWireListener.theOne.wireTo(1); }},
            new EMenuItem("Wire to M2", KeyStroke.getKeyStroke(KeyEvent.VK_2, 0)) { public void run() {
                ClickZoomWireListener.theOne.wireTo(2); }},
            new EMenuItem("Wire to M3", KeyStroke.getKeyStroke(KeyEvent.VK_3, 0)) { public void run() {
                ClickZoomWireListener.theOne.wireTo(3); }},
            new EMenuItem("Wire to M4", KeyStroke.getKeyStroke(KeyEvent.VK_4, 0)) { public void run() {
                ClickZoomWireListener.theOne.wireTo(4); }},
            new EMenuItem("Wire to M5", KeyStroke.getKeyStroke(KeyEvent.VK_5, 0)) { public void run() {
                ClickZoomWireListener.theOne.wireTo(5); }},
            new EMenuItem("Wire to M6", KeyStroke.getKeyStroke(KeyEvent.VK_6, 0)) { public void run() {
                ClickZoomWireListener.theOne.wireTo(6); }},
            new EMenuItem("Wire to M7", KeyStroke.getKeyStroke(KeyEvent.VK_7, 0)) { public void run() {
                ClickZoomWireListener.theOne.wireTo(7); }},
            new EMenuItem("Wire to M8", KeyStroke.getKeyStroke(KeyEvent.VK_8, 0)) { public void run() {
                ClickZoomWireListener.theOne.wireTo(8); }},
            new EMenuItem("Wire to M9", KeyStroke.getKeyStroke(KeyEvent.VK_9, 0)) { public void run() {
                ClickZoomWireListener.theOne.wireTo(9); }},
            new EMenuItem("Switch Wiring Target", KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0)) { public void run() {
                ClickZoomWireListener.theOne.switchWiringTarget(); }});
        wiringShortcuts.genMenuHidden(menuBar);

		// return the menu bar
        //menuBar.deleteEmptyBindings();
        menuBar.restoreSavedBindings(true);
		return menuBar;
	}

    /**
     * Get list of ElectricObjects of what's currently selected by the user,
     * in the window that has focus.
     * @param wantNodes true if the list should include nodes
     * @param wantArcs true if the list should include arcs
     * @return a list of selected objects
     */
    public static List<Geometric> getSelectedObjects(boolean wantNodes, boolean wantArcs) {
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return new ArrayList<Geometric>();
        Highlighter highlighter = wf.getContent().getHighlighter();
        if (highlighter == null) return new ArrayList<Geometric>();

        return highlighter.getHighlightedEObjs(wantNodes, wantArcs);
    }

    /**
     * Get list of NodeInsts of what's currently selected by the user,
     * in the window that has focus.
     * @return a list of selected NodeInsts
     */
    public static List<NodeInst> getSelectedNodes() {
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return new ArrayList<NodeInst>();
        Highlighter highlighter = wf.getContent().getHighlighter();
        if (highlighter == null) return new ArrayList<NodeInst>();

        return highlighter.getHighlightedNodes();
    }

    /**
     * Get list of ArcInsts of what's currently selected by the user,
     * in the window that has focus.
     * @return a list of selected ArcInsts
     */
    public static List<ArcInst> getSelectedArcs() {
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return new ArrayList<ArcInst>();
        Highlighter highlighter = wf.getContent().getHighlighter();
        if (highlighter == null) return new ArrayList<ArcInst>();

        return highlighter.getHighlightedArcs();
    }

}
