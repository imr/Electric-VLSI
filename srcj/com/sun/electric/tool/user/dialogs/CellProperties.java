/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellProperties.java
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
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TempPref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.User;

import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Class to handle the "Cell Properties" dialog.
 */
public class CellProperties extends EDialog
{
	private JList cellList;
	private DefaultListModel cellListModel;
	private HashMap<Cell,PerCellValues> origValues;
	private boolean initialCheckDatesDuringCreation;
	private boolean initialAutoTechnologySwitch;
	private boolean initialPlaceCellCenter;
	private boolean changing = false;

	private static class PerCellValues
	{
        TempPref disAllMod;
        TempPref disInstMod;
        TempPref inCellLib;
        TempPref useTechEditor;
        TempPref defExpanded;
        TempPref charX, charY;
        TempPref frameSize;
        TempPref designerName;
        TempPref technologyName;
        TempPref textCellFont;
        TempPref textCellSize;

        private PerCellValues(Cell cell) {

            // remember the cell's toggle flags
            disAllMod = TempPref.makeBooleanPref(cell.isAllLocked());
            disInstMod = TempPref.makeBooleanPref(cell.isInstancesLocked());
            inCellLib = TempPref.makeBooleanPref(cell.isInCellLibrary());
            useTechEditor = TempPref.makeBooleanPref(cell.isInTechnologyLibrary());
            defExpanded = TempPref.makeBooleanPref(cell.isWantExpanded());

            // remember the characteristic spacing
            double cX = 0, cY = 0;
            Dimension2D spacing = cell.getCharacteristicSpacing();
            if (spacing != null)
            {
                cX = spacing.getWidth();
                cY = spacing.getHeight();
            }
            charX = TempPref.makeDoublePref(cX);
            charY = TempPref.makeDoublePref(cY);

            // remember the frame size
            String fSize = "";
            Variable var = cell.getVar(User.FRAME_SIZE, String.class);
            if (var != null) fSize = (String)var.getObject();
            frameSize = TempPref.makeStringPref(fSize);

            // remember the designer name
            String dName = "";
            var = cell.getVar(User.FRAME_DESIGNER_NAME, String.class);
            if (var != null) dName = (String)var.getObject();
            designerName = TempPref.makeStringPref(dName);

            // remember the technology
            String tName = cell.getTechnology().getTechName();
            technologyName = TempPref.makeStringPref(tName);

            // remember text cell font/size
            String fontName = "DEFAULT FONT";
            var = cell.getVar(Cell.TEXT_CELL_FONT_NAME, String.class);
            if (var != null) fontName = (String)var.getObject();
            textCellFont = TempPref.makeStringPref(fontName);
            int fontSize = User.getDefaultTextCellSize();
            var = cell.getVar(Cell.TEXT_CELL_FONT_SIZE, Integer.class);
            if (var != null) fontSize = ((Integer)var.getObject()).intValue();
            textCellSize = TempPref.makeIntPref(fontSize);
        }
	}

	/** Creates new form Cell Properties */
	public CellProperties(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// cache all information
		origValues = new HashMap<Cell,PerCellValues>();

		// build the cell list
		cellListModel = new DefaultListModel();
		cellList = new JList(cellListModel);
		cellList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		cellPane.setViewportView(cellList);
		cellList.addListSelectionListener(new javax.swing.event.ListSelectionListener()
		{
			public void valueChanged(javax.swing.event.ListSelectionEvent evt) { cellListClick(); }
		});

		// build the technology popup
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext();)
		{
			Technology tech = it.next();
			whichTechnology.addItem(tech.getTechName());
		}

		// build the font popup
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		String [] fontNames = ge.getAvailableFontFamilyNames();
		textCellFont.addItem("DEFAULT FONT");
		for(int i=0; i<fontNames.length; i++)
			textCellFont.addItem(fontNames[i]);

		// initialize frame information
		frameSize.addItem("None");
		frameSize.addItem("Half-A-Size");
		frameSize.addItem("A-Size");
		frameSize.addItem("B-Size");
		frameSize.addItem("C-Size");
		frameSize.addItem("D-Size");
		frameSize.addItem("E-Size");

		// make a popup of libraries
		List<Library> libList = Library.getVisibleLibraries();
		for(Library lib : libList)
		{
			libraryPopup.addItem(lib.getName());
		}
		int curIndex = libList.indexOf(Library.getCurrent());
		if (curIndex >= 0) libraryPopup.setSelectedIndex(curIndex);

		charXSpacing.getDocument().addDocumentListener(new TextFieldListener(this));
		charYSpacing.getDocument().addDocumentListener(new TextFieldListener(this));
		frameDesigner.getDocument().addDocumentListener(new TextFieldListener(this));
		textCellSize.getDocument().addDocumentListener(new TextFieldListener(this));

		loadCellList();
		finishInitialization();
	}

	protected void escapePressed() { cancel(null); }

	/**
	 * Method called when the library popup changes.
	 * Reloads the list of cells.
	 */
	private void loadCellList()
	{
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		if (lib == null) return;
		boolean any = false;
		cellListModel.clear();
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			cellListModel.addElement(cell.noLibDescribe());
			any = true;
		}
		if (any)
		{
			Library curLib = Library.getCurrent();
			if (lib == curLib && Job.getUserInterface().getCurrentCell(curLib) != null)
			{
				cellList.setSelectedValue(Job.getUserInterface().getCurrentCell(curLib).noLibDescribe(), true);
			} else
			{
				cellList.setSelectedIndex(0);
			}
		} else
		{
			cellList.setSelectedValue(null, false);
		}
		cellListClick();
	}

	/**
	 * Method to figure out the current cell.
	 * Examines the library popup and the cell list.
	 * @return the current cell (null if none).
	 */
	private Cell getSelectedCell()
	{
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		String cellName = (String)cellList.getSelectedValue();
		if (cellName == null) return null;
		Cell cell = lib.findNodeProto(cellName);
		return cell;
	}

    /**
     * Lazy caching
     */
    private PerCellValues getPCV(Cell cell)
    {
        PerCellValues pcv = origValues.get(cell);
        if (pcv == null)
        {
            pcv = new PerCellValues(cell);
            origValues.put(cell, pcv);
        }
        return pcv;
    }

	/**
	 * Method called when a cell name is clicked in the list.
	 * Updates the displayed values for that cell.
	 */
	private void cellListClick()
	{
		Cell cell = getSelectedCell();
		if (cell == null) return;
		PerCellValues pcv = getPCV(cell);
		if (pcv == null) return;

		changing = true;
		disallowModAnyInCell.setSelected(pcv.disAllMod.getBoolean());
		disallowModInstInCell.setSelected(pcv.disInstMod.getBoolean());
		partOfCellLib.setSelected(pcv.inCellLib.getBoolean());
		useTechEditor.setSelected(pcv.useTechEditor.getBoolean());
		expandNewInstances.setSelected(pcv.defExpanded.getBoolean());
		unexpandNewInstances.setSelected(!pcv.defExpanded.getBoolean());
		charXSpacing.setText(TextUtils.formatDouble(pcv.charX.getDouble()));
		charYSpacing.setText(TextUtils.formatDouble(pcv.charY.getDouble()));
		frameDesigner.setText(pcv.designerName.getString());
		whichTechnology.setSelectedItem(pcv.technologyName.getString());
		textCellFont.setSelectedItem(pcv.textCellFont.getString());
		textCellSize.setText(Integer.toString(pcv.textCellSize.getInt()));

		frameSize.setSelectedIndex(0);
		frameLandscape.setSelected(true);
		frameTitleBox.setSelected(false);
		String fs = pcv.frameSize.getString();
		if (fs.length() > 0)
		{
			char chr = fs.charAt(0);
			if (chr == 'h') frameSize.setSelectedIndex(1); else
			if (chr == 'a') frameSize.setSelectedIndex(2); else
			if (chr == 'b') frameSize.setSelectedIndex(3); else
			if (chr == 'c') frameSize.setSelectedIndex(4); else
			if (chr == 'd') frameSize.setSelectedIndex(5); else
			if (chr == 'e') frameSize.setSelectedIndex(6);
			frameTitleBox.setSelected(true);
			for(int i=1; i< fs.length(); i++)
			{
				chr = fs.charAt(i);
				if (chr == 'v') framePortrait.setSelected(true); else
					if (chr == 'n') frameTitleBox.setSelected(false);
			}
		}

		changing = false;
	}

	/**
	 * Class to handle special changes to characteristic spacing.
	 */
	private static class TextFieldListener implements DocumentListener
	{
		CellProperties dialog;

		TextFieldListener(CellProperties dialog)
		{
			this.dialog = dialog;
		}

		public void changedUpdate(DocumentEvent e) { dialog.textInfoChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.textInfoChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.textInfoChanged(); }
	}

	private void textInfoChanged()
	{
		if (changing) return;
		Cell cell = getSelectedCell();
		if (cell == null) return;
		PerCellValues pcv = getPCV(cell);
        if (pcv == null) return;

		// get current text fields
		pcv.charX.setDouble(TextUtils.atof(charXSpacing.getText()));
		pcv.charY.setDouble(TextUtils.atof(charYSpacing.getText()));
		pcv.designerName.setString(frameDesigner.getText());
		pcv.textCellSize.setInt(TextUtils.atoi(textCellSize.getText()));
	}

	private void frameInfoChanged()
	{
		if (changing) return;
		Cell cell = getSelectedCell();
		if (cell == null) return;
		PerCellValues pcv = getPCV(cell);
        if (pcv == null) return;

		// get current cell frame information
		String currentFrameSize = "";
		int index = frameSize.getSelectedIndex();
		if (index > 0)
		{
			switch (index)
			{
				case 1: currentFrameSize = "h";   break;
				case 2: currentFrameSize = "a";   break;
				case 3: currentFrameSize = "b";   break;
				case 4: currentFrameSize = "c";   break;
				case 5: currentFrameSize = "d";   break;
				case 6: currentFrameSize = "e";   break;
			}
			if (framePortrait.isSelected()) currentFrameSize += "v";
			if (!frameTitleBox.isSelected()) currentFrameSize += "n";
		} else
		{
			if (frameTitleBox.isSelected()) currentFrameSize = "x";
		}
		pcv.frameSize.setString(currentFrameSize);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents()
    {
        java.awt.GridBagConstraints gridBagConstraints;

        expansion = new javax.swing.ButtonGroup();
        frameOrientation = new javax.swing.ButtonGroup();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
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
        jLabel3 = new javax.swing.JLabel();
        charXSpacing = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        charYSpacing = new javax.swing.JTextField();
        libraryPopup = new javax.swing.JComboBox();
        jPanel1 = new javax.swing.JPanel();
        jLabel14 = new javax.swing.JLabel();
        frameSize = new javax.swing.JComboBox();
        frameLandscape = new javax.swing.JRadioButton();
        framePortrait = new javax.swing.JRadioButton();
        frameTitleBox = new javax.swing.JCheckBox();
        jLabel18 = new javax.swing.JLabel();
        frameDesigner = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        whichTechnology = new javax.swing.JComboBox();
        textCells = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        textCellFont = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        textCellSize = new javax.swing.JTextField();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Cell Properties");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
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
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 11;
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
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        jLabel1.setText("Library:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel1, gridBagConstraints);

        jLabel2.setText("Every cell:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel2, gridBagConstraints);

        cellPane.setMinimumSize(new java.awt.Dimension(200, 250));
        cellPane.setPreferredSize(new java.awt.Dimension(200, 250));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 11;
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
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(disallowModAnyInCell, gridBagConstraints);

        setDisallowModAnyInCell.setText("Set");
        setDisallowModAnyInCell.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                setDisallowModAnyInCellActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(setDisallowModAnyInCell, gridBagConstraints);

        clearDisallowModAnyInCell.setText("Clear");
        clearDisallowModAnyInCell.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                clearDisallowModAnyInCellActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
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
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(disallowModInstInCell, gridBagConstraints);

        setDisallowModInstInCell.setText("Set");
        setDisallowModInstInCell.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                setDisallowModInstInCellActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(setDisallowModInstInCell, gridBagConstraints);

        clearDisallowModInstInCell.setText("Clear");
        clearDisallowModInstInCell.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                clearDisallowModInstInCellActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
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
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(partOfCellLib, gridBagConstraints);

        setPartOfCellLib.setText("Set");
        setPartOfCellLib.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                setPartOfCellLibActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(setPartOfCellLib, gridBagConstraints);

        clearPartOfCellLib.setText("Clear");
        clearPartOfCellLib.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                clearPartOfCellLibActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(clearPartOfCellLib, gridBagConstraints);

        useTechEditor.setText("Part of technology editor library");
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
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(useTechEditor, gridBagConstraints);

        setUseTechEditor.setText("Set");
        setUseTechEditor.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                setUseTechEditorActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(setUseTechEditor, gridBagConstraints);

        clearUseTechEditor.setText("Clear");
        clearUseTechEditor.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                clearUseTechEditorActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(clearUseTechEditor, gridBagConstraints);

        expansion.add(expandNewInstances);
        expandNewInstances.setText("Expand new instances");
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
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(expandNewInstances, gridBagConstraints);

        expansion.add(unexpandNewInstances);
        unexpandNewInstances.setText("Unexpand new instances");
        unexpandNewInstances.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                unexpandNewInstancesActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(unexpandNewInstances, gridBagConstraints);

        jLabel3.setText("Characteristic X Spacing:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(charXSpacing, gridBagConstraints);

        jLabel4.setText("Characteristic Y Spacing:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(charYSpacing, gridBagConstraints);

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
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(libraryPopup, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Cell Frame"));
        jLabel14.setText("Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        jPanel1.add(jLabel14, gridBagConstraints);

        frameSize.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                frameSizeActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(frameSize, gridBagConstraints);

        frameOrientation.add(frameLandscape);
        frameLandscape.setText("Landscape");
        frameLandscape.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                frameLandscapeActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 4);
        jPanel1.add(frameLandscape, gridBagConstraints);

        frameOrientation.add(framePortrait);
        framePortrait.setText("Portrait");
        framePortrait.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                framePortraitActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 0, 4);
        jPanel1.add(framePortrait, gridBagConstraints);

        frameTitleBox.setText("Title Box");
        frameTitleBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                frameTitleBoxActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(frameTitleBox, gridBagConstraints);

        jLabel18.setText("Designer Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 0, 0);
        jPanel1.add(jLabel18, gridBagConstraints);

        frameDesigner.setColumns(15);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        jPanel1.add(frameDesigner, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jPanel1, gridBagConstraints);

        jLabel5.setText("Technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel5, gridBagConstraints);

        whichTechnology.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                whichTechnologyActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(whichTechnology, gridBagConstraints);

        textCells.setLayout(new java.awt.GridBagLayout());

        textCells.setBorder(javax.swing.BorderFactory.createTitledBorder("For Textual Cells"));
        jLabel7.setText("Font:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        textCells.add(jLabel7, gridBagConstraints);

        textCellFont.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                textCellFontActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        textCells.add(textCellFont, gridBagConstraints);

        jLabel6.setText("Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        textCells.add(jLabel6, gridBagConstraints);

        textCellSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        textCells.add(textCellSize, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(textCells, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void textCellFontActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_textCellFontActionPerformed
	{//GEN-HEADEREND:event_textCellFontActionPerformed
		Cell cell = getSelectedCell();
		if (cell == null) return;
		PerCellValues pcv = getPCV(cell);
		pcv.textCellFont.setString((String)textCellFont.getSelectedItem());
	}//GEN-LAST:event_textCellFontActionPerformed

	private void whichTechnologyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_whichTechnologyActionPerformed
	{//GEN-HEADEREND:event_whichTechnologyActionPerformed
		Cell cell = getSelectedCell();
		if (cell == null) return;
		PerCellValues pcv = getPCV(cell);
		pcv.technologyName.setString((String)whichTechnology.getSelectedItem());
	}//GEN-LAST:event_whichTechnologyActionPerformed

	private void frameTitleBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_frameTitleBoxActionPerformed
	{//GEN-HEADEREND:event_frameTitleBoxActionPerformed
		frameInfoChanged();
	}//GEN-LAST:event_frameTitleBoxActionPerformed

	private void framePortraitActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_framePortraitActionPerformed
	{//GEN-HEADEREND:event_framePortraitActionPerformed
		frameInfoChanged();
	}//GEN-LAST:event_framePortraitActionPerformed

	private void frameLandscapeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_frameLandscapeActionPerformed
	{//GEN-HEADEREND:event_frameLandscapeActionPerformed
		frameInfoChanged();
	}//GEN-LAST:event_frameLandscapeActionPerformed

	private void frameSizeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_frameSizeActionPerformed
	{//GEN-HEADEREND:event_frameSizeActionPerformed
		frameInfoChanged();
	}//GEN-LAST:event_frameSizeActionPerformed

	private void unexpandNewInstancesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_unexpandNewInstancesActionPerformed
	{//GEN-HEADEREND:event_unexpandNewInstancesActionPerformed
		expandNewInstancesActionPerformed(evt);
	}//GEN-LAST:event_unexpandNewInstancesActionPerformed

	private void expandNewInstancesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_expandNewInstancesActionPerformed
	{//GEN-HEADEREND:event_expandNewInstancesActionPerformed
		Cell cell = getSelectedCell();
		if (cell == null) return;
		PerCellValues pcv = getPCV(cell);
		pcv.defExpanded.setBoolean(expandNewInstances.isSelected());
	}//GEN-LAST:event_expandNewInstancesActionPerformed

	private void libraryPopupActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_libraryPopupActionPerformed
	{//GEN-HEADEREND:event_libraryPopupActionPerformed
		loadCellList();
	}//GEN-LAST:event_libraryPopupActionPerformed

	private void useTechEditorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_useTechEditorActionPerformed
	{//GEN-HEADEREND:event_useTechEditorActionPerformed
		Cell cell = getSelectedCell();
		if (cell == null) return;
		PerCellValues pcv = getPCV(cell);
		pcv.useTechEditor.setBoolean(useTechEditor.isSelected());
	}//GEN-LAST:event_useTechEditorActionPerformed

	private void partOfCellLibActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_partOfCellLibActionPerformed
	{//GEN-HEADEREND:event_partOfCellLibActionPerformed
		Cell cell = getSelectedCell();
		if (cell == null) return;
		PerCellValues pcv = getPCV(cell);
		pcv.inCellLib.setBoolean(partOfCellLib.isSelected());
	}//GEN-LAST:event_partOfCellLibActionPerformed

	private void disallowModInstInCellActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_disallowModInstInCellActionPerformed
	{//GEN-HEADEREND:event_disallowModInstInCellActionPerformed
		Cell cell = getSelectedCell();
		if (cell == null) return;
		PerCellValues pcv = getPCV(cell);
		pcv.disInstMod.setBoolean(disallowModInstInCell.isSelected());
	}//GEN-LAST:event_disallowModInstInCellActionPerformed

	private void disallowModAnyInCellActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_disallowModAnyInCellActionPerformed
	{//GEN-HEADEREND:event_disallowModAnyInCellActionPerformed
		Cell cell = getSelectedCell();
		if (cell == null) return;
		PerCellValues pcv = getPCV(cell);
		pcv.disAllMod.setBoolean(disallowModAnyInCell.isSelected());
	}//GEN-LAST:event_disallowModAnyInCellActionPerformed

	private void clearUseTechEditorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clearUseTechEditorActionPerformed
	{//GEN-HEADEREND:event_clearUseTechEditorActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			PerCellValues pcv = getPCV(cell);
			pcv.useTechEditor.setBoolean(false);
		}
		cellListClick();
	}//GEN-LAST:event_clearUseTechEditorActionPerformed

	private void setUseTechEditorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_setUseTechEditorActionPerformed
	{//GEN-HEADEREND:event_setUseTechEditorActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			PerCellValues pcv = getPCV(cell);
			pcv.useTechEditor.setBoolean(true);
		}
		cellListClick();
	}//GEN-LAST:event_setUseTechEditorActionPerformed

	private void clearPartOfCellLibActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clearPartOfCellLibActionPerformed
	{//GEN-HEADEREND:event_clearPartOfCellLibActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			PerCellValues pcv = getPCV(cell);
			pcv.inCellLib.setBoolean(false);
		}
		cellListClick();
	}//GEN-LAST:event_clearPartOfCellLibActionPerformed

	private void setPartOfCellLibActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_setPartOfCellLibActionPerformed
	{//GEN-HEADEREND:event_setPartOfCellLibActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			PerCellValues pcv = getPCV(cell);
			pcv.inCellLib.setBoolean(true);
		}
		cellListClick();
	}//GEN-LAST:event_setPartOfCellLibActionPerformed

	private void clearDisallowModInstInCellActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clearDisallowModInstInCellActionPerformed
	{//GEN-HEADEREND:event_clearDisallowModInstInCellActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			PerCellValues pcv = getPCV(cell);
			pcv.disInstMod.setBoolean(false);
		}
		cellListClick();
	}//GEN-LAST:event_clearDisallowModInstInCellActionPerformed

	private void setDisallowModInstInCellActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_setDisallowModInstInCellActionPerformed
	{//GEN-HEADEREND:event_setDisallowModInstInCellActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			PerCellValues pcv = getPCV(cell);
			pcv.disInstMod.setBoolean(true);
		}
		cellListClick();
	}//GEN-LAST:event_setDisallowModInstInCellActionPerformed

	private void clearDisallowModAnyInCellActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clearDisallowModAnyInCellActionPerformed
	{//GEN-HEADEREND:event_clearDisallowModAnyInCellActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			PerCellValues pcv = getPCV(cell);
			pcv.disAllMod.setBoolean(false);
		}
		cellListClick();
	}//GEN-LAST:event_clearDisallowModAnyInCellActionPerformed

	private void setDisallowModAnyInCellActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_setDisallowModAnyInCellActionPerformed
	{//GEN-HEADEREND:event_setDisallowModAnyInCellActionPerformed
		String libName = (String)libraryPopup.getSelectedItem();
		Library lib = Library.findLibrary(libName);
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			PerCellValues pcv = getPCV(cell);
			pcv.disAllMod.setBoolean(true);
		}
		cellListClick();
	}//GEN-LAST:event_setDisallowModAnyInCellActionPerformed

	private void cancel(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancel
	{//GEN-HEADEREND:event_cancel
		closeDialog(null);
	}//GEN-LAST:event_cancel

	private void ok(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ok
	{//GEN-HEADEREND:event_ok
		List<Cell> cells = new ArrayList<Cell>();
		List<Boolean> disAllMod = new ArrayList<Boolean>();
		List<Boolean> disAllModFactory = new ArrayList<Boolean>();
		List<Boolean> disInstMod = new ArrayList<Boolean>();
		List<Boolean> disInstModFactory = new ArrayList<Boolean>();
		List<Boolean> inCellLib = new ArrayList<Boolean>();
		List<Boolean> inCellLibFactory = new ArrayList<Boolean>();
		List<Boolean> useTechEditor = new ArrayList<Boolean>();
		List<Boolean> useTechEditorFactory = new ArrayList<Boolean>();
		List<Boolean> defExpanded = new ArrayList<Boolean>();
		List<Boolean> defExpandedFactory = new ArrayList<Boolean>();
		List<Double> charX = new ArrayList<Double>();
		List<Double> charXFactory = new ArrayList<Double>();
		List<Double> charY = new ArrayList<Double>();
		List<Double> charYFactory = new ArrayList<Double>();
		List<String> frameSize = new ArrayList<String>();
		List<String> frameSizeFactory = new ArrayList<String>();
		List<String> designerName = new ArrayList<String>();
		List<String> designerNameFactory = new ArrayList<String>();
		List<String> technologyName = new ArrayList<String>();
		List<String> technologyNameFactory = new ArrayList<String>();
		List<String> textCellFont = new ArrayList<String>();
		List<String> textCellFontFactory = new ArrayList<String>();
		List<Integer> textCellSize = new ArrayList<Integer>();
		List<Integer> textCellSizeFactory = new ArrayList<Integer>();

		for (Cell cell : origValues.keySet())
		{
            cells.add(cell);
			PerCellValues pcv = getPCV(cell);
			disAllMod.add(new Boolean(pcv.disAllMod.getBoolean()));
			disAllModFactory.add(new Boolean(pcv.disAllMod.getBooleanFactoryValue()));

			disInstMod.add(new Boolean(pcv.disInstMod.getBoolean()));
			disInstModFactory.add(new Boolean(pcv.disInstMod.getBooleanFactoryValue()));
			inCellLib.add(new Boolean(pcv.inCellLib.getBoolean()));
			inCellLibFactory.add(new Boolean(pcv.inCellLib.getBooleanFactoryValue()));
			useTechEditor.add(new Boolean(pcv.useTechEditor.getBoolean()));
			useTechEditorFactory.add(new Boolean(pcv.useTechEditor.getBooleanFactoryValue()));
			defExpanded.add(new Boolean(pcv.defExpanded.getBoolean()));
			defExpandedFactory.add(new Boolean(pcv.defExpanded.getBooleanFactoryValue()));

			charX.add(new Double(pcv.charX.getDouble()));
			charXFactory.add((Double)pcv.charX.getFactoryValue());
			charY.add(new Double(pcv.charY.getDouble()));
			charYFactory.add((Double)pcv.charY.getFactoryValue());

			frameSize.add(pcv.frameSize.getString());
			frameSizeFactory.add((String)pcv.frameSize.getFactoryValue());
			String dn = pcv.designerName.getString();
			if (pcv.frameSize.getString().length() != 0)
			{
				if (dn.length() == 0)
					dn = System.getProperty("user.name");
			}
			designerName.add(dn);
			designerNameFactory.add((String)pcv.designerName.getFactoryValue());
			technologyName.add(pcv.technologyName.getString());
			technologyNameFactory.add((String)pcv.technologyName.getFactoryValue());
			textCellFont.add(pcv.textCellFont.getString());
			textCellFontFactory.add((String)pcv.textCellFont.getFactoryValue());

			textCellSize.add(new Integer(pcv.textCellSize.getInt()));
			textCellSizeFactory.add(new Integer(pcv.textCellSize.getIntFactoryValue()));
		}

		SetCellOptions job = new SetCellOptions(
			cells,
			disAllMod, disAllModFactory,
			disInstMod, disInstModFactory,
			inCellLib, inCellLibFactory,
			useTechEditor, useTechEditorFactory,
			defExpanded, defExpandedFactory,
			charX, charXFactory,
			charY, charYFactory,
			frameSize, frameSizeFactory,
			designerName, designerNameFactory,
			technologyName, technologyNameFactory,
			textCellFont, textCellFontFactory,
			textCellSize, textCellSizeFactory);
		closeDialog(null);
	}//GEN-LAST:event_ok

	/**
	 * Class to set cell options.
	 */
	private static class SetCellOptions extends Job
	{
		private List<Cell> cells;
		private List<Boolean> disAllMod, disAllModFactory;
		private List<Boolean> disInstMod, disInstModFactory;
		private List<Boolean> inCellLib, inCellLibFactory;
		private List<Boolean> useTechEditor, useTechEditorFactory;
		private List<Boolean> defExpanded, defExpandedFactory;
		private List<Double> charX, charXFactory;
		private List<Double> charY, charYFactory;
		private List<String> frameSize, frameSizeFactory;
		private List<String> designerName, designerNameFactory;
		private List<String> technologyName, technologyNameFactory;
		private List<String> textCellFont, textCellFontFactory;
		private List<Integer> textCellSize, textCellSizeFactory;
	
		private SetCellOptions(
			List<Cell> cells,
			List<Boolean> disAllMod,     List<Boolean> disAllModFactory,
			List<Boolean> disInstMod,    List<Boolean> disInstModFactory,
			List<Boolean> inCellLib,     List<Boolean> inCellLibFactory,
			List<Boolean> useTechEditor, List<Boolean> useTechEditorFactory,
			List<Boolean> defExpanded,   List<Boolean> defExpandedFactory,
			List<Double> charX,          List<Double> charXFactory,
			List<Double> charY,          List<Double> charYFactory,
			List<String> frameSize,      List<String> frameSizeFactory,
			List<String> designerName,   List<String> designerNameFactory,
			List<String> technologyName, List<String> technologyNameFactory,
			List<String> textCellFont,   List<String> textCellFontFactory,
			List<Integer> textCellSize,  List<Integer> textCellSizeFactory)
		{
			super("Change Cell Options", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cells = cells;
			this.disAllMod = disAllMod;            this.disAllModFactory = disAllModFactory;
			this.disInstMod = disInstMod;          this.disInstModFactory = disInstModFactory;
			this.inCellLib = inCellLib;            this.inCellLibFactory = inCellLibFactory;
			this.useTechEditor = useTechEditor;    this.useTechEditorFactory = useTechEditorFactory;
			this.defExpanded = defExpanded;        this.defExpandedFactory = defExpandedFactory;
			this.charX = charX;                    this.charXFactory = charXFactory;
			this.charY = charY;                    this.charYFactory = charYFactory;
			this.frameSize = frameSize;            this.frameSizeFactory = frameSizeFactory;
			this.designerName = designerName;      this.designerNameFactory = designerNameFactory;
			this.technologyName = technologyName;  this.technologyNameFactory = technologyNameFactory;
			this.textCellFont = textCellFont;      this.textCellFontFactory = textCellFontFactory;
			this.textCellSize = textCellSize;      this.textCellSizeFactory = textCellSizeFactory;
			startJob();
		}

		public boolean doIt() throws JobException
		{
            for (int i=0; i<cells.size(); i++)
			{
				Cell cell = cells.get(i);
				if (disAllMod.get(i).booleanValue() != disAllModFactory.get(i).booleanValue())
				{
					if (disAllMod.get(i).booleanValue()) cell.setAllLocked(); else cell.clearAllLocked();
				}
				if (disInstMod.get(i).booleanValue() != disInstModFactory.get(i).booleanValue())
				{
					if (disInstMod.get(i).booleanValue()) cell.setInstancesLocked(); else cell.clearInstancesLocked();
				}
				if (inCellLib.get(i).booleanValue() != inCellLibFactory.get(i).booleanValue())
				{
					if (inCellLib.get(i).booleanValue()) cell.setInCellLibrary(); else cell.clearInCellLibrary();
				}
				if (useTechEditor.get(i).booleanValue() != useTechEditorFactory.get(i).booleanValue())
				{
					if (useTechEditor.get(i).booleanValue()) cell.setInTechnologyLibrary(); else cell.clearInTechnologyLibrary();
				}
				if (defExpanded.get(i).booleanValue() != defExpandedFactory.get(i).booleanValue())
				{
					if (defExpanded.get(i).booleanValue()) cell.setWantExpanded(); else cell.clearWantExpanded();
				}
				if (charX.get(i).doubleValue() != charXFactory.get(i).doubleValue() ||
					charY.get(i).doubleValue() != charYFactory.get(i).doubleValue())
				{
					cell.setCharacteristicSpacing(charX.get(i).doubleValue(), charY.get(i).doubleValue());
				}
				if (!frameSize.get(i).equals(frameSizeFactory.get(i)))
				{
					cell.newVar(User.FRAME_SIZE, frameSize.get(i));
				}
				if (!designerName.get(i).equals(designerNameFactory.get(i)))
				{
					cell.newVar(User.FRAME_DESIGNER_NAME, designerName.get(i));
                }
				if (!technologyName.get(i).equals(technologyNameFactory.get(i)))
				{
					Technology tech = Technology.findTechnology(technologyName.get(i));
					if (tech != null) cell.setTechnology(tech);
                }
				if (!textCellFont.get(i).equals(textCellFontFactory.get(i)))
				{
					cell.newVar(Cell.TEXT_CELL_FONT_NAME, textCellFont.get(i));
				}
				if (textCellSize.get(i).intValue() != textCellSizeFactory.get(i).intValue())
				{
					cell.newVar(Cell.TEXT_CELL_FONT_SIZE, textCellSize.get(i));
				}
			}
			return true;
		}
	}

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancel;
    private javax.swing.JScrollPane cellPane;
    private javax.swing.JTextField charXSpacing;
    private javax.swing.JTextField charYSpacing;
    private javax.swing.JButton clearDisallowModAnyInCell;
    private javax.swing.JButton clearDisallowModInstInCell;
    private javax.swing.JButton clearPartOfCellLib;
    private javax.swing.JButton clearUseTechEditor;
    private javax.swing.JCheckBox disallowModAnyInCell;
    private javax.swing.JCheckBox disallowModInstInCell;
    private javax.swing.JRadioButton expandNewInstances;
    private javax.swing.ButtonGroup expansion;
    private javax.swing.JTextField frameDesigner;
    private javax.swing.JRadioButton frameLandscape;
    private javax.swing.ButtonGroup frameOrientation;
    private javax.swing.JRadioButton framePortrait;
    private javax.swing.JComboBox frameSize;
    private javax.swing.JCheckBox frameTitleBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JComboBox libraryPopup;
    private javax.swing.JButton ok;
    private javax.swing.JCheckBox partOfCellLib;
    private javax.swing.JButton setDisallowModAnyInCell;
    private javax.swing.JButton setDisallowModInstInCell;
    private javax.swing.JButton setPartOfCellLib;
    private javax.swing.JButton setUseTechEditor;
    private javax.swing.JComboBox textCellFont;
    private javax.swing.JTextField textCellSize;
    private javax.swing.JPanel textCells;
    private javax.swing.JRadioButton unexpandNewInstances;
    private javax.swing.JCheckBox useTechEditor;
    private javax.swing.JComboBox whichTechnology;
    // End of variables declaration//GEN-END:variables

}
