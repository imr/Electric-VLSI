/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NewArcsTab.java
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
 * Class to handle the "New Arcs" tab of the Preferences dialog.
 */
public class NewArcsTab extends PreferencePanel
{
	private Technology curTech = Technology.getCurrent();

	/** Creates new form NewArcsTab */
	public NewArcsTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	public JPanel getPanel() { return newArc; }

	public String getName() { return "New Arcs"; }

	private static class PrimArcInfo
	{
		boolean initialRigid, rigid;
		boolean initialFixedAngle, fixedAngle;
		boolean initialSlidable, slidable;
		boolean initialDirectional, directional;
		boolean initialEndsExtend, endsExtend;
		double initialWid, wid;
		int initialAngleIncrement, angleIncrement;
		PrimitiveNode initialPin, pin;
	}
	private HashMap initialNewArcsPrimInfo;
	private boolean newArcsDataChanging = false;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the New Arcs tab.
	 */
	public void init()
	{
		// setup popup of possible pins
		for(Iterator it = curTech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			arcPin.addItem(np.getName());
		}

		// gather information about the PrimitiveArcs in the current Technology
		initialNewArcsPrimInfo = new HashMap();
		for(Iterator it = curTech.getArcs(); it.hasNext(); )
		{
			PrimitiveArc ap = (PrimitiveArc)it.next();
			PrimArcInfo pai = new PrimArcInfo();

			pai.initialRigid = pai.rigid = ap.isRigid();
			pai.initialFixedAngle = pai.fixedAngle = ap.isFixedAngle();
			pai.initialSlidable = pai.slidable = ap.isSlidable();
			pai.initialDirectional = pai.directional = ap.isDirectional();
			pai.initialEndsExtend = pai.endsExtend = ap.isExtended();

			pai.initialWid = pai.wid = ap.getDefaultWidth();
			pai.initialAngleIncrement = pai.angleIncrement = ap.getAngleIncrement();
			pai.initialPin = pai.pin = ap.findOverridablePinProto();

			initialNewArcsPrimInfo.put(ap, pai);
			arcProtoList.addItem(ap.getName());
		}
		newArcsPrimPopupChanged();

		// setup listeners to react to a change of the selected arc
		arcProtoList.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { newArcsPrimPopupChanged(); }
		});

		// setup listeners to react to any changes to the arc values
        arcRigid.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { newArcsPrimDataChanged(); }
        });
        arcFixedAngle.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { newArcsPrimDataChanged(); }
        });
        arcSlidable.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { newArcsPrimDataChanged(); }
        });
        arcDirectional.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { newArcsPrimDataChanged(); }
        });
        arcEndsExtend.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { newArcsPrimDataChanged(); }
        });
		arcWidth.getDocument().addDocumentListener(new NewArcDocumentListener(this));
		arcAngle.getDocument().addDocumentListener(new NewArcDocumentListener(this));
        arcPin.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { newArcsPrimDataChanged(); }
        });
	}

	/**
	 * Method called when the primitive arc popup is changed.
	 */
	private void newArcsPrimPopupChanged()
	{
		String primName = (String)arcProtoList.getSelectedItem();
		PrimitiveArc ap = curTech.findArcProto(primName);
		PrimArcInfo pai = (PrimArcInfo)initialNewArcsPrimInfo.get(ap);
		if (pai == null) return;

		newArcsDataChanging = true;
		arcRigid.setSelected(pai.rigid);
		arcFixedAngle.setSelected(pai.fixedAngle);
		arcSlidable.setSelected(pai.slidable);
		arcDirectional.setSelected(pai.directional);
		arcEndsExtend.setSelected(pai.endsExtend);

		arcWidth.setText(Double.toString(pai.wid));
		arcAngle.setText(Integer.toString(pai.angleIncrement));
		arcPin.setSelectedItem(pai.pin.getName());
		newArcsDataChanging = false;
	}

	/**
	 * Class to handle special changes to per-primitive arc options.
	 */
	private static class NewArcDocumentListener implements DocumentListener
	{
		NewArcsTab dialog;

		NewArcDocumentListener(NewArcsTab dialog) { this.dialog = dialog; }

		public void changedUpdate(DocumentEvent e) { dialog.newArcsPrimDataChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.newArcsPrimDataChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.newArcsPrimDataChanged(); }
	}

	/**
	 * Method called when any of the primitive data changes.
	 * Caches all values for the selected primitive arc.
	 */
	private void newArcsPrimDataChanged()
	{
		if (newArcsDataChanging) return;
		String primName = (String)arcProtoList.getSelectedItem();
		PrimitiveArc ap = curTech.findArcProto(primName);
		PrimArcInfo pai = (PrimArcInfo)initialNewArcsPrimInfo.get(ap);
		if (pai == null) return;

		pai.rigid = arcRigid.isSelected();
		pai.fixedAngle = arcFixedAngle.isSelected();
		pai.slidable = arcSlidable.isSelected();
		pai.directional = arcDirectional.isSelected();
		pai.endsExtend = arcEndsExtend.isSelected();

		pai.wid = TextUtils.atof(arcWidth.getText());
		pai.angleIncrement = TextUtils.atoi(arcAngle.getText());
		pai.pin = curTech.findNodeProto((String)arcPin.getSelectedItem());
		PortProto pp = (PortProto)pai.pin.getPorts().next();
		if (!pp.connectsTo(ap))
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"Cannot use " + pai.pin.getName() + " as a pin because it does not connect to " + ap.getName() + " arcs");
			pai.pin = pai.initialPin;
			arcPin.setSelectedItem(pai.pin.getName());
		}
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the New Arcs tab.
	 */
	public void term()
	{
		for(Iterator it = curTech.getArcs(); it.hasNext(); )
		{
			PrimitiveArc ap = (PrimitiveArc)it.next();
			PrimArcInfo pai = (PrimArcInfo)initialNewArcsPrimInfo.get(ap);
			if (pai.rigid != pai.initialRigid)
				ap.setRigid(pai.rigid);
			if (pai.fixedAngle != pai.initialFixedAngle)
				ap.setFixedAngle(pai.fixedAngle);
			if (pai.slidable != pai.initialSlidable)
				ap.setSlidable(pai.slidable);
			if (pai.directional != pai.initialDirectional)
				ap.setDirectional(pai.directional);
			if (pai.endsExtend != pai.initialEndsExtend)
				ap.setExtended(pai.endsExtend);
			if (pai.wid != pai.initialWid)
				ap.setDefaultWidth(pai.wid);
			if (pai.angleIncrement != pai.initialAngleIncrement)
				ap.setAngleIncrement(pai.angleIncrement);
			if (pai.pin != pai.initialPin)
			{
				ap.setPinProto(pai.pin);
			}
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        newArc = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        arcRigid = new javax.swing.JCheckBox();
        arcFixedAngle = new javax.swing.JCheckBox();
        arcDirectional = new javax.swing.JCheckBox();
        arcSlidable = new javax.swing.JCheckBox();
        arcEndsExtend = new javax.swing.JCheckBox();
        jPanel8 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        arcWidth = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        arcAngle = new javax.swing.JTextField();
        arcPin = new javax.swing.JComboBox();
        arcProtoList = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();

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

        newArc.setLayout(new java.awt.GridBagLayout());

        jPanel7.setLayout(new java.awt.GridBagLayout());

        jPanel7.setBorder(new javax.swing.border.TitledBorder("Default Constraints"));
        arcRigid.setText("Rigid");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel7.add(arcRigid, gridBagConstraints);

        arcFixedAngle.setText("Fixed-angle");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel7.add(arcFixedAngle, gridBagConstraints);

        arcDirectional.setText("Directional");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel7.add(arcDirectional, gridBagConstraints);

        arcSlidable.setText("Slidable");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel7.add(arcSlidable, gridBagConstraints);

        arcEndsExtend.setText("Ends extended");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel7.add(arcEndsExtend, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        newArc.add(jPanel7, gridBagConstraints);

        jPanel8.setLayout(new java.awt.GridBagLayout());

        jPanel8.setBorder(new javax.swing.border.TitledBorder("Other Information"));
        jLabel7.setText("Default Width:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        jPanel8.add(jLabel7, gridBagConstraints);

        jLabel9.setText("Pin:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        jPanel8.add(jLabel9, gridBagConstraints);

        arcWidth.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(arcWidth, gridBagConstraints);

        jLabel8.setText("Placement angle:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        jPanel8.add(jLabel8, gridBagConstraints);

        arcAngle.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(arcAngle, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel8.add(arcPin, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        newArc.add(jPanel8, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        newArc.add(arcProtoList, gridBagConstraints);

        jLabel5.setText("For Arc:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        newArc.add(jLabel5, gridBagConstraints);

        getContentPane().add(newArc, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField arcAngle;
    private javax.swing.JCheckBox arcDirectional;
    private javax.swing.JCheckBox arcEndsExtend;
    private javax.swing.JCheckBox arcFixedAngle;
    private javax.swing.JComboBox arcPin;
    private javax.swing.JComboBox arcProtoList;
    private javax.swing.JCheckBox arcRigid;
    private javax.swing.JCheckBox arcSlidable;
    private javax.swing.JTextField arcWidth;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel newArc;
    // End of variables declaration//GEN-END:variables
	
}
