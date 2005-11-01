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
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.user.ViewChanges;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;


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
	private static final Variable.Key PROJPATHKEY   = Variable.newKey("PROJ_path");
	private static final Variable.Key PROJLIBRARYKEY = Variable.newKey("PROJ_library");
	private static final String PUSERFILE   = "projectusers";
	private static final String PROJECTFILE = "project.proj";

	/** the Project tool. */					private static Project tool = new Project();
	/** the users */							private static HashMap<String,String> usersMap;
	/** all libraries read in */				private static HashMap<Library,ProjectLibrary> libraryProjectInfo = new HashMap<Library,ProjectLibrary>();
	/** nonzero if the system is active */		private static boolean pmActive;
	/** nonzero to ignore broadcast changes */	private static boolean ignoreChanges;
	/** check modules */						private static List<FCheck>    fCheckList = new ArrayList<FCheck>();

	/**
	 * Each combination of cell and change-batch is queued by one of these objects.
	 */
	private static class FCheck
	{
		Cell   entry;
		int    batchNumber;
	};

	/**
	 * ProjectCell objects describe a specific version of a cell that is in the repository.
	 */
	private static class ProjectCell
	{
		/** name of the cell */								String   cellName;
		/** cell view */									View     cellView;
		/** cell version */									int      cellVersion;
		/** the type of the library file with this cell */	FileType libType;
		/** the actual cell (if known) */					Cell     cell;
		/** true if this is the latest version */			boolean  latestVersion;
		/** date of cell's checkin */						String   checkInDate;
		/** current owner of this cell (if checked out) */	String   owner;
		/** previous owner of this cell (if checked in) */	String   lastOwner;
		/** comments for this cell */						String   comment;
		/** library that contains this cell */				ProjectLibrary projLib;

		private static ProjectCell findProjectCell(Cell cell)
		{
			ProjectLibrary pl = ProjectLibrary.findProjectLibrary(cell.getLibrary());
			ProjectCell pc = (ProjectCell)pl.byCell.get(cell);
			return pc;
		}

		private String describe()
		{
			String cn = cellName;
			if (cellView != View.UNKNOWN) cn += "{" + cellView.getAbbreviation() + "}";
			return cn;
		}

		private String describeWithVersion()
		{
			String cn = cellName + ";" + cellVersion;
			if (cellView != View.UNKNOWN) cn += "{" + cellView.getAbbreviation() + "}";
			return cn;
		}
	}

	/**
	 * ProjectLibrary objects describe a library of cells in the repository.
	 */
	private static class ProjectLibrary
	{
		/** the project directory */				String                    projDirectory;
		/** Library associated with project file */	Library                   lib;
		/** all cell records in the project */		List<ProjectCell>         allCells;
		/** cell records by Cell in the project */	HashMap<Cell,ProjectCell> byCell;
		/** I/O channel for project file */			RandomAccessFile          raf;
		/** Lock on file when updating it */		FileLock                  lock;

		private ProjectLibrary()
		{
			allCells = new ArrayList<ProjectCell>();
			byCell = new HashMap<Cell,ProjectCell>();
		}

		/**
		 * Method to add a ProjectCell to this ProjectLibrary.
		 * Keeps the list sorted.
		 * @param pc the ProjectCell to add.
		 */
		private void addProjectCell(ProjectCell pc)
		{
			allCells.add(pc);
			Collections.sort(allCells, new OrderedProjectCells());
		}

		/**
		 * Method to remove a ProjectCell from this ProjectLibrary.
		 * Keeps the list sorted.
		 * @param pc the ProjectCell to remove.
		 */
		private void removeProjectCell(ProjectCell pc)
		{
			allCells.remove(pc);
			Collections.sort(allCells, new OrderedProjectCells());
		}

		/**
		 * Method to ensure that there is project information for a given library.
		 * @param lib the Library to check.
		 * @return a ProjectLibrary object for the Library.  If the library is marked
		 * as being part of a project, that project file is read in.  If the library is
		 * not in a project, the returned object has nothing in it.
		 */
		private static ProjectLibrary findProjectLibrary(Library lib)
		{
			// see if this library has a known project database
			ProjectLibrary pl = (ProjectLibrary)libraryProjectInfo.get(lib);
			if (pl != null) return pl;
			pl = createProject(lib);
			libraryProjectInfo.put(lib, pl);
			return pl;
		}

		private ProjectCell findProjectCellByNameView(String name, View view)
		{
			for(Iterator<ProjectCell> it = allCells.iterator(); it.hasNext(); )
			{
				ProjectCell pc = (ProjectCell)it.next();
				if (pc.cellName.equals(name) && pc.cellView == view) return pc;
			}
			return null;
		}

		private ProjectCell findProjectCellByNameViewVersion(String name, View view, int version)
		{
			for(Iterator<ProjectCell> it = allCells.iterator(); it.hasNext(); )
			{
				ProjectCell pc = (ProjectCell)it.next();
				if (pc.cellName.equals(name) && pc.cellView == view && pc.cellVersion == version) return pc;
			}
			return null;
		}

		private static ProjectLibrary createProject(Library lib)
		{
			// create a new project database
			ProjectLibrary pl = new ProjectLibrary();
			pl.lib = lib;

			// figure out the location of the project file
			Variable var = lib.getVar(PROJPATHKEY);
			if (var == null) return pl;
			URL url = TextUtils.makeURLToFile((String)var.getObject());
			if (!TextUtils.URLExists(url))
			{
				url = null;
				if (getRepositoryLocation().length() > 0)
				{
					url = TextUtils.makeURLToFile(getRepositoryLocation() + File.separator + lib.getName() + File.separator + PROJECTFILE);
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
			if (getRepositoryLocation().length() == 0)
			{
				String repositoryLocation = null;
				if (sepPos > 1)
				{
					int nextSepPos = projectFile.lastIndexOf('/', sepPos-1);
					if (nextSepPos >= 0) repositoryLocation = projectFile.substring(0, nextSepPos);
				}
				if (repositoryLocation == null)
				{
					JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
						"You should setup Project Management by choosing a Repository location.  Use the 'Project Management' tab under General Preferences",
						"Setup Project Management", JOptionPane.INFORMATION_MESSAGE);
				} else
				{
					setRepositoryLocation(repositoryLocation);
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
			pl.raf = null;
			return pl;
		}

		/**
		 * Method to lock this project file.
		 * @return true on error (no project file, cannot lock it).
		 * Also prints error message.
		 */
		private boolean lockProjectFile()
		{
			if (tryLockProjectFile())
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Cannot lock the project file.  It may be in use by another user, or it may be damaged.",
					"Internal Error", JOptionPane.ERROR_MESSAGE);
				return true;
			}
			return false;
		}

		/**
		 * Method to lock this project file.
		 * @return true on error (no project file, cannot lock it).
		 */
		private boolean tryLockProjectFile()
		{
			// prepare to read the project file
			String projectFile = projDirectory + File.separator + PROJECTFILE;
			try
			{
				raf = new RandomAccessFile(projectFile, "rw");
			} catch (FileNotFoundException e)
			{
				System.out.println("Cannot read file " + projectFile);
				return true;
			}

			FileChannel fc = raf.getChannel();
			try
			{
				lock = fc.lock();
			} catch (IOException e1)
			{
				System.out.println("Unable to lock project file");
				try
				{
					raf.close();
				} catch (IOException e2)
				{
					System.out.println("Unable to close project file");
				}
				raf = null;
				return true;
			}
			if (loadProjectFile())
			{
				try
				{
					lock.release();
					raf.close();
				} catch (IOException e)
				{
					System.out.println("Unable to release project file lock");
				}
				raf = null;
				return true;
			}
			return false;
		}

		/**
		 * Method to release the lock on this project file.
		 * @param save true to rewrite it first.
		 */
		private void releaseProjectFileLock(boolean save)
		{
			if (save)
			{
				FileChannel fc = raf.getChannel();
				try
				{
					fc.position(0);
					fc.truncate(0);
					for(Iterator<ProjectCell> it = allCells.iterator(); it.hasNext(); )
					{
						ProjectCell pc = (ProjectCell)it.next();
						String line = "::" + pc.cellName + ":" + pc.cellVersion + "-" +
							pc.cellView.getFullName() + "." + pc.libType.getExtensions()[0] + ":" +
							pc.owner + ":" + pc.lastOwner + ":" + pc.comment + "\n";
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
		 * Method to read the project file into memory.
		 * @return true on error.
		 */
		private boolean loadProjectFile()
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

				ProjectCell pc = new ProjectCell();
				pc.projLib = this;
				int prevPos = 0;
				for(int i=0; i<6; i++)
				{
					colonPos[i] = userLine.indexOf(':', prevPos);
					prevPos = colonPos[i] + 1;
					if (prevPos <= 0)
					{
						System.out.println("Too few keywords in project file: " + userLine);
						return true;
					}
				}
				if (colonPos[0] != 0)
				{
					System.out.println("Missing initial ':' in project file: " + userLine);
					return true;
				}

				// get cell name
				pc.cellName = userLine.substring(colonPos[1]+1, colonPos[2]);

				// get version
				String section = userLine.substring(colonPos[2]+1, colonPos[3]);
				int dashPos = section.indexOf('-');
				if (dashPos < 0)
				{
					System.out.println("Missing '-' after version number in project file: " + userLine);
					return true;
				}
				int dotPos = section.indexOf('.');
				if (dotPos < 0)
				{
					System.out.println("Missing '.' after view type in project file: " + userLine);
					return true;
				}
				pc.cellVersion = TextUtils.atoi(section.substring(0, dashPos));

				// get view
				String viewPart = section.substring(dashPos+1, dotPos);
				pc.cellView = View.findView(viewPart);

				// get file type
				String fileType = section.substring(dotPos+1);
				if (fileType.equals("elib")) pc.libType = FileType.ELIB; else
					if (fileType.equals("jelib")) pc.libType = FileType.JELIB; else
						if (fileType.equals("txt")) pc.libType = FileType.READABLEDUMP; else
				{
					System.out.println("Unknown library type in project file: " + userLine);
					return true;
				}

				// get owner
				pc.owner = userLine.substring(colonPos[3]+1, colonPos[4]);

				// get last owner
				pc.lastOwner = userLine.substring(colonPos[4]+1, colonPos[5]);

				// get comments
				pc.comment = userLine.substring(colonPos[5]+1);

				// check for duplication
				for(Iterator<ProjectCell> it = allCells.iterator(); it.hasNext(); )
				{
					ProjectCell opc = (ProjectCell)it.next();
					if (!opc.cellName.equalsIgnoreCase(pc.cellName)) continue;
					if (opc.cellView != pc.cellView) continue;
					if (opc.cellVersion != pc.cellVersion) continue;
					System.out.println("Error in project file: version " + pc.cellVersion + ", view '" +
						pc.cellView.getFullName() + "' of cell '" + pc.cellName + "' exists twice");
				}

				// find the cell associated with this entry
				pc.latestVersion = false;
				String cellName = pc.describeWithVersion();
				pc.cell = lib.findNodeProto(cellName);
				if (pc.cell != null)
				{
					if (pc.cell.getVersion() > pc.cellVersion)
					{
						if (!pc.owner.equals(getCurrentUserName()))
						{
							if (pc.owner.length() == 0)
							{
								System.out.println("WARNING: " + pc.cell + " is being edited, but it is not checked-out");
							} else
							{
								System.out.println("WARNING: " + pc.cell + " is being edited, but it is checked-out to " + pc.owner);
							}
						}
					}
					byCell.put(pc.cell, pc);
				}

				// link it in
				allCells.add(pc);
			}

			// determine the most recent views
			HashMap<String,ProjectCell> mostRecent = new HashMap<String,ProjectCell>();
			for(Iterator<ProjectCell> it = allCells.iterator(); it.hasNext(); )
			{
				ProjectCell pc = (ProjectCell)it.next();
				String cellEntry = pc.describe();
				ProjectCell recent = (ProjectCell)mostRecent.get(cellEntry);
				if (recent != null && recent.cellVersion > pc.cellVersion) continue;
				mostRecent.put(cellEntry, pc);
			}
			for(Iterator<ProjectCell> it = allCells.iterator(); it.hasNext(); )
			{
				ProjectCell pc = (ProjectCell)it.next();
				String cellEntry = pc.describe();
				ProjectCell recent = (ProjectCell)mostRecent.get(cellEntry);
				pc.latestVersion = (recent == pc);
			}
			return false;
		}
	}

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
		ProjectLibrary pl = ProjectLibrary.findProjectLibrary(lib);
		if (pl.allCells.size() == 0) return false;
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
		ProjectCell pc = ProjectCell.findProjectCell(newestVersion);
		if (pc == null) return NOTMANAGED;
		if (newestVersion != cell) return OLDVERSION;
		if (pc.owner.length() == 0) return CHECKEDIN;
		if (pc.owner.equals(getCurrentUserName())) return CHECKEDOUTTOYOU;
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
		ProjectCell pc = ProjectCell.findProjectCell(cell);
		if (pc == null) return "";
		return pc.owner;
	}

	/**
	 * Method to update the project libraries from the repository.
	 */
	public static void updateProject()
	{
		new UpdateJob();
		pmActive = true;
	}

	/**
	 * Method to check the currently edited cell back into the repository.
	 */
	public static void checkInThisCell()
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		checkIn(cell);
	}

	public static void checkIn(Cell cell)
	{
		pmActive = true;
		HashMap<Cell,MutableInteger> cellsMarked = markRelatedCells(cell);
		new CheckInJob(cell.getLibrary(), cellsMarked);
	}

	/**
	 * Method to check the currently edited cell out of the repository.
	 */
	public static void checkOutThisCell()
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		checkOut(cell);
	}

	public static void checkOut(Cell oldVers)
	{
		pmActive = true;
		new CheckOutJob(oldVers);
	}

	/**
	 * Method to cancel the check-out of the currently edited cell.
	 */
	public static void cancelCheckOutThisCell()
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		cancelCheckOut(cell);
	}

	public static void cancelCheckOut(Cell oldVers)
	{
		pmActive = true;

		int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(),
			"Cancel all changes to the checked-out " + oldVers + " and revert to the checked-in version?");
		if (response != JOptionPane.YES_OPTION) return;
		new CancelCheckOutJob(oldVers);
	}

	/**
	 * Method to add the currently edited cell to the repository.
	 */
	public static void addThisCell()
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		addCell(cell);
	}

	/**
	 * Method to add a cell to the repository.
	 */
	public static void addCell(Cell cell)
	{
		pmActive = true;
		new AddCellJob(cell);
	}

	/**
	 * Method to remove the currently edited cell from the repository.
	 */
	public static void removeThisCell()
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		removeCell(cell);
	}

	/**
	 * Method to remove a cell from the repository.
	 */
	public static void removeCell(Cell cell)
	{
		pmActive = true;
		new DeleteCellJob(cell);
	}

	/**
	 * Method to examine the history of the currently edited cell.
	 */
	public static void examineThisHistory()
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		examineHistory(cell);
	}

	public static void examineHistory(Cell oldVers)
	{
		pmActive = true;
		new ShowHistory(oldVers);
	}

	public static void getALibrary()
	{
		pmActive = true;
		new ShowLibsInRepository();
	}

	/**
	 * Method to add the current library to the repository.
	 */
	public static void addThisLibrary()
	{
		addALibrary(Library.getCurrent());
	}

	/**
	 * Method to add a library to the repository.
	 */
	public static void addALibrary(Library lib)
	{
		new AddLibraryJob(lib);
		pmActive = true;
	}

	public static int getNumUsers()
	{
		ensureUserList();
		return usersMap.size();
	}

	public static Iterator<String> getUsers()
	{
		ensureUserList();
		return usersMap.keySet().iterator();
	}

	public static boolean isExistingUser(String user)
	{
		ensureUserList();
		return usersMap.get(user) != null;
	}

	public static void deleteUser(String user)
	{
		usersMap.remove(user);
		saveUserList();
	}

	public static void addUser(String user, String encryptedPassword)
	{
		usersMap.put(user, encryptedPassword);
		saveUserList();
	}

	public static String getEncryptedPassword(String user)
	{
		return (String)usersMap.get(user);
	}

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
			Cell cell = (Cell)it.next();
			if (cell.getVar(PROJLOCKEDKEY) != null)
			{
				pmActive = true;

				// see if this library has a known project database
				ProjectLibrary pl = (ProjectLibrary)libraryProjectInfo.get(lib);
				if (pl == null)
				{
					pl = ProjectLibrary.createProject(lib);
					libraryProjectInfo.put(lib, pl);
				}
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

	private static void detectIllegalChanges()
	{
		if (!pmActive) return;
		if (fCheckList.size() == 0) return;

		int undoneCells = 0;
		int lowBatch = Integer.MAX_VALUE;
		String errorMsg = "";
		for(Iterator<FCheck> it = fCheckList.iterator(); it.hasNext(); )
		{
			FCheck f = (FCheck)it.next();
			Cell cell = f.entry;
			if (cell == null) continue;

			// make sure cell is checked-out
			if (cell.getVar(PROJLOCKEDKEY) != null)
			{
				if (undoneCells != 0) errorMsg += ", ";
				errorMsg += cell.describe(true);
				undoneCells++;
				if (f.batchNumber < lowBatch) lowBatch = f.batchNumber;
			}
		}
		fCheckList.clear();

		if (undoneCells > 0)
		{
			new UndoBatchesJob(lowBatch, errorMsg);
		}
	}

	/**
	 * This class checks out a cell from Project Management.
	 * It involves updating the project database and making a new version of the cell.
	 */
	private static class UndoBatchesJob extends Job
	{
		private int lowestBatch;
		private String errorMsg;

		protected UndoBatchesJob(int lowestBatch, String errorMsg)
		{
			super("Undo changes to locked cells", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.lowestBatch = lowestBatch;
			this.errorMsg = errorMsg;
			startJob();
		}

		public boolean doIt()
		{
			System.out.println("Cannot change unchecked-out cells: " + errorMsg);
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

    private boolean variablesDiffers(ImmutableElectricObject oldImmutable, ImmutableElectricObject newImmutable) {
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
		for(Iterator<FCheck> it = fCheckList.iterator(); it.hasNext(); )
		{
			FCheck f = (FCheck)it.next();
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
		private Cell oldVers;

		protected CheckOutJob(Cell oldVers)
		{
			super("Check out " + oldVers, tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.oldVers = oldVers;
			startJob();
		}

		public boolean doIt()
		{
			Library lib = oldVers.getLibrary();
			ProjectLibrary pl = ProjectLibrary.findProjectLibrary(lib);

			// make sure there is a valid user name
			if (needUserName()) return false;

			if (pl.lockProjectFile()) return false;

			// make a list of newer versions of this cell
			List<ProjectCell> newerProjectCells = new ArrayList<ProjectCell>();
			for(Iterator<ProjectCell> it = pl.allCells.iterator(); it.hasNext(); )
			{
				ProjectCell pc = (ProjectCell)it.next();
				if (pc.cellName.equals(oldVers.getName()) && pc.cellView == oldVers.getView())
				{
					if (pc.cellVersion > oldVers.getVersion()) newerProjectCells.add(pc);
				}
			}
			for(Iterator<ProjectCell> it = newerProjectCells.iterator(); it.hasNext(); )
			{
				ProjectCell pc = (ProjectCell)it.next();
				if (pc.owner.length() == 0)
				{
					JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
						"A more recent version of this cell is in the repository.  Do an update first.",
						"Check-Out Error", JOptionPane.ERROR_MESSAGE);
					pl.releaseProjectFileLock(true);
					return false;
				}
			}
			for(Iterator<ProjectCell> it = newerProjectCells.iterator(); it.hasNext(); )
			{
				ProjectCell pc = (ProjectCell)it.next();
				{
					if (pc.owner.equals(getCurrentUserName()))
					{
						JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
							"You already checked-out this cell, but the changes are not in the current library.  Checking it out again.",
							"Check-Out Warning", JOptionPane.WARNING_MESSAGE);
						pl.removeProjectCell(pc);
						if (pc.cell != null) pl.byCell.remove(pc.cell);
					} else
					{
						JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
							"Cannot check-out cell.  It is checked-out to '" + pc.owner + "'",
							"Check-Out Error", JOptionPane.ERROR_MESSAGE);
						pl.releaseProjectFileLock(true);
						return false;
					}
				}
			}

			// find this in the project file
			Cell newVers = null;
			boolean worked = false;
			ProjectCell pc = (ProjectCell)pl.byCell.get(oldVers);
			if (pc == null)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"This cell is not in the project.  You must add it to the project before being able to check it out and in.",
					"Check Out Error", JOptionPane.ERROR_MESSAGE);
			} else
			{
				// see if it is available
				if (pc.owner.length() != 0)
				{
					if (pc.owner.equals(getCurrentUserName()))
					{
						JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
							"This cell is already checked out to you.",
							"Check Out Error", JOptionPane.ERROR_MESSAGE);
						markLocked(oldVers, false);
					} else
					{
						JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
							"Cannot check this cell out because it is already checked out to '" + pc.owner + "'",
							"Check Out Error", JOptionPane.ERROR_MESSAGE);
					}
				} else
				{
					// make sure we have the latest version
					if (pc.cellVersion > oldVers.getVersion())
					{
						JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
							"Cannot check out " + oldVers +
							" because you don't have the latest version (yours is " + oldVers.getVersion() + ", project has " +
							pc.cellVersion + ").  Do an 'update' first",
							"Check Out Error", JOptionPane.ERROR_MESSAGE);
					} else
					{
						// prevent tools (including this one) from seeing the change
						setChangeStatus(true);

						// make new version
						newVers = Cell.copyNodeProto(oldVers, lib, oldVers.getName(), true);

						if (newVers == null)
						{
							JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
								"Error making new version of cell",
								"Check Out Error", JOptionPane.ERROR_MESSAGE);
						} else
						{
							// replace former usage with new version
							if (useNewestVersion(oldVers, newVers))
							{
								JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
									"Error replacing instances of new " + oldVers,
									"Check Out Error", JOptionPane.ERROR_MESSAGE);
							} else
							{
								// update record for the cell
								ProjectCell newPC = new ProjectCell();
								newPC.cell = newVers;
								newPC.latestVersion = true;
								newPC.cellName = pc.cellName;
								newPC.cellVersion = newVers.getVersion();
								newPC.cellView = pc.cellView;
								newPC.comment = "CHECKED OUT";
								newPC.lastOwner = "";
								newPC.libType = pc.libType;
								newPC.owner = getCurrentUserName();
								newPC.projLib = pl;
								pc.latestVersion = false;
								pc.cell = null;

								pl.byCell.remove(oldVers);
								pl.byCell.put(newVers, newPC);
								pl.addProjectCell(newPC);
								markLocked(newVers, false);
								worked = true;
							}
						}

						// restore tool state
						lib.setChanged();

						setChangeStatus(false);
					}
				}
			}

			// relase project file lock
			pl.releaseProjectFileLock(true);

			// if it worked, print dependencies and display
			if (worked)
			{
				System.out.println("Cell " + newVers.describe(true) + " checked out for your use");

				// advise of possible problems with other checkouts higher up in the hierarchy
				HashMap<Cell,MutableInteger> cellsMarked = new HashMap<Cell,MutableInteger>();
				for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
				{
					Library oLib = (Library)it.next();
					for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
					{
						Cell cell = (Cell)cIt.next();
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
						Library oLib = (Library)it.next();
						for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
						{
							Cell cell = (Cell)cIt.next();
							MutableInteger val = (MutableInteger)cellsMarked.get(cell);
							if (val.intValue() == 1)
							{
								propagated = true;
								val.setValue(2);
								for(Iterator<NodeInst> nIt = cell.getInstancesOf(); nIt.hasNext(); )
								{
									NodeInst ni = (NodeInst)nIt.next();
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
					Library oLib = (Library)it.next();
					for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
					{
						Cell cell = (Cell)cIt.next();
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
						Library oLib = (Library)it.next();
						for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
						{
							Cell cell = (Cell)cIt.next();
							MutableInteger val = (MutableInteger)cellsMarked.get(cell);
							if (val.intValue() != 3) continue;
							System.out.println("    " + cell + " is checked out to " + getCellOwner(cell));
						}
					}
				}

				// advise of possible problems with other checkouts lower down in the hierarchy
				for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
				{
					Library oLib = (Library)it.next();
					for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
					{
						Cell cell = (Cell)cIt.next();
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
						Library oLib = (Library)it.next();
						for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
						{
							Cell cell = (Cell)cIt.next();
							MutableInteger val = (MutableInteger)cellsMarked.get(cell);
							if (val.intValue() == 1)
							{
								propagated = true;
								val.setValue(2);
								for(Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
								{
									NodeInst ni = (NodeInst)nIt.next();
									if (!(ni.getProto() instanceof Cell)) continue;
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
					Library oLib = (Library)it.next();
					for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
					{
						Cell cell = (Cell)cIt.next();
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
						Library oLib = (Library)it.next();
						for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
						{
							Cell cell = (Cell)cIt.next();
							MutableInteger val = (MutableInteger)cellsMarked.get(cell);
							if (val.intValue() != 3) continue;
							String owner = getCellOwner(cell);
							System.out.println("    " + cell + " is checked out to " + owner);
						}
					}
				}
			}
			return true;
		}
	}

	/**
	 * This class checks in cells to Project Management.
	 * It involves updating the project database and saving the current cells to disk.
	 */
	private static class CancelCheckOutJob extends Job
	{
		private Cell cell;

		protected CancelCheckOutJob(Cell cell)
		{
			super("Cancel Check-out " + cell, tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt()
		{
			// make sure there is a valid user name
			if (needUserName()) return false;

			Library lib = cell.getLibrary();
			ProjectLibrary pl = ProjectLibrary.findProjectLibrary(lib);
			if (pl.lockProjectFile()) return false;

			// prevent tools (including this one) from seeing the change
			setChangeStatus(true);

			ProjectCell cancelled = null;
			ProjectCell former = null;
			for(Iterator<ProjectCell> it = pl.allCells.iterator(); it.hasNext(); )
			{
				ProjectCell pc = (ProjectCell)it.next();
				if (pc.cellName.equals(cell.getName()) && pc.cellView == cell.getView())
				{
					if (pc.cellVersion >= cell.getVersion())
					{
						if (pc.owner.length() > 0)
						{
							if (pc.owner.equals(getCurrentUserName()))
							{
								cancelled = pc;
							} else
							{
								JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
									"This cell is not checked out to you.  Only user '" + pc.owner + "' can cancel the check-out.",
									"Error Cancelling Check-out", JOptionPane.ERROR_MESSAGE);
								setChangeStatus(false);
								pl.releaseProjectFileLock(true);
								return false;
							}
						}
					} else
					{
						// find most recent former version
						if (former != null && former.cellVersion < pc.cellVersion) former = null;
						if (former == null) former = pc;
					}
				}
			}

			if (cancelled == null)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"This cell is not checked out.",
					"Error Cancelling Check-out", JOptionPane.ERROR_MESSAGE);
				setChangeStatus(false);
				pl.releaseProjectFileLock(true);
				return false;
			}

			if (former == null)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Cannot find former version to restore.",
					"Error Cancelling Check-out", JOptionPane.ERROR_MESSAGE);
				setChangeStatus(false);
				pl.releaseProjectFileLock(true);
				return false;
			}

			// replace former usage with new version
			getCellFromRepository(former, lib, false);
			if (former.cell == null)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Error bringing in former version (" + former.cellVersion + ")",
					"Error Cancelling Check-out", JOptionPane.ERROR_MESSAGE);
				setChangeStatus(false);
				pl.releaseProjectFileLock(true);
				return false;
			}

			if (useNewestVersion(cell, former.cell))
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Error replacing instances of former " + cell,
					"Error Cancelling Check-out", JOptionPane.ERROR_MESSAGE);
				setChangeStatus(false);
				pl.releaseProjectFileLock(true);
				return false;
			}

			pl.removeProjectCell(cancelled);
			if (cancelled.cell != null)
			{
				markLocked(cancelled.cell, true);
				pl.byCell.remove(cancelled.cell);
			}
			former.latestVersion = true;

			// restore change broadcast
			setChangeStatus(false);

			// relase project file lock
			pl.releaseProjectFileLock(true);

			// update explorer tree
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run() { WindowFrame.wantToRedoLibraryTree(); }
			});

			return true;
		}
	}

	/**
	 * This class checks in cells to Project Management.
	 * It involves updating the project database and saving the current cells to disk.
	 */
	private static class CheckInJob extends Job
	{
		private Library lib;
		private HashMap<Cell,MutableInteger> cellsMarked;

		protected CheckInJob(Library lib, HashMap<Cell,MutableInteger> cellsMarked)
		{
			super("Check in cells", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.lib = lib;
			this.cellsMarked = cellsMarked;
			startJob();
		}

		public boolean doIt()
		{
			// make sure there is a valid user name
			if (needUserName()) return false;

			ProjectLibrary pl = ProjectLibrary.findProjectLibrary(lib);
			if (pl.lockProjectFile()) return false;

			// prevent tools (including this one) from seeing the change
			setChangeStatus(true);

			// check in the requested cells
			String cellNames = "";
			for(Iterator<Cell> it = cellsMarked.keySet().iterator(); it.hasNext(); )
			{
				Cell cell = (Cell)it.next();
				MutableInteger mi = (MutableInteger)cellsMarked.get(cell);
				if (mi.intValue() == 0) continue;
				if (cellNames.length() > 0) cellNames += ", ";
				cellNames += cell.describe(false);
			}

			String comment = null;
			for(Iterator<Cell> it = cellsMarked.keySet().iterator(); it.hasNext(); )
			{
				Cell cell = (Cell)it.next();
				MutableInteger mi = (MutableInteger)cellsMarked.get(cell);
				if (mi.intValue() == 0) continue;

				// find this in the project file
				ProjectCell pc = ProjectCell.findProjectCell(cell);
				if (pc == null)
				{
					JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
						"Cell " + cell.describe(true) + " is not in the project.  Add it before checking it in or out.",
						"Check In Error", JOptionPane.ERROR_MESSAGE);
				} else
				{
					// see if it is available
					if (!pc.owner.equals(getCurrentUserName()))
					{
						JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
							"You cannot check-in " + cell + " because it is checked out to '" + pc.owner + "', not you.",
							"Check In Error", JOptionPane.ERROR_MESSAGE);
					} else
					{
						if (comment == null)
							comment = JOptionPane.showInputDialog("Reason for checking-in " + cellNames, "");
						if (comment == null) break;

						// write the cell out there
						if (writeCell(cell, pc))
						{
							JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
								"Error writing " + cell,
								"Check In Error", JOptionPane.ERROR_MESSAGE);
						} else
						{
							pc.owner = "";
							pc.lastOwner = getCurrentUserName();
							pc.cellVersion = cell.getVersion();
							pc.comment = comment;
							markLocked(cell, true);
							System.out.println("Cell " + cell.describe(true) + " checked in");
						}
					}
				}
			}

			// restore change broadcast
			setChangeStatus(false);

			// relase project file lock
			pl.releaseProjectFileLock(true);
			return true;
		}
	}

	/**
	 * This class displays a dialog for examining a cell's old versions.
	 */
	private static class ShowHistory extends EDialog
	{
		private Cell cell;
		private TableModel dataModel;
		private JTable table;

		private ShowHistory(Cell cell)
		{
			super(null, true);
			this.cell = cell;
			initComponents();
			setVisible(true);
		}

		protected void escapePressed() { doButton(false); }

		private void doButton(boolean retrieve)
		{
			if (retrieve)
			{
				int index = table.getSelectedRow();
				int version = TextUtils.atoi((String)dataModel.getValueAt(index, 0));
				new GetOldVersionJob(cell, version);
			} else
			{
				dispose();
			}
		}

		private void initComponents()
		{
			getContentPane().setLayout(new GridBagLayout());

			setTitle("Examine the History of " + cell);
			setName("");
			addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent evt) { doButton(false); }
			});

			// gather versions found in the project file
			ProjectLibrary pl = ProjectLibrary.findProjectLibrary(cell.getLibrary());
			List<ProjectCell> versions = new ArrayList<ProjectCell>();
			for(Iterator<ProjectCell> it = pl.allCells.iterator(); it.hasNext(); )
			{
				ProjectCell pc = (ProjectCell)it.next();
				if (pc.cellName.equals(cell.getName()) && pc.cellView == cell.getView())
				{
					pc.checkInDate = "Not In Repository Yet";
					versions.add(pc);
				}
			}

//for(Iterator it = pl.allCells.iterator(); it.hasNext(); )
//{
//	ProjectCell xx = (ProjectCell)it.next();
//	System.out.println(xx.cellName+";"+xx.cellVersion+"{"+xx.cellView.getAbbreviation()+"} is cell: "+xx.cell+
//		" owner="+xx.owner+" lastOwner="+xx.lastOwner);
//}
			// consider the files in the repository, too
			String dirName = pl.projDirectory + File.separator + cell.getName();
			File dir = new File(dirName);
			File [] filesInDir = dir.listFiles();
			for(int i=0; i<filesInDir.length; i++)
			{
				File subFile = filesInDir[i];
				Date modDate = new Date(subFile.lastModified());
				int version = TextUtils.atoi(subFile.getName());
				boolean found = false;
				for(Iterator<ProjectCell> it = versions.iterator(); it.hasNext(); )
				{
					ProjectCell pc = (ProjectCell)it.next();
					if (pc.cellVersion == version)
					{
						pc.checkInDate = TextUtils.formatDate(modDate);
						found = true;
						break;
					}
				}
				if (!found)
				{
					ProjectCell pc = new ProjectCell();
					pc.cellName = cell.getName();
					pc.cellVersion = version;
					pc.checkInDate = TextUtils.formatDate(modDate);
					pc.projLib = pl;
					versions.add(pc);
				}
			}

			// sort the list by versions
			Collections.sort(versions, new ProjectCellByVersion());

			// make table
			int numVersions = versions.size();
			Object [][] data = new Object[numVersions][4];
			int index = 0;
			for(Iterator<ProjectCell> it = versions.iterator(); it.hasNext(); )
			{
				ProjectCell pc = (ProjectCell)it.next();
				data[index][0] = Integer.toString(pc.cellVersion);
				data[index][1] = pc.checkInDate;
				data[index][2] = pc.lastOwner;
				if (pc.owner.length() > 0) data[index][2] = pc.owner;
				data[index][3] = pc.comment;
				index++;
			}

			dataModel = new HistoryTableModel(data);
			table = new JTable(dataModel);
//			table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			TableColumn versCol = table.getColumnModel().getColumn(0);
			TableColumn dateCol = table.getColumnModel().getColumn(1);
			TableColumn userCol = table.getColumnModel().getColumn(2);
			TableColumn commentCol = table.getColumnModel().getColumn(3);
			versCol.setPreferredWidth(10);
			dateCol.setPreferredWidth(30);
			userCol.setPreferredWidth(20);
			commentCol.setPreferredWidth(40);
			JScrollPane tableScrollPane = new JScrollPane(table);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 0;
			gbc.gridwidth = 2;
			gbc.weightx = gbc.weighty = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(tableScrollPane, gbc);

			// OK and Cancel
			JButton ok = new JButton("Retrieve");
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 1;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(ok, gbc);
			ok.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { doButton(true); }
			});

			JButton cancel = new JButton("Done");
			getRootPane().setDefaultButton(cancel);
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 1;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(cancel, gbc);
			cancel.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { doButton(false); }
			});

			pack();
		}

		class HistoryTableModel extends AbstractTableModel
		{
	        private String[] columnNames;
	        private Object[][] data;

	        HistoryTableModel(Object [][] data)
	        {
	        	this.data = data;
	        	columnNames = new String[] {"Version", "Date", "Who", "Comments"};
	        }

	        public int getColumnCount() { return columnNames.length; }

	        public int getRowCount() { return data.length; }

	        public String getColumnName(int col) { return columnNames[col]; }

	        public Object getValueAt(int row, int col) { return data[row][col]; }

	        public Class<?> getColumnClass(int c) { return getValueAt(0, c).getClass(); }
	    }
	}

	/**
	 * This class displays a dialog for selecting a library in the repository.
	 */
	private static class ShowLibsInRepository extends EDialog
	{
		private JList libList;
		private DefaultListModel libModel;

		private ShowLibsInRepository()
		{
			super(null, true);
			initComponents();
			setVisible(true);
		}

		protected void escapePressed() { doButton(false); }

		private void doButton(boolean retrieve)
		{
			if (retrieve)
			{
				int index = libList.getSelectedIndex();
				String libName = (String)libModel.getElementAt(index);
				new RetrieveLibraryFromRepositoryJob(libName);
			}
			dispose();
		}

		private void initComponents()
		{
			getContentPane().setLayout(new GridBagLayout());

			setTitle("Retrieve a Library from the Repository");
			setName("");
			addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent evt) { doButton(false); }
			});

			JScrollPane libPane = new JScrollPane();
			libModel = new DefaultListModel();
			libList = new JList(libModel);
			libList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			libPane.setViewportView(libList);
			libList.clearSelection();
			libList.addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2) doButton(true);
				}
			});

			// consider the files in the repository, too
			String dirName = getRepositoryLocation();
			File dir = new File(dirName);
			File [] filesInDir = dir.listFiles();
			for(int i=0; i<filesInDir.length; i++)
			{
				File subFile = filesInDir[i];
				if (subFile.isDirectory())
				libModel.addElement(subFile.getName());
			}

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 0;
			gbc.gridwidth = 2;
			gbc.weightx = gbc.weighty = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(libPane, gbc);

			// OK and Cancel
			JButton ok = new JButton("OK");
			getRootPane().setDefaultButton(ok);
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 1;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(ok, gbc);
			ok.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { doButton(true); }
			});

			JButton cancel = new JButton("Cancel");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 1;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(cancel, gbc);
			cancel.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { doButton(false); }
			});

			pack();
		}
	}

	/**
	 * This class gets old versions of cells from the Project Management repository.
	 */
	private static class GetOldVersionJob extends Job
	{
		private Cell cell;
		private int version;

		protected GetOldVersionJob(Cell cell, int version)
		{
			super("Update " + cell, tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.version = version;
			startJob();
		}

		public boolean doIt()
		{
			Library lib = cell.getLibrary();
			ProjectLibrary pl = ProjectLibrary.findProjectLibrary(lib);

			String cellName = cell.getName() + ";" + version;
			if (cell.getView() != View.UNKNOWN) cellName += "{" + cell.getView().getAbbreviation() + "}";
			Cell exists = cell.getLibrary().findNodeProto(cellName);
			if (exists != null)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Version " + version + " of cell " + cell.getName() + " is already in your library",
					"Cannot Retrieve Version", JOptionPane.ERROR_MESSAGE);
				return false;
			}

			ProjectCell foundPC = pl.findProjectCellByNameViewVersion(cell.getName(), cell.getView(), version);
			if (foundPC == null)
			{
				System.out.println("Can't find that version in the repository!");
				return false;
			}

			// prevent tools (including this one) from seeing the change
			setChangeStatus(true);

			getCellFromRepository(foundPC, lib, false);
			if (foundPC.cell == null)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Error retrieving old version of cell",
					"Error Getting Old Versions", JOptionPane.ERROR_MESSAGE);
			}
			markLocked(foundPC.cell, false);

			// allow changes
			setChangeStatus(false);

			System.out.println("Cell " + foundPC.cell.describe(true) + " is now in this library");
			return true;
		}
	}

	/**
	 * This class gets old versions of cells from the Project Management repository.
	 */
	private static class RetrieveLibraryFromRepositoryJob extends Job
	{
		private String libName;

		protected RetrieveLibraryFromRepositoryJob(String libName)
		{
			super("Retrieve Library from Repository", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.libName = libName;
			startJob();
		}

		public boolean doIt()
		{
			Library lib = Library.findLibrary(libName);
			if (lib != null)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Library '" + lib.getName() + "' already exists",
					"Cannot Retrieve Library", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			lib = Library.newInstance(libName, null);
			String projFile = Project.getRepositoryLocation() + File.separator + libName + File.separator + PROJECTFILE;
			File pf = new File(projFile);
			if (!pf.exists())
			{
				System.out.println("Cannot find project file '" + projFile + "'...retrieve aborted.");
				return false;
			}
			lib.newVar(PROJPATHKEY, projFile);

			ProjectLibrary pl = ProjectLibrary.findProjectLibrary(lib);

			// prevent tools (including this one) from seeing the change
			setChangeStatus(true);

			// get all recent cells
			String userName = getCurrentUserName();
			for(Iterator<ProjectCell> it = pl.allCells.iterator(); it.hasNext(); )
			{
				ProjectCell pc = (ProjectCell)it.next();
				if (!pc.latestVersion) continue;
				if (pc.cell == null)
				{
					getCellFromRepository(pc, lib, true);
					if (pc.cell == null)
					{
						JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
							"Error retrieving old version of cell",
							"Error Getting Old Versions", JOptionPane.ERROR_MESSAGE);
					}
				}
				if (pc.cell != null)
				{
					boolean youOwn = userName.length() > 0 && pc.owner.equals(userName);
					markLocked(pc.cell, !youOwn);
				}
			}

			// allow changes
			setChangeStatus(false);

			System.out.println("Library " + lib.getName() + " has been retrieved from the repository");
			return true;
		}
	}

	/**
	 * This class updates cells from the Project Management repository.
	 */
	private static class UpdateJob extends Job
	{
		protected UpdateJob()
		{
			super("Update all Cells from Repository", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			startJob();
		}

		public boolean doIt()
		{
			// make sure there is a valid user name
			if (needUserName()) return false;

			// make a list of all cells that need to be updated
			List<ProjectCell> updatedProjectCells = new ArrayList<ProjectCell>();
			for(Iterator<Library> lIt = Library.getLibraries(); lIt.hasNext(); )
			{
				Library lib = (Library)lIt.next();
				if (lib.isHidden()) continue;
				ProjectLibrary pl = ProjectLibrary.findProjectLibrary(lib);
				if (!pl.lockProjectFile()) pl.releaseProjectFileLock(false);

				// add ProjectCells that need to be updated to the list
				addNewProjectCells(pl, updatedProjectCells);
			}

			// lock library projects
			HashSet<ProjectLibrary> libsLocked = new HashSet<ProjectLibrary>();
			boolean allLocked = true;
			for(Iterator<ProjectCell> it = updatedProjectCells.iterator(); it.hasNext(); )
			{
				ProjectCell pc = (ProjectCell)it.next();
				ProjectLibrary pl = pc.projLib;
				if (libsLocked.contains(pl)) continue;
				if (pl.lockProjectFile()) { allLocked = false;   break; }
				libsLocked.add(pl);
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
					ProjectCell pc = (ProjectCell)it.next();
					total += updateCellFromRepository(pc, updatedProjectCells);
				}

				// restore change broadcast
				setChangeStatus(false);
			}

			// relase project file locks and validate all cell locks
			for(Iterator<ProjectLibrary> it = libsLocked.iterator(); it.hasNext(); )
			{
				ProjectLibrary pl = (ProjectLibrary)it.next();
				pl.releaseProjectFileLock(false);
				validateLocks(pl.lib);
			}

			// summarize
			if (total == 0) System.out.println("Project is up-to-date"); else
				System.out.println("Updated " + total + " cells");
			return true;
		}
	}
	/**
	 * This class adds the current library to the Project Management repository.
	 */
	private static class AddLibraryJob extends Job
	{
		private Library lib;

		protected AddLibraryJob(Library lib)
		{
			super("Add Library", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.lib = lib;
			startJob();
		}

		public boolean doIt()
		{
			if (getRepositoryLocation().length() == 0)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Before entering a library, set a repository location in the 'Project Management' tab under General Preferences",
					"Must Setup Project Management", JOptionPane.INFORMATION_MESSAGE);
					return false;
			}

			ProjectLibrary pl = ProjectLibrary.findProjectLibrary(lib);
			if (pl.allCells.size() != 0)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"This library is already in the repository",
					"Error Adding Library", JOptionPane.ERROR_MESSAGE);
				return false;
			}

			// verify that a project is to be built
			int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(),
				"Are you sure you want to enter this library into the repository?");
			if (response != JOptionPane.YES_OPTION) return false;

			// get path prefix for cell libraries
			String libraryname = lib.getName();

			// create the top-level directory for this library
			pl.projDirectory = Project.getRepositoryLocation() + File.separator + libraryname;
			File dir = new File(pl.projDirectory);
			if (dir.exists())
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Repository directory '" + pl.projDirectory + "' already exists",
					"Error Adding Library", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			if (!dir.mkdir())
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Could not create repository directory '" + pl.projDirectory + "'",
					"Error Adding Library", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			System.out.println("Making repository directory '" + pl.projDirectory + "'...");

			// turn off all tools
			setChangeStatus(true);

			// make libraries for every cell
			for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
			{
				Cell cell = (Cell)it.next();

				ProjectCell pc = new ProjectCell();
				pc.cellName = cell.getName();
				pc.cellView = cell.getView();
				pc.cellVersion = cell.getVersion();
				pc.owner = "";
				pc.lastOwner = getCurrentUserName();
				pc.comment = "Initial checkin";
				pc.libType = FileType.JELIB;
				pc.cell = cell;
				pc.latestVersion = true;
				pc.projLib = pl;

				// ignore old unused cell versions
				if (cell.getNewestVersion() != cell)
				{
					if (cell.getNumUsagesIn() == 0) continue;
					System.out.println("Warning: including old version of " + cell);
					pc.latestVersion = false;
				}

				// link the cell into the project lists
				pl.addProjectCell(pc);
				pl.byCell.put(cell, pc);

				if (writeCell(cell, pc)) System.out.println("Error writing cell file"); else
				{
					// write the cell to disk in its own library
					System.out.println("Entering " + cell);

					// mark this cell "checked in" and locked
					markLocked(cell, true);
				}
			}

			// create the project file
			String projfile = pl.projDirectory + File.separator + PROJECTFILE;
			lib.newVar(PROJPATHKEY, projfile);
			try
			{
				PrintStream buffWriter = new PrintStream(new FileOutputStream(projfile));
				for(Iterator<ProjectCell> it = pl.allCells.iterator(); it.hasNext(); )
				{
					ProjectCell pc = (ProjectCell)it.next();
					buffWriter.println("::" + pc.cellName + ":" + pc.cellVersion + "-" +
						pc.cellView.getFullName() + "." + pc.libType.getExtensions()[0] + ":" +
						pc.owner + ":" + pc.lastOwner + ":" + pc.comment);
				}
				buffWriter.close();
			} catch (IOException e)
			{
				System.out.println("Error creating " + projfile);
			}

			// restore tool state
			setChangeStatus(false);

			// advise the user of this library
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"The current library has been checked-into the repository and marked appropriately.",
				"Library Added", JOptionPane.INFORMATION_MESSAGE);
			return true;
		}
	}

	/**
	 * This class adds a cell to the Project Management repository.
	 */
	private static class AddCellJob extends Job
	{
		private Cell cell;

		protected AddCellJob(Cell cell)
		{
			super("Add " + cell, tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt()
		{
			if (cell.getNewestVersion() != cell)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Cannot add an old version of the cell",
					"Error Adding Cell", JOptionPane.ERROR_MESSAGE);
				return false;
			}

			// make sure there is a valid user name
			if (needUserName()) return false;

			Library lib = cell.getLibrary();
			ProjectLibrary pl = ProjectLibrary.findProjectLibrary(lib);
			if (pl.lockProjectFile()) return false;

			// prevent tools (including this one) from seeing the change
			setChangeStatus(true);

			// find this in the project file
			ProjectCell foundPC = pl.findProjectCellByNameView(cell.getName(), cell.getView());
			if (foundPC != null)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"This cell is already in the repository",
					"Error Adding Cell", JOptionPane.ERROR_MESSAGE);
			} else
			{
				// create new entry for this cell
				ProjectCell pc = new ProjectCell();
				pc.cellName = cell.getName();
				pc.cellView = cell.getView();
				pc.cellVersion = cell.getVersion();
				pc.owner = "";
				pc.lastOwner = getCurrentUserName();
				pc.comment = "Initial checkin";
				pc.latestVersion = true;
				pc.projLib = pl;
				pc.libType = FileType.JELIB;
				pc.cell = cell;

				if (writeCell(cell, pc))
				{
					JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
						"Error writing the cell to the repository",
						"Error Adding Cell", JOptionPane.ERROR_MESSAGE);
				} else
				{
					// link it in
					pl.addProjectCell(pc);
					pl.byCell.put(cell, pc);

					// mark this cell "checked in" and locked
					markLocked(cell, true);

					System.out.println("Cell " + cell.describe(true) + " added to the project");
				}
			}

			// restore change broadcast
			setChangeStatus(false);

			// relase project file lock
			pl.releaseProjectFileLock(true);

			return true;
		}
	}

	/**
	 * This class deletes a cell from the Project Management repository.
	 */
	private static class DeleteCellJob extends Job
	{
		private Cell cell;

		protected DeleteCellJob(Cell cell)
		{
			super("Delete cell", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt()
		{
			// find out which cell is being deleted
			Library lib = cell.getLibrary();

			// make sure the cell is not being used
			HashSet<Cell> markedCells = new HashSet<Cell>();
			for(Iterator<NodeInst> it = cell.getInstancesOf(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				markedCells.add(ni.getParent());
			}
			StringBuffer err = new StringBuffer();
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library oLib = (Library)it.next();
				for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
				{
					Cell oCell = (Cell)cIt.next();
					if (markedCells.contains(oCell))
					{
						if (err.length() > 0) err.append(", ");
						err.append(oCell.describe(true));
					}
				}
			}
			if (markedCells.size() > 0)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Cannot delete " + cell + " because it is still being used by: " + err.toString(),
					"Error Deleting Cell", JOptionPane.ERROR_MESSAGE);
				return false;
			}

			// make sure there is a valid user name
			if (needUserName()) return false;

			ProjectLibrary pl = ProjectLibrary.findProjectLibrary(lib);
			if (pl.lockProjectFile()) return false;

			// make sure the user has no cells checked-out
			boolean youOwn = false;
			for(Iterator<ProjectCell> it = pl.allCells.iterator(); it.hasNext(); )
			{
				ProjectCell pc = (ProjectCell)it.next();
				if (pc.owner.equals(getCurrentUserName())) { youOwn = true;   break; }
			}
			if (youOwn)
			{
				StringBuffer infstr = new StringBuffer();
				for(Iterator<ProjectCell> it = pl.allCells.iterator(); it.hasNext(); )
				{
					ProjectCell pc = (ProjectCell)it.next();
					if (!pc.owner.equals(getCurrentUserName())) continue;
					if (infstr.length() > 0) infstr.append(", ");
					infstr.append(pc.describe());
				}
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Before deleting a cell from the repository, you must check-in all of your work. " +
					"This is because the deletion may be dependent upon changes recently made. " +
					"These cells are checked out to you: " + infstr.toString(),
					"Error Deleting Cell", JOptionPane.ERROR_MESSAGE);
			} else
			{
				// find this in the project file
				List<ProjectCell> copyList = new ArrayList<ProjectCell>();
				for(Iterator<ProjectCell> it = pl.allCells.iterator(); it.hasNext(); )
					copyList.add(it.next());
				boolean found = false;
				for(Iterator<ProjectCell> it = copyList.iterator(); it.hasNext(); )
				{
					ProjectCell pc = (ProjectCell)it.next();
					if (pc.cellName.equals(cell.getName()) && pc.cellView == cell.getView())
					{
						// unlink it
						pl.removeProjectCell(pc);
						if (pc.cell != null) pl.byCell.remove(pc.cell);

						// disable change broadcast
						setChangeStatus(true);

						// mark this cell unlocked
						markLocked(cell, false);

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
					JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
						"This cell is not in the repository",
						"Error Deleting Cell", JOptionPane.ERROR_MESSAGE);
				}
			}

			// relase project file lock
			pl.releaseProjectFileLock(true);

			// update explorer tree
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run() { WindowFrame.wantToRedoLibraryTree(); }
			});

			return true;
		}
	}

	/************************ SUPPORT ***********************/

	private static void setChangeStatus(boolean quiet)
	{
		if (quiet) ignoreChanges = quiet;
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
//		URL url = TextUtils.makeURLToFile(userFile);
		try
		{
			PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(userFile)));

			for(Iterator<String> it = usersMap.keySet().iterator(); it.hasNext(); )
			{
				String userName = (String)it.next();
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

	private static void validateLocks(Library lib)
	{
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			ProjectCell pc = ProjectCell.findProjectCell(cell);
			if (pc == null)
			{
				// cell not in the project: writable
				markLocked(cell, false);
			} else
			{
				if (cell.getVersion() < pc.cellVersion)
				{
					// cell is an old version: writable
					markLocked(cell, false);
				} else
				{
					if (pc.owner.equals(getCurrentUserName()))
					{
						// cell checked out to current user: writable
						markLocked(cell, false);
					} else
					{
						// cell checked out to someone else: not writable
						markLocked(cell, true);
					}
				}
			}
		}
	}

	private static void markLocked(Cell cell, boolean locked)
	{
		if (!locked)
		{
			for(Iterator<Cell> it = cell.getCellGroup().getCells(); it.hasNext(); )
			{
				Cell oCell = (Cell)it.next();
				if (oCell.getView() != cell.getView()) continue;
				if (oCell.getVar(PROJLOCKEDKEY) != null)
					oCell.delVar(PROJLOCKEDKEY);
			}
		} else
		{
			for(Iterator<Cell> it = cell.getCellGroup().getCells(); it.hasNext(); )
			{
				Cell oCell = (Cell)it.next();
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
			Library oLib = (Library)it.next();
			for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
			{
				Cell oCell = (Cell)cIt.next();
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
				Library oLib = (Library)it.next();
				for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
				{
					Cell oCell = (Cell)cIt.next();
					mi = (MutableInteger)cellsMarked2.get(oCell);
					if (mi.intValue() == 1)
					{
						propagated = true;
						mi.setValue(2);
						for(Iterator<NodeInst> nIt = oCell.getInstancesOf(); nIt.hasNext(); )
						{
							NodeInst ni = (NodeInst)nIt.next();
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
			Library oLib = (Library)it.next();
			for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
			{
				Cell oCell = (Cell)cIt.next();
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
			Library oLib = (Library)it.next();
			for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
			{
				Cell oCell = (Cell)cIt.next();
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
				Library oLib = (Library)it.next();
				for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
				{
					Cell oCell = (Cell)cIt.next();
					mi = (MutableInteger)cellsMarked2.get(oCell);
					if (mi.intValue() == 1)
					{
						propagated = true;
						mi.setValue(2);
						for(Iterator<NodeInst> nIt = oCell.getNodes(); nIt.hasNext(); )
						{
							NodeInst ni = (NodeInst)nIt.next();
							if (!(ni.getProto() instanceof Cell)) continue;
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
			Library oLib = (Library)it.next();
			for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
			{
				Cell oCell = (Cell)cIt.next();
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
				Library oLib = (Library)it.next();
				for(Iterator<Cell> cIt = oLib.getCells(); cIt.hasNext(); )
				{
					Cell oCell = (Cell)cIt.next();
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
	private static void getCellFromRepository(ProjectCell pc, Library lib, boolean recursively)
	{
		// figure out the library name
		ProjectLibrary pl = pc.projLib;
		String libName = pl.projDirectory + File.separator + pc.cellName + File.separator + pc.cellVersion + "-" +
			pc.cellView.getFullName() + "." + pc.libType.getExtensions()[0];

		// read the library
		Cell newCell = null;
		String tempLibName = getTempLibraryName();
		Library fLib = LibraryFiles.readLibrary(TextUtils.makeURLToFile(libName), tempLibName, pc.libType, true);
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
					NodeInst ni = (NodeInst)it.next();
					nodePrototypes.put(ni, ni.getProto());
					if (ni.getProto() instanceof Cell)
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
									ProjectLibrary subPL = ProjectLibrary.findProjectLibrary(subLib);
	
									// get all recent cells
									String userName = getCurrentUserName();
									for(Iterator<ProjectCell> sIt = subPL.allCells.iterator(); sIt.hasNext(); )
									{
										ProjectCell recPC = (ProjectCell)sIt.next();
										if (!recPC.latestVersion) continue;
										if (recPC.cell == null)
										{
											getCellFromRepository(recPC, subLib, true);
											if (recPC.cell == null)
												System.out.println("Error retrieving cell from repository");
										}
										if (recPC.cell != null)
										{
											boolean youOwn = userName.length() > 0 && recPC.owner.equals(userName);
											markLocked(recPC.cell, !youOwn);
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
							ProjectLibrary subPL = ProjectLibrary.findProjectLibrary(subLib);
							ProjectCell subPC = subPL.findProjectCellByNameViewVersion(subCell.getName(), subCell.getView(), subCell.getVersion());
							if (subPC != null)
							{
								if (subPC.cell != null)
								{
									System.out.println("ERROR: cell " + cellName + " does not exist, but it appears as " +
										subPC.cell);
								}
								getCellFromRepository(subPC, subLib, recursively);
								realSubCell = subPC.cell;
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
				newCell = Cell.copyNodeProtoUsingMapping(cur, lib, cellName, nodePrototypes);
				if (newCell == null) System.out.println("Cannot copy " + cur + " from new library");
			}

			// kill the library
			fLib.kill("delete");
		}

		// return the new cell
		pc.cell = newCell;
		if (newCell != null)
			pl.byCell.put(newCell, pc);
	}

	private static void addNewProjectCells(ProjectLibrary pl, List<ProjectCell> updatedProjectCells)
	{
		HashMap<String,ProjectCell> versionToGet = new HashMap<String,ProjectCell>();
		for(Iterator<ProjectCell> it = pl.allCells.iterator(); it.hasNext(); )
		{
			ProjectCell pc = (ProjectCell)it.next();
			String cellName = pc.cellName + "{" + pc.cellView.getAbbreviation() + "}";
			ProjectCell pcToGet = (ProjectCell)versionToGet.get(cellName);
			if (pcToGet != null)
			{
				if (pc.cellVersion <= pcToGet.cellVersion) continue;
				if (pc.owner.length() > 0)
				{
					// this version is checked-out
					Cell oldCell = pl.lib.findNodeProto(pc.describeWithVersion());
					if (oldCell != null)
					{
						// found the cell in the library
						if (pc.owner.equals(Project.getCurrentUserName()))
						{
							versionToGet.remove(cellName);
						} else
						{
							System.out.println("WARNING: " + oldCell + " is checked-out to " + pc.owner);
						}
					} else
					{
						// the cell is not in the library
						if (pc.owner.equals(Project.getCurrentUserName()))
						{
							System.out.println("WARNING: Cell " + pl.lib.getName() + ":" + pc.describe() +
								" is checked-out to you but is missing from this library.  Do another check-out to edit it.");
						}
					}
					continue;
				}
			}
			versionToGet.put(cellName, pc);
		}
		for(Iterator<String> it = versionToGet.keySet().iterator(); it.hasNext(); )
		{
			String cellName = (String)it.next();
			ProjectCell pc = (ProjectCell)versionToGet.get(cellName);
			Cell oldCellAny = pl.lib.findNodeProto(pc.describe());
			Cell oldCell = pl.lib.findNodeProto(pc.describeWithVersion());
			if (oldCellAny != null && oldCellAny.getVersion() > pc.cellVersion)
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
	private static int updateCellFromRepository(ProjectCell pc, List<ProjectCell> updatedProjectCells)
	{
		ProjectLibrary pl = pc.projLib;
		Library lib = pl.lib;
		Cell oldCell = lib.findNodeProto(pc.describe());
		Cell newCell = null;

		// read the library with the new cell
		int total = 0;
		String libName = pl.projDirectory + File.separator + pc.cellName + File.separator + pc.cellVersion + "-" +
			pc.cellView.getFullName() + "." + pc.libType.getExtensions()[0];
		String tempLibName = getTempLibraryName();
		Library fLib = LibraryFiles.readLibrary(TextUtils.makeURLToFile(libName), tempLibName, pc.libType, true);
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
					NodeInst ni = (NodeInst)it.next();
					NodeProto np = ni.getProto();
					nodePrototypes.put(ni, np);
					if (!(np instanceof Cell)) continue;
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
								ProjectLibrary subPL = ProjectLibrary.findProjectLibrary(subLib);

								// get all recent cells
								addNewProjectCells(subPL, updatedProjectCells);
							}
						}
					}

					Cell foundSubCell = subLib.findNodeProto(subCellName);
					if (foundSubCell == null)
					{
						ProjectLibrary subPL = ProjectLibrary.findProjectLibrary(subLib);
						ProjectCell subCellPC = subPL.findProjectCellByNameViewVersion(subCell.getName(), subCell.getView(), subCell.getVersion());
						if (subCellPC != null)
						{
							if (subCellPC.cell != null)
							{
								System.out.println("ERROR: cell " + subCellName + " does not exist, but it appears as " +
									subCellPC.cell);
							}
							if (!updatedProjectCells.contains(subCellPC))
							{
								System.out.println("ERROR: cell " + subCellName + " needs to be updated but isn't in the list");
							}
							total += updateCellFromRepository(subCellPC, updatedProjectCells);
							foundSubCell = subCellPC.cell;
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
			pc.cell = newCell;
			pl.byCell.put(newCell, pc);
			if (oldCell != null)
			{
				if (useNewestVersion(oldCell, newCell))
				{
					System.out.println("Error replacing instances of new " + oldCell);
				} else
				{
					System.out.println("Updated " + newCell);
				}
				pl.byCell.remove(oldCell);
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
		for(Iterator<NodeInst> it = instances.iterator(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
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
			WindowFrame wf = (WindowFrame)it.next();
			if (wf.getContent().getCell() != oldCell) continue;
			wf.getContent().setCell(newCell, VarContext.globalContext);
		}

		// update explorer tree
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run() { WindowFrame.wantToRedoLibraryTree(); }
		});

		// replace library references
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
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
		String dirName = pc.projLib.projDirectory + File.separator + cell.getName();
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

		Cell cellCopy = copyrecursively(cell, fLib);
		if (cellCopy == null)
		{
			System.out.println("Could not place " + cell + " in a library");
			fLib.kill("delete");
			return true;
		}

		fLib.setCurCell(cellCopy);
		fLib.setFromDisk();
		boolean error = Output.writeLibrary(fLib, pc.libType, false);
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
	private static Cell copyrecursively(Cell fromCell, Library toLib)
	{
		Cell newFromCell = toLib.findNodeProto(fromCell.noLibDescribe());
		if (newFromCell != null) return newFromCell;

		// must copy subcells
		HashMap<NodeInst,NodeProto> nodePrototypes = new HashMap<NodeInst,NodeProto>();
		for(Iterator<NodeInst> it = fromCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeProto np = ni.getProto();
			nodePrototypes.put(ni, np);
			if (!(np instanceof Cell)) continue;
			Cell cell = (Cell)np;
			if (cell.getView().isTextView()) continue;

			// get proper subcell name
			String subCellName = cell.noLibDescribe();
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

	/**
	 * Class to sort project cells.
	 */
    private static class OrderedProjectCells implements Comparator<ProjectCell>
    {
        public int compare(ProjectCell pc1, ProjectCell pc2)
        {
        	int diff = pc1.cellName.compareTo(pc2.cellName);
        	if (diff != 0) return diff;
        	diff = pc1.cellView.getFullName().compareTo(pc2.cellView.getFullName());
        	if (diff != 0) return diff;
        	return pc1.cellVersion - pc2.cellVersion;
        }
    }

	/**
	 * Class to sort project cells by reverse version number.
	 */
    private static class ProjectCellByVersion implements Comparator<ProjectCell>
    {
        public int compare(ProjectCell pc1, ProjectCell pc2)
        {
        	return pc2.cellVersion - pc1.cellVersion;
        }
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
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"You must select a user first (in the 'Project Management' panel of the Preferences dialog)",
				"No Valid User", JOptionPane.ERROR_MESSAGE);
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
		if (alter) libraryProjectInfo.clear();
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
