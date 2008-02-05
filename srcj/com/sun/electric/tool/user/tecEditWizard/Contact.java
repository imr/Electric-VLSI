/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Contact.java
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

import javax.swing.JPanel;

/**
 * Class to handle the "Contact" tab of the Numeric Technology Editor dialog.
 */
public class Contact extends TechEditWizardPanel
{
	/** Creates new form Contact */
	public Contact(TechEditWizard parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
		image.setIcon(Resources.getResource(getClass(), "Contact.png"));
		pack();
	}

	/** return the panel to use for this Numeric Technology Editor tab. */
	public JPanel getPanel() { return contact; }

	/** return the name of this Numeric Technology Editor tab. */
	public String getName() { return "Contact"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Contact tab.
	 */
	public void init()
	{
		TechEditWizardData data = wizard.getTechEditData();
		size.setText(Double.toString(data.getContactSize()));
		spacing.setText(Double.toString(data.getContactSpacing()));
		metalOverhangInline.setText(Double.toString(data.getContactMetalOverhangInlineOnly()));
		metalOverhangAll.setText(Double.toString(data.getContactMetalOverhangAllSides()));
		polyOverhang.setText(Double.toString(data.getContactPolyOverhang()));
		activeSpacing.setText(Double.toString(data.getPolyconDiffSpacing()));
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Contact tab.
	 */
	public void term()
	{
		TechEditWizardData data = wizard.getTechEditData();
		data.setContactSize(TextUtils.atof(size.getText()));
		data.setContactSpacing(TextUtils.atof(spacing.getText()));
		data.setContactMetalOverhangInlineOnly(TextUtils.atof(metalOverhangInline.getText()));
		data.setContactMetalOverhangAllSides(TextUtils.atof(metalOverhangAll.getText()));
		data.setContactPolyOverhang(TextUtils.atof(polyOverhang.getText()));
		data.setPolyconDiffSpacing(TextUtils.atof(activeSpacing.getText()));
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        contact = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        size = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        spacing = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        metalOverhangInline = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        metalOverhangAll = new javax.swing.JTextField();
        image = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        polyOverhang = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        activeSpacing = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Contact");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        contact.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("Size (A):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 0);
        contact.add(jLabel1, gridBagConstraints);

        size.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        contact.add(size, gridBagConstraints);

        jLabel2.setText("Spacing (B):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 0);
        contact.add(jLabel2, gridBagConstraints);

        spacing.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        contact.add(spacing, gridBagConstraints);

        jLabel3.setText("Metal overhang, inline (C):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 0);
        contact.add(jLabel3, gridBagConstraints);

        metalOverhangInline.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        contact.add(metalOverhangInline, gridBagConstraints);

        jLabel4.setText("Metal overhang, all (D):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 0);
        contact.add(jLabel4, gridBagConstraints);

        metalOverhangAll.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        contact.add(metalOverhangAll, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        contact.add(image, gridBagConstraints);

        jLabel5.setText("Poly overhang (E):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 0);
        contact.add(jLabel5, gridBagConstraints);

        polyOverhang.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        contact.add(polyOverhang, gridBagConstraints);

        jLabel6.setText("Active spacing (F):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 0);
        contact.add(jLabel6, gridBagConstraints);

        activeSpacing.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        contact.add(activeSpacing, gridBagConstraints);

        jLabel7.setText("All values are in nanometers");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 0);
        contact.add(jLabel7, gridBagConstraints);

        jLabel8.setText("Contact Parameters");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        contact.add(jLabel8, gridBagConstraints);

        getContentPane().add(contact, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField activeSpacing;
    private javax.swing.JPanel contact;
    private javax.swing.JLabel image;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JTextField metalOverhangAll;
    private javax.swing.JTextField metalOverhangInline;
    private javax.swing.JTextField polyOverhang;
    private javax.swing.JTextField size;
    private javax.swing.JTextField spacing;
    // End of variables declaration//GEN-END:variables

}
