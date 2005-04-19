/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ExportMenu.java
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

import com.sun.electric.tool.user.ExportChanges;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.KeyStroke;

/**
 * Class to handle the commands in the "Export" pulldown menu.
 */
public class ExportMenu {

    protected static void addExportMenu(MenuBar menuBar) {
        MenuBar.MenuItem m;
		int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        /****************************** THE EXPORT MENU ******************************/

		// mnemonic keys available:  B   F  IJK     Q   UV XY 
        MenuBar.Menu exportMenu = MenuBar.makeMenu("E_xport");
        menuBar.add(exportMenu);

        exportMenu.addMenuItem("_Create Export...", KeyStroke.getKeyStroke('E', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.newExportCommand(); } });

        exportMenu.addSeparator();

        exportMenu.addMenuItem("Re-Export _Everything", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.reExportAll(); } });
        exportMenu.addMenuItem("Re-Export Sele_cted", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.reExportSelected(false); } });
        exportMenu.addMenuItem("Re-Export Selected, With _Wired Ports", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.reExportSelected(true); } });
        exportMenu.addMenuItem("Re-Export _Highlighted Area", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.reExportHighlighted(false); } });
        exportMenu.addMenuItem("Re-Export Hi_ghlighted Area, With Wired Ports", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.reExportHighlighted(true); } });
        exportMenu.addMenuItem("Re-Export _Power and Ground", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.reExportPowerAndGround(); } });

        exportMenu.addSeparator();

        exportMenu.addMenuItem("_Delete Export", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.deleteExport(); } });
        exportMenu.addMenuItem("Delete Exports _on Selected", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.deleteExportsOnSelected(); } });
        exportMenu.addMenuItem("De_lete Exports in Highlighted Area", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.deleteExportsInArea(); } });
        exportMenu.addMenuItem("_Move Export", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.moveExport(); } });
        exportMenu.addMenuItem("_Rename Export...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.renameExport(); } });

        exportMenu.addSeparator();

        exportMenu.addMenuItem("Summari_ze Exports", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.describeExports(true); } });
        exportMenu.addMenuItem("Lis_t Exports", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.describeExports(false); } });
        exportMenu.addMenuItem("_Show Exports", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.showExports(); } });

        exportMenu.addSeparator();

        exportMenu.addMenuItem("Show Ports on _Node", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.showPorts(); } });

        exportMenu.addSeparator();

        exportMenu.addMenuItem("_Add Exports from Library...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.synchronizeLibrary(); } });
    }
}
