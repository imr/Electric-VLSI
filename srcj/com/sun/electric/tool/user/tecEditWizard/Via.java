/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Via.java
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
import com.sun.electric.tool.user.Resources;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Class to handle the "Via" tab of the Numeric Technology Editor dialog.
 */
public class Via extends TechEditWizardPanel
{
    private JPanel via;
    private JLabel [] sizeLabel, spacingLabel, arraySpacingLabel, overhangInlineLabel;
    private JTextField [] size, spacing, arraySpacing, overhangInline;
    private int numVias;

    /** Creates new form Via */
	public Via(TechEditWizard parent, boolean modal)
	{
		super(parent, modal);

        setTitle("Via");
        setName("");

        via = new JPanel();
        via.setLayout(new GridBagLayout());

        JLabel heading = new JLabel("Via Parameters");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(4, 4, 4, 4);
        via.add(heading, gbc);

        JLabel image = new JLabel();
		image.setIcon(Resources.getResource(getClass(), "Via.png"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(4, 4, 4, 4);
        via.add(image, gbc);

        JLabel nano = new JLabel("All values are in nanometers");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = 99;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(4, 4, 4, 4);
        via.add(nano, gbc);
        
		TechEditWizardData data = getNumericData();
		numVias = data.getNumMetalLayers() - 1;
		if (numVias > 0)
		{
	        sizeLabel = new JLabel[numVias];
	        size = new JTextField[numVias];
	        spacingLabel = new JLabel[numVias];
	        spacing = new JTextField[numVias];
	        arraySpacingLabel = new JLabel[numVias];
	        arraySpacing = new JTextField[numVias];
	        overhangInlineLabel = new JLabel[numVias];
	        overhangInline = new JTextField[numVias];
		}
        for(int i=0; i<numVias; i++)
        	addMetalLayer(i);
	}

	private void addMetalLayer(int i)
	{
		sizeLabel[i] = new JLabel("Via-" + (i+1) + " size (A):");
    	GridBagConstraints gbc = new GridBagConstraints();
    	gbc.gridx = 0;   gbc.gridy = 2+i*4;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 1, 0);
        via.add(sizeLabel[i], gbc);

        size[i] = new JTextField();
        size[i].setColumns(8);
    	gbc = new GridBagConstraints();
    	gbc.gridx = 1;   gbc.gridy = 2+i*4;
        gbc.insets = new Insets(4, 0, 1, 2);
        via.add(size[i], gbc);

        spacingLabel[i] = new JLabel("Via-" + (i+1) + " spacing (B):");
    	gbc = new GridBagConstraints();
    	gbc.gridx = 0;   gbc.gridy = 3+i*4;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(1, 4, 1, 0);
        via.add(spacingLabel[i], gbc);

        spacing[i] = new JTextField();
        spacing[i].setColumns(8);
    	gbc = new GridBagConstraints();
    	gbc.gridx = 1;   gbc.gridy = 3+i*4;
        gbc.insets = new Insets(1, 0, 1, 2);
        via.add(spacing[i], gbc);

        arraySpacingLabel[i] = new JLabel("Via-" + (i+1) + " array spacing (C):");
    	gbc = new GridBagConstraints();
    	gbc.gridx = 0;   gbc.gridy = 4+i*4;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(1, 4, 1, 0);
        via.add(arraySpacingLabel[i], gbc);

        arraySpacing[i] = new JTextField();
        arraySpacing[i].setColumns(8);
    	gbc = new GridBagConstraints();
    	gbc.gridx = 1;   gbc.gridy = 4+i*4;
        gbc.insets = new Insets(1, 0, 1, 2);
        via.add(arraySpacing[i], gbc);

        overhangInlineLabel[i] = new JLabel("Via-" + (i+1) + " inline overhang (D):");
    	gbc = new GridBagConstraints();
    	gbc.gridx = 0;   gbc.gridy = 5+i*4;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(1, 4, 4, 0);
        via.add(overhangInlineLabel[i], gbc);

        overhangInline[i] = new JTextField();
        overhangInline[i].setColumns(8);
    	gbc = new GridBagConstraints();
    	gbc.gridx = 1;   gbc.gridy = 5+i*4;
        gbc.insets = new Insets(1, 0, 4, 2);
        via.add(overhangInline[i], gbc);
	}

	/** return the panel to use for this Numeric Technology Editor tab. */
	public JPanel getPanel() { return via; }

	/** return the name of this Numeric Technology Editor tab. */
	public String getName() { return "Via"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Via tab.
	 */
	public void init()
	{
		TechEditWizardData data = getNumericData();
        for(int i=0; i<numVias; i++)
        {
        	size[i].setText(Double.toString(data.getViaSize()[i]));
        	spacing[i].setText(Double.toString(data.getViaSpacing()[i]));
        	arraySpacing[i].setText(Double.toString(data.getViaArraySpacing()[i]));
        	overhangInline[i].setText(Double.toString(data.getViaOverhangInline()[i]));
        }
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Via tab.
	 */
	public void term()
	{
		TechEditWizardData data = getNumericData();
		if (numVias > 0)
		{
	        double [] newSize = new double[numVias];
	        double [] newSpacing = new double[numVias];
	        double [] newArraySpacing = new double[numVias];
	        double [] newOverhangInline = new double[numVias];
	        for(int i=0; i<numVias; i++)
	        {
	        	newSize[i] = TextUtils.atof(size[i].getText());
	        	newSpacing[i] = TextUtils.atof(spacing[i].getText());
	        	newArraySpacing[i] = TextUtils.atof(arraySpacing[i].getText());
	        	newOverhangInline[i] = TextUtils.atof(overhangInline[i].getText());
	        }
			data.setViaSize(newSize);
			data.setViaSpacing(newSpacing);
			data.setViaArraySpacing(newArraySpacing);
			data.setViaOverhangInline(newOverhangInline);
		}
	}
}
