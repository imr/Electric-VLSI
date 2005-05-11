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
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ViewChanges;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;


/**
 * This is the Project Management tool.
 */
public class Project extends Listener
{
	/** the Project tool. */							protected static Project tool = new Project();
    private static final Variable.Key proj_lockedkey = ElectricObject.newKey("PROJ_locked");
    private static final Variable.Key proj_pathkey = ElectricObject.newKey("PROJ_path");
    private static final Variable.Key proj_userkey = ElectricObject.newKey("PROJ_user");

	public static final int NOTMANAGED         = 0;
	public static final int CHECKEDIN          = 1;
	public static final int CHECKEDOUTTOYOU    = 2;
	public static final int CHECKEDOUTTOOTHERS = 3;

	/***** user database information *****/
	
	private static final String PUSERFILE = "projectusers";
	
	static HashMap proj_users;
	
	/***** project file information *****/
	
	private static class ProjectCell
	{
		/** name of the library */							String   libname;
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
						String line = ":" + pc.libname + ":" + pc.cellname + ":" + pc.cellversion + "-" +
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
		
				// get library name
				pf.libname = sections[1];
		
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
					if (cell.getVersion() != pf.cellversion)
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
	
	static FCheck proj_firstfcheck = null;
	
	/***** miscellaneous *****/

	static String        proj_username;		/* current user's name */
	static boolean       proj_active;				/* nonzero if the system is active */
	static boolean       proj_ignorechanges;		/* nonzero to ignore broadcast changes */
	static Tool         proj_source;				/* source of changes */
//	static PUSER        *proj_userpos;				/* current user for dialog display */
//	static FILE         *proj_io;					/* channel to project file */
//	static INTBIG        proj_filetypeproj;			/* Project disk file descriptor */
//	static ProjectCell proj_firstprojectcell = null;
//	static ProjectCell proj_projectcellfree = null;

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

	public static void updateProject()
	{
		System.out.println("CANNOT UPDATE YET");
	}

	public static void checkInThisCell()
	{
		Cell cell = WindowFrame.needCurCell();;
        if (cell == null) return;
		checkIn(cell);
	}

	public static void checkOutThisCell()
	{
		Cell cell = WindowFrame.needCurCell();;
        if (cell == null) return;
		checkOut(cell);
	}

	public static void addThisCell()
	{
		System.out.println("CANNOT ADD YET");
	}

	public static void removeThisCell()
	{
		System.out.println("CANNOT REMOVE YET");
	}

	public static void getOldVersions()
	{
		System.out.println("CANNOT GET OLD VERSIONS YET");
	}

	public static void addThisLibrary()
	{
		System.out.println("CANNOT ADD LIBRARIES YET");
	}

	/****************************** LISTENER INTERFACE ******************************/

	/**
	 * Method to handle the start of a batch of changes.
	 * @param tool the tool that generated the changes.
	 * @param undoRedo true if these changes are from an undo or redo command.
	 */
	public void startBatch(Tool source, boolean undoRedo)
	{
		if (proj_ignorechanges) proj_source = tool; else
			proj_source = source;
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
		if (proj_ignorechanges || proj_source == tool) return;
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
		if (proj_ignorechanges || proj_source == tool) return;
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
		if (proj_ignorechanges || proj_source == tool) return;
		proj_queuecheck(ai.getParent());
	}

	/**
	 * Method to handle a change to an Export.
	 * @param pp the Export that moved.
	 * @param oldPi the old PortInst on which it resided.
	 */
	public void modifyExport(Export pp, PortInst oldPi)
	{
		if (proj_ignorechanges || proj_source == tool) return;
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
		if (proj_ignorechanges || proj_source == tool) return;
		proj_queuecheck(cell);
	}

	/**
	 * Method to announce a move of a Cell int CellGroup.
	 * @param cell the cell that was moved.
	 * @param oCellGroup the old CellGroup of the Cell.
	 */
	public void modifyCellGroup(Cell cell, Cell.CellGroup oCellGroup)
	{
		if (proj_ignorechanges || proj_source == tool) return;
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
		if (proj_ignorechanges || proj_source == tool) return;
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

	private void checkObject(ElectricObject obj)
	{
		if (proj_ignorechanges || proj_source == tool) return;
		if (obj instanceof NodeInst) { proj_queuecheck(((NodeInst)obj).getParent());   return; }
		if (obj instanceof ArcInst) { proj_queuecheck(((ArcInst)obj).getParent());   return; }
		if (obj instanceof Export) { proj_queuecheck((Cell)((Export)obj).getParent());   return; }
	}

	private void checkVariable(ElectricObject obj, Variable var)
	{
		if (proj_ignorechanges || proj_source == tool) return;
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
			if (f.entry == cell ) return;

		FCheck f = new FCheck();
		f.entry = cell;
		f.nextfcheck = proj_firstfcheck;
		proj_firstfcheck = f;
	}

	/****************************** PROJECT ACCESS ******************************/

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
		new CheckOutJob(oldvers);
	}

	/**
	 * This class checks out a cell from Project Management.
	 * It involves updating the project database and making a new version of the cell.
	 */
	private static class CheckOutJob extends Job
	{
		private Cell oldvers;

		protected CheckOutJob(Cell oldvers)
		{
			super("Check out cell " + oldvers.describe(), User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
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
//					ttyputmsg(_("*** Warning: the following cells are below this in the hierarchy"));
//					ttyputmsg(_("*** and are checked out to others.  This may cause problems"));
//					for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//						for(np = olib.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//					{
//						if (np.temp1 != 3) continue;
//						pf = (PROJECTCELL)np.temp2;
//						ttyputmsg(_("    %s is checked out to %s"), describenodeproto(np), pf.owner);
//					}
//				}
			}
			return true;
		}
	}

	public static void checkIn(Cell np)
	{
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

	/************************ SUPPORT ***********************/
	
//	private static final int MAXTRIES = 10;
//	private static final int NAPTIME  =  5;
//	
//	private static boolean proj_lockprojfile()
//	{
//		String lockfilename = projectpath + projectfile + "LOCK";
//		for(int i=0; i<MAXTRIES; i++)
//		{
//			if (lockfile(lockfilename)) return false;
//			if (i == 0) System.out.println("Project file locked.  Waiting..."); else
//				System.out.println("Still waiting (will try " + (MAXTRIES-i) + " more times)...");
//			for(int j=0; j<NAPTIME; j++)
//			{
//				gotosleep(60);
//				if (stopping(STOPREASONLOCK)) return true;
//			}
//		}
//		return true;
//	}

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

	/************************ USER DATABASE ***********************/
	
	/**
	 * Method to obtain the user's name.  If "newone" is true, force input of a name,
	 * otherwise, only ask if unknown.  Returns true if user name is invalid.
	 */
	static boolean proj_getusername(Library lib)
	{
		// if name exists and not forcing a new user name, stop now
		String userName = getCurrentUserName();
		if (userName != null) return false;

		JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
			"Select a user name in the 'Project Management' tab of the 'Preferences' dialog",
			"Missing User", JOptionPane.ERROR_MESSAGE);
		return false;
	}

	
	/***** command parsing *****/
	
//	static COMCOMP projcop = {NOKEYWORD, topofcells, nextcells, NOPARAMS,
//		INPUTOPT, x_(" \t"), M_("cell to be checked-out"), M_("check-out current cell")};
//	static COMCOMP projcip = {NOKEYWORD, topofcells, nextcells, NOPARAMS,
//		INPUTOPT, x_(" \t"), M_("cell to be checked-in"), M_("check-in current cell")};
//	static COMCOMP projoldp = {NOKEYWORD, topofcells, nextcells, NOPARAMS,
//		INPUTOPT, x_(" \t"), M_("cell from which old version should be retrieved"),
//			M_("use current cell")};
//	static COMCOMP projap = {NOKEYWORD, topofcells, nextcells, NOPARAMS,
//		INPUTOPT, x_(" \t"), M_("cell to be added"), M_("add current cell")};
//	static COMCOMP projdp = {NOKEYWORD, topofcells, nextcells, NOPARAMS,
//		INPUTOPT, x_(" \t"), M_("cell to be deleted"), M_("delete current cell")};
//	static KEYWORD projopt[] =
//	{
//		{x_("build-project"),        0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//		{x_("check-out"),            1,{&projcop,NOKEY,NOKEY,NOKEY,NOKEY}},
//		{x_("check-in"),             1,{&projcip,NOKEY,NOKEY,NOKEY,NOKEY}},
//		{x_("get-old-version"),      1,{&projoldp,NOKEY,NOKEY,NOKEY,NOKEY}},
//		{x_("add-cell"),             1,{&projap,NOKEY,NOKEY,NOKEY,NOKEY}},
//		{x_("delete-cell"),          1,{&projdp,NOKEY,NOKEY,NOKEY,NOKEY}},
//		{x_("update"),               0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//		{x_("set-user"),             0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//		{x_("list-cells"),           0,{NOKEY,NOKEY,NOKEY,NOKEY,NOKEY}},
//		TERMKEY
//	};
//	COMCOMP proj_projp = {projopt, NOTOPLIST, NONEXTLIST, NOPARAMS,
//		0, x_(" \t"), M_("Project management tool action"), x_("")};
//	
//	void proj_slice(void)
//	{
//		REGISTER FCHECK *f, *nextf;
//		REGISTER NODEPROTO *np;
//		TOOL *tool;
//		REGISTER INTBIG lowbatch, highbatch, retval, undonecells;
//		REGISTER void *infstr;
//	
//		if (!proj_active) return;
//		if (proj_firstfcheck == NOFCHECK) return;
//	
//		undonecells = 0;
//		for(f = proj_firstfcheck; f != NOFCHECK; f = nextf)
//		{
//			nextf = f.nextfcheck;
//			np = f.entry;
//	
//			// make sure cell np is checked-out
//			if (getvalkey((INTBIG)np, VNODEPROTO, VINTEGER, proj_lockedkey) != NOVARIABLE)
//			{
//				if (undonecells == 0)
//				{
//					infstr = initinfstr();
//					lowbatch = highbatch = f.batchnumber;
//				} else
//				{
//					if (f.batchnumber < lowbatch) lowbatch = f.batchnumber;
//					if (f.batchnumber > highbatch) highbatch = f.batchnumber;
//					addstringtoinfstr(infstr, x_(", "));
//				}
//				addstringtoinfstr(infstr, describenodeproto(np));
//				undonecells++;
//			}
//		}
//		proj_firstfcheck = NOFCHECK;
//		if (undonecells > 0)
//		{
//			ttyputerr(_("Cannot change unchecked-out %s: %s"),
//				makeplural(_("cell"), undonecells),
//				returninfstr(infstr));
//			proj_ignorechanges = TRUE;
//			for(;;)
//			{
//				retval = undoabatch(&tool);
//				if (retval == 0) break;
//				if (retval < 0) retval = -retval;
//				if (retval <= lowbatch) break;
//			}
//			noredoallowed();
//			proj_ignorechanges = FALSE;
//		}
//	}
//	
//	void proj_set(INTBIG count, CHAR *par[])
//	{
//		REGISTER INTBIG l;
//		REGISTER CHAR *pp, *cellname;
//		REGISTER NODEPROTO *np;
//	
//		if (count <= 0)
//		{
//			ttyputerr(_("Missing command to display tool"));
//			return;
//		}
//		l = estrlen(pp = par[0]);
//		if (namesamen(pp, x_("add-cell"), l) == 0)
//		{
//			if (count >= 2) cellname = par[1]; else
//			{
//				np = getcurcell();
//				if (np == NONODEPROTO)
//				{
//					ttyputerr(_("No current cell to add"));
//					return;
//				}
//				cellname = describenodeproto(np);
//			}
//			proj_addcell(cellname);
//			proj_active = TRUE;
//			return;
//		}
//		if (namesamen(pp, x_("build-project"), l) == 0)
//		{
//			if (el_curlib == NOLIBRARY)
//			{
//				ttyputerr(_("No current library to enter"));
//				return;
//			}
//			proj_buildproject(el_curlib);
//			proj_active = TRUE;
//			return;
//		}
//		if (namesamen(pp, x_("check-in"), l) == 0 && l >= 7)
//		{
//			if (count >= 2) cellname = par[1]; else
//			{
//				np = getcurcell();
//				if (np == NONODEPROTO)
//				{
//					ttyputerr(_("No current cell to check in"));
//					return;
//				}
//				cellname = describenodeproto(np);
//			}
//			proj_checkin(cellname);
//			proj_active = TRUE;
//			return;
//		}
//		if (namesamen(pp, x_("check-out"), l) == 0 && l >= 7)
//		{
//			if (count >= 2) cellname = par[1]; else
//			{
//				np = getcurcell();
//				if (np == NONODEPROTO)
//				{
//					ttyputerr(_("No current cell to check out"));
//					return;
//				}
//				cellname = describenodeproto(np);
//			}
//			proj_checkout(cellname, TRUE);
//			proj_active = TRUE;
//			return;
//		}
//		if (namesamen(pp, x_("delete-cell"), l) == 0)
//		{
//			if (count >= 2) cellname = par[1]; else
//			{
//				np = getcurcell();
//				if (np == NONODEPROTO)
//				{
//					ttyputerr(_("No current cell to delete"));
//					return;
//				}
//				cellname = describenodeproto(np);
//			}
//			proj_deletecell(cellname);
//			proj_active = TRUE;
//			return;
//		}
//		if (namesamen(pp, x_("get-old-version"), l) == 0)
//		{
//			if (count >= 2) cellname = par[1]; else
//			{
//				np = getcurcell();
//				if (np == NONODEPROTO)
//				{
//					ttyputerr(_("No current cell to retrieve old versions"));
//					return;
//				}
//				cellname = describenodeproto(np);
//			}
//			proj_getoldversion(cellname);
//			proj_active = TRUE;
//			return;
//		}
//		if (namesamen(pp, x_("update"), l) == 0)
//		{
//			if (el_curlib == NOLIBRARY)
//			{
//				ttyputerr(_("No current library to update"));
//				return;
//			}
//			proj_update(el_curlib);
//			proj_active = TRUE;
//			return;
//		}
//	}
//	
//	/************************ PROJECT MANAGEMENT ***********************/
//	
//	/* Project Old Version */
//	static DIALOGITEM proj_oldversdialogitems[] =
//	{
//	 /*  1 */ {0, {176,224,200,288}, BUTTON, N_("OK")},
//	 /*  2 */ {0, {176,16,200,80}, BUTTON, N_("Cancel")},
//	 /*  3 */ {0, {4,8,20,140}, MESSAGE, N_("Old version of cell")},
//	 /*  4 */ {0, {28,12,166,312}, SCROLL, x_("")},
//	 /*  5 */ {0, {4,140,20,312}, MESSAGE, x_("")}
//	};
//	static DIALOG proj_oldversdialog = {{108,75,318,396}, N_("Get Old Version of Cell"), 0, 5, proj_oldversdialogitems, 0, 0};
//	
//	/* special items for the "get old version" dialog: */
//	#define DPRV_CELLLIST  4		/* Cell list (scroll) */
//	#define DPRV_CELLNAME  5		/* Cell name (stat text) */
//	
//	void proj_getoldversion(CHAR *cellname)
//	{
//		PROJECTCELL *pf, statpf;
//		REGISTER INTBIG version;
//		REGISTER time_t date;
//		REGISTER INTBIG itemHit, count, i, len;
//		REGISTER LIBRARY *lib;
//		REGISTER NODEPROTO *np;
//		CHAR line[256], projectpath[256], projectfile[256], **filelist, sep[2], *pt;
//		REGISTER void *dia;
//	
//		// find out which cell is being checked out
//		np = getnodeproto(cellname);
//		if (np == NONODEPROTO)
//		{
//			ttyputerr(_("Cannot identify cell '%s'"), cellname);
//			return;
//		}
//		lib = np.lib;
//	
//		// get location of project file
//		if (proj_getprojinfo(lib, projectpath, projectfile))
//		{
//			ttyputerr(_("Cannot find project file"));
//			return;
//		}
//	
//		if (proj_readprojectfile(projectpath, projectfile))
//		{
//			ttyputerr(_("Cannot read project file"));
//			return;
//		}
//	
//		pf = proj_findcell(np);
//		if (pf == NOPROJECTCELL)
//		{
//			ttyputerr(_("Cell %s is not in the project"), describenodeproto(np));
//			return;
//		}
//	
//		// find all files in the directory for this cell
//		projectfile[estrlen(projectfile)-5] = 0;
//		estrcat(projectpath, projectfile);
//		estrcat(projectpath, DIRSEPSTR);
//		estrcat(projectpath, pf.cellname);
//		estrcat(projectpath, DIRSEPSTR);
//		count = filesindirectory(projectpath, &filelist);
//	
//		dia = DiaInitDialog(&proj_oldversdialog);
//		if (dia == 0) return;
//		DiaSetText(dia, DPRV_CELLNAME, cellname);
//		DiaInitTextDialog(dia, DPRV_CELLLIST, DiaNullDlogList, DiaNullDlogItem, DiaNullDlogDone, -1,
//			SCSELMOUSE);
//		for(i=0; i<count; i++)
//		{
//			estrcpy(line, filelist[i]);
//			len = estrlen(line);
//			if (estrcmp(&line[len-5], x_(".elib")) != 0) continue;
//			line[len-5] = 0;
//			version = eatoi(line);
//			if (version <= 0) continue;
//			if (version >= pf.cellversion) continue;
//			for(pt = line; *pt != 0; pt++) if (*pt == '-') break;
//			if (*pt != '-') continue;
//			if (estrcmp(&pt[1], pf.cellview.viewname) != 0) continue;
//	
//			// file is good, display it
//			estrcpy(projectfile, projectpath);
//			estrcat(projectfile, sep);
//			estrcat(projectfile, filelist[i]);
//			date = filedate(projectfile);
//			esnprintf(line, 256, _("Version %ld, %s"), version, timetostring(date));
//			DiaStuffLine(dia, DPRV_CELLLIST, line);
//		}
//		DiaSelectLine(dia, DPRV_CELLLIST, -1);
//		for(;;)
//		{
//			itemHit = DiaNextHit(dia);
//			if (itemHit == OK || itemHit == CANCEL) break;
//		}
//		version = -1;
//		i = DiaGetCurLine(dia, DPRV_CELLLIST);
//		if (i >= 0)
//		{
//			pt = DiaGetScrollLine(dia, DPRV_CELLLIST, i);
//			if (estrlen(pt) > 8) version = eatoi(&pt[8]);
//		}
//		DiaDoneDialog(dia);
//		if (itemHit == CANCEL) return;
//		if (version < 0) return;
//	
//		// build a fake record to describe this cell
//		statpf = *pf;
//		statpf.cellversion = version;
//	
//		np = proj_getcell(&statpf, lib);
//		if (np == NONODEPROTO)
//			ttyputerr(_("Error retrieving old version of cell"));
//		proj_marklocked(np, FALSE);
//		(*el_curconstraint.solve)(np);
//		ttyputmsg(_("Cell %s is now in this library"), describenodeproto(np));
//	}
//	
//	void proj_update(LIBRARY *lib)
//	{
//		CHAR projectpath[256], projectfile[256];
//		PROJECTCELL *pf;
//		REGISTER INTBIG total;
//		REGISTER NODEPROTO *oldnp, *newnp;
//	
//		// make sure there is a valid user name
//		if (proj_getusername(lib))
//		{
//			ttyputerr(_("No valid user"));
//			return;
//		}
//	
//		// get location of project file
//		if (proj_getprojinfo(lib, projectpath, projectfile))
//		{
//			ttyputerr(_("Cannot find project file"));
//			return;
//		}
//	
//		// lock the project file
//		if (proj_lockprojfile(projectpath, projectfile))
//		{
//			ttyputerr(_("Couldn't lock project file"));
//			return;
//		}
//	
//		// read the project file
//		us_clearhighlightcount();
//		if (proj_readprojectfile(projectpath, projectfile))
//			ttyputerr(_("Cannot read project file")); else
//		{
//			// check to see which cells are changed/added
//			total = 0;
//			for(pf = proj_firstprojectcell; pf != NOPROJECTCELL; pf = pf.nextprojectcell)
//			{
//				oldnp = db_findnodeprotoname(pf.cellname, pf.cellview, el_curlib);
//				if (oldnp != NONODEPROTO && oldnp.version >= pf.cellversion) continue;
//	
//				// this is a new one
//				newnp = proj_getcell(pf, lib);
//				if (newnp == NONODEPROTO)
//				{
//					if (pf.cellview == el_unknownview)
//					{
//						ttyputerr(_("Error bringing in %s;%ld"), pf.cellname, pf.cellversion);
//					} else
//					{
//						ttyputerr(_("Error bringing in %s{%s};%ld"), pf.cellname, pf.cellview.sviewname,
//							pf.cellversion);
//					}
//				} else
//				{
//					if (oldnp != NONODEPROTO && proj_usenewestversion(oldnp, newnp))
//						ttyputerr(_("Error replacing instances of new %s"), describenodeproto(oldnp)); else
//					{
//						if (pf.cellview == el_unknownview)
//						{
//							ttyputmsg(_("Brought in cell %s;%ld"), pf.cellname, pf.cellversion);
//						} else
//						{
//							ttyputmsg(_("Brought in cell %s{%s};%ld"), pf.cellname, pf.cellview.sviewname,
//								pf.cellversion);
//						}
//						total++;
//					}
//				}
//			}
//		}
//	
//		// relase project file lock
//		proj_unlockprojfile(projectpath, projectfile);
//	
//		// make sure all cell locks are correct
//		proj_validatelocks(lib);
//	
//		// summarize
//		if (total == 0) ttyputmsg(_("Project is up-to-date")); else
//			ttyputmsg(_("Updated %ld %s"), total, makeplural(_("cell"), total));
//	}
//	
//	void proj_buildproject(LIBRARY *lib)
//	{
//		CHAR libraryname[256], librarypath[256], newname[256], projfile[256], *pars[2];
//		REGISTER INTBIG i, extpos;
//		REGISTER NODEPROTO *np;
//		FILE *io;
//	
//		// verify that a project is to be built
//		us_noyesdlog(_("Are you sure you want to create a multi-user project from this library?"), pars);
//		if (namesame(pars[0], x_("yes")) != 0) return;
//	
//		// get path prefix for cell libraries
//		estrcpy(librarypath, lib.libfile);
//		extpos = -1;
//		for(i = estrlen(librarypath)-1; i > 0; i--)
//		{
//			if (librarypath[i] == DIRSEP) break;
//			if (librarypath[i] == '.')
//			{
//				if (extpos < 0) extpos = i;
//			}
//		}
//		if (extpos < 0) estrcat(librarypath, x_("ELIB")); else
//			estrcpy(&librarypath[extpos], x_("ELIB"));
//		if (librarypath[i] == DIRSEP)
//		{
//			estrcpy(libraryname, &librarypath[i+1]);
//			librarypath[i] = 0;
//		} else
//		{
//			estrcpy(libraryname, librarypath);
//			librarypath[0] = 0;
//		}
//	
//		// create the top-level directory for this library
//		estrcpy(newname, librarypath);
//		estrcat(newname, DIRSEPSTR);
//		estrcat(newname, libraryname);
//		if (fileexistence(newname) != 0)
//		{
//			ttyputerr(_("Project directory '%s' already exists"), newname);
//			return;
//		}
//		if (createdirectory(newname))
//		{
//			ttyputerr(_("Could not create project directory '%s'"), newname);
//			return;
//		}
//		ttyputmsg(_("Making project directory '%s'..."), newname);
//	
//		// create the project file
//		estrcpy(projfile, librarypath);
//		estrcat(projfile, DIRSEPSTR);
//		estrcat(projfile, libraryname);
//		estrcat(projfile, x_(".proj"));
//		io = xcreate(projfile, proj_filetypeproj, 0, 0);
//		if (io == NULL)
//		{
//			ttyputerr(_("Could not create project file '%s'"), projfile);
//			return;
//		}
//	
//		// turn off all tools
//		Undo.changesQuiet(true);
//	
//		// make libraries for every cell
//		setvalkey((INTBIG)lib, VLIBRARY, proj_pathkey, (INTBIG)projfile, VSTRING);
//		for(np = lib.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//		{
//			// ignore old unused cell versions
//			if (np.newestversion != np)
//			{
//				if (np.firstinst == NONODEINST) continue;
//				ttyputmsg(_("Warning: including old version of cell %s"), describenodeproto(np));
//			}
//	
//			// write the cell to disk in its own library
//			ttyputmsg(_("Entering cell %s"), describenodeproto(np));
//			if (proj_writecell(np)) break;
//	
//			// make an entry in the project file
//			xprintf(io, _(":%s:%s:%ld-%s.elib:::Initial checkin\n"), libraryname, np.protoname,
//				np.version, np.cellview.viewname);
//	
//			// mark this cell "checked in" and locked
//			proj_marklocked(np, TRUE);
//		}
//	
//		// restore tool state
//		Undo.changesQuiet(false);
//	
//		// close project file
//		xclose(io);
//	
//		// make sure library variables are proper
//		if (getvalkey((INTBIG)lib, VLIBRARY, VSTRING, proj_userkey) != NOVARIABLE)
//			(void)delvalkey((INTBIG)lib, VLIBRARY, proj_userkey);
//	
//		// advise the user of this library
//		ttyputmsg(_("The current library should be saved and used by new users"));
//	}
//	
//	void proj_addcell(CHAR *cellname)
//	{
//		REGISTER NODEPROTO *np;
//		REGISTER LIBRARY *lib;
//		CHAR projectpath[256], projectfile[256], libname[256];
//		PROJECTCELL *pf, *lastpf;
//	
//		// find out which cell is being added
//		np = getnodeproto(cellname);
//		if (np == NONODEPROTO)
//		{
//			ttyputerr(_("Cannot identify cell '%s'"), cellname);
//			return;
//		}
//		lib = np.lib;
//		if (np.newestversion != np)
//		{
//			ttyputerr(_("Cannot add an old version of the cell"));
//			return;
//		}
//	
//		// make sure there is a valid user name
//		if (proj_getusername(lib))
//		{
//			ttyputerr(_("No valid user"));
//			return;
//		}
//	
//		// get location of project file
//		if (proj_getprojinfo(lib, projectpath, projectfile))
//		{
//			ttyputerr(_("Cannot find project file"));
//			return;
//		}
//	
//		// lock the project file
//		if (proj_lockprojfile(projectpath, projectfile))
//		{
//			ttyputerr(_("Couldn't lock project file"));
//			return;
//		}
//	
//		// read the project file
//		if (proj_readprojectfile(projectpath, projectfile))
//			ttyputerr(_("Cannot read project file")); else
//		{
//			// find this in the project file
//			lastpf = NOPROJECTCELL;
//			for(pf = proj_firstprojectcell; pf != NOPROJECTCELL; pf = pf.nextprojectcell)
//			{
//				if (estrcmp(pf.cellname, np.protoname) == 0 &&
//					pf.cellview == np.cellview) break;
//				lastpf = pf;
//			}
//			if (pf != NOPROJECTCELL) ttyputerr(_("This cell is already in the project")); else
//			{
//				if (proj_startwritingprojectfile(projectpath, projectfile))
//					ttyputerr(_("Cannot write project file")); else
//				{
//					if (proj_writecell(np))
//						ttyputerr(_("Error writing cell file")); else
//					{
//						// create new entry for this cell
//						pf = proj_allocprojectcell();
//						if (pf == 0)
//							ttyputerr(_("Cannot add project record")); else
//						{
//							estrcpy(libname, projectfile);
//							libname[estrlen(libname)-5] = 0;
//							(void)allocstring(&pf.libname, libname, proj_tool.cluster);
//							(void)allocstring(&pf.cellname, np.protoname, proj_tool.cluster);
//							pf.cellview = np.cellview;
//							pf.cellversion = np.version;
//							(void)allocstring(&pf.owner, x_(""), proj_tool.cluster);
//							(void)allocstring(&pf.lastowner, proj_username, proj_tool.cluster);
//							(void)allocstring(&pf.comment, _("Initial checkin"), proj_tool.cluster);
//	
//							// link it in
//							pf.nextprojectcell = NOPROJECTCELL;
//							if (lastpf == NOPROJECTCELL) proj_firstprojectcell = pf; else
//								lastpf.nextprojectcell = pf;
//	
//							// mark this cell "checked in" and locked
//							proj_marklocked(np, TRUE);
//	
//							ttyputmsg(_("Cell %s added to the project"), describenodeproto(np));
//						}
//					}
//	
//					// save new project file
//					proj_endwritingprojectfile();
//				}
//			}
//		}
//	
//		// relase project file lock
//		proj_unlockprojfile(projectpath, projectfile);
//	}
//	
//	void proj_deletecell(CHAR *cellname)
//	{
//		REGISTER NODEPROTO *np, *onp;
//		REGISTER NODEINST *ni;
//		REGISTER LIBRARY *lib, *olib;
//		CHAR projectpath[256], projectfile[256], *pt;
//		PROJECTCELL *pf, *lastpf;
//		REGISTER void *infstr;
//	
//		// find out which cell is being deleted
//		np = getnodeproto(cellname);
//		if (np == NONODEPROTO)
//		{
//			ttyputerr(_("Cannot identify cell '%s'"), cellname);
//			return;
//		}
//		lib = np.lib;
//	
//		// make sure the cell is not being used
//		if (np.firstinst != NONODEINST)
//		{
//			for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//				for(onp = olib.firstnodeproto; onp != NONODEPROTO; onp = onp.nextnodeproto)
//					onp.temp1 = 0;
//			for(ni = np.firstinst; ni != NONODEINST; ni = ni.nextinst)
//				ni.parent.temp1++;
//			ttyputerr(_("Cannot delete cell %s because it is still being used by:"), cellname);
//			for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//				for(onp = olib.firstnodeproto; onp != NONODEPROTO; onp = onp.nextnodeproto)
//					if (onp.temp1 != 0)
//						ttyputmsg(_("   Cell %s has %ld instance(s)"), describenodeproto(onp), onp.temp1);
//			return;
//		}
//	
//		// make sure there is a valid user name
//		if (proj_getusername(lib))
//		{
//			ttyputerr(_("No valid user"));
//			return;
//		}
//	
//		// get location of project file
//		if (proj_getprojinfo(lib, projectpath, projectfile))
//		{
//			ttyputerr(_("Cannot find project file"));
//			return;
//		}
//	
//		// lock the project file
//		if (proj_lockprojfile(projectpath, projectfile))
//		{
//			ttyputerr(_("Couldn't lock project file"));
//			return;
//		}
//	
//		// read the project file
//		if (proj_readprojectfile(projectpath, projectfile))
//			ttyputerr(_("Cannot read project file")); else
//		{
//			// make sure the user has no cells checked-out
//			for(pf = proj_firstprojectcell; pf != NOPROJECTCELL; pf = pf.nextprojectcell)
//				if (namesame(pf.owner, proj_username) == 0) break;
//			if (pf != NOPROJECTCELL)
//			{
//				ttyputerr(_("Before deleting a cell from the project, you must check-in all of your work."));
//				ttyputerr(_("This is because the deletion may be dependent upon changes recently made."));
//				infstr = initinfstr();
//				for(pf = proj_firstprojectcell; pf != NOPROJECTCELL; pf = pf.nextprojectcell)
//				{
//					if (namesame(pf.owner, proj_username) != 0) continue;
//					addstringtoinfstr(infstr, pf.cellname);
//					if (pf.cellview != el_unknownview)
//					{
//						addstringtoinfstr(infstr, x_("{"));
//						addstringtoinfstr(infstr, pf.cellview.sviewname);
//						addstringtoinfstr(infstr, x_("}"));
//					}
//					addstringtoinfstr(infstr, x_(", "));
//				}
//				pt = returninfstr(infstr);
//				pt[estrlen(pt)-2] = 0;
//				ttyputerr(_("These cells are checked out to you: %s"), pt);
//			} else
//			{
//				// find this in the project file
//				lastpf = NOPROJECTCELL;
//				for(pf = proj_firstprojectcell; pf != NOPROJECTCELL; pf = pf.nextprojectcell)
//				{
//					if (estrcmp(pf.cellname, np.protoname) == 0 &&
//						pf.cellview == np.cellview) break;
//					lastpf = pf;
//				}
//				if (pf == NOPROJECTCELL) ttyputerr(_("This cell is not in the project")); else
//				{
//					if (proj_startwritingprojectfile(projectpath, projectfile))
//						ttyputerr(_("Cannot write project file")); else
//					{
//						// unlink it
//						if (lastpf == NOPROJECTCELL)
//							proj_firstprojectcell = pf.nextprojectcell; else
//								lastpf.nextprojectcell = pf.nextprojectcell;
//	
//						// delete the entry
//						efree(pf.libname);
//						efree(pf.cellname);
//						efree(pf.owner);
//						efree(pf.lastowner);
//						efree(pf.comment);
//						proj_freeprojectcell(pf);
//	
//						// mark this cell unlocked
//						proj_marklocked(np, FALSE);
//	
//						// save new project file
//						proj_endwritingprojectfile();
//	
//						ttyputmsg(_("Cell %s deleted from the project"), describenodeproto(np));
//					}
//				}
//			}
//		}
//	
//		// relase project file lock
//		proj_unlockprojfile(projectpath, projectfile);
//	}
	
	/************************ PROJECT DATABASE ***********************/

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
			super("Check in cells", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.lib = lib;
			this.cellsMarked1 = cellsMarked1;
			startJob();
		}

		public boolean doIt()
		{
			// make sure there is a valid user name
			if (proj_getusername(lib))
			{
				System.out.println("No valid user");
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
//							if (!proj_getcomments(pf, "in"))
							{
								// write the cell out there
								if (proj_writecell(np, pl, pf))
									System.out.println("Error writing cell " + np.describe()); else
								{
									pf.owner = "";
									pf.lastowner = proj_username;
									pf.cellversion = np.getVersion();
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
	
//	/*
//	 * Routine to obtain information about the project associated with library "lib".
//	 * The path to the project is placed in "path" and the name of the project file
//	 * in that directory is placed in "projfile".  Returns true on error.
//	 */
//	static String [] proj_getprojinfo(Library lib) // CHAR *path, CHAR *projfile)
//	{
//		// see if there is a variable in the current library with the project path
//		String path = null;
//		Variable var = lib.getVar(proj_pathkey);
//		if (var != null)
//		{
//			path = (String)var.getObject();
//			// make sure the path is valid
//			io = xopen(path, proj_filetypeproj, x_(""), &truename);
//			if (io != 0) xclose(io); else
//				var = NOVARIABLE;
//		}
//		if (var == null)
//		{
//			// prompt for project file
//			infstr = initinfstr();
//			addstringtoinfstr(infstr, x_("proj/"));
//			addstringtoinfstr(infstr, _("Project File"));
//			i = ttygetparam(returninfstr(infstr), &us_colorreadp, 1, params);
//			if (i == 0) return(TRUE);
//			estrcpy(path, params[0]);
//			setvalkey((INTBIG)lib, VLIBRARY, proj_pathkey, (INTBIG)path, VSTRING);
//		}
//	
//		int sepPos = path.lastIndexOf(File.separatorChar);
//		String projFile;
//		if (sepPos >= 0)
//		{
//			projFile = path.substring(sepPos+1);
//			path = path.substring(0, sepPos);
//		} else
//		{
//			projFile = path;
//			path = "";
//		}
//		String [] ret = new String[2];
//		ret[0] = projFile;
//		ret[1] = path;
//		return ret;
//	}
	
//	/* Project Comments */
//	static DIALOGITEM proj_commentsdialogitems[] =
//	{
//	 /*  1 */ {0, {104,244,128,308}, BUTTON, N_("OK")},
//	 /*  2 */ {0, {104,48,128,112}, BUTTON, N_("Cancel")},
//	 /*  3 */ {0, {4,8,20,363}, MESSAGE, N_("Reason for checking out cell")},
//	 /*  4 */ {0, {28,12,92,358}, EDITTEXT, x_("")}
//	};
//	static DIALOG proj_commentsdialog = {{108,75,248,447}, N_("Project Comments"), 0, 4, proj_commentsdialogitems, 0, 0};
//	
//	/* special items for the "Project list" dialog: */
//	#define DPRC_COMMENT_L  3		/* Comment label (stat text) */
//	#define DPRC_COMMENT    4		/* Comment (edit text) */
//	
//	/*
//	 * Routine to obtain comments about a checkin/checkout for cell "pf".  "direction" is
//	 * either "in" or "out".  Returns true if the dialog is cancelled.
//	 */
//	BOOLEAN proj_getcomments(PROJECTCELL *pf, CHAR *direction)
//	{
//		REGISTER INTBIG itemHit;
//		static CHAR line[256];
//		REGISTER void *dia;
//	
//		dia = DiaInitDialog(&proj_commentsdialog);
//		if (dia == 0) return(TRUE);
//		esnprintf(line, 256, _("Reason for checking-%s cell %s"), direction, pf.cellname);
//		DiaSetText(dia, DPRC_COMMENT_L, line);
//		DiaSetText(dia, -DPRC_COMMENT, pf.comment);
//		for(;;)
//		{
//			itemHit = DiaNextHit(dia);
//			if (itemHit == CANCEL || itemHit == OK) break;
//		}
//		if (itemHit == OK)
//			(void)reallocstring(&pf.comment, DiaGetText(dia, DPRC_COMMENT), proj_tool.cluster);
//		DiaDoneDialog(dia);
//		if (itemHit == CANCEL) return(TRUE);
//		return(FALSE);
//	}
	
//	
//	BOOLEAN proj_startwritingprojectfile(CHAR *pathname, CHAR *filename)
//	{
//		CHAR *truename, fullname[256];
//	
//		// read the project file
//		estrcpy(fullname, pathname);
//		estrcat(fullname, filename);
//		proj_io = xopen(fullname, proj_filetypeproj | FILETYPEWRITE, x_(""), &truename);
//		if (proj_io == 0)
//		{
//			ttyputerr(_("Couldn't write project file '%s'"), fullname);
//			return(TRUE);
//		}
//		return(FALSE);
//	}
//	
//	void proj_endwritingprojectfile(void)
//	{
//		PROJECTCELL *pf;
//	
//		for(pf = proj_firstprojectcell; pf != NOPROJECTCELL; pf = pf.nextprojectcell)
//			xprintf(proj_io, x_(":%s:%s:%ld-%s.elib:%s:%s:%s\n"), pf.libname, pf.cellname,
//				pf.cellversion, pf.cellview.viewname, pf.owner, pf.lastowner, pf.comment);
//		xclose(proj_io);
//		noundoallowed();
//	}
//	
//	PROJECTCELL *proj_allocprojectcell(void)
//	{
//		PROJECTCELL *pf;
//	
//		if (proj_projectcellfree != NOPROJECTCELL)
//		{
//			pf = proj_projectcellfree;
//			proj_projectcellfree = proj_projectcellfree.nextprojectcell;
//		} else
//		{
//			pf = (PROJECTCELL *)emalloc(sizeof (PROJECTCELL), proj_tool.cluster);
//			if (pf == 0) return(0);
//		}
//		return(pf);
//	}
//	
//	void proj_freeprojectcell(PROJECTCELL *pf)
//	{
//		pf.nextprojectcell = proj_projectcellfree;
//		proj_projectcellfree = pf;
//	}
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
	/************************ LOCKING ***********************/
	
//	void proj_unlockprojfile(CHAR *projectpath, CHAR *projectfile)
//	{
//		CHAR lockfilename[256];
//	
//		esnprintf(lockfilename, 256, x_("%s%sLOCK"), projectpath, projectfile);
//		unlockfile(lockfilename);
//	}
//	
//	void proj_validatelocks(LIBRARY *lib)
//	{
//		REGISTER NODEPROTO *np;
//		PROJECTCELL *pf;
//	
//		for(np = lib.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//		{
//			pf = proj_findcell(np);
//			if (pf == NOPROJECTCELL)
//			{
//				// cell not in the project: writable
//				proj_marklocked(np, FALSE);
//			} else
//			{
//				if (np.version < pf.cellversion)
//				{
//					// cell is an old version: writable
//					proj_marklocked(np, FALSE);
//				} else
//				{
//					if (namesame(pf.owner, proj_username) == 0)
//					{
//						// cell checked out to current user: writable
//						proj_marklocked(np, FALSE);
//					} else
//					{
//						// cell checked out to someone else: not writable
//						proj_marklocked(np, TRUE);
//					}
//				}
//			}
//		}
//	}
	
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

//	/************************ COPYING CELLS IN AND OUT OF DATABASE ***********************/
//	
//	/*
//	 * Routine to get the latest version of the cell described by "pf" and return
//	 * the newly created cell.  Returns NONODEPROTO on error.
//	 */
//	NODEPROTO *proj_getcell(PROJECTCELL *pf, LIBRARY *lib)
//	{
//		CHAR celllibname[256], celllibpath[256], cellprojfile[256], cellname[256],
//			*templibname;
//		REGISTER LIBRARY *flib;
//		REGISTER NODEPROTO *newnp;
//		REGISTER INTBIG oldverbose, ret;
//	
//		// create the library
//		if (proj_getprojinfo(lib, celllibpath, cellprojfile))
//		{
//			ttyputerr(_("Cannot find project info on library %s"), lib.libname);
//			return(NONODEPROTO);
//		}
//		esnprintf(celllibname, 256, x_("%ld-%s.elib"), pf.cellversion, pf.cellview.viewname);
//		cellprojfile[estrlen(cellprojfile)-5] = 0;
//		estrcat(celllibpath, cellprojfile);
//		estrcat(celllibpath, DIRSEPSTR);
//		estrcat(celllibpath, pf.cellname);
//		estrcat(celllibpath, DIRSEPSTR);
//		estrcat(celllibpath, celllibname);
//	
//		// prevent tools (including this one) from seeing the change
//		Undo.changesQuiet(true);
//	
//		templibname = proj_templibraryname();
//		flib = newlibrary(templibname, celllibpath);
//		if (flib == NOLIBRARY)
//		{
//			ttyputerr(_("Cannot create library %s"), celllibpath);
//			Undo.changesQuiet(false);
//			return(NONODEPROTO);
//		}
//		oldverbose = asktool(io_tool, x_("verbose"), 0);
//		ret = asktool(io_tool, x_("read"), (INTBIG)flib, (INTBIG)x_("binary"));
//		(void)asktool(io_tool, x_("verbose"), oldverbose);
//		if (ret != 0)
//		{
//			ttyputerr(_("Cannot read library %s"), celllibpath);
//			killlibrary(flib);
//			Undo.changesQuiet(false);
//			return(NONODEPROTO);
//		}
//		if (flib.curnodeproto == NONODEPROTO)
//		{
//			ttyputerr(_("Cannot find cell in library %s"), celllibpath);
//			killlibrary(flib);
//			Undo.changesQuiet(false);
//			return(NONODEPROTO);
//		}
//		esnprintf(cellname, 256, x_("%s;%ld"), flib.curnodeproto.protoname, flib.curnodeproto.version);
//		if (flib.curnodeproto.cellview != el_unknownview)
//		{
//			estrcat(cellname, x_("{"));
//			estrcat(cellname, flib.curnodeproto.cellview.sviewname);
//			estrcat(cellname, x_("}"));
//		}
//		newnp = copynodeproto(flib.curnodeproto, lib, cellname, TRUE);
//		if (newnp == NONODEPROTO)
//		{
//			ttyputerr(_("Cannot copy cell %s from new library"), describenodeproto(flib.curnodeproto));
//			killlibrary(flib);
//			Undo.changesQuiet(false);
//			return(NONODEPROTO);
//		}
//		(*el_curconstraint.solve)(newnp);
//	
//		// must do this explicitly because the library kill flushes change batches
//		// (void)asktool(net_tool, "re-number", (INTBIG)newnp);
//	
//		// kill the library
//		killlibrary(flib);
//	
//		// restore tool state
//		Undo.changesQuiet(false);
//	
//		// return the new cell
//		return(newnp);
//	}
	
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

		String libName = dirName + File.separator + np.getVersion() + "-" + np.getName() + ".elib";
	
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
	
	/************************ DATABASE OPERATIONS ***********************/
	
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
	
}
