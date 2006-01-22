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
import com.sun.electric.tool.generator.layout.Tech;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.hierarchy.Cell;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;

/**
 * Unused class to manage fill generators.
 * TODO: RK Decide whether to use this file or discard it.
 */
public class FillGen extends EDialog {

    private JTextField[] vddSpace;
    private JComboBox[] vddUnit;
    private JTextField[] gndSpace;
    private JComboBox[] gndUnit;
//    private Technology tech;
    private JCheckBox[] tiledCells;

    /** Creates new form FillGen */
    public FillGen(Technology tech, java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        int numMetals = (tech == null) ? 6 : tech.getNumMetals();
//        this.tech = tech;
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
//            combox.addActionListener(new java.awt.event.ActionListener() {
//                public void actionPerformed(java.awt.event.ActionEvent evt) {
//                    jComboBox2ActionPerformed(evt);
//                }
//            });
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
		finishInitialization();
   }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        metalPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
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
        jButton1 = new javax.swing.JButton();

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
        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel1.setFont(new java.awt.Font("MS Sans Serif", 1, 14));
        jLabel1.setText("Fill Cell Floorplan");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        jPanel1.add(jLabel1, gridBagConstraints);

        jLabel3.setText("Width (lambda)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        jPanel1.add(jLabel3, gridBagConstraints);

        jLabel5.setText("Height (lambda)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        jPanel1.add(jLabel5, gridBagConstraints);

        jTextField1.setColumns(8);
        jTextField1.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField1.setText("245");
        jTextField1.setMinimumSize(new java.awt.Dimension(100, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        jPanel1.add(jTextField1, gridBagConstraints);

        jTextField2.setColumns(8);
        jTextField2.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField2.setText("128");
        jTextField2.setMinimumSize(new java.awt.Dimension(100, 21));
        jTextField2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField2ActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        jPanel1.add(jTextField2, gridBagConstraints);

        jLabel6.setText("Even layer orientation");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        jPanel1.add(jLabel6, gridBagConstraints);

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "horizontal", "vertical" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        jPanel1.add(jComboBox1, gridBagConstraints);

        jLabel7.setText("Vdd reserved space");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        jPanel1.add(jLabel7, gridBagConstraints);

        jLabel8.setText("gnd reserved space");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        jPanel1.add(jLabel8, gridBagConstraints);

        metalPanel.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel1.add(metalPanel, gridBagConstraints);

        jTabbedPane1.addTab("Floorplan", jPanel1);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jLabel2.setFont(new java.awt.Font("MS Sans Serif", 1, 14));
        jLabel2.setText("Which tiled cells to generate");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        jPanel2.add(jLabel2, gridBagConstraints);

        tiledCells = new JCheckBox[12];
        jCheckBox1.setText("2 x 2");
        tiledCells[0] = jCheckBox1;
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        jPanel2.add(jCheckBox1, gridBagConstraints);

        jCheckBox2.setText("3 x 3");
        tiledCells[1] = jCheckBox2;
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        jPanel2.add(jCheckBox2, gridBagConstraints);

        jCheckBox3.setText("4 x 4");
        tiledCells[2] = jCheckBox3;
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        jPanel2.add(jCheckBox3, gridBagConstraints);

        jCheckBox4.setText("5 x 5");
        tiledCells[3] = jCheckBox4;
        jCheckBox4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox4ActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        jPanel2.add(jCheckBox4, gridBagConstraints);

        jCheckBox5.setText("6 x 6");
        tiledCells[4] = jCheckBox5;
        jCheckBox5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox5ActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        jPanel2.add(jCheckBox5, gridBagConstraints);

        jCheckBox6.setText("7 x 7");
        tiledCells[5] = jCheckBox6;
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        jPanel2.add(jCheckBox6, gridBagConstraints);

        jCheckBox7.setText("8 x 8");
        tiledCells[6] = jCheckBox7;
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        jPanel2.add(jCheckBox7, gridBagConstraints);

        jCheckBox8.setText("9 x 9");
        tiledCells[7] = jCheckBox8;
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        jPanel2.add(jCheckBox8, gridBagConstraints);

        jCheckBox9.setText("10 x 10");
        tiledCells[8] = jCheckBox9;
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        jPanel2.add(jCheckBox9, gridBagConstraints);

        jCheckBox10.setText("11 x 11");
        tiledCells[9] = jCheckBox10;
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        jPanel2.add(jCheckBox10, gridBagConstraints);

        jCheckBox11.setText("12 x 12");
        tiledCells[10] = jCheckBox11;
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        jPanel2.add(jCheckBox11, gridBagConstraints);

        jCheckBox12.setText("13 x 13");
        tiledCells[11] = jCheckBox12;
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        jPanel2.add(jCheckBox12, gridBagConstraints);

        jTabbedPane1.addTab("Tiling", jPanel2);

        jPanel3.setLayout(new java.awt.GridBagLayout());

        jTabbedPane1.addTab("Layers", jPanel3);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(jTabbedPane1, gridBagConstraints);

        jButton1.setText("OK");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        getContentPane().add(jButton1, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        Cell cell = Job.getUserInterface().getCurrentCell();
        Technology tech = cell.getTechnology();

        for (Tech.Type t : Tech.Type.values())
        {

        }

        FillGenerator fg = new FillGenerator(cell.getTechnology());
        fg.setFillLibrary("fillLibGIlda");
        fg.setFillCellWidth(TextUtils.atof(jTextField1.getText()));
        fg.setFillCellHeight(TextUtils.atof(jTextField1.getText()));

        if (jComboBox1.getModel().getSelectedItem().equals("horizontal"))
            fg.makeEvenLayersHorizontal(true);
        FillGenerator.Units LAMBDA = FillGenerator.LAMBDA;
        FillGenerator.Units TRACKS = FillGenerator.TRACKS;
        int firstMetal = -1, lastMetal = -1;

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
            }
        }
        FillGenerator.ExportConfig PERIMETER = FillGenerator.PERIMETER;
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
//        new FillGenerator.FillGenJob(Main.getUserInterface().getCurrentCell(), fg, FillGenerator.PERIMETER, firstMetal, lastMetal, cells);
        setVisible(false);;
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jCheckBox4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox4ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jCheckBox4ActionPerformed

    private void jCheckBox5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox5ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jCheckBox5ActionPerformed

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField2ActionPerformed
    
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
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JButton jButton1;
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
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JPanel metalPanel;
    // End of variables declaration//GEN-END:variables
    private java.awt.Color currentColor = java.awt.Color.lightGray;
}
