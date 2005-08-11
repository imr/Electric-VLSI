/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DesignRulesTab.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.DRCRules;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.utils.MOSRules;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.dialogs.DesignRulesPanel;
import com.sun.electric.tool.user.CircuitChanges;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.JOptionPane;
import javax.swing.JPanel;


/**
 * Class to handle the "Design Rules" tab of the Preferences dialog.
 */
public class DesignRulesTab extends PreferencePanel
{
	DesignRulesPanel rulesPanel;
	private DRCRules drRules;
	private boolean designRulesFactoryReset = false;
	private int foundry;

	/** Creates new form DesignRulesTab */
	public DesignRulesTab(Frame parent, boolean modal)
	{
		super(parent, modal);

		initComponents();

        // Adding the node and layer panels
        rulesPanel = new DesignRulesPanel();
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.insets = new Insets(0, 0, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        designRules.add(rulesPanel, gridBagConstraints);

		factoryReset.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { factoryResetDRCActionPerformed(evt); }
		});
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return designRules; }

	/** return the name of this preferences tab. */
	public String getName() { return "Design Rules"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Design Rules tab.
	 */
	public void init()
	{
		// get the design rules for the current technology
		DRCRules rules = DRC.getRules(curTech);
		if (rules == null)
		{
			drTechName.setText("Technology " + curTech.getTechName() + " HAS NO DESIGN RULES");
			factoryReset.setEnabled(false);
			return;
		}

        drRules = rules;
		foundry = curTech.getFoundry();
        rulesPanel.init(curTech, foundry, drRules);

		// load the dialog
        String text = "Design Rules for Technology '" + curTech.getTechName() + "'";
		drTechName.setText(text);

        // Foundry
        String selectedFoundry = curTech.getSelectedFoundry();
        for (Iterator it = curTech.getFactories(); it.hasNext(); )
        {
            Technology.Foundry factory = (Technology.Foundry)it.next();
            defaultFoundryPulldown.addItem(factory.name);
            if (selectedFoundry.equals(factory.name)) foundry = factory.techMode;
        }
        defaultFoundryPulldown.setEnabled(foundry != -1);
        defaultFoundryPulldown.setSelectedItem(selectedFoundry);

        // Resolution
		drResolutionValue.setText(TextUtils.formatDouble(curTech.getResolution()));

//        pack();
	}

	private void factoryResetDRCActionPerformed(ActionEvent evt)
	{
		int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(),
			"Are you sure you want to do a factory reset of these design rules?");
		if (response != JOptionPane.YES_OPTION) return;
		designRulesFactoryReset = true;
//		okActionPerformed(null);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Design Rules tab.
	 */
	public void term()
	{
        String foundryName = (String)defaultFoundryPulldown.getSelectedItem();
        int val = -1;

        if (!foundryName.equals(curTech.getSelectedFoundry()))
        {
            String [] messages = {
				"Primitives in database might be resized according to values provided by " + foundryName + ".",
                "If you do not resize now, arc widths might not be optimal for " + foundryName + ".",
                "If you cancel the operation, the foundry will not be changed.",
				"Do you want to resize the database?"};
            Object [] options = {"Yes", "No", "Cancel"};
            val = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(), messages,
				"Resize Primitive Nodes and Arcs", JOptionPane.DEFAULT_OPTION,
				JOptionPane.WARNING_MESSAGE, null, options, options[0]);
            if (val != 2)
                curTech.setSelectedFoundry(foundryName);
        }

		double currentResolution = TextUtils.atof(drResolutionValue.getText());
		if (currentResolution != curTech.getResolution())
			curTech.setResolution(currentResolution);

        // Getting last changes
		if (designRulesFactoryReset)
		{
			DRC.resetDRCDates();
            drRules = curTech.getFactoryDesignRules();
//            if (rules instanceof MOSRules)
//			    drRules = (MOSRules)rules;
		}
		DRC.setRules(curTech, drRules);

        // Repaint primitives
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd != null) wnd.repaintContents(null);

        if (val == 0)
        {
            // primitive arcs have to be modified.
            CircuitChanges.ResetDefaultWidthCommand();
        }
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        designRules = new javax.swing.JPanel();
        defaultFoundryLabel = new javax.swing.JLabel();
        defaultFoundryPulldown = new javax.swing.JComboBox();
        drResolutionLabel = new javax.swing.JLabel();
        drResolutionValue = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        factoryReset = new javax.swing.JButton();
        drTechName = new javax.swing.JLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Tool Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        designRules.setLayout(new java.awt.GridBagLayout());

        designRules.setBorder(new javax.swing.border.TitledBorder(""));
        defaultFoundryLabel.setText("Foundry:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        designRules.add(defaultFoundryLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        designRules.add(defaultFoundryPulldown, gridBagConstraints);

        drResolutionLabel.setText("Min. resolution:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        designRules.add(drResolutionLabel, gridBagConstraints);

        drResolutionValue.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        designRules.add(drResolutionValue, gridBagConstraints);

        jLabel6.setText("(use 0 to ignore resolution check)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        designRules.add(jLabel6, gridBagConstraints);

        factoryReset.setText("Factory Reset");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        designRules.add(factoryReset, gridBagConstraints);

        drTechName.setText("jLabel1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        designRules.add(drTechName, gridBagConstraints);

        getContentPane().add(designRules, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel defaultFoundryLabel;
    private javax.swing.JComboBox defaultFoundryPulldown;
    private javax.swing.JPanel designRules;
    private javax.swing.JLabel drResolutionLabel;
    private javax.swing.JTextField drResolutionValue;
    private javax.swing.JLabel drTechName;
    private javax.swing.JButton factoryReset;
    private javax.swing.JLabel jLabel6;
    // End of variables declaration//GEN-END:variables

}
