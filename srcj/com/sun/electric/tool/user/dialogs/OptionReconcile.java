/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OptionReconcile.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.text.Pref;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.User;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;


/**
 * Class to handle the "Project Setting Reconcile" dialog.
 */
public class OptionReconcile extends EDialog
{
	private HashMap<JRadioButton,Pref.Meaning> changedOptions;
    private ArrayList<AbstractButton> currentSettings;

	/** Creates new form Project Settings Reconcile */
	public OptionReconcile(Frame parent, boolean modal, List<Pref.Meaning> optionsThatChanged, String libname)
	{
		super(parent, modal);
		initComponents();
        getRootPane().setDefaultButton(ok);

		changedOptions = new HashMap<JRadioButton,Pref.Meaning>();
        currentSettings = new ArrayList<AbstractButton>();
		JPanel optionBox = new JPanel();
		optionBox.setLayout(new GridBagLayout());
		optionPane.setViewportView(optionBox);
		GridBagConstraints gbc = new GridBagConstraints();

		// the second column header: the option description
		gbc.gridx = 1;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.2;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(4, 4, 4, 4);
		optionBox.add(new JLabel("SETTING"), gbc);

		// the third column header: the current value
		gbc.gridx = 2;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.2;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(4, 4, 4, 4);
		optionBox.add(new JLabel("CURRENT VALUE"), gbc);

		// the fourth column header: the Libraries value
		gbc.gridx = 3;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.2;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(4, 4, 4, 4);
		optionBox.add(new JLabel("LIBRARY VALUE"), gbc);

		// the fifth column header: the location of the option
		gbc.gridx = 4;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.2;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(4, 4, 4, 4);
		optionBox.add(new JLabel("SETTING LOCATION"), gbc);

		// the separator between the header and the body
		gbc.gridx = 0;       gbc.gridy = 1;
		gbc.gridwidth = 5;   gbc.gridheight = 1;
		gbc.weightx = 1.0;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		optionBox.add(new JSeparator(), gbc);

		int rowNumber = 2;
		for(Pref.Meaning meaning : optionsThatChanged)
		{
			Pref pref = meaning.getPref();
//			Variable var = meaning.getElectricObject().getVar(pref.getPrefName());
			Object obj = meaning.getDesiredValue();
			if (obj.equals(pref.getValue())) continue;

			String oldValue = null, newValue = null;
			switch (pref.getType())
			{
				case BOOLEAN:
					oldValue = ((Integer)pref.getValue()).intValue() == 0 ? "OFF" : "ON";
					newValue = ((Integer)obj).intValue() == 0 ? "OFF" : "ON";
					break;
				case INTEGER:
					int oldIntValue = ((Integer)pref.getValue()).intValue();
					int newIntValue = ((Integer)obj).intValue();
					String [] trueMeaning = meaning.getTrueMeaning();
					if (trueMeaning != null)
					{
						oldValue = trueMeaning[oldIntValue];
						newValue = trueMeaning[newIntValue];
					} else
					{
						oldValue = Integer.toString(oldIntValue);
						newValue = Integer.toString(newIntValue);
					}
					break;
				case DOUBLE:
					oldValue = Double.toString(((Double)pref.getValue()).doubleValue());
					if (obj instanceof Double)
						newValue = Double.toString(((Double)obj).doubleValue()); else
					if (obj instanceof Float)
						newValue = Float.toString(((Float)obj).floatValue()); else
					{
						System.out.println("HEY! setting "+pref.getPrefName()+" should have Double/Float but instead has value "+obj);
						break;
					}
					break;
				case STRING:
					oldValue = pref.getValue().toString();
					newValue = obj.toString();
					break;
			}

/*
			// the first column: the "Accept" checkbox
			JCheckBox cb = new JCheckBox("Accept");
			cb.setSelected(true);
			gbc.gridx = 0;       gbc.gridy = rowNumber;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0.2;   gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.NONE;
			optionBox.add(cb, gbc);
			changedOptions.put(cb, meaning);
*/

			// the second column is the option description
			gbc.gridx = 1;       gbc.gridy = rowNumber;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0.2;   gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.NONE;
			optionBox.add(new JLabel(meaning.getDescription()), gbc);

			// the third column is the current value
			gbc.gridx = 2;       gbc.gridy = rowNumber;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0.2;   gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.NONE;
            JRadioButton curValue = new JRadioButton(oldValue, false);
            currentSettings.add(curValue);
			optionBox.add(curValue, gbc);
/*
            curValue.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateButtonState();
                }});
*/

			// the fourth column is the Libraries value
			gbc.gridx = 3;       gbc.gridy = rowNumber;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0.2;   gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.NONE;
            JRadioButton libValue = new JRadioButton(newValue, true);
            changedOptions.put(libValue, meaning);
			optionBox.add(libValue, gbc);
/*
            libValue.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateButtonState();
                }});
*/

            ButtonGroup group = new ButtonGroup();
            group.add(curValue);
            group.add(libValue);

			// the fifth column is the location of the option
			gbc.gridx = 4;       gbc.gridy = rowNumber;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0.2;   gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.NONE;
			optionBox.add(new JLabel(meaning.getLocation()), gbc);

			rowNumber++;
		}

        optionHeader.setText("Library \""+libname+"\" wants to use the following project settings which differ from the current project settings");        
        pack();
		finishInitialization();
	}

	public void termDialog()
	{
		new DoReconciliation(changedOptions);
	}

	/**
	 * Class to apply changes to tool options in a new thread.
	 */
	private static class DoReconciliation extends Job
	{
		private DoReconciliation(HashMap<JRadioButton,Pref.Meaning> changedOptions)
		{
			super("Reconcile Project Settings", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);

            List<Pref.Meaning> meaningsToReconcile = new ArrayList<Pref.Meaning>();
            for(JRadioButton cb : changedOptions.keySet())
			{
				if (!cb.isSelected()) continue;
				Pref.Meaning meaning = changedOptions.get(cb);
                meaningsToReconcile.add(meaning);
			}
            Pref.finishPrefReconcilation(meaningsToReconcile);
			startJob();
		}

		public boolean doIt() throws JobException
		{
			return true;
		}
	}
 
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents()
    {
        java.awt.GridBagConstraints gridBagConstraints;

        ok = new javax.swing.JButton();
        optionPane = new javax.swing.JScrollPane();
        optionHeader = new javax.swing.JLabel();
        ignoreLibraryOptions = new javax.swing.JButton();
        useLibraryOptions = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Project Setting Reconciliation");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        ok.setText("Use Above Settings");
        ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ok(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        optionPane.setMinimumSize(new java.awt.Dimension(500, 150));
        optionPane.setPreferredSize(new java.awt.Dimension(650, 150));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(optionPane, gridBagConstraints);

        optionHeader.setText("The new Project Settings are different from the current Project Settings:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(optionHeader, gridBagConstraints);

        ignoreLibraryOptions.setText("Use All Current Settings");
        ignoreLibraryOptions.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ignoreLibraryOptionsActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ignoreLibraryOptions, gridBagConstraints);

        useLibraryOptions.setText("Use All New Settings");
        useLibraryOptions.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                useLibraryOptionsActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(useLibraryOptions, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void useLibraryOptionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_useLibraryOptionsActionPerformed
        // set all library options selected
        for(JRadioButton b : changedOptions.keySet())
        {
            b.setSelected(true);
        }
        ok(null);
    }//GEN-LAST:event_useLibraryOptionsActionPerformed

	private void ignoreLibraryOptionsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ignoreLibraryOptionsActionPerformed
	{//GEN-HEADEREND:event_ignoreLibraryOptionsActionPerformed
		// set all current options selected
        for (AbstractButton b : currentSettings) {
            b.setSelected(true);
        }
        ok(null);
	}//GEN-LAST:event_ignoreLibraryOptionsActionPerformed

	private void ok(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ok
	{//GEN-HEADEREND:event_ok
		termDialog();
		closeDialog(null);
	}//GEN-LAST:event_ok

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    private void updateButtonState() {
        boolean ignoreAllLibOptionsEnabled = false;
        boolean useAllLibOptionsEnabled = false;
        for (AbstractButton b : currentSettings) {
            // if current setting selected, allow user to push "use all lib settings" button
            if (b.isSelected()) useAllLibOptionsEnabled = true;
            // if library setting selected, allow user to push "ignore all lib settings" button
            if (!b.isSelected()) ignoreAllLibOptionsEnabled = true;
        }
        useLibraryOptions.setEnabled(useAllLibOptionsEnabled);
        ignoreLibraryOptions.setEnabled(ignoreAllLibOptionsEnabled);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton ignoreLibraryOptions;
    private javax.swing.JButton ok;
    private javax.swing.JLabel optionHeader;
    private javax.swing.JScrollPane optionPane;
    private javax.swing.JButton useLibraryOptions;
    // End of variables declaration//GEN-END:variables
}
