/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Antenna.java
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.tecEditWizard;

import com.sun.electric.database.text.TextUtils;

import java.awt.*;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Class to handle the "Antenna" tab of the Numeric Technology Editor dialog.
 */
public class Antenna extends TechEditWizardPanel
{
    private JPanel antenna;
    private JLabel [] metalRatioLabel;
    private JTextField [] metalRatio;
    private JTextField polyRatio;

    /** Creates new form Antenna */
	public Antenna(TechEditWizard parent, boolean modal)
	{
		super(parent, modal);

        setTitle("Antenna");
        setName("");

        antenna = new JPanel();
        antenna.setLayout(new GridBagLayout());

        JLabel heading = new JLabel("Antenna Ratios");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(4, 4, 4, 4);
        antenna.add(heading, gbc);

		JLabel lab = new JLabel("Poly ratio:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 1, 0);
        antenna.add(lab, gbc);

        polyRatio = new JTextField();
        polyRatio.setColumns(8);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;   gbc.gridy = 1;
        gbc.insets = new Insets(4, 0, 1, 2);
        antenna.add(polyRatio, gbc);
	}

	/** return the panel to use for this Numeric Technology Editor tab. */
	public Component getComponent() { return antenna; }

	/** return the name of this Numeric Technology Editor tab. */
	public String getName() { return "Antenna"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Antenna tab.
	 */
	public void init()
	{
		// remove former metal data
		if (metalRatioLabel != null) for(int i=0; i<metalRatioLabel.length; i++) antenna.remove(metalRatioLabel[i]);
		if (metalRatio != null) for(int i=0; i<metalRatio.length; i++) antenna.remove(metalRatio[i]);

		// add appropriate number of metal layers
		TechEditWizardData data = wizard.getTechEditData();
		int numMetals = data.getNumMetalLayers();
		metalRatioLabel = new JLabel[numMetals];
		metalRatio = new JTextField[numMetals];
        for(int i=0; i<numMetals; i++)
    	{
        	metalRatioLabel[i] = new JLabel("Metal-" + (i+1) + " ratio:");
        	GridBagConstraints gbc = new GridBagConstraints();
        	gbc.gridx = 0;   gbc.gridy = 2+i;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(4, 4, 1, 0);
            antenna.add(metalRatioLabel[i], gbc);

            metalRatio[i] = new JTextField();
        	metalRatio[i].setText(Double.toString(data.getMetalAntennaRatio()[i]));
            metalRatio[i].setColumns(8);
        	gbc = new GridBagConstraints();
        	gbc.gridx = 1;   gbc.gridy = 2+i;
            gbc.insets = new Insets(4, 0, 1, 2);
            antenna.add(metalRatio[i], gbc);
    	}
        polyRatio.setText(Double.toString(data.getPolyAntennaRatio()));
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Antenna tab.
	 */
	public void term()
	{
		TechEditWizardData data = wizard.getTechEditData();
		int numMetals = data.getNumMetalLayers();
        for(int i=0; i<numMetals; i++)
        	data.setMetalAntennaRatio(i, TextUtils.atof(metalRatio[i].getText()));
		data.setPolyAntennaRatio(TextUtils.atof(polyRatio.getText()));
	}
}
