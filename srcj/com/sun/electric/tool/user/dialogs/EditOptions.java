/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EditOptions.java
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

import com.sun.electric.database.prototype.PortProto;

import java.util.Iterator;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.text.Document;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 * Class to handle the "Edit Options" dialog.
 */
public class EditOptions extends javax.swing.JDialog
{
	/** Creates new form EditOptions */
	public EditOptions(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		setLocation(100, 50);
		initComponents();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        jTabbedPane1 = new javax.swing.JTabbedPane();
        newNode = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        primitive = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        primitiveXSize = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        primitiveYSize = new javax.swing.JTextField();
        overrideDefaultOrientation = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        primitiveRotation = new javax.swing.JTextField();
        primitiveMirror = new javax.swing.JCheckBox();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        disallowModificationLockedPrims = new javax.swing.JCheckBox();
        moveAfterDuplicate = new javax.swing.JCheckBox();
        copyExports = new javax.swing.JCheckBox();
        instanceRotation = new javax.swing.JTextField();
        instanceMirror = new javax.swing.JCheckBox();
        newArc = new javax.swing.JPanel();
        selection = new javax.swing.JPanel();
        selEasyCellInstances = new javax.swing.JCheckBox();
        selEasyAnnotationText = new javax.swing.JCheckBox();
        selCenterBasedPrimitives = new javax.swing.JCheckBox();
        selDraggingEnclosesEntireObject = new javax.swing.JCheckBox();
        cell = new javax.swing.JPanel();
        port = new javax.swing.JPanel();
        frame = new javax.swing.JPanel();
        icon = new javax.swing.JPanel();
        grid = new javax.swing.JPanel();
        alignment = new javax.swing.JPanel();
        layers = new javax.swing.JPanel();
        colors = new javax.swing.JPanel();
        text = new javax.swing.JPanel();
        threeD = new javax.swing.JPanel();
        technology = new javax.swing.JPanel();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Edit Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        newNode.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("For primitive:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        newNode.add(jLabel1, gridBagConstraints);

        primitive.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                primitiveActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        newNode.add(primitive, gridBagConstraints);

        jLabel2.setText("X size of new primitives:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        newNode.add(jLabel2, gridBagConstraints);

        primitiveXSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        newNode.add(primitiveXSize, gridBagConstraints);

        jLabel3.setText("Y size of new primitives:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        newNode.add(jLabel3, gridBagConstraints);

        primitiveYSize.setColumns(8);
        primitiveYSize.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                primitiveYSizeActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        newNode.add(primitiveYSize, gridBagConstraints);

        overrideDefaultOrientation.setText("Override default orientation");
        overrideDefaultOrientation.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                overrideDefaultOrientationActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        newNode.add(overrideDefaultOrientation, gridBagConstraints);

        jLabel4.setText("Rotation of new nodes:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        newNode.add(jLabel4, gridBagConstraints);

        primitiveRotation.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        newNode.add(primitiveRotation, gridBagConstraints);

        primitiveMirror.setText("Mirror X");
        primitiveMirror.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                primitiveMirrorActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        newNode.add(primitiveMirror, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        newNode.add(jSeparator1, gridBagConstraints);

        jLabel5.setText("For all nodes:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        newNode.add(jLabel5, gridBagConstraints);

        jLabel6.setText("Rotation of new nodes:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        newNode.add(jLabel6, gridBagConstraints);

        disallowModificationLockedPrims.setText("Disallow modification of locked primitives");
        disallowModificationLockedPrims.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                disallowModificationLockedPrimsActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        newNode.add(disallowModificationLockedPrims, gridBagConstraints);

        moveAfterDuplicate.setText("Move after Duplicate");
        moveAfterDuplicate.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                moveAfterDuplicateActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        newNode.add(moveAfterDuplicate, gridBagConstraints);

        copyExports.setText("Duplicate/Array/Extract copies exports");
        copyExports.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                copyExportsActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        newNode.add(copyExports, gridBagConstraints);

        instanceRotation.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        newNode.add(instanceRotation, gridBagConstraints);

        instanceMirror.setText("Mirror X");
        instanceMirror.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                instanceMirrorActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        newNode.add(instanceMirror, gridBagConstraints);

        jTabbedPane1.addTab("New Nodes", newNode);

        newArc.setLayout(new java.awt.GridBagLayout());

        jTabbedPane1.addTab("New Arcs", newArc);

        selection.setLayout(new java.awt.GridBagLayout());

        selEasyCellInstances.setText("Easy selection of cell instances");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        selection.add(selEasyCellInstances, gridBagConstraints);

        selEasyAnnotationText.setText("Easy selection of annotation text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        selection.add(selEasyAnnotationText, gridBagConstraints);

        selCenterBasedPrimitives.setText("Center-based primitives");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        selection.add(selCenterBasedPrimitives, gridBagConstraints);

        selDraggingEnclosesEntireObject.setText("Dragging must enclose entire object");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        selection.add(selDraggingEnclosesEntireObject, gridBagConstraints);

        jTabbedPane1.addTab("Selection", selection);

        cell.setLayout(new java.awt.GridBagLayout());

        jTabbedPane1.addTab("Cells", cell);

        port.setLayout(new java.awt.GridBagLayout());

        jTabbedPane1.addTab("Ports/Exports", port);

        frame.setLayout(new java.awt.GridBagLayout());

        jTabbedPane1.addTab("Frame", frame);

        icon.setLayout(new java.awt.GridBagLayout());

        jTabbedPane1.addTab("Icon", icon);

        grid.setLayout(new java.awt.GridBagLayout());

        jTabbedPane1.addTab("Grid", grid);

        alignment.setLayout(new java.awt.GridBagLayout());

        jTabbedPane1.addTab("Alignment", alignment);

        layers.setLayout(new java.awt.GridBagLayout());

        jTabbedPane1.addTab("Layers", layers);

        colors.setLayout(new java.awt.GridBagLayout());

        jTabbedPane1.addTab("Colors", colors);

        text.setLayout(new java.awt.GridBagLayout());

        jTabbedPane1.addTab("Text", text);

        threeD.setLayout(new java.awt.GridBagLayout());

        jTabbedPane1.addTab("3D", threeD);

        technology.setLayout(new java.awt.GridBagLayout());

        jTabbedPane1.addTab("Technology", technology);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(jTabbedPane1, gridBagConstraints);

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cancelActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 40, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                okActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 40);
        getContentPane().add(ok, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void primitiveActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_primitiveActionPerformed
	{//GEN-HEADEREND:event_primitiveActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_primitiveActionPerformed

	private void instanceMirrorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_instanceMirrorActionPerformed
	{//GEN-HEADEREND:event_instanceMirrorActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_instanceMirrorActionPerformed

	private void primitiveMirrorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_primitiveMirrorActionPerformed
	{//GEN-HEADEREND:event_primitiveMirrorActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_primitiveMirrorActionPerformed

	private void copyExportsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_copyExportsActionPerformed
	{//GEN-HEADEREND:event_copyExportsActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_copyExportsActionPerformed

	private void moveAfterDuplicateActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_moveAfterDuplicateActionPerformed
	{//GEN-HEADEREND:event_moveAfterDuplicateActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_moveAfterDuplicateActionPerformed

	private void disallowModificationLockedPrimsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_disallowModificationLockedPrimsActionPerformed
	{//GEN-HEADEREND:event_disallowModificationLockedPrimsActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_disallowModificationLockedPrimsActionPerformed

	private void overrideDefaultOrientationActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_overrideDefaultOrientationActionPerformed
	{//GEN-HEADEREND:event_overrideDefaultOrientationActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_overrideDefaultOrientationActionPerformed

	private void okActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_okActionPerformed
	{//GEN-HEADEREND:event_okActionPerformed
		closeDialog(null);
	}//GEN-LAST:event_okActionPerformed

	private void cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelActionPerformed
	{//GEN-HEADEREND:event_cancelActionPerformed
		closeDialog(null);
	}//GEN-LAST:event_cancelActionPerformed

	private void primitiveYSizeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_primitiveYSizeActionPerformed
	{//GEN-HEADEREND:event_primitiveYSizeActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_primitiveYSizeActionPerformed
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel alignment;
    private javax.swing.JButton cancel;
    private javax.swing.JPanel cell;
    private javax.swing.JPanel colors;
    private javax.swing.JCheckBox copyExports;
    private javax.swing.JCheckBox disallowModificationLockedPrims;
    private javax.swing.JPanel frame;
    private javax.swing.JPanel grid;
    private javax.swing.JPanel icon;
    private javax.swing.JCheckBox instanceMirror;
    private javax.swing.JTextField instanceRotation;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JPanel layers;
    private javax.swing.JCheckBox moveAfterDuplicate;
    private javax.swing.JPanel newArc;
    private javax.swing.JPanel newNode;
    private javax.swing.JButton ok;
    private javax.swing.JCheckBox overrideDefaultOrientation;
    private javax.swing.JPanel port;
    private javax.swing.JComboBox primitive;
    private javax.swing.JCheckBox primitiveMirror;
    private javax.swing.JTextField primitiveRotation;
    private javax.swing.JTextField primitiveXSize;
    private javax.swing.JTextField primitiveYSize;
    private javax.swing.JCheckBox selCenterBasedPrimitives;
    private javax.swing.JCheckBox selDraggingEnclosesEntireObject;
    private javax.swing.JCheckBox selEasyAnnotationText;
    private javax.swing.JCheckBox selEasyCellInstances;
    private javax.swing.JPanel selection;
    private javax.swing.JPanel technology;
    private javax.swing.JPanel text;
    private javax.swing.JPanel threeD;
    // End of variables declaration//GEN-END:variables
	
}
