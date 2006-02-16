/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BusParameters.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.Highlight2;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 * Class to handle the "Bus Parameters" dialog.
 */
public class BusParameters extends EDialog
{
	/** key for library's bus variables. */	public static final Variable.Key BUS_VARIABLES = Variable.newKey("LIB_Bus_Variables");
	/** key for arc's bus template. */		public static final Variable.Key ARC_BUS_TEMPLATE = Variable.newKey("ARC_Bus_Template");
	/** key for export's bus template. */	public static final Variable.Key EXPORT_BUS_TEMPLATE = Variable.newKey("EXPORT_Bus_Template");

	private JList parametersList;
	private DefaultListModel parametersModel;
	HashMap<Library,String[]> libParameters;
	
	public static void showBusParametersDialog()
	{
		BusParameters dialog = new BusParameters(TopLevel.getCurrentJFrame());
		dialog.setVisible(true);
	}

	public static void makeBusParameter()
	{
		Cell cell = WindowFrame.needCurCell();
		EditWindow wnd = EditWindow.getCurrent();
		Highlight2 h = wnd.getHighlighter().getOneHighlight();
		if (h == null)
		{
			Job.getUserInterface().showErrorMessage("Select an arc or export name first", "Nothing Selected");
			return;
		}
		ElectricObject owner = h.getElectricObject();
		if (owner == null || !(owner instanceof ArcInst || owner instanceof Export))
		{
			Job.getUserInterface().showErrorMessage("Select an arc or export name first", "Incorrect Selection");
			return;
		}
		if (owner instanceof ArcInst)
		{
			if (h.getVarKey() != ArcInst.ARC_NAME)
			{
				Job.getUserInterface().showErrorMessage("Must select the arc's name", "Incorrect Selection");
				return;
			}
		}
		new AddTemplate(owner);
	}

	/** Creates new form Bus Parameters */
	private BusParameters(Frame parent)
	{
		super(parent, true);
		initComponents();

		// build display list for variables
		parametersModel = new DefaultListModel();
		parametersList = new JList(parametersModel);
		parametersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		variablesPane.setViewportView(parametersList);

		value.getDocument().addDocumentListener(new BusParametersDocumentListener(this));

		// find library with variables in it
		libParameters = new HashMap<Library,String[]>();
		Library bestLib = null;
		int mostParameters = 0;
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = it.next();
			if (lib.isHidden()) continue;
			libraryPopup.addItem(lib.getName());
			Variable var = lib.getVar(BUS_VARIABLES);
			String [] parameterList = new String[0];
			if (var != null) parameterList = (String [])var.getObject();
			libParameters.put(lib, parameterList);
			if (parameterList.length > mostParameters)
			{
				bestLib = lib;
				mostParameters = parameterList.length;
			}
		}
		Library curLib = Library.getCurrent();
		String [] parameterList = libParameters.get(curLib);
		if (parameterList.length > 0 || bestLib == null) bestLib = curLib;

		parametersList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { variablesSelected(); }
		});
		libraryPopup.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { libraryChanged(); }
		});
		libraryPopup.setSelectedItem(bestLib.getName());

		pack();
		finishInitialization();
	}

	protected void escapePressed() { doneActionPerformed(null); }

	/**
	 * Method called when the library popup is changed
	 * and the list of bus variables should be updated.
	 */
	private void libraryChanged()
	{
		parametersModel.clear();
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		if (lib == null) return;
		String [] parameterList = libParameters.get(lib);
		boolean gotSome = false;
		for(int i=0; i<parameterList.length; i++)
		{
			String variable = parameterList[i];
			int equalPos = variable.indexOf('=');
			if (equalPos < 0) continue;
			parametersModel.addElement(variable.substring(0, equalPos));
			gotSome = true;
		}
		if (gotSome)
		{
			parametersList.setSelectedIndex(0);
			variablesSelected();
		}
	}

	/**
	 * Method called when a variable has been selected
	 * and its value should be shown.
	 */
	private void variablesSelected()
	{
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		if (lib == null) return;
		String [] parameterList = libParameters.get(lib);
		int selectedIndex = parametersList.getSelectedIndex();
		if (selectedIndex < 0 || selectedIndex >= parameterList.length) return;
		String varSelected = parameterList[selectedIndex];
		int equalPos = varSelected.indexOf('=');
		if (equalPos < 0) return;
		value.setText(varSelected.substring(equalPos+1));
	}

	/**
	 * Method called when a bus variable value has changed.
	 */
	private void valueChanged()
	{
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		if (lib == null) return;
		String [] parameterList = libParameters.get(lib);
		int selectedIndex = parametersList.getSelectedIndex();
		if (selectedIndex < 0 || selectedIndex >= parameterList.length) return;
		String parSelected = parameterList[selectedIndex];
		int equalPos = parSelected.indexOf('=');
		if (equalPos < 0) return;
		parameterList[selectedIndex] = parSelected.substring(0, equalPos+1) + value.getText();
		new UpdateLibrary(lib, parameterList);
	}

	/**
	 * Class to handle special changes to changes to the variable value.
	 */
	private static class BusParametersDocumentListener implements DocumentListener
	{
		BusParameters dialog;

		BusParametersDocumentListener(BusParameters dialog) { this.dialog = dialog; }

		public void changedUpdate(DocumentEvent e) { dialog.valueChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.valueChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.valueChanged(); }
	}

	/**
	 * Class to update variables on a library.
	 */
	private static class UpdateLibrary extends Job
	{
		private Library lib;
		private String [] parameterList;

		private UpdateLibrary(Library lib, String [] parameterList)
		{
			super("Update Bus Parameters", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.lib = lib;
			this.parameterList = parameterList;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			lib.newVar(BUS_VARIABLES, parameterList);
//			lib.setChanged();
			return true;
		}
	}

	/**
	 * Class to update parameters on all libraries.
	 */
	private static class UpdateAllParameters extends Job
	{
		private HashMap<Library,String[]> libParameters;

		private UpdateAllParameters(HashMap<Library,String[]> libParameters)
		{
			super("Update All Bus Parameters", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.libParameters = libParameters;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = it.next();
				if (lib.isHidden()) continue;
				for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = cIt.next();
					for(Iterator<ArcInst> aIt = cell.getArcs(); aIt.hasNext(); )
					{
						ArcInst ai = aIt.next();
						Variable var = ai.getVar(ARC_BUS_TEMPLATE);
						if (var != null)
						{
							String newVarString = updateVariable(var, lib, libParameters);
							String arcName = ai.getName();
							if (!arcName.equalsIgnoreCase(newVarString))
								ai.setName(newVarString);
						}
					}
					for(Iterator<Export> eIt = cell.getExports(); eIt.hasNext(); )
					{
						Export e = eIt.next();
						Variable var = e.getVar(EXPORT_BUS_TEMPLATE);
						if (var != null)
						{
							String newVarString = updateVariable(var, lib, libParameters);
							String exportName = e.getName();
							if (!exportName.equalsIgnoreCase(newVarString))
								e.rename(newVarString);
						}
					}
				}
			}
			return true;
		}
	}

	private static String updateVariable(Variable var, Library lib, HashMap<Library,String[]> libParameters)
	{
		String varString = (String)var.getObject();
		for(;;)
		{
			int dollarPos = varString.indexOf("$(");
			if (dollarPos < 0) break;
			int closePos = varString.indexOf(')', dollarPos);
			if (closePos < 0)
			{
				System.out.println("ERROR: Bus parameter '" + varString + "' is missing the close parenthesis");
				break;
			}
			String varName = varString.substring(dollarPos+2, closePos);

			String [] paramList = libParameters.get(lib);
			String paramValue = findParameterValue(paramList, varName);
			if (paramValue == null)
			{
				for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
				{
					Library aLib = it.next();
					if (aLib == lib || aLib.isHidden()) continue;
					paramList = libParameters.get(aLib);
					paramValue = findParameterValue(paramList, varName);
					if (paramValue != null) break;
				}
				if (paramValue == null)
				{
					System.out.println("ERROR: Bus parameter '" + varName + "' is not defined");
					break;
				}
			}
			varString = varString.substring(0, dollarPos) + paramValue + varString.substring(closePos+1);
		}
		return varString;
	}

	private static String findParameterValue(String [] parameterList, String varName)
	{
		for(int i=0; i<parameterList.length; i++)
		{
			String param = parameterList[i];
			int equalPos = param.indexOf('=');
			if (equalPos < 0) continue;
			if (varName.equalsIgnoreCase(param.substring(0, equalPos)))
				return param.substring(equalPos+1);
		}
		return null;
	}

	/**
	 * Class to create a bus template on an arc or export.
	 */
	private static class AddTemplate extends Job
	{
		private ElectricObject owner;

		private AddTemplate(ElectricObject owner)
		{
			super("Create Bus Parameter", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.owner = owner;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			if (owner instanceof ArcInst)
			{
				// add template to arc
				ArcInst ai = (ArcInst)owner;
				TextDescriptor td = ai.getTextDescriptor(ArcInst.ARC_NAME);
				double relSize = 1;
				if (!td.getSize().isAbsolute())
					relSize = td.getSize().getSize();
				td = td.withOff(td.getXOff(), td.getYOff() - relSize*1.5).withRelSize(relSize/2).withDispPart(TextDescriptor.DispPos.NAMEVALUE);
				ai.newVar(ARC_BUS_TEMPLATE, ai.getName(), td);
			} else
			{
				// add template to export
				Export e = (Export)owner;
				TextDescriptor td = e.getTextDescriptor(Export.EXPORT_NAME);
				double relSize = 1;
				if (!td.getSize().isAbsolute())
					relSize = td.getSize().getSize();
				td = td.withOff(td.getXOff(), td.getYOff() - relSize*1.5).withRelSize(relSize/2).withDispPart(TextDescriptor.DispPos.NAMEVALUE);
				e.newVar(EXPORT_BUS_TEMPLATE, e.getName(), td);
			}
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

        done = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        libraryPopup = new javax.swing.JComboBox();
        variablesPane = new javax.swing.JScrollPane();
        jLabel2 = new javax.swing.JLabel();
        value = new javax.swing.JTextField();
        update = new javax.swing.JButton();
        deleteVariable = new javax.swing.JButton();
        newVariable = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Bus Parameters");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        getAccessibleContext().setAccessibleName("Bus Parameters");
        done.setText("Done");
        done.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doneActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(done, gridBagConstraints);

        jLabel1.setText("Library:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(libraryPopup, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(variablesPane, gridBagConstraints);

        jLabel2.setText("Parameters:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel2, gridBagConstraints);

        value.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(value, gridBagConstraints);

        update.setText("Update All Templates");
        update.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(update, gridBagConstraints);

        deleteVariable.setText("Delete Parameter");
        deleteVariable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteVariableActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(deleteVariable, gridBagConstraints);

        newVariable.setText("New Parameter");
        newVariable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newVariableActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(newVariable, gridBagConstraints);

        jLabel3.setText("Parameter Value:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jSeparator1, gridBagConstraints);

        pack();
    }
    // </editor-fold>//GEN-END:initComponents

    private void newVariableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newVariableActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		if (lib == null) return;
		String [] parameterList = libParameters.get(lib);
		String newParName = Job.getUserInterface().askForInput("New Bus Parameter Name:", "Create New Bus Parameter", "");
		if (newParName == null) return;

		// make sure the name is unique
		int insertAfter = -1;
		for(int i=0; i<parameterList.length; i++)
		{
			int equalPos = parameterList[i].indexOf('=');
			if (equalPos < 0) continue;
			String varName = parameterList[i].substring(0, equalPos);
			if (varName.equalsIgnoreCase(newParName))
			{
				Job.getUserInterface().showErrorMessage("That bus parameter name already exists", "Duplicate Name");
				return;
			}
			if (varName.compareToIgnoreCase(newParName) < 0) insertAfter = i;
		}
		String [] newParameterList = new String[parameterList.length+1];
		int j = 0;
		for(int i=0; i<parameterList.length; i++)
		{
			if (i == insertAfter+1) newParameterList[j++] = newParName + "=1";
			newParameterList[j++] = parameterList[i];
		}
		if (parameterList.length == insertAfter+1) newParameterList[j++] = newParName + "=1";
		libParameters.put(lib, newParameterList);
		new UpdateLibrary(lib, newParameterList);
		libraryChanged();
    }//GEN-LAST:event_newVariableActionPerformed

	private void deleteVariableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteVariableActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		if (lib == null) return;
		String [] parameterList = libParameters.get(lib);
		int selectedIndex = parametersList.getSelectedIndex();
		if (selectedIndex < 0 || selectedIndex >= parameterList.length) return;
		String [] newParameterList = new String[parameterList.length-1];
		int j = 0;
		for(int i=0; i<parameterList.length; i++)
		{
			if (i != selectedIndex) newParameterList[j++] = parameterList[i];
		}
		libParameters.put(lib, newParameterList);
		new UpdateLibrary(lib, newParameterList);
		libraryChanged();
    }//GEN-LAST:event_deleteVariableActionPerformed

    private void updateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateActionPerformed
		new UpdateAllParameters(libParameters);
    }//GEN-LAST:event_updateActionPerformed

    private void doneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_doneActionPerformed
		closeDialog(null);
    }//GEN-LAST:event_doneActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton deleteVariable;
    private javax.swing.JButton done;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JComboBox libraryPopup;
    private javax.swing.JButton newVariable;
    private javax.swing.JButton update;
    private javax.swing.JTextField value;
    private javax.swing.JScrollPane variablesPane;
    // End of variables declaration//GEN-END:variables
}
