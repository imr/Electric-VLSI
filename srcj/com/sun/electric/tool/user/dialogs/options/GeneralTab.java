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

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.change.Undo;
import com.sun.electric.tool.user.User;

import javax.swing.JPanel;
import java.util.Iterator;

/**
 * Class to handle the "General" tab of the Preferences dialog.
 */
public class GeneralTab extends PreferencePanel
{
	/** Creates new form Edit Options */
	public GeneralTab(java.awt.Frame parent, boolean modal)
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
		generalBeepAfterLongJobs.setSelected(User.isBeepAfterLongJobs());
        generalVerboseMode.setSelected(User.isJobVerboseMode());
		generalShowFileDialog.setSelected(User.isShowFileSelectionForNetlists());
		generalShowCursorCoordinates.setSelected(User.isShowHierarchicalCursorCoordinates());
		generalIncludeDateAndVersion.setSelected(User.isIncludeDateAndVersionInOutput());
		sideBarOnRight.setSelected(User.isSideBarOnRight());
		generalPromptForIndex.setSelected(User.isPromptForIndexWhenDescending());
		generalOlderDisplayAlgorithm.setSelected(User.isUseOlderDisplayAlgorithm());
		generalUseGreekImages.setSelected(User.isUseCellGreekingImages());
		generalGreekLimit.setText(Double.toString(User.getGreekSizeLimit()));
		generalGreekCellLimit.setText(Double.toString(User.getGreekCellSizeLimit()));

        for (Iterator it = User.getInitialWorkingDirectorySettings(); it.hasNext(); )
            workingDirComboBox.addItem(it.next());
        workingDirComboBox.setSelectedItem(User.getInitialWorkingDirectorySetting());

		generalPanningDistance.addItem("Small");
		generalPanningDistance.addItem("Medium");
		generalPanningDistance.addItem("Large");
		generalPanningDistance.setSelectedIndex(User.getPanningDistance());

		generalErrorLimit.setText(Integer.toString(User.getErrorLimit()));

        maxUndoHistory.setText(Integer.toString(User.getMaxUndoHistory()));

		java.lang.Runtime runtime = java.lang.Runtime.getRuntime();
		long maxMemLimit = runtime.maxMemory() / 1024 / 1024;
		generalMemoryUsage.setText("Current memory usage: " + Long.toString(maxMemLimit) + " megabytes");
		generalMaxMem.setText(Long.toString(User.getMemorySize()));
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the General tab.
	 */
	public void term()
	{
		boolean currBoolean = generalBeepAfterLongJobs.isSelected();
		if (currBoolean != User.isBeepAfterLongJobs())
			User.setBeepAfterLongJobs(currBoolean);

        currBoolean = generalVerboseMode.isSelected();
		if (currBoolean != User.isJobVerboseMode())
			User.setJobVerboseMode(currBoolean);

		currBoolean = generalShowFileDialog.isSelected();
		if (currBoolean != User.isShowFileSelectionForNetlists())
			User.setShowFileSelectionForNetlists(currBoolean);

		currBoolean = generalShowCursorCoordinates.isSelected();
		if (currBoolean != User.isShowHierarchicalCursorCoordinates())
			User.setShowHierarchicalCursorCoordinates(currBoolean);

		currBoolean = generalIncludeDateAndVersion.isSelected();
		if (currBoolean != User.isIncludeDateAndVersionInOutput())
			User.setIncludeDateAndVersionInOutput(currBoolean);

		currBoolean = sideBarOnRight.isSelected();
		if (currBoolean != User.isSideBarOnRight())
			User.setSideBarOnRight(currBoolean);

		currBoolean = generalPromptForIndex.isSelected();
		if (currBoolean != User.isPromptForIndexWhenDescending())
			User.setPromptForIndexWhenDescending(currBoolean);

		currBoolean = generalOlderDisplayAlgorithm.isSelected();
		if (currBoolean != User.isUseOlderDisplayAlgorithm())
			User.setUseOlderDisplayAlgorithm(currBoolean);

		currBoolean = generalUseGreekImages.isSelected();
		if (currBoolean != User.isUseCellGreekingImages())
			User.setUseCellGreekingImages(currBoolean);

		double currDouble = TextUtils.atof(generalGreekLimit.getText());
		if (currDouble != User.getGreekSizeLimit())
			User.setGreekSizeLimit(currDouble);

		currDouble = TextUtils.atof(generalGreekCellLimit.getText());
		if (currDouble != User.getGreekCellSizeLimit())
			User.setGreekCellSizeLimit(currDouble);

		String currentInitialWorkingDirSetting = (String)workingDirComboBox.getSelectedItem();
        if (!currentInitialWorkingDirSetting.equals(User.getInitialWorkingDirectorySetting()))
            User.setInitialWorkingDirectorySetting(currentInitialWorkingDirSetting);

		int currInt = generalPanningDistance.getSelectedIndex();
		if (currInt != User.getPanningDistance())
			User.setPanningDistance(currInt);

		currInt = TextUtils.atoi(generalErrorLimit.getText());
		if (currInt != User.getErrorLimit())
			User.setErrorLimit(currInt);

		currInt = TextUtils.atoi(generalMaxMem.getText());
		if (currInt != User.getMemorySize())
			User.setMemorySize(currInt);

        currInt = TextUtils.atoi(maxUndoHistory.getText());
        if (currInt != User.getMaxUndoHistory()) {
            User.setMaxUndoHistory(currInt);
            Undo.setHistoryListSize(currInt);
        }
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
        display = new javax.swing.JPanel();
        generalShowCursorCoordinates = new javax.swing.JCheckBox();
        sideBarOnRight = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        generalPanningDistance = new javax.swing.JComboBox();
        generalPromptForIndex = new javax.swing.JCheckBox();
        generalOlderDisplayAlgorithm = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        generalGreekLimit = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        generalGreekCellLimit = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        generalUseGreekImages = new javax.swing.JCheckBox();
        IO = new javax.swing.JPanel();
        generalIncludeDateAndVersion = new javax.swing.JCheckBox();
        generalShowFileDialog = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        workingDirComboBox = new javax.swing.JComboBox();
        jobs = new javax.swing.JPanel();
        generalBeepAfterLongJobs = new javax.swing.JCheckBox();
        jLabel46 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        generalErrorLimit = new javax.swing.JTextField();
        maxUndoHistory = new javax.swing.JTextField();
        jLabel53 = new javax.swing.JLabel();
        generalVerboseMode = new javax.swing.JCheckBox();

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

        memory.setBorder(new javax.swing.border.TitledBorder("Memory"));
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
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        memory.add(jLabel62, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        general.add(memory, gridBagConstraints);

        display.setLayout(new java.awt.GridBagLayout());

        display.setBorder(new javax.swing.border.TitledBorder("Display"));
        generalShowCursorCoordinates.setText("Show hierarchical cursor coordinates in status bar");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        display.add(generalShowCursorCoordinates, gridBagConstraints);

        sideBarOnRight.setText("Side Bar defaults to the right side");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        display.add(sideBarOnRight, gridBagConstraints);

        jLabel1.setText("Panning distance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        display.add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        display.add(generalPanningDistance, gridBagConstraints);

        generalPromptForIndex.setText("Always prompt for index when descending into array nodes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        display.add(generalPromptForIndex, gridBagConstraints);

        generalOlderDisplayAlgorithm.setText("Use older display algorithm");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        display.add(generalOlderDisplayAlgorithm, gridBagConstraints);

        jLabel4.setText("Greek objects smaller than:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        display.add(jLabel4, gridBagConstraints);

        generalGreekLimit.setColumns(5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        display.add(generalGreekLimit, gridBagConstraints);

        jLabel5.setText("pixels");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        display.add(jLabel5, gridBagConstraints);

        jLabel6.setText("Do not greek cells greater than:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        display.add(jLabel6, gridBagConstraints);

        generalGreekCellLimit.setColumns(5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        display.add(generalGreekCellLimit, gridBagConstraints);

        jLabel7.setText("percent of screen");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        display.add(jLabel7, gridBagConstraints);

        generalUseGreekImages.setText("Use cell images when greeking");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        display.add(generalUseGreekImages, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        general.add(display, gridBagConstraints);

        IO.setLayout(new java.awt.GridBagLayout());

        IO.setBorder(new javax.swing.border.TitledBorder("I/O"));
        generalIncludeDateAndVersion.setText("Include date and version in output files");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        IO.add(generalIncludeDateAndVersion, gridBagConstraints);

        generalShowFileDialog.setText("Show file-selection dialog before writing netlists");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        IO.add(generalShowFileDialog, gridBagConstraints);

        jLabel3.setText("Working directory:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        IO.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        IO.add(workingDirComboBox, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        general.add(IO, gridBagConstraints);

        jobs.setLayout(new java.awt.GridBagLayout());

        jobs.setBorder(new javax.swing.border.TitledBorder("Jobs"));
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
        generalVerboseMode.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                generalVerboseModeActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jobs.add(generalVerboseMode, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        general.add(jobs, gridBagConstraints);

        getContentPane().add(general, new java.awt.GridBagConstraints());

        pack();
    }
    // </editor-fold>//GEN-END:initComponents

    private void generalVerboseModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generalVerboseModeActionPerformed
// TODO add your handling code here:
    }//GEN-LAST:event_generalVerboseModeActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel IO;
    private javax.swing.JPanel display;
    private javax.swing.JPanel general;
    private javax.swing.JCheckBox generalBeepAfterLongJobs;
    private javax.swing.JTextField generalErrorLimit;
    private javax.swing.JTextField generalGreekCellLimit;
    private javax.swing.JTextField generalGreekLimit;
    private javax.swing.JCheckBox generalIncludeDateAndVersion;
    private javax.swing.JTextField generalMaxMem;
    private javax.swing.JLabel generalMemoryUsage;
    private javax.swing.JCheckBox generalOlderDisplayAlgorithm;
    private javax.swing.JComboBox generalPanningDistance;
    private javax.swing.JCheckBox generalPromptForIndex;
    private javax.swing.JCheckBox generalShowCursorCoordinates;
    private javax.swing.JCheckBox generalShowFileDialog;
    private javax.swing.JCheckBox generalUseGreekImages;
    private javax.swing.JCheckBox generalVerboseMode;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel60;
    private javax.swing.JLabel jLabel61;
    private javax.swing.JLabel jLabel62;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jobs;
    private javax.swing.JTextField maxUndoHistory;
    private javax.swing.JPanel memory;
    private javax.swing.JCheckBox sideBarOnRight;
    private javax.swing.JComboBox workingDirComboBox;
    // End of variables declaration//GEN-END:variables

}
