/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CompactionTab.java
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
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.dialogs.OpenFile;

import javax.swing.JPanel;


/**
 * Class to handle the "IRSIM" tab of the Preferences dialog.
 */
public class IRSIMTab extends PreferencePanel
{
	/** Creates new form CompactionTab */
	public IRSIMTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}
	public JPanel getPanel() { return irsim; }

	public String getName() { return "IRSIM"; }

	// this is a copy of what is in com.sun.electric.plugins.irsim.Sim.java
	/** event scheduling */							private static final int	DEBUG_EV	= 0x01;		
	/** final value computation */					private static final int	DEBUG_DC	= 0x02;		
	/** tau/delay computation */					private static final int	DEBUG_TAU	= 0x04;		
	/** taup computation */							private static final int	DEBUG_TAUP	= 0x08;		
	/** spike analysis */							private static final int	DEBUG_SPK	= 0x10;		
	/** tree walk */								private static final int	DEBUG_TW	= 0x20;		

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Compaction tab.
	 */
	public void init()
	{
		resimulateEachChange.setSelected(Simulation.isIRSIMResimulateEach());
		autoAdvanceTime.setSelected(Simulation.isIRSIMAutoAdvance());
		showCommands.setSelected(Simulation.isIRSIMShowsCommands());
        delayedX.setSelected(Simulation.isIRSIMDelayedX());

		switch (Simulation.getIRSIMParasiticLevel())
		{
			case 0: quickParasitics.setSelected(true);   break;
			case 1: localParasitics.setSelected(true);   break;
			case 2: fullParasitics.setSelected(true);    break;
		}

		int initialDebugging = Simulation.getIRSIMDebugging();
		if ((initialDebugging&DEBUG_EV) != 0) debugEv.setSelected(true);
		if ((initialDebugging&DEBUG_DC) != 0) debugDC.setSelected(true);
		if ((initialDebugging&DEBUG_TAU) != 0) debugTau.setSelected(true);
		if ((initialDebugging&DEBUG_TAUP) != 0) debugTauP.setSelected(true);
		if ((initialDebugging&DEBUG_SPK) != 0) debugSpk.setSelected(true);
		if ((initialDebugging&DEBUG_TW) != 0) debugTW.setSelected(true);

		parameterFile.setText(Simulation.getIRSIMParameterFile());

		simModel.addItem("RC");
		simModel.addItem("Linear");
		simModel.setSelectedItem(Simulation.getIRSIMStepModel());

		// not yet:
		quickParasitics.setEnabled(false);
		localParasitics.setEnabled(false);
		fullParasitics.setEnabled(false);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Compaction tab.
	 */
	public void term()
	{
		boolean currBoolean = resimulateEachChange.isSelected();
		if (currBoolean != Simulation.isIRSIMResimulateEach())
			Simulation.setIRSIMResimulateEach(currBoolean);

		currBoolean = autoAdvanceTime.isSelected();
		if (currBoolean != Simulation.isIRSIMAutoAdvance())
			Simulation.setIRSIMAutoAdvance(currBoolean);

		currBoolean = showCommands.isSelected();
		if (currBoolean != Simulation.isIRSIMShowsCommands())
			Simulation.setIRSIMShowsCommands(currBoolean);

        currBoolean = delayedX.isSelected();
        if (currBoolean != Simulation.isIRSIMDelayedX())
            Simulation.setIRSIMDelayedX(currBoolean);

		int currInt = 0;
		if (quickParasitics.isSelected()) currInt = 0; else
			if (localParasitics.isSelected()) currInt = 1; else
				if (fullParasitics.isSelected()) currInt = 2;
		if (currInt != Simulation.getIRSIMParasiticLevel())
			Simulation.setIRSIMParasiticLevel(currInt);

		currInt = 0;
		if (debugEv.isSelected()) currInt |= DEBUG_EV;
		if (debugDC.isSelected()) currInt |= DEBUG_DC;
		if (debugTau.isSelected()) currInt |= DEBUG_TAU;
		if (debugTauP.isSelected()) currInt |= DEBUG_TAUP;
		if (debugSpk.isSelected()) currInt |= DEBUG_SPK;
		if (debugTW.isSelected()) currInt |= DEBUG_TW;
		if (currInt != Simulation.getIRSIMParasiticLevel())
			Simulation.setIRSIMDebugging(currInt);

		String currString = parameterFile.getText();
		if (!currString.equals(Simulation.getIRSIMParameterFile()))
			Simulation.setIRSIMParameterFile(currString);

		currString = (String)simModel.getSelectedItem();
		if (!currString.equals(Simulation.getIRSIMStepModel()))
			Simulation.setIRSIMStepModel(currString);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        parasitics = new javax.swing.ButtonGroup();
        irsim = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        showCommands = new javax.swing.JCheckBox();
        autoAdvanceTime = new javax.swing.JCheckBox();
        resimulateEachChange = new javax.swing.JCheckBox();
        delayedX = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        setParameterFile = new javax.swing.JButton();
        parameterFile = new javax.swing.JTextField();
        quickParasitics = new javax.swing.JRadioButton();
        localParasitics = new javax.swing.JRadioButton();
        fullParasitics = new javax.swing.JRadioButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        simModel = new javax.swing.JComboBox();
        jPanel3 = new javax.swing.JPanel();
        debugEv = new javax.swing.JCheckBox();
        debugDC = new javax.swing.JCheckBox();
        debugTau = new javax.swing.JCheckBox();
        debugTauP = new javax.swing.JCheckBox();
        debugSpk = new javax.swing.JCheckBox();
        debugTW = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Tool Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        irsim.setLayout(new java.awt.GridBagLayout());

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setBorder(new javax.swing.border.TitledBorder("Control"));
        showCommands.setText("Show IRSIM commands");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(showCommands, gridBagConstraints);

        autoAdvanceTime.setText("Auto advance time");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(autoAdvanceTime, gridBagConstraints);

        resimulateEachChange.setText("Resimulate each change");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(resimulateEachChange, gridBagConstraints);

        delayedX.setText("Use Delayed X Propogation");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(delayedX, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        irsim.add(jPanel1, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jPanel2.setBorder(new javax.swing.border.TitledBorder("Parasitics"));
        setParameterFile.setText("Set");
        setParameterFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setParameterFileActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(setParameterFile, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(parameterFile, gridBagConstraints);

        quickParasitics.setText("Quick");
        parasitics.add(quickParasitics);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(quickParasitics, gridBagConstraints);

        localParasitics.setText("Local");
        parasitics.add(localParasitics);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(localParasitics, gridBagConstraints);

        fullParasitics.setText("Full");
        parasitics.add(fullParasitics);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(fullParasitics, gridBagConstraints);

        jLabel2.setText("Parameter file:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(jLabel2, gridBagConstraints);

        jLabel4.setText("Model:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(jLabel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(simModel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        irsim.add(jPanel2, gridBagConstraints);

        jPanel3.setLayout(new java.awt.GridBagLayout());

        jPanel3.setBorder(new javax.swing.border.TitledBorder("Debugging"));
        debugEv.setText("Event Scheduling");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel3.add(debugEv, gridBagConstraints);

        debugDC.setText("Final Value Computation");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel3.add(debugDC, gridBagConstraints);

        debugTau.setText("Tau/Delay Computation");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel3.add(debugTau, gridBagConstraints);

        debugTauP.setText("TauP Computation");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel3.add(debugTauP, gridBagConstraints);

        debugSpk.setText("Spike Analysis");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel3.add(debugSpk, gridBagConstraints);

        debugTW.setText("Tree Walk");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel3.add(debugTW, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        irsim.add(jPanel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(irsim, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

    private void setParameterFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setParameterFileActionPerformed
        String paramFile = OpenFile.chooseInputFile(FileType.IRSIMPARAM, "IRSIM Parameter file");
        if (paramFile == null) return;
        parameterFile.setText(paramFile);
    }//GEN-LAST:event_setParameterFileActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox autoAdvanceTime;
    private javax.swing.JCheckBox debugDC;
    private javax.swing.JCheckBox debugEv;
    private javax.swing.JCheckBox debugSpk;
    private javax.swing.JCheckBox debugTW;
    private javax.swing.JCheckBox debugTau;
    private javax.swing.JCheckBox debugTauP;
    private javax.swing.JCheckBox delayedX;
    private javax.swing.JRadioButton fullParasitics;
    private javax.swing.JPanel irsim;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JRadioButton localParasitics;
    private javax.swing.JTextField parameterFile;
    private javax.swing.ButtonGroup parasitics;
    private javax.swing.JRadioButton quickParasitics;
    private javax.swing.JCheckBox resimulateEachChange;
    private javax.swing.JButton setParameterFile;
    private javax.swing.JCheckBox showCommands;
    private javax.swing.JComboBox simModel;
    // End of variables declaration//GEN-END:variables

}
