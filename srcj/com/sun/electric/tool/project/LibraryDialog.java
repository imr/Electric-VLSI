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

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.dialogs.EDialog;
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
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;


/**
 * This is the Project Management tool.
 */
public class LibraryDialog extends EDialog
{
	private JList libList;
	private DefaultListModel libModel;

	/**
	 * Method to prompt for all libraries in the repository and
	 * choose one to retrieve.
	 */
	public static void getALibrary()
	{
		Project.pmActive = true;

		// find a list of files (libraries) in the repository
		String dirName = Project.getRepositoryLocation();
		File dir = new File(dirName);
		File [] filesInDir = dir.listFiles();
		if (filesInDir == null && dirName.length() == 0)
		{
			Job.getUserInterface().showInformationMessage("No repository location is set.  Use the 'Project Management' Preferences to set it.", "Warning");
			return;
		}

		// choose one and read it in
		new LibraryDialog(filesInDir);
	}

	LibraryDialog(File [] filesInDir)
	{
		super(null, true);
		initComponents(filesInDir);
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

	private void initComponents(File [] filesInDir)
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
//		String dirName = Project.getRepositoryLocation();
//		File dir = new File(dirName);
//		File [] filesInDir = dir.listFiles();
//		if (filesInDir == null && dirName.length() == 0)
//		{
//			Job.getUserInterface().showInformationMessage("No repository location is set.  Use the 'Project Management' Preferences to set it.", "Warning");
//		}
//		if (filesInDir != null)
		{
			List<String> libNames = new ArrayList<String>();
			for(int i=0; i<filesInDir.length; i++)
			{
				File subFile = filesInDir[i];
				if (subFile.isDirectory())
					libNames.add(subFile.getName());
			}
			Collections.sort(libNames, new TextUtils.ObjectsByToString());
			for(String libName : libNames)
			{
				libModel.addElement(libName);
			}
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

	/**
	 * This class gets a library from the Project Management repository.
	 */
	private static class RetrieveLibraryFromRepositoryJob extends Job
	{
		private ProjectDB pdb;
		private String libName;

		private RetrieveLibraryFromRepositoryJob(String libName)
		{
			super("Retrieve Library from Repository", Project.getProjectTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.pdb = Project.projectDB;
			this.libName = libName;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			Library lib = Library.findLibrary(libName);
			if (lib != null)
				throw new JobException("Library '" + lib.getName() + "' already exists");
			lib = Library.newInstance(libName, null);
			String projFile = Project.getRepositoryLocation() + File.separator + libName + File.separator + Project.PROJECTFILE;
			File pf = new File(projFile);
			if (!pf.exists())
				throw new JobException("Cannot find project file '" + projFile + "'...retrieve aborted.");
			lib.newVar(Project.PROJPATHKEY, projFile);

			ProjectLibrary pl = pdb.findProjectLibrary(lib);

			// prevent tools (including this one) from seeing the change
			Project.setChangeStatus(true);

			// make a list of the most recent cells that are not checked-out
			List<ProjectCell> cellsToGet = new ArrayList<ProjectCell>();
			String lastName = "";
			for(Iterator<ProjectCell> it = pl.getProjectCells(); it.hasNext(); )
			{
				ProjectCell pc = it.next();
				String name = pc.describe();
				if (pc.getOwner().length() > 0) continue;
				if (name.equals(lastName)) cellsToGet.remove(cellsToGet.size()-1);
				cellsToGet.add(pc);
				lastName = name;
			}

//System.out.println("BEFORE:================================");
//for(ProjectLibrary pll : pdb.getProjectLibraries())
//{
//	for(Iterator<ProjectCell> it = pll.getProjectCells(); it.hasNext(); )
//	{
//		ProjectCell pc = it.next();
//		System.out.println("PL="+pll.getLibrary().getName()+" PC="+pc.describe());
//	}
//}
			// check them out
			String userName = Project.getCurrentUserName();
			for(ProjectCell pc : cellsToGet)
			{
				if (pc.getCell() == null)
				{
					Project.getCellFromRepository(pdb, pc, lib, true, true);		// CHANGES DATABASE
					if (pc.getCell() == null)
					{
						Project.setChangeStatus(false);
						throw new JobException("Error retrieving old version of cell");
					}
				}
				if (pc.getCell() != null)
				{
					boolean youOwn = userName.length() > 0 && pc.getOwner().equals(userName);
					Project.markLocked(pc.getCell(), !youOwn);		// CHANGES DATABASE
				}
			}

			// allow changes
			Project.setChangeStatus(false);

//System.out.println("AFTER:================================");
//for(ProjectLibrary pll : pdb.getProjectLibraries())
//{
//	for(Iterator<ProjectCell> it = pll.getProjectCells(); it.hasNext(); )
//	{
//		ProjectCell pc = it.next();
//		System.out.println("PL="+pll.getLibrary().getName()+" PC="+pc.describe());
//	}
//}
			System.out.println("Library " + lib.getName() + " has been retrieved from the repository");
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
}
