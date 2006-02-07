/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AddCellJob.java
 * Project management tool: Add a cell to the Project Management repository
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

/**
 * This class adds a cell to the Project Management repository.
 */
public class AddCellJob extends Job
{
	private ProjectDB pdb;
	private Cell cell;

	/**
	 * Method to add the currently edited cell to the repository.
	 */
	public static void addThisCell()
	{
		UserInterface ui = Job.getUserInterface();
		Cell cell = ui.needCurrentCell();
		if (cell == null) return;
		addCell(cell);
	}

	/**
	 * Method to add a cell to the repository.
	 */
	public static void addCell(Cell cell)
	{
		// make sure there is a valid user name and repository
		if (Users.needUserName()) return;
		if (Project.ensureRepository()) return;

		if (cell.getNewestVersion() != cell)
		{
			Job.getUserInterface().showErrorMessage("Cannot add an old version of the cell", "Add Cell Error");
			return;
		}

		Library lib = cell.getLibrary();
		ProjectLibrary pl = Project.projectDB.findProjectLibrary(lib);
		ProjectCell foundPC = pl.findProjectCellByNameView(cell.getName(), cell.getView());
		if (foundPC != null)
		{
			Job.getUserInterface().showErrorMessage("This cell is already in the repository", "Error Adding to Repository");
			return;
		}
		new AddCellJob(cell);
	}

	private AddCellJob(Cell cell)
	{
		super("Add " + cell, Project.getProjectTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
		this.cell = cell;
		this.pdb = Project.projectDB;
		startJob();
	}

	public boolean doIt() throws JobException
	{
		Library lib = cell.getLibrary();
		ProjectLibrary pl = pdb.findProjectLibrary(lib);

		// lock access to the project files (throws JobException on error)
		pl.lockProjectFile();

		// prevent tools (including this one) from seeing the change
		Project.setChangeStatus(true);

		// create new entry for this cell
		ProjectCell pc = new ProjectCell(cell, pl);
		pc.setLastOwner(Project.getCurrentUserName());
		pc.setComment("Initial checkin");

		String error = null;
		if (Project.writeCell(cell, pc))		// CHANGES DATABASE
		{
			error = "Error writing the cell to the repository";
		} else
		{
			// link it in
			pl.linkProjectCellToCell(pc, cell);

			// mark this cell "checked in" and locked
			Project.markLocked(cell, true);		// CHANGES DATABASE

			System.out.println("Cell " + cell.describe(true) + " added to the project");
		}

		// restore change broadcast
		Project.setChangeStatus(false);

		// relase project file lock
		pl.releaseProjectFileLock(true);

		if (error != null) throw new JobException(error);
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
