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

import com.sun.electric.tool.simulation.Simulation;
import javax.swing.JPanel;


/**
 * Class to handle the "IRSIM" tab of the Preferences dialog.
 */
public class IRSIMTab extends PreferencePanel
{
	private boolean initialResimulateEach;
	private boolean initialAutoAdvance;
	private boolean initialShowCommands;
	private int initialParasiticsLevel;
	private String initialParameterFile;

	/** Creates new form CompactionTab */
	public IRSIMTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}
	public JPanel getPanel() { return irsim; }

	public String getName() { return "IRSIM"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Compaction tab.
	 */
	public void init()
	{
		initialResimulateEach = Simulation.isIRSIMResimulateEach();
		resimulateEachChange.setSelected(initialResimulateEach);

		initialAutoAdvance = Simulation.isIRSIMAutoAdvance();
		autoAdvanceTime.setSelected(initialAutoAdvance);

		initialShowCommands = Simulation.isIRSIMShowsCommands();
		showCommands.setSelected(initialShowCommands);

		initialParasiticsLevel = Simulation.getIRSIMParasiticLevel();
		switch (initialParasiticsLevel)
		{
			case 0: quickParasitics.setSelected(true);   break;
			case 1: localParasitics.setSelected(true);   break;
			case 2: fullParasitics.setSelected(true);    break;
		}

		initialParameterFile = Simulation.getIRSIMParameterFile();
		parameterFile.setText(initialParameterFile);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Compaction tab.
	 */
	public void term()
	{
		boolean currentResimulateEach = resimulateEachChange.isSelected();
		if (currentResimulateEach != initialResimulateEach)
			Simulation.setIRSIMResimulateEach(currentResimulateEach);

		boolean currentAutoAdvance = autoAdvanceTime.isSelected();
		if (currentAutoAdvance != initialResimulateEach)
			Simulation.setIRSIMAutoAdvance(currentAutoAdvance);

		boolean currentShowCommands = showCommands.isSelected();
		if (currentShowCommands != initialResimulateEach)
			Simulation.setIRSIMShowsCommands(currentShowCommands);

		int currentParasiticsLevel = 0;
		if (quickParasitics.isSelected()) currentParasiticsLevel = 0; else
			if (localParasitics.isSelected()) currentParasiticsLevel = 1; else
				if (fullParasitics.isSelected()) currentParasiticsLevel = 2;
		if (currentParasiticsLevel != initialParasiticsLevel)
			Simulation.setIRSIMParasiticLevel(currentParasiticsLevel);

		String currentParameterFile = parameterFile.getText();
		if (!currentParameterFile.equals(initialParameterFile))
			Simulation.setIRSIMParameterFile(currentParameterFile);
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
        resimulateEachChange = new javax.swing.JCheckBox();
        autoAdvanceTime = new javax.swing.JCheckBox();
        quickParasitics = new javax.swing.JRadioButton();
        localParasitics = new javax.swing.JRadioButton();
        fullParasitics = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        showCommands = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        parameterFile = new javax.swing.JTextField();
        setParameterFile = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Tool Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        irsim.setLayout(new java.awt.GridBagLayout());

        resimulateEachChange.setText("Resimulate each change");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        irsim.add(resimulateEachChange, gridBagConstraints);

        autoAdvanceTime.setText("Auto advance time");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        irsim.add(autoAdvanceTime, gridBagConstraints);

        parasitics.add(quickParasitics);
        quickParasitics.setText("Quick");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        irsim.add(quickParasitics, gridBagConstraints);

        parasitics.add(localParasitics);
        localParasitics.setText("Local");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        irsim.add(localParasitics, gridBagConstraints);

        parasitics.add(fullParasitics);
        fullParasitics.setText("Full");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        irsim.add(fullParasitics, gridBagConstraints);

        jLabel1.setText("Parasitics:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        irsim.add(jLabel1, gridBagConstraints);

        showCommands.setText("Show IRSIM commands");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        irsim.add(showCommands, gridBagConstraints);

        jLabel2.setText("Parameter file:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        irsim.add(jLabel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        irsim.add(parameterFile, gridBagConstraints);

        setParameterFile.setText("Set");
        setParameterFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setParameterFileActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        irsim.add(setParameterFile, gridBagConstraints);

        getContentPane().add(irsim, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

    private void setParameterFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setParameterFileActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_setParameterFileActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox autoAdvanceTime;
    private javax.swing.JRadioButton fullParasitics;
    private javax.swing.JPanel irsim;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JRadioButton localParasitics;
    private javax.swing.JTextField parameterFile;
    private javax.swing.ButtonGroup parasitics;
    private javax.swing.JRadioButton quickParasitics;
    private javax.swing.JCheckBox resimulateEachChange;
    private javax.swing.JButton setParameterFile;
    private javax.swing.JCheckBox showCommands;
    // End of variables declaration//GEN-END:variables

}
