/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DeleteCellJob.java
 * Project management tool: Delete a cell from the Project Management repository
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
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * This class deletes a cell from the Project Management repository.
 */
public class DeleteCellJob extends Job
{
	private ProjectDB pdb;
	private Cell cell;

	/**
	 * Method to remove the currently edited cell from the repository.
	 */
	public static void removeThisCell()
	{
		UserInterface ui = Job.getUserInterface();
		Cell cell = ui.needCurrentCell();
		if (cell == null) return;
		removeCell(cell);
	}

	/**
	 * Method to remove a cell from the repository.
	 */
	public static void removeCell(Cell cell)
	{
		// make sure there is a valid user name and repository
		if (Users.needUserName()) return;
		if (Project.ensureRepository()) return;

		// make sure the cell is not being used
		HashSet<Cell> markedCells = new HashSet<Cell>();
		for(Iterator<NodeInst> it = cell.getInstancesOf(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			Cell parent = ni.getParent();
			int status = Project.getCellStatus(parent);
			if (status == Project.NOTMANAGED || status == Project.OLDVERSION) continue;
			markedCells.add(ni.getParent());
		}
		StringBuffer err = new StringBuffer();
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library oLib = it.next();
			for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
			{
				Cell oCell = cIt.next();
				if (markedCells.contains(oCell))
				{
					if (err.length() > 0) err.append(", ");
					err.append(oCell.describe(true));
				}
			}
		}
		if (markedCells.size() > 0)
		{
			Job.getUserInterface().showErrorMessage("Cannot delete " + cell + " because it is still being used by: " +
				err.toString(), "Delete Cell Error");
			return;
		}

		// make sure the user has no cells checked-out
		boolean youOwn = false;
		Library lib = cell.getLibrary();
		ProjectLibrary pl = Project.projectDB.findProjectLibrary(lib);
		for(Iterator<ProjectCell> it = pl.getProjectCells(); it.hasNext(); )
		{
			ProjectCell pc = it.next();
			if (pc.getOwner().equals(Project.getCurrentUserName())) { youOwn = true;   break; }
		}
		if (youOwn)
		{
			StringBuffer infstr = new StringBuffer();
			for(Iterator<ProjectCell> it = pl.getProjectCells(); it.hasNext(); )
			{
				ProjectCell pc = it.next();
				if (!pc.getOwner().equals(Project.getCurrentUserName())) continue;
				if (infstr.length() > 0) infstr.append(", ");
				infstr.append(pc.describe());
			}
			Job.getUserInterface().showErrorMessage("Before deleting a cell from the repository, you must check-in all of your work. " +
				"This is because the deletion may be dependent upon changes recently made. " +
				"These cells are checked out to you: " + infstr.toString(), "Cell Deletion Error");
			return;
		}

		boolean found = false;
		for(Iterator<ProjectCell> it = pl.getProjectCells(); it.hasNext(); )
		{
			ProjectCell pc = it.next();
			if (pc.getCellName().equals(cell.getName()) && pc.getView() == cell.getView())
			{
				found = true;
				break;
			}
		}
		if (!found)
		{
			Job.getUserInterface().showErrorMessage("This cell is not in the repository", "Cell Deletion Error");
			return;
		}

		new DeleteCellJob(cell);
	}


	private DeleteCellJob(Cell cell)
	{
		super("Delete cell", Project.getProjectTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
		this.pdb = Project.projectDB;
		this.cell = cell;
		startJob();
	}

	public boolean doIt() throws JobException
	{
		// find out which cell is being deleted
		Library lib = cell.getLibrary();
		ProjectLibrary pl = pdb.findProjectLibrary(lib);

		// lock access to the project files (throws JobException on error)
		pl.lockProjectFile();

		// find this in the project file
		List<ProjectCell> copyList = new ArrayList<ProjectCell>();
		for(Iterator<ProjectCell> it = pl.getProjectCells(); it.hasNext(); )
			copyList.add(it.next());
		for(ProjectCell pc : copyList)
		{
			if (pc.getCellName().equals(cell.getName()) && pc.getView() == cell.getView())
			{
				// unlink it
				pl.removeProjectCell(pc);

				// disable change broadcast
				Project.setChangeStatus(true);

				// mark this cell unlocked
				Project.markLocked(cell, false);		// CHANGES DATABASE

				// restore change broadcast
				Project.setChangeStatus(false);
				System.out.println("Cell " + cell.describe(true) + " deleted from the repository");
			}
		}

		// relase project file lock
		pl.releaseProjectFileLock(true);

		fieldVariableChanged("pdb");
		return true;
	}

    public void terminateOK()
    {
    	// take the new version of the project database from the server
    	Project.projectDB = pdb;

    	// update explorer tree
    	WindowFrame.wantToRedoLibraryTree();
    }

}
