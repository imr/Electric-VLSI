/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FastHenryTab.java
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
import com.sun.electric.tool.simulation.Simulation;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;


/**
 * Class to handle the "FastHenry" tab of the Preferences dialog.
 */
public class FastHenryTab extends PreferencePanel
{
	private boolean initialUseSingleFrequency;
	private double initialFreqStart, initialFreqEnd;
	private int initialRunsPerDecade;
	private boolean initialMultiPole;
	private int initialNumPoles;
	private double initialDefThickness;
	private int initialWidthSubdivisions, initialHeightSubdivisions;
	private double initialMaxSegLength;

	/** Creates new form FastHenryTab */
	public FastHenryTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}
	public JPanel getPanel() { return fastHenry; }

	public String getName() { return "Fast Henry"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Fast Henry tab.
	 */
	public void init()
	{
		initialUseSingleFrequency = Simulation.isFastHenryUseSingleFrequency();
		fhUseSingleFrequency.setSelected(initialUseSingleFrequency);
		fhUseSingleFrequency.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { updateWhatIsEnabled(); }
		});

		initialFreqStart = Simulation.getFastHenryStartFrequency();
		fhFrequencyStart.setText(TextUtils.formatDouble(initialFreqStart));

		initialFreqEnd = Simulation.getFastHenryEndFrequency();
		fhFrequencyEnd.setText(TextUtils.formatDouble(initialFreqEnd));

		initialRunsPerDecade = Simulation.getFastHenryRunsPerDecade();
		fhRunsPerDecade.setText(Integer.toString(initialRunsPerDecade));

		initialMultiPole = Simulation.isFastHenryMultiPole();
		fhMakeMultipole.setSelected(initialMultiPole);
		fhMakeMultipole.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { updateWhatIsEnabled(); }
		});

		initialNumPoles = Simulation.getFastHenryNumPoles();
		fhNumberOfPoles.setText(Integer.toString(initialNumPoles));

		initialDefThickness = Simulation.getFastHenryDefThickness();
		fhDefaultThickness.setText(TextUtils.formatDouble(initialDefThickness));

		initialWidthSubdivisions = Simulation.getFastHenryWidthSubdivisions();
		fhDefaultWidthSubs.setText(Integer.toString(initialWidthSubdivisions));

		initialHeightSubdivisions = Simulation.getFastHenryHeightSubdivisions();
		fhDefaultHeightSubs.setText(Integer.toString(initialHeightSubdivisions));

		initialMaxSegLength = Simulation.getFastHenryMaxSegLength();
		fhMaxSegmentLength.setText(TextUtils.formatDouble(initialMaxSegLength));

		updateWhatIsEnabled();
	}

	private void updateWhatIsEnabled()
	{
		fhFrequencyEnd.setEnabled(!fhUseSingleFrequency.isSelected());
		fhFreqEndLabel.setEnabled(!fhUseSingleFrequency.isSelected());
		fhRunsPerDecade.setEnabled(!fhUseSingleFrequency.isSelected());
		fhRunsPerDecadeLabel.setEnabled(!fhUseSingleFrequency.isSelected());
		fhNumberOfPoles.setEnabled(fhMakeMultipole.isSelected());
		fhNumPolesLabel.setEnabled(fhMakeMultipole.isSelected());
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Fast Henry tab.
	 */
	public void term()
	{
		boolean currentUseSingleFrequency = fhUseSingleFrequency.isSelected();
		if (currentUseSingleFrequency != initialUseSingleFrequency)
			Simulation.setFastHenryUseSingleFrequency(currentUseSingleFrequency);

		double currentFreqStart = TextUtils.atof(fhFrequencyStart.getText());
		if (currentFreqStart != initialFreqStart)
			Simulation.setFastHenryStartFrequency(currentFreqStart);

		double currentFreqEnd = TextUtils.atof(fhFrequencyEnd.getText());
		if (currentFreqEnd != initialFreqEnd)
			Simulation.setFastHenryEndFrequency(currentFreqEnd);

		int currentRunsPerDecade = TextUtils.atoi(fhRunsPerDecade.getText());
		if (currentRunsPerDecade != initialRunsPerDecade)
			Simulation.setFastHenryRunsPerDecade(currentRunsPerDecade);

		boolean currentMultiPole = fhMakeMultipole.isSelected();
		if (currentMultiPole != initialMultiPole)
			Simulation.setFastHenryMultiPole(currentMultiPole);

		int currentNumPoles = TextUtils.atoi(fhNumberOfPoles.getText());
		if (currentNumPoles != initialNumPoles)
			Simulation.setFastHenryNumPoles(currentNumPoles);

		double currentDefThickness = TextUtils.atof(fhDefaultThickness.getText());
		if (currentDefThickness != initialDefThickness)
			Simulation.setFastHenryDefThickness(currentDefThickness);

		int currentWidthSubdivisions = TextUtils.atoi(fhDefaultWidthSubs.getText());
		if (currentWidthSubdivisions != initialWidthSubdivisions)
			Simulation.setFastHenryWidthSubdivisions(currentWidthSubdivisions);

		int currentHeightSubdivisions = TextUtils.atoi(fhDefaultHeightSubs.getText());
		if (currentHeightSubdivisions != initialHeightSubdivisions)
			Simulation.setFastHenryHeightSubdivisions(currentHeightSubdivisions);

		double currentMaxSegLength = TextUtils.atof(fhMaxSegmentLength.getText());
		if (currentMaxSegLength != initialMaxSegLength)
			Simulation.setFastHenryMaxSegLength(currentMaxSegLength);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        fastHenry = new javax.swing.JPanel();
        fhUseSingleFrequency = new javax.swing.JCheckBox();
        jLabel55 = new javax.swing.JLabel();
        fhFreqEndLabel = new javax.swing.JLabel();
        fhRunsPerDecadeLabel = new javax.swing.JLabel();
        fhMakeMultipole = new javax.swing.JCheckBox();
        fhNumPolesLabel = new javax.swing.JLabel();
        fhFrequencyStart = new javax.swing.JTextField();
        fhFrequencyEnd = new javax.swing.JTextField();
        fhRunsPerDecade = new javax.swing.JTextField();
        fhNumberOfPoles = new javax.swing.JTextField();
        jLabel59 = new javax.swing.JLabel();
        jLabel60 = new javax.swing.JLabel();
        jLabel61 = new javax.swing.JLabel();
        jLabel62 = new javax.swing.JLabel();
        fhDefaultThickness = new javax.swing.JTextField();
        fhDefaultWidthSubs = new javax.swing.JTextField();
        fhDefaultHeightSubs = new javax.swing.JTextField();
        fhMaxSegmentLength = new javax.swing.JTextField();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Tool Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        fastHenry.setLayout(new java.awt.GridBagLayout());

        fhUseSingleFrequency.setText("Use single frequency");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhUseSingleFrequency, gridBagConstraints);

        jLabel55.setText("Frequency start:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        fastHenry.add(jLabel55, gridBagConstraints);

        fhFreqEndLabel.setText("Frequency end:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        fastHenry.add(fhFreqEndLabel, gridBagConstraints);

        fhRunsPerDecadeLabel.setText("Runs per decade:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        fastHenry.add(fhRunsPerDecadeLabel, gridBagConstraints);

        fhMakeMultipole.setText("Make multipole subcircuit");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhMakeMultipole, gridBagConstraints);

        fhNumPolesLabel.setText("Number of poles:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        fastHenry.add(fhNumPolesLabel, gridBagConstraints);

        fhFrequencyStart.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhFrequencyStart, gridBagConstraints);

        fhFrequencyEnd.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhFrequencyEnd, gridBagConstraints);

        fhRunsPerDecade.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhRunsPerDecade, gridBagConstraints);

        fhNumberOfPoles.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhNumberOfPoles, gridBagConstraints);

        jLabel59.setText("Default thickness:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(jLabel59, gridBagConstraints);

        jLabel60.setText("Default width subdivisions:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(jLabel60, gridBagConstraints);

        jLabel61.setText("Default height subdivisions:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(jLabel61, gridBagConstraints);

        jLabel62.setText("Maximum segment length:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(jLabel62, gridBagConstraints);

        fhDefaultThickness.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhDefaultThickness, gridBagConstraints);

        fhDefaultWidthSubs.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhDefaultWidthSubs, gridBagConstraints);

        fhDefaultHeightSubs.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhDefaultHeightSubs, gridBagConstraints);

        fhMaxSegmentLength.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhMaxSegmentLength, gridBagConstraints);

        getContentPane().add(fastHenry, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel fastHenry;
    private javax.swing.JTextField fhDefaultHeightSubs;
    private javax.swing.JTextField fhDefaultThickness;
    private javax.swing.JTextField fhDefaultWidthSubs;
    private javax.swing.JLabel fhFreqEndLabel;
    private javax.swing.JTextField fhFrequencyEnd;
    private javax.swing.JTextField fhFrequencyStart;
    private javax.swing.JCheckBox fhMakeMultipole;
    private javax.swing.JTextField fhMaxSegmentLength;
    private javax.swing.JLabel fhNumPolesLabel;
    private javax.swing.JTextField fhNumberOfPoles;
    private javax.swing.JTextField fhRunsPerDecade;
    private javax.swing.JLabel fhRunsPerDecadeLabel;
    private javax.swing.JCheckBox fhUseSingleFrequency;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel59;
    private javax.swing.JLabel jLabel60;
    private javax.swing.JLabel jLabel61;
    private javax.swing.JLabel jLabel62;
    // End of variables declaration//GEN-END:variables

}
