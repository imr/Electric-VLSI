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

import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.*;
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

    private TextDescriptor td;
    private ElectricObject owner;

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
            if (pos == TextDescriptor.Position.BOXED) continue;
            textAnchor.addItem(pos);
        }

        setTextDescriptor(null, null);
    }

    /**
     * Set the variable whose Text options will be displayed
     * in the Panel.  If Variable is null, the Panel and it's
     * children will be disabled.
     * @param td the TextDescriptor whose text settings will be displayed.
     * @param owner the object the variable is on.
     */
    public synchronized void setTextDescriptor(TextDescriptor td, ElectricObject owner)
    {
        this.td = td;
        this.owner = owner;

        boolean enabled = (td == null) ? false : true;

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

        if (!enabled) return;

        // set Position
        initialPos = td.getPos();
        textAnchor.setSelectedItem(td.getPos());

        // set the offset
        NodeInst ni = null;
        // use location of owner if it is a generic invisible pin, because
        // this is the location of the text on the cell
        if (owner != null) {
            if (owner instanceof NodeInst) {
                ni = (NodeInst)owner;
                if (ni.getProto() != Generic.tech.invisiblePinNode) {
                    ni = null;                  // ni is null unless owner is invisible pin
                }
            }
        }
        if (ni != null) {
            initialXOffset = ni.getGrabCenterX();
            initialYOffset = ni.getGrabCenterY();
        } else {
            initialXOffset = td.getXOff();
            initialYOffset = td.getYOff();
        }
        xOffset.setText(Double.toString(initialXOffset));
        yOffset.setText(Double.toString(initialYOffset));

        // set the "invisible outside cell"
        initialInvisibleOutsideCell = td.isInterior();
        invisibleOutsideCell.setSelected(initialInvisibleOutsideCell);

        // set the size
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
    }

    /**
     * Apply any changes the user made to the Text options.
     * @return true if any changes made to database, false if no changes made.
     */
    public synchronized boolean applyChanges() {
        return applyChanges(td, owner, null);
    }

    /**
     * This method is used when the Panel's field values will be used to
     * modify a new Variable, i.e. one that has not yet been created.
     * The Job to create the variable must already have been generated before
     * this method is called.
     * @param futureVar the name of the variable that has yet to be created
     * @return true if any changes committed to database, false otherwise.
     */
    public synchronized boolean applyChangesFutureVar(String futureVar) {
        return applyChanges(null, null, futureVar);
    }

    /**
     * This method applies the values in the fields of the Panel to:
     * <p> - var and td on owner if futureVar is null.
     * <p> - a Variable named "futureVar" on owner if futureVar is not null.
     * <p>This allows fields to be applied to a var that will be created by
     * a Job already on the Job queue.
     * @param td the TextDescriptor to be modified, instead of the TextDescriptor passed to setTextDescriptor()
     * @param owner the owner of the Variable
     * @param futureVar the name of the variable on 'owner' to be modified
     * @return true if changes made, false otherwise
     */
    private boolean applyChanges(TextDescriptor td, ElectricObject owner, String futureVar) {
        if ((td == null) && (futureVar == null)) return false;

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
        if (!newSize.equals(initialSize)) changed = true;

        // handle changes to the offset
        double currentXOffset = TextUtils.atof(xOffset.getText());
        double currentYOffset = TextUtils.atof(yOffset.getText());
        if (!EMath.doublesEqual(currentXOffset, initialXOffset) ||
                !EMath.doublesEqual(currentYOffset, initialYOffset))
            changed = true;

        // handle changes to the anchor point
        TextDescriptor.Position newPosition = (TextDescriptor.Position)textAnchor.getSelectedItem();
        if (newPosition != initialPos) changed = true;

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

        // no changes, return false
        if (!changed) return false;

        // changes made: generate job and update initial values
        ChangeText job = new ChangeText(
                td,
                owner,
                futureVar,
                newSize,
                newPosition,
                newRotation,
                (String)font.getSelectedItem(),
                currentXOffset, currentYOffset,
                newItalic, newBold, newUnderlined, newInvis);


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

        return true;
    }


    private static class ChangeText extends Job
	{
        private TextDescriptor td;
        private ElectricObject owner;
        private String futureVar;
        private TextDescriptor.Size size;
        private TextDescriptor.Position position;
        private TextDescriptor.Rotation rotation;
        private String font;
        private double xoffset, yoffset;
        private boolean italic, bold, underline, invis;

        private ChangeText(
                TextDescriptor td,
                ElectricObject owner,
                String futureVar,
                TextDescriptor.Size size,
                TextDescriptor.Position position,
                TextDescriptor.Rotation rotation,
                String font,
                double xoffset, double yoffset,
                boolean italic, boolean bold, boolean underline, boolean invis
                )
        {
            super("Modify Text", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.td = td;
            this.owner = owner;
            this.futureVar = futureVar;
            this.size = size;
            this.position = position;
            this.rotation = rotation;
            this.font = font;
            this.xoffset = xoffset; this.yoffset = yoffset;
            this.italic = italic; this.bold = bold; this.underline = underline; this.invis = invis;
            startJob();
        }

        public void doIt()
        {
            // if futureVar is not null, try get to Var to edit from owner
            // this variable may not have existed when the Job was generated, thus we
            // look it up by name now.
            if (futureVar != null) {
                Variable var = owner.getVar(futureVar);
                if (var == null) return;                // var doesn't exist, failed
                td = var.getTextDescriptor();           // use TextDescriptor from new var
            }

            // handle changes to the size
            if (size.isAbsolute())
                td.setAbsSize((int)size.getSize());
            else
                td.setRelSize(size.getSize());

            // handle changes to the text corner
            td.setPos(position);

            // handle changes to the rotation
            td.setRotation(rotation);

            // handle changes to the offset
            if (owner instanceof NodeInst) {
                NodeInst ni = (NodeInst)owner;
                    double dX = xoffset - ni.getGrabCenterX();
                    double dY = yoffset - ni.getGrabCenterY();
                    ni.modifyInstance(dX, dY, 0, 0, 0);
            } else {
                td.setOff(xoffset, yoffset);
            }

            // handle changes to "invisible outside cell"
            if (invis) td.setInterior(); else
                td.clearInterior();

            // handle changes to the font
            if (font.equals("DEFAULT FONT")) td.setFace(0);
            else {
                TextDescriptor.ActiveFont newFont = TextDescriptor.ActiveFont.findActiveFont(font);
                int newFontIndex = newFont.getIndex();
                td.setFace(newFontIndex);
            }

            // handle changes to italic / bold / underline
            if (italic) td.setItalic(); else
                td.clearItalic();
            if (bold) td.setBold(); else
                td.clearBold();
            if (underline) td.setUnderline(); else
                td.clearUnderline();
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
        gridBagConstraints.insets = new java.awt.Insets(4, 2, 1, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weightx = 0.1;
        add(pointsSize, gridBagConstraints);

        unitsSize.setColumns(8);
        unitsSize.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 0.1;
        add(unitsSize, gridBagConstraints);

        pointsButton.setText("Points");
        sizes.add(pointsButton);
        pointsButton.setMaximumSize(new java.awt.Dimension(75, 24));
        pointsButton.setMinimumSize(new java.awt.Dimension(75, 24));
        pointsButton.setPreferredSize(new java.awt.Dimension(75, 24));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        add(pointsButton, gridBagConstraints);

        unitsButton.setText("Units");
        sizes.add(unitsButton);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(unitsButton, gridBagConstraints);

        jLabel5.setText("Font:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(jLabel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 2, 4);
        add(font, gridBagConstraints);

        italic.setText("Italic");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        add(italic, gridBagConstraints);

        bold.setText("Bold");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        add(bold, gridBagConstraints);

        underline.setText("Underline");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
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
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(6, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(jLabel8, gridBagConstraints);

        xOffset.setColumns(8);
        xOffset.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 1, 4);
        add(xOffset, gridBagConstraints);

        jLabel9.setText("Y offset:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 7, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(jLabel9, gridBagConstraints);

        yOffset.setColumns(8);
        yOffset.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 0, 4);
        add(yOffset, gridBagConstraints);

        invisibleOutsideCell.setText("Invisible outside cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(invisibleOutsideCell, gridBagConstraints);

        seeNode.setText("See Owner");
        seeNode.setMargin(new java.awt.Insets(2, 2, 2, 2));
        seeNode.setMaximumSize(new java.awt.Dimension(80, 26));
        seeNode.setMinimumSize(new java.awt.Dimension(80, 26));
        seeNode.setPreferredSize(new java.awt.Dimension(80, 26));
        seeNode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                seeNodeActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        add(seeNode, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(textAnchor, gridBagConstraints);

        jLabel1.setText("Anchor:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(jLabel1, gridBagConstraints);

    }//GEN-END:initComponents

    private void seeNodeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_seeNodeActionPerformed
    {//GEN-HEADEREND:event_seeNodeActionPerformed
        EditWindow wnd = EditWindow.getCurrent();
        Cell cell = wnd.getCell();
        Highlight.addElectricObject(owner, cell);
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
    private javax.swing.JComboBox font;
    private javax.swing.JCheckBox invisibleOutsideCell;
    private javax.swing.JCheckBox italic;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JRadioButton pointsButton;
    private javax.swing.JTextField pointsSize;
    private javax.swing.JComboBox rotation;
    private javax.swing.JButton seeNode;
    private javax.swing.ButtonGroup sizes;
    private javax.swing.JComboBox textAnchor;
    private javax.swing.JCheckBox underline;
    private javax.swing.JRadioButton unitsButton;
    private javax.swing.JTextField unitsSize;
    private javax.swing.JTextField xOffset;
    private javax.swing.JTextField yOffset;
    // End of variables declaration//GEN-END:variables

}
