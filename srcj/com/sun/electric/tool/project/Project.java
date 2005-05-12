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

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.GenMath.MutableInteger;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
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
import com.sun.electric.tool.compaction.Compaction;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ViewChanges;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;


/**
 * This is the Project Management tool.
 */
public class Project extends Listener
{
	/** the Project tool. */							private static Project tool = new Project();
    private static final Variable.Key proj_lockedkey = ElectricObject.newKey("PROJ_locked");
    private static final Variable.Key proj_pathkey   = ElectricObject.newKey("PROJ_path");

	public static final int NOTMANAGED         = 0;
	public static final int CHECKEDIN          = 1;
	public static final int CHECKEDOUTTOYOU    = 2;
	public static final int CHECKEDOUTTOOTHERS = 3;

	/***** user database information *****/
	
	private static final String PUSERFILE = "projectusers";
	
	private static HashMap proj_users;
	
	/***** project file information *****/
	
	private static class ProjectCell
	{
		/** name of the cell */								String   cellname;
		/** cell view */									View     cellview;
		/** cell version */									int      cellversion;
		/** the type of the library file with this cell */	FileType libType;
		/** the actual cell (if known) */					Cell     cell;
		/** current owner of this cell (if checked out) */	String   owner;
		/** previous owner of this cell (if checked in) */	String   lastowner;
		/** comments for this cell */						String   comment;
	}

	private static class ProjectLibrary
	{
		/** name of the project file */				String           fileName;
		/** Library associated with project file */	Library          lib;
		/** all cell records in the project */		List             allCells;
		/** cell records by Cell in the project */	HashMap          byCell;
		/** I/O channel for project file */			RandomAccessFile raf;
		/** Lock on file when updating it */		FileLock         lock;

		ProjectLibrary()
		{
			allCells = new ArrayList();
			byCell = new HashMap();
		}

		/**
		 * Method to ensure that there is project information for a given library.
		 * @param lib the Library to check.
		 * @param lock true to lock the project file.
		 * @return a ProjectLibrary object for the Library.  If the library is marked
		 * as being part of a project, that project file is read in.  If the library is
		 * not in a project, the returned object has nothing in it.
		 */
		public static ProjectLibrary findProject(Library lib)
		{
			// see if this library has a known project database
			ProjectLibrary pl = (ProjectLibrary)libraryProjectInfo.get(lib);
			if (pl != null) return pl;

			pl = createProject(lib);
			libraryProjectInfo.put(lib, pl);
			return pl;
		}

		private static ProjectLibrary createProject(Library lib)
		{
			// create a new project database
			ProjectLibrary pl = new ProjectLibrary();
			pl.lib = lib;

			// figure out the location of the project file
			Variable var = lib.getVar(proj_pathkey);
			if (var == null) return pl;
			String userFile = (String)var.getObject();
	        if (!TextUtils.URLExists(TextUtils.makeURLToFile(userFile)))
	        {
	    		int sepPos = userFile.lastIndexOf(File.separatorChar);
	    		if (sepPos < 0) userFile = null; else
	    		{
	    			userFile = getRepositoryLocation() + File.separator + userFile.substring(sepPos+1);
	    			if (!TextUtils.URLExists(TextUtils.makeURLToFile(userFile))) userFile = null;
	    		}
	    		if (userFile == null)
	    		{
	    			userFile = OpenFile.chooseInputFile(FileType.PROJECT, "Select Project File");
	    			if (userFile == null) return pl;
	    		}
	        }

			// prepare to read the project file
			try
			{
				pl.raf = new RandomAccessFile(userFile, "r");
			} catch (FileNotFoundException e)
			{
				System.out.println("Cannot read file " + userFile);
				return pl;
			}

			pl.fileName = userFile;
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
		 */
		public boolean lockProjectFile()
		{
			// prepare to read the project file
			try
			{
				raf = new RandomAccessFile(fileName, "rw");
			} catch (FileNotFoundException e)
			{
				System.out.println("Cannot read file " + fileName);
				return true;
			}

			FileChannel fc = raf.getChannel();
			try
			{
				lock = fc.lock();
			} catch (IOException e)
			{
				System.out.println("Unable to lock project file");
				raf = null;
				return true;
			}
			if (loadProjectFile()) return true;
			return false;
		}

		/**
		 * Method to release the lock on this project file.
		 * @param save true to rewrite it first.
		 */
		public void releaseProjectFileLock(boolean save)
		{
			if (save)
			{
				FileChannel fc = raf.getChannel();
				try
				{
					fc.position(0);
					fc.truncate(0);
					for(Iterator it = allCells.iterator(); it.hasNext(); )
					{
						ProjectCell pc = (ProjectCell)it.next();
						String line = ":" + lib.getName() + ":" + pc.cellname + ":" + pc.cellversion + "-" +
							pc.cellview.getFullName() + "." + pc.libType.getExtensions()[0] + ":" +
							pc.owner + ":" + pc.lastowner + ":" + pc.comment + "\n";
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

				ProjectCell pf = new ProjectCell();
				String [] sections = userLine.split("\\:");
				if (sections.length < 7)
				{
					System.out.println("Too few keywords in project file: " + userLine);
					return true;
				}
				if (sections[0].length() > 0)
				{
					System.out.println("Missing initial ':' in project file: " + userLine);
					return true;
				}
		
				// get cell name
				pf.cellname = sections[2];
		
				// get version
				int dashPos = sections[3].indexOf('-');
				if (dashPos < 0)
				{
					System.out.println("Missing '-' after version number in project file: " + userLine);
					return true;
				}
				int dotPos = sections[3].indexOf('.');
				if (dotPos < 0)
				{
					System.out.println("Missing '.' after view type in project file: " + userLine);
					return true;
				}
				pf.cellversion = TextUtils.atoi(sections[3].substring(0, dashPos));
		
				// get view
				String viewPart = sections[3].substring(dashPos+1, dotPos);
				pf.cellview = View.findView(viewPart);

				// get file type
				String fileType = sections[3].substring(dotPos+1);
				if (fileType.equals("elib")) pf.libType = FileType.ELIB; else
					if (fileType.equals("jelib")) pf.libType = FileType.JELIB; else
						if (fileType.equals("txt")) pf.libType = FileType.READABLEDUMP; else
				{
					System.out.println("Unknown library type in project file: " + userLine);
					return true;
				}
		
				// get owner
				pf.owner = sections[4];
		
				// get last owner
				pf.lastowner = sections[5];
		
				// get comments
				pf.comment = sections[6];
		
				// check for duplication
				for(Iterator it = allCells.iterator(); it.hasNext(); )
				{
					ProjectCell opf = (ProjectCell)it.next();
					if (!opf.cellname.equalsIgnoreCase(pf.cellname)) continue;
					if (opf.cellview != pf.cellview) continue;
					System.out.println("Error in project file: view '" + pf.cellview.getFullName() + "' of cell '" +
						pf.cellname + "' exists twice (versions " + pf.cellversion + " and " + opf.cellversion + ")");
				}

				// find the cell associated with this entry
				String cellName = pf.cellname;
				if (pf.cellview != View.UNKNOWN) cellName += "{" + pf.cellview.getAbbreviation() + "}";
				Cell cell = lib.findNodeProto(cellName);
				if (cell != null)
				{
					if (cell.getVersion() > pf.cellversion)
					{
						if (!pf.owner.equals(getCurrentUserName()))
						{
							if (pf.owner.length() == 0)
							{
								System.out.println("WARNING: cell " + cell.describe() + " is being edited, but it is not checked-out");
							} else
							{
								System.out.println("WARNING: cell " + cell.describe() + " is being edited, but it is checked-out to " + pf.owner);
							}
						}
					}
					byCell.put(cell, pf);
				}

				// link it in
				allCells.add(pf);
			}
			return false;
		}
	}

	private static HashMap libraryProjectInfo = new HashMap();
	
	/***** cell checking queue *****/
	
	private static class FCheck
	{
		Cell   entry;
		FCheck nextfcheck;
	};
	
	private static FCheck proj_firstfcheck = null;
	
	/***** miscellaneous *****/

	private static String        proj_username;		/* current user's name */
	private static boolean       proj_active;				/* nonzero if the system is active */
	private static boolean       proj_ignorechanges;		/* nonzero to ignore broadcast changes */

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
		proj_active = false;
		proj_ignorechanges = false;
	}

    /**
     * Method to retrieve the singleton associated with the Project tool.
     * @return the Project tool.
     */
    public static Project getProjectTool() { return tool; }

	/**
	 * Method to update the project libraries from the repository.
	 */
	public static void updateProject()
	{
		new UpdateJob(Library.getCurrent());
		proj_active = true;
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

	/**
	 * Method to check the currently edited cell out of the repository.
	 */
	public static void checkOutThisCell()
	{
		Cell cell = WindowFrame.needCurCell();
        if (cell == null) return;
		checkOut(cell);
	}

	/**
	 * Method to add the currently edited cell to the repository.
	 */
	public static void addThisCell()
	{
		Cell cell = WindowFrame.needCurCell();
        if (cell == null) return;
		new AddCellJob(cell);
		proj_active = true;
	}

	/**
	 * Method to remove the currently edited cell from the repository.
	 */
	public static void removeThisCell()
	{
		Cell cell = WindowFrame.needCurCell();
        if (cell == null) return;
		new DeleteCellJob(cell);
		proj_active = true;
	}

	/**
	 * Method to get an older version of the currently edited cell from the repository.
	 */
	public static void getOldVersions()
	{
		Cell cell = WindowFrame.needCurCell();
        if (cell == null) return;
        new SelectOldVersion(cell);
		proj_active = true;
	}

	/**
	 * Method to add the current library to the repository.
	 */
	public static void addThisLibrary()
	{
		new AddLibraryJob(Library.getCurrent());
		proj_active = true;
	}

	/****************************** LISTENER INTERFACE ******************************/

	/**
	 * Method to handle the start of a batch of changes.
	 * @param tool the tool that generated the changes.
	 * @param undoRedo true if these changes are from an undo or redo command.
	 */
	public void startBatch(Tool source, boolean undoRedo)
	{
	}

	/**
	 * Method to announce the end of a batch of changes.
	 */
	public void endBatch()
	{
		detectIllegalChanges();
	}

	/**
	 * Method to announce a change to a NodeInst.
	 * @param ni the NodeInst that was changed.
	 * @param oCX the old X center of the NodeInst.
	 * @param oCY the old Y center of the NodeInst.
	 * @param oSX the old X size of the NodeInst.
	 * @param oSY the old Y size of the NodeInst.
	 * @param oRot the old rotation of the NodeInst.
	 */
	public void modifyNodeInst(NodeInst ni, double oCX, double oCY, double oSX, double oSY, int oRot)
	{
		if (proj_ignorechanges) return;
		proj_queuecheck(ni.getParent());
	}

	/**
	 * Method to announce a change to many NodeInsts at once.
	 * @param nis the NodeInsts that were changed.
	 * @param oCX the old X centers of the NodeInsts.
	 * @param oCY the old Y centers of the NodeInsts.
	 * @param oSX the old X sizes of the NodeInsts.
	 * @param oSY the old Y sizes of the NodeInsts.
	 * @param oRot the old rotations of the NodeInsts.
	 */
	public void modifyNodeInsts(NodeInst [] nis, double [] oCX, double [] oCY, double [] oSX, double [] oSY, int [] oRot)
	{
		if (proj_ignorechanges) return;
		for(int i=0; i<nis.length; i++)
			proj_queuecheck(nis[i].getParent());
	}

	/**
	 * Method to announce a change to an ArcInst.
	 * @param ai the ArcInst that changed.
	 * @param oHX the old X coordinate of the ArcInst head end.
	 * @param oHY the old Y coordinate of the ArcInst head end.
	 * @param oTX the old X coordinate of the ArcInst tail end.
	 * @param oTY the old Y coordinate of the ArcInst tail end.
	 * @param oWid the old width of the ArcInst.
	 */
	public void modifyArcInst(ArcInst ai, double oHX, double oHY, double oTX, double oTY, double oWid)
	{
		if (proj_ignorechanges) return;
		proj_queuecheck(ai.getParent());
	}

	/**
	 * Method to handle a change to an Export.
	 * @param pp the Export that moved.
	 * @param oldPi the old PortInst on which it resided.
	 */
	public void modifyExport(Export pp, PortInst oldPi)
	{
		if (proj_ignorechanges) return;
		proj_queuecheck((Cell)pp.getParent());
	}

	/**
	 * Method to handle a change to a Cell.
	 * @param cell the cell that was changed.
	 * @param oLX the old low X bound of the Cell.
	 * @param oHX the old high X bound of the Cell.
	 * @param oLY the old low Y bound of the Cell.
	 * @param oHY the old high Y bound of the Cell.
	 */
	public void modifyCell(Cell cell, double oLX, double oHX, double oLY, double oHY)
	{
		if (proj_ignorechanges) return;
		proj_queuecheck(cell);
	}

	/**
	 * Method to announce a move of a Cell int CellGroup.
	 * @param cell the cell that was moved.
	 * @param oCellGroup the old CellGroup of the Cell.
	 */
	public void modifyCellGroup(Cell cell, Cell.CellGroup oCellGroup)
	{
		if (proj_ignorechanges) return;
		proj_queuecheck(cell);
	}

	/**
	 * Method to handle a change to a TextDescriptor.
	 * @param obj the ElectricObject on which the TextDescriptor resides.
	 * @param descript the TextDescriptor that changed.
	 * @param oldDescript0 the former word-0 bits in the TextDescriptor.
	 * @param oldDescript1 the former word-1 bits in the TextDescriptor.
	 * @param oldColorIndex the former color index in the TextDescriptor.
	 */
	public void modifyTextDescript(ElectricObject obj, TextDescriptor descript, int oldDescript0, int oldDescript1, int oldColorIndex)
	{
		checkObject(obj);
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
	 * Method to handle the deletion of an Export.
	 * @param pp the Export that was just deleted.
	 * @param oldPortInsts the PortInsts that were on that Export (?).
	 */
	public void killExport(Export pp, Collection oldPortInsts)
	{
		if (proj_ignorechanges) return;
		proj_queuecheck((Cell)pp.getParent());
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
	 * Method to handle a new Variable.
	 * @param obj the ElectricObject on which the Variable resides.
	 * @param var the newly created Variable.
	 */
	public void newVariable(ElectricObject obj, Variable var)
	{
		checkVariable(obj, var);
	}

	/**
	 * Method to handle a deleted Variable.
	 * @param obj the ElectricObject on which the Variable resided.
	 * @param var the deleted Variable.
	 */
	public void killVariable(ElectricObject obj, Variable var)
	{
		checkVariable(obj, var);
	}

	/**
	 * Method to handle a change to the flag bits of a Variable.
	 * @param obj the ElectricObject on which the Variable resides.
	 * @param var the Variable that was changed.
	 * @param oldFlags the former flag bits on the Variable.
	 */
	public void modifyVariableFlags(ElectricObject obj, Variable var, int oldFlags)
	{
		checkVariable(obj, var);
	}

	/**
	 * Method to handle a change to a single entry of an arrayed Variable.
	 * @param obj the ElectricObject on which the Variable resides.
	 * @param var the Variable that was changed.
	 * @param index the entry in the array that was changed.
	 * @param oldValue the former value at that entry.
	 */
	public void modifyVariable(ElectricObject obj, Variable var, int index, Object oldValue)
	{
		checkVariable(obj, var);
	}

	/**
	 * Method to handle an insertion of a new entry in an arrayed Variable.
	 * @param obj the ElectricObject on which the Variable resides.
	 * @param var the Variable that was changed.
	 * @param index the entry in the array that was inserted.
	 */
	public void insertVariable(ElectricObject obj, Variable var, int index)
	{
		checkVariable(obj, var);
	}

	/**
	 * Method to handle the deletion of a single entry in an arrayed Variable.
	 * @param obj the ElectricObject on which the Variable resides.
	 * @param var the Variable that was changed.
	 * @param index the entry in the array that was deleted.
	 * @param oldValue the former value of that entry.
	 */
	public void deleteVariable(ElectricObject obj, Variable var, int index, Object oldValue)
	{
		checkVariable(obj, var);
	}

	/**
	 * Method to announce that a Library has been read.
	 * @param lib the Library that was read.
	 */
	public void readLibrary(Library lib)
	{
		// scan the library to see if any cells are locked
		if (proj_ignorechanges) return;
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			if (cell.getVar(proj_lockedkey) != null) proj_active = true;
		}
	}

	/**
	 * Method to announce that a Library is about to be erased.
	 * @param lib the Library that will be erased.
	 */
	public void eraseLibrary(Library lib)
	{
	}

	/**
	 * Method to announce that a Library is about to be written to disk.
	 * The method should always be called inside of a Job so that the
	 * implementation can make changes to the database.
	 * @param lib the Library that will be saved.
	 */
	public void writeLibrary(Library lib)
	{
	}

	/****************************** LISTENER SUPPORT ******************************/

	private static void detectIllegalChanges()
	{
		if (!proj_active) return;
		if (proj_firstfcheck == null) return;
	
		int undonecells = 0;
		String errorMsg = "";
		for(FCheck f = proj_firstfcheck; f != null; f = f.nextfcheck)
		{
			Cell np = f.entry;
	
			// make sure cell np is checked-out
			if (np.getVar(proj_lockedkey) != null)
			{
				if (undonecells != 0) errorMsg += ", ";
				errorMsg += np.describe();
				undonecells++;
			}
		}
		proj_firstfcheck = null;

		if (undonecells > 0)
		{
			System.out.println("Cannot change unchecked-out cells: " + errorMsg);
			proj_ignorechanges = true;
			Undo.undoABatch();
			Undo.noRedoAllowed();
			proj_ignorechanges = false;
		}
	}

	private void checkObject(ElectricObject obj)
	{
		if (proj_ignorechanges) return;
		if (obj instanceof NodeInst) { proj_queuecheck(((NodeInst)obj).getParent());   return; }
		if (obj instanceof ArcInst) { proj_queuecheck(((ArcInst)obj).getParent());   return; }
		if (obj instanceof Export) { proj_queuecheck((Cell)((Export)obj).getParent());   return; }
	}

	private void checkVariable(ElectricObject obj, Variable var)
	{
		if (proj_ignorechanges) return;
		if (obj instanceof NodeInst) { proj_queuecheck(((NodeInst)obj).getParent());   return; }
		if (obj instanceof ArcInst) { proj_queuecheck(((ArcInst)obj).getParent());   return; }
		if (obj instanceof Export) { proj_queuecheck((Cell)((Export)obj).getParent());   return; }
		if (obj instanceof Cell)
		{
			if (var.getKey() != proj_lockedkey) proj_queuecheck((Cell)obj);
		}
	}
	
	private static void proj_queuecheck(Cell cell)
	{	
		// see if the cell is already queued
		for(FCheck f = proj_firstfcheck; f != null; f = f.nextfcheck)
			if (f.entry == cell) return;

		FCheck f = new FCheck();
		f.entry = cell;
		f.nextfcheck = proj_firstfcheck;
		proj_firstfcheck = f;
	}

	/****************************** PROJECT MANAGEMENT CONTROL ******************************/

	public static int getNumUsers()
	{
		ensureUserList();
		return proj_users.size();
	}

	public static Iterator getUsers()
	{
		ensureUserList();
		return proj_users.keySet().iterator();
	}

	public static boolean isExistingUser(String user)
	{
		ensureUserList();
		return proj_users.get(user) != null;
	}

	public static void deleteUser(String user)
	{
		proj_users.remove(user);
		saveUserList();
	}

	public static void addUser(String user, String password)
	{
		proj_users.put(user, password);
		saveUserList();
	}

	public static String getPassword(String user)
	{
		return (String)proj_users.get(user);
	}

	public static void changePassword(String user, String newPassword)
	{
		proj_users.put(user, newPassword);
		saveUserList();
	}

	public static int getCellStatus(Cell cell)
	{
		ProjectCell pc = getProjectCell(cell);
		if (pc == null) return NOTMANAGED;
		if (pc.owner.length() == 0) return CHECKEDIN;
		if (pc.owner.equals(getCurrentUserName())) return CHECKEDOUTTOYOU;
		return CHECKEDOUTTOOTHERS;
	}

	public static String getCellOwner(Cell cell)
	{
		ProjectCell pc = getProjectCell(cell);
		if (pc == null) return null;
		return pc.owner;
	}

	private static ProjectCell getProjectCell(Cell cell)
	{
		ProjectLibrary pl = ProjectLibrary.findProject(cell.getLibrary());
		ProjectCell pc = (ProjectCell)pl.byCell.get(cell);
		return pc;
	}

	public static void checkOut(Cell oldvers)
	{
		proj_active = true;
		new CheckOutJob(oldvers);
	}

	/****************************** PROJECT CONTROL CLASSES ******************************/

	/**
	 * This class checks out a cell from Project Management.
	 * It involves updating the project database and making a new version of the cell.
	 */
	private static class CheckOutJob extends Job
	{
		private Cell oldvers;

		protected CheckOutJob(Cell oldvers)
		{
			super("Check out cell " + oldvers.describe(), tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.oldvers = oldvers;
			startJob();
		}

		public boolean doIt()
		{
			Library lib = oldvers.getLibrary();
			ProjectLibrary pl = ProjectLibrary.findProject(lib);
		
			// make sure there is a valid user name
			if (getCurrentUserName().length() == 0)
			{
				System.out.println("No valid user");
				return false;
			}

			if (pl.lockProjectFile())
			{
				System.out.println("Couldn't lock project file");
				return false;
			}

			// find this in the project file
			Cell newvers = null;
			boolean worked = false;
			ProjectCell pf = (ProjectCell)pl.byCell.get(oldvers);
			if (pf == null) System.out.println("This cell is not in the project"); else
			{
				// see if it is available
				if (pf.owner.length() != 0)
				{
					if (pf.owner.equals(getCurrentUserName()))
					{
						System.out.println("This cell is already checked out to you");
						proj_marklocked(oldvers, false);
					} else
					{
						System.out.println("This cell is already checked out to '" + pf.owner + "'");
					}
				} else
				{
					// make sure we have the latest version
					if (pf.cellversion > oldvers.getVersion())
					{
						System.out.println("Cannot check out " + oldvers.describe() +
							" because you don't have the latest version (yours is " + oldvers.getVersion() + ", project has " +
							pf.cellversion + ").  Do an 'update' first");
					} else
					{
						// prevent tools (including this one) from seeing the change
						Undo.changesQuiet(true);

						// make new version
						newvers = Cell.copyNodeProto(oldvers, lib, oldvers.getName(), true);

						if (newvers == null)
						{
							System.out.println("Error making new version of cell");
						} else
						{
							// replace former usage with new version
							if (proj_usenewestversion(oldvers, newvers))
								System.out.println("Error replacing instances of new " + oldvers.describe()); else
							{
								// update record for the cell
								pf.owner = getCurrentUserName();
								pf.lastowner = "";
								pl.byCell.remove(oldvers);
								pl.byCell.put(newvers, pf);
								proj_marklocked(newvers, false);
								worked = true;
							}
						}

						// restore tool state
						lib.setChangedMajor();
						lib.setChangedMinor();
						Undo.changesQuiet(false);
					}
				}
			}
		
			// relase project file lock
			pl.releaseProjectFileLock(true);
		
			// if it worked, print dependencies and display
			if (worked)
			{
				System.out.println("Cell " + newvers.describe() + " checked out for your use");
		
				// advise of possible problems with other checkouts higher up in the hierarchy
				HashMap cellsMarked = new HashMap();
				for(Iterator it = Library.getLibraries(); it.hasNext(); )
				{
					Library oLib = (Library)it.next();
					for(Iterator cIt = oLib.getCells(); cIt.hasNext(); )
					{
						Cell np = (Cell)cIt.next();
						cellsMarked.put(np, new MutableInteger(0));
					}
				}
				MutableInteger mi = (MutableInteger)cellsMarked.get(newvers);
				mi.setValue(1);
				boolean propagated = true;
				while (propagated)
				{
					propagated = false;
					for(Iterator it = Library.getLibraries(); it.hasNext(); )
					{
						Library oLib = (Library)it.next();
						for(Iterator cIt = oLib.getCells(); cIt.hasNext(); )
						{
							Cell np = (Cell)cIt.next();
							MutableInteger val = (MutableInteger)cellsMarked.get(np);
							if (val.intValue() == 1)
							{
								propagated = true;
								val.setValue(2);
								for(Iterator nIt = np.getInstancesOf(); nIt.hasNext(); )
								{
									NodeInst ni = (NodeInst)nIt.next();
									MutableInteger pVal = (MutableInteger)cellsMarked.get(ni.getParent());
									if (pVal.intValue() == 0) pVal.setValue(1);
								}
							}
						}
					}
				}
				mi.setValue(0);
				int total = 0;
				for(Iterator it = Library.getLibraries(); it.hasNext(); )
				{
					Library oLib = (Library)it.next();
					for(Iterator cIt = oLib.getCells(); cIt.hasNext(); )
					{
						Cell np = (Cell)cIt.next();
						MutableInteger val = (MutableInteger)cellsMarked.get(np);
						if (val.intValue() == 0) continue;
						if (getCellStatus(np) == CHECKEDOUTTOOTHERS)
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
					for(Iterator it = Library.getLibraries(); it.hasNext(); )
					{
						Library oLib = (Library)it.next();
						for(Iterator cIt = oLib.getCells(); cIt.hasNext(); )
						{
							Cell np = (Cell)cIt.next();
							MutableInteger val = (MutableInteger)cellsMarked.get(np);
							if (val.intValue() != 3) continue;
							System.out.println("    " + np.describe() + " is checked out to " + getCellOwner(np));
						}
					}
				}
		
//				// advise of possible problems with other checkouts lower down in the hierarchy
//				for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//					for(np = olib.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//						np.temp1 = 0;
//				newvers.temp1 = 1;
//				propagated = TRUE;
//				while(propagated)
//				{
//					propagated = FALSE;
//					for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//						for(np = olib.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//					{
//						if (np.temp1 == 1)
//						{
//							propagated = TRUE;
//							np.temp1 = 2;
//							for(ni = np.firstnodeinst; ni != NONODEINST; ni = ni.nextnodeinst)
//							{
//								if (ni.proto.primindex != 0) continue;
//								if (ni.proto.temp1 == 0) ni.proto.temp1 = 1;
//							}
//						}
//					}
//				}
//				newvers.temp1 = 0;
//				total = 0;
//				for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//					for(np = olib.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//				{
//					if (np.temp1 == 0) continue;
//					pf = proj_findcell(np);
//					if (pf == NOPROJECTCELL) continue;
//					if (pf.owner != null && namesame(pf.owner, proj_username) != 0)
//					{
//						np.temp1 = 3;
//						np.temp2 = (INTBIG)pf;
//						total++;
//					}
//				}
//				if (total != 0)
//				{
//					System.out.println("*** Warning: the following cells are below this in the hierarchy");
//					System.out.println("*** and are checked out to others.  This may cause problems");
//					for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//						for(np = olib.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//					{
//						if (np.temp1 != 3) continue;
//						pf = (PROJECTCELL)np.temp2;
//						System.out.println("    " + np.describe() + " is checked out to " + pf.owner);
//					}
//				}
			}
			return true;
		}
	}

	public static void checkIn(Cell np)
	{
		proj_active = true;

		// mark the cell to be checked-in
		HashMap cellsMarked1 = new HashMap();
		HashMap cellsMarked2 = new HashMap();
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library oLib = (Library)it.next();
			for(Iterator cIt = oLib.getCells(); cIt.hasNext(); )
			{
				Cell onp = (Cell)cIt.next();
				cellsMarked1.put(onp, new MutableInteger(0));
				cellsMarked2.put(onp, new MutableInteger(0));
			}
		}
		MutableInteger mi = (MutableInteger)cellsMarked1.get(np);
		mi.setValue(1);
	
		// look for cells above this one that must also be checked in
		mi = (MutableInteger)cellsMarked2.get(np);
		mi.setValue(1);
		boolean propagated = true;
		while (propagated)
		{
			propagated = false;
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library oLib = (Library)it.next();
				for(Iterator cIt = oLib.getCells(); cIt.hasNext(); )
				{
					Cell onp = (Cell)cIt.next();
					mi = (MutableInteger)cellsMarked2.get(onp);
					if (mi.intValue() == 1)
					{
						propagated = true;
						mi.setValue(2);
						for(Iterator nIt = onp.getInstancesOf(); nIt.hasNext(); )
						{
							NodeInst ni = (NodeInst)nIt.next();
							mi = (MutableInteger)cellsMarked2.get(ni.getParent());
							if (mi.intValue() == 0) mi.setValue(1);
						}
					}
				}
			}
		}
		mi = (MutableInteger)cellsMarked2.get(np);
		mi.setValue(0);
		int total = 0;
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library oLib = (Library)it.next();
			for(Iterator cIt = oLib.getCells(); cIt.hasNext(); )
			{
				Cell onp = (Cell)cIt.next();
				mi = (MutableInteger)cellsMarked2.get(onp);
				if (mi.intValue() == 0) continue;
				String owner = getCellOwner(onp);
				if (owner.length() == 0) continue;
				if (owner.equals(Project.getCurrentUserName()))
				{
					mi = (MutableInteger)cellsMarked1.get(onp);
					mi.setValue(1);
					total++;
				}
			}
		}
	
		// look for cells below this one that must also be checked in
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library oLib = (Library)it.next();
			for(Iterator cIt = oLib.getCells(); cIt.hasNext(); )
			{
				Cell onp = (Cell)cIt.next();
				mi = (MutableInteger)cellsMarked2.get(onp);
				mi.setValue(0);
			}
		}
		mi = (MutableInteger)cellsMarked2.get(np);
		mi.setValue(1);
		propagated = true;
		while (propagated)
		{
			propagated = false;
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library oLib = (Library)it.next();
				for(Iterator cIt = oLib.getCells(); cIt.hasNext(); )
				{
					Cell onp = (Cell)cIt.next();
					mi = (MutableInteger)cellsMarked2.get(onp);
					if (mi.intValue() == 1)
					{
						propagated = true;
						mi.setValue(2);
						for(Iterator nIt = onp.getNodes(); nIt.hasNext(); )
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
		mi = (MutableInteger)cellsMarked2.get(np);
		mi.setValue(0);
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library oLib = (Library)it.next();
			for(Iterator cIt = oLib.getCells(); cIt.hasNext(); )
			{
				Cell onp = (Cell)cIt.next();
				mi = (MutableInteger)cellsMarked2.get(onp);
				if (mi.intValue() == 0) continue;
				String owner = getCellOwner(onp);
				if (owner.length() == 0) continue;
				if (owner.equals(Project.getCurrentUserName()))
				{
					mi = (MutableInteger)cellsMarked1.get(onp);
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
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library oLib = (Library)it.next();
				for(Iterator cIt = oLib.getCells(); cIt.hasNext(); )
				{
					Cell onp = (Cell)cIt.next();
					mi = (MutableInteger)cellsMarked1.get(onp);
					if (onp == np || mi.intValue() == 0) continue;
					if (total > 0) infstr.append(", ");
					infstr.append(onp.describe());
					total++;
				}
			}
			System.out.println("Also checking in related cell(s): " + infstr.toString());
		}
	
		// check it in
		new CheckInJob(np.getLibrary(), cellsMarked1);
	}

	/**
	 * This class displays a dialog for selecting old versions of a cell to restore.
	 */
	private static class SelectOldVersion extends EDialog
	{
		private Cell cell;
		private JList versionList;
		private DefaultListModel versionModel;

		private SelectOldVersion(Cell cell)
		{
			super(null, true);
			this.cell = cell;
			initComponents();
			setVisible(true);
		}

		protected void escapePressed() { exit(false); }

		// Call this method when the user clicks the OK button
		private void exit(boolean goodButton)
		{
			if (goodButton)
			{
				int index = versionList.getSelectedIndex();
				String line = (String)versionModel.getElementAt(index);
				int version = TextUtils.atoi(line);
				new GetOldVersionJob(cell, version);
			}
			dispose();
		}

		private void initComponents()
		{
			getContentPane().setLayout(new GridBagLayout());

			setTitle("Select an Old Version to Restore");
			setName("");
			addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent evt) { exit(false); }
			});

			JScrollPane versionPane = new JScrollPane();
			versionModel = new DefaultListModel();
			versionList = new JList(versionModel);
			versionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			versionPane.setViewportView(versionList);
			versionPane.setPreferredSize(new Dimension(100, 150));
			versionList.clearSelection();

			ProjectLibrary pl = ProjectLibrary.findProject(cell.getLibrary());
			String dirName = pl.fileName;
			if (dirName.endsWith(".proj")) dirName = dirName.substring(0, dirName.length()-5);
			dirName += File.separator + cell.getName();
			File dir = new File(dirName);
			File [] filesInDir = dir.listFiles();
			for(int i=0; i<filesInDir.length; i++)
			{
				File subFile = filesInDir[i];
				Date modDate = new Date(subFile.lastModified());
				int version = TextUtils.atoi(subFile.getName());
				versionModel.addElement(version + " (checked in on " + TextUtils.formatDate(modDate) + ")");
			}
			versionList.setSelectedIndex(0);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 0;
			gbc.gridwidth = 2;
			gbc.weightx = gbc.weighty = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(versionPane, gbc);

			// OK and Cancel
			JButton cancel = new JButton("Cancel");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 1;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(cancel, gbc);
			cancel.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { exit(false); }
			});

			JButton ok = new JButton("OK");
			getRootPane().setDefaultButton(ok);
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 1;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(ok, gbc);
			ok.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { exit(true); }
			});

			pack();
		}
	}

	/**
	 * This class gets old versions of cells from the Project Management repository.
	 */
	private static class GetOldVersionJob extends Job
	{
		private Cell np;
		private int version;

		protected GetOldVersionJob(Cell np, int version)
		{
			super("Update cells", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.np = np;
			this.version = version;
			startJob();
		}

		public boolean doIt()
		{
			System.out.println("Restore version "+version+" of cell "+np.describe());
			Library lib = np.getLibrary();
			ProjectLibrary pl = ProjectLibrary.findProject(lib);
	
			ProjectCell pf = getProjectCell(np);
			if (pf == null)
			{
				System.out.println("Cell " + np.describe() + " is not in the project");
				return false;
			}
	
			// build a fake record to describe this cell
			ProjectCell oldOne = new ProjectCell();
			oldOne.cellname = pf.cellname;
			oldOne.cellview = pf.cellview;
			oldOne.cellversion = version;
			oldOne.libType = pf.libType;
			oldOne.cell = pf.cell;
			oldOne.owner = pf.owner;
			oldOne.lastowner = pf.lastowner;
			oldOne.comment = pf.comment;
	
			np = proj_getcell(pl, oldOne, lib);
			if (np == null)
				System.out.println("Error retrieving old version of cell");
			proj_marklocked(np, false);
			System.out.println("Cell " + np.describe() + " is now in this library");
			return true;
		}
	}

	/**
	 * This class updates cells from the Project Management repository.
	 */
	private static class UpdateJob extends Job
	{
		private Library lib;

		protected UpdateJob(Library lib)
		{
			super("Update cells", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.lib = lib;
			startJob();
		}

		public boolean doIt()
		{
			// make sure there is a valid user name
			if (getCurrentUserName().length() == 0)
			{
				System.out.println("No valid user");
				return false;
			}

			ProjectLibrary pl = ProjectLibrary.findProject(lib);
			if (pl.lockProjectFile())
			{
				System.out.println("Couldn't lock project file");
				return false;
			}

			// prevent tools (including this one) from seeing the change
			Undo.changesQuiet(true);

			// check to see which cells are changed/added
			int total = 0;
			for(Iterator it = pl.allCells.iterator(); it.hasNext(); )
			{
				ProjectCell pf = (ProjectCell)it.next();
				String cellName = pf.cellname;
				if (pf.cellview != View.UNKNOWN) cellName += "{" + pf.cellview.getAbbreviation() + "}";
				Cell oldnp = lib.findNodeProto(cellName);
				if (oldnp != null && oldnp.getVersion() >= pf.cellversion) continue;
	
				// this is a new one
				Cell newnp = proj_getcell(pl, pf, lib);
				if (newnp == null)
				{
					System.out.println("Error bringing in " + cellName);
				} else
				{
					if (oldnp != null)
					{
						if (proj_usenewestversion(oldnp, newnp))
						{
							System.out.println("Error replacing instances of new " + oldnp.describe());
						} else
						{
							System.out.println("Updated cell " + newnp.describe());
						}
					} else
					{
						System.out.println("Added new cell " + newnp.describe());
					}
					pl.byCell.remove(oldnp);
					pl.byCell.put(newnp, pf);
					total++;
				}
			}

			// restore change broadcast
			Undo.changesQuiet(false);

			// relase project file lock
			pl.releaseProjectFileLock(false);
		
			// make sure all cell locks are correct
			proj_validatelocks(lib);
		
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
			ProjectLibrary pl = ProjectLibrary.findProject(lib);
			if (pl.allCells.size() != 0)
			{
				System.out.println("This library is already in Project Management");
				return false;
			}

			// verify that a project is to be built
			int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(),
				"Are you sure you want to enter this library into Project Management?");
			if (response != JOptionPane.YES_OPTION) return false;
		
			// get path prefix for cell libraries
			String libraryname = TextUtils.getFileNameWithoutExtension(lib.getLibFile());
		
			// create the top-level directory for this library
			String newname = Project.getRepositoryLocation() + File.separator + libraryname;
			File dir = new File(newname);
			if (dir.exists())
			{
				System.out.println("Project directory '" + newname + "' already exists");
				return false;
			}
			if (!dir.mkdir())
			{
				System.out.println("Could not create project directory '" + newname + "'");
				return false;
			}
			System.out.println("Making project directory '" + newname + "'...");

			// turn off all tools
			Undo.changesQuiet(true);
		
			// make libraries for every cell
			for(Iterator it = lib.getCells(); it.hasNext(); )
			{
				Cell np = (Cell)it.next();

				// ignore old unused cell versions
				if (np.getNewestVersion() != np)
				{
					if (np.getNumUsagesIn() == 0) continue;
					System.out.println("Warning: including old version of cell " + np.describe());
				}

				ProjectCell pf = new ProjectCell();
				pf.cellname = np.getName();
				pf.cellview = np.getView();
				pf.cellversion = np.getVersion();
				pf.owner = "";
				pf.lastowner = proj_username;
				pf.comment = "Initial checkin";

				if (proj_writecell(np, pl, pf)) System.out.println("Error writing cell file"); else
				{
					// write the cell to disk in its own library
					System.out.println("Entering cell " + np.describe());
		
					// mark this cell "checked in" and locked
					proj_marklocked(np, true);
				}
			}

			// create the project file
			String projfile = newname + File.separator + libraryname + ".proj";
			lib.newVar(proj_pathkey, projfile);
			try
			{
				PrintStream buffWriter = new PrintStream(new FileOutputStream(projfile));
				for(Iterator it = pl.allCells.iterator(); it.hasNext(); )
				{
					ProjectCell pc = (ProjectCell)it.next();
					buffWriter.println(":" + lib.getName() + ":" + pc.cellname + ":" + pc.cellversion + "-" +
						pc.cellview.getFullName() + "." + pc.libType.getExtensions()[0] + ":" +
						pc.owner + ":" + pc.lastowner + ":" + pc.comment);
				}
				buffWriter.close();
			} catch (IOException e)
			{
				System.out.println("Error creating " + projfile);
			}

			// restore tool state
			Undo.changesQuiet(false);

			// advise the user of this library
			System.out.println("The current library should be saved and used by new users");
			return true;
		}
	}

	/**
	 * This class adds a cell to the Project Management repository.
	 */
	private static class AddCellJob extends Job
	{
		private Cell np;

		protected AddCellJob(Cell np)
		{
			super("Add cell", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.np = np;
			startJob();
		}

		public boolean doIt()
		{			
			if (np.getNewestVersion() != np)
			{
				System.out.println("Cannot add an old version of the cell");
				return false;
			}

			// make sure there is a valid user name
			if (getCurrentUserName().length() == 0)
			{
				System.out.println("No valid user");
				return false;
			}

			Library lib = np.getLibrary();
			ProjectLibrary pl = ProjectLibrary.findProject(lib);
			if (pl.lockProjectFile())
			{
				System.out.println("Couldn't lock project file");
				return false;
			}

			// prevent tools (including this one) from seeing the change
			Undo.changesQuiet(true);

			// find this in the project file
			ProjectCell foundPC = null;
			for(Iterator it = pl.allCells.iterator(); it.hasNext(); )
			{
				ProjectCell pf = (ProjectCell)it.next();
				if (pf.cellname.equals(np.getName()) && pf.cellview == np.getView()) { foundPC = pf;   break; }
			}
			if (foundPC != null) System.out.println("This cell is already in the project"); else
			{
				// create new entry for this cell
				ProjectCell pf = new ProjectCell();
				pf.cellname = np.getName();
				pf.cellview = np.getView();
				pf.cellversion = np.getVersion();
				pf.owner = "";
				pf.lastowner = proj_username;
				pf.comment = "Initial checkin";

				if (proj_writecell(np, pl, pf)) System.out.println("Error writing cell file"); else
				{
					// link it in
					pl.allCells.add(pf);
					pl.byCell.put(np, pf);

					// mark this cell "checked in" and locked
					proj_marklocked(np, true);

					System.out.println("Cell " + np.describe() + " added to the project");
				}
			}

			// restore change broadcast
			Undo.changesQuiet(false);

			// relase project file lock
			pl.releaseProjectFileLock(false);

			return true;
		}
	}

	/**
	 * This class deletes a cell from the Project Management repository.
	 */
	private static class DeleteCellJob extends Job
	{
		private Cell np;

		protected DeleteCellJob(Cell np)
		{
			super("Delete cell", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.np = np;
			startJob();
		}

		public boolean doIt()
		{			
			// find out which cell is being deleted
			Library lib = np.getLibrary();
		
			// make sure the cell is not being used
			if (np.getNumUsagesIn() != 0)
			{
				HashSet markedCells = new HashSet();
				for(Iterator it = np.getInstancesOf(); it.hasNext(); )
				{
					NodeInst ni = (NodeInst)it.next();
					markedCells.add(ni.getParent());
				}
				System.out.println("Cannot delete cell " + np.describe() + " because it is still being used by:");
				for(Iterator it = Library.getLibraries(); it.hasNext(); )
				{
					Library oLib = (Library)it.next();
					for(Iterator cIt = oLib.getCells(); cIt.hasNext(); )
					{
						Cell onp = (Cell)cIt.next();
						if (markedCells.contains(onp))
							System.out.println("   Cell " + onp.describe());
					}
				}
				return false;
			}

			// make sure there is a valid user name
			if (getCurrentUserName().length() == 0)
			{
				System.out.println("No valid user");
				return false;
			}

			ProjectLibrary pl = ProjectLibrary.findProject(lib);
			if (pl.lockProjectFile())
			{
				System.out.println("Couldn't lock project file");
				return false;
			}
		
			// make sure the user has no cells checked-out
			boolean youOwn = false;
			for(Iterator it = pl.allCells.iterator(); it.hasNext(); )
			{
				ProjectCell pc = (ProjectCell)it.next();
				if (pc.owner.equals(getCurrentUserName())) { youOwn = true;   break; }
			}
			if (youOwn)
			{
				System.out.println("Before deleting a cell from the project, you must check-in all of your work.");
				System.out.println("This is because the deletion may be dependent upon changes recently made.");
				StringBuffer infstr = new StringBuffer();
				for(Iterator it = pl.allCells.iterator(); it.hasNext(); )
				{
					ProjectCell pc = (ProjectCell)it.next();
					if (!pc.owner.equals(getCurrentUserName())) continue;
					if (infstr.length() > 0) infstr.append(", ");
					infstr.append(pc.cellname);
					if (pc.cellview != View.UNKNOWN) infstr.append("{" + pc.cellview.getAbbreviation() + "}");
				}
				System.out.println("These cells are checked out to you: " + infstr.toString());
			} else
			{
				// find this in the project file
				ProjectCell foundPC = null;
				for(Iterator it = pl.allCells.iterator(); it.hasNext(); )
				{
					ProjectCell pc = (ProjectCell)it.next();
					if (pc.cellname.equals(np.getName()) && pc.cellview == np.getView()) { foundPC = pc;   break; }
				}
				if (foundPC == null) System.out.println("This cell is not in the project"); else
				{
					// unlink it
					pl.allCells.remove(foundPC);
					pl.byCell.remove(foundPC);

					// mark this cell unlocked
					proj_marklocked(np, false);

					System.out.println("Cell " + np.describe() + " deleted from the project");
				}
			}

			// restore change broadcast
			Undo.changesQuiet(false);

			// relase project file lock
			pl.releaseProjectFileLock(false);

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
		private HashMap cellsMarked1;

		protected CheckInJob(Library lib, HashMap cellsMarked1)
		{
			super("Check in cells", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.lib = lib;
			this.cellsMarked1 = cellsMarked1;
			startJob();
		}

		public boolean doIt()
		{
			// make sure there is a valid user name
			if (getCurrentUserName().length() == 0)
			{
				System.out.println("No valid user");
				return false;
			}

			ProjectLibrary pl = ProjectLibrary.findProject(lib);
			if (pl.lockProjectFile())
			{
				System.out.println("Couldn't lock project file");
				return false;
			}

			// prevent tools (including this one) from seeing the change
			Undo.changesQuiet(true);

			// check in the requested cells
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library oLib = (Library)it.next();
				for(Iterator cIt = oLib.getCells(); cIt.hasNext(); )
				{
					Cell np = (Cell)cIt.next();
					MutableInteger mi = (MutableInteger)cellsMarked1.get(np);
					if (mi.intValue() == 0) continue;
		
					// find this in the project file
					ProjectCell pf = getProjectCell(np);
					if (pf == null)
						System.out.println("Cell " + np.describe() + " is not in the project"); else
					{
						// see if it is available
						if (!pf.owner.equals(Project.getCurrentUserName()))
							System.out.println("Cell " + np.describe() + " is not checked out to you"); else
						{
							String comment = JOptionPane.showInputDialog("Reason for checking-in cell " + np.describe(), "");
							if (comment != null)
							{
								// write the cell out there
								if (proj_writecell(np, pl, pf))
									System.out.println("Error writing cell " + np.describe()); else
								{
									pf.owner = "";
									pf.lastowner = proj_username;
									pf.cellversion = np.getVersion();
									pf.comment = comment;
									proj_marklocked(np, true);
									System.out.println("Cell " + np.describe() + " checked in");
								}
							}
						}
					}
				}
			}

			// restore change broadcast
			Undo.changesQuiet(false);

			// relase project file lock
			pl.releaseProjectFileLock(true);
			return true;
		}
	}

	/************************ SUPPORT ***********************/

	private static void ensureUserList()
	{
		if (proj_users == null)
		{
			proj_users = new HashMap();
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
					String password = userLine.substring(colonPos+1);
					proj_users.put(userName, password);
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
		URL url = TextUtils.makeURLToFile(userFile);
		try
		{
			PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(userFile)));

			for(Iterator it = proj_users.keySet().iterator(); it.hasNext(); )
			{
				String userName = (String)it.next();
				String realPassword = (String)proj_users.get(userName);
				printWriter.println(userName + ":" + realPassword);
			}

			printWriter.close();
			System.out.println("Wrote " + userFile);
		} catch (IOException e)
		{
			System.out.println("Error writing " + userFile);
			return;
		}
	}

	private static void proj_validatelocks(Library lib)
	{
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell np = (Cell)it.next();
			ProjectCell pf = getProjectCell(np);
			if (pf == null)
			{
				// cell not in the project: writable
				proj_marklocked(np, false);
			} else
			{
				if (np.getVersion() < pf.cellversion)
				{
					// cell is an old version: writable
					proj_marklocked(np, false);
				} else
				{
					if (pf.owner.equals(getCurrentUserName()))
					{
						// cell checked out to current user: writable
						proj_marklocked(np, false);
					} else
					{
						// cell checked out to someone else: not writable
						proj_marklocked(np, true);
					}
				}
			}
		}
	}
	
	private static void proj_marklocked(Cell np, boolean locked)
	{
		if (!locked)
		{
			for(Iterator it = np.getCellGroup().getCells(); it.hasNext(); )
			{
				Cell onp = (Cell)it.next();
				if (onp.getView() != np.getView()) continue;
				if (onp.getVar(proj_lockedkey) != null)
					onp.delVar(proj_lockedkey);
			}
		} else
		{
			for(Iterator it = np.getCellGroup().getCells(); it.hasNext(); )
			{
				Cell onp = (Cell)it.next();
				if (onp.getView() != np.getView()) continue;
				if (onp.getNewestVersion() == onp)
				{
					if (onp.getVar(proj_lockedkey) == null)
						onp.newVar(proj_lockedkey, new Integer(1));
				} else
				{
					if (onp.getVar(proj_lockedkey) != null)
						onp.delVar(proj_lockedkey);
				}
			}
		}
	}
	
	/**
	 * Method to get the latest version of the cell described by "pf" and return
	 * the newly created cell.  Returns null on error.
	 */
	private static Cell proj_getcell(ProjectLibrary pl, ProjectCell pf, Library lib)
	{
		// prevent tools (including this one) from seeing the change
		Undo.changesQuiet(true);

		// figure out the library name
		String libName = pl.fileName;
		if (libName.endsWith(".proj")) libName = libName.substring(0, libName.length()-5);
		libName += File.separator + pf.cellname + File.separator + pf.cellversion + "-" +
			pf.cellview.getFullName() + "." + pf.libType.getExtensions()[0];

		// read the library
		Cell newCell = null;
		Library flib = Input.readLibrary(TextUtils.makeURLToFile(libName), pf.libType);
		if (flib == null) System.out.println("Cannot read library " + libName); else
		{
			String cellNameInRepository = pf.cellname;
			if (pf.cellview != View.UNKNOWN) cellNameInRepository += "{" + pf.cellview.getAbbreviation() + "}";
			Cell cur = flib.findNodeProto(cellNameInRepository);
			if (cur == null) System.out.println("Cannot find cell " + cellNameInRepository + " in library " + libName); else
			{
				String cellname = cur.getName() + ";" + cur.getVersion();
				if (cur.getView() != View.UNKNOWN) cellname += "{" + cur.getView().getAbbreviation() + "}";
				newCell = Cell.copyNodeProto(cur, lib, cellname, true);
				if (newCell == null) System.out.println("Cannot copy cell " + cur.describe() + " from new library");
			}

			// kill the library
			flib.kill("");
		}

		// restore tool state
		Undo.changesQuiet(false);
	
		// return the new cell
		return newCell;
	}
	
	private static boolean proj_usenewestversion(Cell oldnp, Cell newnp)
	{
		// replace all instances
		List instances = new ArrayList();
		for(Iterator it = oldnp.getInstancesOf(); it.hasNext(); )
			instances.add(it.next());
		for(Iterator it = instances.iterator(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeInst newni = ni.replace(newnp, false, false);
			if (newni == null)
			{
				System.out.println("Failed to update instance of " + newnp.describe() + " in " + ni.getParent().describe());
				return true;
			}
		}
	
		// redraw windows that showed the old cell
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			if (wf.getContent().getCell() != oldnp) continue;
			wf.getContent().setCell(newnp, VarContext.globalContext);
		}

		// update explorer tree
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run() { WindowFrame.wantToRedoLibraryTree(); }
		});
	
		// replace library references
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			if (lib.getCurCell() == oldnp) lib.setCurCell(newnp);
		}

		// finally delete the former cell
		oldnp.kill();

		return false;
	}
	
	private static boolean proj_writecell(Cell np, ProjectLibrary pl, ProjectCell pc)
	{
		String dirName = pl.fileName;
		if (dirName.endsWith(".proj")) dirName = dirName.substring(0, dirName.length()-5);
		dirName += File.separator + np.getName();
		File dir = new File(dirName);
		if (!dir.exists())
		{
			if (!dir.mkdir())
			{
				System.out.println("Unable to create directory " + dirName);
				return true;
			}
		}

		String libName = dirName + File.separator + np.getVersion() + "-" + np.getView().getFullName() + ".elib";
	
		String templibname = proj_templibraryname();
		Library flib = Library.newInstance(templibname, TextUtils.makeURLToFile(libName));
		if (flib == null)
		{
			System.out.println("Cannot create library " + libName);
			return true;
		}

		Cell npcopy = copyrecursively(np, flib);
		if (npcopy == null)
		{
			System.out.println("Could not place " + np.describe() + " in a library");
			flib.kill("");
			return true;
		}
	
		flib.setCurCell(npcopy);
		flib.setFromDisk();
        boolean error = Output.writeLibrary(flib, pc.libType, false);
		if (error)
		{
			System.out.println("Could not save library with " + np.describe() + " in it");
			flib.kill("");
			return true;
		}
		flib.kill("");

		return false;
	}
	
	private static String proj_templibraryname()
	{
		for(int i=1; ; i++)
		{
			String libName = "projecttemp" + i;
			if (Library.findLibrary(libName) == null) return libName;
		}
	}

	private static Cell copyrecursively(Cell fromnp, Library tolib)
	{
		// must copy subcells
		for(Iterator it = fromnp.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (!(ni.getProto() instanceof Cell)) continue;
			Cell np = (Cell)ni.getProto();
	
			// see if there is already a cell with this name and view
			Cell onp = tolib.findNodeProto(np.noLibDescribe());
			if (onp != null) continue;

			if (np.getView().isTextView()) continue;
			String newname = np.getName();
			if (np.getView() != View.UNKNOWN)
				newname += "{" + np.getView().getAbbreviation() + "}";
			onp = Cell.makeInstance(tolib, newname);
			if (onp == null)
			{
				System.out.println("Could not create subcell " + newname);
				continue;
			}

			if (ViewChanges.skeletonizeCell(np, onp))
			{
				System.out.println("Copy of subcell " + np.describe() + " failed");
				return null;
			}
		}

		// copy the cell if it is not already done
		Cell newfromnp = tolib.findNodeProto(fromnp.noLibDescribe());
		if (newfromnp == null)
		{
			String newname = fromnp.getName() + ";" + fromnp.getVersion();
			if (fromnp.getView() != View.UNKNOWN)
				newname += "{" + fromnp.getView().getAbbreviation() + "}";
			newfromnp = Cell.copyNodeProto(fromnp, tolib, newname, true);
			if (newfromnp == null) return null;
		}
	
		return newfromnp;
	}

	/************************ PREFERENCES ***********************/

	private static Pref cacheCurrentUserName = Pref.makeStringPref("CurrentUserName", Project.tool.prefs, "");
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

	private static Pref cacheRepositoryLocation = Pref.makeStringPref("RepositoryLocation", Project.tool.prefs, "");
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
		cacheRepositoryLocation.setString(r);
		proj_users = null;
		libraryProjectInfo.clear();
	}

	
//	
//	PROJECTCELL *proj_findcell(NODEPROTO *np)
//	{
//		PROJECTCELL *pf;
//	
//		for(pf = proj_firstprojectcell; pf != NOPROJECTCELL; pf = pf.nextprojectcell)
//			if (estrcmp(pf.cellname, np.protoname) == 0 && pf.cellview == np.cellview)
//				return(pf);
//		return(NOPROJECTCELL);
//	}
//	
}
