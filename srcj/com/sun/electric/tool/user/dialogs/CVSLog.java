/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CVSLog.java
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

import com.sun.electric.tool.cvspm.Log;

import javax.swing.table.AbstractTableModel;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import java.util.List;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Component;

/**
 *
 * @author  gainsley
 */
public class CVSLog extends EDialog implements MouseListener {

    /** Creates new form CVSLog */
    public CVSLog(List<Log.LogEntry> entries, String title, String workingVersion) {
        super(null, false);
        initComponents();
        // set up table
        setTitle(title);
        LogTableModel model = new LogTableModel(entries);
        model.workingVersion = workingVersion;
        jTable1.setModel(model);
        pack();
        jTable1.addMouseListener(this);
    }

    public void showContextMenu(MouseEvent e) {
        int [] selectedRows = jTable1.getSelectedRows();
        JPopupMenu menu = new JPopupMenu();

        JMenuItem menuCompareLocal = new JMenuItem("Compare with Local");
        menuCompareLocal.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                compareWithLocal();
            }
        });
        menu.add(menuCompareLocal);

        JMenuItem menuCompare = new JMenuItem("Compare Versions");
        menuCompare.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                compare();
            }
        });
        menu.add(menuCompare);

        JMenuItem menuGet = new JMenuItem("Revert to this Version");
        menuGet.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getVersion();
            }
        });
        menu.add(menuGet);

        menuCompareLocal.setEnabled(false);
        menuCompare.setEnabled(false);
        menuGet.setEnabled(false);
        if (selectedRows.length == 1) {
            menuCompareLocal.setEnabled(true);
            menuGet.setEnabled(true);
        }
        else if (selectedRows.length == 2) {
            menuCompare.setEnabled(true);
        }

        menu.show((Component)e.getSource(), e.getX(), e.getY());
    }

    private void compareWithLocal() {
        int [] selectedRows = jTable1.getSelectedRows();
        if (selectedRows.length != 1) return;
        List<Log.LogEntry> entries = ((LogTableModel)jTable1.getModel()).entries;

        Log.compareWithLocal(entries.get(selectedRows[0]));
    }

    private void compare() {
        int [] selectedRows = jTable1.getSelectedRows();
        if (selectedRows.length != 2) return;
        List<Log.LogEntry> entries = ((LogTableModel)jTable1.getModel()).entries;

        Log.compare(entries.get(selectedRows[0]), entries.get(selectedRows[1]));
    }
    private void getVersion() {
        int [] selectedRows = jTable1.getSelectedRows();
        if (selectedRows.length != 1) return;
        List<Log.LogEntry> entries = ((LogTableModel)jTable1.getModel()).entries;

        Log.getVersion(entries.get(selectedRows[0]));
        setVisible(false);      // menu needs to be reloaded, cell reference will no longer be valid
    }

    public void mouseClicked(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showContextMenu(e);
        }
    }
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

    protected void escapePressed() {
        setVisible(false);
    }

    private static class LogTableModel extends AbstractTableModel {

        private static final String [] colHeaders = { "Version", "Branch", "Date", "Author", "Commit Message", "State", "Tag" };

        private String workingVersion = "";
        private List<Log.LogEntry> entries;
        private LogTableModel(List<Log.LogEntry> entries) {
            this.entries = entries;
        }
        public int getRowCount() {
            return entries.size();
        }
        public int getColumnCount() {
            return colHeaders.length;
        }
        public String getColumnName(int col) {
            return colHeaders[col];
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            Log.LogEntry entry = entries.get(rowIndex);
            switch(columnIndex) {
                case 0: {
                    if (entry.version.equals(workingVersion))
                        return entry.version+" (current)";
                    else
                        return entry.version;
                }
                //case 0: return entry.version;
                case 1: return entry.branch;
                case 2: return entry.date;
                case 3: return entry.author;
                case 4: return entry.commitMessage;
                case 5: return entry.state;
                case 6: return entry.tag;
            }
            return "";
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

        logTablePane = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        logTablePane.setPreferredSize(new java.awt.Dimension(1000, 203));
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        logTablePane.setViewportView(jTable1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(logTablePane, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable jTable1;
    private javax.swing.JScrollPane logTablePane;
    // End of variables declaration//GEN-END:variables
    
}
