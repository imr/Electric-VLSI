/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TextInfoPanel.java
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

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Iterator;


/**
 * A Panel to display and edit Text Display options for a Variable.
 * Does not display attribute specific options such as Code or Show Style.
 */
public class TextInfoPanel extends javax.swing.JPanel
{
    private TextDescriptor.Position initialPos;
    private TextDescriptor.Size initialSize;
    private TextDescriptor.Rotation initialRotation;
    private boolean initialItalic, initialBold, initialUnderline;
    private boolean initialInvisibleOutsideCell;
    private int initialFont;
    private double initialXOffset, initialYOffset;
    private double initialBoxedWidth, initialBoxedHeight;
    private int initialColorIndex;

    private TextDescriptor td;
    private String varName;
    private ElectricObject owner;
    private NodeInst unTransformNi;

    /**
     * Create a new TextInfoPanel that can be used to edit
     * the Text Display options of a Variable.
     */
    public TextInfoPanel()
    {
        initComponents();

        font.addItem("DEFAULT FONT");
        Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        for(int i=0; i<fonts.length; i++) {
            font.addItem(fonts[i].getFontName());
        }

        // populate Rotations combo box
        for (int i=0; i<TextDescriptor.Rotation.getNumRotations(); i++) {
            TextDescriptor.Rotation rot = TextDescriptor.Rotation.getRotationAt(i);
            // rotations stored as integers, retrieve by Rotation.getIndex()
            rotation.addItem(new Integer(rot.getAngle()));
        }

        // populate Anchor combo box
        for (Iterator it = TextDescriptor.Position.getPositions(); it.hasNext(); ) {
            TextDescriptor.Position pos = (TextDescriptor.Position)it.next();
            textAnchor.addItem(pos);
        }

        // populate color combo box
        int [] colorIndices = EGraphics.getColorIndices();
        textColorComboBox.addItem("");
        for (int i=0; i<colorIndices.length; i++) {
            String str = EGraphics.getColorIndexName(colorIndices[i]);
            textColorComboBox.addItem(str);
        }

        // default settings

        // offset
        initialXOffset = initialYOffset = 0;
        xOffset.setText("0"); yOffset.setText("0");
        // invisible outside cell
        initialInvisibleOutsideCell = false;
        invisibleOutsideCell.setSelected(initialInvisibleOutsideCell);
        // size
        initialSize = TextDescriptor.Size.newRelSize(1.0);
        unitsButton.setSelected(true);
        unitsSize.setText("1.0");
        // position
        initialPos = TextDescriptor.Position.CENT;
        textAnchor.setSelectedItem(initialPos);
        // font
        initialFont = 0;
        font.setSelectedIndex(initialFont);
        // italic/bold/underline
        initialItalic = false; italic.setEnabled(false);
        initialBold = false; bold.setEnabled(false);
        initialUnderline = false; underline.setEnabled(false);
        // rotation
        initialRotation = TextDescriptor.Rotation.ROT0;
        rotation.setSelectedItem(initialRotation);
        // color
        initialColorIndex = 0;

        setTextDescriptor(null, null);
    }

    /**
     * Set what the dialog displays: It can display and allow editing of the settings
     * for an existing text descriptor, or it can display and allow editing of default values
     * for a text descriptor of a variable that has not yet been created.
     * <p>if owner.getTextDescriptor(varName) returns non-null td, display and allow editing of the td text options
     * <p>else if varName is non-null, display and allow editing of default values.
     * <p>if varName is null, disable entire panel
     * @param varName the name a variable that will be created later.
     * @param owner the object the variable is on.
     */
    public synchronized void setTextDescriptor(String varName, ElectricObject owner)
    {
        // do not allow empty names for future vars
        if (varName != null) {
            varName = varName.trim();
            if (varName.equals("")) varName = null;
        }

        this.varName = varName;
        this.owner = owner;

        boolean enabled = owner != null && varName != null;

        // update enabled state of everything
        // can't just enable all children because objects might be inside JPanel
        pointsSize.setEnabled(enabled);
        unitsSize.setEnabled(enabled);
        pointsButton.setEnabled(enabled);
        unitsButton.setEnabled(enabled);
        xOffset.setEnabled(enabled);
        yOffset.setEnabled(enabled);
        font.setEnabled(enabled);
        textAnchor.setEnabled(enabled);
        rotation.setEnabled(enabled);
        bold.setEnabled(enabled);
        italic.setEnabled(enabled);
        underline.setEnabled(enabled);
        invisibleOutsideCell.setEnabled(enabled);
        seeNode.setEnabled(enabled);
        boxedWidth.setEnabled(false);               // only enabled when boxed anchor is selected
        boxedHeight.setEnabled(false);               // only enabled when boxed anchor is selected
        textColorComboBox.setEnabled(enabled);

        if (!enabled) return;

        // if td is null and we are going to apply value to future var,
        // use current panel settings.
		td = owner.getTextDescriptor(varName);
        if (td == null) return;

        NodeInst ni = null;
        // use location of owner if it is a generic invisible pin, because
        // this is the location of the text on the cell
        if (owner != null)
        {
            if (owner instanceof NodeInst)
            {
                ni = (NodeInst)owner;
                if (ni.getProto() != Generic.tech.invisiblePinNode)
                {
                    ni = null;                  // ni is null unless owner is invisible pin
                }
            }
        }

        // find the node that this is sitting on, to handle offsets and rotations
        unTransformNi = null;
        if (owner != null)
        {
        	if (owner instanceof NodeInst) unTransformNi = (NodeInst)owner; else
        		if (owner instanceof Export) unTransformNi = ((Export)owner).getOriginalPort().getNodeInst();
        }

        // set the offset
        if (ni != null)
        {
            initialXOffset = ni.getAnchorCenterX();
            initialYOffset = ni.getAnchorCenterY();
        } else
        {
            initialXOffset = td.getXOff();
            initialYOffset = td.getYOff();
        	if (unTransformNi != null)
        	{
        		Point2D off = new Point2D.Double(initialXOffset, initialYOffset);
        		AffineTransform trans = unTransformNi.pureRotateOut();
        		trans.transform(off, off);
        		initialXOffset = off.getX();
        		initialYOffset = off.getY();
        	}
        }
        xOffset.setText(TextUtils.formatDouble(initialXOffset));
        yOffset.setText(TextUtils.formatDouble(initialYOffset));

        // set the "invisible outside cell"
        initialInvisibleOutsideCell = td.isInterior();
        invisibleOutsideCell.setSelected(initialInvisibleOutsideCell);

        // set the size
        initialSize = td.getSize();
        EditWindow wnd = EditWindow.getCurrent();
        if (initialSize.isAbsolute())
        {
            pointsButton.setSelected(true);
            pointsSize.setText(TextUtils.formatDouble(initialSize.getSize()));
            unitsSize.setText("");
            if (wnd != null)
            {
                double unitSize = wnd.getTextUnitSize((int)initialSize.getSize());
                unitsSize.setText(TextUtils.formatDouble(unitSize));
            }
        } else
        {
            unitsButton.setSelected(true);
            unitsSize.setText(TextUtils.formatDouble(initialSize.getSize()));
            pointsSize.setText("");
            if (wnd != null)
            {
                double pointSize = wnd.getTextScreenSize(initialSize.getSize());
                pointsSize.setText(TextUtils.formatDouble(pointSize));
            }
        }

        // set Position
        initialPos = td.getPos();
        boxedWidth.setText("");
        boxedHeight.setText("");
        initialBoxedWidth = -1.0;
        initialBoxedHeight = -1.0;
        boolean ownerIsNodeInst = false;
        if (owner instanceof NodeInst) ownerIsNodeInst = true;
        if (ownerIsNodeInst) {
            // make sure BOXED option is part of pull down menu
            boolean found = false;
            for (int i=0; i<textAnchor.getModel().getSize(); i++) {
                TextDescriptor.Position pos = (TextDescriptor.Position)textAnchor.getModel().getElementAt(i);
                if (pos == TextDescriptor.Position.BOXED) {
                    found = true; break;
                }
            }
            if (!found) {
                textAnchor.addItem(TextDescriptor.Position.BOXED);
            }
            // set current boxed width and height, even if disabled
            // later call to textAnchorItemStateChanged(null) will set enabled/disabled state
            NodeInst ni2 = (NodeInst)owner;
            initialBoxedWidth = ni2.getXSize();
            initialBoxedHeight = ni2.getYSize();
            boxedWidth.setText(TextUtils.formatDouble(ni2.getXSize()));
            boxedHeight.setText(TextUtils.formatDouble(ni2.getYSize()));
        }
        if (!ownerIsNodeInst) {
            // The TextDescriptor cannot be set to boxed, so remove it from the list
            textAnchor.removeItem(TextDescriptor.Position.BOXED);
        }

		// set anchor
		Poly.Type type = td.getPos().getPolyType();
		type = Poly.rotateType(type, owner);
		TextDescriptor.Position pos = TextDescriptor.Position.getPosition(type);
        textAnchor.setSelectedItem(pos);

        // update enable/disabled state of boxed height, width textfields
        textAnchorItemStateChanged(null);

        // set the font
        initialFont = td.getFace();
        if (initialFont == 0) font.setSelectedIndex(0); else
        {
            TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(initialFont);
            if (af != null)
                font.setSelectedItem(af.getName());
        }

        // set italic / bold / underline
        initialItalic = td.isItalic();
        italic.setSelected(initialItalic);
        initialBold = td.isBold();
        bold.setSelected(initialBold);
        initialUnderline = td.isUnderline();
        underline.setSelected(initialUnderline);

        // set the rotation
        initialRotation = td.getRotation();
        rotation.setSelectedIndex(td.getRotation().getIndex());

        // set the color
        initialColorIndex = td.getColorIndex();
        int colorComboIndex = 0;
        int [] colorIndices = EGraphics.getColorIndices();
		for(int i=0; i<colorIndices.length; i++)
			if (colorIndices[i] == initialColorIndex) colorComboIndex = i+1;
        textColorComboBox.setSelectedIndex(colorComboIndex);
    }

    /**
     * Apply any changes the user made to the Text options.
     * @return true if any changes made to database, false if no changes made.
     */
    public synchronized boolean applyChanges() {

        if (varName == null) return false;

        boolean changed = false;

        // handle changes to the size
        TextDescriptor.Size newSize = null;
        if (pointsButton.isSelected())
        {
            int size = TextUtils.atoi(pointsSize.getText());
            newSize = TextDescriptor.Size.newAbsSize(size);
        } else
        {
            double size = TextUtils.atof(unitsSize.getText());
            newSize = TextDescriptor.Size.newRelSize(size);
        }
        // default size
        if (newSize == null) newSize = TextDescriptor.Size.newRelSize(1.0);
        if (!newSize.equals(initialSize)) changed = true;

        // handle changes to the offset
        double currentXOffset = TextUtils.atof(xOffset.getText());
        double currentYOffset = TextUtils.atof(yOffset.getText());
        if (!DBMath.doublesEqual(currentXOffset, initialXOffset) ||
                !DBMath.doublesEqual(currentYOffset, initialYOffset))
            changed = true;

        // handle changes to the anchor point
        TextDescriptor.Position newPosition = (TextDescriptor.Position)textAnchor.getSelectedItem();

		Poly.Type type = newPosition.getPolyType();
		type = Poly.unRotateType(type, owner);
		newPosition = TextDescriptor.Position.getPosition(type);
        if (newPosition != initialPos) changed = true;
        double newBoxedWidth = 10;
        double newBoxedHeight = 10;
        if (newPosition == TextDescriptor.Position.BOXED) {
            Double width, height;
            try {
                width = new Double(boxedWidth.getText());
                height = new Double(boxedHeight.getText());
                newBoxedWidth = width.doubleValue();
                newBoxedHeight = height.doubleValue();
            } catch (java.lang.NumberFormatException e) {
                if (owner instanceof NodeInst) {
                    NodeInst ni = (NodeInst)owner;
                    newBoxedWidth = ni.getXSize();
                    newBoxedHeight = ni.getYSize();
                }
            }
            if (newBoxedWidth != initialBoxedWidth) changed = true;
            if (newBoxedHeight != initialBoxedHeight) changed = true;
        }

        // handle changes to the rotation
        int index = rotation.getSelectedIndex();
        TextDescriptor.Rotation newRotation = TextDescriptor.Rotation.getRotationAt(index);
        if (newRotation != initialRotation) changed = true;

        // handle changes to the font
        int newFont = font.getSelectedIndex();
        if (newFont != initialFont) changed = true;

        // handle changes to checkboxes
        boolean newItalic = italic.isSelected();
        if (newItalic != initialItalic) changed = true;
        boolean newBold = bold.isSelected();
        if (newBold != initialBold) changed = true;
        boolean newUnderlined = underline.isSelected();
        if (newUnderlined != initialUnderline) changed = true;
        boolean newInvis = invisibleOutsideCell.isSelected();
        if (newInvis != initialInvisibleOutsideCell) changed = true;

		// handle changes to the color
		int newColorIndex = 0;
		int [] colorIndices = EGraphics.getColorIndices();
        int newColorComboIndex = textColorComboBox.getSelectedIndex();
        if (newColorComboIndex > 0) newColorIndex = colorIndices[newColorComboIndex-1];
        if (newColorIndex != initialColorIndex) changed = true;

        if (td != null) {
            // no changes on current td, return false
            if (!changed) return false;
        } else {
            // because this is a new var, check if owner is a Cell
            // if so, increment the Y-offset so that sequentially created
            // new vars do not overlap on the schematic
            if (owner instanceof Cell) {
                currentYOffset -= 2.0;
            }
        }

        // changes made: generate job and update initial values
        ChangeText job = new ChangeText(
                owner,
				unTransformNi,
                varName,
                newSize,
                newPosition, newBoxedWidth, newBoxedHeight,
                newRotation,
                (String)font.getSelectedItem(),
                currentXOffset, currentYOffset,
                newItalic, newBold, newUnderlined, newInvis, newColorIndex);


        initialSize = newSize;
        initialXOffset = currentXOffset;
        initialYOffset = currentYOffset;
        initialPos = newPosition;
        initialRotation = newRotation;
        initialFont = newFont;
        initialItalic = newItalic;
        initialUnderline = newUnderlined;
        initialBold = newBold;
        initialInvisibleOutsideCell = newInvis;
        initialBoxedWidth = newBoxedWidth;
        initialBoxedHeight = newBoxedHeight;
        initialColorIndex = newColorIndex;

        return true;
    }

    private static class ChangeText extends Job
	{
        private ElectricObject owner;
        private NodeInst unTransformNi;
        private String varName;
        private TextDescriptor.Size size;
        private TextDescriptor.Position position;
        private double boxedWidth, boxedHeight;
        private TextDescriptor.Rotation rotation;
        private String font;
        private double xoffset, yoffset;
        private boolean italic, bold, underline, invis;
        private int newColorIndex;

        private ChangeText(
                ElectricObject owner,
				NodeInst unTransformNi,
                String varName,
                TextDescriptor.Size size,
                TextDescriptor.Position position, double boxedWidth, double boxedHeight,
                TextDescriptor.Rotation rotation,
                String font,
                double xoffset, double yoffset,
                boolean italic, boolean bold, boolean underline, boolean invis, int newColorIndex
                )
        {
            super("Modify Text", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.owner = owner;
            this.unTransformNi = unTransformNi;
            this.varName = varName;
            this.size = size;
            this.position = position;
            this.boxedWidth = boxedWidth;
            this.boxedHeight = boxedHeight;
            this.rotation = rotation;
            this.font = font;
            this.xoffset = xoffset; this.yoffset = yoffset;
            this.italic = italic; this.bold = bold; this.underline = underline; this.invis = invis;
            this.newColorIndex = newColorIndex;
            startJob();
        }

        public boolean doIt()
        {
            // if td is null, use future var name to look up var and get td
//             if (td == null) {
//                 Variable var = owner.getVar(futureVar);
//                 if (var == null) return false;                // var doesn't exist, failed
//                 td = var.getTextDescriptor();           // use TextDescriptor from new var
//             }
			MutableTextDescriptor td = owner.getMutableTextDescriptor(varName);
			if (td == null) return false;

            // handle changes to the size
            if (size.isAbsolute())
                td.setAbsSize((int)size.getSize());
            else
                td.setRelSize(size.getSize());

            // handle changes to the text corner
            td.setPos(position);
            if (owner instanceof NodeInst) {
                NodeInst ni = (NodeInst)owner;
                if (position == TextDescriptor.Position.BOXED) {
                    // set the boxed size
                    ni.modifyInstance(0, 0, boxedWidth-ni.getXSize(),
                            boxedHeight-ni.getYSize(), 0);
                } else {
                    // make invisible pin zero size if no longer boxed
                    if (ni.getProto() == Generic.tech.invisiblePinNode) {
                        if (ni.getXSize() != 0 || ni.getYSize() != 0) {
                            // no longer boxed: make it zero size
                            ni.modifyInstance(0, 0, -ni.getXSize(), -ni.getYSize(), 0);
                        }
                    }
                }
            }

            // handle changes to the rotation
            td.setRotation(rotation);

            // handle changes to the offset
            if (owner instanceof NodeInst) {
                NodeInst ni = (NodeInst)owner;
                if (ni.getProto() == Generic.tech.invisiblePinNode) {
                    double dX = xoffset - ni.getAnchorCenterX();
                    double dY = yoffset - ni.getAnchorCenterY();
                    ni.modifyInstance(dX, dY, 0, 0, 0);
                } else td.setOff(xoffset, yoffset);
            } else {
            	if (unTransformNi != null)
            	{
            		Point2D off = new Point2D.Double(xoffset, yoffset);
            		AffineTransform trans = unTransformNi.pureRotateIn();
            		trans.transform(off, off);
            		xoffset = off.getX();
            		yoffset = off.getY();
            	}
                td.setOff(xoffset, yoffset);
            }

            // handle changes to "invisible outside cell"
            td.setInterior(invis);

            // handle changes to the font
            if (font.equals("DEFAULT FONT")) td.setFace(0);
            else {
                TextDescriptor.ActiveFont newFont = TextDescriptor.ActiveFont.findActiveFont(font);
                int newFontIndex = newFont.getIndex();
                td.setFace(newFontIndex);
            }

            // handle changes to italic / bold / underline
            td.setItalic(italic);
            td.setBold(bold);
            td.setUnderline(underline);
            td.setColorIndex(newColorIndex);

			owner.setTextDescriptor(varName, td);
			return true;
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        sizes = new javax.swing.ButtonGroup();
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
        invisibleOutsideCell = new javax.swing.JCheckBox();
        seeNode = new javax.swing.JButton();
        textAnchor = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        boxedWidth = new javax.swing.JTextField();
        boxedHeight = new javax.swing.JTextField();
        jLabelX = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        textColorComboBox = new javax.swing.JComboBox();

        setLayout(new java.awt.GridBagLayout());

        setBorder(new javax.swing.border.EtchedBorder());
        setName("");
        jLabel4.setText("Text Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(jLabel4, gridBagConstraints);

        pointsSize.setColumns(8);
        pointsSize.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 2, 1, 4);
        add(pointsSize, gridBagConstraints);

        unitsSize.setColumns(8);
        unitsSize.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 4, 4);
        add(unitsSize, gridBagConstraints);

        pointsButton.setText("Points (max 63)");
        sizes.add(pointsButton);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        add(pointsButton, gridBagConstraints);

        unitsButton.setText("Units (max 127.75)");
        sizes.add(unitsButton);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        add(unitsButton, gridBagConstraints);

        jLabel5.setText("Font:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(jLabel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(font, gridBagConstraints);

        italic.setText("Italic");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        add(italic, gridBagConstraints);

        bold.setText("Bold");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        add(bold, gridBagConstraints);

        underline.setText("Underline");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        add(underline, gridBagConstraints);

        jLabel6.setText("Rotation:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(jLabel6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(rotation, gridBagConstraints);

        jLabel8.setText("X offset:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 3, 0);
        add(jLabel8, gridBagConstraints);

        xOffset.setColumns(8);
        xOffset.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 1, 4);
        add(xOffset, gridBagConstraints);

        jLabel9.setText("Y offset:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 0, 0);
        add(jLabel9, gridBagConstraints);

        yOffset.setColumns(8);
        yOffset.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 0, 4);
        add(yOffset, gridBagConstraints);

        invisibleOutsideCell.setText("Invisible outside cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 2, 0);
        add(invisibleOutsideCell, gridBagConstraints);

        seeNode.setText("Highlight Owner");
        seeNode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                seeNodeActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        add(seeNode, gridBagConstraints);

        textAnchor.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                textAnchorItemStateChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(textAnchor, gridBagConstraints);

        jLabel1.setText("Anchor:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(jLabel1, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel2.setText("Boxed width:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jLabel2, gridBagConstraints);

        boxedWidth.setColumns(4);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(boxedWidth, gridBagConstraints);

        boxedHeight.setColumns(4);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(boxedHeight, gridBagConstraints);

        jLabelX.setText("height:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jLabelX, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        add(jPanel1, gridBagConstraints);

        jLabel7.setText("(0.25 increments)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        add(jLabel7, gridBagConstraints);

        jLabel3.setText("Color:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(textColorComboBox, gridBagConstraints);

    }//GEN-END:initComponents

    private void textAnchorItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_textAnchorItemStateChanged
        // if BOXED selected, enable input boxes, otherwise disable them
        TextDescriptor.Position pos = (TextDescriptor.Position)textAnchor.getSelectedItem();
        if (pos == TextDescriptor.Position.BOXED) {
            boxedWidth.setEnabled(true);
            boxedWidth.setBackground(Color.WHITE);
            boxedHeight.setEnabled(true);
            boxedHeight.setBackground(Color.WHITE);
            // if position is boxed, offset means nothing unless this is a generic invisible pin,
            // in which case offset is actually the pin's position.
            // default case: disable offsets
            xOffset.setEnabled(false); yOffset.setEnabled(false);
            if (owner instanceof NodeInst) {
                NodeInst ni = (NodeInst)owner;
                if (ni.getProto() == Generic.tech.invisiblePinNode) {
                    // enable offsets for generic invisible pin
                    xOffset.setEnabled(true); yOffset.setEnabled(true);
                }
            }
        } else {
            boxedWidth.setEnabled(false);
            boxedWidth.setBackground(this.getBackground());
            boxedHeight.setEnabled(false);
            boxedHeight.setBackground(this.getBackground());
            // if position is not boxed, enable offset
            xOffset.setEnabled(true); yOffset.setEnabled(true);
        }
    }//GEN-LAST:event_textAnchorItemStateChanged

    private void seeNodeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_seeNodeActionPerformed
    {//GEN-HEADEREND:event_seeNodeActionPerformed
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        Cell cell = wf.getContent().getCell();
        // if owner is an export, highlight the port inst
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;

        if (owner instanceof Export) {
            wnd.getHighlighter().addElectricObject(((Export)owner).getOriginalPort(), cell);
        } else {
            wnd.getHighlighter().addElectricObject(owner, cell);
        }
        wnd.getHighlighter().finished();
/*        ElectricObject eobj = shownText.getElectricObject();
        if (eobj instanceof NodeInst || eobj instanceof PortInst)
        {
            Cell cell = shownText.getCell();
            Variable var = shownText.getVar();
            Name name = shownText.getName();

            Highlight.clear();
            Highlight.addElectricObject(eobj, cell);
            Highlight newHigh = Highlight.addText(eobj, cell, var, name);
            Highlight.finished();
        }*/
    }//GEN-LAST:event_seeNodeActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox bold;
    private javax.swing.JTextField boxedHeight;
    private javax.swing.JTextField boxedWidth;
    private javax.swing.JComboBox font;
    private javax.swing.JCheckBox invisibleOutsideCell;
    private javax.swing.JCheckBox italic;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelX;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JRadioButton pointsButton;
    private javax.swing.JTextField pointsSize;
    private javax.swing.JComboBox rotation;
    private javax.swing.JButton seeNode;
    private javax.swing.ButtonGroup sizes;
    private javax.swing.JComboBox textAnchor;
    private javax.swing.JComboBox textColorComboBox;
    private javax.swing.JCheckBox underline;
    private javax.swing.JRadioButton unitsButton;
    private javax.swing.JTextField unitsSize;
    private javax.swing.JTextField xOffset;
    private javax.swing.JTextField yOffset;
    // End of variables declaration//GEN-END:variables

}
