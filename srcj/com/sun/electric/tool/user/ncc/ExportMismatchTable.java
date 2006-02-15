/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: ExportMismatchTable.java
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
import com.sun.electric.tool.ncc.result.NetObjReport;
import com.sun.electric.tool.ncc.result.PortReport;
import com.sun.electric.tool.ncc.result.WireReport;
import com.sun.electric.tool.user.Highlighter;

/**
 * This class implements the tbale for Export mismatches
 */
class ExportMismatchTable extends ExportTable {

    ExportMismatch[] mismatches;
    
    public ExportMismatchTable(NccGuiInfo res) {
        super(res, 2);
        height = Math.min(result.getValidExportMismatchCount(), MAXROWS);
        mismatches = new ExportMismatch[height];
        setup();
        
        boolean topoOK = result.isTopologyMatch();
        int row = 0;
        
        for (Iterator<ExportMismatch> it = result.getExportMismatches().iterator(); it.hasNext() && row<height;) {
            ExportMismatch em = (ExportMismatch)it.next();
            if (topoOK && em.isValidOnlyWhenTopologyMismatch()) continue;
            mismatches[row] = em;
            row++;
        }
        
        setModel(new MismatchTableModel(this));
        getTableHeader().setReorderingAllowed(false);
        getColumnModel().getColumn(0).addPropertyChangeListener(this);
        getColumnModel().getColumn(1).addPropertyChangeListener(this);
    }
}

/**
 * This class implements the model for the Export mismatch table
 */
class MismatchTableModel extends ExportTableModel {
    /**
     * Each item in array indicates whether cells in the corresponding 
     * row should be switched  
     */
    private boolean swapCells[];
    /**
     * Array of mismatches
     */
    private ExportMismatch[] mismatches;
    
    public MismatchTableModel(ExportMismatchTable parent) {
        super(parent);
        mismatches = parent.mismatches;
        int[][] cellPrefHeights = parent.cellPrefHeights;
        int[][] cellPrefWidths  = parent.cellPrefWidths;
        swapCells = new boolean[height];
        int swap, cellNdx = 0;
        Border border = BorderFactory.createEmptyBorder();
        StringBuffer html = new StringBuffer(64);
        
        // fill swap array
        for (int row=0; row<height; row++) {
            // if first design in the mismatch object has the same name as
            // the first design in the parent result object, then don't swap.
            // Otherwise swap
            if (mismatches[row].getName(0).equals(parent.result.getNames()[0])) {
                swap = 0;
                swapCells[row] = false;
            } else {
                swap = 1;
                swapCells[row] = true;
            }
            // fill cells with hyperliked Export lists
            for (int j=0; j<2; j++, cellNdx++) {
                html.setLength(0);
                html.append("<html><font size=3><font face=\"Helvetica, TimesRoman\">");
                int lineNdx = cellNdx*10000;
                if (mismatches[row] instanceof ExportMismatch.MultiMatch) {
                    List<PortReport> ports = ((ExportMismatch.MultiMatch)mismatches[row]).getAll((j+swap)%2);
                    // each Port has a list of Exports which are printed as hyperlinked list
                    for (Iterator<PortReport> it=ports.iterator(); it.hasNext();) {
                        appendNameOf((PortReport)it.next(), html, lineNdx, false, null);
                        if (it.hasNext()) html.append("<br>" + LSEP);
                        lineNdx++;
                        cellPrefHeights[row][j] += ExportTable.LINEHEIGHT;
                    }
                } else if (mismatches[row] instanceof ExportMismatch.NameMismatch) {
                    if (j == swap) {
                        PortReport port = ((ExportMismatch.NameMismatch)mismatches[row]).getFirstExport(); 
                        appendNameOf(port, html, lineNdx, false, null);
                    } else {
                        NetObjReport no = ((ExportMismatch.NameMismatch)mismatches[row]).getSuggestion();
                        appendNameOf(no, html, lineNdx, true, ExportTable.GREEN);
                    }
                    lineNdx++;
                    cellPrefHeights[row][j] += ExportTable.LINEHEIGHT;
                } else if (mismatches[row] instanceof ExportMismatch.TopologyMismatch) {
                    PortReport port;
                    if (j == swap) {
                        port = ((ExportMismatch.TopologyMismatch)mismatches[row]).getFirstExport();
                        appendNameOf(port, html, lineNdx, true, ExportTable.RED);
                    } else {
                        port = ((ExportMismatch.TopologyMismatch)mismatches[row]).getSecondExport();
                        appendNameOf(port, html, lineNdx, true, ExportTable.RED);
                    }
                    lineNdx++;
                    if (j != swap) { // if a cell in the right column
                        NetObjReport no = ((ExportMismatch.TopologyMismatch)mismatches[row]).getSuggestion();
                        if (no != null) {  // if suggestion exists
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
                if (cellPrefHeights[row][j] > ExportTable.MAX_VISIBLE_LINES*ExportTable.LINEHEIGHT+ExportTable.HEIGHTMARGIN)
                    cellPrefHeights[row][j] = ExportTable.MAX_VISIBLE_LINES*ExportTable.LINEHEIGHT+ExportTable.HEIGHTMARGIN;
                JPanel panel = new JPanel();
                panel.setBackground(Color.WHITE);
                panel.add(htmlPane);
                panes[row][j] = new JScrollPane(panel);
                panes[row][j].setBorder(border);
            }
        }
    }
    
    /**
     * If the provieded NetObject is a Port, then print all Exports 
     * corresponding to it. If the NetObject is a Wire, print its name.
     * @param no  Port or Wire
     * @param html  buffer to print to
     * @param lineNdx  line number on which this text is going to appear 
     * in the parent table cell. Used to create hyperlink indices
     * @param doColoring  paint text to the provided Color if this is true.
     * Print in default color otherwise.
     * @param sugColor  text color
     */
    protected void appendNameOf(NetObjReport no, StringBuffer html, 
            int lineNdx, boolean doColoring, String sugColor) {
        String href = "<a style=\"text-decoration: none\" href=\"";
        String text = null;
        boolean isImpl = false;
        if (no instanceof PortReport) {
            PortReport port = (PortReport)no;
            isImpl = port.isImplied();
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
        } else if (no instanceof WireReport) {
            text = no.getName();
            if (doColoring) html.append("<font COLOR=\"" + sugColor + "\">");
            html.append(href + lineNdx + "\">"+ text +"</a></font>");
            if (doColoring) html.append("</font>");
        }
    }
    
//    /**
//     * Detect whether a Port in mismamatch has noo Exports.
//     * If no Exports exist then the Port has  an implied Export  
//     * @param port  Port to test
//     * @return true if Port has only an implied Export, false otherwise
//     */
//    protected boolean isImplied(PortReport port) {
//        Iterator it=port.getWire().getNameProxy().getNet().getExports();
//        if (it.hasNext()) return false;
//        return true;
//    }
    
    /**
     * Highlight Exportson Port with the provided index. 
     * Index encodes table row, table column, and line number inside 
     * the table cell on which the Port is printed  
     * @param index  Port index
     */
    protected void highlight(int index) {
        // decode index
        int line = index%10000;
        int row = (index/10000)/2;
        int col = (index/10000)%2;
        if (swapCells[row]) col = (col+1)%2;
        
        ExportMismatch em = mismatches[row];            
        Cell cell = em.getCell(col);
        VarContext context = em.getContext(col);
        
        // find the highlighter corresponding to the cell
        Highlighter highlighter = HighlightTools.getHighlighter(cell, context);
        if (highlighter == null) return;
            
        // find what to highlight 
        if (em instanceof ExportMismatch.MultiMatch) {
            List<PortReport> ports = ((ExportMismatch.MultiMatch)em).getAll(col);
            int i;
            Iterator<PortReport> it;
            // go to the necessary line
            for (it=ports.iterator(), i=0; it.hasNext()&&i<line; i++,it.next());
            PortReport port = (PortReport)it.next();
            HighlightTools.highlightPortExports(highlighter, cell, port);
        } else if (em instanceof ExportMismatch.NameMismatch) {
            PortReport port;
            NetObjReport portOrWire;
            if (col == 0) {
                port = ((ExportMismatch.NameMismatch)em).getFirstExport();
                HighlightTools.highlightPortExports(highlighter, cell, port);
            } else {
                portOrWire = ((ExportMismatch.NameMismatch)em).getSuggestion();
                HighlightTools.highlightPortOrWire(highlighter, cell, portOrWire);
            }
        } else if (em instanceof ExportMismatch.TopologyMismatch) {
            PortReport port1, port2;
            NetObjReport portOrWire;
            if (col == 0) {
                port1 = ((ExportMismatch.TopologyMismatch)em).getFirstExport();
                HighlightTools.highlightPortExports(highlighter, cell, port1);
            } else if (line == 0) {
                port2 = ((ExportMismatch.TopologyMismatch)em).getSecondExport();
                HighlightTools.highlightPortExports(highlighter, cell, port2);
            } else if (line == 1) {  // the second line has a suggestion
                portOrWire = ((ExportMismatch.TopologyMismatch)em).getSuggestion();
                HighlightTools.highlightPortOrWire(highlighter, cell, portOrWire);
            }
        }
        highlighter.finished();
    }
    
    /**
     * Get column name
     * @param col  column
     * @return name of the column
     */ 
    public String getColumnName(int col) {
        return parent.result.getNames()[col];
    }

}
