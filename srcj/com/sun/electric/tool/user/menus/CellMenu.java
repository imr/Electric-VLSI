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

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.CellChangeJobs;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ViewChanges;
import com.sun.electric.tool.user.dialogs.CellBrowser;
import com.sun.electric.tool.user.dialogs.CellLists;
import com.sun.electric.tool.user.dialogs.CellProperties;
import com.sun.electric.tool.user.dialogs.CrossLibCopy;
import com.sun.electric.tool.user.dialogs.NewCell;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

/**
 * Class to handle the commands in the "Cell" pulldown menu.
 */
public class CellMenu {

    protected static void addCellMenu(MenuBar menuBar) {
		int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        /****************************** THE CELL MENU ******************************/

		// mnemonic keys available:  B     H J      Q       YZ
        MenuBar.Menu cellMenu = MenuBar.makeMenu("_Cell");
        menuBar.add(cellMenu);

        cellMenu.addMenuItem("Ne_w Cell...", KeyStroke.getKeyStroke('N', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { newCellCommand(); } });
        cellMenu.addMenuItem("_Edit Cell...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { cellBrowserCommand(CellBrowser.DoAction.editCell); }});
        cellMenu.addMenuItem("_Place Cell Instance...", KeyStroke.getKeyStroke('N', 0),
            new ActionListener() { public void actionPerformed(ActionEvent e) { cellBrowserCommand(CellBrowser.DoAction.newInstance); }});
        cellMenu.addMenuItem("_Rename Cell...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { cellBrowserCommand(CellBrowser.DoAction.renameCell); }});
        cellMenu.addMenuItem("Duplic_ate Cell...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { cellBrowserCommand(CellBrowser.DoAction.duplicateCell); }});
        cellMenu.addMenuItem("De_lete Cell...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { cellBrowserCommand(CellBrowser.DoAction.deleteCell); }});

        // mnemonic keys available: AB   FGHIJKL NOPQRSTU WXYZ
        MenuBar.Menu multiPageSubMenu = MenuBar.makeMenu("_Multi-Page Cells");
        cellMenu.add(multiPageSubMenu);
        multiPageSubMenu.addMenuItem("_Make Cell Multi-Page", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { makeMultiPageCell(); }});
        multiPageSubMenu.addMenuItem("_Create New Page", null,
                new ActionListener() { public void actionPerformed(ActionEvent e) { createNewMultiPage(); }});
        multiPageSubMenu.addMenuItem("_Delete This Page", null,
                new ActionListener() { public void actionPerformed(ActionEvent e) { deleteThisMultiPage(); }});
        multiPageSubMenu.addMenuItem("_Edit Next Page", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { editNextMultiPage(); }});
        multiPageSubMenu.addMenuItem("Con_vert old-style Multi-Page Schematics", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ViewChanges.convertMultiPageViews(); }});

        cellMenu.addSeparator();

        cellMenu.addMenuItem("_Cross-Library Copy...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { crossLibraryCopyCommand(); } });

        cellMenu.addSeparator();

        // mnemonic keys available: ABC E GHIJ LMNO QRSTUV XYZ
        MenuBar.Menu downHierarchySubMenu = MenuBar.makeMenu("_Down Hierarchy");
        cellMenu.add(downHierarchySubMenu);
		downHierarchySubMenu.addMenuItem("_Down Hierarchy", KeyStroke.getKeyStroke('D', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { downHierCommand(false, false); }});
		downHierarchySubMenu.addMenuItem("Down Hierarchy, Keep _Focus", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { downHierCommand(true, false); }});
		downHierarchySubMenu.addMenuItem("Down Hierarchy, New _Window", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { downHierCommand(false, true); }});
		downHierarchySubMenu.addMenuItem("Down Hierarchy, _Keep Focus, New Window", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { downHierCommand(true, true); }});
		downHierarchySubMenu.addSeparator();
		downHierarchySubMenu.addMenuItem("Down Hierarchy In _Place", KeyStroke.getKeyStroke('D', 0),
            new ActionListener() { public void actionPerformed(ActionEvent e) { downHierInPlaceCommand(); }});

		cellMenu.addMenuItem("_Up Hierarchy", KeyStroke.getKeyStroke('U', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { upHierCommand(); }});

        cellMenu.addSeparator();

        cellMenu.addMenuItem("New _Version of Current Cell", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { newCellVersionCommand(); } });
        cellMenu.addMenuItem("Duplicate Curre_nt Cell", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { duplicateCellCommand(); } });
        cellMenu.addMenuItem("Delete Unused _Old Versions", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { deleteOldCellVersionsCommand(); } });

        cellMenu.addSeparator();

		// mnemonic keys available:  BC    H JKLM OPQRST  WXYZ
        MenuBar.Menu cellInfoSubMenu = MenuBar.makeMenu("Cell In_fo");
        cellMenu.add(cellInfoSubMenu);
        cellInfoSubMenu.addMenuItem("_Describe this Cell", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CellLists.describeThisCellCommand(); } });
        cellInfoSubMenu.addMenuItem("_General Cell Lists...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CellLists.generalCellListsCommand(); } });
        cellInfoSubMenu.addSeparator();
        cellInfoSubMenu.addMenuItem("List _Nodes in this Cell", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CellLists.listNodesInCellCommand(); }});
        cellInfoSubMenu.addMenuItem("List Cell _Instances", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CellLists.listCellInstancesCommand(); }});
        cellInfoSubMenu.addMenuItem("List Cell _Usage", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CellLists.listCellUsageCommand(); }});
        cellInfoSubMenu.addSeparator();
        cellInfoSubMenu.addMenuItem("Graphically, _Entire Library", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.graphCellsInLibrary(); }});
        cellInfoSubMenu.addMenuItem("Graphically, _From Current Cell", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.graphCellsFromCell(); }});

        cellMenu.addMenuItem("Cell Propertie_s...", null,
             new ActionListener() { public void actionPerformed(ActionEvent e) { cellControlCommand(); }});

        cellMenu.addSeparator();

		// mnemonic keys available:  BCDEFGHIJKLMN PQR TUVWXYZ
        MenuBar.Menu expandListSubMenu = MenuBar.makeMenu("E_xpand Cell Instances");
        cellMenu.add(expandListSubMenu);
        expandListSubMenu.addMenuItem("_One Level Down", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.DoExpandCommands(false, 1); }});
        expandListSubMenu.addMenuItem("_All the Way", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.DoExpandCommands(false, Integer.MAX_VALUE); }});
        expandListSubMenu.addMenuItem("_Specified Amount...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.DoExpandCommands(false, -1); }});

        // mnemonic keys available:  BCDEFGHIJKLMN PQR TUVWXYZ
        MenuBar.Menu unExpandListSubMenu = MenuBar.makeMenu("Unexpand Cell Ins_tances");
        cellMenu.add(unExpandListSubMenu);
        unExpandListSubMenu.addMenuItem("_One Level Up", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.DoExpandCommands(true, 1); }});
        unExpandListSubMenu.addMenuItem("_All the Way", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.DoExpandCommands(true, Integer.MAX_VALUE); }});
        unExpandListSubMenu.addMenuItem("_Specified Amount...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.DoExpandCommands(true, -1); }});

        cellMenu.addMenuItem("Loo_k Inside Highlighted", KeyStroke.getKeyStroke('P', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { peekCommand(); }});

        cellMenu.addSeparator();
        cellMenu.addMenuItem("Packa_ge Into Cell...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.packageIntoCell(); } });
        cellMenu.addMenuItem("Extract Cell _Instance", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.extractCells(); } });
    }

    /**
     * This method implements the command to do cell options.
     */
    private static void cellControlCommand()
    {
        CellProperties dialog = new CellProperties(TopLevel.getCurrentJFrame(), true);
        dialog.setVisible(true);
    }

    /**
     * This command opens a dialog box to edit a Cell.
     */
    private static void newCellCommand()
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
    private static void makeMultiPageCell()
    {
    	Cell cell = WindowFrame.needCurCell();
    	if (cell == null) return;
    	if (cell.getView() != View.SCHEMATIC)
    	{
    		JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), "Only Schematic cells can be made multi-page",
    			"Cannot make multipage design", JOptionPane.ERROR_MESSAGE);
    		return;
    	}

		SetMultiPageJob job = new SetMultiPageJob(cell, 1);
    }

    /**
     * Class to set a cell to be multi-page with a given page count.
     */
    public static class SetMultiPageJob extends Job
	{
		private Cell cell;
		private int numPages;

		public SetMultiPageJob(Cell cell, int numPages)
		{
			super("Make Cell be Multi-Page", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.numPages = numPages;
			startJob();
		}

		public boolean doIt() throws JobException
		{
	    	Dimension d = new Dimension(0,0);
	    	if (Cell.FrameDescription.getCellFrameInfo(cell, d) != 0)
	    	{
				cell.newVar(User.FRAME_SIZE, "a");
	       		System.out.println("Multi-page schematics must have cell frames turned on.  Setting this to A-size.");
	    	}
			boolean wasMulti = cell.isMultiPage();
	    	cell.setMultiPage(true);
	    	cell.newVar(Cell.MULTIPAGE_COUNT_KEY, new Integer(numPages));
	    	if (!wasMulti) System.out.println("Cell " + cell.describe(true) + " is now a multi-page schematic");
			return true;
		}
	}

    /**
     * Class to delete a page from a multi-page schematic.
     */
    public static class DeleteMultiPageJob extends Job
	{
		private Cell cell;
		private int page;

		public DeleteMultiPageJob(Cell cell, int page)
		{
			super("Delete Page from Multi-Page Schematic", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.page = page;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// first delete all circuitry on the page
			double lY = page * Cell.FrameDescription.MULTIPAGESEPARATION - Cell.FrameDescription.MULTIPAGESEPARATION/2;
			double hY = lY + Cell.FrameDescription.MULTIPAGESEPARATION;
			List<Geometric> deleteList = new ArrayList<Geometric>();
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (ni.getAnchorCenterY() > lY && ni.getAnchorCenterY() < hY) deleteList.add(ni);
			}
			for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				double ctrY = ai.getBounds().getCenterY();
				if (ctrY > lY && ctrY < hY) deleteList.add(ai);
			}
			CircuitChangeJobs.eraseObjectsInList(cell, deleteList);

			// now slide circuitry down if this isn't the last page
			int numPages = cell.getNumMultiPages();
			if (page+1 < numPages)
			{
				CircuitChangeJobs.spreadCircuitry(cell, null, 'u', -Cell.FrameDescription.MULTIPAGESEPARATION, 0, 0, lY, hY);
			}
	    	cell.newVar(Cell.MULTIPAGE_COUNT_KEY, new Integer(numPages-1));
	    	for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
	    	{
	    		WindowFrame wf = (WindowFrame)it.next();
	    		if (wf.getContent() instanceof EditWindow)
	    		{
	               	EditWindow wnd = (EditWindow)wf.getContent();
	               	if (wnd.getCell() == cell)
	               	{
	               		int wndPage = wnd.getMultiPageNumber();
	               		if (wndPage+1 >= numPages)
	               			wnd.setMultiPageNumber(wndPage-1);
	               	}
	    		}
	    	}
			return true;
		}
	}

    /**
     * This method implements the command to create a new page in a multi-page schematic.
     */
    private static void createNewMultiPage()
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
		SetMultiPageJob job = new SetMultiPageJob(cell, numPages+1);
    	wnd.setMultiPageNumber(numPages);
    }

    /**
     * This method implements the command to delete the current page in a multi-page schematic.
     */
    private static void deleteThisMultiPage()
    {
    	EditWindow wnd = EditWindow.needCurrent();
    	if (wnd == null) return;
    	Cell cell = WindowFrame.needCurCell();
    	if (cell == null) return;
    	if (!cell.isMultiPage())
    	{
    		System.out.println("This is not a multi-page schematic.  To delete this cell, use 'Cell / Delete Cell'");
    		return;
    	}
    	int curPage = wnd.getMultiPageNumber();
    	DeleteMultiPageJob job = new DeleteMultiPageJob(cell, curPage);
    	int numPages = cell.getNumMultiPages();
    	if (curPage >= numPages) wnd.setMultiPageNumber(numPages-1);
    }

    /**
     * This method implements the command to edit the next page in a multi-page schematic.
     */
    private static void editNextMultiPage()
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
    private static void crossLibraryCopyCommand()
    {
		CrossLibCopy dialog = new CrossLibCopy(TopLevel.getCurrentJFrame(), true);
        dialog.setVisible(true);
    }

    /**
     * This command pushes down the hierarchy
     * @param keepFocus true to keep the zoom and scale in the new window.
     * @param newWindow true to create a new window for the cell.
     */
    private static void downHierCommand(boolean keepFocus, boolean newWindow) {
        EditWindow curEdit = EditWindow.needCurrent();
        if (curEdit == null) return;
        curEdit.downHierarchy(keepFocus, newWindow, false);
    }

    /**
     * This command pushes down the hierarchy "in place".
     */
    private static void downHierInPlaceCommand()
    {
        EditWindow curEdit = EditWindow.needCurrent();
        if (curEdit == null) return;
        curEdit.downHierarchy(false, false, true);
    }

    /**
     * This command goes up the hierarchy
     */
    private static void upHierCommand() {
        EditWindow curEdit = EditWindow.needCurrent();
		if (curEdit == null) return;
        curEdit.upHierarchy();
    }

    /**
     * This method implements the command to make a new version of the current Cell.
     */
    private static void newCellVersionCommand()
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;
        CircuitChanges.newVersionOfCell(curCell);
    }

    /**
     * This method implements the command to make a copy of the current Cell.
     */
    private static void duplicateCellCommand()
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;

        String newName = JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), "Name of duplicated cell",
            curCell.getName() + "NEW");
        if (newName == null) return;
        new CellChangeJobs.DuplicateCell(curCell, newName);
    }

    /**
     * Method to delete old, unused versions of cells.
     */
    private static void deleteOldCellVersionsCommand()
    {
    	new CellChangeJobs.DeleteUnusedOldCells();
    }
    
    /**
     * Method to temporarily expand the current selected area to the bottom.
     */
    private static void peekCommand()
	{
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();
        if (highlighter == null) return;

		Rectangle2D bounds = highlighter.getHighlightedArea(wnd);
		if (bounds == null)
		{
			System.out.println("Must define an area in which to display");
			return;
		}
		wnd.repaintContents(bounds, true);
	}
}
