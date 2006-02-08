/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FrameTab.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.TempPref;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.user.User;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Class to handle the "Frame" tab of the Preferences dialog.
 */
public class FrameTab extends PreferencePanel
{
	/** Creates new form FrameTab */
	public FrameTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return frame; }

	/** return the name of this preferences tab. */
	public String getName() { return "Frame"; }

	private static class LibraryFrameInfo
	{
		TempPref companyName;
		TempPref designerName;
		TempPref projectName;
	}
	private HashMap<Library,LibraryFrameInfo> frameLibInfo;
	private boolean frameInfoUpdating = false;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Frame tab.
	 */
	public void init()
	{
		Library curLib = Library.getCurrent();

		// cache text in each library
		frameLibInfo = new HashMap<Library,LibraryFrameInfo>();
		for(Library lib : Library.getVisibleLibraries())
		{
			LibraryFrameInfo lfi = new LibraryFrameInfo();
			String company = "";
			String designer = "";
			String project = "";
			Variable var = lib.getVar(User.FRAME_COMPANY_NAME, String.class);
			if (var != null) company = (String)var.getObject();
			var = lib.getVar(User.FRAME_DESIGNER_NAME, String.class);
			if (var != null) designer = (String)var.getObject();
			var = lib.getVar(User.FRAME_PROJECT_NAME, String.class);
			if (var != null) project = (String)var.getObject();
			lfi.companyName = TempPref.makeStringPref(company);
			lfi.designerName = TempPref.makeStringPref(designer);
			lfi.projectName = TempPref.makeStringPref(project);

			frameLibInfo.put(lib, lfi);
			frameLibrary.addItem(lib.getName());
		}
		frameLibrary.setSelectedItem(curLib.getName());

		frameLibrary.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { loadFrameLibInfo(); }
		});
		frameLibraryCompany.getDocument().addDocumentListener(new NewFrameLibInfoListener(this));
		frameLibraryDesigner.getDocument().addDocumentListener(new NewFrameLibInfoListener(this));
		frameLibraryProject.getDocument().addDocumentListener(new NewFrameLibInfoListener(this));
		frameLibrary.setSelectedItem(curLib.getName());

		frameDefaultCompany.setText(User.getFrameCompanyName());
		frameDefaultDesigner.setText(User.getFrameDesignerName());
		frameDefaultProject.setText(User.getFrameProjectName());

		loadFrameLibInfo();
	}

	private void loadFrameLibInfo()
	{
		String libName = (String)frameLibrary.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		LibraryFrameInfo lfi = frameLibInfo.get(lib);
		if (lfi == null) return;
		frameInfoUpdating = true;
		frameLibraryCompany.setText(lfi.companyName.getString());
		frameLibraryDesigner.setText(lfi.designerName.getString());
		frameLibraryProject.setText(lfi.projectName.getString());
		frameInfoUpdating = false;
	}

	private void updateFrameLibInfo()
	{
		if (frameInfoUpdating) return;
		String libName = (String)frameLibrary.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		LibraryFrameInfo lfi = frameLibInfo.get(lib);
		if (lfi == null) return;
		lfi.companyName.setString(frameLibraryCompany.getText());
		lfi.designerName.setString(frameLibraryDesigner.getText());
		lfi.projectName.setString(frameLibraryProject.getText());
	}

	/**
	 * Class to handle special changes to per-primitive node options.
	 */
	private static class NewFrameLibInfoListener implements DocumentListener
	{
		FrameTab dialog;

		NewFrameLibInfoListener(FrameTab dialog) { this.dialog = dialog; }

		public void changedUpdate(DocumentEvent e) { dialog.updateFrameLibInfo(); }
		public void insertUpdate(DocumentEvent e) { dialog.updateFrameLibInfo(); }
		public void removeUpdate(DocumentEvent e) { dialog.updateFrameLibInfo(); }
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Frame tab.
	 */
	public void term()
	{
		// save default title box info
		String currValue = frameDefaultCompany.getText();
		if (!currValue.equals(User.getFrameCompanyName()))
			User.setFrameCompanyName(currValue);
		currValue = frameDefaultDesigner.getText();
		if (!currValue.equals(User.getFrameDesignerName()))
			User.setFrameDesignerName(currValue);
		currValue = frameDefaultProject.getText();
		if (!currValue.equals(User.getFrameProjectName()))
			User.setFrameProjectName(currValue);

		// save per-library title box info
		for(Library lib : frameLibInfo.keySet())
		{
			LibraryFrameInfo lfi = frameLibInfo.get(lib);
			if (lfi == null) continue;
			if (!lfi.companyName.getString().equals(lfi.companyName.getFactoryValue()))
				lib.newVar(User.FRAME_COMPANY_NAME, lfi.companyName.getString());
			if (!lfi.designerName.getString().equals(lfi.designerName.getFactoryValue()))
				lib.newVar(User.FRAME_DESIGNER_NAME, lfi.designerName.getString());
			if (!lfi.projectName.getString().equals(lfi.projectName.getFactoryValue()))
				lib.newVar(User.FRAME_PROJECT_NAME, lfi.projectName.getString());
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

        frame = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        frameLibrary = new javax.swing.JComboBox();
        jLabel17 = new javax.swing.JLabel();
        frameDefaultCompany = new javax.swing.JTextField();
        frameLibraryCompany = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        frameDefaultDesigner = new javax.swing.JTextField();
        frameLibraryDesigner = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        frameDefaultProject = new javax.swing.JTextField();
        frameLibraryProject = new javax.swing.JTextField();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Edit Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        frame.setLayout(new java.awt.GridBagLayout());

        jLabel15.setText("General default:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        frame.add(jLabel15, gridBagConstraints);

        jLabel16.setText("Library default:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        frame.add(jLabel16, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        frame.add(frameLibrary, gridBagConstraints);

        jLabel17.setText("Company Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        frame.add(jLabel17, gridBagConstraints);

        frameDefaultCompany.setColumns(15);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        frame.add(frameDefaultCompany, gridBagConstraints);

        frameLibraryCompany.setColumns(15);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        frame.add(frameLibraryCompany, gridBagConstraints);

        jLabel18.setText("Designer Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        frame.add(jLabel18, gridBagConstraints);

        frameDefaultDesigner.setColumns(15);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        frame.add(frameDefaultDesigner, gridBagConstraints);

        frameLibraryDesigner.setColumns(15);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        frame.add(frameLibraryDesigner, gridBagConstraints);

        jLabel19.setText("Project Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        frame.add(jLabel19, gridBagConstraints);

        frameDefaultProject.setColumns(15);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        frame.add(frameDefaultProject, gridBagConstraints);

        frameLibraryProject.setColumns(15);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        frame.add(frameLibraryProject, gridBagConstraints);

        getContentPane().add(frame, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel frame;
    private javax.swing.JTextField frameDefaultCompany;
    private javax.swing.JTextField frameDefaultDesigner;
    private javax.swing.JTextField frameDefaultProject;
    private javax.swing.JComboBox frameLibrary;
    private javax.swing.JTextField frameLibraryCompany;
    private javax.swing.JTextField frameLibraryDesigner;
    private javax.swing.JTextField frameLibraryProject;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    // End of variables declaration//GEN-END:variables

}
