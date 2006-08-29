/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GeneralTab.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.io.FileType;

import java.awt.Frame;
import java.util.HashMap;
import java.util.Collection;
import java.util.Map;

import javax.swing.JPanel;

/**
 * Class to handle the "General" tab of the Preferences dialog.
 */
public class GeneralTab extends PreferencePanel
{
    private HashMap<Object,String> fileTypeMap = new HashMap<Object,String>();

	/** Creates new form General Options */
	public GeneralTab(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return general; }

	/** return the name of this preferences tab. */
	public String getName() { return "General"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the General tab.
	 */
	public void init()
	{
		// I/O section
		generalShowFileDialog.setSelected(User.isShowFileSelectionForNetlists());
        for (Object obj : FileType.getFileTypeGroups())
        {
            workingDirComboBox.addItem(obj);
            fileTypeMap.put(obj, null);
        }
        workingDirComboBoxActionPerformed(null);
        pathTextField.setText(User.getWorkingDirectory());

        // Jobs section
        generalBeepAfterLongJobs.setSelected(User.isBeepAfterLongJobs());
        generalVerboseMode.setSelected(User.isJobVerboseMode());
		generalErrorLimit.setText(Integer.toString(User.getErrorLimit()));
        maxUndoHistory.setText(Integer.toString(User.getMaxUndoHistory()));

        // Memory section
		Runtime runtime = Runtime.getRuntime();
		long maxMemLimit = runtime.maxMemory() / 1024 / 1024;
		generalMemoryUsage.setText("Current memory usage: " + Long.toString(maxMemLimit) + " megabytes");
		generalMaxMem.setText(Long.toString(User.getMemorySize()));
        generalMaxSize.setText(Long.toString(User.getPermSpace()));

        // Database section
		generalSnapshotLogging.setSelected(User.isUseTwoJVMs());
//		generalSnapshotLogging.setEnabled(false);
		generalLogClientServer.setSelected(User.isUseClientServer());
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the General tab.
	 */
	public void term()
	{
		// I/O section
		boolean currBoolean = generalShowFileDialog.isSelected();
		if (currBoolean != User.isShowFileSelectionForNetlists())
			User.setShowFileSelectionForNetlists(currBoolean);

        // Resetting dir path
        Collection col = fileTypeMap.values();
        for (Map.Entry<Object,String> entry : fileTypeMap.entrySet())
        {
            Object obj = entry.getKey();
            FileType.setFileTypeGroupDir(obj, entry.getValue());
        }

//		String currentInitialWorkingDirSetting = (String)workingDirComboBox.getSelectedItem();
//        if (!currentInitialWorkingDirSetting.equals(User.getInitialWorkingDirectorySetting()))
//            User.setInitialWorkingDirectorySetting(currentInitialWorkingDirSetting);

        // Jobs section
        currBoolean = generalBeepAfterLongJobs.isSelected();
		if (currBoolean != User.isBeepAfterLongJobs())
			User.setBeepAfterLongJobs(currBoolean);

        currBoolean = generalVerboseMode.isSelected();
		if (currBoolean != User.isJobVerboseMode())
			User.setJobVerboseMode(currBoolean);

		int currInt = TextUtils.atoi(generalErrorLimit.getText());
		if (currInt != User.getErrorLimit())
			User.setErrorLimit(currInt);

        currInt = TextUtils.atoi(maxUndoHistory.getText());
        if (currInt != User.getMaxUndoHistory())
        {
            User.setMaxUndoHistory(currInt);
            Undo.setHistoryListSize(currInt);
        }

		// Memory section
		currInt = TextUtils.atoi(generalMaxMem.getText());
		if (currInt != User.getMemorySize())
			User.setMemorySize(currInt);

        currInt = TextUtils.atoi(generalMaxSize.getText());
		if (currInt != User.getPermSpace())
			User.setPermSpace(currInt);

		// Database section
		currBoolean = generalSnapshotLogging.isSelected();
		if (currBoolean != User.isUseTwoJVMs())
			User.setUseTwoJVMs(currBoolean);

		currBoolean = generalLogClientServer.isSelected();
		if (currBoolean != User.isUseClientServer())
			User.setUseClientServer(currBoolean);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents()
    {
        java.awt.GridBagConstraints gridBagConstraints;

        general = new javax.swing.JPanel();
        memory = new javax.swing.JPanel();
        jLabel60 = new javax.swing.JLabel();
        generalMaxMem = new javax.swing.JTextField();
        jLabel61 = new javax.swing.JLabel();
        generalMemoryUsage = new javax.swing.JLabel();
        jLabel62 = new javax.swing.JLabel();
        jLabel63 = new javax.swing.JLabel();
        generalMaxSize = new javax.swing.JTextField();
        jLabel64 = new javax.swing.JLabel();
        IO = new javax.swing.JPanel();
        generalShowFileDialog = new javax.swing.JCheckBox();
        groupPanel = new javax.swing.JPanel();
        newPathLabel = new javax.swing.JLabel();
        pathLabel = new javax.swing.JLabel();
        pathTextField = new javax.swing.JTextField();
        resetButton = new javax.swing.JButton();
        workingDirComboBox = new javax.swing.JComboBox();
        groupLabel = new javax.swing.JLabel();
        currentPathLabel = new javax.swing.JLabel();
        jobs = new javax.swing.JPanel();
        generalBeepAfterLongJobs = new javax.swing.JCheckBox();
        jLabel46 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        generalErrorLimit = new javax.swing.JTextField();
        maxUndoHistory = new javax.swing.JTextField();
        jLabel53 = new javax.swing.JLabel();
        generalVerboseMode = new javax.swing.JCheckBox();
        database = new javax.swing.JPanel();
        generalLogClientServer = new javax.swing.JCheckBox();
        generalSnapshotLogging = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Edit Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        general.setLayout(new java.awt.GridBagLayout());

        memory.setLayout(new java.awt.GridBagLayout());

        memory.setBorder(javax.swing.BorderFactory.createTitledBorder("Memory"));
        jLabel60.setText("Maximum memory:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        memory.add(jLabel60, gridBagConstraints);

        generalMaxMem.setColumns(6);
        generalMaxMem.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        memory.add(generalMaxMem, gridBagConstraints);

        jLabel61.setText("megabytes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        memory.add(jLabel61, gridBagConstraints);

        generalMemoryUsage.setText("Current memory usage:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        memory.add(generalMemoryUsage, gridBagConstraints);

        jLabel62.setText("Changes to memory take effect when Electric is next run");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        memory.add(jLabel62, gridBagConstraints);

        jLabel63.setText("Maximum permanent space:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        memory.add(jLabel63, gridBagConstraints);

        generalMaxSize.setColumns(6);
        generalMaxSize.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        memory.add(generalMaxSize, gridBagConstraints);

        jLabel64.setText("megabytes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        memory.add(jLabel64, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        general.add(memory, gridBagConstraints);

        IO.setLayout(new java.awt.GridBagLayout());

        IO.setBorder(javax.swing.BorderFactory.createTitledBorder("I/O"));
        generalShowFileDialog.setText("Show file-selection dialog before writing netlists");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        IO.add(generalShowFileDialog, gridBagConstraints);

        groupPanel.setLayout(new java.awt.GridBagLayout());

        groupPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Current Directory (by type)"));
        newPathLabel.setText("New:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        groupPanel.add(newPathLabel, gridBagConstraints);

        pathLabel.setText("Current:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        groupPanel.add(pathLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        groupPanel.add(pathTextField, gridBagConstraints);

        resetButton.setText("Reset");
        resetButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                resetButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        groupPanel.add(resetButton, gridBagConstraints);

        workingDirComboBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                workingDirComboBoxActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        groupPanel.add(workingDirComboBox, gridBagConstraints);

        groupLabel.setText("Type:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        groupPanel.add(groupLabel, gridBagConstraints);

        currentPathLabel.setText("jLabel1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        groupPanel.add(currentPathLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        IO.add(groupPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        general.add(IO, gridBagConstraints);

        jobs.setLayout(new java.awt.GridBagLayout());

        jobs.setBorder(javax.swing.BorderFactory.createTitledBorder("Jobs"));
        generalBeepAfterLongJobs.setText("Beep after long jobs");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jobs.add(generalBeepAfterLongJobs, gridBagConstraints);

        jLabel46.setText("Maximum errors to report:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jobs.add(jLabel46, gridBagConstraints);

        jLabel2.setText("Maximum undo history");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jobs.add(jLabel2, gridBagConstraints);

        generalErrorLimit.setColumns(6);
        generalErrorLimit.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jobs.add(generalErrorLimit, gridBagConstraints);

        maxUndoHistory.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jobs.add(maxUndoHistory, gridBagConstraints);

        jLabel53.setText("(0 for infinite)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jobs.add(jLabel53, gridBagConstraints);

        generalVerboseMode.setText("Verbose mode");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jobs.add(generalVerboseMode, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        general.add(jobs, gridBagConstraints);

        database.setLayout(new java.awt.GridBagLayout());

        database.setBorder(javax.swing.BorderFactory.createTitledBorder("Database"));
        generalLogClientServer.setText("Use Client / Server interactions");
        generalLogClientServer.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        generalLogClientServer.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        database.add(generalLogClientServer, gridBagConstraints);

        generalSnapshotLogging.setText("Snapshot Logging");
        generalSnapshotLogging.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        generalSnapshotLogging.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        database.add(generalSnapshotLogging, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        general.add(database, gridBagConstraints);

        getContentPane().add(general, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void workingDirComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_workingDirComboBoxActionPerformed
        currentPathLabel.setText(FileType.getGroupPath(workingDirComboBox.getSelectedItem()));
    }//GEN-LAST:event_workingDirComboBoxActionPerformed

    private void newPath()
    {
         Object obj = workingDirComboBox.getSelectedItem();
        // Storing string
        fileTypeMap.put(obj, pathTextField.getText());
    }
    private void resetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetButtonActionPerformed
        Object obj = workingDirComboBox.getSelectedItem();
        // Storing string
        fileTypeMap.put(obj, pathTextField.getText());

    }//GEN-LAST:event_resetButtonActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel IO;
    private javax.swing.JLabel currentPathLabel;
    private javax.swing.JPanel database;
    private javax.swing.JPanel general;
    private javax.swing.JCheckBox generalBeepAfterLongJobs;
    private javax.swing.JTextField generalErrorLimit;
    private javax.swing.JCheckBox generalLogClientServer;
    private javax.swing.JTextField generalMaxMem;
    private javax.swing.JTextField generalMaxSize;
    private javax.swing.JLabel generalMemoryUsage;
    private javax.swing.JCheckBox generalShowFileDialog;
    private javax.swing.JCheckBox generalSnapshotLogging;
    private javax.swing.JCheckBox generalVerboseMode;
    private javax.swing.JLabel groupLabel;
    private javax.swing.JPanel groupPanel;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel60;
    private javax.swing.JLabel jLabel61;
    private javax.swing.JLabel jLabel62;
    private javax.swing.JLabel jLabel63;
    private javax.swing.JLabel jLabel64;
    private javax.swing.JPanel jobs;
    private javax.swing.JTextField maxUndoHistory;
    private javax.swing.JPanel memory;
    private javax.swing.JLabel newPathLabel;
    private javax.swing.JLabel pathLabel;
    private javax.swing.JTextField pathTextField;
    private javax.swing.JButton resetButton;
    private javax.swing.JComboBox workingDirComboBox;
    // End of variables declaration//GEN-END:variables

}
