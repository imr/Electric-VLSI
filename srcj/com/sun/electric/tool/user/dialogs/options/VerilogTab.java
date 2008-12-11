/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VerilogTab.java
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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.tool.simulation.Simulation;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 * Class to handle the "Verilog" tab of the Preferences dialog.
 */
public class VerilogTab extends PreferencePanel
{
    private JPanel verilog;
    private JCheckBox stopAtStandardCells;
    private JCheckBox preserveVerilogFormatting;
    private JCheckBox parameterizeModuleNames;

    /** Creates new form VerilogTab */
	public VerilogTab(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return verilog; }

	/** return the name of this preferences tab. */
	public String getName() { return "Verilog"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Verilog tab.
	 */
	public void init()
	{
		// Verilog
		stopAtStandardCells.setSelected(Simulation.getVerilogStopAtStandardCells());
		preserveVerilogFormatting.setSelected(Simulation.getPreserveVerilogFormating());
		parameterizeModuleNames.setSelected(Simulation.getVerilogParameterizeModuleNames());
	}
    
	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Verilog tab.
	 */
	public void term()
	{
		boolean nowBoolean = stopAtStandardCells.isSelected();
		// Update the global preference
		Simulation.setVerilogStopAtStandardCells(nowBoolean);

		nowBoolean = preserveVerilogFormatting.isSelected();
		// Update the global preference
		Simulation.setPreserveVerilogFormating(nowBoolean);
		
		nowBoolean = parameterizeModuleNames.isSelected();
		// Update the global preference
		Simulation.setVerilogParameterizeModuleNames(nowBoolean);
	}

	/**
	 * Method called when the factory reset is requested.
	 */
	public void reset()
	{
		if (Simulation.getFactoryVerilogStopAtStandardCells() != Simulation.getVerilogStopAtStandardCells())
			Simulation.setVerilogStopAtStandardCells(Simulation.getFactoryVerilogStopAtStandardCells());
		if (Simulation.getFactoryPreserveVerilogFormating() != Simulation.getPreserveVerilogFormating())
			Simulation.setPreserveVerilogFormating(Simulation.getFactoryPreserveVerilogFormating());
		if (Simulation.getFactoryVerilogParameterizeModuleNames() != Simulation.getVerilogParameterizeModuleNames())
			Simulation.setVerilogParameterizeModuleNames(Simulation.getFactoryVerilogParameterizeModuleNames());
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 */
    private void initComponents()
    {
        GridBagConstraints gridBagConstraints;

        verilog = new JPanel();
        
        getContentPane().setLayout(new GridBagLayout());

        setTitle("Tool Options");
        setName("");
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent evt)
            {
        		setVisible(false);
        		dispose();
            }
        });

        verilog.setLayout(new GridBagLayout());

        // Checkbox to ignore Standard Cells
        stopAtStandardCells = new JCheckBox();
        stopAtStandardCells.setText("Do not netlist Standard Cells");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0; gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(4, 4, 4, 4);
        verilog.add(stopAtStandardCells, gridBagConstraints);

        // Don't trim off whitespace from Verilog text boxes 
        preserveVerilogFormatting = new JCheckBox();
        preserveVerilogFormatting.setText("Preserve Verilog formatting");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0; gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(4, 4, 4, 4);
        verilog.add(preserveVerilogFormatting, gridBagConstraints);

        parameterizeModuleNames = new JCheckBox();
        parameterizeModuleNames.setText("Parameterize Verilog module names");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0; gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(4, 4, 4, 4);
        verilog.add(parameterizeModuleNames, gridBagConstraints);

        getContentPane().add(verilog, new GridBagConstraints());

        pack();
    }
}
