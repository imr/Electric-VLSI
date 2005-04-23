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
package com.sun.electric.plugins.j3d.ui;

import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.plugins.j3d.View3DWindow;
import com.sun.electric.plugins.j3d.utils.J3DUtils;
import com.sun.electric.plugins.j3d.utils.J3DSerialization;

import javax.media.j3d.Transform3D;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

/**
 * Class to handle the "3D View Demo Dialog" dialog.
 * @author  Gilda Garreton
 * @version 0.1
 */
public class J3DDemoDialog extends EDialog
{
    private View3DWindow view3D = null;
    private List knots = new ArrayList();
    private Map interMap;

    public static void create3DDemoDialog(java.awt.Frame parent)
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
        J3DDemoDialog dialog = new J3DDemoDialog(parent, view3D, false);
		dialog.setVisible(true);
    }

	/** Creates new form ThreeView */
	public J3DDemoDialog(java.awt.Frame parent, View3DWindow view3d, boolean modal)
	{
		super(parent, modal);
		initComponents();
        this.view3D = view3d;
        getRootPane().setDefaultButton(enter);
        if (J3DUtils.jAlpha != null)
        {
            slider.addChangeListener(J3DUtils.jAlpha);
            auto.setSelected(J3DUtils.jAlpha.getAutoMode());
        }
        demoMode.addItem("Viewplatform");
        demoMode.addItem("Scene");
        // to calculate window position
		finishInitialization();
	}
	protected void escapePressed() { closeActionPerformed(null); }

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        slider = new javax.swing.JSlider();
        auto = new javax.swing.JCheckBox();
        close = new javax.swing.JButton();
        demo = new javax.swing.JButton();
        enter = new javax.swing.JButton();
        read = new javax.swing.JButton();
        save = new javax.swing.JButton();
        movie = new javax.swing.JButton();
        demoMode = new javax.swing.JComboBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("3D Demo Control Dialog");
        setBackground(java.awt.Color.white);
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(slider, gridBagConstraints);

        auto.setText("Auto");
        auto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(auto, gridBagConstraints);

        close.setText("Close");
        close.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(close, gridBagConstraints);

        demo.setText("Start Demo");
        demo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                demoActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(demo, gridBagConstraints);

        enter.setText("Enter Frame");
        enter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enterActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(enter, gridBagConstraints);

        read.setText("Read Demo");
        read.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                readActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(read, gridBagConstraints);

        save.setText("Save Demo");
        save.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(save, gridBagConstraints);

        movie.setText("Create Movie");
        movie.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                movieActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(movie, gridBagConstraints);

        demoMode.setToolTipText("");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(demoMode, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

    private void movieActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_movieActionPerformed
        String fileName = OpenFile.chooseOutputFile(FileType.MOV, "Save 3D Movie", "demo.mov");
        view3D.saveMovie(fileName);
    }//GEN-LAST:event_movieActionPerformed


    private void saveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveActionPerformed
        String fileName = OpenFile.chooseOutputFile(FileType.J3D, "Save 3D Demo File", "demo.j3d");
        if (fileName == null || knots == null) return;

        try
        {
            Transform3D tmpTrans = new Transform3D();
            FileOutputStream outputStream = new FileOutputStream(fileName);
            ObjectOutputStream out = new ObjectOutputStream(outputStream);
            view3D.getObjTransform(tmpTrans);
            boolean useViewplatform = ((String)demoMode.getSelectedItem()).equals("Viewplatform");
            J3DSerialization serial = new J3DSerialization(new Boolean(useViewplatform), knots, tmpTrans);
            out.writeObject(serial);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }//GEN-LAST:event_saveActionPerformed

    private void readActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_readActionPerformed
        String fileName = OpenFile.chooseInputFile(FileType.J3D, "Read 3D Demo Frames");

        if (fileName == null) return;

        knots = null;
        try
        {
            FileInputStream inputStream = new FileInputStream(fileName);
            ObjectInputStream in = new ObjectInputStream(inputStream);
            J3DSerialization serial = (J3DSerialization)in.readObject();
            boolean useViewplatform = false;
            if (serial.useView != null) // old file is false;
                useViewplatform = serial.useView.booleanValue();
            demoMode.setSelectedIndex(useViewplatform ? 0 : 1);
            knots = serial.list;
            Transform3D tmpTrans = new Transform3D();
            tmpTrans.set(serial.matrix);
            view3D.setObjTransform(tmpTrans);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        };
    }//GEN-LAST:event_readActionPerformed

    private void enterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enterActionPerformed
        knots.add(view3D.addFrame(demoMode.getSelectedIndex() == 0));
        view3D.saveImage(true);

    }//GEN-LAST:event_enterActionPerformed

    private void demoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_demoActionPerformed
        if (demo.getText().equals("Start Demo")) {
            interMap = view3D.addInterpolatorPerGroup(knots, null, interMap, demoMode.getSelectedIndex() == 0);
            if (interMap != null) // no error
                demo.setText("Stop Demo");
        } else {
            demo.setText("Start Demo");
            view3D.removeInterpolator(interMap);
            interMap.clear();
        }
    }//GEN-LAST:event_demoActionPerformed

    private void autoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoActionPerformed
        J3DUtils.jAlpha.setAutoMode(auto.isSelected());
    }//GEN-LAST:event_autoActionPerformed

    private void closeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeActionPerformed
        if (interMap != null)
            view3D.removeInterpolator(interMap);
        setVisible(false);
        dispose();
    }//GEN-LAST:event_closeActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox auto;
    private javax.swing.JButton close;
    private javax.swing.JButton demo;
    private javax.swing.JComboBox demoMode;
    private javax.swing.JButton enter;
    private javax.swing.JButton movie;
    private javax.swing.JButton read;
    private javax.swing.JButton save;
    private javax.swing.JSlider slider;
    // End of variables declaration//GEN-END:variables

}
