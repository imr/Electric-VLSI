/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SunRouterTab.java
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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.routing.Routing;

import java.awt.Frame;

import javax.swing.JPanel;

/**
 * Class to handle the "SunRouter" tab of the Preferences dialog.
 */
public class SunRouterTab extends PreferencePanel
{
	/** Creates new form SunRouterTab */
	public SunRouterTab(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return sunRouter; }

	/** return the name of this preferences tab. */
	public String getName() { return "Sun Router"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Skill tab.
	 */
	public void init()
	{
		if (!Routing.hasSunRouter())
		{
			// global routing
		    horizTileSize.setEnabled(false);
		    vertTileSize.setEnabled(false);
		    horizBitSize.setEnabled(false);
		    vertBitSize.setEnabled(false);
		    cutLineDeviation.setEnabled(false);
		    verbosityCombo.setEnabled(false);

		    // capacity
		    pinFactor.setEnabled(false);
		    oneTileFactor.setEnabled(false);
		    windowSize.setEnabled(false);

		    // ripup
		    delta.setEnabled(false);
		    overloadLimit.setEnabled(false);
		    costLimit.setEnabled(false);

		    // layer assignment
		    tilesPerLongNet.setEnabled(false);
		    tilesPerMedNet.setEnabled(false);
		    layerAssignCapF.setEnabled(false);
		    longNetLength.setEnabled(false);
		    medNetLength.setEnabled(false);
		    unassignedPinDensityF.setEnabled(false);

		    // detail routing
		    wireOffset.setEnabled(false);
		    wireModulo.setEnabled(false);
		    wireBlockageFactor.setEnabled(false);
		    ripupMaximum.setEnabled(false);
		    ripupPenalty.setEnabled(false);
		    ripupExpansion.setEnabled(false);
		    zRipupExpansion.setEnabled(false);
		    ripupSearches.setEnabled(false);
		    globalPathExpansion.setEnabled(false);
		    sourceAccessExpansion.setEnabled(false);
		    sinkAccessExpansion.setEnabled(false);
		    denseViaAreaSize.setEnabled(false);
		    retryExpandRouting.setEnabled(false);
		    retryDenseViaAreaSize.setEnabled(false);
		    pathSearchControl.setEnabled(false);
		    sparseViaModulo.setEnabled(false);
		    lowPathSearchCost.setEnabled(false);
		    mediumPathSearchCost.setEnabled(false);
		    highPathSearchCost.setEnabled(false);
		    takenPathSearchCost.setEnabled(false);
		} else
		{
			// global routing
		    horizTileSize.setText("" + Routing.getSunRouterXTileSize());
		    vertTileSize.setText("" + Routing.getSunRouterYTileSize());
		    horizBitSize.setText("" + Routing.getSunRouterXBitSize());
		    vertBitSize.setText("" + Routing.getSunRouterYBitSize());
		    cutLineDeviation.setText("" + Routing.getSunRouterCutlineDeviation());
		    verbosityCombo.setSelectedIndex(Routing.getSunRouterVerboseLevel());

		    // capacity
		    pinFactor.setText("" + Routing.getSunRouterPinFactor());
		    oneTileFactor.setText("" + Routing.getSunRouterOneTileFactor());
		    windowSize.setText("" + Routing.getSunRouterWindow());

		    // ripup
		    delta.setText("" + Routing.getSunRouterDelta());
		    overloadLimit.setText("" + Routing.getSunRouterOverloadLimit());
		    costLimit.setText("" + Routing.getSunRouterCostLimit());

		    // layer assignment
		    tilesPerLongNet.setText("" + Routing.getSunRouterTilesPerPinLongNet());
		    tilesPerMedNet.setText("" + Routing.getSunRouterTilesPerPinMedNet());
		    layerAssignCapF.setText("" + Routing.getSunRouterLayerAssgnCapF());
		    longNetLength.setText("" + Routing.getSunRouterLengthLongNet());
		    medNetLength.setText("" + Routing.getSunRouterLengthMedNet());
		    unassignedPinDensityF.setText("" + Routing.getSunRouterUPinDensityF());

		    // detail routing
		    wireOffset.setText("" + Routing.getSunRouterWireOffset());
		    wireModulo.setText("" + Routing.getSunRouterWireModulo());
		    wireBlockageFactor.setText("" + Routing.getSunRouterWireBlockageFactor());
		    ripupMaximum.setText("" + Routing.getSunRouterRipUpMaximum());
		    ripupPenalty.setText("" + Routing.getSunRouterRipUpPenalty());
		    ripupExpansion.setText("" + Routing.getSunRouterRipUpExpansion());
		    zRipupExpansion.setText("" + Routing.getSunRouterZRipUpExpansion());
		    ripupSearches.setText("" + Routing.getSunRouterRipUpSearches());
		    globalPathExpansion.setText("" + Routing.getSunRouterGlobalPathExpansion());
		    sourceAccessExpansion.setText("" + Routing.getSunRouterSourceAccessExpansion());
		    sinkAccessExpansion.setText("" + Routing.getSunRouterSinkAccessExpansion());
		    denseViaAreaSize.setText("" + Routing.getSunRouterDenseViaAreaSize());
		    retryExpandRouting.setText("" + Routing.getSunRouterRetryExpandRouting());
		    retryDenseViaAreaSize.setText("" + Routing.getSunRouterRetryDenseViaAreaSize());
		    pathSearchControl.setText("" + Routing.getSunRouterPathSearchControl());
		    sparseViaModulo.setText("" + Routing.getSunRouterSparseViaModulo());
		    lowPathSearchCost.setText("" + Routing.getSunRouterLowPathSearchCost());
		    mediumPathSearchCost.setText("" + Routing.getSunRouterMediumPathSearchCost());
		    highPathSearchCost.setText("" + Routing.getSunRouterHighPathSearchCost());
		    takenPathSearchCost.setText("" + Routing.getSunRouterTakenPathSearchCost());
		}
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Skill tab.
	 */
	public void term()
	{
		double newDValue;
		int newIValue;

		// global routing
		newIValue = TextUtils.atoi(horizTileSize.getText());
		if (newIValue != Routing.getSunRouterXTileSize()) Routing.setSunRouterXTileSize(newIValue);
		newIValue = TextUtils.atoi(vertTileSize.getText());
		if (newIValue != Routing.getSunRouterYTileSize()) Routing.setSunRouterYTileSize(newIValue);
		newIValue = TextUtils.atoi(horizBitSize.getText());
		if (newIValue != Routing.getSunRouterXBitSize()) Routing.setSunRouterXBitSize(newIValue);
		newIValue = TextUtils.atoi(vertBitSize.getText());
		if (newIValue != Routing.getSunRouterYBitSize()) Routing.setSunRouterYBitSize(newIValue);
		newDValue = TextUtils.atof(cutLineDeviation.getText());
		if (newDValue != Routing.getSunRouterCutlineDeviation()) Routing.setSunRouterCutlineDeviation(newDValue);
		newIValue = verbosityCombo.getSelectedIndex();
		if (newIValue != Routing.getSunRouterVerboseLevel()) Routing.setSunRouterVerboseLevel(newIValue);		

		// capacity
		newIValue = TextUtils.atoi(pinFactor.getText());
		if (newIValue != Routing.getSunRouterPinFactor()) Routing.setSunRouterPinFactor(newIValue);
		newDValue = TextUtils.atof(oneTileFactor.getText());
		if (newDValue != Routing.getSunRouterOneTileFactor()) Routing.setSunRouterOneTileFactor(newDValue);
		newIValue = TextUtils.atoi(windowSize.getText());
		if (newIValue != Routing.getSunRouterWindow()) Routing.setSunRouterWindow(newIValue);

		// ripup
		newDValue = TextUtils.atof(delta.getText());
		if (newDValue != Routing.getSunRouterDelta()) Routing.setSunRouterDelta(newDValue);
		newIValue = TextUtils.atoi(overloadLimit.getText());
		if (newIValue != Routing.getSunRouterOneTileFactor()) Routing.setSunRouterOverloadLimit(newIValue);
		newDValue = TextUtils.atof(costLimit.getText());
		if (newDValue != Routing.getSunRouterCostLimit()) Routing.setSunRouterCostLimit(newDValue);

		// layer assignment
		newDValue = TextUtils.atof(tilesPerLongNet.getText());
		if (newDValue != Routing.getSunRouterTilesPerPinLongNet()) Routing.setSunRouterTilesPerPinLongNet(newDValue);
		newDValue = TextUtils.atof(tilesPerMedNet.getText());
		if (newDValue != Routing.getSunRouterTilesPerPinMedNet()) Routing.setSunRouterTilesPerPinMedNet(newDValue);
		newDValue = TextUtils.atof(layerAssignCapF.getText());
		if (newDValue != Routing.getSunRouterLayerAssgnCapF()) Routing.setSunRouterLayerAssgnCapF(newDValue);
		newDValue = TextUtils.atof(longNetLength.getText());
		if (newDValue != Routing.getSunRouterLengthLongNet()) Routing.setSunRouterLengthLongNet(newDValue);
		newDValue = TextUtils.atof(medNetLength.getText());
		if (newDValue != Routing.getSunRouterLengthMedNet()) Routing.setSunRouterLengthMedNet(newDValue);
		newIValue = TextUtils.atoi(overloadLimit.getText());
		if (newIValue != Routing.getSunRouterOverloadLimit()) Routing.setSunRouterOverloadLimit(newIValue);
		newDValue = TextUtils.atof(unassignedPinDensityF.getText());
		if (newDValue != Routing.getSunRouterUPinDensityF()) Routing.setSunRouterUPinDensityF(newDValue);

		// detail routing
		newIValue = TextUtils.atoi(wireOffset.getText());
		if (newIValue != Routing.getSunRouterWireOffset()) Routing.setSunRouterWireOffset(newIValue);
		newIValue = TextUtils.atoi(wireModulo.getText());
		if (newIValue != Routing.getSunRouterWireModulo()) Routing.setSunRouterWireModulo(newIValue);
		newDValue = TextUtils.atof(wireBlockageFactor.getText());
		if (newDValue != Routing.getSunRouterWireBlockageFactor()) Routing.setSunRouterWireBlockageFactor(newDValue);
		newIValue = TextUtils.atoi(ripupMaximum.getText());
		if (newIValue != Routing.getSunRouterRipUpMaximum()) Routing.setSunRouterRipUpMaximum(newIValue);
		newIValue = TextUtils.atoi(ripupPenalty.getText());
		if (newIValue != Routing.getSunRouterRipUpPenalty()) Routing.setSunRouterRipUpPenalty(newIValue);
		newIValue = TextUtils.atoi(ripupExpansion.getText());
		if (newIValue != Routing.getSunRouterRipUpExpansion()) Routing.setSunRouterRipUpExpansion(newIValue);
		newIValue = TextUtils.atoi(zRipupExpansion.getText());
		if (newIValue != Routing.getSunRouterZRipUpExpansion()) Routing.setSunRouterZRipUpExpansion(newIValue);
		newIValue = TextUtils.atoi(ripupSearches.getText());
		if (newIValue != Routing.getSunRouterRipUpSearches()) Routing.setSunRouterRipUpSearches(newIValue);
		newIValue = TextUtils.atoi(globalPathExpansion.getText());
		if (newIValue != Routing.getSunRouterGlobalPathExpansion()) Routing.setSunRouterGlobalPathExpansion(newIValue);
		newIValue = TextUtils.atoi(sourceAccessExpansion.getText());
		if (newIValue != Routing.getSunRouterSourceAccessExpansion()) Routing.setSunRouterSourceAccessExpansion(newIValue);
		newIValue = TextUtils.atoi(sinkAccessExpansion.getText());
		if (newIValue != Routing.getSunRouterSinkAccessExpansion()) Routing.setSunRouterSinkAccessExpansion(newIValue);
		newIValue = TextUtils.atoi(denseViaAreaSize.getText());
		if (newIValue != Routing.getSunRouterDenseViaAreaSize()) Routing.setSunRouterDenseViaAreaSize(newIValue);
		newIValue = TextUtils.atoi(retryExpandRouting.getText());
		if (newIValue != Routing.getSunRouterRetryExpandRouting()) Routing.setSunRouterRetryExpandRouting(newIValue);
		newIValue = TextUtils.atoi(retryDenseViaAreaSize.getText());
		if (newIValue != Routing.getSunRouterRetryDenseViaAreaSize()) Routing.setSunRouterRetryDenseViaAreaSize(newIValue);
		newIValue = TextUtils.atoi(pathSearchControl.getText());
		if (newIValue != Routing.getSunRouterPathSearchControl()) Routing.setSunRouterPathSearchControl(newIValue);
		newIValue = TextUtils.atoi(sparseViaModulo.getText());
		if (newIValue != Routing.getSunRouterSparseViaModulo()) Routing.setSunRouterSparseViaModulo(newIValue);
		newIValue = TextUtils.atoi(lowPathSearchCost.getText());
		if (newIValue != Routing.getSunRouterLowPathSearchCost()) Routing.setSunRouterLowPathSearchCost(newIValue);
		newIValue = TextUtils.atoi(mediumPathSearchCost.getText());
		if (newIValue != Routing.getSunRouterMediumPathSearchCost()) Routing.setSunRouterMediumPathSearchCost(newIValue);
		newIValue = TextUtils.atoi(highPathSearchCost.getText());
		if (newIValue != Routing.getSunRouterHighPathSearchCost()) Routing.setSunRouterHighPathSearchCost(newIValue);
		newIValue = TextUtils.atoi(takenPathSearchCost.getText());
		if (newIValue != Routing.getSunRouterTakenPathSearchCost()) Routing.setSunRouterTakenPathSearchCost(newIValue);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        sunRouter = new javax.swing.JPanel();
        globalPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        horizTileSize = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        vertTileSize = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        horizBitSize = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        vertBitSize = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        cutLineDeviation = new javax.swing.JTextField();
        verbosityCombo = new javax.swing.JComboBox();
        jLabel18 = new javax.swing.JLabel();
        capacityPanel = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        pinFactor = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        oneTileFactor = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        windowSize = new javax.swing.JTextField();
        ripUpPanel = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        delta = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        overloadLimit = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        costLimit = new javax.swing.JTextField();
        layerAssignPanel = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        layerAssignCapF = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        unassignedPinDensityF = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        tilesPerLongNet = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        tilesPerMedNet = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        longNetLength = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        medNetLength = new javax.swing.JTextField();
        detailPanel = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        wireOffset = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        wireModulo = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        wireBlockageFactor = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        ripupMaximum = new javax.swing.JTextField();
        jLabel23 = new javax.swing.JLabel();
        ripupPenalty = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();
        ripupExpansion = new javax.swing.JTextField();
        jLabel25 = new javax.swing.JLabel();
        zRipupExpansion = new javax.swing.JTextField();
        jLabel26 = new javax.swing.JLabel();
        ripupSearches = new javax.swing.JTextField();
        jLabel27 = new javax.swing.JLabel();
        globalPathExpansion = new javax.swing.JTextField();
        jLabel28 = new javax.swing.JLabel();
        sourceAccessExpansion = new javax.swing.JTextField();
        jLabel29 = new javax.swing.JLabel();
        sinkAccessExpansion = new javax.swing.JTextField();
        jLabel30 = new javax.swing.JLabel();
        denseViaAreaSize = new javax.swing.JTextField();
        jLabel31 = new javax.swing.JLabel();
        retryExpandRouting = new javax.swing.JTextField();
        jLabel32 = new javax.swing.JLabel();
        retryDenseViaAreaSize = new javax.swing.JTextField();
        jLabel33 = new javax.swing.JLabel();
        pathSearchControl = new javax.swing.JTextField();
        jLabel34 = new javax.swing.JLabel();
        sparseViaModulo = new javax.swing.JTextField();
        jLabel35 = new javax.swing.JLabel();
        lowPathSearchCost = new javax.swing.JTextField();
        jLabel36 = new javax.swing.JLabel();
        mediumPathSearchCost = new javax.swing.JTextField();
        jLabel37 = new javax.swing.JLabel();
        highPathSearchCost = new javax.swing.JTextField();
        jLabel38 = new javax.swing.JLabel();
        takenPathSearchCost = new javax.swing.JTextField();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("IO Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        sunRouter.setLayout(new java.awt.GridBagLayout());

        globalPanel.setLayout(new java.awt.GridBagLayout());

        globalPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Global Routing Parameters"));
        jLabel1.setText("Horizontal tile size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        globalPanel.add(jLabel1, gridBagConstraints);

        horizTileSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        globalPanel.add(horizTileSize, gridBagConstraints);

        jLabel2.setText("Vertical tile size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        globalPanel.add(jLabel2, gridBagConstraints);

        vertTileSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        globalPanel.add(vertTileSize, gridBagConstraints);

        jLabel3.setText("Horizontal bit size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        globalPanel.add(jLabel3, gridBagConstraints);

        horizBitSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        globalPanel.add(horizBitSize, gridBagConstraints);

        jLabel4.setText("Vertical bit size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        globalPanel.add(jLabel4, gridBagConstraints);

        vertBitSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        globalPanel.add(vertBitSize, gridBagConstraints);

        jLabel13.setText("Cut line deviation:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        globalPanel.add(jLabel13, gridBagConstraints);

        cutLineDeviation.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        globalPanel.add(cutLineDeviation, gridBagConstraints);

        verbosityCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Silent", "Quiet", "Normal", "Verbose" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        globalPanel.add(verbosityCombo, gridBagConstraints);

        jLabel18.setText("Verbosity:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        globalPanel.add(jLabel18, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        sunRouter.add(globalPanel, gridBagConstraints);

        capacityPanel.setLayout(new java.awt.GridBagLayout());

        capacityPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Capacity Calculations"));
        jLabel11.setText("Pin Factor");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        capacityPanel.add(jLabel11, gridBagConstraints);

        pinFactor.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        capacityPanel.add(pinFactor, gridBagConstraints);

        jLabel12.setText("One-tile factor:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        capacityPanel.add(jLabel12, gridBagConstraints);

        oneTileFactor.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        capacityPanel.add(oneTileFactor, gridBagConstraints);

        jLabel5.setText("Window size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        capacityPanel.add(jLabel5, gridBagConstraints);

        windowSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        capacityPanel.add(windowSize, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        sunRouter.add(capacityPanel, gridBagConstraints);

        ripUpPanel.setLayout(new java.awt.GridBagLayout());

        ripUpPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Rip-Up & Reroute"));
        jLabel6.setText("Delta:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        ripUpPanel.add(jLabel6, gridBagConstraints);

        delta.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        ripUpPanel.add(delta, gridBagConstraints);

        jLabel9.setText("Overload limit:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        ripUpPanel.add(jLabel9, gridBagConstraints);

        overloadLimit.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        ripUpPanel.add(overloadLimit, gridBagConstraints);

        jLabel10.setText("Cost limit:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        ripUpPanel.add(jLabel10, gridBagConstraints);

        costLimit.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        ripUpPanel.add(costLimit, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        sunRouter.add(ripUpPanel, gridBagConstraints);

        layerAssignPanel.setLayout(new java.awt.GridBagLayout());

        layerAssignPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Layer Assignment"));
        jLabel8.setText("Layer assignment capacity factor:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        layerAssignPanel.add(jLabel8, gridBagConstraints);

        layerAssignCapF.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        layerAssignPanel.add(layerAssignCapF, gridBagConstraints);

        jLabel7.setText("Unassigned pin density factor:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        layerAssignPanel.add(jLabel7, gridBagConstraints);

        unassignedPinDensityF.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        layerAssignPanel.add(unassignedPinDensityF, gridBagConstraints);

        jLabel14.setText("Tiles per long net:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        layerAssignPanel.add(jLabel14, gridBagConstraints);

        tilesPerLongNet.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        layerAssignPanel.add(tilesPerLongNet, gridBagConstraints);

        jLabel15.setText("Tiles per medium net:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        layerAssignPanel.add(jLabel15, gridBagConstraints);

        tilesPerMedNet.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        layerAssignPanel.add(tilesPerMedNet, gridBagConstraints);

        jLabel16.setText("Long net length (nm):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        layerAssignPanel.add(jLabel16, gridBagConstraints);

        longNetLength.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        layerAssignPanel.add(longNetLength, gridBagConstraints);

        jLabel17.setText("Medium net length (nm):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        layerAssignPanel.add(jLabel17, gridBagConstraints);

        medNetLength.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        layerAssignPanel.add(medNetLength, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        sunRouter.add(layerAssignPanel, gridBagConstraints);

        detailPanel.setLayout(new java.awt.GridBagLayout());

        detailPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Detail Routing Parameters"));
        jLabel19.setText("Wire Offset:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(jLabel19, gridBagConstraints);

        wireOffset.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(wireOffset, gridBagConstraints);

        jLabel20.setText("Wire Modulo:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(jLabel20, gridBagConstraints);

        wireModulo.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(wireModulo, gridBagConstraints);

        jLabel21.setText("Wire Blockage Factor:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(jLabel21, gridBagConstraints);

        wireBlockageFactor.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(wireBlockageFactor, gridBagConstraints);

        jLabel22.setText("RipUp Maximum:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(jLabel22, gridBagConstraints);

        ripupMaximum.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(ripupMaximum, gridBagConstraints);

        jLabel23.setText("RipUp Penalty:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(jLabel23, gridBagConstraints);

        ripupPenalty.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(ripupPenalty, gridBagConstraints);

        jLabel24.setText("RipUp Expansion:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(jLabel24, gridBagConstraints);

        ripupExpansion.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(ripupExpansion, gridBagConstraints);

        jLabel25.setText("Z RipUp Expansion:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(jLabel25, gridBagConstraints);

        zRipupExpansion.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(zRipupExpansion, gridBagConstraints);

        jLabel26.setText("RipUp Searches:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(jLabel26, gridBagConstraints);

        ripupSearches.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(ripupSearches, gridBagConstraints);

        jLabel27.setText("Global Path Expansion:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(jLabel27, gridBagConstraints);

        globalPathExpansion.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(globalPathExpansion, gridBagConstraints);

        jLabel28.setText("Source Access Expansion:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(jLabel28, gridBagConstraints);

        sourceAccessExpansion.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(sourceAccessExpansion, gridBagConstraints);

        jLabel29.setText("Sink Access Expansion:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(jLabel29, gridBagConstraints);

        sinkAccessExpansion.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(sinkAccessExpansion, gridBagConstraints);

        jLabel30.setText("Dense Via Area Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(jLabel30, gridBagConstraints);

        denseViaAreaSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(denseViaAreaSize, gridBagConstraints);

        jLabel31.setText("Retry Expand Routing:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(jLabel31, gridBagConstraints);

        retryExpandRouting.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(retryExpandRouting, gridBagConstraints);

        jLabel32.setText("Retry Dense Via Area Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(jLabel32, gridBagConstraints);

        retryDenseViaAreaSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(retryDenseViaAreaSize, gridBagConstraints);

        jLabel33.setText("Path Search Control:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(jLabel33, gridBagConstraints);

        pathSearchControl.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(pathSearchControl, gridBagConstraints);

        jLabel34.setText("Sparse Via Modulo:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(jLabel34, gridBagConstraints);

        sparseViaModulo.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(sparseViaModulo, gridBagConstraints);

        jLabel35.setText("Low Path Search Cost:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(jLabel35, gridBagConstraints);

        lowPathSearchCost.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(lowPathSearchCost, gridBagConstraints);

        jLabel36.setText("Medium Path Search Cost:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(jLabel36, gridBagConstraints);

        mediumPathSearchCost.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(mediumPathSearchCost, gridBagConstraints);

        jLabel37.setText("High Path Search Cost:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(jLabel37, gridBagConstraints);

        highPathSearchCost.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(highPathSearchCost, gridBagConstraints);

        jLabel38.setText("Taken Path Search Cost:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(jLabel38, gridBagConstraints);

        takenPathSearchCost.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        detailPanel.add(takenPathSearchCost, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        sunRouter.add(detailPanel, gridBagConstraints);

        getContentPane().add(sunRouter, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel capacityPanel;
    private javax.swing.JTextField costLimit;
    private javax.swing.JTextField cutLineDeviation;
    private javax.swing.JTextField delta;
    private javax.swing.JTextField denseViaAreaSize;
    private javax.swing.JPanel detailPanel;
    private javax.swing.JPanel globalPanel;
    private javax.swing.JTextField globalPathExpansion;
    private javax.swing.JTextField highPathSearchCost;
    private javax.swing.JTextField horizBitSize;
    private javax.swing.JTextField horizTileSize;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JTextField layerAssignCapF;
    private javax.swing.JPanel layerAssignPanel;
    private javax.swing.JTextField longNetLength;
    private javax.swing.JTextField lowPathSearchCost;
    private javax.swing.JTextField medNetLength;
    private javax.swing.JTextField mediumPathSearchCost;
    private javax.swing.JTextField oneTileFactor;
    private javax.swing.JTextField overloadLimit;
    private javax.swing.JTextField pathSearchControl;
    private javax.swing.JTextField pinFactor;
    private javax.swing.JTextField retryDenseViaAreaSize;
    private javax.swing.JTextField retryExpandRouting;
    private javax.swing.JPanel ripUpPanel;
    private javax.swing.JTextField ripupExpansion;
    private javax.swing.JTextField ripupMaximum;
    private javax.swing.JTextField ripupPenalty;
    private javax.swing.JTextField ripupSearches;
    private javax.swing.JTextField sinkAccessExpansion;
    private javax.swing.JTextField sourceAccessExpansion;
    private javax.swing.JTextField sparseViaModulo;
    private javax.swing.JPanel sunRouter;
    private javax.swing.JTextField takenPathSearchCost;
    private javax.swing.JTextField tilesPerLongNet;
    private javax.swing.JTextField tilesPerMedNet;
    private javax.swing.JTextField unassignedPinDensityF;
    private javax.swing.JComboBox verbosityCombo;
    private javax.swing.JTextField vertBitSize;
    private javax.swing.JTextField vertTileSize;
    private javax.swing.JTextField windowSize;
    private javax.swing.JTextField wireBlockageFactor;
    private javax.swing.JTextField wireModulo;
    private javax.swing.JTextField wireOffset;
    private javax.swing.JTextField zRipupExpansion;
    // End of variables declaration//GEN-END:variables

}
