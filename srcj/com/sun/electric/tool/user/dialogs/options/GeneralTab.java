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

	public JPanel getPanel() { return general; }

	public String getName() { return "General"; }

	private boolean initialBeepAfterLongJobs;
	private boolean initialClickSounds;
	private boolean initialShowFileSelectionForNetlists;
	private boolean initialShowCursorCoordinates;
	private boolean initialIncludeDateAndVersion;
	private int initialPanningDistance;
	private int initialErrorLimit;
	private long initialMaxMem;
    private int initialMaxUndo;
    private String initialWorkingDirSetting;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the General tab.
	 */
	public void init()
	{
		initialBeepAfterLongJobs = User.isBeepAfterLongJobs();
		generalBeepAfterLongJobs.setSelected(initialBeepAfterLongJobs);

		initialShowFileSelectionForNetlists = User.isShowFileSelectionForNetlists();
		generalShowFileDialog.setSelected(initialShowFileSelectionForNetlists);

		initialShowCursorCoordinates = User.isShowCursorCoordinatesInStatus();
		generalShowCursorCoordinates.setSelected(initialShowCursorCoordinates);

		initialIncludeDateAndVersion = User.isIncludeDateAndVersionInOutput();
		generalIncludeDateAndVersion.setSelected(initialIncludeDateAndVersion);

        initialWorkingDirSetting = User.getInitialWorkingDirectorySetting();
        for (Iterator it = User.getInitialWorkingDirectorySettings(); it.hasNext(); )
            workingDirComboBox.addItem(it.next());

		generalPanningDistance.addItem("Small");
		generalPanningDistance.addItem("Medium");
		generalPanningDistance.addItem("Large");
		initialPanningDistance = User.getPanningDistance();
		generalPanningDistance.setSelectedIndex(initialPanningDistance);

		initialErrorLimit = User.getErrorLimit();
		generalErrorLimit.setText(Integer.toString(initialErrorLimit));

        initialMaxUndo = User.getMaxUndoHistory();
        maxUndoHistory.setText(Integer.toString(initialMaxUndo));

		java.lang.Runtime runtime = java.lang.Runtime.getRuntime();
		long maxMemLimit = runtime.maxMemory() / 1024 / 1024;
		generalMemoryUsage.setText("Current memory usage: " + Long.toString(maxMemLimit) + " megabytes");
		initialMaxMem = User.getMemorySize();
		generalMaxMem.setText(Long.toString(initialMaxMem));
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the General tab.
	 */
	public void term()
	{
		boolean curentBeepAfterLongJobs = generalBeepAfterLongJobs.isSelected();
		if (curentBeepAfterLongJobs != initialBeepAfterLongJobs)
			User.setBeepAfterLongJobs(curentBeepAfterLongJobs);

		boolean curentShowFileSelectionForNetlists = generalShowFileDialog.isSelected();
		if (curentShowFileSelectionForNetlists != initialShowFileSelectionForNetlists)
			User.setShowFileSelectionForNetlists(curentShowFileSelectionForNetlists);

		boolean currentShowCursorCoordinates = generalShowCursorCoordinates.isSelected();
		if (currentShowCursorCoordinates != initialShowCursorCoordinates)
			User.setShowCursorCoordinatesInStatus(currentShowCursorCoordinates);

		boolean curentIncludeDateAndVersion = generalIncludeDateAndVersion.isSelected();
		if (curentIncludeDateAndVersion != initialIncludeDateAndVersion)
			User.setIncludeDateAndVersionInOutput(curentIncludeDateAndVersion);

        String currentInitialWorkingDirSetting = (String)workingDirComboBox.getSelectedItem();
        if (!currentInitialWorkingDirSetting.equals(initialWorkingDirSetting))
            User.setInitialWorkingDirectorySetting(currentInitialWorkingDirSetting);

		int currentPanningDistance = generalPanningDistance.getSelectedIndex();
		if (currentPanningDistance != initialPanningDistance)
			User.setPanningDistance(currentPanningDistance);

		int curentErrorLimit = TextUtils.atoi(generalErrorLimit.getText());
		if (curentErrorLimit != initialErrorLimit)
			User.setErrorLimit(curentErrorLimit);

		int currentMaxMem = TextUtils.atoi(generalMaxMem.getText());
		if (currentMaxMem != initialMaxMem)
			User.setMemorySize(currentMaxMem);

        int currentMaxUndo = TextUtils.atoi(maxUndoHistory.getText());
        if (currentMaxUndo != initialMaxUndo) {
            User.setMaxUndoHistory(currentMaxUndo);
            Undo.setHistoryListSize(currentMaxUndo);
        }
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        general = new javax.swing.JPanel();
        generalBeepAfterLongJobs = new javax.swing.JCheckBox();
        generalIncludeDateAndVersion = new javax.swing.JCheckBox();
        generalShowFileDialog = new javax.swing.JCheckBox();
        jLabel46 = new javax.swing.JLabel();
        generalErrorLimit = new javax.swing.JTextField();
        jLabel53 = new javax.swing.JLabel();
        jPanel11 = new javax.swing.JPanel();
        jLabel60 = new javax.swing.JLabel();
        generalMaxMem = new javax.swing.JTextField();
        jLabel61 = new javax.swing.JLabel();
        generalMemoryUsage = new javax.swing.JLabel();
        jLabel62 = new javax.swing.JLabel();
        generalShowCursorCoordinates = new javax.swing.JCheckBox();
        generalPanningDistance = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        maxUndoHistory = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        workingDirComboBox = new javax.swing.JComboBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Edit Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        general.setLayout(new java.awt.GridBagLayout());

        generalBeepAfterLongJobs.setText("Beep after long jobs");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        general.add(generalBeepAfterLongJobs, gridBagConstraints);

        generalIncludeDateAndVersion.setText("Include date and version in output files");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        general.add(generalIncludeDateAndVersion, gridBagConstraints);

        generalShowFileDialog.setText("Show file-selection dialog before writing netlists");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        general.add(generalShowFileDialog, gridBagConstraints);

        jLabel46.setText("Maximum errors to report:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        general.add(jLabel46, gridBagConstraints);

        generalErrorLimit.setColumns(6);
        generalErrorLimit.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        general.add(generalErrorLimit, gridBagConstraints);

        jLabel53.setText("(0 for infinite)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        general.add(jLabel53, gridBagConstraints);

        jPanel11.setLayout(new java.awt.GridBagLayout());

        jPanel11.setBorder(new javax.swing.border.TitledBorder("Memory"));
        jLabel60.setText("Maximum memory:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel11.add(jLabel60, gridBagConstraints);

        generalMaxMem.setColumns(6);
        generalMaxMem.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel11.add(generalMaxMem, gridBagConstraints);

        jLabel61.setText("megabytes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel11.add(jLabel61, gridBagConstraints);

        generalMemoryUsage.setText("Current memory usage:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel11.add(generalMemoryUsage, gridBagConstraints);

        jLabel62.setText("Changes to memory take effect when Electric is next run");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel11.add(jLabel62, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        general.add(jPanel11, gridBagConstraints);

        generalShowCursorCoordinates.setText("Show cursor coordinates in status bar");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        general.add(generalShowCursorCoordinates, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        general.add(generalPanningDistance, gridBagConstraints);

        jLabel1.setText("Panning distance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        general.add(jLabel1, gridBagConstraints);

        jLabel2.setText("Maximum undo history");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        general.add(jLabel2, gridBagConstraints);

        maxUndoHistory.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        general.add(maxUndoHistory, gridBagConstraints);

        jLabel3.setText("Working directory:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        general.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        general.add(workingDirComboBox, gridBagConstraints);

        getContentPane().add(general, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel general;
    private javax.swing.JCheckBox generalBeepAfterLongJobs;
    private javax.swing.JTextField generalErrorLimit;
    private javax.swing.JCheckBox generalIncludeDateAndVersion;
    private javax.swing.JTextField generalMaxMem;
    private javax.swing.JLabel generalMemoryUsage;
    private javax.swing.JComboBox generalPanningDistance;
    private javax.swing.JCheckBox generalShowCursorCoordinates;
    private javax.swing.JCheckBox generalShowFileDialog;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel60;
    private javax.swing.JLabel jLabel61;
    private javax.swing.JLabel jLabel62;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JTextField maxUndoHistory;
    private javax.swing.JComboBox workingDirComboBox;
    // End of variables declaration//GEN-END:variables
	
}
