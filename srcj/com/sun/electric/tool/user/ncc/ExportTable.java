/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: ExportTable.java
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
import java.awt.Component;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventObject;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

abstract class ExportTable extends JTable 
implements PropertyChangeListener, ActionListener {
    // constants
    static final int MAXROWS = 200;
    static final int WIDTHMARGIN = 11;
    static final int HEIGHTMARGIN = 16;
    static final int LINEHEIGHT = 17;
    static final int SCRLBARHEIGHT = 17;
    static final int MAX_VISIBLE_LINES = 6;
    
    protected static final String RED = "red";
    protected static final String GREEN = "green";

    // GUI variables
    protected int height;
    protected int numCols;
    protected int[][] cellPrefHeights;
    protected int[][] cellPrefWidths;
    protected JPopupMenu cellPopup;
    protected String clipboard;
            
    // data holders
    protected NccGuiInfo result;
 
    public ExportTable(NccGuiInfo res, int cols) {
        result = res;
        numCols = cols;
    }
    
    protected void setup() {
        setDefaultRenderer(JScrollPane.class, new CellRenderer());
        setDefaultEditor(JScrollPane.class, new CellEditor());
        setCellSelectionEnabled(true);
        setColumnSelectionAllowed(true);
        setRowSelectionAllowed(true);
        setGridColor(Color.GRAY);
        setBorder(BorderFactory.createEmptyBorder());
        addMouseMotionListener(new CellMouseMotionAdapter());
        createCellPopup();

        cellPrefHeights = new int[height][numCols];
        cellPrefWidths = new int[height][numCols];
        for (int row = 0; row<height; row++) 
            for (int col=0; col<numCols; col++)
                cellPrefHeights[row][col] = HEIGHTMARGIN;
    }
    
    void adjustRowHeights() {
        int colWidth[] = new int[numCols];
        for (int col=0; col<numCols; col++)
            colWidth[col] = getColumnModel().getColumn(col).getWidth();

        for (int row = 0; row < height; row++) {
            int oldHeight = getRowHeight(row);
            int newHeight = 0, pref = HEIGHTMARGIN;
            for (int col=0; col<numCols; col++) {    
                pref = cellPrefHeights[row][col];
                // if cell requires a horizontal scrollbar
                if (colWidth[col] < cellPrefWidths[row][col])
                    pref += SCRLBARHEIGHT;
                newHeight = Math.max(newHeight, pref);                
            }
            if (newHeight != oldHeight)
                setRowHeight(row, newHeight);
        }
        doLayout();
    }
    
    public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals("width")) {
            adjustRowHeights();
        }
    }

    /* (non-Javadoc)
     * Action Listener interface (for popup menus)
     */
    public void actionPerformed(ActionEvent e) {
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        // copy text prepared during popup to clipboard
        StringSelection ss = new StringSelection(clipboard);
        cb.setContents(ss,ss);                    
    }

    protected void createCellPopup() {
        cellPopup = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("Copy Cell Text To Clipboard");
        menuItem.addActionListener(this);
        cellPopup.add(menuItem);
    }
}

class CellMouseAdapter extends MouseAdapter {
    public int lastClick = -1;
    private ExportTable parent;
    public CellMouseAdapter(ExportTable parent) { this.parent = parent; }
    public void mousePressed(MouseEvent e) {
        lastClick = e.getButton();
        if (! (e.getButton() == MouseEvent.BUTTON2 
            || e.getButton() == MouseEvent.BUTTON3)
            || ! (e.getSource() instanceof JEditorPane))
            return;
                       
        Document doc = ((JEditorPane)e.getSource()).getDocument();
        try {
            parent.clipboard = doc.getText(0, doc.getLength()).trim();
        } catch (BadLocationException e1) {
            System.out.println("Text cannot be retrieved due to bad locale");
        }
        parent.cellPopup.show(e.getComponent(), e.getX(), e.getY());
    }
}

class CellMouseMotionAdapter extends MouseMotionAdapter {
    public void mouseMoved(MouseEvent e) {
        JTable aTable =  (JTable)e.getSource();
        int row = aTable.rowAtPoint(e.getPoint());
        int column = aTable.columnAtPoint(e.getPoint());
        aTable.editCellAt(row, column);
    }
}

abstract class ExportTableModel extends AbstractTableModel implements HyperlinkListener {
    protected final String LSEP = System.getProperty("line.separator");  
    
    protected ExportTable parent;
    protected int height;
    protected int numCols;
    protected JScrollPane[][] panes;
    protected Insets insets = new Insets(0,0,0,0);
    protected CellMouseAdapter mouseAdapter;
    
    public ExportTableModel(ExportTable parent) {
        super();
        this.parent = parent;
        height = parent.height;
        numCols = parent.numCols;
        mouseAdapter = new CellMouseAdapter(parent);        
        panes = new JScrollPane[height][numCols];     
    }
    
    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    public int getColumnCount() { return numCols; }
    public int getRowCount()    { return height; }

    public abstract String getColumnName(int col);

    public Object getValueAt(int row, int col) {
        return panes[row][col];
    }

    public Class<?> getColumnClass(int c) {
        return JScrollPane.class;
    }

    public boolean isCellEditable(int row, int col) {
        return true;
    }

    public void setValueAt(Object value, int row, int col) {
        panes[row][col] = (JScrollPane)value;
    }
    
    public void hyperlinkUpdate(HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED
                && mouseAdapter.lastClick == MouseEvent.BUTTON1) {
            highlight(Integer.parseInt(event.getDescription()));
        }
    }

    protected abstract void highlight(int index);
}

class CellRenderer implements TableCellRenderer { 
    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean hasFocus, int row, int col){
        return (JScrollPane)value;
    }
}

class CellEditor implements TableCellEditor {
    public Component getTableCellEditorComponent(JTable table, Object value, 
                                  boolean isSelected, int row, int column) {
        return (JScrollPane)table.getModel().getValueAt(row, column);
    }

    public Object getCellEditorValue() { return null; }
    public boolean isCellEditable  (EventObject eo) { return true; }
    public boolean shouldSelectCell(EventObject so) { return true; }
    public boolean stopCellEditing ()               { return true; }

    public void cancelCellEditing() {}
    public void addCellEditorListener(CellEditorListener el) {}
    public void removeCellEditorListener(CellEditorListener el) {}
}

