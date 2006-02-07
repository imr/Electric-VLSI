/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CheckOutJob.java
 * Project management tool
 * Written by: Steven M. Rubin
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.project;

import com.sun.electric.database.geometry.GenMath.MutableInteger;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class to check-out cells from the Project Management repository.
 */
public class CheckOutJob extends Job
{
	private List<Cell> checkOutCells;
	private ProjectDB pdb;
	private DisplayedCells displayedCells;
	private boolean autoCheckout;

	/**
	 * Method to check the currently edited cell out of the repository.
	 */
	public static void checkOutThisCell()
	{
		UserInterface ui = Job.getUserInterface();
		Cell cell = ui.needCurrentCell();
		if (cell == null) return;
		checkOut(cell);
	}

	/**
	 * Method to check a cell out of the repository.
	 * @param cell the Cell to check-out.
	 */
	public static void checkOut(Cell cell)
	{	
		// make sure there is a valid user name and repository
		if (Users.needUserName()) return;
		if (Project.ensureRepository()) return;
	
		// make a list of just this cell
		List<Cell> oneCell = new ArrayList<Cell>();
		oneCell.add(cell);
	
		new CheckOutJob(oneCell, false);
	}

	/**
	 * Constructor to make a "check out cells" job.
	 * @param checkOutCells the List of cells to check out.
	 * @param autoCheckout true if the checkout is automatically done to allow a change.
	 * False if the checkout was explicitly requested by the user.
	 */
	CheckOutJob(List<Cell> checkOutCells, boolean autoCheckout)
	{
		super("Check out cells", Project.getProjectTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
		this.checkOutCells = checkOutCells;
		this.autoCheckout = autoCheckout;
		pdb = Project.projectDB;

		// save the current window configuration
		displayedCells = new DisplayedCells();
		displayedCells.setCellsToBeChanged(checkOutCells);

		startJob();
	}

	public boolean doIt() throws JobException
	{
		// make a set of project libraries that are affected
		Set<ProjectLibrary> projectLibs = new HashSet<ProjectLibrary>();
		for(Cell oldVers : checkOutCells)
		{
			Library lib = oldVers.getLibrary();
			ProjectLibrary pl = pdb.findProjectLibrary(lib);
			projectLibs.add(pl);
		}

		// lock access to the project files (throws JobException on error)
		ProjectLibrary.lockManyProjectFiles(projectLibs);

		// check out the cell
		try
		{
			preCheckOutCells(pdb, checkOutCells);
		} catch (JobException e)
		{
			ProjectLibrary.releaseManyProjectFiles(projectLibs);
			throw e;
		}

		// prevent tools (including this one) from seeing the changes
		Project.setChangeStatus(true);

		// make new version
		for(Cell oldVers : checkOutCells)
		{
			// change version information (throws JobException on error)
			Cell newVers = bumpVersion(oldVers);		// CHANGES DATABASE
			if (newVers != null)
			{
				// update records for the changed cells
	        	bumpRecordVersions(pdb, oldVers, newVers);

	        	// record that cells changed so that displays get updated
	        	displayedCells.swap(oldVers, newVers);
			}
		}
		Project.setChangeStatus(false);

		ProjectLibrary.releaseManyProjectFiles(projectLibs);

		fieldVariableChanged("pdb");
		fieldVariableChanged("displayedCells");
		return true;
	}

    public void terminateOK()
    {
    	// take the new version of the project database from the server
    	Project.projectDB = pdb;

    	// redisplay windows to show current versions
    	displayedCells.updateWindows();

		// update explorer tree
		WindowFrame.wantToRedoLibraryTree();

		if (!autoCheckout)
		{
			// print dependencies and display
			Iterator<Cell> createdCells = displayedCells.getCreatedCells();
			if (createdCells.hasNext())
			{
				Cell newVers = createdCells.next();
				StringBuffer cellNames = new StringBuffer();
				int numCells = 0;
				for(Iterator<Cell> it = displayedCells.getCreatedCells(); it.hasNext(); )
				{
					Cell c = it.next();
					if (cellNames.length() > 0) cellNames.append(", ");
					cellNames.append(c.describe(false));
					numCells++;
				}
				if (numCells > 1) System.out.println("Cells " + cellNames + " checked out for your use"); else
					System.out.println("Cell " + cellNames + " checked out for your use");

				// advise of possible problems with other checkouts higher up in the hierarchy
				HashMap<Cell,MutableInteger> cellsMarked = new HashMap<Cell,MutableInteger>();
				for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
				{
					Library oLib = it.next();
					for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
					{
						Cell cell = cIt.next();
						cellsMarked.put(cell, new MutableInteger(0));
					}
				}
				MutableInteger miNewVers = (MutableInteger)cellsMarked.get(newVers);
				miNewVers.setValue(1);
				boolean propagated = true;
				while (propagated)
				{
					propagated = false;
					for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
					{
						Library oLib = it.next();
						for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
						{
							Cell cell = cIt.next();
							MutableInteger val = (MutableInteger)cellsMarked.get(cell);
							if (val.intValue() == 1)
							{
								propagated = true;
								val.setValue(2);
								for(Iterator<NodeInst> nIt = cell.getInstancesOf(); nIt.hasNext(); )
								{
									NodeInst ni = nIt.next();
									MutableInteger pVal = (MutableInteger)cellsMarked.get(ni.getParent());
									if (pVal.intValue() == 0) pVal.setValue(1);
								}
							}
						}
					}
				}
				miNewVers.setValue(0);
				int total = 0;
				for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
				{
					Library oLib = it.next();
					for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
					{
						Cell cell = cIt.next();
						MutableInteger val = (MutableInteger)cellsMarked.get(cell);
						if (val.intValue() == 0) continue;
						if (Project.getCellStatus(cell) == Project.CHECKEDOUTTOOTHERS)
						{
							val.setValue(3);
							total++;
						}
					}
				}
				if (total != 0)
				{
					System.out.println("*** Warning: the following cells are above this in the hierarchy");
					System.out.println("*** and are checked out to others.  This may cause problems");
					for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
					{
						Library oLib = it.next();
						for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
						{
							Cell cell = cIt.next();
							MutableInteger val = (MutableInteger)cellsMarked.get(cell);
							if (val.intValue() != 3) continue;
							System.out.println("    " + cell + " is checked out to " + Project.getCellOwner(cell));
						}
					}
				}

				// advise of possible problems with other checkouts lower down in the hierarchy
				for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
				{
					Library oLib = it.next();
					for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
					{
						Cell cell = cIt.next();
						MutableInteger val = (MutableInteger)cellsMarked.get(cell);
						val.setValue(0);
					}
				}
				miNewVers.setValue(1);
				propagated = true;
				while(propagated)
				{
					propagated = false;
					for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
					{
						Library oLib = it.next();
						for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
						{
							Cell cell = cIt.next();
							MutableInteger val = (MutableInteger)cellsMarked.get(cell);
							if (val.intValue() == 1)
							{
								propagated = true;
								val.setValue(2);
								for(Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
								{
									NodeInst ni = nIt.next();
									if (!ni.isCellInstance()) continue;
									MutableInteger subVal = (MutableInteger)cellsMarked.get(ni.getProto());
									if (subVal.intValue() == 0) subVal.setValue(1);
								}
							}
						}
					}
				}
				miNewVers.setValue(0);
				total = 0;
				for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
				{
					Library oLib = it.next();
					for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
					{
						Cell cell = cIt.next();
						MutableInteger val = (MutableInteger)cellsMarked.get(cell);
						if (val.intValue() == 0) continue;
						String owner = Project.getCellOwner(cell);
						if (owner.length() == 0) continue;
						if (!owner.equals(Project.getCurrentUserName()))
						{
							val.setValue(3);
							total++;
						}
					}
				}
				if (total != 0)
				{
					System.out.println("*** Warning: the following cells are below this in the hierarchy");
					System.out.println("*** and are checked out to others.  This may cause problems");
					for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
					{
						Library oLib = it.next();
						for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
						{
							Cell cell = cIt.next();
							MutableInteger val = (MutableInteger)cellsMarked.get(cell);
							if (val.intValue() != 3) continue;
							String owner = Project.getCellOwner(cell);
							System.out.println("    " + cell + " is checked out to " + owner);
						}
					}
				}
			}
		}
    }

//	/**
//	 * This class checks out cells from Project Management to allow changes that have been made.
//	 */
//	static class AutoCheckoutJob extends Job
//	{
//		private List<Cell> cellsThatChanged;
//		private ProjectDB pdb;
//		private DisplayedCells displayedCells;
//
//		AutoCheckoutJob(List<Cell> cellsThatChanged)
//		{
//			super("Check out locked cells to allow changes", Project.getProjectTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
//			this.cellsThatChanged = cellsThatChanged;
//			pdb = Project.projectDB;
//
//			// save the current window configuration
//			displayedCells = new DisplayedCells();
//			displayedCells.setCellsToBeChanged(cellsThatChanged);
//			startJob();
//		}
//
//		public boolean doIt() throws JobException
//		{
//			// make a set of project libraries that are affected
//			Set<ProjectLibrary> projectLibs = new HashSet<ProjectLibrary>();
//			for(Cell oldVers : cellsThatChanged)
//			{
//				Library lib = oldVers.getLibrary();
//				ProjectLibrary pl = pdb.findProjectLibrary(lib);
//				projectLibs.add(pl);
//			}
//
//			// lock access to the project files (throws JobException on error)
//			ProjectLibrary.lockManyProjectFiles(projectLibs);
//
//			// check out the cell
//			try
//			{
//				preCheckOutCells(pdb, cellsThatChanged);
//			} catch (JobException e)
//			{
//				ProjectLibrary.releaseManyProjectFiles(projectLibs);
//				throw e;
//			}
//
//			// prevent tools (including this one) from seeing the changes
//			Project.setChangeStatus(true);
//
//			// make new version
//			for(Cell oldVers : cellsThatChanged)
//			{
//				// change version information (throws JobException on error)
//				Cell newVers = bumpVersion(oldVers);		// CHANGES DATABASE
//				if (newVers != null)
//				{
//					// update records for the changed cells
//		        	bumpRecordVersions(pdb, oldVers, newVers);
//
//		        	// record that cells changed so that displays get updated
//		        	displayedCells.swap(oldVers, newVers);
//				}
//			}
//
//			Project.setChangeStatus(false);
//
//			ProjectLibrary.releaseManyProjectFiles(projectLibs);
//
//			fieldVariableChanged("pdb");
//			fieldVariableChanged("displayedCells");
//			return true;
//		}
//
//		public void terminateIt(Throwable je)
//        {
//	    	// take the new version of the project database from the server
//	    	Project.projectDB = pdb;
//
//	    	// redisplay windows to show current versions
//	    	displayedCells.updateWindows();
//
//			// update explorer tree
//			WindowFrame.wantToRedoLibraryTree();
//        }
//	}

	/**
	 * Method to "bump" the version of a Cell by duplicating it.
	 * The cell then has a new version number.  The new Cell replaces
	 * the old Cell, and the old one is deleted.
	 * @param oldVers the old Cell.
	 * @return the new Cell (null on error).
	 */
	private static Cell bumpVersion(Cell oldVers)
		throws JobException
	{
		Library lib = oldVers.getLibrary();
		Cell newVers = Cell.copyNodeProto(oldVers, lib, oldVers.getName(), true);
		if (newVers == null)
			throw new JobException("Error making new version of cell " + oldVers.describe(false));

		// replace former usage with new version
		if (Project.useNewestVersion(oldVers, newVers))		// CHANGES DATABASE
			throw new JobException("Error replacing instances of cell " + oldVers.describe(false));
		Project.markLocked(newVers, false);		// CHANGES DATABASE
		lib.setChanged();
		return newVers;
	}

	/**
	 * Method to update the project databases to account for cell replacements.
	 * @param newCells a map from old cells to new cells.
	 */
	private static void bumpRecordVersions(ProjectDB pdb, Cell oldVers, Cell newVers)
	{
		// find the old ProjectCell
		ProjectLibrary pl = pdb.findProjectLibrary(oldVers.getLibrary());
		ProjectCell oldPC = pl.findProjectCell(oldVers);

		// make the new ProjectCell
		ProjectCell newPC = new ProjectCell(newVers, pl);
		newPC.setLibType(oldPC.getLibType());
		newPC.setComment("CHECKED OUT");
		newPC.setOwner(Project.getCurrentUserName());

		pl.linkProjectCellToCell(oldPC, null);
		pl.linkProjectCellToCell(newPC, newVers);
	}

	/**
	 * Method to check out a list of Cells.
	 * @param cellsToCheckOut the List of Cells to check out.
	 * Throws JobException on error.
	 */
	private static void preCheckOutCells(ProjectDB pdb, List<Cell> cellsToCheckOut)
		throws JobException
	{
		// examine each cell being checked out
		for(Cell oldVers : cellsToCheckOut)
		{
			// see if there is a newer version of a cell
			ProjectLibrary pl = pdb.findProjectLibrary(oldVers.getLibrary());
			ProjectCell newestProjectCell = null;
			for(Iterator<ProjectCell> it = pl.getProjectCells(); it.hasNext(); )
			{
				ProjectCell pc = it.next();
				if (pc.getCellName().equals(oldVers.getName()) && pc.getView() == oldVers.getView())
				{
					if (pc.getVersion() > oldVers.getVersion())
					{
						if (newestProjectCell == null || newestProjectCell.getVersion() < pc.getVersion())
							newestProjectCell = pc;
					}
				}
			}
			if (newestProjectCell != null)
			{
				if (newestProjectCell.getOwner().length() == 0)
				{
					throw new JobException(
						"A more recent version of cell " + oldVers.describe(false) + " is in the repository.  Do an update first.");
				}
				if (newestProjectCell.getOwner().equals(Project.getCurrentUserName()))
				{
					throw new JobException(
						"You already checked-out cell " + oldVers.describe(false) + ", but the changes are not in the current library.  Do an update first.");
				} else
				{
					throw new JobException(
						"Cannot check-out cell " + oldVers.describe(false) + ".  It is checked-out to '" + newestProjectCell.getOwner() + "'");
				}
			}

			// find this cell in the project file
			ProjectCell pc = pl.findProjectCell(oldVers);
			if (pc == null)
			{
				throw new JobException(
					"Cell " + oldVers.describe(false) + " is not in the project.  You must add it to the project before being able to check it out and in.");
			}

			// see if it is available
			if (pc.getOwner().length() != 0)
			{
				if (pc.getOwner().equals(Project.getCurrentUserName()))
				{
					Project.markLocked(oldVers, false);		// CHANGES DATABASE
					throw new JobException(
						"Cell " + oldVers.describe(false) + " is already checked out to you.");
				} else
				{
					throw new JobException(
						"Cannot check cell " + oldVers.describe(false) + " out because it is already checked out to '" + pc.getOwner() + "'");
				}
			}

			// make sure we have the latest version
			if (pc.getVersion() > oldVers.getVersion())
			{
				throw new JobException(
					"Cannot check out cell " + oldVers.describe(false) +
					" because you don't have the latest version (yours is " + oldVers.getVersion() + ", project has " +
					pc.getVersion() + ").  Do an 'update' first");
			}
		}
	}

}
