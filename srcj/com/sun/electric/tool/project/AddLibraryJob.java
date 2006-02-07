/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AddLibraryJob.java
 * Project management tool: Add a library to the Project Management repository
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
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This class adds the current library to the Project Management repository.
 */
public class AddLibraryJob extends Job
{
	private Set<Library> libList;
	private ProjectDB pdb;

	/**
	 * Method to add the current library to the repository.
	 */
	public static void addThisLibrary()
	{
		addLibrary(Library.getCurrent());
	}

	/**
	 * Method to add a library to the repository.
	 * Finds dependent libraries and asks if they, too, should be added.
	 * @param lib the Library to add to the repository.
	 */
	public static void addLibrary(Library lib)
	{
		// make sure there is a valid user name and repository
		if (Users.needUserName()) return;
		if (Project.ensureRepository()) return;

		// verify that project is to be built
		boolean response = Job.getUserInterface().confirmMessage(
			"Are you sure you want to enter this library into the repository?");
		if (!response) return;

		// prepare a list of libraries to add to the repository
		Set<Library> yesList = new HashSet<Library>();
		yesList.add(lib);

		// include dependent libraries
		Set<Library> noList = new HashSet<Library>();
		includeDependentLibraries(lib, yesList, noList, false);

		// add them to the repository
		new AddLibraryJob(yesList);
	}

	/**
	 * Method to add all libraries to the repository.
	 */
	public static void addAllLibraries()
	{
		// make sure there is a valid user name and repository
		if (Users.needUserName()) return;
		if (Project.ensureRepository()) return;

		if (Project.getRepositoryLocation().length() == 0)
		{
			Job.getUserInterface().showInformationMessage(
				"Before entering libraries, set a repository location in the 'Project Management' tab under General Preferences",
				"Must Setup Project Management");
			return;
		}

		// verify that projects are to be built
		boolean response = Job.getUserInterface().confirmMessage(
			"Are you sure you want to enter libraries into the repository?");
		if (!response) return;

		Set<Library> libList = new HashSet<Library>();
		for(Iterator<Library>it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = it.next();
			if (lib.isHidden()) continue;
			if (Project.isLibraryManaged(lib)) continue;
			libList.add(lib);
		}
		new AddLibraryJob(libList);
	}

	private AddLibraryJob(Set<Library> libList)
	{
		super("Add Library", Project.getProjectTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
		this.libList = libList;
		this.pdb = Project.projectDB;
		startJob();
	}

	public boolean doIt() throws JobException
	{
		for(Library aLib : libList)
		{
			ProjectLibrary pl = pdb.findProjectLibrary(aLib);
			if (!pl.isEmpty()) continue;

			addLibraryNow(aLib, pl);		// CHANGES DATABASE
		}
		fieldVariableChanged("pdb");
		return true;
	}

    public void terminateOK()
    {
    	// take the new version of the project database from the server
    	Project.projectDB = pdb;

		// update explorer tree
		WindowFrame.wantToRedoLibraryTree();

    	// advise the user of these libraries
		Job.getUserInterface().showInformationMessage(
			"Libraries have been checked-into the repository and marked appropriately.",
			"Libraries Added");
    }

	/**
	 * Method to recursively check library contents to make sure all dependent libraries are
	 * considered for inclusion in the repository.
	 * @param lib the Library being added to the repository.
	 * @param yesList a set of libraries that will be added.
	 * @param noList a set of libraries that will not be added.
	 * @param yesToAll true if dependent libraries should be included without asking.
	 */
	private static void includeDependentLibraries(Library lib, Set<Library>yesList, Set<Library>noList, boolean yesToAll)
	{
		// see if dependent libraries should also be added
		for(Iterator<Cell>it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			for(Iterator<NodeInst>nIt = cell.getNodes(); nIt.hasNext(); )
			{
				NodeInst ni = nIt.next();
				if (ni.isCellInstance())
				{
					Cell subCell = (Cell)ni.getProto();
					Library oLib = subCell.getLibrary();
					if (oLib == lib) continue;

					// external library found: see if it is in the "yes" or "no" lists
					if (yesList.contains(oLib)) continue;
					if (noList.contains(oLib)) continue;

					// see if it is already managed
					if (Project.isLibraryManaged(oLib)) continue;

					// ask the user if this library should also be added to the repository
					int ret = 0;
					if (!yesToAll)
					{
						String [] options = {"Yes", "No", "Yes to All"};
						ret = Job.getUserInterface().askForChoice(
							"Do you also want to add dependent library " + oLib.getName() + " to the repository?",
							"Add Dependent Library To Repository", options, "Yes");		
					}
					if (ret == 1)
					{
						noList.add(oLib);
					} else
					{
						if (ret == 2) yesToAll = true;
						yesList.add(oLib);
						includeDependentLibraries(oLib, yesList, noList, yesToAll);
					}
				}
			}
		}
	}

	/**
	 * Method to check a library into the repository.
	 * @param lib the library to add to the repository.
	 * @param pl the ProjectLibrary associated with the library.
	 * Throws a JobException on error.
	 */
	private void addLibraryNow(Library lib, ProjectLibrary pl)
		throws JobException
	{
		// get path prefix for cell libraries
		String libraryname = lib.getName();

		// create the top-level directory for this library
		pl.setProjectDirectory(Project.getRepositoryLocation() + File.separator + libraryname);
		File dir = new File(pl.getProjectDirectory());
		if (dir.exists())
		{
			throw new JobException(
				"Repository directory '" + pl.getProjectDirectory() + "' already exists");
		}
		if (!dir.mkdir())
		{
			throw new JobException(
				"Could not create repository directory '" + pl.getProjectDirectory() + "'");
		}
		System.out.println("Making repository directory '" + pl.getProjectDirectory() + "'...");

		// turn off all tools
		Project.setChangeStatus(true);

		// make libraries for every cell
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			ProjectCell pc = new ProjectCell(cell, pl);
			pc.setLastOwner(Project.getCurrentUserName());
			pc.setComment("Initial checkin");

			// ignore old unused cell versions
			if (cell.getNewestVersion() != cell)
			{
				if (cell.getNumUsagesIn() == 0) continue;
				System.out.println("Warning: including old version of " + cell);
				pc.setLatestVersion(false);
			}

			// link the cell into the project lists
			pl.linkProjectCellToCell(pc, cell);

			if (Project.writeCell(cell, pc)) System.out.println("Error writing cell file"); else		// CHANGES DATABASE
			{
				// write the cell to disk in its own library
				System.out.println("Entering " + cell);

				// mark this cell "checked in" and locked
				Project.markLocked(cell, true);		// CHANGES DATABASE
			}
		}

		// create the project file
		String projfile = pl.getProjectDirectory() + File.separator + Project.PROJECTFILE;
		lib.newVar(Project.PROJPATHKEY, projfile);
		try
		{
			PrintStream buffWriter = new PrintStream(new FileOutputStream(projfile));
			for(Iterator<ProjectCell> it = pl.getProjectCells(); it.hasNext(); )
			{
				ProjectCell pc = it.next();
				buffWriter.println("::" + pc.getCellName() + ":" + pc.getVersion() + "-" +
					pc.getView().getFullName() + "." + pc.getLibExtension() + ":" +
					pc.getOwner() + ":" + pc.getLastOwner() + ":" + pc.getComment());
			}
			buffWriter.close();
		} catch (IOException e)
		{
			System.out.println("Error creating " + projfile);
		}

		// restore tool state
		Project.setChangeStatus(false);
	}

}
