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
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

/**
 * Class to handle the commands in the "View" pulldown menu.
 */
public class ViewMenu {

    protected static void addViewMenu(MenuBar menuBar) {
//        MenuBar.MenuItem m;
//		int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        /****************************** THE VIEW MENU ******************************/

		// mnemonic keys available:  B DEF   J  MN PQR     X Z
        MenuBar.Menu viewMenu = MenuBar.makeMenu("_View");
        menuBar.add(viewMenu);

        viewMenu.addMenuItem("View _Control...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { viewControlCommand(); } });
        viewMenu.addMenuItem("Chan_ge Cell's View...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { changeViewCommand(); } });

        viewMenu.addSeparator();

        viewMenu.addMenuItem("Edit La_yout View", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { editLayoutViewCommand(); } });
        viewMenu.addMenuItem("Edit Schema_tic View", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { editSchematicViewCommand(); } });
        viewMenu.addMenuItem("Edit Ic_on View", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { editIconViewCommand(); } });
        viewMenu.addMenuItem("Edit V_HDL View", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { editVHDLViewCommand(); } });
        viewMenu.addMenuItem("Edit Document_ation View", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { editDocViewCommand(); } });
        viewMenu.addMenuItem("Edit S_keleton View", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { editSkeletonViewCommand(); } });
        viewMenu.addMenuItem("Edit Other Vie_w...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { editOtherViewCommand(); } });

        viewMenu.addSeparator();

        viewMenu.addMenuItem("Make _Icon View", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ViewChanges.makeIconViewCommand(); } });
		viewMenu.addMenuItem("Make _Schematic View", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ViewChanges.makeSchematicView(); } });
		viewMenu.addMenuItem("Make Alternate Layo_ut View...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ViewChanges.makeLayoutView(); } });
		viewMenu.addMenuItem("Make Ske_leton View", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ViewChanges.makeSkeletonViewCommand(); } });
		viewMenu.addMenuItem("Make _VHDL View", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolMenu.makeVHDL();}});
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

        java.util.List views = View.getOrderedViews();
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
        if (layoutView != null)
            WindowFrame.createEditWindow(layoutView);
    }

    public static void editSchematicViewCommand()
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;
        final Cell schematicView = curCell.otherView(View.SCHEMATIC);
        if (schematicView != null)
            WindowFrame.createEditWindow(schematicView);
        else
            System.out.println("No schematic view for "+curCell);
    }

    public static void editIconViewCommand()
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;
        Cell iconView = curCell.otherView(View.ICON);
        if (iconView != null)
            WindowFrame.createEditWindow(iconView);
    }

    public static void editVHDLViewCommand()
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;
        Cell vhdlView = curCell.otherView(View.VHDL);
        if (vhdlView != null)
            WindowFrame.createEditWindow(vhdlView);
    }

    public static void editDocViewCommand()
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;
        Cell docView = curCell.otherView(View.DOC);
        if (docView != null)
            WindowFrame.createEditWindow(docView);
    }

    public static void editSkeletonViewCommand()
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;
        Cell skelView = curCell.otherView(View.LAYOUTSKEL);
        if (skelView != null)
            WindowFrame.createEditWindow(skelView);
    }

    public static void editOtherViewCommand()
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;

        java.util.List views = View.getOrderedViews();
        String [] viewNames = new String[views.size()];
        for(int i=0; i<views.size(); i++)
            viewNames[i] = ((View)views.get(i)).getFullName();
        Object newName = JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), "Which associated view do you want to see?",
            "Choose alternate view", JOptionPane.QUESTION_MESSAGE, null, viewNames, curCell.getView().getFullName());
        if (newName == null) return;
        String newViewName = (String)newName;
        View newView = View.findView(newViewName);
        Cell otherView = curCell.otherView(newView);
        if (otherView != null)
            WindowFrame.createEditWindow(otherView);
    }

}
