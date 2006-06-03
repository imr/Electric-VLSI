/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NewCell.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.JList;


/**
 * Class to handle the "New Cell" dialog.
 */
public class NewCell extends EDialog
{
	private JList viewList;
	private DefaultListModel viewModel;
	private static boolean makeWindow = false;

    /**
     * Inner class to control options based on views and technologies.
     * This is done to access View or Tech without calling toString()
     */
    private static class ViewTechOption
    {
        View view;
        Technology tech;

        ViewTechOption(View view, Technology tech)
        {
            this.view = view;
            this.tech = tech;
        }
        public String toString()
        {
            if (view != null)
                return view.getFullName();
            return tech.getTechName();
        }
    }

	/** Creates new form New Cell */
	public NewCell(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
        getRootPane().setDefaultButton(ok);

		// make a list of views
		viewModel = new DefaultListModel();
		viewList = new JList(viewModel);
		viewList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		view.setViewportView(viewList);
        Technology curTech = Technology.getCurrent();
        Technology defTech = Technology.findTechnology(User.getDefaultTechnology());
		/*for(View v: View.getOrderedViews())	viewModel.addElement(v.getFullName());*/
        ViewTechOption theViewOption = null;
        View defaultView = View.LAYOUT;
        if (curTech == Schematics.tech) defaultView = View.SCHEMATIC;
        else if (curTech == Artwork.tech) defaultView = View.ICON;
		for (View v : View.getOrderedViews())
		{
            ViewTechOption option = new ViewTechOption(v, null);
			viewModel.addElement(option);
            if (v == defaultView) theViewOption = option;
		}
        viewList.setSelectedValue(theViewOption, true);
		viewList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
                techComboBoxItemStateChanged(null);
				if (e.getClickCount() == 2) ok(null);
			}
		});
        // Choosen appropiate technology
        for (Iterator<Technology> it = Technology.getTechnologies(); it.hasNext();)
        {
            Technology tech = it.next();
            if (tech != Schematics.tech && tech != Artwork.tech)
            {
                ViewTechOption option = new ViewTechOption(null, tech);
                techComboBox.addItem(option);
                if ((defaultView == View.LAYOUT && tech == curTech) ||
                    (tech == defTech)) techComboBox.setSelectedItem(option);
            }
        }
        //techComboBox.setSelectedItem(curTech.getTechName());
//        boolean enableTechList = false;
//		if (curTech == Schematics.tech) viewList.setSelectedValue(View.SCHEMATIC, true);
//        else if (curTech == Artwork.tech) viewList.setSelectedValue(View.ICON, true);
//        else
//        {
//            enableTechList = true;
//            viewList.setSelectedValue(View.LAYOUT, true);
//        }
        // Only capable to switch technology if they are layout-based
        //techComboBox.setEnabled(enableTechList);
        techComboBoxItemStateChanged(null);

		// make a popup of libraries
		List<Library> libList = Library.getVisibleLibraries();
		for (Library lib : libList)
		{
			library.addItem(lib.getName());
		}
		int curIndex = libList.indexOf(Library.getCurrent());
		if (curIndex >= 0) library.setSelectedIndex(curIndex);

		newWindow.setSelected(makeWindow);
		cellName.grabFocus();
		finishInitialization();
	}

	protected void escapePressed() { cancel(null); }

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        cellName = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        library = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        newWindow = new javax.swing.JCheckBox();
        view = new javax.swing.JScrollPane();
        techLabel = new javax.swing.JLabel();
        techComboBox = new javax.swing.JComboBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("New Cell");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancel(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ok(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        cellName.setColumns(20);
        cellName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cellNameActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cellName, gridBagConstraints);

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel4.setText("Library:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(library, gridBagConstraints);

        jLabel1.setText("Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel1, gridBagConstraints);

        jLabel2.setText("View:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel2, gridBagConstraints);

        newWindow.setText("Make new window");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        getContentPane().add(newWindow, gridBagConstraints);

        view.setMinimumSize(new java.awt.Dimension(200, 150));
        view.setPreferredSize(new java.awt.Dimension(200, 150));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(view, gridBagConstraints);

        techLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        techLabel.setText("Technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(techLabel, gridBagConstraints);

        techComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                techComboBoxItemStateChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(techComboBox, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

    private void techComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_techComboBoxItemStateChanged
        ViewTechOption view = (ViewTechOption)viewList.getSelectedValue();
        if (view != null)
            techComboBox.setEnabled(view.view == View.LAYOUT);
    }//GEN-LAST:event_techComboBoxItemStateChanged

	private void cellNameActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cellNameActionPerformed
	{//GEN-HEADEREND:event_cellNameActionPerformed
		ok(evt);
	}//GEN-LAST:event_cellNameActionPerformed

	private void cancel(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancel
	{//GEN-HEADEREND:event_cancel
		closeDialog(null);
	}//GEN-LAST:event_cancel

	private void ok(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ok
	{//GEN-HEADEREND:event_ok
		String name = cellName.getText().trim();
		if (name.length() == 0)
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), "Must type a cell name");
			return;
		}
		String viewName = (String)((ViewTechOption)viewList.getSelectedValue()).view.getFullName();
		View v = View.findView(viewName);
		if (v != View.UNKNOWN)  name += "{" + v.getAbbreviation() + "}";
		String libName = (String)library.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		makeWindow = newWindow.isSelected();

		// create the cell
        Technology tech = null;
        if (techComboBox.isEnabled()) tech = ((ViewTechOption)techComboBox.getSelectedItem()).tech;
		CreateCell job = new CreateCell(lib, name, tech, makeWindow);

		closeDialog(null);
	}//GEN-LAST:event_ok

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		makeWindow = newWindow.isSelected();
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

	/**
	 * Class to create a cell in a new thread.
	 */
	private static class CreateCell extends Job
	{
		private Library lib;
		private String cellName;
		private boolean newWindow;
		private Technology tech;
        private Cell newCell;

		protected CreateCell(Library lib, String cellName, Technology tech, boolean newWindow)
		{
			super("Create Cell " + cellName, User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.lib = lib;
			this.cellName = cellName;
			this.newWindow = newWindow;
            this.tech = tech;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// should ensure that the name is valid
			newCell = Cell.makeInstance(lib, cellName);
			if (newCell == null)
				throw new JobException("Unable to create cell " + cellName);
			newCell.setTechnology(tech);
			fieldVariableChanged("newCell");
			return true;
		}

        public void terminateOK()
        {
            if (newWindow)
            {
                WindowFrame.createEditWindow(newCell);
            } else
            {
                WindowFrame wf = WindowFrame.getCurrentWindowFrame();
                wf.setCellWindow(newCell, null);
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancel;
    private javax.swing.JTextField cellName;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JComboBox library;
    private javax.swing.JCheckBox newWindow;
    private javax.swing.JButton ok;
    private javax.swing.JComboBox techComboBox;
    private javax.swing.JLabel techLabel;
    private javax.swing.JScrollPane view;
    // End of variables declaration//GEN-END:variables

}
