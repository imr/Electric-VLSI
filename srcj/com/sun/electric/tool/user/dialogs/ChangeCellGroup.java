/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ChangeCellGroup.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Class to handle the request to change a cell's group.
 */
public class ChangeCellGroup extends EDialog {

	private static Preferences prefs = Preferences.userNodeForPackage(ChangeCellGroup.class);
    private static final String selectedRadioButton = "ChangeCellGroup-WhichMoveType";

    private List<Cell> cellsToRegroup;              // cells to regroup
    private Library initialLibrary;                 // initial destination library
    private List<Cell.CellGroup> cellGroups;        // list of cell groups

    /** Creates new form ChangeCellGroup */
    public ChangeCellGroup(java.awt.Frame parent, boolean modal, List<Cell> cellsToRegroup, Library initialLibrary) {
        super(parent, modal);
        setTitle("Change Cell Group");
        this.cellsToRegroup = cellsToRegroup;
        this.initialLibrary = initialLibrary;
        cellGroups = new ArrayList<Cell.CellGroup>();

        initComponents();

        cellNameLabel.setText("Change Cell Group for: "+cellsToRegroup.get(0));

        // populate cell group combo box
        populateCellGroupsComboBox(cellsToRegroup, initialLibrary);

        // get last state of dialog
        int selected = prefs.getInt(selectedRadioButton, 0);
        cellGroupsComboBox.setEnabled(false);
        switch(selected) {
            case 0: { moveOwnCellGroup.setSelected(true); break; }
            case 1: { moveToCellGroup.setSelected(true); break; }
        }
        if (cellsToRegroup.size() > 1)
        {
        	moveToCellGroup.setSelected(true);
        	moveOwnCellGroup.setEnabled(false);
        }

        pack();
		finishInitialization();
    }

    private void populateCellGroupsComboBox(List<Cell> cellsToRegroup, Library lib) {
        cellGroups.clear();
        cellGroupsComboBox.removeAllItems();
        for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
            Cell c = it.next();
            Cell.CellGroup cg = c.getCellGroup();
            if (cg == null) continue;
            boolean inList = false;
            for(Cell rgCell : cellsToRegroup)
            {
                if (cg == rgCell.getCellGroup()) { inList = true;   break; }
            }
            if (inList) continue;
            if (!cellGroups.contains(cg)) {
                cellGroups.add(cg);
            }
        }
        // sort cell groups
        Collections.sort(cellGroups, new CellGroupComparator());
        for (Cell.CellGroup cg : cellGroups) {
            cellGroupsComboBox.addItem(cg.getName());
        }
    }

    private static class CellGroupComparator implements Comparator<Cell.CellGroup>
    {
        public int compare(Cell.CellGroup cg1, Cell.CellGroup cg2) {
            String s1 = cg1.getName();
            String s2 = cg2.getName();
            return s1.compareTo(s2);
        }
    }

    private static class ChangeCellGroupJob extends Job
    {
        private List<Cell> cellsToRegroup;
        private Cell newGroupCell;

        ChangeCellGroupJob(List<Cell> cellsToRegroup, Cell newGroupCell) {
            super("Change Cell Group", User.getUserTool(), Job.Type.CHANGE, cellsToRegroup.get(0), cellsToRegroup.get(0), Job.Priority.USER);
            this.cellsToRegroup = cellsToRegroup;
            this.newGroupCell = newGroupCell;
            startJob();
        }

        public boolean doIt() throws JobException {
        	Cell.CellGroup newGroup = null;
        	if (newGroupCell != null) newGroup = newGroupCell.getCellGroup();
        	for(Cell cell : cellsToRegroup)
        	{
                if (newGroup != null && cell.getCellGroup() == newGroup) continue;
        		cell.setCellGroup(newGroup);
        	}
            return true;
        }
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroup1 = new javax.swing.ButtonGroup();
        mainPanel = new javax.swing.JPanel();
        cellNameLabel = new javax.swing.JLabel();
        moveOwnCellGroup = new javax.swing.JRadioButton();
        moveToCellGroup = new javax.swing.JRadioButton();
        cellGroupsComboBox = new javax.swing.JComboBox();
        jPanel1 = new javax.swing.JPanel();
        apply = new javax.swing.JButton();
        cancel = new javax.swing.JButton();

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        mainPanel.setLayout(new java.awt.GridBagLayout());

        cellNameLabel.setText("cellName");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 10, 4, 4);
        mainPanel.add(cellNameLabel, gridBagConstraints);

        moveOwnCellGroup.setText("Move to it's own cell group");
        buttonGroup1.add(moveOwnCellGroup);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        mainPanel.add(moveOwnCellGroup, gridBagConstraints);

        moveToCellGroup.setText("Move to Cell Group: ");
        buttonGroup1.add(moveToCellGroup);
        moveToCellGroup.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                moveToCellGroupItemStateChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        mainPanel.add(moveToCellGroup, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        mainPanel.add(cellGroupsComboBox, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        apply.setText("OK");
        apply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(apply, gridBagConstraints);

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(cancel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        mainPanel.add(jPanel1, gridBagConstraints);

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        pack();
    }//GEN-END:initComponents

    private void cancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelActionPerformed
        closeDialog(null);        
    }//GEN-LAST:event_cancelActionPerformed

    private void moveToCellGroupItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_moveToCellGroupItemStateChanged
        boolean selected = moveToCellGroup.isSelected();
        cellGroupsComboBox.setEnabled(selected);
    }//GEN-LAST:event_moveToCellGroupItemStateChanged

    private void applyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_applyActionPerformed
        Cell newGroupCell = null;
        boolean doIt = true;

        if (moveOwnCellGroup.isSelected())
        {
            // if already only cell in group, do nothing
        	Cell cell = cellsToRegroup.get(0);
            if (cell.getCellGroup() != null && cell.getCellGroup().getNumCells() == 1) doIt = false;
        } else if (moveToCellGroup.isSelected())
        {
            // get group to move to
            int selected = cellGroupsComboBox.getSelectedIndex();
            Cell.CellGroup newGroup = (Cell.CellGroup)(cellGroups.toArray()[selected]);
            newGroupCell = newGroup.getCells().next();
        }

        if (doIt) {
            ChangeCellGroupJob job = new ChangeCellGroupJob(cellsToRegroup, newGroupCell);
        }

        closeDialog(null);

    }//GEN-LAST:event_applyActionPerformed
    
    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
        // save settings
        int selected = 0;
        selected = moveOwnCellGroup.isSelected() ? 0 : selected;
        selected = moveToCellGroup.isSelected() ? 1 : selected;
        prefs.putInt(selectedRadioButton, selected);

        setVisible(false);
        dispose();
    }//GEN-LAST:event_closeDialog
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton apply;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton cancel;
    private javax.swing.JComboBox cellGroupsComboBox;
    private javax.swing.JLabel cellNameLabel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JRadioButton moveOwnCellGroup;
    private javax.swing.JRadioButton moveToCellGroup;
    // End of variables declaration//GEN-END:variables
    
}
