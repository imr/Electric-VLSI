/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: ExportConflictTable.java
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
package com.sun.electric.tool.user.ncc;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.user.Highlighter;

/**
 * This class is a common superclass for Export conflict tables 
 */
abstract class ExportConflictTable extends ExportTable {
    protected ExportConflict[] conflicts;
    protected ExportConflictTable(NccGuiInfo res) { super(res, 4); }
    protected void postSetup() {
        getTableHeader().setReorderingAllowed(false);
        for (int col=0; col < 4; col++)
            getColumnModel().getColumn(col).addPropertyChangeListener(this);
    }
    
    /**
     * This class implements the table for Export/Global Network conflicts
     */
    protected static class NetworkTable extends ExportConflictTable {
        public NetworkTable(NccGuiInfo res) {
            super(res);
            conflicts = (ExportConflict.NetworkConflict[])result
                  .getNetworkExportConflicts().toArray(new ExportConflict.NetworkConflict[0]);
            height = Math.min(conflicts.length, MAXROWS);
            setup();
            setModel(new ExportConflictTableModel.NetworkTableModel(this));
            postSetup();
        }
    }

    /**
     * This class implements the table for Export/Global Characteristics conflicts
     */
    protected static class CharacteristicsTable extends ExportConflictTable {
        public CharacteristicsTable(NccGuiInfo res) {
            super(res);
            conflicts = (ExportConflict.CharactConflict[])result
                .getCharactExportConflicts().toArray(new ExportConflict.CharactConflict[0]);
            height = Math.min(conflicts.length, MAXROWS);
            setup();
            setModel(new ExportConflictTableModel.CharacteristicsTableModel(this));
            postSetup();
        }
    }    
}

/**
 * This class is a common superclass for Export conflict table models
 */
abstract class ExportConflictTableModel extends ExportTableModel {
    protected ExportConflict[] conflicts;
    protected int[][] cellPrefHeights = parent.cellPrefHeights;
    protected int[][] cellPrefWidths  = parent.cellPrefWidths;
    protected static String href = "<a style=\"text-decoration: none\" href=\"";
    
    protected ExportConflictTableModel(ExportConflictTable parent) {
        super(parent);
        conflicts = parent.conflicts;
        cellPrefHeights = parent.cellPrefHeights;
        cellPrefWidths  = parent.cellPrefWidths;
        
        // fill table cells with HTML (JEditorPane)
        StringBuffer txtBuf = new StringBuffer(64);
        for (int col=0; col<numCols; col++) 
            for (int row=0; row<height; row++) {
                txtBuf.setLength(0);
                txtBuf.append("<html><font size=3><font face=\"Helvetica, TimesRoman\">");
                switch (col) {
                    case 0: 
                    CellName cellName = conflicts[row].getCell().getCellName();
                    txtBuf.append(cellName.getName() + " {" + cellName.getView().getAbbreviation() + "}");
                    break;
                    case 1:
                    txtBuf.append(conflicts[row].getName()); 
                    break;
                    case 2:
                    case 3:
                        appendText(row, col, txtBuf);
                }
                txtBuf.append("</font></html>");
                cellPrefHeights[row][col] += ExportTable.LINEHEIGHT;
                
                JEditorPane htmlPane = new JEditorPane();
                htmlPane.setEditable(false);
                htmlPane.addHyperlinkListener(this);
                htmlPane.setContentType("text/html");
                htmlPane.setText(txtBuf.toString());
                htmlPane.setMargin(insets);
                htmlPane.addMouseListener(mouseAdapter);                    
                htmlPane.moveCaretPosition(0);
                cellPrefWidths[row][col] = htmlPane.getPreferredSize().width + ExportTable.WIDTHMARGIN;
                if (cellPrefHeights[row][col] > ExportTable.MAX_VISIBLE_LINES*ExportTable.LINEHEIGHT+ExportTable.HEIGHTMARGIN)
                    cellPrefHeights[row][col] = ExportTable.MAX_VISIBLE_LINES*ExportTable.LINEHEIGHT+ExportTable.HEIGHTMARGIN;
                JPanel panel = new JPanel();
                panel.setBackground(Color.WHITE);
                panel.add(htmlPane);
                panes[row][col] = new JScrollPane(panel);
                panes[row][col].setBorder(BorderFactory.createEmptyBorder());
            }
    }
    
    /**
     * Print necessary hyperlinked or plain text description to the table cell 
     * at the intersection of the given row and column.
     * @param row  table row
     * @param col  table column
     * @param txtBuf  buffer to print to
     */
    protected abstract void appendText(int row, int col, StringBuffer txtBuf);

    /**
     * Highlight an export with the given index.
     * @param index  export index
     */
    protected void highlight(int index) {
        // decrypt Export index 
        int col = index%10;  // table column
        int row  = index/10; // table row
        if (col != 2 && col != 3) return;
        
        ExportConflict nc = conflicts[row];
        Cell cell = nc.getCell();
        VarContext context = nc.getContext();
        
        // find the highlighter corresponding to the cell
        Highlighter highlighter = HighlightTools.getHighlighter(cell, context);
        if (highlighter == null) return;
        addToHighlighter(row, col, highlighter);
        highlighter.finished();
    }
    
    /**
     * Add the Export or Network which is printed in the table cell [row,col]
     * to the provided highlighter
     * @param row
     * @param col
     * @param highlighter
     */
    protected abstract void addToHighlighter(int row, int col, Highlighter highlighter);

    
    /**
     * This class implements the table model for the Export/Global 
     * Network conflict table
     */
    protected static class NetworkTableModel extends ExportConflictTableModel {
        protected static String[] colNames = {"Cell", "Conflicting Name", 
                                       "Global Network", "Export Network"};
        
        protected NetworkTableModel(ExportConflictTable parent) { super(parent); }
        
        /**
         * Print hyperlinked description to the table cell at the intersection 
         * of the given row and column. Column indices other than 2 and 3 and 
         * row indices outside of valid range have no effect.
         * @param row  table row index
         * @param col  table column  index
         * @param buf  buffer to print to
         */        
        protected void appendText(int row, int col, StringBuffer buf) {
            if (row < 0 || row > conflicts.length || (col != 2 && col !=3)) return;
            buf.append(href + (row*10+col) + "\">"
                    + conflicts[row].getDescription(col-2) + "</a>");
        }
        
        /**
         * Add the Network which is printed in the table cell [row,col]
         * to the provided highlighter. 
         * Column indices other than 2 and 3 have no effect.
         * @param row  table row index
         * @param col  table column index
         * @param highlighter  Highlighter to add to
         */        
        protected void addToHighlighter(int row, int col, Highlighter highlighter) {
            if (col != 2 && col != 3) return;
            ExportConflict.NetworkConflict nc = 
                (ExportConflict.NetworkConflict)conflicts[row];
            highlighter.addNetwork(nc.getNetwork(col-2), nc.getCell());
        }
        
        /**
         * Get column name
         * @param col  column
         * @return name of the column
         */        
        public String getColumnName(int col) { return colNames[col]; }        
    }
    

    /**
     * This class implements the table model for the Export/Global 
     * Characteristics conflict table
     */    
    protected static class CharacteristicsTableModel extends ExportConflictTableModel {
        protected static String[] colNames = {"Cell", "Conflicting Name", 
                       "Global Characteristics", "Export Characteristics"};
        
        protected CharacteristicsTableModel(ExportConflictTable parent) { super(parent); }
        
        /**
         * Print plain text (if col == 2) or a hyperlinked (if col == 3) description
         * to the table cell at the intersection of the given row and column. 
         * Column indices other than 2 and 3 and row indices outside of valid 
         * range have no effect.
         * @param row  table row index
         * @param col  table column  index
         * @param buf  buffer to print to
         */         
        protected void appendText(int row, int col, StringBuffer buf) {
            if (row < 0 || row > conflicts.length || (col != 2 && col !=3)) return;
            String text = conflicts[row].getDescription(col-2);
            if (col == 2) {
                buf.append(text);
            } else if (col == 3)
                buf.append(href + (row*10+col) + "\">" + text + "</a>");
        }
        
        /**
         * Add the Export which is printed in the table cell [row,col]
         * to the provided highlighter. 
         * Column indices other than 2 and 3 have no effect.
         * @param row  table row index
         * @param col  table column index
         * @param highlighter  Highlighter to add to
         */         
        protected void addToHighlighter(int row, int col, Highlighter highlighter) {
            if (col != 3) return;
            ExportConflict.CharactConflict cc = 
                (ExportConflict.CharactConflict)conflicts[row];
            highlighter.addText(cc.getLocalExport(), cc.getCell(), null);
        }
        
        /**
         * Get column name
         * @param col  column
         * @return name of the column
         */           
        public String getColumnName(int col) { return colNames[col]; }        
    }     
}
