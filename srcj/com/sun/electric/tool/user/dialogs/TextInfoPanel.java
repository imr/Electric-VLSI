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
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Arrays;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 * A Panel to display and edit Text Display options for a Variable.
 * Does not display attribute specific options such as Code or Show Style.
 */
public class TextInfoPanel extends javax.swing.JPanel
{
	private boolean updateChangesInstantly;
	private boolean loading = false;
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
    private Variable.Key varKey;
    private ElectricObject owner;
    private NodeInst unTransformNi;

    /**
     * Create a new TextInfoPanel that can be used to edit
     * the Text Display options of a Variable.
     */
    public TextInfoPanel(boolean updateChangesInstantly)
    {
    	this.updateChangesInstantly = updateChangesInstantly;
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
        for (Iterator<TextDescriptor.Position> it = TextDescriptor.Position.getPositions(); it.hasNext(); ) {
            TextDescriptor.Position pos = it.next();
            textAnchor.addItem(pos);
        }

        // populate color combo box
        int [] colorIndices = EGraphics.getColorIndices();
        textColorComboBox.addItem("DEFAULT COLOR");
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
        unitsButton.setText("Units (min " + TextDescriptor.Size.TXTMINQGRID + ", max " + TextDescriptor.Size.TXTMAXQGRID + ")");
        initialSize = TextDescriptor.Size.newRelSize(1.0);
        unitsButton.setSelected(true);
        unitsSize.setText("1.0");
        pointsButton.setText("Points (min " + TextDescriptor.Size.TXTMINPOINTS + ", max " + TextDescriptor.Size.TXTMAXPOINTS + ")");
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
        initialColorIndex = 0; // Zero is the default font

        setTextDescriptor(null, null);

        // listeners
        unitsSize.getDocument().addDocumentListener(new TextInfoDocumentListener(this));
        pointsSize.getDocument().addDocumentListener(new TextInfoDocumentListener(this));
        xOffset.getDocument().addDocumentListener(new TextInfoDocumentListener(this));
        yOffset.getDocument().addDocumentListener(new TextInfoDocumentListener(this));
        boxedWidth.getDocument().addDocumentListener(new TextInfoDocumentListener(this));
        boxedHeight.getDocument().addDocumentListener(new TextInfoDocumentListener(this));
        unitsButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt) { fieldChanged(); }
        });
        pointsButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt) { fieldChanged(); }
        });
        italic.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt) { fieldChanged(); }
        });
        bold.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt) { fieldChanged(); }
        });
        underline.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt) { fieldChanged(); }
        });
        invisibleOutsideCell.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt) { fieldChanged(); }
        });
        rotation.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt) { fieldChanged(); }
        });
        textAnchor.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt) { fieldChanged(); }
        });
        font.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt) { fieldChanged(); }
        });
        textColorComboBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt) { fieldChanged(); }
        });
    }

    /**
     * Set what the dialog displays: It can display and allow editing of the settings
     * for an existing text descriptor, or it can display and allow editing of default values
     * for a text descriptor of a variable that has not yet been created.
     * <p>if owner.getTextDescriptor(varKey) returns non-null td, display and allow editing of the td text options
     * <p>else if varKey is non-null, display and allow editing of default values.
     * <p>if varKey is null, disable entire panel
     * @param varKey the key a variable that will be created later.
     * @param owner the object the variable is on.
     */
    public synchronized void setTextDescriptor(Variable.Key varKey, ElectricObject owner)
    {
        // do not allow empty names for future vars
        if (varKey != null) {
            if (varKey.getName().trim().equals("")) varKey = null;
        }

        this.varKey = varKey;
        this.owner = owner;

        boolean enabled = owner != null && varKey != null;

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
		td = owner.getTextDescriptor(varKey);
        if (td == null) return;

        loading = true;

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
                double unitSize = initialSize.getSize() / wnd.getScale();
                if (unitSize > TextDescriptor.Size.TXTMAXQGRID)
                    unitSize = TextDescriptor.Size.TXTMAXQGRID;
                else if (unitSize < TextDescriptor.Size.TXTMINQGRID)
                    unitSize = TextDescriptor.Size.TXTMINQGRID;
                unitsSize.setText(TextUtils.formatDouble(unitSize));
            }
        } else
        {
            unitsButton.setSelected(true);
            unitsSize.setText(TextUtils.formatDouble(initialSize.getSize()));
            pointsSize.setText("");
            if (wnd != null)
            {
                double pointSize = initialSize.getSize()*wnd.getScale();
                if (pointSize > TextDescriptor.Size.TXTMAXPOINTS)
                    pointSize = TextDescriptor.Size.TXTMAXPOINTS;
                else if (pointSize < TextDescriptor.Size.TXTMINPOINTS)
                    pointSize = TextDescriptor.Size.TXTMINPOINTS;
                pointsSize.setText(String.valueOf((int)pointSize));
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
        int [] colorIndices = EGraphics.getColorIndices();
        int colorComboIndex = Arrays.binarySearch(colorIndices, initialColorIndex);
//      int colorComboIndex = 0;
//		for(int i=0; i<colorIndices.length; i++)
//        {
//			if (colorIndices[i] == initialColorIndex)
//            {
//                colorComboIndex = i+1;
//                break; // No need of checking the rest of the array
//            }
//        }
//        if (newValue != -1) newValue += 1;
//        if (newValue != colorComboIndex)
//            System.out.println("Wrong calculation");
        colorComboIndex = (colorComboIndex != -1) ? colorComboIndex + 1 : 0;
        textColorComboBox.setSelectedIndex(colorComboIndex);

        // report the global text scale
        globalTextScale.setText("Scaled by " + TextUtils.formatDouble(User.getGlobalTextScale()*100) + "%");

        loading = false;
    }

	/**
	 * Class to handle special changes to changes to a Text Info Panel edit fields.
	 */
	private static class TextInfoDocumentListener implements DocumentListener
	{
		TextInfoPanel dialog;

		TextInfoDocumentListener(TextInfoPanel dialog) { this.dialog = dialog; }

		public void changedUpdate(DocumentEvent e) { dialog.fieldChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.fieldChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.fieldChanged(); }
	}

	private void fieldChanged()
	{
		if (!updateChangesInstantly) return;
		if (loading) return;
		applyChanges(false);
	}

	/**
     * Apply any changes the user made to the Text options.
     * @return true if any changes made to database, false if no changes made.
     * @param adjustErrors
     */
    public synchronized boolean applyChanges(boolean adjustErrors) {

        if (varKey == null) return false;

        boolean changed = false;

        // handle changes to the size
        TextDescriptor.Size newSize = null;
        if (pointsButton.isSelected())
        {
            String s = pointsSize.getText();
            if (s.equals("")) return false; // removing all data from field
            int size = TextUtils.atoi(s);
            newSize = TextDescriptor.Size.newAbsSize(size);
        } else
        {
            String s = unitsSize.getText();
            if (s.equals("")) return false; // removing all data from field
            double size = TextUtils.atof(s);
            newSize = TextDescriptor.Size.newRelSize(size);
        }
        // default size
        if (newSize == null)
        {
            // if values given in pointsSize or unitsSize are invalid
            String value = (pointsButton.isSelected())? pointsSize.getText() : unitsSize.getText();
            if (adjustErrors)
                pointsSize.setText(String.valueOf(TextDescriptor.Size.TXTMAXPOINTS));
            System.out.println("Error: given size value of " + value + " is out of range. Setting default value.");
            newSize = TextDescriptor.Size.newRelSize(1.0);
        }
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
        Integer absSize = null;
        Double relSize = null;
        if (newSize.isAbsolute()) absSize = new Integer((int)newSize.getSize()); else
        	relSize = new Double(newSize.getSize());
        ChangeText job = new ChangeText(
                owner,
				unTransformNi,
                varKey,
                absSize, relSize,
                newPosition.getIndex(),
                newBoxedWidth, newBoxedHeight,
                newRotation.getIndex(),
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
        private Variable.Key varKey;
        private Integer absSize;
        private Double relSize;
        private int position;
        private double boxedWidth, boxedHeight;
        private int rotation;
        private String font;
        private double xoffset, yoffset;
        private boolean italic, bold, underline, invis;
        private int newColorIndex;

        private ChangeText(
                ElectricObject owner,
				NodeInst unTransformNi,
                Variable.Key varKey,
                Integer absSize, Double relSize,
                int position,
                double boxedWidth, double boxedHeight,
                int rotation,
                String font,
                double xoffset, double yoffset,
                boolean italic, boolean bold, boolean underline, boolean invis, int newColorIndex
                )
        {
            super("Modify Text", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.owner = owner;
            this.unTransformNi = unTransformNi;
            this.varKey = varKey;
            this.absSize = absSize;
            this.relSize = relSize;
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

        public boolean doIt() throws JobException
        {
			MutableTextDescriptor td = owner.getMutableTextDescriptor(varKey);
			if (td == null) return false;

            // handle changes to the size
            if (absSize != null)
                td.setAbsSize(absSize.intValue());
            else
                td.setRelSize(relSize.doubleValue());

            // handle changes to the text corner
            TextDescriptor.Position realPos = TextDescriptor.Position.getPositionAt(position);
            td.setPos(realPos);
            if (owner instanceof NodeInst)
            {
                NodeInst ni = (NodeInst)owner;
                if (realPos == TextDescriptor.Position.BOXED)
                {
                    // set the boxed size
                    ni.resize(boxedWidth-ni.getXSize(), boxedHeight-ni.getYSize());

                    // make invisible pin zero size if no longer boxed
                    if (ni.getProto() == Generic.tech.invisiblePinNode)
                    {
                        if (ni.getXSize() != 0 || ni.getYSize() != 0)
                        {
                            // no longer boxed: make it zero size
                            ni.resize(-ni.getXSize(), -ni.getYSize());
                        }
                    }
                }
            }

            // handle changes to the rotation
            td.setRotation(TextDescriptor.Rotation.getRotationAt(rotation));

            // handle changes to the offset
			NodeInst ni = null;
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

			// set the offset
			if (ni != null)
			{
                double dX = xoffset - ni.getAnchorCenterX();
                double dY = yoffset - ni.getAnchorCenterY();
                ni.move(dX, dY);
			} else
			{
				if (unTransformNi != null)
				{
            		Point2D off = new Point2D.Double(xoffset, yoffset);
            		AffineTransform trans = unTransformNi.pureRotateIn();
            		trans.transform(off, off);
            		xoffset = off.getX();
            		yoffset = off.getY();;
				}
                td.setOff(xoffset, yoffset);
			}

            // handle changes to "invisible outside cell"
            td.setInterior(invis);

            // handle changes to the font
            if (font.equals("DEFAULT FONT")) td.setFace(0);
            else {
                TextDescriptor.ActiveFont newFont = TextDescriptor.ActiveFont.findActiveFont(font);
                int newFontIndex = newFont != null ? newFont.getIndex() : 0;
                td.setFace(newFontIndex);
            }

            // handle changes to italic / bold / underline
            td.setItalic(italic);
            td.setBold(bold);
            td.setUnderline(underline);
            td.setColorIndex(newColorIndex);

			owner.setTextDescriptor(varKey, TextDescriptor.newTextDescriptor(td));
			return true;
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
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
        jLabel3 = new javax.swing.JLabel();
        textColorComboBox = new javax.swing.JComboBox();
        jPanel2 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        globalTextScale = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();

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

        sizes.add(pointsButton);
        pointsButton.setText("Points (max 63)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        add(pointsButton, gridBagConstraints);

        sizes.add(unitsButton);
        unitsButton.setText("Units (max 127.75)");
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
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(font, gridBagConstraints);

        italic.setText("Italic");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
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
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        add(underline, gridBagConstraints);

        jLabel6.setText("Rotation:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(jLabel6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
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
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
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
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        add(seeNode, gridBagConstraints);

        textAnchor.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                textAnchorItemStateChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(textAnchor, gridBagConstraints);

        jLabel1.setText("Anchor:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(jLabel1, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel2.setText("Boxed width:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jLabel2, gridBagConstraints);

        boxedWidth.setColumns(4);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
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
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        add(jPanel1, gridBagConstraints);

        jLabel3.setText("Color:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(textColorComboBox, gridBagConstraints);

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));

        jLabel10.setText("All Text Sizes are");
        jPanel2.add(jLabel10);

        globalTextScale.setText("Scaled by 100%");
        jPanel2.add(globalTextScale);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        add(jPanel2, gridBagConstraints);

        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.Y_AXIS));

        jLabel7.setText("(0.25 increments,");
        jPanel3.add(jLabel7);

        jLabel12.setText("maximum 4088)");
        jPanel3.add(jLabel12);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 2;
        add(jPanel3, gridBagConstraints);

    }
    // </editor-fold>//GEN-END:initComponents

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
    private javax.swing.JLabel globalTextScale;
    private javax.swing.JCheckBox invisibleOutsideCell;
    private javax.swing.JCheckBox italic;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel12;
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
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
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
