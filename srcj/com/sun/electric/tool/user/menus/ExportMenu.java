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
import com.sun.electric.tool.user.dialogs.ManipulatePorts;
import com.sun.electric.tool.user.dialogs.NewExport;
import com.sun.electric.tool.user.menus.EMenu;
import com.sun.electric.tool.user.menus.EMenuItem;
import static com.sun.electric.tool.user.menus.EMenuItem.SEPARATOR;
import com.sun.electric.tool.user.ui.TopLevel;

/**
 * Class to handle the commands in the "Export" pulldown menu.
 */
public class ExportMenu {

    static EMenu makeMenu() {
        /****************************** THE EXPORT MENU ******************************/

		// mnemonic keys available:  B   F   JK     Q       Y 
        return new EMenu("E_xport",

            new EMenuItem("_Create Export...", 'E') { public void run() {
                new NewExport(TopLevel.getCurrentJFrame(), true); }},

            SEPARATOR,

            new EMenuItem("Re-Export E_verything") { public void run() {
                ExportChanges.reExportAll(); }},
            new EMenuItem("Re-Export Sele_cted") { public void run() {
                ExportChanges.reExportSelected(false); }},
            new EMenuItem("Re-Export Selected, With _Wired Ports") { public void run() {
                ExportChanges.reExportSelected(true); }},
            new EMenuItem("Re-E_xport Selected Port on All Nodes") { public void run() {
			    ExportChanges.reExportSelectedPort(); }},
            new EMenuItem("Re-Export _Power and Ground") { public void run() {
                ExportChanges.reExportPowerAndGround(); }},

            SEPARATOR,

            new EMenuItem("Re-Export _Highlighted Area") { public void run() {
                ExportChanges.reExportHighlighted(false, false); }},
            new EMenuItem("Re-Export Hi_ghlighted Area, With Wired Ports") { public void run() {
                ExportChanges.reExportHighlighted(false, true); }},
            new EMenuItem("Re-Export D_eep Highlighted Area") { public void run() {
                ExportChanges.reExportHighlighted(true, false); }},
            new EMenuItem("Re-Export Deep Highlighted Area, With W_ired Ports") { public void run() {
                ExportChanges.reExportHighlighted(true, true); }},

            SEPARATOR,

            new EMenuItem("_Delete Export") { public void run() {
                ExportChanges.deleteExport(); }},
            new EMenuItem("Delete Exports _on Selected") { public void run() {
                ExportChanges.deleteExportsOnSelected(); }},
            new EMenuItem("De_lete Exports in Highlighted Area") { public void run() {
                ExportChanges.deleteExportsInArea(); }},
            new EMenuItem("_Move Export") { public void run() {
                ExportChanges.moveExport(); }},
            new EMenuItem("_Rename Export...") { public void run() {
                ExportChanges.renameExport(); }},

            SEPARATOR,

            new EMenuItem("Summari_ze Exports") { public void run() {
                ExportChanges.describeExports(true); }},
            new EMenuItem("Lis_t Exports") { public void run() {
                ExportChanges.describeExports(false); }},
            new EMenuItem("_Show Exports") { public void run() {
                ExportChanges.showExports(); }},

            SEPARATOR,

            new EMenuItem("Show Ports on _Node") { public void run() {
                ExportChanges.showPorts(); }},
            new EMenuItem("Manip_ulate Ports on Node...") { public void run() {
                ManipulatePorts.showDialog(); }});
    }
}
