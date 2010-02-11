/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DRCTab.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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

import com.sun.electric.tool.drc.DRC;

import com.sun.electric.tool.user.dialogs.PreferencesFrame;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

/**
 * Class to handle the "DRC" tab of the Preferences dialog.
 */
public class DRCTab extends PreferencePanel
{
	/** Creates new form DRCTab */
	public DRCTab(PreferencesFrame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for the user preferences. */
    @Override
	public JPanel getUserPreferencesPanel() { return drc; }

	/** return the name of this preferences tab. */
    @Override
	public String getName() { return "DRC"; }

	private boolean requestedDRCClearDates;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the DRC tab.
	 */
    @Override
	public void init()
	{
        DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
		drcIncrementalOn.setSelected(dp.incrementalDRC);
		drcInteractiveDrag.setSelected(dp.interactiveDRCDrag);
        switch (dp.errorType)
        {
            case ERROR_CHECK_DEFAULT: drcErrorDefault.setSelected(true);   break;
			case ERROR_CHECK_CELL: drcErrorCell.setSelected(true);      break;
			case ERROR_CHECK_EXHAUSTIVE: drcErrorExaustive.setSelected(true);        break;
        }
        // Setting looging type
        loggingCombo.removeAllItems();
        for (DRC.DRCCheckLogging type : DRC.DRCCheckLogging.values())
             loggingCombo.addItem(type);
        loggingCombo.setSelectedItem(dp.errorLoggingType);

         // Setting minArea algorithm
        areaAlgoCombo.removeAllItems();
        for (DRC.DRCCheckMinArea type : DRC.DRCCheckMinArea.values())
             areaAlgoCombo.addItem(type);
        areaAlgoCombo.setSelectedItem(dp.minAreaAlgoOption);

        drcIgnoreCenterCuts.setSelected(dp.ignoreCenterCuts);

        // MinArea rules
		drcIgnoreArea.setSelected(dp.ignoreAreaCheck);

        // PolySelec rule
		drcIgnoreExtensionRules.setSelected(dp.ignoreExtensionRuleChecking);

        // DRC dates in memory stored
        drcDateOnCells.setSelected(!dp.storeDatesInMemory);

        // Interactive logging
        drcInteractive.setSelected(dp.interactiveLog);

		requestedDRCClearDates = false;
		drcClearValidDates.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				drcClearValidDates.setEnabled(false);
				requestedDRCClearDates = true;
			}
		});

        // Setting the multi-threaded option
        drcMultiDRC.setSelected(dp.isMultiThreaded);
    }

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the DRC tab.
	 */
    @Override
	public void term()
	{
        DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
		dp.incrementalDRC = drcIncrementalOn.isSelected();
		dp.interactiveDRCDrag = drcInteractiveDrag.isSelected();

		if (drcErrorDefault.isSelected())
            dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_DEFAULT;
        else if (drcErrorCell.isSelected())
            dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_CELL;
        else if (drcErrorExaustive.isSelected())
            dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;

        // Checking the logging type
        dp.errorLoggingType = (DRC.DRCCheckLogging)loggingCombo.getSelectedItem();
        // Checking the logging type
        dp.minAreaAlgoOption = (DRC.DRCCheckMinArea)areaAlgoCombo.getSelectedItem();
        // Checking center cuts
        dp.ignoreCenterCuts = drcIgnoreCenterCuts.isSelected();
        // For min area rules
        dp.ignoreAreaCheck = drcIgnoreArea.isSelected();
        // Poly Select rule
        dp.ignoreExtensionRuleChecking = drcIgnoreExtensionRules.isSelected();
        // DRC dates in memory
        dp.storeDatesInMemory = !drcDateOnCells.isSelected();
        // Interactive logging
        dp.interactiveLog = drcInteractive.isSelected();

		if (requestedDRCClearDates) DRC.resetDRCDates(true);

        // drcMultiDRC.setSelected(DRC.isMultiThreaded());
        // Setting MTDRC option
        dp.isMultiThreaded = drcMultiDRC.isSelected();
        putPrefs(dp);
        ClickZoomWireListener.theOne.readPrefs();
    }

	/**
	 * Method called when the factory reset is requested.
	 */
    @Override
	public void reset()
	{
        DRC.DRCPreferences factoryDp = new DRC.DRCPreferences(true);
        DRC.DRCPreferences dp = new DRC.DRCPreferences(false);

        dp.incrementalDRC = factoryDp.incrementalDRC;
        dp.interactiveDRCDrag = factoryDp.interactiveDRCDrag;
        dp.errorType = factoryDp.errorType;
        dp.errorLoggingType = factoryDp.errorLoggingType;
        dp.isMultiThreaded = factoryDp.isMultiThreaded;

		dp.storeDatesInMemory = factoryDp.storeDatesInMemory;
        dp.ignoreCenterCuts = factoryDp.ignoreCenterCuts;
        dp.ignoreAreaCheck = factoryDp.ignoreAreaCheck;
        dp.ignoreExtensionRuleChecking = factoryDp.ignoreExtensionRuleChecking;
        dp.interactiveLog = factoryDp.interactiveLog;
        dp.minAreaAlgoOption = factoryDp.minAreaAlgoOption;
        putPrefs(dp);
        ClickZoomWireListener.theOne.readPrefs();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        errorTypeGroup = new javax.swing.ButtonGroup();
        drc = new javax.swing.JPanel();
        IncrPanel = new javax.swing.JPanel();
        drcIncrementalOn = new javax.swing.JCheckBox();
        drcInteractiveDrag = new javax.swing.JCheckBox();
        HierPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        drcErrorExaustive = new javax.swing.JRadioButton();
        drcErrorDefault = new javax.swing.JRadioButton();
        drcErrorCell = new javax.swing.JRadioButton();
        loggingLabel = new javax.swing.JLabel();
        loggingCombo = new javax.swing.JComboBox();
        drcMultiDRC = new javax.swing.JCheckBox();
        BothPanel = new javax.swing.JPanel();
        drcIgnoreCenterCuts = new javax.swing.JCheckBox();
        drcIgnoreExtensionRules = new javax.swing.JCheckBox();
        drcIgnoreArea = new javax.swing.JCheckBox();
        drcDateOnCells = new javax.swing.JCheckBox();
        drcInteractive = new javax.swing.JCheckBox();
        drcClearValidDates = new javax.swing.JButton();
        areaAlgoLabel = new javax.swing.JLabel();
        areaAlgoCombo = new javax.swing.JComboBox();

        setTitle("Tool Options");
        setName(""); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        drc.setLayout(new java.awt.GridBagLayout());

        IncrPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Incremental DRC"));
        IncrPanel.setLayout(new java.awt.GridBagLayout());

        drcIncrementalOn.setText("On");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        IncrPanel.add(drcIncrementalOn, gridBagConstraints);

        drcInteractiveDrag.setText("Show worst violation while moving nodes and arcs");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        IncrPanel.add(drcInteractiveDrag, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        drc.add(IncrPanel, gridBagConstraints);

        HierPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Hierarchical DRC"));
        HierPanel.setLayout(new java.awt.GridBagLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Report Type"));
        jPanel1.setLayout(new java.awt.GridBagLayout());

        errorTypeGroup.add(drcErrorExaustive);
        drcErrorExaustive.setText("Report all errors");
        drcErrorExaustive.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        jPanel1.add(drcErrorExaustive, gridBagConstraints);

        errorTypeGroup.add(drcErrorDefault);
        drcErrorDefault.setSelected(true);
        drcErrorDefault.setText("Report just 1 error per pair of geometries");
        drcErrorDefault.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        jPanel1.add(drcErrorDefault, gridBagConstraints);

        errorTypeGroup.add(drcErrorCell);
        drcErrorCell.setText("Report just 1 error per cell");
        drcErrorCell.setAutoscrolls(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        jPanel1.add(drcErrorCell, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        HierPanel.add(jPanel1, gridBagConstraints);

        loggingLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        loggingLabel.setText("Report Errors: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        HierPanel.add(loggingLabel, gridBagConstraints);

        loggingCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        loggingCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loggingComboActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        HierPanel.add(loggingCombo, gridBagConstraints);

        drcMultiDRC.setText("Multi-threaded DRC");
        drcMultiDRC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                drcMultiDRCActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        HierPanel.add(drcMultiDRC, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        drc.add(HierPanel, gridBagConstraints);

        BothPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Incremental and Hierarchical"));
        BothPanel.setLayout(new java.awt.GridBagLayout());

        drcIgnoreCenterCuts.setText("Ignore center cuts in large contacts");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        BothPanel.add(drcIgnoreCenterCuts, gridBagConstraints);

        drcIgnoreExtensionRules.setText("Ignore extension rules");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        BothPanel.add(drcIgnoreExtensionRules, gridBagConstraints);

        drcIgnoreArea.setText("Ignore area checking");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        BothPanel.add(drcIgnoreArea, gridBagConstraints);

        drcDateOnCells.setText("Save valid DRC dates with cells");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        BothPanel.add(drcDateOnCells, gridBagConstraints);

        drcInteractive.setText("Interactive Logging");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        BothPanel.add(drcInteractive, gridBagConstraints);

        drcClearValidDates.setText("Clear valid DRC dates");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 30, 4, 4);
        BothPanel.add(drcClearValidDates, gridBagConstraints);

        areaAlgoLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        areaAlgoLabel.setText("MinArea Algorithm: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        BothPanel.add(areaAlgoLabel, gridBagConstraints);

        areaAlgoCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        areaAlgoCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                areaAlgoComboActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        BothPanel.add(areaAlgoCombo, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        drc.add(BothPanel, gridBagConstraints);

        getContentPane().add(drc, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void areaAlgoComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_areaAlgoComboActionPerformed
// TODO add your handling code here:
    }//GEN-LAST:event_areaAlgoComboActionPerformed

    private void loggingComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loggingComboActionPerformed
// TODO add your handling code here:
    }//GEN-LAST:event_loggingComboActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

        private void drcMultiDRCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drcMultiDRCActionPerformed
            // TODO add your handling code here:
}//GEN-LAST:event_drcMultiDRCActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel BothPanel;
    private javax.swing.JPanel HierPanel;
    private javax.swing.JPanel IncrPanel;
    private javax.swing.JComboBox areaAlgoCombo;
    private javax.swing.JLabel areaAlgoLabel;
    private javax.swing.JPanel drc;
    private javax.swing.JButton drcClearValidDates;
    private javax.swing.JCheckBox drcDateOnCells;
    private javax.swing.JRadioButton drcErrorCell;
    private javax.swing.JRadioButton drcErrorDefault;
    private javax.swing.JRadioButton drcErrorExaustive;
    private javax.swing.JCheckBox drcIgnoreArea;
    private javax.swing.JCheckBox drcIgnoreCenterCuts;
    private javax.swing.JCheckBox drcIgnoreExtensionRules;
    private javax.swing.JCheckBox drcIncrementalOn;
    private javax.swing.JCheckBox drcInteractive;
    private javax.swing.JCheckBox drcInteractiveDrag;
    private javax.swing.JCheckBox drcMultiDRC;
    private javax.swing.ButtonGroup errorTypeGroup;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JComboBox loggingCombo;
    private javax.swing.JLabel loggingLabel;
    // End of variables declaration//GEN-END:variables

}
