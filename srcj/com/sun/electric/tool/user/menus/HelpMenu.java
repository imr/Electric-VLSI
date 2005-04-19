/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: HelpMenu.java
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

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.lib.LibFile;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.dialogs.About;
import com.sun.electric.tool.user.help.ManualViewer;
import com.sun.electric.tool.user.ui.SizeListener;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.KeyStroke;

/**
 * Class to handle the commands in the "Help" pulldown menu.
 */
public class HelpMenu {

    protected static MenuBar.Menu addHelpMenu(MenuBar menuBar) {
        MenuBar.MenuItem m;
		int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        /****************************** THE HELP MENU ******************************/

		// mnemonic keys available:   CDEFGHIJKLMNOPQR T VWXYZ
        MenuBar.Menu helpMenu = MenuBar.makeMenu("_Help");
        menuBar.add(helpMenu);

        if (TopLevel.getOperatingSystem() != TopLevel.OS.MACINTOSH)
        {
            helpMenu.addMenuItem("_About Electric...", null,
                new ActionListener() { public void actionPerformed(ActionEvent e) { aboutCommand(); } });
			helpMenu.addSeparator();
        }

		helpMenu.addMenuItem("_User's Manual...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { userManualCommand(); } });
		helpMenu.addMenuItem("Load _Samples Library", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ManualViewer.loadSamplesLibrary(); } });

		// mnemonic keys available: ABCDEFGHIJKL NOPQRSTUVWXYZ
		MenuBar.Menu builtInLibSubMenu = MenuBar.makeMenu("Load _Built-in Libraries");
		helpMenu.add(builtInLibSubMenu);
		builtInLibSubMenu.addMenuItem("_MOSIS CMOS Pads", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { loadBuiltInLibraryCommand("pads4u"); } });
		return helpMenu;
    }

    // ---------------------- THE HELP MENU -----------------

	public static void aboutCommand()
    {
		About dialog = new About(TopLevel.getCurrentJFrame(), true);
        dialog.setVisible(true);
    }

	private static void userManualCommand()
	{
		ManualViewer dialog = new ManualViewer(TopLevel.getCurrentJFrame());
		dialog.setVisible(true);
	}

	private static void loadBuiltInLibraryCommand(String libName)
	{
		if (Library.findLibrary(libName) != null) return;
		URL url = LibFile.getLibFile(libName + ".jelib");
		FileMenu.ReadLibrary job = new FileMenu.ReadLibrary(url, FileType.JELIB, null);
	}

}
