/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ViewMenu.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.tool.user.ViewChanges;
import com.sun.electric.tool.user.dialogs.ViewControl;
import com.sun.electric.tool.user.menus.EMenu;
import com.sun.electric.tool.user.menus.EMenuItem;
import static com.sun.electric.tool.user.menus.EMenuItem.SEPARATOR;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.util.List;
import javax.swing.JOptionPane;

/**
 * Class to handle the commands in the "View" pulldown menu.
 */
public class ViewMenu {

    static EMenu makeMenu() {
        /****************************** THE VIEW MENU ******************************/

		// mnemonic keys available:  B DEF   J  MN PQR     X Z
        return new EMenu("_View",

            new EMenuItem("View _Control...") { public void run() {
                viewControlCommand(); }},
            new EMenuItem("Chan_ge Cell's View...") { public void run() {
                changeViewCommand(); }},

            SEPARATOR,

            new EMenuItem("Edit La_yout View") { public void run() {
                editLayoutViewCommand(); }},
            new EMenuItem("Edit Schema_tic View") { public void run() {
                editSchematicViewCommand(); }},
            new EMenuItem("Edit Ic_on View") { public void run() {
                editIconViewCommand(); }},
            new EMenuItem("Edit V_HDL View") { public void run() {
                editVHDLViewCommand(); }},
            new EMenuItem("Edit Document_ation View") { public void run() {
                editDocViewCommand(); }},
            new EMenuItem("Edit S_keleton View") { public void run() {
                editSkeletonViewCommand(); }},
            new EMenuItem("Edit Other Vie_w...") { public void run() {
                editOtherViewCommand(); }},

            SEPARATOR,

            new EMenuItem("Make _Icon View") { public void run() {
                ViewChanges.makeIconViewCommand(); }},
		    new EMenuItem("Make _Schematic View") { public void run() {
			    ViewChanges.makeSchematicView(); }},
		    new EMenuItem("Make Alternate Layo_ut View...") { public void run() {
			    ViewChanges.makeLayoutView(); }},
		    new EMenuItem("Make Ske_leton View") { public void run() {
			    ViewChanges.makeSkeletonViewCommand(); }},
		    new EMenuItem("Make _VHDL View") { public void run() {
			    ToolMenu.makeVHDL(); }});
    }

    /**
     * This method implements the command to control Views.
     */
    public static void viewControlCommand()
    {
         ViewControl dialog = new ViewControl(TopLevel.getCurrentJFrame(), true);
        dialog.setVisible(true);
    }

    public static void changeViewCommand()
    {
        Cell cell = WindowFrame.getCurrentCell();
        if (cell == null) return;

        List<View> views = View.getOrderedViews();
        String [] viewNames = new String[views.size()];
        for(int i=0; i<views.size(); i++)
            viewNames[i] = ((View)views.get(i)).getFullName();
        Object newName = JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), "New view for this cell",
            "Choose alternate view", JOptionPane.QUESTION_MESSAGE, null, viewNames, cell.getView().getFullName());
        if (newName == null) return;
        String newViewName = (String)newName;
        View newView = View.findView(newViewName);
        if (newView != null && newView != cell.getView())
        {
        	ViewChanges.changeCellView(cell, newView);
        }
    }

    public static void editLayoutViewCommand()
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;
        Cell layoutView = curCell.otherView(View.LAYOUT);
        if (layoutView == null) System.out.println("There is no layout view of " + curCell); else
            WindowFrame.createEditWindow(layoutView);
    }

    public static void editSchematicViewCommand()
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;
        final Cell schematicView = curCell.otherView(View.SCHEMATIC);
        if (schematicView == null) System.out.println("There is no schematic view of " + curCell); else
            WindowFrame.createEditWindow(schematicView);
    }

    public static void editIconViewCommand()
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;
        Cell iconView = curCell.otherView(View.ICON);
        if (iconView == null) System.out.println("There is no icon view of " + curCell); else
            WindowFrame.createEditWindow(iconView);
    }

    public static void editVHDLViewCommand()
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;
        Cell vhdlView = curCell.otherView(View.VHDL);
        if (vhdlView == null) System.out.println("There is no VHDL view of " + curCell); else
            WindowFrame.createEditWindow(vhdlView);
    }

    public static void editDocViewCommand()
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;
        Cell docView = curCell.otherView(View.DOC);
        if (docView == null) System.out.println("There is no documentation view of " + curCell); else
            WindowFrame.createEditWindow(docView);
    }

    public static void editSkeletonViewCommand()
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;
        Cell skelView = curCell.otherView(View.LAYOUTSKEL);
        if (skelView == null) System.out.println("There is no skeleton view of " + curCell); else
            WindowFrame.createEditWindow(skelView);
    }

    public static void editOtherViewCommand()
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;

        List<View> views = View.getOrderedViews();
        String [] viewNames = new String[views.size()];
        for(int i=0; i<views.size(); i++)
            viewNames[i] = ((View)views.get(i)).getFullName();
        Object newName = JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), "Which associated view do you want to see?",
            "Choose alternate view", JOptionPane.QUESTION_MESSAGE, null, viewNames, curCell.getView().getFullName());
        if (newName == null) return;
        String newViewName = (String)newName;
        View newView = View.findView(newViewName);
        Cell otherView = curCell.otherView(newView);
        if (otherView == null) System.out.println("There is no " + newViewName + " view of " + curCell); else
            WindowFrame.createEditWindow(otherView);
    }

}
