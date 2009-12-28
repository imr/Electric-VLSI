/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetworkTab.java
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

import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.extract.Extract;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.PreferencesFrame;

import javax.swing.JPanel;

/**
 * Class to handle the "Network" tab of the Preferences dialog.
 */
public class NetworkTab extends PreferencePanel
{
    private Setting ignoreResistorsSetting = NetworkTool.getIgnoreResistorsSetting();
    private Setting includeDateAndVersionInOutputSetting = User.getIncludeDateAndVersionInOutputSetting();

    /** Creates new form NetworkTab */
	public NetworkTab(PreferencesFrame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// make all text fields select-all when entered
	    EDialog.makeTextFieldSelectAllOnTab(extractSmallestPolygonSize);
	    EDialog.makeTextFieldSelectAllOnTab(extractCellPattern);
	}

	/** return the JPanel to use for the user preferences. */
	public JPanel getUserPreferencesPanel() { return preferences; }

	/** return the JPanel to use for the project preferences. */
	public JPanel getProjectPreferencesPanel() { return projectSettings; }

	/** return the name of this preferences tab. */
	public String getName() { return "Network"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Network tab.
	 */
	public void init()
	{
		// network preferences
		if (NetworkTool.isBusAscending()) netAscending.setSelected(true); else
			netDescending.setSelected(true);

		// node extraction preferences
		extractGridAlign.setSelected(Extract.isGridAlignExtraction());
		switch (Extract.getActiveHandling())
		{
			case 0: extractNeedProperActive.setSelected(true);   break;
			case 1: extractUnifyNandP.setSelected(true);         break;
			case 2: extractIgnoreWellSelect.setSelected(true);   break;
		}
		extractApproximateCuts.setSelected(Extract.isApproximateCuts());
		extractIgnoreTiny.setSelected(Extract.isIgnoreTinyPolygons());
		extractSmallestPolygonSize.setText(Double.toString(Extract.getSmallestPolygonSize()));
		extractCellPattern.setText(Extract.getCellExpandPattern());
		extractFlattenPCells.setSelected(Extract.isFlattenPcells());
		extractPureLayer.setSelected(Extract.isUsePureLayerNodes());

		// project preferences
		netIgnoreResistors.setSelected(getBoolean(ignoreResistorsSetting));
		generalIncludeDateAndVersion.setSelected(getBoolean(includeDateAndVersionInOutputSetting));
	}

	public void term()
	{
		// user preferences
		boolean nowBoolean = netAscending.isSelected();
		if (NetworkTool.isBusAscending() != nowBoolean) NetworkTool.setBusAscending(nowBoolean);

		nowBoolean = extractGridAlign.isSelected();
		if (Extract.isGridAlignExtraction() != nowBoolean) Extract.setGridAlignExtraction(nowBoolean);

		int nowInt = 0;
		if (extractUnifyNandP.isSelected()) nowInt = 1; else
			if (extractIgnoreWellSelect.isSelected()) nowInt = 2;
		if (Extract.getActiveHandling() != nowInt) Extract.setActiveHandling(nowInt);

		nowBoolean = extractApproximateCuts.isSelected();
		if (Extract.isApproximateCuts() != nowBoolean) Extract.setApproximateCuts(nowBoolean);

		nowBoolean = extractIgnoreTiny.isSelected();
		if (Extract.isIgnoreTinyPolygons() != nowBoolean) Extract.setIgnoreTinyPolygons(nowBoolean);

		double nowDouble = TextUtils.atof(extractSmallestPolygonSize.getText());
		if (nowDouble != Extract.getSmallestPolygonSize())
			Extract.setSmallestPolygonSize(nowDouble);

		String nowString = extractCellPattern.getText();
		if (!Extract.getCellExpandPattern().equals(nowString)) Extract.setCellExpandPattern(nowString);

		nowBoolean = extractFlattenPCells.isSelected();
		if (Extract.isFlattenPcells() != nowBoolean) Extract.setFlattenPcells(nowBoolean);

		nowBoolean = extractPureLayer.isSelected();
		if (Extract.isUsePureLayerNodes() != nowBoolean) Extract.setUsePureLayerNodes(nowBoolean);

		// project preferences
        setBoolean(ignoreResistorsSetting, netIgnoreResistors.isSelected());
        setBoolean(includeDateAndVersionInOutputSetting, generalIncludeDateAndVersion.isSelected());
	}

	/**
	 * Method called when the factory reset is requested.
	 */
	public void reset()
	{
		if (NetworkTool.isFactoryBusAscending() != NetworkTool.isBusAscending())
			NetworkTool.setBusAscending(NetworkTool.isFactoryBusAscending());

		// node extraction
		if (Extract.isFactoryGridAlignExtraction() != Extract.isGridAlignExtraction())
			Extract.setGridAlignExtraction(Extract.isFactoryGridAlignExtraction());
		if (Extract.isFactoryApproximateCuts() != Extract.isApproximateCuts())
			Extract.setApproximateCuts(Extract.isFactoryApproximateCuts());
		if (Extract.getFactoryActiveHandling() != Extract.getActiveHandling())
			Extract.setActiveHandling(Extract.getFactoryActiveHandling());
		if (Extract.isFactoryIgnoreTinyPolygons() != Extract.isIgnoreTinyPolygons())
			Extract.setIgnoreTinyPolygons(Extract.isFactoryIgnoreTinyPolygons());
		if (Extract.getFactorySmallestPolygonSize() != Extract.getSmallestPolygonSize())
			Extract.setSmallestPolygonSize(Extract.getFactorySmallestPolygonSize());
		if (!Extract.getFactoryCellExpandPattern().equals(Extract.getCellExpandPattern()))
			Extract.setCellExpandPattern(Extract.getFactoryCellExpandPattern());
		if (Extract.isFactoryFlattenPcells() != Extract.isFlattenPcells())
			Extract.setFlattenPcells(Extract.isFactoryFlattenPcells());
		if (Extract.isFactoryUsePureLayerNodes() != Extract.isUsePureLayerNodes())
			Extract.setUsePureLayerNodes(Extract.isFactoryUsePureLayerNodes());
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        netDefaultOrder = new javax.swing.ButtonGroup();
        activeHandling = new javax.swing.ButtonGroup();
        preferences = new javax.swing.JPanel();
        netOrderingLabel = new javax.swing.JLabel();
        netAscending = new javax.swing.JRadioButton();
        netDescending = new javax.swing.JRadioButton();
        nodeExtractionPreferences = new javax.swing.JPanel();
        extractGridAlign = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        extractCellPattern = new javax.swing.JTextField();
        extractSmallestPolygonSize = new javax.swing.JTextField();
        extractApproximateCuts = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        extractNeedProperActive = new javax.swing.JRadioButton();
        extractUnifyNandP = new javax.swing.JRadioButton();
        extractIgnoreWellSelect = new javax.swing.JRadioButton();
        extractIgnoreTiny = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        extractFlattenPCells = new javax.swing.JCheckBox();
        extractPureLayer = new javax.swing.JCheckBox();
        projectSettings = new javax.swing.JPanel();
        generalIncludeDateAndVersion = new javax.swing.JCheckBox();
        netIgnoreResistors = new javax.swing.JCheckBox();

        setTitle("Tool Options");
        setName(""); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        preferences.setLayout(new java.awt.GridBagLayout());

        netOrderingLabel.setText("Default bus order:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        preferences.add(netOrderingLabel, gridBagConstraints);

        netDefaultOrder.add(netAscending);
        netAscending.setText("Ascending (0:N)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        preferences.add(netAscending, gridBagConstraints);

        netDefaultOrder.add(netDescending);
        netDescending.setText("Descending (N:0)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        preferences.add(netDescending, gridBagConstraints);

        nodeExtractionPreferences.setBorder(javax.swing.BorderFactory.createTitledBorder("Node Extraction"));
        nodeExtractionPreferences.setLayout(new java.awt.GridBagLayout());

        extractGridAlign.setText("Grid-align geometry before extraction");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        nodeExtractionPreferences.add(extractGridAlign, gridBagConstraints);

        jLabel1.setText("Flatten cells whose names match this:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        nodeExtractionPreferences.add(jLabel1, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        nodeExtractionPreferences.add(extractCellPattern, gridBagConstraints);

        extractSmallestPolygonSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 1, 2, 1);
        nodeExtractionPreferences.add(extractSmallestPolygonSize, gridBagConstraints);

        extractApproximateCuts.setText("Approximate cut placement");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        nodeExtractionPreferences.add(extractApproximateCuts, gridBagConstraints);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Active Handling", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        jPanel2.setLayout(new java.awt.GridBagLayout());

        activeHandling.add(extractNeedProperActive);
        extractNeedProperActive.setText("Require separate N and P active; require proper select/well");
        extractNeedProperActive.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 1, 2);
        jPanel2.add(extractNeedProperActive, gridBagConstraints);

        activeHandling.add(extractUnifyNandP);
        extractUnifyNandP.setText("Ignore N vs. P active; require proper select/well");
        extractUnifyNandP.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        extractUnifyNandP.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 1, 2);
        jPanel2.add(extractUnifyNandP, gridBagConstraints);

        activeHandling.add(extractIgnoreWellSelect);
        extractIgnoreWellSelect.setText("Require separate N and P active; ignore select/well");
        extractIgnoreWellSelect.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        extractIgnoreWellSelect.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 1, 2);
        jPanel2.add(extractIgnoreWellSelect, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 4, 2);
        nodeExtractionPreferences.add(jPanel2, gridBagConstraints);

        extractIgnoreTiny.setText("Ignore polygons smaller than:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 1);
        nodeExtractionPreferences.add(extractIgnoreTiny, gridBagConstraints);

        jLabel2.setText("square units");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 1, 2, 4);
        nodeExtractionPreferences.add(jLabel2, gridBagConstraints);

        extractFlattenPCells.setText("Flatten Cadence Pcells (with $$number at end of name)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        nodeExtractionPreferences.add(extractFlattenPCells, gridBagConstraints);

        extractPureLayer.setText("Use pure-layer nodes for connectivity");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        nodeExtractionPreferences.add(extractPureLayer, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        preferences.add(nodeExtractionPreferences, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(preferences, gridBagConstraints);

        projectSettings.setLayout(new java.awt.GridBagLayout());

        generalIncludeDateAndVersion.setText("Include date and version in output files");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        projectSettings.add(generalIncludeDateAndVersion, gridBagConstraints);

        netIgnoreResistors.setText("Ignore Resistors when building netlists");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        projectSettings.add(netIgnoreResistors, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        getContentPane().add(projectSettings, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup activeHandling;
    private javax.swing.JCheckBox extractApproximateCuts;
    private javax.swing.JTextField extractCellPattern;
    private javax.swing.JCheckBox extractFlattenPCells;
    private javax.swing.JCheckBox extractGridAlign;
    private javax.swing.JCheckBox extractIgnoreTiny;
    private javax.swing.JRadioButton extractIgnoreWellSelect;
    private javax.swing.JRadioButton extractNeedProperActive;
    private javax.swing.JCheckBox extractPureLayer;
    private javax.swing.JTextField extractSmallestPolygonSize;
    private javax.swing.JRadioButton extractUnifyNandP;
    private javax.swing.JCheckBox generalIncludeDateAndVersion;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JRadioButton netAscending;
    private javax.swing.ButtonGroup netDefaultOrder;
    private javax.swing.JRadioButton netDescending;
    private javax.swing.JCheckBox netIgnoreResistors;
    private javax.swing.JLabel netOrderingLabel;
    private javax.swing.JPanel nodeExtractionPreferences;
    private javax.swing.JPanel preferences;
    private javax.swing.JPanel projectSettings;
    // End of variables declaration//GEN-END:variables

}
