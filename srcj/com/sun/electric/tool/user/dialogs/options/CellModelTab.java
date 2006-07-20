/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellModelFile.java
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
import com.sun.electric.database.text.TempPref;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.output.Verilog;
import com.sun.electric.tool.io.output.CellModelPrefs;
import com.sun.electric.tool.user.dialogs.OpenFile;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
public class CellModelTab extends PreferencePanel
{
    private CellModelPrefs modelPrefs;

    private static String lastLib = "";
    private static String lastCell = "";

    enum Choice { NONE, USEMODELFILE, USELAYOUTNETLIST };

    private static final String recentlySetCellsName = "RecentlySetCells";

    private static class ModelPref {
        private String fileName;
        private Choice choice;
        private ModelPref() {
            fileName = "";
            choice = Choice.NONE;
        }
        public boolean equals(ModelPref other) {
            if (other.fileName.equals(fileName) && other.choice == choice)
                return true;
            return false;
        }
    }

	/** Creates new form CellModelFile panel */
	public CellModelTab(Frame parent, boolean modal, CellModelPrefs modelPrefs)
	{
		super(parent, modal);
        this.modelPrefs = modelPrefs;
		initComponents();
        if (!modelPrefs.isCanLayoutFromNetlist())
            netlistFromLayout.setEnabled(false);
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return chooserPanel; }

	/** return the name of this preferences tab. */
	public String getName() {
        return modelPrefs.getType()+" Model Files";
    }

	private HashMap<Cell,ModelPref> initialBehaveFiles;
	private JList cellList;
	private DefaultListModel cellListModel;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Verilog tab.
	 */
	public void init()
	{
		// gather all existing behave file information
		initialBehaveFiles = new HashMap<Cell,ModelPref>();
		for(Iterator<Library> lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library lib = lIt.next();
			if (lib.isHidden()) continue;
			for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = cIt.next();
                ModelPref pref = new ModelPref();
				pref.fileName = modelPrefs.getModelFile(cell);
                if (modelPrefs.isUseModelFromFile(cell))
                    pref.choice = Choice.USEMODELFILE;
                if (modelPrefs.isUseLayoutView(cell))
                    pref.choice = Choice.USELAYOUTNETLIST;
				//String behaveFile = "";
				//Variable var = cell.getVar(Verilog.VERILOG_BEHAVE_FILE_KEY);
				//if (var != null) behaveFile = var.getObject().toString();
				initialBehaveFiles.put(cell, pref);
			}
		}

		// make list of libraries
		for(Library lib : Library.getVisibleLibraries())
			libraryChoice.addItem(lib.getName());
        libraryChoice.addItem(recentlySetCellsName);
        boolean useLastLib = false;
        if (!lastLib.equals("")) {
            for (int i=0; i<libraryChoice.getItemCount(); i++) {
                String str = (String)libraryChoice.getItemAt(i);
                if (str.equals(lastLib)) {
                    useLastLib = true;
                    libraryChoice.setSelectedIndex(i);
                }
            }
        }
		if (!useLastLib)
            libraryChoice.setSelectedItem(curLib.getName());
		libraryChoice.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { verilogLoadCellList(); }
		});

		// make the list of cells
		cellListModel = new DefaultListModel();
		cellList = new JList(cellListModel);
		cellList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		cellsList.setViewportView(cellList);
		cellList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { verilogCellListClick(); }
		});

		browse.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { verModelFileBrowseActionPerformed(); }
		});
		deriveModel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { verilogModelClick(); }
		});
		useModelFile.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { verilogModelClick(); }
		});
        netlistFromLayout.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { verilogModelClick(); }
        });
		fileNameField.getDocument().addDocumentListener(new VerilogDocumentListener(this));
        showRecentCells.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { showRecentCellsOnlyClick(); }
        });
		verilogLoadCellList();
	}

	private void verModelFileBrowseActionPerformed()
	{
		String fileName = OpenFile.chooseInputFile(modelPrefs.getFileType(), null);
		if (fileName == null) return;
		useModelFile.setSelected(true);
		fileNameField.setEditable(true);
		fileNameField.setText(fileName);
	}

	/**
	 * Class to handle special changes to Verilog model file values.
	 */
	private static class VerilogDocumentListener implements DocumentListener
	{
		CellModelTab dialog;

		VerilogDocumentListener(CellModelTab dialog)
		{
			this.dialog = dialog;
		}

		private void change(DocumentEvent e)
		{
			// get the currently selected Cell
            Cell cell = dialog.getSelectedCell();
			if (cell == null) return;
			ModelPref pref = dialog.initialBehaveFiles.get(cell);
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
			pref.fileName = text;
		}

		public void changedUpdate(DocumentEvent e) { change(e); }
		public void insertUpdate(DocumentEvent e) { change(e); }
		public void removeUpdate(DocumentEvent e) { change(e); }
	}

	private void verilogLoadCellList()
	{
		String libName = (String)libraryChoice.getSelectedItem();
        boolean notEmpty = false;

        if (libName.equals(recentlySetCellsName)) {
            cellListModel.clear();
            for (Map.Entry<Cell,ModelPref> entry : initialBehaveFiles.entrySet()) {
                ModelPref pref = entry.getValue();
                if (pref.fileName.length() > 0 || pref.choice == Choice.USELAYOUTNETLIST ||
                        pref.choice == Choice.USEMODELFILE) {
                    Cell cell = entry.getKey();
                    cellListModel.addElement(cell.describe(false));
                    notEmpty = true;
                }
            }
        } else {
            Library lib = Library.findLibrary(libName);
            if (lib == null) return;
            cellListModel.clear();
            for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
            {
                Cell cell = it.next();
                cellListModel.addElement(cell.noLibDescribe());
                notEmpty = true;
            }
        }

        if (notEmpty)
        {
            cellList.setSelectedIndex(0);
            for (int i=0; i<cellList.getModel().getSize(); i++) {
                String str = (String)cellList.getModel().getElementAt(i);
                if (str.equals(lastCell)) {
                    cellList.setSelectedIndex(i);
                }
            }
            verilogCellListClick();
        }
	}

	private void verilogCellListClick()
	{
        Cell cell = getSelectedCell();
		if (cell == null) return;
		ModelPref pref = initialBehaveFiles.get(cell);
		if (pref == null) {
            deriveModel.setSelected(true);
            fileNameField.setEditable(false);
            fileNameField.setText("");
            return;
        }
		String behaveFile = pref.fileName;
        deriveModel.setSelected(pref.choice == Choice.NONE);
        netlistFromLayout.setSelected(pref.choice == Choice.USELAYOUTNETLIST);
        useModelFile.setSelected(pref.choice == Choice.USEMODELFILE);
        if (pref.choice == Choice.USEMODELFILE) {
            fileNameField.setEditable(true);
        } else {
			fileNameField.setEditable(false);
		}
        fileNameField.setText(behaveFile);

        lastLib = (String)libraryChoice.getSelectedItem();
        lastCell = (String)cellList.getSelectedValue();
	}

	private void verilogModelClick()
	{
		if (deriveModel.isSelected() || netlistFromLayout.isSelected())
		{
			fileNameField.setEditable(false);
		} else
		{
			fileNameField.setEditable(true);
		}
        Cell cell = getSelectedCell();
        if (cell == null) return;
        ModelPref pref = initialBehaveFiles.get(cell);
        if (deriveModel.isSelected()) pref.choice = Choice.NONE;
        if (netlistFromLayout.isSelected()) pref.choice = Choice.USELAYOUTNETLIST;
        if (useModelFile.isSelected()) pref.choice = Choice.USEMODELFILE;
	}

    private void showRecentCellsOnlyClick() {
        if (showRecentCells.isSelected()) {
            libraryChoice.setSelectedItem(recentlySetCellsName);
        } else {
            libraryChoice.setSelectedItem(curLib.getName());
        }
        verilogLoadCellList();
    }

    private Cell getSelectedCell() {
        String libName = (String)libraryChoice.getSelectedItem();
        String cellName = (String)cellList.getSelectedValue();
        Library lib;
        Cell cell = null;
        if (libName.equals(recentlySetCellsName)) {
            cell = (Cell)Cell.findNodeProto(cellName);
            lib = cell.getLibrary();
        } else {
            lib = Library.findLibrary(libName);
            cell = lib.findNodeProto(cellName);
        }
        return cell;
    }

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Verilog tab.
	 */
	public void term()
	{
		for(Iterator<Library> lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library lib = lIt.next();
			if (lib.isHidden()) continue;
			for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = cIt.next();
				ModelPref pref = initialBehaveFiles.get(cell);
				if (pref == null) continue;
                boolean useLayoutView = (pref.choice == Choice.USELAYOUTNETLIST);
                boolean useModelFile = (pref.choice == Choice.USEMODELFILE);
				if (!pref.fileName.equals(modelPrefs.getModelFile(cell)) ||
                    useLayoutView != modelPrefs.isUseLayoutView(cell) ||
                    useModelFile != modelPrefs.isUseModelFromFile(cell))
				{
                    String fileName = pref.fileName.trim();
                    modelPrefs.setModelFile(cell, fileName, useModelFile, useLayoutView);
					//cell.newVar(Verilog.VERILOG_BEHAVE_FILE_KEY, pref.getString());
				}
			}
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

        verilogModel = new javax.swing.ButtonGroup();
        chooserPanel = new javax.swing.JPanel();
        jLabel54 = new javax.swing.JLabel();
        libraryChoice = new javax.swing.JComboBox();
        cellsList = new javax.swing.JScrollPane();
        deriveModel = new javax.swing.JRadioButton();
        useModelFile = new javax.swing.JRadioButton();
        browse = new javax.swing.JButton();
        fileNameField = new javax.swing.JTextField();
        showRecentCells = new javax.swing.JCheckBox();
        netlistFromLayout = new javax.swing.JRadioButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Tool Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        chooserPanel.setLayout(new java.awt.GridBagLayout());

        jLabel54.setText("Library:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        chooserPanel.add(jLabel54, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        chooserPanel.add(libraryChoice, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        chooserPanel.add(cellsList, gridBagConstraints);

        verilogModel.add(deriveModel);
        deriveModel.setText("Derive Model from Circuitry");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(20, 4, 4, 4);
        chooserPanel.add(deriveModel, gridBagConstraints);

        verilogModel.add(useModelFile);
        useModelFile.setText("Use Model from File:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        chooserPanel.add(useModelFile, gridBagConstraints);

        browse.setText("Browse");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        chooserPanel.add(browse, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        chooserPanel.add(fileNameField, gridBagConstraints);

        showRecentCells.setText("Show Recently Used Cells Only");
        showRecentCells.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        showRecentCells.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 7, 4, 4);
        chooserPanel.add(showRecentCells, gridBagConstraints);

        verilogModel.add(netlistFromLayout);
        netlistFromLayout.setText("Netlist from Layout");
        netlistFromLayout.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        netlistFromLayout.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 4);
        chooserPanel.add(netlistFromLayout, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(chooserPanel, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browse;
    private javax.swing.JScrollPane cellsList;
    private javax.swing.JPanel chooserPanel;
    private javax.swing.JRadioButton deriveModel;
    private javax.swing.JTextField fileNameField;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JComboBox libraryChoice;
    private javax.swing.JRadioButton netlistFromLayout;
    private javax.swing.JCheckBox showRecentCells;
    private javax.swing.JRadioButton useModelFile;
    private javax.swing.ButtonGroup verilogModel;
    // End of variables declaration//GEN-END:variables

}
