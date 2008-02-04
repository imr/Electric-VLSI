/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GDS.java
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Class to handle the "GDS" tab of the Numeric Technology Editor dialog.
 */
public class GDS extends TechEditWizardPanel
{
    private JPanel gds;
    private JLabel [] metalGDSLabel, viaGDSLabel;
    private JTextField [] metalGDS, viaGDS;
    private JTextField diffGDS, polyGDS, nPlusGDS, pPlusGDS, nWellGDS, contactGDS, markingGDS;
    private int numMetals;

    /** Creates new form GDS */
	public GDS(TechEditWizard parent, boolean modal)
	{
		super(parent, modal);

        setTitle("GDS");
        setName("");

        gds = new JPanel();
        gds.setLayout(new GridBagLayout());
		TechEditWizardData data = getNumericData();

        JLabel heading = new JLabel("GDS Layer Numbers");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(4, 4, 4, 4);
        gds.add(heading, gbc);

		JLabel lab1 = new JLabel("Active GDS layer:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 1, 0);
        gds.add(lab1, gbc);

        diffGDS = new JTextField();
        diffGDS.setColumns(8);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;   gbc.gridy = 1;
        gbc.insets = new Insets(4, 0, 1, 2);
        gds.add(diffGDS, gbc);

		JLabel lab2 = new JLabel("Poly GDS layer:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 1, 0);
        gds.add(lab2, gbc);

        polyGDS = new JTextField();
        polyGDS.setColumns(8);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;   gbc.gridy = 2;
        gbc.insets = new Insets(4, 0, 1, 2);
        gds.add(polyGDS, gbc);

		JLabel lab3 = new JLabel("NPlus GDS layer:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 1, 0);
        gds.add(lab3, gbc);

        nPlusGDS = new JTextField();
        nPlusGDS.setColumns(8);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;   gbc.gridy = 3;
        gbc.insets = new Insets(4, 0, 1, 2);
        gds.add(nPlusGDS, gbc);

		JLabel lab4 = new JLabel("PPlus GDS layer:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 1, 0);
        gds.add(lab4, gbc);

        pPlusGDS = new JTextField();
        pPlusGDS.setColumns(8);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;   gbc.gridy = 4;
        gbc.insets = new Insets(4, 0, 1, 2);
        gds.add(pPlusGDS, gbc);

		JLabel lab5 = new JLabel("NWell GDS layer:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 1, 0);
        gds.add(lab5, gbc);

        nWellGDS = new JTextField();
        nWellGDS.setColumns(8);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;   gbc.gridy = 5;
        gbc.insets = new Insets(4, 0, 1, 2);
        gds.add(nWellGDS, gbc);

		JLabel lab6 = new JLabel("Contact GDS layer:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 1, 0);
        gds.add(lab6, gbc);

        contactGDS = new JTextField();
        contactGDS.setColumns(8);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;   gbc.gridy = 6;
        gbc.insets = new Insets(4, 0, 1, 2);
        gds.add(contactGDS, gbc);

		JLabel lab7 = new JLabel("Marking GDS layer:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = 7;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 1, 0);
        gds.add(lab7, gbc);

        markingGDS = new JTextField();
        markingGDS.setColumns(8);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;   gbc.gridy = 7;
        gbc.insets = new Insets(4, 0, 1, 2);
        gds.add(markingGDS, gbc);
        
		numMetals = data.getNumMetalLayers();
		metalGDSLabel = new JLabel[numMetals];
		metalGDS = new JTextField[numMetals];
		viaGDSLabel = new JLabel[numMetals-1];
		viaGDS = new JTextField[numMetals-1];
        for(int i=0; i<numMetals; i++)
    	{
        	metalGDSLabel[i] = new JLabel("Metal-" + (i+1) + " GDS layer:");
        	gbc = new GridBagConstraints();
        	gbc.gridx = 0;   gbc.gridy = 8+i*2;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(4, 4, 1, 0);
            gds.add(metalGDSLabel[i], gbc);

            metalGDS[i] = new JTextField();
            metalGDS[i].setColumns(8);
        	gbc = new GridBagConstraints();
        	gbc.gridx = 1;   gbc.gridy = 8+i*2;
            gbc.insets = new Insets(4, 0, 1, 2);
            gds.add(metalGDS[i], gbc);

            if (i < numMetals-1)
            {
	            viaGDSLabel[i] = new JLabel("Via-" + (i+1) + " GDS layer:");
	        	gbc = new GridBagConstraints();
	        	gbc.gridx = 0;   gbc.gridy = 9+i*2;
	            gbc.anchor = GridBagConstraints.WEST;
	            gbc.insets = new Insets(4, 4, 1, 0);
	            gds.add(viaGDSLabel[i], gbc);

	            viaGDS[i] = new JTextField();
	            viaGDS[i].setColumns(8);
	        	gbc = new GridBagConstraints();
	        	gbc.gridx = 1;   gbc.gridy = 9+i*2;
	            gbc.insets = new Insets(4, 0, 1, 2);
	            gds.add(viaGDS[i], gbc);
            }
    	}
	}

	/** return the panel to use for this Numeric Technology Editor tab. */
	public JPanel getPanel() { return gds; }

	/** return the name of this Numeric Technology Editor tab. */
	public String getName() { return "GDS"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the GDS tab.
	 */
	public void init()
	{
		TechEditWizardData data = getNumericData();
		diffGDS.setText(Integer.toString(data.getGDSDiff()));
		polyGDS.setText(Integer.toString(data.getGDSPoly()));
		nPlusGDS.setText(Integer.toString(data.getGDSNPlus()));
		pPlusGDS.setText(Integer.toString(data.getGDSPPlus()));
		nWellGDS.setText(Integer.toString(data.getGDSNWell()));
		contactGDS.setText(Integer.toString(data.getGDSContact()));
		markingGDS.setText(Integer.toString(data.getGDSMarking()));
        for(int i=0; i<numMetals; i++)
        {
        	metalGDS[i].setText(Integer.toString(data.getGDSMetal()[i]));
        	if (i < numMetals-1)
        		viaGDS[i].setText(Integer.toString(data.getGDSVia()[i]));
        }
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the GDS tab.
	 */
	public void term()
	{
		TechEditWizardData data = getNumericData();
        int [] newMetalGDS = new int[numMetals];
        int [] newViaGDS = new int[numMetals-1];
        for(int i=0; i<numMetals; i++)
        {
        	newMetalGDS[i] = TextUtils.atoi(metalGDS[i].getText());
        	if (i < numMetals-1)
        		newViaGDS[i] = TextUtils.atoi(viaGDS[i].getText());
        }
		data.setGDSMetal(newMetalGDS);
		data.setGDSVia(newViaGDS);
		data.setGDSDiff(TextUtils.atoi(diffGDS.getText()));
		data.setGDSPoly(TextUtils.atoi(polyGDS.getText()));
		data.setGDSNPlus(TextUtils.atoi(nPlusGDS.getText()));
		data.setGDSPPlus(TextUtils.atoi(pPlusGDS.getText()));
		data.setGDSNWell(TextUtils.atoi(nWellGDS.getText()));
		data.setGDSContact(TextUtils.atoi(contactGDS.getText()));
		data.setGDSMarking(TextUtils.atoi(markingGDS.getText()));
	}
}
