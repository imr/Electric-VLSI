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
			verbosityCombo.setEnabled(false);
		    costLimit.setEnabled(false);
		    cutLineDeviation.setEnabled(false);
		    delta.setEnabled(false);
		    horizBitSize.setEnabled(false);
		    horizTileSize.setEnabled(false);
		    layerAssignCapF.setEnabled(false);
		    longNetLength.setEnabled(false);
		    medNetLength.setEnabled(false);
		    oneTileFactor.setEnabled(false);
		    overloadLimit.setEnabled(false);
		    pinFactor.setEnabled(false);
		    tilesPerLongNet.setEnabled(false);
		    tilesPerMedNet.setEnabled(false);
		    unassignedPinDensityF.setEnabled(false);
		    vertBitSize.setEnabled(false);
		    vertTileSize.setEnabled(false);
		    windowSize.setEnabled(false);
		} else
		{
			verbosityCombo.setSelectedIndex(Routing.getSunRouterVerboseLevel());
		    costLimit.setText("" + Routing.getSunRouterCostLimit());
		    cutLineDeviation.setText("" + Routing.getSunRouterCutlineDeviation());
		    delta.setText("" + Routing.getSunRouterDelta());
		    horizBitSize.setText("" + Routing.getSunRouterXBitSize());
		    horizTileSize.setText("" + Routing.getSunRouterXTileSize());
		    layerAssignCapF.setText("" + Routing.getSunRouterLayerAssgnCapF());
		    longNetLength.setText("" + Routing.getSunRouterLengthLongNet());
		    medNetLength.setText("" + Routing.getSunRouterLengthMedNet());
		    oneTileFactor.setText("" + Routing.getSunRouterOneTileFactor());
		    overloadLimit.setText("" + Routing.getSunRouterOverloadLimit());
		    pinFactor.setText("" + Routing.getSunRouterPinFactor());
		    tilesPerLongNet.setText("" + Routing.getSunRouterTilesPerPinLongNet());
		    tilesPerMedNet.setText("" + Routing.getSunRouterTilesPerPinMedNet());
		    unassignedPinDensityF.setText("" + Routing.getSunRouterUPinDensityF());
		    vertBitSize.setText("" + Routing.getSunRouterYBitSize());
		    vertTileSize.setText("" + Routing.getSunRouterYTileSize());
		    windowSize.setText("" + Routing.getSunRouterWindow());
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

		newIValue = verbosityCombo.getSelectedIndex();
		if (newIValue != Routing.getSunRouterVerboseLevel())
			Routing.setSunRouterVerboseLevel(newIValue);		
		newDValue = TextUtils.atof(costLimit.getText());
		if (newDValue != Routing.getSunRouterCostLimit())
			Routing.setSunRouterCostLimit(newDValue);
		newDValue = TextUtils.atof(cutLineDeviation.getText());
		if (newDValue != Routing.getSunRouterCutlineDeviation())
			Routing.setSunRouterCutlineDeviation(newDValue);
		newDValue = TextUtils.atof(delta.getText());
		if (newDValue != Routing.getSunRouterDelta())
			Routing.setSunRouterDelta(newDValue);
		newIValue = TextUtils.atoi(horizBitSize.getText());
		if (newIValue != Routing.getSunRouterXBitSize())
			Routing.setSunRouterXBitSize(newIValue);
		newIValue = TextUtils.atoi(horizTileSize.getText());
		if (newIValue != Routing.getSunRouterXTileSize())
			Routing.setSunRouterXTileSize(newIValue);
		newDValue = TextUtils.atof(layerAssignCapF.getText());
		if (newDValue != Routing.getSunRouterLayerAssgnCapF())
			Routing.setSunRouterLayerAssgnCapF(newDValue);
		newDValue = TextUtils.atof(longNetLength.getText());
		if (newDValue != Routing.getSunRouterLengthLongNet())
			Routing.setSunRouterLengthLongNet(newDValue);
		newDValue = TextUtils.atof(medNetLength.getText());
		if (newDValue != Routing.getSunRouterLengthMedNet())
			Routing.setSunRouterLengthMedNet(newDValue);
		newDValue = TextUtils.atof(oneTileFactor.getText());
		if (newDValue != Routing.getSunRouterOneTileFactor())
			Routing.setSunRouterOneTileFactor(newDValue);
		newDValue = TextUtils.atof(overloadLimit.getText());
		if (newDValue != Routing.getSunRouterOneTileFactor())
			Routing.setSunRouterOneTileFactor(newDValue);
		newIValue = TextUtils.atoi(overloadLimit.getText());
		if (newIValue != Routing.getSunRouterOverloadLimit())
			Routing.setSunRouterOverloadLimit(newIValue);
		newIValue = TextUtils.atoi(pinFactor.getText());
		if (newIValue != Routing.getSunRouterPinFactor())
			Routing.setSunRouterPinFactor(newIValue);
		newDValue = TextUtils.atof(tilesPerLongNet.getText());
		if (newDValue != Routing.getSunRouterTilesPerPinLongNet())
			Routing.setSunRouterTilesPerPinLongNet(newDValue);
		newDValue = TextUtils.atof(tilesPerMedNet.getText());
		if (newDValue != Routing.getSunRouterTilesPerPinMedNet())
			Routing.setSunRouterTilesPerPinMedNet(newDValue);
		newDValue = TextUtils.atof(unassignedPinDensityF.getText());
		if (newDValue != Routing.getSunRouterUPinDensityF())
			Routing.setSunRouterUPinDensityF(newDValue);
		newIValue = TextUtils.atoi(vertBitSize.getText());
		if (newIValue != Routing.getSunRouterYBitSize())
			Routing.setSunRouterYBitSize(newIValue);
		newIValue = TextUtils.atoi(vertTileSize.getText());
		if (newIValue != Routing.getSunRouterYTileSize())
			Routing.setSunRouterYTileSize(newIValue);
		newIValue = TextUtils.atoi(windowSize.getText());
		if (newIValue != Routing.getSunRouterWindow())
			Routing.setSunRouterWindow(newIValue);
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

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("IO Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
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
    private javax.swing.JPanel globalPanel;
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
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JTextField layerAssignCapF;
    private javax.swing.JPanel layerAssignPanel;
    private javax.swing.JTextField longNetLength;
    private javax.swing.JTextField medNetLength;
    private javax.swing.JTextField oneTileFactor;
    private javax.swing.JTextField overloadLimit;
    private javax.swing.JTextField pinFactor;
    private javax.swing.JPanel ripUpPanel;
    private javax.swing.JPanel sunRouter;
    private javax.swing.JTextField tilesPerLongNet;
    private javax.swing.JTextField tilesPerMedNet;
    private javax.swing.JTextField unassignedPinDensityF;
    private javax.swing.JComboBox verbosityCombo;
    private javax.swing.JTextField vertBitSize;
    private javax.swing.JTextField vertTileSize;
    private javax.swing.JTextField windowSize;
    // End of variables declaration//GEN-END:variables

}
