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
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.user.ui.UIEdit;
import com.sun.electric.tool.user.ui.UIEditFrame;
import com.sun.electric.tool.user.ui.UIDialogOpenFile;
import com.sun.electric.tool.user.ui.UITopLevel;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.io.Input;
import com.sun.electric.tool.io.Output;

import java.util.Iterator;

/**
 * This class has all of the pulldown menu commands in Electric.
 */
public final class UserMenuCommands
{
	// ---------------------- private and protected methods -----------------

	// It is never useful for anyone to create an instance of this class
	private UserMenuCommands()
	{
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
				UIEditFrame.CreateEditWindow(cell);
		}
	}

	/**
	 * This routine implements the command to read a library.
	 */
	public static void openLibraryCommand()
	{
		String fileName = UIDialogOpenFile.ELIB.chooseInputFile(null);
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
				UIEditFrame.CreateEditWindow(cell);
		}
	}

	/**
	 * This routine implements the command to import a library (Readable Dump format).
	 */
	public static void importLibraryCommand()
	{
		String fileName = UIDialogOpenFile.TEXT.chooseInputFile(null);
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
			fileName = UIDialogOpenFile.ELIB.chooseOutputFile(lib.getLibName()+".elib");
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

	// ---------------------- THE STEVE MENU -----------------

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
		UIEditFrame uif = UITopLevel.getCurrent();
		if (uif != null)
		{
			uif.setTimeTracking(true);
			uif.redraw();
		}
	}

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

	/**
	 * This routine implements the command to toggle the display of the grid.
	 */
	public static void toggleGridCommand()
	{
		UIEditFrame uif = UITopLevel.getCurrent();
		if (uif != null)
		{
			uif.setGrid(!uif.getGrid());
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

	/**
	 * This routine implements the command to show the undo history.
	 */
	public static void showUndoListCommand()
	{
		Undo.showHistoryList();
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
					for (Iterator nit = cell.getNodesDirect(); nit.hasNext();)
					{
						NodeInst ni = (NodeInst)nit.next();
						nnodes++;
					}
				}
			}
		}
		endTime = System.currentTimeMillis();
		finalTime = (endTime - startTime) / 1000F;
		System.out.println("**** getNodesDirect() on "+nnodes+" nodes took " + finalTime + " seconds ("+
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
					cell.getBoundsDirect();
					ncells++;
				}
			}
		}
		endTime = System.currentTimeMillis();
		finalTime = (endTime - startTime) / 1000F;
		System.out.println("**** getBoundsDirect() on "+ncells+" cells took " + finalTime + " seconds ("+
			(int)(finalTime/ncells*1e6)+" usec/cell)");
	}
    
    // ---------------------- THE JON GAINSLEY MENU -----------------

    public static void downHierCommand() {
        UIEditFrame curFrame = UITopLevel.getCurrent();
        UIEdit curEdit = curFrame.getEdit();
        curEdit.downHierarchy();
    }
    
    public static void upHierCommand() {
        UIEditFrame curFrame = UITopLevel.getCurrent();
        UIEdit curEdit = curFrame.getEdit();
        curEdit.upHierarchy();
    }
    
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
        UIEditFrame curFrame = UITopLevel.getCurrent();
        UIEdit curEdit = curFrame.getEdit();
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
}
