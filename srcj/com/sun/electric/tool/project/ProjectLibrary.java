/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ProjectLibrary.java
 * Project management tool: library information
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
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.dialogs.OpenFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * Class to describe libraries checked into the Project Management system.
 */
public class ProjectLibrary implements Serializable
{
	/** the project directory */				private String                     projDirectory;
	/** Library associated with project file */	private Library                    lib;
	/** all cell records in the project */		private List<ProjectCell>          allCells;
	/** cell records by Cell in the project */	private HashMap<Cell,ProjectCell>  byCell;
	/** I/O channel for project file */			private transient RandomAccessFile raf;
	/** Lock on file when updating it */		private transient FileLock         lock;

//	void validate()
//	{
//		for(ProjectCell pc : allCells)
//		{
//			Cell c = pc.getCell();
//			if (c != null && !c.isLinked()) System.out.println("HEY! "+c+" IS NOT LINKED");
//		}
//		for(Cell c : byCell.keySet())
//		{
//			if (c != null && !c.isLinked()) System.out.println("HEY! "+c+" IS NOT LINKED IN BYCELLS");
//		}
//	}

	private ProjectLibrary()
	{
		allCells = new ArrayList<ProjectCell>();
		byCell = new HashMap<Cell,ProjectCell>();
	}

	static ProjectLibrary createProject(Library lib)
	{
		// create a new project database
		ProjectLibrary pl = new ProjectLibrary();
		pl.lib = lib;

		// figure out the location of the project file
		Variable var = lib.getVar(Project.PROJPATHKEY);
		if (var == null) return pl;
		URL url = TextUtils.makeURLToFile((String)var.getObject());
		if (!TextUtils.URLExists(url))
		{
			url = null;
			if (Project.getRepositoryLocation().length() > 0)
			{
				url = TextUtils.makeURLToFile(Project.getRepositoryLocation() + File.separator + lib.getName() + File.separator + Project.PROJECTFILE);
				if (!TextUtils.URLExists(url)) url = null;
			}
			if (url == null)
			{
				String userFile = OpenFile.chooseInputFile(FileType.PROJECT, "Find Project File for " + lib);
				if (userFile == null) return pl;
				url = TextUtils.makeURLToFile(userFile);
			}
		}

		// prepare to read the project file
		String projectFile = url.getFile();
		String projDir = "";
		int sepPos = projectFile.lastIndexOf('/');
		if (sepPos >= 0) projDir = projectFile.substring(0, sepPos);
		try
		{
			pl.raf = new RandomAccessFile(projectFile, "r");
		} catch (FileNotFoundException e)
		{
			System.out.println("Cannot read file: " + projectFile);
			return pl;
		}

		// learn the repository location if this path is valid
		if (Project.getRepositoryLocation().length() == 0)
		{
			String repositoryLocation = null;
			if (sepPos > 1)
			{
				int nextSepPos = projectFile.lastIndexOf('/', sepPos-1);
				if (nextSepPos >= 0) repositoryLocation = projectFile.substring(0, nextSepPos);
			}
			if (repositoryLocation == null)
			{
				Job.getUserInterface().showInformationMessage(
					"You should setup Project Management by choosing a Repository location.  Use the 'Project Management' tab under General Preferences",
					"Setup Project Management");
			} else
			{
				Project.setRepositoryLocation(repositoryLocation);
			}
		}

		pl.projDirectory = projDir;
		pl.loadProjectFile();

		try
		{
			pl.raf.close();
		} catch (IOException e)
		{
			System.out.println("Error closing project file");
		}
		return pl;
	}

	/**
	 * Method to add a ProjectCell to this ProjectLibrary.
	 * Keeps the list sorted.
	 * @param pc the ProjectCell to add.
	 */
	void addProjectCell(ProjectCell pc)
	{
		allCells.add(pc);
		Collections.sort(allCells, new OrderedProjectCells());
	}

	/**
	 * Method to remove a ProjectCell from this ProjectLibrary.
	 * Keeps the list sorted.
	 * @param pc the ProjectCell to remove.
	 */
	void removeProjectCell(ProjectCell pc)
	{
		for(ProjectCell c : allCells)
		{
			if (!c.getCellName().equals(pc.getCellName())) continue;
			if (c.getVersion() != pc.getVersion()) continue;
			if (c.getView() != pc.getView()) continue;
			allCells.remove(c);
			break;
		}
		if (pc.getCell() != null) byCell.remove(pc.getCell());
		Collections.sort(allCells, new OrderedProjectCells());
	}

	void linkProjectCellToCell(ProjectCell pc, Cell cell)
	{
		if (cell == null)
		{
			pc.setLatestVersion(false);
			byCell.remove(pc.getCell());
		} else
		{
			byCell.put(cell, pc);
		}
		pc.setCell(cell);
	}

	void ignoreCell(Cell cell)
	{
		byCell.remove(cell);
	}

	boolean isEmpty() { return allCells.size() == 0; }

	Library getLibrary() { return lib; }

	Iterator<ProjectCell> getProjectCells() { return allCells.iterator(); }

	String getProjectDirectory() { return projDirectory; }

	void setProjectDirectory(String dir) { projDirectory = dir; }

	/**
	 * Class to sort project cells.
	 */
    private static class OrderedProjectCells implements Comparator<ProjectCell>
    {
        public int compare(ProjectCell pc1, ProjectCell pc2)
        {
        	int diff = pc1.getCellName().compareTo(pc2.getCellName());
        	if (diff != 0) return diff;
        	diff = pc1.getView().getFullName().compareTo(pc2.getView().getFullName());
        	if (diff != 0) return diff;
        	return pc1.getVersion() - pc2.getVersion();
        }
    }

	ProjectCell findProjectCell(Cell cell)
	{
		ProjectCell pc = byCell.get(cell);
		return pc;
	}

	ProjectCell findProjectCellByNameView(String name, View view)
	{
		for(ProjectCell pc : allCells)
		{
			if (pc.getCellName().equals(name) && pc.getView() == view) return pc;
		}
		return null;
	}

	ProjectCell findProjectCellByNameViewVersion(String name, View view, int version)
	{
		for(ProjectCell pc : allCells)
		{
			if (pc.getCellName().equals(name) && pc.getView() == view && pc.getVersion() == version) return pc;
		}
		return null;
	}

	/**
	 * Method to lock this project file.
	 * Throws JobException on error.
	 */
	void lockProjectFile()
		throws JobException
	{
		String errMsg = tryLockProjectFile();
		if (errMsg != null)
		{
			throw new JobException(
				"Cannot lock the project file (" + errMsg + ").  It may be in use by another user, or it may be damaged.");
		}
	}

	/**
	 * Method to lock a set of project files.
	 * @param projectFiles the set of project files.
	 * Throws JobException on error.
	 */
	static void lockManyProjectFiles(Set<ProjectLibrary> projectFiles)
		throws JobException
	{
		List<ProjectLibrary> didThem = new ArrayList<ProjectLibrary>();
		for(ProjectLibrary pl : projectFiles)
		{
			String errMsg = pl.tryLockProjectFile();
			if (errMsg != null)
			{
				// failed, unlock those already locked
				for(ProjectLibrary uPl : didThem)
					uPl.releaseProjectFileLock(false);
				throw new JobException(
					"Cannot lock the project file for library " + pl.lib.getName() +
					"(" + errMsg + ").  It may be in use by another user, or it may be damaged.");
			}
			didThem.add(pl);
		}
	}

	/**
	 * Method to lock this project file.
	 * @return error message on error, null on success.
	 */
	private String tryLockProjectFile()
	{
		// prepare to read the project file
		String projectFile = projDirectory + File.separator + Project.PROJECTFILE;
		try
		{
			raf = new RandomAccessFile(projectFile, "rw");
		} catch (FileNotFoundException e)
		{
			return "Cannot read file " + projectFile;
		}

		FileChannel fc = raf.getChannel();
		try
		{
			lock = fc.lock();
		} catch (IOException e1)
		{
			String errMsg = "Unable to lock project file";
			try
			{
				raf.close();
			} catch (IOException e2)
			{
				errMsg = "Unable to close project file";
			}
			raf = null;
			return errMsg;
		}
		String errMsg = loadProjectFile();
		if (errMsg != null)
		{
			try
			{
				lock.release();
				raf.close();
			} catch (IOException e)
			{
				errMsg += "; Unable to release project file lock";
			}
			raf = null;
		}
		return errMsg;
	}

	/**
	 * Method to release the lock on this project file.
	 * @param save true to rewrite it first.
	 */
	void releaseProjectFileLock(boolean save)
	{
		if (save)
		{
			FileChannel fc = raf.getChannel();
			try
			{
				fc.position(0);
				fc.truncate(0);
				for(ProjectCell pc : allCells)
				{
					String line = "::" + pc.getCellName() + ":" + pc.getVersion() + "-" +
						pc.getView().getFullName() + "." + pc.getLibExtension() + ":" +
						pc.getOwner() + ":" + pc.getLastOwner() + ":" + pc.getComment() + "\n";
					ByteBuffer bb = ByteBuffer.wrap(line.getBytes());
					fc.write(bb);
				}
			} catch (IOException e)
			{
				System.out.println("Error saving project file");
			}
		}
		try
		{
			lock.release();
			raf.close();
		} catch (IOException e)
		{
			System.out.println("Unable to unlock and close project file");
			lock = null;
		}
	}
	/**
	 * Method to unlock a set of project files.
	 * @param projectFiles the set of project files.
	 */
	static void releaseManyProjectFiles(Set<ProjectLibrary> projectFiles)
	{
		for(ProjectLibrary pl : projectFiles)
		{
			pl.releaseProjectFileLock(true);
		}
	}

	/**
	 * Method to read the project file into memory.
	 * @return an error message on error (null if OK).
	 */
	private String loadProjectFile()
	{
		allCells.clear();
		byCell.clear();

		// read the project file
		int [] colonPos = new int[6];
		for(;;)
		{
			String userLine = null;
			try
			{
				userLine = raf.readLine();
			} catch (IOException e)
			{
				userLine = null;
			}
			if (userLine == null) break;

			ProjectCell pc = new ProjectCell(null, this);
			int prevPos = 0;
			for(int i=0; i<6; i++)
			{
				colonPos[i] = userLine.indexOf(':', prevPos);
				prevPos = colonPos[i] + 1;
				if (prevPos <= 0)
				{
					return "Too few keywords in project file: " + userLine;
				}
			}
			if (colonPos[0] != 0)
			{
				return "Missing initial ':' in project file: " + userLine;
			}

			// get cell name
			pc.setCellName(userLine.substring(colonPos[1]+1, colonPos[2]));

			// get version
			String section = userLine.substring(colonPos[2]+1, colonPos[3]);
			int dashPos = section.indexOf('-');
			if (dashPos < 0)
			{
				return "Missing '-' after version number in project file: " + userLine;
			}
			int dotPos = section.indexOf('.');
			if (dotPos < 0)
			{
				return "Missing '.' after view type in project file: " + userLine;
			}
			pc.setVersion(TextUtils.atoi(section.substring(0, dashPos)));

			// get view
			String viewPart = section.substring(dashPos+1, dotPos);
			pc.setView(View.findView(viewPart));

			// get file type
			String fileType = section.substring(dotPos+1);
			if (fileType.equals("elib")) pc.setLibType(FileType.ELIB); else
				if (fileType.equals("jelib")) pc.setLibType(FileType.JELIB); else
					if (fileType.equals("txt")) pc.setLibType(FileType.READABLEDUMP); else
			{
				return "Unknown library type in project file: " + userLine;
			}

			// get owner
			pc.setOwner(userLine.substring(colonPos[3]+1, colonPos[4]));

			// get last owner
			pc.setLastOwner(userLine.substring(colonPos[4]+1, colonPos[5]));

			// get comments
			pc.setComment(userLine.substring(colonPos[5]+1));

			// check for duplication
			for(ProjectCell opc : allCells)
			{
				if (opc == pc) continue;
				if (!opc.getCellName().equalsIgnoreCase(pc.getCellName())) continue;
				if (opc.getView() != pc.getView()) continue;
				if (opc.getVersion() != pc.getVersion()) continue;
				System.out.println("Error in project file: version " + pc.getVersion() + ", view '" +
					pc.getView().getFullName() + "' of cell '" + pc.getCellName() + "' exists twice");
			}

			// find the cell associated with this entry
			pc.setLatestVersion(false);
			String cellName = pc.describeWithVersion();
			Cell cell = lib.findNodeProto(cellName);
			pc.setCell(cell);
			if (cell != null)
			{
				if (cell.getVersion() > pc.getVersion())
				{
					if (!pc.getOwner().equals(Project.getCurrentUserName()))
					{
						if (pc.getOwner().length() == 0)
						{
							System.out.println("WARNING: " + cell + " is being edited, but it is not checked-out");
						} else
						{
							System.out.println("WARNING: " + cell + " is being edited, but it is checked-out to " + pc.getOwner());
						}
					}
				}
				byCell.put(cell, pc);
			}

			// link it in
//			allCells.add(pc);
		}

		// determine the most recent views
		HashMap<String,ProjectCell> mostRecent = new HashMap<String,ProjectCell>();
		for(ProjectCell pc : allCells)
		{
			String cellEntry = pc.describe();
			ProjectCell recent = mostRecent.get(cellEntry);
			if (recent != null && recent.getVersion() > pc.getVersion()) continue;
			mostRecent.put(cellEntry, pc);
		}
		for(ProjectCell pc : allCells)
		{
			String cellEntry = pc.describe();
			ProjectCell recent = mostRecent.get(cellEntry);
			pc.setLatestVersion(recent == pc);
		}
		return null;
	}
}
