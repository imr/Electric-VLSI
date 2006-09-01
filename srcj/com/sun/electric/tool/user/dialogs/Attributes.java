/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Attributes.java
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

import com.sun.electric.database.change.DatabaseChangeEvent;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.Highlight2;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.UserInterfaceMain;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 * Class to handle the "Attributes" dialog.
 */
public class Attributes extends EDialog implements HighlightListener, DatabaseChangeListener
{
    private static Attributes theDialog = null;
    private DefaultListModel listModel;
    private JList list;
    private ElectricObject selectedObject;
    private Cell selectedCell;
    private NodeInst selectedNode;
    private ArcInst selectedArc;
    private Export selectedExport;
    private PortInst selectedPort;
    private Variable.Key selectedVarKey;
    private JRadioButton currentButton;

    private String initialName;
    private String initialValue;
	private boolean showAttrsOnly = !Job.getDebug();

    private TextAttributesPanel attrPanel;
    private TextInfoPanel textPanel;

    private VariableCellRenderer cellRenderer;
    private boolean loading = false;

    /**
     * Method to show the Attributes dialog.
     */
    public static void showDialog()
    {
        if (theDialog == null)
        {
            if (TopLevel.isMDIMode()) {
                JFrame jf = TopLevel.getCurrentJFrame();
                theDialog = new Attributes(jf, false);
            } else {
                theDialog = new Attributes(null, false);
            }
        }
        theDialog.loadAttributesInfo(false);
        if (!theDialog.isVisible()) theDialog.pack();
		theDialog.setVisible(true);
   }

    /**
     * Reloads the dialog when Highlights change
     */
    public void highlightChanged(Highlighter which)
    {
        if (!isVisible()) return;
        loadAttributesInfo(false);
    }

    /**
     * Called when by a Highlighter when it loses focus. The argument
     * is the Highlighter that has gained focus (may be null).
     * @param highlighterGainedFocus the highlighter for the current window (may be null).
     */
    public void highlighterLostFocus(Highlighter highlighterGainedFocus) {
        if (!isVisible()) return;
        loadAttributesInfo(false);        
    }

    /**
     * Reload if the database has changed in a way we care about
     * @param e database change event
     */
    public void databaseChanged(DatabaseChangeEvent e) {
        if (!isVisible()) return;

		// update dialog
		if (e.objectChanged(selectedObject))
		{
            loadAttributesInfo(true);
		}
    }

    /**
     * Creates new form Attributes.
     */
    private Attributes(Frame parent, boolean modal)
    {
        super(parent, modal);
        initComponents();

        UserInterfaceMain.addDatabaseChangeListener(this);
        Highlighter.addHighlightListener(this);

        // make the list
        listModel = new DefaultListModel();
        list = new JList(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cellRenderer = new VariableCellRenderer();
        list.setCellRenderer(cellRenderer);
        listPane.setViewportView(list);
        list.addMouseListener(new MouseAdapter()
		{
            public void mouseClicked(MouseEvent evt) { listClick(); }
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
        attrPanel = new TextAttributesPanel(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(attrPanel, gridBagConstraints);

        textPanel = new TextInfoPanel(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(textPanel, gridBagConstraints);
        pack();

        loadAttributesInfo(false);

        name.getDocument().addDocumentListener(new TextInfoDocumentListener(this));
        value.getDocument().addDocumentListener(new TextInfoDocumentListener(this));

        finishInitialization();
    }

	protected void escapePressed() { done(null); }

	/**
	 * Class to handle special changes to changes to a Attributes edit fields.
	 */
	private static class TextInfoDocumentListener implements DocumentListener
	{
		Attributes dialog;

		TextInfoDocumentListener(Attributes dialog) { this.dialog = dialog; }

		public void changedUpdate(DocumentEvent e) { dialog.fieldChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.fieldChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.fieldChanged(); }
	}

	private void fieldChanged()
	{
		if (loading) return;

        Variable var = getSelectedVariable();
        if (var == null) return;

        // see if value changed
        String varValue = value.getText().trim();
        if (!varValue.equals(initialValue))
        {
            // generate Job to update value
            new ChangeAttribute(var.getKey(), selectedObject, getVariableObject(varValue));
            initialValue = varValue;
        }
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
    private void loadAttributesInfo(boolean keepObj)
    {
    	loading = true;

    	if (!keepObj)
    	{
	        // determine what attributes can be set
	        selectedObject = null;
	        selectedCell = null;
	        selectedNode = null;
	        selectedArc = null;
	        selectedExport = null;
	        selectedPort = null;
	        selectedVarKey = null;
	
	        currentButton = currentCell;
	
	        // update current window
	        EditWindow curWnd = EditWindow.getCurrent();	
	        selectedCell = WindowFrame.needCurCell();   selectedObject = selectedCell;
	        if (curWnd == null) selectedCell = null;
	        if (selectedCell != null)
	        {
	            if (curWnd.getHighlighter().getNumHighlights() == 1)
	            {
	                Highlight2 high = curWnd.getHighlighter().getHighlights().iterator().next();
	                ElectricObject eobj = high.getElectricObject();
	                if (high.isHighlightEOBJ())
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
	                } else if (high.isHighlightText())
	                {
	                	selectedVarKey = high.getVarKey();
	                    if (selectedVarKey != null)
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
	                    } else if (high.getVarKey() == NodeInst.NODE_NAME || high.getVarKey() == NodeInst.NODE_PROTO)
	                    {
                            // node name
                            selectedNode = (NodeInst)eobj;   selectedObject = selectedNode;   currentButton = currentNode;
	                    } else if (high.getVarKey() == ArcInst.ARC_NAME)
	                    {
                            // arc name
                            selectedArc = (ArcInst)eobj;   selectedObject = selectedArc;   currentButton = currentArc;
	                    } else if (high.getVarKey() == Export.EXPORT_NAME)
	                    {
	                        selectedExport = (Export)eobj;   selectedObject = selectedExport;   currentButton = currentExport;
	                    }
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
            renameButton.setEnabled(false);
            textPanel.setTextDescriptor(null, null);
            attrPanel.setVariable(null, null);
        	loading = false;
            return;
        }

        // enable the dialog
        currentCell.setEnabled(true);
        currentNode.setEnabled(selectedNode != null);
        currentArc.setEnabled(selectedArc != null);
        currentExport.setEnabled(selectedExport != null);
        currentPort.setEnabled(selectedPort != null);
        currentButton.setSelected(true);
        cellName.setText(selectedCell.describe(false));

        name.setEditable(true);
        value.setEditable(true);

        // show all attributes on the selected object
        updateList();
        if (selectedVarKey != null)
            showSelectedAttribute(selectedVarKey);
        else
            checkName();
    	loading = false;
    }

    private void checkName() {
        // if name does not equal name of selected var, disable update button
        String varName = name.getText().trim();

        // can't create new variable of empty, no vars can be empty text
        if (varName.equals("")) {
            newButton.setEnabled(false);
            deleteButton.setEnabled(false);
            renameButton.setEnabled(false);
            textPanel.setTextDescriptor(null, null);
            attrPanel.setVariable(null, null);
            return;
        }

        if (showAttrsOnly)
            varName = "ATTR_" + varName;

        // try to find variable
        Variable.Key varKey = Variable.newKey(varName);
        if (selectedObject.getVar(varKey) != null)
        {
            // make sure var is selected
            if (varKey != null)
            list.setSelectedValue(varKey, true);
        } else {
            // no such var, remove selection and enable new buttons
            newButton.setEnabled(true);
            renameButton.setEnabled(false);
            deleteButton.setEnabled(false);
            list.clearSelection();
            textPanel.setTextDescriptor(varKey, selectedObject);
            attrPanel.setVariable(varKey, selectedObject);
        }
    }

    /**
     * Updates List of attributes on selected object
     */
    private void updateList()
    {
        list.clearSelection();
        listModel.clear();

        // show the variables
        for(Iterator<Variable> it = selectedObject.getVariables(); it.hasNext(); )
        {
            Variable var = it.next();
            String varName = var.getKey().getName();
            if (showAttrsOnly) {
                // if only showing Attributes, only add if it is an attribute
                if (varName.startsWith("ATTR_")) {
                    listModel.addElement(var.getKey());
                }
                continue;
            } else {
                listModel.addElement(var.getKey());
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
        Variable.Key key = (Variable.Key)list.getSelectedValue();
        return selectedObject.getVar(key);
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
     * @param selectThisKey if non-null, select this variable first, and then update dialog.
     */
    void showSelectedAttribute(Variable.Key selectThisKey)
    {
        if (selectThisKey != null)
            list.setSelectedValue(selectThisKey, true);

        Variable var = getSelectedVariable();
        if (var == null) return;
        selectedVarKey = var.getKey();

        // set the Name field
		boolean oldLoading = loading;
		loading = true;
        initialName = getVariableText(var.getKey());
        String pt = initialName;
        name.setText(pt);

        // get initial the Value field
        initialValue = var.getPureValue(-1);

        // set the Value field
		String oldValue = value.getText();
		if (!oldValue.equals(initialValue))
			value.setText(initialValue);
        if (var.getObject() instanceof Object []) {
            value.setEditable(false);
        } else {
            value.setEditable(true);
        }
		loading = oldLoading;

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
		if (var.isDisplay())
			textPanel.setTextDescriptor(var.getKey(), selectedObject);
		else
			textPanel.setTextDescriptor(null, null);
        attrPanel.setVariable(var.getKey(), selectedObject);

        // disable create button because var name already exists, enable selected: buttons
        newButton.setEnabled(false);
        renameButton.setEnabled(true);
        deleteButton.setEnabled(true);
    }

    /**
     * Class to delete an attribute in a new thread.
     */
    private static class DeleteAttribute extends Job
	{
        private Variable var;
        private ElectricObject owner;

        private DeleteAttribute(Variable var, ElectricObject owner)
        {
            super("Delete Attribute", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.var = var;
            this.owner = owner;
            startJob();
        }

        public boolean doIt() throws JobException
        {
            if (var == null) return false;
            owner.delVar(var.getKey());
            return true;
        }
    }

    private static class CreateAttribute extends Job {

        private String newName;
        private Object newValue;
        private ElectricObject owner;

        private CreateAttribute(String newName, Object newValue, ElectricObject owner) {
            super("Create Attribute", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.newName = newName;
            this.newValue = newValue;
            this.owner = owner;
            startJob();
        }

        public boolean doIt() throws JobException
        {
            Variable.Key newKey = Variable.newKey(newName);

            // check if var of this name already exists on object
            if (owner.getVar(newKey) != null)
				throw new JobException("Can't create new attribute "+newName+", already exists");

            // create the attribute
            Variable var = owner.newVar(newKey, newValue);

            // if created on a cell, set the parameter and inherits properties
            if (owner instanceof Cell)
                owner.addVar(var.withParam(true).withInherit(true));
            return true;
        }
    }

    private static class RenameAttribute extends Job
    {
        private String varName;
        private String newVarName;
        private ElectricObject owner;

        protected RenameAttribute(String varName, String newVarName, ElectricObject owner)
        {
            super("Rename Attribute", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.varName = varName;
            this.newVarName = newVarName;
            this.owner = owner;
            startJob();
        }

        public boolean doIt() throws JobException
        {
            Variable var = owner.renameVar(varName, newVarName);
            if (var == null)
            {
                System.out.println("Rename of variable failed");
                return false;
            }
            return true;
        }
    }

    /**
     * Class to create or modify an attribute in a new thread.
     */
    private static class ChangeAttribute extends Job
	{
        private Variable.Key varKey;
        private ElectricObject owner;
        private Object newValue;

        protected ChangeAttribute(Variable.Key varKey, ElectricObject owner, Object newValue)
        {
            super("Change Attribute", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.varKey = varKey;
            this.owner = owner;
            this.newValue = newValue;
            startJob();
        }

        public boolean doIt() throws JobException
        {
            // get Variable by name
            Variable var = owner.getVar(varKey);
            if (var == null)
				throw new JobException("Could not update Attribute " + varKey + ": it does not exist");

            // change the Value field if a new Variable is being created or if the value changed
            var = owner.updateVar(varKey, newValue);
            if (var == null)
				throw new JobException("Error updating Attribute " + varKey);
            return true;
        }
    }

	/**
	 * Used to display Variables in the JList
	 */
	private class VariableCellRenderer extends JLabel implements ListCellRenderer {

		private VariableCellRenderer() { }

		public Component getListCellRendererComponent(JList list, Object value, int index,
			boolean isSelected, boolean cellHasFocus)
		{
			if (value instanceof Variable.Key)
			{
				setText(getVariableText((Variable.Key)value));
			} else if (value instanceof Variable)
			{
				setText(getVariableText(((Variable)value).getKey()));
			} else
			{
				setText(value.toString());
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

	private String getVariableText(Variable.Key varKey)
	{
		String varName = varKey.getName();

		// two modes: show attributes only, and show everything
		if (showAttrsOnly)
		{
			if (varName.startsWith("ATTR_"))
				return varName.substring(5);
		}

		// else this is not an attribute
		// see if any cell, node, or arc variables are available to the user
		String betterName = Variable.betterVariableName(varName);
		if (betterName != null) return betterName;
		return varName;
	}

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        which = new javax.swing.ButtonGroup();
        corner = new javax.swing.ButtonGroup();
        size = new javax.swing.ButtonGroup();
        currentCell = new javax.swing.JRadioButton();
        currentNode = new javax.swing.JRadioButton();
        currentExport = new javax.swing.JRadioButton();
        currentPort = new javax.swing.JRadioButton();
        currentArc = new javax.swing.JRadioButton();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel1 = new javax.swing.JLabel();
        cellName = new javax.swing.JLabel();
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
        deleteButton = new javax.swing.JButton();
        renameButton = new javax.swing.JButton();
        done = new javax.swing.JButton();
        editValue = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Edit Attributes");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        which.add(currentCell);
        currentCell.setText("On Current Cell:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        getContentPane().add(currentCell, gridBagConstraints);

        which.add(currentNode);
        currentNode.setText(" Node");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        getContentPane().add(currentNode, gridBagConstraints);

        which.add(currentExport);
        currentExport.setText(" Export");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        getContentPane().add(currentExport, gridBagConstraints);

        which.add(currentPort);
        currentPort.setText("Port on Node");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        getContentPane().add(currentPort, gridBagConstraints);

        which.add(currentArc);
        currentArc.setText(" Arc");
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

        cellName.setText("clock{sch}");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(cellName, gridBagConstraints);

        body.setLayout(new java.awt.GridBagLayout());

        jLabel10.setText("Attributes:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        body.add(jLabel10, gridBagConstraints);

        listPane.setMinimumSize(new java.awt.Dimension(400, 100));
        listPane.setPreferredSize(new java.awt.Dimension(400, 100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
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
        name.addKeyListener(new java.awt.event.KeyAdapter()
        {
            public void keyReleased(java.awt.event.KeyEvent evt)
            {
                nameKeyReleased(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
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
        gridBagConstraints.gridwidth = 2;
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
        newButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                newButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(newButton, gridBagConstraints);

        deleteButton.setText("Delete");
        deleteButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                deleteButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(deleteButton, gridBagConstraints);

        renameButton.setText("Rename...");
        renameButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                renameButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(renameButton, gridBagConstraints);

        done.setText("Done");
        done.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                done(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(done, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        body.add(jPanel1, gridBagConstraints);

        editValue.setText("Edit...");
        editValue.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                editValueActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(editValue, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(body, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void editValueActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_editValueActionPerformed
	{//GEN-HEADEREND:event_editValueActionPerformed
		String editedValue = JOptionPane.showInputDialog("New Value:", value.getText());
		if (editedValue == null) return;
		value.setText(editedValue);
	}//GEN-LAST:event_editValueActionPerformed

    private void renameButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_renameButtonActionPerformed
    	String newName = (String)JOptionPane.showInputDialog(this, "New name for " + name.getText(),
            "Rename Attribute", JOptionPane.QUESTION_MESSAGE, null, null, name.getText());
        if (newName == null) return;
        newName = newName.trim();
        if (newName.equals(""))
        {
            JOptionPane.showMessageDialog(this, "Attribute name must not be empty",
                "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // if same name, ignore
        if (newName.equals(name.getText())) return;

        if (showAttrsOnly)
            newName = "ATTR_" + newName;

        // check if variable name already exists
        Variable var = selectedObject.getVar(newName);
        if (var != null) {
            JOptionPane.showMessageDialog(this, "Attribute of that name already exists",
                    "No Action Taken", JOptionPane.ERROR_MESSAGE);
            return;
        }

        RenameAttribute job = new RenameAttribute(getSelectedVariable().getKey().getName(), newName, selectedObject);

        if (showAttrsOnly)
            newName = newName.substring(5);

        // set current name to renamed name
        initialName = newName;
        name.setText(newName);

    }//GEN-LAST:event_renameButtonActionPerformed

    private void nameKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_nameKeyReleased
        checkName();
    }//GEN-LAST:event_nameKeyReleased

	private void done(java.awt.event.ActionEvent evt)//GEN-FIRST:event_done
	{//GEN-HEADEREND:event_done
        closeDialog(null);
	}//GEN-LAST:event_done

    private void newButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newButtonActionPerformed

        // check variable name
        String varName = name.getText().trim();
        if (varName.trim().length() == 0) {
            JOptionPane.showMessageDialog(null, "Attribute name must not be empty",
                    "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (showAttrsOnly)
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
        new CreateAttribute(varName, getVariableObject(val), selectedObject);

        // Spawn a Job to set the new Variable's text options
        // because the var has not been created yet, set the futureVarName for the panel
        textPanel.applyChanges(true);

        // same for text attributes panel
        attrPanel.applyChanges();

        initialName = varName;
        initialValue = val;
    }//GEN-LAST:event_newButtonActionPerformed

    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_deleteButtonActionPerformed
    {//GEN-HEADEREND:event_deleteButtonActionPerformed
        // delete the attribute
        new DeleteAttribute(getSelectedVariable(), selectedObject);
    }//GEN-LAST:event_deleteButtonActionPerformed

    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
    {
        setVisible(false);
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
    private javax.swing.JButton done;
    private javax.swing.JButton editValue;
    private javax.swing.JLabel evalLabel;
    private javax.swing.JLabel evaluation;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JScrollPane listPane;
    private javax.swing.JTextField name;
    private javax.swing.JButton newButton;
    private javax.swing.JButton renameButton;
    private javax.swing.ButtonGroup size;
    private javax.swing.JTextField value;
    private javax.swing.ButtonGroup which;
    // End of variables declaration//GEN-END:variables

}
