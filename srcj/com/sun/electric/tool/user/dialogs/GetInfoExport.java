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

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.Iterator;
import javax.swing.JFrame;


/**
 * Class to handle the "Export Get-Info" dialog.
 */
public class GetInfoExport extends javax.swing.JDialog
{
	private static GetInfoExport theDialog = null;
	private Export shownExport;
	private Highlight exportHighlight;
	private String initialName;
	private String initialRefName;
	private TextDescriptor.Position initialPos;
	private PortProto.Characteristic initialCharacteristic;
	private TextDescriptor.Size initialSize;
	private TextDescriptor.Rotation initialRotation;
	private boolean initialItalic, initialBold, initialUnderline;
	private boolean initialBodyOnly, initialAlwaysDrawn;
	private double initialXOffset, initialYOffset;
	private int initialFont;

	/**
	 * Method to show the Export Get-Info dialog.
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
	 * Method to reload the Export Get-Info dialog from the current highlighting.
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
			ElectricObject eobj = h.getElectricObject();
			if (eobj instanceof Export)
			{
				pp = (Export)eobj;
				exportHighlight = h;
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
				bodyOnly.setEnabled(false);
				alwaysDrawn.setEnabled(false);
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
		initialName = pp.getProtoName();
		theText.setText(initialName);

		TextDescriptor td = pp.getTextDescriptor();
		initialPos = td.getPos();
		center.setEnabled(true);
		bottom.setEnabled(true);
		top.setEnabled(true);
		left.setEnabled(true);
		right.setEnabled(true);
		lowerRight.setEnabled(true);
		lowerLeft.setEnabled(true);
		upperRight.setEnabled(true);
		upperLeft.setEnabled(true);
		if (initialPos == TextDescriptor.Position.CENT)      center.setSelected(true); else
		if (initialPos == TextDescriptor.Position.UP)        bottom.setSelected(true); else
		if (initialPos == TextDescriptor.Position.DOWN)      top.setSelected(true); else
		if (initialPos == TextDescriptor.Position.RIGHT)     left.setSelected(true); else
		if (initialPos == TextDescriptor.Position.LEFT)      right.setSelected(true); else
		if (initialPos == TextDescriptor.Position.UPLEFT)    lowerRight.setSelected(true); else
		if (initialPos == TextDescriptor.Position.UPRIGHT)   lowerLeft.setSelected(true); else
		if (initialPos == TextDescriptor.Position.DOWNLEFT)  upperRight.setSelected(true); else
		if (initialPos == TextDescriptor.Position.DOWNRIGHT) upperLeft.setSelected(true);

		bodyOnly.setEnabled(true);
		initialBodyOnly = pp.isBodyOnly();
		bodyOnly.setSelected(initialBodyOnly);
		alwaysDrawn.setEnabled(true);
		initialAlwaysDrawn = pp.isAlwaysDrawn();
		alwaysDrawn.setSelected(initialAlwaysDrawn);

		characteristics.setEnabled(true);
		initialCharacteristic = pp.getCharacteristic();
		characteristics.setSelectedItem(initialCharacteristic.getName());
		initialRefName = "";
		if (initialCharacteristic == PortProto.Characteristic.REFBASE ||
			initialCharacteristic == PortProto.Characteristic.REFIN ||
			initialCharacteristic == PortProto.Characteristic.REFOUT)
		{
			Variable var = pp.getVar(Export.EXPORT_REFERENCE_NAME);
			if (var != null)
				initialRefName = var.describe(-1, -1);
			refName.setEditable(true);
		} else
		{
			refName.setEditable(false);
		}
		refName.setText(initialRefName);

		pointsSize.setEditable(true);
		unitsSize.setEditable(true);
		pointsButton.setEnabled(true);
		unitsButton.setEnabled(true);
		initialSize = td.getSize();
		EditWindow wnd = EditWindow.getCurrent();
		if (initialSize.isAbsolute())
		{
			pointsButton.setSelected(true);
			pointsSize.setText(Double.toString(initialSize.getSize()));
			unitsSize.setText("");
			if (wnd != null)
			{
				double unitSize = wnd.getTextUnitSize((int)initialSize.getSize());
				unitsSize.setText(Double.toString(unitSize));
			}
		} else
		{
			unitsButton.setSelected(true);
			unitsSize.setText(Double.toString(initialSize.getSize()));
			pointsSize.setText("");
			if (wnd != null)
			{
				int pointSize = wnd.getTextPointSize(initialSize.getSize());
				pointsSize.setText(Integer.toString(pointSize));
			}
		}

		font.setEnabled(true);
		initialFont = td.getFace();
		if (initialFont == 0) font.setSelectedIndex(0); else
		{
			TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(initialFont);
			if (af != null)
				font.setSelectedItem(af.getName());
		}

		rotation.setEnabled(true);
		initialRotation = td.getRotation();
		if (initialRotation == TextDescriptor.Rotation.ROT0) rotation.setSelectedIndex(0); else
		if (initialRotation == TextDescriptor.Rotation.ROT90) rotation.setSelectedIndex(1); else
		if (initialRotation == TextDescriptor.Rotation.ROT180) rotation.setSelectedIndex(2); else
		if (initialRotation == TextDescriptor.Rotation.ROT270) rotation.setSelectedIndex(3);

		italic.setEnabled(true);
		initialItalic = td.isItalic();
		italic.setSelected(initialItalic);
		initialBold = td.isBold();
		bold.setEnabled(true);
		bold.setSelected(initialBold);
		initialUnderline = td.isUnderline();
		underline.setEnabled(true);
		underline.setSelected(initialUnderline);

		xOffset.setEditable(true);
		yOffset.setEditable(true);
		initialXOffset = td.getXOff();
		initialYOffset = td.getYOff();
		xOffset.setText(Double.toString(initialXOffset));
		yOffset.setText(Double.toString(initialYOffset));

		shownExport = pp;
	}

	/** Creates new form Export Get-Info */
	private GetInfoExport(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		setLocation(100, 50);
		initComponents();
        getRootPane().setDefaultButton(ok);

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
		Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		for(int i=0; i<fonts.length; i++)
			font.addItem(fonts[i].getFontName());

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

		loadExportInfo();
	}

	protected static class ChangeExport extends Job
	{
		Export pp;
		GetInfoExport dialog;

		protected ChangeExport(Export pp, GetInfoExport dialog)
		{
			super("Modify Export", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.pp = pp;
			this.dialog = dialog;
			startJob();
		}

		public void doIt()
		{
			boolean changed = false;
			TextDescriptor td = pp.getTextDescriptor();

			String currentName = dialog.theText.getText();
			if (!currentName.equals(dialog.initialName))
			{
				// change the name
				changed = true;
				pp.setProtoName(currentName);
				dialog.initialName = currentName;
			}

			TextDescriptor.Position currentPos = TextDescriptor.Position.CENT;
			if (dialog.bottom.isSelected()) currentPos = TextDescriptor.Position.UP; else
			if (dialog.top.isSelected()) currentPos = TextDescriptor.Position.DOWN; else
			if (dialog.left.isSelected()) currentPos = TextDescriptor.Position.RIGHT; else
			if (dialog.right.isSelected()) currentPos = TextDescriptor.Position.LEFT; else
			if (dialog.lowerRight.isSelected()) currentPos = TextDescriptor.Position.UPLEFT; else
			if (dialog.lowerLeft.isSelected()) currentPos = TextDescriptor.Position.UPRIGHT; else
			if (dialog.upperRight.isSelected()) currentPos = TextDescriptor.Position.DOWNLEFT; else
			if (dialog.upperLeft.isSelected()) currentPos = TextDescriptor.Position.DOWNRIGHT;
			if (currentPos != dialog.initialPos)
			{
				// change the position
				changed = true;
				td.setPos(currentPos);
//				dialog.exportHighlight.setTextStyle(currentPos.getPolyType());
				dialog.initialPos = currentPos;
			}

			boolean currentBodyOnly = dialog.bodyOnly.isSelected();
			if (currentBodyOnly != dialog.initialBodyOnly)
			{
				// change the body-only
				changed = true;
				if (currentBodyOnly) pp.setBodyOnly(); else
					pp.clearBodyOnly();
				dialog.initialBodyOnly = currentBodyOnly;
			}
			boolean currentAlwaysDrawn = dialog.alwaysDrawn.isSelected();
			if (currentAlwaysDrawn != dialog.initialAlwaysDrawn)
			{
				// change the body-only
				changed = true;
				if (currentAlwaysDrawn) pp.setAlwaysDrawn(); else
					pp.clearAlwaysDrawn();
				dialog.initialAlwaysDrawn = currentAlwaysDrawn;
			}

			String charName = (String)dialog.characteristics.getSelectedItem();
			PortProto.Characteristic currentCharacteristic = PortProto.Characteristic.findCharacteristic(charName);
			if (currentCharacteristic != dialog.initialCharacteristic)
			{
				// change the characteristic
				pp.setCharacteristic(currentCharacteristic);
				changed = true;
				dialog.initialCharacteristic = currentCharacteristic;
			}

			String currentRefName = dialog.refName.getText();
			if (!currentRefName.equals(dialog.initialRefName))
			{
				// change the reference name
				changed = true;
				if (currentCharacteristic.isReference())
					pp.newVar(Export.EXPORT_REFERENCE_NAME, currentRefName);
				dialog.initialRefName = currentRefName;
			}

			TextDescriptor.Size currentSize = null;
			if (dialog.pointsButton.isSelected())
			{
				int newSize = TextUtils.atoi(dialog.pointsSize.getText());
				currentSize = TextDescriptor.Size.newAbsSize(newSize);
			} else
			{
				double newSize = TextUtils.atof(dialog.unitsSize.getText());
				currentSize = TextDescriptor.Size.newRelSize(newSize);
			}
			if (!currentSize.equals(dialog.initialSize))
			{
				// change the size
				changed = true;
				if (currentSize.isAbsolute())
					td.setAbsSize((int)currentSize.getSize()); else
						td.setRelSize(currentSize.getSize());
				dialog.initialSize = currentSize;
			}

			TextDescriptor.Rotation currentRotation = null;
			int rotIndex = dialog.rotation.getSelectedIndex();
			switch (rotIndex)
			{
				case 1:  currentRotation = TextDescriptor.Rotation.ROT90;    break;
				case 2:  currentRotation = TextDescriptor.Rotation.ROT180;   break;
				case 3:  currentRotation = TextDescriptor.Rotation.ROT270;   break;
				default: currentRotation = TextDescriptor.Rotation.ROT0;     break;
			}
			if (currentRotation != dialog.initialRotation)
			{
				// change the rotation
				changed = true;
				td.setRotation(currentRotation);
				dialog.initialRotation = currentRotation;
			}

			// handle changes to the font
			int currentFont = dialog.font.getSelectedIndex();
			if (currentFont != dialog.initialFont)
			{
				changed = true;
				if (currentFont == 0) td.setFace(0); else
				{
					String fontName = (String)dialog.font.getSelectedItem();
					TextDescriptor.ActiveFont newFont = TextDescriptor.ActiveFont.findActiveFont(fontName);
					int newFontIndex = newFont.getIndex();
					td.setFace(newFontIndex);
				}
				dialog.initialFont = currentFont;
			}

			boolean currentItalic = dialog.italic.isSelected();
			if (currentItalic != dialog.initialItalic)
			{
				// change the italic
				changed = true;
				if (currentItalic) td.setItalic(); else
					td.clearItalic();
				dialog.initialItalic = currentItalic;
			}

			boolean currentBold = dialog.bold.isSelected();
			if (currentBold != dialog.initialBold)
			{
				// change the bold
				changed = true;
				if (currentBold) td.setBold(); else
					td.clearBold();
				dialog.initialBold = currentBold;
			}

			boolean currentUnderline = dialog.underline.isSelected();
			if (currentUnderline != dialog.initialUnderline)
			{
				// change the underline
				changed = true;
				if (currentUnderline) td.setUnderline(); else
					td.clearUnderline();
				dialog.initialUnderline = currentUnderline;
			}

			double currentXOffset = TextUtils.atof(dialog.xOffset.getText());
			double currentYOffset = TextUtils.atof(dialog.yOffset.getText());
			if (!EMath.doublesEqual(currentXOffset, dialog.initialXOffset) ||
				!EMath.doublesEqual(currentYOffset, dialog.initialYOffset))
			{
				// change the offset
				changed = true;
				td.setOff(currentXOffset, currentYOffset);
				dialog.initialXOffset = currentXOffset;
				dialog.initialYOffset = currentYOffset;
			}

			if (changed)
				Undo.redrawObject(pp.getOriginalPort().getNodeInst());
//				pp.getOriginalPort().getNodeInst().modifyInstance(0, 0, 0, 0, 0);
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

        grab = new javax.swing.ButtonGroup();
        sizes = new javax.swing.ButtonGroup();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        apply = new javax.swing.JButton();
        leftSide = new javax.swing.JPanel();
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
        jPanel1 = new javax.swing.JPanel();
        textCorner = new javax.swing.JPanel();
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
        bodyOnly = new javax.swing.JCheckBox();
        alwaysDrawn = new javax.swing.JCheckBox();

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
        gridBagConstraints.weightx = 0.25;
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
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.25;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
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

        jLabel4.setText("Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        leftSide.add(jLabel4, gridBagConstraints);

        pointsSize.setColumns(8);
        pointsSize.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        leftSide.add(pointsSize, gridBagConstraints);

        unitsSize.setColumns(8);
        unitsSize.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        leftSide.add(unitsSize, gridBagConstraints);

        pointsButton.setText("Points (max 63)");
        sizes.add(pointsButton);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        leftSide.add(pointsButton, gridBagConstraints);

        unitsButton.setText("Units (max 127.75)");
        sizes.add(unitsButton);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        leftSide.add(unitsButton, gridBagConstraints);

        jLabel5.setText("Font:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        leftSide.add(jLabel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        leftSide.add(font, gridBagConstraints);

        italic.setText("Italic");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        leftSide.add(italic, gridBagConstraints);

        bold.setText("Bold");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        leftSide.add(bold, gridBagConstraints);

        underline.setText("Underline");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        leftSide.add(underline, gridBagConstraints);

        jLabel6.setText("Rotation:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        leftSide.add(jLabel6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        leftSide.add(rotation, gridBagConstraints);

        jLabel8.setText("X offset:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        leftSide.add(jLabel8, gridBagConstraints);

        xOffset.setColumns(8);
        xOffset.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        leftSide.add(xOffset, gridBagConstraints);

        jLabel9.setText("Y offset:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        leftSide.add(jLabel9, gridBagConstraints);

        yOffset.setColumns(8);
        yOffset.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        leftSide.add(yOffset, gridBagConstraints);

        jLabel10.setText("Characteristics:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        leftSide.add(jLabel10, gridBagConstraints);

        characteristics.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                characteristicsActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        leftSide.add(characteristics, gridBagConstraints);

        jLabel1.setText("Reference name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        leftSide.add(jLabel1, gridBagConstraints);

        refName.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        leftSide.add(refName, gridBagConstraints);

        jLabel3.setText("Offset in increments of 0.25");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        leftSide.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(leftSide, gridBagConstraints);

        header.setText("Export name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(header, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
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

        jPanel1.setLayout(new java.awt.GridBagLayout());

        textCorner.setLayout(new java.awt.GridBagLayout());

        textCorner.setBorder(new javax.swing.border.TitledBorder("Text Corner"));
        center.setText("Center");
        grab.add(center);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        textCorner.add(center, gridBagConstraints);

        bottom.setText("Bottom");
        grab.add(bottom);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        textCorner.add(bottom, gridBagConstraints);

        top.setText("Top");
        grab.add(top);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        textCorner.add(top, gridBagConstraints);

        right.setText("Right");
        grab.add(right);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        textCorner.add(right, gridBagConstraints);

        left.setText("Left");
        grab.add(left);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        textCorner.add(left, gridBagConstraints);

        lowerRight.setText("Lower right");
        grab.add(lowerRight);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        textCorner.add(lowerRight, gridBagConstraints);

        lowerLeft.setText("Lower left");
        grab.add(lowerLeft);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        textCorner.add(lowerLeft, gridBagConstraints);

        upperRight.setText("Upper right");
        grab.add(upperRight);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        textCorner.add(upperRight, gridBagConstraints);

        upperLeft.setText("Upper left");
        grab.add(upperLeft);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 0);
        textCorner.add(upperLeft, gridBagConstraints);

        textIconCenter.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconCenter.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        textCorner.add(textIconCenter, gridBagConstraints);

        textIconBottom.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconBottom.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        textCorner.add(textIconBottom, gridBagConstraints);

        textIconTop.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconTop.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        textCorner.add(textIconTop, gridBagConstraints);

        textIconRight.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconRight.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        textCorner.add(textIconRight, gridBagConstraints);

        textIconLeft.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconLeft.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        textCorner.add(textIconLeft, gridBagConstraints);

        textIconLowerRight.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconLowerRight.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        textCorner.add(textIconLowerRight, gridBagConstraints);

        textIconLowerLeft.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconLowerLeft.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        textCorner.add(textIconLowerLeft, gridBagConstraints);

        textIconUpperRight.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconUpperRight.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        textCorner.add(textIconUpperRight, gridBagConstraints);

        textIconUpperLeft.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconUpperLeft.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        textCorner.add(textIconUpperLeft, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        jPanel1.add(textCorner, gridBagConstraints);

        bodyOnly.setText("Body only");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(bodyOnly, gridBagConstraints);

        alwaysDrawn.setText("Always drawn");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(alwaysDrawn, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        getContentPane().add(jPanel1, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void characteristicsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_characteristicsActionPerformed
	{//GEN-HEADEREND:event_characteristicsActionPerformed
		String stringNow = (String)characteristics.getSelectedItem();
		PortProto.Characteristic ch = PortProto.Characteristic.findCharacteristic(stringNow);
		refName.setEditable(ch.isReference());
	}//GEN-LAST:event_characteristicsActionPerformed

	private void attributesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_attributesActionPerformed
	{//GEN-HEADEREND:event_attributesActionPerformed
		Attributes.showDialog();
	}//GEN-LAST:event_attributesActionPerformed

	private void seeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_seeActionPerformed
	{//GEN-HEADEREND:event_seeActionPerformed
		if (shownExport == null) return;
		NodeInst ni = shownExport.getOriginalPort().getNodeInst();
		Cell cell = exportHighlight.getCell();

		Highlight.clear();
		Highlight.addElectricObject(ni, cell);
		Highlight newHigh = Highlight.addText(shownExport, cell, null, null);
		Highlight.finished();
	}//GEN-LAST:event_seeActionPerformed

	private void applyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_applyActionPerformed
	{//GEN-HEADEREND:event_applyActionPerformed
		if (shownExport == null) return;
		ChangeExport job = new ChangeExport(shownExport, this);
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
		theDialog = null;
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox alwaysDrawn;
    private javax.swing.JButton apply;
    private javax.swing.JButton attributes;
    private javax.swing.JCheckBox bodyOnly;
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
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JRadioButton left;
    private javax.swing.JPanel leftSide;
    private javax.swing.JRadioButton lowerLeft;
    private javax.swing.JRadioButton lowerRight;
    private javax.swing.JButton ok;
    private javax.swing.JRadioButton pointsButton;
    private javax.swing.JTextField pointsSize;
    private javax.swing.JTextField refName;
    private javax.swing.JRadioButton right;
    private javax.swing.JComboBox rotation;
    private javax.swing.JButton see;
    private javax.swing.ButtonGroup sizes;
    private javax.swing.JPanel textCorner;
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
