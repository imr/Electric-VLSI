/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: Ncc.java
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
package com.sun.electric.tool.ncc.ui;

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

class ExportAssertionTable extends ExportTable {
    
    ExportAssertionFailures[] failures;
    
    public ExportAssertionTable(NccComparisonMismatches res) {
        super(res);
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

class AssertionTableModel extends ExportTableModel {
    ExportAssertionFailures[] failures;
    int[][] cellPrefHeights = parent.cellPrefHeights;
    int[][] cellPrefWidths  = parent.cellPrefWidths;
    String[] colNames = {"Cell", "Networks"};
    
    public AssertionTableModel(ExportAssertionTable parent) {
        super(parent);
        failures = parent.failures;
        cellPrefHeights = parent.cellPrefHeights;
        cellPrefWidths  = parent.cellPrefWidths;

        StringBuffer text = new StringBuffer(64);
        for (int row=0; row<height; row++) {
            text.setLength(0);
            text.append("<html><font size=3><font face=\"Helvetica, TimesRoman\">");
            CellName cellName = failures[row].getCell().getCellName();
            text.append(cellName.getName() + " {" + cellName.getView().getAbbreviation() + "}");
            text.append("</font></html>");
            cellPrefHeights[row][0] += ExportTable.LINEHEIGHT;
            
            JEditorPane textPane = new JEditorPane();
            textPane.setEditable(false);
            textPane.setContentType("text/html");
            textPane.setText(text.toString());
            textPane.setMargin(insets);
            textPane.addMouseListener(mouseAdapter);                    
            textPane.moveCaretPosition(0);
            cellPrefWidths[row][0] = textPane.getPreferredSize().width + ExportTable.WIDTHMARGIN;
            if (cellPrefHeights[row][0] > ExportTable.MAXLINES*ExportTable.LINEHEIGHT+ExportTable.HEIGHTMARGIN)
                cellPrefHeights[row][0] = ExportTable.MAXLINES*ExportTable.LINEHEIGHT+ExportTable.HEIGHTMARGIN;
            JPanel panel = new JPanel();
            panel.setBackground(Color.WHITE);
            panel.add(textPane);
            panes[row][0] = new JScrollPane(panel);
            panes[row][0].setBorder(BorderFactory.createEmptyBorder());
        }
        
        for (int row=0; row<height; row++) {
            Object[][] items = failures[row].getExportsGlobals();
            if (items == null || items.length == 0) continue;
            text.setLength(0);
            text.append("<html><font size=3><font face=\"Helvetica, TimesRoman\">");
            appendLinks(items, text, row);
            text.append("</font></html>");
            
            JEditorPane textPane = new JEditorPane();
            textPane.setEditable(false);
            textPane.addHyperlinkListener(this);
            textPane.setContentType("text/html");
            textPane.setText(text.toString());
            textPane.setMargin(insets);
            textPane.addMouseListener(mouseAdapter);                    
            textPane.moveCaretPosition(0);
            cellPrefWidths[row][1] = textPane.getPreferredSize().width + ExportTable.WIDTHMARGIN;
            if (cellPrefHeights[row][1] > ExportTable.MAXLINES*ExportTable.LINEHEIGHT+ExportTable.HEIGHTMARGIN)
                cellPrefHeights[row][1] = ExportTable.MAXLINES*ExportTable.LINEHEIGHT+ExportTable.HEIGHTMARGIN;
            JPanel panel = new JPanel();
            panel.setBackground(Color.WHITE);
            panel.add(textPane);
            panes[row][1] = new JScrollPane(panel);
            panes[row][1].setBorder(BorderFactory.createEmptyBorder());
        }
    }

    private void appendLinks(Object[][] items, StringBuffer html, int row) {
        String href = "<a style=\"text-decoration: none\" href=\"";
        String text = "";
        int rowNdx = row*1000000;
        for (int i = 0; i<items.length; i++) {
            html.append("{ ");
            int lineNdx = rowNdx + i*1000; 
            for (int j = 0; j<items[i].length; j++) {
                int itemNdx = lineNdx + j;
                if (items[i][j] instanceof Export) {
                    text = ((Export)items[i][j]).getName();
                    System.out.print("e");
                } else if (items[i][j] instanceof Network) {
                    text = ((Network)items[i][j]).describe(false);
                    System.out.print("N");
                }
                html.append(href + itemNdx +"\">"+ text +"</a>");
                if (j < items[i].length-1) html.append(", ");
            }
            html.append(" }");
            if (i < items.length-1) html.append("<br>");
            cellPrefHeights[row][1] += ExportTable.LINEHEIGHT;
        }
        System.out.println();
    }
    
    protected void highlight(int index) {
        int item = index%1000;
        int line = (index/1000)%1000;
        int row  = index/1000000;
        
        ExportAssertionFailures eaf = failures[row];
        Object obj = eaf.getExportsGlobals()[line][item];
        Cell cell = eaf.getCell();
        VarContext context = eaf.getContext();
        
        // find the highlighter corresponding to the cell
        Highlighter highlighter = HighlightTools.getHighlighter(cell, context);
        if (highlighter == null) return;
            
        // find what to highlight 
        if (obj instanceof Export) {
            highlighter.addText((Export)obj, cell, null, null);
        } else if (obj instanceof Network) {
            highlighter.addNetwork((Network)obj, cell);
        }
        highlighter.finished();
    }
    
    public String getColumnName(int col) {
        return colNames[col];
    }
    
}

