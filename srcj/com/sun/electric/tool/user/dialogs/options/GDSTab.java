/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GDSTab.java
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

import com.sun.electric.tool.io.IOTool;

import java.awt.Frame;

import javax.swing.JPanel;

/**
 * Class to handle the "GDS" tab of the Preferences dialog.
 */
public class GDSTab extends PreferencePanel
{
	/** Creates new form GDSTab */
	public GDSTab(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return gds; }

	/** return the name of this preferences tab. */
	public String getName() { return "GDS"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the GDS tab.
	 */
	public void init()
	{
        gdsConvertNCCExportsConnectedByParentPins.setSelected(IOTool.getGDSConvertNCCExportsConnectedByParentPins());
        gdsInputMergesBoxes.setSelected(IOTool.isGDSInMergesBoxes());
		gdsInputIncludesText.setSelected(IOTool.isGDSInIncludesText());
		gdsInputExpandsCells.setSelected(IOTool.isGDSInExpandsCells());
		gdsInputInstantiatesArrays.setSelected(IOTool.isGDSInInstantiatesArrays());
        gdsUnknownLayers.addItem("Ignore");
        gdsUnknownLayers.addItem("Convert to DRC Exclusion layer");
        gdsUnknownLayers.addItem("Convert to random layer");
        gdsUnknownLayers.setSelectedIndex(IOTool.getGDSInUnknownLayerHandling());
        gdsSimplifyCells.setSelected(IOTool.isGDSInSimplifyCells());
        gdsColapseNames.setSelected(IOTool.isGDSColapseVddGndPinNames());
        gdsArraySimplification.addItem("None");
        gdsArraySimplification.addItem("Merge individual arrays");
        gdsArraySimplification.addItem("Merge all arrays");
        gdsArraySimplification.setSelectedIndex(IOTool.getGDSArraySimplification());
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the GDS tab.
	 */
	public void term()
	{
		boolean currentValue = gdsConvertNCCExportsConnectedByParentPins.isSelected();
        if (currentValue != IOTool.getGDSConvertNCCExportsConnectedByParentPins())
            IOTool.setGDSConvertNCCExportsConnectedByParentPins(currentValue);

		currentValue = gdsInputMergesBoxes.isSelected();
		if (currentValue != IOTool.isGDSInMergesBoxes())
			IOTool.setGDSInMergesBoxes(currentValue);
		currentValue = gdsInputIncludesText.isSelected();
		if (currentValue != IOTool.isGDSInIncludesText())
			IOTool.setGDSInIncludesText(currentValue);
		currentValue = gdsInputExpandsCells.isSelected();
		if (currentValue != IOTool.isGDSInExpandsCells())
			IOTool.setGDSInExpandsCells(currentValue);
		currentValue = gdsInputInstantiatesArrays.isSelected();
		if (currentValue != IOTool.isGDSInInstantiatesArrays())
			IOTool.setGDSInInstantiatesArrays(currentValue);
		int currentI = gdsUnknownLayers.getSelectedIndex();
		if (currentI != IOTool.getGDSInUnknownLayerHandling())
			IOTool.setGDSInUnknownLayerHandling(currentI);
        currentValue = gdsSimplifyCells.isSelected();
        if (currentValue != IOTool.isGDSInSimplifyCells())
			IOTool.setGDSInSimplifyCells(currentValue);
        currentValue = gdsColapseNames.isSelected();
        if (currentValue != IOTool.isGDSColapseVddGndPinNames())
			IOTool.setGDSColapseVddGndPinNames(currentValue);
        currentI = gdsArraySimplification.getSelectedIndex();
        if (currentI != IOTool.getGDSArraySimplification())
        	IOTool.setGDSArraySimplification(currentI);
	}

	/**
	 * Method called when the factory reset is requested.
	 */
	public void reset()
	{
		if (IOTool.getFactoryGDSConvertNCCExportsConnectedByParentPins() != IOTool.getGDSConvertNCCExportsConnectedByParentPins())
			IOTool.setGDSConvertNCCExportsConnectedByParentPins(IOTool.getFactoryGDSConvertNCCExportsConnectedByParentPins());
		if (IOTool.isFactoryGDSInMergesBoxes() != IOTool.isGDSInMergesBoxes())
			IOTool.setGDSInMergesBoxes(IOTool.isFactoryGDSInMergesBoxes());
		if (IOTool.isFactoryGDSInIncludesText() != IOTool.isGDSInIncludesText())
			IOTool.setGDSInIncludesText(IOTool.isFactoryGDSInIncludesText());
		if (IOTool.isFactoryGDSInExpandsCells() != IOTool.isGDSInExpandsCells())
			IOTool.setGDSInExpandsCells(IOTool.isFactoryGDSInExpandsCells());
		if (IOTool.isFactoryGDSInInstantiatesArrays() != IOTool.isGDSInInstantiatesArrays())
			IOTool.setGDSInInstantiatesArrays(IOTool.isFactoryGDSInInstantiatesArrays());
		if (IOTool.getFactoryGDSInUnknownLayerHandling() != IOTool.getGDSInUnknownLayerHandling())
			IOTool.setGDSInUnknownLayerHandling(IOTool.getFactoryGDSInUnknownLayerHandling());
		if (IOTool.isFactoryGDSInSimplifyCells() != IOTool.isGDSInSimplifyCells())
			IOTool.setGDSInSimplifyCells(IOTool.isFactoryGDSInSimplifyCells());
		if (IOTool.isFactoryGDSColapseVddGndPinNames() != IOTool.isGDSColapseVddGndPinNames())
			IOTool.setGDSColapseVddGndPinNames(IOTool.isFactoryGDSColapseVddGndPinNames());
		if (IOTool.getFactoryGDSArraySimplification() != IOTool.getGDSArraySimplification())
			IOTool.setGDSArraySimplification(IOTool.getFactoryGDSArraySimplification());
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        gds = new javax.swing.JPanel();
        GDSInput = new javax.swing.JPanel();
        gdsInputMergesBoxes = new javax.swing.JCheckBox();
        gdsInputIncludesText = new javax.swing.JCheckBox();
        gdsInputExpandsCells = new javax.swing.JCheckBox();
        gdsInputInstantiatesArrays = new javax.swing.JCheckBox();
        gdsSimplifyCells = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        gdsArraySimplification = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        gdsUnknownLayers = new javax.swing.JComboBox();
        GDSOutput = new javax.swing.JPanel();
        gdsConvertNCCExportsConnectedByParentPins = new javax.swing.JCheckBox();
        gdsColapseNames = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("IO Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        gds.setLayout(new java.awt.GridBagLayout());

        GDSInput.setLayout(new java.awt.GridBagLayout());

        GDSInput.setBorder(javax.swing.BorderFactory.createTitledBorder("GDS Input"));
        gdsInputMergesBoxes.setText("Merge boxes (slow)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        GDSInput.add(gdsInputMergesBoxes, gridBagConstraints);

        gdsInputIncludesText.setText("Include text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        GDSInput.add(gdsInputIncludesText, gridBagConstraints);

        gdsInputExpandsCells.setText("Expand cells");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        GDSInput.add(gdsInputExpandsCells, gridBagConstraints);

        gdsInputInstantiatesArrays.setText("Instantiate arrays");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        GDSInput.add(gdsInputInstantiatesArrays, gridBagConstraints);

        gdsSimplifyCells.setText("Simplify contact vias");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        GDSInput.add(gdsSimplifyCells, gridBagConstraints);

        jLabel3.setText("Array simplification:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        GDSInput.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 4);
        GDSInput.add(gdsArraySimplification, gridBagConstraints);

        jLabel1.setText("Unknown layers:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        GDSInput.add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 4);
        GDSInput.add(gdsUnknownLayers, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gds.add(GDSInput, gridBagConstraints);

        GDSOutput.setLayout(new java.awt.GridBagLayout());

        GDSOutput.setBorder(javax.swing.BorderFactory.createTitledBorder("GDS Output"));
        gdsConvertNCCExportsConnectedByParentPins.setText("Use NCC annotations for exports");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        GDSOutput.add(gdsConvertNCCExportsConnectedByParentPins, gridBagConstraints);

        gdsColapseNames.setText("Collapse VDD/GND pin names");
        gdsColapseNames.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gdsColapseNamesActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        GDSOutput.add(gdsColapseNames, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gds.add(GDSOutput, gridBagConstraints);

        getContentPane().add(gds, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void gdsColapseNamesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gdsColapseNamesActionPerformed
// TODO add your handling code here:
    }//GEN-LAST:event_gdsColapseNamesActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel GDSInput;
    private javax.swing.JPanel GDSOutput;
    private javax.swing.JPanel gds;
    private javax.swing.JComboBox gdsArraySimplification;
    private javax.swing.JCheckBox gdsColapseNames;
    private javax.swing.JCheckBox gdsConvertNCCExportsConnectedByParentPins;
    private javax.swing.JCheckBox gdsInputExpandsCells;
    private javax.swing.JCheckBox gdsInputIncludesText;
    private javax.swing.JCheckBox gdsInputInstantiatesArrays;
    private javax.swing.JCheckBox gdsInputMergesBoxes;
    private javax.swing.JCheckBox gdsSimplifyCells;
    private javax.swing.JComboBox gdsUnknownLayers;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    // End of variables declaration//GEN-END:variables
}
