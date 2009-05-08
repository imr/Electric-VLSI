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
import com.sun.electric.database.text.TextUtils.UnitScale;
import com.sun.electric.tool.user.User;

import java.awt.Frame;

import javax.swing.JPanel;

/**
 * Class to handle the "Units" tab of the Preferences dialog.
 */
public class UnitsTab extends PreferencePanel
{
	private static final boolean OTHERUNITS = false;

	private static final int DISTANCE_OFFSET = TextUtils.UnitScale.MILLI.getIndex();
	private static final int RESISTANCE_OFFSET = TextUtils.UnitScale.GIGA.getIndex();
	private static final int CAPACITANCE_OFFSET = TextUtils.UnitScale.NONE.getIndex();
	private static final int INDUCTANCE_OFFSET = TextUtils.UnitScale.NONE.getIndex();
	private static final int CURRENT_OFFSET = TextUtils.UnitScale.NONE.getIndex();
	private static final int VOLTAGE_OFFSET = TextUtils.UnitScale.KILO.getIndex();
	private static final int TIME_OFFSET = TextUtils.UnitScale.NONE.getIndex();

	private TextUtils.UnitScale initialUnitsDistance;
	private TextUtils.UnitScale initialUnitsResistance;
	private TextUtils.UnitScale initialUnitsCapacitance;
	private TextUtils.UnitScale initialUnitsInductance;
	private TextUtils.UnitScale initialUnitsAmperage;
	private TextUtils.UnitScale initialUnitsVoltage;
	private TextUtils.UnitScale initialUnitsTime;

	/** Creates new form Units Options */
	public UnitsTab(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for user preferences. */
	public JPanel getUserPreferencesPanel() { return units; }

	/** return the name of this preferences tab. */
	public String getName() { return "Units"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Units tab.
	 */
	public void init()
	{
		unitsDistance.addItem("Units (scalable)");
		unitsDistance.addItem("Millimeters");
		unitsDistance.addItem("Microns");
		unitsDistance.addItem("Nanometers");
		unitsDistance.addItem("Picometers");
		initialUnitsDistance = User.getDistanceUnits();
		int index = 0;
		if (initialUnitsDistance != null)
		{
			index = initialUnitsDistance.getIndex() - DISTANCE_OFFSET + 1;
			if (index < 0) index = 0;
			if (index >= unitsDistance.getItemCount()) index = unitsDistance.getItemCount() - 1;
		}
		unitsDistance.setSelectedIndex(index);

		if (OTHERUNITS)
		{
			unitsResistance.addItem("Giga-ohms");
			unitsResistance.addItem("Mega-ohms");
			unitsResistance.addItem("Kilo-ohms");
			unitsResistance.addItem("Ohms");
			initialUnitsResistance = User.getResistanceUnits();
			index = initialUnitsResistance.getIndex() - RESISTANCE_OFFSET;
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
			index = initialUnitsCapacitance.getIndex() - CAPACITANCE_OFFSET;
			if (index < 0) index = 0;
			if (index >= unitsDistance.getItemCount()) index = unitsDistance.getItemCount() - 1;
			unitsCapacitance.setSelectedIndex(index);

			unitsInductance.addItem("Henrys");
			unitsInductance.addItem("Milli-henrys");
			unitsInductance.addItem("Micro-henrys");
			unitsInductance.addItem("Nano-henrys");
			initialUnitsInductance = User.getInductanceUnits();
			index = initialUnitsInductance.getIndex() - INDUCTANCE_OFFSET;
			if (index < 0) index = 0;
			if (index >= unitsDistance.getItemCount()) index = unitsDistance.getItemCount() - 1;
			unitsInductance.setSelectedIndex(index);

			unitsCurrent.addItem("Amps");
			unitsCurrent.addItem("Milli-amps");
			unitsCurrent.addItem("Micro-amps");
			initialUnitsAmperage = User.getAmperageUnits();
			index = initialUnitsAmperage.getIndex() - CURRENT_OFFSET;
			if (index < 0) index = 0;
			if (index >= unitsDistance.getItemCount()) index = unitsDistance.getItemCount() - 1;
			unitsCurrent.setSelectedIndex(index);

			unitsVoltage.addItem("Kilo-volts");
			unitsVoltage.addItem("Volts");
			unitsVoltage.addItem("Milli-volts");
			unitsVoltage.addItem("Micro-volts");
			initialUnitsVoltage = User.getVoltageUnits();
			index = initialUnitsVoltage.getIndex() - VOLTAGE_OFFSET;
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
			index = initialUnitsTime.getIndex() - TIME_OFFSET;
			if (index < 0) index = 0;
			if (index >= unitsDistance.getItemCount()) index = unitsDistance.getItemCount() - 1;
			unitsTime.setSelectedIndex(index);
		} else
		{
			units.remove(unitsResistance);    units.remove(labelResistance);
			units.remove(unitsCapacitance);   units.remove(labelCapacitance);
			units.remove(unitsInductance);    units.remove(labelInductance);
			units.remove(unitsCurrent);       units.remove(labelCurrent);
			units.remove(unitsVoltage);       units.remove(labelVoltage);
			units.remove(unitsTime);          units.remove(labelTime);
		}
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Units tab.
	 */
	public void term()
	{
		int distanceIndex = unitsDistance.getSelectedIndex();
		TextUtils.UnitScale currentDistance = null;
		if (distanceIndex != 0) currentDistance = TextUtils.UnitScale.findFromIndex(distanceIndex + DISTANCE_OFFSET - 1);
		if (currentDistance != initialUnitsDistance)
			User.setDistanceUnits(currentDistance);

		if (OTHERUNITS)
		{
			TextUtils.UnitScale currentResistance = TextUtils.UnitScale.findFromIndex(unitsResistance.getSelectedIndex() + RESISTANCE_OFFSET);
			if (currentResistance != initialUnitsResistance)
				User.setResistanceUnits(currentResistance);

			TextUtils.UnitScale currentCapacitance = TextUtils.UnitScale.findFromIndex(unitsCapacitance.getSelectedIndex() + CAPACITANCE_OFFSET);
			if (currentCapacitance != initialUnitsCapacitance)
				User.setCapacitanceUnits(currentCapacitance);

			TextUtils.UnitScale currentInductance = TextUtils.UnitScale.findFromIndex(unitsInductance.getSelectedIndex() + INDUCTANCE_OFFSET);
			if (currentInductance != initialUnitsInductance)
				User.setInductanceUnits(currentInductance);

			TextUtils.UnitScale currentAmperage = TextUtils.UnitScale.findFromIndex(unitsCurrent.getSelectedIndex() + CURRENT_OFFSET);
			if (currentAmperage != initialUnitsAmperage)
				User.setAmperageUnits(currentAmperage);

			TextUtils.UnitScale currentVoltage = TextUtils.UnitScale.findFromIndex(unitsVoltage.getSelectedIndex() + VOLTAGE_OFFSET);
			if (currentVoltage != initialUnitsVoltage)
				User.setVoltageUnits(currentVoltage);

			TextUtils.UnitScale currentTime = TextUtils.UnitScale.findFromIndex(unitsTime.getSelectedIndex() + TIME_OFFSET);
			if (currentTime != initialUnitsTime)
				User.setTimeUnits(currentTime);
		}
	}

	/**
	 * Method called when the factory reset is requested.
	 */
	public void reset()
	{
		if (User.getFactoryDistanceUnits() != User.getDistanceUnits())
			User.setDistanceUnits(User.getFactoryDistanceUnits());
		if (OTHERUNITS)
		{
			if (!User.getFactoryResistanceUnits().equals(User.getResistanceUnits()))
				User.setResistanceUnits(User.getFactoryResistanceUnits());
			if (!User.getFactoryCapacitanceUnits().equals(User.getCapacitanceUnits()))
				User.setCapacitanceUnits(User.getFactoryCapacitanceUnits());
			if (!User.getFactoryInductanceUnits().equals(User.getInductanceUnits()))
				User.setInductanceUnits(User.getFactoryInductanceUnits());
			if (!User.getFactoryAmperageUnits().equals(User.getAmperageUnits()))
				User.setAmperageUnits(User.getFactoryAmperageUnits());
			if (!User.getFactoryVoltageUnits().equals(User.getVoltageUnits()))
				User.setVoltageUnits(User.getFactoryVoltageUnits());
			if (!User.getFactoryTimeUnits().equals(User.getTimeUnits()))
				User.setTimeUnits(User.getFactoryTimeUnits());
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        units = new javax.swing.JPanel();
        labelDistance = new javax.swing.JLabel();
        unitsDistance = new javax.swing.JComboBox();
        labelResistance = new javax.swing.JLabel();
        unitsResistance = new javax.swing.JComboBox();
        labelCapacitance = new javax.swing.JLabel();
        unitsCapacitance = new javax.swing.JComboBox();
        labelInductance = new javax.swing.JLabel();
        unitsInductance = new javax.swing.JComboBox();
        labelCurrent = new javax.swing.JLabel();
        unitsCurrent = new javax.swing.JComboBox();
        labelVoltage = new javax.swing.JLabel();
        unitsVoltage = new javax.swing.JComboBox();
        labelTime = new javax.swing.JLabel();
        unitsTime = new javax.swing.JComboBox();
        jLabel66 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Edit Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        units.setLayout(new java.awt.GridBagLayout());

        labelDistance.setText("Distance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(labelDistance, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(unitsDistance, gridBagConstraints);

        labelResistance.setText("Resistance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(labelResistance, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(unitsResistance, gridBagConstraints);

        labelCapacitance.setText("Capacitance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(labelCapacitance, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(unitsCapacitance, gridBagConstraints);

        labelInductance.setText("Inductance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(labelInductance, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(unitsInductance, gridBagConstraints);

        labelCurrent.setText("Current:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(labelCurrent, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(unitsCurrent, gridBagConstraints);

        labelVoltage.setText("Voltage:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(labelVoltage, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(unitsVoltage, gridBagConstraints);

        labelTime.setText("Time:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(labelTime, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(unitsTime, gridBagConstraints);

        jLabel66.setText("When real units are specified,");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        units.add(jLabel66, gridBagConstraints);

        jLabel1.setText("they are used in display and in dialogs");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        units.add(jLabel1, gridBagConstraints);

        getContentPane().add(units, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel66;
    private javax.swing.JLabel labelCapacitance;
    private javax.swing.JLabel labelCurrent;
    private javax.swing.JLabel labelDistance;
    private javax.swing.JLabel labelInductance;
    private javax.swing.JLabel labelResistance;
    private javax.swing.JLabel labelTime;
    private javax.swing.JLabel labelVoltage;
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
