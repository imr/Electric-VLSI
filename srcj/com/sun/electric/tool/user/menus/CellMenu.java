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
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.extract.GeometrySearch;
import com.sun.electric.tool.user.CellChangeJobs;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.ExportChanges;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ViewChanges;
import com.sun.electric.tool.user.dialogs.CellBrowser;
import com.sun.electric.tool.user.dialogs.CellLists;
import com.sun.electric.tool.user.dialogs.CellProperties;
import com.sun.electric.tool.user.dialogs.CrossLibCopy;
import com.sun.electric.tool.user.dialogs.NewCell;
import static com.sun.electric.tool.user.menus.EMenuItem.SEPARATOR;
import com.sun.electric.tool.user.ui.*;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

/**
 * Class to handle the commands in the "Cell" pulldown menu.
 */
public class CellMenu {

    static EMenu makeMenu() {
        /****************************** THE CELL MENU ******************************/

		// mnemonic keys available:        H J      Q       YZ
        return new EMenu("_Cell",

            new EMenuItem("Ne_w Cell...", 'N') { public void run() {
                newCellCommand(); }},
            new EMenuItem("_Edit Cell...") { public void run() {
                cellBrowserCommand(CellBrowser.DoAction.editCell); }},
            new EMenuItem("_Place Cell Instance...", KeyStroke.getKeyStroke('N', 0)) { public void run() {
                cellBrowserCommand(CellBrowser.DoAction.newInstance); }},
            new EMenuItem("_Rename Cell...") { public void run() {
                cellBrowserCommand(CellBrowser.DoAction.renameCell); }},
            new EMenuItem("Duplic_ate Cell...") { public void run() {
                cellBrowserCommand(CellBrowser.DoAction.duplicateCell); }},
            new EMenuItem("De_lete Cell...") { public void run() {
                cellBrowserCommand(CellBrowser.DoAction.deleteCell); }},

        // mnemonic keys available: AB   FGHIJKL NOPQRSTU WXYZ
            new EMenu("_Multi-Page Cells",
                new EMenuItem("_Make Cell Multi-Page") { public void run() {
                    makeMultiPageCell(); }},
                new EMenuItem("_Create New Page") { public void run() {
                    createNewMultiPage(); }},
                new EMenuItem("_Delete This Page") { public void run() {
                    deleteThisMultiPage(); }},
                new EMenuItem("_Edit Next Page") { public void run() {
                editNextMultiPage(); }},
                new EMenuItem("Con_vert old-style Multi-Page Schematics") { public void run() {
                ViewChanges.convertMultiPageViews(); }}),

            SEPARATOR,

            new EMenuItem("_Cross-Library Copy...") { public void run() {
                crossLibraryCopyCommand(); }},

        // mnemonic keys available:  BCDEFGHIJKLMNOPQ STUVWXYZ
            new EMenu("Merge Li_braries",
                new EMenuItem("_Add Exports from Library...") { public void run() {
                    ExportChanges.synchronizeLibrary(); }},
                new EMenuItem("_Replace Cells from Library...") { public void run() {
                    ExportChanges.replaceFromOtherLibrary(); }}),

            SEPARATOR,

        // mnemonic keys available: ABC E GHIJ LMNO QRSTUV XYZ
            new EMenu("_Down Hierarchy",
		        new EMenuItem("_Down Hierarchy", 'D') { public void run() {
                    downHierCommand(false, false); }},
		        new EMenuItem("Down Hierarchy, Keep _Focus") { public void run() {
                    downHierCommand(true, false); }},
		        new EMenuItem("Down Hierarchy, New _Window") { public void run() {
                    downHierCommand(false, true); }},
		        new EMenuItem("Down Hierarchy, _Keep Focus, New Window") { public void run() {
                    downHierCommand(true, true); }},
                SEPARATOR,
		        new EMenuItem("Down Hierarchy In _Place", KeyStroke.getKeyStroke('D', 0)) { public void run() {
                    downHierInPlaceCommand(); }},
                new EMenuItem("Down Hierarchy In Place To Object", KeyStroke.getKeyStroke('D', KeyEvent.SHIFT_MASK)) { public void run() {
                    downHierInPlaceToObject(); }}),

		    new EMenuItem("_Up Hierarchy", 'U') { public void run() {
                upHierCommand(); }},

            SEPARATOR,

            new EMenuItem("New _Version of Current Cell") { public void run() {
                newCellVersionCommand(); }},
            new EMenuItem("Duplicate Curre_nt Cell") { public void run() {
                duplicateCellCommand(); }},
            new EMenuItem("Delete Unused _Old Versions") { public void run() {
                deleteOldCellVersionsCommand(); }},

            SEPARATOR,

		// mnemonic keys available:  BC    H JKLM OPQRS   WXYZ
            new EMenu("Cell In_fo",
                new EMenuItem("_Describe this Cell") { public void run() {
                    CellLists.describeThisCellCommand(); }},
                new EMenuItem("_General Cell Lists...") { public void run() {
                    CellLists.generalCellListsCommand(); }},
                SEPARATOR,
                new EMenuItem("List _Nodes in this Cell") { public void run() {
                    CellLists.listNodesInCellCommand(); }},
                new EMenuItem("List Cell _Instances") { public void run() {
                    CellLists.listCellInstancesCommand(); }},
                new EMenuItem("List Cell _Usage") { public void run() {
                    CellLists.listCellUsageCommand(); }},
                new EMenuItem("Number of _Transistors") { public void run() {
                    CellLists.numberOfTransistorsCommand(); }},
                SEPARATOR,
                new EMenuItem("Graphically, _Entire Library") { public void run() {
                    CircuitChanges.graphCellsInLibrary(); }},
                new EMenuItem("Graphically, _From Current Cell") { public void run() {
                    CircuitChanges.graphCellsFromCell(); }}),

            new EMenuItem("Cell Propertie_s...") { public void run() {
                 cellControlCommand(); }},

            SEPARATOR,

		// mnemonic keys available:  BCDEFGHIJKLMN PQR TUVWXYZ
            new EMenu("E_xpand Cell Instances",
                ToolBar.expandOneLevelCommand, // O
                new EMenuItem("_All the Way") { public void run() {
                    CircuitChanges.DoExpandCommands(false, Integer.MAX_VALUE); }},
                new EMenuItem("_Specified Amount...") { public void run() {
                    CircuitChanges.DoExpandCommands(false, -1); }}),

        // mnemonic keys available:  BCDEFGHIJKLMN PQR TUVWXYZ
            new EMenu("Unexpand Cell Ins_tances",
                ToolBar.unexpandOneLevelCommand, // O
                new EMenuItem("_All the Way") { public void run() {
                    CircuitChanges.DoExpandCommands(true, Integer.MAX_VALUE); }},
                new EMenuItem("_Specified Amount...") { public void run() {
                    CircuitChanges.DoExpandCommands(true, -1); }}),

            new EMenuItem("Loo_k Inside Highlighted", 'P') { public void run() {
                peekCommand(); }},

            SEPARATOR,
            new EMenuItem("Packa_ge Into Cell...") { public void run() {
                CircuitChanges.packageIntoCell(); }},
            new EMenuItem("Extract Cell _Instance") { public void run() {
                CircuitChanges.extractCells(); }});
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

		new SetMultiPageJob(cell, 1);
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
	    	cell.newVar(Cell.MULTIPAGE_COUNT_KEY, numPages); // autoboxing
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
		private int page, numPages;

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
				NodeInst ni = it.next();
				if (ni.getAnchorCenterY() > lY && ni.getAnchorCenterY() < hY) deleteList.add(ni);
			}
			for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				double ctrY = ai.getBounds().getCenterY();
				if (ctrY > lY && ctrY < hY) deleteList.add(ai);
			}
			CircuitChangeJobs.eraseObjectsInList(cell, deleteList, false);

			// now slide circuitry down if this isn't the last page
			numPages = cell.getNumMultiPages();
			if (page+1 < numPages)
			{
				CircuitChangeJobs.spreadCircuitry(cell, null, 'u', -Cell.FrameDescription.MULTIPAGESEPARATION, 0, 0, lY, hY);
			}
	    	cell.newVar(Cell.MULTIPAGE_COUNT_KEY, (numPages-1)); // autoboxing
			fieldVariableChanged("numPages");
			return true;
		}

        public void terminateOK()
        {
	    	for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
	    	{
	    		WindowFrame wf = it.next();
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
		new SetMultiPageJob(cell, numPages+1);
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
    	new DeleteMultiPageJob(cell, curPage);
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

    private static void downHierInPlaceToObject()
    {
        EditWindow curEdit = EditWindow.needCurrent();
        if (curEdit == null) return;
        Cell cell = curEdit.getCell();
        if (cell == null) return;
        if (cell.getView() != View.LAYOUT) {
            System.out.println("Current cell should be layout cell for down hier in place to object");
            return;
        }

        // if object under mouse, descend to that location
        Point2D mouse = ClickZoomWireListener.theOne.getLastMouse();
        Point2D mouseDB = curEdit.screenToDatabase((int)mouse.getX(), (int)mouse.getY());
        EPoint point = new EPoint(mouseDB.getX(), mouseDB.getY());
        GeometrySearch search = new GeometrySearch();
        long start = System.currentTimeMillis();
        if (search.searchGeometries(cell, point, true)) {
            VarContext context = search.getContext();

            if (context == VarContext.globalContext) {
                System.out.println(search.describeFoundGeometry()+", not descending down hierarchy");
                return;
            }
            Geometric geom = search.getGeometricFound();
            System.out.println("Descending to "+geom+" at point ("+point.getX()+","+point.getY()+") in cell "+geom.getParent().getName());
            for (Iterator<Nodable> it = context.getPathIterator(); it.hasNext(); ) {
                Nodable no = it.next();
                Cell curCell = no.getParent();
                curEdit.getHighlighter().clear();
                curEdit.getHighlighter().addElectricObject(no.getNodeInst(), curCell);
                curEdit.getHighlighter().finished();
                System.out.println("  descended into "+no.getName()+"["+no.getProto().getName()+"] in cell "+curCell.getName());
                curEdit.downHierarchy(false, false, true);
            }
            curEdit.getHighlighter().clear();
            curEdit.getHighlighter().addElectricObject(geom, geom.getParent());
            curEdit.getHighlighter().finished();
        } else {
            // nothing found
            System.out.println("No primitive node or arc found under mouse to descend to.");
        }
        System.out.println("Search took "+ TextUtils.getElapsedTime(System.currentTimeMillis()-start));
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
        duplicateCell(curCell, false);
    }
    
    public static void duplicateCell(Cell cell, boolean allInGroup)
    {
    	String prompt = "Name of duplicated cell";
    	if (allInGroup) prompt += " group";
        String newName = JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(),
        	prompt, cell.getName() + "NEW");
        if (newName == null) return;
        Cell already = cell.getLibrary().findNodeProto(newName);
		if (already != null && already.getView() == cell.getView())
		{
			int response = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
				"Cell " + newName + " already exists.  Make this a new version?", "Confirm duplication",
				JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[] {"Yes", "Cancel"}, "Yes");
			if (response != 0) return;
		}
        new CellChangeJobs.DuplicateCell(cell, newName, allInGroup);
    }

    /**
     * Method to delete old, unused versions of cells.
     */
    private static void deleteOldCellVersionsCommand()
    {
		for(Iterator<Cell> it = Library.getCurrent().getCells(); it.hasNext(); )
    	{
    		Cell cell = it.next();
			if (cell.getNewestVersion() == cell) continue;
			if (cell.getInstancesOf().hasNext()) continue;
	    	CircuitChanges.cleanCellRef(cell);
    	}
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
