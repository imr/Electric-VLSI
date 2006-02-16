/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UpdateJob.java
 * Project management tool: update libraries from the repository
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
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This class updates cells from the Project Management repository.
 */
public class UpdateJob extends Job
{
	private ProjectDB pdb;
	private DisplayedCells displayedCells;

	/**
	 * Method to update the project libraries from the repository.
	 */
	public static void updateProject()
	{
		// make sure there is a valid user name and repository
		if (Users.needUserName()) return;
		if (Project.ensureRepository()) return;

		new UpdateJob();
	}

	private UpdateJob()
	{
		super("Update all Cells from Repository", Project.getProjectTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
		this.pdb = Project.projectDB;

		// save the current window configuration
		displayedCells = new DisplayedCells();
		startJob();
	}

	public boolean doIt() throws JobException
	{
		Set<ProjectLibrary> projectLibs = new HashSet<ProjectLibrary>();
		for(Iterator<Library> lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library lib = lIt.next();
			if (lib.isHidden()) continue;
			ProjectLibrary pl = pdb.findProjectLibrary(lib);
			if (pl.getProjectDirectory() == null) continue;
			projectLibs.add(pl);
		}

		// lock access to the project files (throws JobException on error)
		ProjectLibrary.lockManyProjectFiles(projectLibs);

		// make a list of all cells that need to be updated
		List<ProjectCell> updatedProjectCells = new ArrayList<ProjectCell>();
		for(ProjectLibrary pl : projectLibs)
		{
			// add ProjectCells that need to be updated to the list
			addNewProjectCells(pl, updatedProjectCells);		// CHANGES DATABASE
		}

		// lock library projects
		boolean allLocked = true;
		for(ProjectCell pc : updatedProjectCells)
		{
			ProjectLibrary pl = pc.getProjectLibrary();
			if (projectLibs.contains(pl)) continue;
			try
			{
				pl.lockProjectFile();
			} catch (JobException e)
			{
				allLocked = false;
				break;
			}
			projectLibs.add(pl);
		}

		int total = 0;
		if (allLocked)
		{
			// prevent tools (including this one) from seeing the change
			Project.setChangeStatus(true);

			for(;;)
			{
				Iterator<ProjectCell> it = updatedProjectCells.iterator();
				if (!it.hasNext()) break;
				ProjectCell pc = it.next();
				total += updateCellFromRepository(pdb, pc, updatedProjectCells);		// CHANGES DATABASE
			}

			// restore change broadcast
			Project.setChangeStatus(false);
		}

		// relase project file locks and validate all cell locks
		for(ProjectLibrary pl : projectLibs)
		{
			pl.releaseProjectFileLock(false);
			validateLocks(pdb, pl.getLibrary());		// CHANGES DATABASE
		}

		// summarize
		if (total == 0) System.out.println("Project is up-to-date"); else
			System.out.println("Updated " + total + " cells");

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
    }

	private void validateLocks(ProjectDB pdb, Library lib)
	{
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			ProjectCell pc = pdb.findProjectCell(cell);
			if (pc == null)
			{
				// cell not in the project: writable
				Project.markLocked(cell, false);		// CHANGES DATABASE
			} else
			{
				if (cell.getVersion() < pc.getVersion())
				{
					// cell is an old version: writable
					Project.markLocked(cell, false);		// CHANGES DATABASE
				} else
				{
					if (pc.getOwner().equals(Project.getCurrentUserName()))
					{
						// cell checked out to current user: writable
						Project.markLocked(cell, false);		// CHANGES DATABASE
					} else
					{
						// cell checked out to someone else: not writable
						Project.markLocked(cell, true);		// CHANGES DATABASE
					}
				}
			}
		}
	}
	/**
	 * Method to recursively update the project.
	 * @param pc the ProjectCell to update.
	 * If subcells need to be updated first, that will happen.
	 * @return the number of cells that were updated.
	 */
	private int updateCellFromRepository(ProjectDB pdb, ProjectCell pc, List<ProjectCell> updatedProjectCells)
	{
		ProjectLibrary pl = pc.getProjectLibrary();
		Library lib = pl.getLibrary();
		Cell oldCell = lib.findNodeProto(pc.describe());
		Cell newCell = null;

		// read the library with the new cell
		int total = 0;
		String libName = pl.getProjectDirectory() + File.separator + pc.getCellName() + File.separator + pc.getVersion() + "-" +
			pc.getView().getFullName() + "." + pc.getLibExtension();
		String tempLibName = Project.getTempLibraryName();
		NetworkTool.setInformationOutput(false);
		Library fLib = LibraryFiles.readLibrary(TextUtils.makeURLToFile(libName), tempLibName, pc.getLibType(), true);
		NetworkTool.setInformationOutput(true);
		if (fLib == null) System.out.println("Cannot read library " + libName); else
		{
			String cellNameInRepository = pc.describe();
			Cell cur = fLib.findNodeProto(cellNameInRepository);
			if (cur == null) System.out.println("Cannot find cell " + cellNameInRepository + " in library " + libName); else
			{
				// build node map and see if others should be copied first
				HashMap<NodeInst,NodeProto> nodePrototypes = new HashMap<NodeInst,NodeProto>();
				for(Iterator<NodeInst> it = cur.getNodes(); it.hasNext(); )
				{
					NodeInst ni = it.next();
					NodeProto np = ni.getProto();
					nodePrototypes.put(ni, np);
					if (!ni.isCellInstance()) continue;
					Cell subCell = (Cell)np;
					if (subCell.getView().isTextView()) continue;
					Library subLib = lib;

					String subCellName = Project.describeFullCellName(subCell);
					Variable var = subCell.getVar(Project.PROJLIBRARYKEY);
					if (var != null)
					{
						String subLibName = (String)var.getObject();
						subLib = Library.findLibrary(subLibName);
						if (subCellName.startsWith(subLibName+"__"))
							subCellName = subCellName.substring(subLibName.length()+2);
						if (subLib == null)
						{
							// find a new library in the repository
							subLib = Library.newInstance(subLibName, null);
							String projFile = Project.getRepositoryLocation() + File.separator + subLibName + File.separator + Project.PROJECTFILE;
							File pf = new File(projFile);
							if (!pf.exists())
							{
								System.out.println("Cannot find project file '" + projFile + "'...retrieve aborted.");
							} else
							{
								subLib.newVar(Project.PROJPATHKEY, projFile);
								ProjectLibrary subPL = pdb.findProjectLibrary(subLib);

								// get all recent cells
								addNewProjectCells(subPL, updatedProjectCells);
							}
						}
					}

					Cell foundSubCell = subLib.findNodeProto(subCellName);
					if (foundSubCell == null)
					{
						ProjectLibrary subPL = pdb.findProjectLibrary(subLib);
						ProjectCell subCellPC = subPL.findProjectCellByNameViewVersion(subCell.getName(), subCell.getView(), subCell.getVersion());
						if (subCellPC != null)
						{
							if (subCellPC.getCell() != null)
							{
								System.out.println("ERROR: cell " + subCellName + " does not exist, but it appears as " +
									subCellPC.getCell());
							}
							if (!updatedProjectCells.contains(subCellPC))
							{
								System.out.println("ERROR: cell " + subCellName + " needs to be updated but isn't in the list");
							}
							total += updateCellFromRepository(pdb, subCellPC, updatedProjectCells);
							foundSubCell = subCellPC.getCell();
						}
					}
					nodePrototypes.put(ni, foundSubCell);
				}

				String cellName = Project.describeFullCellName(cur);
				newCell = Cell.copyNodeProtoUsingMapping(cur, lib, cellName, nodePrototypes);
				if (newCell == null) System.out.println("Cannot copy " + cur + " from new library");
			}

			// kill the library
			fLib.kill("delete");
		}

		// return the new cell
		if (newCell != null)
		{
			pl.linkProjectCellToCell(pc, newCell);
			if (oldCell != null)
			{
				if (Project.useNewestVersion(oldCell, newCell))		// CHANGES DATABASE
				{
					System.out.println("Error replacing instances of new " + oldCell);
				} else
				{
					ProjectCell oldPC = pl.findProjectCell(oldCell);
					pl.linkProjectCellToCell(oldPC, null);

					// record that cells changed so that displays get updated
		        	displayedCells.swap(oldCell, newCell);
					System.out.println("Updated " + newCell);
				}
			} else
			{
				System.out.println("Added new " + newCell);
			}
			total++;
		}
		updatedProjectCells.remove(pc);
		return total;
	}

	private void addNewProjectCells(ProjectLibrary pl, List<ProjectCell> updatedProjectCells)
	{
		HashMap<String,ProjectCell> versionToGet = new HashMap<String,ProjectCell>();
		for(Iterator<ProjectCell> it = pl.getProjectCells(); it.hasNext(); )
		{
			ProjectCell pc = it.next();
			String cellName = pc.getCellName() + "{" + pc.getView().getAbbreviation() + "}";
			ProjectCell pcToGet = versionToGet.get(cellName);
			if (pcToGet != null)
			{
				if (pc.getVersion() <= pcToGet.getVersion()) continue;
				if (pc.getOwner().length() > 0)
				{
					// this version is checked-out
					Cell oldCell = pl.getLibrary().findNodeProto(pc.describeWithVersion());
					if (oldCell != null)
					{
						// found the cell in the library
						if (pc.getOwner().equals(Project.getCurrentUserName()))
						{
							versionToGet.remove(cellName);
						} else
						{
							System.out.println("WARNING: " + oldCell + " is checked-out to " + pc.getOwner());
						}
						continue;
					} else
					{
						// the cell is not in the library
						if (!pc.getOwner().equals(Project.getCurrentUserName())) continue;

						System.out.println("WARNING: Cell " + pl.getLibrary().getName() + ":" + pc.describe() +
							" is checked-out to you but is missing from this library.  Re-building it.");
						// prevent tools (including this one) from seeing the changes
						Project.setChangeStatus(true);

						oldCell = pl.getLibrary().findNodeProto(pc.describe());
						Library lib = oldCell.getLibrary();
						String newName = oldCell.getName() + ";" + pc.getVersion() + "{" + pc.getView().getAbbreviation() + "}";
						if (oldCell != null)
						{
							Cell newVers = Cell.copyNodeProto(oldCell, lib, newName, true);
							if (newVers == null)
							{
								System.out.println("Error making new version of cell " + oldCell.describe(false));
								Project.setChangeStatus(false);
								continue;
							}

							// replace former usage with new version
							if (Project.useNewestVersion(oldCell, newVers))		// CHANGES DATABASE
							{
								System.out.println("Error replacing instances of cell " + oldCell.describe(false));
								Project.setChangeStatus(false);
								continue;
							}
							pl.ignoreCell(oldCell);
							pl.linkProjectCellToCell(pc, newVers);
							Project.markLocked(newVers, false);		// CHANGES DATABASE
						} else
						{
							// the cell never existed before: create it
							Cell newVers = Cell.makeInstance(lib, newName);
							pl.linkProjectCellToCell(pc, newVers);
						}
						Project.setChangeStatus(false);
					}
				}
			}
			versionToGet.put(cellName, pc);
		}
		for(String cellName : versionToGet.keySet())
		{
			ProjectCell pc = versionToGet.get(cellName);
			Cell oldCellAny = pl.getLibrary().findNodeProto(pc.describe());
			Cell oldCell = pl.getLibrary().findNodeProto(pc.describeWithVersion());
			if (oldCellAny != null && oldCellAny.getVersion() > pc.getVersion())
				System.out.println("WARNING: " + oldCellAny + " is newer than what is in the repository.  Updating it from the repository version");
			if (oldCell == null) updatedProjectCells.add(pc);
		}
	}

}
