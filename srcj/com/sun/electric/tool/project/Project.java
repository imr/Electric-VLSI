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

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellId;
import com.sun.electric.database.ImmutableCell;
import com.sun.electric.database.ImmutableElectricObject;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.user.ViewChanges;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingUtilities;

/**
 * This is the Project Management tool.
 */
public class Project extends Listener
{
	public static final int NOTMANAGED         = 0;
	public static final int CHECKEDIN          = 1;
	public static final int CHECKEDOUTTOYOU    = 2;
	public static final int CHECKEDOUTTOOTHERS = 3;
	public static final int OLDVERSION         = 4;

	private static final Variable.Key PROJLOCKEDKEY = Variable.newKey("PROJ_locked");
	        static final Variable.Key PROJPATHKEY   = Variable.newKey("PROJ_path");
	        static final Variable.Key PROJLIBRARYKEY = Variable.newKey("PROJ_library");
	        static final String PROJECTFILE = "project.proj";

	/** the Project tool. */					private static Project tool = new Project();
	/** the users */							private static HashMap<String,String> usersMap;
	/** nonzero to ignore broadcast changes */	private static boolean ignoreChanges;
	/** check modules */						private static List<FCheck>    fCheckList = new ArrayList<FCheck>();
	/** nonzero if the system is active */		        static boolean pmActive;
	/** the database describing the project */	        static ProjectDB projectDB = new ProjectDB();

	/**
	 * Each combination of cell and change-batch is queued by one of these objects.
	 */
	private static class FCheck
	{
		CellId entry;
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

	/****************************** LISTENER INTERFACE ******************************/

//	/**
//	 * Method to handle the start of a batch of changes.
//	 * @param tool the tool that generated the changes.
//	 * @param undoRedo true if these changes are from an undo or redo command.
//	 */
//	public void startBatch(Tool tool, boolean undoRedo) {}

   /**
     * Handles database changes of a Job.
     * @param oldSnapshot database snapshot before Job.
     * @param undoRedo true if Job was Undo/Redo job.
     */
    public void endBatch(Snapshot oldSnapshot, Snapshot newSnapshot, boolean undoRedo)
	{
        int batchNumber = newSnapshot.snapshotId;
        for (CellId cellId: newSnapshot.getChangedCells(oldSnapshot)) {
            CellBackup oldBackup = oldSnapshot.getCell(cellId);
            CellBackup newBackup = newSnapshot.getCell(cellId);
            if (cellChanged(oldBackup, newBackup))
                queueCheck(cellId, batchNumber);
        }
		detectIllegalChanges();

		// always reset change ignorance at the end of a batch
		ignoreChanges = false;
	}

//	/**
//	 * Method to announce a change to a NodeInst.
//	 * @param ni the NodeInst that was changed.
//	 * @param oD the old contents of the NodeInst.
//	 */
//	public void modifyNodeInst(NodeInst ni, ImmutableNodeInst oD)
//	{
//		if (ignoreChanges) return;
//		queueCheck(ni.getParent());
//	}
//
//	/**
//	 * Method to announce a change to an ArcInst.
//	 * @param ai the ArcInst that changed.
//     * @param oD the old contents of the ArcInst.
//	 */
//	public void modifyArcInst(ArcInst ai, ImmutableArcInst oD)
//	{
//		if (ignoreChanges) return;
//		queueCheck(ai.getParent());
//	}
//
//	/**
//	 * Method to handle a change to an Export.
//	 * @param pp the Export that moved.
//	 * @param oD the old contents of the Export.
//	 */
//	public void modifyExport(Export pp, ImmutableExport oD)
//	{
//		if (ignoreChanges) return;
//		queueCheck((Cell)pp.getParent());
//	}
//
//	/**
//	 * Method to handle a change to a Cell.
//	 * @param cell the Cell that was changed.
//	 * @param oD the old contents of the Cell.
//	 */
//	public void modifyCell(Cell cell, ImmutableCell oD) {
//		if (ignoreChanges) return;
//        if (cellDiffers(oD, cell.getD()))
//            queueCheck(cell);
//    }
//
//	/**
//	 * Method to announce a move of a Cell int CellGroup.
//	 * @param cell the cell that was moved.
//	 * @param oCellGroup the old CellGroup of the Cell.
//	 */
//	public void modifyCellGroup(Cell cell, Cell.CellGroup oCellGroup)
//	{
//		if (ignoreChanges) return;
//		queueCheck(cell);
//	}
//
//	/**
//	 * Method to handle a change to a Library.
//	 * @param lib the Library that was changed.
//	 * @param oldD the old contents of the Library.
//	 */
//	public void modifyLibrary(Library lib, ImmutableLibrary oldD) {}
//
//	/**
//	 * Method to handle the creation of a new ElectricObject.
//	 * @param obj the ElectricObject that was just created.
//	 */
//	public void newObject(ElectricObject obj)
//	{
//		checkObject(obj);
//	}
//
//	/**
//	 * Method to handle the deletion of an ElectricObject.
//	 * @param obj the ElectricObject that was just deleted.
//	 */
//	public void killObject(ElectricObject obj)
//	{
//		checkObject(obj);
//	}
//
//	/**
//	 * Method to handle the renaming of an ElectricObject.
//	 * @param obj the ElectricObject that was renamed.
//	 * @param oldName the former name of that ElectricObject.
//	 */
//	public void renameObject(ElectricObject obj, Object oldName)
//	{
//		checkObject(obj);
//	}

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

//	/**
//	 * Method to announce that a Library is about to be erased.
//	 * @param lib the Library that will be erased.
//	 */
//	public void eraseLibrary(Library lib) {}
//
//	/**
//	 * Method to announce that a Library is about to be written to disk.
//	 * The method should always be called inside of a Job so that the
//	 * implementation can make changes to the database.
//	 * @param lib the Library that will be saved.
//	 */
//	public void writeLibrary(Library lib) {}

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
			Cell cell = Cell.inCurrentThread(f.entry);
            // Cell cell = f.entry;
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
                int ret = 1;
				//int ret = Job.getUserInterface().askForChoice("Cannot change unchecked-out cells: " + errorMsg +
				//	".  Do you want to check them out?", "Change Blocked by Checked-in Cells", options, "No");
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
				new CheckOutJob(cellsThatChanged, true);
			}
		}
	}

	/**
	 * This class undoes changes to locked cells.
	 */
	private static class UndoBatchesJob extends Undo.UndoJob
	{
		private UndoBatchesJob(int lowestBatch)
		{
			super("Undo changes to locked cells", lowestBatch);
		}
        
        public void terminateOK() {
			Undo.noRedoAllowed();
        }
	}

//	private void checkObject(ElectricObject obj)
//	{
//		if (ignoreChanges) return;
//		if (obj instanceof NodeInst) { queueCheck(((NodeInst)obj).getParent());   return; }
//		if (obj instanceof ArcInst) { queueCheck(((ArcInst)obj).getParent());   return; }
//		if (obj instanceof Export) { queueCheck((Cell)((Export)obj).getParent());   return; }
//		if (obj instanceof Cell) { queueCheck((Cell)obj);   return; }
//	}

    /**
     * Compares two CellBackups. Ignores value of PROJLOCKEDKEY.
     * @param oldBackup first CellBackup.
     * @param newBackup second ImmutableCell.
     * @param true  if two ImmutableCells differs more than by value of PROJLOCKEDKEY.
     */
    private boolean cellChanged(CellBackup oldBackup, CellBackup newBackup)
	{
        if (oldBackup == null || newBackup == null) return true;
        assert oldBackup != newBackup;
        if (oldBackup.nodes != newBackup.nodes) return true;
        if (oldBackup.arcs != newBackup.arcs) return true;
        if (oldBackup.exports != newBackup.exports) return true;
        // if (oldBackup.revisionDate != newBackup.revisionDate) return true;
        // if (oldBackup.modified != newBackup.modified) return true; // This will happen if subcells are renamed.
        
        ImmutableCell oldD = oldBackup.d;
        ImmutableCell newD = newBackup.d;
        if (!oldD.equalsExceptVariables(newD)) return true;
        
		int oldLength = oldD.getNumVariables();
		int newLength = newD.getNumVariables();
		int oldIndex = oldD.searchVar(PROJLOCKEDKEY);
		int newIndex = newD.searchVar(PROJLOCKEDKEY);
		if (oldLength == newLength) {
			if (oldIndex != newIndex) return true;
			if (oldIndex < 0) return variablesDiffers(oldD, 0, newD, 0, oldLength);
			return variablesDiffers(oldD, 0, newD, 0, oldIndex) ||
				variablesDiffers(oldD, oldIndex + 1, newD, oldIndex + 1, oldLength - oldIndex - 1);
		}
		if (oldLength == newLength + 1) {
			if (oldIndex < 0 || oldIndex != ~newIndex) return true;
			return variablesDiffers(oldD, 0, newD, 0, oldIndex) ||
				variablesDiffers(oldD, oldIndex + 1, newD, ~newIndex, oldLength - oldIndex - 1);
		}
		if (newLength == oldIndex + 1) {
			if (newIndex < 0 || newIndex != ~oldIndex) return true;
			return variablesDiffers(oldD, 0, newD, 0, newIndex) ||
				variablesDiffers(oldD, newIndex, newD, newIndex + 1, newLength - newIndex - 1);
		}
		return true;
    }
    
    private boolean variablesDiffers(ImmutableElectricObject oldImmutable, int oldStart, ImmutableElectricObject newImmutable, int newStart, int count) {
        for (int i = 0; i < count; i++)
            if (oldImmutable.getVar(oldStart + i) != newImmutable.getVar(newStart + i)) return true;
        return false;
    }
    
	private static void queueCheck(CellId cellId, int batchNumber)
	{
		// see if the cell is already queued
		for(FCheck f : fCheckList)
		{
			if (f.entry == cellId && f.batchNumber == batchNumber) return;
		}

		FCheck f = new FCheck();
		f.entry = cellId;
		f.batchNumber = batchNumber;
		fCheckList.add(f);
	}

	/************************ SUPPORT ***********************/

	static boolean ensureRepository()
	{
		if (Project.getRepositoryLocation().length() == 0)
		{
			Job.getUserInterface().showInformationMessage(
				"Before entering a library, set a repository location in the 'Project Management' tab under General Preferences",
				"Must Setup Project Management");
			return true;
		}
		return false;
	}

	static void setChangeStatus(boolean quiet)
	{
		if (quiet) ignoreChanges = quiet;
		Input.changesQuiet(quiet);
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

	static boolean useNewestVersion(Cell oldCell, Cell newCell)
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
	static boolean writeCell(Cell cell, ProjectCell pc)
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
		boolean error = Output.writeLibrary(fLib, pc.getLibType(), false, true, false);
		if (error)
		{
			System.out.println("Could not save library with " + cell + " in it");
			fLib.kill("delete");
			return true;
		}
		fLib.kill("delete");
		return false;
	}

	static String getTempLibraryName()
	{
		for(int i=1; ; i++)
		{
			String libName = "projecttemp" + i;
			if (Library.findLibrary(libName) == null) return libName;
		}
	}

	static String describeFullCellName(Cell cell)
	{
		String cellName = cell.getName() + ";" + cell.getVersion();
		if (cell.getView() != View.UNKNOWN) cellName += "{" + cell.getView().getAbbreviation() + "}";
		return cellName;
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
