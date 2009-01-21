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
    private LabelContainer[] metalContainers, viaContainers;
    private LabelContainer[] basicContainers;

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

        TechEditWizardData data = wizard.getTechEditData();
        TechEditWizardData.LayerInfo[] basics = data.getBasicLayers();
        basicContainers = new LabelContainer[basics.length];

        for (int i = 0; i < basics.length; i++)
        {
            basicContainers[i] = addRow(basics[i], i + 1, 4);
        }
	}

    private void setValues(LabelContainer con, TechEditWizardData.LayerInfo info)
    {
        con.valueField.setText(Integer.toString(info.getValue()));
        con.typeField.setText(Integer.toString(info.getType()));
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
        TechEditWizardData.LayerInfo[] basics = data.getBasicLayers();

        for (int i = 0; i < basicContainers.length; i++)
        {
            setValues(basicContainers[i], basics[i]);
        }

		// remove former metal data
		if (metalContainers != null)
            for(int i=0; i<metalContainers.length; i++)
            {
                gds.remove(metalContainers[i].label);
                gds.remove(metalContainers[i].valueField);
                gds.remove(metalContainers[i].typeField);
            }

        if (viaContainers != null)
            for(int i=0; i<viaContainers.length; i++)
            {
                gds.remove(viaContainers[i].label);
                gds.remove(viaContainers[i].valueField);
                gds.remove(viaContainers[i].typeField);
            }

        // add appropriate number of metal layers
		int numMetals = data.getNumMetalLayers();
		metalContainers = new LabelContainer[numMetals];
		viaContainers = new LabelContainer[numMetals-1];
        int base = basics.length + 1;
        for(int i=0; i<numMetals; i++)
    	{
            metalContainers[i] = addRow(data.getGDSMetal()[i], base+i*2, 4);

            if (i < numMetals-1)
            {
                viaContainers[i] = addRow(data.getGDSVia()[i], base+1+i*2, 10);
            }
    	}
        this.pack();
    }

    private LabelContainer addRow(TechEditWizardData.LayerInfo gdsValue, int posY, int deltaY)
    {
        LabelContainer cont = new LabelContainer();

        cont.label = new JLabel(gdsValue.name);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = posY;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, deltaY, 1, 0);
        gds.add(cont.label, gbc);

        cont.valueField = new JTextField();
        cont.valueField.setText(Integer.toString(gdsValue.getValue()));
        cont.valueField.setColumns(8);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;   gbc.gridy = posY;
        gbc.insets = new Insets(4, 0, 1, 2);
        gds.add(cont.valueField, gbc);

        cont.typeField = new JTextField();
        cont.typeField.setText(Integer.toString(gdsValue.getType()));
        cont.typeField.setColumns(8);
        gbc = new GridBagConstraints();
        gbc.gridx = 2;   gbc.gridy = posY;
        gbc.insets = new Insets(4, 0, 1, 2);
        gds.add(cont.typeField, gbc);

        return cont;
    }

    /**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the GDS tab.
	 */
	public void term()
	{
		TechEditWizardData data = wizard.getTechEditData();
        TechEditWizardData.LayerInfo[] metalLayers = data.getGDSMetal();
        TechEditWizardData.LayerInfo[] viaLayers = data.getGDSVia();

        for(int i=0; i<metalLayers.length; i++)
        {
            metalLayers[i].setData(new int[]{TextUtils.atoi(metalContainers[i].valueField.getText()),
            TextUtils.atoi(metalContainers[i].typeField.getText())});
        	if (i < metalLayers.length-1)
                viaLayers[i].setData(new int[]{TextUtils.atoi(viaContainers[i].valueField.getText()),
            TextUtils.atoi(viaContainers[i].typeField.getText())});
        }

        TechEditWizardData.LayerInfo[] basics = data.getBasicLayers();
        for (int i = 0; i < basicContainers.length; i++)
        {
            basics[i].setData(new int[]{TextUtils.atoi(basicContainers[i].valueField.getText()),
            TextUtils.atoi(basicContainers[i].typeField.getText())});
        }
	}
}
