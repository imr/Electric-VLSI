/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WellCheckTab.java
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

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.erc.ERC;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.io.output.Verilog;
import com.sun.electric.tool.logicaleffort.LETool;
import com.sun.electric.tool.routing.Routing;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.util.Iterator;
import java.util.HashMap;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;


/**
 * Class to handle the "Well Check" tab of the Preferences dialog.
 */
public class WellCheckTab extends PreferencePanel
{
	/** Creates new form WellCheckTab */
	public WellCheckTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}
	public JPanel getPanel() { return wellCheck; }

	public String getName() { return "Well Check"; }

	private int initialWellCheckPWellRule;
	private boolean initialWellCheckPWellConnectToGround;
	private int initialWellCheckNWellRule;
	private boolean initialWellCheckNWellConnectToPower;
	private boolean initialWellCheckFindFarthest;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Well Check tab.
	 */
	public void init()
	{
		initialWellCheckPWellRule = ERC.getPWellCheck();
		switch (initialWellCheckPWellRule)
		{
			case 0: wellPMustHaveAllContacts.setSelected(true);   break;
			case 1: wellPMustHave1Contact.setSelected(true);      break;
			case 2: wellPNoContactCheck.setSelected(true);        break;
		}

		initialWellCheckPWellConnectToGround = ERC.isMustConnectPWellToGround();
		wellPMustConnectGround.setSelected(initialWellCheckPWellConnectToGround);

		initialWellCheckNWellRule = ERC.getNWellCheck();
		switch (initialWellCheckNWellRule)
		{
			case 0: wellNMustHaveAllContacts.setSelected(true);   break;
			case 1: wellNMustHave1Contact.setSelected(true);      break;
			case 2: wellNNoContactCheck.setSelected(true);        break;
		}

		initialWellCheckNWellConnectToPower = ERC.isMustConnectNWellToPower();
		wellNMustConnectPower.setSelected(initialWellCheckNWellConnectToPower);

		initialWellCheckFindFarthest = ERC.isFindWorstCaseWell();
		wellFindFarthestDistance.setSelected(initialWellCheckFindFarthest);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Well Check tab.
	 */
	public void term()
	{
		int currentPWellRule = 0;
		if (wellPMustHave1Contact.isSelected()) currentPWellRule = 1; else
			if (wellPNoContactCheck.isSelected()) currentPWellRule = 2;
		if (currentPWellRule != initialWellCheckPWellRule)
			ERC.setPWellCheck(currentPWellRule);

		boolean currentPWellGroundCheck = wellPMustConnectGround.isSelected();
		if (currentPWellGroundCheck != initialWellCheckPWellConnectToGround)
			ERC.setMustConnectPWellToGround(currentPWellGroundCheck);

		int currentNWellRule = 0;
		if (wellNMustHave1Contact.isSelected()) currentNWellRule = 1; else
			if (wellNNoContactCheck.isSelected()) currentNWellRule = 2;
		if (currentNWellRule != initialWellCheckNWellRule)
			ERC.setNWellCheck(currentNWellRule);

		boolean currentNWellPowerCheck = wellNMustConnectPower.isSelected();
		if (currentNWellPowerCheck != initialWellCheckNWellConnectToPower)
			ERC.setMustConnectNWellToPower(currentNWellPowerCheck);

		boolean currentFarCheck = wellFindFarthestDistance.isSelected();
		if (currentFarCheck != initialWellCheckFindFarthest)
			ERC.setFindWorstCaseWell(currentFarCheck);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        wellCheckPWell = new javax.swing.ButtonGroup();
        wellCheckNWell = new javax.swing.ButtonGroup();
        wellCheck = new javax.swing.JPanel();
        jLabel64 = new javax.swing.JLabel();
        jLabel65 = new javax.swing.JLabel();
        wellPMustHaveAllContacts = new javax.swing.JRadioButton();
        wellPMustHave1Contact = new javax.swing.JRadioButton();
        wellPNoContactCheck = new javax.swing.JRadioButton();
        wellNMustHaveAllContacts = new javax.swing.JRadioButton();
        wellNMustHave1Contact = new javax.swing.JRadioButton();
        wellNNoContactCheck = new javax.swing.JRadioButton();
        wellPMustConnectGround = new javax.swing.JCheckBox();
        wellNMustConnectPower = new javax.swing.JCheckBox();
        wellFindFarthestDistance = new javax.swing.JCheckBox();

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

        wellCheck.setLayout(new java.awt.GridBagLayout());

        jLabel64.setText("For P-Well:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        wellCheck.add(jLabel64, gridBagConstraints);

        jLabel65.setText("For N-Well:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        wellCheck.add(jLabel65, gridBagConstraints);

        wellPMustHaveAllContacts.setText("Must have contact in every area");
        wellCheckPWell.add(wellPMustHaveAllContacts);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellPMustHaveAllContacts, gridBagConstraints);

        wellPMustHave1Contact.setText("Must have at least 1 contact");
        wellCheckPWell.add(wellPMustHave1Contact);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellPMustHave1Contact, gridBagConstraints);

        wellPNoContactCheck.setText("Do not check for contacts");
        wellCheckPWell.add(wellPNoContactCheck);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellPNoContactCheck, gridBagConstraints);

        wellNMustHaveAllContacts.setText("Must have contact in every area");
        wellCheckNWell.add(wellNMustHaveAllContacts);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellNMustHaveAllContacts, gridBagConstraints);

        wellNMustHave1Contact.setText("Must have at least 1 contact");
        wellCheckNWell.add(wellNMustHave1Contact);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellNMustHave1Contact, gridBagConstraints);

        wellNNoContactCheck.setText("Do not check for contacts");
        wellCheckNWell.add(wellNNoContactCheck);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellNNoContactCheck, gridBagConstraints);

        wellPMustConnectGround.setText("Must connect to Ground");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellPMustConnectGround, gridBagConstraints);

        wellNMustConnectPower.setText("Must connect to Power");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellNMustConnectPower, gridBagConstraints);

        wellFindFarthestDistance.setText("Find farthest distance from contact to edge");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellFindFarthestDistance, gridBagConstraints);

        getContentPane().add(wellCheck, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel64;
    private javax.swing.JLabel jLabel65;
    private javax.swing.JPanel wellCheck;
    private javax.swing.ButtonGroup wellCheckNWell;
    private javax.swing.ButtonGroup wellCheckPWell;
    private javax.swing.JCheckBox wellFindFarthestDistance;
    private javax.swing.JCheckBox wellNMustConnectPower;
    private javax.swing.JRadioButton wellNMustHave1Contact;
    private javax.swing.JRadioButton wellNMustHaveAllContacts;
    private javax.swing.JRadioButton wellNNoContactCheck;
    private javax.swing.JCheckBox wellPMustConnectGround;
    private javax.swing.JRadioButton wellPMustHave1Contact;
    private javax.swing.JRadioButton wellPMustHaveAllContacts;
    private javax.swing.JRadioButton wellPNoContactCheck;
    // End of variables declaration//GEN-END:variables
	
}
