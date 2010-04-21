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
 * the Free Software Foundation; either version 3 of the License, or
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

import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.ui.LayerTab;
import com.sun.electric.tool.Job;
import com.sun.electric.technology.Technology;

import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import javax.swing.KeyStroke;
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

    private static EMenuBar menuBar = null;
    
	/**
	 * Method to create the pulldown menus.
	 */
	public static EMenuBar menuBar()
	{
        if (menuBar == null) {
            List<EMenuItem> itemsList = new ArrayList<EMenuItem>(8);
    
            itemsList.add(FileMenu.makeMenu());
            itemsList.add(EditMenu.makeMenu());
            itemsList.add(CellMenu.makeMenu());
            itemsList.add(ExportMenu.makeMenu());
            itemsList.add(ViewMenu.makeMenu());
            itemsList.add(WindowMenu.makeMenu());
            itemsList.add(ToolMenu.makeMenu());
            itemsList.add(makeExtraMenu("menus.SunAsyncMenu"));
            itemsList.add(HelpMenu.makeMenu());
            if (Job.getDebug())
                itemsList.addAll(makeTestMenus());
            menuBar = new EMenuBar("",
                wiringShortcuts(),
                    itemsList);
            menuBar.restoreSavedBindings(true);
        }
        return menuBar;
	}

    private static List<EMenuItem> makeTestMenus()
    {
        List<EMenuItem> list = new ArrayList<EMenuItem>(5);
        list.add(makeExtraMenu("tests.TestMenu"));

        // Adding developers menus. They are accessed by DevelopersMenus
        try {
            Class<?> menuClass = Class.forName("com.sun.electric.plugins.tests.DevelopersMenus");
            java.lang.reflect.Method makeMenus = menuClass.getMethod("makeMenus"); // varargs
            Object menus = makeMenus.invoke(null); // varargs
            if (menus != null)
                list.addAll((List<EMenuItem>)menus);
        } catch (Exception e) {
            if (Job.getDebug())
                System.out.println("GNU Release can't find developers menus");
        }
        return list;
    }

    static EMenu makeExtraMenu(String plugin) {
        try {
            Class<?> menuClass = Class.forName("com.sun.electric.plugins."+plugin);
            java.lang.reflect.Method makeMenu = menuClass.getMethod("makeMenu"); // varargs
            return (EMenu)makeMenu.invoke(null); // varargs
        } catch (Exception e) {
//            e.printStackTrace();
            return null;
        }
    }

        /********************************* Hidden Menus *******************************/

    private static EMenu wiringShortcuts() {
        return new EMenu("Shortcuts",
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
            new EMenuItem("Wire Down One Level", KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0)) { public void run() {
                ClickZoomWireListener.theOne.wireDownUp(true); }},
            new EMenuItem("Wire Up One Level", KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0)) { public void run() {
                ClickZoomWireListener.theOne.wireDownUp(false); }},
            new EMenuItem("Switch Wiring Target", KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0)) { public void run() {
                ClickZoomWireListener.theOne.switchWiringTarget(); }},
            EMenuItem.SEPARATOR,
//            new EMenuItem("Set M1 Visible", KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.SHIFT_MASK)) { public void run() {
//                setLayerVisible(1); }},
//            new EMenuItem("Set M2 Visible", KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.SHIFT_MASK)) { public void run() {
//                setLayerVisible(2); }},
//            new EMenuItem("Set M3 Visible", KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.SHIFT_MASK)) { public void run() {
//                setLayerVisible(3); }},
//            new EMenuItem("Set M4 Visible", KeyStroke.getKeyStroke(KeyEvent.VK_4, InputEvent.SHIFT_MASK)) { public void run() {
//                setLayerVisible(4); }},
//            new EMenuItem("Set M5 Visible", KeyStroke.getKeyStroke(KeyEvent.VK_5, InputEvent.SHIFT_MASK)) { public void run() {
//                setLayerVisible(5); }},
//            new EMenuItem("Set M6 Visible", KeyStroke.getKeyStroke(KeyEvent.VK_6, InputEvent.SHIFT_MASK)) { public void run() {
//                setLayerVisible(6); }},
//            new EMenuItem("Set M7 Visible", KeyStroke.getKeyStroke(KeyEvent.VK_7, InputEvent.SHIFT_MASK)) { public void run() {
//                setLayerVisible(7); }},
//            new EMenuItem("Set M8 Visible", KeyStroke.getKeyStroke(KeyEvent.VK_8, InputEvent.SHIFT_MASK)) { public void run() {
//                setLayerVisible(8); }},
//            new EMenuItem("Set M9 Visible", KeyStroke.getKeyStroke(KeyEvent.VK_9, InputEvent.SHIFT_MASK)) { public void run() {
//                setLayerVisible(9); }},
//            new EMenuItem("Set All Visible", KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.SHIFT_MASK)) { public void run() {
//                setLayerVisible(0); }},
//            EMenuItem.SEPARATOR,
            WindowMenu.getHiddenWindowCycleMenuItem());
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
