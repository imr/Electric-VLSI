/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellOptions.java
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
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Dimension;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import javax.swing.JList;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.GridBagLayout;
import javax.swing.ButtonGroup;
import javax.swing.JTextField;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.text.BadLocationException;


/**
 * Class to handle the "Cell Options" dialog.
 */
/**
* This code was generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a
* for-profit company or business) then you should purchase
* a license - please visit www.cloudgarden.com for details.
*/

public class CellOptions extends javax.swing.JDialog
{
	private JList cellList;
	private DefaultListModel cellListModel;
	private HashMap origValues;
	private boolean initialCheckDatesDuringCreation;
	private boolean initialAutoTechnologySwitch;
	private boolean initialPlaceCellCenter;


	public CellOptions() {
		initGUI();
	}
	static class OldValues
	{
		boolean disAllMod;
		boolean disInstMod;
		boolean inCellLib;
		boolean useTechEditor;
		boolean defExpanded;
		double charX, charY;
		boolean disAllModChanged;
		boolean disInstModChanged;
		boolean inCellLibChanged;
		boolean useTechEditorChanged;
		boolean defExpandedChanged;
		boolean characteristicChanged;
	};

	/** Creates new form Cell Options */
	public CellOptions(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		setLocation(100, 50);
		initComponents();

		// cache all information
		origValues = new HashMap();
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			if (lib.isHidden()) continue;
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				OldValues ov = new OldValues();
				ov.disAllMod = cell.isAllLocked();
				ov.disInstMod = cell.isInstancesLocked();
				ov.inCellLib = cell.isInCellLibrary();
				ov.useTechEditor = cell.isInTechnologyLibrary();
				ov.defExpanded = cell.isWantExpanded();
				ov.charX = ov.charY = 0;
				ov.disAllModChanged = false;
				ov.disInstModChanged = false;
				ov.inCellLibChanged = false;
				ov.useTechEditorChanged = false;
				ov.defExpandedChanged = false;
				ov.characteristicChanged = false;
				Dimension spacing = cell.getCharacteristicSpacing();
				if (spacing != null)
				{
					ov.charX = spacing.getWidth();
					ov.charY = spacing.getHeight();
				}
				origValues.put(cell, ov);
			}
		}

		// build the cell list
		cellListModel = new DefaultListModel();
		cellList = new JList(cellListModel);
		cellList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		cellPane.setViewportView(cellList);
		cellList.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(java.awt.event.MouseEvent evt) { cellListClick(); }
		});

		// make a popup of libraries
		List libList = Library.getVisibleLibrariesSortedByName();
		for(Iterator it = libList.iterator(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			libraryPopup.addItem(lib.getLibName());
		}
		int curIndex = libList.indexOf(Library.getCurrent());
		if (curIndex >= 0) libraryPopup.setSelectedIndex(curIndex);

		charXSpacing.getDocument().addDocumentListener(new CharSpacingListener(this, true));
		charYSpacing.getDocument().addDocumentListener(new CharSpacingListener(this, false));

		confirmDelete.setSelected(true);

		loadCellList();
	}

	private void loadCellList()
	{
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		if (lib == null) return;
		boolean any = false;
		cellListModel.clear();
		for(Iterator it = lib.getCellsSortedByName().iterator(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			cellListModel.addElement(cell.noLibDescribe());
			any = true;
		}
		if (any) cellList.setSelectedIndex(0); else
			cellList.setSelectedValue(null, false);
		cellListClick();
	}

	private Cell getSelectedCell()
	{
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		String cellName = (String)cellList.getSelectedValue();
		if (cellName == null) return null;
		Cell cell = lib.findNodeProto(cellName);
		return cell;
	}

	private void cellListClick()
	{
		Cell cell = getSelectedCell();
		if (cell == null) return;
		OldValues ov = (OldValues)origValues.get(cell);
		if (ov != null)
		{
			cellName.setText(cell.getProtoName());
			disallowModAnyInCell.setSelected(ov.disAllMod);
			disallowModInstInCell.setSelected(ov.disInstMod);
			partOfCellLib.setSelected(ov.inCellLib);
			useTechEditor.setSelected(ov.useTechEditor);
			expandNewInstances.setSelected(ov.defExpanded);
			unexpandNewInstances.setSelected(!ov.defExpanded);
			charXSpacing.setText(Double.toString(ov.charX));
			charYSpacing.setText(Double.toString(ov.charY));
		}
	}

	/**
	 * Class to handle special changes to characteristic spacing.
	 */
	private static class CharSpacingListener implements DocumentListener
	{
		CellOptions dialog;
		boolean x;

		CharSpacingListener(CellOptions dialog, boolean x)
		{
			this.dialog = dialog;
			this.x = x;
		}

		private void change(DocumentEvent e)
		{
			// get the currently selected cell
			Cell cell = dialog.getSelectedCell();
			if (cell == null) return;
			OldValues ov = (OldValues)dialog.origValues.get(cell);

			// get the typed value
			Document doc = e.getDocument();
			int len = doc.getLength();
			String text;
			try
			{
				text = doc.getText(0, len);
			} catch (BadLocationException ex) { return; }
			double v = TextUtils.atof(text);

			// update the option
			if (x)
			{
				if (ov.charX != v) ov.characteristicChanged = true;
				ov.charX = v;
			} else
			{
				if (ov.charY != v) ov.characteristicChanged = true;
				ov.charY = v;
			}
		}

		public void changedUpdate(DocumentEvent e) { change(e); }
		public void insertUpdate(DocumentEvent e) { change(e); }
		public void removeUpdate(DocumentEvent e) { change(e); }
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        expansion = new javax.swing.ButtonGroup();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        libraryPopup = new javax.swing.JComboBox();
        cellPane = new javax.swing.JScrollPane();
        disallowModAnyInCell = new javax.swing.JCheckBox();
        setDisallowModAnyInCell = new javax.swing.JButton();
        clearDisallowModAnyInCell = new javax.swing.JButton();
        disallowModInstInCell = new javax.swing.JCheckBox();
        setDisallowModInstInCell = new javax.swing.JButton();
        clearDisallowModInstInCell = new javax.swing.JButton();
        partOfCellLib = new javax.swing.JCheckBox();
        setPartOfCellLib = new javax.swing.JButton();
        clearPartOfCellLib = new javax.swing.JButton();
        useTechEditor = new javax.swing.JCheckBox();
        setUseTechEditor = new javax.swing.JButton();
        clearUseTechEditor = new javax.swing.JButton();
        expandNewInstances = new javax.swing.JRadioButton();
        unexpandNewInstances = new javax.swing.JRadioButton();
        jLabel2 = new javax.swing.JLabel();
        charXSpacing = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        charYSpacing = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        editCell = new javax.swing.JButton();
        cellName = new javax.swing.JTextField();
        rename = new javax.swing.JButton();
        delete = new javax.swing.JButton();
        confirmDelete = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Cell Control");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
				CellOptionsWindowClosing(evt);
            }
        });

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cancel(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ok(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        jLabel1.setText("Library:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel1, gridBagConstraints);

        libraryPopup.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                libraryPopupActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(libraryPopup, gridBagConstraints);

        cellPane.setMinimumSize(new java.awt.Dimension(200, 250));
        cellPane.setPreferredSize(new java.awt.Dimension(200, 250));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cellPane, gridBagConstraints);

        disallowModAnyInCell.setText("Disallow modification of anything in this cell");
        disallowModAnyInCell.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                disallowModAnyInCellActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        getContentPane().add(disallowModAnyInCell, gridBagConstraints);

        setDisallowModAnyInCell.setText("Set");
        setDisallowModAnyInCell.setMinimumSize(new java.awt.Dimension(53, 20));
        setDisallowModAnyInCell.setPreferredSize(new java.awt.Dimension(53, 20));
        setDisallowModAnyInCell.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                setDisallowModAnyInCellActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 2);
        getContentPane().add(setDisallowModAnyInCell, gridBagConstraints);

        clearDisallowModAnyInCell.setText("Clear");
        clearDisallowModAnyInCell.setMinimumSize(new java.awt.Dimension(64, 20));
        clearDisallowModAnyInCell.setPreferredSize(new java.awt.Dimension(64, 20));
        clearDisallowModAnyInCell.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                clearDisallowModAnyInCellActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 2, 2, 4);
        getContentPane().add(clearDisallowModAnyInCell, gridBagConstraints);

        disallowModInstInCell.setText("Disallow modification of instances in this cell");
        disallowModInstInCell.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                disallowModInstInCellActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(disallowModInstInCell, gridBagConstraints);

        setDisallowModInstInCell.setText("Set");
        setDisallowModInstInCell.setMinimumSize(new java.awt.Dimension(53, 20));
        setDisallowModInstInCell.setPreferredSize(new java.awt.Dimension(53, 20));
        setDisallowModInstInCell.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                setDisallowModInstInCellActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 2);
        getContentPane().add(setDisallowModInstInCell, gridBagConstraints);

        clearDisallowModInstInCell.setText("Clear");
        clearDisallowModInstInCell.setMinimumSize(new java.awt.Dimension(64, 20));
        clearDisallowModInstInCell.setPreferredSize(new java.awt.Dimension(64, 20));
        clearDisallowModInstInCell.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                clearDisallowModInstInCellActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 4);
        getContentPane().add(clearDisallowModInstInCell, gridBagConstraints);

        partOfCellLib.setText("Part of a cell-library");
        partOfCellLib.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                partOfCellLibActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(partOfCellLib, gridBagConstraints);

        setPartOfCellLib.setText("Set");
        setPartOfCellLib.setMinimumSize(new java.awt.Dimension(53, 20));
        setPartOfCellLib.setPreferredSize(new java.awt.Dimension(53, 20));
        setPartOfCellLib.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                setPartOfCellLibActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 2);
        getContentPane().add(setPartOfCellLib, gridBagConstraints);

        clearPartOfCellLib.setText("Clear");
        clearPartOfCellLib.setMinimumSize(new java.awt.Dimension(64, 20));
        clearPartOfCellLib.setPreferredSize(new java.awt.Dimension(64, 20));
        clearPartOfCellLib.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                clearPartOfCellLibActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 4);
        getContentPane().add(clearPartOfCellLib, gridBagConstraints);

        useTechEditor.setText("Use technology editor on this cell");
        useTechEditor.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                useTechEditorActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        getContentPane().add(useTechEditor, gridBagConstraints);

        setUseTechEditor.setText("Set");
        setUseTechEditor.setMinimumSize(new java.awt.Dimension(53, 20));
        setUseTechEditor.setPreferredSize(new java.awt.Dimension(53, 20));
        setUseTechEditor.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                setUseTechEditorActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 2);
        getContentPane().add(setUseTechEditor, gridBagConstraints);

        clearUseTechEditor.setText("Clear");
        clearUseTechEditor.setMinimumSize(new java.awt.Dimension(64, 20));
        clearUseTechEditor.setPreferredSize(new java.awt.Dimension(64, 20));
        clearUseTechEditor.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                clearUseTechEditorActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 4, 4);
        getContentPane().add(clearUseTechEditor, gridBagConstraints);

        expandNewInstances.setText("Expand new instances");
        expansion.add(expandNewInstances);
        expandNewInstances.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                expandNewInstancesActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(expandNewInstances, gridBagConstraints);

        unexpandNewInstances.setText("Unexpand new instances");
        expansion.add(unexpandNewInstances);
        unexpandNewInstances.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                unexpandNewInstancesActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(unexpandNewInstances, gridBagConstraints);

        jLabel2.setText("Characteristic X Spacing:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel2, gridBagConstraints);

        charXSpacing.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(charXSpacing, gridBagConstraints);

        jLabel3.setText("Characteristic Y Spacing:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel3, gridBagConstraints);

        charYSpacing.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(charYSpacing, gridBagConstraints);

        jLabel4.setText("Every cell:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        getContentPane().add(jLabel4, gridBagConstraints);

        editCell.setText("Edit Cell");
        editCell.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                editCellActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(editCell, gridBagConstraints);

        cellName.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cellName, gridBagConstraints);

        rename.setText("Rename Cell");
        rename.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                renameActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(rename, gridBagConstraints);

        delete.setText("Delete Cell");
        delete.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                deleteActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(delete, gridBagConstraints);

        confirmDelete.setText("Confirm Delete");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(confirmDelete, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void renameActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_renameActionPerformed
	{//GEN-HEADEREND:event_renameActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		String cellStr = (String)cellList.getSelectedValue();
		if (cellStr == null) return;
		Cell cell = lib.findNodeProto(cellStr);
		if (cell == null)
		{
			System.out.println("cannot find cell "+cellStr+" here");
			return;
		}
		String newName = cellName.getText();
		CircuitChanges.renameCellInJob(cell, newName);

		loadCellList();
		cellList.setSelectedValue(cell.noLibDescribe(), true);
	}//GEN-LAST:event_renameActionPerformed

	private void deleteActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_deleteActionPerformed
	{//GEN-HEADEREND:event_deleteActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		String cellName = (String)cellList.getSelectedValue();
		if (cellName == null) return;
		Cell cell = lib.findNodeProto(cellName);
		boolean confirm = confirmDelete.isSelected();
		CircuitChanges.deleteCell(cell, confirm);
		loadCellList();
	}//GEN-LAST:event_deleteActionPerformed

	private void editCellActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_editCellActionPerformed
	{//GEN-HEADEREND:event_editCellActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		String cellName = (String)cellList.getSelectedValue();
		if (cellName == null) return;
		Cell cell = lib.findNodeProto(cellName);
		WindowFrame.createEditWindow(cell);
		ok(null);
	}//GEN-LAST:event_editCellActionPerformed

	private void unexpandNewInstancesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_unexpandNewInstancesActionPerformed
	{//GEN-HEADEREND:event_unexpandNewInstancesActionPerformed
		expandNewInstancesActionPerformed(evt);
	}//GEN-LAST:event_unexpandNewInstancesActionPerformed

	private void expandNewInstancesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_expandNewInstancesActionPerformed
	{//GEN-HEADEREND:event_expandNewInstancesActionPerformed
		Cell cell = getSelectedCell();
		if (cell == null) return;
		OldValues ov = (OldValues)origValues.get(cell);
		boolean expanded = expandNewInstances.isSelected();
		if (ov.defExpanded != expanded) ov.defExpandedChanged = true;
		ov.defExpanded = expanded;
	}//GEN-LAST:event_expandNewInstancesActionPerformed

	private void useTechEditorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_useTechEditorActionPerformed
	{//GEN-HEADEREND:event_useTechEditorActionPerformed
		Cell cell = getSelectedCell();
		if (cell == null) return;
		OldValues ov = (OldValues)origValues.get(cell);
		boolean techEditor = useTechEditor.isSelected();
		if (ov.useTechEditor != techEditor) ov.useTechEditorChanged = true;
		ov.useTechEditor = techEditor;
	}//GEN-LAST:event_useTechEditorActionPerformed

	private void partOfCellLibActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_partOfCellLibActionPerformed
	{//GEN-HEADEREND:event_partOfCellLibActionPerformed
		Cell cell = getSelectedCell();
		if (cell == null) return;
		OldValues ov = (OldValues)origValues.get(cell);
		boolean cellLib = partOfCellLib.isSelected();
		if (ov.inCellLib != cellLib) ov.inCellLibChanged = true;
		ov.inCellLib = cellLib;
	}//GEN-LAST:event_partOfCellLibActionPerformed

	private void disallowModInstInCellActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_disallowModInstInCellActionPerformed
	{//GEN-HEADEREND:event_disallowModInstInCellActionPerformed
		Cell cell = getSelectedCell();
		if (cell == null) return;
		OldValues ov = (OldValues)origValues.get(cell);
		boolean disallow = disallowModInstInCell.isSelected();
		if (ov.disInstMod != disallow) ov.disInstModChanged = true;
		ov.disInstMod = disallow;
	}//GEN-LAST:event_disallowModInstInCellActionPerformed

	private void disallowModAnyInCellActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_disallowModAnyInCellActionPerformed
	{//GEN-HEADEREND:event_disallowModAnyInCellActionPerformed
		Cell cell = getSelectedCell();
		if (cell == null) return;
		OldValues ov = (OldValues)origValues.get(cell);
		boolean disallow = disallowModAnyInCell.isSelected();
		if (ov.disAllMod != disallow) ov.disAllModChanged = true;
		ov.disAllMod = disallow;
	}//GEN-LAST:event_disallowModAnyInCellActionPerformed

	private void libraryPopupActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_libraryPopupActionPerformed
	{//GEN-HEADEREND:event_libraryPopupActionPerformed
		loadCellList();
	}//GEN-LAST:event_libraryPopupActionPerformed

	private void setUseTechEditorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_setUseTechEditorActionPerformed
	{//GEN-HEADEREND:event_setUseTechEditorActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			OldValues ov = (OldValues)origValues.get(cell);
			if (!ov.useTechEditor) ov.useTechEditorChanged = true;
			ov.useTechEditor = true;
		}
		cellListClick();
	}//GEN-LAST:event_setUseTechEditorActionPerformed

	private void clearPartOfCellLibActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clearPartOfCellLibActionPerformed
	{//GEN-HEADEREND:event_clearPartOfCellLibActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			OldValues ov = (OldValues)origValues.get(cell);
			if (ov.inCellLib) ov.inCellLibChanged = true;
			ov.inCellLib = false;
		}
		cellListClick();
	}//GEN-LAST:event_clearPartOfCellLibActionPerformed

	private void setPartOfCellLibActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_setPartOfCellLibActionPerformed
	{//GEN-HEADEREND:event_setPartOfCellLibActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			OldValues ov = (OldValues)origValues.get(cell);
			if (!ov.inCellLib) ov.inCellLibChanged = true;
			ov.inCellLib = true;
		}
		cellListClick();
	}//GEN-LAST:event_setPartOfCellLibActionPerformed

	private void clearDisallowModInstInCellActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clearDisallowModInstInCellActionPerformed
	{//GEN-HEADEREND:event_clearDisallowModInstInCellActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			OldValues ov = (OldValues)origValues.get(cell);
			if (ov.disInstMod) ov.disInstModChanged = true;
			ov.disInstMod = false;
		}
		cellListClick();
	}//GEN-LAST:event_clearDisallowModInstInCellActionPerformed

	private void setDisallowModInstInCellActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_setDisallowModInstInCellActionPerformed
	{//GEN-HEADEREND:event_setDisallowModInstInCellActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			OldValues ov = (OldValues)origValues.get(cell);
			if (!ov.disInstMod) ov.disInstModChanged = true;
			ov.disInstMod = true;
		}
		cellListClick();
	}//GEN-LAST:event_setDisallowModInstInCellActionPerformed

	private void clearDisallowModAnyInCellActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clearDisallowModAnyInCellActionPerformed
	{//GEN-HEADEREND:event_clearDisallowModAnyInCellActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			OldValues ov = (OldValues)origValues.get(cell);
			if (ov.disAllMod) ov.disAllModChanged = true;
			ov.disAllMod = false;
		}
		cellListClick();
	}//GEN-LAST:event_clearDisallowModAnyInCellActionPerformed

	private void setDisallowModAnyInCellActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_setDisallowModAnyInCellActionPerformed
	{//GEN-HEADEREND:event_setDisallowModAnyInCellActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			OldValues ov = (OldValues)origValues.get(cell);
			if (!ov.disAllMod) ov.disAllModChanged = true;
			ov.disAllMod = true;
		}
		cellListClick();
	}//GEN-LAST:event_setDisallowModAnyInCellActionPerformed

	private void clearUseTechEditorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clearUseTechEditorActionPerformed
	{//GEN-HEADEREND:event_clearUseTechEditorActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			OldValues ov = (OldValues)origValues.get(cell);
			if (ov.useTechEditor) ov.useTechEditorChanged = true;
			ov.useTechEditor = false;
		}
		cellListClick();
	}//GEN-LAST:event_clearUseTechEditorActionPerformed

	private void cancel(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancel
	{//GEN-HEADEREND:event_cancel
		CellOptionsWindowClosing(null);
	}//GEN-LAST:event_cancel

	private void ok(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ok
	{//GEN-HEADEREND:event_ok
		SetCellOptions job = new SetCellOptions(this);
		CellOptionsWindowClosing(null);
	}//GEN-LAST:event_ok

	/**
	 * Class to set cell options.
	 */
	protected static class SetCellOptions extends Job
	{
		CellOptions dialog;
		
		protected SetCellOptions(CellOptions dialog)
		{
			super("Change Cell Options", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dialog = dialog;
			startJob();
		}

		public void doIt()
		{
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					OldValues ov = (OldValues)dialog.origValues.get(cell);
					if (ov.disAllModChanged)
					{
						if (ov.disAllMod) cell.setAllLocked(); else cell.clearAllLocked();
					}
					if (ov.disInstModChanged)
					{
						if (ov.disInstMod) cell.setInstancesLocked(); else cell.clearInstancesLocked();
					}
					if (ov.inCellLibChanged)
					{
						if (ov.inCellLib) cell.setInCellLibrary(); else cell.clearInCellLibrary();
					}
					if (ov.useTechEditorChanged)
					{
						if (ov.useTechEditor) cell.setInTechnologyLibrary(); else cell.clearInTechnologyLibrary();
					}
					if (ov.defExpandedChanged)
					{
						if (ov.defExpanded) cell.setWantExpanded(); else cell.clearWantExpanded();
					}
					if (ov.characteristicChanged)
					{
						cell.setCharacteristicSpacing(ov.charX, ov.charY);
					}
				}
			}
		}
	}

	/** Closes the dialog */
	private void CellOptionsWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton cancel;
    private JTextField cellName;
    private JScrollPane cellPane;
    private JTextField charXSpacing;
    private JTextField charYSpacing;
    private JButton clearDisallowModAnyInCell;
    private JButton clearDisallowModInstInCell;
    private JButton clearPartOfCellLib;
    private JButton clearUseTechEditor;
    private JCheckBox confirmDelete;
    private JButton delete;
    private JCheckBox disallowModAnyInCell;
    private JCheckBox disallowModInstInCell;
    private JButton editCell;
    private JRadioButton expandNewInstances;
    private ButtonGroup expansion;
    private JLabel jLabel1;
    private JLabel jLabel2;
    private JLabel jLabel3;
    private JLabel jLabel4;
    private JComboBox libraryPopup;
    private JButton ok;
    private JCheckBox partOfCellLib;
    private JButton rename;
    private JButton setDisallowModAnyInCell;
    private JButton setDisallowModInstInCell;
    private JButton setPartOfCellLib;
    private JButton setUseTechEditor;
    private JRadioButton unexpandNewInstances;
    private JCheckBox useTechEditor;
	/**
	* Initializes the GUI.
	* Auto-generated code - any changes you make will disappear.
	*/
	public void initGUI(){
		try {
			preInitGUI();
			expansion = new ButtonGroup();
	
	
			cancel = new JButton();
			ok = new JButton();
			jLabel1 = new JLabel();
			libraryPopup = new JComboBox();
			cellPane = new JScrollPane();
			disallowModAnyInCell = new JCheckBox();
			setDisallowModAnyInCell = new JButton();
			clearDisallowModAnyInCell = new JButton();
			disallowModInstInCell = new JCheckBox();
			setDisallowModInstInCell = new JButton();
			clearDisallowModInstInCell = new JButton();
			partOfCellLib = new JCheckBox();
			setPartOfCellLib = new JButton();
			clearPartOfCellLib = new JButton();
			useTechEditor = new JCheckBox();
			setUseTechEditor = new JButton();
			clearUseTechEditor = new JButton();
			expandNewInstances = new JRadioButton();
			unexpandNewInstances = new JRadioButton();
			jLabel2 = new JLabel();
			charXSpacing = new JTextField();
			jLabel3 = new JLabel();
			charYSpacing = new JTextField();
			jLabel4 = new JLabel();
			editCell = new JButton();
			cellName = new JTextField();
			rename = new JButton();
			delete = new JButton();
			confirmDelete = new JCheckBox();
	
			GridBagLayout thisLayout = new GridBagLayout();
			this.getContentPane().setLayout(thisLayout);
			thisLayout.columnWidths = new int[] {1,1,1,1,1,1};
			thisLayout.rowHeights = new int[] {1,1,1,1,1,1,1,1,1,1};
			thisLayout.columnWeights = new double[] {0.1,0.1,0.1,0.1,0.1,0.1};
			thisLayout.rowWeights = new double[] {0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1};
			this.setTitle("Cell Control");
			this.setName("");
			this.addWindowListener( new WindowAdapter() {
				public void windowClosing(WindowEvent evt) {
					CellOptionsWindowClosing(evt);
				}
			});
	
			cancel.setText("Cancel");
			this.getContentPane().add(cancel, new GridBagConstraints(4, 8, 2, 1, 0.5, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
			cancel.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					cancel(evt);
				}
			});
	
			ok.setText("OK");
			this.getContentPane().add(ok, new GridBagConstraints(4, 9, 2, 1, 0.5, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
			ok.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					ok(evt);
				}
			});
	
			jLabel1.setText("Library:");
			this.getContentPane().add(jLabel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
	
			this.getContentPane().add(libraryPopup, new GridBagConstraints(1, 0, 2, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
			libraryPopup.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					libraryPopupActionPerformed(evt);
				}
			});
	
			cellPane.setMinimumSize(new java.awt.Dimension(200,250));
			cellPane.setPreferredSize(new java.awt.Dimension(200,250));
			this.getContentPane().add(cellPane, new GridBagConstraints(0, 1, 2, 8, 1.0, 1.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
	
			disallowModAnyInCell.setText("Disallow modification of anything in this cell");
			this.getContentPane().add(disallowModAnyInCell, new GridBagConstraints(2, 1, 2, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
			disallowModAnyInCell.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					disallowModAnyInCellActionPerformed(evt);
				}
			});
	
			setDisallowModAnyInCell.setText("Set");
			setDisallowModAnyInCell.setMinimumSize(new java.awt.Dimension(53,20));
			setDisallowModAnyInCell.setPreferredSize(new java.awt.Dimension(53,20));
			this.getContentPane().add(setDisallowModAnyInCell, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
			setDisallowModAnyInCell.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					setDisallowModAnyInCellActionPerformed(evt);
				}
			});
	
			clearDisallowModAnyInCell.setText("Clear");
			clearDisallowModAnyInCell.setMinimumSize(new java.awt.Dimension(64,20));
			clearDisallowModAnyInCell.setPreferredSize(new java.awt.Dimension(64,20));
			this.getContentPane().add(clearDisallowModAnyInCell, new GridBagConstraints(5, 1, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
			clearDisallowModAnyInCell.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					clearDisallowModAnyInCellActionPerformed(evt);
				}
			});
	
			disallowModInstInCell.setText("Disallow modification of instances in this cell");
			this.getContentPane().add(disallowModInstInCell, new GridBagConstraints(2, 2, 2, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
			disallowModInstInCell.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					disallowModInstInCellActionPerformed(evt);
				}
			});
	
			setDisallowModInstInCell.setText("Set");
			setDisallowModInstInCell.setMinimumSize(new java.awt.Dimension(53,20));
			setDisallowModInstInCell.setPreferredSize(new java.awt.Dimension(53,20));
			this.getContentPane().add(setDisallowModInstInCell, new GridBagConstraints(4, 2, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
			setDisallowModInstInCell.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					setDisallowModInstInCellActionPerformed(evt);
				}
			});
	
			clearDisallowModInstInCell.setText("Clear");
			clearDisallowModInstInCell.setMinimumSize(new java.awt.Dimension(64,20));
			clearDisallowModInstInCell.setPreferredSize(new java.awt.Dimension(64,20));
			this.getContentPane().add(clearDisallowModInstInCell, new GridBagConstraints(5, 2, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
			clearDisallowModInstInCell.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					clearDisallowModInstInCellActionPerformed(evt);
				}
			});
	
			partOfCellLib.setText("Part of a cell-library");
			this.getContentPane().add(partOfCellLib, new GridBagConstraints(2, 3, 2, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
			partOfCellLib.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					partOfCellLibActionPerformed(evt);
				}
			});
	
			setPartOfCellLib.setText("Set");
			setPartOfCellLib.setMinimumSize(new java.awt.Dimension(53,20));
			setPartOfCellLib.setPreferredSize(new java.awt.Dimension(53,20));
			this.getContentPane().add(setPartOfCellLib, new GridBagConstraints(4, 3, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
			setPartOfCellLib.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					setPartOfCellLibActionPerformed(evt);
				}
			});
	
			clearPartOfCellLib.setText("Clear");
			clearPartOfCellLib.setMinimumSize(new java.awt.Dimension(64,20));
			clearPartOfCellLib.setPreferredSize(new java.awt.Dimension(64,20));
			this.getContentPane().add(clearPartOfCellLib, new GridBagConstraints(5, 3, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
			clearPartOfCellLib.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					clearPartOfCellLibActionPerformed(evt);
				}
			});
	
			useTechEditor.setText("Use technology editor on this cell");
			this.getContentPane().add(useTechEditor, new GridBagConstraints(2, 4, 2, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
			useTechEditor.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					useTechEditorActionPerformed(evt);
				}
			});
	
			setUseTechEditor.setText("Set");
			setUseTechEditor.setMinimumSize(new java.awt.Dimension(53,20));
			setUseTechEditor.setPreferredSize(new java.awt.Dimension(53,20));
			this.getContentPane().add(setUseTechEditor, new GridBagConstraints(4, 4, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
			setUseTechEditor.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					setUseTechEditorActionPerformed(evt);
				}
			});
	
			clearUseTechEditor.setText("Clear");
			clearUseTechEditor.setMinimumSize(new java.awt.Dimension(64,20));
			clearUseTechEditor.setPreferredSize(new java.awt.Dimension(64,20));
			this.getContentPane().add(clearUseTechEditor, new GridBagConstraints(5, 4, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
			clearUseTechEditor.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					clearUseTechEditorActionPerformed(evt);
				}
			});
	
			expandNewInstances.setText("Expand new instances");
			expansion.add(expandNewInstances);
			this.getContentPane().add(expandNewInstances, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
			expandNewInstances.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					expandNewInstancesActionPerformed(evt);
				}
			});
	
			unexpandNewInstances.setText("Unexpand new instances");
			expansion.add(unexpandNewInstances);
			this.getContentPane().add(unexpandNewInstances, new GridBagConstraints(3, 5, 3, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
			unexpandNewInstances.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					unexpandNewInstancesActionPerformed(evt);
				}
			});
	
			jLabel2.setText("Characteristic X Spacing:");
			this.getContentPane().add(jLabel2, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
	
			charXSpacing.setText(" ");
			this.getContentPane().add(charXSpacing, new GridBagConstraints(3, 6, 2, 1, 0.0, 0.0, 17, 2, new Insets(0, 0, 0, 0), 0, 0));
	
			jLabel3.setText("Characteristic Y Spacing:");
			this.getContentPane().add(jLabel3, new GridBagConstraints(2, 7, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
	
			charYSpacing.setText(" ");
			this.getContentPane().add(charYSpacing, new GridBagConstraints(3, 7, 2, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
	
			jLabel4.setText("Every cell:");
			this.getContentPane().add(jLabel4, new GridBagConstraints(4, 0, 2, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
	
			editCell.setText("Edit Cell");
			this.getContentPane().add(editCell, new GridBagConstraints(2, 8, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
			editCell.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					editCellActionPerformed(evt);
				}
			});
	
			cellName.setText(" ");
			this.getContentPane().add(cellName, new GridBagConstraints(0, 9, 2, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
	
			rename.setText("Rename Cell");
			this.getContentPane().add(rename, new GridBagConstraints(2, 9, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
			rename.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					renameActionPerformed(evt);
				}
			});
	
			delete.setText("Delete Cell");
			this.getContentPane().add(delete, new GridBagConstraints(3, 8, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
			delete.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					deleteActionPerformed(evt);
				}
			});
	
			confirmDelete.setText("Confirm Delete");
			this.getContentPane().add(confirmDelete, new GridBagConstraints(3, 9, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
			pack();
	
			postInitGUI();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/** Add your pre-init code in here 	*/
	public void preInitGUI(){
	}

	/** Add your post-init code in here 	*/
	public void postInitGUI(){
	}

	/** Auto-generated main method */
	public static void main(String[] args){
		showGUI();
	}

	/**
	* This static method creates a new instance of this class and shows
	* it inside a new JFrame, (unless it is already a JFrame).
	*
	* It is a convenience method for showing the GUI, but it can be
	* copied and used as a basis for your own code.	*
	* It is auto-generated code - the body of this method will be
	* re-generated after any changes are made to the GUI.
	* However, if you delete this method it will not be re-created.	*/
	public static void showGUI(){
		try {
			CellOptions inst = new CellOptions();
			inst.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
