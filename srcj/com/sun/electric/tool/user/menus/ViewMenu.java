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

import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.dialogs.ViewControl;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.text.TextUtils;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;

/**
 * Class to handle the commands in the "View" pulldown menu.
 */
public class ViewMenu {

    protected static void addViewMenu(MenuBar menuBar) {
        MenuBar.MenuItem m;
		int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        /****************************** THE VIEW MENU ******************************/

        MenuBar.Menu viewMenu = new MenuBar.Menu("View", 'V');
        menuBar.add(viewMenu);

        viewMenu.addMenuItem("View Control...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { viewControlCommand(); } });
        viewMenu.addMenuItem("Change Cell's View...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { changeViewCommand(); } });

        viewMenu.addSeparator();

        viewMenu.addMenuItem("Edit Layout View", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { editLayoutViewCommand(); } });
        viewMenu.addMenuItem("Edit Schematic View", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { editSchematicViewCommand(); } });
        viewMenu.addMenuItem("Edit Multi-Page Schematic View...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { editMultiPageSchematicViewCommand(); } });
        viewMenu.addMenuItem("Edit Icon View", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { editIconViewCommand(); } });
        viewMenu.addMenuItem("Edit VHDL View", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { editVHDLViewCommand(); } });
        viewMenu.addMenuItem("Edit Documentation View", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { editDocViewCommand(); } });
        viewMenu.addMenuItem("Edit Skeleton View", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { editSkeletonViewCommand(); } });
        viewMenu.addMenuItem("Edit Other View...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { editOtherViewCommand(); } });

        viewMenu.addSeparator();

        viewMenu.addMenuItem("Make Icon View", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.makeIconViewCommand(); } });
		viewMenu.addMenuItem("Make Multi-Page Schematic View...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.makeMultiPageSchematicViewCommand(); } });
		viewMenu.addMenuItem("Make Skeleton View", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.makeSkeletonViewCommand(); } });

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
            CircuitChanges.changeCellView(cell, newView);
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
        Cell schematicView = curCell.otherView(View.SCHEMATIC);
        if (schematicView != null)
            WindowFrame.createEditWindow(schematicView);
        else
            System.out.println("No schematic view for cell "+curCell.describe());
    }

    public static void editMultiPageSchematicViewCommand()
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;
        String newSchematicPage = JOptionPane.showInputDialog("Page Number", "");
        if (newSchematicPage == null) return;
        int pageNo = TextUtils.atoi(newSchematicPage);
        if (pageNo <= 0)
        {
            System.out.println("Multi-page schematics are numbered starting at page 1");
            return;
        }
        View v = View.findMultiPageSchematicView(pageNo);
        if (v != null)
        {
            Cell otherView = curCell.otherView(v);
            if (otherView != null)
            {
                WindowFrame.createEditWindow(otherView);
                return;
            }
        }
        System.out.println("Cannot find schematic page " + pageNo + " of this cell");
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
