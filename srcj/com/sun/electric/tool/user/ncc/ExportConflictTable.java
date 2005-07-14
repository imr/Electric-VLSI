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

public abstract class ExportConflictTable extends ExportTable {
    protected ExportConflict[] conflicts;
    public ExportConflictTable(NccComparisonMismatches res) { super(res, 4); }
    public void postSetup() {
        getTableHeader().setReorderingAllowed(false);
        for (int col=0; col < 4; col++)
            getColumnModel().getColumn(col).addPropertyChangeListener(this);
    }
    
    public static class NetworkTable extends ExportConflictTable {
        public NetworkTable(NccComparisonMismatches res) {
            super(res);
            conflicts = (ExportConflict.NetworkConflict[])result
                  .getNetworkExportConflicts().toArray(new ExportConflict.NetworkConflict[0]);
            height = Math.min(conflicts.length, MAXROWS);
            setup();
            setModel(new ExportConflictTableModel.NetworkTableModel(this));
            postSetup();
        }
    }
    
    public static class CharacteristicsTable extends ExportConflictTable {
        public CharacteristicsTable(NccComparisonMismatches res) {
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


abstract class ExportConflictTableModel extends ExportTableModel {
    protected ExportConflict[] conflicts;
    protected int[][] cellPrefHeights = parent.cellPrefHeights;
    protected int[][] cellPrefWidths  = parent.cellPrefWidths;
    protected static String href = "<a style=\"text-decoration: none\" href=\"";
    
    public ExportConflictTableModel(ExportConflictTable parent) {
        super(parent);
        conflicts = parent.conflicts;
        cellPrefHeights = parent.cellPrefHeights;
        cellPrefWidths  = parent.cellPrefWidths;
        
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
                if (cellPrefHeights[row][col] > ExportTable.MAXLINES*ExportTable.LINEHEIGHT+ExportTable.HEIGHTMARGIN)
                    cellPrefHeights[row][col] = ExportTable.MAXLINES*ExportTable.LINEHEIGHT+ExportTable.HEIGHTMARGIN;
                JPanel panel = new JPanel();
                panel.setBackground(Color.WHITE);
                panel.add(htmlPane);
                panes[row][col] = new JScrollPane(panel);
                panes[row][col].setBorder(BorderFactory.createEmptyBorder());
            }
    }

    protected abstract void appendText(int row, int col, StringBuffer txtBuf);

    protected void highlight(int index) {
        int col = index%10;
        int row  = index/10;
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
    
    protected abstract void addToHighlighter(int row, int col, Highlighter highlighter);

    
    protected static class NetworkTableModel extends ExportConflictTableModel {
        protected static String[] colNames = {"Cell", "Conflicting Name", 
                                       "Global Network", "Export Network"};        
        public NetworkTableModel(ExportConflictTable parent) { super(parent); }
        protected void appendText(int row, int col, StringBuffer buf) {
            buf.append(href + (row*10+col) + "\">"
                    + conflicts[row].getDescription(col-2) + "</a>");
        }
        protected void addToHighlighter(int row, int col, Highlighter highlighter) {
            if (col != 2 && col != 3) return;
            ExportConflict.NetworkConflict nc = 
                (ExportConflict.NetworkConflict)conflicts[row];
            highlighter.addNetwork(nc.getNetwork(col-2), nc.getCell());
            System.out.println(" ### Added to highlight Network: " + nc.getNetwork(col-2).describe(false));            
        }
        public String getColumnName(int col) { return colNames[col]; }        
    }
    

    protected static class CharacteristicsTableModel extends ExportConflictTableModel {
        protected static String[] colNames = {"Cell", "Conflicting Name", 
                       "Global Characteristics", "Export Characteristics"};        
        public CharacteristicsTableModel(ExportConflictTable parent) { super(parent); }
        protected void appendText(int row, int col, StringBuffer buf) {
            String text = conflicts[row].getDescription(col-2);
            if (col == 2) {
                buf.append(text);
            } else if (col == 3)
                buf.append(href + (row*10+col) + "\">" + text + "</a>");
        }
        protected void addToHighlighter(int row, int col, Highlighter highlighter) {
            if (col != 3) return;
            ExportConflict.CharactConflict cc = 
                (ExportConflict.CharactConflict)conflicts[row];
            highlighter.addText(cc.getGlobalExport(), cc.getCell(), null, null);
            System.out.println(" ### Added to highlight Export: " + cc.getGlobalExport().getName());
        }
        public String getColumnName(int col) { return colNames[col]; }        
    }     
}
