/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GetInfoText.java
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
 * Class to handle the "Text Get-Info" dialog.
 */
public class GetInfoText extends javax.swing.JDialog
{
	private static GetInfoText theDialog = null;
	private Highlight shownText;

	/**
	 * Routine to show the Text Get-Info dialog.
	 */
	public static void showDialog()
	{
		if (theDialog == null)
		{
			JFrame jf = TopLevel.getCurrentJFrame();
			theDialog = new GetInfoText(jf, false);
		}
		theDialog.show();
	}

	/**
	 * Routine to reload the Text Get-Info dialog from the current highlighting.
	 */
	public static void load()
	{
		if (theDialog == null) return;
		theDialog.loadTextInfo();
	}

	private void loadTextInfo()
	{
		// must have a single text selected
		Highlight textHighlight = null;
		int textCount = 0;
		for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.TEXT) continue;

			// ignore export text
			if (h.getVar() == null && h.getPort() != null) continue;
			textHighlight = h;
			textCount++;
		}
		if (textCount > 1) textHighlight = null;
		if (textHighlight == null)
		{
			if (shownText != null)
			{
				// no text selected, disable the dialog
				theText.setEditable(false);
				xOffset.setEditable(false);
				yOffset.setEditable(false);
				pointsSize.setEditable(false);
				unitsSize.setEditable(false);
				theText.setText("");
				xOffset.setText("");
				yOffset.setText("");
				pointsSize.setText("");
				unitsSize.setText("");
				center.setEnabled(false);
				bottom.setEnabled(false);
				top.setEnabled(false);
				left.setEnabled(false);
				right.setEnabled(false);
				lowerRight.setEnabled(false);
				lowerLeft.setEnabled(false);
				upperRight.setEnabled(false);
				upperLeft.setEnabled(false);
				boxed.setEnabled(false);
				editText.setEnabled(false);
				language.setEnabled(false);
				invisibleOutsideCell.setEnabled(false);
				seeNode.setEnabled(false);
				show.setEnabled(false);
				pointsButton.setEnabled(false);
				unitsButton.setEnabled(false);
				font.setEnabled(false);
				italic.setEnabled(false);
				bold.setEnabled(false);
				underline.setEnabled(false);
				rotation.setEnabled(false);
				units.setEnabled(false);

				shownText = null;
			}
			return;
		}

		// enable it
		String description = "Unknown text";
		String value = "";
		TextDescriptor td = null;
		NodeInst ni = null;
		boolean posNotOffset = false;
		if (textHighlight.getVar() != null)
		{
			td = textHighlight.getVar().getTextDescriptor();
			value = textHighlight.getVar().describe(-1, -1);
			String trueName = textHighlight.getVar().getReadableName();
			if (textHighlight.getGeom() != null)
			{
				if (textHighlight.getPort() != null)
				{
					description = "'" + trueName + "' on export '" + textHighlight.getPort().getProtoName() + "'";
				} else
				{
					description = "'" + trueName + "' on geometry " + textHighlight.getGeom().describe();
					if (textHighlight.getGeom() instanceof NodeInst)
					{
						ni = (NodeInst)textHighlight.getGeom();
						if (ni.getProto() == Generic.tech.invisiblePinNode)
						{
							posNotOffset = true;
							String varName = textHighlight.getVar().getKey().getName();
							if (varName.equals("ART_message"))
							{
								description = "Nonlayout text";
							} else if (varName.equals("VERILOG_code"))
							{
								description = "Verilog Code";
							} else if (varName.equals("VERILOG_declaration"))
							{
								description = "Verilog declaration";
							} else if (varName.equals("SIM_spice_card"))
							{
								description = "SPICE card";
							}
						}
					}
				}
			} else
			{
				description = "'" + trueName + "' on cell " + textHighlight.getCell().describe();
			}
		} else
		{
			if (textHighlight.getGeom() != null)
			{
				description = "Name of cell instance " + textHighlight.getGeom().describe();
				td = ((NodeInst)textHighlight.getGeom()).getProtoTextDescriptor();
				value = ((NodeInst)textHighlight.getGeom()).getProto().describe();
			}
		}
		header.setText(description);
		theText.setEditable(true);
		theText.setText(value);

		xOffset.setEditable(true);
		yOffset.setEditable(true);
		if (posNotOffset)
		{
			xOffset.setText(Double.toString(ni.getCenterX()));
			yOffset.setText(Double.toString(ni.getCenterY()));
		} else
		{
			xOffset.setText(Double.toString(td.getXOff() / 4));
			yOffset.setText(Double.toString(td.getYOff() / 4));
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

		center.setEnabled(true);
		bottom.setEnabled(true);
		top.setEnabled(true);
		left.setEnabled(true);
		right.setEnabled(true);
		lowerRight.setEnabled(true);
		lowerLeft.setEnabled(true);
		upperRight.setEnabled(true);
		upperLeft.setEnabled(true);
		boxed.setEnabled(true);
		if (td.getPos() == TextDescriptor.Position.CENT)      center.setSelected(true); else
		if (td.getPos() == TextDescriptor.Position.UP)        bottom.setSelected(true); else
		if (td.getPos() == TextDescriptor.Position.DOWN)      top.setSelected(true); else
		if (td.getPos() == TextDescriptor.Position.RIGHT)     left.setSelected(true); else
		if (td.getPos() == TextDescriptor.Position.LEFT)      right.setSelected(true); else
		if (td.getPos() == TextDescriptor.Position.UPLEFT)    lowerRight.setSelected(true); else
		if (td.getPos() == TextDescriptor.Position.UPRIGHT)   lowerLeft.setSelected(true); else
		if (td.getPos() == TextDescriptor.Position.DOWNLEFT)  upperRight.setSelected(true); else
		if (td.getPos() == TextDescriptor.Position.DOWNRIGHT) upperLeft.setSelected(true); else
		if (td.getPos() == TextDescriptor.Position.BOXED)     boxed.setSelected(true);

//		editText.setEnabled(true);
//		language.setEnabled(true);
//		seeNode.setEnabled(true);

		invisibleOutsideCell.setEnabled(true);
		invisibleOutsideCell.setSelected(td.isInterior());

		show.setEnabled(true);
		if (td.getDispPart() == TextDescriptor.DispPos.VALUE) show.setSelectedIndex(0); else
		if (td.getDispPart() == TextDescriptor.DispPos.NAMEVALUE) show.setSelectedIndex(1); else
		if (td.getDispPart() == TextDescriptor.DispPos.NAMEVALINH) show.setSelectedIndex(2); else
		if (td.getDispPart() == TextDescriptor.DispPos.NAMEVALINHALL) show.setSelectedIndex(3);

		font.setEnabled(true);

		italic.setEnabled(true);
		italic.setSelected(td.isItalic());
		bold.setEnabled(true);
		bold.setSelected(td.isBold());
		underline.setEnabled(true);
		underline.setSelected(td.isUnderline());

		rotation.setEnabled(true);
		if (td.getRotation() == TextDescriptor.Rotation.ROT0) rotation.setSelectedIndex(0); else
		if (td.getRotation() == TextDescriptor.Rotation.ROT90) rotation.setSelectedIndex(1); else
		if (td.getRotation() == TextDescriptor.Rotation.ROT180) rotation.setSelectedIndex(2); else
		if (td.getRotation() == TextDescriptor.Rotation.ROT270) rotation.setSelectedIndex(3);
		units.setEnabled(true);
		if (td.getUnits() == TextDescriptor.Units.NONE) units.setSelectedIndex(0); else
		if (td.getUnits() == TextDescriptor.Units.RESISTANCE) units.setSelectedIndex(1); else
		if (td.getUnits() == TextDescriptor.Units.CAPACITANCE) units.setSelectedIndex(2); else
		if (td.getUnits() == TextDescriptor.Units.INDUCTANCE) units.setSelectedIndex(3); else
		if (td.getUnits() == TextDescriptor.Units.CURRENT) units.setSelectedIndex(4); else
		if (td.getUnits() == TextDescriptor.Units.VOLTAGE) units.setSelectedIndex(5); else
		if (td.getUnits() == TextDescriptor.Units.DISTANCE) units.setSelectedIndex(6); else
		if (td.getUnits() == TextDescriptor.Units.TIME) units.setSelectedIndex(7);

		shownText = textHighlight;
	}

	/** Creates new form Text Get-Info */
	public GetInfoText(java.awt.Frame parent, boolean modal)
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
		textIconBoxed.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabBoxed.gif")));

		language.addItem("Not Code");
		language.addItem("TCL (not available)");
		language.addItem("LISP (not available)");
		language.addItem("Java");

		show.addItem("Value");
		show.addItem("Name&Value");
		show.addItem("Name,Inherit,Value");
		show.addItem("Name,Inherit-All,Value");

		font.addItem("DEFAULT FONT");

		rotation.addItem("None");
		rotation.addItem("90 degrees counterclockwise");
		rotation.addItem("180 degrees");
		rotation.addItem("90 degrees clockwise");

		units.addItem("None");
		units.addItem("Resistance");
		units.addItem("Capacitance");
		units.addItem("Inductance");
		units.addItem("Current");
		units.addItem("Voltage");
		units.addItem("Distance");
		units.addItem("Time");

		apply.setEnabled(false);
		ok.setEnabled(false);
		
		loadTextInfo();
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
        header = new javax.swing.JLabel();
        theText = new javax.swing.JTextField();
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
        boxed = new javax.swing.JRadioButton();
        textIconCenter = new javax.swing.JLabel();
        textIconBottom = new javax.swing.JLabel();
        textIconTop = new javax.swing.JLabel();
        textIconRight = new javax.swing.JLabel();
        textIconLeft = new javax.swing.JLabel();
        textIconLowerRight = new javax.swing.JLabel();
        textIconLowerLeft = new javax.swing.JLabel();
        textIconUpperRight = new javax.swing.JLabel();
        textIconUpperLeft = new javax.swing.JLabel();
        textIconBoxed = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        show = new javax.swing.JComboBox();
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
        jLabel7 = new javax.swing.JLabel();
        units = new javax.swing.JComboBox();
        editText = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        xOffset = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        yOffset = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        language = new javax.swing.JComboBox();
        invisibleOutsideCell = new javax.swing.JCheckBox();
        seeNode = new javax.swing.JButton();
        apply = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Text Information");
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
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
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
        gridBagConstraints.gridy = 20;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        header.setText("jLabel1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 0);
        getContentPane().add(header, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(theText, gridBagConstraints);

        jLabel2.setText("Text corner:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel2, gridBagConstraints);

        center.setText("Center");
        grab.add(center);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(center, gridBagConstraints);

        bottom.setText("Bottom");
        grab.add(bottom);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(bottom, gridBagConstraints);

        top.setText("Top");
        grab.add(top);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(top, gridBagConstraints);

        right.setText("Right");
        grab.add(right);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(right, gridBagConstraints);

        left.setText("Left");
        grab.add(left);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(left, gridBagConstraints);

        lowerRight.setText("Lower right");
        grab.add(lowerRight);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(lowerRight, gridBagConstraints);

        lowerLeft.setText("Lower left");
        grab.add(lowerLeft);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(lowerLeft, gridBagConstraints);

        upperRight.setText("Upper right");
        grab.add(upperRight);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(upperRight, gridBagConstraints);

        upperLeft.setText("Upper left");
        grab.add(upperLeft);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(upperLeft, gridBagConstraints);

        boxed.setText("Boxed");
        grab.add(boxed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(boxed, gridBagConstraints);

        textIconCenter.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconCenter.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        getContentPane().add(textIconCenter, gridBagConstraints);

        textIconBottom.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconBottom.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        getContentPane().add(textIconBottom, gridBagConstraints);

        textIconTop.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconTop.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        getContentPane().add(textIconTop, gridBagConstraints);

        textIconRight.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconRight.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        getContentPane().add(textIconRight, gridBagConstraints);

        textIconLeft.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconLeft.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        getContentPane().add(textIconLeft, gridBagConstraints);

        textIconLowerRight.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconLowerRight.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        getContentPane().add(textIconLowerRight, gridBagConstraints);

        textIconLowerLeft.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconLowerLeft.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        getContentPane().add(textIconLowerLeft, gridBagConstraints);

        textIconUpperRight.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconUpperRight.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        getContentPane().add(textIconUpperRight, gridBagConstraints);

        textIconUpperLeft.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconUpperLeft.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        getContentPane().add(textIconUpperLeft, gridBagConstraints);

        textIconBoxed.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconBoxed.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        getContentPane().add(textIconBoxed, gridBagConstraints);

        jLabel3.setText("Show:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(show, gridBagConstraints);

        jLabel4.setText("Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel4, gridBagConstraints);

        pointsSize.setColumns(8);
        pointsSize.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        getContentPane().add(pointsSize, gridBagConstraints);

        unitsSize.setColumns(8);
        unitsSize.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        getContentPane().add(unitsSize, gridBagConstraints);

        pointsButton.setText("Points (max 63)");
        sizes.add(pointsButton);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(pointsButton, gridBagConstraints);

        unitsButton.setText("Units (max 127.75)");
        sizes.add(unitsButton);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(unitsButton, gridBagConstraints);

        jLabel5.setText("Font:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(font, gridBagConstraints);

        italic.setText("Italic");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(italic, gridBagConstraints);

        bold.setText("Bold");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(bold, gridBagConstraints);

        underline.setText("Underline");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(underline, gridBagConstraints);

        jLabel6.setText("Rotation:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(rotation, gridBagConstraints);

        jLabel7.setText("Units:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel7, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(units, gridBagConstraints);

        editText.setText("Edit Text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(editText, gridBagConstraints);

        jLabel8.setText("X offset:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jLabel8, gridBagConstraints);

        xOffset.setColumns(8);
        xOffset.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        getContentPane().add(xOffset, gridBagConstraints);

        jLabel9.setText("Y offset:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jLabel9, gridBagConstraints);

        yOffset.setColumns(8);
        yOffset.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        getContentPane().add(yOffset, gridBagConstraints);

        jLabel10.setText("Language:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jLabel10, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(language, gridBagConstraints);

        invisibleOutsideCell.setText("Invisible outside cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(invisibleOutsideCell, gridBagConstraints);

        seeNode.setText("See");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(seeNode, gridBagConstraints);

        apply.setText("Apply");
        apply.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                applyActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 20;
        getContentPane().add(apply, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

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
    private javax.swing.JCheckBox bold;
    private javax.swing.JRadioButton bottom;
    private javax.swing.JRadioButton boxed;
    private javax.swing.JButton cancel;
    private javax.swing.JRadioButton center;
    private javax.swing.JButton editText;
    private javax.swing.JComboBox font;
    private javax.swing.ButtonGroup grab;
    private javax.swing.JLabel header;
    private javax.swing.JCheckBox invisibleOutsideCell;
    private javax.swing.JCheckBox italic;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JComboBox language;
    private javax.swing.JRadioButton left;
    private javax.swing.JRadioButton lowerLeft;
    private javax.swing.JRadioButton lowerRight;
    private javax.swing.JButton ok;
    private javax.swing.JRadioButton pointsButton;
    private javax.swing.JTextField pointsSize;
    private javax.swing.JRadioButton right;
    private javax.swing.JComboBox rotation;
    private javax.swing.JButton seeNode;
    private javax.swing.JComboBox show;
    private javax.swing.ButtonGroup sizes;
    private javax.swing.JLabel textIconBottom;
    private javax.swing.JLabel textIconBoxed;
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
    private javax.swing.JComboBox units;
    private javax.swing.JRadioButton unitsButton;
    private javax.swing.JTextField unitsSize;
    private javax.swing.JRadioButton upperLeft;
    private javax.swing.JRadioButton upperRight;
    private javax.swing.JTextField xOffset;
    private javax.swing.JTextField yOffset;
    // End of variables declaration//GEN-END:variables
	
}
