/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UserMenuCommands.java
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
package com.sun.electric.tool.user;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.hierarchy.NodeUsage;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.DialogOpenFile;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.io.Input;
import com.sun.electric.tool.io.Output;
import com.sun.electric.tool.user.ui.Menu;
import com.sun.electric.tool.logicaleffort.LENetlister;

import java.util.Iterator;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import javax.swing.JMenuBar;
import javax.swing.KeyStroke;

/**
 * This class has all of the pulldown menu commands in Electric.
 */
public final class UserMenuCommands
{
	// It is never useful for anyone to create an instance of this class
	private UserMenuCommands() {}

	/**
	 * Routine to create the pulldown menus.
	 */
	public static JMenuBar createMenuBar()
	{
		// create the menu bar
		JMenuBar menuBar = new JMenuBar();

		// setup the File menu
		Menu fileMenu = Menu.createMenu("File", 'F');
		menuBar.add(fileMenu);
		fileMenu.addMenuItem("Open", KeyStroke.getKeyStroke('O', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { openLibraryCommand(); } });
		Menu importSubMenu = Menu.createMenu("Import");
		fileMenu.add(importSubMenu);
		importSubMenu.addMenuItem("Readable Dump", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(); } });
		fileMenu.addMenuItem("Save", KeyStroke.getKeyStroke('S', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveLibraryCommand(); } });
		fileMenu.addMenuItem("Save as...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveAsLibraryCommand(); } });
		fileMenu.addSeparator();
		fileMenu.addMenuItem("Quit", KeyStroke.getKeyStroke('Q', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { quitCommand(); } });

		// setup the Edit menu
		Menu editMenu = Menu.createMenu("Edit", 'E');
		menuBar.add(editMenu);
		editMenu.addMenuItem("Undo", KeyStroke.getKeyStroke('Z', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { undoCommand(); } });
		editMenu.addMenuItem("Redo", KeyStroke.getKeyStroke('Y', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { redoCommand(); } });
		editMenu.addMenuItem("Show Undo List", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { showUndoListCommand(); } });

		// setup the Cell menu
		Menu cellMenu = Menu.createMenu("Cell", 'C');
		menuBar.add(cellMenu);
        cellMenu.addMenuItem("Down Hierarchy", KeyStroke.getKeyStroke('D', InputEvent.CTRL_MASK),
            new ActionListener() { public void actionPerformed(ActionEvent e) { downHierCommand(); }});
        cellMenu.addMenuItem("Up Hierarchy", KeyStroke.getKeyStroke('U', InputEvent.CTRL_MASK),
            new ActionListener() { public void actionPerformed(ActionEvent e) { upHierCommand(); }});
		cellMenu.addMenuItem("Show Cell Groups", KeyStroke.getKeyStroke('T', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { showCellGroupsCommand(); } });

		// setup the Export menu
		Menu exportMenu = Menu.createMenu("Export", 'X');
		menuBar.add(exportMenu);

		// setup the View menu
		Menu viewMenu = Menu.createMenu("View", 'V');
		menuBar.add(viewMenu);

		// setup the Window menu
		Menu windowMenu = Menu.createMenu("Window", 'W');
		menuBar.add(windowMenu);
		windowMenu.addMenuItem("Full Display", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { fullDisplayCommand(); } });
		windowMenu.addMenuItem("Toggle Grid", KeyStroke.getKeyStroke('G', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { toggleGridCommand(); } });

		// setup the Technology menu
		Menu technologyMenu = Menu.createMenu("Technology", 'H');
		menuBar.add(technologyMenu);

		// setup the Tool menu
		Menu toolMenu = Menu.createMenu("Tool", 'T');
		menuBar.add(toolMenu);
		Menu drcSubMenu = Menu.createMenu("DRC", 'D');
		toolMenu.add(drcSubMenu);
		Menu simulationSubMenu = Menu.createMenu("Simulation", 'S');
		toolMenu.add(simulationSubMenu);
		Menu ercSubMenu = Menu.createMenu("ERC", 'E');
		toolMenu.add(ercSubMenu);
		Menu networkSubMenu = Menu.createMenu("Network", 'N');
		toolMenu.add(networkSubMenu);
		Menu logEffortSubMenu = Menu.createMenu("Logical Effort", 'L');
        logEffortSubMenu.addMenuItem("Analyze Cell", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { analyzeCellCommand(); }});
		toolMenu.add(logEffortSubMenu);
		Menu routingSubMenu = Menu.createMenu("Routing", 'R');
		toolMenu.add(routingSubMenu);
		Menu generationSubMenu = Menu.createMenu("Generation", 'G');
		toolMenu.add(generationSubMenu);
		Menu compactionSubMenu = Menu.createMenu("Compaction", 'C');
		toolMenu.add(compactionSubMenu);

		// setup the Help menu
		Menu helpMenu = Menu.createMenu("Help", 'H');
		menuBar.add(helpMenu);

		// setup Steve's test menu
		Menu steveMenu = Menu.createMenu("Steve", 'S');
		menuBar.add(steveMenu);
		steveMenu.addMenuItem("Get Info", KeyStroke.getKeyStroke('I', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { getInfoCommand(); } });
		steveMenu.addMenuItem("Show R-Tree", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { showRTreeCommand(); } });

		// setup Russell's test menu
		Menu russMenu = Menu.createMenu("Russell", 'R');
		menuBar.add(russMenu);
		russMenu.addMenuItem("ivanFlat", new com.sun.electric.rkao.IvanFlat());
		russMenu.addMenuItem("layout flat", new com.sun.electric.rkao.LayFlat());

		// setup Dima's test menu
		Menu dimaMenu = Menu.createMenu("Dima", 'D');
		menuBar.add(dimaMenu);
		dimaMenu.addMenuItem("redo Network Numbering", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { redoNetworkNumberingCommand(); } });
		dimaMenu.addMenuItem("test NodeInstsOterator", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { testNodeInstsIteratorCommand(); } });

        // setup JonGainsley's test menu
        Menu jongMenu = Menu.createMenu("JonG", 'J');
		menuBar.add(jongMenu);
        jongMenu.addMenuItem("Describe Vars", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { listVarsOnObject(); }});
        jongMenu.addMenuItem("Eval Vars", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { evalVarsOnObject(); }});
        jongMenu.addMenuItem("LE test1", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { LENetlister.test1(); }});
        jongMenu.addMenuItem("Open Purple Lib", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { openP4libCommand(); }});
            
		// return the menu bar
		return menuBar;
	}

	// ---------------------- THE FILE MENU -----------------

	/**
	 * Class to read a library in a new thread.
	 */
	private static class OpenBinLibraryThread extends Thread
	{
		private String fileName;

		OpenBinLibraryThread(String fileName) { this.fileName = fileName; }

		public void run()
		{
			Library lib = Input.readLibrary(fileName, Input.ImportType.BINARY);
			if (lib == null) return;
			Library.setCurrent(lib);
			Cell cell = lib.getCurCell();
			if (cell == null) System.out.println("No current cell in this library"); else
				WindowFrame.createEditWindow(cell);
		}
	}

	/**
	 * This routine implements the command to read a library.
	 */
	public static void openLibraryCommand()
	{
		String fileName = DialogOpenFile.ELIB.chooseInputFile(null);
		if (fileName != null)
		{
			// start a new thread to do the input
			OpenBinLibraryThread oThread = new OpenBinLibraryThread(fileName);
			oThread.start();
		}
	}

	/**
	 * Class to read a library in a new thread.
	 */
	private static class OpenTxtLibraryThread extends Thread
	{
		private String fileName;

		OpenTxtLibraryThread(String fileName) { this.fileName = fileName; }

		public void run()
		{
			Library lib = Input.readLibrary(fileName, Input.ImportType.TEXT);
			if (lib == null) return;
			Library.setCurrent(lib);
			Cell cell = lib.getCurCell();
			if (cell == null) System.out.println("No current cell in this library"); else
				WindowFrame.createEditWindow(cell);
		}
	}

	/**
	 * This routine implements the command to import a library (Readable Dump format).
	 */
	public static void importLibraryCommand()
	{
		String fileName = DialogOpenFile.TEXT.chooseInputFile(null);
		if (fileName != null)
		{
			// start a new thread to do the input
			OpenTxtLibraryThread oThread = new OpenTxtLibraryThread(fileName);
			oThread.start();
		}
	}

	/**
	 * This routine implements the command to save a library.
	 */
	public static void saveLibraryCommand()
	{
		Library lib = Library.getCurrent();
		String fileName;
		if (lib.isFromDisk())
		{
			fileName = lib.getLibFile();
		} else
		{
			fileName = DialogOpenFile.ELIB.chooseOutputFile(lib.getLibName()+".elib");
			if (fileName != null)
			{
				Library.Name n = Library.Name.newInstance(fileName);
				n.setExtension("elib");
				lib.setLibFile(n.makeName());
				lib.setLibName(n.getName());
			}
		}
		boolean error = Output.writeLibrary(lib, Output.ExportType.BINARY);
		if (error)
		{
			System.out.println("Error writing the library file");
		}
	}

	/**
	 * This routine implements the command to save a library to a different file.
	 */
	public static void saveAsLibraryCommand()
	{
		Library lib = Library.getCurrent();
		lib.clearFromDisk();
		saveLibraryCommand();
	}

	/**
	 * This routine implements the command to quit Electric.
	 */
	public static void quitCommand()
	{
		System.exit(0);
	}

	// ---------------------- THE EDIT MENU -----------------

	/**
	 * This routine implements the command to undo the last change.
	 */
	public static void undoCommand()
	{
		if (!Undo.undoABatch())
			System.out.println("Undo failed!");
	}

	/**
	 * This routine implements the command to redo the last change.
	 */
	public static void redoCommand()
	{
		if (!Undo.redoABatch())
			System.out.println("Redo failed!");
	}

	/**
	 * This routine implements the command to show the undo history.
	 */
	public static void showUndoListCommand()
	{
		Undo.showHistoryList();
	}

	// ---------------------- THE CELL MENU -----------------

    public static void downHierCommand() {
        EditWindow curEdit = TopLevel.getCurrentEditWindow();
        curEdit.downHierarchy();
    }
    
    public static void upHierCommand() {
        EditWindow curEdit = TopLevel.getCurrentEditWindow();
        curEdit.upHierarchy();
    }

	public static void showCellGroupsCommand()
	{
		// list things by cell group
		FlagSet cellFlag = NodeProto.getFlagSet(1);
		Library curLib = Library.getCurrent();
		for(Iterator it = curLib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			cell.clearBit(cellFlag);
		}
		for(Iterator it = curLib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			if (cell.isBit(cellFlag)) continue;

			// untouched: show whole cell group
			System.out.println("**** Cell group:");
			for(Iterator git = cell.getCellGroup().getCells(); git.hasNext(); )
			{
				Cell cellInGroup = (Cell)git.next();
				System.out.println("    " + cellInGroup.describe());
				cellInGroup.setBit(cellFlag);
			}
		}
		cellFlag.freeFlagSet();
	}

	// ---------------------- THE WINDOW MENU -----------------

	public static void fullDisplayCommand()
	{
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			for(Iterator cit = lib.getCells(); cit.hasNext(); )
			{
				Cell cell = (Cell)cit.next();
				for(Iterator nit = cell.getNodes(); nit.hasNext(); )
				{
					NodeInst ni = (NodeInst)nit.next();
					ni.setExpanded();
				}
			}
		}

		// get the current window
        EditWindow curEdit = TopLevel.getCurrentEditWindow();
		if (curEdit != null)
		{
			curEdit.setTimeTracking(true);
			curEdit.redraw();
		}
	}

	/**
	 * This routine implements the command to toggle the display of the grid.
	 */
	public static void toggleGridCommand()
	{
		EditWindow wnd = TopLevel.getCurrentEditWindow();
		if (wnd != null)
		{
			wnd.setGrid(!wnd.getGrid());
		}
	}

	// ---------------------- THE TOOLS MENU -----------------

    // Logical Effort Tool
    public static void analyzeCellCommand()
    {
        EditWindow curEdit = TopLevel.getCurrentEditWindow();
        if (curEdit == null) {
            System.out.println("Please select valid window first");
            return;
        }
        LENetlister.netlistAndSize(curEdit.getCell(), curEdit.getVarContext());
    }
    
    // ---------------------- THE STEVE MENU -----------------

	public static void getInfoCommand()
	{
		if (Highlight.getNumHighlights() == 0)
		{
			// information about the cell
			Cell c = Library.getCurrent().getCurCell();
			c.getInfo();
		} else
		{
			// information about the selected items
			for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() != Highlight.Type.GEOM) continue;
				Geometric geom = h.getGeom();
				geom.getInfo();
			}
		}
	}

	public static void showRTreeCommand()
	{
		Library curLib = Library.getCurrent();
		Cell curCell = curLib.getCurCell();
		System.out.println("Current cell is " + curCell.describe());
		if (curCell == null) return;
		curCell.getRTree().printRTree(0);
	}

	// ---------------------- THE DIMA MENU -----------------

	public static void redoNetworkNumberingCommand()
	{
		long startTime = System.currentTimeMillis();
		System.out.println("**** Renumber networks of layout cells");
		if (false)
		{
			int ncell = 0;
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				for(Iterator cit = lib.getCells(); cit.hasNext(); )
				{
					Cell cell = (Cell)cit.next();
					if (cell.getView() != View.LAYOUT) continue;
					ncell++;
					cell.rebuildNetworks(null);
				}
			}
		} else
		{
			Cell.rebuildAllNetworks(null);
		}
		long endTime = System.currentTimeMillis();
		float finalTime = (endTime - startTime) / 1000F;
		System.out.println("**** Renumber networks took " + finalTime + " seconds");
	}
    
	public static void testNodeInstsIteratorCommand()
	{
		int ncells, nnodes;
		long startTime, endTime;
		float finalTime;

		startTime = System.currentTimeMillis();
		nnodes = 0;
		for (int i = 0; i < 1000; i++)
		{
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				for(Iterator cit = lib.getCells(); cit.hasNext(); )
				{
					Cell cell = (Cell)cit.next();
					for (Iterator nit = cell.getNodes(); nit.hasNext();)
					{
						NodeInst ni = (NodeInst)nit.next();
						nnodes++;
					}
				}
			}
		}
		endTime = System.currentTimeMillis();
		finalTime = (endTime - startTime) / 1000F;
		System.out.println("**** getNodes() on "+nnodes+" nodes took " + finalTime + " seconds ("+
			(int)(finalTime/nnodes*1e9)+" nsec/node)");

		startTime = System.currentTimeMillis();
		nnodes = 0;
		for (int i = 0; i < 1000; i++)
		{
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				for(Iterator cit = lib.getCells(); cit.hasNext(); )
				{
					Cell cell = (Cell)cit.next();
					for (Iterator nit = cell.getNodesByUsage(); nit.hasNext();)
					{
						NodeInst ni = (NodeInst)nit.next();
						nnodes++;
					}
				}
			}
		}
		endTime = System.currentTimeMillis();
		finalTime = (endTime - startTime) / 1000F;
		System.out.println("**** getNodesByUsage() on "+nnodes+" nodes took " + finalTime + " seconds ("+
			(int)(finalTime/nnodes*1e9)+" nsec/node)");

		startTime = System.currentTimeMillis();
		nnodes = 0;
		for (int i = 0; i < 1000; i++)
		{
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				for(Iterator cit = lib.getCells(); cit.hasNext(); )
				{
					Cell cell = (Cell)cit.next();
					for (Iterator nit = cell.getUsagesIn(); nit.hasNext();)
					{
						NodeUsage nu = (NodeUsage)nit.next();
						nnodes++;
					}
				}
			}
		}
		endTime = System.currentTimeMillis();
		finalTime = (endTime - startTime) / 1000F;
		System.out.println("**** getNodeUsages() on "+nnodes+" node usages took " + finalTime + " seconds ("+
			(int)(finalTime/nnodes*1e9)+" nsec/node)");

		startTime = System.currentTimeMillis();
		ncells = 0;
		for (int i = 0; i < 10; i++)
		{
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				for(Iterator cit = lib.getCells(); cit.hasNext(); )
				{
					Cell cell = (Cell)cit.next();
					cell.setDirty();
					cell.getBounds();
					ncells++;
				}
			}
		}
		endTime = System.currentTimeMillis();
		finalTime = (endTime - startTime) / 1000F;
		System.out.println("**** getBounds() on "+ncells+" cells took " + finalTime + " seconds ("+
			(int)(finalTime/ncells*1e6)+" usec/cell)");

		startTime = System.currentTimeMillis();
		ncells = 0;
		for (int i = 0; i < 10; i++)
		{
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				for(Iterator cit = lib.getCells(); cit.hasNext(); )
				{
					Cell cell = (Cell)cit.next();
					cell.setDirty();
					cell.getBoundsByUsage();
					ncells++;
				}
			}
		}
		endTime = System.currentTimeMillis();
		finalTime = (endTime - startTime) / 1000F;
		System.out.println("**** getBoundsByUsage() on "+ncells+" cells took " + finalTime + " seconds ("+
			(int)(finalTime/ncells*1e6)+" usec/cell)");

		startTime = System.currentTimeMillis();
		ncells = 0;
		for (int i = 0; i < 10; i++)
		{
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				for(Iterator cit = lib.getCells(); cit.hasNext(); )
				{
					Cell cell = (Cell)cit.next();
					cell.setDirty();
					cell.getBoundsByArray();
					ncells++;
				}
			}
		}
		endTime = System.currentTimeMillis();
		finalTime = (endTime - startTime) / 1000F;
		System.out.println("**** getBoundsByArray() on "+ncells+" cells took " + finalTime + " seconds ("+
			(int)(finalTime/ncells*1e6)+" usec/cell)");
	}
    
    // ---------------------- THE JON GAINSLEY MENU -----------------
    
    public static void listVarsOnObject() {
        if (Highlight.getNumHighlights() == 0) {
            System.out.println("Nothing highlighted");
            return;
        }
        for (Iterator it = Highlight.getHighlights(); it.hasNext();) {
            Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.GEOM) continue;
            Geometric geom = (Geometric)h.getGeom();
            geom.getInfo();
        }
    }
    
    public static void evalVarsOnObject() {
        EditWindow curEdit = TopLevel.getCurrentEditWindow();
        if (Highlight.getNumHighlights() == 0) {
            System.out.println("Nothing highlighted");
            return;
        }
        for (Iterator it = Highlight.getHighlights(); it.hasNext();) {
            Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.GEOM) continue;
            ElectricObject eobj = (ElectricObject)h.getGeom();
            Iterator itVar = eobj.getVariables();
            while(itVar.hasNext()) {
                Variable var = (Variable)itVar.next();
                Object obj = curEdit.getVarContext().evalVar(var);
                System.out.print(var.getName().getName() + ": ");
                System.out.println(obj);
            }
        }
    }
    
    public static void openP4libCommand() {
        OpenBinLibraryThread oThread = new OpenBinLibraryThread("/export/gainsley/soesrc_java/test/purpleFour.elib");
        oThread.start();
    }
    
    
}
