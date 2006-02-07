/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CancelCheckOutJob.java
 * Project management tool: Cancel the checkout of a cell
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class cancels the checkout of a cell from the Project Management repository.
 */
public class CancelCheckOutJob extends Job
{
	private ProjectCell cancelled, former;
	private ProjectDB pdb;
	private DisplayedCells displayedCells;

	/**
	 * Method to cancel the check-out of the currently edited cell.
	 */
	public static void cancelCheckOutThisCell()
	{
		UserInterface ui = Job.getUserInterface();
		Cell cell = ui.needCurrentCell();
		if (cell == null) return;
		cancelCheckOut(cell);
	}

	/**
	 * Method to cancel the check-out of a cell.
	 * @param cell the Cell whose check-out should be cancelled.
	 */
	public static void cancelCheckOut(Cell cell)
	{
		// make sure there is a valid user name and repository
		if (Users.needUserName()) return;
		if (Project.ensureRepository()) return;
	
		boolean response = Job.getUserInterface().confirmMessage(
			"Cancel all changes to the checked-out " + cell + " and revert to the checked-in version?");
		if (!response) return;
	
		ProjectCell cancelled = null;
		ProjectCell former = null;
		Library lib = cell.getLibrary();
		ProjectLibrary pl = Project.projectDB.findProjectLibrary(lib);
		for(Iterator<ProjectCell> it = pl.getProjectCells(); it.hasNext(); )
		{
			ProjectCell pc = it.next();
			if (pc.getCellName().equals(cell.getName()) && pc.getView() == cell.getView())
			{
				if (pc.getVersion() >= cell.getVersion())
				{
					if (pc.getOwner().length() > 0)
					{
						if (pc.getOwner().equals(Project.getCurrentUserName()))
						{
							cancelled = pc;
						} else
						{
							pl.releaseProjectFileLock(true);
							Job.getUserInterface().showErrorMessage(
								"This cell is not checked out to you.  Only user '" + pc.getOwner() + "' can cancel the check-out.",
								"Cannot Cancel Checkout");
							return;
						}
					}
				} else
				{
					// find most recent former version
					if (former != null && former.getVersion() < pc.getVersion()) former = null;
					if (former == null) former = pc;
				}
			}
		}
	
		if (cancelled == null)
		{
			pl.releaseProjectFileLock(true);
			Job.getUserInterface().showErrorMessage("This cell is not checked out.",
				"Cannot Cancel Checkout");
			return;
		}
	
		if (former == null)
		{
			pl.releaseProjectFileLock(true);
			Job.getUserInterface().showErrorMessage("Cannot find former version to restore.",
				"Cannot Cancel Checkout");
			return;
		}
	
		new CancelCheckOutJob(cancelled, former);
	}

	private CancelCheckOutJob(ProjectCell cancelled, ProjectCell former)
	{
		super("Cancel Check-out", Project.getProjectTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
		this.cancelled = cancelled;
		this.former = former;

		this.pdb = Project.projectDB;

		// save the current window configuration
		displayedCells = new DisplayedCells();
		ArrayList<Cell> justOne = new ArrayList<Cell>();
		justOne.add(cancelled.getCell());
		displayedCells.setCellsToBeChanged(justOne);
		startJob();
	}

	public boolean doIt() throws JobException
	{
		ProjectLibrary pl = cancelled.getProjectLibrary();

		// lock access to the project files (throws JobException on error)
		pl.lockProjectFile();

		// prevent tools (including this one) from seeing the change
		Project.setChangeStatus(true);

		// pull in the former cell from the repository
		Project.getCellFromRepository(pdb, former, pl.getLibrary(), false, false);		// CHANGES DATABASE

		Cell oldCell = cancelled.getCell();
		Cell newCell = former.getCell();
		if (newCell == null)
		{
			Project.setChangeStatus(false);
			pl.releaseProjectFileLock(true);
			throw new JobException("Error bringing in former version (" + former.getVersion() + ")");
		}

		if (Project.useNewestVersion(oldCell, newCell))		// CHANGES DATABASE
		{
			Project.setChangeStatus(false);
			pl.releaseProjectFileLock(true);
			throw new JobException("Error replacing instances of former " + oldCell);
		}

		// record that cells changed so that displays get updated
    	displayedCells.swap(oldCell, newCell);
//System.out.println("BEFORE:====================");
//for(Iterator<ProjectCell> it = pl.getProjectCells(); it.hasNext(); )
//{
//	ProjectCell c = it.next();
//	System.out.println("PC="+c.getCellName()+";"+c.getVersion()+"{"+c.getView().getAbbreviation()+"}");
//}
		pl.removeProjectCell(cancelled);
		if (cancelled.getCell() != null)
		{
			Project.markLocked(cancelled.getCell(), true);		// CHANGES DATABASE
		}
		former.setLatestVersion(true);

		// restore change broadcast
		Project.setChangeStatus(false);

		// relase project file lock
		pl.releaseProjectFileLock(true);

		fieldVariableChanged("pdb");
		fieldVariableChanged("displayedCells");
		return true;
	}

    public void terminateOK()
    {
    	// redisplay windows to show current versions
    	displayedCells.updateWindows();

    	// update explorer tree
    	WindowFrame.wantToRedoLibraryTree();
    }

}
