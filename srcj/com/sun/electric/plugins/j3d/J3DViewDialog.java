/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NewExport.java
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
package com.sun.electric.plugins.j3d;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.Job;

import java.util.List;
import java.util.ArrayList;

/**
 * Class to handle the "3D View Demo Dialog" dialog.
 * @author  Gilda Garreton
 * @version 0.1
 */
public class J3DViewDialog extends EDialog
{
    private View3DWindow view3D = null;
    private Job socketJob = null;
    private String hostname;
    private List knots = new ArrayList();

    public static void createThreeViewDialog(java.awt.Frame parent, String hostname)
    {
        View3DWindow view3D = null;
        WindowContent content = WindowFrame.getCurrentWindowFrame().getContent();
        if (content instanceof View3DWindow)
            view3D = (View3DWindow)content;
        else
        {
            System.out.println("Current Window Frame is not a 3D View");
            return;
        }
        J3DViewDialog dialog = new J3DViewDialog(parent, view3D, true, hostname);
		dialog.setVisible(true);
    }

	/** Creates new form ThreeView */
	public J3DViewDialog(java.awt.Frame parent, View3DWindow view3d, boolean modal, String hostname)
	{
		super(parent, modal);
		initComponents();
        this.view3D = view3d;
        this.hostname = hostname;
        getRootPane().setDefaultButton(start);
//        spline.addItem("KB Spline");
//        spline.addItem("TCB Spline");
        if (view3d.jAlpha != null)
        {
            slider.addChangeListener(view3d.jAlpha);
            auto.setSelected(view3d.jAlpha.getAutoMode());
        }

        // Motion button
        javax.swing.JButton motion = new javax.swing.JButton();
        motion.setText("Demo");
        motion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createDemoActionPerformed(evt);
            }
        });

        java.awt.GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 40, 4, 4);
        getContentPane().add(motion, gridBagConstraints);

        // Motion button
        javax.swing.JButton enter = new javax.swing.JButton();
        enter.setText("Enter");
        enter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enterDataActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 40, 4, 4);
        getContentPane().add(enter, gridBagConstraints);

		finishInitialization();
	}

    public void socketAction(String inData)
    {
        double[] values = new double[9];
        J3DClientApp.parseValues(inData, 0, values);

        xField.setText(Double.toString(values[0]));
        yField.setText(Double.toString(values[1]));
        zField.setText(Double.toString(values[2]));
        xRotField.setText(Double.toString(J3DClientApp.covertToDegrees(values[3])));
        yRotField.setText(Double.toString(J3DClientApp.covertToDegrees(values[4])));
        zRotField.setText(Double.toString(J3DClientApp.covertToDegrees(values[5])));
        xRotPosField.setText(Double.toString(values[6]));
        yRotPosField.setText(Double.toString(values[7]));
        zRotPosField.setText(Double.toString(values[8]));
        knots.add(view3D.moveAndRotate(values));
    }

	protected void escapePressed() { cancelActionPerformed(null); }

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        cancel = new javax.swing.JButton();
        start = new javax.swing.JButton();
        slider = new javax.swing.JSlider();
        auto = new javax.swing.JCheckBox();
        separator = new javax.swing.JSeparator();
        xLabel = new javax.swing.JLabel();
        xField = new javax.swing.JTextField();
        yLabel = new javax.swing.JLabel();
        yField = new javax.swing.JTextField();
        zLabel = new javax.swing.JLabel();
        zField = new javax.swing.JTextField();
        xRotLabel = new javax.swing.JLabel();
        xRotField = new javax.swing.JTextField();
        yRotLabel = new javax.swing.JLabel();
        yRotField = new javax.swing.JTextField();
        zRotLabel = new javax.swing.JLabel();
        zRotField = new javax.swing.JTextField();
        xRotPosLabel = new javax.swing.JLabel();
        xRotPosField = new javax.swing.JTextField();
        yRotPosLabel = new javax.swing.JLabel();
        yRotPosField = new javax.swing.JTextField();
        zRotPosLabel = new javax.swing.JLabel();
        zRotPosField = new javax.swing.JTextField();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("3D Demo Control Dialog");
        setBackground(java.awt.Color.white);
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 40, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

        start.setText("Connect");
        start.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 40);
        getContentPane().add(start, gridBagConstraints);

        slider.setVerifyInputWhenFocusTarget(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(slider, gridBagConstraints);

        auto.setSelected(true);
        auto.setText("Auto");
        auto.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                autoStateChanged(evt);
            }
        });

        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        getContentPane().add(auto, new java.awt.GridBagConstraints());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(separator, gridBagConstraints);

        xLabel.setText("X:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        getContentPane().add(xLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(xField, gridBagConstraints);

        yLabel.setText("Y:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        getContentPane().add(yLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(yField, gridBagConstraints);

        zLabel.setText("Z:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        getContentPane().add(zLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(zField, gridBagConstraints);

        xRotLabel.setText("Rot X:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        getContentPane().add(xRotLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(xRotField, gridBagConstraints);

        yRotLabel.setText("Rot Y:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        getContentPane().add(yRotLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(yRotField, gridBagConstraints);

        zRotLabel.setText("Rot Z:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        getContentPane().add(zRotLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(zRotField, gridBagConstraints);

        xRotPosLabel.setText("Rot Pos X:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        getContentPane().add(xRotPosLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(xRotPosField, gridBagConstraints);

        yRotPosLabel.setText("Rot Pos Y:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        getContentPane().add(yRotPosLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(yRotPosField, gridBagConstraints);

        zRotPosLabel.setText("Rot Pos Z:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        getContentPane().add(zRotPosLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        getContentPane().add(zRotPosField, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

    private void autoStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_autoStateChanged
        view3D.jAlpha.setAutoMode(auto.isSelected());
    }//GEN-LAST:event_autoStateChanged

    private void createDemoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startActionPerformed
        view3D.addInterpolator(knots);
        //view3D.set3DCamera(spline.getSelectedIndex());
    }//GEN-LAST:event_startActionPerformed

    //enterDataActionPerformed
    private void enterDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startActionPerformed
        double[] values = new double[9];

        values[0] = TextUtils.atof(xField.getText());
        values[1] = TextUtils.atof(yField.getText());
        values[2] = TextUtils.atof(zField.getText());
        values[3] = TextUtils.atof(xRotField.getText());
        values[4] = TextUtils.atof(yRotField.getText());
        values[5] = TextUtils.atof(zRotField.getText());
        values[6] = TextUtils.atof(xRotPosField.getText());
        values[7] = TextUtils.atof(yRotPosField.getText());
        values[8] = TextUtils.atof(zRotPosField.getText());
        knots.add(view3D.moveAndRotate(values));
    }//GEN-LAST:event_startActionPerformed

    private void startActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startActionPerformed
        if (start.getText().equals("Connect"))
        {
            start.setText("Stop");
            socketJob = new J3DClientApp(this, hostname);
            socketJob.startJob();
        }
        else
        {
            start.setText("Connect");
            if (socketJob != null)
            {
                socketJob.abort();
                socketJob.checkAbort();
                socketJob.remove();
            }
        }
        //view3D.set3DCamera(spline.getSelectedIndex());
    }//GEN-LAST:event_startActionPerformed

	private void cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelActionPerformed
	{//GEN-HEADEREND:event_cancelActionPerformed
        if (socketJob != null)
        {
            socketJob.abort();
            socketJob.checkAbort();
            socketJob.remove();
        }
		closeDialog(null);
	}//GEN-LAST:event_cancelActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox auto;
    private javax.swing.JButton cancel;
    private javax.swing.JSeparator separator;
    private javax.swing.JSlider slider;
    private javax.swing.JButton start;
    private javax.swing.JTextField xField;
    private javax.swing.JLabel xLabel;
    private javax.swing.JTextField xRotField;
    private javax.swing.JLabel xRotLabel;
    private javax.swing.JTextField xRotPosField;
    private javax.swing.JLabel xRotPosLabel;
    private javax.swing.JTextField yField;
    private javax.swing.JLabel yLabel;
    private javax.swing.JTextField yRotField;
    private javax.swing.JLabel yRotLabel;
    private javax.swing.JTextField yRotPosField;
    private javax.swing.JLabel yRotPosLabel;
    private javax.swing.JTextField zField;
    private javax.swing.JLabel zLabel;
    private javax.swing.JTextField zRotField;
    private javax.swing.JLabel zRotLabel;
    private javax.swing.JTextField zRotPosField;
    private javax.swing.JLabel zRotPosLabel;
    // End of variables declaration//GEN-END:variables

}
