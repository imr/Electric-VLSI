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

import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;

import java.util.Iterator;
import java.awt.*;

/**
 * A Panel to display Code, Units, ShowStyle, and isParameter/isInherits
 * information about a Variable (or TextDescriptor, if passed Variable is null).
 */
public class TextAttributesPanel extends javax.swing.JPanel {

    private static final String notcode = "Not Code";
    private static final String isjava = "Java";
    private static final String istcl = "TCL (not avail.)";
    private static final String islisp = "Lisp (not avail.)";

    private Variable var;
    private TextDescriptor td;
    private ElectricObject owner;
    private TextDescriptor.Unit initialUnit;
    private TextDescriptor.DispPos initialDispPos;
    private String initialCode;
    private boolean initialParamter;
    private boolean initialInherits;

    /**
     * Create a Panel for editing attribute specific
     * text options of a Variable
     */
    public TextAttributesPanel() {
        initComponents();

        // populate code dialog box
        code.addItem(notcode);
        code.addItem(isjava);
        code.addItem(istcl);
        code.addItem(islisp);

        // populate units dialog box
        for (Iterator it = TextDescriptor.Unit.getUnits(); it.hasNext(); ) {
            units.addItem((TextDescriptor.Unit)it.next());
        }

        // populate show style dialog box
        for (Iterator it = TextDescriptor.DispPos.getShowStyles(); it.hasNext(); ) {
            show.addItem((TextDescriptor.DispPos)it.next());
        }

        setVariable(null, null, null);
    }

    /**
     * Set the Variable that can be edited through this Panel
     * @param var the Variable to be edited
     * @param owner the owner of the variable
     */
    public synchronized void setVariable(Variable var, TextDescriptor tdesc, ElectricObject owner) {
        this.var = var;
        this.td = tdesc;
        this.owner = owner;

        boolean enabled = ((var == null) && (td == null)) ? false : true;

        // update enabled state of everything
        // can't just enable all children because objects might be inside JPanel
        code.setEnabled(enabled);
        units.setEnabled(enabled);
        show.setEnabled(enabled);
        parameter.setEnabled(enabled);
        inherited.setEnabled(enabled);

        if (!enabled) return;

        if (var != null) this.td = var.getTextDescriptor(); else td = tdesc;

        // set code
        code.setSelectedItem(notcode); initialCode = notcode;
        if (var != null) {
            if (var.isJava()) { code.setSelectedItem(isjava); initialCode = isjava; }
            if (var.isTCL()) { code.setSelectedItem(istcl); initialCode = istcl; }
            if (var.isLisp()) { code.setSelectedItem(islisp); initialCode = islisp; }
        } else {
            // var null, disable code
            code.setEnabled(false);
        }
        // set units
        initialUnit = td.getUnit();
        units.setSelectedItem(td.getUnit());
        // set show style
        initialDispPos = td.getDispPart();
        show.setSelectedItem(td.getDispPart());
        // set isParam
        initialParamter = td.isParam();
        parameter.setSelected(td.isParam());
        // only vars on Cell and NodeInst can have their parameter property set
        if (!(owner instanceof Cell) && !(owner instanceof NodeInst))
            parameter.setEnabled(false);
        // set isInherits
        initialInherits = td.isInherit();
        inherited.setSelected(td.isInherit());
        // only vars on Cell and Exports can have their inherits property set
        if (!(owner instanceof Cell) && !(owner instanceof Export))
            inherited.setEnabled(false);
    }

    /**
     * Apply any changes the user has made through the Panel.
     * @return true if any changes committed to database, false otherwise
     */
    public synchronized boolean applyChanges() {
        return applyChanges(var, td, owner, null);
    }

    /**
     * This method is used when the Panel's field values will be used to
     * modify a new Variable, i.e. one that has not yet been created.
     * The Job to create the variable must already have been generated before
     * this method is called.
     * @param name the name of the variable that has yet to be created
     * @return true if any changes committed to database, false otherwise.
     */
    public synchronized boolean applyChangesFutureVar(String name) {
        return applyChanges(null, null, owner, name);
    }

    /**
     * This method applies the values in the fields of the Panel to:
     * <p> - var and td on owner if futureVar is null.
     * <p> - a Variable named "futureVar" on owner if futureVar is not null.
     * <p>This allows fields to be applied to a var that will be created by
     * a Job already on the Job queue.
     * @param var the Variable to be modified, instead of the Variable passed to setVariable()
     * @param td the TextDescriptor to be modified, instead of the TextDescriptor passed to setVariable()
     * @param owner the owner of the Variable
     * @param futureVar the name of the variable on 'owner' to be modified
     * @return true if changes made, false otherwise
     */
    private boolean applyChanges(Variable var, TextDescriptor td, ElectricObject owner, String futureVar) {
        if ((var == null) && (td == null) && (futureVar == null)) return false;

        boolean changed = false;

        // see if code changed
        String newCode = (String)code.getSelectedItem();
        if (!newCode.equals(initialCode)) changed = true;
        // see if units changed
        TextDescriptor.Unit newUnit = (TextDescriptor.Unit)units.getSelectedItem();
        if (newUnit != initialUnit) changed = true;
        // see if show style changed
        TextDescriptor.DispPos newDisp = (TextDescriptor.DispPos)show.getSelectedItem();
        if (newDisp != initialDispPos) changed = true;
        // see if is param changed
        boolean newParameter = parameter.isSelected();
        if (newParameter != initialParamter) changed = true;
        // see if is inherits changed
        boolean newInherits = inherited.isSelected();
        if (newInherits != initialInherits) changed = true;

        // nothing changed, return
        if (!changed) return false;

        ChangeText job = new ChangeText(
                owner,
                futureVar,
                newCode,
                newUnit,
                newDisp,
                newParameter,
                newInherits
        );

        initialCode = newCode;
        initialUnit = newUnit;
        initialDispPos = newDisp;
        initialParamter = newParameter;
        initialInherits = newInherits;
        return true;
    }

    private static class ChangeText extends Job {

        private ElectricObject owner;
        private String varName;
        private String code;
        private TextDescriptor.Unit unit;
        private TextDescriptor.DispPos dispPos;
        private boolean parameter;
        private boolean inherits;
        
        private ChangeText(
                ElectricObject owner,
                String varName,
                String code,
                TextDescriptor.Unit unit,
                TextDescriptor.DispPos dispPos,
                boolean parameter,
                boolean inherits
                )
        {
            super("Modify Text Attribute", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.owner = owner;
            this.varName = varName;
            this.code = code;
            this.unit = unit;
            this.dispPos = dispPos;
            this.parameter = parameter;
            this.inherits = inherits;
            startJob();
        }

        public void doIt() {
            Variable var;
            TextDescriptor td;
            if (varName != null) {
                var = owner.getVar(varName);
                if (var == null) return;                // var doesn't exist, abort
                td = var.getTextDescriptor();
            } else return;

            // change the code type
            if (var != null) {
                if (code.equals(notcode)) var.clearCode();
                else if (code.equals(isjava)) var.setJava();
                else if (code.equals(istcl)) var.setTCL();
                else if (code.equals(islisp)) var.setLisp();
            }
            // change the units
            td.setUnit(unit);
            // change the show style
            td.setDispPart(dispPos);
            // change the parameter
            if (parameter) td.setParam(); else td.clearParam();
            // change the inherits
            if (inherits) td.setInherit(); else td.clearInherit();
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        code = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        units = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        show = new javax.swing.JComboBox();
        jPanel2 = new javax.swing.JPanel();
        parameter = new javax.swing.JCheckBox();
        inherited = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        setBorder(new javax.swing.border.EtchedBorder());
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

        parameter.setText("Parameter");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 30, 0, 0);
        jPanel2.add(parameter, gridBagConstraints);

        inherited.setText("Is Inherited");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 30, 0, 0);
        jPanel2.add(inherited, gridBagConstraints);

        jLabel4.setText("Attributes marked as Parameters");
        jPanel2.add(jLabel4, new java.awt.GridBagConstraints());

        jLabel5.setText("can be inherited on instances");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel2.add(jLabel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 10, 4, 4);
        add(jPanel2, gridBagConstraints);

    }//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox code;
    private javax.swing.JCheckBox inherited;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JCheckBox parameter;
    private javax.swing.JComboBox show;
    private javax.swing.JComboBox units;
    // End of variables declaration//GEN-END:variables
    
}
