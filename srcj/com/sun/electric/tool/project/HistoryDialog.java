/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Project.java
 * Project management tool: cell history dialog
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
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * Class to display a cell history dialog.
 */
public class HistoryDialog extends EDialog
{
	private ProjectDB pdb;
	private Cell cell;
	private TableModel dataModel;
	private JTable table;

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
		Project.pmActive = true;
		new HistoryDialog(cell);
	}

	HistoryDialog(Cell cell)
	{
		super(null, true);
		this.pdb = Project.projectDB;
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
			Library lib = cell.getLibrary();
			ProjectLibrary pl = pdb.findProjectLibrary(lib);

			String cellName = cell.getName() + ";" + version;
			if (cell.getView() != View.UNKNOWN) cellName += "{" + cell.getView().getAbbreviation() + "}";
			Cell exists = cell.getLibrary().findNodeProto(cellName);
			if (exists != null)
			{
				Job.getUserInterface().showErrorMessage("Version " + version + " of cell " + cell.getName() +
					" is already in your library", "Version Retrieval Error");
				return;
			}

			ProjectCell foundPC = pl.findProjectCellByNameViewVersion(cell.getName(), cell.getView(), version);
			if (foundPC == null)
			{
				Job.getUserInterface().showErrorMessage("Can't find that version in the repository!", "Version Retrieval Error");
				return;
			}
			new GetOldVersionJob(pdb, cell, version);
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
		ProjectLibrary pl = pdb.findProjectLibrary(cell.getLibrary());
		List<ProjectCell> versions = new ArrayList<ProjectCell>();
		for(Iterator<ProjectCell> it = pl.getProjectCells(); it.hasNext(); )
		{
			ProjectCell pc = it.next();
			if (pc.getCellName().equals(cell.getName()) && pc.getView() == cell.getView())
			{
				pc.setCheckInDate("Not In Repository Yet");
				versions.add(pc);
			}
		}

		// consider the files in the repository, too
		String dirName = pl.getProjectDirectory() + File.separator + cell.getName();
		File dir = new File(dirName);
		File [] filesInDir = dir.listFiles();
		for(int i=0; i<filesInDir.length; i++)
		{
			File subFile = filesInDir[i];
			Date modDate = new Date(subFile.lastModified());
			int version = TextUtils.atoi(subFile.getName());
			boolean found = false;
			for(ProjectCell pc : versions)
			{
				if (pc.getVersion() == version)
				{
					pc.setCheckInDate(TextUtils.formatDate(modDate));
					found = true;
					break;
				}
			}
			if (!found)
			{
				ProjectCell pc = new ProjectCell(null, pl);
				pc.setCheckInDate(TextUtils.formatDate(modDate));
				pc.setCellName(cell.getName());
				pc.setVersion(version);
				versions.add(pc);
			}
		}

		// sort the list by versions
		Collections.sort(versions, new ProjectCellByVersion());

		// make table
		int numVersions = versions.size();
		Object [][] data = new Object[numVersions][4];
		int index = 0;
		for(ProjectCell pc : versions)
		{
			data[index][0] = Integer.toString(pc.getVersion());
			data[index][1] = pc.getCheckInDate();
			data[index][2] = pc.getLastOwner();
			if (pc.getOwner().length() > 0) data[index][2] = pc.getOwner();
			data[index][3] = (pc.getComment() != null) ? pc.getComment() : "";
			index++;
		}

		dataModel = new HistoryTableModel(data);
		table = new JTable(dataModel);
//		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
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

	/**
	 * This class gets old versions of cells from the Project Management repository.
	 */
	private static class GetOldVersionJob extends Job
	{
		private ProjectDB pdb;
		private Cell cell;
		private int version;

		private GetOldVersionJob(ProjectDB pdb, Cell cell, int version)
		{
			super("Update " + cell, Project.getProjectTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.pdb = pdb;
			this.cell = cell;
			this.version = version;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// prevent tools (including this one) from seeing the change
			Project.setChangeStatus(true);

			Library lib = cell.getLibrary();
			ProjectLibrary pl = pdb.findProjectLibrary(lib);
			ProjectCell foundPC = pl.findProjectCellByNameViewVersion(cell.getName(), cell.getView(), version);
			Project.getCellFromRepository(pdb, foundPC, lib, false, false);		// CHANGES DATABASE
			if (foundPC.getCell() == null)
			{
				Project.setChangeStatus(false);
				throw new JobException("Error retrieving old version of cell");
			}
			Project.markLocked(foundPC.getCell(), false);		// CHANGES DATABASE

			// allow changes
			Project.setChangeStatus(false);

			System.out.println("Cell " + foundPC.getCell().describe(true) + " is now in this library");
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
	
	/**
	 * Class to sort project cells by reverse version number.
	 */
	private static class ProjectCellByVersion implements Comparator<ProjectCell>
	{
	    public int compare(ProjectCell pc1, ProjectCell pc2)
	    {
	    	return pc2.getVersion() - pc1.getVersion();
	    }
	}

}
