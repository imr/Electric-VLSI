/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SUETab.java
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

import com.sun.electric.tool.io.IOTool;

import javax.swing.JPanel;


/**
 * Class to handle the "SUE" tab of the Preferences dialog.
 */
public class SUETab extends PreferencePanel
{
	private boolean initialUse4PortTransistors;

	/** Creates new form SUETab */
	public SUETab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}
	public JPanel getPanel() { return sue; }

	public String getName() { return "SUE"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the SUE tab.
	 */
	public void init()
	{
		initialUse4PortTransistors = IOTool.isSueUses4PortTransistors();
		sueMake4PortTransistors.setSelected(initialUse4PortTransistors);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the SUE tab.
	 */
	public void term()
	{
		boolean currentUse4PortTransistors = sueMake4PortTransistors.isSelected();
		if (currentUse4PortTransistors != initialUse4PortTransistors)
			IOTool.setSueUses4PortTransistors(currentUse4PortTransistors);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        sue = new javax.swing.JPanel();
        sueMake4PortTransistors = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("IO Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        sue.setLayout(new java.awt.GridBagLayout());

        sueMake4PortTransistors.setText("Make 4-port transistors");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        sue.add(sueMake4PortTransistors, gridBagConstraints);

        getContentPane().add(sue, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel sue;
    private javax.swing.JCheckBox sueMake4PortTransistors;
    // End of variables declaration//GEN-END:variables
	
}
