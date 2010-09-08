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
import com.sun.electric.tool.placement.PlacementFrame.PlacementParameter;
import com.sun.electric.tool.user.dialogs.PreferencesFrame;
import com.sun.electric.util.TextUtils;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class PlacementTab extends PreferencePanel
{
	private PreferencesFrame parent;
	private Map<PlacementParameter,JTextField> currentParameters;

	/** Creates new form PlacementTab */
	public PlacementTab(PreferencesFrame parent, boolean modal)
	{
		super(parent, modal);
		this.parent = parent;
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
		placementAlgorithm.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) { getAlgorithmParameters();   setupForAlgorithm(); }
        });

		// reset temp parameters in all algorithms
		for(PlacementFrame alg : algorithms)
		{
			List<PlacementParameter> params = alg.getParameters();
			if (params == null) continue;
			for(PlacementParameter pp : params)
				pp.clearTempValue();
		}

		// show parameters for current algorithm
		setupForAlgorithm();
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

		// load values into temp parameters
		getAlgorithmParameters();

		// set parameters in all algorithms
		PlacementFrame [] algorithms = PlacementFrame.getPlacementAlgorithms();
		for(PlacementFrame alg : algorithms)
		{
			List<PlacementParameter> params = alg.getParameters();
			if (params == null) continue;
			for(PlacementParameter pp : params)
				pp.makeTempSettingReal();
		}
	}

	/**
	 * Method called when the factory reset is requested.
	 */
	public void reset()
	{
		if (!Placement.getFactoryAlgorithmName().equals(Placement.getAlgorithmName()))
			Placement.setAlgorithmName(Placement.getFactoryAlgorithmName());

		// reset parameters in all algorithms
		PlacementFrame [] algorithms = PlacementFrame.getPlacementAlgorithms();
		for(PlacementFrame alg : algorithms)
		{
			List<PlacementParameter> params = alg.getParameters();
			if (params == null) continue;
			for(PlacementParameter pp : params)
				pp.resetToFactory();
		}
	}

	private void getAlgorithmParameters()
	{
		if (currentParameters == null) return;

		// get the parameters
		for(PlacementParameter pp : currentParameters.keySet())
		{
			JTextField txt = currentParameters.get(pp);
			if (pp.getType() == PlacementParameter.TYPEINTEGER)
			{
				pp.setTempIntValue(TextUtils.atoi(txt.getText()));
			} else if (pp.getType() == PlacementParameter.TYPESTRING)
			{
				pp.setTempStringValue(txt.getText());
			} else if (pp.getType() == PlacementParameter.TYPEDOUBLE)
			{
				pp.setTempDoubleValue(TextUtils.atof(txt.getText()));
			}
		}
	}

	private void setupForAlgorithm()
	{
		parametersPanel.removeAll();
		parametersPanel.updateUI();
		currentParameters = new HashMap<PlacementParameter,JTextField>();

		String algName = (String)placementAlgorithm.getSelectedItem();
		PlacementFrame [] algorithms = PlacementFrame.getPlacementAlgorithms();
		PlacementFrame whichOne = null;
		for(PlacementFrame an : algorithms)
		{
			if (algName.equals(an.getAlgorithmName())) { whichOne = an;   break; }
		}
		if (whichOne != null)
		{
			List<PlacementParameter> allParams = whichOne.getParameters();
			if (allParams != null)
			{
				// load the parameters
				int yPos = 0;
				for(PlacementParameter pp : allParams)
				{
					JLabel lab = new JLabel(pp.getName());
					GridBagConstraints gbc = new GridBagConstraints();
					gbc.gridx = 0;   gbc.gridy = yPos;
					gbc.insets = new java.awt.Insets(4, 4, 4, 4);
					parametersPanel.add(lab, gbc);

					String init = null;
					if (pp.getType() == PlacementParameter.TYPEINTEGER)
					{
						init = "" + (pp.hasTempValue() ? pp.getTempIntValue() : pp.getIntValue());
					} else if (pp.getType() == PlacementParameter.TYPESTRING)
					{
						init = pp.hasTempValue() ? pp.getTempStringValue() : pp.getStringValue();
					} else if (pp.getType() == PlacementParameter.TYPEDOUBLE)
					{
						init = TextUtils.formatDouble(pp.hasTempValue() ? pp.getTempDoubleValue() : pp.getDoubleValue());
					}
					JTextField txt = new JTextField(init);
					txt.setColumns(init.length()*2);
					gbc = new GridBagConstraints();
					gbc.gridx = 1;   gbc.gridy = yPos;
					gbc.insets = new java.awt.Insets(4, 4, 4, 4);
					parametersPanel.add(txt, gbc);
					currentParameters.put(pp, txt);

					yPos++;
				}
			}
		}
		parent.pack();
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
        parametersPanel = new javax.swing.JPanel();

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
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        frame.add(jLabel15, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        frame.add(placementAlgorithm, gridBagConstraints);

        parametersPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Parameters"));
        parametersPanel.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        frame.add(parametersPanel, gridBagConstraints);

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
    private javax.swing.JPanel parametersPanel;
    private javax.swing.JComboBox placementAlgorithm;
    // End of variables declaration//GEN-END:variables
}
