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

    private class LabelContainer
    {
        JLabel label;
        JTextField valueField;
        JTextField typeField;
    }

    /** Creates new form GDS */
	public GDS(TechEditWizard parent, boolean modal)
	{
		super(parent, modal);

        setTitle("GDS");
        setName("");

        gds = new JPanel();
        gds.setLayout(new GridBagLayout());

        // Head
        JLabel heading = new JLabel("Layer Name");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(4, 4, 4, 4);
        gds.add(heading, gbc);

        heading = new JLabel("GDS Number");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;   gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(4, 4, 4, 4);
        gds.add(heading, gbc);

        heading = new JLabel("GDS Datatype");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;   gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(4, 4, 4, 4);
        gds.add(heading, gbc);

        LabelContainer cont = new LabelContainer();
            addRow(cont, "Active", 1, 4, 0);
            diffGDS = cont.valueField;

        cont = new LabelContainer();
            addRow(cont, "Poly", 2, 4, 0);
            polyGDS = cont.valueField;

        cont = new LabelContainer();
            addRow(cont, "NPlus", 3, 4, 0);
            nPlusGDS = cont.valueField;

        cont = new LabelContainer();
            addRow(cont, "PPlus", 4, 4, 0);
            pPlusGDS = cont.valueField;

        cont = new LabelContainer();
            addRow(cont, "NWell", 5, 4, 0);
            nWellGDS = cont.valueField;

        cont = new LabelContainer();
            addRow(cont, "Contact", 6, 4, 0);
            contactGDS = cont.valueField;

        cont = new LabelContainer();
            addRow(cont, "Marking", 7, 4, 0);
            markingGDS = cont.valueField;
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
		TechEditWizardData data = wizard.getTechEditData();
		diffGDS.setText(Integer.toString(data.getGDSDiff()));
		polyGDS.setText(Integer.toString(data.getGDSPoly()));
		nPlusGDS.setText(Integer.toString(data.getGDSNPlus()));
		pPlusGDS.setText(Integer.toString(data.getGDSPPlus()));
		nWellGDS.setText(Integer.toString(data.getGDSNWell()));
		contactGDS.setText(Integer.toString(data.getGDSContact()));
		markingGDS.setText(Integer.toString(data.getGDSMarking()));

		// remove former metal data
		if (metalGDSLabel != null) for(int i=0; i<metalGDSLabel.length; i++) gds.remove(metalGDSLabel[i]);
		if (metalGDS != null) for(int i=0; i<metalGDS.length; i++) gds.remove(metalGDS[i]);
		if (viaGDSLabel != null) for(int i=0; i<viaGDSLabel.length; i++) gds.remove(viaGDSLabel[i]);
		if (viaGDS != null) for(int i=0; i<viaGDS.length; i++) gds.remove(viaGDS[i]);

		// add appropriate number of metal layers
		int numMetals = data.getNumMetalLayers();
		metalGDSLabel = new JLabel[numMetals];
		metalGDS = new JTextField[numMetals];
		viaGDSLabel = new JLabel[numMetals-1];
		viaGDS = new JTextField[numMetals-1];
        for(int i=0; i<numMetals; i++)
    	{
            LabelContainer cont = new LabelContainer();
            addRow(cont, "Metal-" + (i+1), 8+i*2, 4, data.getGDSMetal()[i]);
            metalGDSLabel[i] = cont.label;
            metalGDS[i] = cont.valueField;

            if (i < numMetals-1)
            {
                cont = new LabelContainer();
                addRow(cont, "Via-" + (i+1), 9+i*2, 10, data.getGDSVia()[i]);
                viaGDSLabel[i] = cont.label;
                viaGDS[i] = cont.valueField;
            }
    	}
	}

    private void addRow(LabelContainer cont, String labelName, int posY, int deltaY, int gdsValue)
    {
        cont.label = new JLabel(labelName);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = posY;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, deltaY, 1, 0);
        gds.add(cont.label, gbc);

        cont.valueField = new JTextField();
        cont.valueField.setText(Integer.toString(gdsValue));
        cont.valueField.setColumns(8);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;   gbc.gridy = posY;
        gbc.insets = new Insets(4, 0, 1, 2);
        gds.add(cont.valueField, gbc);

        cont.typeField = new JTextField();
//        cont.valueField.setText(Integer.toString(gdsValue));
        cont.typeField.setColumns(8);
        gbc = new GridBagConstraints();
        gbc.gridx = 2;   gbc.gridy = posY;
        gbc.insets = new Insets(4, 0, 1, 2);
        gds.add(cont.typeField, gbc);
    }

    /**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the GDS tab.
	 */
	public void term()
	{
		TechEditWizardData data = wizard.getTechEditData();
		int numMetals = data.getNumMetalLayers();
        for(int i=0; i<numMetals; i++)
        {
        	data.setGDSMetal(i, TextUtils.atoi(metalGDS[i].getText()));
        	if (i < numMetals-1)
        		data.setGDSVia(i, TextUtils.atoi(viaGDS[i].getText()));
        }
		data.setGDSDiff(TextUtils.atoi(diffGDS.getText()));
		data.setGDSPoly(TextUtils.atoi(polyGDS.getText()));
		data.setGDSNPlus(TextUtils.atoi(nPlusGDS.getText()));
		data.setGDSPPlus(TextUtils.atoi(pPlusGDS.getText()));
		data.setGDSNWell(TextUtils.atoi(nWellGDS.getText()));
		data.setGDSContact(TextUtils.atoi(contactGDS.getText()));
		data.setGDSMarking(TextUtils.atoi(markingGDS.getText()));
	}
}
