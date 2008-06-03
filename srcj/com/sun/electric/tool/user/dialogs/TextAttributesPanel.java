/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TextAttributesPanel.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.CodeExpression;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.User;

import java.util.Iterator;

import javax.swing.JPanel;

/**
 * A Panel to display Code, Units, ShowStyle, and isParameter/isInherits
 * information about a Variable (or TextDescriptor, if passed Variable is null).
 */
public class TextAttributesPanel extends JPanel
{
    private static final String displaynone = "None";

	private boolean updateChangesInstantly;
	private boolean loading = false;
    private Variable var;
    private TextDescriptor td;
    private Variable.Key varKey;
    private ElectricObject owner;
    private TextDescriptor.Unit initialUnit;
    private Object initialDispPos;      // this needs to be an object because one choice, "none" is a string
                                        // instead of a TextDescriptor.DispPos
    private CodeExpression.Code initialCode;

    /**
     * Create a Panel for editing attribute specific
     * text options of a Variable
     */
    public TextAttributesPanel(boolean updateChangesInstantly)
    {
    	this.updateChangesInstantly = updateChangesInstantly;
        initComponents();

        // add variable code types
        for (Iterator<CodeExpression.Code> it = CodeExpression.Code.getCodes(); it.hasNext(); ) {
            code.addItem(it.next());
        }

        // populate units dialog box
        for (Iterator<TextDescriptor.Unit> it = TextDescriptor.Unit.getUnits(); it.hasNext(); ) {
            units.addItem(it.next());
        }

        // populate show style dialog box
        populateShowComboBox(true);

        // default settings

        // set code
        initialCode = CodeExpression.Code.NONE;
        code.setSelectedItem(CodeExpression.Code.NONE);
        // set units
        initialUnit = TextDescriptor.Unit.NONE;
        units.setSelectedItem(initialUnit);
        // set show style
        initialDispPos = TextDescriptor.DispPos.NAMEVALUE;
        show.setSelectedItem(initialDispPos);

        // dialog is disabled by default
        setVariable(null, null);

        // listeners
        code.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt) { fieldChanged(); }
        });
        units.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt) { fieldChanged(); }
        });
        show.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt) { fieldChanged(); }
        });
    }

	private void fieldChanged()
	{
		if (!updateChangesInstantly) return;
		if (loading) return;
		applyChanges();
	}

    /**
     * Set the Variable that can be edited through this Panel.
     * <p>if owner.getTextDescriptor(varKey) returns non-null td, display and allow editing of the td text options
     * <p>else if varKey is non-null, display and allow editing of default values.
     * <p>if varKey is null, the entire Panel is disabled.
     * @param varKey the key of a variable to be changed
     * @param owner the owner of the variable
     */
    public synchronized void setVariable(Variable.Key varKey, ElectricObject owner) {

        loading = true;

		// default information
        if (Job.getDebug())
        {
	        attrInfo1.setText("YOU ARE RUNNING IN DEBUG MODE, SO:");
	        attrInfo2.setText("To create a parameter on a cell");
        	attrInfo3.setText("its name must start with 'ATTR_'");
        } else
        {
	        attrInfo1.setText("");
	        attrInfo2.setText("");
	        attrInfo3.setText("");
        }

		// do not allow empty var names
        if (varKey != null) {
            if (varKey.getName().trim().equals("")) varKey = null;
        }

        this.varKey = varKey;
        this.owner = owner;

        boolean enabled = owner != null && varKey != null;

        // update enabled state of everything
        // can't just enable all children because objects might be inside JPanel
        code.setEnabled(enabled);
        units.setEnabled(enabled);
        show.setEnabled(enabled);

        if (!enabled) return;

        // if td is null (implies var is null)
        // then use the current panel values to apply to varName.
        td = owner.getTextDescriptor(varKey);
        if (td == null) return;
        var = owner.getParameterOrVariable(varKey);

        // otherwise, use td

        // set code
        initialCode = CodeExpression.Code.NONE;
        if (var != null) {
            initialCode = var.getCode();
            code.setSelectedItem(initialCode);
        } else {
            // var null, disable code
            code.setEnabled(false);
        }
        // set units
        initialUnit = td.getUnit();
        units.setSelectedItem(td.getUnit());
        // set show style
		if (td.isDisplay())
			initialDispPos = td.getDispPart();
		else
			initialDispPos = displaynone;
        // show style is none if var non-null and isDisplay is false
        if (var != null) {
            // make sure "none" is a choice
            populateShowComboBox(true);
            if (!var.isDisplay()) {
                show.setSelectedIndex(0);
            } else {
                show.setSelectedItem(initialDispPos);
                if (!td.isParam()) {
                    show.setEnabled(false);
                }
            }
        } else {
            populateShowComboBox(false);
            show.setSelectedItem(initialDispPos);
        }

		if (owner instanceof Cell && !td.isInherit())
		{
	        attrInfo1.setText("NOTE: This cell attribute is NOT");
	        attrInfo2.setText("inherited by instances of the cell");
		}

        loading = false;
    }

    /**
     * Method to modify a TextDescriptor to match the settings in this panel.
     * @param td the input TextDescriptor.
     * @return the TextDescriptor with code/units/display in this panel.
     */
    public TextDescriptor withPanelValues(TextDescriptor td)
    {
        // change the units
        TextDescriptor.Unit newUnit = (TextDescriptor.Unit)units.getSelectedItem();
        td = td.withUnit(newUnit);

        // change the show style
        Object newDispObj = show.getSelectedItem();
        if (newDispObj == displaynone)
        {
            td = td.withDisplay(false);
        } else
        {
            td = td.withDisplay(true);
        	TextDescriptor.DispPos newDisp = (TextDescriptor.DispPos)newDispObj;
            td = td.withDispPart(newDisp);
        }
		return td;
    }

    /**
     * Method to modify a variable value to match the Code settings in this panel.
     * @param value the input value
     * @return the value with code in this panel.
     */
    public Object withPanelCode(Object value)
    {
        // change the code type
        CodeExpression.Code newCode = (CodeExpression.Code)code.getSelectedItem();
        return Variable.withCode(value, newCode);
    }

    /**
     * Apply any changes the user has made through the Panel.
     * @return true if any changes committed to database, false otherwise
     */
    public synchronized boolean applyChanges() {
        if (varKey == null) return false;

        boolean changed = false;

        // see if code changed
        CodeExpression.Code newCode = (CodeExpression.Code)code.getSelectedItem();
        if (newCode != initialCode) changed = true;
        // see if units changed
        TextDescriptor.Unit newUnit = (TextDescriptor.Unit)units.getSelectedItem();
        if (newUnit != initialUnit) changed = true;
        // see if show style changed - check if DispPos changed
        Object newDisp = show.getSelectedItem();
        if (newDisp != initialDispPos) changed = true;
        int newDispIndex = -1;
        if (newDisp != displaynone)
        	newDispIndex = ((TextDescriptor.DispPos)newDisp).getIndex();

        if (td != null) {
            // nothing changed on current var/td, return
            if (!changed) return false;
        }

        new ChangeText(
                owner,
                varKey,
                newCode,
                newUnit.getIndex(),
                newDispIndex
        );

        initialCode = newCode;
        initialUnit = newUnit;
        initialDispPos = newDisp;
        return true;
    }

    // populate show combo box. If includeNoneChoice is true, include
    // the "None" option in the combo box. Note that it is a String, while
    // all the other objects are TextDescriptor.DispPos objects.
    private void populateShowComboBox(boolean includeNoneChoice) {
        show.removeAllItems();
        // populate show style dialog box
        if (includeNoneChoice) show.addItem(displaynone);
        for (Iterator<TextDescriptor.DispPos> it = TextDescriptor.DispPos.getShowStyles(); it.hasNext(); ) {
            show.addItem(it.next());
        }

    }

    private static class ChangeText extends Job {

        private ElectricObject owner;
        private Variable.Key varKey;
        private CodeExpression.Code code;
        private int unit;
        private int dispPos;

        private ChangeText(
                ElectricObject owner,
                Variable.Key varKey,
                CodeExpression.Code code,
                int unit,
                int dispPos)
        {
            super("Modify Text Attribute", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.owner = owner;
            this.varKey = varKey;
            this.code = code;
            this.unit = unit;
            this.dispPos = dispPos;
            startJob();
        }

        public boolean doIt() throws JobException {
            // change the code type
            if (owner.isParam(varKey)) {
                if (owner instanceof Cell) {
                    Cell cell = (Cell)owner;
                    cell.getCellGroup().updateParam((Variable.AttrKey)varKey, cell.getParameter(varKey).withCode(code).getObject());
                } else if (owner instanceof NodeInst) {
                    NodeInst ni = (NodeInst)owner;
                    ni.addParameter(ni.getParameter(varKey).withCode(code));
                }
            } else {
                owner.updateVarCode(varKey, code);
            }

			TextDescriptor td = owner.getTextDescriptor(varKey);
			if (td == null) return false;

            // change the units
            td = td.withUnit(TextDescriptor.Unit.getUnitAt(unit));
            // change the show style
			Variable var = owner.getParameterOrVariable(varKey);
            if (dispPos < 0) {
                // var should not be null
                if (var != null) td = td.withDisplay(false);
            } else {
                if (var != null) td = td.withDisplay(true);
                td = td.withDispPart(TextDescriptor.DispPos.getShowStylesAt(dispPos));
            }
			owner.setTextDescriptor(varKey, td);
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

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        code = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        units = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        show = new javax.swing.JComboBox();
        jPanel2 = new javax.swing.JPanel();
        attrInfo1 = new javax.swing.JLabel();
        attrInfo2 = new javax.swing.JLabel();
        attrInfo3 = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("Code:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        jPanel1.add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.1;
        jPanel1.add(code, gridBagConstraints);

        jLabel2.setText("Units:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        jPanel1.add(jLabel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.1;
        jPanel1.add(units, gridBagConstraints);

        jLabel3.setText("Show:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        jPanel1.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.1;
        jPanel1.add(show, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(jPanel1, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        attrInfo1.setText("Attributes created on a cell are");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(attrInfo1, gridBagConstraints);

        attrInfo2.setText("inherited by instances of that cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(attrInfo2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(attrInfo3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 10, 4, 4);
        add(jPanel2, gridBagConstraints);

    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel attrInfo1;
    private javax.swing.JLabel attrInfo2;
    private javax.swing.JLabel attrInfo3;
    private javax.swing.JComboBox code;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JComboBox show;
    private javax.swing.JComboBox units;
    // End of variables declaration//GEN-END:variables
}
