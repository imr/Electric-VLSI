/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FillGenDialog.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.extract.LayerCoverageTool;
import com.sun.electric.tool.generator.layout.TechType;
import com.sun.electric.tool.generator.layout.fill.FillGenConfig;
import com.sun.electric.tool.generator.layout.fill.FillGenJob;
import com.sun.electric.tool.generator.layout.fill.FillGeneratorTool;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.util.TextUtils;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/**
 * Unused class to manage fill generators.
 */
public class FillGenDialog extends EDialog {
    private JCheckBox[] tiledCells;
    private List<FillGenButton> metalOptions = new ArrayList<FillGenButton>();
    private Technology tech;

    private static class FillGenButton extends JCheckBox
    {
        JTextField vddSpace, vddWidth, gndSpace, gndWidth;
        JComboBox vddUnit, vddWUnit, gndUnit, gndWUnit;
        int metal;

        FillGenButton(int metal)
        {
            super("Metal " + metal);
            this.metal = metal;
        }
    }

    /** Creates new form FillGenDialog */
    public FillGenDialog(Technology tech, Frame parent) {
        super(parent, true);

        this.tech = tech;

        // Setting the correct default
        initComponents();

        templateButton.setSelected(true);

        // top group
        topGroup.add(templateButton);

        assert(tech != null);
        // Setting the title
        setTitle("Fill Cell Generator for '" + tech.getTechName() + "'");

        int numMetals = tech.getNumMetals();
        String[] units = new String[] { "lambda", "tracks" };

        for (int i = 0; i < numMetals; i++)
        {
            int metal = i+1;
            Layer metalLayer = tech.findLayer("Metal-"+metal);
            DRCTemplate rule = DRC.getSpacingRule(metalLayer, null, metalLayer, null, false, -1, 0, 0);

            FillGenButton button = new FillGenButton(metal);
            metalOptions.add(button);
            java.awt.GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = metal;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            metalPanel.add(button, gridBagConstraints);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    metalOptionActionPerformed(evt);
                }
            });
            button.setSelected(false);

            // vdd space
            JTextField text = new JTextField();
            button.vddSpace = text;
            text.setColumns(4);
            text.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
            if (rule != null)
                text.setText(TextUtils.formatDistance(rule.getValue(0)));
            text.setMinimumSize(new Dimension(40, 21));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = metal;
            metalPanel.add(text, gridBagConstraints);

            // vdd space unit
            JComboBox combox = new JComboBox();
            button.vddUnit = combox;
            combox.setModel(new javax.swing.DefaultComboBoxModel(units));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = metal;
            metalPanel.add(combox, gridBagConstraints);

            // Gnd space
            text = new JTextField();
            button.gndSpace = text;
            text.setColumns(4);
            text.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
            text.setText(TextUtils.formatDistance(rule.getValue(0)));
            text.setMinimumSize(new Dimension(40, 21));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 3;
            gridBagConstraints.gridy = metal;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            metalPanel.add(text, gridBagConstraints);

            // Gnd space unit
            combox = new JComboBox();
            button.gndUnit = combox;
            combox.setModel(new DefaultComboBoxModel(units));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 4;
            gridBagConstraints.gridy = metal;
            metalPanel.add(combox, gridBagConstraints);

            // Min size rule
            rule = DRC.getMinValue(metalLayer, DRCTemplate.DRCRuleType.MINWID);

            // vdd width
            text = new JTextField();
            button.vddWidth = text;
            text.setColumns(4);
            text.setHorizontalAlignment(JTextField.TRAILING);
            text.setText(TextUtils.formatDistance(rule.getValue(0)));
            text.setMinimumSize(new Dimension(40, 21));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 5;
            gridBagConstraints.gridy = metal;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            metalPanel.add(text, gridBagConstraints);

            // vdd width unit
            combox = new JComboBox();
            button.vddWUnit = combox;
            combox.setModel(new DefaultComboBoxModel(units));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 6;
            gridBagConstraints.gridy = metal;
            metalPanel.add(combox, gridBagConstraints);

            // Gnd width
            text = new JTextField();
            button.gndWidth = text;
            text.setColumns(4);
            text.setHorizontalAlignment(JTextField.TRAILING);
            text.setText(TextUtils.formatDistance(rule.getValue(0)));
            text.setMinimumSize(new Dimension(40, 21));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 7;
            gridBagConstraints.gridy = metal;
            gridBagConstraints.insets = new Insets(0, 5, 0, 0);
            metalPanel.add(text, gridBagConstraints);

            // Gnd width unit
            combox = new JComboBox();
            button.gndWUnit = combox;
            combox.setModel(new DefaultComboBoxModel(units));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 8;
            gridBagConstraints.gridy = metal;
            metalPanel.add(combox, gridBagConstraints);
        }

        // Loading tiles information
        tiledCells = new JCheckBox[12];
        tiledCells[0] = jCheckBox1;
        tiledCells[1] = jCheckBox2;
        tiledCells[2] = jCheckBox3;
        tiledCells[3] = jCheckBox4;
        tiledCells[4] = jCheckBox5;
        tiledCells[5] = jCheckBox6;
        tiledCells[6] = jCheckBox7;
        tiledCells[7] = jCheckBox8;
        tiledCells[8] = jCheckBox9;
        tiledCells[9] = jCheckBox10;
        tiledCells[10] = jCheckBox11;
        tiledCells[11] = jCheckBox12;

        try
        {
            Class<?> extraPanelClass = Class.forName("com.sun.electric.plugins.generator.FillCelllGenPanel");
            Constructor instance = extraPanelClass.getDeclaredConstructor(FillGenDialog.class, JPanel.class,
            ButtonGroup.class, JButton.class, JRadioButton.class);  // using varargs
            instance.newInstance(this, floorplanPanel, topGroup, okButton, templateButton); // using varargs
        } catch (Exception e)
        {
             if (Job.getDebug())
                System.out.println("GNU Release can't find the Fill Cell Generator dialog");
            // Adding here the default OKAction
            okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed();
            }
            });

            optionActionPerformed();
        }

        finishInitialization();
        setVisible(true);
   }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        topGroup = new javax.swing.ButtonGroup();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        floorplanPanel = new javax.swing.JPanel();
        metalPanel = new javax.swing.JPanel();
        vddSpaceLabel = new javax.swing.JLabel();
        vddWidthLabel = new javax.swing.JLabel();
        gndSpaceLabel = new javax.swing.JLabel();
        gndWidthLabel = new javax.swing.JLabel();
        templatePanel = new javax.swing.JPanel();
        masterDimPanel = new javax.swing.JPanel();
        jTextField2 = new javax.swing.JTextField();
        jTextField1 = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        otherMasterPanel = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        templateButton = new javax.swing.JRadioButton();
        tilingPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jCheckBox1 = new javax.swing.JCheckBox();
        jCheckBox2 = new javax.swing.JCheckBox();
        jCheckBox3 = new javax.swing.JCheckBox();
        jCheckBox4 = new javax.swing.JCheckBox();
        jCheckBox5 = new javax.swing.JCheckBox();
        jCheckBox6 = new javax.swing.JCheckBox();
        jCheckBox7 = new javax.swing.JCheckBox();
        jCheckBox8 = new javax.swing.JCheckBox();
        jCheckBox9 = new javax.swing.JCheckBox();
        jCheckBox10 = new javax.swing.JCheckBox();
        jCheckBox11 = new javax.swing.JCheckBox();
        jCheckBox12 = new javax.swing.JCheckBox();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Fill Cell Generator");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jTabbedPane1.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        jTabbedPane1.setMaximumSize(new java.awt.Dimension(327670, 327670));
        jTabbedPane1.setMinimumSize(new java.awt.Dimension(600, 500));
        jTabbedPane1.setPreferredSize(new java.awt.Dimension(635, 500));

        floorplanPanel.setMinimumSize(new java.awt.Dimension(550, 300));
        floorplanPanel.setPreferredSize(new java.awt.Dimension(630, 350));
        floorplanPanel.setLayout(new java.awt.GridBagLayout());

        metalPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Reserved Space"));
        metalPanel.setLayout(new java.awt.GridBagLayout());

        vddSpaceLabel.setText("Vdd Space");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        metalPanel.add(vddSpaceLabel, gridBagConstraints);

        vddWidthLabel.setText("Vdd Width");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        metalPanel.add(vddWidthLabel, gridBagConstraints);

        gndSpaceLabel.setText("Gnd Space");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        metalPanel.add(gndSpaceLabel, gridBagConstraints);

        gndWidthLabel.setText("Gnd Width");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        metalPanel.add(gndWidthLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 9;
        gridBagConstraints.gridheight = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        floorplanPanel.add(metalPanel, gridBagConstraints);

        templatePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Master Cell"));
        templatePanel.setLayout(new java.awt.GridBagLayout());

        masterDimPanel.setLayout(new java.awt.GridBagLayout());

        jTextField2.setColumns(8);
        jTextField2.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField2.setText("128");
        jTextField2.setMinimumSize(new java.awt.Dimension(100, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        masterDimPanel.add(jTextField2, gridBagConstraints);

        jTextField1.setColumns(8);
        jTextField1.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField1.setText("245");
        jTextField1.setMinimumSize(new java.awt.Dimension(100, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        masterDimPanel.add(jTextField1, gridBagConstraints);

        jLabel3.setText("Width (lambda)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        masterDimPanel.add(jLabel3, gridBagConstraints);

        jLabel5.setText("Height (lambda)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        masterDimPanel.add(jLabel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        templatePanel.add(masterDimPanel, gridBagConstraints);

        otherMasterPanel.setLayout(new java.awt.GridBagLayout());

        jLabel6.setText("Even layer orientation");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        otherMasterPanel.add(jLabel6, gridBagConstraints);

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "horiz", "vert" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        otherMasterPanel.add(jComboBox1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        templatePanel.add(otherMasterPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        floorplanPanel.add(templatePanel, gridBagConstraints);

        templateButton.setText("Template Fill");
        templateButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        templateButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        floorplanPanel.add(templateButton, gridBagConstraints);

        jTabbedPane1.addTab("Floorplan", floorplanPanel);

        tilingPanel.setLayout(new java.awt.GridBagLayout());

        jLabel2.setFont(new java.awt.Font("MS Sans Serif", 1, 14));
        jLabel2.setText("Which tiled cells to generate");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        tilingPanel.add(jLabel2, gridBagConstraints);

        jCheckBox1.setText("2 x 2");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        tilingPanel.add(jCheckBox1, gridBagConstraints);

        jCheckBox2.setText("3 x 3");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        tilingPanel.add(jCheckBox2, gridBagConstraints);

        jCheckBox3.setText("4 x 4");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        tilingPanel.add(jCheckBox3, gridBagConstraints);

        jCheckBox4.setText("5 x 5");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        tilingPanel.add(jCheckBox4, gridBagConstraints);

        jCheckBox5.setText("6 x 6");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        tilingPanel.add(jCheckBox5, gridBagConstraints);

        jCheckBox6.setText("7 x 7");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        tilingPanel.add(jCheckBox6, gridBagConstraints);

        jCheckBox7.setText("8 x 8");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        tilingPanel.add(jCheckBox7, gridBagConstraints);

        jCheckBox8.setText("9 x 9");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        tilingPanel.add(jCheckBox8, gridBagConstraints);

        jCheckBox9.setText("10 x 10");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        tilingPanel.add(jCheckBox9, gridBagConstraints);

        jCheckBox10.setText("11 x 11");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        tilingPanel.add(jCheckBox10, gridBagConstraints);

        jCheckBox11.setText("12 x 12");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        tilingPanel.add(jCheckBox11, gridBagConstraints);

        jCheckBox12.setText("13 x 13");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        tilingPanel.add(jCheckBox12, gridBagConstraints);

        jTabbedPane1.addTab("Tiling", tilingPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(jTabbedPane1, gridBagConstraints);

        okButton.setText("OK");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(okButton, gridBagConstraints);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cancelButton, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void metalOptionActionPerformed(java.awt.event.ActionEvent evt)
    {
        FillGenButton b = (FillGenButton)evt.getSource();
        setMetalOption(b, false); // MISSING install observer/observable
    }

    private void setMetalOption(FillGenButton b, boolean flatSelected)
    {
        boolean option = b.isSelected();
        b.vddSpace.setEnabled(option);
        b.vddUnit.setEnabled(option);
        b.gndSpace.setEnabled(option);
        b.gndUnit.setEnabled(option);
        boolean value = /*flatSelected &&*/ option && !templateButton.isSelected();
        b.vddWidth.setEnabled(value);
        b.vddWUnit.setEnabled(value);
        b.gndWidth.setEnabled(value);
        b.gndWUnit.setEnabled(value);
    }

    private void setMetalOptions(boolean flatSelected)
    {
        for (FillGenButton b : metalOptions)
        {
            setMetalOption(b, flatSelected);
        }
    }

    public void generalSetup(boolean flatSelected, boolean createMaster)
    {
        boolean value = !flatSelected && createMaster || templateButton.isSelected();
        setEnabledInHierarchy(masterDimPanel, value);

        value = value || flatSelected;
        setEnabledInHierarchy(metalPanel, value);
        setMetalOptions(flatSelected);
        setEnabledInHierarchy(otherMasterPanel, value);
    }

    private void optionActionPerformed() {
        optionAction(false, false, false);
    }

    public void optionAction(boolean flatSelected, boolean createMaster, boolean isCellSelected)
    {
        // Disable tiling
        setEnabledInHierarchy(tilingPanel, !isCellSelected);
        // Calls master select setting
        generalSetup(flatSelected, createMaster);
    }

    private static void setEnabledInHierarchy(Container c, boolean value)
    {
        c.setEnabled(value);
        for (int i = 0; i < c.getComponentCount(); i++)
        {
            Component co = c.getComponent(i);
            co.setEnabled(value);
            if (co instanceof Container)
               setEnabledInHierarchy((Container)co, value);
        }
    }

    private void okButtonActionPerformed() {
        FillGenConfig config = okButtonClick(false, false, false, false, 0, FillGenConfig.FillGenType.INTERNAL, -1);
        if (config != null)
            new FillGenJob(Job.getUserInterface().getCurrentCell(), config, false, new LayerCoverageTool.LayerCoveragePreferences(false));
    }

    public FillGenConfig okButtonClick(boolean isFlatSelected, boolean createMaster, boolean binary, boolean around,
                                       double gap, FillGenConfig.FillGenType genType, int level)
    {
        boolean hierarchy = (!isFlatSelected);
        boolean useMaster = hierarchy && !createMaster;
        boolean even = (jComboBox1.getModel().getSelectedItem().equals("horiz"));

        FillGeneratorTool.Units LAMBDA = FillGeneratorTool.LAMBDA;
        FillGeneratorTool.Units TRACKS = FillGeneratorTool.TRACKS;

        int firstMetal = -1, lastMetal = -1;
        double vddReserve = 0, gndReserve = 0;
        double vddWidth = 0, gndWidth = 0;

        for (FillGenButton b : metalOptions)
        {
            if (!b.isSelected()) continue;
            double vddS = TextUtils.atofDistance(b.vddSpace.getText());
            double gndS = TextUtils.atofDistance(b.gndSpace.getText());
            double vddW = TextUtils.atofDistance(b.vddWidth.getText());
            double gndW = TextUtils.atofDistance(b.gndWidth.getText());
//            if (vddS > -1 && gndS > -1)
            {
                if (firstMetal == -1) firstMetal = b.metal;
                lastMetal = b.metal;
                if (vddS > vddReserve) vddReserve = vddS;  //@@TODO we don't check that units are identical
                if (gndS > gndReserve) gndReserve = gndS;
                if (vddW > vddWidth) vddWidth = vddW;
                if (gndW > gndWidth) gndWidth = gndW;
            }
        }

        if (!useMaster && vddReserve == 0 && gndReserve == 0) // nothing reserve
        {
            System.out.println("Nothing reserve");
            return null;
        }

        // This assumes the wires are long enough for wide values are going to be retrieved
        double drcSpacingRule = DRC.getWorstSpacingDistance(tech, lastMetal); // only metals
        double width = TextUtils.atofDistance(jTextField1.getText());
        double height = TextUtils.atofDistance(jTextField2.getText());

        // Only when the fill will be with respect to a given cell

        FillGeneratorTool.FillTypeEnum type = FillGeneratorTool.FillTypeEnum.TEMPLATE;
        Cell cellToFill = Job.getUserInterface().getCurrentCell();

        if (!templateButton.isSelected())
        {
            if (cellToFill == null)
            {
                System.out.println("No cell to fill");
                return null;
            }
            type = FillGeneratorTool.FillTypeEnum.CELL;
            Rectangle2D bnd = cellToFill.getBounds();
            width = bnd.getWidth();
            height = bnd.getHeight();
        }

        List<Integer> items = new ArrayList<Integer>(12);

        for (int i = 0; i < tiledCells.length; i++)
            {
            if (tiledCells[i].getModel().isSelected())
                items.add(new Integer(i+2));
        }
        int[] cells = null;
        if (items.size() > 0)
        {
            cells = new int[items.size()];
            for (int i = 0; i < items.size(); i++)
                cells[i] = items.get(i).intValue();
        }

        // testing new code
        boolean withWidth = false;
        // Width and Height are either the w/h of the template to create or the size of the cell to fill
        FillGenConfig config = new FillGenConfig(tech, type, "autoFillLib",
                FillGeneratorTool.PERIMETER, firstMetal, lastMetal, width, height,
                even, cells, hierarchy, 0.1, drcSpacingRule, binary,
                useMaster, around, gap, genType, level);

        if (!templateButton.isSelected())
        {
            Rectangle2D bnd = cellToFill.getBounds();
            double minSize = vddReserve + gndReserve + 2*drcSpacingRule + 2*gndReserve + 2*vddReserve;
            minSize = vddReserve + gndReserve + 2*drcSpacingRule + 2*vddWidth + 2*gndWidth;
            withWidth = true;
            config.setTargetValues(bnd.getWidth(), bnd.getHeight(), minSize, minSize);
        }

        boolean metalW = isFlatSelected || withWidth;
        for (FillGenButton b : metalOptions)
            {
            if (!b.isSelected()) continue;
            double vddVal = TextUtils.atofDistance(b.vddSpace.getText());
            double gndVal = TextUtils.atofDistance(b.gndSpace.getText());
            FillGeneratorTool.Units vddU = TRACKS;
            if (b.vddUnit.getModel().getSelectedItem().equals("lambda"))
                vddU = LAMBDA;
            FillGeneratorTool.Units gndU = TRACKS;
            if (b.gndUnit.getModel().getSelectedItem().equals("lambda"))
                gndU = LAMBDA;

            FillGenConfig.ReserveConfig c = config.reserveSpaceOnLayer(tech, b.metal, vddVal, vddU, gndVal, gndU);

            // Width values
            if (metalW)
            {
                vddU = TRACKS;
                if (b.vddUnit.getModel().getSelectedItem().equals("lambda"))
                    vddU = LAMBDA;
                gndU = TRACKS;
                if (b.gndUnit.getModel().getSelectedItem().equals("lambda"))
                    gndU = LAMBDA;
                vddVal = TextUtils.atofDistance(b.vddWidth.getText());
                gndVal = TextUtils.atofDistance(b.gndWidth.getText());

                c.reserveWidthOnLayer(vddVal, vddU, gndVal, gndU);
            }
        }

        setVisible(false);
        return config;
    }

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        closeDialog();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
//        System.out.println("that's all folks");
//        System.exit(0);
    }//GEN-LAST:event_formWindowClosed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        new FillGenDialog(null, new JFrame());
    }

    public static void openFillGeneratorDialog(Technology tech)
    {
        new FillGenDialog(tech, TopLevel.getCurrentJFrame());
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JPanel floorplanPanel;
    private javax.swing.JLabel gndSpaceLabel;
    private javax.swing.JLabel gndWidthLabel;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox10;
    private javax.swing.JCheckBox jCheckBox11;
    private javax.swing.JCheckBox jCheckBox12;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JCheckBox jCheckBox3;
    private javax.swing.JCheckBox jCheckBox4;
    private javax.swing.JCheckBox jCheckBox5;
    private javax.swing.JCheckBox jCheckBox6;
    private javax.swing.JCheckBox jCheckBox7;
    private javax.swing.JCheckBox jCheckBox8;
    private javax.swing.JCheckBox jCheckBox9;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JPanel masterDimPanel;
    private javax.swing.JPanel metalPanel;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel otherMasterPanel;
    private javax.swing.JRadioButton templateButton;
    private javax.swing.JPanel templatePanel;
    private javax.swing.JPanel tilingPanel;
    private javax.swing.ButtonGroup topGroup;
    private javax.swing.JLabel vddSpaceLabel;
    private javax.swing.JLabel vddWidthLabel;
    // End of variables declaration//GEN-END:variables
}
