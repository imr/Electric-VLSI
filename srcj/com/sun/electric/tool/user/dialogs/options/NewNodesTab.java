/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NewNodesTab.java
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
 * Class to handle the "New Nodes" tab of the Preferences dialog.
 */
public class NewNodesTab extends PreferencePanel
{
	private Technology curTech = Technology.getCurrent();

	/** Creates new form NewNodesTab */
	public NewNodesTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	public JPanel getPanel() { return newNode; }

	public String getName() { return "New Nodes"; }

	private static class PrimNodeInfo
	{
		double initialWid, wid;
		double initialHei, hei;
//		boolean initialOverride, override;
//		int initialRotation, rotation;
//		boolean initialMirrorX, mirrorX;
		Variable var;
	}
	private HashMap initialNewNodesPrimInfo;
	private boolean initialNewNodesCheckDatesDuringCreation;
	private boolean initialNewNodesAutoTechnologySwitch;
	private boolean initialNewNodesPlaceCellCenter;
	private boolean initialNewNodesDisallowModificationLockedPrims;
	private boolean initialNewNodesMoveAfterDuplicate;
	private boolean initialNewNodesDupCopiesExports;
	private boolean initialNewNodesExtractCopiesExports;
	private boolean newNodesDataChanging = false;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the New Nodes tab.
	 */
	public void init()
	{
		// gather information about the PrimitiveNodes in the current Technology
		initialNewNodesPrimInfo = new HashMap();
		for(Iterator it = curTech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			PrimNodeInfo pni = new PrimNodeInfo();
			SizeOffset so = np.getSizeOffset();
			pni.initialWid = pni.wid = np.getDefWidth() - so.getLowXOffset() - so.getHighXOffset();
			pni.initialHei = pni.hei = np.getDefHeight() - so.getLowYOffset() - so.getHighYOffset();
			initialNewNodesPrimInfo.put(np, pni);
			nodePrimitive.addItem(np.getName());
		}
		newNodesPrimPopupChanged();

		// set checkboxes for "Cells" area
		nodeCheckCellDates.setSelected(initialNewNodesCheckDatesDuringCreation = User.isCheckCellDates());
		nodeSwitchTechnology.setSelected(initialNewNodesAutoTechnologySwitch = User.isAutoTechnologySwitch());
		nodePlaceCellCenter.setSelected(initialNewNodesPlaceCellCenter = User.isPlaceCellCenter());

		// set checkboxes for "all nodes" area
		nodeDisallowModificationLockedPrims.setSelected(initialNewNodesDisallowModificationLockedPrims = User.isDisallowModificationLockedPrims());
		nodeMoveAfterDuplicate.setSelected(initialNewNodesMoveAfterDuplicate = User.isMoveAfterDuplicate());
		nodeDupArrayCopyExports.setSelected(initialNewNodesDupCopiesExports = User.isDupCopiesExports());
		nodeExtractCopyExports.setSelected(initialNewNodesExtractCopiesExports = User.isExtractCopiesExports());
		
		// setup listeners to react to any changes to a primitive size
		nodePrimitive.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { newNodesPrimPopupChanged(); }
		});
		nodePrimitiveXSize.getDocument().addDocumentListener(new NewNodeDocumentListener(this));
		nodePrimitiveYSize.getDocument().addDocumentListener(new NewNodeDocumentListener(this));
	}

	/**
	 * Method called when the primitive node popup is changed.
	 */
	private void newNodesPrimPopupChanged()
	{
		String primName = (String)nodePrimitive.getSelectedItem();
		PrimitiveNode np = curTech.findNodeProto(primName);
		PrimNodeInfo pni = (PrimNodeInfo)initialNewNodesPrimInfo.get(np);
		if (pni == null) return;
		newNodesDataChanging = true;
		nodePrimitiveXSize.setText(Double.toString(pni.wid));
		nodePrimitiveYSize.setText(Double.toString(pni.hei));
		newNodesDataChanging = false;
	}

	/**
	 * Class to handle special changes to per-primitive node options.
	 */
	private static class NewNodeDocumentListener implements DocumentListener
	{
		NewNodesTab dialog;

		NewNodeDocumentListener(NewNodesTab dialog) { this.dialog = dialog; }

		public void changedUpdate(DocumentEvent e) { dialog.newNodesPrimDataChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.newNodesPrimDataChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.newNodesPrimDataChanged(); }
	}

	/**
	 * Method called when any of the primitive data (in the top part) changes.
	 * Caches all values for the selected primitive node.
	 */
	private void newNodesPrimDataChanged()
	{
		if (newNodesDataChanging) return;
		String primName = (String)nodePrimitive.getSelectedItem();
		PrimitiveNode np = curTech.findNodeProto(primName);
		PrimNodeInfo pni = (PrimNodeInfo)initialNewNodesPrimInfo.get(np);
		if (pni == null) return;
		pni.wid = TextUtils.atof(nodePrimitiveXSize.getText());
		pni.hei = TextUtils.atof(nodePrimitiveYSize.getText());
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the New Nodes tab.
	 */
	public void term()
	{
		for(Iterator it = curTech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			PrimNodeInfo pni = (PrimNodeInfo)initialNewNodesPrimInfo.get(np);
			if (pni.wid != pni.initialWid || pni.hei != pni.initialHei)
			{
				SizeOffset so = np.getSizeOffset();
				pni.wid += so.getLowXOffset() + so.getHighXOffset();
				pni.hei += so.getLowYOffset() + so.getHighYOffset();
				np.setDefSize(pni.wid, pni.hei);
			}
		}

		boolean currentCheckCellDates = nodeCheckCellDates.isSelected();
		if (currentCheckCellDates != initialNewNodesCheckDatesDuringCreation)
			User.setCheckCellDates(currentCheckCellDates);

		boolean currentSwitchTechnology = nodeSwitchTechnology.isSelected();
		if (currentSwitchTechnology != initialNewNodesAutoTechnologySwitch)
			User.setAutoTechnologySwitch(currentSwitchTechnology);

		boolean currentPlaceCellCenters = nodePlaceCellCenter.isSelected();
		if (currentPlaceCellCenters != initialNewNodesPlaceCellCenter)
			User.setPlaceCellCenter(currentPlaceCellCenters);

		boolean currentDisallowModificationLockedPrims = nodeDisallowModificationLockedPrims.isSelected();
		if (currentDisallowModificationLockedPrims != initialNewNodesDisallowModificationLockedPrims)
			User.setDisallowModificationLockedPrims(currentDisallowModificationLockedPrims);

		boolean currentMoveAfterDuplicate = nodeMoveAfterDuplicate.isSelected();
		if (currentMoveAfterDuplicate != initialNewNodesMoveAfterDuplicate)
			User.setMoveAfterDuplicate(currentMoveAfterDuplicate);

		boolean currentCopyExports = nodeDupArrayCopyExports.isSelected();
		if (currentCopyExports != initialNewNodesDupCopiesExports)
			User.setDupCopiesExports(currentCopyExports);

		boolean currentExtractCopyExports = nodeExtractCopyExports.isSelected();
		if (currentExtractCopyExports != initialNewNodesExtractCopiesExports)
			User.setExtractCopiesExports(currentExtractCopyExports);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        newNode = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        nodePrimitive = new javax.swing.JComboBox();
        nodePrimitiveXSize = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        nodePrimitiveYSize = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        nodeCheckCellDates = new javax.swing.JCheckBox();
        nodeSwitchTechnology = new javax.swing.JCheckBox();
        nodePlaceCellCenter = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        nodeDisallowModificationLockedPrims = new javax.swing.JCheckBox();
        nodeMoveAfterDuplicate = new javax.swing.JCheckBox();
        nodeDupArrayCopyExports = new javax.swing.JCheckBox();
        nodeExtractCopyExports = new javax.swing.JCheckBox();

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

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jPanel2.setBorder(new javax.swing.border.TitledBorder("For Primitive Nodes"));
        jLabel1.setText("Primitive:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(nodePrimitive, gridBagConstraints);

        nodePrimitiveXSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(nodePrimitiveXSize, gridBagConstraints);

        jLabel2.setText("Default X size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(jLabel2, gridBagConstraints);

        jLabel3.setText("Default Y size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(jLabel3, gridBagConstraints);

        nodePrimitiveYSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(nodePrimitiveYSize, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        newNode.add(jPanel2, gridBagConstraints);

        jPanel3.setLayout(new java.awt.GridBagLayout());

        jPanel3.setBorder(new javax.swing.border.TitledBorder("For Cells"));
        nodeCheckCellDates.setText("Check cell dates during editing");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        jPanel3.add(nodeCheckCellDates, gridBagConstraints);

        nodeSwitchTechnology.setText("Switch technology to match current cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel3.add(nodeSwitchTechnology, gridBagConstraints);

        nodePlaceCellCenter.setText("Place Cell-Center in new cells");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel3.add(nodePlaceCellCenter, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        newNode.add(jPanel3, gridBagConstraints);

        jPanel4.setLayout(new java.awt.GridBagLayout());

        jPanel4.setBorder(new javax.swing.border.TitledBorder("For All Nodes"));
        nodeDisallowModificationLockedPrims.setText("Disallow modification of locked primitives");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        jPanel4.add(nodeDisallowModificationLockedPrims, gridBagConstraints);

        nodeMoveAfterDuplicate.setText("Move after Duplicate");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel4.add(nodeMoveAfterDuplicate, gridBagConstraints);

        nodeDupArrayCopyExports.setText("Duplicate/Array/Paste copies exports");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel4.add(nodeDupArrayCopyExports, gridBagConstraints);

        nodeExtractCopyExports.setText("Extract copies exports");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        jPanel4.add(nodeExtractCopyExports, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        newNode.add(jPanel4, gridBagConstraints);

        getContentPane().add(newNode, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel newNode;
    private javax.swing.JCheckBox nodeCheckCellDates;
    private javax.swing.JCheckBox nodeDisallowModificationLockedPrims;
    private javax.swing.JCheckBox nodeDupArrayCopyExports;
    private javax.swing.JCheckBox nodeExtractCopyExports;
    private javax.swing.JCheckBox nodeMoveAfterDuplicate;
    private javax.swing.JCheckBox nodePlaceCellCenter;
    private javax.swing.JComboBox nodePrimitive;
    private javax.swing.JTextField nodePrimitiveXSize;
    private javax.swing.JTextField nodePrimitiveYSize;
    private javax.swing.JCheckBox nodeSwitchTechnology;
    // End of variables declaration//GEN-END:variables
	
}
