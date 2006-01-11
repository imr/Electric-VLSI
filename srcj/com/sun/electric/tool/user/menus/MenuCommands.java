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

import com.sun.electric.Main;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.user.menus.MenuBar.Menu;
import com.sun.electric.tool.user.menus.MenuBar.MenuItem;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.Highlight2;
import com.sun.electric.tool.Job;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.ArrayList;


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

        if (true) {            // under test
            // add the SunAsyncMenu if there
            try {
                Class SunAsyncMenuClass = Class.forName("com.sun.electric.plugins.menus.SunAsyncMenu");
                java.lang.reflect.Method addMenu = SunAsyncMenuClass.getMethod("addMenu", new Class[] { MenuBar.class });
                addMenu.invoke(null, new Object [] {menuBar});
            } catch (Exception e) {}
        }

        MenuBar.Menu helpMenu = HelpMenu.addHelpMenu(menuBar);

		if (Job.getDebug())
	        DebugMenus.addDebugMenus(menuBar, helpMenu);

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
        wiringShortcuts.addMenuItem("Wire to M7", KeyStroke.getKeyStroke(KeyEvent.VK_7, 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.wireTo(7); }});
        wiringShortcuts.addMenuItem("Wire to M8", KeyStroke.getKeyStroke(KeyEvent.VK_8, 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.wireTo(8); }});
        wiringShortcuts.addMenuItem("Wire to M9", KeyStroke.getKeyStroke(KeyEvent.VK_9, 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.wireTo(9); }});
        wiringShortcuts.addMenuItem("Switch Wiring Target", KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.switchWiringTarget(); }});

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
