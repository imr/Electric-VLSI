/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: Attributes2.java
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
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.Main;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import javax.swing.*;


/**
 * Class to handle the "Attributes" dialog.
 */
public class Attributes2 extends EDialog
{
    private static Attributes2 theDialog = null;
    private DefaultListModel listModel;
    private JList list;
    private ElectricObject selectedObject;
    private Cell selectedCell;
    private NodeInst selectedNode;
    private ArcInst selectedArc;
    private Export selectedExport;
    private PortInst selectedPort;
    private JRadioButton currentButton;

    private String initialName;
    private String initialValue;

    private TextAttributesPanel attrPanel;
    private TextInfoPanel textPanel;

    private VariableCellRenderer cellRenderer;

    /**
     * Method to show the Attributes dialog.
     */
    public static void showDialog()
    {
        if (theDialog == null)
        {
            if (TopLevel.isMDIMode()) {
                JFrame jf = TopLevel.getCurrentJFrame();
                theDialog = new Attributes2(jf, false);
            } else {
                theDialog = new Attributes2(null, false);
            }
        }
        theDialog.show();
    }

    /**
     * Method to reload the Attributes dialog from the current highlighting.
     */
    public static void load()
    {
        if (theDialog == null) return;
        theDialog.loadAttributesInfo();
    }

    /**
     * Creates new form Attributes.
     */
    private Attributes2(java.awt.Frame parent, boolean modal)
    {
        super(parent, modal);
        initComponents();

        // make the list
        listModel = new DefaultListModel();
        list = new JList(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cellRenderer = new VariableCellRenderer(!Main.getDebug());
        list.setCellRenderer(cellRenderer);
        listPane.setViewportView(list);
        list.addMouseListener(new java.awt.event.MouseAdapter()
		{
            public void mouseClicked(java.awt.event.MouseEvent evt) { listClick(); }
        });

        // have the radio buttons at the top reevaluate
        currentCell.addActionListener(new ActionListener()
		{
            public void actionPerformed(ActionEvent evt) { objectSelectorActionPerformed(evt); }
        });
        currentNode.addActionListener(new ActionListener()
		{
            public void actionPerformed(ActionEvent evt) { objectSelectorActionPerformed(evt); }
        });
        currentArc.addActionListener(new ActionListener()
		{
            public void actionPerformed(ActionEvent evt) { objectSelectorActionPerformed(evt); }
        });
        currentExport.addActionListener(new ActionListener()
		{
            public void actionPerformed(ActionEvent evt) { objectSelectorActionPerformed(evt); }
        });
        currentPort.addActionListener(new ActionListener()
		{
            public void actionPerformed(ActionEvent evt) { objectSelectorActionPerformed(evt); }
        });

        currentPort.setEnabled(false);

        java.awt.GridBagConstraints gridBagConstraints;
        attrPanel = new TextAttributesPanel();
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(attrPanel, gridBagConstraints);

        textPanel = new TextInfoPanel();
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(textPanel, gridBagConstraints);
        pack();

        loadAttributesInfo();
    }

    /**
     * Set whether Attributes dialog shows attributes only
     * or all variables. Showing all variables is useful for debug.
     * @param b true to show attributes only (default), false to show everything.
     */
    public void setShowAttrOnly(boolean b) {
        cellRenderer.setShowAttrOnly(b);
    }

    /**
     * Method called when the user clicks on one of the top radio buttons.
     * Changes the object being examined for attributes.
     */
    private void objectSelectorActionPerformed(ActionEvent evt)
    {
        currentButton = (JRadioButton)evt.getSource();
        if (currentButton == currentCell) selectedObject = selectedCell;
        else if (currentButton == currentNode) selectedObject = selectedNode;
        else if (currentButton == currentArc) selectedObject = selectedArc;
        else if (currentButton == currentExport) selectedObject = selectedExport;
        else if (currentButton == currentPort) selectedObject = selectedPort;
        updateList();
        checkName();
    }

    /**
     * Method called when the user clicks in the list of attribute names.
     */
    private void listClick()
    {
        showSelectedAttribute(null);
    }

    /**
     * Method to reload the entire dialog from the current highlighting.
     */
    private void loadAttributesInfo()
    {
        // determine what attributes can be set
        selectedObject = null;
        selectedCell = null;
        selectedNode = null;
        selectedArc = null;
        selectedExport = null;
        selectedPort = null;
        Variable selectedVar = null;

        currentButton = currentCell;
        selectedCell = WindowFrame.needCurCell();   selectedObject = selectedCell;
        if (selectedCell != null)
        {
            if (Highlight.getNumHighlights() == 1)
            {
                Highlight high = (Highlight)Highlight.getHighlights().next();
                ElectricObject eobj = high.getElectricObject();
                selectedVar = high.getVar();
                if (high.getType() == Highlight.Type.EOBJ)
                {
                    if (eobj instanceof ArcInst)
                    {
                        selectedArc = (ArcInst)eobj;   selectedObject = selectedArc;   currentButton = currentArc;
                    } else if (eobj instanceof NodeInst)
                    {
                        selectedNode = (NodeInst)eobj;   selectedObject = selectedNode;   currentButton = currentNode;
                    } else if (eobj instanceof PortInst)
                    {
                        PortInst pi = (PortInst)eobj;
                        selectedNode = (NodeInst)pi.getNodeInst();   selectedObject = selectedNode;   currentButton = currentNode;
                        selectedPort = pi;
                    }
                } else if (high.getType() == Highlight.Type.TEXT)
                {
                    if (selectedVar != null)
                    {
                        if (eobj instanceof NodeInst)
                        {
                            selectedNode = (NodeInst)eobj;   selectedObject = selectedNode;   currentButton = currentNode;
                        } else if (eobj instanceof ArcInst)
                        {
                            selectedArc = (ArcInst)eobj;   selectedObject = selectedArc;   currentButton = currentArc;
                        } else if (eobj instanceof PortInst)
                        {
                            selectedPort = (PortInst)eobj;   selectedObject = selectedPort;   currentButton = currentPort;
                            selectedNode = selectedPort.getNodeInst();
                        } else if (eobj instanceof Export)
                        {
                            selectedExport = (Export)eobj;   selectedObject = selectedExport;   currentButton = currentExport;
                        }
                    } else if (high.getName() != null)
                    {
                        // node or arc name
                        if (eobj instanceof NodeInst)
                        {
                            // node name
                            selectedNode = (NodeInst)eobj;   selectedObject = selectedNode;   currentButton = currentNode;
                        } else if (eobj instanceof ArcInst)
                        {
                            // arc variable
                            selectedArc = (ArcInst)eobj;   selectedObject = selectedArc;   currentButton = currentArc;
                        }
                    } else if (eobj instanceof Export)
                    {
                        selectedExport = (Export)eobj;   selectedObject = selectedExport;   currentButton = currentExport;
                    }
                }
            }
        }

        // show initial values in the dialog
        if (selectedCell == null)
        {
            // nothing can be done: dim the entire dialog
            currentCell.setEnabled(false);
            cellName.setText("NO CURRENT CELL");
            currentNode.setEnabled(false);
            currentArc.setEnabled(false);
            currentExport.setEnabled(false);
            currentPort.setEnabled(false);
            list.clearSelection();
            listModel.clear();
            name.setText("");
            name.setEditable(false);
            value.setText("");
            value.setEditable(false);
            evaluation.setText("");
            deleteButton.setEnabled(false);
            newButton.setEnabled(false);
            updateButton.setEnabled(false);
            renameButton.setEnabled(false);
            textPanel.setTextDescriptor(null, null, null);
            attrPanel.setVariable(null, null, null, null);
            return;
        }

        // enable the dialog
        currentCell.setEnabled(true);
        currentNode.setEnabled(selectedNode != null);
        currentArc.setEnabled(selectedArc != null);
        currentExport.setEnabled(selectedExport != null);
        currentPort.setEnabled(selectedPort != null);
        currentButton.setSelected(true);
        cellName.setText(selectedCell.describe());

        name.setEditable(true);
        value.setEditable(true);
        //deleteButton.setEnabled(true);
        //updateButton.setEnabled(true);
        //renameButton.setEnabled(true);
        //newButton.setEnabled(true);

        // show all attributes on the selected object
        updateList();
        if (selectedVar != null)
            showSelectedAttribute(selectedVar);
        else
            checkName();

    }

    /**
     * Class to sort Variables by name.
     */
    static class VariableNameSort implements Comparator
	{
        public int compare(Object o1, Object o2)
        {
            Variable v1 = (Variable)o1;
            Variable v2 = (Variable)o2;
            String s1 = v1.getKey().getName();
            String s2 = v2.getKey().getName();
            return s1.compareToIgnoreCase(s2);
        }
    }

    private void checkName() {
        // if name does not equal name of selected var, disable update button
        String varName = name.getText().trim();

        // can't create new variable of empty, no vars can be empty text
        if (varName.equals("")) {
            updateButton.setEnabled(false);
            newButton.setEnabled(false);
            deleteButton.setEnabled(false);
            renameButton.setEnabled(false);
            textPanel.setTextDescriptor(null, null, null);
            attrPanel.setVariable(null, null, null, null);
            return;
        }

        if (cellRenderer.getShowAttrOnly())
            varName = "ATTR_" + varName;

        // try to find variable
        Variable var = selectedObject.getVar(varName);
        if (var != null) {
            // make sure var is selected
            showSelectedAttribute(var);
        } else {
            // no such var, remove selection and enable new buttons
            newButton.setEnabled(true);
            updateButton.setEnabled(false);
            renameButton.setEnabled(false);
            deleteButton.setEnabled(false);
            list.clearSelection();
            textPanel.setTextDescriptor(null, varName, selectedObject);
            attrPanel.setVariable(null, null, varName, selectedObject);
        }
    }

    /**
     * Updates List of attributes on selected object
     */
    private void updateList()
    {
        list.clearSelection();
        listModel.clear();

        // make a sorted list of variable names
        List variables = new ArrayList();
        for(Iterator it = selectedObject.getVariables(); it.hasNext(); )
        {
            Variable var = (Variable)it.next();
            if (cellRenderer.getShowAttrOnly()) {
                if (!var.isDisplay()) continue;
            }
            variables.add(var);
        }
        Collections.sort(variables, new VariableNameSort());

        // show the variables
        for(Iterator it = variables.iterator(); it.hasNext(); )
        {
            Variable var = (Variable)it.next();
            String varName = var.getKey().getName();
            if (cellRenderer.getShowAttrOnly()) {
                // if only showing Attributes, only add if it is an attribute
                if (varName.startsWith("ATTR_")) {
                    listModel.addElement(var);
                }
                continue;
            } else {
                listModel.addElement(var);
            }
        }
    }

    /**
     * Method to return the Variable that is selected in the dialog.
     * @return the Variable that is selected.  Returns null if none are.
     */
    Variable getSelectedVariable()
    {
        int i = list.getSelectedIndex();
        if (i < 0) return null;
        return (Variable)list.getSelectedValue();
    }

    /**
     * Method to convert a string to a proper Object for storage in a Variable.
     * Currently knows only Integer, Double, and String.
     * @param text the text describing the Variable's value.
     * @return an Object that contains the value.
     */
    private Object getVariableObject(String text)
    {
        if (TextUtils.isANumber(text))
        {
            int i = TextUtils.atoi(text);
            double d = TextUtils.atof(text);
            if (i == d) return new Integer(i);
            return new Double(d);
        }
        return text;
    }

    /**
     * Method to display the aspects of the currently selected Variable in the dialog.
     * @param selectThis if non-null, select this variable first, and then update dialog.
     */
    void showSelectedAttribute(Variable selectThis)
    {
        if (selectThis != null)
            list.setSelectedValue(selectThis, true);

        Variable var = getSelectedVariable();
        if (var == null) return;

        // set the Name field
        initialName = cellRenderer.getVariableText(var);
        String pt = initialName;
        name.setText(pt);

        // get initial the Value field
        initialValue = var.getPureValue(-1, 0);

        // set the Value field
        value.setText(initialValue);
        if (var.getObject() instanceof Object []) {
            value.setEditable(false);
        } else {
            value.setEditable(true);
        }

        // set the evaluation field
        if (var.isCode()) {
            Object eval = VarContext.globalContext.evalVar(var);
            if (eval == null)
                evaluation.setText("");
            else
                evaluation.setText(eval.toString());
        } else {
            evaluation.setText("");
        }

        // set the text info panel
        textPanel.setTextDescriptor(var.getTextDescriptor(), null, selectedObject);
        attrPanel.setVariable(var, var.getTextDescriptor(), null, selectedObject);

        // disable create button because var name already exists, enable selected: buttons
        newButton.setEnabled(false);
        updateButton.setEnabled(true);
        renameButton.setEnabled(true);
        deleteButton.setEnabled(true);
    }

    /**
     * Class to delete an attribute in a new thread.
     */
    private static class DeleteAttribute extends Job
	{
        Variable var;
        ElectricObject owner;

        private DeleteAttribute(Variable var, ElectricObject owner)
        {
            super("Delete Attribute", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.var = var;
            this.owner = owner;
            startJob();
        }

        public void doIt()
        {
            if (var == null) return;
            owner.delVar(var.getKey());
            // update the attributes dialog
            Attributes2.load();
        }
    }

    private static class CreateAttribute extends Job {

        private String newName;
        private Object newValue;
        private ElectricObject owner;

        private CreateAttribute(String newName, Object newValue, ElectricObject owner) {
            super("Create Attribute", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.newName = newName;
            this.newValue = newValue;
            this.owner = owner;
            startJob();
        }

        public void doIt() {
            // check if var of this name already exists on object
            if (owner.getVar(newName) != null) {
                JOptionPane.showMessageDialog(null, "Can't create new attribute "+newName+", already exists",
                        "Invalid Action", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // create the attribute
            owner.newVar(newName, newValue);
        }
    }

    /**
     * Job to trigger update to Attributes dialog.  Type set to CHANGE and priority to USER
     * so that in queues in order behind other Jobs from this class: this assures it will
     * occur after the queued changes
     */
    private static class UpdateDialog extends Job {
        private UpdateDialog() {
            super("Update Attributes Dialog", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            startJob();
        }
        public void doIt() {
            Attributes2.load();
        }
    }

    protected static class RenameAttribute extends Job
    {
        String varName;
        String newVarName;
        ElectricObject owner;

        protected RenameAttribute(String varName, String newVarName, ElectricObject owner)
        {
            super("Rename Attribute", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.varName = varName;
            this.newVarName = newVarName;
            this.owner = owner;
            startJob();
        }

        public void doIt()
        {
            Variable var = owner.renameVar(varName, newVarName);
            if (var == null)
                System.out.println("Rename of variable failed");
        }

    }

    /**
     * Class to create or modify an attribute in a new thread.
     */
    protected static class ChangeAttribute extends Job
	{
        String varName;
        ElectricObject owner;
        Object newValue;

        protected ChangeAttribute(String varName, ElectricObject owner, Object newValue)
        {
            super("Change Attribute", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.varName = varName;
            this.owner = owner;
            this.newValue = newValue;
            startJob();
        }

        public void doIt()
        {
            // get Variable by name
            Variable var = owner.getVar(varName);
            if (var == null) {
                System.out.println("Could not update Attribute "+varName+": it does not exist");
                return;
            }

            // change the Value field if a new Variable is being created or if the value changed
            var = owner.updateVar(varName, newValue);
            if (var == null) {
                System.out.println("Error updating Attribute "+varName);
                return;
            }

            // queue it for redraw
            Undo.redrawObject(owner);
        }
    }

    /**
     * Used to display Variables in the JList
     */
    private static class VariableCellRenderer extends JLabel implements ListCellRenderer {

        private boolean showAttrOnly;

        private VariableCellRenderer(boolean showAttrOnly) {
            this.showAttrOnly = showAttrOnly;
        }

        private void setShowAttrOnly(boolean b) { showAttrOnly = b; }

        private boolean getShowAttrOnly() { return showAttrOnly; }

        private String getVariableText(Variable var) {
            String varName = var.getKey().getName();

            // two modes: show attributes only, and show everything
            if (showAttrOnly) {
                if (varName.startsWith("ATTR_")) {
                    return varName.substring(5);
                }
            }
            // else this is not an attribute
            // see if any cell, node, or arc variables are available to the user
            String betterName = Variable.betterVariableName(varName);
            if (betterName != null)
                return betterName;
            else
                return varName;
        }

        public Component getListCellRendererComponent(
                JList list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            if (!(value instanceof Variable)) {
                // this is not a variable
                setText(value.toString());
            } else {
                // this is a variable
                Variable var = (Variable)value;
                setText(getVariableText(var));
            }

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setEnabled(list.isEnabled());
            setFont(list.getFont());
            setOpaque(true);
            return this;
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        which = new javax.swing.ButtonGroup();
        corner = new javax.swing.ButtonGroup();
        size = new javax.swing.ButtonGroup();
        ok = new javax.swing.JButton();
        currentCell = new javax.swing.JRadioButton();
        currentNode = new javax.swing.JRadioButton();
        currentExport = new javax.swing.JRadioButton();
        currentPort = new javax.swing.JRadioButton();
        currentArc = new javax.swing.JRadioButton();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel1 = new javax.swing.JLabel();
        body = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        listPane = new javax.swing.JScrollPane();
        jLabel2 = new javax.swing.JLabel();
        name = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        value = new javax.swing.JTextField();
        evaluation = new javax.swing.JLabel();
        evalLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        newButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        updateButton = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();
        renameButton = new javax.swing.JButton();
        cellName = new javax.swing.JLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Edit Attributes");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        ok.setText("Done");
        ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ok(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        currentCell.setText("On Current Cell:");
        which.add(currentCell);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        getContentPane().add(currentCell, gridBagConstraints);

        currentNode.setText(" Node");
        which.add(currentNode);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        getContentPane().add(currentNode, gridBagConstraints);

        currentExport.setText(" Export");
        which.add(currentExport);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        getContentPane().add(currentExport, gridBagConstraints);

        currentPort.setText("Port on Node");
        which.add(currentPort);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        getContentPane().add(currentPort, gridBagConstraints);

        currentArc.setText(" Arc");
        which.add(currentArc);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        getContentPane().add(currentArc, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(jSeparator1, gridBagConstraints);

        jLabel1.setText("or Highlighted Object:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jLabel1, gridBagConstraints);

        body.setLayout(new java.awt.GridBagLayout());

        jLabel10.setText("Attributes:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        body.add(jLabel10, gridBagConstraints);

        listPane.setMinimumSize(new java.awt.Dimension(400, 100));
        listPane.setPreferredSize(new java.awt.Dimension(400, 100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        body.add(listPane, gridBagConstraints);

        jLabel2.setText("Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(jLabel2, gridBagConstraints);

        name.setText(" ");
        name.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                nameKeyReleased(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(name, gridBagConstraints);

        jLabel11.setText("Value:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(jLabel11, gridBagConstraints);

        value.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(value, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        body.add(evaluation, gridBagConstraints);

        evalLabel.setText("Evaluation:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(evalLabel, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        newButton.setText("Create New");
        newButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        jPanel1.add(newButton, gridBagConstraints);

        jLabel3.setText("Selected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 12, 4, 4);
        jPanel1.add(jLabel3, gridBagConstraints);

        updateButton.setText("Update");
        updateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        jPanel1.add(updateButton, gridBagConstraints);

        deleteButton.setText("Delete");
        deleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        jPanel1.add(deleteButton, gridBagConstraints);

        renameButton.setText("Rename");
        renameButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                renameButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(renameButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        body.add(jPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(body, gridBagConstraints);

        cellName.setText("clock{sch}");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(cellName, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

    private void renameButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_renameButtonActionPerformed
        Object ret = JOptionPane.showInputDialog(this, "New name for "+name.getText(),
                "Rename Attribute", JOptionPane.QUESTION_MESSAGE, null, null, name.getText());
        String newName = (String)ret;
        newName = newName.trim();
        if (newName.equals("")) {
            JOptionPane.showMessageDialog(this, "Attribute name must not be empty",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // if same name, ignore
        if (newName.equals(name.getText())) return;

        if (cellRenderer.getShowAttrOnly())
            newName = "ATTR_" + newName;

        // check if variable name already exists
        Variable var = selectedObject.getVar(newName);
        if (var != null) {
            JOptionPane.showMessageDialog(this, "Attribute of that name already exists",
                    "No Action Taken", JOptionPane.ERROR_MESSAGE);
            return;
        }

        RenameAttribute job = new RenameAttribute(getSelectedVariable().getKey().getName(), newName, selectedObject);

        if (cellRenderer.getShowAttrOnly())
            newName = newName.substring(5);

        // set current name to renamed name
        initialName = newName;
        name.setText(newName);

    }//GEN-LAST:event_renameButtonActionPerformed

    private void nameKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_nameKeyReleased
        checkName();
    }//GEN-LAST:event_nameKeyReleased

    private void ok(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ok
        closeDialog(null);
    }//GEN-LAST:event_ok

    private void newButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newButtonActionPerformed

        // check variable name
        String varName = name.getText().trim();
        if (varName.trim().length() == 0) {
            JOptionPane.showMessageDialog(null, "Attribute name must not be empty",
                    "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (cellRenderer.getShowAttrOnly())
            varName = "ATTR_" + varName;

        // check if var of this name already exists on object
        if (selectedObject.getVar(varName) != null) {
            JOptionPane.showMessageDialog(null, "Can't create new attribute "+varName+", already exists",
                    "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // get value
        String val = value.getText().trim();

        // Spawn a Job to create the Variable
        CreateAttribute job = new CreateAttribute(varName, getVariableObject(val), selectedObject);
        // Spawn a Job to set the new Variable's text options
        // because the var has not been created yet, set the futureVarName for the panel
        //textPanel.setTextDescriptor(null, varName, selectedObject);
        textPanel.applyChanges();
        // same for text attributes panel
        //attrPanel.setVariable(null, null, varName, selectedObject);
        attrPanel.applyChanges();
        // generate Job to update this dialog when the changes have been processed
        //UpdateDialog job2 = new UpdateDialog();

        initialName = varName;
        initialValue = val;
    }//GEN-LAST:event_newButtonActionPerformed

    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_deleteButtonActionPerformed
    {//GEN-HEADEREND:event_deleteButtonActionPerformed
        // delete the attribute
        DeleteAttribute job = new DeleteAttribute(getSelectedVariable(), selectedObject);
    }//GEN-LAST:event_deleteButtonActionPerformed

    private void updateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateButtonActionPerformed

        boolean changed = false;

        // Name will be correct, or update button will be disabled
        Variable selectedVar = getSelectedVariable();
        String varName = selectedVar.getKey().getName();

        // see if value changed
        String varValue = value.getText().trim();
        if (!varValue.equals(initialValue)) changed = true;

        if (changed) {
            // generate Job to update value
            ChangeAttribute job = new ChangeAttribute(varName, selectedObject, getVariableObject(varValue));
            initialValue = varValue;
        }
        // update text options and attribute options (will check for changes)
        textPanel.applyChanges();
        attrPanel.applyChanges();
        // generate Job to update this dialog once changes have completed
        //UpdateDialog job2 = new UpdateDialog();

    }//GEN-LAST:event_updateButtonActionPerformed

    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
    {
        setVisible(false);
//		dispose();
    }//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel body;
    private javax.swing.JLabel cellName;
    private javax.swing.ButtonGroup corner;
    private javax.swing.JRadioButton currentArc;
    private javax.swing.JRadioButton currentCell;
    private javax.swing.JRadioButton currentExport;
    private javax.swing.JRadioButton currentNode;
    private javax.swing.JRadioButton currentPort;
    private javax.swing.JButton deleteButton;
    private javax.swing.JLabel evalLabel;
    private javax.swing.JLabel evaluation;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JScrollPane listPane;
    private javax.swing.JTextField name;
    private javax.swing.JButton newButton;
    private javax.swing.JButton ok;
    private javax.swing.JButton renameButton;
    private javax.swing.ButtonGroup size;
    private javax.swing.JButton updateButton;
    private javax.swing.JTextField value;
    private javax.swing.ButtonGroup which;
    // End of variables declaration//GEN-END:variables

}
