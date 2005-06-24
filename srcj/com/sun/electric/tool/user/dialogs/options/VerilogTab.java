/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VerilogTab.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.io.output.Verilog;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.dialogs.OpenFile;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * Class to handle the "Verilog" tab of the Preferences dialog.
 */
public class VerilogTab extends PreferencePanel
{
	/** Creates new form VerilogTab */
	public VerilogTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return verilog; }

	/** return the name of this preferences tab. */
	public String getName() { return "Verilog"; }

	private HashMap initialVerilogBehaveFiles;
	private JList verilogCellList;
	private DefaultListModel verilogCellListModel;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Verilog tab.
	 */
	public void init()
	{
		verUseAssign.setSelected(Simulation.getVerilogUseAssign());
		verDefWireTrireg.setSelected(Simulation.getVerilogUseTrireg());

		// gather all existing behave file information
		initialVerilogBehaveFiles = new HashMap();
		for(Iterator lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library lib = (Library)lIt.next();
			if (lib.isHidden()) continue;
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				String behaveFile = "";
				Variable var = cell.getVar(Verilog.VERILOG_BEHAVE_FILE_KEY);
				if (var != null) behaveFile = var.getObject().toString();
				initialVerilogBehaveFiles.put(cell, Pref.makeStringPref(null, null, behaveFile));
			}
		}

		// make list of libraries
		/*for(Library lib: Library.getVisibleLibraries()) verLibrary.addItem(lib.getName());*/
		for(Iterator it = Library.getVisibleLibraries().iterator(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			verLibrary.addItem(lib.getName());
		}
		verLibrary.setSelectedItem(curLib.getName());
		verLibrary.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { verilogLoadCellList(); }
		});

		// make the list of cells
		verilogCellListModel = new DefaultListModel();
		verilogCellList = new JList(verilogCellListModel);
		verilogCellList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		verCells.setViewportView(verilogCellList);
		verilogCellList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { verilogCellListClick(); }
		});

		verBrowse.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { verModelFileBrowseActionPerformed(); }
		});
		verDeriveModel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { verilogModelClick(); }
		});
		verUseModelFile.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { verilogModelClick(); }
		});
		verFileName.getDocument().addDocumentListener(new VerilogDocumentListener(this));
		verilogLoadCellList();
	}

	private void verModelFileBrowseActionPerformed()
	{
		String fileName = OpenFile.chooseInputFile(FileType.VERILOG, null);
		if (fileName == null) return;
		verUseModelFile.setSelected(true);
		verFileName.setEditable(true);
		verFileName.setText(fileName);
	}

	/**
	 * Class to handle special changes to Verilog model file values.
	 */
	private static class VerilogDocumentListener implements DocumentListener
	{
		VerilogTab dialog;

		VerilogDocumentListener(VerilogTab dialog)
		{
			this.dialog = dialog;
		}

		private void change(DocumentEvent e)
		{
			// get the currently selected Cell
			String libName = (String)dialog.verLibrary.getSelectedItem();
			Library lib = Library.findLibrary(libName);
			String cellName = (String)dialog.verilogCellList.getSelectedValue();
			Cell cell = lib.findNodeProto(cellName);
			if (cell == null) return;
			Pref pref = (Pref)dialog.initialVerilogBehaveFiles.get(cell);
			if (pref == null) return;

			// get the typed value
			Document doc = e.getDocument();
			int len = doc.getLength();
			String text;
			try
			{
				text = doc.getText(0, len);
			} catch (BadLocationException ex) { return; }

			// update the option
			pref.setString(text);
		}

		public void changedUpdate(DocumentEvent e) { change(e); }
		public void insertUpdate(DocumentEvent e) { change(e); }
		public void removeUpdate(DocumentEvent e) { change(e); }
	}

	private void verilogLoadCellList()
	{
		String libName = (String)verLibrary.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		if (lib == null) return;
		verilogCellListModel.clear();
		boolean notEmpty = false;
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			verilogCellListModel.addElement(cell.noLibDescribe());
			notEmpty = true;
		}
		if (notEmpty)
		{
			verilogCellList.setSelectedIndex(0);
			verilogCellListClick();
		}
	}

	private void verilogCellListClick()
	{
		String libName = (String)verLibrary.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		String cellName = (String)verilogCellList.getSelectedValue();
		Cell cell = lib.findNodeProto(cellName);
		if (cell == null) return;
		Pref pref = (Pref)initialVerilogBehaveFiles.get(cell);
		if (pref == null) return;
		String behaveFile = pref.getString();
		if (behaveFile.length() == 0)
		{
			verDeriveModel.setSelected(true);
			verFileName.setEditable(false);
			verFileName.setText("");
		} else
		{
			verUseModelFile.setSelected(true);
			verFileName.setEditable(true);
			verFileName.setText(behaveFile);
		}
	}

	private void verilogModelClick()
	{
		if (verDeriveModel.isSelected())
		{
			verFileName.setEditable(false);
			verFileName.setText("");
		} else
		{
			verFileName.setEditable(true);
		}
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Verilog tab.
	 */
	public void term()
	{
		boolean currBoolean = verUseAssign.isSelected();
		if (currBoolean != Simulation.getVerilogUseAssign())
			Simulation.setVerilogUseAssign(currBoolean);

		currBoolean = verDefWireTrireg.isSelected();
		if (currBoolean != Simulation.getVerilogUseTrireg())
			Simulation.setVerilogUseTrireg(currBoolean);

		for(Iterator lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library lib = (Library)lIt.next();
			if (lib.isHidden()) continue;
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				Pref pref = (Pref)initialVerilogBehaveFiles.get(cell);
				if (pref == null) continue;
				if (!pref.getStringFactoryValue().equals(pref.getString()))
				{
					cell.newVar(Verilog.VERILOG_BEHAVE_FILE_KEY, pref.getString());
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

        verilogModel = new javax.swing.ButtonGroup();
        verilog = new javax.swing.JPanel();
        jLabel54 = new javax.swing.JLabel();
        verLibrary = new javax.swing.JComboBox();
        verCells = new javax.swing.JScrollPane();
        verUseAssign = new javax.swing.JCheckBox();
        verDefWireTrireg = new javax.swing.JCheckBox();
        verDeriveModel = new javax.swing.JRadioButton();
        verUseModelFile = new javax.swing.JRadioButton();
        verBrowse = new javax.swing.JButton();
        verFileName = new javax.swing.JTextField();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Tool Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        verilog.setLayout(new java.awt.GridBagLayout());

        jLabel54.setText("Library:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        verilog.add(jLabel54, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        verilog.add(verLibrary, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        verilog.add(verCells, gridBagConstraints);

        verUseAssign.setText("Use ASSIGN Construct");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        verilog.add(verUseAssign, gridBagConstraints);

        verDefWireTrireg.setText("Default wire is Trireg");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        verilog.add(verDefWireTrireg, gridBagConstraints);

        verDeriveModel.setText("Derive Model from Circuitry");
        verilogModel.add(verDeriveModel);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(20, 4, 4, 4);
        verilog.add(verDeriveModel, gridBagConstraints);

        verUseModelFile.setText("Use Model from File:");
        verilogModel.add(verUseModelFile);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        verilog.add(verUseModelFile, gridBagConstraints);

        verBrowse.setText("Browse");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        verilog.add(verBrowse, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        verilog.add(verFileName, gridBagConstraints);

        getContentPane().add(verilog, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel54;
    private javax.swing.JButton verBrowse;
    private javax.swing.JScrollPane verCells;
    private javax.swing.JCheckBox verDefWireTrireg;
    private javax.swing.JRadioButton verDeriveModel;
    private javax.swing.JTextField verFileName;
    private javax.swing.JComboBox verLibrary;
    private javax.swing.JCheckBox verUseAssign;
    private javax.swing.JRadioButton verUseModelFile;
    private javax.swing.JPanel verilog;
    private javax.swing.ButtonGroup verilogModel;
    // End of variables declaration//GEN-END:variables

}
