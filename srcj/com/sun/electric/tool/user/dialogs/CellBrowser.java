/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellBrowser.java
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
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.change.Undo;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.PaletteFrame;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.CircuitChanges;

import javax.swing.*;
import java.awt.*;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 *
 * @author  gainsley
 *
 * <p>The CellBrowser is a general purpose browser that can browse
 * by Library, View, and Cell.  It can filter Cell names using a regular
 * expression.
 * <p>The DoAction class is checked to adjust several configurable features:
 * <p>- What action to perform
 * <p>- What buttons are available
 * <p>- What additional JComponents are included
 * <p>- If the Cell List is multi- or single-select.
 */
public class CellBrowser extends EDialog implements DatabaseChangeListener {

    private static Preferences prefs = Preferences.userNodeForPackage(CellBrowser.class);

    private static final String prefFilter = "CellBrowser-Filter";
    private static final String prefSelectedLib = "CellBrowser-SelectedLib";
    private static final String prefSelectedView = "CellBrowser-SelectedView";
    private static final String prefSelectedCell = "CellBrowser-SelectedCell";
    private static final String prefEditInNewWindow = "CellBrowser-EditInNewWindow";

    private String lastSelectedLib = null;
    private String lastSelectedView = null;
    private String lastSelectedCell = null;
    private String lastFilter = "";
    private static Pattern lastPattern = null;


    private DoAction action;
    private List cellList = null;                       // list of cells displayed
    private List cellListNames = null;                  // list of cells by name displayed (matches cellList)


    public static class DoAction {
        public String name;

        private DoAction(String name) {
            this.name = name;
        }

        public String toString() { return name; }

        public static final DoAction newInstance = new DoAction("New Cell Instance");
        public static final DoAction editCell = new DoAction("Edit Cell");
        public static final DoAction renameCell = new DoAction("Rename Cell");
        public static final DoAction duplicateCell = new DoAction("Duplicate Cell");
        public static final DoAction deleteCell = new DoAction("Delete Cell");
    }

    /** Creates new form CellBrowser */
    public CellBrowser(java.awt.Frame parent, boolean modal, DoAction action) {
        super(parent, modal);
        this.action = action;

        initComponents();                       // init components (netbeans generated method)
        setTitle(action.name);                  // set the dialog title
        doAction.setText(action.name);          // set the action button's text
        getRootPane().setDefaultButton(doAction); // return will do action
        lastFilter = prefs.get(action+prefFilter, "");
        lastPattern = Pattern.compile(lastFilter);
        lastSelectedCell = prefs.get(action+prefSelectedCell, null);
        cellFilter.setText(lastFilter);         // restore last filter
        initComboBoxes();                       // set up the combo boxes
        initExtras();                           // set up an extra components
        pack();

        Undo.addDatabaseChangeListener(this);
    }

    public void databaseEndChangeBatch(Undo.ChangeBatch batch) {
        if (!isVisible()) return;
        // would take too long to search for change we care about, just reload it
        updateCellList();
    }

    public void databaseChanged(Undo.Change evt) {}

	protected void escapePressed() { cancelActionPerformed(null); }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        jLabel1 = new javax.swing.JLabel();
        libraryComboBox = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        viewComboBox = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        cellFilter = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        jPanel1 = new javax.swing.JPanel();
        cancel = new javax.swing.JButton();
        doAction = new javax.swing.JButton();
        done = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList();
        extrasPanel = new javax.swing.JPanel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        jLabel1.setText("Library:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel1, gridBagConstraints);

        libraryComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        libraryComboBox.setMinimumSize(new java.awt.Dimension(32, 20));
        libraryComboBox.setPreferredSize(new java.awt.Dimension(32, 20));
        libraryComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                libraryComboBoxItemStateChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        getContentPane().add(libraryComboBox, gridBagConstraints);

        jLabel2.setText("View:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel2, gridBagConstraints);

        viewComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        viewComboBox.setMinimumSize(new java.awt.Dimension(32, 20));
        viewComboBox.setPreferredSize(new java.awt.Dimension(32, 20));
        viewComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                viewComboBoxItemStateChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        getContentPane().add(viewComboBox, gridBagConstraints);

        jLabel3.setText("Filter:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(7, 4, 7, 4);
        getContentPane().add(jLabel3, gridBagConstraints);

        cellFilter.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                cellFilterKeyReleased(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        getContentPane().add(cellFilter, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(6, 6, 6, 6);
        getContentPane().add(jSeparator1, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        jPanel1.add(cancel, gridBagConstraints);

        doAction.setText("doAction");
        doAction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doActionActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        jPanel1.add(doAction, gridBagConstraints);

        done.setText("Done");
        done.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doneActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(done, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(jPanel1, gridBagConstraints);

        jScrollPane1.setPreferredSize(new java.awt.Dimension(230, 350));
        jList1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jList1.setPreferredSize(new java.awt.Dimension(150, 300));
        jList1.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jList1ValueChanged(evt);
            }
        });
        jList1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jList1MouseClicked(evt);
            }
        });

        jScrollPane1.setViewportView(jList1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        getContentPane().add(jScrollPane1, gridBagConstraints);

        extrasPanel.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(extrasPanel, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

    // ---------------------------- Extra Initialization ---------------------------------

    /** Initialize Combo Boxes */
    private void initComboBoxes() {

        // initialize library combo box with choices
        libraryComboBox.addItem("All");

        lastSelectedLib = prefs.get(action+prefSelectedLib, null);
        int curLibIndex = -1;
        int curIndex = -1;
        int i = 1;

        List libList = Library.getVisibleLibrariesSortedByName();
        for (Iterator it = libList.iterator(); it.hasNext(); ) {
            Library lib = (Library)it.next();
            libraryComboBox.addItem(lib.getName());
            if (lib.getName().equals(lastSelectedLib))
                curIndex = i;               // see if this is the last selected lib
            if (lib == Library.getCurrent())
                curLibIndex = i;            // see if this is the current lib
            i++;
        }
        if (curIndex == -1) curIndex = curLibIndex;
        if (curIndex > 0) libraryComboBox.setSelectedIndex(curIndex);

        viewComboBox.addItem("All");

        lastSelectedView = prefs.get(action+prefSelectedView, null);
        curIndex = -1;
        i = 1;

        List viewList = View.getOrderedViews();
        for (Iterator it = viewList.iterator(); it.hasNext(); ) {
            View view = (View)it.next();
            viewComboBox.addItem(view.getFullName());
            if (view.getFullName().equals(lastSelectedView))
                curIndex = i;               // see if this is the last selected view
            i++;
        }
        if (curIndex == -1) curIndex = 0;
        viewComboBox.setSelectedIndex(curIndex);

    }

    // ---- Edit Cell extras ----
    private JCheckBox editInNewWindow;
    // ---- Rename Cell extras ----
    private JLabel newCellNameLabel;
    private JTextField newCellName;

    private void initExtras() {
        // extras should be added in gridboxlayout inside JPanel "extrasPanel",
        //  starting with gridx = 0, gridy=0.
        java.awt.GridBagConstraints gridBagConstraints;

        if (action == DoAction.newInstance) {
            // remove done button, this is a one-shot action button
            jPanel1.remove(done);
        }
        else if (action == DoAction.editCell) {
            // add in a check box to open in a new window
            boolean checked = prefs.getBoolean(prefEditInNewWindow, false);
            editInNewWindow = new JCheckBox("Edit in New Window", checked);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            extrasPanel.add(editInNewWindow, gridBagConstraints);

            // remove done button, this is a one-shot action button
            jPanel1.remove(done);
            // however, it would be cool to be able to pick several cells to edit
            jList1.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }
        else if (action == DoAction.renameCell) {
            // add in a text box for the new name
            newCellNameLabel = new JLabel("New Cell Name:  ");
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            extrasPanel.add(newCellNameLabel, gridBagConstraints);

            newCellName = new JTextField();
            if (lastSelectedCell != null) {
                String nameOnly = lastSelectedCell.replaceAll("\\{.*?\\}", "");
                newCellName.setText(nameOnly);
            }
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            extrasPanel.add(newCellName, gridBagConstraints);

            // not a one-shot action:
            doAction.setText("Apply Rename");
        }
        else if (action == DoAction.deleteCell) {
            // set current cell as the default
            Cell cell = WindowFrame.getCurrentCell();
            setCell(cell);

            // not a one-shot action:
            jList1.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            doAction.setText("Apply Delete");
        }
        else if (action == DoAction.duplicateCell) {
            // set current cell as the default
            Cell cell = WindowFrame.getCurrentCell();
            setCell(cell);

            // remove done button, this is a one-shot action button
            jPanel1.remove(done);
        }
    }


    // --------------------------------- Actions ----------------------------------

    private void jList1ValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jList1ValueChanged
        int index = jList1.getSelectedIndex();
        jList1.ensureIndexIsVisible(index);
        if (jList1.getSelectedValue() != null) {
            lastSelectedCell = (String)jList1.getSelectedValue();
        }
        if (action == DoAction.renameCell) {
            if (lastSelectedCell != null && newCellName != null) {
                String nameOnly = lastSelectedCell.replaceAll("\\{.*?\\}", "");
                newCellName.setText(nameOnly);
            }
        }
    }//GEN-LAST:event_jList1ValueChanged

    private void cellFilterKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_cellFilterKeyReleased
        updateCellList();
    }//GEN-LAST:event_cellFilterKeyReleased

    private void viewComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_viewComboBoxItemStateChanged
        updateCellList();
    }//GEN-LAST:event_viewComboBoxItemStateChanged

    private void libraryComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_libraryComboBoxItemStateChanged
        updateCellList();
    }//GEN-LAST:event_libraryComboBoxItemStateChanged

    private void jList1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jList1MouseClicked
        // do action on double click, close dialog
        if (evt.getClickCount() >= 2) performAction();
        if (jList1.getSelectedValue() != null) {
            lastSelectedCell = (String)jList1.getSelectedValue();
        }
    }//GEN-LAST:event_jList1MouseClicked

    private void doActionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_doActionActionPerformed
        performAction();
    }//GEN-LAST:event_doActionActionPerformed

    private void cancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelActionPerformed
        closeDialog(null);
    }//GEN-LAST:event_cancelActionPerformed
    
    private void doneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_doneActionPerformed
        closeDialog(null);
    }//GEN-LAST:event_doneActionPerformed

    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog

        // save all preferences
        prefs.put(action+prefSelectedLib, (String)libraryComboBox.getSelectedItem());
        prefs.put(action+prefSelectedView, (String)viewComboBox.getSelectedItem());
        if (jList1.getSelectedValue() != null) {
            prefs.put(action+prefSelectedCell, (String)jList1.getSelectedValue());
        }
        prefs.put(action+prefFilter, lastFilter);

        if (action == DoAction.editCell) {
            prefs.putBoolean(prefEditInNewWindow, editInNewWindow.isSelected());
        }
        setVisible(false);
        Undo.removeDatabaseChangeListener(this);
        dispose();
    }//GEN-LAST:event_closeDialog

    private void performAction() {

        if (action == DoAction.newInstance) {
            Cell cell = getSelectedCell();
            if (cell == null) return;

            EventListener oldListener = WindowFrame.getListener();
            Cursor oldCursor = TopLevel.getCurrentCursor();

            // install listener that will place new node inst
            EventListener newListener = oldListener;
            if (newListener != null && newListener instanceof PaletteFrame.PlaceNodeListener)
            {
                ((PaletteFrame.PlaceNodeListener)newListener).setParameter(cell);
            } else
            {
                newListener = new PaletteFrame.PlaceNodeListener(null, cell, oldListener, oldCursor);
                WindowFrame.setListener(newListener);
            }
            closeDialog(null);                     // we have performed the action

        } else if (action == DoAction.editCell) {
            boolean newWindow = editInNewWindow.isSelected();

            List cells = getSelectedCells();
            for (Iterator it = cells.iterator(); it.hasNext(); ) {
                Cell cell = (Cell)it.next();
                WindowFrame wf = WindowFrame.getCurrentWindowFrame();
                if (!newWindow && wf == null) newWindow = true;
                if (newWindow)
                {
                    WindowFrame.createEditWindow(cell);
                } else
                {
                    wf.setCellWindow(cell);
                    WindowFrame.setCurrentWindowFrame(wf);
                }
                // if multiple cells selected, all cells after first
                // should be opened in new window
                newWindow = true;
            }
            closeDialog(null);                     // we have performed the action

        } else if (action == DoAction.deleteCell) {
            boolean confirm = true;

            List cells = getSelectedCells();
            for (Iterator it = cells.iterator(); it.hasNext(); ) {
                Cell cell = (Cell)it.next();
                CircuitChanges.deleteCell(cell, confirm);
            }
            updateCellList();

        } else if (action == DoAction.renameCell) {
            Cell cell = getSelectedCell();
            if (cell == null) return;

            String newName = newCellName.getText();
            if (newName == null || newName.equals("")) return;

            CircuitChanges.renameCellInJob(cell, newName);
            lastSelectedCell = newName;
            //setCell(cell);

        } else if (action == DoAction.duplicateCell) {
            Cell cell = getSelectedCell();
            if (cell == null) return;

            String newName = JOptionPane.showInputDialog(this, "Name of duplicated cell",
                cell.getName() + "NEW");
            if (newName == null) return;
            CircuitChanges.duplicateCell(cell, newName);
            closeDialog(null);                     // we have performed the action

        }

    }

    // -------------------------------- Set State of Dialog ------------------------------

    private void setLibrary(Library lib) {
        String libName = lib.getName();
        libraryComboBox.setSelectedItem(libName);
    }

    private void setView(View view) {
        String viewName = view.getFullName();
        viewComboBox.setSelectedItem(viewName);
    }

    private void setCell(Cell cell) {
        // clear filter
        cellFilter.setText("");
        // this will make cell selected on update of list
        lastSelectedCell = cell.noLibDescribe();

        Library lib = cell.getLibrary();
        setLibrary(lib);
        View view = cell.getView();
        setView(view);
        //updateCellList();
    }

    // ---------------------------------- List ----------------------------------------

    /**
     * Updates the Cell list depending on the library, view selected, and any filter in
     * the text box.
     */
    private void updateCellList() {
        String libName = (String)libraryComboBox.getSelectedItem();
        String viewName = (String)viewComboBox.getSelectedItem();
        String filter = cellFilter.getText();

        // get library if specified (all if not)
        Library lib = Library.findLibrary(libName);
        // get view if specified (all if not)
        View view = null;
        for (Iterator it = View.getViews(); it.hasNext(); ) {
            View v = (View)it.next();
            if (v.getFullName().equals(viewName)) {
                view = v;
                break;
            }
        }

        // use cached (last) filter Pattern if no change
        Pattern pat;
        if (filter.equals("*")) filter = "";
        if (filter.equals(lastFilter)) {
            pat = lastPattern;
        } else {
            try {
                pat = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);
            } catch(java.util.regex.PatternSyntaxException e) {
                pat = null;
                filter = "";
            }
            lastPattern = pat;
            lastFilter = filter;
        }

        cellList = new ArrayList();
        if (lib == null) {
            // do all libraries
            for (Iterator it = Library.getLibraries(); it.hasNext(); ) {
                Library library = (Library)it.next();
                for (Iterator it2 = library.getCells(); it2.hasNext(); ) {
                    Cell c = (Cell)it2.next();
                    if (view != null) {
                        if (view != c.getView()) continue;     // skip if not filtered view
                    }
                    if (pat != null) {
                        Matcher mat = pat.matcher(c.noLibDescribe());
                        if (!mat.find()) continue;              // skip if doesn't match filter
                    }
                    cellList.add(c);
                }
            }
        } else {
            // just do selected library
            for (Iterator it = lib.getCells(); it.hasNext(); ) {
                Cell c = (Cell)it.next();
                if (view != null) {
                    if (view != c.getView()) continue;     // skip if not filtered view
                }
                if (!filter.equals("")) {
                    Matcher mat = pat.matcher(c.noLibDescribe());
                    if (!mat.find()) continue;              // skip if doesn't match filter
                }
                cellList.add(c);
            }
        }
        cellList = Library.getCellsSortedByName(cellList);      // sort list by name

        cellListNames = new ArrayList();
        for (Iterator it = cellList.iterator(); it.hasNext(); ) {
            Cell c = (Cell)it.next();
            cellListNames.add(c.noLibDescribe());
        }

        jList1.setListData(cellListNames.toArray());
		// if nothing selected or found, then disable the button
		doAction.setEnabled(cellListNames.size()>0);

        // update JScrollPane's scroll bars for new size list
        jList1.setVisibleRowCount(jList1.getModel().getSize());
        jList1.setPreferredSize(jList1.getPreferredScrollableViewportSize());
        jList1.revalidate();

        // changing lists will have triggered update to cell list, go to last selected cell now
        if (lastSelectedCell != null) {
            int i;
            for (i=0; i<cellListNames.size(); i++) {
                String name = (String)cellListNames.get(i);
                if (name.equals(lastSelectedCell)) {
                    jList1.setSelectedIndex(i);
                    break;
                }
            }
            if (i == cellListNames.size()) {
                lastSelectedCell = null;
            }
        }
        if (lastSelectedCell == null) {
            // didn't find anything, try to find current cell in the list
            Cell cell = WindowFrame.getCurrentCell();
            if (cell != null) {
                String findname = cell.noLibDescribe();
                for (int i=0; i<cellListNames.size(); i++) {
                    String name = (String)cellListNames.get(i);
                    if (name.equals(findname)) {
                        jList1.setSelectedIndex(i);
                        lastSelectedCell = findname;
                        break;
                    }
                }
            }
        }
    }


    /**
     * Get selected Cell by user in Cell List
     * @return Cell index or -1 if no cell is selected
     */
    private Cell getSelectedCell() {
        int i = jList1.getSelectedIndex();
		return (i == -1)? null : (Cell)cellList.get(i);
    }

    /**
     * Get a list of selected Cells.
     * @return a list of selected Cells, or an empty list if none selected.
     */
    private List getSelectedCells() {
        int [] is = jList1.getSelectedIndices();
        ArrayList list = new ArrayList();
        for (int i=0; i<is.length; i++) {
            int celli = is[i];
            Cell cell = (Cell)cellList.get(celli);
            if (cell == null) continue;
            list.add(cell);
        }
        return list;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancel;
    private javax.swing.JTextField cellFilter;
    private javax.swing.JButton doAction;
    private javax.swing.JButton done;
    private javax.swing.JPanel extrasPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JList jList1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JComboBox libraryComboBox;
    private javax.swing.JComboBox viewComboBox;
    // End of variables declaration//GEN-END:variables
    
}
