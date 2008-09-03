/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ParasiticTab.java
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

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.extract.ParasiticTool;
import com.sun.electric.tool.simulation.Simulation;

import java.awt.Frame;

import javax.swing.JPanel;

/**
 * Class to handle the "Parasitics" tab of the Preferences dialog.
 */
public class ParasiticTab extends PreferencePanel {

	/** Creates new form ParasiticTab */
	public ParasiticTab(Frame parent, boolean modal) {
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return topPanel; }

	/** return the name of this preferences tab. */
	public String getName() { return "Parasitic"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Routing tab.
	 */
	public void init()
	{		
		verboseNaming.setSelected(Simulation.isParasiticsUseVerboseNaming());
		backannotateLayout.setSelected(Simulation.isParasiticsBackAnnotateLayout());
		extractPowerGround.setSelected(Simulation.isParasiticsExtractPowerGround());
		extractPowerGround.setEnabled(false);
        useExemptedNetsFile.setSelected(Simulation.isParasiticsUseExemptedNetsFile());
        ignoreExemptedNets.setEnabled(useExemptedNetsFile.isSelected());
        extractExemptedNets.setEnabled(useExemptedNetsFile.isSelected());
        ignoreExemptedNets.setSelected(Simulation.isParasiticsIgnoreExemptedNets());
        extractExemptedNets.setSelected(!Simulation.isParasiticsIgnoreExemptedNets());
        extractR.setSelected(Simulation.isParasiticsExtractsR());
        extractC.setSelected(Simulation.isParasiticsExtractsC());

        // the parasitics panel (not visible)
		maxDistValue.setText(TextUtils.formatDouble(ParasiticTool.getMaxDistance()));
		parasiticPanel.setVisible(false);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Routing tab.
	 */
	public void term()
	{
		ParasiticTool.setMaxDistance(Double.parseDouble(maxDistValue.getText()));

		boolean b = verboseNaming.isSelected();
		if (b != Simulation.isParasiticsUseVerboseNaming()) Simulation.setParasiticsUseVerboseNaming(b);
		b = backannotateLayout.isSelected();
		if (b != Simulation.isParasiticsBackAnnotateLayout()) Simulation.setParasiticsBackAnnotateLayout(b);
		b = extractPowerGround.isSelected();
		if (b != Simulation.isParasiticsExtractPowerGround()) Simulation.setParasiticsExtractPowerGround(b);
        b = useExemptedNetsFile.isSelected();
        if (b != Simulation.isParasiticsUseExemptedNetsFile()) Simulation.setParasiticsUseExemptedNetsFile(b);
        b = ignoreExemptedNets.isSelected();
            Simulation.setParasiticsIgnoreExemptedNets(b);
        b = extractR.isSelected();
        if (b != Simulation.isParasiticsExtractsR())
            Simulation.setParasiticsExtractsR(b);
        b = extractC.isSelected();
        if (b != Simulation.isParasiticsExtractsC())
            Simulation.setParasiticsExtractsC(b);
    }

	/**
	 * Method called when the factory reset is requested.
	 */
	public void reset()
	{
		if (Simulation.isFactoryParasiticsUseVerboseNaming() != Simulation.isParasiticsUseVerboseNaming())
			Simulation.setParasiticsUseVerboseNaming(Simulation.isFactoryParasiticsUseVerboseNaming());
		if (Simulation.isFactoryParasiticsBackAnnotateLayout() != Simulation.isParasiticsBackAnnotateLayout())
			Simulation.setParasiticsBackAnnotateLayout(Simulation.isFactoryParasiticsBackAnnotateLayout());
		if (Simulation.isFactoryParasiticsExtractPowerGround() != Simulation.isParasiticsExtractPowerGround())
			Simulation.setParasiticsExtractPowerGround(Simulation.isFactoryParasiticsExtractPowerGround());
		if (Simulation.isFactoryParasiticsUseExemptedNetsFile() != Simulation.isParasiticsUseExemptedNetsFile())
			Simulation.setParasiticsUseExemptedNetsFile(Simulation.isFactoryParasiticsUseExemptedNetsFile());
		if (Simulation.isFactoryParasiticsIgnoreExemptedNets() != Simulation.isParasiticsIgnoreExemptedNets())
			Simulation.setParasiticsIgnoreExemptedNets(Simulation.isFactoryParasiticsIgnoreExemptedNets());
		if (Simulation.isFactoryParasiticsExtractsR() != Simulation.isParasiticsExtractsR())
			Simulation.setParasiticsExtractsR(Simulation.isFactoryParasiticsExtractsR());
		if (Simulation.isFactoryParasiticsExtractsC() != Simulation.isParasiticsExtractsC())
			Simulation.setParasiticsExtractsC(Simulation.isFactoryParasiticsExtractsC());
		if (ParasiticTool.getFactoryMaxDistance() != ParasiticTool.getMaxDistance())
			ParasiticTool.setMaxDistance(ParasiticTool.getFactoryMaxDistance());
	}

	/** This method is called from within the constructor to
	 * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        exemptedNetsGroup = new javax.swing.ButtonGroup();
        topPanel = new javax.swing.JPanel();
        parasiticPanel = new javax.swing.JPanel();
        maxDist = new javax.swing.JLabel();
        maxDistValue = new javax.swing.JTextField();
        simpleParasiticOptions = new javax.swing.JPanel();
        verboseNaming = new javax.swing.JCheckBox();
        backannotateLayout = new javax.swing.JCheckBox();
        extractPowerGround = new javax.swing.JCheckBox();
        useExemptedNetsFile = new javax.swing.JCheckBox();
        ignoreExemptedNets = new javax.swing.JRadioButton();
        extractExemptedNets = new javax.swing.JRadioButton();
        extractR = new javax.swing.JCheckBox();
        extractC = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        topPanel.setLayout(new java.awt.GridBagLayout());

        parasiticPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Parasitic Coupling Options"));
        parasiticPanel.setEnabled(false);
        parasiticPanel.setLayout(new java.awt.GridBagLayout());

        maxDist.setText("Maximum distance (lambda)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        parasiticPanel.add(maxDist, gridBagConstraints);

        maxDistValue.setColumns(6);
        maxDistValue.setText("20");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        parasiticPanel.add(maxDistValue, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        topPanel.add(parasiticPanel, gridBagConstraints);

        simpleParasiticOptions.setBorder(javax.swing.BorderFactory.createTitledBorder("Simple Parasitic Options"));
        simpleParasiticOptions.setLayout(new java.awt.GridBagLayout());

        verboseNaming.setText("Use Verbose Naming");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        simpleParasiticOptions.add(verboseNaming, gridBagConstraints);

        backannotateLayout.setText("Back-Annotate Layout");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        simpleParasiticOptions.add(backannotateLayout, gridBagConstraints);

        extractPowerGround.setText("Extract Power/Ground");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        simpleParasiticOptions.add(extractPowerGround, gridBagConstraints);

        useExemptedNetsFile.setText("Use exemptedNets.txt file");
        useExemptedNetsFile.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                useExemptedNetsFileStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        simpleParasiticOptions.add(useExemptedNetsFile, gridBagConstraints);

        exemptedNetsGroup.add(ignoreExemptedNets);
        ignoreExemptedNets.setText("Extract everything except exempted nets");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        simpleParasiticOptions.add(ignoreExemptedNets, gridBagConstraints);

        exemptedNetsGroup.add(extractExemptedNets);
        extractExemptedNets.setText("Extract only exempted nets");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        simpleParasiticOptions.add(extractExemptedNets, gridBagConstraints);

        extractR.setText("Extract R");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        simpleParasiticOptions.add(extractR, gridBagConstraints);

        extractC.setText("Extract C");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        simpleParasiticOptions.add(extractC, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        topPanel.add(simpleParasiticOptions, gridBagConstraints);

        getContentPane().add(topPanel, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void useExemptedNetsFileStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_useExemptedNetsFileStateChanged
        ignoreExemptedNets.setEnabled(useExemptedNetsFile.isSelected());
        extractExemptedNets.setEnabled(useExemptedNetsFile.isSelected());
    }//GEN-LAST:event_useExemptedNetsFileStateChanged

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        new ParasiticTab(new javax.swing.JFrame(), true).setVisible(true);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox backannotateLayout;
    private javax.swing.ButtonGroup exemptedNetsGroup;
    private javax.swing.JCheckBox extractC;
    private javax.swing.JRadioButton extractExemptedNets;
    private javax.swing.JCheckBox extractPowerGround;
    private javax.swing.JCheckBox extractR;
    private javax.swing.JRadioButton ignoreExemptedNets;
    private javax.swing.JLabel maxDist;
    private javax.swing.JTextField maxDistValue;
    private javax.swing.JPanel parasiticPanel;
    private javax.swing.JPanel simpleParasiticOptions;
    private javax.swing.JPanel topPanel;
    private javax.swing.JCheckBox useExemptedNetsFile;
    private javax.swing.JCheckBox verboseNaming;
    // End of variables declaration//GEN-END:variables
    
}
