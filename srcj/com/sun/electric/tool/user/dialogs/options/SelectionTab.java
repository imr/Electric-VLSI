/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SelectionTab.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.StatusBar;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import javax.swing.JOptionPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JSplitPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * Class to handle the "Selection" tab of the Preferences dialog.
 */
public class SelectionTab extends PreferencePanel
{
	/** Creates new form SelectionTab */
	public SelectionTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	public JPanel getPanel() { return selection; }

	public String getName() { return "Selection"; }

	private boolean initialSelectionEasyCellInstances;
	private boolean initialSelectionEasyAnnotationText;
	private boolean initialSelectionDraggingMustEnclose;
    private long cancelMoveDelayMillis;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Selection tab.
	 */
	public void init()
	{
		selEasyCellInstances.setSelected(initialSelectionEasyCellInstances = User.isEasySelectionOfCellInstances());
		selEasyAnnotationText.setSelected(initialSelectionEasyAnnotationText = User.isEasySelectionOfAnnotationText());
		selDraggingEnclosesEntireObject.setSelected(initialSelectionDraggingMustEnclose = User.isDraggingMustEncloseObjects());
        cancelMoveDelayMillis = ClickZoomWireListener.theOne.getCancelMoveDelayMillis();
        selectionCancelMoveDelay.setText(String.valueOf(cancelMoveDelayMillis));
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Selection tab.
	 */
	public void term()
	{
		boolean currentEasyCellInstances = selEasyCellInstances.isSelected();
		if (currentEasyCellInstances != initialSelectionEasyCellInstances)
			User.setEasySelectionOfCellInstances(currentEasyCellInstances);

		boolean currentEasyAnnotationText = selEasyAnnotationText.isSelected();
		if (currentEasyAnnotationText != initialSelectionEasyAnnotationText)
			User.setEasySelectionOfAnnotationText(currentEasyAnnotationText);

		boolean currentDraggingMustEnclose = selDraggingEnclosesEntireObject.isSelected();
		if (currentDraggingMustEnclose != initialSelectionDraggingMustEnclose)
			User.setDraggingMustEncloseObjects(currentDraggingMustEnclose);

        long delay;
        try {
            Long num = Long.valueOf(selectionCancelMoveDelay.getText());
            delay = num.longValue();
        } catch (NumberFormatException e) {
            delay = cancelMoveDelayMillis;
        }
        if (delay != cancelMoveDelayMillis)
            ClickZoomWireListener.theOne.setCancelMoveDelayMillis(delay);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        selection = new javax.swing.JPanel();
        selEasyCellInstances = new javax.swing.JCheckBox();
        selEasyAnnotationText = new javax.swing.JCheckBox();
        selDraggingEnclosesEntireObject = new javax.swing.JCheckBox();
        jLabel55 = new javax.swing.JLabel();
        selectionCancelMoveDelay = new javax.swing.JTextField();
        jLabel58 = new javax.swing.JLabel();

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

        selection.setLayout(new java.awt.GridBagLayout());

        selEasyCellInstances.setText("Easy selection of cell instances");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        selection.add(selEasyCellInstances, gridBagConstraints);

        selEasyAnnotationText.setText("Easy selection of annotation text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        selection.add(selEasyAnnotationText, gridBagConstraints);

        selDraggingEnclosesEntireObject.setText("Dragging must enclose entire object");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        selection.add(selDraggingEnclosesEntireObject, gridBagConstraints);

        jLabel55.setText("Cancel move if move done within:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        selection.add(jLabel55, gridBagConstraints);

        selectionCancelMoveDelay.setColumns(5);
        selectionCancelMoveDelay.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        selectionCancelMoveDelay.setToolTipText("Prevents accidental object movement when double-clicking");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        selection.add(selectionCancelMoveDelay, gridBagConstraints);

        jLabel58.setText("ms");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        selection.add(jLabel58, gridBagConstraints);

        getContentPane().add(selection, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel58;
    private javax.swing.JCheckBox selDraggingEnclosesEntireObject;
    private javax.swing.JCheckBox selEasyAnnotationText;
    private javax.swing.JCheckBox selEasyCellInstances;
    private javax.swing.JPanel selection;
    private javax.swing.JTextField selectionCancelMoveDelay;
    // End of variables declaration//GEN-END:variables
	
}
