/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: UnrecognizedPartTable.java
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

class UnrecognizedPartTable extends ExportTable {
    
    UnrecognizedPart[] moses;
    
    public UnrecognizedPartTable(NccGuiInfo res) {
        super(res, 2);
        moses = (UnrecognizedPart[])result.getUnrecognizedParts()
                                         .toArray(new UnrecognizedPart[0]);
        height = Math.min(moses.length, MAXROWS);
        setup();
        
        setModel(new UnrecognizedPartTableModel(this));
        getTableHeader().setReorderingAllowed(false);
        getColumnModel().getColumn(0).addPropertyChangeListener(this);
        getColumnModel().getColumn(1).addPropertyChangeListener(this);
    }
}

class UnrecognizedPartTableModel extends ExportTableModel {
    UnrecognizedPart[] moses;
    int[][] cellPrefHeights = parent.cellPrefHeights;
    int[][] cellPrefWidths  = parent.cellPrefWidths;
    String[] colNames = {"Cell", "Part Type"};
    
    public UnrecognizedPartTableModel(UnrecognizedPartTable parent) {
        super(parent);
        moses = parent.moses;
        cellPrefHeights = parent.cellPrefHeights;
        cellPrefWidths  = parent.cellPrefWidths;

        String href = "<a style=\"text-decoration: none\" href=\"";
        StringBuffer text = new StringBuffer(64);
        for (int col=0; col<numCols; col++)
            for (int row=0; row<height; row++) {
                text.setLength(0);
                text.append("<html><font size=3><font face=\"Helvetica, TimesRoman\">");
                if (col == 0) {
                    CellName cellName = moses[row].getCell().getCellName();
                    text.append(cellName.getName() + " {" + cellName.getView().getAbbreviation() + "}");
                } else {
                    text.append(href+ (row*10+col) +"\">"+ moses[row].getName() +"</a>");                    
                }
                cellPrefHeights[row][col] += ExportTable.LINEHEIGHT;
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

    protected void highlight(int index) {
        int col = index%10;
        int row  = index/10;
        if (col != 1) return;
        
        UnrecognizedPart mos = moses[row];
        Cell cell = mos.getCell();
        VarContext context = mos.getContext();
        
        // find the highlighter corresponding to the cell
        Highlighter highlighter = HighlightTools.getHighlighter(cell, context);
        if (highlighter == null) return;
        highlighter.addElectricObject(mos.getNodeInst(), cell);
        highlighter.finished();
    }
    
    public String getColumnName(int col) {
        return colNames[col];
    }
    
}

