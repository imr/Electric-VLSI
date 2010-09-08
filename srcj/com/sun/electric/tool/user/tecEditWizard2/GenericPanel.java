/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GenericPanel.java
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
package com.sun.electric.tool.user.tecEditWizard2;

import com.sun.electric.tool.user.Resources;
import com.sun.electric.util.TextUtils;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 */
class GenericPanel extends TechEditWizardPanel {
    private final String name;
    private final String title;
    private final List<String> labels;
    private final List<WizardField> fields;
    private JPanel panel;
    private JLabel image;
    private List<JTextField> ruleValues = new ArrayList<JTextField>();
    private List<JTextField> ruleNames = new ArrayList<JTextField>();

    GenericPanel(TechEditWizard parent, String name, String title, List<String> labels, List<WizardField> fields) {
        super(parent, parent.isModal());
        this.name = name;
        this.title = title;
        this.labels = labels;
        this.fields = fields;
		initComponents();
        String imageFileName = name + ".png";
		image.setIcon(Resources.getResource(getClass(), imageFileName));
		pack();
    }
    
	/** return the panel to use for this Numeric Technology Editor tab. */
    @Override
	public Component getComponent() { return panel; }

	/** return the name of this Numeric Technology Editor tab. */
    @Override
	public String getName() { return name; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Active tab.
	 */
    @Override
	public void init()
	{
        for (int i = 0; i < fields.size(); i++) {
            WizardField field = fields.get(i);
            JTextField ruleValue = ruleValues.get(i);
            JTextField ruleName = ruleNames.get(i);
            ruleValue.setText(TextUtils.formatDouble(field.value));
            ruleName.setText(field.rule);
        }
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Active tab.
	 */
    @Override
	public void term()
	{
        for (int i = 0; i < fields.size(); i++) {
            WizardField field = fields.get(i);
            JTextField ruleValue = ruleValues.get(i);
            JTextField ruleName = ruleNames.get(i);
            field.rule = ruleName.getText();
            field.value = TextUtils.atof(ruleValue.getText());
        }
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 */
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle(getName());
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        panel = new javax.swing.JPanel();
        panel.setLayout(new GridBagLayout());

        for (int i = 0; i < fields.size(); i++) {
//            WizardField field = fields.get(i);

            JLabel jLabel = new JLabel();
            jLabel.setText(labels.get(i));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i + 3;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 0);
            panel.add(jLabel, gridBagConstraints);

            JTextField ruleValue = new JTextField();
            ruleValues.add(ruleValue);
            ruleValue.setColumns(8);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = i + 3;
            gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
            panel.add(ruleValue, gridBagConstraints);

        }

        image = new JLabel();
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        panel.add(image, gridBagConstraints);

        JLabel jLabel5 = new JLabel();
        jLabel5.setText("Distances are in nanometers");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = fields.size() + 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 0);
        panel.add(jLabel5, gridBagConstraints);

        JLabel jLabel6 = new JLabel();
        jLabel6.setText(title);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        panel.add(jLabel6, gridBagConstraints);

        for (int i = 0; i < fields.size(); i++) {
//            WizardField field = fields.get(i);

            JTextField ruleName = new JTextField();
            ruleNames.add(ruleName);
            ruleName.setColumns(8);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = i + 3;
            gridBagConstraints.insets = new java.awt.Insets(1, 2, 1, 2);
            panel.add(ruleName, gridBagConstraints);

        }

        JLabel jLabel7 = new JLabel();
        jLabel7.setText("Rule Name");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        panel.add(jLabel7, gridBagConstraints);

        JLabel jLabel8 = new JLabel();
        jLabel8.setText("Distance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        panel.add(jLabel8, gridBagConstraints);

        getContentPane().add(panel, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)
	{
		setVisible(false);
		dispose();
	}
}
