/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FillGen.java
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

import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.generator.layout.FillGenerator;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.hierarchy.Cell;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Unused class to manage fill generators.
 */
public class FillGen extends EDialog {

    private JTextField[] vddSpace;
    private JComboBox[] vddUnit;
    private JTextField[] gndSpace;
    private JComboBox[] gndUnit;
    private JCheckBox[] tiledCells;
    private Cell cellToFill;

    /** Creates new form FillGen */
    public FillGen(Technology tech, java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();

        cellToFill = Job.getUserInterface().getCurrentCell();

        // Putting buttons in group
        autoFillGroup.add(flatFill);
        autoFillGroup.add(binaryFill);
        autoFillGroup.add(adaptiveFill);
        flatFill.setSelected(true);
        // master
        masterGroup.add(createButton);
        masterGroup.add(selectButton);
        createButton.setSelected(true);

        // top group
        topGroup.add(templateButton);
        topGroup.add(cellButton);
        if (cellToFill != null)
           cellButton.setSelected(true);
        else
        {
            cellButton.setEnabled(false); // you can't select it if cell is null.
            templateButton.setSelected(true);
        }
        optionActionPerformed(null);


        int numMetals = (tech == null) ? 6 : tech.getNumMetals();
        int size = numMetals - 1;
        vddSpace = new JTextField[size];
        vddUnit = new JComboBox[size];
        gndSpace = new JTextField[size];
        gndUnit = new JComboBox[size];

        for (int i = 1; i < numMetals; i++)
        {
            JLabel label = new javax.swing.JLabel();

            label.setText("Metal " + (i+1));
            GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            metalPanel.add(label, gridBagConstraints);

            JTextField text = new JTextField();
            vddSpace[i-1] = text;
            text.setColumns(8);
            text.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
            text.setText("-1");
            text.setMinimumSize(new java.awt.Dimension(100, 21));
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = i;
            metalPanel.add(text, gridBagConstraints);

            JComboBox combox = new JComboBox();
            vddUnit[i-1] = combox;
            combox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "lambda", "tracks" }));
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = i;
            metalPanel.add(combox, gridBagConstraints);

            text = new JTextField();
            gndSpace[i-1] = text;
            text.setColumns(8);
            text.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
            text.setText("0");
            text.setMinimumSize(new java.awt.Dimension(100, 21));
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 3;
            gridBagConstraints.gridy = i;
            gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
            metalPanel.add(text, gridBagConstraints);

            combox = new JComboBox();
            gndUnit[i-1] = combox;
            combox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "lambda", "tracks" }));
//            combox.addActionListener(new java.awt.event.ActionListener() {
//                public void actionPerformed(java.awt.event.ActionEvent evt) {
//                    jComboBox2ActionPerformed(evt);
//                }
//            });
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 4;
            gridBagConstraints.gridy = i;
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

		finishInitialization();
   }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        autoFillGroup = new javax.swing.ButtonGroup();
        masterGroup = new javax.swing.ButtonGroup();
        topGroup = new javax.swing.ButtonGroup();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        floorplanPanel = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        metalPanel = new javax.swing.JPanel();
        templatePanel = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        templateButton = new javax.swing.JRadioButton();
        cellButton = new javax.swing.JRadioButton();
        cellPanel = new javax.swing.JPanel();
        flatFill = new javax.swing.JRadioButton();
        binaryFill = new javax.swing.JRadioButton();
        adaptiveFill = new javax.swing.JRadioButton();
        masterSelPanel = new javax.swing.JPanel();
        createButton = new javax.swing.JRadioButton();
        selectButton = new javax.swing.JRadioButton();
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
        jPanel3 = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Fill Cell Generator");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        jTabbedPane1.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        jTabbedPane1.setMaximumSize(new java.awt.Dimension(327670, 327670));
        jTabbedPane1.setPreferredSize(new java.awt.Dimension(470, 300));
        floorplanPanel.setLayout(new java.awt.GridBagLayout());

        jLabel7.setText("Vdd reserved space");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        floorplanPanel.add(jLabel7, gridBagConstraints);

        jLabel8.setText("gnd reserved space");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        floorplanPanel.add(jLabel8, gridBagConstraints);

        metalPanel.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        floorplanPanel.add(metalPanel, gridBagConstraints);

        templatePanel.setLayout(new java.awt.GridBagLayout());

        templatePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Master Cell"));
        jLabel6.setText("Even layer orientation");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        templatePanel.add(jLabel6, gridBagConstraints);

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "horizontal", "vertical" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        templatePanel.add(jComboBox1, gridBagConstraints);

        jTextField1.setColumns(8);
        jTextField1.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField1.setText("245");
        jTextField1.setMinimumSize(new java.awt.Dimension(100, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        templatePanel.add(jTextField1, gridBagConstraints);

        jTextField2.setColumns(8);
        jTextField2.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField2.setText("128");
        jTextField2.setMinimumSize(new java.awt.Dimension(100, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        templatePanel.add(jTextField2, gridBagConstraints);

        jLabel5.setText("Height (lambda)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        templatePanel.add(jLabel5, gridBagConstraints);

        jLabel3.setText("Width (lambda)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        templatePanel.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        floorplanPanel.add(templatePanel, gridBagConstraints);

        templateButton.setText("Template Fill");
        templateButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        templateButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        templateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optionActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 0;
        floorplanPanel.add(templateButton, gridBagConstraints);

        cellButton.setText("Fill Cell");
        cellButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        cellButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        cellButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optionActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        floorplanPanel.add(cellButton, gridBagConstraints);

        cellPanel.setLayout(new java.awt.GridBagLayout());

        cellPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Fill Type"));
        flatFill.setText("Flat Fill");
        flatFill.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        flatFill.setMargin(new java.awt.Insets(0, 0, 0, 0));
        flatFill.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generalActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        cellPanel.add(flatFill, gridBagConstraints);

        binaryFill.setText("Binary Fill");
        binaryFill.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        binaryFill.setMargin(new java.awt.Insets(0, 0, 0, 0));
        binaryFill.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generalActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        cellPanel.add(binaryFill, gridBagConstraints);

        adaptiveFill.setText("Adaptive Fill");
        adaptiveFill.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        adaptiveFill.setMargin(new java.awt.Insets(0, 0, 0, 0));
        adaptiveFill.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generalActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        cellPanel.add(adaptiveFill, gridBagConstraints);

        masterSelPanel.setLayout(new java.awt.GridBagLayout());

        masterSelPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        createButton.setText("Create Master");
        createButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        createButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        createButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        masterSelPanel.add(createButton, gridBagConstraints);

        selectButton.setText("Given Master");
        selectButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        selectButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        selectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        masterSelPanel.add(selectButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        cellPanel.add(masterSelPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        floorplanPanel.add(cellPanel, gridBagConstraints);

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
        gridBagConstraints.gridwidth = 2;
        getContentPane().add(jTabbedPane1, gridBagConstraints);

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        jPanel3.add(okButton);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        jPanel3.add(cancelButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        getContentPane().add(jPanel3, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void setEnableInMasterData()
    {
        boolean value = !flatFill.isSelected() && createButton.isSelected() || templateButton.isSelected();
        setEnabledInHierarchy(templatePanel, value);
        setEnabledInHierarchy(metalPanel, value);
    }

    private void createActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createActionPerformed
        setEnableInMasterData();
    }//GEN-LAST:event_createActionPerformed

    private void generalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generalActionPerformed
        boolean isFlatSelected = flatFill.isSelected();
        setEnabledInHierarchy(masterSelPanel, !isFlatSelected);
        setEnableInMasterData();
    }//GEN-LAST:event_generalActionPerformed

    private void optionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optionActionPerformed
        boolean isCellSelected = cellButton.isSelected();
        setEnabledInHierarchy(cellPanel, isCellSelected);
        // Disable tiling
        setEnabledInHierarchy(tilingPanel, !isCellSelected);
        // Calls master select setting
        generalActionPerformed(null);
    }//GEN-LAST:event_optionActionPerformed

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

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        boolean hierarchy = !flatFill.isSelected();
        Technology tech = (cellToFill != null) ? cellToFill.getTechnology() : Technology.getCurrent();
        FillGenerator fg = new FillGenerator(tech);
        fg.setFillLibrary("autoFillLib");

        if (jComboBox1.getModel().getSelectedItem().equals("horizontal"))
            fg.makeEvenLayersHorizontal(true);

        fg.setBinaryMode(binaryFill.isSelected());

        FillGenerator.Units LAMBDA = FillGenerator.LAMBDA;
        FillGenerator.Units TRACKS = FillGenerator.TRACKS;
        int firstMetal = -1, lastMetal = -1;

        double vddReserve = 0, gndReserve = 0;

        for (int i = 0; i < vddSpace.length; i++)
        {
            int vddS = TextUtils.atoi(vddSpace[i].getText());
            int gndS = TextUtils.atoi(gndSpace[i].getText());
            FillGenerator.Units vddU = TRACKS;
            if (vddUnit[i].getModel().getSelectedItem().equals("lambda"))
                vddU = LAMBDA;
            FillGenerator.Units gndU = TRACKS;
            if (gndUnit[i].getModel().getSelectedItem().equals("lambda"))
                gndU = LAMBDA;
            if (vddS > -1 && gndS > -1)
            {
                if (firstMetal == -1) firstMetal = i+2;
                lastMetal = i+2;
                fg.reserveSpaceOnLayer(i+2, vddS, vddU, gndS, gndU);
                if (vddS > vddReserve) vddReserve = vddS;
                if (gndS > gndReserve) gndReserve = gndS;
            }
        }
        double drcSpacingRule = 6;               //@TODO this value should be calculated!!!
        fg.setDRCSpacing(drcSpacingRule);

        double width = TextUtils.atof(jTextField1.getText());
        double height = TextUtils.atof(jTextField2.getText());

        // Only when the fill will be with respect to a given cell
        if (!templateButton.isSelected())
        {
            Rectangle2D bnd = cellToFill.getBounds();
            width = bnd.getWidth();
            height = bnd.getHeight();
            double minSize = vddReserve + gndReserve + 2*drcSpacingRule + 2*gndReserve + 2*vddReserve;
            fg.setTargetValues(bnd.getWidth(), bnd.getHeight(), minSize, minSize);
        }
        fg.setFillCellWidth(width);
        fg.setFillCellHeight(height);

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
        new FillGenerator.FillGenJob(cellToFill, fg, FillGenerator.PERIMETER, firstMetal, lastMetal, cells, hierarchy, false, 0.1);
        setVisible(false);;
    }//GEN-LAST:event_okButtonActionPerformed

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
        new FillGen(null, new javax.swing.JFrame(), true).setVisible(true);
    }

    public static void openFillGeneratorDialog(Technology tech)
    {
        new FillGen(tech, new javax.swing.JFrame(), true).setVisible(true);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton adaptiveFill;
    private javax.swing.ButtonGroup autoFillGroup;
    private javax.swing.JRadioButton binaryFill;
    private javax.swing.JButton cancelButton;
    private javax.swing.JRadioButton cellButton;
    private javax.swing.JPanel cellPanel;
    private javax.swing.JRadioButton createButton;
    private javax.swing.JRadioButton flatFill;
    private javax.swing.JPanel floorplanPanel;
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
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.ButtonGroup masterGroup;
    private javax.swing.JPanel masterSelPanel;
    private javax.swing.JPanel metalPanel;
    private javax.swing.JButton okButton;
    private javax.swing.JRadioButton selectButton;
    private javax.swing.JRadioButton templateButton;
    private javax.swing.JPanel templatePanel;
    private javax.swing.JPanel tilingPanel;
    private javax.swing.ButtonGroup topGroup;
    // End of variables declaration//GEN-END:variables
    private java.awt.Color currentColor = java.awt.Color.lightGray;
}
