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

import com.sun.electric.tool.user.menus.MenuBar.Menu;
import com.sun.electric.tool.user.menus.MenuBar.MenuItem;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

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

    // It is never useful for anyone to create an instance of this class
	private MenuCommands() {}

    /** Used to enable/disable menus based on a property change */
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

	/**
	 * Method to create the pulldown menus.
	 */
	public static MenuBar createMenuBar()
	{
		// create the menu bar
		MenuBar menuBar = new MenuBar();

        FileMenu.addFileMenu(menuBar);
        EditMenu.addEditMenu(menuBar);
        CellMenu.addCellMenu(menuBar);
        ExportMenu.addExportMenu(menuBar);
        ViewMenu.addViewMenu(menuBar);
        WindowMenu.addWindowMenu(menuBar);
        ToolMenu.addToolMenu(menuBar);
        HelpMenu.addHelpMenu(menuBar);
        DebugMenus.addDebugMenus(menuBar);

        /********************************* Hidden Menus *******************************/

        Menu wiringShortcuts = new Menu("Circuit Editing");
        menuBar.addHidden(wiringShortcuts);
        wiringShortcuts.addMenuItem("Wire to Poly", KeyStroke.getKeyStroke(KeyEvent.VK_0, 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.wireTo(0); }});
        wiringShortcuts.addMenuItem("Wire to M1", KeyStroke.getKeyStroke(KeyEvent.VK_1, 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.wireTo(1); }});
        wiringShortcuts.addMenuItem("Wire to M2", KeyStroke.getKeyStroke(KeyEvent.VK_2, 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.wireTo(2); }});
        wiringShortcuts.addMenuItem("Wire to M3", KeyStroke.getKeyStroke(KeyEvent.VK_3, 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.wireTo(3); }});
        wiringShortcuts.addMenuItem("Wire to M4", KeyStroke.getKeyStroke(KeyEvent.VK_4, 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.wireTo(4); }});
        wiringShortcuts.addMenuItem("Wire to M5", KeyStroke.getKeyStroke(KeyEvent.VK_5, 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.wireTo(5); }});
        wiringShortcuts.addMenuItem("Wire to M6", KeyStroke.getKeyStroke(KeyEvent.VK_6, 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.wireTo(6); }});
        wiringShortcuts.addMenuItem("Switch Wiring Target", KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.switchWiringTarget(); }});

		// return the menu bar
        menuBar.deleteEmptyBindings();
		return menuBar;
	}
}
