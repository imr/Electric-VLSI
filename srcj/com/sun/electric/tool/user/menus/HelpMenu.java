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

import static com.sun.electric.tool.user.menus.EMenuItem.SEPARATOR;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.lib.LibFile;
import com.sun.electric.tool.Client;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.dialogs.About;
import com.sun.electric.tool.user.help.ManualViewer;
import com.sun.electric.tool.user.ui.TopLevel;

import java.net.URL;

/**
 * Class to handle the commands in the "Help" pulldown menu.
 */
public class HelpMenu {

    static EMenu makeMenu() {
        /****************************** THE HELP MENU ******************************/

		// mnemonic keys available:  BCDEFGHIJ  MNOPQ ST VWXYZ
        return new EMenu("_Help",

            !Client.isOSMac() ? new EMenuItem("_About Electric...") { public void run() {
                aboutCommand(); }} : null,
			!Client.isOSMac() ? SEPARATOR : null,

		    new EMenuItem("_User's Manual...") { public void run() {
                ManualViewer.userManualCommand(); }},

            ManualViewer.hasRussianManual() ? new EMenuItem("User's Manual (_Russian)...") { public void run() {
                ManualViewer.userManualRussianCommand(); }} : null,

            new EMenuItem("Show _Key Bindings") { public void run() {
                MenuCommands.menuBar().keyBindingManager.printKeyBindings(); }},

        // mnemonic keys available:  BCDEFGHIJK MNOPQRSTUVWXYZ
            new EMenu("_3D Showcase",
                new EMenuItem("_Load Library") { public void run() {
                    ManualViewer.loadSamplesLibrary("floatingGates", "topCell"); }},
                new EMenuItem("_3D View of Cage Cell") { public void run() {
                    ManualViewer.open3DSample("floatingGates" ,"topCell", "3D ShowCase"); }},
                new EMenuItem("_Animate Cage Cell") { public void run() {
                    ManualViewer.animate3DSample("demoCage.j3d"); }}),

		// mnemonic keys available: ABCDEFGHIJKL NO QR TUVWXYZ
            new EMenu("_Load Built-in Libraries",
                new EMenuItem("_Sample Cells") { public void run() {
                    ManualViewer.loadSamplesLibrary("samples", "tech-MOSISCMOS"); }},
                new EMenuItem("_MOSIS CMOS Pads") { public void run() {
                    loadBuiltInLibraryCommand("pads4u"); }},
                new EMenuItem("MI_PS Cells") { public void run() {
                    loadBuiltInLibraryCommand("mipscells"); }})
        );
    }

    // ---------------------- THE HELP MENU -----------------

	/**
	 * Method to invoke the "About" dialog.
	 */
	public static void aboutCommand()
    {
		About dialog = new About(TopLevel.getCurrentJFrame(), true);
        dialog.setVisible(true);
    }

	private static void loadBuiltInLibraryCommand(String libName)
	{
		if (Library.findLibrary(libName) != null) return;
		URL url = LibFile.getLibFile(libName + ".jelib");
		new FileMenu.ReadLibrary(url, FileType.JELIB, null, null);
	}
    
}
