/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LibraryPalette.java
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.database.change.DatabaseChangeEvent;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.UserInterfaceMain;
import com.sun.electric.tool.user.menus.CellMenu;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/**
 * This JPanel is a palette of library cells that can be placed in the
 * PaletteFrame.
 */
public class LibraryPalette extends JPanel implements DatabaseChangeListener, MouseListener, 
        PaletteFrame.PlaceNodeEventListener {

    private Library library;
    private JScrollPane scrollPane;
    private JList cellJList;
    private Map<Library,Rectangle> viewPortMap;    // key: library. Object: Integer
    private JPopupMenu cellPopup;
    private PaletteFrame.PlaceNodeListener lastPlaceNodeListener = null;

    /**
     * The palette panel. This constructor is never used, use the Factory Method
     * newInstance instead.
     */
    public LibraryPalette(Dimension preferredSize) {
        library = null;
        viewPortMap = new HashMap<Library,Rectangle>();
        initComponents(preferredSize);
        UserInterfaceMain.addDatabaseChangeListener(this);
    }

    /**
     * Set the library whose cells will be displayed in the palette
     */
    public void setLibrary(Library lib) {
        if (library != lib) {
            // record old view port point
            viewPortMap.put(library, cellJList.getVisibleRect());
        }
        library = lib;
        updateCellList();
    }

    /**
     * Initialize components
     */
    private void initComponents(Dimension preferredSize) {
        scrollPane = new JScrollPane();
        cellJList = new JList();
        cellJList.setCellRenderer(new CustomCellRenderer());
        cellJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cellJList.addMouseListener(this);
        scrollPane.setViewportView(cellJList);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(preferredSize);
        setLayout(new java.awt.BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        updateCellList();
    }

    private void updateCellList() {
        if (library == null) {
            cellJList.setListData(new Object[0]);
            return;
        }

        List<Cell> cellList = new ArrayList<Cell>();
        for (Iterator<Cell> it = library.getCells(); it.hasNext(); ) {
            cellList.add(it.next());
        }
        cellJList.setListData(cellList.toArray());

        Rectangle rect = (Rectangle)viewPortMap.get(library);
        if (rect != null) {
            cellJList.scrollRectToVisible(rect);
        }
    }

//     public void databaseChanged(Undo.Change evt) {}
//     public boolean isGUIListener() { return true; }
//     public void databaseEndChangeBatch(Undo.ChangeBatch batch) {
//         updateCellList();
//     }
    public void databaseChanged(DatabaseChangeEvent e) {
        updateCellList();
    }

    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {
        if (e.isShiftDown() || e.isControlDown() || e.isAltDown()) return;

        if (e.isMetaDown()) {
            // right click: popup menu at location
            initCellPopup();
            int index = cellJList.locationToIndex(new Point(e.getX(), e.getY()));
            Object selected = cellJList.getModel().getElementAt(index);
            if (selected == null) return;

            cellJList.setSelectedValue(selected, false);
            cellPopup.show(this, e.getX(), e.getY());
        } else {
            Object selected = cellJList.getSelectedValue();
            if (selected == null) return;

            Cell cell = (Cell)selected;
            if (e.getClickCount() == 2) {
                // edit cell at location
                selectedCellEdit();
                // cancel any placing of cells (double click is always preceeded with single click)
                if (lastPlaceNodeListener != null) {
                    lastPlaceNodeListener.finished(EditWindow.getCurrent(), true);
                }
            } else {
                // single click
                // place cell at location
                // ignore schematics for now
                if (cell.isSchematic()) return;
                lastPlaceNodeListener = PaletteFrame.placeInstance(cell, this, false);
            }
        }
    }

    public void placeNodeStarted(Object nodeToBePlaced) {
    }
    public void placeNodeFinished(boolean cancelled) {
        cellJList.clearSelection();
        lastPlaceNodeListener = null;
    }

    private void initCellPopup() {
        if (cellPopup != null) return;

        cellPopup = new JPopupMenu();
        JMenuItem m;
        // edit / duplicate / rename / delete
        m = new JMenuItem("Edit");
        m.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { selectedCellEdit(); }
        });
        cellPopup.add(m);
        m = new JMenuItem("Duplicate");
        m.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { selectedCellDuplicate(); }
        });
        cellPopup.add(m);
        m = new JMenuItem("Rename");
        m.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { selectedCellRename(); }
        });
        cellPopup.add(m);
        m = new JMenuItem("Delete");
        m.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { selectedCellDelete(); }
        });
        cellPopup.add(m);
    }

    private void selectedCellEdit() {
        Cell cell = (Cell)cellJList.getSelectedValue();
        if (cell == null) return;
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) {
            WindowFrame.createEditWindow(cell);
        } else {
            wf.setCellWindow(cell, null);
        }
        cellJList.clearSelection();
    }
    private void selectedCellDuplicate() {
        Cell cell = (Cell)cellJList.getSelectedValue();
        if (cell == null) return;
        CellMenu.duplicateCell(cell, false);
        cellJList.clearSelection();
    }
    private void selectedCellRename() {
        Cell cell = (Cell)cellJList.getSelectedValue();
        if (cell == null) return;
        String newName = JOptionPane.showInputDialog(this, "New name for cell "+cell.getName(),
            cell.getName() + "NEW");
        if (newName == null) return;
        CircuitChanges.renameCellInJob(cell, newName);
        cellJList.clearSelection();
    }
    private void selectedCellDelete() {
        Cell cell = (Cell)cellJList.getSelectedValue();
        if (cell == null) return;
        CircuitChanges.deleteCell(cell, true, false);
        cellJList.clearSelection();
    }

    // -------------------------------------------------------------------

    class CustomCellRenderer extends DefaultListCellRenderer {

        /* This is the only method defined by ListCellRenderer.  We just
        * reconfigure the Jlabel each time we're called.
        */
        public Component getListCellRendererComponent(
                JList list,
                Object value,   // value to display
                int index,      // cell index
                boolean iss,    // is the cell selected
                boolean chf)    // the list and the cell have the focus
        {
            /* The DefaultListCellRenderer class will take care of
            * the JLabels text property, it's foreground and background
            * colors, and so on.
            */
            Cell cell = (Cell)value;
            super.getListCellRendererComponent(list, cell.noLibDescribe(), index, iss, chf);
            return this;
        }
    }

}
