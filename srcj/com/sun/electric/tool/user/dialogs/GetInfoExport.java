/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GetInfoExport.java
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

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.TopLevel;

import java.util.List;
import java.util.Iterator;
import javax.swing.JFrame;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.text.Document;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 * Class to handle the "Export Get-Info" dialog.
 */
public class GetInfoExport extends javax.swing.JDialog
{
	private static GetInfoExport theDialog = null;
	private Export shownExport;

	/**
	 * Routine to show the Export Get-Info dialog.
	 */
	public static void showDialog()
	{
		if (theDialog == null)
		{
			JFrame jf = TopLevel.getCurrentJFrame();
			theDialog = new GetInfoExport(jf, false);
		}
		theDialog.show();
	}

	/**
	 * Routine to reload the Export Get-Info dialog from the current highlighting.
	 */
	public static void load()
	{
		if (theDialog == null) return;
		theDialog.loadExportInfo();
	}

	private void loadExportInfo()
	{
		// must have a single export selected
		Export pp = null;
		int exportCount = 0;
		for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.TEXT) continue;
			if (h.getVar() != null) continue;
			if (h.getPort() != null)
			{
				pp = (Export)h.getPort();
				exportCount++;
			}
		}
		if (exportCount > 1) pp = null;
		if (pp == null)
		{
			if (shownExport != null)
			{
				// no export selected, disable the dialog
				theText.setEditable(false);
				theText.setText("");
				center.setEnabled(false);
				bottom.setEnabled(false);
				top.setEnabled(false);
				left.setEnabled(false);
				right.setEnabled(false);
				lowerRight.setEnabled(false);
				lowerLeft.setEnabled(false);
				upperRight.setEnabled(false);
				upperLeft.setEnabled(false);
				characteristics.setEnabled(false);
				refName.setEditable(false);
				refName.setText("");
				pointsSize.setEditable(false);
				unitsSize.setEditable(false);
				pointsSize.setText("");
				unitsSize.setText("");
				pointsButton.setEnabled(false);
				unitsButton.setEnabled(false);
				font.setEnabled(false);
				rotation.setEnabled(false);
				italic.setEnabled(false);
				bold.setEnabled(false);
				underline.setEnabled(false);
				xOffset.setEditable(false);
				yOffset.setEditable(false);
				xOffset.setText("");
				yOffset.setText("");

				shownExport = null;
			}
			return;
		}

		// enable it
		theText.setEditable(true);
		theText.setText(pp.getProtoName());

		TextDescriptor td = pp.getTextDescriptor();
		center.setEnabled(true);
		bottom.setEnabled(true);
		top.setEnabled(true);
		left.setEnabled(true);
		right.setEnabled(true);
		lowerRight.setEnabled(true);
		lowerLeft.setEnabled(true);
		upperRight.setEnabled(true);
		upperLeft.setEnabled(true);
		if (td.getPos() == TextDescriptor.Position.CENT)      center.setSelected(true); else
		if (td.getPos() == TextDescriptor.Position.UP)        bottom.setSelected(true); else
		if (td.getPos() == TextDescriptor.Position.DOWN)      top.setSelected(true); else
		if (td.getPos() == TextDescriptor.Position.RIGHT)     left.setSelected(true); else
		if (td.getPos() == TextDescriptor.Position.LEFT)      right.setSelected(true); else
		if (td.getPos() == TextDescriptor.Position.UPLEFT)    lowerRight.setSelected(true); else
		if (td.getPos() == TextDescriptor.Position.UPRIGHT)   lowerLeft.setSelected(true); else
		if (td.getPos() == TextDescriptor.Position.DOWNLEFT)  upperRight.setSelected(true); else
		if (td.getPos() == TextDescriptor.Position.DOWNRIGHT) upperLeft.setSelected(true);

		characteristics.setEnabled(true);
		PortProto.Characteristic ch = pp.getCharacteristic();
		characteristics.setSelectedItem(ch.getName());
		if (ch == PortProto.Characteristic.REFBASE || ch == PortProto.Characteristic.REFIN ||
			ch == PortProto.Characteristic.REFOUT)
		{
			refName.setText("??");
			refName.setEditable(true);
		} else
		{
			refName.setText("");
			refName.setEditable(false);
		}

		pointsSize.setEditable(true);
		unitsSize.setEditable(true);
		pointsButton.setEnabled(true);
		unitsButton.setEnabled(true);
		TextDescriptor.Size sz = td.getSize();
		if (sz.isAbsolute())
		{
			pointsButton.setSelected(true);
			unitsSize.setText("");
			pointsSize.setText(Double.toString(sz.getSize()));
		} else
		{
			unitsButton.setSelected(true);
			pointsSize.setText("");
			unitsSize.setText(Double.toString(sz.getSize()));
		}

		font.setEnabled(true);

		rotation.setEnabled(true);
		if (td.getRotation() == TextDescriptor.Rotation.ROT0) rotation.setSelectedIndex(0); else
		if (td.getRotation() == TextDescriptor.Rotation.ROT90) rotation.setSelectedIndex(1); else
		if (td.getRotation() == TextDescriptor.Rotation.ROT180) rotation.setSelectedIndex(2); else
		if (td.getRotation() == TextDescriptor.Rotation.ROT270) rotation.setSelectedIndex(3);

		italic.setEnabled(true);
		italic.setSelected(td.isItalic());
		bold.setEnabled(true);
		bold.setSelected(td.isBold());
		underline.setEnabled(true);
		underline.setSelected(td.isUnderline());

		xOffset.setEditable(true);
		yOffset.setEditable(true);
		xOffset.setText(Double.toString(td.getXOff() / 4));
		yOffset.setText(Double.toString(td.getYOff() / 4));

		shownExport = pp;
	}

	/** Creates new form Export Get-Info */
	public GetInfoExport(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		setLocation(100, 50);
		initComponents();

		textIconCenter.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabCenter.gif")));
		textIconLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabLeft.gif")));
		textIconRight.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabRight.gif")));
		textIconTop.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabTop.gif")));
		textIconBottom.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabBottom.gif")));
		textIconLowerRight.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabLowerRight.gif")));
		textIconLowerLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabLowerLeft.gif")));
		textIconUpperRight.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabUpperRight.gif")));
		textIconUpperLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabUpperLeft.gif")));

		font.addItem("DEFAULT FONT");

		rotation.addItem("None");
		rotation.addItem("90 degrees counterclockwise");
		rotation.addItem("180 degrees");
		rotation.addItem("90 degrees clockwise");

		List chars = PortProto.Characteristic.getOrderedCharacteristics();
		for(Iterator it = chars.iterator(); it.hasNext(); )
		{
			PortProto.Characteristic ch = (PortProto.Characteristic)it.next();
			characteristics.addItem(ch.getName());
		}

		apply.setEnabled(false);
		ok.setEnabled(false);
		see.setEnabled(false);
		attributes.setEnabled(false);
		
		loadExportInfo();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        grab = new javax.swing.ButtonGroup();
        sizes = new javax.swing.ButtonGroup();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        apply = new javax.swing.JButton();
        leftSide = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        center = new javax.swing.JRadioButton();
        bottom = new javax.swing.JRadioButton();
        top = new javax.swing.JRadioButton();
        right = new javax.swing.JRadioButton();
        left = new javax.swing.JRadioButton();
        lowerRight = new javax.swing.JRadioButton();
        lowerLeft = new javax.swing.JRadioButton();
        upperRight = new javax.swing.JRadioButton();
        upperLeft = new javax.swing.JRadioButton();
        textIconCenter = new javax.swing.JLabel();
        textIconBottom = new javax.swing.JLabel();
        textIconTop = new javax.swing.JLabel();
        textIconRight = new javax.swing.JLabel();
        textIconLeft = new javax.swing.JLabel();
        textIconLowerRight = new javax.swing.JLabel();
        textIconLowerLeft = new javax.swing.JLabel();
        textIconUpperRight = new javax.swing.JLabel();
        textIconUpperLeft = new javax.swing.JLabel();
        rightSide = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        pointsSize = new javax.swing.JTextField();
        unitsSize = new javax.swing.JTextField();
        pointsButton = new javax.swing.JRadioButton();
        unitsButton = new javax.swing.JRadioButton();
        jLabel5 = new javax.swing.JLabel();
        font = new javax.swing.JComboBox();
        italic = new javax.swing.JCheckBox();
        bold = new javax.swing.JCheckBox();
        underline = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        rotation = new javax.swing.JComboBox();
        jLabel8 = new javax.swing.JLabel();
        xOffset = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        yOffset = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        characteristics = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        refName = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        header = new javax.swing.JLabel();
        theText = new javax.swing.JTextField();
        see = new javax.swing.JButton();
        attributes = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Export Information");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cancelActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 0.25;
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
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 0.25;
        getContentPane().add(ok, gridBagConstraints);

        apply.setText("Apply");
        apply.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                applyActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.25;
        getContentPane().add(apply, gridBagConstraints);

        leftSide.setLayout(new java.awt.GridBagLayout());

        jLabel2.setText("Text corner:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        leftSide.add(jLabel2, gridBagConstraints);

        center.setText("Center");
        grab.add(center);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        leftSide.add(center, gridBagConstraints);

        bottom.setText("Bottom");
        grab.add(bottom);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        leftSide.add(bottom, gridBagConstraints);

        top.setText("Top");
        grab.add(top);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        leftSide.add(top, gridBagConstraints);

        right.setText("Right");
        grab.add(right);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        leftSide.add(right, gridBagConstraints);

        left.setText("Left");
        grab.add(left);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        leftSide.add(left, gridBagConstraints);

        lowerRight.setText("Lower right");
        grab.add(lowerRight);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        leftSide.add(lowerRight, gridBagConstraints);

        lowerLeft.setText("Lower left");
        grab.add(lowerLeft);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        leftSide.add(lowerLeft, gridBagConstraints);

        upperRight.setText("Upper right");
        grab.add(upperRight);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        leftSide.add(upperRight, gridBagConstraints);

        upperLeft.setText("Upper left");
        grab.add(upperLeft);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        leftSide.add(upperLeft, gridBagConstraints);

        textIconCenter.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconCenter.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        leftSide.add(textIconCenter, gridBagConstraints);

        textIconBottom.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconBottom.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        leftSide.add(textIconBottom, gridBagConstraints);

        textIconTop.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconTop.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        leftSide.add(textIconTop, gridBagConstraints);

        textIconRight.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconRight.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        leftSide.add(textIconRight, gridBagConstraints);

        textIconLeft.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconLeft.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        leftSide.add(textIconLeft, gridBagConstraints);

        textIconLowerRight.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconLowerRight.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        leftSide.add(textIconLowerRight, gridBagConstraints);

        textIconLowerLeft.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconLowerLeft.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        leftSide.add(textIconLowerLeft, gridBagConstraints);

        textIconUpperRight.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconUpperRight.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        leftSide.add(textIconUpperRight, gridBagConstraints);

        textIconUpperLeft.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconUpperLeft.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        leftSide.add(textIconUpperLeft, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        getContentPane().add(leftSide, gridBagConstraints);

        rightSide.setLayout(new java.awt.GridBagLayout());

        jLabel4.setText("Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        rightSide.add(jLabel4, gridBagConstraints);

        pointsSize.setColumns(8);
        pointsSize.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        rightSide.add(pointsSize, gridBagConstraints);

        unitsSize.setColumns(8);
        unitsSize.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        rightSide.add(unitsSize, gridBagConstraints);

        pointsButton.setText("Points (max 63)");
        sizes.add(pointsButton);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        rightSide.add(pointsButton, gridBagConstraints);

        unitsButton.setText("Units (max 127.75)");
        sizes.add(unitsButton);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        rightSide.add(unitsButton, gridBagConstraints);

        jLabel5.setText("Font:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        rightSide.add(jLabel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        rightSide.add(font, gridBagConstraints);

        italic.setText("Italic");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        rightSide.add(italic, gridBagConstraints);

        bold.setText("Bold");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        rightSide.add(bold, gridBagConstraints);

        underline.setText("Underline");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        rightSide.add(underline, gridBagConstraints);

        jLabel6.setText("Rotation:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        rightSide.add(jLabel6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        rightSide.add(rotation, gridBagConstraints);

        jLabel8.setText("X offset:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        rightSide.add(jLabel8, gridBagConstraints);

        xOffset.setColumns(8);
        xOffset.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        rightSide.add(xOffset, gridBagConstraints);

        jLabel9.setText("Y offset:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        rightSide.add(jLabel9, gridBagConstraints);

        yOffset.setColumns(8);
        yOffset.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        rightSide.add(yOffset, gridBagConstraints);

        jLabel10.setText("Characteristics:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        rightSide.add(jLabel10, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        rightSide.add(characteristics, gridBagConstraints);

        jLabel1.setText("Reference name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        rightSide.add(jLabel1, gridBagConstraints);

        refName.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        rightSide.add(refName, gridBagConstraints);

        jLabel3.setText("Offset in increments of 0.25");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        rightSide.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(rightSide, gridBagConstraints);

        header.setText("Export name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(header, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(theText, gridBagConstraints);

        see.setText("See Node");
        see.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                seeActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.25;
        getContentPane().add(see, gridBagConstraints);

        attributes.setText("Attributes");
        attributes.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                attributesActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.25;
        getContentPane().add(attributes, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void attributesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_attributesActionPerformed
	{//GEN-HEADEREND:event_attributesActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_attributesActionPerformed

	private void seeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_seeActionPerformed
	{//GEN-HEADEREND:event_seeActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_seeActionPerformed

	private void applyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_applyActionPerformed
	{//GEN-HEADEREND:event_applyActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_applyActionPerformed

	private void okActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_okActionPerformed
	{//GEN-HEADEREND:event_okActionPerformed
		applyActionPerformed(evt);
		closeDialog(null);
	}//GEN-LAST:event_okActionPerformed

	private void cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelActionPerformed
	{//GEN-HEADEREND:event_cancelActionPerformed
		closeDialog(null);
	}//GEN-LAST:event_cancelActionPerformed
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
//		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton apply;
    private javax.swing.JButton attributes;
    private javax.swing.JCheckBox bold;
    private javax.swing.JRadioButton bottom;
    private javax.swing.JButton cancel;
    private javax.swing.JRadioButton center;
    private javax.swing.JComboBox characteristics;
    private javax.swing.JComboBox font;
    private javax.swing.ButtonGroup grab;
    private javax.swing.JLabel header;
    private javax.swing.JCheckBox italic;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JRadioButton left;
    private javax.swing.JPanel leftSide;
    private javax.swing.JRadioButton lowerLeft;
    private javax.swing.JRadioButton lowerRight;
    private javax.swing.JButton ok;
    private javax.swing.JRadioButton pointsButton;
    private javax.swing.JTextField pointsSize;
    private javax.swing.JTextField refName;
    private javax.swing.JRadioButton right;
    private javax.swing.JPanel rightSide;
    private javax.swing.JComboBox rotation;
    private javax.swing.JButton see;
    private javax.swing.ButtonGroup sizes;
    private javax.swing.JLabel textIconBottom;
    private javax.swing.JLabel textIconCenter;
    private javax.swing.JLabel textIconLeft;
    private javax.swing.JLabel textIconLowerLeft;
    private javax.swing.JLabel textIconLowerRight;
    private javax.swing.JLabel textIconRight;
    private javax.swing.JLabel textIconTop;
    private javax.swing.JLabel textIconUpperLeft;
    private javax.swing.JLabel textIconUpperRight;
    private javax.swing.JTextField theText;
    private javax.swing.JRadioButton top;
    private javax.swing.JCheckBox underline;
    private javax.swing.JRadioButton unitsButton;
    private javax.swing.JTextField unitsSize;
    private javax.swing.JRadioButton upperLeft;
    private javax.swing.JRadioButton upperRight;
    private javax.swing.JTextField xOffset;
    private javax.swing.JTextField yOffset;
    // End of variables declaration//GEN-END:variables
	
}
