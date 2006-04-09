/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DebugMenuSteve.java
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

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.user.menus.MenuBar.Menu;
import com.sun.electric.tool.user.menus.MenuCommands.EMenu;
import com.sun.electric.tool.user.menus.MenuCommands.EMenuItem;
import com.sun.electric.tool.user.ui.KeyBindings;
import com.sun.electric.tool.user.ui.KeyStrokePair;
import com.sun.electric.tool.user.ui.TopLevel;

import java.util.Date;
import java.util.Iterator;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Steve's TEST MENU
 */
public class DebugMenuSteve {
    
    static EMenu makeMenu() {
        // mnemonic keys available:
        return new EMenu("_Steve",
            new EMenuItem("Dump pulldown menus") { public void run() {
                dumpPulldownMenus(); }});
    }
    
	// ---------------------- Steve's Stuff MENU -----------------
    
    /**
     * Method to dump the pulldown menus in indented style.
     */
    private static void dumpPulldownMenus()
    {
		Date now = new Date();
    	System.out.println("Pulldown menus in Electric as of " + TextUtils.formatDate(now));
        TopLevel top = (TopLevel)TopLevel.getCurrentJFrame();
    	MenuBar menuBar = top.getTheMenuBar();
        for (int i=0; i<menuBar.getMenuCount(); i++)
        {
            Menu menu = (Menu)menuBar.getMenu(i);
            printIndented("\n" + menu.getText() + ":", 0);
            addMenu(menuBar, menu, 1);
        }
    }
    private static void printIndented(String str, int depth)
    {
    	for(int i=0; i<depth*3; i++) System.out.print(" ");
    	System.out.println(str);
    	
    }
    private static void addMenu(MenuBar menuBar, Menu menu, int depth)
    {
        for (int i=0; i<menu.getItemCount(); i++)
        {
            JMenuItem menuItem = menu.getItem(i);
            if (menuItem == null) { printIndented("----------", depth); continue; }
            String s = menuItem.getText();
            if (!(menuItem instanceof JMenu)) {
                KeyBindings keyBindings = menuBar.getKeyBindings(menuItem);
                for (Iterator<KeyStrokePair> it = keyBindings.getDefaultKeyStrokePairs(); it.hasNext(); ) {
                    KeyStrokePair key = it.next();
                    s += " [" + key + "]";
                }
            }
            printIndented(s, depth);
            if (menuItem instanceof JMenu)
                addMenu(menuBar, (Menu)menuItem, depth+1);              // recurse
        }
    }
}
