/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PortsAndExportsTab.java
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
 * Class to handle the "Ports/Exports" tab of the Preferences dialog.
 */
public class PortsAndExportsTab extends PreferencePanel
{
	/** Creates new form PortsAndExportsTab */
	public PortsAndExportsTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	public JPanel getPanel() { return port; }

	public String getName() { return "Ports/Exports"; }

	private int initialPortDisplayPortLevel;
	private int initialPortDisplayExportLevel;
	private boolean initialPortMoveNodeWithExport;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Ports tab.
	 */
	public void init()
	{
		switch (initialPortDisplayPortLevel = User.getPortDisplayLevel())
		{
			case 0: portFullPort.setSelected(true);    break;
			case 1: portShortPort.setSelected(true);   break;
			case 2: portCrossPort.setSelected(true);   break;
		}

		switch (initialPortDisplayExportLevel = User.getExportDisplayLevel())
		{
			case 0: portFullExport.setSelected(true);    break;
			case 1: portShortExport.setSelected(true);   break;
			case 2: portCrossExport.setSelected(true);   break;
		}

		initialPortMoveNodeWithExport = User.isMoveNodeWithExport();
		portMoveNode.setSelected(initialPortMoveNodeWithExport);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Ports tab.
	 */
	public void term()
	{
		int currentDisplayPortLevel = 0;
		if (portShortPort.isSelected()) currentDisplayPortLevel = 1; else
			if (portCrossPort.isSelected()) currentDisplayPortLevel = 2;
		if (currentDisplayPortLevel != initialPortDisplayPortLevel)
			User.setPortDisplayLevels(currentDisplayPortLevel);

		int currentDisplayExportLevel = 0;
		if (portShortExport.isSelected()) currentDisplayExportLevel = 1; else
			if (portCrossExport.isSelected()) currentDisplayExportLevel = 2;
		if (currentDisplayExportLevel != initialPortDisplayExportLevel)
			User.setExportDisplayLevels(currentDisplayExportLevel);

		boolean currentMoveNodeWithExport = portMoveNode.isSelected();
		if (currentMoveNodeWithExport != initialPortMoveNodeWithExport)
			User.setMoveNodeWithExport(currentMoveNodeWithExport);

		// redisplay everything if port options changed
		if (currentDisplayPortLevel != initialPortDisplayPortLevel ||
			currentDisplayExportLevel != initialPortDisplayExportLevel)
				EditWindow.repaintAllContents();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        portGroup = new javax.swing.ButtonGroup();
        exportGroup = new javax.swing.ButtonGroup();
        port = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        portFullPort = new javax.swing.JRadioButton();
        portFullExport = new javax.swing.JRadioButton();
        portShortPort = new javax.swing.JRadioButton();
        portShortExport = new javax.swing.JRadioButton();
        portCrossPort = new javax.swing.JRadioButton();
        portCrossExport = new javax.swing.JRadioButton();
        jSeparator2 = new javax.swing.JSeparator();
        portMoveNode = new javax.swing.JCheckBox();
        jSeparator9 = new javax.swing.JSeparator();

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

        port.setLayout(new java.awt.GridBagLayout());

        jLabel11.setText("Ports (in instances):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(jLabel11, gridBagConstraints);

        jLabel12.setText("Exports (in cells):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(jLabel12, gridBagConstraints);

        portFullPort.setText("Full Names");
        portGroup.add(portFullPort);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portFullPort, gridBagConstraints);

        portFullExport.setText("Full Names");
        exportGroup.add(portFullExport);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portFullExport, gridBagConstraints);

        portShortPort.setText("Short Names");
        portGroup.add(portShortPort);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portShortPort, gridBagConstraints);

        portShortExport.setText("Short Names");
        exportGroup.add(portShortExport);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portShortExport, gridBagConstraints);

        portCrossPort.setText("Crosses");
        portGroup.add(portCrossPort);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portCrossPort, gridBagConstraints);

        portCrossExport.setText("Crosses");
        exportGroup.add(portCrossExport);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portCrossExport, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        port.add(jSeparator2, gridBagConstraints);

        portMoveNode.setText("Move node with export name");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portMoveNode, gridBagConstraints);

        jSeparator9.setOrientation(javax.swing.SwingConstants.VERTICAL);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        port.add(jSeparator9, gridBagConstraints);

        getContentPane().add(port, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup exportGroup;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator9;
    private javax.swing.JPanel port;
    private javax.swing.JRadioButton portCrossExport;
    private javax.swing.JRadioButton portCrossPort;
    private javax.swing.JRadioButton portFullExport;
    private javax.swing.JRadioButton portFullPort;
    private javax.swing.ButtonGroup portGroup;
    private javax.swing.JCheckBox portMoveNode;
    private javax.swing.JRadioButton portShortExport;
    private javax.swing.JRadioButton portShortPort;
    // End of variables declaration//GEN-END:variables
	
}
