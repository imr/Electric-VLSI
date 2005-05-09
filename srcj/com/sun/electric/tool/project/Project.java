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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;


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
		/** name of the library */							String libname;
		/** name of the cell */								String cellname;
		/** cell view */									View   cellview;
		/** cell version */									int    cellversion;
		/** the actual cell (if known) */					Cell   cell;
		/** current owner of this cell (if checked out) */	String owner;
		/** previous owner of this cell (if checked in) */	String lastowner;
		/** comments for this cell */						String comment;
	}

	private static class ProjectLibrary
	{
		List    allCells;
		HashMap byCell;

		ProjectLibrary()
		{
			allCells = new ArrayList();
			byCell = new HashMap();
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
//	static INTBIG       *proj_savetoolstate = 0;	/* saved tool state information */
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

	public static void checkInAndOut()
	{
//		String editedValue = JOptionPane.showInputDialog("The Value:", "initial");
//		if (editedValue == null) return;
	}
	public static void buildRepository()
	{
//		String editedValue = JOptionPane.showInputDialog("The Value:", "initial");
//		if (editedValue == null) return;
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
		ProjectLibrary pl = ensureProjectFile(cell.getLibrary());
		if (pl.allCells.size() == 0) return NOTMANAGED;
		ProjectCell pc = (ProjectCell)pl.byCell.get(cell);
		if (pc == null) return NOTMANAGED;
		if (pc.owner == null) return CHECKEDIN;
		if (pc.owner.equals(getCurrentUserName())) return CHECKEDOUTTOYOU;
		return CHECKEDOUTTOOTHERS;
	}

	public static boolean checkOut(Cell cell)
	{
		return false;
	}

	public static boolean checkIn(Cell cell)
	{
		return false;
	}

	/************************ SUPPORT ***********************/

	/**
	 * Method to ensure that there is project information for a given library.
	 * @param lib the Library to check.
	 * @return a ProjectLibrary object for the Library.  If the library is marked
	 * as being part of a project, that project file is read in.  If the library is
	 * not in a project, the returned object has nothing in it.
	 */
	private static ProjectLibrary ensureProjectFile(Library lib)
	{
		// see if this library has a known project database
		ProjectLibrary pl = (ProjectLibrary)libraryProjectInfo.get(lib);
		if (pl != null) return pl;

		// not known: create a new project database for this library
		pl = new ProjectLibrary();
		libraryProjectInfo.put(lib, pl);

		// if the library isn't marked with a project file, stop now
		Variable var = lib.getVar(proj_pathkey);
		if (var == null) return pl;

		// read the project file
		String userFile = (String)var.getObject();
		URL url = TextUtils.makeURLToFile(userFile);
        if (!TextUtils.URLExists(url))
        {
        	url = null;
    		int sepPos = userFile.lastIndexOf(File.separatorChar);
    		if (sepPos >= 0)
    		{
    			userFile = getRepositoryLocation() + File.separator + userFile.substring(sepPos+1);
    			url = TextUtils.makeURLToFile(userFile);
    			if (!TextUtils.URLExists(url)) url = null;
    		}
    		if (url == null)
    		{
    			userFile = OpenFile.chooseInputFile(FileType.PROJECT, "Select Project File");
    			if (userFile == null) return pl;
    			url = TextUtils.makeURLToFile(userFile);
    		}
        }
		try
		{
			URLConnection urlCon = url.openConnection();
			InputStreamReader is = new InputStreamReader(urlCon.getInputStream());
			LineNumberReader lnr = new LineNumberReader(is);

			for(;;)
			{
				String userLine = lnr.readLine();
				if (userLine == null) break;

				ProjectCell pf = new ProjectCell();
				String [] sections = userLine.split("\\:");
				if (sections.length < 7)
				{
					System.out.println("Too few keywords in project file: " + userLine);
					return null;
				}
				if (sections[0].length() > 0)
				{
					System.out.println("Missing initial ':' in project file: " + userLine);
					return null;
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
					return null;
				}
				pf.cellversion = TextUtils.atoi(sections[3].substring(0, dashPos));
		
				// get view
				String viewPart = sections[3].substring(dashPos+1);
				if (!viewPart.endsWith(".elib") && !viewPart.endsWith(".jelib"))
				{
					System.out.println("Missing '.elib' after view name in project file: " + userLine);
					return null;
				}
				if (viewPart.endsWith(".elib")) viewPart = viewPart.substring(0, viewPart.length()-5); else
					if (viewPart.endsWith(".jelib")) viewPart = viewPart.substring(0, viewPart.length()-6);
				pf.cellview = View.findView(viewPart);
		
				// get owner
				pf.owner = (sections[4].length() > 0 ? sections[4] : null);
		
				// get last owner
				pf.lastowner = (sections[5].length() > 0 ? sections[5] : null);
		
				// get comments
				pf.comment = sections[6];
		
				// check for duplication
				for(Iterator it = pl.allCells.iterator(); it.hasNext(); )
				{
					ProjectCell opf = (ProjectCell)it.next();
					if (!opf.cellname.equalsIgnoreCase(pf.cellname)) continue;
					if (opf.cellview != pf.cellview) continue;
					System.out.println("Error in project file: view '" + pf.cellview.getFullName() + "' of cell '" +
						pf.cellname + "' exists twice (versions " + pf.cellversion + " and " + opf.cellversion + ")");
				}

				CellName cn = CellName.newName(pf.cellname, pf.cellview, pf.cellversion);
				Cell cell = lib.findNodeProto(cn.getName());
				if (cell != null) pl.byCell.put(cell, pf);
		
				// link it in
				pl.allCells.add(pf);
			}

			lnr.close();
		} catch (IOException e)
		{
			System.out.println("Error reading project file");
		}
		return pl;
	}

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
	public static void setRepositoryLocation(String r) { cacheRepositoryLocation.setString(r); }

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
//		if (namesamen(pp, x_("list-cells"), l) == 0)
//		{
//			proj_showlistdialog(el_curlib);
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
//	void proj_checkin(CHAR *cellname)
//	{
//		REGISTER NODEPROTO *np, *onp;
//		REGISTER NODEINST *ni;
//		REGISTER INTBIG total;
//		REGISTER BOOLEAN propagated;
//		REGISTER LIBRARY *olib;
//		REGISTER PROJECTCELL *pf;
//		REGISTER void *infstr;
//	
//		// find out which cell is being checked in
//		np = getnodeproto(cellname);
//		if (np == NONODEPROTO)
//		{
//			ttyputerr(_("Cannot identify cell '%s'"), cellname);
//			return;
//		}
//	
//		// mark the cell to be checked-in
//		for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//			for(onp = olib.firstnodeproto; onp != NONODEPROTO; onp = onp.nextnodeproto)
//				onp.temp1 = 0;
//		np.temp1 = 1;
//	
//		// look for cells above this one that must also be checked in
//		for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//			for(onp = olib.firstnodeproto; onp != NONODEPROTO; onp = onp.nextnodeproto)
//				onp.temp2 = 0;
//		np.temp2 = 1;
//		propagated = TRUE;
//		while (propagated)
//		{
//			propagated = FALSE;
//			for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//				for(onp = olib.firstnodeproto; onp != NONODEPROTO; onp = onp.nextnodeproto)
//			{
//				if (onp.temp2 == 1)
//				{
//					propagated = TRUE;
//					onp.temp2 = 2;
//					for(ni = onp.firstinst; ni != NONODEINST; ni = ni.nextinst)
//					{
//						if (ni.parent.temp2 == 0) ni.parent.temp2 = 1;
//					}
//				}
//			}
//		}
//		np.temp2 = 0;
//		total = 0;
//		for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//			for(onp = olib.firstnodeproto; onp != NONODEPROTO; onp = onp.nextnodeproto)
//		{
//			if (onp.temp2 == 0) continue;
//			pf = proj_findcell(onp);
//			if (pf == NOPROJECTCELL) continue;
//			if (namesame(pf.owner, proj_username) == 0)
//			{
//				onp.temp1 = 1;
//				total++;
//			}
//		}
//	
//		// look for cells below this one that must also be checked in
//		for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//			for(onp = olib.firstnodeproto; onp != NONODEPROTO; onp = onp.nextnodeproto)
//				onp.temp2 = 0;
//		np.temp2 = 1;
//		propagated = TRUE;
//		while (propagated)
//		{
//			propagated = FALSE;
//			for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//				for(onp = olib.firstnodeproto; onp != NONODEPROTO; onp = onp.nextnodeproto)
//			{
//				if (onp.temp2 == 1)
//				{
//					propagated = TRUE;
//					onp.temp2 = 2;
//					for(ni = onp.firstnodeinst; ni != NONODEINST; ni = ni.nextnodeinst)
//					{
//						if (ni.proto.primindex != 0) continue;
//						if (ni.proto.temp2 == 0) ni.proto.temp2 = 1;
//					}
//				}
//			}
//		}
//		np.temp2 = 0;
//		for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//			for(onp = olib.firstnodeproto; onp != NONODEPROTO; onp = onp.nextnodeproto)
//		{
//			if (onp.temp2 == 0) continue;
//			pf = proj_findcell(onp);
//			if (pf == NOPROJECTCELL) continue;
//			if (namesame(pf.owner, proj_username) == 0)
//			{
//				onp.temp1 = 1;
//				total++;
//			}
//		}
//	
//		// advise of additional cells that must be checked-in
//		if (total > 0)
//		{
//			total = 0;
//			infstr = initinfstr();
//			for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//				for(onp = olib.firstnodeproto; onp != NONODEPROTO; onp = onp.nextnodeproto)
//			{
//				if (onp == np || onp.temp1 == 0) continue;
//				if (total > 0) addstringtoinfstr(infstr, x_(", "));
//				addstringtoinfstr(infstr, describenodeproto(onp));
//				total++;
//			}
//			ttyputmsg(_("Also checking in related cell(s): %s"), returninfstr(infstr));
//		}
//	
//		// check it in
//		proj_checkinmany(np.lib);
//	}
//	
//	void proj_checkout(CHAR *cellname, BOOLEAN showcell)
//	{
//		REGISTER NODEPROTO *np, *newvers, *oldvers;
//		REGISTER NODEINST *ni;
//		REGISTER BOOLEAN worked, propagated;
//		REGISTER INTBIG total;
//		REGISTER LIBRARY *lib, *olib;
//		CHAR projectpath[256], projectfile[256], *argv[3];
//		PROJECTCELL *pf;
//	
//		// find out which cell is being checked out
//		oldvers = getnodeproto(cellname);
//		if (oldvers == NONODEPROTO)
//		{
//			ttyputerr(_("Cannot identify cell '%s'"), cellname);
//			return;
//		}
//		lib = oldvers.lib;
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
//		worked = FALSE;
//		if (proj_readprojectfile(projectpath, projectfile))
//			ttyputerr(_("Cannot read project file")); else
//		{
//			// find this in the project file
//			pf = proj_findcell(oldvers);
//			if (pf == NOPROJECTCELL) ttyputerr(_("This cell is not in the project")); else
//			{
//				// see if it is available
//				if (*pf.owner != 0)
//				{
//					if (namesame(pf.owner, proj_username) == 0)
//					{
//						ttyputerr(_("This cell is already checked out to you"));
//						proj_marklocked(oldvers, FALSE);
//					} else
//					{
//						ttyputerr(_("This cell is already checked out to '%s'"), pf.owner);
//					}
//				} else
//				{
//					// make sure we have the latest version
//					if (pf.cellversion > oldvers.version)
//					{
//						ttyputerr(_("Cannot check out %s because you don't have the latest version (yours is %ld, project has %ld)"),
//							describenodeproto(oldvers), oldvers.version, pf.cellversion);
//						ttyputmsg(_("Do an 'update' first"));
//					} else
//					{
//						if (!proj_getcomments(pf, x_("out")))
//						{
//							if (proj_startwritingprojectfile(projectpath, projectfile))
//								ttyputerr(_("Cannot write project file")); else
//							{
//								// prevent tools (including this one) from seeing the change
//								(void)proj_turnofftools();
//	
//								// remove highlighting
//								us_clearhighlightcount();
//	
//								// make new version
//								newvers = copynodeproto(oldvers, lib, oldvers.protoname, TRUE);
//	
//								// restore tool state
//								proj_restoretoolstate();
//	
//								if (newvers == NONODEPROTO)
//									ttyputerr(_("Error making new version of cell")); else
//								{
//									(*el_curconstraint.solve)(newvers);
//	
//									// replace former usage with new version
//									if (proj_usenewestversion(oldvers, newvers))
//										ttyputerr(_("Error replacing instances of new %s"),
//											describenodeproto(oldvers)); else
//									{
//										// update record for the cell
//										(void)reallocstring(&pf.owner, proj_username, proj_tool.cluster);
//										(void)reallocstring(&pf.lastowner, x_(""), proj_tool.cluster);
//										proj_marklocked(newvers, FALSE);
//										worked = TRUE;
//									}
//								}
//							}
//							proj_endwritingprojectfile();
//						}
//					}
//				}
//			}
//		}
//	
//		// relase project file lock
//		proj_unlockprojfile(projectpath, projectfile);
//	
//		// if it worked, print dependencies and display
//		if (worked)
//		{
//			ttyputmsg(_("Cell %s checked out for your use"), describenodeproto(newvers));
//	
//			// advise of possible problems with other checkouts higher up in the hierarchy
//			for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//				for(np = olib.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//					np.temp1 = 0;
//			newvers.temp1 = 1;
//			propagated = TRUE;
//			while (propagated)
//			{
//				propagated = FALSE;
//				for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//					for(np = olib.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//				{
//					if (np.temp1 == 1)
//					{
//						propagated = TRUE;
//						np.temp1 = 2;
//						for(ni = np.firstinst; ni != NONODEINST; ni = ni.nextinst)
//							if (ni.parent.temp1 == 0) ni.parent.temp1 = 1;
//					}
//				}
//			}
//			newvers.temp1 = 0;
//			total = 0;
//			for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//				for(np = olib.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//			{
//				if (np.temp1 == 0) continue;
//				pf = proj_findcell(np);
//				if (pf == NOPROJECTCELL) continue;
//				if (*pf.owner != 0 && namesame(pf.owner, proj_username) != 0)
//				{
//					np.temp1 = 3;
//					np.temp2 = (INTBIG)pf;
//					total++;
//				}
//			}
//			if (total != 0)
//			{
//				ttyputmsg(_("*** Warning: the following cells are above this in the hierarchy"));
//				ttyputmsg(_("*** and are checked out to others.  This may cause problems"));
//				for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//					for(np = olib.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//				{
//					if (np.temp1 != 3) continue;
//					pf = (PROJECTCELL *)np.temp2;
//					ttyputmsg(_("    %s is checked out to %s"), describenodeproto(np), pf.owner);
//				}
//			}
//	
//			// advise of possible problems with other checkouts lower down in the hierarchy
//			for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//				for(np = olib.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//					np.temp1 = 0;
//			newvers.temp1 = 1;
//			propagated = TRUE;
//			while(propagated)
//			{
//				propagated = FALSE;
//				for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//					for(np = olib.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//				{
//					if (np.temp1 == 1)
//					{
//						propagated = TRUE;
//						np.temp1 = 2;
//						for(ni = np.firstnodeinst; ni != NONODEINST; ni = ni.nextnodeinst)
//						{
//							if (ni.proto.primindex != 0) continue;
//							if (ni.proto.temp1 == 0) ni.proto.temp1 = 1;
//						}
//					}
//				}
//			}
//			newvers.temp1 = 0;
//			total = 0;
//			for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//				for(np = olib.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//			{
//				if (np.temp1 == 0) continue;
//				pf = proj_findcell(np);
//				if (pf == NOPROJECTCELL) continue;
//				if (*pf.owner != 0 && namesame(pf.owner, proj_username) != 0)
//				{
//					np.temp1 = 3;
//					np.temp2 = (INTBIG)pf;
//					total++;
//				}
//			}
//			if (total != 0)
//			{
//				ttyputmsg(_("*** Warning: the following cells are below this in the hierarchy"));
//				ttyputmsg(_("*** and are checked out to others.  This may cause problems"));
//				for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//					for(np = olib.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//				{
//					if (np.temp1 != 3) continue;
//					pf = (PROJECTCELL *)np.temp2;
//					ttyputmsg(_("    %s is checked out to %s"), describenodeproto(np), pf.owner);
//				}
//			}
//	
//			// display the checked-out cell
//			if (showcell)
//			{
//				argv[0] = describenodeproto(newvers);
//				us_editcell(1, argv);
//				us_endchanges(NOWINDOWPART);
//			}
//		}
//	}
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
//		if (proj_turnofftools())
//		{
//			ttyputerr(_("Could not save tool state"));
//			return;
//		}
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
//		proj_restoretoolstate();
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
//	
//	/************************ PROJECT DATABASE ***********************/
//	
//	void proj_checkinmany(LIBRARY *lib)
//	{
//		REGISTER NODEPROTO *np;
//		CHAR projectpath[256], projectfile[256];
//		PROJECTCELL *pf;
//		REGISTER LIBRARY *olib;
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
//			for(olib = el_curlib; olib != NOLIBRARY; olib = olib.nextlibrary)
//				for(np = olib.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//			{
//				if (np.temp1 == 0) continue;
//				if (stopping(STOPREASONCHECKIN)) break;
//	
//				// find this in the project file
//				pf = proj_findcell(np);
//				if (pf == NOPROJECTCELL)
//					ttyputerr(_("Cell %s is not in the project"), describenodeproto(np)); else
//				{
//					// see if it is available
//					if (estrcmp(pf.owner, proj_username) != 0)
//						ttyputerr(_("Cell %s is not checked out to you"), describenodeproto(np)); else
//					{
//						if (!proj_getcomments(pf, x_("in")))
//						{
//							// prepare to write it back
//							if (proj_startwritingprojectfile(projectpath, projectfile))
//								ttyputerr(_("Cannot write project file")); else
//							{
//								// write the cell out there
//								if (proj_writecell(np))
//									ttyputerr(_("Error writing cell %s"), describenodeproto(np)); else
//								{
//									(void)reallocstring(&pf.owner, x_(""), proj_tool.cluster);
//									(void)reallocstring(&pf.lastowner, proj_username, proj_tool.cluster);
//									pf.cellversion = np.version;
//									proj_marklocked(np, TRUE);
//									ttyputmsg(_("Cell %s checked in"), describenodeproto(np));
//								}
//								proj_endwritingprojectfile();
//							}
//						}
//					}
//				}
//			}
//		}
//	
//		// relase project file lock
//		proj_unlockprojfile(projectpath, projectfile);
//	}
	
	/*
	 * Routine to obtain information about the project associated with library "lib".
	 * The path to the project is placed in "path" and the name of the project file
	 * in that directory is placed in "projfile".  Returns true on error.
	 */
	static String [] proj_getprojinfo(Library lib) // CHAR *path, CHAR *projfile)
	{
		// see if there is a variable in the current library with the project path
		String path = null;
		Variable var = lib.getVar(proj_pathkey);
		if (var != null)
		{
			path = (String)var.getObject();
			// make sure the path is valid
//			io = xopen(path, proj_filetypeproj, x_(""), &truename);
//			if (io != 0) xclose(io); else
//				var = NOVARIABLE;
		}
		if (var == null)
		{
			// prompt for project file
//			infstr = initinfstr();
//			addstringtoinfstr(infstr, x_("proj/"));
//			addstringtoinfstr(infstr, _("Project File"));
//			i = ttygetparam(returninfstr(infstr), &us_colorreadp, 1, params);
//			if (i == 0) return(TRUE);
//			estrcpy(path, params[0]);
//			setvalkey((INTBIG)lib, VLIBRARY, proj_pathkey, (INTBIG)path, VSTRING);
		}
	
		int sepPos = path.lastIndexOf(File.separatorChar);
		String projFile;
		if (sepPos >= 0)
		{
			projFile = path.substring(sepPos+1);
			path = path.substring(0, sepPos);
		} else
		{
			projFile = path;
			path = "";
		}
		String [] ret = new String[2];
		ret[0] = projFile;
		ret[1] = path;
		return ret;
	}
	
//	/* Project List */
//	static DIALOGITEM proj_listdialogitems[] =
//	{
//	 /*  1 */ {0, {204,312,228,376}, BUTTON, N_("Done")},
//	 /*  2 */ {0, {4,4,196,376}, SCROLL, x_("")},
//	 /*  3 */ {0, {260,4,324,376}, MESSAGE, x_("")},
//	 /*  4 */ {0, {240,4,256,86}, MESSAGE, N_("Comments:")},
//	 /*  5 */ {0, {204,120,228,220}, BUTTON, N_("Check It Out")},
//	 /*  6 */ {0, {236,4,237,376}, DIVIDELINE, x_("")},
//	 /*  7 */ {0, {204,8,228,108}, BUTTON, N_("Check It In")},
//	 /*  8 */ {0, {204,232,228,296}, BUTTON, N_("Update")}
//	};
//	static DIALOG proj_listdialog = {{50,75,383,462}, N_("Project Management"), 0, 8, proj_listdialogitems, 0, 0};
//	
//	/* special items for the "Project list" dialog: */
//	#define DPRL_PROJLIST  2		/* Project list (scroll) */
//	#define DPRL_COMMENTS  3		/* Comments (stat text) */
//	#define DPRL_CHECKOUT  5		/* Check out (button) */
//	#define DPRL_CHECKIN   7		/* Check in (button) */
//	#define DPRL_UPDATE    8		/* Update (button) */
//	
//	void proj_showlistdialog(LIBRARY *lib)
//	{
//		REGISTER INTBIG itemHit, i, j;
//		REGISTER PROJECTCELL *pf;
//		REGISTER void *infstr, *dia;
//	
//		if (proj_getusername(lib))
//		{
//			ttyputerr(_("No valid user"));
//			return;
//		}
//		proj_active = TRUE;
//	
//		// show the dialog
//		dia = DiaInitDialog(&proj_listdialog);
//		if (dia == 0) return;
//		DiaInitTextDialog(dia, DPRL_PROJLIST, DiaNullDlogList, DiaNullDlogItem, DiaNullDlogDone,
//			-1, SCSELMOUSE | SCREPORT);
//	
//		// load project information into the scroll area
//		if (proj_loadlist(lib, dia))
//		{
//			DiaDoneDialog(dia);
//			return;
//		}
//	
//		for(;;)
//		{
//			itemHit = DiaNextHit(dia);
//			if (itemHit == OK) break;
//			if (itemHit == DPRL_UPDATE)
//			{
//				// update project
//				proj_update(lib);
//				(void)proj_loadlist(lib, dia);
//				us_endbatch();
//				continue;
//			}
//			if (itemHit == DPRL_CHECKOUT || itemHit == DPRL_CHECKIN ||
//				itemHit == DPRL_PROJLIST)
//			{
//				// figure out which cell is selected
//				i = DiaGetCurLine(dia, DPRL_PROJLIST);
//				if (i < 0) continue;
//				for(j=0, pf = proj_firstprojectcell; pf != NOPROJECTCELL; pf = pf.nextprojectcell, j++)
//					if (i == j) break;
//				if (pf == NOPROJECTCELL) continue;
//	
//				if (itemHit == DPRL_CHECKOUT)
//				{
//					// check it out
//					infstr = initinfstr();
//					addstringtoinfstr(infstr, pf.cellname);
//					if (pf.cellview != el_unknownview)
//					{
//						addtoinfstr(infstr, '{');
//						addstringtoinfstr(infstr, pf.cellview.sviewname);
//						addtoinfstr(infstr, '}');
//					}
//					proj_checkout(returninfstr(infstr), FALSE);
//					(void)proj_loadlist(lib, dia);
//					us_endbatch();
//					continue;
//				}
//				if (itemHit == DPRL_CHECKIN)
//				{
//					// check it in
//					infstr = initinfstr();
//					addstringtoinfstr(infstr, pf.cellname);
//					if (pf.cellview != el_unknownview)
//					{
//						addtoinfstr(infstr, '{');
//						addstringtoinfstr(infstr, pf.cellview.sviewname);
//						addtoinfstr(infstr, '}');
//					}
//					proj_checkin(returninfstr(infstr));
//					(void)proj_loadlist(lib, dia);
//					continue;
//				}
//				if (itemHit == DPRL_PROJLIST)
//				{
//					if (*pf.comment != 0) DiaSetText(dia, DPRL_COMMENTS, pf.comment);
//					if (*pf.owner == 0) DiaUnDimItem(dia, DPRL_CHECKOUT); else
//						DiaDimItem(dia, DPRL_CHECKOUT);
//					if (estrcmp(pf.owner, proj_username) == 0) DiaUnDimItem(dia, DPRL_CHECKIN); else
//						DiaDimItem(dia, DPRL_CHECKIN);
//					continue;
//				}
//			}
//		}
//		DiaDoneDialog(dia);
//	}
//	
//	/*
//	 * Routine to display the current project information in the list dialog.
//	 * Returns true on error.
//	 */
//	BOOLEAN proj_loadlist(LIBRARY *lib, void *dia)
//	{
//		REGISTER BOOLEAN failed, uptodate;
//		REGISTER INTBIG whichline, thisline;
//		REGISTER PROJECTCELL *pf, *curpf;
//		CHAR line[256], projectpath[256], projectfile[256];
//		REGISTER NODEPROTO *np, *curcell;
//	
//		// get location of project file
//		if (proj_getprojinfo(lib, projectpath, projectfile))
//		{
//			ttyputerr(_("Cannot find project file"));
//			return(TRUE);
//		}
//	
//		// lock the project file
//		if (proj_lockprojfile(projectpath, projectfile))
//		{
//			ttyputerr(_("Couldn't lock project file"));
//			return(TRUE);
//		}
//	
//		// read the project file
//		failed = FALSE;
//		if (proj_readprojectfile(projectpath, projectfile))
//		{
//			ttyputerr(_("Cannot read project file"));
//			failed = TRUE;
//		}
//	
//		// relase project file lock
//		proj_unlockprojfile(projectpath, projectfile);
//		if (failed) return(TRUE);
//	
//		// find current cell
//		curcell = getcurcell();
//	
//		// show what is in the project file
//		DiaLoadTextDialog(dia, DPRL_PROJLIST, DiaNullDlogList, DiaNullDlogItem, DiaNullDlogDone, -1);
//		whichline = -1;
//		thisline = 0;
//		uptodate = TRUE;
//		for(pf = proj_firstprojectcell; pf != NOPROJECTCELL; pf = pf.nextprojectcell)
//		{
//			// see if project is up-to-date
//			np = db_findnodeprotoname(pf.cellname, pf.cellview, el_curlib);
//			if (np == NONODEPROTO || np.version < pf.cellversion) uptodate = FALSE;
//	
//			// remember this line if it describes the current cell
//			if (curcell != NONODEPROTO && namesame(curcell.protoname, pf.cellname) == 0 &&
//				curcell.cellview == pf.cellview)
//			{
//				whichline = thisline;
//				curpf = pf;
//			}
//	
//			// describe this project cell
//			if (pf.cellview == el_unknownview)
//			{
//				esnprintf(line, 256, x_("%s;%ld"), pf.cellname, pf.cellversion);
//			} else
//			{
//				esnprintf(line, 256, x_("%s{%s};%ld"), pf.cellname, pf.cellview.sviewname,
//					pf.cellversion);
//			}
//			if (*pf.owner == 0)
//			{
//				estrcat(line, _(" AVAILABLE"));
//				if (*pf.lastowner != 0)
//				{
//					estrcat(line, _(", last mod by "));
//					estrcat(line, pf.lastowner);
//				}
//			} else
//			{
//				if (estrcmp(pf.owner, proj_username) == 0)
//				{
//					estrcat(line, _(" EDITABLE, checked out to you"));
//				} else
//				{
//					estrcat(line, _(" UNAVAILABLE, checked out to "));
//					estrcat(line, pf.owner);
//				}
//			}
//			DiaStuffLine(dia, DPRL_PROJLIST, line);
//			thisline++;
//		}
//		DiaSelectLine(dia, DPRL_PROJLIST, whichline);
//	
//		DiaDimItem(dia, DPRL_CHECKOUT);
//		DiaDimItem(dia, DPRL_CHECKIN);
//		if (whichline >= 0)
//		{
//			if (*curpf.comment != 0) DiaSetText(dia, DPRL_COMMENTS, curpf.comment);
//			if (*curpf.owner == 0) DiaUnDimItem(dia, DPRL_CHECKOUT); else
//				DiaDimItem(dia, DPRL_CHECKOUT);
//			if (estrcmp(curpf.owner, proj_username) == 0) DiaUnDimItem(dia, DPRL_CHECKIN); else
//				DiaDimItem(dia, DPRL_CHECKIN);
//		}
//	
//		if (uptodate) DiaDimItem(dia, DPRL_UPDATE); else
//		{
//			ttyputmsg(_("Your library does not contain the most recent additions to the project."));
//			ttyputmsg(_("You should do an 'Update' to make it current."));
//			DiaUnDimItem(dia, DPRL_UPDATE);
//		}
//		return(FALSE);
//	}
//	
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
//	/************************ LOCKING ***********************/
//	
//	#define MAXTRIES 10
//	#define NAPTIME  5
//	
//	BOOLEAN proj_lockprojfile(CHAR *projectpath, CHAR *projectfile)
//	{
//		CHAR lockfilename[256];
//		REGISTER INTBIG i, j;
//	
//		esnprintf(lockfilename, 256, x_("%s%sLOCK"), projectpath, projectfile);
//		for(i=0; i<MAXTRIES; i++)
//		{
//			if (lockfile(lockfilename)) return(FALSE);
//			if (i == 0) ttyputmsg(_("Project file locked.  Waiting...")); else
//				ttyputmsg(_("Still waiting (will try %d more times)..."), MAXTRIES-i);
//			for(j=0; j<NAPTIME; j++)
//			{
//				gotosleep(60);
//				if (stopping(STOPREASONLOCK)) return(TRUE);
//			}
//		}
//		return(TRUE);
//	}
//	
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
//	
//	void proj_marklocked(NODEPROTO *np, BOOLEAN locked)
//	{
//		REGISTER NODEPROTO *onp;
//	
//		if (!locked)
//		{
//			FOR_CELLGROUP(onp, np)
//			{
//				if (onp.cellview != np.cellview) continue;
//				if (getvalkey((INTBIG)onp, VNODEPROTO, VINTEGER, proj_lockedkey) != NOVARIABLE)
//					(void)delvalkey((INTBIG)onp, VNODEPROTO, proj_lockedkey);
//			}
//		} else
//		{
//			FOR_CELLGROUP(onp, np)
//			{
//				if (onp.cellview != np.cellview) continue;
//				if (onp.newestversion == onp)
//				{
//					if (getvalkey((INTBIG)onp, VNODEPROTO, VINTEGER, proj_lockedkey) == NOVARIABLE)
//						setvalkey((INTBIG)onp, VNODEPROTO, proj_lockedkey, 1, VINTEGER);
//				} else
//				{
//					if (getvalkey((INTBIG)onp, VNODEPROTO, VINTEGER, proj_lockedkey) != NOVARIABLE)
//						(void)delvalkey((INTBIG)onp, VNODEPROTO, proj_lockedkey);
//				}
//			}
//		}
//	}
//
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
//		(void)proj_turnofftools();
//	
//		templibname = proj_templibraryname();
//		flib = newlibrary(templibname, celllibpath);
//		if (flib == NOLIBRARY)
//		{
//			ttyputerr(_("Cannot create library %s"), celllibpath);
//			proj_restoretoolstate();
//			return(NONODEPROTO);
//		}
//		oldverbose = asktool(io_tool, x_("verbose"), 0);
//		ret = asktool(io_tool, x_("read"), (INTBIG)flib, (INTBIG)x_("binary"));
//		(void)asktool(io_tool, x_("verbose"), oldverbose);
//		if (ret != 0)
//		{
//			ttyputerr(_("Cannot read library %s"), celllibpath);
//			killlibrary(flib);
//			proj_restoretoolstate();
//			return(NONODEPROTO);
//		}
//		if (flib.curnodeproto == NONODEPROTO)
//		{
//			ttyputerr(_("Cannot find cell in library %s"), celllibpath);
//			killlibrary(flib);
//			proj_restoretoolstate();
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
//			proj_restoretoolstate();
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
//		proj_restoretoolstate();
//	
//		// return the new cell
//		return(newnp);
//	}
//	
//	BOOLEAN proj_usenewestversion(NODEPROTO *oldnp, NODEPROTO *newnp)
//	{
//		INTBIG lx, hx, ly, hy;
//		REGISTER WINDOWPART *w;
//		REGISTER LIBRARY *lib;
//		REGISTER NODEINST *ni, *newni, *nextni;
//	
//		// prevent tools (including this one) from seeing the change
//		(void)proj_turnofftools();
//	
//		// replace them all
//		for(ni = oldnp.firstinst; ni != NONODEINST; ni = nextni)
//		{
//			nextni = ni.nextinst;
//			newni = replacenodeinst(ni, newnp, FALSE, FALSE);
//			if (newni == NONODEINST)
//			{
//				ttyputerr(_("Failed to update instance of %s in %s"), describenodeproto(newnp),
//					describenodeproto(ni.parent));
//				proj_restoretoolstate();
//				return(TRUE);
//			}
//		}
//	
//		// redraw windows that updated
//		for(w = el_topwindowpart; w != NOWINDOWPART; w = w.nextwindowpart)
//		{
//			if (w.curnodeproto == NONODEPROTO) continue;
//			if (w.curnodeproto != newnp) continue;
//			w.curnodeproto = newnp;
//	
//			// redisplay the window with the new cell
//			us_fullview(newnp, &lx, &hx, &ly, &hy);
//			us_squarescreen(w, NOWINDOWPART, FALSE, &lx, &hx, &ly, &hy, 0);
//			startobjectchange((INTBIG)w, VWINDOWPART);
//			(void)setval((INTBIG)w, VWINDOWPART, x_("screenlx"), lx, VINTEGER);
//			(void)setval((INTBIG)w, VWINDOWPART, x_("screenhx"), hx, VINTEGER);
//			(void)setval((INTBIG)w, VWINDOWPART, x_("screenly"), ly, VINTEGER);
//			(void)setval((INTBIG)w, VWINDOWPART, x_("screenhy"), hy, VINTEGER);
//			us_gridset(w, w.state);
//			endobjectchange((INTBIG)w, VWINDOWPART);
//		}
//	
//		// update status display if necessary
//		if (us_curnodeproto == oldnp) us_setnodeproto(newnp);
//	
//		// replace library references
//		for(lib = el_curlib; lib != NOLIBRARY; lib = lib.nextlibrary)
//			if (lib.curnodeproto == oldnp)
//				(void)setval((INTBIG)lib, VLIBRARY, x_("curnodeproto"), (INTBIG)newnp, VNODEPROTO);
//	
//		if (killnodeproto(oldnp))
//			ttyputerr(_("Could not delete old version"));
//	
//		// restore tool state
//		proj_restoretoolstate();
//		return(FALSE);
//	}
//	
//	BOOLEAN proj_writecell(NODEPROTO *np)
//	{
//		REGISTER LIBRARY *flib;
//		REGISTER INTBIG retval;
//		CHAR libname[256], libfile[256], projname[256], *templibname;
//		REGISTER NODEPROTO *npcopy;
//		INTBIG filestatus;
//	
//		if (proj_getprojinfo(np.lib, libfile, projname))
//		{
//			ttyputerr(_("Cannot find project info on library %s"), np.lib.libname);
//			return(TRUE);
//		}
//		projname[estrlen(projname)-5] = 0;
//		estrcat(libfile, projname);
//		estrcat(libfile, DIRSEPSTR);
//		estrcat(libfile, np.protoname);
//	
//		// make the directory if necessary
//		filestatus = fileexistence(libfile);
//		if (filestatus == 1 || filestatus == 3)
//		{
//			ttyputerr(_("Could not create cell directory '%s'"), libfile);
//			return(TRUE);
//		}
//		if (filestatus == 0)
//		{
//			if (createdirectory(libfile))
//			{
//				ttyputerr(_("Could not create cell directory '%s'"), libfile);
//				return(TRUE);
//			}
//		}
//	
//		estrcat(libfile, DIRSEPSTR);
//		esnprintf(libname, 256, x_("%ld-%s.elib"), np.version, np.cellview.viewname);
//		estrcat(libfile, libname);
//	
//		// prevent tools (including this one) from seeing the change
//		(void)proj_turnofftools();
//	
//		templibname = proj_templibraryname();
//		flib = newlibrary(templibname, libfile);
//		if (flib == NOLIBRARY)
//		{
//			ttyputerr(_("Cannot create library %s"), libfile);
//			proj_restoretoolstate();
//			return(TRUE);
//		}
//		npcopy = copyrecursively(np, flib);
//		if (npcopy == NONODEPROTO)
//		{
//			ttyputerr(_("Could not place %s in a library"), describenodeproto(np));
//			killlibrary(flib);
//			proj_restoretoolstate();
//			return(TRUE);
//		}
//	
//		flib.curnodeproto = npcopy;
//		flib.userbits |= READFROMDISK;
//		makeoptionstemporary(flib);
//		retval = asktool(io_tool, x_("write"), (INTBIG)flib, (INTBIG)x_("binary"));
//		restoreoptionstate(flib);
//		if (retval != 0)
//		{
//			ttyputerr(_("Could not save library with %s in it"), describenodeproto(np));
//			killlibrary(flib);
//			proj_restoretoolstate();
//			return(TRUE);
//		}
//		killlibrary(flib);
//	
//		// restore tool state
//		proj_restoretoolstate();
//	
//		return(FALSE);
//	}
//	
//	CHAR *proj_templibraryname(void)
//	{
//		static CHAR libname[256];
//		REGISTER LIBRARY *lib;
//		REGISTER INTBIG i;
//	
//		for(i=1; ; i++)
//		{
//			esnprintf(libname, 256, x_("projecttemp%ld"), i);
//			for(lib = el_curlib; lib != NOLIBRARY; lib = lib.nextlibrary)
//				if (namesame(libname, lib.libname) == 0) break;
//			if (lib == NOLIBRARY) break;
//		}
//		return(libname);
//	}
//	
//	/*
//	 * Routine to save the state of all tools and turn them off.
//	 */
//	BOOLEAN proj_turnofftools(void)
//	{
//		REGISTER INTBIG i;
//		REGISTER TOOL *tool;
//	
//		// turn off all tools for this operation
//		if (proj_savetoolstate == 0)
//		{
//			proj_savetoolstate = (INTBIG *)emalloc(el_maxtools * SIZEOFINTBIG, el_tempcluster);
//			if (proj_savetoolstate == 0) return(TRUE);
//		}
//		for(i=0; i<el_maxtools; i++)
//		{
//			tool = &el_tools[i];
//			proj_savetoolstate[i] = tool.toolstate;
//			if (tool == us_tool || tool == proj_tool || tool == net_tool) continue;
//			tool.toolstate &= ~TOOLON;
//		}
//		proj_ignorechanges = TRUE;
//		return(FALSE);
//	}
//	
//	/*
//	 * Routine to restore the state of all tools that were reset by "proj_turnofftools()".
//	 */
//	void proj_restoretoolstate(void)
//	{
//		REGISTER INTBIG i;
//	
//		if (proj_savetoolstate == 0) return;
//		for(i=0; i<el_maxtools; i++)
//			el_tools[i].toolstate = proj_savetoolstate[i];
//		proj_ignorechanges = FALSE;
//	}
//	
//	/************************ DATABASE OPERATIONS ***********************/
//	
//	NODEPROTO *copyrecursively(NODEPROTO *fromnp, LIBRARY *tolib)
//	{
//		REGISTER NODEPROTO *np, *onp, *newfromnp;
//		REGISTER NODEINST *ni;
//		REGISTER CHAR *newname;
//		CHAR versnum[20];
//		REGISTER void *infstr;
//	
//		// must copy subcells
//		for(ni = fromnp.firstnodeinst; ni != NONODEINST; ni = ni.nextnodeinst)
//		{
//			np = ni.proto;
//			if (np.primindex != 0) continue;
//	
//			// see if there is already a cell with this name and view
//			for(onp = tolib.firstnodeproto; onp != NONODEPROTO; onp = onp.nextnodeproto)
//				if (namesame(onp.protoname, np.protoname) == 0 &&
//					onp.cellview == np.cellview) break;
//			if (onp != NONODEPROTO) continue;
//	
//			onp = copyskeleton(np, tolib);
//			if (onp == NONODEPROTO)
//			{
//				ttyputerr(_("Copy of subcell %s failed"), describenodeproto(np));
//				return(NONODEPROTO);
//			}
//		}
//	
//		// copy the cell if it is not already done
//		for(newfromnp = tolib.firstnodeproto; newfromnp != NONODEPROTO; newfromnp = newfromnp.nextnodeproto)
//			if (namesame(newfromnp.protoname, fromnp.protoname) == 0 &&
//				newfromnp.cellview == fromnp.cellview && newfromnp.version == fromnp.version) break;
//		if (newfromnp == NONODEPROTO)
//		{
//			infstr = initinfstr();
//			addstringtoinfstr(infstr, fromnp.protoname);
//			addtoinfstr(infstr, ';');
//			esnprintf(versnum, 20, x_("%ld"), fromnp.version);
//			addstringtoinfstr(infstr, versnum);
//			if (fromnp.cellview != el_unknownview)
//			{
//				addtoinfstr(infstr, '{');
//				addstringtoinfstr(infstr, fromnp.cellview.sviewname);
//				addtoinfstr(infstr, '}');
//			}
//			newname = returninfstr(infstr);
//			newfromnp = copynodeproto(fromnp, tolib, newname, TRUE);
//			if (newfromnp == NONODEPROTO) return(NONODEPROTO);
//	
//			// ensure that the copied cell is the right size
//			(*el_curconstraint.solve)(newfromnp);
//		}
//	
//		return(newfromnp);
//	}
//	
//	NODEPROTO *copyskeleton(NODEPROTO *fromnp, LIBRARY *tolib)
//	{
//		CHAR *newname;
//		REGISTER INTBIG newang, newtran;
//		REGISTER INTBIG i, xc, yc;
//		INTBIG newx, newy;
//		XARRAY trans, localtrans, ntrans;
//		REGISTER NODEPROTO *np;
//		REGISTER PORTPROTO *pp, *rpp;
//		REGISTER NODEINST *ni, *newni;
//		REGISTER void *infstr;
//	
//		// cannot skeletonize text-only views
//		if ((fromnp.cellview.viewstate&TEXTVIEW) != 0) return(NONODEPROTO);
//	
//		infstr = initinfstr();
//		addstringtoinfstr(infstr, fromnp.protoname);
//		if (fromnp.cellview != el_unknownview)
//		{
//			addtoinfstr(infstr, '{');
//			addstringtoinfstr(infstr, fromnp.cellview.sviewname);
//			addtoinfstr(infstr, '}');
//		}
//		newname = returninfstr(infstr);
//		np = newnodeproto(newname, tolib);
//		if (np == NONODEPROTO) return(NONODEPROTO);
//	
//		// place all exports in the new cell
//		for(pp = fromnp.firstportproto; pp != NOPORTPROTO; pp = pp.nextportproto)
//		{
//			// make a transformation matrix for the node that has exports
//			ni = pp.subnodeinst;
//			rpp = pp.subportproto;
//			newang = ni.rotation;
//			newtran = ni.transpose;
//			makerot(ni, trans);
//			while (ni.proto.primindex == 0)
//			{
//				maketrans(ni, localtrans);
//				transmult(localtrans, trans, ntrans);
//				ni = rpp.subnodeinst;
//				rpp = rpp.subportproto;
//				if (ni.transpose == 0) newang = ni.rotation + newang; else
//					newang = ni.rotation + 3600 - newang;
//				newtran = (newtran + ni.transpose) & 1;
//				makerot(ni, localtrans);
//				transmult(localtrans, ntrans, trans);
//			}
//	
//			// create this node
//			xc = (ni.lowx + ni.highx) / 2;   yc = (ni.lowy + ni.highy) / 2;
//			xform(xc, yc, &newx, &newy, trans);
//			newx -= (ni.highx - ni.lowx) / 2;
//			newy -= (ni.highy - ni.lowy) / 2;
//			newang = newang % 3600;   if (newang < 0) newang += 3600;
//			newni = newnodeinst(ni.proto, newx, newx+ni.highx-ni.lowx,
//				newy, newy+ni.highy-ni.lowy, newtran, newang, np);
//			if (newni == NONODEINST) return(NONODEPROTO);
//			endobjectchange((INTBIG)newni, VNODEINST);
//	
//			// export the port from the node
//			(void)newportproto(np, newni, rpp, pp.protoname);
//		}
//	
//		// make sure cell is the same size
//		i = (fromnp.highy+fromnp.lowy)/2 - (gen_invispinprim.highy-gen_invispinprim.lowy)/2;
//		(void)newnodeinst(gen_invispinprim, fromnp.lowx, fromnp.lowx+gen_invispinprim.highx-gen_invispinprim.lowx,
//			i, i+gen_invispinprim.highy-gen_invispinprim.lowy, 0, 0, np);
//	
//		i = (fromnp.highy+fromnp.lowy)/2 - (gen_invispinprim.highy-gen_invispinprim.lowy)/2;
//		(void)newnodeinst(gen_invispinprim, fromnp.highx-(gen_invispinprim.highx-gen_invispinprim.lowx), fromnp.highx,
//			i, i+gen_invispinprim.highy-gen_invispinprim.lowy, 0, 0, np);
//	
//		i = (fromnp.highx+fromnp.lowx)/2 - (gen_invispinprim.highx-gen_invispinprim.lowx)/2;
//		(void)newnodeinst(gen_invispinprim, i, i+gen_invispinprim.highx-gen_invispinprim.lowx,
//			fromnp.lowy, fromnp.lowy+gen_invispinprim.highy-gen_invispinprim.lowy, 0, 0, np);
//	
//		i = (fromnp.highx+fromnp.lowx)/2 - (gen_invispinprim.highx-gen_invispinprim.lowx)/2;
//		(void)newnodeinst(gen_invispinprim, i, i+gen_invispinprim.highx-gen_invispinprim.lowx,
//			fromnp.highy-(gen_invispinprim.highy-gen_invispinprim.lowy), fromnp.highy, 0, 0,np);
//	
//		(*el_curconstraint.solve)(np);
//		return(np);
//	}
}
