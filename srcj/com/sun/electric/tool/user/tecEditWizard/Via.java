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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Class to handle the "Via" tab of the Numeric Technology Editor dialog.
 */
public class Via extends TechEditWizardPanel
{
    private JPanel via;
    private JComboBox whichVia;
    private JTextField size, spacing, arraySpacing, overhangInline;
    private double [] vSize, vSpacing, vArraySpacing, vOverhangInline;
    private boolean dataChanging;

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

        JLabel whichViaLabel = new JLabel("Which via:");
    	gbc = new GridBagConstraints();
    	gbc.gridx = 0;   gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 1, 0);
        via.add(whichViaLabel, gbc);

        whichVia = new JComboBox();
		whichVia.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { viaChanged(); }
		});
        gbc = new GridBagConstraints();
        gbc.gridx = 1;   gbc.gridy = 2;
        gbc.insets = new Insets(4, 0, 4, 2);
        via.add(whichVia, gbc);

        JLabel sizeLabel = new JLabel("Via size (A):");
    	gbc = new GridBagConstraints();
    	gbc.gridx = 0;   gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 1, 0);
        via.add(sizeLabel, gbc);

        size = new JTextField();
        size.getDocument().addDocumentListener(new ViaDocumentListener());
        size.setColumns(8);
    	gbc = new GridBagConstraints();
    	gbc.gridx = 1;   gbc.gridy = 3;
        gbc.insets = new Insets(4, 0, 1, 2);
        via.add(size, gbc);

        JLabel spacingLabel = new JLabel("Via spacing (B):");
    	gbc = new GridBagConstraints();
    	gbc.gridx = 0;   gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(1, 4, 1, 0);
        via.add(spacingLabel, gbc);

        spacing = new JTextField();
        spacing.getDocument().addDocumentListener(new ViaDocumentListener());
        spacing.setColumns(8);
    	gbc = new GridBagConstraints();
    	gbc.gridx = 1;   gbc.gridy = 4;
        gbc.insets = new Insets(1, 0, 1, 2);
        via.add(spacing, gbc);

        JLabel arraySpacingLabel = new JLabel("Via array spacing (C):");
    	gbc = new GridBagConstraints();
    	gbc.gridx = 0;   gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(1, 4, 1, 0);
        via.add(arraySpacingLabel, gbc);

        arraySpacing = new JTextField();
        arraySpacing.getDocument().addDocumentListener(new ViaDocumentListener());
        arraySpacing.setColumns(8);
    	gbc = new GridBagConstraints();
    	gbc.gridx = 1;   gbc.gridy = 5;
        gbc.insets = new Insets(1, 0, 1, 2);
        via.add(arraySpacing, gbc);

        JLabel overhangInlineLabel = new JLabel("Via inline overhang (D):");
    	gbc = new GridBagConstraints();
    	gbc.gridx = 0;   gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(1, 4, 4, 0);
        via.add(overhangInlineLabel, gbc);

        overhangInline = new JTextField();
        overhangInline.getDocument().addDocumentListener(new ViaDocumentListener());
        overhangInline.setColumns(8);
    	gbc = new GridBagConstraints();
    	gbc.gridx = 1;   gbc.gridy = 6;
        gbc.insets = new Insets(1, 0, 4, 2);
        via.add(overhangInline, gbc);

        JLabel nano = new JLabel("All values are in nanometers");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;   gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(4, 4, 4, 4);
        via.add(nano, gbc);
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
		// add appropriate number of via layers
		TechEditWizardData data = wizard.getTechEditData();
		int numVias = data.getNumMetalLayers() - 1;
		whichVia.removeAllItems();
        for(int i=0; i<numVias; i++)
	        whichVia.addItem((i+1) + " to " + (i+2));
		if (numVias > 0)
		{
		    vSize = new double[numVias];
		    vSpacing = new double[numVias];
		    vArraySpacing = new double[numVias];
		    vOverhangInline = new double[numVias];
	        for(int i=0; i<numVias; i++)
	        {
	        	vSize[i] = data.getViaSize()[i];
	        	vSpacing[i] = data.getViaSpacing()[i];
	        	vArraySpacing[i] = data.getViaArraySpacing()[i];
	        	vOverhangInline[i] = data.getViaOverhangInline()[i];
	        }
			whichVia.setSelectedIndex(0);
		}
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Via tab.
	 */
	public void term()
	{
		TechEditWizardData data = wizard.getTechEditData();
		int numVias = data.getNumMetalLayers() - 1;
        for(int i=0; i<numVias; i++)
        {
        	data.setViaSize(i, vSize[i]);
        	data.setViaSpacing(i, vSpacing[i]);
        	data.setViaArraySpacing(i, vArraySpacing[i]);
        	data.setViaOverhangInline(i, vOverhangInline[i]);
		}
	}

	private void viaChanged()
	{
		dataChanging = true;
		int which = whichVia.getSelectedIndex();
		if (which < 0 || vSize == null || which >= vSize.length)
		{
		    size.setText("");
		    spacing.setText("");
		    arraySpacing.setText("");
		    overhangInline.setText("");
		} else
		{
		    size.setText(TextUtils.formatDouble(vSize[which]));
		    spacing.setText(TextUtils.formatDouble(vSpacing[which]));
		    arraySpacing.setText(TextUtils.formatDouble(vArraySpacing[which]));
		    overhangInline.setText(TextUtils.formatDouble(vOverhangInline[which]));
		}
		dataChanging = false;
	}

	private void viaDataChanged()
	{
		if (dataChanging) return;
		int which = whichVia.getSelectedIndex();
		if (which < 0 || vSize == null || which >= vSize.length) return;
		vSize[which] = TextUtils.atof(size.getText());
		vSpacing[which] = TextUtils.atof(spacing.getText());
		vArraySpacing[which] = TextUtils.atof(arraySpacing.getText());
		vOverhangInline[which] = TextUtils.atof(overhangInline.getText());
	}

	/**
	 * Class to handle special changes to changes to a via size data.
	 */
	private class ViaDocumentListener implements DocumentListener
	{
		public void changedUpdate(DocumentEvent e) { viaDataChanged(); }
		public void insertUpdate(DocumentEvent e) { viaDataChanged(); }
		public void removeUpdate(DocumentEvent e) { viaDataChanged(); }
	}
}
