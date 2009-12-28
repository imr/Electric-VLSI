/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Metal.java
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
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.Resources;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

/**
 * Class to handle the "Metal" tab of the Numeric Technology Editor dialog.
 */
public class Metal extends TechEditWizardPanel
{
    private JPanel metal;
    private JScrollPane metalPane;
    private MetalContainer[] metalContainers;
    private int numMetals;
    private TechEditWizard parent;

    private class MetalContainer
    {
        JLabel widthLabel, spacingLabel;
        JTextField spacing, spacingRule;
        JTextField width, widthRule;
    }

    /** Creates new form Metal */
	public Metal(TechEditWizard parent, boolean modal)
	{
		super(parent, modal);
		this.parent = parent;

        setTitle("Metal");
        setName("");

        metal = new JPanel();
        metal.setLayout(new GridBagLayout());

        metalPane = new javax.swing.JScrollPane();

        JLabel heading = new JLabel("Metal Parameters");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.insets = new Insets(4, 4, 4, 4);
        metal.add(heading, gbc);

        JLabel image = new JLabel();
		image.setIcon(Resources.getResource(getClass(), "Metal.png"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.insets = new Insets(4, 4, 4, 4);
        metal.add(image, gbc);

        JButton addMetal = new JButton("Add Metal");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = 2;
        gbc.insets = new Insets(4, 4, 4, 4);
        addMetal.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { addMetal(); }
        });
        metal.add(addMetal, gbc);

        JButton removeMetal = new JButton("Remove Metal");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;   gbc.gridy = 2;
        gbc.insets = new Insets(4, 4, 4, 4);
        removeMetal.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { removeMetal(); }
        });
        metal.add(removeMetal, gbc);

        JLabel l1 = new JLabel("Distance");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;   gbc.gridy = 3;
        metal.add(l1, gbc);

        JLabel l2 = new JLabel("Rule Name");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;   gbc.gridy = 3;
        metal.add(l2, gbc);

        metalPane.setViewportView(metal);

        getContentPane().setLayout(new java.awt.GridBagLayout());
        getContentPane().add(metalPane, new java.awt.GridBagConstraints());

        JLabel nano = new JLabel("Distances are in nanometers");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = 99;
        gbc.gridwidth = 3;
        gbc.insets = new Insets(4, 4, 4, 4);
        metal.add(nano, gbc);
	}

	/** return the panel to use for this Numeric Technology Editor tab. */
	public Component getComponent() { return metalPane; }

	/** return the name of this Numeric Technology Editor tab. */
	public String getName() { return "Metal"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Metal tab.
	 */
	public void init()
	{
		TechEditWizardData data = wizard.getTechEditData();
        numMetals = data.getNumMetalLayers();
        metalContainers = new MetalContainer[numMetals];
        for(int i=0; i<numMetals; i++)
        {
        	metalContainers[i] = addMetalLayer(i, data);
        }
	}

	/**
	 * Method to create the dialog fields for a metal layer.
	 * @param i the metal layer to fill-in.
	 */
	private MetalContainer addMetalLayer(int i, TechEditWizardData data)
	{
        MetalContainer mc = new MetalContainer();
        mc.widthLabel = new JLabel("Metal-" + (i+1) + " width (A):");
    	GridBagConstraints gbc = new GridBagConstraints();
    	gbc.gridx = 0;   gbc.gridy = 4+i*2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 1, 0);
        metal.add(mc.widthLabel, gbc);

        mc.width = new JTextField();
        mc.width.setColumns(8);
    	gbc = new GridBagConstraints();
    	gbc.gridx = 1;   gbc.gridy = 4+i*2;
        gbc.insets = new Insets(4, 0, 1, 2);
        metal.add(mc.width, gbc);

        mc.widthRule = new JTextField();
        mc.widthRule.setColumns(8);
    	gbc = new GridBagConstraints();
    	gbc.gridx = 2;   gbc.gridy = 4+i*2;
        gbc.insets = new Insets(4, 0, 1, 2);
        metal.add(mc.widthRule, gbc);

        mc.spacingLabel = new JLabel("Metal-" + (i+1) + " spacing (B):");
    	gbc = new GridBagConstraints();
    	gbc.gridx = 0;   gbc.gridy = 5+i*2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(1, 4, 4, 0);
        metal.add(mc.spacingLabel, gbc);

        mc.spacing = new JTextField();
        mc.spacing.setColumns(8);
    	gbc = new GridBagConstraints();
    	gbc.gridx = 1;   gbc.gridy = 5+i*2;
        gbc.insets = new Insets(1, 0, 4, 2);
        metal.add(mc.spacing, gbc);

        mc.spacingRule = new JTextField();
        mc.spacingRule.setColumns(8);
    	gbc = new GridBagConstraints();
    	gbc.gridx = 2;   gbc.gridy = 5+i*2;
        gbc.insets = new Insets(1, 0, 4, 2);
        metal.add(mc.spacingRule, gbc);

        if (data != null)
        {
            mc.width.setText(TextUtils.formatDouble(data.getMetalWidth()[i].value));
            mc.widthRule.setText(data.getMetalWidth()[i].rule);
            mc.spacing.setText(TextUtils.formatDouble(data.getMetalSpacing()[i].value));
            mc.spacingRule.setText(data.getMetalSpacing()[i].rule);
        }

        return mc;
    }

	/**
	 * Method called when the user clicks "Add Metal"
	 */
	private void addMetal()
	{
        numMetals++;
        MetalContainer[] newMetalContainers = new MetalContainer[numMetals];
        System.arraycopy(metalContainers, 0, newMetalContainers, 0, numMetals-1);
        metalContainers = newMetalContainers;
	    metalContainers[numMetals-1] = addMetalLayer(numMetals-1, null);
		parent.pack();
	}

	/**
	 * Method called when the user clicks "Remove Metal"
	 */
	private void removeMetal()
	{
		if (numMetals <= 1)
		{
			Job.getUserInterface().showErrorMessage("Cannot delete the last metal layer: must be at least one",
				"Illegal Operation");
			return;
		}
        numMetals--;
        MetalContainer mc = metalContainers[numMetals];
        metalContainers[numMetals] = null;
        metal.remove(mc.widthLabel);
        metal.remove(mc.width);
        metal.remove(mc.widthRule);
        metal.remove(mc.spacingLabel);
        metal.remove(mc.spacing);
        metal.remove(mc.spacingRule);
		parent.pack();
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Metal tab.
	 */
	public void term()
	{
		TechEditWizardData data = wizard.getTechEditData();
		data.setNumMetalLayers(numMetals);
        for(int i=0; i<numMetals; i++)
        {
            MetalContainer mc = metalContainers[i];
        	data.setMetalWidth(i, new WizardField(TextUtils.atof(mc.width.getText()), mc.widthRule.getText()));
        	data.setMetalSpacing(i, new WizardField(TextUtils.atof(mc.spacing.getText()), mc.spacingRule.getText()));
        }
	}
}
