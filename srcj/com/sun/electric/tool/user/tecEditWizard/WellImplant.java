/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WellImplant.java
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
 * Class to handle the "WellImplant" tab of the Numeric Technology Editor dialog.
 */
public class WellImplant extends TechEditWizardPanel
{
	/** Creates new form WellImplant */
	public WellImplant(TechEditWizard parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
		image.setIcon(Resources.getResource(getClass(), "WellImplant.png"));
		pack();
	}

	/** return the panel to use for this Numeric Technology Editor tab. */
	public JPanel getPanel() { return wellImplant; }

	/** return the name of this Numeric Technology Editor tab. */
	public String getName() { return "WellImplant"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the WellImplant tab.
	 */
	public void init()
	{
		TechEditWizardData data = wizard.getTechEditData();
		nPlusWidth.setText(TextUtils.formatDistance(data.getNPlusWidth().v));
		nPlusWidthRule.setText(data.getNPlusWidth().rule);
		nPlusOverhangDiff.setText(TextUtils.formatDistance(data.getNPlusOverhangDiff().v));
		nPlusOverhangDiffRule.setText(data.getNPlusOverhangDiff().rule);
		nPlusOverhangPoly.setText(TextUtils.formatDistance(data.getNPlusOverhangPoly().v));
		nPlusOverhangPolyRule.setText(data.getNPlusOverhangPoly().rule);
        nPlusSpacing.setText(TextUtils.formatDistance(data.getNPlusSpacing().v));
		nPlusSpacingRule.setText(data.getNPlusSpacing().rule);

		pPlusWidth.setText(TextUtils.formatDistance(data.getPPlusWidth().v));
		pPlusWidthRule.setText(data.getPPlusWidth().rule);
		pPlusOverhangDiff.setText(TextUtils.formatDistance(data.getPPlusOverhangDiff().v));
		pPlusOverhangDiffRule.setText(data.getPPlusOverhangDiff().rule);
		pPlusOverhangPoly.setText(TextUtils.formatDistance(data.getPPlusOverhangPoly().v));
		pPlusOverhangPolyRule.setText(data.getPPlusOverhangPoly().rule);
		pPlusSpacing.setText(TextUtils.formatDistance(data.getPPlusSpacing().v));
		pPlusSpacingRule.setText(data.getPPlusSpacing().rule);

		nWellWidth.setText(TextUtils.formatDistance(data.getNWellWidth().v));
		nWellWidthRule.setText(data.getNWellWidth().rule);
		nWellOverhangP.setText(TextUtils.formatDistance(data.getNWellOverhangDiffP().v));
		nWellOverhangRuleP.setText(data.getNWellOverhangDiffP().rule);
        nWellOverhangN.setText(TextUtils.formatDistance(data.getNWellOverhangDiffN().v));
		nWellOverhangRuleN.setText(data.getNWellOverhangDiffN().rule);
        nWellSpacing.setText(TextUtils.formatDistance(data.getNWellSpacing().v));
		nWellSpacingRule.setText(data.getNWellSpacing().rule);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the WellImplant tab.
	 */
	public void term()
	{
		TechEditWizardData data = wizard.getTechEditData();
		data.setNPlusWidth(new WizardField(TextUtils.atofDistance(nPlusWidth.getText()), nPlusWidthRule.getText()));
		data.setNPlusOverhangDiff(new WizardField(TextUtils.atofDistance(nPlusOverhangDiff.getText()), nPlusOverhangDiffRule.getText()));
        data.setNPlusOverhangPoly(new WizardField(TextUtils.atofDistance(nPlusOverhangPoly.getText()), nPlusOverhangPolyRule.getText()));
        data.setNPlusSpacing(new WizardField(TextUtils.atofDistance(nPlusSpacing.getText()), nPlusSpacingRule.getText()));

		data.setPPlusWidth(new WizardField(TextUtils.atofDistance(pPlusWidth.getText()), pPlusWidthRule.getText()));
		data.setPPlusOverhangDiff(new WizardField(TextUtils.atofDistance(pPlusOverhangDiff.getText()), pPlusOverhangDiffRule.getText()));
        data.setPPlusOverhangPoly(new WizardField(TextUtils.atofDistance(pPlusOverhangPoly.getText()), pPlusOverhangPolyRule.getText()));
        data.setPPlusSpacing(new WizardField(TextUtils.atofDistance(pPlusSpacing.getText()), pPlusSpacingRule.getText()));

		data.setNWellWidth(new WizardField(TextUtils.atofDistance(nWellWidth.getText()), nWellWidthRule.getText()));
		data.setNWellOverhangDiffP(new WizardField(TextUtils.atofDistance(nWellOverhangP.getText()), nWellOverhangRuleP.getText()));
        data.setNWellOverhangDiffN(new WizardField(TextUtils.atofDistance(nWellOverhangN.getText()), nWellOverhangRuleN.getText()));
        data.setNWellSpacing(new WizardField(TextUtils.atofDistance(nWellSpacing.getText()), nWellSpacingRule.getText()));
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        wellImplant = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        nPlusWidth = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        nPlusOverhangDiff = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        nPlusSpacing = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        pPlusWidth = new javax.swing.JTextField();
        image = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        pPlusOverhangDiff = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        pPlusSpacing = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        nWellWidth = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        nWellOverhangP = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        nWellSpacing = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        nPlusWidthRule = new javax.swing.JTextField();
        nPlusOverhangDiffRule = new javax.swing.JTextField();
        nPlusSpacingRule = new javax.swing.JTextField();
        pPlusWidthRule = new javax.swing.JTextField();
        pPlusOverhangDiffRule = new javax.swing.JTextField();
        pPlusSpacingRule = new javax.swing.JTextField();
        nWellWidthRule = new javax.swing.JTextField();
        nWellOverhangRuleP = new javax.swing.JTextField();
        nWellSpacingRule = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        nPlusOverhangPoly = new javax.swing.JTextField();
        nPlusOverhangPolyRule = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        pPlusOverhangPolyRule = new javax.swing.JTextField();
        pPlusOverhangPoly = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        nWellOverhangN = new javax.swing.JTextField();
        nWellOverhangRuleN = new javax.swing.JTextField();

        setTitle("Well-Implant");
        setName(""); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        wellImplant.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("NPlus width (A):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 0);
        wellImplant.add(jLabel1, gridBagConstraints);

        nPlusWidth.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        wellImplant.add(nPlusWidth, gridBagConstraints);

        jLabel2.setText("NPlus active overhang (B):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 0);
        wellImplant.add(jLabel2, gridBagConstraints);

        nPlusOverhangDiff.setColumns(8);
        nPlusOverhangDiff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nPlusOverhangDiffActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        wellImplant.add(nPlusOverhangDiff, gridBagConstraints);

        jLabel3.setText("NPlus spacing (D):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 0);
        wellImplant.add(jLabel3, gridBagConstraints);

        nPlusSpacing.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 4, 2);
        wellImplant.add(nPlusSpacing, gridBagConstraints);

        jLabel4.setText("PPlus width (E):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 0);
        wellImplant.add(jLabel4, gridBagConstraints);

        pPlusWidth.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 1, 2);
        wellImplant.add(pPlusWidth, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellImplant.add(image, gridBagConstraints);

        jLabel5.setText("PPlus active overhang (F):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 0);
        wellImplant.add(jLabel5, gridBagConstraints);

        pPlusOverhangDiff.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        wellImplant.add(pPlusOverhangDiff, gridBagConstraints);

        jLabel6.setText("PPlus spacing (H):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 0);
        wellImplant.add(jLabel6, gridBagConstraints);

        pPlusSpacing.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 4, 2);
        wellImplant.add(pPlusSpacing, gridBagConstraints);

        jLabel7.setText("NWell width (I):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 0);
        wellImplant.add(jLabel7, gridBagConstraints);

        nWellWidth.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 1, 2);
        wellImplant.add(nWellWidth, gridBagConstraints);

        jLabel8.setText("NWell P active overhang (J):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 0);
        wellImplant.add(jLabel8, gridBagConstraints);

        nWellOverhangP.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        wellImplant.add(nWellOverhangP, gridBagConstraints);

        jLabel9.setText("NWell spacing (L):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 0);
        wellImplant.add(jLabel9, gridBagConstraints);

        nWellSpacing.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        wellImplant.add(nWellSpacing, gridBagConstraints);

        jLabel10.setText("Distances are in nanometers");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 0);
        wellImplant.add(jLabel10, gridBagConstraints);

        jLabel11.setText("Well / Implant Parameters");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        wellImplant.add(jLabel11, gridBagConstraints);

        jLabel12.setText("Distance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        wellImplant.add(jLabel12, gridBagConstraints);

        jLabel13.setText("Rule Name");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        wellImplant.add(jLabel13, gridBagConstraints);

        nPlusWidthRule.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        wellImplant.add(nPlusWidthRule, gridBagConstraints);

        nPlusOverhangDiffRule.setColumns(8);
        nPlusOverhangDiffRule.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nPlusOverhangDiffRuleActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        wellImplant.add(nPlusOverhangDiffRule, gridBagConstraints);

        nPlusSpacingRule.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        wellImplant.add(nPlusSpacingRule, gridBagConstraints);

        pPlusWidthRule.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 1, 2);
        wellImplant.add(pPlusWidthRule, gridBagConstraints);

        pPlusOverhangDiffRule.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        wellImplant.add(pPlusOverhangDiffRule, gridBagConstraints);

        pPlusSpacingRule.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        wellImplant.add(pPlusSpacingRule, gridBagConstraints);

        nWellWidthRule.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 1, 2);
        wellImplant.add(nWellWidthRule, gridBagConstraints);

        nWellOverhangRuleP.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        wellImplant.add(nWellOverhangRuleP, gridBagConstraints);

        nWellSpacingRule.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        wellImplant.add(nWellSpacingRule, gridBagConstraints);

        jLabel14.setText("NPlus poly overhang (C):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 0);
        wellImplant.add(jLabel14, gridBagConstraints);

        nPlusOverhangPoly.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 4, 2);
        wellImplant.add(nPlusOverhangPoly, gridBagConstraints);

        nPlusOverhangPolyRule.setColumns(8);
        nPlusOverhangPolyRule.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nPlusOverhangPolyRuleActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        wellImplant.add(nPlusOverhangPolyRule, gridBagConstraints);

        jLabel15.setText("PPlus poly overhang (G):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 0);
        wellImplant.add(jLabel15, gridBagConstraints);

        pPlusOverhangPolyRule.setColumns(8);
        pPlusOverhangPolyRule.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pPlusOverhangPolyRuleActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        wellImplant.add(pPlusOverhangPolyRule, gridBagConstraints);

        pPlusOverhangPoly.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        wellImplant.add(pPlusOverhangPoly, gridBagConstraints);

        jLabel16.setText("NWell N active overhang (K):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 0);
        wellImplant.add(jLabel16, gridBagConstraints);

        nWellOverhangN.setColumns(8);
        nWellOverhangN.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nWellOverhangNActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        wellImplant.add(nWellOverhangN, gridBagConstraints);

        nWellOverhangRuleN.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        wellImplant.add(nWellOverhangRuleN, gridBagConstraints);

        getContentPane().add(wellImplant, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

        private void nPlusOverhangPolyRuleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nPlusOverhangPolyRuleActionPerformed
            // TODO add your handling code here:
}//GEN-LAST:event_nPlusOverhangPolyRuleActionPerformed

        private void nPlusOverhangDiffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nPlusOverhangDiffActionPerformed
            // TODO add your handling code here:
}//GEN-LAST:event_nPlusOverhangDiffActionPerformed

        private void pPlusOverhangPolyRuleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pPlusOverhangPolyRuleActionPerformed
            // TODO add your handling code here:
}//GEN-LAST:event_pPlusOverhangPolyRuleActionPerformed

        private void nPlusOverhangDiffRuleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nPlusOverhangDiffRuleActionPerformed
            // TODO add your handling code here:
}//GEN-LAST:event_nPlusOverhangDiffRuleActionPerformed

private void nWellOverhangNActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nWellOverhangNActionPerformed
// TODO add your handling code here:
}//GEN-LAST:event_nWellOverhangNActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel image;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JTextField nPlusOverhangDiff;
    private javax.swing.JTextField nPlusOverhangDiffRule;
    private javax.swing.JTextField nPlusOverhangPoly;
    private javax.swing.JTextField nPlusOverhangPolyRule;
    private javax.swing.JTextField nPlusSpacing;
    private javax.swing.JTextField nPlusSpacingRule;
    private javax.swing.JTextField nPlusWidth;
    private javax.swing.JTextField nPlusWidthRule;
    private javax.swing.JTextField nWellOverhangN;
    private javax.swing.JTextField nWellOverhangP;
    private javax.swing.JTextField nWellOverhangRuleN;
    private javax.swing.JTextField nWellOverhangRuleP;
    private javax.swing.JTextField nWellSpacing;
    private javax.swing.JTextField nWellSpacingRule;
    private javax.swing.JTextField nWellWidth;
    private javax.swing.JTextField nWellWidthRule;
    private javax.swing.JTextField pPlusOverhangDiff;
    private javax.swing.JTextField pPlusOverhangDiffRule;
    private javax.swing.JTextField pPlusOverhangPoly;
    private javax.swing.JTextField pPlusOverhangPolyRule;
    private javax.swing.JTextField pPlusSpacing;
    private javax.swing.JTextField pPlusSpacingRule;
    private javax.swing.JTextField pPlusWidth;
    private javax.swing.JTextField pPlusWidthRule;
    private javax.swing.JPanel wellImplant;
    // End of variables declaration//GEN-END:variables

}
