/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PlacementTab.java
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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

import com.sun.electric.tool.placement.Placement;
import com.sun.electric.tool.placement.PlacementFrame;

import java.awt.Frame;
import java.util.List;

import javax.swing.JPanel;

public class PlacementTab extends PreferencePanel
{
	/** Creates new form PlacementTab */
	public PlacementTab(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for user preferences. */
	public JPanel getUserPreferencesPanel() { return frame; }

	/** return the name of this preferences tab. */
	public String getName() { return "Placement"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Frame tab.
	 */
	public void init()
	{
		PlacementFrame [] algorithms = PlacementFrame.getPlacementAlgorithms();
		for(PlacementFrame an : algorithms)
			placementAlgorithm.addItem(an.getAlgorithmName());
		placementAlgorithm.setSelectedItem(Placement.getAlgorithmName());
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Frame tab.
	 */
	public void term()
	{
		String algorithm = (String)placementAlgorithm.getSelectedItem();
		if (!algorithm.equals(Placement.getAlgorithmName()))
			Placement.setAlgorithmName(algorithm);
	}

	/**
	 * Method called when the factory reset is requested.
	 */
	public void reset()
	{
		if (!Placement.getFactoryAlgorithmName().equals(Placement.getAlgorithmName()))
			Placement.setAlgorithmName(Placement.getFactoryAlgorithmName());
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        frame = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        placementAlgorithm = new javax.swing.JComboBox();

        setTitle("Edit Options");
        setName(""); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        frame.setLayout(new java.awt.GridBagLayout());

        jLabel15.setText("Placement algorithm:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        frame.add(jLabel15, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        frame.add(placementAlgorithm, gridBagConstraints);

        getContentPane().add(frame, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel frame;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JComboBox placementAlgorithm;
    // End of variables declaration//GEN-END:variables
}