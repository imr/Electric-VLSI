/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UnitsTab.java
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
import com.sun.electric.tool.user.User;

import javax.swing.JPanel;

/**
 * Class to handle the "Units" tab of the Preferences dialog.
 */
public class UnitsTab extends PreferencePanel
{
	/** Creates new form Edit Options */
	public UnitsTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return units; }

	/** return the name of this preferences tab. */
	public String getName() { return "Units"; }

	private TextUtils.UnitScale initialUnitsDistance;
	private TextUtils.UnitScale initialUnitsResistance;
	private TextUtils.UnitScale initialUnitsCapacitance;
	private TextUtils.UnitScale initialUnitsInductance;
	private TextUtils.UnitScale initialUnitsAmperage;
	private TextUtils.UnitScale initialUnitsVoltage;
	private TextUtils.UnitScale initialUnitsTime;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Units tab.
	 */
	public void init()
	{
		unitsDistance.addItem("Millimeters");
		unitsDistance.addItem("Microns");
		unitsDistance.addItem("Nanometers");
		initialUnitsDistance = User.getDistanceUnits();
		int index = initialUnitsDistance.getIndex() - TextUtils.UnitScale.MILLI.getIndex();
		if (index < 0) index = 0;
		if (index >= unitsDistance.getItemCount()) index = unitsDistance.getItemCount() - 1;
		unitsDistance.setSelectedIndex(index);

		unitsResistance.addItem("Giga-ohms");
		unitsResistance.addItem("Mega-ohms");
		unitsResistance.addItem("Kilo-ohms");
		unitsResistance.addItem("Ohms");
		initialUnitsResistance = User.getResistanceUnits();
		index = initialUnitsResistance.getIndex() - TextUtils.UnitScale.GIGA.getIndex();
		if (index < 0) index = 0;
		if (index >= unitsDistance.getItemCount()) index = unitsDistance.getItemCount() - 1;
		unitsResistance.setSelectedIndex(index);

		unitsCapacitance.addItem("Farads");
		unitsCapacitance.addItem("Milli-farads");
		unitsCapacitance.addItem("Micro-farads");
		unitsCapacitance.addItem("Nano-farads");
		unitsCapacitance.addItem("Pico-farads");
		unitsCapacitance.addItem("Femto-farads");
		initialUnitsCapacitance = User.getCapacitanceUnits();
		index = initialUnitsCapacitance.getIndex() - TextUtils.UnitScale.NONE.getIndex();
		if (index < 0) index = 0;
		if (index >= unitsDistance.getItemCount()) index = unitsDistance.getItemCount() - 1;
		unitsCapacitance.setSelectedIndex(index);

		unitsInductance.addItem("Henrys");
		unitsInductance.addItem("Milli-henrys");
		unitsInductance.addItem("Micro-henrys");
		unitsInductance.addItem("Nano-henrys");
		initialUnitsInductance = User.getInductanceUnits();
		index = initialUnitsInductance.getIndex() - TextUtils.UnitScale.NONE.getIndex();
		if (index < 0) index = 0;
		if (index >= unitsDistance.getItemCount()) index = unitsDistance.getItemCount() - 1;
		unitsInductance.setSelectedIndex(index);

		unitsCurrent.addItem("Amps");
		unitsCurrent.addItem("Milli-amps");
		unitsCurrent.addItem("Micro-amps");
		initialUnitsAmperage = User.getAmperageUnits();
		index = initialUnitsAmperage.getIndex() - TextUtils.UnitScale.NONE.getIndex();
		if (index < 0) index = 0;
		if (index >= unitsDistance.getItemCount()) index = unitsDistance.getItemCount() - 1;
		unitsCurrent.setSelectedIndex(index);

		unitsVoltage.addItem("Kilo-volts");
		unitsVoltage.addItem("Volts");
		unitsVoltage.addItem("Milli-volts");
		unitsVoltage.addItem("Micro-volts");
		initialUnitsVoltage = User.getVoltageUnits();
		index = initialUnitsVoltage.getIndex() - TextUtils.UnitScale.KILO.getIndex();
		if (index < 0) index = 0;
		if (index >= unitsDistance.getItemCount()) index = unitsDistance.getItemCount() - 1;
		unitsVoltage.setSelectedIndex(index);

		unitsTime.addItem("Seconds");
		unitsTime.addItem("Milli-seconds");
		unitsTime.addItem("Micro-seconds");
		unitsTime.addItem("Nano-seconds");
		unitsTime.addItem("Pico-seconds");
		unitsTime.addItem("Femto-seconds");
		initialUnitsTime = User.getTimeUnits();
		index = initialUnitsTime.getIndex() - TextUtils.UnitScale.NONE.getIndex();
		if (index < 0) index = 0;
		if (index >= unitsDistance.getItemCount()) index = unitsDistance.getItemCount() - 1;
		unitsTime.setSelectedIndex(index);

        // Units no longer mean anything - all numbers are in real units, and are
        // displayed scaled with appropriate post-fix.
        index = TextUtils.UnitScale.NONE.getIndex();
        unitsResistance.setSelectedIndex(-1);
        unitsResistance.setEnabled(false);
        unitsCapacitance.setSelectedIndex(-1);
        unitsCapacitance.setEnabled(false);
        unitsInductance.setSelectedIndex(-1);
        unitsInductance.setEnabled(false);
        unitsCurrent.setSelectedIndex(-1);
        unitsCurrent.setEnabled(false);
        unitsVoltage.setSelectedIndex(-1);
        unitsVoltage.setEnabled(false);
        unitsTime.setSelectedIndex(-1);
        unitsTime.setEnabled(false);

	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Units tab.
	 */
	public void term()
	{
		TextUtils.UnitScale currentDistance = TextUtils.UnitScale.findFromIndex(unitsDistance.getSelectedIndex());
		if (currentDistance != initialUnitsDistance)
			User.setDistanceUnits(currentDistance);

		TextUtils.UnitScale currentResistance = TextUtils.UnitScale.findFromIndex(unitsResistance.getSelectedIndex());
		if (currentResistance != initialUnitsResistance)
			User.setResistanceUnits(currentResistance);

		TextUtils.UnitScale currentCapacitance = TextUtils.UnitScale.findFromIndex(unitsCapacitance.getSelectedIndex());
		if (currentCapacitance != initialUnitsCapacitance)
			User.setCapacitanceUnits(currentCapacitance);

		TextUtils.UnitScale currentInductance = TextUtils.UnitScale.findFromIndex(unitsInductance.getSelectedIndex());
		if (currentInductance != initialUnitsInductance)
			User.setInductanceUnits(currentInductance);

		TextUtils.UnitScale currentAmperage = TextUtils.UnitScale.findFromIndex(unitsCurrent.getSelectedIndex());
		if (currentAmperage != initialUnitsAmperage)
			User.setAmperageUnits(currentAmperage);

		TextUtils.UnitScale currentVoltage = TextUtils.UnitScale.findFromIndex(unitsVoltage.getSelectedIndex());
		if (currentVoltage != initialUnitsVoltage)
			User.setVoltageUnits(currentVoltage);

		TextUtils.UnitScale currentTime = TextUtils.UnitScale.findFromIndex(unitsTime.getSelectedIndex());
		if (currentTime != initialUnitsTime)
			User.setTimeUnits(currentTime);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        units = new javax.swing.JPanel();
        jLabel39 = new javax.swing.JLabel();
        unitsDistance = new javax.swing.JComboBox();
        jLabel40 = new javax.swing.JLabel();
        unitsResistance = new javax.swing.JComboBox();
        jLabel50 = new javax.swing.JLabel();
        unitsCapacitance = new javax.swing.JComboBox();
        jLabel51 = new javax.swing.JLabel();
        unitsInductance = new javax.swing.JComboBox();
        jLabel63 = new javax.swing.JLabel();
        unitsCurrent = new javax.swing.JComboBox();
        jLabel64 = new javax.swing.JLabel();
        unitsVoltage = new javax.swing.JComboBox();
        jLabel65 = new javax.swing.JLabel();
        unitsTime = new javax.swing.JComboBox();
        jLabel66 = new javax.swing.JLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Edit Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        units.setLayout(new java.awt.GridBagLayout());

        jLabel39.setText("Distance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(jLabel39, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(unitsDistance, gridBagConstraints);

        jLabel40.setText("Resistance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(jLabel40, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(unitsResistance, gridBagConstraints);

        jLabel50.setText("Capacitance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(jLabel50, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(unitsCapacitance, gridBagConstraints);

        jLabel51.setText("Inductance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(jLabel51, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(unitsInductance, gridBagConstraints);

        jLabel63.setText("Current:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(jLabel63, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(unitsCurrent, gridBagConstraints);

        jLabel64.setText("Voltage:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(jLabel64, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(unitsVoltage, gridBagConstraints);

        jLabel65.setText("Time:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(jLabel65, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(unitsTime, gridBagConstraints);

        jLabel66.setText("These units will be used for display");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(jLabel66, gridBagConstraints);

        getContentPane().add(units, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel63;
    private javax.swing.JLabel jLabel64;
    private javax.swing.JLabel jLabel65;
    private javax.swing.JLabel jLabel66;
    private javax.swing.JPanel units;
    private javax.swing.JComboBox unitsCapacitance;
    private javax.swing.JComboBox unitsCurrent;
    private javax.swing.JComboBox unitsDistance;
    private javax.swing.JComboBox unitsInductance;
    private javax.swing.JComboBox unitsResistance;
    private javax.swing.JComboBox unitsTime;
    private javax.swing.JComboBox unitsVoltage;
    // End of variables declaration//GEN-END:variables

}
