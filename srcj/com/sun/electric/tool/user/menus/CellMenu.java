/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellMenu.java
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

import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.dialogs.*;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Class to handle the commands in the "Cell" pulldown menu.
 */
public class CellMenu {

    protected static void addCellMenu(MenuBar menuBar) {
        MenuBar.MenuItem m;
		int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        /****************************** THE CELL MENU ******************************/

        MenuBar.Menu cellMenu = new MenuBar.Menu("Cell", 'C');
        menuBar.add(cellMenu);

        cellMenu.addMenuItem("New Cell...", KeyStroke.getKeyStroke('N', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { newCellCommand(); } });
        cellMenu.addMenuItem("Edit Cell...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { cellBrowserCommand(CellBrowser.DoAction.editCell); }});
        cellMenu.addMenuItem("Place Cell Instance...", KeyStroke.getKeyStroke('N', 0),
            new ActionListener() { public void actionPerformed(ActionEvent e) { cellBrowserCommand(CellBrowser.DoAction.newInstance); }});
        cellMenu.addMenuItem("Rename Cell...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { cellBrowserCommand(CellBrowser.DoAction.renameCell); }});
        cellMenu.addMenuItem("Duplicate Cell...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { cellBrowserCommand(CellBrowser.DoAction.duplicateCell); }});
        cellMenu.addMenuItem("Delete Cell...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { cellBrowserCommand(CellBrowser.DoAction.deleteCell); }});
        MenuBar.Menu multiPageSubMenu = new MenuBar.Menu("Multi-Page Cells");
        cellMenu.add(multiPageSubMenu);
        multiPageSubMenu.addMenuItem("Make Cell Multi-Page", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { makeMultiPageCell(); }});
        multiPageSubMenu.addMenuItem("Create New Page", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { createNewMultiPage(); }});
        multiPageSubMenu.addMenuItem("Edit Next Page", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { editNextMultiPage(); }});

        cellMenu.addSeparator();

         cellMenu.addMenuItem("Cross-Library Copy...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { crossLibraryCopyCommand(); } });

        cellMenu.addSeparator();

        cellMenu.addMenuItem("Down Hierarchy", KeyStroke.getKeyStroke('D', buckyBit),
                new ActionListener() { public void actionPerformed(ActionEvent e) { downHierCommand(); }});
        cellMenu.addMenuItem("Down Hierarchy In Place", KeyStroke.getKeyStroke('D', 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { downHierInPlaceCommand(); }});
        cellMenu.addMenuItem("Up Hierarchy", KeyStroke.getKeyStroke('U', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { upHierCommand(); }});

        cellMenu.addSeparator();

        cellMenu.addMenuItem("New Version of Current Cell", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { newCellVersionCommand(); } });
        cellMenu.addMenuItem("Duplicate Current Cell", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { duplicateCellCommand(); } });
        cellMenu.addMenuItem("Delete Unused Old Versions", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { deleteOldCellVersionsCommand(); } });

        cellMenu.addSeparator();

        MenuBar.Menu cellInfoSubMenu = new MenuBar.Menu("Cell Info");
        cellMenu.add(cellInfoSubMenu);
        cellInfoSubMenu.addMenuItem("Describe this Cell", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CellLists.describeThisCellCommand(); } });
        cellInfoSubMenu.addMenuItem("General Cell Lists...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CellLists.generalCellListsCommand(); } });
        cellInfoSubMenu.addSeparator();
        cellInfoSubMenu.addMenuItem("List Nodes in this Cell", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CellLists.listNodesInCellCommand(); }});
        cellInfoSubMenu.addMenuItem("List Cell Instances", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CellLists.listCellInstancesCommand(); }});
        cellInfoSubMenu.addMenuItem("List Cell Usage", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CellLists.listCellUsageCommand(); }});
        cellInfoSubMenu.addSeparator();
        cellInfoSubMenu.addMenuItem("Graphically, Entire Library", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.graphCellsInLibrary(); }});
        cellInfoSubMenu.addMenuItem("Graphically, From Current Cell", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.graphCellsFromCell(); }});

        cellMenu.addMenuItem("Cell Properties...", null,
             new ActionListener() { public void actionPerformed(ActionEvent e) { cellControlCommand(); }});

        cellMenu.addSeparator();

        MenuBar.Menu expandListSubMenu = new MenuBar.Menu("Expand Cell Instances");
        cellMenu.add(expandListSubMenu);
        expandListSubMenu.addMenuItem("One Level Down", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.expandOneLevelDownCommand(); }});
        expandListSubMenu.addMenuItem("All the Way", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.expandFullCommand(); }});
        expandListSubMenu.addMenuItem("Specified Amount...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.expandSpecificCommand(); }});
        MenuBar.Menu unExpandListSubMenu = new MenuBar.Menu("Unexpand Cell Instances");
        cellMenu.add(unExpandListSubMenu);
        unExpandListSubMenu.addMenuItem("One Level Up", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.unexpandOneLevelUpCommand(); }});
        unExpandListSubMenu.addMenuItem("All the Way", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.unexpandFullCommand(); }});
        unExpandListSubMenu.addMenuItem("Specified Amount...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.unexpandSpecificCommand(); }});
        cellMenu.addMenuItem("Look Inside Highlighted", KeyStroke.getKeyStroke('P', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.peekCommand(); }});

        cellMenu.addSeparator();
        cellMenu.addMenuItem("Package Into Cell...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.packageIntoCell(); } });
        cellMenu.addMenuItem("Extract Cell Instance", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.extractCells(); } });

    }

    /**
     * This method implements the command to do cell options.
     */
    public static void cellControlCommand()
    {
        CellProperties dialog = new CellProperties(TopLevel.getCurrentJFrame(), true);
        dialog.setVisible(true);
    }

    /**
     * This command opens a dialog box to edit a Cell.
     */
    public static void newCellCommand()
    {
		NewCell dialog = new NewCell(TopLevel.getCurrentJFrame(), true);
        dialog.setVisible(true);
    }

    public static void cellBrowserCommand(CellBrowser.DoAction action)
    {
        CellBrowser dialog = new CellBrowser(TopLevel.getCurrentJFrame(), false, action);
        dialog.setVisible(true);
    }

    /**
     * This method implements the command to make the current cell a multi-page schematic.
     */
    public static void makeMultiPageCell()
    {
    	Cell cell = WindowFrame.needCurCell();
    	if (cell == null) return;
    	if (cell.getView() != View.SCHEMATIC)
    	{
    		System.out.println("Only Schematic cells can be made multi-page");
    		return;
    	}
    	Dimension d = new Dimension(0,0);
    	if (Cell.FrameDescription.getCellFrameInfo(cell, d) != 0)
    	{
       		System.out.println("Must turn on cell frames before making the cell multi-page");
    		return;
    	}

		SetMultiPageJob job = new SetMultiPageJob(cell);
    }

    private static class SetMultiPageJob extends Job
	{
		private Cell cell;

		private SetMultiPageJob(Cell cell)
		{
			super("Make Cell be Multi-Page", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt()
		{
	    	cell.setMultiPage(true);
	    	System.out.println("Cell " + cell.describe() + " is now a multi-page schematic");
			return true;
		}
	}

    /**
     * This method implements the command to create a new page in a multi-page schematic.
     */
    public static void createNewMultiPage()
    {
    	EditWindow wnd = EditWindow.needCurrent();
    	if (wnd == null) return;
    	Cell cell = WindowFrame.needCurCell();
    	if (cell == null) return;
    	if (!cell.isMultiPage())
    	{
    		System.out.println("First turn this cell into a multi-page schematic");
    		return;
    	}
    	int numPages = cell.getNumMultiPages();
    	wnd.setMultiPageNumber(numPages);
    }

    /**
     * This method implements the command to edit the next page in a multi-page schematic.
     */
    public static void editNextMultiPage()
    {
    	EditWindow wnd = EditWindow.needCurrent();
    	if (wnd == null) return;
    	Cell cell = WindowFrame.needCurCell();
    	if (cell == null) return;
    	if (!cell.isMultiPage())
    	{
    		System.out.println("First turn this cell into a multi-page schematic");
    		return;
    	}
    	int curPage = wnd.getMultiPageNumber();
    	int numPages = cell.getNumMultiPages();
    	wnd.setMultiPageNumber((curPage+1) % numPages);
    }

    /**
     * This method implements the command to do cross-library copies.
     */
    public static void crossLibraryCopyCommand()
    {
		CrossLibCopy dialog = new CrossLibCopy(TopLevel.getCurrentJFrame(), true);
        dialog.setVisible(true);
    }

    /**
     * This command pushes down the hierarchy
     */
    public static void downHierCommand() {
        EditWindow curEdit = EditWindow.needCurrent();
        if (curEdit == null) return;
        curEdit.downHierarchy(false);
    }

    /**
     * This command pushes down the hierarchy "in place".
     */
    public static void downHierInPlaceCommand()
    {
        EditWindow curEdit = EditWindow.needCurrent();
        if (curEdit == null) return;
        curEdit.downHierarchy(true);
    }

    /**
     * This command goes up the hierarchy
     */
    public static void upHierCommand() {
        EditWindow curEdit = EditWindow.needCurrent();
		if (curEdit == null) return;
        curEdit.upHierarchy();
    }

    /**
     * This method implements the command to make a new version of the current Cell.
     */
    public static void newCellVersionCommand()
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;
        CircuitChanges.newVersionOfCell(curCell);
    }

    /**
     * This method implements the command to make a copy of the current Cell.
     */
    public static void duplicateCellCommand()
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;

        String newName = JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), "Name of duplicated cell",
            curCell.getName() + "NEW");
        if (newName == null) return;
        CircuitChanges.duplicateCell(curCell, newName);
    }

    /**
     * This method implements the command to delete old, unused versions of cells.
     */
    public static void deleteOldCellVersionsCommand()
    {
        CircuitChanges.deleteUnusedOldVersions();
    }
}
