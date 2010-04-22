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
 * the Free Software Foundation; either version 3 of the License, or
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

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
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
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.NewCell;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

/**
 * Class to handle the commands in the "Cell" pulldown menu.
 */
public class CellMenu {

	static EMenu makeMenu() {
		/****************************** THE CELL MENU ******************************/

		// mnemonic keys available:	    H J      Q        Z
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

			new EMenu("_Up Hierarchy",
				new EMenuItem("_Up Hierarchy", 'U') { public void run() {
					upHierCommand(false); }},
				new EMenuItem("Up Hierarchy, Keep _Focus") { public void run() {
					upHierCommand(true); }}),

			// mnemonic keys available: A CDE GHIJKLMNOPQRSTUVWXYZ
			new EMenu("Cell Viewing Histor_y",
				new EMenuItem("Go _Back a Cell") { public void run() {
					changeCellHistory(true); }},
				new EMenuItem("Go _Forward a Cell") { public void run() {
					changeCellHistory(false); }}),

			SEPARATOR,

			new EMenuItem("New _Version of Current Cell") { public void run() {
				newCellVersionCommand(); }},
			new EMenuItem("Duplicate Curre_nt Cell") { public void run() {
				duplicateCellCommand(); }},
			new EMenuItem("Delete Unused _Old Versions") { public void run() {
				deleteOldCellVersionsCommand(); }},

			SEPARATOR,

			// mnemonic keys available:  BC      JK M OPQR   WXYZ
			new EMenu("Cell In_fo",
				new EMenuItem("_Describe this Cell") { public void run() {
					CellLists.describeThisCellCommand(); }},
				new EMenuItem("_General Cell Lists...") { public void run() {
					CellLists.generalCellListsCommand(); }},
				SEPARATOR,
				new EMenuItem("_Summarize Cell Contents") { public void run() {
					CellLists.designSummaryCommand(); }},
				new EMenuItem("List _Nodes/Arcs in this Cell") { public void run() {
					CellLists.listNodesAndArcsInCellCommand(); }},
				new EMenuItem("List Cell _Instances") { public void run() {
					CellLists.listCellInstancesCommand(); }},
				new EMenuItem("List Cell _Usage") { public void run() {
					CellLists.listCellUsageCommand(false); }},
				new EMenuItem("List Cell Usage, _Hierarchically") { public void run() {
					CellLists.listCellUsageCommand(true); }},
				new EMenuItem("Number of _Transistors") { public void run() {
					CellLists.numberOfTransistorsCommand(); }},
				SEPARATOR,
				new EMenuItem("Cell Graph, _Entire Library") { public void run() {
					CircuitChanges.graphCellsInLibrary(); }},
				new EMenuItem("Cell Graph, _From Current Cell") { public void run() {
					CircuitChanges.graphCellsFromCell(); }},
				new EMenuItem("_Library Graph") { public void run() {
					CircuitChanges.graphLibraries(); }}),

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

			// mnemonic keys available:  BCDEFGHIJKLMN PQR TUVWXYZ
			new EMenu("Extract Cell _Instance",
				new EMenuItem("_One Level Down") { public void run() {
					CircuitChanges.extractCells(1); }},
				new EMenuItem("_All the Way") { public void run() {
					CircuitChanges.extractCells(Integer.MAX_VALUE); }},
				new EMenuItem("_Specified Amount...") { public void run() {
					CircuitChanges.extractCells(-1); }}));
	}

	/**
	 * This method implements the command to do cell options.
	 */
	private static void cellControlCommand()
	{
		CellProperties dialog = new CellProperties(TopLevel.getCurrentJFrame());
		dialog.setVisible(true);
	}

	/**
	 * This command opens a dialog box to edit a Cell.
	 */
	private static void newCellCommand()
	{
		NewCell dialog = new NewCell(TopLevel.getCurrentJFrame());
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
		if (!cell.isSchematic())
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
			cell.newVar(Cell.MULTIPAGE_COUNT_KEY, new Integer(numPages)); // autoboxing
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
			CircuitChangeJobs.eraseObjectsInList(cell, deleteList, false, null);

			// now slide circuitry down if this isn't the last page
			numPages = cell.getNumMultiPages();
			if (page+1 < numPages)
			{
				CircuitChangeJobs.spreadCircuitry(cell, null, 'u', -Cell.FrameDescription.MULTIPAGESEPARATION, 0, 0, lY, hY);
			}
			cell.newVar(Cell.MULTIPAGE_COUNT_KEY, new Integer((numPages-1))); // autoboxing
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
		CrossLibCopy dialog = new CrossLibCopy(TopLevel.getCurrentJFrame());
		dialog.setVisible(true);
	}

	/**
	 * This command pushes down the hierarchy
	 * @param keepFocus true to keep the zoom and scale in the new window.
	 * @param newWindow true to create a new window for the cell.
	 */
	private static void downHierCommand(boolean keepFocus, boolean newWindow)
	{
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

	private static class DownHierToObjectActionListener implements ActionListener
	{
		GeometrySearch.GeometrySearchResult result;

		DownHierToObjectActionListener(GeometrySearch.GeometrySearchResult r) { result = r; }

		public void actionPerformed(ActionEvent e)
		{
			descendToObject(result);
		}
	}

	private static void downHierInPlaceToObject()
	{
		EditWindow curEdit = EditWindow.needCurrent();
		if (curEdit == null) return;
		Cell cell = curEdit.getCell();
		if (cell == null) return;
		if (cell.getView() != View.LAYOUT)
		{
			System.out.println("Current cell should be a layout cell for 'Down Hierarchy In Place To Object'");
			return;
		}

		// find all objects under the mouse
		Point2D mouse = ClickZoomWireListener.theOne.getLastMouse();
		Point2D mouseDB = curEdit.screenToDatabase((int)mouse.getX(), (int)mouse.getY());
		EPoint point = new EPoint(mouseDB.getX(), mouseDB.getY());
		GeometrySearch.GeometrySearchResult foundAtTopLevel = null;
		GeometrySearch search = new GeometrySearch(curEdit.getLayerVisibility());
		List<GeometrySearch.GeometrySearchResult> possibleTargets = search.searchGeometries(cell, point, true);

		// eliminate results at the top level and duplicate results at any lower level
		for(int i=0; i<possibleTargets.size(); i++)
		{
			GeometrySearch.GeometrySearchResult res = possibleTargets.get(i);
			if (res.getContext() == VarContext.globalContext)
			{
				possibleTargets.remove(i);
				i--;
				foundAtTopLevel = res;
				continue;
			}

			// also remove duplicate contexts at lower levels
			for(int j=0; j<i; j++)
			{
				GeometrySearch.GeometrySearchResult oRes = possibleTargets.get(j);
				if (sameContext(oRes.getContext(), res.getContext()))
				{
					possibleTargets.remove(i);
					i--;
				}
			}
		}

		// give error if nothing was found
		if (possibleTargets.size() == 0)
		{
			// nothing found, if top-level stuff found, say so
			if (foundAtTopLevel != null)
				System.out.println(foundAtTopLevel.describe() + " is at the top level, not down the hierarchy"); else
					System.out.println("No primitive node or arc found under the mouse at lower levels of hierarchy");
			return;
		}

		// get the selected object to edit
		if (possibleTargets.size() == 1)
		{
			descendToObject(possibleTargets.get(0));
		} else
		{
			// let the user choose
            JPopupMenu menu = new JPopupMenu();
        	JMenuItem menuItem = new JMenuItem("Multiple objects under the cursor...choose one");
            menu.add(menuItem);
            menu.addSeparator();
            for(GeometrySearch.GeometrySearchResult res : possibleTargets)
            {
            	menuItem = new JMenuItem(res.describe());
            	menuItem.addActionListener(new DownHierToObjectActionListener(res));
                menu.add(menuItem);
            }
            menu.show(curEdit, 100, 100);
		}
	}

	private static boolean sameContext(VarContext vc1, VarContext vc2)
	{
		if (vc1.getNumLevels() != vc2.getNumLevels()) return false;
		while (vc1.getNodable() != null && vc2.getNodable() != null)
		{
			if (vc1.getNodable() != vc2.getNodable()) return false;
			vc1 = vc1.pop();
			vc2 = vc2.pop();
		}
		return true;
	}

	private static void descendToObject(GeometrySearch.GeometrySearchResult res)
	{
		// descend to that object
		EditWindow curEdit = EditWindow.needCurrent();
		System.out.println("Descending to cell " + res.getGeometric().getParent().getName());
		for (Iterator<Nodable> it = res.getContext().getPathIterator(); it.hasNext(); )
		{
			Nodable no = it.next();
			Cell curCell = no.getParent();
			curEdit.getHighlighter().clear();
			curEdit.getHighlighter().addElectricObject(no.getNodeInst(), curCell);
			curEdit.getHighlighter().finished();
			System.out.println("  Descended into "+no.getName()+"["+no.getProto().getName()+"] in cell "+curCell.getName());
			curEdit.downHierarchy(false, false, true);
		}
		curEdit.getHighlighter().clear();
		curEdit.getHighlighter().addElectricObject(res.getGeometric(), res.getGeometric().getParent());
		curEdit.getHighlighter().finished();
	}

	/**
	 * This command goes up the hierarchy
	 */
	private static void upHierCommand(boolean keepFocus)
	{
		EditWindow curEdit = EditWindow.needCurrent();
		if (curEdit == null) return;
		curEdit.upHierarchy(keepFocus);
	}

	private static void changeCellHistory(boolean back)
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return;
		if (back) wf.cellHistoryGoBack(); else
			wf.cellHistoryGoForward();
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
		new CellMenu.NewCellName(false, curCell);
	}

	public static class NewCellName extends EDialog
	{
		private JTextField cellName;
		private Cell cell;
		private boolean allInGroup;

		/** Creates new form New Cell Name */
		public NewCellName(boolean allInGroup, Cell cell)
		{
			super(TopLevel.getCurrentJFrame(), true);
			this.allInGroup = allInGroup;
			this.cell = cell;

	        setTitle(allInGroup ? "New Group Name" : "New Cell Name");
	        setName("");
	        addWindowListener(new WindowAdapter() {
	            public void windowClosing(WindowEvent evt) { closeDialog(); }
	        });
	        getContentPane().setLayout(new GridBagLayout());

			String prompt = "Name of duplicated cell";
			if (allInGroup) prompt += " group";
	        JLabel lab = new JLabel(prompt + ":");
	        lab.setHorizontalAlignment(SwingConstants.LEFT);
	        GridBagConstraints gbc = new GridBagConstraints();
	        gbc.gridx = 0;   gbc.gridy = 0;
	        gbc.gridwidth = 2;
	        gbc.anchor = GridBagConstraints.WEST;
	        gbc.insets = new Insets(4, 4, 4, 4);
	        getContentPane().add(lab, gbc);

	        String oldCellName = cell.getName();
	        String newName = oldCellName + "NEW";
	        cellName = new JTextField(newName);
	        cellName.setSelectionStart(oldCellName.length());
	        cellName.setSelectionEnd(newName.length());
	        cellName.setColumns(Math.max(20, newName.length()));
	        cellName.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent evt) { ok(); }
	        });
	        gbc = new GridBagConstraints();
	        gbc.gridx = 0;   gbc.gridy = 1;
	        gbc.gridwidth = 2;
	        gbc.fill = GridBagConstraints.HORIZONTAL;
	        gbc.anchor = GridBagConstraints.WEST;
	        gbc.weightx = 1.0;
	        gbc.insets = new Insets(4, 4, 4, 4);
	        getContentPane().add(cellName, gbc);

	        JButton cancel = new JButton("Cancel");
	        cancel.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent evt) { closeDialog(); }
	        });
	        gbc = new GridBagConstraints();
	        gbc.gridx = 0;   gbc.gridy = 2;
	        gbc.insets = new Insets(4, 4, 4, 4);
	        getContentPane().add(cancel, gbc);

	        JButton ok = new JButton("OK");
	        ok.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent evt) { ok(); }
	        });
	        gbc = new GridBagConstraints();
	        gbc.gridx = 1;   gbc.gridy = 2;
	        gbc.insets = new Insets(4, 4, 4, 4);
	        getContentPane().add(ok, gbc);
			getRootPane().setDefaultButton(ok);

	        pack();
			finishInitialization();
			setVisible(true);
	    }

		protected void escapePressed() { closeDialog(); }

		private void ok()
		{
	    	String newName = cellName.getText();
			closeDialog();
			Cell already = cell.getLibrary().findNodeProto(newName);
			if (already != null && already.getView() == cell.getView())
			{
				int response = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
					"Cell " + newName + " already exists.  Make this a new version?", "Confirm duplication",
					JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[] {"Yes", "Cancel"}, "Yes");
				if (response != 0) return;
			}
			new CellChangeJobs.DuplicateCell(cell, newName, cell.getLibrary(), allInGroup, false);
		}
	}

	/**
	 * Method to delete old, unused versions of cells.
	 */
	private static void deleteOldCellVersionsCommand()
	{
		// count the number of old unused cells to delete in the current and in other libraries
		int oldUnusedCurrent = 0, oldUnusedElsewhere = 0;
		for(Library lib : Library.getVisibleLibraries())
		{
			for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
			{
				Cell cell = it.next();
				if (cell.getNewestVersion() == cell) continue;
				if (cell.getInstancesOf().hasNext()) continue;
				if (lib == Library.getCurrent()) oldUnusedCurrent++; else
					oldUnusedElsewhere++;
			}
		}

		// if complex, prompt for what to do
		if (oldUnusedCurrent+oldUnusedElsewhere != 0 && oldUnusedElsewhere != 0)
		{
			// old unused cells are not just in the current library: ask what to do
			String [] options = {"Current library", "Other libraries", "All libraries", "Cancel"};
			int ret = Job.getUserInterface().askForChoice("There are " + oldUnusedCurrent +
				" old unused cells in the current library and " + oldUnusedElsewhere +
				" in other libraries.  Which libraries should have their old unused cells deleted?",
				"Which Old Unused Cells to Delete", options, "No");
			if (ret == 0) oldUnusedElsewhere = 0;
			if (ret == 1) oldUnusedCurrent = 0;
			if (ret == 3) return;
		}

		// stop now if nothing to delete
		if (oldUnusedCurrent == 0 && oldUnusedElsewhere == 0)
		{
			System.out.println("There are no old unused cells to delete");
			return;
		}

		// pre-clean the cell references
		List<Cell> cellsToDelete = new ArrayList<Cell>();
		for(Library lib : Library.getVisibleLibraries())
		{
			if (lib == Library.getCurrent())
			{
				if (oldUnusedCurrent == 0) continue;
			} else
			{
				if (oldUnusedElsewhere == 0) continue;
			}
			for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
			{
				Cell cell = it.next();
				if (cell.getNewestVersion() == cell) continue;
				if (cell.getInstancesOf().hasNext()) continue;
				CircuitChanges.cleanCellRef(cell);
				cellsToDelete.add(cell);
			}
		}

		// do the deletion
		new CellChangeJobs.DeleteManyCells(cellsToDelete);
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
