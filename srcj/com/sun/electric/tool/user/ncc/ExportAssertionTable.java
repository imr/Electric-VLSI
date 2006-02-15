/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: ExportAssertionTable.java
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
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.user.Highlighter;

/**
 * This class implements a table for Export Assertion Failures
 */
class ExportAssertionTable extends ExportTable {
    
    protected ExportAssertionFailures[] failures;
    
    protected ExportAssertionTable(NccGuiInfo res) {
        super(res, 2);
        failures = (ExportAssertionFailures[])result.getExportAssertionFailures()
                                       .toArray(new ExportAssertionFailures[0]);
        height = Math.min(failures.length, MAXROWS);
        setup();
        
        setModel(new AssertionTableModel(this));
        getTableHeader().setReorderingAllowed(false);
        getColumnModel().getColumn(0).addPropertyChangeListener(this);
        getColumnModel().getColumn(1).addPropertyChangeListener(this);
    }
}

/**
 * This class implements a table model for Export Assertion Failures table
 */
class AssertionTableModel extends ExportTableModel {
    ExportAssertionFailures[] failures;
    int[][] cellPrefHeights = parent.cellPrefHeights;
    int[][] cellPrefWidths  = parent.cellPrefWidths;
    String[] colNames = {"Cell", "Exports"};
    
    protected AssertionTableModel(ExportAssertionTable parent) {
        super(parent);
        failures = parent.failures;
        cellPrefHeights = parent.cellPrefHeights;
        cellPrefWidths  = parent.cellPrefWidths;
        
        // fill table cells with HTML (JEditorPane)
        StringBuffer text = new StringBuffer(64);
        for (int col=0; col<numCols; col++)
            for (int row=0; row<height; row++) {
                Object[][] items = null;
                if (col == 1) {
                    items = failures[row].getExportsGlobals();
                    if (items == null || items.length == 0) continue;
                }
                text.setLength(0);
                text.append("<html><font size=3><font face=\"Helvetica, TimesRoman\">");
                if (col == 0) {
                    CellName cellName = failures[row].getCell().getCellName();
                    text.append(cellName.getName() + " {" + cellName.getView().getAbbreviation() + "}");
                    cellPrefHeights[row][0] += ExportTable.LINEHEIGHT;
                } else {
                    appendLinks(text, row);                    
                }
                text.append("</font></html>");
                
                JEditorPane textPane = new JEditorPane();
                textPane.setEditable(false);
                if (col == 1) textPane.addHyperlinkListener(this);
                textPane.setContentType("text/html");
                textPane.setText(text.toString());
                textPane.setMargin(insets);
                textPane.addMouseListener(mouseAdapter);                    
                textPane.moveCaretPosition(0);
                cellPrefWidths[row][col] = textPane.getPreferredSize().width + ExportTable.WIDTHMARGIN;
                if (cellPrefHeights[row][col] > ExportTable.MAX_VISIBLE_LINES*ExportTable.LINEHEIGHT+ExportTable.HEIGHTMARGIN)
                    cellPrefHeights[row][col] = ExportTable.MAX_VISIBLE_LINES*ExportTable.LINEHEIGHT+ExportTable.HEIGHTMARGIN;
                JPanel panel = new JPanel();
                panel.setBackground(Color.WHITE);
                panel.add(textPane);
                panes[row][col] = new JScrollPane(panel);
                panes[row][col].setBorder(BorderFactory.createEmptyBorder());
            }
    }
    
    /**
     * A helper method for printing HTML hyperlinks to table cells.
     * @param html  buffer to print to
     * @param row  table row
     */
    private void appendLinks(StringBuffer html, int row) {
        if (html == null || row < 0 || row > failures.length) return;
        String[][] names = failures[row].getNames();
        String href = "<a style=\"text-decoration: none\" href=\"";
        int rowNdx = row*1000000;
        for (int i = 0; i<names.length; i++) {
            html.append("{ ");
            int lineNdx = rowNdx + i*1000; 
            for (int j = 0; j<names[i].length; j++) {
                int itemNdx = lineNdx + j;
                html.append(href + itemNdx +"\">"+ names[i][j] +"</a>");
                if (j < names[i].length-1) html.append(", ");
            }
            html.append(" }");
            if (i < names.length-1) html.append("<br>");
            cellPrefHeights[row][1] += ExportTable.LINEHEIGHT;
        }
    }
    
    /**
     * Highlight an export with the given index.
     * @param index  export index
     */
    protected void highlight(int index) {
        // decrypt index
        int item = index%1000;   // position in line
        int line = (index/1000)%1000;  // line in cell
        int row  = index/1000000;  // row in table
        
        ExportAssertionFailures eaf = failures[row];
        Object obj = eaf.getExportsGlobals()[line][item];
        Cell cell = eaf.getCell();
        VarContext context = eaf.getContext();
        
        // find the highlighter corresponding to the cell
        Highlighter highlighter = HighlightTools.getHighlighter(cell, context);
        if (highlighter == null) return;
            
        // find what to highlight 
        if (obj instanceof Export) {
            highlighter.addText((Export)obj, cell, null);
        } else if (obj instanceof Network) {
            highlighter.addNetwork((Network)obj, cell);
        }
        highlighter.finished();
    }

    /**
     * Get column name
     * @param col  column
     * @return name of the column
     */
    public String getColumnName(int col) {
        return colNames[col];
    }
}
