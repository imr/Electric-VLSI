/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PlacementTab.java
 *
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.electric.tool.placement.PlacementAdapter;
import com.sun.electric.tool.placement.PlacementFrame;
import com.sun.electric.tool.placement.PlacementFrame.PlacementParameter;
import com.sun.electric.tool.user.dialogs.PreferencesFrame;
import com.sun.electric.util.TextUtils;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class PlacementTab extends PreferencePanel
{
	private PreferencesFrame parent;
	private Map<PlacementParameter,JComponent> currentParameters;
    private Placement.PlacementPreferences placementOptions;

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
    @Override
	public void init()
	{
		PlacementFrame [] algorithms = PlacementAdapter.getPlacementAlgorithms();
		for(PlacementFrame an : algorithms)
			placementAlgorithm.addItem(an.getAlgorithmName());
        placementOptions = new Placement.PlacementPreferences(false);
		placementAlgorithm.setSelectedItem(placementOptions.placementAlgorithm);
		placementAlgorithm.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) { getAlgorithmParameters();   setupForAlgorithm(); }
        });


		// show parameters for current algorithm
		setupForAlgorithm();
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Frame tab.
	 */
    @Override
	public void term()
	{
        placementOptions.placementAlgorithm = (String)placementAlgorithm.getSelectedItem();

        // load values into temporary parameters
		getAlgorithmParameters();

		// set parameters in all algorithms
        putPrefs(placementOptions);
	}

	/**
	 * Method called when the factory reset is requested.
	 */
    @Override
	public void reset()
	{
		// reset parameters in experimental algorithms
        putPrefs(new Placement.PlacementPreferences(true));
	}

	private void getAlgorithmParameters()
	{
		if (currentParameters == null) return;

		// get the parameters
		for(PlacementParameter pp : currentParameters.keySet())
		{
			JComponent comp = currentParameters.get(pp);
            Object value;
            switch (pp.getType()) {
                case PlacementFrame.PlacementParameter.TYPEINTEGER:
                	if (pp.getIntMeanings() == null)
                	{
                		value = Integer.valueOf(TextUtils.atoi(((JTextField)comp).getText()));
                	} else
                	{
                		value = new Integer(((JComboBox)comp).getSelectedIndex());
                	}
                    break;
                case PlacementFrame.PlacementParameter.TYPESTRING:
                    value = String.valueOf(((JTextField)comp).getText());
                    break;
                case PlacementFrame.PlacementParameter.TYPEDOUBLE:
                    value = Double.valueOf(TextUtils.atof(((JTextField)comp).getText()));
                    break;
                case PlacementFrame.PlacementParameter.TYPEBOOLEAN:
                    value = Boolean.valueOf(((JCheckBox)comp).isSelected());
                    break;
                default:
                    throw new AssertionError();
            }
            placementOptions.setParameter(pp, value);
		}
	}

	private void setupForAlgorithm()
	{
		parametersPanel.removeAll();
		parametersPanel.updateUI();
		currentParameters = new HashMap<PlacementParameter,JComponent>();

		String algName = (String)placementAlgorithm.getSelectedItem();
		PlacementFrame [] algorithms = PlacementAdapter.getPlacementAlgorithms();
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
                    Object value = placementOptions.getParameter(pp);
                    if (pp.getType() == PlacementParameter.TYPEBOOLEAN)
					{
                    	JCheckBox cb = new JCheckBox(pp.getName());
    					GridBagConstraints gbc = new GridBagConstraints();
    					gbc.gridx = 0;   gbc.gridy = yPos;
    					gbc.gridwidth = 2;
    					gbc.insets = new Insets(4, 4, 4, 4);
    					parametersPanel.add(cb, gbc);
    					currentParameters.put(pp, cb);
    					cb.setSelected(((Boolean)value).booleanValue());
					} else if (pp.getType() == PlacementParameter.TYPEINTEGER && pp.getIntMeanings() != null)
					{
						JLabel lab = new JLabel(pp.getName());
						GridBagConstraints gbc = new GridBagConstraints();
						gbc.gridx = 0;   gbc.gridy = yPos;
						gbc.anchor = GridBagConstraints.EAST;
						gbc.insets = new Insets(4, 4, 4, 4);
						parametersPanel.add(lab, gbc);

						JComboBox cb = new JComboBox();
						String[] meanings = pp.getIntMeanings();
						for(int i=0; i<meanings.length; i++) cb.addItem(meanings[i]);
						cb.setSelectedIndex(((Integer)value).intValue());
						gbc = new GridBagConstraints();
						gbc.gridx = 1;   gbc.gridy = yPos;
						gbc.anchor = GridBagConstraints.WEST;
						gbc.fill = GridBagConstraints.HORIZONTAL;
						gbc.insets = new Insets(4, 4, 4, 4);
						parametersPanel.add(cb, gbc);
						currentParameters.put(pp, cb);
					} else
					{
						JLabel lab = new JLabel(pp.getName());
						GridBagConstraints gbc = new GridBagConstraints();
						gbc.gridx = 0;   gbc.gridy = yPos;
						gbc.anchor = GridBagConstraints.EAST;
						gbc.insets = new Insets(4, 4, 4, 4);
						parametersPanel.add(lab, gbc);

						String init = null;
						if (pp.getType() == PlacementParameter.TYPEINTEGER)
						{
	                        init = value.toString();
						} else if (pp.getType() == PlacementParameter.TYPESTRING)
						{
	                        init = value.toString();
						} else if (pp.getType() == PlacementParameter.TYPEDOUBLE)
						{
							init = TextUtils.formatDouble(((Double)value).doubleValue());
						}
						JTextField txt = new JTextField(init);
						txt.setColumns(init.length()*2);
						gbc = new GridBagConstraints();
						gbc.gridx = 1;   gbc.gridy = yPos;
						gbc.anchor = GridBagConstraints.WEST;
						gbc.fill = GridBagConstraints.HORIZONTAL;
						gbc.insets = new Insets(4, 4, 4, 4);
						parametersPanel.add(txt, gbc);
						currentParameters.put(pp, txt);
					}

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
