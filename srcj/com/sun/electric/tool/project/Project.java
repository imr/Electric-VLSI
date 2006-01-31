/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Project.java
 * Project management tool
 * Written by: Steven M. Rubin
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableElectricObject;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.GenMath.MutableInteger;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.user.ViewChanges;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;

/**
 * This is the Project Management tool.
 */
public class Project extends Listener
{
	/**
	 * There are two levels of security: low and medium.
	 * Medium security manages a list of user names/passwords and requires logging-in.
	 * It is only "medium" becuase the passwords are badly encrypted, and it is easy to add user names.
	 * Low security simply uses the user's name without questioning it.
	 */
	public static final boolean LOWSECURITY    = true;

	public static final int NOTMANAGED         = 0;
	public static final int CHECKEDIN          = 1;
	public static final int CHECKEDOUTTOYOU    = 2;
	public static final int CHECKEDOUTTOOTHERS = 3;
	public static final int OLDVERSION         = 4;

	private static final Variable.Key PROJLOCKEDKEY = Variable.newKey("PROJ_locked");
	        static final Variable.Key PROJPATHKEY   = Variable.newKey("PROJ_path");
	private static final Variable.Key PROJLIBRARYKEY = Variable.newKey("PROJ_library");
	private static final String PUSERFILE   = "projectusers";
	        static final String PROJECTFILE = "project.proj";

	/** the Project tool. */					private static Project tool = new Project();
	/** the users */							private static HashMap<String,String> usersMap;
	/** nonzero if the system is active */		private static boolean pmActive;
	/** nonzero to ignore broadcast changes */	private static boolean ignoreChanges;
	/** check modules */						private static List<FCheck>    fCheckList = new ArrayList<FCheck>();
	/** the database describing the project */	private static ProjectDB projectDB = new ProjectDB();

	/**
	 * Each combination of cell and change-batch is queued by one of these objects.
	 */
	private static class FCheck
	{
		Cell   entry;
		int    batchNumber;
	};

	/****************************** TOOL CONTROL ******************************/

	/**
	 * The constructor sets up the Project Management tool.
	 */
	private Project()
	{
		super("project");
	}

	/**
	 * Method to initialize the Project Management tool.
	 */
	public void init()
	{
		setOn();
		pmActive = false;
		ignoreChanges = false;
	}

	/**
	 * Method to retrieve the singleton associated with the Project tool.
	 * @return the Project tool.
	 */
	public static Project getProjectTool() { return tool; }

	/**
	 * Method to tell whether a Library is in the repository.
	 * @param lib the Library in quesiton.
	 * @return true if the Library is in the repository, and under the control of Project Management.
	 */
	public static boolean isLibraryManaged(Library lib)
	{
		ProjectLibrary pl = projectDB.findProjectLibrary(lib);
		if (pl.isEmpty()) return false;
		return true;
	}
	
	/**
	 * Method to return the status of a Cell in Project Management.
	 * @param cell the Cell in question.
	 * @return NOTMANAGED: this cell is not in any repository<BR>
	 * CHECKEDIN: the cell is checked into the repository and is available for checkout.<BR>
	 * CHECKEDOUTTOYOU: the cell is checked out to the currently-logged in user.<BR>
	 * CHECKEDOUTTOOTHERS: the cell is checked out to someone else
	 * (use "getCellOwner" to find out who).<BR>
	 * OLDVERSION: this is an old version of a cell in the repository.<BR>
	 */
	public static int getCellStatus(Cell cell)
	{
		Cell newestVersion = cell.getNewestVersion();
		ProjectCell pc = projectDB.findProjectCell(newestVersion);
		if (pc == null) return NOTMANAGED;
		if (newestVersion != cell) return OLDVERSION;
		if (pc.getOwner().length() == 0) return CHECKEDIN;
		if (pc.getOwner().equals(getCurrentUserName())) return CHECKEDOUTTOYOU;
		return CHECKEDOUTTOOTHERS;
	}

	/**
	 * Method to get the name of the owner of a Cell.
	 * @param cell the Cell in question.
	 * @return the name of the user who owns the Cell.
	 * Returns a null string if no owner can be found.
	 */
	public static String getCellOwner(Cell cell)
	{
		ProjectCell pc = projectDB.findProjectCell(cell);
		if (pc == null) return "";
		return pc.getOwner();
	}

	/**
	 * Method to update the project libraries from the repository.
	 */
	public static void updateProject()
	{
		// make sure there is a valid user name
		if (needUserName()) return;

		new UpdateJob(projectDB);
		pmActive = true;
	}

	/**
	 * Method to check the currently edited cell back into the repository.
	 */
	public static void checkInThisCell()
	{
		UserInterface ui = Job.getUserInterface();
		Cell cell = ui.needCurrentCell();
		if (cell == null) return;
		checkIn(cell);
	}

	/**
	 * Method to check a cell back into the repository.
	 * @param cell the Cell to check back in.
	 */
	public static void checkIn(Cell cell)
	{
		pmActive = true;
		HashMap<Cell,MutableInteger> cellsMarked = markRelatedCells(cell);

		// make sure there is a valid user name
		if (needUserName()) return;

		new CheckInJob(projectDB, cell.getLibrary(), cellsMarked);
	}

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
		pmActive = true;

		// make sure there is a valid user name
		if (needUserName()) return;

		// make a list of just this cell
		List<Cell> oneCell = new ArrayList<Cell>();
		oneCell.add(cell);

		new CheckOutJob(projectDB, oneCell);

	}

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
		pmActive = true;

		// make sure there is a valid user name
		if (needUserName()) return;

		boolean response = Job.getUserInterface().confirmMessage(
			"Cancel all changes to the checked-out " + cell + " and revert to the checked-in version?");
		if (!response) return;

		new CancelCheckOutJob(projectDB, cell);
	}

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
		pmActive = true;

		// make sure there is a valid user name
		if (needUserName()) return;
		
		new AddCellJob(projectDB, cell);
	}

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
		pmActive = true;

		// make sure there is a valid user name
		if (needUserName()) return;

		new DeleteCellJob(projectDB, cell);
	}

	/**
	 * Method to examine the history of the currently edited cell.
	 */
	public static void examineThisHistory()
	{
		UserInterface ui = Job.getUserInterface();
		Cell cell = ui.needCurrentCell();
		if (cell == null) return;
		examineHistory(cell);
	}

	/**
	 * Method to examine the history of a cell.
	 * @param cell the Cell to examine.
	 */
	public static void examineHistory(Cell cell)
	{
		pmActive = true;
		new HistoryDialog(projectDB, cell);
	}

	/**
	 * Method to prompt for all libraries in the repository and
	 * choose one to retrieve.
	 */
	public static void getALibrary()
	{
		pmActive = true;
		new LibraryDialog(projectDB);
	}

	/**
	 * Method to add the current library to the repository.
	 */
	public static void addThisLibrary()
	{
		addALibrary(Library.getCurrent());
	}

	/**
	 * Method to add all libraries to the repository.
	 */
	public static void addAllLibraries()
	{
		if (getRepositoryLocation().length() == 0)
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
			if (isLibraryManaged(lib)) continue;
			libList.add(lib);
		}
		new AddLibraryJob(projectDB, libList);
	}

	/**
	 * Method to add a library to the repository.
	 * Finds dependent libraries and asks if they, too, should be added.
	 * @param lib the Library to add to the repository.
	 */
	public static void addALibrary(Library lib)
	{
		if (getRepositoryLocation().length() == 0)
		{
			Job.getUserInterface().showInformationMessage(
				"Before entering a library, set a repository location in the 'Project Management' tab under General Preferences",
				"Must Setup Project Management");
			return;
		}

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
		new AddLibraryJob(projectDB, yesList);
		pmActive = true;
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
					if (isLibraryManaged(oLib)) continue;

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
	 * Method to return the number of users in the user database.
	 * @return the number of users in the user database.
	 */
	public static int getNumUsers()
	{
		ensureUserList();
		return usersMap.size();
	}

	/**
	 * Method to return an Iterator over the users in the user database.
	 * @return an Iterator over the users in the user database.
	 */
	public static Iterator<String> getUsers()
	{
		ensureUserList();
		return usersMap.keySet().iterator();
	}

	/**
	 * Method to tell whether a user name is in the user database.
	 * @param user the user name.
	 * @return true if the user name is in the user database.
	 */
	public static boolean isExistingUser(String user)
	{
		ensureUserList();
		return usersMap.get(user) != null;
	}

	/**
	 * Method to remove a user name from the user database.
	 * @param user the user name to remove from the user database.
	 */
	public static void deleteUser(String user)
	{
		usersMap.remove(user);
		saveUserList();
	}

	/**
	 * Method to add a user to the user database.
	 * @param user the user name to add.
	 * @param encryptedPassword the encrypted password for the user.
	 */
	public static void addUser(String user, String encryptedPassword)
	{
		usersMap.put(user, encryptedPassword);
		saveUserList();
	}

	/**
	 * Method to return the encrypted password associated with a given user.
	 * @param user the user name.
	 * @return the user's encrypted password (null if not found).
	 */
	public static String getEncryptedPassword(String user)
	{
		return (String)usersMap.get(user);
	}

	/**
	 * Method to change a user's encrypted password.
	 * @param user the user name.
	 * @param newEncryptedPassword the new encrypted password for the user.
	 */
	public static void changeEncryptedPassword(String user, String newEncryptedPassword)
	{
		usersMap.put(user, newEncryptedPassword);
		saveUserList();
	}

	/****************************** LISTENER INTERFACE ******************************/

	/**
	 * Method to handle the start of a batch of changes.
	 * @param tool the tool that generated the changes.
	 * @param undoRedo true if these changes are from an undo or redo command.
	 */
	public void startBatch(Tool tool, boolean undoRedo) {}

	/**
	 * Method to announce the end of a batch of changes.
	 */
	public void endBatch()
	{
		detectIllegalChanges();

		// always reset change ignorance at the end of a batch
		ignoreChanges = false;
	}

	/**
	 * Method to announce a change to a NodeInst.
	 * @param ni the NodeInst that was changed.
	 * @param oD the old contents of the NodeInst.
	 */
	public void modifyNodeInst(NodeInst ni, ImmutableNodeInst oD)
	{
		if (ignoreChanges) return;
		queueCheck(ni.getParent());
	}

	/**
	 * Method to announce a change to an ArcInst.
	 * @param ai the ArcInst that changed.
     * @param oD the old contents of the ArcInst.
	 */
	public void modifyArcInst(ArcInst ai, ImmutableArcInst oD)
	{
		if (ignoreChanges) return;
		queueCheck(ai.getParent());
	}

	/**
	 * Method to handle a change to an Export.
	 * @param pp the Export that moved.
	 * @param oD the old contents of the Export.
	 */
	public void modifyExport(Export pp, ImmutableExport oD)
	{
		if (ignoreChanges) return;
		queueCheck((Cell)pp.getParent());
	}

//	/**
//	 * Method to handle a change to a Cell.
//	 * @param cell the cell that was changed.
//	 * @param oLX the old low X bound of the Cell.
//	 * @param oHX the old high X bound of the Cell.
//	 * @param oLY the old low Y bound of the Cell.
//	 * @param oHY the old high Y bound of the Cell.
//	 */
//	public void modifyCell(Cell cell, double oLX, double oHX, double oLY, double oHY)
//	{
//		if (ignoreChanges) return;
//		queueCheck(cell);
//	}

	/**
	 * Method to announce a move of a Cell int CellGroup.
	 * @param cell the cell that was moved.
	 * @param oCellGroup the old CellGroup of the Cell.
	 */
	public void modifyCellGroup(Cell cell, Cell.CellGroup oCellGroup)
	{
		if (ignoreChanges) return;
		queueCheck(cell);
	}

	/**
	 * Method to handle the creation of a new ElectricObject.
	 * @param obj the ElectricObject that was just created.
	 */
	public void newObject(ElectricObject obj)
	{
		checkObject(obj);
	}

	/**
	 * Method to handle the deletion of an ElectricObject.
	 * @param obj the ElectricObject that was just deleted.
	 */
	public void killObject(ElectricObject obj)
	{
		checkObject(obj);
	}

	/**
	 * Method to handle the renaming of an ElectricObject.
	 * @param obj the ElectricObject that was renamed.
	 * @param oldName the former name of that ElectricObject.
	 */
	public void renameObject(ElectricObject obj, Object oldName)
	{
		checkObject(obj);
	}

	/**
	 * Method to handle a change of object Variables.
	 * @param obj the ElectricObject on which Variables changed.
	 * @param oldImmutable the old Variables.
	 */
	public void modifyVariables(ElectricObject obj, ImmutableElectricObject oldImmutable)
    {
        checkVariables(obj, oldImmutable);
    }

	/**
	 * Method to announce that a Library has been read.
	 * @param lib the Library that was read.
	 */
	public void readLibrary(Library lib)
	{
		// scan the library to see if any cells are locked
		if (ignoreChanges) return;
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			if (cell.getVar(PROJLOCKEDKEY) != null)
			{
				pmActive = true;

				// see if this library has a known project database
				projectDB.findProjectLibrary(lib);
			}
		}
	}

	/**
	 * Method to announce that a Library is about to be erased.
	 * @param lib the Library that will be erased.
	 */
	public void eraseLibrary(Library lib) {}

	/**
	 * Method to announce that a Library is about to be written to disk.
	 * The method should always be called inside of a Job so that the
	 * implementation can make changes to the database.
	 * @param lib the Library that will be saved.
	 */
	public void writeLibrary(Library lib) {}

	/****************************** LISTENER SUPPORT ******************************/

	private static boolean alwaysCheckOut = false;

	private static void detectIllegalChanges()
	{
		if (!pmActive) return;
		if (fCheckList.size() == 0) return;

		int lowBatch = Integer.MAX_VALUE;
		List<Cell> cellsThatChanged = new ArrayList<Cell>();
		for(FCheck f : fCheckList)
		{
			Cell cell = f.entry;
			if (cell == null) continue;

			// make sure cell is checked-out
			if (cell.getVar(PROJLOCKEDKEY) != null)
			{
				cellsThatChanged.add(cell);
				if (f.batchNumber < lowBatch) lowBatch = f.batchNumber;
			}
		}
		fCheckList.clear();

		if (cellsThatChanged.size() > 0)
		{
			SwingUtilities.invokeLater(new UndoRunnable(lowBatch, cellsThatChanged));
		}
	}

	/**
	 * Class to undo changes made to cells that are not checked-out.
	 */
	private static class UndoRunnable implements Runnable
	{
		private int lowBatch;
		private List<Cell> cellsThatChanged;

		UndoRunnable(int lowBatch, List<Cell> cellsThatChanged)
		{
			this.lowBatch = lowBatch;
			this.cellsThatChanged = cellsThatChanged;
		}

		public void run()
		{
			// construct an error message
			boolean undoChange = true;
			if (alwaysCheckOut) undoChange = false; else
			{
				String errorMsg = "";
				for(Cell cell : cellsThatChanged)
				{
					if (errorMsg.length() > 0) errorMsg += ", ";
					errorMsg += cell.describe(true);
				}
				String [] options = {"Yes", "No", "Always"};
				int ret = Job.getUserInterface().askForChoice("Cannot change unchecked-out cells: " + errorMsg +
					".  Do you want to check them out?", "Change Blocked by Checked-in Cells", options, "No");
				if (ret == 0) undoChange = false;
				if (ret == 2) { alwaysCheckOut = true;   undoChange = false; }
			}
			if (undoChange)
			{
				// change disallowed: undo it
				new UndoBatchesJob(lowBatch);
			} else
			{
				// change allowed: check-out necessary cells
				new AutoCheckoutJob(projectDB, cellsThatChanged);
			}
		}
	}

	/**
	 * This class checks out cells from Project Management to allow changes that have been made.
	 */
	private static class AutoCheckoutJob extends Job
	{
		private ProjectDB pdb;
		private List<Cell> cellsThatChanged;
		private HashMap<Cell,Cell> newCells;

		private AutoCheckoutJob(ProjectDB pdb, List<Cell> cellsThatChanged)
		{
			super("Undo changes to locked cells", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.pdb = pdb;
			this.cellsThatChanged = cellsThatChanged;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// make a set of project libraries that are affected
			Set<ProjectLibrary> projectLibs = new HashSet<ProjectLibrary>();
			for(Cell oldVers : cellsThatChanged)
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
				preCheckOutCells(pdb, cellsThatChanged);
			} catch (JobException e)
			{
				ProjectLibrary.releaseManyProjectFiles(projectLibs);
				throw e;
			}

			// prevent tools (including this one) from seeing the changes
			setChangeStatus(true);

			// make new version
			newCells = new HashMap<Cell,Cell>();
			for(Cell oldVers : cellsThatChanged)
			{
				// change version information (throws JobException on error)
				Cell newVers = bumpVersion(oldVers);		// CHANGES DATABASE
				if (newVers != null)
				{
					// update records for the changed cells
		        	bumpRecordVersions(pdb, oldVers, newVers);

		        	newCells.put(oldVers, newVers);
				}
			}

			setChangeStatus(false);
			fieldVariableChanged("newCells");
			return true;
		}

		public void terminateIt(JobException je)
        {
        	// update user interface for the changed cells
			for(Cell oldVers : newCells.keySet())
			{
				Cell newVers = newCells.get(oldVers);
		        	updateUI(oldVers, newVers);
			}

			// update explorer tree
			WindowFrame.wantToRedoLibraryTree();
        }
	}

	/**
	 * This class undoes changes to locked cells.
	 */
	private static class UndoBatchesJob extends Job
	{
		private int lowestBatch;

		private UndoBatchesJob(int lowestBatch)
		{
			super("Undo changes to locked cells", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.lowestBatch = lowestBatch;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// undo the changes
			ignoreChanges = true;
			for(;;)
			{
				Undo.ChangeBatch batch = Undo.undoABatch();
				if (batch == null) break;
				if (batch.getBatchNumber() == lowestBatch) break;
			}
			Undo.noRedoAllowed();
			ignoreChanges = false;
			return true;
		}
	}

	private void checkObject(ElectricObject obj)
	{
		if (ignoreChanges) return;
		if (obj instanceof NodeInst) { queueCheck(((NodeInst)obj).getParent());   return; }
		if (obj instanceof ArcInst) { queueCheck(((ArcInst)obj).getParent());   return; }
		if (obj instanceof Export) { queueCheck((Cell)((Export)obj).getParent());   return; }
		if (obj instanceof Cell) { queueCheck((Cell)obj);   return; }
	}

	private void checkVariables(ElectricObject obj, ImmutableElectricObject oldImmutable)
	{
		if (ignoreChanges) return;
		if (obj instanceof NodeInst) { queueCheck(((NodeInst)obj).getParent());   return; }
		if (obj instanceof ArcInst) { queueCheck(((ArcInst)obj).getParent());   return; }
		if (obj instanceof Export) { queueCheck((Cell)((Export)obj).getParent());   return; }
		if (obj instanceof Cell)
		{
            ImmutableElectricObject newImmutable = obj.getImmutable();
            if (variablesDiffers(oldImmutable, newImmutable))
                queueCheck((Cell)obj);
		}
	}

    private boolean variablesDiffers(ImmutableElectricObject oldImmutable, ImmutableElectricObject newImmutable)
	{
		int oldLength = oldImmutable.getNumVariables();
		int newLength = newImmutable.getNumVariables();
		int oldIndex = oldImmutable.searchVar(PROJLOCKEDKEY);
		int newIndex = newImmutable.searchVar(PROJLOCKEDKEY);
		if (oldLength == newLength) {
			if (oldIndex != newIndex) return true;
			if (oldIndex < 0) return variablesDiffers(oldImmutable, 0, newImmutable, 0, oldLength);
			return variablesDiffers(oldImmutable, 0, newImmutable, 0, oldIndex) ||
				variablesDiffers(oldImmutable, oldIndex + 1, newImmutable, oldIndex + 1, oldLength - oldIndex - 1);
		}
		if (oldLength == newLength + 1) {
			if (oldIndex < 0 || oldIndex != ~newIndex) return true;
			return variablesDiffers(oldImmutable, 0, newImmutable, 0, oldIndex) ||
				variablesDiffers(oldImmutable, oldIndex + 1, newImmutable, ~newIndex, oldLength - oldIndex - 1);
		}
		if (newLength == oldIndex + 1) {
			if (newIndex < 0 || newIndex != ~oldIndex) return true;
			return variablesDiffers(oldImmutable, 0, newImmutable, 0, newIndex) ||
				variablesDiffers(oldImmutable, newIndex, newImmutable, newIndex + 1, newLength - newIndex - 1);
		}
		return true;
    }
    
    private boolean variablesDiffers(ImmutableElectricObject oldImmutable, int oldStart, ImmutableElectricObject newImmutable, int newStart, int count) {
        for (int i = 0; i < count; i++)
            if (oldImmutable.getVar(oldStart + i) != newImmutable.getVar(newStart + i)) return true;
        return false;
    }
    
	private static void queueCheck(Cell cell)
	{
		// get the current batch number
		Undo.ChangeBatch batch = Undo.getCurrentBatch();
		if (batch == null) return;
		int batchNumber = batch.getBatchNumber();

		// see if the cell is already queued
		for(FCheck f : fCheckList)
		{
			if (f.entry == cell && f.batchNumber == batchNumber) return;
		}

		FCheck f = new FCheck();
		f.entry = cell;
		f.batchNumber = batchNumber;
		fCheckList.add(f);
	}

	/****************************** PROJECT CONTROL CLASSES ******************************/

	/**
	 * This class checks out a cell from Project Management.
	 * It involves updating the project database and making a new version of the cell.
	 */
	private static class CheckOutJob extends Job
	{
		private ProjectDB pdb;
		private List<Cell> checkOutCells;
		private HashMap<Cell, Cell> createdCells;

		private CheckOutJob(ProjectDB pdb, List<Cell> checkOutCells)
		{
			super("Check out cells", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.pdb = pdb;
			this.checkOutCells = checkOutCells;
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
			setChangeStatus(true);

			// make new version
			createdCells = new HashMap<Cell,Cell>();
			for(Cell oldVers : checkOutCells)
			{
				// change version information (throws JobException on error)
				Cell newVers = bumpVersion(oldVers);		// CHANGES DATABASE
				if (newVers != null)
				{
					// update records for the changed cells
		        	bumpRecordVersions(pdb, oldVers, newVers);

		        	createdCells.put(oldVers, newVers);
				}
			}

			setChangeStatus(false);

			ProjectLibrary.releaseManyProjectFiles(projectLibs);

			fieldVariableChanged("createdCells");
			return true;
		}

        public void terminateIt(JobException je)
        {
        	// update user interface for the changed cells
			for(Cell oldVers : createdCells.keySet())
			{
				Cell newVers = createdCells.get(oldVers);
	        	updateUI(oldVers, newVers);
			}

			// update explorer tree
			WindowFrame.wantToRedoLibraryTree();

			if (je == null)
			{
				// if it worked, print dependencies and display
				if (createdCells != null && createdCells.size() > 0)
				{
					StringBuffer cellNames = new StringBuffer();
					int numCells = 0;
					for(Cell oldVers : createdCells.keySet())
					{
						Cell newVers = createdCells.get(oldVers);
						if (cellNames.length() > 0) cellNames.append(", ");
						cellNames.append(newVers.describe(false));
						numCells++;
					}
					if (numCells > 1) System.out.println("Cells " + cellNames + " checked out for your use"); else
						System.out.println("Cell " + cellNames + " checked out for your use");
					Cell newVers = (Cell)createdCells.get(0);

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
							if (getCellStatus(cell) == CHECKEDOUTTOOTHERS)
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
								System.out.println("    " + cell + " is checked out to " + getCellOwner(cell));
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
							String owner = getCellOwner(cell);
							if (owner.length() == 0) continue;
							if (!owner.equals(getCurrentUserName()))
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
								String owner = getCellOwner(cell);
								System.out.println("    " + cell + " is checked out to " + owner);
							}
						}
					}
				}
			}
        }
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
				if (newestProjectCell.getOwner().equals(getCurrentUserName()))
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
				if (pc.getOwner().equals(getCurrentUserName()))
				{
					markLocked(oldVers, false);		// CHANGES DATABASE
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
		if (useNewestVersion(oldVers, newVers))		// CHANGES DATABASE
			throw new JobException("Error replacing instances of cell " + oldVers.describe(false));
		markLocked(newVers, false);		// CHANGES DATABASE
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
		newPC.setOwner(getCurrentUserName());

		pl.linkProjectCellToCell(oldPC, null);
		pl.linkProjectCellToCell(newPC, newVers);
	}

	/**
	 * Method to fix the user interface to account for cell replacements.
	 * @param newCells a map from old cells to new cells.
	 */
	private static void updateUI(Cell oldVers, Cell newVers)
	{
		// redraw windows that showed the old cell
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			if (wf.getContent().getCell() != oldVers) continue;
			double scale = 1;
			Point2D offset = null;
			if (wf.getContent() instanceof EditWindow_)
			{
				EditWindow_ wnd = (EditWindow_)wf.getContent();
				scale = wnd.getScale();
				offset = wnd.getOffset();
			}
			wf.getContent().setCell(newVers, VarContext.globalContext);
			if (wf.getContent() instanceof EditWindow_)
			{
				EditWindow_ wnd = (EditWindow_)wf.getContent();
				wnd.setScale(scale);
				wnd.setOffset(offset);
			}
		}
	}

	/**
	 * This class checks in cells to Project Management.
	 * It involves updating the project database and saving the current cells to disk.
	 */
	private static class CancelCheckOutJob extends Job
	{
		private ProjectDB pdb;
		private Cell cell, newCell, oldCell;

		private CancelCheckOutJob(ProjectDB pdb, Cell cell)
		{
			super("Cancel Check-out " + cell, tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.pdb = pdb;
			this.cell = cell;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			Library lib = cell.getLibrary();
			ProjectLibrary pl = pdb.findProjectLibrary(lib);

			// lock access to the project files (throws JobException on error)
			pl.lockProjectFile();

			ProjectCell cancelled = null;
			ProjectCell former = null;
			for(Iterator<ProjectCell> it = pl.getProjectCells(); it.hasNext(); )
			{
				ProjectCell pc = it.next();
				if (pc.getCellName().equals(cell.getName()) && pc.getView() == cell.getView())
				{
					if (pc.getVersion() >= cell.getVersion())
					{
						if (pc.getOwner().length() > 0)
						{
							if (pc.getOwner().equals(getCurrentUserName()))
							{
								cancelled = pc;
							} else
							{
								pl.releaseProjectFileLock(true);
								throw new JobException(
									"This cell is not checked out to you.  Only user '" + pc.getOwner() + "' can cancel the check-out.");
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
				throw new JobException("This cell is not checked out.");
			}

			if (former == null)
			{
				pl.releaseProjectFileLock(true);
				throw new JobException("Cannot find former version to restore.");
			}
			oldCell = cancelled.getCell();

			// prevent tools (including this one) from seeing the change
			setChangeStatus(true);

			// replace former usage with new version
			getCellFromRepository(pdb, former, lib, false, false);		// CHANGES DATABASE
			newCell = former.getCell();
			if (newCell == null)
			{
				setChangeStatus(false);
				pl.releaseProjectFileLock(true);
				throw new JobException("Error bringing in former version (" + former.getVersion() + ")");
			}

			if (useNewestVersion(oldCell, newCell))		// CHANGES DATABASE
			{
				setChangeStatus(false);
				pl.releaseProjectFileLock(true);
				throw new JobException("Error replacing instances of former " + oldCell);
			}

			pl.removeProjectCell(cancelled);
			if (cancelled.getCell() != null)
			{
				markLocked(cancelled.getCell(), true);		// CHANGES DATABASE
			}
			former.setLatestVersion(true);

			// restore change broadcast
			setChangeStatus(false);

			// relase project file lock
			pl.releaseProjectFileLock(true);
			return true;
		}

        public void terminateOK()
        {
        	updateUI(oldCell, newCell);

        	// update explorer tree
        	WindowFrame.wantToRedoLibraryTree();
        }
	}

	/**
	 * This class checks in cells to Project Management.
	 * It involves updating the project database and saving the current cells to disk.
	 */
	private static class CheckInJob extends Job
	{
		private ProjectDB pdb;
		private Library lib;
		private HashMap<Cell,MutableInteger> cellsMarked;

		protected CheckInJob(ProjectDB pdb, Library lib, HashMap<Cell,MutableInteger> cellsMarked)
		{
			super("Check in cells", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.pdb = pdb;
			this.lib = lib;
			this.cellsMarked = cellsMarked;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			ProjectLibrary pl = pdb.findProjectLibrary(lib);

			// lock access to the project files (throws JobException on error)
			pl.lockProjectFile();

			// prevent tools (including this one) from seeing the change
			setChangeStatus(true);

			// check in the requested cells
			String cellNames = "";
			for(Cell cell : cellsMarked.keySet())
			{
				MutableInteger mi = (MutableInteger)cellsMarked.get(cell);
				if (mi.intValue() == 0) continue;
				if (cellNames.length() > 0) cellNames += ", ";
				cellNames += cell.describe(false);
			}

			String comment = null;
			String error = null;
			for(Cell cell : cellsMarked.keySet())
			{
				MutableInteger mi = (MutableInteger)cellsMarked.get(cell);
				if (mi.intValue() == 0) continue;

				// find this in the project file
				ProjectCell pc = pdb.findProjectCell(cell);
				if (pc == null)
				{
					error = "Cell " + cell.describe(true) + " is not in the project.  Add it before checking it in or out.";
				} else
				{
					// see if it is available
					if (!pc.getOwner().equals(getCurrentUserName()))
					{
						error = "You cannot check-in " + cell + " because it is checked out to '" + pc.getOwner() + "', not you.";
					} else
					{
						if (comment == null)
							comment = Job.getUserInterface().askForInput("Reason for checking-in " + cellNames, "", null);
						if (comment == null) break;

						// write the cell out there
						if (writeCell(cell, pc))		// CHANGES DATABASE
						{
							error = "Error writing " + cell;
						} else
						{
							pc.setOwner("");
							pc.setLastOwner(getCurrentUserName());
							pc.setVersion(cell.getVersion());
							pc.setComment(comment);
							markLocked(cell, true);		// CHANGES DATABASE
							System.out.println("Cell " + cell.describe(true) + " checked in");
						}
					}
				}
			}

			// restore change broadcast
			setChangeStatus(false);

			// relase project file lock
			pl.releaseProjectFileLock(true);
			if (error != null) throw new JobException(error);

			return true;
		}
	}

	/**
	 * This class updates cells from the Project Management repository.
	 */
	private static class UpdateJob extends Job
	{
		private ProjectDB pdb;

		private UpdateJob(ProjectDB pdb)
		{
			super("Update all Cells from Repository", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.pdb = pdb;
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
				setChangeStatus(true);

				for(;;)
				{
					Iterator<ProjectCell> it = updatedProjectCells.iterator();
					if (!it.hasNext()) break;
					ProjectCell pc = it.next();
					total += updateCellFromRepository(pdb, pc, updatedProjectCells);		// CHANGES DATABASE
				}

				// restore change broadcast
				setChangeStatus(false);
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
			return true;
		}

		public void terminateIt(JobException je)
        {
			// update explorer tree
			WindowFrame.wantToRedoLibraryTree();
        }
	}

	/**
	 * This class adds the current library to the Project Management repository.
	 */
	private static class AddLibraryJob extends Job
	{
		private ProjectDB pdb;
		private Set<Library> libList;

		protected AddLibraryJob(ProjectDB pdb, Set<Library> libList)
		{
			super("Add Library", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.pdb = pdb;
			this.libList = libList;
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
			return true;
		}

        public void terminateOK()
        {
			// advise the user of these libraries
			Job.getUserInterface().showInformationMessage(
				"Libraries have been checked-into the repository and marked appropriately.",
				"Libraries Added");
        }
	}

	/**
	 * Method to check a library into the repository.
	 * @param lib the library to add to the repository.
	 * @param pl the ProjectLibrary associated with the library.
	 * Throws a JobException on error.
	 */
	private static void addLibraryNow(Library lib, ProjectLibrary pl)
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
		setChangeStatus(true);

		// make libraries for every cell
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();

			ProjectCell pc = new ProjectCell(cell, pl);
			pc.setLastOwner(getCurrentUserName());
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

			if (writeCell(cell, pc)) System.out.println("Error writing cell file"); else		// CHANGES DATABASE
			{
				// write the cell to disk in its own library
				System.out.println("Entering " + cell);

				// mark this cell "checked in" and locked
				markLocked(cell, true);		// CHANGES DATABASE
			}
		}

		// create the project file
		String projfile = pl.getProjectDirectory() + File.separator + PROJECTFILE;
		lib.newVar(PROJPATHKEY, projfile);
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
		setChangeStatus(false);
	}

	/**
	 * This class adds a cell to the Project Management repository.
	 */
	private static class AddCellJob extends Job
	{
		private ProjectDB pdb;
		private Cell cell;

		private AddCellJob(ProjectDB pdb, Cell cell)
		{
			super("Add " + cell, tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.pdb = pdb;
			this.cell = cell;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			if (cell.getNewestVersion() != cell)
				throw new JobException("Cannot add an old version of the cell");

			Library lib = cell.getLibrary();
			ProjectLibrary pl = pdb.findProjectLibrary(lib);

			// lock access to the project files (throws JobException on error)
			pl.lockProjectFile();

			// prevent tools (including this one) from seeing the change
			setChangeStatus(true);

			// find this in the project file
			ProjectCell foundPC = pl.findProjectCellByNameView(cell.getName(), cell.getView());
			String error = null;
			if (foundPC != null)
			{
				error = "This cell is already in the repository";
			} else
			{
				// create new entry for this cell
				ProjectCell pc = new ProjectCell(cell, pl);
				pc.setLastOwner(getCurrentUserName());
				pc.setComment("Initial checkin");

				if (writeCell(cell, pc))		// CHANGES DATABASE
				{
					error = "Error writing the cell to the repository";
				} else
				{
					// link it in
					pl.linkProjectCellToCell(pc, cell);

					// mark this cell "checked in" and locked
					markLocked(cell, true);		// CHANGES DATABASE

					System.out.println("Cell " + cell.describe(true) + " added to the project");
				}
			}

			// restore change broadcast
			setChangeStatus(false);

			// relase project file lock
			pl.releaseProjectFileLock(true);

			if (error != null) throw new JobException(error);
			return true;
		}
	}

	/**
	 * This class deletes a cell from the Project Management repository.
	 */
	private static class DeleteCellJob extends Job
	{
		private ProjectDB pdb;
		private Cell cell;

		protected DeleteCellJob(ProjectDB pdb, Cell cell)
		{
			super("Delete cell", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.pdb = pdb;
			this.cell = cell;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// find out which cell is being deleted
			Library lib = cell.getLibrary();

			// make sure the cell is not being used
			HashSet<Cell> markedCells = new HashSet<Cell>();
			for(Iterator<NodeInst> it = cell.getInstancesOf(); it.hasNext(); )
			{
				NodeInst ni = it.next();
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
				throw new JobException("Cannot delete " + cell + " because it is still being used by: " + err.toString());

			ProjectLibrary pl = pdb.findProjectLibrary(lib);

			// lock access to the project files (throws JobException on error)
			pl.lockProjectFile();

			// make sure the user has no cells checked-out
			boolean youOwn = false;
			String error = null;
			for(Iterator<ProjectCell> it = pl.getProjectCells(); it.hasNext(); )
			{
				ProjectCell pc = it.next();
				if (pc.getOwner().equals(getCurrentUserName())) { youOwn = true;   break; }
			}
			if (youOwn)
			{
				StringBuffer infstr = new StringBuffer();
				for(Iterator<ProjectCell> it = pl.getProjectCells(); it.hasNext(); )
				{
					ProjectCell pc = it.next();
					if (!pc.getOwner().equals(getCurrentUserName())) continue;
					if (infstr.length() > 0) infstr.append(", ");
					infstr.append(pc.describe());
				}
				error = "Before deleting a cell from the repository, you must check-in all of your work. " +
					"This is because the deletion may be dependent upon changes recently made. " +
					"These cells are checked out to you: " + infstr.toString();
			} else
			{
				// find this in the project file
				List<ProjectCell> copyList = new ArrayList<ProjectCell>();
				for(Iterator<ProjectCell> it = pl.getProjectCells(); it.hasNext(); )
				{
					ProjectCell pc = it.next();
					copyList.add(pc);
				}
				boolean found = false;
				for(ProjectCell pc : copyList)
				{
					if (pc.getCellName().equals(cell.getName()) && pc.getView() == cell.getView())
					{
						// unlink it
						pl.removeProjectCell(pc);

						// disable change broadcast
						setChangeStatus(true);

						// mark this cell unlocked
						markLocked(cell, false);		// CHANGES DATABASE

						// restore change broadcast
						setChangeStatus(false);
						found = true;
					}
				}
				if (found)
				{
					System.out.println("Cell " + cell.describe(true) + " deleted from the repository");
				} else
				{
					error = "This cell is not in the repository";
				}
			}

			// relase project file lock
			pl.releaseProjectFileLock(true);

			if (error != null) throw new JobException(error);
			return true;
		}

        public void terminateOK()
        {
			// update explorer tree
        	WindowFrame.wantToRedoLibraryTree();
        }
	}

	/************************ SUPPORT ***********************/

	static void setChangeStatus(boolean quiet)
	{
		if (quiet) ignoreChanges = quiet;
		Undo.changesQuiet(quiet);
	}

	private static void ensureUserList()
	{
		if (usersMap == null)
		{
			usersMap = new HashMap<String,String>();
			String userFile = getRepositoryLocation() + File.separator + PUSERFILE;
			URL url = TextUtils.makeURLToFile(userFile);
			try
			{
				URLConnection urlCon = url.openConnection();
				InputStreamReader is = new InputStreamReader(urlCon.getInputStream());
				LineNumberReader lnr = new LineNumberReader(is);

				for(;;)
				{
					String userLine = lnr.readLine();
					if (userLine == null) break;
					int colonPos = userLine.indexOf(':');
					if (colonPos < 0)
					{
						System.out.println("Missing ':' in user file: " + userLine);
						break;
					}
					String userName = userLine.substring(0, colonPos);
					String encryptedPassword = userLine.substring(colonPos+1);
					usersMap.put(userName, encryptedPassword);
				}

				lnr.close();
			} catch (IOException e)
			{
				System.out.println("Creating new user database");
			}
		}
	}

	private static void saveUserList()
	{
		// write the file back
		String userFile = getRepositoryLocation() + File.separator + PUSERFILE;
		try
		{
			PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(userFile)));

			for(String userName : usersMap.keySet())
			{
				String encryptedPassword = (String)usersMap.get(userName);
				printWriter.println(userName + ":" + encryptedPassword);
			}

			printWriter.close();
			System.out.println("Wrote " + userFile);
		} catch (IOException e)
		{
			System.out.println("Error writing " + userFile);
			return;
		}
	}

	private static void validateLocks(ProjectDB pdb, Library lib)
	{
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			ProjectCell pc = pdb.findProjectCell(cell);
			if (pc == null)
			{
				// cell not in the project: writable
				markLocked(cell, false);		// CHANGES DATABASE
			} else
			{
				if (cell.getVersion() < pc.getVersion())
				{
					// cell is an old version: writable
					markLocked(cell, false);		// CHANGES DATABASE
				} else
				{
					if (pc.getOwner().equals(getCurrentUserName()))
					{
						// cell checked out to current user: writable
						markLocked(cell, false);		// CHANGES DATABASE
					} else
					{
						// cell checked out to someone else: not writable
						markLocked(cell, true);		// CHANGES DATABASE
					}
				}
			}
		}
	}

	static void markLocked(Cell cell, boolean locked)
	{
		if (!locked)
		{
			for(Iterator<Cell> it = cell.getCellGroup().getCells(); it.hasNext(); )
			{
				Cell oCell = it.next();
				if (oCell.getView() != cell.getView()) continue;
				if (oCell.getVar(PROJLOCKEDKEY) != null)
					oCell.delVar(PROJLOCKEDKEY);
			}
		} else
		{
			for(Iterator<Cell> it = cell.getCellGroup().getCells(); it.hasNext(); )
			{
				Cell oCell = it.next();
				if (oCell.getView() != cell.getView()) continue;
				if (oCell.getNewestVersion() == oCell)
				{
					if (oCell.getVar(PROJLOCKEDKEY) == null)
						oCell.newVar(PROJLOCKEDKEY, new Integer(1));
				} else
				{
					if (oCell.getVar(PROJLOCKEDKEY) != null)
						oCell.delVar(PROJLOCKEDKEY);
				}
			}
		}
	}

	/**
	 * Method to determine what other cells need to be checked-in with a given Cell.
	 * @param cell the Cell being checked-in.
	 * @return a Map of Cells to check-in (if an entry in the map, associated with a Cell,
	 * is not null, that Cell should be checked-in).
	 */
	private static HashMap<Cell,MutableInteger> markRelatedCells(Cell cell)
	{
		// mark the cell to be checked-in
		HashMap<Cell,MutableInteger> cellsMarked1 = new HashMap<Cell,MutableInteger>();
		HashMap<Cell,MutableInteger> cellsMarked2 = new HashMap<Cell,MutableInteger>();
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library oLib = it.next();
			for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
			{
				Cell oCell = cIt.next();
				cellsMarked1.put(oCell, new MutableInteger(0));
				cellsMarked2.put(oCell, new MutableInteger(0));
			}
		}
		MutableInteger mi = (MutableInteger)cellsMarked1.get(cell);
		mi.setValue(1);

		// look for cells above this one that must also be checked in
		mi = (MutableInteger)cellsMarked2.get(cell);
		mi.setValue(1);
		boolean propagated = true;
		while (propagated)
		{
			propagated = false;
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library oLib = it.next();
				for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
				{
					Cell oCell = cIt.next();
					mi = (MutableInteger)cellsMarked2.get(oCell);
					if (mi.intValue() == 1)
					{
						propagated = true;
						mi.setValue(2);
						for(Iterator<NodeInst> nIt = oCell.getInstancesOf(); nIt.hasNext(); )
						{
							NodeInst ni = nIt.next();
							mi = (MutableInteger)cellsMarked2.get(ni.getParent());
							if (mi.intValue() == 0) mi.setValue(1);
						}
					}
				}
			}
		}
		mi = (MutableInteger)cellsMarked2.get(cell);
		mi.setValue(0);
		int total = 0;
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library oLib = it.next();
			for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
			{
				Cell oCell = cIt.next();
				mi = (MutableInteger)cellsMarked2.get(oCell);
				if (mi.intValue() == 0) continue;
				String owner = getCellOwner(oCell);
				if (owner.length() == 0) continue;
				if (owner.equals(getCurrentUserName()))
				{
					mi = (MutableInteger)cellsMarked1.get(oCell);
					mi.setValue(1);
					total++;
				}
			}
		}

		// look for cells below this one that must also be checked in
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library oLib = it.next();
			for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
			{
				Cell oCell = cIt.next();
				mi = (MutableInteger)cellsMarked2.get(oCell);
				mi.setValue(0);
			}
		}
		mi = (MutableInteger)cellsMarked2.get(cell);
		mi.setValue(1);
		propagated = true;
		while (propagated)
		{
			propagated = false;
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library oLib = it.next();
				for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
				{
					Cell oCell = cIt.next();
					mi = (MutableInteger)cellsMarked2.get(oCell);
					if (mi.intValue() == 1)
					{
						propagated = true;
						mi.setValue(2);
						for(Iterator<NodeInst> nIt = oCell.getNodes(); nIt.hasNext(); )
						{
							NodeInst ni = nIt.next();
							if (!ni.isCellInstance()) continue;
							mi = (MutableInteger)cellsMarked2.get(ni.getProto());
							if (mi.intValue() == 0) mi.setValue(1);
						}
					}
				}
			}
		}
		mi = (MutableInteger)cellsMarked2.get(cell);
		mi.setValue(0);
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library oLib = it.next();
			for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
			{
				Cell oCell = cIt.next();
				mi = (MutableInteger)cellsMarked2.get(oCell);
				if (mi.intValue() == 0) continue;
				String owner = getCellOwner(oCell);
				if (owner.length() == 0) continue;
				if (owner.equals(getCurrentUserName()))
				{
					mi = (MutableInteger)cellsMarked1.get(oCell);
					mi.setValue(1);
					total++;
				}
			}
		}

		// advise of additional cells that must be checked-in
		if (total > 0)
		{
			total = 0;
			StringBuffer infstr = new StringBuffer();
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library oLib = it.next();
				for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
				{
					Cell oCell = cIt.next();
					mi = (MutableInteger)cellsMarked1.get(oCell);
					if (oCell == cell || mi.intValue() == 0) continue;
					if (total > 0) infstr.append(", ");
					infstr.append(oCell.describe(true));
					total++;
				}
			}
			System.out.println("Also checking in related cell(s): " + infstr.toString());
		}
		return cellsMarked1;
	}

	/**
	 * Method to get the latest version of the cell described by "pc" and return
	 * the newly created cell.
	 */
	static void getCellFromRepository(ProjectDB pdb, ProjectCell pc, Library lib, boolean recursively, boolean report)
	{
		// figure out the library name
		ProjectLibrary pl = pc.getProjectLibrary();
		String libName = pl.getProjectDirectory() + File.separator + pc.getCellName() + File.separator + pc.getVersion() + "-" +
			pc.getView().getFullName() + "." + pc.getLibExtension();
		URL libURL = TextUtils.makeURLToFile(libName);

		// read the library
		Cell newCell = null;
		String tempLibName = getTempLibraryName();
		NetworkTool.setInformationOutput(false);
		Library fLib = LibraryFiles.readLibrary(TextUtils.makeURLToFile(libName), tempLibName, pc.getLibType(), true);
		NetworkTool.setInformationOutput(true);
		if (fLib == null) System.out.println("Cannot read library " + libName); else
		{
			String cellNameInRepository = pc.describe();
			Cell cur = fLib.findNodeProto(cellNameInRepository);
			if (cur == null) System.out.println("Cannot find cell " + cellNameInRepository + " in library " + libName); else
			{
				// make the mapping from repository dummy cells to real cells
				HashMap<NodeInst,NodeProto> nodePrototypes = new HashMap<NodeInst,NodeProto>();
				for(Iterator<NodeInst> it = cur.getNodes(); it.hasNext(); )
				{
					NodeInst ni = it.next();
					if (ni.isCellInstance())
					{
						Cell subCell = (Cell)ni.getProto();
						Library subLib = lib;
						String cellName = subCell.noLibDescribe();
						Variable var = subCell.getVar(PROJLIBRARYKEY);
						if (var != null)
						{
							String subLibName = (String)var.getObject();
							subLib = Library.findLibrary(subLibName);
							if (cellName.startsWith(subLibName+"__"))
								cellName = cellName.substring(subLibName.length()+2);
							if (subLib == null && recursively)
							{
								// find a new library in the repository
								subLib = Library.newInstance(subLibName, null);
								String projFile = Project.getRepositoryLocation() + File.separator + subLibName + File.separator + PROJECTFILE;
								File pf = new File(projFile);
								if (!pf.exists())
								{
									System.out.println("Cannot find project file '" + projFile + "'...retrieve aborted.");
								} else
								{
									subLib.newVar(PROJPATHKEY, projFile);
									ProjectLibrary subPL = pdb.findProjectLibrary(subLib);
	
									// get all recent cells
									String userName = getCurrentUserName();
									for(Iterator<ProjectCell> pIt = subPL.getProjectCells(); pIt.hasNext(); )
									{
										ProjectCell recPC = pIt.next();
										if (!recPC.isLatestVersion()) continue;
										if (recPC.getCell() == null)
										{
											getCellFromRepository(pdb, recPC, subLib, true, report);
											if (recPC.getCell() == null)
												System.out.println("Error retrieving cell from repository");
										}
										if (recPC.getCell() != null)
										{
											boolean youOwn = userName.length() > 0 && recPC.getOwner().equals(userName);
											markLocked(recPC.getCell(), !youOwn);		// CHANGES DATABASE
										}
									}
								}
							}
						}
						Cell realSubCell = null;
						if (subLib != null)
							realSubCell = subLib.findNodeProto(cellName);

						// if doing a recursive cell copy, see if others should be copied first
						if (realSubCell == null && recursively)
						{
							ProjectLibrary subPL = pdb.findProjectLibrary(subLib);
							ProjectCell subPC = subPL.findProjectCellByNameViewVersion(subCell.getName(), subCell.getView(), subCell.getVersion());
							if (subPC == null)
							{
								// could not find that cell, see if a different version is available
								for(Iterator<ProjectCell> pIt = subPL.getProjectCells(); pIt.hasNext(); )
								{
									ProjectCell oPc = pIt.next();
									if (oPc.getCellName().equals(subCell.getName()) && oPc.getView() == subCell.getView())
									{
										if (subPC != null && subPC.getVersion() > oPc.getVersion()) continue;
										subPC = oPc;
									}
								}
							}
							if (subPC != null)
							{
								if (subPC.getCell() != null)
								{
									System.out.println("ERROR: cell " + cellName + " does not exist, but it appears as " +
										subPC.getCell());
								}
								getCellFromRepository(pdb, subPC, subLib, recursively, false);
								realSubCell = subPC.getCell();
							}
						}
						if (realSubCell == null)
						{
							System.out.println("Cannot find subcell " + cellName + " referenced by cell " + cellNameInRepository);
							continue;
						}
						nodePrototypes.put(ni, realSubCell);
					}
				}

				String cellName = describeFullCellName(cur);
				if (report) System.out.println("Retrieving cell " + lib.getName() + ":" + cellName);
				newCell = Cell.copyNodeProtoUsingMapping(cur, lib, cellName, nodePrototypes);
				if (newCell == null) System.out.println("Cannot copy " + cur + " from new library");
			}

			// kill the library
			fLib.kill("delete");
		}

		// return the new cell
		if (newCell != null)
			pl.linkProjectCellToCell(pc, newCell);
	}

	private static void addNewProjectCells(ProjectLibrary pl, List<ProjectCell> updatedProjectCells)
	{
		HashMap<String,ProjectCell> versionToGet = new HashMap<String,ProjectCell>();
		for(Iterator<ProjectCell> it = pl.getProjectCells(); it.hasNext(); )
		{
			ProjectCell pc = it.next();
			String cellName = pc.getCellName() + "{" + pc.getView().getAbbreviation() + "}";
			ProjectCell pcToGet = (ProjectCell)versionToGet.get(cellName);
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
						setChangeStatus(true);

						oldCell = pl.getLibrary().findNodeProto(pc.describe());
						Library lib = oldCell.getLibrary();
						String newName = oldCell.getName() + ";" + pc.getVersion() + "{" + pc.getView().getAbbreviation() + "}";
						if (oldCell != null)
						{
							Cell newVers = Cell.copyNodeProto(oldCell, lib, newName, true);
							if (newVers == null)
							{
								System.out.println("Error making new version of cell " + oldCell.describe(false));
								setChangeStatus(false);
								continue;
							}

							// replace former usage with new version
							if (useNewestVersion(oldCell, newVers))		// CHANGES DATABASE
							{
								System.out.println("Error replacing instances of cell " + oldCell.describe(false));
								setChangeStatus(false);
								continue;
							}
							pl.ignoreCell(oldCell);
							pl.linkProjectCellToCell(pc, newVers);
							markLocked(newVers, false);		// CHANGES DATABASE
						} else
						{
							// the cell never existed before: create it
							Cell newVers = Cell.makeInstance(lib, newName);
							pl.linkProjectCellToCell(pc, newVers);
						}
						setChangeStatus(false);
					}
				}
			}
			versionToGet.put(cellName, pc);
		}
		for(String cellName : versionToGet.keySet())
		{
			ProjectCell pc = (ProjectCell)versionToGet.get(cellName);
			Cell oldCellAny = pl.getLibrary().findNodeProto(pc.describe());
			Cell oldCell = pl.getLibrary().findNodeProto(pc.describeWithVersion());
			if (oldCellAny != null && oldCellAny.getVersion() > pc.getVersion())
				System.out.println("WARNING: " + oldCellAny + " is newer than what is in the repository.  Updating it from the repository version");
			if (oldCell == null) updatedProjectCells.add(pc);
		}
	}

	/**
	 * Method to recursively update the project.
	 * @param pc the ProjectCell to update.
	 * If subcells need to be updated first, that will happen.
	 * @return the number of cells that were updated.
	 */
	private static int updateCellFromRepository(ProjectDB pdb, ProjectCell pc, List<ProjectCell> updatedProjectCells)
	{
		ProjectLibrary pl = pc.getProjectLibrary();
		Library lib = pl.getLibrary();
		Cell oldCell = lib.findNodeProto(pc.describe());
		Cell newCell = null;

		// read the library with the new cell
		int total = 0;
		String libName = pl.getProjectDirectory() + File.separator + pc.getCellName() + File.separator + pc.getVersion() + "-" +
			pc.getView().getFullName() + "." + pc.getLibExtension();
		String tempLibName = getTempLibraryName();
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

					String subCellName = describeFullCellName(subCell);
					Variable var = subCell.getVar(PROJLIBRARYKEY);
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
							String projFile = Project.getRepositoryLocation() + File.separator + subLibName + File.separator + PROJECTFILE;
							File pf = new File(projFile);
							if (!pf.exists())
							{
								System.out.println("Cannot find project file '" + projFile + "'...retrieve aborted.");
							} else
							{
								subLib.newVar(PROJPATHKEY, projFile);
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

				String cellName = describeFullCellName(cur);
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
				if (useNewestVersion(oldCell, newCell))		// CHANGES DATABASE
				{
					System.out.println("Error replacing instances of new " + oldCell);
				} else
				{
					System.out.println("Updated " + newCell);
				}
				pl.ignoreCell(oldCell);
			} else
			{
				System.out.println("Added new " + newCell);
			}
			total++;
		}
		updatedProjectCells.remove(pc);
		return total;
	}

	private static boolean useNewestVersion(Cell oldCell, Cell newCell)
	{
		// replace all instances
		List<NodeInst> instances = new ArrayList<NodeInst>();
		for(Iterator<NodeInst> it = oldCell.getInstancesOf(); it.hasNext(); )
			instances.add(it.next());
		for(NodeInst ni : instances)
		{
			NodeInst newNi = ni.replace(newCell, false, false);
			if (newNi == null)
			{
				System.out.println("Failed to update instance of " + newCell + " in " + ni.getParent());
				return true;
			}
		}

		// redraw windows that showed the old cell
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			if (wf.getContent().getCell() != oldCell) continue;
			double scale = 1;
			Point2D offset = null;
			if (wf.getContent() instanceof EditWindow_)
			{
				EditWindow_ wnd = (EditWindow_)wf.getContent();
				scale = wnd.getScale();
				offset = wnd.getOffset();
			}
			wf.getContent().setCell(newCell, VarContext.globalContext);
			if (wf.getContent() instanceof EditWindow_)
			{
				EditWindow_ wnd = (EditWindow_)wf.getContent();
				wnd.setScale(scale);
				wnd.setOffset(offset);
			}
		}

		// replace library references
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = it.next();
			if (lib.getCurCell() == oldCell) lib.setCurCell(newCell);
		}

		// finally delete the former cell
		oldCell.kill();

		return false;
	}

	/**
	 * Method to save a Cell to the repository.
	 * @param cell the Cell to save.
	 * @param pc the ProjectCell record associated with the Cell.
	 * @return true on error.
	 */
	private static boolean writeCell(Cell cell, ProjectCell pc)
	{
		String dirName = pc.getProjectLibrary().getProjectDirectory() + File.separator + cell.getName();
		File dir = new File(dirName);
		if (!dir.exists())
		{
			if (!dir.mkdir())
			{
				System.out.println("Unable to create directory " + dirName);
				return true;
			}
		}

		String libName = dirName + File.separator + cell.getVersion() + "-" + cell.getView().getFullName() + ".elib";
		String tempLibName = getTempLibraryName();
		Library fLib = Library.newInstance(tempLibName, TextUtils.makeURLToFile(libName));
		if (fLib == null)
		{
			System.out.println("Cannot create library " + libName);
			return true;
		}

		Cell cellCopy = copyRecursively(cell, fLib);		// CHANGES DATABASE
		if (cellCopy == null)
		{
			System.out.println("Could not place " + cell + " in a library");
			fLib.kill("delete");
			return true;
		}

		fLib.setCurCell(cellCopy);
		fLib.setFromDisk();
		boolean error = Output.writeLibrary(fLib, pc.getLibType(), false, true);
		if (error)
		{
			System.out.println("Could not save library with " + cell + " in it");
			fLib.kill("delete");
			return true;
		}
		fLib.kill("delete");
		return false;
	}

	private static String getTempLibraryName()
	{
		for(int i=1; ; i++)
		{
			String libName = "projecttemp" + i;
			if (Library.findLibrary(libName) == null) return libName;
		}
	}

	/**
	 * Method to copy a Cell to a different Library, including skeleton copies of any subcells.
	 * @param fromCell the cell to copy.
	 * @param toLib the destination Library.
	 * @return the Cell in that Library which was created.
	 */
	private static Cell copyRecursively(Cell fromCell, Library toLib)
	{
		Cell newFromCell = toLib.findNodeProto(fromCell.noLibDescribe());
		if (newFromCell != null) return newFromCell;

		// must copy subcells
		HashMap<NodeInst,NodeProto> nodePrototypes = new HashMap<NodeInst,NodeProto>();
		for(Iterator<NodeInst> it = fromCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			NodeProto np = ni.getProto();
			nodePrototypes.put(ni, np);
			if (!ni.isCellInstance()) continue;
			Cell cell = (Cell)np;
			if (cell.getView().isTextView()) continue;

			// get proper subcell name
			String subCellName = describeFullCellName(cell);
			if (cell.getLibrary() != fromCell.getLibrary())
				subCellName = cell.getLibrary().getName() + "__" + subCellName;

			// see if there is already a cell with this name
			Cell oCell = toLib.findNodeProto(subCellName);
			if (oCell == null)
			{
				oCell = Cell.makeInstance(toLib, subCellName);
				if (oCell == null)
				{
					System.out.println("Could not create subcell " + subCellName);
					continue;
				}
				if (cell.getLibrary() != fromCell.getLibrary())
					oCell.newVar(PROJLIBRARYKEY, cell.getLibrary().getName());

				if (ViewChanges.skeletonizeCell(cell, oCell))
				{
					System.out.println("Copy of sub" + cell + " failed");
					return null;
				}
			}
			nodePrototypes.put(ni, oCell);
		}

		// copy the cell
		newFromCell = Cell.copyNodeProtoUsingMapping(fromCell, toLib, describeFullCellName(fromCell), nodePrototypes);
		return newFromCell;
	}

	private static String describeFullCellName(Cell cell)
	{
		String cellName = cell.getName() + ";" + cell.getVersion();
		if (cell.getView() != View.UNKNOWN) cellName += "{" + cell.getView().getAbbreviation() + "}";
		return cellName;
	}

	private static final int ROTORSZ = 256;		/* a power of two */
	private static final int MASK =   (ROTORSZ-1);
	/**
	 * Method to encrypt a string in the most simple of ways.
	 * A one-rotor machine designed along the lines of Enigma but considerably trivialized.
	 * @param text the text to encrypt.
	 * @return an encrypted version of the text.
	 */
	public static String encryptPassword(String text)
	{
		// first setup the machine
		String key = "BicIsSchediwy";
		String readable = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+-";
		int seed = 123;
		int keyLen = key.length();
		for (int i=0; i<keyLen; i++) seed = seed*key.charAt(i) + i;
		char [] t1 = new char[ROTORSZ];
		char [] t2 = new char[ROTORSZ];
		char [] t3 = new char[ROTORSZ];
		char [] deck = new char[ROTORSZ];
		for(int i=0; i<ROTORSZ; i++)
		{
			t1[i] = (char)i;
			t3[i] = 0;
			deck[i] = (char)i;
		}
		for(int i=0; i<ROTORSZ; i++)
		{
			seed = 5*seed + key.charAt(i%keyLen);
			int random = seed % 65521;
			int k = ROTORSZ-1 - i;
			int ic = (random&MASK) % (k+1);
			random >>= 8;
			int temp = t1[k];
			t1[k] = t1[ic];
			t1[ic] = (char)temp;
			if (t3[k] != 0) continue;
			ic = (random&MASK) % k;
			while (t3[ic] != 0) ic = (ic+1) % k;
			t3[k] = (char)ic;
			t3[ic] = (char)k;
		}
		for(int i=0; i<ROTORSZ; i++) t2[t1[i]&MASK] = (char)i;

		// now run the machine
		int n1 = 0;
		int n2 = 0;
		int nr2 = 0;
		StringBuffer result = new StringBuffer();
		for(int pt=0; pt<text.length(); pt++)
		{
			int nr1 = deck[n1]&MASK;
			nr2 = deck[nr1]&MASK;
			int i = t2[(t3[(t1[(text.charAt(pt)+nr1)&MASK]+nr2)&MASK]-nr2)&MASK]-nr1;
			result.append(readable.charAt(i&63));
			n1++;
			if (n1 == ROTORSZ)
			{
				n1 = 0;
				n2++;
				if (n2 == ROTORSZ) n2 = 0;
				shuffle(deck, key);
			}
		}
		String res = result.toString();
		return res;
	}

	private static void shuffle(char [] deck, String key)
	{
		int seed = 123;
		int keyLen = key.length();
		for(int i=0; i<ROTORSZ; i++)
		{
			seed = 5*seed + key.charAt(i%keyLen);
			int random = seed % 65521;
			int k = ROTORSZ-1 - i;
			int ic = (random&MASK) % (k+1);
			int temp = deck[k];
			deck[k] = deck[ic];
			deck[ic] = (char)temp;
		}
	}

	/**
	 * Method to ensuer that there is a valid user name.
	 * @return true if there is NO valid user name (also displays error message).
	 */
	private static boolean needUserName()
	{
		if (getCurrentUserName().length() == 0)
		{
			if (LOWSECURITY)
			{
				setCurrentUserName(System.getProperty("user.name"));
				return false;
			}
			Job.getUserInterface().showErrorMessage(
				"You must select a user first (in the 'Project Management' panel of the Preferences dialog)",
				"No Valid User Name");
			return true;
		}
		return false;
	}

	/************************ PREFERENCES ***********************/

	private static Pref cacheCurrentUserName = Pref.makeStringPref("CurrentUserName", tool.prefs, "");
	/**
	 * Method to tell the name of the current user of Project Management.
	 * The default is "".
	 * @return the name of the current user of Project Management.
	 */
	public static String getCurrentUserName() { return cacheCurrentUserName.getString(); }
	/**
	 * Method to set the name of the current user of Project Management.
	 * @param u the name of the current user of Project Management.
	 */
	public static void setCurrentUserName(String u) { cacheCurrentUserName.setString(u); }

	private static Pref cacheRepositoryLocation = Pref.makeStringPref("RepositoryLocation", tool.prefs, "");
	/**
	 * Method to tell the location of the project management repository.
	 * The default is "".
	 * @return the location of the project management repository.
	 */
	public static String getRepositoryLocation() { return cacheRepositoryLocation.getString(); }
	/**
	 * Method to set the location of the project management repository.
	 * @param r the location of the project management repository.
	 */
	public static void setRepositoryLocation(String r)
	{
		boolean alter = getRepositoryLocation().length() > 0;
		cacheRepositoryLocation.setString(r);
		usersMap = null;
		if (alter) projectDB.clearDatabase();
	}

	private static Pref cacheAuthorizationPassword = Pref.makeStringPref("e", tool.prefs, "e");
	/**
	 * Method to tell the authorization password for administering users in Project Management.
	 * The default is "".
	 * @return the authorization password for administering users in Project Management.
	 */
	public static String getAuthorizationPassword() { return cacheAuthorizationPassword.getString(); }
	/**
	 * Method to set the authorization password for administering users in Project Management.
	 * @param a the authorization password for administering users in Project Management.
	 */
	public static void setAuthorizationPassword(String a)
	{
		cacheAuthorizationPassword.setString(a);
	}

}
