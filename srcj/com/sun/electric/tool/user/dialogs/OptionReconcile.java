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

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import javax.swing.Box;


/**
 * Class to handle the "Option Reconcile" dialog.
 */
public class OptionReconcile extends javax.swing.JDialog
{
	private HashMap changedOptions;
	
	/** Creates new form Option Reconcile */
	public OptionReconcile(java.awt.Frame parent, boolean modal, List optionsThatChanged)
	{
		super(parent, modal);
		setLocation(100, 50);
		initComponents();

		changedOptions = new HashMap();
		Box optionBox = Box.createVerticalBox();
		optionPane.setViewportView(optionBox);
		for(Iterator it = optionsThatChanged.iterator(); it.hasNext(); )
		{
			Pref.Meaning meaning = (Pref.Meaning)it.next();
			Pref pref = meaning.getPref();
			Variable var = meaning.getElectricObject().getVar(pref.getPrefName());
			Object obj = null;
			if (var == null)
			{
				// not in a variable, so it must be at factory default
				obj = pref.getFactoryValue();
			} else
			{
				// in a variable: make sure that value is set
				obj = var.getObject();
			}
			if (obj.equals(pref.getValue())) continue;

			String oldValue = null, newValue = null;
			switch (pref.getType())
			{
				case Pref.BOOLEAN:
					oldValue = ((Integer)pref.getValue()).intValue() == 0 ? "FALSE" : "TRUE";
					newValue = ((Integer)obj).intValue() == 0 ? "FALSE" : "TRUE";
					break;
				case Pref.INTEGER:
					oldValue = Integer.toString(((Integer)pref.getValue()).intValue());
					newValue = Integer.toString(((Integer)obj).intValue());
					break;
				case Pref.DOUBLE:
					oldValue = Double.toString(((Double)pref.getValue()).doubleValue());
					newValue = Double.toString(((Double)obj).doubleValue());
					break;
				case Pref.STRING:
					oldValue = pref.getValue().toString();
					newValue = obj.toString();
					break;
			}
			String line = meaning.getDescription() + " is " + oldValue + " but library wants " + newValue + " (" + meaning.getLocation() + ")";
			JCheckBox cb = new JCheckBox(line);
			cb.setSelected(true);
			optionBox.add(cb);
			changedOptions.put(cb, meaning);
		}
	}

	private void termDialog()
	{
		for(Iterator it = changedOptions.keySet().iterator(); it.hasNext(); )
		{
			JCheckBox cb = (JCheckBox)it.next();
			if (!cb.isSelected()) continue;
			Pref.Meaning meaning = (Pref.Meaning)changedOptions.get(cb);
			Pref pref = meaning.getPref();

			Variable var = meaning.getElectricObject().getVar(pref.getPrefName());
			Object obj = null;
			if (var == null)
			{
				// not in a variable, so it must be at factory default
				obj = pref.getFactoryValue();
			} else
			{
				// in a variable: make sure that value is set
				obj = var.getObject();
			}

			// set the option
			switch (pref.getType())
			{
				case Pref.BOOLEAN: pref.setBoolean(((Integer)obj).intValue() != 0);   break;
				case Pref.INTEGER: pref.setInt(((Integer)obj).intValue());            break;
				case Pref.DOUBLE:  pref.setDouble(((Double)obj).doubleValue());       break;
				case Pref.STRING:  pref.setString((String)obj);                       break;
			}
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

        ok = new javax.swing.JButton();
        optionPane = new javax.swing.JScrollPane();
        optionHeader = new javax.swing.JLabel();
        ignoreLibraryOptions = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Option Reconciliation");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ok(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        optionPane.setMinimumSize(new java.awt.Dimension(350, 150));
        optionPane.setPreferredSize(new java.awt.Dimension(350, 150));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(optionPane, gridBagConstraints);

        optionHeader.setText("Options used with this library are different than current options:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(optionHeader, gridBagConstraints);

        ignoreLibraryOptions.setText("Ignore Library Options");
        ignoreLibraryOptions.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ignoreLibraryOptionsActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ignoreLibraryOptions, gridBagConstraints);

        jLabel1.setText("Checked options will be taken from the library that was read.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jLabel1, gridBagConstraints);

        jLabel2.setText("Unchecked options will use the existing values.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jLabel2, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void ignoreLibraryOptionsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ignoreLibraryOptionsActionPerformed
	{//GEN-HEADEREND:event_ignoreLibraryOptionsActionPerformed
		closeDialog(null);
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton ignoreLibraryOptions;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JButton ok;
    private javax.swing.JLabel optionHeader;
    private javax.swing.JScrollPane optionPane;
    // End of variables declaration//GEN-END:variables
	
}
