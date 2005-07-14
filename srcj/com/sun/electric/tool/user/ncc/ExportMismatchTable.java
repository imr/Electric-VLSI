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
package com.sun.electric.tool.user.ncc;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.netlist.Port;
import com.sun.electric.tool.ncc.netlist.Wire;
import com.sun.electric.tool.user.Highlighter;

class ExportMismatchTable extends ExportTable {

    ExportMismatch[] matches;
    
    public ExportMismatchTable(NccComparisonMismatches res) {
        super(res, 2);
        height = Math.min(result.getValidExportMismatchCount(), MAXROWS);
        matches = new ExportMismatch[height];
        setup();
        
        boolean topoOK = result.isTopologyMatch();
        int row = 0;
        
        for (Iterator it = result.getExportMismatches().iterator(); it.hasNext() && row<height;) {
            ExportMismatch em = (ExportMismatch)it.next();
            if (topoOK && em.isValidOnlyWhenTopologyMismatch()) continue;
            matches[row] = em;
            row++;
        }
        
        setModel(new MismatchTableModel(this));
        getTableHeader().setReorderingAllowed(false);
        getColumnModel().getColumn(0).addPropertyChangeListener(this);
        getColumnModel().getColumn(1).addPropertyChangeListener(this);
    }
}

class MismatchTableModel extends ExportTableModel {
    private boolean swapCells[];
    private ExportMismatch[] matches;
    
    public MismatchTableModel(ExportMismatchTable parent) {
        super(parent);
        matches = parent.matches;
        int[][] cellPrefHeights = parent.cellPrefHeights;
        int[][] cellPrefWidths  = parent.cellPrefWidths;
        swapCells = new boolean[height];
        int swap, cellNdx = 0;
        Border border = BorderFactory.createEmptyBorder();
        StringBuffer html = new StringBuffer(64);
        
        for (int row=0; row<height; row++) {
            if (matches[row].getName(0).equals(parent.result.getNames()[0])) {
                swap = 0;
            } else {
                swap = 1;
                swapCells[row] = true;
            }
            for (int j=0; j<2; j++, cellNdx++) {
                html.setLength(0);
                html.append("<html><font size=3><font face=\"Helvetica, TimesRoman\">");
                int lineNdx = cellNdx*10000;
                if (matches[row] instanceof ExportMismatch.MultiMatch) {
                    List ports = ((ExportMismatch.MultiMatch)matches[row]).getAll((j+swap)%2);
                    for (Iterator it=ports.iterator(); it.hasNext();) {
                        appendNameOf((Port)it.next(), html, lineNdx, false, null);
                        if (it.hasNext()) html.append("<br>" + LSEP);
                        lineNdx++;
                        cellPrefHeights[row][j] += ExportTable.LINEHEIGHT;
                    }
                } else if (matches[row] instanceof ExportMismatch.NameMismatch) {
                    if (j == swap) {
                        Port port = ((ExportMismatch.NameMismatch)matches[row]).getFirstExport(); 
                        appendNameOf(port, html, lineNdx, false, null);
                    } else {
                        NetObject no = ((ExportMismatch.NameMismatch)matches[row]).getSuggestion();
                        appendNameOf(no, html, lineNdx, true, ExportTable.GREEN);
                    }
                    lineNdx++;
                    cellPrefHeights[row][j] += ExportTable.LINEHEIGHT;
                } else if (matches[row] instanceof ExportMismatch.TopologyMismatch) {
                    Port port;
                    if (j == swap) {
                        port = ((ExportMismatch.TopologyMismatch)matches[row]).getFirstExport();
                        appendNameOf(port, html, lineNdx, true, ExportTable.RED);
                    } else {
                        port = ((ExportMismatch.TopologyMismatch)matches[row]).getSecondExport();
                        appendNameOf(port, html, lineNdx, true, ExportTable.RED);
                    }
                    lineNdx++;
                    if (j != swap) {
                        NetObject no = ((ExportMismatch.TopologyMismatch)matches[row]).getSuggestion();
                        if (no != null) {
                            html.append("<br>");
                            appendNameOf(no, html, lineNdx, true, ExportTable.GREEN);
                            lineNdx++;
                            cellPrefHeights[row][j] += ExportTable.LINEHEIGHT;
                        }
                    }
                    cellPrefHeights[row][j] += ExportTable.LINEHEIGHT;
                }
                html.append("</font></html>");
                
                JEditorPane htmlPane = new JEditorPane();
                htmlPane.setEditable(false);
                htmlPane.addHyperlinkListener(this);
                htmlPane.setContentType("text/html");
                htmlPane.setText(html.toString());
                htmlPane.setMargin(insets);
                htmlPane.addMouseListener(mouseAdapter);                    
                htmlPane.moveCaretPosition(0);
                cellPrefWidths[row][j] = htmlPane.getPreferredSize().width + ExportTable.WIDTHMARGIN;
                if (cellPrefHeights[row][j] > ExportTable.MAXLINES*ExportTable.LINEHEIGHT+ExportTable.HEIGHTMARGIN)
                    cellPrefHeights[row][j] = ExportTable.MAXLINES*ExportTable.LINEHEIGHT+ExportTable.HEIGHTMARGIN;
                JPanel panel = new JPanel();
                panel.setBackground(Color.WHITE);
                panel.add(htmlPane);
                panes[row][j] = new JScrollPane(panel);
                panes[row][j].setBorder(border);
            }
        }
    }
    
    protected void appendNameOf(NetObject no, StringBuffer html, 
            int lineNdx, boolean doColoring, String sugColor) {
        String href = "<a style=\"text-decoration: none\" href=\"";
        String text = null;
        boolean isImpl = false;
        if (no instanceof Port) {
            Port port = (Port)no;
            isImpl = isImplied(port);
            text = port.exportNamesString();
            if (doColoring) html.append("<font COLOR=\"" + sugColor + "\">");
            if (isImpl) {
                html.append(text);
                if (doColoring) html.append("</font>");
                html.append("<font COLOR=\"gray\"> : implied</font>");
            } else {
                html.append(href + lineNdx +"\">"+ text +"</a>");
                if (doColoring) html.append("</font>");
            }
        } else if (no instanceof Wire) {
            text = no.getName();
            if (doColoring) html.append("<font COLOR=\"" + sugColor + "\">");
            html.append(href + lineNdx + "\">"+ text +"</a></font>");
            if (doColoring) html.append("</font>");
        }
    }

    protected boolean isImplied(Port port) {
        Iterator it=port.getWire().getNameProxy().getNet().getExports();
        if (it.hasNext()) return false;
        return true;
    }
    
    protected void highlight(int index) {
        int line = index%10000;
        int row = (index/10000)/2;
        int col = (index/10000)%2;
        if (swapCells[row]) col = (col+1)%2;
        
        ExportMismatch em = matches[row];            
        Cell cell = em.getCell(col);
        VarContext context = em.getContext(col);
        
        // find the highlighter corresponding to the cell
        Highlighter highlighter = HighlightTools.getHighlighter(cell, context);
        if (highlighter == null) return;
            
        // find what to highlight 
        if (em instanceof ExportMismatch.MultiMatch) {
            List ports = ((ExportMismatch.MultiMatch)em).getAll(col);
            int i;
            Iterator it;
            for (it=ports.iterator(), i=0; it.hasNext()&&i<line; i++,it.next());
            Port port = (Port)it.next();
            HighlightTools.highlightPortExports(highlighter, cell, port);
        } else if (em instanceof ExportMismatch.NameMismatch) {
            Port port;
            NetObject portOrWire;
            if (col == 0) {
                port = ((ExportMismatch.NameMismatch)em).getFirstExport();
                HighlightTools.highlightPortExports(highlighter, cell, port);
            } else {
                portOrWire = ((ExportMismatch.NameMismatch)em).getSuggestion();
                HighlightTools.highlightPortOrWire(highlighter, cell, portOrWire);
            }
        } else if (em instanceof ExportMismatch.TopologyMismatch) {
            Port port1, port2;
            NetObject portOrWire;
            if (col == 0) {
                port1 = ((ExportMismatch.TopologyMismatch)em).getFirstExport();
                HighlightTools.highlightPortExports(highlighter, cell, port1);
            } else if (line == 0) {
                port2 = ((ExportMismatch.TopologyMismatch)em).getSecondExport();
                HighlightTools.highlightPortExports(highlighter, cell, port2);
            } else if (line == 1) {
                portOrWire = ((ExportMismatch.TopologyMismatch)em).getSuggestion();
                HighlightTools.highlightPortOrWire(highlighter, cell, portOrWire);
            }
        }
        highlighter.finished();
    }
    
    public String getColumnName(int col) {
        return parent.result.getNames()[col];
    }

}
