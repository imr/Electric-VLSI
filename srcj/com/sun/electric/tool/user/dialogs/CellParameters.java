/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellParameters.java
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

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;


/**
 * Class to handle the "Cell Parameters" dialog.
 */
public class CellParameters extends javax.swing.JDialog
{
	private DefaultListModel paramListModel;
	private JList paramJList;
	private Cell curCell;

	/** Creates new form Cell Parameters */
	public CellParameters(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		setLocation(100, 50);
		initComponents();

		// build the parameter list
		paramListModel = new DefaultListModel();
		paramJList = new JList(paramListModel);
		paramJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		paramPane.setViewportView(paramJList);
		paramJList.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(java.awt.event.MouseEvent evt) { paramListClick(); }
		});

		language.addItem("Not Code");
		language.addItem("TCL (not available)");
		language.addItem("LISP (not available)");
		language.addItem("Java");

		units.addItem("None");
		units.addItem("Resistance");
		units.addItem("Capacitance");
		units.addItem("Inductance");
		units.addItem("Current");
		units.addItem("Voltage");
		units.addItem("Distance");
		units.addItem("Time");

		curCell = Library.getCurrent().getCurCell();
		if (curCell == null) return;

		reloadParamList();
	}

	private void reloadParamList()
	{
		paramListModel.clear();
		for(Iterator it = curCell.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.getTextDescriptor().isParam()) continue;
			String msg = var.getTrueName() + " (default: " + var.getPureValue(-1, -1) + ")";
			paramListModel.addElement(msg);
		}
	}

	private Variable getSelectedParameter()
	{
		String line = (String)paramJList.getSelectedValue();
		int openParen = line.lastIndexOf('(');
		if (openParen > 0)
			line = line.substring(0, openParen-1);
		line = "ATTR_" + line;
		for(Iterator it = curCell.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (var.getKey().getName().equals(line)) return var;
		}
		return null;
	}

	private void changeParameter()
	{
		// change the parameter
		CreateParameter job = new CreateParameter(this, false);
	}

	private void deleteParameter()
	{
		// delete the parameter
		DeleteParameter job = new DeleteParameter(this);
	}

	/**
	 * Class to delete a parameter in a new thread.
	 */
	protected static class DeleteParameter extends Job
	{
		CellParameters dialog;

		protected DeleteParameter(CellParameters dialog)
		{
			super("Delete Parameter from Cell " + dialog.curCell.describe(),
				User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dialog = dialog;
			this.startJob();
		}

		public void doIt()
		{
			Variable var = dialog.getSelectedParameter();
			if (var == null) return;

			dialog.curCell.delVar(var.getKey());

			// delete this parameter from all instances
			dialog.updateInstances();

			dialog.reloadParamList();
			EditWindow.repaintAllContents();
		}
	}

	private void createParameter()
	{
		// create the parameter
		CreateParameter job = new CreateParameter(this, true);
	}

	/**
	 * Class to create a parameter in a new thread.
	 */
	protected static class CreateParameter extends Job
	{
		CellParameters dialog;

		protected CreateParameter(CellParameters dialog, boolean create)
		{
			super((create ? "Create" : "Change") + " Cell Parameter " + dialog.newParameter.getText(), User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dialog = dialog;
			this.startJob();
		}

		public void doIt()
		{
			String name = dialog.newParameter.getText().trim();
			if (name.length() == 0)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), "Must type a parameter name");
				return;
			}
			String value = dialog.defaultValue.getText();
			Variable oldVar = dialog.curCell.getVar("ATTR_" + name);
			Variable var = dialog.curCell.newVar("ATTR_" + name, value);
			if (var != null)
			{
				var.setDisplay();
				TextDescriptor td = var.getTextDescriptor();
				td.setParam();
				td.setInherit();
				td.setDispPart(TextDescriptor.DispPos.NAMEVALINH);
				if (oldVar != null)
				{
					// copy other factors too
					TextDescriptor oldTd = oldVar.getTextDescriptor();
					if (oldTd.getSize().isAbsolute()) td.setAbsSize((int)oldTd.getSize().getSize()); else
						td.setRelSize(oldTd.getSize().getSize());
					if (oldTd.isBold()) td.setBold();
					if (oldTd.isItalic()) td.setItalic();
					if (oldTd.isUnderline()) td.setUnderline();
					if (oldTd.isInterior()) td.setInterior();
					td.setFace(oldTd.getFace());
					td.setOff(oldTd.getXOff(), oldTd.getYOff());
					td.setPos(oldTd.getPos());
					td.setRotation(oldTd.getRotation());
				}

				int currentLanguage = dialog.language.getSelectedIndex();
				if (currentLanguage == 3) var.setJava();

				int unitIndex = dialog.units.getSelectedIndex();
				switch (unitIndex)
				{
					case 1:  td.setUnit(TextDescriptor.Unit.RESISTANCE);   break;
					case 2:  td.setUnit(TextDescriptor.Unit.CAPACITANCE);  break;
					case 3:  td.setUnit(TextDescriptor.Unit.INDUCTANCE);   break;
					case 4:  td.setUnit(TextDescriptor.Unit.CURRENT);      break;
					case 5:  td.setUnit(TextDescriptor.Unit.VOLTAGE);      break;
					case 6:  td.setUnit(TextDescriptor.Unit.DISTANCE);     break;
					case 7:  td.setUnit(TextDescriptor.Unit.TIME);         break;
					default: td.setUnit(TextDescriptor.Unit.NONE);         break;
				}

				// add this parameter to all instances
				dialog.updateInstances();

				dialog.reloadParamList();
				EditWindow.repaintAllContents();
			}
		}
	}

	private void paramListClick()
	{
		Variable var = getSelectedParameter();
		if (var == null) return;
		newParameter.setText(var.getTrueName());
		defaultValue.setText(var.getPureValue(-1, -1));

		int initialLanguage = 0;
		if (var.isTCL()) initialLanguage = 1; else
		if (var.isLisp()) initialLanguage = 2; else
		if (var.isJava()) initialLanguage = 3;
		language.setSelectedIndex(initialLanguage);

		TextDescriptor.Unit unit = var.getTextDescriptor().getUnit();
		if (unit == TextDescriptor.Unit.NONE) units.setSelectedIndex(0); else
		if (unit == TextDescriptor.Unit.RESISTANCE) units.setSelectedIndex(1); else
		if (unit == TextDescriptor.Unit.CAPACITANCE) units.setSelectedIndex(2); else
		if (unit == TextDescriptor.Unit.INDUCTANCE) units.setSelectedIndex(3); else
		if (unit == TextDescriptor.Unit.CURRENT) units.setSelectedIndex(4); else
		if (unit == TextDescriptor.Unit.VOLTAGE) units.setSelectedIndex(5); else
		if (unit == TextDescriptor.Unit.DISTANCE) units.setSelectedIndex(6); else
		if (unit == TextDescriptor.Unit.TIME) units.setSelectedIndex(7);
	}

	private void updateInstances()
	{
		// update all instances
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				for(Iterator nIt = cell.getNodes(); nIt.hasNext(); )
				{
					NodeInst ni = (NodeInst)nIt.next();
					NodeProto np = ni.getProto();
					if (!(np instanceof Cell)) continue;
					Cell subCell = (Cell)np;
					if (subCell.getCellGroup() != curCell.getCellGroup()) continue;
					if (subCell != curCell)
					{
						if (subCell.getView() != View.ICON) continue;
						if (curCell.iconView() != subCell) continue;
					}

					// ensure that this node matches the updated parameter list
					boolean keepOn = true;
					while (keepOn)
					{
						keepOn = false;
						for(Iterator vIt = ni.getVariables(); vIt.hasNext(); )
						{
							Variable var = (Variable)vIt.next();
							TextDescriptor td = var.getTextDescriptor();
							if (!td.isParam()) continue;
							Variable cellVar = null;
							for(Iterator cVIt = curCell.getVariables(); cVIt.hasNext(); )
							{
								Variable cVar = (Variable)cVIt.next();
								if (!cVar.getTextDescriptor().isParam()) continue;
								if (var.getKey() == cVar.getKey()) { cellVar = cVar;   break; }
							}
							if (cellVar == null)
							{
								// this node's parameter is no longer on the cell: delete from instance
								ni.delVar(var.getKey());
								keepOn = true;
								break;
							}

							// this node's parameter is still on the cell: make sure units are OK
							TextDescriptor cTd = cellVar.getTextDescriptor();
							if (td.getUnit() != cTd.getUnit())
							{
								td.setUnit(cTd.getUnit());
							}

							// make sure visibility is OK
							if (cTd.isInterior()) var.clearDisplay(); else
								var.setDisplay();
						}
					}
					for(Iterator cVIt = curCell.getVariables(); cVIt.hasNext(); )
					{
						Variable cVar = (Variable)cVIt.next();
						TextDescriptor cTd = cVar.getTextDescriptor();
						if (!cTd.isParam()) continue;
						Variable instVar = null;
						for(Iterator vIt = ni.getVariables(); vIt.hasNext(); )
						{
							Variable var = (Variable)vIt.next();
							TextDescriptor td = var.getTextDescriptor();
							if (!td.isParam()) continue;
							if (var.getKey() == cVar.getKey()) { instVar = var;   break; }
						}
						if (instVar == null)
						{
							// this cell parameter is not on the node: add to instance
							Variable var = ni.newVar(cVar.getKey(), cVar.getObject());
							if (var != null)
							{
								if (cVar.isTCL()) var.setTCL();
								if (cVar.isLisp()) var.setLisp();
								if (cVar.isJava()) var.setJava();
								TextDescriptor td = var.getTextDescriptor();
								td.setParam();
								td.clearInterior();
								td.setDispPart(TextDescriptor.DispPos.NAMEVALUE);
								td.setUnit(cTd.getUnit());
							}
						}
					}
				}
			}
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

        done = new javax.swing.JButton();
        changeParameter = new javax.swing.JButton();
        paramPane = new javax.swing.JScrollPane();
        headerMessage = new javax.swing.JLabel();
        newParameter = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        defaultValue = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        language = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        units = new javax.swing.JComboBox();
        createParameter = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        deleteParameter = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Cell Parameters");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        done.setText("Done");
        done.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                done(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 0.5;
        getContentPane().add(done, gridBagConstraints);

        changeParameter.setText("Change Parameter");
        changeParameter.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                changeParameter(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 0.5;
        getContentPane().add(changeParameter, gridBagConstraints);

        paramPane.setMinimumSize(new java.awt.Dimension(100, 22));
        paramPane.setPreferredSize(new java.awt.Dimension(100, 22));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(paramPane, gridBagConstraints);

        headerMessage.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(headerMessage, gridBagConstraints);

        newParameter.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(newParameter, gridBagConstraints);

        jLabel2.setText("New Parameter:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jLabel2, gridBagConstraints);

        defaultValue.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(defaultValue, gridBagConstraints);

        jLabel3.setText("Default Value:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(language, gridBagConstraints);

        jLabel4.setText("Language:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jLabel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(units, gridBagConstraints);

        createParameter.setText("Create Parameter");
        createParameter.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                createParameterActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(createParameter, gridBagConstraints);

        jLabel5.setText("Units:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jLabel5, gridBagConstraints);

        deleteParameter.setText("Delete Parameter");
        deleteParameter.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                deleteParameterActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        getContentPane().add(deleteParameter, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void deleteParameterActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_deleteParameterActionPerformed
	{//GEN-HEADEREND:event_deleteParameterActionPerformed
		deleteParameter();
	}//GEN-LAST:event_deleteParameterActionPerformed

	private void createParameterActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_createParameterActionPerformed
	{//GEN-HEADEREND:event_createParameterActionPerformed
		createParameter();
	}//GEN-LAST:event_createParameterActionPerformed

	private void done(java.awt.event.ActionEvent evt)//GEN-FIRST:event_done
	{//GEN-HEADEREND:event_done
		closeDialog(null);
	}//GEN-LAST:event_done

	private void changeParameter(java.awt.event.ActionEvent evt)//GEN-FIRST:event_changeParameter
	{//GEN-HEADEREND:event_changeParameter
		changeParameter();
	}//GEN-LAST:event_changeParameter

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton changeParameter;
    private javax.swing.JButton createParameter;
    private javax.swing.JTextField defaultValue;
    private javax.swing.JButton deleteParameter;
    private javax.swing.JButton done;
    private javax.swing.JLabel headerMessage;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JComboBox language;
    private javax.swing.JTextField newParameter;
    private javax.swing.JScrollPane paramPane;
    private javax.swing.JComboBox units;
    // End of variables declaration//GEN-END:variables
	
}
